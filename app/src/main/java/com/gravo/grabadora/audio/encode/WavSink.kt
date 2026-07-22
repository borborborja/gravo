package com.gravo.grabadora.audio.encode

import com.gravo.grabadora.audio.BitDepth
import com.gravo.grabadora.audio.RecordingSpec
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

/** WAV: 16/24-bit PCM entero o 32-bit float IEEE. JVM puro. */
class WavSink : AudioSink {
    private lateinit var file: File
    private lateinit var out: BufferedOutputStream
    private lateinit var depth: BitDepth
    private var channels = 2
    private var sampleRate = 48_000
    private var dataBytes = 0
    private var scratch = ByteArray(0)

    private val audioFormat: Int
        get() = if (depth == BitDepth.B32) WavHeader.FORMAT_FLOAT else WavHeader.FORMAT_PCM

    override fun start(spec: RecordingSpec, output: File) {
        file = output
        depth = spec.effectiveDepth
        channels = spec.channels
        sampleRate = spec.sampleRate
        dataBytes = 0
        out = BufferedOutputStream(FileOutputStream(file), 1 shl 16)
        out.write(WavHeader.build(audioFormat, sampleRate, channels, depth.bits, 0))
    }

    override fun write(buffer: FloatArray, n: Int) {
        val bytesPerSample = depth.bits / 8
        if (scratch.size < n * bytesPerSample) scratch = ByteArray(n * bytesPerSample)
        val written = when (depth) {
            BitDepth.B16 -> PcmConvert.toInt16(buffer, n, scratch)
            BitDepth.B24 -> PcmConvert.toInt24(buffer, n, scratch)
            BitDepth.B32 -> PcmConvert.toFloat32(buffer, n, scratch)
        }
        out.write(scratch, 0, written)
        dataBytes += written
    }

    override fun finish(): File {
        out.flush()
        out.close()
        RandomAccessFile(file, "rw").use {
            WavHeader.patchSizes(it, audioFormat, channels, depth.bits, dataBytes)
        }
        return file
    }

    override fun abort() {
        runCatching { out.close() }
        file.delete()
    }
}
