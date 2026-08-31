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
    val moduleColors = rememberDeskModuleColors(
        darkTheme = dark,
        hour = state.now.hour,
        backgroundMode = state.backgroundMode,
        usePeaks = usePeaks,
    )

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
            ProvideDeskModuleColors(moduleColors) {
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

                if (state.showTime) {
                    DraggableModule(
                        fracX = state.modules.timeX,
                        fracY = state.modules.timeY,
                        scale = state.modules.timeScale,
                        parentSize = size,
                        editMode = layoutEditMode,
                        onEnterEditMode = { layoutEditMode = true },
                        onMoved = { x, y ->
                            viewModel.setModuleOffsets(state.modules.withModule(DeskModule.TIME, x, y))
                        },
                        onScaled = { s ->
                            viewModel.setModuleOffsets(
                                state.modules.withModuleScale(DeskModule.TIME, s),
                            )
                        },
                    ) {
                        TimeModule(state)
                    }
                }

                if (state.showDate) {
                    DraggableModule(
                        fracX = state.modules.dateX,
                        fracY = state.modules.dateY,
                        scale = state.modules.dateScale,
                        parentSize = size,
                        editMode = layoutEditMode,
                        onEnterEditMode = { layoutEditMode = true },
                        onMoved = { x, y ->
                            viewModel.setModuleOffsets(state.modules.withModule(DeskModule.DATE, x, y))
                        },
                        onScaled = { s ->
                            viewModel.setModuleOffsets(
                                state.modules.withModuleScale(DeskModule.DATE, s),
                            )
                        },
                    ) {
                        DateModule(state)
                    }
                }

                if (state.showWeather) {
                    DraggableModule(
                        fracX = state.modules.weatherX,
                        fracY = state.modules.weatherY,
                        scale = state.modules.weatherScale,
                        parentSize = size,
                        editMode = layoutEditMode,
                        onEnterEditMode = { layoutEditMode = true },
                        onMoved = { x, y ->
                            viewModel.setModuleOffsets(
                                state.modules.withModule(DeskModule.WEATHER, x, y),
                            )
                        },
                        onScaled = { s ->
                            viewModel.setModuleOffsets(
                                state.modules.withModuleScale(DeskModule.WEATHER, s),
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
                        scale = state.modules.spotifyScale,
                        parentSize = size,
                        editMode = layoutEditMode,
                        onEnterEditMode = { layoutEditMode = true },
                        onMoved = { x, y ->
                            viewModel.setModuleOffsets(
                                state.modules.withModule(DeskModule.SPOTIFY, x, y),
                            )
                        },
                        onScaled = { s ->
                            viewModel.setModuleOffsets(
                                state.modules.withModuleScale(DeskModule.SPOTIFY, s),
                            )
                        },
                    ) {
                        SpotifyModule(state, viewModel)
                    }
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
                        moduleColors.surface,
                        RoundedCornerShape(24.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.layout_edit_hint),
                    style = MaterialTheme.typography.labelLarge,
                    color = moduleColors.onSurface,
                    modifier = Modifier.padding(start = 8.dp),
                )
                TextButton(onClick = { layoutEditMode = false }) {
                    Text(
                        stringResource(R.string.layout_edit_done),
                        color = moduleColors.accent,
                    )
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
    val colors = LocalDeskModuleColors.current
    ModuleSurface(
        shape = RoundedCornerShape(28.dp),
        horizontalPadding = 20.dp,
        verticalPadding = 16.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (state.clockStyle) {
                ClockStyle.ANALOG -> AnalogClock(
                    now = state.now,
                    size = 168.dp,
                    hourColor = colors.onSurface,
                    minuteColor = colors.accent,
                    secondColor = colors.accentSecondary,
                    markerColor = colors.onSurfaceVariant,
                    trackColor = colors.outline.copy(alpha = 0.35f),
                )
                ClockStyle.DIGITAL -> DigitalClock(
                    now = state.now,
                    use24Hour = state.use24Hour,
                    showSeconds = state.showSeconds,
                    contentColor = colors.onSurface,
                )
                ClockStyle.BOTH -> {
                    AnalogClock(
                        now = state.now,
                        size = 140.dp,
                        hourColor = colors.onSurface,
                        minuteColor = colors.accent,
                        secondColor = colors.accentSecondary,
                        markerColor = colors.onSurfaceVariant,
                        trackColor = colors.outline.copy(alpha = 0.35f),
                    )
                    Spacer(Modifier.height(6.dp))
                    DigitalClock(
                        now = state.now,
                        use24Hour = state.use24Hour,
                        showSeconds = state.showSeconds,
                        contentColor = colors.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun DateModule(state: ClockUiState) {
    val colors = LocalDeskModuleColors.current
    val date = state.now.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
    val weekday = state.now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    ModuleSurface(
        shape = RoundedCornerShape(20.dp),
        horizontalPadding = 20.dp,
        verticalPadding = 12.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = weekday.replaceFirstChar { it.titlecase(Locale.getDefault()) },
                style = MaterialTheme.typography.headlineMedium,
                color = colors.accent,
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
            )
        }
    }
}
