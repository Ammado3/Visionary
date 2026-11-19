package com.o3.vanguard.visionary.features.vault.biometricsetup.states

import com.o3.vanguard.visionary.core.mvi.UiIntent

sealed interface BiometricIntent : UiIntent {
    data class OnNumberClicked(val digit: Char) : BiometricIntent
    data object OnDeleteClicked : BiometricIntent
    data class OnDotSelected(val dotIndex: Int) : BiometricIntent
    data object OnPatternCompleted : BiometricIntent
    data object OnClearPatternClicked : BiometricIntent
    data class OnSwitchMode(val mode: LockMode) : BiometricIntent
    data object OnUnlockClicked : BiometricIntent
    data object OnBiometricClicked : BiometricIntent
    data object OnForgotPinClicked : BiometricIntent
    data object OnBackClicked : BiometricIntent
    data object DismissDialog : BiometricIntent
}
