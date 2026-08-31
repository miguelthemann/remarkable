/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miguelthemann.remarkable.R
import com.miguelthemann.remarkable.ui.clock.ClockViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ClockViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var cityDraft by remember { mutableStateOf(state.city) }
    var clientDraft by remember { mutableStateOf(state.spotifyClientId) }
    var cityDirty by remember { mutableStateOf(false) }
    var clientDirty by remember { mutableStateOf(false) }

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
                .padding(bottom = 32.dp),
        ) {
            SectionTitle(stringResource(R.string.settings_clock))
            ToggleRow(
                title = stringResource(R.string.settings_use_24h),
                checked = state.use24Hour,
                onCheckedChange = viewModel::setUse24Hour,
            )
            ToggleRow(
                title = stringResource(R.string.settings_show_seconds),
                checked = state.showSeconds,
                onCheckedChange = viewModel::setShowSeconds,
            )
            ToggleRow(
                title = stringResource(R.string.settings_keep_awake),
                checked = state.keepAwake,
                onCheckedChange = viewModel::setKeepAwake,
            )
            ToggleRow(
                title = stringResource(R.string.settings_night_dim),
                checked = state.nightDim,
                onCheckedChange = viewModel::setNightDim,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.settings_weather))
            ToggleRow(
                title = stringResource(R.string.settings_use_celsius),
                checked = state.useCelsius,
                onCheckedChange = viewModel::setUseCelsius,
            )
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
