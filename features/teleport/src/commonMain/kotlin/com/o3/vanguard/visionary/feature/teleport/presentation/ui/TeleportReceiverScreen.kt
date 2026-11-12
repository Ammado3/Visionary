package com.o3.vanguard.visionary.feature.teleport.presentation.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.o3.vanguard.visionary.feature.teleport.domain.model.ReceiverState
import com.o3.vanguard.visionary.feature.teleport.gesture.AirGestureTracker
import com.o3.vanguard.visionary.feature.teleport.gesture.AirHint
import com.o3.vanguard.visionary.feature.teleport.gesture.SpreadMode
import com.o3.vanguard.visionary.feature.teleport.gesture.TargetAirGesture
import com.o3.vanguard.visionary.feature.teleport.gesture.teleportGesture
import com.o3.vanguard.visionary.feature.teleport.presentation.TeleportCopy
import com.o3.vanguard.visionary.feature.teleport.presentation.TeleportReceiverViewModel
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

@Composable
fun TeleportReceiverScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeleportReceiverViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val animatableGesture = remember { Animatable(0f) }

    val choreography = remember { PortalChoreography() }
    var currentStage by remember { mutableStateOf(choreography.stage) }

    var rawAirHint by remember { mutableStateOf<AirHint?>(null) }
    var isCameraUnavailable by remember { mutableStateOf(false) }

    val sessionReady = state is ReceiverState.Waiting || state is ReceiverState.Connected || state is ReceiverState.Incoming || state is ReceiverState.Committed
    val sessionFailed = state is ReceiverState.Failed

    LaunchedEffect(Unit) {
        viewModel.start()
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
        if (state is ReceiverState.Committed) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(1_500)
            onClose()
        }
        if (state is ReceiverState.Cancelled) {
            onClose()
        }
    }

    TeleportBackHandler {
        viewModel.cancel()
    }

    val photoAlphaAnim = remember { Animatable(0f) }
    val photoScaleAnim = remember { Animatable(0.96f) }

    val incomingState = state as? ReceiverState.Incoming
    val isReceiverActive = state is ReceiverState.Waiting || state is ReceiverState.Connected || (incomingState != null && !incomingState.userDropped)

    LaunchedEffect(currentStage, incomingState != null) {
        if (currentStage == PortalStage.Live && (incomingState != null || state is ReceiverState.Committed)) {
            launch { photoAlphaAnim.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
            launch { photoScaleAnim.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        }
    }

    val energyValue = when {
        incomingState != null -> 0.8f
        state is ReceiverState.Committed -> 0.9f
        currentStage == PortalStage.Live -> 0.4f
        currentStage == PortalStage.Holding -> 0.4f
        else -> 0.3f
    }

    val targetHintText = when {
        !isReceiverActive -> null
        isCameraUnavailable -> TeleportCopy.fallbackHintFor(TargetAirGesture.OPEN_HAND)
        else -> TeleportCopy.hintFor(rawAirHint ?: AirHint.NoHand, TargetAirGesture.OPEN_HAND)
    }

    val debouncedHint = rememberDebouncedHint(targetHintText)

    Box(modifier = modifier.fillMaxSize()) {
        PortalBackground(energy = { energyValue })

        AirGestureTracker(
            targetGesture = TargetAirGesture.OPEN_HAND,
            enabled = true,
            tracking = currentStage == PortalStage.Live && isReceiverActive,
            onHint = { rawAirHint = it },
            onGestureProgress = { value -> scope.launch { animatableGesture.snapTo(value) } },
            onGestureCommitted = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    animatableGesture.animateTo(1f, animationSpec = tween(250))
                }
                viewModel.onDropCommitted()
            },
            onUnavailable = { isCameraUnavailable = true },
        )

        // Photo / Portal Stage layer
        val imageModel = when (val s = state) {
            is ReceiverState.Incoming -> s.thumbnailPath?.let { if (it.startsWith("file://")) it else "file://$it" }
            is ReceiverState.Committed -> s.savedUri
            else -> null
        }

        if (currentStage == PortalStage.Live || currentStage == PortalStage.Ready) {
            val transferProgress = if (incomingState != null && !incomingState.senderReleased) incomingState.transferProgress else null
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (imageModel != null) photoAlphaAnim.value else 1f)
                    .scale(if (imageModel != null) photoScaleAnim.value else 1f)
                    .teleportGesture(
                        mode = SpreadMode.Spread,
                        enabled = isReceiverActive,
                        onStart = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                        onProgress = { value -> scope.launch { animatableGesture.snapTo(value) } },
                        onCommit = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                animatableGesture.animateTo(1f, animationSpec = tween(250))
                            }
                            viewModel.onDropCommitted()
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
                    imageModel = imageModel,
                    progress = { animatableGesture.value },
                    mode = SpreadMode.Spread,
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
                    text = TeleportCopy.ReadyToReceive,
                    onFinished = {
                        choreography.onReadyAnimationFinished()
                        currentStage = choreography.stage
                    },
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )
            }
            PortalStage.Live -> {
                if (state is ReceiverState.Committed) {
                    CheckmarkAnimation(
                        modifier = Modifier.align(Alignment.Center),
                        size = 80.dp,
                    )
                } else if (isReceiverActive) {
                    PortalHint(
                        hint = debouncedHint,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
                    )
                }
            }
            PortalStage.Failed -> {
                val err = (state as? ReceiverState.Failed)?.error
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
                        TextButton(onClick = { viewModel.start() }) {
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
