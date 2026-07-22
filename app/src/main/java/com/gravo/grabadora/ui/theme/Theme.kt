package com.gravo.grabadora.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

object GrabadoraTheme {
    val colors: GrabadoraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalGrabadoraColors.current
}

@Composable
fun GrabadoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = Accent,
            onPrimary = colors.onAccent,
            secondary = Yellow,
            background = colors.bg,
            onBackground = colors.fg1,
            surface = colors.bg,
            onSurface = colors.fg1,
            outline = colors.bd4,
        )
    } else {
        lightColorScheme(
            primary = Accent,
            onPrimary = colors.onAccent,
            secondary = Yellow,
            background = colors.bg,
            onBackground = colors.fg1,
            surface = colors.bg,
            onSurface = colors.fg1,
            outline = colors.bd4,
        )
    }
    CompositionLocalProvider(LocalGrabadoraColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
