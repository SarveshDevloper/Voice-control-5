package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NeonCyan = Color(0xFF00E5FF)
val ElectricPurple = Color(0xFF7C4DFF)
val DeepNavy = Color(0xFF0A0E21)
val SurfaceDark = Color(0xFF141A32)
val NeonGreen = Color(0xFF00E676)
val BrightCoral = Color(0xFFFF5252)

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = ElectricPurple,
    tertiary = NeonGreen,
    background = DeepNavy,
    surface = SurfaceDark,
    error = BrightCoral
)

private val LightColorScheme = lightColorScheme(
    primary = NeonCyan,
    secondary = ElectricPurple,
    tertiary = NeonGreen
)

@Composable
fun VoiceScreenLockTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
