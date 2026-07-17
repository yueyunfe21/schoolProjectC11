# CommonBox Count Unit Integration Helper H3

## Scope

- Role: Internal Review Helper H3, non-binding integration-risk precheck only; not a final reviewer.
- Count unit inspected: `CommonBoxService::consumePendingBoxIfAllowed` (`countDelta=+1` remains parent-owned).
- Authority read: repository `AGENTS.md`, `docs/DHXY_CONTEXT.md`, CR271 top of `docs/ACTIVE_WORK.md`,
  `docs/业务逻辑.md` common-box rules, the whole-service plan, the service migration matrix, and I1's
  `2026-07-15-cloud-common-box-count-unit-worker-i1.md` report.
- Source inspected read-only in both repositories. No Java, External log, CR/card, dashboard, plan, matrix, or
  existing report was edited. No build, test, runtime, application, server, Task, poller, UI, capture, input, Git
  mutation, rollback, cleanup, commit, or submission was performed.

## Observations

1. **The dedicated Cloud contract is narrow and typed.**
   `CloudCommonBoxPort` exposes exactly one observation and one click operation. `CommonBoxObservationResult`
   keeps the five mechanical states separate from the three transport terminals, and only `MATCHED` may carry
   all four match fields. `CommonBoxClickResult` keeps `EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN` separate.

2. **The observation coordinate path is internally consistent in the normal, stationary-window case.**
   `CommonBoxLocalObservationMechanics.observe` returns window-client pixels in the fixed
   `(623,590)-(682,618)` ROI. `LocalRemoteGameCommandHandler.toCommonBoxFact` adds the exact binding origin and
   emits `SCREEN_ABSOLUTE_PX`; `RemoteCommonBoxFact` and Cloud `WindowFact.CommonBoxFact` both enforce that space.
   The assembly forwards the point to one `SCREEN_ABSOLUTE_PX` input bundle without a second conversion.

3. **The primary exact-identity fences are present.**
   Cloud uses the passed `TaskExecutionContext` and its per-run `CloudGameClient`; it does not resolve a default
   context. DHXY `requireBoundWindow` resolves the command window id and rechecks native handle, process id, and
   player-identity epoch. Both `COMMON_BOX` observation and input execution run under
   `WindowTaskContextHolder.callWith(access.context(), ...)`.

4. **The local match timestamp remains the TTL anchor.**
   DHXY records `matchedAtEpochMs` immediately after a valid match, the handler/fact/Cloud result preserve it,
   and Cloud stores `expiresAtMs = matchedAtEpochMs + 30_000`. Capture/transport latency therefore consumes the
   existing 30-second budget instead of silently restarting it at Cloud receipt time.

5. **The physical click reaches the existing single queue as one atomic request.**
   The assembly creates one ordered list:
   `MOVE_MOUSE(x,y) -> SLEEP(80ms) -> CLICK_LEFT(x,y,120ms)`, then calls `executeInputBundle` once. DHXY maps the
   complete list once and calls `InputActionQueue.submitRemoteAndWaitDetailed` once. No CommonBox callback runs
   inside the input worker, and no nested queue submission was found on this path.

6. **The actual Cloud caller inventory is smaller than I1's narrative.**
   Current `commonBoxService.*` callers are:
   `AutoCombatService.detectMemberBoxAfterCombatExit`,
   `AutoCombatService.hasPendingBoxForCurrentWindow/consumePendingBoxIfAllowed`, and
   `AutoBattleTask.consumePendingBoxIfAllowed`. `detectLeaderBoxAfterReturnHome` has no caller outside its own
   definition in the current Cloud source tree. The two consume call sites are source-reachable, but a leader
   pending producer-to-consumer chain is not demonstrated by current callers.

7. **The five mechanical outcomes are not folded together, but transport detail is discarded.**
   `OBSERVED` CommonBox facts map all five states one-to-one. For non-observation terminals, however, the assembly
   maps every transport `NOT_EXECUTED` code to one CommonBox status and drops `CommonOutcome.code`. Detection then
   logs this as `reason=not-executed`; click returns `false` and retains pending. `STOPPED` passes through a task
   checkpoint, while `UNKNOWN` reaches the Service fatal path and is not final-consumed by `CloudGameClient`.

8. **The new port implementation is not registered by the production service-host configuration currently in
   source.**
   `CloudServiceHost.create` registers only `CloudServiceConfiguration`. That configuration scans only
   `com.bot.dhxy.service` and has no `@Bean` for `CloudCommonBoxPort`. The sole implementation,
   `CloudCommonBoxPortAssembly`, is an `@Component` under `com.yueyunfe.dhxy.cloudbrain.remote`, outside that scan.
   No second application context, explicit registration, or `@Import` for this assembly was found.

## Risks

