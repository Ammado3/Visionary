package com.o3.vanguard.visionary.features.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.o3.vanguard.visionary.core.mvi.CollectEvents
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import com.o3.vanguard.visionary.core.designsystem.AppBottomNavigation
import com.o3.vanguard.visionary.core.designsystem.AppTab
import com.o3.vanguard.visionary.domain.model.CollectionKind
import com.o3.vanguard.visionary.domain.model.IndexProgress
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.MediaCategory
import com.o3.vanguard.visionary.domain.model.MediaItem
import kotlin.time.Clock
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeRoute(
    onOpenCollection: (CollectionKind) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenViewer: (MediaId) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAirTeleportReceiver: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is HomeEvent.OpenCollection -> onOpenCollection(event.kind)
            is HomeEvent.OpenViewer -> onOpenViewer(event.id)
            HomeEvent.OpenSearch -> onOpenSearch()
            HomeEvent.OpenAlbums -> onOpenAlbums()
            HomeEvent.OpenScanner -> onOpenScanner()
            HomeEvent.OpenVault -> onOpenVault()
            HomeEvent.OpenSettings -> onOpenSettings()
        }
    }
    HomeScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onAirTeleportReceiverClick = onOpenAirTeleportReceiver,
    )
}

@Composable
fun HomeScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    onAirTeleportReceiverClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    Surface(modifier = modifier.fillMaxSize(), color = VisionaryColors.colorF4FDF7) {
        Column(Modifier.fillMaxSize()) {
            HomeHeader(
                onMenuClick = { onIntent(HomeIntent.OnSettingsClicked) },
                searchOpen = searchOpen,
                query = searchQuery,
                onSearchClick = { searchOpen = true },
                onQueryChanged = { searchQuery = it },
                onCloseSearch = {
                    searchQuery = ""
                    searchOpen = false
                },
                onAirTeleportReceiverClick = onAirTeleportReceiverClick,
            )
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    state.loading -> CircularProgressIndicator(color = VisionaryColors.color007847)
                    state.error -> HomeError(onRetry = { onIntent(HomeIntent.OnRetryClicked) })
                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        state.indexProgress?.let { progress ->
                            item(span = { GridItemSpan(maxLineSpan) }) { ProcessingCard(progress) }
                        }

                        val media = state.recentFinds
                            .filter { searchQuery.isBlank() || it.matchesGalleryQuery(searchQuery) }
                            .sortedByDescending { it.takenAtMs }
                        if (media.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                if (searchQuery.isBlank()) {
                                    EmptyHomeState()
                                } else {
                                    SearchEmptyState(searchQuery)
                                }
                            }
                        } else {
                            groupHomeMedia(media).forEach { section ->
                                item(
                                    key = "header_${section.title}",
                                    span = { GridItemSpan(maxLineSpan) },
                                ) {
                                    val labelMedium = MaterialTheme.typography.labelMedium
                                    val ls = labelMedium.letterSpacing
                                    Text(
                                        text = section.title.uppercase(),
                                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                                        style = labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = VisionaryColors.color496156,
                                        letterSpacing = if (ls.isUnspecified) 0.sp else ls * 1.4f,
                                    )
                                }
                                items(section.items, key = { it.id.value }) { item ->
                                    MediaTile(item, onIntent)
                                }
                            }
                        }
                    }
                }
            }
            AppBottomNavigation(
                selected = AppTab.PHOTOS,
                onSelected = { tab ->
                    when (tab) {
                        AppTab.PHOTOS -> {}
                        AppTab.ALBUMS -> onIntent(HomeIntent.OnAlbumsClicked)
                        AppTab.SEARCH -> onIntent(HomeIntent.OnSearchClicked)
                        AppTab.VAULT -> onIntent(HomeIntent.OnVaultClicked)
                    }
                },
            )
        }
    }
}

private data class HomeMediaSection(val title: String, val items: List<MediaItem>)

private fun groupHomeMedia(media: List<MediaItem>): List<HomeMediaSection> {
    val today = epochDate(Clock.System.now().toEpochMilliseconds())
    val grouped = linkedMapOf<String, MutableList<MediaItem>>()

    media.forEach { item ->
        val date = epochDate(item.takenAtMs)
        val title = if (date == today) {
            "Today"
        } else {
            "${MONTH_NAMES[date.month - 1]} ${date.year}"
        }
        grouped.getOrPut(title) { mutableListOf() }.add(item)
    }

    return grouped.map { (title, items) -> HomeMediaSection(title, items) }
}

private data class EpochDate(val year: Int, val month: Int, val day: Int)

