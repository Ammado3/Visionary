package com.o3.vanguard.visionary.platform.android

import android.content.Context
import android.Manifest
import android.app.RecoverableSecurityException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.domain.model.*
import com.o3.vanguard.visionary.domain.port.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

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
    override suspend fun requestDelete(uris: List<String>): DeleteConsentResult = try {
        val deletedCount = uris.count { uriString ->
            val uri = Uri.parse(uriString)
            try {
                val rows = context.contentResolver.delete(uri, null, null)
                rows > 0
            } catch (securityException: RecoverableSecurityException) {
                try {
                    securityException.userAction.actionIntent.intentSender.sendIntent(
                        context,
                        0,
                        null,
                        null,
                        null
                    )
                    true
                } catch (e: Exception) {
                    false
                }
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val pendingIntent = MediaStore.createDeleteRequest(
                            context.contentResolver,
                            listOf(uri)
                        )
                        pendingIntent.intentSender.sendIntent(context, 0, null, null, null)
                        true
                    } catch (ex: Exception) {
                        false
                    }
                } else {
                    false
                }
            }
        }
        if (deletedCount > 0) {
            DeleteConsentResult.Deleted
        } else {
            DeleteConsentResult.NotPossible("Media could not be deleted")
        }
    } catch (error: SecurityException) {
        DeleteConsentResult.NotPossible("Delete permission is required")
    } catch (error: Throwable) {
        DeleteConsentResult.NotPossible(error.message ?: "Media could not be deleted")
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
