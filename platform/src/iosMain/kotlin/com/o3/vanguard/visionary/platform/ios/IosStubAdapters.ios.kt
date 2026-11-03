package com.o3.vanguard.visionary.platform.ios

import com.o3.vanguard.visionary.core.common.AppError
import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.domain.model.*
import com.o3.vanguard.visionary.domain.port.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class IosStubPermissionGateway : PermissionGateway {
    private val access = MutableStateFlow<MediaAccess>(MediaAccess.Full)
    override fun observeAccess(): Flow<MediaAccess> = access
    override suspend fun requestAccess(): MediaAccess = MediaAccess.Full
}

class IosStubMediaSourceEngine : MediaSourceEngine {
    override suspend fun queryDelta(sinceCheckpoint: Long): AppResult<List<MediaItem>> = AppResult.Ok(emptyList())
    override suspend fun queryAllLibraryIds(): AppResult<Set<Long>> = AppResult.Ok(emptySet())
}

class IosStubStorageAlbumSourceEngine : StorageAlbumSourceEngine {
    override suspend fun loadAlbums(): AppResult<List<StorageAlbum>> = AppResult.Ok(emptyList())
}

class IosStubMediaDeleteGateway : MediaDeleteGateway {
    override suspend fun requestDelete(uris: List<String>): DeleteConsentResult =
        DeleteConsentResult.NotPossible("iOS media delete gateway stub")
}

class IosStubOcrEngine : OcrEngine {
    override suspend fun recognizeText(uri: String): AppResult<OcrResult> =
        AppResult.Err(AppError.Unsupported("OS_NOT_SUPPORTED_YET"))
}

class IosStubImageLabelEngine : ImageLabelEngine {
    override suspend fun labelImage(uri: String): AppResult<List<String>> =
        AppResult.Err(AppError.Unsupported("OS_NOT_SUPPORTED_YET"))
}

class IosStubScannerPort : ScannerPort {
    override suspend fun isAvailable(): Boolean = false
    override suspend fun scan(): AppResult<ScannedDocument> =
        AppResult.Err(AppError.Unsupported("OS_NOT_SUPPORTED_YET"))
}

class IosStubBiometricAuthenticator : BiometricAuthenticator {
    override suspend fun authenticate(): AppResult<Unit> =
        AppResult.Err(AppError.Unsupported("OS_NOT_SUPPORTED_YET"))
}

class IosStubVaultCryptoEngine : VaultCryptoEngine {
    override suspend fun unlockSession(): AppResult<Unit> =
        AppResult.Err(AppError.Unsupported("OS_NOT_SUPPORTED_YET"))
    override fun lockSession() {}
    override suspend fun encryptToVault(sourceUri: String, itemId: String): AppResult<Unit> =
        AppResult.Err(AppError.Unsupported("OS_NOT_SUPPORTED_YET"))
    override suspend fun decryptFromVault(itemId: String, thumbnail: Boolean): AppResult<ByteArray> =
        AppResult.Err(AppError.Unsupported("OS_NOT_SUPPORTED_YET"))
    override suspend fun deleteVaultBlobs(itemId: String): AppResult<Unit> =
        AppResult.Err(AppError.Unsupported("OS_NOT_SUPPORTED_YET"))
}

class IosStubSecureScreenPort : SecureScreenPort {
    override fun setSecureScreen(enabled: Boolean) {}
}

class IosStubBackgroundSyncScheduler : BackgroundSyncScheduler {
    override fun scheduleIncrementalIndexing(chargingOnly: Boolean) {}
}
