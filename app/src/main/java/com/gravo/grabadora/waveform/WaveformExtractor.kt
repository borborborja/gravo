package com.gravo.grabadora.waveform

import android.content.Context
import com.gravo.grabadora.audio.decode.AudioDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * Extrae la envolvente (picos por bucket) para pintar la forma de onda,
 * con caché binaria en cacheDir/waveforms/<hash>.peaks.
 */
class WaveformExtractor(private val context: Context) {
    suspend fun peaks(audio: File, buckets: Int = 2000): FloatArray = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "waveforms").apply { mkdirs() }
        val cache = File(cacheDir, "${audio.name}_${audio.lastModified()}_$buckets.peaks")
        if (cache.exists()) {
            runCatching { return@withContext load(cache) }
        }
        val tmp = File(context.cacheDir, "wf_${audio.nameWithoutExtension}.pcm")
        try {
            val decoded = AudioDecoder.decodeToFile(audio, tmp)
            val result = extract(decoded.file, decoded.channels, buckets)
            runCatching { save(cache, result) }
            result
        } finally {
            tmp.delete()
        }
    }

    private fun extract(pcmFile: File, channels: Int, buckets: Int): FloatArray {
        val bytesPerFrame = 4 * channels
        val totalFrames = pcmFile.length() / bytesPerFrame
        if (totalFrames == 0L) return FloatArray(buckets)
        val framesPerBucket = (totalFrames / buckets).coerceAtLeast(1)
        val peaks = FloatArray(buckets)
        RandomAccessFile(pcmFile, "r").use { raf ->
            val blockBytes = 1 shl 16
            val buf = ByteArray(blockBytes)
            var frame = 0L
            var read: Int
            while (true) {
                read = raf.read(buf)
                if (read <= 0) break
                val bb = ByteBuffer.wrap(buf, 0, read).order(ByteOrder.LITTLE_ENDIAN)
                val n = read / 4
                for (i in 0 until n) {
                    val v = abs(bb.getFloat(i * 4))
                    val f = frame + i / channels
                    val bucket = ((f / framesPerBucket).toInt()).coerceAtMost(buckets - 1)
                    if (v > peaks[bucket]) peaks[bucket] = v
                }
                frame += n / channels
            }
        }
        // normaliza al pico global para que la onda llene el lienzo
        val max = peaks.max()
        if (max > 0.01f) {
            for (i in peaks.indices) peaks[i] = peaks[i] / max
        }
        return peaks
    }

    private fun save(file: File, peaks: FloatArray) {
        DataOutputStream(file.outputStream().buffered()).use { out ->
            out.writeInt(peaks.size)
            peaks.forEach { out.writeFloat(it) }
        }
    }

    private fun load(file: File): FloatArray {
        DataInputStream(file.inputStream().buffered()).use { input ->
            val n = input.readInt()
            return FloatArray(n) { input.readFloat() }
        }
    }
}
