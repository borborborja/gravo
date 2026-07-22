package com.gravo.grabadora.sync

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient as ApacheFtp
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.IOException

/** FTP con commons-net. */
class FtpClient : SyncClient {
    private fun connect(config: SyncConfig): ApacheFtp {
        val (host, port, basePath) = SyncConfig.parseHost(config.server, 21)
        val ftp = ApacheFtp()
        ftp.connectTimeout = 15_000
        ftp.connect(host, port)
        if (!FTPReply.isPositiveCompletion(ftp.replyCode)) {
            ftp.disconnect()
            throw IOException("El servidor rechazó la conexión (${ftp.replyCode})")
        }
        if (!ftp.login(config.user.ifEmpty { "anonymous" }, config.password)) {
            ftp.disconnect()
            throw IOException("Usuario o contraseña incorrectos")
        }
        ftp.enterLocalPassiveMode()
        ftp.setFileType(FTP.BINARY_FILE_TYPE)
        if (basePath.isNotEmpty() && !ftp.changeWorkingDirectory(basePath)) {
            throw IOException("No existe la ruta $basePath")
        }
        return ftp
    }

    override fun testConnection(config: SyncConfig) {
        val ftp = connect(config)
        runCatching { ftp.logout() }
        ftp.disconnect()
    }

    override fun upload(config: SyncConfig, file: File) {
        val ftp = connect(config)
        try {
            val folder = SyncConfig.joinRemote(config.remoteFolder)
            if (folder.isNotEmpty()) {
                for (segment in folder.split('/')) {
                    if (!ftp.changeWorkingDirectory(segment)) {
                        if (!ftp.makeDirectory(segment) || !ftp.changeWorkingDirectory(segment)) {
                            throw IOException("No se pudo crear la carpeta $segment")
                        }
                    }
                }
            }
            file.inputStream().use {
                if (!ftp.storeFile(file.name, it)) throw IOException("Fallo al subir (${ftp.replyString.trim()})")
            }
        } finally {
            runCatching { ftp.logout() }
            runCatching { ftp.disconnect() }
        }
    }
}
