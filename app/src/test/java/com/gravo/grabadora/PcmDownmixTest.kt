package com.gravo.grabadora

import com.gravo.grabadora.audio.encode.PcmDownmix
import org.junit.Assert.assertEquals
import org.junit.Test

class PcmDownmixTest {
    @Test
    fun `estereo intercalado a mono promedia canales`() {
        val input = floatArrayOf(0.0f, 1.0f, 0.5f, 0.5f)
        val out = FloatArray(2)
        val frames = PcmDownmix.stereoToMono(input, input.size, out)
        assertEquals(2, frames)
        assertEquals(0.5f, out[0], 0.0001f)
        assertEquals(0.5f, out[1], 0.0001f)
    }

    @Test
    fun `devuelve el numero de frames mono`() {
        val input = FloatArray(6)
        for (i in input.indices) input[i] = i.toFloat()
        val out = FloatArray(3)
        val frames = PcmDownmix.stereoToMono(input, input.size, out)
        assertEquals(3, frames)
        // frame 1 = (2+3)/2, frame 2 = (4+5)/2
        assertEquals(2.5f, out[1], 0.0001f)
        assertEquals(4.5f, out[2], 0.0001f)
    }
}
