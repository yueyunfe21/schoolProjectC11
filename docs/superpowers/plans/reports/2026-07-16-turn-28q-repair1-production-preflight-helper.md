# TURN-28Q Repair #1 Production Preflight

## Role And Authority

- Role: TURN-28Q Repair #1 production preflight helper only.
- This report is a non-binding implementation checklist. It is not implementation, review, approval, or
  parent adjudication.
- Read-only scope used for this preflight:
  - `2026-07-16-turn-card-TURN-28Q.md` latest true EOF Parent Review #1;
  - `InputActionWorker.java` pause/stop and frozen callback/action-list paths;
  - `InputActionRequest.java` captured tokens, typed cancellation, progress, and completion paths;
  - existing `InputActionFrozenExclusiveContractTest` and `InputActionPauseCancellationGuardTest` evidence;
  - `TaskPauseToken` / `TaskStopToken` wait semantics needed to interpret those paths.

## Snapshot Read

| File | Lines | SHA-256 |
| --- | ---: | --- |
| `2026-07-16-turn-card-TURN-28Q.md` | 275 | `879373fb4ac8d98ed0b3397e18e3cd0fe90f1ef88eaa2a6c5e90454142610d7d` |
| `InputActionWorker.java` | 735 | `4b853f9552ae4f59ae46346bc1ba8606de40bcc5e9ad3392cf73ddea1f83b6e5` |
| `InputActionRequest.java` | 1118 | `23973b7eee06949138e8a2841e249c009eb69184804c2be0689aa317c29988de` |
| `InputActionFrozenExclusiveContractTest.java` | 734 | `943dc4867b55569f24788f50bcf83227ce1543a1b71c3c53232f11eb12deea16` |
| `InputActionPauseCancellationGuardTest.java` | 426 | `601bc726a685f6485f3e71fc1725421391ddde77733149e959f9efb70fd5ccd1` |

Parent Review #1 true EOF is `REPAIR-1-REQUIRED P0P1P2=0/3/1`. This helper addresses only the
production part of P1-1. P1-2, P1-3, and P2-1 remain test-source work owned by the card.

## Current Production Gap

- `InputActionWorker.runFrozenExactWindowActions(...)` calls `waitIfPaused(...)` once before entering the
  context monitor, then loops over every action without another pause checkpoint.
- The loop already checks cancellation, worker interruption, exact generation, detailed safety, and typed
  step admission before each action. Pause is the only missing per-action boundary.
- Therefore a pause requested by the first provider action can currently allow the next action to start.
- `InputActionRequest.frozenExactWindowActions(...)` already captures the same `TaskPauseToken` and
  `TaskStopToken` in one immutable request. No request-model change is required.

## Minimal Production Repair Checklist

1. **Touch only the frozen action-list loop.**
   - In `InputActionWorker.runFrozenExactWindowActions(...)`, add the existing
     `waitIfPaused(request, stage)` gate at every action start, before `tryStartStep(...)` and before the
     action reaches `execute(...)`.
   - Keep the existing initial `before-frozen-actions` pause/admission gate outside the monitor. It still
     prevents a request paused before start from owning the generation monitor.

2. **Keep a mid-list pause inside the same transaction and context monitor.**
   - The new per-action wait must remain inside the existing `inputCoordinator.callInputTransaction(...)`,
     `synchronized (context)`, and `InputActionScope.callWith(request, ...)` nesting.
   - Do not release/reacquire `context`, return to the queue, submit another request, refresh/re-resolve the
     window, refocus, or call worker admission again while paused.
   - While the wait is blocked, no later action may call `tryStartStep(...)`, `execute(...)`, or the physical
     provider.

3. **Resume the same request at the same next action.**
   - A normal `TaskPauseToken.resume()` must let `waitIfPaused(...)` return and continue the existing local
     loop with the same `InputActionRequest`, request id, frozen binding/epoch witness, transaction, context
     monitor, and truthful completed prefix.
   - Pause alone must not call `request.cancel(...)`, complete the result, reset action indexes, replay a
     completed action, or create a new UUID/request.

4. **Preserve typed stop projection.**
   - Stop while paused must continue to escape `TaskPauseToken.waitIfPaused(stopToken)` as
     `TaskStopRequestedException`; the existing outer `handle(...)` catch remains the sole projection to
     `InputActionSafetyReason.STOP_REQUESTED`.
   - Stop observed when no pause is active must continue through the existing per-action
     `checkDetailedSafety(stage)` / frozen stop-token path and produce the same typed
     `STOP_REQUESTED` result.
   - In both cases, keep the already completed prefix and start zero later actions. Do not map stop to bare
     interruption, `CLEAR`, boolean-only failure, or fabricated completion.

5. **Do not regress the callback path.**
   - Do not edit `runFrozenExactWindowExclusive(...)`, the `hasExclusiveCallback()` branch selection,
     `InputActionScope.checkpoint()`, or callback completion/finally behavior.
   - Do not add the per-action loop gate to the callback path: callbacks retain their own cooperative
     checkpoints and completion barrier.
   - Existing callback no-second-refresh, cancellation-finally, drift/A-B-A, non-Runtime throwable, and
     exact-field rejection cases must remain behaviorally unchanged.

6. **Keep `InputActionRequest` unchanged for this production repair.**
   - Its frozen factory already copies the complete ordered list and captures pause/stop tokens.
   - Its cooperative cancellation and `complete(...)` paths already preserve typed safety reason and prefix
     progress after a started frozen step.
   - Do not add a new result DTO, token, pause state, wrapper method, retry/replay path, session, ledger, TTL,
     deadline, or durable workflow.

7. **Correct the stale method contract comment.**
   - Update only the `runFrozenExactWindowActions(...)` JavaDoc text that currently says a mid-list pause is
     deferred to the next request boundary.
   - State the repaired invariant: each action start is a cooperative pause boundary held inside the same
     transaction/context monitor; resume continues the same request, and stop remains typed and terminal.

## Production-Side Evidence Expected From The Card Test Repair

- A latch-driven first action requests pause; before resume, later provider calls and later step starts are
  zero.
- Resume completes the remaining ordered actions under the same taken request id, one queue transaction,
  one exact focus, and one frozen generation witness; the first action is not replayed.
- A real `TaskStopToken` requested while the worker is waiting at the mid-list pause boundary yields
  `STOP_REQUESTED`, preserves the truthful prefix, and starts zero later actions.
- Existing frozen callback cases remain unchanged and continue to exercise the original callback branch.

## Explicit Non-Solutions

- Do not treat pause as terminal cancellation for this frozen action-list contract. The old ordinary-list
  guard that expected pause to end the request is not the TURN-28Q resume contract.
- Do not move the wait outside `synchronized (context)` for mid-list actions.
- Do not split the list, nest queue calls, add a second comparator/refresh, poll with wall-clock sleep, or
  manufacture a success/failure result in production.

PRECHECK_COMPLETE

<!-- TRUE_EOF: TURN-28Q REPAIR-1 PRODUCTION-PREFLIGHT MID-LIST-PAUSE SAME-TRANSACTION-SAME-REQUEST TYPED-STOP CALLBACK-NONREGRESSION -->
