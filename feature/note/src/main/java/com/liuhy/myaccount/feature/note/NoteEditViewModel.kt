/**
 * 笔记编辑 ViewModel。
 *
 * 管理笔记的标题、内容等 UI 状态，
 * noteId 存在时从数据库加载已有内容，保存时写入数据库。
 */
package com.liuhy.myaccount.feature.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liuhy.myaccount.core.data.di.RepositoryProvider
import com.liuhy.myaccount.core.data.model.Note
import com.liuhy.myaccount.core.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NoteEditViewModel(
    private val noteRepo: NoteRepository,
    private val noteId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditUiState())
    val uiState: StateFlow<NoteEditUiState> = _uiState

    init {
        if (noteId != null && noteId > 0) {
            viewModelScope.launch {
                noteRepo.getNoteById(noteId)?.let { note ->
                    _uiState.value = NoteEditUiState(
                        title = note.title,
                        content = note.content,
                        linkedTransactionId = note.linkedTransactionId
                    )
                }
            }
        }
    }

    fun setTitle(value: String) {
        _uiState.value = _uiState.value.copy(title = value)
    }

    fun setContent(value: String) {
        _uiState.value = _uiState.value.copy(content = value)
    }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank()) return

        viewModelScope.launch {
            val note = Note(
                id = noteId ?: 0,
                title = state.title,
                content = state.content,
                linkedTransactionId = state.linkedTransactionId
            )
            if (noteId != null && noteId > 0) {
                noteRepo.update(note)
            } else {
                noteRepo.insert(note)
            }
            onSuccess()
        }
    }

    companion object {
        fun factory(noteId: Long? = null): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NoteEditViewModel(RepositoryProvider.noteRepository(), noteId) as T
                }
            }
        }
    }
}

data class NoteEditUiState(
    val title: String = "",
    val content: String = "",
    val linkedTransactionId: Long? = null
)
