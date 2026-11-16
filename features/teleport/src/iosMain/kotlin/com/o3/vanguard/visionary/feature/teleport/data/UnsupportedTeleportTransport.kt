package com.o3.vanguard.visionary.feature.teleport.data

import com.o3.vanguard.visionary.feature.teleport.domain.model.TeleportPeer
import com.o3.vanguard.visionary.feature.teleport.domain.protocol.TeleportMessage
import com.o3.vanguard.visionary.feature.teleport.domain.transport.TeleportTransport
import com.o3.vanguard.visionary.feature.teleport.domain.transport.TransportEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class UnsupportedTeleportTransport : TeleportTransport {
    override val peers: StateFlow<List<TeleportPeer>> = MutableStateFlow<List<TeleportPeer>>(emptyList()).asStateFlow()
    override val events: SharedFlow<TransportEvent> = MutableSharedFlow<TransportEvent>().asSharedFlow()

    override suspend fun startAdvertising(deviceName: String) = error("Teleport is not supported on iOS")
    override suspend fun startDiscovery() = error("Teleport is not supported on iOS")
    override suspend fun connect(peerId: String, localName: String) = error("Teleport is not supported on iOS")
    override suspend fun sendMessage(message: TeleportMessage) = error("Teleport is not supported on iOS")
    override suspend fun prepareFile(source: String): Long = error("Teleport is not supported on iOS")
    override suspend fun sendPrepared(payloadId: Long) = error("Teleport is not supported on iOS")
    override fun shutdown() = Unit
}
