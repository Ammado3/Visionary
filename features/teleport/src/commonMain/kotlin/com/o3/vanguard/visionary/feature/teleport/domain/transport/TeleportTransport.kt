package com.o3.vanguard.visionary.feature.teleport.domain.transport

import com.o3.vanguard.visionary.feature.teleport.domain.model.TeleportPeer
import com.o3.vanguard.visionary.feature.teleport.domain.protocol.TeleportMessage
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface TransportEvent {
    data class Connected(val peer: TeleportPeer, val pairingCode: String) : TransportEvent
    data class ConnectionFailed(val peerId: String, val reason: String) : TransportEvent
    data class Disconnected(val peerId: String) : TransportEvent
    data class MessageReceived(val message: TeleportMessage) : TransportEvent
    data class FileProgress(val payloadId: Long, val transferred: Long, val total: Long, val outgoing: Boolean) : TransportEvent

    /** For incoming files [stagedPath] is the app-private path of the received file. */
    data class FileCompleted(val payloadId: Long, val outgoing: Boolean, val stagedPath: String?) : TransportEvent
    data class FileFailed(val payloadId: Long, val outgoing: Boolean) : TransportEvent
}

/**
 * One instance == one session with at most ONE connected peer.
 * After [shutdown] the instance must not be reused.
 */
interface TeleportTransport {
    val peers: StateFlow<List<TeleportPeer>>
    val events: SharedFlow<TransportEvent>

    suspend fun startAdvertising(deviceName: String)
    suspend fun startDiscovery()
    suspend fun connect(peerId: String, localName: String)
    suspend fun sendMessage(message: TeleportMessage)

    /** [source] is a content:// URI string or an absolute file path. Returns the payload id. */
    suspend fun prepareFile(source: String): Long
    suspend fun sendPrepared(payloadId: Long)

    /** Stops advertising/discovery, disconnects, cleans staging files. Non-suspending on purpose. */
    fun shutdown()
}
