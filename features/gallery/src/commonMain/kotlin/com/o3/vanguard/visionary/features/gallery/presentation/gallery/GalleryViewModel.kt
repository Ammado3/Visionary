package com.o3.vanguard.visionary.features.gallery

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.common.AppError
import com.o3.vanguard.visionary.core.common.AppResult
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
            else -> {}
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
                updateState { copy(isProcessing = true, errorMessage = null) }
                viewModelScope.launch {
                    val result = moveToVaultUseCase(currentId)
                    updateState { copy(isProcessing = false) }
                    when (result) {
                        is AppResult.Ok -> sendEvent(ViewerEvent.NavigateBack)
                        is AppResult.Err -> {
                            val msg = when (val err = result.error) {
                                is AppError.Io -> err.kind
                                is AppError.Crypto -> err.kind
                                is AppError.Unsupported -> err.reason
                                is AppError.Unknown -> err.kind
                                AppError.PermissionDenied -> "Permission denied"
                                AppError.NotFound -> "Not found"
                                AppError.Cancelled -> "Cancelled"
                            }
                            updateState { copy(errorMessage = msg) }
                        }
                    }
                }
            }
            ViewerIntent.OnDeleteClicked -> {
                val currentItem = state.value.item ?: return
                updateState { copy(isProcessing = true, errorMessage = null) }
                viewModelScope.launch {
                    val deleteResult = deleteMediaUseCase(currentItem)
                    updateState { copy(isProcessing = false) }
                    when (deleteResult) {
                        is DeleteConsentResult.Deleted -> sendEvent(ViewerEvent.NavigateBack)
                        is DeleteConsentResult.DeclinedByUser -> {}
                        is DeleteConsentResult.NotPossible -> updateState { copy(errorMessage = deleteResult.reason) }
                    }
                }
            }
        }
    }
}