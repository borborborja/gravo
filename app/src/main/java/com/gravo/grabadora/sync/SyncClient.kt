package com.gravo.grabadora.sync

import com.gravo.grabadora.data.settings.SyncProtocol
import java.io.File

data class SyncConfig(
    val protocol: SyncProtocol,
    val server: String,
    val user: String,
    val password: String,
    val remoteFolder: String,
) {
    val isConfigured: Boolean get() = server.isNotBlank()

    companion object {
        /**
         * "host", "host:2222" o "sftp://host:2222/ruta" → (host, puerto, rutaBase).
         * JVM puro, testeable.
         */
        fun parseHost(server: String, defaultPort: Int): Triple<String, Int, String> {
            var s = server.trim()
            for (scheme in listOf("sftp://", "ftp://", "ftps://")) {
                if (s.startsWith(scheme, ignoreCase = true)) s = s.substring(scheme.length)
            }
            val slash = s.indexOf('/')
            val basePath = if (slash >= 0) s.substring(slash).trimEnd('/') else ""
            val hostPort = if (slash >= 0) s.substring(0, slash) else s
            val colon = hostPort.lastIndexOf(':')
            return if (colon > 0 && hostPort.substring(colon + 1).toIntOrNull() != null) {
                Triple(hostPort.substring(0, colon), hostPort.substring(colon + 1).toInt(), basePath)
            } else {
                Triple(hostPort, defaultPort, basePath)
            }
        }

        /** Une segmentos de ruta remota sin dobles barras. JVM puro, testeable. */
        fun joinRemote(vararg segments: String): String =
            segments.filter { it.isNotBlank() }
                .joinToString("/") { it.trim('/') }
    }
}

interface SyncClient {
    /** Lanza IOException/Exception con mensaje legible si falla. */
    fun testConnection(config: SyncConfig)

    /** Sube [file] a la carpeta remota configurada (creándola si no existe). */
    fun upload(config: SyncConfig, file: File)
}
