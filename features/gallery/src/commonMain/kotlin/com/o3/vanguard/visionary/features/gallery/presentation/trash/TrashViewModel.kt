package com.o3.vanguard.visionary.features.gallery.presentation.trash

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.domain.port.ShareGateway
import com.o3.vanguard.visionary.domain.repository.TrashRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TrashViewModel(
    private val trashRepository: TrashRepository,
    private val shareGateway: ShareGateway,
) : BaseMviViewModel<TrashState, TrashIntent, TrashEvent>(TrashState()) {
    init {
        viewModelScope.launch {
            trashRepository.observeTrashItems().collectLatest { items ->
                updateState { copy(items = items) }
            }
        }
    }

    override fun onIntent(intent: TrashIntent) {
        when (intent) {
            TrashIntent.OnBackClicked -> sendEvent(TrashEvent.NavigateBack)
            is TrashIntent.OnItemClicked -> {
                val current = state.value
                if (current.isSelectionMode) {
                    val selected = if (intent.item.id in current.selectedTrashIds) {
                        current.selectedTrashIds - intent.item.id
                    } else {
                        current.selectedTrashIds + intent.item.id
                    }
                    updateState { copy(selectedTrashIds = selected) }
                } else {
                    updateState { copy(selectedItem = intent.item) }
                }
            }
            is TrashIntent.OnItemLongClicked -> {
                if (!state.value.isSelectionMode) {
                    updateState { copy(selectedTrashIds = setOf(intent.item.id)) }
                }
            }
            TrashIntent.OnClearSelection -> updateState { copy(selectedTrashIds = emptySet()) }
            TrashIntent.OnDismissDetail -> updateState { copy(selectedItem = null) }
            is TrashIntent.OnDeletePermanently -> viewModelScope.launch {
                intent.ids.forEach { trashRepository.deleteFromTrash(it) }
                updateState { copy(selectedTrashIds = emptySet(), selectedItem = null) }
            }
            is TrashIntent.OnRestoreItems -> viewModelScope.launch {
                intent.ids.forEach { trashRepository.restoreFromTrash(it) }
                updateState { copy(selectedTrashIds = emptySet(), selectedItem = null) }
            }
            is TrashIntent.OnShareItems -> {
                shareGateway.share(intent.items.map { it.uri })
                updateState { copy(selectedTrashIds = emptySet(), selectedItem = null) }
            }
        }
    }
}
