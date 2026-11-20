package com.o3.vanguard.visionary.features.vault.valutgridscreen

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.domain.model.VaultItem
import com.o3.vanguard.visionary.domain.repository.VaultRepository
import com.o3.vanguard.visionary.features.vault.valutgridscreen.states.VaultGridEvent
import com.o3.vanguard.visionary.features.vault.valutgridscreen.states.VaultGridIntent
import com.o3.vanguard.visionary.features.vault.valutgridscreen.states.VaultGridState
import com.o3.vanguard.visionary.features.vault.valutgridscreen.states.VaultGridTab
import kotlinx.coroutines.launch

class VaultGridViewModel(
    private val vaultRepository: VaultRepository,
) : BaseMviViewModel<VaultGridState, VaultGridIntent, VaultGridEvent>(VaultGridState()) {

    init {
        viewModelScope.launch {
            vaultRepository.observeItems().collect { list ->
                updateState { copy(items = list, isLoading = false) }
            }
        }
    }

    override fun onIntent(intent: VaultGridIntent) {
        when (intent) {
            is VaultGridIntent.OnItemClicked -> sendEvent(VaultGridEvent.OpenVaultViewer(intent.itemId))
            is VaultGridIntent.OnItemSelected -> {
                val current = state.value.selectedItemIds
                val updated = if (current.contains(intent.itemId)) current - intent.itemId else current + intent.itemId
                updateState { copy(selectedItemIds = updated) }
            }
            VaultGridIntent.OnSelectAllToggled -> {
                val filtered = filteredItems()
                val allIds = filtered.map { it.id }.toSet()
                val updated = if (state.value.selectedItemIds.size == allIds.size) emptySet() else allIds
                updateState { copy(selectedItemIds = updated) }
            }
            is VaultGridIntent.OnTabSelected -> {
                updateState { copy(selectedTab = intent.tab, selectedItemIds = emptySet()) }
            }
            VaultGridIntent.OnRemoveSelectedClicked -> {
                if (state.value.selectedItemIds.isNotEmpty()) {
                    updateState { copy(showRemoveDialog = true) }
                }
            }
            VaultGridIntent.OnRestoreConfirmed -> {
                val ids = state.value.selectedItemIds
                viewModelScope.launch {
                    for (id in ids) {
                        vaultRepository.restore(id)
                    }
                    updateState { copy(selectedItemIds = emptySet(), showRemoveDialog = false) }
                }
            }
            VaultGridIntent.OnDeletePermanentlyConfirmed -> {
                val ids = state.value.selectedItemIds
                viewModelScope.launch {
                    for (id in ids) {
                        vaultRepository.delete(id)
                    }
                    updateState { copy(selectedItemIds = emptySet(), showRemoveDialog = false) }
                }
            }
            VaultGridIntent.OnDismissRemoveDialog -> {
                updateState { copy(showRemoveDialog = false) }
            }
            VaultGridIntent.OnBackClicked -> sendEvent(VaultGridEvent.NavigateToHome)
        }
    }

    private fun filteredItems(): List<VaultItem> {
        val items = state.value.items
        return when (state.value.selectedTab) {
            VaultGridTab.PHOTOS -> items.filter { !it.mime.contains("video", ignoreCase = true) }
            VaultGridTab.VIDEOS -> items.filter { it.mime.contains("video", ignoreCase = true) }
        }
    }
}
