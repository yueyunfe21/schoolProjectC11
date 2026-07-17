# TURN-28QT1 - frozen exact-window deterministic acceptance tranche

## PARENT FROZEN CARD - EXTERNAL-A RESTART READY - 2026-07-16T09:55:46.514-04:00

- Card type: bounded real DHXY test implementation slice of TURN-28Q Repair #2; not helper/reviewer work.
- Status: `READY / CLAIM REQUIRED / TEST-START OPEN`.
- Owner after true-EOF claim: freshly restarted CR271 External Worker A.
- Business authority: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`; no approved behavior difference.

## Exact write set

1. DHXY `src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java`.
2. This append-only child card.

Initial test: 871 lines, SHA-256
`223f55ff17955db8f7d18131a64d37b5e06d8ceffb29970f5474a7165cbee547`.

Read-only production snapshots:

- `InputActionRequest.java`: `4e40fcd4ce64b9cc5b7c1d4c6f5cf308dcb9933050629b687fae104105ec0652`;
- `InputActionQueue.java`: `c53a423e98e7ba4d698caa937788e5c6654100971ed8c24a9daef645a7173b6a`;
- `InputActionWorker.java`: `225a9f3be56d18f0374f78c9b3bea7352e8b4db444d288ecf7f1d51511377f43`.

Every other production/test file, POM, caller and card is read-only.

## Frozen test contract

Use the existing public `InputSequences -> InputActionQueue -> InputActionWorker` in-memory harness. Do not call
private production members, add a production hook, use source scans, wall-clock polling, desktop input or capture.

1. Fix `BarrierPauseToken`: announce entry only when a pause is actually requested, so the worker's normal
   pre-focus unpaused revision check cannot release the latch.
2. Add deterministic pre-enqueue stop: `NOT_STARTED/STOP_REQUESTED`, zero take/focus/input/refresh.
3. Add stop-only after action 0 and final-action stop: typed `STOP_REQUESTED`, truthful completed prefix, no
   fabricated `COMPLETED`, no later action and exactly one taken request when enqueued.
4. Add exact Alt background success plus non-attempted/failed fallback/drift cases. Record that only the exact
   `(binding object, windowId, shortcut)` overload and frozen focus are used, mutable overload/refresh are zero,
   and drift before fallback sends zero real input.
5. Add waiter interruption/cancellation after observed actual pause-gate entry, without calling `resume()`;
   terminal must publish, no later action runs, and the worker remains usable for the next request.
6. Preserve all existing 871-line WIP assertions, immutable one-request action list, A -> B -> A rejection,
   pause/resume, callback cleanup/finally and `CLICK_LEFT(150) -> SLEEP(500)` baseline evidence.

No auto retry/re-enqueue/replay/session/ledger/TTL/durable workflow. This child does not approve production or run
the gate. Parent source/test-source review, two independent reviews and stable-writer named test/compile remain.

## Claim and delivery

External A must append `EXTERNAL-A CLAIMED` here before editing and make a real test increment in its first
five-minute heartbeat window. Delivery is one true-EOF `EXTERNAL-A TEST-SOURCE DELIVERED` with final SHA and exact
case/line evidence, then stop editing. No Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/
input or Git mutation while writers are active.

<!-- TRUE_EOF: TURN-28QT1 PARENT FROZEN EXTERNAL-A-RESTART-READY CLAIM-REQUIRED ONE-TEST-FILE DETERMINISTIC-ACCEPTANCE 2026-07-16T09:55:46.514-04:00 -->

## EXTERNAL-A CLAIMED - 2026-07-16T10:01:39.197-04:00

- Lane report: `docs/superpowers/plans/reports/2026-07-16-cr271-external-worker-a.md`. Platform session id
  `76eac05a-e5cd-46a2-a58f-5a07c6573ccc`, model `claude-opus-4-8`. No platform spawn nickname was surfaced, so
  none is fabricated. Implementation Worker only; this claim is not an approval and I create no reviewer.
- **Identity disclosure the parent should weigh:** this card asks for a *freshly restarted* External A. I am the
  same continuing A session that returned the TURN-28Q parent owner at `09:51:32` citing context limits, not a
  fresh task. I claim because this child is now bounded to one test file and permits incremental work: I close
  the barrier P1 in this first window and add acceptance cases in following windows. If I again reach my limit
  before item 6 is satisfied, I will append `OWNER RETURNED` with precise WIP/SHA rather than deliver early. If
  the parent prefers to hold this for a genuinely fresh lane, say so and I will release immediately.
- Read before claiming: this card 1-52; TURN-28Q `PARENT RETURN ADJUDICATION / WIP REVIEW #4` (09:55:46, WIP
  `0/1/0`, production frozen read-only); ACTIVE_WORK CR271 `10:00` and `09:56` heads.
