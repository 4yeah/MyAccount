/**
 * 新增/编辑交易页面。
 *
 * 支持：
 * - 输入金额、备注、选择日期
 * - 切换支出/收入类型
 * - 从二级分类网格中选择分类（可跳转到分类管理新增）
 * - 接收通知监听推送的预填数据（金额/类型/备注）
 */
package com.liuhy.myaccount.feature.accounting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liuhy.myaccount.core.data.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionScreen(
    transactionId: Long? = null,
    onBack: () -> Unit,
    onNavigateToCategoryManage: (Long?) -> Unit = {},
    prefillAmount: Double? = null,
    prefillType: TransactionType? = null,
    prefillNote: String? = null,
    viewModel: TransactionViewModel = viewModel(factory = TransactionViewModel.Factory(transactionId))
) {
    val isEditMode = transactionId != null

    // 仅在新增模式下处理预填数据
    LaunchedEffect(prefillAmount, prefillType, prefillNote) {
        viewModel.applyPrefillOnce(prefillAmount, prefillType, prefillNote)
    }

    // 监听导航事件
    LaunchedEffect(Unit) {
        viewModel.navigateToAddCategory.collect { parentId ->
            onNavigateToCategoryManage(parentId)
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()
    val categories = if (uiState.type == TransactionType.EXPENSE) expenseCategories else incomeCategories

    // 编辑模式加载中
    if (uiState.isLoading) {
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
                        text = "编辑记录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("加载中...")
            }
        }
        return
    }

    // 内层 Scaffold 必须 fillMaxSize 且 contentWindowInsets 置零，
    // 否则会与 MainActivity 外层 Scaffold 的 systemBars padding 重复扣减，
    // 导致底部"备注 / 保存按钮"被挤出可视区域。
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
                    text = if (isEditMode) "编辑记录" else "记一笔",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilterChip(
                    selected = uiState.type == TransactionType.EXPENSE,
                    onClick = { viewModel.setType(TransactionType.EXPENSE) },
                    label = { Text("支出") }
                )
                FilterChip(
                    selected = uiState.type == TransactionType.INCOME,
                    onClick = { viewModel.setType(TransactionType.INCOME) },
                    label = { Text("收入") }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // 金额计算器：点击金额字段后展开，避免默认占用过多空间
            var calcDisplay by remember { mutableStateOf(uiState.amount) }
            var showCalculator by remember { mutableStateOf(false) }
            LaunchedEffect(uiState.amount) {
                if (uiState.amount != calcDisplay && !calcDisplay.contains("+") && !calcDisplay.contains("-")) {
                    calcDisplay = uiState.amount
                }
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = calcDisplay,
                    onValueChange = { },
                    label = { Text("金额") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showCalculator = !showCalculator }
                )
            }
            if (showCalculator) {
                Spacer(modifier = Modifier.height(8.dp))
                CalculatorPanel(
                    expression = calcDisplay,
                    onExpressionChange = { calcDisplay = it },
                    onConfirm = {
                        viewModel.setAmount(calcDisplay)
                        showCalculator = false
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text("分类", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            CategoryGridSelector(
                categories = categories,
                selectedCategoryId = uiState.categoryId,
                onCategorySelected = { viewModel.setCategory(it) },
                onAddCategory = { viewModel.onAddCategory(it) }
            )
            Spacer(modifier = Modifier.height(8.dp))

            val showDatePicker = remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.date?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) ?: "",
                    onValueChange = { },
                    label = { Text("日期") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker.value = true }
                )
            }
            if (showDatePicker.value) {
                SimpleDatePickerDialog(
                    initialDate = uiState.date ?: LocalDate.now(),
                    onConfirm = { date ->
                        viewModel.setDate(date)
                        showDatePicker.value = false
                    },
                    onDismiss = { showDatePicker.value = false }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::setNote,
                label = { Text("备注") },
                // 备注非空时在右侧显示清除按钮，方便一键清空（特别是预填后想换内容）
                trailingIcon = {
                    if (uiState.note.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setNote("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除备注")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.save(onBack) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.amount.isNotBlank() && uiState.categoryId != null && uiState.date != null
            ) {
                Text(if (isEditMode) "保存修改" else "保存")
            }
        }
    }
}

/**
 * 自绘的月历日期选择对话框。
 *
 * 替代 Material3 DatePicker 是为了规避其在中文 locale 下星期表头
 * 全部显示为「星」的 bug（系统短星期返回「星期日/星期一/...」，被截首字符后全是「星」）。
 *
 * 特性：
 * - 顶部「‹ 2026年5月 ›」可切换月份
 * - 长按月份标题可快速切换年份（弹年份选择）
 * - 周日开头的月历网格，星期表头硬编码中文
 * - 支持「今天」快捷按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDatePickerDialog(
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(initialDate) }
    var displayedMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var showYearPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择日期") },
        text = {
            Column {
                // 月份切换头
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        displayedMonth = displayedMonth.minusMonths(1)
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "上一月"
                        )
                    }
                    TextButton(onClick = { showYearPicker = true }) {
                        Text(
                            text = "${displayedMonth.year} 年 ${displayedMonth.monthValue} 月",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    IconButton(onClick = {
                        displayedMonth = displayedMonth.plusMonths(1)
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "下一月"
                        )
                    }
                }

                // 星期表头（硬编码中文，绕开 Material3 bug）
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("日", "一", "二", "三", "四", "五", "六").forEach { d ->
                        Text(
                            text = d,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 月历网格
                CalendarGrid(
                    yearMonth = displayedMonth,
                    selected = selected,
                    today = LocalDate.now(),
                    onSelect = { date ->
                        selected = date
                        // 选了别的月份的某天时（理论上格子里不会出现别月日期，
                        // 但如果将来扩展为 6×7 网格带前后月，这里安全切换）
                        displayedMonth = YearMonth.from(date)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 已选 + 今天快捷
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已选：${selected.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = {
                        val today = LocalDate.now()
                        selected = today
                        displayedMonth = YearMonth.from(today)
                    }) { Text("今天") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

    if (showYearPicker) {
        YearPickerDialog(
            currentYear = displayedMonth.year,
            onYearSelected = { y ->
                displayedMonth = displayedMonth.withYear(y)
                showYearPicker = false
            },
            onDismiss = { showYearPicker = false }
        )
    }
}

/**
 * 月历网格：固定 6 行 × 7 列；当月日期可点，其它格子留空。
 */
@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    val firstDay = yearMonth.atDay(1)
    // Java DayOfWeek: MONDAY=1..SUNDAY=7；本网格周日开头：SUN=0, MON=1, ..., SAT=6
    val leadingEmpty = firstDay.dayOfWeek.value % 7
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalRows = ((leadingEmpty + daysInMonth + 6) / 7)

    Column {
        for (row in 0 until totalRows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayOfMonth = cellIndex - leadingEmpty + 1
                    if (dayOfMonth in 1..daysInMonth) {
                        val date = yearMonth.atDay(dayOfMonth)
                        DayCell(
                            day = dayOfMonth,
                            isSelected = date == selected,
                            isToday = date == today,
                            onClick = { onSelect(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * 年份选择对话框：弹一个 LazyColumn 让用户挑年份。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearPickerDialog(
    currentYear: Int,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val years = remember { (1970..2100).toList() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择年份") },
        text = {
            LazyColumn(
                modifier = Modifier.height(300.dp)
            ) {
                items(years) { y ->
                    val isCurrent = y == currentYear
                    Text(
                        text = "$y 年",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onYearSelected(y) }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        color = if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                        style = if (isCurrent) MaterialTheme.typography.titleMedium
                                else MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

// ---------- 金额计算器 ----------

/**
 * 简易金额计算器面板，支持数字输入和加减运算。
 *
 * 布局：4 列网格，包含数字 0-9、小数点、+、-、=、C、退格。
 * 按「=」计算表达式结果并通过 [onConfirm] 同步给调用方。
 */
@Composable
private fun CalculatorPanel(
    expression: String,
    onExpressionChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    val rows = listOf(
        listOf("C", "←", "÷", "×"),
        listOf("7", "8", "9", "-"),
        listOf("4", "5", "6", "+"),
        listOf("1", "2", "3", "="),
        listOf(".", "0", "", "")   // 空字符串占位，保持 4 列对齐
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { label ->
                    if (label.isEmpty()) {
                        Spacer(modifier = Modifier.weight(1f))
                        return@forEach
                    }
                    val isOp = label in listOf("+", "-", "×", "÷", "=", "C", "←")
                    androidx.compose.material3.FilledTonalButton(
                        onClick = {
                            when (label) {
                                "C" -> onExpressionChange("")
                                "←" -> onExpressionChange(expression.dropLast(1))
                                "=" -> {
                                    val result = evaluateCalc(expression)
                                    onExpressionChange(result)
                                    onConfirm()
                                }
                                "+", "-", "×", "÷" -> {
                                    // 若末尾已有运算符，替换；否则追加
                                    val last = expression.lastOrNull()
                                    if (last != null && last in "+-×÷") {
                                        onExpressionChange(expression.dropLast(1) + label)
                                    } else {
                                        onExpressionChange(expression + label)
                                    }
                                }
                                else -> onExpressionChange(expression + label)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = androidx.compose.material3.MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = label,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = if (isOp) androidx.compose.material3.MaterialTheme.colorScheme.primary
                                    else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

/**
 * 计算简单加减表达式，从左到右依次运算。
 *
 * 支持格式："12.5+3.2"、"100-20+5" 等。
 * 结果去除末尾无意义的 .0。
 */
private fun evaluateCalc(expr: String): String {
    if (expr.isBlank()) return ""
    val clean = expr.trim().replace(" ", "")
    if (clean.isEmpty()) return ""

    return try {
        // 第一步：按 + - 分割成项，保留符号
        val tokens = mutableListOf<String>()
        var current = ""
        for (char in clean) {
            if (char == '+' || char == '-') {
                if (current.isNotEmpty()) {
                    tokens.add(current)
                    current = ""
                }
                tokens.add(char.toString())
            } else {
                current += char
            }
        }
        if (current.isNotEmpty()) tokens.add(current)

        // 第二步：计算每个项（内部处理乘除）
        var result = evaluateTerm(tokens[0])
        var i = 1
        while (i < tokens.size) {
            val op = tokens[i]
            val value = evaluateTerm(tokens[i + 1])
            result = when (op) {
                "+" -> result + value
                "-" -> result - value
                else -> result
            }
            i += 2
        }

        if (result == result.toInt().toDouble()) result.toInt().toString()
        else result.toString()
    } catch (_: Exception) {
        expr
    }
}

/**
 * 计算单项内的乘除，如 "3×4÷2" -> 6.0
 */
private fun evaluateTerm(term: String): Double {
    if (term.isEmpty()) return 0.0
    val tokens = mutableListOf<String>()
    var current = ""
    for (char in term) {
        if (char == '×' || char == '÷') {
            if (current.isNotEmpty()) {
                tokens.add(current)
                current = ""
            }
            tokens.add(char.toString())
        } else {
            current += char
        }
    }
    if (current.isNotEmpty()) tokens.add(current)

    var result = tokens[0].toDouble()
    var i = 1
    while (i < tokens.size) {
        val op = tokens[i]
        val value = tokens[i + 1].toDouble()
        result = when (op) {
            "×" -> result * value
            "÷" -> if (value != 0.0) result / value else result
            else -> result
        }
        i += 2
    }
    return result
}
