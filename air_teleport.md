# Air Teleport: Implementation Guide (Android, Nearby Connections)

Feature for **Visionary** (KMP + CMP). Base package: `com.o3.vanguard.visionary`. Feature package: `com.o3.vanguard.visionary.feature.teleport`.

---

## 0. AGENT OPERATING PROTOCOL (READ FIRST, FOLLOW STRICTLY)

1. Execute **exactly one step at a time**, in order. Never start the next step until the user replies "yes" / "next" / "start".
2. Before writing code for a step, state its goal in one sentence.
3. After finishing a step, run (adapt task names to the project): `./gradlew ktlintCheck detekt`, the unit tests of the touched modules, and `assembleDebug`. Warnings fail the build in this project, so fix every warning. Do not suppress rules unless a step explicitly allows it.
4. Then report in **at most 8 lines**: what was done, files created/changed, any deviation from this guide (and why), and verification results (build/lint/tests).
5. End every report with exactly this question and **STOP**:
   `Step N is complete. Start Step N+1: <title>? (yes / no / adjust)`
6. Follow the repo's `docs/HANDBOOK.md` and the AI rules file for naming, architecture and style. If they conflict with this guide, the handbook wins; mention it in the report.
7. Code in `commonMain` must not import Android APIs. All Android code lives in `androidMain`. `iosMain` gets stubs only (iOS is out of scope for now but the project must still compile).
8. The code in this guide is a **reference implementation**. If an API signature differs in the versions the project uses, fix it by checking the official docs. Do not invent APIs. Report such fixes.
9. Do not touch unrelated code. Do not add libraries beyond those listed without asking.
10. UI strings in this guide are literals for brevity. Move them into the project's string resources (English-only).

---

## Feature summary (context for the agent)

Two Android phones, both with Bluetooth and Wi-Fi on, both running Visionary.

**Device A (sender)**
1. Select a photo in the gallery, tap **Share**, pick **Air Teleport** in the share sheet.
2. The photo zooms out and the screen says "Ready to share".
3. In the background the app discovers and connects to the receiver, then sends a small thumbnail and the full photo.
4. The user does a **3-finger pinch ("grab")**. The photo fades and scales out as if leaving the screen, driven continuously by the gesture. If released early it snaps back.

**Device B (receiver)**
1. The user opens **Air Teleport Receiver** (home screen entry). The device advertises itself.
2. When A's photo is incoming, B shows the thumbnail faintly.
3. The user does a **3-finger spread ("drop")**. The photo materializes, driven by the gesture. When the file is fully received **and** A has grabbed, the photo is saved to the gallery.

The connection and transfer happen while the user is doing the gestures, so the finish feels instant.

**Assumption:** the "share button in the gallery" opens an **in-app share sheet** where an **Air Teleport** item can be added. If the app uses the system chooser instead, create an in-app bottom sheet with "Air Teleport" and "More…" (which launches the system chooser).

### Protocol overview

```
A (sender)                                     B (receiver)
discover ─────────────── BLE/Nearby ─────────▶ advertise
connect  ◀──────── auto Wi-Fi upgrade ────────▶
Offer(transferId, name, mime, size, thumbPayloadId, filePayloadId) ─▶
thumbnail FILE payload ─────────────────────────▶ (shown on B)
full photo FILE payload ────────────────────────▶ (staged in B's cache)
[user grabs]  Released(transferId) ─────────────▶
                                    [user drops] (B commits only when file complete AND Released AND dropped)
Committed(transferId) ◀─────────────────────────  (saved to MediaStore)
```

### Target file tree

```
commonMain/.../feature/teleport/
  domain/
    model/TeleportModels.kt
    protocol/TeleportMessage.kt
    transport/TeleportTransport.kt
    port/Ports.kt
    session/TeleportConfig.kt
    session/TeleportSenderSession.kt
    session/TeleportReceiverSession.kt
  gesture/SpreadTracker.kt
  gesture/TeleportGestureModifier.kt
  platform/TeleportGate.kt            (expect)
  presentation/TeleportSenderViewModel.kt
  presentation/TeleportReceiverViewModel.kt
  presentation/ui/TeleportStage.kt
  presentation/ui/TeleportSenderScreen.kt
  presentation/ui/TeleportReceiverScreen.kt
  di/TeleportModule.kt
androidMain/.../feature/teleport/
  data/NearbyTeleportTransport.kt
  data/AndroidThumbnailMaker.kt
  data/MediaStoreReceivedPhotoStore.kt
  data/AndroidDeviceNameProvider.kt
  platform/AndroidReadiness.kt
  platform/TeleportGate.android.kt
  di/TeleportPlatformModule.android.kt
iosMain/.../feature/teleport/
  platform/TeleportGate.ios.kt
  data/UnsupportedTeleportTransport.kt
  di/TeleportPlatformModule.ios.kt
commonTest/.../feature/teleport/ (tests + FakeTransport)
```

---

## STEP 0: Recon (NO production code)

**Goal:** learn the project's conventions so later steps match them.

Inspect and summarize:
1. Module layout (`composeApp`? `shared`?), where `commonMain`, `androidMain`, `iosMain`, `commonTest` live, and the real manifest path for Android.
2. Gradle: version catalog file, Kotlin version, whether `kotlinx-serialization`, `kotlinx-coroutines-play-services`, `kotlinx-coroutines-test` already exist.
3. DI: how Koin modules are declared and loaded (per-feature modules? `expect`/`actual` modules?). How ViewModels are provided (`koinViewModel`?).
4. Navigation 3: route/key declaration style, where the `entryProvider` is, how screens get arguments.
5. Image loading library used for Compose (Coil 3?). Name the composable and model types used.
6. Gallery: where the in-app share UI/sheet is, what model represents a photo (URI, name, mime, size), and the home screen entries list.
7. How photos are read and indexed (MediaStore observer? Room index?). After a new photo is written to MediaStore, what triggers the gallery to refresh.
8. Existing runtime-permission flow for media access.
9. Exact Gradle task names for ktlint, detekt, unit tests, and how the 80% coverage floor is configured (which source sets count).
10. How strings/resources are managed.

