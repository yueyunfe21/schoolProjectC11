# TURN-28Q Repair #3 Deterministic Test Boundary Preflight Helper

Status: `PRECHECK_COMPLETE`

## 角色与结论边界

- 角色：CR271 Internal helper，只为 TURN-28Q Repair #3 的两个新 deterministic public-path
  acceptance case 做最小测试边界预检。
- 不是 implementation owner、reviewer、parent/final reviewer 或批准者；本文不 claim、不交付、不写
  `Approved/Blocked`，也不替代 Repair #3 后续 named test、compile 或独立 review。
- 本轮唯一写入是本报告。Java、TURN-28Q 原卡、`ACTIVE_WORK`、其它文档、POM、runtime、input 和 Git
  状态全部只读。

## 已完整读取的当前权威切片

1. `AGENTS.md` 392 行；`docs/DHXY_CONTEXT.md` 1349 行。
2. `docs/ACTIVE_WORK.md` 当前 CR271 `11:03` 块。该块把 TURN-28Q 当前状态冻结为
   `P0/P1/P2=0/2/0 / REPAIR #3 REQUIRED`，并明确只补 pre-enqueue 与 queued/taken 两个
   STOP+drift public-path 证据。
3. TURN-28Q 原卡 Parent Review #6 全节及 physical true EOF，原卡 SHA-256
   `4a6ebbc70154d1df0c57bb198987b08558706bc2fc511998032fbed3b7da03f0`。
4. 最新独立 R2 全文，包括 `LATEST INDEPENDENT REVIEW ROUND R2` 与 true EOF，SHA-256
   `97fd4f0b9557da7cfcc51e7dbdb764b380f612ed74240c0ff5a649bf3943ced8`。
5. 当前 `InputActionQueue`、`InputActionWorker`、`InputActionRequest` 与同一 named test 全文：

| 文件 | 行数 | SHA-256 |
|---|---:|---|
| `InputActionQueue.java` | 870 | `c53a423e98e7ba4d698caa937788e5c6654100971ed8c24a9daef645a7173b6a` |
| `InputActionWorker.java` | 811 | `225a9f3be56d18f0374f78c9b3bea7352e8b4db444d288ecf7f1d51511377f43` |
| `InputActionRequest.java` | 1148 | `7f4f8fdc4baf90f613fa6556f2a860849933724a6de5591d07d1eeb2d91ab0c8` |
| `InputActionFrozenExclusiveContractTest.java` | 1283 | `f72c7db076f7944555d3c89b7a8a1f3b1a2e6f396efe71c5ca00d801c07fd38c` |

当前字节与最新 R2 审查快照一致。`InputActionRequest.detectFrozenExactWindowFailure(...)` 当前已经把
STOP 放在 identity/binding drift 前；本次测试只需要证明 queue/worker 的 public route 是否尊重这个既有
typed priority，不需要也不允许修改 Request。

## 最小边界结论

两个新 case 都应继续放在现有
`InputActionFrozenExclusiveContractTest.java`，并且都从 public
`InputSequences.submitFrozenExactWindowActionsAndWait(...)` 进入真实
`InputActionQueue -> InputActionWorker`。不新建第二测试类，不直接调用 queue public method，不手工构造
`InputActionRequest` 或 `InputActionExecutionResult`。

现有 Harness 足够承担绝大部分证据，只需给 `CountingQueue` 增加一个 test-private、一次性的 take handoff
gate。该 gate 只观察并暂缓真实 `super.take()` 已返回的 request，不模拟 enqueue、admission、worker safety、
focus、action dispatch 或 terminal publication。

Parent Review #6 还要求 callback 与 action-list 两个 queue entry 的 production 排序一致；该源码义务仍由
Repair #3 implementation/source review 检查。本次用户冻结的是两个 action-list acceptance case，不应偷加第三个
callback matrix，也不应改已有 callback fixture。

## 现有 Harness 可直接复用点

