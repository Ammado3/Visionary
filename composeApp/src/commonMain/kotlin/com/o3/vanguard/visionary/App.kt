package com.o3.vanguard.visionary

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.o3.vanguard.visionary.core.designsystem.VisionaryTheme
import com.o3.vanguard.visionary.core.navigation.AppNavSavedStateConfig
import com.o3.vanguard.visionary.core.navigation.AppRoute
import com.o3.vanguard.visionary.core.navigation.Navigator
import com.o3.vanguard.visionary.domain.usecase.ResolveStartRouteUseCase
import com.o3.vanguard.visionary.domain.usecase.StartDestination
import com.o3.vanguard.visionary.features.gallery.galleryEntries
import com.o3.vanguard.visionary.features.home.homeEntries
import com.o3.vanguard.visionary.features.onboarding.onboardingEntries
import com.o3.vanguard.visionary.features.scanner.scannerEntries
import com.o3.vanguard.visionary.features.search.searchEntries
import com.o3.vanguard.visionary.features.settings.settingsEntries
import com.o3.vanguard.visionary.features.vault.vaultEntries
import org.koin.compose.koinInject

@Composable
fun App() {
    VisionaryTheme {
        val resolveStart = koinInject<ResolveStartRouteUseCase>()
        var startRoute by remember { mutableStateOf<AppRoute?>(null) }

        LaunchedEffect(Unit) {
            startRoute = when (resolveStart()) {
                StartDestination.ONBOARDING -> AppRoute.Onboarding
                StartDestination.HOME -> AppRoute.Home
            }
        }

        startRoute?.let { AppNavHost(it) } ?: SplashScreen()
    }
}

@Composable
private fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AppNavHost(start: AppRoute) {
    val backStack = rememberNavBackStack(AppNavSavedStateConfig, start)
    val navigator = remember(backStack) { Navigator(backStack) }

    NavDisplay(
        backStack = backStack,
        onBack = { navigator.back() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            onboardingEntries(navigator)
            homeEntries(navigator)
            galleryEntries(navigator)
            searchEntries(navigator)
            scannerEntries(navigator)
            vaultEntries(navigator)
            settingsEntries(navigator)
        },
    )
}
