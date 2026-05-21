/**
 * 账本首页 ViewModel。
 *
 * 暴露所有交易记录、本月/总支出/收入等 Flow，
 * 供 [AccountBookScreen] 通过 collectAsState 订阅自动刷新。
 */
package com.liuhy.myaccount.feature.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liuhy.myaccount.core.data.di.RepositoryProvider
import com.liuhy.myaccount.core.data.model.TransactionType
import com.liuhy.myaccount.core.data.repository.CategoryRepository
import com.liuhy.myaccount.core.data.ThemePreferences
import com.liuhy.myaccount.core.data.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountBookViewModel(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val themePrefs: ThemePreferences
) : ViewModel() {

    val transactions = transactionRepo.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalExpense = transactionRepo.getTotalByType(TransactionType.EXPENSE)
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIncome = transactionRepo.getTotalByType(TransactionType.INCOME)
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyIncome = transactionRepo.getMonthlyTotalByType(TransactionType.INCOME)
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyExpense = transactionRepo.getMonthlyTotalByType(TransactionType.EXPENSE)
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val budget = themePrefs.budgetFlow
        .map { it.toDouble() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val bookName = themePrefs.bookNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "我的账本")

    val categories = categoryRepo.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionRepo.delete(id)
        }
    }

    // 【批量删除】同时删除多条记录
    fun deleteTransactions(ids: List<Long>) {
        viewModelScope.launch {
            ids.forEach { transactionRepo.delete(it) }
        }
    }

    // 【修改账本名称】
    fun setBookName(name: String) {
        viewModelScope.launch {
            themePrefs.setBookName(name)
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AccountBookViewModel(
                    RepositoryProvider.transactionRepository(),
                    RepositoryProvider.categoryRepository(),
                    ThemePreferences(RepositoryProvider.getContext())
                ) as T
            }
        }
    }
}
