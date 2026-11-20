package com.o3.vanguard.visionary.features.gallery.presentation.albums

// State and intent contracts for the albums flow.

import androidx.compose.runtime.Immutable
import com.o3.vanguard.visionary.core.mvi.UiIntent
import com.o3.vanguard.visionary.core.mvi.UiState
import com.o3.vanguard.visionary.domain.model.StorageAlbum

@Immutable
data class AlbumsState(
    val loading: Boolean = true,
    val albums: List<StorageAlbum> = emptyList(),
    val favoriteCount: Int = 0,
    val error: String? = null,
) : UiState

sealed interface AlbumsIntent : UiIntent {
    data object OnRetryClicked : AlbumsIntent
}
