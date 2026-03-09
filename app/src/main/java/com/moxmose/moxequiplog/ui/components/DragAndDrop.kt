package com.moxmose.moxequiplog.ui.components

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

interface DragDropStateHolder {
    val lazyListState: LazyListState?
    val lazyGridState: LazyGridState?
    fun onMove(from: Int, to: Int)
    fun onDrop()
}

class DragDropState(private val stateHolder: DragDropStateHolder, private val spacing: Float) {
    var draggedDistanceY by mutableFloatStateOf(0f)
        private set
    var draggedDistanceX by mutableFloatStateOf(0f)
        private set
    var draggedItemKey by mutableStateOf<Any?>(null)
        private set

    val isDragging: Boolean get() = draggedItemKey != null

    private var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)
    private var initialDragOffsetInElementY by mutableFloatStateOf(0f)
    private var initialDragOffsetInElementX by mutableFloatStateOf(0f)
    private var dragStartPointerY by mutableFloatStateOf(0f)
    private var dragStartPointerX by mutableFloatStateOf(0f)
    
    // Proportional Swap Guard
    private var lastActionedPointerY by mutableFloatStateOf(0f)
    private var lastActionedPointerX by mutableFloatStateOf(0f)
    private var lastActionedItemSizeY by mutableFloatStateOf(0f)
    private var lastActionedItemSizeX by mutableFloatStateOf(0f)

    fun isDragging(itemKey: Any): Boolean = itemKey == draggedItemKey

    fun offsetOf(itemKey: Any): State<Offset> = derivedStateOf {
        if (itemKey != draggedItemKey) return@derivedStateOf Offset.Zero
        
        val currentPointerY = dragStartPointerY + draggedDistanceY
        val currentPointerX = dragStartPointerX + draggedDistanceX
        
        val listState = stateHolder.lazyListState
        if (listState != null) {
            val item = listState.layoutInfo.visibleItemsInfo.find { it.key == itemKey }
            if (item != null) {
                return@derivedStateOf Offset(0f, currentPointerY - initialDragOffsetInElementY - item.offset)
            }
        }

        val gridState = stateHolder.lazyGridState
        if (gridState != null) {
            val item = gridState.layoutInfo.visibleItemsInfo.find { it.key == itemKey }
            if (item != null) {
                return@derivedStateOf Offset(
                    currentPointerX - initialDragOffsetInElementX - item.offset.x,
                    currentPointerY - initialDragOffsetInElementY - item.offset.y
                )
            }
        }
        
        Offset(draggedDistanceX, draggedDistanceY)
    }

    fun onDragStart(offset: Offset) {
        stateHolder.lazyListState?.layoutInfo?.visibleItemsInfo
            ?.firstOrNull { offset.y.toInt() in it.offset..(it.offset + it.size) }
            ?.also { item ->
                currentIndexOfDraggedItem = item.index
                draggedItemKey = item.key
                initialDragOffsetInElementY = offset.y - item.offset
                initialDragOffsetInElementX = 0f
                dragStartPointerY = offset.y
                dragStartPointerX = offset.x
                lastActionedPointerY = offset.y
                lastActionedPointerX = offset.x
                lastActionedItemSizeY = item.size.toFloat()
                lastActionedItemSizeX = 0f
                draggedDistanceY = 0f
                draggedDistanceX = 0f
            }

        stateHolder.lazyGridState?.layoutInfo?.visibleItemsInfo
            ?.firstOrNull { offset.y.toInt() in it.offset.y..(it.offset.y + it.size.height) && offset.x.toInt() in it.offset.x..(it.offset.x + it.size.width) }
            ?.also { item ->
                currentIndexOfDraggedItem = item.index
                draggedItemKey = item.key
                initialDragOffsetInElementY = offset.y - item.offset.y
                initialDragOffsetInElementX = offset.x - item.offset.x
                dragStartPointerY = offset.y
                dragStartPointerX = offset.x
                lastActionedPointerY = offset.y
                lastActionedPointerX = offset.x
                lastActionedItemSizeY = item.size.height.toFloat()
                lastActionedItemSizeX = item.size.width.toFloat()
                draggedDistanceY = 0f
                draggedDistanceX = 0f
            }
    }

    fun onDrag(change: PointerInputChange, dragAmount: Offset, scope: CoroutineScope) {
        val currentPointerY = change.position.y
        val currentPointerX = change.position.x
        draggedDistanceY = currentPointerY - dragStartPointerY
        draggedDistanceX = currentPointerX - dragStartPointerX
        
        val fromIndex = currentIndexOfDraggedItem ?: return

        // Swap Guard bidimensionale
        val movementSinceLastSwap = sqrt(
            (currentPointerY - lastActionedPointerY).let { it * it } + 
            (currentPointerX - lastActionedPointerX).let { it * it }
        )
        val threshold = if (lastActionedItemSizeY > 0) {
             (if (lastActionedItemSizeX > 0) (lastActionedItemSizeX + lastActionedItemSizeY)/2 else lastActionedItemSizeY) * 0.4f
        } else 5f
        
        if (movementSinceLastSwap < threshold) return

        val listState = stateHolder.lazyListState
        if (listState != null) {
            val wasAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
            findTarget(currentPointerY, dragAmount, listState.layoutInfo.visibleItemsInfo, fromIndex)?.also { targetItem ->
                if (fromIndex != targetItem.index) {
                    stateHolder.onMove(fromIndex, targetItem.index)
                    currentIndexOfDraggedItem = targetItem.index
                    lastActionedPointerY = currentPointerY
                    lastActionedPointerX = currentPointerX
                    lastActionedItemSizeY = targetItem.size.toFloat()
                    if (wasAtTop && (fromIndex == 0 || targetItem.index == 0)) {
                        scope.launch { listState.scrollToItem(0, 0) }
                    }
                }
            }
            return
        }

        val gridState = stateHolder.lazyGridState
        if (gridState != null) {
            val wasAtTop = gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
            findTargetGrid(currentPointerX, currentPointerY, gridState.layoutInfo.visibleItemsInfo, fromIndex)?.also { targetItem ->
                if (fromIndex != targetItem.index) {
                    stateHolder.onMove(fromIndex, targetItem.index)
                    currentIndexOfDraggedItem = targetItem.index
                    lastActionedPointerY = currentPointerY
                    lastActionedPointerX = currentPointerX
                    lastActionedItemSizeY = targetItem.size.height.toFloat()
                    lastActionedItemSizeX = targetItem.size.width.toFloat()
                    if (wasAtTop && (fromIndex == 0 || targetItem.index == 0)) {
                        scope.launch { gridState.scrollToItem(0, 0) }
                    }
                }
            }
        }
    }

    private fun findTarget(fingerY: Float, dragAmount: Offset, items: List<LazyListItemInfo>, currentIdx: Int): LazyListItemInfo? {
        return if (dragAmount.y > 0) {
            items.firstOrNull { it.index == currentIdx + 1 && fingerY > it.offset + it.size / 2 }
        } else {
            items.firstOrNull { it.index == currentIdx - 1 && fingerY < it.offset + it.size / 2 }
        }
    }

    private fun findTargetGrid(fingerX: Float, fingerY: Float, items: List<LazyGridItemInfo>, currentIdx: Int): LazyGridItemInfo? {
        // Nella griglia cerchiamo l'elemento il cui centro è più vicino alla posizione del dito
        return items.firstOrNull { item ->
            item.index != currentIdx &&
            fingerX.toInt() in item.offset.x..(item.offset.x + item.size.width) &&
            fingerY.toInt() in item.offset.y..(item.offset.y + item.size.height)
        }
    }

    fun onDragEnd() {
        stateHolder.onDrop()
        reset()
    }

    fun checkForOverscroll(scope: CoroutineScope, dragAmount: Offset): Job? {
        val listState = stateHolder.lazyListState
        val gridState = stateHolder.lazyGridState
        val key = draggedItemKey ?: return null

        val (itemSize, viewportStart, viewportEnd) = if (listState != null) {
            listState.layoutInfo.visibleItemsInfo.find { it.key == key }?.let {
                listOf(it.size.toFloat(), listState.layoutInfo.viewportStartOffset.toFloat(), listState.layoutInfo.viewportEndOffset.toFloat())
            } ?: return null
        } else if (gridState != null) {
            gridState.layoutInfo.visibleItemsInfo.find { it.key == key }?.let {
                listOf(it.size.height.toFloat(), gridState.layoutInfo.viewportStartOffset.toFloat(), gridState.layoutInfo.viewportEndOffset.toFloat())
            } ?: return null
        } else {
            return null
        }

        val absolutePos = dragStartPointerY + draggedDistanceY - initialDragOffsetInElementY
        
        val overscroll = when {
            dragAmount.y > 0 && absolutePos + itemSize > viewportEnd - 100 -> dragAmount.y * 0.2f
            dragAmount.y < 0 && absolutePos < viewportStart + 100 -> dragAmount.y * 0.2f
            else -> 0f
        }

        return if (overscroll != 0f) {
            scope.launch {
                listState?.scrollBy(overscroll)
                gridState?.scrollBy(overscroll)
            }
        } else null
    }

    private fun reset() {
        draggedDistanceY = 0f
        draggedDistanceX = 0f
        draggedItemKey = null
        currentIndexOfDraggedItem = null
        initialDragOffsetInElementY = 0f
        initialDragOffsetInElementX = 0f
        dragStartPointerY = 0f
        dragStartPointerX = 0f
        lastActionedPointerY = 0f
        lastActionedPointerX = 0f
        lastActionedItemSizeY = 0f
        lastActionedItemSizeX = 0f
    }
}
