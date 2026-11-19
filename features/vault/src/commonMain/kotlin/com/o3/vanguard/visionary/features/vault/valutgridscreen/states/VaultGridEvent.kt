package com.o3.vanguard.visionary.features.vault.valutgridscreen.states

import com.o3.vanguard.visionary.core.mvi.UiEvent

sealed class VaultGridEvent : UiEvent {
    data class OpenVaultViewer(val itemId: String) : VaultGridEvent()
    data object NavigateToHome : VaultGridEvent()
}
