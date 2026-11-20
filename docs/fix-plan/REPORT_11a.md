# Report — Step 11a (one indexing trigger, a state that can't stick)

Implemented directly by Claude at the human's request (normally the Implementer's job). Not committed; the human commits.

Step 11 from `PLAN.md` is split:
- **11a (this report):** one trigger per platform, a run guard that always ends `IDLE`, and foreground-service fixes.
- **11b (next):** incremental sync with `sync_checkpoint`, deletions diffed in Kotlin and deleted in chunks of ≤500, batched writes, and the splash only until the start route is known.

## Changes

1. **Run guard** — `data/.../IndexRunGuard.kt` (new), used by `RoomIndexRepository.rebuild()`.
   - A call while a pass is running returns `Ok` at once (`Mutex.tryLock`), instead of queueing a second full MediaStore scan. This replaces the `RUNNING` check inside the lock, which could never be true.
   - `finally` sets the state back to `IDLE` if the pass ends while still `RUNNING` (error, cancellation, or early return). A `PAUSED_BATTERY` state is kept.
   - Exceptions become `AppResult.Err(AppError.Unknown(<exception class>))`; `CancellationException` is rethrown. Before, an exception escaped into the service's `SupervisorJob` scope, which has no handler, so it would crash the app.
   - Removed `indexedThisSession` (it was set but never read).
   - The three `catch (_: Throwable)` blocks in the pass now catch `Exception` and rethrow cancellation.
2. **One trigger per platform** — `composeApp/.../IndexingTrigger.kt` (+ Android and iOS actuals).
   - Android: only `MainActivity` → `IndexingForegroundService`. `App.kt` no longer calls `rebuild()`.
   - iOS: `App.kt` still calls `rebuild()`, because it's iOS's only trigger (there's no background service). `PLAN.md` said to remove the `App.kt` call, which would have stopped iOS indexing.
3. **Foreground service** — `androidApp/.../IndexingForegroundService.kt`.
   - `onTimeout(startId, fgsType)` (Android 15+): cancels the pass and stops the service, so the `dataSync` 6-hour limit can't crash the app.
   - `START_NOT_STICKY` instead of `START_STICKY`.
   - Dispatchers come from `AppDispatchers` (Koin), not `Dispatchers.Default`.
   - Repeated start commands don't launch a second pass while one is active.
   - `ServiceCompat.startForeground(..., FOREGROUND_SERVICE_TYPE_DATA_SYNC)`; `stopForeground(STOP_FOREGROUND_REMOVE)` instead of the deprecated boolean version.
   - New status-bar icon `res/drawable/ic_stat_indexing.xml` (photo library) instead of `stat_sys_upload`, which implied uploading.
   - Removed the pre-API-26 notification branches (minSdk is 26).

## Deviations from `PLAN.md`
- Kept the `App.kt` trigger on iOS (see 2).
- Kept the foreground service rather than moving to WorkManager. The service already exists, shows progress to the user, and `onTimeout` plus `START_NOT_STICKY` remove its two crash and rerun risks. The "index only while charging" setting (empty `WorkManagerSyncScheduler`) is still not wired up; it belongs with 11b.
- No `POST_NOTIFICATIONS` request. On 13+ without it, the service still runs, but its notification is hidden from the drawer. Left for a UX decision.

## Checks
- `./gradlew testAndroidHostTest`: PASS — 111 tests, 0 failures (6 new in `IndexRunGuardTest`: ends `IDLE` after return, error and cancellation; a concurrent call is skipped; paused state kept; cancellation not turned into `Err`).
- `./gradlew :androidApp:assembleDebug`: PASS.
- iOS compile: `:data` PASS. `:platform` (and so `:composeApp`) **FAIL, not caused by this step**: `IosVaultCryptoEngine` and `IosStubVaultCryptoEngine` don't implement the vault port's `restoreToStorage(itemId, mime, takenAtMs)` / `getBlobPath`. That is vault code, so it's left for the vault owner (see notes below).
- Device (Pixel 2, Android 10): installed and launched; the service starts about 3 s after launch, finishes its pass about 2 s later and stops itself; no crashes in `logcat -b crash`.
- Android 15 `onTimeout`: not run (needs an Android 15 device and `adb shell cmd activity` FGS timeout testing).

## Notes for the vault PR
- iOS doesn't compile: `platform/src/iosMain/.../IosSecurityAndVaultAdapters.kt:58` and `IosStubAdapters.ios.kt:46,58` must match the current `VaultCryptoEngine` (`restoreToStorage(itemId, mime, takenAtMs)`, `getBlobPath`).

## Noticed but not touched
- `MainActivity` requests media permissions itself on first launch, while onboarding has its own permission gate and also calls `rebuild()`. The guard makes the overlap harmless, but there are two permission flows.
- Notification texts are hardcoded (Step 17, strings).
