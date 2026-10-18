package com.o3.vanguard.visionary.features.vault

import androidx.compose.runtime.Immutable
import com.o3.vanguard.visionary.core.mvi.UiEvent
import com.o3.vanguard.visionary.core.mvi.UiIntent
import com.o3.vanguard.visionary.core.mvi.UiState
import com.o3.vanguard.visionary.domain.model.VaultItem

@Immutable
data class VaultState(
    val isUnlocked: Boolean = false,
    val items: List<VaultItem> = emptyList(),
    val authenticating: Boolean = false,
    val error: String? = null,
) : UiState

sealed interface VaultIntent : UiIntent {
    data object OnUnlockClicked : VaultIntent
    data object OnLockClicked : VaultIntent
    data class OnItemClicked(val itemId: String) : VaultIntent
    data object OnBackClicked : VaultIntent
}

sealed interface VaultEvent : UiEvent {
    data object OpenVaultGrid : VaultEvent
    data class OpenVaultViewer(val itemId: String) : VaultEvent
    data object NavigateToHome : VaultEvent
}
