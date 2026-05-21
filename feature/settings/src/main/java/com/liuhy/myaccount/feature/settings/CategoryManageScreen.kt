package com.liuhy.myaccount.feature.settings

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liuhy.myaccount.core.common.getIconVector
import com.liuhy.myaccount.core.common.parseColorHex
import com.liuhy.myaccount.core.data.model.Category
import com.liuhy.myaccount.core.data.model.Transaction
import com.liuhy.myaccount.core.data.model.TransactionType

/**
 * 分类管理页面
 * 
 * 支持树形展开查看一级和二级分类，可添加、编辑、删除分类
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManageScreen(
    onBack: () -> Unit,
    initialParentId: Long? = null,
    viewModel: CategoryManageViewModel = viewModel(factory = CategoryManageViewModel.Factory)
) {
    val level1Categories by viewModel.level1Categories.collectAsState()
    val currentType by viewModel.currentType.collectAsState()
    // 必须在 Composable 里收集 allChildren，否则二级分类异步加载完成后 UI 不会重组。
    // 之前用 viewModel.getChildren(...) 普通函数调用读 .value，
    // Compose 看不到对 allChildren 的依赖，于是永远拿不到最新的二级分类列表。
    val allChildren by viewModel.allChildren.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var draftCategory by remember { mutableStateOf<Category?>(null) }
    var deleteOptionCategory by remember { mutableStateOf<Category?>(null) }
    var transactionCount by remember { mutableStateOf(0) }
    var showRecycleBin by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasHandledInitial by remember { mutableStateOf(false) }
    // 记录当前正在排序的二级分类 parentId，确保同时只有一个二级分类处于排序模式
    var activeReorderParentId by remember { mutableStateOf<Long?>(null) }
    // 查看关联记录弹窗
    var showTransactionsDialog by remember { mutableStateOf(false) }
    var categoryTransactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    // 二次确认删除
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingDeleteMessage by remember { mutableStateOf("") }
    var pendingDeleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val scope = rememberCoroutineScope()
    
    // 自动打开新增对话框逻辑：
    // - initialParentId == null：从设置页进入，不弹窗
    // - initialParentId == -1L：从记账页「新增一级分类」入口跳过来，打开新增一级分类对话框
    // - initialParentId > 0：从记账页「新增二级分类」入口跳过来，打开新增二级分类对话框
    LaunchedEffect(initialParentId, level1Categories) {
        if (hasHandledInitial || level1Categories.isEmpty()) return@LaunchedEffect
        hasHandledInitial = true

        when (initialParentId) {
            null -> {
                // 从设置页进入，不弹窗
                return@LaunchedEffect
            }
            -1L -> {
                // 新增一级分类
                draftCategory = Category(
                    id = 0,
                    name = "",
                    iconName = "category",
                    colorHex = "#9E9E9E",
                    type = currentType,
                    parentId = null,
                    isDefault = false
                )
                showAddDialog = true
            }
            else -> {
                // 新增二级分类
                val parent = level1Categories.find { it.id == initialParentId }
                if (parent != null) {
                    draftCategory = Category(
                        id = 0,
                        name = "",
                        iconName = parent.iconName,
                        colorHex = parent.colorHex,
                        type = parent.type,
                        parentId = parent.id,
                        isDefault = false
                    )
                    showAddDialog = true
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = "分类管理",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    draftCategory = Category(
                        id = 0,
                        name = "",
                        iconName = "category",
                        colorHex = "#9E9E9E",
                        type = currentType,
                        parentId = null,
                        isDefault = false
                    )
                    showAddDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加一级分类")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 支出/收入切换 + 回收站
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = currentType == TransactionType.EXPENSE,
                    onClick = { viewModel.setCurrentType(TransactionType.EXPENSE) },
                    label = { Text("支出") }
                )
                FilterChip(
                    selected = currentType == TransactionType.INCOME,
                    onClick = { viewModel.setCurrentType(TransactionType.INCOME) },
                    label = { Text("收入") }
                )
                Spacer(modifier = Modifier.weight(1f))
                val hiddenCount = allChildren.count { !it.isVisible && level1Categories.any { l1 -> l1.id == it.parentId && l1.type == currentType } }
                TextButton(
                    onClick = { if (hiddenCount > 0) showRecycleBin = true },
                    enabled = hiddenCount > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "回收站",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("回收站${if (hiddenCount > 0) " ($hiddenCount)" else ""}")
                }
            }
            
            // 顶部小提示，告诉用户「分类支持长按拖动调整顺序」。
            // 没有这个提示绝大多数用户不会发现这个交互。
            Text(
                text = "长按一级分类可拖动排序；长按二级分类弹出左右移动按钮；删除按钮变灰表示不可删除",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                ReorderableColumn(
                    items = level1Categories,
                    keyOf = { it.id },
                    onReorder = { orderedKeys ->
                        @Suppress("UNCHECKED_CAST")
                        val ids = orderedKeys as List<Long>
                        viewModel.applyLevel1Order(ids)
                    }
                ) { category, isDragging, dragHandleModifier ->
                    Column {
                        CategoryTreeItem(
                            category = category,
                            children = allChildren.filter { it.parentId == category.id && it.isVisible },
                            isDragging = isDragging,
                            dragHandleModifier = dragHandleModifier,
                            onEdit = { editingCategory = it },
                            onDelete = { cat ->
                                scope.launch {
                                    val count = viewModel.getTransactionCountByCategory(cat.id)
                                    transactionCount = count
                                    deleteOptionCategory = cat
                                }
                            },
                            onAddChild = { parent ->
                                draftCategory = Category(
                                    id = 0,
                                    name = "",
                                    iconName = parent.iconName,
                                    colorHex = parent.colorHex,
                                    type = parent.type,
                                    parentId = parent.id,
                                    isDefault = false
                                )
                                showAddDialog = true
                            },
                            onMoveChild = { parentId, fromIndex, toIndex ->
                                viewModel.moveLevel2(parentId, fromIndex, toIndex)
                            },
                            activeReorderParentId = activeReorderParentId,
                            onReorderModeChange = { parentId, inMode ->
                                activeReorderParentId = if (inMode) parentId else null
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // 添加/编辑对话框
    if (showAddDialog || editingCategory != null) {
        AddEditCategoryDialog(
            isEdit = editingCategory != null,
            initialCategory = editingCategory ?: draftCategory,
            parentCategories = level1Categories,
            onCategorySaved = { category ->
                if (editingCategory != null) {
                    viewModel.updateCategory(category)
                } else {
                    viewModel.addCategory(
                        name = category.name,
                        iconName = category.iconName,
                        colorHex = category.colorHex,
                        type = category.type,
                        parentId = category.parentId
                    )
                }
                showAddDialog = false
                editingCategory = null
                draftCategory = null
            },
            onDismiss = {
                showAddDialog = false
                editingCategory = null
                draftCategory = null
            }
        )
    }

    // 删除选项对话框（有记录时提供查看记录/删除记录/隐藏选项，无记录时直接删除）
    if (deleteOptionCategory != null) {
        val cat = deleteOptionCategory!!
        AlertDialog(
            onDismissRequest = { deleteOptionCategory = null },
            title = { Text("删除分类") },
            text = {
                Log.i("CategoryManage", "Dialog shown: transactionCount=$transactionCount, cat=${cat.name}")
                Column {
                    if (transactionCount > 0) {
                        Text("分类 \"${cat.name}\" 下有 ${transactionCount} 条记录，请选择操作方式：")
                        Spacer(modifier = Modifier.height(16.dp))
                        // 查看关联记录
                        Button(
                            onClick = {
                                scope.launch {
                                    val list = viewModel.getTransactionsByCategory(cat.id)
                                    categoryTransactions = list
                                    showTransactionsDialog = true
                                    deleteOptionCategory = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("查看关联记录")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // 删除分类及记录（触发二次确认）
                        Button(
                            onClick = {
                                Log.i("CategoryManage", "Delete with transactions button clicked: cat.id=${cat.id}")
                                pendingDeleteMessage = "确定要删除分类 \"${cat.name}\" 及其关联的 ${transactionCount} 条记录吗？此操作不可撤销。"
                                pendingDeleteAction = {
                                    Log.i("CategoryManage", "pendingDeleteAction invoked: cat.id=${cat.id}")
                                    viewModel.deleteCategoryWithTransactions(
                                        cat.id,
                                        onSuccess = { deleteOptionCategory = null },
                                        onError = { msg ->
                                            errorMessage = msg
                                            deleteOptionCategory = null
                                        }
                                    )
                                }
                                showDeleteConfirm = true
                                deleteOptionCategory = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("删除分类及${transactionCount}条记录")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                Log.i("CategoryManage", "Hide button clicked: cat.id=${cat.id}")
                                viewModel.toggleVisibility(cat.id)
                                deleteOptionCategory = null
                                errorMessage = "分类已隐藏，可在回收站中恢复"
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("隐藏分类（保留记录）")
                        }
                    } else {
                        Text("确定要删除分类 \"${cat.name}\" 吗？")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                pendingDeleteMessage = "确定要删除分类 \"${cat.name}\" 吗？此操作不可撤销。"
                                pendingDeleteAction = {
                                    viewModel.deleteCategory(
                                        cat.id,
                                        onSuccess = { deleteOptionCategory = null },
                                        onError = { msg ->
                                            errorMessage = msg
                                            deleteOptionCategory = null
                                        }
                                    )
                                }
                                showDeleteConfirm = true
                                deleteOptionCategory = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("删除")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { deleteOptionCategory = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("取消")
                }
            }
        )
    }

    // 查看关联记录弹窗
    if (showTransactionsDialog) {
        AlertDialog(
            onDismissRequest = { showTransactionsDialog = false },
            title = { Text("关联记录") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (categoryTransactions.isEmpty()) {
                        Text("没有关联记录")
                    } else {
                        categoryTransactions.forEach { transaction ->
                            val typeLabel = if (transaction.type == TransactionType.EXPENSE) "支出" else "收入"
                            val color = if (transaction.type == TransactionType.EXPENSE) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${transaction.date} | $typeLabel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = transaction.note.ifBlank { "无备注" },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Text(
                                    text = String.format("%.2f", transaction.amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = color,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTransactionsDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    // 二次确认删除弹窗
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text(pendingDeleteMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        Log.i("CategoryManage", "Confirm delete button clicked")
                        pendingDeleteAction?.invoke()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 回收站弹窗
    if (showRecycleBin) {
        val hiddenCategories = allChildren.filter {
            !it.isVisible && level1Categories.any { l1 -> l1.id == it.parentId }
        }
        AlertDialog(
            onDismissRequest = { showRecycleBin = false },
            title = { Text("回收站") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hiddenCategories.isEmpty()) {
                        Text("没有隐藏的分类")
                    } else {
                        hiddenCategories.forEach { cat ->
                            val parent = level1Categories.find { it.id == cat.parentId }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${parent?.name ?: ""} > ${cat.name}",
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { viewModel.toggleVisibility(cat.id) }
                                ) {
                                    Text("恢复")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRecycleBin = false }) {
                    Text("关闭")
                }
            }
        )
    }

    // 错误提示对话框
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("提示") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                Button(onClick = { errorMessage = null }) {
                    Text("确定")
                }
            }
        )
    }
}

/**
 * 一级分类卡片：顶部一行展示一级分类自身（图标/名称/操作），
 * 下方用 [FlowRow] 平铺展示其下所有二级分类（不再需要展开/折叠）。
 *
 * @param isDragging 当前卡片是否处于拖拽中——若是则加大阴影并显示拖拽提示图标
 * @param onMoveChild 二级分类内部移动回调：(parentId, fromIndex, toIndex)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryTreeItem(
    category: Category,
    children: List<Category>,
    isDragging: Boolean,
    dragHandleModifier: Modifier = Modifier,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit,
    onAddChild: (Category) -> Unit,
    onMoveChild: (parentId: Long, fromIndex: Int, toIndex: Int) -> Unit,
    activeReorderParentId: Long?,
    onReorderModeChange: (parentId: Long, inMode: Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 12.dp else 2.dp
        )
    ) {
        Column {
            // 一级分类行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 拖拽手柄图标
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "长按拖动排序",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = dragHandleModifier.then(Modifier.size(20.dp))
                )
                Spacer(modifier = Modifier.size(8.dp))
                Icon(
                    imageVector = getIconVector(category.iconName),
                    contentDescription = null,
                    tint = parseColorHex(category.colorHex),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                if (category.isDefault) {
                    Text(
                        text = "默认",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                IconButton(onClick = { onAddChild(category) }) {
                    Icon(Icons.Default.Add, contentDescription = "添加二级分类")
                }
                IconButton(onClick = { onEdit(category) }) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                if (!category.isDefault) {
                    IconButton(onClick = { onDelete(category) }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            }

            // 二级分类：始终展开，FlowRow 平铺成芯片
            if (children.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    children.forEachIndexed { index, child ->
                        ChildCategoryChip(
                            category = child,
                            canMoveLeft = index > 0,
                            canMoveRight = index < children.size - 1,
                            canDelete = children.size > 1,
                            onClick = { onEdit(child) },
                            onDelete = { onDelete(child) },
                            onMoveLeft = { onMoveChild(category.id, index, index - 1) },
                            onMoveRight = { onMoveChild(category.id, index, index + 1) },
                            inReorderMode = activeReorderParentId == category.id,
                            onReorderModeChange = { inMode -> onReorderModeChange(category.id, inMode) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 二级分类 chip：紧凑显示图标 + 名称。
 *
 * - 整体点击 = 编辑（走 [Surface] 的 onClick，自带 ripple）
 * - 长按 = 切换到「排序模式」，把图标/名称区临时换成「← →」按钮，方便逐格调整顺序
 *   （FlowRow 自动换行，不适合做真正的拖拽，用左右按钮更简洁可靠）
 * - 末尾的 X 按钮 = 删除（默认分类隐藏；IconButton 自带的 clickable 会消费事件，
 *   所以不会同时触发外层 Surface 的 onClick）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChildCategoryChip(
    category: Category,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    inReorderMode: Boolean,
    onReorderModeChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.combinedClickable(
            onClick = {
                if (inReorderMode) {
                    onReorderModeChange(false)
                } else {
                    onClick()
                }
            },
            onLongClick = { onReorderModeChange(true) }
        ),
        shape = MaterialTheme.shapes.small,
        color = when {
            inReorderMode -> MaterialTheme.colorScheme.primaryContainer
            !category.isVisible -> MaterialTheme.colorScheme.surfaceContainerLow
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        tonalElevation = 1.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                start = if (inReorderMode) 4.dp else 10.dp,
                end = when {
                    inReorderMode -> 4.dp
                    !canDelete -> 12.dp
                    else -> 2.dp
                },
                top = 4.dp,
                bottom = 4.dp
            )
        ) {
            if (inReorderMode) {
                // 排序模式：显示 ← 名称 → ，点完一次顺序更新但保持模式直到点别处
                IconButton(
                    onClick = { if (canMoveLeft) onMoveLeft() },
                    enabled = canMoveLeft,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "左移",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(
                    onClick = { if (canMoveRight) onMoveRight() },
                    enabled = canMoveRight,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "右移",
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (category.isVisible) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
                Spacer(modifier = Modifier.size(2.dp))
                IconButton(
                    onClick = onDelete,
                    enabled = canDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
