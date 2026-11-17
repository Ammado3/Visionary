# Step 02 — Persist settings

## Context

Visionary is a Kotlin Multiplatform app (Android + iOS). Read `AGENTS.md` in the repo root first; it lists the modules, the commands that work, and the testing rules.

**Problem.** `RoomSettingsRepository` in `data/src/commonMain/kotlin/com/o3/vanguard/visionary/data/RoomRepositories.kt` is only a `MutableStateFlow(AppSettings())`. Nothing is saved, so every app restart resets:
- `onboardingDone` → the user sees onboarding on every launch,
- `indexOnlyWhileCharging` → the Settings toggle forgets its value,
- `vaultLockTimeoutSeconds`.

This step replaces it with a real store using **Jetpack DataStore (Preferences)**, which supports Kotlin Multiplatform (Android + iOS). Keep the change small; do not fix anything outside the scope below.

### Branch and commits

- Work on the existing `review-fixes` branch: `git checkout review-fixes && git pull`.
- One commit per task, message prefixed with the step and task number, e.g. `Step 02.1: Add DataStore dependency and settings file path`.
- Push `review-fixes` when done. Don't merge into `Develop`.

---

## Task 0 — Move the fix-plan docs

`STEP_01_stop_silent_data_loss.md` was committed at the repo root. Create `docs/fix-plan/`, move it there with `git mv`, and put this file (`STEP_02_persist_settings.md`) there too. Commit as `Step 02.0: Move fix-plan docs to docs/fix-plan`.

---

## Task 1 — Add DataStore and a settings file location

1. Add **DataStore Preferences core** to `gradle/libs.versions.toml`:
   - version entry `datastore = "<latest stable>"`. Use the latest **stable** release of `androidx.datastore:datastore-preferences-core` that supports Kotlin Multiplatform with `iosArm64` and `iosSimulatorArm64`. No alpha/beta.
   - library entry `datastore-preferences-core = { module = "androidx.datastore:datastore-preferences-core", version.ref = "datastore" }`.
2. Add it as a `commonMain` dependency of `:data` and of `:platform`.
3. Add a function to the `expect class AppDirectories` in `platform/src/commonMain/.../platform/AppDirectories.kt`:
   ```kotlin
   fun settingsFilePath(): String
   ```
   Implement it on both platforms. The file name must end in `.preferences_pb` (DataStore requires it):
   - **Android:** `File(context.filesDir, "datastore").apply { mkdirs() }` + `/settings.preferences_pb`.
   - **iOS:** use the **Application Support** directory (`NSApplicationSupportDirectory`, `create = true`), **not** `Documents`. Create the directory if it doesn't exist, then return `<appSupport>/settings.preferences_pb`. Do not move or change the existing `Documents`-based paths for vault/trash/scans; a later step handles those.

**Acceptance.** Both `assembleDebug` and the iOS framework compile (`./gradlew :composeApp:compileKotlinIosSimulatorArm64` or the equivalent link task you normally use).

---

## Task 2 — Provide one DataStore instance through Koin

DataStore must have **exactly one instance per file** for the whole process, or it throws at runtime.

