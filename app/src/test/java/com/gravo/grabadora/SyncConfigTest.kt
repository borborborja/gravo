package com.gravo.grabadora

import com.gravo.grabadora.sync.SyncConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncConfigTest {
    @Test
    fun `parseHost solo host usa puerto por defecto`() {
        assertEquals(Triple("nube.ejemplo.net", 22, ""), SyncConfig.parseHost("nube.ejemplo.net", 22))
    }

    @Test
    fun `parseHost con puerto`() {
        assertEquals(Triple("host", 2222, ""), SyncConfig.parseHost("host:2222", 22))
    }

    @Test
    fun `parseHost con esquema y ruta`() {
        assertEquals(
            Triple("host", 2222, "/subida/audios"),
            SyncConfig.parseHost("sftp://host:2222/subida/audios/", 22),
        )
        assertEquals(Triple("host", 21, "/pub"), SyncConfig.parseHost("ftp://host/pub", 21))
    }

    @Test
    fun `joinRemote evita dobles barras`() {
        assertEquals("a/b/c", SyncConfig.joinRemote("/a/", "/b", "c/"))
        assertEquals("Grabadora", SyncConfig.joinRemote("", "Grabadora"))
        assertEquals("", SyncConfig.joinRemote("", ""))
    }

    @Test
    fun `isConfigured requiere servidor`() {
        val base = SyncConfig(com.gravo.grabadora.data.settings.SyncProtocol.WEBDAV, "", "u", "p", "f")
        assertEquals(false, base.isConfigured)
        assertEquals(true, base.copy(server = "https://x").isConfigured)
    }
}
