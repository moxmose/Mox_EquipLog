package com.moxmose.moxequiplog.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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

    val dragDropState = remember { DragDropState(spacingPx) }
    dragDropState.onMove = onMove
    dragDropState.onDrop = onDrop
    dragDropState.lazyListState = lazyListState

    LaunchedEffect(items) {
        if (dragDropState.isDragging) {
            dragDropState.reset()
        }
    }

    LazyColumn(
        modifier = modifier.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragDropState.onDrag(dragAmount)

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

private class DragDropState(private val spacing: Float) {
    var onMove: (from: Int, to: Int) -> Unit = { _, _ -> }
    var onDrop: () -> Unit = {}
    lateinit var lazyListState: LazyListState

    var draggedDistance by mutableFloatStateOf(0f)
        private set
    var draggedItemKey by mutableStateOf<Any?>(null)
        private set

    val isDragging: Boolean get() = draggedItemKey != null

    private var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)

    // Offset di "ancoraggio" che viene aggiornato dopo ogni swap.
    private var dragAnchorOffset by mutableFloatStateOf(0f)

    private var initialDragOffsetInElement by mutableFloatStateOf(0f)

    fun isDragging(itemKey: Any): Boolean = itemKey == draggedItemKey

    fun offsetOf(itemKey: Any): State<Float> = derivedStateOf {
        if (itemKey != draggedItemKey) 0f else draggedDistance
    }

    fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { offset.y.toInt() in it.offset..(it.offset + it.size) }
            ?.also {
                currentIndexOfDraggedItem = it.index
                draggedItemKey = it.key
                initialDragOffsetInElement = offset.y - it.offset
                dragAnchorOffset = it.offset.toFloat() // Imposta l'ancoraggio iniziale
                draggedDistance = 0f
            }
    }

    fun onDrag(dragAmount: Offset) {
        draggedDistance += dragAmount.y

        val currentDraggedIndex = currentIndexOfDraggedItem ?: return

        // Posizione del dito calcolata usando l'ancoraggio aggiornato
        val absoluteFingerY = dragAnchorOffset + draggedDistance + initialDragOffsetInElement

        val targetItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            if (item.index == currentDraggedIndex) return@firstOrNull false
            when {
                dragAmount.y > 0 && item.index > currentDraggedIndex ->
                    absoluteFingerY > item.offset + item.size / 2
                dragAmount.y < 0 && item.index < currentDraggedIndex ->
                    absoluteFingerY < item.offset + item.size / 2
                else -> false
            }
        }

        if (targetItem != null) {
            val from = currentDraggedIndex
            val to = targetItem.index
            if (from != to) {
                // Calcola il delta per la compensazione e per aggiornare l'ancoraggio
                val diff = if (from < to) (targetItem.size + spacing) else -(targetItem.size + spacing)

                // Compensa la distanza di trascinamento per evitare il salto
                draggedDistance -= diff

                // AGGIORNA L'ANCORAGGIO per il calcolo successivo
                dragAnchorOffset += diff

                // Esegui lo spostamento
                onMove(from, to)
                currentIndexOfDraggedItem = to
            }
        }
    }

    fun onDragEnd() {
        onDrop()
        reset()
    }

    fun checkForOverscroll(scope: kotlinx.coroutines.CoroutineScope, dragAmount: Offset): Job? {
        val currentElementInfo = lazyListState.layoutInfo.visibleItemsInfo.find { it.key == draggedItemKey } ?: return null
        val itemSize = currentElementInfo.size

        val itemTranslateY = draggedDistance
        val startOffset = currentElementInfo.offset + itemTranslateY
        val endOffset = startOffset + itemSize

        val overscrollAmount = when {
            dragAmount.y > 0 && endOffset > lazyListState.layoutInfo.viewportEndOffset - 100 ->
                dragAmount.y * 0.2f
            dragAmount.y < 0 && startOffset < lazyListState.layoutInfo.viewportStartOffset + 100 ->
                dragAmount.y * 0.2f
            else -> 0f
        }

        return if (overscrollAmount != 0f) {
            scope.launch { lazyListState.scrollBy(overscrollAmount) }
        } else null
    }

    fun reset() {
        draggedDistance = 0f
        draggedItemKey = null
        currentIndexOfDraggedItem = null
        initialDragOffsetInElement = 0f
        dragAnchorOffset = 0f
    }
}
