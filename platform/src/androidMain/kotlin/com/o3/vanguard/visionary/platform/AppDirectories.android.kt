package com.o3.vanguard.visionary.platform

import android.content.Context
import java.io.File

actual class AppDirectories(private val context: Context) {
    actual fun vaultBlobsDirPath(): String =
        File(context.filesDir, "vault_blobs").apply { mkdirs() }.absolutePath

    actual fun scansDirPath(): String =
        File(context.filesDir, "scans").apply { mkdirs() }.absolutePath

    actual fun cacheDirPath(): String =
        context.cacheDir.absolutePath
}
