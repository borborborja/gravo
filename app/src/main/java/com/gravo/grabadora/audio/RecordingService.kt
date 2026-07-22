package com.gravo.grabadora.audio

import android.app.Service
import android.content.Intent
import android.os.IBinder

/** Servicio foreground de grabación (se completa en la fase de grabación). */
class RecordingService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
