# TURN-28Q Repair #1 Test Preflight Helper

Status: `PRECHECK_COMPLETE`

Role: CR271 TURN-28Q Repair #1 test preflight helper only. This report is a non-binding implementation
checklist. It is not delivery, review, approval, parent adjudication, or permission to widen the repair write
set.

## Read-only inputs

- Original card latest physical EOF: `PARENT SOURCE+TEST-SOURCE REVIEW #1 - REPAIR #1 REQUIRED`,
  `P0/P1/P2=0/3/1`, timestamp `2026-07-16T08:46:17.085-04:00`.
- Card snapshot read: SHA-256
  `879373fb4ac8d98ed0b3397e18e3cd0fe90f1ef88eaa2a6c5e90454142610d7d`.
- `InputActionFrozenExclusiveContractTest.java`: 734 lines, SHA-256
  `943dc4867b55569f24788f50bcf83227ce1543a1b71c3c53232f11eb12deea16`.
- No production source, unrelated report, CR document, or repository history was used as an additional
  authority in this preflight.

## Frozen edit boundary

The implementation owner may use this checklist only inside the parent-frozen Repair #1 write set:

1. `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
2. `src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java`
3. the append-only original TURN-28Q card

For the test file, preserve every pre-existing callback-path test and the existing callback harness behavior.
Do not rewrite those cases merely to remove their historical `Unsafe`, reflection, or queue polling. The no-
`Unsafe` / no-private-reflection / no-source-scan / no-sleep-polling requirement applies to the repaired and new
action-list cases. This keeps callback non-regression evidence byte-stable while giving the action-list path an
independent, honest harness.

## Minimal real action-list harness

Add one test-private action-list harness beside, not underneath, the callback harness:

1. Construct a real `TaskExecutionContextHolder`, real `InputActionQueue`, real `InputActionWorker`, real
   `WindowAwareInputCoordinator`, in-memory `InputProvider`, and `InputSequences(realQueue)`. Every repaired
   action-list case must invoke the public `InputSequences` frozen exact-window action-list API. Direct calls to
   `InputActionQueue.submitFrozenExactWindowActionsAndWait(...)` are forbidden in those cases.
2. Use a test-private counting queue that delegates its overridden `take()` to `super.take()`. It may count,
   snapshot, and latch the request returned by the real queue, but must not emulate admission, execution,
   completion, or result projection. Record the target request id and immutable action list at the actual take
   boundary.
3. Build `WindowRuntimeContext` and `WindowNativeBinding` directly. Pass those exact objects to the public
   facade. The new harness must not call `Harness.resolve(...)`, `Unsafe.allocateInstance`, private reflection,
   source scanning, or a production-private method.
4. Keep focus, refresh, provider calls, request take, first-action completion, pause-wait entry, resume, and
   terminal completion observable with latches/counters. No `Thread.sleep`, queue-size polling, wall-clock race
   assertion, or manual construction of an `InputActionExecutionResult` is admissible.
5. Worker output is the sole result authority. Test helpers may trigger stop/pause/rebind and record mechanics;
   they must not synthesize status, safety reason, completed-prefix indexes, or command results.

## Required repaired cases

### 1. Public facade, one take, exact complete list

- Submit through `InputSequences` exactly
  `[CLICK_LEFT(x=300,y=400,clickDelayMs=150), SLEEP(500)]`.
- Wait on the counting queue's target-taken latch and final result, then assert exactly one target `take()`, one
  non-null request id, and that the taken request contains exactly two immutable actions in that order with the
  exact type and parameters above.
- Assert one exact-binding focus, one physical `clickLeft(300,400,150)`, no second input call, no second refresh,
  typed `COMPLETED/CLEAR`, `startedStepIndex=0`, and `lastCompletedStepIndex=1`.
- A second request, a callback-local nested submission, or inspection inferred only from progress indexes fails
  this case.

### 2. Real stop token and typed terminal projection

- Install a real `TaskExecutionContext` carrying a real `TaskStopToken` before the public-facade submission so
  that the queue/request captures production stop state.
- Use a two-action list whose second action is physically observable. The first provider action requests stop
  through that real token and then returns normally; do not interrupt the worker thread.
- Assert one take and one request id, first-action truthful completion, zero later provider action, typed
  `STOP_REQUESTED`, non-completed result, and no retry/re-enqueue. A bare boolean, `status != COMPLETED`,
  `Thread.interrupt()`, or manually fabricated result is insufficient.

### 3. Typed A -> B -> A without race polling

- Capture exact binding A, publish B, then publish field-equal but object-distinct A' before calling the public
  facade. This synchronous stale-snapshot rejection avoids blocker queues and `waitUntilQueued` polling.
- Keep the full returned result and assert typed `NOT_STARTED` plus
  `WINDOW_BINDING_CHANGED`, zero target take, zero focus, zero input, and zero extra refresh.
- Also assert A' is value-equal where relevant but object-distinct from A. Do not reduce the result to
  `isCompleted()` or a boolean holder.

### 4. Pause then resume inside the same taken request

- Submit through `InputSequences` a two-action list with an observable second action. The first provider action
  requests pause through the request's task context and returns normally.
- Observe entry into the real public pause-wait contract using a test-private latch/delegating pause controller;
  do not infer pause from elapsed time or thread state. At that positive wait-entry boundary assert the second
  provider action has not started.
- Resume through the same task context, then assert the second action executes, the result completes, and the
  counting queue still reports exactly one take and the same request id. No release/re-enqueue, refresh,
  replay, or new generation is allowed.
- If the existing public pause contract provides no deterministic wait-entry seam, the implementation must
  report that exact blocker to the parent. It must not replace the proof with sleeps, polling, reflection, or a
  test-only production shortcut.

## Non-regression and rejection checklist

- Existing callback tests and callback harness behavior remain unchanged; Repair #1 tests only the new
  action-list branch plus the worker's per-action pause checkpoint required by Review #1.
- New action-list cases contain no `Unsafe`, private reflection, source/text scan, `Thread.sleep`, queue-size
  polling, real desktop input, capture, runtime startup, or hand-built terminal result.
- All terminal evidence remains typed: STOP is `STOP_REQUESTED`; stale A -> B -> A is
  `NOT_STARTED/WINDOW_BINDING_CHANGED`; pause/resume returns the original request's completed result.
- Counting/inspection observes the real request after real `take()` and verifies the complete list. It does not
  become a fake queue or bypass the worker.
- No automatic retry/replay/session/ledger/TTL/durable workflow and no business/OCR behavior are introduced.
  The success list preserves the `696a12b0` click delay `150ms` followed by the same-queue `500ms` hold.

## Verification boundary

This helper did not modify Java or the original card and did not run Maven, JUnit, compile, package, runtime,
application, server, Task, UI, capture, or input. It performed no Git mutation. Passing this preflight does not
mean Repair #1 is delivered or approved; the implementation owner must append a true-EOF delivery to the
original card, after which the parent and independent reviewers apply their own gates.

<!-- TRUE_EOF: TURN-28Q REPAIR-1 TEST-PREFLIGHT PRECHECK_COMPLETE PUBLIC-FACADE REAL-QUEUE-WORKER TYPED-STOP-ABA PAUSE-RESUME NO-UNSAFE-NEW-CASES 2026-07-16T08:49:53.691-04:00 -->
