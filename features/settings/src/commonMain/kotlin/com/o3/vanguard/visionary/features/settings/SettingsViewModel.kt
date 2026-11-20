package com.o3.vanguard.visionary.features.settings

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.domain.port.BackgroundSyncScheduler
import com.o3.vanguard.visionary.domain.repository.IndexRepository
import com.o3.vanguard.visionary.domain.repository.SettingsRepository
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val indexRepository: IndexRepository,
    private val indexScheduler: BackgroundSyncScheduler,
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
            is SettingsIntent.OnVisualAnalysisToggled -> {
                viewModelScope.launch {
                    settingsRepository.update { it.copy(visualAnalysisEnabled = intent.enabled) }
                    if (intent.enabled) {
                        indexRepository.resume()
                        // Runs in the foreground service, so leaving Settings doesn't stop it.
                        indexScheduler.requestIndexing()
                    } else {
                        indexRepository.pause()
                    }
                }
            }
            SettingsIntent.OnOpenIndexHealthClicked -> sendEvent(SettingsEvent.OpenIndexHealth)
            SettingsIntent.OnBackClicked -> sendEvent(SettingsEvent.NavigateBack)
        }
    }
}
