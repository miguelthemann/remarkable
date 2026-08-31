/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.spotify

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.miguelthemann.remarkable.R
import com.miguelthemann.remarkable.spotify.SpotifyAuth
import com.miguelthemann.remarkable.ui.clock.ClockViewModel

/**
 * Opens Spotify's /authorize page in the browser when
 * [ClockViewModel.authorizeSpotify] is called. The result comes back as the
 * `remarkable://callback` deep link handled by MainActivity, so there is no
 * activity result to collect here.
 */
@Composable
fun SpotifyAuthHandler(viewModel: ClockViewModel) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.spotifyAuthRequests.collect { clientId ->
            val launched = runCatching {
                context.startActivity(SpotifyAuth.createLoginIntent(clientId))
            }.isSuccess
            if (!launched) {
                Toast.makeText(
                    context,
                    R.string.spotify_auth_no_browser,
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }
}
