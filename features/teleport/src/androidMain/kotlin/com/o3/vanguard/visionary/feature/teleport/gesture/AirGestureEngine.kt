package com.o3.vanguard.visionary.feature.teleport.gesture

/**
 * Turns a stream of [HandFrame]s into gesture progress.
 *
 * GRAB: the user must first show an OPEN hand (armed), then close it.
 * OPEN_HAND: the user must first show a CLOSED fist (armed), then open it.
 *
 * Arming removes the old "baseline = first frame" bug, smoothing removes jitter, the commit needs the end pose
 * held for [Config.commitHoldMs] (no accidental commits from one noisy frame), and a short grace window
 * keeps the gesture alive across a dropped detection. Not thread-safe: feed it from a single thread.
 */
class AirGestureEngine(
    private val target: TargetAirGesture,
    private val config: Config = Config(),
) {
    data class Config(
        val closedLevel: Float = 0.20f,
        val openLevel: Float = 0.85f,
        val armProgress: Float = 0.10f,
        val armHoldMs: Long = 250,
        val commitProgress: Float = 0.90f,
        val commitHoldMs: Long = 120,
        val handLostMs: Long = 300,
        val smoothing: Float = 0.5f,
        val minHandSize: Float = 0.18f,
        val maxHandSize: Float = 0.90f,
    )

    enum class Phase { NoHand, TooFar, TooClose, Arming, Ready, Active, Committed }

    data class Output(val phase: Phase, val progress: Float, val committed: Boolean = false)

    private var smoothed: Float? = null
    private var lastSeenMs = NONE
    private var armingSinceMs = NONE
    private var commitSinceMs = NONE
    private var armed = false
    private var done = false
    private var last = Output(Phase.NoHand, 0f)

    fun reset() {
        smoothed = null
        lastSeenMs = NONE
        armingSinceMs = NONE
        commitSinceMs = NONE
        armed = false
        done = false
        last = Output(Phase.NoHand, 0f)
    }

    /** [frame] is null when no hand was detected. */
    fun onFrame(frame: HandFrame?, nowMs: Long): Output {
        if (done) return last.copy(committed = false)
        if (frame == null) return onHandMissing(nowMs)
        lastSeenMs = nowMs

        if (frame.handSize < config.minHandSize) return hold(Phase.TooFar)
        if (frame.handSize > config.maxHandSize) return hold(Phase.TooClose)

        val s = smoothed?.let { config.smoothing * frame.openness + (1f - config.smoothing) * it } ?: frame.openness
        smoothed = s
        val p = progressOf(s)

        if (!armed) return onArming(p, nowMs)

        if (p >= config.commitProgress) {
            if (commitSinceMs == NONE) commitSinceMs = nowMs
            if (nowMs - commitSinceMs >= config.commitHoldMs) {
                done = true
                last = Output(Phase.Committed, 1f, committed = true)
                return last
            }
        } else {
            commitSinceMs = NONE
        }
        last = Output(if (p > ACTIVE_EPS) Phase.Active else Phase.Ready, p)
        return last
    }

    private fun onArming(p: Float, nowMs: Long): Output {
        if (p <= config.armProgress) {
            if (armingSinceMs == NONE) armingSinceMs = nowMs
            if (nowMs - armingSinceMs >= config.armHoldMs) armed = true
        } else {
            armingSinceMs = NONE
        }
        last = Output(if (armed) Phase.Ready else Phase.Arming, 0f)
        return last
    }

    private fun onHandMissing(nowMs: Long): Output {
        if (lastSeenMs == NONE || nowMs - lastSeenMs > config.handLostMs) {
            reset()
        }
        return last
    }

    private fun hold(phase: Phase): Output {
        armingSinceMs = NONE
        commitSinceMs = NONE
        last = Output(phase, 0f)
        return last
    }

    /** 0 at the start pose, 1 at the end pose. */
    private fun progressOf(openness: Float): Float {
        val span = config.openLevel - config.closedLevel
        val raw = when (target) {
            TargetAirGesture.GRAB -> (config.openLevel - openness) / span
            TargetAirGesture.OPEN_HAND -> (openness - config.closedLevel) / span
        }
        return raw.coerceIn(0f, 1f)
    }

    private companion object {
        const val NONE = Long.MIN_VALUE
        const val ACTIVE_EPS = 0.05f
    }
}
