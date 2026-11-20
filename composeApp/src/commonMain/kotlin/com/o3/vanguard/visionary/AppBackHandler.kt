package com.o3.vanguard.visionary

import androidx.compose.runtime.Composable

@Composable
expect fun AppBackHandler(enabled: Boolean, onBack: () -> Unit)
