package com.o3.vanguard.visionary.features.gallery.presentation.albumgallery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import com.o3.vanguard.visionary.core.mvi.CollectEvents
import com.o3.vanguard.visionary.domain.model.MediaCategory
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.MediaItem
import kotlin.time.Clock
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AlbumGalleryRoute(
    albumId: String,
    albumName: String,
    onOpenViewer: (MediaId) -> Unit,
    onBack: () -> Unit,
    viewModel: AlbumGalleryViewModel = koinViewModel { parametersOf(albumId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is AlbumGalleryEvent.OpenViewer -> onOpenViewer(event.id)
            AlbumGalleryEvent.NavigateBack -> onBack()
        }
    }
    var menuOpen by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val groupedItems = remember(state.items) {
        state.items.sortedByDescending { it.takenAtMs }.groupBy { it.albumDateLabel() }
    }
    Surface(Modifier.fillMaxSize(), color = VisionaryColors.colorF4FDF7) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(84.dp).padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { viewModel.onIntent(AlbumGalleryIntent.OnBackClicked) },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(30.dp),
                        tint = VisionaryColors.color0B1B14,
                    )
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(
                        albumName,
                        fontSize = 16.sp,
                        lineHeight = 31.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        color = VisionaryColors.color0B1B14,
                    )
                    Text(
                        "${state.items.size} photos",
                        fontSize = 12.sp,
                        lineHeight = 26.sp,
                        color = VisionaryColors.color496156,
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Album options", tint = VisionaryColors.color0B1B14)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Sort by") }, onClick = { menuOpen = false })
                        DropdownMenuItem(text = { Text("Select") }, onClick = { menuOpen = false })
                        DropdownMenuItem(text = { Text("Manage album") }, onClick = { menuOpen = false })
                    }
                }
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    state.loading -> CircularProgressIndicator(color = VisionaryColors.color007847)
                    state.errorMessage != null -> Text(state.errorMessage ?: "Failed to load album")
                    state.items.isEmpty() -> Text("No photos in this album", color = VisionaryColors.color496156)
                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        groupedItems.forEach { (date, items) ->
                            item(key = "header_$date", span = { GridItemSpan(maxLineSpan) }) {
                                Text(date.uppercase(), Modifier.padding(top = 20.dp, bottom = 16.dp), fontWeight = FontWeight.Bold, letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing, color = VisionaryColors.color496156)
                            }
                            items(items, key = { it.id.value }) { item ->
                                AlbumMediaTile(item, enabled = !gridState.isScrollInProgress) {
                                    viewModel.onIntent(AlbumGalleryIntent.OnItemClicked(item.id))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumMediaTile(item: MediaItem, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp))
            .background(VisionaryColors.colorE4EEE8)
            .combinedClickable(enabled = enabled, onClick = onClick, onLongClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(model = item.uri, contentDescription = item.fileName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (item.category == MediaCategory.VIDEO) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Video", modifier = Modifier.size(38.dp), tint = Color.White)
        }
    }
}

private data class AlbumDate(val year: Int, val month: Int, val day: Int)

private fun MediaItem.albumDateLabel(): String {
    val date = albumDate(takenAtMs)
    val today = albumDate(Clock.System.now().toEpochMilliseconds())
    return if (date == today) "Today" else "${ALBUM_MONTHS[date.month - 1]}, ${date.year}"
}

private fun albumDate(value: Long): AlbumDate {
    val days = value.floorDiv(86_400_000L)
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
    return AlbumDate(year, month, day)
}

private val ALBUM_MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
