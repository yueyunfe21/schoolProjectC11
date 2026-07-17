# CR271 Full Card Plan Round 2 Runtime DAG Audit

> Role: non-binding reverse-DAG preflight helper
>
> Scope: source-only audit of the authoritative HTTPS-turn plan against the real DHXY-to-Cloud runtime chain.
> This report changes no Java, authoritative plan, CR271, migration matrix, or dashboard. All conclusions are
> `PRECHECK` findings or recommendations for the parent manager.

## 1. Audit question and result

This pass starts at the intended user-visible runtime and walks backwards through every required ownership boundary:

```text
DHXY user selects Task/window
  -> lifecycle intent in HTTPS turn request
  -> Cloud authentication -> tenant/user scope -> host
  -> concrete prototype Task construction and execution
  -> turn-native TaskExecutionContext / actionId / gateway
  -> shared CloudTurnExchange
  -> DHXY action -> outcome + optional raw PNG
  -> Cloud Task consumes result and continues
  -> pause/resume/stop/unregister reaches both ends and releases runtime state
```

**Overall PRECHECK:** the plan has a strong mechanical-turn foundation from Cloud action publication through DHXY
execution and HTTP return, but it does not yet describe a complete runnable Task DAG. The missing ownership is not
spread across the 20+ business cutover cards; it is concentrated in five integration boundaries:

1. `TurnRequest` has no Task selection or lifecycle intent.
2. A valid bearer is checked, but no authenticated principal is converted into `CloudServiceScope`, no host is
   activated, and no concrete prototype Task is constructed or run.
3. DHXY sends the raw PNG correctly, but `CloudTurnExchange` completes its command future with `TurnOutcome` only;
   Cloud business/OCR callers cannot consume the pixels.
4. The planned context/facade order is reversed: Tasks are wired in TURN-35..37 before TURN-38/39 creates the
   turn-native context/facade they need.
5. Start/stop/pause/resume/unregister are not an end-to-end lifecycle. Current DHXY control methods still target the
   local task manager, while `WindowTurnLoop.stop()` only interrupts the local transport loop.

Until those boundaries receive explicit cards, dependencies, exact write sets, and acceptance points, TURN-14..37
can replace individual callers in source but cannot collectively produce the final user-visible runtime.

## 2. Reverse-DAG coverage table

