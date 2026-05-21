/**
 * 记账记录领域模型（UI 层直接使用）。
 *
 * - type: EXPENSE（支出）或 INCOME（收入）
 * - categoryId: 关联的二级分类 id
 * - date: 交易发生的日期（LocalDate 通过 Converters 存为 epochDay）
 * - createdAt: 记录创建时间戳，用于列表排序
 */
package com.liuhy.myaccount.core.data.model

import java.time.LocalDate

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val categoryName: String = "",
    val note: String,
    val date: LocalDate,
    val createdAt: Long = System.currentTimeMillis()
)
