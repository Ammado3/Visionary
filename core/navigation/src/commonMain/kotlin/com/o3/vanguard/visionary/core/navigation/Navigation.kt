package com.o3.vanguard.visionary.core.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable data object Onboarding : AppRoute
    @Serializable data object Home : AppRoute
    @Serializable data object Albums : AppRoute
    @Serializable data class Gallery(val collectionId: String) : AppRoute
    @Serializable data class Viewer(val mediaId: Long, val highlight: String? = null) : AppRoute
    @Serializable data class Search(val initialQuery: String = "") : AppRoute
    @Serializable data object Scanner : AppRoute
    @Serializable data object VaultLock : AppRoute
    @Serializable data object VaultGrid : AppRoute
    @Serializable data class VaultViewer(val itemId: String) : AppRoute
    @Serializable data object Settings : AppRoute
    @Serializable data object IndexHealth : AppRoute
    @Serializable data object TrashBin : AppRoute
}

val AppNavSavedStateConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(AppRoute.Onboarding::class, AppRoute.Onboarding.serializer())
            subclass(AppRoute.Home::class, AppRoute.Home.serializer())
            subclass(AppRoute.Albums::class, AppRoute.Albums.serializer())
            subclass(AppRoute.Gallery::class, AppRoute.Gallery.serializer())
            subclass(AppRoute.Viewer::class, AppRoute.Viewer.serializer())
            subclass(AppRoute.Search::class, AppRoute.Search.serializer())
            subclass(AppRoute.Scanner::class, AppRoute.Scanner.serializer())
            subclass(AppRoute.VaultLock::class, AppRoute.VaultLock.serializer())
            subclass(AppRoute.VaultGrid::class, AppRoute.VaultGrid.serializer())
            subclass(AppRoute.VaultViewer::class, AppRoute.VaultViewer.serializer())
            subclass(AppRoute.Settings::class, AppRoute.Settings.serializer())
            subclass(AppRoute.IndexHealth::class, AppRoute.IndexHealth.serializer())
            subclass(AppRoute.TrashBin::class, AppRoute.TrashBin.serializer())
        }
    }
}

class Navigator(private val backStack: MutableList<NavKey>) {
    fun navigate(route: AppRoute) {
        if (backStack.lastOrNull() != route) backStack.add(route)
    }
    fun back(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }
    fun replaceTop(route: AppRoute) {
        if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex)
        backStack.add(route)
    }
    fun replaceAll(route: AppRoute) {
        backStack.clear()
        backStack.add(route)
    }
    fun popTo(route: AppRoute, inclusive: Boolean = false) {
        val index = backStack.indexOfLast { it == route }
        if (index < 0) return
        val keep = if (inclusive) index else index + 1
        while (backStack.size > keep) backStack.removeAt(backStack.lastIndex)
    }
}
