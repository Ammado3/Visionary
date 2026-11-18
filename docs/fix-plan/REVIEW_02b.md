# Review — Step 02b

**Verdict: Approved**

Reviewed `d853216..ac3f8ef` against `STEP_02b_use_onboarding_flag.md` and `REPORT_02b.md`.

## What I checked

- `./gradlew testAndroidHostTest assembleDebug`: **PASS** (run by the reviewer). From the XML test reports: 69 tests, 0 failures, 0 errors, 0 skipped. `StartRouteTest`: 4 tests, 0 failures. This matches the report.
- **Task 1:** `AppRoute.Onboarding` is now `data class Onboarding(val skipIntro: Boolean = false)` (`core/navigation/.../Navigation.kt:11`). The `AppNavSavedStateConfig` registration is unchanged and compiles. `OnboardingEntries.kt:19-20` seeds `showPermissionGate` from `key.skipIntro`. No other usages of the old object remain.
- **Task 2:** `composeApp/.../StartRoute.kt` matches the spec exactly. `App.kt` injects `ResolveStartRouteUseCase`, keeps the splash delay, and starts `rebuild()` only for `Home`. No new Koin binding was added.
- **Task 3:** `StartRouteTest` covers the 4 table rows with exact `assertEquals` on the route, including `skipIntro`. The "done + Denied → `Onboarding(skipIntro = true)`" test would fail against the old logic, so the tests do pin the new behaviour.
- **Checklist:** no hardcoded dispatchers, no logs, no `!!`, no `@Suppress`, no schema changes, no mocking. The scope is respected.
- **Report truthfulness:** the commits, file locations, test count and "device checks not run" all match what I found.

## Minor notes (no action needed)

- Commits `11e5edc` and `ac3f8ef` use the `Step 02b fix:` prefix, but no review had asked for fixes. Per `WORKFLOW.md`, housekeeping or report commits should use the next task number (e.g. `Step 02b.4: Add report`). The prefix `Step XX fix:` is only for fixes requested in a review.
- "Not done + Denied" isn't tested. The spec only asked for 4 cases, and the `when` order makes that case identical to "not done + Full".

## Deferred

- `OnboardingEntries` still calls `indexRepository.rebuild()` on `appScope`. This is the double indexing trigger handled in Step 11.
- Noted for Step 08: `RoomTrashRepository.observeTrashItems` uses `MediaCategory.valueOf(entity.category)` without `runCatching`. A bad value would crash the Bin flow. `toDomain()` already guards this.

## Device checks (human)

These are still outstanding for 02b and don't block Step 03 (not gated):
1. Fresh install → intro pages → finish → grant permission → Home.
2. Swipe away from Recents, relaunch → Home directly.
3. Revoke Photos/media permission in system Settings, relaunch → the **permission screen** opens, not the intro pages.
4. Grant access from that screen → Home.
