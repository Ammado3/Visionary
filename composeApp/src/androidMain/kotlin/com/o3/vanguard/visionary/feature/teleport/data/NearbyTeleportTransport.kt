package com.o3.vanguard.visionary.feature.teleport.data

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.o3.vanguard.visionary.feature.teleport.domain.model.TeleportPeer
import com.o3.vanguard.visionary.feature.teleport.domain.protocol.TeleportCodec
import com.o3.vanguard.visionary.feature.teleport.domain.protocol.TeleportMessage
import com.o3.vanguard.visionary.feature.teleport.domain.transport.TeleportTransport
import com.o3.vanguard.visionary.feature.teleport.domain.transport.TransportEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class NearbyTeleportTransport(private val context: Context) : TeleportTransport {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client: ConnectionsClient = Nearby.getConnectionsClient(context)

    private val _peers = MutableStateFlow<List<TeleportPeer>>(emptyList())
    override val peers: StateFlow<List<TeleportPeer>> = _peers.asStateFlow()

    private val _events = MutableSharedFlow<TransportEvent>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val events: SharedFlow<TransportEvent> = _events.asSharedFlow()

    @Volatile private var connectedEndpointId: String? = null
    private val pendingInfo = ConcurrentHashMap<String, ConnectionInfo>()
    private val prepared = ConcurrentHashMap<Long, Payload>()
    private val incomingFiles = ConcurrentHashMap<Long, Payload>()
    private val outgoingIds = ConcurrentHashMap.newKeySet<Long>()
    private val lastPercent = ConcurrentHashMap<Long, Int>()

    override suspend fun startAdvertising(deviceName: String) {
        client.startAdvertising(
            deviceName,
            SERVICE_ID,
            lifecycleCallback,
            AdvertisingOptions.Builder().setStrategy(STRATEGY).build(),
        ).await()
    }

    override suspend fun startDiscovery() {
        client.startDiscovery(
            SERVICE_ID,
            discoveryCallback,
            DiscoveryOptions.Builder().setStrategy(STRATEGY).build(),
        ).await()
    }

    override suspend fun connect(peerId: String, localName: String) {
        client.requestConnection(localName, peerId, lifecycleCallback).await()
    }

    override suspend fun sendMessage(message: TeleportMessage) {
        val endpoint = connectedEndpointId ?: error("Not connected")
        client.sendPayload(endpoint, Payload.fromBytes(TeleportCodec.encode(message))).await()
    }

    override suspend fun prepareFile(source: String): Long = withContext(Dispatchers.IO) {
        val payload = if (source.startsWith("content://")) {
            val pfd = context.contentResolver.openFileDescriptor(Uri.parse(source), "r")
                ?: error("Cannot open $source")
            Payload.fromFile(pfd)
        } else {
            Payload.fromFile(File(source))
        }
        prepared[payload.id] = payload
        payload.id
    }

    override suspend fun sendPrepared(payloadId: Long) {
        val endpoint = connectedEndpointId ?: error("Not connected")
        val payload = prepared.remove(payloadId) ?: error("Unknown payload $payloadId")
        outgoingIds.add(payloadId)
        client.sendPayload(endpoint, payload).await()
    }

    override fun shutdown() {
        runCatching { client.stopAdvertising() }
        runCatching { client.stopDiscovery() }
        runCatching { client.stopAllEndpoints() }
        prepared.values.forEach { runCatching { it.asFile()?.asParcelFileDescriptor()?.close() } }
        prepared.clear()
        incomingFiles.clear()
        outgoingIds.clear()
        connectedEndpointId = null
        _peers.value = emptyList()
        File(context.cacheDir, STAGING_DIR).deleteRecursively()
        scope.cancel()
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            _peers.update { list -> list.filterNot { it.id == endpointId } + TeleportPeer(endpointId, info.endpointName) }
        }

        override fun onEndpointLost(endpointId: String) {
            _peers.update { list -> list.filterNot { it.id == endpointId } }
        }
    }

    private val lifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            pendingInfo[endpointId] = info
            client.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            val info = pendingInfo.remove(endpointId)
            if (result.status.isSuccess) {
                connectedEndpointId = endpointId
                runCatching { client.stopAdvertising() }
                runCatching { client.stopDiscovery() }
                _events.tryEmit(
                    TransportEvent.Connected(
                        TeleportPeer(endpointId, info?.endpointName ?: "Device"),
                        info?.authenticationDigits.orEmpty(),
                    ),
                )
            } else {
                _events.tryEmit(
                    TransportEvent.ConnectionFailed(endpointId, result.status.statusMessage ?: "status ${result.status.statusCode}"),
                )
            }
        }

        override fun onDisconnected(endpointId: String) {
            if (connectedEndpointId == endpointId) connectedEndpointId = null
            _events.tryEmit(TransportEvent.Disconnected(endpointId))
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> payload.asBytes()
                    ?.let(TeleportCodec::decode)
                    ?.let { _events.tryEmit(TransportEvent.MessageReceived(it)) }
                Payload.Type.FILE -> incomingFiles[payload.id] = payload
                else -> Unit
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            val id = update.payloadId
            val outgoing = id in outgoingIds
            val tracked = outgoing || incomingFiles.containsKey(id)
            when (update.status) {
                PayloadTransferUpdate.Status.IN_PROGRESS -> if (tracked) emitProgress(id, update, outgoing)
                PayloadTransferUpdate.Status.SUCCESS -> onSuccess(id, outgoing)
                PayloadTransferUpdate.Status.FAILURE, PayloadTransferUpdate.Status.CANCELED ->
                    if (tracked) _events.tryEmit(TransportEvent.FileFailed(id, outgoing))
            }
        }
    }

    private fun emitProgress(id: Long, update: PayloadTransferUpdate, outgoing: Boolean) {
        val total = update.totalBytes
        if (total <= 0) return
        val percent = (update.bytesTransferred * PERCENT / total).toInt()
        if (lastPercent.put(id, percent) != percent) {
            _events.tryEmit(TransportEvent.FileProgress(id, update.bytesTransferred, total, outgoing))
        }
    }

    private fun onSuccess(id: Long, outgoing: Boolean) {
        if (outgoing) {
            outgoingIds.remove(id)
            _events.tryEmit(TransportEvent.FileCompleted(id, outgoing = true, stagedPath = null))
            return
        }
        val payload = incomingFiles.remove(id) ?: return
        scope.launch {
            try {
                val path = stage(payload)
                _events.tryEmit(TransportEvent.FileCompleted(id, outgoing = false, stagedPath = path))
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                _events.tryEmit(TransportEvent.FileFailed(id, outgoing = false))
            }
        }
    }

    private fun stage(payload: Payload): String {
        val file = payload.asFile() ?: error("Not a file payload")
        val dir = File(context.cacheDir, STAGING_DIR).apply { mkdirs() }
        val target = File(dir, "${payload.id}.bin")
        val pfd = file.asParcelFileDescriptor()
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
            target.outputStream().use { out -> input.copyTo(out) }
        }
        runCatching { file.asUri()?.let { context.contentResolver.delete(it, null, null) } }
        @Suppress("DEPRECATION")
        runCatching { file.asJavaFile()?.delete() }
        return target.absolutePath
    }

    private companion object {
        const val SERVICE_ID = "com.o3.vanguard.visionary.airteleport"
        const val STAGING_DIR = "teleport"
        const val PERCENT = 100L
        val STRATEGY: Strategy = Strategy.P2P_POINT_TO_POINT
    }
}
