package com.gravo.grabadora

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StringsParityTest {

    private val nameRegex = Regex("""<string\s+name="([^"]+)"""")

    private fun resolveFile(relative: String): File {
        val candidates = listOf(
            File("app/$relative"),
            File(relative),
        )
        return candidates.firstOrNull { it.isFile }
            ?: throw AssertionError(
                "No se encontró $relative (probado: ${candidates.joinToString { it.path }})",
            )
    }

    private fun extractNames(file: File): Set<String> =
        file.readText()
            .let { nameRegex.findAll(it) }
            .map { it.groupValues[1] }
            .toSet()

    @Test
    fun `paridad de nombres entre ES y EN`() {
        val es = resolveFile("src/main/res/values/strings.xml")
        val en = resolveFile("src/main/res/values-en/strings.xml")

        val esNames = extractNames(es)
        val enNames = extractNames(en)

        assertTrue("El fichero ES no contiene ninguna cadena", esNames.isNotEmpty())
        assertTrue("El fichero EN no contiene ninguna cadena", enNames.isNotEmpty())

        val soloEs = (esNames - enNames).sorted()
        val soloEn = (enNames - esNames).sorted()

        assertEquals(
            "Faltan claves en EN: $soloEn",
            emptyList<String>(),
            soloEs,
        )
        assertEquals(
            "Faltan claves en ES: $soloEs",
            emptyList<String>(),
            soloEn,
        )
    }
}
