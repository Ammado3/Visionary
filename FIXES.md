# Visionary fixes

One file for the whole fix plan: how to work, where we are, and every step. It was written against `review-fixes` at `a448af9` (20 Nov 2026). Code moves, so read the current code before changing it.

---

## How to work

**Branch:** `review-fixes`. Start every session with `git checkout review-fixes && git pull`. Never merge into `Develop` and never force-push. The human merges.

**Roles:**

| Role | Does | Never does |
|---|---|---|
| Implementer (Gemini) | Implements the step named under **Status**, runs the checks, writes the step's report | Reviews its own work, starts the next step |
| Reviewer (Claude) | Reviews the step: re-runs the checks, reads the diff, writes the step's review | Writes production code (except merges of `Develop`) |
| Human | Starts each agent, does device checks, merges into `Develop` | — |

**Prompt for both agents:** `Read docs/FIXES.md and do your part.` If **Status** says it isn't your turn, say so and stop.

**Rules for every step:**
- Read `AGENTS.md` first. Its architecture and database rules apply.
- Do only the current step. Anything else you notice goes under "Noticed" in the report.
- Commits: one per task, message `Fix N.M: <what>` (e.g. `Fix 2.3: Batch media upserts`). Fixes after a review: `Fix N review: <what>`.
- Checks: `./gradlew testAndroidHostTest` and `./gradlew :androidApp:assembleDebug` must pass. Report the real number of tests from the test report, not the Gradle task count.
- **Never claim a check you didn't run.** Device checks you couldn't do: write "not run" and why. Never claim a push that didn't succeed.
- Push at the end of every turn: `git push origin review-fixes`. If it fails, say so.
- **Vault is out of scope** (a teammate owns it): don't edit `features/vault`, `VaultCryptoEngine` implementations, `RoomVaultRepository`, `AndroidBiometricAuthenticator`, or iOS vault adapters. If a step seems to need it, stop and tell the human.
- **Database:** no destructive fallback for any new version. A schema change bumps the version, adds a `Migration`, and commits the exported schema JSON.
- **Privacy claim, "your media is never exposed to the internet":** never log or send media URIs, file paths, OCR text or voice transcripts.

**Writing the report (Implementer):** at the end of the step, add a `#### Report` section under it:
- commit hashes;
- deviations, with reasons;
- check results, with the test count;
- device checks: done or "not run";
- noticed but not touched.

Then set **Status** to `Next: Reviewer`. Commit and push.

**Writing the review (Reviewer):**
1. `git fetch` and merge `origin/Develop` into `review-fixes` if it has new commits (`Sync: merge Develop`). Resolve conflicts without adding behaviour, and rerun the checks.
2. Re-run the checks yourself. Read the diff against the step's tasks and the rules above.
3. Add `#### Review` under the step: **Approved** or **Changes needed**, with file:line for each problem.
4. Set **Status**:
   - changes needed → `Next: Implementer, fix Step N`;
   - approved, and the step has a device check → `Next: Human`;
   - otherwise → `Next: Implementer, Step N+1`.
5. Commit and push.

**Human sign-off:** do the device check, add a line under **Sign-off**, set **Status** to the next step, push. Merge `review-fixes` into `Develop` at the merge points marked below.

---

## Status

**Next:** Reviewer
**Action:** Step 0 (review Step 03 and the 11a follow-up check), then review Step 1
**Last approved commit:** `a448af9`

### Log

| Step | Title | State |
|---|---|---|
| 0 | Review Step 03 (category mapping) | Next |
| 1 | One indexing trigger | Done, awaiting review |
| 2 | Incremental indexing | — |
| 3 | Delete, trash and restore | — |
| 4 | Classification, entities, OCR boxes | — |
| 5 | Search on FTS5 | — |
| 6 | Voice search follow-ups | — |
| 7 | Privacy and hygiene | — |
| 8 | Architecture leftovers | — |
| 9 | Teleport and permissions | — |
| 10 | Tooling and code health | — |

**Merge points** (human merges `review-fixes` → `Develop`): after Step 2, after Step 5, after Step 10.