| Runtime node | Current source truth | Current plan coverage | PRECHECK |
|---|---|---|---|
| User chooses Task/window | `WindowTaskControlService.start(...)` resolves SAME_TASK/SELECTED_TASK/DETECTED_ROLE and submits only to `MultiWindowTaskManager`. | TURN-40 names the control service but only says explicit remote activation. | Partial: no card freezes how the selected Task/queue becomes a remote lifecycle request. |
| DHXY starts remote loop | `TurnModeGuard.startRemote(...)` accepts device/window/metadata only and creates a loop. | TURN-13 covers mode exclusion; TURN-40 defers activation. | Partial: no Task selection, lifecycle operation, or lifecycle acknowledgement enters the loop. |
| HTTPS request carries lifecycle | `TurnRequest` has only contract version, window metadata, wait timeout, and previous outcome. | TURN-01C froze this DTO; no later protocol-extension card exists. | Missing card, dependency, write set, and acceptance. |
| Cloud authenticates scope | `/turn` compares one static bearer string. `CloudServiceScope` needs tenant/user, but no resolver connects the two. | TURN-40 names only final `CloudBrainServer` wiring. | Missing authenticated-principal-to-scope contract and files. |
| Cloud host exists | `CloudServiceHost.create(scope, stateRoot)` is dormant and has no production call site; it cannot receive the shared command port. | TURN-40 broadly says final wiring; TURN-13H exists only as a helper recommendation, not as an authoritative card. | Missing formal card and exact construction acceptance. |
| Concrete Task is built | Three Tasks are prototype components, but host scanning covers only `com.bot.dhxy.service`; no Task factory/registry/runner call site exists. | TURN-35..37 edit Task callers only. TURN-40 does not name task construction files. | Missing runtime construction/execution card. |
| TaskExecutionContext is turn-native | Current Cloud context wraps old `CloudTaskServiceExecutionContext` and exposes old task-run scope/revision/gates. | TURN-38 removes old authority after Tasks; TURN-39 creates turn facade after TURN-38. | Dependency cycle/order defect. |
| Task creates stable actionId and waits | `CloudTurnActionFactory` requires caller-supplied IDs; `CloudTurnCommandPort` waits synchronously. No task-native gateway owns identity/correlation. | No card freezes actionId source or context-to-gateway binding. | Missing integration contract; parent decision required for stable ID derivation without a new ledger. |
| Shared exchange reaches DHXY | `CloudTurnRoutes` builds one handler and command capability over the same exchange. | TURN-02/04/05 cover the mechanical path. | Present mechanically, but `Bundle.commandPort()` is package-private and not retained/injected into a host. |
| DHXY executes and returns evidence | Executor preserves ordered mechanics, exact window, outcome metadata, and raw PNG; client sends JSON or multipart. | TURN-06/08..12 cover this. | Present through Cloud HTTP ingress. |
| Cloud Task receives raw frame | Handler constructs `CloudTurnFrame`, but exchange future/result carries only `TurnOutcome`. | TURN-02 lists frame/result files but acceptance does not require caller-visible bytes. | Concrete data-loss boundary; requires repair card before vision cutovers. |
| Task continues | No Task thread/factory/context currently consumes a completed command result. | Business cards assume this boundary without owning it. | Missing runtime runner and continuation acceptance. |
| Pause/resume/stop/unregister | DHXY methods call only local manager; loop stop interrupts transport only; Cloud has no new Task runtime lifecycle. | TURN-40 mentions stop/unregister, not pause/resume or Cloud cleanup. | Incomplete lifecycle card and acceptance. |

## 3. Exact source evidence

### 3.1 Selection never becomes lifecycle intent

- DHXY `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java:88-97` maps the user start request to
  the three existing local start methods. `:114-130` enters `TurnModeGuard.startLocal(...)`, then the real queue is
  submitted locally.
- DHXY `src/main/java/com/bot/dhxy/cloud/turn/TurnModeGuard.java:65-100` starts a remote loop with only `deviceId`,
  `windowId`, and a metadata supplier. No task code, ordered task queue, failure policy, or lifecycle operation exists.
- DHXY `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnRequest.java:3-7` contains exactly four fields:
  `contractVersion`, `window`, `waitTimeoutMs`, and `previousOutcome`.
- DHXY `src/main/java/com/bot/dhxy/cloud/turn/WindowTurnLoop.java:192-208` constructs exactly that request. The
  `stopRequested` value inside `TurnWindowMetadata` is current-window metadata, not a command; the protocol spec at
  `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md:52-57` explicitly says STOP_STATE
  is diagnostic and cannot become new business truth.
- The authoritative plan assigns TURN-40 only `WindowTaskControlService`, `TurnConfiguration`, properties, and final
  `CloudBrainServer` wiring (`...complete-migration-card-plan.md:711-717`). That write set cannot legally add or
  validate a lifecycle field in the byte-identical protocol.

### 3.2 Authentication does not construct scope, host, or Task

- Cloud `turn/CloudTurnHttpHandler.java:113-117` compares one exact bearer header. It does not produce tenant/user
  identity.
- Cloud `host/CloudServiceScope.java:3-18` requires tenant and user identity and intentionally excludes device ID.
- Cloud `CloudBrainServer.java:69-93` normalizes one token, creates one `CloudTurnExchange`, and registers handlers.
  It does not create or retain an authenticated service host.
