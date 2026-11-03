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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.o3.vanguard.visionary.core.mvi.CollectEvents
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
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
    HomeScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun HomeScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = VisionaryColors.colorF4FDF7) {
        Column(Modifier.fillMaxSize()) {
            HomeHeader(
                onMenuClick = { onIntent(HomeIntent.OnSettingsClicked) },
                onSearchClick = { onIntent(HomeIntent.OnSearchClicked) },
            )
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    state.loading -> CircularProgressIndicator(color = VisionaryColors.color007847)
                    state.error -> HomeError(onRetry = { onIntent(HomeIntent.OnRetryClicked) })
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        state.indexProgress?.let { progress ->
                            item { ProcessingCard(progress) }
                        }

                        val media = state.recentFinds.sortedByDescending { it.takenAtMs }
                        if (media.isEmpty()) {
                            item { EmptyHomeState() }
                        } else {
                            groupHomeMedia(media).forEach { section ->
                                item(key = section.title) {
                                    MediaSection(section.title, section.items, onIntent)
                                }
                            }
                        }
                    }
                }
            }
            HomeBottomNavigation(
                onPhotos = { },
                onAlbums = { onIntent(HomeIntent.OnAlbumsClicked) },
                onSearch = { onIntent(HomeIntent.OnSearchClicked) },
                onVault = { onIntent(HomeIntent.OnVaultClicked) },
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
private fun HomeHeader(onMenuClick: () -> Unit, onSearchClick: () -> Unit) {
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
        IconButton(onClick = onSearchClick) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = VisionaryColors.color0B1B14)
        }
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = VisionaryColors.color0B1B14)
        }
    }
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
private fun MediaSection(
    title: String,
    media: List<MediaItem>,
    onIntent: (HomeIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = VisionaryColors.color496156,
            letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing * 1.4f,
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .height(((media.size + 2) / 3 * 112).dp),
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(media, key = { it.id.value }) { item ->
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
        }
    }
}

@Composable
private fun HomeBottomNavigation(
    onPhotos: () -> Unit,
    onAlbums: () -> Unit,
    onSearch: () -> Unit,
    onVault: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = VisionaryColors.colorF4FDF7,
        border = BorderStroke(1.dp, VisionaryColors.colorCEDED3),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            NavigationItem("Photos", Icons.Default.PhotoLibrary, true, onPhotos)
            NavigationItem("Albums", Icons.Default.Album, false, onAlbums)
            NavigationItem("AI Search", Icons.Default.Search, false, onSearch)
            NavigationItem("Vault", Icons.Default.Lock, false, onVault)
        }
    }
}

@Composable
private fun RowScope.NavigationItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .height(28.dp)
                .background(
                    color = if (selected) VisionaryColors.color007847 else VisionaryColors.color00000000,
                    shape = RoundedCornerShape(50),
                )
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = if (selected) VisionaryColors.colorFFFFFFFF else VisionaryColors.color496156,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) VisionaryColors.color007847 else VisionaryColors.color496156,
        )
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
