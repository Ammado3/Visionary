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
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.io.IOException

/**
 * Feeds CameraX frames to MediaPipe Hand Landmarker (LIVE_STREAM).
 *
 * - The model is loaded lazily on the first frame, i.e. on the analyzer thread, never on the UI thread.
 * - A missing or truncated model never reaches native code (a native SIGSEGV cannot be caught in Kotlin).
 * - [onUnavailable] fires once if detection cannot run, so the UI can say so and offer the manual buttons.
 * - [onFrame] is invoked on MediaPipe's result thread, always serially.
 *
 * Requires `hand_landmarker.task` in androidMain/assets/models/.
 */
internal class HandLandmarkerAnalyzer(
    private val context: Context,
    private val onUnavailable: () -> Unit = {},
    private val onFrame: (HandFrame?) -> Unit,
) : ImageAnalysis.Analyzer, AutoCloseable {

    @Volatile private var closed = false
    @Volatile private var landmarker: HandLandmarker? = null
    private var initialized = false
    private var lastTimestampMs = 0L

    override fun analyze(image: ImageProxy) {
        try {
            if (closed) return
            if (!initialized) {
                initialized = true
                landmarker = createLandmarker()
                if (landmarker == null) onUnavailable()
            }
            val lm = landmarker ?: return
            val rotation = image.imageInfo.rotationDegrees
            val bitmap = image.toBitmap() // requires OUTPUT_IMAGE_FORMAT_RGBA_8888
            // MediaPipe requires strictly increasing timestamps.
            val ts = maxOf(SystemClock.uptimeMillis(), lastTimestampMs + 1)
            lastTimestampMs = ts
            val options = ImageProcessingOptions.builder().setRotationDegrees(rotation).build()
            lm.detectAsync(BitmapImageBuilder(bitmap).build(), options, ts)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Frame dropped: ${e.javaClass.simpleName}") // landmarker closed concurrently
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Frame dropped: ${e.javaClass.simpleName}")
        } finally {
            image.close()
        }
    }

    override fun close() {
        closed = true
        landmarker?.close()
        landmarker = null
    }

    private fun createLandmarker(): HandLandmarker? {
        val app = context.applicationContext
        if (!isModelValid(app)) {
            Log.w(TAG, "Model '$MODEL_ASSET' missing or invalid. Air gesture detection disabled.")
            return null
        }
        return try {
            HandLandmarker.createFromOptions(
                app,
                HandLandmarker.HandLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL_ASSET).build())
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setNumHands(1)
                    .setMinHandDetectionConfidence(MIN_CONFIDENCE)
                    .setMinHandPresenceConfidence(MIN_CONFIDENCE)
                    .setMinTrackingConfidence(MIN_CONFIDENCE)
                    .setResultListener { result, _ -> if (!closed) onFrame(toFrame(result)) }
                    .setErrorListener { if (!closed) onFrame(null) }
                    .build(),
            )
        } catch (e: IllegalStateException) {
            Log.e(TAG, "HandLandmarker init failed", e)
            null
        } catch (e: RuntimeException) {
            Log.e(TAG, "HandLandmarker init failed", e)
            null
        }
    }

    private fun toFrame(result: HandLandmarkerResult): HandFrame? {
        val world = result.worldLandmarks().firstOrNull() ?: return null
        val image = result.landmarks().firstOrNull() ?: return null
        val openness = HandOpenness.fromWorld(world.map { HandPoint(it.x(), it.y(), it.z()) }) ?: return null
        val size = HandOpenness.sizeOf(image.map { HandPoint(it.x(), it.y(), it.z()) })
        return HandFrame(openness, size)
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
        const val TAG = "HandLandmarkerAnalyzer"
        const val MODEL_ASSET = "models/hand_landmarker.task"
        const val MIN_CONFIDENCE = 0.5f
        const val MIN_MODEL_BYTES = 1_000_000
    }
}