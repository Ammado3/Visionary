package com.o3.vanguard.visionary.features.search

import androidx.compose.runtime.Immutable
import com.o3.vanguard.visionary.core.mvi.UiEvent
import com.o3.vanguard.visionary.core.mvi.UiIntent
import com.o3.vanguard.visionary.core.mvi.UiState
import com.o3.vanguard.visionary.domain.model.IndexProgress
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.MediaItem
import com.o3.vanguard.visionary.domain.model.SearchFilter
import com.o3.vanguard.visionary.domain.model.SearchHit
import com.o3.vanguard.visionary.domain.model.SearchMode

@Immutable
data class SearchState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.ALL,
    val searchMode: SearchMode = SearchMode.FUZZY,
    val results: List<SearchHit> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val allImages: List<MediaItem> = emptyList(),
    val indexProgress: IndexProgress? = null,
    val isIndexing: Boolean = false,
    val isSearching: Boolean = false,
    val errorMessage: String? = null,
    val selectedImage: MediaItem? = null,
) : UiState

sealed interface SearchIntent : UiIntent {
    data class OnQueryChanged(val query: String) : SearchIntent
    data class OnFilterChanged(val filter: SearchFilter) : SearchIntent
    data class OnSearchModeChanged(val mode: SearchMode) : SearchIntent
    data class OnHitClicked(val item: MediaItem) : SearchIntent
    data object OnReindexClicked : SearchIntent
    data object OnClearError : SearchIntent
    data object OnBackClicked : SearchIntent
    data class OnDeleteImage(val item: MediaItem) : SearchIntent
    data object OnDismissDetail : SearchIntent
}

sealed interface SearchEvent : UiEvent {
    data class OpenViewer(val id: MediaId, val highlight: String) : SearchEvent
    data object NavigateBack : SearchEvent
    data class ShowToast(val message: String) : SearchEvent
}
