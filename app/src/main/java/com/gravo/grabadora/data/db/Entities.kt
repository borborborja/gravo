package com.gravo.grabadora.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val filePath: String,
    val format: String,
    val durationMs: Long,
    val sampleRate: Int,
    val channels: Int,
    val bitDepth: Int,
    val sizeBytes: Long,
    val createdAt: Long,
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val name: String,
)

@Entity(
    tableName = "recording_tags",
    primaryKeys = ["recordingId", "tagName"],
    indices = [Index("tagName")],
)
data class RecordingTagCrossRef(
    val recordingId: Long,
    val tagName: String,
)

@Entity(tableName = "chapters", indices = [Index("recordingId")])
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordingId: Long,
    val timeMs: Long,
    val name: String,
)

@Entity(
    tableName = "transcripts",
    foreignKeys = [ForeignKey(
        entity = RecordingEntity::class,
        parentColumns = ["id"],
        childColumns = ["recordingId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class TranscriptEntity(
    @PrimaryKey val recordingId: Long,
    val text: String,
    val provider: String,
    val model: String,
    val language: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class RecordingWithTags(
    @androidx.room.Embedded val recording: RecordingEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "name",
        associateBy = Junction(
            value = RecordingTagCrossRef::class,
            parentColumn = "recordingId",
            entityColumn = "tagName",
        ),
    )
    val tags: List<TagEntity>,
)
