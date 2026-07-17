# CR271 TURN-28Q Independent Delivery Review R1

- Reviewer role: independent delivery reviewer; not implementation owner and not parent final reviewer
- Review time: 2026-07-16T09:30:53.1766088-04:00
- Review mode: static source and contract review only
- Current branch / HEAD: `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f`
- Independent result: **P0/P1/P2 = 0/2/0 - BLOCKED**

## Findings

### P1-1 - stop-only closure is deterministically relabelled as `WINDOW_BINDING_CHANGED`

The frozen action-list path does stop later input, but it does not preserve the required typed
`STOP_REQUESTED` result unless pause and stop happen together.

Evidence:

1. `InputActionRequest.java:440-450` names `isFrozenExactWindowGenerationCurrent()` as the binding-generation
   witness, but its final decision calls `detectFrozenExactWindowFailure(...)`.
2. `InputActionRequest.java:897-925` makes that detector a mixed safety gate. In particular, lines 908-912
   return `STOP_REQUESTED` when the stop token is closed; therefore a pure stop makes the method named
   "generation current" return false even when binding identity, exact fields and epoch are unchanged.
3. The pre-enqueue path in `InputActionQueue.java:403-420` interprets every false result from that method as
   `WINDOW_BINDING_CHANGED`. A stop already requested before the public facade call therefore returns
   `NOT_STARTED/WINDOW_BINDING_CHANGED`, instead of `NOT_STARTED/STOP_REQUESTED`, with no queue take.
4. The worker repeats the same relabelling at `InputActionWorker.java:483-487` and, between actions, at
   `InputActionWorker.java:504-519`. A stop-only request arriving after action 0 makes the generation check at
   line 513 false and line 514 overwrites the cause with `WINDOW_BINDING_CHANGED` before the typed detailed
   gate at line 518 can publish `STOP_REQUESTED`.
5. This is not a scheduling hypothesis. `InputActionWorker.java:378-380` returns immediately from
   `waitIfPaused(...)` when pause is false, while `TaskPauseToken.java:71-95` checks the stop token only inside
   an active pause wait. A deterministic first-click hook that requests stop only will always take the
   mislabelling branch before the second action.
6. The named STOP case does not cover that path: `InputActionFrozenExclusiveContractTest.java:421-424`
   requests **pause and stop together**. The pause wait throws `TaskStopRequestedException`, so the test reaches
   the outer STOP catch and passes while the stop-only path remains wrong.
7. The caller consequence is typed and user-visible: `TurnInputStepExecutor.java:196-202` maps only
   `STOP_REQUESTED` to `STOPPED`; the relabelled result becomes `INPUT_QUEUE_FAILED`.

There is also no post-final-action frozen safety check before success publication
(`InputActionWorker.java:524-533`, then lines 204-205). A stop closed synchronously inside the final provider
action can therefore be published as success. The callback path does have a post-callback detailed check at
`InputActionWorker.java:165-173`, so this is an action-list closure gap, not callback behavior.

Required repair/acceptance:

- Keep a generation-only identity/epoch witness separate from typed safety detection, or run the typed detailed
  gate before translating an actual generation mismatch. Never translate STOP/cancel/safety failure into drift.
- Before publishing action-list success, preserve a stop that closed during the final action as typed
  `STOP_REQUESTED`; do not replay or retry the action.
- Add deterministic real queue/worker cases for: stop already closed before the public facade call; stop-only
  (no pause) requested inside action 0 before action 1; and stop requested synchronously inside the final action.
  Assert typed reason, truthful progress, one taken request where enqueued, and zero later action.

### P1-2 - supported Alt actions leave the frozen binding and invoke mutable refresh/focus paths

The public API accepts a complete `List<InputAction>` and promises one exact binding identity/epoch across every
action. That invariant is true for the reviewed click/sleep list, but false for supported Alt action lists.

Evidence:

1. `InputActionWorker.java:653-666` elects the background-keyboard path for lists containing only `SLEEP` and
   supported Alt shortcuts. The frozen worker initially focuses the exact supplied binding at lines 483-493.
2. Alt dispatch then calls the **mutable legacy overload** at `InputActionWorker.java:556-584`, specifically
   `boundWindowKeyboardService.pressShortcut(shortcut)` at line 562.
3. That overload performs `refreshAndCommit(context)` at `BoundWindowKeyboardService.java:65-95`. The same class
   already provides the exact no-refresh overload `pressShortcut(binding, windowId, shortcut)` at lines
   98-108, but the frozen worker does not use it.
