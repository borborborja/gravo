package com.gravo.grabadora.transcription

/**
 * Convierte un código de estado HTTP de error en una [TranscriptionException] tipada.
 *
 * Los códigos 2xx (p. ej. 200) no representan error: devuelven una excepción con el
 * kind genérico [TranscriptionErrorKind.SERVER] en lugar de lanzar, para que el
 * llamador pueda decidir. En la práctica sólo se invoca con códigos no-2xx.
 *
 * @param code Código de estado HTTP.
 * @param body Cuerpo de la respuesta, usado para adjuntar un fragmento legible al mensaje.
 */
fun mapHttpError(code: Int, body: String): TranscriptionException {
    val kind = when (code) {
        401, 403 -> TranscriptionErrorKind.AUTH
        413 -> TranscriptionErrorKind.PAYLOAD_TOO_LARGE
        400, 415, 422 -> TranscriptionErrorKind.UNSUPPORTED
        429 -> TranscriptionErrorKind.RATE_LIMIT
        in 500..599 -> TranscriptionErrorKind.SERVER
        else -> TranscriptionErrorKind.SERVER
    }
    val snippet = snippet(body)
    val message = if (snippet.isEmpty()) "HTTP $code" else "HTTP $code: $snippet"
    return TranscriptionException(message, kind)
}

private fun snippet(body: String, max: Int = 200): String {
    val trimmed = body.trim()
    return if (trimmed.length <= max) trimmed else trimmed.take(max) + "…"
}
