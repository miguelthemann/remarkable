/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.dream

import android.service.dreams.DreamService
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
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
import com.miguelthemann.remarkable.ui.clock.ClockViewModel
import com.miguelthemann.remarkable.ui.clock.DeskClockContent
import com.miguelthemann.remarkable.ui.theme.RemarkableTheme

/** Android Daydream / screen saver. Enable under Settings → Display → Screen saver. */
class RemarkableDreamService : DreamService(), ViewModelStoreOwner, SavedStateRegistryOwner {
    private val store = ViewModelStore()
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private lateinit var viewModel: ClockViewModel

    override val viewModelStore: ViewModelStore get() = store
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry get() = savedStateController.savedStateRegistry

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        isInteractive = true
        isFullscreen = true
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application),
        )[ClockViewModel::class.java]

        val composeView = ComposeView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setViewTreeLifecycleOwner(this@RemarkableDreamService)
            setViewTreeViewModelStoreOwner(this@RemarkableDreamService)
            setViewTreeSavedStateRegistryOwner(this@RemarkableDreamService)
            setContent {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                RemarkableTheme(
                    themeMode = state.themeMode,
                    nightDim = state.nightDim,
                    accentArgb = state.accentArgb,
                ) {
                    DeskClockContent(
                        state = state,
                        viewModel = viewModel,
                        immersive = true,
                        onOpenSettings = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        setContentView(composeView)
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onDetachedFromWindow() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
        super.onDetachedFromWindow()
    }
}
