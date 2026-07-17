# Cloud LeftTop Resolve-State Count Unit Worker I25

## CLAIMED

- task: `W-COUNT-LEFT-TOP-RESOLVE-STATE-1`
- worker: `Internal I25 implementation-only Worker`
- role: implementation only; not a reviewer
- countUnit: `LeftTopStatusSwitchService::resolveState`
- requested countDelta: `+1`
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- disposition: `NO_CODE_CHANGE_DELIVERED_FOR_PARENT_REVIEW`
- count gate: parent source review and the parent's fresh applicable Maven build must both pass before any ledger update

## Authority And Workspace State

- Read in full before disposition: `AGENTS.md`, `docs/DHXY_CONTEXT.md`, the top CR271 entries in
  `docs/ACTIVE_WORK.md`, `docs/业务逻辑.md`,
  `docs/superpowers/plans/2026-07-14-696a12b0-whole-service-first-migration.md`, and
  `docs/superpowers/specs/2026-07-12-service-migration-matrix.md`.
- Applicable business baseline: `docs/业务逻辑.md` sections `战斗中启动任务的逻辑` and
  `通用任务类热启动 Policy`, plus the explicit `696a12b0` whole-Service source baseline. This count unit does
  not change startup role selection, startup preparation, combat-maintenance cadence, capability gates, pending
  ownership, click order, retry/fallback, or stop behavior.
- CR271 hard ledger remains `189/407`. The top CR271 record requires parent source approval and unified fresh
  Maven before an approved unit is counted; this worker does not update that ledger.
- DHXY status at claim: branch `thin-client-design`, heavily dirty/untracked, including the local
  `LeftTopStatusSwitchService.java` and shared migration documents.
- Cloud status at claim: branch `navigation-migration`, heavily dirty/untracked, including the whole promoted
  `src/main/java/com/bot/**` tree.
- No existing dirty or untracked file was reverted, cleaned, staged, committed, moved, or overwritten.

## Baseline Resolve-State Contract

`migration-baseline/696a12b0/.../LeftTopStatusSwitchService.java:214-224` is the behavioral authority:

| Ordered branch | Exact condition | Result |
|---|---|---|
| open | `openScore >= 0.90 && openScore >= closedScore + 0.02` | `OPEN` |
| closed | open branch did not win, then `closedScore >= 0.90 && closedScore > openScore` | `CLOSED` |
| fallback | neither branch wins | `UNKNOWN` |

The active DHXY exact-window mechanics preserves those conditions verbatim at
`LeftTopStatusSwitchService.java:238-248`. Its `detect` path captures the window-relative ROI
`(8,147,11,19)`, scores OPEN and CLOSED templates from that same capture, calls `resolveState` exactly once, and
only retains the OPEN template center when the resolved state is `OPEN` (`:192-210`).

No new threshold, state, verification read, TTL, retry, fallback, cleanup, owner, session, or wrapper is needed.

## Complete Reachable Chain

### Startup caller

1. DHXY `DefaultWindowTaskStartupInitializer.java:99-108` is the real startup caller. Five-ring and member windows
   call `probeMemberStartup`; the remaining leader path calls `handleLeaderStartup`.
2. Both public entries use the existing supported-task gate and existing `checkAndMaybeClose`; no startup phase,
   role, or allow-click behavior is changed by this count unit.
3. DHXY `LeftTopStatusSwitchService.detect` performs one exact-window capture, scores the OPEN and CLOSED templates
   on the same frame, then calls the baseline-exact `resolveState`.
4. Leader startup uses `allowClick=true`: `OPEN` with a point reaches the existing atomic move/click path;
   `CLOSED` and `UNKNOWN` skip input. Member startup uses `allowClick=false`: `OPEN` marks the existing per-window
   pending flag, `CLOSED` clears it, and `UNKNOWN` leaves it unresolved without input.

### Maintenance caller and typed observation

1. Cloud `AutoCombatService.java:665-685` is the real sparse maintenance caller. Local-support members enter only
   while `TeamSupportCapability.LEFT_TOP_STATUS` is open; pending leader detection defers; the remaining eligible
   path calls `handleCombatMaintenance`.
