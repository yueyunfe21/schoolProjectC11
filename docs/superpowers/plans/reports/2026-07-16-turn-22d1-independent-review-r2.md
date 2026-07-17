# TURN-22D1 Repair #1 独立 Delivery Review R2

## 结论

**APPROVED**

- 严重级别：`P0/P1/P2 = 0/0/0`。
- 审批范围：External A Repair #1 的 production source + named-test source delivery。
- 不代表 named test、DHXY compile、TURN-22 聚合卡或 fresh runtime 已通过；这些门仍独立待办。
- 无返修项。若下列已审 SHA、frozen action-list API 或 TURN-22D1 true EOF 再发生变化，本结论失效，必须按新字节重新独立审查。

## 独立性与审查边界

- 角色：TURN-22D1 Repair #1 独立 delivery reviewer R2；不是实现者、父级或 R1。
- 未打开、未引用任何独立 R1 报告。TURN-22D1 原卡内为满足“完整读取最新 true EOF”而出现的历史审查文字，不作为本结论证据。
- 完整读取了 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、权威计划第 14-19 节、HTTPS turn 协议、`docs/业务逻辑.md`、TURN-22D1 原卡至最新 true EOF、当前目标源码/测试、`TurnExecutionWindow`、frozen action-list queue/request/worker API，以及 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的 `TeamReturnService`。
- 已审交付 SHA：
  - `TurnInputStepExecutor.java`：`a64422b061021dcbcec51837ac9f68a59bf21d57984469703cd1a68c2963134e`。
  - `TurnInputStepExecutorContractTest.java`：`f5a7992fc6566f00b56f4e7e21c8e66fcf328f519523e73d6858ae93042e7a81`。
- 未运行 Maven、JUnit、compile、package、runtime、application/server/Task/UI/capture/input；未执行 Git mutation。

## 逐文件证据

### 1. `TurnInputStepExecutor.java`

1. `:182-188` 将完整 mouse action list 一次性交给公开的 `submitFrozenExactWindowActionsAndWait(...)`，参数是同一个 `TurnExecutionWindow.context()` 和 `binding()`；本文件没有二次 refresh、title search、resolver 或 comparator。
2. `:185-188` 外层 `WindowTaskContextHolder.callWith(...)` 只临时绑定 action context；恢复由 holder 的 `finally` 完成，不改变 frozen generation witness。
3. `:189-202` 只有 `InputActionExecutionResult.isCompleted()` 才映射 `COMPLETED/OK`；typed `STOP_REQUESTED` 映射 `STOPPED`；其它 drift、cancel、failed、`NOT_STARTED`、`PARTIALLY_COMPLETED`、`STARTED_UNKNOWN` 均不能进入成功分支，也没有 retry/replay。
4. `TurnInputActionMapper.java:39-47` 把单次左键映射为一个 `CLICK_LEFT(clickDelayMs)`，并在 `queueHoldMs>0` 时只追加一个同列表 `SLEEP`。这与 `696a12b0` `TeamReturnService.java:86-89` 的一次 `CLICK_LEFT(150) -> SLEEP(500)` 完全一致。
5. 本文件未引入 retry、session、owner、ledger、TTL、durable workflow、OCR 或业务决策。

### 2. `TurnExecutionWindow.java` 与 binding refresh/generation

1. 生产 resolver `TurnExecutionWindow.java:51-87` 真实执行 `taskManager.getRunner(windowId)`、registered context/windowId 校验、`refreshAndCommit(context)`、live HWND/geometry 校验、metadata 构造，最后才通过 private constructor 生成 immutable snapshot。
2. 测试 fixture `TurnInputStepExecutorContractTest.java:478-494` 直接调用上述公开 `resolveForAction(...)`，没有再调用 private constructor。初始 context 没有 live binding；唯一能发布 `12345/pid=88/rect=137,241,10,10` 的路径是 scripted refresh 的 `:506-514 -> context.setNativeBinding(binding)`，所以 resolver 的 refresh seam 不是装饰性调用，也无法被 fixture 绕过。
3. frozen API 的 generation 证明仍来自生产代码：`InputActionQueue.java:393-421` 在 `synchronized(context)` 内要求当前 binding 对象与 frozen binding 为同一对象并冻结 epoch；`InputActionRequest.java:440-450` 以对象身份加 exact value/epoch 校验；`InputActionWorker.java:470-534` 在同一 context monitor 内完成 generation check、focus 和整张 action list。

