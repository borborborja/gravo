package com.gravo.grabadora.ui.home

import com.gravo.grabadora.audio.RecStatus
import com.gravo.grabadora.data.settings.RecordStopMode

/** Acción de un toque corto en el botón grande de grabar. */
enum class RecordTapAction { START, PAUSE_TOGGLE, STOP }

/** Decide el comportamiento del botón grande según el estado y el modo configurado. JVM puro. */
object RecordTapLogic {
    fun tapAction(status: RecStatus, mode: RecordStopMode): RecordTapAction = when (status) {
        RecStatus.IDLE -> RecordTapAction.START
        RecStatus.RECORDING, RecStatus.PAUSED ->
            if (mode == RecordStopMode.TWO_BUTTONS) RecordTapAction.STOP else RecordTapAction.PAUSE_TOGGLE
    }

    /** El gesto de mantener solo está activo en el modo "mantener para parar". */
    fun holdEnabled(mode: RecordStopMode): Boolean = mode == RecordStopMode.HOLD
}
