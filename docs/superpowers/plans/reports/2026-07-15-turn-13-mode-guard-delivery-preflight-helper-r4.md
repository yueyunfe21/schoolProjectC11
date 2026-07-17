# TURN-13 Mode Guard Delivery Preflight Helper-R4

## Scope

- Role: CR271 non-binding Helper-R4.
- Review surface: TURN-13 mode exclusion and local control-entry delivery only.
- Sources read in full: `AGENTS.md`, `docs/DHXY_CONTEXT.md`, the current CR271 section at the top of
  `docs/ACTIVE_WORK.md`, the HTTPS turn master plan, the TURN-13 report, `TurnModeGuard.java`,
  `WindowTaskControlService.java`, `MultiWindowTaskManager.java`, `WindowTaskRunner.java`,
  `TurnLoopRegistry.java`, and `WindowTurnLoop.java`.
- Only this report was written. No Java, plan, CR, dashboard, Maven, test, runtime, application, Task, poller, UI,
  capture, input, or Git command was used.

## PRECHECK Summary

- P0 risk candidates: `0`.
- P1 risk candidates: `1`.
- P2 risk candidates: `2`.
- This is evidence for the parent review. It is not a manager/reviewer conclusion.

## PRECHECK - `startSameQueue` Side-Effect Boundary

No risk candidate found in the requested side-effect ordering.

- `WindowTaskControlService.java:114-123` performs only window-id normalization and empty queue validation before
  entering the mode boundary.
- `:125-129` calls `turnModeGuard.startLocal(...)` before invoking the extracted real workflow.
- The original local-team work now begins inside the guarded supplier at `:132`: leader lookup is `:133-139`,
  local-team candidacy/key creation is `:140-147`, and the first externally visible registration is not until
  `TaskMaintenanceService.registerLocalTeamSessionCandidate(...)` at `:148-152`.
- The leader submit at `:155-167`, remaining window submits at `:175-205`, and result construction at `:209` all
  remain inside the same `startLocal` call.
- `TurnModeGuard.java:40-52` checks every exact window for any registered remote loop and invokes the supplied
  workflow before releasing the same `modeMonitor`. A mode conflict therefore occurs before local-team
  registration, UUID-backed session publication, leader submission, or any member submission.
- The extracted method preserves the pre-existing leader-first/member order and cleanup behavior; the guard did
  not introduce a second registration or submit pass.

## PRECHECK - Three Real Local Submit Entries

No current unguarded production call site was found among the three requested control entries.

1. Same queue: `WindowTaskControlService.java:114-130` enters the guard; the actual manager calls are
   `:156-157` and `:182-187` inside the guarded supplier.
2. Selected task: `:229-240` enters the guard; `submitSelectedTaskWithResult(...)` is at `:246` inside the
   guarded supplier.
3. Detected-role test start: `:265-278` enters the guard; `submitWithResult(...)` is at `:293` inside the guarded
   supplier. The deprecated public method at `:260-263` delegates to this guarded path.

The other public convenience methods at `:88-112` delegate to one of those three paths. A source-wide caller scan
found the four real `taskManager.submit*` invocations only at `:156`, `:182`, `:246`, and `:293` in this service.

## P1 Risk Candidate - Remote Start Accepts A Missing Or Closed Local Runner

### Evidence

- `TurnModeGuard.java:70-75` rejects remote start only when
  `taskManager.getRunner(exactWindowId).filter(runner -> runner.isRunning()).isPresent()` is true.
- An absent runner makes the optional empty and passes the gate. A registered but shutdown runner also passes
  because it is not running.
- The code then immediately creates and starts a remote loop at `:76-83`.
- The parent-frozen TURN-13 contract requires checking the exact local runner before remote create/start
  (`2026-07-15-turn-card-TURN-13.md:25-30`). The later explicit activation contract says turn starts only after
  window registration.
- Exact action execution ultimately requires the registered runner and its bound context; allowing a loop before
  that authority exists can reserve a remote mode that cannot execute a subsequent action safely.

### Impact

- A caller can start a live long-wait loop for an unregistered `windowId` or a permanently closed runner.
- Once the loop is registered, `startLocal(...)` rejects every local start for that ID at
  `TurnModeGuard.java:43-49`, so the invalid remote reservation can also prevent recovery through the normal local
  start flow.
- If Cloud sends an action, exact-window resolution will fail later instead of rejecting the invalid mode
  transition at its control boundary.

### Suggested Repair

- In `TurnModeGuard.startRemote(...)`, resolve the exact runner once while holding `modeMonitor`.
- Reject when the runner is absent or `runner.isShutdown()`; then separately reject when `runner.isRunning()`.
- Only after those checks may the existing `registry.create(...)` plus one `loop.start()` execute.
- Keep the metadata supplier unchanged, add no fallback registration, and do not create/retry a loop for an
  unavailable window.

