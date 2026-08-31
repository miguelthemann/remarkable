/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.burnin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Shifts content slowly and optionally draws a sparse black-pixel mask ("smart pixels")
 * to reduce OLED burn-in on static desk-clock UI.
 */
@Composable
fun BurnInProtectedContent(
    enabled: Boolean,
    shiftDp: Int,
    intervalSec: Int,
    smartPixels: Boolean,
    smartPixelsStrength: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(enabled, intervalSec) {
        if (!enabled) return@LaunchedEffect
        while (true) {
            delay(intervalSec.coerceAtLeast(15) * 1000L)
            step++
        }
    }

    val density = LocalDensity.current
    val maxPx = with(density) { shiftDp.coerceIn(2, 24).dp.toPx() }
    val offset = if (enabled) {
        val angle = (step % 8) * (Math.PI / 4.0)
        IntOffset(
            x = (kotlin.math.cos(angle) * maxPx).roundToInt(),
            y = (kotlin.math.sin(angle) * maxPx).roundToInt(),
        )
    } else {
        IntOffset.Zero
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { offset },
        ) {
            content()
        }
        if (enabled && smartPixels) {
            SmartPixelsOverlay(strength = smartPixelsStrength, seed = step)
        }
    }
}

@Composable
private fun SmartPixelsOverlay(strength: Float, seed: Int) {
    val density = strength.coerceIn(0.1f, 0.8f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val rng = Random(seed * 31L + size.width.toLong())
        val cell = 6.dp.toPx()
        val cols = (size.width / cell).toInt().coerceAtLeast(1)
        val rows = (size.height / cell).toInt().coerceAtLeast(1)
        val black = Color.Black.copy(alpha = 0.55f * density)
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                // Checker + random culling keeps average luminance down without a solid veil.
                val checker = (x + y + seed) % 2 == 0
                if (checker && rng.nextFloat() < density) {
                    drawCircle(
                        color = black,
                        radius = 1.2.dp.toPx(),
                        center = Offset((x + 0.5f) * cell, (y + 0.5f) * cell),
                    )
                }
            }
        }
    }
}
