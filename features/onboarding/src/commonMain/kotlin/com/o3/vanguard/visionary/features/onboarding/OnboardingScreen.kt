package com.o3.vanguard.visionary.features.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.o3.vanguard.visionary.core.mvi.CollectEvents
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingRoute(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            OnboardingEvent.NavigateToHome -> onComplete()
        }
    }
    OnboardingScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onIntent: (OnboardingIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.currentPage) {
        0 -> OnBoardingFirst(
            onNext = { onIntent(OnboardingIntent.OnNextPageClicked) },
            onSkip = { onIntent(OnboardingIntent.OnCompleteClicked) },
            modifier = modifier,
        )

        1 -> OnboardingSearchPage(
            onNext = { onIntent(OnboardingIntent.OnNextPageClicked) },
            onSkip = { onIntent(OnboardingIntent.OnCompleteClicked) },
            modifier = modifier,
        )

        else -> OnboardingDescribePage(
            onGetStarted = { onIntent(OnboardingIntent.OnCompleteClicked) },
            onSkip = { onIntent(OnboardingIntent.OnCompleteClicked) },
            modifier = modifier,
        )
    }
}