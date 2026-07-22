package com.gravo.grabadora

import com.gravo.grabadora.edit.PcmOps
import com.gravo.grabadora.edit.PcmSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.abs

/** PcmSource en memoria para tests. */
private class MemorySource(private val data: FloatArray, override val channels: Int = 1) : PcmSource {
    override val frames: Long = (data.size / channels).toLong()
    override fun read(frameStart: Long, frameCount: Int, out: FloatArray): Int {
        val start = frameStart.toInt() * channels
        val count = minOf(frameCount * channels, data.size - start)
        System.arraycopy(data, start, out, 0, count)
        return count / channels
    }
}

private fun render(source: PcmSource, kept: List<LongRange>, sampleRate: Int = 1000, gain: Float = 1f, fadeIn: Long = 0, fadeOut: Long = 0): FloatArray {
    val out = mutableListOf<Float>()
    PcmOps.render(source, kept, sampleRate, gain, fadeIn, fadeOut, blockFrames = 7) { buf, n ->
        for (i in 0 until n) out.add(buf[i])
    }
    return out.toFloatArray()
}

class PcmOpsTest {
    @Test
    fun `keptRanges solo trim`() {
        val kept = PcmOps.keptRanges(100, 10, 90, emptyList())
        assertEquals(listOf(10L until 90L), kept)
        assertEquals(80, PcmOps.outputFrames(kept))
    }

    @Test
    fun `keptRanges con corte central`() {
        val kept = PcmOps.keptRanges(100, 0, 100, listOf(40L..59L))
        assertEquals(2, kept.size)
        assertEquals(0L..39L, LongRange(kept[0].first, kept[0].last))
        assertEquals(60L..99L, LongRange(kept[1].first, kept[1].last))
        assertEquals(80, PcmOps.outputFrames(kept))
    }

    @Test
    fun `keptRanges corte fuera del trim se ignora`() {
        val kept = PcmOps.keptRanges(100, 20, 80, listOf(0L..10L, 90L..99L))
        assertEquals(60, PcmOps.outputFrames(kept))
    }

    @Test
    fun `remapFrame tras un corte`() {
        val kept = PcmOps.keptRanges(100, 0, 100, listOf(40L..59L))
        assertEquals(0L, PcmOps.remapFrame(0, kept))
        assertEquals(39L, PcmOps.remapFrame(39, kept))
        assertNull(PcmOps.remapFrame(50, kept)) // dentro del corte
        assertEquals(40L, PcmOps.remapFrame(60, kept))
        assertEquals(79L, PcmOps.remapFrame(99, kept))
    }

    @Test
    fun `render aplica ganancia`() {
        val source = MemorySource(FloatArray(50) { 0.5f })
        val out = render(source, listOf(0L until 50L), gain = 2f)
        assertEquals(50, out.size)
        assertEquals(1f, out[25], 1e-6f)
    }

    @Test
    fun `normalize a -1dB con pico conocido`() {
        val source = MemorySource(floatArrayOf(0.1f, -0.45f, 0.2f))
        val kept = listOf(0L until 3L)
        val peak = PcmOps.scanPeak(source, kept, gain = 1f)
        assertEquals(0.45f, peak, 1e-6f)
        val factor = PcmOps.normalizeFactor(peak, targetDb = -1f)
        val out = render(source, kept, gain = factor)
        // el pico debe quedar en 10^(-1/20) ≈ 0.891
        assertEquals(0.891f, abs(out[1]), 0.001f)
    }

    @Test
    fun `fade in y out lineales`() {
        val source = MemorySource(FloatArray(100) { 1f })
        val out = render(source, listOf(0L until 100L), fadeIn = 10, fadeOut = 10)
        assertEquals(0f, out[0], 1e-6f) // arranque en silencio
        assertEquals(0.5f, out[5], 1e-6f)
        assertEquals(1f, out[50], 1e-6f) // centro intacto
        assertEquals(0f, out[99], 1e-6f) // final en silencio
    }

    @Test
    fun `empalme suave en el corte`() {
        // 1000 frames a 1000 Hz → splice de 5 frames (5 ms)
        val source = MemorySource(FloatArray(1000) { 1f })
        val kept = PcmOps.keptRanges(1000, 0, 1000, listOf(400L..599L))
        val out = render(source, kept)
        assertEquals(800, out.size)
        // final del primer segmento atenuado (fade-out del empalme)
        assertEquals(0f, out[399], 1e-6f)
        // inicio del segundo segmento atenuado (fade-in del empalme)
        assertEquals(0f, out[400], 1e-6f)
        assertEquals(1f, out[200], 1e-6f) // fuera del empalme intacto
        assertEquals(1f, out[700], 1e-6f)
    }

    @Test
    fun `gainFactor y dB`() {
        assertEquals(1f, PcmOps.gainFactor(0f), 1e-6f)
        assertEquals(2f, PcmOps.gainFactor(6.0206f), 1e-3f)
        assertEquals(0.5f, PcmOps.gainFactor(-6.0206f), 1e-3f)
    }
}
