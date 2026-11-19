# Architecture cleanup plan

A step-by-step plan for an implementing agent. Written against `origin/Develop` at `0d687d3` (18 Nov 2026). Line numbers can move; **always read the current file before editing it**.

The goals:

- **Phase A:** test fakes no longer compile into the shipped app.
- **Phase B:** one source of truth for tab navigation, a safety test for routes, and Koin wiring that doesn't break silently.
- **Phase C:** the agreed conventions written into `AGENTS.md`.
- **Phase D (follow-up after review):** fix what the first pass got wrong, make iOS compile, and remove redundant dependencies, dead code and unused imports. **Phases A–C are done (commits `bd0ec57`…`2df252d`). If you are starting now, go straight to Phase D.**

**How we work (from Phase D on):** Gemini and Claude run in the same Android Studio window on the same local checkout. Gemini makes the changes in the working copy **without committing**. Claude reviews the local files and diff and re-runs the checks. The human commits once the review passes. Nothing is handed over through git.

---

## 0. Before you start

### 0.1 Rules for the implementing agent

- Read `AGENTS.md` first. Its rules apply to every task. In particular, the build treats **all warnings as errors**, and multiline declarations use trailing commas.
- **Do not modify anything related to the vault, anywhere.** A team member owns all vault work in a separate PR. That means more than `features/vault/`; it also covers:
  - vault adapters in `:platform` (`*VaultCryptoEngine*`, `IosSecurityAndVaultAdapters.kt`, the vault parts of `AndroidPlatformAdapters.android.kt`);
  - `VaultRepository` / `RoomVaultRepository`, `VaultCryptoEngine` / `VaultItem` in `:domain`;
  - vault fakes in `:core:testkit`;
  - `vault_item` / `AppDatabase` schema files.

  This holds **even if it means a build fails.** If a task seems to need a vault change, don't make it; list it (with the exact error) under "Noticed but not touched" in your report. Task D1 only *checks* that a file exists.
- Do only the tasks in this file. Anything else you notice goes in the report, not in the code.
- Edit files with your file-editing tools, not shell commands (`sed`, `echo >`, and so on).
- **Phase D: don't commit, stage or push.** Leave every change in the working copy. Don't run git commands that change the index, history or files (`add`, `commit`, `reset`, `checkout`, `restore`, `stash`, `rebase`, `push`). Read-only commands (`git status`, `git diff`, `git log`) are fine.
- Never claim a check you didn't run. If you can't run something (for example, no emulator), write "not run" and the reason.
- When all Phase D tasks are done, update the report (see "Report back") and **stop**. Claude reviews next.

### 0.1a Working-copy hygiene (the first pass got this wrong)

In Phases A–C, commit `bd0ec57` ("Arch A1") also committed three unrelated files that happened to be modified in the working copy: `AppDatabase/5.json`, two `core/testkit` files and `features/vault/.../VaultContract.kt`. It also left `VaultState.kt` behind, so the commits didn't compile on their own. Now that the human commits, the rules are:

- **Before you start**, run `git status --porcelain` and paste the output into the report as the "baseline". The reviewer uses it to tell your changes apart from what was already there.
- Change only the files a task names. If a task seems to need another file, make the change only if it is needed to compile, and list it under "Deviations".
- Don't edit or revert `core/database/schemas/**`. Room regenerates these on every build.
- "Deviations from the plan" in the report must list **every** file you changed that this plan didn't name, with the reason. "Noticed but not touched" is only for things you did **not** change.

### 0.2 Preconditions (human checks these before handing over)

1. **The vault compile fix.** On `origin/Develop`, `features/vault` doesn't compile because `vault/states/VaultState.kt` was never committed. `VaultContract.kt` is already trimmed in `bd0ec57`. `VaultState.kt` exists in the working copy (untracked), so local builds work. The human must include it in the next commit (see "Human commit checklist").
2. **Branch:** work on the current branch, `review-audit`. **Do not create or check out other branches, and don't use `git worktree`.**
3. **Overlap with `origin/review-fixes`.** That branch has its own fix plan (`docs/fix-plan/`). It was cut from an older `Develop` (`67767e3`) and has diverged a lot: it still has `features/home` and `features/scanner`, and `features/aisearch` doesn't exist there. It also edits some of the files this plan touches:
   - `App.kt`, `Navigation.kt` (it renames `AppRoute.Photos` to `AppRoute.Home` and removes `Navigator.handleBack`),
   - `AppModules.kt`, `DataModule.kt`, `PlatformModule.android.kt` / `PlatformModule.ios.kt`,
   - `AGENTS.md`.

   Its Step 12c already lists "merge `:core:testing` into `:core:testkit`", which Phase A of this plan does. **Whoever lands second will have merge conflicts in these files.** They're small and mechanical, but the human should decide the order. After this plan lands, mark Step 12c's `:core:testing` item as done in `docs/fix-plan/PLAN.md` on `review-fixes`.

### 0.3 Commands

macOS/Linux shell (on Windows use `.\gradlew.bat`):

```bash
./gradlew :androidApp:assembleDebug                      # Android app builds
./gradlew :composeApp:compileKotlinIosSimulatorArm64     # shared code compiles for iOS
./gradlew testAndroidHostTest                            # all host (JVM) tests
./gradlew :core:navigation:testAndroidHostTest           # navigation tests only
```

Report test counts from the test report (`<module>/build/reports/tests/`), not the number of Gradle tasks.

---

## Phase A — Take test fakes out of the production app

### Background

There are two test-helper modules:

| Module | What it holds | Who uses it |
|---|---|---|
| `:core:testkit` | Fakes, factories, MVI test helpers for **unit tests** | Added automatically to every module's `commonTest` by `build-logic/src/main/kotlin/KmpLibraryConventionPlugin.kt` |
| `:core:testing` | A "fake data" app mode (`fakeDataModule`, `FakeData`, `FakeControls`, fake repositories) | **Production code:** `composeApp` `commonMain`, plus a dependency in `features/teleport` `commonTest` that nothing imports |

The fake data mode is switched by `AppConfig.useFakeData`, which is hard-coded to `false` on both platforms:

- `androidApp/src/main/kotlin/com/o3/vanguard/visionary/VisionaryApp.kt`
- `composeApp/src/iosMain/kotlin/com/o3/vanguard/visionary/MainViewController.kt`

So `:core:testing` ships in every build and its code never runs.

