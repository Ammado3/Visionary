package com.o3.vanguard.visionary.features.gallery.presentation.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.domain.usecase.ObserveFavoriteMediaUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FavoriteViewModel(
    private val observeFavorites: ObserveFavoriteMediaUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(FavoriteState())
    val state: StateFlow<FavoriteState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeFavorites()
                .catch { error -> _state.value = FavoriteState(loading = false, error = error.message ?: "Failed to load favorites") }
                .collect { items -> _state.value = FavoriteState(loading = false, items = items) }
        }
    }
}
