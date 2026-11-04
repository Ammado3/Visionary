package com.o3.vanguard.visionary.features.home

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.domain.usecase.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class HomeViewModel(
    private val observeCollections: ObserveCollectionsUseCase,
    private val observeRecent: ObserveRecentFindsUseCase,
    private val observeIndex: ObserveIndexProgressUseCase,
) : BaseMviViewModel<HomeState, HomeIntent, HomeEvent>(HomeState()) {

    init {
        loadData()
    }

    private fun loadData() {
        updateState { copy(loading = true, error = false) }
        viewModelScope.launch {
            combine(
                observeCollections(),
                observeRecent(limit = Int.MAX_VALUE),
                observeIndex()
            ) { collections, recent, index ->
                { state: HomeState ->
                    state.copy(
                        loading = false,
                        error = false,
                        collections = collections,
                        recentFinds = recent,
                        indexProgress = index
                    )
                }
            }
                .catch { emit { state -> state.copy(loading = false, error = true) } }
                .collect { reducer -> updateState { reducer(this) } }
        }
    }

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.OnCollectionClicked -> sendEvent(HomeEvent.OpenCollection(intent.kind))
            is HomeIntent.OnRecentClicked -> sendEvent(HomeEvent.OpenViewer(intent.id))
            HomeIntent.OnSearchClicked -> sendEvent(HomeEvent.OpenSearch)
            HomeIntent.OnAlbumsClicked -> sendEvent(HomeEvent.OpenAlbums)
            HomeIntent.OnScanClicked -> sendEvent(HomeEvent.OpenScanner)
            HomeIntent.OnVaultClicked -> sendEvent(HomeEvent.OpenVault)
            HomeIntent.OnSettingsClicked -> sendEvent(HomeEvent.OpenSettings)
            HomeIntent.OnRetryClicked -> loadData()
        }
    }
}
