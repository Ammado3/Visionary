package com.o3.vanguard.visionary.features.gallery.di

import com.o3.vanguard.visionary.features.gallery.presentation.albumgallery.AlbumGalleryViewModel
import com.o3.vanguard.visionary.features.gallery.presentation.albums.AlbumsViewModel
import com.o3.vanguard.visionary.features.gallery.presentation.favorite.FavoriteViewModel
import com.o3.vanguard.visionary.features.gallery.presentation.photos.PhotoViewModel
import com.o3.vanguard.visionary.features.gallery.presentation.photoviewer.PhotoViewerViewModel
import com.o3.vanguard.visionary.features.gallery.presentation.trash.TrashViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val galleryModule = module {
    viewModelOf(::AlbumsViewModel)
    viewModelOf(::FavoriteViewModel)
    viewModelOf(::PhotoViewModel)
    viewModel { (albumId: String) ->
        AlbumGalleryViewModel(
            albumId = albumId,
            observeAlbumMediaUseCase = get(),
        )
    }
    viewModel { (mediaIdValue: Long, highlight: String?) ->
        PhotoViewerViewModel(
            mediaIdValue = mediaIdValue,
            highlight = highlight,
            moveToVaultUseCase = get(),
            observeItem = get(),
            deleteMediaUseCase = get(),
            setFavoriteUseCase = get(),
        )
    }
    viewModelOf(::TrashViewModel)
}
