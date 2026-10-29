@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.o3.vanguard.visionary.features.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.MediaItem
import com.o3.vanguard.visionary.domain.model.SearchHit
import com.o3.vanguard.visionary.domain.model.SearchMode
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SearchRoute(
    initialQuery: String,
    onOpenViewer: (MediaId, String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = koinViewModel { parametersOf(initialQuery) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEvents(viewModel.events) { event ->
        when (event) {
            is SearchEvent.OpenViewer -> {
                val item = state.results.firstOrNull { it.item.id == event.id }?.item
                    ?: state.allImages.firstOrNull { it.id == event.id }
                if (item != null) {
                    viewModel.onIntent(SearchIntent.OnHitClicked(item))
                } else {
                    onOpenViewer(event.id, event.highlight)
                }
            }
            SearchEvent.NavigateBack -> onBack()
            is SearchEvent.ShowToast -> { }
        }
    }

    SearchScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun SearchScreen(
    state: SearchState,
    onIntent: (SearchIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayImages = if (state.query.isNotBlank() && state.results.isNotEmpty()) {
        state.results.map { it.item }
    } else if (state.query.isBlank()) {
        state.allImages
    } else {
        emptyList()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("AI Gallery Search", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { onIntent(SearchIntent.OnReindexClicked) },
                        enabled = !state.isIndexing
                    ) {
                        if (state.isIndexing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Reindex Gallery")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search field
            SearchBar(
                query = state.query,
                onQueryChange = { onIntent(SearchIntent.OnQueryChanged(it)) },
                isSearching = state.isSearching
            )

            // Search mode selector
            SearchModeSelector(
                currentMode = state.searchMode,
                onModeChange = { onIntent(SearchIntent.OnSearchModeChanged(it)) }
            )

            // Indexing progress card
            AnimatedVisibility(visible = state.isIndexing && state.indexProgress != null) {
                state.indexProgress?.let { progress ->
                    val frac = if (progress.total > 0) progress.indexed.toFloat() / progress.total.toFloat() else 0f
                    IndexingProgressCard(
                        progress = frac,
                        message = "${progress.indexed} indexed • ${progress.pending} remaining"
                    )
                }
            }

            // Results grid or empty/error states
            if (displayImages.isEmpty() && !state.isIndexing) {
                state.errorMessage?.let { error ->
                    ErrorCard(
                        error = error,
                        onDismiss = { onIntent(SearchIntent.OnClearError) }
                    )
                } ?: run {
                    EmptyStateView(
                        hasSearchQuery = state.query.isNotBlank(),
                        searchMode = state.searchMode
                    )
                }
            } else {
                ImageGrid(
                    images = displayImages,
                    searchResults = if (state.query.isNotBlank()) state.results else null,
                    onImageClick = { item -> onIntent(SearchIntent.OnHitClicked(item)) }
                )
            }
        }

        // Full Screen Image Detail Viewer
        state.selectedImage?.let { image ->
            ImageDetailViewer(
                image = image,
                onDismiss = { onIntent(SearchIntent.OnDismissDetail) },
                onDeleteImage = { onIntent(SearchIntent.OnDeleteImage(image)) }
            )
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearching: Boolean
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search: 'dogs', 'sunset', 'receipt'...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun SearchModeSelector(
    currentMode: SearchMode,
    onModeChange: (SearchMode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                "Search Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SearchMode.entries.toTypedArray()) { mode ->
                    FilterChip(
                        selected = mode == currentMode,
                        onClick = { onModeChange(mode) },
                        label = {
                            Text(
                                text = when (mode) {
                                    SearchMode.EXACT -> "Exact"
                                    SearchMode.PREFIX -> "Prefix"
                                    SearchMode.CONTAINS -> "Contains"
                                    SearchMode.FUZZY -> "Fuzzy"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = if (mode == currentMode) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when (currentMode) {
                    SearchMode.EXACT -> "Only exact label matches (e.g., 'dog' matches only 'dog')"
                    SearchMode.PREFIX -> "Labels starting with query (e.g., 'hand' matches 'handbag')"
                    SearchMode.CONTAINS -> "Labels containing query anywhere (e.g., 'hand' matches 'secondhand')"
                    SearchMode.FUZZY -> "Smart matching with AI similarity (best for natural search)"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun IndexingProgressCard(
    progress: Float,
    message: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Indexing Gallery...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            if (message.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
fun EmptyStateView(
    hasSearchQuery: Boolean,
    searchMode: SearchMode
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (hasSearchQuery) {
                    "No results found in ${searchMode.name.lowercase()} mode"
                } else {
                    "Your gallery is ready. Try searching for text inside screenshots or photos!"
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ImageGrid(
    images: List<MediaItem>,
    searchResults: List<SearchHit>? = null,
    onImageClick: (MediaItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(images, key = { it.id.value }) { image ->
            val hit = searchResults?.find { it.item.id == image.id }
            ImageCard(
                image = image,
                relevanceScore = hit?.relevanceScore,
                onImageClick = onImageClick
            )
        }
    }
}

@Composable
fun ImageCard(
    image: MediaItem,
    relevanceScore: Float? = null,
    onImageClick: (MediaItem) -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }

    Card(
        onClick = { onImageClick(image) },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = image.uri,
                contentDescription = image.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Relevance score badge
            relevanceScore?.let { score ->
                if (score > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${(score * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            val textPreview = image.textPreview
            // OCR Text / Info Badge
            if (!textPreview.isNullOrBlank()) {
                Surface(
                    onClick = { showDetails = !showDetails },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.92f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OCR Text",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            } else if (image.labels.isNotBlank()) {
                Surface(
                    onClick = { showDetails = !showDetails },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Show Details",
                        modifier = Modifier
                            .padding(4.dp)
                            .size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Image details overlay
            this@Card.AnimatedVisibility(
                visible = showDetails,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        if (image.labels.isNotEmpty()) {
                            Text(
                                text = "Labels: " + image.labels.split(",").take(3).joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!textPreview.isNullOrBlank()) {
                            Text(
                                text = "Text: " + textPreview.replace("\n", " "),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageDetailViewer(
    image: MediaItem,
    onDismiss: () -> Unit,
    onDeleteImage: (MediaItem) -> Unit
) {
    var showOcrTextSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val detailTextPreview = image.textPreview

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                // Center Image View
                AsyncImage(
                    model = image.uri,
                    contentDescription = image.fileName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Navigation Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Back", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = image.fileName.ifBlank { "Photo" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color.White
                            )
                        }
                    }

                    val detailTextPreview = image.textPreview
                    // Optional Pill Badge ("OCR Text")
                    if (!detailTextPreview.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                onClick = { showOcrTextSheet = !showOcrTextSheet },
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.85f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "OCR Text Available",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.Black
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // OCR Text Details Sheet Overlay
                AnimatedVisibility(
                    visible = showOcrTextSheet,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.DarkGray.copy(alpha = 0.95f),
                        contentColor = Color.White
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Extracted Text (OCR)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = detailTextPreview.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Bottom Action Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    color = Color.Black.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomViewerActionItem(
                            icon = Icons.Default.Share,
                            label = "Share",
                            onClick = { }
                        )

                        BottomViewerActionItem(
                            icon = Icons.Default.Edit,
                            label = "Edit",
                            onClick = { }
                        )

                        BottomViewerActionItem(
                            icon = Icons.Default.Add,
                            label = "Add to",
                            onClick = { }
                        )

                        BottomViewerActionItem(
                            icon = Icons.Default.Delete,
                            label = "Bin",
                            onClick = { showDeleteConfirmDialog = true }
                        )
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
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteImage(image)
                        onDismiss()
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
}

@Composable
fun BottomViewerActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White,
                fontSize = 12.sp
            )
        )
    }
}
