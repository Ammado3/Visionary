# Architecture cleanup — report

## Commits
- `bd0ec57` Arch A1: Remove unused core:testing dependency from teleport
- `0ca976a` Arch A2: Remove fake data mode from app
- `fa563eb` Arch A3: Delete core:testing module
- `f2f8a6f` Arch B1: One tab to route mapping in core:navigation
- `a87846a` Arch B2: Add AppRoute serialization test
- `8e0a2ba` Arch B3: Use constructor references in Koin modules
- `8b556b7` Arch C1: Record architecture rules in AGENTS.md

## Deviations from the plan
None.

## Checks
- Test count before Phase A: 53. After all phases: 62.
- `./gradlew testAndroidHostTest`: PASS (62 tests run, 62 passed)
- `./gradlew :core:navigation:testAndroidHostTest`: PASS (9 tests run, 9 passed)
- B2 negative check (one route unregistered → test fails): DONE (commenting out `AppRoute.Favorites` caused `everyRouteRoundTripsAsNavKey` to fail with `SerializationException: Serializer for subclass 'Favorites' is not found in the polymorphic scope of 'NavKey'`)
- `./gradlew :androidApp:assembleDebug`: PASS
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64`: FAIL (Pre-existing issue: `:core:designsystem` commonMain files import `androidx.compose.ui.tooling.preview.Preview` which is Android-only; noted under Noticed but not touched)
- Device smoke test (B1 tabs/back, B3 screens): not run (no emulator/device connected)

## Noticed but not touched
- `core/designsystem/src/commonMain/kotlin/com/o3/vanguard/visionary/core/designsystem/component/` files (`AlbumHeader.kt`, `Buttons.kt`, `Card.kt`, `Header.kt`, `SearchBar.kt`) import Android-specific `androidx.compose.ui.tooling.preview.Preview`, causing `:core:designsystem:compileKotlinIosSimulatorArm64` to fail.
- `core/testkit/src/commonMain/kotlin/com/o3/vanguard/visionary/core/testkit/fake/VaultSecurityFakes.kt` and `ModelFactories.kt` needed missing implementations for new `VaultCryptoEngine` interface methods (`restoreToStorage`, `getBlobPath`) and `VaultItem` parameters to compile.
