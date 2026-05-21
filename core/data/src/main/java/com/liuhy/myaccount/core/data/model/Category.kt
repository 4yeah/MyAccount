/**
 * 分类领域模型（UI 层直接使用）。
 *
 * - id: 数据库主键，0 表示未持久化的新分类
 * - parentId: null 为一级分类，有值则为对应一级分类下的二级分类
 * - isDefault: 系统预置分类，不允许删除
 * - sortOrder: 同级分类的展示顺序，越小越靠前
 * - isVisible: false 表示已隐藏（放入回收站），记账页不可见但历史记录保留
 */
package com.liuhy.myaccount.core.data.model

data class Category(
    val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val type: TransactionType,
    val parentId: Long? = null,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
    val isVisible: Boolean = true
)
