package com.o3.vanguard.visionary.features.scanner

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.o3.vanguard.visionary.core.navigation.AppRoute
import com.o3.vanguard.visionary.core.navigation.Navigator

fun EntryProviderScope<NavKey>.scannerEntries(navigator: Navigator) {
    entry<AppRoute.Scanner> {
        ScannerRoute(
            onOpenScans = { navigator.replaceTop(AppRoute.Gallery("scans")) },
            onBack = { navigator.back() },
        )
    }
}
