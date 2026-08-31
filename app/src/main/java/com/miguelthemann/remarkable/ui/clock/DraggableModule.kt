/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.ui.clock

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * @param editMode when true, the module can be dragged immediately.
 * Long-press to enter edit mode is handled by the parent via [onBoundsChanged] hit-testing
 * because Compose gesture arenas are a dark forest and we got lost there once.
 */
@Composable
fun DraggableModule(
    fracX: Float,
    fracY: Float,
    parentSize: IntSize,
    editMode: Boolean,
    onBoundsChanged: (Rect) -> Unit,
    onMoved: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var drag by remember { mutableStateOf(Offset.Zero) }

    val baseX = (fracX * parentSize.width - size.width / 2f)
    val baseY = (fracY * parentSize.height - size.height / 2f)

    fun commitDrag() {
        if (parentSize.width > 0 && parentSize.height > 0 && size.width > 0) {
            val cx = (baseX + drag.x + size.width / 2f) / parentSize.width
            val cy = (baseY + drag.y + size.height / 2f) / parentSize.height
            onMoved(cx.coerceIn(0.05f, 0.95f), cy.coerceIn(0.05f, 0.95f))
        }
        drag = Offset.Zero
    }

    Box(
        modifier = modifier
            .zIndex(if (editMode) 2f else 1f)
            .onSizeChanged { size = it }
            .onGloballyPositioned { onBoundsChanged(it.boundsInParent()) }
            .offset {
                IntOffset(
                    (baseX + drag.x).roundToInt(),
                    (baseY + drag.y).roundToInt(),
                )
            }
            .graphicsLayer {
                scaleX = if (editMode) 1.04f else 1f
                scaleY = if (editMode) 1.04f else 1f
            }
            .then(
                if (editMode) {
                    Modifier
                        .shadow(10.dp, RoundedCornerShape(20.dp))
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            RoundedCornerShape(20.dp),
                        )
                        .clip(RoundedCornerShape(20.dp))
                } else {
                    Modifier
                },
            )
            .padding(4.dp)
            .then(
                if (editMode) {
                    // Only consume pointers while editing so long-press can reach the parent otherwise.
                    Modifier.pointerInput(parentSize, fracX, fracY) {
                        detectDragGestures(
                            onDragEnd = { commitDrag() },
                            onDragCancel = { drag = Offset.Zero },
                            onDrag = { change, amount ->
                                change.consume()
                                drag += amount
                            },
                        )
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        content()
    }
}
