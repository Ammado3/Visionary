package com.o3.vanguard.visionary.platform.di

import com.o3.vanguard.visionary.domain.port.*
import com.o3.vanguard.visionary.platform.AppDirectories
import com.o3.vanguard.visionary.platform.ios.*
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { AppDirectories() }
    single<PermissionGateway> { IosStubPermissionGateway() }
    single<MediaSourceEngine> { IosStubMediaSourceEngine() }
    single<StorageAlbumSourceEngine> { IosStubStorageAlbumSourceEngine() }
    single<MediaDeleteGateway> { IosStubMediaDeleteGateway() }
    single<OcrEngine> { IosStubOcrEngine() }
    single<ImageLabelEngine> { IosStubImageLabelEngine() }
    single<ScannerPort> { IosStubScannerPort() }
    single<BiometricAuthenticator> { IosStubBiometricAuthenticator() }
    single<VaultCryptoEngine> { IosStubVaultCryptoEngine() }
    single<SecureScreenPort> { IosStubSecureScreenPort() }
    single<BackgroundSyncScheduler> { IosStubBackgroundSyncScheduler() }
    single<ShareGateway> { IosStubShareGateway() }
}

