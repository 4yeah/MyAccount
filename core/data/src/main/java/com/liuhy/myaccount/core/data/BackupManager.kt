package com.liuhy.myaccount.core.data

import android.content.Context
import com.liuhy.myaccount.core.data.model.Category
import com.liuhy.myaccount.core.data.model.Note
import com.liuhy.myaccount.core.data.model.Transaction
import com.liuhy.myaccount.core.data.repository.CategoryRepository
import com.liuhy.myaccount.core.data.repository.NoteRepository
import com.liuhy.myaccount.core.data.repository.TransactionRepository
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 数据备份管理器：负责把本地数据库导出为 JSON，或从 JSON 恢复数据。
 *
 * 备份文件结构：
 * {
 *   "version": 1,
 *   "exportTime": "2026-05-01T12:00:00",
 *   "appName": "豪账本",
 *   "transactions": [...],
 *   "categories": [...],
 *   "notes": [...]
 * }
 */
class BackupManager(private val context: Context) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(BackupData::class.java).indent("  ")

    /**
     * 导出当前所有数据为 JSON 文件。
     *
     * @return 导出的 JSON 字符串
     */
    suspend fun exportToJson(
        transactionRepo: TransactionRepository,
        categoryRepo: CategoryRepository,
        noteRepo: NoteRepository
    ): String {
        val transactions = transactionRepo.getAllTransactions().first()
        val categories = categoryRepo.getAllCategories().first()
        val notes = noteRepo.getAllNotes().first()

        val backupData = BackupData(
            version = CURRENT_VERSION,
            exportTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            appName = "豪账本",
            transactions = transactions.map { it.toDto() },
            categories = categories.map { it.toDto() },
            notes = notes.map { it.toDto() }
        )

        return adapter.toJson(backupData) ?: ""
    }

    /**
     * 从 JSON 恢复数据到本地数据库。
     *
     * @param json JSON 字符串
     * @param transactionRepo 交易仓储
     * @param categoryRepo 分类仓储
     * @param noteRepo 笔记仓储
     * @param onProgress 进度回调 (0.0 ~ 1.0)
     */
    suspend fun importFromJson(
        json: String,
        transactionRepo: TransactionRepository,
        categoryRepo: CategoryRepository,
        noteRepo: NoteRepository,
        onProgress: (Double) -> Unit = {}
    ) {
        val backupData = adapter.fromJson(json) ?: throw IllegalStateException("JSON 解析失败")

        // 验证版本
        if (backupData.version > CURRENT_VERSION) {
            throw IllegalStateException("备份版本过新，请升级应用后再恢复")
        }

        // 清空现有数据（恢复时采用全量覆盖策略）
        onProgress(0.1)
        transactionRepo.deleteAll()
        onProgress(0.2)
        categoryRepo.deleteAll()
        onProgress(0.3)
        noteRepo.deleteAll()
        onProgress(0.4)

        // 恢复分类（因为交易和笔记可能依赖分类 id）
        val categoryMap = mutableMapOf<Long, Long>() // 旧 id -> 新 id
        backupData.categories.forEachIndexed { index, dto ->
            val newId = categoryRepo.insert(dto.toEntity())
            categoryMap[dto.id] = newId
            if (index % 10 == 0) onProgress(0.4 + (index.toDouble() / backupData.categories.size) * 0.2)
        }
        onProgress(0.6)

        // 恢复交易（更新分类 id 映射）
        backupData.transactions.forEachIndexed { index, dto ->
            val mappedDto = dto.copy(
                categoryId = categoryMap[dto.categoryId] ?: dto.categoryId
            )
            transactionRepo.insert(mappedDto.toEntity())
            if (index % 10 == 0) onProgress(0.6 + (index.toDouble() / backupData.transactions.size) * 0.3)
        }
        onProgress(0.9)

        // 恢复笔记
        backupData.notes.forEach { dto ->
            noteRepo.insert(dto.toEntity())
        }
        onProgress(1.0)
    }

    /**
     * 获取备份文件临时路径（用于上传前写入磁盘）
     */
    fun getTempBackupFile(): File {
        val fileName = "haozhangben_backup_${System.currentTimeMillis()}.json"
        return File(context.cacheDir, fileName)
    }

    companion object {
        const val CURRENT_VERSION = 1

        private const val AES_ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val KEY_SIZE = 32 // 256-bit
        private const val IV_SIZE = 16  // 128-bit

        /**
         * 用密码加密文本。
         * @param plainText 明文
         * @param password  用户密码
         * @return Base64(IV + cipherText)
         */
        fun encrypt(plainText: String, password: String): String {
            val iv = ByteArray(IV_SIZE).apply { SecureRandom().nextBytes(this) }
            val key = deriveKey(password)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
            }
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            return Base64.getEncoder().encodeToString(iv + encrypted)
        }

        /**
         * 用密码解密文本。
         * @param cipherText Base64(IV + cipherText)
         * @param password   用户密码
         * @return 明文
         */
        fun decrypt(cipherText: String, password: String): String {
            val decoded = Base64.getDecoder().decode(cipherText)
            val iv = decoded.copyOfRange(0, IV_SIZE)
            val encrypted = decoded.copyOfRange(IV_SIZE, decoded.size)
            val key = deriveKey(password)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
            }
            return String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }

        private fun deriveKey(password: String): SecretKeySpec {
            val digest = MessageDigest.getInstance("SHA-256")
            val keyBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
            return SecretKeySpec(keyBytes.copyOf(KEY_SIZE), AES_ALGORITHM)
        }
    }
}

