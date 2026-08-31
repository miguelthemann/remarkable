/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.miguelthemann.remarkable.ui.clock.ClockScreen
import com.miguelthemann.remarkable.ui.clock.ClockViewModel
import com.miguelthemann.remarkable.ui.onboarding.OnboardingScreen
import com.miguelthemann.remarkable.ui.settings.SettingsScreen

private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_CLOCK = "clock"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun RemarkableNav(viewModel: ClockViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (!state.prefsReady) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val start = if (state.onboardingDone) ROUTE_CLOCK else ROUTE_ONBOARDING
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = start) {
        composable(ROUTE_ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    viewModel.completeOnboarding()
                    navController.navigate(ROUTE_CLOCK) {
                        popUpTo(ROUTE_ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(ROUTE_CLOCK) {
            ClockScreen(
                viewModel = viewModel,
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
            )
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
