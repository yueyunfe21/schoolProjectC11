# Cloud Retained Action State and Service Port - Shared Work Log

> Append-only handoff for one external implementation worker and the local 5-minute monitor.
> Do not rewrite/delete earlier entries. Design, review, repair, build evidence, and final
> disposition must be appended here rather than left only in chat.

## Gate

- Task: establish the minimum Cloud-side retained business-action state adapter and typed
  synchronous Service facade over the existing `RemoteGameClientPort`.
- Status: `DESIGN REQUIRED`. The worker must append `External Worker - Design #1` and stop. No Java
  implementation is allowed until the local monitor appends explicit `DESIGN APPROVED`.
- Host state: dormant. Do not construct/start a Task host, poller, scheduler, UI, capture, or input.
- Build gate after design approval: Cloud `mvn -q clean package`, without skip flags. DHXY compile
  is required only if an approved design proves a local Java change unavoidable.
- Test policy: no new/restored/modified local test source.

## Frozen Inputs

- DHXY baseline: `thin-client-design@0114604e1ff5f15491d2910959c45252e893d04f`.
- Cloud baseline: `navigation-migration@3b988caa010254973e03342272e6d1d6a9685b01`.
- Both worktrees are heavily dirty. Preserve all unrelated/user work; no reset, checkout, clean,
  revert, add, commit, or push.
- `2026-07-12-cloud-task-service-context.md` is CLOSED by `Local Review #2 - APPROVED`,
  `P0/P1/P2=0`. Its exact scope/taskRun/window/stopEpoch/runRevision context, effective-vs-requested
  task identity, package reachability, and dormant-host decisions are frozen.
- The earlier execution-context/lifecycle gate is also frozen: stable retained request/action IDs,
  immutable request redelivery, digest-covered runRevision, broker-trusted outcomes, Cloud
  enqueue/final-dispatch fences, DHXY pre-side-effect/worker-admission fences, strict payload before
  ledger, and package-internal client ingress.

## Existing Authority Surface

- `CloudTaskRunAuthorityAssembly` owns exactly one coordinator, `CloudTaskRunActionLedger`,
  `CloudTaskRunExecutionGate`, and ledger-bound `CloudTaskRunCommandExecutor`.
- `CloudTaskRunActionLedger.acquire(context, operation, businessActionKey)` is package-private,
  synchronized, hard-capped, and retains one identity per `taskRunId + canonical key`.
- `CloudTaskRunCommandExecutor` is the only path that records a broker-returned typed outcome into
  the ledger. `UNKNOWN`, `STOPPED`, `EXECUTED`, and `OBSERVED` never unlock a new attempt; only a
  verified `NOT_EXECUTED` can reach `renewAfterNotExecuted`.
- `CloudTaskServiceExecutionContext` has package-private access to the same run context, command
  port, and ledger, but migrated business/host code currently receives none of those raw objects.

## Required Design Invariants

1. Stable identity remains ledger-owned. No Service, Task, host, Spring bean, or public API may
   construct/request arbitrary requestId/actionId/captureId or select a second ledger.
2. Do not expose a public free-form `String businessActionKey -> acquire/new identity` API. The
   design must show how canonical action keys originate from retained Task/phase/action state and
   how an opaque non-mintable handle reaches a migrated Service.
3. An action handle must remain bound to the exact taskRun/context and one `RemoteOperation`.
   Cross-run, cross-operation, foreign-ledger, superseded-attempt, or stale-context use must fail
   closed before a mechanical request is accepted.
4. Service-facing operations must be typed synchronous methods for `WINDOW_FACT`, `CAPTURE`, and
   indivisible `EXECUTE_INPUT_BUNDLE`. Business code may receive typed outcomes, but not raw
   `RemoteRequest`, broker, ledger, client-ingress poll, or outcome-completion capabilities.
5. Exact redelivery semantics are immutable: a repeated in-flight/`UNKNOWN` attempt reuses the
   original bound request bytes and IDs. Changed payload/timeout/revision under one attempt is
   rejected. The adapter/facade must not auto retry, rebuild, reorder, or reinterpret outcomes.
6. A replacement attempt is possible only after the existing executor recorded an exact verified
   `NOT_EXECUTED`. The design must state which trusted Task-state boundary requests renewal and why
   ordinary Service code cannot use renewal to invent retry/fallback behavior.
7. Preserve coordinator revision staleness, ledger synchronization/capacity/no-eviction policy,
   one authority assembly, and package-internal client-ingress separation. No TTL, takeover,
   cleanup, fallback, or second identity path.
8. This slice must not migrate a business Service/Task or change phase/retry/fallback/OCR/template/
   click/navigation/sleep/stop/pause semantics. It may only create the retained-state and typed
   mechanical-call boundary needed by later migration slices.

## Design #1 Required Contents

- Exact proposed Java files/types and whether each is new or modified.
- Caller/authority graph from future persisted Task action state to opaque handle to typed Service
  call to existing port/executor/broker/local fences.
- Public/package API table proving who can declare a business action, obtain a handle, invoke a
  typed call, redeliver, and request `NOT_EXECUTED` renewal.
- State machine for one action attempt: acquired -> request-bound -> outcome state -> same-byte
  redelivery or verified-NOT_EXECUTED renewal; include concurrency and stale-context behavior.
- At least three concrete future call examples (one fact, one capture, one atomic input bundle)
  showing canonical key ownership without exposing a public arbitrary key factory.
- Exact deferred gaps and an explicit statement if safe implementation requires broader Task-state
  persistence/activation work. Do not improvise that broader work in this slice.
- Threat review: forged handles, cross-context use, operation mismatch, changed params on redelivery,
  caller-fabricated outcome, duplicate concurrency, capacity exhaustion, lifecycle transition.

## Acceptance Evidence After Design Approval

- Exact changed-file list and source reachability scan.
- `javap` proof for public/package construction and capability surfaces.
- Fresh Cloud `mvn -q clean package` and exact Surefire totals.
- No `src/test` change; no DHXY Java unless separately approved; no host/poller/UI/capture/input
  execution and no Git mutation.
- Final worker entry must be `READY FOR LOCAL REVIEW` with no known P0/P1/P2 or explicit `BLOCKED`
  with severity/evidence/impact/repair condition.

## Next Writer

The one existing external worker owns Design #1. It must read this entire log and the frozen
authority sources, append its design here, and then wait for the local monitor's explicit design
review. A chat-only response is not review material.

## External Worker - Claimed (2026-07-12)

- 已领取本切片，当前仅进行 retained business-action state 与 typed Service port 的只读设计核查。
- Cloud/DHXY Java、测试及其它文档尚未修改；Maven 与 host/poller/UI/capture/input 均未运行。

## External Worker - Design #1 (2026-07-12)

### Baseline and selected approach

- Read-only baseline is complete: `AGENTS.md`, full `docs/DHXY_CONTEXT.md`, the CR271 top of
  `docs/ACTIVE_WORK.md`, this complete log, both dirty worktree statuses, the current ledger/gate/
  executor/port/context/assembly sources, and both frozen execution/context review logs.
- Rejected approach A: expose the existing ledger identity plus `RemoteGameClientPort` directly.
  That would let business code see raw identity/context and would leave renewal/request builders in
  the public capability graph.
- Rejected approach B: expose a public `String businessActionKey` registry or derive a key from
  payload bytes. The former permits semantic key hopping after `UNKNOWN`; the latter confuses one
  business action with its mechanical parameters and can turn a parameter change into a new action.
- Selected approach: one package-internal retained Task-action state owns structured action
  addresses and issues operation-specific opaque handles; one public, non-constructible, per-context
  typed Service facade accepts those handles. Existing ledger/gate/executor/broker remain the wire
  authority. No caller is wired and the host remains dormant.

### Design invariants

1. `CloudTaskRunActionLedger` remains the only wire-ID/request/outcome authority. The new retained
   state does not duplicate requestId/actionId/captureId, request bytes, or outcome state.
2. A business action is addressed by the trusted persisted Task state as the structured tuple
   `(phaseCode, actionSlot, occurrence)`. `occurrence` is the persisted ordinal of a genuinely new
   semantic action, not a transport attempt/retry counter. The adapter length-prefix encodes the
   tuple into the ledger key to avoid delimiter collisions. It never accepts or exposes one
   free-form public key string.
3. Only the package-internal future Task-state/activation adapter may create an action address,
   declare an action, or request renewal. A migrated Task/Service receives only an opaque final
   operation-specific handle. Handles expose no key, ledger identity, request/action/capture ID,
   raw request, or renewal method.
4. Every attempt is bound to one exact non-mintable `CloudTaskRunExecutionContext` object and one
   `RemoteOperation`. Initial acquire records that context. Moving an existing semantic action to a
   replacement context/revision is possible only through verified-`NOT_EXECUTED` renewal; ordinary
   acquire under a different context is rejected.
