package com.o3.vanguard.visionary.features.gallery

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val galleryModule = module {
    viewModel { (kindId: String) -> GalleryViewModel(kindId, get()) }
    viewModel { (mediaIdValue: Long, highlight: String?) -> ViewerViewModel(mediaIdValue, highlight, get(), get()) }
}
