package com.gravo.grabadora

import com.gravo.grabadora.ui.transcription.TranscriptionSearch
import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptionSearchTest {

    @Test
    fun `conteo exacto`() {
        assertEquals(2, TranscriptionSearch.count("hola hola", "hola"))
    }

    @Test
    fun `conteo insensible a mayusculas`() {
        assertEquals(1, TranscriptionSearch.count("Hola Mundo", "HOLA"))
    }

    @Test
    fun `conteo insensible a acentos`() {
        assertEquals(1, TranscriptionSearch.count("música", "musica"))
    }

    @Test
    fun `query en blanco devuelve cero`() {
        assertEquals(0, TranscriptionSearch.count("hola", ""))
        assertEquals(0, TranscriptionSearch.count("hola", "   "))
    }

    @Test
    fun `rangos no solapados y correctos`() {
        assertEquals(listOf(0..3, 5..8), TranscriptionSearch.ranges("hola hola", "hola"))
        assertEquals(listOf(0..1, 2..3), TranscriptionSearch.ranges("aaaa", "aa"))
    }

    @Test
    fun `rangos insensibles a acentos`() {
        assertEquals(listOf(0..5), TranscriptionSearch.ranges("música", "musica"))
    }
}