- Cloud `host/CloudServiceHost.java:11-13` explicitly leaves authenticated activation and lifecycle ownership to a
  later layer. Its only factory at `:35-46` registers scope/storage and refreshes the service graph; repository search
  finds no production call site of `CloudServiceHost.create(...)`.
- Cloud `host/CloudServiceConfiguration.java:23-30` scans only `com.bot.dhxy.service`. The prototype Task classes are
  outside that graph: `WubeiTask.java:111-115`, `FiveRingTaskV2.java:98-102`, and `XiuluoTaskV2.java:118-122`.
- No production `TaskFactory`, Task catalog, Task runner, `getBean(Task...)`, or direct construction of those three
  Task classes exists. The old `RemoteTaskRunRoutes.java:18-24` also explicitly states that its route factory creates
  no Task executor.
- TURN-35..37 allow only each Task source and same-package DTOs (`...card-plan.md:653-672`). TURN-40 permits only
  `CloudBrainServer.java` on Cloud (`:711-717`). Neither write set owns a task catalog/factory/runner or host context
  construction.

### 3.3 Raw PNG is lost after Cloud ingress

The frame chain is valid until the exchange accepts it:

- DHXY `LocalTurnActionExecutor.java:58-97` returns an `ExecutedTurn` containing outcome plus exact PNG bytes.
- DHXY `ExecutedTurn.java:19-38` enforces that metadata and PNG are present together and defensively copies bytes.
- DHXY `WindowTurnLoop.java:198-250` retains both previous outcome and previous PNG across transport uncertainty.
- DHXY `HttpsTurnClient.java:97-123` sends raw multipart bytes, and `:220-244` validates metadata/PNG presence and
  SHA-256 correlation.
- Cloud `CloudTurnHttpHandler.java:133-155` parses multipart bytes into `CloudTurnFrame` and passes it to the exchange.
- Cloud `CloudTurnExchange.java:127-160` verifies the frame but completes a `CompletableFuture<TurnOutcome>` with
  only the validated metadata object. Its per-window state at `:309-315` stores no frame.
- Cloud `CloudTurnCommandResult.java:18-40` can carry only `TurnOutcome`; it has no `CloudTurnFrame` or PNG bytes.

Therefore a Cloud capture/OCR/template caller receives frame dimensions/hash/purpose but not pixels. TURN-18..34
and TURN-41's capture/OCR runtime evidence cannot be satisfied through the current command result. TURN-02's
acceptance (`...card-plan.md:255-263`) covers fencing and duplicate behavior but does not require caller-visible frame
bytes, so this is a plan acceptance gap as well as a source gap.

### 3.4 Context/facade order is circular

- Cloud `runner/context/TaskExecutionContext.java:8-13` imports the old `CloudGameClient`, old service context/port,
  and old task-run authorization/scope.
- Its constructor at `:26-40` can only wrap `CloudTaskServiceExecutionContext`, which its own documentation says is
  produced by the old package-internal task-run authority.
- It exposes old task-run ID/scope/epochs/revision at `:107-145`, old checkpoint authority at `:147-186`, and old
  game/service clients at `:188-203`.
- Yet TURN-35..37 first require all three Tasks to be fully wired (`...card-plan.md:653-672`). TURN-38 only afterward
  removes old retained authority (`:674-692`), and TURN-39 only after TURN-38 creates `TurnGameClient`,
  `TurnTaskServicePort`, and `TurnTaskServiceExecutionContext` (`:694-707`).

This ordering asks a Task to migrate to a facade/context that does not exist until after the Task is migrated. It
also leaves TURN-14..34 clients without an exact source for device/window/action identity. The foundation half of
TURN-39 must precede the first caller cutover; old-facade deletion can remain after all callers.

### 3.5 Shared exchange capability exists but is inaccessible to the service host

- Cloud `CloudTurnRoutes.java:29-39` correctly binds handler and command capability to the same exchange.
- `CloudTurnRoutes.Bundle` retains that command capability at `:42-55`, but its accessor at `:66-68` is
  package-private.
