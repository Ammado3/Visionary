package com.o3.vanguard.visionary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.o3.vanguard.visionary.core.designsystem.VisionaryTheme
import com.o3.vanguard.visionary.core.navigation.AppNavSavedStateConfig
import com.o3.vanguard.visionary.core.navigation.AppRoute
import com.o3.vanguard.visionary.core.navigation.Navigator
import com.o3.vanguard.visionary.domain.model.MediaAccess
import com.o3.vanguard.visionary.domain.port.PermissionGateway
import com.o3.vanguard.visionary.features.gallery.galleryEntries
import com.o3.vanguard.visionary.features.home.homeEntries
import com.o3.vanguard.visionary.features.onboarding.onboardingEntries
import com.o3.vanguard.visionary.features.scanner.scannerEntries
import com.o3.vanguard.visionary.features.search.searchEntries
import com.o3.vanguard.visionary.features.settings.settingsEntries
import com.o3.vanguard.visionary.features.vault.vault.vaultEntries
import com.o3.vanguard.visionary.feature.teleport.teleportEntries
import com.o3.vanguard.visionary.screens.SPLASH_DURATION_MS
import com.o3.vanguard.visionary.screens.SplashScreen
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import com.o3.vanguard.visionary.features.vault.biometricsetup.biometricSetupEntries

@Composable
fun App() {
    VisionaryTheme {
        val permissionGateway = koinInject<PermissionGateway>()
        var startRoute by remember { mutableStateOf<AppRoute?>(null) }

        LaunchedEffect(Unit) {
            delay(SPLASH_DURATION_MS)
            startRoute = when (permissionGateway.requestAccess()) {
                MediaAccess.Full,
                is MediaAccess.Partial -> AppRoute.Home
                MediaAccess.Denied -> AppRoute.Onboarding
            }
        }

        startRoute?.let { AppNavHost(it) }
            ?: SplashScreen(
                onSplashFinished = {
                    // Your existing permission flow is handled by LaunchedEffect
                }
            )
    }
}

@Composable
private fun AppNavHost(start: AppRoute) {
    val backStack = rememberNavBackStack(AppNavSavedStateConfig, start)
    val navigator = remember(backStack) { Navigator(backStack) }

    Surface(modifier = Modifier.fillMaxSize(), color = VisionaryColors.colorF4FDF7) {
        Scaffold { paddingValues ->
            NavDisplay(
                modifier = Modifier.fillMaxSize().padding(paddingValues).consumeWindowInsets(paddingValues),
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
                    teleportEntries(navigator)
                    biometricSetupEntries(navigator)
                },
            )
        }
    }
}
