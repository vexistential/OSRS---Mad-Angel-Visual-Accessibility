# In-game verify matrix (leftover circle / under-boss)

Watch client logs for `ARM`, `SKIP_BOUNCE`, `CLEAR_PENDING`, `TRACK`, `FALLBACK_ON`/`OFF`, `DROP_SHADOW`.

| Case | Setup | Pass criteria | Log pattern |
|------|--------|---------------|-------------|
| A1 | Stand hop far from boss; watch after shadow fades | No leftover circle | `TRACK` then `DROP_SHADOW`; **no** `FALLBACK_ON` after drop |
| A2 | Stand→boss flight | No circle on old stand during boss hop | `SKIP_BOUNCE` + `CLEAR_PENDING cause=bounce` |
| A3 | Last hop; wait 8+ ticks after ball gone | No residual circle | `CLEAR_PENDING cause=ttl`; no lingering `FALLBACK_ON` |
| B1 | Stand dest **inside** boss footprint | Outline appears | `ARM` (not `SKIP_BOUNCE reason=footprint` alone on first under-boss stand) |
| B2 | Adjacent tile **outside** footprint | Outline appears | `ARM` + `TRACK` or `FALLBACK_ON` then `TRACK` |
| B3 | Far tile under footprint if possible | Same as B1 | Same as B1 |

True boss bounce-backs should still log `SKIP_BOUNCE reason=targetActor`.

| Case | Setup | Pass criteria |
|------|--------|---------------|
| A4 | Adjacent stand hop with angel close to both tiles | Full outline on **new** tile only; no small circle on old tile; logs show `CLEAR_PENDING cause=retarget` then `ARM` on new scene XY |