**Decision taken in this plan:** remove the fake data mode and delete `:core:testing`. If the team wants a demo mode later, re-add it in a debug-only source set; it must never be a `commonMain` dependency of `:composeApp`.

### Task A1 — Remove the unused `:core:testing` dependency from teleport

**File:** `features/teleport/build.gradle.kts`

**Change:** in `commonTest.dependencies { … }`, delete the line `implementation(project(":core:testing"))`. Leave the other lines alone.

**Before editing:** search `features/teleport/src` for `core.testing`. It should have no matches. If it has any, stop and report.

**Acceptance:** `./gradlew :features:teleport:testAndroidHostTest` passes with the same number of tests as before the change. Run it once before editing to get the baseline.

### Task A2 — Remove the fake data mode from the app

1. `composeApp/src/commonMain/kotlin/com/o3/vanguard/visionary/di/AppModules.kt`:
   - Delete `import com.o3.vanguard.visionary.core.testing.fakeDataModule`.
   - Replace the `if (config.useFakeData) { … } else { … }` block so the three real modules are always added:
     ```kotlin
     fun appModules(config: AppConfig): List<Module> = buildList {
         add(module { single { config } })
         add(coreCommonModule)
         add(domainModule)
         addAll(featureModules)
         add(databaseModule)
         add(dataModule)
         add(platformModule)
     }
     ```
2. `core/common/src/commonMain/kotlin/com/o3/vanguard/visionary/core/common/AppContracts.kt`: remove `val useFakeData: Boolean,` from `data class AppConfig`. Keep `versionName` and `isDebug`.
3. `androidApp/src/main/kotlin/com/o3/vanguard/visionary/VisionaryApp.kt` and `composeApp/src/iosMain/kotlin/com/o3/vanguard/visionary/MainViewController.kt`: remove the `useFakeData = false` argument. Keep a trailing comma after the last remaining argument.
4. `composeApp/build.gradle.kts`: delete `implementation(project(":core:testing"))`.
5. Search the whole repo (excluding `build/` folders and `core/testing/` itself) for `useFakeData`, `fakeDataModule` and `core.testing`. There must be no matches left.

**Acceptance:** `./gradlew :androidApp:assembleDebug` and `./gradlew :composeApp:compileKotlinIosSimulatorArm64` both pass.

### Task A3 — Delete the `:core:testing` module

1. `settings.gradle.kts`: remove `":core:testing"` from the `include(...)` list for `core`.
2. Delete the folder: `git rm -r core/testing`.
3. Search again for `:core:testing` and `core.testing` (excluding `build/` and `docs/`). There must be no matches.

**Do not** move anything from `core/testing` into `core/testkit`. Unit tests already use `testkit`'s own fakes, and nothing else needs these.

**Acceptance:**
- `./gradlew testAndroidHostTest` passes. Compare the test count with the count before Phase A; it must not drop.
- `./gradlew :androidApp:assembleDebug` passes.

---

## Phase B — Navigation and DI consistency

### Background

The mapping between bottom-navigation tabs (`AppTab`, in `:core:designsystem`) and routes (`AppRoute`, in `:core:navigation`) is written out by hand in several places:

- `composeApp/src/commonMain/kotlin/com/o3/vanguard/visionary/App.kt`: `currentTab`, `isSecondaryTab`, and `onSelected` in the bottom bar
- `core/navigation/src/commonMain/kotlin/com/o3/vanguard/visionary/core/navigation/Navigation.kt`: `Navigator.handleBack()`
- `features/aisearch/.../features/search/SearchEntries.kt`: `onNavigate`
- `features/gallery/.../features/gallery/navigation/GalleryNav.kt`: `onPhotos` / `onSearch` / `onVault`
- (vault files too, which are out of scope)

Separately, every route must be registered by hand in `AppNavSavedStateConfig`. A missed one compiles fine and only crashes when the back stack is saved or restored.

Several Koin modules wire constructors with positional `get()` chains such as `PhotoViewModel(get(), get(), get(), get(), get())`. These are easy to get wrong and fail only at runtime.

**Behaviour must not change in Phase B.** These are refactors.

### Task B1 — One tab ↔ route mapping in `:core:navigation`

1. `core/navigation/build.gradle.kts`: in `commonMain.dependencies { … }`, add `implementation(project(":core:designsystem"))`. This doesn't create a cycle: `:core:designsystem` depends only on `:core:common`.
2. Create `core/navigation/src/commonMain/kotlin/com/o3/vanguard/visionary/core/navigation/TabNavigation.kt`:
   ```kotlin
   package com.o3.vanguard.visionary.core.navigation

   import com.o3.vanguard.visionary.core.designsystem.AppTab

   /** The route a bottom-navigation tab opens. */
   fun AppTab.rootRoute(): AppRoute = when (this) {
       AppTab.PHOTOS -> AppRoute.Photos
       AppTab.ALBUMS -> AppRoute.Albums
       AppTab.SEARCH -> AppRoute.Search()
       AppTab.VAULT -> AppRoute.VaultLock
   }

   /** The tab to highlight for this route, or null when the bottom bar is hidden. */
   fun AppRoute.tabOrNull(): AppTab? = when (this) {
       AppRoute.Photos -> AppTab.PHOTOS
       AppRoute.Albums -> AppTab.ALBUMS
       is AppRoute.Search -> AppTab.SEARCH
       AppRoute.VaultLock, AppRoute.VaultGrid -> AppTab.VAULT
       else -> null
   }

   /** Switch tabs. Tabs replace each other; they don't stack. */
   fun Navigator.navigateToTab(tab: AppTab) {
       replaceTop(tab.rootRoute())
   }
   ```
3. `Navigation.kt`: rewrite `Navigator.handleBack()` with the new helpers. Keep the KDoc comment above it.
   ```kotlin
   fun handleBack(): Boolean {
       val tab = (backStack.lastOrNull() as? AppRoute)?.tabOrNull()
       return if (tab != null && tab != AppTab.PHOTOS) {
           navigateToTab(AppTab.PHOTOS)
           true
       } else {
           back()
       }
   }
   ```
   Add `import com.o3.vanguard.visionary.core.designsystem.AppTab`. This behaves exactly like the old code: the old list was `Albums`, `Search`, `VaultLock`, `VaultGrid`, which are exactly the routes whose tab isn't `PHOTOS`.
