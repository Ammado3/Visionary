package com.o3.vanguard.visionary.feature.teleport

import com.o3.vanguard.visionary.feature.teleport.domain.model.TeleportPeer
import com.o3.vanguard.visionary.feature.teleport.domain.port.DeviceNameProvider
import com.o3.vanguard.visionary.feature.teleport.domain.port.ReceivedPhotoStore
import com.o3.vanguard.visionary.feature.teleport.domain.port.ThumbnailMaker
import com.o3.vanguard.visionary.feature.teleport.domain.protocol.TeleportMessage
import com.o3.vanguard.visionary.feature.teleport.domain.transport.TeleportTransport
import com.o3.vanguard.visionary.feature.teleport.domain.transport.TransportEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeTransport : TeleportTransport {
    override val peers = MutableStateFlow<List<TeleportPeer>>(emptyList())
    private val _events = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 64)
    override val events = _events.asSharedFlow()
    val sent = mutableListOf<TeleportMessage>()
    val sentPayloads = mutableListOf<Long>()
    var shutdownCalled = false
    var advertisingStarted = false
    var discoveryStarted = false
    var connectedPeerId: String? = null
    var shouldFailStartDiscovery = false
    var shouldFailStartAdvertising = false
    var shouldFailConnect = false
    private var nextId = 1L

    override suspend fun startAdvertising(deviceName: String) {
        if (shouldFailStartAdvertising) error("Advertising failed")
        advertisingStarted = true
    }

    override suspend fun startDiscovery() {
        if (shouldFailStartDiscovery) error("Discovery failed")
        discoveryStarted = true
    }

    override suspend fun connect(peerId: String, localName: String) {
        if (shouldFailConnect) error("Connect failed")
        connectedPeerId = peerId
    }

    override suspend fun sendMessage(message: TeleportMessage) {
        sent += message
    }

    override suspend fun prepareFile(source: String): Long = nextId++

    override suspend fun sendPrepared(payloadId: Long) {
        sentPayloads += payloadId
    }

    override fun shutdown() {
        shutdownCalled = true
    }

    suspend fun emit(event: TransportEvent) = _events.emit(event)
}

object FakeThumbs : ThumbnailMaker {
    var shouldFail = false
    override suspend fun make(photoUri: String): String {
        if (shouldFail) error("Thumb creation failed")
        return "/tmp/thumb.jpg"
    }
}

object FakeNames : DeviceNameProvider {
    override fun deviceName(): String = "TestPhone"
}

class FakeStore : ReceivedPhotoStore {
    var shouldFail = false
    var savedStagedPath: String? = null
    override suspend fun save(stagedPath: String, fileName: String, mimeType: String): String {
        if (shouldFail) error("Save failed")
        savedStagedPath = stagedPath
        return "content://media/external/images/media/999"
    }
}
