package com.o3.vanguard.visionary.features.vault.vault

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.o3.vanguard.visionary.core.designsystem.AppTab
import com.o3.vanguard.visionary.core.navigation.AppRoute
import com.o3.vanguard.visionary.core.navigation.Navigator

fun EntryProviderScope<NavKey>.vaultEntries(navigator: Navigator) {
    entry<AppRoute.VaultLock> {
        VaultLockRoute(
            onUnlocked = { navigator.replaceTop(AppRoute.BiometricSetup) },
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
