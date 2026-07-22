package com.gravo.grabadora

import com.gravo.grabadora.ui.library.LibraryFilter
import com.gravo.grabadora.ui.library.LibraryItem
import com.gravo.grabadora.ui.library.SortBy
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryFilterTest {
    private val items = listOf(
        LibraryItem(1, "Reunión de equipo", 400, 12 * 60_000L, "WAV", listOf("Trabajo")),
        LibraryItem(2, "Idea para el pódcast", 300, 3 * 60_000L, "M4A", listOf("Ideas", "Pódcast")),
        LibraryItem(3, "Ensayo — guitarra", 200, 7 * 60_000L, "FLAC", listOf("Música")),
        LibraryItem(4, "Nota de voz", 100, 47_000L, "MP3", listOf("Personal")),
    )

    @Test
    fun `orden por recientes`() {
        val result = LibraryFilter.apply(items, "", LibraryFilter.ALL_TAG, SortBy.RECENT)
        assertEquals(listOf(1L, 2L, 3L, 4L), result.map { it.id })
    }

    @Test
    fun `orden por nombre`() {
        val result = LibraryFilter.apply(items, "", LibraryFilter.ALL_TAG, SortBy.NAME)
        assertEquals(listOf(3L, 2L, 4L, 1L), result.map { it.id })
    }

    @Test
    fun `orden por duracion descendente`() {
        val result = LibraryFilter.apply(items, "", LibraryFilter.ALL_TAG, SortBy.DURATION)
        assertEquals(listOf(1L, 3L, 2L, 4L), result.map { it.id })
    }

    @Test
    fun `filtro por etiqueta`() {
        val result = LibraryFilter.apply(items, "", "Pódcast", SortBy.RECENT)
        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun `busqueda insensible a mayusculas en nombre y etiquetas`() {
        assertEquals(listOf(1L), LibraryFilter.apply(items, "reunión", LibraryFilter.ALL_TAG, SortBy.RECENT).map { it.id })
        assertEquals(listOf(3L), LibraryFilter.apply(items, "música", LibraryFilter.ALL_TAG, SortBy.RECENT).map { it.id })
    }

    @Test
    fun `busqueda y filtro combinados`() {
        val result = LibraryFilter.apply(items, "idea", "Ideas", SortBy.RECENT)
        assertEquals(listOf(2L), result.map { it.id })
    }
}