### R1 - High: Spring host cannot currently supply `CloudCommonBoxPort`

`CommonBoxService` is discovered by the service-only component scan and requires `CloudCommonBoxPort` in its
generated constructor, but the only implementation is outside the scan. On the source-visible production host
path, context refresh is therefore expected to fail with an unsatisfied `CloudCommonBoxPort` dependency unless an
external registration not present in this repository is injected. A compile/package pass alone does not exercise
this context-refresh failure. This is the clearest integration gap for the countable chain.

### R2 - High: leader end-to-end reachability is not present in current callers

I1 states that the leader public caller remains unchanged and reaches role-specific detection, but source search
finds no caller of `detectLeaderBoxAfterReturnHome`. A leader consume opportunity cannot obtain a pending record
through the reviewed Cloud graph. The member producer and both consumer call sites exist, so this is a role-specific
reachability gap rather than absence of every caller.

### R3 - Medium: transport `NOT_EXECUTED` reasons lose authority before business handling

`TASK_RUN_MISMATCH`, `WINDOW_BINDING_CHANGED`, `TASK_RUN_PAUSED`, `TIMEOUT`, `FACT_UNAVAILABLE`, and queue rejection
can all arrive as `NOT_EXECUTED`, but `CommonBoxObservationResult`/`CommonBoxClickResult` retain no outcome code.
Detection treats the aggregate terminal like a completed no-pending observation, and click treats it like ordinary
non-execution with pending retention. Parent review should decide from the approved baseline whether each code may
share that handling; H3 does not infer that all negative transport signals are equivalent business truth.

### R4 - Medium: observation post-fence does not bind the returned point to unchanged geometry

The DHXY handler refreshes the binding after observation, but compares only command identity fields. It converts
the matched client point using the pre-observation binding origin and does not compare pre/post `x/y/width/height`.
If the same HWND moves during capture/match, an `OBSERVED` fact can carry a stale screen-absolute point. The later
input handler revalidates current binding and bounds, which rejects large drift, but a stale point that remains
inside the moved window can still pass coordinate validation. This is also relevant to the existing baseline's
screen-absolute pending model and should not be changed casually without a business-baseline decision.

### R5 - Medium: synchronous remote observation conflicts with the documented background boundary

Both role-specific detect methods call `readWindowFact` inline with a 120-second timeout. The migration matrix says
CommonBox detection keeps an asynchronous entry boundary, and `docs/业务逻辑.md` says background detection must not
block the leader from continuing the next round. The `696a12b0` Service source itself is synchronous around its
local capture, so the documents and selected source baseline are not fully aligned; replacing a quick local call
with a potentially long remote wait amplifies that discrepancy. Parent should resolve the authority conflict before
requesting any behavior change.

### R6 - Low/operational: TTL compares clocks from two processes

The TTL is correctly anchored to the DHXY-local epoch timestamp, but expiration is evaluated with Cloud
`System.currentTimeMillis()`. If DHXY and Cloud can run on hosts with materially skewed wall clocks, pending may
expire immediately or live longer than 30 real seconds. If deployment guarantees synchronized clocks or same-host
execution, this risk is contained; that assumption is not encoded in the reviewed contract.

## Parent-checklist

- [ ] Register exactly one `CloudCommonBoxPort` in the actual `CloudServiceHost` context, or identify the existing
  production registration path with source evidence; confirm the service-only scan is not the final runtime graph.
- [ ] Re-scan the current source after concurrent writers settle and identify a real
  `detectLeaderBoxAfterReturnHome` caller, or record that leader production is outside this count unit instead of
  claiming a complete leader chain.
- [ ] Trace both existing consume callers from their executable task entry and exact `TaskExecutionContext` producer;
  do not count mere method references as runtime reachability.
- [ ] Compare each `NOT_EXECUTED` outcome code against the `696a12b0`/approved CommonBox behavior and preserve code
  authority where task-run, binding, pause, or timeout semantics differ from an ordinary observation miss.
- [ ] Confirm whether pre/post geometry equality is required by the existing exact-window fact contract. Any new
  geometry fence, retry, re-observation, or TTL rule needs baseline/CR authority rather than a helper inference.
- [ ] Reconcile the migration-matrix/background requirement with the synchronous `696a12b0` source and the new
  120-second remote wait before changing scheduling or call placement.
- [ ] Confirm the deployment clock model for DHXY-local `matchedAtEpochMs` versus Cloud expiry checks; retain local
  match time as the TTL authority.
- [ ] Preserve the current single ordered input bundle and verify no future wrapper places
  `CloudCommonBoxPort.click` inside an input-worker callback.
- [ ] Parent should perform the required source review and fresh build gates only after shared writers settle. H3
  supplied no build/test/runtime evidence and makes no final review decision.

