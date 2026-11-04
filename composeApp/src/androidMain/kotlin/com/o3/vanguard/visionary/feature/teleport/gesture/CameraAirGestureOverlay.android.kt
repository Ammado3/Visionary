package com.o3.vanguard.visionary.feature.teleport.gesture

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import java.util.concurrent.Executors

@Composable
actual fun CameraAirGestureOverlay(
    targetGesture: TargetAirGesture,
    enabled: Boolean,
    onGestureProgress: (Float) -> Unit,
    onGestureCommitted: () -> Unit,
    modifier: Modifier,
) {
    if (!enabled) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPermission = it
    }
    LaunchedEffect(hasCameraPermission) { if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA) }

    var isExpanded by remember { mutableStateOf(true) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var output by remember { mutableStateOf(AirGestureEngine.Output(AirGestureEngine.Phase.NoHand, 0f)) }

    val onProgress by rememberUpdatedState(onGestureProgress)
    val onCommitted by rememberUpdatedState(onGestureCommitted)

    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val engine = remember(targetGesture) { AirGestureEngine(targetGesture) }
    val analyzer = remember(engine) {
        // Engine is only touched from MediaPipe's serial result thread; UI state is posted to main.
        HandLandmarkerAnalyzer(
            context = context,
            onUnavailable = {
                mainHandler.post {
                    Toast.makeText(
                        context,
                        "Air gesture detection is unavailable: missing model file",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
            onFrame = { frame ->
                val out = engine.onFrame(frame, android.os.SystemClock.uptimeMillis())
                mainHandler.post {
                    output = out
                    onProgress(out.progress)
                    if (out.committed) onCommitted()
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
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    // Camera binding is independent from the preview: collapsing the panel keeps detection running.
    DisposableEffect(hasCameraPermission, previewView, analyzer) {
        if (!hasCameraPermission) return@DisposableEffect onDispose { }
        val future = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val provider = future.get()
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { it.setAnalyzer(executor, analyzer) }
            val useCases = buildList {
                previewView?.let { pv -> add(Preview.Builder().build().also { it.surfaceProvider = pv.surfaceProvider }) }
                add(analysis)
            }
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, *useCases.toTypedArray())
            }
        }
        future.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose { runCatching { future.get().unbindAll() } }
    }

    val handDetected = output.phase != AirGestureEngine.Phase.NoHand
    val statusText = statusFor(output, targetGesture)

    Box(modifier = modifier) {
        if (!hasCameraPermission) return@Box
        if (isExpanded) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).width(185.dp).clip(RoundedCornerShape(16.dp)),
                color = VisionaryColors.colorFF000000.copy(alpha = 0.85f),
                shadowElevation = 8.dp,
            ) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Videocam, "Camera Air Gesture", Modifier.size(16.dp), VisionaryColors.color007847)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Air Gesture",
                                style = MaterialTheme.typography.labelSmall,
                                color = VisionaryColors.colorFFFFFFFF,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        IconButton({ isExpanded = false }, Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, "Minimize Camera", Modifier.size(14.dp), VisionaryColors.colorFFFFFFFF)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                                    .also { previewView = it }
                            },
                            onRelease = { previewView = null },
                            modifier = Modifier.fillMaxSize(),
                        )
                        LinearProgressIndicator(
                            progress = { output.progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp).align(Alignment.BottomCenter),
                            color = VisionaryColors.color007847,
                            trackColor = Color.White.copy(alpha = 0.3f),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (handDetected) VisionaryColors.color007847 else VisionaryColors.colorFFFFFFFF,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                    )
                }
            }
        } else {
            FloatingActionButton(
                onClick = { isExpanded = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(40.dp),
                containerColor = VisionaryColors.color007847,
                contentColor = VisionaryColors.colorFFFFFFFF,
            ) {
                Icon(Icons.Default.PanTool, "Expand Air Gesture Camera", Modifier.size(20.dp))
            }
        }
    }
}

private fun statusFor(out: AirGestureEngine.Output, target: TargetAirGesture): String {
    val grab = target == TargetAirGesture.GRAB
    val pct = (out.progress * 100).toInt()
    return when (out.phase) {
        AirGestureEngine.Phase.NoHand -> if (grab) "Show your open hand 🖐️" else "Show your closed fist ✊"
        AirGestureEngine.Phase.TooFar -> "Move hand closer"
        AirGestureEngine.Phase.TooClose -> "Move hand back slightly"
        AirGestureEngine.Phase.Arming -> if (grab) "Open your hand and hold 🖐️" else "Make a fist and hold ✊"
        AirGestureEngine.Phase.Ready -> if (grab) "Now close your fist ✊" else "Now open your hand 🖐️"
        AirGestureEngine.Phase.Active -> if (grab) "Grabbing… $pct%" else "Dropping… $pct%"
        AirGestureEngine.Phase.Committed -> "Done ✓"
    }
}