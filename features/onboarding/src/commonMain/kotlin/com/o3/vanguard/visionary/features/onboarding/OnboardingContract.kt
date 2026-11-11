package com.o3.vanguard.visionary.features.onboarding

import androidx.compose.runtime.Immutable
import com.o3.vanguard.visionary.core.mvi.UiEvent
import com.o3.vanguard.visionary.core.mvi.UiIntent
import com.o3.vanguard.visionary.core.mvi.UiState

@Immutable
data class OnboardingState(
    val currentPage: Int = 0,
    val permissionGranted: Boolean = false,
) : UiState

sealed interface OnboardingIntent : UiIntent {
    data object OnNextPageClicked : OnboardingIntent
    data object OnPreviousPageClicked : OnboardingIntent
    data object OnGrantPermissionClicked : OnboardingIntent
    data object OnCompleteClicked : OnboardingIntent
}

sealed interface OnboardingEvent : UiEvent {
    data object NavigateToHome : OnboardingEvent
}
