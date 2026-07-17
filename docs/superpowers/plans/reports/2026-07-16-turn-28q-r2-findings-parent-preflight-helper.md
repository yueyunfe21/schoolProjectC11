# CLAIMED: CR271 TURN-28Q parent preflight helper

- Helper UUID: `69e6109f-2c10-4c91-befd-7677c8aba423`
- Nickname: `Caliper`
- Claimed at: `2026-07-16T11:09:35.7217264-04:00`
- Role: CR271 Internal helper only. Not implementation owner, independent reviewer, parent reviewer, approver, or blocker.
- Deliverable boundary: byte-level preflight evidence for the parent. This report does not repeat, replace, approve,
  block, or otherwise alter any reviewer/parent verdict.

## Scope And Snapshot

This pass fully read the required project instructions/context, the CR271 top status, authoritative plan sections
14-19, the HTTPS turn protocol and foundation, `docs/业务逻辑.md`, the complete TURN-28Q card and its physical latest
true EOF, both independent review reports through their latest true EOF, the current queue/worker/request and safety
token sources, direct TURN callers, the current test fixture, and baseline
`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.

At report claim time the relevant current bytes were:

| File | Lines | SHA-256 |
|---|---:|---|
| `InputActionQueue.java` | 870 | `c53a423e98e7ba4d698caa937788e5c6654100971ed8c24a9daef645a7173b6a` |
| `InputActionWorker.java` | 811 | `225a9f3be56d18f0374f78c9b3bea7352e8b4db444d288ecf7f1d51511377f43` |
| `InputActionRequest.java` | 1148 | `7f4f8fdc4baf90f613fa6556f2a860849933724a6de5591d07d1eeb2d91ab0c8` |
| `InputActionFrozenExclusiveContractTest.java` | 1283 | `f72c7db076f7944555d3c89b7a8a1f3b1a2e6f396efe71c5ca00d801c07fd38c` |

Both repositories already contained extensive dirty/untracked work. This helper did not mutate Git, touch the cloud
repository, or alter any existing source, test, card, plan, `ACTIVE_WORK`, package/dashboard, or dirty/untracked file.
While this report was being prepared, the original card acquired a later Parent Review #6 true EOF at
`2026-07-16T11:03:03.155-04:00`. That parent record is merely the current coordination state; the evidence below is
an independent helper trace and is not a parent decision.

## Byte-Level Result

The two questioned typed-order paths are both present in the hash-pinned bytes. In each path, a raw generation
witness/comparator can publish `WINDOW_BINDING_CHANGED` before the frozen typed detector is consulted, even when
the same request's stop token is already closed. These facts establish call-path reachability only; they are not a
review verdict.

## Finding A: Queue Admission

### Callback entry

The exact public path is:

1. `TurnCaptureStepExecutor.java:214-218` calls
   `InputSequences.submitFrozenExactWindowExclusiveAndWait(...)` for the pixel-change probe.
2. `InputSequences.java:80-86` delegates without flattening the result.
3. `InputActionQueue.java:337-347` captures task tokens and enters `synchronized (exactContext)`.
4. `InputActionQueue.java:348-355` tests raw binding object identity before constructing the normal request. If the
   context publishes a field-equal replacement `A'` instead of the supplied frozen object `A`, it immediately
   returns `WINDOW_BINDING_CHANGED/frozen-generation-changed-before-enqueue`.
5. The typed detector is reachable only later at `InputActionQueue.java:363-367`.

### Action-list entry

The parallel public path is:

1. `TurnInputStepExecutor.java:185-188` calls
   `InputActionQueue.submitFrozenExactWindowActionsAndWait(...)` under the resolved context.
2. `InputActionQueue.java:403-413` copies the complete list, captures task tokens, and enters the context monitor.
3. `InputActionQueue.java:414-421` performs the same raw object-identity shortcut and returns binding drift.
4. The typed detector is reachable only at `InputActionQueue.java:429-433`.

### Deterministic collision

Use one logical `windowId`, freeze binding object `A`, then publish `B` followed by field-equal object `A'`, close
the captured task stop token, and submit with stale object `A`. The first raw shortcut is true in both entries, so
the request never reaches either typed call. By contrast, if `frozenExactWindowFailure(...)` runs first,
`InputActionRequest.java:932-954` keeps the logical window id constant, observes stop at `:938-942`, and only then
examines identity/binding drift at `:943-954`. Its result for this exact collision is therefore
`NOT_STARTED/STOP_REQUESTED`.