**Output:** a recon report of at most 15 lines. Do not change any file.
Then ask: `Step 0 is complete. Start Step 1: Dependencies and manifest? (yes / no / adjust)`

---

## STEP 1: Dependencies and manifest

**Goal:** add libraries, plugin and permissions so the project builds with the new feature skeleton.

### 1.1 Version catalog (reuse existing entries if present; use latest stable versions compatible with the project's Kotlin)

```toml
[versions]
playServicesNearby = "19.3.0"          # indicative: use latest stable
kotlinxSerialization = "1.7.3"         # indicative
# kotlinx-coroutines version: reuse the project's existing "coroutines" version ref

[libraries]
play-services-nearby = { module = "com.google.android.gms:play-services-nearby", version.ref = "playServicesNearby" }
kotlinx-coroutines-play-services = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-play-services", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }

[plugins]
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

### 1.2 Module build file

```kotlin
plugins { alias(libs.plugins.kotlinSerialization) }

kotlin {
    sourceSets {
        commonMain.dependencies { implementation(libs.kotlinx.serialization.json) }
        androidMain.dependencies {
            implementation(libs.play.services.nearby)
            implementation(libs.kotlinx.coroutines.play.services)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
```

### 1.3 Android manifest (merge into the app's real manifest)

```xml
<!-- Nearby Connections -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" android:maxSdkVersion="28" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" android:minSdkVersion="31" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" android:minSdkVersion="31" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:minSdkVersion="31" />
<uses-permission
    android:name="android.permission.NEARBY_WIFI_DEVICES"
    android:usesPermissionFlags="neverForLocation"
    android:minSdkVersion="32" />
<!-- Saving received photos on API 26-28 only -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />

<uses-feature android:name="android.hardware.bluetooth_le" android:required="false" />
```

If the manifest merger rejects an attribute for the project's AGP version, adapt per the official Nearby Connections docs and report it.

**Verify:** the project syncs and builds. Nothing else changes.
**Report, then ask:** `Step 1 is complete. Start Step 2: Domain contracts? (yes / no / adjust)`

---

## STEP 2: Domain contracts (commonMain, no logic yet)

**Goal:** define models, wire protocol, transport interface and ports.

### 2.1 `domain/model/TeleportModels.kt`

```kotlin
package com.o3.vanguard.visionary.feature.teleport.domain.model

data class PhotoRef(
    val uri: String,          // content:// URI string
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
    get() = this is SenderState.Delivered || this is SenderState.Failed || this is SenderState.Cancelled

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
    get() = this is ReceiverState.Committed || this is ReceiverState.Failed || this is ReceiverState.Cancelled
```

### 2.2 `domain/protocol/TeleportMessage.kt`

```kotlin
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
```

Control messages are tiny JSON sent as Nearby `BYTES` payloads (hard limit 32 KB). Images and thumbnails always go as `FILE` payloads.

### 2.3 `domain/transport/TeleportTransport.kt`

```kotlin
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
```

### 2.4 `domain/port/Ports.kt`

```kotlin
package com.o3.vanguard.visionary.feature.teleport.domain.port

interface ThumbnailMaker {
    /** Creates a small JPEG (max ~320px) in app cache and returns its absolute path. */
    suspend fun make(photoUri: String): String
}

interface ReceivedPhotoStore {
    /** Moves/copies the staged file into the device gallery. Returns the saved content URI string. */
    suspend fun save(stagedPath: String, fileName: String, mimeType: String): String
}

interface DeviceNameProvider {
    fun deviceName(): String
}

interface TeleportAvailability {
    val isSupported: Boolean
}
```

### 2.5 `domain/session/TeleportConfig.kt`

```kotlin
package com.o3.vanguard.visionary.feature.teleport.domain.session

data class TeleportConfig(
    val autoConnectDelayMs: Long = 1_200,
    val searchTimeoutMs: Long = 45_000,
    val transferTimeoutMs: Long = 120_000,
    val receiverIdleTimeoutMs: Long = 180_000,
    val shutdownGraceMs: Long = 600,
)
```

**Verify:** compiles for all targets (add temporary no-op code only if the compiler demands it).
**Report, then ask:** `Step 2 is complete. Start Step 3: Sender and receiver session logic with tests? (yes / no / adjust)`

---

## STEP 3: Session state machines + unit tests (commonMain, pure Kotlin)

**Goal:** all behavior lives in platform-free, unit-tested classes. ViewModels (Step 7) only wrap them.

Important rules: collectors are launched with `CoroutineStart.UNDISPATCHED` so they are subscribed before the transport can emit; the transport is shut down on every terminal state; every state change goes through `MutableStateFlow`.

### 3.1 `domain/session/TeleportSenderSession.kt`

```kotlin
package com.o3.vanguard.visionary.feature.teleport.domain.session

import com.o3.vanguard.visionary.feature.teleport.domain.model.*
import com.o3.vanguard.visionary.feature.teleport.domain.port.DeviceNameProvider
import com.o3.vanguard.visionary.feature.teleport.domain.port.ThumbnailMaker
import com.o3.vanguard.visionary.feature.teleport.domain.protocol.TeleportMessage
import com.o3.vanguard.visionary.feature.teleport.domain.transport.TeleportTransport
import com.o3.vanguard.visionary.feature.teleport.domain.transport.TransportEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
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
            transport.sendPrepared(thumbId) // small first, so B can show a preview immediately
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
            // intentionally ignored: best-effort signalling
        }
    }

    private companion object {
        const val NO_ID = -1L
        const val HEX = 16
    }
}
```

### 3.2 `domain/session/TeleportReceiverSession.kt`

Same style and rules as the sender. Behavior spec:

- `start()`: state `Waiting`, subscribe (UNDISPATCHED) to `transport.events`, call `transport.startAdvertising(deviceNames.deviceName())`. On failure: `Failed(ConnectionFailed)`. Arm `receiverIdleTimeoutMs` timeout → `Failed(TimedOut)`.
- `TransportEvent.Connected` → store `peer` + `pairingCode`, state `Connected`, re-arm the timeout with `transferTimeoutMs`.
- `MessageReceived(Offer)` → store the offer, `render()`.
- `MessageReceived(Released)` → `released = true`, `render()`, `tryCommit()`. (Released may arrive before the Offer is processed, so keep it as a flag, not tied to the offer.)
- `MessageReceived(Cancel)` → `Failed(RemoteCancelled)`.
- `FileProgress(incoming)` → `progress[payloadId]`, `render()`.
- `FileCompleted(incoming, stagedPath)` → `completed[payloadId] = stagedPath`, `progress[payloadId] = 1f`, `render()`, `tryCommit()`. (Keep a map because the thumbnail or file may complete **before** the Offer arrives.)
- `FileFailed(incoming)` → if `payloadId == offer.filePayloadId` → `Failed(TransferFailed)`; thumbnail failure is ignored.
- `Disconnected` → if NOT (file completed AND released) → `Failed(Disconnected)`; otherwise ignore (the user can still finish the drop).
- `onDropCommitted()` (called by the UI when the spread gesture completes): only valid in `Incoming`; set `dropped = true`, `render()`, `tryCommit()`.
- `render()`: needs `offer` and `peer`; emits `Incoming(peer, pairingCode, offer.fileName, completed[offer.thumbnailPayloadId], progress[offer.filePayloadId] ?: 0f, released, dropped)`. No-op if terminal.
- `tryCommit()`: requires `offer != null`, `completed[offer.filePayloadId] != null`, `dropped`, `released`, and not already committing. Then (in `scope.launch`): `uri = store.save(path, offer.fileName, offer.mimeType)`; best-effort `sendMessage(Committed(offer.transferId))`; `finish(Committed(uri), graceful = true)`. On save failure → `Failed(SaveFailed)`.
- `finish(terminal, graceful)`: if terminal already → return. Set state. If `graceful`, `scope.launch { delay(config.shutdownGraceMs); transport.shutdown() }` so the `Committed` message can be delivered before the link closes; otherwise `transport.shutdown()` immediately. Cancel timeout and collectors.
- `cancel()`: best-effort `Cancel(offer?.transferId)` if connected, then `finish(Cancelled)`.
- `dispose()`: synchronous `transport.shutdown()` + cancel jobs (for `ViewModel.onCleared`).

Constructor: `TeleportReceiverSession(transport, store: ReceivedPhotoStore, deviceNames: DeviceNameProvider, scope: CoroutineScope, config: TeleportConfig = TeleportConfig())`.

### 3.3 Tests (`commonTest`)

`FakeTransport`:

```kotlin
class FakeTransport : TeleportTransport {
    override val peers = MutableStateFlow<List<TeleportPeer>>(emptyList())
    private val _events = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 64)
    override val events = _events.asSharedFlow()
    val sent = mutableListOf<TeleportMessage>()
    val sentPayloads = mutableListOf<Long>()
    var shutdownCalled = false
    private var nextId = 1L

    override suspend fun startAdvertising(deviceName: String) = Unit
    override suspend fun startDiscovery() = Unit
    override suspend fun connect(peerId: String, localName: String) = Unit
    override suspend fun sendMessage(message: TeleportMessage) { sent += message }
    override suspend fun prepareFile(source: String): Long = nextId++
    override suspend fun sendPrepared(payloadId: Long) { sentPayloads += payloadId }
    override fun shutdown() { shutdownCalled = true }
    suspend fun emit(event: TransportEvent) = _events.emit(event)
}
```

Sample test (sender happy path), use `runTest` and `backgroundScope` as the session scope:

```kotlin
@Test
fun sender_happyPath() = runTest {
    val t = FakeTransport()
    val s = TeleportSenderSession(t, FakeThumbs, FakeNames, backgroundScope)
    val peer = TeleportPeer("e1", "Pixel")
    s.start(photo)
    t.peers.value = listOf(peer)
    advanceTimeBy(1_300); runCurrent()                       // auto-connect after delay
    t.emit(TransportEvent.Connected(peer, "1234")); runCurrent()
    assertIs<SenderState.Connected>(s.state.value)
    assertIs<TeleportMessage.Offer>(t.sent.first())
    s.onGrabCommitted(); runCurrent()
    assertIs<SenderState.Released>(s.state.value)
    val id = (t.sent.first() as TeleportMessage.Offer).transferId
    t.emit(TransportEvent.MessageReceived(TeleportMessage.Committed(id))); runCurrent()
    assertEquals(SenderState.Delivered, s.state.value)
    assertTrue(t.shutdownCalled)
}
```

**Required test cases (to reach ≥ 80% on domain):**
- Sender: no receiver found (timeout), connect failure, manual connect with 2 peers (no auto-connect), progress updates, remote cancel, disconnect, user cancel sends `Cancel`, grab ignored before connected, transfer failure.
- Receiver: commit **only** when file complete AND released AND dropped (test all 3 orders), thumbnail completes before offer, released before offer, save failure → `SaveFailed`, disconnect before/after file completion, remote cancel, graceful shutdown delay, idle timeout.
- `TeleportCodec`: round-trip for each message; `decode` of garbage returns `null`.

**Verify:** all tests green, coverage on `domain` ≥ 80%, no warnings.
**Report, then ask:** `Step 3 is complete. Start Step 4: Android Nearby transport and platform adapters? (yes / no / adjust)`

---

## STEP 4: Android transport and adapters (androidMain)

**Goal:** implement `TeleportTransport` on Google Nearby Connections, plus thumbnails, gallery saving and device name.

Design rules:
- `Strategy.P2P_POINT_TO_POINT` (1-to-1, highest bandwidth). Nearby upgrades the medium (BLE → Bluetooth/Wi-Fi LAN/Wi-Fi Direct/hotspot) automatically.
- Connections are auto-accepted. The receiver only advertises while the Receiver screen is open, the sender connects only to a peer it chose, and the receiver never saves anything until the user finishes the drop gesture. Both sides get the same 4-digit `pairingCode` (shown in the UI).
- Progress events are throttled to 1% steps. Received files are copied from Nearby's temp location into `cacheDir/teleport/` and Nearby's copy is deleted.
- One transport instance = one session, with its own internal scope cancelled in `shutdown()`.

### 4.1 `data/NearbyTeleportTransport.kt`

```kotlin
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
                runCatching { client.stopAdvertising() }   // free the radio, improves bandwidth
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

    /** Copies Nearby's received file into our private staging dir and removes Nearby's own copy. */
    private fun stage(payload: Payload): String {
        val file = payload.asFile() ?: error("Not a file payload")
        val dir = File(context.cacheDir, STAGING_DIR).apply { mkdirs() }
        val target = File(dir, "${payload.id}.bin")
        ParcelFileDescriptor.AutoCloseInputStream(file.asParcelFileDescriptor()).use { input ->
            target.outputStream().use { out -> input.copyTo(out) }
        }
        runCatching { file.asUri()?.let { context.contentResolver.delete(it, null, null) } }
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
```

### 4.2 `data/AndroidThumbnailMaker.kt`

Spec (write it yourself, following project style):
- Implements `ThumbnailMaker`. `withContext(Dispatchers.IO)`.
- API 28+: `ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))` with `setTargetSize` so the longest side is ≤ 320 px and `allocator = ALLOCATOR_SOFTWARE`. (This handles EXIF rotation.)
- API 26–27: `BitmapFactory` two-pass decode (`inJustDecodeBounds`, then `inSampleSize`), then scale to ≤ 320 px.
- Compress JPEG quality 70 to `cacheDir/teleport/thumb_<millis>.jpg`, recycle bitmaps, return the absolute path.
- Extract the pure math (target size calculation, sample size) into a small testable function.

### 4.3 `data/MediaStoreReceivedPhotoStore.kt`

Spec:
- Implements `ReceivedPhotoStore`, `withContext(Dispatchers.IO)`.
- **API 29+:** `ContentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)` with `DISPLAY_NAME`, `MIME_TYPE` (fall back to `image/jpeg` if the mime isn't `image/*`), `RELATIVE_PATH = Pictures/Visionary`, `IS_PENDING = 1`. Stream the staged file into `openOutputStream(uri)`, then set `IS_PENDING = 0`. If anything throws, delete the inserted row and rethrow. Return `uri.toString()`.
- **API 26–28:** copy to `Pictures/Visionary/` (create dir, unique file name), then `MediaScannerConnection.scanFile`. Return `Uri.fromFile(target).toString()`. (`@Suppress("DEPRECATION")` is allowed on this branch only.)
- Sanitize the file name (replace `\ / : * ? " < > |` with `_`); fall back to `teleport_<millis>.jpg` if blank.
- Delete the staged file after a successful save.
- Extract name sanitizing and unique-name logic into pure functions and unit test them.

### 4.4 `data/AndroidDeviceNameProvider.kt`

```kotlin
class AndroidDeviceNameProvider : DeviceNameProvider {
    override fun deviceName(): String = Build.MODEL.take(MAX_LEN).ifBlank { "Android" }
    private companion object { const val MAX_LEN = 32 }
}
```

**Verify:** compiles, lint/detekt clean, unit tests for the extracted pure functions pass. Coverage: keep the Nearby glue thin and push logic into pure functions. If the coverage config counts androidMain adapters and the floor cannot be met, ask the user before excluding anything.
**Report, then ask:** `Step 4 is complete. Start Step 5: Readiness checks and runtime permissions? (yes / no / adjust)`

---

## STEP 5: Readiness checks and runtime permissions

**Goal:** before opening the sender/receiver screens, make sure permissions, Bluetooth, Wi-Fi, location (old Android) and Play Services are OK, with a clean UX.

### 5.1 `commonMain/.../platform/TeleportGate.kt`

```kotlin
package com.o3.vanguard.visionary.feature.teleport.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

sealed interface ReadinessResult {
    data object Ready : ReadinessResult
    data class MissingPermissions(val permissions: List<String>) : ReadinessResult
    data object BluetoothDisabled : ReadinessResult
    data object WifiDisabled : ReadinessResult
    data object LocationDisabled : ReadinessResult      // only needed on Android <= 11
    data object PlayServicesUnavailable : ReadinessResult
    data object Unsupported : ReadinessResult
}

@Stable
interface TeleportGate {
    /** Requests missing permissions if needed, then calls [onReady] only if everything is OK. */
    fun ensureReady(onReady: () -> Unit)
    fun openSettingsFor(result: ReadinessResult)
}

