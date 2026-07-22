package com.gravo.grabadora

import com.gravo.grabadora.audio.BitDepth
import com.gravo.grabadora.audio.RecordFormat
import com.gravo.grabadora.audio.RecordingSpec
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingSpecTest {
    @Test
    fun `WAV admite todas las profundidades`() {
        for (depth in BitDepth.entries) {
            assertEquals(depth, RecordingSpec.effectiveDepth(RecordFormat.WAV, depth))
        }
    }

    @Test
    fun `FLAC degrada 32 a 24`() {
        assertEquals(BitDepth.B16, RecordingSpec.effectiveDepth(RecordFormat.FLAC, BitDepth.B16))
        assertEquals(BitDepth.B24, RecordingSpec.effectiveDepth(RecordFormat.FLAC, BitDepth.B24))
        assertEquals(BitDepth.B24, RecordingSpec.effectiveDepth(RecordFormat.FLAC, BitDepth.B32))
    }

    @Test
    fun `los formatos lossy son 16-bit efectivos`() {
        for (format in listOf(RecordFormat.MP3, RecordFormat.M4A, RecordFormat.OGG)) {
            for (depth in BitDepth.entries) {
                assertEquals(BitDepth.B16, RecordingSpec.effectiveDepth(format, depth))
            }
        }
    }

    @Test
    fun `slider de ganancia a dB y vuelta`() {
        assertEquals(0f, RecordingSpec.sliderToDb(50), 1e-6f)
        assertEquals(25f, RecordingSpec.sliderToDb(100), 1e-6f)
        assertEquals(-25f, RecordingSpec.sliderToDb(0), 1e-6f)
        assertEquals(50, RecordingSpec.dbToSlider(0f))
        assertEquals(100, RecordingSpec.dbToSlider(25f))
        assertEquals(0, RecordingSpec.dbToSlider(-25f))
    }
}
