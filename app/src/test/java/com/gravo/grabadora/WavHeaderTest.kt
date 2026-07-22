package com.gravo.grabadora

import com.gravo.grabadora.audio.encode.WavHeader
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

class WavHeaderTest {
    private fun u32(bytes: ByteArray, offset: Int): Long =
        (bytes[offset].toLong() and 0xFF) or ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 16) or ((bytes[offset + 3].toLong() and 0xFF) shl 24)

    private fun u16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    @Test
    fun `cabecera PCM 16-bit estereo 48k`() {
        val h = WavHeader.build(WavHeader.FORMAT_PCM, 48_000, 2, 16, 1000)
        assertEquals(44, h.size)
        assertEquals("RIFF", String(h, 0, 4))
        assertEquals("WAVE", String(h, 8, 4))
        assertEquals("fmt ", String(h, 12, 4))
        assertEquals(16, u32(h, 16).toInt())
        assertEquals(1, u16(h, 20)) // PCM
        assertEquals(2, u16(h, 22)) // canales
        assertEquals(48_000, u32(h, 24).toInt())
        assertEquals(48_000 * 4, u32(h, 28).toInt()) // byteRate
        assertEquals(4, u16(h, 32)) // blockAlign
        assertEquals(16, u16(h, 34))
        assertEquals("data", String(h, 36, 4))
        assertEquals(1000, u32(h, 40).toInt())
        assertEquals(36 + 1000, u32(h, 4).toInt()) // RIFF size
    }

    @Test
    fun `cabecera float32 lleva formato 3 y chunk fact`() {
        val h = WavHeader.build(WavHeader.FORMAT_FLOAT, 44_100, 1, 32, 400)
        assertEquals(58, h.size)
        assertEquals(3, u16(h, 20))
        assertEquals(18, u32(h, 16).toInt()) // fmt extendido
        assertEquals("fact", String(h, 38, 4))
        assertEquals(100, u32(h, 46).toInt()) // 400 bytes / 4 = 100 muestras
        assertEquals("data", String(h, 50, 4))
        assertEquals(400, u32(h, 54).toInt())
    }

    @Test
    fun `patchSizes actualiza RIFF, fact y data`() {
        val file = File.createTempFile("wav", ".wav")
        file.writeBytes(WavHeader.build(WavHeader.FORMAT_FLOAT, 48_000, 2, 32, 0) + ByteArray(800))
        RandomAccessFile(file, "rw").use {
            WavHeader.patchSizes(it, WavHeader.FORMAT_FLOAT, 2, 32, 800)
        }
        val bytes = file.readBytes()
        assertEquals(50 + 800, u32(bytes, 4).toInt())
        assertEquals(100, u32(bytes, 46).toInt()) // 800/8 frames
        assertEquals(800, u32(bytes, 54).toInt())
        file.delete()
    }
}
