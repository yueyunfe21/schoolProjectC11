# CR277 五环后台准备、流式候选与停滞恢复

## 状态

`IN PROGRESS / P1 / Parent review required`

## 运行事故

- 2026-07-26 10:29 fresh run 中，四个五环窗口的 observation request 持续被 Cloud 以
  `INVALID_OBSERVATION_REQUEST: interest.roiWidth must be within (0, 640]` 拒绝。
- Cloud `CloudWholeTaskObserver.syncNpcPreparedInterest(...)` 将整窗 `1024x768` 错误注册为
  普通小 ROI；Client 保留状态重试，但请求永远无法通过协议校验。
- `FiveRingTaskV2.clickInitialNpcForAccept(...)` 在 `NpcPreparedClickPlanState` 中等待完整 plan 和
  `NPC_CLICK_PLAN_READY`，没有 transport-failure terminal，因此形成永久 parking。

## 用户冻结合同

1. 五环是多窗口后台准备任务。Tracker、Dialog、NPC 三类视觉计算都必须在窗口取得 task turn
   之前完成；窗口取得 turn 后只能校验并立即消费现成动作，禁止再截图、OCR、模板匹配或等待 Cloud。
2. NPC 使用现有 `NpcClickSmartQueueStore` 流式 FIFO。`MEMORY -> TOOLTIP -> YELLOW ->
   PURPLE -> CTRL` 每完成一项立即入队；首个可执行候选或最终 `END/FAILED` 发布 Ready Event，
   不等待完整 candidate plan。
3. Tracker 与 Dialog 复用现有 `CloudDialogPreparedActionState`；后台 action ready 后发布
   `PREPARED_ACTION_READY`，五环 phase 只消费 exact action。
4. 所有 prepared/ready 状态按 exact tenant/device/window/hwnd/taskRun/demand 或 generation
   隔离。旧 run、旧帧、旧 event、绑定漂移和过期 action 零输入 fail-closed。
5. 普通 observation ROI 上限 `640x640` 不放宽。Runner 到达场景复用 exact
   `1024x768` terminal frame；无 pathing 的 nearby 场景使用专门的一次性整帧 demand，
   不伪造 pathing、不拆成跨时刻 tiles。
6. Runner 现有 `2.2s STOPPED_AWAY` 首次终态保留。从首次停稳起 `10s` 后，若相同
   intent/generation 仍未 clear/replace 且坐标未变化，恰一次重发同一终态以重新触发既有
   `PATHING_TERMINAL` 唤醒；movement/clear/replace/reset/pause/stop 清 watchdog。
7. Cloud 恢复到“产生该 pathing intent 的当前步骤入口”，不得笼统退回上一 phase：
   accept-NPC navigation 重试同目标导航；tracker pathing 回 `SYNC_TASK_PANEL`；dialog route
   回对应 Dialog/route prepare；普通坐标导航重试当前导航。既有有界重试耗尽后才走任务失败策略。

## 禁止项

- 禁止扩大普通 ROI 上限来容纳整窗。
- 禁止获得 task turn 后现场计算。
- 禁止等待完整 NPC plan 才唤醒。
- 禁止新增第二套 Tracker/Dialog/NPC 识别算法、模板、阈值或点击公式。
- 禁止永久 park、无界重试、跨窗口 prepared 状态或重复物理输入。
- 禁止改写只读基线 `D:\mavenProject\DHXY`。

## 验收

- 五个窗口可独立后台准备；任一窗口 Ready 后通过 Event Bus 排队，获得 turn 后立即提交现成输入。
- 不再出现 `wuhuan-accept-npc` 的 `1024x768 ObservationInterest` 或相关 HTTP 400。
- NPC 首候选即时唤醒，后续候选继续流式入队；全 miss 也以终态唤醒恢复。
- Tracker/Dialog production phase 无 task-turn 内截图/OCR/模板计算。
- STOPPED_AWAY 首次事件与 10 秒未确认重发均 exact-once，恢复分支按 intent source。
- focused Client/Cloud named tests 与双仓 compile 通过；fresh runtime 另行验收五窗隔离和实际时延。

<!-- TRUE_EOF: CR277 IN-PROGRESS USER-CONTRACT-FROZEN 2026-07-26 -->

## 2026-07-26 实现检查点（父级未验收）

- 已落地双仓 `ObservationPreparedFrameDemand/ObservationPreparedFrame` wire：
  普通 ROI 仍限制 `<=640`，一次性整窗 carrier 严格限定 `1024x768 PNG`。
- Client `WindowObservationRunner` 已支持 exact demand 捕获、失败保留及重发；未修改
  `WindowObservationSampler`，也未覆盖 `CloudWholeTaskReadyEventState` 的同 intent
  `STOPPED_AWAY` 从 `fact.locationChangedAtMs` 起 10 秒单次 re-wake。
- 路径到达复用 terminal frame；nearby/non-pathing 使用一次性 exact-window demand。
  Cloud 接收后接入现有 `NpcClickSmartQueueStore`。
- NPC producer 保持 `MEMORY -> TOOLTIP -> YELLOW -> PURPLE -> CTRL` 流式入队；首个
  `click != null`、`ctrlProbePoints` 非空的 CTRL 候选，或 `END/INVALID/ABORT` 终态，
  均 exact-once 发布 Ready。
- `FiveRingTaskV2` 接任务 NPC 已改用 terminal-frame/one-shot frame FIFO，不再等待完整
  `NpcPreparedClickPlan`；旧 `NpcPreparedClickPlanState` 依赖已从该任务移除。
- 五环 Tracker 现场 `readWuhuanTrackerTitle/prepareWuhuanPathingLink` fallback 已清除；
  后台继续复用 `CloudDialogPreparedActionState + PREPARED_ACTION_READY`。
- 五环接任务 Dialog、完成故事和鞋店购买选项已建立 `WuhuanDialogCatalog` 后台准备，
  phase 消费 exact prepared action；模板、偏移和候选顺序未改。

### 当前门禁

- Client main compile：`exit 0`。
- Cloud main compile：`exit 0`。
- focused Cloud test 尚不能运行：仓内全局 `testCompile` 当前存在并行改动留下的旧 API
  调用（`probeTypedPathing`、`updateObserved`、旧构造器参数），Maven 在进入指定测试前失败。
- 整卡仍为 `IN PROGRESS`：`FiveRingTaskV2` 尚有已有任务、未知故事、给物和完成故事等旧
  `dialogService.handleDialog(...)` 现场视觉分支待迁至 prepared consume；清零前不得写
  `SOURCE+TEST REVIEW PASSED`。

<!-- TRUE_EOF: CR277 IN-PROGRESS IMPLEMENTATION-CHECKPOINT 2026-07-26 -->

