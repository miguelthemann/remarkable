/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.miguelthemann.remarkable.prefs.ThemeMode

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealDark,
    secondary = TealDark,
    background = Cream,
    surface = Cream,
    surfaceContainerLow = Color(0xFFEEF6F4),
    surfaceContainerHigh = Color(0xFFE3EFEC),
)

private val DarkColors = darkColorScheme(
    primary = TealContainer,
    onPrimary = TealDark,
    primaryContainer = Teal,
    onPrimaryContainer = TealContainer,
    background = NightBackground,
    surface = NightSurface,
    surfaceContainerLow = Color(0xFF1C2322),
    surfaceContainerHigh = Color(0xFF252C2B),
)

fun accentColorScheme(accent: Color, darkTheme: Boolean): ColorScheme {
    val container = accent.copy(alpha = 0.28f).compositeOver(if (darkTheme) NightSurface else Cream)
    return if (darkTheme) {
        darkColorScheme(
            primary = accent,
            onPrimary = if (accent.luminance() > 0.5f) Color.Black else Color.White,
            primaryContainer = container,
            onPrimaryContainer = accent,
            secondary = accent.copy(alpha = 0.85f),
            background = NightBackground,
            surface = NightSurface,
            surfaceContainerLow = Color(0xFF1C2322),
            surfaceContainerHigh = Color(0xFF252C2B),
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = if (accent.luminance() > 0.55f) Color.Black else Color.White,
            primaryContainer = container,
            onPrimaryContainer = accent.darken(0.35f),
            secondary = accent.darken(0.2f),
            background = Cream,
            surface = Cream,
            surfaceContainerLow = Color(0xFFEEF6F4),
            surfaceContainerHigh = Color(0xFFE3EFEC),
        )
    }
}

private fun Color.darken(amount: Float): Color = Color(
    red = (red * (1f - amount)).coerceIn(0f, 1f),
    green = (green * (1f - amount)).coerceIn(0f, 1f),
    blue = (blue * (1f - amount)).coerceIn(0f, 1f),
    alpha = alpha,
)

private fun Color.compositeOver(background: Color): Color {
    val a = alpha
    val aOut = a + background.alpha * (1f - a)
    if (aOut == 0f) return Color.Transparent
    return Color(
        red = (red * a + background.red * background.alpha * (1f - a)) / aOut,
        green = (green * a + background.green * background.alpha * (1f - a)) / aOut,
        blue = (blue * a + background.blue * background.alpha * (1f - a)) / aOut,
        alpha = aOut,
    )
}

@Composable
fun RemarkableTheme(
    themeMode: ThemeMode = ThemeMode.MONET,
    nightDim: Boolean = false,
    accentArgb: Long = 0xFF006A6A,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when {
        nightDim -> true
        themeMode == ThemeMode.DARK -> true
        themeMode == ThemeMode.LIGHT -> false
        else -> systemDark
    }
    val useMonet = themeMode == ThemeMode.MONET &&
        !nightDim &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colors = when {
        useMonet -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeMode == ThemeMode.SYSTEM && !nightDim -> {
            if (darkTheme) DarkColors else LightColors
        }
        else -> accentColorScheme(Color.fromArgbLong(accentArgb), darkTheme)
    }

    MaterialTheme(
        colorScheme = colors,
        typography = RemarkableTypography,
        content = content,
    )
}
