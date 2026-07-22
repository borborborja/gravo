package com.gravo.grabadora.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gravo.grabadora.AppContainer
import com.gravo.grabadora.data.db.AppDatabase
import com.gravo.grabadora.data.db.RecordingWithTags
import com.gravo.grabadora.player.PlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class DetailViewModel(
    private val container: AppContainer,
    private val recordingId: Long,
) : ViewModel() {
    private val repo = container.recordingRepository

    val player = PlayerController(container.app)
    val masterTags = AppDatabase.MASTER_TAGS

    val recording: StateFlow<RecordingWithTags?> = repo.observeRecording(recordingId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _peaks = MutableStateFlow(FloatArray(0))
    val peaks: StateFlow<FloatArray> = _peaks

    private var loadedPath: String? = null

    init {
        viewModelScope.launch {
            recording.collect { rec ->
                val path = rec?.recording?.filePath ?: return@collect
                if (path != loadedPath) {
                    loadedPath = path
                    player.load(File(path))
                    _peaks.value = FloatArray(0)
                    runCatching {
                        _peaks.value = com.gravo.grabadora.waveform.WaveformExtractor(container.app).peaks(File(path))
                    }
                }
            }
        }
    }

    fun toggleTag(tag: String) {
        val current = recording.value ?: return
        val has = current.tags.any { it.name == tag }
        viewModelScope.launch { repo.toggleTag(recordingId, tag, !has) }
    }

    fun rename(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.rename(recordingId, name.trim()) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            player.pause()
            repo.delete(recordingId)
            onDeleted()
        }
    }

    fun shareFile(): File? = recording.value?.recording?.filePath?.let { File(it) }

    override fun onCleared() {
        player.release()
    }

    companion object {
        fun factory(container: AppContainer, id: Long) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = DetailViewModel(container, id) as T
        }
    }
}
