package com.gravo.grabadora.edit

import com.gravo.grabadora.audio.BitDepth
import com.gravo.grabadora.audio.RecordFormat
import com.gravo.grabadora.audio.RecordingSpec
import com.gravo.grabadora.audio.decode.AudioDecoder
import com.gravo.grabadora.audio.decode.DecodedPcm
import com.gravo.grabadora.audio.encode.AudioSink
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** PcmSource sobre el fichero temporal float32 del decodificador. */
class FilePcmSource(decoded: DecodedPcm) : PcmSource, AutoCloseable {
    private val raf = RandomAccessFile(decoded.file, "r")
    override val frames: Long = decoded.frames
    override val channels: Int = decoded.channels
    private var byteBuf = ByteArray(0)

    override fun read(frameStart: Long, frameCount: Int, out: FloatArray): Int {
        val bytesPerFrame = 4 * channels
        val needed = frameCount * bytesPerFrame
        if (byteBuf.size < needed) byteBuf = ByteArray(needed)
        raf.seek(frameStart * bytesPerFrame)
        var readBytes = 0
        while (readBytes < needed) {
            val r = raf.read(byteBuf, readBytes, needed - readBytes)
            if (r < 0) break
            readBytes += r
        }
        val framesRead = readBytes / bytesPerFrame
        val bb = ByteBuffer.wrap(byteBuf, 0, framesRead * bytesPerFrame).order(ByteOrder.LITTLE_ENDIAN)
        bb.asFloatBuffer().get(out, 0, framesRead * channels)
        return framesRead
    }

    override fun close() = raf.close()
}

/** Plan de edición no destructivo acumulado en el editor. */
data class EditPlan(
    val trimStartMs: Long = 0,
    val trimEndMs: Long,
    val cutsMs: List<LongRange> = emptyList(),
    val gainDb: Float = 0f,
    val normalize: Boolean = false,
    val fadeInMs: Long = 0,
    val fadeOutMs: Long = 0,
) {
    val isNoop: Boolean
        get() = trimStartMs == 0L && cutsMs.isEmpty() && gainDb == 0f && !normalize &&
            fadeInMs == 0L && fadeOutMs == 0L
}

/** Decodifica → aplica PcmOps → reencodifica con el AudioSink del formato original. */
object EditorEngine {
    fun decode(input: File, cacheDir: File): DecodedPcm {
        cacheDir.mkdirs()
        val tmp = File(cacheDir, "edit_${input.nameWithoutExtension}.pcm")
        return AudioDecoder.decodeToFile(input, tmp)
    }

    /**
     * Renderiza [plan] sobre [decoded] y escribe el resultado en [output] con el formato dado.
     * Devuelve la duración final en ms. También remapea [chapterTimesMs] a la nueva línea temporal.
     */
    fun render(
        decoded: DecodedPcm,
        plan: EditPlan,
        format: RecordFormat,
        depth: BitDepth,
        output: File,
        chapterTimesMs: List<Long> = emptyList(),
    ): RenderResult {
        val sr = decoded.sampleRate
        fun msToFrame(ms: Long) = ms * sr / 1000
        val kept = PcmOps.keptRanges(
            totalFrames = decoded.frames,
            trimStart = msToFrame(plan.trimStartMs),
            trimEnd = msToFrame(plan.trimEndMs).coerceAtMost(decoded.frames),
            cuts = plan.cutsMs.map { msToFrame(it.first)..msToFrame(it.last) },
        )
        require(kept.isNotEmpty()) { "La selección está vacía" }

        val spec = RecordingSpec(
            format = format,
            sampleRate = sr,
            channels = decoded.channels,
            depth = depth,
        )
        FilePcmSource(decoded).use { source ->
            val baseGain = PcmOps.gainFactor(plan.gainDb)
            val gain = if (plan.normalize) {
                val peak = PcmOps.scanPeak(source, kept, baseGain)
                baseGain * PcmOps.normalizeFactor(peak, targetDb = -1f)
            } else {
                baseGain
            }
            val sink = AudioSink.create(format)
            sink.start(spec, output)
            try {
                PcmOps.render(
                    source = source,
                    kept = kept,
                    sampleRate = sr,
                    gain = gain,
                    fadeInFrames = msToFrame(plan.fadeInMs),
                    fadeOutFrames = msToFrame(plan.fadeOutMs),
                ) { buffer, samples -> sink.write(buffer, samples) }
                sink.finish()
            } catch (e: Exception) {
                sink.abort()
                throw e
            }
        }
        val durationMs = PcmOps.outputFrames(kept) * 1000L / sr
        val chapters = chapterTimesMs.map { ms ->
            PcmOps.remapFrame(msToFrame(ms), kept)?.let { it * 1000L / sr }
        }
        return RenderResult(durationMs, chapters)
    }

    data class RenderResult(
        val durationMs: Long,
        /** Tiempos de capítulos remapeados; null si el capítulo cayó en zona eliminada. */
        val chapterTimesMs: List<Long?>,
    )
}
