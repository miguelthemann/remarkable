/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.spotify

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

object SpotifyAuth {
    private const val PKCE_VERIFIER_LENGTH = 64
    private val secureRandom = SecureRandom()
    private val tokenExchangeClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var pendingCodeVerifier: String? = null

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

    fun createLoginIntent(activity: Activity, clientId: String): Intent {
        val codeVerifier = generateCodeVerifier()
        pendingCodeVerifier = codeVerifier
        val codeChallenge = generateCodeChallenge(codeVerifier)

        val request = AuthorizationRequest.Builder(
            clientId,
            AuthorizationResponse.Type.CODE,
            SPOTIFY_REDIRECT_URI,
        )
            .setScopes(scopes)
            .setShowDialog(false)
            .setCustomParam("code_challenge", codeChallenge)
            .setCustomParam("code_challenge_method", "S256")
            .build()
        return AuthorizationClient.createLoginActivityIntent(activity, request)
    }

    suspend fun parseActivityResult(
        resultCode: Int,
        data: Intent?,
        clientId: String,
    ): Result<SpotifyAuthResult> {
        if (data == null) return Result.failure(IllegalStateException("Spotify auth cancelled"))
        return parseResponse(AuthorizationClient.getResponse(resultCode, data), clientId)
    }

    suspend fun parseRedirect(uri: Uri?, clientId: String): Result<SpotifyAuthResult>? {
        if (uri == null) return null
        if (uri.scheme != "remarkable" || uri.host != "callback") return null
        return parseResponse(AuthorizationResponse.fromUri(uri), clientId)
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

    private suspend fun parseResponse(
        response: AuthorizationResponse,
        clientId: String,
    ): Result<SpotifyAuthResult> {
        return when (response.getType()) {
            AuthorizationResponse.Type.CODE -> {
                val code = response.getCode()
                if (code.isNullOrBlank()) {
                    Result.failure(IllegalStateException("Spotify returned an empty authorization code"))
                } else {
                    exchangeCodeForToken(code, clientId)
                }
            }
            AuthorizationResponse.Type.ERROR -> {
                val detail = response.getError()?.takeIf { it.isNotBlank() }
                    ?: "Spotify authorization failed"
                Result.failure(IllegalStateException(detail))
            }
            else -> Result.failure(IllegalStateException("Spotify authorization cancelled"))
        }
    }

    private suspend fun exchangeCodeForToken(
        code: String,
        clientId: String,
    ): Result<SpotifyAuthResult> = withContext(Dispatchers.IO) {
        runCatching {
            val codeVerifier = pendingCodeVerifier
                ?: throw IllegalStateException("Spotify PKCE code verifier missing")
            pendingCodeVerifier = null

            val formBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", SPOTIFY_REDIRECT_URI)
                .add("code_verifier", codeVerifier)
                .build()

            val request = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .post(formBody)
                .header("Content-Type", "application/x-www-form-urlencoded")
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

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(PKCE_VERIFIER_LENGTH)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun generateCodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(codeVerifier.toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
