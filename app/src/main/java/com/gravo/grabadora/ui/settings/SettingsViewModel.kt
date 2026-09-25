package com.gravo.grabadora.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gravo.grabadora.AppContainer
import com.gravo.grabadora.audio.BitDepth
import com.gravo.grabadora.audio.MicOption
import com.gravo.grabadora.audio.MicSelector
import com.gravo.grabadora.audio.RecordFormat
import com.gravo.grabadora.data.settings.AppSettings
import com.gravo.grabadora.data.settings.SyncProtocol
import com.gravo.grabadora.transcription.TranscriptionProviderId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class SyncTestState {
    data object Idle : SyncTestState()
    data object Testing : SyncTestState()
    data object Ok : SyncTestState()
    data class Error(val message: String) : SyncTestState()
}

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.settingsRepository

    val settings: StateFlow<AppSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val mics: List<MicOption> by lazy { MicSelector.availableMics(container.app) }

    val syncTest = MutableStateFlow<SyncTestState>(SyncTestState.Idle)
    val hasPassword = MutableStateFlow(false)
    val hasTranscriptionKey = MutableStateFlow<Set<TranscriptionProviderId>>(emptySet())

    init {
        viewModelScope.launch { hasPassword.value = repo.syncPasswordEnc.first().isNotEmpty() }
        viewModelScope.launch {
            hasTranscriptionKey.value = TranscriptionProviderId.entries
                .filter { repo.transcriptionKeyEnc(it).first().isNotEmpty() }
                .toSet()
        }
    }

    fun setFormat(v: RecordFormat) = launchSet { repo.setFormat(v) }
    fun setDepth(v: BitDepth) = launchSet { repo.setDepth(v) }
    fun setGainSlider(v: Int) = launchSet {
        repo.setGainSlider(v)
        container.recordingController.setGainDb(com.gravo.grabadora.audio.RecordingSpec.sliderToDb(v))
    }
    fun setMicId(v: Int) = launchSet { repo.setMicId(v) }
    fun setStereo(v: Boolean) = launchSet { repo.setStereo(v) }
    fun setDarkTheme(v: Boolean) = launchSet { repo.setDarkTheme(v) }
    fun setHideNotification(v: Boolean) = launchSet { repo.setHideNotification(v) }
    fun setAutoStart(v: Boolean) = launchSet { repo.setAutoStart(v) }
    fun setSyncProtocol(v: SyncProtocol) = launchSet { repo.setSyncProtocol(v) }
    fun setSyncServer(v: String) = launchSet { repo.setSyncServer(v) }
    fun setSyncUser(v: String) = launchSet { repo.setSyncUser(v) }
    fun setSyncFolder(v: String) = launchSet { repo.setSyncFolder(v) }
    fun setAutoUpload(v: Boolean) = launchSet { repo.setAutoUpload(v) }
    fun setTranscriptionProvider(v: TranscriptionProviderId) = launchSet { repo.setTranscriptionProvider(v) }
    fun setTranscriptionEndpoint(provider: TranscriptionProviderId, v: String) =
        launchSet { repo.setTranscriptionEndpoint(provider, v) }
    fun setTranscriptionModel(provider: TranscriptionProviderId, v: String) =
        launchSet { repo.setTranscriptionModel(provider, v) }
    fun setTranscribeLanguage(v: String) = launchSet { repo.setTranscribeLanguage(v) }
    fun setAutoTranscribe(v: Boolean) = launchSet { repo.setAutoTranscribe(v) }

    fun setTranscriptionKey(provider: TranscriptionProviderId, plain: String) {
        viewModelScope.launch {
            val blob = if (plain.isEmpty()) "" else container.cryptoManager.encrypt(plain)
            repo.setTranscriptionKeyEnc(provider, blob)
            hasTranscriptionKey.value = if (plain.isEmpty()) {
                hasTranscriptionKey.value - provider
            } else {
                hasTranscriptionKey.value + provider
            }
        }
    }

    fun setLanguage(lang: String) {
        launchSet { repo.setLanguage(lang) }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
    }

    fun setSyncPassword(plain: String) {
        viewModelScope.launch {
            container.syncManager.savePassword(plain)
            hasPassword.value = plain.isNotEmpty()
        }
    }

    fun testConnection() {
        syncTest.value = SyncTestState.Testing
        viewModelScope.launch {
            try {
                container.syncManager.testConnection()
                syncTest.value = SyncTestState.Ok
            } catch (e: Exception) {
                syncTest.value = SyncTestState.Error(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    private fun launchSet(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(container) as T
        }
    }
}
