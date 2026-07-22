package com.gravo.grabadora.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFFFF6B35)
val Yellow = Color(0xFFFFD23F)
val RecordRed = Color(0xFFFF3B30)
val WaveGreen = Color(0xFF5A7D78)
val WaveGreenDim = Color(0xFF3A4A47)

/** Paleta de la maqueta: superficies y bordes por niveles, tinta por jerarquía. */
@Immutable
data class GrabadoraColors(
    val isDark: Boolean,
    val bg: Color,
    val sf1: Color,
    val sf2: Color,
    val sf3: Color,
    val bd1: Color,
    val bd2: Color,
    val bd3: Color,
    val bd4: Color,
    val bd5: Color,
    val fg1: Color,
    val fg2: Color,
    val fg3: Color,
    val fg4: Color,
    val dim: Color,
    val onAccent: Color,
)

val DarkColors = GrabadoraColors(
    isDark = true,
    bg = Color(0xFF08080C),
    sf1 = Color.White.copy(alpha = 0.03f),
    sf2 = Color.White.copy(alpha = 0.04f),
    sf3 = Color.White.copy(alpha = 0.05f),
    bd1 = Color.White.copy(alpha = 0.06f),
    bd2 = Color.White.copy(alpha = 0.07f),
    bd3 = Color.White.copy(alpha = 0.08f),
    bd4 = Color.White.copy(alpha = 0.10f),
    bd5 = Color.White.copy(alpha = 0.14f),
    fg1 = Color(0xFFF0EDE8),
    fg2 = Color(0xFFC8C4BE),
    fg3 = Color(0xFF8A8580),
    fg4 = Color(0xFF4A4642),
    dim = Color(0xFF08080C).copy(alpha = 0.72f),
    onAccent = Color(0xFF08080C),
)

val LightColors = GrabadoraColors(
    isDark = false,
    bg = Color(0xFFF4F1EA),
    sf1 = Color.White,
    sf2 = Color.White,
    sf3 = Color(0xFFFBF9F4),
    bd1 = Color.Black.copy(alpha = 0.05f),
    bd2 = Color.Black.copy(alpha = 0.07f),
    bd3 = Color.Black.copy(alpha = 0.09f),
    bd4 = Color.Black.copy(alpha = 0.12f),
    bd5 = Color.Black.copy(alpha = 0.16f),
    fg1 = Color(0xFF1A1A1A),
    fg2 = Color(0xFF444240),
    fg3 = Color(0xFF7A7572),
    fg4 = Color(0xFFC2BEB8),
    dim = Color(0xFFF4F1EA).copy(alpha = 0.74f),
    onAccent = Color(0xFF08080C),
)

val LocalGrabadoraColors = staticCompositionLocalOf { DarkColors }