1. In **both** `platform/src/androidMain/.../platform/di/PlatformModule.android.kt` and `platform/src/iosMain/.../platform/di/PlatformModule.ios.kt`, add:
   ```kotlin
   single<DataStore<Preferences>> {
       PreferenceDataStoreFactory.createWithPath(
           produceFile = { get<AppDirectories>().settingsFilePath().toPath() },
       )
   }
   ```
   (`toPath()` is `okio.Path.Companion.toPath`, available through DataStore's okio dependency.)
2. Don't create a DataStore anywhere else.

---

## Task 3 — Replace the in-memory settings repository

1. In `:data`, create `data/src/commonMain/kotlin/com/o3/vanguard/visionary/data/DataStoreSettingsRepository.kt`:
   ```kotlin
   class DataStoreSettingsRepository(
       private val dataStore: DataStore<Preferences>,
   ) : SettingsRepository
   ```
   - Keys (keep these exact names; they are now a storage contract):
     - `booleanPreferencesKey("onboarding_done")`
     - `booleanPreferencesKey("index_only_while_charging")`
     - `intPreferencesKey("vault_lock_timeout_seconds")`
   - Missing keys fall back to the defaults in `AppSettings()`. Read the defaults from a `val defaults = AppSettings()`; don't repeat the literal values.
   - `observe()`: `dataStore.data` mapped to `AppSettings`. Add `.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }` before the `map`, using `okio.IOException`. Also apply `distinctUntilChanged()`.
   - `update(transform)`: do the whole read-transform-write **inside one** `dataStore.edit { prefs -> … }` block, so concurrent updates can't overwrite each other:
     ```kotlin
     dataStore.edit { prefs ->
         val updated = transform(prefs.toAppSettings())
         prefs[ONBOARDING_DONE] = updated.onboardingDone
         prefs[INDEX_ONLY_WHILE_CHARGING] = updated.indexOnlyWhileCharging
         prefs[VAULT_LOCK_TIMEOUT_SECONDS] = updated.vaultLockTimeoutSeconds
     }
     ```
2. Delete `RoomSettingsRepository` from `RoomRepositories.kt`.
3. In `data/src/commonMain/.../data/di/DataModule.kt`, change the binding to:
   ```kotlin
   single<SettingsRepository> { DataStoreSettingsRepository(get()) }
   ```
4. Do not change `SettingsRepository`, `AppSettings`, any use case, or any view model.
5. Fake-data mode (`fakeDataModule` in `:core:testing`) must still build. If it binds its own `SettingsRepository`, leave it as is.

---

## Task 4 — Tests

Create `data/src/commonTest/kotlin/com/o3/vanguard/visionary/data/DataStoreSettingsRepositoryTest.kt`. DataStore Preferences core is pure Kotlin and runs in host tests with a real temporary file.

Build the store per test with a unique file:
```kotlin
val file = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "settings-test-${randomUuid()}.preferences_pb"
val dataStore = PreferenceDataStoreFactory.createWithPath(
    scope = backgroundScope,
    produceFile = { file },
)
```
Use `runTest`, and delete the file at the end of each test (`FileSystem.SYSTEM.delete(file, mustExist = false)`).

If `FileSystem.SYSTEM` isn't visible from `commonTest` for these targets, put the test in the Android host test source set used by `testAndroidHostTest` instead, and say so in your report. Don't change the convention plugin for this.

Cover:
1. Empty store → `observe().first()` equals `AppSettings()`.
2. `update { it.copy(onboardingDone = true) }` → `observe().first().onboardingDone` is `true`.
3. **Persistence across instances:** write with one repository, then create a **second** `DataStore` on the **same file** (after the first one's scope is cancelled, e.g. use a child `Job` you cancel) and a second repository → it reads the saved values. This is the test that proves the original bug is fixed.
4. Two updates changing different fields (`onboardingDone`, then `indexOnlyWhileCharging`) → both values present.
5. `vaultLockTimeoutSeconds` round-trips a non-default value (e.g. `120`).

---

## Out of scope (do not touch)

- Making the "charging only" toggle actually affect indexing (WorkManager is a later step).
- Vault lock timeout behaviour (vault steps).
- Moving vault/trash/scan files out of iOS `Documents` (Step 06).
- Category mapping, search, trash, strings, lint.

## Verify before reporting

```bash
./gradlew testAndroidHostTest
./gradlew assembleDebug
```
plus an iOS compile/link of the shared framework.

Manual check on a device or emulator, **from a fresh install**:
1. Complete onboarding → land on Home.
2. Swipe the app away from Recents (fully kill it), relaunch → it opens on **Home**, not onboarding.
3. Settings → turn "index only while charging" on → kill and relaunch → still on.

## Report back

Push `review-fixes`, then reply with:
1. Commit hashes (one per task, 0–4).
2. The DataStore version you used.
3. Where the test lives (`commonTest` or Android host test) and the **number of tests** that ran and passed (from the test report, not the Gradle task count).
4. The result of each manual check above, including whether you also re-did the Step 01 manual checks (delete from Search; cancel the delete dialog; fresh install launches).
5. Anything you could not do as written, and why.
