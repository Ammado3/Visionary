package com.o3.vanguard.visionary.feature.teleport.presentation.ui


import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.o3.vanguard.visionary.feature.teleport.domain.model.PhotoRef
import com.o3.vanguard.visionary.feature.teleport.domain.model.SenderState
import com.o3.vanguard.visionary.feature.teleport.domain.model.TeleportError
import com.o3.vanguard.visionary.feature.teleport.gesture.AirGestureTracker
import com.o3.vanguard.visionary.feature.teleport.gesture.AirHint
import com.o3.vanguard.visionary.feature.teleport.gesture.SpreadMode
import com.o3.vanguard.visionary.feature.teleport.gesture.TargetAirGesture
import com.o3.vanguard.visionary.feature.teleport.gesture.teleportGesture
import com.o3.vanguard.visionary.feature.teleport.presentation.TeleportCopy
import com.o3.vanguard.visionary.feature.teleport.presentation.TeleportSenderViewModel
import com.o3.vanguard.visionary.feature.teleport.presentation.ui.portal.CheckmarkAnimation
import com.o3.vanguard.visionary.feature.teleport.presentation.ui.portal.PortalBackground
import com.o3.vanguard.visionary.feature.teleport.presentation.ui.portal.PortalCaption
import com.o3.vanguard.visionary.feature.teleport.presentation.ui.portal.PortalChoreography
import com.o3.vanguard.visionary.feature.teleport.presentation.ui.portal.PortalHint
import com.o3.vanguard.visionary.feature.teleport.presentation.ui.portal.PortalPalette
import com.o3.vanguard.visionary.feature.teleport.presentation.ui.portal.PortalStage
import com.o3.vanguard.visionary.feature.teleport.presentation.ui.portal.rememberDebouncedHint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

fun TeleportError.toMessage(): String = when (this) {
    TeleportError.NoReceiverFound -> "No receiver found. Open Air Teleport Receiver on the other phone."
    TeleportError.ConnectionFailed -> "Couldn't connect. Try again."
    TeleportError.Disconnected -> "Connection lost."
    TeleportError.TimedOut -> "Took too long. Try again."
    TeleportError.RemoteCancelled -> "The other phone cancelled."
    TeleportError.TransferFailed -> "The transfer failed."
    TeleportError.SaveFailed -> "Couldn't save the photo."
}

