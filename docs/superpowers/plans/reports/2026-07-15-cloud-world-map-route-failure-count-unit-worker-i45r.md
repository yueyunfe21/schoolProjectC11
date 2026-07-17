# I45R Worker Report

- Status: `REPAIR_1_SOURCE_READY_PENDING_PARENT_REVIEW_AND_BUILD`
- Task: `W-COUNT-WORLD-MAP-ROUTE-FAILURE-1`
- Count unit: `WorldMapRouteResultMemoryService::recordFailure`
- Target count delta: `+1`
- Business baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- Workspace/transport reference: `0114604e`
- Java write set:
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\WorldMapRouteResultMemoryService.java`
  and
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\model\navigation\WorldMapRouteResultMode.java`.
- Fixed caller chain: `NavigationService:1723 -> MemoryService.recordWorldMapRouteResultFailure -> WorldMapRouteResultMemoryService.recordFailure -> route-mode/key failure/consecutive/disabled/clean persistence -> caller fallback continuation`.
- Worker boundary: implementation only; no reviewer judgment; no Maven, tests, runtime, server, host, task, poller, UI, capture, input, or Git mutation.

## Investigation

### Worker Result

- Status: `BLOCKED_OUT_OF_WRITESET_TYPED_PREREQUISITE`
- Count delta: `0`
- Java changes: none.
- Build/test/runtime: not run, as explicitly prohibited while External C and the other Java writers are active.
- Git mutation: none.

The requested active caller and baseline method both exist, and the target Service is already byte-identical to
the authoritative baseline. However, the active Cloud typed model required to compile that exact Service is
incomplete outside this Worker's unique write set. The count unit therefore cannot honestly close as `+1`.

### Baseline Evidence

- Authoritative source: DHXY commit `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.
- Baseline mirror:
  `D:\mavenProject\dhxy-cloud-brain\migration-baseline\696a12b0\src\main\java\com\bot\dhxy\service\WorldMapRouteResultMemoryService.java`.
- Active target:
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\WorldMapRouteResultMemoryService.java`.
- Byte comparison: `BYTE_EQUAL=true`, both `16443` bytes.
- SHA-256 for both files: `B14377869B473211242D5EFD7B2F56EBBA7A89A1A89B1CBC6EF86194293DDBEB`.
- Git blob for both files: `8de1e3347793d9da61ee7c42e9a56211eeb3a4ec`.

### Active Caller And Closed Fallback Evidence

1. `NavigationService.performWorldMapSearchAndClickDestination(...)` is active under
   `navigateToMap(...) -> submitWorldMapSearchAndClickDestination(...)`:
   - `NavigationService:431` calls `submitWorldMapSearchAndClickDestination(...)` from the active map-navigation path.
   - `NavigationService:1548` calls `performWorldMapSearchAndClickDestination(...)`.
   - `NavigationService:1383-1386` invokes the remembered yellow-destination fast path.
2. Both post-click failure branches reach the requested caller:
   - `NavigationService:1689-1694` records failure after an attempted yellow click whose ordered action failed,
     performs `memoryFailureCleanup`, then returns `WRONG_DESTINATION`.
   - `NavigationService:1698-1705` records failure when mini-map handoff is not confirmed, performs the same
     cleanup, then returns `WRONG_DESTINATION`.
3. `NavigationService:1723-1734` constructs a typed `WorldMapRouteResultPendingMemory` with
   `routeMode=YELLOW_DESTINATION_MINI_MAP`, route key facts, relative point, matched text, failure source,
   `usedMemory=true`, and delegates to `MemoryService.recordWorldMapRouteResultFailure(...)`.
4. `MemoryService:112-113` directly delegates to
   `WorldMapRouteResultMemoryService.recordFailure(...)`; `MemoryService` is an active Spring `@Service` with
   constructor-injected `WorldMapRouteResultMemoryService` (`MemoryService:19-28`).
5. Caller fallback continuation remains baseline-ordered:
   - `NavigationService:1428-1434` closes the mismatched route-search panel, waits `250ms`, and continues to
     attempt 2 for `WRONG_DESTINATION`.
   - On the terminal attempt, `NavigationService:1435-1436` closes the panel and returns `false`;
     `NavigationService:1549-1550` maps that to `NavigationResult.mapNotReached(...)`.

### Baseline `recordFailure` Semantics

The active target preserves `696a12b0` exactly:

- `recordFailure:173-175` resolves the pending route mode before building the route-mode-aware key.
- New key: failure count becomes `1`, consecutive success resets to `0`, consecutive failure becomes `1`,
  and `clean=false`; the incoming relative point/text/source are retained.
- Existing key: `toBuilder()` preserves prior fields including `disabled`, increments total and consecutive
  failures, resets consecutive success, and marks the entry dirty without adding a new disable/TTL/retry rule.
