package com.gravo.grabadora

import com.gravo.grabadora.audio.encode.PcmConvert
import org.junit.Assert.assertEquals
import org.junit.Test

class PcmConvertTest {
    @Test
    fun `int16 con redondeo y little-endian`() {
        val out = ByteArray(8)
        val n = PcmConvert.toInt16(floatArrayOf(0f, 1f, -1f, 0.5f), 4, out)
        assertEquals(8, n)
        assertEquals(0, out[0].toInt()); assertEquals(0, out[1].toInt())
        assertEquals(0xFF.toByte(), out[2]); assertEquals(0x7F.toByte(), out[3]) // 32767
        assertEquals(0x01.toByte(), out[4]); assertEquals(0x80.toByte(), out[5]) // -32767
    }

    @Test
    fun `clipping por encima de rango`() {
        val out = ByteArray(4)
        PcmConvert.toInt16(floatArrayOf(2f, -3f), 2, out)
        assertEquals(0xFF.toByte(), out[0]); assertEquals(0x7F.toByte(), out[1])
        assertEquals(0x01.toByte(), out[2]); assertEquals(0x80.toByte(), out[3])
    }

    @Test
    fun `int24 empaqueta 3 bytes little-endian`() {
        val out = ByteArray(6)
        val n = PcmConvert.toInt24(floatArrayOf(1f, 0f), 2, out)
        assertEquals(6, n)
        // 8388607 = 0x7FFFFF
        assertEquals(0xFF.toByte(), out[0]); assertEquals(0xFF.toByte(), out[1]); assertEquals(0x7F.toByte(), out[2])
        assertEquals(0, out[3].toInt()); assertEquals(0, out[4].toInt()); assertEquals(0, out[5].toInt())
    }

    @Test
    fun `float32 conserva los bits IEEE`() {
        val out = ByteArray(4)
        PcmConvert.toFloat32(floatArrayOf(1f), 1, out)
        // 1.0f = 0x3F800000 LE
        assertEquals(0x00.toByte(), out[0]); assertEquals(0x00.toByte(), out[1])
        assertEquals(0x80.toByte(), out[2]); assertEquals(0x3F.toByte(), out[3])
    }

    @Test
    fun `ida y vuelta int16 a float`() {
        val bytes = ByteArray(4)
        PcmConvert.toInt16(floatArrayOf(0.25f, -0.5f), 2, bytes)
        val floats = FloatArray(2)
        PcmConvert.int16ToFloat(bytes, 0, 4, floats)
        assertEquals(0.25f, floats[0], 0.001f)
        assertEquals(-0.5f, floats[1], 0.001f)
    }

    @Test
    fun `toInt32Samples escala al bits por muestra`() {
        val out = IntArray(2)
        PcmConvert.toInt32Samples(floatArrayOf(1f, -1f), 2, 24, out)
        assertEquals(8_388_607, out[0])
        assertEquals(-8_388_607, out[1])
    }
}
