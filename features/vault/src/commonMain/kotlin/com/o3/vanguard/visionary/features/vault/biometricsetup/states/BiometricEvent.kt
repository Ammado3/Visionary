package com.o3.vanguard.visionary.features.vault.biometricsetup.states

import com.o3.vanguard.visionary.core.mvi.UiEvent

sealed interface BiometricEvent : UiEvent {
    data object UnlockSuccess : BiometricEvent
    data object NavigateBack : BiometricEvent
}
