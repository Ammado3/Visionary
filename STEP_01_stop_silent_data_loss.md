# Step 01 — Stop silent data loss

## Context

Visionary is a Kotlin Multiplatform app (Android + iOS). Read `AGENTS.md` in the repo root first; it lists the modules, the commands that work, and the testing rules (handwritten fakes in `:core:testkit`, `runViewModelTest`, `collectEvents`, no mocking libraries).

This step fixes places where the app either **loses user data silently** or **tells the user something happened when it didn't**. It is deliberately small. Do not fix anything outside the scope below, even if you notice it; later steps cover it.

- Branch: `review-fixes`. All steps of this fix plan go on this one branch.
  - If it doesn't exist yet, create it from the latest `Develop`: `git checkout Develop && git pull && git checkout -b review-fixes`.
  - If it already exists, check it out and pull before starting.
- One commit per task below (4 commits), so each can be reviewed separately.
- Start every commit message with the step and task number, e.g. `Step 01.2: Make delete from Search actually delete`.
- Don't merge `review-fixes` into `Develop`; that happens after review.

---

## Task 1 — Stop destructive database migrations

**Problem.** `core/database/src/commonMain/kotlin/com/o3/vanguard/visionary/core/database/DatabaseBuilder.kt` calls `.fallbackToDestructiveMigration()`. On any future schema version bump, Room drops every table, including `vault_item` and `trash_item`. Vault items have already been deleted from the system gallery when they were moved into the vault, so wiping `vault_item` makes those photos unreachable from the app. That is permanent data loss.

**Change.**
1. Remove `.fallbackToDestructiveMigration()`.
2. Allow destructive fallback **only** from the old pre-release versions 1 and 2, so existing dev installs don't crash:
   ```kotlin
   .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2)
   ```
   Check the exact signature in the Room 3 version in `gradle/libs.versions.toml` (`room3 = "3.0.3"`). If the signature differs, use the equivalent Room 3 API that limits destructive fallback to start versions 1 and 2. Do **not** use `fallbackToDestructiveMigrationOnDowngrade` either.
3. Keep `version = 3` and `exportSchema = true` in `AppDatabase.kt`. Do not change any entity in this step.
4. Add a short comment above the builder call:
   ```kotlin
   // Never add a blanket destructive fallback. Every version bump after 3 needs an explicit
   // Migration and a committed schema JSON in core/database/schemas/.
   ```
5. Add the same rule to `AGENTS.md` under a new heading `## Database rules` (2–3 bullet points: no destructive fallback, every bump needs a `Migration`, commit the exported schema JSON).

**Acceptance.** `grep -rn "fallbackToDestructiveMigration()" .` returns nothing. The app still launches on a fresh install.

---

## Task 2 — Make "delete" from Search actually delete

**Problem.** In `features/search/.../SearchViewModel.kt`, `SearchIntent.OnDeleteImage` removes the item from `results` and `allImages` and sends `ShowToast("Image moved to Bin")`, but never deletes anything. The image comes back on the next search, and the toast is false.

**Change.**
1. Inject `DeleteMediaUseCase` into `SearchViewModel` (constructor parameter, last position). Update the Koin definition in `features/search/.../SearchModule.kt` to pass one more `get()`.
2. Rewrite the `OnDeleteImage` branch so it:
   - launches in `viewModelScope`,
   - calls `deleteMediaUseCase(intent.item)`,
   - then branches on the `DeleteConsentResult`:
     - `Deleted` → clear `selectedImage`, remove the item from `results` and `allImages`, send `ShowToast("Image moved to Bin")`.
     - `DeclinedByUser` → keep the item in the lists, clear `selectedImage`, send no toast.
     - `NotPossible` → keep the item in the lists, send `ShowToast("Couldn't delete image")`.
3. Do not change the existing toast text pattern or move strings to resources in this step (a later step handles strings).

**Tests.** Create `features/search/src/commonTest/kotlin/com/o3/vanguard/visionary/features/search/SearchViewModelTest.kt`. Build the view model with real use cases over existing testkit fakes: `FakeSearchRepository`, `FakeMediaRepository`, `FakeIndexRepository`, `FakeTrashRepository`, `FakeMediaDeleteGateway`. Use `runViewModelTest` and `collectEvents`. Cover:
- delete with `FakeMediaDeleteGateway.nextResult = Deleted` → item removed from `results`, toast event emitted, `FakeMediaDeleteGateway.deleteRequests` contains the item's URI.
- delete with `DeclinedByUser` → item still in `results`, no toast.
- delete with `NotPossible("x")` → item still in `results`, "Couldn't delete image" toast.