5. The public typed facade is itself bound to one exact context and the same retained state. It
   rejects foreign-state, foreign-context, wrong-operation, and superseded handles before invoking
   the internal port. The existing gate then revalidates context and ledger ownership again.
6. The first typed call binds the complete digested immutable request. Repeating the same call with
   the same handle and identical parameters returns/submits the original bound request object.
   Changed fact kind, region, format, purpose, description, action list, timeout, or runRevision
   under one attempt is rejected; the facade never repairs or rebuilds it automatically.
7. The facade never retries, renews, reorders, interprets an outcome, or selects fallback. It returns
   the existing typed outcome unchanged. `UNKNOWN`/in-flight redelivery happens only when the
   existing business flow explicitly invokes the same typed method with the same handle/arguments.
8. Renewal accepts no caller-supplied outcome. The trusted Task-state boundary asks the retained
   state to renew; the ledger itself must already contain the exact executor-recorded
   `NOT_EXECUTED`. `UNKNOWN`, `STOPPED`, `EXECUTED`, `OBSERVED`, unrecorded, foreign, and superseded
   attempts remain non-renewable.
9. Calls and renewal for one semantic action serialize on that action's internal record. Independent
   actions may proceed concurrently. This prevents renewal from racing an in-flight/redelivery call
   while preserving broker/local input serialization.
10. No lifecycle, pause, stop, retry, fallback, OCR, capture policy, click/navigation order, sleep,
    or success condition changes. No TTL, eviction, takeover, cleanup, second ledger, or alternate
    identity path is introduced.

### Exact proposed Java files and types

1. **New** `remote/CloudTaskRetainedActionState.java` (package-private final).
   - Bound to one taskRun, the exact authority assembly's gate/ledger, and retained across context
     rehydration by the future trusted Task-state adapter.
   - Contains package-private structured `ActionAddress(phaseCode, actionSlot, occurrence)` and one
     internal per-action record/lock.
   - Package-private typed declarations `retainWindowFact`, `retainCapture`, and
     `retainInputBundle` call the existing ledger `acquire` and return the same opaque current
     handle for the same address.
   - Package-private typed `renewAfterNotExecuted` overloads validate current handle, current/new
     exact context, and then delegate to ledger renewal. They accept no outcome DTO or state enum.
2. **New** `remote/CloudTaskServicePort.java` (public final, package-private constructor).
   - Owns one exact run context, one `CloudTaskRetainedActionState`, and the internal
     `RemoteGameClientPort` implemented by the existing executor.
   - Public synchronous methods: `readWindowFact(WindowFactAction, ...)`,
     `capture(CaptureAction, ...)`, and
     `executeInputBundle(InputBundleAction, ...)`.
   - Contains three public final opaque nested handle types. Their constructors and all authority
     accessors are package-private; public callers can only carry them and pass them to the matching
     typed method.
3. **Modify** `remote/CloudTaskRunActionLedger.java`.
   - Narrow the ledger, retained identity, `retainedRequest`, and renewal surface to package scope.
   - Bind each retained attempt to its exact execution-context object in addition to taskRunId and
     operation. Existing-key acquire under another context fails.
   - Renewal takes the replacement exact context, keeps the same semantic key, and changes attempt
     context only after the old attempt's verified recorded state is `NOT_EXECUTED`.
4. **Modify** `remote/CloudTaskRunExecutionGate.java`.
   - Narrow the raw request-building class/methods to package scope.
   - Require ledger ownership/current-attempt plus exact identity-context equality before building;
     all existing digest, request-byte binding, and coordinator validation behavior remains.
5. **Modify** `remote/RemoteGameClientPort.java`.
   - Make it the package-internal executor contract. It continues to take raw context/identity only
     inside the trusted remote package; `CloudTaskServicePort` becomes the sole public Service call
     surface.
6. **Modify** `remote/CloudTaskRunCommandExecutor.java`.
   - No behavior change; update its safety documentation to identify it as the internal port behind
     the typed facade. It remains the only caller of ledger `recordOutcome`.
7. **Modify** `remote/CloudTaskServiceExecutionContext.java`.
   - Replace raw `commandPort()`/`actionLedger()` exposure with a public read-only
     `servicePort()` and package-private retained-state access for the trusted runtime record.
   - Construct the typed port from the exact context and same-assembly collaborators; no public
     constructor is added.
8. **Modify** Cloud-only `com/bot/dhxy/runner/context/TaskExecutionContext.java`.
   - Add one getter for the public typed `CloudTaskServicePort`. It exposes no ledger, raw port,
     context delegate, identity, or action declaration/renewal method.
9. **Modify** `remote/CloudTaskRunAuthorityAssembly.java`.
   - Construct exactly one retained action state and typed port with the existing ledger/gate/
     executor when creating a Task/Service runtime.
   - Add a package-private nested runtime record carrying the public TaskExecutionContext plus the
     package-private retained state for the future trusted activation adapter. Preserve the existing
     process one-assembly-per-coordinator claim and dormant state.

No DHXY Java, test source, route, broker ingress endpoint, host/configuration, Spring registration,
or business Task/Service file belongs to this design.

### Caller and authority graph

```text
future persisted Task phase/action state (deferred activation slice)
  -> trusted remote-package activation adapter
       -> ActionAddress(phaseCode, actionSlot, occurrence)
       -> CloudTaskRetainedActionState.retain<Operation>(exact context, address)
            -> same assembly CloudTaskRunActionLedger.acquire(...)
            -> opaque non-mintable typed handle
       -> migrated Task passes handle to migrated Service
            -> TaskExecutionContext.getRemoteGameClient()
            -> CloudTaskServicePort typed synchronous call
                 -> exact state/context/current-handle check + per-action serialization
                 -> package-internal RemoteGameClientPort
                 -> CloudTaskRunCommandExecutor
                 -> CloudTaskRunExecutionGate builds/binds original immutable request
                 -> RemoteGameCommandBroker enqueue revision fence
                 -> package-private client-ingress route -> authenticated local client
                 -> DHXY strict payload + pre-side-effect revision fence
                 -> input only: one-shot worker-admission revision fence
                 -> broker correlated outcome
                 -> executor records exact returned outcome in ledger
                 -> unchanged typed outcome returned to Service

verified NOT_EXECUTED only:
persisted Task-state transition (not Service)
  -> retained state renewAfterNotExecuted(old handle, exact replacement context)
  -> ledger verifies recorded NOT_EXECUTED and supersedes old attempt
  -> new opaque handle; old handle permanently fails current-attempt checks
```

### Public/package capability table

| Actor/type | Declare action/address | Obtain handle | Typed call | Same-byte redelivery | Request renewal | Raw authority |
|---|---|---|---|---|---|---|
| Migrated Service / Task | No | Receives opaque typed handle as argument | Yes, through public non-constructible `CloudTaskServicePort` | Only same method + same handle + same parameters; no loop/auto retry | No | None |
| Cloud `TaskExecutionContext` | No | No minting; only carries typed port | Delegates typed port getter | No independent behavior | No | None |
| Future trusted Task-state adapter in `remote` package | Builds structured address from persisted phase/action/occurrence | Yes, through package-private retained state | May pass handle onward; does not use raw requests | Coordinates only an already existing baseline decision | Yes, but ledger must already record verified `NOT_EXECUTED` | Retained-state capability only; no broker ingress |
| `CloudTaskRetainedActionState` | Package-private structured API | Creates opaque wrapper over ledger identity | Serializes/validates handle use | Preserves current wrapper/attempt | Package-private, no outcome input | No raw request/outcome mutation |
| `CloudTaskServicePort` | No | No | Public typed fact/capture/input methods | Relies on ledger-bound original request | No | Holds internal port privately only |
| Internal `RemoteGameClientPort` / executor | No | Consumes current internal identity | Package-private raw-context call | Gate returns retained request | No | Executor alone records broker-returned outcome |
| Ledger / gate / broker / ingress endpoints | No public API | Package-internal only | Package-internal only | Authoritative request/digest/outcome state | Ledger package-private only | Never exposed to host/business packages |
| Cloud host / Spring configuration | No | No | No current runtime caller | No | No | No assembly/ledger/broker poll/completion capability |

### One-attempt state machine and concurrency

```text
DECLARED/ACQUIRED attempt N
  - exact taskRun context + operation + structured semantic address fixed
  - stable requestId/actionId/(captureId) minted once by ledger
  -> first typed call after context validation

REQUEST_BOUND
  - complete digested immutable request retained
  - same digest returns ORIGINAL request object
  - changed payload/timeout/revision is rejected
  -> executor invokes broker (no adapter retry)

OUTCOME
  - unrecorded/port exception: no renewal; same handle+same params is the only redelivery path
  - UNKNOWN: same-byte redelivery only; no replacement identity
  - OBSERVED / EXECUTED / STOPPED: terminal for this semantic action attempt; no renewal
  - NOT_EXECUTED: recorded only from executor's exact broker-returned correlated outcome
       -> ordinary Service still cannot renew
       -> trusted persisted Task-state boundary may explicitly renew

VERIFIED_NOT_EXECUTED_RENEWAL
  - old handle/current record checked under per-action lock
  - replacement exact context must be current and same taskRun
  - ledger mints attempt N+1 and supersedes N
  - old handle fails forever; N+1 starts unbound/unrecorded
```