## 2026-07-26 WHOLE-CARD SOURCE+TEST DELIVERED

状态：`WHOLE-CARD SOURCE+TEST DELIVERED / AWAITING PARENT REVIEW`

### 最终实现

1. 五环 Tracker、Dialog、NPC 的 production phase 不再现场执行
   `dialogService.handleDialog(...)`、`readWuhuanTrackerTitle(...)` 或
   `prepareWuhuanPathingLink(...)`。Tracker/Dialog 使用
   `CloudDialogPreparedActionState + PREPARED_ACTION_READY`；NPC 使用
   `NpcClickSmartQueueStore` 流式 FIFO。
2. `NpcClickSmartQueueStore.Session.isReadyMessage(...)` 将
   `click == null && ctrlProbePoints 非空` 的 CTRL 候选视为首个可执行候选并立即发布 Ready；
   MEMORY/TOOLTIP/YELLOW/PURPLE/CTRL 仍按既有顺序逐项生产，不等待完整 plan。
3. pathing 到达复用 exact terminal frame；nearby/non-pathing 使用一次性
   `1024x768` exact-window prepared-frame demand。普通 observation ROI 仍为 `<=640`，
   `CloudWholeTaskObserver` 不再注册非法 `wuhuan-accept-npc 1024x768` 普通 ROI。
4. `clickInitialNpcForAccept(...)` 在注册 demand/等待 Ready 前显式
   `forceRelease` inherited coarse turn；等待上限为 15 秒。超时、暂停或停止会 exact cancel
   demand/FIFO session 并清 correlation；普通超时返回 `false`，由既有
   `MAX_ACCEPT_RETRY=5` 的 ACCEPT_TASK 重试合同处理。Ready 后才重新
   `taskTurnCoordination.run(...)` 并原子消费 FIFO。既有小地图 pathing 保权语义未修改。
5. Cloud inbox 只暂存匹配 frame，不提前清 demand。Http handler 仅在
   `DecisionEngine.prepareAndUnlockDemandedNpcFrame(...)` 返回
   `ARRIVAL_UNLOCKED` 后 exact ACK/clear；prepare/unlock 失败保留 demand 供 Client 重发。
   `FRAME_ALREADY_PREPARED` 仍继续执行幂等 unlock。
6. 双仓 validator 对 response demand 增加 exact `windowId/hwnd/taskRunId` cross-check；
   foreign window、foreign hwnd、foreign taskRun 三种 response 均 fail-closed。
7. 鞋店 `BUY_SHOES/PREPARE` 的两类遗留 capture 已限制为后台 producer：
   模板 ROI 匹配与回城画面差异轮询均通过
   `CompletableFuture.supplyAsync + TaskExecutionContextHolder.callWith` 执行；
   phase 线程只在不持 coarse turn 时等待 prepared 结果，再提交既有输入。模板、rect、
   threshold、候选顺序、随机点击偏移、点击次数及 fallback 坐标均未改变。后台 stop 异常会
   解包后按原类型传播，不会被误报为普通 phase failure。
8. 父级已有 `CloudWholeTaskReadyEventState` 同 intent `STOPPED_AWAY` 从
   `fact.locationChangedAtMs` 起 10 秒 exact-once re-wake 代码与测试未改。

### 七处旧现场 Dialog 调用与替代路径

1. setup 屏幕类型检查：现场 `handleDialog(INSPECT)` -> 后台 `INSPECT` prepared action，
   `inspectPreparedDialogType(...)` 消费。
2. Tracker miss 的“已有任务”判断：现场 option 识别 -> 后台
   `VERIFY_EXPECTED_DIALOG/WuhuanDialogCatalog.ALREADY_HAS_TASK`。
3. 当前接任务屏幕检查：现场 Dialog inspect -> 后台 `INSPECT` prepared action。
4. 接任务返回后的未知 STORY 处理：现场 detect/handle -> prepared 分类后仅执行
   `DialogService.clickPreparedStory()` 的 input-only 动作。
5. Tracker anchor 后 Dialog recheck：现场 detect/white-template -> prepared
   `INSPECT/VERIFY_WHITE_TEMPLATE`。
6. give-item：现场 `handleDialog(giveItem)` -> prepared `INSPECT`，确认 option 后仅执行
   `DialogService.giveItemFromPreparedOpenOption(...)` 的 input-only 动作。
7. 完成/日常故事判断：现场白模板识别 -> 后台 `VERIFY_WHITE_TEMPLATE`，按 catalog target
   消费 exact prepared action。

鞋店购买 option 另使用
`WUHUAN_SHOE_SHOP_BUY_OPTION/WuhuanDialogCatalog.SHOE_SHOP_BUY` 后台 prepared action；
获得输入机会后只消费坐标并点击。

### 精确写集

Client `D:\mavenProject\DHXY-cr271`：

- `src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationPreparedFrameDemand.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationPreparedFrame.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationRequest.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationResponse.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationProtocolValidator.java`
- `src/main/java/com/bot/dhxy/window/observation/PreparedFrameCapture.java`
- `src/main/java/com/bot/dhxy/window/observation/ExactWindowPreparedFrameCapture.java`
- `src/main/java/com/bot/dhxy/window/observation/WindowObservationRunner.java`
- `src/main/java/com/bot/dhxy/window/observation/SpringObservationRunnerFactory.java`
- `src/test/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationPreparedFrameDemandIdentityContractTest.java`

Cloud `D:\mavenProject\dhxy-cloud-brain`：

