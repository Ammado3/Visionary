package com.o3.vanguard.visionary.features.search

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.domain.model.CollectionKind
import com.o3.vanguard.visionary.domain.model.SearchFilter
import com.o3.vanguard.visionary.domain.model.SearchMode
import com.o3.vanguard.visionary.domain.repository.SearchRepository
import com.o3.vanguard.visionary.domain.usecase.ObserveIndexProgressUseCase
import com.o3.vanguard.visionary.domain.usecase.ObserveMediaUseCase
import com.o3.vanguard.visionary.domain.usecase.RebuildIndexUseCase
import com.o3.vanguard.visionary.domain.usecase.SearchMediaUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SearchViewModel(
    initialQuery: String,
    private val searchMediaUseCase: SearchMediaUseCase,
    observeMediaUseCase: ObserveMediaUseCase,
    observeIndexProgressUseCase: ObserveIndexProgressUseCase,
    private val rebuildIndexUseCase: RebuildIndexUseCase,
    private val searchRepository: SearchRepository,
) : BaseMviViewModel<SearchState, SearchIntent, SearchEvent>(
    SearchState(query = initialQuery)
) {
    init {
        viewModelScope.launch {
            observeMediaUseCase(CollectionKind.ALL).collectLatest { images ->
                updateState { copy(allImages = images) }
            }
        }
        viewModelScope.launch {
            observeIndexProgressUseCase().collectLatest { progress ->
                updateState {
                    copy(
                        indexProgress = progress,
                        isIndexing = progress.state.name == "RUNNING"
                    )
                }
            }
        }
        viewModelScope.launch {
            searchRepository.observeRecentSearches().collectLatest { searches ->
                updateState { copy(recentSearches = searches) }
            }
        }

        observeQueryChanges()

        if (initialQuery.isNotBlank()) {
            executeSearch(initialQuery, state.value.filter, state.value.searchMode)
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeQueryChanges() {
        viewModelScope.launch {
            state.map { Pair(it.query.trim(), it.searchMode) }
                .distinctUntilChanged()
                .debounce(300)
                .collectLatest { (query, mode) ->
                    if (query.isNotBlank() && query.length >= 2) {
                        executeSearch(query, state.value.filter, mode)
                    } else if (query.isBlank()) {
                        updateState { copy(results = emptyList(), isSearching = false) }
                    }
                }
        }
    }

    private fun executeSearch(query: String, filter: SearchFilter, mode: SearchMode) {
        viewModelScope.launch {
            updateState { copy(isSearching = true, errorMessage = null) }
            try {
                searchMediaUseCase(query, filter, mode).collectLatest { hits ->
                    updateState {
                        copy(
                            results = hits,
                            isSearching = false,
                            errorMessage = if (hits.isEmpty() && query.isNotBlank()) {
                                "No matching images found for '$query' in ${mode.name.lowercase()} mode"
                            } else null
                        )
                    }
                    if (hits.isNotEmpty()) {
                        searchRepository.saveRecentSearch(query)
                    }
                }
            } catch (e: Exception) {
                updateState {
                    copy(
                        isSearching = false,
                        errorMessage = "Search failed: ${e.message}",
                        results = emptyList()
                    )
                }
            }
        }
    }

    override fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.OnQueryChanged -> {
                updateState { copy(query = intent.query) }
                if (intent.query.isBlank()) {
                    updateState { copy(results = emptyList(), isSearching = false) }
                }
            }
            is SearchIntent.OnFilterChanged -> {
                updateState { copy(filter = intent.filter) }
                if (state.value.query.isNotBlank()) {
                    executeSearch(state.value.query, intent.filter, state.value.searchMode)
                }
            }
            is SearchIntent.OnSearchModeChanged -> {
                updateState { copy(searchMode = intent.mode) }
                if (state.value.query.isNotBlank()) {
                    executeSearch(state.value.query, state.value.filter, intent.mode)
                }
            }
            is SearchIntent.OnHitClicked -> {
                updateState { copy(selectedImage = intent.item) }
            }
            SearchIntent.OnDismissDetail -> {
                updateState { copy(selectedImage = null) }
            }
            is SearchIntent.OnDeleteImage -> {
                updateState {
                    copy(
                        selectedImage = null,
                        allImages = allImages.filterNot { it.id == intent.item.id },
                        results = results.filterNot { it.item.id == intent.item.id }
                    )
                }
                sendEvent(SearchEvent.ShowToast("Image moved to Bin"))
            }
            SearchIntent.OnReindexClicked -> {
                viewModelScope.launch {
                    updateState { copy(isIndexing = true, errorMessage = null) }
                    val result = rebuildIndexUseCase()
                    if (result is AppResult.Err) {
                        updateState { copy(isIndexing = false, errorMessage = "Indexing failed") }
                    }
                }
            }
            SearchIntent.OnClearError -> updateState { copy(errorMessage = null) }
            SearchIntent.OnBackClicked -> sendEvent(SearchEvent.NavigateBack)
        }
    }
}
