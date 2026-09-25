package com.gravo.grabadora.transcription

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject

/**
 * Proveedor de transcripción compatible con la API de Whisper / OpenAI
 * (`POST /audio/transcriptions`).
 *
 * La construcción de la petición y el parseo de la respuesta son funciones puras
 * (JVM), de modo que puedan probarse con MockWebServer sin tocar el framework Android.
 */
class WhisperTranscriptionProvider(
    private val client: OkHttpClient,
) : TranscriptionProvider {

    override val id: TranscriptionProviderId = TranscriptionProviderId.WHISPER

    override suspend fun transcribe(
        config: TranscriptionConfig,
        request: TranscriptionRequest,
    ): TranscriptionResult {
        val req = buildWhisperRequest(config, request)
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw mapHttpError(resp.code, body)
            return parseWhisperResponse(body)
        }
    }
}

/**
 * Construye la petición HTTP multipart a un endpoint compatible con Whisper.
 *
 * El cuerpo es `multipart/form-data` con la parte `file` (el audio, con su MIME),
 * `model` y `response_format=json`. La parte `language` sólo se añade si
 * [TranscriptionConfig.language] no está en blanco (cadena vacía = detección automática).
 */
fun buildWhisperRequest(
    config: TranscriptionConfig,
    request: TranscriptionRequest,
): Request {
    val body = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart(
            "file",
            request.audio.name,
            request.audio.asRequestBody(request.mimeType.toMediaTypeOrNull()),
        )
        .addFormDataPart("model", config.model)
        .addFormDataPart("response_format", "json")
        .apply {
            if (config.language.isNotBlank()) {
                addFormDataPart("language", config.language)
            }
        }
        .build()

    return Request.Builder()
        .url(config.endpoint)
        .post(body)
        .header("Authorization", "Bearer ${config.apiKey}")
        .build()
}

/**
 * Parsea la respuesta JSON de Whisper y extrae el campo `text`.
 */
fun parseWhisperResponse(body: String): TranscriptionResult {
    val json = JSONObject(body)
    val text = json.getString("text")
    return TranscriptionResult(text, TranscriptionProviderId.WHISPER, "")
}
