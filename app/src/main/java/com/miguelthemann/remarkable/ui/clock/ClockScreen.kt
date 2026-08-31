/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.clock

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miguelthemann.remarkable.R
import com.miguelthemann.remarkable.prefs.BackgroundMode
import com.miguelthemann.remarkable.prefs.ClockStyle
import com.miguelthemann.remarkable.prefs.ThemeMode
import com.miguelthemann.remarkable.prefs.WeatherEffect
import com.miguelthemann.remarkable.ui.ambient.AmbientBackground
import com.miguelthemann.remarkable.ui.ambient.PeaksWeatherScene
import com.miguelthemann.remarkable.ui.ambient.rememberAmbientPalette
import com.miguelthemann.remarkable.ui.burnin.BurnInProtectedContent
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

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

    DeskClockContent(
        state = state,
        viewModel = viewModel,
        immersive = true,
        onOpenSettings = onOpenSettings,
        onRequestLocation = {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
fun DeskClockContent(
    state: ClockUiState,
    viewModel: ClockViewModel,
    immersive: Boolean,
    onOpenSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onRequestLocation: (() -> Unit)? = null,
) {
    val dark = when (state.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        else -> isSystemInDarkTheme() || state.nightDim
    }
    val usePeaks = state.backgroundMode == BackgroundMode.WEATHER &&
        state.weatherEffect != WeatherEffect.NONE
    val palette = rememberAmbientPalette(
        mode = if (usePeaks) BackgroundMode.SOLID else state.backgroundMode,
        now = state.now,
        weather = state.weather,
        accentArgb = state.accentArgb,
        customBgArgb = state.customBgArgb,
        darkTheme = dark || state.nightDim,
    )

    var layoutEditMode by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (immersive) Modifier else Modifier.systemBarsPadding()),
    ) {
        if (usePeaks) {
            PeaksLayer(
                minuteOfDay = state.now.hour * 60 + state.now.minute,
                weatherCode = state.weather?.weatherCode,
                effectOverride = state.weatherEffect,
            )
        } else {
            AmbientBackground(
                palette = palette,
                imagePath = state.backgroundImagePath,
                useImage = state.backgroundMode == BackgroundMode.IMAGE,
                darkTheme = dark || state.nightDim,
            )
        }

        BurnInProtectedContent(
            enabled = state.burnInProtection && !layoutEditMode,
            shiftDp = state.burnInShiftDp,
            intervalSec = state.burnInIntervalSec,
            smartPixels = state.smartPixels && !layoutEditMode,
            smartPixelsStrength = state.smartPixelsStrength,
            modifier = Modifier.fillMaxSize(),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val size = IntSize(constraints.maxWidth, constraints.maxHeight)

                // Catcher sits under modules in the SAME parent so empty space → Settings,
                // but a finger on a widget hits the widget first (siblings, later on top).
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(0f)
                        .pointerInput(layoutEditMode, onOpenSettings) {
                            if (layoutEditMode) {
                                detectTapGestures(onTap = { layoutEditMode = false })
                            } else if (onOpenSettings != null) {
                                detectTapGestures(onLongPress = { onOpenSettings() })
                            }
                        },
                )

                DraggableModule(
                    fracX = state.modules.timeX,
                    fracY = state.modules.timeY,
                    parentSize = size,
                    editMode = layoutEditMode,
                    onEnterEditMode = { layoutEditMode = true },
                    onMoved = { x, y ->
                        viewModel.setModuleOffsets(state.modules.withModule(DeskModule.TIME, x, y))
                    },
                ) {
                    TimeModule(state)
                }

                if (state.showDate) {
                    DraggableModule(
                        fracX = state.modules.dateX,
                        fracY = state.modules.dateY,
                        parentSize = size,
                        editMode = layoutEditMode,
                        onEnterEditMode = { layoutEditMode = true },
                        onMoved = { x, y ->
                            viewModel.setModuleOffsets(state.modules.withModule(DeskModule.DATE, x, y))
                        },
                    ) {
                        DateModule(state)
                    }
                }

                if (state.showWeather) {
                    DraggableModule(
                        fracX = state.modules.weatherX,
                        fracY = state.modules.weatherY,
                        parentSize = size,
                        editMode = layoutEditMode,
                        onEnterEditMode = { layoutEditMode = true },
                        onMoved = { x, y ->
                            viewModel.setModuleOffsets(
                                state.modules.withModule(DeskModule.WEATHER, x, y),
                            )
                        },
                    ) {
                        WeatherModule(state, onRequestLocation)
                    }
                }

                if (state.showSpotify) {
                    DraggableModule(
                        fracX = state.modules.spotifyX,
                        fracY = state.modules.spotifyY,
                        parentSize = size,
                        editMode = layoutEditMode,
                        onEnterEditMode = { layoutEditMode = true },
                        onMoved = { x, y ->
                            viewModel.setModuleOffsets(
                                state.modules.withModule(DeskModule.SPOTIFY, x, y),
                            )
                        },
                    ) {
                        SpotifyModule(state, viewModel)
                    }
                }
            }
        }

        if (layoutEditMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(10f)
                    .padding(top = 36.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        RoundedCornerShape(24.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.layout_edit_hint),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
                TextButton(onClick = { layoutEditMode = false }) {
                    Text(stringResource(R.string.layout_edit_done))
                }
            }
        }
    }
}

@Composable
private fun PeaksLayer(
    minuteOfDay: Int,
    weatherCode: Int?,
    effectOverride: WeatherEffect,
) {
    PeaksWeatherScene(
        hourOfDay = minuteOfDay / 60f,
        weatherCode = weatherCode,
        effectOverride = effectOverride,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun TimeModule(state: ClockUiState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when (state.clockStyle) {
            ClockStyle.ANALOG -> AnalogClock(now = state.now, size = 168.dp)
            ClockStyle.DIGITAL -> DigitalClock(
                now = state.now,
                use24Hour = state.use24Hour,
                showSeconds = state.showSeconds,
            )
            ClockStyle.BOTH -> {
                AnalogClock(now = state.now, size = 140.dp)
                Spacer(Modifier.height(6.dp))
                DigitalClock(
                    now = state.now,
                    use24Hour = state.use24Hour,
                    showSeconds = state.showSeconds,
                )
            }
        }
    }
}

@Composable
private fun DateModule(state: ClockUiState) {
    val date = state.now.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
    val weekday = state.now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = weekday.replaceFirstChar { it.titlecase(Locale.getDefault()) },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = date,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
