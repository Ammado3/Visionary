package com.o3.vanguard.visionary.features.gallery.di

import com.o3.vanguard.visionary.features.gallery.GalleryViewModel
import com.o3.vanguard.visionary.features.gallery.ViewerViewModel
import com.o3.vanguard.visionary.features.gallery.TrashViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val galleryModule = module {
    viewModel { (kindId: String) -> GalleryViewModel(kindId, get()) }
    viewModel { (mediaIdValue: Long, highlight: String?) ->
        ViewerViewModel(
            mediaIdValue,
            highlight,
            get(),
            get(),
            get()
        )
    }
    viewModel { TrashViewModel(get(), get()) }
}
