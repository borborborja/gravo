package com.gravo.grabadora.transcription

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private val JSON_MEDIA_TYPE = "application/json".toMediaType()

private const val MAX_FINALIZE_RETRIES = 1

/**
 * Proveedor de transcripción Gemini (Files API resumible + Interactions API).
 *
 * Paso 1: subida resumible del audio a `{endpoint}/upload/v1beta/files` con el
 * protocolo `X-Goog-Upload-*`; se obtiene una URL de sesión y se suben los bytes.
 * Paso 2: `{endpoint}/v1beta/interactions` con el modelo y la referencia al fichero.
 *
 * JVM puro: no importa clases de Android y se puede probar con MockWebServer.
 */
class GeminiTranscriptionProvider(
    private val client: OkHttpClient,
) : TranscriptionProvider {

    override val id: TranscriptionProviderId = TranscriptionProviderId.GEMINI

    override suspend fun transcribe(
        config: TranscriptionConfig,
        request: TranscriptionRequest,
    ): TranscriptionResult {
        val uploadUrl = client.newCall(buildGeminiUploadStart(config, request)).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw mapHttpError(resp.code, resp.body?.string().orEmpty())
            }
            resp.header("X-Goog-Upload-URL")?.trim().orEmpty().ifEmpty {
                throw TranscriptionException(
                    "Respuesta de subida sin X-Goog-Upload-URL",
                    TranscriptionErrorKind.PARSE,
                )
            }
        }

        val fileUri = finalizeUpload(uploadUrl, request)

        val text = client.newCall(buildGeminiInteraction(config, fileUri, request.mimeType))
            .execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw mapHttpError(resp.code, resp.body?.string().orEmpty())
                }
                parseGeminiResponse(resp.body?.string().orEmpty())
            }

        return TranscriptionResult(text = text, provider = id, language = config.language)
    }

    /**
     * Sube los bytes y finaliza la subida resumible. Un `308` indica que el servidor
     * aún no ha terminado: se relee `X-Goog-Upload-URL` y se reintenta el finalize una
     * vez (acotado por [MAX_FINALIZE_RETRIES]).
     */
    private fun finalizeUpload(initialUrl: String, request: TranscriptionRequest): String {
        var url = initialUrl
        var retries = 0
        while (true) {
            val result = client.newCall(buildGeminiUploadBytes(url, request)).execute().use { resp ->
                when {
                    resp.code == 308 -> {
                        val next = resp.header("X-Goog-Upload-URL")?.trim().orEmpty()
                        if (next.isNotEmpty() && retries < MAX_FINALIZE_RETRIES) {
                            url = next
                            retries++
                            null
                        } else {
                            throw mapHttpError(resp.code, resp.body?.string().orEmpty())
                        }
                    }
                    !resp.isSuccessful -> throw mapHttpError(resp.code, resp.body?.string().orEmpty())
                    else -> parseGeminiFileUri(resp.body?.string().orEmpty())
                }
            }
            if (result != null) return result
        }
    }
}

/** Construye la petición `start` de la subida resumible (sólo metadatos). */
fun buildGeminiUploadStart(config: TranscriptionConfig, request: TranscriptionRequest): Request {
    val endpoint = config.endpoint.trimEnd('/')
    val metadata = JSONObject()
        .put("file", JSONObject().put("display_name", request.audio.name))
        .toString()
    return Request.Builder()
        .url("$endpoint/upload/v1beta/files")
        .header("x-goog-api-key", config.apiKey)
        .header("X-Goog-Upload-Protocol", "resumable")
        .header("X-Goog-Upload-Command", "start")
        .header("X-Goog-Upload-Header-Content-Length", request.audio.length().toString())
        .header("X-Goog-Upload-Header-Content-Type", request.mimeType)
        .post(metadata.toRequestBody(JSON_MEDIA_TYPE))
        .build()
}

/** Construye la petición de subida de bytes sobre la URL de sesión devuelta por `start`. */
fun buildGeminiUploadBytes(uploadUrl: String, request: TranscriptionRequest): Request {
    return Request.Builder()
        .url(uploadUrl)
        .header("X-Goog-Upload-Offset", "0")
        .header("X-Goog-Upload-Command", "upload, finalize")
        .post(request.audio.asRequestBody(request.mimeType.toMediaType()))
        .build()
}

/** Extrae `file.uri` del cuerpo JSON de la respuesta de subida. */
fun parseGeminiFileUri(body: String): String {
    val uri = JSONObject(body).optJSONObject("file")?.optString("uri")?.trim().orEmpty()
    if (uri.isEmpty()) {
        throw TranscriptionException("Respuesta de subida sin file.uri", TranscriptionErrorKind.PARSE)
    }
    return uri
}

/** Construye la petición a `v1beta/interactions` referenciando el fichero ya subido. */
fun buildGeminiInteraction(config: TranscriptionConfig, fileUri: String, mimeType: String): Request {
    val endpoint = config.endpoint.trimEnd('/')
    val body = JSONObject()
        .put("model", config.model)
        .put(
            "input",
            JSONArray().put(
                JSONObject()
                    .put("type", "audio")
                    .put("uri", fileUri)
                    .put("mime_type", mimeType),
            ),
        )
        .toString()
    return Request.Builder()
        .url("$endpoint/v1beta/interactions")
        .header("x-goog-api-key", config.apiKey)
        .post(body.toRequestBody(JSON_MEDIA_TYPE))
        .build()
}

/**
 * Extrae el texto transcrito de la respuesta de `interactions`.
 *
 * Une todos los `outputs[].text` con `type == "text"`; si no hay ninguno, cae al
 * formato clásico `candidates[0].content.parts[].text`; en último término lanza
 * [TranscriptionException] con kind [TranscriptionErrorKind.PARSE].
 */
fun parseGeminiResponse(body: String): String {
    val json = JSONObject(body)

    val outputs = json.optJSONArray("outputs")
    if (outputs != null) {
        val text = buildString {
            for (i in 0 until outputs.length()) {
                val item = outputs.optJSONObject(i) ?: continue
                if (item.optString("type") == "text") append(item.optString("text"))
            }
        }
        if (text.isNotEmpty()) return text
    }

    val parts = json.optJSONArray("candidates")
        ?.optJSONObject(0)
        ?.optJSONObject("content")
        ?.optJSONArray("parts")
    if (parts != null) {
        val text = buildString {
            for (i in 0 until parts.length()) {
                append(parts.optJSONObject(i)?.optString("text").orEmpty())
            }
        }
        if (text.isNotEmpty()) return text
    }

    throw TranscriptionException("Respuesta sin texto transcrito", TranscriptionErrorKind.PARSE)
}