- Duplicate declaration of one address is serialized and returns the same opaque handle. Same-handle
  concurrent calls serialize per semantic action; the later call is a redelivery, not a second
  identity. Independent addresses remain concurrent.
- Renewal uses the same per-action lock, so it cannot race an in-flight call. Two renewal requests
  cannot mint two attempts; after the first replacement, the old handle is no longer current.
- Ledger capacity exhaustion rejects a new declaration before a request/side effect. Nothing is
  evicted, cleaned, retried, or remapped; existing records remain usable.
- Lifecycle transition makes the exact context stale. Retain/call/renew revalidation fails; a bound
  old-revision request also remains fenced by Cloud enqueue/final dispatch and DHXY local gates.

### Three future typed-call examples

1. **WINDOW_FACT:** persisted Xiuluo phase state owns
   `ActionAddress("WAIT_TARGET_PATHING_TERMINAL", "READ_BINDING", 0)`. The trusted adapter obtains
   a `WindowFactAction` for `WINDOW_FACT`; a migrated Service receives it and calls
   `readWindowFact(handle, BINDING, timeoutMs)`. Repeating that exact call after `UNKNOWN` reuses the
   original bytes. Changing to `GEOMETRY` requires a different persisted action slot; it cannot
   mutate the bound attempt.
2. **CAPTURE:** persisted task-sync state owns
   `ActionAddress("TASK_PANEL_SYNC", "CAPTURE_PANEL", scanOccurrence)`. It receives a
   `CaptureAction`; the Service calls `capture(handle, window-relative region, PNG,
   CLOUD_SERVICE_INPUT, timeoutMs)`. The captureId is hidden and retained by the ledger. A changed
   region/timeout under that handle is rejected instead of silently taking a second image.
3. **Atomic EXECUTE_INPUT_BUNDLE:** persisted NPC-click phase owns
   `ActionAddress("NPC_CLICK", "PLAYER_ANCHOR_MOVE_CLICK", clickOccurrence)`. It receives an
   `InputBundleAction`; the Service submits one indivisible list
   `MOVE_MOUSE -> SLEEP -> CLICK_LEFT -> SLEEP`. No Service can obtain another actionId by changing
   a string. A baseline-approved new click/fallback must first advance persisted business state to
   a new occurrence/address; transport uncertainty alone never does so.

### Deferred gaps and activation statement

1. Persisted Task phase/action storage and its rehydration adapter do not exist in this slice. The
   boundary can be implemented safely now because it has no public declaration caller, but it must
   remain dormant. Host activation requires a separate review proving addresses/occurrences come
   from persisted Task state and cannot be advanced by a Service outcome alone.
2. Per-Task catalogs mapping existing phases/actions to stable `phaseCode/actionSlot/occurrence`
   are deferred to each Task migration and must preserve its baseline retry/fallback semantics.
3. Pause/resume context rehydration is deferred. A stale handle is never reused directly. An
   unresolved `UNKNOWN` action blocks replacement; only a ledger-verified `NOT_EXECUTED` may move
   the same semantic action to a replacement exact context.
4. Typed higher-level adapters for title/geometry facts, image decoding/OCR inputs, and local input
   DTO construction remain separate Service migration slices. This port only transports existing
   protocol types.
5. Crash durability is not added: the frozen ledger is process-local, capped, and non-evicting.
   Durable action recovery/DR requires its own approved state model; this design makes no claim
   that process restart can safely resolve an in-flight `UNKNOWN`.
6. Task checkpoint/sleep, mutable runner state, activation wiring, and production cutover remain
   explicitly out of scope.

### Threat review

| Threat | Design response |
|---|---|
| Forged handle | Final public handle types have no public constructor or authority getters and carry a private retained-state/record reference. Normal host/business code cannot mint one. |
| Cross-run/context use | Handle attempt stores exact non-mintable context; facade requires the same retained state and exact context; ledger/gate verify taskRun, context, owner and current attempt again. |
| Operation mismatch | Three operation-specific handle types give compile-time separation; retained record and gate also verify `RemoteOperation`. |
| Foreign ledger/second assembly | Ledger/gate/port constructors are package-internal and the existing assembly coordinator claim remains unique; owner reference rejects foreign identities. |
| Superseded attempt | Ledger current-identity and retained-state current-wrapper checks reject the old handle before request submission. |
| Changed parameters on redelivery | First complete request/digest is frozen; equal calls return the original request, different payload/timeout/revision fails. |
| Caller-fabricated outcome | Public facade has no outcome-record method; executor alone records the exact broker return. Renewal reads ledger state and accepts no caller outcome. |
| Duplicate concurrency | Per-action record serializes calls/renewal; ledger bind/outcome operations stay synchronized; broker request ledger still deduplicates exact requestId+digest. |
| Semantic key hopping after UNKNOWN | Service/Task/host has no declaration API or address constructor. Only future persisted Task-state adapter can advance occurrence, and that activation mapping is separately gated. |
| Capacity exhaustion | Ledger hard cap/no eviction is preserved. New declaration fails before mechanics; no cleanup, fallback, TTL or alternate ledger is introduced. |
| Lifecycle transition | Context revalidation plus existing Cloud enqueue/final-dispatch and DHXY pre-side-effect/worker-admission revision fences reject stale work; no old handle is rebound implicitly. |
| Client-ingress abuse | Existing package-private broker and private route endpoint separation is unchanged; typed Service objects expose no poll/outcome-completion capability. |

### Design disposition

The dormant boundary is implementable within the exact Cloud-only file set above without migrating
business code or broad Task persistence. Runtime use is intentionally blocked on the separate
persisted Task-state/activation slice. No known design-level P0/P1/P2 remains. **DESIGN REVIEW
REQUIRED**: no Java or Maven work may begin until the parent appends explicit `DESIGN APPROVED`;
otherwise any `BLOCKED` entry will be repaired append-only in this same log.

## Local Design Review #1 - BLOCKED (2026-07-12)

### Verdict

- **BLOCKED**. `P0=0, P1=1, P2=0`. The opaque-handle, single-ledger, exact-context, typed-facade,
  no-public-mint, no-auto-retry, and dormant-host directions are otherwise acceptable, but the
  proposed same-byte `UNKNOWN` redelivery state machine cannot converge against the current
  broker/ledger implementation. No Java or Maven work is approved yet.

### P1-1 - Same-byte UNKNOWN redelivery cannot surface or record a late final resolution

- **Evidence:** `RemoteGameCommandBroker.acceptLateResolutionLocked` stores a non-`UNKNOWN` late
  result in `PendingCommand.lateResolution`, but `dispatchAndAwait` reuses an existing identical
  request by calling `await(existing)`, and `await` immediately returns the already-completed
  `terminalOutcome` (`UNKNOWN`). No production read path consumes `lateResolution`; therefore a
  repeated typed call with the original bytes remains `UNKNOWN` even after the authenticated local
  client supplied an exact correlated final result.
- **Second fence conflict:** `CloudTaskRunCommandExecutor` records every returned outcome, while
  `CloudTaskRunActionLedger.recordOutcome` stores the first `ExecutionState`, including `UNKNOWN`,
  and rejects any later different state. Thus even if the broker were changed to return its late
  result, the ledger would reject the required `UNKNOWN -> NOT_EXECUTED/EXECUTED/OBSERVED/STOPPED`
  resolution as a conflicting second state.
- **Impact:** the Design #1 promise that an explicit same-handle/same-parameter call can redeliver
  an uncertain attempt is not implementable with the proposed file set. A real late
  `NOT_EXECUTED` can never become renewable, and a real late executed/observed result can never be
  returned through the typed facade. The retained semantic action can remain permanently stuck
  despite an authenticated, digest-correlated local outcome.

### Exact repair conditions for Design Repair #1

1. Add `RemoteGameCommandBroker.java` to the exact design scope. For an existing requestId with the
   same request digest, the broker must return the exact stored `lateResolution` when present;
   otherwise it may return the existing `UNKNOWN`. It must not redispatch automatically, mint an
   ID, rebuild bytes, or alter the existing client-ingress completion authority.
2. Amend `CloudTaskRunActionLedger.recordOutcome` semantics so an exact executor-recorded
   `UNKNOWN` remains unresolved: repeated identical `UNKNOWN` is idempotent, and one later exact
   correlated non-`UNKNOWN` state may resolve it. Once non-`UNKNOWN`, the state is immutable.
   Renewal must still require only the resulting verified `NOT_EXECUTED` state.
3. Keep `CloudTaskRunCommandExecutor` as the sole ledger outcome writer and retain all existing
   operation/requestId/actionId/taskRunId/requestDigest checks. No Service/Task/host caller may
   submit an outcome or read broker internals.
