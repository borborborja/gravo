package com.gravo.grabadora.audio.encode

import com.gravo.grabadora.audio.RecordingSpec
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

/** MP3 vía libmp3lame (JNI). 320/256 kbps CBR según canales. */
class Mp3LameSink : AudioSink {
    private lateinit var file: File
    private lateinit var out: BufferedOutputStream
    private var handle = 0L
    private var channels = 2
    private var mp3Buf = ByteArray(0)

    override fun start(spec: RecordingSpec, output: File) {
        file = output
        channels = spec.channels
        out = BufferedOutputStream(FileOutputStream(file), 1 shl 16)
        handle = NativeCodecs.lameInit(spec.sampleRate, channels, if (channels == 2) 320 else 256)
        check(handle != 0L) { "lameInit falló" }
    }

    override fun write(buffer: FloatArray, n: Int) {
        val frames = n / channels
        // margen recomendado por LAME: 1.25*frames + 7200
        val needed = (frames * 5) / 4 + 7200
        if (mp3Buf.size < needed) mp3Buf = ByteArray(needed)
        val bytes = NativeCodecs.lameEncode(handle, buffer, frames, mp3Buf)
        check(bytes >= 0) { "lameEncode error $bytes" }
        if (bytes > 0) out.write(mp3Buf, 0, bytes)
    }

    override fun finish(): File {
        if (mp3Buf.size < 7200) mp3Buf = ByteArray(7200)
        val bytes = NativeCodecs.lameFlush(handle, mp3Buf)
        if (bytes > 0) out.write(mp3Buf, 0, bytes)
        NativeCodecs.lameClose(handle)
        handle = 0
        out.flush()
        out.close()
        return file
    }

    override fun abort() {
        if (handle != 0L) NativeCodecs.lameClose(handle)
        handle = 0
        runCatching { out.close() }
        file.delete()
    }
}
