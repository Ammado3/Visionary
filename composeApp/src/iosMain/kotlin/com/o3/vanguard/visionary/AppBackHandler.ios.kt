package com.o3.vanguard.visionary

import androidx.compose.runtime.Composable

@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS uses the navigation controller's back gesture.
}
