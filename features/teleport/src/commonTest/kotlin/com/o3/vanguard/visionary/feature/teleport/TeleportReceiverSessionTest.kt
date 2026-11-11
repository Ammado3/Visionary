package com.o3.vanguard.visionary.feature.teleport

import com.o3.vanguard.visionary.feature.teleport.domain.model.*
import com.o3.vanguard.visionary.feature.teleport.domain.protocol.TeleportMessage
import com.o3.vanguard.visionary.feature.teleport.domain.session.TeleportConfig
import com.o3.vanguard.visionary.feature.teleport.domain.session.TeleportReceiverSession
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
class TeleportReceiverSessionTest {

    private val peer1 = TeleportPeer("p1", "SenderPhone")
    private val offerMsg = TeleportMessage.Offer(
        transferId = "t123",
        fileName = "photo.jpg",
        mimeType = "image/jpeg",
        sizeBytes = 2048L,
        thumbnailPayloadId = 100L,
        filePayloadId = 200L,
    )

    @Test
    fun receiver_happyPath_commitOnlyWhenFileCompleteReleasedAndDropped() = runTest {
        val transport = FakeTransport()
        val store = FakeStore()
        val session = TeleportReceiverSession(transport, store, FakeNames, backgroundScope)

        session.start()
        runCurrent()
        assertIs<ReceiverState.Waiting>(session.state.value)

        transport.emit(TransportEvent.Connected(peer1, "4321"))
        runCurrent()
        assertIs<ReceiverState.Connected>(session.state.value)

        transport.emit(TransportEvent.MessageReceived(offerMsg))
        runCurrent()

        transport.emit(TransportEvent.FileCompleted(200L, outgoing = false, stagedPath = "/cache/full.jpg"))
        runCurrent()

        var incoming = session.state.value
        assertIs<ReceiverState.Incoming>(incoming)
        assertEquals(1f, incoming.transferProgress)
        assertEquals(false, incoming.senderReleased)
        assertEquals(false, incoming.userDropped)

        transport.emit(TransportEvent.MessageReceived(TeleportMessage.Released("t123")))
        runCurrent()

        incoming = session.state.value
        assertIs<ReceiverState.Incoming>(incoming)
        assertTrue(incoming.senderReleased)
        assertEquals(false, incoming.userDropped)

        session.onDropCommitted()
        runCurrent()

        val committed = session.state.value
        assertIs<ReceiverState.Committed>(committed)
        assertEquals("content://media/external/images/media/999", committed.savedUri)
        assertTrue(transport.sent.any { it is TeleportMessage.Committed })
    }

    @Test
    fun receiver_thumbnailAndReleasedBeforeOffer() = runTest {
        val transport = FakeTransport()
        val store = FakeStore()
        val session = TeleportReceiverSession(transport, store, FakeNames, backgroundScope)

        session.start()
        transport.emit(TransportEvent.Connected(peer1, "4321"))
        runCurrent()

        transport.emit(TransportEvent.FileCompleted(100L, outgoing = false, stagedPath = "/cache/thumb.jpg"))
        transport.emit(TransportEvent.MessageReceived(TeleportMessage.Released("t123")))
        runCurrent()

        transport.emit(TransportEvent.MessageReceived(offerMsg))
        runCurrent()

        val incoming = session.state.value
        assertIs<ReceiverState.Incoming>(incoming)
        assertEquals("/cache/thumb.jpg", incoming.thumbnailPath)
        assertTrue(incoming.senderReleased)
    }

    @Test
    fun receiver_saveFailure_resultsInSaveFailed() = runTest {
        val transport = FakeTransport()
        val store = FakeStore().apply { shouldFail = true }
        val session = TeleportReceiverSession(transport, store, FakeNames, backgroundScope)

        session.start()
        transport.emit(TransportEvent.Connected(peer1, "4321"))
        transport.emit(TransportEvent.MessageReceived(offerMsg))
        transport.emit(TransportEvent.FileCompleted(200L, outgoing = false, stagedPath = "/cache/full.jpg"))
        transport.emit(TransportEvent.MessageReceived(TeleportMessage.Released("t123")))
        runCurrent()

        session.onDropCommitted()
        runCurrent()

        val failed = session.state.value
        assertIs<ReceiverState.Failed>(failed)
        assertEquals(TeleportError.SaveFailed, failed.error)
    }

    @Test
    fun receiver_idleTimeout() = runTest {
        val transport = FakeTransport()
        val store = FakeStore()
        val config = TeleportConfig(receiverIdleTimeoutMs = 1_000)
        val session = TeleportReceiverSession(transport, store, FakeNames, backgroundScope, config)

        session.start()
        advanceTimeBy(1_500)
        runCurrent()

        val failed = session.state.value
        assertIs<ReceiverState.Failed>(failed)
        assertEquals(TeleportError.TimedOut, failed.error)
        assertTrue(transport.shutdownCalled)
    }

    @Test
    fun receiver_remoteCancelled() = runTest {
        val transport = FakeTransport()
        val store = FakeStore()
        val session = TeleportReceiverSession(transport, store, FakeNames, backgroundScope)

        session.start()
        transport.emit(TransportEvent.Connected(peer1, "4321"))
        runCurrent()

        transport.emit(TransportEvent.MessageReceived(TeleportMessage.Cancel("t123")))
        runCurrent()

        val failed = session.state.value
        assertIs<ReceiverState.Failed>(failed)
        assertEquals(TeleportError.RemoteCancelled, failed.error)
    }

    @Test
    fun receiver_userCancelled() = runTest {
        val transport = FakeTransport()
        val store = FakeStore()
        val session = TeleportReceiverSession(transport, store, FakeNames, backgroundScope)

        session.start()
        transport.emit(TransportEvent.Connected(peer1, "4321"))
        runCurrent()

        session.cancel()
        runCurrent()

        assertEquals(ReceiverState.Cancelled, session.state.value)
        assertTrue(transport.sent.any { it is TeleportMessage.Cancel })
    }
}
