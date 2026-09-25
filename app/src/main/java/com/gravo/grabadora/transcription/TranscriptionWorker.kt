package com.gravo.grabadora.transcription

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gravo.grabadora.GrabadoraApp
import kotlinx.coroutines.flow.first

/** Transcribe automáticamente una grabación; reintenta con backoff si falla. */
class TranscriptionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val recordingId = inputData.getLong(KEY_RECORDING_ID, -1L)
        if (recordingId < 0) return Result.failure()
        val container = (applicationContext as GrabadoraApp).container
        val provider = container.settingsRepository.settings.first().transcriptionProvider
        if (!container.transcriptionManager.hasKey(provider)) return Result.success()
        return runCatching { container.transcriptionManager.transcribe(recordingId, null) }
            .fold(
                onSuccess = { Result.success() },
                onFailure = {
                    if (runAttemptCount < 3) Result.retry() else Result.failure()
                },
            )
    }

    companion object {
        const val KEY_RECORDING_ID = "recording_id"
    }
}
