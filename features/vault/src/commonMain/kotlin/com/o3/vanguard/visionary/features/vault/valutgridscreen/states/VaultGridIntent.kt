package com.o3.vanguard.visionary.features.vault.valutgridscreen.states

import com.o3.vanguard.visionary.core.mvi.UiIntent

sealed class VaultGridIntent : UiIntent {
    data class OnItemClicked(val itemId: String) : VaultGridIntent()
    data class OnItemSelected(val itemId: String) : VaultGridIntent()
    data object OnSelectAllToggled : VaultGridIntent()
    data class OnTabSelected(val tab: VaultGridTab) : VaultGridIntent()
    data object OnRemoveSelectedClicked : VaultGridIntent()
    data object OnRestoreConfirmed : VaultGridIntent()
    data object OnDeletePermanentlyConfirmed : VaultGridIntent()
    data object OnDismissRemoveDialog : VaultGridIntent()
    data object OnBackClicked : VaultGridIntent()
}
