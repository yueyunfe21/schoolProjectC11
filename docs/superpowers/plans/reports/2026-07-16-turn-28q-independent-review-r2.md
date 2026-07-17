# CR271 TURN-28Q Independent Delivery Review R2

- Review role: independent delivery reviewer only; not implementation owner and not parent/final reviewer.
- Review cut: `2026-07-16T09:28:53.7619971-04:00`.
- Decision: **BLOCKED**.
- Severity count: **P0/P1/P2 = 0/3/0**.
- This decision is derived from the current source and frozen contract. The parent verdict present at the TURN-28Q
  physical EOF was not adopted as review evidence, and no other reviewer report was used.

## Findings

### P1-1 - The public frozen action-list API can refresh/rebind during an Alt action

The API accepts a generic `List<InputAction>`, but an Alt-only/Alt-plus-sleep list does not remain on the exact
binding object frozen by the queue:

1. `InputActionWorker.java:117-131` classifies the request with `canUseBackgroundKeyboard(...)` and routes one
   frozen request into `runFrozenExactWindowActions(...)` inside one input transaction.
2. `InputActionWorker.java:653-666` returns true for every list containing only supported Alt actions and sleeps.
3. The shared dispatcher reaches `pressAltShortcut(...)`; `InputActionWorker.java:560-563` calls
   `boundWindowKeyboardService.pressShortcut(shortcut)`, the context-resolving overload, rather than the exact
   overload taking the request's frozen binding.
4. `BoundWindowKeyboardService.java:77-95` reads the current context and calls
   `bindingRefreshService.refreshAndCommit(context)` before sending the shortcut.
5. `WindowNativeBindingRefreshService.java:80-84`, `WindowNativeBinding.java:65-80`, and
   `WindowRuntimeContext.java:166-207` show that a successful refresh creates and commits a new binding object.
   Java monitors are reentrant, so the same worker can perform this commit while already inside the
   `synchronized (context)` section at `InputActionWorker.java:483-534`; the monitor prevents another thread from
   committing, but does not prevent this same-thread refresh.
6. If background delivery falls back, `InputActionWorker.java:578-591` calls
   `focusCurrentWindowInActiveTransaction(...)`; `WindowAwareInputCoordinator.java:182-205` refreshes and commits
   again. The post-focus generation check at `InputActionWorker.java:585-588` is conditional on
   `request.hasDeadline()`, while `InputActionRequest.java:286-289` creates this frozen request without a deadline.

Consequences: a one-element Alt list can report success after its frozen binding identity has been replaced; a
longer list can execute an Alt action and only reject at the next per-action generation check; the focused fallback
can send real input after re-resolution without an exact-generation recheck. This violates TURN-28Q contract
items 2, 3, and 5 (`no refresh`, exact binding identity plus epoch, terminal drift before later input).

Required repair: the frozen action-list route must use the exact-binding keyboard overload and an exact frozen
focus fallback, or otherwise reject that action before input. Add a real queue/worker case for Alt background
success and failed/not-attempted fallback that proves zero refresh, exact binding identity, typed drift closure,
and no input after drift.

### P1-2 - Cancellation while paused can leave the waiter and global input transaction blocked indefinitely

The frozen action-list factory supplies no deadline and therefore does not use the cancellation-aware pause wait:

1. `InputActionRequest.java:286-289` passes a null deadline; the constructor at `InputActionRequest.java:233-236`
   consequently leaves `excludePauseFromDeadline=false`.
2. Each action calls `waitIfPaused(...)` at `InputActionWorker.java:504-506`, but the applicable non-deadline branch
   at `InputActionWorker.java:378-386` delegates to `TaskPauseToken.waitIfPaused(stopToken)`.
3. `TaskPauseToken.java:71-96` waits while pause remains requested and observes only resume, the stop token, or a
   worker-thread interrupt. It does not observe `request.isCancelled()`.
4. When the submitting waiter is interrupted after the worker has taken the request, `InputActionQueue.java:793-805`
   calls `request.requestDetailedCancellation("waiter interrupted")` and then blocks on
   `request.getResult().join()`.
5. `InputActionRequest.java:734-753` records cooperative cancellation without completing the future or notifying
   the pause token. The worker remains inside the pause wait until an unrelated resume/stop occurs, while retaining
   the single global input transaction and context monitor.

