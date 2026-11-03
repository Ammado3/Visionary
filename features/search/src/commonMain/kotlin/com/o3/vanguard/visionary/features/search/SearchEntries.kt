package com.o3.vanguard.visionary.features.search

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.o3.vanguard.visionary.core.navigation.AppRoute
import com.o3.vanguard.visionary.core.navigation.Navigator
import com.o3.vanguard.visionary.core.designsystem.AppTab

fun EntryProviderScope<NavKey>.searchEntries(navigator: Navigator) {
    entry<AppRoute.Search> { key ->
        SearchRoute(
            initialQuery = key.initialQuery,
            onOpenViewer = { id, highlight -> navigator.navigate(AppRoute.Viewer(id.value, highlight)) },
            onBack = { navigator.back() },
            onNavigate = { tab ->
                when (tab) {
                    AppTab.PHOTOS -> navigator.replaceTop(AppRoute.Home)
                    AppTab.ALBUMS -> navigator.replaceTop(AppRoute.Albums)
                    AppTab.VAULT -> navigator.replaceTop(AppRoute.VaultLock)
                    AppTab.SEARCH -> Unit
                }
            },
        )
    }
}
