package com.gravo.grabadora

import com.gravo.grabadora.audio.LevelMeter
import org.junit.Assert.assertEquals
import org.junit.Test

class LevelMeterTest {
    @Test
    fun `amplitud a dBFS`() {
        assertEquals(0f, LevelMeter.toDb(1f), 0.01f)
        assertEquals(-6.02f, LevelMeter.toDb(0.5f), 0.1f)
        assertEquals(-20f, LevelMeter.toDb(0.1f), 0.1f)
        assertEquals(LevelMeter.SILENCE_DB, LevelMeter.toDb(0f), 0.01f)
        assertEquals(LevelMeter.SILENCE_DB, LevelMeter.toDb(0.0005f), 0.01f)
    }

    @Test
    fun `el pico del bloque se publica`() {
        val meter = LevelMeter()
        meter.configure(48_000, 1)
        meter.process(floatArrayOf(0.1f, -0.7f, 0.3f), 3)
        assertEquals(LevelMeter.toDb(0.7f), meter.peakDb.value, 0.01f)
    }

    @Test
    fun `las barras se desplazan al llenar la ventana`() {
        val meter = LevelMeter(barCount = 4)
        meter.configure(10, 1) // 1 barra por muestra
        meter.process(floatArrayOf(0.5f), 1)
        meter.process(floatArrayOf(0.9f), 1)
        val levels = meter.levels.value
        assertEquals(0.5f, levels[2], 1e-6f)
        assertEquals(0.9f, levels[3], 1e-6f)
    }
}
