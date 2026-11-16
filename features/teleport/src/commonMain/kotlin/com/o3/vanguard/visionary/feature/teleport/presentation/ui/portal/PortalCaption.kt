package com.o3.vanguard.visionary.feature.teleport.presentation.ui.portal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

suspend fun Animatable<Float, *>.playCaptionCycle() {
    // 1. Fade in
    animateTo(1f, animationSpec = tween(PortalTiming.FadeInMs, easing = FastOutSlowInEasing))
    // 2. Blink twice (dip to 0.2, rise to 1.0)
    repeat(2) {
        animateTo(0.2f, animationSpec = tween(PortalTiming.BlinkDipMs))
        animateTo(1f, animationSpec = tween(PortalTiming.BlinkRiseMs))
    }
    // 3. Hold
    delay(PortalTiming.HoldMs.toLong())
    // 4. Fade out
    animateTo(0f, animationSpec = tween(PortalTiming.FadeOutMs))
}

@Composable
fun PortalCaption(
    text: String,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(text) {
        alphaAnim.snapTo(0f)
        alphaAnim.playCaptionCycle()
        onFinished()
    }

    Box(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontWeight = FontWeight.W300,
                fontSize = 28.sp,
                letterSpacing = 2.sp,
                color = PortalPalette.TextWhite,
                shadow = Shadow(
                    color = PortalPalette.EmeraldGlow.copy(alpha = 0.8f),
                    blurRadius = 12f,
                ),
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                alpha = alphaAnim.value
            },
        )
    }
}
