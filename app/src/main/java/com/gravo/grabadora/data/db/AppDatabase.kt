package com.gravo.grabadora.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [RecordingEntity::class, TagEntity::class, RecordingTagCrossRef::class, ChapterEntity::class, TranscriptEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun tagDao(): TagDao
    abstract fun chapterDao(): ChapterDao
    abstract fun transcriptDao(): TranscriptDao

    companion object {
        /** Etiquetas maestras de la maqueta. */
        val MASTER_TAGS = listOf("Trabajo", "Ideas", "Pódcast", "Música", "Personal", "Entrevista")

        const val SQL_CREATE_TRANSCRIPTS =
            "CREATE TABLE IF NOT EXISTS `transcripts` (" +
                "`recordingId` INTEGER NOT NULL PRIMARY KEY, " +
                "`text` TEXT NOT NULL, " +
                "`provider` TEXT NOT NULL, " +
                "`model` TEXT NOT NULL, " +
                "`language` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`recordingId`) REFERENCES `recordings`(`id`) ON DELETE CASCADE)"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(SQL_CREATE_TRANSCRIPTS)
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "grabadora.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
