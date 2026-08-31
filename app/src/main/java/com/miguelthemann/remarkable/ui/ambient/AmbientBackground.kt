/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.ambient

import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.miguelthemann.remarkable.prefs.BackgroundMode
import com.miguelthemann.remarkable.ui.theme.fromArgbLong
import com.miguelthemann.remarkable.weather.WeatherSnapshot
import java.io.File
import java.time.ZonedDateTime

data class AmbientPalette(
    val top: Color,
    val bottom: Color,
    val accent: Color,
)

@Composable
fun rememberAmbientPalette(
    mode: BackgroundMode,
    now: ZonedDateTime,
    weather: WeatherSnapshot?,
    accentArgb: Long,
    customBgArgb: Long,
    darkTheme: Boolean,
): AmbientPalette {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    return remember(
        mode,
        now.hour,
        weather?.weatherCode,
        accentArgb,
        customBgArgb,
        darkTheme,
        scheme,
    ) {
        when (mode) {
            BackgroundMode.SOLID -> AmbientPalette(
                top = scheme.background,
                bottom = scheme.surface,
                accent = scheme.primary,
            )
            BackgroundMode.MONET -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val dyn = if (darkTheme) {
                        dynamicDarkColorScheme(context)
                    } else {
                        dynamicLightColorScheme(context)
                    }
                    AmbientPalette(
                        top = dyn.primaryContainer.copy(alpha = 0.55f).composite(dyn.background),
                        bottom = dyn.tertiaryContainer.copy(alpha = 0.4f).composite(dyn.surface),
                        accent = dyn.primary,
                    )
                } else {
                    val accent = Color.fromArgbLong(accentArgb)
                    AmbientPalette(
                        top = accent.copy(alpha = 0.22f).composite(scheme.background),
                        bottom = scheme.surface,
                        accent = accent,
                    )
                }
            }
            BackgroundMode.WEATHER -> weatherPalette(weather?.weatherCode, darkTheme, scheme)
            BackgroundMode.TIME_OF_DAY -> timeOfDayPalette(now.hour, darkTheme)
            BackgroundMode.CUSTOM -> {
                val accent = Color.fromArgbLong(accentArgb)
                AmbientPalette(
                    top = accent.copy(alpha = 0.35f).composite(
                        if (darkTheme) Color(0xFF101418) else Color(0xFFF7F4EF),
                    ),
                    bottom = accent.copy(alpha = 0.12f).composite(
                        if (darkTheme) Color(0xFF1A1F24) else Color(0xFFECE7DF),
                    ),
                    accent = accent,
                )
            }
            BackgroundMode.CUSTOM_COLOR -> {
                val solid = customColorPalette(
                    Color.fromArgbLong(customBgArgb),
                    darkTheme,
                    scheme.primary,
                )
                // Solid fill: same colour top and bottom after light/dark remap.
                AmbientPalette(top = solid.top, bottom = solid.top, accent = solid.accent)
            }
            BackgroundMode.IMAGE -> AmbientPalette(
                top = if (darkTheme) Color(0xFF101418) else Color(0xFFF5F5F5),
                bottom = if (darkTheme) Color(0xFF1A1F24) else Color(0xFFE8E8E8),
                accent = scheme.primary,
            )
        }
    }
}

@Composable
fun AmbientBackground(
    palette: AmbientPalette,
    imagePath: String?,
    useImage: Boolean,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(imagePath, useImage) {
        if (useImage && !imagePath.isNullOrBlank()) {
            val file = File(imagePath)
            if (file.exists()) {
                runCatching {
                    BitmapFactory.decodeFile(file.absolutePath)
                }.getOrNull()
            } else {
                null
            }
        } else {
            null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Scrim so clock text stays readable in both themes.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (darkTheme) {
                            Color.Black.copy(alpha = 0.45f)
                        } else {
                            Color.White.copy(alpha = 0.28f)
                        },
                    ),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(palette.top, palette.bottom))),
            )
        }
    }
}

/** Maps a user-picked “normal” colour to a light or dark ambient gradient. */
fun customColorPalette(base: Color, darkTheme: Boolean, accent: Color): AmbientPalette {
    return if (darkTheme) {
        val deep = if (base.luminance() > 0.45f) base.darken(0.72f) else base.darken(0.35f)
        val deeper = deep.darken(0.18f)
        AmbientPalette(top = deep, bottom = deeper, accent = accent)
    } else {
        val soft = if (base.luminance() < 0.35f) base.lighten(0.45f) else base.lighten(0.08f)
        val softBottom = soft.darken(0.06f)
        AmbientPalette(top = soft, bottom = softBottom, accent = accent)
    }
}

private fun Color.darken(amount: Float): Color = Color(
    red = (red * (1f - amount)).coerceIn(0f, 1f),
    green = (green * (1f - amount)).coerceIn(0f, 1f),
    blue = (blue * (1f - amount)).coerceIn(0f, 1f),
    alpha = alpha,
)

private fun Color.lighten(amount: Float): Color = Color(
    red = (red + (1f - red) * amount).coerceIn(0f, 1f),
    green = (green + (1f - green) * amount).coerceIn(0f, 1f),
    blue = (blue + (1f - blue) * amount).coerceIn(0f, 1f),
    alpha = alpha,
)

private fun timeOfDayPalette(hour: Int, darkTheme: Boolean): AmbientPalette = when (hour) {
    in 5..7 -> AmbientPalette(Color(0xFFFFE0B2), Color(0xFFFFCC80), Color(0xFFFF8A65))
    in 8..16 -> AmbientPalette(
        if (darkTheme) Color(0xFF1A2A3A) else Color(0xFFE3F2FD),
        if (darkTheme) Color(0xFF0E1A24) else Color(0xFFBBDEFB),
        Color(0xFF42A5F5),
    )
    in 17..19 -> AmbientPalette(Color(0xFFFFAB91), Color(0xFFCE93D8), Color(0xFFFF7043))
    else -> AmbientPalette(Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFF778DA9))
}

private fun weatherPalette(
    code: Int?,
    darkTheme: Boolean,
    scheme: androidx.compose.material3.ColorScheme,
): AmbientPalette = when (code) {
    null -> AmbientPalette(scheme.background, scheme.surface, scheme.primary)
    0, 1 -> AmbientPalette(
        if (darkTheme) Color(0xFF1B2A3A) else Color(0xFFFFF8E1),
        if (darkTheme) Color(0xFF0F1720) else Color(0xFFFFECB3),
        Color(0xFFFFB300),
    )
    2, 3 -> AmbientPalette(
        if (darkTheme) Color(0xFF22262B) else Color(0xFFECEFF1),
        if (darkTheme) Color(0xFF15181C) else Color(0xFFCFD8DC),
        Color(0xFF78909C),
    )
    45, 48 -> AmbientPalette(Color(0xFFB0BEC5), Color(0xFF78909C), Color(0xFF546E7A))
    in 51..67, in 80..82 -> AmbientPalette(Color(0xFF1565C0), Color(0xFF0D47A1), Color(0xFF90CAF9))
    in 71..77, in 85..86 -> AmbientPalette(Color(0xFFE3F2FD), Color(0xFF90CAF9), Color(0xFFE1F5FE))
    in 95..99 -> AmbientPalette(Color(0xFF311B92), Color(0xFF1A237E), Color(0xFFB39DDB))
    else -> AmbientPalette(scheme.background, scheme.surface, scheme.primary)
}

private fun Color.composite(background: Color): Color {
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
