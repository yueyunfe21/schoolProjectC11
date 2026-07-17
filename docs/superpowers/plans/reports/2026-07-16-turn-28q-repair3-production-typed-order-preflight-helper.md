# TURN-28Q Repair #3 production typed-order acceptance preflight

## 1. 角色与边界

- 角色：CR271 Internal helper，仅整理 TURN-28Q Repair #3 的 production typed-order 父级验收前置证据。
- 本报告不是实现记录，也不形成 reviewer 或 approver 结论。
- 本轮没有修改 Java、TURN-28Q 原卡、权威计划、`docs/ACTIVE_WORK.md` 或 dashboard。
- 本轮没有运行 Maven、JUnit、compile、runtime、application、server、Task、UI、capture 或 input，也没有执行 Git mutation。
- 本报告只覆盖 frozen request 在两个 public queue entry、worker preamble 和两个 context-monitor/focus 边界的顺序，以及两组确定性验收场景和 legacy 不变边界。

## 2. 已读取权威材料与当前快照

已完整读取本轮指定材料：

- `AGENTS.md`，392 行。
- `docs/DHXY_CONTEXT.md`，1349 行。
- `docs/ACTIVE_WORK.md` 顶部 CR271 当前段落；顶部记录已将本 helper 限定为 TURN-28Q Repair #3 production typed-order preflight。
- `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节，行 1095-1776。
- `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`，383 行。
- `docs/业务逻辑.md`，1426 行。
- `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28Q.md`，658 行；物理 true EOF 是 Parent Review #6。
- `InputActionQueue.java`，870 行，SHA-256 `c53a423e98e7ba4d698caa937788e5c6654100971ed8c24a9daef645a7173b6a`。
- `InputActionWorker.java`，811 行，SHA-256 `225a9f3be56d18f0374f78c9b3bea7352e8b4db444d288ecf7f1d51511377f43`。
- `InputActionRequest.java`，1148 行，SHA-256 `7f4f8fdc4baf90f613fa6556f2a860849933724a6de5591d07d1eeb2d91ab0c8`。
- `InputActionFrozenExclusiveContractTest.java`，1283 行，SHA-256 `f72c7db076f7944555d3c89b7a8a1f3b1a2e6f396efe71c5ca00d801c07fd38c`。

TURN-28Q 原卡的物理末行是：

```text
<!-- TRUE_EOF: TURN-28Q PARENT-REVIEW-6 REPAIR-3-REQUIRED P0P1P2=0/2/0 EXTERNAL-A-FRESH-RESTART CLAIM-REQUIRED THREE-FILE-WRITESET 2026-07-16T11:03:03.155-04:00 -->
```

Parent Review #6 在原卡行 630-658 冻结了本次 Repair #3 的生产顺序、两组验收场景、限定 write set 和 legacy 不变要求。本报告只把该要求映射到当前源码。

## 3. 三种判定的语义边界

### 3.1 Frozen typed safety

`InputActionRequest.detectFrozenExactWindowFailure(String)` 位于 `InputActionRequest.java:927-955`，是 frozen request 的 typed safety 判定源：

1. `windowId` 不一致在行 932-936 投影为 window mismatch。
2. task stop 在行 938-942 投影为 `STOP_REQUESTED`。
3. identity suspension 或 identity epoch drift 在行 943-947 投影为 binding/identity typed failure。
4. exact window 字段或 native binding drift 在行 949-953 投影为 `WINDOW_BINDING_CHANGED`；字段比较落在 `sameExactWindow(...)`，行 964-976。

在本卡冻结的两个场景中，`windowId` 保持不变，因此该 detector 内的 STOP 顺序先于 identity/native drift。typed 入口分别是：

- `frozenExactWindowFailure(String)`，`InputActionRequest.java:465-480`。
- `checkDetailedSafety(String)`，`InputActionRequest.java:718-743`。
- `tryStartStep(...)` 内再次调用 detector，`InputActionRequest.java:790-823`。

`cancel(...)` 位于 `InputActionRequest.java:645-663`；在没有 step 开始时，结果构造位于行 1021-1040，终态保持 `NOT_STARTED`，并保留 typed safety reason。

### 3.2 Raw generation witness

`InputActionRequest.isFrozenExactWindowGenerationCurrent()` 位于 `InputActionRequest.java:428-463`。其 JavaDoc 在行 428-447 明确限定：

- 它是 pure generation witness，不负责选择 typed cancellation reason。
- caller 必须已经持有 `synchronized(windowContext)`。
- caller 必须先运行 typed safety。

实现只比较当前 context/binding 是否存在、native binding 对象身份、window id、identity suspension 和 epoch：

- binding 对象身份：行 455-456。
- window id：行 458。
- suspension 与 epoch：行 461-462。

因此 raw witness 可以证明“捕获的 generation 仍是同一代”，但不能抢在 typed detector 前把 STOP 改写成 binding drift。

### 3.3 Legacy comparator

`InputActionRequest.isPlayerIdentityEpochCurrent()` 位于 `InputActionRequest.java:390-395`，只比较 legacy identity epoch。worker 包装方法 `InputActionWorker.isPlayerIdentityEpochCurrent(...)` 位于 `InputActionWorker.java:575-588`，失败时直接记录 binding changed。它属于 legacy transaction 的既有 comparator，不是 frozen typed safety 的替代入口。

## 4. 当前两个 public queue entry 的顺序证据

### 4.1 Callback entry

方法 `InputActionQueue.submitFrozenExactWindowExclusiveAndWait(...)` 位于 `InputActionQueue.java:337-375`：

1. 行 345 捕获 task tokens，行 347 进入 window context monitor。
2. 行 348-355 先比较 `exactContext.getNativeBinding() != exactBinding`。命中时会创建一个 rejected request，并直接记录 `WINDOW_BINDING_CHANGED`。
3. 只有未命中上述 raw object-identity 分支时，行 356-358 才创建正常 frozen request。
4. 行 359-368 才调用 `request.frozenExactWindowFailure("before-enqueue")`。
5. 行 369-373 随后调用 pure witness `request.isFrozenExactWindowGenerationCurrent()`。
6. 行 375 才进入 `await(request)`。

这条入口当前存在两个 request-construction 分支，并且 raw binding-object witness 位于 typed safety 之前。

### 4.2 Typed action-list entry

方法 `InputActionQueue.submitFrozenExactWindowActionsAndWait(...)` 位于 `InputActionQueue.java:403-441`：

1. 行 408-410 固化 action list，行 411 捕获 task tokens，行 413 进入 context monitor。
2. 行 414-421 先运行同样的 raw binding-object identity shortcut，并创建 rejected request。
3. 行 422-424 才创建正常 frozen action-list request。
4. 行 425-434 才运行 `request.frozenExactWindowFailure("before-enqueue")`。
5. 行 435-439 随后运行 pure generation witness。
6. 行 441 才进入 `await(request)`。

这条入口与 callback entry 的关键顺序相同：raw witness shortcut 早于 typed safety。

### 4.3 Repair #3 冻结的 queue acceptance shape

未来父级验收应在两个 public entry 分别确认以下同构顺序：

1. 在既有 context monitor 内只构造一个 frozen request。
2. 先用该 request 运行 typed safety。
3. typed safety 已给出 reason 时，保留该 reason 并在 enqueue 前结束；不再运行 raw witness 来覆盖 reason。
4. typed safety 无 failure 时，才运行 pure generation witness。
5. 两道门均清晰时，才进入既有 `await(request)`。

这里的“一次构造”也意味着 A 与 A' 对象身份不同时，不再先创建一个专用 rejected request。一个 action id、一个 frozen request、一次既有 queue 路径仍是 HTTPS turn 与权威计划第 15 节的边界。

## 5. 当前 worker preamble 与 context-monitor 顺序证据

### 5.1 Shared worker preamble

`InputActionWorker.runLoop()` 在 `InputActionWorker.java:78-87` 调用 `queue.take()`；`handle(...)` 从行 94 开始。当前 preamble 顺序是：

1. pause/stop 等待：行 98-102。
2. 已取消检查：行 103-108。
3. legacy epoch comparator：行 109-112，调用 worker 的 `isPlayerIdentityEpochCurrent(request, "before-focus")`。
4. typed detailed safety：行 113-116，调用 `request.checkDetailedSafety("before-input-coordinator")`。
5. 行 120-121 之后才取得 input coordinator；frozen 分流在行 128-132。

因此 queued/taken request 同时出现 STOP 与 identity epoch drift 时，当前 shared preamble 先消费 legacy epoch drift，typed STOP 尚未获得首个判定位置。

Repair #3 的 frozen preamble 验收点是：frozen request 的 typed safety 必须先于任何 legacy epoch comparator；legacy request 的 preamble/order 不随之全局改写。

### 5.2 Frozen callback monitor/focus boundary

`InputActionWorker.runFrozenExactWindowExclusive(...)` 位于 `InputActionWorker.java:423-460`：

1. 行 424-428 在 monitor 外先运行 cancellation 与 `checkDetailedSafety("before-frozen-exclusive")`。
2. 行 436 进入 `synchronized(windowContext)`。
3. 行 437-440 在 monitor 内只运行 raw generation witness。
4. 行 442-445 紧接着执行 exact focus。
5. 行 446-458 才进入 callback 和 cleanup safety。

当前 monitor acquisition 后、exact focus 前缺少一次同一 monitor 内的 typed safety。monitor 外的行 424-428 不能覆盖“外层检查结束到 monitor 真正获得之间”的状态变化。

### 5.3 Frozen typed action-list monitor/focus boundary

`InputActionWorker.runFrozenExactWindowActions(...)` 位于 `InputActionWorker.java:490-551`：

1. 行 491-495 在 monitor 外先运行 cancellation 与 `checkDetailedSafety("before-frozen-actions")`。
2. 行 503 进入 `synchronized(windowContext)`。
3. 行 504-508 在 monitor 内只运行 raw generation witness。
4. 行 509-512 紧接着执行 exact focus。
5. 每个 action 之前，行 524-530 调用 `isFrozenExactWindowStillOwned(...)`；最终 gate 在行 548。

`isFrozenExactWindowStillOwned(...)` 位于 `InputActionWorker.java:553-573`，其现有顺序已经是 typed `checkDetailedSafety` 在前、pure witness 在后。未来 production 验收可确认两个 monitor/focus 边界使用该顺序或语义等价的内联顺序，并且这两步都发生在持有对应 context monitor 时、紧邻 exact focus 之前。

## 6. 冻结场景 A：pre-enqueue STOP + A-B-A'

### 6.1 确定性状态

- frozen snapshot 捕获 native binding 对象 A。
- context 先发布 B，再恢复为字段和值与 A 相同但对象身份不同的 A'。
- A 与 A' 的 window id、HWND、class、process 和 geometry 值一致；`A != A'`。
- identity epoch 保持不变。
- 在调用 public typed action-list queue entry 前，task stop 已关闭。
- action list 仍为 `CLICK_LEFT(150)` 后 `SLEEP(500)`，不改变已冻结输入语义。

