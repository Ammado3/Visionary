package com.o3.vanguard.visionary.feature.teleport

import com.o3.vanguard.visionary.feature.teleport.domain.protocol.TeleportCodec
import com.o3.vanguard.visionary.feature.teleport.domain.protocol.TeleportMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TeleportCodecTest {

    @Test
    fun offerMessage_roundTrip() {
        val offer = TeleportMessage.Offer(
            transferId = "abc12345",
            fileName = "IMG_001.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 4096L,
            thumbnailPayloadId = 12L,
            filePayloadId = 34L,
        )
        val encoded = TeleportCodec.encode(offer)
        val decoded = TeleportCodec.decode(encoded)
        assertEquals(offer, decoded)
    }

    @Test
    fun releasedMessage_roundTrip() {
        val msg = TeleportMessage.Released("trans-99")
        val encoded = TeleportCodec.encode(msg)
        val decoded = TeleportCodec.decode(encoded)
        assertEquals(msg, decoded)
    }

    @Test
    fun committedMessage_roundTrip() {
        val msg = TeleportMessage.Committed("trans-99")
        val encoded = TeleportCodec.encode(msg)
        val decoded = TeleportCodec.decode(encoded)
        assertEquals(msg, decoded)
    }

    @Test
    fun cancelMessage_roundTrip() {
        val msg = TeleportMessage.Cancel("trans-99")
        val encoded = TeleportCodec.encode(msg)
        val decoded = TeleportCodec.decode(encoded)
        assertEquals(msg, decoded)
    }

    @Test
    fun invalidJson_returnsNull() {
        val garbage = "not a valid json string".encodeToByteArray()
        val decoded = TeleportCodec.decode(garbage)
        assertNull(decoded)
    }
}