4. `WindowNativeBindingRefreshService.java:38-63,72-85` creates the live result through
   `binding.withLiveState(...)` and commits it under the context monitor; `WindowNativeBinding.java:65-80`
   confirms `withLiveState(...)` constructs a new object. Java monitor reentrancy means the worker itself is
   allowed to commit while it holds the frozen monitor; the monitor blocks other writers, not this same-thread
   refresh.
5. `WindowRuntimeContext.java:166-207` always publishes `this.nativeBinding = next`, even when refreshed fields
   are value-equal. Thus a successful Alt refresh necessarily replaces the frozen object identity. A one-action
   Alt list has no post-action generation check and can return `COMPLETED` after abandoning its frozen generation;
   with a later action, the next gate fails only after the Alt was already delivered.
6. If background delivery is unavailable, `InputActionWorker.java:578-591` calls
   `focusCurrentWindowInActiveTransaction(...)`. `WindowAwareInputCoordinator.java:137-145,182-210` again invokes
   `refreshAndCommit(context)` and focuses the mutable current binding. The post-fallback safety check at worker
   lines 585-588 is conditional on `hasDeadline()`, while this frozen factory is non-deadline
   (`InputActionRequest.java:278-290`). A changed/reused native generation can therefore reach focused real input.
7. The named test covers only `[CLICK_LEFT(150), SLEEP(500)]`. Its worker is constructed with a null
   `BoundWindowKeyboardService` at `InputActionFrozenExclusiveContractTest.java:607-614`, so it cannot expose the
   supported-Alt frozen path at all.

This does not invalidate the immediate TURN-22 mouse sequence, but it blocks delivery of the frozen public
action-list contract as written. Exact binding is an API invariant, not a property that may depend on action type.

Required repair/acceptance:

- Frozen Alt dispatch must use the existing exact binding overload and exact frozen focus; it must not call a
  refresh/commit or mutable-current focus from inside the frozen request.
- Add a real in-memory queue/worker supported-Alt case with a non-null keyboard collaborator. Assert one request,
  one take/transaction, delivery against the original frozen binding, refresh count zero, and typed closure on
  drift/stop. Do not add retry, replay, session, ledger or TTL behavior.

## Independently Verified Passes

- Queue capture is under `synchronized(exactContext)` and freezes caller binding object identity plus the epoch;
  `InputActionRequest` stores an immutable copy. The synchronous A -> B -> A stale-object rejection is valid.
- `InputSequences.java:110-117` exposes the required public facade and forwards the existing typed result.
- The click/sleep delivery uses one `InputActionRequest`, one queue offer/take, one global input transaction and
  one context monitor. `CountingQueue.take()` at test lines 665-680 delegates to the production `super.take()`;
  the success test at lines 358-399 genuinely observes exactly one taken request carrying the complete list.
- The named A -> B -> A case at test lines 518-555 is synchronous, typed
  `NOT_STARTED/WINDOW_BINDING_CHANGED`, and asserts zero take/focus/input/refresh. It is not sleep-race evidence.
- The per-action pause/resume case continues the same request without re-enqueue/replay. Cancellation and external
  binding drift stop later actions; the STOP typing exception is P1-1 above.
- The frozen callback branch remains separately routed through `runFrozenExactWindowExclusive`; no change to its
  callback/finally completion barrier was found in the reviewed bytes.
- The real dispatcher preserves baseline commit `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` for the TURN-22 mouse
  bundle: `CLICK_LEFT(..., 150)` followed by queue-owned `SLEEP(500)` in the same transaction. No auto retry,
  replay, durable session, owner, ledger or TTL was added to this frozen factory/path.
- The new action-list cases do not call `Harness.resolve(...)`, `Unsafe.allocateInstance(...)`, or
  `waitUntilQueued(...)`. The class still contains pre-existing `Unsafe` and `Thread.sleep(1)` polling fixtures for
  older callback cases (`test:48,557-563,767-783`), but TURN-28Q did not add or use them for its action-list proof.

## Named Test Assessment

| Required proof | Independent assessment |
| --- | --- |
| Public `InputSequences` facade | Proven by source |
| Real in-memory queue/worker | Proven by source |
| One taken request with complete immutable list | Proven by source |
| Exact focus for `CLICK_LEFT -> SLEEP` | Proven by source |
| Typed A -> B -> A zero-input rejection | Proven by source |
| Typed stop-only closure | **Not proven; current pause+stop test masks P1-1** |
| Exact binding for every supported action | **Not proven; Alt path violates it (P1-2)** |
| No newly added Unsafe/sleep race | Proven for the new action-list cases |
| Callback barrier non-regression | No static regression found |

## Reviewed Snapshot

The five reviewed files were rehashed immediately before this report:

