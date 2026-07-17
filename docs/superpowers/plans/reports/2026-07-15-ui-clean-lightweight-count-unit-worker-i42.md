# I42 - W-COUNT-UI-CLEAN-LIGHTWEIGHT-1

- worker: Internal implementation Worker I42
- countUnit: `UICleanerService::cleanLightweightInterruptions`
- requested countDelta: `+1`
- delivered countDelta: `0`
- result: `BLOCKED_WRITE_SET / NO_CODE_CHANGE`
- business baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- intentional business differences: none approved; migrate behavior-equivalently from `696a12b0`

## Preconditions read

Read before source inspection:

- `D:\mavenProject\DHXY\AGENTS.md`
- `D:\mavenProject\DHXY\docs\DHXY_CONTEXT.md`
- top CR271 entries in `D:\mavenProject\DHXY\docs\ACTIVE_WORK.md`
- `D:\mavenProject\DHXY\docs\superpowers\plans\2026-07-14-696a12b0-whole-service-first-migration.md`
- `D:\mavenProject\DHXY\docs\superpowers\specs\2026-07-12-service-migration-matrix.md`
- both repository `git status --short --branch`

Both worktrees contain extensive pre-existing dirty/untracked material. Nothing was reverted, overwritten,
cleaned, staged, committed, or deleted.

## Existing requested chain

The requested typed chain is present:

1. Active Cloud caller: Cloud `SummonSkillService.cleanSummonSkillsOnce(request)` invokes
   `runSummonSkillWholePass(...)`, then invokes
   `cloudUiCleanerPort.cleanLightweightInterruptions("summon-skill", "finish-lightweight-clean",
   "summon-skill:finish")` at `SummonSkillService.java:172-182`. The method is reached from
   `TaskMaintenanceService.java:745-755`.
2. Cloud port: `CloudUiCleanerPort.java:52-60` emits exactly
   `UiCleanMacroCommand.Operation.CLEAN_LIGHTWEIGHT_INTERRUPTIONS`; `EXECUTED/HANDLED` maps to `true`,
   `EXECUTED/NOT_HANDLED` and `NOT_EXECUTED` map to `false`, and `STOPPED/UNKNOWN` map to fatal.
3. Stable task context: `CloudUiCleanerPort.java:74-81` obtains the current `TaskExecutionContext`, performs
   stop checkpoints before/after, and calls `CloudGameClient.executeLocalMacro(...)` with
   `LocalMacroKind.UI_CLEAN`. It adds no retry or local state.
4. Exact local binding: `LocalRemoteGameCommandHandler.java:446-461` validates the registered task run and exact
   bound window before dispatch. `LocalRemoteGameCommandHandler.java:1155-1166` dispatches the typed
   `RemoteUiCleanMacroCommandPayload` to `executeUiCleanMacro(...)`.
5. DHXY mechanics: `LocalRemoteGameCommandHandler.java:1266-1310` runs the self-queued cleaner outside the remote
   exclusive queue under `windowTaskContextHolder.callWith(access.context(), ...)`, invokes
   `UICleanerService.cleanLightweightInterruptions(request.getSource())`, and maps the boolean to typed
   `HANDLED/NOT_HANDLED`.
6. Local method parity: `UICleanerService.java:202-231` preserves the `696a12b0` method body and branch order:
   business option first; `BUSINESS_OPTION_CLICKED -> true`; `INTERRUPTED/FAILED -> false`; then generic-window
   cleanup; otherwise `false`. The only diff inside this method is formatting of the fluent call.
7. Closed terminal: `LocalRemoteGameCommandHandler.java:1379-1392` emits the exact four-key payload
   `macroKind/operation/state/cachePoint`, with `cachePoint=null`. Cloud
   `RemoteCommandOutcomeEnvelope.java:259-420` accepts only `EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN`, enforces the
   exact UI-clean result shape, reconstructs the typed result, and verifies the outcome digest.

## Blocking duplicate execution

The full active Summon cleanup path is not baseline-equivalent because it executes the same lightweight cleanup
twice for deterministic `EXECUTED` and `NOT_EXECUTED` whole-pass terminals:

1. DHXY `LocalRemoteGameCommandHandler.executeSummonSkillWholePass(...)` invokes
   `uiCleanerService.cleanLightweightInterruptions("summon-skill:finish")` at
   `LocalRemoteGameCommandHandler.java:2750-2763` before publishing the whole-pass terminal.
2. Cloud `SummonSkillService.runSummonSkillWholePass(...)` maps both `Executed` and `NotExecuted` to a normal return
   at `SummonSkillService.java:211-216`.
3. The caller then invokes the separate `UI_CLEAN` local macro at `SummonSkillService.java:178-182`, causing a
   second dialog scan / generic-window close pass.
4. The `696a12b0` baseline `SummonSkillService.cleanSummonSkillsOnce(request)` invokes
   `UICleanerService.cleanLightweightInterruptions("summon-skill:finish")` exactly once after the exclusive pass.

Impact: one business cleanup occurrence can perform two capture/dialog/input passes. The first pass may consume a
dialog or close a window, changing what the second pass observes and potentially closing another generic window.
That changes the baseline physical-input count/order and cannot be counted as one exact caller-to-terminal unit.

## Why no Java change was made

The duplicate owner is
`D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\LocalRemoteGameCommandHandler.java`, which is
outside I42's exclusive Java write set. Making `CloudUiCleanerPort` or `UICleanerService` silently deduplicate by
source/time would add hidden state/TTL and would violate the closed typed operation and the explicit no-TTL rule.
No stub, wrapper, retry, owner, or behavior-changing shortcut was added.

## Exact unblock condition

Expand the same count unit's exclusive write set to include `LocalRemoteGameCommandHandler.java`, then establish one
cleanup owner per whole-pass terminal without changing baseline ordering:

- for whole-pass `EXECUTED` and `NOT_EXECUTED`, the handler must not perform the direct post-pass cleanup; the
  existing Cloud caller must issue the single typed `UI_CLEAN` macro and consume its boolean/fatal terminal;
- retain the local cleanup only for a terminal that unwinds before the Cloud follow-up can legally run, where
  required to preserve the baseline stop path;
- keep exact task-run/window binding, the local self-owned input queue, source text, terminal mapping, and no retry;
- re-scan the whole-pass plus UI-clean sequence to prove exactly one cleanup attempt for every baseline-equivalent
  terminal.

No build, test, runtime, capture, or input path was run, as required by the task.

## Parent Source Review #1 - 2026-07-15T05:18:00-04:00

父级独立核对 `SummonSkillService:172-182,211-216`、DHXY
`LocalRemoteGameCommandHandler:2750-2763` 与 `UICleanerService:202-231`，确认当前 whole-pass handler 已先做
一次 lightweight cleanup，Cloud normal terminal 返回后又发一次 `UI_CLEAN`，会把基线一次观察/输入变成两次。

结论：**P0=0/P1=1/P2=0，BLOCKED_DUPLICATE_LOCAL_CLEANUP_OWNER，countDelta=0**。精确返修必须把
`LocalRemoteGameCommandHandler.java` 纳入同一整链写集并证明每种 terminal 恰有一个 cleanup owner；该文件当前由
External C 的 TaskTracker 29-Java 整链占用，故先 PARKED，绝不并发抢写、不以 TTL/状态去重。当前内部实现槽释放。
