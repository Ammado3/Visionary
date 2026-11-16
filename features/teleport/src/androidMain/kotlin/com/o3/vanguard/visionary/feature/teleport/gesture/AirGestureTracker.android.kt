package com.o3.vanguard.visionary.feature.teleport.gesture

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

@Composable
actual fun AirGestureTracker(
    targetGesture: TargetAirGesture,
    enabled: Boolean,
    tracking: Boolean,
    onHint: (AirHint) -> Unit,
    onGestureProgress: (Float) -> Unit,
    onGestureCommitted: () -> Unit,
    onUnavailable: () -> Unit,
) {
    if (!enabled) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentOnHint by rememberUpdatedState(onHint)
    val currentOnProgress by rememberUpdatedState(onGestureProgress)
    val currentOnCommitted by rememberUpdatedState(onGestureCommitted)
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            currentOnUnavailable()
        }
    }
    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val engine = remember(targetGesture) { AirGestureEngine(targetGesture) }

    var wasTracking by remember { mutableStateOf(false) }
    LaunchedEffect(tracking) {
        if (tracking && !wasTracking) {
            engine.reset()
        }
        wasTracking = tracking
    }

    val trackingState by rememberUpdatedState(tracking)

    val analyzer = remember(engine) {
        GestureRecognizerAnalyzer(
            context = context,
            onUnavailable = {
                mainHandler.post {
                    currentOnUnavailable()
                }
            },
            onFrame = { gestureFrame ->
                if (!trackingState) return@GestureRecognizerAnalyzer

                val handFrame = gestureFrame?.let { gf ->
                    val points = gf.landmarks.map { HandPoint(it.x(), it.y(), it.z()) }
                    val openness = HandOpenness.fromWorld(points) ?: 0.5f
                    val size = HandOpenness.sizeOf(points)
                    HandFrame(openness, size)
                }
                val out = engine.onFrame(handFrame, SystemClock.uptimeMillis())
                mainHandler.post {
                    currentOnHint(out.phase.toAirHint())
                    currentOnProgress(out.progress)
                    if (out.committed) {
                        currentOnCommitted()
                    }
                }
            },
        )
    }

    DisposableEffect(analyzer) {
        onDispose {
            analyzer.close()
            mainHandler.removeCallbacksAndMessages(null)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
        }
    }

    DisposableEffect(hasCameraPermission, analyzer) {
        if (!hasCameraPermission) return@DisposableEffect onDispose { }
        val future = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            runCatching {
                val provider = future.get()
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also { it.setAnalyzer(executor, analyzer) }

                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, analysis)
            }.onFailure {
                currentOnUnavailable()
            }
        }
        future.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { future.get().unbindAll() }
        }
    }
}

fun AirGestureEngine.Phase.toAirHint(): AirHint = when (this) {
    AirGestureEngine.Phase.NoHand -> AirHint.NoHand
    AirGestureEngine.Phase.TooFar -> AirHint.TooFar
    AirGestureEngine.Phase.TooClose -> AirHint.TooClose
    AirGestureEngine.Phase.Arming -> AirHint.Arming
    AirGestureEngine.Phase.Ready -> AirHint.Ready
    AirGestureEngine.Phase.Active -> AirHint.Active
    AirGestureEngine.Phase.Committed -> AirHint.Committed
}
