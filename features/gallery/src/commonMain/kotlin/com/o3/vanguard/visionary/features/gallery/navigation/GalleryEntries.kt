package com.o3.vanguard.visionary.features.gallery.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.o3.vanguard.visionary.core.navigation.AppRoute
import com.o3.vanguard.visionary.core.navigation.Navigator
import com.o3.vanguard.visionary.features.gallery.presentation.albums.AlbumsRoute
import com.o3.vanguard.visionary.features.gallery.presentation.gallery.GalleryRoute
import com.o3.vanguard.visionary.features.gallery.presentation.gallery.ViewerRoute
import com.o3.vanguard.visionary.features.gallery.presentation.trash.TrashBinRoute

fun EntryProviderScope<NavKey>.galleryEntries(navigator: Navigator) {
    entry<AppRoute.Albums> {
        AlbumsRoute(
            onOpenAlbum = { navigator.navigate(AppRoute.Gallery(it.id)) },
            onPhotos = { navigator.replaceTop(AppRoute.Home) },
            onSearch = { navigator.replaceTop(AppRoute.Search()) },
            onVault = { navigator.replaceTop(AppRoute.VaultLock) },
            onTrashBin = { navigator.navigate(AppRoute.TrashBin) },
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
        )
    }
    entry<AppRoute.TrashBin> {
        TrashBinRoute(
            onBack = { navigator.back() },
        )
    }
}
