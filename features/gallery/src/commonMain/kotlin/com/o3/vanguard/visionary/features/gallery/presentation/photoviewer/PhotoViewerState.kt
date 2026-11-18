package com.o3.vanguard.visionary.features.gallery.presentation.photoviewer

import androidx.compose.runtime.Immutable
import com.o3.vanguard.visionary.core.mvi.UiEvent
import com.o3.vanguard.visionary.core.mvi.UiIntent
import com.o3.vanguard.visionary.core.mvi.UiState
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.MediaItem

@Immutable
data class PhotoViewerState(
    val mediaId: MediaId? = null,
    val item: MediaItem? = null,
    val highlightText: String? = null,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
) : UiState

sealed interface PhotoViewerIntent : UiIntent {
    data object OnBackClicked : PhotoViewerIntent
    data object OnFavoriteClicked : PhotoViewerIntent
    data object OnMoveToVaultClicked : PhotoViewerIntent
    data object OnDeleteClicked : PhotoViewerIntent
}

sealed interface PhotoViewerEvent : UiEvent {
    data object NavigateBack : PhotoViewerEvent
}
