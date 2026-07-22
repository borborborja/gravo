package com.gravo.grabadora.sync

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpException
import java.io.File
import java.io.IOException

/** SFTP con JSch (fork mwiede). */
class SftpClient : SyncClient {
    private fun session(config: SyncConfig): Session {
        val (host, port, _) = SyncConfig.parseHost(config.server, 22)
        val session = JSch().getSession(config.user, host, port)
        session.setPassword(config.password)
        // sin known_hosts en la app; la confianza es del usuario al configurar el servidor
        session.setConfig("StrictHostKeyChecking", "no")
        session.timeout = 15_000
        session.connect()
        return session
    }

    override fun testConnection(config: SyncConfig) {
        val s = try {
            session(config)
        } catch (e: Exception) {
            throw IOException(e.message ?: "No se pudo conectar", e)
        }
        try {
            val channel = s.openChannel("sftp") as ChannelSftp
            channel.connect(10_000)
            channel.disconnect()
        } finally {
            s.disconnect()
        }
    }

    override fun upload(config: SyncConfig, file: File) {
        val (_, _, basePath) = SyncConfig.parseHost(config.server, 22)
        val s = session(config)
        try {
            val channel = s.openChannel("sftp") as ChannelSftp
            channel.connect(10_000)
            try {
                if (basePath.isNotEmpty()) channel.cd(basePath)
                val folder = SyncConfig.joinRemote(config.remoteFolder)
                if (folder.isNotEmpty()) {
                    for (segment in folder.split('/')) {
                        try {
                            channel.cd(segment)
                        } catch (_: SftpException) {
                            channel.mkdir(segment)
                            channel.cd(segment)
                        }
                    }
                }
                file.inputStream().use { channel.put(it, file.name) }
            } finally {
                channel.disconnect()
            }
        } catch (e: SftpException) {
            throw IOException(e.message ?: "Error SFTP", e)
        } finally {
            s.disconnect()
        }
    }
}
