/**
 * 账本首页（记账列表）。
 *
 * 展示本月所有收支记录列表，顶部显示本月支出/收入汇总。
 * 支持长按进入批量删除模式，点击单条进入编辑。
 */
package com.liuhy.myaccount.feature.accounting

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liuhy.myaccount.core.common.getIconVector
import com.liuhy.myaccount.core.common.parseColorHex
import com.liuhy.myaccount.core.data.model.Transaction
import com.liuhy.myaccount.core.data.model.TransactionType
import java.time.format.DateTimeFormatter

/**
 * 我的账本首页界面
 *
 * 这是应用的核心页面，展示月度收支汇总、预算信息和历史记录列表。
 * 采用 Jetpack Compose 声明式 UI 编写，所有界面元素都是 Kotlin 函数。
 *
 * @param onAddTransaction 点击"添加记账"按钮时的回调，用于跳转到添加页面
 * @param onEditTransaction 点击编辑按钮时的回调，传入交易 ID 用于跳转到编辑页面
 * @param onBudgetSettings 点击预算设置时的回调，用于跳转到预算设置页面
 * @param onBatchModeChange 批量模式变化回调，通知外部当前是否处于批量模式
 * @param viewModel 页面数据管理器（MVVM 架构中的 ViewModel），负责提供数据和处理业务逻辑
 */
