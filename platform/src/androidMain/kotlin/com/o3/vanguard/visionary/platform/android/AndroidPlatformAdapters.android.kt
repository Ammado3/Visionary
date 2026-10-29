package com.o3.vanguard.visionary.platform.android

import android.content.Context
import android.Manifest
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
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE
        return if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            MediaAccess.Full
        } else MediaAccess.Denied
    }
}

class AndroidMediaStoreSource(private val context: Context) : MediaSourceEngine {
    override suspend fun queryDelta(sinceCheckpoint: Long): AppResult<List<MediaItem>> = try {
        val result = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DISPLAY_NAME,
        )
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val taken = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
            val added = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val width = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
            val height = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
            val name = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val mediaId = cursor.getLong(id)
                val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId.toString())
                val fileName = cursor.getString(name).orEmpty()
                val isScreenshot = fileName.contains("screenshot", ignoreCase = true)
                result += MediaItem(
                    id = MediaId(mediaId),
                    uri = uri.toString(),
                    origin = MediaOrigin.LIBRARY,
                    category = if (isScreenshot) MediaCategory.SCREENSHOT else MediaCategory.PHOTO,
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

class AndroidMediaDeleteGateway(private val context: Context) : MediaDeleteGateway {
    override suspend fun requestDelete(uris: List<String>): DeleteConsentResult = DeleteConsentResult.Deleted
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
