package com.gravo.grabadora.sync

import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Cliente WebDAV mínimo sobre OkHttp: OPTIONS (probar), MKCOL (carpeta) y PUT (subir). */
class WebDavClient : SyncClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun baseUrl(config: SyncConfig): String {
        var url = config.server.trim().trimEnd('/')
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
        url.toHttpUrlOrNull() ?: throw IOException("URL no válida: ${config.server}")
        return url
    }

    private fun auth(config: SyncConfig): String = Credentials.basic(config.user, config.password)

    override fun testConnection(config: SyncConfig) {
        val request = Request.Builder()
            .url(baseUrl(config))
            .method("OPTIONS", null)
            .header("Authorization", auth(config))
            .build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val dav = resp.header("DAV")
                ?: throw IOException("El servidor no anuncia soporte WebDAV")
            if (!dav.contains("1")) throw IOException("Soporte WebDAV inesperado: $dav")
        }
    }

    override fun upload(config: SyncConfig, file: File) {
        val base = baseUrl(config)
        val folder = SyncConfig.joinRemote(config.remoteFolder)
        if (folder.isNotEmpty()) {
            var path = base
            for (segment in folder.split('/')) {
                path = "$path/$segment"
                val mkcol = Request.Builder()
                    .url(path)
                    .method("MKCOL", null)
                    .header("Authorization", auth(config))
                    .build()
                http.newCall(mkcol).execute().use { resp ->
                    // 201 creada, 405 ya existe: ambas valen
                    if (!resp.isSuccessful && resp.code != 405) {
                        throw IOException("MKCOL $segment: HTTP ${resp.code}")
                    }
                }
            }
        }
        val target = if (folder.isEmpty()) "$base/${file.name}" else "$base/$folder/${file.name}"
        val put = Request.Builder()
            .url(target)
            .put(file.asRequestBody("application/octet-stream".toMediaType()))
            .header("Authorization", auth(config))
            .build()
        http.newCall(put).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("PUT: HTTP ${resp.code}")
        }
    }
}