| 现有构件 | 本次用途 |
|---|---|
| `Harness(MouseRecorder)` | 构造真实 in-memory queue、public `InputSequences`、真实 daemon worker 与真实 input transaction coordinator。 |
| `Harness.taskContextHolder` | 让 public submission 捕获真实 `TaskPauseToken` / `TaskStopToken`。 |
| `Harness.context(...)` | 创建独立 `WindowRuntimeContext` 与初始 binding；不需要 `Harness.resolve(...)`。 |
| `CountingQueue.take()` | 已调用真实 `super.take()` 并记录 worker 实际取走的 request，是 one-take/request-id 的事实来源。 |
| `MouseRecorder.calls` + `Harness.inputCalls` | 双重证明 action dispatcher 没有送出物理 input。 |
| `Harness.focusCalls` + `focusedBindings` | 证明 target request 在 terminal 前没有进入 focus。 |
| `CountingRefreshService.calls` | 证明 frozen public path 没有 refresh。 |
| `TaskExecutionContext.builder()` | 给两个 case 安装真实 task code/window id/pause/stop tokens。 |

以下现有设施明确不用于这两个新 case：

- 不用 `Harness.blockWorker()`。它会制造额外 taken request 和一次 focus，直接污染第二例的
  `one take / zero focus` 精确断言。
- 不用 `waitUntilQueued(...)`。它依赖 `queue.size()` + `Thread.sleep(1)` polling，正是本轮禁止的
  scheduling guess。
- 不用 `BarrierPauseToken`。本轮不测 pause gate，复用它会引入无关状态。
- 不用 `Harness.resolve(...)`、`Unsafe.allocateInstance(...)`、private reflection 或源码扫描。

## CountingQueue 唯一最小扩展

在现有 `CountingQueue` 内增加一个一次性 `TakeHandoffGate`，只需要四个事实字段：

1. `expectedDescription`：只拦指定 target，避免误拦其它 request；
2. `CountDownLatch taken`：真实 `super.take()` 返回并已写入 `taken` list 后触发；
3. `CountDownLatch releaseToWorker`：测试线程关闭 STOP 与 drift 后才释放；
4. `AtomicReference<InputActionRequest> request`：保存刚取走的真实 request，供 snapshot/request-id 断言。

精确 `take()` 顺序必须是：

1. 调用 `InputActionRequest request = super.take()`；
2. `taken.add(request)`；
3. 读取一次当前 armed gate；description 不匹配就原样返回；
4. 匹配时先 `gate.request.set(request)`，再 `gate.taken.countDown()`；
5. 只做一次 `gate.releaseToWorker.await()`；
6. 在 `finally` 清除该 gate，然后把同一个 request 返回给 production worker `handle(...)`。

`armNextTake(description)` 必须在 target submission thread 启动前调用，并拒绝第二个同时 armed gate；不要用
CAS retry loop。daemon worker 可能早已阻塞在 `super.take()` 内，这不构成 race，因为 gate 是在
`super.take()` 返回后才读取。`taken` event 因而同时证明 request 已被真实 queue offer 且已被唯一 worker 取走，
不需要另造 queued flag 或查看 queue size。

测试线程必须用 `try/finally { releaseToWorker.countDown(); }` 保证 assertion failure 也不会把 daemon worker
永久卡在 test gate。bounded `latch.await(timeout)` 与最终 `thread.join(timeout)` 只用于失败上限和线程收口，
不得拿 timeout/thread liveness 证明业务 ordering。

## Case 1: pre-enqueue STOP + A -> B -> A'

建议 test 名：
`preEnqueueStopWinsOverValueEqualRebindThroughThePublicFrozenActionListPath`

### Setup 与事件顺序

1. 新建 `MouseRecorder`、fresh `Harness(recorder)`、context 和原始 binding A；记录初始 identity epoch。
2. 复用现有 A-B-A' 构造：先发布 geometry-x 改动后的 B，再发布 geometry 恢复后的 A'。A' 必须与 A 的
   HWND、processId、x/y/width/height 逐字段相同，但 `assertNotSame(A, A')`；epoch 必须保持不变，证明这里只
   关闭 object-generation witness，不混入 identity epoch drift。
3. 新建真实 `TaskPauseToken`、`TaskStopToken` 和 `TaskExecutionContext`，在 public call 前执行
   `stopToken.requestStop("stop-with-preenqueue-aba")`。
4. 在调用前显式确认两个 gate 都已关闭：`stopToken.isStopRequested()==true` 且
   `context.getNativeBinding()!=A`。
5. 通过 `taskContextHolder.callWith(taskContext, ...)` 调一次 public
   `harness.sequences.submitFrozenExactWindowActionsAndWait(...)`，仍携带基线 list
   `[CLICK_LEFT(300,400,150), SLEEP(500)]`。不得启动 waiter thread、不得 latch、不得 retry；这是同步
   pre-enqueue rejection。

