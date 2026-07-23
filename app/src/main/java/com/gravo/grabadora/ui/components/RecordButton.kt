package com.gravo.grabadora.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.gravo.grabadora.audio.RecStatus
import com.gravo.grabadora.ui.theme.Accent
import com.gravo.grabadora.ui.theme.GrabadoraTheme
import com.gravo.grabadora.ui.theme.RecordRed
import com.gravo.grabadora.ui.theme.Yellow
import kotlinx.coroutines.delay

/**
 * Botón de grabar de la maqueta: 118 dp, anillo de progreso que se rellena al mantener
 * pulsado 850 ms (= finalizar). Toque corto: iniciar / pausar / reanudar.
 */
@Composable
fun RecordButton(
    status: RecStatus,
    onTap: () -> Unit,
    onHoldComplete: () -> Unit,
    onPressingChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = GrabadoraTheme.colors
    var pressing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var holdFired by remember { mutableStateOf(false) }

    // el anillo solo se rellena si ya hay grabación en curso (idle: toque simple)
    val holdEnabled = status != RecStatus.IDLE

    LaunchedEffect(pressing, holdEnabled) {
        onPressingChange(pressing && holdEnabled)
        if (pressing && holdEnabled) {
            holdFired = false
            val start = System.currentTimeMillis()
            while (pressing) {
                val elapsed = System.currentTimeMillis() - start
                progress = (elapsed / HOLD_MS).coerceAtMost(1f)
                if (progress >= 1f) {
                    holdFired = true
                    onHoldComplete()
                    break
                }
                delay(16)
            }
            progress = 0f
        } else {
            progress = 0f
        }
    }

    Box(
        modifier = modifier
            .size(118.dp)
            .pointerInput(status) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent()
                        if (down.changes.any { it.pressedChange() }) {
                            pressing = true
                            var released = false
                            while (!released) {
                                val ev = awaitPointerEvent()
                                if (ev.changes.all { !it.pressed }) released = true
                            }
                            val wasHold = holdFired
                            pressing = false
                            if (!wasHold) onTap()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val radius = (size.minDimension - stroke) / 2f
            drawCircle(color = colors.bd3, radius = radius, style = Stroke(stroke))
            if (progress > 0f) {
                drawArc(
                    color = Accent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                )
            }
        }
        // disco interior: rojo (listo/pausa) o amarillo (grabando, muestra pausa)
        Box(
            Modifier
                .padding(12.dp)
                .fillMaxSize()
                .background(if (status == RecStatus.RECORDING) Yellow else RecordRed, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when (status) {
                RecStatus.RECORDING -> Row {
                    Box(Modifier.size(width = 9.dp, height = 34.dp).background(Color(0xFF08080C), RoundedCornerShape(2.dp)))
                    Box(Modifier.width(8.dp))
                    Box(Modifier.size(width = 9.dp, height = 34.dp).background(Color(0xFF08080C), RoundedCornerShape(2.dp)))
                }
                RecStatus.PAUSED -> Box(Modifier.size(34.dp).background(Color(0xFF08080C), CircleShape))
                RecStatus.IDLE -> {} // disco rojo liso = grabar
            }
        }
    }
}

private fun androidx.compose.ui.input.pointer.PointerInputChange.pressedChange(): Boolean =
    pressed && previousPressed != pressed

private const val HOLD_MS = 850f