### 6.2 Production 预期

- 单一 frozen request 首先运行 typed safety。
- detector 在 `InputActionRequest.java:938-942` 取得 `STOP_REQUESTED`；A/A' raw identity mismatch 不再先行改写 reason。
- 结果为非 completed、`NOT_STARTED`、`STOP_REQUESTED`，step/action index 保持未开始值，reason 保留 task-stop 语义。

### 6.3 零副作用证据链

- queue entry 在 `InputActionQueue.java:375` 或行 441 进入 `await(...)` 之前结束。
- 实际 queue offer 位于 `InputActionQueue.await(...)` 的行 721-852 内，offer 起点为行 725。
- `InputActionQueue.take()` 位于行 854-855。因此 enqueue 前 typed return 对应 take count 为 0。
- worker 未 take 到 request，故 exact focus、callback/action execution 和真实 input 均为 0。
- 两个 frozen public entry 均不调用 `refreshAndValidateNativeBinding(...)`；该 legacy/detail refresh helper 位于 `InputActionQueue.java:679-718`。因此 refresh count 为 0。

验收计数：`take=0, focus=0, input=0, refresh=0`。

## 7. 冻结场景 B：queued/taken STOP + identity/generation drift

### 7.1 确定性状态

- public typed action-list entry 用 binding A 与 identity epoch N 构造一个 frozen request。
- request 经真实 queue 被 `super.take()` 取出后，在交给 worker `handle(...)` 前由一次性 latch gate 暂停。
- gate 已确认真实 take 后，先关闭 task stop，再向同一 window id 发布 identity B，使 epoch 变为 N+1，并产生新的 native binding generation/object。
- 最后释放同一个已 taken request 给 worker；不新建第二个 request，也不 retry/replay。

