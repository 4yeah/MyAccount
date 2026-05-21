package com.liuhy.myaccount.feature.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liuhy.myaccount.core.data.BackupManager
import com.liuhy.myaccount.core.data.BackupPreferences
import com.liuhy.myaccount.core.data.OSSBackupService
import com.liuhy.myaccount.core.data.OSSConfig
import com.liuhy.myaccount.core.data.di.RepositoryProvider
import com.liuhy.myaccount.core.data.repository.CategoryRepository
import com.liuhy.myaccount.core.data.repository.NoteRepository
import com.liuhy.myaccount.core.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState

/**
 * 备份 ViewModel：协调备份/恢复流程。
 */
class BackupViewModel(
    application: Application,
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val noteRepo: NoteRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "BackupViewModel"

        /**
         * 从本地配置文件加载 OSS 配置
         * 如果配置文件不存在或解析失败，返回 null
         */
        private fun loadOSSConfigFromLocalFile(context: android.content.Context): OSSConfig? {
            return try {
                val configFile = File(context.filesDir, "oss-config.json")
                if (!configFile.exists()) return null
                
                val jsonString = configFile.readText()
                val json = org.json.JSONObject(jsonString)
                
                OSSConfig(
                    endpoint = json.optString("endpoint"),
                    accessKeyId = json.optString("accessKeyId"),
                    accessKeySecret = json.optString("accessKeySecret"),
                    bucketName = json.optString("bucketName")
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        /**
         * 开发者默认 OSS 配置（内置，供所有用户使用）
         * 用户只需填写用户标识和密码即可使用
         * 注意：实际使用时应从 oss-config.json 加载，避免硬编码敏感信息
         */
        val DEFAULT_OSS_CONFIG = loadOSSConfigFromLocalFile(RepositoryProvider.getContext()) ?: OSSConfig(
            endpoint = "oss-cn-beijing.aliyuncs.com",
            accessKeyId = "",
            accessKeySecret = "",
            bucketName = ""
        )

        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val ctx = RepositoryProvider.getContext()
                return BackupViewModel(
                    ctx as Application,
                    RepositoryProvider.transactionRepository(),
                    RepositoryProvider.categoryRepository(),
                    RepositoryProvider.noteRepository()
                ) as T
            }
        }
    }

    private val backupManager = BackupManager(application)
    private val backupPrefs = BackupPreferences(application)

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    var showConfigDialog = mutableStateOf(false)
        internal set

    private val _configForm = mutableStateOf(ConfigForm())
    val configForm: MutableState<ConfigForm> = _configForm
    
    fun getConfigForm(): ConfigForm = _configForm.value

    init {
        viewModelScope.launch {
            backupPrefs.configFlow.collect { config ->
                _uiState.value = _uiState.value.copy(
                    isConfigured = config != null
                )
            }
        }
    }

    /**
     * 关闭配置对话框
     */
    fun dismissConfigDialog() {
        showConfigDialog.value = false
    }

    /**
     * 更新配置表单字段
     */
    fun updateConfigField(field: String, value: String) {
        _configForm.value = _configForm.value.copy(
            endpoint = if (field == "endpoint") value else _configForm.value.endpoint,
            bucketName = if (field == "bucketName") value else _configForm.value.bucketName,
            accessKeyId = if (field == "accessKeyId") value else _configForm.value.accessKeyId,
            accessKeySecret = if (field == "accessKeySecret") value else _configForm.value.accessKeySecret,
            userId = if (field == "userId") value else _configForm.value.userId,
            password = if (field == "password") value else _configForm.value.password
        )
    }

    fun setUseDefaultConfig(useDefault: Boolean) {
        _configForm.value = _configForm.value.copy(
            useDefaultConfig = useDefault,
            // 切换到自定义模式时，清空敏感字段（只保留用户标识和密码）
            endpoint = if (useDefault) _configForm.value.endpoint else "",
            bucketName = if (useDefault) _configForm.value.bucketName else "",
            accessKeyId = if (useDefault) _configForm.value.accessKeyId else "",
            accessKeySecret = if (useDefault) _configForm.value.accessKeySecret else ""
        )
    }

    /**
     * 保存配置
     */
    fun saveConfig() {
        val form = _configForm.value
        
        if (form.useDefaultConfig) {
            // 默认配置模式：用户标识和密码必填
            if (form.userId.isBlank() || form.password.isBlank()) {
                _uiState.value = _uiState.value.copy(error = "使用默认配置时，用户标识和密码必须填写")
                return
            }
        } else {
            // 自定义模式：所有 OSS 参数必填
            if (form.endpoint.isBlank() || form.bucketName.isBlank() || 
                form.accessKeyId.isBlank() || form.accessKeySecret.isBlank()) {
                _uiState.value = _uiState.value.copy(error = "请填写所有 OSS 配置项")
                return
            }
        }
        
        viewModelScope.launch {
            try {
                val config = if (form.useDefaultConfig) {
                    // 默认配置模式：使用开发者内置 OSS 配置
                    if (form.userId.isBlank() || form.password.isBlank()) {
                        _uiState.value = _uiState.value.copy(error = "使用默认配置时，用户标识和密码必须填写")
                        return@launch
                    }
                    DEFAULT_OSS_CONFIG.copy(
                        userId = form.userId.trim(),
                        password = form.password.trim()
                    )
                } else {
                    // 自定义模式：使用表单填写的参数
                    OSSConfig(
                        endpoint = form.endpoint.trim(),
                        accessKeyId = form.accessKeyId.trim(),
                        accessKeySecret = form.accessKeySecret.trim(),
                        bucketName = form.bucketName.trim(),
                        userId = form.userId.trim(),
                        password = form.password.trim()
                    )
                }
                
                backupPrefs.saveConfig(config)
                showConfigDialog.value = false
                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "保存失败：${e.message}")
            }
        }
    }

    /**
     * 删除配置
     */
    fun clearConfig() {
        viewModelScope.launch {
            backupPrefs.clearConfig()
            showConfigDialog.value = false
            _uiState.value = _uiState.value.copy(error = null)
        }
    }

    /**
     * 加载当前配置到表单
     */
    fun loadConfigToForm() {
        viewModelScope.launch {
            backupPrefs.configFlow.first()?.let { config ->
                _configForm.value = ConfigForm(
                    endpoint = config.endpoint,
                    bucketName = config.bucketName,
                    accessKeyId = config.accessKeyId,
                    accessKeySecret = config.accessKeySecret,
                    userId = config.userId,
                    password = config.password,
                    useDefaultConfig = true // 加载时默认显示默认模式
                )
            } ?: run {
                // 本地没有配置时，使用开发者默认配置作为表单初始值
                _configForm.value = ConfigForm(
                    endpoint = DEFAULT_OSS_CONFIG.endpoint,
                    bucketName = DEFAULT_OSS_CONFIG.bucketName,
                    accessKeyId = DEFAULT_OSS_CONFIG.accessKeyId,
                    accessKeySecret = DEFAULT_OSS_CONFIG.accessKeySecret,
                    useDefaultConfig = true
                )
            }
        }
    }

    /**
     * 执行备份：导出 JSON → 上传到 OSS
     */
    fun backup() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isBackingUp = true,
                    backupProgress = 0.0,
                    error = null
                )

                // 1. 导出 JSON
                val json = backupManager.exportToJson(transactionRepo, categoryRepo, noteRepo)
                _uiState.value = _uiState.value.copy(backupProgress = 0.3)

                // 2. 加密（如有密码）并写入临时文件
                val config = backupPrefs.configFlow.first()
                    ?: throw IllegalStateException("未配置 OSS 信息")

                val contentToWrite = if (config.password.isNotBlank()) {
                    BackupManager.encrypt(json, config.password)
                } else {
                    json
                }

                val tempFile = backupManager.getTempBackupFile()
                tempFile.writeText(contentToWrite)
                _uiState.value = _uiState.value.copy(backupProgress = 0.5)

                // 3. 上传到 OSS
                val ossService = OSSBackupService(
                    getApplication(),
                    config.endpoint,
                    config.accessKeyId,
                    config.accessKeySecret,
                    config.bucketName
                )

                val prefix = if (config.userId.isNotBlank()) "backup/${config.userId}/" else "backup/"
                val objectKey = "${prefix}haozhangben_${System.currentTimeMillis()}.json"
                withContext(Dispatchers.IO) {
                    ossService.upload(tempFile, objectKey) { progress ->
                        _uiState.value = _uiState.value.copy(
                            backupProgress = 0.5 + progress * 0.5
                        )
                    }
                }

                // 4. 清理临时文件
                tempFile.delete()

                _uiState.value = _uiState.value.copy(
                    isBackingUp = false,
                    backupProgress = 1.0,
                    lastBackupTime = System.currentTimeMillis(),
                    error = null
                )

                Log.d("BackupViewModel", "备份成功: $objectKey")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBackingUp = false,
                    error = "备份失败：${e.message}"
                )
                Log.e("BackupViewModel", "备份失败", e)
            }
        }
    }

    /**
     * 自动恢复最新的备份文件
     */
    fun restoreLatest() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isRestoring = true,
                    restoreProgress = 0.0,
                    error = null
                )

                val config = backupPrefs.configFlow.first()
                    ?: throw IllegalStateException("未配置 OSS 信息")

                val ossService = OSSBackupService(
                    getApplication(),
                    config.endpoint,
                    config.accessKeyId,
                    config.accessKeySecret,
                    config.bucketName
                )

                // 1. 列出备份文件
                _uiState.value = _uiState.value.copy(restoreProgress = 0.1)
                val prefix = if (config.userId.isNotBlank()) "backup/${config.userId}/" else "backup/"
                val (allObjects, latestKey) = withContext(Dispatchers.IO) {
                    val objs = ossService.listObjects(prefix)
                    val filtered = objs
                        .filter { it.startsWith(prefix) }
                        .filter { !it.endsWith("/") } // 排除目录本身
                    Log.d(TAG, "listObjects 返回: ${objs.size} 个, 过滤后: ${filtered.size} 个")
                    
                    if (filtered.isEmpty()) {
                        throw IllegalStateException("云端 ${prefix} 目录下没有找到备份文件，请检查用户标识是否正确")
                    }
                    
                    val key = filtered.maxOrNull()
                        ?: throw IllegalStateException("云端没有找到备份文件")
                    Log.d(TAG, "最新备份: $key")
                    objs to key
                }

                // 2. 下载
                val tempFile = backupManager.getTempBackupFile()
                withContext(Dispatchers.IO) {
                    ossService.download(latestKey, tempFile) { progress ->
                        _uiState.value = _uiState.value.copy(restoreProgress = 0.1 + progress * 0.4)
                    }
                }

                _uiState.value = _uiState.value.copy(restoreProgress = 0.5)

                // 3. 解密并导入 JSON
                _uiState.value = _uiState.value.copy(restoreProgress = 0.5)
                val encryptedContent = tempFile.readText()

                // 尝试解密
                val json = tryDecrypt(encryptedContent, config.password)
                if (json != null) {
                    // 解密成功，直接恢复
                    doRestore(json, tempFile)
                } else {
                    // 需要密码：保存文件，弹出密码输入框
                    _uiState.value = _uiState.value.copy(
                        isRestoring = false,
                        showRestorePasswordDialog = true,
                        pendingRestoreFile = tempFile,
                        error = null
                    )
                }
            } catch (e: Exception) {
                val errorDetail = "${e.javaClass.simpleName}: ${e.message ?: "无详细错误信息"}"
                _uiState.value = _uiState.value.copy(
                    isRestoring = false,
                    error = "恢复失败：$errorDetail"
                )
                Log.e(TAG, "恢复失败", e)
            }
        }
    }

    /**
     * 尝试用密码解密内容。
     * 优先级：1. 尝试解密（如果配置了密码） 2. 尝试直接解析 JSON（明文或未加密旧备份）
     * @return 解密/解析后的明文，如果都失败返回 null
     */
    private fun tryDecrypt(content: String, password: String): String? {
        // 1. 如果配置了密码，先尝试解密
        if (password.isNotBlank()) {
            try {
                return BackupManager.decrypt(content, password)
            } catch (e: Exception) {
                // 解密失败，可能是明文旧备份，继续尝试直接解析
                Log.d(TAG, "配置密码解密失败，尝试作为明文解析")
            }
        }
        // 2. 尝试直接解析 JSON（明文备份）
        return try {
            org.json.JSONObject(content)
            content
        } catch (e: Exception) {
            // 既不是有效 JSON，也无法用密码解密
            null
        }
    }

    /**
     * 执行恢复操作（已解密的 JSON）
     */
    private fun doRestore(json: String, tempFile: File) {
        viewModelScope.launch {
            try {
                backupManager.importFromJson(
                    json = json,
                    transactionRepo = transactionRepo,
                    categoryRepo = categoryRepo,
                    noteRepo = noteRepo,
                    onProgress = { progress ->
                        _uiState.value = _uiState.value.copy(
                            restoreProgress = 0.5 + progress * 0.5
                        )
                    }
                )

                tempFile.delete()

                _uiState.value = _uiState.value.copy(
                    isRestoring = false,
                    restoreProgress = 1.0,
                    showRestorePasswordDialog = false,
                    pendingRestoreFile = null,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRestoring = false,
                    error = "恢复失败：${e.message}"
                )
                Log.e(TAG, "恢复失败", e)
            }
        }
    }

    /**
     * 提交恢复密码并继续恢复
     */
    fun submitRestorePassword(password: String) {
        val tempFile = _uiState.value.pendingRestoreFile ?: return
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isRestoring = true,
                    restoreProgress = 0.5,
                    showRestorePasswordDialog = false,
                    error = null
                )

                val encryptedContent = tempFile.readText()
                val json = BackupManager.decrypt(encryptedContent, password)
                doRestore(json, tempFile)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRestoring = false,
                    showRestorePasswordDialog = true,
                    error = "密码错误，请重新输入"
                )
                Log.e(TAG, "密码解密失败", e)
            }
        }
    }

    /**
     * 取消恢复密码输入
     */
    fun cancelRestorePassword() {
        _uiState.value.pendingRestoreFile?.delete()
        _uiState.value = _uiState.value.copy(
            showRestorePasswordDialog = false,
            pendingRestoreFile = null,
            isRestoring = false,
            error = null
        )
    }

    /**
     * 执行恢复：从 OSS 下载 → 导入 JSON（指定对象键）
     */
    fun restore(objectKey: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isRestoring = true,
                    restoreProgress = 0.0,
                    error = null
                )

                // 1. 从 OSS 下载
                val config = backupPrefs.configFlow.first()
                    ?: throw IllegalStateException("未配置 OSS 信息")

                val ossService = OSSBackupService(
                    getApplication(),
                    config.endpoint,
                    config.accessKeyId,
                    config.accessKeySecret,
                    config.bucketName
                )

                val tempFile = backupManager.getTempBackupFile()
                ossService.download(objectKey, tempFile) { progress ->
                    _uiState.value = _uiState.value.copy(restoreProgress = progress * 0.5)
                }

                _uiState.value = _uiState.value.copy(restoreProgress = 0.5)

                // 2. 导入 JSON
                val json = tempFile.readText()
                backupManager.importFromJson(
                    json = json,
                    transactionRepo = transactionRepo,
                    categoryRepo = categoryRepo,
                    noteRepo = noteRepo,
                    onProgress = { progress ->
                        _uiState.value = _uiState.value.copy(
                            restoreProgress = 0.5 + progress * 0.5
                        )
                    }
                )

                // 3. 清理临时文件
                tempFile.delete()

                _uiState.value = _uiState.value.copy(
                    isRestoring = false,
                    restoreProgress = 1.0,
                    error = null
                )

                Log.d("BackupViewModel", "恢复成功: $objectKey")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRestoring = false,
                    error = "恢复失败：${e.message}"
                )
                Log.e("BackupViewModel", "恢复失败", e)
            }
        }
    }
}

/**
 * 备份 UI 状态
 */
data class BackupUiState(
    val isConfigured: Boolean = false,
    val isBackingUp: Boolean = false,
    val backupProgress: Double = 0.0,
    val isRestoring: Boolean = false,
    val restoreProgress: Double = 0.0,
    val lastBackupTime: Long? = null,
    val showRestorePasswordDialog: Boolean = false,
    val pendingRestoreFile: File? = null,
    val error: String? = null
)

/**
 * 配置表单数据类
 */
data class ConfigForm(
    val endpoint: String = "",
    val bucketName: String = "",
    val accessKeyId: String = "",
    val accessKeySecret: String = "",
    val userId: String = "",
    val password: String = "",
    val useDefaultConfig: Boolean = true
)
