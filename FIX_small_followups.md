# Fix: indexing status, splash time, stale build folder

Branch: `review-fixes`. Read `AGENTS.md` first. Three small, independent fixes, one commit each.

---

## 1. The "indexing" bar in Search can get stuck

### Why

- `SearchViewModel` sets `isIndexing = true` when "Rebuild index" is tapped, then waits for `observeIndexProgressUseCase()` to set it back (`isIndexing = progress.state == RUNNING`).
- If a pass ends **without changing the progress value**, the `StateFlow` doesn't emit, and the bar stays on until some later progress change. For example, `mediaSource.queryDelta()` can fail before the state becomes `RUNNING`.
- The earlier fix also dropped the "Indexing failed" message, because the result of a pass is no longer awaited.

### Change

**a) Publish the end of every pass.**
- File: `domain/.../model/DomainModels.kt`. Add two fields with defaults at the end of `IndexProgress`, so existing constructor calls keep compiling:
  ```kotlin
  data class IndexProgress(
      val total: Int,
      val indexed: Int,
      val pending: Int,
      val failed: Int,
      val state: IndexState,
      /** Increases by 1 every time a pass ends (success or error, not cancellation). */
      val completedPasses: Int = 0,
      /** Whether the most recent finished pass returned an error. */
      val lastPassFailed: Boolean = false,
  )
  ```
- File: `data/.../IndexRunGuard.kt`. In `runOnce`, after `pass()` returns or throws a non-cancellation exception, update `progress` in one assignment:
  - `completedPasses + 1`;
  - `lastPassFailed = result is AppResult.Err`;
  - the existing `RUNNING` → `IDLE` reset.

  On `CancellationException`, change nothing except the existing `RUNNING` → `IDLE` reset. Because `completedPasses` always changes, the flow always emits when a pass ends.

**b) Search tracks its own request.**
- File: `features/aisearch/.../SearchViewModel.kt`.
- Add a private `var reindexBaseline: Int? = null`.
- `OnReindexClicked`:
  - set `reindexBaseline = state.value.indexProgress?.completedPasses ?: 0`;
  - `updateState { copy(isIndexing = true, errorMessage = null) }`;
  - `indexScheduler.requestIndexing()`.
- In the progress collector:
  ```kotlin
  val baseline = reindexBaseline
  val finished = baseline != null && progress.completedPasses > baseline
  if (finished) reindexBaseline = null
  updateState {
      copy(
          indexProgress = progress,
          isIndexing = progress.state == IndexState.RUNNING || reindexBaseline != null,
          errorMessage = if (finished && progress.lastPassFailed) "Indexing failed" else errorMessage,
      )
  }
  ```
  Compare with `IndexState.RUNNING`, not `state.name == "RUNNING"`.
- If `isIndexing` is still `true` 2 minutes after the request with no progress emission at all (for example, the app was in the background and the start was deferred), leave it. The next foreground start runs the pass and ends it.

### Tests

- `data/.../IndexRunGuardTest.kt`, 3 new tests:
  - a successful pass raises `completedPasses` by 1, with `lastPassFailed = false`;
  - a pass that returns `Err` (or throws) raises it by 1, with `lastPassFailed = true`;
  - a cancelled pass doesn't change `completedPasses`.
- `features/aisearch/.../SearchViewModelTest.kt`, 3 new tests with `FakeIndexRepository` (set `indexRepository.progress.value` to simulate the end of a pass):
  - reindex → a pass ends **without** ever being `RUNNING` (only `completedPasses` changes) → `isIndexing == false`;
  - reindex → a pass ends with `lastPassFailed = true` → `errorMessage == "Indexing failed"`, `isIndexing == false`;
  - reindex → the state goes `RUNNING` then `IDLE` with `completedPasses + 1` → `isIndexing` is `true` while running and `false` after, with no error.

Commit: `Fix 1c.1: Publish the end of every index pass and clear Search's indexing bar`

---

## 2. The splash always waits 3 seconds

### Why

`App.kt` waits `delay(SPLASH_DURATION_MS)` (3,000 ms, in `screens/SplashScreen.kt`) **before** it even checks access and resolves the start route. Every launch shows 3 s of splash, and since Step 1b the first indexing request waits for it too. The splash animation loops, so it doesn't need a fixed length.

### Change

File: `composeApp/src/commonMain/.../App.kt`. In the `LaunchedEffect`:
1. Record the start time with `TimeSource.Monotonic.markNow()`.
2. Check access, resolve the route, and call `indexScheduler.requestIndexing()` when the route is `Photos`, **straight away**, with no delay before.
3. Then wait only for what's left of a minimum display time, so the splash doesn't flash:
   ```kotlin
   val remaining = MIN_SPLASH_MS.milliseconds - started.elapsedNow()
   if (remaining.isPositive()) delay(remaining)
   startRoute = route
   ```
4. Replace `SPLASH_DURATION_MS = 3_000L` with `MIN_SPLASH_MS = 600L` in `SplashScreen.kt`, and update the import in `App.kt`.

No other splash changes (design, animation).

### Tests

None needed (UI timing). Device check below.

Commit: `Fix 1c.2: Show the splash only until the start route is known`

---

## 3. Stale test reports from the old `features/search` folder

### Why

The search module moved from `features/search` to `features/aisearch`, but an old `features/search/build/` folder is still on the development machine. Its old test reports were being counted in the test total. The folder isn't in git (`build/` is ignored), so this is a local clean-up only.

### Change

```bash
rm -rf features/search
```
Then check that `features/search` doesn't exist and that `git status` shows no change (nothing is committed for this one). Re-run the tests and report the new total.

---

## Verify

```bash
./gradlew testAndroidHostTest
./gradlew :androidApp:assembleDebug
```
Report the real test count, after removing the stale folder.

Device:
1. **Splash:** cold start (force-stop, then open) → the splash shows briefly (well under 3 s) and the app opens on Photos. On first install, onboarding still appears.
2. **Search, rebuild index:** tap it → the progress bar shows while indexing runs and disappears when it ends.
3. **Search, failure:** revoke the photo permission in system settings, return to Search, and tap "Rebuild index" → the bar disappears instead of staying on. (If the app shows the permission screen first, that's fine too.)

Push to `review-fixes`.

## Don't touch

Vault code, `IndexingForegroundService`, `AndroidIndexScheduler`, onboarding, and the splash design.
