/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.spotify

import android.content.Context
import android.graphics.Bitmap
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.PlayerState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

const val SPOTIFY_REDIRECT_URI = "remarkable://callback"
const val SPOTIFY_PACKAGE = "com.spotify.music"

data class SpotifyNowPlaying(
    val title: String,
    val artist: String,
    val isPaused: Boolean,
    val artwork: Bitmap? = null,
)

sealed interface SpotifyStatus {
    data object Idle : SpotifyStatus
    data object MissingApp : SpotifyStatus
    data object NeedClientId : SpotifyStatus
    data object Connecting : SpotifyStatus
    data class Connected(val nowPlaying: SpotifyNowPlaying?) : SpotifyStatus
    data class Failed(val message: String) : SpotifyStatus
}

class SpotifyRemote(private val context: Context) {
    private var remote: SpotifyAppRemote? = null

    fun isSpotifyInstalled(): Boolean {
        return runCatching {
            context.packageManager.getPackageInfo(SPOTIFY_PACKAGE, 0)
            true
        }.getOrDefault(false)
    }

    fun connect(clientId: String): Flow<SpotifyStatus> = callbackFlow {
        if (clientId.isBlank()) {
            trySend(SpotifyStatus.NeedClientId)
            close()
            return@callbackFlow
        }
        if (!isSpotifyInstalled()) {
            trySend(SpotifyStatus.MissingApp)
            close()
            return@callbackFlow
        }

        trySend(SpotifyStatus.Connecting)
        val params = ConnectionParams.Builder(clientId)
            .setRedirectUri(SPOTIFY_REDIRECT_URI)
            .showAuthView(true)
            .build()

        val listener = object : Connector.ConnectionListener {
            override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                remote = spotifyAppRemote
                spotifyAppRemote.playerApi.subscribeToPlayerState()
                    .setEventCallback { state ->
                        trySend(SpotifyStatus.Connected(state.toNowPlaying()))
                        loadArtwork(spotifyAppRemote, state)
                    }
                    .setErrorCallback { error ->
                        trySend(SpotifyStatus.Failed(error.message ?: error.toString()))
                    }
                trySend(SpotifyStatus.Connected(null))
            }

            override fun onFailure(throwable: Throwable) {
                trySend(SpotifyStatus.Failed(throwable.message ?: throwable.toString()))
            }

            private fun loadArtwork(appRemote: SpotifyAppRemote, state: PlayerState) {
                val imageUri = state.track?.imageUri ?: return
                appRemote.imagesApi.getImage(imageUri)
                    .setResultCallback { bitmap ->
                        trySend(
                            SpotifyStatus.Connected(
                                state.toNowPlaying().copy(artwork = bitmap),
                            ),
                        )
                    }
            }
        }

        SpotifyAppRemote.connect(context, params, listener)
        awaitClose { disconnect() }
    }

    fun play() {
        remote?.playerApi?.resume()
    }

    fun pause() {
        remote?.playerApi?.pause()
    }

    fun skipNext() {
        remote?.playerApi?.skipNext()
    }

    fun skipPrevious() {
        remote?.playerApi?.skipPrevious()
    }

    fun disconnect() {
        remote?.let { SpotifyAppRemote.disconnect(it) }
        remote = null
    }

    private fun PlayerState.toNowPlaying(): SpotifyNowPlaying {
        val track = track
        return SpotifyNowPlaying(
            title = track?.name.orEmpty().ifBlank { "â€”" },
            artist = track?.artist?.name.orEmpty(),
            isPaused = isPaused,
        )
    }
}
