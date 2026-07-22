package com.gravo.grabadora.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.gravo.grabadora.data.settings.AppSettings
import com.gravo.grabadora.data.settings.SettingsRepository
import com.gravo.grabadora.data.settings.SyncProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class SyncManager(
    private val context: Context,
    private val settings: SettingsRepository,
    private val crypto: CryptoManager,
) {
    fun clientFor(protocol: SyncProtocol): SyncClient = when (protocol) {
        SyncProtocol.WEBDAV -> WebDavClient()
        SyncProtocol.FTP -> FtpClient()
        SyncProtocol.SFTP -> SftpClient()
    }

    suspend fun currentConfig(): SyncConfig {
        val s = settings.settings.first()
        return config(s)
    }

    suspend fun config(s: AppSettings): SyncConfig = SyncConfig(
        protocol = s.syncProtocol,
        server = s.syncServer,
        user = s.syncUser,
        password = crypto.decrypt(settings.syncPasswordEnc.first()),
        remoteFolder = s.syncFolder,
    )

    suspend fun savePassword(plain: String) {
        settings.setSyncPasswordEnc(if (plain.isEmpty()) "" else crypto.encrypt(plain))
    }

    /** Prueba la conexión con la configuración actual; lanza excepción con mensaje legible. */
    suspend fun testConnection() = withContext(Dispatchers.IO) {
        val config = currentConfig()
        if (!config.isConfigured) throw IllegalStateException("Configura el servidor primero")
        clientFor(config.protocol).testConnection(config)
    }

    /** Encola la subida en segundo plano (con red y reintentos). */
    fun enqueueUpload(file: File) {
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(workDataOf(UploadWorker.KEY_PATH to file.absolutePath))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    /** Subida síncrona usada por el worker. */
    suspend fun uploadNow(file: File) = withContext(Dispatchers.IO) {
        val config = currentConfig()
        if (!config.isConfigured) return@withContext
        clientFor(config.protocol).upload(config, file)
    }
}
