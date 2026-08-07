package com.blakelabs.guitartuner.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object BlakeColors {
    val Background = Color(0xFF050605)
    val Surface = Color(0xFF0B0D0B)
    val SurfaceRaised = Color(0xFF121511)
    val SurfaceSoft = Color(0xFF171B15)
    val Border = Color(0xFF252B22)
    val Primary = Color(0xFFA8F20D)
    val PrimarySoft = Color(0xFF83BD0A)
    val PrimaryMuted = Color(0xFF3C5807)
    val Text = Color(0xFFF4F6F1)
    val TextMuted = Color(0xFF969D90)
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
