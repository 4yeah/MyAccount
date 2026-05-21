package com.liuhy.myaccount.feature.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liuhy.myaccount.core.data.di.RepositoryProvider
import com.liuhy.myaccount.core.data.model.Transaction
import com.liuhy.myaccount.core.data.model.TransactionType
import com.liuhy.myaccount.core.data.repository.CategoryRepository
import com.liuhy.myaccount.core.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 统一的新增/编辑交易 ViewModel
 *
 * @param transactionId 为 null 表示新增模式，非 null 表示编辑模式
 */
class TransactionViewModel(
    private val transactionId: Long?,
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository
) : ViewModel() {

    val expenseCategories = categoryRepo.getExpenseCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomeCategories = categoryRepo.getIncomeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(
        TransactionUiState(
            isLoading = transactionId != null,
            date = if (transactionId == null) LocalDate.now() else null
        )
    )
    val uiState: StateFlow<TransactionUiState> = _uiState

    private val _navigateToAddCategory = MutableSharedFlow<Long?>()
    val navigateToAddCategory = _navigateToAddCategory

    init {
        if (transactionId != null) {
            loadTransaction()
        }
    }

    private fun loadTransaction() {
        viewModelScope.launch {
            val transaction = transactionRepo.getTransactionById(transactionId!!)
            if (transaction != null) {
                _uiState.value = TransactionUiState(
                    amount = transaction.amount.toString(),
                    type = transaction.type,
                    categoryId = transaction.categoryId,
                    note = transaction.note,
                    date = transaction.date,
                    isLoading = false
                )
            } else {
                _uiState.value = TransactionUiState(
                    isLoading = false,
                    error = "未找到记录",
                    date = LocalDate.now()
                )
            }
        }
    }

    fun setAmount(value: String) {
        _uiState.value = _uiState.value.copy(amount = value)
    }

    fun setType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(type = type, categoryId = null)
    }

    fun setCategory(id: Long) {
        _uiState.value = _uiState.value.copy(categoryId = id)
    }

    fun setNote(value: String) {
        _uiState.value = _uiState.value.copy(note = value)
    }

    fun setDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    /**
     * 触发新增分类事件
     */
    fun onAddCategory(parent: com.liuhy.myaccount.core.data.model.Category?) {
        viewModelScope.launch {
            _navigateToAddCategory.emit(parent?.id)
        }
    }

    /**
     * 应用一次性预填数据（仅在新增模式下有效）
     */
    fun applyPrefillOnce(amount: Double?, type: TransactionType?, note: String?) {
        if (transactionId != null) return // 编辑模式不处理预填
        if (alreadyPrefilled) return
        alreadyPrefilled = true
        val current = _uiState.value
        if (current.amount.isNotBlank()) return

        if (amount == null && type == null && note == null) return

        val newType = type ?: current.type
        _uiState.value = current.copy(
            amount = amount?.let { "%.2f".format(it) } ?: current.amount,
            type = newType,
            note = note ?: current.note
        )

        viewModelScope.launch {
            val flow = if (newType == TransactionType.EXPENSE) {
                categoryRepo.getExpenseCategories()
            } else {
                categoryRepo.getIncomeCategories()
            }
            val firstNonEmpty = flow.first { it.isNotEmpty() }
            if (_uiState.value.categoryId == null) {
                _uiState.value = _uiState.value.copy(categoryId = firstNonEmpty.first().id)
            }
        }
    }

    private var alreadyPrefilled: Boolean = false

    /**
     * 保存交易（新增时插入，编辑时更新）
     */
    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull() ?: return
        val categoryId = state.categoryId ?: return
        val date = state.date ?: return

        viewModelScope.launch {
            if (transactionId != null) {
                transactionRepo.update(
                    Transaction(
                        id = transactionId,
                        amount = amount,
                        type = state.type,
                        categoryId = categoryId,
                        note = state.note,
                        date = date
                    )
                )
            } else {
                transactionRepo.insert(
                    Transaction(
                        amount = amount,
                        type = state.type,
                        categoryId = categoryId,
                        note = state.note,
                        date = date
                    )
                )
            }
            onSuccess()
        }
    }

    companion object {
        fun Factory(transactionId: Long?) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TransactionViewModel(
                    transactionId,
                    RepositoryProvider.transactionRepository(),
                    RepositoryProvider.categoryRepository()
                ) as T
            }
        }
    }
}

data class TransactionUiState(
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val categoryId: Long? = null,
    val note: String = "",
    val date: LocalDate? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