Thus cancellation is not guaranteed to become a typed terminal result, the completion barrier can hang, and the
single physical-input worker can be held indefinitely. This violates TURN-28Q contract item 5.

Required repair: make the frozen action-list pause wait cancellation-aware without re-enqueue/replay or releasing
the transaction boundary. Add a real queue/worker case that enters the pause gate, interrupts/cancels the waiter,
does not call resume, receives a non-completed typed terminal result, executes no later action, and observes one
taken request.

### P1-3 - The named pause test has a scheduling false-positive window

`pauseMidFrozenActionListStartsNoLaterActionAndResumeContinuesTheSameRequest` does not deterministically prove that
the worker reached the per-action pause barrier:

1. At `InputActionFrozenExclusiveContractTest.java:471-475`, the first click hook requests pause and immediately
   releases the `paused` latch while the worker is still inside the click provider callback.
2. The test waits only for that latch at line 489, checks the call list at lines 491-493, and calls
   `pauseToken.resume()` at line 495.
3. There is no latch/event from entry into `InputActionWorker.waitIfPaused(...)`. The main test thread can resume
   the token before the worker returns from the click and evaluates the next action's pause gate.

An implementation with the per-action pause gate removed can therefore still pass when the test thread asserts
and resumes before the worker proceeds. This is not a new `Thread.sleep`, but it is still a timing-dependent race
proof forbidden by TURN-28Q contract item 6.

Required repair: instrument a test pause token/barrier that signals actual entry into `waitIfPaused`, wait for that
signal before asserting zero later action, and only then resume. Keep the real queue/worker and avoid wall-clock
sleep polling.

## Independent Contract Audit

| Frozen requirement | Static review result |
|---|---|
| Public `InputSequences` API with typed result | Satisfied at `InputSequences.java:98-116`; direct one-layer forwarding to the queue. |
| Queue freezes exact binding object plus epoch | Queue-side capture is correct at `InputActionQueue.java:393-421`; overall requirement is blocked by P1-1's in-worker refresh. |
| One request / one queue transaction / one context monitor | Mouse action-list structure is one request and one transaction at `InputActionRequest.java:278-289` and `InputActionWorker.java:120-131,483-534`; P1-1 breaks the claimed unchanging generation for Alt lists. |
| Per-action pause/stop/cancel/drift typed closure | STOP and next-boundary drift gates are present at `InputActionWorker.java:504-530`; cancellation is blocked by P1-2, and pause proof is blocked by P1-3. |
| Callback completion/finally barrier non-regression | No static regression found: routing remains additive at `InputActionWorker.java:128-131`, and callback execution remains isolated at lines 403-440 with its post-callback safety barrier. |
| Baseline timing | Statically preserved: the named success case carries exactly `CLICK_LEFT(delay=150)` then `SLEEP(500)` at `InputActionFrozenExclusiveContractTest.java:358-398`. Commit `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` supplies the same two actions in `TeamReturnService.java:86-89`. |
| No auto retry/replay/session/ledger/TTL | No such mechanism was added to the frozen action-list factory/worker route; it submits and consumes one request. Existing shared retained/deadline machinery is not activated by this factory. |

## Named Test Audit

- Real queue/worker: source constructs `CountingQueue extends InputActionQueue`, public `InputSequences`, real
  `WindowAwareInputCoordinator`, and real daemon `InputActionWorker.start()` at
  `InputActionFrozenExclusiveContractTest.java:580-615`.
- One taken request: `CountingQueue.take()` delegates to production `super.take()` at lines 665-680; the success
  case asserts one taken immutable request carrying both 150/500 actions at lines 358-398.
- Typed STOP: lines 408-449 use real task pause/stop tokens and assert
  `PARTIALLY_COMPLETED/STOP_REQUESTED`, one completed prefix action, no later provider action, and one taken request.
- Typed A-B-A: lines 518-555 use object-identity A-B-A before enqueue and assert
  `NOT_STARTED/WINDOW_BINDING_CHANGED`, zero take/focus/input/refresh.
- Unsafe/sleep review: the pre-existing callback fixture still contains `sun.misc.Unsafe` at lines 27, 48, and
  767-783 and an old `Thread.sleep(1)` poll at line 560. The TURN-28Q action-list cases at lines 358-555 no longer
  call `Harness.resolve`, `allocate`, or `waitUntilQueued`, and add no `Thread.sleep`. Nevertheless, P1-3 leaves a
  new scheduling race in the pause proof, so the no-race acceptance gate is not met.
