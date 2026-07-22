package com.gravo.grabadora.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

data class MicOption(val id: Int, val name: String)

object MicSelector {
    const val DEFAULT_ID = -1

    fun availableMics(context: Context): List<MicOption> {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.getDevices(AudioManager.GET_DEVICES_INPUTS)
            .filter { it.type != AudioDeviceInfo.TYPE_TELEPHONY }
            .map { MicOption(it.id, describe(it)) }
            .distinctBy { it.name }
    }

    fun deviceById(context: Context, id: Int): AudioDeviceInfo? {
        if (id == DEFAULT_ID) return null
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.getDevices(AudioManager.GET_DEVICES_INPUTS).firstOrNull { it.id == id }
    }

    private fun describe(d: AudioDeviceInfo): String = when (d.type) {
        AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Micrófono integrado"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Auriculares con cable"
        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "Micrófono USB"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth"
        else -> d.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Entrada ${d.id}"
    }
}
