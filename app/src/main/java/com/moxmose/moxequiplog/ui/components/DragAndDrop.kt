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
    private var dragAnchorOffset by mutableFloatStateOf(0f)
    private var initialDragOffsetInElement by mutableFloatStateOf(0f)

    fun isDragging(itemKey: Any): Boolean = itemKey == draggedItemKey

    fun offsetOf(itemKey: Any): State<Float> = derivedStateOf {
        if (itemKey != draggedItemKey) 0f else draggedDistance
    }

    fun onDragStart(offset: Offset) {
        stateHolder.lazyListState?.layoutInfo?.visibleItemsInfo
            ?.firstOrNull { offset.y.toInt() in it.offset..(it.offset + it.size) }
            ?.also { item ->
                currentIndexOfDraggedItem = item.index
                draggedItemKey = item.key
                initialDragOffsetInElement = offset.y - item.offset
                dragAnchorOffset = item.offset.toFloat()
                draggedDistance = 0f
            }

        stateHolder.lazyGridState?.layoutInfo?.visibleItemsInfo
            ?.firstOrNull { offset.y.toInt() in it.offset.y..(it.offset.y + it.size.height) && offset.x.toInt() in it.offset.x..(it.offset.x + it.size.width) }
            ?.also { item ->
                currentIndexOfDraggedItem = item.index
                draggedItemKey = item.key
                initialDragOffsetInElement = offset.y - item.offset.y
                dragAnchorOffset = item.offset.y.toFloat()
                draggedDistance = 0f
            }
    }

    fun onDrag(change: PointerInputChange, dragAmount: Offset) {
        draggedDistance += dragAmount.y
        val fromIndex = currentIndexOfDraggedItem ?: return
        val absoluteFingerY = dragAnchorOffset + draggedDistance + initialDragOffsetInElement

        val listState = stateHolder.lazyListState
        if (listState != null) {
            findTarget(absoluteFingerY, dragAmount, listState.layoutInfo.visibleItemsInfo, fromIndex)?.also { targetItem ->
                val toIndex = targetItem.index
                if (fromIndex != toIndex) {
                    val diff = if (fromIndex < toIndex) (targetItem.size + spacing) else -(targetItem.size + spacing)
                    draggedDistance -= diff
                    dragAnchorOffset += diff
                    stateHolder.onMove(fromIndex, toIndex)
                    currentIndexOfDraggedItem = toIndex
                }
            }
            return
        }

        val gridState = stateHolder.lazyGridState
        if (gridState != null) {
            findTargetGrid(absoluteFingerY, dragAmount, gridState.layoutInfo.visibleItemsInfo, fromIndex)?.also { targetItem ->
                val toIndex = targetItem.index
                if (fromIndex != toIndex) {
                    val diff = if (fromIndex < toIndex) (targetItem.size.height + spacing) else -(targetItem.size.height + spacing)
                    draggedDistance -= diff
                    dragAnchorOffset += diff
                    stateHolder.onMove(fromIndex, toIndex)
                    currentIndexOfDraggedItem = toIndex
                }
            }
        }
    }

    private fun findTarget(absFingerY: Float, dragAmount: Offset, items: List<LazyListItemInfo>, currentIdx: Int): LazyListItemInfo? {
        return if (dragAmount.y > 0) { // dragging down
            items.firstOrNull { it.index > currentIdx && absFingerY > it.offset + it.size / 2 }
        } else { // dragging up
            items.findLast { it.index < currentIdx && absFingerY < it.offset + it.size / 2 }
        }
    }

    private fun findTargetGrid(absFingerY: Float, dragAmount: Offset, items: List<LazyGridItemInfo>, currentIdx: Int): LazyGridItemInfo? {
        return if (dragAmount.y > 0) { // dragging down
            items.firstOrNull { it.index > currentIdx && absFingerY > it.offset.y + it.size.height / 2 }
        } else { // dragging up
            items.findLast { it.index < currentIdx && absFingerY < it.offset.y + it.size.height / 2 }
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

        val (itemOffset, itemSize, viewportStart, viewportEnd) = if (listState != null) {
            listState.layoutInfo.visibleItemsInfo.find { it.key == key }?.let {
                listOf(it.offset.toFloat(), it.size.toFloat(), listState.layoutInfo.viewportStartOffset.toFloat(), listState.layoutInfo.viewportEndOffset.toFloat())
            } ?: return null
        } else if (gridState != null) {
            gridState.layoutInfo.visibleItemsInfo.find { it.key == key }?.let {
                listOf(it.offset.y.toFloat(), it.size.height.toFloat(), gridState.layoutInfo.viewportStartOffset.toFloat(), gridState.layoutInfo.viewportEndOffset.toFloat())
            } ?: return null
        } else {
            return null
        }

        val absolutePos = itemOffset + draggedDistance
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
        dragAnchorOffset = 0f
    }
}
