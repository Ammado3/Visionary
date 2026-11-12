package com.o3.vanguard.visionary.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class AppDirectories {
    @OptIn(ExperimentalForeignApi::class)
    private fun docRoot(): String {
        val url = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
        return requireNotNull(url?.path)
    }

    actual fun vaultBlobsDirPath(): String = "${docRoot()}/vault_blobs"
    actual fun scansDirPath(): String = "${docRoot()}/scans"
    actual fun trashBlobsDirPath(): String = "${docRoot()}/trash_blobs"
    @OptIn(ExperimentalForeignApi::class)
    actual fun cacheDirPath(): String {
        val url = NSFileManager.defaultManager.URLForDirectory(
            directory = NSCachesDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
        return requireNotNull(url?.path)
    }
}
