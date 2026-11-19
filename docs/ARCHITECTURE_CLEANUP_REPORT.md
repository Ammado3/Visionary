# Architecture cleanup — report

## Commits (Phases A–C)
- `bd0ec57` Arch A1: Remove unused core:testing dependency from teleport
- `0ca976a` Arch A2: Remove fake data mode from app
- `fa563eb` Arch A3: Delete core:testing module
- `f2f8a6f` Arch B1: One tab to route mapping in core:navigation
- `a87846a` Arch B2: Add AppRoute serialization test
- `8e0a2ba` Arch B3: Use constructor references in Koin modules
- `8b556b7` Arch C1: Record architecture rules in AGENTS.md

## Deviations from the plan (Phases A–C)
- Commit `bd0ec57` (Task A1) included 4 extra files modified in the working copy: `core/database/schemas/AppDatabase/5.json`, `core/testkit/src/commonMain/.../ModelFactories.kt`, `core/testkit/src/commonMain/.../VaultSecurityFakes.kt`, and `features/vault/src/commonMain/.../VaultContract.kt`. `ModelFactories.kt` and `VaultSecurityFakes.kt` were needed to fix `:core:testkit` compilation due to updated domain interface signatures.

## Checks (Phases A–C)
- Test count before Phase A: 53. After Phase C: 62.
- `./gradlew testAndroidHostTest`: PASS (62 tests run, 62 passed)
- `./gradlew :core:navigation:testAndroidHostTest`: PASS (9 tests run, 9 passed)
- B2 negative check (one route unregistered → test fails): DONE (commenting out `AppRoute.Favorites` caused `everyRouteRoundTripsAsNavKey` to fail with `SerializationException: Serializer for subclass 'Favorites' is not found in the polymorphic scope of 'NavKey'`)
- `./gradlew :androidApp:assembleDebug`: PASS
- Device smoke test (B1 tabs/back, B3 screens): not run (no emulator/device connected)

## Noticed but not touched
- `features/vault/src/commonMain/kotlin/com/o3/vanguard/visionary/features/vault/vault/states/VaultState.kt` remained untracked in working copy for human commit checklist.

---

## Phase D — Follow-up after review

### Baseline `git status --porcelain` before starting Phase D:
```
?? docs/ARCHITECTURE_CLEANUP_PLAN.md
?? features/vault/src/commonMain/kotlin/com/o3/vanguard/visionary/features/vault/vault/states/VaultState.kt
?? scripts/
```

### `git status --porcelain` after finishing Phase D:
```
M AGENTS.md
 M androidApp/build.gradle.kts
 M build-logic/src/main/kotlin/KmpComposeConventionPlugin.kt
 M composeApp/build.gradle.kts
 M composeApp/src/commonMain/kotlin/com/o3/vanguard/visionary/App.kt
 M composeApp/src/commonMain/kotlin/com/o3/vanguard/visionary/screens/SplashScreen.kt
 M core/designsystem/build.gradle.kts
D  core/designsystem/src/androidMain/kotlin/com/o3/vanguard/visionary/core/designsystem/component/AppPermissionGate.android.kt
 M core/designsystem/src/commonMain/kotlin/com/o3/vanguard/visionary/core/designsystem/AppBottomNavigation.kt
 M core/designsystem/src/commonMain/kotlin/com/o3/vanguard/visionary/core/designsystem/component/Card.kt
 M core/navigation/build.gradle.kts
 M data/build.gradle.kts
 M docs/ARCHITECTURE_CLEANUP_REPORT.md
 M features/aisearch/src/commonMain/kotlin/com/o3/vanguard/visionary/features/search/SearchEntries.kt
 M features/aisearch/src/commonMain/kotlin/com/o3/vanguard/visionary/features/search/SearchScreen.kt
 M features/gallery/build.gradle.kts
 M features/gallery/src/commonMain/kotlin/com/o3/vanguard/visionary/features/gallery/navigation/GalleryNav.kt
 M features/gallery/src/commonMain/kotlin/com/o3/vanguard/visionary/features/gallery/presentation/albumgallery/AlbumGalleryScreen.kt
 M features/gallery/src/commonMain/kotlin/com/o3/vanguard/visionary/features/gallery/presentation/albums/AlbumsScreen.kt
 M features/gallery/src/commonMain/kotlin/com/o3/vanguard/visionary/features/gallery/presentation/photos/PhotoScreen.kt
 M features/gallery/src/commonMain/kotlin/com/o3/vanguard/visionary/features/gallery/presentation/photoviewer/PhotoViewerScreen.kt
 M features/onboarding/build.gradle.kts
 M features/onboarding/src/commonMain/kotlin/com/o3/vanguard/visionary/features/onboarding/OnboardingScreen.kt
 M features/settings/build.gradle.kts
 M features/teleport/build.gradle.kts
 M gradle/libs.versions.toml
 M platform/src/androidMain/kotlin/com/o3/vanguard/visionary/platform/android/AndroidPlatformAdapters.android.kt
?? docs/ARCHITECTURE_CLEANUP_PLAN.md
?? docs/archive/
?? features/vault/src/commonMain/kotlin/com/o3/vanguard/visionary/features/vault/vault/states/VaultState.kt
?? scripts/
```

