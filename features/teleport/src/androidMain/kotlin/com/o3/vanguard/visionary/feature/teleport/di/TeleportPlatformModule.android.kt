package com.o3.vanguard.visionary.feature.teleport.di

import com.o3.vanguard.visionary.feature.teleport.data.AndroidDeviceNameProvider
import com.o3.vanguard.visionary.feature.teleport.data.AndroidThumbnailMaker
import com.o3.vanguard.visionary.feature.teleport.data.MediaStoreReceivedPhotoStore
import com.o3.vanguard.visionary.feature.teleport.data.NearbyTeleportTransport
import com.o3.vanguard.visionary.feature.teleport.domain.port.DeviceNameProvider
import com.o3.vanguard.visionary.feature.teleport.domain.port.ReceivedPhotoStore
import com.o3.vanguard.visionary.feature.teleport.domain.port.ThumbnailMaker
import com.o3.vanguard.visionary.feature.teleport.domain.transport.TeleportTransport
import org.koin.core.module.Module
import org.koin.dsl.module

actual val teleportPlatformModule: Module = module {
    factory<TeleportTransport> { NearbyTeleportTransport(get()) }
    factory<ThumbnailMaker> { AndroidThumbnailMaker(get()) }
    factory<ReceivedPhotoStore> { MediaStoreReceivedPhotoStore(get()) }
    factory<DeviceNameProvider> { AndroidDeviceNameProvider() }
}
