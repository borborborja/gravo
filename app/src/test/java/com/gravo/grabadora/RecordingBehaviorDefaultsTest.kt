package com.gravo.grabadora

import com.gravo.grabadora.data.settings.AppSettings
import com.gravo.grabadora.data.settings.RecordStopMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RecordingBehaviorDefaultsTest {

    @Test
    fun `el audiometro no tiene vista previa por defecto`() {
        assertFalse(AppSettings().meterPreview)
    }

    @Test
    fun `el modo de parada por defecto es dos botones`() {
        assertEquals(RecordStopMode.TWO_BUTTONS, AppSettings().recordStopMode)
    }
}
