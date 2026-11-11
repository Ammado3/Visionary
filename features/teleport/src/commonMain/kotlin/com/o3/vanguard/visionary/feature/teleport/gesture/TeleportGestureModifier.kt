package com.o3.vanguard.visionary.feature.teleport.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

const val DEFAULT_MIN_POINTERS = 3

@Composable
fun Modifier.teleportGesture(
    mode: SpreadMode,
    enabled: Boolean,
    onStart: () -> Unit,
    onProgress: (Float) -> Unit,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
    minPointers: Int = DEFAULT_MIN_POINTERS,
    commitThreshold: Float = 0.8f,
): Modifier {
    val start by rememberUpdatedState(onStart)
    val progress by rememberUpdatedState(onProgress)
    val commit by rememberUpdatedState(onCommit)
    val cancel by rememberUpdatedState(onCancel)
    if (!enabled) return this

    return pointerInput(mode, minPointers, commitThreshold) {
        val tracker = SpreadTracker(
            mode = mode,
            minPinchStartPx = 80.dp.toPx(),
            spreadTargetPx = 140.dp.toPx(),
        )
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var active = false
            var committed = false
            do {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.size >= minPointers) {
                    val points = pressed.map { it.position }
                    if (!active) {
                        active = tracker.begin(points)
                        if (active) start()
                    } else if (!committed) {
                        val p = tracker.progress(points)
                        progress(p)
                        if (p >= commitThreshold) {
                            committed = true
                            commit()
                        }
                    }
                    if (active) event.changes.forEach { it.consume() }
                }
            } while (event.changes.any { it.pressed })
            if (active && !committed) cancel()
        }
    }
}
