package com.o3.vanguard.visionary.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

object AppTheme {
    val colorScheme: VisionaryColorss
        @Composable
        @ReadOnlyComposable
        get() = VisionaryColorss()
}

private val DarkColorScheme = lightColorScheme(
    primary = VisionaryColors.color007847,
    onPrimary = VisionaryColors.colorFFFFFFFF,
    surface = VisionaryColors.colorFBFFFD,
    onSurface = VisionaryColors.color0B1B14,
    onSurfaceVariant = VisionaryColors.color496156,
    outlineVariant = VisionaryColors.colorCEDED3,
    background = VisionaryColors.colorF4FDF7,
    onBackground = VisionaryColors.color0B1B14,
)

private val LightColorScheme = lightColorScheme(
    primary = VisionaryColors.color007847,
    onPrimary = VisionaryColors.colorFFFFFFFF,
    surface = VisionaryColors.colorFBFFFD,
    onSurface = VisionaryColors.color0B1B14,
    onSurfaceVariant = VisionaryColors.color496156,
    outlineVariant = VisionaryColors.colorCEDED3,
    background = VisionaryColors.colorF4FDF7,
    onBackground = VisionaryColors.color0B1B14,
)

@Composable
fun VisionaryTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography.material,
        content = content
    )
}
