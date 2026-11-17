package com.o3.vanguard.visionary.platform.permission

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import com.o3.vanguard.visionary.core.designsystem.component.AppGpsEnableDialog
import com.o3.vanguard.visionary.core.designsystem.component.AppPermissionRationaleDialog

@Composable
actual fun AppPermissionGate(
    requireGpsCheck: Boolean,
    titleText: String,
    onPermissionsReady: () -> Unit,
) {
    AppPermissionHandlingScreen(
        requiredPermissions = getRequiredMediaPermissions(),
        requireGpsCheck = requireGpsCheck,
        titleText = titleText,
        onPermissionsAndLocationReady = onPermissionsReady,
    )
}

/**
 * Reusable permission and location service gate component.
 * Integrates permissions launcher, GPS service verification, lifecycle re-checking on ON_RESUME,
 * and Visionary's M3 design system styling.
 */
@Composable
fun AppPermissionHandlingScreen(
    requiredPermissions: List<String> = getRequiredLocationPermissions(),
    requireGpsCheck: Boolean = true,
    titleText: String = "Feature Security & Location Verification",
    onPermissionsAndLocationReady: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun checkPermissionsGranted(): Boolean {
        return context.hasMediaAccess() || context.hasPermissions(requiredPermissions)
    }

    var hasAllPermissions by remember { mutableStateOf(checkPermissionsGranted()) }
    var isGpsActive by remember { mutableStateOf(if (requireGpsCheck) context.isLocationGpsEnabled() else true) }

    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    var showGpsEnableDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasAllPermissions = checkPermissionsGranted()
        isGpsActive = if (requireGpsCheck) context.isLocationGpsEnabled() else true

        if (hasAllPermissions) {
            if (requireGpsCheck && !context.isLocationGpsEnabled()) {
                showGpsEnableDialog = true
            } else {
                onPermissionsAndLocationReady()
            }
        } else {
            showPermissionRationaleDialog = true
        }
    }

    fun startCheckFlow() {
        if (!checkPermissionsGranted()) {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        } else if (requireGpsCheck && !context.isLocationGpsEnabled()) {
            showGpsEnableDialog = true
        } else {
            onPermissionsAndLocationReady()
        }
    }

    LaunchedEffect(Unit) {
        hasAllPermissions = checkPermissionsGranted()
        isGpsActive = if (requireGpsCheck) context.isLocationGpsEnabled() else true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAllPermissions = checkPermissionsGranted()
                isGpsActive = if (requireGpsCheck) context.isLocationGpsEnabled() else true

                if (hasAllPermissions && isGpsActive) {
                    showPermissionRationaleDialog = false
                    showGpsEnableDialog = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = titleText,
            style = MaterialTheme.typography.titleLarge,
            color = VisionaryColors.color0B1B14,
        )

        Spacer(modifier = Modifier.height(16.dp))

        val isReady = hasAllPermissions && isGpsActive
        Text(
            text = if (isReady)
                "✓ All Permissions Granted"
            else
                "✕ Permissions or Location Services Missing",
            color = if (isReady) VisionaryColors.color007847 else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { startCheckFlow() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = VisionaryColors.color007847,
                contentColor = VisionaryColors.colorF4FDF7,
            ),
        ) {
            Text(text = if (isReady) "Continue" else "Check & Request Permissions")
        }
    }

    AppPermissionRationaleDialog(
        visible = showPermissionRationaleDialog,
        onOpenSettings = {
            showPermissionRationaleDialog = false
            context.openAppSettings()
        },
        onDismiss = { showPermissionRationaleDialog = false }
    )

    AppGpsEnableDialog(
        visible = showGpsEnableDialog,
        onOpenLocationSettings = {
            showGpsEnableDialog = false
            context.openLocationSettings()
        },
        onDismiss = { showGpsEnableDialog = false }
    )
}
