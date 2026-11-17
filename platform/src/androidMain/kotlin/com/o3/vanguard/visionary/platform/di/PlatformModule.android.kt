package com.o3.vanguard.visionary.platform.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.o3.vanguard.visionary.domain.port.*
import com.o3.vanguard.visionary.platform.AppDirectories
import com.o3.vanguard.visionary.platform.android.*
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { AppDirectories(get()) }
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { get<AppDirectories>().settingsFilePath().toPath() },
        )
    }
    single<PermissionGateway> { AndroidPermissionGateway(get()) }
    single<MediaSourceEngine> { AndroidMediaStoreSource(get()) }
    single<StorageAlbumSourceEngine> { AndroidStorageAlbumSource(get()) }
    single<MediaDeleteGateway> { AndroidMediaDeleteGateway(get()) }
    single<OcrEngine> { MlKitOcrEngine(get()) }
    single<ImageLabelEngine> { MlKitImageLabelEngine(get()) }
    single<ScannerPort> { MlKitDocumentScannerAdapter(get(), get()) }
    single<BiometricAuthenticator> { AndroidBiometricAuthenticator(get()) }
    single<VaultCryptoEngine> { AndroidVaultCryptoEngine(get(), get()) }
    single<SecureScreenPort> { AndroidSecureScreenPort(get()) }
    single<BackgroundSyncScheduler> { WorkManagerSyncScheduler(get()) }
    single<ShareGateway> { AndroidShareGateway(get()) }
}
