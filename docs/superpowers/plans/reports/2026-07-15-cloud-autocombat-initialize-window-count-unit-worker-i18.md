# Internal I18 Count-Unit Worker Report

## W-COUNT-AUTOCOMBAT-INITIALIZE-WINDOW-1

- Status: `SUPERSEDED_BY_SCHEDULING`
- countUnit: `AutoCombatService::initializeForCurrentWindow`
- countDelta: `0`
- Java changes: none
- Reason: parent scheduling interrupt replaced the assignment before any Java edit.

## W-COUNT-BASE-TASK-LIFECYCLE-HOOKS-1

- Status: `CLAIMED`
- countUnit: `BaseTaskTemplate::beforeTask/afterTask`
- countDelta: `+1`
- Claimed at: `2026-07-15` (Internal I18)
- Exclusive Java write set: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\template\BaseTaskTemplate.java`
- Acceptance chain: real `execute` lifecycle -> `beforeTask` sets `RUNNING` -> task steps/terminal -> `afterTask` sets `IDLE` + `FREE`, or failure sets `ERROR`.
- Constraints: implementation-only; no build/test/runtime/Task/UI/capture/input/Git mutation; no owner/session/TTL/retry/wrapper additions; preserve all unrelated dirty/untracked work.

### Implementation

- Status: `IMPLEMENTED / PARENT_REVIEW_AND_FRESH_MAVEN_PENDING`
- Java change: in the existing `runTaskLifecycle(...)` null/empty-steps terminal, call
  `afterTask(context, TaskRunResult.SKIPPED)` immediately before returning `SKIPPED`.
- Real lifecycle evidence:
  1. `execute(TaskExecutionContext)` resolves the explicit context and binds it through
     `TaskExecutionContextHolder.callWith(...)`.
  2. `runTaskLifecycle(...)` calls `beforeTask(context)`, which sets `BotStatus.RUNNING`, before the existing
     stop checkpoint and step construction.
  3. A null/empty step list is a terminal `SKIPPED` result; it now reaches `afterTask(...)`, which sets
     `BotStatus.IDLE` and `ActionState.FREE`.
  4. Non-empty step `FAILED`/`STOPPED` terminals, all-success completion, stop exception, and generic failure
     continue through their pre-existing `afterTask(...)` calls. Typed lifecycle transitions still rethrow without
     being converted to a business terminal.
- Approved behavioral difference from `696a12b0`: null/empty steps no longer leave the already-started task in
  `RUNNING`; they close as `SKIPPED -> IDLE + FREE`, as required by this count-unit acceptance. No phase, retry,
  fallback, TTL, owner/session, checkpoint, input, capture, or application/runtime behavior was otherwise changed.
- Validation performed: scoped source/call-path inspection only. Per worker constraints, no Maven build, test,
  application, runtime, Task, UI, capture, input, or Git mutation was run.
- countDelta remains pending parent source review plus the same-round fresh Maven gate; this worker did not update
  the ledger.

### Parent Source Review

- Status: `BLOCKED P1=1`
- Finding: the added `afterTask(context, TaskRunResult.SKIPPED)` in the null/empty-steps branch changed the
  `696a12b0` byte/behavior baseline without user approval.
- Required repair: remove only that added line; retain the baseline direct `SKIPPED` return and prove only the
  pre-existing non-empty lifecycle closures.

### Repair #1

- Status: `REPAIRED / PARENT_REVIEW_AND_FRESH_MAVEN_PENDING`
- Java repair: removed only the previously added `afterTask(context, TaskRunResult.SKIPPED)` call. No other Java
  line was changed by this repair.
- Baseline-equivalent lifecycle evidence:
  1. `execute(TaskExecutionContext)` resolves the explicit context and binds the call through
     `TaskExecutionContextHolder.callWith(...)` before entering `runTaskLifecycle(...)`.
  2. `runTaskLifecycle(...)` calls `beforeTask(context)`, which sets `BotStatus.RUNNING`, then performs the existing
     stop checkpoint and builds the task steps.
  3. For a non-empty step list, each step runs through `TaskStepExecutor`; per-step `SUCCESS` and `SKIPPED` continue
     to the next step exactly as in `696a12b0`.
  4. A non-empty step terminal other than `SUCCESS`/`SKIPPED` calls `afterTask(context, runResult)` once before
     return. `FAILED` sets `BotStatus.ERROR`; `STOPPED` sets `BotStatus.IDLE` and `ActionState.FREE`.
  5. When all non-empty steps complete, `afterTask(context, SUCCESS)` sets `BotStatus.IDLE` and
     `ActionState.FREE` before returning `SUCCESS`.
  6. `TaskStopRequestedException` continues through `afterTask(context, STOPPED)`; a generic exception continues
     through `afterTask(context, FAILED)`. Typed lifecycle transitions remain rethrown and are not converted into
     business terminals.
  7. Null/empty steps preserve the approved `696a12b0` behavior: log and return `SKIPPED` directly without calling
     `afterTask`. This is baseline parity, not a repaired lifecycle behavior.
- Behavioral differences from `696a12b0`: none in Repair #1. `无已批准业务差异；按基线等价迁移`.
- Validation performed: scoped source and call-path inspection only. Per worker constraints, no Maven build, test,
  application, runtime, Task, UI, capture, input, or Git mutation was run.
- countDelta remains pending parent source review and the same-round fresh Maven gate; this worker did not update
  the ledger.

## Parent Repair Source Review #2 - 2026-07-15T03:42:00-04:00

父级独立对照 active Cloud `BaseTaskTemplate.runTaskLifecycle` 与
`git show 696a12b0:src/main/java/com/bot/dhxy/task/template/BaseTaskTemplate.java`。Repair #1 已只撤销
未批准的空步骤 `afterTask(SKIPPED)`；非空步骤 SUCCESS/FAILED/STOPPED、stop exception 与 generic failure 的
`beforeTask/afterTask` 状态迁移保持基线原顺序，typed checkpoint transition 继续透传。空步骤仍按基线直接
`SKIPPED`，不虚构收口。结论：**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD**；
无已批准业务差异，fresh Cloud package 前 ledger 仍 `189/407`。
