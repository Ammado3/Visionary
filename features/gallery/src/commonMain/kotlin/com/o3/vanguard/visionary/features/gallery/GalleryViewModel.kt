package com.o3.vanguard.visionary.features.gallery

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.domain.model.CollectionKind
import com.o3.vanguard.visionary.domain.model.DeleteConsentResult
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.usecase.MoveToVaultUseCase
import com.o3.vanguard.visionary.domain.usecase.DeleteMediaUseCase
import com.o3.vanguard.visionary.domain.usecase.ObserveItemUseCase
import com.o3.vanguard.visionary.domain.usecase.ObserveMediaUseCase
import kotlinx.coroutines.launch

class GalleryViewModel(
    kindId: String,
    private val observeMediaUseCase: ObserveMediaUseCase,
) : BaseMviViewModel<GalleryState, GalleryIntent, GalleryEvent>(
    GalleryState(collectionKind = CollectionKind.fromId(kindId))
) {
    init {
        viewModelScope.launch {
            observeMediaUseCase(state.value.collectionKind).collect { list ->
                updateState { copy(items = list, loading = false) }
            }
        }
    }

    override fun onIntent(intent: GalleryIntent) {
        when (intent) {
            is GalleryIntent.OnItemClicked -> sendEvent(GalleryEvent.OpenViewer(intent.id))
            GalleryIntent.OnBackClicked -> sendEvent(GalleryEvent.NavigateBack)
        }
    }
}

class ViewerViewModel(
    mediaIdValue: Long,
    highlight: String?,
    private val moveToVaultUseCase: MoveToVaultUseCase,
    observeItem: ObserveItemUseCase,
    private val deleteMediaUseCase: DeleteMediaUseCase,
) : BaseMviViewModel<ViewerState, ViewerIntent, ViewerEvent>(
    ViewerState(mediaId = MediaId(mediaIdValue), highlightText = highlight)
) {
    init {
        viewModelScope.launch {
            observeItem(MediaId(mediaIdValue)).collect { item ->
                updateState { copy(item = item) }
            }
        }
    }

    override fun onIntent(intent: ViewerIntent) {
        when (intent) {
            ViewerIntent.OnBackClicked -> sendEvent(ViewerEvent.NavigateBack)
            ViewerIntent.OnMoveToVaultClicked -> {
                val currentId = state.value.mediaId ?: return
                viewModelScope.launch {
                    moveToVaultUseCase(currentId)
                    sendEvent(ViewerEvent.NavigateBack)
                }
            }
            ViewerIntent.OnDeleteClicked -> {
                val currentItem = state.value.item ?: return
                viewModelScope.launch {
                    if (deleteMediaUseCase(currentItem.uri) is DeleteConsentResult.Deleted) {
                        sendEvent(ViewerEvent.NavigateBack)
                    }
                }
            }
        }
    }
}
