package com.o3.vanguard.visionary.features.gallery.presentation.albumgallery


import androidx.compose.runtime.Immutable
import com.o3.vanguard.visionary.core.mvi.UiEvent
import com.o3.vanguard.visionary.core.mvi.UiIntent
import com.o3.vanguard.visionary.core.mvi.UiState
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.MediaItem

@Immutable
data class AlbumGalleryState(
    val albumId: String = "",
    val items: List<MediaItem> = emptyList(),
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val selectedIds: Set<MediaId> = emptySet(),
) : UiState

sealed interface AlbumGalleryIntent : UiIntent {
    data class OnItemClicked(val id: MediaId) : AlbumGalleryIntent
    data class OnItemLongClicked(val id: MediaId) : AlbumGalleryIntent
    data class OnItemSelectedToggled(val id: MediaId) : AlbumGalleryIntent
    data object OnClearSelection : AlbumGalleryIntent
    data object OnDeleteSelectedClicked : AlbumGalleryIntent
    data object OnShareSelectedClicked : AlbumGalleryIntent
    data object OnBackClicked : AlbumGalleryIntent
}

sealed interface AlbumGalleryEvent : UiEvent {
    data class OpenViewer(val id: MediaId) : AlbumGalleryEvent
    data object NavigateBack : AlbumGalleryEvent
}
