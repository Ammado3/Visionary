package com.o3.vanguard.visionary.features.vault

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.o3.vanguard.visionary.core.navigation.AppRoute
import com.o3.vanguard.visionary.core.navigation.Navigator

fun EntryProviderScope<NavKey>.vaultEntries(navigator: Navigator) {
    entry<AppRoute.VaultLock> {
        VaultLockRoute(
            onUnlocked = { navigator.replaceTop(AppRoute.VaultGrid) },
            onBack = { navigator.back() },
        )
    }
    entry<AppRoute.VaultGrid> {
        VaultGridRoute(
            onOpenViewer = { itemId -> navigator.navigate(AppRoute.VaultViewer(itemId)) },
            onLock = { navigator.popTo(AppRoute.Home) },
        )
    }
    entry<AppRoute.VaultViewer> { key ->
        VaultViewerRoute(
            itemId = key.itemId,
            onBack = { navigator.back() },
        )
    }
}
