package com.gravo.grabadora

import com.gravo.grabadora.transcription.AutoTranscribeDecider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoTranscribeDeciderTest {

    @Test
    fun `encola solo con auto activo y clave presente`() {
        assertTrue(AutoTranscribeDecider.shouldEnqueue(autoTranscribe = true, hasKey = true))
    }

    @Test
    fun `sin auto no encola aunque haya clave`() {
        assertFalse(AutoTranscribeDecider.shouldEnqueue(autoTranscribe = false, hasKey = true))
    }

    @Test
    fun `sin clave no encola aunque haya auto`() {
        assertFalse(AutoTranscribeDecider.shouldEnqueue(autoTranscribe = true, hasKey = false))
    }

    @Test
    fun `sin auto y sin clave no encola`() {
        assertFalse(AutoTranscribeDecider.shouldEnqueue(autoTranscribe = false, hasKey = false))
    }
}
