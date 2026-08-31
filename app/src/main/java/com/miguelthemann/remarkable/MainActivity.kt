/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miguelthemann.remarkable.ui.RemarkableNav
import com.miguelthemann.remarkable.ui.clock.ClockViewModel
import com.miguelthemann.remarkable.ui.theme.RemarkableTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ClockViewModel = viewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(state.keepAwake, state.nightDim) {
                if (state.keepAwake) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                val lp = window.attributes
                lp.screenBrightness =
                    if (state.nightDim) 0.08f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }

            RemarkableTheme(
                themeMode = state.themeMode,
                nightDim = state.nightDim,
                accentArgb = state.accentArgb,
            ) {
                RemarkableNav(viewModel = viewModel)
            }
        }
    }
}
