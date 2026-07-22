package com.gravo.grabadora

import android.app.Application
import com.gravo.grabadora.audio.RecordingController
import com.gravo.grabadora.data.RecordingRepository
import com.gravo.grabadora.data.db.AppDatabase
import com.gravo.grabadora.data.settings.SettingsRepository
import com.gravo.grabadora.sync.CryptoManager
import com.gravo.grabadora.sync.SyncManager

/** Inyección manual: raíz de dependencias de la app. */
class AppContainer(val app: Application) {
    val database: AppDatabase by lazy { AppDatabase.build(app) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(app) }
    val recordingRepository: RecordingRepository by lazy { RecordingRepository(app, database) }
    val recordingController: RecordingController by lazy { RecordingController(app, recordingRepository) }
    val cryptoManager: CryptoManager by lazy { CryptoManager(app) }
    val syncManager: SyncManager by lazy { SyncManager(app, settingsRepository, cryptoManager) }
}
