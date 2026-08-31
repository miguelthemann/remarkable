/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.miguelthemann.remarkable.MainActivity
import com.miguelthemann.remarkable.R
import com.miguelthemann.remarkable.ui.clock.ClockViewModel
import com.miguelthemann.remarkable.ui.clock.DigitalClock
import com.miguelthemann.remarkable.ui.theme.RemarkableTheme

class ClockOverlayService : Service(), ViewModelStoreOwner, SavedStateRegistryOwner {
    private val store = ViewModelStore()
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private lateinit var viewModel: ClockViewModel

    override val viewModelStore: ViewModelStore get() = store
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry get() = savedStateController.savedStateRegistry

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application),
        )[ClockViewModel::class.java]
        startForeground(NOTIFICATION_ID, buildNotification())
        attachOverlay()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    private fun attachOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 120
        }
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ClockOverlayService)
            setViewTreeViewModelStoreOwner(this@ClockOverlayService)
            setViewTreeSavedStateRegistryOwner(this@ClockOverlayService)
            setContent {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                RemarkableTheme(
                    themeMode = state.themeMode,
                    nightDim = state.nightDim,
                    accentArgb = state.accentArgb,
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                                RoundedCornerShape(16.dp),
                            )
                            .clickable {
                                startActivity(
                                    Intent(this@ClockOverlayService, MainActivity::class.java)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        DigitalClock(
                            now = state.now,
                            use24Hour = state.use24Hour,
                            showSeconds = state.showSeconds,
                            compact = true,
                        )
                    }
                }
            }
        }
        composeView = view
        windowManager?.addView(view, params)
    }

    private fun buildNotification(): Notification {
        val channelId = "remarkable_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.overlay_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val pending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.overlay_running))
            .setSmallIcon(R.drawable.ic_stat_clock)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        composeView?.let { windowManager?.removeView(it) }
        composeView = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 42
        const val ACTION_STOP = "com.miguelthemann.remarkable.STOP_OVERLAY"
    }
}
