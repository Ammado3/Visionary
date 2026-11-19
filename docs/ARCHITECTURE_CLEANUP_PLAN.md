# Architecture cleanup plan — open tasks only

Phases A–C (committed `bd0ec57`…`2df252d`) and Phase D tasks D1–D5 (in the working copy, reviewed) are done. Their full specs are in `docs/archive/ARCHITECTURE_CLEANUP_PLAN_phases_A-D.md`. **Don't read the archive unless a task below points you to it.** This file holds only the rules and the open tasks.

## How we work

Gemini and Claude run in the same Android Studio window on the same local checkout.
1. **Gemini** edits the working copy, **without committing**, and updates the report.
2. **Claude** reviews the local diff and re-runs the checks.
3. **The human** commits once the review passes.

Nothing is handed over through git.

## Rules (all tasks)

- Read `AGENTS.md` first. All warnings are errors; multiline declarations use trailing commas.
- **Never modify anything vault-related, anywhere, even if a build stays broken.** That covers:
  - `features/vault/**`;
  - vault adapters in `:platform` (`*VaultCryptoEngine*`, `IosSecurityAndVaultAdapters.kt`, the vault parts of `AndroidPlatformAdapters.android.kt`);
  - `VaultRepository` / `RoomVaultRepository` / `VaultItem` / `VaultCryptoEngine`;
  - vault fakes in `:core:testkit`;
  - `AppDatabase` schema files.

  Report vault-caused failures; don't fix them. (The only exception is Task D7, which *undoes* earlier vault edits.)
- **Keep the design system's public API that vault uses source-compatible:** `AppBottomNavigation`, `AppTab`, `VisionaryColors`, `VisionaryTheme`, `PrimaryButton`, `VisionaryHeader`, `Res.drawable.vault`. Deprecate; don't rename or remove.
- **Don't commit, stage or push.** No `git add/commit/reset/checkout/restore/stash/rebase/push`. Read-only git (`status`, `diff`, `log`) is fine. Don't create branches or worktrees.
- Change only files a task names. Don't edit or revert `core/database/schemas/**`.
- Before starting, paste `git status --porcelain` into the report as the baseline. When done, paste it again and list every changed file per task. Any file a task didn't name goes under "Deviations", with the reason. "Noticed but not touched" is only for things you did **not** change.
- Edit with file tools, not `sed`/`echo >`. Never claim a check you didn't run; write "not run" and why.
- When the open tasks are done, update `docs/ARCHITECTURE_CLEANUP_REPORT.md` and **stop**. Claude reviews next.

## Commands

```bash
./gradlew :androidApp:assembleDebug
./gradlew :composeApp:compileKotlinIosSimulatorArm64
./gradlew testAndroidHostTest            # currently 62 tests; teleport 38
```

---

## Open tasks

### Task D7 — Undo the iOS vault adapter edits

During D2, Gemini edited two vault adapters in `platform/src/iosMain/kotlin/com/o3/vanguard/visionary/platform/ios/` to make iOS compile. That's vault code, so the edits must be undone. (The added `IosVaultCryptoEngine.restoreToStorage` also returned a fake success. `RoomVaultRepository.restore()` would then delete the vault row and lose the photo on iOS.)

Restore both files to their committed content **by editing them**; `git checkout`/`restore` aren't allowed.
1. `IosSecurityAndVaultAdapters.kt`: in `class IosVaultCryptoEngine`, delete the added `override suspend fun restoreToStorage(itemId: String, mime: String?, takenAtMs: Long)` and `override fun getBlobPath(itemId: String)`, and the blank line after them. They sit just before `private suspend fun loadDataFromUri`.
2. `IosStubAdapters.ios.kt`: in `class IosStubVaultCryptoEngine`, put `restoreToStorage` back to:
   ```kotlin
       override suspend fun restoreToStorage(itemId: String): AppResult<String> =
           AppResult.Err(AppError.Unsupported("OS_NOT_SUPPORTED_YET"))
   ```
3. Check: `git diff -- platform/src/iosMain/kotlin/com/o3/vanguard/visionary/platform/ios/IosSecurityAndVaultAdapters.kt platform/src/iosMain/kotlin/com/o3/vanguard/visionary/platform/ios/IosStubAdapters.ios.kt` prints nothing.