- 上述五个 byte-identical observation wire/validator 文件
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngine.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/NpcClickSmartQueueStore.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/observation/CloudWindowObservationInbox.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/observation/CloudObservationHttpHandler.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java`
- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `src/main/java/com/bot/dhxy/service/DialogService.java`
- `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`
- `src/main/java/com/bot/dhxy/task/wuhuan/WuhuanDialogCatalog.java`
- `src/test/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationPreparedFrameDemandIdentityContractTest.java`
- `src/test/java/com/yueyunfe/dhxy/cloudbrain/observation/PreparedFrameInboxAckContractTest.java`
- `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wuhuan/FiveRingPreparedNpcContractTest.java`
- `src/test/java/com/yueyunfe/dhxy/cloudbrain/NpcArrivalFrameQueueStoreContractTest.java`

### 验证

- Client focused Maven：
  `mvn -q -DskipTests=false "-Dtest=ObservationPreparedFrameDemandIdentityContractTest" test`
  -> `3/3 PASS`。
- Cloud focused 隔离编译 + JUnit Console：
  `ObservationPreparedFrameDemandIdentityContractTest`,
  `PreparedFrameInboxAckContractTest`,
  `FiveRingPreparedNpcContractTest`,
  `NpcArrivalFrameQueueStoreContractTest` -> `16/16 PASS`。
- Client：`mvn -q -DskipTests=false compile` -> `exit 0`。
- Cloud：`mvn -q -DskipTests=false compile` -> `exit 0`。
- 五个共享 wire/validator 文件双仓 SHA-256 -> `5/5 byte-identical`。
- 双仓 `git diff --check` -> `exit 0`；仅既有 LF/CRLF 提示。
- Cloud 常规 Maven focused test 在进入指定测试前仍被仓内既有 stale test API
  （`probeTypedPathing`、`updateObserved`、旧构造器签名）阻断；本卡四个 focused class
  已独立对当前 `target/classes` 编译并真实运行，非仅 source scan。
- 未运行 runtime、UI、capture 或物理输入；fresh 五窗运行验收保留给用户。

### Worker 自检

- `P0=0`
- `P1=0`
- `P2=0`

<!-- TRUE_EOF: CR277 WHOLE-CARD SOURCE+TEST DELIVERED AWAITING-PARENT-REVIEW 2026-07-26 -->

## 2026-07-26 父级 SOURCE+TEST REVIEW #1

结论：`P0/P1/P2 = 0/3/2`，`REPAIR REQUIRED`，owner 保留。

### P1-1：Ready 可早于 exact frame unlock，违反“唤醒即消费”

- `NpcClickSmartQueueStore.Session.push(...)` 在首个可执行候选入队后立即调用
  `readyCallback`，但 arrival session 此时仍可能是 `unlocked=false`。
- `DecisionEngine.prepareAndUnlockDemandedNpcFrame(...)` 的 producer 是异步启动；producer
  可以先于随后执行的 `unlockArrivalFrame(...)` 产生首候选。
- 被提前唤醒的五环窗口取得 task turn 后，Client
  `NpcArrivalFrameFifoLocalExecutor.consumeOne(...)` 会收到
  `cloud-brain-npc-arrival-awaiting-exact-frame-gate` 的 `WAIT`，并可在持有 coarse turn 时
  轮询等待，违反本卡“Ready 后获得 turn 只能立即消费”的冻结合同。

返修条件：Session 仅在“首个 ready message 已存在”和“exact frame 已 unlock”两个条件同时
满足时 exact-once 发布 Ready；必须覆盖 producer-first/unlock-second 与
unlock-first/producer-second 两种顺序，以及重复 push/unlock 不重复发布。

### P1-2：四条 prepared 物理输入仍绕过 coarse task turn

`FiveRingTaskV2` 的 `BUY_SHOES`、`ACCEPT_TASK`、`HANDLE_DIALOG` 等 phase 由
`runPhaseWithoutTaskTurn(...)` 执行，但以下 prepared consumer 在等待/读取完成后直接提交输入，
没有像 NPC FIFO 和 Tracker handover 一样重新调用 `taskTurnCoordination.run(...)`：

- `consumePreparedWuhuanClick(...)` 的鞋店购买选项点击；
- `consumePreparedAcceptDialog(...)` 的接任务选项点击；
- `tryAcceptInitialTaskFromCurrentScreen(...)` 的 `dialogService.clickPreparedStory()`；
- `tryGiveItemAndTriggerPathingIfPossible(...)` 的
  `dialogService.giveItemFromPreparedOpenOption(...)`。

这会让五窗 prepared 动作只经过物理输入队列，却没有遵守既有 task-turn 放权边界，队员仍可在
队长未放权时插入。返修必须保持“等待/后台计算不持 turn”，但每次真实 input-only consume
都必须在 Ready 后通过现有 `taskTurnCoordination.run(...)` 获取 exact window 的 coarse turn；
不得新增第二套协调器或把视觉计算搬回 turn 内。

### P1-3：鞋店 NPC 仍走 foreground `clickNpcSmart(...)`

`FiveRingTaskV2.buyShoeFromShopOwner(...)` 仍直接调用
`npcClickService.clickNpcSmart(shoeShopOwnerNpc()...)`。该入口会在当前 phase 内执行
`runSingleFrameNpcClickPlan(...)`：`Alt+4 + 180ms + 1024x768 capture` 后才启动 FIFO，
不是本卡要求的后台 prepared-frame demand/Event Bus handover。`BUY_SHOES` 又由
`runPhaseWithoutTaskTurn(...)` 运行，因此这条生产路径既没有预先后台准备，也没有在 Ready 后
取得 coarse task turn。

返修条件：复用本卡已经建立的 nearby/non-pathing 一次性 exact-frame demand +
`NpcClickSmartQueueStore` 流式 FIFO；鞋店 owner 的等待必须不持 turn，首候选/终态 Ready 后才
取得 turn 并消费。禁止复制 `NpcClickSmart` 算法、模板、顺序或点击公式；必须删除五环 production
对普通 foreground `clickNpcSmart(...)` 的调用，并覆盖鞋店 exact correlation/超时取消。

### P2-1：一次性整帧捕获未释放 BufferedImage

Client `ExactWindowPreparedFrameCapture.capture(...)` 编码 PNG 后没有 `image.flush()`。五窗 demand
重试会持续保留 native image 资源。请在保留宽高字段后使用 `finally` 释放，不改变截图、编码或
协议语义。

### P2-2：本卡测试仍在证明已退役的“完整 plan”语义

`FiveRingPreparedNpcContractTest.preparedPlanPreservesFifoAndDefensivelyCopiesCandidates()` 仍构造
`NpcPreparedClickPlan` 并断言“foreground consumer receives the complete immutable FIFO”。
这不是当前 production 路径，且文字与本卡“首候选立即唤醒、不等待完整 plan”相反。请删除或
替换为真实 `NpcClickSmartQueueStore` 流式 ready/consume 合同，避免将退役行为误当回归门禁。

### 已确认通过的部分

- HTTP handler 构造 response 时已经读取 post-ACK 的
  `inbox.currentPreparedFrameDemands(...)`，成功 ACK 不会在同一响应重发旧 demand。
- 接任务 NPC FIFO 与 Tracker handover 已在 Ready 后重新取得 task turn。
- focused 16/16、Ready recovery 2/2、Client identity 3/3、双仓 compile 与共享 wire SHA
  证据有效，但不能覆盖上述生产时序和全部 prepared input 边界。

<!-- TRUE_EOF: CR277 PARENT-REVIEW-1 REPAIR-REQUIRED P0-P1-P2=0-3-2 2026-07-26 -->

## 2026-07-26 Repair #1 WHOLE-CARD RE-DELIVERY

父级 Review #1 的 `P0/P1/P2=0/3/2` 已逐项返修，当前 Worker 自检
`P0/P1/P2=0/0/0`。

### Review finding 关闭证据

1. **Ready 双条件 exact-once**
   - `NpcClickSmartQueueStore.Session` 分离记录 `readyMessagePresent` 与 `unlocked`；
     只有两者同时成立才通过 `readyPublished.compareAndSet(false, true)` 发布一次 Ready。
   - `click`、`ctrlProbePoints`、`END/INVALID/ABORT` 均沿用既有 ready-message 语义；
     CTRL-only 首候选不会漏唤醒。
   - 测试覆盖 producer-first/unlock-second、unlock-first/producer-second，以及重复
     `push/unlock` 不重复发布。

2. **全部 converted prepared 物理输入重新取得既有 coarse task turn**
   - 鞋店 prepared option、接任务 prepared option、`clickPreparedStory`、
     `giveItemFromPreparedOpenOption` 均在等待/视觉准备结束后通过既有
     `taskTurnCoordination.run(...)` 执行真实 input-only consume。
   - prepared 等待前显式 `forceRelease(...)`；真实输入后使用 `MUST_YIELD`，
     没有新增协调器，也没有把截图/OCR/template 搬回 task turn。
   - Tracker handover 与接任务 NPC FIFO 原有 Ready 后取 turn 路径保持不变。

3. **鞋店老板删除 foreground `clickNpcSmart(...)`**
   - `buyShoeFromShopOwnerWithRetry(...)` 改为 nearby/non-pathing exact-frame demand +
     现有 `NpcClickSmartQueueStore` 流式 FIFO。
   - 每次尝试使用 UUID exact correlation；等待不持 turn；15 秒 bounded deadline；
     pause/stop/timeout 精确取消 demand/session；Ready 后才取 coarse turn 消费。
   - `FiveRingTaskV2` production 中
     `npcClickService.clickNpcSmart(...)` 引用计数为 `0`；未复制或修改 SmartClick
     算法、模板、阈值、候选顺序和点击公式。

4. **Client 图片资源释放**
   - `ExactWindowPreparedFrameCapture.capture(...)` 在编码前保存宽高，并在
     `finally` 中执行 `image.flush()`；成功、几何失败和编码异常均释放图片。

5. **测试合同清理**
   - 删除退役 `NpcPreparedClickPlan` “完整 FIFO”测试。
   - 改为真实流式首候选 Ready/consume、双顺序门控、exact-once 与鞋店 exact demand
     生产路径合同。

6. **父级既有修复保持**
   - handler 继续以 post-ACK `currentPreparedFrameDemands(...)` 构造响应。
   - inbox 仅在 `prepareAndUnlockDemandedNpcFrame(...)` 返回
     `ARRIVAL_UNLOCKED` 后 exact ACK/clear；失败保留 demand；
     `FRAME_ALREADY_PREPARED` 仍幂等 unlock。
   - `windowId/hwnd/taskRunId` 三种 foreign demand 负例均保留并通过。
   - `CloudWholeTaskReadyEventState` 的同 intent `STOPPED_AWAY` 10 秒单次 re-wake
     文件无本轮 diff。
   - 清理 `FiveRingTaskV2` 重复的
     `java.util.concurrent.CompletionException` import。

### Repair #1 精确写集与 SHA-256

Cloud：

- `NpcClickSmartQueueStore.java`
  `1C48720220EC9DAB900A5D20B488AFBDF8CFF4A9862DF8103FDD31D27991183D`
- `FiveRingTaskV2.java`
  `1BD7CA300E027B00811DFB1DCAA8EC21C3D59635BB6B14BD17DE674E0DD74731`
- `NpcArrivalFrameQueueStoreContractTest.java`
  `F1ACDBDA00AE0162F4A57EBFBF451F176BF66C0A1EFBDC739C2BBF520A235CCE`
- `FiveRingPreparedNpcContractTest.java`
  `DFBB6588FAA502F258D85E8801C9D2F662A190855F33D8D388611D139D8C78FB`

Client：

- `ExactWindowPreparedFrameCapture.java`
  `AA38DE438CBF0285991C170F5A888693A0708C199F8B3B7DC3EDCC23070E7E26`
- `ExactWindowPreparedFrameCaptureResourceContractTest.java`
  `F86D31119531EA817DF9A442B08F245157528CC502AFADDEEB8336D0B666DFD9`

### 验证

- Cloud：`mvn -q -DskipTests=false compile` -> `exit 0`。
- Client：`mvn -q -DskipTests=false compile` -> `exit 0`。
- Client focused Maven：
  `ObservationPreparedFrameDemandIdentityContractTest` +
  `ExactWindowPreparedFrameCaptureResourceContractTest` -> `4/4 PASS`。
- Cloud 四个 CR277 focused class 隔离 `javac` + JUnit Console：
  `ObservationPreparedFrameDemandIdentityContractTest`,
  `PreparedFrameInboxAckContractTest`,
  `FiveRingPreparedNpcContractTest`,
  `NpcArrivalFrameQueueStoreContractTest` -> `18/18 PASS`。
- Cloud 常规 Maven focused test 仍在进入指定测试前被仓内既有 stale test API
  （`probeTypedPathing`、`updateObserved`、旧构造器签名）阻断；本卡测试已对当前
  `target/classes` 独立编译并真实执行。
- 五个共享 observation wire/validator 文件双仓 SHA-256：
  `5/5 byte-identical`。
- 双仓本卡目标文件 `git diff --check` -> `exit 0`；Cloud 仅既有 LF/CRLF 提示。
- 未运行 runtime、UI、capture 或物理输入；未做 Git mutation。

<!-- TRUE_EOF: CR277 REPAIR-1 WHOLE-CARD SOURCE+TEST DELIVERED AWAITING-PARENT-REVIEW P0-P1-P2=0-0-0 2026-07-26 -->

## 2026-07-26 父级 SOURCE+TEST REVIEW #2

结论：`P0/P1/P2=0/0/0 / SOURCE+TEST REVIEW PASSED / OWNER RELEASED /
FRESH FIVE-WINDOW RUNTIME REQUIRED`。

父级本人已逐文件复核 Repair #1 全写集，确认：

- `NpcClickSmartQueueStore.Session` 只有在 ready message 已存在且 exact arrival frame
  已 unlock 后才发布 Ready；producer-first、unlock-first 和重复调用均保持 exact-once。
- 五环鞋店 option、接任务 option、story、give-item reacquire 及鞋店老板 NPC FIFO
  均在等待结束后重新取得既有 coarse task turn，再执行物理输入；等待期间不持 turn。
- 鞋店老板生产路径已删除 foreground `clickNpcSmart(...)`，改用 exact-frame demand +
  既有 SmartClick 流式 FIFO；`FiveRingTaskV2` 中该 foreground 调用计数为 `0`。
- Client exact frame capture 在编码前保存宽高，并在所有出口 `flush()`；
  handler 以 post-ACK demand 集构造响应，不会在成功 ACK 后重发旧 demand。
- 退役 complete-plan 断言已删除；当前测试覆盖真实 FIFO 首候选唤醒、双顺序门控、
  exact identity、取消与资源释放。

父级独立验证：

- Client focused Maven：`4/4 PASS`；Client compile：`exit 0`。
- Cloud 当前源码隔离合同：CR277 四类 `18/18 PASS`；Ready recovery `2/2 PASS`；
  Cloud compile：`exit 0`。
- observation wire/validator 五个共享文件：`5/5 byte-identical`。
- 双仓目标写集 `git diff --check`：`exit 0`（Cloud 仅既有 LF/CRLF 提示）。
- Cloud 常规 Maven focused test 仍会在进入指定测试前被仓内既有 stale test API
  阻断；父级已将本卡测试对当前 `target/classes` 独立编译并真实执行。

未运行 runtime、UI、capture 或物理输入，未做额外 Git mutation。源码与测试门已关闭；
用户 fresh 五窗口验收仍需确认各窗口后台预计算、首候选 Ready 唤醒、task-turn 排他和
同 intent 10 秒 re-wake 的真实时序。

<!-- TRUE_EOF: CR277 PARENT-SOURCE-TEST-REVIEW-2 PASSED P0-P1-P2=0-0-0 OWNER-RELEASED CLIENT-4-4 CLOUD-18-18 READY-2-2 DUAL-COMPILE-0 WIRE-5-5 FRESH-FIVE-WINDOW-RUNTIME-REQUIRED 2026-07-26 -->

## 2026-07-26 Fresh Runtime Review #3 - P1 REOPEN

结论：`P0/P1/P2=0/1/0 / FRESH RUNTIME FAILED / REPAIR REQUIRED`。

Fresh run `remote-turn-f5d8f8d3-b04b-4001-bc78-39c88d917157` 的任务启动 ACK
成功，Client Runner 也已启动；失败不是进程、HTTP 或输入队列启动失败，而是五环启动
handover 的负结果消费断档：

- Cloud Observer 持续发布 exact `TASK_TRACKER_NEGATIVE_READY`，sequence 至少从
  `30` 增长到 `82`，说明后台 Tracker 计算已经完成并明确给出负结果。
- `FiveRingTaskV2.detectHandover(...)` 在没有正 prepared action 时，始终把结果折叠为
  `RUNNER_PREPARED_NOT_READY`，每 `900ms` 返回
  `background tracker preparation not ready; release turn and retry`。
- 因此任务永久停留在 `HANDOVER_DETECT`，没有把 `TASK_NOT_FOUND` 转为
  `ACCEPT_TASK`，也没有把 `TASK_FOUND_NO_GREEN/NO_LINK` 转为 `SYNC_TASK_PANEL`。
  这正是用户看到“启动后不动”的原因。

Repair #2 冻结边界：

1. 正 prepared Tracker action 继续优先并沿既有 task-turn 消费。
2. fresh exact `TASK_NOT_FOUND` 必须转 `ACCEPT_TASK`。
3. fresh exact `TASK_FOUND_NO_GREEN/NO_LINK` 必须转 `SYNC_TASK_PANEL`。
4. 只有既无正结果也无 fresh exact 负结果时才允许继续 park。
5. 不改 Tracker 算法、ROI、模板、阈值、输入顺序、其他任务或 Client。

<!-- TRUE_EOF: CR277 FRESH-RUNTIME-REVIEW-3 REOPEN P0-P1-P2=0-1-0 REPAIR-2-REQUIRED HANDOVER-NEGATIVE-CONSUMPTION 2026-07-26 -->

## 2026-07-26 Fresh Runtime Repair #2 WHOLE-CARD RE-DELIVERY

Fresh run `remote-turn-f5d8f8d3-b04b-4001-bc78-39c88d917157` 暴露的
`HANDOVER_DETECT` negative consumption 缺口已修复。当前自检
`P0/P1/P2=0/0/0`，等待父级审查。

### 生产修复

`FiveRingTaskV2.tryClickWuhuanTrackerLink(...)` 现在按冻结优先级消费 Runner 结果：

1. 先消费 fresh prepared Tracker action；存在正候选时仍沿既有点击/路径启动链进入
   `WAIT_PATHING`，negative 不得覆盖。
2. 没有正候选时，读取 exact slot 的最新 `TASK_TRACKER_NEGATIVE_READY`。
3. negative 必须同时满足：
   - age 不超过既有 `READY_EVENT_PRIORITY_MAX_AGE_MS=3000ms`；
   - `windowId/hwnd/taskRunId` 与当前执行上下文完全一致；
   - `taskType=WUHuan_V2`；
   - `operation=TASK_TRACKER_PATHING`；
   - `targetKeyword=wuhuan`；
   - summary 携带受限的 `TaskTrackerPanelNegativeResult.Status`。
4. 状态映射：
   - `TASK_NOT_FOUND` -> `ACCEPT_TASK`，不伪造 `taskAccepted`；
   - `TASK_FOUND_NO_GREEN` -> `SYNC_TASK_PANEL`，保留已有任务事实；
   - `TASK_FOUND_NO_LINK` -> `SYNC_TASK_PANEL`，保留已有任务事实。
5. 只有既没有 fresh 正候选，也没有 fresh exact negative 时，才继续返回
   `RUNNER_PREPARED_NOT_READY` 并 park。

未修改 Tracker 算法、ROI、模板、阈值、输入顺序、Observer 发布、其他任务或 Client。

### 精确写集与 SHA-256

Cloud `D:\mavenProject\dhxy-cloud-brain`：

- `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`
  `535753173EE8E567954CF159161E775B28909CBFC8B8C585FFF672BA87216710`
- `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wuhuan/FiveRingHandoverNegativeReadyContractTest.java`
  `9A38878639CD56900EBF7A4E210448084F21EFCC11150D26E635FDBFEE7456DA`

Client 与用户基线保持只读、无本轮写入。

### Focused 验证

- 独立 `javac` + JUnit Console：
  `FiveRingHandoverNegativeReadyContractTest` -> `3/3 PASS`。
- 行为覆盖：
  - 三种 fresh exact negative 的 phase/`taskAccepted` 映射；
  - stale negative 不推进；
  - wrong window negative 被 exact fence 拒绝且不推进；
  - wrong taskRun negative 被 exact fence 拒绝且不推进；
  - fresh positive 与 fresh negative 同时存在时，positive 优先且进入 `WAIT_PATHING`。
- `mvn -q -DskipTests=false compile` -> `exit 0`。
- 两个目标文件 `git diff --check` -> `exit 0`。
- 未运行 runtime、UI、capture 或物理输入；未做 Git mutation。

<!-- TRUE_EOF: CR277 FRESH-RUNTIME-REPAIR-2 WHOLE-CARD SOURCE+TEST DELIVERED AWAITING-PARENT-REVIEW P0-P1-P2=0-0-0 HANDOVER-NEGATIVE-CONSUMPTION 2026-07-26 -->

## 2026-07-26 父级 SOURCE+TEST REVIEW #4

结论：`P0/P1/P2=0/0/0 / REPAIR #2 SOURCE+TEST REVIEW PASSED /
FRESH CLOUD RESTART+RUNTIME RETEST REQUIRED`。

父级逐文件确认：

- `tryClickWuhuanTrackerLink(...)` 先消费既有正 prepared action；正候选与负事件同时
  存在时，正候选优先并进入 `WAIT_PATHING`。
- 只有没有正候选时才读取 exact `TASK_TRACKER_NEGATIVE_READY`；window/HWND/taskRun、
  taskType、operation、target 和 `3000ms` freshness 全部匹配后才允许映射。
- Observer 生产格式 `status=<TaskTrackerPanelNegativeResult.Status>, reason=...`
  只接受三个固定 enum 前缀，不接受任意 summary 推进。
- `TASK_NOT_FOUND` 不伪造 `taskAccepted` 并转 `ACCEPT_TASK`；
  `TASK_FOUND_NO_GREEN/NO_LINK` 保留已有任务事实并转 `SYNC_TASK_PANEL`；
  无正负结果才保持 `HANDOVER_DETECT`。
- 本轮没有修改视觉算法、ROI、模板、阈值、Observer、输入顺序、Client 或其他任务。

父级独立验证：

- 从当前 `target/classes` 重新 `javac` focused test，并运行 JUnit Console：
  `FiveRingHandoverNegativeReadyContractTest` -> `3/3 PASS`。
- Cloud `mvn -q -DskipTests=false compile` -> `exit 0`。
- SHA 与 Worker 交付一致；目标写集 `git diff --check` -> `exit 0`。
- 旧 `FiveRingTaskTrackerTurnContractTest` 独立重编译运行仍为 `0/7`；该夹具继续依赖
  已退役的 turn 内直接 Tracker 调用、旧 task-turn wiring 和旧 timeout，不能覆盖当前
  event-driven 生产链。本轮未修改或谎报该旧族通过。

未运行 runtime/UI/capture/input。必须重启 Cloud JVM 后 fresh 验证：
启动阶段出现 exact `TASK_NOT_FOUND` 后应立即离开 `HANDOVER_DETECT` 并进入接任务流程，
不得继续每 `900ms` 输出 `background tracker preparation not ready`。

<!-- TRUE_EOF: CR277 PARENT-SOURCE-TEST-REVIEW-4 REPAIR-2-PASSED P0-P1-P2=0-0-0 CLOUD-FOCUSED-3-3 COMPILE-0 LEGACY-TRACKER-FAMILY-0-7-STALE FRESH-CLOUD-RESTART-RUNTIME-RETEST-REQUIRED 2026-07-26 -->

## 2026-07-26 FRESH RUNTIME REVIEW #5 / REPAIR #3

结论：`P0/P1/P2=0/1/0 / FRESH RUNTIME FAILED / REPAIR #3 IMPLEMENTED /
CLOUD COMPILE PASSED / FRESH RETEST REQUIRED`。

Fresh run `remote-turn-2767b323-dc32-4979-bb14-ed00d58f71fe` 的完整证据：

- Client 为 exact intent `ab8fa86b-d3ac-48ac-9b04-aab9a626eda8` 注册长安
  `(87,174)` 路径，并于 `21:14:42.939` 发布 `ARRIVED`。
- Cloud 随后发布同 intent 的 `PATHING_TERMINAL` sequence `10` 与
  `NPC_CLICK_PLAN_READY` sequence `11`，证明 Runner、HTTP ingress、Ready Event 与
  NPC arrival FIFO 均已工作。
- `FiveRingTaskV2` 被 sequence `10` 唤醒后仍重复输出
  `accept NPC navigation has no terminal snapshot yet`，没有进入既有
  `clickInitialNpcForAccept(...)`。
- 根因是 `FiveRingPhaseContext.waitForAcceptNpcPathing(...)` 在 Cloud 收到导航结果后才用
  `System.currentTimeMillis()` 记录 phase 开始；它比 Client intent 的真实创建时间晚约
  `2.2s`。`isUsablePathingSnapshot(...)` 的 `1s` 时间 grace 因而把 exact 同一 intent
  错判为 stale。

Repair #3 只修改 `FiveRingTaskV2.isUsablePathingSnapshot(...)`：当
`acceptNpcArrivalIntentId` 与 snapshot intentId 精确一致时，不再让延迟记录的 Cloud
phase 时间否决它；source、target 和 exact id 仍须匹配，非 exact 路径继续执行原时间栅栏。
未修改 Runner、导航、NPC ClickSmart、FIFO、视觉算法、ROI、模板或输入顺序。

验证：Cloud `mvn -q -DskipTests=false compile` -> `exit 0`。必须重启 Cloud JVM 后
fresh 验收；预期 `ARRIVED` 后立即输出 `accept NPC navigation wait ended by watcher`，
随后消费既有 exact NPC FIFO，不再重复 terminal wait。

<!-- TRUE_EOF: CR277 FRESH-RUNTIME-REVIEW-5 REPAIR-3-IMPLEMENTED P0-P1-P2=0-1-0 CLOUD-COMPILE-0 FRESH-CLOUD-RESTART-RUNTIME-RETEST-REQUIRED 2026-07-26 -->

## 2026-07-26 FRESH RUNTIME REVIEW #6 / REPAIR #4

结论：`P0/P1/P2=0/1/0 / FRESH RUNTIME FAILED / REPAIR #4 IMPLEMENTED /
DUAL COMPILE PASSED / FRESH DUAL RESTART+RETEST REQUIRED`。

Fresh run `remote-turn-5f7897fb-77be-4b52-8032-e6ea7133da41`：

- Repair #3 已生效：Runner 于 `22:12:09.090` 判定 exact intent
  `69dad396-ef04-48d6-94cb-3e97b8eb1823` 为 `ARRIVED`；Cloud 随后记录
  `accept NPC navigation wait ended by watcher`，并发布同 intent 的
  `NPC_CLICK_PLAN_READY` sequence `8`。
- 但 `continueIfAcceptNpcNavigationStillPathing(...)` 在返回点击流程前调用
  `clearPathing()`。Client 的 `CLEARED` fact 会按合同取消同 intent arrival demand/session，
  所以 `clickInitialNpcForAccept(...)` 稍后消费时得到
  `cloud-brain-npc-arrival-session-missing-or-stale`，`verified=false`。
- Repair #4 将顺序改为：保留 exact ARRIVED/session -> 消费既有 FIFO -> 再
  `clearPathing()`；与当前修罗 exact arrival 路径一致。
- 同轮 fallback 还暴露 `ExactWindowPreparedFrameCapture` 用屏幕绝对 `(0,0)` 调用
  `captureToMemory(...)`，绑定窗口位于 `(395,239)` 时产生负相对坐标并连续失败。现改为
  `tracker.getWindowBaseX/Y()` 到 `base+1024x768`，协议上传仍保持窗口相对 origin `(0,0)`。

未修改 SmartClick 识别器、候选顺序、模板、ROI、阈值或输入动作。Cloud 与 Client
`mvn -q -DskipTests=false compile` 均 `exit 0`。必须重启双端后 fresh 验收：exact FIFO
不得再出现 session missing；若进入 replacement prepared-frame，也不得再出现
`crop outside window ... relative=(-baseX,-baseY)`。

<!-- TRUE_EOF: CR277 FRESH-RUNTIME-REVIEW-6 REPAIR-4-IMPLEMENTED P0-P1-P2=0-1-0 DUAL-COMPILE-0 FRESH-DUAL-RESTART-RUNTIME-RETEST-REQUIRED 2026-07-26 -->

## 2026-07-26 FRESH RUNTIME REVIEW #7 / REPAIR #5 REQUIRED

结论：`P0/P1/P2=0/1/0 / FRESH RUNTIME FAILED / REPAIR #5 IN PROGRESS`。