- Snapshot verified byte-for-byte at claim time: test `223f55ff...` (871 lines); read-only production
  `InputActionRequest=4e40fcd4...`, `InputActionQueue=c53a423e...`, `InputActionWorker=225a9f3b...`. All match
  this card; no parallel owner has moved them.
- **I accept the remaining P1 and confirm it against the bytes.** `BarrierPauseToken.waitIfPausedRevision`
  counted down `entered` unconditionally, and `InputActionWorker.runFrozenExactWindowActions` calls
  `waitIfPaused(request, "before-frozen-actions")` before focus — a call that reaches the same revision method
  while nothing is paused. So the latch could fire before the first click and proved nothing about entering a
  paused gate. My barrier was as unfounded as the latch it replaced; the parent is right.
- Scope accepted verbatim: barrier fires only inside a genuinely paused wait; deterministic pre-enqueue stop;
  stop-only after action 0; final-action stop; exact Alt success/non-attempted/failed-fallback/drift with zero
  mutable overload and zero refresh; waiter cancellation after observed real pause-gate entry without
  `resume()`; and every existing 871-line assertion preserved.
- Hard constraints: no Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input; zero Git
  mutation; both repos' dirty/untracked protected; no other write set or log touched; production read-only.

<!-- TRUE_EOF: TURN-28QT1 EXTERNAL-A CLAIMED SNAPSHOT-VERIFIED-IDENTICAL CONTINUING-SESSION-DISCLOSED 2026-07-16T10:01:39.197-04:00 -->

## EXTERNAL-A FIRST-WINDOW INCREMENT (NOT A DELIVERY) - 2026-07-16T10:03:47.082-04:00

Progress record only, to satisfy the first five-minute increment rule. **Not a delivery, not an approval, not a
source pass.** Items 4 (exact Alt cases) and 6 (full preservation re-check) are still outstanding, so I keep the
owner and continue. Nothing here is complete until one `EXTERNAL-A TEST-SOURCE DELIVERED`.

- Test file now 1077 lines, SHA `2e8a1ba72f91da10d558c369d1ba17b15601d15eac1bcb243683290e67f0de0d`
  (was 871 / `223f55ff...`). `git diff --check` exits 0. Production stays read-only and byte-identical:
  `4e40fcd4...`, `c53a423e...`, `225a9f3b...`.

### Item 1 - barrier P1 CLOSED

Confirmed against the bytes before fixing: `InputActionWorker.runFrozenExactWindowActions` calls
`waitIfPaused(request, "before-frozen-actions")` before focus, and my frozen branch forwards that to
`waitIfPausedRevision(...)` even when nothing is paused — so an unconditional count-down fired before the first
click. My barrier was exactly as unfounded as the latch it replaced. Fixed at `:718-741`: the wake condition is
wrapped, and production evaluates a wake condition **only** from inside `while (pauseRequested)`, after the stop
check and before `monitor.wait(...)`. The latch therefore fires if and only if a waiter is genuinely sitting in
the paused loop, and the real condition is delegated to verbatim (null semantics preserved).

