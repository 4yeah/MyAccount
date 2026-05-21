/**
 * 统计页面。
 *
 * 展示按月/按年维度的收支汇总，以及分类维度（一级或二级）的饼图和排行榜。
 * 支持左右切换月份、切换支出/收入、切换一级/二级分类维度。
 */
package com.liuhy.myaccount.feature.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liuhy.myaccount.core.data.model.TransactionType
import com.liuhy.myaccount.core.data.repository.StatisticsRepository.CategoryDimension
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModel.Factory)
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val selectedYearMode by viewModel.selectedYearMode.collectAsState()
    val dimension by viewModel.dimension.collectAsState()
    val summary by viewModel.monthlySummary.collectAsState()
    val breakdown by viewModel.categoryBreakdown.collectAsState()

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
                    text = "统计",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, bottom = 16.dp)
        ) {
            // 【筛选行：一级/二级（左） + 支出/收入（右）】
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = dimension == CategoryDimension.LEVEL_1,
                            onClick = { viewModel.setDimension(CategoryDimension.LEVEL_1) },
                            label = { Text("一级分类") }
                        )
                        FilterChip(
                            selected = dimension == CategoryDimension.LEVEL_2,
                            onClick = { viewModel.setDimension(CategoryDimension.LEVEL_2) },
                            label = { Text("二级分类") }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedType == TransactionType.EXPENSE,
                            onClick = { viewModel.setType(TransactionType.EXPENSE) },
                            label = { Text("支出") }
                        )
                        FilterChip(
                            selected = selectedType == TransactionType.INCOME,
                            onClick = { viewModel.setType(TransactionType.INCOME) },
                            label = { Text("收入") }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 【按年/按月切换】
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = !selectedYearMode,
                        onClick = { viewModel.setYearlyMode(false) },
                        label = { Text("按月") }
                    )
                    FilterChip(
                        selected = selectedYearMode,
                        onClick = { viewModel.setYearlyMode(true) },
                        label = { Text("按年") }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            // 【月份/年份选择器】
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (selectedYearMode) {
                            viewModel.setYear(selectedMonth.year - 1)
                        } else {
                            viewModel.setMonth(selectedMonth.minusMonths(1))
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上一个月")
                    }
                    Text(
                        if (selectedYearMode) {
                            "${selectedMonth.year}年"
                        } else {
                            selectedMonth.format(DateTimeFormatter.ofPattern("yyyy年MM月"))
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        if (selectedYearMode) {
                            viewModel.setYear(selectedMonth.year + 1)
                        } else {
                            viewModel.setMonth(selectedMonth.plusMonths(1))
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下一个月")
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { SummaryCard(summary) }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { Text("分类占比", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item { Spacer(modifier = Modifier.height(8.dp)) }
            // 【饼状图】
            item {
                PieChart(breakdown)
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            // 【分类列表】
            items(breakdown) { item ->
                CategoryBreakdownItem(item)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: com.liuhy.myaccount.core.data.repository.StatisticsRepository.MonthlySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryColumn("收入", summary.income, Color(0xFF4CAF50))
            SummaryColumn("支出", summary.expense, Color(0xFFF44336))
            SummaryColumn("结余", summary.balance, Color(0xFF2196F3))
        }
    }
}

@Composable
private fun SummaryColumn(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(
            "%.2f".format(amount),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun CategoryBreakdownItem(
    item: com.liuhy.myaccount.core.data.repository.StatisticsRepository.CategoryBreakdown
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(item.categoryName, style = MaterialTheme.typography.bodyMedium)
            Text(
                "%.2f (%.1f%%)".format(item.amount, item.percentage),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { item.percentage / 100f },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 饼状图组件
 * 使用 Canvas 绘制扇形，展示各分类的占比
 */
@Composable
private fun PieChart(breakdown: List<com.liuhy.myaccount.core.data.repository.StatisticsRepository.CategoryBreakdown>) {
    // 【颜色数组】为每个分类分配颜色
    val colors = listOf(
        Color(0xFFFF6B6B), // 红色
        Color(0xFF4ECDC4), // 青色
        Color(0xFF45B7D1), // 蓝色
        Color(0xFF96CEB4), // 绿色
        Color(0xFFFFEAA7), // 黄色
        Color(0xFFDDA0DD), // 紫色
        Color(0xFF98D8C8), // 薄荷绿
        Color(0xFFF7DC6F), // 亮黄
        Color(0xFFBB8FCE), // 淡紫
        Color(0xFF85C1E9)  // 天蓝
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (breakdown.isEmpty()) {
            // 无数据时显示提示
            Text(
                "暂无数据",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.size(180.dp)
                ) {
                    val diameter = size.minDimension
                    val radius = diameter / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    var startAngle = 0f

                    breakdown.forEachIndexed { index, item ->
                        // 计算当前扇形的角度（百分比转角度）
                        val sweepAngle = (item.percentage / 100f * 360f).toFloat()

                        // 绘制扇形
                        drawArc(
                            color = colors[index % colors.size],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            topLeft = Offset(
                                center.x - radius,
                                center.y - radius
                            ),
                            size = Size(radius * 2, radius * 2)
                        )

                        // 绘制边框（分隔线）
                        drawArc(
                            color = Color.White,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            topLeft = Offset(
                                center.x - radius,
                                center.y - radius
                            ),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        startAngle += sweepAngle
                    }
                }
            }

            // 【图例】
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                breakdown.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 颜色标记
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(colors[index % colors.size])
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        // 分类名称和占比
                        Text(
                            "${item.categoryName}: %.1f%%".format(item.percentage),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
