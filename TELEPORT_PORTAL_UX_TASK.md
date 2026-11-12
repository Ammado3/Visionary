# Task: "Portal" UX for Air Teleport (sender + receiver)

Branch from `feature/atp1`: `feature/teleport-portal-ux`. Presentation layer only. One commit per phase. Report exact Gradle output after each phase. If something here contradicts what you find in the code, stop and ask.

## 0. Goal

Make Air Teleport feel premium and minimal. No camera preview, no hand skeleton, no status text blocks. Text appears only as short animated captions over a futuristic animated background, while the connection is established in the background.

### Sender timeline (user taps Share, sender screen opens)
1. Background fades in. Caption **"Opening portal"** fades in, blinks twice, fades out.
2. Caption **"Ready to share"** fades in, blinks twice, fades out.
3. Photo fades in on the stage. Live gesture hints appear one at a time (see section 5): "Bring your hand closer", "Move your hand back a little", "Close your hand" and so on, driven by the real hand tracking. Closing the hand grabs the photo.

### Receiver timeline
1. Same background. Caption **"Opening portal"** (same animation).
2. Caption **"Ready to receive"** (same animation).
3. Calm waiting visual with no text. When a photo is incoming it appears on the stage, and live hints for the open-hand gesture show (see section 5).

During all of this the transport keeps working. The animation must never delay or block connection or transfer.

## 1. What the code does today (read these before changing anything)

All under `features/teleport/src/` (package `...visionary.feature.teleport`):

- `commonMain/presentation/ui/TeleportSenderScreen.kt` (259 lines) and `TeleportReceiverScreen.kt` (242): a full-screen `Surface(color = VisionaryColors.colorF4FDF7)` containing `TeleportStage` (glow + photo), `CameraAirGestureOverlay`, a header row ("Air Teleport" / "Air Teleport Receiver" + text "Close" button) and a bottom `Column` that switches on the session state, showing status text, `LinearProgressIndicator`, a pairing code chip, peer names, "Try Again" and "Done" buttons.
- `commonMain/presentation/ui/TeleportStage.kt`: radial glow + `AsyncImage`; `progress: () -> Float` drives glow/alpha/scale. Keep this progress contract.
- `commonMain/gesture/CameraAirGestureOverlay.kt`: `expect @Composable fun CameraAirGestureOverlay(targetGesture, enabled, onGestureProgress, onGestureCommitted, modifier)`.
- `androidMain/gesture/CameraAirGestureOverlay.android.kt` (255 lines): requests the CAMERA permission itself, binds CameraX `Preview` + `ImageAnalysis` (front camera), runs `GestureRecognizerAnalyzer` (MediaPipe) into `AirGestureEngine`, and draws the UI: a black 185dp card with `PreviewView`, `GestureOverlayView` (landmark skeleton), a progress bar, a status line (`statusFor(...)`, with emoji), a minimize button and a FAB. It also shows a Toast when the model is missing.
- `androidMain/gesture/AirGestureEngine.kt`: pure logic. `Phase { NoHand, TooFar, TooClose, Arming, Ready, Active, Committed }`, `Output(phase, progress, committed)`. It latches after commit (`done = true`) and only `reset()` clears it. Important for section 4.
- `commonMain/gesture/teleportGesture` (Modifier): the 3-finger pinch/spread touch fallback. Leave it unchanged.
- Sessions (`domain/session/*`) and states (`SenderState`, `ReceiverState`): **do not change.** Sender flow: `Idle -> Searching -> (auto-connect 1.2s after a single peer is found) Connecting -> Connected -> Released -> Delivered`. Receiver: `Idle -> Waiting -> Connected -> Incoming -> Committed`. The sender session starts discovery as soon as `start()` is called from the screen's `LaunchedEffect`, so the connection already runs concurrently with whatever the UI shows.
- `TeleportEntries.kt`: the screen is only composed after `gate.ensureReady` (Nearby/Bluetooth/location readiness). The CAMERA permission is **not** part of that gate today, which means the system dialog appears on top of the screen from inside the overlay.

## 2. Hard constraints