- Mutation order is unchanged at `recordFailure:205-210`: `memory.entries.put(key, next)` -> `save(memory)` ->
  warning log with route mode/key counters and pending identity diagnostics.
- `save(...)` retains the baseline temp-file write followed by atomic move with replace fallback.

### Exact Blocker

The active typed enum is outside the only permitted Java write set:

`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\model\navigation\WorldMapRouteResultMode.java`

It currently declares only:

```java
public enum WorldMapRouteResultMode {
    YELLOW_DESTINATION_MINI_MAP
}
```

The authoritative `696a12b0` enum declares both `LEGACY_GREEN_LINK` and
`YELLOW_DESTINATION_MINI_MAP`. The byte-exact target Service necessarily references
`WorldMapRouteResultMode.LEGACY_GREEN_LINK` at lines `57`, `102`, `298`, `305`, `310`, and `315`; active
`NavigationService` also references it at lines `1566`, `1582`, and `1590`. Java must resolve every enum
constant referenced by the class, including methods outside this one call branch, so the target Service cannot
compile against the current active typed model.

Repair requires restoring the baseline `LEGACY_GREEN_LINK` enum member in the model file, which is outside this
Worker's unique write set. This Worker did not modify that file, did not stub around it, and did not weaken the
exact baseline Service to remove the references.

### Business Difference

`无已批准业务差异；目标 Service 按 696a12b0 字节等价保留。` The blocker is a missing typed model member,
not an intentional change to persistence, failure counters, clean/disabled state, logging, or fallback order.

## Repair #1 Scope Amendment

- Status: `CLAIMED_SCOPE_AMENDMENT`
- Parent review: Parent Source Review #1 confirmed the prior `P1=1` scope blocker and expanded the same count unit in place.
- Count unit: `WorldMapRouteResultMemoryService::recordFailure`
- Target count delta: `+1`; the enum prerequisite is not counted separately.
- Expanded Java write set:
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\WorldMapRouteResultMemoryService.java`
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\model\navigation\WorldMapRouteResultMode.java`
- Repair boundary: restore only the authoritative `696a12b0` enum members and order. Do not modify the Service,
  `NavigationService`, or `MemoryService` facade.

## Repair #1

### Implementation Result

- Status: `SOURCE_READY_PENDING_PARENT_REVIEW_AND_BUILD`
- Count unit: `WorldMapRouteResultMemoryService::recordFailure`
- Requested count delta: `+1` for the complete count unit; the enum restoration is a non-counted prerequisite
  inside the amended scope.
- Java change: restored `WorldMapRouteResultMode` to the exact authoritative order:

```java
public enum WorldMapRouteResultMode {
    LEGACY_GREEN_LINK,
    YELLOW_DESTINATION_MINI_MAP
}
```

- Current enum Git blob: `6b10bc2d046d11d20d850554b938c4b70520e58e`.
- `696a12b0` enum Git blob: `6b10bc2d046d11d20d850554b938c4b70520e58e`.
- Current enum SHA-256: `12C7CA596A79EE6BDE221F01317D04B88269C46D40E3BCA79EBBF2021261C9A7`.
- No Service, facade, caller, persistence, TTL, retry, wrapper, or business-flow code was changed.

### Unchanged-File Evidence

The prohibited caller/Service files retain their pre-repair SHA-256 values:

- `WorldMapRouteResultMemoryService.java`:
  `B14377869B473211242D5EFD7B2F56EBBA7A89A1A89B1CBC6EF86194293DDBEB`; this remains byte-identical to
  the `696a12b0` Service and Git blob `8de1e3347793d9da61ee7c42e9a56211eeb3a4ec`.
- `NavigationService.java`:
  `66D5480722CF07C643BDABB9E53D84FFA203FD6184B8DFCAE6DEED313ED4AFF2`.
- `MemoryService.java`:
  `C227E353B597F67FCE6544DE1046559E8A233814132FAC1DC0115055DCBBB94B`.

### Active Caller To Persistence To Fallback

1. Active entry: `NavigationService:431` calls `submitWorldMapSearchAndClickDestination(...)`; line `1548`
   enters `performWorldMapSearchAndClickDestination(...)`, and lines `1383-1386` invoke the clean remembered
   yellow-destination path.
2. Failure production: after an attempted remembered yellow click, input/settle failure reaches lines
   `1689-1694`; unconfirmed handoff reaches lines `1698-1705`. Both call
   `recordYellowMemoryFastPathFailure(...)`, perform `memoryFailureCleanup`, and return
   `WRONG_DESTINATION`.
