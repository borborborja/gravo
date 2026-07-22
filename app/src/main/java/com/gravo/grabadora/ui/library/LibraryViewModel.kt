package com.gravo.grabadora.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gravo.grabadora.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(container: AppContainer) : ViewModel() {
    private val repo = container.recordingRepository

    val query = MutableStateFlow("")
    val tagFilter = MutableStateFlow(LibraryFilter.ALL_TAG)
    val sortBy = MutableStateFlow(SortBy.RECENT)

    val usedTags: StateFlow<List<String>> = repo.usedTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val items: StateFlow<List<LibraryItem>> =
        combine(repo.recordings, query, tagFilter, sortBy) { recordings, q, tag, sort ->
            val mapped = recordings.map { rec ->
                LibraryItem(
                    id = rec.recording.id,
                    name = rec.recording.name,
                    createdAt = rec.recording.createdAt,
                    durationMs = rec.recording.durationMs,
                    format = rec.recording.format,
                    tags = rec.tags.map { it.name },
                )
            }
            LibraryFilter.apply(mapped, q, tag, sort)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = LibraryViewModel(container) as T
        }
    }
}