### 3. test-owned `Unsafe` 不构成假证明

1. `TurnInputStepExecutorContractTest.java:541-579` 的 `Unsafe.allocateInstance` 只分配测试文件自己声明的 `BareWindowTaskRunner` subclass，用来避开 production runner 的 20 协作者/线程构造器。
2. 该 subclass 只覆盖公开的 `getWindowContext()` 与 `getCurrentTask()`；没有读取或写入 `WindowTaskRunner` 私有字段，没有构造 `TurnExecutionWindow`，也没有覆盖 resolver、refresh、executor 或 real queue generation check。
3. `TestTaskManager` 只作为 in-memory registry 返回该 runner。生产 resolver 仍必须完成 windowId 校验、refresh commit、live binding 校验、stop derivation和 snapshot 构造。
4. 因此这里虽通过 JDK `Unsafe.theUnsafe` 使用 reflection，但没有 private-production reflection，也不能制造 executor 成功、sentinel restore 或 A-B-A rejection。它不越过本卡要验收的生产边界，不构成 P1/P2。

### 4. sentinel、动作列表、typed STOP 与 A-B-A

1. Sentinel：测试 `:295-343` 在不同的 `window-sentinel/HWND=99999/pid=4242/rect=900,800,20,30` context 内调用真实 executor；返回后断言同一 sentinel 对象、同一 binding 对象、同一 windowId 和 player epoch，离开外层 scope 后 holder 为空。恢复的是原引用，不是值相等的重建对象。
2. 一次提交：同一测试 `:320-342` 断言 frozen submission 恰好一次、收到 resolver 的 exact context/binding 对象，且列表严格为 `[CLICK_LEFT(delay=150), SLEEP(500)]`。
3. Typed STOP：`:351-367` 让 frozen boundary 返回 `STOP_REQUESTED`，真实 executor 必须得到 `STOPPED/STOPPED`，且 submission count 仍为 1，证明没有重试；`:246-259` 还覆盖 resolver metadata 的 pre-input stop short-circuit。
4. Terminal/uncertain：production 的成功门是 `status == COMPLETED`；测试 `:222-243` 覆盖非完成 queue result，真实 A-B-A case 覆盖 binding failure。`STARTED_UNKNOWN/PARTIALLY_COMPLETED` 由同一 `isCompleted()` 封闭门排除，不会伪成功；D1 没有新增任何 resend/re-enqueue 路径。
5. A-B-A：`:375-416` 使用真实 `InputActionQueue`，把 frozen A 改成 B 后再恢复为字段完全相等但对象不同的 A'；结果为 `FAILED/INPUT_QUEUE_FAILED` 且 `realQueue.size()==0`。这直接证明 generation drift 在 enqueue 前被拒绝，provider 不可能收到输入。

## 影响与返修条件

- 影响：Repair #1 删除了 production-private `TurnExecutionWindow` 构造器反射，改由公开 resolver 和 refresh commit 产生 frozen binding witness；没有改变 production SHA、TeamReturn 次序、动作数量、延时或业务结果。
- 当前无 P0/P1/P2，故无立即返修条件。
- 重新打开条件：任一已审 SHA 改变；测试改回 hand-built/private `TurnExecutionWindow`；`Unsafe` 开始访问 production 私有状态或替代 resolver/executor/queue 结果；sentinel 不再恢复原对象；150/500 被拆成两次 submission；typed STOP/uncertain 被映射为成功；A-B-A 可以 enqueue；或引入 retry/session/ledger/TTL。
- 后续门：在 Java writer 稳定且父级允许后，仍须运行授权的 named test 与适用 DHXY compile；本 R2 不以未运行结果冒充通过。

**无已批准业务差异；按 `696a12b0` 的一次 queue `CLICK_LEFT(150)+SLEEP(500)` 等价迁移。**

<!-- TRUE_EOF: TURN-22D1 REPAIR-1 INDEPENDENT DELIVERY REVIEW R2 APPROVED P0P1P2=0/0/0 SOURCE-TEST-SOURCE-ONLY NAMED-TEST-COMPILE-PENDING 2026-07-16 -->
