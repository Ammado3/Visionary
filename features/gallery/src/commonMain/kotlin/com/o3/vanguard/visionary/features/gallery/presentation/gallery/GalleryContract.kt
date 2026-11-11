package com.o3.vanguard.visionary.features.gallery.presentation.gallery

import androidx.compose.runtime.Immutable
import com.o3.vanguard.visionary.core.mvi.UiEvent
import com.o3.vanguard.visionary.core.mvi.UiIntent
import com.o3.vanguard.visionary.core.mvi.UiState
import com.o3.vanguard.visionary.domain.model.CollectionKind
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.MediaItem

@Immutable
data class GalleryState(
    val collectionKind: CollectionKind = CollectionKind.ALL,
    val items: List<MediaItem> = emptyList(),
    val loading: Boolean = true,
    val selectedIds: Set<MediaId> = emptySet(),
) : UiState

sealed interface GalleryIntent : UiIntent {
    data class OnItemClicked(val id: MediaId) : GalleryIntent
    data class OnItemLongClicked(val id: MediaId) : GalleryIntent
    data class OnItemSelectedToggled(val id: MediaId) : GalleryIntent
    data object OnClearSelection : GalleryIntent
    data object OnDeleteSelectedClicked : GalleryIntent
    data object OnShareSelectedClicked : GalleryIntent
    data object OnBackClicked : GalleryIntent
}

sealed interface GalleryEvent : UiEvent {
    data class OpenViewer(val id: MediaId) : GalleryEvent
    data object NavigateBack : GalleryEvent
}

@Immutable
data class ViewerState(
    val mediaId: MediaId? = null,
    val item: MediaItem? = null,
    val highlightText: String? = null,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
) : UiState

sealed interface ViewerIntent : UiIntent {
    data object OnBackClicked : ViewerIntent
    data object OnMoveToVaultClicked : ViewerIntent
    data object OnDeleteClicked : ViewerIntent
}

sealed interface ViewerEvent : UiEvent {
    data object NavigateBack : ViewerEvent
}
