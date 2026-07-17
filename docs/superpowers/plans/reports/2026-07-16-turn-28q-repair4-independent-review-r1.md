# TURN-28Q Repair #4 Independent Whole-Card Delivery Review R1

- Role: independent whole-card delivery reviewer R1; not implementation owner and not parent final reviewer.
- Verdict: **BLOCKED**.
- Severity: **P0/P1/P2 = 0/2/0**.
- Reviewed scope: the complete TURN-28Q card through physical true EOF Parent Review #8, the frozen
  `InputActionQueue.java`, `InputActionWorker.java`, and `InputActionFrozenExclusiveContractTest.java`, the
  public facade/request seams needed to verify those files, HTTPS turn protocol, plan Sections 14-19,
  `docs/业务逻辑.md`, both repository statuses, and baseline `696a12b0`.
- Frozen SHA-256 values independently matched Parent Review #8:
  - `InputActionQueue.java`: `0d1bc01fbce331b75a958f105e3c21d50048aafcc5ed0b973edf0dcc30241057`
  - `InputActionWorker.java`: `5d41a074e403a154e05611a31b0f2db5dc8684b064c76603fbfa1293c452a8c6`
  - `InputActionFrozenExclusiveContractTest.java`: `36637672d091eab709bc8d222cc75fd21b3823b3d2f943aa1f4d199ba93f0974`

## Blocking findings

### P1-1 - The complete named test still uses forbidden private reflection

The frozen card explicitly requires the named test to use no private reflection. The current complete test
violates that contract:

- `InputActionFrozenExclusiveContractTest.java:28-30` imports `sun.misc.Unsafe` and reflection `Field`.
- `InputActionFrozenExclusiveContractTest.java:52` initializes a global `UNSAFE` through `findUnsafe()`.
- `InputActionFrozenExclusiveContractTest.java:1318-1322` calls
  `Unsafe.class.getDeclaredField("theUnsafe")`, makes the private field accessible, and reads it.
- `InputActionFrozenExclusiveContractTest.java:1309-1314` then uses `UNSAFE.allocateInstance(...)` to bypass
  constructors for the runner/manager harness used by `Harness.resolve(...)`.

This is not merely an implementation detail outside the card: it is inside the sole whole-card named test and
is used to assemble the path that produces `TurnExecutionWindow`. It directly contradicts the card's
`no private reflection` acceptance criterion and weakens the claimed public production-path evidence by
bypassing normal construction. Remove the private-reflection/Unsafe harness and construct the required inert
collaborators through legal public/package-visible test wiring. Do not replace it with source scanning.

### P1-2 - The complete named test still contains polling sleep race proof

The whole-card contract requires deterministic queue/worker evidence and forbids sleep-based race proof. Repair
#4 correctly removed polling from the new taken-boundary case, but the complete named test still relies on the
polling helper in four card-relevant exact-window/callback non-regression cases:

- calls at `InputActionFrozenExclusiveContractTest.java:97`, `:190`, `:279`, and `:321`;
- helper at `InputActionFrozenExclusiveContractTest.java:1033-1038` polls `queue.size()` until a wall-clock
  deadline and calls `Thread.sleep(1L)` on every iteration.

Those cases cover queued cancellation, queued binding drift, per-field exact-window drift, and A -> B -> A'
callback-path rejection. They are part of the same whole-card evidence, including the frozen callback
non-regression required by the card, so they cannot be exempted as an older Repair delta. Replace this helper
and all four uses with deterministic enqueue/take latch or event evidence from the real in-memory queue/worker.
No polling sleep or timing guess may remain in this named contract test.

## Boundaries independently accepted

- `InputActionQueue.java:398-430` creates one immutable frozen action-list request, evaluates typed frozen
  safety before the object-identity generation witness, performs no refresh/title search, and returns the
  existing typed result.
- `InputActionWorker.java:116-130`, `:520-568`, and `:582-591` preserve typed STOP/pause/drift ordering at the
  preamble, monitor acquisition, per-action, fallback, and completion boundaries. The exact context monitor and
  one input transaction cover focus and the complete action list; rejection has no automatic retry/replay.
- The Repair #4 case at `InputActionFrozenExclusiveContractTest.java:794-856` deterministically closes STOP and
  identity/generation drift after the target is returned by real production `take()`, distinguishes blocker
  take #1 from target take #2, binds the caller result to the target request id, and proves typed
  `STOP_REQUESTED`, zero target focus/input/refresh, and no replay without polling.
- The complete-list case at `:362-403` observes one taken request carrying immutable
  `CLICK_LEFT(delay=150) -> SLEEP(500)`, exact frozen focus, zero refresh, and truthful two-step completion.
- No source scan was found in the reviewed test. No approved business difference from `696a12b0` was found in
  the reviewed production path.

## Gate record

- This review is **BLOCKED** until both P1 findings are repaired on the same complete TURN-28Q card and the
  latest whole-card SHA set is reviewed again.
- No Java, original card, plan, ACTIVE_WORK, or dashboard file was modified.
- No Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input was run.
- No Git mutation was performed; both repositories' existing dirty/untracked state was preserved.

<!-- TRUE_EOF REVIEW_COMPLETE -->
