package com.gravo.grabadora.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.gravo.grabadora.ui.theme.Accent
import com.gravo.grabadora.ui.theme.WaveGreenDim
import com.gravo.grabadora.ui.theme.Yellow

/**
 * Forma de onda del reproductor: barras redondeadas, naranjas hasta la posición
 * reproducida, línea amarilla de posición; tocar/arrastrar busca.
 */
@Composable
fun WaveformView(
    peaks: FloatArray,
    progress: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 56,
    onScrub: ((Float) -> Unit)? = null,
) {
    val scrubModifier = if (onScrub != null) {
        modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    event.changes.firstOrNull()?.let { change ->
                        if (change.pressed) {
                            onScrub((change.position.x / size.width).coerceIn(0f, 1f))
                        }
                    }
                }
            }
        }
    } else {
        modifier
    }
    Canvas(scrubModifier) {
        if (peaks.isEmpty()) return@Canvas
        val n = barCount
        val gap = 2f * density
        val barWidth = (size.width - (n - 1) * gap) / n
        for (i in 0 until n) {
            val peakIdx = (i * peaks.size / n).coerceIn(0, peaks.size - 1)
            // pico máximo del tramo que representa esta barra
            var v = 0f
            val next = ((i + 1) * peaks.size / n).coerceAtMost(peaks.size)
            for (j in peakIdx until next) if (peaks[j] > v) v = peaks[j]
            val h = (v * size.height).coerceAtLeast(3f * density)
            val played = i.toFloat() / n < progress
            drawRoundRect(
                color = if (played) Accent else WaveGreenDim,
                topLeft = Offset(i * (barWidth + gap), (size.height - h) / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
        val x = progress * size.width
        drawLine(Yellow, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2f * density)
    }
}

/** Onda estática con zonas atenuadas fuera de [dimStart, dimEnd] (editor). */
@Composable
fun EditorWaveform(
    peaks: FloatArray,
    dimColor: Color,
    dimStart: Float,
    dimEnd: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 64,
) {
    Canvas(modifier) {
        if (peaks.isEmpty()) return@Canvas
        val n = barCount
        val gap = 2f * density
        val barWidth = (size.width - (n - 1) * gap) / n
        for (i in 0 until n) {
            val from = (i * peaks.size / n).coerceIn(0, peaks.size - 1)
            val to = ((i + 1) * peaks.size / n).coerceAtMost(peaks.size)
            var v = 0f
            for (j in from until to) if (peaks[j] > v) v = peaks[j]
            val h = (v * size.height).coerceAtLeast(3f * density)
            drawRoundRect(
                color = com.gravo.grabadora.ui.theme.WaveGreen,
                topLeft = Offset(i * (barWidth + gap), (size.height - h) / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
        // zonas atenuadas fuera de la selección
        if (dimStart > 0f) {
            drawRect(dimColor, topLeft = Offset.Zero, size = Size(dimStart * size.width, size.height))
        }
        if (dimEnd < 1f) {
            drawRect(dimColor, topLeft = Offset(dimEnd * size.width, 0f), size = Size((1f - dimEnd) * size.width, size.height))
        }
    }
}
