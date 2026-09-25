package com.gravo.grabadora.audio.encode

/** Reducción de canales sobre PCM float32 intercalado. JVM puro y testeable. */
object PcmDownmix {
    /**
     * Downmix estéreo intercalado a mono.
     *
     * @param input muestras intercaladas (frames*2).
     * @param samples número de muestras intercaladas en [input].
     * @param out destino mono; debe tener al menos `samples/2` posiciones.
     * @return número de frames mono escritos.
     */
    fun stereoToMono(input: FloatArray, samples: Int, out: FloatArray): Int {
        require(out.size >= samples / 2) { "out demasiado pequeño: ${out.size} < ${samples / 2}" }
        val frames = samples / 2
        for (f in 0 until frames) {
            out[f] = (input[2 * f] + input[2 * f + 1]) * 0.5f
        }
        return frames
    }
}
