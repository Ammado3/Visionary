package com.o3.vanguard.visionary.platform.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun AppPermissionGate(
    requireGpsCheck: Boolean,
    titleText: String,
    onPermissionsReady: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onPermissionsReady()
    }
}
