package com.gravo.grabadora.audio.encode

import com.gravo.grabadora.audio.RecordingSpec
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * MP3 vía libmp3lame (JNI). Por defecto 320/256 kbps CBR según canales.
 *
 * @param bitrateKbps bitrate fijo; si es null usa 320 (estéreo) o 256 (mono).
 * @param forceMono fuerza la codificación a 1 canal haciendo downmix del estéreo.
 */
class Mp3LameSink(
    private val bitrateKbps: Int? = null,
    private val forceMono: Boolean = false,
) : AudioSink {
    private lateinit var file: File
    private lateinit var out: BufferedOutputStream
    private var handle = 0L
    private var channels = 2
    private var srcChannels = 2
    private var monoScratch = FloatArray(0)
    private var mp3Buf = ByteArray(0)

    override fun start(spec: RecordingSpec, output: File) {
        file = output
        srcChannels = spec.channels
        channels = if (forceMono) 1 else spec.channels
        out = BufferedOutputStream(FileOutputStream(file), 1 shl 16)
        val bitrate = bitrateKbps ?: if (channels == 2) 320 else 256
        handle = NativeCodecs.lameInit(spec.sampleRate, channels, bitrate)
        check(handle != 0L) { "lameInit falló" }
    }

    override fun write(buffer: FloatArray, n: Int) {
        val (pcm, frames) = if (forceMono && srcChannels == 2) {
            if (monoScratch.size < n / 2) monoScratch = FloatArray(n / 2)
            PcmDownmix.stereoToMono(buffer, n, monoScratch)
            monoScratch to (n / 2)
        } else {
            buffer to (n / channels)
        }
        // margen recomendado por LAME: 1.25*frames + 7200
        val needed = (frames * 5) / 4 + 7200
        if (mp3Buf.size < needed) mp3Buf = ByteArray(needed)
        val bytes = NativeCodecs.lameEncode(handle, pcm, frames, mp3Buf)
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
