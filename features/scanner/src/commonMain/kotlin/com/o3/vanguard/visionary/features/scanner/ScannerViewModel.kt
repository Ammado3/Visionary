package com.o3.vanguard.visionary.features.scanner

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.domain.usecase.StartScanUseCase
import kotlinx.coroutines.launch

class ScannerViewModel(
    private val startScanUseCase: StartScanUseCase,
) : BaseMviViewModel<ScannerState, ScannerIntent, ScannerEvent>(ScannerState()) {

    override fun onIntent(intent: ScannerIntent) {
        when (intent) {
            ScannerIntent.OnStartScanClicked -> {
                updateState { copy(scanning = true, error = null) }
                viewModelScope.launch {
                    when (val result = startScanUseCase()) {
                        is AppResult.Ok -> {
                            updateState { copy(scanning = false, document = result.value) }
                            sendEvent(ScannerEvent.NavigateToScansCollection)
                        }
                        is AppResult.Err -> {
                            updateState { copy(scanning = false, error = "Scan failed") }
                        }
                    }
                }
            }
            ScannerIntent.OnBackClicked -> sendEvent(ScannerEvent.NavigateBack)
        }
    }
}
