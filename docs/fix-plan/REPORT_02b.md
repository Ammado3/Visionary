# Report — Step 02b

## Commits
- `895fa62` Step 02b.0: Move STEP_02b_use_onboarding_flag.md to docs/fix-plan
- `feedb50` Step 02b.1: Let the onboarding route skip the intro
- `3c1cd3a` Step 02b.2: Choose start route from settings and permission
- `bbc725b` Step 02b.3: Add tests for chooseStartRoute

## Deviations from the spec
- `chooseStartRoute` was placed in `composeApp` at `composeApp/src/commonMain/kotlin/com/o3/vanguard/visionary/StartRoute.kt`.
- `StartRouteTest` was placed in `composeApp` at `composeApp/src/commonTest/kotlin/com/o3/vanguard/visionary/StartRouteTest.kt` as `composeApp` supports host tests.
- No other deviations from `docs/fix-plan/STEP_02b_use_onboarding_flag.md`.

## Checks
- `./gradlew testAndroidHostTest`: PASS — 69 tests run, 69 passed
- `./gradlew assembleDebug`: PASS
- Device checks:
  1. Fresh install → intro pages → finish → grant permission → Home: not run — needs device
  2. Swipe the app away from Recents, relaunch → Home directly: not run — needs device
  3. In system Settings → Apps → Visionary → Permissions, remove Photos/media access. Relaunch → permission screen: not run — needs device
  4. Grant access again from that screen → Home: not run — needs device

## Noticed but not touched
- None.