@Composable
expect fun rememberTeleportGate(onBlocked: (ReadinessResult) -> Unit): TeleportGate
```

### 5.2 `androidMain/.../platform/AndroidReadiness.kt`

```kotlin
internal object AndroidReadiness {

    fun requiredPermissions(sdk: Int = Build.VERSION.SDK_INT): List<String> = buildList {
        when {
            sdk >= Build.VERSION_CODES.TIRAMISU -> {
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
            sdk >= Build.VERSION_CODES.S -> {
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            sdk >= Build.VERSION_CODES.Q -> add(Manifest.permission.ACCESS_FINE_LOCATION)
            else -> {
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    fun check(context: Context): ReadinessResult {
        val missing = requiredPermissions().filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) return ReadinessResult.MissingPermissions(missing)

        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS) {
            return ReadinessResult.PlayServicesUnavailable
        }
        val bluetooth = context.getSystemService(BluetoothManager::class.java)?.adapter
        if (bluetooth == null || !bluetooth.isEnabled) return ReadinessResult.BluetoothDisabled

        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        if (wifi == null || !wifi.isWifiEnabled) return ReadinessResult.WifiDisabled

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val location = context.getSystemService(LocationManager::class.java)
            if (location == null || !LocationManagerCompat.isLocationEnabled(location)) return ReadinessResult.LocationDisabled
        }
        return ReadinessResult.Ready
    }
}
```

(`requiredPermissions(sdk)` takes the SDK as a parameter so it can be unit tested for each API range.)

### 5.3 `androidMain/.../platform/TeleportGate.android.kt`

```kotlin
@Composable
actual fun rememberTeleportGate(onBlocked: (ReadinessResult) -> Unit): TeleportGate {
    val context = LocalContext.current
    val currentOnBlocked by rememberUpdatedState(onBlocked)
    var pendingOnReady by remember { mutableStateOf<(() -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val callback = pendingOnReady
        pendingOnReady = null
        val result = AndroidReadiness.check(context)
        if (result is ReadinessResult.Ready) callback?.invoke() else currentOnBlocked(result)
    }

    return remember(context, launcher) {
        object : TeleportGate {
            override fun ensureReady(onReady: () -> Unit) {
                when (val result = AndroidReadiness.check(context)) {
                    ReadinessResult.Ready -> onReady()
                    is ReadinessResult.MissingPermissions -> {
                        pendingOnReady = onReady
                        launcher.launch(result.permissions.toTypedArray())
                    }
                    else -> currentOnBlocked(result)
                }
            }

            override fun openSettingsFor(result: ReadinessResult) {
                val action = when (result) {
                    ReadinessResult.BluetoothDisabled -> Settings.ACTION_BLUETOOTH_SETTINGS
                    ReadinessResult.WifiDisabled -> Settings.ACTION_WIFI_SETTINGS
                    ReadinessResult.LocationDisabled -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
                    is ReadinessResult.MissingPermissions -> Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    else -> return
                }
                val intent = Intent(action).apply {
                    if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }
}
```

### 5.4 iOS stub (`iosMain/.../platform/TeleportGate.ios.kt`)

```kotlin
@Composable
actual fun rememberTeleportGate(onBlocked: (ReadinessResult) -> Unit): TeleportGate = remember {
    object : TeleportGate {
        override fun ensureReady(onReady: () -> Unit) = onBlocked(ReadinessResult.Unsupported)
        override fun openSettingsFor(result: ReadinessResult) = Unit
    }
}
```

### 5.5 Blocked dialog (commonMain)

Create `TeleportBlockedDialog(result, onDismiss, onOpenSettings)` that maps each `ReadinessResult` to a short English message and an action:
- MissingPermissions: "Air Teleport needs Nearby devices permission to find your other phone." → "Open settings"
- BluetoothDisabled: "Turn on Bluetooth." → "Open Bluetooth settings"
- WifiDisabled: "Turn on Wi-Fi (no internet needed)." → "Open Wi-Fi settings"
- LocationDisabled: "Turn on Location (required by Android 11 and below for device discovery)." → "Open Location settings"
- PlayServicesUnavailable: "Air Teleport needs Google Play services." → no action
- Unsupported: not shown (the entry points are hidden via `TeleportAvailability`).

**Verify:** unit test `requiredPermissions` for API 26, 28, 29, 31, 33, 34. Build and lint pass.
**Report, then ask:** `Step 5 is complete. Start Step 6: Gesture engine? (yes / no / adjust)`

---

## STEP 6: Gesture engine (commonMain Compose)

**Goal:** a reusable multi-finger gesture: **Pinch** ("grab", fingers close together) and **Spread** ("drop", fingers open). Pure math is separated and unit-tested.

### 6.1 `gesture/SpreadTracker.kt`

```kotlin
package com.o3.vanguard.visionary.feature.teleport.gesture

import androidx.compose.ui.geometry.Offset

enum class SpreadMode { Pinch, Spread }

/**
 * Progress 0..1 of a multi-finger pinch/spread. "Spread" of N points = mean distance to their centroid.
 *  Pinch:  fingers must start at least [minPinchStartPx] apart; progress hits 1 at [pinchEndRatio] of the start spread.
 *  Spread: progress hits 1 when spread reaches [spreadTargetPx].
 */
class SpreadTracker(
    private val mode: SpreadMode,
    private val minPinchStartPx: Float,
    private val spreadTargetPx: Float,
    private val pinchEndRatio: Float = 0.35f,
) {
    private var initialSpread = 0f

    /** Returns false when the gesture start is not valid (e.g. fingers too close for a pinch). */
    fun begin(points: List<Offset>): Boolean {
        initialSpread = spreadOf(points)
        return mode == SpreadMode.Spread || initialSpread >= minPinchStartPx
    }

    fun progress(points: List<Offset>): Float {
        val current = spreadOf(points)
        val raw = when (mode) {
            SpreadMode.Pinch -> (1f - current / initialSpread) / (1f - pinchEndRatio)
            SpreadMode.Spread -> (current - initialSpread) / (spreadTargetPx - initialSpread).coerceAtLeast(1f)
        }
        return raw.coerceIn(0f, 1f)
    }

    companion object {
        fun spreadOf(points: List<Offset>): Float {
            if (points.isEmpty()) return 0f
            val centroid = Offset(points.sumOf { it.x.toDouble() }.toFloat() / points.size, points.sumOf { it.y.toDouble() }.toFloat() / points.size)
            return points.map { (it - centroid).getDistance() }.average().toFloat()
        }
    }
}
```

### 6.2 `gesture/TeleportGestureModifier.kt`

```kotlin
@Composable
fun Modifier.teleportGesture(
    mode: SpreadMode,
    enabled: Boolean,
    onStart: () -> Unit,
    onProgress: (Float) -> Unit,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
    minPointers: Int = 3,
    commitThreshold: Float = 0.8f,
): Modifier {
    val start by rememberUpdatedState(onStart)
    val progress by rememberUpdatedState(onProgress)
    val commit by rememberUpdatedState(onCommit)
    val cancel by rememberUpdatedState(onCancel)
    if (!enabled) return this

    return pointerInput(mode, minPointers, commitThreshold) {
        val tracker = SpreadTracker(
            mode = mode,
            minPinchStartPx = 80.dp.toPx(),
            spreadTargetPx = 140.dp.toPx(),
        )
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var active = false
            var committed = false
            do {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.size >= minPointers) {
                    val points = pressed.map { it.position }
                    if (!active) {
                        active = tracker.begin(points)
                        if (active) start()
                    } else if (!committed) {
                        val p = tracker.progress(points)
                        progress(p)
                        if (p >= commitThreshold) {
                            committed = true
                            commit()
                        }
                    }
                    if (active) event.changes.forEach { it.consume() }
                }
            } while (event.changes.any { it.pressed })
            if (active && !committed) cancel()
        }
    }
}
```

Notes for the agent:
- Imports: `androidx.compose.foundation.gestures.awaitEachGesture`, `awaitFirstDown`, `androidx.compose.ui.input.pointer.pointerInput`.
- `minPointers = 3`. Many OEMs reserve 3-finger *swipes* (screenshots), but pinches are rarely intercepted. Make `minPointers` a single named constant so it is trivial to change after real-device testing (3 vs 4 vs 5).
- Do not add a second gesture detector on the same node.

### 6.3 Tests (`commonTest`)

- Triangle of 3 points at radius 200: `begin` is true for `Pinch`; `progress` is 0 at r = 200, ≈ 0.5 at r = 135, 1.0 at r = 70, clamped to 1 below that.
- `Pinch` with start radius 40 (< 80 dp equivalent): `begin` returns false.
- `Spread` from radius 40: progress is 0 at 40, 1 at ≥ target, monotonic in between.
- `spreadOf(emptyList())` is 0.

**Verify:** tests pass. (The Compose modifier itself is verified manually in Step 10.)
**Report, then ask:** `Step 6 is complete. Start Step 7: ViewModels and UI? (yes / no / adjust)`

---

## STEP 7: ViewModels and UI (commonMain)

**Goal:** sender and receiver screens with gesture-driven animations.

Rules:
- High-frequency gesture progress is **UI-local state** (`mutableFloatStateOf`), NOT part of the session state, to avoid recomposition storms and cross-device coupling.
- The ViewModels create sessions with `viewModelScope` and call `session.dispose()` in `onCleared()`.
- Use the project's image loader (Coil 3 shown below). Staged thumbnails are shown via `"file://$path"`.

### 7.1 ViewModels

```kotlin
class TeleportSenderViewModel(
    transport: TeleportTransport,
    thumbnails: ThumbnailMaker,
    deviceNames: DeviceNameProvider,
) : ViewModel() {
    private val session = TeleportSenderSession(transport, thumbnails, deviceNames, viewModelScope)
    val state: StateFlow<SenderState> = session.state

    fun start(photo: PhotoRef) = session.start(photo)
    fun connect(peer: TeleportPeer) = session.connect(peer)
    fun onGrabCommitted() = session.onGrabCommitted()
    fun cancel() = session.cancel()

    override fun onCleared() = session.dispose()
}

class TeleportReceiverViewModel(
    transport: TeleportTransport,
    store: ReceivedPhotoStore,
    deviceNames: DeviceNameProvider,
) : ViewModel() {
    private val session = TeleportReceiverSession(transport, store, deviceNames, viewModelScope)
    val state: StateFlow<ReceiverState> = session.state

    fun start() = session.start()
    fun onDropCommitted() = session.onDropCommitted()
    fun cancel() = session.cancel()

    override fun onCleared() = session.dispose()
}
```

### 7.2 `TeleportStage`: the shared visual

```kotlin
@Composable
fun TeleportStage(
    imageModel: Any?,
    progress: () -> Float,          // lambda: read in draw/layer phase, no recomposition per frame
    mode: SpreadMode,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val p = progress()
            val radius = size.minDimension * (0.35f + 0.5f * p)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.45f * p), Color.Transparent),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize(0.8f)
                .graphicsLayer {
                    val p = progress()
                    when (mode) {
                        // Sender: photo leaves the screen, growing toward the viewer while fading
                        SpreadMode.Pinch -> { alpha = 1f - p; scaleX = 1f + 0.18f * p }
                        // Receiver: photo materializes from the viewer side
                        SpreadMode.Spread -> { alpha = p; scaleX = 1.18f - 0.18f * p }
                    }
                    scaleY = scaleX
                },
        )
    }
}
```

### 7.3 `TeleportSenderScreen` (structure and behavior)

```kotlin
@Composable
fun TeleportSenderScreen(photo: PhotoRef, onClose: () -> Unit, viewModel: TeleportSenderViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var gesture by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(photo.uri) { viewModel.start(photo) }
    LaunchedEffect(state) {                                  // auto-close after success / cancel
        if (state is SenderState.Delivered) { delay(1_200); onClose() }
        if (state is SenderState.Cancelled) onClose()
    }
    BackHandler { viewModel.cancel() }                       // use the project's cross-platform back handling

    Box(
        Modifier
            .fillMaxSize()
            .teleportGesture(
                mode = SpreadMode.Pinch,
                enabled = state is SenderState.Connected,
                onStart = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                onProgress = { gesture = it },
                onCommit = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch { animate(gesture, 1f, animationSpec = tween(250)) { v, _ -> gesture = v } }
                    viewModel.onGrabCommitted()
                },
                onCancel = {
                    scope.launch { animate(gesture, 0f, animationSpec = spring()) { v, _ -> gesture = v } }
                },
            ),
    ) {
        TeleportStage(imageModel = photo.uri, progress = { gesture }, mode = SpreadMode.Pinch, modifier = Modifier.fillMaxSize())
        // Caption + extras anchored at the bottom, by state:
    }
}
```

Behavior per state (the zoomed-out photo is always visible; photo starts slightly scaled down, e.g. `0.8f` fill, to give the "zoom out" feel on entry via `animateFloatAsState`):
- `Searching(peers)`: caption "Looking for a receiver nearby…" with a small indeterminate progress. If `peers.size > 1`: show a list "Choose a device" (each row calls `viewModel.connect(peer)`).
- `Connecting(peer)`: "Connecting to {peer.name}…"
- `Connected`: title "Ready to share", subtitle "Pinch three fingers together to grab the photo". Small chips: "{peer.name} · code {pairingCode}" and "Preparing {pct}%".
- `Released`: "Teleporting…" with the transfer progress. Keep `gesture = 1f` (photo gone).
- `Delivered`: "Delivered ✓".
- `Failed(error)`: error message (map `TeleportError` to English text) plus **Try again** (re-enter the screen / call `start` on a fresh ViewModel) and **Close**.
- A **Cancel** text button (top-end) is always visible until terminal.

### 7.4 `TeleportReceiverScreen` (structure and behavior)

Mirror of the sender:
- `LaunchedEffect(Unit) { viewModel.start() }`, back handler → `viewModel.cancel()`.
- Gesture: `teleportGesture(mode = Spread, enabled = state is Incoming && !state.userDropped, ...)`. On commit: haptic, animate `gesture` to 1, `viewModel.onDropCommitted()`.
- Image model: `"file://${incoming.thumbnailPath}"` while `Incoming`; after `Committed(savedUri)` show `savedUri` (full photo) with `gesture = 1f`.
- When `Incoming && senderReleased && !userDropped`: pulse the glow (looping `infiniteTransition` alpha) so the user notices the photo is "in the air".
- Captions:
  - `Waiting`: "Waiting for a sender… Open Air Teleport on the other phone."
  - `Connected`: "Connected to {name} · code {code}"
  - `Incoming` not released: "Photo incoming from {name}… {pct}%"
  - `Incoming` released and not dropped: "The photo is in the air. Put three fingers together, then spread them open."
  - `Incoming` dropped but not yet committed: "Finishing…"
  - `Committed`: "Saved to your gallery ✓" with a **Done** button (auto-close after 1.5 s).
  - `Failed(error)`: message plus **Close**.

### 7.5 Error text mapping

`TeleportError` → English strings (resources): NoReceiverFound: "No receiver found. Open Air Teleport Receiver on the other phone."; ConnectionFailed: "Couldn't connect. Try again."; Disconnected: "Connection lost."; TimedOut: "Took too long. Try again."; RemoteCancelled: "The other phone cancelled."; TransferFailed: "The transfer failed."; SaveFailed: "Couldn't save the photo."

**Verify:** compiles for Android and iOS targets, previews/ladder screenshots if the project uses them, lint/detekt clean.
**Report, then ask:** `Step 7 is complete. Start Step 8: Integration (DI, navigation, entry points)? (yes / no / adjust)`

---

## STEP 8: Integration (DI, navigation, entry points, gallery refresh)

**Goal:** wire everything into the real app.

### 8.1 Koin

```kotlin
// commonMain
val teleportModule = module {
    viewModel { TeleportSenderViewModel(get(), get(), get()) }
    viewModel { TeleportReceiverViewModel(get(), get(), get()) }
}
expect val teleportPlatformModule: Module

