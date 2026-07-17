# TURN-28Q Action-List Production/Test Delivery Preflight

Status: `PRECHECK_COMPLETE`

Role boundary: CR271 Internal helper only. This report is neither a delivery review nor an implementation
claim. It does not approve, reject, block, close, or change the status of TURN-28Q. The parent remains the sole
manager/final reviewer, and External A remains the only implementation owner recorded at the card's physical
true EOF.

Platform identity:

- nickname: `Archimedes`
- platform thread id: `019f6ae7-29bc-7fa1-b5aa-2624f0acdf44`
- thread source: platform subagent, depth 1

Observation cut: `2026-07-16T08:42:28.3646394-04:00`.

## Read Scope And Authority

Read completely for this preflight:

- `D:\mavenProject\DHXY\AGENTS.md`
- `docs/DHXY_CONTEXT.md`
- the CR271 head of `docs/ACTIVE_WORK.md`
- sections 14-19 of `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md`
- `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`
- `docs/业务逻辑.md`
- the physical true EOF of `2026-07-16-turn-card-TURN-28Q.md`
- the latest physical true EOF of `2026-07-16-turn-card-TURN-28P.md` and
  `2026-07-16-turn-card-TURN-22.md`
- both repository statuses and all five TURN-28Q Java files through their latest observed EOF

Authority facts used by this checklist:

1. TURN-28Q's latest card true EOF is `EXTERNAL-A CLAIMED` at
   `2026-07-16T08:33:53.876-04:00`; there is no `SOURCE+TEST DELIVERED` marker at this observation cut.
   Therefore every Java delta below is active WIP evidence, not a delivery and not reviewable completion.
2. TURN-28P's latest true EOF is Euler's two-test real queue/worker delivery at
   `2026-07-16T08:28:05.095-04:00`; it explicitly owns only its two tests and leaves the TURN-28Q five-file
   write set to External A.
3. TURN-22's latest true EOF is parent adjudication #2 at `2026-07-16T08:23:11.657-04:00`: its Repair #3 is
   prerequisite-blocked by TURN-28Q because the previous public frozen boundary accepted only a callback.
4. Protocol lines 216-222 require `clickDelayMs` on the click plus `queueHoldMs` as one sleep in the same mapped
   action list, and require that complete list to be submitted once to the global input queue. No second command,
   retry, no-op input, or business decision is authorized.
