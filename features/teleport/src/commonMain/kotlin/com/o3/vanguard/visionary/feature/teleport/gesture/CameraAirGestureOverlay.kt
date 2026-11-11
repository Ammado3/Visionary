package com.o3.vanguard.visionary.feature.teleport.gesture

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CameraAirGestureOverlay(
    targetGesture: TargetAirGesture,
    enabled: Boolean,
    onGestureProgress: (Float) -> Unit,
    onGestureCommitted: () -> Unit,
    modifier: Modifier = Modifier,
)
