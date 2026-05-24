package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SJLGold,
    secondary = ForestGreen,
    tertiary = Color(0xFF34D399),
    background = DarkForestBg,
    surface = SlateCardBg,
    onPrimary = DarkForestBg,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    secondary = SJLGold,
    tertiary = Color(0xFF10B981),
    background = LightEmeraldBg,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = DarkForestBg,
    onBackground = DarkGrey,
    onSurface = DarkGrey
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