If the view model's `init` blocks make the test awkward (it collects several flows), seed the fakes before constructing the view model rather than changing production code to suit the test.

---

## Task 3 — Make `DeleteMediaUseCase` cancellation-safe

**Problem.** `domain/.../usecase/UseCases.kt` → `DeleteMediaUseCase` wraps each repository call in `try { … } catch (e: Exception) { // ignore }`. This also swallows `CancellationException`, which breaks structured concurrency (a cancelled screen keeps deleting).

**Change.**
1. In every `catch (e: Exception)` block in `DeleteMediaUseCase`, rethrow `CancellationException` first:
   ```kotlin
   } catch (e: CancellationException) {
       throw e
   } catch (e: Exception) {
       // best-effort: continue with the remaining items
   }
   ```
   Import `kotlinx.coroutines.CancellationException`. Keep the existing best-effort behaviour for other exceptions; changing the overall flow (trash-before-consent) is a later step.
2. Do not change the public signature of the use case.

**Tests.** Create `domain/src/commonTest/kotlin/com/o3/vanguard/visionary/domain/DeleteMediaUseCaseTest.kt`, using `FakeMediaDeleteGateway`, `FakeMediaRepository`, `FakeTrashRepository`. Cover:
- `Deleted` → `FakeMediaRepository` received `deleteMedia` for each item, and the trash fake contains the items.
- `DeclinedByUser` → the trash fake no longer contains the items (they are rolled back), and `deleteMedia` was not called.
- Empty list → returns `Deleted` and makes no gateway call.
- Cancellation: make a small local test fake (inside the test file) whose `moveToTrash` throws `CancellationException`, and assert the use case call throws `CancellationException` instead of returning.

If `FakeMediaRepository` or `FakeTrashRepository` don't record calls in a way you can assert, add a recording list to the fake in `:core:testkit` (e.g. `val deletedIds = mutableListOf<MediaId>()`). That is the only allowed change to `:core:testkit`.

---

## Task 4 — Vault delete and restore must not lie

**Problem.** In `data/.../RoomRepositories.kt` → `RoomVaultRepository`:
- `delete(itemId)` removes the database row but leaves the file in `vault_blobs`, so the photo stays on disk forever.
- `restore(itemId)` returns `AppResult.Ok(Unit)` and does nothing.

**Change.**
1. `delete(itemId)`:
   - First call `vaultCryptoEngine.deleteVaultBlobs(itemId)`.
   - If it returns `AppResult.Err`, return that error and **keep** the database row (so the user can retry).
   - Only on success, call `db.vaultDao().deleteVaultItem(itemId)` and return `AppResult.Ok(Unit)`.
2. `restore(itemId)`: return
   ```kotlin
   AppResult.Err(AppError.Unsupported("VAULT_RESTORE_NOT_IMPLEMENTED"))
   ```
   Real restore needs a new platform port to write back to the gallery; that comes in a later step. Returning an error is better than a fake success.
3. Search the features for any caller of `VaultRepository.restore` or `VaultRepository.delete`. If a screen calls them and ignores the result, handle the `Err` by showing the existing error/toast mechanism of that screen. If no feature calls them, change nothing in the UI.
4. Update `FakeVaultRepository` in `:core:testkit` only if it no longer matches the interface (it should, since the interface doesn't change).

**Tests.** `RoomVaultRepository` needs a real Room database, which host tests can't load (see `AGENTS.md`). No unit test is required for this task; describe the manual check you did in your report.

---

## Out of scope (do not touch)

- Vault encryption, PIN, biometric, route guards (Steps 04–07).
- Settings persistence (Step 02).
- Category name mismatch in collections/gallery/search (Step 03).
- Trash restore bugs, indexing, search performance, FTS.
- Moving strings into resources, ktlint/detekt, splitting large files.

## Verify before reporting

```bash
./gradlew testAndroidHostTest
./gradlew assembleDebug
```

Both must pass. Then manually on a device or emulator:
1. Search for an image → open it → delete → accept the system dialog → it disappears and doesn't come back on a new search.
2. Repeat but cancel the system dialog → image stays, no "moved to Bin" toast.
3. Fresh install still launches (migration change).

## Report back

When done, push the branch and reply with:
1. Confirm you pushed `review-fixes`, and list the commit hashes (one per task).
2. Anything in this spec you could not do exactly as written, and why.
3. Test results (number of tests run/passed) and the manual checks you did.
4. Any problem you noticed but left alone because it was out of scope.
