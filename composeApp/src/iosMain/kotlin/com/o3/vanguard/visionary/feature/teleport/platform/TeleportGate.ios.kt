package com.o3.vanguard.visionary.feature.teleport.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberTeleportGate(onBlocked: (ReadinessResult) -> Unit): TeleportGate = remember {
    object : TeleportGate {
        override fun ensureReady(onReady: () -> Unit) = onBlocked(ReadinessResult.Unsupported)
        override fun openSettingsFor(result: ReadinessResult) = Unit
    }
}