4. `App.kt`, inside `AppNavHost`:
   - `val currentTab: AppTab? = currentRoute?.tabOrNull()`
   - `val isSecondaryTab = currentTab != null && currentTab != AppTab.PHOTOS`
   - `AppBackHandler(enabled = isSecondaryTab) { navigator.navigateToTab(AppTab.PHOTOS) }`
   - Bottom bar: `onSelected = { tab -> navigator.navigateToTab(tab) }`
   - Import `tabOrNull` and `navigateToTab` from `com.o3.vanguard.visionary.core.navigation`. Remove any imports that are no longer used (unused imports are warnings, and warnings fail the build).
5. `features/aisearch/src/commonMain/kotlin/com/o3/vanguard/visionary/features/search/SearchEntries.kt`: replace the `when (tab) { … }` inside `onNavigate` with
   ```kotlin
   onNavigate = { tab -> if (tab != AppTab.SEARCH) navigator.navigateToTab(tab) },
   ```
   Keep the `SEARCH` check: re-selecting Search must not reset the query. Don't change `SearchScreen.kt`.
6. `features/gallery/src/commonMain/kotlin/com/o3/vanguard/visionary/features/gallery/navigation/GalleryNav.kt`, in the `AppRoute.Albums` entry:
   - `onPhotos = { navigator.navigateToTab(AppTab.PHOTOS) },`
   - `onSearch = { navigator.navigateToTab(AppTab.SEARCH) },`
   - `onVault = { navigator.navigateToTab(AppTab.VAULT) },`
   - Add the `AppTab` and `navigateToTab` imports. Don't change `AlbumsScreen.kt`.
7. **Don't touch** `features/vault/**`. Its `onNavigate` blocks stay as they are. They're listed in "Notes for the vault PR" below.

**Tests:** create `core/navigation/src/commonTest/kotlin/com/o3/vanguard/visionary/core/navigation/TabNavigationTest.kt` (the `commonTest` folder is new). Use `kotlin.test`. Cases:
- For every `AppTab` in `AppTab.entries`: `tab.rootRoute().tabOrNull() == tab`.
- `AppRoute.VaultGrid.tabOrNull() == AppTab.VAULT`.
- `AppRoute.Settings`, `AppRoute.Viewer(1L)` and `AppRoute.TrashBin` give `tabOrNull() == null`.
- `handleBack` with stack `[Albums]` returns `true`, and the stack becomes `[Photos]`.
- `handleBack` with stack `[Photos]` returns `false`, and the stack is unchanged.
- `handleBack` with stack `[Photos, Settings]` returns `true`, and the stack becomes `[Photos]`.
- `navigateToTab(AppTab.SEARCH)` on `[Photos]` gives `[Search()]`.

Build stacks with `mutableListOf<NavKey>(…)` and pass them to `Navigator(…)`.

**Acceptance:**
- `./gradlew :core:navigation:testAndroidHostTest` passes, and the new tests run (check the report).
- `./gradlew :androidApp:assembleDebug` passes.
- Device check (human, if the agent has no emulator): switch through all four tabs; back from Albums, Search and Vault goes to Photos; back on Photos leaves the app.

### Task B2 — A test that fails when a route isn't registered for saving

1. `Navigation.kt`: move the `SerializersModule { polymorphic(NavKey::class) { … } }` block out of `AppNavSavedStateConfig` into its own public value, without changing its contents:
   ```kotlin
   val AppRouteSerializersModule = SerializersModule {
       polymorphic(NavKey::class) {
           // …the existing subclass(...) lines, unchanged…
       }
   }

   val AppNavSavedStateConfig = SavedStateConfiguration {
       serializersModule = AppRouteSerializersModule
   }
   ```
2. Create `core/navigation/src/commonTest/kotlin/com/o3/vanguard/visionary/core/navigation/AppRouteSerializationTest.kt`:
   ```kotlin
   package com.o3.vanguard.visionary.core.navigation

   import androidx.navigation3.runtime.NavKey
   import kotlinx.serialization.PolymorphicSerializer
   import kotlinx.serialization.json.Json
   import kotlin.test.Test
   import kotlin.test.assertEquals

   class AppRouteSerializationTest {
       private val json = Json { serializersModule = AppRouteSerializersModule }

       // One sample per AppRoute subclass. When you add a route, add it here too.
       private val samples: List<AppRoute> = listOf(
           AppRoute.Onboarding,
           AppRoute.Photos,
           AppRoute.Albums,
           AppRoute.Gallery(albumId = "1", albumName = "Camera"),
           AppRoute.Viewer(mediaId = 1L, highlight = "total"),
           AppRoute.Search(initialQuery = "receipt"),
           AppRoute.VaultLock,
           AppRoute.VaultGrid,
           AppRoute.VaultViewer(itemId = "v1"),
           AppRoute.Settings,
           AppRoute.IndexHealth,
           AppRoute.BiometricSetup,
           AppRoute.TrashBin,
           AppRoute.Favorites,
           AppRoute.TeleportSender(uri = "content://media/1", displayName = "a.jpg"),
           AppRoute.TeleportReceiver,
       )

       @Test
       fun everyRouteHasASample() {
           // A sealed serializer's descriptor is ("type", "value"); "value" has one element per subclass.
           val subclassCount = AppRoute.serializer().descriptor.getElementDescriptor(1).elementsCount
           assertEquals(subclassCount, samples.map { it::class }.distinct().size)
       }

       @Test
       fun everyRouteRoundTripsAsNavKey() {
           val serializer = PolymorphicSerializer(NavKey::class)
           samples.forEach { route ->
               val encoded = json.encodeToString(serializer, route)
               assertEquals<NavKey>(route, json.decodeFromString(serializer, encoded))
           }
       }
   }
   ```
   **Verify:** check the current `AppRoute` constructors before you write the samples. If the descriptor-count trick doesn't compile or gives a wrong number with the project's kotlinx.serialization version (1.8.0), replace `everyRouteHasASample` with a comment explaining why, and say so in the report. Don't add `@OptIn` or `@Suppress` to work around a warning without explaining it in the report.
3. Prove the test works. Temporarily comment out one `subclass(...)` line, run the test, confirm `everyRouteRoundTripsAsNavKey` **fails**, then restore the line. Write the result in the report. Don't commit the broken state.

**Acceptance:** `./gradlew :core:navigation:testAndroidHostTest` passes, and the report confirms the negative check.

