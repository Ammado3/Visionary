package com.o3.vanguard.visionary.features.gallery.di

import com.o3.vanguard.visionary.features.gallery.presentation.albums.AlbumsViewModel
import com.o3.vanguard.visionary.features.gallery.presentation.albumgallery.AlbumGalleryViewModel
import com.o3.vanguard.visionary.features.gallery.presentation.photoviewer.PhotoViewerViewModel
import com.o3.vanguard.visionary.features.gallery.presentation.photos.PhotoViewModel
import com.o3.vanguard.visionary.features.gallery.presentation.favorite.FavoriteViewModel
import com.o3.vanguard.visionary.features.gallery.presentation.trash.TrashViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val galleryModule = module {
    viewModel { AlbumsViewModel(get(), get()) }
    viewModel { FavoriteViewModel(get()) }
    viewModel { PhotoViewModel(get(), get(), get(), get(), get()) }
        viewModel { (albumId: String) -> AlbumGalleryViewModel(albumId, get()) }
    viewModel { (mediaIdValue: Long, highlight: String?) ->
        PhotoViewerViewModel(
            mediaIdValue,
            highlight,
            get(),
            get(),
            get(),
            get()
        )
    }
    viewModel { TrashViewModel(get(), get()) }
}
