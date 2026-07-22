package com.gravo.grabadora.audio.encode

import com.gravo.grabadora.audio.RecordFormat
import com.gravo.grabadora.audio.RecordingSpec
import java.io.File

/**
 * Destino de codificación. Recibe PCM float32 intercalado [-1,1] y produce el fichero final.
 * Se usa tanto al grabar como al reencodificar en el editor.
 */
interface AudioSink {
    fun start(spec: RecordingSpec, output: File)

    /** Escribe [n] muestras (intercaladas) de [buffer]. */
    fun write(buffer: FloatArray, n: Int)

    /** Cierra y devuelve el fichero final. */
    fun finish(): File

    /** Libera recursos si se aborta la grabación. */
    fun abort()

    companion object {
        fun create(format: RecordFormat): AudioSink = when (format) {
            RecordFormat.WAV -> WavSink()
            RecordFormat.MP3 -> Mp3LameSink()
            RecordFormat.M4A -> AacM4aSink()
            RecordFormat.OGG -> OpusOggSink()
            RecordFormat.FLAC -> FlacSink()
        }
    }
}
