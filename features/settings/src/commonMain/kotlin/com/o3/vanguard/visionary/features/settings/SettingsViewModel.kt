package com.o3.vanguard.visionary.features.settings

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.domain.repository.IndexRepository
import com.o3.vanguard.visionary.domain.repository.SettingsRepository
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val indexRepository: IndexRepository,
) : BaseMviViewModel<SettingsState, SettingsIntent, SettingsEvent>(SettingsState()) {

    init {
        viewModelScope.launch {
            settingsRepository.observe().collect { s ->
                updateState { copy(settings = s) }
            }
        }
        viewModelScope.launch {
            indexRepository.observeProgress().collect { p ->
                updateState { copy(indexProgress = p) }
            }
        }
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.OnChargingOnlyToggled -> {
                viewModelScope.launch {
                    settingsRepository.update { it.copy(indexOnlyWhileCharging = intent.enabled) }
                }
            }
            SettingsIntent.OnOpenIndexHealthClicked -> sendEvent(SettingsEvent.OpenIndexHealth)
            SettingsIntent.OnBackClicked -> sendEvent(SettingsEvent.NavigateBack)
        }
    }
}
