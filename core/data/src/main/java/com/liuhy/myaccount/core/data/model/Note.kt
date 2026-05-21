/**
 * 笔记领域模型（UI 层直接使用）。
 *
 * - linkedTransactionId: 可关联到某条记账记录，方便记录消费场景
 * - createdAt / updatedAt: 创建和最后修改时间戳
 */
package com.liuhy.myaccount.core.data.model

data class Note(
    val id: Long = 0,
    val title: String,
    val content: String,
    val linkedTransactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
