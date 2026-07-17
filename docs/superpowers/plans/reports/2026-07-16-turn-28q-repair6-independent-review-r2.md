# CR271 TURN-28Q Repair #6 Independent Whole-Card Review - R2

## Verdict

**APPROVED**

- P0: 0
- P1: 0
- P2: 0
- Role: independent whole-card reviewer R2
- Review type: frozen source/test artifact review only
- Business baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- Approved business differences: none; this is equivalent input-ownership/turn-safety plumbing.

## Frozen Artifact Identity

The reviewed files match the card's frozen whole-card identity:

| File | Lines / tests | SHA-256 |
|---|---:|---|
| `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java` | 859 lines | `0d1bc01fbce331b75a958f105e3c21d50048aafcc5ed0b973edf0dcc30241057` |
| `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java` | 830 lines | `5d41a074e403a154e05611a31b0f2db5dc8684b064c76603fbfa1293c452a8c6` |
| `src/main/java/com/bot/dhxy/input/action/InputActionRequest.java` | 1148 lines | `7f4f8fdc4baf90f613fa6556f2a860849933724a6de5591d07d1eeb2d91ab0c8` |
| `src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java` | 1451 lines / 21 `@Test` methods | `7d17722fb911331a2a51572f727bd280df9029b9a5ecad1ab9f4f94d219ab25e` |

The original card was read through its canonical true EOF:

`<!-- TRUE_EOF: TURN-28Q PARENT-INDEPENDENT-WHOLE-CARD-REVIEW-ASSIGNED R1-GIBBS-019f6c29-d104-7533-b41c-187c11218ff0 R2-PASCAL-019f6c29-e4ff-7b23-83fc-205de3801805 TEST-SHA=7d17722f DUAL-REVIEW-IN-PROGRESS 2026-07-16T14:21:00-04:00 -->`

## Whole-Card Findings

### 1. Five public resolver-to-worker callback paths

All five Repair #6 callback cases call the test harness's public `resolve(...)`, which invokes `TurnExecutionWindow.resolveForAction(action, taskManager, refresh)`, then submit through the real public `InputActionQueue.submitFrozenExactWindowExclusiveAndWait(...)` path to the real started `InputActionWorker` callback path:

1. Success: `resolvedSnapshotRunsOnTheRealWorkerWithoutASecondRefresh` (`InputActionFrozenExclusiveContractTest.java:63`).
2. Binding drift after take: `bindingDriftAfterTakeRunsZeroCallbackAndAddsNoRefresh` (`:194`).
3. Non-runtime throwable propagation: `nonRuntimeThrowableEscapingTheCallbackCompletesTheRequestExceptionally` (`:236`).
4. Six exact-window field drifts: `everyExactWindowFieldDriftRunsZeroCallbackAndAddsNoRefresh` (`:267`).
5. Value-equal A-B-A rebind: `valueEqualRebindRunsZeroCallbackAndAddsNoRefresh` (`:323`).

`Harness.resolve(...)` at `:1128-1135` is the sole shared resolver entry. There are exactly five calls to `harness.resolve(...)`, and each case asserts `refresh.calls == 1`. The harness starts the real worker at `:1101-1108`; it does not call the callback directly.

### 2. Exactly one resolver refresh and no second refresh

`TurnExecutionWindow.resolveForAction(...)` performs the one allowed `refreshAndCommit(...)` call. The frozen queue submissions (`InputActionQueue.java:337-430`) only freeze and validate the returned exact binding/context generation. Worker callback execution (`InputActionWorker.java:437-476`) focuses that frozen binding and executes under the active input transaction; neither the queue nor this worker path calls the resolver or mutable binding refresh again.

The five callback cases therefore cover one resolver refresh each, including success, post-take drift, exceptional callback, all six exact binding fields, and A-B-A replacement. No second-refresh path was found.

### 3. Legal collaborator construction

`BareWindowTaskRunner` and `TestTaskManager` use ordinary accessible Java constructors with arities matching current production constructors. Required runner collaborators are non-null. `RouteCloudDecisionService` receives a concrete `CloudDecisionProperties`; `AutomationMetricsService` is constructed normally and its Spring `@PostConstruct` lifecycle is not invoked by `new`; `WindowTaskRunner` construction creates executor objects but submits no task and starts no application/runtime flow. No collaborator is manufactured through allocation bypass or private-field mutation.

The test's `java.lang.reflect.Proxy` is used only as the public dynamic-proxy API for an `InputProvider` fake. It does not inspect or mutate private members and is not private reflection.

### 4. Deterministic queue/take boundaries

