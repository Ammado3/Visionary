package com.o3.vanguard.visionary.core.testkit.fake

import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.VaultItem
import com.o3.vanguard.visionary.domain.port.BiometricAuthenticator
import com.o3.vanguard.visionary.domain.port.SecureScreenPort
import com.o3.vanguard.visionary.domain.port.VaultCryptoEngine
import com.o3.vanguard.visionary.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeVaultRepository(initialUnlocked: Boolean = false) : VaultRepository {
    private val _isUnlocked = MutableStateFlow(initialUnlocked)
    override val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    val items = MutableStateFlow<List<VaultItem>>(emptyList())
    var unlockCalls = 0
    var lockCalls = 0
    val moveToVaultCalls = mutableListOf<MediaId>()
    val restoreCalls = mutableListOf<String>()
    val deleteCalls = mutableListOf<String>()

    var nextUnlockResult: AppResult<Unit> = AppResult.Ok(Unit)
    var nextMoveToVaultResult: AppResult<Unit> = AppResult.Ok(Unit)
    var nextRestoreResult: AppResult<Unit> = AppResult.Ok(Unit)
    var nextDeleteResult: AppResult<Unit> = AppResult.Ok(Unit)

    override fun observeItems(): Flow<List<VaultItem>> = items

    override suspend fun unlock(): AppResult<Unit> {
        unlockCalls++
        val res = nextUnlockResult
        if (res is AppResult.Ok) {
            _isUnlocked.value = true
        }
        return res
    }

    override fun lock() {
        lockCalls++
        _isUnlocked.value = false
    }

    override suspend fun moveToVault(id: MediaId): AppResult<Unit> {
        moveToVaultCalls.add(id)
        return nextMoveToVaultResult
    }

    override suspend fun restore(itemId: String): AppResult<Unit> {
        restoreCalls.add(itemId)
        return nextRestoreResult
    }

    override suspend fun delete(itemId: String): AppResult<Unit> {
        deleteCalls.add(itemId)
        return nextDeleteResult
    }
}

class FakeBiometricAuthenticator : BiometricAuthenticator {
    var authenticateCalls = 0
    var nextResult: AppResult<Unit> = AppResult.Ok(Unit)

    override suspend fun authenticate(): AppResult<Unit> {
        authenticateCalls++
        return nextResult
    }
}

class FakeVaultCryptoEngine : VaultCryptoEngine {
    var unlockSessionCalls = 0
    var lockSessionCalls = 0
    val encryptCalls = mutableListOf<Pair<String, String>>()
    val decryptCalls = mutableListOf<Pair<String, Boolean>>()
    val deleteBlobsCalls = mutableListOf<String>()

    var nextUnlockSessionResult: AppResult<Unit> = AppResult.Ok(Unit)
    var nextEncryptResult: AppResult<Unit> = AppResult.Ok(Unit)
    var nextDecryptResult: AppResult<ByteArray> = AppResult.Ok(ByteArray(0))
    var nextDeleteBlobsResult: AppResult<Unit> = AppResult.Ok(Unit)

    override suspend fun unlockSession(): AppResult<Unit> {
        unlockSessionCalls++
        return nextUnlockSessionResult
    }

    override fun lockSession() {
        lockSessionCalls++
    }

    override suspend fun encryptToVault(sourceUri: String, itemId: String): AppResult<Unit> {
        encryptCalls.add(sourceUri to itemId)
        return nextEncryptResult
    }

    override suspend fun decryptFromVault(itemId: String, thumbnail: Boolean): AppResult<ByteArray> {
        decryptCalls.add(itemId to thumbnail)
        return nextDecryptResult
    }

    override suspend fun deleteVaultBlobs(itemId: String): AppResult<Unit> {
        deleteBlobsCalls.add(itemId)
        return nextDeleteBlobsResult
    }
}

class FakeSecureScreenPort : SecureScreenPort {
    val setSecureCalls = mutableListOf<Boolean>()

    override fun setSecureScreen(enabled: Boolean) {
        setSecureCalls.add(enabled)
    }
}
