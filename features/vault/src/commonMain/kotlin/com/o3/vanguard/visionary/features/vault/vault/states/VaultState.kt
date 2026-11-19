package com.o3.vanguard.visionary.features.vault.vault.states

import androidx.compose.runtime.Immutable
import com.o3.vanguard.visionary.core.mvi.UiState
import com.o3.vanguard.visionary.domain.model.VaultItem
import com.o3.vanguard.visionary.features.vault.vault.VaultTab

@Immutable
data class VaultState(
    val isUnlocked: Boolean = false,
    val items: List<VaultItem> = emptyList(),
    val authenticating: Boolean = false,
    val error: String? = null,
    val selectedTab: VaultTab = VaultTab.PHOTOS,
    val selectedItemIds: Set<String> = emptySet(),
    val showRemoveDialog: Boolean = false,
) : UiState
