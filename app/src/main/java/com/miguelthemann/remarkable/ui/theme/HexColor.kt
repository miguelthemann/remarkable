/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt

fun Color.toHexRgb(): String {
    val argb = toArgb()
    return String.format("#%06X", argb and 0xFFFFFF)
}

fun parseHexColor(raw: String): Color? {
    val cleaned = raw.trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
    val hex = when (cleaned.length) {
        6 -> cleaned
        8 -> cleaned.takeLast(6) // ignore alpha in input
        3 -> cleaned.map { "$it$it" }.joinToString("")
        else -> return null
    }
    val value = hex.toIntOrNull(16) ?: return null
    return Color(0xFF000000 or value.toLong())
}

fun Color.toHsv(): FloatArray {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val h = when {
        delta == 0f -> 0f
        max == r -> ((60f * ((g - b) / delta) + 360f) % 360f)
        max == g -> (60f * ((b - r) / delta) + 120f)
        else -> (60f * ((r - g) / delta) + 240f)
    }
    val s = if (max == 0f) 0f else delta / max
    val v = max
    return floatArrayOf(h, s, v)
}

fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val hue = ((h % 360f) + 360f) % 360f
    val c = v * s
    val x = c * (1f - kotlin.math.abs((hue / 60f) % 2f - 1f))
    val m = v - c
    val (rp, gp, bp) = when {
        hue < 60f -> Triple(c, x, 0f)
        hue < 120f -> Triple(x, c, 0f)
        hue < 180f -> Triple(0f, c, x)
        hue < 240f -> Triple(0f, x, c)
        hue < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(rp + m, gp + m, bp + m, 1f)
}

fun Color.toArgbLong(): Long = (toArgb().toLong() and 0xFFFFFFFFL)

fun Float.asPercentLabel(): String = "${(this * 100f).roundToInt()}%"
