package com.o3.vanguard.visionary.features.gallery.presentation.favorite

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import com.o3.vanguard.visionary.domain.model.MediaCategory
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.MediaItem
import kotlin.time.Clock
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FavoriteRoute(
    onOpenViewer: (MediaId) -> Unit,
    onBack: () -> Unit,
    viewModel: FavoriteViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FavoriteScreen(state = state, onBack = onBack, onOpenViewer = onOpenViewer)
}

@Composable
fun FavoriteScreen(
    state: FavoriteState,
    onBack: () -> Unit,
    onOpenViewer: (MediaId) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val grouped = remember(state.items) { state.items.sortedByDescending { it.takenAtMs }.groupBy { it.favoriteDateLabel() } }
    Surface(Modifier.fillMaxSize(), color = VisionaryColors.colorF4FDF7) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = VisionaryColors.color0B1B14) }
                Column(Modifier.padding(start = 8.dp)) {
                    Text("Favorites", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = VisionaryColors.color0B1B14)
                    Text("${state.items.size} files", style = MaterialTheme.typography.bodyLarge, color = VisionaryColors.color496156)
                }
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    state.loading -> CircularProgressIndicator(color = VisionaryColors.color007847)
                    state.error != null -> Text(state.error, color = MaterialTheme.colorScheme.error)
                    state.items.isEmpty() -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Star, null, tint = VisionaryColors.color496156)
                        Text("No favorite files yet", color = VisionaryColors.color496156)
                    }
                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(3), state = gridState, modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        grouped.forEach { (date, items) ->
                            item(key = "favorite_$date", span = { GridItemSpan(maxLineSpan) }) {
                                Text(date.uppercase(), Modifier.padding(top = 16.dp, bottom = 12.dp), fontWeight = FontWeight.Bold, color = VisionaryColors.color496156)
                            }
                            items(items, key = { it.id.value }) { item ->
                                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).clickable(enabled = !gridState.isScrollInProgress) { onOpenViewer(item.id) }, contentAlignment = Alignment.Center) {
                                    AsyncImage(item.uri, item.fileName, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    if (item.category == MediaCategory.VIDEO) Icon(Icons.Default.PlayArrow, "Video", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class FavoriteDate(val year: Int, val month: Int, val day: Int)
private fun MediaItem.favoriteDateLabel(): String {
    val date = favoriteDate(takenAtMs)
    val today = favoriteDate(Clock.System.now().toEpochMilliseconds())
    return if (date == today) "Today" else "${FAVORITE_MONTHS[date.month - 1]}, ${date.year}"
}
private fun favoriteDate(value: Long): FavoriteDate {
    val days = value.floorDiv(86_400_000L); val z = days + 719468L; val era = (if (z >= 0) z else z - 146096L).floorDiv(146097L); val doe = z - era * 146097L; val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L; var year = (yoe + era * 400L).toInt(); val dayOfYear = doe - (365L * yoe + yoe / 4L - yoe / 100L); val monthPart = (5L * dayOfYear + 2L) / 153L; val day = (dayOfYear - (153L * monthPart + 2L) / 5L + 1L).toInt(); val month = (monthPart + if (monthPart < 10L) 3L else -9L).toInt(); if (month <= 2) year++; return FavoriteDate(year, month, day)
}
private val FAVORITE_MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
