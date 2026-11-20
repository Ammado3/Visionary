package com.o3.vanguard.visionary

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.o3.vanguard.visionary.core.designsystem.AppBottomNavigation
import com.o3.vanguard.visionary.core.designsystem.AppTab
import com.o3.vanguard.visionary.core.designsystem.SystemBarColors
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import com.o3.vanguard.visionary.core.designsystem.VisionaryTheme
import com.o3.vanguard.visionary.core.navigation.AppNavSavedStateConfig
import com.o3.vanguard.visionary.core.navigation.AppRoute
import com.o3.vanguard.visionary.core.navigation.Navigator
import com.o3.vanguard.visionary.core.navigation.tabOrNull
import com.o3.vanguard.visionary.core.navigation.navigateToTab
import com.o3.vanguard.visionary.domain.model.MediaAccess
import com.o3.vanguard.visionary.domain.port.PermissionGateway
import com.o3.vanguard.visionary.domain.repository.IndexRepository
import com.o3.vanguard.visionary.feature.teleport.teleportEntries
import com.o3.vanguard.visionary.features.gallery.navigation.galleryEntries
import com.o3.vanguard.visionary.features.onboarding.onboardingEntries
import com.o3.vanguard.visionary.features.search.searchEntries
import com.o3.vanguard.visionary.features.settings.settingsEntries

import com.o3.vanguard.visionary.platform.getPlatformDecoderFactories
import com.o3.vanguard.visionary.features.vault.navigation.vaultEntries
import com.o3.vanguard.visionary.platform.getPlatformFetcherFactory
import com.o3.vanguard.visionary.screens.SPLASH_DURATION_MS
import com.o3.vanguard.visionary.screens.SplashScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private data class SystemBarConfig(
    val statusBarColor: Color,
    val darkStatusBarIcons: Boolean,
    val navBarColor: Color,
    val darkNavBarIcons: Boolean,
)

@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                getPlatformFetcherFactory()?.let { add(it) }
                getPlatformDecoderFactories().forEach { add(it) }
            }
            .build()
    }

    VisionaryTheme {
        val permissionGateway = koinInject<PermissionGateway>()
        val indexRepository = koinInject<IndexRepository>()
        var startRoute by remember { mutableStateOf<AppRoute?>(null) }

        LaunchedEffect(Unit) {
            delay(SPLASH_DURATION_MS)
            val access = permissionGateway.requestAccess()
            if (access is MediaAccess.Full || access is MediaAccess.Partial) {
                launch { indexRepository.rebuild() }
                startRoute = AppRoute.Photos
            } else {
                startRoute = AppRoute.Onboarding
            }
        }

        startRoute?.let { AppNavHost(it) }
            ?: SplashScreen()
    }
}

@Composable
private fun AppNavHost(start: AppRoute) {
    val backStack = rememberNavBackStack(AppNavSavedStateConfig, start)
    val navigator = remember(backStack) { Navigator(backStack) }

    val currentRoute = backStack.lastOrNull() as? AppRoute
    val currentTab: AppTab? = currentRoute?.tabOrNull()
    val isSecondaryTab = currentTab != null && currentTab != AppTab.PHOTOS

    AppBackHandler(enabled = isSecondaryTab) {
        navigator.navigateToTab(AppTab.PHOTOS)
    }

    val systemBarConfig = when (currentRoute) {
        is AppRoute.TeleportSender, AppRoute.TeleportReceiver -> {
            val darkGreen = Color(0xFF050B09)
            SystemBarConfig(darkGreen, false, darkGreen, false)
        }
        is AppRoute.Viewer, is AppRoute.VaultViewer -> {
            SystemBarConfig(Color.Black, false, Color.Black, false)
        }
        else -> {
            SystemBarConfig(VisionaryColors.colorF4FDF7, true, VisionaryColors.colorF4FDF7, true)
        }
    }

    SystemBarColors(
        statusBarColor = systemBarConfig.statusBarColor,
        darkStatusBarIcons = systemBarConfig.darkStatusBarIcons,
        navigationBarColor = systemBarConfig.navBarColor,
        darkNavigationBarIcons = systemBarConfig.darkNavBarIcons,
    )

    Surface(modifier = Modifier.fillMaxSize(), color = systemBarConfig.navBarColor) {
        Scaffold(
            containerColor = systemBarConfig.navBarColor,
            bottomBar = {
                currentTab?.let { selectedTab ->
                    AppBottomNavigation(
                        selected = selectedTab,
                        onSelected = { tab -> navigator.navigateToTab(tab) }
                    )
                }
            }
        ) { paddingValues ->
            NavDisplay(
                modifier = Modifier.fillMaxSize().padding(paddingValues).consumeWindowInsets(paddingValues),
                backStack = backStack,
                onBack = { navigator.handleBack() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    onboardingEntries(navigator)
                    galleryEntries(navigator)
                    searchEntries(navigator)
                    vaultEntries(navigator)
                    settingsEntries(navigator)
                    teleportEntries(navigator)
                },
            )
        }
    }
}
