package com.gravo.grabadora.audio.encode

/** Conversión de PCM float32 [-1,1] a enteros little-endian. JVM puro, testeable. */
object PcmConvert {
    fun clamp(v: Float): Float = when {
        v > 1f -> 1f
        v < -1f -> -1f
        else -> v
    }

    /** float [-1,1] → int16 LE. Devuelve bytes escritos (n*2). */
    fun toInt16(src: FloatArray, n: Int, dst: ByteArray): Int {
        for (i in 0 until n) {
            val s = (clamp(src[i]) * 32767f).toInt()
            dst[i * 2] = (s and 0xFF).toByte()
            dst[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
        }
        return n * 2
    }

    /** float [-1,1] → int24 LE empaquetado en 3 bytes. */
    fun toInt24(src: FloatArray, n: Int, dst: ByteArray): Int {
        for (i in 0 until n) {
            val s = (clamp(src[i]).toDouble() * 8_388_607.0).toInt()
            dst[i * 3] = (s and 0xFF).toByte()
            dst[i * 3 + 1] = ((s shr 8) and 0xFF).toByte()
            dst[i * 3 + 2] = ((s shr 16) and 0xFF).toByte()
        }
        return n * 3
    }

    /** float32 IEEE LE tal cual (WAV formato 3). */
    fun toFloat32(src: FloatArray, n: Int, dst: ByteArray): Int {
        for (i in 0 until n) {
            val bits = java.lang.Float.floatToIntBits(src[i])
            dst[i * 4] = (bits and 0xFF).toByte()
            dst[i * 4 + 1] = ((bits shr 8) and 0xFF).toByte()
            dst[i * 4 + 2] = ((bits shr 16) and 0xFF).toByte()
            dst[i * 4 + 3] = ((bits shr 24) and 0xFF).toByte()
        }
        return n * 4
    }

    /** float [-1,1] → int32 (para libFLAC, que espera muestras int32 con los bits útiles abajo). */
    fun toInt32Samples(src: FloatArray, n: Int, bitsPerSample: Int, dst: IntArray): Int {
        val max = (1L shl (bitsPerSample - 1)) - 1
        for (i in 0 until n) {
            dst[i] = (clamp(src[i]).toDouble() * max).toInt()
        }
        return n
    }

    /** int16 LE → float [-1,1] (decodificación). */
    fun int16ToFloat(src: ByteArray, offset: Int, byteCount: Int, dst: FloatArray): Int {
        val n = byteCount / 2
        for (i in 0 until n) {
            val lo = src[offset + i * 2].toInt() and 0xFF
            val hi = src[offset + i * 2 + 1].toInt()
            dst[i] = ((hi shl 8) or lo) / 32768f
        }
        return n
    }
}
