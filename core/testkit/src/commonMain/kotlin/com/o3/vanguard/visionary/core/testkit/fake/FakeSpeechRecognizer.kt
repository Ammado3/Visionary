package com.o3.vanguard.visionary.core.testkit.fake

import com.o3.vanguard.visionary.domain.port.SpeechAvailability
import com.o3.vanguard.visionary.domain.port.SpeechEvent
import com.o3.vanguard.visionary.domain.port.SpeechRecognizerPort
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeSpeechRecognizer(
    var availability: SpeechAvailability = SpeechAvailability.UNAVAILABLE,
) : SpeechRecognizerPort {
    val script: MutableList<SpeechEvent> = mutableListOf()
    var listenCount: Int = 0
    var suspendForeverAfterScript: Boolean = false

    override fun availability(): SpeechAvailability = availability

    override fun listen(): Flow<SpeechEvent> = flow {
        listenCount++
        for (event in script) {
            emit(event)
        }
        if (suspendForeverAfterScript) {
            awaitCancellation()
        }
    }
}
