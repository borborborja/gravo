package com.gravo.grabadora

import com.gravo.grabadora.data.settings.AppSettings
import com.gravo.grabadora.transcription.TranscriptionProviderId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TranscriptionSettingsDefaultsTest {

    @Test
    fun `proveedor de transcripcion por defecto es whisper`() {
        assertEquals(TranscriptionProviderId.WHISPER, AppSettings().transcriptionProvider)
    }

    @Test
    fun `idioma de transcripcion por defecto es auto`() {
        assertEquals("", AppSettings().transcribeLanguage)
    }

    @Test
    fun `autoTranscribe por defecto es falso`() {
        assertFalse(AppSettings().autoTranscribe)
    }

    @Test
    fun `endpoints por defecto coinciden con los del proveedor`() {
        val s = AppSettings()
        assertEquals(TranscriptionProviderId.WHISPER.defaultEndpoint, s.trWhisperEndpoint)
        assertEquals(TranscriptionProviderId.GEMINI.defaultEndpoint, s.trGeminiEndpoint)
        assertEquals(TranscriptionProviderId.DEEPGRAM.defaultEndpoint, s.trDeepgramEndpoint)
    }

    @Test
    fun `modelos por defecto coinciden con los del proveedor`() {
        val s = AppSettings()
        assertEquals(TranscriptionProviderId.WHISPER.defaultModel, s.trWhisperModel)
        assertEquals(TranscriptionProviderId.GEMINI.defaultModel, s.trGeminiModel)
        assertEquals(TranscriptionProviderId.DEEPGRAM.defaultModel, s.trDeepgramModel)
    }
}
