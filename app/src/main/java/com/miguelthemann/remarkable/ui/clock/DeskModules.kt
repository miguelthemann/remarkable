/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.clock

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.miguelthemann.remarkable.R
import com.miguelthemann.remarkable.media.MusicStatus
import com.miguelthemann.remarkable.media.NowPlayingTrack
import com.miguelthemann.remarkable.prefs.ModuleOffsets
import com.miguelthemann.remarkable.prefs.SpotifyModuleStyle
import com.miguelthemann.remarkable.weather.celsiusToFahrenheit
import com.miguelthemann.remarkable.weather.weatherIcon
import com.miguelthemann.remarkable.weather.weatherLabelRes

enum class DeskModule { TIME, DATE, WEATHER, SPOTIFY }

@Composable
fun SpotifyModule(
    state: ClockUiState,
    viewModel: ClockViewModel,
    modifier: Modifier = Modifier,
) {
    val playing = (state.music as? MusicStatus.Ready)?.track
    when (state.spotifyStyle) {
        SpotifyModuleStyle.ONE_LINER -> SpotifyOneLiner(state, playing, viewModel, modifier)
        SpotifyModuleStyle.CARD -> SpotifyCardStyle(state, playing, viewModel, modifier)
        SpotifyModuleStyle.WIDGET -> SpotifyWidgetStyle(state, playing, viewModel, modifier)
    }
}

@Composable
private fun SpotifyIconBadge(state: ClockUiState) {
    if (!state.showSpotifyIcon) return
    val colors = LocalDeskModuleColors.current
    Icon(
        imageVector = Icons.Outlined.MusicNote,
        contentDescription = null,
        tint = if (state.useGenericMusicIcon) {
            colors.onSurfaceVariant
        } else {
            Color(0xFF1DB954)
        },
        modifier = Modifier.size(22.dp),
    )
}

