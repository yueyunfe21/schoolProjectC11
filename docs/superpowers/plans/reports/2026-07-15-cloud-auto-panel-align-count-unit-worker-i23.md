# W-COUNT-AUTO-PANEL-ALIGN-1 - Internal I23 Implementation Report

`CLAIMED | task=W-COUNT-AUTO-PANEL-ALIGN-1 | claimedAt=2026-07-15T03:50:13-04:00 | countUnit=AutoCombatPanelService::alignPanelIfNeeded | requestedCountDelta=+1 | writeSet=[Cloud AutoCombatPanelService.java; this report]`

## Status

- Role: Internal I23 implementation-only Worker; not reviewer.
- Delivery: `IMPLEMENTED / PARENT SOURCE REVIEW PENDING / FRESH MAVEN PENDING`.
- Accounting now: `countDelta=0`. Do not book `+1` until parent review and the parent-run fresh Maven gate both pass.
- Business difference: `无已批准业务差异；按 696a12b0 基线等价迁移`.

## Baseline And Workspace Read

- Read complete `AGENTS.md` and `docs/DHXY_CONTEXT.md`.
- Read the top CR271 entry in `docs/ACTIVE_WORK.md` (03:42): I23 owns this count unit; Java writers must stabilize before Maven.
- Read the applicable baseline gate in `docs/业务逻辑.md`: migration may not add TTL, retry, extra verification, park/yield, cleanup, or change action/verification order. This file has no AutoCombatPanel-specific business row, so the method authority is the selected pre-cloud source `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.
- Read the complete `2026-07-14-696a12b0-whole-service-first-migration.md` plan and the complete migration matrix. Matrix rule: panel center farther than 20px from the target is the only drag condition.
- DHXY status baseline: branch `thin-client-design`, already heavily dirty/untracked before this task.
- Cloud status baseline: branch `navigation-migration`, already heavily dirty/untracked; `src/main/java/com/bot/**` (including the target) was untracked before this task. No pre-existing file was reverted or cleaned.

## Closed Caller-To-Terminal Evidence

1. Real callers: Cloud `AutoCombatService.java:657,696-697,727-728` call `verifyAndAlignPanel(...)` in entry-maintenance and verify-refresh paths.
2. Public Service flow: Cloud `AutoCombatPanelService.java:88-102` obtains the typed visible panel match, calls `alignPanelIfNeeded`, and then preserves the existing rounds branch unchanged.
3. Panel observation: the existing `AUTO_COMBAT_PANEL` fact remains read-only and carries a FOUND panel center in `SCREEN_ABSOLUTE_PX`; DHXY `LocalRemoteGameCommandHandler.java:824-826` routes it to `probeAutoCombatPanelFact`, whose existing capture/template path returns the match.
4. Geometry: `alignPanelIfNeeded` now reads the existing typed `GEOMETRY` fact at Cloud `AutoCombatPanelService.java:275-313`; DHXY handler `LocalRemoteGameCommandHandler.java:800-811` supplies the exact bound-window screen origin. Target remains `origin + (489,726)`.
5. Decision: Cloud `AutoCombatPanelService.java:315` preserves the exact baseline condition `panelPoint.distance(dropX, dropY) > 20.0`. The `<=20.0` branch skips input and takes no post-drag panel observation.
6. Typed drag: Cloud `AutoCombatPanelService.java:318-346` submits one ordered `SCREEN_ABSOLUTE_PX` bundle: `DRAG_AND_DROP(from,to)` then `SLEEP(500)`. DHXY's existing input-bundle handler validates both points against the exact binding and maps `DRAG_AND_DROP` to the single input queue.
7. Closed result consumer: Cloud `AutoCombatPanelService.java:348-395` consumes exactly one post-drag `AUTO_COMBAT_PANEL` fact. FOUND replaces the match; every ordinary miss/unavailable result preserves the exact baseline `drag-target-fallback` match at the drop point. STOPPED/UNKNOWN/wrong terminal types unwind and are never converted into business success.
8. Final state: Cloud `AutoCombatPanelService.java:400-401` preserves `panelAligned=true` and returns the resolved/fallback match.
9. Existing `CloudGameClient.readWindowFact` and `executeInputBundle` final-consume non-UNKNOWN outcomes (`CloudGameClient.java:40-53,98-121`); no owner/session/TTL/retry/wrapper was added.

## 696 Equivalence Check

- Baseline source: Cloud evidence mirror `migration-baseline/696a12b0/.../AutoCombatPanelService.java:133-155`.
- Preserved: panel-center drag origin; target offsets `(489,726)`; strict `>20.0`; ordered drag then 500ms sleep; exactly one post-drag observation; successful refreshed match; `drag-target-fallback` point/marker/width/source; no-drag log path; `panelAligned=true`; returned match.
- Typed-boundary-only substitution: baseline tracker base read -> existing `GEOMETRY` fact; baseline local input sequence -> existing ordered `InputBundle`; baseline local `findAutoCombatBox` recheck -> existing `AUTO_COMBAT_PANEL` fact.
- Intentional business differences: none.

## Changed Files

- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`
  - Only `alignPanelIfNeeded` changed.
  - Current SHA-256: `818B2135B0A4749EE981B51BAD409CADD98DD80C655B8CEEE6C5061D0A1EB1E7`.
- `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-cloud-auto-panel-align-count-unit-worker-i23.md`
  - This report only.

## Prohibited Actions Honored

- No build, test, runtime, Task/UI start, capture, physical input, or Git mutation was run.
- No protocol/DTO/codec/digest/handler/DHXY mechanics file was edited.
- No owner, session, TTL, retry, wrapper, or extra verification loop was added.

## Parent Source Review #1 - 2026-07-15T03:55:00-04:00

父级逐行对照 active `AutoCombatPanelService.java:270-401` 与
`696a12b0:AutoCombatPanelService.alignPanelIfNeeded`。typed substitution 只把 tracker base 换为 exact-context
`GEOMETRY` fact、把 local queue 换为同一 ordered `DRAG_AND_DROP + SLEEP(500)` bundle、把一次本地复查换为
一次 `AUTO_COMBAT_PANEL` fact；`>20.0`、目标 offset、普通 input miss 后继续单次复查、FOUND 替换、其它普通
miss 使用 `drag-target-fallback`、no-drag 与 `panelAligned=true` 顺序均保持。STOPPED/UNKNOWN 没有被压成业务
成功。结论：**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD**。fresh Cloud package 前
不记账；无已批准业务差异。
