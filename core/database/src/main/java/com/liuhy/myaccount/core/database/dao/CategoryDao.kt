/**
 * 分类（Category）数据访问对象。
 *
 * 核心查询策略：
 * - 管理界面用 `getLevel1ByType`：不过滤 isVisible，确保被隐藏的父分类仍能显示出来。
 * - 记账页面用 `getByType`：排除已隐藏的二级分类，避免用户选到不可用的分类。
 */
package com.liuhy.myaccount.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.liuhy.myaccount.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    // 同 (type, parentId) 组内：先按 sortOrder（用户拖拽设的权重），再按 name 兜底。
    // 管理界面需要看到所有一级分类（包括被误隐藏的），所以不过滤 isVisible
    @Query("SELECT * FROM categories WHERE type = :type AND parentId IS NULL ORDER BY sortOrder ASC, name ASC")
    fun getLevel1ByType(type: Int): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type AND (parentId IS NULL OR isVisible = 1) ORDER BY sortOrder ASC, name ASC")
    fun getByType(type: Int): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parentId = :parentId AND isVisible = 1 ORDER BY sortOrder ASC, name ASC")
    fun getChildren(parentId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories WHERE parentId = :parentId")
    suspend fun countChildren(parentId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Update
    suspend fun updateAll(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Query("UPDATE categories SET isVisible = :isVisible WHERE id = :id")
    suspend fun updateVisibility(id: Long, isVisible: Boolean)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
