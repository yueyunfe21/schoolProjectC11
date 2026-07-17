# I38 - FiveRingTaskV2 whole execute migration

## CLAIMED

- task: `W-COUNT-FIVE-RING-TASK-EXECUTE-WHOLE-1`
- claimedAt: `2026-07-15T05:06:14-04:00`
- countUnit: `FiveRingTaskV2::execute(TaskExecutionContext)`
- requestedCountDelta: `+1`
- finalCountDelta: `0`
- result: `BLOCKED / FULL_BASELINE_FILE_PRESERVED`
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- unique Java write set: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\wuhuan\FiveRingTaskV2.java`
- report write set: this file only

## Required baseline reads

- Read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, the top CR271 entry in `docs/ACTIVE_WORK.md`, the Five-ring section in `docs/业务逻辑.md`, the whole-Service plan, the migration matrix, and both repository statuses before writing.
- The checked Five-ring business rows are `docs/业务逻辑.md:1038-1085`: preserve the phase loop, outside-turn yield behavior, the existing ready-event priority checks, tracker prepared action semantics, two different completion stories, and the current `NavigationService` ownership of `ROUTE_TRANSFER`.
- No approved business difference; migrate behavior-equivalently from `696a12b0`.

## Implementation delivered

- Added the complete 2,716-line baseline class at the unique Cloud path. No method, branch, delay, fallback, state update, comment, or nested type was removed or rewritten.
- Blob proof:
  - baseline blob: `f5c5022162b89953216e1787546f4a0c616e5fe0`
  - active Cloud blob: `f5c5022162b89953216e1787546f4a0c616e5fe0`
  - equality: `true`
- The preserved file contains the full entry and lifecycle: `execute()` at line 228, `execute(TaskExecutionContext)` at line 241, startup check at line 252, configured run loop at line 263, `runPhases(...)` at line 364, phase-terminal loop at line 368, and `finally forceReleaseTurn(...)` at line 314.

## BLOCKED evidence (worker finding, parent review required)

The requested single-file scope cannot close the active caller-to-terminal chain. Adapting around these gaps inside `FiveRingTaskV2.java` would require stubs or would move/change baseline mechanics, which the task explicitly forbids.

1. **Task transaction and window-runtime ownership are absent from active Cloud.**
   - Missing source types include `TaskTransactionRunner`, `WindowRuntimeContext`, `WindowReadyEventBus`, `WindowScopedTempPath`, and `WindowTaskContextHolder` (`FiveRingTaskV2.java:51,63-66,207,210-212`).
   - They are live dependencies, not unused imports: phase transactions and outside-turn release occur at lines 417/484; ready-event/runtime state is read at lines 504-552 and throughout pathing; the final release is line 314.
   - Impact: `execute -> runPhases` cannot compile or preserve turn/yield/ready-event semantics using the current one-file reservation.

2. **Two user-fixed permanent-local Services are still direct task dependencies.**
   - `BagService` and `UICleanerService` are absent from active Cloud by design (`FiveRingTaskV2.java:36,42,202,204`).
   - Live calls include bag lookup/count/open at lines 837/1107/1114 and cleanup calls at lines 761/1186-1243/1801-1805/2584/2596.
   - Existing generic bag/UI macro infrastructure does not expose a complete Five-ring task-level typed replacement for all of these baseline operations in this file's scope.
   - Impact: replacing these calls locally would require new typed contracts/handlers or business-shape changes outside the unique write set.

3. **Desktop observation and physical-input mechanics remain embedded in the baseline task.**
   - Missing active Cloud collaborators include `GameClientTracker`, `CoordinateHelper`, `GameStateUtil`, `TextRecognizer`, `OcrWindowScanService`, and `AutomationMetricsService` (`FiveRingTaskV2.java:3,7,8,54-55,67,200-215`).
   - Live desktop work includes direct `InputSequences` actions at lines 1017/1031/1143/1176/1284/1418/1448/1477/2493/2532, exact-window capture at line 1335, and local template/coordinate work at lines 1330-1477.
   - Impact: the task does not yet reach typed DHXY observation/input terminals; preserving click/capture/order/delay requires closed local mechanics outside this one-file scope.

4. **The full entry therefore has no active Cloud construction/caller closure yet.**
   - The class is now preserved, but its constructor graph cannot be built while the dependencies above are absent.
   - A source file existing or matching the baseline blob is not enough for the hard `+1` gate.

## Exact unblock condition

Keep this complete file unchanged. Reopen the same count unit only after the parent reserves a non-overlapping whole-chain scope that supplies existing typed Cloud replacements for:

- task transaction/turn release and window ready/pathing facts;
- all Five-ring bag and UI-clean local operations;
- tracker/capture/template/OCR/coordinate observations;
- every ordered physical-input sequence and its closed terminal;
- a real Cloud task construction/caller path.

That follow-up must adapt only the original call sites while preserving every baseline phase, condition, delay, fallback, state mutation, completion-story distinction, and final `forceReleaseTurn`. No stub, no extra owner/session/TTL/retry, and no helper-only count is acceptable.

## Verification performed

- Confirmed the new Cloud file is byte-identical to the Git baseline by Git blob hash.
- Confirmed only the reserved Java path plus this report were written by I38.
- Per task instruction, did not run Maven build, tests, runtime, application/server/host/Task/poller/UI/capture/input, or any Git mutation.

## Parent Source Review #1 - 2026-07-15T05:10:00-04:00

父级独立 `git hash-object` 确认 active/baseline blob 均为
`f5c5022162b89953216e1787546f4a0c616e5fe0`，完整 Task 源码保全 **APPROVED**；但 transaction/
window-ready/pathing、永久本地 Bag/UI-clean、tracker/capture/template/OCR/input 与真实 Cloud construction
均未闭合。结论 **P0=0/P1=4/P2=0，BLOCKED_MISSING_TYPED_BOUNDARIES / countDelta=0**。保留整类，
后续必须在同一 `FiveRingTaskV2::execute` 单接现有 typed owners，保持 phase/yield/story/final release。
