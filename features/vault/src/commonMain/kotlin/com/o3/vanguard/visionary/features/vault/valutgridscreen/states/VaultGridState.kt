package com.o3.vanguard.visionary.features.vault.valutgridscreen.states

import androidx.compose.runtime.Immutable
import com.o3.vanguard.visionary.core.mvi.UiState
import com.o3.vanguard.visionary.domain.model.VaultItem

enum class VaultGridTab { PHOTOS, VIDEOS }

@Immutable
data class VaultGridState(
    val items: List<VaultItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTab: VaultGridTab = VaultGridTab.PHOTOS,
    val selectedItemIds: Set<String> = emptySet(),
    val showRemoveDialog: Boolean = false,
) : UiState