2. Cloud `LeftTopStatusSwitchService.handleCombatMaintenance` preserves requested-task-first resolution and the
   three-task allowlist, then enters the already approved `checkAndMaybeClose` foundation.
3. `CloudLeftTopStatusPortAssembly.observe` requests `WindowFactKind.LEFT_TOP_STATUS` from the current run context.
   DHXY `LocalRemoteGameCommandHandler.java:820-823` binds the exact registered window with
   `WindowTaskContextHolder.callWith(...)` and calls `probeLeftTopStatusFact`.
4. `probeLeftTopStatusFact` runs the same DHXY `detect -> scoreTemplate(open/closed) -> resolveState` chain and emits
   one typed fact carrying finite same-frame scores; only `OPEN` carries the screen-absolute click point.
5. The assembly maps the closed typed fact to Cloud `OPEN/CLOSED/UNKNOWN/CAPTURE_FAILED`. Cloud preserves the
   state and scores; the already approved gate submits one ordered click bundle only for `OPEN` with a point.
6. `CLOSED`, `UNKNOWN`, `CAPTURE_FAILED`, and observation `NOT_EXECUTED` do not click. `STOPPED` is checked through
   `TaskCheckpoint`; unresolved transport and impossible terminals remain fatal instead of being converted into a
   visual miss. Input returns only the existing `EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN` closed outcomes.

## No-Code-Change Decision

The active chain already contains the exact `696a12b0` resolver at the approved local observation boundary. The
active Cloud Service SHA-256 is
`EAF02F735DA4E1E4B7C5B3CEE442B1A050AE3E00E9AD5910971688CE201F54E3`, matching the earlier parent-approved
LeftTop foundation. The active DHXY Service SHA-256 is
`6AC4CB59D82126BE606B519371F819166FFD9A1D3F063F477940D701354B977A`.

Adding another `resolveState` to the sole allowed Cloud Java file would make Cloud re-decide a state that the
closed typed observation already resolved with the same scores. That would create a second business decision and
could introduce disagreement between typed state and the OPEN-only click-point invariant. It is therefore not an
equivalence repair and was not made.

This delivery claims only the independently assigned `resolveState` count unit. It does not re-claim or modify the
already approved `checkAndMaybeClose`, `handleLeaderStartup`, `probeMemberStartup`, or
`consumeFollowerSafeWindow` units, and it does not claim the previously approved combat-maintenance unit.

## Changed Files

| File | Change |
|---|---|
| `docs/superpowers/plans/reports/2026-07-15-cloud-left-top-resolve-state-count-unit-worker-i25.md` | Added this implementation-worker evidence report. |
| `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java` | Inspected only; `NO_CODE_CHANGE`. |

No other file was changed by I25.

## Restrictions And Status

- No build, compile, package, test, runtime, application, server, Task, UI, capture, input, or poller was run.
- No Git mutation was run.
- Java changes: `0`.
- Worker status: `NO_CODE_CHANGE_DELIVERED_FOR_PARENT_REVIEW`.
- Requested accounting: `countDelta=+1`, but actual accounting remains unchanged until parent source review and
  fresh Maven pass. I25 does not mark this unit counted or complete.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## Parent Source Review #1 - 2026-07-15T03:55:00-04:00

父级独立核对 DHXY exact-window mechanics 的 `resolveState` 与 696：按顺序严格保持
`openScore>=0.90 && openScore>=closedScore+0.02 -> OPEN`、否则
`closedScore>=0.90 && closedScore>openScore -> CLOSED`、否则 `UNKNOWN`。Cloud maintenance caller 经
`LEFT_TOP_STATUS` typed observation 消费该唯一判定；startup caller 也直接复用同一 local resolver，未在 Cloud
复制第二个状态裁决。OPEN-only click point、CLOSED/UNKNOWN 零输入与 pending 语义均未漂移。结论：
**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD**。fresh 双构建前不记账；无已批准业务差异。
