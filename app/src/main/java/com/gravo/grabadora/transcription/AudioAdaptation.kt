package com.gravo.grabadora.transcription

import com.gravo.grabadora.audio.RecordFormat
import java.io.File
import kotlin.math.ceil

/** Cómo se entrega el audio al proveedor de transcripción. */
enum class AdaptationMode { ORIGINAL, TRANSCODE }

/** Plan de adaptación: entrega tal cual o recodificación (posiblemente troceada). */
data class AdaptationPlan(
    val mode: AdaptationMode,
    val mimeType: String,
    /** Rangos de duración en ms a transcribir por separado; vacío si el audio va entero. */
    val chunks: List<LongRange>,
)

/**
 * Decide cómo adaptar una grabación a las restricciones del proveedor.
 * JVM puro, sin dependencias de Android.
 */
object AudioAdaptation {
    /** Bitrate efectivo estimado del MP3 mono a 64 kbps (~8 KB/s). */
    private const val MP3_MONO_BYTES_PER_SECOND = 8000.0

    fun mimeTypeFor(ext: String): String = when (ext) {
        "wav" -> "audio/wav"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "ogg" -> "audio/ogg"
        "flac" -> "audio/flac"
        "aac" -> "audio/aac"
        else -> "audio/$ext"
    }

    fun plan(
        input: File,
        format: RecordFormat,
        provider: TranscriptionProviderId,
        durationMs: Long,
    ): AdaptationPlan {
        val ext = format.ext
        if (ext in provider.acceptedExtensions && input.length() <= provider.maxBytes) {
            return AdaptationPlan(AdaptationMode.ORIGINAL, mimeTypeFor(ext), emptyList())
        }
        val estBytes = durationMs / 1000.0 * MP3_MONO_BYTES_PER_SECOND
        val chunks = if (estBytes > provider.maxBytes) {
            val parts = ceil(estBytes / provider.maxBytes).toInt().coerceAtLeast(1)
            val chunkMs = durationMs / parts
            List(parts) { i ->
                val start = i * chunkMs
                val end = if (i == parts - 1) durationMs else start + chunkMs - 1
                start..end
            }
        } else {
            emptyList()
        }
        return AdaptationPlan(AdaptationMode.TRANSCODE, "audio/mpeg", chunks)
    }
}