- Coverage gap: there is no frozen action-list Alt/background/fallback case and no paused-waiter cancellation case;
  those omissions leave P1-1 and P1-2 undetected.

## Reviewed Snapshot And Constraints

| File | Lines | SHA-256 |
|---|---:|---|
| `src/main/java/com/bot/dhxy/input/InputSequences.java` | 210 | `b293e0c6792303d45a4314050c6e4f1c8b39d0f4dea426632586ed0f292dacb3` |
| `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java` | 850 | `66fa536ef8b4c6cbf8874cd94d8842fd8b0f9d3f4e74bc52719f31f39e4660bf` |
| `src/main/java/com/bot/dhxy/input/action/InputActionRequest.java` | 1118 | `23973b7eee06949138e8a2841e249c009eb69184804c2be0689aa317c29988de` |
| `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java` | 748 | `7489084b773e6066213d383af86c82ac9c3431fb9e2d1d5acf3e9c11d423eac0` |
| `src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java` | 852 | `475399ef8656c7d193bfeb6f18ba69b7e01d4c531710367e74d00165ded03c44` |

The frozen TURN-28Q contract (lines 48-69), its current physical true EOF, TURN-28P and TURN-22 dependency cards,
the applicable `docs/业务逻辑.md` baseline, and the `696a12b0` implementation were read. No approved business
difference was found or proposed.

Per the review assignment, no Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input command
was run, and no Git mutation was performed. This is a source/test-source delivery review, not an executed-test or
build result. No Java, original card, plan, `ACTIVE_WORK`, CR271 matrix/dashboard, dirty file, or untracked artifact
other than this required report was modified.

## Final Verdict

**BLOCKED.** TURN-28Q cannot be approved until P1-1 exact-binding refresh escape, P1-2 paused cancellation hang,
and P1-3 nondeterministic pause proof are repaired and independently re-reviewed on the resulting bytes.

<!-- TRUE_EOF: CR271 TURN-28Q INDEPENDENT DELIVERY REVIEW R2 BLOCKED P0P1P2=0/3/0 EXACT-BINDING-REFRESH PAUSED-CANCEL-HANG PAUSE-PROOF-RACE 2026-07-16T09:28:53.7619971-04:00 -->

## LATEST INDEPENDENT REVIEW ROUND R2 - 2026-07-16T10:52:47.2169621-04:00

本节是对最新 integrated delivery 的独立重审，不继承上方 `09:28` 旧轮结论。角色边界为独立
reviewer R2；不是实现者、parent 或批准人。Parent Review #5 只用于定位最终快照，以下结论均由本轮
重新读取原卡 physical true EOF、当前源码/测试和 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
得出。

## Findings

### P1-1 - Queue admission 仍在 typed safety 之前执行 generation witness

TURN-28Q 原卡 50-66 要求保留 typed STOP，Repair #2 又明确要求“typed safety first, witness
second”。`InputActionRequest.detectFrozenExactWindowFailure` 也把 stop 放在 identity/binding drift 前面：

1. `InputActionRequest.java:938-942` 先返回 `STOP_REQUESTED`；
2. `InputActionRequest.java:943-954` 才检查 identity epoch 和 exact binding drift；
3. `InputActionRequest.java:438-444` 明文要求 caller 先咨询 typed safety，再使用 pure generation
   witness。

但两个 public queue 入口仍各自先做一次 raw object-identity witness：

- callback 入口 `InputActionQueue.java:347-355` 先判断
  `exactContext.getNativeBinding() != exactBinding`，命中后直接发布
  `WINDOW_BINDING_CHANGED/frozen-generation-changed-before-enqueue`；typed detector 直到
  `:363-367` 才会调用；
- action-list 入口 `InputActionQueue.java:413-421` 同样先发布 binding change；typed detector 直到
  `:429-433` 才会调用。

这是可确定复现的错误 terminal typing：冻结 A，context 发生 `A -> B -> A'`（A' 字段相同、对象不同），
同时 task stop 已关闭，再用 A 调 public frozen API。当前代码在上述第一个 `if` 返回
`WINDOW_BINDING_CHANGED`，从未让 detector 返回其优先的 `STOP_REQUESTED`。请求虽未入队且不会发送输入，
但公开 typed contract 已被破坏；这也直接反证了源码注释所声称的“两个入口均 typed-first”。

