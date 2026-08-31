/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.clock

import android.app.Application
import android.location.Location
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miguelthemann.remarkable.lastfm.LastFmClient
import com.miguelthemann.remarkable.location.DeviceLocation
import com.miguelthemann.remarkable.media.MusicSource
import com.miguelthemann.remarkable.media.MusicStatus
import com.miguelthemann.remarkable.media.NowPlayingTrack
import com.miguelthemann.remarkable.media.SystemMediaBus
import com.miguelthemann.remarkable.prefs.AccentPreset
import com.miguelthemann.remarkable.prefs.BackgroundImageStore
import com.miguelthemann.remarkable.prefs.BackgroundMode
import com.miguelthemann.remarkable.prefs.ClockStyle
import com.miguelthemann.remarkable.prefs.ModuleOffsets
import com.miguelthemann.remarkable.prefs.SpotifyModuleStyle
import com.miguelthemann.remarkable.prefs.ThemeMode
import com.miguelthemann.remarkable.prefs.UserPreferences
import com.miguelthemann.remarkable.prefs.UserSettings
import com.miguelthemann.remarkable.prefs.WeatherEffect
import com.miguelthemann.remarkable.spotify.SpotifyRemote
import com.miguelthemann.remarkable.spotify.SpotifyStatus
import com.miguelthemann.remarkable.weather.WeatherRepository
import com.miguelthemann.remarkable.weather.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZonedDateTime

data class ClockUiState(
    val prefsReady: Boolean = false,
    val onboardingDone: Boolean = false,
    val now: ZonedDateTime = ZonedDateTime.now(),
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
    val customBgArgb: Long = 0xFFF7F1E5,
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
    val weather: WeatherSnapshot? = null,
    val weatherMessage: WeatherMessage = WeatherMessage.Idle,
    val music: MusicStatus = MusicStatus.Idle,
    val lastFmLoginMessage: String? = null,
)

sealed interface WeatherMessage {
    data object Idle : WeatherMessage
    data object NeedsLocation : WeatherMessage
    data object Loading : WeatherMessage
    data class Error(val text: String) : WeatherMessage
}

class ClockViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = UserPreferences(application)
    private val weatherRepository = WeatherRepository()
    private val deviceLocation = DeviceLocation(application)
    private val spotifyRemote = SpotifyRemote(application)
    private val lastFm = LastFmClient()

    private val _uiState = MutableStateFlow(ClockUiState())
    val uiState: StateFlow<ClockUiState> = _uiState.asStateFlow()

    private var musicJob: Job? = null
    private var locationGranted = false
    private var lastScrobbledKey: String? = null
    private var scrobbleStartedAt: Long = 0L

    init {
        viewModelScope.launch {
            while (isActive) {
                _uiState.update { it.copy(now = ZonedDateTime.now()) }
                delay(250)
            }
        }
        viewModelScope.launch {
            var lastCity: String? = null
            var lastMusicKey: String? = null
            preferences.settings.collect { settings ->
                applySettings(settings)
                if (lastCity == null || lastCity != settings.city) {
                    lastCity = settings.city
                    refreshWeather()
                }
                val musicKey = listOf(
                    settings.musicSource.name,
                    settings.spotifyClientId,
                    settings.lastFmApiKey,
                    settings.lastFmUsername,
                    settings.lastFmSessionKey,
                    settings.lastFmScrobble.toString(),
                ).joinToString("|")
                if (lastMusicKey != musicKey) {
                    lastMusicKey = musicKey
                    restartMusic(settings)
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(15 * 60 * 1000L)
                refreshWeather()
            }
        }
    }

    private fun applySettings(settings: UserSettings) {
        _uiState.update {
            it.copy(
                prefsReady = true,
                onboardingDone = settings.onboardingDone,
                use24Hour = settings.use24Hour,
                showSeconds = settings.showSeconds,
                keepAwake = settings.keepAwake,
                nightDim = settings.nightDim,
                useCelsius = settings.useCelsius,
                city = settings.city,
                spotifyClientId = settings.spotifyClientId,
                themeMode = settings.themeMode,
                backgroundMode = settings.backgroundMode,
                clockStyle = settings.clockStyle,
                accentArgb = settings.accentArgb,
                customBgArgb = settings.customBgArgb,
                backgroundImagePath = settings.backgroundImagePath,
                showDate = settings.showDate,
                showWeather = settings.showWeather,
                showSpotify = settings.showSpotify,
                modules = settings.modules,
                burnInProtection = settings.burnInProtection,
                burnInShiftDp = settings.burnInShiftDp,
                burnInIntervalSec = settings.burnInIntervalSec,
                smartPixels = settings.smartPixels,
                smartPixelsStrength = settings.smartPixelsStrength,
                overlayEnabled = settings.overlayEnabled,
                spotifyStyle = settings.spotifyStyle,
                showSpotifyIcon = settings.showSpotifyIcon,
                useGenericMusicIcon = settings.useGenericMusicIcon,
                showTrackTitle = settings.showTrackTitle,
                showArtist = settings.showArtist,
                showAlbum = settings.showAlbum,
                showReleaseYear = settings.showReleaseYear,
                weatherEffect = settings.weatherEffect,
                musicSource = settings.musicSource,
                lastFmApiKey = settings.lastFmApiKey,
                lastFmSharedSecret = settings.lastFmSharedSecret,
                lastFmUsername = settings.lastFmUsername,
                lastFmSessionKey = settings.lastFmSessionKey,
                lastFmScrobble = settings.lastFmScrobble,
            )
        }
    }

    private fun restartMusic(settings: UserSettings) {
        musicJob?.cancel()
        spotifyRemote.disconnect()
        musicJob = viewModelScope.launch {
            when (settings.musicSource) {
                MusicSource.SYSTEM -> collectSystemMedia(settings)
                MusicSource.SPOTIFY -> collectSpotify(settings.spotifyClientId)
                MusicSource.LASTFM -> collectLastFm(settings)
            }
        }
    }

    private suspend fun collectSystemMedia(settings: UserSettings) {
        SystemMediaBus.refreshFrom(getApplication())
        SystemMediaBus.status.collect { status ->
            _uiState.update { it.copy(music = status) }
            if (settings.lastFmScrobble &&
                settings.lastFmSessionKey.isNotBlank() &&
                status is MusicStatus.Ready &&
                !status.track.isPaused
            ) {
                maybeScrobble(settings, status.track)
            }
        }
    }

    private suspend fun collectSpotify(clientId: String) {
        if (clientId.isBlank()) {
            _uiState.update { it.copy(music = MusicStatus.NeedSpotifySetup) }
            return
        }
        spotifyRemote.connect(clientId).collect { status ->
            _uiState.update {
                it.copy(
                    music = when (status) {
                        SpotifyStatus.NeedClientId -> MusicStatus.NeedSpotifySetup
                        SpotifyStatus.MissingApp -> MusicStatus.Failed("Spotify app missing")
                        SpotifyStatus.Connecting, SpotifyStatus.Idle -> MusicStatus.Loading
                        is SpotifyStatus.Failed -> MusicStatus.Failed(status.message)
                        is SpotifyStatus.Connected -> {
                            val np = status.nowPlaying
                            if (np == null) MusicStatus.NothingPlaying
                            else MusicStatus.Ready(
                                track = NowPlayingTrack(
                                    title = np.title,
                                    artist = np.artist,
                                    album = np.album,
                                    releaseYear = np.releaseYear,
                                    isPaused = np.isPaused,
                                    artwork = np.artwork,
                                    appLabel = "Spotify",
                                ),
                                canControl = true,
                            )
                        }
                    },
                )
            }
        }
    }

    private suspend fun collectLastFm(settings: UserSettings) {
        if (settings.lastFmApiKey.isBlank() || settings.lastFmUsername.isBlank()) {
            _uiState.update { it.copy(music = MusicStatus.NeedLastFmSetup) }
            return
        }
        while (isActive) {
            _uiState.update { it.copy(music = MusicStatus.Loading) }
            val result = runCatching {
                lastFm.recentTrack(settings.lastFmApiKey, settings.lastFmUsername)
            }
            result.fold(
                onSuccess = { track ->
                    _uiState.update {
                        it.copy(
                            music = if (track == null) {
                                MusicStatus.NothingPlaying
                            } else {
                                MusicStatus.Ready(
                                    track = NowPlayingTrack(
                                        title = track.title,
                                        artist = track.artist,
                                        album = track.album,
                                        isPaused = !track.isNowPlaying,
                                        appLabel = "Last.fm",
                                    ),
                                    canControl = false,
                                )
                            },
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(music = MusicStatus.Failed(err.message ?: err.toString()))
                    }
                },
            )
            delay(30_000)
        }
    }

    private suspend fun maybeScrobble(settings: UserSettings, track: NowPlayingTrack) {
        val key = "${track.artist}|${track.title}"
        if (key != lastScrobbledKey) {
            lastScrobbledKey = key
            scrobbleStartedAt = Instant.now().epochSecond
            runCatching {
                lastFm.updateNowPlaying(
                    apiKey = settings.lastFmApiKey,
                    sharedSecret = settings.lastFmSharedSecret,
                    sessionKey = settings.lastFmSessionKey,
                    artist = track.artist,
                    track = track.title,
                    album = track.album,
                )
            }
            return
        }
        val listened = Instant.now().epochSecond - scrobbleStartedAt
        if (listened >= 30) {
            runCatching {
                lastFm.scrobble(
                    apiKey = settings.lastFmApiKey,
                    sharedSecret = settings.lastFmSharedSecret,
                    sessionKey = settings.lastFmSessionKey,
                    artist = track.artist,
                    track = track.title,
                    album = track.album,
                    timestampSec = scrobbleStartedAt,
                )
            }
            // Prevent double-scrobble spam for the same play
            scrobbleStartedAt = Instant.now().epochSecond + 10_000
        }
    }

    fun onLocationPermission(granted: Boolean) {
        locationGranted = granted
        refreshWeather()
    }

    fun refreshWeather() {
        viewModelScope.launch {
            _uiState.update { it.copy(weatherMessage = WeatherMessage.Loading) }
            val city = _uiState.value.city.trim()
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    if (city.isNotEmpty()) {
                        val geo = weatherRepository.geocodeCity(city)
                            ?: error("City not found")
                        weatherRepository.fetch(geo.first, geo.second, geo.third)
                    } else {
                        val location: Location = deviceLocation.lastKnown()
                            ?: return@withContext null
                        val label = deviceLocation.reverseLabel(location)
                        weatherRepository.fetch(location.latitude, location.longitude, label)
                    }
                }
            }
            result.fold(
                onSuccess = { snapshot ->
                    if (snapshot == null) {
                        _uiState.update {
                            it.copy(
                                weather = null,
                                weatherMessage = if (locationGranted) {
                                    WeatherMessage.Error("No location yet")
                                } else {
                                    WeatherMessage.NeedsLocation
                                },
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(weather = snapshot, weatherMessage = WeatherMessage.Idle)
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            weather = null,
                            weatherMessage = WeatherMessage.Error(
                                error.message ?: error.toString(),
                            ),
                        )
                    }
                },
            )
        }
    }

    fun refreshSystemMedia() {
        SystemMediaBus.refreshFrom(getApplication())
    }

    fun connectSpotify() {
        musicJob?.cancel()
        spotifyRemote.disconnect()
        musicJob = viewModelScope.launch {
            collectSpotify(_uiState.value.spotifyClientId)
        }
    }

    fun playPause() {
        when (_uiState.value.musicSource) {
            MusicSource.SYSTEM -> SystemMediaBus.playPause()
            MusicSource.SPOTIFY -> {
                val track = (_uiState.value.music as? MusicStatus.Ready)?.track
                if (track?.isPaused == false) spotifyRemote.pause() else spotifyRemote.play()
            }
            MusicSource.LASTFM -> Unit
        }
    }

    fun skipNext() {
        when (_uiState.value.musicSource) {
            MusicSource.SYSTEM -> SystemMediaBus.skipNext()
            MusicSource.SPOTIFY -> spotifyRemote.skipNext()
            MusicSource.LASTFM -> Unit
        }
    }

    fun skipPrevious() {
        when (_uiState.value.musicSource) {
            MusicSource.SYSTEM -> SystemMediaBus.skipPrevious()
            MusicSource.SPOTIFY -> spotifyRemote.skipPrevious()
            MusicSource.LASTFM -> Unit
        }
    }

    fun loginLastFm(password: String) {
        viewModelScope.launch {
            val s = _uiState.value
            _uiState.update { it.copy(lastFmLoginMessage = null) }
            runCatching {
                lastFm.mobileSession(
                    apiKey = s.lastFmApiKey,
                    sharedSecret = s.lastFmSharedSecret,
                    username = s.lastFmUsername,
                    password = password,
                )
            }.fold(
                onSuccess = { key ->
                    preferences.setLastFmSessionKey(key)
                    _uiState.update { it.copy(lastFmLoginMessage = "ok") }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(lastFmLoginMessage = err.message ?: err.toString())
                    }
                },
            )
        }
    }

    fun completeOnboarding() = viewModelScope.launch { preferences.setOnboardingDone(true) }

    fun setUse24Hour(value: Boolean) = viewModelScope.launch { preferences.setUse24Hour(value) }
    fun setShowSeconds(value: Boolean) = viewModelScope.launch { preferences.setShowSeconds(value) }
    fun setKeepAwake(value: Boolean) = viewModelScope.launch { preferences.setKeepAwake(value) }
    fun setNightDim(value: Boolean) = viewModelScope.launch { preferences.setNightDim(value) }
    fun setUseCelsius(value: Boolean) = viewModelScope.launch { preferences.setUseCelsius(value) }
    fun setCity(value: String) = viewModelScope.launch { preferences.setCity(value) }
    fun setSpotifyClientId(value: String) =
        viewModelScope.launch { preferences.setSpotifyClientId(value) }
    fun setThemeMode(value: ThemeMode) = viewModelScope.launch { preferences.setThemeMode(value) }
    fun setBackgroundMode(value: BackgroundMode) =
        viewModelScope.launch { preferences.setBackgroundMode(value) }
    fun setClockStyle(value: ClockStyle) = viewModelScope.launch { preferences.setClockStyle(value) }
    fun setAccentArgb(value: Long) = viewModelScope.launch { preferences.setAccentArgb(value) }
    fun setCustomBgArgb(value: Long) = viewModelScope.launch { preferences.setCustomBgArgb(value) }
    fun importBackgroundImage(uri: Uri) = viewModelScope.launch {
        val path = BackgroundImageStore.importFromUri(getApplication(), uri)
        preferences.setBackgroundImagePath(path)
        preferences.setBackgroundMode(BackgroundMode.IMAGE)
    }
    fun clearBackgroundImage() = viewModelScope.launch {
        BackgroundImageStore.clear(getApplication())
        preferences.setBackgroundImagePath("")
        if (_uiState.value.backgroundMode == BackgroundMode.IMAGE) {
            preferences.setBackgroundMode(BackgroundMode.CUSTOM_COLOR)
        }
    }
    fun setShowDate(value: Boolean) = viewModelScope.launch { preferences.setShowDate(value) }
    fun setShowWeather(value: Boolean) = viewModelScope.launch { preferences.setShowWeather(value) }
    fun setShowSpotify(value: Boolean) = viewModelScope.launch { preferences.setShowSpotify(value) }
    fun setModuleOffsets(modules: ModuleOffsets) =
        viewModelScope.launch { preferences.setModuleOffsets(modules) }
    fun resetModules() = setModuleOffsets(ModuleOffsets())
    fun setBurnInProtection(value: Boolean) =
        viewModelScope.launch { preferences.setBurnInProtection(value) }
    fun setBurnInShiftDp(value: Int) =
        viewModelScope.launch { preferences.setBurnInShiftDp(value) }
    fun setBurnInIntervalSec(value: Int) =
        viewModelScope.launch { preferences.setBurnInIntervalSec(value) }
    fun setSmartPixels(value: Boolean) =
        viewModelScope.launch { preferences.setSmartPixels(value) }
    fun setSmartPixelsStrength(value: Float) =
        viewModelScope.launch { preferences.setSmartPixelsStrength(value) }
    fun setOverlayEnabled(value: Boolean) =
        viewModelScope.launch { preferences.setOverlayEnabled(value) }
    fun setSpotifyStyle(value: SpotifyModuleStyle) =
        viewModelScope.launch { preferences.setSpotifyStyle(value) }
    fun setShowSpotifyIcon(value: Boolean) =
        viewModelScope.launch { preferences.setShowSpotifyIcon(value) }
    fun setUseGenericMusicIcon(value: Boolean) =
        viewModelScope.launch { preferences.setUseGenericMusicIcon(value) }
    fun setShowTrackTitle(value: Boolean) =
        viewModelScope.launch { preferences.setShowTrackTitle(value) }
    fun setShowArtist(value: Boolean) =
        viewModelScope.launch { preferences.setShowArtist(value) }
    fun setShowAlbum(value: Boolean) =
        viewModelScope.launch { preferences.setShowAlbum(value) }
    fun setShowReleaseYear(value: Boolean) =
        viewModelScope.launch { preferences.setShowReleaseYear(value) }
    fun setWeatherEffect(value: WeatherEffect) =
        viewModelScope.launch { preferences.setWeatherEffect(value) }
    fun setMusicSource(value: MusicSource) =
        viewModelScope.launch { preferences.setMusicSource(value) }
    fun setLastFmApiKey(value: String) =
        viewModelScope.launch { preferences.setLastFmApiKey(value) }
    fun setLastFmSharedSecret(value: String) =
        viewModelScope.launch { preferences.setLastFmSharedSecret(value) }
    fun setLastFmUsername(value: String) =
        viewModelScope.launch { preferences.setLastFmUsername(value) }
    fun setLastFmScrobble(value: Boolean) =
        viewModelScope.launch { preferences.setLastFmScrobble(value) }

    fun resetEverything(onDone: () -> Unit) = viewModelScope.launch {
        BackgroundImageStore.clear(getApplication())
        preferences.clearAll()
        onDone()
    }

    override fun onCleared() {
        spotifyRemote.disconnect()
        super.onCleared()
    }
}
