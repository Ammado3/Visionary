package com.o3.vanguard.visionary.features.search.voice

import com.o3.vanguard.visionary.core.mvi.UiEvent
import com.o3.vanguard.visionary.core.mvi.UiIntent
import com.o3.vanguard.visionary.core.mvi.UiState
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.SearchHit
import com.o3.vanguard.visionary.domain.port.SpeechAvailability
import com.o3.vanguard.visionary.domain.voice.VoiceQuery

data class VoiceSearchState(
    val phase: Phase = Phase.Idle,
    val availability: SpeechAvailability = SpeechAvailability.UNAVAILABLE,
    val liveText: String = "",
    val query: VoiceQuery? = null,
    val hits: List<SearchHit> = emptyList(),
) : UiState {
    enum class Phase { Idle, NeedsPermission, Listening, Searching, Results, NoResults, NoKeywords, Error }
}

sealed interface VoiceSearchIntent : UiIntent {
    data object OnStart : VoiceSearchIntent
    data class OnPermissionResult(val granted: Boolean) : VoiceSearchIntent
    data object OnStopListening : VoiceSearchIntent
    data object OnRetry : VoiceSearchIntent
    data class OnEditedText(val text: String) : VoiceSearchIntent
    data class OnResultClicked(val hit: SearchHit) : VoiceSearchIntent
    data object OnTypeInstead : VoiceSearchIntent
    data object OnBackClicked : VoiceSearchIntent
}

sealed interface VoiceSearchEvent : UiEvent {
    data object RequestMicPermission : VoiceSearchEvent
    data class OpenViewer(val mediaId: MediaId, val highlight: String?) : VoiceSearchEvent
    data class OpenTextSearch(val query: String) : VoiceSearchEvent
    data object NavigateBack : VoiceSearchEvent
}
