package com.o3.vanguard.visionary.features.vault.valutscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.o3.vanguard.visionary.core.mvi.CollectEvents
import com.o3.vanguard.visionary.core.designsystem.AppBottomNavigation
import com.o3.vanguard.visionary.core.designsystem.AppTab
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import com.o3.vanguard.visionary.core.designsystem.VisionaryTheme
import com.o3.vanguard.visionary.core.designsystem.component.PrimaryButton
import com.o3.vanguard.visionary.features.vault.vault.VaultTab
import com.o3.vanguard.visionary.features.vault.vault.states.VaultEvent
import com.o3.vanguard.visionary.features.vault.vault.states.VaultIntent
import com.o3.vanguard.visionary.features.vault.vault.states.VaultState
import com.o3.vanguard.visionary.features.vault.vault.VaultViewModel
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
                Text("Keep special moments\nprivate.", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text("Move selected photos and videos into\na protected area, separate from your\neveryday gallery.", color = VisionaryColors.color496156, textAlign = TextAlign.Center)
                Button(onClick = { onIntent(VaultIntent.OnUnlockClicked) }, modifier = Modifier.fillMaxWidth()) {
                    if (state.authenticating) CircularProgressIndicator(Modifier.size(20.dp), color = VisionaryColors.colorFFFFFFFF)
                    else Text("Set Up Vault")
                }
                Text("You can change Vault settings anytime.", style = MaterialTheme.typography.bodySmall, color = VisionaryColors.color496156)
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
    val filteredItems = when (state.selectedTab) {
        VaultTab.PHOTOS -> state.items.filter { !it.mime.contains("video", ignoreCase = true) }
        VaultTab.VIDEOS -> state.items.filter { it.mime.contains("video", ignoreCase = true) }
    }
    val selectedCount = state.selectedItemIds.size

    Surface(modifier = modifier.fillMaxSize(), color = VisionaryColors.colorF4FDF7) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { onIntent(VaultIntent.OnBackClicked) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VisionaryColors.color0B1B14)
                }
                Text("Add to Vault", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = VisionaryColors.color0B1B14)
                TextButton(onClick = { onIntent(VaultIntent.OnBackClicked) }) {
                    Text("Cancel", color = VisionaryColors.color007847, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            // Segmented Tab Toggle (Photos / Videos)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(VaultTab.PHOTOS to "Photos", VaultTab.VIDEOS to "Videos").forEach { (tab, label) ->
                    val isSelected = state.selectedTab == tab
                    Surface(
                        modifier = Modifier.weight(1f).height(40.dp).clickable { onIntent(VaultIntent.OnTabSelected(tab)) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) VisionaryColors.colorFFFFFFFF else VisionaryColors.colorCEDED3.copy(alpha = 0.5f),
                        tonalElevation = if (isSelected) 2.dp else 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                label,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) VisionaryColors.color007847 else VisionaryColors.color496156
                            )
                        }
                    }
                }
            }

            // Subheader: Selected count & Select all
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$selectedCount selected", fontWeight = FontWeight.Medium, color = VisionaryColors.color0B1B14)
                TextButton(onClick = { onIntent(VaultIntent.OnSelectAllToggled) }) {
                    Text(
                        if (state.selectedItemIds.size == filteredItems.size && filteredItems.isNotEmpty()) "Deselect all" else "Select all",
                        color = VisionaryColors.color007847,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(filteredItems) { item ->
                    val isSelected = state.selectedItemIds.contains(item.id)
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { onIntent(VaultIntent.OnItemSelected(item.id)) },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = item.filePath,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Checkbox badge in top-right corner
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(24.dp)
                                    .background(
                                        color = if (isSelected) VisionaryColors.color007847 else VisionaryColors.colorFFFFFFFF.copy(alpha = 0.7f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = VisionaryColors.colorFFFFFFFF, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Bottom action button
            if (selectedCount > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = VisionaryColors.colorFFFFFFFF,
                    tonalElevation = 8.dp
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        PrimaryButton(
                            text = "Remove $selectedCount items from Vault",
                            onClick = { onIntent(VaultIntent.OnRemoveSelectedClicked) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (state.showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(VaultIntent.OnDismissRemoveDialog) },
            title = { Text("Remove from Vault") },
            text = { Text("Do you want to restore these $selectedCount items back to your gallery or delete them permanently?") },
            confirmButton = {
                TextButton(onClick = { onIntent(VaultIntent.OnRestoreConfirmed) }) {
                    Text("Restore in gallery", color = VisionaryColors.color007847, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { onIntent(VaultIntent.OnDeletePermanentlyConfirmed) }) {
                        Text("Delete permanently", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { onIntent(VaultIntent.OnDismissRemoveDialog) }) {
                        Text("Cancel")
                    }
                }
            }
        )
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

@Preview
@Composable
fun ValutScreenPreview(){
    VisionaryTheme {
        VaultLockScreen(state = VaultState(), onIntent = {}, onNavigate = {})
    }
}
