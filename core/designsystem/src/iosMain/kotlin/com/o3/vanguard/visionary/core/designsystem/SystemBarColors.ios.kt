package com.o3.vanguard.visionary.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.setStatusBarStyle

@Composable
actual fun SystemBarColors(
    statusBarColor: Color,
    darkStatusBarIcons: Boolean,
    navigationBarColor: Color,
    darkNavigationBarIcons: Boolean,
) {
    SideEffect {
        val statusBarStyle = if (darkStatusBarIcons) {
            UIStatusBarStyleDarkContent
        } else {
            UIStatusBarStyleLightContent
        }
        UIApplication.sharedApplication.setStatusBarStyle(statusBarStyle, animated = true)
    }
}
