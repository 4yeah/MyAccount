/**
 * 记账记录数据库实体（Room @Entity）。
 *
 * 对应表 `transactions`，通过外键关联 categories 表（categoryId）。
 * 使用 RESTRICT 策略：被引用的分类不可直接删除，需先清理关联记录或一并删除。
 */
package com.liuhy.myaccount.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["categoryId"]), Index(value = ["date"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: Int, // 0 = expense, 1 = income
    val categoryId: Long,
    val note: String,
    val date: LocalDate,
    val createdAt: Long = System.currentTimeMillis()
)
