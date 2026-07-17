# TURN-13 Readiness Helper-R2 Preflight

## Role And Scope

- Role: CR271 Helper-R2, non-binding dependency/readiness preflight only.
- This report does not make a manager/reviewer decision and does not dispatch work.
- Only this report was written. No Java, runtime, Task, poller, UI, capture, input, test, authoritative plan, CR, Git state, or existing dirty/untracked file was modified.
- Sources read in full: `AGENTS.md`, `docs/DHXY_CONTEXT.md`, the current CR271 section at the top of
  `docs/ACTIVE_WORK.md`, the HTTPS turn master plan, the authoritative HTTPS turn protocol design, and the
  TURN-05/06/11/12 reports. Relevant DHXY and Cloud wiring/source was then read directly.

## Overall Status

`MATERIAL_NOT_YET_AVAILABLE`

- TURN-05, TURN-06, and TURN-11 have parent source conclusions recorded in their reports and are available as
  TURN-13 dependency material.
- TURN-12 has a real three-file source delivery at `2026-07-15T16:28:57-04:00`, but its report still says
  `PARENT REVIEW PENDING` (`2026-07-15-turn-card-TURN-12.md:19-62`). TURN-13 depends directly on TURN-12
  (`2026-07-15-https-turn-complete-migration-card-plan.md:428-434`), so TURN-13 must not be claimed until the
  parent has independently resolved that pending review.
- No Maven gate should be used to bypass the missing parent source conclusion. The final Foundation build
  convergence point is after TURN-12 is resolved, TURN-13 Java/config writing is stable, and no Java writer is
  active in either repository.

## Dependency And Write-Set Precheck

### `READY_PRECHECK` - Existing Foundation material

- Cloud routes are materially present: `CloudBrainServer.java:87-94` creates one `CloudTurnRoutes.Bundle`,
  registers `/api/v1/client/turn` and `/api/v1/templates/` once each, then leaves the legacy root gateway in
  place. TURN-05's report records the route integration and its repaired authentication boundary.
- DHXY transport/action material is present: TURN-06 supplies `TurnClient`/`HttpsTurnClient` and template
  download transport; TURN-11 supplies the Spring component `LocalTurnActionExecutor` and the exact-window
  action path; TURN-12 supplies the inert loop/factory/registry source.
- TURN-12 does not auto-start: `TurnLoopFactory.java:19-38` creates a stopped loop, `TurnLoopRegistry.java:21-46`
  registers it without calling `start()`, and `WindowTurnLoop.java:59-88` exposes only explicit start/stop.

### `NEEDS_PARENT_DECISION` - Correct the exact control-service path before briefing TURN-13

- The master card names conditional modification of
  `window/execution/WindowTaskControlService.java` (`...card-plan.md:432-433`), but that path does not exist.
  The real Spring service is
  `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java` (`:1`, `:26-28`).
- Suggested parent action: correct/freeze the TURN-13 exact write set to the real `window/control` path before
  assignment. Do not let an implementation worker create a duplicate class under `window/execution`.
- Current target-file status is otherwise clear: `TurnClientProperties.java`, `TurnConfiguration.java`, and
  `TurnModeGuard.java` do not yet exist; the real control service and `application.properties` are tracked and
  currently have no target-scoped status entry. TURN-12's three delivered files are distinct. A search of current
  turn-card reports found no other active card claiming these TURN-13 target files.

## Spring Bean Readiness

### `READY_PRECHECK` - Beans TURN-13 must assemble

The following classes intentionally are not component-scanned beans and therefore need one central
`TurnConfiguration` assembly:

1. `HttpsTurnClient` / `TurnClient`: `HttpsTurnClient.java:37,65-85` is a plain final class requiring base URI,
   bearer token, connect timeout, request timeout, and `ObjectMapper`.
2. `TurnTemplateCache`: `TurnTemplateCache.java:26,43` is plain and requires template root plus `TurnClient`.
3. `TurnMatchStepExecutor`: `TurnMatchStepExecutor.java:24,29-32` is plain and requires `TurnTemplateCache` plus
   the existing `TurnCaptureStepExecutor` bean. This is immediately necessary because the already scanned
   `LocalTurnActionExecutor` requires it in its constructor (`LocalTurnActionExecutor.java:22-39`).
4. `TurnLoopFactory`: `TurnLoopFactory.java:9,14-17` is plain and requires `TurnClient` plus
   `LocalTurnActionExecutor`.
5. `TurnLoopRegistry`: `TurnLoopRegistry.java:12,17-19` is plain and requires `TurnLoopFactory`.

Do not register `WindowTurnLoop` as a singleton bean. It is a per-window object created inertly through the
factory/registry. `TurnExecutionWindow` is also not a bean; its static exact-window resolver is invoked per action.

### Suggested `TurnClientProperties` minimum

`READY_PRECHECK`

- A dedicated turn prefix and typed values for base URI, bearer token, connect timeout, HTTP request timeout,
  Cloud long-wait timeout, and local template root.
- Fail fast on blank token, nonpositive durations, a request timeout that is not greater than the long-wait
  timeout plus a small transport margin, non-loopback plain HTTP, or an unusable template root.
