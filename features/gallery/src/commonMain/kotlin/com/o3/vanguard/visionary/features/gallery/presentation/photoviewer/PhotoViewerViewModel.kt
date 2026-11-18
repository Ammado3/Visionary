package com.o3.vanguard.visionary.features.gallery.presentation.photoviewer

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.common.AppError
import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.domain.model.DeleteConsentResult
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.usecase.MoveToVaultUseCase
import com.o3.vanguard.visionary.domain.usecase.DeleteMediaUseCase
import com.o3.vanguard.visionary.domain.usecase.ObserveItemUseCase
import com.o3.vanguard.visionary.domain.usecase.SetFavoriteUseCase
import kotlinx.coroutines.launch

class PhotoViewerViewModel(
    mediaIdValue: Long,
    highlight: String?,
    private val moveToVaultUseCase: MoveToVaultUseCase,
    observeItem: ObserveItemUseCase,
    private val deleteMediaUseCase: DeleteMediaUseCase,
    private val setFavoriteUseCase: SetFavoriteUseCase,
) : BaseMviViewModel<PhotoViewerState, PhotoViewerIntent, PhotoViewerEvent>(
    PhotoViewerState(mediaId = MediaId(mediaIdValue), highlightText = highlight)
) {
    init {
        viewModelScope.launch {
            observeItem(MediaId(mediaIdValue)).collect { item ->
                updateState { copy(item = item) }
            }
        }
    }

    override fun onIntent(intent: PhotoViewerIntent) {
        when (intent) {
            PhotoViewerIntent.OnBackClicked -> sendEvent(PhotoViewerEvent.NavigateBack)
            PhotoViewerIntent.OnFavoriteClicked -> {
                val item = state.value.item ?: return
                viewModelScope.launch {
                    setFavoriteUseCase(item.id, !item.isFavorite)
                }
            }
            PhotoViewerIntent.OnMoveToVaultClicked -> {
                val currentId = state.value.mediaId ?: return
                updateState { copy(isProcessing = true, errorMessage = null) }
                viewModelScope.launch {
                    val result = moveToVaultUseCase(currentId)
                    updateState { copy(isProcessing = false) }
                    when (result) {
                        is AppResult.Ok -> sendEvent(PhotoViewerEvent.NavigateBack)
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
            PhotoViewerIntent.OnDeleteClicked -> {
                val currentItem = state.value.item ?: return
                updateState { copy(isProcessing = true, errorMessage = null) }
                viewModelScope.launch {
                    val deleteResult = deleteMediaUseCase(currentItem)
                    updateState { copy(isProcessing = false) }
                    when (deleteResult) {
                        is DeleteConsentResult.Deleted -> sendEvent(PhotoViewerEvent.NavigateBack)
                        is DeleteConsentResult.DeclinedByUser -> {}
                        is DeleteConsentResult.NotPossible -> updateState { copy(errorMessage = deleteResult.reason) }
                    }
                }
            }
        }
    }
}
