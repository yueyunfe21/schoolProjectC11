# W-COUNT-WORLD-MAP-ROUTE-ABANDONED-1 - Internal I47 Worker Report

## CLAIMED

- task: `W-COUNT-WORLD-MAP-ROUTE-ABANDONED-1`
- claimedAt: `2026-07-15T05:35:11-04:00`
- worker: `Internal implementation Worker I47` (not reviewer)
- countUnit: `WorldMapRouteResultMemoryService::recordAbandoned`
- countDelta: `+1`
- Java write set: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\WorldMapRouteResultMemoryService.java`
- Report write set: this file only.

## Implementation Result

- Final status: `NO_CODE_CHANGE / READY_FOR_PARENT_SOURCE_REVIEW`
- Requested count result: `countDelta=+1`, pending the parent manager/final reviewer and deferred unified fresh build.
- Java changes: none. The active Navigation caller, facade delegation, exact persistence mutation, and caller
  continuation are already complete, while the only writable Java file is byte-identical to the authoritative
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` mirror. Editing it would create baseline drift or meaningless churn.
- Worker boundary: implementation evidence only; this report is not reviewer approval.

## Baseline And Scope Gates

Read before closure:

- `D:\mavenProject\DHXY\AGENTS.md`
- `D:\mavenProject\DHXY\docs\DHXY_CONTEXT.md`
- the current CR271 head in `docs/ACTIVE_WORK.md`
- `docs/业务逻辑.md`, including the mandatory 五倍/修罗 baseline gate and the 修罗 failure baseline anchored at
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- `docs/superpowers/plans/2026-07-14-696a12b0-whole-service-first-migration.md`
- `docs/superpowers/specs/2026-07-12-service-migration-matrix.md`
- both required repository status snapshots: DHXY is on `thin-client-design`; Cloud is on
  `navigation-migration`; both are heavily dirty/shared, and I47 did not revert, overwrite, clean, stage, commit,
  checkout, or otherwise mutate either repository through Git

Baseline evidence:

- Active target and preserved baseline mirror are each `16443` bytes / `333` lines.
- Both SHA-256 values are
  `B14377869B473211242D5EFD7B2F56EBBA7A89A1A89B1CBC6EF86194293DDBEB`.
- Active `MemoryService.java` and its 696 mirror are also byte-identical, both SHA-256
  `C227E353B597F67FCE6544DE1046559E8A233814132FAC1DC0115055DCBBB94B`.
- The typed prerequisite is already baseline exact: active `WorldMapRouteResultMode` declares
  `LEGACY_GREEN_LINK` followed by `YELLOW_DESTINATION_MINI_MAP`. I47 did not edit the enum.

`无已批准业务差异；按 696a12b0 基线等价迁移。`

## Active Caller To Persistence To Continuation

1. **Runnable Navigation entry** - `NavigationService:216-234` exposes the production `navigateToNPC(...)`
   entry and calls `navigateToMap(...)`; `NavigationService:431` reaches
   `submitWorldMapSearchAndClickDestination(...)`. This active Navigation path does not depend on the blocked
   Wubei whole-Task caller.
2. **Pending production** - after successful world-map route submission, `NavigationService:1552-1567`
   registers the active pathing intent, calls `rememberPendingWorldMapRouteResultClick(...)` with the exact yellow
   or legacy route mode, and returns the existing `PATHING_STARTED` result.
3. **Second-navigation replacement** - `NavigationService:1760-1771` builds the next typed pending record with
   canonical route key facts, exact route mode, relative point, matched text, source, memory flag, and the active
   intent id. Lines `1772-1775` consume the previous pending and, only when its intent differs, call
   `MemoryService.recordWorldMapRouteResultAbandoned(previous, "second-navigation")`.
4. **Facade delegation** - `MemoryService:116-117` forwards the same pending object and reason directly to
   `WorldMapRouteResultMemoryService.recordAbandoned(...)`; it introduces no extra read, gate, retry, TTL, or
   wrapper policy.
