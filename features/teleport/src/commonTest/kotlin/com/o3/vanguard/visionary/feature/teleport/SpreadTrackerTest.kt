package com.o3.vanguard.visionary.feature.teleport

import androidx.compose.ui.geometry.Offset
import com.o3.vanguard.visionary.feature.teleport.gesture.SpreadMode
import com.o3.vanguard.visionary.feature.teleport.gesture.SpreadTracker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpreadTrackerTest {

    @Test
    fun spreadOf_emptyPoints_returnsZero() {
        assertEquals(0f, SpreadTracker.spreadOf(emptyList()))
    }

    @Test
    fun spreadOf_equilateralTriangle_computesCorrectSpread() {
        val points = listOf(
            Offset(0f, -100f),
            Offset(86.6f, 50f),
            Offset(-86.6f, 50f),
        )
        val spread = SpreadTracker.spreadOf(points)
        assertTrue(spread in 99f..101f)
    }

    @Test
    fun pinch_validStart_progressIncreasesAsRadiusDecreases() {
        val tracker = SpreadTracker(
            mode = SpreadMode.Pinch,
            minPinchStartPx = 80f,
            spreadTargetPx = 140f,
            pinchEndRatio = 0.35f,
        )
        val initialPoints = listOf(Offset(0f, -200f), Offset(173.2f, 100f), Offset(-173.2f, 100f))
        assertTrue(tracker.begin(initialPoints))

        val progress0 = tracker.progress(initialPoints)
        assertEquals(0f, progress0)

        val halfPoints = listOf(Offset(0f, -100f), Offset(86.6f, 50f), Offset(-86.6f, 50f))
        val progressHalf = tracker.progress(halfPoints)
        assertTrue(progressHalf > 0.5f)

        val closePoints = listOf(Offset(0f, -20f), Offset(17.3f, 10f), Offset(-17.3f, 10f))
        val progressFull = tracker.progress(closePoints)
        assertEquals(1f, progressFull)
    }

    @Test
    fun pinch_smallStart_beginReturnsFalse() {
        val tracker = SpreadTracker(
            mode = SpreadMode.Pinch,
            minPinchStartPx = 80f,
            spreadTargetPx = 140f,
        )
        val smallPoints = listOf(Offset(0f, -20f), Offset(17.3f, 10f), Offset(-17.3f, 10f))
        assertFalse(tracker.begin(smallPoints))
    }

    @Test
    fun spread_progressIncreasesMonotonically() {
        val tracker = SpreadTracker(
            mode = SpreadMode.Spread,
            minPinchStartPx = 80f,
            spreadTargetPx = 200f,
        )
        val startPoints = listOf(Offset(0f, -40f), Offset(34.6f, 20f), Offset(-34.6f, 20f))
        assertTrue(tracker.begin(startPoints))

        val midPoints = listOf(Offset(0f, -120f), Offset(103.9f, 60f), Offset(-103.9f, 60f))
        val midProgress = tracker.progress(midPoints)
        assertTrue(midProgress in 0.4f..0.6f)

        val widePoints = listOf(Offset(0f, -250f), Offset(216.5f, 125f), Offset(-216.5f, 125f))
        val fullProgress = tracker.progress(widePoints)
        assertEquals(1f, fullProgress)
    }
}
