package com.gravo.grabadora.audio.encode

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import androidx.annotation.RequiresApi
import com.gravo.grabadora.audio.RecordingSpec
import java.io.File

/** OGG/Opus con MediaCodec + MediaMuxer OGG. Requiere API 29. */
@RequiresApi(Build.VERSION_CODES.Q)
class OpusOggSink : AudioSink {
    private lateinit var file: File
    private lateinit var codec: MediaCodec
    private lateinit var muxer: MediaMuxer
    private var trackIndex = -1
    private var muxerStarted = false
    private var presentationUs = 0L
    private var sampleRate = 48_000
    private var channels = 2
    private var pcmScratch = ByteArray(0)
    private val bufferInfo = MediaCodec.BufferInfo()

    override fun start(spec: RecordingSpec, output: File) {
        file = output
        sampleRate = spec.sampleRate
        channels = spec.channels
        presentationUs = 0
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, sampleRate, channels).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, if (channels == 2) 128_000 else 96_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 65_536)
        }
        codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG)
    }

    override fun write(buffer: FloatArray, n: Int) {
        if (pcmScratch.size < n * 2) pcmScratch = ByteArray(n * 2)
        val bytes = PcmConvert.toInt16(buffer, n, pcmScratch)
        var offset = 0
        while (offset < bytes) {
            val inIndex = codec.dequeueInputBuffer(10_000)
            if (inIndex < 0) { drain(false); continue }
            val inBuf = codec.getInputBuffer(inIndex)!!
            val chunk = minOf(bytes - offset, inBuf.remaining())
            inBuf.put(pcmScratch, offset, chunk)
            codec.queueInputBuffer(inIndex, 0, chunk, presentationUs, 0)
            presentationUs += chunk.toLong() * 1_000_000L / (2L * channels * sampleRate)
            offset += chunk
            drain(false)
        }
    }

    private fun drain(end: Boolean) {
        while (true) {
            val outIndex = codec.dequeueOutputBuffer(bufferInfo, if (end) 10_000 else 0)
            when {
                outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    trackIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    muxerStarted = true
                }
                outIndex >= 0 -> {
                    val outBuf = codec.getOutputBuffer(outIndex)!!
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && bufferInfo.size > 0 && muxerStarted) {
                        muxer.writeSampleData(trackIndex, outBuf, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
                else -> if (!end) return else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) continue else return
            }
        }
    }

    override fun finish(): File {
        val inIndex = codec.dequeueInputBuffer(10_000)
        if (inIndex >= 0) {
            codec.queueInputBuffer(inIndex, 0, 0, presentationUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        }
        drain(true)
        runCatching { codec.stop(); codec.release() }
        runCatching { if (muxerStarted) muxer.stop(); muxer.release() }
        return file
    }

    override fun abort() {
        runCatching { codec.stop(); codec.release() }
        runCatching { muxer.release() }
        file.delete()
    }
}
