package com.gravo.grabadora

import com.gravo.grabadora.transcription.TranscriptionErrorKind
import com.gravo.grabadora.transcription.mapHttpError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpErrorMappingTest {

    @Test
    fun `401 y 403 mapean a AUTH`() {
        assertEquals(TranscriptionErrorKind.AUTH, mapHttpError(401, "").kind)
        assertEquals(TranscriptionErrorKind.AUTH, mapHttpError(403, "forbidden").kind)
    }

    @Test
    fun `413 mapea a PAYLOAD_TOO_LARGE`() {
        assertEquals(TranscriptionErrorKind.PAYLOAD_TOO_LARGE, mapHttpError(413, "demasiado grande").kind)
    }

    @Test
    fun `400 415 y 422 mapean a UNSUPPORTED`() {
        assertEquals(TranscriptionErrorKind.UNSUPPORTED, mapHttpError(400, "").kind)
        assertEquals(TranscriptionErrorKind.UNSUPPORTED, mapHttpError(415, "").kind)
        assertEquals(TranscriptionErrorKind.UNSUPPORTED, mapHttpError(422, "").kind)
    }

    @Test
    fun `429 mapea a RATE_LIMIT`() {
        assertEquals(TranscriptionErrorKind.RATE_LIMIT, mapHttpError(429, "too many requests").kind)
    }

    @Test
    fun `errores 5xx mapean a SERVER`() {
        for (code in intArrayOf(500, 503, 599)) {
            assertEquals("code $code", TranscriptionErrorKind.SERVER, mapHttpError(code, "").kind)
        }
    }

    @Test
    fun `el mensaje incluye el codigo y un fragmento del cuerpo`() {
        val message = mapHttpError(401, "  {\"error\": \"bad key\"}  ").message
        assertTrue(message!!.contains("401"))
        assertTrue(message.contains("{\"error\": \"bad key\"}"))
    }

    @Test
    fun `el fragmento del cuerpo se recorta y no incluye espacios sobrantes`() {
        val message = mapHttpError(500, "   " + "x".repeat(500)).message
        assertTrue(message!!.contains("500"))
        assertTrue(message.length < 300)
    }

    @Test
    fun `codigo 200 no lanza excepcion`() {
        val result = mapHttpError(200, "ok")
        assertEquals(TranscriptionErrorKind.SERVER, result.kind)
    }
}
