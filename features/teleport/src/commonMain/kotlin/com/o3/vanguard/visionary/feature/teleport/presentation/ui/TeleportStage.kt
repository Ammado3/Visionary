package com.o3.vanguard.visionary.feature.teleport.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.o3.vanguard.visionary.feature.teleport.gesture.SpreadMode
import com.o3.vanguard.visionary.feature.teleport.presentation.ui.portal.PortalPalette

@Composable
fun TeleportStage(
    imageModel: Any?,
    progress: () -> Float,
    mode: SpreadMode,
    modifier: Modifier = Modifier,
    transferProgress: Float? = null,
) {
    val accent = PortalPalette.EmeraldGlow
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val p = progress()
            val radius = size.minDimension * (0.35f + 0.5f * p)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.45f * p), Color.Transparent),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )

            // Draw glowing progress arc during transfer
            transferProgress?.let { tp ->
                val arcSize = size.minDimension * 0.72f
                val strokeWidth = 6f
                val topLeft = Offset((size.width - arcSize) / 2f, (size.height - arcSize) / 2f)

                drawArc(
                    color = PortalPalette.EmeraldGlow.copy(alpha = 0.3f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth),
                )
                drawArc(
                    color = PortalPalette.EmeraldGlow,
                    startAngle = -90f,
                    sweepAngle = 360f * tp.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth + 2f),
                )
            }
        }
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize(0.8f)
                .graphicsLayer {
                    val p = progress()
                    when (mode) {
                        SpreadMode.Pinch -> {
                            alpha = (1f - p).coerceIn(0f, 1f)
                            scaleX = 1f + 0.18f * p
                        }
                        SpreadMode.Spread -> {
                            alpha = p.coerceIn(0f, 1f)
                            scaleX = 1.18f - 0.18f * p
                        }
                    }
                    scaleY = scaleX
                },
        )
    }
}
