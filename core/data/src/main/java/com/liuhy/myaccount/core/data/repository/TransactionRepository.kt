/**
 * 记账记录业务仓库。
 *
 * 职责：
 * 1. 封装 TransactionDao，将数据库实体（Entity）转成 UI 层使用的领域模型（Model）。
 * 2. 提供按日期、类型、分类等维度的查询，供统计和列表页使用。
 * 3. 隔离底层数据库细节，UI 层只依赖 `:core:data`，不直接访问 `:core:database`。
 */
package com.liuhy.myaccount.core.data.repository

import com.liuhy.myaccount.core.data.model.Transaction
import com.liuhy.myaccount.core.data.model.TransactionType
import com.liuhy.myaccount.core.database.dao.TransactionDao
import com.liuhy.myaccount.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class TransactionRepository(private val dao: TransactionDao) {

    fun getAllTransactions(): Flow<List<Transaction>> {
        return dao.getAll().map { list ->
            list.map { it.toModel() }
        }
    }

    fun getTransactionsByDateRange(start: LocalDate, end: LocalDate): Flow<List<Transaction>> {
        return dao.getByDateRange(start.toEpochDay(), end.toEpochDay()).map { list ->
            list.map { it.toModel() }
        }
    }

    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> {
        return dao.getByType(type.value).map { list ->
            list.map { it.toModel() }
        }
    }

    suspend fun getTransactionById(id: Long): Transaction? {
        return dao.getById(id)?.toModel()
    }

    suspend fun insert(transaction: Transaction): Long {
        return dao.insert(transaction.toEntity())
    }

    suspend fun update(transaction: Transaction) {
        dao.update(transaction.toEntity())
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    suspend fun getTransactionsByCategory(categoryId: Long): List<Transaction> {
        return dao.getByCategoryId(categoryId).map { it.toModel() }
    }

    suspend fun countByCategoryId(categoryId: Long): Int {
        return dao.countByCategoryId(categoryId)
    }

    suspend fun deleteByCategoryId(categoryId: Long) {
        dao.deleteByCategoryId(categoryId)
    }

    suspend fun getMonthlyTotal(type: TransactionType, yearMonth: java.time.YearMonth): Double? {
        val start = yearMonth.atDay(1)
        val end = yearMonth.atEndOfMonth()
        return dao.getSumByTypeAndDateRange(type.value, start, end)
    }

    fun getTotalByType(type: TransactionType): Flow<Double?> {
        return dao.getTotalByType(type.value)
    }

    fun getMonthlyTotalByType(type: TransactionType): Flow<Double?> {
        val now = java.time.YearMonth.now()
        val startOfMonth = now.atDay(1).toEpochDay()
        val endOfMonth = now.atEndOfMonth().toEpochDay()
        return dao.getMonthlyTotalByType(type.value, startOfMonth, endOfMonth)
    }

    private fun TransactionEntity.toModel(): Transaction {
        return Transaction(
            id = id,
            amount = amount,
            type = TransactionType.fromValue(type),
            categoryId = categoryId,
            note = note,
            date = date,
            createdAt = createdAt
        )
    }

    private fun Transaction.toEntity(): TransactionEntity {
        return TransactionEntity(
            id = id,
            amount = amount,
            type = type.value,
            categoryId = categoryId,
            note = note,
            date = date,
            createdAt = createdAt
        )
    }
}
