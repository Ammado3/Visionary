package com.o3.vanguard.visionary.feature.teleport.gesture

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.max

class GestureOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var landmarksList: List<List<NormalizedLandmark>> = emptyList()
    private var imgWidth = 0
    private var imgHeight = 0
    private var rotationDegrees = 0

    private val linePaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val pointPaint = Paint().apply {
        color = Color.YELLOW
        strokeWidth = 10f
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    companion object {
        val LANDMARK_CONNECTIONS = listOf(
            Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4),
            Pair(0, 5), Pair(5, 6), Pair(6, 7), Pair(7, 8),
            Pair(5, 9), Pair(9, 10), Pair(10, 11), Pair(11, 12),
            Pair(9, 13), Pair(13, 14), Pair(14, 15), Pair(15, 16),
            Pair(13, 17), Pair(0, 17), Pair(17, 18), Pair(18, 19), Pair(19, 20)
        )
    }

    fun setLandmarks(
        landmarks: List<List<NormalizedLandmark>>,
        imageWidth: Int,
        imageHeight: Int,
        rotation: Int,
    ) {
        this.landmarksList = landmarks
        this.imgWidth = imageWidth
        this.imgHeight = imageHeight
        this.rotationDegrees = rotation
        invalidate()
    }

    fun transformX(nx: Float, ny: Float): Float {
        if (width == 0 || height == 0 || imgWidth == 0 || imgHeight == 0) return 0f
        val viewWidth = width.toFloat()
        val sourceWidth: Float = if (rotationDegrees == 90 || rotationDegrees == 270) imgHeight.toFloat() else imgWidth.toFloat()
        val sourceHeight: Float = if (rotationDegrees == 90 || rotationDegrees == 270) imgWidth.toFloat() else imgHeight.toFloat()

        val scale = max(viewWidth / sourceWidth, height.toFloat() / sourceHeight)
        val renderedWidth = sourceWidth * scale
        val offsetX = (viewWidth - renderedWidth) / 2f

        val rx: Float = when (rotationDegrees) {
            90 -> ny
            270 -> 1f - ny
            180 -> 1f - nx
            else -> nx
        }
        return rx * sourceWidth * scale + offsetX
    }

    fun transformY(nx: Float, ny: Float): Float {
        if (width == 0 || height == 0 || imgWidth == 0 || imgHeight == 0) return 0f
        val viewHeight = height.toFloat()
        val sourceWidth: Float = if (rotationDegrees == 90 || rotationDegrees == 270) imgHeight.toFloat() else imgWidth.toFloat()
        val sourceHeight: Float = if (rotationDegrees == 90 || rotationDegrees == 270) imgWidth.toFloat() else imgHeight.toFloat()

        val scale = max(width.toFloat() / sourceWidth, viewHeight / sourceHeight)
        val renderedHeight = sourceHeight * scale
        val offsetY = (viewHeight - renderedHeight) / 2f

        val ry: Float = when (rotationDegrees) {
            90 -> 1f - nx
            270 -> nx
            180 -> 1f - ny
            else -> ny
        }
        val yPos = ry * sourceHeight * scale + offsetY
        return viewHeight - yPos
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (landmarksList.isEmpty() || imgWidth == 0 || imgHeight == 0) return

        for (landmarks in landmarksList) {
            for (connection in LANDMARK_CONNECTIONS) {
                if (connection.first >= landmarks.size || connection.second >= landmarks.size) continue
                val start = landmarks[connection.first]
                val end = landmarks[connection.second]
                val startX = transformX(start.x(), start.y())
                val startY = transformY(start.x(), start.y())
                val endX = transformX(end.x(), end.y())
                val endY = transformY(end.x(), end.y())
                canvas.drawLine(startX, startY, endX, endY, linePaint)
            }

            for (landmark in landmarks) {
                val x = transformX(landmark.x(), landmark.y())
                val y = transformY(landmark.x(), landmark.y())
                canvas.drawCircle(x, y, 8f, pointPaint)
            }
        }
    }
}
