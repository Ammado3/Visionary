package com.o3.vanguard.visionary.feature.teleport.domain.port

interface ThumbnailMaker {
    /** Creates a small JPEG (max ~320px) in app cache and returns its absolute path. */
    suspend fun make(photoUri: String): String
}

interface ReceivedPhotoStore {
    /** Moves/copies the staged file into the device gallery. Returns the saved content URI string. */
    suspend fun save(stagedPath: String, fileName: String, mimeType: String): String
}

interface DeviceNameProvider {
    fun deviceName(): String
}

interface TeleportAvailability {
    val isSupported: Boolean
}
