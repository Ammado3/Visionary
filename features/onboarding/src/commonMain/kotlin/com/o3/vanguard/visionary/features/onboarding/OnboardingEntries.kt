package com.o3.vanguard.visionary.features.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.o3.vanguard.visionary.core.navigation.AppRoute
import com.o3.vanguard.visionary.core.navigation.Navigator
import com.o3.vanguard.visionary.domain.port.BackgroundSyncScheduler
import com.o3.vanguard.visionary.domain.port.PermissionGateway
import com.o3.vanguard.visionary.platform.permission.AppPermissionGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

fun EntryProviderScope<NavKey>.onboardingEntries(navigator: Navigator) {
    entry<AppRoute.Onboarding> { key ->
        var showPermissionGate by remember { mutableStateOf(key.skipIntro) }
        var navigated by remember { mutableStateOf(false) }
        val permissionGateway = koinInject<PermissionGateway>()
        val indexScheduler = koinInject<BackgroundSyncScheduler>()
        val appScope = koinInject<CoroutineScope>()

        if (showPermissionGate) {
            AppPermissionGate(
                titleText = "Gallery & Media Access Required",
                onPermissionsReady = {
                    if (navigated) return@AppPermissionGate
                    navigated = true
                    appScope.launch {
                        permissionGateway.requestAccess()
                        indexScheduler.requestIndexing()
                    }
                    navigator.replaceAll(AppRoute.Photos)
                },
            )
        } else {
            OnboardingRoute(
                onComplete = { showPermissionGate = true },
            )
        }
    }
}
