package com.o3.vanguard.visionary.feature.teleport.domain.model

data class PhotoRef(
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
)

data class TeleportPeer(val id: String, val name: String)

enum class TeleportError {
    NoReceiverFound,
    ConnectionFailed,
    Disconnected,
    TimedOut,
    RemoteCancelled,
    TransferFailed,
    SaveFailed,
}

sealed interface SenderState {
    data object Idle : SenderState
    data class Searching(val peers: List<TeleportPeer>) : SenderState
    data class Connecting(val peer: TeleportPeer) : SenderState

    /** Gesture is enabled in this state. */
    data class Connected(
        val peer: TeleportPeer,
        val pairingCode: String,
        val transferProgress: Float,
    ) : SenderState

    data class Released(val peer: TeleportPeer, val transferProgress: Float) : SenderState
    data object Delivered : SenderState
    data class Failed(val error: TeleportError) : SenderState
    data object Cancelled : SenderState
}

val SenderState.isTerminal: Boolean
    get() = (this is SenderState.Delivered) || (this is SenderState.Failed) || (this is SenderState.Cancelled)

sealed interface ReceiverState {
    data object Idle : ReceiverState
    data object Waiting : ReceiverState
    data class Connected(val peer: TeleportPeer, val pairingCode: String) : ReceiverState

    /** Gesture is enabled while [userDropped] is false. */
    data class Incoming(
        val peer: TeleportPeer,
        val pairingCode: String,
        val fileName: String,
        val thumbnailPath: String?,
        val transferProgress: Float,
        val senderReleased: Boolean,
        val userDropped: Boolean,
    ) : ReceiverState

    data class Committed(val savedUri: String) : ReceiverState
    data class Failed(val error: TeleportError) : ReceiverState
    data object Cancelled : ReceiverState
}

val ReceiverState.isTerminal: Boolean
    get() = (this is ReceiverState.Committed) || (this is ReceiverState.Failed) || (this is ReceiverState.Cancelled)