4. Update the attempt state machine, changed-file list, API table, and threat review to show the
   broker late-resolution read path and the ledger transition
   `unrecorded -> UNKNOWN -> one exact final state`; explain concurrent same-byte calls and why
   they cannot produce two final states.
5. The same external worker must append `External Worker - Design Repair #1` and stop. Do not edit
   Java, tests, or other documents and do not run Maven/host/poller/UI/capture/input before a later
   explicit `DESIGN APPROVED` entry.

## External Worker - Design Repair #1 (2026-07-12)

### P1 verification and superseding correction

- The review evidence is correct. Current broker ingress can atomically retain one authenticated,
  digest-correlated non-`UNKNOWN` `lateResolution`, but existing identical request lookup reaches
  `await(existing)` and returns the already completed `terminalOutcome=UNKNOWN`; no read path
  prefers the retained late value.
- Current ledger correlation checks are also correct but its first-state-wins rule is incomplete:
  recording the executor-returned `UNKNOWN` prevents the same executor from later recording the
  broker's exact late final result. Design #1 therefore cannot promise convergence without adding
  the broker and ledger transitions below.
- This entry supersedes only Design #1's `UNKNOWN`/outcome state-machine text. Opaque handles,
  structured Task-state keys, exact context/operation ownership, same-byte request binding,
  non-public renewal, typed Service facade, one assembly, and dormant-host constraints remain.

### Corrected exact Java file scope

1. **New** `remote/CloudTaskRetainedActionState.java`: unchanged from Design #1.
2. **New** `remote/CloudTaskServicePort.java`: unchanged from Design #1.
3. **Modify** `remote/CloudTaskRunActionLedger.java`:
   - retain exact verified outcome state plus outcome digest for the current attempt;
   - allow only `unrecorded -> UNKNOWN -> one non-UNKNOWN final`, or direct
     `unrecorded -> one non-UNKNOWN final`;
   - repeated exact same-digest `UNKNOWN`/final reports are idempotent; any different digest,
     final-to-UNKNOWN regression, or final-to-different-final transition fails closed;
   - renewal still reads only the immutable resolved state and requires verified
     `NOT_EXECUTED`.
4. **Modify** `remote/CloudTaskRunExecutionGate.java`: unchanged Design #1 narrowing and exact
   context/identity checks.
5. **Modify** `remote/RemoteGameClientPort.java`: unchanged Design #1 package-internal role.
6. **Modify** `remote/CloudTaskRunCommandExecutor.java`:
   - no new branch or retry; it remains the sole ledger outcome writer and records exactly the
     typed object returned by the broker-backed internal port;
   - it may therefore first record `UNKNOWN` and, on a later explicit same-byte call, record the
     broker-returned final late resolution under the corrected ledger transition.
7. **Modify** `remote/RemoteGameCommandBroker.java` (**added to scope by this repair**):
   - for an existing `requestId` with the same request digest, consult the exact retained
     `PendingCommand.lateResolution` and return it when present;
   - when it is absent, preserve the existing terminal result (including `UNKNOWN`); no enqueue,
     dispatch, timeout, identity, or completion-authority rule changes;
   - check late resolution both when selecting the existing pending command and after an `await`
     returns, so a value published during that read can be observed without redispatch. A late
     result that wins immediately after both reads is observed by the next explicit same-byte call.
8. **Modify** `remote/CloudTaskServiceExecutionContext.java`: unchanged Design #1 typed-port
   exposure/removal of raw collaborator access.
9. **Modify** Cloud-only `com/bot/dhxy/runner/context/TaskExecutionContext.java`: unchanged Design
   #1 typed-port getter.
10. **Modify** `remote/CloudTaskRunAuthorityAssembly.java`: unchanged Design #1 same-assembly
    retained state/typed port/runtime construction.

No DHXY Java, test, other document, route/endpoint, host/configuration, Task, or Service file is
added. In particular, client-ingress `completeOutcome` keeps its current package-private/private-
route authority and correlation checks; this repair only makes its already retained final value
readable by an explicit identical request call.

### Corrected caller/outcome graph

```text
typed Service call with opaque current handle
  -> per-action serialization + exact context/operation validation
  -> executor -> gate returns the original bound request bytes
  -> broker lookup by same scope/operation/requestId
       same digest required
       if lateResolution exists: return that exact stored typed final outcome
       else: return/await existing terminal outcome (possibly UNKNOWN)
       after await: prefer a concurrently published lateResolution if now present
  -> executor records exactly the broker-returned object
  -> ledger correlation checks operation/requestId/actionId/taskRunId/requestDigest
  -> ledger transition:
       unrecorded -> UNKNOWN
       UNKNOWN -> same UNKNOWN (exact-digest idempotent)
       UNKNOWN -> one exact non-UNKNOWN final
       unrecorded -> one exact non-UNKNOWN final
       final -> same final (exact-digest idempotent)
       every other transition rejected
  -> typed outcome returned unchanged to Service

only when resolved final == NOT_EXECUTED:
trusted persisted Task-state boundary -> renew current attempt
```

There is no broker-initiated callback, polling loop, auto resend, or automatic renewal. A Service
sees the late final only by making the baseline-authorized explicit call again with the same opaque
handle and exactly the same parameters; the retained request object and all IDs remain unchanged.

### Corrected API/capability table delta

| Actor/type | Read late resolution | Write ledger outcome | Resolve UNKNOWN | Renew |
|---|---|---|---|---|
| Migrated Task/Service | No broker access; receives only the typed return of its explicit same-byte call | No | Cannot submit/choose a final state | No |
| `CloudTaskServicePort` | Indirectly through internal executor/broker only | No | No interpretation; returns exact typed result | No |
| `CloudTaskRunCommandExecutor` | Receives the exact broker-selected terminal/late result | Sole package-internal writer | Records exact correlated broker return | No |
| `RemoteGameCommandBroker` | Reads its own atomic `lateResolution`; client ingress remains the only writer | No ledger access | One late final is selected by existing CAS/digest conflict rules | No |
| `CloudTaskRunActionLedger` | No broker access | Accepts only executor call | Allows verified `UNKNOWN -> one final`; prevents rollback/conflict | Package-private verified-`NOT_EXECUTED` only |
| Trusted persisted Task-state adapter | No broker/ledger outcome mutation | No | Observes typed state only after executor recorded it | May request renewal; cannot supply the state |
| Host/Spring/client-ingress caller | No Service-side broker object or ledger | No | Authenticated ingress may submit correlated outcome only through existing private route | No |

All Design #1 declaration/handle/raw-authority permissions remain unchanged: public business code
still has no action-address constructor, acquire, ledger, raw request, poll, completion, or renewal
capability.

### Corrected attempt state machine

```text
ACQUIRED N
  -> first typed call binds one immutable request
REQUEST_BOUND / IN_FLIGHT
  -> broker returns exact final: RESOLVED_FINAL
  -> uncertainty timeout/loss: UNRESOLVED_UNKNOWN

UNRESOLVED_UNKNOWN
  - identity and original request remain frozen
  - renewal forbidden
  - explicit same-handle/same-parameter call performs broker lookup only
      no late resolution yet -> returns same UNKNOWN
      exact late resolution retained -> returns that final outcome
  - executor records UNKNOWN -> one exact final resolution

RESOLVED_FINAL
  - OBSERVED / EXECUTED / STOPPED: immutable, non-renewable
  - NOT_EXECUTED: immutable; only trusted Task-state boundary may renew
  - same exact outcome re-record is idempotent
  - UNKNOWN regression or conflicting final is rejected

VERIFIED_NOT_EXECUTED_RENEWAL
  -> attempt N+1, old handle superseded, new exact context allowed as already designed
```

### Concurrency and finality analysis

- `PendingCommand.lateResolution` already uses one atomic winner. The first non-`UNKNOWN` late
  outcome is retained; the same outcome digest is duplicate/idempotent and a different digest is
  rejected. Broker redelivery never creates a second final result.
- Multiple explicit same-byte callers share one request-ledger entry. Before publication they may
  all observe the same `UNKNOWN`; after publication they all converge on the exact stored late
  outcome. A call racing publication may return `UNKNOWN`, but a later explicit call converges; no
  call redispatches or mints anything.
- Design #1's per-semantic-action record serializes public typed calls and renewal, so one Service
  action cannot concurrently record final then stale UNKNOWN through the supported facade. The
  ledger independently rejects final-to-UNKNOWN even if an internal misuse races.
- Ledger outcome recording remains synchronized. If duplicate executor records carry the exact
  same outcome digest they are idempotent; only the first exact non-`UNKNOWN` value may replace
  `UNKNOWN`. No two final states can commit.
- Renewal takes the same per-action lock and ledger lock after final recording. It cannot occur
  while the call is in flight and cannot observe `UNKNOWN`; only committed `NOT_EXECUTED` permits
  one replacement, after which the old identity/handle is superseded.

