package com.o3.vanguard.visionary.features.search

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.domain.usecase.SearchMediaUseCase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchViewModel(
    initialQuery: String,
    private val searchMediaUseCase: SearchMediaUseCase,
) : BaseMviViewModel<SearchState, SearchIntent, SearchEvent>(
    SearchState(query = initialQuery)
) {
    init {
        if (initialQuery.isNotBlank()) {
            executeSearch(initialQuery, state.value.filter)
        }
    }

    private fun executeSearch(query: String, filter: com.o3.vanguard.visionary.domain.model.SearchFilter) {
        viewModelScope.launch {
            searchMediaUseCase(query, filter).collectLatest { hits ->
                updateState { copy(results = hits) }
            }
        }
    }

    override fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.OnQueryChanged -> {
                updateState { copy(query = intent.query) }
                executeSearch(intent.query, state.value.filter)
            }
            is SearchIntent.OnFilterChanged -> {
                updateState { copy(filter = intent.filter) }
                executeSearch(state.value.query, intent.filter)
            }
            is SearchIntent.OnHitClicked -> sendEvent(SearchEvent.OpenViewer(intent.id, intent.snippet))
            SearchIntent.OnBackClicked -> sendEvent(SearchEvent.NavigateBack)
        }
    }
}
