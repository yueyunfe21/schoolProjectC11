# CR271 TURN-22 Repair #3 下一 implementation slice 预检证据

记录时间：`2026-07-16T09:19:00-04:00`

## 1. 角色与结论边界

- 本文只向 CR271 父级提供 internal readiness helper 的只读证据，不是 implementation delivery、code review、父级批准或卡片状态判定。
- 本 helper 未修改 Java、原卡、权威计划、`docs/ACTIVE_WORK.md`、CR271、矩阵或 dashboard；未运行 Maven、JUnit、compile、package、runtime、application、server、Task、UI、capture 或 input；未执行 Git mutation。
- helper 读取期间，父级已于 `09:09:13` 建立同一最小切片 `TURN-22D1`，并于 `09:15:59` 由 External A 在子卡 physical EOF 领取。因此不得再创建一个重叠的 C2/D2 writer；以下证据供父级核对现有 D1 的冻结合同与最终门，不构成对 D1 当前 WIP 的审查。

## 2. 已完整读取与对账的权威材料

- `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271。
- `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14 至 19 节。
- `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`。
- `docs/业务逻辑.md`，重点核对通用热启动队伍回归顺序、已验证回城快照、五倍/修罗归队路径及 STOP 非业务失败规则。
- TURN-22 原卡、TURN-22C1 子卡、TURN-28P、TURN-28Q 当前卡及 helper 读取期间出现的 TURN-22D1 子卡。
- DHXY 当前 `TurnExecutionWindow`、`TurnInputStepExecutor`、`TurnInputStepExecutorContractTest`、`TurnInputActionMapper`、`InputSequences`、`InputActionQueue`、`InputActionRequest`、`InputActionWorker`、`InputActionExecutionResult`、`InputActionSafetyReason`、`WindowTaskContextHolder`、`WindowRuntimeContext`、`WindowNativeBinding` 及相邻真实 queue/worker contract harness。
- Cloud 当前 `TeamReturnService`、`CloudTeamReturnPortAssembly`、`TeamReturnTurnContractTest` 与双端 turn protocol。
- `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的 `TeamReturnService.java` 和 AutoBattle/五倍/修罗调用位置。

## 3. 只读时间截面

- TURN-22C1 卡在 `09:13:36` 记录父级 source/test-source 复核 `P0/P1/P2=0/0/0`；Cloud named test 当前交付快照为 1612 行，SHA-256 `d270d7dcacb73bc66b50af7be9d2dbc3f53098587f430fb6ebdcde7f66e07fab`。其独立审查与构建仍属于后续总门。
- TURN-28P 卡在 `08:42:21` 记录父级 source/test-source 复核 `P0/P1/P2=0/0/0`；两份真实 queue/worker contract test 已交付，独立审查与构建仍属于后续总门。
- TURN-28Q 卡在 `09:09:13` 记录 Repair #1 父级 source/test-source 复核 `P0/P1/P2=0/0/0`。冻结 action-list public API 的五文件 SHA 为：
  - `InputSequences.java`: `b293e0c6...`
  - `InputActionQueue.java`: `66fa536e...`
  - `InputActionRequest.java`: `23973b7e...`
  - `InputActionWorker.java`: `7489084b...`
  - `InputActionFrozenExclusiveContractTest.java`: `475399ef...`
- TURN-28Q 的独立审查与 build 尚属于后续总门，但其父级已明确把 source-start 门与最终门分开；D1 源码落盘无需等待 TURN-28Q 的最终卡片结论，前提是上述 API 签名和字节继续冻结且没有 owner 冲突。
- TURN-22D1 卡冻结的领取前快照为：
  - `TurnInputStepExecutor.java`: 229 行，SHA-256 `0ee95cbd48d3ec76fb9e50385108f9898f2979a33966487b39065352af1f43fd`
  - `TurnInputStepExecutorContractTest.java`: 394 行，SHA-256 `bb1ccc432020a8acd61c82abe207e13fb7959d94e9f8f6f27db28b43dafb738d`
- helper 最后只读时，两份 D1 文件已由其唯一 implementation owner 开始增量修改；本文不把中间 SHA 当作 delivery，也不评价中间字节。

## 4. 下一真实 implementation slice

下一切片应复用父级已实例化的 `TURN-22D1 - DHXY frozen TeamReturn executor integration`。这是 TURN-22C1 之后最小、可独立施工、与 C1/28P/28Q 写集互斥的闭环：生产切流与拥有该切流的具名契约测试必须在同一 slice，不能再拆成一个故意红的 test-only slice或一个缺少验收证据的 production-only slice。

### Exact modify set

1. DHXY `src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java`
2. DHXY `src/test/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutorContractTest.java`
3. append-only 子卡 `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-22D1.md`

除上述三项外不得扩写。TURN-22 原卡不由 D1 worker 追加；聚合记录由父级在 D1 与 C1 都交付后处理。

### Exact read-only set

DHXY 只读：

- `src/main/java/com/bot/dhxy/input/InputSequences.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionRequest.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionExecutionResult.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionSafetyReason.java`
- `src/main/java/com/bot/dhxy/cloud/turn/TurnExecutionWindow.java`
- `src/main/java/com/bot/dhxy/cloud/turn/TurnInputActionMapper.java`
- `src/main/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutor.java`
- `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`
- `src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java`
- `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowTaskContextHolder.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/model/WindowNativeBinding.java`
- turn protocol、mapper、resolver、POM、Task、Service、caller 与其余 DHXY 源码/测试全部只读。

Cloud 只读：

- `src/main/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnService.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTeamReturnPortAssembly.java`
- `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java`
- Cloud protocol、golden fixture、模板资源、POM 及其余 Cloud 源码/测试全部只读。

文档与过程只读：TURN-22、TURN-22C1、TURN-28P、TURN-28Q 原卡，权威计划、HTTPS turn 协议、`docs/业务逻辑.md`、`docs/ACTIVE_WORK.md`、CR271、矩阵和 dashboard。两仓既有 dirty/untracked 字节全部保护。

## 5. 输入合同

1. `TurnExecutionWindow.resolveForAction(...)` 已经完成本 action 唯一一次注册窗口解析与 native binding refresh，并提供同一份：
   - `WindowRuntimeContext` 对象；
   - `WindowNativeBinding` 对象身份；
   - `windowId`、HWND、processId；
   - screen-absolute `windowRect(left, top, width, height)`；
   - action resolver 当时的 player identity generation。
2. `TurnInputStepExecutor` 只消费该 immutable window snapshot，不得 title-search、refresh、re-resolve 或构造第二份 binding comparator。
3. 鼠标输入由现有 `TurnInputActionMapper.mapMouse(...)` 生成完整 ordered `List<InputAction>`；坐标保持 screen-absolute，不缩放。
4. TeamReturn 的 Cloud 输入是一个 `CLICK_LEFT` step，`TurnInputSpec.clickDelayMs=150`、`queueHoldMs=500`。mapper 的本地机械列表必须精确为：
   - `InputAction.clickLeft(x, y, 150)`
   - `InputAction.sleep(500)`
5. D1 保持现有 constructor/public surface。当前生产构造器接收 `InputActionQueue`，且另有 `LocalTurnActionExecutorContractTest` 两个只读构造点；改为注入 `InputSequences` 会扩大调用方和测试写集，因此本 slice 应直接调用已复核的 public `InputActionQueue.submitFrozenExactWindowActionsAndWait(...)`，不新增适配层。

## 6. 输出合同

1. 对每个 mouse step 或 closed mouse/WAIT sequence，执行器只调用一次 frozen exact-window action-list public boundary，并一次传入完整 immutable action list、exact context 与 exact binding。
2. queue 在 `synchronized(exactContext)` 内要求 `exactContext.getNativeBinding() == exactBinding`，在同一 monitor 内读取 `playerIdentityEpoch`，将完整 list `List.copyOf(...)` 后放入一个 request。worker 在一个 global input transaction、一次 frozen focus、同一 generation monitor 内执行全部 action。
3. `InputActionExecutionResult.Status.COMPLETED` 映射为 `TurnInputStepExecutor.Result(COMPLETED, OK)`。
4. `InputActionSafetyReason.STOP_REQUESTED` 映射为 `Result(STOPPED, STOPPED)`；不得依靠 reason 字符串猜 STOP。
5. `WINDOW_BINDING_CHANGED`、`TASK_RUN_MISMATCH`、cancel、partial、started-unknown 或其他未完整完成结果映射为 `FAILED/INPUT_QUEUE_FAILED`；不得伪造 success，不得本地 retry/replay。
6. 等待线程自身被中断时继续保持现有 STOPPED 投影；background keyboard、WAIT、七种 mouse mapping 和既有 result enum/code 不变。
7. 调用线程原先已有的 sentinel `WindowTaskContextHolder.rawCurrent()` 必须在返回后恢复为同一对象；worker 线程在 provider/focus 内只能看到 execution window 的 exact context。
8. `OK` 只代表机械点击完整执行，不代表归队业务目标已经成功；后续业务判断仍归 Cloud/原 TeamReturn 流程所有。

## 7. `696a12b0` baseline 行为

基线 `TeamReturnService.java:65-91` 的成员归队合同必须逐项保持：

1. 先找一次归队按钮；未找到返回 false。
2. 找到后先执行 `ensureSheYaoXiangActive(context)`。
3. 补香检查后再次找归队按钮；消失则返回 false。
4. 对第二次点位做 `getRandomizedPoint(..., 3, 3)`。
5. 一次 `inputSequences.submitAndWait(...)` 提交完整 `[CLICK_LEFT(delay=150), SLEEP(500)]`。
6. 随后记录 clicked timestamp 并返回 true。

同时保持：

- leader wait timeout `120_000ms` 与 poll `3_000ms`；
- leader 只等待信号，不点击成员按钮；
- precheck 是一次 immutable screenshot 的后台分析，失败/未完成/过期时回落原 live detector；
- `docs/业务逻辑.md:1125-1140` 的固定顺序：战斗中、当前任务 dialog、队伍回归信号、tracker、回程道具、保存上下文、接任务入口；
- `docs/业务逻辑.md:243-254` 的已验证回城快照不得因归队等待、云端命令或排队而过期或被清除；
- `docs/业务逻辑.md:1288` 的 `WAIT_TEAM_RETURN` 仍是信号在则等待、信号消失才按来源继续，poll 3000ms；
- STOP/暂停中断不是业务 FAILED。

本 slice 只替换 DHXY 鼠标 mechanical submission ownership，不修改任何观察、OCR、模板、补香、随机点、phase、fallback、retry、等待、快照 expiry 或 caller 顺序。无已批准业务差异；按基线等价迁移。

## 8. 具名测试证据合同

唯一扩展测试族是现有 `TurnInputStepExecutorContractTest`，不得另造重复 test suite。建议证据分层如下：

1. 使用 production `TurnInputStepExecutor`，并由 test-private queue subclass 覆盖 public frozen action-list 方法后立即委托 `super`。它只记录调用参数和真实返回结果，不手工构造 `InputActionExecutionResult`。
2. 成功路径使用真实 in-memory `InputActionQueue -> InputActionWorker -> WindowAwareInputCoordinator`，配 fake focus/provider；不得触真实桌面输入。
3. execution fixture 必须让 `WindowRuntimeContext` 当前发布的 binding 与 `TurnExecutionWindow.binding()` 是同一个对象。新证据优先走 public `TurnExecutionWindow.resolveForAction(...)`；不得为新证据增加 private production reflection、`Unsafe` 或 production hook。
4. 调用线程先绑定一个与 execution context 不同的 sentinel context。提交边界记录 exact context/binding/windowId/HWND/process/rect/player epoch；fake focus/provider 内再断言 worker holder 看到 execution context；返回后 `assertSame(sentinel, holder.rawCurrent().orElseThrow())`。
5. TeamReturn timing case 断言 frozen public method 调用恰好一次，完整 list 恰为 `CLICK_LEFT(x,y,delay=150)` 后接 `SLEEP(500)`，且结果为 `COMPLETED/OK`。
6. D1 只证明 executor 调用一次 frozen boundary。一个 queue request 被 worker `take()` 一次、list immutable、一次 focus 与 progress `0..1` 的 generic mechanics 证据由 TURN-28Q 的同包 contract test 所有；D1 不复制 package-private `take()` 证明或并发/pause/cancellation suite。
7. A -> B -> A：保留 resolver 产生的原 binding A，context 依次发布 B 与字段值等于 A 但对象不同的 A'，再调用 production executor。断言执行器结果为 `FAILED/INPUT_QUEUE_FAILED`；委托后的真实 queue result 为 `NOT_STARTED/WINDOW_BINDING_CHANGED`；provider input、focus 和 retry 均为 0。
8. typed STOP：通过真实 task pause/stop token 让 frozen boundary 返回 `STOP_REQUESTED`，断言 executor 投影为 `STOPPED/STOPPED`，且不以线程 interrupt 或 reason substring 冒充 typed STOP。
9. 保留已有七种 mouse mapping、keyboard-only HWND path、unsupported keyboard typed failure、positive/interrupted WAIT 等非回归断言。
10. 禁止 source scan、Java/Markdown 文本计数、wall-clock sleep 猜竞态、manual fake terminal result、真实 input/capture/focus，以及对 TURN-28Q generic proof的重复实现。

后续允许执行时的最小命令证据：

- DHXY D1：`mvn -q -Dtest=TurnInputStepExecutorContractTest test`
- TURN-28Q：`mvn -q -Dtest=InputActionFrozenExclusiveContractTest test`
- TURN-28P：`mvn -q "-Dtest=TurnCapturePixelChangeProbeContractTest,LocalTurnActionExecutorContractTest" test`
- DHXY compile gate：`mvn -q -DskipTests compile`
- Cloud C1：在 `dhxy-cloud-brain` 执行 `mvn -q -Dtest=TeamReturnTurnContractTest test`，并完成其适用 compile/package gate。

本 helper 未执行上述命令。具名测试属于权威计划明确授权的 test family；实际运行必须等 Java writers 稳定，并由父级按总门统一安排。

## 9. 最终依赖门

### 源码落盘门

- 只依赖 TURN-28Q 已复核的 public frozen action-list API 签名继续冻结、D1 三项写集无第二 owner，以及当前字节增量保护。
- 不依赖 TURN-28Q 的最终独立审查/构建结论才开始 D1 源码；父级已按 source-start 与 final-gate 分层并建立 D1。

### TURN-22 Repair #3 父级聚合门

- TURN-22C1 Cloud cleanup 已交付并完成父级 source/test-source 复核。
- TURN-22D1 必须完成 executor + named test 同一 delivery，父级按本报告的输入/输出/baseline/test 合同重读当前最终字节。
- 父级只在 C1 与 D1 都有最新 delivery 后聚合 TURN-22 Repair #3；不得用某一切片的中间 WIP 代替另一切片。

### 最终审查与构建门

- TURN-22C1、TURN-22D1、TURN-28P、TURN-28Q 各自所需独立 reviewer 结论无未解决 P0/P1/P2。
- 上述具名测试全部针对当前最终源码成功执行。
- DHXY Java compile gate成功；Cloud TeamReturn test 与 Cloud 适用 compile/package gate成功。
- 双端 protocol/golden/resource parity 保持，Cloud 一 command/一 UUID/无 frame/terminal uncertainty fail-closed/零 transport retry 仍成立。
- fresh runtime 证据由后续集成/运行验收卡独立记录，不用 runtime 代替本 slice 的源码、具名测试或 compile 门。

## 10. 禁止项

- 不修改 TURN-28P/28Q production/test、Cloud C1 文件、assembly、mapper、protocol、POM、Service、Task、caller 或原 TURN-22 卡。
- 不改变 `TurnInputStepExecutor` constructor/public surface，不为引入 `InputSequences` 扩写只读调用方。
- 不走 legacy `submitAndWait`，不 refresh/title-search/re-resolve，不新增 comparator、binding epoch 参数或本地 generation 算法。
- 不把 action list 塞入 exclusive callback；不在 input worker callback 内嵌套 queue submission；不直接复制 worker dispatcher。
- 不拆分 click 与 hold，不增加 MOVE/no-op input/第二 request/第二 command/自动 retry/replay。
- 不新增 wrapper nesting、session、owner、ledger、TTL、durable workflow、OCR/模板/业务 decision 或 fallback。
- 不用 empty-to-empty context assertion充当恢复证据；不使用 private reflection、`Unsafe`、source guard、sleep race proof 或 manual fake result冒充真实 queue/worker。
- 不运行真实 desktop input、runtime、application 或 server；不执行 Git mutation；不覆盖、回滚、格式化或清理任何既有 dirty/untracked 字节。

<!-- TRUE_EOF PRECHECK_COMPLETE -->
