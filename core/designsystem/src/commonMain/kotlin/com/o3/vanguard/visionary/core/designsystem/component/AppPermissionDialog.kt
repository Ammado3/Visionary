package com.o3.vanguard.visionary.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors

@Composable
fun AppPermissionRationaleDialog(
    visible: Boolean,
    title: String = "Permissions Required",
    description: String = "This app requires location and system permissions to function securely. Please grant them in App Settings.",
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VisionaryColors.colorFBFFFD,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = VisionaryColors.color0B1B14,
            )
        },
        text = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = VisionaryColors.color496156,
            )
        },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VisionaryColors.color007847,
                    contentColor = VisionaryColors.colorF4FDF7,
                ),
            ) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = VisionaryColors.color496156)
            }
        },
    )
}

@Composable
fun AppGpsEnableDialog(
    visible: Boolean,
    title: String = "Enable Location/GPS",
    description: String = "Your device Location/GPS service is disabled. Please turn it ON in settings to continue.",
    onOpenLocationSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VisionaryColors.colorFBFFFD,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = VisionaryColors.color0B1B14,
            )
        },
        text = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = VisionaryColors.color496156,
            )
        },
        confirmButton = {
            Button(
                onClick = onOpenLocationSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VisionaryColors.color007847,
                    contentColor = VisionaryColors.colorF4FDF7,
                ),
            ) {
                Text("Turn ON GPS")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = VisionaryColors.color496156)
            }
        },
    )
}
