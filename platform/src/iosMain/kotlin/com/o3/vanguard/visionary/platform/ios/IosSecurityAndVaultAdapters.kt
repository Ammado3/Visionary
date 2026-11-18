package com.o3.vanguard.visionary.platform.ios

import com.o3.vanguard.visionary.core.common.AppError
import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.domain.port.BiometricAuthenticator
import com.o3.vanguard.visionary.domain.port.VaultCryptoEngine
import com.o3.vanguard.visionary.platform.AppDirectories
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume

class IosBiometricAuthenticator : BiometricAuthenticator {

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun authenticate(): AppResult<Unit> = suspendCancellableCoroutine { continuation ->
        val context = LAContext()
        val reason = "Confirm your biometric credential to unlock Private Vault"

        val canEvaluate = context.canEvaluatePolicy(
            LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            error = null
        ) || context.canEvaluatePolicy(
            LAPolicyDeviceOwnerAuthentication,
            error = null
        )

        if (!canEvaluate) {
            continuation.resume(AppResult.Err(AppError.Io("Biometric or Passcode authentication not available on this device")))
            return@suspendCancellableCoroutine
        }

        context.evaluatePolicy(
            LAPolicyDeviceOwnerAuthentication,
            localizedReason = reason
        ) { success, evalError ->
            if (continuation.isActive) {
                if (success) {
                    continuation.resume(AppResult.Ok(Unit))
                } else {
                    val msg = evalError?.localizedDescription ?: "Biometric authentication failed"
                    continuation.resume(AppResult.Err(AppError.Io(msg)))
                }
            }
        }
    }
}

class IosVaultCryptoEngine(private val dirs: AppDirectories) : VaultCryptoEngine {

    override suspend fun unlockSession(): AppResult<Unit> = AppResult.Ok(Unit)

    override fun lockSession() {}

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun encryptToVault(sourceUri: String, itemId: String): AppResult<Unit> = try {
        val nsData = loadDataFromUri(sourceUri)
            ?: return AppResult.Err(AppError.Io("Could not open input for $sourceUri"))
        val destPath = "${dirs.vaultBlobsDirPath()}/$itemId"
        val success = nsData.writeToFile(destPath, atomically = true)
        if (success) AppResult.Ok(Unit)
        else AppResult.Err(AppError.Io("Failed to save to vault: $destPath"))
    } catch (e: Throwable) {
        AppResult.Err(AppError.Io(e.message ?: "Vault encryption failed"))
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun decryptFromVault(itemId: String, thumbnail: Boolean): AppResult<ByteArray> = try {
        val path = "${dirs.vaultBlobsDirPath()}/$itemId"
        val data = NSFileManager.defaultManager.contentsAtPath(path)
            ?: return AppResult.Err(AppError.Io("Vault item not found at $path"))
        AppResult.Ok(data.toByteArray())
    } catch (e: Throwable) {
        AppResult.Err(AppError.Io(e.message ?: "Vault decryption failed"))
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun deleteVaultBlobs(itemId: String): AppResult<Unit> = try {
        val path = "${dirs.vaultBlobsDirPath()}/$itemId"
        val fm = NSFileManager.defaultManager
        if (fm.fileExistsAtPath(path)) {
            fm.removeItemAtPath(path, null)
        }
        AppResult.Ok(Unit)
    } catch (e: Throwable) {
        AppResult.Err(AppError.Io(e.message ?: "Failed to delete vault blob"))
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun copyToTrash(sourceUri: String, itemId: String): AppResult<String> = try {
        val nsData = loadDataFromUri(sourceUri)
            ?: return AppResult.Err(AppError.Io("Could not open input for $sourceUri"))
        val destPath = "${dirs.trashBlobsDirPath()}/$itemId"
        val success = nsData.writeToFile(destPath, atomically = true)
        if (success) AppResult.Ok(destPath)
        else AppResult.Err(AppError.Io("Failed to copy to trash: $destPath"))
    } catch (e: Throwable) {
        AppResult.Err(AppError.Io(e.message ?: "Failed to copy to trash"))
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private suspend fun loadDataFromUri(uri: String): NSData? {
        if (uri.startsWith("ph://")) {
            val bytes = PhAssetResolver.fetchImageData(uri) ?: return null
            if (bytes.isEmpty()) return null
            return bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }
        }
        val path = uri.removePrefix("file://")
        return NSFileManager.defaultManager.contentsAtPath(path)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun NSData.toByteArray(): ByteArray {
        val size = length.toInt()
        if (size <= 0) return ByteArray(0)
        val bytes = ByteArray(size)
        bytes.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), this.bytes, length)
        }
        return bytes
    }
}