/** Converts epoch milliseconds to a UTC calendar date without platform-specific date APIs. */
private fun epochDate(epochMillis: Long): EpochDate {
    val days = epochMillis.floorDiv(MILLIS_PER_DAY)
    val z = days + 719468L
    val era = (if (z >= 0) z else z - 146096L).floorDiv(146097L)
    val doe = z - era * 146097L
    val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L
    var year = (yoe + era * 400L).toInt()
    val dayOfYear = doe - (365L * yoe + yoe / 4L - yoe / 100L)
    val monthPart = (5L * dayOfYear + 2L) / 153L
    val day = (dayOfYear - (153L * monthPart + 2L) / 5L + 1L).toInt()
    val month = (monthPart + if (monthPart < 10L) 3L else -9L).toInt()
    if (month <= 2) year++
    return EpochDate(year, month, day)
}

private const val MILLIS_PER_DAY = 86_400_000L
private val MONTH_NAMES = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

@Composable
private fun HomeHeader(
    onMenuClick: () -> Unit,
    searchOpen: Boolean,
    query: String,
    onSearchClick: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onAirTeleportReceiverClick: () -> Unit = {},
) {
    if (searchOpen) {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCloseSearch) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                singleLine = true,
                placeholder = { Text("Search photos and videos") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { onQueryChanged("") }) { Icon(Icons.Default.Clear, "Clear search") } }
                } else null,
                shape = RoundedCornerShape(32.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = VisionaryColors.colorF4FDF7,
                    unfocusedContainerColor = VisionaryColors.colorF4FDF7,
                ),
            )
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(VisionaryColors.colorF4FDF7)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.Menu, contentDescription = "Open navigation", tint = VisionaryColors.color0B1B14)
        }
        Text(
            text = "Gallery",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = VisionaryColors.color0B1B14,
        )
        IconButton(onClick = onAirTeleportReceiverClick) {
            Icon(Icons.Default.Share, contentDescription = "Air Teleport Receiver", tint = VisionaryColors.color007847)
        }
        IconButton(onClick = onSearchClick) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = VisionaryColors.color0B1B14)
        }
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = VisionaryColors.color0B1B14)
        }
    }
}

private fun MediaItem.matchesGalleryQuery(query: String): Boolean {
    val normalizedQuery = query.trim().lowercase()
    if (normalizedQuery.isEmpty()) return true
    val searchableText = listOf(
        fileName,
        uri,
        category.name,
        textPreview.orEmpty(),
        labels,
        takenAtMs.toString(),
        epochDate(takenAtMs).let { "${MONTH_NAMES[it.month - 1]} ${it.year}" },
    )
    return searchableText.any { it.lowercase().contains(normalizedQuery) }
}

@Composable
private fun ProcessingCard(progress: IndexProgress) {
    val fraction = if (progress.total == 0) 0f else progress.indexed.toFloat() / progress.total
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = VisionaryColors.colorFFFFFFFF,
        border = BorderStroke(1.dp, VisionaryColors.colorCEDED3),
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = when {
                    progress.pending > 0 -> "Tracing text from images"
                    progress.failed > 0 -> "Some images need attention"
                    else -> "Gallery is up to date"
                },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = VisionaryColors.color0B1B14,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { fraction.coerceIn(0f, 1f) },
                    modifier = Modifier.size(32.dp),
                    color = VisionaryColors.color007847,
                    trackColor = VisionaryColors.colorCEDED3,
                    strokeWidth = 3.dp,
                )
                Text(
                    text = "${(fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = VisionaryColors.color0B1B14,
                )
            }
        }
    }
}

@Composable
private fun MediaTile(item: MediaItem, onIntent: (HomeIntent) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (item.category == MediaCategory.VIDEO) VisionaryColors.color24352D else VisionaryColors.colorE4EEE8)
            .clickable { onIntent(HomeIntent.OnRecentClicked(item.id)) },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (item.category == MediaCategory.VIDEO) {
            Surface(
                shape = RoundedCornerShape(50),
                color = VisionaryColors.colorFF000000.copy(alpha = 0.55f),
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    modifier = Modifier.padding(6.dp).size(24.dp),
                    tint = VisionaryColors.colorFFFFFFFF,
                )
            }
        }
    }
}



@Composable
private fun EmptyHomeState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = VisionaryColors.color496156, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("Your gallery is ready", style = MaterialTheme.typography.titleMedium, color = VisionaryColors.color0B1B14)
        Text("Photos will appear here after indexing.", color = VisionaryColors.color496156)
    }
}

@Composable
private fun SearchEmptyState(query: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = VisionaryColors.color496156, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("No matching photos or videos", style = MaterialTheme.typography.titleMedium, color = VisionaryColors.color0B1B14)
        Text("Nothing found for \"$query\"", color = VisionaryColors.color496156)
    }
}

@Composable
private fun HomeError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Settings, contentDescription = null, tint = VisionaryColors.color496156, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text("Failed to load your gallery", color = VisionaryColors.color0B1B14)
        androidx.compose.material3.TextButton(onClick = onRetry) { Text("Retry", color = VisionaryColors.color007847) }
    }
}