### Sign-off

- Steps 01, 02b (earlier plan): device checks still to do. Delete from Search and cancel the dialog; fresh install → onboarding → Photos; revoke the photo permission → relaunch → permission screen.
- Step 11a (earlier plan): launch check passed on Pixel 2 / Android 10. The Android 15 timeout wasn't checked.

### Already done (earlier plan, history in git)

- **01:** no blanket destructive fallback; Search delete really deletes; `DeleteMediaUseCase` rethrows cancellation; vault delete removes the file first.
- **02b:** the start screen uses the saved onboarding flag.
- **03:** category mapping (`CategoryMapping.kt`, `observeByCategories`, `matchesSearchFilter`). Not yet reviewed, so that's Step 0.
- **R0:** `Develop` merged in, and settings stay in Room.
- **11a:** `IndexRunGuard`, foreground service fixes, Android-only service trigger.

### Skipped on purpose

- **Real migrations 3→4→5.** Versions 1–4 still reset on upgrade. Any **new** schema change (Steps 3 and 4) needs a real `Migration` from 5.
- **Vault security** (lock, PIN, encryption, iOS storage, FLAG_SECURE, biometric prompt): owned by the vault teammate.
- **iOS doesn't compile** until the vault teammate adds `restoreToStorage` and `getBlobPath` to the iOS vault engines. Until then, iOS checks are "not run".

---

## Step 0 — Review Step 03 (Reviewer only)

Step 03 was merged without a review. Its code:
- `domain/.../model/CategoryMapping.kt` and `CategoryMappingTest`;
- `MediaDao.observeByCategories`;
- `RoomMediaRepository.observeMedia`;
- `matchesSearchFilter` in `RoomRepositories.kt`, and `SearchFilterMatchingTest`.

Check:
- every `CollectionKind` and `SearchFilter` maps to the right `MediaCategory` values;
- `HAS_PHONE_OR_URL` uses entity flags;
- the category gallery and filtered search no longer compare plural names with stored singular ones.

Write `#### Review` here. If approved, set Status to `Next: Implementer, Step 1`.

---

## Step 1 — One indexing trigger

**Problem:**
- Step 11a made the foreground service Android's trigger, but `features/onboarding/.../OnboardingEntries.kt` still calls `indexRepository.rebuild()` in the app scope after the permission gate.
- On a first launch this races with the service:
  - if onboarding wins, `IndexRunGuard` skips the service's call and the service stops, so the first and biggest index runs **outside** the foreground service, where Android can kill it;
  - if the service finishes first, onboarding starts a second full pass.
- `composeApp`'s `startsIndexingFromApp` expect/actual is a workaround for the missing port.
- A "Retry" or "Resume" tapped while a pass is running is silently skipped.

