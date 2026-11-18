# Fix plan

The source of truth for what to fix, in order. The reviewer writes each step spec from this file. Read `WORKFLOW.md` for how the loop works.

Findings come from a code review of `Develop` at `67767e3` (17 Nov 2026). Line numbers and code may have moved since; always read the current code before writing a spec.

**Gated** steps stop for a human device check before the next step starts (see `WORKFLOW.md`).

---

## Done

| Step | Title | Notes |
|---|---|---|
| 01 | Stop silent data loss | Destructive migration removed (only versions 1–2 may be wiped); Search delete is real; `DeleteMediaUseCase` rethrows cancellation; vault delete removes the blob; vault restore returns `VAULT_RESTORE_NOT_IMPLEMENTED`. |
| 02 | Persist settings | `DataStoreSettingsRepository` (keys `onboarding_done`, `index_only_while_charging`, `vault_lock_timeout_seconds`). One DataStore via Koin in `:platform`. iOS file in Application Support. |
| 02b | Use onboarding flag at startup | `App.kt` chooses the start route from `ResolveStartRouteUseCase` + media access; `AppRoute.Onboarding(skipIntro)` goes straight to the permission gate. |

---

## Step 03 — Category mapping (collections, gallery, search filters)

**Not gated.**

**Problem.**
- Stored `media_item.category` values are `MediaCategory` names, which are singular: `PHOTO`, `VIDEO`, `SCREENSHOT`, `DOCUMENT`, `RECEIPT`, `SCAN`.
- `CollectionKind` names are plural and include `ALL`: `ALL`, `SCREENSHOTS`, `DOCUMENTS`, `RECEIPTS`, `SCANS`.
- `RoomMediaRepository.observeCollections()` looks up counts by `kind.name`, so every count is 0, including `ALL`.
- `observeMedia(kind)` calls `observeByCategory(kind.name)`, so category galleries are always empty.
- `RoomSearchRepository.search` filters with `"SCREENSHOTS"`, `"RECEIPTS"` and so on, so filtered searches return nothing.
- `SearchFilter.HAS_PHONE_OR_URL` is mapped to `"ALL"`, so it filters nothing.

**Fix direction.**
- Add one explicit mapping in `:domain`, e.g. `CollectionKind.categories(): Set<MediaCategory>` (`ALL` = every category; `SCREENSHOTS` = `{SCREENSHOT}`, etc.), and `SearchFilter.categories()`.
- DAO: `observeByCategories(categories: List<String>)` with `WHERE category IN (:categories)`. `ALL` uses `observeAll()`.
- `observeCollections`: `ALL` = total count; every other kind = sum of its categories' counts. Pick a cover URI if cheap (newest item); otherwise leave `null`.
- `HAS_PHONE_OR_URL`: filter on `entityFlags` with the PHONE and URL bits. It only returns results after Step 09 sets `entityFlags`; say that in the spec.
- Tests: pure mapping tests in `:domain`. Repository changes can't be host-tested (Room), so add device checks: the Screenshots collection shows screenshots, and Home counts are non-zero.

---

## Step 04 — Vault lock (real auth, PIN, route guard)

**Gated.** Likely split into **04a** (auth flow and route guard) and **04b** (PIN).

**Problem.**
- `BiometricViewModel.OnUnlockClicked` accepts any PIN of 4+ digits. Nothing is stored or compared.
- `OnForgotPinClicked` says "Contact support to reset PIN" (an offline app has no support).
- `VaultViewModel.OnUnlockClicked` sends `OpenVaultGrid` without authenticating.
- `RoomVaultRepository.unlock()` only sets `isUnlocked = true`. It never calls `BiometricAuthenticator` or `VaultCryptoEngine.unlockSession()`.
- `AppRoute.VaultGrid` and `AppRoute.VaultViewer` have no guard. After process death, the restored back stack can reopen the vault with no auth.
- `vaultLockTimeoutSeconds` (persisted since Step 02) is never used.