### Files changed per task:
- **D1**: None (verified `VaultState.kt` exists and `:features:vault:compileAndroidMain` passes).
- **D2**: `gradle/libs.versions.toml`, `build-logic/src/main/kotlin/KmpComposeConventionPlugin.kt`, `core/designsystem/build.gradle.kts`, `features/onboarding/build.gradle.kts`, `features/settings/build.gradle.kts`, `core/designsystem/src/commonMain/.../AppBottomNavigation.kt`.
- **D3**: `core/navigation/build.gradle.kts`, `features/teleport/build.gradle.kts`, `composeApp/build.gradle.kts`, `features/settings/build.gradle.kts`, `features/gallery/build.gradle.kts`, `features/onboarding/build.gradle.kts`, `core/designsystem/build.gradle.kts`, `gradle/libs.versions.toml`, `androidApp/build.gradle.kts`, `data/build.gradle.kts`.
- **D4**: `core/designsystem/src/androidMain/.../component/AppPermissionGate.android.kt` (deleted), `features/aisearch/src/commonMain/.../SearchScreen.kt`, `features/aisearch/src/commonMain/.../SearchEntries.kt`, `features/gallery/src/commonMain/.../albums/AlbumsScreen.kt`, `features/gallery/src/commonMain/.../navigation/GalleryNav.kt`, `composeApp/src/commonMain/.../screens/SplashScreen.kt`, `composeApp/src/commonMain/.../App.kt`, `core/designsystem/src/commonMain/.../AppBottomNavigation.kt`, `core/designsystem/src/commonMain/.../component/Card.kt`, `features/gallery/src/commonMain/.../albumgallery/AlbumGalleryScreen.kt`, `features/gallery/src/commonMain/.../photos/PhotoScreen.kt`, `features/gallery/src/commonMain/.../photoviewer/PhotoViewerScreen.kt`, `features/onboarding/src/commonMain/.../OnboardingScreen.kt`, `platform/src/androidMain/.../android/AndroidPlatformAdapters.android.kt`, `platform/src/iosMain/.../ios/PhAssetResolver.kt`.
- **D5**: `AGENTS.md`.
- **D7**: Reverted earlier D2 edits in `platform/src/iosMain/kotlin/com/o3/vanguard/visionary/platform/ios/IosSecurityAndVaultAdapters.kt` and `platform/src/iosMain/kotlin/com/o3/vanguard/visionary/platform/ios/IosStubAdapters.ios.kt` to restore original committed content.

### Checks:
- `./gradlew :androidApp:assembleDebug`: PASS
- `./gradlew testAndroidHostTest`: PASS (62 tests run, 62 passed)
- `:features:teleport:testAndroidHostTest`: 38 tests run, 38 passed
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64`: FAIL (Fails only in the two vault adapters as expected; see exact errors below)
- iOS compile errors remaining after D7:
  - `IosSecurityAndVaultAdapters.kt:58:1 Class 'IosVaultCryptoEngine' is not abstract and does not implement abstract members: suspend fun restoreToStorage(itemId: String, mime: String? = ..., takenAtMs: Long = ...): AppResult<String>, fun getBlobPath(itemId: String): String`
  - `IosStubAdapters.ios.kt:45:1 Class 'IosStubVaultCryptoEngine' is not abstract and does not implement abstract member: suspend fun restoreToStorage(itemId: String, mime: String? = ..., takenAtMs: Long = ...): AppResult<String>`
  - `IosStubAdapters.ios.kt:57:5 'restoreToStorage' overrides nothing. Potential signatures for overriding: suspend fun restoreToStorage(itemId: String, mime: String? = ..., takenAtMs: Long = ...): AppResult<String>`
- Device smoke test: not run (no emulator/device connected)

### Task D3 Dependency Removals Status:
- `core/navigation/build.gradle.kts` (`commonTest` block): removed
- `features/teleport/build.gradle.kts` (`commonTest` & `androidUnitTest` blocks): removed
- `composeApp/build.gradle.kts` (`commonTest` block): removed
- `features/settings/build.gradle.kts` (`project(":core:designsystem")`): removed
- `features/gallery/build.gradle.kts` (`compose.multiplatform` plugin & version string): removed / updated
- `features/onboarding/build.gradle.kts` (`compose.components.resources` block): moved & removed bottom block
- `core/designsystem/build.gradle.kts` (`activity.compose` & `lifecycle.runtime.compose`): removed
- `core/designsystem/build.gradle.kts` + `libs.versions.toml` (`core-ktx` catalog alias): updated to catalog
- `gradle/libs.versions.toml` (`androidx-ui-tooling-preview` entry): removed
- `androidApp/build.gradle.kts` (`project(":data")`): removed
- `data/build.gradle.kts` (`libs.room3.runtime`): removed

### Deviations from the plan (Phase D)
- D2 edited the iOS vault adapters to compile; reverted in D7 because vault code is out of scope. iOS compile now fails only in those adapters, which the vault PR owns.
- Note: Deletion of `core/designsystem/src/androidMain/kotlin/com/o3/vanguard/visionary/core/designsystem/component/AppPermissionGate.android.kt` was staged during D4.
