package com.o3.vanguard.visionary.platform.di

import com.o3.vanguard.visionary.domain.port.*
import com.o3.vanguard.visionary.platform.AppDirectories
import com.o3.vanguard.visionary.platform.android.*
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule: Module = module {
    singleOf(::AppDirectories)
    singleOf(::AndroidPermissionGateway) bind PermissionGateway::class
    singleOf(::AndroidMediaStoreSource) bind MediaSourceEngine::class
    singleOf(::AndroidStorageAlbumSource) bind StorageAlbumSourceEngine::class
    singleOf(::AndroidMediaDeleteGateway) bind MediaDeleteGateway::class
    singleOf(::MlKitOcrEngine) bind OcrEngine::class
    singleOf(::MlKitImageLabelEngine) bind ImageLabelEngine::class
    singleOf(::AndroidBiometricAuthenticator) bind BiometricAuthenticator::class
    singleOf(::AndroidVaultCryptoEngine) bind VaultCryptoEngine::class
    singleOf(::AndroidSecureScreenPort) bind SecureScreenPort::class
    singleOf(::AndroidIndexScheduler) bind BackgroundSyncScheduler::class
    singleOf(::AndroidShareGateway) bind ShareGateway::class
    single<SpeechRecognizerPort> { AndroidSpeechRecognizer(get(), get()) }
}
