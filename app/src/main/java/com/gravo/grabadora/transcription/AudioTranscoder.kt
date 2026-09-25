package com.gravo.grabadora.transcription

import com.gravo.grabadora.audio.BitDepth
import com.gravo.grabadora.audio.RecordFormat
import com.gravo.grabadora.audio.RecordingSpec
import com.gravo.grabadora.audio.decode.AudioDecoder
import com.gravo.grabadora.audio.encode.Mp3LameSink
import com.gravo.grabadora.audio.encode.PcmDownmix
import com.gravo.grabadora.edit.FilePcmSource
import java.io.File

/**
 * Recodifica audio al formato que el proveedor de transcripción acepta.
 * Android: decodifica con MediaCodec ([AudioDecoder]) y codifica con libmp3lame
 * ([Mp3LameSink]). No se prueba en JVM (depende del framework y del códec nativo).
 */
class AudioTranscoder {
    fun transcode(
        input: File,
        format: RecordFormat,
        plan: AdaptationPlan,
        outDir: File,
    ): List<File> {
        if (plan.mode == AdaptationMode.ORIGINAL) return listOf(input)

        outDir.mkdirs()
        val tmpPcm = File(outDir, "transcode_${input.nameWithoutExtension}.pcm")
        val decoded = AudioDecoder.decodeToFile(input, tmpPcm)
        val spec = RecordingSpec(
            format = RecordFormat.MP3,
            sampleRate = decoded.sampleRate,
            channels = 1,
            depth = BitDepth.B16,
        )

        return try {
            FilePcmSource(decoded).use { source ->
                if (plan.chunks.isEmpty()) {
                    val out = File(outDir, "${input.nameWithoutExtension}.mp3")
                    encodeRange(source, spec, 0, source.frames, out)
                    listOf(out)
                } else {
                    plan.chunks.mapIndexed { index, chunk ->
                        val out = File(outDir, "${input.nameWithoutExtension}_$index.mp3")
                        val startFrame = chunk.first * decoded.sampleRate / 1000
                        val endFrame = (chunk.last * decoded.sampleRate / 1000 + 1)
                            .coerceAtMost(source.frames)
                        encodeRange(source, spec, startFrame, endFrame, out)
                        out
                    }
                }
            }
        } finally {
            runCatching { tmpPcm.delete() }
        }
    }

    private fun encodeRange(
        source: FilePcmSource,
        spec: RecordingSpec,
        startFrame: Long,
        endFrame: Long,
        out: File,
    ) {
        val sink = Mp3LameSink(bitrateKbps = 64, forceMono = true)
        sink.start(spec, out)
        try {
            val blockFrames = 65_536
            val buf = FloatArray(blockFrames * source.channels)
            val mono = FloatArray(blockFrames)
            var pos = startFrame
            while (pos < endFrame) {
                val count = minOf(blockFrames.toLong(), endFrame - pos).toInt()
                val read = source.read(pos, count, buf)
                if (read <= 0) break
                val samples = read * source.channels
                if (source.channels == 2) {
                    PcmDownmix.stereoToMono(buf, samples, mono)
                    sink.write(mono, read)
                } else {
                    sink.write(buf, samples)
                }
                pos += read
            }
            sink.finish()
        } catch (e: Exception) {
            sink.abort()
            throw e
        }
    }
}
