package com.o3.vanguard.visionary.features.vault.vault

import androidx.compose.runtime.Immutable
import com.o3.vanguard.visionary.core.mvi.UiEvent
import com.o3.vanguard.visionary.core.mvi.UiIntent
import com.o3.vanguard.visionary.core.mvi.UiState
import com.o3.vanguard.visionary.domain.model.VaultItem

enum class VaultTab { PHOTOS, VIDEOS }

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

sealed interface VaultIntent : UiIntent {
    data object OnUnlockClicked : VaultIntent
    data object OnLockClicked : VaultIntent
    data class OnItemClicked(val itemId: String) : VaultIntent
    data class OnItemSelected(val itemId: String) : VaultIntent
    data object OnSelectAllToggled : VaultIntent
    data class OnTabSelected(val tab: VaultTab) : VaultIntent
    data object OnRemoveSelectedClicked : VaultIntent
    data object OnRestoreConfirmed : VaultIntent
    data object OnDeletePermanentlyConfirmed : VaultIntent
    data object OnDismissRemoveDialog : VaultIntent
    data object OnBackClicked : VaultIntent
}

sealed interface VaultEvent : UiEvent {
    data object OpenVaultGrid : VaultEvent
    data class OpenVaultViewer(val itemId: String) : VaultEvent
    data object NavigateToHome : VaultEvent
}
