package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color(0xFF031623),
    primaryContainer = Color(0xFF0F3450),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = AccentEmerald,
    onSecondary = Color(0xFF022C1A),
    secondaryContainer = Color(0xFF094A2F),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = AccentPurple,
    onTertiary = Color(0xFF1E174E),
    tertiaryContainer = Color(0xFF2E246E),
    onTertiaryContainer = Color(0xFFDDD6FE),
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

@Composable
fun ClujShopTheme(
    darkTheme: Boolean = true, // Default to true as user explicitly requested "tema inchisa"
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