@Composable
private fun SpotifyOneLiner(
    state: ClockUiState,
    playing: NowPlayingTrack?,
    viewModel: ClockViewModel,
    modifier: Modifier,
) {
    val colors = LocalDeskModuleColors.current
    ModuleSurface(
        modifier = modifier.widthIn(max = 420.dp),
        shape = RoundedCornerShape(20.dp),
        horizontalPadding = 14.dp,
        verticalPadding = 10.dp,
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SpotifyIconBadge(state)
            Text(
                text = buildSpotifyLine(state, playing),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            FilledTonalIconButton(onClick = viewModel::playPause, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (playing?.isPaused != false) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun SpotifyCardStyle(
    state: ClockUiState,
    playing: NowPlayingTrack?,
    viewModel: ClockViewModel,
    modifier: Modifier,
) {
    val colors = LocalDeskModuleColors.current
    ModuleSurface(
        modifier = modifier.widthIn(max = 420.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SpotifyIconBadge(state)
                Text(
                    stringResource(R.string.music_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                )
            }
            Spacer(Modifier.height(8.dp))
            SpotifyStatusBody(state, playing, viewModel, showArt = false)
        }
    }
}

@Composable
private fun SpotifyWidgetStyle(
    state: ClockUiState,
    playing: NowPlayingTrack?,
    viewModel: ClockViewModel,
    modifier: Modifier,
) {
    ModuleSurface(
        modifier = modifier.widthIn(max = 420.dp),
        shape = RoundedCornerShape(28.dp),
    ) {
        SpotifyStatusBody(state, playing, viewModel, showArt = true)
    }
}

@Composable
private fun SpotifyStatusBody(
    state: ClockUiState,
    playing: NowPlayingTrack?,
    viewModel: ClockViewModel,
    showArt: Boolean,
) {
    val colors = LocalDeskModuleColors.current
    when (val music = state.music) {
        MusicStatus.NeedNotificationAccess -> {
            Text(stringResource(R.string.music_need_notification_access), color = colors.onSurface)
            val context = androidx.compose.ui.platform.LocalContext.current
            Button(
                onClick = {
                    context.startActivity(
                        android.content.Intent(
                            android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
                        ),
                    )
                },
            ) {
                Text(stringResource(R.string.music_open_notification_access))
            }
            TextButton(onClick = viewModel::refreshSystemMedia) {
                Text(stringResource(R.string.music_refresh))
            }
        }
        MusicStatus.NeedSpotifySetup -> Text(
            stringResource(R.string.spotify_need_client_id),
            color = colors.onSurface,
        )
        MusicStatus.NeedLastFmSetup -> Text(
            stringResource(R.string.lastfm_need_setup),
            color = colors.onSurface,
        )
        MusicStatus.NothingPlaying, MusicStatus.Idle -> {
            Text(stringResource(R.string.spotify_idle), color = colors.onSurface)
            if (state.musicSource == com.miguelthemann.remarkable.media.MusicSource.SYSTEM) {
                TextButton(onClick = viewModel::refreshSystemMedia) {
                    Text(stringResource(R.string.music_refresh))
                }
            }
        }
        MusicStatus.Loading -> Text(stringResource(R.string.music_loading), color = colors.onSurface)
        is MusicStatus.Failed -> {
            Text(stringResource(R.string.music_error), color = colors.onSurface)
            Text(music.message, color = colors.onSurfaceVariant)
            if (state.musicSource == com.miguelthemann.remarkable.media.MusicSource.SPOTIFY) {
                Button(onClick = viewModel::connectSpotify) {
                    Text(stringResource(R.string.spotify_connect))
                }
            }
        }
        is MusicStatus.Ready -> {
            MusicReadyContent(
                state = state,
                track = music.track,
                canControl = music.canControl,
                showArt = showArt,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun MusicReadyContent(
    state: ClockUiState,
    track: NowPlayingTrack,
    canControl: Boolean,
    showArt: Boolean,
    viewModel: ClockViewModel,
) {
    val colors = LocalDeskModuleColors.current
    val art = track.artwork
    val year = track.releaseYear
    val appLabel = track.appLabel
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showArt && art != null) {
            Image(
                bitmap = art.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
        } else if (showArt) {
            SpotifyIconBadge(state)
        }
        Box(modifier = Modifier.weight(1f)) {
            Column {
                if (state.showTrackTitle) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (state.showArtist && track.artist.isNotBlank()) {
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (state.showAlbum && track.album.isNotBlank()) {
                    Text(
                        text = track.album,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (state.showReleaseYear && !year.isNullOrBlank()) {
                    Text(
                        text = year,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.outline,
                    )
                }
                if (!appLabel.isNullOrBlank()) {
                    Text(
                        text = appLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.outline,
                    )
                }
            }
        }
    }
    if (canControl) {
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            FilledTonalIconButton(onClick = viewModel::skipPrevious) {
                Icon(Icons.Outlined.SkipPrevious, contentDescription = stringResource(R.string.skip_previous))
            }
            FilledTonalIconButton(onClick = viewModel::playPause) {
                Icon(
                    imageVector = if (track.isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                    contentDescription = null,
                )
            }
            FilledTonalIconButton(onClick = viewModel::skipNext) {
                Icon(Icons.Outlined.SkipNext, contentDescription = stringResource(R.string.skip_next))
            }
        }
    }
}

private fun buildSpotifyLine(state: ClockUiState, playing: NowPlayingTrack?): String {
    if (playing == null) return "—"
    val parts = buildList {
        if (state.showTrackTitle) add(playing.title)
        if (state.showArtist && playing.artist.isNotBlank()) add(playing.artist)
        if (state.showAlbum && playing.album.isNotBlank()) add(playing.album)
        if (state.showReleaseYear && !playing.releaseYear.isNullOrBlank()) add(playing.releaseYear)
    }
    return parts.joinToString(" · ").ifBlank { "—" }
}

@Composable
fun WeatherModule(
    state: ClockUiState,
    onRequestLocation: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDeskModuleColors.current
    ModuleSurface(
        modifier = modifier.widthIn(max = 380.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column {
            Text(
                stringResource(R.string.weather_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(8.dp))
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            weatherIcon(weather.weatherCode, state.now.hour),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(40.dp),
                        )
                        Column {
                            Text(
                                temp,
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.onSurface,
                            )
                            Text(
                                stringResource(weatherLabelRes(weather.weatherCode)),
                                color = colors.onSurface,
                            )
                            Text(
                                weather.place,
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
                state.weatherMessage is WeatherMessage.NeedsLocation -> {
                    Text(stringResource(R.string.weather_permission), color = colors.onSurface)
                    if (onRequestLocation != null) {
                        TextButton(onClick = onRequestLocation) {
                            Text(stringResource(R.string.grant_location))
                        }
                    }
                }
                state.weatherMessage is WeatherMessage.Error -> {
                    Text(stringResource(R.string.weather_unavailable), color = colors.onSurface)
                }
                else -> Text(stringResource(R.string.weather_unavailable), color = colors.onSurface)
            }
            Text(
                stringResource(R.string.weather_attribution),
                style = MaterialTheme.typography.labelLarge,
                color = colors.outline,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

fun ModuleOffsets.withModule(module: DeskModule, x: Float, y: Float): ModuleOffsets = when (module) {
    DeskModule.TIME -> copy(timeX = x, timeY = y)
    DeskModule.DATE -> copy(dateX = x, dateY = y)
    DeskModule.WEATHER -> copy(weatherX = x, weatherY = y)
    DeskModule.SPOTIFY -> copy(spotifyX = x, spotifyY = y)
}

fun ModuleOffsets.withModuleScale(module: DeskModule, scale: Float): ModuleOffsets {
    val s = scale.coerceIn(ModuleOffsets.MinScale, ModuleOffsets.MaxScale)
    return when (module) {
        DeskModule.TIME -> copy(timeScale = s)
        DeskModule.DATE -> copy(dateScale = s)
        DeskModule.WEATHER -> copy(weatherScale = s)
        DeskModule.SPOTIFY -> copy(spotifyScale = s)
    }
}

fun ModuleOffsets.scaleOf(module: DeskModule): Float = when (module) {
    DeskModule.TIME -> timeScale
    DeskModule.DATE -> dateScale
    DeskModule.WEATHER -> weatherScale
    DeskModule.SPOTIFY -> spotifyScale
}
