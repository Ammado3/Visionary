package com.o3.vanguard.visionary.platform.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

private class IosMicrophonePermission(
    private val onResult: (Boolean) -> Unit,
) : MicrophonePermission {
    override val isGranted: Boolean = false

    override fun request() {
        onResult(false)
    }
}

@Composable
actual fun rememberMicrophonePermission(onResult: (Boolean) -> Unit): MicrophonePermission {
    return remember(onResult) {
        IosMicrophonePermission(onResult)
    }
}
