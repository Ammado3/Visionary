package com.o3.vanguard.visionary.platform.di

import com.o3.vanguard.visionary.domain.port.*
import com.o3.vanguard.visionary.platform.AppDirectories
import com.o3.vanguard.visionary.platform.android.*
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { AppDirectories(get()) }
    single<PermissionGateway> { AndroidPermissionGateway(get()) }
    single<MediaSourceEngine> { AndroidMediaStoreSource(get()) }
    single<MediaDeleteGateway> { AndroidMediaDeleteGateway(get()) }
    single<OcrEngine> { MlKitOcrEngine(get()) }
    single<ImageLabelEngine> { MlKitImageLabelEngine(get()) }
    single<ScannerPort> { MlKitDocumentScannerAdapter(get(), get()) }
    single<BiometricAuthenticator> { AndroidBiometricAuthenticator(get()) }
    single<VaultCryptoEngine> { AndroidVaultCryptoEngine(get()) }
    single<SecureScreenPort> { AndroidSecureScreenPort(get()) }
    single<BackgroundSyncScheduler> { WorkManagerSyncScheduler(get()) }
}
