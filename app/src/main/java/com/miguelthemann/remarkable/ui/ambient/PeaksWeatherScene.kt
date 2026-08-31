/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.ambient

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.miguelthemann.remarkable.prefs.WeatherEffect
import com.miguelthemann.remarkable.time.isNightHour
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Samsung Peaks–inspired weather scene.
 *
 * Takes [hourOfDay] (not a full timestamp) so 250ms clock ticks do not invalidate this
 * composition. Animation advances on the render clock via [withFrameNanos].
 * If this looks smooth, that is luck + spite. If it doesn't, blame the particles.
 */
@Composable
fun PeaksWeatherScene(
    hourOfDay: Float,
    weatherCode: Int?,
    effectOverride: WeatherEffect,
    modifier: Modifier = Modifier,
) {
    val effect = remember(effectOverride, weatherCode) {
        resolveEffect(effectOverride, weatherCode)
    }
    // Stable across seconds within the same minute when parent passes hour+minute/60.
    val hour = remember(hourOfDay) { hourOfDay }

    var timeSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    timeSec += (now - last) / 1_000_000_000f
                }
                last = now
            }
        }
    }

    val clouds = remember { List(5) { CloudSeed(it) } }
    val drops = remember { List(36) { ParticleSeed(it * 17) } }
    val flakes = remember { List(28) { ParticleSeed(it * 31 + 3) } }

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = timeSec
        drawSky(hour, effect)
        if (effect != WeatherEffect.NONE && effect != WeatherEffect.FOG) {
            if (isNightHour(hour)) {
                drawMoon(hour, t)
            } else {
                drawSun(hour, t, effect)
            }
        }
        when (effect) {
            WeatherEffect.CLEAR -> drawClouds(clouds, t, effect)
            WeatherEffect.CLOUDY, WeatherEffect.RAIN, WeatherEffect.SNOW,
            WeatherEffect.THUNDER, WeatherEffect.FOG,
            -> drawClouds(clouds, t, effect)
            else -> Unit
        }
        when (effect) {
            WeatherEffect.RAIN, WeatherEffect.THUNDER -> drawRain(drops, t)
            WeatherEffect.SNOW -> drawSnow(flakes, t)
            else -> Unit
        }
        if (effect == WeatherEffect.THUNDER) drawLightning(t)
        if (effect == WeatherEffect.FOG) drawFog(t)
    }
}

/** Backward-compatible overload used by older call sites. */
@Composable
fun PeaksWeatherScene(
    now: java.time.ZonedDateTime,
    weather: com.miguelthemann.remarkable.weather.WeatherSnapshot?,
    effectOverride: WeatherEffect,
    modifier: Modifier = Modifier,
) {
    PeaksWeatherScene(
        hourOfDay = now.hour + now.minute / 60f,
        weatherCode = weather?.weatherCode,
        effectOverride = effectOverride,
        modifier = modifier,
    )
}

fun resolveEffect(override: WeatherEffect, code: Int?): WeatherEffect {
    if (override != WeatherEffect.AUTO) return override
    return when (code) {
        null -> WeatherEffect.CLEAR
        0, 1 -> WeatherEffect.CLEAR
        2, 3 -> WeatherEffect.CLOUDY
        45, 48 -> WeatherEffect.FOG
        in 51..67, in 80..82 -> WeatherEffect.RAIN
        in 71..77, in 85..86 -> WeatherEffect.SNOW
        in 95..99 -> WeatherEffect.THUNDER
        else -> WeatherEffect.CLOUDY
    }
}

private data class CloudSeed(val i: Int) {
    val yFrac = 0.12f + (i % 5) * 0.07f
    val size = 0.10f + (i % 3) * 0.04f
    val speed = 0.018f + (i % 4) * 0.008f
    val phase = i * 0.37f
}

private data class ParticleSeed(val seed: Int) {
    val xFrac = Random(seed).nextFloat()
    val speed = 0.35f + Random(seed + 1).nextFloat() * 0.55f
    val len = 10f + Random(seed + 2).nextFloat() * 16f
    val drift = (Random(seed + 3).nextFloat() - 0.5f) * 0.08f
}

private fun DrawScope.drawSky(hour: Float, effect: WeatherEffect) {
    val (top, bottom) = skyColors(hour, effect)
    drawRect(Brush.verticalGradient(listOf(top, bottom)))
}

private fun skyColors(hour: Float, effect: WeatherEffect): Pair<Color, Color> {
    val base = when {
        isNightHour(hour) -> Color(0xFF0B1320) to Color(0xFF1C2541)
        hour < 7f -> Color(0xFFFFB88C) to Color(0xFF87CEEB)
        hour < 17f -> Color(0xFF7EC8E3) to Color(0xFFB8E0F0)
        hour < 19.5f -> Color(0xFFFF8A65) to Color(0xFF5C6BC0)
        else -> Color(0xFF1A237E) to Color(0xFF0D1B2A)
    }
    return when (effect) {
        WeatherEffect.RAIN, WeatherEffect.THUNDER -> Color(0xFF546E7A) to Color(0xFF263238)
        WeatherEffect.SNOW -> Color(0xFF90A4AE) to Color(0xFFCFD8DC)
        WeatherEffect.FOG -> Color(0xFFB0BEC5) to Color(0xFF78909C)
        WeatherEffect.CLOUDY -> Color(0xFF78909C) to base.second
        else -> base
    }
}

