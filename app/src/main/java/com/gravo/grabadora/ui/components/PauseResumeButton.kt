package com.gravo.grabadora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gravo.grabadora.R
import com.gravo.grabadora.audio.RecStatus
import com.gravo.grabadora.ui.theme.GrabadoraTheme

/**
 * Botón pequeño de pausa/reanudar para el modo de dos botones:
 * mientras se graba muestra "pausa" y en pausa muestra "reanudar".
 */
@Composable
fun PauseResumeButton(
    status: RecStatus,
    onPause: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GrabadoraTheme.colors
    val resume = status == RecStatus.PAUSED
    Box(
        modifier = modifier
            .size(52.dp)
            .background(colors.sf2, CircleShape)
            .border(1.dp, colors.bd3, CircleShape)
            .clickable(remember { MutableInteractionSource() }, null) { if (resume) onResume() else onPause() },
        contentAlignment = Alignment.Center,
    ) {
        if (resume) {
            Icon(Icons.Filled.PlayArrow, stringResource(R.string.cd_resume_small), Modifier.size(26.dp), tint = colors.fg2)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 5.dp, height = 18.dp).background(colors.fg2, RoundedCornerShape(1.dp)))
                Box(Modifier.width(6.dp))
                Box(Modifier.size(width = 5.dp, height = 18.dp).background(colors.fg2, RoundedCornerShape(1.dp)))
            }
        }
    }
}
