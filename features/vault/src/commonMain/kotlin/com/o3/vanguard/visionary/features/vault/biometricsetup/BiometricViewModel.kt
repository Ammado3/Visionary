package com.o3.vanguard.visionary.features.vault.biometricsetup

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.domain.port.BiometricAuthenticator
import com.o3.vanguard.visionary.features.vault.biometricsetup.states.BiometricEvent
import com.o3.vanguard.visionary.features.vault.biometricsetup.states.BiometricIntent
import com.o3.vanguard.visionary.features.vault.biometricsetup.states.BiometricState
import com.o3.vanguard.visionary.features.vault.biometricsetup.states.LockMode
import kotlinx.coroutines.launch

class BiometricViewModel(
    private val biometricAuthenticator: BiometricAuthenticator,
) : BaseMviViewModel<BiometricState, BiometricIntent, BiometricEvent>(BiometricState()) {
    override fun onIntent(intent: BiometricIntent) {
        when (intent) {
            is BiometricIntent.OnNumberClicked -> {
                if (state.value.pin.length < 8) {
                    val newPin = state.value.pin + intent.digit
                    updateState { copy(pin = newPin, errorMessage = null) }
                }
            }
            BiometricIntent.OnDeleteClicked -> {
                if (state.value.pin.isNotEmpty()) {
                    updateState { copy(pin = state.value.pin.dropLast(1), errorMessage = null) }
                }
            }
            is BiometricIntent.OnDotSelected -> {
                val currentSeq = state.value.patternSequence
                if (!currentSeq.contains(intent.dotIndex)) {
                    updateState { copy(patternSequence = currentSeq + intent.dotIndex, errorMessage = null) }
                }
            }
            BiometricIntent.OnPatternCompleted -> {
                if (state.value.patternSequence.size < 4) {
                    updateState { copy(errorMessage = "Connect at least 4 dots.", patternSequence = emptyList()) }
                } else {
                    sendEvent(BiometricEvent.UnlockSuccess)
                }
            }
            BiometricIntent.OnClearPatternClicked -> {
                updateState { copy(patternSequence = emptyList(), errorMessage = null) }
            }
            is BiometricIntent.OnSwitchMode -> {
                updateState { copy(lockMode = intent.mode, pin = "", patternSequence = emptyList(), errorMessage = null) }
            }
            BiometricIntent.OnUnlockClicked -> {
                if (state.value.lockMode == LockMode.PIN && state.value.pin.length < 4) {
                    updateState { copy(errorMessage = "Please enter at least 4 digits PIN.") }
                } else {
                    sendEvent(BiometricEvent.UnlockSuccess)
                }
            }
            BiometricIntent.OnBiometricClicked -> {
                viewModelScope.launch {
                    when (biometricAuthenticator.authenticate()) {
                        is AppResult.Ok -> {
                            sendEvent(BiometricEvent.UnlockSuccess)
                        }
                        is AppResult.Err -> {
                            updateState {
                                copy(
                                    showFailureDialog = true,
                                    dialogMessage = "Biometric is not set up on your device or authentication failed."
                                )
                            }
                        }
                    }
                }
            }
            BiometricIntent.DismissDialog -> {
                val wasSuccess = state.value.showSuccessDialog
                updateState { copy(showSuccessDialog = false, showFailureDialog = false) }
                if (wasSuccess) {
                    sendEvent(BiometricEvent.UnlockSuccess)
                }
            }
            BiometricIntent.OnForgotPinClicked -> {
                updateState { copy(errorMessage = "Contact support to reset security credentials.") }
            }
            BiometricIntent.OnBackClicked -> {
                sendEvent(BiometricEvent.NavigateBack)
            }
        }
    }
}
