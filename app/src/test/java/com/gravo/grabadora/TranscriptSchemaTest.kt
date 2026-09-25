package com.gravo.grabadora

import com.gravo.grabadora.data.db.AppDatabase
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptSchemaTest {

    @Test
    fun `crea la tabla transcripts`() {
        assertTrue(AppDatabase.SQL_CREATE_TRANSCRIPTS.contains("CREATE TABLE IF NOT EXISTS `transcripts`"))
    }

    @Test
    fun `define las siete columnas`() {
        val sql = AppDatabase.SQL_CREATE_TRANSCRIPTS
        for (col in listOf("recordingId", "text", "provider", "model", "language", "createdAt", "updatedAt")) {
            assertTrue("falta la columna `$col`", sql.contains("`$col`"))
        }
    }

    @Test
    fun `declara clave foranea con borrado en cascada`() {
        val sql = AppDatabase.SQL_CREATE_TRANSCRIPTS
        assertTrue(sql.contains("FOREIGN KEY"))
        assertTrue(sql.contains("CASCADE"))
    }
}
