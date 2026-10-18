package com.o3.vanguard.visionary.features.home

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.o3.vanguard.visionary.core.navigation.AppRoute
import com.o3.vanguard.visionary.core.navigation.Navigator

fun EntryProviderScope<NavKey>.homeEntries(navigator: Navigator) {
    entry<AppRoute.Home> {
        HomeRoute(
            onOpenCollection = { navigator.navigate(AppRoute.Gallery(it.id)) },
            onOpenSearch = { navigator.navigate(AppRoute.Search()) },
            onOpenViewer = { navigator.navigate(AppRoute.Viewer(it.value)) },
            onOpenScanner = { navigator.navigate(AppRoute.Scanner) },
            onOpenVault = { navigator.navigate(AppRoute.VaultLock) },
            onOpenSettings = { navigator.navigate(AppRoute.Settings) },
        )
    }
}
