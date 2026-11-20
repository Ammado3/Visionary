# Fix plan (v2)

The source of truth for what to fix, in order. The reviewer writes each step spec from this file. Read `WORKFLOW.md` for how the loop works.

This version replaces the first plan. It was written from a fresh review of `Develop` at `2dc811f` (20 Nov 2026), which includes:
- the `review-audit` architecture cleanup;
- the vault lock, pattern lock and vault grid;
- the gallery refactor (albums, favorites);
- the background indexing service;
- iOS engines;
- voice search.

Line numbers move; always read the current code before writing a spec.

**Gated** steps stop for a human device check before the next step starts. Each **phase** ends with the human merging `review-fixes` into `Develop`.

---

## Where things stand

### Done on `review-fixes` (not yet in `Develop`)

| Step | What it fixed | Fate in v2 |
|---|---|---|
| 01 | No destructive DB fallback; Search delete really deletes; `DeleteMediaUseCase` rethrows cancellation; vault delete removes the blob first | Keep. All four bugs are still on `Develop`. |
| 02 | Settings persisted with DataStore | **Drop.** `Develop` now persists settings in Room (`app_settings` table, schema v5) with two extra fields. Keep Room; remove the DataStore code. |
| 02b | Start screen uses the onboarding flag | Keep, adapted: `Develop` starts on `AppRoute.Photos`, not `Home`, and `Onboarding` is still a `data object`. |
| 03 | Category mapping for collections, galleries and search filters | Keep. Still awaiting review; the bug is still on `Develop`. |

### Fixed on `Develop` since the first review
- N+1 queries in `observeMedia`, `observeMediaByAlbum` and `observeFavorites` (they use `getTexts` in batch now). `observeItem` and search still query per row.
- Vault restore writes the file back to the gallery (`restoreToStorage`) instead of returning a fake success.
- Fake data mode and `:core:testing` are removed. `:core:testkit` is test-only.
- The indexing loop rethrows `CancellationException`.
- Voice search ("Talk to Visionary") is implemented and tested, per spec.

### Still broken on `Develop` (details in the steps below)
- **Vault security:**
  - files are stored unencrypted;
  - any PIN of 4+ digits or any 4-dot pattern unlocks it;
  - `unlock()` only flips a boolean;
  - there's no route guard and no `FLAG_SECURE`.
- **iOS backup:** the vault, database and trash are in iOS `Documents`, so they get backed up to iCloud.
- **Database migrations:** the database is at v5, with `fallbackToDestructiveMigration()` and no migrations, so every schema bump wipes the vault index. Merging `review-fixes` removes the fallback, so **migrations 3→4 and 4→5 are required in the same phase**. Otherwise, upgrading from an old install crashes.
- **Categories and classification:** the category mapping is broken, and the classifier and entity extractor are never called.
- **Search:** full-scan search, and fake embeddings.
- **Indexing:**
  - it runs twice on every launch (the foreground service plus `App.kt`);
  - it can be left stuck in `RUNNING`;
  - the foreground service has no Android 15 time-limit handling.
- **Trash:** the bugs from Step 08 (old plan) are still there, plus a new one: if copying a photo to the trash fails, the trash row points at the original file, which is then deleted.
- **Architecture:** voice search added a `:features:aisearch` → `:platform` dependency, which breaks the new rule in `AGENTS.md`.

---

## Phase A — Reconcile branches and protect data

Goal: `review-fixes` and `Develop` become one codebase again, and no upgrade can wipe or crash. **Ends with the human merging `review-fixes` into `Develop`.**

### Step R0 — Merge `Develop` into `review-fixes`

**Not gated, but the reviewer reviews Step 03 as part of it.**

1. Review Step 03 first (`STEP_03_category_mapping.md`, `REPORT_03.md`) as a normal review. Fix it if needed, then do the merge.
2. `git merge origin/Develop` into `review-fixes`. A trial merge on 20 Nov had **9 conflicts**. Resolution rules:

