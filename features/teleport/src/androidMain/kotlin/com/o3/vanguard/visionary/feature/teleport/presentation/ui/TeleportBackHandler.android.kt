package com.o3.vanguard.visionary.feature.teleport.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun TeleportBackHandler(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
}
