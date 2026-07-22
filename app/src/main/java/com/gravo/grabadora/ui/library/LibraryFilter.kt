package com.gravo.grabadora.ui.library

/** Datos mínimos para ordenar/filtrar/buscar; desacoplado de Room para testear en JVM. */
data class LibraryItem(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val durationMs: Long,
    val format: String,
    val tags: List<String>,
)

enum class SortBy { RECENT, NAME, DURATION }

object LibraryFilter {
    const val ALL_TAG = "__all__"

    fun apply(items: List<LibraryItem>, query: String, tag: String, sortBy: SortBy): List<LibraryItem> {
        var result = items
        if (tag != ALL_TAG) result = result.filter { tag in it.tags }
        val q = query.trim()
        if (q.isNotEmpty()) {
            result = result.filter {
                it.name.contains(q, ignoreCase = true) || it.tags.any { t -> t.contains(q, ignoreCase = true) }
            }
        }
        return when (sortBy) {
            SortBy.RECENT -> result.sortedByDescending { it.createdAt }
            SortBy.NAME -> result.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            SortBy.DURATION -> result.sortedByDescending { it.durationMs }
        }
    }
}
