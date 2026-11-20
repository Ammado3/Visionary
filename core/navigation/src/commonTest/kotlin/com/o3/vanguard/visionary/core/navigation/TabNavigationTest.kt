package com.o3.vanguard.visionary.core.navigation

import androidx.navigation3.runtime.NavKey
import com.o3.vanguard.visionary.core.designsystem.AppTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TabNavigationTest {

    @Test
    fun everyTabRootRouteRoundTripsToTab() {
        for (tab in AppTab.entries) {
            assertEquals(tab, tab.rootRoute().tabOrNull())
        }
    }

    @Test
    fun vaultGridMapsToVaultTab() {
        assertEquals(AppTab.VAULT, AppRoute.VaultGrid.tabOrNull())
    }

    @Test
    fun nonTabRoutesReturnNullForTabOrNull() {
        assertNull(AppRoute.Settings.tabOrNull())
        assertNull(AppRoute.Viewer(1L).tabOrNull())
        assertNull(AppRoute.TrashBin.tabOrNull())
    }

    @Test
    fun handleBackOnAlbumsReturnsTrueAndNavigatesToPhotos() {
        val stack = mutableListOf<NavKey>(AppRoute.Albums)
        val navigator = Navigator(stack)

        val handled = navigator.handleBack()

        assertTrue(handled)
        assertEquals(listOf<NavKey>(AppRoute.Photos), stack)
    }

    @Test
    fun handleBackOnPhotosReturnsFalseAndLeavesStackUnchanged() {
        val stack = mutableListOf<NavKey>(AppRoute.Photos)
        val navigator = Navigator(stack)

        val handled = navigator.handleBack()

        assertFalse(handled)
        assertEquals(listOf<NavKey>(AppRoute.Photos), stack)
    }

    @Test
    fun handleBackOnPhotosAndSettingsReturnsTrueAndNavigatesToPhotos() {
        val stack = mutableListOf<NavKey>(AppRoute.Photos, AppRoute.Settings)
        val navigator = Navigator(stack)

        val handled = navigator.handleBack()

        assertTrue(handled)
        assertEquals(listOf<NavKey>(AppRoute.Photos), stack)
    }

    @Test
    fun navigateToTabSwapsTopRoute() {
        val stack = mutableListOf<NavKey>(AppRoute.Photos)
        val navigator = Navigator(stack)

        navigator.navigateToTab(AppTab.SEARCH)

        assertEquals(listOf<NavKey>(AppRoute.Search()), stack)
    }
}