- `CloudBrainServer` is in the parent package and currently retains neither the bundle nor its command port.
- `CloudServiceHost.create(...)` accepts only scope and state root, so constructor-injected turn clients cannot
  receive the same exchange capability.

The existing `2026-07-15-turn-13h-command-capability-readiness-helper.md` already proposes a narrow inert repair,
but the authoritative plan has no TURN-13H node or equivalent dependency. TURN-14/15/16 currently depend directly
on TURN-13, so their declared facade clients have no constructible command-port bean.

### 3.6 Lifecycle closes only the local side

- DHXY `WindowTaskControlService.java:320-401` sends stop/pause/resume only to `MultiWindowTaskManager`; unregister at
  `:426-449` removes only local runners.
- DHXY `WindowTurnLoop.stop()` at `WindowTurnLoop.java:85-94` only sets a local flag and interrupts the long-wait or
  action thread. It does not send a Cloud Task stop intent.
- `TurnLoopRegistry.remove(...)` at `TurnLoopRegistry.java:61-69` retires/removes a stopped local loop, but there is
  no Cloud Task/host completion paired with that removal.
- TURN-40 acceptance says remote starts explicitly and stop/unregister stops it, but omits pause/resume, Cloud Task
  checkpoint behavior, Task completion, host release, start-failure cleanup across both ends, and transport
  uncertainty semantics.

## 4. Recommended corrected dependency shape

The parent can keep TURN-14..47, but the runtime foundation should be extended before TURN-14. Suggested identifiers
below are non-binding and may be renumbered.

```text
TURN-01D + TURN-12/13
  -> TURN-13L lifecycle protocol
  -> TURN-13F frame-preserving command result
  -> TURN-13H shared command capability in dormant host
  -> TURN-13C turn-native context / gateway / actionId contract
  -> TURN-13R authenticated scope + Task construction/runtime
  -> TURN-14..34 Service and caller cutovers
  -> TURN-35..37 full Task cutovers
  -> TURN-38B old-context authority removal
  -> TURN-39B old-facade removal
  -> TURN-40 final DHXY lifecycle activation
  -> TURN-41 runtime evidence
```

TURN-13L, TURN-13F, and the inert portion of TURN-13H can be parallel only if their exact files remain mutually
exclusive. TURN-13C depends on the frame result and command capability. TURN-13R depends on lifecycle protocol,
turn-native context, and host command capability. No business caller should depend only on TURN-13 after this split.

## 5. Suggested cards with exact write sets

### 5.1 TURN-13L candidate: lifecycle protocol and DHXY intent handoff

**Suggested dependencies:** TURN-01D, TURN-12, TURN-13 source completion.

**Protocol write set in both repositories, byte-identical:**

- Modify `com/bot/dhxy/cloud/turn/protocol/TurnRequest.java`.
- Modify `com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`.
- Create a closed lifecycle operation enum and typed lifecycle-intent/task-selection DTO. Exact names should be
  frozen by the parent before claim.

**DHXY integration write set:**

- `src/main/java/com/bot/dhxy/cloud/turn/WindowTurnLoop.java`
- `src/main/java/com/bot/dhxy/cloud/turn/TurnLoopFactory.java`
- `src/main/java/com/bot/dhxy/cloud/turn/TurnLoopRegistry.java`
- `src/main/java/com/bot/dhxy/cloud/turn/TurnModeGuard.java`

Do not include `WindowTaskControlService.java` here if TURN-40 retains its final user-facing activation ownership.
Instead expose a typed, inert lifecycle-intent input from loop/registry that TURN-40 can call.

**Acceptance to add:** a first request can carry exact selected Task/queue plus START; subsequent requests carry no
new business command unless the user requests PAUSE/RESUME/STOP/UNREGISTER; a successful response has a defined
lifecycle acceptance meaning; transport uncertainty does not create a new Task or silently choose a retry. The
parent must decide whether V1 supports one Task only or the existing ordered `WindowTaskQueue` and failure policy.

