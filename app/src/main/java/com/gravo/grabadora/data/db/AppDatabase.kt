package com.gravo.grabadora.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RecordingEntity::class, TagEntity::class, RecordingTagCrossRef::class, ChapterEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun tagDao(): TagDao
    abstract fun chapterDao(): ChapterDao

    companion object {
        /** Etiquetas maestras de la maqueta. */
        val MASTER_TAGS = listOf("Trabajo", "Ideas", "Pódcast", "Música", "Personal", "Entrevista")

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "grabadora.db").build()
    }
}