**Fix direction.**
- **04a:**
  - Unlocking goes through one path: VM → `VaultRepository.unlock()` → authenticator (biometric or PIN) → `vaultCryptoEngine.unlockSession()` → `isUnlocked = true`.
  - Vault grid and viewer entries check `isUnlocked`. If locked, replace the top of the stack with `VaultLock`.
  - Auto-lock when the app goes to the background for longer than `vaultLockTimeoutSeconds`, and on process start (default locked).
  - Tests with `FakeBiometricAuthenticator`, `FakeVaultCryptoEngine`, `FakeVaultRepository`.
- **04b:**
  - PIN setup flow (first time) versus PIN unlock.
  - Store only a salted hash: PBKDF2-HMAC-SHA256, random 16-byte salt, ≥100k iterations, constant-time compare. Store hash, salt and iterations in DataStore (new keys).
  - Use a KMP crypto library (e.g. `cryptography-kotlin`) or `expect`/`actual` (Android `SecretKeyFactory("PBKDF2WithHmacSHA256")`, iOS CommonCrypto `CCKeyDerivationPBKDF`).
  - Rate-limit wrong attempts (e.g. a delay after 5 failures).
  - Remove the "contact support" text. Forgot PIN = biometric re-auth if available; otherwise explain the vault can't be recovered.
- **Device checks:**
  - The vault can't be opened without auth.
  - A wrong PIN is rejected.
  - Kill the app while in the vault → relaunch → locked.
  - Background past the timeout → locked.

---

## Step 05 — Vault encryption on Android

**Gated. Highest risk step: a bug here can make vaulted photos unreadable.**

**Problem.**
- `AndroidVaultCryptoEngine.encryptToVault` copies the plaintext file into `filesDir/vault_blobs`.
- `decryptFromVault` reads it back raw.
- `unlockSession` and `lockSession` are empty.

**Fix direction.**
- Encrypt with a key the user's authentication protects:
  - Preferred: Google Tink `StreamingAead` (AES-GCM-HKDF streaming), with the keyset encrypted by an Android Keystore master key (`setUserAuthenticationRequired(true)`).
  - API 30+: `setUserAuthenticationParameters(timeout, BIOMETRIC_STRONG or DEVICE_CREDENTIAL)`. Below 30: `setUserAuthenticationValidityDurationSeconds`. minSdk is 26.
  - Alternative: plain Keystore AES-256-GCM, with chunked encryption for large files. Don't load whole videos into memory.
- File format:
  - A small header (magic bytes + format version) so encrypted and legacy plaintext files can be told apart.
  - Write to a temp file, `fsync`, rename.
  - Verify the new file decrypts before deleting anything.
- Encrypted thumbnails (`decryptFromVault(thumbnail = true)` currently returns the full file).
- **Migration of existing plaintext blobs:** on first unlock after the update, encrypt each legacy file in place (temp, verify, swap). Never delete a plaintext file before its encrypted copy is verified.
- `lockSession` must drop any in-memory key material and cached decrypted bytes.
- Tests:
  - Format and header parsing, and the migration decision logic, with fakes.
  - The Keystore itself needs a device. Add device checks:
    - Vault a photo, then confirm the file in `vault_blobs` isn't a readable image (`adb` run-as).
    - It opens after unlock.
    - A legacy plaintext item still opens after migration.

---

## Step 06 — Vault encryption and storage location on iOS

**Gated.**

**Problem.**
- `IosVaultCryptoEngine` writes plaintext with `writeToFile`.
- `AppDirectories` (iOS) and `DatabaseBuilder.ios.kt` use `Documents`, which iOS backs up to iCloud. Vaulted photos and the database go to iCloud in plaintext.
- `IosStubSecureScreenPort` and `IosStubBackgroundSyncScheduler` are stubs.

**Fix direction.**
- Move `vault_blobs`, `trash_blobs`, `scans` and the Room database to Application Support, with:
  - `NSURLIsExcludedFromBackupKey` = true for vault and trash,
  - `NSFileProtectionComplete`.
  - Migrate existing files from `Documents` once.
- Key in the Keychain:
  - `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`,
  - access control `.biometryCurrentSet` (or `.userPresence` if a PIN fallback is wanted).
- AES-GCM is not public in CommonCrypto. Choose and justify one in the spec:
  - `cryptography-kotlin` with its CryptoKit/Apple provider, or
  - a small Swift `VaultCipher` implemented in `iosApp` and injected into Kotlin through an interface.