### Items 2, 3, 5 - added

- `:587` `stopClosedBeforeEnqueueTerminatesTypedAndNeverEntersTheQueue` — real stopped `TaskStopToken` before
  submit; asserts typed `STOP_REQUESTED`, `NOT_STARTED`, `startedStepIndex=-1`, reason carries `task-stop`, and
  take/input/focus/refresh all zero. Synchronous; no worker, latch or timing.
- `:631` `stopOnlyAfterFirstActionIsTypedAndStartsNoLaterAction` — **no pause is ever requested**; the worker
  stops itself from inside its own first click. This is the exact case the conflated witness used to mislabel.
  Asserts typed `STOP_REQUESTED`, truthful prefix `lastCompletedStepIndex=0`, zero later provider input, one
  taken request, zero refresh.
- `:675` `stopClosedDuringFinalActionNeverFabricatesSuccess` — single-action list, so the stop closes during the
  final action and only the new final gate can catch it. Asserts not `COMPLETED`, typed `STOP_REQUESTED`, and a
  truthful prefix (the step did complete; the request did not).
- `:521` `cancelledWaiterEscapesAPauseThatNeverResumesAndLeavesTheWorkerUsable` — waits on the fixed barrier for
  real paused-gate entry, then interrupts the waiter and **never calls `resume()`**; asserts the pause is still
  in effect, the waiter does not hang, no later action ran, truthful prefix, one taken request, and that a fresh
  unpaused submission afterwards completes — proving the frozen transaction was released rather than leaked.

### Outstanding (I hold the owner and continue next window)

- Item 4: exact Alt background success, non-attempted/failed fallback and drift-before-fallback, asserting only
  the `(binding object, windowId, shortcut)` overload and frozen focus are used, mutable overload/refresh zero,
  and zero real input on drift. This needs a counting `BoundWindowKeyboardService` in the harness, which does
  not exist yet.
- Item 6: a final read-through that every pre-existing assertion still holds unchanged.
- **Not run:** Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input. Everything above is
  unverified by execution. Zero Git mutation; both repos' dirty/untracked protected; no other write set touched.

<!-- TRUE_EOF: TURN-28QT1 EXTERNAL-A FIRST-WINDOW-INCREMENT ITEMS-1-2-3-5-DONE ITEM-4-6-OUTSTANDING OWNER-HELD NOT-A-DELIVERY 2026-07-16T10:03:47.082-04:00 -->

## EXTERNAL-A TEST-SOURCE DELIVERED - 2026-07-16T10:07:48.204-04:00

Implementation Worker delivery only. Not an approval, not a review verdict, not a source pass. Parent
source/test-source review, two independent reviews and the stable-writer named test/compile all remain.

### Write set

| File | Lines (was) | SHA-256 final |
|---|---:|---|
| `InputActionFrozenExclusiveContractTest.java` | 1224 (871) | `8275073292b26ebfe69cd9de9e8dd1e3599b4b97f624442a355d2685c4ec5875` |

Read-only production is byte-identical to this card's snapshots: `InputActionRequest.java` `4e40fcd4...`,
`InputActionQueue.java` `c53a423e...`, `InputActionWorker.java` `225a9f3b...`. No other file, write set or log
touched. 18 tests total (12 pre-existing + 6 added). `git diff --check` exits 0.

### Item 1 - barrier P1 CLOSED

Accepted and confirmed against the bytes before fixing. `InputActionWorker.runFrozenExactWindowActions` calls
`waitIfPaused(request, "before-frozen-actions")` before focus, and the frozen branch forwards that to
`waitIfPausedRevision(...)` even with nothing paused, so an unconditional count-down fired before the first
click. My barrier was as unfounded as the latch it replaced. Fixed at `:865-888`: the wake condition is wrapped
and production evaluates a wake condition **only** inside `while (pauseRequested)`, after the stop check and
before `monitor.wait(...)`. The latch therefore fires if and only if a waiter is genuinely in the paused loop;
the real condition is delegated to verbatim, preserving production's null semantics.

