# TURN-22 Repair #3 post-TURN-28Q continuation delta preflight

- Role: CR271 TURN-22 Repair #3 post-TURN-28Q continuation delta helper.
- Scope: read-only delivery preparation for External A after TURN-28Q reaches parent
  `SOURCE+TEST SOURCE REVIEW PASSED`.
- This report is not an implementation, review verdict, approval, owner assignment, or permission to edit while
  TURN-28Q remains in repair.
- Snapshot time: `2026-07-16T08:53:28-04:00`.
- Business authority: `696a12b0`; no approved business difference.

## 1. Gate and true-EOF state

1. `TURN-22` true EOF is parent Adjudication #2:
   `P0/P1/P2=0/1/0 / REPAIR #3 PREREQUISITE BLOCKED BY TURN-28Q`, External A owner released, Cloud WIP
   preserved. No TURN-22 implementation may resume before a later parent directive opens the gate.
2. `TURN-28Q` true EOF is Parent Review #1:
   `P0/P1/P2=0/3/1 / REPAIR #1 REQUIRED`; External A retains the unique TURN-28Q owner. The public action-list
   signatures are present, but the worker/test bytes are in-flight and are not an accepted frozen delivery yet.
   At this snapshot `InputActionWorker.java` already has a later mtime than Review #1, so its intermediate bytes
   must not be used as delivery evidence.
3. `TURN-28P` true EOF is Parent Review #4:
   `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / INDEPENDENT REVIEW+BUILD PENDING`. Its public
   resolver -> real queue/worker harnesses are valid reference shapes, but they do not replace TURN-22's own
   production-through named test.
4. Resume condition: parent must first read a new TURN-28Q `SOURCE+TEST DELIVERED`, independently pass its
   source/test-source gate, freeze the final action-list signature and result semantics, then append a new
   TURN-22 `REPAIR #3 READY / CLAIM REQUIRED` directive. External A must claim TURN-22 again at true EOF before
   changing its remaining files.

## 2. Current TURN-22 WIP snapshot

| File | Current SHA-256 | State |
|---|---|---|
| Cloud `CloudTeamReturnPortAssembly.java` | `4435B30C4BFC923E222B12DE3CDA5BE9AEEC766AA1F826F26EA534BC1A5CFD66` | Read-only production; one typed `CLICK_LEFT` carries `clickDelayMs=150` and `queueHoldMs=500` |
| Cloud `TeamReturnTurnContractTest.java` | `2D2907592E96D3C44E4AE239A8F569ADBA785568B19309D3F35CE90CB49E9496` | Repair #3 item 1 WIP complete; illegal DHXY-only mechanics imports/fixtures removed |
| DHXY `TurnInputStepExecutor.java` | `0EE95CBD48D3EC76FB9E50385108F9898F2979A33966487B39065352AF1F43FD` | Unchanged; still calls legacy `contextHolder.callWith(... inputActionQueue.submitAndWait(...))` |
| DHXY `TurnInputStepExecutorContractTest.java` | `BB1CCC432020A8ACD61C82ABE207E13FB7959D94E9F8F6F27DB28B43DAFB738D` | Unchanged; `RecordingInputQueue` overrides only legacy boolean `submitAndWait` and proves neither frozen generation nor real worker execution |

The Cloud WIP already owns its module boundary: every remaining `com.bot.dhxy.*` import in the test resolves to a
source file inside the Cloud repository, and the file has zero imports of DHXY-only mechanics such as
`TurnExecutionWindow`, `TurnInputStepExecutor`, `TurnInputActionMapper`, `TurnKeyMapper`, `InputAction*`, or
`Window*`. Do not reintroduce those imports. The Cloud test remains responsible only for assembly/JSON, one
command, one UUID, terminal/uncertain projection, no frame, and zero transport retry.

## 3. Frozen action-list API to consume after TURN-28Q source pass

Current public signatures are:

