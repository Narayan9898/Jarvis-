package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = Color(0xFF00222A),
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = JarvisCyanBright,
    secondary = JarvisElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF133678),
    onSecondaryContainer = Color(0xFFD4E3FF),
    tertiary = JarvisCyanBright,
    onTertiary = Color(0xFF00363F),
    background = JarvisBackground,
    onBackground = JarvisTextPrimary,
    surface = JarvisSurface,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisSurfaceVariant,
    onSurfaceVariant = JarvisTextSecondary,
    outline = Color(0xFF1F3559),
    outlineVariant = Color(0xFF13223A),
    error = JarvisRed,
    onError = Color.White
)

@Composable
fun JarvisTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