## PRECHECK - Local/Remote Race

The currently reachable control paths are linearly ordered by the same monitor.

- Local mode holds `modeMonitor` from all-loop checks through the actual manager submits
  (`TurnModeGuard.java:43-52`).
- Remote mode holds that same monitor from runner-state inspection through registry create and loop start
  (`:70-88`). Remote start therefore cannot interleave between a local check and `runner.submit(...)`, and local
  start cannot interleave between a remote check and registry publication.
- `WindowTaskRunner.submit(...)` is synchronized and publishes the volatile `currentTask` before scheduling and
  returning (`WindowTaskRunner.java:285-323`; field at `:162`). Once a guarded local submit succeeds, the next
  guarded remote start can observe it through `runner.isRunning()` (`:423-429`).
- `WindowTaskControlService.normalizeWindowIds(...)` removes null/blank/duplicate IDs before the guarded batch
  (`:461-470`), so the same batch does not submit a duplicate exact window due to input duplication.

## P2 Risk Candidate - Public Manager/Runner APIs Remain A Structural Bypass Surface

### Evidence

- `MultiWindowTaskManager` still exposes public `submit`, `submitWithResult`, `submitQueueWithResult`, and
  `submitSelectedTaskWithResult` APIs (`MultiWindowTaskManager.java:229-301`, `:332-352`).
- `WindowTaskRunner.submit(...)` is also public (`WindowTaskRunner.java:262-323`). Neither type knows
  `TurnModeGuard`.
- Current production source calls the manager submit APIs only through the guarded control service, so this is not
  a currently observed bypass.

### Impact

- A later card could call the public manager or runner directly and reintroduce a local-start/remote-loop race
  without modifying TURN-13 code.

### Suggested Follow-Up

- Keep TURN-13 within its frozen write set; do not modify manager/runner from this card.
- In TURN-40 and every later activation/caller card, require local starts to remain behind
  `WindowTaskControlService`/`TurnModeGuard`. Parent review should reject any new direct submit caller unless a
  separately frozen integration change moves the guard to the manager boundary.

## PRECHECK - Remote Start Failure Cleanup

No current P0/P1 candidate found in the requested synchronous start-failure path.

- `TurnModeGuard.java:76-87` retains the exact loop returned by this create, calls `start()` once, and enters cleanup
  only when that call throws. There is no automatic retry.
- Cleanup first requires the created loop to be stopped and still be the registry's exact instance (`:91-101`).
- `WindowTurnLoop.start()` resets `workerThread` and `running` before rethrowing a synchronous thread-start failure
  (`WindowTurnLoop.java:60-81`).
- `TurnLoopRegistry.remove(...)` permanently retires the stopped loop before removing it
  (`TurnLoopRegistry.java:61-69`), and `WindowTurnLoop.retireIfStopped()` shares the loop lifecycle monitor with
  start (`WindowTurnLoop.java:141-149`). A stale reference cannot successfully restart after cleanup.
- If cleanup itself fails, the cleanup exception is attached to the original start failure
  (`TurnModeGuard.java:97-101`) and the registry remains conservative rather than silently admitting local mode.

## P2 Risk Candidate - Cleanup Relies On All Registry Mutation Remaining Behind The Guard

### Evidence

- The exact-instance check and `remove(...)` are two registry calls at `TurnModeGuard.java:94-99`, while
  `TurnLoopRegistry.find/create/remove` and `WindowTurnLoop.start()` remain callable APIs.
- Current source has no registry create/remove or loop start caller outside `TurnModeGuard`, so the intended
  discipline holds today.

### Impact

- A future direct registry caller could race between the cleanup check and removal. The registry's permanent
  retire step remains fail-closed, but cleanup could leave the failed loop registered and suppress the cleanup
  exception under the original failure.

### Suggested Follow-Up

- TURN-40 must use `TurnModeGuard.startRemote(...)` and must not call registry create/remove or loop start
  directly.
- If a future requirement needs independent registry mutation, first add a separately reviewed atomic
  `removeIfSameAndStopped` boundary inside `TurnLoopRegistry`; do not weaken retirement or add retry.

## Requested Parent Recheck Points

`PRECHECK`

1. Resolve the P1 candidate by requiring an existing, non-shutdown exact runner before remote create/start.
2. Re-read the three guarded local entry paths and confirm mode conflict still precedes all local-team
   registration and submissions.
3. Confirm current source still has no direct manager submit, registry mutation, or loop start caller outside the
   approved control/guard boundaries.
4. Recheck synchronous start failure with exact-instance cleanup and permanent retire semantics.
5. Keep stop/unregister and user-facing remote activation deferred to TURN-40; do not add automatic start here.