// androidMain
actual val teleportPlatformModule: Module = module {
    factory<TeleportTransport> { NearbyTeleportTransport(androidContext()) }   // factory: one transport per session
    single<ThumbnailMaker> { AndroidThumbnailMaker(androidContext()) }
    single<ReceivedPhotoStore> { MediaStoreReceivedPhotoStore(androidContext()) }
    single<DeviceNameProvider> { AndroidDeviceNameProvider() }
    single<TeleportAvailability> { object : TeleportAvailability { override val isSupported = true } }
}

// iosMain
actual val teleportPlatformModule: Module = module {
    factory<TeleportTransport> { UnsupportedTeleportTransport() }   // every method throws UnsupportedOperationException
    single<ThumbnailMaker> { /* throws UnsupportedOperationException */ }
    single<ReceivedPhotoStore> { /* throws UnsupportedOperationException */ }
    single<DeviceNameProvider> { object : DeviceNameProvider { override fun deviceName() = "iPhone" } }
    single<TeleportAvailability> { object : TeleportAvailability { override val isSupported = false } }
}
```

Register both modules wherever the project loads its modules.

### 8.2 Navigation 3

Add two destinations in the project's style, for example:

```kotlin
@Serializable data class TeleportSendRoute(val uri: String, val displayName: String, val mimeType: String, val sizeBytes: Long) : NavKey
@Serializable data object TeleportReceiveRoute : NavKey
```

Register them in the existing `entryProvider`. Sender: build `PhotoRef` from the route args. Both screens call `onClose = { backStack.removeLastOrNull() }` (use the project's pattern).

### 8.3 Share sheet entry (sender)

- In the in-app share sheet, add an **Air Teleport** item (icon + label) shown only when `TeleportAvailability.isSupported` and exactly one image is selected (multi-select is out of scope for now).
- On tap: `gate.ensureReady { navigate(TeleportSendRoute(...)) }` using `rememberTeleportGate(onBlocked = { blocked = it })`, and show `TeleportBlockedDialog` when `blocked != null`.

### 8.4 Home entry (receiver)

- Add **Air Teleport Receiver** to the home screen entries (next to gallery, screenshots, documents), hidden when `isSupported` is false.
- On tap: `gate.ensureReady { navigate(TeleportReceiveRoute) }`.

### 8.5 Gallery refresh after receiving

- After `ReceivedPhotoStore.save` succeeds, trigger the **same** refresh mechanism the gallery already uses for new media (found in Step 0, item 7). If the gallery already observes MediaStore, no extra code is needed; verify and say so. If it keeps its own index, call its "index this URI" use case from the receiver flow via a new port rather than coupling to the repository from the session.

### 8.6 Startup cleanup

On app start (and in `NearbyTeleportTransport.shutdown()`), delete `cacheDir/teleport/` files older than 1 hour so interrupted sessions never leave orphans.

**Verify:** app launches; share sheet shows Air Teleport; the Receiver entry opens the receiver screen; blocked dialog appears with Bluetooth off. Lint/detekt/tests green.
**Report, then ask:** `Step 8 is complete. Start Step 9: Hardening and edge cases? (yes / no / adjust)`

---

## STEP 9: Hardening and edge cases

**Goal:** make it robust. Implement, test and verify each item.

1. **Lifecycle:** leaving either screen (back, app backgrounded for > 30 s) cancels the session and shuts the transport down. No leaked advertising/discovery. Verify `shutdown()` is called on every exit path.
2. **Rotation / process death:** the sessions live in ViewModels, so rotation keeps the transfer. Process death simply cancels (acceptable). Make sure nothing crashes on restore.
3. **Large photos:** test with 10–25 MB images; progress ring must stay accurate; sender can grab before completion and the receiver waits in "Finishing…".
4. **Concurrent senders:** two phones both trying to send to one receiver → the second connection is rejected/ignored (point-to-point). The receiver must not crash or mix payloads; verify `transferId` checks.
5. **Stale messages:** ignore `Committed`/`Released` with a non-matching `transferId`.
6. **Disk space:** if staging fails (`IOException`), surface `TransferFailed` cleanly.
7. **Security review:** receiver never writes to the gallery without the explicit drop gesture; staged files are in app-private cache; pairing code is visible on both screens; only `image/*` mime types are accepted by the receiver (reject others with `TransferFailed`).
8. **Accessibility:** provide a non-gesture fallback: a visible **Send now** button on the sender (calls `onGrabCommitted`) and **Receive** button on the receiver (calls `onDropCommitted`) when TalkBack is on or after 6 s with no gesture.
9. **Battery:** advertising/discovery run only while the screens are visible; verify nothing remains active afterward (check `adb shell dumpsys bluetooth_manager` or Android Studio's profiler).
10. **Logging:** log only lifecycle events (state transitions, error enums). Never log file names, URIs or pairing codes.
11. Add missing unit tests for everything above; confirm coverage ≥ 80% (domain and data) and lint/detekt clean.

**Report, then ask:** `Step 9 is complete. Start Step 10: Manual QA on two real devices? (yes / no / adjust)`

---

## STEP 10: Manual QA matrix (two real Android phones)

**Goal:** a checklist the team can execute, plus a short results template. The agent writes `docs/AIR_TELEPORT_QA.md` and walks the user through each case; it does not fake results.

| # | Scenario | Expected |
|---|---|---|
| 1 | Happy path, 3 MB photo, both phones on Android 13+ | Connects < 3 s; grab/drop animations track fingers; photo appears in gallery |
| 2 | Large photo (20+ MB) | Grab before completion → B shows "Finishing…" then saves |
| 3 | Receiver opens AFTER sender | Sender finds B within the search timeout |
| 4 | Sender grabs but B never drops | B times out; A shows failure after timeout; nothing saved |
| 5 | Pinch released early on A | Photo snaps back; can retry |
| 6 | Bluetooth off / Wi-Fi off / permission denied | Blocked dialog with a correct settings shortcut |
| 7 | Move phones apart mid-transfer | Clear error; staging cleaned; no crash |
| 8 | Cancel on A mid-transfer | B shows "cancelled" |
| 9 | Android 8–10 device (API 26–30) | Location prompt/toggle flow works; saving works |
| 10 | Android 12 and 14 devices | Correct permission set |
| 11 | OEM matrix (Samsung, Xiaomi, Pixel at least) | 3-finger gesture not hijacked; if hijacked, change `minPointers` (4 or 5) and re-test |
| 12 | Repeat transfers 10x in a row | No leaked state; no "already advertising/discovering" errors |
| 13 | Non-GMS device (if available) | Blocked dialog "Needs Google Play services" |

Record per scenario: device pair, Android versions, connection time, transfer time, pass/fail, notes.

**Report:** summarize results and open issues. Then say: `All steps are complete. Want a final review pass (lint, coverage, handbook updates)?`

---

## Appendix A: Out of scope (future)

- iOS transport (MultipeerConnectivity / local network), cross-platform LAN fallback
- Multi-photo and video transfer
- Camera-based air gestures (MediaPipe hand tracking)
- UWB direction/distance awareness
- Persistent trusted-device pairing

## Appendix B: Troubleshooting

- `STATUS_ALREADY_ADVERTISING` / `STATUS_ALREADY_DISCOVERING`: a previous session wasn't shut down. Check that `shutdown()` runs on every exit path and that the transport is a Koin `factory`.
- Discovery finds nothing on Android ≤ 11: Location services toggle is off.
- Connection works but transfer is slow: the link stayed on Bluetooth. Retry; keep phones close; confirm Wi-Fi is on for both.
- 3-finger gesture does nothing on some OEMs: system gesture conflict; increase `minPointers` or disable the OEM gesture while testing.