package com.blakelabs.guitartuner.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Blake Labs visual tokens.
 *
 * The palette deliberately stays close to true black so the UI feels at home on OLED panels,
 * while the alien-mark lime is reserved for state, focus and brand moments.
 */
object BlakeColors {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF090B09)
    val SurfaceRaised = Color(0xFF101310)
    val Border = Color(0xFF1A201A)
    val BorderStrong = Color(0xFF303830)
    val Primary = Color(0xFFA7F20A)
    val PrimarySoft = Color(0xFF7DB600)
    val Text = Color(0xFFF5F7F2)
    val TextMuted = Color(0xFF9AA398)
    val TextDim = Color(0xFF626A61)
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