5. The only business baseline for this migration is `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.
   TURN-28Q is mechanics-only. No approved business difference exists.

Both repositories were dirty/untracked before this helper started. DHXY is on `thin-client-design`; Cloud is on
`navigation-migration`. This helper performed no Git mutation and did not alter or normalize either worktree.

## WIP Snapshot

These hashes are an observation cut only. They must not replace External A's final delivery hashes.

| File | Lines | SHA-256 at 08:42:28 -04:00 |
|---|---:|---|
| `InputSequences.java` | 210 | `b293e0c6792303d45a4314050c6e4f1c8b39d0f4dea426632586ed0f292dacb3` |
| `InputActionQueue.java` | 850 | `66fa536ef8b4c6cbf8874cd94d8842fd8b0f9d3f4e74bc52719f31f39e4660bf` |
| `InputActionRequest.java` | 1118 | `23973b7eee06949138e8a2841e249c009eb69184804c2be0689aa317c29988de` |
| `InputActionWorker.java` | 735 | `4b853f9552ae4f59ae46346bc1ba8606de40bcc5e9ad3392cf73ddea1f83b6e5` |
| `InputActionFrozenExclusiveContractTest.java` | 734 | `943dc4867b55569f24788f50bcf83227ce1543a1b71c3c53232f11eb12deea16` |

The four production files changed between the initial card snapshot and this cut. The named test was still the
original 561-line snapshot during the first read, then became 734 lines at `08:38:13`; this confirms active
concurrent implementation and is why this helper records only preflight evidence.

## Current Production Evidence

### Public facade

- `InputSequences.java:110-117` now exposes one typed
  `submitFrozenExactWindowActionsAndWait(description, context, binding, actions)` and delegates directly to the
  queue. This one facade-to-owner delegation is the required public boundary, not an invitation to add another
  adapter or synonym.
- The legacy `submitAndWait` at `:40-42` still returns boolean and remains the wrong route for TURN-22's frozen
  click list. It must not be called from inside the frozen callback.

### Queue freeze and one request

- `InputActionQueue.java:393-421` is the current WIP sibling boundary. It copies the supplied list at `:400`,
  captures task tokens at `:401`, takes `synchronized (exactContext)` at `:403`, requires binding object identity
  at `:404`, reads the player epoch only under that monitor at `:405-414`, reuses the established generation
  witness at `:415`, and then awaits exactly that request at `:421`.
- This method contains no binding refresh, title search, caller-supplied epoch, or call to legacy
  `submitAndWait`. The accepted shape is direct freeze -> one request -> `await`, with no callback adapter.
- The existing callback boundary at `InputActionQueue.java:337-365` remains the reference behavior and must not
  be changed to implement the list route.

### Request shape and typed state

- `InputActionRequest.java:231` already performs `List.copyOf(actions)` in the common constructor.
- The WIP factory `frozenExactWindowActions` at `:278-289` directly invokes that common constructor with the
  complete list, `exclusiveCallback=null`, exact binding, exact epoch, and `frozenExactWindow=true`. It creates
  no second DTO and no callback shim.
- Object-generation protection remains centralized in
  `isFrozenExactWindowGenerationCurrent()` at `:440-451`: the current context must publish the identical binding
  object before exact-field and epoch checks can pass. This is the A -> B -> A witness.
- STOP typing remains centralized in `detectFrozenExactWindowFailure` at `:897-925`, specifically
  `STOP_REQUESTED` at `:908-911`; terminal progress remains the existing `InputActionExecutionResult`, not a
  boolean or new result type.

### Worker ownership and dispatcher reuse

- `InputActionWorker.java:128-131` now routes every frozen request through either the unchanged callback branch or
  the WIP action-list branch. It does not enqueue from inside the worker.
- The callback mechanics remain in `runFrozenExactWindowExclusive` at `:403-440`; its context monitor,
  callback/finally completion barrier, and typed completion path are still structurally separate.
- `runFrozenExactWindowActions` at `:464-522` takes the same context monitor at `:477`, checks the generation,
  focuses the exact frozen binding at `:483-486`, iterates the request's immutable list at `:488-519`, uses the
  existing per-step `tryStartStep/markStepCompleted` at `:511/:517`, and calls the existing private action
  dispatcher `execute(...)` at `:514`. It does not copy click/key mechanics.
- The existing dispatcher remains `InputActionWorker.java:319-357`. Keeping all click delay and `SLEEP` execution
  there is the important no-duplication boundary.

## Delivery Risks To Check

These are preflight risks and acceptance prompts, not P0/P1/P2 findings and not a card verdict.

### Wrapper and deadlock

1. Keep exactly one public facade delegation (`InputSequences` -> `InputActionQueue`). Queue must directly freeze,
   construct, enqueue, and await the request. Do not add a second `internal/resolve/submit` wrapper chain.
2. Never implement the list route as a callback that calls `InputSequences.submitAndWait` or any other queue API.
   The sole worker would wait for itself and deadlock.
3. Never run `InputProvider` mechanics in TURN-22 or in the queue. The worker's existing `execute` dispatcher is
   the only mechanics owner.

### Typed STOP, pause, and cancellation

1. Current WIP production checks `checkDetailedSafety(stage)` before every frozen action, which can retain the
   existing typed `STOP_REQUESTED`. The named test must prove this with a real captured `TaskStopToken`, not by
   interrupting the worker thread.
2. Current WIP test `stopMidFrozenActionListStartsNoLaterActionAndKeepsTypedPrefixProgress`
   (`InputActionFrozenExclusiveContractTest.java:390-414`) interrupts the worker from the first click at `:392`.
   It does not assert `result.getSafetyReason()==STOP_REQUESTED`. That is interruption evidence, not the card's
   typed STOP contract, and it can terminate that harness worker loop.
3. Current WIP worker prose at `InputActionWorker.java:446-452` says a pause arriving mid-list is deferred to the
   next request boundary, while the loop at `:488-519` has no `waitIfPaused` between elements. The frozen card says
   STOP/pause/cancellation/safety closure must permit no unauthorized later action. Before delivery, the owner and
   parent must reconcile this exact wording: either wait at each action boundary while retaining the same context
   monitor/request, or provide card-consistent evidence that no later action starts while paused. This helper does
   not adjudicate that policy.
4. A cancellation observed after one action must preserve the truthful completed prefix, return a non-completed
   typed status, and prevent action index `n+1` from starting. It must not be relabelled as success or replayed.

### A -> B -> A and generation authority

1. The queue's binding-object identity check and the request's one authoritative generation witness must remain the
   only generation policy. Do not add a caller epoch, value-only comparator, second refresh, or title lookup.
2. Rechecking the same established safety/generation methods inside the held monitor must not become an independent
   comparator with different fields or failure typing. The proof of atomicity is the shared context monitor, not a
   best-effort series of comparisons.
3. A value-equal replacement binding must produce `NOT_STARTED` plus typed
   `WINDOW_BINDING_CHANGED` and zero focus/input for the stale request.

### One list submission

1. One non-null request id alone does not prove one queue submission. The named test needs an observable test-only
   request count and must inspect the request taken by the real worker.
2. The observed request must contain exactly two ordered elements for the baseline case:
   `CLICK_LEFT(x,y,delayMs=150)` then `SLEEP(delayMs=500)`. There must be no trailing WAIT, second command,
   no-op mouse action, second UUID, retry, replay, or session.
3. The worker must focus once using the exact frozen binding and execute the complete list inside one global input
   transaction and one context-generation monitor.

## Current Test Evidence And Gaps

- The success case at `InputActionFrozenExclusiveContractTest.java:354-382` now drives the real queue/worker and
  records one physical click plus typed step indices. However it calls `harness.queue` directly at `:360`, so it
  does not exercise the required `InputSequences` public facade.
- The same case infers one request from one request id, one focus, and progress indices at `:368-381`; it does not
  independently count worker-taken requests or inspect the exact action list stored in that request.
- The STOP case at `:390-414` uses worker interruption and omits a `STOP_REQUESTED` assertion, as described above.
- The A -> B -> A case at `:422-460` stores only `Boolean completed` at `:429-436`. It proves zero input but discards
  the typed terminal result, so it cannot assert `NOT_STARTED/WINDOW_BINDING_CHANGED`.
- The new action-list cases call `Harness.resolve(...)` at `:530-543`, which reaches the pre-existing
  `Unsafe.allocateInstance` helper at `:649-663`. TURN-28Q explicitly forbids private reflection for its named
  evidence. The new action-list tests can use their directly constructed exact context/binding instead; the older
  callback tests may remain untouched for non-regression.
- The queued A -> B -> A case uses `waitUntilQueued` at `:462-467`, whose proof loop calls `Thread.sleep(1)`. The
  TURN-28Q evidence must use latches/request observation or a synchronous pre-enqueue A -> B -> A rejection rather
  than a sleep-based race proof.
- Existing callback cases at `:45-343` remain the non-regression corpus for no second refresh, queued cancellation,
  started-callback completion barrier, exact-field drift, non-Runtime throwable normalization, and A -> B -> A.

## Minimal Test Shape

The smallest card-compliant named-test extension can stay in the existing test file:

1. Construct `InputSequences(realQueue)` and call
   `submitFrozenExactWindowActionsAndWait(...)` through that facade for every new action-list case.
2. Use a test-only `InputActionQueue` subclass in the same package that overrides package-private `take()`, delegates
   to `super.take()`, records each taken `InputActionRequest`, and returns it to the real `InputActionWorker`. This
   adds no production hook, reflection, source scan, or synchronous fake.
3. In the success case, assert exactly one taken request, one request id, the exact two-element immutable list,
   exact focus binding, one focus, zero added refresh, one provider click with delay 150, and completed step index 1.
4. For STOP, bind a real `TaskExecutionContext`/`TaskStopToken` as the TURN-28P delivered tests already do. Have the
   first provider click coordinate with a latch, request stop, then release the click. Assert typed
   `STOP_REQUESTED`, non-completed status, truthful completed prefix, and zero later provider action. Do not interrupt
   the worker or use wall-clock sleeps to win the race.
5. For A -> B -> A, preserve the original binding object, publish B then a value-equal A' object, invoke the public
   facade with the stale original binding, and retain the full `InputActionExecutionResult`. Assert
   `NOT_STARTED`, `WINDOW_BINDING_CHANGED`, zero focus/input, and zero added refresh. This can reject synchronously
   and needs neither `Unsafe` nor queue polling.
6. Keep all existing callback tests behaviorally unchanged. The action-list route must not weaken the callback's
   finally/completion barrier or non-Runtime throwable normalization.

## Parent/Delivery Checklist

Production source delivery should provide exact final line/SHA evidence for all five files and demonstrate:

- [ ] one public `InputSequences` action-list entry returning existing typed result
- [ ] one direct queue sibling with no refresh/search/caller epoch and one immutable request
- [ ] binding identity, exact fields, and player epoch frozen under one context monitor
- [ ] one worker transaction and monitor from authoritative check through focus and every action/delay
- [ ] existing dispatcher and per-step progress reused; no copied mechanics or new DTO
- [ ] no nested queue call and no wrapper chain beyond the required facade boundary
- [ ] real STOP/pause/cancel/drift closure prevents unauthorized later action and remains typed
- [ ] callback path source and behavior remain unchanged
- [ ] no retry/replay/session/owner/ledger/TTL/durable workflow or business/OCR logic

Named-test source delivery should demonstrate:

- [ ] public `InputSequences` -> real in-memory queue -> real worker path
- [ ] exactly one worker-taken request containing `CLICK_LEFT(150)` then `SLEEP(500)`
- [ ] exact frozen focus/binding, one focus, no second refresh
- [ ] A -> B -> A returns typed `NOT_STARTED/WINDOW_BINDING_CHANGED` with zero input
- [ ] real task STOP returns typed `STOP_REQUESTED`, truthful prefix, and zero later action
- [ ] callback non-regression remains covered
- [ ] no desktop input/capture, private reflection, source scan, synchronous callback fake, or sleep-based race proof

This helper ran no Maven, JUnit, compile, package, runtime, application, server, Task, UI, capture, or input action.
It made no Git mutation and did not edit Java, the card, ACTIVE_WORK, the main plan, the matrix, or dashboard.

`PRECHECK_COMPLETE`: evidence and delivery checklist are complete for the observed WIP cut. Parent review remains
pending until External A appends a canonical `SOURCE+TEST DELIVERED` at the TURN-28Q card's physical true EOF.

<!-- TRUE_EOF: TURN-28Q ACTION-LIST PREFLIGHT PRECHECK_COMPLETE Archimedes 019f6ae7-29bc-7fa1-b5aa-2624f0acdf44 NON-REVIEWER NON-OWNER 2026-07-16T08:42:28.3646394-04:00 -->
