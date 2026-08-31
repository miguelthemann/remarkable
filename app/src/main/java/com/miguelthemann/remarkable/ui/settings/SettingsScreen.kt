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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miguelthemann.remarkable.R
import com.miguelthemann.remarkable.media.MusicSource
import com.miguelthemann.remarkable.prefs.AccentPreset
import com.miguelthemann.remarkable.prefs.BackgroundMode
import com.miguelthemann.remarkable.prefs.ClockStyle
import com.miguelthemann.remarkable.prefs.CustomBgPreset
import com.miguelthemann.remarkable.prefs.SpotifyModuleStyle
import com.miguelthemann.remarkable.prefs.ThemeMode
import com.miguelthemann.remarkable.prefs.WeatherEffect
import com.miguelthemann.remarkable.ui.clock.ClockUiState
import com.miguelthemann.remarkable.ui.clock.ClockViewModel
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect

internal enum class SettingsPage {
    Hub, Appearance, Clock, Music, Weather, BurnIn, System, About,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ClockViewModel,
    onBack: () -> Unit,
    onFactoryResetDone: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var page by remember { mutableStateOf(SettingsPage.Hub) }
    var resetStep by remember { mutableStateOf(0) }

    LaunchedEffect(state.overlayEnabled) {
        val runningIntent = Intent(context, com.miguelthemann.remarkable.overlay.ClockOverlayService::class.java)
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

    val title = when (page) {
        SettingsPage.Hub -> stringResource(R.string.settings)
        SettingsPage.Appearance -> stringResource(R.string.settings_appearance)
        SettingsPage.Clock -> stringResource(R.string.settings_clock)
        SettingsPage.Music -> stringResource(R.string.settings_music)
        SettingsPage.Weather -> stringResource(R.string.settings_weather)
        SettingsPage.BurnIn -> stringResource(R.string.settings_burn_in)
        SettingsPage.System -> stringResource(R.string.settings_system)
        SettingsPage.About -> stringResource(R.string.settings_about)
    }

    val navigateBack = {
        if (page == SettingsPage.Hub) onBack() else page = SettingsPage.Hub
    }

    BackHandlerCompat(enabled = true, onBack = navigateBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            when (page) {
                SettingsPage.Hub -> SettingsHub(state = state, onOpen = { page = it })
                SettingsPage.Appearance -> AppearanceSettings(state, viewModel)
                SettingsPage.Clock -> ClockSettings(state, viewModel, context)
                SettingsPage.Music -> MusicSettings(state, viewModel, context)
                SettingsPage.Weather -> WeatherSettings(state, viewModel)
                SettingsPage.BurnIn -> BurnInSettings(state, viewModel)
                SettingsPage.System -> SystemSettings(state, viewModel, context)
                SettingsPage.About -> AboutSettings(
                    onReset = { resetStep = 1 },
                    onGitHub = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/miguelthemann/remarkable")),
                        )
                    },
                )
            }
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
                context.stopService(Intent(context, com.miguelthemann.remarkable.overlay.ClockOverlayService::class.java))
                viewModel.resetEverything {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.settings_reset_success),
                        android.widget.Toast.LENGTH_LONG,
                    ).show()
                    onFactoryResetDone()
                }
            },
            onDismiss = { resetStep = 0 },
        )
    }
}

@Composable
private fun BackHandlerCompat(enabled: Boolean, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)
}

