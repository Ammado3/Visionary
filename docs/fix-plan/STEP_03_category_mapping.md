# Step 03 — Category mapping (collections, gallery, search filters)

## Context

Read `AGENTS.md` first.

**Problem.** Stored `media_item.category` values are `MediaCategory` names, which are **singular**: `PHOTO`, `VIDEO`, `SCREENSHOT`, `DOCUMENT`, `RECEIPT`, `SCAN` (written by `MediaItem.toEntity()` in `data/.../RoomRepositories.kt`, `category = category.name`). The code that reads them uses **plural** `CollectionKind` / `SearchFilter` names, so nothing ever matches:

- `RoomMediaRepository.observeCollections()` (`RoomRepositories.kt` ~line 30) looks up `countMap[kind.name]`, e.g. `"SCREENSHOTS"`. Every count is 0, including `ALL` (there is no `"ALL"` category row). `coverUri` is always `null`.
- `RoomMediaRepository.observeMedia(kind)` (~line 38) calls `observeByCategory(kind.name)`, so every category gallery except `ALL` is empty.
- `RoomSearchRepository.search` (~line 85) maps filters to `"SCREENSHOTS"`, `"RECEIPTS"`, `"DOCUMENTS"`, `"SCANS"` and compares them with `entity.category`, so every filtered search returns nothing.
- `SearchFilter.HAS_PHONE_OR_URL` is mapped to `"ALL"`, so it filters nothing.

**Goal.** One explicit, tested mapping in `:domain`, used by the data layer.

Note: Search itself is rewritten on FTS5 in Step 10. In this step, **only fix the filtering** in the existing Kotlin search code. Don't restructure the scoring.

### Branch and commits

- `git checkout review-fixes && git pull`.
- One commit per task, prefix `Step 03.N: …`. The report and status hand-off is its own task commit (`Step 03.5: Add report`), not `Step 03 fix:`.
- Push when done. Don't merge into `Develop`.

---

## Task 1 — Domain mapping

In `domain/src/commonMain/kotlin/com/o3/vanguard/visionary/domain/model/`, add a new file `CategoryMapping.kt`:

```kotlin
package com.o3.vanguard.visionary.domain.model

/** Categories shown in this collection. `ALL` returns every category. */
fun CollectionKind.categories(): Set<MediaCategory> = when (this) {
    CollectionKind.ALL -> MediaCategory.entries.toSet()
    CollectionKind.SCREENSHOTS -> setOf(MediaCategory.SCREENSHOT)
    CollectionKind.DOCUMENTS -> setOf(MediaCategory.DOCUMENT)
    CollectionKind.RECEIPTS -> setOf(MediaCategory.RECEIPT)
    CollectionKind.SCANS -> setOf(MediaCategory.SCAN)
}

/** Categories this filter allows. `null` means any category. */
fun SearchFilter.categories(): Set<MediaCategory>? = when (this) {
    SearchFilter.ALL, SearchFilter.HAS_PHONE_OR_URL -> null
    SearchFilter.SCREENSHOTS -> setOf(MediaCategory.SCREENSHOT)
    SearchFilter.RECEIPTS -> setOf(MediaCategory.RECEIPT)
    SearchFilter.DOCUMENTS -> setOf(MediaCategory.DOCUMENT)
    SearchFilter.SCANS -> setOf(MediaCategory.SCAN)
}

/** Entity bits an item must have at least one of. `0` means no entity requirement. */
fun SearchFilter.requiredEntityFlags(): Int = when (this) {
    SearchFilter.HAS_PHONE_OR_URL -> SmartEntityType.PHONE.flagBit or SmartEntityType.URL.flagBit
    else -> 0
}
```

Use exhaustive `when` (no `else`) for `categories()` so that adding a new enum value fails to compile. `requiredEntityFlags()` may use `else`.

**Acceptance.** No other code changes in this task.

## Task 2 — DAO: query by a set of categories, with a cheap cover

In `core/database/src/commonMain/.../dao/Daos.kt`:

1. Add:
   ```kotlin
   @Query("SELECT * FROM media_item WHERE category IN (:categories) ORDER BY sortKey DESC")
   fun observeByCategories(categories: List<String>): Flow<List<MediaEntity>>
   ```
2. Replace `observeByCategory(category: String)` with it. Delete the old method once nothing uses it.
3. Change the counts query to also return the newest item's `ref` per category:
   ```kotlin
   data class CategoryCount(
       val category: String,
       val count: Int,
       val coverRef: String?,
       val newestSortKey: Long,
   )

   @Query("SELECT category AS category, COUNT(*) AS count, ref AS coverRef, MAX(sortKey) AS newestSortKey FROM media_item GROUP BY category")
   fun observeCategoryCounts(): Flow<List<CategoryCount>>
   ```
   SQLite guarantees that, with a single `MAX()` aggregate, bare columns (`ref`) come from the row that has the max. Keep the query exactly in that form: one `MAX()`, no `MIN()`.

**Acceptance.** No schema change: this is a query-only change, so there's no DB version bump and no new schema JSON. Confirm KSP still generates the DAO (`assembleDebug`). If Room rejects the bare column, fall back to `coverRef = null` (drop the column from the query and the data class) and say so in the report.