### 5.2 TURN-13F candidate: preserve raw frame through command completion

**Suggested dependency:** TURN-02 and TURN-04.

**Cloud write set:**

- `turn/CloudTurnExchange.java`
- `turn/CloudTurnCommandResult.java`
- `turn/CloudTurnFrame.java` only if the completed-result type cannot reuse it unchanged
- one new immutable completed outcome+frame value type, if needed

**Acceptance to add:** `CloudTurnCommandPort.execute(...)` returns the exact verified PNG bytes together with the
matching `TurnOutcome`; JSON-only outcomes still carry no frame; duplicate/late/fenced semantics remain unchanged;
the command future releases frame bytes to the waiting caller and does not introduce history, timer, retry, durable
storage, or a new ledger. A capture action must be decodable by a Cloud OCR caller using only the returned command
result.

### 5.3 TURN-13H candidate: formalize the existing command-capability preflight

**Suggested dependency:** TURN-05 and TURN-13 source completion.

**Cloud write set:**

- `turn/CloudTurnRoutes.java`
- `CloudBrainServer.java`
- `host/CloudServiceHost.java`
- `host/CloudServiceConfiguration.java`

**Acceptance to add:** the exact exchange registered at `/turn` is the command port injected into the dormant host;
no second exchange is created; host construction requires the capability before context refresh; component scanning
is narrow and does not instantiate HTTP/exchange infrastructure per host; this card does not activate a host, Task,
thread, loop, or server behavior.

### 5.4 TURN-13C candidate: move the foundation half of TURN-39 before callers

**Suggested dependencies:** TURN-13F and TURN-13H.

**Cloud write set:**

- Create `turn/TurnGameClient.java`.
- Create `turn/TurnTaskServicePort.java`.
- Create `turn/TurnTaskServiceExecutionContext.java`.
- Create one narrow turn Task gateway/action identity collaborator if the three planned facades do not own that
  responsibility directly.
- Modify `com/bot/dhxy/runner/context/TaskExecutionContext.java` only enough to accept the new turn-native context.

**Acceptance to add:** exact authenticated scope plus device/window identity reaches every Task; one stable actionId
is supplied for each explicit action; the factory never generates a retry; capture/input/local-service results and
raw frame are available through typed return values; stop/pause checkpoints have a turn-native source; no old
`RemoteTaskRun*` object is required to construct a new Task context.

The parent must freeze the stable actionId rule. Current `CloudTurnActionFactory.java:13-24` intentionally requires
caller-provided IDs. The replacement must not revive the old action ledger or add a durable ID store.

### 5.5 TURN-13R candidate: authenticated scope, Task construction, and runtime continuation

**Suggested dependencies:** TURN-13L, TURN-13C, TURN-13H.

**Cloud write set:**

- Modify `turn/CloudTurnHttpHandler.java` only if authenticated principal/scope must be passed with the accepted
  request rather than resolved before handler construction.
- Modify `CloudBrainServer.java` for resolver/runtime assembly only; final external activation remains TURN-40.
- Modify `host/CloudServiceConfiguration.java` for explicit Task construction or a deliberately narrow Task scan.
- Create explicit allowlisted Task catalog/factory, lifecycle controller, and Task runner under a new narrow
  `turn/runtime` package. The card brief must name every file before claim.

**Acceptance to add:** authenticated identity, never request-body tenant/user text, selects `CloudServiceScope`; START
maps only an allowlisted task code to one prototype `GameTask`; the runner constructs a turn-native
`TaskExecutionContext`, invokes `execute(context)`, and allows the synchronous gateway to continue after each real
outcome; normal Task terminal, STOP, start failure, and unregister all release the Task and host resources; PAUSE
reaches Task checkpoints without inventing new business semantics; no startup hook, automatic Task launch, automatic
business retry, durable workflow, owner/session/ledger, or TTL is added.

