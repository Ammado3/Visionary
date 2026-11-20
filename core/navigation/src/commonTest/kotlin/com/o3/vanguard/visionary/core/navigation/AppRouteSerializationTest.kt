package com.o3.vanguard.visionary.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AppRouteSerializationTest {
    private val json = Json { serializersModule = AppRouteSerializersModule }

    // One sample per AppRoute subclass. When you add a route, add it here too.
    private val samples: List<AppRoute> = listOf(
        AppRoute.Onboarding,
        AppRoute.Photos,
        AppRoute.Albums,
        AppRoute.Gallery(albumId = "1", albumName = "Camera"),
        AppRoute.Viewer(mediaId = 1L, highlight = "total"),
        AppRoute.Search(initialQuery = "receipt"),
        AppRoute.VaultLock,
        AppRoute.VaultGrid,
        AppRoute.VaultViewer(itemId = "v1"),
        AppRoute.Settings,
        AppRoute.IndexHealth,
        AppRoute.BiometricSetup,
        AppRoute.TrashBin,
        AppRoute.Favorites,
        AppRoute.TeleportSender(uri = "content://media/1", displayName = "a.jpg"),
        AppRoute.TeleportReceiver,
        AppRoute.VoiceSearch,
    )

    @Test
    fun everyRouteHasASample() {
        // A sealed serializer's descriptor is ("type", "value"); "value" has one element per subclass.
        val subclassCount = AppRoute.serializer().descriptor.getElementDescriptor(1).elementsCount
        assertEquals(subclassCount, samples.map { it::class }.distinct().size)
    }

    @Test
    fun everyRouteRoundTripsAsNavKey() {
        val serializer = PolymorphicSerializer(NavKey::class)
        samples.forEach { route ->
            val encoded = json.encodeToString(serializer, route)
            assertEquals<NavKey>(route, json.decodeFromString(serializer, encoded))
        }
    }
}
