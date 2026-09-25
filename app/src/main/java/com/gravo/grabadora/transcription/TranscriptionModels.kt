package com.gravo.grabadora.transcription

import java.io.File

/**
 * Configuración de una llamada de transcripción para un proveedor concreto.
 *
 * @param provider Identificador del proveedor al que se envía la petición.
 * @param endpoint Endpoint del servicio (el del proveedor por defecto o uno personalizado).
 * @param model Modelo a usar (p. ej. `whisper-1`, `nova-3`).
 * @param apiKey Clave de API del proveedor (nunca se registra ni se persiste en claro).
 * @param language Código de idioma (p. ej. `es`, `en`); cadena vacía = detección automática.
 */
data class TranscriptionConfig(
    val provider: TranscriptionProviderId,
    val endpoint: String,
    val model: String,
    val apiKey: String,
    val language: String,
)

/** Audio a transcribir junto a su tipo MIME. */
data class TranscriptionRequest(
    val audio: File,
    val mimeType: String,
)

/** Resultado de una transcripción correcta. */
data class TranscriptionResult(
    val text: String,
    val provider: TranscriptionProviderId,
    val language: String,
)

/** Categoría de error de transcripción, mapeada desde el estado HTTP o el parseo. */
enum class TranscriptionErrorKind {
    AUTH,
    PAYLOAD_TOO_LARGE,
    UNSUPPORTED,
    RATE_LIMIT,
    SERVER,
    NETWORK,
    PARSE,
}

/** Error de transcripción tipado. */
class TranscriptionException(
    message: String,
    val kind: TranscriptionErrorKind,
    cause: Throwable? = null,
) : Exception(message, cause)

/** Contrato que debe cumplir todo proveedor de transcripción. */
interface TranscriptionProvider {
    val id: TranscriptionProviderId

    /** Transcribe [request] con [config] y devuelve el texto resultante. */
    suspend fun transcribe(config: TranscriptionConfig, request: TranscriptionRequest): TranscriptionResult
}
