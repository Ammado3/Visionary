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
import com.o3.vanguard.visionary.platform.android.AndroidBiometricAuthenticator
import com.o3.vanguard.visionary.platform.android.AndroidMediaDeleteGateway
import com.o3.vanguard.visionary.platform.android.AndroidMediaStoreSource
import com.o3.vanguard.visionary.platform.android.AndroidPermissionGateway
import com.o3.vanguard.visionary.platform.android.AndroidSecureScreenPort
import com.o3.vanguard.visionary.platform.android.AndroidShareGateway
import com.o3.vanguard.visionary.platform.android.AndroidStorageAlbumSource
import com.o3.vanguard.visionary.platform.android.AndroidVaultCryptoEngine
import com.o3.vanguard.visionary.platform.android.MlKitImageLabelEngine
import com.o3.vanguard.visionary.platform.android.MlKitOcrEngine
import com.o3.vanguard.visionary.platform.android.WorkManagerSyncScheduler
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
    singleOf(::WorkManagerSyncScheduler) bind BackgroundSyncScheduler::class
    singleOf(::AndroidShareGateway) bind ShareGateway::class
}
