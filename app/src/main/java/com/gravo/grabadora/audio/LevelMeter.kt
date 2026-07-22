package com.gravo.grabadora.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max

/**
 * Medidor de nivel: mantiene las 28 barras del audímetro de la maqueta y el pico en dBFS.
 * JVM puro (testeable); recibe bloques PCM float desde el hilo de audio.
 */
class LevelMeter(val barCount: Int = BARS) {
    private val _levels = MutableStateFlow(FloatArray(barCount))
    val levels: StateFlow<FloatArray> = _levels

    private val _peakDb = MutableStateFlow(SILENCE_DB)
    val peakDb: StateFlow<Float> = _peakDb

    private var maxSinceLastBar = 0f
    private var samplesSinceLastBar = 0
    private var samplesPerBar = 4800 // ~100 ms a 48 kHz (se recalcula en configure)

    fun configure(sampleRate: Int, channels: Int) {
        samplesPerBar = sampleRate * channels / 10
        reset()
    }

    fun process(buffer: FloatArray, n: Int) {
        var blockPeak = 0f
        for (i in 0 until n) {
            val a = abs(buffer[i])
            if (a > blockPeak) blockPeak = a
        }
        maxSinceLastBar = max(maxSinceLastBar, blockPeak)
        _peakDb.value = toDb(blockPeak)
        samplesSinceLastBar += n
        if (samplesSinceLastBar >= samplesPerBar) {
            push(maxSinceLastBar)
            maxSinceLastBar = 0f
            samplesSinceLastBar = 0
        }
    }

    private fun push(value: Float) {
        val old = _levels.value
        val next = FloatArray(barCount)
        System.arraycopy(old, 1, next, 0, barCount - 1)
        next[barCount - 1] = value.coerceIn(0f, 1f)
        _levels.value = next
    }

    fun reset() {
        _levels.value = FloatArray(barCount)
        _peakDb.value = SILENCE_DB
        maxSinceLastBar = 0f
        samplesSinceLastBar = 0
    }

    companion object {
        const val BARS = 28
        const val SILENCE_DB = -60f

        /** Amplitud lineal [0,1] → dBFS, con suelo en −60. */
        fun toDb(amplitude: Float): Float {
            if (amplitude <= 0.001f) return SILENCE_DB
            return (20f * log10(amplitude)).coerceIn(SILENCE_DB, 0f)
        }
    }
}
