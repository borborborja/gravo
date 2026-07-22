package com.gravo.grabadora.audio.decode

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.gravo.grabadora.audio.encode.PcmConvert
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Resultado de decodificar: PCM float32 intercalado crudo en un fichero temporal. */
data class DecodedPcm(
    val file: File,
    val sampleRate: Int,
    val channels: Int,
) {
    val frames: Long get() = file.length() / 4L / channels
}

/** MediaExtractor + MediaCodec → PCM float32 en streaming (nunca en RAM). */
object AudioDecoder {
    fun decodeToFile(input: File, output: File): DecodedPcm {
        val extractor = MediaExtractor()
        extractor.setDataSource(input.absolutePath)
        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                trackIndex = i
                format = f
                break
            }
        }
        val trackFormat = format ?: run {
            extractor.release()
            throw IllegalArgumentException("Sin pista de audio: ${input.name}")
        }
        val mime = trackFormat.getString(MediaFormat.KEY_MIME)!!
        extractor.selectTrack(trackIndex)

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(trackFormat, null, null, 0)
        codec.start()

        var sampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var pcmEncoding = if (trackFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            trackFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
        } else {
            AudioFormat.ENCODING_PCM_16BIT
        }

        val out = BufferedOutputStream(FileOutputStream(output), 1 shl 16)
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var floatScratch = FloatArray(0)
        var byteScratch = ByteArray(0)

        try {
            while (!outputDone) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val buf = codec.getInputBuffer(inIndex)!!
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val f = codec.outputFormat
                        sampleRate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        if (f.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            pcmEncoding = f.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        }
                    }
                    outIndex >= 0 -> {
                        if (info.size > 0) {
                            val buf = codec.getOutputBuffer(outIndex)!!
                            buf.position(info.offset)
                            buf.limit(info.offset + info.size)
                            writePcmAsFloat(
                                buf, info.size, pcmEncoding, out,
                                floatScratchOf = { needed ->
                                    if (floatScratch.size < needed) floatScratch = FloatArray(needed)
                                    floatScratch
                                },
                                byteScratchOf = { needed ->
                                    if (byteScratch.size < needed) byteScratch = ByteArray(needed)
                                    byteScratch
                                },
                            )
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                }
            }
        } finally {
            out.flush()
            out.close()
            runCatching { codec.stop() }
            codec.release()
            extractor.release()
        }
        return DecodedPcm(output, sampleRate, channels)
    }

    private inline fun writePcmAsFloat(
        buf: ByteBuffer,
        size: Int,
        pcmEncoding: Int,
        out: BufferedOutputStream,
        floatScratchOf: (Int) -> FloatArray,
        byteScratchOf: (Int) -> ByteArray,
    ) {
        if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
            // ya es float32 LE: copia directa
            val bytes = byteScratchOf(size)
            buf.get(bytes, 0, size)
            out.write(bytes, 0, size)
        } else {
            val bytes = byteScratchOf(size)
            buf.get(bytes, 0, size)
            val n = size / 2
            val floats = floatScratchOf(n)
            PcmConvert.int16ToFloat(bytes, 0, size, floats)
            val outBytes = byteScratchOf(n * 4)
            val bb = ByteBuffer.wrap(outBytes).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until n) bb.putFloat(floats[i])
            out.write(outBytes, 0, n * 4)
        }
    }
}
