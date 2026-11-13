package com.o3.vanguard.visionary.feature.teleport.di

import com.o3.vanguard.visionary.feature.teleport.data.UnsupportedTeleportTransport
import com.o3.vanguard.visionary.feature.teleport.domain.port.DeviceNameProvider
import com.o3.vanguard.visionary.feature.teleport.domain.port.ReceivedPhotoStore
import com.o3.vanguard.visionary.feature.teleport.domain.port.ThumbnailMaker
import com.o3.vanguard.visionary.feature.teleport.domain.transport.TeleportTransport
import org.koin.core.module.Module
import org.koin.dsl.module

actual val teleportPlatformModule: Module = module {
    factory<TeleportTransport> { UnsupportedTeleportTransport() }
    factory<ThumbnailMaker> { error("ThumbnailMaker unsupported on iOS") }
    factory<ReceivedPhotoStore> { error("ReceivedPhotoStore unsupported on iOS") }
    factory<DeviceNameProvider> { object : DeviceNameProvider { override fun deviceName() = "iOS" } }
}
