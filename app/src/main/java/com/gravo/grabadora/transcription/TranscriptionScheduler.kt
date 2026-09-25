package com.gravo.grabadora.transcription

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/** Encola la transcripción automática en segundo plano (con red y reintentos). */
class TranscriptionScheduler(private val context: Context) {
    fun enqueueAuto(recordingId: Long) {
        val request = OneTimeWorkRequestBuilder<TranscriptionWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(workDataOf(TranscriptionWorker.KEY_RECORDING_ID to recordingId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "transcription-$recordingId",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
