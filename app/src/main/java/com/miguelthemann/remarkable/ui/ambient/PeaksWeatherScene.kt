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
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.miguelthemann.remarkable.prefs.WeatherEffect
import com.miguelthemann.remarkable.weather.WeatherSnapshot
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Samsung Peaks–inspired full-bleed weather scene: moving sun, drifting clouds,
 * rain / snow / thunder particles driven by weather code or a manual override.
 */
@Composable
fun PeaksWeatherScene(
    now: ZonedDateTime,
    weather: WeatherSnapshot?,
    effectOverride: WeatherEffect,
    modifier: Modifier = Modifier,
) {
    val effect = remember(effectOverride, weather?.weatherCode) {
        resolveEffect(effectOverride, weather?.weatherCode)
    }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { frame ->
                if (last != 0L) {
                    t += ((frame - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                }
                last = frame
            }
        }
    }

    val hour = now.hour + now.minute / 60f
    val clouds = remember { List(7) { CloudSeed(it) } }
    val drops = remember { List(70) { ParticleSeed(it * 17) } }
    val flakes = remember { List(55) { ParticleSeed(it * 31 + 3) } }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawSky(hour, effect)
        if (effect != WeatherEffect.NONE && effect != WeatherEffect.FOG) {
            drawSun(hour, t, effect)
        }
        if (effect == WeatherEffect.FOG || effect == WeatherEffect.CLOUDY ||
            effect == WeatherEffect.RAIN || effect == WeatherEffect.SNOW ||
            effect == WeatherEffect.THUNDER
        ) {
            drawClouds(clouds, t, effect)
        }
        if (effect == WeatherEffect.RAIN || effect == WeatherEffect.THUNDER) {
            drawRain(drops, t)
        }
        if (effect == WeatherEffect.SNOW) {
            drawSnow(flakes, t)
        }
        if (effect == WeatherEffect.THUNDER) {
            drawLightning(t)
        }
        if (effect == WeatherEffect.FOG) {
            drawFog(t)
        }
    }
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
    val speed = 0.015f + (i % 4) * 0.008f
    val phase = i * 0.37f
}

private data class ParticleSeed(val seed: Int) {
    val xFrac = Random(seed).nextFloat()
    val speed = 0.25f + Random(seed + 1).nextFloat() * 0.55f
    val len = 8f + Random(seed + 2).nextFloat() * 18f
    val drift = (Random(seed + 3).nextFloat() - 0.5f) * 0.08f
}

private fun DrawScope.drawSky(hour: Float, effect: WeatherEffect) {
    val (top, bottom) = skyColors(hour, effect)
    drawRect(Brush.verticalGradient(listOf(top, bottom)))
}

private fun skyColors(hour: Float, effect: WeatherEffect): Pair<Color, Color> {
    val base = when {
        hour < 5f || hour >= 21f -> Color(0xFF0B1320) to Color(0xFF1C2541)
        hour < 7f -> Color(0xFFFFB88C) to Color(0xFF87CEEB)
        hour < 17f -> Color(0xFF7EC8E3) to Color(0xFFB8E0F0)
        hour < 19.5f -> Color(0xFFFF8A65) to Color(0xFF5C6BC0)
        else -> Color(0xFF1A237E) to Color(0xFF0D1B2A)
    }
    return when (effect) {
        WeatherEffect.RAIN, WeatherEffect.THUNDER ->
            Color(0xFF546E7A) to Color(0xFF263238)
        WeatherEffect.SNOW ->
            Color(0xFF90A4AE) to Color(0xFFCFD8DC)
        WeatherEffect.FOG ->
            Color(0xFFB0BEC5) to Color(0xFF78909C)
        WeatherEffect.CLOUDY ->
            Color(0xFF90A4AE).copy(alpha = 0.85f).let { c ->
                Color(0xFF78909C) to base.second
            }
        else -> base
    }
}

private fun DrawScope.drawSun(hour: Float, t: Float, effect: WeatherEffect) {
    // Arc across the sky from ~6h to ~20h
    val day = ((hour - 6f) / 14f).coerceIn(0f, 1f)
    val x = size.width * (0.12f + day * 0.76f)
    val y = size.height * (0.55f - sin(day * PI).toFloat() * 0.42f)
    val pulse = 1f + 0.03f * sin(t * 1.2f)
    val radius = size.minDimension * 0.07f * pulse
    val glow = if (effect == WeatherEffect.CLEAR) 0.45f else 0.22f
    drawCircle(Color(0xFFFFF59D).copy(alpha = glow), radius * 2.4f, Offset(x, y))
    drawCircle(Color(0xFFFFEE58), radius * 1.35f, Offset(x, y))
    drawCircle(Color(0xFFFFFDE7), radius, Offset(x, y))
}

private fun DrawScope.drawClouds(clouds: List<CloudSeed>, t: Float, effect: WeatherEffect) {
    val alpha = when (effect) {
        WeatherEffect.CLEAR -> 0.25f
        WeatherEffect.CLOUDY, WeatherEffect.FOG -> 0.75f
        else -> 0.55f
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
    val color = Color(0xFFBBDEFB).copy(alpha = 0.7f)
    drops.forEach { d ->
        val y = ((d.xFrac * 0.3f + t * d.speed) % 1.2f) * size.height
        val x = (d.xFrac + t * d.drift) * size.width
        drawLine(
            color = color,
            start = Offset(x, y),
            end = Offset(x - 4f, y + d.len),
            strokeWidth = 2.2f,
        )
    }
}

private fun DrawScope.drawSnow(flakes: List<ParticleSeed>, t: Float) {
    val color = Color.White.copy(alpha = 0.9f)
    flakes.forEach { d ->
        val y = ((d.xFrac * 0.4f + t * d.speed * 0.35f) % 1.15f) * size.height
        val x = (d.xFrac + sin(t * 1.4f + d.seed) * 0.03f + t * d.drift * 0.4f) *
            size.width
        drawCircle(color, 2.5f + (d.seed % 4), Offset(x, y))
    }
}

private fun DrawScope.drawLightning(t: Float) {
    val flash = sin(t * 7f)
    if (flash > 0.92f) {
        drawRect(Color.White.copy(alpha = (flash - 0.92f) * 4f))
        val path = Path().apply {
            moveTo(size.width * 0.55f, 0f)
            lineTo(size.width * 0.48f, size.height * 0.28f)
            lineTo(size.width * 0.58f, size.height * 0.28f)
            lineTo(size.width * 0.42f, size.height * 0.62f)
        }
        drawPath(path, Color(0xFFE3F2FD).copy(alpha = 0.85f))
    }
}

private fun DrawScope.drawFog(t: Float) {
    val a = 0.18f + 0.05f * sin(t * 0.6f)
    drawRect(Color.White.copy(alpha = a))
    drawRect(
        Brush.verticalGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
        ),
    )
}
