package com.gravo.grabadora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gravo.grabadora.ui.theme.Accent
import com.gravo.grabadora.ui.theme.DmMono
import com.gravo.grabadora.ui.theme.DmSans
import com.gravo.grabadora.ui.theme.GrabadoraTheme

/** Botón segmentado/chip de la maqueta: borde naranja + fondo translúcido al activarse. */
@Composable
fun SegChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mono: Boolean = true,
    horizontalPadding: Int = 12,
    verticalPadding: Int = 9,
) {
    val colors = GrabadoraTheme.colors
    Box(
        modifier = modifier
            .background(
                if (active) Accent.copy(alpha = 0.14f) else colors.sf1,
                RoundedCornerShape(9.dp),
            )
            .border(1.dp, if (active) Accent else colors.bd4, RoundedCornerShape(9.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = horizontalPadding.dp, vertical = verticalPadding.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = TextStyle(
                fontFamily = if (mono) DmMono else DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = if (active) Accent else colors.fg2,
            ),
        )
    }
}

/** Chip de filtro/etiqueta (más redondeado, texto DM Sans). */
@Composable
fun TagChip(label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = GrabadoraTheme.colors
    Box(
        modifier = modifier
            .background(if (active) Accent.copy(alpha = 0.14f) else colors.sf1, RoundedCornerShape(8.dp))
            .border(1.dp, if (active) Accent else colors.bd4, RoundedCornerShape(8.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            style = TextStyle(
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = if (active) Accent else colors.fg3,
            ),
        )
    }
}

/** Toggle iOS-style de la maqueta: pista 44×26, bola clara. */
@Composable
fun GravoSwitch(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val colors = GrabadoraTheme.colors
    Box(
        modifier = modifier
            .size(width = 44.dp, height = 26.dp)
            .background(if (checked) Accent else colors.bd5, RoundedCornerShape(13.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggle)
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(20.dp).background(colors.fg1, CircleShape))
    }
}
