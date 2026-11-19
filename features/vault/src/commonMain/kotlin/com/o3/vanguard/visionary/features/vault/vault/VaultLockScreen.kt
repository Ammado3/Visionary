package com.o3.vanguard.visionary.features.vault.vault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.o3.vanguard.visionary.core.designsystem.AppBottomNavigation
import com.o3.vanguard.visionary.core.designsystem.AppTab
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import com.o3.vanguard.visionary.core.designsystem.component.PrimaryButton
import com.o3.vanguard.visionary.core.designsystem.component.VisionaryHeader
import com.o3.vanguard.visionary.core.mvi.CollectEvents
import com.o3.vanguard.visionary.features.vault.vault.states.VaultLockEvent
import com.o3.vanguard.visionary.features.vault.vault.states.VaultLockIntent
import com.o3.vanguard.visionary.features.vault.vault.states.VaultLockState
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VaultLockRoute(
    onOpenBiometricSetup: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (AppTab) -> Unit,
    viewModel: VaultLockViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            VaultLockEvent.OpenBiometricSetup -> onOpenBiometricSetup()
            VaultLockEvent.NavigateToHome -> onBack()
        }
    }
    VaultLockScreen(state = state, onIntent = viewModel::onIntent, onNavigate = onNavigate)
}

@Composable
fun VaultLockScreen(
    state: VaultLockState,
    onIntent: (VaultLockIntent) -> Unit,
    onNavigate: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = VisionaryColors.colorF4FDF7) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(24.dp))
            VisionaryHeader(
                title = "Private Vault",
                navigationIcon = null,
                actions = null,
                backgroundColor = VisionaryColors.colorF4FDF7,
                contentColor = VisionaryColors.color0B1B14,
            )
            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = VisionaryColors.color1A007847,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PhotoLibrary, null, Modifier.size(36.dp), tint = VisionaryColors.color007847)
                    }
                }
                Text("Keep special moments\nprivate.", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(
                    "Move selected photos and videos into\na protected area, separate from your\neveryday gallery.",
                    color = VisionaryColors.color496156,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    lineHeight = 24.sp,
                )

                Spacer(modifier = Modifier.weight(1f))
                if (state.authenticating) {
                    CircularProgressIndicator()
                } else {
                    PrimaryButton(
                        text = "Set Up Vault",
                        onClick = { onIntent(VaultLockIntent.OnSetupVaultClicked) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "You can change Vault settings anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VisionaryColors.color496156,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun VaultLockScreenPreview() {
    VaultLockScreen(
        state = VaultLockState(),
        onIntent = {},
        onNavigate = {},
    )
}
