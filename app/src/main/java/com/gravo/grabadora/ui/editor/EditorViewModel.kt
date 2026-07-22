package com.gravo.grabadora.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gravo.grabadora.AppContainer
import com.gravo.grabadora.audio.BitDepth
import com.gravo.grabadora.audio.RecordFormat
import com.gravo.grabadora.data.db.ChapterEntity
import com.gravo.grabadora.data.db.RecordingEntity
import com.gravo.grabadora.edit.EditPlan
import com.gravo.grabadora.edit.EditorEngine
import com.gravo.grabadora.waveform.WaveformExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class EditorTool { TRIM, CUT, MARKERS, VOLUME, FADE }

class EditorViewModel(
    private val container: AppContainer,
    private val recordingId: Long,
) : ViewModel() {
    private val repo = container.recordingRepository

    private val _recording = MutableStateFlow<RecordingEntity?>(null)
    val recording: StateFlow<RecordingEntity?> = _recording

    private val _peaks = MutableStateFlow(FloatArray(0))
    val peaks: StateFlow<FloatArray> = _peaks

    val chapters: StateFlow<List<ChapterEntity>> = repo.observeChapters(recordingId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val tool = MutableStateFlow(EditorTool.TRIM)

    // selección/puntero como fracción [0,1]
    val trimStart = MutableStateFlow(0f)
    val trimEnd = MutableStateFlow(1f)
    val pointer = MutableStateFlow(0.45f)

    // ajustes de herramientas
    val gainDb = MutableStateFlow(0f)
    val normalize = MutableStateFlow(false)
    val fadeIn = MutableStateFlow(false)
    val fadeOut = MutableStateFlow(false)
    val cuts = MutableStateFlow<List<ClosedFloatingPointRange<Float>>>(emptyList())

    val saving = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    val durationMs: Long get() = _recording.value?.durationMs ?: 1L

    init {
        viewModelScope.launch {
            _recording.value = repo.recording(recordingId)
            _recording.value?.let { rec ->
                runCatching {
                    _peaks.value = WaveformExtractor(container.app).peaks(File(rec.filePath))
                }
            }
        }
    }

    fun nudgePointerMs(deltaMs: Long) {
        val dur = durationMs.toFloat()
        pointer.value = (pointer.value + deltaMs / dur).coerceIn(0f, 1f)
    }

    /** En la herramienta Cortar, elimina la selección actual [trimStart, trimEnd] interna. */
    fun addCutFromSelection() {
        val start = trimStart.value
        val end = trimEnd.value
        if (end - start < 0.005f) return
        cuts.value = cuts.value + listOf(start..end)
        // restaura la selección global para seguir editando
        trimStart.value = 0f
        trimEnd.value = 1f
    }

    fun removeCut(index: Int) {
        cuts.value = cuts.value.filterIndexed { i, _ -> i != index }
    }

    fun addChapterAtPointer(nameTemplate: String) {
        viewModelScope.launch {
            val timeMs = (pointer.value * durationMs).toLong()
            repo.addChapter(recordingId, timeMs, nameTemplate.format(chapters.value.size + 1))
        }
    }

    fun deleteChapter(id: Long) {
        viewModelScope.launch { repo.deleteChapter(id) }
    }

    fun save(onDone: () -> Unit) {
        val rec = _recording.value ?: return
        if (saving.value) return
        saving.value = true
        error.value = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val plan = EditPlan(
                        trimStartMs = (trimStart.value * rec.durationMs).toLong(),
                        trimEndMs = (trimEnd.value * rec.durationMs).toLong(),
                        cutsMs = cuts.value.map {
                            (it.start * rec.durationMs).toLong()..(it.endInclusive * rec.durationMs).toLong()
                        },
                        gainDb = gainDb.value,
                        normalize = normalize.value,
                        fadeInMs = if (fadeIn.value) FADE_MS else 0,
                        fadeOutMs = if (fadeOut.value) FADE_MS else 0,
                    )
                    if (plan.isNoop && plan.trimEndMs >= rec.durationMs) return@withContext

                    val cacheDir = File(container.app.cacheDir, "editor")
                    val decoded = EditorEngine.decode(File(rec.filePath), cacheDir)
                    try {
                        val format = RecordFormat.valueOf(rec.format)
                        val depth = BitDepth.entries.firstOrNull { it.bits == rec.bitDepth } ?: BitDepth.B24
                        val tmpOut = File(cacheDir, "out_${rec.id}.${format.ext}")
                        val chapterList = repo.chapters(recordingId)
                        val result = EditorEngine.render(
                            decoded, plan, format, depth, tmpOut,
                            chapterTimesMs = chapterList.map { it.timeMs },
                        )
                        // sustituye el fichero original y actualiza metadatos
                        val target = File(rec.filePath)
                        tmpOut.copyTo(target, overwrite = true)
                        tmpOut.delete()
                        repo.updateAfterEdit(rec.id, result.durationMs, target.length())
                        val remapped = chapterList.zip(result.chapterTimesMs)
                            .mapNotNull { (chapter, newTime) -> newTime?.let { chapter.copy(id = 0, timeMs = it) } }
                        repo.replaceChapters(recordingId, remapped)
                    } finally {
                        decoded.file.delete()
                    }
                }
                onDone()
            } catch (e: Exception) {
                error.value = e.message ?: e.javaClass.simpleName
            } finally {
                saving.value = false
            }
        }
    }

    override fun onCleared() {
        File(container.app.cacheDir, "editor").deleteRecursively()
    }

    companion object {
        const val FADE_MS = 1500L

        fun factory(container: AppContainer, id: Long) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = EditorViewModel(container, id) as T
        }
    }
}
