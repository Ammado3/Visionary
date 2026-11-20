# Fix-plan status

**Next:** Human
**Action:** review and commit Step 11a (uncommitted in the working copy; see `REPORT_11a.md`). Then the reviewer:
1. reviews Step 03 as merged (`STEP_03_category_mapping.md`, `REPORT_03.md`) and writes `REVIEW_03.md`;
2. writes Step 11b (incremental sync, chunked deletions, batched writes, splash) from `PLAN.md`.

**Last reviewed commit:** `ac3f8ef` (Step 02b report)
**Plan version:** v2 (`PLAN.md`, written against `Develop` at `2dc811f`)

## History

| Step | Title | State |
|---|---|---|
| 01 | Stop silent data loss | Approved, kept in v2 |
| 02 | Persist settings (DataStore) | Approved, **removed in R0** (Develop persists settings in Room) |
| 02b | Use the saved onboarding flag at startup | Approved, kept (adapted to `AppRoute.Photos` in R0) |
| 03 | Category mapping | Merged in R0, awaiting review (collection counts removed; see REPORT_R0) |
| R0 | Merge Develop into review-fixes | Built: `testAndroidHostTest` and `assembleDebug` pass. iOS fails in vault adapters (vault owner) |
| 04 | Database migrations 3→4→5 | **Skipped by the human** for now. `DatabaseBuilder` keeps the destructive fallback from 1–4 |
| 05–08 | Vault security | **Skipped**: vault code is owned by a teammate |
| 11a | One indexing trigger, run guard, service fixes | Done, uncommitted, awaiting human review |
| 11b | Incremental sync, batched writes, splash | Next spec |

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
- Step 11a: launch check passed on Pixel 2 / Android 10 (service runs once and stops; no crash). Android 15 timeout not checked
