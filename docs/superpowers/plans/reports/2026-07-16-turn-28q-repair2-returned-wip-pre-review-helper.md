# TURN-28Q Repair #2 OWNER RETURNED 最新 WIP 独立 Pre-review Helper

## 角色与边界

- 角色：CR271 Internal helper，只做 `TURN-28Q Repair #2` owner returned 后四文件最新 WIP 的独立
  pre-review；不是 implementation owner、正式 reviewer、批准者或 parent adjudicator。
- 本文不改 TURN-28Q / TURN-28QT1 原卡，不改变卡状态，不给出 Approved / Blocked 判定。
- 本轮只写本报告。未修改 Java、测试、原卡、dashboard 或其它文件；未运行 Maven、JUnit、compile、
  package、runtime、application、server、Task、UI、capture 或 input；未做任何 Git mutation。
- 读取期间测试依次从 owner-returned 的 `871 / 223f55ff...` 移到 `1077 / 2e8a1ba7...`、首次
  TURN-28QT1 delivery 的 `1224 / 82750732...`，再经 parent Repair #1 移到
  `1283 / f72c7db0...`。本报告没有把任何活动中间态冒充最新态；最终切片在
  `2026-07-16T10:17:44-10:18:34-04:00` 连续六次读取得到相同 test/card SHA。

## 完整读取与权威边界

已完整读取：

1. `AGENTS.md`（392 行）与 `docs/DHXY_CONTEXT.md`（物理内容 1349 行）。
2. `docs/ACTIVE_WORK.md` 顶部当前 CR271 的 `10:13 / 10:00 / 09:56 / 09:50 / 09:38` 相关块。
3. 权威计划 `2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节，包括第 17 节精确
   写集、第 18 节构建门和第 19 节显式测试合同。
4. `2026-07-15-https-turn-thin-client-protocol-design.md` 全文，以及 `docs/业务逻辑.md` 全文。
5. TURN-28Q 原卡最新 603 行物理 EOF、两份旧独立 reviewer 报告 R1/R2、Repair #2 preflight 全文。
6. 当前三份 production 与 named test 全文；为追踪 owner-returned 后测试 WIP 的权威来源，额外完整读取
   TURN-28QT1 子卡最新 332 行物理 EOF，包括 parent Test-Source Review #1 与 Repair #1 delivery。

本次判断只使用下列已冻结约束：

- `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 是业务基线；stop / pause / 用户停止不是业务
  `FAILED`，不得改变任务 phase、fallback、点击顺序、验证次数或重试语义。
- 一份 action list 是一个 request、一个全局 input transaction、一个 frozen context generation；不得
  re-enqueue、retry、replay、拆 list、增加 session / ledger / TTL / deadline / durable workflow。
- frozen window 只接受 resolver 已冻结的 exact binding object 与 identity epoch；HWND keyboard、focus、
  real-input fallback 均不得在 frozen monitor 内重新解析或 refresh mutable current binding。
- Repair #2 preflight 对 paused waiter production 增量的原文边界是
  `frozenExactWindow && !hasExclusiveCallback()`；frozen callback 与普通 legacy request 的 pause/completion
  边界应保持不变。
- 第 19 节和 TURN-28QT1 要求用现有 public
  `InputSequences -> InputActionQueue -> InputActionWorker` in-memory harness 给出确定性证据；本 helper
  不执行该测试门。

## 最终审查切片

| 文件 | 行数 | SHA-256 |
|---|---:|---|
| `InputActionRequest.java` | 1148 | `4e40fcd4ce64b9cc5b7c1d4c6f5cf308dcb9933050629b687fae104105ec0652` |
| `InputActionQueue.java` | 870 | `c53a423e98e7ba4d698caa937788e5c6654100971ed8c24a9daef645a7173b6a` |
| `InputActionWorker.java` | 811 | `225a9f3be56d18f0374f78c9b3bea7352e8b4db444d288ecf7f1d51511377f43` |
| `InputActionFrozenExclusiveContractTest.java` | 1283 | `f72c7db076f7944555d3c89b7a8a1f3b1a2e6f396efe71c5ca00d801c07fd38c` |

三份 production 与 TURN-28Q owner-returned/parent freeze SHA 一致。测试是 TURN-28QT1 在
`10:17:37` 登记的 Repair #1 test-source delivery。子卡仍披露实现者是继续运行的旧 A session，而父卡最初要求
fresh A；这是 parent 需要处理的 provenance/process 事实，不是本 helper 的卡片结论。

## 四项逐项核实

### 1. Typed stop-only / final-stop

生产静态链路已经形成目标顺序：

- `InputActionRequest.java:448-463` 把 `isFrozenExactWindowGenerationCurrent()` 改成纯 object-generation
  witness；`frozenExactWindowFailure(...)` 在 `:474-479` 保留 detector 的 typed reason。
- `InputActionQueue.java:359-373` 与 `:425-439` 都先取 typed frozen failure，再检查 generation witness；
  有效 frozen snapshot 上的 pre-enqueue stop 应保持 `STOP_REQUESTED`，而不是被翻译成
  `WINDOW_BINDING_CHANGED`。
