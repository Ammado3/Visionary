package com.o3.vanguard.visionary.platform.android

import android.content.Context
import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.domain.model.*
import com.o3.vanguard.visionary.domain.port.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class AndroidPermissionGateway(private val context: Context) : PermissionGateway {
    private val access = MutableStateFlow<MediaAccess>(MediaAccess.Full)
    override fun observeAccess(): Flow<MediaAccess> = access
    override suspend fun requestAccess(): MediaAccess = MediaAccess.Full
}

class AndroidMediaStoreSource(private val context: Context) : MediaSourceEngine {
    override suspend fun queryDelta(sinceCheckpoint: Long): AppResult<List<MediaItem>> = AppResult.Ok(emptyList())
    override suspend fun queryAllLibraryIds(): AppResult<Set<Long>> = AppResult.Ok(emptySet())
}

class AndroidMediaDeleteGateway(private val context: Context) : MediaDeleteGateway {
    override suspend fun requestDelete(uris: List<String>): DeleteConsentResult = DeleteConsentResult.Deleted
}

class MlKitOcrEngine(private val context: Context) : OcrEngine {
    override suspend fun recognizeText(uri: String): AppResult<OcrResult> =
        AppResult.Ok(OcrResult("", emptyList()))
}

class MlKitImageLabelEngine(private val context: Context) : ImageLabelEngine {
    override suspend fun labelImage(uri: String): AppResult<List<String>> = AppResult.Ok(emptyList())
}

class MlKitDocumentScannerAdapter(private val context: Context, private val dirs: com.o3.vanguard.visionary.platform.AppDirectories) : ScannerPort {
    override suspend fun isAvailable(): Boolean = true
    override suspend fun scan(): AppResult<ScannedDocument> =
        AppResult.Ok(ScannedDocument(emptyList(), null))
}

class AndroidBiometricAuthenticator(private val context: Context) : BiometricAuthenticator {
    override suspend fun authenticate(): AppResult<Unit> = AppResult.Ok(Unit)
}

class AndroidVaultCryptoEngine(private val context: Context) : VaultCryptoEngine {
    override suspend fun unlockSession(): AppResult<Unit> = AppResult.Ok(Unit)
    override fun lockSession() {}
    override suspend fun encryptToVault(sourceUri: String, itemId: String): AppResult<Unit> = AppResult.Ok(Unit)
    override suspend fun decryptFromVault(itemId: String, thumbnail: Boolean): AppResult<ByteArray> = AppResult.Ok(ByteArray(0))
    override suspend fun deleteVaultBlobs(itemId: String): AppResult<Unit> = AppResult.Ok(Unit)
}

class AndroidSecureScreenPort(private val context: Context) : SecureScreenPort {
    override fun setSecureScreen(enabled: Boolean) {}
}

class WorkManagerSyncScheduler(private val context: Context) : BackgroundSyncScheduler {
    override fun scheduleIncrementalIndexing(chargingOnly: Boolean) {}
}
