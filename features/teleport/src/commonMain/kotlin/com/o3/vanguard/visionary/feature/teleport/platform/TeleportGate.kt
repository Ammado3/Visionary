package com.o3.vanguard.visionary.feature.teleport.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

sealed interface ReadinessResult {
    data object Ready : ReadinessResult
    data class MissingPermissions(val permissions: List<String>) : ReadinessResult
    data object BluetoothDisabled : ReadinessResult
    data object WifiDisabled : ReadinessResult
    data object LocationDisabled : ReadinessResult
    data object PlayServicesUnavailable : ReadinessResult
    data object Unsupported : ReadinessResult
}

@Stable
interface TeleportGate {
    /** Requests missing permissions if needed, then calls [onReady] only if everything is OK. */
    fun ensureReady(onReady: () -> Unit)
    fun openSettingsFor(result: ReadinessResult)
}

@Composable
expect fun rememberTeleportGate(onBlocked: (ReadinessResult) -> Unit): TeleportGate
