package com.gravo.grabadora.ui.transcription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gravo.grabadora.AppContainer
import com.gravo.grabadora.data.db.RecordingWithTags
import com.gravo.grabadora.data.db.TranscriptEntity
import com.gravo.grabadora.data.settings.AppSettings
import com.gravo.grabadora.transcription.TranscriptionErrorKind
import com.gravo.grabadora.transcription.TranscriptionException
import com.gravo.grabadora.transcription.TranscriptionProviderId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TranscriptionViewModel(
    private val container: AppContainer,
    private val recordingId: Long,
) : ViewModel() {

    sealed interface TranscriptionUiState {
        data object Idle : TranscriptionUiState
        data object Running : TranscriptionUiState
        data class Error(val kind: TranscriptionErrorKind, val message: String) : TranscriptionUiState
        data object Done : TranscriptionUiState
    }

    private val _state = MutableStateFlow<TranscriptionUiState>(TranscriptionUiState.Idle)
    val state: StateFlow<TranscriptionUiState> = _state

    val recording: StateFlow<RecordingWithTags?> = container.recordingRepository.observeRecording(recordingId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val transcript: StateFlow<String?> = container.database.transcriptDao().observeById(recordingId)
        .map { it?.text }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val text = MutableStateFlow("")
    val query = MutableStateFlow("")

    val matchCount: StateFlow<Int> = combine(text, query) { t, q ->
        TranscriptionSearch.count(t, q)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val selectedLanguage = MutableStateFlow("")

    val provider: StateFlow<TranscriptionProviderId> = container.settingsRepository.settings
        .map { it.transcriptionProvider }
        .stateIn(viewModelScope, SharingStarted.Eagerly, TranscriptionProviderId.WHISPER)

    init {
        viewModelScope.launch {
            selectedLanguage.value = container.settingsRepository.settings.first().transcribeLanguage
        }
    }

    fun setLanguage(code: String) {
        selectedLanguage.value = code
    }

    fun setQuery(q: String) {
        query.value = q
    }

    fun start() {
        if (_state.value == TranscriptionUiState.Running) return
        _state.value = TranscriptionUiState.Running
        viewModelScope.launch {
            try {
                val r = container.transcriptionManager.transcribe(recordingId, selectedLanguage.value.ifBlank { null })
                text.value = r.text
                _state.value = TranscriptionUiState.Done
            } catch (e: TranscriptionException) {
                _state.value = TranscriptionUiState.Error(e.kind, e.message ?: "error")
            } catch (e: Exception) {
                _state.value = TranscriptionUiState.Error(TranscriptionErrorKind.NETWORK, e.message ?: "error")
            }
        }
    }

    fun saveText() {
        viewModelScope.launch {
            val dao = container.database.transcriptDao()
            val existing = dao.observeById(recordingId).first()
            val now = System.currentTimeMillis()
            val updated = if (existing != null) {
                existing.copy(text = text.value, updatedAt = now)
            } else {
                val settings = container.settingsRepository.settings.first()
                TranscriptEntity(
                    recordingId = recordingId,
                    text = text.value,
                    provider = settings.transcriptionProvider.name,
                    model = modelFor(settings),
                    language = selectedLanguage.value,
                    createdAt = now,
                    updatedAt = now,
                )
            }
            dao.upsert(updated)
        }
    }

    private fun modelFor(settings: AppSettings): String = when (settings.transcriptionProvider) {
        TranscriptionProviderId.WHISPER -> settings.trWhisperModel
        TranscriptionProviderId.GEMINI -> settings.trGeminiModel
        TranscriptionProviderId.DEEPGRAM -> settings.trDeepgramModel
    }

    companion object {
        fun factory(container: AppContainer, id: Long) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TranscriptionViewModel(container, id) as T
        }
    }
}
