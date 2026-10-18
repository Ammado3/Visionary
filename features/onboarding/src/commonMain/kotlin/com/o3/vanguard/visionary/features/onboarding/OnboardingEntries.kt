package com.o3.vanguard.visionary.features.onboarding

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.o3.vanguard.visionary.core.navigation.AppRoute
import com.o3.vanguard.visionary.core.navigation.Navigator

fun EntryProviderScope<NavKey>.onboardingEntries(navigator: Navigator) {
    entry<AppRoute.Onboarding> {
        OnboardingRoute(
            onComplete = { navigator.replaceAll(AppRoute.Home) }
        )
    }
}
