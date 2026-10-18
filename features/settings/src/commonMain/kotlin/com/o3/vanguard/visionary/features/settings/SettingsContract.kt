package com.o3.vanguard.visionary.features.settings

import androidx.compose.runtime.Immutable
import com.o3.vanguard.visionary.core.mvi.UiEvent
import com.o3.vanguard.visionary.core.mvi.UiIntent
import com.o3.vanguard.visionary.core.mvi.UiState
import com.o3.vanguard.visionary.domain.model.AppSettings
import com.o3.vanguard.visionary.domain.model.IndexProgress

@Immutable
data class SettingsState(
    val settings: AppSettings = AppSettings(),
    val indexProgress: IndexProgress? = null,
) : UiState

sealed interface SettingsIntent : UiIntent {
    data class OnChargingOnlyToggled(val enabled: Boolean) : SettingsIntent
    data object OnOpenIndexHealthClicked : SettingsIntent
    data object OnBackClicked : SettingsIntent
}

sealed interface SettingsEvent : UiEvent {
    data object OpenIndexHealth : SettingsEvent
    data object NavigateBack : SettingsEvent
}