| File | Lines | SHA-256 |
| --- | ---: | --- |
| `InputSequences.java` | 210 | `b293e0c6792303d45a4314050c6e4f1c8b39d0f4dea426632586ed0f292dacb3` |
| `InputActionQueue.java` | 850 | `66fa536ef8b4c6cbf8874cd94d8842fd8b0f9d3f4e74bc52719f31f39e4660bf` |
| `InputActionRequest.java` | 1118 | `23973b7eee06949138e8a2841e249c009eb69184804c2be0689aa317c29988de` |
| `InputActionWorker.java` | 748 | `7489084b773e6066213d383af86c82ac9c3431fb9e2d1d5acf3e9c11d423eac0` |
| `InputActionFrozenExclusiveContractTest.java` | 852 | `475399ef8656c7d193bfeb6f18ba69b7e01d4c531710367e74d00165ded03c44` |

I independently read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, the CR271 section at the top of
`docs/ACTIVE_WORK.md`, the complete TURN-28Q fixed card through its physical true EOF, TURN-28P, TURN-22,
`docs/业务逻辑.md`, baseline `696a12b0`, all five files above, and the directly invoked token/binding/focus/caller
dependencies. The existing parent/reviewer verdicts were read only as required context and were not used as the
basis for this result.

## Verification Boundary

Per the assignment, I did not run Maven, JUnit, compile, package, runtime, application/server, Task/UI, capture or
physical input. I performed no Git mutation and did not modify Java, tests, cards, plans, `ACTIVE_WORK`, CR271,
matrix or dashboard files. Existing dirty/untracked work was left untouched. This report is the only write.

## Delivery Verdict

**BLOCKED. P0/P1/P2 = 0/2/0.** TURN-28Q is not delivery-approvable until stop-only/final-stop typed closure and
exact no-refresh Alt dispatch are repaired and independently re-reviewed against deterministic real queue/worker
evidence.

<!-- TRUE_EOF: CR271 TURN-28Q INDEPENDENT DELIVERY REVIEW R1 BLOCKED P0/P1/P2=0/2/0 EVIDENCE=P1-STOP-RELABEL-AND-FINAL-STOP-LOSS+P1-FROZEN-ALT-SELF-REFRESH 2026-07-16T09:30:53.1766088-04:00 -->

## Latest Integrated Delivery Re-review - 2026-07-16T10:53:38.8771939-04:00

- Reviewer role remains independent R1: not an implementation owner and not the parent/final reviewer.
- This round reviews the integrated TURN-28Q + TURN-28QT1 Repair #1 + TURN-28QP1 bytes. It supersedes this
  report's 09:30 verdict for the current snapshot; the earlier `BLOCKED` section remains append-only history.
- Parent Review #5 was used only to locate the claimed final snapshot. The verdict below comes from an
  independent full read of the three original cards through physical true EOF and a fresh line-by-line review
  of the four current production/test files and their directly invoked token/focus/keyboard facades.
- Independent result: **P0/P1/P2 = 0/0/0 - APPROVED**.

### Reviewed Snapshot

| File | Lines | SHA-256 |
| --- | ---: | --- |
| `InputActionRequest.java` | 1148 | `7f4f8fdc4baf90f613fa6556f2a860849933724a6de5591d07d1eeb2d91ab0c8` |
| `InputActionQueue.java` | 870 | `c53a423e98e7ba4d698caa937788e5c6654100971ed8c24a9daef645a7173b6a` |
| `InputActionWorker.java` | 811 | `225a9f3be56d18f0374f78c9b3bea7352e8b4db444d288ecf7f1d51511377f43` |
| `InputActionFrozenExclusiveContractTest.java` | 1283 | `f72c7db076f7944555d3c89b7a8a1f3b1a2e6f396efe71c5ca00d801c07fd38c` |

These hashes independently match the final child-card snapshots. Hash agreement identifies the reviewed bytes;
it is not the basis of approval.

### Findings

- **P0: none.**
- **P1: none.**
- **P2: none.**

### Independent Closure Evidence

1. **Typed stop and final gate are closed.** `InputActionRequest.java:448-463` is now a pure object/epoch
   generation witness; typed safety remains in `frozenExactWindowFailure` / `checkDetailedSafety` and the frozen
   detector (`:474-480`, `:722-739`, `:927-958`). `InputActionQueue.java:403-441` checks the typed failure before
   the generation witness. `InputActionWorker.java:529-548` repeats typed safety plus object identity before each
   action and after the final action, before success publication. The real public queue/worker cases independently
   pin pre-enqueue stop, stop-only after action 0, and stop during the final action at test `:736-860`; all assert
   `STOP_REQUESTED`, truthful prefix progress, zero later input, and one take only when actually enqueued.