Required repair：在 context monitor 内先构造唯一 frozen request 并运行 typed failure，再运行唯一 pure
generation witness；不能在它们之前保留第二个 object-identity shortcut。callback 与 action-list 两个入口
都必须一致。新增确定性 public-path case：pre-enqueue `STOP + A -> B -> A'`，断言
`NOT_STARTED/STOP_REQUESTED`、zero take/focus/input/refresh。

### P1-2 - Worker preamble 和 monitor acquisition 仍可绕过 typed-first 顺序

成功入队后，frozen request 在 `InputActionWorker.handle` 仍先经过 generic epoch comparator：

1. `InputActionWorker.java:98-115` 的顺序是 pause wait、cancel、
   `isPlayerIdentityEpochCurrent(...)`，然后才是 `checkDetailedSafety(...)`；
2. 未暂停时，`TaskPauseToken.waitIfPausedRevision` 在 `TaskPauseToken.java:110-113` 直接返回，不检查
   stop token；
3. `InputActionWorker.java:575-588` 的 generic epoch comparator 发现 epoch drift 后立即以
   `WINDOW_BINDING_CHANGED` cancel 并返回，后面的 frozen detector 不再获得机会。

因此可以先让一个 frozen list 在真实 queue 中等待，再同时关闭 stop 并提交 player-identity drift，最后
释放前序 blocker。worker 会先发布 `WINDOW_BINDING_CHANGED`，而不是 frozen detector 已定义优先的
`STOP_REQUESTED`。这是 queue admission 后的第二个独立 typed-order 缺口。

同一缺口还存在于 monitor acquisition 边界：`runFrozenExactWindowExclusive` 的 `:424-440` 和
`runFrozenExactWindowActions` 的 `:490-507` 在 monitor 外做 typed check，取得 monitor 后却只做 raw
generation witness。若 stop/drift 在等待 monitor 期间一同关闭，raw witness 可再次先把 terminal 标成
binding drift。`isFrozenExactWindowStillOwned` 在 `:563-573` 已经提供正确的 typed-then-witness 次序，
但这两个 pre-focus 位置没有使用等价顺序。

Required repair：frozen request 的 worker preamble 不得先走 generic epoch cancellation；取得 context
monitor 后、exact focus 前还要按 typed safety -> generation witness 的固定次序重验。不得改变 legacy
request 顺序。新增真实 queue/worker case：queued frozen request 同时 stop + identity/generation drift，
断言 `NOT_STARTED/STOP_REQUESTED`、zero focus/input、one taken request，并用 latch/event 而不是 polling
sleep 证明 admission 顺序。

## Rechecked Resolved Items

本轮重新核验后，上方旧轮的三个历史 blocker 在当前字节中确已实质闭合，未被沿用为本轮 finding：

- frozen Alt 在 `InputActionWorker.java:605-655` 使用
  `pressShortcut(binding, windowId, shortcut)` 和
  `focusFrozenBindingInActiveTransaction(...)`；mutable overload 只留给 legacy branch，fallback 前和 real
  input 前均重验 ownership；
- frozen pause 在 `InputActionWorker.java:387-396` 使用 cancellation-aware revision wait；waiter interruption
  在 `InputActionQueue.java:813-825` 把 cancellation 交给已取走请求并等待 worker terminal，当前不存在旧轮
  的 paused join hang；
- `BarrierPauseToken` 在测试 `:1222-1232` 只会从 production paused loop 内触发 latch；pause/resume case
  `:461-509` 已证明实际 gate entry，不再以 click hook 充当 pause-entry 证据。

Worker 的其它 terminal-progress 路径也成立：per-step progress 在 `InputActionRequest.java:790-866` 冻结；
`successful && !cancelled` 才能发布 success；worker `InputActionWorker.java:205-224` 的 `finally` 兜底发布
terminal 并在 callback/cleanup 返回后释放 publication；action-list `:543-548` 有 final typed gate。未发现
第二个 completion hang 或 fabricated success blocker。

## Contract And Test Audit

