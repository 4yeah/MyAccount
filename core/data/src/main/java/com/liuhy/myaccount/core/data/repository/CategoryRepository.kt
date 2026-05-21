/**
 * 分类业务仓库。
 *
 * 职责：
 * 1. 管理支出/收入一级、二级分类的增删改查。
 * 2. 维护默认分类（insertDefaults），新装/升级时自动补充缺失数据。
 * 3. 处理排序（reorder）、隐藏（toggleVisibility）、删除（delete）等业务规则。
 * 4. 一级分类允许连带删除所有子分类；二级分类删除时保证父类下至少保留一个。
 */
package com.liuhy.myaccount.core.data.repository

import android.util.Log
import com.liuhy.myaccount.core.data.model.Category
import com.liuhy.myaccount.core.data.model.TransactionType
import com.liuhy.myaccount.core.database.dao.CategoryDao
import com.liuhy.myaccount.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryRepository(private val dao: CategoryDao) {

    fun getExpenseCategories(): Flow<List<Category>> {
        return dao.getByType(TransactionType.EXPENSE.value).map { list ->
            list.map { it.toModel() }
        }
    }

    fun getIncomeCategories(): Flow<List<Category>> {
        return dao.getByType(TransactionType.INCOME.value).map { list ->
            list.map { it.toModel() }
        }
    }

    fun getLevel1ExpenseCategories(): Flow<List<Category>> {
        return dao.getLevel1ByType(TransactionType.EXPENSE.value).map { list ->
            list.map { it.toModel() }
        }
    }

    fun getLevel1IncomeCategories(): Flow<List<Category>> {
        return dao.getLevel1ByType(TransactionType.INCOME.value).map { list ->
            list.map { it.toModel() }
        }
    }

    fun getCategoryChildren(parentId: Long): Flow<List<Category>> {
        return dao.getChildren(parentId).map { list ->
            list.map { it.toModel() }
        }
    }

    fun getAllCategories(): Flow<List<Category>> {
        return dao.getAll().map { list ->
            list.map { it.toModel() }
        }
    }

    suspend fun insert(category: Category): Long {
        val entity = category.toEntity()
        val newId = dao.insert(entity)
        
        // 新增分类时，自动将其 sortOrder 设置为当前最大 sortOrder + 1，确保排在最后
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val maxSortOrder = dao.getAll().first()
                    .filter { it.type == entity.type && it.parentId == entity.parentId }
                    .maxOfOrNull { it.sortOrder } ?: -1
                dao.updateSortOrder(newId, maxSortOrder + 1)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return newId
    }

    suspend fun update(category: Category) {
        dao.update(category.toEntity())
    }

    /**
     * 获取指定父分类下的所有子分类（同步返回列表，用于删除等场景）
     */
    suspend fun getChildrenList(parentId: Long): List<Category> {
        return dao.getAll().first()
            .filter { it.parentId == parentId }
            .map { it.toModel() }
    }

    suspend fun insertDefaults() {
        // 检查是否已有二级分类
        val existingCategories = dao.getAll().first()
        val hasLevel2Categories = existingCategories.any { it.parentId != null }
        
        // 如果没有任何数据，插入所有一二级分类
        if (existingCategories.isEmpty()) {
            insertAllDefaultCategories()
            return
        }
        
        // 如果有一级分类但没有二级分类，只补充二级分类
        if (!hasLevel2Categories) {
            insertMissingLevel2Categories(existingCategories)
        }
    }
    
    /**
     * 插入所有一二级分类（全新安装时使用）
     */
    private suspend fun insertAllDefaultCategories() {
        // 一级分类 - 支出
        val expenseLevel1 = listOf(
            CategoryEntity(name = "餐饮", iconName = "restaurant", colorHex = "#FF5722", type = 0, isDefault = true),
            CategoryEntity(name = "交通", iconName = "directions_car", colorHex = "#2196F3", type = 0, isDefault = true),
            CategoryEntity(name = "购物", iconName = "shopping_cart", colorHex = "#E91E63", type = 0, isDefault = true),
            CategoryEntity(name = "娱乐", iconName = "movie", colorHex = "#9C27B0", type = 0, isDefault = true),
            CategoryEntity(name = "居住", iconName = "home", colorHex = "#795548", type = 0, isDefault = true),
            CategoryEntity(name = "医疗", iconName = "local_hospital", colorHex = "#F44336", type = 0, isDefault = true)
        )
        
        // 一级分类 - 收入
        val incomeLevel1 = listOf(
            CategoryEntity(name = "工资", iconName = "attach_money", colorHex = "#4CAF50", type = 1, isDefault = true),
            CategoryEntity(name = "奖金", iconName = "card_giftcard", colorHex = "#8BC34A", type = 1, isDefault = true),
            CategoryEntity(name = "投资", iconName = "trending_up", colorHex = "#00BCD4", type = 1, isDefault = true),
            CategoryEntity(name = "兼职", iconName = "work", colorHex = "#FFC107", type = 1, isDefault = true)
        )

        // 插入一级分类
        dao.insertAll(expenseLevel1 + incomeLevel1)

        // 为每个一级分类创建一个同名的二级分类作为默认选项
        val allLevel1 = dao.getAll().first().filter { it.parentId == null }
        val sameNameChildren = allLevel1.map { parent ->
            CategoryEntity(
                name = parent.name,  // 同名
                iconName = parent.iconName,
                colorHex = parent.colorHex,
                type = parent.type,
                parentId = parent.id,
                isDefault = true
            )
        }
        dao.insertAll(sameNameChildren)

        // 插入其他二级分类 - 餐饮
        val foodCategory = allLevel1.find { it.name == "餐饮" }
        if (foodCategory != null) {
            dao.insertAll(listOf(
                CategoryEntity(name = "早餐", iconName = "free_breakfast", colorHex = "#FF5722", type = 0, parentId = foodCategory.id, isDefault = true),
                CategoryEntity(name = "午餐", iconName = "lunch_dining", colorHex = "#FF5722", type = 0, parentId = foodCategory.id, isDefault = true),
                CategoryEntity(name = "晚餐", iconName = "dinner_dining", colorHex = "#FF5722", type = 0, parentId = foodCategory.id, isDefault = true),
                CategoryEntity(name = "零食", iconName = "cookie", colorHex = "#FF5722", type = 0, parentId = foodCategory.id, isDefault = true)
            ))
        }

        // 插入其他二级分类 - 交通
        val transportCategory = allLevel1.find { it.name == "交通" }
        if (transportCategory != null) {
            dao.insertAll(listOf(
                CategoryEntity(name = "公交地铁", iconName = "directions_bus", colorHex = "#2196F3", type = 0, parentId = transportCategory.id, isDefault = true),
                CategoryEntity(name = "打车", iconName = "local_taxi", colorHex = "#2196F3", type = 0, parentId = transportCategory.id, isDefault = true),
                CategoryEntity(name = "加油", iconName = "local_gas_station", colorHex = "#2196F3", type = 0, parentId = transportCategory.id, isDefault = true)
            ))
        }

        // 插入其他二级分类 - 购物
        val shoppingCategory = allLevel1.find { it.name == "购物" }
        if (shoppingCategory != null) {
            dao.insertAll(listOf(
                CategoryEntity(name = "日用品", iconName = "shopping_bag", colorHex = "#E91E63", type = 0, parentId = shoppingCategory.id, isDefault = true),
                CategoryEntity(name = "服装", iconName = "checkroom", colorHex = "#E91E63", type = 0, parentId = shoppingCategory.id, isDefault = true),
                CategoryEntity(name = "数码", iconName = "devices", colorHex = "#E91E63", type = 0, parentId = shoppingCategory.id, isDefault = true)
            ))
        }
    }
    
    /**
     * 为已有的一级分类补充二级分类（旧版本升级时使用）
     */
    private suspend fun insertMissingLevel2Categories(existingCategories: List<CategoryEntity>) {
        val level1Categories = existingCategories.filter { it.parentId == null }
        
        // 为每个一级分类创建同名二级分类
        val sameNameChildren = level1Categories.map { parent ->
            CategoryEntity(
                name = parent.name,
                iconName = parent.iconName,
                colorHex = parent.colorHex,
                type = parent.type,
                parentId = parent.id,
                isDefault = true
            )
        }
        dao.insertAll(sameNameChildren)
        
        Log.d("CategoryRepository", "已为 ${level1Categories.size} 个一级分类补充同名二级分类")
    }

    /**
     * 批量更新 sortOrder。
     *
     * 入参 [orderedIds] 是某一组（同 type、同 parentId）分类的 id 序列，
     * 顺序就代表新的展示顺序。本方法把 sortOrder 写成 0..N-1。
     *
     * 注意：只更新 sortOrder 字段，不动其他字段，避免与并发的编辑冲突。
     */
    suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            dao.updateSortOrder(id, index)
        }
    }

    /**
     * 切换分类可见性
     */
    suspend fun toggleVisibility(id: Long) {
        val category = dao.getById(id)
        Log.i("CategoryRepo", "toggleVisibility getById id=$id, category=$category")
        if (category == null) {
            Log.w("CategoryRepo", "toggleVisibility: category not found, id=$id")
            return
        }
        val newValue = !category.isVisible
        Log.i("CategoryRepo", "toggleVisibility updating id=$id, old=${category.isVisible}, new=$newValue")
        dao.updateVisibility(id, newValue)
    }

    suspend fun delete(id: Long) {
        val category = dao.getById(id) ?: return

        // 一级分类的默认分类不可删除，二级分类都可以删除
        if (category.parentId == null && category.isDefault) {
            throw IllegalStateException("默认分类不可删除")
        }

        val parentId = category.parentId
        if (parentId == null) {
            // 一级分类：允许删除，连带删除所有子分类
            val children = dao.getAll().first().filter { it.parentId == category.id }
            children.forEach { child ->
                dao.deleteById(child.id)
            }
            dao.deleteById(id)
        } else {
            // 二级分类：删除后父分类下必须至少保留一个二级分类
            val siblingCount = dao.countChildren(parentId)
            if (siblingCount <= 1) {
                throw IllegalStateException("每个一级分类下必须保留至少一个二级分类")
            }
            dao.deleteById(id)
        }
    }

    /**
     * 清空所有分类（备份恢复时使用，跳过所有校验）
     */
    suspend fun deleteAll() {
        dao.deleteAll()
    }

    private fun CategoryEntity.toModel(): Category {
        return Category(
            id = id,
            name = name,
            iconName = iconName,
            colorHex = colorHex,
            type = TransactionType.fromValue(type),
            parentId = parentId,
            isDefault = isDefault,
            sortOrder = sortOrder,
            isVisible = isVisible
        )
    }

    private fun Category.toEntity(): CategoryEntity {
        return CategoryEntity(
            id = id,
            name = name,
            iconName = iconName,
            colorHex = colorHex,
            type = type.value,
            parentId = parentId,
            isDefault = isDefault,
            sortOrder = sortOrder,
            isVisible = isVisible
        )
    }
}