- Keep these values separate from the existing legacy `cloud.*` decision/sidecar switches at
  `application.properties:43-59`; especially do not reuse `cloud.dev-sidecar.auto-start-enabled` as permission to
  start a turn loop.
- Configuration creates only inert collaborators. No `@PostConstruct`, `CommandLineRunner`, scheduler, loop
  creation, loop start, server start, or automatic retry belongs in TURN-13.

## Same-Window Mode Exclusion

### `NEEDS_PARENT_DECISION` - Freeze the atomic guard contract

- The current local runner guard only knows local task state:
  `WindowTaskRunner.java:423-429` accepts a task whenever the runner is open and has no local active queue.
- The new registry only knows turn-loop uniqueness:
  `TurnLoopRegistry.java:31-45` rejects a second loop for the same `windowId`, but does not inspect local task
  activity. `WindowTurnLoop.start()` is independently public (`:59-79`). A check in only one subsystem would leave
  a check-then-start race.
- All current production calls to `MultiWindowTaskManager.submit*` are in the real
  `WindowTaskControlService` at `:144`, `:170`, `:226`, and `:262`. This makes that service the present local-side
  integration point, but the guard must run before local-team registration/other start side effects in
  `startSameQueue` (`:110-160`), not after a per-window submit has begun.

Suggested TURN-13 guard contract for the parent to freeze:

1. One in-memory, synchronized `TurnModeGuard` coordinates the check and transition for a single exact
   `windowId`; it is not a durable owner/session/ledger.
2. Local task submission is executed through one guard operation that checks no running remote loop and performs
   the actual `MultiWindowTaskManager` submit before releasing the same guard.
3. Remote loop create/start is executed through one guard operation that checks no running local task and then
   creates/starts the registry loop before releasing the same guard. On start failure, remove only the newly
   created stopped loop; do not retry.
4. The guard exposes status/check primitives now, while the actual user-facing remote start/stop wiring remains
   deferred to TURN-40. TURN-13 must not add an automatic activation path.
5. Stop/unregister behavior that chooses between local Task and remote loop remains a TURN-40 concern, because
   TURN-40 explicitly owns the final control-service and activation wiring.

## Cloud Wiring Boundary

### `READY_PRECHECK` - Route presence; final command activation remains deferred

- `CloudTurnRoutes.java:29-39` builds one exchange-backed route bundle; `:43-68` retains the typed command port
  inside the bundle. `CloudBrainServer.java:87-94` currently uses the two handlers but does not retain/expose the
  command capability to Cloud business callers.
- No source outside the Cloud turn package currently calls `CloudTurnCommandPort.execute`; this is consistent
  with the system still being inactive.
- The master plan explicitly assigns the final `CloudBrainServer.java` command wiring and explicit remote-turn
  activation to TURN-40 (`...card-plan.md:675-681`). Therefore TURN-13 should verify route compilation but must not
  expand its write set into Cloud server activation or make the command port globally reachable early.
- Parent brief should state this deferral explicitly so a TURN-13 worker does not mistake the currently dormant
  command port for missing work inside its card.

## Build Convergence

### `MATERIAL_NOT_YET_AVAILABLE`

- Current TURN-12 source is delivered but not yet resolved by the parent. A repair could still reopen Java
  writing, so the final Foundation cohort is not yet stable.
- After the parent resolves TURN-12 and the TURN-13 writer finishes the frozen write set, the allowed convergence
  is:

```powershell
# D:\mavenProject\dhxy-cloud-brain
mvn -q clean package

# D:\mavenProject\DHXY
mvn -q -DskipTests compile
```

- Run no application/server, Task, poller, UI, capture, input, or automated tests. Do not use either build while a
  Java writer is active. A single successful pair may provide the pending Foundation cohort evidence after the
  parent independently reviews TURN-13 source.

## Suggested TURN-13 Worker Brief

`READY_PRECHECK`

1. Own only new DHXY `cloud/turn/TurnClientProperties.java`, `TurnConfiguration.java`, `TurnModeGuard.java`, the
   real conditional file `window/control/WindowTaskControlService.java`, `application.properties`, and the card
   report. Do not touch Cloud source or TURN-12 files.
2. Assemble only inert Spring beans for `TurnClient`, template cache, match executor, loop factory, and loop
   registry. Do not create/start a per-window loop during bean creation.
3. Validate the dedicated turn configuration, including HTTPS/non-loopback policy and request-timeout greater
   than long-wait timeout. Keep image bytes raw multipart and template root under the existing template tree.
4. Implement the parent-frozen atomic same-window guard contract. Add the local-task guard at the real control
   service before start side effects; do not introduce a second task manager, mode owner, session, ledger,
   scheduler, polling loop, TTL, durable state, or retry.
5. Leave user-facing turn start/stop and Cloud command-port final activation to TURN-40. No startup hook and no
   runtime execution.
6. Deliver source evidence and stop for parent review. The parent, not the worker/helper, owns the two-repository
   build gate and any authoritative plan/CR/dashboard update.

