# Fix-plan status

**Next:** Reviewer
**Action:** Step R0 (the merge) is committed but **not yet built**. Read `REPORT_R0.md`, then:
1. run `./gradlew testAndroidHostTest assembleDebug` and an iOS compile, and fix only merge-caused errors (as `Sync fix:` commits);
2. review Step 03 as merged (`STEP_03_category_mapping.md`, `REPORT_03.md`) and write `REVIEW_03.md`;
3. write the Step 04 spec (database migrations 3→4→5, see `PLAN.md`) and hand it to the Implementer.

**Last reviewed commit:** `ac3f8ef` (Step 02b report)
**Plan version:** v2 (`PLAN.md`, written against `Develop` at `2dc811f`)

## History

| Step | Title | State |
|---|---|---|
| 01 | Stop silent data loss | Approved, kept in v2 |
| 02 | Persist settings (DataStore) | Approved, **removed in R0** (Develop persists settings in Room) |
| 02b | Use the saved onboarding flag at startup | Approved, kept (adapted to `AppRoute.Photos` in R0) |
| 03 | Category mapping | Merged in R0, awaiting review (collection counts removed; see REPORT_R0) |
| R0 | Merge Develop into review-fixes | Committed, awaiting build check |
| 04 | Database migrations 3→4→5 | Next spec |

## Phases

| Phase | Steps | Ends with |
|---|---|---|
| A — Reconcile and protect data | R0, 04 | Human merges `review-fixes` → `Develop` |
| B — Vault security (gated) | 05, 06, 07, 08 | Human merge |
| C — Correctness | 09, 10 | Human merge |
| D — Performance and indexing | 11, 12 | Human merge |
| E — Cleanup, privacy, tooling | 13–17 | Human merge |

## Human sign-off

Record device-check results here, one line per step, e.g. `Step 05: device check passed`.

- Step 01: device checks not yet confirmed (delete from Search; cancel delete dialog; fresh install launches)
- Step 02b: device checks not yet confirmed (see REVIEW_02b.md)
- Step 04: will need an upgrade test from an old install (see PLAN.md, Step 04)