```java
InputActionExecutionResult InputSequences.submitFrozenExactWindowActionsAndWait(
        String description,
        WindowRuntimeContext context,
        WindowNativeBinding binding,
        List<InputAction> actions)

InputActionExecutionResult InputActionQueue.submitFrozenExactWindowActionsAndWait(
        String description,
        WindowRuntimeContext context,
        WindowNativeBinding binding,
        List<InputAction> actions)
```

The queue copies the complete list, freezes binding-object identity plus `playerIdentityEpoch` under the context
monitor, performs no refresh/title search, and returns the existing typed `InputActionExecutionResult`. The final
TURN-28Q source pass may change worker/test details; A must re-read the accepted true EOF and actual signatures
immediately before editing TURN-22.

### Parent freeze point before resume

The smallest compile-safe TURN-22 production delta under the already frozen write set is to retain the existing
`InputActionQueue` constructor dependency and replace only the legacy call in `submitMouseActions(...)` with the
queue's public `submitFrozenExactWindowActionsAndWait(description, window.context(), window.binding(), actions)`.
That is the lower half of the same TURN-28Q public API and avoids a new wrapper, nested queue, second refresh, or
constructor churn.

The older TURN-22 directive names the `InputSequences` facade. Replacing the constructor dependency with
`InputSequences` is not an exact two-file delta: it also breaks the two current `new TurnInputStepExecutor(...)`
call sites in protected `LocalTurnActionExecutorContractTest.java`. Parent must therefore explicitly choose and
record one of these before A resumes:

- recommended minimal route: authorize the existing `InputActionQueue` dependency to call the accepted public
  frozen action-list method directly; or
- facade route: expand the exact write set to every affected constructor call and freeze that wider change.

A must not invent an adapter constructor, manually `new InputSequences(...)` in production, add a wrapper chain,
or modify protected tests without that parent decision.

## 4. Minimal production diff checklist

Only after the gate and parent freeze above:

1. Keep `TurnInputActionMapper.mapMouse(...)` as the sole mapper. For the TeamReturn spec it must produce exactly
   one immutable ordered list:
   `CLICK_LEFT(x=emitted absolute x, y=emitted absolute y, delay=150)` then `SLEEP(500)`.
2. Submit that complete list once through the accepted frozen action-list API with exactly
   `window.context()` and the same `window.binding()` object returned by the action resolver. Remove the mouse
   path's `contextHolder.callWith(...)` plus legacy `submitAndWait(...)`; do not refresh, re-resolve, compare a
   second snapshot, or submit one action at a time.
3. Project the typed result truthfully:
   - only `executionResult.isCompleted()` becomes `Result(COMPLETED, OK)`;
   - `safetyReason == STOP_REQUESTED` becomes typed `STOPPED`;
   - `NOT_STARTED`, `PARTIALLY_COMPLETED`, `STARTED_UNKNOWN`, binding/task mismatch, interruption without typed
     STOP, and every other uncertain/failure result remain non-success (`INPUT_QUEUE_FAILED` with diagnostic
     status/reason). Never infer success from a completed prefix.
4. Preserve the key-tap/background keyboard path and `waitFor(...)` byte-for-byte unless compilation makes an
   import cleanup unavoidable. Do not add business/OCR/retry/session/ledger/TTL/durable-workflow behavior.
5. Preserve the Cloud assembly and current Cloud test WIP hashes/semantics. The baseline is the one
   `696a12b0` queue submission containing `clickLeft(..., 150)` followed by `sleep(500)`; there is no second
   command, UUID, click, move, transport retry, or local business decision.

## 5. DHXY named-test checklist

Replace the proof-only `RecordingInputQueue(super(null, null, null))` path for the frozen cases with a fully
in-memory real queue/worker harness. The harness may count the public frozen submission only if it delegates to
`super`; it must not fabricate `InputActionExecutionResult` or bypass `InputActionWorker`.

### A. Happy path: one request, exact ordered mechanics

- Resolve/build one exact action window whose context currently publishes the very same binding object.
- Preinstall a distinct caller-thread sentinel `WindowRuntimeContext` in `WindowTaskContextHolder`.
- Execute the real production `TurnInputStepExecutor.execute(...)` with the actual
  `TurnInputSpec(..., clickDelayMs=150, queueHoldMs=500)`.