### Updated threat review delta

| Threat | Repair response |
|---|---|
| Permanent UNKNOWN despite authenticated late final | Existing-request broker path now prefers its exact stored `lateResolution`; explicit same-byte calls can converge without redispatch. |
| Fabricated late final from Service/host | No broker/completeOutcome capability is exposed by the typed facade. Existing authenticated client-ingress and full correlation/digest checks remain the sole late-resolution writer. |
| UNKNOWN rollback after final | Ledger rejects final-to-UNKNOWN; public typed-call serialization prevents supported callers from reordering one semantic action. |
| Two conflicting late finals | Broker atomic late-resolution winner plus outcome-digest conflict rejection admits at most one; ledger also admits at most one final digest. |
| Different payload used to probe late result | Broker reuse requires same requestId+requestDigest, while ledger request binding rejects changed payload/timeout/revision before broker lookup. |
| Late NOT_EXECUTED used as automatic retry | Broker only returns the stored result; executor only records it. No facade renewal exists. The trusted Task-state boundary must separately request renewal under existing business semantics. |
| Late EXECUTED/OBSERVED/STOPPED reminted | These become immutable resolved finals and remain non-renewable. |
| Lifecycle transition during late convergence | The old request stays bound to its old revision and is not redispatched. Reading an already correlated late result does not cause a mechanical action; every new attempt/context still passes the existing revision gates. |

All other Design #1 threat controls remain unchanged, including opaque-handle non-forgeability,
cross-context/operation/ledger rejection, semantic-key ownership, capacity/no-eviction, and private
client-ingress separation.

### Repair disposition

The five Design Review #1 repair conditions are covered by this append-only correction. The exact
implementation scope is now ten Cloud files, with `RemoteGameCommandBroker.java` added solely for
late-resolution reads and `CloudTaskRunActionLedger.java` amended for unresolved-to-final
convergence. No Java/Maven work is authorized by this entry. **DESIGN REVIEW REQUIRED**; stop and
wait for an explicit parent `DESIGN APPROVED` or a later `BLOCKED` entry.

## External Worker - Design #1 (design only; no Java/Maven/Git in this entry)

### 0. Disclosure (process order)

Before this design was appended, the working tree already contains implementation of this slice's
Service facade. Files present now under `com.yueyunfe.dhxy.cloudbrain.remote`:
`RemoteGameClientPort` (redefined as the typed, handle-based Service port), `CloudTaskRunCommandExecutor`
(implements it), `CloudTaskServiceExecutionContext`, `CloudTaskServiceMetadata`, and
`CloudTaskRunAuthorityAssembly` (now package-private, broker-wired, with `createTaskExecutionContext`
bridging to the migrated DHXY `com.bot.dhxy.runner.context.TaskExecutionContext`). This precedes any
`DESIGN APPROVED` and is a disclosed deviation from the `DESIGN REQUIRED` gate. The code was marked
intentional by the owner, so it is neither reverted nor built here; this design documents the actual
surface and specifies the one remaining piece. No Java, Maven, or Git action was taken for this entry.

### 1. Proposed Java files/types (present vs remaining)

| Type | State | Role |
|---|---|---|
| `RemoteGameClientPort` | present (redefined) | typed Service port: `readWindowFact/capture/executeInputBundle(context, ..., RetainedActionIdentity, timeoutMs)`. No raw-request or scope+request method remains. |
| `CloudTaskRunCommandExecutor` | present | sole `RemoteGameClientPort` impl; builds via gate, submits via package-private broker, records broker outcome into ledger. `final`, assembly-only construction. |
| `CloudTaskRunActionLedger` | present (frozen) | retained identity/bytes owner; `acquire` package-private; `recordOutcome`/renewal package-private; owner-ref + current-attempt checks. |
| `CloudTaskRunExecutionGate` | present (frozen) | context mint + per-send revalidate + bind-or-verify bytes. Package-private ctor. |
| `CloudTaskServiceExecutionContext` | present | public run-identity view + `revalidate()`; package-private `runContext()/commandPort()/actionLedger()` reserved for the adapter. |
| `CloudTaskServiceMetadata` | present | public powerless record; `taskCode` must equal coordinator `taskType`. |
| `CloudTaskRunAuthorityAssembly` | present | single authority assembly; one-per-coordinator claim; `createTaskExecutionContext(scope, taskRunId, metadata)`. |
| **`CloudTaskRunActionScope`** (name TBD) | **REMAINING / to design** | the retained-action-state adapter: derives a canonical business-action key from retained Task/phase/action identity, calls package-private `ledger.acquire`, and hands a migrated Service an opaque handle bound to one op, plus the typed `RemoteGameClientPort`. This is the only piece that closes invariants 2 and 4 end-to-end. |

### 2. Caller / authority graph (future, once the adapter is approved)

```
retained Task/phase/action state (trusted, in-package activation boundary)
  -> CloudTaskRunActionScope.declare(phaseActionId, RemoteOperation)   // canonical key derived here
       -> ledger.acquire(runContext, op, canonicalKey)                 // package-private, one id per key
       -> returns opaque RetainedActionIdentity (non-mintable handle)
  -> migrated Service holds { CloudTaskServiceExecutionContext, handle }
       -> port.readWindowFact/capture/executeInputBundle(context, ..., handle, timeoutMs)
            -> executor: gate.newXRequest (revalidate context + bind-or-verify bytes)
                 -> broker.<op>(scope, request)   // package-private ingress
                      -> DHXY local fences (pre-side-effect + worker-admission revision, exact-window)
                 -> typed outcome
            -> ledger.recordOutcome(handle, outcome)   // executor-only
       -> Service receives typed outcome only
  renewal: only the trusted Task-state boundary, only after a recorded verified NOT_EXECUTED
```

Services never touch broker, ledger, gate, raw `RemoteRequest`, client-ingress poll, or
outcome-completion. They receive a context, opaque handles, and typed outcomes.

### 3. Public / package capability table

| Capability | Surface | Reachable by |
|---|---|---|
| Declare a business action (mint key to handle) | `ledger.acquire` (pkg-private) via `CloudTaskRunActionScope` (pkg-internal adapter) | protocol package only; no public free-form `String key` factory |
| Obtain an opaque handle | adapter return value | migrated Service (given by adapter, cannot self-mint) |
| Invoke a typed mechanical call | `RemoteGameClientPort` (public) | any holder of context + handle; still gated by revalidate |
| Exact redelivery | re-call with same handle -> gate returns bound bytes; broker dedupes reqId+digest | Service (safe: identical bytes only) |
| Request NOT_EXECUTED renewal | `ledger.renewAfterNotExecuted` (pkg-private) | trusted Task-state boundary only; not Service code |
| Record outcome | `ledger.recordOutcome` (pkg-private) | `CloudTaskRunCommandExecutor` only |
| Construct authority | `CloudTaskRunAuthorityAssembly.create` (pkg-private) | protocol package only, one per coordinator |

### 4. Single-attempt state machine

```
ACQUIRED (key->ids, no bytes)
  --gate build--> REQUEST_BOUND (immutable request + digest frozen)
     --executor broker call--> OUTCOME_RECORDED{ NOT_EXECUTED | OBSERVED | EXECUTED | STOPPED | UNKNOWN }
        - any state: re-call with same handle == redelivery -> gate returns the bound request object,
          broker returns the retained terminal outcome, recordOutcome is idempotent (same state).
        - NOT_EXECUTED only: renewAfterNotExecuted -> new attempt (new requestId; new actionId only for
          EXECUTE_INPUT_BUNDLE), cleared bytes -> back to ACQUIRED.
        - UNKNOWN/STOPPED/EXECUTED/OBSERVED: renewal throws; no new id.
```

- Concurrency: ledger is `synchronized`; `bindOrVerifyRequest`, `recordOutcome`, `acquire`, renewal
  are mutually exclusive; identical re-record is idempotent, divergent re-record throws.
- Stale context: every build calls `gate.validate` -> `coordinator.authorize(..., runRevision)`; a paused/
  resumed/stopped/re-confirmed run (revision moved) is denied before any request is accepted, and the
  DHXY pre-side-effect + one-shot worker-admission fences reject a stale command independently.

### 5. Three concrete future call examples (canonical key ownership, no public key factory)

```
// A. window fact - phase-owned key
var h = actionScope.declare(PhaseAction.of("xiuluo:phase3:readBinding"), WINDOW_FACT);
WindowFactOutcome o = port.readWindowFact(ctx, WindowFactKind.BINDING, h, 3000);

// B. capture - phase-owned key
var h = actionScope.declare(PhaseAction.of("xiuluo:phase3:captureRadar"), CAPTURE);
CaptureOutcome o = port.capture(ctx, radarRegion, PNG, CLOUD_SERVICE_INPUT, h, 3000);

// C. atomic input bundle - action-owned key
var h = actionScope.declare(PhaseAction.of("xiuluo:phase3:clickNpc#1"), EXECUTE_INPUT_BUNDLE);
InputBundleOutcome o = port.executeInputBundle(ctx, "moveClickNpc", SCREEN_ABSOLUTE_PX, actions, h, 3000);
```

