package com.o3.vanguard.visionary.feature.teleport.presentation.ui.portal

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
    var stage: PortalStage = initialStage
        private set

    fun onIntroAnimationFinished(sessionReady: Boolean) {
        if (stage == PortalStage.Opening) {
            stage = if (sessionReady) PortalStage.Ready else PortalStage.Holding
        }
    }

    fun onSessionReady() {
        if (stage == PortalStage.Holding) {
            stage = PortalStage.Ready
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
