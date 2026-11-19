package com.o3.vanguard.visionary.platform.ios

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Photos.*
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.coroutines.resume

object PhAssetResolver {

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    suspend fun fetchImageData(
        phUri: String,
        targetWidth: Double = 0.0,
        targetHeight: Double = 0.0
    ): ByteArray? = suspendCancellableCoroutine { continuation ->
        val localIdentifier = phUri.substringAfter("ph://").removePrefix("/")
        val fetchResult = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(localIdentifier), null)
        var asset = fetchResult.firstObject() as? PHAsset

        if (asset == null) {
            val options = PHFetchOptions()
            val allAssets = PHAsset.fetchAssetsWithOptions(options)
            val total = allAssets.count.toInt()
            for (i in 0 until total) {
                val candidate = allAssets.objectAtIndex(i.toULong()) as? PHAsset
                if (candidate != null && candidate.localIdentifier == localIdentifier) {
                    asset = candidate
                    break
                }
            }
        }

        if (asset == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val targetSize = if (targetWidth > 0 && targetHeight > 0) {
            CGSizeMake(targetWidth, targetHeight)
        } else {
            CGSizeMake(4000.0, 4000.0)
        }

        val contentMode = if (targetWidth > 0 && targetHeight > 0) {
            PHImageContentModeAspectFill
        } else {
            PHImageContentModeAspectFit
        }

        val options = PHImageRequestOptions().apply {
            deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat
            networkAccessAllowed = true
            synchronous = false
        }

        PHImageManager.defaultManager().requestImageForAsset(
            asset = asset,
            targetSize = targetSize,
            contentMode = contentMode,
            options = options,
            resultHandler = { uiImage, _ ->
                if (continuation.isActive) {
                    if (uiImage != null) {
                        val jpegData = UIImageJPEGRepresentation(uiImage, 0.85)
                        continuation.resume(jpegData?.toByteArray())
                    } else {
                        continuation.resume(null)
                    }
                }
            }
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun NSData.toByteArray(): ByteArray {
        val size = length.toInt()
        if (size <= 0) return ByteArray(0)
        val bytes = ByteArray(size)
        bytes.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), this.bytes, length)
        }
        return bytes
    }
}
