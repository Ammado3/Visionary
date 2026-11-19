package com.o3.vanguard.visionary.features.vault.vault.states

import androidx.compose.runtime.Immutable
import com.o3.vanguard.visionary.core.mvi.UiState

@Immutable
data class VaultLockState(
    val isUnlocked: Boolean = false,
    val authenticating: Boolean = false,
    val error: String? = null,
) : UiState