### Task B3 — Koin: constructor references instead of `get()` chains

**Rule:**
- Use `viewModelOf(::X)`, `singleOf(::X)` or `factoryOf(::X)` when every constructor parameter comes from Koin.
- Bind to an interface with `singleOf(::Impl) bind Interface::class`. The imports are `org.koin.core.module.dsl.singleOf` and `org.koin.dsl.bind`.
- Keep a lambda **only** when the class takes runtime parameters (`parametersOf`), or when a constructor parameter has a default value you rely on. In those lambdas, use **named arguments**, for example `PhotoViewerViewModel(mediaIdValue = mediaIdValue, highlight = highlight, observeItem = get(), …)`. Read each constructor to get the names.

**Files:**

1. `data/src/commonMain/kotlin/com/o3/vanguard/visionary/data/di/DataModule.kt`: every line can use `singleOf`. Example:
   ```kotlin
   val dataModule = module {
       singleOf(::VectorSearchEngine)
       singleOf(::RoomMediaRepository) bind MediaRepository::class
       singleOf(::RoomSearchRepository) bind SearchRepository::class
       singleOf(::RoomIndexRepository) bind IndexRepository::class
       singleOf(::RoomSettingsRepository) bind SettingsRepository::class
       singleOf(::RoomVaultRepository) bind VaultRepository::class
       singleOf(::RoomTrashRepository) bind TrashRepository::class
   }
   ```
   Replace the wildcard imports (`data.*`, `domain.repository.*`) with explicit imports.
2. `features/gallery/src/commonMain/kotlin/com/o3/vanguard/visionary/features/gallery/di/GalleryModule.kt`:
   - `AlbumsViewModel`, `FavoriteViewModel`, `PhotoViewModel` and `TrashViewModel` become `viewModelOf(::X)`.
   - `AlbumGalleryViewModel` and `PhotoViewerViewModel` take runtime parameters. Keep their lambdas and switch to named arguments.
   - Fix the indentation of the `AlbumGalleryViewModel` line.
3. `features/aisearch/src/commonMain/kotlin/com/o3/vanguard/visionary/features/search/SearchModule.kt`: it has a runtime parameter, so keep the lambda and use named arguments.
4. `platform/src/androidMain/kotlin/com/o3/vanguard/visionary/platform/di/PlatformModule.android.kt` and `platform/src/iosMain/kotlin/com/o3/vanguard/visionary/platform/di/PlatformModule.ios.kt`: `singleOf(::X)` / `singleOf(::Impl) bind Port::class` for every line. Replace wildcard imports with explicit ones.
5. `features/teleport/src/androidMain/kotlin/com/o3/vanguard/visionary/feature/teleport/di/TeleportPlatformModule.android.kt`: `factoryOf(::Impl) bind Port::class`. Leave the iOS teleport module alone; its lambdas create objects or throw errors on purpose.
6. **Don't touch** `features/vault/**/VaultModule.kt` or `BiometricModule.kt`. They already use `viewModelOf`.

**Why care:** Koin errors happen at **runtime**, not compile time. A wrong binding compiles and then crashes when a screen opens.

**Acceptance:**
- `./gradlew :androidApp:assembleDebug` and `./gradlew :composeApp:compileKotlinIosSimulatorArm64` pass.
- `./gradlew testAndroidHostTest` passes.
- **Device smoke test** (required; mark "not run — needs device" if you can't). Launch the app and open each of these once without a crash: Photos, Albums, one album, a photo in the viewer, Favorites, Trash, Search (type a query), Settings, Teleport receiver. Then open the Vault tab, only to check that Koin resolves it (don't test vault behaviour). If anything crashes, include the logcat `NoDefinitionFoundException` / `InstanceCreationException` message in the report.

---

## Phase C — Record the conventions in `AGENTS.md`

### Task C1 — Update `AGENTS.md`

1. In "Project Structure & Module Organization", change the `core/` list from `common, mvi, navigation, designsystem, database, and testing` to `common, mvi, navigation, designsystem, database, and testkit`.
2. In "Testing Guidelines", replace "Tests are not currently checked in." with: "Shared test fakes, factories and MVI helpers live in `:core:testkit`, which the build adds to every module's `commonTest` automatically. Production code must never depend on `:core:testkit`."
3. Add a new section after "Coding Style & Naming Conventions":

   ```markdown
   ## Architecture Rules

   - **Module dependencies:** features depend on `:domain` and `:core:*` only, never on `:data`, `:core:database` or `:platform`. (Known exception: `:features:onboarding` imports `AppPermissionGate` from `:platform`; to be removed.) `:domain` depends only on `:core:common`.
   - **Use cases:** add a use case only when it combines several repositories or ports, or contains business logic (for example `DeleteMediaUseCase`). For a single read or write, a ViewModel may call the repository directly. Don't add pass-through use cases.
   - **Koin:** use `viewModelOf` / `singleOf` / `factoryOf` with constructor references and `bind` for interfaces. Use a lambda only for runtime parameters, with named arguments.
   - **Navigation:** every route is an `AppRoute` in `:core:navigation`. When you add one, register it in `AppRouteSerializersModule` and add a sample to `AppRouteSerializationTest`. Tab ↔ route mapping lives only in `TabNavigation.kt`; switch tabs with `navigator.navigateToTab(tab)`.
   - **Packages:** a module's package is `com.o3.vanguard.visionary.<layer>.<module>`, matching its folder name (for example `features/gallery` → `com.o3.vanguard.visionary.features.gallery`). Use correct English spelling in package and file names. Known exceptions, to be renamed in a dedicated PR: `feature.teleport` (should be `features.teleport`), `features.search` in `features/aisearch`, `features.vault.valutscreen`, `core.designsystem.component.customekeyboard`.
   - **Screen files:** each screen has `<Name>Screen.kt` (Route + stateless Screen + Preview), `<Name>ViewModel.kt`, and its State, Intent and Event types. For new screens, put those three types in one `<Name>Contract.kt`.
   ```

**Acceptance:** `AGENTS.md` renders correctly as Markdown and no other file changes in this commit.

---

## Phase D — Follow-up after review

### What the review found

Phases A–C were reviewed on 18 Nov 2026. The code changes are correct: 62 host tests pass and `:androidApp:assembleDebug` passes. But:

