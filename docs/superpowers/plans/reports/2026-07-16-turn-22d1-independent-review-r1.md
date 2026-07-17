# CR271 TURN-22D1 Repair #1 独立 Delivery Review R1

## 结论

**APPROVED**

- `P0/P1/P2 = 0/0/0`。
- 本结论仅是 TURN-22D1 Repair #1 的独立 production/test-source review；不是实现者自审，不替代 named
  test、DHXY compile、TURN-22C1 或 TURN-22 父卡聚合门。
- 没有待返修条件。若后续另立“整个 test source tree 禁止 `Unsafe`”政策，应作为独立公共 fixture/POM/
  production seam 决策处理；该政策不在本卡冻结合同内。

## 审查边界与证据

已完整读取并按当前磁盘字节复核：`AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部
CR271、权威计划第 14-19 节、HTTPS turn 协议、`docs/业务逻辑.md`、TURN-22D1 原卡至物理 EOF，及：

- `src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java`，264 行，SHA-256
  `a64422b061021dcbcec51837ac9f68a59bf21d57984469703cd1a68c2963134e`；
- `src/test/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutorContractTest.java`，695 行，SHA-256
  `f5a7992fc6566f00b56f4e7e21c8e66fcf328f519523e73d6858ae93042e7a81`；
- `TurnExecutionWindow.java`、`InputActionQueue.java`、`InputActionRequest.java`、
  `InputActionWorker.java`、`TurnInputActionMapper.java`、`WindowTaskContextHolder.java`、public resolver 所需
  manager/runner/refresh seam，以及 frozen action-list 的公用 contract source；
- git `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的
  `src/main/java/com/bot/dhxy/service/TeamReturnService.java`。

未运行 Maven/JUnit/compile/package，也未启动 runtime/application/server/Task/UI/capture/input；未做任何
Git mutation。当前 verdict 因而是源码与测试断言审查通过，运行门仍待父级安排。

## 逐项裁决

### 1. Public resolver fixture 与 reflection

**通过。** `TurnInputStepExecutorContractTest.java:478-495` 直接调用 production public
`TurnExecutionWindow.resolveForAction(...)`；`498-515` 的 scripted refresh 把 exact binding commit 到
context，`518-563` 的 test manager/runner 只提供 resolver 所需的 public lookup/accessor。真实 resolver 的
runner lookup、windowId 对账、refresh/commit、live HWND/geometry 校验及 stop metadata 推导仍由
`TurnExecutionWindow.java:51-87,106-112` 执行。

原 P1 指向的 private-production reflection 已归零：当前 named test 没有
`TurnExecutionWindow.class`、`getDeclaredConstructor(...)`，也没有读取/写入任何 production private field。

测试仍在 `TurnInputStepExecutorContractTest.java:26-28,44,565-580` 通过 JDK `theUnsafe` 字段分配本测试文件
声明的 `BareWindowTaskRunner`。这意味着全文件的 `setAccessible(true)` 并非字面零，但它只访问 JDK
`Unsafe`，分配目标是 test-owned subclass；该 subclass 仅覆盖 resolver 实际调用的 public
`getWindowContext()` / `getCurrentTask()`。它没有构造 `TurnExecutionWindow`、没有注入 metadata、没有跳过
public resolver，也没有替代 frozen queue 结果。因此该 residual 不造成假证明，不构成 P0/P1/P2。

### 2. Sentinel restore 与 exact identity

**通过。** `TurnInputStepExecutorContractTest.java:295-343` 预装与 action window 不同的
`window-sentinel`/HWND `99999`/pid `4242`/rect `900,800,20,30` context，executor 返回后以 `assertSame`
核对 sentinel context 与 binding，并核对 windowId/player epoch；外层 scope 结束后 holder 为空。
`TurnInputStepExecutor.java:185-188` 的嵌套 `callWith` 与
`WindowTaskContextHolder.java:106-117` 的 `finally` 恢复路径对应这些断言。