Fresh run `remote-turn-a3f36d77-e4f9-401c-840f-df943623f056` 的精确证据：

1. Runner 于 `22:23:24.891` 判定 exact intent
   `4fc7e2ab-2a29-489a-bdf3-80a243a9f675` 为 `ARRIVED`；Cloud 于
   `22:23:26.270` 已发布同 intent 的 `NPC_CLICK_PLAN_READY`。但
   `FiveRingTaskV2.acceptTask(...)` 在消费 FIFO 前继续串行执行
   `current-screen-accept-check` 与 `setup-dialog-check`，直至
   `22:23:34.286` 才开始 Tooltip 候选点击。图片方案约 `1.38s` 已完成，
   多余延迟约 `8s`。
2. prepared Tracker action 在 `ACCEPT_TASK` phase priority 中已真实点击，intent
   `ec23b4d0-4607-4cb4-82ae-ced98f1c178e` 随后由 Client Runner 判为
   `STOPPED_AWAY`。但 `submitWuhuanTrackerGreenClick(...)` 只读取点击前的 Cloud
   mirror，没有像 NavigationService 那样读回 exact 本地 pathing fact，因而错误返回
   false，phase 未进入 `WAIT_PATHING`。
3. `STOPPED_AWAY` 于 `22:24:01.690` 已发布，但正在执行的 accept Dialog wait 只等待
   `PREPARED_ACTION_READY`，没有消费 terminal，白等 `30s` 后错误重走接任务 NPC，
   而不是 `SYNC_TASK_PANEL -> 重新点击绿色链接`。
