/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miguelthemann.remarkable.R
import com.miguelthemann.remarkable.overlay.ClockOverlayService
import com.miguelthemann.remarkable.prefs.AccentPreset
import com.miguelthemann.remarkable.prefs.BackgroundMode
import com.miguelthemann.remarkable.prefs.ClockStyle
import com.miguelthemann.remarkable.prefs.CustomBgPreset
import com.miguelthemann.remarkable.prefs.ThemeMode
import com.miguelthemann.remarkable.ui.clock.ClockViewModel
import com.miguelthemann.remarkable.ui.clock.ModuleAxis
import com.miguelthemann.remarkable.ui.theme.fromArgbLong
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ClockViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var cityDraft by remember { mutableStateOf(state.city) }
    var clientDraft by remember { mutableStateOf(state.spotifyClientId) }
    var cityDirty by remember { mutableStateOf(false) }
    var clientDirty by remember { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) viewModel.importBackgroundImage(uri)
    }

    LaunchedEffect(state.city) {
        if (!cityDirty) cityDraft = state.city
    }
    LaunchedEffect(state.spotifyClientId) {
        if (!clientDirty) clientDraft = state.spotifyClientId
    }
    LaunchedEffect(cityDraft) {
        delay(600)
        if (cityDirty && cityDraft != state.city) viewModel.setCity(cityDraft)
    }
    LaunchedEffect(clientDraft) {
        delay(600)
        if (clientDirty && clientDraft != state.spotifyClientId) {
            viewModel.setSpotifyClientId(clientDraft)
        }
    }
    LaunchedEffect(state.overlayEnabled) {
        val runningIntent = Intent(context, ClockOverlayService::class.java)
        if (state.overlayEnabled) {
            if (Settings.canDrawOverlays(context)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(runningIntent)
                } else {
                    context.startService(runningIntent)
                }
            }
        } else {
            context.stopService(runningIntent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp),
        ) {
            SectionTitle(stringResource(R.string.settings_appearance))
            Text(
                text = stringResource(R.string.settings_theme_mode),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            ChipRow {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(themeModeLabel(mode)) },
                    )
                }
            }
            Text(
                text = stringResource(R.string.settings_accent),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AccentPreset.entries.forEach { preset ->
                    val color = Color.fromArgbLong(preset.argb)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (state.accentArgb == preset.argb) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape,
                            )
                            .clickable { viewModel.setAccentArgb(preset.argb) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_background),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            ChipRow {
                BackgroundMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.backgroundMode == mode,
                        onClick = { viewModel.setBackgroundMode(mode) },
                        label = { Text(backgroundModeLabel(mode)) },
                    )
                }
            }
            if (state.backgroundMode == BackgroundMode.CUSTOM_COLOR) {
                Text(
                    text = stringResource(R.string.settings_custom_bg_color),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = stringResource(R.string.settings_custom_bg_color_help),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                ColorSwatchRow(
                    selectedArgb = state.customBgArgb,
                    presets = CustomBgPreset.entries.map { it.argb },
                    onSelect = {
                        viewModel.setCustomBgArgb(it)
                    },
                )
                Spacer(Modifier.height(8.dp))
                SolidColorPicker(
                    argb = state.customBgArgb,
                    onColorChange = viewModel::setCustomBgArgb,
                )
            }
            if (state.backgroundMode == BackgroundMode.IMAGE) {
                Text(
                    text = stringResource(R.string.settings_background_image_help),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = { imagePicker.launch("image/*") }) {
                        Text(stringResource(R.string.settings_pick_image))
                    }
                    if (state.backgroundImagePath.isNotBlank()) {
                        OutlinedButton(onClick = viewModel::clearBackgroundImage) {
                            Text(stringResource(R.string.settings_clear_image))
                        }
                    }
                }
                if (state.backgroundImagePath.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.settings_image_selected),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
            Text(
                text = stringResource(R.string.settings_clock_style),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            ChipRow {
                ClockStyle.entries.forEach { style ->
                    FilterChip(
                        selected = state.clockStyle == style,
                        onClick = { viewModel.setClockStyle(style) },
                        label = { Text(clockStyleLabel(style)) },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.settings_clock))
            ToggleRow(stringResource(R.string.settings_use_24h), state.use24Hour, viewModel::setUse24Hour)
            ToggleRow(stringResource(R.string.settings_show_seconds), state.showSeconds, viewModel::setShowSeconds)
            ToggleRow(stringResource(R.string.settings_keep_awake), state.keepAwake, viewModel::setKeepAwake)
            ToggleRow(stringResource(R.string.settings_night_dim), state.nightDim, viewModel::setNightDim)
            ToggleRow(stringResource(R.string.settings_show_date), state.showDate, viewModel::setShowDate)
            ToggleRow(stringResource(R.string.settings_show_weather_mod), state.showWeather, viewModel::setShowWeather)
            ToggleRow(stringResource(R.string.settings_show_spotify_mod), state.showSpotify, viewModel::setShowSpotify)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.settings_modules))
            Text(
                text = stringResource(R.string.settings_modules_help),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            ModuleNudge(stringResource(R.string.module_time), ModuleAxis.TIME_X, ModuleAxis.TIME_Y, viewModel)
            ModuleNudge(stringResource(R.string.module_date), ModuleAxis.DATE_X, ModuleAxis.DATE_Y, viewModel)
            ModuleNudge(stringResource(R.string.module_weather), ModuleAxis.WEATHER_X, ModuleAxis.WEATHER_Y, viewModel)
            ModuleNudge(stringResource(R.string.module_spotify), ModuleAxis.SPOTIFY_X, ModuleAxis.SPOTIFY_Y, viewModel)
            TextButton(
                onClick = viewModel::resetModules,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(stringResource(R.string.settings_reset_modules))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.settings_burn_in))
            ToggleRow(
                stringResource(R.string.settings_burn_in_enable),
                state.burnInProtection,
                viewModel::setBurnInProtection,
            )
            Text(
                text = stringResource(R.string.settings_burn_in_shift, state.burnInShiftDp),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Slider(
                value = state.burnInShiftDp.toFloat(),
                onValueChange = { viewModel.setBurnInShiftDp(it.toInt()) },
                valueRange = 2f..24f,
                steps = 21,
                modifier = Modifier.padding(horizontal = 16.dp),
                enabled = state.burnInProtection,
            )
            Text(
                text = stringResource(R.string.settings_burn_in_interval, state.burnInIntervalSec),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Slider(
                value = state.burnInIntervalSec.toFloat(),
                onValueChange = { viewModel.setBurnInIntervalSec(it.toInt()) },
                valueRange = 15f..300f,
                steps = 18,
                modifier = Modifier.padding(horizontal = 16.dp),
                enabled = state.burnInProtection,
            )
            ToggleRow(
                stringResource(R.string.settings_smart_pixels),
                state.smartPixels,
                viewModel::setSmartPixels,
            )
            Text(
                text = stringResource(R.string.settings_smart_pixels_help),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Text(
                text = stringResource(
                    R.string.settings_smart_pixels_strength,
                    (state.smartPixelsStrength * 100).toInt(),
                ),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Slider(
                value = state.smartPixelsStrength,
                onValueChange = viewModel::setSmartPixelsStrength,
                valueRange = 0.1f..0.8f,
                modifier = Modifier.padding(horizontal = 16.dp),
                enabled = state.burnInProtection && state.smartPixels,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.settings_system))
            Text(
                text = stringResource(R.string.settings_launcher_help),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            TextButton(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                },
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(stringResource(R.string.settings_open_home))
            }
            Text(
                text = stringResource(R.string.settings_screensaver_help),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(Intent("android.settings.DREAM_SETTINGS"))
                    }.onFailure {
                        context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
                    }
                },
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(stringResource(R.string.settings_open_screensaver))
            }
            ToggleRow(
                stringResource(R.string.settings_overlay),
                state.overlayEnabled,
            ) { enabled ->
                if (enabled && !Settings.canDrawOverlays(context)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    )
                    context.startActivity(intent)
                    viewModel.setOverlayEnabled(false)
                } else {
                    viewModel.setOverlayEnabled(enabled)
                }
            }
            Text(
                text = stringResource(R.string.settings_overlay_help),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.settings_weather))
            ToggleRow(stringResource(R.string.settings_use_celsius), state.useCelsius, viewModel::setUseCelsius)
            OutlinedTextField(
                value = cityDraft,
                onValueChange = {
                    cityDirty = true
                    cityDraft = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text(stringResource(R.string.settings_city)) },
                placeholder = { Text(stringResource(R.string.settings_city_placeholder)) },
                singleLine = true,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.settings_spotify))
            OutlinedTextField(
                value = clientDraft,
                onValueChange = {
                    clientDirty = true
                    clientDraft = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text(stringResource(R.string.settings_client_id)) },
                singleLine = true,
            )
            Text(
                text = stringResource(R.string.settings_client_id_help),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.settings_about))
            Text(
                text = stringResource(R.string.settings_license),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
private fun ModuleNudge(
    title: String,
    axisX: ModuleAxis,
    axisY: ModuleAxis,
    viewModel: ClockViewModel,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { viewModel.nudgeModule(axisX, -4f) }) { Text("←") }
            TextButton(onClick = { viewModel.nudgeModule(axisX, 4f) }) { Text("→") }
            TextButton(onClick = { viewModel.nudgeModule(axisY, -4f) }) { Text("↑") }
            TextButton(onClick = { viewModel.nudgeModule(axisY, 4f) }) { Text("↓") }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
    )
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
        ThemeMode.MONET -> R.string.theme_monet
    },
)

@Composable
private fun backgroundModeLabel(mode: BackgroundMode): String = stringResource(
    when (mode) {
        BackgroundMode.SOLID -> R.string.bg_solid
        BackgroundMode.MONET -> R.string.bg_monet
        BackgroundMode.WEATHER -> R.string.bg_weather
        BackgroundMode.TIME_OF_DAY -> R.string.bg_time
        BackgroundMode.CUSTOM -> R.string.bg_custom
        BackgroundMode.CUSTOM_COLOR -> R.string.bg_custom_color
        BackgroundMode.IMAGE -> R.string.bg_image
    },
)

@Composable
private fun clockStyleLabel(style: ClockStyle): String = stringResource(
    when (style) {
        ClockStyle.ANALOG -> R.string.clock_analog
        ClockStyle.DIGITAL -> R.string.clock_digital
        ClockStyle.BOTH -> R.string.clock_both
    },
)