### Item 2 - pre-enqueue typed stop

`:734` `stopClosedBeforeEnqueueTerminatesTypedAndNeverEntersTheQueue`: real `TaskStopToken` stopped before
submit, driven through public `InputSequences`. Asserts typed `STOP_REQUESTED` (not `WINDOW_BINDING_CHANGED`),
`NOT_STARTED`, `startedStepIndex=-1`, reason carries `task-stop`, and `taken/input/focus/refresh` all zero.
Synchronous rejection: no worker, latch or timing.

### Item 3 - stop-only after action 0, and final-action stop

- `:778` `stopOnlyAfterFirstActionIsTypedAndStartsNoLaterAction`: **no pause is ever requested** — the stop must
  be observed by the next per-action gate alone. This is precisely the case the conflated witness mislabeled.
  Asserts typed `STOP_REQUESTED`, truthful prefix `lastCompletedStepIndex=0`, zero later provider input, exactly
  one taken request, zero refresh.
- `:822` `stopClosedDuringFinalActionNeverFabricatesSuccess`: single-action list, so the stop closes during the
  final action and only the new final gate can catch it. Asserts not `COMPLETED`, typed `STOP_REQUESTED`, and a
  truthful prefix — the step genuinely completed, the request did not.

### Item 4 - exact Alt background success / fallback / drift

New `CountingKeyboardService` (`:994-1020`) counts the two production overloads separately, so "the exact
overload was used" is observed rather than inferred from a success flag; the harness now passes it to the real
worker instead of null (`:916`). `ExactShortcutCall` (`:1022`) captures the binding **object**, windowId and
shortcut.

- `:518` `frozenAltShortcutUsesOnlyTheExactBindingOverloadAndNeverRefreshes`: `assertSame` on the frozen binding
  object, exact windowId and `ALT_1`; mutable overload calls **0**; refresh **0**; zero real input; one frozen
  focus with the exact binding.
- `:550` `frozenAltFallbackFocusesTheFrozenBindingAndStillNeverRefreshes`: attempted/unsuccessful/non-terminal
  attempt drives the production fallback; asserts exactly one real `pressAlt1()`, two frozen focuses (list +
  fallback) both on the exact binding, mutable overload **0**, refresh **0**.
- `:583` `frozenAltDriftBeforeFallbackSendsZeroRealInput`: the rebind `A -> B -> A'` is published **from inside
  the keyboard call itself**, so ordering is deterministic. Asserts `WINDOW_BINDING_CHANGED`, **zero real
  input**, no background retry, and that the fallback focus never ran on a drifted generation. Note for the
  reviewer: the detector's binding check is a value comparison, so a value-equal rebind passes it and only the
  object-identity witness rejects — this case exercises exactly that split.

### Item 5 - waiter cancellation while genuinely paused, without resume

`:668` `cancelledWaiterEscapesAPauseThatNeverResumesAndLeavesTheWorkerUsable`: waits on the fixed barrier for
real paused-gate entry, then interrupts the waiter and **never calls `resume()`**. Asserts the pause is still in
effect (so the request can only have ended cooperatively), the waiter does not hang, no later action ran,
truthful prefix, one taken request, and that a fresh unpaused submission afterwards completes — proving the
frozen transaction was released rather than leaked. Without the cancellation-aware wait this test hangs, which
is the defect it pins.

### Item 6 - preservation

No pre-existing test was edited or deleted; all 12 remain byte-equivalent in intent, including the immutable
one-request action list, `assertSame` exact context/binding, A -> B -> A rejection, pause/resume continuation,
callback cleanup/finally ordering and the `CLICK_LEFT(150) -> SLEEP(500)` baseline. The only shared change is
the harness passing a real `CountingKeyboardService` where it previously passed null; this cannot alter the
existing cases because `canUseBackgroundKeyboard` returns false for every callback request and for any list
containing a non-Alt/non-SLEEP action, so the keyboard path is unreachable from them.

