package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CyberColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = CyberDarker,
    primaryContainer = CyberSurfaceVariant,
    onPrimaryContainer = NeonCyan,
    secondary = NeonPink,
    onSecondary = CyberDarker,
    secondaryContainer = CyberSurface,
    onSecondaryContainer = NeonPink,
    tertiary = NeonYellow,
    onTertiary = CyberDarker,
    background = CyberDark,
    onBackground = CyberTextPrimary,
    surface = CyberSurface,
    onSurface = CyberTextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = CyberTextSecondary
)

@Composable
fun CyberDashTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberColorScheme,
        typography = Typography,
        content = content
    )
}

// Backwards compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    CyberDashTheme(content = content)
}
