package com.o3.vanguard.visionary.platform.di

import com.o3.vanguard.visionary.domain.port.BackgroundSyncScheduler
import com.o3.vanguard.visionary.domain.port.BiometricAuthenticator
import com.o3.vanguard.visionary.domain.port.ImageLabelEngine
import com.o3.vanguard.visionary.domain.port.MediaDeleteGateway
import com.o3.vanguard.visionary.domain.port.MediaSourceEngine
import com.o3.vanguard.visionary.domain.port.OcrEngine
import com.o3.vanguard.visionary.domain.port.PermissionGateway
import com.o3.vanguard.visionary.domain.port.SecureScreenPort
import com.o3.vanguard.visionary.domain.port.ShareGateway
import com.o3.vanguard.visionary.domain.port.StorageAlbumSourceEngine
import com.o3.vanguard.visionary.domain.port.VaultCryptoEngine
import com.o3.vanguard.visionary.platform.AppDirectories
import com.o3.vanguard.visionary.platform.ios.IosBiometricAuthenticator
import com.o3.vanguard.visionary.platform.ios.IosImageLabelEngine
import com.o3.vanguard.visionary.platform.ios.IosMediaDeleteGateway
import com.o3.vanguard.visionary.platform.ios.IosMediaSourceEngine
import com.o3.vanguard.visionary.platform.ios.IosOcrEngine
import com.o3.vanguard.visionary.platform.ios.IosPermissionGateway
import com.o3.vanguard.visionary.platform.ios.IosShareGateway
import com.o3.vanguard.visionary.platform.ios.IosStorageAlbumSourceEngine
import com.o3.vanguard.visionary.platform.ios.IosStubBackgroundSyncScheduler
import com.o3.vanguard.visionary.platform.ios.IosStubSecureScreenPort
import com.o3.vanguard.visionary.platform.ios.IosVaultCryptoEngine
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule: Module = module {
    singleOf(::AppDirectories)
    singleOf(::IosPermissionGateway) bind PermissionGateway::class
    singleOf(::IosMediaSourceEngine) bind MediaSourceEngine::class
    singleOf(::IosStorageAlbumSourceEngine) bind StorageAlbumSourceEngine::class
    singleOf(::IosMediaDeleteGateway) bind MediaDeleteGateway::class
    singleOf(::IosOcrEngine) bind OcrEngine::class
    singleOf(::IosImageLabelEngine) bind ImageLabelEngine::class
    singleOf(::IosBiometricAuthenticator) bind BiometricAuthenticator::class
    singleOf(::IosVaultCryptoEngine) bind VaultCryptoEngine::class
    singleOf(::IosStubSecureScreenPort) bind SecureScreenPort::class
    singleOf(::IosStubBackgroundSyncScheduler) bind BackgroundSyncScheduler::class
    singleOf(::IosShareGateway) bind ShareGateway::class
}
