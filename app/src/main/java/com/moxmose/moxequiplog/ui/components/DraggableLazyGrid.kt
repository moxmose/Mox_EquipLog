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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun <T> DraggableLazyGrid(
    items: List<T>,
    onMove: (Pair<Int, Int>) -> Unit,
    modifier: Modifier = Modifier,
    canDrag: Boolean = true,
    key: ((index: Int, item: T) -> Any)? = null,
    gridState: LazyGridState = rememberLazyGridState(),
    itemContent: @Composable (T) -> Unit,
) {
    var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableStateOf(Offset.Zero) }

    val pointerInputModifier = if (canDrag) {
        Modifier.pointerInput(items) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    gridState.layoutInfo.visibleItemsInfo
                        .firstOrNull {
                            offset.y.toInt() in it.offset.y..(it.offset.y + it.size.height) &&
                                    offset.x.toInt() in it.offset.x..(it.offset.x + it.size.width)
                        }
                        ?.let { draggingItemIndex = it.index }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    draggingOffset += dragAmount
                    val currentIndex = draggingItemIndex ?: return@detectDragGesturesAfterLongPress

                    gridState.layoutInfo.visibleItemsInfo
                        .firstOrNull { item ->
                            val center = Offset(item.offset.x + item.size.width / 2f, item.offset.y + item.size.height / 2f)
                            (change.position - center).getDistance() < item.size.width / 2f
                        }?.let { target ->
                            if (currentIndex != target.index) {
                                onMove(Pair(currentIndex, target.index))
                                draggingItemIndex = target.index
                            }
                        }
                },
                onDragEnd = { draggingItemIndex = null; draggingOffset = Offset.Zero },
                onDragCancel = { draggingItemIndex = null; draggingOffset = Offset.Zero }
            )
        }
    } else Modifier

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 80.dp),
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.then(pointerInputModifier)
    ) {
        itemsIndexed(items, key = key) { index, item ->
            Box(modifier = Modifier.zIndex(if (draggingItemIndex == index) 1f else 0f)) {
                itemContent(item)
            }
        }
    }
}