| Requirement | Latest static result |
|---|---|
| Public typed API / immutable one-request list | Satisfied by `InputSequences.java:89-116`, `InputActionQueue.java:403-441` and double `List.copyOf`; one UUID-bearing request carries the whole list. |
| Binding object + exact fields + epoch frozen under monitor | Snapshot capture is satisfied at queue `:408-440`; terminal precedence is **blocked by P1-1**. No caller epoch, refresh, title search or second value comparator was added. |
| One transaction / one generation monitor / shared dispatcher | Satisfied structurally by worker `:120-132,490-550` and shared `execute(...)`; monitor spans exact focus and every action/delay. Pre-focus typed ordering is **blocked by P1-2**. |
| Alt exact overload/focus | Satisfied statically at worker `:605-655`; tests `:518-656` distinguish exact vs mutable overload, record each focus by object identity, cover success, attempted=false fallback, failed fallback and drift-before-fallback. |
| Pause/cancel completion barrier | Satisfied by queue `:813-825`, worker `:361-407`, and deterministic test `:669-727`; cancellation does not resume, re-enqueue or replay the request. |
| Named-test truth / compile surface | The file has 19 `@Test` cases and constructs real `InputSequences`, real in-memory queue and daemon worker at `:921-964`; `CountingQueue.take()` delegates to production at `:1014-1029`. QP1's `java.util.Objects.equals` at request `:458` and QT1's `assertSame` import at test `:45` close the known static symbol defects. The missing combined-gate cases leave P1-1/P1-2 unobserved. |
| Callback non-regression | Additive frozen routing remains split at worker `:128-132`; callback cleanup/terminal barrier tests remain present. The legacy callback fixture still contains its pre-existing `Unsafe` and `waitUntilQueued` polling, but no TURN-28Q/QT1 action-list acceptance case calls those helpers; no new private production reflection or polling proof was added. |
| No retry/session/ledger/TTL | Frozen factories set no deadline/session mode, queue offers once, worker consumes once, and no re-enqueue/replay/owner/ledger/durable workflow exists. Alt foreground fallback is the pre-existing one-shot delivery fallback, not a replay. The shared 120-second waiter budget predates this tranche. |

## 696 Anchor

Read-only baseline comparison confirms `696a12b0` `TeamReturnService.java:86-89` submitted one queue request with
`CLICK_LEFT(delay=150)` followed by `SLEEP(500)`. Current named success case `:360-401` carries exactly those two
elements in one taken immutable request, and current worker uses the shared click dispatcher plus the same 500 ms
hold. Single global queue ownership and timing are preserved. No approved business difference, retry, session,
ledger or TTL was found.

## Reviewed Snapshot And Constraints

| File | Lines | SHA-256 |
|---|---:|---|
| `InputActionRequest.java` | 1148 | `7f4f8fdc4baf90f613fa6556f2a860849933724a6de5591d07d1eeb2d91ab0c8` |
| `InputActionQueue.java` | 870 | `c53a423e98e7ba4d698caa937788e5c6654100971ed8c24a9daef645a7173b6a` |
| `InputActionWorker.java` | 811 | `225a9f3be56d18f0374f78c9b3bea7352e8b4db444d288ecf7f1d51511377f43` |
| `InputActionFrozenExclusiveContractTest.java` | 1283 | `f72c7db076f7944555d3c89b7a8a1f3b1a2e6f396efe71c5ca00d801c07fd38c` |

本轮没有运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input；因此只给出
source/test-source review，不伪造 build 或 test 通过。没有修改 Java、原卡、`ACTIVE_WORK`、dashboard、
dirty/untracked 文件或任何 Git 状态；唯一写入是本报告的 append。

## Latest Verdict

**BLOCKED: `P0/P1/P2 = 0/2/0`.** Alt exact binding、pause/cancel completion 和 QP1 compile symbol 的旧问题
已闭合，但 queue admission 与 worker preamble/monitor acquisition 仍未把 typed safety 放在所有 generation
witness/comparator 之前。修复并补齐上述两个 combined-gate public-path cases 后，需要在新字节上重新独立审查；
本结论不是 READY、CARD APPROVED 或 build/test 通过。

<!-- TRUE_EOF: CR271 TURN-28Q INDEPENDENT DELIVERY REVIEW R2 LATEST-ROUND BLOCKED P0P1P2=0/2/0 QUEUE-TYPED-ORDER WORKER-TYPED-ORDER NOT-BUILT-NOT-RUN 2026-07-16T10:52:47.2169621-04:00 -->