| File | Keep |
|---|---|
| `AGENTS.md` | `Develop`'s version, plus the "Database rules" section from `review-fixes` |
| `App.kt` | `review-fixes`' `chooseStartRoute(resolveStartRoute(), access)`, adapted to start at `AppRoute.Photos` (not `Home`), with `Onboarding`'s skip-intro behaviour from 02b. Keep `Develop`'s new imports (vault navigation, decoder factories) |
| `Navigation.kt` | `Develop`'s routes plus 02b's `Onboarding(skipIntro)`. Update `AppRouteSerializationTest` |
| `Daos.kt` | both sides: `Develop`'s albums and favorites queries plus 03's `observeByCategories` and `observeCategoryCounts` |
| `RoomRepositories.kt` | `Develop`'s structure (batched `getTexts`, albums, favorites, real vault restore, Room settings) plus 01's vault delete order and 03's category mapping |
| `DataModule.kt`, `PlatformModule.android.kt`, `PlatformModule.ios.kt` | `Develop`'s Room settings binding and constructor-reference style. **Remove** the DataStore binding |
| `features/search/.../SearchModule.kt` (deleted on `Develop`, modified on `review-fixes`) | delete it; apply 01's `DeleteMediaUseCase` injection to `features/aisearch/.../SearchModule.kt` and `SearchViewModel.kt` instead |

3. Remove what Step 02 added:
   - `DataStoreSettingsRepository` and its test;
   - the DataStore dependencies in `:data` and `:platform`;
   - `AppDirectories.settingsFilePath()`;
   - the catalog entry, if unused.
4. Check that the auto-merged files still hold the fixes:
   - `SearchViewModel` delete calls the use case;
   - `DeleteMediaUseCase` rethrows `CancellationException`;
   - `DatabaseBuilder` has no blanket destructive fallback.
5. Everything builds and tests pass. Commit as `Sync: merge Develop into review-fixes`, with a short `REPORT_R0.md` that lists each conflict and how it was resolved.

### Step 04 — Real database migrations 3→4→5

**Not gated. Must land before Phase A is merged.**

After R0, the database is at v5 with destructive fallback only from versions 1–2, so installs at v3 or v4 would crash on launch. The exported schemas show these changes:

| Version | Changes |
|---|---|
| 3→4 | `media_item` adds `albumId`; `vault_item` drops `filePath` |
| 4→5 | adds table `app_settings` (`id, onboardingDone, indexOnlyWhileCharging, vaultLockTimeoutSeconds, visualAnalysisEnabled, indexingCardShown`); `media_item` adds `isFavorite`; `vault_item` adds `filePath` back |