@OptIn(ExperimentalMaterial3Api::class) // 使用 Material3 实验性 API（如 TopAppBar）需要加此注解
@Composable
fun AccountBookScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onBudgetSettings: () -> Unit,
    onBatchModeChange: (Boolean) -> Unit = {},
    viewModel: AccountBookViewModel = viewModel(factory = AccountBookViewModel.Factory)
) {
    // 【状态收集】从 ViewModel 获取数据，界面会自动跟随数据变化刷新
    // collectAsState() 把 Kotlin Flow 数据流转换为 Compose 可以感知的状态
    val transactions by viewModel.transactions.collectAsState()      // 所有交易记录列表
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()    // 本月总收入
    val monthlyExpense by viewModel.monthlyExpense.collectAsState()  // 本月总支出
    val budget by viewModel.budget.collectAsState()                  // 用户设置的月度预算
    val categories by viewModel.categories.collectAsState()          // 收支分类列表
    val bookName by viewModel.bookName.collectAsState()              // 账本名称

    // 【删除确认状态】记录用户点击删除的那条记录的 ID
    // 为 null 表示没有待删除记录，不显示确认对话框
    var transactionToDelete by remember { mutableStateOf<Long?>(null) }

    // 【批量操作状态】
    var isBatchMode by remember { mutableStateOf(false) }           // 是否处于批量操作模式
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }   // 当前选中的记录 ID 集合

    // 【账本名称编辑状态】
    var showEditBookNameDialog by remember { mutableStateOf(false) }
    var newBookName by remember { mutableStateOf("") }

    // 【通知外部批量模式变化】
    LaunchedEffect(isBatchMode) {
        onBatchModeChange(isBatchMode)
    }

    // 【编辑账本名称对话框】
    if (showEditBookNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditBookNameDialog = false },
            title = { Text("修改账本名称") },
            text = {
                OutlinedTextField(
                    value = newBookName,
                    onValueChange = { newBookName = it },
                    label = { Text("账本名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newBookName.isNotBlank()) {
                            viewModel.setBookName(newBookName.trim())
                        }
                        showEditBookNameDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditBookNameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 【数据计算】根据收入和支出计算结余和剩余预算
    val monthlyBalance = monthlyIncome - monthlyExpense
    // 剩余预算 = 预算 - 本月支出（预算只看支出，不看收入）
    val remainingBudget = budget - monthlyExpense

    // 【界面骨架】Scaffold 提供标准页面结构（顶部标题栏 + 内容区域）
    // 批量模式下底部显示操作栏
    // 注意：外层 MainActivity 已有 Scaffold，且已经处理过 systemBars 内边距，
    //   - 内层 Scaffold 必须 fillMaxSize，否则会以 wrap_content 方式让 LazyColumn 拿不到正确高度；
    //   - 必须把 contentWindowInsets 置零，避免底部导航栏的 inset 被重复扣减导致 LazyColumn 视口被压短，
    //     表现是"刚好放下 4 条记录、第 5 条看不到也滚不动"。
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bookName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        newBookName = bookName
                        showEditBookNameDialog = true
                    }
                )
            }
        },
        bottomBar = {
            // 【批量操作栏】仅在批量模式下显示在页面底部
            if (isBatchMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左边：全选复选框
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedIds.size == transactions.size && transactions.isNotEmpty(),
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) {
                                        transactions.map { it.id }.toSet()
                                    } else {
                                        emptySet()
                                    }
                                }
                            )
                            Text("全选")
                        }
                        // 中间：删除按钮（有选中项时才可点击）
                        Button(
                            onClick = { transactionToDelete = -1L },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Text("删除(${selectedIds.size})")
                        }
                        // 右边：退出按钮
                        TextButton(
                            onClick = {
                                isBatchMode = false
                                selectedIds = emptySet()
                                onBatchModeChange(false)
                            }
                        ) {
                            Text("退出")
                        }
                    }
                }
            }
        }
    ) { padding ->
        // 【性能优化】缓存分组结果和当天收支计算，避免每次重组都重新遍历全量数据
        val groupedTransactions = remember(transactions) {
            transactions.groupBy { it.date }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                .padding(16.dp)
        ) {
            item {
                SummaryCard(
                    income = monthlyIncome,
                    expense = monthlyExpense,
                    balance = monthlyBalance,
                    budget = budget,
                    remaining = remainingBudget,
                    onBudgetSettings = onBudgetSettings
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                FloatingActionButton(
                    onClick = onAddTransaction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("添加一条记账", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 列表模式：按日期分组显示所有交易记录
            groupedTransactions.forEach { (date, dayTransactions) ->
                val dayOfWeekMap = mapOf(
                    java.time.DayOfWeek.MONDAY to "周一",
                    java.time.DayOfWeek.TUESDAY to "周二",
                    java.time.DayOfWeek.WEDNESDAY to "周三",
                    java.time.DayOfWeek.THURSDAY to "周四",
                    java.time.DayOfWeek.FRIDAY to "周五",
                    java.time.DayOfWeek.SATURDAY to "周六",
                    java.time.DayOfWeek.SUNDAY to "周日"
                )
                val dayOfWeekText = dayOfWeekMap[date.dayOfWeek] ?: ""

                item {
                    val dayIncome = remember(dayTransactions) {
                        dayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                    }
                    val dayExpense = remember(dayTransactions) {
                        dayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " $dayOfWeekText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "收入:${String.format("%.2f", dayIncome)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Text(
                                text = "支出:${String.format("%.2f", dayExpense)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF44336),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                items(dayTransactions, key = { it.id }) { transaction ->
                    val category = categories.find { it.id == transaction.categoryId }
                    val parentCategory = when {
                        category == null -> null
                        category.parentId == null -> category
                        else -> categories.find { it.id == category.parentId }
                    }
                    val categoryDisplayName = when {
                        category == null -> "未知"
                        category.parentId == null -> category.name
                        else -> {
                            if (parentCategory != null && parentCategory.name != category.name) {
                                "${parentCategory.name}-${category.name}"
                            } else {
                                category.name
                            }
                        }
                    }
                    TransactionItem(
                        transaction = transaction,
                        categoryName = categoryDisplayName,
                        iconName = parentCategory?.iconName,
                        iconColor = parentCategory?.colorHex,
                        onEdit = { onEditTransaction(transaction.id) },
                        onLongClick = {
                            if (!isBatchMode) {
                                isBatchMode = true
                                selectedIds = setOf(transaction.id)
                            }
                        },
                        isBatchMode = isBatchMode,
                        isSelected = selectedIds.contains(transaction.id),
                        onSelectToggle = {
                            selectedIds = if (selectedIds.contains(transaction.id)) {
                                selectedIds - transaction.id
                            } else {
                                selectedIds + transaction.id
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            item {
                Text(
                    text = "共加载 ${transactions.size} 条记录，${groupedTransactions.size} 个日期分组",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        // 【删除确认对话框】当 transactionToDelete 不为 null 时弹出
        if (transactionToDelete != null) {
            AlertDialog(
                onDismissRequest = { transactionToDelete = null }, // 点击对话框外部或返回键时取消
                title = { Text("确认删除") },
                text = { Text("确定要删除这条记录吗？") },
                confirmButton = {
                    // 确认删除按钮：调用 ViewModel 删除方法，然后清空状态关闭对话框
                    Button(
                        onClick = {
                            viewModel.deleteTransaction(transactionToDelete!!)
                            transactionToDelete = null
                        }
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    // 取消按钮：仅清空状态关闭对话框
                    Button(onClick = { transactionToDelete = null }) {
                        Text("取消")
                    }
                }
            )
        }

        // 【批量删除确认对话框】批量模式下点击删除按钮时弹出
        if (isBatchMode && selectedIds.isNotEmpty() && transactionToDelete == -1L) {
            AlertDialog(
                onDismissRequest = { transactionToDelete = null },
                title = { Text("确认删除") },
                text = { Text("确定要删除选中的 ${selectedIds.size} 条记录吗？") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTransactions(selectedIds.toList())
                            selectedIds = emptySet()
                            isBatchMode = false
                            transactionToDelete = null
                            onBatchModeChange(false)
                        }
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    Button(onClick = { transactionToDelete = null }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

/**
 * 月度收支汇总卡片
 *
 * 用 Card 组件展示 4 个核心指标：本月收入、本月支出、本月结余、剩余预算。
 * Card 是 Material Design 的卡片组件，带阴影和圆角，视觉上独立成块。
 */
@Composable
private fun SummaryCard(
    income: Double,
    expense: Double,
    balance: Double,
    budget: Double,
    remaining: Double,
    onBudgetSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp)
        ) {
            Column {
                // 第一行：本月收入 + 本月支出
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryItem("本月收入", income, Color(0xFF4CAF50), modifier = Modifier.weight(1f))
                    SummaryItem("本月支出", expense, Color(0xFFF44336), modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                // 分隔线
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    thickness = 0.5.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                // 第二行：本月结余 + 剩余预算/设置预算按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryItem("本月结余", balance, Color(0xFF2196F3), modifier = Modifier.weight(1f))
                    if (budget > 0) {
                        val progress = (expense / budget).toFloat()
                        val progressColor = if (remaining >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .clickable { onBudgetSettings() }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                SummaryItem("剩余预算", remaining, progressColor, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(3.dp)
                                            .background(Color.Gray.copy(alpha = 0.2f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(progress.coerceAtMost(1f))
                                                .height(3.dp)
                                                .background(progressColor)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = onBudgetSettings,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("设置预算")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 汇总指标项（新样式）
 */
@Composable
private fun SummaryItem(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(
            "%.2f".format(amount),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * 单条交易记录卡片
 *
 * 展示一条记录的金额、分类、备注，以及编辑按钮。
 * 收入显示绿色"+"号，支出显示红色"-"号。
 * 长按可进入批量操作模式，批量模式下左侧显示复选框。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionItem(
    transaction: Transaction,
    categoryName: String,
    iconName: String? = null,
    iconColor: String? = null,
    onEdit: () -> Unit,
    onLongClick: () -> Unit,
    isBatchMode: Boolean,
    isSelected: Boolean,
    onSelectToggle: () -> Unit
) {
    // 根据交易类型决定颜色和符号
    val color = if (transaction.type == TransactionType.INCOME) Color(0xFF4CAF50) else Color(0xFFF44336)
    val sign = if (transaction.type == TransactionType.INCOME) "+" else "-"
    Card(
        // 使用 combinedClickable 兼容 LazyColumn 的滚动手势：
        // 它在检测到祖先容器开始滚动时会主动让出指针事件，
        // 而 detectTapGestures 会独占按下事件，导致整个列表无法滚动。
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isBatchMode) onSelectToggle() },
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        // Row 将子元素水平排列（类似 LinearLayout 的 horizontal 方向）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically // 垂直方向居中对齐
        ) {
            // 【批量模式复选框】仅在批量模式下显示
            if (isBatchMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectToggle() }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            // 左侧信息区域，weight(1f) 占据剩余所有空间，把按钮挤到右边
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (iconName != null) {
                            Icon(
                                imageVector = getIconVector(iconName),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (iconColor != null) parseColorHex(iconColor) else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            categoryName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Text(
                        "$sign%.2f".format(transaction.amount),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1,
                        modifier = Modifier.height(16.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            // 编辑按钮（批量模式下隐藏）
            if (!isBatchMode) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", tint = Color.Gray)
                }
            }
        }
    }
}
