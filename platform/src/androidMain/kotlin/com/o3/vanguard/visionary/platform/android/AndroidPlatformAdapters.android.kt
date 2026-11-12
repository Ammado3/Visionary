package com.o3.vanguard.visionary.platform.android

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import com.o3.vanguard.visionary.platform.AppDirectories
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.o3.vanguard.visionary.core.common.AppError
import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.domain.model.*
import com.o3.vanguard.visionary.domain.port.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

class AndroidPermissionGateway(private val context: Context) : PermissionGateway {
    private val access = MutableStateFlow(currentAccess())
    override fun observeAccess(): Flow<MediaAccess> = access
    override suspend fun requestAccess(): MediaAccess {
        val value = currentAccess()
        access.value = value
        return value
    }

    private fun currentAccess(): MediaAccess {
        val permissions = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val granted = permissions.count { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
        return if (granted == permissions.size) {
            MediaAccess.Full
        } else if (granted > 0) {
            MediaAccess.Partial(granted)
        } else MediaAccess.Denied
    }
}

class AndroidMediaStoreSource(private val context: Context) : MediaSourceEngine {
    override suspend fun queryDelta(sinceCheckpoint: Long): AppResult<List<MediaItem>> = try {
        val result = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
        )
        val mediaUri = MediaStore.Files.getContentUri("external")
        context.contentResolver.query(
            mediaUri,
            projection,
            "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?",
            arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            ),
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC",
        )?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val taken = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_TAKEN)
            val added = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val width = cursor.getColumnIndex(MediaStore.Files.FileColumns.WIDTH)
            val height = cursor.getColumnIndex(MediaStore.Files.FileColumns.HEIGHT)
            val name = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mediaType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            while (cursor.moveToNext()) {
                val mediaId = cursor.getLong(id)
                val isVideo = cursor.getInt(mediaType) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val uri = Uri.withAppendedPath(mediaUri, mediaId.toString())
                val fileName = cursor.getString(name).orEmpty()
                val isScreenshot = fileName.contains("screenshot", ignoreCase = true)
                result += MediaItem(
                    id = MediaId(mediaId),
                    uri = uri.toString(),
                    origin = MediaOrigin.LIBRARY,
                    category = when {
                        isVideo -> MediaCategory.VIDEO
                        isScreenshot -> MediaCategory.SCREENSHOT
                        else -> MediaCategory.PHOTO
                    },
                    takenAtMs = cursor.getLong(taken).takeIf { it > 0 } ?: cursor.getLong(added) * 1000,
                    width = if (width >= 0) cursor.getInt(width) else 0,
                    height = if (height >= 0) cursor.getInt(height) else 0,
                    textPreview = null,
                )
            }
        }
        AppResult.Ok(result)
    } catch (error: SecurityException) {
        AppResult.Err(com.o3.vanguard.visionary.core.common.AppError.PermissionDenied)
    } catch (error: Throwable) {
        AppResult.Err(com.o3.vanguard.visionary.core.common.AppError.Io(error.message ?: "MediaStore query failed"))
    }

    override suspend fun queryAllLibraryIds(): AppResult<Set<Long>> =
        queryDelta(0L).let { result ->
            when (result) {
                is AppResult.Ok -> AppResult.Ok(result.value.map { it.id.value }.toSet())
                is AppResult.Err -> result
            }
        }
}

class AndroidStorageAlbumSource(private val context: Context) : StorageAlbumSourceEngine {
    override suspend fun loadAlbums(): AppResult<List<StorageAlbum>> = try {
        data class AlbumAccumulator(
            val id: String,
            val name: String,
            var count: Int = 0,
            var videos: Int = 0,
            var latestAdded: Long = Long.MIN_VALUE,
            var coverUri: String? = null,
        )

        val albums = linkedMapOf<String, AlbumAccumulator>()
        val filesUri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            "bucket_id",
            "bucket_display_name",
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED,
        )

