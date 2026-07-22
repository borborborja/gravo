package com.gravo.grabadora.audio

/** Formatos de grabación de la maqueta. */
enum class RecordFormat(val ext: String, val label: String, val mime: String?) {
    WAV("wav", "WAV", null),
    MP3("mp3", "MP3", null),
    M4A("m4a", "M4A", "audio/mp4a-latm"),
    OGG("ogg", "OGG", "audio/opus"),
    FLAC("flac", "FLAC", null);

    val isLossy: Boolean get() = this == MP3 || this == M4A || this == OGG
}

enum class BitDepth(val bits: Int, val label: String) {
    B16(16, "16-bit"), B24(24, "24-bit"), B32(32, "32-bit");
}

data class RecordingSpec(
    val format: RecordFormat = RecordFormat.WAV,
    val sampleRate: Int = 48_000,
    val channels: Int = 2,
    val depth: BitDepth = BitDepth.B24,
    /** Ganancia de entrada en dB (−25..+25). */
    val gainDb: Float = 0f,
) {
    /** Profundidad real que admite cada formato: WAV todas, FLAC máx. 24, lossy → 16 efectivo. */
    val effectiveDepth: BitDepth
        get() = effectiveDepth(format, depth)

    companion object {
        fun effectiveDepth(format: RecordFormat, requested: BitDepth): BitDepth = when (format) {
            RecordFormat.WAV -> requested
            RecordFormat.FLAC -> if (requested == BitDepth.B32) BitDepth.B24 else requested
            RecordFormat.MP3, RecordFormat.M4A, RecordFormat.OGG -> BitDepth.B16
        }

        /** Slider 0..100 de la maqueta → dB (−25..+25, 50 = 0 dB). */
        fun sliderToDb(value: Int): Float = (value - 50) * 0.5f

        fun dbToSlider(db: Float): Int = (db / 0.5f + 50).toInt().coerceIn(0, 100)
    }
}