- Do not touch `domain/**`, `data/**` or the 6 existing teleport test files except to fix imports. They must still pass.
- Keep the touch fallback (`teleportGesture`) working: sender 3-finger pinch, receiver 3-finger spread.
- No hardcoded user-facing strings spread through the UI. Check whether the teleport module has Compose resources configured. If yes, use `stringResource`. If not (the build-logic cleanup may not have landed), put every caption, hint and error string in ONE file `presentation/TeleportCopy.kt` so it can be migrated mechanically later. Say which you chose.
- Do not edit `:core:designsystem` or other shared modules (other people own them). Put the portal palette in a teleport-local `PortalPalette` object.
- Only use `AppDispatchers`/coroutine scopes from the composition. No `Dispatchers.*` in new code and no `GlobalScope`.
- No `!!`, no `@Suppress` without asking.
- Smooth: the background and captions must animate in the draw/graphicsLayer phase only. No per-frame recomposition, no per-frame allocations in draw lambdas.

## 3. Phase 1: Make the gesture tracker headless (remove the camera preview)

Goal: camera analysis keeps working, but nothing camera-related is drawn.

1. In `commonMain/gesture/`, define:
   ```kotlin
   enum class AirHint { NoHand, TooFar, TooClose, Arming, Ready, Active, Committed }
   ```
   and map `AirGestureEngine.Phase` to it (or move `Phase` to commonMain and reuse it; pick the smaller change).
2. Replace the `expect` composable with a headless tracker, e.g.
   ```kotlin
   @Composable
   expect fun AirGestureTracker(
       targetGesture: TargetAirGesture,
       enabled: Boolean,        // camera bound and analysing
       tracking: Boolean,       // frames are fed to the engine and callbacks are fired
       onHint: (AirHint) -> Unit,
       onGestureProgress: (Float) -> Unit,
       onGestureCommitted: () -> Unit,
   )
   ```
   It draws nothing (returns after setting up effects).
3. Android actual: keep the permission request, the CameraX `ImageAnalysis` binding, the analyzer and the engine. **Delete:** `Preview` use case and `PreviewView`, `GestureOverlayView.kt` (the file and its use), the `Surface`/card/progress bar/status text/minimize button/FAB, `isExpanded`, and the Toast (report "model missing" via a callback instead, see section 6).
   - `DisposableEffect(hasCameraPermission, previewView, analyzer)` loses `previewView` as a key.
   - Verify on a device that `ImageAnalysis` alone delivers frames from the front camera with no `Preview` bound. If it does not on any device you test, stop and report instead of working around it.
4. **Engine latch rule:** `AirGestureEngine` stays committed forever after one commit. When `tracking` is false, drop frames before they reach the engine (do not call `engine.onFrame`, do not call callbacks). When `tracking` flips from false to true, call `engine.reset()` so a pose held during the intro cannot commit instantly or leave the engine latched.
5. iOS actual: a no-op that compiles (unsupported already; keep behavior).
6. Update both screens minimally so the project compiles (they can still use the old layout for now). Delete `AirGestureDetectionState.statusMessage` only if unused.

Done when: the app builds, the gesture still works on a device with no camera preview visible, and `GestureOverlayView` no longer exists.

## 4. Phase 2: Portal building blocks (new files in `presentation/ui/portal/`)

### 4.1 `PortalPalette`
Dark, futuristic, not the app's light theme: base `#050B09` to `#0A1F17`, emerald accents around `#00C27A` and a brighter `#6BFFC8` for glow, text white at about 92% alpha. Declare them once.

### 4.2 `PortalBackground(energy: () -> Float, modifier)`
One `Canvas` (or `drawWithCache`), layers from back to front:
1. Vertical deep gradient base.
2. Two large soft radial "aurora" blobs drifting on slow Lissajous paths (period 14 to 22 s).
3. Concentric rings expanding from the centre every ~3 s and fading as they grow (the portal feel).
4. About 36 tiny particles, seeded deterministically (fixed seed), drifting upward with a twinkle.

