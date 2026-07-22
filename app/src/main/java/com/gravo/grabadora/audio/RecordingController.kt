package com.gravo.grabadora.audio

import android.content.Context
import android.content.Intent
import com.gravo.grabadora.audio.encode.AudioSink
import com.gravo.grabadora.data.RecordingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

enum class RecStatus { IDLE, RECORDING, PAUSED }

/**
 * Orquesta captura → audímetro + sink. Único para toda la app (vive en AppContainer):
 * la Home lo usa para monitorizar y el servicio foreground para grabar.
 */
class RecordingController(
    private val context: Context,
    private val repository: RecordingRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    val meter = LevelMeter()

    private val _status = MutableStateFlow(RecStatus.IDLE)
    val status: StateFlow<RecStatus> = _status

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs

    private var sink: AudioSink? = null
    private var spec: RecordingSpec = RecordingSpec()
    private var outputFile: File? = null
    private var framesWritten = 0L
    private var monitorRefs = 0

    private val engine = AudioCaptureEngine { buffer, n ->
        meter.process(buffer, n)
        if (_status.value == RecStatus.RECORDING) {
            val s = sink
            if (s != null) {
                runCatching { s.write(buffer, n) }.onFailure { stopEngineOnError() }
                framesWritten += n / spec.channels
                _elapsedMs.value = framesWritten * 1000L / spec.sampleRate
            }
        }
    }

    private fun stopEngineOnError() {
        scope.launch { mutex.withLock { abortLocked() } }
    }

    /** La Home pide monitorización del audímetro mientras es visible. */
    fun startMonitoring(settingsSpec: RecordingSpec, micId: Int) {
        synchronized(this) {
            monitorRefs++
            ensureEngine(settingsSpec, micId)
        }
    }

    fun stopMonitoring() {
        synchronized(this) {
            monitorRefs--
            if (monitorRefs <= 0 && _status.value == RecStatus.IDLE) {
                engine.stop()
                meter.reset()
            }
        }
    }

    private fun ensureEngine(s: RecordingSpec, micId: Int) {
        if (!engine.isRunning) {
            meter.configure(s.sampleRate, s.channels)
            engine.setGainDb(s.gainDb)
            engine.start(s.sampleRate, s.channels, MicSelector.deviceById(context, micId))
        } else {
            engine.setGainDb(s.gainDb)
        }
    }

    fun setGainDb(db: Float) = engine.setGainDb(db)

    suspend fun startRecording(s: RecordingSpec, micId: Int) = mutex.withLock {
        if (_status.value != RecStatus.IDLE) return@withLock
        // el formato/canales de la sesión quedan fijados al empezar
        if (engine.isRunning && (engine.sampleRate != s.sampleRate || engine.channels != s.channels)) {
            engine.stop()
        }
        spec = s
        ensureEngine(s, micId)
        val file = repository.newRecordingFile(s.format.ext)
        val newSink = AudioSink.create(s.format)
        newSink.start(s, file)
        outputFile = file
        sink = newSink
        framesWritten = 0
        _elapsedMs.value = 0
        _status.value = RecStatus.RECORDING
        context.startForegroundService(Intent(context, RecordingService::class.java))
    }

    fun pause() {
        if (_status.value == RecStatus.RECORDING) _status.value = RecStatus.PAUSED
    }

    fun resume() {
        if (_status.value == RecStatus.PAUSED) _status.value = RecStatus.RECORDING
    }

    fun togglePause() {
        when (_status.value) {
            RecStatus.RECORDING -> pause()
            RecStatus.PAUSED -> resume()
            RecStatus.IDLE -> {}
        }
    }

    /** Cierra la grabación, la persiste y devuelve el id de la nueva entrada. */
    suspend fun finishRecording(defaultNameTemplate: String): Long? = mutex.withLock {
        if (_status.value == RecStatus.IDLE) return@withLock null
        _status.value = RecStatus.IDLE
        val s = sink ?: return@withLock null
        sink = null
        val durationMs = framesWritten * 1000L / spec.sampleRate
        val file = runCatching { s.finish() }.getOrElse {
            outputFile?.delete()
            stopServiceAndMaybeEngine()
            return@withLock null
        }
        val name = repository.defaultName(defaultNameTemplate)
        val id = repository.insertFinished(file, spec, durationMs, name)
        stopServiceAndMaybeEngine()
        return@withLock id
    }

    private fun abortLocked() {
        _status.value = RecStatus.IDLE
        sink?.abort()
        sink = null
        stopServiceAndMaybeEngine()
    }

    private fun stopServiceAndMaybeEngine() {
        context.stopService(Intent(context, RecordingService::class.java))
        synchronized(this) {
            if (monitorRefs <= 0) {
                engine.stop()
                meter.reset()
            }
        }
    }
}
