package com.gravo.grabadora.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Transaction
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RecordingWithTags>>

    @Transaction
    @Query("SELECT * FROM recordings WHERE id = :id")
    fun observeById(id: Long): Flow<RecordingWithTags?>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun byId(id: Long): RecordingEntity?

    @Insert
    suspend fun insert(recording: RecordingEntity): Long

    @Query("UPDATE recordings SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE recordings SET durationMs = :durationMs, sizeBytes = :sizeBytes WHERE id = :id")
    suspend fun updateAfterEdit(id: Long, durationMs: Long, sizeBytes: Long)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM recordings")
    suspend fun count(): Int
}

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tags: List<TagEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addToRecording(ref: RecordingTagCrossRef)

    @Delete
    suspend fun removeFromRecording(ref: RecordingTagCrossRef)

    @Query("SELECT DISTINCT tagName FROM recording_tags ORDER BY tagName")
    fun observeUsedTags(): Flow<List<String>>
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE recordingId = :recordingId ORDER BY timeMs")
    fun observeForRecording(recordingId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE recordingId = :recordingId ORDER BY timeMs")
    suspend fun forRecording(recordingId: Long): List<ChapterEntity>

    @Insert
    suspend fun insert(chapter: ChapterEntity): Long

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM chapters WHERE recordingId = :recordingId")
    suspend fun deleteAllFor(recordingId: Long)

    @Insert
    suspend fun insertAll(chapters: List<ChapterEntity>)
}
