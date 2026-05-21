package com.liuhy.myaccount.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 备份配置存储：使用 DataStore Preferences 加密保存 OSS 凭证。
 */
private val Context.backupDataStore by preferencesDataStore(name = "backup_prefs")

class BackupPreferences(private val context: Context) {

    private object Keys {
        val ENDPOINT = stringPreferencesKey("oss_endpoint")
        val ACCESS_KEY_ID = stringPreferencesKey("oss_access_key_id")
        val ACCESS_KEY_SECRET = stringPreferencesKey("oss_access_key_secret")
        val BUCKET_NAME = stringPreferencesKey("oss_bucket_name")
        val USER_ID = stringPreferencesKey("oss_user_id")
        val PASSWORD = stringPreferencesKey("oss_password")
    }

    /**
     * 配置数据流
     */
    val configFlow: Flow<OSSConfig?> = context.backupDataStore.data.map { prefs ->
        val endpoint = prefs[Keys.ENDPOINT]
        val accessKeyId = prefs[Keys.ACCESS_KEY_ID]
        val accessKeySecret = prefs[Keys.ACCESS_KEY_SECRET]
        val bucketName = prefs[Keys.BUCKET_NAME]
        val userId = prefs[Keys.USER_ID] ?: ""
        val password = prefs[Keys.PASSWORD] ?: ""

        if (endpoint != null && accessKeyId != null && accessKeySecret != null && bucketName != null) {
            OSSConfig(endpoint, accessKeyId, accessKeySecret, bucketName, userId, password)
        } else {
            null
        }
    }

    /**
     * 保存配置
     */
    suspend fun saveConfig(config: OSSConfig) {
        context.backupDataStore.edit { prefs ->
            prefs[Keys.ENDPOINT] = config.endpoint
            prefs[Keys.ACCESS_KEY_ID] = config.accessKeyId
            prefs[Keys.ACCESS_KEY_SECRET] = config.accessKeySecret
            prefs[Keys.BUCKET_NAME] = config.bucketName
            prefs[Keys.USER_ID] = config.userId
            prefs[Keys.PASSWORD] = config.password
        }
    }

    /**
     * 清除配置
     */
    suspend fun clearConfig() {
        context.backupDataStore.edit { prefs ->
            prefs.remove(Keys.ENDPOINT)
            prefs.remove(Keys.ACCESS_KEY_ID)
            prefs.remove(Keys.ACCESS_KEY_SECRET)
            prefs.remove(Keys.BUCKET_NAME)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.PASSWORD)
        }
    }
}

/**
 * OSS 配置数据类
 */
data class OSSConfig(
    val endpoint: String,
    val accessKeyId: String,
    val accessKeySecret: String,
    val bucketName: String,
    val userId: String = "",
    val password: String = ""
)
