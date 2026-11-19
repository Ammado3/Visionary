package com.o3.vanguard.visionary.features.gallery.presentation.favorite

import androidx.compose.runtime.Immutable
import com.o3.vanguard.visionary.core.mvi.UiState
import com.o3.vanguard.visionary.domain.model.MediaItem

@Immutable
data class FavoriteState(
    val items: List<MediaItem> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
) : UiState
