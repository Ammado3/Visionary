package com.o3.vanguard.visionary.feature.teleport.presentation.ui.portal

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class HintDebouncer(
    private val minDisplayMs: Long = 400L,
    private val ignoreShorterThanMs: Long = 250L,
) {
    var displayedHint by mutableStateOf<String?>(null)
        private set

    fun processHintUpdate(
        candidate: String?,
        nowMs: Long,
        lastChangeMs: Long,
        candidateStartMs: Long,
    ): Pair<String?, Boolean> {
        if (candidate == displayedHint) {
            return Pair(displayedHint, false)
        }

        val candidateDuration = nowMs - candidateStartMs
        if (candidateDuration < ignoreShorterThanMs) {
            return Pair(displayedHint, false)
        }

        val currentDisplayDuration = nowMs - lastChangeMs
        if (displayedHint != null && currentDisplayDuration < minDisplayMs) {
            return Pair(displayedHint, false)
        }

        displayedHint = candidate
        return Pair(candidate, true)
    }
}

@Composable
fun rememberDebouncedHint(
    targetHint: String?,
    minDisplayMs: Long = 400L,
    ignoreShorterThanMs: Long = 250L,
): String? {
    var displayedHint by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(targetHint) {
        if (targetHint == displayedHint) return@LaunchedEffect

        if (displayedHint == null) {
            // Immediate display when transitioning from no hint to a hint
            displayedHint = targetHint
            delay(minDisplayMs)
        } else if (targetHint == null) {
            // Delay before clearing hint
            delay(minDisplayMs)
            displayedHint = null
        } else {
            // Transitioning between hints: debounce change
            delay(ignoreShorterThanMs)
            displayedHint = targetHint
            delay(minDisplayMs)
        }
    }

    return displayedHint
}

@Composable
fun PortalHint(
    hint: String?,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = hint,
        animationSpec = tween(220),
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        label = "PortalHintCrossfade",
    ) { currentText ->
        if (!currentText.isNullOrBlank()) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = currentText,
                    style = TextStyle(
                        fontWeight = FontWeight.W300,
                        fontSize = 18.sp,
                        letterSpacing = 1.5.sp,
                        color = PortalPalette.TextWhite,
                        shadow = Shadow(
                            color = PortalPalette.EmeraldGlow.copy(alpha = 0.6f),
                            blurRadius = 8f,
                        ),
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
