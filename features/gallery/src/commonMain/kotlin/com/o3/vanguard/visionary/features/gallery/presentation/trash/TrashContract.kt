package com.o3.vanguard.visionary.features.gallery

import androidx.compose.runtime.Immutable
import com.o3.vanguard.visionary.core.mvi.UiEvent
import com.o3.vanguard.visionary.core.mvi.UiIntent
import com.o3.vanguard.visionary.core.mvi.UiState
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.TrashItem

@Immutable
data class TrashState(
    val items: List<TrashItem> = emptyList(),
    val selectedItem: TrashItem? = null,
    val selectedTrashIds: Set<MediaId> = emptySet(),
) : UiState {
    val isSelectionMode get() = selectedTrashIds.isNotEmpty()
}

sealed interface TrashIntent : UiIntent {
    data object OnBackClicked : TrashIntent
    data class OnItemClicked(val item: TrashItem) : TrashIntent
    data class OnItemLongClicked(val item: TrashItem) : TrashIntent
    data object OnClearSelection : TrashIntent
    data object OnDismissDetail : TrashIntent
    data class OnDeletePermanently(val ids: List<MediaId>) : TrashIntent
    data class OnRestoreItems(val ids: List<MediaId>) : TrashIntent
    data class OnShareItems(val items: List<TrashItem>) : TrashIntent
}

sealed interface TrashEvent : UiEvent {
    data object NavigateBack : TrashEvent
}