The current static bearer token does not identify tenant/user. The parent must choose and document either a
development-only configured token-to-fixed-scope resolver or the production authenticated principal source before
this card is claimable.

### 5.6 TURN-38/39/40 adjustments

- Split TURN-38 into an early context-construction slice owned by TURN-13C and a late zero-old-authority cleanup
  slice after TURN-35..37.
- Split TURN-39 into the early turn-facade construction slice owned by TURN-13C and a late old-facade removal slice.
- Keep TURN-40 small: wire `WindowTaskControlService` start/pause/resume/stop/unregister to the already-typed lifecycle
  API, preserve same-window local/remote exclusion, and close the loop only after Cloud accepts terminal lifecycle.
- Expand TURN-40's exact Cloud files beyond `CloudBrainServer.java` only if final activation truly needs them; do not
  leave hidden implementation work under “final wiring.”
- Make TURN-14..37 depend on TURN-13F/H/C/R as applicable. Vision/OCR cards must at least depend on TURN-13F;
  permanent-local-Service facades must at least depend on TURN-13H/C; Task cards must depend on TURN-13R.

## 6. End-to-end acceptance checklist the plan currently lacks

The parent should add these as source/build/runtime gates to the appropriate cards:

1. A selected DHXY window and selected Task/queue produce an exact remote START intent; no local Task is submitted.
2. The accepted Cloud principal maps to one explicit tenant/user scope; device/window remains execution identity, not
   private-memory ownership.
3. START constructs exactly one allowlisted prototype Task with a turn-native context; duplicate transport does not
   create a second Task.
4. The Task publishes a stable actionId through the same `CloudTurnExchange` that backs `/turn`.
5. DHXY executes one closed action against the exact registered window and returns typed outcome plus optional raw
   PNG without Base64 or coordinate scaling.
6. Cloud receives the raw PNG bytes, verifies correlation once, exposes them to OCR/business code, and the same Task
   thread continues from the typed result.
7. Post-action and failure evidence remain part of the same action payload/result; no extra implicit command is
   invented.
8. PAUSE and RESUME reach the Cloud Task's existing checkpoint semantics without stopping the DHXY transport loop.
9. STOP reaches the Cloud Task and then stops/retires the DHXY loop without losing a final outcome acknowledgement.
10. UNREGISTER performs STOP semantics first, then removes the exact loop/window binding and releases Cloud Task/host
    resources; start failure cleans both sides.
11. Local and remote execution never control the same window simultaneously, including start/stop/unregister races.
12. Only after these pass may TURN-42/44/45 remove old lifecycle/context/routes; their zero-reference gate must include
    the new Task construction and lifecycle paths, not only business caller references.

## 7. Parent decisions needed before plan repair

1. **Task selection payload:** single task code only, or ordered task queue plus existing failure policy.
2. **Lifecycle uncertainty:** exact acceptance/idempotence rule when START/STOP transport response is uncertain,
   without adding automatic retry, history, session, or ledger.
3. **Authenticated scope source:** configured development mapping or production principal resolver.
4. **ActionId source:** deterministic task-local action identity supplied to `CloudTurnActionFactory`, with no durable
   action ledger.
5. **Host granularity:** one authenticated user host containing per-window prototype Tasks, or another explicit
   scope; device ID must not become the private-memory key.
6. **Pause semantics:** whether PAUSE stops only Task progression while the DHXY long-wait loop stays alive, which is
   the least disruptive interpretation of the existing turn transport.

## 8. Scope and validation statement

This is a read-only PRECHECK. No Java, authoritative plan, CR271, matrix, dashboard, runtime, Task, poller, UI,
capture, input, build, test, or Git operation was performed. No business behavior difference is proposed: the
recommended cards only make the already-approved HTTPS-turn ownership path constructible and observable end to end.

