/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "remarkable")

enum class ThemeMode { SYSTEM, LIGHT, DARK, MONET }

enum class BackgroundMode { SOLID, MONET, WEATHER, TIME_OF_DAY, CUSTOM, CUSTOM_COLOR, IMAGE }

enum class ClockStyle { ANALOG, DIGITAL, BOTH }

enum class AccentPreset(val argb: Long) {
    TEAL(0xFF006A6A),
    BLUE(0xFF1565C0),
    INDIGO(0xFF3949AB),
    GREEN(0xFF2E7D32),
    AMBER(0xFFF9A825),
    ROSE(0xFFC2185B),
    ORANGE(0xFFEF6C00),
    SLATE(0xFF546E7A),
}

/** Neutral “normal” colours for custom backgrounds; remapped for light/dark at draw time. */
enum class CustomBgPreset(val argb: Long) {
    WHITE(0xFFF5F5F5),
    CREAM(0xFFF7F1E5),
    SAND(0xFFE8DCC8),
    SAGE(0xFFD5DDD0),
    SKY(0xFFD7E6F2),
    LAVENDER(0xFFE4DFF0),
    ROSE(0xFFF3DDE3),
    SLATE(0xFFCFD8DC),
    CHARCOAL(0xFF455A64),
}

data class ModuleOffsets(
    val timeX: Float = 0f,
    val timeY: Float = 0f,
    val dateX: Float = 0f,
    val dateY: Float = 0f,
    val weatherX: Float = 0f,
    val weatherY: Float = 0f,
    val spotifyX: Float = 0f,
    val spotifyY: Float = 0f,
)

data class UserSettings(
    val use24Hour: Boolean = true,
    val showSeconds: Boolean = true,
    val keepAwake: Boolean = true,
    val nightDim: Boolean = false,
    val useCelsius: Boolean = true,
    val city: String = "",
    val spotifyClientId: String = "",
    val themeMode: ThemeMode = ThemeMode.MONET,
    val backgroundMode: BackgroundMode = BackgroundMode.MONET,
    val clockStyle: ClockStyle = ClockStyle.BOTH,
    val accentArgb: Long = AccentPreset.TEAL.argb,
    val customBgArgb: Long = CustomBgPreset.CREAM.argb,
    val backgroundImagePath: String = "",
    val showDate: Boolean = true,
    val showWeather: Boolean = true,
    val showSpotify: Boolean = true,
    val modules: ModuleOffsets = ModuleOffsets(),
    val burnInProtection: Boolean = true,
    val burnInShiftDp: Int = 8,
    val burnInIntervalSec: Int = 60,
    val smartPixels: Boolean = false,
    val smartPixelsStrength: Float = 0.35f,
    val overlayEnabled: Boolean = false,
)

