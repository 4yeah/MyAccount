/**
 * 记账记录（Transaction）数据访问对象。
 *
 * 支持按日期范围、类型、分类等多维度查询，
 * 以及增删改基础操作。返回 Flow 的方法可被 Compose collectAsState 订阅自动刷新。
 */
package com.liuhy.myaccount.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.liuhy.myaccount.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date >= :start AND date <= :end ORDER BY date DESC")
    fun getByDateRange(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getByType(type: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC, createdAt DESC")
    suspend fun getByCategoryId(categoryId: Long): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countByCategoryId(categoryId: Long): Int

    @Query("DELETE FROM transactions WHERE categoryId = :categoryId")
    suspend fun deleteByCategoryId(categoryId: Long)

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND date BETWEEN :start AND :end")
    suspend fun getSumByTypeAndDateRange(type: Int, start: LocalDate, end: LocalDate): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type")
    fun getTotalByType(type: Int): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND date >= :startOfMonth AND date <= :endOfMonth")
    fun getMonthlyTotalByType(type: Int, startOfMonth: Long, endOfMonth: Long): Flow<Double?>
}
