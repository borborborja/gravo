package com.gravo.grabadora.ui.transcription

import java.text.Normalizer

/** Búsqueda de coincidencias insensible a mayúsculas y a acentos. JVM puro. */
object TranscriptionSearch {

    private fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()

    fun count(text: String, query: String): Int {
        val q = normalize(query)
        if (q.isEmpty()) return 0
        val t = normalize(text)
        var i = 0
        var c = 0
        while (true) {
            val idx = t.indexOf(q, i)
            if (idx < 0) break
            c++
            i = idx + q.length
        }
        return c
    }

    fun ranges(text: String, query: String): List<IntRange> {
        val q = normalize(query)
        if (q.isEmpty()) return emptyList()
        val t = normalize(text)
        val result = mutableListOf<IntRange>()
        var i = 0
        while (true) {
            val idx = t.indexOf(q, i)
            if (idx < 0) break
            result.add(idx until (idx + q.length))
            i = idx + q.length
        }
        return result
    }
}