### 7.2 Production 预期

- take count 精确为 1，并且 worker 收到的 request id 与 gate 捕获的 request id 相同。
- frozen worker preamble 先运行 typed safety；在 window id 不变的前提下，`InputActionRequest.java:938-942` 的 STOP 先于行 943-953 的 identity/generation drift。
- 结果为非 completed、`NOT_STARTED`、`STOP_REQUESTED`，step/action index 保持未开始值，reason 保留 task-stop 语义。
- typed gate 在 `InputActionWorker.java:120-132` 的 coordinator/frozen dispatch 之前结束处理，因此 exact focus 和真实 input 均为 0。
- frozen path 不走 mutable binding refresh，因此 refresh count 为 0。

验收计数：`take=1, focus=0, input=0, refresh=0`。

### 7.3 确定性测试机制约束

当前测试 harness 的真实 `CountingQueue` 位于 `InputActionFrozenExclusiveContractTest.java:1014-1029`，真实 worker/sequences 组装位于行 921-964。未来测试应在 test-private `CountingQueue.take()` 中增加一次性 gate：

1. 先调用 `super.take()` 得到真实 request。
2. 记录 taken request 和计数。
3. 命中目标 description 后，保存该 request，count down `taken` latch，并 await `releaseToWorker` latch。
4. release 后返回同一个 request 给 worker。
5. gate 必须在 waiter 启动前 arm，并在 `finally` 中保证 release/clear。

