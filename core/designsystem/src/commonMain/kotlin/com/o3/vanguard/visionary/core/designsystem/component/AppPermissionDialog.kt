package com.o3.vanguard.visionary.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import com.o3.vanguard.visionary.core.designsystem.generated.resources.Permissions_Required
import com.o3.vanguard.visionary.core.designsystem.generated.resources.Permissions_Required_Description
import com.o3.vanguard.visionary.core.designsystem.generated.resources.Res
import com.o3.vanguard.visionary.core.designsystem.generated.resources.cancel
import com.o3.vanguard.visionary.core.designsystem.generated.resources.gps_enable
import com.o3.vanguard.visionary.core.designsystem.generated.resources.gps_on
import com.o3.vanguard.visionary.core.designsystem.generated.resources.gps_permission
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppPermissionRationaleDialog(
    visible: Boolean,
    title: String = stringResource(Res.string.Permissions_Required),
    description: String = stringResource(Res.string.Permissions_Required_Description),
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
                Text(stringResource(Res.string.Permissions_Required_Description))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel), color = VisionaryColors.color496156)
            }
        },
    )
}

@Composable
fun AppGpsEnableDialog(
    visible: Boolean,
    title: String = stringResource(Res.string.gps_enable),
    description: String = stringResource(Res.string.gps_permission),
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
                Text(stringResource(Res.string.gps_on))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel), color = VisionaryColors.color496156)
            }
        },
    )
}
