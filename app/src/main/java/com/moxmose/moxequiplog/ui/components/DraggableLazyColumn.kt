package com.moxmose.moxequiplog.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T : Any> DraggableLazyColumn(
    modifier: Modifier = Modifier,
    items: List<T>,
    key: (index: Int, item: T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    onDrop: () -> Unit,
    itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    var overscrollJob by remember { mutableStateOf<Job?>(null) }

    val spacing = 16.dp
    val spacingPx = with(LocalDensity.current) { spacing.toPx() }

    // Utilizziamo rememberUpdatedState per evitare che il cambiamento delle lambda
    // provochi la ricreazione del DragDropState durante il drag.
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnDrop by rememberUpdatedState(onDrop)

    val stateHolder = remember(lazyListState) {
        object : DragDropStateHolder {
            override val lazyListState: LazyListState = lazyListState
            override val lazyGridState = null
            override fun onMove(from: Int, to: Int) = currentOnMove(from, to)
            override fun onDrop() = currentOnDrop()
        }
    }

    val dragDropState = remember(stateHolder, spacingPx) { DragDropState(stateHolder, spacingPx) }

    LazyColumn(
        modifier = modifier.pointerInput(lazyListState) {
            detectDragGesturesAfterLongPress(
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragDropState.onDrag(change, dragAmount)

                    if (overscrollJob?.isActive != true) {
                        overscrollJob = dragDropState.checkForOverscroll(scope, dragAmount)
                    }
                },
                onDragStart = { offset -> dragDropState.onDragStart(offset) },
                onDragEnd = { dragDropState.onDragEnd() },
                onDragCancel = { dragDropState.onDragEnd() }
            )
        },
        state = lazyListState,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        itemsIndexed(items, key = key) { index, item ->
            val currentKey = key(index, item)
            val isDragging = dragDropState.isDragging(currentKey)
            val offset by dragDropState.offsetOf(currentKey)

            Box(
                modifier = Modifier
                    .graphicsLayer { translationY = offset }
                    .zIndex(if (isDragging) 1f else 0f)
            ) {
                itemContent(index, item)
            }
        }
    }
}
