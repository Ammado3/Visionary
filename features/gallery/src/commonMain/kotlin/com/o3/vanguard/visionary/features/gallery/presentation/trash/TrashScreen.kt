package com.o3.vanguard.visionary.features.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import com.o3.vanguard.visionary.core.mvi.CollectEvents
import com.o3.vanguard.visionary.domain.model.TrashItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TrashBinRoute(
    onBack: () -> Unit,
    viewModel: TrashViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        if (event is TrashEvent.NavigateBack) onBack()
    }
    TrashBinScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun TrashBinScreen(
    state: TrashState,
    onIntent: (TrashIntent) -> Unit,
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var itemsPendingDelete by remember { mutableStateOf<List<TrashItem>>(emptyList()) }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Permanently?") },
            text = { Text("Are you sure you want to permanently delete the selected items?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onIntent(TrashIntent.OnDeletePermanently(itemsPendingDelete.map { it.id }))
                        showDeleteConfirmDialog = false
                        itemsPendingDelete = emptyList()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Detail viewer for single tapped trash item
    if (state.selectedItem != null) {
        TrashDetailView(
            item = state.selectedItem,
            onDismiss = { onIntent(TrashIntent.OnDismissDetail) },
            onDelete = {
                itemsPendingDelete = listOf(state.selectedItem)
                showDeleteConfirmDialog = true
            },
            onRestore = {
                onIntent(TrashIntent.OnRestoreItems(listOf(state.selectedItem.id)))
            }
        )
        return
    }

    Surface(Modifier.fillMaxSize(), color = VisionaryColors.colorF4FDF7) {
        Column(Modifier.fillMaxSize()) {
            if (state.isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(VisionaryColors.colorF4FDF7)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { onIntent(TrashIntent.OnClearSelection) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear selection", tint = VisionaryColors.color0B1B14)
                    }
                    Text(
                        text = "${state.selectedTrashIds.size} selected",
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = VisionaryColors.color0B1B14,
                    )
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { onIntent(TrashIntent.OnBackClicked) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VisionaryColors.color0B1B14)
                        }
                        Text("Trash Bin", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = VisionaryColors.color0B1B14)
                    }
                }
            }

            if (state.items.isEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = VisionaryColors.color496156)
                        Text("Trash bin is empty", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = VisionaryColors.color496156, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    modifier = Modifier.fillMaxSize().weight(1f).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.id.value }) { item ->
                        val isSelected = item.id in state.selectedTrashIds
                        Box(
                            Modifier
                                .height(120.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = { onIntent(TrashIntent.OnItemClicked(item)) },
                                    onLongClick = { onIntent(TrashIntent.OnItemLongClicked(item)) },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = item.fileName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            if (state.isSelectionMode) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(if (isSelected) VisionaryColors.color007847.copy(alpha = 0.3f) else Color.Transparent)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.TopEnd,
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = if (isSelected) "Selected" else "Not selected",
                                        tint = if (isSelected) VisionaryColors.color007847 else VisionaryColors.colorFFFFFFFF,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.isSelectionMode) {
                TrashSelectionBottomBar(
                    onRestore = {
                        val idsToRestore = state.items.filter { it.id in state.selectedTrashIds }.map { it.id }
                        onIntent(TrashIntent.OnRestoreItems(idsToRestore))
                    },
                    onDelete = {
                        itemsPendingDelete = state.items.filter { it.id in state.selectedTrashIds }
                        showDeleteConfirmDialog = true
                    },
                )
            }
        }
    }
}

@Composable
private fun TrashDetailView(
    item: TrashItem,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = VisionaryColors.colorFF000000) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.padding(16.dp).align(Alignment.TopStart),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VisionaryColors.colorFFFFFFFF)
            }

            AsyncImage(
                model = item.uri,
                contentDescription = item.fileName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(vertical = 64.dp),
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                color = VisionaryColors.colorFF000000.copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrashBottomActionItem(
                        icon = Icons.Default.Restore,
                        label = "Restore",
                        onClick = onRestore
                    )
                    TrashBottomActionItem(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        onClick = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun TrashBottomActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = VisionaryColors.colorFFFFFFFF,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = VisionaryColors.colorFFFFFFFF,
        )
    }
}

@Composable
private fun TrashSelectionBottomBar(
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = VisionaryColors.colorFFFFFFFF,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrashSelectionActionButton(
                icon = Icons.Default.Restore,
                label = "Restore",
                onClick = onRestore,
            )
            TrashSelectionActionButton(
                icon = Icons.Default.Delete,
                label = "Delete",
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun TrashSelectionActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = VisionaryColors.color0B1B14,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = VisionaryColors.color0B1B14,
        )
    }
}