        context.contentResolver.query(
            filesUri,
            projection,
            "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?",
            arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            ),
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val bucketIdIndex = cursor.getColumnIndex("bucket_id")
            val bucketNameIndex = cursor.getColumnIndex("bucket_display_name")
            val mediaTypeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val addedIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)

            while (cursor.moveToNext()) {
                val bucketId = if (bucketIdIndex >= 0) cursor.getString(bucketIdIndex).orEmpty() else "all"
                val name = if (bucketNameIndex >= 0) cursor.getString(bucketNameIndex).orEmpty() else "Other"
                val albumId = bucketId.ifBlank { name.ifBlank { "other" } }
                val isVideo = cursor.getInt(mediaTypeIndex) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val added = cursor.getLong(addedIndex)
                val album = albums.getOrPut(albumId) {
                    AlbumAccumulator(albumId, name.ifBlank { "Other" })
                }
                album.count++
                if (isVideo) album.videos++
                if (added > album.latestAdded) {
                    album.latestAdded = added
                    album.coverUri = Uri.withAppendedPath(filesUri, cursor.getLong(idIndex).toString()).toString()
                }
            }
        }

        AppResult.Ok(albums.values.map { album ->
            StorageAlbum(
                id = album.id,
                name = album.name,
                itemCount = album.count,
                videoCount = album.videos,
                coverUri = album.coverUri,
            )
        })
    } catch (error: SecurityException) {
        AppResult.Err(com.o3.vanguard.visionary.core.common.AppError.PermissionDenied)
    } catch (error: Throwable) {
        AppResult.Err(com.o3.vanguard.visionary.core.common.AppError.Io(error.message ?: "Album query failed"))
    }
}

class AndroidMediaDeleteGateway(private val context: Context) : MediaDeleteGateway {
    companion object {
        var currentLauncher: ((IntentSender) -> Unit)? = null
        var pendingDeleteContinuation: ((DeleteConsentResult) -> Unit)? = null

        fun handleActivityResult(resultCode: Int) {
            if (resultCode == Activity.RESULT_OK) {
                pendingDeleteContinuation?.invoke(DeleteConsentResult.Deleted)
            } else {
                pendingDeleteContinuation?.invoke(DeleteConsentResult.DeclinedByUser)
            }
            pendingDeleteContinuation = null
        }
    }

    override suspend fun requestDelete(uris: List<String>): DeleteConsentResult =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { pendingDeleteContinuation = null }

            val mediaUris = mutableListOf<Uri>()
            var ok = true

            for (s in uris) {
                android.util.Log.d("DeleteGateway", "ref=$s")

                val resolved = resolveMediaStoreUri(s)
                if (resolved != null) {
                    android.util.Log.d("DeleteGateway", "resolved=$resolved")
                    mediaUris.add(resolved)
                    continue
                }

                // MediaStore URI resolve nahi hui: purana fallback
                val uri = Uri.parse(s)
                when {
                    uri.scheme == "file" || uri.scheme == null -> {
                        val f = File(uri.path ?: s)
                        if (f.exists() && !f.delete()) ok = false
                    }
                    else -> try {
                        if (context.contentResolver.delete(uri, null, null) <= 0) ok = false
                    } catch (e: Exception) {
                        ok = false
                    }
                }
            }