`CountingQueue.take()` (`InputActionFrozenExclusiveContractTest.java:1162-1187`) delegates to the real `InputActionQueue.take()` and records the request immediately after the actual take, with an optional hook before worker preamble validation. Drift/STOP scenarios are injected at this exact boundary. The after-take STOP-plus-drift case (`:817-879`) first takes a blocker and then the target, asserts the target request identity and exact take count, and proves zero target focus/input/callback.

This is a deterministic queue-owned boundary. The named test contains no `waitUntilQueued`, `Thread.sleep`, wall-clock polling, or timing race used to infer queue state.

### 5. Typed STOP, drift, and A-B-A semantics

`InputActionRequest.frozenExactWindowFailure(...)` (`InputActionRequest.java:927-955`) evaluates STOP before exact-window identity/field drift and returns typed safety results. `sameExactWindow(...)` (`:964-976`) compares HWND, process id, x, y, width, and height. The context-generation witness (`:448-463`) additionally requires the same binding object, same window id, non-suspended context, and unchanged epoch.

The tests establish:

- Pre-enqueue STOP plus A-B-A returns typed STOP, `NOT_STARTED`, with zero queue take/focus/input/refresh (`test:761-802`).
- Post-take STOP plus simultaneous identity drift preserves STOP priority and `NOT_STARTED`, with zero target work (`:817-879`).
- STOP before enqueue, after the first action, and after the final action preserve truthful `NOT_STARTED` / prefix-progress semantics and never fabricate success (`:888-1008`).
- Action-list A-B-A and callback A-B-A reject value-equal replacement identities with zero input/callback (`:323-374`, `:1017-1054`).
- All six exact-window field drifts reject before callback and add no refresh (`:267-314`).

The worker checks typed safety before the generation witness (`InputActionWorker.java:582-591`), both before frozen focus and at execution boundaries. Callback execution is monitor-protected (`:437-476`); action-list execution rechecks before each action and before success (`:507-569`). No retry or later-action execution follows a terminal STOP/drift result.

### 6. Forbidden-mechanism audit

The frozen named test has zero use of `sun.misc.Unsafe`, `theUnsafe`, `allocateInstance`, private reflection (`setAccessible`, `getDeclaredField`, `getDeclaredMethod`, or equivalent private-member access), production-source scanning, `waitUntilQueued`, or `Thread.sleep` polling. No real input provider is invoked. The queue/take proof uses the real package-visible queue boundary rather than source inspection or timing inference.

## Baseline and Scope Check

Baseline commit `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` exists and identifies `InputActionQueue` as the global `offer/take` serialization boundary. Its worker dispatches `CLICK_LEFT` through `inputProvider.clickLeft(..., action.getDelayMs())` and `SLEEP` through `TaskSleep.sleep(action.getDelayMs())`. The frozen implementation reuses that dispatcher, preserving the card's exact `CLICK_LEFT(150)` then `SLEEP(500)` queue semantics without adding a second command, retry, resolver refresh, or business decision.

The review also checked `AGENTS.md`, `docs/DHXY_CONTEXT.md`, top CR271 in `docs/ACTIVE_WORK.md`, authoritative plan Sections 14-19, the HTTPS turn protocol, `docs/业务逻辑.md`, the complete original TURN-28Q card, all production/test files named by the frozen card, and both repository statuses. DHXY (`thin-client-design`) and cloud-brain (`navigation-migration`) were already dirty/untracked; all such state was protected and left untouched.

## Gate Statement

Per reviewer restrictions, no Maven, JUnit, compile, package, runtime, application/server, Task, UI, capture, or input command was run. No Git mutation was performed. This report approves the frozen whole-card source/test artifact only; it does not claim the later named-test, compile, or fresh-runtime gates.

<!-- TRUE_EOF: CR271 TURN-28Q REPAIR-6 INDEPENDENT-WHOLE-CARD-REVIEW-R2 APPROVED P0-0-P1-0-P2-0 QUEUE-SHA=0d1bc01fbce331b75a958f105e3c21d50048aafcc5ed0b973edf0dcc30241057 WORKER-SHA=5d41a074e403a154e05611a31b0f2db5dc8684b064c76603fbfa1293c452a8c6 REQUEST-SHA=7f4f8fdc4baf90f613fa6556f2a860849933724a6de5591d07d1eeb2d91ab0c8 TEST-SHA=7d17722fb911331a2a51572f727bd280df9029b9a5ecad1ab9f4f94d219ab25e NO-PENDING-P0-P1-P2 NO-MAVEN-JUNIT-COMPILE-RUNTIME-APPLICATION-SERVER-TASK-UI-CAPTURE-INPUT-GIT-MUTATION 2026-07-16T14:29:07-04:00 -->