class UserPreferences(private val context: Context) {
    private val use24Hour = booleanPreferencesKey("use_24h")
    private val showSeconds = booleanPreferencesKey("show_seconds")
    private val keepAwake = booleanPreferencesKey("keep_awake")
    private val nightDim = booleanPreferencesKey("night_dim")
    private val useCelsius = booleanPreferencesKey("use_celsius")
    private val city = stringPreferencesKey("city")
    private val spotifyClientId = stringPreferencesKey("spotify_client_id")
    private val themeMode = stringPreferencesKey("theme_mode")
    private val backgroundMode = stringPreferencesKey("background_mode")
    private val clockStyle = stringPreferencesKey("clock_style")
    private val accentArgb = stringPreferencesKey("accent_argb")
    private val customBgArgb = stringPreferencesKey("custom_bg_argb")
    private val backgroundImagePath = stringPreferencesKey("background_image_path")
    private val showDate = booleanPreferencesKey("show_date")
    private val showWeather = booleanPreferencesKey("show_weather")
    private val showSpotify = booleanPreferencesKey("show_spotify")
    private val timeX = floatPreferencesKey("mod_time_x")
    private val timeY = floatPreferencesKey("mod_time_y")
    private val dateX = floatPreferencesKey("mod_date_x")
    private val dateY = floatPreferencesKey("mod_date_y")
    private val weatherX = floatPreferencesKey("mod_weather_x")
    private val weatherY = floatPreferencesKey("mod_weather_y")
    private val spotifyX = floatPreferencesKey("mod_spotify_x")
    private val spotifyY = floatPreferencesKey("mod_spotify_y")
    private val burnInProtection = booleanPreferencesKey("burn_in")
    private val burnInShiftDp = intPreferencesKey("burn_in_shift_dp")
    private val burnInIntervalSec = intPreferencesKey("burn_in_interval_sec")
    private val smartPixels = booleanPreferencesKey("smart_pixels")
    private val smartPixelsStrength = floatPreferencesKey("smart_pixels_strength")
    private val overlayEnabled = booleanPreferencesKey("overlay_enabled")

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            use24Hour = prefs[use24Hour] ?: true,
            showSeconds = prefs[showSeconds] ?: true,
            keepAwake = prefs[keepAwake] ?: true,
            nightDim = prefs[nightDim] ?: false,
            useCelsius = prefs[useCelsius] ?: true,
            city = prefs[city].orEmpty(),
            spotifyClientId = prefs[spotifyClientId].orEmpty(),
            themeMode = prefs[themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.MONET,
            backgroundMode = prefs[backgroundMode]?.let {
                runCatching { BackgroundMode.valueOf(it) }.getOrNull()
            } ?: BackgroundMode.MONET,
            clockStyle = prefs[clockStyle]?.let { runCatching { ClockStyle.valueOf(it) }.getOrNull() }
                ?: ClockStyle.BOTH,
            accentArgb = prefs[accentArgb]?.toLongOrNull() ?: AccentPreset.TEAL.argb,
            customBgArgb = prefs[customBgArgb]?.toLongOrNull() ?: CustomBgPreset.CREAM.argb,
            backgroundImagePath = prefs[backgroundImagePath].orEmpty(),
            showDate = prefs[showDate] ?: true,
            showWeather = prefs[showWeather] ?: true,
            showSpotify = prefs[showSpotify] ?: true,
            modules = ModuleOffsets(
                timeX = prefs[timeX] ?: 0f,
                timeY = prefs[timeY] ?: 0f,
                dateX = prefs[dateX] ?: 0f,
                dateY = prefs[dateY] ?: 0f,
                weatherX = prefs[weatherX] ?: 0f,
                weatherY = prefs[weatherY] ?: 0f,
                spotifyX = prefs[spotifyX] ?: 0f,
                spotifyY = prefs[spotifyY] ?: 0f,
            ),
            burnInProtection = prefs[burnInProtection] ?: true,
            burnInShiftDp = prefs[burnInShiftDp] ?: 8,
            burnInIntervalSec = prefs[burnInIntervalSec] ?: 60,
            smartPixels = prefs[smartPixels] ?: false,
            smartPixelsStrength = prefs[smartPixelsStrength] ?: 0.35f,
            overlayEnabled = prefs[overlayEnabled] ?: false,
        )
    }

    suspend fun setUse24Hour(value: Boolean) = context.dataStore.edit { it[use24Hour] = value }
    suspend fun setShowSeconds(value: Boolean) = context.dataStore.edit { it[showSeconds] = value }
    suspend fun setKeepAwake(value: Boolean) = context.dataStore.edit { it[keepAwake] = value }
    suspend fun setNightDim(value: Boolean) = context.dataStore.edit { it[nightDim] = value }
    suspend fun setUseCelsius(value: Boolean) = context.dataStore.edit { it[useCelsius] = value }
    suspend fun setCity(value: String) = context.dataStore.edit { it[city] = value }
    suspend fun setSpotifyClientId(value: String) =
        context.dataStore.edit { it[spotifyClientId] = value.trim() }
    suspend fun setThemeMode(value: ThemeMode) =
        context.dataStore.edit { it[themeMode] = value.name }
    suspend fun setBackgroundMode(value: BackgroundMode) =
        context.dataStore.edit { it[backgroundMode] = value.name }
    suspend fun setClockStyle(value: ClockStyle) =
        context.dataStore.edit { it[clockStyle] = value.name }
    suspend fun setAccentArgb(value: Long) =
        context.dataStore.edit { it[accentArgb] = value.toString() }
    suspend fun setCustomBgArgb(value: Long) =
        context.dataStore.edit { it[customBgArgb] = value.toString() }
    suspend fun setBackgroundImagePath(value: String) =
        context.dataStore.edit { it[backgroundImagePath] = value }
    suspend fun setShowDate(value: Boolean) = context.dataStore.edit { it[showDate] = value }
    suspend fun setShowWeather(value: Boolean) = context.dataStore.edit { it[showWeather] = value }
    suspend fun setShowSpotify(value: Boolean) = context.dataStore.edit { it[showSpotify] = value }
    suspend fun setModuleOffsets(modules: ModuleOffsets) = context.dataStore.edit {
        it[timeX] = modules.timeX
        it[timeY] = modules.timeY
        it[dateX] = modules.dateX
        it[dateY] = modules.dateY
        it[weatherX] = modules.weatherX
        it[weatherY] = modules.weatherY
        it[spotifyX] = modules.spotifyX
        it[spotifyY] = modules.spotifyY
    }
    suspend fun setBurnInProtection(value: Boolean) =
        context.dataStore.edit { it[burnInProtection] = value }
    suspend fun setBurnInShiftDp(value: Int) =
        context.dataStore.edit { it[burnInShiftDp] = value.coerceIn(2, 24) }
    suspend fun setBurnInIntervalSec(value: Int) =
        context.dataStore.edit { it[burnInIntervalSec] = value.coerceIn(15, 300) }
    suspend fun setSmartPixels(value: Boolean) =
        context.dataStore.edit { it[smartPixels] = value }
    suspend fun setSmartPixelsStrength(value: Float) =
        context.dataStore.edit { it[smartPixelsStrength] = value.coerceIn(0.1f, 0.8f) }
    suspend fun setOverlayEnabled(value: Boolean) =
        context.dataStore.edit { it[overlayEnabled] = value }
}
