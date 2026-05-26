package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CosmicColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color.White,
    secondary = NeonCyan,
    onSecondary = Color.White,
    tertiary = NeonPink,
    background = CosmicBackground,
    onBackground = TextWhite,
    surface = CosmicSurface,
    onSurface = TextWhite,
    surfaceVariant = CosmicSurfaceVariant,
    onSurfaceVariant = TextGray,
    error = NeonPink,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark theme for gorgeous premium design
    dynamicColor: Boolean = false, // Preserve our beautiful neon colors instead of dynamic
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CosmicColorScheme,
        typography = Typography,
        content = content
    )
}
