package com.gravo.grabadora

import com.gravo.grabadora.util.RelativeDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RelativeDateTest {
    private fun format(ts: Long, now: Long) =
        RelativeDate.format(ts, now, "Hoy %s", "Ayer %s", "Ahora mismo")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun `hace menos de un minuto`() {
        val now = at(2026, 6, 22, 12, 0)
        assertEquals("Ahora mismo", format(now - 30_000, now))
    }

    @Test
    fun `mismo dia`() {
        val now = at(2026, 6, 22, 12, 0)
        assertEquals("Hoy 09:12", format(at(2026, 6, 22, 9, 12), now))
    }

    @Test
    fun `ayer`() {
        val now = at(2026, 6, 22, 12, 0)
        assertEquals("Ayer 21:40", format(at(2026, 6, 21, 21, 40), now))
    }

    @Test
    fun `dias anteriores del mismo ano`() {
        val now = at(2026, 6, 22, 12, 0)
        val result = format(at(2026, 6, 20, 10, 0), now)
        assertTrue(result, result.startsWith("20 jul"))
    }

    @Test
    fun `otro ano incluye el ano`() {
        val now = at(2026, 6, 22, 12, 0)
        val result = format(at(2025, 11, 31, 10, 0), now)
        assertTrue(result, result.contains("2025"))
    }
}
