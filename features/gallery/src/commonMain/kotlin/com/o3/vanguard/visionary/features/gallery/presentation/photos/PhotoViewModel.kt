package com.o3.vanguard.visionary.features.gallery.presentation.photos

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.domain.model.CollectionKind
import com.o3.vanguard.visionary.domain.port.ShareGateway
import com.o3.vanguard.visionary.domain.usecase.DeleteMediaUseCase
import com.o3.vanguard.visionary.domain.usecase.ObserveIndexProgressUseCase
import com.o3.vanguard.visionary.domain.usecase.ObserveMediaUseCase
import com.o3.vanguard.visionary.domain.repository.SettingsRepository
import com.o3.vanguard.visionary.domain.model.IndexState
import com.o3.vanguard.visionary.domain.port.SpeechAvailability
import com.o3.vanguard.visionary.domain.port.SpeechRecognizerPort
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class PhotoViewModel(
    private val observeMedia: ObserveMediaUseCase,
    private val observeIndex: ObserveIndexProgressUseCase,
    private val deleteMedia: DeleteMediaUseCase,
    private val shareGateway: ShareGateway,
    private val settingsRepository: SettingsRepository,
    private val speechRecognizerPort: SpeechRecognizerPort,
) : BaseMviViewModel<PhotoState, PhotoIntent, PhotoEvent>(PhotoState()) {

    private var observeJob: Job? = null
    private val isVoiceAvailable = speechRecognizerPort.availability() != SpeechAvailability.UNAVAILABLE

    init {
        updateState { copy(voiceAvailable = isVoiceAvailable) }
        observePhotos()
    }

    private fun observePhotos() {
        observeJob?.cancel()
        updateState { copy(loading = true, error = false) }
        observeJob = viewModelScope.launch {
            combine(
                observeMedia(CollectionKind.ALL),
                observeIndex(),
                settingsRepository.observe(),
            ) { items, progress, settings ->
                val showProcessingCard = progress.state == IndexState.RUNNING && !settings.indexingCardShown
                PhotoState(
                    loading = false,
                    items = items,
                    indexProgress = progress,
                    showProcessingCard = showProcessingCard,
                    voiceAvailable = isVoiceAvailable,
                )
            }.catch {
                emit(PhotoState(loading = false, error = true, voiceAvailable = isVoiceAvailable))
            }.collect { newState ->
                updateState { newState }
                if (newState.showProcessingCard) {
                    settingsRepository.update { it.copy(indexingCardShown = true) }
                }
            }
        }
    }

    override fun onIntent(intent: PhotoIntent) {
        when (intent) {
            is PhotoIntent.OnItemClicked -> sendEvent(PhotoEvent.OpenViewer(intent.id))
            PhotoIntent.OnRetryClicked -> observePhotos()
            PhotoIntent.OnSettingsClicked -> sendEvent(PhotoEvent.OpenSettings)
            is PhotoIntent.OnDeleteSelected -> viewModelScope.launch { deleteMedia(intent.items) }
            is PhotoIntent.OnShareSelected -> shareGateway.share(intent.items.map { it.uri })
        }
    }
}
