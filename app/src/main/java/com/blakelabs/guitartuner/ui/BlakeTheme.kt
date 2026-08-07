package com.blakelabs.guitartuner.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object BlakeColors {
    val Background = Color(0xFF070908)
    val Surface = Color(0xFF0D110F)
    val SurfaceRaised = Color(0xFF121713)
    val Border = Color(0xFF263029)
    val Primary = Color(0xFF7DFF9B)
    val PrimaryMuted = Color(0xFF2D6B3C)
    val Text = Color(0xFFF1F7F2)
    val TextMuted = Color(0xFF8F9A92)
    val Warning = Color(0xFFFFC857)
    val Error = Color(0xFFFF6B6B)
}

private val BlakeColorScheme = darkColorScheme(
    primary = BlakeColors.Primary,
    onPrimary = BlakeColors.Background,
    background = BlakeColors.Background,
    onBackground = BlakeColors.Text,
    surface = BlakeColors.Surface,
    onSurface = BlakeColors.Text,
    surfaceVariant = BlakeColors.SurfaceRaised,
    onSurfaceVariant = BlakeColors.TextMuted,
    outline = BlakeColors.Border,
    error = BlakeColors.Error,
)

@Composable
fun BlakeTunerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BlakeColorScheme,
        content = content,
    )
}
