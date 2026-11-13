package com.o3.vanguard.visionary.feature.teleport.presentation.ui.portal

object PortalTiming {
    const val FadeInMs: Int = 450
    const val BlinkDipMs: Int = 320
    const val BlinkRiseMs: Int = 320
    const val HoldMs: Int = 150
    const val FadeOutMs: Int = 450

    val TotalCaptionMs: Long = (FadeInMs + 2 * (BlinkDipMs + BlinkRiseMs) + HoldMs + FadeOutMs).toLong()
}
