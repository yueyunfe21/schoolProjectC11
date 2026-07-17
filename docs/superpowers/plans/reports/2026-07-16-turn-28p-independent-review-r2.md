# CR271 TURN-28P Repair #2 Independent Delivery Review R2

- Reviewer role: independent delivery reviewer R2; not the implementation owner and not the parent/final adjudicator.
- Review time: 2026-07-16T08:49:53.792-04:00.
- Decision: **APPROVED**.
- Severity count: **P0/P1/P2 = 0/0/0**.
- Gate represented by this report: source and test-source delivery review only. Named tests, Maven compile/build, runtime, capture, focus, keyboard, mouse and other physical input were not run, as explicitly prohibited for this review.
- Independence: this review was derived from the authoritative card delivery, required specifications, `696a12b0` business baseline and actual production/test sources. It did not import an R1 conclusion.

## Reviewed authority and workspace state

Read in full before judgment:

- `AGENTS.md` and `docs/DHXY_CONTEXT.md`;
- the top CR271 block in `docs/ACTIVE_WORK.md`;
- sections 14-19 of `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md`;
- `docs/superpowers/plans/2026-07-15-https-turn-protocol-foundation.md` and the HTTPS turn protocol/specification material referenced by the card;
- `docs/业务逻辑.md`, with `696a12b0` treated as the sole business-logic baseline;
- both repository statuses (`DHXY` HEAD `0114604e`; Cloud HEAD `3b988ca`). These are repository HEADs, not competing business baselines;
- the complete TURN-28P card through its current true EOF, including Euler's delivery at card lines 1072-1125;
- all TURN-28P production/test files named below and the two Cloud contract tests.

Both repositories were already heavily dirty/untracked. This review performed no cleanup, restore, stage, commit or other Git mutation.

## Delivery integrity and review boundary

Euler's exact two-test delivery is still byte-identical to its true EOF declaration:

| Delivered file | Delivery SHA-256 | Current SHA-256 | Result |
|---|---|---|---|
| `TurnCapturePixelChangeProbeContractTest.java` | `5d563bbb08747c7b298ec6c7c0795a600269bc86d8f5769bcc67588268fda818` | same | exact |
| `LocalTurnActionExecutorContractTest.java` | `88011cf17b24e68b8dcf5c7ef11edd30fb8a9df2aac27e639e320e3bd4dd3709` | same | exact |

The card froze Euler's write set to those two tests plus the append-only card (`TURN-28P` card lines 1077-1081), and the current hashes match the delivered hashes at lines 1111-1114.

After that delivery, TURN-28Q legitimately changed the shared action-list side of `InputActionQueue`, `InputActionRequest`, `InputActionWorker`, `InputSequences` and `InputActionFrozenExclusiveContractTest`. I reviewed the current callback path reached by TURN-28P and confirmed it remains intact, but I do not approve or reject TURN-28Q's disjoint action-list delta in this report.

## Findings

No actionable P0, P1 or P2 finding was found.

### 1. Public resolver reaches the real queue and worker

- `TurnExecutionWindow.resolveForAction(...)` starts at `TurnExecutionWindow.java:51`. It resolves the runner/context, performs one refresh, validates native binding geometry and returns the immutable action window snapshot; the probe tests enter through this public boundary rather than a private helper.
- The production frozen callback API starts at `InputActionQueue.java:337`. It accepts the exact context/binding but no caller-provided epoch. Under the context monitor it requires binding object identity, snapshots the live epoch and creates the frozen request before waiting for a typed result.
- Real execution is selected by `InputActionWorker.java:130` and enters `runFrozenExactWindowExclusive(...)` at `:403`. Exact generation validation, frozen focus, callback execution and terminal cleanup remain inside the serialized real worker path.
- `TurnCapturePixelChangeProbeContractTest.java:485-652` builds a real in-memory `WindowTaskContextHolder` / `GlobalInputLock` / `InputActionQueue` / `WindowAwareInputCoordinator` / `InputActionWorker`; `:549-556` uses the public resolver and `InputSequences(realQueue)`. Its counting queue at `:626-647` delegates to production `super` and does not run the callback or manufacture an `InputActionExecutionResult`.
- `LocalTurnActionExecutorContractTest.java:579-661,1077-1101` independently uses the same real queue/worker path. The public whole-action proof at `:350-400` penetrates `LocalTurnActionExecutor.execute(...)` through resolver, queue, worker, focus, capture, keyboard and input mechanics.

This closes the previous synchronous `ProbeInputSequences`/manual-result weakness for the TURN-28P probe seam. Older `RecordingInputQueue` use in unrelated mouse-step tests is outside this probe contract and is not used as TURN-28P evidence.

