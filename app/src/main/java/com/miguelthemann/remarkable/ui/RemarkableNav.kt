/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miguelthemann.remarkable.ui.clock.ClockScreen
import com.miguelthemann.remarkable.ui.clock.ClockViewModel
import com.miguelthemann.remarkable.ui.motion.EmphasizedAccelerate
import com.miguelthemann.remarkable.ui.motion.EmphasizedDecelerate
import com.miguelthemann.remarkable.ui.onboarding.OnboardingScreen
import com.miguelthemann.remarkable.ui.settings.SettingsScreen

/**
 * Nav without NavHost for Settings — Compose route transitions were invisible on device,
 * so Settings is a full-screen slide overlay. Boring? Maybe. Works? Absolutely.
 */
@Composable
fun RemarkableNav(viewModel: ClockViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (!state.prefsReady) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var showSettings by remember { mutableStateOf(false) }
    var showOnboarding by remember(state.onboardingDone) {
        mutableStateOf(!state.onboardingDone)
    }

    Box(Modifier.fillMaxSize()) {
        when {
            showOnboarding -> {
                OnboardingScreen(
                    onFinished = {
                        viewModel.completeOnboarding()
                        showOnboarding = false
                    },
                )
            }
            else -> {
                ClockScreen(
                    viewModel = viewModel,
                    onOpenSettings = { showSettings = true },
                )
            }
        }

        AnimatedVisibility(
            visible = showSettings && !showOnboarding,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(20f),
            enter = slideInHorizontally(
                animationSpec = tween(460, easing = EmphasizedDecelerate),
                initialOffsetX = { full -> full },
            ) + fadeIn(animationSpec = tween(280, easing = EmphasizedDecelerate)),
            exit = slideOutHorizontally(
                animationSpec = tween(380, easing = EmphasizedAccelerate),
                targetOffsetX = { full -> full },
            ) + fadeOut(animationSpec = tween(220, easing = EmphasizedAccelerate)),
        ) {
            BackHandler { showSettings = false }
            Surface(modifier = Modifier.fillMaxSize()) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { showSettings = false },
                    onFactoryResetDone = {
                        showSettings = false
                        showOnboarding = true
                    },
                )
            }
        }
    }
}