The key string is derived by `CloudTaskRunActionScope` from retained phase/action identity; the
Service never calls `acquire` and never fabricates a raw key.

### 6. Deferred gaps (explicit)

- The `CloudTaskRunActionScope` adapter and the retained Task/phase/action state it reads are NOT
  implemented and are the actual remaining work of this slice. A fully safe end-to-end path also needs
  the broader Task-state persistence + activation boundary (who rehydrates a run, assigns effective
  vs requested task, and requests renewal). Per the gate I do NOT improvise that here.
- Until the adapter is reviewed, `CloudTaskServiceExecutionContext` keeps `commandPort()/actionLedger()`
  package-private, so no migrated Service can yet obtain a handle - intentional dormancy.

### 7. Threat review

| Threat | Defense |
|---|---|
| Forged / foreign-ledger handle | `RetainedActionIdentity.owner` ref-equality + current-attempt identity in `requireOwnedCurrent` |
| Cross-run / cross-context use | gate checks `handle.taskRunId == context.taskRunId`; `revalidate` re-authorizes exact scope/window/epoch/revision |
| Operation mismatch | handle carries one `RemoteOperation`; builder rejects a different op |
| Changed params on redelivery | `bindOrVerifyRequest` returns original bytes for equal digest, throws on divergent |
| Caller-fabricated outcome | `recordOutcome` package-private, executor-only, records the broker-returned object |
| Duplicate concurrency | synchronized ledger; idempotent same-state re-record; divergent state throws |
| Capacity exhaustion | hard cap, no eviction; `acquire` throws when full (fail-closed, no silent reuse) |
| Lifecycle transition mid-flight | coordinator revision staleness + DHXY pre-side-effect + one-shot worker-admission fences |

### 8. Disposition

This entry is **design only**. No Java/Maven/Git was performed. Per the gate I stop here and wait for
the local monitor's explicit `DESIGN APPROVED` (or `BLOCKED` with conditions) before any
implementation of `CloudTaskRunActionScope` or any build. The worker keeps a 5-minute monitor on this
file.

## Local Process Note #1 - Latest Gate Remains BLOCKED (2026-07-12)

- The duplicate `External Worker - Design #1` immediately above is stale/non-responsive material:
  it was appended after `Local Design Review #1 - BLOCKED`, repeats the already-rejected assumption
  that broker dedupe returns a usable late final outcome, and does not address any of the five
  required repair conditions.
- It is **not** `Design Repair #1`, does not supersede the P1 finding, and grants no permission to
  edit Java or run Maven. The latest valid gate remains `BLOCKED`, `P0=0, P1=1, P2=0`.
- The next valid external entry must be from the same assigned worker and must be titled
  `External Worker - Design Repair #1`, explicitly covering broker late-resolution retrieval and
  the ledger transition `unrecorded -> UNKNOWN -> one exact final state`, then stopping for review.

## External Worker - Retraction of duplicate Design #1 entry (2026-07-12)

The entry titled "External Worker - Design #1 (design only; no Java/Maven/Git in this entry)" that
appears immediately above was appended in error from a stale 106-line read of this log, before I saw
the real `External Worker - Design #1` (the `ActionAddress` / `CloudTaskRetainedActionState` /
`CloudTaskServicePort` design) and its `Local Design Review #1 - BLOCKED`. That duplicate proposed a
`CloudTaskRunActionScope` adapter and a public `RemoteGameClientPort`, which contradicts the accepted
Design #1 direction (narrow `RemoteGameClientPort` to package-internal; public typed facade is
`CloudTaskServicePort` with opaque handles). **Please ignore that duplicate entry.** Per the
append-only rule it is not deleted; it is superseded by this retraction and by Design Repair #1 below.
No Java/Maven/Git was performed for it.

## External Worker - Design Repair #1 (2026-07-12; design only; no Java/Maven/Git)

Addresses `Local Design Review #1 - BLOCKED` P1-1 (same-byte `UNKNOWN` redelivery cannot surface or
record a late final resolution). This repair keeps every accepted direction of the real Design #1 and
changes only the redelivery/late-resolution semantics per the five exact conditions.

### R-1. Broker late-resolution read path (condition 1)

- Add `remote/RemoteGameCommandBroker.java` to the design scope (Modify).
- In `dispatchAndAwait`, when an existing pending command is matched by identical
  `(route, operation, requestId)` and identical `requestDigest`, and its `terminalOutcome` is already
  `UNKNOWN`, the broker must return the stored `PendingCommand.lateResolution` when present; otherwise
  it returns the existing `UNKNOWN`. This is a pure read of the already-authenticated, digest-correlated
  completion the local client supplied via `completeOutcome` / `acceptLateResolutionLocked`.
- Explicitly unchanged: no automatic redispatch, no new requestId/actionId, no request-byte rebuild,
  no change to `completeOutcome` client-ingress completion authority, and no new public method. The
  read stays package-internal behind the executor. A digest mismatch still returns
  `IDEMPOTENCY_CONFLICT` exactly as today.

### R-2. Ledger `UNKNOWN -> one exact final state` transition (condition 2)

- Amend `CloudTaskRunActionLedger.recordOutcome` (still package-private, still executor-only):
  - `unrecorded -> UNKNOWN`: record `UNKNOWN` (attempt remains unresolved/in-flight, not terminal).
  - `UNKNOWN -> UNKNOWN` (same exact correlated outcome): idempotent no-op.
  - `UNKNOWN -> one exact correlated non-UNKNOWN` (`NOT_EXECUTED` / `EXECUTED` / `OBSERVED` / `STOPPED`):
    single permitted resolution; the correlated outcome must still match operation, requestId,
    actionId, taskRunId, and the bound requestDigest.
  - once non-UNKNOWN: immutable; any different later state throws; identical re-record idempotent.
  - `unrecorded -> non-UNKNOWN`: unchanged (direct terminal record).
- Renewal is still gated solely on the resulting verified `NOT_EXECUTED`. `UNKNOWN` (unresolved),
  `EXECUTED`, `OBSERVED`, `STOPPED`, unrecorded, foreign, and superseded attempts remain non-renewable.
  A never-resolved `UNKNOWN` therefore stays permanently non-renewable and non-executable (fail-closed),
  but a real late `NOT_EXECUTED` can now resolve and become renewable, and a real late
  executed/observed result can now be returned through the facade.

### R-3. Sole outcome writer preserved (condition 3)

- `CloudTaskRunCommandExecutor` remains the only caller of `recordOutcome`: each typed call performs
  gate build (revalidate context + bind-or-verify bytes) -> internal broker submit (which now returns
  the late resolution when present) -> `recordOutcome(handle, returnedOutcome)`. All existing
  operation/requestId/actionId/taskRunId/requestDigest checks are retained. No Service/Task/host caller
  may submit an outcome, read `lateResolution`, or touch broker internals.

### R-4. Updated attempt state machine (condition 4)

```text
unrecorded
  --gate build--> request-bound (immutable request + digest frozen)
     --executor broker submit-->
        broker terminal == UNKNOWN and no lateResolution -> record UNKNOWN (unresolved)
        broker terminal == non-UNKNOWN                  -> record that exact final state
     redelivery (same handle, identical params):
        gate returns the original bound request object; broker returns lateResolution if present,
        else the existing terminal; executor records:
          UNKNOWN -> UNKNOWN            : idempotent
          UNKNOWN -> exact non-UNKNOWN  : single resolution (immutable thereafter)
          non-UNKNOWN (same)            : idempotent
          non-UNKNOWN (different)       : throws
     renewal: only on resolved NOT_EXECUTED -> new attempt, cleared bytes -> unrecorded
```

- Concurrent same-byte calls cannot produce two final states: the broker resolves at most one
  non-`UNKNOWN` completion per `(route, operation, requestId, digest)` (`acceptLateResolutionLocked`
  rejects a *different* late outcome as `IDEMPOTENCY_CONFLICT`), so every redelivery observes the same
  single `lateResolution`; and the ledger serializes on the action record, permits exactly one
  `UNKNOWN -> non-UNKNOWN` transition, and treats every other same-state write as idempotent. Two
  concurrent redeliveries thus record the identical resolved state or no-op, never divergent states.

### R-5. Changed-file list delta and API table delta

- Design #1 file set is unchanged except: **add** Modify `remote/RemoteGameCommandBroker.java`
  (late-resolution read on same-digest redelivery; package-internal, no new public surface) and the
  amended `recordOutcome` transition inside the already-listed Modify of
  `remote/CloudTaskRunActionLedger.java`.