@Composable
private fun SettingsHub(
    state: ClockUiState,
    onOpen: (SettingsPage) -> Unit,
) {
    Column {
        SettingsNavRow(
            title = stringResource(R.string.settings_appearance),
            subtitle = "${settingsThemeLabel(state.themeMode)} · ${settingsBackgroundLabel(state.backgroundMode)}",
            onClick = { onOpen(SettingsPage.Appearance) },
        )
        SettingsNavRow(
            title = stringResource(R.string.settings_clock),
            subtitle = "${settingsClockStyleLabel(state.clockStyle)} · ${settingsWidgetsSummary(state)}",
            onClick = { onOpen(SettingsPage.Clock) },
        )
        SettingsNavRow(
            title = stringResource(R.string.settings_music),
            subtitle = settingsMusicSourceLabel(state.musicSource),
            onClick = { onOpen(SettingsPage.Music) },
        )
        SettingsNavRow(
            title = stringResource(R.string.settings_weather),
            subtitle = state.city.ifBlank { stringResource(R.string.settings_weather_gps) },
            onClick = { onOpen(SettingsPage.Weather) },
        )
        SettingsNavRow(
            title = stringResource(R.string.settings_burn_in),
            subtitle = if (state.burnInProtection) {
                stringResource(R.string.settings_burn_in_on)
            } else {
                stringResource(R.string.settings_burn_in_off)
            },
            onClick = { onOpen(SettingsPage.BurnIn) },
        )
        SettingsNavRow(
            title = stringResource(R.string.settings_system),
            onClick = { onOpen(SettingsPage.System) },
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SettingsNavRow(
            title = stringResource(R.string.settings_about),
            onClick = { onOpen(SettingsPage.About) },
        )
    }
}

@Composable
private fun AppearanceSettings(state: ClockUiState, viewModel: ClockViewModel) {
    var themeDialog by remember { mutableStateOf(false) }
    var bgDialog by remember { mutableStateOf(false) }
    var clockDialog by remember { mutableStateOf(false) }
    var accentPicker by remember { mutableStateOf(false) }
    var bgPicker by remember { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.importBackgroundImage(uri)
    }

    Column {
        SettingsNavRow(
            title = stringResource(R.string.settings_theme_mode),
            subtitle = settingsThemeLabel(state.themeMode),
            onClick = { themeDialog = true },
        )
        if (state.themeMode == ThemeMode.MONET) {
            SettingsHint(stringResource(R.string.settings_monet_accent_help))
        } else {
            SettingsSectionLabel(stringResource(R.string.settings_accent))
            ColorSwatchRow(
                selectedArgb = state.accentArgb,
                presets = AccentPreset.entries.map { it.argb },
                onSelect = viewModel::setAccentArgb,
            )
            ColorPickerNavRow(
                title = stringResource(R.string.settings_accent_custom),
                argb = state.accentArgb,
                onClick = { accentPicker = true },
            )
        }
        SettingsNavRow(
            title = stringResource(R.string.settings_background),
            subtitle = settingsBackgroundLabel(state.backgroundMode),
            onClick = { bgDialog = true },
        )
        if (state.backgroundMode == BackgroundMode.CUSTOM_COLOR) {
            SettingsSectionLabel(stringResource(R.string.settings_custom_bg_color))
            ColorSwatchRow(
                selectedArgb = state.customBgArgb,
                presets = CustomBgPreset.entries.map { it.argb },
                onSelect = viewModel::setCustomBgArgb,
            )
            ColorPickerNavRow(
                title = stringResource(R.string.settings_custom_bg_color),
                argb = state.customBgArgb,
                onClick = { bgPicker = true },
            )
        }
        if (state.backgroundMode == BackgroundMode.IMAGE) {
            SettingsHint(stringResource(R.string.settings_background_image_help))
            OutlinedButton(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(stringResource(R.string.settings_pick_image))
            }
            if (state.backgroundImagePath.isNotBlank()) {
                TextButton(onClick = viewModel::clearBackgroundImage) {
                    Text(stringResource(R.string.settings_clear_image))
                }
            }
        }
        SettingsNavRow(
            title = stringResource(R.string.settings_clock_style),
            subtitle = settingsClockStyleLabel(state.clockStyle),
            onClick = { clockDialog = true },
        )
    }

    if (themeDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_theme_mode),
            options = listOf(
                ThemeMode.MONET,
                ThemeMode.SYSTEM,
                ThemeMode.LIGHT,
                ThemeMode.DARK,
            ),
            selected = state.themeMode,
            optionLabel = { settingsThemeLabel(it) },
            onSelect = viewModel::setThemeMode,
            onDismiss = { themeDialog = false },
        )
    }
    if (bgDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_background),
            options = BackgroundMode.entries,
            selected = state.backgroundMode,
            optionLabel = { settingsBackgroundLabel(it) },
            onSelect = viewModel::setBackgroundMode,
            onDismiss = { bgDialog = false },
        )
    }
    if (clockDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_clock_style),
            options = ClockStyle.entries,
            selected = state.clockStyle,
            optionLabel = { settingsClockStyleLabel(it) },
            onSelect = viewModel::setClockStyle,
            onDismiss = { clockDialog = false },
        )
    }
    if (accentPicker) {
        ColorPickerSheet(
            argb = state.accentArgb,
            onColorChange = viewModel::setAccentArgb,
            onDismiss = { accentPicker = false },
        )
    }
    if (bgPicker) {
        ColorPickerSheet(
            argb = state.customBgArgb,
            onColorChange = viewModel::setCustomBgArgb,
            onDismiss = { bgPicker = false },
        )
    }
}

