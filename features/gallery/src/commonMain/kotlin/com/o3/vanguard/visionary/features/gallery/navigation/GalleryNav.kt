package com.o3.vanguard.visionary.features.gallery.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.o3.vanguard.visionary.core.designsystem.AppTab
import com.o3.vanguard.visionary.core.navigation.AppRoute
import com.o3.vanguard.visionary.core.navigation.Navigator
import com.o3.vanguard.visionary.core.navigation.navigateToTab
import com.o3.vanguard.visionary.features.gallery.presentation.albums.AlbumsRoute
import com.o3.vanguard.visionary.features.gallery.presentation.albumgallery.AlbumGalleryRoute
import com.o3.vanguard.visionary.features.gallery.presentation.photoviewer.PhotoViewerRoute
import com.o3.vanguard.visionary.features.gallery.presentation.trash.TrashBinRoute
import com.o3.vanguard.visionary.features.gallery.presentation.photos.PhotoRoute
import com.o3.vanguard.visionary.features.gallery.presentation.favorite.FavoriteRoute

fun EntryProviderScope<NavKey>.galleryEntries(navigator: Navigator) {
    entry<AppRoute.Photos> {
        PhotoRoute(
            onOpenViewer = { navigator.navigate(AppRoute.Viewer(it.value)) },
            onOpenSettings = { navigator.navigate(AppRoute.Settings) },
            onOpenAirTeleportReceiver = { navigator.navigate(AppRoute.TeleportReceiver) },
        )
    }
    entry<AppRoute.Albums> {
        AlbumsRoute(
            onOpenAlbum = { albumId, albumName -> navigator.navigate(AppRoute.Gallery(albumId, albumName)) },
            onPhotos = { navigator.navigateToTab(AppTab.PHOTOS) },
            onSearch = { navigator.navigateToTab(AppTab.SEARCH) },
            onVault = { navigator.navigateToTab(AppTab.VAULT) },
            onTrashBin = { navigator.navigate(AppRoute.TrashBin) },
            onFavorites = { navigator.navigate(AppRoute.Favorites) },
        )
    }
    entry<AppRoute.Gallery> { key ->
        AlbumGalleryRoute(
            albumId = key.albumId,
            albumName = key.albumName,
            onOpenViewer = { navigator.navigate(AppRoute.Viewer(it.value)) },
            onBack = { navigator.back() },
        )
    }
    entry<AppRoute.Viewer> { key ->
        PhotoViewerRoute(
            mediaId = key.mediaId,
            highlight = key.highlight,
            onBack = { navigator.back() },
            onShareToTeleport = { item ->
                navigator.navigate(
                    AppRoute.TeleportSender(
                        uri = item.uri,
                        displayName = item.fileName.ifBlank { "photo.jpg" },
                        mimeType = "image/jpeg",
                        sizeBytes = 0L,
                    ),
                )
            },
        )
    }
    entry<AppRoute.TrashBin> {
        TrashBinRoute(
            onBack = { navigator.back() },
        )
    }
    entry<AppRoute.Favorites> {
        FavoriteRoute(
            onOpenViewer = { navigator.navigate(AppRoute.Viewer(it.value)) },
            onBack = { navigator.back() },
        )
    }
}
