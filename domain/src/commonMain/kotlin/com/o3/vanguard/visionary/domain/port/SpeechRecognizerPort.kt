package com.o3.vanguard.visionary.domain.port

import kotlinx.coroutines.flow.Flow

enum class SpeechAvailability { ON_DEVICE, SYSTEM_SERVICE, UNAVAILABLE }

sealed interface SpeechEvent {
    data object Ready : SpeechEvent
    data class Partial(val text: String) : SpeechEvent
    data class Final(val alternatives: List<String>) : SpeechEvent
    data class Failed(val reason: SpeechFailure) : SpeechEvent
}

enum class SpeechFailure { NO_MATCH, NO_SPEECH, PERMISSION_DENIED, BUSY, NOT_AVAILABLE_OFFLINE, OTHER }

interface SpeechRecognizerPort {
    fun availability(): SpeechAvailability
    /** Cold flow: collecting starts listening, cancelling the collector stops it. Ends after Final or Failed. */
    fun listen(): Flow<SpeechEvent>
}
