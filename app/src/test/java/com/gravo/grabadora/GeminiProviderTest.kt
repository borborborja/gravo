package com.gravo.grabadora

import com.gravo.grabadora.transcription.GeminiTranscriptionProvider
import com.gravo.grabadora.transcription.TranscriptionConfig
import com.gravo.grabadora.transcription.TranscriptionErrorKind
import com.gravo.grabadora.transcription.TranscriptionException
import com.gravo.grabadora.transcription.TranscriptionProviderId
import com.gravo.grabadora.transcription.TranscriptionRequest
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class GeminiProviderTest {

    private lateinit var server: MockWebServer
    private lateinit var provider: GeminiTranscriptionProvider
    private lateinit var audio: File

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        provider = GeminiTranscriptionProvider(OkHttpClient())
        audio = File.createTempFile("gemini", ".wav").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }
    }

    @After
    fun tearDown() {
        server.shutdown()
        audio.delete()
    }

    private fun config() = TranscriptionConfig(
        provider = TranscriptionProviderId.GEMINI,
        endpoint = server.url("/").toString().trimEnd('/'),
        model = "gemini-3.5-transcribe",
        apiKey = "clave-de-prueba",
        language = "",
    )

    private fun request() = TranscriptionRequest(audio, "audio/wav")

    @Test
    fun `flujo completo devuelve el texto de outputs`() = runTest {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/upload/v1beta/files" -> MockResponse()
                    .setHeader("X-Goog-Upload-URL", server.url("/upload/session").toString())
                "/upload/session" -> MockResponse().setBody("""{"file":{"uri":"files/abc"}}""")
                "/v1beta/interactions" ->
                    MockResponse().setBody("""{"outputs":[{"type":"text","text":"hola"}]}""")
                else -> MockResponse().setResponseCode(404)
            }
        }

        val result = provider.transcribe(config(), request())
        assertEquals("hola", result.text)
    }

    @Test
    fun `respuesta sin outputs usa el fallback de candidates`() = runTest {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/upload/v1beta/files" -> MockResponse()
                    .setHeader("X-Goog-Upload-URL", server.url("/upload/session").toString())
                "/upload/session" -> MockResponse().setBody("""{"file":{"uri":"files/abc"}}""")
                "/v1beta/interactions" -> MockResponse().setBody(
                    """{"candidates":[{"content":{"parts":[{"text":"mundo"}]}}]}"""
                )
                else -> MockResponse().setResponseCode(404)
            }
        }

        val result = provider.transcribe(config(), request())
        assertEquals("mundo", result.text)
    }

    @Test
    fun `401 en la subida inicial mapea a AUTH`() = runTest {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                MockResponse().setResponseCode(401).setBody("unauthorized")
        }

        val error = runCatching { provider.transcribe(config(), request()) }.exceptionOrNull()
        assertTrue(error is TranscriptionException)
        assertEquals(TranscriptionErrorKind.AUTH, (error as TranscriptionException).kind)
    }

    @Test
    fun `la peticion de interactions lleva la cabecera x-goog-api-key`() = runTest {
        var interactionsRequest: RecordedRequest? = null
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/upload/v1beta/files" -> MockResponse()
                    .setHeader("X-Goog-Upload-URL", server.url("/upload/session").toString())
                "/upload/session" -> MockResponse().setBody("""{"file":{"uri":"files/abc"}}""")
                "/v1beta/interactions" -> {
                    interactionsRequest = request
                    MockResponse().setBody("""{"outputs":[{"type":"text","text":"hola"}]}""")
                }
                else -> MockResponse().setResponseCode(404)
            }
        }

        provider.transcribe(config(), request())

        val recorded = interactionsRequest
        assertTrue(recorded != null)
        assertEquals("clave-de-prueba", recorded!!.getHeader("x-goog-api-key"))
    }
}
