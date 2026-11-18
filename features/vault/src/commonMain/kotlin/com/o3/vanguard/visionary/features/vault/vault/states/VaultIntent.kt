package com.o3.vanguard.visionary.features.vault.vault.states

import com.o3.vanguard.visionary.core.mvi.UiIntent
import com.o3.vanguard.visionary.features.vault.vault.VaultTab

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
