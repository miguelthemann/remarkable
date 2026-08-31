/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.miguelthemann.remarkable.prefs.SpotifyModuleStyle
import com.miguelthemann.remarkable.prefs.ThemeMode
import com.miguelthemann.remarkable.prefs.WeatherEffect
import com.miguelthemann.remarkable.ui.clock.ClockViewModel
import com.miguelthemann.remarkable.ui.theme.fromArgbLong
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ClockViewModel,
    onBack: () -> Unit,
    onFactoryResetDone: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var cityDraft by remember { mutableStateOf(state.city) }
    var clientDraft by remember { mutableStateOf(state.spotifyClientId) }
    var cityDirty by remember { mutableStateOf(false) }
    var clientDirty by remember { mutableStateOf(false) }
    var resetStep by remember { mutableStateOf(0) } // 0 = idle, 1 = first warn, 2 = final warn
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
                text = stringResource(R.string.settings_modules_drag_help),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            TextButton(
                onClick = viewModel::resetModules,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(stringResource(R.string.settings_reset_modules))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.settings_spotify_module))
            Text(
                text = stringResource(R.string.settings_spotify_style),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            ChipRow {
                SpotifyModuleStyle.entries.forEach { style ->
                    FilterChip(
                        selected = state.spotifyStyle == style,
                        onClick = { viewModel.setSpotifyStyle(style) },
                        label = { Text(spotifyStyleLabel(style)) },
                    )
                }
            }
            ToggleRow(stringResource(R.string.settings_show_spotify_icon), state.showSpotifyIcon, viewModel::setShowSpotifyIcon)
            ToggleRow(stringResource(R.string.settings_generic_music_icon), state.useGenericMusicIcon, viewModel::setUseGenericMusicIcon)
            ToggleRow(stringResource(R.string.settings_show_track_title), state.showTrackTitle, viewModel::setShowTrackTitle)
            ToggleRow(stringResource(R.string.settings_show_artist), state.showArtist, viewModel::setShowArtist)
            ToggleRow(stringResource(R.string.settings_show_album), state.showAlbum, viewModel::setShowAlbum)
            ToggleRow(stringResource(R.string.settings_show_release_year), state.showReleaseYear, viewModel::setShowReleaseYear)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.settings_weather_effect))
            Text(
                text = stringResource(R.string.settings_weather_effect_help),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            ChipRow {
                WeatherEffect.entries.forEach { effect ->
                    FilterChip(
                        selected = state.weatherEffect == effect,
                        onClick = { viewModel.setWeatherEffect(effect) },
                        label = { Text(weatherEffectLabel(effect)) },
                    )
                }
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.settings_danger_zone))
            Text(
                text = stringResource(R.string.settings_reset_all_help),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            OutlinedButton(
                onClick = { resetStep = 1 },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.settings_reset_all))
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (resetStep == 1) {
        ResetConfirmDialog(
            title = stringResource(R.string.settings_reset_confirm1_title),
            body = stringResource(R.string.settings_reset_confirm1_body),
            destructive = false,
            onConfirm = { resetStep = 2 },
            onDismiss = { resetStep = 0 },
        )
    }

    if (resetStep == 2) {
        ResetConfirmDialog(
            title = stringResource(R.string.settings_reset_confirm2_title),
            body = stringResource(R.string.settings_reset_confirm2_body),
            destructive = true,
            onConfirm = {
                resetStep = 0
                context.stopService(Intent(context, ClockOverlayService::class.java))
                viewModel.resetEverything {
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_reset_success),
                        Toast.LENGTH_LONG,
                    ).show()
                    onFactoryResetDone()
                }
            },
            onDismiss = { resetStep = 0 },
        )
    }
}

/**
 * Two-step factory reset gate. The confirm button naps for 3 seconds so
 * rage-taps don't yeet your Spotify Client ID into the sun.
 */
@Composable
private fun ResetConfirmDialog(
    title: String,
    body: String,
    destructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var secondsLeft by remember { mutableIntStateOf(3) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1_000)
            secondsLeft--
        }
    }
    val ready = secondsLeft == 0
    val label = if (ready) {
        stringResource(R.string.settings_reset_action)
    } else {
        stringResource(R.string.settings_reset_action_countdown, secondsLeft)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            if (destructive) {
                Button(
                    onClick = onConfirm,
                    enabled = ready,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f),
                        disabledContentColor = MaterialTheme.colorScheme.onError.copy(alpha = 0.6f),
                    ),
                ) {
                    Text(label)
                }
            } else {
                TextButton(
                    onClick = onConfirm,
                    enabled = ready,
                ) {
                    Text(label)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_reset_cancel))
            }
        },
    )
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
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        headlineContent = { Text(title) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
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

@Composable
private fun spotifyStyleLabel(style: SpotifyModuleStyle): String = stringResource(
    when (style) {
        SpotifyModuleStyle.ONE_LINER -> R.string.spotify_style_oneliner
        SpotifyModuleStyle.CARD -> R.string.spotify_style_card
        SpotifyModuleStyle.WIDGET -> R.string.spotify_style_widget
    },
)

@Composable
private fun weatherEffectLabel(effect: WeatherEffect): String = stringResource(
    when (effect) {
        WeatherEffect.AUTO -> R.string.weather_fx_auto
        WeatherEffect.CLEAR -> R.string.weather_fx_clear
        WeatherEffect.CLOUDY -> R.string.weather_fx_cloudy
        WeatherEffect.RAIN -> R.string.weather_fx_rain
        WeatherEffect.SNOW -> R.string.weather_fx_snow
        WeatherEffect.THUNDER -> R.string.weather_fx_thunder
        WeatherEffect.FOG -> R.string.weather_fx_fog
        WeatherEffect.NONE -> R.string.weather_fx_none
    },
)
