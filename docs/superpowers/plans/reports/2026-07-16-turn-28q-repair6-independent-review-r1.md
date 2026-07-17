# CR271 TURN-28Q Repair #6 Independent Whole-Card Review R1

## Verdict

**APPROVED**

- Role: `R1`, independent whole-card reviewer.
- Severity count: **P0 = 0, P1 = 0, P2 = 0**.
- Review object: the complete frozen whole-card artifact only. No contract expansion was used.
- This is a source/test-source review approval. Per the assignment, I did not run Maven, JUnit, compile, runtime, application, server, Task, UI, capture, or input. Those gates are not claimed by this report.

## Frozen Artifact Identity

| File | Reviewed SHA-256 |
|---|---|
| `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java` | `0d1bc01fbce331b75a958f105e3c21d50048aafcc5ed0b973edf0dcc30241057` |
| `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java` | `5d41a074e403a154e05611a31b0f2db5dc8684b064c76603fbfa1293c452a8c6` |
| `src/main/java/com/bot/dhxy/input/action/InputActionRequest.java` | `7f4f8fdc4baf90f613fa6556f2a860849933724a6de5591d07d1eeb2d91ab0c8` |
| `src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java` | `7d17722fb911331a2a51572f727bd280df9029b9a5ecad1ab9f4f94d219ab25e` |

The frozen test file is exactly **1451 physical lines** and contains exactly **21 `@Test` methods**. The original card was read through its actual final line; its canonical tail was:

`<!-- TRUE_EOF: TURN-28Q PARENT-INDEPENDENT-WHOLE-CARD-REVIEW-ASSIGNED R1-GIBBS-019f6c29-d104-7533-b41c-187c11218ff0 R2-PASCAL-019f6c29-e4ff-7b23-83fc-205de3801805 TEST-SHA=7d17722f DUAL-REVIEW-IN-PROGRESS 2026-07-16T14:21:00-04:00 -->`

## Findings

No P0, P1, or P2 findings.

## Exact Evidence

### Five public resolver-to-real-worker callback paths

All five required tests call the public `TurnExecutionWindow.resolveForAction(...)` through `Harness.resolve(...)`, then call public `InputActionQueue.submitFrozenExactWindowExclusiveAndWait(...)`. `CountingQueue.take()` delegates to the production `super.take()`, and a real `InputActionWorker` executes the callback. These are not synchronous queue doubles.

1. Normal frozen snapshot: test line 63; resolve line 65; refresh-count assertion line 85.
2. Binding drift after real `take()`: test line 194; resolve line 196; refresh-count assertion line 225.
3. Escaping non-runtime throwable normalization: test line 236; resolve line 238; refresh-count assertion line 256.
4. Independent exact-field drift rejection: test line 267; resolve line 284; refresh-count assertion line 311 for every field case.
5. Value-equal A-B-A rebind rejection: test line 323; resolve line 325; refresh-count assertion line 372.

The production resolver performs exactly one refresh at `TurnExecutionWindow.java:68`. The frozen worker enters `callInputTransaction(..., false)` at `InputActionWorker.java:135`, then uses `focusFrozenBindingInActiveTransaction(...)` at line 459. That coordinator method focuses the supplied binding verbatim and performs no refresh (`WindowAwareInputCoordinator.java:148-179`). The mutable refresh path at coordinator line 194 is therefore unreachable from these frozen callback paths. No second resolver/worker refresh exists.

### Queue/take ordering and deterministic barriers

- `CountingQueue.take()` at test line 1179 first obtains the request from production `super.take()` and only then runs `onTake`; drift mutations therefore occur after the real dequeue and before the worker preamble.
- `blockWorker()` at line 1138 uses entered/release latches around a real queued callback. It establishes queue ownership without `waitUntilQueued` or sleep polling.
- The pause barrier derives its signal from the production `wakeCondition` while `TaskPauseToken.waitIfPausedRevision(...)` is inside `while (pauseRequested)` (`TaskPauseToken.java:107-129`), so the latch proves actual pause-loop entry rather than mere pause request.
- The 50 ms `join` at test line 170 is not polling and is not used to infer queue/take placement; the `allowSettleCompletion` latch remains closed and supplies the causal completion barrier.

### Typed STOP, drift, and A-B-A behavior

- `InputActionRequest.frozenExactWindowFailure(...)` preserves the detector's typed cancellation at line 474. The detector checks `STOP_REQUESTED` before identity and binding drift at lines 938-950, so concurrent STOP plus drift remains STOP rather than being reclassified.
- Exact-window equality includes window id, HWND, process id, rectangle, and player identity epoch (`InputActionRequest.java:420-455`, `964+`).
- The generation witness uses binding object identity (`currentBinding != nativeBinding` at line 455). Consequently an A-B-A value restoration still fails as a new generation.
- `WindowNativeBindingRefreshService.refreshAndCommit(...)` holds the runtime-context monitor for read/refresh/commit (`WindowNativeBindingRefreshService.java:72-85`), and `WindowRuntimeContext.setNativeBinding(...)` is synchronized at line 166. This matches the monitor used by the frozen request/worker gates.

### Collaborator construction

The harness constructs real collaborators through accessible constructors and public extension points. `TestTaskManager` subclasses the real manager and overrides public `getRunner`; `BareWindowTaskRunner` invokes the real constructor with concrete required collaborators. The inspected constructors only assign/normalize dependencies or create dormant executor objects; no lifecycle method, application context, server, task, capture, input, or background work is started by harness construction. The only reflection API imported is public `java.lang.reflect.Proxy` for an interface test double; there is no private reflection.

### Forbidden-technique audit

The frozen test has zero occurrences of:

- `sun.misc.Unsafe`, `theUnsafe`, `allocateInstance`
- `setAccessible`, `getDeclaredField`, `getDeclaredMethod`, `getDeclaredConstructor`
- `waitUntilQueued`, `Thread.sleep`
- `System.currentTimeMillis`, `System.nanoTime`
- `Files.read`, `readString(`, `src/main`

Thus there is no Unsafe allocation, private reflection, source scan, queue-state helper, or sleep-based polling.

## Baseline and Scope

Commit `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` exists and was inspected as the confirmed pre-cloud baseline identified by `docs/业务逻辑.md`. Its `TeamReturnService` submits the click and settle delay as one queue sequence: `clickLeft(..., 150)` followed by `sleep(500)`. The reviewed artifact preserves that HTTPS turn-input mechanic and introduces no task-phase, prompt/OCR, navigation, retry/fallback, park/yield, expiry, or business-decision change. Result: `无已批准业务差异；按基线等价迁移`.

Both DHXY (`thin-client-design`) and `dhxy-cloud-brain` (`navigation-migration`) statuses were read before review. Both contain pre-existing dirty/untracked work. No file other than this assigned report was written, and no Git mutation was performed.

<!-- TRUE_EOF: TURN-28Q REPAIR-6 INDEPENDENT-WHOLE-CARD-REVIEW-R1 APPROVED P0=0 P1=0 P2=0 QUEUE-SHA=0d1bc01f WORKER-SHA=5d41a074 REQUEST-SHA=7f4f8fdc TEST-SHA=7d17722f TEST-LINES=1451 TESTS=21 ROLE=R1 2026-07-16T14:28:00-04:00 -->
