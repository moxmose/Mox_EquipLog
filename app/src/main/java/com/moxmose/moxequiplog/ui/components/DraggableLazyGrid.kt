package com.moxmose.moxequiplog.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job

@Composable
fun <T : Any> DraggableLazyGrid(
    items: List<T>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onDrop: () -> Unit = {},
    modifier: Modifier = Modifier,
    canDrag: Boolean = true,
    key: ((index: Int, item: T) -> Any)? = null,
    gridState: LazyGridState = rememberLazyGridState(),
    itemContent: @Composable (T) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var overscrollJob by remember { mutableStateOf<Job?>(null) }
    
    val spacing = 12.dp
    val spacingPx = with(LocalDensity.current) { spacing.toPx() }

    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnDrop by rememberUpdatedState(onDrop)

    val stateHolder = remember(gridState) {
        object : DragDropStateHolder {
            override val lazyListState = null
            override val lazyGridState: LazyGridState = gridState
            override fun onMove(from: Int, to: Int) = currentOnMove(from, to)
            override fun onDrop() = currentOnDrop()
        }
    }

    val dragDropState = remember(stateHolder, spacingPx) { DragDropState(stateHolder, spacingPx) }

    val pointerInputModifier = if (canDrag) {
        Modifier.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset -> dragDropState.onDragStart(offset) },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragDropState.onDrag(change, dragAmount, scope)

                    if (overscrollJob?.isActive != true) {
                        overscrollJob = dragDropState.checkForOverscroll(scope, dragAmount)
                    }
                },
                onDragEnd = { dragDropState.onDragEnd() },
                onDragCancel = { dragDropState.onDragEnd() }
            )
        }
    } else Modifier

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 80.dp),
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = modifier.then(pointerInputModifier)
    ) {
        itemsIndexed(items, key = key) { index, item ->
            val itemKey = key?.invoke(index, item) ?: index
            val isDragging = dragDropState.isDragging(itemKey)
            val offsetState by dragDropState.offsetOf(itemKey)

            Box(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationX = offsetState.x
                        translationY = offsetState.y
                    }
            ) {
                itemContent(item)
            }
        }
    }
}