4. 重导航 `(87,174)` 命中 `isImmediateMiniMapFireAndHandoff(...)` 特例；该分支把
   `TurnPathingIntent` 设为 null，之后的 `registerWindowPathingIntent(...)` 又仅记录
   trace、不写本地事实。`FiveRingTaskV2` 随即执行 exact-intent guard 并抛
   `IllegalStateException: accept NPC pathing started without an exact active intent`。

用户批准的 Repair #5：

- exact ARRIVED 且同 intent `NPC_CLICK_PLAN_READY` 已存在时，直接消费既有 FIFO，
  不再执行两次到达前检查；
- accept/prepared Dialog 等待必须能被 exact `PATHING_TERMINAL` 唤醒；
- prepared Tracker 点击后读回并接纳 exact 本地 pathing fact，成功后进入
  `WAIT_PATHING`；
- Tracker `STOPPED_AWAY` 必须恢复到现有热启动 `HANDOVER_DETECT`，由热启动重新判断
  Tracker 面板并沿既有 prepared-event 链点击绿色链接；不得新增专用恢复状态；
- 删除五环 `(87,174)` 的无 intent fire-and-handoff 特例，恢复标准 exact-intent
  handoff。

冻结：不改 Client、视觉算法、模板、ROI、阈值、NPC ClickSmart 候选顺序或输入坐标。

