package com.gravo.grabadora.transcription

import com.gravo.grabadora.audio.RecordFormat

/** Datos mínimos de una grabación necesarios para transcribirla. */
data class RecordingInfo(
    val path: String,
    val format: RecordFormat,
    val durationMs: Long,
)