            if (mediaUris.isEmpty()) {
                if (continuation.isActive) continuation.resume(
                    if (ok) DeleteConsentResult.Deleted
                    else DeleteConsentResult.NotPossible("Could not delete file")
                )
                return@suspendCancellableCoroutine
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestSystemDelete(mediaUris, continuation)
            } else {
                try {
                    var all = true
                    for (u in mediaUris) {
                        if (context.contentResolver.delete(u, null, null) <= 0) all = false
                    }
                    if (continuation.isActive) continuation.resume(
                        if (all && ok) DeleteConsentResult.Deleted
                        else DeleteConsentResult.NotPossible("Delete failed")
                    )
                } catch (e: SecurityException) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        e is RecoverableSecurityException
                    ) {
                        launchIntentSender(e.userAction.actionIntent.intentSender, continuation)
                    } else if (continuation.isActive) {
                        continuation.resume(
                            DeleteConsentResult.NotPossible(e.message ?: "Permission denied")
                        )
                    }
                }
            }
        }

    private fun requestSystemDelete(uris: List<Uri>, continuation: CancellableContinuation<DeleteConsentResult>) {
        val launcher = currentLauncher
        if (launcher == null) {
            continuation.resume(DeleteConsentResult.NotPossible("No delete launcher registered"))
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val pi = MediaStore.createDeleteRequest(context.contentResolver, uris)
                pendingDeleteContinuation = { result -> continuation.resume(result) }
                CoroutineScope(Dispatchers.Main).launch {
                    launcher(pi.intentSender)
                }
            } catch (e: Exception) {
                continuation.resume(DeleteConsentResult.NotPossible(e.message ?: "Delete request failed"))
            }
        } else {
            continuation.resume(DeleteConsentResult.NotPossible("Delete permission required"))
        }
    }

    private fun launchIntentSender(intentSender: IntentSender, continuation: CancellableContinuation<DeleteConsentResult>) {
        val launcher = currentLauncher
        if (launcher == null) {
            continuation.resume(DeleteConsentResult.NotPossible("No delete launcher registered"))
            return
        }
        try {
            pendingDeleteContinuation = { result -> continuation.resume(result) }
            CoroutineScope(Dispatchers.Main).launch {
                launcher(intentSender)
            }
        } catch (e: Exception) {
            continuation.resume(DeleteConsentResult.NotPossible(e.message ?: "Delete intent failed"))
        }
    }



    private fun resolveMediaStoreUri(raw: String): Uri? {
        val uri = Uri.parse(raw)

        if (uri.scheme == "content" && uri.authority == MediaStore.AUTHORITY) {
            val segs = uri.pathSegments // e.g. [external, file, 1000600112]
            val id = uri.lastPathSegment?.toLongOrNull()

            if (id != null && !segs.contains("picker")) {
                // Already specific collection
                if (segs.contains("images") || segs.contains("video") || segs.contains("audio")) {
                    return uri
                }
                // Generic Files URI -> media_type dekh kar convert karo
                if (segs.contains("file")) {
                    context.contentResolver.query(
                        uri,
                        arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE),
                        null, null, null
                    )?.use { c ->
                        if (c.moveToFirst()) {
                            val base = when (c.getInt(0)) {
                                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE ->
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO ->
                                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO ->
                                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                                else -> null
                            }
                            if (base != null) return ContentUris.withAppendedId(base, id)
                        }
                    }
                    return null
                }
            }
        }

        // Documents URI aur file path wale cases pehle jaise rakho
        if (uri.authority == "com.android.providers.media.documents") {
            val docId = android.provider.DocumentsContract.getDocumentId(uri)
            val parts = docId.split(":")
            val id = parts.getOrNull(1)?.toLongOrNull()
            if (id != null) {
                val base = when (parts[0]) {
                    "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    else -> return null
                }
                return ContentUris.withAppendedId(base, id)
            }
        }

        val path = when (uri.scheme) {
            "file" -> uri.path
            null -> raw
            else -> null
        } ?: return null

        for (collection in listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )) {
            context.contentResolver.query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.DATA}=?",
                arrayOf(path),
                null
            )?.use { c ->
                if (c.moveToFirst()) return ContentUris.withAppendedId(collection, c.getLong(0))
            }
        }
        return null
    }


}

class MlKitOcrEngine(private val context: Context) : OcrEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognizeText(uri: String): AppResult<OcrResult> = try {
        val result = recognizer.process(InputImage.fromFilePath(context, Uri.parse(uri))).await()
        AppResult.Ok(OcrResult(result.text.trim(), emptyList()))
    } catch (error: Throwable) {
        AppResult.Err(com.o3.vanguard.visionary.core.common.AppError.Io(error.message ?: "OCR failed"))
    }
}

class MlKitImageLabelEngine(private val context: Context) : ImageLabelEngine {
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder().setConfidenceThreshold(0.6f).build()
    )

    override suspend fun labelImage(uri: String): AppResult<List<String>> = try {
        val labels = labeler.process(InputImage.fromFilePath(context, Uri.parse(uri))).await()
        AppResult.Ok(labels.map { it.text.trim() }.filter { it.isNotBlank() }.distinct())
    } catch (error: Throwable) {
        AppResult.Err(com.o3.vanguard.visionary.core.common.AppError.Io(error.message ?: "Image labeling failed"))
    }
}

class MlKitDocumentScannerAdapter(private val context: Context, private val dirs: com.o3.vanguard.visionary.platform.AppDirectories) : ScannerPort {
    override suspend fun isAvailable(): Boolean = true
    override suspend fun scan(): AppResult<ScannedDocument> =
        AppResult.Ok(ScannedDocument(emptyList(), null))
}

class AndroidBiometricAuthenticator(private val context: Context) : BiometricAuthenticator {
    companion object {
        var currentActivityProvider: (() -> AppCompatActivity?)? = null
    }