<!-- TRUE_EOF: CR277 FRESH-RUNTIME-REVIEW-7 REPAIR-5-REQUIRED P0-P1-P2=0-1-0 OWNER-CODEX 2026-07-26 -->

## 2026-07-26 REPAIR #5 IMPLEMENTED / SOURCE REVIEW

结论：`P0/P1/P2=0/0/0 / REPAIR #5 IMPLEMENTED / CLOUD COMPILE PASSED /
FRESH CLOUD RESTART+RUNTIME RETEST REQUIRED`。

实际写集：

1. `FiveRingTaskV2.acceptTask(...)` 在 exact ARRIVED intent 已存在
   `NPC_CLICK_PLAN_READY` 时直接进入 FIFO 消费，不再先执行
   `current-screen-accept-check` 与 `setup-dialog-check`。
2. `acceptInitialDialogAndTriggerPathing(...)` 同时等待
   `PREPARED_ACTION_READY` 与 `PATHING_TERMINAL`。终态先到时清除本轮 Dialog interest，
   返回 `HANDOVER_DETECT` 热启动；没有新增恢复 phase 或第二套 Tracker 解析。
3. `submitWuhuanTrackerGreenClick(...)` 在原子点击完成后调用 Client
   `readPathing(...)`，将唯一的本地 exact fact 接纳进 Cloud mirror，再按同 intentId
   判定 `PATHING_STARTED`。
