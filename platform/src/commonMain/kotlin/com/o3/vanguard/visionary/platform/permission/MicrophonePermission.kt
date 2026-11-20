package com.o3.vanguard.visionary.platform.permission

import androidx.compose.runtime.Composable

interface MicrophonePermission {
    val isGranted: Boolean
    fun request()
}

@Composable
expect fun rememberMicrophonePermission(onResult: (Boolean) -> Unit): MicrophonePermission