3. Typed caller: `NavigationService:1723-1734` builds `WorldMapRouteResultPendingMemory` with
   `YELLOW_DESTINATION_MINI_MAP`, route key facts, relative point, matched text, diagnostic failure source,
   `usedMemory=true`, and calls `MemoryService.recordWorldMapRouteResultFailure(...)`.
4. Facade to Service: `MemoryService:112-113` directly delegates to
   `WorldMapRouteResultMemoryService.recordFailure(...)` at line `172`.
5. Persistence semantics: the byte-exact Service resolves route mode/key, increments total and consecutive
   failure counts, resets consecutive success, sets `clean=false`, preserves existing `disabled` through
   `toBuilder()`, then performs `entries.put` -> `save` -> log at lines `205-210`. No TTL, disable threshold,
   retry, or additional read was introduced.
6. Closed caller continuation: `WRONG_DESTINATION` reaches `NavigationService:1428-1434`, which closes the
   mismatched route panel, waits the baseline `250ms`, and continues attempt 2. The terminal failure closes the
   panel at lines `1435-1436`; lines `1549-1550` return `NavigationResult.mapNotReached(...)`.
7. Restored typed closure: active enum lines `7-8` now supply both `LEGACY_GREEN_LINK` and
   `YELLOW_DESTINATION_MINI_MAP`, resolving the exact Service's legacy-default/key branches and the active
   Navigation caller's yellow failure branch without modifying either class.

### Verification Boundary

- Static source/blob/hash verification completed.
- Maven, tests, runtime/application/server/host, input, and Git mutation were not run, per the explicit shared-writer
  prohibition. Fresh build and final `+1` ledger decision remain with the parent after all Java writers stabilize.
- `无已批准业务差异；按 696a12b0 基线等价修复 typed prerequisite。`

## Parent Source Review #1 / Scope Amendment #1 - 2026-07-15T05:25:00-04:00

父级独立核验 active caller、`recordFailure` 字节等价与 enum 缺口，结论
**P0=0/P1=1/P2=0，BLOCKED_BY_SCOPE / countDelta=0**。`WorldMapRouteResultMode` 当前确实只含
`YELLOW_DESTINATION_MINI_MAP`，而 baseline 与 active Service 均需要 `LEGACY_GREEN_LINK`；删除引用或默认折叠会改变
route-mode key，禁止采用。

同一 `countUnit` 原地扩写集为：

- Cloud `service/WorldMapRouteResultMemoryService.java`；
- Cloud `model/navigation/WorldMapRouteResultMode.java`；
- 本报告。

Repair #1 仅把 enum 恢复为 baseline exact 两值顺序，再复核 active Navigation failure caller -> facade ->
recordFailure persistence -> retry/terminal continuation；不得改 Service/Navigation/Memory facade，不得拆成 enum 零计数单。
统一 fresh build 前仍 `countDelta=0`。

## Repair #1 Delivery After Scope Amendment

- Status: `REPAIR_1_SOURCE_READY_PENDING_PARENT_REVIEW_AND_BUILD`.
- Restored only `WorldMapRouteResultMode` to exact baseline order:
  `LEGACY_GREEN_LINK, YELLOW_DESTINATION_MINI_MAP`.
- Current/baseline enum blob identity: `6b10bc2d046d11d20d850554b938c4b70520e58e` =
  `6b10bc2d046d11d20d850554b938c4b70520e58e`.
- `WorldMapRouteResultMemoryService`, `NavigationService`, and `MemoryService` hashes remain unchanged from the
  pre-repair evidence above.
- Active closure rechecked:
  `NavigationService:1723-1734 -> MemoryService:112-113 -> WorldMapRouteResultMemoryService:172-210 ->`
  `NavigationService:1428-1436 / 1549-1550` retry-or-terminal fallback.
- No separate enum count was claimed. Requested count remains this one complete unit at `countDelta=+1`, pending
  parent source review and the deferred unified fresh build.
- Maven/test/runtime/input/Git were not run. No TTL, retry, wrapper, or business behavior was added.

## Parent Repair Source Review #2 - 2026-07-15T05:33:00-04:00

父级独立核验 enum current/baseline blob 均为 `6b10bc2d046d11d20d850554b938c4b70520e58e`，目标
Service 仍与 baseline blob `8de1e3347793d9da61ee7c42e9a56211eeb3a4ec` 一致；active
`NavigationService:1689-1734 -> MemoryService:112-113 -> recordFailure:172-210 -> retry/terminal fallback`
可达。enum 仅作为同一整链 typed prerequisite 恢复，没有单独计数。

结论：**P0=0/P1=0/P2=0，REPAIR SOURCE APPROVED / COUNT PENDING BUILD**。该去重 count unit 进入
待统一构建池；fresh Cloud package 前 ledger 仍 `189/407`。无已批准业务差异；按 `696a12b0` 等价迁移。
