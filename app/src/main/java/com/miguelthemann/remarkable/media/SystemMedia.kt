/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bus between the notification listener and the clock.
 * I don't know how MediaSessionManager decided this API needed a listener service, but it does so gg.
 */
object SystemMediaBus {
    private val _status = MutableStateFlow<MusicStatus>(MusicStatus.Idle)
    val status: StateFlow<MusicStatus> = _status.asStateFlow()

    @Volatile
    private var activeController: MediaController? = null

    fun publish(status: MusicStatus, controller: MediaController? = null) {
        activeController = controller
        _status.value = status
    }

    fun playPause() {
        val c = activeController ?: return
        val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
        if (playing) c.transportControls.pause() else c.transportControls.play()
    }

    fun skipNext() {
        activeController?.transportControls?.skipToNext()
    }

    fun skipPrevious() {
        activeController?.transportControls?.skipToPrevious()
    }

    fun hasNotificationAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

    fun refreshFrom(context: Context) {
        if (!hasNotificationAccess(context)) {
            publish(MusicStatus.NeedNotificationAccess, null)
            return
        }
        val listener = ComponentName(context, MediaNotificationListener::class.java)
        val manager = context.getSystemService(MediaSessionManager::class.java) ?: run {
            publish(MusicStatus.Failed("No MediaSessionManager"), null)
            return
        }
        val sessions = runCatching { manager.getActiveSessions(listener) }.getOrElse {
            publish(MusicStatus.NeedNotificationAccess, null)
            return
        }
        val controller = sessions.firstOrNull { session ->
            val state = session.playbackState?.state
            state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
        } ?: sessions.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PAUSED
        } ?: sessions.firstOrNull()

        if (controller == null) {
            publish(MusicStatus.NothingPlaying, null)
            return
        }
        val meta = controller.metadata
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty()
        val album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
        if (title.isBlank() && artist.isBlank()) {
            publish(MusicStatus.NothingPlaying, controller)
            return
        }
        val art: Bitmap? = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val paused = controller.playbackState?.state != PlaybackState.STATE_PLAYING
        val appLabel = runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(controller.packageName, 0)).toString()
        }.getOrNull()
        publish(
            MusicStatus.Ready(
                track = NowPlayingTrack(
                    title = title.ifBlank { "—" },
                    artist = artist,
                    album = album,
                    isPaused = paused,
                    artwork = art,
                    appLabel = appLabel,
                ),
                canControl = true,
            ),
            controller,
        )
    }
}

class MediaNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        SystemMediaBus.refreshFrom(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        SystemMediaBus.refreshFrom(this)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        SystemMediaBus.refreshFrom(this)
    }
}
