package com.o3.vanguard.visionary.features.vault.biometricsetup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import org.jetbrains.compose.resources.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.o3.vanguard.visionary.core.designsystem.AppTab
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import com.o3.vanguard.visionary.core.designsystem.VisionaryTheme
import com.o3.vanguard.visionary.core.designsystem.component.VisionaryHeader
import com.o3.vanguard.visionary.core.designsystem.generated.resources.Res
import com.o3.vanguard.visionary.core.designsystem.generated.resources.vault
import com.o3.vanguard.visionary.core.mvi.CollectEvents
import com.o3.vanguard.visionary.features.vault.biometricsetup.states.BiometricEvent
import com.o3.vanguard.visionary.features.vault.biometricsetup.states.BiometricIntent
import com.o3.vanguard.visionary.features.vault.biometricsetup.states.BiometricState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BiometricSetupRoute(
    onUnlocked: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (AppTab) -> Unit,
    viewModel: BiometricViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            BiometricEvent.UnlockSuccess -> onUnlocked()
            BiometricEvent.NavigateBack -> onBack()
        }
    }
    BiometricSetupScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun BiometricSetupScreen(
    state: BiometricState,
    onIntent: (BiometricIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(BiometricIntent.DismissDialog) },
            title = { Text("Success") },
            text = { Text(state.dialogMessage) },
            confirmButton = {
                Button(onClick = { onIntent(BiometricIntent.DismissDialog) }) {
                    Text("OK")
                }
            }
        )
    }

    if (state.showFailureDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(BiometricIntent.DismissDialog) },
            title = { Text("Authentication Failed") },
            text = { Text(state.dialogMessage) },
            confirmButton = {
                Button(onClick = { onIntent(BiometricIntent.DismissDialog) }) {
                    Text("OK")
                }
            }
        )
    }

    Surface(modifier = modifier.fillMaxSize(), color = VisionaryColors.colorF4FDF7) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(24.dp))

            VisionaryHeader(
                title = "Private Vault",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = { onIntent(BiometricIntent.OnBackClicked) },
                actions = null,
                backgroundColor = VisionaryColors.colorF4FDF7,
                contentColor = VisionaryColors.color0B1B14,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Lock Icon Container
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = VisionaryColors.color1A007847,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(Res.drawable.vault),
                                contentDescription = "Lock",
                                modifier = Modifier.size(36.dp),
                                colorFilter = ColorFilter.tint(VisionaryColors.color007847)
                            )

                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Unlock your Vault",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = VisionaryColors.color0B1B14,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter your 8-digit PIN to continue.",
                    fontSize = 14.sp,
                    color = VisionaryColors.color496156,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Minimum 4 dots indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(4) { index ->
                        val isFilled = index < state.pin.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(
                                    color = if (isFilled) VisionaryColors.color007847 else VisionaryColors.colorCEDED3,
                                    shape = CircleShape,
                                )
                        )
                    }
                }

                if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.errorMessage,
                        color = Color(0xFFBA1A1A),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Keypad
                val rows = state.rowNumber
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    for (row in rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            for (item in row) {
                                KeypadButton(
                                    item = item,
                                    onClick = {
                                        when (item) {
                                            "delete" -> onIntent(BiometricIntent.OnDeleteClicked)
                                            "biometric" -> onIntent(BiometricIntent.OnBiometricClicked)
                                            else -> onIntent(BiometricIntent.OnNumberClicked(item.first()))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Use biometric button
                Button(
                    onClick = { onIntent(BiometricIntent.OnBiometricClicked) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VisionaryColors.color1A007847,
                        contentColor = VisionaryColors.color007847,
                    ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = VisionaryColors.color007847,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Use biometric",
                            fontWeight = FontWeight.Bold,
                            color = VisionaryColors.color007847,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Forgot PIN?\nRecovery options depend on your device security settings.",
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = VisionaryColors.color496156,
                    modifier = Modifier.clickable { onIntent(BiometricIntent.OnForgotPinClicked) },
                )
            }
        }
    }
}

@Composable
fun KeypadButton(
    item: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when (item) {
            "delete" -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Delete",
                    tint = VisionaryColors.color0B1B14,
                    modifier = Modifier.size(24.dp),
                )
            }
            "biometric" -> {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric",
                    tint = VisionaryColors.color007847,
                    modifier = Modifier.size(26.dp),
                )
            }
            else -> {
                Text(
                    text = item,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = VisionaryColors.color0B1B14,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BiometricSetupScreenPreview() {
    VisionaryTheme {
        BiometricSetupScreen(
            state = BiometricState(pin = "1234"),
            onIntent = {},
        )
    }
}
