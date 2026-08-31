/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.miguelthemann.remarkable.prefs.BackgroundMode
import com.miguelthemann.remarkable.time.isNightHour

data class DeskModuleColors(
    val surface: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val accent: Color,
    val accentSecondary: Color,
    val outline: Color,
)

val LocalDeskModuleColors = compositionLocalOf {
    DeskModuleColors(
        surface = Color.White.copy(alpha = 0.78f),
        onSurface = Color(0xFF1A1A1A),
        onSurfaceVariant = Color(0xFF616161),
        accent = Color(0xFF006A6A),
        accentSecondary = Color(0xFF7D5260),
        outline = Color(0xFF9E9E9E),
    )
}

@Composable
fun rememberDeskModuleColors(
    darkTheme: Boolean,
    hour: Int,
    backgroundMode: BackgroundMode,
    usePeaks: Boolean,
): DeskModuleColors {
    val scheme = MaterialTheme.colorScheme
    val darkBackground = isNightHour(hour) &&
        (backgroundMode == BackgroundMode.TIME_OF_DAY || usePeaks)
    val useDark = darkTheme || darkBackground

    return remember(darkTheme, hour, backgroundMode, usePeaks, scheme.primary, scheme.tertiary) {
        if (useDark) {
            DeskModuleColors(
                surface = Color(0xFF1C1C1E).copy(alpha = 0.74f),
                onSurface = Color(0xFFF5F5F5),
                onSurfaceVariant = Color(0xFFB0B0B0),
                accent = Color(0xFF90CAF9),
                accentSecondary = Color(0xFFCE93D8),
                outline = Color(0xFF757575),
            )
        } else {
            DeskModuleColors(
                surface = Color.White.copy(alpha = 0.78f),
                onSurface = Color(0xFF1A1A1A),
                onSurfaceVariant = Color(0xFF616161),
                accent = scheme.primary,
                accentSecondary = scheme.tertiary,
                outline = Color(0xFF9E9E9E),
            )
        }
    }
}

@Composable
fun ProvideDeskModuleColors(
    colors: DeskModuleColors,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDeskModuleColors provides colors, content = content)
}

@Composable
fun ModuleSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 16.dp,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit,
) {
    val colors = LocalDeskModuleColors.current
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.surface)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = contentAlignment,
    ) {
        content()
    }
}
