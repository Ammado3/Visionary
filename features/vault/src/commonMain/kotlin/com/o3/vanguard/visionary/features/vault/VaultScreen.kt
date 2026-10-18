package com.o3.vanguard.visionary.features.vault

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.o3.vanguard.visionary.core.mvi.CollectEvents
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VaultLockRoute(
    onUnlocked: () -> Unit,
    onBack: () -> Unit,
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
    VaultLockScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun VaultLockScreen(
    state: VaultState,
    onIntent: (VaultIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Biometric Vault", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(24.dp))
            if (state.authenticating) {
                CircularProgressIndicator()
            } else {
                Button(onClick = { onIntent(VaultIntent.OnUnlockClicked) }) {
                    Text("Unlock Vault")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onIntent(VaultIntent.OnBackClicked) }) {
                Text("Back")
            }
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
                Button(onClick = { onIntent(VaultIntent.OnLockClicked) }) { Text("Lock") }
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
            Button(onClick = onBack) { Text("Back") }
        }
    }
}
