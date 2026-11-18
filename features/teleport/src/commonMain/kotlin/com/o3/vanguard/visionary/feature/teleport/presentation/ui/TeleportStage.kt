package com.o3.vanguard.visionary.feature.teleport.presentation.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.o3.vanguard.visionary.feature.teleport.gesture.SpreadMode
import kotlin.math.cos
import kotlin.math.sin

private object GlowingBlueOrbPalette {
    val CoreWhiteCyan = Color(0xFFE0FBFF)
    val BrightCyan = Color(0xFF00E5FF)
    val ElectricBlue = Color(0xFF0077FF)
    val DeepBlueAura = Color(0xFF0033CC)
}

@Composable
fun TeleportStage(
    imageModel: Any?,
    progress: () -> Float,
    mode: SpreadMode,
    modifier: Modifier = Modifier,
    transferProgress: Float? = null,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    val rotationDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Restart,
        ),
    )

    val p = progress()

    // Calculate Photo & Orb alpha/scale based on mode and progress
    val photoScale: Float
    val photoAlpha: Float
    val orbAlpha: Float
    val baseOrbRadiusDp: Float

    when (mode) {
        SpreadMode.Pinch -> {
            // Grab / Pinch Gesture (Sender): photo shrinks & fades into glowing blue ball
            photoScale = (1f - 0.82f * p).coerceIn(0.18f, 1f)
            photoAlpha = (1f - p).coerceIn(0f, 1f)
            orbAlpha = p.coerceIn(0f, 1f)
            baseOrbRadiusDp = 24f + 46f * p
        }
        SpreadMode.Spread -> {
            // Drop / Spread Gesture (Receiver): glowing blue ball expands & fades out as photo reveals
            if (imageModel == null) {
                photoScale = 0.18f
                photoAlpha = 0f
                orbAlpha = 1f
                baseOrbRadiusDp = 60f
            } else {
                photoScale = (0.18f + 0.82f * p).coerceIn(0.18f, 1f)
                photoAlpha = p.coerceIn(0f, 1f)
                orbAlpha = (1f - p).coerceIn(0f, 1f)
                baseOrbRadiusDp = 60f + 160f * p
            }
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val centerOffset = center
            val densityVal = density

            // Draw Glowing Blue Ball / Energy Orb
            if (orbAlpha > 0.01f) {
                val orbRadiusPx = baseOrbRadiusDp * densityVal * pulseScale

                // 1. Deep Blue Outer Aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlowingBlueOrbPalette.ElectricBlue.copy(alpha = 0.5f * orbAlpha),
                            GlowingBlueOrbPalette.DeepBlueAura.copy(alpha = 0.25f * orbAlpha),
                            Color.Transparent,
                        ),
                        center = centerOffset,
                        radius = orbRadiusPx * 2.8f,
                    ),
                    radius = orbRadiusPx * 2.8f,
                    center = centerOffset,
                )

                // 2. Vibrant Cyan Energy Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlowingBlueOrbPalette.BrightCyan.copy(alpha = 0.95f * orbAlpha),
                            GlowingBlueOrbPalette.ElectricBlue.copy(alpha = 0.65f * orbAlpha),
                            Color.Transparent,
                        ),
                        center = centerOffset,
                        radius = orbRadiusPx * 1.5f,
                    ),
                    radius = orbRadiusPx * 1.5f,
                    center = centerOffset,
                )

                // 3. Concentrated White-Cyan Core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlowingBlueOrbPalette.CoreWhiteCyan.copy(alpha = 1f * orbAlpha),
                            GlowingBlueOrbPalette.BrightCyan.copy(alpha = 0.9f * orbAlpha),
                            GlowingBlueOrbPalette.ElectricBlue.copy(alpha = 0.75f * orbAlpha),
                        ),
                        center = centerOffset,
                        radius = orbRadiusPx * 0.75f,
                    ),
                    radius = orbRadiusPx * 0.75f,
                    center = centerOffset,
                )

                // 4. Rotating Orbital Ring
                val ringRadius = orbRadiusPx * 1.6f
                val strokeWidthPx = 3f * densityVal
                drawCircle(
                    color = GlowingBlueOrbPalette.BrightCyan.copy(alpha = 0.7f * orbAlpha),
                    center = centerOffset,
                    radius = ringRadius,
                    style = Stroke(width = strokeWidthPx),
                )

                // 5. Orbiting Energy Sparks
                val angleRad = kotlin.math.PI * rotationDegrees / 180.0
                val sparkX1 = centerOffset.x + ringRadius * cos(angleRad).toFloat()
                val sparkY1 = centerOffset.y + ringRadius * sin(angleRad).toFloat()
                drawCircle(
                    color = GlowingBlueOrbPalette.CoreWhiteCyan.copy(alpha = orbAlpha),
                    center = Offset(sparkX1, sparkY1),
                    radius = 5f * densityVal,
                )

                val sparkX2 = centerOffset.x - ringRadius * cos(angleRad).toFloat()
                val sparkY2 = centerOffset.y - ringRadius * sin(angleRad).toFloat()
                drawCircle(
                    color = GlowingBlueOrbPalette.BrightCyan.copy(alpha = orbAlpha),
                    center = Offset(sparkX2, sparkY2),
                    radius = 4f * densityVal,
                )
            }

            // Draw glowing progress arc during data transfer
            transferProgress?.let { tp ->
                val arcSize = size.minDimension * 0.72f
                val strokeWidth = 8f
                val topLeft = Offset((size.width - arcSize) / 2f, (size.height - arcSize) / 2f)

                drawArc(
                    color = GlowingBlueOrbPalette.ElectricBlue.copy(alpha = 0.35f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth),
                )
                drawArc(
                    color = GlowingBlueOrbPalette.BrightCyan,
                    startAngle = -90f,
                    sweepAngle = 360f * tp.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth + 3f),
                )
            }
        }

        // Photo Image Layer
        if (imageModel != null && photoAlpha > 0.01f) {
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize(0.8f)
                    .graphicsLayer {
                        alpha = photoAlpha
                        scaleX = photoScale
                        scaleY = photoScale
                        shape = RoundedCornerShape(24.dp)
                        clip = true
                    }
                    .clip(RoundedCornerShape(24.dp)),
            )
        }
    }
}
