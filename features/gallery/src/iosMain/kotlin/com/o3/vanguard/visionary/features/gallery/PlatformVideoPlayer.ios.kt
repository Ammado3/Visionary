package com.o3.vanguard.visionary.features.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL
import platform.Photos.PHAsset
import platform.Photos.PHImageManager
import platform.Photos.PHVideoRequestOptions
import platform.Photos.PHVideoRequestOptionsDeliveryModeHighQualityFormat

@OptIn(ExperimentalForeignApi::class)
@Suppress("DEPRECATION")
@Composable
actual fun PlatformVideoPlayer(uri: String, modifier: Modifier) {
    val player = remember(uri) { AVPlayer() }

    LaunchedEffect(uri) {
        if (uri.startsWith("ph://")) {
            val localId = uri.removePrefix("ph://")
            val fetchResult = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(localId), null)
            val asset = fetchResult.firstObject() as? PHAsset
            if (asset != null) {
                val options = PHVideoRequestOptions().apply {
                    deliveryMode = PHVideoRequestOptionsDeliveryModeHighQualityFormat
                    networkAccessAllowed = true
                }
                PHImageManager.defaultManager().requestAVAssetForVideo(
                    asset = asset,
                    options = options,
                    resultHandler = { avAsset, _, _ ->
                        if (avAsset != null) {
                            val playerItem = AVPlayerItem(asset = avAsset)
                            player.replaceCurrentItemWithPlayerItem(playerItem)
                            player.play()
                        }
                    }
                )
            }
        } else {
            val nsUrl = if (uri.startsWith("file://")) NSURL.URLWithString(uri) else NSURL.fileURLWithPath(uri)
            if (nsUrl != null) {
                val playerItem = AVPlayerItem(uRL = nsUrl)
                player.replaceCurrentItemWithPlayerItem(playerItem)
                player.play()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.pause()
        }
    }

    UIKitViewController(
        factory = {
            AVPlayerViewController().apply {
                this.player = player
                this.showsPlaybackControls = true
            }
        },
        modifier = modifier
    )
}
