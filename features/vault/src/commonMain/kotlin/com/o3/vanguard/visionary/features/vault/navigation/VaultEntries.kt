package com.o3.vanguard.visionary.features.vault.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.o3.vanguard.visionary.core.designsystem.AppTab
import com.o3.vanguard.visionary.core.navigation.AppRoute
import com.o3.vanguard.visionary.core.navigation.Navigator
import com.o3.vanguard.visionary.features.vault.biometricsetup.BiometricSetupRoute
import com.o3.vanguard.visionary.features.vault.valutgridscreen.VaultGridRoute
import com.o3.vanguard.visionary.features.vault.valutgridscreen.VaultViewerRoute
import com.o3.vanguard.visionary.features.vault.vault.VaultLockRoute

fun EntryProviderScope<NavKey>.vaultEntries(navigator: Navigator) {
    entry<AppRoute.VaultLock> {
        VaultLockRoute(
            onOpenBiometricSetup = { navigator.replaceTop(AppRoute.BiometricSetup) },
            onBack = { navigator.back() },
            onNavigate = { tab ->
                when (tab) {
                    AppTab.PHOTOS -> navigator.replaceTop(AppRoute.Photos)
                    AppTab.ALBUMS -> navigator.replaceTop(AppRoute.Albums)
                    AppTab.SEARCH -> navigator.replaceTop(AppRoute.Search())
                    AppTab.VAULT -> Unit
                }
            },
        )
    }
    entry<AppRoute.BiometricSetup> {
        BiometricSetupRoute(
            onUnlocked = { navigator.replaceTop(AppRoute.VaultGrid) },
            onBack = { navigator.back() },
            onNavigate = { tab ->
                when (tab) {
                    AppTab.PHOTOS -> navigator.replaceTop(AppRoute.Photos)
                    AppTab.ALBUMS -> navigator.replaceTop(AppRoute.Albums)
                    AppTab.SEARCH -> navigator.replaceTop(AppRoute.Search())
                    AppTab.VAULT -> Unit
                }
            },
        )
    }
    entry<AppRoute.VaultGrid> {
        VaultGridRoute(
            onOpenViewer = { itemId -> navigator.navigate(AppRoute.VaultViewer(itemId)) },
            onMoveToBiometric = { navigator.navigate(AppRoute.BiometricSetup) },
            onLock = { navigator.replaceTop(AppRoute.VaultLock) },
        )
    }
    entry<AppRoute.VaultViewer> { key ->
        VaultViewerRoute(
            itemId = key.itemId,
            onBack = { navigator.back() },
        )
    }
}
