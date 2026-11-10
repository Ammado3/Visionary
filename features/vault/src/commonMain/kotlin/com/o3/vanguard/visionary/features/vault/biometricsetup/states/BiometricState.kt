package com.o3.vanguard.visionary.features.vault.biometricsetup.states

import com.o3.vanguard.visionary.core.mvi.UiState

data class BiometricState(
    val pin: String = "",
    val errorMessage: String? = null,
    val rowNumber:List<List<String>> = listOf(
        listOf("1","2","3"),
        listOf("4","5","6"),
        listOf("7","8","9"),
        listOf("biometric","0","delete"),
    ),
    val isUnlocked: Boolean = false,
    val isLoading: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val showFailureDialog: Boolean = false,
    val dialogMessage: String = "",
) : UiState