private fun DrawScope.drawSun(hour: Float, t: Float, effect: WeatherEffect) {
    val day = ((hour - 6f) / 14f).coerceIn(0f, 1f)
    val x = size.width * (0.12f + day * 0.76f)
    val y = size.height * (0.55f - sin(day * PI).toFloat() * 0.42f)
    val pulse = 1f + 0.025f * sin(t * 1.2f)
    val radius = size.minDimension * 0.07f * pulse
    val glow = if (effect == WeatherEffect.CLEAR) 0.45f else 0.2f
    drawCircle(Color(0xFFFFF59D).copy(alpha = glow), radius * 2.4f, Offset(x, y))
    drawCircle(Color(0xFFFFEE58), radius * 1.35f, Offset(x, y))
    drawCircle(Color(0xFFFFFDE7), radius, Offset(x, y))
}

private fun DrawScope.drawMoon(hour: Float, t: Float) {
    val night = ((hour - 20f).mod(24f) / 10f).coerceIn(0f, 1f)
    val x = size.width * (0.78f - night * 0.56f)
    val y = size.height * (0.2f + sin(night * PI).toFloat() * 0.06f)
    val pulse = 1f + 0.015f * sin(t * 0.8f)
    val radius = size.minDimension * 0.055f * pulse
    drawCircle(Color(0xFFE8EAF6).copy(alpha = 0.22f), radius * 2.4f, Offset(x, y))
    drawCircle(Color(0xFFF5F5F5), radius * 1.15f, Offset(x, y))
    drawCircle(Color(0xFF0B1320), radius * 1.05f, Offset(x + radius * 0.42f, y - radius * 0.12f))
}

private fun DrawScope.drawClouds(clouds: List<CloudSeed>, t: Float, effect: WeatherEffect) {
    val alpha = when (effect) {
        WeatherEffect.CLEAR -> 0.22f
        WeatherEffect.CLOUDY, WeatherEffect.FOG -> 0.72f
        else -> 0.5f
    }
    val color = Color.White.copy(alpha = alpha)
    clouds.forEach { c ->
        val travel = ((c.phase + t * c.speed) % 1.35f) - 0.2f
        val cx = size.width * travel
        val cy = size.height * c.yFrac
        val r = size.minDimension * c.size
        drawCircle(color, r, Offset(cx, cy))
        drawCircle(color, r * 0.75f, Offset(cx + r * 0.7f, cy + r * 0.1f))
        drawCircle(color, r * 0.65f, Offset(cx - r * 0.65f, cy + r * 0.15f))
    }
}

private fun DrawScope.drawRain(drops: List<ParticleSeed>, t: Float) {
    val color = Color(0xFFBBDEFB).copy(alpha = 0.65f)
    drops.forEach { d ->
        val y = ((d.xFrac * 0.3f + t * d.speed * 0.45f) % 1.2f) * size.height
        val x = (d.xFrac + t * d.drift * 0.05f).mod(1f) * size.width
        drawLine(
            color = color,
            start = Offset(x, y),
            end = Offset(x - 3.5f, y + d.len),
            strokeWidth = 2f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawSnow(flakes: List<ParticleSeed>, t: Float) {
    val color = Color.White.copy(alpha = 0.88f)
    flakes.forEach { d ->
        val y = ((d.xFrac * 0.4f + t * d.speed * 0.12f) % 1.15f) * size.height
        val x = (d.xFrac + sin(t * 0.9f + d.seed) * 0.03f + t * d.drift * 0.04f).mod(1f) * size.width
        drawCircle(color, 2.2f + (d.seed % 3), Offset(x, y))
    }
}

private fun DrawScope.drawLightning(t: Float) {
    val flash = sin(t * 3.2f)
    if (flash > 0.93f) {
        drawRect(Color.White.copy(alpha = ((flash - 0.93f) * 5f).coerceIn(0f, 0.35f)))
        val path = Path().apply {
            moveTo(size.width * 0.55f, 0f)
            lineTo(size.width * 0.48f, size.height * 0.28f)
            lineTo(size.width * 0.58f, size.height * 0.28f)
            lineTo(size.width * 0.42f, size.height * 0.62f)
        }
        drawPath(path, Color(0xFFE3F2FD).copy(alpha = 0.8f))
    }
}

private fun DrawScope.drawFog(t: Float) {
    val a = 0.16f + 0.04f * sin(t * 0.4f)
    drawRect(Color.White.copy(alpha = a))
}