4. 已接任务 Tracker intent 的 `STOPPED_AWAY` 从 `WAIT_PATHING` 回
   `HANDOVER_DETECT`，由现有热启动重新读取任务面板并沿 prepared-event 链点击绿色链接。
   接任务前 NPC 导航自身的 `STOPPED_AWAY` 仍按原语义重试接任务导航。
5. `NavigationService.isImmediateMiniMapFireAndHandoff(...)` 删除五环接任务
   `(长安,87,174)` 无 intent 特例；该路径恢复标准 exact-intent 点击、Client pathing
   proof 与 readback。

冻结项保持：未改 Client、视觉算法、模板、ROI、阈值、`NPC ClickSmart` 候选顺序、
点击坐标或输入动作。

验证：

- Cloud `mvn -q compile` -> `exit 0`。
- `mvn -q -DskipTests compile` 被仓库 Enforcer 按设计拒绝，未绕过。
- 本轮依照仓库规则不运行 runtime、UI、capture、物理输入或视觉 testcase。

Fresh gate：重启 Cloud JVM 后验证 exact ARRIVED 到 FIFO 点击不再重复两次 Dialog
inspection；prepared Tracker 点击必须进入 `WAIT_PATHING`；Tracker
`STOPPED_AWAY` 必须出现 `HANDOVER_DETECT` 热启动并重新读取 Tracker；接任务导航不得再
抛 `without an exact active intent`。