### 2. Exact HWND and generation failures are closed before mechanics

- `InputActionRequest.detectFrozenExactWindowFailure(...)` starts at `InputActionRequest.java:897`. It checks window identity, task STOP, suspension/epoch and every exact binding field: native handle, process id, x, y, width and height.
- Binding object identity is part of generation matching before that field comparison (`InputActionRequest.java:425-450`), so a value-equal replacement binding is still a new generation.
- `InputActionFrozenExclusiveContractTest.java:240-285` independently mutates each exact HWND/process/geometry field and verifies zero callback/input. Its A -> B -> A value-equal replacement proof at `:295-343` also verifies zero callback/input/focus and no extra refresh.
- `TurnCapturePixelChangeProbeContractTest.java:315-353` exercises A -> B -> A while queued through the real worker harness and rejects before focus, capture, Ctrl or MOVE.

There is no second resolver refresh, drift retry or caller-controlled generation comparator on the reviewed callback route.

### 3. STOP, uncertainty and Ctrl-UP completion are fail-closed

- The probe is submitted once through the frozen callback at `TurnCaptureStepExecutor.java:214`. Ctrl-UP is attempted from `finally`; even a non-`RuntimeException` release failure sets `releaseFailed` (`:320-336`). Projection order begins with `CTRL_RELEASE_FAILED` at `:371-372`, then typed STOP/safety at `:374-387`, then incomplete queue/mechanics failure, and only then changed/unchanged completion.
- The worker maps `TaskStopRequestedException` to `STOP_REQUESTED` (`InputActionWorker.java:209-211`) and normalizes any other `Throwable` before publishing terminal completion (`:212-224`).
- Cooperative cancellation is requested in `InputActionRequest.java:701-703,775-777,928+`; the queue does not let an interrupted waiter return before the started callback has published its terminal result. The worker keeps exact generation, callback and finally cleanup within the context monitor.
- `TurnCapturePixelChangeProbeContractTest.java:174-218` proves one Ctrl-UP attempt and release-failure precedence, including a non-runtime release throwable. `:222-292` covers interruption and STOP before admission. `:299-312` proves a non-STOP incomplete worker result becomes `PIXEL_PROBE_FAILED`, not false STOP or success. `:356-385` uses latches/atomics to prove the caller returns only after Ctrl-UP settle and that no later mechanics run.
- `LocalTurnActionExecutorContractTest.java:403-498` verifies the same failure/STOP/no-completed-frame rules through the public whole-action executor and verifies failure evidence is captured only after Ctrl release.

No terminal or uncertain path can project a completed probe code/frame, and no automatic retry was found.

### 4. Cloud correlation and one-command uncertainty remain credible

- Cloud frame validation rejects a completed probe with a missing/wrong code at `TurnCapturePixelChangeInvocationContractTest.java:61-88`, a frame-only result at `:91-106`, terminal masquerading at `:110-134`, and duplicate/uncertain completed evidence at `:137-171`. ROI, dimensions, SHA and raw PNG validation continue at `:175-222`.
- `TurnGameClientContractTest.java:297-324` separately proves timeout and interruption remain typed uncertainty with one UUID, one command and zero retry.
- Correlation rejection tests at `TurnGameClientContractTest.java:385-464` cover step count, step type, action id, device and window mismatches without retry. Ordered payload and typed local projection remain covered at `:327-363`.

### 5. Contract-test credibility checks

For the two delivered tests, searches found zero use of the removed synchronous `ProbeInputSequences`, zero manual `InputActionExecutionResult.builder()` result construction, zero source scanning and zero `Thread.sleep` race guessing. Concurrency proofs use real worker threads plus latches/atomics. Reflection is limited to inert test allocation of `WindowTaskRunner`; no private production decision helper is invoked reflectively.

## Decision

**APPROVED - P0/P1/P2 = 0/0/0.**

TURN-28P Repair #2 source and test-source meet this independent R2 delivery gate for the reviewed callback/probe contract: public resolver -> real queue/worker, exact HWND and generation rejection, typed STOP/uncertain/correlation behavior, Ctrl-UP finally/completion barrier and A -> B -> A drift all have production-penetrating evidence. No approved business difference from `696a12b0` was found.

This is not the parent's final card approval and is not build evidence. Named tests and applicable compile/build remain pending the parent-controlled stable-writer window.

<!-- TRUE_EOF: TURN-28P INDEPENDENT DELIVERY REVIEW R2 APPROVED P0P1P2=0/0/0 SOURCE-TEST-SOURCE 2026-07-16T08:49:53.792-04:00 -->