// ---------- 数据模型 ----------

@JsonClass(generateAdapter = true)
data class BackupData(
    val version: Int,
    val exportTime: String,
    val appName: String,
    val transactions: List<TransactionDto>,
    val categories: List<CategoryDto>,
    val notes: List<NoteDto>
)

@JsonClass(generateAdapter = true)
data class TransactionDto(
    val id: Long,
    val amount: Double,
    val type: String,
    val categoryId: Long,
    val date: Long,  // LocalDate.toEpochDay()
    val note: String,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class CategoryDto(
    val id: Long,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val type: Int,
    val parentId: Long?,
    val sortOrder: Int,
    val isDefault: Boolean
)

@JsonClass(generateAdapter = true)
data class NoteDto(
    val id: Long,
    val title: String,
    val content: String,
    val linkedTransactionId: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

// ---------- Entity 与 DTO 互转 ----------

private fun Transaction.toDto() = TransactionDto(
    id = id,
    amount = amount,
    type = type.name,
    categoryId = categoryId,
    date = date.toEpochDay(),
    note = note,
    createdAt = createdAt
)

private fun TransactionDto.toEntity() = Transaction(
    id = id,
    amount = amount,
    type = com.liuhy.myaccount.core.data.model.TransactionType.valueOf(type),
    categoryId = categoryId,
    date = java.time.LocalDate.ofEpochDay(date),
    note = note,
    createdAt = createdAt
)

private fun Category.toDto() = CategoryDto(
    id = id,
    name = name,
    iconName = iconName,
    colorHex = colorHex,
    type = type.value,
    parentId = parentId,
    sortOrder = sortOrder,
    isDefault = isDefault
)

private fun CategoryDto.toEntity() = Category(
    id = id,
    name = name,
    iconName = iconName,
    colorHex = colorHex,
    type = com.liuhy.myaccount.core.data.model.TransactionType.fromValue(type),
    parentId = parentId,
    sortOrder = sortOrder,
    isDefault = isDefault
)

private fun Note.toDto() = NoteDto(
    id = id,
    title = title,
    content = content,
    linkedTransactionId = linkedTransactionId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun NoteDto.toEntity() = Note(
    id = id,
    title = title,
    content = content,
    linkedTransactionId = linkedTransactionId,
    createdAt = createdAt,
    updatedAt = updatedAt
)
