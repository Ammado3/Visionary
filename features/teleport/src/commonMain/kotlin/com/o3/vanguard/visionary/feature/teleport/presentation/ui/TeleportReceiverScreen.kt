package com.o3.vanguard.visionary.feature.teleport.presentation.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import com.o3.vanguard.visionary.feature.teleport.domain.model.*
import com.o3.vanguard.visionary.feature.teleport.gesture.AirGestureTracker
import com.o3.vanguard.visionary.feature.teleport.gesture.SpreadMode
import com.o3.vanguard.visionary.feature.teleport.gesture.TargetAirGesture
import com.o3.vanguard.visionary.feature.teleport.gesture.teleportGesture
import com.o3.vanguard.visionary.feature.teleport.presentation.TeleportReceiverViewModel
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

    LaunchedEffect(Unit) { viewModel.start() }
    LaunchedEffect(state) {
        if (state is ReceiverState.Committed) {
            delay(1_500)
            onClose()
        }
        if (state is ReceiverState.Cancelled) {
            onClose()
        }
    }

    val incomingState = state as? ReceiverState.Incoming
    val canDrop = incomingState != null && !incomingState.userDropped

    Surface(modifier = modifier.fillMaxSize(), color = VisionaryColors.colorF4FDF7) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .teleportGesture(
                    mode = SpreadMode.Spread,
                    enabled = canDrop,
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
        ) {
            val imageModel = when (val s = state) {
                is ReceiverState.Incoming -> s.thumbnailPath?.let { "file://$it" }
                is ReceiverState.Committed -> s.savedUri
                else -> null
            }

            TeleportStage(
                imageModel = imageModel,
                progress = { animatableGesture.value },
                mode = SpreadMode.Spread,
                modifier = Modifier.fillMaxSize(),
            )

            AirGestureTracker(
                targetGesture = TargetAirGesture.OPEN_HAND,
                enabled = canDrop,
                tracking = canDrop,
                onHint = { },
                onGestureProgress = { value -> scope.launch { animatableGesture.snapTo(value) } },
                onGestureCommitted = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        animatableGesture.animateTo(1f, animationSpec = tween(250))
                    }
                    viewModel.onDropCommitted()
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Air Teleport Receiver",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = VisionaryColors.color0B1B14,
                )
                TextButton(onClick = { viewModel.cancel() }) {
                    Text("Close", color = VisionaryColors.color007847)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (val currentState = state) {
                    ReceiverState.Idle, ReceiverState.Waiting -> {
                        Text(
                            text = "Waiting for sender nearby…",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = VisionaryColors.color0B1B14,
                        )
                        Text(
                            text = "Open Air Teleport on the sending phone to begin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VisionaryColors.color496156,
                            textAlign = TextAlign.Center,
                        )
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(0.6f),
                            color = VisionaryColors.color007847,
                        )
                    }

                    is ReceiverState.Connected -> {
                        Text(
                            text = "Connected to ${currentState.peer.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = VisionaryColors.color0B1B14,
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = VisionaryColors.colorCEDED3,
                        ) {
                            Text(
                                text = "Pairing Code: ${currentState.pairingCode}",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    is ReceiverState.Incoming -> {
                        if (currentState.userDropped) {
                            Text(
                                text = "Finishing transfer…",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = VisionaryColors.color007847,
                            )
                            CircularProgressIndicator(color = VisionaryColors.color007847)
                        } else if (currentState.senderReleased) {
                            Text(
                                text = "Photo in the air!",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = VisionaryColors.color007847,
                            )
                            Text(
                                text = "Open your hand in front of the camera or spread three fingers to catch the photo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VisionaryColors.color496156,
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            Text(
                                text = "Receiving ${currentState.fileName}… ${(currentState.transferProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = VisionaryColors.color0B1B14,
                            )
                            LinearProgressIndicator(
                                progress = { currentState.transferProgress },
                                modifier = Modifier.fillMaxWidth(0.8f),
                                color = VisionaryColors.color007847,
                            )
                        }
                    }

                    is ReceiverState.Committed -> {
                        Text(
                            text = "Saved to your gallery ✓",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = VisionaryColors.color007847,
                        )
                        Button(
                            onClick = onClose,
                            colors = ButtonDefaults.buttonColors(containerColor = VisionaryColors.color007847),
                        ) {
                            Text("Done")
                        }
                    }

                    is ReceiverState.Failed -> {
                        Text(
                            text = currentState.error.toMessage(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Button(
                            onClick = onClose,
                            colors = ButtonDefaults.buttonColors(containerColor = VisionaryColors.color007847),
                        ) {
                            Text("Close")
                        }
                    }

                    ReceiverState.Cancelled -> Unit
                }
            }
        }
    }
}
