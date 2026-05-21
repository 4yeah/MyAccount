/**
 * 本地数据库入口（Room）。
 *
 * 版本演进：
 * - v1：基础表结构
 * - v2：新增 categories.parentId / isDefault
 * - v3：新增 categories.sortOrder（手动排序）
 * - v4：新增 categories.isVisible（隐藏/回收站）
 */
package com.liuhy.myaccount.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.liuhy.myaccount.core.database.dao.CategoryDao
import com.liuhy.myaccount.core.database.dao.NoteDao
import com.liuhy.myaccount.core.database.dao.TransactionDao
import com.liuhy.myaccount.core.database.entity.CategoryEntity
import com.liuhy.myaccount.core.database.entity.NoteEntity
import com.liuhy.myaccount.core.database.entity.TransactionEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE categories ADD COLUMN parentId INTEGER")
        database.execSQL("ALTER TABLE categories ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0")
    }
}

// v3：分类支持手动排序，新增 sortOrder 列。老数据全部置 0，
// 同 sortOrder 的项保留按 name 的回退排序，行为对老用户保持兼容。
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE categories ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
    }
}

// v4：分类支持隐藏功能，新增 isVisible 列。默认为 1（可见）。
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE categories ADD COLUMN isVisible INTEGER NOT NULL DEFAULT 1")
    }
}

@Database(
    entities = [CategoryEntity::class, TransactionEntity::class, NoteEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    // 三个 DAO 分别对应分类、记账记录、笔记三张表
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun noteDao(): NoteDao
}
