package com.o3.vanguard.visionary.features.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Welcome to Visionary", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Page ${state.currentPage + 1} of 3", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(32.dp))

            if (!state.permissionGranted) {
                Button(onClick = { onIntent(OnboardingIntent.OnGrantPermissionClicked) }) {
                    Text("Grant Permission")
                }
            } else {
                Text("Permission Granted!", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(24.dp))
            if (state.currentPage < 2) {
                Button(onClick = { onIntent(OnboardingIntent.OnNextPageClicked) }) {
                    Text("Next")
                }
            } else {
                Button(onClick = { onIntent(OnboardingIntent.OnCompleteClicked) }) {
                    Text("Get Started")
                }
            }
        }
    }
}
