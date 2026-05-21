/**
 * 统计业务仓库。
 *
 * 聚合交易和分类数据，输出按月/按年维度的收支汇总，
 * 以及按一级或二级分类拆解的排行榜和饼图数据。
 */
package com.liuhy.myaccount.core.data.repository

import com.liuhy.myaccount.core.data.model.TransactionType
import com.liuhy.myaccount.core.database.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.time.YearMonth

class StatisticsRepository(private val db: AppDatabase) {

    fun getMonthlySummary(yearMonth: YearMonth, isYearly: Boolean = false): Flow<MonthlySummary> = flow {
        val start = if (isYearly) {
            YearMonth.of(yearMonth.year, 1).atDay(1)
        } else {
            yearMonth.atDay(1)
        }
        val end = if (isYearly) {
            YearMonth.of(yearMonth.year, 12).atEndOfMonth()
        } else {
            yearMonth.atEndOfMonth()
        }
        val expense = db.transactionDao().getSumByTypeAndDateRange(TransactionType.EXPENSE.value, start, end) ?: 0.0
        val income = db.transactionDao().getSumByTypeAndDateRange(TransactionType.INCOME.value, start, end) ?: 0.0
        emit(MonthlySummary(income = income, expense = expense, balance = income - expense))
    }

    fun getCategoryBreakdown(
        yearMonth: YearMonth,
        type: TransactionType,
        isYearly: Boolean = false,
        dimension: CategoryDimension = CategoryDimension.LEVEL_2
    ): Flow<List<CategoryBreakdown>> {
        val start = if (isYearly) {
            YearMonth.of(yearMonth.year, 1).atDay(1).toEpochDay()
        } else {
            yearMonth.atDay(1).toEpochDay()
        }
        val end = if (isYearly) {
            YearMonth.of(yearMonth.year, 12).atEndOfMonth().toEpochDay()
        } else {
            yearMonth.atEndOfMonth().toEpochDay()
        }

        return combine(
            db.categoryDao().getAll(),
            db.transactionDao().getByDateRange(start, end)
        ) { categories, transactions ->
            val filtered = transactions.filter { it.type == type.value }
            val total = filtered.sumOf { it.amount }

            when (dimension) {
                CategoryDimension.LEVEL_2 -> {
                    // 按实际分类（二级）分组
                    val grouped = filtered.groupBy { it.categoryId }
                    grouped.map { (categoryId, items) ->
                        val category = categories.find { it.id == categoryId }
                        val categoryName = category?.name ?: "未知"
                        CategoryBreakdown(
                            categoryName = categoryName,
                            amount = items.sumOf { it.amount },
                            percentage = if (total > 0) (items.sumOf { it.amount } / total * 100).toFloat() else 0f
                        )
                    }
                }
                CategoryDimension.LEVEL_1 -> {
                    // 按一级分类聚合：把二级分类的金额汇总到父分类
                    val grouped = filtered.groupBy { transaction ->
                        val category = categories.find { it.id == transaction.categoryId }
                        // 如果是一级分类直接用自身，如果是二级分类找父分类
                        category?.parentId ?: transaction.categoryId
                    }
                    grouped.map { (parentId, items) ->
                        val parentCategory = categories.find { it.id == parentId }
                        CategoryBreakdown(
                            categoryName = parentCategory?.name ?: "未知",
                            amount = items.sumOf { it.amount },
                            percentage = if (total > 0) (items.sumOf { it.amount } / total * 100).toFloat() else 0f
                        )
                    }
                }
            }.sortedByDescending { it.amount }
        }
    }

    enum class CategoryDimension {
        LEVEL_1, // 按一级分类聚合
        LEVEL_2  // 按二级分类（实际分类）统计
    }

    data class MonthlySummary(
        val income: Double,
        val expense: Double,
        val balance: Double
    )

    data class CategoryBreakdown(
        val categoryName: String,
        val amount: Double,
        val percentage: Float
    )
}
