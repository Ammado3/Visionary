# Report — Step 03

## Commits
- `369104c` Step 03.1: Add category mapping for CollectionKind and SearchFilter
- `0b9793e` Step 03.2: Add observeByCategories and observeCategoryCounts with coverRef to MediaDao
- `841ff12` Step 03.3: Use category mapping in RoomMediaRepository and summarizeCollections
- `2c9d6ec` Step 03.4: Use category and entity flag mapping in RoomSearchRepository
- `ced1af8` Step 03.5: Add tests for category mapping and collection summaries

## Deviations from the spec
- None. `observeByCategories` and `observeCategoryCounts` with `coverRef` were compiled successfully by Room 3 KSP without falling back to `coverRef = null`.

## Checks
- `./gradlew testAndroidHostTest`: PASS — 78 tests run, 78 passed
- `./gradlew assembleDebug`: PASS
- Device checks:
  1. Open the app → Home. Collection counts non-zero (All = total, Screenshots ≥ 1), covers show: not run — needs device
  2. Tap Screenshots → gallery shows only screenshots, not empty: not run — needs device
  3. Tap All → every item shows: not run — needs device
  4. Search word in screenshot with Screenshots filter → screenshot appears; switch to Receipts → disappears: not run — needs device

## Noticed but not touched
- `HAS_PHONE_OR_URL` search filter will return empty in real use until Step 09 starts setting `entityFlags` during indexing.
