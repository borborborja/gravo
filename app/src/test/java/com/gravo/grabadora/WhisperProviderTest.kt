package com.gravo.grabadora

import com.gravo.grabadora.transcription.TranscriptionConfig
import com.gravo.grabadora.transcription.TranscriptionErrorKind
import com.gravo.grabadora.transcription.TranscriptionException
import com.gravo.grabadora.transcription.TranscriptionProviderId
import com.gravo.grabadora.transcription.TranscriptionRequest
import com.gravo.grabadora.transcription.WhisperTranscriptionProvider
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File

class WhisperProviderTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `200 devuelve el texto de la respuesta`() = runTest {
        server.enqueue(MockResponse().setBody("""{"text":"hola mundo"}"""))

        val result = provider().transcribe(configFor(server), request())

        assertEquals("hola mundo", result.text)
        assertEquals(TranscriptionProviderId.WHISPER, result.provider)
        assertEquals("", result.language)
    }

    @Test
    fun `401 mapea a AUTH`() = runTest {
        assertKind(401, """{"error":{"message":"bad key"}}""", TranscriptionErrorKind.AUTH)
    }

    @Test
    fun `413 mapea a PAYLOAD_TOO_LARGE`() = runTest {
        assertKind(413, "payload too large", TranscriptionErrorKind.PAYLOAD_TOO_LARGE)
    }

    @Test
    fun `la peticion lleva path header y campos multipart`() = runTest {
        server.enqueue(MockResponse().setBody("""{"text":"ok"}"""))

        provider().transcribe(configFor(server), request())

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/audio/transcriptions", recorded.path)
        assertEquals("Bearer k", recorded.getHeader("Authorization"))

        val body = recorded.body.readUtf8()
        assertTrue(body.contains("name=\"file\""))
        assertTrue(body.contains("name=\"model\""))
        assertTrue(body.contains("whisper-1"))
        assertTrue(body.contains("name=\"response_format\""))
        assertTrue(body.contains("json"))
        assertFalse(body.contains("name=\"language\""))
    }

    @Test
    fun `language aparece solo cuando se indica`() = runTest {
        server.enqueue(MockResponse().setBody("""{"text":"ok"}"""))

        provider().transcribe(configFor(server, language = "es"), request())

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("name=\"language\""))
        assertTrue(body.contains("es"))
    }

    private fun provider() = WhisperTranscriptionProvider(OkHttpClient())

    private fun configFor(server: MockWebServer, language: String = ""): TranscriptionConfig =
        TranscriptionConfig(
            provider = TranscriptionProviderId.WHISPER,
            endpoint = server.url("/v1/audio/transcriptions").toString(),
            model = "whisper-1",
            apiKey = "k",
            language = language,
        )

    private fun request(): TranscriptionRequest =
        TranscriptionRequest(audio = audioFile(), mimeType = "audio/mpeg")

    private fun audioFile(): File {
        val file = File.createTempFile("whisper", ".mp3")
        file.writeBytes(byteArrayOf(1, 2, 3, 4))
        file.deleteOnExit()
        return file
    }

    private suspend fun assertKind(code: Int, body: String, expected: TranscriptionErrorKind) {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))
        try {
            provider().transcribe(configFor(server), request())
            fail("debería lanzar TranscriptionException")
        } catch (e: TranscriptionException) {
            assertEquals(expected, e.kind)
        }
    }
}
