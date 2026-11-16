package com.o3.vanguard.visionary.feature.teleport.gesture

import androidx.compose.ui.geometry.Offset

enum class SpreadMode { Pinch, Spread }

/**
 * Progress 0..1 of a multi-finger pinch/spread. "Spread" of N points = mean distance to their centroid.
 *  Pinch:  fingers must start at least [minPinchStartPx] apart; progress hits 1 at [pinchEndRatio] of the start spread.
 *  Spread: progress hits 1 when spread reaches [spreadTargetPx].
 */
class SpreadTracker(
    private val mode: SpreadMode,
    private val minPinchStartPx: Float,
    private val spreadTargetPx: Float,
    private val pinchEndRatio: Float = 0.35f,
) {
    private var initialSpread = 0f

    /** Returns false when the gesture start is not valid (e.g. fingers too close for a pinch). */
    fun begin(points: List<Offset>): Boolean {
        initialSpread = spreadOf(points)
        return mode == SpreadMode.Spread || initialSpread >= minPinchStartPx
    }

    fun progress(points: List<Offset>): Float {
        if (initialSpread <= 0f) return 0f
        val current = spreadOf(points)
        val raw = when (mode) {
            SpreadMode.Pinch -> (1f - current / initialSpread) / (1f - pinchEndRatio)
            SpreadMode.Spread -> (current - initialSpread) / (spreadTargetPx - initialSpread).coerceAtLeast(1f)
        }
        return raw.coerceIn(0f, 1f)
    }

    companion object {
        fun spreadOf(points: List<Offset>): Float {
            if (points.isEmpty()) return 0f
            val centroid = Offset(
                x = points.sumOf { it.x.toDouble() }.toFloat() / points.size,
                y = points.sumOf { it.y.toDouble() }.toFloat() / points.size,
            )
            return points.map { (it - centroid).getDistance() }.average().toFloat()
        }
    }
}
