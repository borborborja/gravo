package com.gravo.grabadora

import com.gravo.grabadora.transcription.TranscriptionProviderId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptionProviderDefaultsTest {

    @Test
    fun `existen tres proveedores de transcripcion`() {
        assertEquals(3, TranscriptionProviderId.entries.size)
    }

    @Test
    fun `whisper tiene el endpoint modelo y extensiones por defecto`() {
        val w = TranscriptionProviderId.WHISPER
        assertEquals("Whisper", w.label)
        assertEquals("https://api.openai.com/v1/audio/transcriptions", w.defaultEndpoint)
        assertEquals("whisper-1", w.defaultModel)
        assertEquals(setOf("mp3", "mp4", "mpeg", "mpga", "m4a", "wav", "webm"), w.acceptedExtensions)
    }

    @Test
    fun `gemini tiene el endpoint modelo y extensiones por defecto`() {
        val g = TranscriptionProviderId.GEMINI
        assertEquals("Gemini", g.label)
        assertEquals("https://generativelanguage.googleapis.com", g.defaultEndpoint)
        assertEquals("gemini-3.5-transcribe", g.defaultModel)
        assertEquals(
            setOf(
                "wav", "mp3", "aiff", "aac", "ogg", "flac", "mpeg",
                "m4a", "l16", "opus", "alaw", "mulaw", "webm"
            ),
            g.acceptedExtensions
        )
    }

    @Test
    fun `deepgram tiene el endpoint modelo y extensiones por defecto`() {
        val d = TranscriptionProviderId.DEEPGRAM
        assertEquals("Deepgram", d.label)
        assertEquals("https://api.deepgram.com", d.defaultEndpoint)
        assertEquals("nova-3", d.defaultModel)
        assertEquals(setOf("wav", "mp3", "m4a", "flac", "ogg"), d.acceptedExtensions)
    }

    @Test
    fun `solo gemini no admite pista de idioma`() {
        assertTrue(TranscriptionProviderId.WHISPER.supportsLanguageHint)
        assertFalse(TranscriptionProviderId.GEMINI.supportsLanguageHint)
        assertTrue(TranscriptionProviderId.DEEPGRAM.supportsLanguageHint)
    }

    @Test
    fun `whisper admite hasta 25 MiB`() {
        assertEquals(25L * 1024 * 1024, TranscriptionProviderId.WHISPER.maxBytes)
    }

    @Test
    fun `gemini admite hasta 2 GiB`() {
        assertEquals(2L * 1024 * 1024 * 1024, TranscriptionProviderId.GEMINI.maxBytes)
    }
}