- Same file header and format version as Step 05, if practical.
- Device check (simulator or device): vault a photo; the file isn't a readable image; the app opens it after Face ID or passcode.

---

## Step 07 — Screen privacy and biometric prompt fixes

**Gated.**

**Problem.**
- `AndroidSecureScreenPort.setSecureScreen` is empty and nothing calls it, so vault contents show in Recents and screenshots.
- `AndroidBiometricAuthenticator`:
  - `onAuthenticationFailed` resumes with an error on the first wrong finger, while the prompt stays open. A later success is then ignored.
  - The pre-check uses `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`, but the prompt doesn't set allowed authenticators, so it defaults to biometric with a Cancel button.
  - The prompt strings are hardcoded.

**Fix direction.**
- `setSecureScreen` sets or clears `WindowManager.LayoutParams.FLAG_SECURE` on the current activity. Vault lock, grid and viewer screens enable it on enter and disable it on leave.
- iOS: cover the window with a blur or solid view when the app resigns active, while the vault is open.
- Biometric:
  - `onAuthenticationFailed` must not resume (it's a soft failure).
  - Use `setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)`, with no negative button (not allowed with device credential).
  - Match the pre-check to that.
  - Move strings to resources.
- Device checks:
  - The vault is black in Recents and screenshots are blocked.
  - A wrong finger, then the right finger, unlocks.
  - Device PIN works as a fallback.

---

## Step 08 — Trash fixes and restore-to-gallery

**08a not gated. 08b gated** (it touches the vault).

**Problem.**
- `RoomTrashRepository.restoreFromTrash`:
  - deletes the index exclusion keyed by the trash-copy path, but the exclusion was stored under the **original** URI, so restored items stay excluded from indexing;
  - writes `origin = "GALLERY"` (not a `MediaOrigin` value);
  - points `ref` at the app-private trash copy;
  - uses `deletedAtMs` as the photo date;
  - never puts the file back in the system gallery.
- `deleteFromTrash` leaves the trash file on disk.
- There's no auto-purge.
- `DeleteMediaUseCase` copies to trash before the user consents, and the rollback leaves the copied file behind.
- Double-tapping delete in Search starts two deletes.
- Vault restore returns `VAULT_RESTORE_NOT_IMPLEMENTED` (Step 01).

**Fix direction.**
- **08a:**
  - Store the original URI, taken date and mime in `trash_item` (schema change → Migration 3→4 + schema JSON).
  - Delete the trash file in `deleteFromTrash` and on rollback.
  - Remove the exclusion by the original URI.
  - Auto-purge items older than 30 days (on app start for now; WorkManager in Step 11).
  - In-flight guard for delete in Search.
- **08b:**
  - New port `MediaRestoreGateway.restoreToGallery(sourcePath, displayName, mime, takenAtMs): AppResult<String>`. Android: `MediaStore` insert with `IS_PENDING`. iOS: `PHPhotoLibrary`.
  - Use it for trash restore and to implement vault restore (decrypt → restore → delete blob and row only after success).
- Device checks: delete → Bin → restore → the photo is back in the system gallery app, and searchable after re-index.

---

## Step 09 — Indexing quality: classification and entities

**Not gated.**

**Problem.**
- `SmartMediaClassifier` and `EntityExtractor` are registered in Koin but never called during indexing. Nothing becomes `RECEIPT` or `DOCUMENT`, and `entityFlags` is always 0.
- `EntityExtractor`:
  - `rawValue` is the **whole** OCR text, not the matched value;
  - only phone and email are implemented, of 7 `SmartEntityType`s;
  - the phone regex is US-only, so Pakistani formats like `0300-1234567` or `+92 300 1234567` don't match.
- The classifier uses substring matching, so "totally" matches "total" and becomes a receipt.
- Screenshot detection is by file name only (Android `AndroidMediaStoreSource`).
- `MlKitOcrEngine` returns no line boxes, `blocksJson` is always `"[]"`, and `observeOcrLines` returns an empty list, so the OCR highlight overlay can never work.
- Already-indexed items are skipped forever, so better indexing never reaches them.

**Fix direction.**
- Call the classifier and extractor in `RoomIndexRepository.rebuild` per image. Keep `VIDEO` and `SCREENSHOT` as base categories; refine `PHOTO` and `SCREENSHOT` to `RECEIPT` or `DOCUMENT`.
- Extractor:
  - return the matched substring;
  - add URL (and amount if simple);
  - international phone patterns including PK mobile and landline.
  - Classifier: word-boundary matching.
- Screenshots: also check `RELATIVE_PATH` contains `Screenshots` on API 29+.
- OCR: map ML Kit lines to `OcrTextLine` with normalized boxes, store them in `blocksJson` (kotlinx.serialization), implement `observeOcrLines`.
- Add an `indexVersion` column to `media_text` (Migration + schema JSON). Re-index rows below the current version.
- Tests: extractor and classifier cases (PK numbers, URL, word boundaries); the re-index decision logic with fakes.

---

## Step 10 — Search on FTS5 and query performance

**Not gated.**

**Problem.**
- `RoomSearchRepository.search` loads **every** row plus its text per query and scores in Kotlin with edit distance. It runs on the collector's dispatcher, which is Main from `viewModelScope`.
- An FTS5 table `media_fts` is created and kept up to date by triggers in `FtsCallback`, but no query ever reads it.
- `VectorSearchEngine` is a fake embedding (word hash → `sin`), with no semantic meaning; it is 40% of the fuzzy score.
- N+1 queries: every list emission calls `getText(id)` once per row (`observeMedia`, `observeItem`, `observeRecentFinds`, search).
- Home requests `observeRecent(limit = Int.MAX_VALUE)`. During indexing each upsert re-emits `observeAll`, which is roughly O(N²) queries.
- `SearchViewModel`:
  - loads the whole library into `allImages`;
  - `executeSearch` launches new coroutines without cancelling the previous one, so stale results can overwrite fresh ones;
  - changing mode or filter triggers two searches.
- `MediaItem` carries the `embedding` `ByteArray` into UI state.

**Fix direction.**
- Search through `media_fts`:
  - `MATCH` with escaped user terms and `term*` prefix matching, ranked by `bm25()`, joined to `media_item`, `LIMIT`;
  - category filter from Step 03.
  - Keep `SearchMode` only if the modes still mean something; otherwise simplify and update the UI.
- Remove `VectorSearchEngine` usage and stop writing embeddings. Leave the column (dropping it needs a migration; do it later if wanted).
- One JOIN query (or Room `@Relation`) for item + text. Remove every per-row `getText`.
- Home recent finds: a real `LIMIT` (e.g. 30) in SQL.
- `SearchViewModel`: a single query flow with `debounce` + `flatMapLatest`, so there's one search at a time. Drop `allImages` if unused.
- Remove `embedding` from `MediaItem`.
- Tests: FTS query-string builder (escaping, prefixes); VM debounce and cancellation with `FakeSearchRepository`. SQL needs a device test or manual check.

---

## Step 11 — Indexing pipeline: WorkManager, incremental, robust

**Not gated.**

**Problem.**
- Indexing starts twice on every launch (`MainActivity.startIndexing` and `App.kt`). It runs in `lifecycleScope`, so it's cancelled when the user leaves, and it does a full MediaStore scan every time (`queryDelta(0L)`).
- `RoomIndexRepository.rebuild`:
  - `indexedThisSession` is written but never read;
  - the `RUNNING` check inside the mutex can never be true;
  - a cancellation leaves the state `RUNNING` forever;
  - upserts are one row at a time with no transaction;
  - `catch (_: Throwable)` swallows everything;
  - `deleteNonExistingImages(... NOT IN (:list))` binds one parameter per photo and can exceed SQLite's variable limit on large libraries.
- `WorkManagerSyncScheduler` is empty, so the "index only while charging" setting does nothing.
- `SyncCheckpointEntity` and `IndexJobDao` are unused.
- The splash is a fixed 3 seconds on every launch.

**Fix direction.**
- One trigger: a unique WorkManager `CoroutineWorker` (keep-existing policy) with charging constraint from settings. Remove both direct `rebuild()` calls.
- Incremental: a checkpoint (`DATE_ADDED` or MediaStore generation) in `sync_checkpoint`. Deletions via `queryAllLibraryIds()` diffed in Kotlin, deleted in chunks of ≤500 ids.
- `try`/`finally` so state always returns to `IDLE`; rethrow `CancellationException`; catch `Exception`, not `Throwable`.
- Batch writes inside `useWriterConnection`/transactions.
- iOS: keep the scheduler stub but make indexing run on demand; `BGTaskScheduler` later.
- Splash: show it only until the start route is known (minimum display time optional, short).
- Tests: checkpoint and diff logic, the state machine (always ends `IDLE`), with fakes.

---

## Step 12 — Hardening and cleanup

**Not gated, except 12e** (merge readiness). Split into small specs:

- **12a Privacy and correctness leftovers:**
  - remove the URI logs in `AndroidMediaDeleteGateway` (`Log.d("DeleteGateway", "ref=$s")` / `resolved=`);
  - replace the hardcoded dispatchers (`NearbyTeleportTransport`, `AndroidThumbnailMaker`, `MediaStoreReceivedPhotoStore`, `AndroidPlatformAdapters`, `IosMediaDeleteAndShareGateways`) with `AppDispatchers`;
  - `coreCommonModule`'s scope should use `AppDispatchers`.
- **12b Lifecycle-safe platform bridges:** replace the static companion `var`s (`AndroidMediaDeleteGateway.currentLauncher` / `pendingDeleteContinuation`, `AndroidBiometricAuthenticator.currentActivityProvider`) with an activity-scoped bridge (`ActivityResultRegistry` / lifecycle-aware holder) that survives rotation and doesn't leak the Activity.
- **12c Architecture:**
  - merge `:core:testing` into `:core:testkit`; fake-data mode only in debug, not a production dependency of `:composeApp` / `:features:teleport`;
  - `:androidApp` depends only on `:composeApp` (+ Koin Android);
  - `:features:onboarding` stops importing `AppPermissionGate` from `:platform`;
  - remove the Koin module from `:core:common`;
  - delete dead DAO methods (`searchByLabelsExact`, `getImagesWithEmbeddings`, `getAllTextEntities`) and anything else unused;
  - `VisionaryApp` uses `BuildConfig` for `isDebug` and `versionName`;
  - `KmpLibraryConventionPlugin`: typed AGP KMP Android API instead of reflection.
- **12d Tooling and code health:**
  - ktlint + detekt with a baseline, a module-graph / forbidden-import check, and GitHub Actions CI running tests and `assembleDebug`;
  - strings to resources (~76 literal `Text("…")`, plus strings in view models);
  - split large files (`SearchScreen` 851 lines, `HomeScreen` 649, `AndroidPlatformAdapters` 640, `RoomRepositories` 566);
  - drop `material-icons-extended` for the icons actually used;
  - typed `AppError` reasons instead of raw exception messages shown to users.
- **12e Teleport and permissions:**
  - show and confirm Nearby `authenticationDigits` before accepting a connection; enforce a max payload size;
  - cap `ACCESS_FINE_LOCATION` with `maxSdkVersion` where Nearby no longer needs it (verify against current Nearby docs);
  - `NEARBY_WIFI_DEVICES` should start at API 33.
  - Human: make `Develop` the default branch, and merge `review-fixes`.

---

## Deferred notes (add to a step when it fits)

- DataStore is pinned at 1.1.3; bump to the latest stable when convenient.
- `features/mlKit` has 3 commits not in `Develop` ("Performance Optimization"). The human decides whether they're wanted.
- `MlKitDocumentScannerAdapter.scan()` is a stub returning an empty document; the scanner feature doesn't work. Needs its own step, after Step 11.
- OCR is Latin-script only (`TextRecognizerOptions.DEFAULT_OPTIONS`). Urdu isn't supported by ML Kit text recognition. Decide whether that's acceptable for the audience.
- `OnboardingViewModel.OnGrantPermissionClicked` sets `permissionGranted = true` without checking the result of `requestAccess()`, which doesn't request anything (it only reads the current state).
