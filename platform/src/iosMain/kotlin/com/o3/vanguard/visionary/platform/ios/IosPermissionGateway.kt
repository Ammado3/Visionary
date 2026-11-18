package com.o3.vanguard.visionary.platform.ios

import com.o3.vanguard.visionary.domain.model.MediaAccess
import com.o3.vanguard.visionary.domain.port.PermissionGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Photos.PHAccessLevelReadWrite
import platform.Photos.PHAuthorizationStatus
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHPhotoLibrary
import kotlin.coroutines.resume

class IosPermissionGateway : PermissionGateway {
    private val access = MutableStateFlow(currentAccess())

    override fun observeAccess(): Flow<MediaAccess> = access

    override suspend fun requestAccess(): MediaAccess {
        val current = currentAccess()
        if (current !is MediaAccess.Denied) {
            access.value = current
            return current
        }

        val status = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite)
        if (status != PHAuthorizationStatusNotDetermined) {
            val mapped = mapStatus(status)
            access.value = mapped
            return mapped
        }

        return suspendCancellableCoroutine { continuation ->
            PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelReadWrite) { newStatus ->
                val mapped = mapStatus(newStatus)
                access.value = mapped
                if (continuation.isActive) {
                    continuation.resume(mapped)
                }
            }
        }
    }

    private fun currentAccess(): MediaAccess {
        val status = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite)
        return mapStatus(status)
    }

    private fun mapStatus(status: PHAuthorizationStatus): MediaAccess = when (status) {
        PHAuthorizationStatusAuthorized -> MediaAccess.Full
        PHAuthorizationStatusLimited -> MediaAccess.Partial(count = 0)
        PHAuthorizationStatusDenied,
        PHAuthorizationStatusRestricted,
        PHAuthorizationStatusNotDetermined -> MediaAccess.Denied
        else -> MediaAccess.Denied
    }
}