新场景不得沿用当前 `waitUntilQueued(...)` 的 polling sleep；该 helper 位于测试行 904-910。也不得依赖 `Unsafe`、阻塞占位 request、额外 submit、重试或 timing guess。

## 8. Legacy request 不变边界

Repair #3 的 production 顺序调整只围绕 frozen request。未来父级 diff 验收应明确保护以下边界：

- `InputActionQueue` 的普通 `submitAndWait`，行 67-80。
- detailed/remote submission 与 refresh safety，行 99-263。
- 普通 `submit`，行 278-291。
- legacy exclusive submission，行 303-316。
- retained-session 路径，行 448-630。
- refresh helper，行 679-718。
- `await(...)` 的既有 offer、waiter cancellation、timeout 与 terminal 语义，行 721-852。
- `InputActionWorker` 普通 focus/callback/action transaction，行 133-200。
- retained-session worker transaction，行 228-313。
- legacy `isPlayerIdentityEpochCurrent(...)` comparator 自身，行 575-588。
- frozen Alt exact-binding/fallback 已有行为，行 594-655；不把本修复扩展为 keyboard/focus 重写。
- `InputActionRequest.java` 保持 read-only；typed detector、pure witness、request result model 与 frozen factory 不在本 repair write set。
- `InputSequences.java`、focus service、keyboard service、callers 和业务 task 保持 read-only。
- `CLICK_LEFT(150) -> SLEEP(500)`、单 action id、单次 queue submission、无自动 retry/replay、无 session/ledger/TTL/durable workflow 保持不变。
- `docs/业务逻辑.md` 的确认基线保持不变；本 repair 不改变任务阶段、OCR/template/click/navigation/fallback 决策。

实现隔离点是“仅 frozen request 获得 typed-first 顺序”。若把 worker shared preamble 对所有 request 全局重排，legacy comparator 的既有先后关系就不再属于本卡限定范围。

