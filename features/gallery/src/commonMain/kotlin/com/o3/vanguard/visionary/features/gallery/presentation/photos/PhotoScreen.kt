package com.o3.vanguard.visionary.features.gallery.presentation.photos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import com.o3.vanguard.visionary.core.mvi.CollectEvents
import com.o3.vanguard.visionary.domain.model.IndexProgress
import com.o3.vanguard.visionary.domain.model.MediaCategory
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.MediaItem
import kotlin.time.Clock
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PhotoRoute(
    onOpenViewer: (MediaId) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAirTeleportReceiver: () -> Unit = {},
    viewModel: PhotoViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is PhotoEvent.OpenViewer -> onOpenViewer(event.id)
            PhotoEvent.OpenSettings -> onOpenSettings()
        }
    }
    PhotoScreen(state, viewModel::onIntent, onOpenAirTeleportReceiver)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoScreen(
    state: PhotoState,
    onIntent: (PhotoIntent) -> Unit,
    onOpenAirTeleportReceiver: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(emptySet<MediaId>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    val selectionMode = selectedIds.isNotEmpty()
    val visibleItems = state.items.filter { query.isBlank() || it.matchesQuery(query) }
    val gridState = rememberLazyGridState()
    val groupedItems = remember(state.items, query) {
        visibleItems.sortedByDescending { it.takenAtMs }.groupBy { it.photoDateLabel() }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Move to Bin?") },
            text = { Text("Are you sure you want to delete the selected images?") },
            confirmButton = {
                TextButton(onClick = {
                    onIntent(PhotoIntent.OnDeleteSelected(state.items.filter { it.id in selectedIds }))
                    selectedIds = emptySet()
                    showDeleteDialog = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
        )
    }

    Surface(modifier.fillMaxSize(), color = VisionaryColors.colorF4FDF7) {
        Column(Modifier.fillMaxSize()) {
            PhotoHeader(
                searchOpen = searchOpen,
                query = query,
                selectionMode = selectionMode,
                selectedCount = selectedIds.size,
                onMenu = { onIntent(PhotoIntent.OnSettingsClicked) },
                onSearch = { searchOpen = true },
                onQueryChanged = { query = it },
                onCloseSearch = { query = ""; searchOpen = false },
                onAirTeleportReceiver = onOpenAirTeleportReceiver,
                onClearSelection = { selectedIds = emptySet() },
                showMore = showMore,
                onShowMore = { showMore = it },
            )
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    state.loading -> CircularProgressIndicator(color = VisionaryColors.color007847)
                    state.error -> PhotoError { onIntent(PhotoIntent.OnRetryClicked) }
                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (state.showProcessingCard) {
                            state.indexProgress?.let { item(span = { GridItemSpan(maxLineSpan) }) { ProcessingCard(it) } }
                        }
                        if (visibleItems.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) { PhotoEmptyState(query) }
                        } else {
                            groupedItems.forEach { (title, items) ->
                                item(key = "header_$title", span = { GridItemSpan(maxLineSpan) }) {
                                    Text(title.uppercase(), Modifier.padding(top = 6.dp, bottom = 6.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = VisionaryColors.color95A6BF)
                                }
                                items(items, key = { it.id.value }) { item ->
                                    val selected = item.id in selectedIds
                                    MediaTile(item, selectionMode, selected, enabled = !gridState.isScrollInProgress,
                                        onClick = {
                                            selectedIds = if (selectionMode) {
                                                if (selected) selectedIds - item.id else selectedIds + item.id
                                            } else selectedIds
                                            if (!selectionMode) onIntent(PhotoIntent.OnItemClicked(item.id))
                                        },
                                        onLongClick = { selectedIds = selectedIds + item.id },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (selectionMode) {
                SelectionBar(
                    selectedCount = selectedIds.size,
                    onDelete = { showDeleteDialog = true },
                    onShare = {
                        onIntent(PhotoIntent.OnShareSelected(state.items.filter { it.id in selectedIds }))
                        selectedIds = emptySet()
                    },
                    onClear = { selectedIds = emptySet() },
                )
            }
        }
    }
}

@Composable
private fun PhotoHeader(
    searchOpen: Boolean, query: String, selectionMode: Boolean, selectedCount: Int,
    onMenu: () -> Unit, onSearch: () -> Unit, onQueryChanged: (String) -> Unit,
    onCloseSearch: () -> Unit, onAirTeleportReceiver: () -> Unit, onClearSelection: () -> Unit,
    showMore: Boolean, onShowMore: (Boolean) -> Unit,
) {
    if (selectionMode) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClearSelection) { Icon(Icons.Default.Clear, "Clear selection") }
            Text("$selectedCount selected", Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
        }
        return
    }
    if (searchOpen) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCloseSearch) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close search") }
            OutlinedTextField(value = query, onValueChange = onQueryChanged, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("Search photos and videos") }, leadingIcon = { Icon(Icons.Default.Search, "Search") }, shape = RoundedCornerShape(32.dp), colors = OutlinedTextFieldDefaults.colors())
        }
        return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(62.dp)
            .drawWithContent {
                drawContent()
                drawLine(
                    color = VisionaryColors.colorCEDED3,
                    start = Offset(0f, size.height - 0.5.dp.toPx()),
                    end = Offset(size.width, size.height - 0.5.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, "Open settings") }
        Text("Gallery", Modifier.weight(1f).padding(start = 8.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onSearch) { Icon(Icons.Default.Search, "Search") }
        Box {
            IconButton(onClick = { onShowMore(true) }) { Icon(Icons.Default.MoreVert, "More options") }
            DropdownMenu(showMore, { onShowMore(false) }) {
                DropdownMenuItem(text = { Text("Air Teleport Receiver") }, onClick = { onShowMore(false); onAirTeleportReceiver() })
                DropdownMenuItem(text = { Text("Settings") }, onClick = { onShowMore(false); onMenu() })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaTile(item: MediaItem, selectionMode: Boolean, selected: Boolean, enabled: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(VisionaryColors.colorE4EEE8).combinedClickable(enabled = enabled, onClick = onClick, onLongClick = onLongClick), contentAlignment = Alignment.Center) {
        AsyncImage(item.uri, item.fileName, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (item.category == MediaCategory.VIDEO && !selectionMode) Icon(Icons.Default.PlayArrow, "Video", Modifier.size(36.dp), tint = Color.White)
        if (selectionMode) Icon(if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, if (selected) "Selected" else "Not selected", Modifier.align(Alignment.TopEnd).padding(8.dp), tint = if (selected) VisionaryColors.color007847 else Color.White)
    }
}

@Composable
private fun SelectionBar(selectedCount: Int, onDelete: () -> Unit, onShare: () -> Unit, onClear: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 8.dp) {
        Row(Modifier.navigationBarsPadding().fillMaxWidth().padding(12.dp), Arrangement.SpaceAround) {
            SelectionAction(Icons.Default.Delete, "Delete", onDelete)
            SelectionAction(Icons.Default.Share, "Share", onShare)
            SelectionAction(Icons.Default.Add, "Add to", onClear)
        }
    }
}

@Composable
private fun SelectionAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(8.dp)) { Icon(icon, label); Text(label, style = MaterialTheme.typography.labelMedium) }
}

@Composable
private fun ProcessingCard(progress: IndexProgress) {
    val fraction = if (progress.total == 0) 0f else (progress.indexed.toFloat() / progress.total).coerceIn(0f, 1f)
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), color = Color.White) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${(fraction * 100).toInt()}% indexed", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            if (fraction >= 1f) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Indexing complete", modifier = Modifier.size(32.dp), tint = VisionaryColors.color007847)
            } else {
                CircularProgressIndicator(progress = { fraction }, modifier = Modifier.size(32.dp), color = VisionaryColors.color007847)
            }
        }
    }
}

