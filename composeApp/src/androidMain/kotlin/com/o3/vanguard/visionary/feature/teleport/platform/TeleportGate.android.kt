package com.o3.vanguard.visionary.feature.teleport.platform

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberTeleportGate(onBlocked: (ReadinessResult) -> Unit): TeleportGate {
    val context = LocalContext.current
    val currentOnBlocked by rememberUpdatedState(onBlocked)
    var pendingOnReady by remember { mutableStateOf<(() -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val callback = pendingOnReady
        pendingOnReady = null
        val result = AndroidReadiness.check(context)
        if (result is ReadinessResult.Ready) callback?.invoke() else currentOnBlocked(result)
    }

    return remember(context, launcher) {
        object : TeleportGate {
            override fun ensureReady(onReady: () -> Unit) {
                when (val result = AndroidReadiness.check(context)) {
                    ReadinessResult.Ready -> onReady()
                    is ReadinessResult.MissingPermissions -> {
                        pendingOnReady = onReady
                        launcher.launch(result.permissions.toTypedArray())
                    }
                    else -> currentOnBlocked(result)
                }
            }

            override fun openSettingsFor(result: ReadinessResult) {
                val action = when (result) {
                    ReadinessResult.BluetoothDisabled -> Settings.ACTION_BLUETOOTH_SETTINGS
                    ReadinessResult.WifiDisabled -> Settings.ACTION_WIFI_SETTINGS
                    ReadinessResult.LocationDisabled -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
                    is ReadinessResult.MissingPermissions -> Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    else -> return
                }
                val intent = Intent(action).apply {
                    if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }
}
