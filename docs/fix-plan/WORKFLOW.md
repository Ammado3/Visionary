# Fix-plan workflow

Two agents work through `PLAN.md` one step at a time on the `review-fixes` branch, with a human in charge.

| Role | Who | Does | Never does |
|---|---|---|---|
| **Implementer** | Gemini agent | Implements one step spec, writes a report | Writes specs, reviews its own work, merges |
| **Reviewer** | Claude agent | Reviews the implementer's commits, runs the checks, writes the next spec | Edits production or test code, merges |
| **Human** | Project owner | Starts each agent, does device checks, approves gated steps, merges to `Develop` | — |

The repo is the handoff. Both agents start every session the same way:

> Read `docs/fix-plan/STATUS.md` and do your part.

`STATUS.md` says whose turn it is. If it isn't your turn, say so and stop.

---

## Ground rules (both agents)

- Read `AGENTS.md` (repo root) before doing anything else. Its rules apply to every step.
- Branch: `review-fixes` only. Always `git checkout review-fixes && git pull` first, and `git push` at the end. If the push fails, say so plainly; never claim a push that didn't happen.
- Never merge into `Develop` and never force-push.
- Never claim a check you did not run. If you couldn't run something (no device, no emulator), write "not run" and why.
- All fix-plan files live in `docs/fix-plan/`.

### Keeping in step with `Develop`

The human keeps building features on `Develop` while this plan runs. To stop the branches drifting apart again:

- **Reviewer, at the start of every review turn:** `git fetch origin` and check `git log --oneline review-fixes..origin/Develop`. If `Develop` has new commits, merge it into `review-fixes` **before** reviewing (`git merge origin/Develop`). Resolve conflicts by keeping both sides' intent, run `./gradlew testAndroidHostTest assembleDebug`, and commit as `Sync: merge Develop into review-fixes`. If a conflict needs a product decision, stop and set `Next: Human`.
- **Before writing a spec:** re-read the current code it touches on `review-fixes` (after the sync), not the code `PLAN.md` describes.
- **Human:** merge `review-fixes` into `Develop` (through a PR) after each phase in `PLAN.md`, not only at the end. Small, frequent merges keep conflicts small.

---

## Implementer (Gemini)

When `STATUS.md` says `Next: Implementer`:

1. Open the step file it names (e.g. `STEP_03_category_mapping.md`), or the review it names if the step was sent back.
2. Implement **only** that step. Anything outside its scope goes in your report under "Noticed but not touched".
3. One commit per task, message prefix `Step XX.N: …` (e.g. `Step 03.2: Map search filters to categories`). Fix-up commits after a review: `Step XX fix: …`.
4. Run the "Verify before reporting" commands from the spec. Device checks you can't do: write "not run — needs device".
5. Write `docs/fix-plan/REPORT_XX.md` (template below) and commit it.
6. Update `STATUS.md`: `Next: Reviewer`, `Action: review Step XX`. Commit, push.

### REPORT_XX.md template

```markdown
# Report — Step XX

## Commits
- `<hash>` Step XX.1: …

## Deviations from the spec
None / list with reasons.

## Checks
- `./gradlew testAndroidHostTest`: PASS/FAIL — N tests run, N passed (from the test report, not the Gradle task count)
- `./gradlew assembleDebug`: PASS/FAIL
- Device checks: one line each — PASS / FAIL / not run (reason)

## Noticed but not touched
- …
```

---

## Reviewer (Claude)

When `STATUS.md` says `Next: Reviewer`:

1. Pull, then read the step spec, `REPORT_XX.md`, and the diff: `git log --oneline` and `git diff <last reviewed commit>..HEAD`. The last reviewed commit is in `STATUS.md`.
2. **Re-run the checks yourself.** Don't trust the report:
   ```bash
   ./gradlew testAndroidHostTest
   ./gradlew assembleDebug
   ```
   Confirm the new tests exist and actually assert the behaviour (not tests that would pass with the old code).
3. Review against the checklist below.
4. Write `docs/fix-plan/REVIEW_XX.md`: verdict (**Approved** or **Changes needed**), what you checked, each problem with file and line, and anything deferred.
5. Then:
   - **Changes needed** → `STATUS.md`: `Next: Implementer`, `Action: fix issues in REVIEW_XX.md`.
   - **Approved, next step not gated** → write the next step spec from `PLAN.md` (template below), set `Next: Implementer`, `Action: implement STEP_YY_…`.
   - **Approved, but this step or the next is gated** (see `PLAN.md`) → set `Next: Human`, `Action:` the exact device checks the human must do, and what to write back. Write the next spec only after the human records the result in `STATUS.md`.
6. Update `Last reviewed commit` in `STATUS.md`. Commit only files in `docs/fix-plan/`. Push.

The reviewer may edit `AGENTS.md` only when a step spec says so.

**One exception to "reviewer never edits code":** merging `Develop` into `review-fixes` (the `Sync:` commits, and Step R0). Conflict resolution needs the review context, so the reviewer does it. It must not add new behaviour while resolving conflicts. Anything beyond a merge goes into a spec for the Implementer.

### Review checklist

- Does every task in the spec exist, and nothing outside its scope?
- Correctness: edge cases, error paths, null and empty input, cancellation (`CancellationException` must never be swallowed).
- No new hardcoded dispatchers (`Dispatchers.IO` etc.). Use `AppDispatchers`.
- No URIs, file paths, OCR text or other user content in logs.
- No `!!`, no `GlobalScope`, no new `@Suppress` without a reason in the report.
- Database: no destructive fallback. Any schema change has a `Migration`, a bumped version, and a committed schema JSON.
- Tests use handwritten fakes from `:core:testkit`. No mocking libraries.
- Commit messages follow the `Step XX.N:` format.
- The report is truthful: compare its claims with the diff and your own check results.

### Step spec template

Model new specs on the existing `STEP_01` and `STEP_02` files. Every spec has:

```markdown
# Step XX — <title>

## Context
Read AGENTS.md first. One paragraph on the problem, with exact file paths and what the code does today.

### Branch and commits
review-fixes; one commit per task, prefix `Step XX.N:`; push; don't merge.

## Task 1 — <title>
**Problem.** … **Change.** numbered, concrete, naming files, classes and functions. **Acceptance.** …

## Task N — Tests
Which test file, which fakes, which cases. Say what to do if something can't be host-tested.

## Out of scope (do not touch)
## Verify before reporting
Commands + device checks, written as steps a person can follow.
## Report back
Write REPORT_XX.md per WORKFLOW.md and update STATUS.md.
```

Rules for specs:
- Keep each step small: 2–5 tasks, one area. If a `PLAN.md` step is big, split it (`STEP_04a`, `STEP_04b`) and say so in `STATUS.md`.
- Before writing a spec, read the current code it touches. `PLAN.md` was written against `Develop` at `67767e3`, and earlier steps may have changed things.
- Prefer exact instructions (names, signatures, keys) over goals. Mark anything the implementer must verify (library versions, API signatures).
- Every behaviour change gets a test, unless it truly can't be host-tested. Then say how to check it on a device.

---

## Human

- When `STATUS.md` says `Next: Human`, do the listed device checks, then edit `STATUS.md`. Under `Human sign-off`, add one line such as `Step 04: device check passed` or `Step 04: FAILED — <what happened>`. Set `Next: Reviewer` and push.
- Merging `review-fixes` into `Develop` is always your decision.
