package com.o3.vanguard.visionary.feature.teleport.presentation.ui.portal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class PortalStage {
    Opening,
    Holding,
    Ready,
    Live,
    Failed,
}

class PortalChoreography(
    initialStage: PortalStage = PortalStage.Opening,
) {
    var stage: PortalStage by mutableStateOf(initialStage)
        private set

    var isSessionReady: Boolean by mutableStateOf(false)
        private set

    fun updateSessionReady(ready: Boolean) {
        isSessionReady = ready
        if (ready && stage == PortalStage.Holding) {
            stage = PortalStage.Ready
        }
    }

    fun onIntroAnimationFinished() {
        if (stage == PortalStage.Opening) {
            stage = if (isSessionReady) PortalStage.Ready else PortalStage.Holding
        }
    }

    fun onReadyAnimationFinished() {
        if (stage == PortalStage.Ready) {
            stage = PortalStage.Live
        }
    }

    fun onSessionFailed() {
        if (stage != PortalStage.Failed) {
            stage = PortalStage.Failed
        }
    }
}
