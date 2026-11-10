package com.o3.vanguard.visionary.features.gallery.presentation.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors

@Composable
actual fun PlatformVideoPlayer(uri: String, modifier: Modifier) {
    Box(modifier.background(VisionaryColors.colorFF000000), contentAlignment = Alignment.Center) {
        Text("Video playback is available on iOS media integration.", color = VisionaryColors.colorFFFFFFFF)
    }
}
