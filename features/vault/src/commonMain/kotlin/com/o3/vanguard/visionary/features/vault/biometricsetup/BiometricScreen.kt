package com.o3.vanguard.visionary.features.vault.biometricsetup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import org.jetbrains.compose.resources.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.o3.vanguard.visionary.core.designsystem.AppTab
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import com.o3.vanguard.visionary.core.designsystem.VisionaryTheme
import com.o3.vanguard.visionary.core.designsystem.component.VisionaryHeader
import com.o3.vanguard.visionary.core.designsystem.generated.resources.Res
import com.o3.vanguard.visionary.core.designsystem.generated.resources.vault
import com.o3.vanguard.visionary.core.mvi.CollectEvents
import com.o3.vanguard.visionary.features.vault.biometricsetup.states.BiometricEvent
import com.o3.vanguard.visionary.features.vault.biometricsetup.states.BiometricIntent
import com.o3.vanguard.visionary.features.vault.biometricsetup.states.BiometricState
import com.o3.vanguard.visionary.features.vault.biometricsetup.states.LockMode
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BiometricSetupRoute(
    onUnlocked: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (AppTab) -> Unit,
    viewModel: BiometricViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            BiometricEvent.UnlockSuccess -> onUnlocked()
            BiometricEvent.NavigateBack -> onBack()
        }
    }
    BiometricSetupScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun BiometricSetupScreen(
    state: BiometricState,
    onIntent: (BiometricIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(BiometricIntent.DismissDialog) },
            title = { Text("Success") },
            text = { Text(state.dialogMessage) },
            confirmButton = {
                Button(onClick = { onIntent(BiometricIntent.DismissDialog) }) {
                    Text("OK")
                }
            }
        )
    }

    if (state.showFailureDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(BiometricIntent.DismissDialog) },
            title = { Text("Authentication") },
            text = { Text(state.dialogMessage) },
            confirmButton = {
                Button(onClick = { onIntent(BiometricIntent.DismissDialog) }) {
                    Text("OK")
                }
            }
        )
    }

    Surface(modifier = modifier.fillMaxSize(), color = VisionaryColors.colorF4FDF7) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(24.dp))

            VisionaryHeader(
                title = "Private Vault",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = { onIntent(BiometricIntent.OnBackClicked) },
                actions = null,
                backgroundColor = VisionaryColors.colorF4FDF7,
                contentColor = VisionaryColors.color0B1B14,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Lock Icon Container
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = VisionaryColors.color1A007847,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(Res.drawable.vault),
                            contentDescription = "Lock",
                            modifier = Modifier.size(36.dp),
                            colorFilter = ColorFilter.tint(VisionaryColors.color007847)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Unlock your Vault",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = VisionaryColors.color0B1B14,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (state.lockMode == LockMode.PATTERN) "Connect at least 4 dots." else "Enter your PIN to continue.",
                    fontSize = 14.sp,
                    color = VisionaryColors.color496156,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (state.lockMode == LockMode.PIN) {
                    // Dots indicator
                    val dotCount = maxOf(4, state.pin.length)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(dotCount) { index ->
                            val isFilled = index < state.pin.length
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(
                                        color = if (isFilled) VisionaryColors.color007847 else Color.Transparent,
                                        shape = CircleShape,
                                    )
                                    .then(
                                        if (!isFilled) Modifier.border(1.5.dp, VisionaryColors.color007847, CircleShape)
                                        else Modifier
                                    )
                            )
                        }
                    }
                } else {
                    // Pattern Lock 3x3 Grid
                    PatternLockGrid(
                        selectedDots = state.patternSequence,
                        onDotSelected = { dot -> onIntent(BiometricIntent.OnDotSelected(dot)) },
                        onPatternCompleted = { onIntent(BiometricIntent.OnPatternCompleted) },
                        onCleared = { onIntent(BiometricIntent.OnClearPatternClicked) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { onIntent(BiometricIntent.OnClearPatternClicked) }) {
                        Text("Clear pattern", color = VisionaryColors.color007847, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.errorMessage,
                        color = Color(0xFFBA1A1A),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (state.lockMode == LockMode.PIN) {
                    // Keypad
                    val rows = state.rowNumber
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        for (row in rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                for (item in row) {
                                    KeypadButton(
                                        item = item,
                                        onClick = {
                                            when (item) {
                                                "delete" -> onIntent(BiometricIntent.OnDeleteClicked)
                                                "biometric" -> onIntent(BiometricIntent.OnBiometricClicked)
                                                else -> onIntent(BiometricIntent.OnNumberClicked(item.first()))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Use biometric button
                Button(
                    onClick = { onIntent(BiometricIntent.OnBiometricClicked) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VisionaryColors.color1A007847,
                        contentColor = VisionaryColors.color007847,
                    ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = VisionaryColors.color007847,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Use biometric",
                            fontWeight = FontWeight.Bold,
                            color = VisionaryColors.color007847,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle between PIN and Pattern
                TextButton(onClick = {
                    val newMode = if (state.lockMode == LockMode.PIN) LockMode.PATTERN else LockMode.PIN
                    onIntent(BiometricIntent.OnSwitchMode(newMode))
                }) {
                    Text(
                        text = if (state.lockMode == LockMode.PIN) "Use Pattern lock instead" else "Use PIN instead",
                        color = VisionaryColors.color007847,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Forgot security credentials?\nRecovery options depend on your device security settings.",
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = VisionaryColors.color496156,
                    modifier = Modifier.clickable { onIntent(BiometricIntent.OnForgotPinClicked) },
                )
            }
        }
    }
}

@Composable
fun PatternLockGrid(
    selectedDots: List<Int>,
    onDotSelected: (Int) -> Unit,
    onPatternCompleted: () -> Unit,
    onCleared: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentPosition by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier
            .size(260.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val dot = getDotAtOffset(offset, size.width.toFloat(), size.height.toFloat())
                        if (dot != null && !selectedDots.contains(dot)) {
                            onDotSelected(dot)
                        }
                        currentPosition = offset
                    },
                    onDrag = { change, _ ->
                        currentPosition = change.position
                        val dot = getDotAtOffset(change.position, size.width.toFloat(), size.height.toFloat())
                        if (dot != null && !selectedDots.contains(dot)) {
                            onDotSelected(dot)
                        }
                    },
                    onDragEnd = {
                        currentPosition = null
                        onPatternCompleted()
                    },
                    onDragCancel = {
                        currentPosition = null
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val spacingX = width / 4f
            val spacingY = height / 4f

            val points = listOf(
                Offset(spacingX, spacingY), Offset(width / 2f, spacingY), Offset(width - spacingX, spacingY),
                Offset(spacingX, height / 2f), Offset(width / 2f, height / 2f), Offset(width - spacingX, height / 2f),
                Offset(spacingX, height - spacingY), Offset(width / 2f, height - spacingY), Offset(width - spacingX, height - spacingY)
            )

            // Draw lines between selected dots
            for (i in 0 until selectedDots.size - 1) {
                val start = points[selectedDots[i]]
                val end = points[selectedDots[i + 1]]
                drawLine(
                    color = VisionaryColors.color007847,
                    start = start,
                    end = end,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Draw line to current drag position
            if (currentPosition != null && selectedDots.isNotEmpty()) {
                val lastPoint = points[selectedDots.last()]
                drawLine(
                    color = VisionaryColors.color007847.copy(alpha = 0.7f),
                    start = lastPoint,
                    end = currentPosition!!,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Draw 9 dots
            points.forEachIndexed { index, point ->
                val isSelected = selectedDots.contains(index)
                drawCircle(
                    color = if (isSelected) VisionaryColors.color007847 else VisionaryColors.colorCEDED3,
                    radius = if (isSelected) 14.dp.toPx() else 8.dp.toPx(),
                    center = point
                )
                if (isSelected) {
                    drawCircle(
                        color = VisionaryColors.color1A007847,
                        radius = 24.dp.toPx(),
                        center = point
                    )
                }
            }
        }
    }
}

private fun getDotAtOffset(offset: Offset, width: Float, height: Float): Int? {
    val spacingX = width / 4f
    val spacingY = height / 4f
    val points = listOf(
        Offset(spacingX, spacingY), Offset(width / 2f, spacingY), Offset(width - spacingX, spacingY),
        Offset(spacingX, height / 2f), Offset(width / 2f, height / 2f), Offset(width - spacingX, height / 2f),
        Offset(spacingX, height - spacingY), Offset(width / 2f, height - spacingY), Offset(width - spacingX, height - spacingY)
    )
    val threshold = width / 5f
    points.forEachIndexed { index, point ->
        val distance = (offset - point).getDistance()
        if (distance < threshold) {
            return index
        }
    }
    return null
}

@Composable
fun KeypadButton(
    item: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when (item) {
            "delete" -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Delete",
                    tint = VisionaryColors.color0B1B14,
                    modifier = Modifier.size(24.dp),
                )
            }
            "biometric" -> {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric",
                    tint = VisionaryColors.color007847,
                    modifier = Modifier.size(26.dp),
                )
            }
            else -> {
                Text(
                    text = item,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = VisionaryColors.color0B1B14,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BiometricSetupScreenPreview() {
    VisionaryTheme {
        BiometricSetupScreen(
            state = BiometricState(lockMode = LockMode.PATTERN),
            onIntent = {},
        )
    }
}
