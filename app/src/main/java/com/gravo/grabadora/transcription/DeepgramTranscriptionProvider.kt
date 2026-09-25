package com.gravo.grabadora.transcription

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * Proveedor de transcripción Deepgram (audio pre-grabado, `POST /v1/listen`).
 *
 * La construcción de la petición y el parseo de la respuesta son funciones puras
 * (JVM), de modo que puedan probarse con MockWebServer sin tocar el framework Android.
 */
class DeepgramTranscriptionProvider(
    private val client: OkHttpClient,
) : TranscriptionProvider {

    override val id: TranscriptionProviderId = TranscriptionProviderId.DEEPGRAM

    override suspend fun transcribe(
        config: TranscriptionConfig,
        request: TranscriptionRequest,
    ): TranscriptionResult {
        val req = buildDeepgramRequest(config, request)
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw mapHttpError(resp.code, body)
            return parseDeepgramResponse(body)
        }
    }
}

/**
 * Construye la petición HTTP POST a Deepgram `/v1/listen`.
 *
 * La URL lleva `model` y `smart_format=true`. El idioma se pasa como `language=<lang>`
 * cuando [TranscriptionConfig.language] no está en blanco; si está vacío se añade
 * `detect_language=true` (detección automática). El cuerpo es el audio en crudo con su
 * MIME y la cabecera de autorización usa el esquema `Token`.
 */
fun buildDeepgramRequest(
    config: TranscriptionConfig,
    request: TranscriptionRequest,
): Request {
    val base = config.endpoint.trim().trimEnd('/')
    val url = "$base/v1/listen".toHttpUrlOrNull()?.newBuilder()
        ?.addQueryParameter("model", config.model)
        ?.addQueryParameter("smart_format", "true")
        ?.apply {
            if (config.language.isNotBlank()) {
                addQueryParameter("language", config.language)
            } else {
                addQueryParameter("detect_language", "true")
            }
        }
        ?.build()
        ?: throw IOException("URL no válida: ${config.endpoint}")

    return Request.Builder()
        .url(url)
        .header("Authorization", "Token ${config.apiKey}")
        .header("Content-Type", request.mimeType)
        .post(request.audio.asRequestBody(request.mimeType.toMediaTypeOrNull()))
        .build()
}

/**
 * Parsea la respuesta JSON de Deepgram y extrae
 * `results.channels[0].alternatives[0].transcript`.
 */
fun parseDeepgramResponse(body: String): TranscriptionResult {
    val json = JSONObject(body)
    val transcript = json
        .getJSONObject("results")
        .getJSONArray("channels")
        .getJSONObject(0)
        .getJSONArray("alternatives")
        .getJSONObject(0)
        .getString("transcript")
    return TranscriptionResult(transcript, TranscriptionProviderId.DEEPGRAM, "")
}
