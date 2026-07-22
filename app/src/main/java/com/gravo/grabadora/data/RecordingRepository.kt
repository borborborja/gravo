package com.gravo.grabadora.data

import android.content.Context
import com.gravo.grabadora.audio.RecordingSpec
import com.gravo.grabadora.data.db.AppDatabase
import com.gravo.grabadora.data.db.ChapterEntity
import com.gravo.grabadora.data.db.RecordingEntity
import com.gravo.grabadora.data.db.RecordingTagCrossRef
import com.gravo.grabadora.data.db.RecordingWithTags
import kotlinx.coroutines.flow.Flow
import java.io.File

class RecordingRepository(
    private val context: Context,
    private val db: AppDatabase,
) {
    val recordings: Flow<List<RecordingWithTags>> = db.recordingDao().observeAll()
    val usedTags: Flow<List<String>> = db.tagDao().observeUsedTags()

    fun recordingsDir(): File = File(context.filesDir, "recordings").apply { mkdirs() }

    /** Fichero destino para una grabación nueva. */
    fun newRecordingFile(ext: String): File {
        var f: File
        var i = 0
        do {
            val stamp = System.currentTimeMillis() + i
            f = File(recordingsDir(), "rec_$stamp.$ext")
            i++
        } while (f.exists())
        return f
    }

    suspend fun defaultName(template: String): String = template.format(db.recordingDao().count() + 1)

    suspend fun insertFinished(file: File, spec: RecordingSpec, durationMs: Long, name: String): Long {
        return db.recordingDao().insert(
            RecordingEntity(
                name = name,
                filePath = file.absolutePath,
                format = spec.format.name,
                durationMs = durationMs,
                sampleRate = spec.sampleRate,
                channels = spec.channels,
                bitDepth = spec.effectiveDepth.bits,
                sizeBytes = file.length(),
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    fun observeRecording(id: Long): Flow<RecordingWithTags?> = db.recordingDao().observeById(id)
    suspend fun recording(id: Long): RecordingEntity? = db.recordingDao().byId(id)

    suspend fun rename(id: Long, name: String) = db.recordingDao().rename(id, name)

    suspend fun delete(id: Long) {
        db.recordingDao().byId(id)?.let { File(it.filePath).delete() }
        db.chapterDao().deleteAllFor(id)
        db.recordingDao().delete(id)
    }

    suspend fun toggleTag(recordingId: Long, tag: String, add: Boolean) {
        db.tagDao().insertAll(listOf(com.gravo.grabadora.data.db.TagEntity(tag)))
        val ref = RecordingTagCrossRef(recordingId, tag)
        if (add) db.tagDao().addToRecording(ref) else db.tagDao().removeFromRecording(ref)
    }

    // --- capítulos ---
    fun observeChapters(recordingId: Long): Flow<List<ChapterEntity>> =
        db.chapterDao().observeForRecording(recordingId)

    suspend fun chapters(recordingId: Long): List<ChapterEntity> = db.chapterDao().forRecording(recordingId)
    suspend fun addChapter(recordingId: Long, timeMs: Long, name: String) {
        db.chapterDao().insert(ChapterEntity(recordingId = recordingId, timeMs = timeMs, name = name))
    }

    suspend fun deleteChapter(id: Long) = db.chapterDao().delete(id)

    suspend fun replaceChapters(recordingId: Long, chapters: List<ChapterEntity>) {
        db.chapterDao().deleteAllFor(recordingId)
        db.chapterDao().insertAll(chapters)
    }

    suspend fun updateAfterEdit(id: Long, durationMs: Long, sizeBytes: Long) =
        db.recordingDao().updateAfterEdit(id, durationMs, sizeBytes)
}
