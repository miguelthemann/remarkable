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
import com.miguelthemann.remarkable.media.MusicSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.abs

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "remarkable")

enum class ThemeMode { SYSTEM, LIGHT, DARK, MONET }

enum class BackgroundMode { SOLID, MONET, WEATHER, TIME_OF_DAY, CUSTOM, CUSTOM_COLOR, IMAGE }

enum class ClockStyle { ANALOG, DIGITAL, BOTH }

enum class SpotifyModuleStyle { ONE_LINER, CARD, WIDGET }

enum class WeatherEffect {
    AUTO, CLEAR, CLOUDY, RAIN, SNOW, THUNDER, FOG, NONE,
}

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

/**
 * Module centres as fractions of the screen (0–1).
 * I don't know why we used fractions instead of dp either, but it survives rotation so gg.
 */
data class ModuleOffsets(
    val timeX: Float = 0.5f,
    val timeY: Float = 0.30f,
    val dateX: Float = 0.5f,
    val dateY: Float = 0.46f,
    val weatherX: Float = 0.5f,
    val weatherY: Float = 0.66f,
    val spotifyX: Float = 0.5f,
    val spotifyY: Float = 0.84f,
) {
    companion object {
        fun fromStored(
            timeX: Float?, timeY: Float?,
            dateX: Float?, dateY: Float?,
            weatherX: Float?, weatherY: Float?,
            spotifyX: Float?, spotifyY: Float?,
        ): ModuleOffsets {
            val defaults = ModuleOffsets()
            fun frac(v: Float?, d: Float): Float {
                if (v == null) return d
                // Legacy dp-style offsets were roughly -80..80
                return if (abs(v) > 1.5f) d else v.coerceIn(0.05f, 0.95f)
            }
            return ModuleOffsets(
                timeX = frac(timeX, defaults.timeX),
                timeY = frac(timeY, defaults.timeY),
                dateX = frac(dateX, defaults.dateX),
                dateY = frac(dateY, defaults.dateY),
                weatherX = frac(weatherX, defaults.weatherX),
                weatherY = frac(weatherY, defaults.weatherY),
                spotifyX = frac(spotifyX, defaults.spotifyX),
                spotifyY = frac(spotifyY, defaults.spotifyY),
            )
        }
    }
}

data class UserSettings(
    val onboardingDone: Boolean = false,
    val use24Hour: Boolean = true,
    val showSeconds: Boolean = true,
    val keepAwake: Boolean = true,
    val nightDim: Boolean = false,
    val useCelsius: Boolean = true,
    val city: String = "",
    val spotifyClientId: String = "",
    val themeMode: ThemeMode = ThemeMode.MONET,
    val backgroundMode: BackgroundMode = BackgroundMode.WEATHER,
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
    val spotifyStyle: SpotifyModuleStyle = SpotifyModuleStyle.WIDGET,
    val showSpotifyIcon: Boolean = true,
    val useGenericMusicIcon: Boolean = false,
    val showTrackTitle: Boolean = true,
    val showArtist: Boolean = true,
    val showAlbum: Boolean = true,
    val showReleaseYear: Boolean = false,
    val weatherEffect: WeatherEffect = WeatherEffect.AUTO,
    val musicSource: MusicSource = MusicSource.SYSTEM,
    val lastFmApiKey: String = "",
    val lastFmSharedSecret: String = "",
    val lastFmUsername: String = "",
    val lastFmSessionKey: String = "",
    val lastFmScrobble: Boolean = false,
)

