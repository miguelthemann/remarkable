/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.miguelthemann.remarkable.prefs.ModuleOffsets
import kotlin.math.roundToInt

/**
 * Desk widget that can be rearranged and resized.
 *
 * Outside edit mode: long-press then drag (claims the gesture so Settings behind never steals it).
 * In edit mode: drag body to move; drag a corner diagonally to resize.
 */
@Composable
fun DraggableModule(
    fracX: Float,
    fracY: Float,
    scale: Float,
    parentSize: IntSize,
    editMode: Boolean,
    onEnterEditMode: () -> Unit,
    onMoved: (Float, Float) -> Unit,
    onScaled: (Float) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var drag by remember { mutableStateOf(Offset.Zero) }
    var liveScale by remember(scale) { mutableFloatStateOf(scale) }
    val density = LocalDensity.current
    val moduleColors = LocalDeskModuleColors.current

    val displayScale = if (editMode) liveScale * 1.02f else liveScale

    val baseX = if (size.width > 0 && parentSize.width > 0) {
        fracX * parentSize.width - size.width / 2f
    } else {
        0f
    }
    val baseY = if (size.height > 0 && parentSize.height > 0) {
        fracY * parentSize.height - size.height / 2f
    } else {
        0f
    }

    fun commitDrag() {
        if (parentSize.width > 0 && parentSize.height > 0 && size.width > 0 && size.height > 0) {
            val cx = (baseX + drag.x + size.width / 2f) / parentSize.width
            val cy = (baseY + drag.y + size.height / 2f) / parentSize.height
            onMoved(cx.coerceIn(0.05f, 0.95f), cy.coerceIn(0.05f, 0.95f))
        }
        drag = Offset.Zero
    }

    fun commitScale() {
        onScaled(liveScale.coerceIn(ModuleOffsets.MinScale, ModuleOffsets.MaxScale))
    }

    Box(
        modifier = modifier
            .zIndex(if (editMode) 5f else 2f)
            .offset {
                IntOffset(
                    (baseX + drag.x).roundToInt(),
                    (baseY + drag.y).roundToInt(),
                )
            }
            .graphicsLayer {
                scaleX = displayScale
                scaleY = displayScale
            },
    ) {
        Box(
            modifier = Modifier
                .onSizeChanged { size = it }
                .then(
                    if (editMode) {
                        Modifier
                            .shadow(12.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                    } else {
                        Modifier
                    },
                )
                .padding(8.dp)
                .pointerInput(editMode, parentSize, fracX, fracY) {
                    if (editMode) {
                        detectDragGestures(
                            onDragEnd = { commitDrag() },
                            onDragCancel = { drag = Offset.Zero },
                            onDrag = { change, amount ->
                                change.consume()
                                drag += amount
                            },
                        )
                    } else {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                onEnterEditMode()
                            },
                            onDragEnd = { commitDrag() },
                            onDragCancel = { drag = Offset.Zero },
                            onDrag = { change, amount ->
                                change.consume()
                                drag += amount
                            },
                        )
                    }
                },
        ) {
            content()
        }

        if (editMode) {
            val sensitivity = with(density) { 180.dp.toPx() }
            val accent = moduleColors.accent
            ResizeHandle(
                corner = ResizeCorner.TopStart,
                accent = accent,
                onResize = { amount ->
                    val delta = (-amount.x - amount.y) / sensitivity
                    liveScale = (liveScale + delta)
                        .coerceIn(ModuleOffsets.MinScale, ModuleOffsets.MaxScale)
                },
                onResizeEnd = { commitScale() },
            )
            ResizeHandle(
                corner = ResizeCorner.TopEnd,
                accent = accent,
                onResize = { amount ->
                    val delta = (amount.x - amount.y) / sensitivity
                    liveScale = (liveScale + delta)
                        .coerceIn(ModuleOffsets.MinScale, ModuleOffsets.MaxScale)
                },
                onResizeEnd = { commitScale() },
            )
            ResizeHandle(
                corner = ResizeCorner.BottomStart,
                accent = accent,
                onResize = { amount ->
                    val delta = (-amount.x + amount.y) / sensitivity
                    liveScale = (liveScale + delta)
                        .coerceIn(ModuleOffsets.MinScale, ModuleOffsets.MaxScale)
                },
                onResizeEnd = { commitScale() },
            )
            ResizeHandle(
                corner = ResizeCorner.BottomEnd,
                accent = accent,
                onResize = { amount ->
                    val delta = (amount.x + amount.y) / sensitivity
                    liveScale = (liveScale + delta)
                        .coerceIn(ModuleOffsets.MinScale, ModuleOffsets.MaxScale)
                },
                onResizeEnd = { commitScale() },
            )
        }
    }
}

