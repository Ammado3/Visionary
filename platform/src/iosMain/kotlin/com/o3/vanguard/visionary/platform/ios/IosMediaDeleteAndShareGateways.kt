package com.o3.vanguard.visionary.platform.ios

import com.o3.vanguard.visionary.domain.model.DeleteConsentResult
import com.o3.vanguard.visionary.domain.port.MediaDeleteGateway
import com.o3.vanguard.visionary.domain.port.ShareGateway
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Photos.PHAsset
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIWindow
import kotlin.coroutines.resume

class IosMediaDeleteGateway : MediaDeleteGateway {

    override suspend fun requestDelete(uris: List<String>): DeleteConsentResult {
        if (uris.isEmpty()) return DeleteConsentResult.Deleted

        val localIdentifiers = uris.map { it.removePrefix("ph://") }
        val assets = PHAsset.fetchAssetsWithLocalIdentifiers(localIdentifiers, null)

        if (assets.count.toInt() == 0) {
            return DeleteConsentResult.NotPossible("Assets not found in photo library")
        }

        return suspendCancellableCoroutine { continuation ->
            PHPhotoLibrary.sharedPhotoLibrary().performChanges(
                changeBlock = {
                    PHAssetChangeRequest.deleteAssets(assets)
                },
                completionHandler = { success, error ->
                    if (continuation.isActive) {
                        if (success) {
                            continuation.resume(DeleteConsentResult.Deleted)
                        } else if (error != null) {
                            continuation.resume(DeleteConsentResult.DeclinedByUser)
                        } else {
                            continuation.resume(DeleteConsentResult.NotPossible("Deletion failed"))
                        }
                    }
                }
            )
        }
    }
}

class IosShareGateway : ShareGateway {

    @OptIn(ExperimentalForeignApi::class)
    override fun share(uris: List<String>) {
        if (uris.isEmpty()) return

        CoroutineScope(Dispatchers.Main).launch {
            val items = uris.mapNotNull { uriStr ->
                when {
                    uriStr.startsWith("ph://") -> {
                        val bytes = PhAssetResolver.fetchImageData(uriStr)
                        if (bytes != null && bytes.isNotEmpty()) {
                            val nsData = bytes.toNSData()
                            UIImage.imageWithData(nsData)
                        } else null
                    }
                    uriStr.startsWith("file://") -> {
                        NSURL.URLWithString(uriStr)
                    }
                    uriStr.startsWith("/") -> {
                        NSURL.fileURLWithPath(uriStr)
                    }
                    else -> null
                }
            }

            if (items.isEmpty()) return@launch

            val window = UIApplication.sharedApplication.keyWindow
                ?: (UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow)
            var rootVc = window?.rootViewController
            while (rootVc?.presentedViewController != null) {
                rootVc = rootVc.presentedViewController
            }

            if (rootVc != null) {
                val activityVc = UIActivityViewController(activityItems = items, applicationActivities = null)
                rootVc.presentViewController(activityVc, animated = true, completion = null)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun ByteArray.toNSData(): NSData {
        if (isEmpty()) return NSData()
        return usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
        }
    }
}
