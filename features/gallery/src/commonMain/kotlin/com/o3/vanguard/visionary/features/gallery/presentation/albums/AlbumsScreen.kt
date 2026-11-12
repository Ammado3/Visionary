package com.o3.vanguard.visionary.features.gallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import coil3.compose.AsyncImage
import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.core.designsystem.AppBottomNavigation
import com.o3.vanguard.visionary.core.designsystem.AppTab
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
                        ShortcutCard("Trash Bin", "View items", Icons.Default.DeleteOutline, onClick = onTrashBin)
                        ShortcutCard("Favorites", "0 files", Icons.Default.FavoriteBorder)
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("On this device", style = MaterialTheme.typography.bodyMedium, color = VisionaryColors.color496156)
                    Spacer(Modifier.height(16.dp))
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
                                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(MaterialTheme.shapes.small),
                                )
                                Text(album.name, fontWeight = FontWeight.SemiBold, color = VisionaryColors.color0B1B14)
                                Text(
                                    text = if (album.videoCount > 0) {
                                        "${album.itemCount} items • ${album.videoCount} videos"
                                    } else {
                                        "${album.itemCount} photos"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
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
            Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 12.dp),
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
            .height(80.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Albums", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = VisionaryColors.color0B1B14)
        Row {
            IconButton(onClick = onSearch) { Icon(Icons.Default.Search, "Search", tint = VisionaryColors.color0B1B14) }
            IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, "More options", tint = VisionaryColors.color0B1B14) }
        }
    }
}

@Composable
private fun RowScope.ShortcutCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier.weight(1f).clickable(onClick = onClick),
        color = VisionaryColors.color72DFE7F3,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, VisionaryColors.color00000000),
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, Modifier.size(32.dp), tint = VisionaryColors.color496156)
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = VisionaryColors.color0B1B14)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = VisionaryColors.color496156)
            }
        }
    }
}
