package com.o3.vanguard.visionary.features.vault.biometricsetup

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.o3.vanguard.visionary.core.designsystem.AppTab
import com.o3.vanguard.visionary.core.navigation.AppRoute
import com.o3.vanguard.visionary.core.navigation.Navigator
import com.o3.vanguard.visionary.features.vault.vault.VaultLockRoute


fun EntryProviderScope<NavKey>. biometricSetupEntries(navigator: Navigator) {
    entry<AppRoute.BiometricSetup> {
       BiometricSetupRoute(
            onUnlocked = { navigator.replaceTop(AppRoute.VaultGrid) },
            onBack = { navigator.back() },
            onNavigate = { tab ->
                when (tab) {
                    AppTab.PHOTOS -> navigator.replaceTop(AppRoute.Home)
                    AppTab.ALBUMS -> navigator.replaceTop(AppRoute.Albums)
                    AppTab.SEARCH -> navigator.replaceTop(AppRoute.Search())
                    AppTab.VAULT -> Unit
                }
            },
        )
    }
}