### 精确断言

- result 非 null，`requestId` 非 null/非 blank；`isCompleted()==false`。
- `status == NOT_STARTED`，`started==false`，`startedStepIndex == -1`，
  `lastCompletedStepIndex == -1`。
- `safetyReason == STOP_REQUESTED`；reason 包含 `task-stop`，不得是 generation/binding-change reason。
- `harness.queue.taken.size() == 0`。
- `harness.focusCalls.get() == 0` 且 `focusedBindings` 为空。
- `recorder.calls` 为空且 `harness.inputCalls.get() == 0`。
- `harness.refresh.calls.get() == 0`。
- A' 的 exact fields 与 A 逐项相同、object identity 不同、epoch 未变化的前置断言必须保留；否则该 case
  退化为普通 STOP-only test，无法击中 P1-1。

当前冻结字节应在这里错误返回 `WINDOW_BINDING_CHANGED`；Repair #3 后才应返回上述 typed STOP。这保证
case 不是重复已有 `stopClosedBeforeEnqueue...` 或 `valueEqualRebind...` 的单门自证。

## Case 2: queued/taken STOP + identity/generation drift

建议 test 名：
`takenFrozenRequestStopWinsOverIdentityAndGenerationDriftBeforeWorkerHandling`

### Identity fixture

`Harness.context(...)` 默认 title 不可解析。为确定地产生真实 identity epoch drift，在 target request 前先对同一
HWND/class/process/geometry 发布一个可解析 identity A，例如：

`大话西游2经典版 - 江山如画 - 忆叶知秋（ID：451753529）`

然后捕获此时的 exact binding A 与 `frozenEpoch`。初始 fixture 自己是否曾增加 epoch 不重要，权威基点是 request
提交前记录的 `frozenEpoch`。

worker take gate 关闭后，再对相同 HWND/class/process/geometry 发布 identity B：

`大话西游2经典版 - 江山如画 - うprinoe大叔（ID：316365558）`

这会发布不同 binding object 并把 `playerIdentityEpoch` 从 `frozenEpoch` 增加 1。无需把 context 标成 busy，
无需依赖 `identitySuspended`；epoch drift + object-generation drift 已经精确击中 worker P1-2。

### Latch/event 顺序

| Event | 唯一允许的动作 | 此时可证明的事实 |
|---|---|---|
| E0 | 建立 identity A，捕获 A 与 `frozenEpoch` | request 尚不存在，A 是当前 generation。 |
| E1 | 建立真实 pause/stop tokens 与 task context | STOP 仍 open。 |
| E2 | `gate = harness.queue.armNextTake(targetDescription)` | 下一份匹配 request 会在真实 take 后暂停交接。 |
| E3 | 启动一个 waiter thread，经 `taskContextHolder.callWith` 调一次 public frozen action-list API | 只有一次 submission，无 callback/nested queue。 |
| E4 | 主线程等待 `gate.taken.await(boundedTimeout)` | `super.take()` 已返回；request 已真实 queued + taken，但尚未进入 `handle()`。 |
| E5 | 在 gate 仍关闭时检查 captured request | exactly one take；request binding `assertSame(A)`；request epoch 等于 `frozenEpoch`；focus/input/refresh 仍为 0。 |
| E6 | `stopToken.requestStop(...)`，随后发布 identity B | STOP、identity epoch drift、binding generation drift 都已关闭。 |
| E7 | 断言 stop=true、epoch=`frozenEpoch+1`、current binding 与 A object-distinct，再 `releaseToWorker.countDown()` | worker 只能在所有 gate 已关闭后进入 preamble。 |
| E8 | waiter bounded join，读取 worker 真实 terminal result | 结果 ordering 来自 worker，不来自 test fake。 |

`releaseToWorker.countDown()` 必须放在 `finally`；正常路径只 count down 一次。不要先释放再漂移，也不要以
“线程看起来还活着”替代 E4 的真实 take event。

提交 payload 继续用单 request 的 `[CLICK_LEFT(300,400,150), SLEEP(500)]`。这两个 action 在本 case 中都
不得启动；保留它们只是让测试仍属于 reviewed 696 baseline action-list route，不是用 500ms 等待 ordering。

