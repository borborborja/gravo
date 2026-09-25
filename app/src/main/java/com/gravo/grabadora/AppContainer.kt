package com.gravo.grabadora

import android.app.Application
import com.gravo.grabadora.audio.RecordFormat
import com.gravo.grabadora.audio.RecordingController
import com.gravo.grabadora.data.RecordingRepository
import com.gravo.grabadora.data.db.AppDatabase
import com.gravo.grabadora.data.settings.SettingsRepository
import com.gravo.grabadora.sync.CryptoManager
import com.gravo.grabadora.sync.SyncManager
import com.gravo.grabadora.transcription.AudioTranscoder
import com.gravo.grabadora.transcription.DeepgramTranscriptionProvider
import com.gravo.grabadora.transcription.GeminiTranscriptionProvider
import com.gravo.grabadora.transcription.RecordingInfo
import com.gravo.grabadora.transcription.TranscriptionManager
import com.gravo.grabadora.transcription.TranscriptionProviderId
import com.gravo.grabadora.transcription.TranscriptionScheduler
import com.gravo.grabadora.transcription.WhisperTranscriptionProvider
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient

/** Inyección manual: raíz de dependencias de la app. */
class AppContainer(val app: Application) {
    val database: AppDatabase by lazy { AppDatabase.build(app) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(app) }
    val recordingRepository: RecordingRepository by lazy { RecordingRepository(app, database) }
    val recordingController: RecordingController by lazy { RecordingController(app, recordingRepository) }
    val cryptoManager: CryptoManager by lazy { CryptoManager(app) }
    val syncManager: SyncManager by lazy { SyncManager(app, settingsRepository, cryptoManager) }
    val transcriptionScheduler: TranscriptionScheduler by lazy { TranscriptionScheduler(app) }
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .build()
    }
    val transcriptionManager: TranscriptionManager by lazy {
        TranscriptionManager(
            settingsProvider = { settingsRepository.settings.first() },
            keyProvider = { p -> cryptoManager.decrypt(settingsRepository.transcriptionKeyEnc(p).first()) },
            keySaver = { p, plain ->
                settingsRepository.setTranscriptionKeyEnc(p, if (plain.isEmpty()) "" else cryptoManager.encrypt(plain))
            },
            providers = mapOf(
                TranscriptionProviderId.WHISPER to WhisperTranscriptionProvider(okHttpClient),
                TranscriptionProviderId.GEMINI to GeminiTranscriptionProvider(okHttpClient),
                TranscriptionProviderId.DEEPGRAM to DeepgramTranscriptionProvider(okHttpClient),
            ),
            recordingProvider = { id ->
                recordingRepository.recording(id)?.let {
                    RecordingInfo(it.filePath, RecordFormat.valueOf(it.format), it.durationMs)
                }
            },
            dao = database.transcriptDao(),
            transcode = { f, fmt, plan, out -> AudioTranscoder().transcode(f, fmt, plan, out) },
            cacheDir = File(app.cacheDir, "transcription"),
        )
    }
}
