package com.o3.vanguard.visionary.domain.port

import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.domain.model.*
import kotlinx.coroutines.flow.Flow

interface PermissionGateway {
    fun observeAccess(): Flow<MediaAccess>
    suspend fun requestAccess(): MediaAccess
}

interface MediaSourceEngine {
    suspend fun queryDelta(sinceCheckpoint: Long): AppResult<List<MediaItem>>
    suspend fun queryAllLibraryIds(): AppResult<Set<Long>>
}

interface MediaDeleteGateway {
    suspend fun requestDelete(uris: List<String>): DeleteConsentResult
}

interface OcrEngine {
    suspend fun recognizeText(uri: String): AppResult<OcrResult>
}

interface ImageLabelEngine {
    suspend fun labelImage(uri: String): AppResult<List<String>>
}

interface ScannerPort {
    suspend fun isAvailable(): Boolean
    suspend fun scan(): AppResult<ScannedDocument>
}

interface BiometricAuthenticator {
    suspend fun authenticate(): AppResult<Unit>
}

interface VaultCryptoEngine {
    suspend fun unlockSession(): AppResult<Unit>
    fun lockSession()
    suspend fun encryptToVault(sourceUri: String, itemId: String): AppResult<Unit>
    suspend fun decryptFromVault(itemId: String, thumbnail: Boolean): AppResult<ByteArray>
    suspend fun deleteVaultBlobs(itemId: String): AppResult<Unit>
}

interface SecureScreenPort {
    fun setSecureScreen(enabled: Boolean)
}

interface BackgroundSyncScheduler {
    fun scheduleIncrementalIndexing(chargingOnly: Boolean)
}
