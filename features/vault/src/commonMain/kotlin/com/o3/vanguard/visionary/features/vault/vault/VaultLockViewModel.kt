package com.o3.vanguard.visionary.features.vault.vault

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.domain.repository.VaultRepository
import com.o3.vanguard.visionary.features.vault.vault.states.VaultLockEvent
import com.o3.vanguard.visionary.features.vault.vault.states.VaultLockIntent
import com.o3.vanguard.visionary.features.vault.vault.states.VaultLockState
import kotlinx.coroutines.launch

class VaultLockViewModel(
    private val vaultRepository: VaultRepository,
) : BaseMviViewModel<VaultLockState, VaultLockIntent, VaultLockEvent>(VaultLockState()) {

    init {
        viewModelScope.launch {
            vaultRepository.isUnlocked.collect { unlocked ->
                updateState { copy(isUnlocked = unlocked) }
            }
        }
    }

    override fun onIntent(intent: VaultLockIntent) {
        when (intent) {
            VaultLockIntent.OnSetupVaultClicked -> {
                sendEvent(VaultLockEvent.OpenBiometricSetup)
            }
            VaultLockIntent.OnBackClicked -> {
                sendEvent(VaultLockEvent.NavigateToHome)
            }
        }
    }
}
