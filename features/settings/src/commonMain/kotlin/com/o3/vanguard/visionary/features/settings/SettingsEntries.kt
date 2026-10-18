package com.o3.vanguard.visionary.features.settings

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.o3.vanguard.visionary.core.navigation.AppRoute
import com.o3.vanguard.visionary.core.navigation.Navigator

fun EntryProviderScope<NavKey>.settingsEntries(navigator: Navigator) {
    entry<AppRoute.Settings> {
        SettingsRoute(
            onOpenIndexHealth = { navigator.navigate(AppRoute.IndexHealth) },
            onBack = { navigator.back() },
        )
    }
    entry<AppRoute.IndexHealth> {
        IndexHealthRoute(onBack = { navigator.back() })
    }
}