1. **The commits don't compile on their own.** `bd0ec57` committed the trimmed `VaultContract.kt` but not `vault/states/VaultState.kt`, which is still untracked. Local builds pass only because the file exists in the working copy. The human fixes this at commit time.
2. `bd0ec57` also committed files outside its task (`AppDatabase/5.json`, `core/testkit/.../VaultSecurityFakes.kt`, `core/testkit/.../ModelFactories.kt`), and the report said "Deviations: None" and listed the testkit files as "not touched". The testkit changes are correct and needed; leave them in. Don't revert anything; just follow section 0.1a from now on.
3. **iOS doesn't compile** (`:core:designsystem:compileKotlinIosSimulatorArm64` fails with `Unresolved reference 'Preview'`). This was already the case on `Develop`; the cause is below (Task D2).
4. Several build files declare dependencies that the convention plugins already add, or that nothing uses. Some dependencies are hard-coded instead of coming from the version catalog. One `commonTest` block was added in Phase B2 that the convention already provides.
5. There are unused composable parameters, an empty Kotlin file, a dead splash callback, and unused imports.

Do the tasks in order. Run the checks named in each task before moving on to the next one.

### Task D1 — Check `VaultState.kt` is present (no edits)

1. Confirm `features/vault/src/commonMain/kotlin/com/o3/vanguard/visionary/features/vault/vault/states/VaultState.kt` exists. Don't edit, move or stage it.
2. Run `./gradlew :features:vault:compileAndroidMain`. It must pass.

**If the file is missing**, stop and report. Don't recreate it.

### Task D2 — Make `@Preview` work on iOS

**Root cause (verified in the Gradle cache).** The catalog alias `composeUiToolingPreview` points to `org.jetbrains.compose.components:components-ui-tooling-preview`, which is deprecated.
- Its **iOS** artifact contains only the package `org.jetbrains.compose.ui.tooling.preview`.
- Its **Android** artifact depends on `androidx.compose.ui:ui-tooling-preview`. That's the only reason `import androidx.compose.ui.tooling.preview.Preview` works on Android.
- The replacement artifact `org.jetbrains.compose.ui:ui-tooling-preview` (same version, 1.12.2) contains `androidx.compose.ui.tooling.preview` for iOS too.
- Every file except one already imports `androidx.compose.ui.tooling.preview.Preview`, so no import changes are needed apart from step 4.

**Change:**

1. `gradle/libs.versions.toml`: change the module of `composeUiToolingPreview` (keep the alias name and `version.ref`):
   ```toml
   composeUiToolingPreview = { module = "org.jetbrains.compose.ui:ui-tooling-preview", version.ref = "composeMultiplatform" }
   ```
   Leave the unused `androidx-ui-tooling-preview` entry alone for now; Task D3 removes it.
2. `build-logic/src/main/kotlin/KmpComposeConventionPlugin.kt`: make every Compose module get the preview annotation in `commonMain`, plus the preview renderer on Android:
   ```kotlin
   import org.gradle.api.Plugin
   import org.gradle.api.Project
   import org.gradle.api.artifacts.VersionCatalogsExtension
   import org.gradle.kotlin.dsl.configure
   import org.gradle.kotlin.dsl.getByType
   import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

   class KmpComposeConventionPlugin : Plugin<Project> {
       override fun apply(target: Project) {
           with(target) {
               pluginManager.apply("visionary.kmp.library")
               pluginManager.apply("org.jetbrains.compose")
               pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

               val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

               extensions.configure<KotlinMultiplatformExtension> {
                   sourceSets.named("commonMain").configure {
                       dependencies {
                           implementation(libs.findLibrary("composeUiToolingPreview").get())
                       }
                   }
                   sourceSets.matching { it.name == "androidMain" }.configureEach {
                       dependencies {
                           implementation(libs.findLibrary("composeUiTooling").get())
                       }
                   }
               }
           }
       }
   }
   ```
   Use `matching { … }.configureEach`, not `named("androidMain")`, for Android. It works whether or not the Android source set exists yet when the plugin runs.
3. Remove the explicit preview and tooling lines the convention now provides. **Not** in `features/vault` (see vault notes).
   - `core/designsystem/build.gradle.kts`: remove `implementation(libs.composeUiToolingPreview)` from `commonMain`, and `implementation(libs.composeUiTooling)` from `androidMain`.
   - `features/onboarding/build.gradle.kts`: remove `implementation(libs.composeUiTooling)` from `androidMain` (delete the now-empty `androidMain.dependencies { }` block), and remove `commonMainImplementation(libs.composeUiToolingPreview)` from the bottom `dependencies { }` block.
   - `features/settings/build.gradle.kts`: remove **both** `implementation(libs.composeUiToolingPreview)` and the hard-coded, older `implementation("org.jetbrains.compose.components:components-ui-tooling-preview:1.7.3")`.
4. `core/designsystem/src/commonMain/kotlin/com/o3/vanguard/visionary/core/designsystem/AppBottomNavigation.kt`: delete `import org.jetbrains.compose.ui.tooling.preview.Preview`. Nothing in that file uses it, and the package no longer exists after step 1.
5. Search all `*.kt` files (excluding `build/`) for `org.jetbrains.compose.ui.tooling.preview`. There must be no matches.

