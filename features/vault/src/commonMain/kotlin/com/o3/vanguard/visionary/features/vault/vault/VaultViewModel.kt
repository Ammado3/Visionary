package com.o3.vanguard.visionary.features.vault.vault

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.domain.model.VaultItem
import com.o3.vanguard.visionary.domain.port.BiometricAuthenticator
import com.o3.vanguard.visionary.domain.repository.VaultRepository
import com.o3.vanguard.visionary.features.vault.vault.states.VaultEvent
import com.o3.vanguard.visionary.features.vault.vault.states.VaultIntent
import com.o3.vanguard.visionary.features.vault.vault.states.VaultState
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
            is VaultIntent.OnItemSelected -> {
                val current = state.value.selectedItemIds
                val updated = if (current.contains(intent.itemId)) current - intent.itemId else current + intent.itemId
                updateState { copy(selectedItemIds = updated) }
            }
            VaultIntent.OnSelectAllToggled -> {
                val filtered = filteredItems()
                val allIds = filtered.map { it.id }.toSet()
                val updated = if (state.value.selectedItemIds.size == allIds.size) emptySet() else allIds
                updateState { copy(selectedItemIds = updated) }
            }
            is VaultIntent.OnTabSelected -> {
                updateState { copy(selectedTab = intent.tab, selectedItemIds = emptySet()) }
            }
            VaultIntent.OnRemoveSelectedClicked -> {
                if (state.value.selectedItemIds.isNotEmpty()) {
                    updateState { copy(showRemoveDialog = true) }
                }
            }
            VaultIntent.OnRestoreConfirmed -> {
                val ids = state.value.selectedItemIds
                viewModelScope.launch {
                    for (id in ids) {
                        vaultRepository.restore(id)
                    }
                    updateState { copy(selectedItemIds = emptySet(), showRemoveDialog = false) }
                }
            }
            VaultIntent.OnDeletePermanentlyConfirmed -> {
                val ids = state.value.selectedItemIds
                viewModelScope.launch {
                    for (id in ids) {
                        vaultRepository.delete(id)
                    }
                    updateState { copy(selectedItemIds = emptySet(), showRemoveDialog = false) }
                }
            }
            VaultIntent.OnDismissRemoveDialog -> {
                updateState { copy(showRemoveDialog = false) }
            }
            VaultIntent.OnBackClicked -> sendEvent(VaultEvent.NavigateToHome)
        }
    }

    private fun filteredItems(): List<VaultItem> {
        val items = state.value.items
        return when (state.value.selectedTab) {
            VaultTab.PHOTOS -> items.filter { !it.mime.contains("video", ignoreCase = true) }
            VaultTab.VIDEOS -> items.filter { it.mime.contains("video", ignoreCase = true) }
        }
    }
}
