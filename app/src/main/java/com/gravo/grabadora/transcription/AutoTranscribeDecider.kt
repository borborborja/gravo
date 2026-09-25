package com.gravo.grabadora.transcription

/** Decide si la transcripción automática debe encolarse: solo con auto activo y clave presente. */
object AutoTranscribeDecider {
    fun shouldEnqueue(autoTranscribe: Boolean, hasKey: Boolean): Boolean = autoTranscribe && hasKey
}
