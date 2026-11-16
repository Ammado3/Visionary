package com.o3.vanguard.visionary.feature.teleport.gesture

import androidx.compose.runtime.Composable

@Composable
actual fun AirGestureTracker(
    targetGesture: TargetAirGesture,
    enabled: Boolean,
    tracking: Boolean,
    onHint: (AirHint) -> Unit,
    onGestureProgress: (Float) -> Unit,
    onGestureCommitted: () -> Unit,
    onUnavailable: () -> Unit,
) {
    // Unsupported on iOS
}
