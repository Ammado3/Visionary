package com.o3.vanguard.visionary.feature.teleport

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.o3.vanguard.visionary.core.navigation.AppRoute
import com.o3.vanguard.visionary.core.navigation.Navigator
import com.o3.vanguard.visionary.feature.teleport.domain.model.PhotoRef
import com.o3.vanguard.visionary.feature.teleport.platform.ReadinessResult
import com.o3.vanguard.visionary.feature.teleport.platform.TeleportBlockedDialog
import com.o3.vanguard.visionary.feature.teleport.platform.rememberTeleportGate
import com.o3.vanguard.visionary.feature.teleport.presentation.ui.TeleportReceiverScreen
import com.o3.vanguard.visionary.feature.teleport.presentation.ui.TeleportSenderScreen

fun EntryProviderScope<NavKey>.teleportEntries(navigator: Navigator) {
    entry<AppRoute.TeleportSender> { route ->
        var blockedResult by remember { mutableStateOf<ReadinessResult?>(null) }
        val gate = rememberTeleportGate(onBlocked = { blockedResult = it })

        var ready by remember { mutableStateOf(false) }
        LaunchedEffect(gate) {
            gate.ensureReady(onReady = { ready = true })
        }

        val photo = PhotoRef(
            uri = route.uri,
            displayName = route.displayName,
            mimeType = route.mimeType,
            sizeBytes = route.sizeBytes,
        )

        if (ready) {
            TeleportSenderScreen(
                photo = photo,
                onClose = { navigator.back() },
            )
        }

        blockedResult?.let { result ->
            TeleportBlockedDialog(
                result = result,
                onDismiss = {
                    blockedResult = null
                    navigator.back()
                },
                onOpenSettings = { gate.openSettingsFor(it) },
            )
        }
    }

    entry<AppRoute.TeleportReceiver> {
        var blockedResult by remember { mutableStateOf<ReadinessResult?>(null) }
        val gate = rememberTeleportGate(onBlocked = { blockedResult = it })

        var ready by remember { mutableStateOf(false) }
        LaunchedEffect(gate) {
            gate.ensureReady(onReady = { ready = true })
        }

        if (ready) {
            TeleportReceiverScreen(
                onClose = { navigator.back() },
            )
        }

        blockedResult?.let { result ->
            TeleportBlockedDialog(
                result = result,
                onDismiss = {
                    blockedResult = null
                    navigator.back()
                },
                onOpenSettings = { gate.openSettingsFor(it) },
            )
        }
    }
}