**Tasks:**
1. **Port.** In `domain/.../port/Ports.kt`, change `BackgroundSyncScheduler` to:
   ```kotlin
   interface BackgroundSyncScheduler {
       /** Starts an indexing pass now in the platform's right place (Android: foreground service). */
       fun requestIndexing()
   }
   ```
   Remove `scheduleIncrementalIndexing` (it's empty everywhere; charging-only comes back in Step 2).
2. **Android.**
   - Move `IndexingForegroundService` from `:androidApp` to `platform/src/androidMain/.../platform/android/`, keeping its 11a behaviour, and move `ic_stat_indexing.xml` with it.
   - Declare the service, its `dataSync` type and the `FOREGROUND_SERVICE*` permissions in `platform/src/androidMain/AndroidManifest.xml`, and remove them from the app manifest (manifest merging puts them back).
   - Replace `WorkManagerSyncScheduler` with `AndroidIndexScheduler(context)`. `requestIndexing()` calls `ContextCompat.startForegroundService(context, Intent(context, IndexingForegroundService::class.java))`.
   - `MainActivity.startIndexing()` calls the scheduler.
3. **iOS.** `IosIndexScheduler(indexRepository, appScope)`: `requestIndexing()` launches `indexRepository.rebuild()` in the app `CoroutineScope` from Koin.
4. **Callers.**
   - `App.kt` calls `scheduler.requestIndexing()` when the route is `Photos` (on Android the service guards against duplicates).
   - `OnboardingEntries` calls the scheduler instead of `rebuild()`.
   - Delete `IndexingTrigger.kt` and its two actuals.
5. **Re-run after skip.** In `IndexRunGuard`, a call that arrives during a pass sets a `rerunRequested` flag, and returns `Ok` as now. After the pass ends normally (not on cancellation or error), the guard runs one more pass if the flag is set, then clears it. At most one extra pass, however many calls came in.
6. **Tests:**
   - `IndexRunGuardTest`:
     - a call during a pass causes exactly one extra pass after it;
     - three calls during a pass still cause one extra pass;
     - no extra pass after a cancellation.
   - A small `FakeBackgroundSyncScheduler` in `:core:testkit` (counts `requestIndexing` calls).

**Device check (Android):**
1. Uninstall, install, go through onboarding, and grant access.
2. The "Processing your photos" notification appears once, and indexing runs once.
3. Press Home during indexing: indexing continues, because the service is still in the notification shade.
4. Kill and relaunch: one service run, which stops itself.

#### Report

Implemented by Claude.

**Commits:**
- `fb1d7e4` Fix 1.1: Replace scheduleIncrementalIndexing with requestIndexing on BackgroundSyncScheduler
- `6b0d21f` Fix 1.2: Move IndexingForegroundService to platform and add AndroidIndexScheduler
- `567b183` Fix 1.3: Add IosIndexScheduler running rebuild in the app scope
- `b26cbd9` Fix 1.4: Request indexing through the scheduler from App and onboarding
- `497bd1b` Fix 1.5: Re-run once after a pass when indexing was requested during it
- `25f9bef` Fix 1.6: Test re-run after skip and count requestIndexing in the fake scheduler

One commit per task, as asked. `fb1d7e4` and `6b0d21f` don't build on their own (callers of the old method are updated in later commits); the build passes from `b26cbd9` on.

**What changed:**
- `BackgroundSyncScheduler.requestIndexing()` replaces the empty `scheduleIncrementalIndexing`.
- Android:
  - `IndexingForegroundService` moved (`git mv`) to `platform/src/androidMain/.../platform/android/`, with its 11a behaviour unchanged, and `ic_stat_indexing.xml` moved to `platform/src/androidMain/res/drawable/`;
  - `platform/src/androidMain/AndroidManifest.xml` (new) declares the service, its `dataSync` type, and `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC`; they're removed from the app manifest;
  - `:platform` has `androidResources { enable = true }` for the icon;
  - `AndroidIndexScheduler` replaces `WorkManagerSyncScheduler`; `MainActivity.startIndexing()` calls it.
- iOS: `IosIndexScheduler(indexRepository, appScope)` launches `rebuild()` in the app `CoroutineScope` already registered in `:core:common`. It replaces `IosStubBackgroundSyncScheduler` (removed from `IosStubAdapters.ios.kt`; the vault stub in that file is untouched).
- `App.kt` (route `Photos`) and `OnboardingEntries` call `requestIndexing()`. `IndexingTrigger.kt` and its two actuals are deleted.
- `IndexRunGuard`: a call during a pass asks for a re-run and returns `Ok` at once. After a pass ends with `Ok`, one more pass runs, however many calls came in. On an error or a cancellation the request is dropped. The state is a single `AtomicInt` (`IDLE` / `RUNNING` / `RERUN_REQUESTED`) changed only by compare-and-set, so a request that arrives just as a pass ends is never lost.
- `FakeBackgroundSyncScheduler` (already in `:core:testkit`) now counts `requestIndexing` calls.

**Deviations:**
- **Calls during the extra pass.** A request that arrives while the extra pass is running queues one more pass. I read "at most one extra pass, however many calls came in" as "many calls during one pass make one extra pass" (tested). Dropping requests that arrive during the extra pass would bring back the "Retry silently skipped" bug.
- **Kotlin atomics.** `IndexRunGuard` uses `kotlin.concurrent.atomics.AtomicInt` with `@OptIn(ExperimentalAtomicApi::class)`, so there's no new dependency.
- **Koin in the service.** It now uses `KoinComponent` (koin-core), because `:platform` has no `koin-android`.
- **No new fake.** `FakeBackgroundSyncScheduler` already existed, so it was updated instead of added. Nothing uses it in tests yet; onboarding has no testable view model.

**Checks:**
- `./gradlew testAndroidHostTest`: PASS, 115 tests, 0 failures. `IndexRunGuardTest` has 10, 4 of them new:
  - a call during a pass causes exactly one extra pass;
  - three calls still cause one;
  - no extra pass after a cancellation, or after an error;
  - the guard is free again after the extra pass.
- `./gradlew :androidApp:assembleDebug`: PASS. The merged manifest has `com.o3.vanguard.visionary.platform.android.IndexingForegroundService` with `foregroundServiceType="dataSync"` and both permissions.
- iOS: `:data` compiles. `:platform` fails only with the 3 known vault-adapter errors (`IosSecurityAndVaultAdapters.kt:58`, `IosStubAdapters.ios.kt:46,58`), and none come from this step's files.

**Device check:** not run. The Pixel 2 disconnected (`adb: no devices/emulators found`) during the install. Steps 1–3 also need an uninstall, which wipes the app's data on the phone (including vault items), so the human should run them.

**Noticed but not touched:**
- On Android, `MainActivity` requests indexing on create, and `App.kt` does too once the start route is `Photos`. While the first pass runs, the service ignores the second start. If the first pass finishes before the splash ends (a small, already-indexed library), a second, quick pass runs.
- `MainActivity` still has its own media-permission request on first launch, separate from onboarding's `AppPermissionGate` (Step 8).
- The notification texts are still hardcoded (Step 10).

---

## Step 2 — Incremental indexing

**Problem:**
- Every pass calls `mediaSource.queryDelta(0L)`, a full MediaStore scan, then does a `getById` and a single-row upsert for **every** item.
- `deleteNonExistingImages(... NOT IN (:list))` binds one parameter per photo, which breaks SQLite's variable limit on big libraries (tens of thousands of photos).
- `SyncCheckpointEntity` exists but isn't used.
- "Index only while charging" does nothing.
- The splash always waits 3 s (`SPLASH_DURATION_MS`).

**Tasks:**
1. **Checkpoint.** Store the highest `DATE_MODIFIED` (or the MediaStore generation, on API 30+) seen in `sync_checkpoint`, and pass it to `queryDelta(since)`. A full scan happens only when there's no checkpoint. Check how `queryDelta` uses its argument on Android and iOS, and fix it if it ignores it.
2. **Deletions.**
   - Add `MediaSourceEngine.queryAllIds(): AppResult<Set<String>>` (ids or refs only, cheap).
   - Diff against the DB in Kotlin, and delete missing rows in chunks of ≤500 ids.
   - Remove `deleteNonExistingImages`.
3. **Batched writes.**
   - Upsert new or changed media in chunks of 200, inside a write transaction.
   - Keep favourites with one query for the chunk (`SELECT id FROM media_item WHERE isFavorite = 1 AND id IN (...)`), not `getById` per item.
4. **Charging-only.** If `indexOnlyWhileCharging` is on and the device isn't charging, `AndroidIndexScheduler.requestIndexing()` enqueues a unique WorkManager `OneTimeWorkRequest` with `setRequiresCharging(true)`, instead of starting the service. The worker calls `indexRepository.rebuild()` and uses `setForeground` with the same notification. Add the WorkManager dependency to `:platform` if it's missing.
5. **Splash.** Show the splash only until the start route is known. Keep a minimum of 600 ms, so it doesn't flash.
6. **Tests:**
   - the diff and chunking logic as a pure function (e.g. `planIndexChanges(dbIds, deviceIds, chunkSize)`): additions, deletions, chunk sizes, the empty-library case;
   - the checkpoint decision (first run is full, later runs incremental).

**Device check:**
1. Index fully, take one screenshot, and relaunch: only the new screenshot gets indexed (watch the progress count).
2. Delete a photo in the system gallery app and relaunch: it disappears from Visionary.
3. With "Index only while charging" on and the charger unplugged, relaunch: no indexing until you plug in.

➡ **Merge point:** the human merges `review-fixes` into `Develop` after Step 2 is approved.

---

## Step 3 — Delete, trash and restore

**Problem:**
- `RoomTrashRepository.moveToTrash`: if `copyToTrash` fails, it stores the **original** URI as the trash copy. The original is then deleted, so the Bin entry points at nothing.
- `restoreFromTrash`:
  - writes `origin = "GALLERY"` (not a `MediaOrigin` value) and `mime = "image/*"`;
  - uses `deletedAtMs` as the photo date;
  - points `ref` at the private trash copy;
  - removes the exclusion by the trash path instead of the original URI, so the restored photo stays excluded from indexing;
  - never puts the file back in the system gallery.
- `deleteFromTrash` leaves the file on disk, and nothing purges old items.
- `DeleteMediaUseCase` copies files to the trash before the user consents. When they decline, the rows are removed, but the copied files stay.
- `PhotoViewModel.OnDeleteSelected` ignores the delete result.
- Search delete has no in-flight guard (a double tap starts two deletes).

**Tasks:**
1. **Schema v6.** Add `originalUri`, `mime` and `takenAtMs` to `TrashItemEntity`. Write `MIGRATION_5_6` with `ALTER TABLE ... ADD COLUMN`, defaults copied from the current row (`originalUri` = `uri`). Bump the DB version to 6, commit `6.json`, and register the migration. Keep the 1–4 fallback as it is.
2. **Move to trash.**
   - If `copyToTrash` fails, don't add a trash row and don't delete that item. `DeleteMediaUseCase` reports it as `NotPossible` when nothing could be trashed, and skips only the failed items otherwise.
   - Store the original URI, mime and taken date.
3. **Rollback** (the user declined): delete the copied trash files too. Add `TrashRepository.discard(id)`, which deletes the row **and** the file.
4. **Restore to gallery.**
   - New port `MediaRestoreGateway.restoreToGallery(sourcePath, displayName, mime, takenAtMs): AppResult<String>`, returning the new content URI.
   - Android: a MediaStore insert with `IS_PENDING`. Copy the pattern, but **not the code location**, from the vault's `restoreToStorage`; don't edit vault code. Android ≤ 9 needs `WRITE_EXTERNAL_STORAGE` with `maxSdkVersion="28"`.
   - iOS: `PHPhotoLibrary` `creationRequestForAssetFromImage(atFileURL:)`, or a stub returning `Unsupported` if that's too much for this step (say so in the report).
   - `restoreFromTrash`: restore the file → on success, delete the trash file and row, and remove the exclusion by **`originalUri`**. Don't insert a `media_item` row by hand; the next index pass picks up the restored file. Then call `scheduler.requestIndexing()`.
5. **Purge.**
   - `deleteFromTrash` deletes the file too.
   - Add `TrashRepository.purgeOlderThan(ms)`. Call it at the start of every index pass with 30 days.
6. **UI results.**
   - `PhotoViewModel` and the trash screen show a message when a delete or restore fails.
   - `SearchViewModel` ignores `OnDeleteImage` while a delete is in flight.
7. **Tests** (with fakes):
   - `DeleteMediaUseCase`: a failed copy means the item isn't deleted; on decline, `discard` is called for each copied item.
   - `TrashViewModel`: a restore error shows a message.
   - `SearchViewModel`: a double delete makes one gateway request.

**Device check:**
1. Delete a photo → it's in the Bin → restore it → it's back in the **system gallery app** with its original date, and searchable after the next index.
2. Delete, then cancel the system dialog → the Bin stays empty.
3. Upgrade check: install the build from before this step, delete a photo to the Bin, install this build → the app opens and the Bin item is still there (Migration 5→6).

---

## Step 4 — Classification, entities, OCR boxes

**Problem:**
- `SmartMediaClassifier` and `EntityExtractor` (`domain/.../usecase/UseCases.kt`) are never called while indexing, so nothing becomes `RECEIPT` or `DOCUMENT`, and `entityFlags` is always 0. The Receipts and Documents filters, voice search hints and `HAS_PHONE_OR_URL` all find nothing.
- `EntityExtractor`:
  - stores the **whole** OCR text as `rawValue`;
  - handles only phone and email;
  - its phone regex is US-only, so `0300-1234567`, `03001234567` and `+92 300 1234567` don't match.
- The classifier uses `contains`, so "totally" counts as a receipt.
- Screenshot detection uses the file name only.
- `MlKitOcrEngine` returns `lines = emptyList()`, `blocksJson` is always `"[]"`, and `observeOcrLines` returns an empty list, so the viewer can't highlight matches.
- Already-indexed items are never re-processed, so these fixes would never reach existing photos.

**Tasks:**
1. **Extractor:**
   - return the matched substring as `rawValue`;
   - phone: international plus Pakistani mobile and landline formats;
   - add `URL` (`https?://…` and `www.…`) and `AMOUNT` (`Rs`, `PKR`, `Rs.` followed by a number, with commas).
2. **Classifier:** match whole words (`\b…\b`). Add "bill", "paid", "subtotal", "tax" and "invoice no" to receipts. Keep `VIDEO` and `SCREENSHOT` as base categories; only `PHOTO` or `SCREENSHOT` can become `RECEIPT` or `DOCUMENT`.
3. **Indexing:** in `RoomIndexRepository`, after OCR:
   - category = `classifier.classify(text, isScreenshot)`;
   - `entityFlags` = OR of the found types' `flagBit`;
   - update `media_item` in the same transaction as `media_text`.
   - Inject both classes through Koin (`singleOf`).
4. **Screenshots (Android):** also treat items whose `RELATIVE_PATH` contains `Screenshots` (API 29+) as screenshots.
5. **OCR lines:**
   - `MlKitOcrEngine` maps `Text.TextBlock.lines` to `OcrTextLine`, with boxes normalised by image width and height.
   - Store them in `blocksJson` with kotlinx.serialization.
   - `observeOcrLines(id)` reads and decodes them.
   - iOS (`IosOcrEngine`, Vision): return lines too, if it's simple; otherwise note it.
6. **Re-index:** add `indexVersion INTEGER NOT NULL DEFAULT 0` to `media_text` (schema 7, with `MIGRATION_6_7`). A constant `CURRENT_INDEX_VERSION = 1`; rows below it are processed again.
7. **Tests:**
   - `EntityExtractorTest`:
     - PK mobile, landline and `+92` numbers, plus a US number;
     - a URL and an email;
     - `rawValue` is just the match;
     - "totally fine" isn't a receipt.
   - `SmartMediaClassifierTest`: the new words, and word boundaries.
   - The re-index decision as a pure function.

**Device check:**
1. Take a screenshot of a bill with a phone number → after indexing it appears under the Receipts filter, and under "has phone or URL".
2. Search its shop name → open it → the matched word is highlighted.
3. Upgrade from the Step 3 build: no crash (Migration 6→7), and old photos get re-processed.

---

## Step 5 — Search on FTS5

**Problem:**
- `RoomSearchRepository.search` loads **every** row (`getAll()`), then one `getText` per row, scores with edit distance in Kotlin, and runs on the caller's thread. Voice search calls it up to 3 times per question.
- The FTS5 table `media_fts` and its triggers exist but are never queried.
- They're created only in `FtsCallback.onCreate`, so a destructive reset or a migration can leave them missing.
- `VectorSearchEngine` is a fake "embedding" (word hash → `sin`), but it's 40% of the fuzzy score and is computed for every image during indexing.
- `MediaItem.embedding` travels into UI state.
- `SearchViewModel`:
  - loads the whole library into `allImages`;
  - starts a new search per keystroke without cancelling the old one;
  - mode and filter changes search twice.
- The Photos screen's inline search filters every item in composition, on every recomposition.

**Tasks:**
1. **FTS setup:** move the `CREATE VIRTUAL TABLE` and `CREATE TRIGGER ... IF NOT EXISTS` statements to `onOpen`, so they're idempotent. If `media_fts` is empty while `media_text` isn't, run `INSERT INTO media_fts(media_fts) VALUES('rebuild')`.
2. **Query builder** (pure Kotlin, `:data`): `buildFtsQuery(input): String?`.
   - Split into words and drop FTS syntax characters (`" * ( ) : ^ -` and the keywords `AND`, `OR`, `NOT`, `NEAR`).
   - Quote each term and add `*` for a prefix match.
   - Join with spaces (AND).
   - Return `null` when nothing is left.
3. **DAO:**
   ```sql
   SELECT m.*, t.fullText, t.labelsText FROM media_fts f
     JOIN media_item m ON m.id = f.rowid
     JOIN media_text t ON t.mediaId = m.id
   WHERE media_fts MATCH :q AND (:categoriesEmpty OR m.category IN (:categories))
   ORDER BY bm25(media_fts) LIMIT :limit
   ```
   Map the result in one pass, with no per-row `getText`. Apply the entity-flag filter in SQL as well.
4. **Fallback:** only when FTS returns nothing, run the existing edit-distance scoring, limited to the 2,000 newest rows. It catches names voice search mishears.
5. **Remove the fake embeddings:**
   - stop generating them in indexing;
   - remove `VectorSearchEngine` usage and `MediaItem.embedding`;
   - leave the DB column, since dropping it needs a migration.
6. **`SearchViewModel`:** one `query + filter + mode` flow with `debounce(250)` + `flatMapLatest`. Drop `allImages` if it's only used for the old search.
7. **Photos inline search:** use the repository (debounced), or at least `remember(state.items, query)` the filtered list.
8. **Tests:**
   - `buildFtsQuery`: plain words; quotes and `*` in the input; keywords; empty and whitespace; Unicode.
   - `SearchViewModel`: fast typing makes one search with the last query; a filter change makes one search.

**Device check:**
1. On a library of 1,000+ photos, search a word from a receipt → results appear almost instantly.
2. Voice search "find the receipt for <shop>" still finds it.
3. Search a misspelled shop name → the fallback still finds it.

➡ **Merge point:** the human merges into `Develop` after Step 5 is approved.

---

## Step 6 — Voice search follow-ups

**Tasks:**
1. **Dependency rule:** `:features:aisearch` depends on `:platform` only for `rememberMicrophonePermission`, against the `AGENTS.md` rule. Move the microphone permission behind a port:
   - `domain`: `MicrophonePermissionPort { fun isGranted(): Boolean; suspend fun request(): Boolean }`;
   - Android: an implementation with an activity-scoped launcher, like the delete launcher in `MainActivity`;
   - iOS: a stub.
   - Remove `project(":platform")` from `features/aisearch/build.gradle.kts`.
2. **Missing language model:** when on-device recognition fails with `NOT_AVAILABLE_OFFLINE` and the system recognizer is available, retry once with the system recognizer (`SYSTEM_SERVICE`), and show the existing notice. On API 33+, first try `checkRecognitionSupport`, then `triggerModelDownload` for the language.
3. **Split `VoiceSearchScreen.kt`** (624 lines) into the screen plus components (mic button, chips row, results grid, state messages). No behaviour change.
4. **Tests:** VM test, where the first failure is `NOT_AVAILABLE_OFFLINE` and the retry succeeds → `Results`.

**Device check:** voice search works on the phone, and in airplane mode it either works or shows the offline message.

---

## Step 7 — Privacy and hygiene

**Tasks:**
1. Remove the media URI logs: `AndroidPlatformAdapters.android.kt:276` (`"ref=$s"`) and `:280` (`"resolved=$resolved"`).
2. Replace the hardcoded dispatchers with injected `AppDispatchers`:
   - `AndroidPlatformAdapters.android.kt:345, 364`; leave `:616`, because it's vault code;
   - `IosMediaDeleteAndShareGateways.kt:65`
   - `AndroidThumbnailMaker.kt:17`
   - `MediaStoreReceivedPhotoStore.kt:19`
   - `NearbyTeleportTransport.kt:22, 67`
3. Replace `catch (_: Throwable)` with `catch (e: Exception)`, and rethrow `CancellationException` where the code is in a coroutine:
   - `AndroidPlatformAdapters.android.kt:164, 248, 458, 471`
   - `IosMediaSourceEngine.kt:37, 134`
   - `IosVisionAiEngines.kt:74, 107`
   - `MediaStoreReceivedPhotoStore.kt:54`
   - `AlbumGalleryViewModel.kt:20`

   Leave `IosSecurityAndVaultAdapters.kt` (vault).
4. `AGENTS.md`, under Security, add: "No code may log or send media URIs, file paths, OCR text or voice transcripts. Any feature that needs network removes `tools:node="remove"` from `INTERNET` in the manifest and explains why in its PR."
5. Ask for `POST_NOTIFICATIONS` on Android 13+ once, the first time indexing starts, so the indexing notification is visible. If it's denied, carry on.

---

## Step 8 — Architecture leftovers

**Tasks:**
1. `:androidApp` depends only on `:composeApp` (and Koin Android). Expose what `MainActivity` needs through `composeApp`, or move it there.
2. `:features:onboarding` stops importing `AppPermissionGate` from `:platform`. Use a port, as in Step 6, then remove the known exception from `AGENTS.md`.
3. Remove the Koin module from `:core:common` (`AppContracts.kt`), and register `AppDispatchers` and the app scope in `composeApp`.
4. Replace the static `AndroidMediaDeleteGateway.currentLauncher` bridge with a lifecycle-aware, activity-scoped holder that survives rotation and doesn't leak the Activity. Leave `AndroidBiometricAuthenticator.currentActivityProvider` (vault); note it for the teammate.
5. `VisionaryApp` reads `isDebug` and the version from `BuildConfig`.
6. `RoomSettingsRepository.update()` runs its read-transform-write inside one write transaction.
7. Delete dead code:
   - `IndexJobDao` (if unused);
   - `searchByLabelsExact`, `getImagesWithEmbeddings`, `getAllTextEntities` (if unused after Step 5);
   - the `SyncCheckpointEntity` leftovers, if Step 2 didn't use them.

---

## Step 9 — Teleport and permissions

**Tasks:**
1. **Connection check:** `NearbyTeleportTransport` calls `acceptConnection` straight away. Show the `authenticationDigits` from `ConnectionInfo` on both phones, and accept only after the user confirms they match. Rejecting calls `rejectConnection`.
2. **Size limit:** the receiver rejects payloads larger than 100 MB (a constant), and shows a message.
3. **Location permission:** `ACCESS_FINE_LOCATION` is requested on every API level. Check the current Nearby Connections docs: if Android 12+ (with `BLUETOOTH_*` and `NEARBY_WIFI_DEVICES`) doesn't need it, add `android:maxSdkVersion="30"` (or what the docs say). Put the doc link in the report.
4. **Tests:** session tests for "digits not confirmed" and "payload too large" (with the existing teleport fakes).

**Device check:** two phones; the digits match and the transfer works; Cancel on one phone stops it.

---

## Step 10 — Tooling and code health

**Tasks:**
1. ktlint and detekt through the convention plugins, with a baseline file for existing issues, so only new code must be clean.
2. A module-graph check: a Gradle task that fails if a `features/*` module depends on `:data`, `:core:database` or `:platform`.
3. GitHub Actions: on every PR to `Develop` or `review-fixes`, run `testAndroidHostTest`, `:androidApp:assembleDebug`, ktlint, detekt and the module check.
4. Move strings to resources: the 77 literal `Text("…")` calls, plus user-facing strings in view models (as `StringResource` keys or typed events).
5. Split big files, with no behaviour change:
   - `AndroidPlatformAdapters.android.kt`: one file per adapter;
   - `RoomRepositories.kt`: one file per repository;
   - `SearchScreen.kt`: screen plus components.
6. Replace `material-icons-extended` with the icons actually used (copied as vector resources, or the core set), if it reduces app size. Report before and after APK sizes.
7. **Human:** make `Develop` the default branch on GitHub.
