package com.o3.vanguard.visionary.features.vault.vault.states

import com.o3.vanguard.visionary.core.mvi.UiIntent

sealed class VaultLockIntent : UiIntent {
    data object OnSetupVaultClicked : VaultLockIntent()
    data object OnBackClicked : VaultLockIntent()
}
