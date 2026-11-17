# Step 02b — Use the saved onboarding flag at startup

## Context

Read `AGENTS.md` first. This is a small follow-up to Step 02 (DataStore settings), which is merged on `review-fixes`.

**Problem.** Step 02 made `onboardingDone` persist, and `OnboardingViewModel` writes it when the user finishes the intro pages. But nothing reads it at startup. `composeApp/src/commonMain/kotlin/com/o3/vanguard/visionary/App.kt` picks the first screen from the **media permission only**:
- permission granted → `Home`
- permission not granted → `Onboarding` (the full intro pages again), even if the user already finished onboarding.

`ResolveStartRouteUseCase` in `:domain` already reads `onboardingDone`, and it has tests, but no code calls it.

**Goal.** Start-up should be:

| `onboardingDone` | media access | first screen |
|---|---|---|
| false | any | Onboarding intro pages |
| true | Full or Partial | Home (and start indexing, as today) |
| true | Denied | Onboarding, **skipping the intro pages**, straight to the permission gate |

### Branch and commits

- Work on `review-fixes`: `git checkout review-fixes && git pull`.
- One commit per task, prefixed `Step 02b.N: …`.
- Move this file into `docs/fix-plan/` in the first commit.
- Push when done. Don't merge into `Develop`.

---

## Task 1 — Let the onboarding route skip the intro

1. In `core/navigation/.../AppRoute`, change
   ```kotlin
   @Serializable data object Onboarding : AppRoute
   ```
   to
   ```kotlin
   @Serializable data class Onboarding(val skipIntro: Boolean = false) : AppRoute
   ```
   The registration in `AppNavSavedStateConfig` (`subclass(AppRoute.Onboarding::class, AppRoute.Onboarding.serializer())`) stays valid; just make sure it still compiles.
2. Fix every usage of `AppRoute.Onboarding` (it is now `AppRoute.Onboarding()`).
3. In `features/onboarding/.../OnboardingEntries.kt`, use the route's key to set the initial value of `showPermissionGate`:
   ```kotlin
   entry<AppRoute.Onboarding> { key ->
       var showPermissionGate by remember { mutableStateOf(key.skipIntro) }
       …
   ```
   Nothing else in the entry changes.

## Task 2 — Choose the start route from settings and permission

1. In `composeApp`, add a small pure function (same package as `App.kt`, its own file `StartRoute.kt`):
   ```kotlin
   internal fun chooseStartRoute(destination: StartDestination, access: MediaAccess): AppRoute =
       when {
           destination == StartDestination.ONBOARDING -> AppRoute.Onboarding()
           access is MediaAccess.Full || access is MediaAccess.Partial -> AppRoute.Home
           else -> AppRoute.Onboarding(skipIntro = true)
       }
   ```
2. In `App()`'s `LaunchedEffect`, inject `ResolveStartRouteUseCase` with `koinInject()`, then:
   ```kotlin
   val access = permissionGateway.requestAccess()
   val route = chooseStartRoute(resolveStartRoute(), access)
   if (route == AppRoute.Home) launch { indexRepository.rebuild() }
   startRoute = route
   ```
   Keep the existing splash delay and the existing `rebuild()` call exactly as they are otherwise; both are handled in later steps.
3. `ResolveStartRouteUseCase` is already registered in `domainModule`; don't add a new binding.

## Task 3 — Tests

Add `composeApp/src/commonTest/kotlin/com/o3/vanguard/visionary/StartRouteTest.kt` with one test per table row (4 tests: not done + Full, done + Full, done + Partial, done + Denied).

If `composeApp` has no working host-test setup, don't change build logic for it. Instead, move `chooseStartRoute` to a module that already runs host tests and can see both `AppRoute` and `MediaAccess`, and say where in your report. If no such module exists without adding a dependency, keep the function in `composeApp`, skip the test, and say so.

---

## Out of scope (do not touch)

- The fixed 3-second splash delay.
- Indexing being started twice (once in `App.kt`, once in `MainActivity`). A later step fixes that.
- `:features:onboarding` importing from `:platform` (`AppPermissionGate`). Noted for the architecture cleanup step.

## Verify before reporting

```bash
./gradlew testAndroidHostTest
./gradlew assembleDebug
```

Manual checks **on a device or emulator**. These are the important part; please do them on the device, not as unit tests:
1. Fresh install → intro pages → finish → grant permission → Home.
2. Swipe the app away from Recents, relaunch → Home directly.
3. In system Settings → Apps → Visionary → Permissions, remove Photos/media access. Relaunch → it should open on the **permission screen**, not the intro pages.
4. Grant access again from that screen → Home.

## Report back

Push `review-fixes`, then reply with:
1. Commit hashes.
2. Where `chooseStartRoute` and its test ended up, and how many tests ran and passed.
3. The result of each of the 4 manual checks, done on a device.
4. Anything you couldn't do as written, and why.