- `InputActionWorker.java:524-541` 每个 action 前先做 typed safety + generation gate；`:543-548` 在最后
  action 已记录 completed prefix 后再做 final gate。stop 在 action 0 内关闭时 later action 不应启动；stop 在
  final action 内关闭时不应发布 `COMPLETED`，但已完成 prefix 仍应保留。
- 最新测试已有 pre-enqueue stop（test `:736-770`）、stop-only after action 0（`:780-816`）和 final-action
  stop（`:824-856`），触发点均由 caller/worker 自身控制，不靠 sleep 猜测 ordering。

仍有两个真实风险：

1. **Production 静态编译表面错误。** `InputActionRequest.java:458` 使用未限定的
   `Objects.equals(...)`，但 imports `:8-16` 没有 `java.util.Objects`；同文件其它位置使用的是
   `java.util.Objects.equals(...)`。不运行编译也可确定该标识符无法解析。
2. stop-only 用例只断言 non-completed + prefix，未断言精确
   `PARTIALLY_COMPLETED`（test `:806-815`）；final-stop 只断言 `status != COMPLETED`（`:847-855`）。前者
   仍允许“later SLEEP 已启动但未知”的 `STARTED_UNKNOWN` 错误实现通过，不能完整冻结 zero-later-action。

### 2. Frozen Alt exact binding / zero refresh

生产静态链路已改为 exact-only：

- `InputActionWorker.java:605-610` 对 frozen request 调用现有
  `pressShortcut(request.getNativeBinding(), request.getWindowId(), shortcut)`；mutable overload 只留给
  legacy request。
- background 未执行或非 terminal 失败时，`:632-639` 先重证 ownership，再调用
  `focusFrozenBindingInActiveTransaction(...)`；`:650-654` 在不可逆 real input 前再次重证 ownership。
- `BoundWindowKeyboardService.java:106-137` 的 exact overload 不 refresh；
  `WindowAwareInputCoordinator.java:161-179` 的 exact focus 也不 refresh。

TURN-28QT1 Repair #1 已消除首次 delivery 的三个旧缺口，不能再把它们写成当前 finding：

- `assertSame` static import 已在 test `:45`；
- matrix 现在包含 background success（`:519-545`）、`attempted=false` fallback（`:554-582`）、
  attempted-but-failed fallback（`:589-617`）和 drift-before-fallback（`:625-657`）；
- `CountingFocusService` 在 `:1098-1120` 按顺序记录每次 binding，四类用例逐次以 `assertSame` 检查
  frozen binding object，mutable keyboard overload 与 refresh 均保持 0。

剩余 test evidence 只需小幅收紧：

- success、failed-fallback、drift 三例未都显式断言 `queue.taken.size()==1`；non-attempted 例已断言 one take。
- failed-fallback 应像 success/non-attempted 一样直接检查 exact keyboard call 的 binding object/windowId/shortcut；
  drift 应钉住 `STARTED_UNKNOWN`、`startedStepIndex=0`、`lastCompletedStepIndex=-1`，证明失败 action 没有被
  记成 completed prefix。
- test `:1092-1096` 声称“value equality 会接受 copy”，但 `WindowNativeBinding` 当前没有 override
  `equals/hashCode`，`assertEquals` 目前仍退化为 object identity。改用 `assertSame` 本身是正确且更明确的合同，
  但注释/子卡不应把当前类描述成 value-equality model；应改成“不依赖未来 equals 实现，显式冻结 identity”。

### 3. Paused waiter cancellation

生产链路对 frozen action-list 的目标问题可以静态闭合：

- queue waiter 在已 take 后被 interrupt 时，`InputActionQueue.java:813-825` 发送 cooperative cancellation，
  并等待 worker-owned terminal future。
- `InputActionWorker.java:378-397` 复用
  `waitIfPausedRevision(stopToken, request::shouldAbortPauseWait)`；
  `TaskPauseToken.java:107-137` 在 pause loop 内观察 wake condition，故无需 `resume()` 就能看到 request
  cancellation。Worker 仍持有同一 transaction/context monitor，退出后才冻结 terminal 并释放。
- test `:670-727` 在真实 pause-gate barrier 后 interrupt waiter，不调用 `resume()`，断言 later provider input=0、
  one take，并再提交新 request 证明 worker/transaction 可继续使用。

但存在一个 production scope risk：

- preflight 只授权 `frozenExactWindow && !hasExclusiveCallback()`；当前 Worker `:387` 写成
  `if (request.isFrozenExactWindow())`。因此 frozen exclusive callback 也从旧
  `waitIfPaused(stopToken)` 改成 cancellation/identity wake-aware revision wait，影响 `handle(...):98` 和
  `runFrozenExactWindowExclusive(...):424` 的 pre-focus/pre-callback pause 语义。现有 callback tests 没有 paused
  callback non-regression case，也没有已批准业务差异授权这个扩张。

测试本身还有两个边界未钉死：