Current observable result and containment:

- Terminal is `NOT_STARTED/WINDOW_BINDING_CHANGED`, with no queue offer/take, focus, refresh, callback, or input.
- `TurnInputStepExecutor.java:196-202` recognizes only `STOP_REQUESTED` as stopped; the mislabeled result becomes
  `INPUT_QUEUE_FAILED`.
- `TurnCaptureStepExecutor.java:374-390` likewise relies on the queue's typed stop when the resolve-time metadata,
  callback state, and caller interrupt do not contain the later stop; the mislabeled result becomes
  `PIXEL_PROBE_FAILED`.
- Thus this collision does not leak physical input or fabricate success. It corrupts terminal stop semantics and
  the resulting TURN outcome/diagnostic classification.

## Finding B: Worker Preamble And Monitor Acquisition

### Common worker preamble

The exact queue-to-worker path is:

1. A frozen request passes queue admission while stop and identity are clear, is offered once by `await(...)`, and
   is taken once by the real worker.
2. `InputActionWorker.handle(...)` calls `waitIfPaused(request, "before-focus")` at `:98`.
3. For a non-paused frozen request, worker `:387-396` delegates to
   `TaskPauseToken.waitIfPausedRevision(...)`; `TaskPauseToken.java:110-113` returns immediately when not paused
   and does not inspect the stop token on that branch.
4. `InputActionWorker.java:109-112` then calls the generic `isPlayerIdentityEpochCurrent(...)` before
   `checkDetailedSafety(...)` at `:113-115`.
5. If stop and player identity epoch drift close after take, `isPlayerIdentityEpochCurrent(...)` at worker
   `:575-588` publishes `WINDOW_BINDING_CHANGED` and returns. The frozen detector never sees the closed stop.

This produces one taken request and `NOT_STARTED/WINDOW_BINDING_CHANGED`, with no input transaction/focus/action.
The same TURN caller projections described above then degrade a stop to an input/probe failure.

### Context-monitor acquisition

Both frozen execution modes have a second, distinct ordering window:

- Callback: worker `:424-428` performs typed safety outside the context monitor, then `:436-440` performs only the
  raw generation witness after monitor acquisition and before exact focus.
- Action list: worker `:491-495` performs typed safety outside the monitor, then `:503-507` performs only the raw
  witness after acquisition and before exact focus.

If the outside typed check passes, the worker waits for the context monitor, and stop plus generation drift close
before that monitor is acquired, the first post-acquisition observation is the raw witness. It publishes
`WINDOW_BINDING_CHANGED`; no post-acquisition typed check gets an opportunity to publish `STOP_REQUESTED`.

This path has one take and has already entered the global input transaction, but it fails before exact focus,
callback/action start, or physical input. The minimal repair does not make Java monitor acquisition interruptible
and should not change monitor ownership; it only restores typed revalidation immediately after acquisition.

`InputActionWorker.java:563-572` already contains the required operation in
`isFrozenExactWindowStillOwned(...)`: `checkDetailedSafety(stage)` first, pure object/epoch witness second. The two
pre-focus monitor sites do not currently use that ordering.

## Minimal Repair Write Set

The minimum production/test byte write set is exactly three existing DHXY files:

