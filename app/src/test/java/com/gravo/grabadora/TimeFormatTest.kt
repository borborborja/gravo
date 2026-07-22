package com.gravo.grabadora

import com.gravo.grabadora.util.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    @Test
    fun `mmss basico`() {
        assertEquals("00:00", TimeFormat.mmss(0L))
        assertEquals("00:47", TimeFormat.mmss(47L))
        assertEquals("12:48", TimeFormat.mmss(768L))
        assertEquals("75:03", TimeFormat.mmss(4503L)) // más de una hora sigue en minutos
    }

    @Test
    fun `mmss no negativo`() {
        assertEquals("00:00", TimeFormat.mmss(-5L))
    }

    @Test
    fun `fine con decimas y coma`() {
        assertEquals("00:00,0", TimeFormat.fine(0))
        assertEquals("00:01,5", TimeFormat.fine(1_500))
        assertEquals("02:14,9", TimeFormat.fine(134_900))
    }

    @Test
    fun `etiquetas de seek con signo unicode`() {
        assertEquals("−5s", TimeFormat.seekLabel(-5))
        assertEquals("+10s", TimeFormat.seekLabel(10))
    }

    @Test
    fun `velocidad con coma decimal`() {
        assertEquals("1×", TimeFormat.speed(1f))
        assertEquals("0,5×", TimeFormat.speed(0.5f))
        assertEquals("1,25×", TimeFormat.speed(1.25f))
        assertEquals("2×", TimeFormat.speed(2f))
    }
}
