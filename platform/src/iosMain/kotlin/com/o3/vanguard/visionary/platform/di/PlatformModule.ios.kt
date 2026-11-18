package com.o3.vanguard.visionary.platform.di

import com.o3.vanguard.visionary.domain.port.*
import com.o3.vanguard.visionary.platform.AppDirectories
import com.o3.vanguard.visionary.platform.ios.*
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { AppDirectories() }
    single<PermissionGateway> { IosPermissionGateway() }
    single<MediaSourceEngine> { IosMediaSourceEngine() }
    single<StorageAlbumSourceEngine> { IosStorageAlbumSourceEngine() }
    single<MediaDeleteGateway> { IosMediaDeleteGateway() }
    single<OcrEngine> { IosOcrEngine() }
    single<ImageLabelEngine> { IosImageLabelEngine() }
    single<BiometricAuthenticator> { IosBiometricAuthenticator() }
    single<VaultCryptoEngine> { IosVaultCryptoEngine(get()) }
    single<SecureScreenPort> { IosStubSecureScreenPort() }
    single<BackgroundSyncScheduler> { IosStubBackgroundSyncScheduler() }
    single<ShareGateway> { IosShareGateway() }
}

