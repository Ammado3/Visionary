# Fix: the pass counter is reset during every pass

Branch: `review-fixes`. Read `AGENTS.md` first. One small fix, one commit.

## Why

Fix 1c.1 added `completedPasses` and `lastPassFailed` to `IndexProgress`. `IndexRunGuard` raises `completedPasses` by 1 when a pass ends, and `SearchViewModel` turns the indexing bar off when `completedPasses` goes above the value it saved when "Rebuild index" was tapped.

But `RoomIndexRepository` (`data/.../RoomRepositories.kt`) writes progress by **building a new object**, e.g.:

```kotlin
progress.value = IndexProgress(media.size, indexed, media.size - indexed - failed, failed, IndexState.RUNNING)
```

The new object gets the default `completedPasses = 0` and `lastPassFailed = false`. That happens at these lines:
- **296:** the initial counts in `observeProgress()`
- **370:** the start of a pass
- **377:** paused mid-pass
- **384 and 390:** per-item updates
- **435:** the per-item update in the main loop
- **437:** the end of a pass

So the counter goes back to 0 during every pass, and the guard sets it to 1 at the end. It's always 1 after a pass.

Result in Search:
- **First "Rebuild index":** the baseline is 0, the pass ends at 1, so the bar turns off. Works.
- **Any later tap:** the baseline is 1, the pass ends at 1, and `1 > 1` is false, so **the bar never turns off.**

The unit tests didn't catch this because they use `FakeIndexRepository` and set the progress by hand.

## Change

1. **Helper.** In `data/.../IndexRunGuard.kt` (or a small new file in the same package), add:
   ```kotlin
   /** Updates the counts and state, keeping the pass history (completedPasses, lastPassFailed). */
   internal fun IndexProgress.withCounts(
       total: Int,
       indexed: Int,
       pending: Int,
       failed: Int,
       state: IndexState,
   ): IndexProgress = copy(total = total, indexed = indexed, pending = pending, failed = failed, state = state)
   ```
2. **Use it everywhere.** In `RoomIndexRepository`, replace **every** `progress.value = IndexProgress(…)` with `progress.value = progress.value.withCounts(…)`, using the same arguments. Afterwards:
   ```bash
   grep -n "progress.value = IndexProgress(" data/src/commonMain/kotlin/com/o3/vanguard/visionary/data/RoomRepositories.kt
   ```
   must print nothing. The only `IndexProgress(…)` constructor left in that class is the initial `MutableStateFlow(IndexProgress(0, 0, 0, 0, IndexState.IDLE))`.
3. Don't change `IndexRunGuard`'s logic, `SearchViewModel`, or `IndexProgress` itself.

## Tests

Add to `data/.../IndexRunGuardTest.kt`, or a new `IndexProgressTest.kt` in the same folder:
- `withCounts` keeps `completedPasses` and `lastPassFailed`, and changes the five count and state fields.
- **The real scenario, with the guard:** run **three** passes in a row through `IndexRunGuard`. Each pass, like the real one, writes progress with `progress.value = progress.value.withCounts(…, IndexState.RUNNING)` and then `withCounts(…, IndexState.IDLE)` before returning `Ok`. After each pass, `completedPasses` is 1, then 2, then 3.

Commit: `Fix 1c.3: Keep the pass counter when updating index progress`

## Verify

```bash
./gradlew testAndroidHostTest
./gradlew :androidApp:assembleDebug
```
Report the real test count.

Device: open Search and tap "Rebuild index" **three times**, waiting for each pass to finish. Each time the bar appears and then disappears.

Push to `review-fixes`.