**Acceptance:**
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64` passes. **If it still fails:**
  - errors in `features/vault/**`: don't fix them; copy them into the report;
  - errors elsewhere that are clearly Android-only APIs in `commonMain`: don't fix them either; list each file, line and error in the report, so the next step can be planned.
- `./gradlew :androidApp:assembleDebug` passes.
- Human check in Android Studio: open `core/designsystem/.../component/Buttons.kt` and confirm the preview pane still renders.

Suggested commit message, for the human: `Arch D2: Use multiplatform ui-tooling-preview so previews compile on iOS`.

### Task D3 — Remove redundant and hard-coded dependencies

**Rule.** A dependency is redundant when a convention plugin already adds it, or when nothing in the module uses it. Before removing an "unused" dependency, search the module's `src/` for its package; the search for each item is given below. After **each** file, run `./gradlew :androidApp:assembleDebug`. If removing a line breaks the build, put it back, and write the error in the report. Don't add other dependencies to compensate.

What the conventions already provide (from `build-logic/src/main/kotlin/`):
- `visionary.kmp.library`: `commonTest` gets `kotlin("test")`, `kotlinx-coroutines-test`, `turbine` and `:core:testkit`.
- `visionary.kmp.compose`: applies `org.jetbrains.compose` and the Compose compiler. After D2 it also adds the preview annotation and tooling.
- `visionary.kmp.feature`: `commonMain` gets `:domain`, `:core:common`, `:core:mvi`, `:core:navigation`, `:core:designsystem`, Material 3, the extended icons, Koin Compose, lifecycle Compose and Coil.

Changes:

| File | Remove / change | Why |
|---|---|---|
| `core/navigation/build.gradle.kts` | Remove the whole `commonTest.dependencies { implementation(kotlin("test")) }` block (added in B2) | The convention adds it |
| `features/teleport/build.gradle.kts` | Remove the whole `commonTest.dependencies { … }` block **and** the whole `androidUnitTest.dependencies { … }` block | The convention adds the `commonTest` ones. `androidUnitTest` isn't this module's host-test source set: its host tests live in `src/androidHostTest`, and there is no `src/androidUnitTest`. Check that `:features:teleport:testAndroidHostTest` still runs **38** tests. |
| `composeApp/build.gradle.kts` | Remove the whole `commonTest.dependencies { … }` block | The convention adds it (and `composeApp` has no tests yet) |
| `features/settings/build.gradle.kts` | Remove `implementation(project(":core:designsystem"))` | The feature convention adds it |
| `features/gallery/build.gradle.kts` | Remove `alias(libs.plugins.compose.multiplatform)` from `plugins { }`. Replace `implementation("org.jetbrains.compose.components:components-resources:1.12.2")` with `implementation(compose.components.resources)`. | The convention applies the Compose plugin; the version must not be hard-coded (other modules use `compose.components.resources`). |
| `features/onboarding/build.gradle.kts` | Move `commonMainImplementation(compose.components.resources)` from the bottom `dependencies { }` block into `kotlin { sourceSets { commonMain.dependencies { … } } }` as `implementation(compose.components.resources)`. Then delete the empty bottom `dependencies { }` block. | Same style as every other module |
| `core/designsystem/build.gradle.kts` | In `androidMain`, remove `implementation(libs.androidx.activity.compose)` and `implementation(libs.lifecycle.runtime.compose)`. First confirm that searching `core/designsystem/src` for `import androidx.activity` and `import androidx.lifecycle` finds nothing. | Unused |
| `core/designsystem/build.gradle.kts` + `gradle/libs.versions.toml` | Replace hard-coded `implementation("androidx.core:core-ktx:1.15.0")` with `implementation(libs.androidx.core.ktx)`, and add to the catalog `[libraries]`: `androidx-core-ktx = { module = "androidx.core:core-ktx", version = "1.15.0" }` | It is used (`SystemBarColors.android.kt`), but versions belong in the catalog. Keep 1.15.0; don't upgrade. |
| `gradle/libs.versions.toml` | Remove the `androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }` entry | No build file uses `libs.androidx.ui.tooling.preview` (confirm with a search), and it has no version |
| `androidApp/build.gradle.kts` | Remove `implementation(project(":data"))` | Nothing in `androidApp/src` imports `com.o3.vanguard.visionary.data`. Keep `:core:common`, `:core:designsystem`, `:domain` and `:platform`; `MainActivity` / `VisionaryApp` / `IndexingForegroundService` import from them. |
| `data/build.gradle.kts` | Remove `implementation(libs.room3.runtime)` | `:core:database` already exposes it with `api(libs.room3.runtime)` |

**Don't touch** `features/vault/build.gradle.kts` (see vault notes), the `platform` Compose dependencies, or `features/onboarding`'s `:platform` dependency (a known exception recorded in `AGENTS.md`).

**Acceptance:** `./gradlew :androidApp:assembleDebug`, `./gradlew testAndroidHostTest` (still 62 tests, or more) and `./gradlew :composeApp:compileKotlinIosSimulatorArm64` pass (or fail only as recorded in D2).

Suggested commit message, for the human: `Arch D3: Remove redundant and hard-coded dependencies`.

### Task D4 — Remove dead code and unused imports

Remove only code that is provably unused. **Keep explanatory comments** (for example `// Title`, `// rollback`, KDoc). Remove only *commented-out code*. Outside `features/vault`, a search found no commented-out code. If you find any, remove it, and list it in the report.

Don't delete classes that look unused but are planned for use on `review-fixes`: `ResolveStartRouteUseCase`, `SmartMediaClassifier`, `EntityExtractor`, `VectorSearchEngine`, `SyncCheckpointEntity`, `IndexJobDao`.

1. **Empty file.** Delete `core/designsystem/src/androidMain/kotlin/com/o3/vanguard/visionary/core/designsystem/component/AppPermissionGate.android.kt`. It contains only a `package` line and a comment saying the code lives in `:platform`.
2. **Unused `onNavigate` in Search.**
   - In `features/aisearch/src/commonMain/kotlin/com/o3/vanguard/visionary/features/search/SearchScreen.kt`, `onNavigate: (AppTab) -> Unit` is a parameter of `SearchRoute` (around line 103) and `SearchScreen` (around line 131), and is only passed along, never called. Remove it from both signatures, from the `SearchScreen(...)` call inside `SearchRoute`, and from any `@Preview` that passes it.
   - In `SearchEntries.kt`, remove the `onNavigate = …` argument, then the now-unused imports `AppTab` and `navigateToTab`. Also remove the blank lines that B1 left between the imports.
   - In `SearchScreen.kt`, also remove the unused imports `com.o3.vanguard.visionary.core.designsystem.AppBottomNavigation` and, if no longer used, `AppTab`.
3. **Unused tab callbacks in Albums.**
   - In `features/gallery/src/commonMain/kotlin/com/o3/vanguard/visionary/features/gallery/presentation/albums/AlbumsScreen.kt`, `AlbumsRoute` declares `onPhotos`, `onSearch` and `onVault` (around lines 74–76) but never uses them.
   - **Careful:** there is a *different* `onSearch` parameter on the private `AlbumsHeader` (around line 204) and a local lambda `onSearch = { searchOpen = true }` (around line 89). Both are used; keep them. Remove only the three `AlbumsRoute` parameters.
   - In `GalleryNav.kt`, remove the three matching arguments from the `AlbumsRoute(...)` call, then the now-unused imports `AppTab` and `navigateToTab`.
   - In `AlbumsScreen.kt`, remove the unused imports `AppBottomNavigation` and `AppTab`.
4. **Dead splash callback.**
   - `composeApp/src/commonMain/kotlin/com/o3/vanguard/visionary/screens/SplashScreen.kt`: `SplashScreen(onSplashFinished: () -> Unit)` runs its own `LaunchedEffect { delay(3.seconds); onSplashFinished() }`. But `App.kt` drives the timing itself (`delay(SPLASH_DURATION_MS)`) and passes a no-op lambda.
   - Remove the `onSplashFinished` parameter and that `LaunchedEffect`, so the signature is `fun SplashScreen()`. Remove any imports that become unused (`LaunchedEffect`, `delay`, `seconds`; check each).
   - Update any `@Preview` in that file.
   - In `App.kt`, change the call to `SplashScreen()` and delete the `// Permission flow handled by LaunchedEffect` lambda.
   - Behaviour doesn't change: the callback did nothing.
5. **Unused imports.** Remove each of these, checking first that the name isn't used in the file:
   - `core/designsystem/.../AppBottomNavigation.kt`: `androidx.compose.runtime.mutableStateOf`
   - `core/designsystem/.../component/Card.kt`: `androidx.compose.foundation.layout.width`
   - `features/gallery/.../albumgallery/AlbumGalleryScreen.kt`: `androidx.compose.foundation.layout.Spacer`
   - `features/gallery/.../photos/PhotoScreen.kt`: `androidx.compose.material.icons.filled.Edit`
   - `features/gallery/.../photoviewer/PhotoViewerScreen.kt`: `androidx.compose.ui.draw.clip`, `com.o3.vanguard.visionary.domain.model.MediaId`
   - `features/onboarding/.../OnboardingScreen.kt`: `androidx.compose.foundation.layout.Box`, `androidx.compose.foundation.layout.Row`
   - `platform/src/androidMain/.../android/AndroidPlatformAdapters.android.kt`: `androidx.activity.ComponentActivity`
   - `platform/src/iosMain/.../ios/PhAssetResolver.kt`: `platform.UIKit.UIImage`

   (This list came from a text search. If a name turns out to be used, for example as an extension function or in a KDoc link, keep that import and say so in the report.)

**Acceptance:** `./gradlew :androidApp:assembleDebug`, `./gradlew testAndroidHostTest` and `./gradlew :composeApp:compileKotlinIosSimulatorArm64` pass (or fail only as recorded in D2). Behaviour doesn't change.

Suggested commit message, for the human: `Arch D4: Remove dead code and unused imports`.

### Task D5 — Small `AGENTS.md` fix

`AGENTS.md` gives `features/home` as an example module, but it doesn't exist on this branch. Change that example to `features/gallery`. Nothing else.

Suggested commit message, for the human: `Arch D5: Fix module example in AGENTS.md`.

### Task D6 — Device smoke test (human)

This hasn't been run for Phases A–D. Koin and navigation mistakes only show up at runtime. On an emulator or device:

1. Install: `./gradlew :androidApp:installDebug`, then launch the app.
2. Open each screen once without a crash: Photos, Albums, one album, a photo in the viewer, Favorites, Trash, Search (type a query), Settings, Teleport receiver, and the Vault tab (only checking that it opens).
3. Switch through all four bottom tabs. Back from Albums, Search and Vault goes to Photos; back on Photos leaves the app.
4. The splash screen still shows on launch and moves on by itself.

If anything crashes, copy the logcat `NoDefinitionFoundException` / `InstanceCreationException` / stack trace into the report.

### Task D7 — Undo the iOS vault adapter edits (from the Phase D review)

**Why.** Vault code is owned by a teammate, and **nothing related to the vault may be modified in this plan**, wherever it lives (see 0.1). To make iOS compile, D2 edited two vault adapters in `platform/src/iosMain/kotlin/com/o3/vanguard/visionary/platform/ios/`. Those edits must be undone. (The new `IosVaultCryptoEngine.restoreToStorage` also returned success without restoring anything. `RoomVaultRepository.restore()` would then delete the vault row, so the photo would vanish on iOS. That's one more reason this belongs with the vault owner.)

**Change.** Restore both files to exactly their committed content, by editing them; you may not use `git checkout` or `git restore`. Afterwards `git diff -- <file>` must print nothing for both files.

1. `IosSecurityAndVaultAdapters.kt`: in `class IosVaultCryptoEngine`, delete the two members D2 added, `override suspend fun restoreToStorage(itemId: String, mime: String?, takenAtMs: Long)` and `override fun getBlobPath(itemId: String)`, and the blank line after them. They sit just before `@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class) private suspend fun loadDataFromUri`.
2. `IosStubAdapters.ios.kt`: in `class IosStubVaultCryptoEngine`, change the multi-line `restoreToStorage(itemId, mime, takenAtMs)` back to the original single parameter:
   ```kotlin
       override suspend fun restoreToStorage(itemId: String): AppResult<String> =
           AppResult.Err(AppError.Unsupported("OS_NOT_SUPPORTED_YET"))
   ```
3. Check: `git diff -- platform/src/iosMain/kotlin/com/o3/vanguard/visionary/platform/ios/IosSecurityAndVaultAdapters.kt platform/src/iosMain/kotlin/com/o3/vanguard/visionary/platform/ios/IosStubAdapters.ios.kt` prints nothing.

**Expected result:** `:composeApp:compileKotlinIosSimulatorArm64` **fails again**, but **only** in those two vault adapter classes. They don't implement the `VaultCryptoEngine` interface the vault PR changed, which gives errors like "is not abstract and does not implement abstract member" or "'restoreToStorage' overrides nothing". That is expected; the vault owner fixes it. Copy the exact errors into the report. If iOS errors appear in any **other** file, that's a real problem: report it.

`./gradlew :androidApp:assembleDebug` and `./gradlew testAndroidHostTest` (62 tests) must still pass.

In the report's Phase D "Deviations", replace the line about the iOS stubs with: "D2 edited the iOS vault adapters to compile; reverted in D7 because vault code is out of scope. iOS compile now fails only in those adapters, which the vault PR owns."

**Also:** during D4 the deleted file `core/designsystem/.../AppPermissionGate.android.kt` was **staged** (`git status` shows `D ` in the index column). That breaks the "don't stage" rule, but it's harmless; leave it staged. Just note it in the report.

---

## Out of scope (do not do in this plan)

| Item | Why it's deferred |
|---|---|
| Anything vault-related, anywhere (see 0.1) | A teammate's PR covers it |
| The iOS compile errors in the vault adapters | Caused by the vault PR's `VaultCryptoEngine` change; the vault owner fixes them |
| Database migrations / `fallbackToDestructiveMigration` / `AppDatabase` schema files | Already fixed in `review-fixes` Step 01; the `vault_item` schema issue belongs to the vault PR (see notes) |
| Package renames (`feature.teleport`, `aisearch`, `valutscreen`, `customekeyboard`) | About 60 files of churn. Do it in a dedicated PR after `review-fixes` and the vault PR are merged, to avoid conflicts. |
| Splitting `RoomRepositories.kt`, `AndroidPlatformAdapters.android.kt`, `SearchScreen.kt` | `review-fixes` Steps 03 and 08–11 change these files heavily; splitting now guarantees conflicts |
| Removing the Koin module and `CoroutineScope` from `:core:common`; `BuildConfig` for `AppConfig` | Covered by `review-fixes` Steps 12a and 12c |
| Wildcard imports (`import ….*`) | Not unused; changing them is churn. Replace them only in files a task already edits. |

## Notes for the vault PR (pass these to the teammate)

When this plan is merged, the vault code should follow the same conventions:

- **iOS doesn't compile until the vault adapters are updated.** `IosVaultCryptoEngine` (`platform/src/iosMain/.../IosSecurityAndVaultAdapters.kt`) and `IosStubVaultCryptoEngine` (`IosStubAdapters.ios.kt`) don't implement the current `VaultCryptoEngine`: `restoreToStorage(itemId, mime, takenAtMs)` and `getBlobPath`. Until iOS restore really exists, `restoreToStorage` must return `AppResult.Err(...)`, **never** `Ok` with a placeholder. `RoomVaultRepository.restore()` deletes the vault row after an `Ok`, so a fake success loses the user's photo.
- **Database version (important).** Commit `fa7b050` added a `filePath` column to `vault_item` without raising `AppDatabase` from version 5, so `5.json` now has a different identity hash for the same version. Any install that already has a version-5 database created before that commit crashes on launch ("Room cannot verify the data integrity"). Fix: bump to version 6 with a `Migration(5, 6)` that runs `ALTER TABLE vault_item ADD COLUMN filePath TEXT NOT NULL DEFAULT ''`, and commit `6.json`.
- Replace the hand-written `when (tab) { … }` blocks in `vault/VaultEntries.kt`, `valutscreen/VaultEntries.kt` and `biometricsetup/BiometricSetup.kt` with `navigator.navigateToTab(tab)`. Keep the `AppTab.VAULT -> Unit` behaviour: `if (tab != AppTab.VAULT) navigator.navigateToTab(tab)`.
- Keep a single copy of the vault screens. Either `vault/` or `valutscreen/` should go.
- Remove the commented-out code. `vault/VaultScreen.kt` has a commented-out `VaultGridRoute` and `VaultGridScreen` (about 50 lines). Check `valutscreen/VaultScreen.kt` and `biometricsetup/BiometricScreen.kt` for commented-out lines too.
- `features/vault/build.gradle.kts`: remove `implementation(project(":core:designsystem"))` (the feature convention adds it) and, after D2, `implementation(libs.composeUiToolingPreview)` (the Compose convention adds it).
- Remove the unused import of `VaultViewModel` in `biometricsetup/di/BiometricModule.kt`, and rename the Koin module `biometric` to `biometricModule`.
- Any new vault route must be added to `AppRouteSerializersModule` and `AppRouteSerializationTest`.

---

## Report back

For Phase D, **add a `## Phase D` section to the existing `docs/ARCHITECTURE_CLEANUP_REPORT.md`** (same headings as the template below; don't rewrite the Phase A–C part). Also correct the Phase A–C part: under "Deviations", list the extra files committed in `bd0ec57`, and move the testkit item out of "Noticed but not touched". Don't commit it; Claude reviews it with the rest.

The template used for Phases A–C:

```markdown
# Architecture cleanup — report

## Commits
- `<hash>` Arch A1: …
- …

## Deviations from the plan
None / list each with the reason.

## Checks
- Test count before Phase A: N. After all phases: N.
- `./gradlew testAndroidHostTest`: PASS/FAIL (N tests run, N passed)
- `./gradlew :core:navigation:testAndroidHostTest`: PASS/FAIL (N tests)
- B2 negative check (one route unregistered → test fails): DONE/NOT DONE
- `./gradlew :androidApp:assembleDebug`: PASS/FAIL
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64`: PASS/FAIL
- Device smoke test (B1 tabs/back, B3 screens): PASS / FAIL (details) / not run (reason)

## Noticed but not touched
- …
```

The Phase D section must also include:

```markdown
- Baseline `git status --porcelain` before starting: (paste)
- `git status --porcelain` after finishing: (paste). Every new line compared with the baseline must be explained by a task or listed under "Deviations".
- Files changed per task: D2: …, D3: …, D4: …, D5: …
- `:features:teleport:testAndroidHostTest`: N tests (must be 38)
- iOS compile errors still left after D2, if any: file:line — error (one per line)
- Each D3 dependency removal: removed / restored (reason)
```

## Review (Claude)

Claude reviews the working copy directly in the same checkout:

- reads `git diff` and the new or untracked files against this plan;
- re-runs `./gradlew testAndroidHostTest`, `:androidApp:assembleDebug` and `:composeApp:compileKotlinIosSimulatorArm64`;
- checks the report against the diff.

Problems go back to Gemini as a short list in chat or as an added task here. Repeat until the review passes.

## Human commit checklist (after the review passes)

1. Commit per task with the suggested messages, staging **by path** (`git add <files>`). Don't use `git add -A`, `git add .` or `git commit -a`.
2. Include `features/vault/.../vault/states/VaultState.kt` (for example, in its own commit: `Add missing VaultState to vault module`).
3. Don't commit `core/database/schemas/**` (that belongs to the vault PR; see the notes).
4. Afterwards, `git status --porcelain -- '*.kt' '*.kts' '*.toml'` should print nothing, so the commits build the same as the working copy did.
5. Push `review-audit` and open a PR to `Develop`. Put the report's "Checks" section in the PR description, and say that `review-fixes` overlaps (section 0.2).
