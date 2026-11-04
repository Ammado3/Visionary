package com.o3.vanguard.visionary.features.gallery

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.o3.vanguard.visionary.core.navigation.AppRoute
import com.o3.vanguard.visionary.core.navigation.Navigator

fun EntryProviderScope<NavKey>.galleryEntries(navigator: Navigator) {
    entry<AppRoute.Albums> {
        AlbumsRoute(
            onOpenAlbum = { navigator.navigate(AppRoute.Gallery(it.id)) },
            onPhotos = { navigator.replaceTop(AppRoute.Home) },
            onSearch = { navigator.replaceTop(AppRoute.Search()) },
            onVault = { navigator.replaceTop(AppRoute.VaultLock) },
        )
    }
    entry<AppRoute.Gallery> { key ->
        GalleryRoute(
            kindId = key.collectionId,
            onOpenViewer = { navigator.navigate(AppRoute.Viewer(it.value)) },
            onBack = { navigator.back() },
        )
    }
    entry<AppRoute.Viewer> { key ->
        ViewerRoute(
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
}
