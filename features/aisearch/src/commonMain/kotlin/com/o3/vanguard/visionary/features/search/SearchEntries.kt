package com.o3.vanguard.visionary.features.search

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.o3.vanguard.visionary.core.navigation.AppRoute
import com.o3.vanguard.visionary.core.navigation.Navigator
import com.o3.vanguard.visionary.core.designsystem.AppTab

import com.o3.vanguard.visionary.core.navigation.navigateToTab

fun EntryProviderScope<NavKey>.searchEntries(navigator: Navigator) {
    entry<AppRoute.Search> { key ->
        SearchRoute(
            initialQuery = key.initialQuery,
            onOpenViewer = { id, highlight -> navigator.navigate(AppRoute.Viewer(id.value, highlight)) },
            onBack = { navigator.handleBack() },
            onNavigate = { tab -> if (tab != AppTab.SEARCH) navigator.navigateToTab(tab) },
        )
    }
}
