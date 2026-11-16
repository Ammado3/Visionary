package com.o3.vanguard.visionary.feature.teleport.domain.session

import com.o3.vanguard.visionary.feature.teleport.domain.model.*
import com.o3.vanguard.visionary.feature.teleport.domain.port.DeviceNameProvider
import com.o3.vanguard.visionary.feature.teleport.domain.port.ReceivedPhotoStore
import com.o3.vanguard.visionary.feature.teleport.domain.protocol.TeleportMessage
import com.o3.vanguard.visionary.feature.teleport.domain.transport.TeleportTransport
import com.o3.vanguard.visionary.feature.teleport.domain.transport.TransportEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TeleportReceiverSession(
    private val transport: TeleportTransport,
    private val store: ReceivedPhotoStore,
    private val deviceNames: DeviceNameProvider,
    private val scope: CoroutineScope,
    private val config: TeleportConfig = TeleportConfig(),
) {
    private val _state = MutableStateFlow<ReceiverState>(ReceiverState.Idle)
    val state: StateFlow<ReceiverState> = _state.asStateFlow()

    private var currentPeer: TeleportPeer? = null
    private var pairingCode: String = ""
    private var offer: TeleportMessage.Offer? = null
    private var released: Boolean = false
    private var dropped: Boolean = false
    private var isCommitting: Boolean = false

    private val progress = mutableMapOf<Long, Float>()
    private val completed = mutableMapOf<Long, String>()
    private val collectors = mutableListOf<Job>()
    private var timeoutJob: Job? = null

    fun start() {
        if (_state.value != ReceiverState.Idle) return
        _state.value = ReceiverState.Waiting
        collectors += scope.launch(start = CoroutineStart.UNDISPATCHED) { transport.events.collect(::onEvent) }
        scope.launch {
            try {
                transport.startAdvertising(deviceNames.deviceName())
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                fail(TeleportError.ConnectionFailed)
            }
        }
        armTimeout(config.receiverIdleTimeoutMs, TeleportError.TimedOut)
    }

    fun onDropCommitted() {
        if (_state.value.isTerminal) return
        dropped = true
        render()
        tryCommit()
    }

    fun cancel() {
        val current = _state.value
        if (current.isTerminal) return
        val wasConnected = (current is ReceiverState.Connected) || (current is ReceiverState.Incoming)
        val id = offer?.transferId
        scope.launch {
            if (wasConnected) bestEffort { transport.sendMessage(TeleportMessage.Cancel(id)) }
            finish(ReceiverState.Cancelled, graceful = false)
        }
    }

    fun dispose() {
        transport.shutdown()
        collectors.forEach { it.cancel() }
        collectors.clear()
        timeoutJob?.cancel()
    }

    private fun onEvent(event: TransportEvent) {
        when (event) {
            is TransportEvent.Connected -> onConnected(event)
            is TransportEvent.ConnectionFailed -> if (!_state.value.isTerminal) fail(TeleportError.ConnectionFailed)
            is TransportEvent.Disconnected -> onDisconnected()
            is TransportEvent.MessageReceived -> onMessage(event.message)
            is TransportEvent.FileProgress -> if (!event.outgoing) {
                progress[event.payloadId] = if (event.total > 0) event.transferred.toFloat() / event.total else 0f
                render()
            }
            is TransportEvent.FileCompleted -> if (!event.outgoing) {
                event.stagedPath?.let { completed[event.payloadId] = it }
                progress[event.payloadId] = 1f
                render()
                tryCommit()
            }
            is TransportEvent.FileFailed -> if (!event.outgoing) {
                val currentOffer = offer
                if (currentOffer != null && event.payloadId == currentOffer.filePayloadId) {
                    fail(TeleportError.TransferFailed)
                }
            }
        }
    }

    private fun onConnected(event: TransportEvent.Connected) {
        if (_state.value !is ReceiverState.Waiting) return
        currentPeer = event.peer
        pairingCode = event.pairingCode
        _state.value = ReceiverState.Connected(event.peer, event.pairingCode)
        armTimeout(config.transferTimeoutMs, TeleportError.TimedOut)
    }

    private fun onDisconnected() {
        if (_state.value.isTerminal) return
        val o = offer
        val fileDone = o != null && completed.containsKey(o.filePayloadId)
        if (fileDone && released) {
            return
        }
        fail(TeleportError.Disconnected)
    }

    private fun onMessage(message: TeleportMessage) {
        when (message) {
            is TeleportMessage.Offer -> {
                offer = message
                render()
                tryCommit()
            }
            is TeleportMessage.Released -> {
                released = true
                render()
                tryCommit()
            }
            is TeleportMessage.Cancel -> fail(TeleportError.RemoteCancelled)
            else -> Unit
        }
    }

    private fun render() {
        if (_state.value.isTerminal) return
        val p = currentPeer ?: return
        val o = offer ?: return
        val thumbPath = completed[o.thumbnailPayloadId]
        val prog = progress[o.filePayloadId] ?: 0f
        _state.value = ReceiverState.Incoming(
            peer = p,
            pairingCode = pairingCode,
            fileName = o.fileName,
            thumbnailPath = thumbPath,
            transferProgress = prog,
            senderReleased = released,
            userDropped = dropped,
        )
    }

    private fun tryCommit() {
        val o = offer ?: return
        val stagedFile = completed[o.filePayloadId] ?: return
        if (!dropped || !released || isCommitting || _state.value.isTerminal) return

        isCommitting = true
        scope.launch {
            try {
                val savedUri = store.save(stagedFile, o.fileName, o.mimeType)
                bestEffort { transport.sendMessage(TeleportMessage.Committed(o.transferId)) }
                finish(ReceiverState.Committed(savedUri), graceful = true)
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                fail(TeleportError.SaveFailed)
            }
        }
    }

    private fun armTimeout(ms: Long, error: TeleportError) {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(ms)
            fail(error)
        }
    }

    private fun fail(error: TeleportError) = finish(ReceiverState.Failed(error), graceful = false)

    private fun finish(terminal: ReceiverState, graceful: Boolean) {
        if (_state.value.isTerminal) return
        _state.value = terminal
        timeoutJob?.cancel()
        collectors.forEach { it.cancel() }
        collectors.clear()
        if (graceful) {
            scope.launch {
                delay(config.shutdownGraceMs)
                transport.shutdown()
            }
        } else {
            transport.shutdown()
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun bestEffort(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // best-effort
        }
    }
}
