package com.o3.vanguard.visionary.feature.teleport.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.o3.vanguard.visionary.feature.teleport.domain.port.ThumbnailMaker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

class AndroidThumbnailMaker(private val context: Context) : ThumbnailMaker {

    override suspend fun make(photoUri: String): String = withContext(Dispatchers.IO) {
        val bitmap = loadResizedBitmap(context, photoUri, MAX_SIDE)
        val dir = File(context.cacheDir, "teleport").apply { mkdirs() }
        val file = File(dir, "thumb_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        bitmap.recycle()
        file.absolutePath
    }

    companion object {
        const val MAX_SIDE = 320
        const val JPEG_QUALITY = 70

        fun calculateTargetSize(width: Int, height: Int, maxSide: Int = MAX_SIDE): Pair<Int, Int> {
            val maxDimension = max(width, height)
            if (maxDimension <= maxSide) return Pair(width, height)
            val scale = maxSide.toFloat() / maxDimension
            return Pair((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
        }

        fun calculateSampleSize(origWidth: Int, origHeight: Int, reqSide: Int = MAX_SIDE): Int {
            var sampleSize = 1
            if (origWidth > reqSide || origHeight > reqSide) {
                val halfWidth = origWidth / 2
                val halfHeight = origHeight / 2
                while ((halfWidth / sampleSize) >= reqSide && (halfHeight / sampleSize) >= reqSide) {
                    sampleSize *= 2
                }
            }
            return sampleSize
        }
    }

    private fun loadResizedBitmap(context: Context, photoUriStr: String, maxSide: Int): Bitmap {
        val uri = Uri.parse(photoUriStr)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = if (photoUriStr.startsWith("content://")) {
                ImageDecoder.createSource(context.contentResolver, uri)
            } else {
                ImageDecoder.createSource(File(photoUriStr))
            }
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val (targetW, targetH) = calculateTargetSize(info.size.width, info.size.height, maxSide)
                decoder.setTargetSize(targetW, targetH)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            if (photoUriStr.startsWith("content://")) {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            } else {
                BitmapFactory.decodeFile(photoUriStr, options)
            }
            val sample = calculateSampleSize(options.outWidth, options.outHeight, maxSide)
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = if (photoUriStr.startsWith("content://")) {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
            } else {
                BitmapFactory.decodeFile(photoUriStr, decodeOptions)
            } ?: error("Failed to decode $photoUriStr")
            val (targetW, targetH) = calculateTargetSize(decoded.width, decoded.height, maxSide)
            return Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                if (it != decoded) decoded.recycle()
            }
        }
    }
}
