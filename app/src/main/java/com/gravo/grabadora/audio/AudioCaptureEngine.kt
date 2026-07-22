package com.gravo.grabadora.audio

import android.annotation.SuppressLint
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.pow

/**
 * Captura PCM float32 intercalado de AudioRecord en un hilo propio, aplica la ganancia
 * de entrada y entrega bloques al callback (audímetro + sink activo).
 */
class AudioCaptureEngine(
    private val onBuffer: (buffer: FloatArray, n: Int) -> Unit,
) {
    @Volatile private var gainFactor = 1f
    @Volatile private var running = false
    private var record: AudioRecord? = null
    private var thread: Thread? = null

    var sampleRate = 48_000; private set
    var channels = 2; private set

    fun setGainDb(db: Float) {
        gainFactor = 10f.pow(db / 20f)
    }

    val isRunning: Boolean get() = running

    @SuppressLint("MissingPermission")
    fun start(sampleRate: Int, channels: Int, preferredDevice: AudioDeviceInfo?) {
        if (running) return
        this.sampleRate = sampleRate
        this.channels = channels
        val channelMask = if (channels == 2) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_FLOAT)
        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelMask,
            AudioFormat.ENCODING_PCM_FLOAT,
            maxOf(minBuf * 2, sampleRate * channels), // ~0,25 s en float
        )
        check(rec.state == AudioRecord.STATE_INITIALIZED) { "AudioRecord no inicializado" }
        preferredDevice?.let { rec.preferredDevice = it }
        record = rec
        running = true
        rec.startRecording()
        thread = Thread({
            val buf = FloatArray(sampleRate * channels / 10) // bloques de ~100 ms
            while (running) {
                val n = rec.read(buf, 0, buf.size, AudioRecord.READ_BLOCKING)
                if (n > 0) {
                    val g = gainFactor
                    if (g != 1f) {
                        for (i in 0 until n) buf[i] = buf[i] * g
                    }
                    onBuffer(buf, n)
                }
            }
        }, "audio-capture").apply { priority = Thread.MAX_PRIORITY; start() }
    }

    fun stop() {
        running = false
        thread?.join(1000)
        thread = null
        record?.let {
            runCatching { it.stop() }
            it.release()
        }
        record = null
    }
}
