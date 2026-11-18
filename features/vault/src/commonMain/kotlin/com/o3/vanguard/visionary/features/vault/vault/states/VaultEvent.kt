package com.o3.vanguard.visionary.features.vault.vault.states

import com.o3.vanguard.visionary.core.mvi.UiEvent

sealed interface VaultEvent : UiEvent {
    data object OpenVaultGrid : VaultEvent
    data class OpenVaultViewer(val itemId: String) : VaultEvent
    data object NavigateToHome : VaultEvent
}
