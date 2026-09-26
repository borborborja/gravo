package com.gravo.grabadora.transcription

/**
 * Catálogo de proveedores de transcripción soportados.
 *
 * Cada entrada fija la configuración por defecto del proveedor: su endpoint base,
 * el modelo recomendado, si acepta una pista de idioma, las extensiones de audio que
 * admite sin re-codificar y el tamaño máximo de fichero que puede recibir directamente.
 *
 * @param label Nombre visible del proveedor.
 * @param defaultEndpoint Endpoint por defecto del servicio.
 * @param defaultModel Modelo por defecto del servicio.
 * @param supportsLanguageHint `true` si el proveedor acepta una pista de idioma explícita.
 * @param acceptedExtensions Extensiones de fichero (sin punto) que el proveedor acepta tal cual.
 * @param maxBytes Tamaño máximo en bytes del audio que el proveedor admite sin adaptación.
 */
enum class TranscriptionProviderId(
    val label: String,
    val defaultEndpoint: String,
    val defaultModel: String,
    val supportsLanguageHint: Boolean,
    val acceptedExtensions: Set<String>,
    val maxBytes: Long,
) {
    WHISPER(
        "Whisper",
        "https://api.groq.com/openai/v1/audio/transcriptions",
        "whisper-large-v3-turbo",
        supportsLanguageHint = true,
        acceptedExtensions = setOf("mp3", "mp4", "mpeg", "mpga", "m4a", "wav", "webm"),
        maxBytes = 25L * 1024 * 1024,
    ),
    GEMINI(
        "Gemini",
        "https://generativelanguage.googleapis.com",
        "gemini-3.5-transcribe",
        supportsLanguageHint = false,
        acceptedExtensions = setOf(
            "wav", "mp3", "aiff", "aac", "ogg", "flac", "mpeg",
            "m4a", "l16", "opus", "alaw", "mulaw", "webm"
        ),
        maxBytes = 2L * 1024 * 1024 * 1024,
    ),
    DEEPGRAM(
        "Deepgram",
        "https://api.deepgram.com",
        "nova-3",
        supportsLanguageHint = true,
        acceptedExtensions = setOf("wav", "mp3", "m4a", "flac", "ogg"),
        maxBytes = 2L * 1024 * 1024 * 1024,
    ),
}
