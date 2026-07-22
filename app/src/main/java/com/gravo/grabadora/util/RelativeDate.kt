package com.gravo.grabadora.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Fechas relativas como en la maqueta: "Ahora mismo", "Hoy 09:12", "Ayer 21:40", "20 jul".
 * Las plantillas de Hoy/Ayer/Ahora llegan por parámetro para poder localizarlas con recursos.
 */
object RelativeDate {
    fun format(
        timestamp: Long,
        now: Long,
        todayTemplate: String,
        yesterdayTemplate: String,
        nowLabel: String,
        locale: Locale = Locale("es"),
    ): String {
        if (now - timestamp < 60_000) return nowLabel
        val time = SimpleDateFormat("HH:mm", locale).format(Date(timestamp))
        val calNow = Calendar.getInstance().apply { timeInMillis = now }
        val calTs = Calendar.getInstance().apply { timeInMillis = timestamp }
        val sameDay = calNow.get(Calendar.YEAR) == calTs.get(Calendar.YEAR) &&
            calNow.get(Calendar.DAY_OF_YEAR) == calTs.get(Calendar.DAY_OF_YEAR)
        if (sameDay) return todayTemplate.format(time)
        calNow.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calNow.get(Calendar.YEAR) == calTs.get(Calendar.YEAR) &&
            calNow.get(Calendar.DAY_OF_YEAR) == calTs.get(Calendar.DAY_OF_YEAR)
        if (yesterday) return yesterdayTemplate.format(time)
        val sameYear = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.YEAR) == calTs.get(Calendar.YEAR)
        val pattern = if (sameYear) "d MMM" else "d MMM yyyy"
        return SimpleDateFormat(pattern, locale).format(Date(timestamp)).replace(".", "")
    }
}
