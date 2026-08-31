/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.clock

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miguelthemann.remarkable.R
import com.miguelthemann.remarkable.prefs.BackgroundMode
import com.miguelthemann.remarkable.prefs.ClockStyle
import com.miguelthemann.remarkable.prefs.ThemeMode
import com.miguelthemann.remarkable.spotify.SpotifyStatus
import com.miguelthemann.remarkable.ui.ambient.AmbientBackground
import com.miguelthemann.remarkable.ui.ambient.rememberAmbientPalette
import com.miguelthemann.remarkable.ui.burnin.BurnInProtectedContent
import com.miguelthemann.remarkable.weather.celsiusToFahrenheit
import com.miguelthemann.remarkable.weather.weatherIcon
import com.miguelthemann.remarkable.weather.weatherLabelRes
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockScreen(
    viewModel: ClockViewModel,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onLocationPermission(granted) }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onLocationPermission(granted)
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                ),
            )
        },
    ) { inner ->
        DeskClockContent(
            state = state,
            viewModel = viewModel,
            immersive = false,
            onOpenSettings = onOpenSettings,
            onRequestLocation = {
                permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            },
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
        )
    }
}

@Composable
fun DeskClockContent(
    state: ClockUiState,
    viewModel: ClockViewModel,
    immersive: Boolean,
    @Suppress("UNUSED_PARAMETER") onOpenSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onRequestLocation: (() -> Unit)? = null,
) {
    val dark = when (state.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        else -> isSystemInDarkTheme() || state.nightDim
    }
    val palette = rememberAmbientPalette(
        mode = state.backgroundMode,
        now = state.now,
        weather = state.weather,
        accentArgb = state.accentArgb,
        customBgArgb = state.customBgArgb,
        darkTheme = dark || state.nightDim,
    )

    Box(modifier = modifier.fillMaxSize()) {
        AmbientBackground(
            palette = palette,
            imagePath = state.backgroundImagePath,
            useImage = state.backgroundMode == BackgroundMode.IMAGE,
            darkTheme = dark || state.nightDim,
        )
        BurnInProtectedContent(
            enabled = state.burnInProtection,
            shiftDp = state.burnInShiftDp,
            intervalSec = state.burnInIntervalSec,
            smartPixels = state.smartPixels,
            smartPixelsStrength = state.smartPixelsStrength,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = if (immersive) 24.dp else 8.dp),
            ) {
                val landscape = maxWidth > maxHeight
                if (landscape) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ClockHero(
                            state = state,
                            modifier = Modifier
                                .weight(1.15f)
                                .fillMaxHeight(),
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (state.showWeather) {
                                Box(Modifier = Modifier.offset(state.modules.weatherX.dp, state.modules.weatherY.dp)) {
                                    WeatherCard(state, onRequestLocation)
                                }
                            }
                            if (state.showSpotify) {
                                Box(modifier = Modifier.offset(state.modules.spotifyX.dp, state.modules.spotifyY.dp)) {
                                    SpotifyCard(state, viewModel)
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ClockHero(state = state)
                        if (state.showWeather) {
                            Spacer(Modifier.height(16.dp))
                            Box(modifier = Modifier.offset(state.modules.weatherX.dp, state.modules.weatherY.dp)) {
                                WeatherCard(state, onRequestLocation)
                            }
                        }
                        if (state.showSpotify) {
                            Spacer(Modifier.height(12.dp))
                            Box(modifier = Modifier.offset(state.modules.spotifyX.dp, state.modules.spotifyY.dp)) {
                                SpotifyCard(state, viewModel)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ClockHero(
    state: ClockUiState,
    modifier: Modifier = Modifier,
) {
    val date = state.now.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
    val weekday = state.now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.offset(state.modules.timeX.dp, state.modules.timeY.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (state.clockStyle) {
                    ClockStyle.ANALOG -> AnalogClock(now = state.now)
                    ClockStyle.DIGITAL -> DigitalClock(
                        now = state.now,
                        use24Hour = state.use24Hour,
                        showSeconds = state.showSeconds,
                    )
                    ClockStyle.BOTH -> {
                        AnalogClock(now = state.now, size = 160.dp)
                        Spacer(Modifier.height(8.dp))
                        DigitalClock(
                            now = state.now,
                            use24Hour = state.use24Hour,
                            showSeconds = state.showSeconds,
                        )
                    }
                }
            }
        }
        if (state.showDate) {
            Box(modifier = Modifier.offset(state.modules.dateX.dp, state.modules.dateY.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = weekday.replaceFirstChar { it.titlecase(Locale.getDefault()) },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherCard(
    state: ClockUiState,
    onRequestLocation: (() -> Unit)?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f),
        ),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.weather_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            val weather = state.weather
            when {
                weather != null -> {
                    val temp = if (state.useCelsius) {
                        "${weather.temperatureC.toInt()}°C"
                    } else {
                        "${weather.temperatureC.celsiusToFahrenheit().toInt()}°F"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            imageVector = weatherIcon(weather.weatherCode),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column {
                            Text(temp, style = MaterialTheme.typography.headlineMedium)
                            Text(
                                text = stringResource(weatherLabelRes(weather.weatherCode)),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = weather.place,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    weather.humidity?.let {
                        Text(
                            text = stringResource(R.string.humidity, it),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                state.weatherMessage is WeatherMessage.NeedsLocation -> {
                    Text(stringResource(R.string.weather_permission))
                    if (onRequestLocation != null) {
                        TextButton(onClick = onRequestLocation) {
                            Text(stringResource(R.string.grant_location))
                        }
                    }
                }
                state.weatherMessage is WeatherMessage.Error -> {
                    Text(stringResource(R.string.weather_unavailable))
                    Text(
                        text = (state.weatherMessage as WeatherMessage.Error).text,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> Text(stringResource(R.string.weather_unavailable))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.weather_attribution),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun SpotifyCard(
    state: ClockUiState,
    viewModel: ClockViewModel,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        ),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.spotify_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            when (val spotify = state.spotify) {
                SpotifyStatus.NeedClientId -> Text(stringResource(R.string.spotify_need_client_id))
                SpotifyStatus.MissingApp -> Text(stringResource(R.string.spotify_not_installed))
                is SpotifyStatus.Failed -> {
                    Text(stringResource(R.string.spotify_error))
                    Text(spotify.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = viewModel::connectSpotify) {
                        Text(stringResource(R.string.spotify_connect))
                    }
                }
                SpotifyStatus.Connecting, SpotifyStatus.Idle -> {
                    Button(onClick = viewModel::connectSpotify) {
                        Text(stringResource(R.string.spotify_connect))
                    }
                }
                is SpotifyStatus.Connected -> {
                    val playing = spotify.nowPlaying
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Artwork(playing?.artwork)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playing?.title ?: stringResource(R.string.spotify_idle),
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!playing?.artist.isNullOrBlank()) {
                                Text(
                                    text = playing?.artist.orEmpty(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledTonalIconButton(onClick = viewModel::skipPrevious) {
                            Icon(
                                Icons.Outlined.SkipPrevious,
                                contentDescription = stringResource(R.string.skip_previous),
                            )
                        }
                        FilledTonalIconButton(
                            onClick = viewModel::playPause,
                            modifier = Modifier.size(56.dp),
                        ) {
                            val paused = playing?.isPaused != false
                            Icon(
                                imageVector = if (paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                                contentDescription = stringResource(
                                    if (paused) R.string.play else R.string.pause,
                                ),
                            )
                        }
                        FilledTonalIconButton(onClick = viewModel::skipNext) {
                            Icon(
                                Icons.Outlined.SkipNext,
                                contentDescription = stringResource(R.string.skip_next),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Artwork(bitmap: Bitmap?) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
    }
}
