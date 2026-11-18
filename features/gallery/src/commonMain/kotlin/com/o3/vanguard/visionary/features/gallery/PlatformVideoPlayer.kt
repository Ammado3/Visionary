package com.o3.vanguard.visionary.features.gallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformVideoPlayer(uri: String, modifier: Modifier)
