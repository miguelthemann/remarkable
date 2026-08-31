/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.theme

import androidx.compose.ui.graphics.Color

val TealDark = Color(0xFF003737)
val Teal = Color(0xFF006A6A)
val TealContainer = Color(0xFF9EF1F0)
val Cream = Color(0xFFFAFDFB)
val NightBackground = Color(0xFF0E1413)
val NightSurface = Color(0xFF171D1C)

fun Color.Companion.fromArgbLong(argb: Long): Color = Color(argb.toInt())