@Composable
fun TeleportSenderScreen(
    photo: PhotoRef,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeleportSenderViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val animatableGesture = remember { Animatable(0f) }

    val choreography = remember { PortalChoreography() }
    var currentStage by remember { mutableStateOf(choreography.stage) }

    var rawAirHint by remember { mutableStateOf<AirHint?>(null) }
    var isCameraUnavailable by remember { mutableStateOf(false) }

    val sessionReady = state is SenderState.Connected || state is SenderState.Released || state is SenderState.Delivered
    val sessionFailed = state is SenderState.Failed

    LaunchedEffect(photo.uri) {
        viewModel.start(photo)
    }

    LaunchedEffect(sessionReady, sessionFailed) {
        if (sessionReady) {
            choreography.onSessionReady()
            currentStage = choreography.stage
        }
        if (sessionFailed) {
            choreography.onSessionFailed()
            currentStage = choreography.stage
        }
    }

    LaunchedEffect(state) {
        if (state is SenderState.Delivered) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(1_200)
            onClose()
        }
        if (state is SenderState.Cancelled) {
            onClose()
        }
    }

    TeleportBackHandler {
        viewModel.cancel()
    }

    val photoAlphaAnim = remember { Animatable(0f) }
    val photoScaleAnim = remember { Animatable(0.96f) }

    LaunchedEffect(currentStage) {
        if (currentStage == PortalStage.Live) {
            launch { photoAlphaAnim.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
            launch { photoScaleAnim.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        }
    }

    val energyValue = when {
        state is SenderState.Released -> 1.0f
        currentStage == PortalStage.Live -> 0.5f
        currentStage == PortalStage.Holding -> 0.4f
        else -> 0.3f
    }

    val targetHintText = when {
        state !is SenderState.Connected -> null
        isCameraUnavailable -> TeleportCopy.fallbackHintFor(TargetAirGesture.GRAB)
        else -> TeleportCopy.hintFor(rawAirHint ?: AirHint.NoHand, TargetAirGesture.GRAB)
    }

    val debouncedHint = rememberDebouncedHint(targetHintText)

    Box(modifier = modifier.fillMaxSize()) {
        PortalBackground(energy = { energyValue })

        AirGestureTracker(
            targetGesture = TargetAirGesture.GRAB,
            enabled = true,
            tracking = currentStage == PortalStage.Live && state is SenderState.Connected,
            onHint = { rawAirHint = it },
            onGestureProgress = { value -> scope.launch { animatableGesture.snapTo(value) } },
            onGestureCommitted = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    animatableGesture.animateTo(1f, animationSpec = tween(250))
                }
                viewModel.onGrabCommitted()
            },
            onUnavailable = { isCameraUnavailable = true },
        )

        // Photo Stage layer
        if (currentStage == PortalStage.Live || currentStage == PortalStage.Ready) {
            val transferProgress = (state as? SenderState.Released)?.transferProgress
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(photoAlphaAnim.value)
                    .scale(photoScaleAnim.value)
                    .teleportGesture(
                        mode = SpreadMode.Pinch,
                        enabled = state is SenderState.Connected,
                        onStart = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                        onProgress = { value -> scope.launch { animatableGesture.snapTo(value) } },
                        onCommit = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                animatableGesture.animateTo(1f, animationSpec = tween(250))
                            }
                            viewModel.onGrabCommitted()
                        },
                        onCancel = {
                            scope.launch {
                                animatableGesture.animateTo(0f, animationSpec = tween(200))
                            }
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                TeleportStage(
                    imageModel = photo.uri,
                    progress = { animatableGesture.value },
                    mode = SpreadMode.Pinch,
                    modifier = Modifier.fillMaxSize(),
                    transferProgress = transferProgress,
                )
            }
        }

        // Caption / Stage Choreography Overlay
        when (currentStage) {
            PortalStage.Opening -> {
                PortalCaption(
                    text = TeleportCopy.OpeningPortal,
                    onFinished = {
                        choreography.onIntroAnimationFinished(sessionReady)
                        currentStage = choreography.stage
                    },
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )
            }
            PortalStage.Holding -> {
                // No text displayed while holding for session ready
            }
            PortalStage.Ready -> {
                PortalCaption(
                    text = TeleportCopy.ReadyToShare,
                    onFinished = {
                        choreography.onReadyAnimationFinished()
                        currentStage = choreography.stage
                    },
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )
            }
            PortalStage.Live -> {
                if (state is SenderState.Delivered) {
                    CheckmarkAnimation(
                        modifier = Modifier.align(Alignment.Center),
                        size = 80.dp,
                    )
                } else if (state is SenderState.Connected) {
                    PortalHint(
                        hint = debouncedHint,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
                    )
                }

                // Peer Picker when multiple receivers found
                val peers = (state as? SenderState.Searching)?.peers.orEmpty()
                if (peers.size > 1) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 96.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = TeleportCopy.MultipleReceiversFound,
                            style = MaterialTheme.typography.labelMedium,
                            color = PortalPalette.TextWhite,
                        )
                        LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                            items(peers) { peer ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { viewModel.connect(peer) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = PortalPalette.GlassSurface,
                                    border = BorderStroke(1.dp, PortalPalette.GlassBorder),
                                ) {
                                    Text(
                                        peer.name,
                                        modifier = Modifier.padding(12.dp),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        color = PortalPalette.TextWhite,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            PortalStage.Failed -> {
                val err = (state as? SenderState.Failed)?.error
                val msg = err?.toMessage() ?: "Connection failed"
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.W300,
                            fontSize = 20.sp,
                        ),
                        color = PortalPalette.TextWhite,
                        textAlign = TextAlign.Center,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TextButton(onClick = { viewModel.start(photo) }) {
                            Text(
                                TeleportCopy.TryAgain,
                                color = PortalPalette.EmeraldAccent,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        TextButton(onClick = onClose) {
                            Text(
                                TeleportCopy.Close,
                                color = PortalPalette.EmeraldAccent.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }

        // Top-End Small Icon-Only Close Button
        IconButton(
            onClick = { viewModel.cancel() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = TeleportCopy.Close,
                tint = PortalPalette.TextWhite.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