## Task 3 — Use the mapping in `RoomMediaRepository`

In `data/.../RoomRepositories.kt`:

1. `observeCollections()`:
   - `ALL`: count = sum of all rows' `count`; `coverUri` = `coverRef` of the row with the largest `newestSortKey` (null if there are no rows).
   - Every other kind: take the rows whose `category` is in `kind.categories().map { it.name }`; count = sum of their counts; `coverUri` = `coverRef` of the matching row with the largest `newestSortKey`.
   - To keep this testable, put the aggregation in a pure `internal` function in `:data`, e.g.
     ```kotlin
     internal fun summarizeCollections(counts: List<CategoryCount>): List<CollectionSummary>
     ```
     and have `observeCollections()` call it inside `.map { }`. Return one summary per `CollectionKind.entries`, in enum order, so Home's layout doesn't change.
2. `observeMedia(kind)`: `ALL` keeps `observeAll()`. Otherwise call `observeByCategories(kind.categories().map { it.name })`. Leave the per-row `getText` as is (Step 10 fixes N+1).

## Task 4 — Use the mapping in `RoomSearchRepository.search`

Replace the `categoryFilter` string block and the `filteredEntities` logic with:

```kotlin
val allowedCategories = filter.categories()?.map { it.name }?.toSet()
val requiredFlags = filter.requiredEntityFlags()
val filteredEntities = db.mediaDao().getAll().filter { entity ->
    (allowedCategories == null || entity.category in allowedCategories) &&
        (requiredFlags == 0 || (entity.entityFlags and requiredFlags) != 0)
}
```

If it's cleaner, extract the predicate as a pure `internal fun matchesSearchFilter(category: String, entityFlags: Int, filter: SearchFilter): Boolean` in `:data` and test it (Task 5).

Don't touch scoring, `SearchMode`, `VectorSearchEngine` or the snippet logic (Step 10).

**Known limitation (say it in the report):** `HAS_PHONE_OR_URL` will return nothing in real use until Step 09 starts setting `entityFlags` during indexing. That's expected.

## Task 5 — Tests

1. `domain/src/commonTest/.../CategoryMappingTest.kt`:
   - `CollectionKind.ALL.categories()` equals `MediaCategory.entries.toSet()`.
   - Each of `SCREENSHOTS`, `DOCUMENTS`, `RECEIPTS`, `SCANS` maps to exactly its singular category.
   - **Regression guard:** for every non-`ALL` kind, every returned category's `name` is a real `MediaCategory` name, and no kind returns its own `kind.name` as a category (this is the old bug).
   - `SearchFilter.ALL` and `HAS_PHONE_OR_URL` → `null`; each category filter → its singular category.
   - `requiredEntityFlags()`: `HAS_PHONE_OR_URL` has the PHONE and URL bits and not the EMAIL bit; every other filter → `0`.
2. `data/src/commonTest/kotlin/com/o3/vanguard/visionary/data/CollectionSummaryTest.kt` (`:data` already runs host tests, e.g. `DataStoreSettingsRepositoryTest`):
   - Empty input → 5 summaries, all count 0, all `coverUri` null.
   - Input `[PHOTO×3 (newest 100), SCREENSHOT×2 (newest 300), RECEIPT×1 (newest 200)]` → ALL = 6 with the SCREENSHOT cover; SCREENSHOTS = 2; RECEIPTS = 1; DOCUMENTS = 0 with a null cover; SCANS = 0.
   - Summaries come back in `CollectionKind.entries` order.
   - If you extracted `matchesSearchFilter`: category filter match/miss; `HAS_PHONE_OR_URL` with PHONE only → true, URL only → true, EMAIL only → false, 0 → false.

The Room queries themselves can't be host-tested (see `AGENTS.md`). They're covered by the device checks below.

---

## Out of scope (do not touch)

- Search scoring, FTS, `VectorSearchEngine`, `SearchViewModel`, N+1 `getText` (Step 10).
- Setting `entityFlags` or reclassifying items during indexing (Step 09).
- Trash code, including `MediaCategory.valueOf` in `RoomTrashRepository` (Step 08).
- The fakes in `:core:testing` (`FakeRepositoriesAndPorts.kt`). Its `SCANS` uses `origin == APP_SCAN`; leave it for Step 12c.
- Removing dead DAO methods other than `observeByCategory` (Step 12c).

## Verify before reporting

```bash
./gradlew testAndroidHostTest
./gradlew assembleDebug
```

Report the test count from the XML test reports, not the Gradle task count.

Device checks (on a phone or emulator with some photos **and** at least one screenshot):
1. Open the app → Home. The collection counts are non-zero: **All** equals the total number of indexed items, and **Screenshots** is ≥ 1. Covers show if Task 2's cover query worked.
2. Tap **Screenshots** → the gallery shows only screenshots, and isn't empty.
3. Tap **All** → every item shows, as before.
4. Search for a word that appears in a screenshot, with the **Screenshots** filter on → the screenshot appears. Switch to **Receipts** → it disappears (unless it's also a receipt).

## Report back

Write `docs/fix-plan/REPORT_03.md` per `WORKFLOW.md`, update `STATUS.md` (`Next: Reviewer`, `Action: review Step 03`), commit and push.
