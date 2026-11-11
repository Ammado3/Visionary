package com.o3.vanguard.visionary.feature.teleport.gesture

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.io.IOException

data class GestureFrame(
    val landmarks: List<NormalizedLandmark>,
    val isPinching: Boolean,
    val gestureName: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val rotation: Int,
)

internal class GestureRecognizerAnalyzer(
    private val context: Context,
    private val onUnavailable: () -> Unit = {},
    private val onFrame: (GestureFrame?) -> Unit,
) : ImageAnalysis.Analyzer, AutoCloseable {

    @Volatile private var closed = false
    @Volatile private var recognizer: GestureRecognizer? = null
    private var initialized = false
    private var lastTimestampMs = 0L

    private var currentImageWidth = 0
    private var currentImageHeight = 0
    private var currentRotation = 0

    override fun analyze(image: ImageProxy) {
        try {
            if (closed) return
            if (!initialized) {
                initialized = true
                recognizer = createRecognizer()
                if (recognizer == null) onUnavailable()
            }
            val rec = recognizer ?: return
            currentImageWidth = image.width
            currentImageHeight = image.height
            currentRotation = image.imageInfo.rotationDegrees

            val bitmap = image.toBitmap()
            val ts = maxOf(SystemClock.uptimeMillis(), lastTimestampMs + 1)
            lastTimestampMs = ts
            val options = ImageProcessingOptions.builder().setRotationDegrees(currentRotation).build()
            rec.recognizeAsync(BitmapImageBuilder(bitmap).build(), options, ts)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Frame dropped: ${e.javaClass.simpleName}")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Frame dropped: ${e.javaClass.simpleName}")
        } catch (e: Exception) {
            Log.w(TAG, "Frame error: ${e.message}")
        } finally {
            image.close()
        }
    }

    override fun close() {
        closed = true
        recognizer?.close()
        recognizer = null
    }

    private fun createRecognizer(): GestureRecognizer? {
        val app = context.applicationContext
        if (!isModelValid(app)) {
            Log.w(TAG, "Model '$MODEL_ASSET' missing or invalid. Gesture recognition disabled.")
            return null
        }
        return try {
            val baseOptions = BaseOptions.builder().setModelAssetPath(MODEL_ASSET).build()
            val options = GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, _ ->
                    if (!closed) {
                        onFrame(toGestureFrame(result, currentImageWidth, currentImageHeight, currentRotation))
                    }
                }
                .setErrorListener { if (!closed) onFrame(null) }
                .build()
            GestureRecognizer.createFromOptions(app, options)
        } catch (e: Exception) {
            Log.e(TAG, "GestureRecognizer init failed", e)
            null
        }
    }

    private fun toGestureFrame(result: GestureRecognizerResult, imgWidth: Int, imgHeight: Int, rot: Int): GestureFrame? {
        val handLandmarks = result.landmarks().firstOrNull() ?: return null
        if (handLandmarks.isEmpty()) return null
        val isPinching = calculateRobustPinch(handLandmarks)
        val gestureName = result.gestures().firstOrNull()?.firstOrNull()?.categoryName() ?: "Unknown"
        return GestureFrame(handLandmarks, isPinching, gestureName, imgWidth, imgHeight, rot)
    }

    private fun calculateRobustPinch(handLandmarks: List<NormalizedLandmark>): Boolean {
        if (handLandmarks.size < 21) return false
        val thumbTip = handLandmarks[4]
        val indexTip = handLandmarks[8]
        val indexBase = handLandmarks[5]
        val pinkyBase = handLandmarks[17]

        val pinchDistance = Math.sqrt(
            Math.pow((indexTip.x() - thumbTip.x()).toDouble(), 2.0) +
            Math.pow((indexTip.y() - thumbTip.y()).toDouble(), 2.0)
        )
        val palmWidth = Math.sqrt(
            Math.pow((pinkyBase.x() - indexBase.x()).toDouble(), 2.0) +
            Math.pow((pinkyBase.y() - indexBase.y()).toDouble(), 2.0)
        )
        if (palmWidth == 0.0) return false
        val pinchRatio = pinchDistance / palmWidth
        return pinchRatio < 0.20
    }

    private fun isModelValid(context: Context): Boolean = try {
        context.assets.open(MODEL_ASSET).use { stream ->
            var bytesRead = 0L
            val buffer = ByteArray(8192)
            while (bytesRead < MIN_MODEL_BYTES) {
                val read = stream.read(buffer)
                if (read <= 0) break
                bytesRead += read
            }
            bytesRead >= MIN_MODEL_BYTES
        }
    } catch (_: IOException) {
        false
    }

    private companion object {
        const val TAG = "GestureRecognizerAnalyzer"
        const val MODEL_ASSET = "models/gesture_recognizer.task"
        const val MIN_MODEL_BYTES = 1_000_000
    }
}
