/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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

@Composable
fun RemarkableTheme(
    nightDim: Boolean = false,
    darkTheme: Boolean = nightDim || isSystemInDarkTheme(),
    dynamicColor: Boolean = !nightDim,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = RemarkableTypography,
        content = content,
    )
}
