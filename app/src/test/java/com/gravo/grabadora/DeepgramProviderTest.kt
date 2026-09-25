package com.gravo.grabadora

import com.gravo.grabadora.transcription.DeepgramTranscriptionProvider
import com.gravo.grabadora.transcription.TranscriptionConfig
import com.gravo.grabadora.transcription.TranscriptionErrorKind
import com.gravo.grabadora.transcription.TranscriptionException
import com.gravo.grabadora.transcription.TranscriptionProviderId
import com.gravo.grabadora.transcription.TranscriptionRequest
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File

class DeepgramProviderTest {

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
    fun `200 devuelve el texto del transcript`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"results":{"channels":[{"alternatives":[{"transcript":"hola mundo"}]}]}}"""
            )
        )

        val result = provider().transcribe(configFor(server), request())

        assertEquals("hola mundo", result.text)
        assertEquals(TranscriptionProviderId.DEEPGRAM, result.provider)
        assertEquals("", result.language)
    }

    @Test
    fun `401 mapea a AUTH`() = runTest {
        assertKind(401, """{"err_msg":"bad key"}""", TranscriptionErrorKind.AUTH)
    }

    @Test
    fun `429 mapea a RATE_LIMIT`() = runTest {
        assertKind(429, "too many requests", TranscriptionErrorKind.RATE_LIMIT)
    }

    @Test
    fun `la peticion usa listen con smart_format y detect_language en auto`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"results":{"channels":[{"alternatives":[{"transcript":"ok"}]}]}}"""
            )
        )

        provider().transcribe(configFor(server), request())

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/listen", recorded.requestUrl!!.encodedPath)
        assertEquals("Token k", recorded.getHeader("Authorization"))
        assertEquals("audio/mpeg", recorded.getHeader("Content-Type"))

        val url = recorded.requestUrl!!
        assertEquals("nova-3", url.queryParameter("model"))
        assertEquals("true", url.queryParameter("smart_format"))
        assertEquals("true", url.queryParameter("detect_language"))
        assertNull(url.queryParameter("language"))
    }

    @Test
    fun `language aparece en la consulta y quita detect_language`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"results":{"channels":[{"alternatives":[{"transcript":"ok"}]}]}}"""
            )
        )

        provider().transcribe(configFor(server, language = "es"), request())

        val url = server.takeRequest().requestUrl!!
        assertEquals("es", url.queryParameter("language"))
        assertNull(url.queryParameter("detect_language"))
    }

    private fun provider() = DeepgramTranscriptionProvider(OkHttpClient())

    private fun configFor(server: MockWebServer, language: String = ""): TranscriptionConfig =
        TranscriptionConfig(
            provider = TranscriptionProviderId.DEEPGRAM,
            endpoint = server.url("/").toString(),
            model = "nova-3",
            apiKey = "k",
            language = language,
        )

    private fun request(): TranscriptionRequest =
        TranscriptionRequest(audio = audioFile(), mimeType = "audio/mpeg")

    private fun audioFile(): File {
        val file = File.createTempFile("deepgram", ".mp3")
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
