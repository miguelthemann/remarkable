/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.clock

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * Desk widget that can be rearranged.
 *
 * Outside edit mode: long-press then drag (claims the gesture so Settings behind never steals it).
 * In edit mode: drag immediately. I don't know how Compose gesture arenas work but this does, so gg.
 */
@Composable
fun DraggableModule(
    fracX: Float,
    fracY: Float,
    parentSize: IntSize,
    editMode: Boolean,
    onEnterEditMode: () -> Unit,
    onMoved: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var drag by remember { mutableStateOf(Offset.Zero) }

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

    Box(
        modifier = modifier
            .zIndex(if (editMode) 5f else 2f)
            .onSizeChanged { size = it }
            .offset {
                IntOffset(
                    (baseX + drag.x).roundToInt(),
                    (baseY + drag.y).roundToInt(),
                )
            }
            .graphicsLayer {
                scaleX = if (editMode) 1.05f else 1f
                scaleY = if (editMode) 1.05f else 1f
            }
            .then(
                if (editMode) {
                    Modifier
                        .shadow(12.dp, RoundedCornerShape(20.dp))
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            RoundedCornerShape(20.dp),
                        )
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
}