2. **Frozen Alt stays on the exact binding with zero refresh.** `InputActionWorker.java:594-655` selects
   `pressShortcut(request.getNativeBinding(), request.getWindowId(), shortcut)` for frozen requests, uses
   `focusFrozenBindingInActiveTransaction` for fallback, and rechecks typed ownership before irreversible real
   input. It never calls the mutable keyboard/focus overload on this path. Tests `:519-659` cover background
   success, `attempted=false` fallback, attempted-but-failed fallback, and deterministic A -> B -> A drift before
   fallback. `CountingKeyboardService` and the ordered `CountingFocusService` (`:1040-1071`, `:1098-1128`) prove
   exact object identity on every call, mutable-overload count zero, refresh count zero, one fallback input at
   most, and zero real input after drift.
3. **A -> B -> A cannot regain authority.** Queue capture is under `synchronized (exactContext)` and requires the
   exact binding object before freezing the epoch (`InputActionQueue.java:413-440`); the worker holds that context
   monitor across exact focus and the complete list (`InputActionWorker.java:501-549`). The typed synchronous
   action-list case at test `:865-906` proves a field-identical replacement object is rejected before take,
   focus, refresh, or input. The mid-Alt replacement case separately proves the worker-side witness.
4. **Pause cancellation and the real barrier are closed.** Frozen waits use
   `waitIfPausedRevision(stopToken, request::shouldAbortPauseWait)` (`InputActionWorker.java:385-397`), so waiter
   cancellation releases a genuinely paused request without `resume()`, re-enqueue, or transaction leakage.
   `BarrierPauseToken` at test `:1222-1234` announces only when production evaluates the wake condition inside an
   active pause loop; the pause/resume and no-resume cancellation cases at `:461-510` and `:670-730` therefore
   establish ordering rather than polling timing. The latter also proves the same worker accepts a follow-up
   request after cancellation.
5. **One queue request owns the complete 696 sequence.** `InputActionRequest.java:231,278-291` copies the complete
   list immutably into one request. Test `:361-400` drives the public `InputSequences` facade through the real
   queue/worker and observes exactly one taken request carrying
   `[CLICK_LEFT(x=300,y=400,delay=150), SLEEP(500)]`, one exact focus, one physical click, and completed step 1.
   This preserves the `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` single-queue ownership and
   `150ms click + 500ms queue hold` behavior without an extra move, click, retry, or timing split.
6. **Compile surface has no remaining static blocker in the reviewed slice.** The QP1 reference is fully
   qualified at `InputActionRequest.java:458`; the test has the required `assertSame` static import at `:45`;
   its overrides match the public/package-visible production signatures, and the file contains 19 `@Test`
   methods. This is static source review only, not a compile claim.
7. **No new retry/session/ledger/TTL workflow is reachable from this API.** The frozen action-list factory is
   non-deadline, non-retained, callback-null and creates one request; queue submission offers it once; the worker
   iterates that request's immutable list once. Alt permits one background attempt followed by one focused
   fallback only when non-terminal, never a retry. Existing retained-session/deadline machinery elsewhere in
   these shared files is a disjoint pre-existing path and is not entered by `frozenExactWindowActions`.

### Verification Boundary

Per assignment, this round ran no Maven, JUnit, compile/package, runtime/application/server, Task/UI, capture or
physical input. It ran no Git command or mutation and changed no Java, test, card, plan, `ACTIVE_WORK`, matrix or
dashboard file. The user-prohibited Git boundary means the `696a12b0` comparison here uses the fully read business
baseline and frozen card's exact single-queue/timing contract, corroborated by the current production action list;
no repository object was queried. Existing dirty/untracked content was left untouched. This append is the only
write.

### Latest Delivery Verdict

**APPROVED. P0/P1/P2 = 0/0/0.** The current integrated TURN-28Q source/test-source slice closes both former R1
P1 findings and satisfies the frozen exact-window action-list contract under static independent review. Named
test execution and DHXY compile remain separate authorized gates; this R1 approval is not parent/card approval.

<!-- TRUE_EOF: CR271 TURN-28Q INDEPENDENT DELIVERY REVIEW R1 LATEST-INTEGRATED APPROVED P0/P1/P2=0/0/0 SNAPSHOT=REQUEST-7f4f8fdc+QUEUE-c53a423e+WORKER-225a9f3b+TEST-f72c7db0 STATIC-ONLY BUILD-NOT-RUN 2026-07-16T10:53:38.8771939-04:00 -->
