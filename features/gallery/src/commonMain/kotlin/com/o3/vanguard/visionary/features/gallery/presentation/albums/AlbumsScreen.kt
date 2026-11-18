package com.o3.vanguard.visionary.features.gallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import com.o3.vanguard.visionary.features.gallery.generated.resources.Res
import com.o3.vanguard.visionary.features.gallery.generated.resources.trash
import com.o3.vanguard.visionary.features.gallery.generated.resources.favorite
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import coil3.compose.AsyncImage
import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.core.designsystem.AppBottomNavigation
import com.o3.vanguard.visionary.core.designsystem.AppTab
import com.o3.vanguard.visionary.core.designsystem.AppTypography
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import com.o3.vanguard.visionary.domain.model.CollectionKind
import com.o3.vanguard.visionary.domain.model.StorageAlbum
import com.o3.vanguard.visionary.domain.usecase.ObserveStorageAlbumsUseCase
import org.koin.compose.koinInject

@Composable
fun AlbumsRoute(
    onOpenAlbum: (CollectionKind) -> Unit,
    onPhotos: () -> Unit,
    onSearch: () -> Unit,
    onVault: () -> Unit,
    onTrashBin: () -> Unit,
) {
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val observeAlbums = koinInject<ObserveStorageAlbumsUseCase>()
    val albumsResult by produceState<AppResult<List<StorageAlbum>>?>(initialValue = null, key1 = observeAlbums) {
        value = observeAlbums()
    }
    val albums = (albumsResult as? AppResult.Ok<List<StorageAlbum>>)?.value.orEmpty()

    Surface(Modifier.fillMaxSize(), color = VisionaryColors.colorF4FDF7) {
        Column(Modifier.fillMaxSize()) {
            AlbumsHeader(
                searchOpen = searchOpen,
                query = searchQuery,
                onSearch = { searchOpen = true },
                onQueryChanged = { searchQuery = it },
                onCloseSearch = {
                    searchQuery = ""
                    searchOpen = false
                },
            )
            if (albumsResult == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VisionaryColors.color007847)
                }
            } else {
                Column(Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 24.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShortcutCard(
                            title = "Trash Bin",
                            subtitle = "View items",
                            icon = Res.drawable.trash,
                            iconBackground = androidx.compose.ui.graphics.Color(0xFFD3F0F5),
                            onClick = onTrashBin,
                        )
                        ShortcutCard(
                            title = "Favorites",
                            subtitle = "0 files",
                            icon = Res.drawable.favorite,
                            iconBackground = androidx.compose.ui.graphics.Color(0xFFE7E0F2),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "On this device",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            letterSpacing = 0.sp,
                        ),
                        color = VisionaryColors.color496156,
                    )
                    Spacer(Modifier.height(8.dp))
                    val visibleAlbums = albums.filter { album ->
                        searchQuery.isBlank() || album.name.contains(searchQuery.trim(), ignoreCase = true)
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (visibleAlbums.isEmpty() && searchQuery.isNotBlank()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                AlbumSearchEmptyState(searchQuery)
                            }
                        }
                        items(visibleAlbums, key = { it.id }) { album ->
                            Column(
                                modifier = Modifier.clickable { onOpenAlbum(CollectionKind.ALL) },
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                AsyncImage(
                                    model = album.coverUri,
                                    contentDescription = album.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(8.dp)),
                                )
                                Text(
                                    album.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = VisionaryColors.color0B1B14,
                                )
                                Text(
                                    text = if (album.videoCount > 0) {
                                        "${album.itemCount} items • ${album.videoCount} videos"
                                    } else {
                                        "${album.itemCount} photos"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                                    color = VisionaryColors.color496156,
                                )
                            }
                        }
                    }
                }
            }
            AppBottomNavigation(
                selected = AppTab.ALBUMS,
                onSelected = { tab ->
                    when (tab) {
                        AppTab.PHOTOS -> onPhotos()
                        AppTab.SEARCH -> onSearch()
                        AppTab.VAULT -> onVault()
                        AppTab.ALBUMS -> Unit
                    }
                },
            )
        }
    }
}

@Composable
private fun AlbumSearchEmptyState(query: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = VisionaryColors.color496156, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("No albums found", style = MaterialTheme.typography.titleMedium, color = VisionaryColors.color0B1B14)
        Text("Nothing found for \"$query\"", color = VisionaryColors.color496156)
    }
}

@Composable
private fun AlbumsHeader(
    searchOpen: Boolean,
    query: String,
    onSearch: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onCloseSearch: () -> Unit,
) {
    if (searchOpen) {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCloseSearch) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close search")
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                singleLine = true,
                placeholder = { Text("Search albums") },
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
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(
                    color = VisionaryColors.colorCEDED3,
                    start = Offset(0f, size.height - stroke / 2),
                    end = Offset(size.width, size.height - stroke / 2),
                    strokeWidth = stroke,
                )
            }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Albums",
            style = AppTypography.screenTitle,
            color = VisionaryColors.color0B1B14,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(onClick = onSearch, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Search, "Search", tint = VisionaryColors.color0B1B14)
            }
            IconButton(onClick = {}, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.MoreVert, "More options", tint = VisionaryColors.color0B1B14)
            }
        }
    }
}

@Composable
private fun RowScope.ShortcutCard(
    title: String,
    subtitle: String,
    icon: DrawableResource,
    iconBackground: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier = Modifier.weight(1f).clickable(onClick = onClick),
        color = VisionaryColors.color72DFE7F3,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, VisionaryColors.color00000000),
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(50),
                color = iconBackground,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = VisionaryColors.color0B1B14,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, lineHeight = 17.sp),
                    fontWeight = FontWeight.Bold,
                    color = VisionaryColors.color0B1B14,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                    color = VisionaryColors.color496156,
                )
            }
        }
    }
}
