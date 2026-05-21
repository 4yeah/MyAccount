/**
 * 收支类型枚举。
 *
 * 数据库中存 Int（0 = 支出, 1 = 收入），
 * 通过 [fromValue] 安全地将数据库值转回枚举，避免硬编码。
 */
package com.liuhy.myaccount.core.data.model

enum class TransactionType(val value: Int) {
    EXPENSE(0),
    INCOME(1);

    companion object {
        fun fromValue(value: Int): TransactionType {
            return entries.find { it.value == value } ?: EXPENSE
        }
    }
}
