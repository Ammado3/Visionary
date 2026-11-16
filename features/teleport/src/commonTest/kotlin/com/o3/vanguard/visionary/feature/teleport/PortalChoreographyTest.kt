package com.o3.vanguard.visionary.feature.teleport

import com.o3.vanguard.visionary.feature.teleport.presentation.ui.portal.PortalChoreography
import com.o3.vanguard.visionary.feature.teleport.presentation.ui.portal.PortalStage
import kotlin.test.Test
import kotlin.test.assertEquals

class PortalChoreographyTest {

    @Test
    fun choreography_normalOrder_sessionReadyBeforeIntroEnds() {
        val choreography = PortalChoreography()
        assertEquals(PortalStage.Opening, choreography.stage)

        choreography.updateSessionReady(true)
        assertEquals(PortalStage.Opening, choreography.stage)

        choreography.onIntroAnimationFinished()
        assertEquals(PortalStage.Ready, choreography.stage)

        choreography.onReadyAnimationFinished()
        assertEquals(PortalStage.Live, choreography.stage)
    }

    @Test
    fun choreography_sessionReadyAfterIntroEnds_goesThroughHolding() {
        val choreography = PortalChoreography()
        assertEquals(PortalStage.Opening, choreography.stage)

        choreography.onIntroAnimationFinished()
        assertEquals(PortalStage.Holding, choreography.stage)

        choreography.updateSessionReady(true)
        assertEquals(PortalStage.Ready, choreography.stage)

        choreography.onReadyAnimationFinished()
        assertEquals(PortalStage.Live, choreography.stage)
    }

    @Test
    fun choreography_failureInOpening_transitionsToFailedImmediately() {
        val choreography = PortalChoreography()
        choreography.onSessionFailed()
        assertEquals(PortalStage.Failed, choreography.stage)
    }

    @Test
    fun choreography_failureInHolding_transitionsToFailed() {
        val choreography = PortalChoreography()
        choreography.onIntroAnimationFinished()
        assertEquals(PortalStage.Holding, choreography.stage)

        choreography.onSessionFailed()
        assertEquals(PortalStage.Failed, choreography.stage)
    }

    @Test
    fun choreography_failureInReady_transitionsToFailed() {
        val choreography = PortalChoreography()
        choreography.updateSessionReady(true)
        choreography.onIntroAnimationFinished()
        assertEquals(PortalStage.Ready, choreography.stage)

        choreography.onSessionFailed()
        assertEquals(PortalStage.Failed, choreography.stage)
    }

    @Test
    fun choreography_failureInLive_transitionsToFailed() {
        val choreography = PortalChoreography()
        choreography.updateSessionReady(true)
        choreography.onIntroAnimationFinished()
        choreography.onReadyAnimationFinished()
        assertEquals(PortalStage.Live, choreography.stage)

        choreography.onSessionFailed()
        assertEquals(PortalStage.Failed, choreography.stage)
    }

    @Test
    fun choreography_onceLive_doesNotReplayIntro() {
        val choreography = PortalChoreography(initialStage = PortalStage.Live)
        assertEquals(PortalStage.Live, choreography.stage)

        choreography.onIntroAnimationFinished()
        assertEquals(PortalStage.Live, choreography.stage)

        choreography.updateSessionReady(true)
        assertEquals(PortalStage.Live, choreography.stage)
    }
}
