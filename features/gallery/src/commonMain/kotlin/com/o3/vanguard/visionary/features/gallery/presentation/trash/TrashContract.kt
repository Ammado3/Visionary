package com.o3.vanguard.visionary.features.gallery.presentation.trash

import androidx.compose.runtime.Immutable
import com.o3.vanguard.visionary.core.mvi.UiEvent
import com.o3.vanguard.visionary.core.mvi.UiIntent
import com.o3.vanguard.visionary.core.mvi.UiState
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.TrashItem

@Immutable
data class TrashState(
    val items: List<TrashItem> = emptyList(),
) : UiState

sealed interface TrashIntent : UiIntent {
    data object OnBackClicked : TrashIntent
    data class OnDeletePermanentlyClicked(val id: MediaId) : TrashIntent
}

sealed interface TrashEvent : UiEvent {
    data object NavigateBack : TrashEvent
}
