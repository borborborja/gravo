package com.gravo.grabadora.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gravo.grabadora.GrabadoraApp
import java.io.File

/** Sube una grabación al servidor configurado; reintenta con backoff si falla. */
class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val path = inputData.getString(KEY_PATH) ?: return Result.failure()
        val file = File(path)
        if (!file.exists()) return Result.failure()
        val sync = (applicationContext as GrabadoraApp).container.syncManager
        return try {
            sync.uploadNow(file)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_PATH = "path"
    }
}