- cancellation 用例未断言精确 `PARTIALLY_COMPLETED`、`startedStepIndex=0`、`CLEAR` safety 与
  `waiter interrupted` reason；仅有 prefix 不能排除 later SLEEP 曾进入 `STARTED_UNKNOWN`。
- 如果 cancellation 机制回归，`:701-704` 的 bounded join 会失败，但 non-daemon waiter 留在永不 resume 的 pause
  中，可能让 named-test fork 不能干净退出。正常证明仍不得调用 `resume()`；可把该 test waiter 设为 daemon，
  或增加只在失败清理路径触发的 bounded cleanup，使测试报告失败而不是永久挂住测试进程。

### 4. Pause-gate test barrier

最新 barrier 已修正旧 reviewer 指出的 false-positive：

- `BarrierPauseToken` 在 test `:1222-1233` 不在 method entry 无条件 count down；它包装真实
  `wakeCondition`，只在 production `TaskPauseToken.waitIfPausedRevision(...)` 的
  `while (pauseRequested)` 内求值时放行 latch。未暂停的 pre-focus check 在 production `:111-113` 直接返回，
  不会触发 barrier。
- pause/resume 用例在 test `:487-493` 等待 barrier 后才断言 zero later action，再 resume；cancellation 用例
  在 `:697-710` 等待同一真实 barrier 后才 interrupt waiter。两个 ordering 都不靠 wall-clock sleep 推测
  worker 是否到达 pause gate。

在当前源码切片上，这个 barrier 的静态结构符合要求；仍需后续 named test 实际执行确认，本 helper 没有产生
运行证据。

## Successor 最小写集冻结

当前切片不是纯 test-only 就能形成可编译交付：至少有一处 production 标识符错误。successor 必须由 parent
明确领取后串行处理，不能在现有 test-only delivery 上静默写 production。

### 必要 production 写集

1. `src/main/java/com/bot/dhxy/input/action/InputActionRequest.java`
   - 仅把 `:458` 改为 `java.util.Objects.equals(...)`；与同文件 `:910/:933/:971` 既有写法一致。
   - 不改变 detector、typed reason、generation witness 或 terminal 语义。
2. `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
   - 为保持 Repair #2 preflight 的 action-list-only 边界，`:387` 最小改为
     `request.isFrozenExactWindow() && !request.hasExclusiveCallback()`。
   - 如果 parent 决定保留 broad callback 行为，必须先在卡内明确批准该范围差异，并另加 paused callback
     contract evidence；不能由 test successor 默许。baseline-equivalent 的最小路线是上述单条件收窄。

`InputActionQueue.java` 对后续修复保持只读；其 typed-before-witness 顺序无需再改。

### 最小 test-only 写集

只允许继续修改
`src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java`，不新建第二测试文件：

1. stop-only、final-stop、paused cancellation 增加精确 `PARTIALLY_COMPLETED`；cancellation 另钉住
   `startedStepIndex=0`、`CLEAR`、`waiter interrupted` reason 与 one-take/no-later-action。
2. Alt success/failed/drift 三例补 one-take；failed fallback 补 exact call 的 binding/windowId/shortcut；drift
   补 `STARTED_UNKNOWN` 与 `0/-1` progress，继续保持 zero real input/no retry。
3. 给永不 resume 的 cancellation waiter 加失败隔离（优先 `setDaemon(true)`，不改变正常路径），使回归时测试
   给出失败而不是遗留 non-daemon hang。pause/resume waiter可做相同 failure containment，但不得用 sleep/polling
   代替 barrier。
4. 只修正 `CountingFocusService` JavaDoc 对 `WindowNativeBinding.equals` 的错误事实；保留 ordered recorder 与
   `assertSame`，不撤回已经正确补上的 non-attempted/focus identity 证据。

明确只读：`BoundWindowKeyboardService.java`、`WindowAwareInputCoordinator.java`、`TaskPauseToken.java`、
`InputSequences.java`、所有 caller/Task/Service/protocol/Cloud 文件、POM/config/resources。不得新增 wrapper、
retry/replay/session/ledger/TTL/deadline/test hook，也不得改变 `CLICK_LEFT(delay=150) -> SLEEP(500)` 基线。

## Pre-review 收束

- typed stop/final gate、frozen exact Alt、action-list paused cancellation 与真实 pause barrier 的主要结构均已
  出现在最新 WIP；本 helper 未把它们转化为批准结论。
- TURN-28QT1 Repair #1 已真实补齐 `assertSame` import、non-attempted Alt 与逐次 focus identity；当前不得继续
  引用已被新 SHA 消除的旧 finding。
- successor 首先要处理 production 的静态 symbol-resolution 缺口；Worker callback 范围偏移必须由 parent
  选择“收窄回 preflight”或“显式批准并补 callback contract”。其余增量严格留在现有 named test 内。
- 本报告没有 Maven/JUnit/compile/runtime/input 证据，也没有 Git 证据或状态变更。

TRUE_EOF PRECHECK_COMPLETE
