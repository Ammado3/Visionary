package com.o3.vanguard.visionary.feature.teleport.gesture

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class HandPoint(val x: Float, val y: Float, val z: Float)

/**
 * One analysed camera frame.
 * [openness]: 0 = closed fist, 1 = fully open palm.
 * [handSize]: longest side of the hand's bounding box as a fraction of the frame (0..1).
 */
data class HandFrame(val openness: Float, val handSize: Float)

/**
 * Openness from the 21 MediaPipe hand landmarks.
 *
 * For each of the four fingers we take |wrist -> tip| / |wrist -> MCP|. The ratio is independent of
 * hand distance, camera resolution, hand rotation and handedness. Open finger is about 1.9, curled finger is about 1.0.
 * Use world landmarks (meters, camera-aspect independent) for [fromWorld].
 */
object HandOpenness {
    private const val LANDMARK_COUNT = 21
    private const val WRIST = 0
    private const val MIN_BASE = 1e-4f
    const val CLOSED_RATIO = 1.15f
    const val OPEN_RATIO = 1.85f

    /** (MCP, TIP) landmark indices for index, middle, ring and pinky. */
    private val fingers = listOf(5 to 8, 9 to 12, 13 to 16, 17 to 20)

    fun fromWorld(points: List<HandPoint>): Float? {
        if (points.size < LANDMARK_COUNT) return null
        val wrist = points[WRIST]
        var sum = 0f
        for ((mcp, tip) in fingers) {
            val base = distance(wrist, points[mcp])
            if (base < MIN_BASE) return null
            sum += distance(wrist, points[tip]) / base
        }
        val mean = sum / fingers.size
        return ((mean - CLOSED_RATIO) / (OPEN_RATIO - CLOSED_RATIO)).coerceIn(0f, 1f)
    }

    /** [points] are the normalized (0..1) image landmarks. */
    fun sizeOf(points: List<HandPoint>): Float {
        if (points.isEmpty()) return 0f
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (p in points) {
            minX = min(minX, p.x)
            maxX = max(maxX, p.x)
            minY = min(minY, p.y)
            maxY = max(maxY, p.y)
        }
        return max(maxX - minX, maxY - minY)
    }

    private fun distance(a: HandPoint, b: HandPoint): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
