package com.o3.vanguard.visionary.features.scanner

import androidx.compose.runtime.Immutable
import com.o3.vanguard.visionary.core.mvi.UiEvent
import com.o3.vanguard.visionary.core.mvi.UiIntent
import com.o3.vanguard.visionary.core.mvi.UiState
import com.o3.vanguard.visionary.domain.model.ScannedDocument

@Immutable
data class ScannerState(
    val scanning: Boolean = false,
    val document: ScannedDocument? = null,
    val error: String? = null,
) : UiState

sealed interface ScannerIntent : UiIntent {
    data object OnStartScanClicked : ScannerIntent
    data object OnBackClicked : ScannerIntent
}

sealed interface ScannerEvent : UiEvent {
    data object NavigateToScansCollection : ScannerEvent
    data object NavigateBack : ScannerEvent
}
