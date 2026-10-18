package com.o3.vanguard.visionary.features.home

import androidx.compose.runtime.Immutable
import com.o3.vanguard.visionary.core.mvi.UiEvent
import com.o3.vanguard.visionary.core.mvi.UiIntent
import com.o3.vanguard.visionary.core.mvi.UiState
import com.o3.vanguard.visionary.domain.model.*

@Immutable
data class HomeState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val collections: List<CollectionSummary> = emptyList(),
    val recentFinds: List<MediaItem> = emptyList(),
    val indexProgress: IndexProgress? = null,
) : UiState {
    val isEmpty get() = !loading && !error && collections.all { it.count == 0 }
}

sealed interface HomeIntent : UiIntent {
    data class OnCollectionClicked(val kind: CollectionKind) : HomeIntent
    data class OnRecentClicked(val id: MediaId) : HomeIntent
    data object OnSearchClicked : HomeIntent
    data object OnScanClicked : HomeIntent
    data object OnVaultClicked : HomeIntent
    data object OnSettingsClicked : HomeIntent
    data object OnRetryClicked : HomeIntent
}

sealed interface HomeEvent : UiEvent {
    data class OpenCollection(val kind: CollectionKind) : HomeEvent
    data class OpenViewer(val id: MediaId) : HomeEvent
    data object OpenSearch : HomeEvent
    data object OpenScanner : HomeEvent
    data object OpenVault : HomeEvent
    data object OpenSettings : HomeEvent
}
