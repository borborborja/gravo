package com.gravo.grabadora.audio.encode

/** JNI a libmp3lame y libFLAC (compiladas en app/src/main/cpp). */
object NativeCodecs {
    init {
        System.loadLibrary("gravocodecs")
    }

    // --- LAME ---
    external fun lameInit(sampleRate: Int, channels: Int, bitrateKbps: Int): Long
    /** Codifica [frames] frames float intercalados; devuelve bytes MP3 escritos en [out]. */
    external fun lameEncode(handle: Long, pcm: FloatArray, frames: Int, out: ByteArray): Int
    external fun lameFlush(handle: Long, out: ByteArray): Int
    external fun lameClose(handle: Long)

    // --- FLAC ---
    external fun flacInit(path: String, sampleRate: Int, channels: Int, bitsPerSample: Int): Long
    /** Procesa [frames] frames int32 intercalados; false si el encoder falló. */
    external fun flacWrite(handle: Long, samples: IntArray, frames: Int): Boolean
    external fun flacFinish(handle: Long): Boolean
}
