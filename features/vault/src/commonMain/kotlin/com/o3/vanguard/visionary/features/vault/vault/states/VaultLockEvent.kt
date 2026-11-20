package com.o3.vanguard.visionary.features.vault.vault.states

import com.o3.vanguard.visionary.core.mvi.UiEvent

sealed class VaultLockEvent : UiEvent {
    data object OpenBiometricSetup : VaultLockEvent()
    data object NavigateToHome : VaultLockEvent()
}
