package com.gravo.grabadora.util

import kotlin.math.abs

object TimeFormat {
    /** MM:SS (minutos sin límite, p.ej. 75:03). */
    fun mmss(totalSeconds: Long): String {
        val s = if (totalSeconds < 0) 0 else totalSeconds
        return "%02d:%02d".format(s / 60, s % 60)
    }

    fun mmss(millis: Int): String = mmss(millis / 1000L)

    /** MM:SS,d con décimas y coma decimal (formato de la maqueta). */
    fun fine(millis: Long): String {
        val ms = if (millis < 0) 0 else millis
        val tenths = (ms / 100) % 10
        return mmss(ms / 1000) + "," + tenths
    }

    /** Segundos con signo para los botones de seek: "−5s" / "+5s". */
    fun seekLabel(seconds: Int): String =
        (if (seconds < 0) "−" else "+") + "${abs(seconds)}s"

    /** Velocidad "1,5×" con coma decimal. */
    fun speed(speed: Float): String {
        val txt = if (speed == speed.toInt().toFloat()) "${speed.toInt()}" else "$speed".replace('.', ',')
        return "$txt×"
    }
}
