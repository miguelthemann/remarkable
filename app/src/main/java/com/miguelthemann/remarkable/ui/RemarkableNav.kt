/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.miguelthemann.remarkable.ui.clock.ClockScreen
import com.miguelthemann.remarkable.ui.clock.ClockViewModel
import com.miguelthemann.remarkable.ui.settings.SettingsScreen

private const val ROUTE_CLOCK = "clock"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun RemarkableNav(viewModel: ClockViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ROUTE_CLOCK) {
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
