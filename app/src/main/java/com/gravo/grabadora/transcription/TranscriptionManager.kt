package com.gravo.grabadora.transcription

import com.gravo.grabadora.audio.RecordFormat
import com.gravo.grabadora.data.db.TranscriptDao
import com.gravo.grabadora.data.db.TranscriptEntity
import com.gravo.grabadora.data.settings.AppSettings
import java.io.File

/**
 * Orquesta la transcripción de una grabación: resuelve ajustes, clave y proveedor,
 * adapta el audio, transcribe (posiblemente por trozos) y persiste el resultado.
 *
 * JVM puro: no importa clases de Android para poder probarse en los tests JVM.
 * Las dependencias que tocan Android (DataStore, Keystore, OkHttp, Room, códec)
 * se inyectan como lambdas o interfaces estrechas desde [com.gravo.grabadora.AppContainer].
 */
class TranscriptionManager(
    private val settingsProvider: suspend () -> AppSettings,
    private val keyProvider: suspend (TranscriptionProviderId) -> String,
    private val keySaver: suspend (TranscriptionProviderId, String) -> Unit,
    private val providers: Map<TranscriptionProviderId, TranscriptionProvider>,
    private val recordingProvider: suspend (Long) -> RecordingInfo?,
    private val dao: TranscriptDao,
    private val transcode: (File, RecordFormat, AdaptationPlan, File) -> List<File>,
    private val cacheDir: File,
) {
    suspend fun transcribe(recordingId: Long, languageOverride: String?): TranscriptionResult {
        val settings = settingsProvider()
        val provider = settings.transcriptionProvider

        val recording = recordingProvider(recordingId)
            ?: throw TranscriptionException("Grabación no encontrada", TranscriptionErrorKind.PARSE)

        val apiKey = keyProvider(provider)
        if (apiKey.isBlank()) {
            throw TranscriptionException("Falta la clave de transcripción", TranscriptionErrorKind.AUTH)
        }

        val effectiveLanguage = languageOverride ?: settings.transcribeLanguage
        val language = if (provider.supportsLanguageHint) effectiveLanguage else ""

        val config = TranscriptionConfig(
            provider = provider,
            endpoint = endpointFor(settings, provider),
            model = modelFor(settings, provider),
            apiKey = apiKey,
            language = language,
        )

        val plan = AudioAdaptation.plan(
            File(recording.path),
            recording.format,
            provider,
            recording.durationMs,
        )
        val files = transcode(File(recording.path), recording.format, plan, cacheDir)

        val text = files.map { file ->
            providers.getValue(provider).transcribe(
                config,
                TranscriptionRequest(file, plan.mimeType),
            ).text
        }.filter { it.isNotBlank() }
            .joinToString("\n\n")

        val now = System.currentTimeMillis()
        dao.upsert(
            TranscriptEntity(
                recordingId = recordingId,
                text = text,
                provider = provider.name,
                model = config.model,
                language = effectiveLanguage,
                createdAt = now,
                updatedAt = now,
            ),
        )

        return TranscriptionResult(text, provider, effectiveLanguage)
    }

    suspend fun saveKey(provider: TranscriptionProviderId, plain: String) = keySaver(provider, plain)

    suspend fun hasKey(provider: TranscriptionProviderId): Boolean = keyProvider(provider).isNotBlank()

    suspend fun clearTranscript(recordingId: Long) = dao.delete(recordingId)

    private fun endpointFor(settings: AppSettings, provider: TranscriptionProviderId): String = when (provider) {
        TranscriptionProviderId.WHISPER -> settings.trWhisperEndpoint
        TranscriptionProviderId.GEMINI -> settings.trGeminiEndpoint
        TranscriptionProviderId.DEEPGRAM -> settings.trDeepgramEndpoint
    }

    private fun modelFor(settings: AppSettings, provider: TranscriptionProviderId): String = when (provider) {
        TranscriptionProviderId.WHISPER -> settings.trWhisperModel
        TranscriptionProviderId.GEMINI -> settings.trGeminiModel
        TranscriptionProviderId.DEEPGRAM -> settings.trDeepgramModel
    }
}
