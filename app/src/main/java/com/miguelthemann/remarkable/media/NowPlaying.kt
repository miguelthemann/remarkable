/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.media

import android.graphics.Bitmap

/** One track, any source — system session, Spotify, or Last.fm. */
data class NowPlayingTrack(
    val title: String,
    val artist: String,
    val album: String = "",
    val releaseYear: String? = null,
    val isPaused: Boolean = false,
    val artwork: Bitmap? = null,
    val appLabel: String? = null,
)

enum class MusicSource {
    /** Android MediaSession / whatever is playing on the device. Default. */
    SYSTEM,
    SPOTIFY,
    LASTFM,
}

sealed interface MusicStatus {
    data object Idle : MusicStatus
    data object Loading : MusicStatus
    data object NeedNotificationAccess : MusicStatus
    data object NeedSpotifySetup : MusicStatus
    data object NeedLastFmSetup : MusicStatus
    data object NothingPlaying : MusicStatus
    data class Ready(val track: NowPlayingTrack, val canControl: Boolean) : MusicStatus
    data class Failed(val message: String) : MusicStatus
}
