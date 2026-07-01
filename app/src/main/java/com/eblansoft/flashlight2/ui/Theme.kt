package com.eblansoft.flashlight2.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFD34D),
    onPrimary = Color(0xFF1A1A1A),
    secondary = Color(0xFFB44DFF),
    background = Color(0xFF0B0B0F),
    onBackground = Color(0xFFECECF1),
    surface = Color(0xFF16161C),
    onSurface = Color(0xFFECECF1),
    surfaceVariant = Color(0xFF222230),
    error = Color(0xFFFF5C5C),
)

@Composable
fun Flashlight2Theme(content: @Composable () -> Unit) {
    // «Еблан Софт» ships one theme: dark. It saves battery on the ad servers.
    MaterialTheme(colorScheme = DarkColors, content = content)
}
