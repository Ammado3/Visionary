package com.o3.vanguard.visionary.features.vault.biometricsetup

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.domain.port.BiometricAuthenticator
import com.o3.vanguard.visionary.features.vault.biometricsetup.states.BiometricEvent
import com.o3.vanguard.visionary.features.vault.biometricsetup.states.BiometricIntent
import com.o3.vanguard.visionary.features.vault.biometricsetup.states.BiometricState
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
            BiometricIntent.OnUnlockClicked -> {
                if (state.value.pin.length < 4) {
                    updateState { copy(errorMessage = "Please enter at least 4 digits PIN.") }
                } else {
                    sendEvent(BiometricEvent.UnlockSuccess)
                }
            }
            BiometricIntent.OnBiometricClicked -> {
                viewModelScope.launch {
                    when (biometricAuthenticator.authenticate()) {
                        is AppResult.Ok -> {
                            updateState {
                                copy(
                                    showSuccessDialog = true,
                                    dialogMessage = "Biometric authentication successful!"
                                )
                            }
                        }
                        is AppResult.Err -> {
                            updateState {
                                copy(
                                    showFailureDialog = true,
                                    dialogMessage = "Biometric authentication failed or cancelled."
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
                updateState { copy(errorMessage = "Contact support to reset PIN.") }
            }
            BiometricIntent.OnBackClicked -> {
                sendEvent(BiometricEvent.NavigateBack)
            }
        }
    }
}
