package com.o3.vanguard.visionary.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
expect fun SystemBarColors(
    statusBarColor: Color,
    darkStatusBarIcons: Boolean,
    navigationBarColor: Color = statusBarColor,
    darkNavigationBarIcons: Boolean = darkStatusBarIcons,
)
