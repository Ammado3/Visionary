package com.o3.vanguard.visionary.feature.teleport.domain.session

import com.o3.vanguard.visionary.feature.teleport.domain.model.*
import com.o3.vanguard.visionary.feature.teleport.domain.port.DeviceNameProvider
import com.o3.vanguard.visionary.feature.teleport.domain.port.ThumbnailMaker
import com.o3.vanguard.visionary.feature.teleport.domain.protocol.TeleportMessage
import com.o3.vanguard.visionary.feature.teleport.domain.transport.TeleportTransport
import com.o3.vanguard.visionary.feature.teleport.domain.transport.TransportEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class TeleportSenderSession(
    private val transport: TeleportTransport,
    private val thumbnails: ThumbnailMaker,
    private val deviceNames: DeviceNameProvider,
    private val scope: CoroutineScope,
    private val config: TeleportConfig = TeleportConfig(),
) {
    private val _state = MutableStateFlow<SenderState>(SenderState.Idle)
    val state: StateFlow<SenderState> = _state.asStateFlow()

    private var photo: PhotoRef? = null
    private var transferId: String = ""
    private var filePayloadId: Long = NO_ID
    private val offerSent = CompletableDeferred<Unit>()
    private val collectors = mutableListOf<Job>()
    private var autoConnectJob: Job? = null
    private var timeoutJob: Job? = null

    fun start(photo: PhotoRef) {
        if (_state.value != SenderState.Idle) return
        this.photo = photo
        transferId = Random.nextLong().toULong().toString(HEX)
        _state.value = SenderState.Searching(emptyList())
        collectors += scope.launch(start = CoroutineStart.UNDISPATCHED) { transport.events.collect(::onEvent) }
        collectors += scope.launch(start = CoroutineStart.UNDISPATCHED) { transport.peers.collect(::onPeers) }
        scope.launch {
            try {
                transport.startDiscovery()
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                fail(TeleportError.ConnectionFailed)
            }
        }
        armTimeout(config.searchTimeoutMs, TeleportError.NoReceiverFound)
    }

    fun connect(peer: TeleportPeer) {
        if (_state.value !is SenderState.Searching) return
        autoConnectJob?.cancel()
        _state.value = SenderState.Connecting(peer)
        scope.launch {
            try {
                transport.connect(peer.id, deviceNames.deviceName())
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                fail(TeleportError.ConnectionFailed)
            }
        }
    }

    /** Called by the UI when the grab gesture completes. */
    fun onGrabCommitted() {
        val s = _state.value as? SenderState.Connected ?: return
        _state.value = SenderState.Released(s.peer, s.transferProgress)
        scope.launch {
            offerSent.await()
            bestEffort { transport.sendMessage(TeleportMessage.Released(transferId)) }
        }
    }

    fun cancel() {
        val current = _state.value
        if (current.isTerminal) return
        val wasConnected = current is SenderState.Connected || current is SenderState.Released
        scope.launch {
            if (wasConnected) bestEffort { transport.sendMessage(TeleportMessage.Cancel(transferId)) }
            finish(SenderState.Cancelled)
        }
    }

    /** Call from ViewModel.onCleared(): synchronous, no coroutines. */
    fun dispose() {
        transport.shutdown()
        collectors.forEach { it.cancel() }
        collectors.clear()
        timeoutJob?.cancel()
        autoConnectJob?.cancel()
    }

    private fun onPeers(peers: List<TeleportPeer>) {
        if (_state.value !is SenderState.Searching) return
        _state.value = SenderState.Searching(peers)
        if (peers.size == 1 && autoConnectJob == null) {
            autoConnectJob = scope.launch {
                delay(config.autoConnectDelayMs)
                val latest = transport.peers.value
                if (_state.value is SenderState.Searching && latest.size == 1) connect(latest.first())
                autoConnectJob = null
            }
        }
    }

    private fun onEvent(event: TransportEvent) {
        when (event) {
            is TransportEvent.Connected -> onConnected(event)
            is TransportEvent.ConnectionFailed -> if (!_state.value.isTerminal) fail(TeleportError.ConnectionFailed)
            is TransportEvent.Disconnected -> if (!_state.value.isTerminal) fail(TeleportError.Disconnected)
            is TransportEvent.FileProgress ->
                if (event.outgoing && event.payloadId == filePayloadId && event.total > 0) {
                    updateProgress(event.transferred.toFloat() / event.total)
                }
            is TransportEvent.FileCompleted -> if (event.outgoing && event.payloadId == filePayloadId) updateProgress(1f)
            is TransportEvent.FileFailed -> if (event.outgoing && event.payloadId == filePayloadId) fail(TeleportError.TransferFailed)
            is TransportEvent.MessageReceived -> onMessage(event.message)
        }
    }

    private fun onMessage(message: TeleportMessage) {
        when (message) {
            is TeleportMessage.Committed -> if (message.transferId == transferId) finish(SenderState.Delivered)
            is TeleportMessage.Cancel -> fail(TeleportError.RemoteCancelled)
            else -> Unit
        }
    }

    private fun onConnected(event: TransportEvent.Connected) {
        val current = _state.value
        if (current !is SenderState.Connecting && current !is SenderState.Searching) return
        autoConnectJob?.cancel()
        armTimeout(config.transferTimeoutMs, TeleportError.TimedOut)
        _state.value = SenderState.Connected(event.peer, event.pairingCode, 0f)
        scope.launch { sendOffer() }
    }

    private suspend fun sendOffer() {
        val p = photo ?: return
        try {
            val thumbId = transport.prepareFile(thumbnails.make(p.uri))
            val fileId = transport.prepareFile(p.uri)
            filePayloadId = fileId
            transport.sendMessage(
                TeleportMessage.Offer(transferId, p.displayName, p.mimeType, p.sizeBytes, thumbId, fileId),
            )
            offerSent.complete(Unit)
            transport.sendPrepared(thumbId)
            transport.sendPrepared(fileId)
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            fail(TeleportError.TransferFailed)
        }
    }

    private fun updateProgress(value: Float) {
        _state.value = when (val s = _state.value) {
            is SenderState.Connected -> s.copy(transferProgress = value)
            is SenderState.Released -> s.copy(transferProgress = value)
            else -> s
        }
    }

    private fun armTimeout(ms: Long, error: TeleportError) {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(ms)
            fail(error)
        }
    }

    private fun fail(error: TeleportError) = finish(SenderState.Failed(error))

    private fun finish(terminal: SenderState) {
        if (_state.value.isTerminal) return
        _state.value = terminal
        transport.shutdown()
        timeoutJob?.cancel()
        autoConnectJob?.cancel()
        collectors.forEach { it.cancel() }
        collectors.clear()
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

    private companion object {
        const val NO_ID = -1L
        const val HEX = 16
    }
}
