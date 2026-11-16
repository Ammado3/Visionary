package com.o3.vanguard.visionary.feature.teleport.presentation.ui.portal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CheckmarkAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    onFinished: () -> Unit = {},
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        )
        onFinished()
    }

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = w * 0.08f

        drawCircle(
            color = PortalPalette.EmeraldGlow.copy(alpha = 0.2f * progress.value),
            radius = w * 0.48f,
        )

        val checkPath = Path().apply {
            moveTo(w * 0.25f, h * 0.52f)
            lineTo(w * 0.44f, h * 0.70f)
            lineTo(w * 0.75f, h * 0.35f)
        }

        val pathMeasure = PathMeasure().apply {
            setPath(checkPath, false)
        }
        val length = pathMeasure.length
        val destinationPath = Path()
        pathMeasure.getSegment(0f, length * progress.value, destinationPath, true)

        drawPath(
            path = destinationPath,
            color = PortalPalette.EmeraldGlow,
            style = Stroke(width = strokeWidth),
        )
    }
}
