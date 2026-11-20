package com.o3.vanguard.visionary.features.search.voice

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.common.AppDispatchers
import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.domain.port.SpeechAvailability
import com.o3.vanguard.visionary.domain.port.SpeechEvent
import com.o3.vanguard.visionary.domain.port.SpeechFailure
import com.o3.vanguard.visionary.domain.port.SpeechRecognizerPort
import com.o3.vanguard.visionary.domain.voice.VoiceSearchUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoiceSearchViewModel(
    private val speechRecognizerPort: SpeechRecognizerPort,
    private val voiceSearchUseCase: VoiceSearchUseCase,
    private val dispatchers: AppDispatchers,
) : BaseMviViewModel<VoiceSearchState, VoiceSearchIntent, VoiceSearchEvent>(
    VoiceSearchState()
) {
    private var listenJob: Job? = null

    init {
        updateState { copy(availability = speechRecognizerPort.availability()) }
    }

    override fun onIntent(intent: VoiceSearchIntent) {
        when (intent) {
            VoiceSearchIntent.OnStart -> {
                if (state.value.availability == SpeechAvailability.UNAVAILABLE) {
                    updateState { copy(phase = VoiceSearchState.Phase.Error) }
                } else {
                    sendEvent(VoiceSearchEvent.RequestMicPermission)
                }
            }

            is VoiceSearchIntent.OnPermissionResult -> {
                if (intent.granted) {
                    startListening()
                } else {
                    updateState { copy(phase = VoiceSearchState.Phase.NeedsPermission) }
                }
            }

            VoiceSearchIntent.OnStopListening -> {
                listenJob?.cancel()
                updateState { copy(phase = VoiceSearchState.Phase.Idle) }
            }

            VoiceSearchIntent.OnRetry -> {
                onIntent(VoiceSearchIntent.OnStart)
            }

            is VoiceSearchIntent.OnEditedText -> {
                listenJob?.cancel()
                performSearch(listOf(intent.text))
            }

            is VoiceSearchIntent.OnResultClicked -> {
                val highlight = state.value.query?.keywords?.firstOrNull()
                sendEvent(VoiceSearchEvent.OpenViewer(intent.hit.item.id, highlight))
            }

            VoiceSearchIntent.OnTypeInstead -> {
                val text = state.value.liveText.ifBlank { state.value.query?.searchText ?: "" }
                sendEvent(VoiceSearchEvent.OpenTextSearch(text))
            }

            VoiceSearchIntent.OnBackClicked -> {
                sendEvent(VoiceSearchEvent.NavigateBack)
            }
        }
    }

    private fun startListening() {
        listenJob?.cancel()
        updateState { copy(phase = VoiceSearchState.Phase.Listening, liveText = "") }
        listenJob = viewModelScope.launch {
            try {
                speechRecognizerPort.listen().collect { event ->
                    when (event) {
                        SpeechEvent.Ready -> {
                            updateState { copy(phase = VoiceSearchState.Phase.Listening) }
                        }

                        is SpeechEvent.Partial -> {
                            updateState { copy(phase = VoiceSearchState.Phase.Listening, liveText = event.text) }
                        }

                        is SpeechEvent.Final -> {
                            updateState {
                                copy(
                                    phase = VoiceSearchState.Phase.Searching,
                                    liveText = event.alternatives.firstOrNull() ?: liveText,
                                )
                            }
                            performSearch(event.alternatives)
                        }

                        is SpeechEvent.Failed -> {
                            handleSpeechFailure(event.reason)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                updateState { copy(phase = VoiceSearchState.Phase.Error) }
            }
        }
    }

    private fun handleSpeechFailure(reason: SpeechFailure) {
        when (reason) {
            SpeechFailure.NO_MATCH, SpeechFailure.NO_SPEECH -> {
                updateState { copy(phase = VoiceSearchState.Phase.Idle) }
            }

            SpeechFailure.PERMISSION_DENIED -> {
                updateState { copy(phase = VoiceSearchState.Phase.NeedsPermission) }
            }

            SpeechFailure.NOT_AVAILABLE_OFFLINE, SpeechFailure.BUSY, SpeechFailure.OTHER -> {
                updateState { copy(phase = VoiceSearchState.Phase.Error) }
            }
        }
    }

    private fun performSearch(alternatives: List<String>) {
        viewModelScope.launch {
            updateState { copy(phase = VoiceSearchState.Phase.Searching) }
            val result = withContext(dispatchers.default) {
                voiceSearchUseCase(alternatives)
            }
            val phase = when {
                result.query.keywords.isEmpty() -> VoiceSearchState.Phase.NoKeywords
                result.hits.isEmpty() -> VoiceSearchState.Phase.NoResults
                else -> VoiceSearchState.Phase.Results
            }
            updateState {
                copy(
                    phase = phase,
                    query = result.query,
                    hits = result.hits,
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenJob?.cancel()
    }
}
