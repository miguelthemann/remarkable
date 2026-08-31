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
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "remarkable")

data class UserSettings(
    val use24Hour: Boolean = true,
    val showSeconds: Boolean = true,
    val keepAwake: Boolean = true,
    val nightDim: Boolean = false,
    val useCelsius: Boolean = true,
    val city: String = "",
    val spotifyClientId: String = "",
)

class UserPreferences(private val context: Context) {
    private val use24Hour = booleanPreferencesKey("use_24h")
    private val showSeconds = booleanPreferencesKey("show_seconds")
    private val keepAwake = booleanPreferencesKey("keep_awake")
    private val nightDim = booleanPreferencesKey("night_dim")
    private val useCelsius = booleanPreferencesKey("use_celsius")
    private val city = stringPreferencesKey("city")
    private val spotifyClientId = stringPreferencesKey("spotify_client_id")

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            use24Hour = prefs[use24Hour] ?: true,
            showSeconds = prefs[showSeconds] ?: true,
            keepAwake = prefs[keepAwake] ?: true,
            nightDim = prefs[nightDim] ?: false,
            useCelsius = prefs[useCelsius] ?: true,
            city = prefs[city].orEmpty(),
            spotifyClientId = prefs[spotifyClientId].orEmpty(),
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
}
