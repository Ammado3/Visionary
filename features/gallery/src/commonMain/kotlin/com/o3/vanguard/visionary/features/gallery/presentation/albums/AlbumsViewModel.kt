package com.o3.vanguard.visionary.features.gallery.presentation.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.domain.usecase.ObserveStorageAlbumsUseCase
import com.o3.vanguard.visionary.domain.usecase.ObserveFavoriteMediaUseCase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AlbumsViewModel(
    private val observeAlbums: ObserveStorageAlbumsUseCase,
    private val observeFavorites: ObserveFavoriteMediaUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AlbumsState())
    val state: StateFlow<AlbumsState> = _state.asStateFlow()

    init {
        loadAlbums()
        viewModelScope.launch {
            observeFavorites().collectLatest { favorites ->
                _state.value = _state.value.copy(favoriteCount = favorites.size)
            }
        }
    }

    fun onIntent(intent: AlbumsIntent) {
        when (intent) {
            AlbumsIntent.OnRetryClicked -> loadAlbums()
        }
    }

    private fun loadAlbums() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            _state.value = when (val result = observeAlbums()) {
                is AppResult.Ok -> _state.value.copy(loading = false, albums = result.value)
                is AppResult.Err -> _state.value.copy(loading = false, error = "Failed to load albums")
            }
        }
    }
}
