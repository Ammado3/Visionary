package com.o3.vanguard.visionary.feature.teleport.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.o3.vanguard.visionary.feature.teleport.domain.port.ReceivedPhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

class MediaStoreReceivedPhotoStore(private val context: Context) : ReceivedPhotoStore {

    override suspend fun save(stagedPath: String, fileName: String, mimeType: String): String = withContext(Dispatchers.IO) {
        val sanitized = sanitizeFileName(fileName)
        val validMime = if (mimeType.startsWith("image/")) mimeType else "image/jpeg"
        val stagedFile = File(stagedPath)

        val savedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveScopedStorage(stagedFile, sanitized, validMime)
        } else {
            saveLegacyStorage(stagedFile, sanitized)
        }

        stagedFile.delete()
        savedUri
    }

    private fun saveScopedStorage(stagedFile: File, fileName: String, mimeType: String): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Visionary")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Failed to create MediaStore row")

        try {
            resolver.openOutputStream(uri)?.use { out ->
                stagedFile.inputStream().use { input -> input.copyTo(out) }
            } ?: error("Failed to open output stream for $uri")

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri.toString()
        } catch (e: Throwable) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun saveLegacyStorage(stagedFile: File, fileName: String): String {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Visionary").apply { mkdirs() }
        val target = ensureUniqueFile(dir, fileName)
        stagedFile.copyTo(target, overwrite = true)

        return suspendCancellableCoroutine { continuation ->
            MediaScannerConnection.scanFile(
                context,
                arrayOf(target.absolutePath),
                null,
            ) { _, uri ->
                continuation.resume(uri?.toString() ?: Uri.fromFile(target).toString())
            }
        }
    }

    companion object {
        fun sanitizeFileName(name: String): String {
            val cleaned = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
            return if (cleaned.isBlank()) "teleport_${System.currentTimeMillis()}.jpg" else cleaned
        }

        fun ensureUniqueFile(dir: File, fileName: String): File {
            var target = File(dir, fileName)
            if (!target.exists()) return target
            val dot = fileName.lastIndexOf('.')
            val base = if (dot > 0) fileName.substring(0, dot) else fileName
            val ext = if (dot > 0) fileName.substring(dot) else ""
            var count = 1
            while (target.exists()) {
                target = File(dir, "${base}_$count$ext")
                count++
            }
            return target
        }
    }
}
