/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.miguelthemann.remarkable.R
import com.miguelthemann.remarkable.ui.theme.fromArgbLong
import com.miguelthemann.remarkable.ui.theme.hsvToColor
import com.miguelthemann.remarkable.ui.theme.parseHexColor
import com.miguelthemann.remarkable.ui.theme.toArgbLong
import com.miguelthemann.remarkable.ui.theme.toHexRgb
import com.miguelthemann.remarkable.ui.theme.toHsv
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

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
        Spacer(Modifier.height(12.dp))
        SaturationValuePlane(
            hue = hue,
            saturation = sat,
            value = value,
            onChange = { s, v -> commitHsv(s = s, v = v) },
        )
        Spacer(Modifier.height(12.dp))
        HueWheel(
            hue = hue,
            onHueChange = { commitHsv(h = it) },
        )
    }
}

@Composable
private fun SaturationValuePlane(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pure = hsvToColor(hue, 1f, 1f)
    val selector = MaterialTheme.colorScheme.onSurface
    val corner = 16.dp

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(corner))
            .pointerInput(hue) {
                fun pick(offset: Offset) {
                    val s = (offset.x / size.width).coerceIn(0f, 1f)
                    val v = (1f - offset.y / size.height).coerceIn(0f, 1f)
                    onChange(s, v)
                }
                detectDragGestures(
                    onDragStart = { pick(it) },
                    onDrag = { change, _ ->
                        change.consume()
                        pick(change.position)
                    },
                )
            },
    ) {
        val radius = CornerRadius(corner.toPx())
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color.White, pure)),
            cornerRadius = radius,
        )
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)),
            cornerRadius = radius,
        )
        val cx = saturation * size.width
        val cy = (1f - value) * size.height
        drawCircle(Color.White, 10f, Offset(cx, cy), style = Stroke(3f))
        drawCircle(selector, 7f, Offset(cx, cy), style = Stroke(2f))
    }
}

@Composable
private fun HueWheel(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    val wheelSize = 200.dp
    val density = LocalDensity.current

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(wheelSize)
                .pointerInput(Unit) {
                    val wheelPx = with(density) { wheelSize.toPx() }
                    val innerR = wheelPx * 0.36f
                    val cx = wheelPx / 2f
                    val cy = wheelPx / 2f

                    fun pick(offset: Offset) {
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        if (hypot(dx, dy) < innerR) return
                        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        onHueChange((angle + 360f) % 360f)
                    }

                    detectDragGestures(
                        onDragStart = { pick(it) },
                        onDrag = { change, _ ->
                            change.consume()
                            pick(change.position)
                        },
                    )
                },
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerR = size.minDimension / 2f
            val innerR = outerR * 0.72f

            drawCircle(
                brush = Brush.sweepGradient(
                    0f to Color(0xFFFF0000),
                    60f to Color(0xFFFFFF00),
                    120f to Color(0xFF00FF00),
                    180f to Color(0xFF00FFFF),
                    240f to Color(0xFF0000FF),
                    300f to Color(0xFFFF00FF),
                    360f to Color(0xFFFF0000),
                    center = center,
                ),
                radius = outerR,
                center = center,
            )
            drawCircle(color = surface, radius = innerR, center = center)

            val angleRad = Math.toRadians(hue.toDouble())
            val markerR = (outerR + innerR) / 2f
            val mx = center.x + cos(angleRad).toFloat() * markerR
            val my = center.y + sin(angleRad).toFloat() * markerR
            drawCircle(Color.White, 11f, Offset(mx, my))
            drawCircle(hsvToColor(hue, 1f, 1f), 8f, Offset(mx, my))
            drawCircle(Color.Black.copy(alpha = 0.35f), 11f, Offset(mx, my), style = Stroke(2f))
        }
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
