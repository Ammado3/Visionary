package com.o3.vanguard.visionary.feature.teleport.domain.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed interface TeleportMessage {
    @Serializable @SerialName("offer")
    data class Offer(
        val transferId: String,
        val fileName: String,
        val mimeType: String,
        val sizeBytes: Long,
        val thumbnailPayloadId: Long,
        val filePayloadId: Long,
    ) : TeleportMessage

    @Serializable @SerialName("released")
    data class Released(val transferId: String) : TeleportMessage

    @Serializable @SerialName("committed")
    data class Committed(val transferId: String) : TeleportMessage

    @Serializable @SerialName("cancel")
    data class Cancel(val transferId: String?) : TeleportMessage
}

object TeleportCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(message: TeleportMessage): ByteArray =
        json.encodeToString(TeleportMessage.serializer(), message).encodeToByteArray()

    /** Returns null for malformed input; never throws. */
    fun decode(bytes: ByteArray): TeleportMessage? =
        runCatching { json.decodeFromString(TeleportMessage.serializer(), bytes.decodeToString()) }.getOrNull()
}
