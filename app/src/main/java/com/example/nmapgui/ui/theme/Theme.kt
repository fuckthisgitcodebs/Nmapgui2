package com.example.nmapgui.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Green400,
    onPrimary = DarkBg,
    primaryContainer = DarkCard,
    onPrimaryContainer = Green400,
    secondary = Cyan400,
    onSecondary = DarkBg,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = Red400,
    onError = DarkBg,
)

private val LightColors = lightColorScheme(
    primary = Green700,
    onPrimary = LightBg,
    background = LightBg,
    surface = LightSurface,
    surfaceVariant = LightCard,
    outline = DarkBorder,
)

@Composable
fun NmapGuiTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
