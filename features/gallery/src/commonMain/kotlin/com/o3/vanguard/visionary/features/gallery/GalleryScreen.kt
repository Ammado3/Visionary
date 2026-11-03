package com.o3.vanguard.visionary.features.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.o3.vanguard.visionary.core.mvi.CollectEvents
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import com.o3.vanguard.visionary.domain.model.MediaCategory
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.MediaItem
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GalleryRoute(
    kindId: String,
    onOpenViewer: (MediaId) -> Unit,
    onBack: () -> Unit,
    viewModel: GalleryViewModel = koinViewModel { parametersOf(kindId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is GalleryEvent.OpenViewer -> onOpenViewer(event.id)
            GalleryEvent.NavigateBack -> onBack()
        }
    }
    Surface(Modifier.fillMaxSize(), color = VisionaryColors.colorF4FDF7) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(state.collectionKind.name, style = MaterialTheme.typography.headlineMedium, color = VisionaryColors.color0B1B14)
                Button(onClick = { viewModel.onIntent(GalleryIntent.OnBackClicked) }) { Text("Back") }
            }
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.items) { item ->
                    AsyncImage(
                        model = item.uri,
                        contentDescription = item.fileName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.height(120.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                            viewModel.onIntent(GalleryIntent.OnItemClicked(item.id))
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun ViewerRoute(
    mediaId: Long,
    highlight: String?,
    onBack: () -> Unit,
    viewModel: ViewerViewModel = koinViewModel { parametersOf(mediaId, highlight) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        if (event == ViewerEvent.NavigateBack) onBack()
    }
    ViewerScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ViewerScreen(
    state: ViewerState,
    onIntent: (ViewerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = state.item
    if (item == null) {
        Surface(modifier.fillMaxSize(), color = VisionaryColors.colorF4FDF7) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VisionaryColors.color007847)
            }
        }
        return
    }

    var showOcrTextSheet by remember(item.id) { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember(item.id) { mutableStateOf(false) }
    var showOptionsSheet by remember(item.id) { mutableStateOf(false) }
    val detailTextPreview = item.textPreview

    Dialog(
        onDismissRequest = { onIntent(ViewerIntent.OnBackClicked) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = VisionaryColors.colorFF000000) {
            Box(Modifier.fillMaxSize()) {
                if (item.category == MediaCategory.VIDEO) {
                    PlatformVideoPlayer(item.uri, Modifier.fillMaxSize())
                } else {
                    AsyncImage(
                        model = item.uri,
                        contentDescription = item.fileName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { onIntent(ViewerIntent.OnBackClicked) }) {
                        Text("Back", color = VisionaryColors.colorFFFFFFFF, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = item.fileName.ifBlank { if (item.category == MediaCategory.VIDEO) "Video" else "Photo" },
                            color = VisionaryColors.colorFFFFFFFF,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    IconButton(onClick = { showOptionsSheet = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = VisionaryColors.colorFFFFFFFF)
                    }
                }

                if (!detailTextPreview.isNullOrBlank()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Surface(
                            onClick = { showOcrTextSheet = !showOcrTextSheet },
                            shape = RoundedCornerShape(20.dp),
                            color = VisionaryColors.colorFFFFFFFF.copy(alpha = 0.85f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Show extracted text",
                                    modifier = Modifier.size(16.dp),
                                    tint = VisionaryColors.colorFF000000,
                                )
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    "OCR Text Available",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = VisionaryColors.colorFF000000,
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showOcrTextSheet,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = VisionaryColors.colorFF444444.copy(alpha = 0.95f),
                        contentColor = VisionaryColors.colorFFFFFFFF,
                    ) {
                        Column(Modifier.fillMaxWidth().height(220.dp).padding(16.dp)) {
                            Text(
                                "Extracted Text (OCR)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primaryContainer,
                            )
                            Spacer(Modifier.height(8.dp))
                            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                                Text(
                                    detailTextPreview.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VisionaryColors.colorFFFFFFFF,
                                )
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                    color = VisionaryColors.colorFF000000.copy(alpha = 0.9f),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FullscreenViewerAction(Icons.Default.Share, "Share") { }
                        FullscreenViewerAction(Icons.Default.Edit, "Edit") { }
                        FullscreenViewerAction(Icons.Default.Add, "Add to") { }
                        FullscreenViewerAction(Icons.Default.Delete, "Bin") { showDeleteConfirmDialog = true }
                    }
                }

                if (showOptionsSheet) {

                    ModalBottomSheet(onDismissRequest = { showOptionsSheet = false }) {
                        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                            Text(
                                "Photo options",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                            )
                            TextButton(
                                onClick = { showOptionsSheet = false },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            ) { Text("Share", modifier = Modifier.fillMaxWidth()) }
                            TextButton(
                                onClick = { showOptionsSheet = false },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            ) { Text("Edit", modifier = Modifier.fillMaxWidth()) }
                            TextButton(
                                onClick = { showOptionsSheet = false },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            ) { Text("Add to", modifier = Modifier.fillMaxWidth()) }
                            TextButton(
                                onClick = {
                                    showOptionsSheet = false
                                    showDeleteConfirmDialog = true
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            ) { Text("Move to Bin", color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth()) }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Move to Bin?") },
            text = { Text("Are you sure you want to delete this image?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    onIntent(ViewerIntent.OnDeleteClicked)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun FullscreenViewerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp), tint = VisionaryColors.colorFFFFFFFF)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = VisionaryColors.colorFFFFFFFF)
    }
}

@Composable
private fun ViewerHeader(item: MediaItem, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = VisionaryColors.color0B1B14) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = item.fileName.ifBlank { if (item.category == MediaCategory.VIDEO) "Video" else "Photo" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = VisionaryColors.color0B1B14,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("${item.takenAtMs}", style = MaterialTheme.typography.bodySmall, color = VisionaryColors.color496156)
        }
        IconButton(onClick = {}) { Icon(Icons.Default.StarBorder, "Add to favorites", tint = VisionaryColors.color0B1B14) }
        IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, "More options", tint = VisionaryColors.color0B1B14) }
    }
}

@Composable
private fun ViewerActions(onMoveToVault: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        color = VisionaryColors.colorF4FDF7,
        border = androidx.compose.foundation.BorderStroke(1.dp, VisionaryColors.colorCEDED3),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            ViewerAction(Icons.Default.Share, "Share")
            ViewerAction(Icons.Default.Tune, "Edit")
            ViewerAction(Icons.Default.Add, "Add to", onMoveToVault)
            ViewerAction(Icons.Default.Delete, "Bin")
        }
    }
}

@Composable
private fun ViewerAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit = {}) {
    Column(Modifier.clickable(onClick = onClick).padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, Modifier.size(24.dp), tint = VisionaryColors.color496156)
        Text(label, style = MaterialTheme.typography.labelSmall, color = VisionaryColors.color496156, textAlign = TextAlign.Center)
    }
}

@Composable
expect fun PlatformVideoPlayer(uri: String, modifier: Modifier)
