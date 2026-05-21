/**
 * 笔记数据库实体（Room @Entity）。
 *
 * 对应表 `notes`，通过外键关联 transactions 表（linkedTransactionId）。
 * 当关联的交易记录被删除时，外键置空（SET_NULL），笔记本身保留。
 */
package com.liuhy.myaccount.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedTransactionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["linkedTransactionId"])]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val linkedTransactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