**Expected:**
- `:composeApp:compileKotlinIosSimulatorArm64` **fails again**, but only in those two classes (for example "does not implement abstract member" or "'restoreToStorage' overrides nothing"). Copy the exact errors into the report. iOS errors in any other file are a real problem; report them.
- `:androidApp:assembleDebug` and `testAndroidHostTest` (62) still pass.
- In the report's Phase D "Deviations", replace the iOS stub line with: "D2 edited the iOS vault adapters to compile; reverted in D7 because vault code is out of scope. iOS compile now fails only in those adapters, which the vault PR owns."
- Also note in the report that the deletion of `core/designsystem/.../AppPermissionGate.android.kt` was staged during D4 (against the rules, but harmless; leave it staged).

### Task D6 — Device smoke test (human)

On an emulator or device, after `./gradlew :androidApp:installDebug`:
1. Open each screen once without a crash: Photos, Albums, one album, the viewer, Favorites, Trash, Search (type a query), Settings, the Teleport receiver, and the Vault tab (only checking that it opens).
2. Switch through all four tabs. Back from Albums, Search and Vault goes to Photos; back on Photos leaves the app.
3. The splash shows on launch and moves on by itself.
4. In Android Studio, the preview of `core/designsystem/.../component/Buttons.kt` still renders.

Record any crash (logcat `NoDefinitionFoundException` / `InstanceCreationException` / stack trace) in the report.

### Phase E — Design system

Not written yet. It's waiting on two decisions from the human: real dark mode now or not, and whether to delete or keep the unused components. The review findings are in the chat of 18–19 Nov 2026.

---

## Human commit checklist (after the review passes)

1. Commit per task, staging **by path**, using the per-task file lists in the report. No `git add -A` / `git add .` / `commit -a`. Suggested messages:
   - `Arch D2: Use multiplatform ui-tooling-preview so previews compile on iOS`
   - `Arch D3: Remove redundant and hard-coded dependencies`
   - `Arch D4: Remove dead code and unused imports`
   - `Arch D5: Fix module example in AGENTS.md`
   - `Arch D: Update cleanup report`
2. The untracked `features/vault/.../vault/states/VaultState.kt` is the vault compile fix. Commit it or not, according to your decision on vault (it's vault code).
3. Don't commit `core/database/schemas/**`.
4. Commit `docs/ARCHITECTURE_CLEANUP_PLAN.md` and `docs/archive/` too, so the history isn't lost.
5. Afterwards, `git status --porcelain -- '*.kt' '*.kts' '*.toml'` should be empty except deliberately uncommitted vault files. Then push `review-audit` and open a PR to `Develop`. Note in the PR that `origin/review-fixes` overlaps: `App.kt`, `Navigation.kt`, the DI modules and `AGENTS.md`.

## Notes for the vault PR (pass to the teammate)

- **iOS doesn't compile** until `IosVaultCryptoEngine` and `IosStubVaultCryptoEngine` implement the current `VaultCryptoEngine` (`restoreToStorage(itemId, mime, takenAtMs)`, `getBlobPath`). Until iOS restore really exists, `restoreToStorage` must return `AppResult.Err(...)`, **never** `Ok` with a placeholder. `RoomVaultRepository.restore()` deletes the vault row after `Ok`, which loses the photo.
- **Database version.** `fa7b050` added `vault_item.filePath` without leaving version 5, so any install that already has a version-5 database from before that commit crashes on launch. Bump to 6 with `Migration(5, 6)`: `ALTER TABLE vault_item ADD COLUMN filePath TEXT NOT NULL DEFAULT ''`, and commit `6.json`.
- Switch tabs with `navigator.navigateToTab(tab)` (`if (tab != AppTab.VAULT) …`) in `vault/VaultEntries.kt`, `valutscreen/VaultEntries.kt` and `biometricsetup/BiometricSetup.kt`.
- Keep one copy of the vault screens (`vault/` or `valutscreen/`), and remove the commented-out `VaultGridRoute`/`VaultGridScreen` in `vault/VaultScreen.kt`.
- `features/vault/build.gradle.kts`: drop `:core:designsystem` and `libs.composeUiToolingPreview`; the conventions add both.
- `BiometricModule.kt`: remove the unused `VaultViewModel` import, and rename `biometric` to `biometricModule`.
- New vault routes go into `AppRouteSerializersModule` and `AppRouteSerializationTest`.
- The design system's `VisionaryHeader` default `actions` are three no-op buttons. Keep passing `actions = null` until Phase E fixes the default.
