package com.gravo.grabadora

import com.gravo.grabadora.audio.RecordFormat
import com.gravo.grabadora.transcription.AdaptationMode
import com.gravo.grabadora.transcription.AudioAdaptation
import com.gravo.grabadora.transcription.TranscriptionProviderId
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioAdaptationTest {

    @Test
    fun `mp3 pequeno con whisper se entrega original`() {
        val plan = AudioAdaptation.plan(
            File("audio.mp3"),
            RecordFormat.MP3,
            TranscriptionProviderId.WHISPER,
            durationMs = 60_000,
        )
        assertEquals(AdaptationMode.ORIGINAL, plan.mode)
        assertEquals("audio/mpeg", plan.mimeType)
        assertTrue(plan.chunks.isEmpty())
    }

    @Test
    fun `flac con whisper se transcodifica`() {
        val plan = AudioAdaptation.plan(
            File("audio.flac"),
            RecordFormat.FLAC,
            TranscriptionProviderId.WHISPER,
            durationMs = 60_000,
        )
        assertEquals(AdaptationMode.TRANSCODE, plan.mode)
        assertEquals("audio/mpeg", plan.mimeType)
        assertTrue(plan.chunks.isEmpty())
    }

    @Test
    fun `duracion larga con whisper se trocea en varios chunks`() {
        val plan = AudioAdaptation.plan(
            File("audio.flac"),
            RecordFormat.FLAC,
            TranscriptionProviderId.WHISPER,
            durationMs = 100 * 60_000L,
        )
        assertEquals(AdaptationMode.TRANSCODE, plan.mode)
        assertTrue("esperaba más de un chunk, obtuve ${plan.chunks.size}", plan.chunks.size > 1)
        assertEquals(100 * 60_000L, plan.chunks.last().last)
    }

    @Test
    fun `flac con deepgram se entrega original`() {
        val plan = AudioAdaptation.plan(
            File("audio.flac"),
            RecordFormat.FLAC,
            TranscriptionProviderId.DEEPGRAM,
            durationMs = 60_000,
        )
        assertEquals(AdaptationMode.ORIGINAL, plan.mode)
        assertEquals("audio/flac", plan.mimeType)
    }

    @Test
    fun `mime por extension`() {
        assertEquals("audio/wav", AudioAdaptation.mimeTypeFor("wav"))
        assertEquals("audio/mpeg", AudioAdaptation.mimeTypeFor("mp3"))
        assertEquals("audio/mp4", AudioAdaptation.mimeTypeFor("m4a"))
        assertEquals("audio/ogg", AudioAdaptation.mimeTypeFor("ogg"))
        assertEquals("audio/flac", AudioAdaptation.mimeTypeFor("flac"))
        assertEquals("audio/aac", AudioAdaptation.mimeTypeFor("aac"))
        assertEquals("audio/webm", AudioAdaptation.mimeTypeFor("webm"))
    }
}
