package com.o3.vanguard.visionary.feature.teleport.presentation.ui.portal

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

private data class ParticleSeed(
    val xRatio: Float,
    val initialYRatio: Float,
    val speed: Float,
    val size: Float,
    val phaseOffset: Float,
)

@Composable
fun PortalBackground(
    energy: () -> Float,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "PortalClock")
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "clockProgress",
    )

    val particles = remember {
        val rand = Random(1337)
        List(36) {
            ParticleSeed(
                xRatio = rand.nextFloat(),
                initialYRatio = rand.nextFloat(),
                speed = 0.2f + rand.nextFloat() * 0.3f,
                size = 1.5f + rand.nextFloat() * 2.5f,
                phaseOffset = rand.nextFloat() * 2f * PI.toFloat(),
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val cx = width / 2f
        val cy = height / 2f
        val e = energy().coerceIn(0f, 1f)
        val t = clock

        // 1. Vertical deep gradient base
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(PortalPalette.BaseDark, PortalPalette.BaseDarkGradientBottom),
            ),
        )

        // 2. Aurora blobs on slow Lissajous paths
        val angle1 = 2f * PI.toFloat() * t
        val angle2 = 2f * PI.toFloat() * t * (14f / 22f)
        val blob1X = cx + width * 0.22f * sin(angle1)
        val blob1Y = cy + height * 0.18f * cos(angle2)

        val angle3 = 2f * PI.toFloat() * t * (18f / 14f)
        val angle4 = 2f * PI.toFloat() * t
        val blob2X = cx - width * 0.22f * sin(angle3)
        val blob2Y = cy - height * 0.18f * cos(angle4)

        val blobRadius = max(width, height) * 0.45f
        val blobAlpha = 0.18f + 0.15f * e

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PortalPalette.EmeraldAccent.copy(alpha = blobAlpha), Color.Transparent),
                center = Offset(blob1X, blob1Y),
                radius = blobRadius,
            ),
            radius = blobRadius,
            center = Offset(blob1X, blob1Y),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PortalPalette.EmeraldGlow.copy(alpha = blobAlpha * 0.8f), Color.Transparent),
                center = Offset(blob2X, blob2Y),
                radius = blobRadius * 0.9f,
            ),
            radius = blobRadius * 0.9f,
            center = Offset(blob2X, blob2Y),
        )

        // 3. Concentric rings expanding from centre
        val maxRingRadius = max(width, height) * 0.65f
        val ringCount = 4
        val ringSpeedFactor = 1f + 0.6f * e

        for (i in 0 until ringCount) {
            val offset = (t * ringSpeedFactor + i.toFloat() / ringCount) % 1f
            val ringRadius = maxRingRadius * offset
            val ringAlpha = (1f - offset) * (0.10f + 0.20f * e)
            val strokeWidth = (2f + 3f * e) * (1f - offset * 0.5f)

            if (ringRadius > 0f) {
                drawCircle(
                    color = PortalPalette.EmeraldGlow.copy(alpha = ringAlpha),
                    radius = ringRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = strokeWidth),
                )
            }
        }

        // 4. Upward drifting particles with twinkle
        for (p in particles) {
            val px = p.xRatio * width
            val rawY = p.initialYRatio - t * p.speed * (1f + 0.5f * e)
            val py = ((rawY % 1f + 1f) % 1f) * height
            val twinkle = 0.25f + 0.75f * abs(sin(angle1 * 3f + p.phaseOffset))
            val pAlpha = (twinkle * (0.6f + 0.4f * e)).coerceIn(0f, 1f)

            drawCircle(
                color = PortalPalette.EmeraldGlow.copy(alpha = pAlpha),
                radius = p.size * (1f + 0.3f * e),
                center = Offset(px, py),
            )
        }
    }
}