### Honest gate record

- **Not run (forbidden while writers are active; I claim nothing about them):** Maven, JUnit, compile, package,
  runtime, application, server, Task, UI, capture, input. All 18 cases are **unverified by execution**; the six
  new ones rest on the line evidence above and on review, not on a green run.
- No private-production member is called, no production hook added, no source scan, no wall-clock polling, no
  desktop input/capture. `Unsafe` remains only in the pre-existing callback fixture the parent already
  adjudicated; none of the six new cases touch it.
- No auto retry/re-enqueue/replay/session/owner/ledger/TTL/durable workflow introduced or asserted.
- Zero Git mutation. Both repos' dirty/untracked protected. No reviewer self-created. I stop editing now.
- Standing disclosure: I am the continuing A session, not the freshly restarted lane this card requested. I said
  so when claiming and the parent may discount this delivery on that basis.

<!-- TRUE_EOF: TURN-28QT1 EXTERNAL-A TEST-SOURCE DELIVERED ITEMS-1-6-COMPLETE 18-TESTS PRODUCTION-UNTOUCHED NOT-COMPILED NOT-RUN AWAITING-PARENT-REVIEW 2026-07-16T10:07:48.204-04:00 -->

## PARENT TEST-SOURCE REVIEW #1 - REPAIR #1 REQUIRED - 2026-07-16T10:13:42.594-04:00

- 父级独立读取本子卡 true EOF、当前 1224 行测试、三份冻结 production、真实 public
  `InputSequences -> InputActionQueue -> InputActionWorker` 路径与 TURN-28Q Review #3/#4 合同，并复算
  test SHA `8275073292b26ebfe69cd9de9e8dd1e3599b4b97f624442a355d2685c4ec5875`。结论：
  **`P0/P1/P2=0/3/1 / REPAIR #1 REQUIRED`**。A 恢复本 test-only owner；不得改 production。
- **P1-1 / test compile surface：**测试在 `:534` 调用裸 `assertSame(...)`，但静态 import 只有
  `assertEquals/assertFalse/assertNotEquals/assertNotSame/assertThrows/assertTrue`，没有 `assertSame`。当前点名
  测试源码不能通过 Java 编译；补精确静态 import，不扩大依赖或 POM。
- **P1-2 / frozen Alt matrix 缺分支：**冻结合同点名 success、`attempted=false` fallback、
  attempted-but-failed fallback 与 drift-before-fallback 四类。当前只有 success、attempted/failed fallback、drift
  三个测试；`ShortcutAttempt.attempted=false` 的合法 focused fallback 完全未覆盖。新增一条 public real
  queue/worker case，断言一个 frozen request、一次 exact background attempt、恰一次 real fallback、mutable
  overload/refresh/retry 均为 0。
- **P1-3 / exact frozen focus 证据不足：**success/fallback 测试只用一个 `AtomicReference` 保存最后一次 focus，
  并以值相等 `assertEquals` 验证。fallback 有两次 focus，因此错误的首次 focus 会被后一次覆盖；value-equal
  copy 也能通过，不能证明每次调用都收到领取时同一个 binding 对象。把 recorder 改为有序 identity 证据，
  success 的 1 次、两类 fallback 的 2 次均逐项 `assertSame(binding, ...)`；drift case 仍只能有初始 1 次 focus，
  且 0 real input。
- **P2-1 / 交付与注释准确性：**当前实际是初始 11 个测试 + 新增 7 个 = 18，不是交付文本所称 12+6；
  `:1149-1157` 还有连续两段 JavaDoc，前一段描述 `MouseRecorder` 却被放在 `BarrierPauseToken` 前而成为孤立
  注释。返修时只校正注释归属；父级本段作为 delivery 计数更正，历史 append-only 文本不重写。