1. `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java`
2. `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
3. `src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java`

Required in-place production direction:

- In both frozen queue entry methods, remove the pre-request object-identity shortcut. Under the existing context
  monitor, construct exactly one request, call `frozenExactWindowFailure(...)`, and only if clear call the pure
  `isFrozenExactWindowGenerationCurrent()` witness. Keep one offer and the current immutable list.
- In `handle(...)`, route frozen requests through `checkDetailedSafety(...)` before any generic epoch cancellation.
  The generic legacy ordering must remain unchanged. The frozen detector already covers epoch drift, so no new
  safety abstraction is needed.
- In both post-monitor/pre-focus sites, use `isFrozenExactWindowStillOwned(...)` or the exact equivalent
  typed-then-witness sequence. Do not move focus or mechanics outside the current monitor/transaction.

`InputActionRequest.java` should remain read-only: its detector precedence and pure witness separation are already
sufficient for these two collisions. `InputSequences.java`, TURN callers, focus/keyboard services, POMs, and every
other production file are also outside the minimum repair. The implementation owner must append claim/delivery
evidence to the original card as required by process, but that append is process persistence, not an additional
mechanics repair file; this helper does not perform it.

Scope boundary: the deterministic collisions above preserve the logical `windowId`. The detector currently checks
logical window-id loss before stop at `InputActionRequest.java:932-942`; changing that separate precedence is not
needed to repair the two verified generation witness/comparator inversions and should not be introduced without a
parent-authorized scope decision.

## Deterministic Acceptance

All cases must use fake input with the real public facade, real in-memory queue, and real worker. They must not use
runtime/UI/capture/physical input, `Thread.sleep`, queue-size polling, or an inferred timing window.

### A. Pre-enqueue collision

Exercise both duplicated public frozen entries, callback and action-list, with stop already closed plus
`A -> B -> A'` under one unchanged logical `windowId`.

Assert for each variant:

- `status == NOT_STARTED`
- `safetyReason == STOP_REQUESTED`
- `startedStepIndex == -1` and `lastCompletedStepIndex == -1`
- zero queue take, focus, refresh, callback/action, and fake input
- no retry, replay, second request, session, ledger, TTL, or durable workflow

### B. Worker preamble collision

Extend the existing `CountingQueue.take()` fixture with an armable one-shot take barrier. After `super.take()` has
returned and the request has been recorded, signal `taken` and block before returning it to `handle(...)`. The test
thread then closes stop and performs a hard identity/epoch drift, releases the barrier, and awaits the typed result.

Assert `NOT_STARTED/STOP_REQUESTED`, truthful `-1/-1` progress, exactly one take, and zero transaction focus/input.
This directly proves the common `handle(...)` ordering without a preceding blocker or polling.

### C. Monitor-acquisition collision

This must be a separate stage case. Once the repaired preamble observes a closed stop it short-circuits, so the
preamble test cannot also prove the later monitor boundary.

Use a test-only latch/event fixture that:

1. admits and takes the request while all gates are clear;
2. lets the outside `before-frozen-*` typed check capture the still-current generation;
3. holds the context monitor before the worker can acquire it;
4. closes stop and publishes a replacement generation under that monitor;
5. releases the monitor and lets the worker execute its first post-acquisition gate.

A stage-aware test context/token hook may signal immediately after the outside detector captures the old binding;
the hook must be test-only and event-driven. Run the post-monitor assertion for both callback and action-list
variants, either as separate tests or one table-driven helper, because the production sites are separate.

Assert `NOT_STARTED/STOP_REQUESTED`, `-1/-1` progress, exactly one take, zero exact focus/callback/action/input, zero
refresh, and no replay. The assertion must fail on the hash-pinned bytes by observing `WINDOW_BINDING_CHANGED` and
pass only when post-acquisition typed safety precedes the witness.

### D. Baseline non-regression

Retain the existing real queue/worker success proof for one immutable request containing exactly
`CLICK_LEFT(delay=150)` followed by `SLEEP(500)`. Baseline `696a12b0` `TeamReturnService.java:86-89` owns those two
steps in one queue submission. The repair changes only competing terminal-reason precedence; it must not alter
click order, delay/hold timing, focus ownership, retry/fallback order, or queue atomicity.

The applicable future gate remains the already authorized `HTTPS_TURN_CONTRACT_TEST_FAMILY`, including
`InputActionFrozenExclusiveContractTest`; this helper did not run it. Java compile is likewise a later owner/parent
gate after the writer is stable, not evidence claimed here.

## Helper Boundary

No Maven, JUnit, compile/package, runtime/application/server, Task/UI, capture, or input was run. No Git mutation was
performed. This report is the sole write. It is a `PRECHECK_COMPLETE` helper artifact for parent use and explicitly
is not parent approval, reviewer approval, or a card status decision.

<!-- TRUE_EOF: CR271 TURN-28Q R2-FINDINGS PARENT-PREFLIGHT HELPER UUID=69e6109f-2c10-4c91-befd-7677c8aba423 NICKNAME=Caliper PRECHECK_COMPLETE NON-PARENT-APPROVAL 2026-07-16T11:10:27.6136698-04:00 -->
