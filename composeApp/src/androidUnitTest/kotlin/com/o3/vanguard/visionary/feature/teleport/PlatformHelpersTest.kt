package com.o3.vanguard.visionary.feature.teleport

import com.o3.vanguard.visionary.feature.teleport.data.AndroidThumbnailMaker
import com.o3.vanguard.visionary.feature.teleport.data.MediaStoreReceivedPhotoStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatformHelpersTest {

    @Test
    fun sanitizeFileName_replacesInvalidCharacters() {
        val raw = "photo:my*file?.jpg"
        val clean = MediaStoreReceivedPhotoStore.sanitizeFileName(raw)
        assertEquals("photo_my_file_.jpg", clean)
    }

    @Test
    fun sanitizeFileName_handlesBlank() {
        val clean = MediaStoreReceivedPhotoStore.sanitizeFileName("   ")
        assertTrue(clean.startsWith("teleport_"))
    }

    @Test
    fun calculateTargetSize_preservesAspectRatio() {
        val (w, h) = AndroidThumbnailMaker.calculateTargetSize(1920, 1080, maxSide = 320)
        assertEquals(320, w)
        assertEquals(180, h)
    }

    @Test
    fun calculateSampleSize_computesCorrectPowerOfTwo() {
        val sample = AndroidThumbnailMaker.calculateSampleSize(1280, 720, reqSide = 320)
        assertEquals(2, sample)
    }

    @Test
    fun requiredPermissions_returnsCorrectPermissionsForSdk() {
        val tiramisuPerms = com.o3.vanguard.visionary.feature.teleport.platform.AndroidReadiness.requiredPermissions(33)
        assertTrue(tiramisuPerms.contains(android.Manifest.permission.NEARBY_WIFI_DEVICES))

        val legacyPerms = com.o3.vanguard.visionary.feature.teleport.platform.AndroidReadiness.requiredPermissions(28)
        assertTrue(legacyPerms.contains(android.Manifest.permission.ACCESS_COARSE_LOCATION))
    }
}
