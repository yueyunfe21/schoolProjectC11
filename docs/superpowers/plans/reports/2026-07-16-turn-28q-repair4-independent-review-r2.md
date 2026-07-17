# TURN-28Q Repair #4 Independent Whole-Card Delivery Review R2

- Role: independent whole-card delivery reviewer R2; not the implementation Worker and not the parent final
  adjudicator.
- Independence: this review did not read or rely on the R1 report.
- Verdict: **BLOCKED**.
- Severity: **P0/P1/P2 = `0/2/0`**.
- Reviewed snapshot:
  - `InputActionQueue.java`: SHA-256
    `0d1bc01fbce331b75a958f105e3c21d50048aafcc5ed0b973edf0dcc30241057`.
  - `InputActionWorker.java`: SHA-256
    `5d41a074e403a154e05611a31b0f2db5dc8684b064c76603fbfa1293c452a8c6`.
  - `InputActionFrozenExclusiveContractTest.java`: SHA-256
    `36637672d091eab709bc8d222cc75fd21b3823b3d2f943aa1f4d199ba93f0974`.
- Authority checked: `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/ACTIVE_WORK.md` CR271 head,
  authoritative plan sections 14-19, HTTPS turn protocol/foundation specifications, `docs/业务逻辑.md`, the
  TURN-28Q card through its physical true EOF, both repository statuses, and business baseline `696a12b0`.
  There is no approved business difference.

## Accepted production and Repair #4 behavior

- `InputActionQueue.java:337-430` constructs one frozen request under the exact context monitor. Both callback
  and action-list entries run the typed frozen failure detector before the object-identity generation witness;
  no caller epoch, title search, refresh or second comparator is introduced on this path.
- `InputActionWorker.java:109-145,437-475,507-592` preserves the same typed-safety-before-witness order at the
  worker preamble and after acquiring the generation monitor. Exact focus plus the complete action list stay in
  one input transaction/monitor; STOP, pause, interruption and drift return before a later action starts.
- `InputActionFrozenExclusiveContractTest.java:362-403` observes one real queue take carrying immutable
  `CLICK_LEFT(delay=150) -> SLEEP(500)`, exact frozen focus, zero refresh and truthful step completion.
- Repair #4 itself is deterministic: `InputActionFrozenExclusiveContractTest.java:793-856` installs a
  target-specific `onTake` event; `CountingQueue.take()` at `:1159-1167` first delegates to production
  `super.take()`, records the request, then closes STOP plus native-generation drift before the worker preamble.
  Assertions distinguish blocker take #1 and target take #2, correlate the target request id, require typed
  `STOP_REQUESTED/NOT_STARTED`, and prove zero target input/focus/refresh plus no replay.
- No source scan was found. No automatic retry/replay/session/ledger/TTL/durable workflow was added by the
  TURN-28Q frozen action-list path.

## P1-1 - Whole-card callback non-regression still uses sleep polling

- The frozen contract at TURN-28Q card lines 63-66 requires the named test to prove callback-path
  non-regression with no sleep-based race proof. The current whole-card test still calls
  `waitUntilQueued(...)` at test lines `97`, `190`, `279`, and `321` for cancellation, queued drift, every-field
  drift, and A -> B -> A' callback cases.
- `waitUntilQueued(...)` at test lines `1033-1038` polls `queue.size()` and uses `Thread.sleep(1L)`. This is a
  wall-clock race proof: success depends on observing an intermediate queue state before the worker changes it.
  These cases are part of the card's required callback-path non-regression evidence, so they cannot be excluded
  merely because Repair #4's new taken test no longer calls the helper.
- Impact: the named whole-card gate remains timing-sensitive and does not satisfy the explicit no-polling
  acceptance contract. A fast or delayed worker can make the evidence flaky or miss the intended queued
  boundary.
- Required repair: replace every remaining `waitUntilQueued` use in this named test with deterministic
  queue/take admission events or another latch/barrier tied to the real queue/worker boundary. Remove the helper
  and its `Thread.sleep(1L)` polling. Do not replace it with another timed polling loop.

## P1-2 - Whole-card named test still performs private reflection through Unsafe

- The class initializes `UNSAFE` at test line `52`. `findUnsafe()` at test lines `1318-1325` calls
  `Unsafe.class.getDeclaredField("theUnsafe")` and `field.setAccessible(true)`, which is direct private
  reflection forbidden by TURN-28Q card lines 63-66 and by this whole-card review contract.
- `Harness.resolve(...)` at test lines `1103-1116` calls `allocate(BareWindowTaskRunner.class)`; `allocate(...)`
  at `1310-1315` invokes `UNSAFE.allocateInstance(...)`. The reflected singleton is initialized for the whole
  test class, so this is not isolated documentation or dead source: the required callback-path tests depend on
  a private-reflection fixture.
- Impact: the named test can fail at class initialization under stricter JDK/module access and its evidence
  bypasses normal object construction. More importantly, it directly violates the frozen no-private-reflection
  acceptance boundary, so the whole card cannot be approved on this SHA.
- Required repair: remove `Unsafe`, `Field`, `findUnsafe()` and `allocate()`. Construct the required public-path
  fixture through normal constructors/public collaborators, or reshape the test harness so exact context and
  binding are supplied without bypassing constructors. Do not substitute source scanning or reflection on
  another private member.

## Gate record

- This is a source/test-source review only. No Java or authoritative card/plan file was modified.
- Maven, JUnit, compile, package, runtime, application, server, Task, UI, capture and input were not run.
- No Git mutation was performed; both repositories' dirty/untracked state was preserved.
- Re-review must cover the complete card on the new SHA. Repair #4's deterministic taken-boundary evidence is
  accepted, but it does not waive the two whole-card test-contract blockers above.

TRUE_EOF REVIEW_COMPLETE
