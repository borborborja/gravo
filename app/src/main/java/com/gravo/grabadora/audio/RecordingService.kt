package com.gravo.grabadora.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gravo.grabadora.GrabadoraApp
import com.gravo.grabadora.MainActivity
import com.gravo.grabadora.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Servicio foreground de la grabación en curso: mantiene el proceso vivo y muestra la
 * notificación con cronómetro y acciones. La lógica de audio vive en RecordingController.
 */
class RecordingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val controller get() = (application as GrabadoraApp).container.recordingController
    private val settings get() = (application as GrabadoraApp).container.settingsRepository

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> controller.togglePause()
            ACTION_STOP -> {
                scope.launch {
                    controller.finishRecording(getString(R.string.recording_default_name))
                }
                return START_NOT_STICKY
            }
        }
        scope.launch {
            val hide = settings.settings.first().hideNotification
            createChannel(hide)
            val notification = buildNotification(RecStatus.RECORDING, 0L, hide)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIF_ID, notification)
            }
            observeState(hide)
        }
        return START_NOT_STICKY
    }

    private var observing = false
    private fun observeState(hide: Boolean) {
        if (observing) return
        observing = true
        scope.launch {
            combine(controller.status, controller.elapsedMs) { st, ms -> st to ms }
                .collect { (st, ms) ->
                    if (st != RecStatus.IDLE) {
                        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(NOTIF_ID, buildNotification(st, ms, hide))
                    }
                }
        }
    }

    private fun createChannel(hide: Boolean) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val importance = if (hide) NotificationManager.IMPORTANCE_MIN else NotificationManager.IMPORTANCE_LOW
        val id = if (hide) CHANNEL_HIDDEN else CHANNEL
        nm.createNotificationChannel(
            NotificationChannel(id, getString(R.string.notification_channel), importance).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            },
        )
    }

    private fun buildNotification(status: RecStatus, elapsedMs: Long, hide: Boolean): Notification {
        val channelId = if (hide) CHANNEL_HIDDEN else CHANNEL
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val pauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, RecordingService::class.java).setAction(ACTION_PAUSE),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, RecordingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (status == RecStatus.PAUSED) {
            getString(R.string.notification_paused)
        } else {
            getString(R.string.notification_recording)
        }
        val pauseLabel = if (status == RecStatus.PAUSED) {
            getString(R.string.notification_action_resume)
        } else {
            getString(R.string.notification_action_pause)
        }
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(com.gravo.grabadora.util.TimeFormat.mmss(elapsedMs / 1000))
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(contentIntent)
            .setPriority(if (hide) NotificationCompat.PRIORITY_MIN else NotificationCompat.PRIORITY_LOW)
            .addAction(0, pauseLabel, pauseIntent)
            .addAction(0, getString(R.string.notification_action_stop), stopIntent)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIF_ID = 1
        const val CHANNEL = "recording"
        const val CHANNEL_HIDDEN = "recording_hidden"
        const val ACTION_PAUSE = "com.gravo.grabadora.PAUSE"
        const val ACTION_STOP = "com.gravo.grabadora.STOP"
    }
}
