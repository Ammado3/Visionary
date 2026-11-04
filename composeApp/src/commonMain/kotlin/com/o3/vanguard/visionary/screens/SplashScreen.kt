package com.o3.vanguard.visionary.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import visionary.composeapp.generated.resources.Res
import visionary.composeapp.generated.resources.ic_ai_search

internal const val SPLASH_DURATION_MS = 3_000L

private val SplashIconVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "SplashIcon",
        defaultWidth = 122.dp,
        defaultHeight = 122.dp,
        viewportWidth = 122f,
        viewportHeight = 122f
    ).apply {
        val green = SolidColor(VisionaryColors.color007847)
        val bgGreen = SolidColor(VisionaryColors.colorF4FDF7)

        addPath(
            pathData = addPathNodes("M12,11L100,11A11,11 0,0 1,111 22L111,110A11,11 0,0 1,100 121L12,121A11,11 0,0 1,1 110L1,22A11,11 0,0 1,12 11z"),
            stroke = green,
            strokeLineWidth = 2f
        )
        addPath(
            pathData = addPathNodes("M46.67,45L56,63.67L67.67,52L79.33,87H32.67L46.67,45V45"),
            stroke = green,
            strokeLineWidth = 3.15f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        group(clipPathData = addPathNodes("M30,32h16v16h-16z")) {
            addPath(
                pathData = addPathNodes("M35.33,40C35.33,41.47 36.53,42.67 38,42.67C39.47,42.67 40.67,41.47 40.67,40C40.67,38.53 39.47,37.33 38,37.33C36.53,37.33 35.33,38.53 35.33,40V40"),
                stroke = green,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes("M38,33.33V34.67"),
                stroke = green,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes("M38,45.33V46.67"),
                stroke = green,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes("M33.29,35.29L34.23,36.23"),
                stroke = green,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes("M41.77,43.77L42.71,44.71"),
                stroke = green,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes("M31.33,40H32.67"),
                stroke = green,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes("M43.33,40H44.67"),
                stroke = green,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes("M34.23,43.77L33.29,44.71"),
                stroke = green,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes("M42.71,35.29L41.77,36.23"),
                stroke = green,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
        addPath(
            pathData = addPathNodes("M104,0L104,0A18,18 0,0 1,122 18L122,18A18,18 0,0 1,104 36L104,36A18,18 0,0 1,86 18L86,18A18,18 0,0 1,104 0z"),
            fill = bgGreen
        )
        group(clipPathData = addPathNodes("M94,8h20v20h-20z")) {
            addPath(
                pathData = addPathNodes("M103.18,10.35C103.25,9.95 103.6,9.66 104,9.66C104.4,9.66 104.75,9.95 104.82,10.35L105.69,14.98C105.82,15.65 106.35,16.18 107.02,16.31L111.65,17.18C112.05,17.25 112.33,17.6 112.33,18C112.33,18.4 112.05,18.75 111.65,18.82L107.02,19.69C106.35,19.82 105.82,20.35 105.69,21.02L104.82,25.66C104.75,26.05 104.4,26.34 104,26.34C103.6,26.34 103.25,26.05 103.18,25.66L102.31,21.02C102.18,20.35 101.65,19.82 100.98,19.69L96.35,18.82C95.95,18.75 95.66,18.4 95.66,18C95.66,17.6 95.95,17.25 96.35,17.18L100.98,16.31C101.65,16.18 102.18,15.65 102.31,14.98L103.18,10.35"),
                stroke = green,
                strokeLineWidth = 1.33333f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes("M110.67,9.67V13"),
                stroke = green,
                strokeLineWidth = 1.33333f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes("M112.33,11.33H109"),
                stroke = green,
                strokeLineWidth = 1.33333f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes("M95.67,24.67C95.67,25.59 96.41,26.33 97.33,26.33C98.25,26.33 99,25.59 99,24.67C99,23.75 98.25,23 97.33,23C96.41,23 95.67,23.75 95.67,24.67V24.67"),
                stroke = green,
                strokeLineWidth = 1.33333f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
    }.build()
}

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {

    // Automatically finish splash after 3 seconds
    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        onSplashFinished()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(VisionaryColors.colorF4FDF7)
            .padding(bottom = 30.dp)
    ) {

        // Main Splash Content
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // App Icon
            Image(
                painter = rememberVectorPainter(SplashIconVector),
                contentDescription = "AI Gallery Logo",
                modifier = Modifier.size(112.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App Title
            Text(
                text = "AI Gallery",
                color = VisionaryColors.color0B1B14,
                fontSize = 24.sp,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // App Description
            Text(
                text = "Every photo, within reach.",
                color = VisionaryColors.color496156,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }

        // Loading Animation
        Box(
            modifier = Modifier.fillMaxWidth().systemBarsPadding().padding(bottom = 30.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            LoadingDots(
                dotCount = 3, dotSize = 8.dp, activeColor = VisionaryColors.color007847
            )
        }
    }
}

@Composable
fun LoadingDots(
    modifier: Modifier = Modifier,
    dotCount: Int = 3,
    dotSize: Dp = 7.dp,
    activeColor: Color = VisionaryColors.color007847,
    bounceHeight: Dp = 6.dp,
    dotAnimationDuration: Int = 400,
    delayBetweenDots: Int = 120,
    pauseAfterCycle: Int = 600
) {
    val offsets = remember {
        List(dotCount) { Animatable(0.dp, Dp.VectorConverter) }
    }
    val alphas = remember {
        List(dotCount) { Animatable(0.4f) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            for (i in 0 until dotCount) {
                launch {
                    offsets[i].animateTo(
                        targetValue = -bounceHeight,
                        animationSpec = tween(dotAnimationDuration / 2, easing = EaseInOutCubic)
                    )
                    offsets[i].animateTo(
                        targetValue = 0.dp,
                        animationSpec = tween(dotAnimationDuration / 2, easing = EaseInOutCubic)
                    )
                }

                launch {
                    alphas[i].animateTo(
                        targetValue = 1f,
                        animationSpec = tween(dotAnimationDuration / 2, easing = EaseInOutCubic)
                    )
                    alphas[i].animateTo(
                        targetValue = 0.4f,
                        animationSpec = tween(dotAnimationDuration / 2, easing = EaseInOutCubic)
                    )
                }
                delay(delayBetweenDots.toLong())
            }
            delay(pauseAfterCycle.toLong())
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Res.drawable.ic_ai_search
        for (i in 0 until dotCount) {
            Box(
                modifier = Modifier.size(dotSize).offset(y = offsets[i].value)
                    .alpha(alphas[i].value).background(activeColor, shape = CircleShape)
            )
        }
    }
}
