/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.spotify

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.miguelthemann.remarkable.spotify.SpotifyAuth
import com.miguelthemann.remarkable.ui.clock.ClockViewModel

/** Launches Spotify OAuth when [ClockViewModel.authorizeSpotify] is called. */
@Composable
fun SpotifyAuthHandler(viewModel: ClockViewModel) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.handleSpotifyAuthActivityResult(result.resultCode, result.data)
    }

    LaunchedEffect(activity) {
        val host = activity ?: return@LaunchedEffect
        viewModel.spotifyAuthRequests.collect { clientId ->
            launcher.launch(SpotifyAuth.createLoginIntent(host, clientId))
        }
    }
}
