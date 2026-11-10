package com.o3.vanguard.visionary.features.vault.vault

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.domain.port.BiometricAuthenticator
import com.o3.vanguard.visionary.domain.repository.VaultRepository
import kotlinx.coroutines.launch

class VaultViewModel(
    private val vaultRepository: VaultRepository,
    private val biometricAuthenticator: BiometricAuthenticator,
) : BaseMviViewModel<VaultState, VaultIntent, VaultEvent>(VaultState()) {

    init {
        viewModelScope.launch {
            vaultRepository.isUnlocked.collect { unlocked ->
                updateState { copy(isUnlocked = unlocked) }
            }
        }
        viewModelScope.launch {
            vaultRepository.observeItems().collect { list ->
                updateState { copy(items = list) }
            }
        }
    }

    override fun onIntent(intent: VaultIntent) {
        when (intent) {
            VaultIntent.OnUnlockClicked -> {
                updateState { copy(authenticating = true) }
                viewModelScope.launch {
                    sendEvent(VaultEvent.OpenVaultGrid)
                }
            }
            VaultIntent.OnLockClicked -> {
                vaultRepository.lock()
                sendEvent(VaultEvent.NavigateToHome)
            }
            is VaultIntent.OnItemClicked -> sendEvent(VaultEvent.OpenVaultViewer(intent.itemId))
            VaultIntent.OnBackClicked -> sendEvent(VaultEvent.NavigateToHome)
        }
    }
}
