package com.gravo.grabadora

import com.gravo.grabadora.audio.RecStatus
import com.gravo.grabadora.data.settings.RecordStopMode
import com.gravo.grabadora.ui.home.RecordTapAction
import com.gravo.grabadora.ui.home.RecordTapLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordTapLogicTest {

    @Test
    fun `idle inicia en ambos modos`() {
        assertEquals(RecordTapAction.START, RecordTapLogic.tapAction(RecStatus.IDLE, RecordStopMode.TWO_BUTTONS))
        assertEquals(RecordTapAction.START, RecordTapLogic.tapAction(RecStatus.IDLE, RecordStopMode.HOLD))
    }

    @Test
    fun `dos botones para grabando y en pausa`() {
        assertEquals(RecordTapAction.STOP, RecordTapLogic.tapAction(RecStatus.RECORDING, RecordStopMode.TWO_BUTTONS))
        assertEquals(RecordTapAction.STOP, RecordTapLogic.tapAction(RecStatus.PAUSED, RecordStopMode.TWO_BUTTONS))
    }

    @Test
    fun `mantener alterna pausa`() {
        assertEquals(RecordTapAction.PAUSE_TOGGLE, RecordTapLogic.tapAction(RecStatus.RECORDING, RecordStopMode.HOLD))
        assertEquals(RecordTapAction.PAUSE_TOGGLE, RecordTapLogic.tapAction(RecStatus.PAUSED, RecordStopMode.HOLD))
    }

    @Test
    fun `el gesto de mantener solo esta activo en modo hold`() {
        assertTrue(RecordTapLogic.holdEnabled(RecordStopMode.HOLD))
        assertFalse(RecordTapLogic.holdEnabled(RecordStopMode.TWO_BUTTONS))
    }
}
