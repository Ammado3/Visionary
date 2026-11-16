package com.o3.vanguard.visionary.features.gallery

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
                val stateValue = state.value
                if (stateValue.isSelectionMode) {
                    val isSelected = intent.item.id in stateValue.selectedTrashIds
                    val newSelection = if (isSelected) {
                        stateValue.selectedTrashIds - intent.item.id
                    } else {
                        stateValue.selectedTrashIds + intent.item.id
                    }
                    updateState { copy(selectedTrashIds = newSelection) }
                } else {
                    updateState { copy(selectedItem = intent.item) }
                }
            }
            is TrashIntent.OnItemLongClicked -> {
                val stateValue = state.value
                if (!stateValue.isSelectionMode) {
                    updateState { copy(selectedTrashIds = setOf(intent.item.id)) }
                }
            }
            TrashIntent.OnClearSelection -> {
                updateState { copy(selectedTrashIds = emptySet()) }
            }
            TrashIntent.OnDismissDetail -> {
                updateState { copy(selectedItem = null) }
            }
            is TrashIntent.OnDeletePermanently -> {
                viewModelScope.launch {
                    for (id in intent.ids) {
                        trashRepository.deleteFromTrash(id)
                    }
                    updateState {
                        copy(
                            selectedTrashIds = emptySet(),
                            selectedItem = null
                        )
                    }
                }
            }
            is TrashIntent.OnRestoreItems -> {
                viewModelScope.launch {
                    for (id in intent.ids) {
                        trashRepository.restoreFromTrash(id)
                    }
                    updateState {
                        copy(
                            selectedTrashIds = emptySet(),
                            selectedItem = null
                        )
                    }
                }
            }
            is TrashIntent.OnShareItems -> {
                shareGateway.share(intent.items.map { it.uri })
                updateState { copy(selectedTrashIds = emptySet(), selectedItem = null) }
            }
        }
    }
}
