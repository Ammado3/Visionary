package com.o3.vanguard.visionary.features.gallery.presentation.albumgallery

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.domain.usecase.ObserveAlbumMediaUseCase
import kotlinx.coroutines.launch

class AlbumGalleryViewModel(
    albumId: String,
    private val observeAlbumMediaUseCase: ObserveAlbumMediaUseCase,
) : BaseMviViewModel<AlbumGalleryState, AlbumGalleryIntent, AlbumGalleryEvent>(
    AlbumGalleryState(albumId = albumId),
) {
    init {
        viewModelScope.launch {
            try {
                observeAlbumMediaUseCase(albumId).collect { items ->
                    updateState { copy(items = items, loading = false, errorMessage = null) }
                }
            } catch (error: Throwable) {
                updateState { copy(loading = false, errorMessage = error.message ?: "Failed to load album") }
            }
        }
    }

    override fun onIntent(intent: AlbumGalleryIntent) {
        when (intent) {
            is AlbumGalleryIntent.OnItemClicked -> sendEvent(AlbumGalleryEvent.OpenViewer(intent.id))
            AlbumGalleryIntent.OnBackClicked -> sendEvent(AlbumGalleryEvent.NavigateBack)
            else -> Unit
        }
    }
}
