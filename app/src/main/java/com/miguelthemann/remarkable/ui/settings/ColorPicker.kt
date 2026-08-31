/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.miguelthemann.remarkable.R
import com.miguelthemann.remarkable.ui.theme.asPercentLabel
import com.miguelthemann.remarkable.ui.theme.fromArgbLong
import com.miguelthemann.remarkable.ui.theme.hsvToColor
import com.miguelthemann.remarkable.ui.theme.parseHexColor
import com.miguelthemann.remarkable.ui.theme.toArgbLong
import com.miguelthemann.remarkable.ui.theme.toHexRgb
import com.miguelthemann.remarkable.ui.theme.toHsv
import kotlin.math.roundToInt

@Composable
fun SolidColorPicker(
    argb: Long,
    onColorChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    val current = Color.fromArgbLong(argb)
    val hsv = remember(argb) { current.toHsv() }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    var hexDraft by remember { mutableStateOf(current.toHexRgb()) }
    var hexError by remember { mutableStateOf(false) }

    LaunchedEffect(argb) {
        val next = Color.fromArgbLong(argb)
        val parts = next.toHsv()
        hue = parts[0]
        sat = parts[1]
        value = parts[2]
        hexDraft = next.toHexRgb()
        hexError = false
    }

    fun commitHsv(h: Float = hue, s: Float = sat, v: Float = value) {
        val color = hsvToColor(h, s, v)
        onColorChange(color.toArgbLong())
        hexDraft = color.toHexRgb()
        hexError = false
    }

    fun commitHex() {
        val parsed = parseHexColor(hexDraft)
        if (parsed == null) {
            hexError = true
            return
        }
        hexError = false
        onColorChange(parsed.toArgbLong())
        focus.clearFocus()
    }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.fromArgbLong(argb))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
            )
            OutlinedTextField(
                value = hexDraft,
                onValueChange = {
                    hexDraft = it.uppercase()
                    hexError = false
                    parseHexColor(it)?.let { color ->
                        onColorChange(color.toArgbLong())
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = hexError,
                label = { Text(stringResource(R.string.settings_color_hex)) },
                supportingText = {
                    Text(
                        if (hexError) {
                            stringResource(R.string.settings_color_hex_invalid)
                        } else {
                            stringResource(R.string.settings_color_hex_help)
                        },
                    )
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { commitHex() }),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.settings_color_hue, hue.roundToInt()))
        Slider(
            value = hue,
            onValueChange = {
                hue = it
                commitHsv(h = it)
            },
            valueRange = 0f..360f,
        )
        Text(stringResource(R.string.settings_color_saturation, sat.asPercentLabel()))
        Slider(
            value = sat,
            onValueChange = {
                sat = it
                commitHsv(s = it)
            },
            valueRange = 0f..1f,
        )
        Text(stringResource(R.string.settings_color_value, value.asPercentLabel()))
        Slider(
            value = value,
            onValueChange = {
                value = it
                commitHsv(v = it)
            },
            valueRange = 0f..1f,
        )
    }
}

@Composable
fun ColorSwatchRow(
    selectedArgb: Long,
    presets: List<Long>,
    onSelect: (Long) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        presets.forEach { argb ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.fromArgbLong(argb))
                    .border(
                        width = if (selectedArgb == argb) 3.dp else 0.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(argb) },
            )
        }
    }
}
