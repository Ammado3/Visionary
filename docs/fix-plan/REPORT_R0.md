# Report — Step R0 (merge `Develop` into `review-fixes`)

Done by the second-opinion reviewer, outside the Android Studio loop. Merged `origin/Develop` at `2dc811f` into `review-fixes` at `28da984`.

**Not built or tested.** The merge was done in an environment without the Android SDK. The next reviewer must run `./gradlew testAndroidHostTest assembleDebug` (and an iOS compile) before approving.

## Conflicts and how they were resolved

| File | Resolution |
|---|---|
| `AGENTS.md` | `Develop`'s version, plus a new "Database Rules" section (from Step 01, with the Room testing note) and the `testAndroidHostTest` command |
| `core/navigation/.../Navigation.kt` | `Onboarding(skipIntro: Boolean = false)` from 02b, plus `Develop`'s `Photos`. `Home` removed (it no longer exists on `Develop`). `AppRouteSerializationTest` sample updated to `Onboarding(skipIntro = true)` |
| `composeApp/.../App.kt` | 02b's `chooseStartRoute(resolveStartRoute(), access)`, starting indexing when the route is `Photos`. `StartRoute.kt` and `StartRouteTest` updated: `Home` → `Photos` |
| `core/database/.../Daos.kt` | `Develop`'s albums and favorites queries plus 03's `observeByCategories`. 03's `observeCategoryCounts` and `CategoryCount` dropped (see below) |
| `data/.../RoomRepositories.kt` | `Develop`'s structure: batched `getTexts`, Room settings, real vault restore. Kept 03's category mapping (`observeByCategories`, `matchesSearchFilter`) and 01's vault delete (blob first, keep row on failure). The auto-merge had added a second blob delete; removed |
| `data/.../di/DataModule.kt`, `platform/.../PlatformModule.android.kt`, `PlatformModule.ios.kt` | `Develop`'s versions (constructor references, Room settings, speech recognizer). DataStore binding removed |
| `features/search/.../SearchModule.kt` (modify/delete) | Deleted (the module moved to `features/aisearch` on `Develop`). Added `deleteMediaUseCase = get()` to `features/aisearch/.../SearchModule.kt`. Moved Step 01's `SearchViewModelTest` to `features/aisearch/src/commonTest` |

## Other changes made during the merge

- **Step 02 (DataStore) removed.** `Develop` persists settings in Room. Deleted `DataStoreSettingsRepository` and its test, the DataStore catalog entries and dependencies, and `AppDirectories.settingsFilePath()`. These files are now identical to `Develop`.
- **Step 03 collection counts removed.** `Develop` deleted `MediaRepository.observeCollections()` along with the Home screen, so 03's `observeCollections`, `summarizeCollections`, `observeCategoryCounts`, `CategoryCount` and `CollectionSummaryTest` had nothing to implement. The two `matchesSearchFilter` tests from that file were kept, as `data/.../SearchFilterMatchingTest.kt`.
- **Database fallback.** The merge removes `Develop`'s blanket `fallbackToDestructiveMigration()`. The database is at v5, with no migrations from 3 or 4, so an old install would crash on launch. As a temporary measure, `fallbackToDestructiveMigrationFrom(1, 2, 3, 4)` resets those versions, which is what `Develop` already did. **Step 04 replaces 3 and 4 with real migrations before Phase A is merged into `Develop`.**

## Checked after the merge

- Step 01 fixes are present:
  - `SearchViewModel` delete calls `DeleteMediaUseCase`;
  - `DeleteMediaUseCase` rethrows `CancellationException`;
  - vault delete removes the blob first.
- Step 02b: `OnboardingEntries` uses `key.skipIntro`, and `OnboardingViewModel` still saves `onboardingDone`.
- No conflict markers, and no remaining references to DataStore, `observeByCategory(`, `AppRoute.Home` or `observeCollections`.
- Test fakes used by the Step 01 tests (`deleteMediaCalls`, `trashItems`, `deleteRequests`, `nextResult`, `mediaItemsByKind`) exist in the merged `:core:testkit`.

## Still to do (next reviewer)

1. Build and run all checks.
2. Review Step 03 as merged (`STEP_03_category_mapping.md`, `REPORT_03.md`, plus the changes above).
3. Write the Step 04 spec (migrations 3→4→5) from `PLAN.md`.
