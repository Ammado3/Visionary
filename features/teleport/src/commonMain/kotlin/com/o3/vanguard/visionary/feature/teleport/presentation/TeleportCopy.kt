package com.o3.vanguard.visionary.feature.teleport.presentation

import com.o3.vanguard.visionary.feature.teleport.gesture.TargetAirGesture
import com.o3.vanguard.visionary.feature.teleport.gesture.AirHint

object TeleportCopy {
    const val OpeningPortal = "Opening portal"
    const val ReadyToShare = "Ready to share"
    const val ReadyToReceive = "Ready to receive"

    const val ShowYourHand = "Show your hand"
    const val BringHandCloser = "Bring your hand closer"
    const val MoveHandBack = "Move your hand back a little"
    const val OpenYourHand = "Open your hand"
    const val CloseYourHand = "Close your hand"
    const val MakeAFist = "Make a fist"

    const val PinchFallback = "Pinch with three fingers"
    const val SpreadFallback = "Spread three fingers"

    const val Close = "Close"
    const val TryAgain = "Try again"
    const val MultipleReceiversFound = "Multiple receivers found"

    fun hintFor(hint: AirHint, target: TargetAirGesture): String? = when (hint) {
        AirHint.NoHand -> ShowYourHand
        AirHint.TooFar -> BringHandCloser
        AirHint.TooClose -> MoveHandBack
        AirHint.Arming -> if (target == TargetAirGesture.GRAB) OpenYourHand else MakeAFist
        AirHint.Ready -> if (target == TargetAirGesture.GRAB) CloseYourHand else OpenYourHand
        AirHint.Active, AirHint.Committed -> null
    }

    fun fallbackHintFor(target: TargetAirGesture): String =
        if (target == TargetAirGesture.GRAB) PinchFallback else SpreadFallback
}
