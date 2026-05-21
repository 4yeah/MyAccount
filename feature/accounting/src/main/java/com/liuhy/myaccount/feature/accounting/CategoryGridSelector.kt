/**
 * 分类选择器（记账页使用）。
 *
 * 顶部横向滚动显示一级分类图标，下方网格展示对应的二级分类。
 * 当选中一级分类后，下方自动切换显示其子分类供用户选择。
 * 每个二级分类卡片右上角有「+」按钮，可跳转到分类管理新增。
 */
package com.liuhy.myaccount.feature.accounting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.liuhy.myaccount.core.common.getIconVector
import com.liuhy.myaccount.core.common.parseColorHex
import com.liuhy.myaccount.core.data.model.Category

/**
 * 网格分类选择器（类似主流记账 App 的布局）
 *
 * 一级分类以 5 列网格展示，点击后在对应行下方展开二级分类。
 * 一级分类网格末尾有新增按钮，二级分类行末尾也有新增按钮。
 */
@Composable
fun CategoryGridSelector(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long) -> Unit,
    onAddCategory: (Category?) -> Unit
) {
    // 排序：按用户设置的 sortOrder，相同 sortOrder 的按 name 兜底
    val level1Categories = categories.filter { it.parentId == null }.sortedWith(
        compareBy({ it.sortOrder }, { it.name })
    )

    // 展开的一级分类 id。规则（优先级从高到低）：
    // 1. 如果 selectedCategoryId 命中某个二级分类 → 展开它的父级
    // 2. 如果 selectedCategoryId 命中某个一级分类 → 展开它自己
    // 3. 用户手动点过别的一级分类 → 沿用上一次的展开
    // 4. 都没有 → 默认展开第一个一级分类
    var expandedCategoryId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(selectedCategoryId, categories) {
        if (level1Categories.isEmpty()) return@LaunchedEffect

        val targetFromSelection = selectedCategoryId?.let { id ->
            categories.firstOrNull { it.id == id }?.let { sel ->
                sel.parentId ?: sel.id
            }
        }
        expandedCategoryId =
            targetFromSelection ?: expandedCategoryId ?: level1Categories.first().id
    }

    // 把"末尾新增按钮"作为虚拟一格加进网格末尾，
    // 这样它就能和真实分类一样进入 chunked(5) 的同一个网格里，宽度自动一致。
    val gridSlots: List<Category?> = level1Categories + listOf<Category?>(null)
    val rows = gridSlots.chunked(COLUMNS_PER_ROW)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEachIndexed { rowIndex, rowSlots ->

            // 一级分类行（含末尾"新增"虚拟格）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowSlots.forEach { parent ->
                    if (parent == null) {
                        AddCategoryCell(
                            modifier = Modifier.weight(1f),
                            contentDescription = "新增一级分类",
                            onClick = { onAddCategory(null) }
                        )
                    } else {
                        // 点击一级分类只切换展开（永不折叠回去）。
                        // 之前用 if (expanded == parent.id) null else parent.id，
                        // 导致再次点同一个一级分类会把已展开的二级分类收起来——
                        // 用户的预期是「不要让二级分类被隐藏」。
                        // 一级分类高亮条件：直接选中自己，或选中了它的某个二级分类
                        val isParentOfSelected = selectedCategoryId?.let { id ->
                            categories.find { it.id == id }?.parentId == parent.id
                        } ?: false
                        Level1CategoryCell(
                            modifier = Modifier.weight(1f),
                            category = parent,
                            isSelected = selectedCategoryId == parent.id || isParentOfSelected,
                            onClick = { expandedCategoryId = parent.id }
                        )
                    }
                }
                // 不足 COLUMNS_PER_ROW 个时用透明占位填满，保证每个真实格子宽度都是 1/5。
                repeat(COLUMNS_PER_ROW - rowSlots.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // 当前行内若有展开的一级分类，把它的二级分类紧跟在下方
            rowSlots.filterNotNull().forEach { parent ->
                val children = categories.filter { it.parentId == parent.id }
                val isExpanded = expandedCategoryId == parent.id
                if (isExpanded && children.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Level2CategoryRow(
                        parent = parent,
                        children = children,
                        selectedCategoryId = selectedCategoryId,
                        onCategorySelected = onCategorySelected,
                        onAddChild = { onAddCategory(parent) }
                    )
                }
            }
        }
    }
}

private const val COLUMNS_PER_ROW = 5

@Composable
private fun Level1CategoryCell(
    modifier: Modifier = Modifier,
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        parseColorHex(category.colorHex).copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    Column(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = getIconVector(category.iconName),
            contentDescription = category.name,
            modifier = Modifier.size(24.dp),
            tint = if (isSelected) parseColorHex(category.colorHex) else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun AddCategoryCell(
    modifier: Modifier = Modifier,
    contentDescription: String,
    onClick: () -> Unit
) {
    // 视觉上和一级分类格保持完全一致的尺寸 + 圆角 + 内边距，只是图标换成 +
    Column(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "新增",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun Level2CategoryRow(
    parent: Category,
    children: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long) -> Unit,
    onAddChild: () -> Unit
) {
    val parentColor = parseColorHex(parent.colorHex)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧彩色竖线，暗示层级关系
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(parentColor)
        )
        Spacer(modifier = Modifier.width(8.dp))

        // 二级分类 Chip 横向滚动
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            children.forEach { child ->
                val childSelected = selectedCategoryId == child.id
                val childColor = parseColorHex(child.colorHex)
                Surface(
                    onClick = { onCategorySelected(child.id) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (childSelected) childColor.copy(alpha = 0.15f) else Color.Transparent,
                    border = if (childSelected) null else androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    tonalElevation = if (childSelected) 2.dp else 0.dp
                ) {
                    Text(
                        text = child.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (childSelected) childColor else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            // 新增按钮
            Surface(
                onClick = onAddChild,
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "新增二级分类",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "新增",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