private enum class ResizeCorner {
    TopStart, TopEnd, BottomStart, BottomEnd,
}

@Composable
private fun BoxScope.ResizeHandle(
    corner: ResizeCorner,
    accent: Color,
    onResize: (Offset) -> Unit,
    onResizeEnd: () -> Unit,
) {
    val alignment = when (corner) {
        ResizeCorner.TopStart -> Alignment.TopStart
        ResizeCorner.TopEnd -> Alignment.TopEnd
        ResizeCorner.BottomStart -> Alignment.BottomStart
        ResizeCorner.BottomEnd -> Alignment.BottomEnd
    }
    val contentAlignment = alignment
    val inset = with(LocalDensity.current) { 6.dp.roundToPx() }

    Box(
        modifier = Modifier
            .align(alignment)
            .offset {
                when (corner) {
                    ResizeCorner.TopStart -> IntOffset(-inset, -inset)
                    ResizeCorner.TopEnd -> IntOffset(inset, -inset)
                    ResizeCorner.BottomStart -> IntOffset(-inset, inset)
                    ResizeCorner.BottomEnd -> IntOffset(inset, inset)
                }
            }
            .size(36.dp)
            .zIndex(20f)
            .pointerInput(corner) {
                detectDragGestures(
                    onDragEnd = { onResizeEnd() },
                    onDragCancel = { onResizeEnd() },
                    onDrag = { change, amount ->
                        change.consume()
                        onResize(amount)
                    },
                )
            },
        contentAlignment = contentAlignment,
    ) {
        Canvas(Modifier.size(20.dp)) {
            val stroke = 2.5f.dp.toPx()
            val len = size.minDimension
            val halo = accent.copy(alpha = 0.35f)
            val line = accent.copy(alpha = 0.95f)
            val cap = StrokeCap.Round

            fun drawL(
                hStart: Offset,
                hEnd: Offset,
                vStart: Offset,
                vEnd: Offset,
            ) {
                drawLine(halo, hStart, hEnd, stroke * 2.4f, cap)
                drawLine(halo, vStart, vEnd, stroke * 2.4f, cap)
                drawLine(line, hStart, hEnd, stroke, cap)
                drawLine(line, vStart, vEnd, stroke, cap)
            }

            when (corner) {
                ResizeCorner.TopStart -> drawL(
                    Offset(0f, stroke / 2f),
                    Offset(len, stroke / 2f),
                    Offset(stroke / 2f, 0f),
                    Offset(stroke / 2f, len),
                )
                ResizeCorner.TopEnd -> drawL(
                    Offset(0f, stroke / 2f),
                    Offset(len, stroke / 2f),
                    Offset(len - stroke / 2f, 0f),
                    Offset(len - stroke / 2f, len),
                )
                ResizeCorner.BottomStart -> drawL(
                    Offset(0f, len - stroke / 2f),
                    Offset(len, len - stroke / 2f),
                    Offset(stroke / 2f, 0f),
                    Offset(stroke / 2f, len),
                )
                ResizeCorner.BottomEnd -> drawL(
                    Offset(0f, len - stroke / 2f),
                    Offset(len, len - stroke / 2f),
                    Offset(len - stroke / 2f, 0f),
                    Offset(len - stroke / 2f, len),
                )
            }
        }
    }
}
