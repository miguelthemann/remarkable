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

data class SpotifyAuthResult(
    val accessToken: String,
    val expiresAtEpochMs: Long,
)

object SpotifyAuth {
    private val scopes = arrayOf(
        "app-remote-control",
        "user-read-playback-state",
        "user-modify-playback-state",
        "user-read-currently-playing",
    )

    fun createLoginIntent(activity: Activity, clientId: String): Intent {
        val request = AuthorizationRequest.Builder(
            clientId,
            AuthorizationResponse.Type.TOKEN,
            SPOTIFY_REDIRECT_URI,
        )
            .setScopes(scopes)
            .setShowDialog(false)
            .build()
        return AuthorizationClient.createLoginActivityIntent(activity, request)
    }

    fun parseActivityResult(resultCode: Int, data: Intent?, clientId: String): Result<SpotifyAuthResult> {
        if (data == null) return Result.failure(IllegalStateException("Spotify auth cancelled"))
        return parseResponse(AuthorizationClient.getResponse(resultCode, data), clientId)
    }

    fun parseRedirect(uri: Uri?, clientId: String): Result<SpotifyAuthResult>? {
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

    private fun parseResponse(response: AuthorizationResponse, clientId: String): Result<SpotifyAuthResult> {
        return when (response.getType()) {
            AuthorizationResponse.Type.TOKEN -> {
                val token = response.getAccessToken()
                if (token.isNullOrBlank()) {
                    Result.failure(IllegalStateException("Spotify returned an empty token"))
                } else {
                    val expiresInSec = response.getExpiresIn().coerceAtLeast(60)
                    Result.success(
                        SpotifyAuthResult(
                            accessToken = token,
                            expiresAtEpochMs = System.currentTimeMillis() + expiresInSec * 1000L,
                        ),
                    )
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
}