- Write `MIGRATION_3_4` and `MIGRATION_4_5` as manual `Migration` objects.
  - Take column types, nullability and defaults exactly from `schemas/.../4.json` and `5.json`.
  - Dropping a column means rebuilding the table: create the new table, copy, drop, rename.
  - The FTS triggers in `FtsCallback` reference `media_item`, so check they still work after `media_item` changes (add-column is safe; don't rebuild `media_item`).
- Register them in `DatabaseBuilder` with `addMigrations(...)`.
- Migration test, if Room's `MigrationTestHelper` works in this KMP setup. If not, document a device check:
  1. install a build at v3 (tag or commit before `albumId`);
  2. add photos, a vault item and a favorite;
  3. install the new build;
  4. check everything is still there.
- `AGENTS.md` database rules: every future version bump adds a `Migration` and a test or device check.

---

## Phase B — Vault security

All gated. **Ends with a human merge.** Do the steps in order; 06 depends on 05's unlock session.

### Step 05 — Vault lock that actually locks

**Gated.** Split into 05a (auth flow and guard) and 05b (PIN and pattern secrets).

**Problem (current `Develop`):**
- `BiometricViewModel`:
  - `OnUnlockClicked` sends `UnlockSuccess` for any PIN of 4+ digits;
  - `OnPatternCompleted` unlocks with any pattern of 4+ dots;
  - nothing is stored or compared;
  - "Forgot" says "Contact support".
- The `BiometricSetup` route is used both for setup and for unlock, and `onUnlocked` goes straight to `VaultGrid`.
- `RoomVaultRepository.unlock()` only sets `isUnlocked = true`, and nothing calls it from the unlock flow.
- `VaultGrid` and `VaultViewer` entries have no guard, so after process death the saved back stack reopens them.
- `vaultLockTimeoutSeconds` is never used.

**Fix direction:**
- **05a:**
  - Separate **setup** (first time: choose PIN or pattern, confirm it, optionally enable biometrics) from **unlock**.
  - One unlock path: VM → `VaultRepository.unlock(credential)` → verify → `vaultCryptoEngine.unlockSession()` → `isUnlocked = true`.
  - `VaultGrid` and `VaultViewer` entries check `isUnlocked`. If it's false, `replaceTop(AppRoute.VaultLock)`.
  - Lock on process start and when the app is backgrounded longer than `vaultLockTimeoutSeconds`.
  - Tests with the vault and biometric fakes.
- **05b:**
  - Store only a salted hash of the PIN or pattern (the pattern as its dot sequence): PBKDF2-HMAC-SHA256, 16-byte random salt, ≥100k iterations, constant-time compare.
  - Store hash, salt, iterations and mode in `app_settings` (schema change → Migration 5→6 + JSON) or in a separate table.
  - Rate-limit after 5 wrong attempts.
  - Replace "Contact support": forgot = biometric re-auth if enrolled; otherwise explain the vault can't be recovered.
- **Device checks:**
  - a wrong PIN or pattern is rejected;
  - kill the app inside the vault → relaunch → locked;
  - background past the timeout → locked.

### Step 06 — Vault encryption on Android

**Gated. Highest-risk step. Bring the review to the second-opinion reviewer before approving.**

**Problem:**
- `AndroidVaultCryptoEngine.encryptToVault` copies plaintext into `filesDir/vault_blobs`;
- `decryptFromVault` reads raw bytes, and `thumbnail = true` returns the full file;
- `restoreToStorage` copies the blob as-is;
- `unlockSession` and `lockSession` are empty.

**Fix direction:**
- Tink `StreamingAead` (AES-GCM-HKDF streaming), with the keyset wrapped by an Android Keystore key that requires user auth:
  - API 30+: `setUserAuthenticationParameters(timeout, BIOMETRIC_STRONG or DEVICE_CREDENTIAL)`;
  - API 26–29: `setUserAuthenticationValidityDurationSeconds`.
  - Plain Keystore AES-GCM with chunking is acceptable instead.
  - Never load whole videos into memory.
- File header (magic bytes + format version), so encrypted files and legacy plaintext files can be told apart. Write to temp → fsync → rename. Verify that the file decrypts before deleting the source or legacy file.
- Encrypted thumbnails, stored separately.
- `restoreToStorage` decrypts as it writes to MediaStore.
- One-time migration of existing plaintext blobs on first unlock after the update.
- `lockSession` drops key material and any cached decrypted bytes.
- Tests: header parsing and the migration decision logic, with fakes.
- **Device checks:**
  - vault a photo; the blob isn't a readable image (`adb shell run-as`);
  - it opens after unlock;
  - restore works;
  - a legacy item still opens after migration.

### Step 07 — iOS storage location and encryption

**Gated.**

**Problem:**
- `AppDirectories.ios.kt` and `DatabaseBuilder.ios.kt` use `NSDocumentDirectory`, so the vault, trash, scans and the DB (which holds all OCR text) get backed up to iCloud.
- `IosVaultCryptoEngine` writes plaintext with `writeToFile`.
- `IosStubSecureScreenPort` is a stub.

**Fix direction:**
- Move everything to Application Support:
  - `NSURLIsExcludedFromBackupKey` for vault and trash;
  - `NSFileProtectionComplete`;
  - a one-time move from `Documents`.
- Keychain key (`kSecAttrAccessibleWhenUnlockedThisDeviceOnly`, `.biometryCurrentSet` or `.userPresence`).
- AES-GCM via `cryptography-kotlin` (Apple provider) or a small Swift `VaultCipher` injected into Kotlin. Use the same file format as 06 if practical.
- **Device check (device or simulator):** vault a photo; the file isn't readable; it opens after Face ID or passcode; nothing is in `Documents`.

### Step 08 — Screen privacy and biometric prompt

**Gated.**

**Problem:**
- `AndroidSecureScreenPort.setSecureScreen` is empty and never called.
- `AndroidBiometricAuthenticator`:
  - fails on the first wrong finger (`onAuthenticationFailed` resumes) while the prompt stays open;
  - the pre-check and the prompt use different authenticators;
  - its strings are hardcoded.
- `MainActivity` clears the static `currentLauncher` and `currentActivityProvider` only in `onDestroy` when finishing.

**Fix direction:**
- `FLAG_SECURE` on the vault lock, grid and viewer (on while shown, off when leaving). On iOS, blur the window on resign-active while the vault is open.
- Biometric:
  - `onAuthenticationFailed` doesn't resume;
  - `setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)`, with no negative button;
  - matching pre-check;
  - strings in resources.
- **Device checks:**
  - the vault is black in Recents and screenshots are blocked;
  - wrong finger then right finger unlocks;
  - device PIN fallback works.

---

## Phase C — Correctness

Not gated. **Ends with a human merge.**

### Step 09 — Delete, trash and restore

**Problem:**
- `RoomTrashRepository.moveToTrash`: if `copyToTrash` fails, it stores `item.uri` (the original) as the trash copy. Then the original is deleted, so the Bin item points at nothing.
- `restoreFromTrash`:
  - writes `origin = "GALLERY"` (not a `MediaOrigin` value);
  - uses `deletedAtMs` as the photo date and `mime = "image/*"`;
  - points `ref` at the private trash copy;
  - removes the exclusion by the trash path instead of the original URI;
  - never puts the file back into the system gallery.
- `deleteFromTrash` leaves the file on disk, and there's no auto-purge.
- `DeleteMediaUseCase` copies to trash before the user consents; the rollback leaves files behind.
- Vault and gallery:
  - `VaultGridViewModel` ignores `restore` and `delete` results, so failures are silent;
  - `restoreToStorage` on Android ≤ 9 writes to public storage without `WRITE_EXTERNAL_STORAGE` (needs `maxSdkVersion="28"`).
- `PhotoViewModel` and `SearchViewModel` ignore the delete result.

**Fix direction:**
- If the trash copy fails, abort the delete for that item and report it.
- Store the original URI, mime and taken date in `trash_item` (Migration + JSON).
- Restore with the same MediaStore insert that the vault restore uses. Extract it into a shared `MediaRestoreGateway`.
- Delete files on purge and on rollback, and auto-purge after 30 days.
- Show errors from vault restore and delete.
- An in-flight guard against double-tap delete.
- **Device check:** delete → Bin → restore → the photo is back in the system gallery app with the right date.

### Step 10 — Indexing quality: classification, entities, OCR boxes

**Problem:**
- `SmartMediaClassifier` and `EntityExtractor` are never called during indexing, so nothing becomes `RECEIPT` or `DOCUMENT`, and `entityFlags` is always 0. This weakens voice search hints, collections and filters.
- `EntityExtractor`:
  - `rawValue` is the whole OCR text, not the match;
  - only phone and email are implemented;
  - the phone regex is US-only (Pakistani `0300-1234567` / `+92 …` formats don't match).
- The classifier matches substrings ("totally" → "total").
- Screenshot detection uses the file name only.
- OCR stores `blocksJson = "[]"`, and `observeOcrLines` returns an empty list, so viewer highlighting can't work.
- Already-indexed items are skipped forever, so better indexing never reaches them.

**Fix direction:**
- Call the classifier and extractor per image in `rebuild`, refining `PHOTO` or `SCREENSHOT` to `RECEIPT` or `DOCUMENT`, and setting `entityFlags`.
- Extractor:
  - return the matched substring;
  - add URL;
  - international and PK phone patterns.
- Word-boundary matching in the classifier.
- Screenshot detection also checks `RELATIVE_PATH` contains `Screenshots` on API 29+.
- Store ML Kit line boxes (normalized) in `blocksJson`, and implement `observeOcrLines`.
- Add an `indexVersion` column to `media_text` (Migration + JSON), and re-index rows below the current version.
- Tests: extractor and classifier cases, including PK numbers, URLs and word boundaries; the re-index decision.

---

## Phase D — Performance and indexing pipeline

Not gated. **Ends with a human merge.**

### Step 11 — One indexing pipeline

**Problem:**
- Indexing starts twice on every launch: `MainActivity.startIndexing()` starts `IndexingForegroundService`, and `App.kt` also calls `indexRepository.rebuild()`. The mutex serialises them, but `indexedThisSession` is never read, so the second run repeats the full MediaStore scan and per-item DB checks.
- `rebuild`:
  - the `RUNNING` check inside the mutex can never be true;
  - a cancellation or exception leaves the state `RUNNING` (no `finally`);
  - `deleteNonExistingImages(... NOT IN (:list))` binds one parameter per photo, which breaks SQLite's variable limit on large libraries;
  - it upserts one row at a time with a `getById` per item;
  - it uses `catch (_: Throwable)`.
- The foreground service:
  - type `dataSync`, `targetSdk 36`: Android 15+ caps `dataSync` at 6 hours a day and requires `onTimeout()` to stop the service; otherwise the app crashes;
  - hardcoded `Dispatchers.Default`;
  - `START_STICKY` reruns the whole scan after the process dies;
  - the notification icon is `stat_sys_upload` (an "uploading" icon, in an app that promises photos never leave the phone);
  - no `POST_NOTIFICATIONS` request on 13+.
- `WorkManagerSyncScheduler` is empty, so the "index only while charging" setting does nothing.
- The splash is a fixed 3 seconds.

**Fix direction:**
- Exactly one trigger: either a unique `WorkManager` job (with charging constraint from settings, foreground info for long runs), or keep the foreground service with `onTimeout`, `START_NOT_STICKY` and a non-upload icon. Pick one in the spec and justify it. Remove the `App.kt` call.
- Incremental sync: a checkpoint in `sync_checkpoint`; deletions diffed in Kotlin and deleted in chunks of ≤500.
- `try`/`finally` returns the state to `IDLE`; catch `Exception`, rethrow cancellation.
- Batched writes in transactions.
- The splash stays only until the start route is known.
- Tests: the state machine always ends `IDLE`; the checkpoint and diff logic.

### Step 12 — Search on FTS5

**Problem:**
- `RoomSearchRepository.search`:
  - loads every row (`getAll()`), then one `getText` per row;
  - scores with edit distance in Kotlin, up to 100 hits;
  - runs on the caller's dispatcher.
- The FTS5 table `media_fts` is maintained by triggers but never queried.
- `VectorSearchEngine` is a fake embedding (word hash → `sin`), but it's 40% of the fuzzy score and is computed for every image during indexing.
- Voice search calls this search up to 3 times per query.
- The Photos screen's inline search filters `state.items` in composition, by substring, on every recomposition.
- `SearchViewModel` loads the whole library into `allImages`, and searches can race (no cancel or `flatMapLatest`).
- `MediaItem` carries `embedding` into UI state.

**Fix direction:**
- `media_fts` `MATCH` with escaped terms and prefix matching (`term*`), ranked with `bm25()`, joined to `media_item`, with `LIMIT`, plus the category filter from Step 03.
- Keep a small edit-distance fallback only when FTS returns nothing (voice transcripts mishear names).
- Stop generating and using embeddings, and remove `embedding` from `MediaItem`. Dropping the column can wait.
- `SearchViewModel`: `debounce` + `flatMapLatest`.
- The Photos inline search uses the repository, or `remember`s its filtering.
- Tests: the FTS query builder (escaping, prefixes, empty input); VM debounce and cancellation.

---

## Phase E — Cleanup, privacy claim, tooling

Not gated. **Ends with a human merge.** Split into small specs.

### Step 13 — Voice search follow-ups
- **Architecture:** `:features:aisearch` now depends on `:platform` (for `rememberMicrophonePermission`), against the `AGENTS.md` rule. Move the permission composable behind a port, or into `:core:designsystem` with platform actuals, or let `composeApp` provide it. Remove the dependency.
- **Language model fallback:** on-device recognition can report available while the language model isn't downloaded (`ERROR_LANGUAGE_UNAVAILABLE` → `NOT_AVAILABLE_OFFLINE`). Then:
  - retry once with the system recognizer if the user has accepted that, or
  - on API 33+, use `checkRecognitionSupport` / `triggerModelDownload`.
- **Big file:** split `VoiceSearchScreen.kt` (624 lines) into components.

### Step 14 — Privacy claim and hygiene
The new claim is "your media is never exposed to the internet". Make the code back it up:
- Remove the media URI logs in `AndroidMediaDeleteGateway` (`Log.d("DeleteGateway", "ref=$s")` and `"resolved=$resolved"`).
- `AGENTS.md` rule: no network code may read or send media, OCR text, transcripts or file paths. The reviewer checks it every step.
- When a feature needs network, remove the `tools:node="remove"` on `INTERNET` / `ACCESS_NETWORK_STATE` in that feature's step, and say why in the report.
- Replace the hardcoded dispatchers:
  - `IndexingForegroundService`
  - `AndroidThumbnailMaker`
  - `MediaStoreReceivedPhotoStore`
  - `NearbyTeleportTransport` ×2
  - `AndroidPlatformAdapters` ×3
  - `IosMediaDeleteAndShareGateways`
- Replace 12 `catch (_: Throwable)` blocks with `Exception`, rethrowing cancellation.

### Step 15 — Architecture leftovers
- `:androidApp` depends only on `:composeApp` (it also depends on `:core:common`, `:core:designsystem`, `:domain`, `:platform`).
- `:features:onboarding` stops importing `AppPermissionGate` from `:platform` (the known exception in `AGENTS.md`).
- Remove the Koin module from `:core:common`.
- Replace the static companion bridges (`AndroidMediaDeleteGateway.currentLauncher`, `AndroidBiometricAuthenticator.currentActivityProvider`) with a lifecycle-aware, activity-scoped bridge.
- `VisionaryApp` uses `BuildConfig` for `isDebug` and the version.
- Room settings `update()` isn't atomic. Wrap the read-transform-write in a transaction.
- Delete dead code: `IndexJobDao`, `SyncCheckpointEntity` if unused after Step 11, `searchByLabelsExact`, `getImagesWithEmbeddings`, `indexedThisSession`.

### Step 16 — Teleport and permissions
- Show and confirm the Nearby `authenticationDigits` before `acceptConnection` (it currently accepts immediately), and enforce a max payload size.
- `ACCESS_FINE_LOCATION` is requested on every API level. Cap it with `maxSdkVersion` where Nearby no longer needs it (verify against current Nearby docs).

### Step 17 — Tooling and code health
- ktlint + detekt with a baseline, a module-graph / forbidden-import check (features ↛ `:data`, `:core:database`, `:platform`), and GitHub Actions running `testAndroidHostTest` and `assembleDebug` on every PR.
- Move strings to resources (78 literal `Text("…")` calls, plus view model strings).
- Split the large files:
  - `SearchScreen` (862 lines)
  - `AndroidPlatformAdapters` (743), by port
  - `RoomRepositories` (694), one file per repository
  - `BiometricScreen` (461)
- Swap `material-icons-extended` for the icons actually used.
- **Human:** make `Develop` the default branch.

---

## Deferred (add to a step when it fits)
- The document scanner (`MlKitDocumentScannerAdapter.scan()`) is still a stub.
- OCR is Latin-script only. Decide whether Urdu matters for the audience.
- `OnboardingViewModel` sets `permissionGranted = true` without checking the result.
- Teleport's gesture analyzer logs `e.message`. That's fine as long as messages never contain paths. Check during Step 14.
