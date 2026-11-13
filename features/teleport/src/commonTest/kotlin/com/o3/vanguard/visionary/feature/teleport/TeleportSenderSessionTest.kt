package com.o3.vanguard.visionary.feature.teleport

import com.o3.vanguard.visionary.feature.teleport.domain.model.*
import com.o3.vanguard.visionary.feature.teleport.domain.protocol.TeleportMessage
import com.o3.vanguard.visionary.feature.teleport.domain.session.TeleportConfig
import com.o3.vanguard.visionary.feature.teleport.domain.session.TeleportSenderSession
import com.o3.vanguard.visionary.feature.teleport.domain.transport.TransportEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TeleportSenderSessionTest {

    private val samplePhoto = PhotoRef(
        uri = "content://media/1",
        displayName = "photo.jpg",
        mimeType = "image/jpeg",
        sizeBytes = 1024L,
    )
    private val peer1 = TeleportPeer("p1", "Pixel")
    private val peer2 = TeleportPeer("p2", "Galaxy")

    @Test
    fun sender_happyPath() = runTest {
        val transport = FakeTransport()
        val session = TeleportSenderSession(transport, FakeThumbs, FakeNames, backgroundScope)
        session.start(samplePhoto)

        transport.peers.value = listOf(peer1)
        advanceTimeBy(1_300)
        runCurrent()

        transport.emit(TransportEvent.Connected(peer1, "1234"))
        runCurrent()

        assertIs<SenderState.Connected>(session.state.value)
        assertIs<TeleportMessage.Offer>(transport.sent.first())

        session.onGrabCommitted()
        runCurrent()
        assertIs<SenderState.Released>(session.state.value)

        val offer = transport.sent.first() as TeleportMessage.Offer
        transport.emit(TransportEvent.MessageReceived(TeleportMessage.Committed(offer.transferId)))
        runCurrent()

        assertEquals(SenderState.Delivered, session.state.value)
        assertTrue(transport.shutdownCalled)
    }

    @Test
    fun sender_noReceiverFound_timesOut() = runTest {
        val transport = FakeTransport()
        val config = TeleportConfig(searchTimeoutMs = 1_000)
        val session = TeleportSenderSession(transport, FakeThumbs, FakeNames, backgroundScope, config)

        session.start(samplePhoto)
        advanceTimeBy(1_500)
        runCurrent()

        val current = session.state.value
        assertIs<SenderState.Failed>(current)
        assertEquals(TeleportError.NoReceiverFound, current.error)
        assertTrue(transport.shutdownCalled)
    }

    @Test
    fun sender_manualConnect_withMultiplePeers() = runTest {
        val transport = FakeTransport()
        val session = TeleportSenderSession(transport, FakeThumbs, FakeNames, backgroundScope)

        session.start(samplePhoto)
        transport.peers.value = listOf(peer1, peer2)
        advanceTimeBy(2_000)
        runCurrent()

        val searchingState = session.state.value
        assertIs<SenderState.Searching>(searchingState)
        assertEquals(2, searchingState.peers.size)

        session.connect(peer2)
        runCurrent()
        assertIs<SenderState.Connecting>(session.state.value)
        assertEquals("p2", transport.connectedPeerId)
    }

    @Test
    fun sender_progressUpdates() = runTest {
        val transport = FakeTransport()
        val session = TeleportSenderSession(transport, FakeThumbs, FakeNames, backgroundScope)

        session.start(samplePhoto)
        transport.peers.value = listOf(peer1)
        advanceTimeBy(1_300)
        runCurrent()

        transport.emit(TransportEvent.Connected(peer1, "9999"))
        runCurrent()

        val offer = transport.sent.first() as TeleportMessage.Offer
        transport.emit(TransportEvent.FileProgress(offer.filePayloadId, 512L, 1024L, outgoing = true))
        runCurrent()

        val connectedState = session.state.value
        assertIs<SenderState.Connected>(connectedState)
        assertEquals(0.5f, connectedState.transferProgress)
    }

    @Test
    fun sender_remoteCancelled() = runTest {
        val transport = FakeTransport()
        val session = TeleportSenderSession(transport, FakeThumbs, FakeNames, backgroundScope)

        session.start(samplePhoto)
        transport.peers.value = listOf(peer1)
        advanceTimeBy(1_300)
        runCurrent()

        transport.emit(TransportEvent.Connected(peer1, "1234"))
        runCurrent()

        transport.emit(TransportEvent.MessageReceived(TeleportMessage.Cancel(null)))
        runCurrent()

        val failedState = session.state.value
        assertIs<SenderState.Failed>(failedState)
        assertEquals(TeleportError.RemoteCancelled, failedState.error)
        assertTrue(transport.shutdownCalled)
    }

    @Test
    fun sender_userCancel() = runTest {
        val transport = FakeTransport()
        val session = TeleportSenderSession(transport, FakeThumbs, FakeNames, backgroundScope)

        session.start(samplePhoto)
        transport.peers.value = listOf(peer1)
        advanceTimeBy(1_300)
        runCurrent()

        transport.emit(TransportEvent.Connected(peer1, "1234"))
        runCurrent()

        session.cancel()
        runCurrent()

        assertEquals(SenderState.Cancelled, session.state.value)
        assertTrue(transport.sent.any { it is TeleportMessage.Cancel })
        assertTrue(transport.shutdownCalled)
    }

    @Test
    fun sender_connectionFailed() = runTest {
        val transport = FakeTransport()
        val session = TeleportSenderSession(transport, FakeThumbs, FakeNames, backgroundScope)

        session.start(samplePhoto)
        transport.emit(TransportEvent.ConnectionFailed("p1", "Refused"))
        runCurrent()

        val failedState = session.state.value
        assertIs<SenderState.Failed>(failedState)
        assertEquals(TeleportError.ConnectionFailed, failedState.error)
    }
}