@Composable
private fun PhotoEmptyState(query: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(if (query.isBlank()) Icons.Default.PhotoLibrary else Icons.Default.Search, null, Modifier.size(48.dp), tint = VisionaryColors.color496156); Spacer(Modifier.height(12.dp)); Text(if (query.isBlank()) "Your gallery is ready" else "No matching photos or videos", style = MaterialTheme.typography.titleMedium); if (query.isBlank()) Text("Photos will appear here after indexing.") else Text("Nothing found for \"$query\"") }
}

@Composable
private fun PhotoError(onRetry: () -> Unit) { Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Default.Settings, null); Text("Failed to load your gallery"); TextButton(onClick = onRetry) { Text("Retry") } } }

private fun MediaItem.matchesQuery(query: String): Boolean = listOf(fileName, uri, category.name, textPreview.orEmpty(), labels).any { it.contains(query.trim(), ignoreCase = true) }
private fun MediaItem.photoDateLabel(): String = if (epochDate(takenAtMs) == epochDate(Clock.System.now().toEpochMilliseconds())) "Today" else "${MONTH_NAMES[epochDate(takenAtMs).month - 1]}, ${epochDate(takenAtMs).year}"
private data class PhotoDate(val year: Int, val month: Int, val day: Int)
private fun epochDate(value: Long): PhotoDate { val days = value.floorDiv(86_400_000L); val z = days + 719468L; val era = (if (z >= 0) z else z - 146096L).floorDiv(146097L); val doe = z - era * 146097L; val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L; var year = (yoe + era * 400L).toInt(); val dayOfYear = doe - (365L * yoe + yoe / 4L - yoe / 100L); val monthPart = (5L * dayOfYear + 2L) / 153L; val day = (dayOfYear - (153L * monthPart + 2L) / 5L + 1L).toInt(); val month = (monthPart + if (monthPart < 10L) 3L else -9L).toInt(); if (month <= 2) year++; return PhotoDate(year, month, day) }
private val MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
