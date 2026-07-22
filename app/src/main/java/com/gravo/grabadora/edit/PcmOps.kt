package com.gravo.grabadora.edit

import kotlin.math.abs
import kotlin.math.pow

/** Fuente de PCM float32 intercalado direccionable por frame. */
interface PcmSource {
    val frames: Long
    val channels: Int

    /** Lee [frameCount] frames desde [frameStart] en [out]; devuelve frames leídos. */
    fun read(frameStart: Long, frameCount: Int, out: FloatArray): Int
}

/** Operaciones de edición sobre PCM. JVM puro y testeable. */
object PcmOps {
    const val SPLICE_FADE_MS = 5

    fun gainFactor(db: Float): Float = 10f.pow(db / 20f)

    /**
     * Rangos de frames que se conservan tras el recorte y los cortes.
     * [trimStart]/[trimEnd] delimitan la selección global; [cuts] son rangos eliminados.
     */
    fun keptRanges(totalFrames: Long, trimStart: Long, trimEnd: Long, cuts: List<LongRange>): List<LongRange> {
        val start = trimStart.coerceIn(0, totalFrames)
        val end = trimEnd.coerceIn(start, totalFrames)
        var kept = listOf(start until end)
        for (cut in cuts.sortedBy { it.first }) {
            kept = kept.flatMap { range ->
                when {
                    cut.last < range.first || cut.first > range.last -> listOf(range)
                    else -> buildList {
                        if (cut.first > range.first) add(range.first until cut.first)
                        if (cut.last < range.last) add((cut.last + 1)..range.last)
                    }
                }
            }
        }
        return kept.filter { !it.isEmpty() }
    }

    fun outputFrames(kept: List<LongRange>): Long = kept.sumOf { it.last - it.first + 1 }

    /** Reubica un instante (frame) del original a la línea temporal resultante; null si cae en zona eliminada. */
    fun remapFrame(frame: Long, kept: List<LongRange>): Long? {
        var acc = 0L
        for (range in kept) {
            if (frame < range.first) return null
            if (frame <= range.last) return acc + (frame - range.first)
            acc += range.last - range.first + 1
        }
        return null
    }

    /** Pico absoluto (post-ganancia) sobre los rangos conservados; para normalizar. */
    fun scanPeak(source: PcmSource, kept: List<LongRange>, gain: Float, blockFrames: Int = 65_536): Float {
        val buf = FloatArray(blockFrames * source.channels)
        var peak = 0f
        for (range in kept) {
            var pos = range.first
            while (pos <= range.last) {
                val count = minOf(blockFrames.toLong(), range.last - pos + 1).toInt()
                val read = source.read(pos, count, buf)
                if (read <= 0) break
                val samples = read * source.channels
                for (i in 0 until samples) {
                    val a = abs(buf[i] * gain)
                    if (a > peak) peak = a
                }
                pos += read
            }
        }
        return peak
    }

    /** Factor para normalizar el pico a [targetDb] (p.ej. −1 dB). 1f si no hay señal. */
    fun normalizeFactor(peak: Float, targetDb: Float = -1f): Float =
        if (peak <= 0f) 1f else gainFactor(targetDb) / peak

    /**
     * Renderiza la edición: rangos conservados con empalme suave entre ellos, ganancia
     * y fundidos globales de entrada/salida. Entrega bloques al [write] en streaming.
     */
    fun render(
        source: PcmSource,
        kept: List<LongRange>,
        sampleRate: Int,
        gain: Float,
        fadeInFrames: Long,
        fadeOutFrames: Long,
        blockFrames: Int = 65_536,
        write: (buffer: FloatArray, samples: Int) -> Unit,
    ) {
        val channels = source.channels
        val total = outputFrames(kept)
        val splice = (SPLICE_FADE_MS * sampleRate / 1000).toLong()
        val buf = FloatArray(blockFrames * channels)
        var outPos = 0L

        for ((index, range) in kept.withIndex()) {
            val rangeLen = range.last - range.first + 1
            var pos = range.first
            while (pos <= range.last) {
                val count = minOf(blockFrames.toLong(), range.last - pos + 1).toInt()
                val read = source.read(pos, count, buf)
                if (read <= 0) break
                for (f in 0 until read) {
                    val inRange = pos - range.first + f
                    val globalOut = outPos + f
                    var factor = gain
                    // empalme entre segmentos: fade-out al final del previo, fade-in al inicio del siguiente
                    if (index > 0 && inRange < splice) {
                        factor *= inRange / splice.toFloat()
                    }
                    if (index < kept.size - 1 && rangeLen - 1 - inRange < splice) {
                        factor *= (rangeLen - 1 - inRange) / splice.toFloat()
                    }
                    // fundidos globales
                    if (fadeInFrames > 0 && globalOut < fadeInFrames) {
                        factor *= globalOut / fadeInFrames.toFloat()
                    }
                    if (fadeOutFrames > 0 && total - 1 - globalOut < fadeOutFrames) {
                        factor *= (total - 1 - globalOut) / fadeOutFrames.toFloat()
                    }
                    if (factor != 1f) {
                        val base = f * channels
                        for (c in 0 until channels) buf[base + c] = buf[base + c] * factor
                    }
                }
                write(buf, read * channels)
                pos += read
                outPos += read
            }
        }
    }
}