@Composable
private fun ClockSettings(state: ClockUiState, viewModel: ClockViewModel, context: android.content.Context) {
    Column {
        SettingsSectionLabel(stringResource(R.string.settings_clock))
        SettingsToggleRow(stringResource(R.string.settings_use_24h), state.use24Hour, viewModel::setUse24Hour)
        SettingsToggleRow(stringResource(R.string.settings_show_seconds), state.showSeconds, viewModel::setShowSeconds)
        SettingsToggleRow(stringResource(R.string.settings_keep_awake), state.keepAwake, viewModel::setKeepAwake)
        SettingsToggleRow(stringResource(R.string.settings_night_dim), state.nightDim, viewModel::setNightDim)

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SettingsSectionLabel(stringResource(R.string.settings_widgets))
        SettingsToggleRow(stringResource(R.string.settings_show_time), state.showTime) { enabled ->
            viewModel.setShowTime(enabled)
            warnIfNoWidgets(context, enabled, state.showDate, state.showWeather, state.showSpotify)
        }
        SettingsToggleRow(stringResource(R.string.settings_show_date), state.showDate) { enabled ->
            viewModel.setShowDate(enabled)
            warnIfNoWidgets(context, state.showTime, enabled, state.showWeather, state.showSpotify)
        }
        SettingsToggleRow(stringResource(R.string.settings_show_weather_mod), state.showWeather) { enabled ->
            viewModel.setShowWeather(enabled)
            warnIfNoWidgets(context, state.showTime, state.showDate, enabled, state.showSpotify)
        }
        SettingsToggleRow(stringResource(R.string.settings_show_spotify_mod), state.showSpotify) { enabled ->
            viewModel.setShowSpotify(enabled)
            warnIfNoWidgets(context, state.showTime, state.showDate, state.showWeather, enabled)
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SettingsSectionLabel(stringResource(R.string.settings_modules))
        SettingsHint(stringResource(R.string.settings_modules_drag_help))
        TextButton(onClick = viewModel::resetModules, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(stringResource(R.string.settings_reset_modules))
        }
    }
}

@Composable
private fun MusicSettings(state: ClockUiState, viewModel: ClockViewModel, context: android.content.Context) {
    var styleDialog by remember { mutableStateOf(false) }
    var sourceDialog by remember { mutableStateOf(false) }
    var clientDraft by remember { mutableStateOf(state.spotifyClientId) }
    var clientDirty by remember { mutableStateOf(false) }
    var lastFmPassword by remember { mutableStateOf("") }

    LaunchedEffect(state.spotifyClientId) {
        if (!clientDirty) clientDraft = state.spotifyClientId
    }
    LaunchedEffect(clientDraft) {
        delay(600)
        if (clientDirty && clientDraft != state.spotifyClientId) {
            viewModel.setSpotifyClientId(clientDraft)
        }
    }

    Column {
        SettingsNavRow(
            title = stringResource(R.string.settings_spotify_style),
            subtitle = settingsSpotifyStyleLabel(state.spotifyStyle),
            onClick = { styleDialog = true },
        )
        SettingsNavRow(
            title = stringResource(R.string.settings_music_source),
            subtitle = settingsMusicSourceLabel(state.musicSource),
            onClick = { sourceDialog = true },
        )

        when (state.musicSource) {
            MusicSource.SYSTEM -> {
                SettingsHint(stringResource(R.string.music_need_notification_access))
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }) {
                    Text(stringResource(R.string.music_open_notification_access))
                }
            }
            MusicSource.SPOTIFY -> {
                OutlinedTextField(
                    value = clientDraft,
                    onValueChange = { clientDirty = true; clientDraft = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    label = { Text(stringResource(R.string.settings_client_id)) },
                    singleLine = true,
                )
                SettingsHint(stringResource(R.string.settings_client_id_help))
            }
            MusicSource.LASTFM -> {
                SettingsHint(stringResource(R.string.settings_lastfm_help))
                OutlinedTextField(
                    value = state.lastFmApiKey,
                    onValueChange = viewModel::setLastFmApiKey,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    label = { Text(stringResource(R.string.settings_lastfm_api_key)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.lastFmSharedSecret,
                    onValueChange = viewModel::setLastFmSharedSecret,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    label = { Text(stringResource(R.string.settings_lastfm_shared_secret)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.lastFmUsername,
                    onValueChange = viewModel::setLastFmUsername,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    label = { Text(stringResource(R.string.settings_lastfm_username)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = lastFmPassword,
                    onValueChange = { lastFmPassword = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    label = { Text(stringResource(R.string.settings_lastfm_password)) },
                    singleLine = true,
                )
                Button(
                    onClick = { viewModel.loginLastFm(lastFmPassword); lastFmPassword = "" },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    enabled = state.lastFmApiKey.isNotBlank() && state.lastFmSharedSecret.isNotBlank() &&
                        state.lastFmUsername.isNotBlank() && lastFmPassword.isNotBlank(),
                ) {
                    Text(stringResource(R.string.settings_lastfm_login))
                }
                if (state.lastFmSessionKey.isNotBlank()) {
                    SettingsHint(stringResource(R.string.settings_lastfm_signed_in))
                }
                if (state.lastFmLoginMessage != null && state.lastFmLoginMessage != "ok") {
                    Text(
                        state.lastFmLoginMessage.orEmpty(),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SettingsSectionLabel(stringResource(R.string.settings_spotify_module))
        SettingsToggleRow(stringResource(R.string.settings_show_spotify_icon), state.showSpotifyIcon, viewModel::setShowSpotifyIcon)
        SettingsToggleRow(stringResource(R.string.settings_generic_music_icon), state.useGenericMusicIcon, viewModel::setUseGenericMusicIcon)
        SettingsToggleRow(stringResource(R.string.settings_show_track_title), state.showTrackTitle, viewModel::setShowTrackTitle)
        SettingsToggleRow(stringResource(R.string.settings_show_artist), state.showArtist, viewModel::setShowArtist)
        SettingsToggleRow(stringResource(R.string.settings_show_album), state.showAlbum, viewModel::setShowAlbum)
        SettingsToggleRow(stringResource(R.string.settings_show_release_year), state.showReleaseYear, viewModel::setShowReleaseYear)
    }

    if (styleDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_spotify_style),
            options = SpotifyModuleStyle.entries,
            selected = state.spotifyStyle,
            optionLabel = { settingsSpotifyStyleLabel(it) },
            onSelect = viewModel::setSpotifyStyle,
            onDismiss = { styleDialog = false },
        )
    }
    if (sourceDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_music_source),
            options = MusicSource.entries,
            selected = state.musicSource,
            optionLabel = { settingsMusicSourceLabel(it) },
            onSelect = viewModel::setMusicSource,
            onDismiss = { sourceDialog = false },
        )
    }
}

@Composable
private fun WeatherSettings(state: ClockUiState, viewModel: ClockViewModel) {
    var fxDialog by remember { mutableStateOf(false) }
    var cityDraft by remember { mutableStateOf(state.city) }
    var cityDirty by remember { mutableStateOf(false) }

    LaunchedEffect(state.city) {
        if (!cityDirty) cityDraft = state.city
    }
    LaunchedEffect(cityDraft) {
        delay(600)
        if (cityDirty && cityDraft != state.city) viewModel.setCity(cityDraft)
    }

    Column {
        SettingsToggleRow(stringResource(R.string.settings_use_celsius), state.useCelsius, viewModel::setUseCelsius)
        OutlinedTextField(
            value = cityDraft,
            onValueChange = { cityDirty = true; cityDraft = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            label = { Text(stringResource(R.string.settings_city)) },
            placeholder = { Text(stringResource(R.string.settings_city_placeholder)) },
            singleLine = true,
        )
        SettingsNavRow(
            title = stringResource(R.string.settings_weather_effect),
            subtitle = settingsWeatherEffectLabel(state.weatherEffect),
            onClick = { fxDialog = true },
        )
        SettingsHint(stringResource(R.string.settings_weather_effect_help))
    }

    if (fxDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_weather_effect),
            options = WeatherEffect.entries,
            selected = state.weatherEffect,
            optionLabel = { settingsWeatherEffectLabel(it) },
            onSelect = viewModel::setWeatherEffect,
            onDismiss = { fxDialog = false },
        )
    }
}

@Composable
private fun BurnInSettings(state: ClockUiState, viewModel: ClockViewModel) {
    Column {
        SettingsToggleRow(stringResource(R.string.settings_burn_in_enable), state.burnInProtection, viewModel::setBurnInProtection)
        Text(stringResource(R.string.settings_burn_in_shift, state.burnInShiftDp), modifier = Modifier.padding(horizontal = 16.dp))
        Slider(
            value = state.burnInShiftDp.toFloat(),
            onValueChange = { viewModel.setBurnInShiftDp(it.toInt()) },
            valueRange = 2f..24f,
            steps = 21,
            modifier = Modifier.padding(horizontal = 16.dp),
            enabled = state.burnInProtection,
        )
        Text(stringResource(R.string.settings_burn_in_interval, state.burnInIntervalSec), modifier = Modifier.padding(horizontal = 16.dp))
        Slider(
            value = state.burnInIntervalSec.toFloat(),
            onValueChange = { viewModel.setBurnInIntervalSec(it.toInt()) },
            valueRange = 15f..300f,
            steps = 18,
            modifier = Modifier.padding(horizontal = 16.dp),
            enabled = state.burnInProtection,
        )
        SettingsToggleRow(stringResource(R.string.settings_smart_pixels), state.smartPixels, viewModel::setSmartPixels)
        SettingsHint(stringResource(R.string.settings_smart_pixels_help))
        Text(
            stringResource(R.string.settings_smart_pixels_strength, (state.smartPixelsStrength * 100).toInt()),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Slider(
            value = state.smartPixelsStrength,
            onValueChange = viewModel::setSmartPixelsStrength,
            valueRange = 0.1f..0.8f,
            modifier = Modifier.padding(horizontal = 16.dp),
            enabled = state.burnInProtection && state.smartPixels,
        )
    }
}

@Composable
private fun SystemSettings(state: ClockUiState, viewModel: ClockViewModel, context: android.content.Context) {
    Column {
        SettingsHint(stringResource(R.string.settings_launcher_help))
        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }) {
            Text(stringResource(R.string.settings_open_home))
        }
        SettingsHint(stringResource(R.string.settings_screensaver_help))
        TextButton(onClick = {
            runCatching { context.startActivity(Intent("android.settings.DREAM_SETTINGS")) }
                .onFailure { context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)) }
        }) {
            Text(stringResource(R.string.settings_open_screensaver))
        }
        SettingsToggleRow(stringResource(R.string.settings_overlay), state.overlayEnabled) { enabled ->
            if (enabled && !Settings.canDrawOverlays(context)) {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
                )
                viewModel.setOverlayEnabled(false)
            } else {
                viewModel.setOverlayEnabled(enabled)
            }
        }
        SettingsHint(stringResource(R.string.settings_overlay_help))
    }
}

@Composable
private fun AboutSettings(onReset: () -> Unit, onGitHub: () -> Unit) {
    Column {
        SettingsHint(stringResource(R.string.settings_license))
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        SettingsSectionLabel(stringResource(R.string.settings_danger_zone))
        SettingsHint(stringResource(R.string.settings_reset_all_help))
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.error,
            ),
        ) {
            Text(stringResource(R.string.settings_reset_all))
        }
        TextButton(onClick = onGitHub, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_github))
        }
    }
}