5. **Exact mode/key mutation** - `recordAbandoned:219-227` resolves the pending route mode, derives the key from
   normalized `fromMap/targetMap/mode`, rejects only a blank key, then reads the exact existing entry. The route-key
   rule at lines `291-301` remains `from->target` for legacy and `MODE|from->target` for non-legacy modes.
6. **Metadata-only settlement** - for a new key, lines `228-238` retain route metadata and set only
   `lastAbandonedAt/source` beyond builder defaults; for an existing key, lines `239-243` use `toBuilder()` and
   update only normalized route mode, `lastAbandonedAt`, and source. Total/consecutive success and failure counts,
   `clean`, `disabled`, last success, and last failure are unchanged.
7. **Required order** - lines `244-249` preserve `entries.put(key, next) -> save(memory) -> info log`. The log
   normalizes and records `reason` only after persistence, together with the resulting route mode, key metadata,
   unchanged counters, pending identity, and source. `save(...)` keeps the baseline temp write, atomic replace, and
   replace fallback.
8. **Caller continues** - `NavigationService:1776-1780` installs the next pending after abandoning the previous
   one and logs the new pending. No branch changes the caller's existing `PATHING_STARTED` continuation.

## Wubei Read-Only Cross-Check

- `WubeiTask:3476-3483` contains a second source caller: it consumes a pending route memory, records abandoned with
  the supplied clear reason, and then clears the pathing signal.
- This is supporting parity evidence only. CR271 records that the copied Wubei/Xiuluo whole Tasks still lack
  runnable typed task-runtime/turn/ready-event and permanent-local mechanics boundaries. I47 does not use this
  blocked whole-Task caller as the sole or required production reachability proof and does not claim its execute
  unit.

## Count Boundary

- Included once: active second-navigation replacement -> facade -> `recordAbandoned` exact mode/key metadata-only
  persistence -> installation of the next pending and normal caller continuation.
- Excluded: world-map input/OCR/click mechanics, `recordSuccess`, `recordFailure`, `findClean`, MemoryService facade
  as a separate unit, Wubei execute, either route mode as a second count, and any build/ledger increment.
- No owner/session, TTL, retry, cleanup, wrapper, enum, counter, save, log, or business-flow change was added.

## Changed Files And Verification Boundary

- Java: none.
- Report: `docs/superpowers/plans/reports/2026-07-15-cloud-world-map-route-abandoned-count-unit-worker-i47.md`.
- Static source, line-order, byte-size, and SHA-256 comparisons completed.
- Maven, tests, runtime/application/server/host, task/poller/UI, capture/OCR/input, and further Git commands were
  not run, as explicitly prohibited while C/I46 and other Java writers remain active.
- Fresh Cloud package, parent source review, final severity judgment, and ledger update remain exclusively with the
  parent manager/final reviewer.

## Parent Source Review #1 - 2026-07-15T05:39:00-04:00

父级独立复核 active `NavigationService:1760-1780`、`MemoryService:116-117` 与
`WorldMapRouteResultMemoryService:219-249,291-301`：第二次导航会先消费旧 pending，仅在 intent 不同时以
`second-navigation` 调用 `recordAbandoned`，随后安装新 pending 并继续原调用链。目标 Service active blob
`8de1e3347793d9da61ee7c42e9a56211eeb3a4ec` 与 DHXY `696a12b0` 同路径 blob 完全一致；route-mode/key、
metadata-only mutation、`put -> save -> log` 顺序和计数器不变。

结论：**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD**。该唯一 count unit 进入去重待构建池，
池 `52 -> 53`；fresh Cloud package 前 hard ledger 仍 `189/407`。Wubei caller 仅为旁证，不作为 active
reachability 权威；本次批准只计算 runnable Navigation second-navigation 整链一次。
