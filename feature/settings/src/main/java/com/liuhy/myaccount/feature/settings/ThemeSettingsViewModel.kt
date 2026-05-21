/**
 * 设置 ViewModel。
 *
 * 管理主题选项和预算值两个持久化偏好设置，
 * 通过 ThemePreferences（DataStore）读写，UI 层通过 StateFlow 订阅。
 */
package com.liuhy.myaccount.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liuhy.myaccount.core.common.ThemeOption
import com.liuhy.myaccount.core.data.ThemePreferences
import com.liuhy.myaccount.core.data.di.RepositoryProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeSettingsViewModel(private val themePrefs: ThemePreferences) : ViewModel() {

    val currentTheme: StateFlow<ThemeOption> = themePrefs.themeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeOption.MATERIAL)

    val budget: StateFlow<Float> = themePrefs.budgetFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    fun setTheme(theme: ThemeOption) {
        viewModelScope.launch {
            themePrefs.setTheme(theme)
        }
    }

    fun setBudget(budget: Float) {
        viewModelScope.launch {
            themePrefs.setBudget(budget)
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ThemeSettingsViewModel(ThemePreferences(RepositoryProvider.getContext())) as T
            }
        }
    }
}
