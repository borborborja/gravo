package com.gravo.grabadora

import com.gravo.grabadora.audio.RecordFormat
import com.gravo.grabadora.data.db.TranscriptDao
import com.gravo.grabadora.data.db.TranscriptEntity
import com.gravo.grabadora.data.settings.AppSettings
import com.gravo.grabadora.transcription.AdaptationPlan
import com.gravo.grabadora.transcription.RecordingInfo
import com.gravo.grabadora.transcription.TranscriptionConfig
import com.gravo.grabadora.transcription.TranscriptionErrorKind
import com.gravo.grabadora.transcription.TranscriptionException
import com.gravo.grabadora.transcription.TranscriptionManager
import com.gravo.grabadora.transcription.TranscriptionProvider
import com.gravo.grabadora.transcription.TranscriptionProviderId
import com.gravo.grabadora.transcription.TranscriptionRequest
import com.gravo.grabadora.transcription.TranscriptionResult
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptionManagerTest {

    private class FakeProvider(
        override val id: TranscriptionProviderId = TranscriptionProviderId.WHISPER,
        private val onTranscribe: suspend (TranscriptionRequest) -> TranscriptionResult,
    ) : TranscriptionProvider {
        val requests = mutableListOf<TranscriptionRequest>()
        override suspend fun transcribe(
            config: TranscriptionConfig,
            request: TranscriptionRequest,
        ): TranscriptionResult {
            requests.add(request)
            return onTranscribe(request)
        }
    }

    private class FakeTranscriptDao : TranscriptDao {
        val upserted = mutableListOf<TranscriptEntity>()
        override fun observeById(recordingId: Long): Flow<TranscriptEntity?> = emptyFlow()
        override suspend fun upsert(t: TranscriptEntity) { upserted.add(t) }
        override suspend fun delete(id: Long) { upserted.removeAll { it.recordingId == id } }
    }

    private fun manager(
        dao: TranscriptDao,
        provider: TranscriptionProvider,
        transcode: (File, RecordFormat, AdaptationPlan, File) -> List<File>,
        cacheDir: File,
        settings: AppSettings = AppSettings(),
    ) = TranscriptionManager(
        settingsProvider = { settings },
        keyProvider = { "clave-fake" },
        keySaver = { _, _ -> },
        providers = mapOf(provider.id to provider),
        recordingProvider = { id ->
            RecordingInfo(File(cacheDir, "rec_$id.wav").absolutePath, RecordFormat.WAV, 60_000)
        },
        dao = dao,
        transcode = transcode,
        cacheDir = cacheDir,
    )

    @Test
    fun `concatena los trozos con doble salto y hace un unico upsert`() = runTest {
        val cacheDir = tempDir()
        val dao = FakeTranscriptDao()
        var calls = 0
        val provider = FakeProvider { _ ->
            calls++
            TranscriptionResult("fragmento $calls", TranscriptionProviderId.WHISPER, "es")
        }
        val transcode: (File, RecordFormat, AdaptationPlan, File) -> List<File> = { _, _, _, _ ->
            listOf(File(cacheDir, "part_0.mp3"), File(cacheDir, "part_1.mp3"))
        }

        val result = manager(dao, provider, transcode, cacheDir).transcribe(42L, null)

        assertEquals("fragmento 1\n\nfragmento 2", result.text)
        assertEquals(1, dao.upserted.size)
        assertEquals("fragmento 1\n\nfragmento 2", dao.upserted.single().text)
        assertEquals(42L, dao.upserted.single().recordingId)
        assertEquals(2, provider.requests.size)
    }

    @Test
    fun `propaga el tipo de error del proveedor sin persistir`() = runTest {
        val cacheDir = tempDir()
        val dao = FakeTranscriptDao()
        val provider = FakeProvider {
            throw TranscriptionException("demasiadas peticiones", TranscriptionErrorKind.RATE_LIMIT)
        }
        val transcode: (File, RecordFormat, AdaptationPlan, File) -> List<File> = { _, _, _, _ ->
            listOf(File(cacheDir, "part_0.mp3"))
        }

        val thrown = try {
            manager(dao, provider, transcode, cacheDir).transcribe(42L, null)
            null
        } catch (e: TranscriptionException) {
            e
        }

        assertTrue("se esperaba una TranscriptionException", thrown != null)
        assertEquals(TranscriptionErrorKind.RATE_LIMIT, thrown?.kind)
        assertTrue(dao.upserted.isEmpty())
    }

    private fun tempDir(): File =
        File.createTempFile("transcription-test", "").apply { delete() }.apply { mkdirs() }
}