class UserPreferences(private val context: Context) {
    private val onboardingDone = booleanPreferencesKey("onboarding_done")
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
    private val spotifyStyle = stringPreferencesKey("spotify_style")
    private val showSpotifyIcon = booleanPreferencesKey("show_spotify_icon")
    private val useGenericMusicIcon = booleanPreferencesKey("generic_music_icon")
    private val showTrackTitle = booleanPreferencesKey("show_track_title")
    private val showArtist = booleanPreferencesKey("show_artist")
    private val showAlbum = booleanPreferencesKey("show_album")
    private val showReleaseYear = booleanPreferencesKey("show_release_year")
    private val weatherEffect = stringPreferencesKey("weather_effect")
    private val musicSource = stringPreferencesKey("music_source")
    private val lastFmApiKey = stringPreferencesKey("lastfm_api_key")
    private val lastFmSharedSecret = stringPreferencesKey("lastfm_shared_secret")
    private val lastFmUsername = stringPreferencesKey("lastfm_username")
    private val lastFmSessionKey = stringPreferencesKey("lastfm_session_key")
    private val lastFmScrobble = booleanPreferencesKey("lastfm_scrobble")

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            onboardingDone = prefs[onboardingDone] ?: false,
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
            } ?: BackgroundMode.WEATHER,
            clockStyle = prefs[clockStyle]?.let { runCatching { ClockStyle.valueOf(it) }.getOrNull() }
                ?: ClockStyle.BOTH,
            accentArgb = prefs[accentArgb]?.toLongOrNull() ?: AccentPreset.TEAL.argb,
            customBgArgb = prefs[customBgArgb]?.toLongOrNull() ?: CustomBgPreset.CREAM.argb,
            backgroundImagePath = prefs[backgroundImagePath].orEmpty(),
            showDate = prefs[showDate] ?: true,
            showWeather = prefs[showWeather] ?: true,
            showSpotify = prefs[showSpotify] ?: true,
            modules = ModuleOffsets.fromStored(
                prefs[timeX], prefs[timeY],
                prefs[dateX], prefs[dateY],
                prefs[weatherX], prefs[weatherY],
                prefs[spotifyX], prefs[spotifyY],
            ),
            burnInProtection = prefs[burnInProtection] ?: true,
            burnInShiftDp = prefs[burnInShiftDp] ?: 8,
            burnInIntervalSec = prefs[burnInIntervalSec] ?: 60,
            smartPixels = prefs[smartPixels] ?: false,
            smartPixelsStrength = prefs[smartPixelsStrength] ?: 0.35f,
            overlayEnabled = prefs[overlayEnabled] ?: false,
            spotifyStyle = prefs[spotifyStyle]?.let {
                runCatching { SpotifyModuleStyle.valueOf(it) }.getOrNull()
            } ?: SpotifyModuleStyle.WIDGET,
            showSpotifyIcon = prefs[showSpotifyIcon] ?: true,
            useGenericMusicIcon = prefs[useGenericMusicIcon] ?: false,
            showTrackTitle = prefs[showTrackTitle] ?: true,
            showArtist = prefs[showArtist] ?: true,
            showAlbum = prefs[showAlbum] ?: true,
            showReleaseYear = prefs[showReleaseYear] ?: false,
            weatherEffect = prefs[weatherEffect]?.let {
                runCatching { WeatherEffect.valueOf(it) }.getOrNull()
            } ?: WeatherEffect.AUTO,
            musicSource = prefs[musicSource]?.let {
                runCatching { MusicSource.valueOf(it) }.getOrNull()
            } ?: MusicSource.SYSTEM,
            lastFmApiKey = prefs[lastFmApiKey].orEmpty(),
            lastFmSharedSecret = prefs[lastFmSharedSecret].orEmpty(),
            lastFmUsername = prefs[lastFmUsername].orEmpty(),
            lastFmSessionKey = prefs[lastFmSessionKey].orEmpty(),
            lastFmScrobble = prefs[lastFmScrobble] ?: false,
        )
    }

    suspend fun setOnboardingDone(value: Boolean) =
        context.dataStore.edit { it[onboardingDone] = value }
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
        it[timeX] = modules.timeX.coerceIn(0.05f, 0.95f)
        it[timeY] = modules.timeY.coerceIn(0.05f, 0.95f)
        it[dateX] = modules.dateX.coerceIn(0.05f, 0.95f)
        it[dateY] = modules.dateY.coerceIn(0.05f, 0.95f)
        it[weatherX] = modules.weatherX.coerceIn(0.05f, 0.95f)
        it[weatherY] = modules.weatherY.coerceIn(0.05f, 0.95f)
        it[spotifyX] = modules.spotifyX.coerceIn(0.05f, 0.95f)
        it[spotifyY] = modules.spotifyY.coerceIn(0.05f, 0.95f)
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
    suspend fun setSpotifyStyle(value: SpotifyModuleStyle) =
        context.dataStore.edit { it[spotifyStyle] = value.name }
    suspend fun setShowSpotifyIcon(value: Boolean) =
        context.dataStore.edit { it[showSpotifyIcon] = value }
    suspend fun setUseGenericMusicIcon(value: Boolean) =
        context.dataStore.edit { it[useGenericMusicIcon] = value }
    suspend fun setShowTrackTitle(value: Boolean) =
        context.dataStore.edit { it[showTrackTitle] = value }
    suspend fun setShowArtist(value: Boolean) =
        context.dataStore.edit { it[showArtist] = value }
    suspend fun setShowAlbum(value: Boolean) =
        context.dataStore.edit { it[showAlbum] = value }
    suspend fun setShowReleaseYear(value: Boolean) =
        context.dataStore.edit { it[showReleaseYear] = value }
    suspend fun setWeatherEffect(value: WeatherEffect) =
        context.dataStore.edit { it[weatherEffect] = value.name }
    suspend fun setMusicSource(value: MusicSource) =
        context.dataStore.edit { it[musicSource] = value.name }
    suspend fun setLastFmApiKey(value: String) =
        context.dataStore.edit { it[lastFmApiKey] = value.trim() }
    suspend fun setLastFmSharedSecret(value: String) =
        context.dataStore.edit { it[lastFmSharedSecret] = value.trim() }
    suspend fun setLastFmUsername(value: String) =
        context.dataStore.edit { it[lastFmUsername] = value.trim() }
    suspend fun setLastFmSessionKey(value: String) =
        context.dataStore.edit { it[lastFmSessionKey] = value.trim() }
    suspend fun setLastFmScrobble(value: Boolean) =
        context.dataStore.edit { it[lastFmScrobble] = value }

    /** Yeets every preference into the void. Defaults crawl back from the enum graveyard. */
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
