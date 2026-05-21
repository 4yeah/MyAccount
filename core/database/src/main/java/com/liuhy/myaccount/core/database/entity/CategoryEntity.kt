/**
 * 分类数据库实体（Room @Entity）。
 *
 * 对应表 `categories`，与 [Category] 领域模型通过 Repository 进行互转。
 * 外键约束：parentId 自引用，通过应用层代码维护树形结构一致性。
 */
package com.liuhy.myaccount.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val type: Int, // 0 = expense, 1 = income
    val parentId: Long? = null, // null = 一级分类, 有值 = 二级分类
    val isDefault: Boolean = false, // 系统默认分类不可删除
    // 排序权重：同 (type, parentId) 组内按 sortOrder ASC, name ASC 显示。
    // 迁移老数据时统一赋 0，相同值会回退到 name 排序——用户拖拽后才会变。
    val sortOrder: Int = 0,
    // 是否可见：隐藏的分类在记账时不显示，但在分类管理中可见
    val isVisible: Boolean = true
)
