package com.o3.vanguard.visionary.features.vault

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.o3.vanguard.visionary.core.designsystem.AppBottomNavigation
import com.o3.vanguard.visionary.core.designsystem.AppTab
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import com.o3.vanguard.visionary.core.designsystem.component.PrimaryButton
import com.o3.vanguard.visionary.core.designsystem.generated.resources.Res
import com.o3.vanguard.visionary.core.designsystem.generated.resources.cancel
import com.o3.vanguard.visionary.core.designsystem.generated.resources.setup_vault
import com.o3.vanguard.visionary.core.designsystem.generated.resources.vault_content
import com.o3.vanguard.visionary.core.designsystem.generated.resources.vault_desc
import com.o3.vanguard.visionary.core.designsystem.generated.resources.vault_tetx
import com.o3.vanguard.visionary.core.mvi.CollectEvents
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VaultLockRoute(
    onUnlocked: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (AppTab) -> Unit,
    viewModel: VaultViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            VaultEvent.OpenVaultGrid -> onUnlocked()
            VaultEvent.NavigateToHome -> onBack()
            else -> {}
        }
    }
    VaultLockScreen(state = state, onIntent = viewModel::onIntent, onNavigate = onNavigate)
}

@Composable
fun VaultLockScreen(
    state: VaultState,
    onIntent: (VaultIntent) -> Unit,
    onNavigate: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = VisionaryColors.colorF4FDF7) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                Text(stringResource(Res.string.vault_tetx), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(stringResource(Res.string.vault_desc), color = VisionaryColors.color496156, textAlign = TextAlign.Center)
                
                PrimaryButton(
                    text = stringResource(Res.string.setup_vault),
                    onClick = { onIntent(VaultIntent.OnUnlockClicked) },
                    enabled = !state.authenticating
                )
                
                Text(stringResource(Res.string.vault_content), style = MaterialTheme.typography.bodySmall, color = VisionaryColors.color496156)
            }
            AppBottomNavigation(AppTab.VAULT, onNavigate)
        }
    }
}

@Composable
fun VaultGridRoute(
    onOpenViewer: (String) -> Unit,
    onLock: () -> Unit,
    viewModel: VaultViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is VaultEvent.OpenVaultViewer -> onOpenViewer(event.itemId)
            VaultEvent.NavigateToHome -> onLock()
            else -> {}
        }
    }
    VaultGridScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun VaultGridScreen(
    state: VaultState,
    onIntent: (VaultIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vault Contents", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = { onIntent(VaultIntent.OnLockClicked) }) { Text(stringResource(Res.string.cancel)) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.items) { item ->
                    Card(
                        modifier = Modifier
                            .height(120.dp)
                            .clickable { onIntent(VaultIntent.OnItemClicked(item.id)) }
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Vault Item")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VaultViewerRoute(
    itemId: String,
    onBack: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Vault Item: $itemId", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onBack) { Text(stringResource(Res.string.cancel)) }
        }
    }
}
