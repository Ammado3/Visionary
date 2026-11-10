package com.o3.vanguard.visionary.features.gallery.presentation.trash

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.domain.repository.TrashRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TrashViewModel(
    private val trashRepository: TrashRepository,
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
            is TrashIntent.OnDeletePermanentlyClicked -> {
                viewModelScope.launch {
                    trashRepository.deleteFromTrash(intent.id)
                }
            }
        }
    }
}