- API/capability table delta: `recordOutcome` stays package-private/executor-only; broker
  `lateResolution` read stays package-internal (no Service/Task/host reachability); no new public
  method, no new mint or outcome-submission surface. All other rows of Design #1 stand.

### R-6. Threat review delta

| Threat | Defense (delta) |
|---|---|
| Caller forces a false late resolution | broker only returns a `lateResolution` produced by the authenticated client-ingress `completeOutcome` path; executor is still the only ledger writer; no caller can inject one |
| Two final states via concurrent redelivery | single broker `lateResolution` per requestId+digest + single ledger `UNKNOWN -> non-UNKNOWN` transition under the action lock |
| Renewal off an unresolved `UNKNOWN` | renewal requires resolved `NOT_EXECUTED`; unresolved `UNKNOWN` stays non-renewable (fail-closed) |
| Redelivery with changed bytes | unchanged: `bindOrVerifyRequest` rejects a divergent digest before any broker read |

### Disposition

Design only. No Java/tests/other-docs edited; no Maven/host/poller/UI/capture/input run; no Git. Per
condition 5 I stop here and wait for an explicit `DESIGN APPROVED` (or a further `BLOCKED`) from the
local monitor. The worker keeps a 5-minute monitor on this file.

## Local Design Review #2 - DESIGN APPROVED (2026-07-12)

### Verdict

- **DESIGN APPROVED. `P0=0, P1=0, P2=0`.** Design Repair #1 satisfies all five conditions from
  Local Design Review #1. The stale duplicate Design #1 remains retracted and has no authority.
- Same-byte redelivery is now coherent end to end: the gate returns the original retained request;
  the broker returns its single authenticated/digest-correlated late resolution when available and
  otherwise the existing `UNKNOWN`; the executor remains the sole ledger writer; and the ledger
  permits only `unrecorded -> UNKNOWN -> one exact non-UNKNOWN state` before becoming immutable.
  Renewal remains possible only from the resulting verified `NOT_EXECUTED`.

### Approved implementation scope

- **New:** `remote/CloudTaskRetainedActionState.java`, `remote/CloudTaskServicePort.java`.
- **Modify only:** `remote/CloudTaskRunActionLedger.java`,
  `remote/CloudTaskRunExecutionGate.java`, `remote/RemoteGameClientPort.java`,
  `remote/CloudTaskRunCommandExecutor.java`, `remote/CloudTaskServiceExecutionContext.java`,
  `remote/CloudTaskRunAuthorityAssembly.java`, `remote/RemoteGameCommandBroker.java`, and Cloud-only
  `com/bot/dhxy/runner/context/TaskExecutionContext.java`.
- No DHXY Java, test source, host/configuration, endpoint/client-ingress route, business Service/Task,
  Spring registration, or unrelated document is approved for modification.

### Binding implementation conditions

1. Preserve the structured `(phaseCode, actionSlot, occurrence)` address, package-internal
   declaration/renewal, operation-specific opaque non-mintable handles, exact state/context/current
   attempt checks, first-request immutable binding, and per-action call/renewal serialization.
2. Broker redelivery may only read the already stored `lateResolution` for an identical retained
   request. It must not redispatch, create IDs, rebuild bytes, expose a public query, or relax
   ingress/digest/idempotency checks.
3. Ledger outcome progression must match Design Repair #1 exactly. Non-`UNKNOWN` state is immutable;
   unresolved `UNKNOWN` is never renewable; only verified `NOT_EXECUTED` unlocks trusted renewal.
4. `RemoteGameClientPort`, raw gate builders, ledger/identity/renewal, broker, and activation runtime
   access must be package-internal. The only public mechanical-call capability is the
   non-constructible typed `CloudTaskServicePort`; ordinary Service/Task code receives no mint or
   renewal method.
5. Keep the host dormant and add no retry/fallback/TTL/takeover/cleanup/business-semantic behavior.
   **无已批准业务差异；按基线等价迁移。**

### Implementation handoff

- The same external worker may now implement only the approved file set, then run fresh Cloud
  `mvn -q clean package` with no skip flags. It must append exact changed files, reachability scan,
  `javap` visibility evidence, Surefire totals, `src/test`/DHXY-Java/no-runtime/no-Git checks, and
  either `READY FOR LOCAL REVIEW` or an explicit severity-rated `BLOCKED` entry to this log.

## External Worker - Implementation #1 - READY FOR LOCAL REVIEW (2026-07-12)

### Verdict and scope

- Implemented only the ten Cloud files authorized by `Local Design Review #2 - DESIGN APPROVED`.
  No known `P0/P1/P2` remains in this implementation slice: `P0=0, P1=0, P2=0`.
- This is an external-worker handoff, not an approval. The parent must perform the independent local
  source review and append the actual `APPROVED` or `BLOCKED` decision.
- The boundary is dormant. No host/configuration/endpoint/client-ingress route, Spring registration,
  business Task/Service, DHXY Java, or test source was edited or activated.

### Exact Java edit set

New:

1. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRetainedActionState.java`
   (`10842` bytes, SHA-256 `56927DEEA204C62288AADE60794FDAD40BA1CF4370F7E008E35B4B4C4120B5A9`).
   - Package-internal, taskRun-bound retained state.
   - Owns structured `ActionAddress(phaseCode, actionSlot, occurrence)` declarations, one record/lock
     per semantic action, typed handle creation, exact owner/context/current-attempt validation, and
     verified-`NOT_EXECUTED` renewal.
2. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServicePort.java`
   (`7502` bytes, SHA-256 `59F0F0C96DF0C5036DCFB2E1D20A877592A3A059EAA685E659523A5D4E0B597F`).
   - Public final typed facade with a package-private constructor.
   - Exposes only `readWindowFact`, `capture`, and `executeInputBundle`, each requiring its matching
     opaque operation-specific handle. Handle constructors and all authority accessors are
     package-private.

Modified only:

1. `CloudTaskRunActionLedger.java` (`20495` bytes,
   `BF2B3A330B0CF0334D1796C990C7C79CEC34AD8B2137541D6A74ADB60141896E`): narrowed ledger/identity/
   retained-request/renewal authority to package scope; bound attempts to the exact context object;
   retained outcome digest; implemented the approved `UNKNOWN -> one exact final` transition; and
   allowed replacement context only through verified `NOT_EXECUTED` renewal.
2. `CloudTaskRunExecutionGate.java` (`12496` bytes,
   `3E1CF9036EFC45AD1CFB2E79BAD45F0CB77839839E33C1BF3AC1085D0E9BBD7B`): narrowed raw builders to
   package scope and required exact ledger-owned/current identity-context equality before request
   construction/binding.
3. `RemoteGameClientPort.java` (`3142` bytes,
   `98464B56D629118FD5CFBF5002E0D77F2590D70C3E86C7AD42B64DCEA6C720DB`): narrowed the raw
   context/identity executor contract to package scope behind `CloudTaskServicePort`.
4. `CloudTaskRunCommandExecutor.java` (`5486` bytes,
   `69B82F16B3EDFFD9816A8ABB0CA5D6FEFEDBE3D7EFBB43D8A4D963733E73166F`): retained behavior and sole
   ledger-outcome-writer ownership; safety documentation now describes unresolved/correlated
   outcomes accurately.
5. `CloudTaskServiceExecutionContext.java` (`4914` bytes,
   `CC3F3EB0021087DCCEDC5FFAE0BA5C05224B74BCF469B2CE167989AB4B427110`): constructs the retained
   state and typed facade from the exact same-assembly context/gate/ledger/executor; replaces raw
   public collaborator access with `servicePort()` and package-internal retained-state access.
6. `CloudTaskRunAuthorityAssembly.java` (`6967` bytes,
   `D1E6C86B2145DB1411A26E71243563459FD07D30D01E9442BBC794C6C0FCFEBB`): creates one dormant
   Task/Service runtime and returns a package-internal runtime record containing the compatibility
   `TaskExecutionContext` plus retained action state. The one-assembly-per-coordinator claim remains.
7. `RemoteGameCommandBroker.java` (`42603` bytes,
   `331BB7612B89A8C27976B8ACB87A04EEA3A1EC76AA4B1E890DEE290D121648A6`): an identical retained
   request now reads its already stored `lateResolution` before and after the existing await; the
   new path never registers, enqueues, redispatches, mints IDs, rebuilds bytes, or exposes a query.
8. Cloud-only `com/bot/dhxy/runner/context/TaskExecutionContext.java` (`6855` bytes,
   `10A099FA9C49B76DB42DEB7E836CD2FB089AD7602E78F2D46CE539BCBB770D12`): adds only
   `getRemoteGameClient()` returning the typed `CloudTaskServicePort`; no declaration/renewal/raw
   context/identity capability is exposed.

The eight modified files all changed from the pre-implementation SHA snapshot recorded by this
worker; both new files were absent at that snapshot. No file outside this exact ten-file Java set
was written by this implementation.

### Retained action, immutable delivery, and outcome state evidence