- Assert exactly one frozen API invocation/request and one worker take/transaction; the taken immutable list is
  exactly two elements in order: `CLICK_LEFT(delay=150)` and `SLEEP(500)`. Assert emitted absolute x/y, no move,
  no second click, no extra refresh, and exactly one frozen focus using the exact HWND/process/rect/binding.
- Assert typed executor result `COMPLETED/OK`, `startedStepIndex=0`, `lastCompletedStepIndex=1`, and after return
  the caller sentinel is the identical object with identical binding/epoch. Empty-to-empty is not acceptable.

### B. Exact snapshot and A -> B -> A drift

- Queue a real blocker with latches, enqueue the production executor request against binding object A, then make
  the same context publish B and a value-equal but different A object before worker admission.
- Assert the request returns a non-success typed result with
  `NOT_STARTED/WINDOW_BINDING_CHANGED`, zero TeamReturn input-provider calls, zero TeamReturn focus, zero later
  action, and zero added refresh. Equality of HWND/process/rect values must not hide the object-generation drift.
- Assert the caller sentinel is still restored exactly after the rejected call.

### C. STOP and uncertainty projection

- Use a real `TaskExecutionContext`/`TaskStopToken` and latch-based ordering, not thread sleep or source scanning.
  Admission-before-input STOP must return executor `STOPPED`, with zero click/sleep/focus for the TeamReturn
  request.
- For a started/partial/unknown queue result, assert executor never reports `COMPLETED/OK`; preserve truthful
  prefix indexes and run no later action.
- Keep generic pause/cancellation/action-list mechanics in TURN-28Q/28P tests; TURN-22 only needs enough
  production-through evidence to prove its own `150/500` action and typed projection.

### D. Cloud module boundary

- Keep `TeamReturnTurnContractTest` free of imports for DHXY-only executor/queue/window/input mechanics.
- Assert the same emitted Cloud action still has one INPUT step, `CLICK_LEFT`, `clickDelayMs=150`,
  `queueHoldMs=500`, no WAIT step/frame, one HTTPS command, one UUID, terminal/uncertain fail-closed, and zero
  transport retry. DHXY queue mechanics belong exclusively to the DHXY named test.

## 6. Exact continuation write set and no-go list

Expected continuation write set after parent reopens TURN-22:

1. Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java` only if a minimal
   line/reference cleanup is still necessary; otherwise preserve SHA `2D290759...`.
2. DHXY `src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java`.
3. DHXY `src/test/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutorContractTest.java`.
4. Original append-only TURN-22 card.

Read-only: Cloud assembly, mapper/protocol/POM, TURN-28P/28Q files, callers/Tasks/Services, and all unrelated
dirty/untracked bytes. Prohibited: nested queue, second refresh/resolver/comparator, manual mechanics in callback,
manual typed-result construction as proof, private-production reflection, wall-clock race sleeps, auto retry,
session/owner/ledger/TTL/durable workflow, runtime/input, or Git mutation.

## 7. Parent handoff checklist

- Re-read TURN-28Q true EOF and accepted file SHAs after its Repair #1 delivery.
- Verify TURN-28Q parent source/test-source verdict is `0/0/0`; this report cannot open that gate.
- Freeze queue-direct versus `InputSequences`-facade dependency route in TURN-22 before assigning A.
- Recompute all four TURN-22 WIP SHAs and verify no other writer changed the exact write set.
- Append TURN-22 READY/CLAIM directive; require A's new true-EOF claim before source edits.
- After A delivery, parent independently reviews production/test/Cloud boundary and `696a12b0`; independent
  reviewers and authorized named-test/compile gates remain separate.

No Java, original card, or other document was modified. No Maven/JUnit/compile/package/runtime/application/
server/Task/UI/capture/input was run. No Git mutation was performed.

<!-- PRECHECK_COMPLETE + TRUE_EOF: TURN-22-POST-28Q-CONTINUATION-DELTA-HELPER 2026-07-16T08:53:28-04:00 -->
