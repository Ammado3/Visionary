package com.o3.vanguard.visionary.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = VisionaryColors.color007847,
    background = VisionaryColors.colorF4FDF7,
    surface = VisionaryColors.colorF4FDF7,
)
private val LightColorScheme = lightColorScheme(
    primary = VisionaryColors.color007847,
    background = VisionaryColors.colorF4FDF7,
    surface = VisionaryColors.colorF4FDF7,
    onBackground = VisionaryColors.color0B1B14,
    onSurface = VisionaryColors.color0B1B14,
)

@Composable
fun VisionaryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
