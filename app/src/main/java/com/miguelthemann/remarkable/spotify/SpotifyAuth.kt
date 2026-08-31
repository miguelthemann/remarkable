/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.spotify

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.TimeUnit

data class SpotifyAuthResult(
    val accessToken: String,
    val expiresAtEpochMs: Long,
)

/**
 * Authorization Code + PKCE flow for Spotify.
 *
 * The authorize step is opened in the browser instead of going through
 * `AuthorizationClient.createLoginActivityIntent`: the Spotify auth SDK prefers the
 * app-to-app flow, which silently drops `setCustomParam` values. Without
 * `code_challenge` reaching /authorize, Spotify treats the app as a confidential
 * client and the token exchange then fails with "invalid client secret".
 */
object SpotifyAuth {
    private const val AUTHORIZE_ENDPOINT = "https://accounts.spotify.com/authorize"
    private const val TOKEN_ENDPOINT = "https://accounts.spotify.com/api/token"
    private const val PKCE_VERIFIER_LENGTH = 64
    private const val STATE_LENGTH = 16

    private val secureRandom = SecureRandom()
    private val tokenExchangeClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }
    private val redirectUri: Uri = Uri.parse(SPOTIFY_REDIRECT_URI)

    @Volatile
    private var pendingCodeVerifier: String? = null

    @Volatile
    private var pendingState: String? = null

    private val scopes = arrayOf(
        "app-remote-control",
        "user-read-playback-state",
        "user-modify-playback-state",
        "user-read-currently-playing",
    )

    @Serializable
    private data class SpotifyTokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("token_type") val tokenType: String,
        @SerialName("expires_in") val expiresIn: Long,
        @SerialName("refresh_token") val refreshToken: String? = null,
    )

    /** Browser intent for /authorize, carrying the PKCE challenge. */
    fun createLoginIntent(clientId: String): Intent {
        val codeVerifier = generateCodeVerifier()
        val state = generateState()
        pendingCodeVerifier = codeVerifier
        pendingState = state

        val authorizeUri = Uri.parse(AUTHORIZE_ENDPOINT)
            .buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", SPOTIFY_REDIRECT_URI)
            .appendQueryParameter("scope", scopes.joinToString(" "))
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", generateCodeChallenge(codeVerifier))
            .appendQueryParameter("state", state)
            .build()

        return Intent(Intent.ACTION_VIEW, authorizeUri)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /**
     * Handles the `remarkable://callback` deep link.
     * Returns null when [uri] is not a Spotify redirect.
     */
    suspend fun parseRedirect(
        uri: Uri?,
        clientId: String,
        clientSecret: String = "",
    ): Result<SpotifyAuthResult>? {
        if (uri == null) return null
        if (uri.scheme != redirectUri.scheme || uri.host != redirectUri.host) return null

        val error = uri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            clearPendingAuth()
            return Result.failure(IllegalStateException("Spotify authorization failed: $error"))
        }

        val expectedState = pendingState
        if (expectedState != null && uri.getQueryParameter("state") != expectedState) {
            clearPendingAuth()
            return Result.failure(
                IllegalStateException("Spotify authorization state mismatch"),
            )
        }

        val code = uri.getQueryParameter("code")
        if (code.isNullOrBlank()) {
            clearPendingAuth()
            return Result.failure(
                IllegalStateException("Spotify returned an empty authorization code"),
            )
        }

        return exchangeCodeForToken(code, clientId, clientSecret)
    }

    fun isAuthorizationRequired(message: String): Boolean {
        val lower = message.lowercase()
        return lower.contains("authorization") ||
            lower.contains("auth flow") ||
            lower.contains("user authorization") ||
            lower.contains("explicit user")
    }

    fun isTokenValid(expiresAtEpochMs: Long): Boolean =
        expiresAtEpochMs > System.currentTimeMillis() + 60_000L

    private suspend fun exchangeCodeForToken(
        code: String,
        clientId: String,
        clientSecret: String = "",
    ): Result<SpotifyAuthResult> = withContext(Dispatchers.IO) {
        runCatching {
            val codeVerifier = pendingCodeVerifier
                ?: throw IllegalStateException("Spotify PKCE code verifier missing")
            clearPendingAuth()

            val formBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", SPOTIFY_REDIRECT_URI)
                .add("code_verifier", codeVerifier)
                .apply {
                    if (clientSecret.isNotBlank()) {
                        add("client_secret", clientSecret)
                    }
                }
                .build()

            val request = Request.Builder()
                .url(TOKEN_ENDPOINT)
                .post(formBody)
                .build()

            tokenExchangeClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                    ?: throw IllegalStateException("Spotify token exchange returned an empty response")

                if (!response.isSuccessful) {
                    throw IllegalStateException(
                        "Spotify token exchange failed: ${response.code} $responseBody",
                    )
                }

                val tokenResponse = json.decodeFromString<SpotifyTokenResponse>(responseBody)
                val expiresAtEpochMs = System.currentTimeMillis() +
                    tokenResponse.expiresIn.coerceAtLeast(60) * 1000L

                SpotifyAuthResult(
                    accessToken = tokenResponse.accessToken,
                    expiresAtEpochMs = expiresAtEpochMs,
                )
            }
        }.recoverCatching { throwable ->
            throw IllegalStateException(
                "Spotify authorization failed: ${throwable.message ?: throwable.toString()}",
            )
        }
    }

    private fun clearPendingAuth() {
        pendingCodeVerifier = null
        pendingState = null
    }

    private fun generateCodeVerifier(): String = randomUrlSafeString(PKCE_VERIFIER_LENGTH)

    private fun generateState(): String = randomUrlSafeString(STATE_LENGTH)

    private fun randomUrlSafeString(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun generateCodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(codeVerifier.toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
