package com.o3.vanguard.visionary.features.gallery.presentation.photos

// State and intent contracts for the Photos tab.

import androidx.compose.runtime.Immutable
import com.o3.vanguard.visionary.core.mvi.UiEvent
import com.o3.vanguard.visionary.core.mvi.UiIntent
import com.o3.vanguard.visionary.core.mvi.UiState
import com.o3.vanguard.visionary.domain.model.IndexProgress
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.MediaItem

@Immutable
data class PhotoState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val items: List<MediaItem> = emptyList(),
    val indexProgress: IndexProgress? = null,
    val showProcessingCard: Boolean = false,
    val voiceAvailable: Boolean = false,
) : UiState

sealed interface PhotoIntent : UiIntent {
    data class OnItemClicked(val id: MediaId) : PhotoIntent
    data object OnRetryClicked : PhotoIntent
    data object OnSettingsClicked : PhotoIntent
    data class OnDeleteSelected(val items: List<MediaItem>) : PhotoIntent
    data class OnShareSelected(val items: List<MediaItem>) : PhotoIntent
}

sealed interface PhotoEvent : UiEvent {
    data class OpenViewer(val id: MediaId) : PhotoEvent
    data object OpenSettings : PhotoEvent
}