同一用例在 submission 内核对 `window-7`、HWND `12345`、pid `88`、rect
`137,241,10,10` 与 player epoch，并在 `TurnInputStepExecutorContractTest.java:320-335` 对 resolver 产出的
context/binding 使用 exact object identity。production queue 在 `InputActionQueue.java:393-421` 又以当前
binding object 为 generation witness，`InputActionRequest.java:440-450` 保留 A-B-A 可见的 identity gate；没有
title search、第二次 refresh、re-resolve 或 value-only comparator。

### 3. 一次 `CLICK_LEFT(150) -> SLEEP(500)`

**通过。** `696a12b0` 的 `TeamReturnService.java:65-91` 在成员归队按钮二次确认和香检查后只提交一次
`CLICK_LEFT(delay=150)`，随后同 request `SLEEP(500)`。当前 mapper 在
`TurnInputActionMapper.java:39-47` 把 `clickDelayMs=150` 与 `queueHoldMs=500` 映射为该两项 immutable list；
executor 在 `TurnInputStepExecutor.java:182-202` 只调用一次 frozen action-list boundary。

`TurnInputStepExecutorContractTest.java:295-343` 断言 frozen submission 数为 1、完整 action type 顺序精确为
`[CLICK_LEFT, SLEEP]`、delay 精确为 `150/500`。没有第二 command、拆分 submit 或自动重放。

### 4. Typed STOP

**通过。** `TurnInputStepExecutor.java:189-202` 仅把 completed 映射为 `COMPLETED/OK`；typed
`InputActionSafetyReason.STOP_REQUESTED` 映射为 `STOPPED/STOPPED`；drift/cancel/failed/uncertain 及其它未完成
状态统一保持 typed failure。`TurnInputStepExecutorContractTest.java:351-367` 在 caller thread 未 interrupt 的
条件下证明 queue typed STOP，不依赖旧的 thread-interrupt 猜测，并断言 submission 恰好一次、零 retry/replay。
resolver-derived pre-stop 另由 `478-494` 设置真实 `WindowRuntimeStatus.STOPPING` 后生成 metadata，未手造 stop
boolean。

### 5. A-B-A drift 与零 enqueue/input

**通过。** D1 integration case `TurnInputStepExecutorContractTest.java:376-415` 先形成字段完全相同但对象不同的
`A -> B -> A'` binding，再经真实 `TurnInputStepExecutor` 和真实 `InputActionQueue` 得到
`FAILED/INPUT_QUEUE_FAILED`，最终 queue size 为 0，holder 也恢复为空。

零 enqueue 的公用边界证据没有被 D1 用 fake 重写：`InputActionQueue.java:403-419` 在 context monitor 内发现
binding object 不同即构造 typed rejected result并直接返回，尚未进入 `await(request)`；公用 frozen action-list
contract `InputActionFrozenExclusiveContractTest.java:518-554` 进一步断言 `NOT_STARTED`、
`WINDOW_BINDING_CHANGED`、worker taken count 为 0、provider/focus/refresh 调用均为 0。D1 卡按冻结约束只证明
executor 已接入该真实边界，没有重复 TURN-28Q 的 generic worker/concurrency harness；这不是不可失败的 stub 证明。

### 6. 禁止新增语义

**通过。** 两个交付文件未新增业务 retry、transport replay、session、owner、ledger、TTL、durable workflow、OCR
或 TeamReturn 业务决策。production 只搬动 queue ownership/typed terminal mapping；一次 UUID/action 的上层语义
未变。无已批准业务差异；按 `696a12b0` 的一次 queue
`CLICK_LEFT(150) -> SLEEP(500)` 等价迁移。

## 返修条件

无。若 named test 或适用 compile 后续失败，应以真实首错重新打开本卡；本次未运行这些门，不能把本报告写成
`CARD APPROVED` 或运行验收通过。

<!-- TRUE_EOF: TURN-22D1 REPAIR-1 INDEPENDENT-DELIVERY-REVIEW-R1 APPROVED P0P1P2=0/0/0 SOURCE-AND-TEST-SOURCE-ONLY TEST-COMPILE-NOT-RUN 2026-07-16 -->
