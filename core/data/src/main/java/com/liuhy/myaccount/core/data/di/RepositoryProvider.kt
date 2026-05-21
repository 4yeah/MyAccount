/**
 * 全局单例仓库提供者（手动依赖注入）。
 *
 * 由于项目暂未引入 Hilt/Koin 等 DI 框架，所有 Repository 都通过这个 object 统一创建和持有。
 * 在 [MainActivity.onCreate] 中调用 [initialize] 完成初始化后，
 * 各 ViewModel 可通过 [categoryRepository]、[transactionRepository] 等方法获取实例。
 */
package com.liuhy.myaccount.core.data.di

import android.content.Context
import com.liuhy.myaccount.core.data.repository.CategoryRepository
import com.liuhy.myaccount.core.data.repository.NoteRepository
import com.liuhy.myaccount.core.data.repository.StatisticsRepository
import com.liuhy.myaccount.core.data.repository.TransactionRepository
import com.liuhy.myaccount.core.database.DatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object RepositoryProvider {

    @Volatile
    private lateinit var appContext: Context

    @Volatile
    private var categoryRepo: CategoryRepository? = null
    @Volatile
    private var transactionRepo: TransactionRepository? = null
    @Volatile
    private var noteRepo: NoteRepository? = null
    @Volatile
    private var statisticsRepo: StatisticsRepository? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (categoryRepo != null) return

        val database = DatabaseProvider.getInstance(context)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        categoryRepo = CategoryRepository(database.categoryDao())
        transactionRepo = TransactionRepository(database.transactionDao())
        noteRepo = NoteRepository(database.noteDao())
        statisticsRepo = StatisticsRepository(database)

        // Insert default categories on first launch
        scope.launch {
            val allCategories = database.categoryDao().getAll().first()
            val hasLevel2Categories = allCategories.any { it.parentId != null }
            if (!hasLevel2Categories) {
                categoryRepo?.insertDefaults()
            }
        }
    }

    fun categoryRepository(): CategoryRepository {
        return categoryRepo ?: throw IllegalStateException("RepositoryProvider not initialized")
    }

    fun transactionRepository(): TransactionRepository {
        return transactionRepo ?: throw IllegalStateException("RepositoryProvider not initialized")
    }

    fun noteRepository(): NoteRepository {
        return noteRepo ?: throw IllegalStateException("RepositoryProvider not initialized")
    }

    fun statisticsRepository(): StatisticsRepository {
        return statisticsRepo ?: throw IllegalStateException("RepositoryProvider not initialized")
    }

    fun getContext(): Context {
        return appContext
    }
}
