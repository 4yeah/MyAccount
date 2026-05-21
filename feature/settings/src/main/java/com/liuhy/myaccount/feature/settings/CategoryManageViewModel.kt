package com.liuhy.myaccount.feature.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liuhy.myaccount.core.data.model.Category
import com.liuhy.myaccount.core.data.model.Transaction
import com.liuhy.myaccount.core.data.model.TransactionType
import com.liuhy.myaccount.core.data.repository.CategoryRepository
import com.liuhy.myaccount.core.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 分类管理 ViewModel
 * 
 * 负责分类的增删改查和树形结构管理
 */
class CategoryManageViewModel(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    // 所有一级分类
    private val expenseLevel1 = categoryRepository.getLevel1ExpenseCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val incomeLevel1 = categoryRepository.getLevel1IncomeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 当前显示的分类（根据类型筛选）
    private val _currentType = MutableStateFlow(TransactionType.EXPENSE)
    val currentType: StateFlow<TransactionType> = _currentType.asStateFlow()
    
    // 合并收入和支出的一级分类
    val level1Categories: StateFlow<List<Category>> = combine(
        expenseLevel1,
        incomeLevel1,
        _currentType
    ) { expense, income, type ->
        if (type == TransactionType.EXPENSE) expense else income
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 所有二级分类
    val allChildren: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 展开状态管理
    private val _expandedIds = MutableStateFlow(setOf<Long>())
    val expandedIds: StateFlow<Set<Long>> = _expandedIds.asStateFlow()

    /**
     * 切换分类类型（支出/收入）
     */
    fun setCurrentType(type: TransactionType) {
        _currentType.value = type
    }
    
    /**
     * 切换分类展开/折叠状态
     */
    fun toggleExpand(categoryId: Long) {
        val current = _expandedIds.value
        _expandedIds.value = if (current.contains(categoryId)) {
            current - categoryId
        } else {
            current + categoryId
        }
    }

    /**
     * 添加分类
     *
     * 特殊逻辑：当新增一级分类（parentId == null）时，
     * 自动为其创建一个同名的二级分类，方便用户直接记账。
     * 该二级分类不是系统默认（isDefault = false），用户后续可删除。
     */
    fun addCategory(
        name: String,
        iconName: String,
        colorHex: String,
        type: TransactionType,
        parentId: Long? = null
    ) {
        viewModelScope.launch {
            try {
                val category = Category(
                    name = name,
                    iconName = iconName,
                    colorHex = colorHex,
                    type = type,
                    parentId = parentId,
                    isDefault = false
                )
                val newId = categoryRepository.insert(category)

                // 新增一级分类时，自动创建同名二级分类
                if (parentId == null) {
                    val child = Category(
                        name = name,
                        iconName = iconName,
                        colorHex = colorHex,
                        type = type,
                        parentId = newId,
                        isDefault = false
                    )
                    categoryRepository.insert(child)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 更新分类
     */
    fun updateCategory(category: Category) {
        viewModelScope.launch {
            try {
                categoryRepository.update(category)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 删除分类（带保护逻辑）
     */
    fun deleteCategory(id: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                categoryRepository.delete(id)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "删除失败")
            }
        }
    }

    /**
     * 查询分类绑定的交易记录列表
     */
    suspend fun getTransactionsByCategory(id: Long): List<Transaction> {
        return try {
            transactionRepository.getTransactionsByCategory(id)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 查询分类绑定的交易记录数量
     */
    suspend fun getTransactionCountByCategory(id: Long): Int {
        return try {
            transactionRepository.countByCategoryId(id)
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * 删除分类及其所有关联的交易记录
     *
     * 注意：如果删除的是一级分类，其下所有子分类也会被连带删除。
     * 因此需要先清理子分类关联的交易记录，否则外键约束会阻止删除。
     */
    fun deleteCategoryWithTransactions(id: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Log.i("CategoryManage", "deleteCategoryWithTransactions start, id=$id")
                // 先删除该分类关联的交易记录
                transactionRepository.deleteByCategoryId(id)
                Log.i("CategoryManage", "deleted transactions for category $id")
                // 如果是一级分类，还需删除所有子分类关联的交易记录
                val children = categoryRepository.getChildrenList(id)
                Log.i("CategoryManage", "found ${children.size} children for category $id")
                children.forEach { child ->
                    transactionRepository.deleteByCategoryId(child.id)
                    Log.i("CategoryManage", "deleted transactions for child ${child.id}")
                }
                // 再删除分类
                categoryRepository.delete(id)
                Log.i("CategoryManage", "deleted category $id")
                onSuccess()
            } catch (e: Exception) {
                Log.e("CategoryManage", "deleteCategoryWithTransactions failed", e)
                onError(e.message ?: "删除失败")
            }
        }
    }

    /**
     * 获取某分类的子分类
     */
    fun getChildren(parentId: Long): List<Category> {
        return allChildren.value.filter { it.parentId == parentId }
    }

    /**
     * 在当前类型（支出/收入）的一级分类列表中把第 [fromIndex] 项移动到第 [toIndex] 项。
     * 仅持久化新顺序，UI 会通过 Flow 自动刷新。
     */
    fun moveLevel1(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val current = level1Categories.value
        if (fromIndex !in current.indices || toIndex !in current.indices) return

        val reordered = current.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        viewModelScope.launch {
            try {
                categoryRepository.reorder(reordered.map { it.id })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 直接按新的 id 顺序写入 sortOrder，给 [ReorderableColumn] 这种「拖完一次给出整列新顺序」
     * 的入口用，比 moveLevel1 单步移动更稳健（无需推断中间步骤）。
     */
    fun applyLevel1Order(orderedIds: List<Long>) {
        if (orderedIds.isEmpty()) return
        viewModelScope.launch {
            try {
                categoryRepository.reorder(orderedIds)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 在某个一级分类的二级分类列表中把第 [fromIndex] 项移动到第 [toIndex] 项。
     */
    fun moveLevel2(parentId: Long, fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val siblings = allChildren.value.filter { it.parentId == parentId }
        if (fromIndex !in siblings.indices || toIndex !in siblings.indices) return

        val reordered = siblings.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        viewModelScope.launch {
            try {
                categoryRepository.reorder(reordered.map { it.id })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 切换分类可见性
     */
    fun toggleVisibility(id: Long) {
        viewModelScope.launch {
            try {
                Log.i("CategoryManage", "toggleVisibility called, id=$id")
                categoryRepository.toggleVisibility(id)
                Log.i("CategoryManage", "toggleVisibility success, id=$id")
            } catch (e: Exception) {
                Log.e("CategoryManage", "toggleVisibility failed", e)
                e.printStackTrace()
            }
        }
    }

    companion object {
        val Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return CategoryManageViewModel(
                    com.liuhy.myaccount.core.data.di.RepositoryProvider.categoryRepository(),
                    com.liuhy.myaccount.core.data.di.RepositoryProvider.transactionRepository()
                ) as T
            }
        }
    }
}