- `CloudTaskRetainedActionState` canonicalizes a structured, length-prefixed semantic address and
  serializes declaration, invocation, and renewal per action record. Duplicate declaration under
  the same exact context returns the current opaque handle; a different operation or context fails.
- The ledger mints IDs only on first declaration and verified-`NOT_EXECUTED` renewal. Request bytes
  are frozen by `bindOrVerifyRequest`; equal reconstruction returns the original retained object,
  while a changed payload/timeout/revision digest fails closed.
- Ledger progression is exactly:
  - unrecorded -> `UNKNOWN`;
  - exact same state/outcomeDigest -> idempotent;
  - `UNKNOWN` -> one correlated non-`UNKNOWN` state;
  - unrecorded -> one correlated non-`UNKNOWN` state;
  - any non-`UNKNOWN` -> different state/digest, or final -> `UNKNOWN`, is rejected;
  - renewal reads only recorded `NOT_EXECUTED`, then clears request/outcome state and supersedes the
    old identity/handle.
- Broker existing-request selection first requires the same route/operation/requestId and exact
  requestDigest. Only that retained branch calls `awaitRetainedResolution`; it reads the existing
  atomic `lateResolution`, otherwise observes the existing terminal future, then reads late state
  once more for a concurrent publication. `await` itself only observes/completes the existing
  future/deadline and never enqueues or dispatches.
- The executor records exactly the broker-returned typed object. Existing authenticated ingress,
  correlation, outcome-digest conflict, enqueue revision, and final-dispatch revision checks are
  unchanged.

### Source reachability evidence

Fresh `rg` over all Cloud `src/main/java` produced this authority graph:

- `actionLedger.acquire(...)`: exactly one caller,
  `CloudTaskRetainedActionState.java:129`.
- `actionLedger.renewAfterNotExecuted(...)`: exactly one caller,
  `CloudTaskRetainedActionState.java:150`.
- `actionLedger.recordOutcome(...)`: exactly three operation-specific calls, all in
  `CloudTaskRunCommandExecutor.java:56,82,108`.
- `new CloudTaskRetainedActionState(...)` and `new CloudTaskServicePort(...)`: only
  `CloudTaskServiceExecutionContext.java:42,44`; opaque handle constructors occur only in retained
  state at lines `198,200,202`.
- `createTaskServiceRuntime(...)`: only its package-internal declaration and the assembly's own
  compatibility delegation; no `CloudTaskRunAuthorityAssembly.create(...)` runtime caller exists.
- Host/config/server/api/gateway scan for `CloudTaskServicePort`, retained state, authority assembly,
  or runtime creation returned no matches.
- Public raw-authority scan for acquire/renew/record/declaration/raw request builders returned
  `NONE`.

Therefore source reachability remains dormant and package-internal up to the single typed public
mechanical facade. There is no raw request, poll, completion, ledger, identity, declaration, renewal,
broker, or activation capability reachable by ordinary Service/Task code.

### `javap` visibility evidence from the fresh build

- `javap -public CloudTaskServicePort` shows exactly the three typed methods and **no public
  constructor**.
- `javap -public` for `WindowFactAction`, `CaptureAction`, and `InputBundleAction` shows empty public
  bodies and **no public constructors or authority getters**.
- `javap -p` declarations are all non-public:
  `final class CloudTaskRunActionLedger`, `final class CloudTaskRunExecutionGate`,
  `interface RemoteGameClientPort`, `final class CloudTaskRetainedActionState`,
  `final class CloudTaskRunAuthorityAssembly`, `final class RemoteGameCommandBroker`, and
  package-internal `CloudTaskRunAuthorityAssembly$TaskServiceRuntime`.
- `javap -public TaskExecutionContext` shows the compatibility getter
  `public CloudTaskServicePort getRemoteGameClient()` and no retained-state/ledger/raw-port method.
- The fresh shaded JAR contains `CloudTaskRetainedActionState.class`, `CloudTaskServicePort.class`,
  all three opaque handle classes, and the Cloud compatibility `TaskExecutionContext.class`.

### Fresh build and Surefire evidence

- Final command (after the last Java/Javadoc edit): Cloud `mvn -q clean package` with no skip flags.
- Result: `exit 0` in `59.8s`.
- Artifact: `target/dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`, `119464598` bytes.
- Fresh Surefire XML totals:
  - suites `4`;
  - tests `21`;
  - failures `0`;
  - errors `0`;
  - skipped `0`.
- Suites: template resource guard `1`, incense contract `11`, Wuhuan route contract `1`, Xiuluo
  contract `8`. No test source was added, restored, modified, or deleted by this worker.

### Scope, runtime, and Git mutation checks

- Cloud `git status -- src/test`, `git diff -- src/test`, and staged-index checks were empty.
- DHXY `src/main/java` status after implementation is identical to the worker's pre-implementation
  snapshot: the same pre-existing modified/untracked migration files remain; this worker made no
  DHXY Java write and ran no DHXY build/runtime command.
- The Cloud tracked dirty files and unrelated untracked migration material predated this slice and
  were not edited, reset, overwritten, cleaned, staged, or committed by this worker.
- No `git add`, commit, push, reset, checkout, clean, revert, branch, merge, or index mutation was
  executed in either repository. Both staged-index scans were empty.
- No host, poller, UI, capture, input, Spring application, Task, thread, or production cutover was
  started. Maven was used only for the required clean/package verification.
- No retry, fallback, TTL, takeover, cleanup, lifecycle transition, OCR/capture policy,
  click/navigation order, or business result interpretation was added.
  **无已批准业务差异；按基线等价迁移。**

### Handoff

`P0=0, P1=0, P2=0`. The approved dormant retained-action + typed Service-port boundary is
**READY FOR LOCAL REVIEW**. It is not `APPROVED` by this entry; the parent must independently review
the latest source/build evidence and append the next gate decision here.

## Local Implementation Review #1 - APPROVED (2026-07-12)

### Verdict

- **APPROVED. `P0=0, P1=0, P2=0`.** The latest ten-file Cloud implementation matches Local Design
  Review #2 and Design Repair #1. No repair is required for this dormant boundary.
- This approval closes only retained action identity/state, typed synchronous Service calls, and
  same-byte late-resolution convergence. Persisted Task action catalogs, activation,
  pause/resume rehydration/reconciliation, checkpoint/sleep, business Service/Task migration, and
  production cutover remain separate later gates.

### Independent source review

- Recomputed all ten source lengths/SHA-256 values; each exactly matches the worker handoff.
- `CloudTaskRetainedActionState` is package-internal and binds structured
  `(phaseCode, actionSlot, occurrence)` addresses to one taskRun, exact execution-context object,
  one operation, one ledger identity, and one current opaque handle. Declaration, invocation, and
  renewal serialize per action; foreign-state/context/operation and superseded handles fail closed.
- `CloudTaskServicePort` has no public constructor. Its three public nested handles have no public
  constructor or public authority accessor. The only public methods are typed `readWindowFact`,
  `capture`, and atomic `executeInputBundle`; no mint, renewal, raw request, poll, completion,
  broker, ledger, or identity capability is exposed.
- Gate builders, ledger/identity/renewal, raw `RemoteGameClientPort`, authority assembly/runtime,
  and broker are package-internal. Reachability scan found `acquire` and renewal only in retained
  state, and all three `recordOutcome` calls only in `CloudTaskRunCommandExecutor`; host/config/api/
  gateway has no runtime-creation caller.
- Broker duplicate selection still requires exact route/operation/requestId and requestDigest. It
  only reads an already stored authenticated `lateResolution`; it never registers, queues,
  redispatches, mints, or rebuilds a retained request. Ledger progression is exactly
  `unrecorded -> UNKNOWN -> one exact non-UNKNOWN state`, with non-`UNKNOWN` immutable and renewal
  restricted to verified `NOT_EXECUTED`.

### Independent build and binary evidence

- Fresh parent-run Cloud `mvn -q clean package` completed with exit `0` in `63.4s`, without skip
  flags. Surefire totals: `4` suites, `21` tests, `0` failures, `0` errors, `0` skipped.
- Shaded JAR: `target/dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`, `119464598` bytes, SHA-256
  `A95AF8085F0A65B0EB2CB1A8959C46B798943A5D549D66F70A48199CBE101D05`; it contains retained state,
  typed port, all three opaque handles, and Cloud `TaskExecutionContext`.
- Independent `javap -public` shows exactly three typed facade methods and no public constructor;
  each handle has an empty public body. `javap -p` confirms ledger and raw port declarations are
  non-public. `src/test` and staged-index status are empty.
- No DHXY Java, host/poller/UI/capture/input/Task runtime, production switch, or Git mutation was
  performed in this review. **无已批准业务差异；按基线等价迁移。**

### Closure

- This shared slice is CLOSED/APPROVED. The next migration slice may consume this boundary only
  after defining its trusted persisted action catalog/rehydration rules; the Task host remains
  dormant until its independent activation/cutover gate.
