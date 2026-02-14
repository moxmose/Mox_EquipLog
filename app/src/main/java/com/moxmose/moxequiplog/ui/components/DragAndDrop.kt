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

interface DragDropStateHolder {
    val lazyListState: LazyListState?
    val lazyGridState: LazyGridState?
    fun onMove(from: Int, to: Int)
    fun onDrop()
}

class DragDropState(private val stateHolder: DragDropStateHolder, private val spacing: Float) {
    var draggedDistance by mutableFloatStateOf(0f)
        private set
    var draggedItemKey by mutableStateOf<Any?>(null)
        private set

    val isDragging: Boolean get() = draggedItemKey != null

    private var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)
    private var initialDragOffsetInElement by mutableFloatStateOf(0f)
    private var dragStartPointerY by mutableFloatStateOf(0f)
    
    // Proportional Swap Guard
    private var lastActionedPointerY by mutableFloatStateOf(0f)
    private var lastActionedItemSize by mutableFloatStateOf(0f)

    fun isDragging(itemKey: Any): Boolean = itemKey == draggedItemKey

    fun offsetOf(itemKey: Any): State<Float> = derivedStateOf {
        if (itemKey != draggedItemKey) return@derivedStateOf 0f
        
        val currentPointerY = dragStartPointerY + draggedDistance
        val listState = stateHolder.lazyListState
        if (listState != null) {
            val item = listState.layoutInfo.visibleItemsInfo.find { it.key == itemKey }
            if (item != null) {
                return@derivedStateOf currentPointerY - initialDragOffsetInElement - item.offset
            }
        }

        val gridState = stateHolder.lazyGridState
        if (gridState != null) {
            val item = gridState.layoutInfo.visibleItemsInfo.find { it.key == itemKey }
            if (item != null) {
                return@derivedStateOf currentPointerY - initialDragOffsetInElement - item.offset.y
            }
        }
        
        draggedDistance
    }

    fun onDragStart(offset: Offset) {
        stateHolder.lazyListState?.layoutInfo?.visibleItemsInfo
            ?.firstOrNull { offset.y.toInt() in it.offset..(it.offset + it.size) }
            ?.also { item ->
                currentIndexOfDraggedItem = item.index
                draggedItemKey = item.key
                initialDragOffsetInElement = offset.y - item.offset
                dragStartPointerY = offset.y
                lastActionedPointerY = offset.y
                lastActionedItemSize = item.size.toFloat()
                draggedDistance = 0f
            }

        stateHolder.lazyGridState?.layoutInfo?.visibleItemsInfo
            ?.firstOrNull { offset.y.toInt() in it.offset.y..(it.offset.y + it.size.height) && offset.x.toInt() in it.offset.x..(it.offset.x + it.size.width) }
            ?.also { item ->
                currentIndexOfDraggedItem = item.index
                draggedItemKey = item.key
                initialDragOffsetInElement = offset.y - item.offset.y
                dragStartPointerY = offset.y
                lastActionedPointerY = offset.y
                lastActionedItemSize = item.size.height.toFloat()
                draggedDistance = 0f
            }
    }

    fun onDrag(change: PointerInputChange, dragAmount: Offset) {
        val currentPointerY = change.position.y
        draggedDistance = currentPointerY - dragStartPointerY
        
        val fromIndex = currentIndexOfDraggedItem ?: return

        // Swap Guard: richiede un movimento fisico del dito pari ad almeno il 40% della dimensione dell'elemento
        // Questo impedisce swap a catena causati dallo scroll automatico mentre il dito è quasi fermo.
        val movementSinceLastSwap = abs(currentPointerY - lastActionedPointerY)
        val threshold = if (lastActionedItemSize > 0) (lastActionedItemSize + spacing) * 0.4f else 5f
        
        if (movementSinceLastSwap < threshold) return

        val listState = stateHolder.lazyListState
        if (listState != null) {
            findTarget(currentPointerY, dragAmount, listState.layoutInfo.visibleItemsInfo, fromIndex)?.also { targetItem ->
                if (fromIndex != targetItem.index) {
                    stateHolder.onMove(fromIndex, targetItem.index)
                    currentIndexOfDraggedItem = targetItem.index
                    lastActionedPointerY = currentPointerY
                    lastActionedItemSize = targetItem.size.toFloat()
                }
            }
            return
        }

        val gridState = stateHolder.lazyGridState
        if (gridState != null) {
            findTargetGrid(currentPointerY, dragAmount, gridState.layoutInfo.visibleItemsInfo, fromIndex)?.also { targetItem ->
                if (fromIndex != targetItem.index) {
                    stateHolder.onMove(fromIndex, targetItem.index)
                    currentIndexOfDraggedItem = targetItem.index
                    lastActionedPointerY = currentPointerY
                    lastActionedItemSize = targetItem.size.height.toFloat()
                }
            }
        }
    }

    private fun findTarget(fingerY: Float, dragAmount: Offset, items: List<LazyListItemInfo>, currentIdx: Int): LazyListItemInfo? {
        return if (dragAmount.y > 0) { // dragging down
            items.firstOrNull { it.index == currentIdx + 1 && fingerY > it.offset + it.size / 2 }
        } else { // dragging up
            items.firstOrNull { it.index == currentIdx - 1 && fingerY < it.offset + it.size / 2 }
        }
    }

    private fun findTargetGrid(fingerY: Float, dragAmount: Offset, items: List<LazyGridItemInfo>, currentIdx: Int): LazyGridItemInfo? {
        // Nella griglia permettiamo scambi con i vicini immediati (stessa riga o righe adiacenti)
        return if (dragAmount.y > 0) { // dragging down
            items.firstOrNull { it.index > currentIdx && it.index <= currentIdx + 3 && fingerY > it.offset.y + it.size.height / 2 }
        } else { // dragging up
            items.findLast { it.index < currentIdx && it.index >= currentIdx - 3 && fingerY < it.offset.y + it.size.height / 2 }
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

        val currentPointerY = dragStartPointerY + draggedDistance
        val absolutePos = currentPointerY - initialDragOffsetInElement
        
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
        draggedDistance = 0f
        draggedItemKey = null
        currentIndexOfDraggedItem = null
        initialDragOffsetInElement = 0f
        dragStartPointerY = 0f
        lastActionedPointerY = 0f
        lastActionedItemSize = 0f
    }
}