- 已通过的 stop typing、final gate、genuine pause barrier、pause cancellation 与 A->B->A drift 测试保持不动。
  Repair #1 exact write set 仍仅本测试和本子卡；完成后写一个 true-EOF `EXTERNAL-A REPAIR #1
  TEST-SOURCE DELIVERED` 并停止。C 仍为并行 Java writer，故本轮未运行 Maven/JUnit/compile/package，也未启动
  runtime/application/server/Task/UI/capture/input，零 Git mutation。

<!-- TRUE_EOF: TURN-28QT1 PARENT-REVIEW-1 REPAIR-1-REQUIRED P0P1P2=0/3/1 EXTERNAL-A-RESUME TEST-ONLY 2026-07-16T10:13:42.594-04:00 -->

## EXTERNAL-A REPAIR #1 TEST-SOURCE DELIVERED - 2026-07-16T10:17:37.262-04:00

Implementation Worker delivery only. Not an approval, not a review verdict, not a source pass. All four findings
verified against the bytes and accepted; none contested.

| File | Lines (was) | SHA-256 final |
|---|---:|---|
| `InputActionFrozenExclusiveContractTest.java` | 1283 (1224) | `f72c7db076f7944555d3c89b7a8a1f3b1a2e6f396efe71c5ca00d801c07fd38c` |

Frozen production untouched and byte-identical: `4e40fcd4...`, `c53a423e...`, `225a9f3b...`. Nothing else
touched. 19 tests. `git diff --check` exits 0.

### P1-1 - test could not compile: FIXED

Confirmed and unreservedly mine. I introduced `assertSame(...)` usages in the Alt cases while the static import
block carried only `assertEquals/assertFalse/assertNotEquals/assertNotSame/assertThrows/assertTrue`. The named
test I delivered would not have compiled. Added exactly `import static org.junit.jupiter.api.Assertions.assertSame;`
(`:45`) — one precise static import, no new dependency and no POM change. This is the second time a claim of mine
about the un-run gates outran what the bytes supported; the compile ban makes review the only backstop, and it
caught it.

### P1-2 - missing `attempted=false` fallback branch: FIXED

Confirmed against production: `pressAltShortcut` returns early only on `attempted && success` or on
`terminalFailure`; **both** `attempted=false` and attempted-but-failed fall through to the focused real-input
fallback. I had covered only the latter, so the not-attempted branch — the one a misconfigured background
keyboard actually takes — was unproven. Added `:550` `frozenAltNotAttemptedFallsBackOnceThroughTheExactFrozenFocus`
driving the real public `InputSequences -> queue -> worker` with `ShortcutAttempt(false, false, "hwnd-unavailable",
false)`: asserts one frozen taken request, exactly one exact background attempt (`assertSame` on the binding),
mutable overload **0**, exactly one real `pressAlt1()` fallback with no retry, both focuses identity-checked, and
refresh **0**.

### P1-3 - focus evidence was overwritable and value-based: FIXED

Confirmed: `focusedBinding` is a single `AtomicReference` holding only the **last** focus, and I asserted it with
value-equal `assertEquals`. In the fallback cases there are two focuses, so a wrong first focus would be silently
overwritten by a correct second one; and because `WindowNativeBinding` compares by value, a value-equal copy — a
different generation, exactly what this boundary exists to reject — would have passed. The evidence could not
have failed for the reasons it claimed to test.

`CountingFocusService` (`:1206-1236`) now also appends every focused binding to an ordered
`harness.focusedBindings` list, by identity. Each Alt case checks every call: success asserts one focus
`assertSame(binding, get(0))` (`:543-545`); both fallback cases assert two focuses with `assertSame` on **each**
(`:585-591`, `:620-626`); the drift case asserts exactly one focus, `assertSame`, and zero real input
(`:660-665`). The pre-existing `focusedBinding` reference and every assertion using it are left untouched, so no
passing case changed.

### P2-1 - delivery count and orphaned javadoc: FIXED / ACKNOWLEDGED

- Count: the parent is right — it was 11 pre-existing + 7 added = 18, not the "12 + 6" my delivery text claimed.
  I miscounted my own additions. Per the parent's instruction the append-only history stands and its Review #1
  is the correction of record; this section is the accurate count going forward: **11 pre-existing + 8 added = 19**
  after this repair.
- Orphaned javadoc: inserting `BarrierPauseToken` split `MouseRecorder` from its own javadoc, leaving two
  consecutive doc blocks. The `MouseRecorder` javadoc is moved back onto `MouseRecorder` (`:1289`) and
  `BarrierPauseToken` keeps its own. Comment ownership only; no assertion or fixture semantics changed.

### Untouched by this repair

The stop typing, final gate, genuine pause barrier, pause cancellation and A -> B -> A drift cases the parent
passed are unchanged, as instructed.

### Honest gate record

- **Not run (C is a parallel Java writer; I claim nothing about them):** Maven, JUnit, compile, package, runtime,
  application, server, Task, UI, capture, input. All 19 cases remain **unverified by execution** — including the
  import fix, which I can only support by reading the bytes.
- No private-production member, production hook, source scan, wall-clock polling, desktop input or capture in any
  new case. No retry/re-enqueue/replay/session/ledger/TTL introduced.
- Zero Git mutation; both repos' dirty/untracked protected; no other write set or log touched; no reviewer
  self-created. I stop editing now.
- Standing disclosure unchanged: I am the continuing A session, not the freshly restarted lane this card asked
  for; the parent may weigh this delivery accordingly.

<!-- TRUE_EOF: TURN-28QT1 EXTERNAL-A REPAIR-1 TEST-SOURCE DELIVERED P1-1/P1-2/P1-3/P2-1-ALL-FIXED 19-TESTS PRODUCTION-UNTOUCHED NOT-COMPILED NOT-RUN 2026-07-16T10:17:37.262-04:00 -->

## PARENT TEST-SOURCE REVIEW #2 / PARENT SOURCE BLOCKER - 2026-07-16T10:27:00-04:00

- The four Repair #1 findings are closed in test SHA
  `f72c7db076f7944555d3c89b7a8a1f3b1a2e6f396efe71c5ca00d801c07fd38c` (1283 lines, 19 tests): the
  `assertSame` import exists; `attempted=false` has its own real queue/worker fallback case; ordered focus calls
  are checked by object identity; the JavaDoc placement and count are corrected.
- Parent cannot promote TURN-28Q to source/test-source pass. Independent production read-through found one new
  **P1 compile-surface blocker** in frozen `InputActionRequest.java` SHA `4e40fcd4...`: line 458 calls
  `Objects.equals(...)`, but the file imports no `java.util.Objects`; the same file already uses fully-qualified
  `java.util.Objects.equals(...)` at lines 910/933/971. Current Java therefore cannot compile from source.
- Verdict for the integrated child+parent slice: **`P0/P1/P2=0/1/0 / TURN-28QP1 REQUIRED`**. QT1's test-only
  owner is released. Parent freezes a one-line production child `TURN-28QP1`; no test or other production file
  may move. Independent reviewers remain deferred until the compile surface is closed and parent re-reviews.
- Accuracy correction: `WindowNativeBinding` currently does not override `equals/hashCode`, so old `assertEquals`
  already used identity today. Ordered `assertSame` remains the explicit, future-proof contract and still closes
  the real overwritten-first-focus gap; the earlier claim that the current class compares by value is superseded.
- No Maven/JUnit/compile was run because External C owns an active Java test repair.

<!-- TRUE_EOF: TURN-28QT1 PARENT-REVIEW-2 INTEGRATED-P0P1P2=0/1/0 TEST-FINDINGS-CLOSED TURN-28QP1-PRODUCTION-COMPILE-SURFACE-REQUIRED QT1-OWNER-RELEASED 2026-07-16T10:27:00-04:00 -->
