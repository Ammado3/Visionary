package com.o3.vanguard.visionary.features.onboarding

import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.domain.port.PermissionGateway
import com.o3.vanguard.visionary.domain.repository.SettingsRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val settingsRepository: SettingsRepository,
    private val permissionGateway: PermissionGateway,
) : BaseMviViewModel<OnboardingState, OnboardingIntent, OnboardingEvent>(OnboardingState()) {

    override fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            OnboardingIntent.OnNextPageClicked -> {
                updateState { copy(currentPage = (currentPage + 1).coerceAtMost(onboardingPages.lastIndex)) }
            }
            OnboardingIntent.OnPreviousPageClicked -> {
                updateState { copy(currentPage = (currentPage - 1).coerceAtLeast(0)) }
            }
            OnboardingIntent.OnGrantPermissionClicked -> {
                viewModelScope.launch {
                    permissionGateway.requestAccess()
                    updateState { copy(permissionGranted = true) }
                }
            }
            OnboardingIntent.OnCompleteClicked -> {
                viewModelScope.launch {
                    settingsRepository.update { it.copy(onboardingDone = true) }
                    sendEvent(OnboardingEvent.NavigateToHome)
                }
            }
        }
    }
}
