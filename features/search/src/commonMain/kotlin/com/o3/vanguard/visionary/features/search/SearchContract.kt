package com.o3.vanguard.visionary.features.search

import androidx.compose.runtime.Immutable
import com.o3.vanguard.visionary.core.mvi.UiEvent
import com.o3.vanguard.visionary.core.mvi.UiIntent
import com.o3.vanguard.visionary.core.mvi.UiState
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.SearchFilter
import com.o3.vanguard.visionary.domain.model.SearchHit

@Immutable
data class SearchState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.ALL,
    val results: List<SearchHit> = emptyList(),
    val recentSearches: List<String> = emptyList(),
) : UiState

sealed interface SearchIntent : UiIntent {
    data class OnQueryChanged(val query: String) : SearchIntent
    data class OnFilterChanged(val filter: SearchFilter) : SearchIntent
    data class OnHitClicked(val id: MediaId, val snippet: String) : SearchIntent
    data object OnBackClicked : SearchIntent
}

sealed interface SearchEvent : UiEvent {
    data class OpenViewer(val id: MediaId, val highlight: String) : SearchEvent
    data object NavigateBack : SearchEvent
}
