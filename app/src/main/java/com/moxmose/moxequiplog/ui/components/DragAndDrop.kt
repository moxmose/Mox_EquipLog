package com.moxmose.moxequiplog.ui.components

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
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

    // Offset di "ancoraggio" che viene aggiornato dopo ogni swap.
    private var dragAnchorOffset by mutableFloatStateOf(0f)

    private var initialDragOffsetInElement by mutableFloatStateOf(0f)

    fun isDragging(itemKey: Any): Boolean = itemKey == draggedItemKey

    fun offsetOf(itemKey: Any): State<Float> = derivedStateOf {
        if (itemKey != draggedItemKey) 0f else draggedDistance
    }

    fun onDragStart(offset: Offset) {
        val listState = stateHolder.lazyListState
        val gridState = stateHolder.lazyGridState

        if (listState != null) {
            listState.layoutInfo.visibleItemsInfo
                .firstOrNull { offset.y.toInt() in it.offset..(it.offset + it.size) }
                ?.also {
                    currentIndexOfDraggedItem = it.index
                    draggedItemKey = it.key
                    initialDragOffsetInElement = offset.y - it.offset
                    dragAnchorOffset = it.offset.toFloat()
                    draggedDistance = 0f
                }
        } else if (gridState != null) {
            gridState.layoutInfo.visibleItemsInfo
                .firstOrNull { offset.y.toInt() in it.offset.y..(it.offset.y + it.size.height) && offset.x.toInt() in it.offset.x..(it.offset.x + it.size.width) }
                ?.also {
                    currentIndexOfDraggedItem = it.index
                    draggedItemKey = it.key
                    initialDragOffsetInElement = offset.y - it.offset.y
                    dragAnchorOffset = it.offset.y.toFloat()
                    draggedDistance = 0f
                }
        }
    }

    fun onDrag(dragAmount: Offset) {
        draggedDistance += dragAmount.y

        val currentDraggedIndex = currentIndexOfDraggedItem ?: return
        val absoluteFingerY = dragAnchorOffset + draggedDistance + initialDragOffsetInElement

        val listState = stateHolder.lazyListState
        val gridState = stateHolder.lazyGridState

        if (listState != null) {
            val targetItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                if (item.index == currentDraggedIndex) return@firstOrNull false
                when {
                    dragAmount.y > 0 && item.index > currentDraggedIndex -> absoluteFingerY > item.offset + item.size / 2
                    dragAmount.y < 0 && item.index < currentDraggedIndex -> absoluteFingerY < item.offset + item.size / 2
                    else -> false
                }
            }

            if (targetItem != null && currentDraggedIndex != targetItem.index) {
                val diff = if (currentDraggedIndex < targetItem.index) (targetItem.size + spacing) else -(targetItem.size + spacing)
                draggedDistance -= diff
                dragAnchorOffset += diff
                stateHolder.onMove(currentDraggedIndex, targetItem.index)
                currentIndexOfDraggedItem = targetItem.index
            }
        } else if (gridState != null) {
            val targetItem = gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                if (item.index == currentDraggedIndex) return@firstOrNull false
                when {
                    dragAmount.y > 0 && item.index > currentDraggedIndex -> absoluteFingerY > item.offset.y + item.size.height / 2
                    dragAmount.y < 0 && item.index < currentDraggedIndex -> absoluteFingerY < item.offset.y + item.size.height / 2
                    else -> false
                }
            }
            if (targetItem != null && currentDraggedIndex != targetItem.index) {
                val diff = if (currentDraggedIndex < targetItem.index) (targetItem.size.height + spacing) else -(targetItem.size.height + spacing)
                draggedDistance -= diff
                dragAnchorOffset += diff
                stateHolder.onMove(currentDraggedIndex, targetItem.index)
                currentIndexOfDraggedItem = targetItem.index
            }
        }
    }

    fun onDragEnd() {
        stateHolder.onDrop()
        reset()
    }

    fun checkForOverscroll(scope: CoroutineScope, dragAmount: Offset): Job? {
        val listState = stateHolder.lazyListState
        val gridState = stateHolder.lazyGridState

        val viewportStartOffset: Int
        val viewportEndOffset: Int
        var itemOffsetY = 0
        var itemHeight = 0

        if (listState != null) {
            val element = listState.layoutInfo.visibleItemsInfo.find { it.key == draggedItemKey } ?: return null
            itemOffsetY = element.offset
            itemHeight = element.size
            viewportStartOffset = listState.layoutInfo.viewportStartOffset
            viewportEndOffset = listState.layoutInfo.viewportEndOffset
        } else if (gridState != null) {
            val element = gridState.layoutInfo.visibleItemsInfo.find { it.key == draggedItemKey } ?: return null
            itemOffsetY = element.offset.y
            itemHeight = element.size.height
            viewportStartOffset = gridState.layoutInfo.viewportStartOffset
            viewportEndOffset = gridState.layoutInfo.viewportEndOffset
        } else {
            return null
        }

        val startOffset = itemOffsetY + draggedDistance
        val endOffset = startOffset + itemHeight

        val overscrollAmount = when {
            dragAmount.y > 0 && endOffset > viewportEndOffset - 100 -> dragAmount.y * 0.2f
            dragAmount.y < 0 && startOffset < viewportStartOffset + 100 -> dragAmount.y * 0.2f
            else -> 0f
        }

        return if (overscrollAmount != 0f) {
            scope.launch {
                listState?.scrollBy(overscrollAmount)
                gridState?.scrollBy(overscrollAmount)
            }
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
