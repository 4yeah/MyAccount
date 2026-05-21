package com.liuhy.myaccount.feature.settings

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * 可长按拖拽重排的纵向列表（自实现，无第三方依赖）。
 *
 * 工作流程：
 * 1. 用户长按某项 → 进入拖动模式（[itemContent] 收到 isDragging=true，可以加阴影/高亮）
 * 2. 上下拖动时，光标位置跨过相邻项「半高 + 1px」时立刻交换，视觉上跟随移动
 * 3. 松手时调用 [onReorder]，传出新的 id 顺序，由调用方持久化到数据库
 *
 * 不依赖 LazyColumn，因为 LazyColumn 内的拖拽实现复杂；分类数量很小（< 30），
 * 用 Column 没有性能问题。
 *
 * @param items 当前外部数据（来自 ViewModel 的 StateFlow）
 * @param keyOf 提取每项的稳定 key，用于追踪「这是哪一项」
 * @param onReorder 拖动结束时回调，参数是新顺序下的 key 列表
 * @param itemContent 每一项的内容（接收 item 本身、isDragging 和 dragHandleModifier 三参）。
 *        调用方应把 [dragHandleModifier] 应用到专门的拖拽手柄上（如 DragHandle 图标），
 *        避免与内部的 clickable / IconButton 产生手势冲突。
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    keyOf: (T) -> Any,
    onReorder: (orderedKeys: List<Any>) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T, isDragging: Boolean, dragHandleModifier: Modifier) -> Unit
) {
    // 本地副本：拖动期间实时重排，仅在松手时同步给外部。
    // 外部 items 变化（如其他用户操作或数据库回流）时，通过 LaunchedEffect 同步重置。
    var workingList by remember { mutableStateOf(items) }
    LaunchedEffect(items) {
        workingList = items
    }

    var draggingKey by remember { mutableStateOf<Any?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    // 记录每项实测高度，给阈值判断用。普通 Map 即可，不必触发重组。
    val itemHeights = remember { mutableMapOf<Any, Int>() }

    Column(modifier = modifier) {
        workingList.forEach { item ->
            val key = keyOf(item)
            val isDragging = key == draggingKey

            // 手势 Modifier：仅附着在调用方指定的拖拽手柄上，
            // 避免与卡片内部的 IconButton/clickable 产生 pointer 消费冲突。
            val dragHandleModifier = remember(key) {
                Modifier.pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            draggingKey = key
                            dragOffsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetY += dragAmount.y

                            val currentList = workingList
                            val currentIndex = currentList.indexOfFirst { keyOf(it) == key }
                            if (currentIndex < 0) return@detectDragGesturesAfterLongPress

                            val height = itemHeights[key] ?: return@detectDragGesturesAfterLongPress
                            val threshold = height * 0.55f

                            when {
                                dragOffsetY > threshold && currentIndex < currentList.size - 1 -> {
                                    workingList = currentList.toMutableList().apply {
                                        val moved = removeAt(currentIndex)
                                        add(currentIndex + 1, moved)
                                    }
                                    // 减去一个 item 高度，让光标视觉上仍贴着拖动项
                                    dragOffsetY -= height
                                }
                                dragOffsetY < -threshold && currentIndex > 0 -> {
                                    workingList = currentList.toMutableList().apply {
                                        val moved = removeAt(currentIndex)
                                        add(currentIndex - 1, moved)
                                    }
                                    dragOffsetY += height
                                }
                            }
                        },
                        onDragEnd = {
                            if (draggingKey != null) {
                                onReorder(workingList.map { keyOf(it) })
                            }
                            draggingKey = null
                            dragOffsetY = 0f
                        },
                        onDragCancel = {
                            draggingKey = null
                            dragOffsetY = 0f
                            workingList = items
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .onSizeChanged { size -> itemHeights[key] = size.height }
                    .zIndex(if (isDragging) 1f else 0f)
                    .offset {
                        IntOffset(
                            x = 0,
                            y = if (isDragging) dragOffsetY.roundToInt() else 0
                        )
                    }
            ) {
                itemContent(item, isDragging, dragHandleModifier)
            }
        }
    }
}
