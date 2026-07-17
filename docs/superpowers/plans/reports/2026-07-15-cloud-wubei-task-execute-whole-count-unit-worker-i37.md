# I37 - WubeiTask Whole Execute Count Unit

## Claim

- task: `W-COUNT-WUBEI-TASK-EXECUTE-WHOLE-1`
- countUnit: `WubeiTask::execute(TaskExecutionContext)`
- requested countDelta: `+1`
- worker role: implementation only; not reviewer
- unique Java write set: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\wubei\WubeiTask.java`
- report: this file
- business baseline: DHXY `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`

## Delivery

- status: `BLOCKED`
- effective countDelta: `0`
- source disposition: `FULL_BASELINE_SOURCE_PRESERVED`
- Java changes: one new Cloud file, no other Java files touched
- build/test/runtime/input: not run, as explicitly required

The complete baseline class was mechanically promoted before compatibility analysis. No public or
private method, branch, ordering decision, delay, fallback, state field, nested type, or comment was
removed. The active Cloud file is 4,319 lines and is byte-exact with the baseline Git blob:

- baseline blob: `7c85ca645494623f102ca0ccd873bb4ef74e41c3`
- active Cloud blob: `7c85ca645494623f102ca0ccd873bb4ef74e41c3`
- comparison: `BYTE_EXACT=YES`

The requested chain is visibly preserved in the promoted source:

- public entry and configured max-runs loop: `WubeiTask.java:344-428`
- round phase loop and transaction boundary: `WubeiTask.java:502-590`
- terminal mapping to `TaskRunResult`: `WubeiTask.java:585-590`
- `finally` release boundary: `WubeiTask.java:427-428`

No compile-oriented deletion or stub was introduced because the current Cloud contracts cannot close
this class inside the one-file write set without changing the baseline behavior.

## Blocking Evidence

### P1-1 - Cloud TaskExecutionContext is not source-compatible

The baseline task relies on local-run context members that the active Cloud context intentionally does
not expose:

- `WubeiTask.java:365-366` and `:411-412` call `getWindowRuntimeContext()`; the active Cloud
  `TaskExecutionContext` has no such method.
- `WubeiTask.java:451` assigns `getTaskRunId()` to `long`; active Cloud returns the stable task-run
  identity as `String`.
- `WubeiTask.java:4236-4246` uses `TaskExecutionContext.builder()` to create a local debug context;
  active Cloud `TaskExecutionContext` has no builder and only accepts a non-mintable authorized
  `CloudTaskServiceExecutionContext` delegate.

Impact: the public entry cannot compile or obtain a legitimate Cloud execution authority. Adding a
task-local fake builder or null fallback would mint authority or change the baseline debug/stop
semantics, so it is forbidden.

Repair condition: provide an approved Cloud task-entry assembly/context projection outside this
one-file write set, then adapt this file to that existing authority without minting a second owner or
changing stop/run-progress behavior.

### P1-2 - Phase transaction, turn, and ready-event collaborators are absent

The active Cloud tree has no declarations for these baseline collaborators:

- `TaskTransactionRunner` (`WubeiTask.java:63`, field `:278`, phase call `:515`, release `:604` and
  final release `:428`)
- `TaskTurnCoordinator` (`:64`, field `:279`, live maintenance handoff `:1123`)
- `WindowReadyEventBus` (`:79`, field `:283`, priority/wakeup reads beginning at `:641`)
- `WindowRuntimeContext`, `WindowTaskContextHolder`, and `MultiWindowTaskManager` (used throughout
  phase park/wakeup, prepared-dialog, pathing, and progress state)

Impact: removing these calls would break the baseline phase transaction result, must-yield ordering,
prepared-action priority, pathing wakeups, recovery release, and final `forceReleaseTurn` invariant.
None can be replaced by a one-file wrapper without creating the prohibited second owner/session.

Repair condition: land the approved shared Cloud equivalents and their real task-run wiring first, or
expand this same `+1` unit's write set to include the complete shared boundary. Then reopen I37 and
adapt the preserved class in place.

### P1-3 - Local mechanics still occur directly inside the baseline Task

The active Cloud tree deliberately has no same-name `BagService` or `UICleanerService`, and it also
lacks the local-only `GameClientTracker`, `TextRecognizer`, `CoordinateHelper`, `GameStateUtil`,
`WindowScopedTempPath`, and `InputSequences` mechanics required by this source. Representative live
calls are:

- UI cleanup during round recovery: `WubeiTask.java:605`
- post-accept Alt+C physical sequence: `:2083-2088`
- tracker click input: `:2699-2737`
- probe/return bag mechanics: `:2626` and `:3892`
- bound tracker capture and OCR: `:3034-3180`
- chained tracker physical click: `:4201-4231`

Impact: importing local desktop implementations into Cloud would violate the settled architecture;
deleting them would break exact 五倍 input/capture/order/fallback behavior. The permanent-local
`BagService` and `UICleanerService` require closed typed DHXY boundaries, not duplicate Cloud beans.

Repair condition: supply the existing/approved typed DHXY local macro or observation ports for these
exact call sites, preserving input order, capture binding, delays, terminal results, and fallback
order. Reopen this same count unit only when the complete public caller -> Cloud task -> typed DHXY
mechanics -> closed result chain can be reviewed as one delivery.

### Missing imported Cloud declarations

Static declaration scan found these imported baseline types absent from the active Cloud source tree:

`GameClientTracker`, `TextRecognizer`, `AutomationMetricsService`, `BagService`,
`UICleanerService`, `TaskTransactionRunner`, `TaskTurnCoordinator`, `CoordinateHelper`,
`GameStateUtil`, `MultiWindowTaskManager`, `WindowReadyEventBus`, `WindowRuntimeContext`,
`WindowScopedTempPath`, and `WindowTaskContextHolder`.

These are hard write-set-external prerequisites, not harmless import renames.

## Verification

- `git hash-object` baseline/active equality: passed (`7c85ca645494623f102ca0ccd873bb4ef74e41c3`)
- complete class line count: 4,319
- active Cloud declaration scan: completed
- Maven compile/package: intentionally not run because the task explicitly forbids build/test and the
  missing shared contracts are already a deterministic source blocker
- runtime/application/server/host/Task/poller/UI/capture/input: not started
- Git mutation: none

无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Parent Source Review #1 - 2026-07-15T05:10:00-04:00

父级独立 `git hash-object` 确认 active/baseline blob 均为
`7c85ca645494623f102ca0ccd873bb4ef74e41c3`，完整 Task 源码保全 **APPROVED**；但 active Cloud
缺合法 TaskExecutionContext runtime projection、transaction/turn/ready-event owner，以及 Bag/UI-clean/
capture/OCR/input closed DHXY mechanics。结论 **P0=0/P1=3/P2=0，BLOCKED_MISSING_TYPED_BOUNDARIES /
countDelta=0**。保留整类，不得复制本地 runtime 到 Cloud；待现有单一 shared owners 稳定后，在同一
`WubeiTask::execute` 单只替换原调用点并重新验收。