    override suspend fun authenticate(): AppResult<Unit> = suspendCancellableCoroutine { continuation ->
        val activity = currentActivityProvider?.invoke() ?: (context as? FragmentActivity)
        if (activity == null) {
            continuation.resume(AppResult.Err(AppError.Io("No active FragmentActivity found")))
            return@suspendCancellableCoroutine
        }

        val biometricManager = BiometricManager.from(activity)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            try {
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                continuation.resume(AppResult.Err(AppError.Io("No security set up, opening settings")))
            } catch (e: Exception) {
                continuation.resume(AppResult.Err(AppError.Io(e.message ?: "Failed to open settings")))
            }
            return@suspendCancellableCoroutine
        }

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            continuation.resume(AppResult.Err(AppError.Io("Biometric not available")))
            return@suspendCancellableCoroutine
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    if (continuation.isActive) {
                        continuation.resume(AppResult.Ok(Unit))
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (continuation.isActive) {
                        continuation.resume(AppResult.Err(AppError.Io(errString.toString())))
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    if (continuation.isActive) {
                        continuation.resume(AppResult.Err(AppError.Io("Authentication failed")))
                    }
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Vault")
            .setSubtitle("Confirm your biometric credential to unlock Private Vault")
            .setNegativeButtonText("Cancel")
            .build()

        activity.runOnUiThread {
            try {
                biometricPrompt.authenticate(promptInfo)
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(AppResult.Err(AppError.Io(e.message ?: "Authentication error")))
                }
            }
        }
    }
}

class AndroidVaultCryptoEngine(private val context: Context, private val dirs: AppDirectories) : VaultCryptoEngine {
    override suspend fun unlockSession(): AppResult<Unit> = AppResult.Ok(Unit)
    override fun lockSession() {}

    override suspend fun encryptToVault(sourceUri: String, itemId: String): AppResult<Unit> = try {
        val destFile = File(dirs.vaultBlobsDirPath(), itemId)
        context.contentResolver.openInputStream(Uri.parse(sourceUri))?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: return AppResult.Err(AppError.Io("Could not open input stream for $sourceUri"))
        AppResult.Ok(Unit)
    } catch (e: Exception) {
        AppResult.Err(AppError.Io(e.message ?: "Failed to save to vault"))
    }

    override suspend fun decryptFromVault(itemId: String, thumbnail: Boolean): AppResult<ByteArray> = try {
        val file = File(dirs.vaultBlobsDirPath(), itemId)
        if (!file.exists()) return AppResult.Err(AppError.Io("Vault item not found"))
        AppResult.Ok(file.readBytes())
    } catch (e: Exception) {
        AppResult.Err(AppError.Io(e.message ?: "Failed to read vault item"))
    }

    override suspend fun deleteVaultBlobs(itemId: String): AppResult<Unit> = try {
        val file = File(dirs.vaultBlobsDirPath(), itemId)
        if (file.exists()) file.delete()
        AppResult.Ok(Unit)
    } catch (e: Exception) {
        AppResult.Err(AppError.Io(e.message ?: "Failed to delete vault blob"))
    }

    override suspend fun copyToTrash(sourceUri: String, itemId: String): AppResult<String> = try {
        val destFile = File(dirs.trashBlobsDirPath(), itemId)
        context.contentResolver.openInputStream(Uri.parse(sourceUri))?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: return AppResult.Err(AppError.Io("Could not open input stream for $sourceUri"))
        AppResult.Ok(destFile.absolutePath)
    } catch (e: Exception) {
        AppResult.Err(AppError.Io(e.message ?: "Failed to cache trash item"))
    }
}

class AndroidSecureScreenPort(private val context: Context) : SecureScreenPort {
    override fun setSecureScreen(enabled: Boolean) {}
}

class WorkManagerSyncScheduler(private val context: Context) : BackgroundSyncScheduler {
    override fun scheduleIncrementalIndexing(chargingOnly: Boolean) {}
}

class AndroidShareGateway(private val context: Context) : ShareGateway {
    override fun share(uris: List<String>) {
        val resolvedUris = uris.mapNotNull { s ->
            try {
                Uri.parse(s)
            } catch (e: Exception) {
                null
            }
        }
        if (resolvedUris.isEmpty()) return

        val intent = if (resolvedUris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, resolvedUris[0])
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(resolvedUris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        val chooser = Intent.createChooser(intent, "Share media").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


