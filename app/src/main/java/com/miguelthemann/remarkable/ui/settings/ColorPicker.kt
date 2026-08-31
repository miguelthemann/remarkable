/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.settings

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.miguelthemann.remarkable.R
import com.miguelthemann.remarkable.ui.theme.fromArgbLong
import com.miguelthemann.remarkable.ui.theme.hsvToColor
import com.miguelthemann.remarkable.ui.theme.parseHexColor
import com.miguelthemann.remarkable.ui.theme.toArgbLong
import com.miguelthemann.remarkable.ui.theme.toHexRgb
import com.miguelthemann.remarkable.ui.theme.toHsv
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val WHEEL_PX = 512

private fun buildColorDisc(value: Float): Bitmap {
    val pixels = IntArray(WHEEL_PX * WHEEL_PX)
    val cx = WHEEL_PX / 2f
    val maxR = cx * 0.98f
    for (y in 0 until WHEEL_PX) {
        for (x in 0 until WHEEL_PX) {
            val dx = x - cx
            val dy = y - cx
            val dist = hypot(dx, dy)
            if (dist > maxR) {
                pixels[y * WHEEL_PX + x] = 0
            } else {
                val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                val hue = (angle + 360f) % 360f
                val sat = (dist / maxR).coerceIn(0f, 1f)
                pixels[y * WHEEL_PX + x] = hsvToColor(hue, sat, value).toArgb()
            }
        }
    }
    return Bitmap.createBitmap(WHEEL_PX, WHEEL_PX, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, WHEEL_PX, 0, 0, WHEEL_PX, WHEEL_PX)
    }
}

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

    val discBitmap = remember(value) { buildColorDisc(value).asImageBitmap() }
    val wheelSize = 280.dp
    val density = LocalDensity.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                bitmap = discBitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(wheelSize)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .pointerInput(value) {
                        val px = with(density) { wheelSize.toPx() }
                        val cx = px / 2f
                        val cy = px / 2f
                        val maxR = cx * 0.98f

                        fun pick(offset: Offset) {
                            val dx = offset.x - cx
                            val dy = offset.y - cy
                            val dist = hypot(dx, dy)
                            if (dist > maxR) return
                            val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            val h = (angle + 360f) % 360f
                            val s = (dist / maxR).coerceIn(0f, 1f)
                            hue = h
                            sat = s
                            commitHsv(h, s, value)
                        }

                        detectTapGestures(onTap = { pick(it) })
                        detectDragGestures(
                            onDragStart = { pick(it) },
                            onDrag = { change, _ ->
                                change.consume()
                                pick(change.position)
                            },
                        )
                    },
            )
            Canvas(Modifier.size(wheelSize)) {
                val cx = size.width / 2f
                val maxR = cx * 0.98f
                val angleRad = Math.toRadians(hue.toDouble())
                val r = sat * maxR
                val mx = cx + r * kotlin.math.cos(angleRad).toFloat()
                val my = cx + r * kotlin.math.sin(angleRad).toFloat()
                drawCircle(Color.White, 14f, Offset(mx, my))
                drawCircle(Color.fromArgbLong(argb), 10f, Offset(mx, my))
                drawCircle(Color.Black.copy(alpha = 0.45f), 14f, Offset(mx, my), style = Stroke(2.5f))
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.settings_color_brightness, (value * 100f).roundToInt()),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth(),
        )
        Slider(
            value = value,
            onValueChange = {
                value = it
                commitHsv(v = it)
            },
            valueRange = 0.05f..1f,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.fromArgbLong(argb))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
            )
            OutlinedTextField(
                value = hexDraft,
                onValueChange = {
                    hexDraft = it.uppercase()
                    hexError = false
                    parseHexColor(it)?.let { color -> onColorChange(color.toArgbLong()) }
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
    }
}

/** Opens the picker sheet from a compact swatch row. */
@Composable
fun ColorPickerNavRow(
    title: String,
    argb: Long,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(title) },
        supportingContent = { Text(Color.fromArgbLong(argb).toHexRgb()) },
        trailingContent = {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.fromArgbLong(argb))
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
        },
    )
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
        presets.forEach { preset ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.fromArgbLong(preset))
                    .border(
                        width = if (selectedArgb == preset) 3.dp else 1.dp,
                        color = if (selectedArgb == preset) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        },
                        shape = CircleShape,
                    )
                    .clickable { onSelect(preset) },
            )
        }
    }
}
