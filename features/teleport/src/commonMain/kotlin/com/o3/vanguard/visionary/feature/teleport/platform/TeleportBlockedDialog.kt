package com.o3.vanguard.visionary.feature.teleport.platform

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun TeleportBlockedDialog(
    result: ReadinessResult,
    onDismiss: () -> Unit,
    onOpenSettings: (ReadinessResult) -> Unit,
) {
    if (result is ReadinessResult.Ready) return

    val title = when (result) {
        is ReadinessResult.MissingPermissions -> "Permissions Required"
        ReadinessResult.BluetoothDisabled -> "Bluetooth Required"
        ReadinessResult.WifiDisabled -> "Wi-Fi Required"
        ReadinessResult.LocationDisabled -> "Location Services Required"
        ReadinessResult.PlayServicesUnavailable -> "Google Play Services Required"
        ReadinessResult.Unsupported -> "Feature Unsupported"
        ReadinessResult.Ready -> ""
    }

    val message = when (result) {
        is ReadinessResult.MissingPermissions -> "Air Teleport needs nearby device permissions to discover and connect to nearby phones."
        ReadinessResult.BluetoothDisabled -> "Please enable Bluetooth to use Air Teleport."
        ReadinessResult.WifiDisabled -> "Please enable Wi-Fi to send files via Air Teleport."
        ReadinessResult.LocationDisabled -> "Please enable Location Services to discover nearby devices on your version of Android."
        ReadinessResult.PlayServicesUnavailable -> "Air Teleport requires updated Google Play Services."
        ReadinessResult.Unsupported -> "Air Teleport is currently not supported on this platform."
        ReadinessResult.Ready -> ""
    }

    val showSettings = result is ReadinessResult.MissingPermissions ||
            result == ReadinessResult.BluetoothDisabled ||
            result == ReadinessResult.WifiDisabled ||
            result == ReadinessResult.LocationDisabled

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            if (showSettings) {
                TextButton(
                    onClick = {
                        onDismiss()
                        onOpenSettings(result)
                    },
                ) {
                    Text("Open Settings")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        },
        dismissButton = if (showSettings) {
            { TextButton(onClick = onDismiss) { Text("Cancel") } }
        } else null,
    )
}
