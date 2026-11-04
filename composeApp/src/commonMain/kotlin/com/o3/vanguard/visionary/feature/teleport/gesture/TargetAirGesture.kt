package com.o3.vanguard.visionary.feature.teleport.gesture

enum class TargetAirGesture {
    GRAB,      // Hand closing / fist gesture for Sender
    OPEN_HAND, // Hand opening / open palm gesture for Receiver
}

data class AirGestureDetectionState(
    val isCameraActive: Boolean = false,
    val isHandDetected: Boolean = false,
    val progress: Float = 0f,
    val statusMessage: String = "",
)
