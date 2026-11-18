package com.o3.vanguard.visionary.platform.permission

import androidx.compose.runtime.Composable

@Composable
expect fun AppPermissionGate(
    requireGpsCheck: Boolean = false,
    titleText: String = "Permissions Required",
    onPermissionsReady: () -> Unit,
)
