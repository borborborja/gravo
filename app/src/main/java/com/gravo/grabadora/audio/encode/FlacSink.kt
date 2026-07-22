package com.gravo.grabadora.audio.encode

import com.gravo.grabadora.audio.RecordingSpec
import java.io.File

/** FLAC vía libFLAC (JNI). 16/24-bit reales. */
class FlacSink : AudioSink {
    private lateinit var file: File
    private var handle = 0L
    private var channels = 2
    private var bits = 24
    private var intBuf = IntArray(0)

    override fun start(spec: RecordingSpec, output: File) {
        file = output
        channels = spec.channels
        bits = spec.effectiveDepth.bits
        handle = NativeCodecs.flacInit(file.absolutePath, spec.sampleRate, channels, bits)
        check(handle != 0L) { "flacInit falló" }
    }

    override fun write(buffer: FloatArray, n: Int) {
        if (intBuf.size < n) intBuf = IntArray(n)
        PcmConvert.toInt32Samples(buffer, n, bits, intBuf)
        check(NativeCodecs.flacWrite(handle, intBuf, n / channels)) { "flacWrite falló" }
    }

    override fun finish(): File {
        NativeCodecs.flacFinish(handle)
        handle = 0
        return file
    }

    override fun abort() {
        if (handle != 0L) NativeCodecs.flacFinish(handle)
        handle = 0
        file.delete()
    }
}
