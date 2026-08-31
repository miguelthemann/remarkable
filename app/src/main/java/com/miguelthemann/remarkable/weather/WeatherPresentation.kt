/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.weather

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.miguelthemann.remarkable.R
import com.miguelthemann.remarkable.time.isNightHour

fun weatherLabel(code: Int): Int = when (code) {
    0 -> R.string.weather_clear
    1 -> R.string.weather_mainly_clear
    2 -> R.string.weather_partly_cloudy
    3 -> R.string.weather_overcast
    45, 48 -> R.string.weather_fog
    in 51..57 -> R.string.weather_drizzle
    in 61..67, in 80..82 -> R.string.weather_rain
    in 71..77, in 85..86 -> R.string.weather_snow
    in 95..99 -> R.string.weather_thunder
    else -> R.string.weather_unknown
}

fun weatherLabelRes(code: Int): Int = weatherLabel(code)

fun weatherIcon(code: Int, hour: Int = 12): ImageVector = when (code) {
    0, 1 -> if (isNightHour(hour)) Icons.Outlined.Nightlight else Icons.Outlined.WbSunny
    2 -> Icons.Outlined.WbCloudy
    3, 45, 48 -> Icons.Outlined.Cloud
    in 51..67, in 80..82 -> Icons.Outlined.WaterDrop
    in 71..77, in 85..86 -> Icons.Outlined.AcUnit
    in 95..99 -> Icons.Outlined.FlashOn
    else -> Icons.Outlined.Cloud
}