`energy` (0..1, read inside the draw lambda) scales ring speed/opacity and glow. One shared `rememberInfiniteTransition` clock (a single `0..1` float over ~20 s, linear) feeds all layers. Precompute particle seeds once with `remember`. Check frame time on a mid-range device in Android Studio's profiler; there must be no visible jank.

### 4.3 `PortalCaption`
A composable (or a `suspend fun` on an `Animatable<Float>`) that plays one caption cycle:
- fade in 450 ms (`FastOutSlowInEasing`),
- blink twice: each blink dips alpha to 0.2 over 320 ms and returns to 1.0 over 320 ms,
- hold 150 ms,
- fade out 450 ms.

Define all durations in one `PortalTiming` object. Style: thin/light weight (300), about 28sp, letter spacing about 2sp, a soft glow (`Shadow` with the accent colour). Add `semantics { liveRegion = LiveRegionMode.Polite }` so screen readers announce the caption. Caption alpha goes through `graphicsLayer { alpha = ... }`.

### 4.4 `PortalChoreography` (pure logic, commonMain, unit-tested)
A small reducer/state machine, independent of Compose, so the rules are testable:
```
Stage { Opening, Holding, Ready, Live, Failed }
```
Inputs: `introAnimationFinished`, `sessionReady` (see below), `sessionFailed`.
- `Opening` plays the "Opening portal" caption.
- When that animation ends: if `sessionReady` is true go to `Ready`, otherwise `Holding` (no text; the background energy rises slowly; session timeouts already exist, so no new timeout).
- `Holding -> Ready` as soon as `sessionReady` becomes true.
- `Ready` plays the second caption, then goes to `Live`.
- Any `sessionFailed` at any stage goes immediately to `Failed` (cancel running animations, show the error UI from section 7).
- Never go backwards, and the intro plays **once per screen entry**. Keep the "intro already played" flag in `rememberSaveable` so rotation or recomposition does not replay it.

`sessionReady` means: **sender** = state is `Connected` (or later); **receiver** = state is `Waiting` (advertising started) or later.
Gestures (touch and camera) are enabled only in `Live` **and** when the session state allows it (sender `Connected`, receiver `Incoming && !userDropped`). The camera tracker is enabled (warm) from the start of the screen so tracking is instant at `Live`, but `tracking = false` until `Live` (section 3.4).

## 5. Live gesture hints (shown in `Live` only)

A single hint line at the bottom centre, same typography as captions but smaller (about 18sp), crossfading between hints (220 ms). Debounce so text does not flicker: a hint stays at least 400 ms, and ignore changes that last under 250 ms.

Sender (target GRAB):
| AirHint | Text |
|---|---|
| NoHand | "Show your hand" |
| TooFar | "Bring your hand closer" |
| TooClose | "Move your hand back a little" |
| Arming | "Open your hand" |
| Ready | "Close your hand" |
| Active / Committed | no text (the glow and photo animation carry it) |

Receiver (target OPEN_HAND), shown only while a photo is incoming (`Incoming && !userDropped`):
| AirHint | Text |
|---|---|
| NoHand | "Show your hand" |
| TooFar / TooClose | same as above |
| Arming | "Make a fist" |
| Ready | "Open your hand" |
| Active / Committed | no text |

If CAMERA permission is denied or the model is unavailable, show "Pinch with three fingers" (sender) / "Spread three fingers" (receiver) as the single hint instead, since the touch fallback still works.

No emoji anywhere.

## 6. Camera permission

Today the permission dialog pops up inside the overlay on top of the screen. Move it earlier: add CAMERA to the Teleport gate (`TeleportGate` / `AndroidReadiness`) as an **optional** permission (ask once, but a denial does not block Teleport, because touch fallback exists). Then the dialog shows before the portal screen and never interrupts the animation. In the tracker, expose `onUnavailable` (permission denied or model missing) instead of a Toast, and use it to switch to the fallback hint.

## 7. Phase 3: Sender screen

Replace the content of `TeleportSenderScreen` using the pieces above. Layers: `PortalBackground` > photo stage > caption/hint > controls.

