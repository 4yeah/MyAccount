/**
 * 统计页面 ViewModel。
 *
 * 管理月份、收支类型、统计维度（一级/二级分类）等筛选状态，
 * 组合多个 Flow 输出按月/按年/按分类的汇总数据供 UI 订阅。
 */
package com.liuhy.myaccount.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liuhy.myaccount.core.data.di.RepositoryProvider
import com.liuhy.myaccount.core.data.model.TransactionType
import com.liuhy.myaccount.core.data.repository.StatisticsRepository
import com.liuhy.myaccount.core.data.repository.StatisticsRepository.CategoryDimension
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

class StatisticsViewModel(
    private val statisticsRepo: StatisticsRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    private val _selectedType = MutableStateFlow(TransactionType.EXPENSE)
    private val _isYearlyMode = MutableStateFlow(false)
    private val _dimension = MutableStateFlow(CategoryDimension.LEVEL_2)

    val selectedMonth = _selectedMonth
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), YearMonth.now())

    val selectedType = _selectedType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionType.EXPENSE)

    val selectedYearMode = _isYearlyMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val dimension = _dimension
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoryDimension.LEVEL_2)

    val monthlySummary = combine(_selectedMonth, _isYearlyMode) { month, isYearly ->
        month to isYearly
    }.flatMapLatest { (month, isYearly) ->
        statisticsRepo.getMonthlySummary(month, isYearly)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatisticsRepository.MonthlySummary(0.0, 0.0, 0.0))

    val categoryBreakdown = combine(_selectedMonth, _selectedType, _isYearlyMode, _dimension) { month, type, isYearly, dim ->
        statisticsRepo.getCategoryBreakdown(month, type, isYearly, dim)
    }.flatMapLatest { it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setMonth(month: YearMonth) {
        _selectedMonth.value = month
    }

    fun setYear(year: Int) {
        _selectedMonth.value = YearMonth.of(year, 1)
    }

    fun setType(type: TransactionType) {
        _selectedType.value = type
    }

    fun setYearlyMode(isYearly: Boolean) {
        _isYearlyMode.value = isYearly
    }

    fun setDimension(dimension: CategoryDimension) {
        _dimension.value = dimension
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StatisticsViewModel(RepositoryProvider.statisticsRepository()) as T
            }
        }
    }
}
