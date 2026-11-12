package com.o3.vanguard.visionary.feature.teleport.presentation.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.o3.vanguard.visionary.feature.teleport.presentation.TeleportSenderViewModel
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

    LaunchedEffect(photo.uri) { viewModel.start(photo) }
    LaunchedEffect(state) {
        if (state is SenderState.Delivered) {
            delay(1_200)
            onClose()
        }
        if (state is SenderState.Cancelled) {
            onClose()
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = VisionaryColors.colorF4FDF7) {
        Box(
            modifier = Modifier
                .fillMaxSize()
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
        ) {
            TeleportStage(
                imageModel = photo.uri,
                progress = { animatableGesture.value },
                mode = SpreadMode.Pinch,
                modifier = Modifier.fillMaxSize(),
            )

            AirGestureTracker(
                targetGesture = TargetAirGesture.GRAB,
                enabled = state is SenderState.Connected,
                tracking = state is SenderState.Connected,
                onHint = { },
                onGestureProgress = { value -> scope.launch { animatableGesture.snapTo(value) } },
                onGestureCommitted = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        animatableGesture.animateTo(1f, animationSpec = tween(250))
                    }
                    viewModel.onGrabCommitted()
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
                    text = "Air Teleport",
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
                    SenderState.Idle, is SenderState.Searching -> {
                        val peers = (currentState as? SenderState.Searching)?.peers.orEmpty()
                        Text(
                            text = "Looking for a receiver nearby…",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = VisionaryColors.color0B1B14,
                        )
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(0.6f),
                            color = VisionaryColors.color007847,
                        )
                        if (peers.size > 1) {
                            Spacer(Modifier.height(8.dp))
                            Text("Multiple receivers found:", style = MaterialTheme.typography.labelMedium)
                            LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                                items(peers) { peer ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { viewModel.connect(peer) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = VisionaryColors.colorCEDED3,
                                    ) {
                                        Text(
                                            peer.name,
                                            modifier = Modifier.padding(12.dp),
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is SenderState.Connecting -> {
                        Text(
                            text = "Connecting to ${currentState.peer.name}…",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = VisionaryColors.color0B1B14,
                        )
                        CircularProgressIndicator(color = VisionaryColors.color007847)
                    }

                    is SenderState.Connected -> {
                        Text(
                            text = "Ready to share",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = VisionaryColors.color0B1B14,
                        )
                        Text(
                            text = "Close your fist in front of the camera or pinch three fingers to grab the photo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VisionaryColors.color496156,
                            textAlign = TextAlign.Center,
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = VisionaryColors.colorCEDED3,
                        ) {
                            Text(
                                text = "${currentState.peer.name} · Pairing Code: ${currentState.pairingCode}",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    is SenderState.Released -> {
                        Text(
                            text = "Teleporting… ${(currentState.transferProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = VisionaryColors.color007847,
                        )
                        LinearProgressIndicator(
                            progress = { currentState.transferProgress },
                            modifier = Modifier.fillMaxWidth(0.8f),
                            color = VisionaryColors.color007847,
                        )
                    }

                    SenderState.Delivered -> {
                        Text(
                            text = "Delivered ✓",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = VisionaryColors.color007847,
                        )
                    }

                    is SenderState.Failed -> {
                        Text(
                            text = currentState.error.toMessage(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { viewModel.start(photo) },
                                colors = ButtonDefaults.buttonColors(containerColor = VisionaryColors.color007847),
                            ) {
                                Text("Try Again")
                            }
                            OutlinedButton(onClick = onClose) {
                                Text("Close")
                            }
                        }
                    }

                    SenderState.Cancelled -> Unit
                }
            }
        }
    }
}
