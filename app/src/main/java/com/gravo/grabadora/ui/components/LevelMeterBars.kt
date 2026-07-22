package com.gravo.grabadora.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.gravo.grabadora.ui.theme.Accent
import com.gravo.grabadora.ui.theme.RecordRed
import com.gravo.grabadora.ui.theme.WaveGreen

/**
 * Barras del audímetro (maqueta: centradas verticalmente, redondeadas; verde-grisáceo,
 * naranja >0.6, rojo >0.86).
 */
@Composable
fun LevelMeterBars(levels: FloatArray, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val n = levels.size
        if (n == 0) return@Canvas
        val gap = 3f * density
        val barWidth = (size.width - (n - 1) * gap) / n
        for (i in 0 until n) {
            val v = levels[i]
            val h = (v * size.height).coerceAtLeast(2f * density)
            val color = when {
                v > 0.86f -> RecordRed
                v > 0.6f -> Accent
                else -> WaveGreen
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barWidth + gap), (size.height - h) / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}