@Composable private fun settingsThemeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
        ThemeMode.MONET -> R.string.theme_monet
    },
)

@Composable private fun settingsBackgroundLabel(mode: BackgroundMode): String = stringResource(
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

@Composable private fun settingsClockStyleLabel(style: ClockStyle): String = stringResource(
    when (style) {
        ClockStyle.ANALOG -> R.string.clock_analog
        ClockStyle.DIGITAL -> R.string.clock_digital
        ClockStyle.BOTH -> R.string.clock_both
    },
)

@Composable private fun settingsSpotifyStyleLabel(style: SpotifyModuleStyle): String = stringResource(
    when (style) {
        SpotifyModuleStyle.ONE_LINER -> R.string.spotify_style_oneliner
        SpotifyModuleStyle.CARD -> R.string.spotify_style_card
        SpotifyModuleStyle.WIDGET -> R.string.spotify_style_widget
    },
)

@Composable private fun settingsMusicSourceLabel(source: MusicSource): String = stringResource(
    when (source) {
        MusicSource.SYSTEM -> R.string.music_source_system
        MusicSource.SPOTIFY -> R.string.music_source_spotify
        MusicSource.LASTFM -> R.string.music_source_lastfm
    },
)

@Composable private fun settingsWeatherEffectLabel(effect: WeatherEffect): String = stringResource(
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

@Composable
private fun settingsWidgetsSummary(state: ClockUiState): String {
    val on = buildList {
        if (state.showTime) add(stringResource(R.string.settings_show_time))
        if (state.showDate) add(stringResource(R.string.settings_show_date))
        if (state.showWeather) add(stringResource(R.string.settings_show_weather_mod))
        if (state.showSpotify) add(stringResource(R.string.settings_show_spotify_mod))
    }
    return on.joinToString(" · ").ifBlank { "—" }
}
