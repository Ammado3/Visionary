package com.o3.vanguard.visionary.core.navigation

import com.o3.vanguard.visionary.core.designsystem.AppTab

/** The route a bottom-navigation tab opens. */
fun AppTab.rootRoute(): AppRoute = when (this) {
    AppTab.PHOTOS -> AppRoute.Photos
    AppTab.ALBUMS -> AppRoute.Albums
    AppTab.SEARCH -> AppRoute.Search()
    AppTab.VAULT -> AppRoute.VaultLock
}

/** The tab to highlight for this route, or null when the bottom bar is hidden. */
fun AppRoute.tabOrNull(): AppTab? = when (this) {
    AppRoute.Photos -> AppTab.PHOTOS
    AppRoute.Albums -> AppTab.ALBUMS
    is AppRoute.Search -> AppTab.SEARCH
    AppRoute.VaultLock, AppRoute.VaultGrid -> AppTab.VAULT
    else -> null
}

/** Switch tabs. Tabs replace each other; they don't stack. */
fun Navigator.navigateToTab(tab: AppTab) {
    replaceTop(tab.rootRoute())
}
