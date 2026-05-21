/**
 * 笔记列表 ViewModel。
 *
 * 暴露笔记列表和搜索关键词两个 Flow，
 * 组合后输出按关键词过滤后的笔记列表。
 */
package com.liuhy.myaccount.feature.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liuhy.myaccount.core.data.di.RepositoryProvider
import com.liuhy.myaccount.core.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteListViewModel(
    private val noteRepo: NoteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val notes = combine(noteRepo.getAllNotes(), _searchQuery) { notes, query ->
        if (query.isBlank()) notes
        else notes.filter { it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            noteRepo.delete(id)
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NoteListViewModel(RepositoryProvider.noteRepository()) as T
            }
        }
    }
}
