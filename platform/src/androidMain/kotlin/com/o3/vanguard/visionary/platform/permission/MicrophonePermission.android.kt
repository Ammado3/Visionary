package com.o3.vanguard.visionary.platform.permission

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private class AndroidMicrophonePermission(
    private val checkIsGranted: () -> Boolean,
    private val onRequest: () -> Unit,
) : MicrophonePermission {
    override val isGranted: Boolean
        get() = checkIsGranted()

    override fun request() {
        onRequest()
    }
}

@Composable
actual fun rememberMicrophonePermission(onResult: (Boolean) -> Unit): MicrophonePermission {
    val context = LocalContext.current
    var isGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isGranted = granted
        onResult(granted)
    }

    return remember(context, isGranted) {
        AndroidMicrophonePermission(
            checkIsGranted = {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                isGranted = granted
                granted
            },
            onRequest = {
                if (isGranted) {
                    onResult(true)
                } else {
                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
        )
    }
}