## 9. 当前测试覆盖定位与未来补充点

`InputActionFrozenExclusiveContractTest` 当前有 19 个 `@Test` 场景，相关已有证据包括：

- public typed action-list 单 request 路径：`frozenActionListTravelsAsOneTakenRequestThroughTheInputSequencesFacade()`，行 361-402。
- stop-only enqueue 前关闭：`stopClosedBeforeEnqueueTerminatesTypedAndNeverEntersTheQueue()`，行 736-770。
- action-list 中途/final stop：行 780-856。
- A-B-A' 单独 drift：`valueEqualRebindRejectsTheFrozenActionListBeforeEnqueueWithTypedBindingChange()`，行 865-902。
- 真实 harness、queue、focus/input/refresh recorder：行 921-1121 与 1243-1280。

未来补充范围限定为同一个命名测试类中的两例：

1. `pre-enqueue STOP + A-B-A'` 的组合优先级与四项零计数。
2. `queued/taken STOP + identity/generation drift` 的组合优先级、一次真实 take 与三项零计数。

两例都应从 `InputSequences.submitFrozenExactWindowActionsAndWait(...)` 进入 public typed action-list facade。callback public entry 的相同 production 顺序由父级 source inspection 覆盖，不需要扩成第二套 callback test matrix。补充后预期测试数为 21。

## 10. 未来父级验收清单

- [ ] 当前 TURN-28Q 原卡仍以 Parent Review #6 true EOF 为本轮范围来源。
- [ ] production diff 只改 `InputActionQueue.java` 与 `InputActionWorker.java`；contract diff 只改 `InputActionFrozenExclusiveContractTest.java`，另由实现者按流程追加原卡证据。
- [ ] `InputActionRequest.java` SHA 与本报告快照一致，或父级明确解释任何非本卡变动来源；本 repair 不应修改该文件。
- [ ] callback public queue entry 在同一 monitor 内只构造一个 frozen request，typed safety 在 pure witness 前。
- [ ] typed action-list public queue entry 在同一 monitor 内只构造一个 frozen request，typed safety 在 pure witness 前。
- [ ] 两个 queue entry 都不存在 raw native-binding object shortcut 抢在 typed safety 前的路径。
- [ ] frozen worker preamble 不再让 legacy epoch comparator 抢在 typed safety 前；legacy request 顺序保持原样。
- [ ] `runFrozenExactWindowExclusive(...)` 获得 context monitor 后，按 typed safety、pure witness、exact focus 的顺序相邻执行。
- [ ] `runFrozenExactWindowActions(...)` 获得 context monitor 后，按 typed safety、pure witness、exact focus 的顺序相邻执行。
- [ ] pure witness 只在持有对应 window context monitor 时调用。
- [ ] pre-enqueue 组合场景得到 `NOT_STARTED/STOP_REQUESTED`，并证明 `take/focus/input/refresh = 0/0/0/0`。
- [ ] queued/taken 组合场景得到 `NOT_STARTED/STOP_REQUESTED`，并证明 `take/focus/input/refresh = 1/0/0/0`。
- [ ] queued/taken case 使用一次性 latch/event gate，捕获并释放同一个真实 request；没有 polling sleep、Unsafe、占位 request、额外 submit、retry 或 replay。
- [ ] 两个新增 case 仍使用完整 `CLICK_LEFT(150) -> SLEEP(500)` action list。
- [ ] 测试类总数从当前 19 增至预期 21，且没有复制 callback matrix。
- [ ] 父级核对 legacy queue、worker、retained、Alt、refresh、await 和业务路径均无语义漂移。
- [ ] 实现者提供当前代码上的 `mvn -q -Dtest=InputActionFrozenExclusiveContractTest test` 与 `mvn -q -DskipTests compile` 新鲜证据；本 helper 未执行这些命令。
- [ ] 父级核对最终 write set、原卡追加位置和 physical true EOF，避免覆盖 Parent Review #6 或其它并发材料。

TRUE_EOF PRECHECK_COMPLETE
