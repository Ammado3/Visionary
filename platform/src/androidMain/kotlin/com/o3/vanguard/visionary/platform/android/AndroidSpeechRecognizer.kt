package com.o3.vanguard.visionary.platform.android

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.o3.vanguard.visionary.core.common.AppDispatchers
import com.o3.vanguard.visionary.domain.port.SpeechAvailability
import com.o3.vanguard.visionary.domain.port.SpeechEvent
import com.o3.vanguard.visionary.domain.port.SpeechFailure
import com.o3.vanguard.visionary.domain.port.SpeechRecognizerPort
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.util.Locale

class AndroidSpeechRecognizer(
    private val context: Context,
    private val dispatchers: AppDispatchers,
) : SpeechRecognizerPort {

    override fun availability(): SpeechAvailability {
        return when {
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) && SpeechRecognizer.isOnDeviceRecognitionAvailable(context) -> {
                SpeechAvailability.ON_DEVICE
            }
            SpeechRecognizer.isRecognitionAvailable(context) -> {
                SpeechAvailability.SYSTEM_SERVICE
            }
            else -> SpeechAvailability.UNAVAILABLE
        }
    }

    override fun listen(): Flow<SpeechEvent> = callbackFlow {
        val isDeviceMode = availability() == SpeechAvailability.ON_DEVICE

        val recognizer = if (isDeviceMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }

        val languageExtra = if (Locale.getDefault().language == "en") {
            Locale.getDefault().toLanguageTag()
        } else {
            "en-US"
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageExtra)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(SpeechEvent.Ready)
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val failureReason = mapSpeechError(error)
                trySend(SpeechEvent.Failed(failureReason))
                close()
            }

            override fun onResults(results: Bundle?) {
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
                trySend(SpeechEvent.Final(list))
                close()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val list = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val first = list?.firstOrNull()
                if (!first.isNullOrBlank()) {
                    trySend(SpeechEvent.Partial(first))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)

        awaitClose {
            recognizer.cancel()
            recognizer.destroy()
        }
    }.flowOn(dispatchers.main)

    private fun mapSpeechError(error: Int): SpeechFailure {
        return when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> SpeechFailure.NO_MATCH
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechFailure.NO_SPEECH
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechFailure.PERMISSION_DENIED
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SpeechFailure.BUSY
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, -> SpeechFailure.NOT_AVAILABLE_OFFLINE
            else -> SpeechFailure.OTHER
        }
    }
}
