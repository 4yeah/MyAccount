/**
 * 主题/预算/账本名称等用户偏好设置。
 *
 * 基于 Jetpack DataStore Preferences 持久化，
 * 所有字段均以 Flow 形式暴露，UI 层通过 collectAsState 订阅自动刷新。
 */
package com.liuhy.myaccount.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.liuhy.myaccount.core.common.ThemeOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class ThemePreferences(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_option")
    private val budgetKey = floatPreferencesKey("monthly_budget")
    private val bookNameKey = stringPreferencesKey("book_name")

    val themeFlow: Flow<ThemeOption> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[themeKey] ?: ThemeOption.MATERIAL.displayName
            ThemeOption.entries.find { it.displayName == themeName } ?: ThemeOption.MATERIAL
        }

    suspend fun setTheme(theme: ThemeOption) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = theme.displayName
        }
    }

    val budgetFlow: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[budgetKey] ?: 0f
        }

    suspend fun setBudget(budget: Float) {
        context.dataStore.edit { preferences ->
            preferences[budgetKey] = budget
        }
    }

    val bookNameFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[bookNameKey] ?: "我的账本"
        }

    suspend fun setBookName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[bookNameKey] = name
        }
    }
}
