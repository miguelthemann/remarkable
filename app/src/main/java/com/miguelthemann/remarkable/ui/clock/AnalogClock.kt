/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.ZonedDateTime
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnalogClock(
    now: ZonedDateTime,
    modifier: Modifier = Modifier,
    size: Dp = 196.dp,
) {
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val hourColor = MaterialTheme.colorScheme.onSurface
    val minuteColor = MaterialTheme.colorScheme.primary
    val secondColor = MaterialTheme.colorScheme.tertiary
    val marker = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(
            color = track,
            radius = radius - 4.dp.toPx(),
            center = center,
            style = Stroke(width = 3.dp.toPx()),
        )
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30 - 90).toDouble())
            val inner = radius - 16.dp.toPx()
            val outer = radius - 8.dp.toPx()
            drawLine(
                color = marker,
                start = Offset(
                    center.x + inner * cos(angle).toFloat(),
                    center.y + inner * sin(angle).toFloat(),
                ),
                end = Offset(
                    center.x + outer * cos(angle).toFloat(),
                    center.y + outer * sin(angle).toFloat(),
                ),
                strokeWidth = if (i % 3 == 0) 4.dp.toPx() else 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        fun hand(value: Double, length: Float, width: Float, color: androidx.compose.ui.graphics.Color) {
            val angle = Math.toRadians(value * 6.0 - 90.0)
            drawLine(
                color = color,
                start = center,
                end = Offset(
                    center.x + length * cos(angle).toFloat(),
                    center.y + length * sin(angle).toFloat(),
                ),
                strokeWidth = width,
                cap = StrokeCap.Round,
            )
        }

        val hours = now.hour % 12 + now.minute / 60.0 + now.second / 3600.0
        val minutes = now.minute + now.second / 60.0
        val seconds = now.second + now.nano / 1_000_000_000.0
        hand(hours * 5.0, radius * 0.5f, 8.dp.toPx(), hourColor)
        hand(minutes, radius * 0.7f, 5.dp.toPx(), minuteColor)
        hand(seconds, radius * 0.82f, 2.dp.toPx(), secondColor)
        drawCircle(color = hourColor, radius = 6.dp.toPx(), center = center)
    }
}
