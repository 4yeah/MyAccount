/**
 * 笔记业务仓库。
 *
 * 封装 NoteDao，提供笔记的增删改查，
 * 负责 Entity 与领域模型 [Note] 之间的转换。
 */
package com.liuhy.myaccount.core.data.repository

import com.liuhy.myaccount.core.data.model.Note
import com.liuhy.myaccount.core.database.dao.NoteDao
import com.liuhy.myaccount.core.database.entity.NoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepository(private val dao: NoteDao) {

    fun getAllNotes(): Flow<List<Note>> {
        return dao.getAll().map { list ->
            list.map { it.toModel() }
        }
    }

    suspend fun getNoteById(id: Long): Note? {
        return dao.getById(id)?.toModel()
    }

    fun searchNotes(query: String): Flow<List<Note>> {
        return dao.search(query).map { list ->
            list.map { it.toModel() }
        }
    }

    suspend fun insert(note: Note): Long {
        return dao.insert(note.toEntity())
    }

    suspend fun update(note: Note) {
        dao.update(note.toEntity().copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    private fun NoteEntity.toModel(): Note {
        return Note(
            id = id,
            title = title,
            content = content,
            linkedTransactionId = linkedTransactionId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun Note.toEntity(): NoteEntity {
        return NoteEntity(
            id = id,
            title = title,
            content = content,
            linkedTransactionId = linkedTransactionId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