- **Remove:** the "Air Teleport" title, the text "Close" button, "Looking for a receiver nearby…", "Connecting to …", the paragraph about closing a fist, the pairing-code chip, peer name labels, `LinearProgressIndicator`/`CircularProgressIndicator`, "Teleporting… N%", "Delivered ✓" text.
- **Close:** a small icon-only button (top end, low alpha, 48dp touch target, with a content description). System Back and the icon both call `viewModel.cancel()`. Add `BackHandler` so Back during the intro cancels the session.
- **Stage:** `TeleportStage` restyled for the dark background. During `Opening`/`Holding`/`Ready` the photo is hidden (alpha 0). On entering `Live` it fades in over 500 ms with a scale from 0.96 to 1.0.
- **Gesture wiring:** `enabled`/`tracking` as in section 4.4. The existing grab handlers (haptics, `animatableGesture`, `viewModel.onGrabCommitted()`) stay exactly as they are.
- **Released (transfer in progress):** no text. Show a thin glowing progress arc around the photo from `transferProgress`, and raise `energy` to 1.0.
- **Delivered:** a drawn checkmark animation (about 600 ms) plus a haptic, then the existing auto-close delay. No text.
- **Multiple receivers:** the peer picker is functional, so keep it, but only after the intro (`Live`) and only when `peers.size > 1`. Style it as small glass chips (translucent dark surface, emerald border). This is the only persistent text on the sender screen.
- **Failed:** the error message (existing `toMessage()` strings), centred, in the same typography, with two quiet actions "Try again" and "Close" as text buttons in the accent colour. Errors are the one exception to "no extra text", because the user needs to know what happened. If `Failed` happens during the intro, show it immediately (section 4.4).

## 8. Phase 4: Receiver screen

Same structure and components.
- Same removals as the sender (title, close text button, all status blocks, pairing code, "Connected to …", progress bars, "Saved to your gallery ✓", the "Done" button).
- `Opening portal`, then `Ready to receive`, then `Live`.
- While `Waiting`/`Connected` in `Live`: no text, only the ambient background (the energy stays low, about 0.4).
- `Incoming`: the thumbnail fades onto the stage; while the file is still arriving (`!senderReleased`) show a thin progress arc from `transferProgress`; hints from section 5 apply while `!userDropped`.
- `userDropped` and finishing: arc continues, no text.
- `Committed`: checkmark animation + haptic, the saved image shown, then the existing 1.5 s auto-close (keep the delay, drop the Done button).
- `Failed`: same pattern as the sender.

## 9. Phase 5: Tests

- Unit tests for `PortalChoreography` (commonTest): normal order; session ready before intro ends; session ready after intro ends (goes through `Holding`); failure in each stage; no replay after completion; saved-flag behavior.
- Unit tests for the hint debouncer (pure function over (hint, timestamp) inputs).
- Existing 6 teleport test files still pass. **First check** that teleport tests are actually executed by `./gradlew testAndroidHostTest`: the module still has `src/androidUnitTest`, and the working source set under this plugin may be `androidHostTest`. Report the teleport test count. If they are not being counted, move that directory (`PlatformHelpersTest`) and report it as a separate commit.

## 10. Verification (report all)

1. `./gradlew assembleDebug` and `./gradlew testAndroidHostTest`, with the teleport test count.
2. `grep -rn "PreviewView\|GestureOverlayView\|Toast" features/teleport/src` returns nothing.
3. Manual test on **two real devices** (record a screen video of each):
   - normal flow, both sides, full timeline as in section 0;
   - connection established **before** the intro ends (move devices close first), and connection **slower** than the intro (the `Holding` state shows no text, then "Ready to share");
   - failure during the intro (turn the receiver off) shows the error immediately;
   - Back pressed during the intro and during `Live`;
   - camera permission denied: fallback hint, 3-finger touch works;
   - rotate the sender screen mid-intro: the intro does not replay;
   - hold an open hand in front of the camera during the intro, then reach `Live`: no instant commit;
   - frame rate of the background on the lowest-end device you have (note the device).
4. List anything you left out or changed from this spec, and why.

## 11. Out of scope

Session/transport/protocol changes, iOS behavior, the teleport share entry points, the strings sweep for other modules, and changes to `:core:designsystem`.
