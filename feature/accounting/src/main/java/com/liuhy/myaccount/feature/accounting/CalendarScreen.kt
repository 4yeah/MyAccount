/**
 * 日历视图页面。
 *
 * 以月历形式展示每天的收支情况，点击日期下方列出当天的所有交易记录。
 * 支持左右切换月份，收支金额用不同颜色标识。
 */
package com.liuhy.myaccount.feature.accounting

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liuhy.myaccount.core.common.getIconVector
import com.liuhy.myaccount.core.common.parseColorHex
import com.liuhy.myaccount.core.data.model.Transaction
import com.liuhy.myaccount.core.data.model.TransactionType
import com.nlf.calendar.Solar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory)
) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val dayTransactions by viewModel.dayTransactions.collectAsState()
    val dayIncome by viewModel.dayIncome.collectAsState()
    val dayExpense by viewModel.dayExpense.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "日历",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { viewModel.goToToday() },
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("今天")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 日历视图（固定，不随列表滚动）
            CalendarView(
                selectedMonth = selectedMonth,
                selectedDate = selectedDate,
                transactions = transactions,
                onMonthChange = viewModel::changeMonth,
                onDateSelect = viewModel::selectDate
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 当日明细头部（固定）
            DayDetailHeader(
                date = selectedDate,
                income = dayIncome,
                expense = dayExpense
            )

            // 当日交易明细（可滚动）
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                if (dayTransactions.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onAddTransaction)
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "这一天你还没有任何记账",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
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
                    CalendarTransactionItem(
                        transaction = transaction,
                        categoryName = categoryDisplayName,
                        iconName = parentCategory?.iconName,
                        iconColor = parentCategory?.colorHex,
                        onEdit = { onEditTransaction(transaction.id) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
}

/**
 * 获取指定日期的农历显示信息。
 *
 * 优先级：节日 > 节气 > 初一（显示月份）> 普通农历日期
 * @return Pair<显示文本, 是否为节日/节气>
 */
private fun getLunarInfo(date: LocalDate): Pair<String, Boolean> {
    return try {
        val solar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth)
        val lunar = solar.lunar
        // 节日
        val festivals = lunar.festivals
        if (festivals.isNotEmpty()) {
            return festivals.first() to true
        }
        // 节气
        val jieQi = lunar.jieQi
        if (jieQi.isNotEmpty()) {
            return jieQi to true
        }
        // 初一显示月份
        if (lunar.day == 1) {
            return (lunar.monthInChinese + "月") to false
        }
        // 普通农历日期
        lunar.dayInChinese to false
    } catch (e: Exception) {
        "" to false
    }
}

/**
 * 获取指定月份的农历年月描述，如"丙午年 四月"。
 */
private fun getLunarYearMonth(yearMonth: YearMonth): String {
    return try {
        val solar = Solar.fromYmd(yearMonth.year, yearMonth.monthValue, 1)
        val lunar = solar.lunar
        lunar.yearInGanZhi + "年 " + lunar.monthInChinese + "月"
    } catch (e: Exception) {
        ""
    }
}

@Composable
private fun CalendarView(
    selectedMonth: YearMonth,
    selectedDate: LocalDate,
    transactions: List<Transaction>,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelect: (LocalDate) -> Unit
) {
    val dailyData = remember(transactions, selectedMonth) {
        transactions
            .filter { YearMonth.from(it.date) == selectedMonth }
            .groupBy { it.date }
            .mapValues { (_, txns) ->
                val income = txns.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val expense = txns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                income to expense
            }
    }

    // 当月总收支
    val monthIncome = dailyData.values.sumOf { it.first }
    val monthExpense = dailyData.values.sumOf { it.second }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // 月份切换头部
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMonthChange(selectedMonth.minusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一月")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${selectedMonth.year} 年 ${selectedMonth.monthValue} 月",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = getLunarYearMonth(selectedMonth),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    // 当月收支汇总
                    Column(horizontalAlignment = Alignment.End) {
                        if (monthIncome > 0) {
                            Text(
                                text = "+${String.format("%.0f", monthIncome)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (monthExpense > 0) {
                            Text(
                                text = "-${String.format("%.0f", monthExpense)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF44336),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (monthIncome == 0.0 && monthExpense == 0.0) {
                            Text(
                                text = "无收支",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                IconButton(onClick = { onMonthChange(selectedMonth.plusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一月")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 星期表头
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 日历网格
            val firstDay = selectedMonth.atDay(1)
            val leadingEmpty = firstDay.dayOfWeek.value % 7
            val daysInMonth = selectedMonth.lengthOfMonth()
            val totalRows = ((leadingEmpty + daysInMonth + 6) / 7)

            Column {
                for (row in 0 until totalRows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayOfMonth = cellIndex - leadingEmpty + 1
                            if (dayOfMonth in 1..daysInMonth) {
                                val date = selectedMonth.atDay(dayOfMonth)
                                val (income, expense) = dailyData[date] ?: (0.0 to 0.0)
                                val hasRecord = income > 0 || expense > 0

                                CalendarDayCell(
                                    day = dayOfMonth,
                                    date = date,
                                    isSelected = date == selectedDate,
                                    isToday = date == LocalDate.now(),
                                    income = income,
                                    expense = expense,
                                    onClick = { onDateSelect(date) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    income: Double,
    expense: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (lunarText, isFestival) = remember(date) { getLunarInfo(date) }
    val hasRecord = income > 0 || expense > 0

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp, horizontal = 2.dp)
            .height(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：日期 + 农历
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                // 阳历日期
                Text(
                    text = day.toString(),
                    color = when {
                        isToday -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    style = if (isToday) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    else MaterialTheme.typography.bodySmall,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(1.dp))

                // 农历 / 节日
                Text(
                    text = lunarText,
                    color = when {
                        isFestival -> Color(0xFFE91E63)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    fontWeight = if (isFestival) FontWeight.Bold else FontWeight.Normal,
                    lineHeight = 12.sp
                )
            }

            // 右侧：收支金额
            Column(
                horizontalAlignment = Alignment.End
            ) {
                if (income > 0) {
                    Text(
                        text = "+${income.toInt()}",
                        color = Color(0xFF4CAF50),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        maxLines = 1
                    )
                }
                if (expense > 0) {
                    Text(
                        text = "-${expense.toInt()}",
                        color = Color(0xFFF44336),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun DayDetailHeader(
    date: LocalDate,
    income: Double,
    expense: Double
) {
    val dayOfWeekMap = mapOf(
        DayOfWeek.MONDAY to "周一",
        DayOfWeek.TUESDAY to "周二",
        DayOfWeek.WEDNESDAY to "周三",
        DayOfWeek.THURSDAY to "周四",
        DayOfWeek.FRIDAY to "周五",
        DayOfWeek.SATURDAY to "周六",
        DayOfWeek.SUNDAY to "周日"
    )
    val dayOfWeekText = dayOfWeekMap[date.dayOfWeek] ?: ""

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " $dayOfWeekText",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (income > 0) {
                Text(
                    text = "收入 ${String.format("%.2f", income)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50)
                )
            }
            if (expense > 0) {
                Text(
                    text = "支出 ${String.format("%.2f", expense)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF44336)
                )
            }
            if (income == 0.0 && expense == 0.0) {
                Text(
                    text = "暂无收支",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun CalendarTransactionItem(
    transaction: Transaction,
    categoryName: String,
    iconName: String? = null,
    iconColor: String? = null,
    onEdit: () -> Unit
) {
    val color = if (transaction.type == TransactionType.INCOME) Color(0xFF4CAF50) else Color(0xFFF44336)
    val sign = if (transaction.type == TransactionType.INCOME) "+" else "-"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
        }
    }
}