<!-- TRUE_EOF: CR277 REPAIR-5-IMPLEMENTED SOURCE-REVIEW-P0-P1-P2=0-0-0 CLOUD-COMPILE-0 FRESH-CLOUD-RESTART-RUNTIME-RETEST-REQUIRED 2026-07-26 -->

## 2026-07-27 RESTART / BACKGROUND WAKE / MINI-MAP OWNERSHIP REPAIR

结论：`P0/P1/P2=0/0/0 / SOURCE REVIEW PASSED / DUAL COMPILE PASSED /
FRESH DUAL-RESTART RUNTIME RETEST REQUIRED`。

本轮以 `2026-07-26 23:53` fresh 日志和当前源码为准，关闭四个直接断点：

1. 五环“距离下次领取时间未到”白色 STORY 模板加入现有
   `WuhuanDialogCatalog.completionStorySpecs()`；Observer 仍在后台准备，Cloud 消费后按
   正常终态结束，不再把该画面当未知 Dialog 留在前台重试。
2. 五环三个现有 NPC/商店导航入口复用唯一 `CloudTaskTurnCoordination`，只保护
   `NavigationService` 的打开小地图、点击、确认和关闭这一段高层交互；方法返回后
   `MUST_YIELD`，不锁住后续走路，也没有增加第二把锁或第二条输入队列。
3. Client 主启动不再于调用控制服务前丢弃 `accepting=false` 的已选窗口。控制服务发现
   同窗口旧 loop 时，先请求停止并等待注册表移除，再启动新 run；旧 terminal 回调以既有
   `RemoteTaskHandle` 对象身份隔离，不能覆盖替换后的新 run。
4. Dashboard 的 `getSnapshot/getAllSnapshots` 改为纯读取，不再在每次 UI 刷新时把同一
   HWND 的瞬时标题提交到 `WindowRuntimeContext`。窗口注册和真实 turn/capture 入口仍按
   原路径刷新 exact binding；由此避免 UI 轮询造成角色身份来回漂移并清空
   prepared/pathing 状态。

Tracker/prepared ready-event 的生产和消费合同未另造实现；`23:53` 三个已接任务窗口不动的
上游原因是本次启动被过滤成 `targetWindowIds=[]`，同时 passive binding refresh 持续清空
运行态。上述两处已关闭后继续沿 CR277 既有 `PREPARED_ACTION_READY` /
`TASK_TRACKER_NEGATIVE_READY` 链路。

验证：

- Client `mvn -q compile` -> `exit 0`。
- Cloud `mvn -q compile` -> `exit 0`。
- 双仓 `git diff --check` 无 whitespace error（仅既有 CRLF 提示）。
- 未启动 runtime/UI/capture/物理输入；fresh gate 仍需重启双端验证多窗口。

<!-- TRUE_EOF: CR277 RESTART-BACKGROUND-WAKE-MINIMAP-OWNERSHIP SOURCE-REVIEW-P0-P1-P2=0-0-0 DUAL-COMPILE-0 FRESH-DUAL-RESTART-RUNTIME-RETEST-REQUIRED 2026-07-27 -->

## 2026-07-27 REPAIR #6 / ARRIVED 后 NPC 点击失败禁止回退导航

Fresh 运行证据：

- `13:23:16.522`：Client Runner 对 `67555` 发布 exact `ARRIVED`，当前位置
  `(87,173)`、目标 `(87,174)`。
- 随后五环接任务 NPC FIFO 已实际进入候选点击阶段；候选全部失败。
- `13:25:22`：Cloud 又执行 `Alt+1`，点击小地图 `(87,174)`，并再次关闭小地图。

P1 根因：

`FiveRingTaskV2.acceptTask(...)` 在 `tryClickNearbyAcceptNpc(context)` 返回 false 后，
直接落穿到 `navigateWithTaskTurn(...)`。但能够进入该点击分支，已经由 exact
`NPC_CLICK_PLAN_READY` 或 fresh nearby 坐标证明导航阶段结束。点击失败不构成新的导航证据，
不得倒退状态。

修复合同与实现：

1. 删除 `tryClickNearbyAcceptNpc(...)` 整个中间层，包括其中“点击失败 fallback 到
   小地图导航”的错误语义。
2. `exactArrivalPlanReady || nearAcceptNpc` 直接进入 NPC 点击分支，是从导航域进入
   NPC 交互域的单向边界。
3. 该边界后的 NPC 点击失败，只调用既有
   `cleanupUiBeforeAcceptNpcClick("setup:arrived-npc-click-failed")`，递增 retry，
   并 `continue` 重试 NPC 点击。
4. `navigateWithTaskTurn(...)` 仅允许在从未取得 exact arrival plan 且 fresh 位置不在
   NPC 附近时执行。
5. 未改 `NPC ClickSmart`、prepared FIFO、候选顺序、视觉算法、模板、ROI、阈值、
   点击坐标或 Client。

验证：

- Cloud `mvn -q compile` -> `exit 0`。
- 新增合同锁定 cleanup/continue 必须位于导航调用之前；直接控制流检查通过。
- `mvn -q -Dtest=FiveRingPreparedNpcContractTest test` 未进入目标测试执行：全仓既有
  test source 在 `testCompile` 阶段仍引用已删除的
  `CloudWholeTaskObserver.probeTypedPathing/classify`、旧构造器等接口。该门禁漂移与
  本次两文件修改无关，但在修复前不能声称完整 test family 通过。

Fresh gate：重启 Cloud JVM 后，ARRIVED 后 NPC 点击失败只能看到 cleanup 与点击重试；
不得再出现五环接任务导航 `Alt+1`。运行验证通过前 CR277 仍保持 fresh pending。

<!-- TRUE_EOF: CR277 REPAIR-6-IMPLEMENTED SOURCE-REVIEW-P0-P1-P2=0-0-0 CLOUD-COMPILE-0 TESTCOMPILE-BLOCKED FRESH-CLOUD-RESTART-REQUIRED 2026-07-27 -->