### 精确断言

- `gate.request.get()` 非 null；`harness.queue.taken.size() == 1`。
- taken request 的 `requestId` 等于 terminal result 的 `requestId`，证明无 re-enqueue/replay；taken request 的
  binding 用 `assertSame(A, ...)`，captured epoch 等于 `frozenEpoch`。
- waiter 已结束且 result 非 null；`isCompleted()==false`。
- `status == NOT_STARTED`，`started==false`，`startedStepIndex == -1`，
  `lastCompletedStepIndex == -1`。
- `safetyReason == STOP_REQUESTED`；reason 包含 `task-stop`，不得是
  `player-identity-epoch-changed`、`frozen-generation-changed` 或其它 binding-change reason。
- `harness.focusCalls.get() == 0` 且 `focusedBindings` 为空。
- `recorder.calls` 为空且 `harness.inputCalls.get() == 0`。
- `harness.refresh.calls.get() == 0`。
- identity B 发布后的前置事实必须钉死：epoch 恰为 `frozenEpoch + 1`、current binding 与 A
  `assertNotSame`，同时 HWND/process/geometry 不变。否则该 case 可能只测 STOP 而未真正关闭 drift gate。

当前冻结字节在 E8 会先走 `InputActionWorker.isPlayerIdentityEpochCurrent(...)`，错误发布
`WINDOW_BINDING_CHANGED`；Repair #3 必须让 frozen typed safety 先得到 `STOP_REQUESTED`。take gate 位于
`super.take()` 与 `handle()` 之间，因此该失败/通过均是确定性的，不依赖 OS scheduling。

## 明确禁止项

- 禁止 `waitUntilQueued(...)`、`queue.size()` polling、spin polling、重复查询直到成功。
- 禁止 `Thread.sleep(...)`、`join(50)`、elapsed-time 猜测或“等待 worker 大概走到某处”。
- `InputAction.sleep(500)` 仅是 immutable payload/baseline 元素；不得把它当测试协调 sleep。第二例在正确实现下
  根本不会执行该 step。
- 禁止 retry、第二次 submission、re-enqueue、replay、auto-resume、session、ledger、TTL、deadline 或 durable
  workflow。
- 禁止 blocker request、额外 focus、额外 refresh、真实 desktop input、PostMessage、runtime/UI/capture。
- 禁止 direct queue call、private production reflection、`Unsafe`、源码字符串扫描、手工 terminal result 或
  production test hook。
- 禁止改造已有 callback tests、pause tests、Alt matrix 或其它 19 个 current case 来“顺便清理”。本次只新增
  两个 case 和 `CountingQueue` 的一次性 take observation gate。

## 交付前测试源码自检点

1. named test 应从当前 19 cases 增加到 21 cases；不合并/删除旧 case。
2. 两个新 case 都只调用 public `InputSequences` frozen action-list facade。
3. Case 1 target take 为 0；Case 2 target take 恰为 1。不能用总 request id 或 queue size 推断。
4. 所有 ordering 都由同步 pre-enqueue state 或 `super.take()` 后 latch event 证明。
5. 所有 terminal status/safety/progress 都来自 real worker/request future。
6. 不新增 import/fixture 以外的生产依赖；`InputActionRequest.java` 继续只读。
7. 保持 `CLICK_LEFT(delay=150) -> SLEEP(500)`、一个 request、一个 transaction、无 retry 的
   `696a12b0` 等价语义。

## 本 Helper 操作边界

- 未修改或创建任何 Java/test source、TURN-28Q 原卡、`ACTIVE_WORK`、dashboard、POM/config/resource 或其它
  文档。
- 未运行 Maven、JUnit、compile、package、runtime、application、server、Task、UI、capture 或 input。
- 未执行 commit、stage、branch、merge、rebase、checkout、reset、restore、clean 或其它 Git mutation。
- 全部既有 dirty/untracked 内容保持原样；本报告不构成 Repair #3 claim、delivery、review 或 approval。

<!-- TRUE_EOF: CR271 TURN-28Q REPAIR-3 TEST-BOUNDARY-PREFLIGHT PRECHECK_COMPLETE TWO-DETERMINISTIC-PUBLIC-PATH-CASES TAKE-HANDOFF-LATCH NO-POLLING-SLEEP-RETRY 2026-07-16T11:16:42.8822137-04:00 -->
