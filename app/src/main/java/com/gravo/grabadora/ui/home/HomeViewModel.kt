package com.gravo.grabadora.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gravo.grabadora.AppContainer
import com.gravo.grabadora.audio.BitDepth
import com.gravo.grabadora.audio.RecStatus
import com.gravo.grabadora.audio.RecordFormat
import com.gravo.grabadora.data.settings.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val controller = container.recordingController
    private val settingsRepo = container.settingsRepository

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val status: StateFlow<RecStatus> = controller.status
    val elapsedMs: StateFlow<Long> = controller.elapsedMs
    val levels: StateFlow<FloatArray> = controller.meter.levels
    val peakDb: StateFlow<Float> = controller.meter.peakDb

    /** Llamado cuando la Home es visible y hay permiso de micrófono. */
    fun startMonitoring() {
        viewModelScope.launch {
            val s = settings.first()
            controller.startMonitoring(s.toSpec(), s.micId)
        }
    }

    fun stopMonitoring() = controller.stopMonitoring()

    fun onRecordTap() {
        viewModelScope.launch {
            when (status.value) {
                RecStatus.IDLE -> {
                    val s = settingsRepo.settings.first()
                    controller.startRecording(s.toSpec(), s.micId)
                }
                else -> controller.togglePause()
            }
        }
    }

    /** Mantener pulsado completado → finalizar y navegar al detalle. */
    fun onRecordHoldComplete(defaultNameTemplate: String, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val settings = settingsRepo.settings.first()
            val id = controller.finishRecording(defaultNameTemplate)
            if (id != null) {
                if (settings.autoUpload) {
                    container.recordingRepository.recording(id)?.let {
                        container.syncManager.enqueueUpload(java.io.File(it.filePath))
                    }
                }
                onSaved(id)
            }
        }
    }

    fun setGainSlider(value: Int) {
        controller.setGainDb(com.gravo.grabadora.audio.RecordingSpec.sliderToDb(value))
        viewModelScope.launch { settingsRepo.setGainSlider(value) }
    }

    fun setDepth(depth: BitDepth) = viewModelScope.launch { settingsRepo.setDepth(depth) }
    fun setFormat(format: RecordFormat) = viewModelScope.launch { settingsRepo.setFormat(format) }
    fun toggleTheme() = viewModelScope.launch { settingsRepo.setDarkTheme(!settings.first().darkTheme) }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(container) as T
        }
    }
}
