# CR271 / TURN-37 Xiuluo Whole-Task HTTPS Turn Card

## PARENT FROZEN WHOLE-CARD SOURCE-START READY - 2026-07-17T01:10:00-04:00

- 状态：`WHOLE-CARD SOURCE-START READY / ZERO OWNER`。
- 类型：既有完整 `TURN-37` 父卡；禁止 tranche、fragment、子卡或多人共享写集。
- sourceDependsOn 已满足：`13C+14+15+17+21+22+23+28+30+34A+34B`。
- approvalDependsOn：`TURN-26+TURN-27+TURN-T01/T02/T03/T04`、本卡父级 source/test-source review、
  唯一 named test 与 Cloud compile。
- 领取点 production：`XiuluoTaskV2.java` 4,225 行，SHA-256
  `46f9665999f644be63b7f27e772429e68190322fbde487641cbeff0f747f519a`；唯一 test 当前不存在。

## 唯一完整写集

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
2. Create `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/xiuluo/XiuluoWholeTaskTurnContractTest.java`
3. 本固定报告只允许 claim/delivery/return/repair 追加；其余 production/test 全部只读。

## 整卡验收合同

- 严格保持 `696a12b0` 修罗完整 phase、快捷 tracker 与非快捷 fallback、接任务/导航/NPC/dialog、看打入战、
  maintenance、combat source、回程、retry/recovery、keep-turn/park、watchdog/expiry 次数和顺序。
- 只把 ownership/transport 迁到 HTTPS turn；不得新增 TTL、验证、park/yield、retry、cleanup、fail-closed 或
  第二协议/store，不得复制 Dialog/Navigation/NpcClick 业务算法。
- `TURN-26/27` 保持当前 Task public caller signature；Worker 不修改 predecessor/API 文件。
- 唯一 test 从 public Task path 覆盖 `BC4+BASE+TASK+IMG+LS` 和 `docs/业务逻辑.md` 修罗失败处理表，包含
  shortcut/non-shortcut、terminal/uncertain、raw PNG、closed services、exact context、UUID/command 正负矩阵。
- `TaskExecutionContext.builder()` 等缺失构造在本 Task 内迁到 bound turn-native entry，禁止 shim/manual client。
- 无已批准业务差异；唯一业务基线 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。

## 自行领取协议

Worker 领取前重读三张 READY 卡 EOF 和 SHA；仅最早在本文件 EOF 追加 `EXTERNAL-X TURN-37 WHOLE-CARD
CLAIMED` 并回读确认唯一者为 owner。领取后负责完整 production/test/report/返修；Java writer 活动时不运行
Maven，稳定后只运行授权 named test 与适用 compile。

<!-- TRUE_EOF: TURN-37 PARENT-FROZEN WHOLE-CARD-SOURCE-START-READY ZERO-OWNER PROD=46f96659/4225 TEST=ABSENT APPROVAL-WAITS-26-27-T01-T04 STRICT-696 NO-FRAGMENT NO-DISPATCH 2026-07-17T01:10:00-04:00 -->

## EXTERNAL-d TURN-37 WHOLE-CARD CLAIMED - 2026-07-17T01:15:00-04:00

- Implementation Worker：**CR271 External Worker d**（会话 `2d492c23-3376-4f43-b376-e4ee48038045`；本 lane
  已完成 TURN-34C 一轮 PASSED 与 TURN-28 Repair #4/#5 至 Review #3 `0/0/0` PASSED）。非 reviewer，不自批，
  本段不含 `APPROVED/CLOSED`；父级为唯一 manager/final reviewer。
- 领取时间：`2026-07-17T01:15:00-04:00`。响应父级 01:10 `PARALLEL WHOLE-TASK SOURCE START`（状态总账
  PARENT MESSAGE，to A/C/D）；claim 前按自行领取协议重读三张 READY 卡 EOF——TURN-35/36/37 均 4 sections、
  EOF=自行领取协议、零 claim（mtime 01:10:46）。
- 完整任务卡：既有完整父卡 `TURN-37`（Xiuluo Whole-Task HTTPS Turn），合同=本卡 01:10 PARENT FROZEN brief
  全文（整卡验收合同 + 唯一完整写集 + 自行领取协议）。我承担整卡全部 production/test/report 与父级审核后的
  全部返修，直至父级 `SOURCE+TEST SOURCE REVIEW PASSED` 或 canonical whole-card `OWNER RETURNED`；
  不拆卡、不建子卡、不做 fragment/tranche，不与他人共享写集。
- 唯一完整写集（不增不减）：
  1. `dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
  2. Create `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/xiuluo/XiuluoWholeTaskTurnContractTest.java`
  3. 本固定报告 append-only。其余两仓 production/test 全部只读。
- 领取点实测：production `XiuluoTaskV2.java` **4,225 行 / SHA-256
  `46f9665999f644be63b7f27e772429e68190322fbde487641cbeff0f747f519a`**（与父级冻结值逐字一致）；唯一 test
  路径当前不存在（与 brief 一致）。
- 依赖检查：sourceDependsOn `13C+14+15+17+21+22+23+28+30+34A+34B` 已由父级 01:10 审计宣告满足；其中
  TURN-28 shared API 我本 lane 亲历交付并 Review #3 PASSED（`ObjectiveTextRecognizer.recognize` public typed
  :66/Result :842、`currentExactWindowPendingSmartClickProofToken` :2399 实盘在位）。approvalDependsOn
  （TURN-26/27、T01-T04、named test、Cloud compile）不阻 source-start。
- 冲突检查：EXTERNAL-B=TURN-26（DialogService+三 port+LocalOcrClient+prepared-state+三 dialog test）零交集；
  TURN-33（Goodall，SummonSkill 三文件，Task caller 只读）零交集；TURN-35/36（Wubei/FiveRing task 文件）
  与本写集零交集且领取时均无 owner；无第二 TURN-37 writer（append 后回读 EOF 复核）。
- 实施边界：严格 `696a12b0` 修罗完整 phase/快捷 tracker/非快捷 fallback/接任务/导航/NPC/dialog/看打入战/
  maintenance/combat source/回程/retry-recovery/keep-turn-park/watchdog-expiry 次数与顺序逐值保持；仅
  ownership/transport 迁 HTTPS turn；`TaskExecutionContext.builder()` 等缺失构造在本 Task 内迁 bound
  turn-native entry，禁 shim/manual client；不改 predecessor/API 文件；不新增 TTL/验证/park-yield/retry/
  cleanup/fail-closed/第二协议/store；不复制 Dialog/Navigation/NpcClick 业务算法。唯一 test 从 public Task
  path 覆盖 `BC4+BASE+TASK+IMG+LS` + 修罗失败处理表正负矩阵。
- 禁令：零 Git mutation；其它 Java writer（B=TURN-26 等）活动期间不运行 Maven/JUnit/compile；不启
  runtime/application/server/Task/UI/capture/input；保护两仓全部 dirty/untracked。
- 无已批准业务差异；按 `696a12b0` 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-37 EXTERNAL-d WHOLE-CARD CLAIMED PROD=46f96659/4225 TEST=ABSENT ANTI-RACE-PRECHECKED 2026-07-17T01:15:00-04:00 -->

## EXTERNAL-d TURN-37 WHOLE-CARD OWNER RETURNED / PLAN-CONTRACT BLOCKED - 2026-07-17T01:42:00-04:00

交还完整 TURN-37。不是 delivery，不是 approval，未拆卡，未建子卡。执行父级 TURN-26 Review #2 既定规程：
"若冻结写集无法合法承载 typed producer/state，必须 canonical `WHOLE-CARD OWNER RETURNED / PLAN-CONTRACT
BLOCKED`，由父级修正整卡合同后再领，不能带占位交付。"

### 本轮零字节；写集字节 = 领取点原值（归还时实测）

- `XiuluoTaskV2.java` 4,225 行 / `46f9665999f644be63b7f27e772429e68190322fbde487641cbeff0f747f519a`（未动）
- 唯一 test 路径仍不存在（未创建）；零 Git mutation；未运行 Maven/JUnit/compile/runtime/input。

### 完整传递审计 A：可在冻结写集内合法闭合的维度（约占调用面 80%，供下任直接复用）

1. `uiCleanerService` ×21（cleanUpAll×18/cleanLightweightInterruptions×2/closeAllGenericWindows×1）→ 既有
   `remote/CloudUiCleanerPort` **三方法逐一同名对应**（:41/:60/:79，phaseCode+actionSlot 约定见
   `AutoCombatService:625/:644` 已接受先例）。
2. `questManagerService.captureCurrentQuestDetailForTask` ×1 → 既有
   `turn/client/CloudQuestLocalServiceClient.captureCurrentQuestDetailForTask`（:93，**同名精确对应**）。
3. `bagService.findAndUseMainBagTaskPageItem` ×2 → 既有 `CloudBagLocalServiceClient`（executeReturnItem :73；
   `ReturnItemPrescanService` 已是其 Cloud 消费先例）。
4. `objectiveTextRecognitionService.recognize` ×1（:3539）→ TURN-28 已 PASSED 的
   `ObjectiveTextRecognizer.recognize` public typed API（:66）。
5. `tracker` ×4（refreshWindowState/getWindowBaseX/Y/captureToMemory，:3193-3200 accept-window snapshot）→
   `TurnGameClient.capture` + `latestWindowMetadata().windowRect`（TURN-26 已接受范式）。
6. `resolveExecutionContext`（:3885-3894 `TaskExecutionContext.builder()` 本地铸造）→ 按 AutoBattleTask
   已 PASSED 形态改为拒绝 context-free（合同点名项）。
7. `context.getTaskRunId()`（:419 按 long 用）→ Cloud 返回 String（:202），机械修正。
8. `windowScopedTempPath.resolve` ×1（失败存档诊断）→ TURN-26 已接受的 cloud-memory 诊断口径。
9. 死字段移除：`hotStartResolver`（类型 `XiuluoHotStartResolver` 全仓不存在且 **0 调用**）、
   `taskTurnCoordinator`、`multiWindowTaskManager`（均 0 调用）。
10. `TaskCheckpoint.throwIfStopRequested(context, holder, msg)` ~20 处：Cloud TaskCheckpoint 有 3 个 long
    返回重载（:25/:43/:62），大概率已兼容（逐一核即可）。
11. `inputSequences` ×3（moveAndClickLeft×2/pressAltC×1）→ TURN-19/21/22 已定 MOVE+WAIT+CLICK / KEY_TAP
    closed turn 命令形态。

### 完整传递审计 B：冻结写集内无合法落点的四个族（PLAN-CONTRACT BLOCKED 根因）

1. **park/wake 生命周期**：`windowReadyEventBus` ×9（`awaitNewer`/`awaitNewerPathingTerminalOrPreparedRoute`
   :840/:847、`currentSequence`×6、`latest(COMBAT_STATE_CHANGED)` :2274）+ `WindowPathingIntent` 注册/清除
   （`runtime.markPathingStarted` :1774、clear :1812、`attachShortcutTargetMapUpgrade` :1780）+
   `getPathingSnapshot` :2174。`WindowReadyEventBus/WindowRuntimeContext` 全仓不存在（`window/runtime/` 现仅
   WindowHandleParser/WindowTitleIdentity[Parser] 三个无关小类）；事件生产者属 **TURN-27 写集**
   （NavigationRoutePlanResolver 等），per-window authority state 的 Cloud owner 归 **TURN-38 系列**
   （B 的 TURN-26 归还 18:26 已指认 38M/38C，父级未冻结）。in-file 轮询替代要么改变合同冻结的
   park/watchdog/expiry 次数与顺序，要么复制 Navigation/location mechanics（明令禁止）。
2. **task turn ownership 引擎**：`taskTransactionRunner.run` 包裹**每个 phase**（:498）+
   `forceReleaseTurn`（:408 finally）。`TaskTransactionRunner/TaskTurnCoordinator` 全仓不存在
   （task/transaction 仅 Result/Outcome/YieldPolicy 三模型）。已 PASSED 的 AutoBattleTask 是"无 runner 直跑"
   形态，但修罗合同同时冻结 keep-turn/park/handoff（fair-lock leader/follower 让渡 :778-787）语义——
   直跑塌缩=业务语义变化，重建 runner=新增 owner/session 机制（禁止）。需父级裁决 turn 世界的等价形态。
3. **WindowRuntimeContext 杂项族**：`updateTaskRunProgress` ×2（:331/:399 进度上报宿主缺失）、
   `registerXiuluoDialogInterest`（:3286，WindowDialogInterest 挂载宿主缺失；与 B 实施中的 TURN-26
   prepared-action state 互锁）、`currentWindowLabel` :3274-3277。
4. **windowTaskContextHolder** ×12（rawCurrent×5/callWith :3136 等）：holder 类型不存在且返回值即缺失的
   runtime 类型，无法机械替换为 `TaskExecutionContextHolder`（后者无 runtime 视图）。

旁证：TURN-30（写集=同一 XiuluoTaskV2，已 SOURCE PASSED/BUILD PENDING）的
`XiuluoTaskTrackerTurnContractTest`（840L，同包在盘）在 :361-371 反射构造
`window.runtime.WindowTaskContextHolder/WindowReadyEventBus` ——该 test 编译需要这些类型在 classpath，
说明其曾存在或该卡同样从未编译；其 :332-345 断言 PARKED outcome + waitSpec 形状为**已接受行为**，
即 park 语义被冻结保留而非授权删除。

### 建议的计划修正项（供父级冻结新合同，非我扩权）

- 裁决 turn 世界的 keep-turn/park/事件等待宿主：新增 typed wait/事件设施（含 owner 文件与写集）或明确
  授权的等待映射语义；同一裁决适用 **TURN-35/36**（WubeiTask :430/:517、FiveRingTaskV2 同引用
  runner/bus——建议三卡统一，避免 A/C/d 各自撞同墙）。
- 裁决 `TaskTransactionRunner` 的 Cloud 形态（AutoBattleTask 直跑先例 vs 修罗 fair-lock handoff 冻结语义）。
- 指定 `WindowRuntimeContext` 家族（进度/pathing intent/dialog interest/label）Cloud owner 或前置 38 系列
  对应行；与 TURN-27（pathing 生命周期）、TURN-26（prepared state，B 实施中）排序。
- 裁决 `gameStateUtil`（isSameMapName×9/isNearCoordinate×5/recordMovementIntent×1）与
  `coordinateHelper` ×3 纯 helper 的承载：in-file 私有 helper（与同用它们的 NavigationService=TURN-27 债
  形成双拷贝漂移风险）或共享 util 归属卡。
- `textRecognizer.getAllTextResultsForMatch` ×1 的 typed 替代源指定。

### 诚实记录

领取（01:15）后本轮工作=纯只读深勘（16 缺失类型全量清点、~73 调用点定位、映射基建实盘核验、先例比对
TURN-26/30/34C/AutoBattleTask），全程零 production/test 字节、零写集外写入、零 Git mutation、未运行
Maven/JUnit/compile、未启 runtime/UI/capture/input；两仓 dirty/untracked 与他人写集（B=TURN-26、
C=TURN-36）未触碰。我自此不持有本卡 owner；父级修正整卡合同重开 READY 后，本 lane 可再领（届时以
修正合同为准）。审计 A 全部结论可被任何下任 owner 直接复用。

**无已批准业务差异；唯一业务基线 `696a12b0`。**

TRUE_EOF

<!-- TRUE_EOF: TURN-37 EXTERNAL-d WHOLE-CARD OWNER RETURNED PLAN-CONTRACT-BLOCKED ZERO-BYTES 4-BLOCKED-FAMILIES(PARK-WAKE/TRANSACTION-RUNNER/RUNTIME-MISC/CONTEXT-HOLDER) 80PCT-MAPPABLE-AUDIT-PRESERVED SAME-WALL-APPLIES-TURN-35-36 2026-07-17T01:42:00-04:00 -->

## PARENT ACCEPTS WHOLE-CARD RETURN / PLAN AMENDMENT #1 - 2026-07-17T01:32:26-04:00

- 接受 External d 的 canonical 零字节整卡归还；owner 已释放。状态为
  `PLAN-CONTRACT BLOCKED / ZERO OWNER`，不是 delivery/review failure。
- 四类阻断成立，且已确认同墙影响 TURN-35/36。根因是 01:10 错把 TURN-26/27 降为纯 approval gate，
  同时要求 Task 在不存在的 Cloud runtime API 上保持 park/wake/pathing/prepared 语义。
- 统一合同修正：`TURN-26+TURN-27` 恢复为 source gate；TURN-27 创建唯一 exact-context、无 TTL/ledger 的
  Cloud pathing state，Tasks 只读消费。`WindowTaskContextHolder/WindowRuntimeContext` 改为显式
  `TaskExecutionContext`/metadata；progress/label 只作诊断，不得成为业务 truth；dialog prepared 只读 TURN-26。
- 本地 `TaskTransactionRunner` 不迁入 Cloud Task：每 phase 必须原位恰好执行一次，保留原
  `TaskTransactionResult`、`TaskYieldPolicy`、park/yield/retry/fallback gate 与顺序；Cloud runtime 单窗串行批准门
  仍归 TURN-40B。不得用轮询、额外 sleep、TTL、第二 store 或复制 Navigation/Dialog mechanics 代替。
- `gameStateUtil/CoordinateHelper` 的 map/near/pathing 计算归 TURN-27；Task 通过其 public typed API 消费。
  `textRecognizer.getAllTextResultsForMatch` 改读 TURN-28 已通过的 canonical typed recognizer，不复制 OCR。
- TURN-26/27 source pass 后父级按实际 API 追加 Amendment #2 并恢复 READY；本轮不派卡。

<!-- TRUE_EOF: TURN-37 PARENT-ACCEPTS-RETURN PLAN-AMENDMENT-1 BLOCKED ZERO-OWNER SOURCE-WAITS-TURN26-27 PATHING-STATE-OWNER-TURN27 NO-RUNTIME-COPY 2026-07-17T01:32:26-04:00 -->

## PARENT PLAN-CONTRACT AUDIT #2 - AMENDMENT #1 ASSUMPTIONS NOT DELIVERED - 2026-07-17T14:24:00-04:00

- `TURN-26/27` 均已 source review passed；但 Amendment #1 预设的 Task-facing API 未完整交付，本卡继续
  `PLAN-CONTRACT BLOCKED / ZERO OWNER`，不得仅因 27 passed 自动恢复 READY。
- `CloudDialogPreparedActionState` 没有非破坏性 exact-bound read，无法等价替换 prepared route/enter-battle 的
  先看后消费、时效与 binding 判断；`CloudNavigationPathingState` 只提供 active intent/snapshot，不提供
  ready-event sequence、early wake、fresh combat/prepared arbitration。
- Amendment #1 归给 TURN-27 的 `GameStateUtil/CoordinateHelper` Task-facing map/near/movement/randomize/
  approach/plausibility API 实际不存在；TURN-28 的 `ObjectiveTextRecognizer.recognize` 也不是
  `TextRecognizer.getAllTextResultsForMatch` 的逐字等价替代，须先冻结 OCR input/result/fallback 映射。
- 本地 `TaskTransactionRunner`/event bus 的 fair-lock handoff、afterSequence 防丢事件、timeout 与 park 补偿没有
  typed Cloud owner。直接删掉后连续 loop、固定 sleep、轮询或复制 local runtime 均改变严格 696 基线。
- `TURN-40B` 不能前置：`40B <- 39 <- 38A <- 35/36/37`，会形成依赖环。父级须先冻结无环的共享状态/等待/
  utility/OCR owner 与验收合同，再追加 Amendment #3；此前无合法 READY，不允许 claim。

<!-- TRUE_EOF: TURN-37 PARENT-PLAN-CONTRACT-AUDIT-2 BLOCKED ZERO-OWNER TURN26-27-PASSED AMENDMENT1-API-ASSUMPTIONS-MISSING NO-DAG-CYCLE NO-READY 2026-07-17T14:24:00-04:00 -->

## EXTERNAL-d TURN-37 WHOLE-CARD CLAIMED UNDER USER DIRECT ORDER - 2026-07-17T14:48:00-04:00

- Implementation Worker：**CR271 External Worker d**（会话 `2d492c23-3376-4f43-b376-e4ee48038045`；本卡 01:42
  归还审计的作者，TURN-28/34C 两卡 PASSED）。非 reviewer，不自批；父级仍为唯一 final reviewer。
- **领取依据**：用户于 14:47 直接明令"不要停 给我去领卡接着做"。该用户指令优先于父级 14:24
  `NO-READY / 不允许 claim` 的暂缓；本 claim 如实标注为 USER DIRECT ORDER 领取，父级可随时以 canonical
  段落纠正/撤销，我即刻服从。防竞态规程已执行：claim 前独立调用完整读卡（EOF=Audit #2，零 claim），
  append 后另行回读确认唯一。
- 完整任务卡：既有完整父卡 `TURN-37`，合同=01:10 frozen brief + 01:32 Amendment #1（827 归还裁决）+
  14:24 Audit #2 全文。写集不变：`dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java` +
  Create 唯一 `XiuluoWholeTaskTurnContractTest.java` + 本卡 append-only；其余只读。
- **执行策略（零占位承诺）**：
  1. 立即实施 01:42 归还审计 A 中在现行冻结合同下完全合法的映射维度（约 80% 调用面）：
     uiCleaner×21→既有 `CloudUiCleanerPort` 同名三方法；quest×1→`CloudQuestLocalServiceClient.
     captureCurrentQuestDetailForTask`（同名）；bag×2→`CloudBagLocalServiceClient`；
     objective×1→TURN-28 `ObjectiveTextRecognizer.recognize`（其与 `TextRecognizer.getAllTextResultsForMatch`
     的映射差异将按 Audit #2 要求在交付中逐字段冻结说明，供父级裁决而非擅自等价）；tracker×4→
     `TurnGameClient.capture`+`latestWindowMetadata().windowRect`；`resolveExecutionContext` 拒 context-free；
     3 死字段移除；`getTaskRunId` String 化；inputSequences×3→closed MOVE/WAIT/CLICK、KEY_TAP 命令。
  2. pathing/park 维度只读消费 **已交付且 PASSED** 的 `CloudNavigationPathingState` 只读镜像与
     `TurnPathingSnapshot` metadata bridge——不足以覆盖的 event-sequence/early-wake/fair-lock 语义，
     **不做轮询/sleep/shim 替代**。
  3. Audit #2 四个缺口族（prepared peek、event-wait/afterSequence、Task-facing map/near/movement/randomize、
     runner/park typed owner）：随交付附**逐 API 的 typed 合同提案**（签名/语义/负例/owner 建议，含无环
     依赖排布），作为父级 Amendment #3 的输入；缺口未冻结前对应调用点保持可编译的最小忠实结构并
     **逐点显式披露**，绝不伪装完成。
- 禁令不变：零 Git mutation；不动 `D:\mavenProject\DHXY`（用户 IntelliJ 基线）；他 Java writer 活动时不跑
  Maven；不启 runtime/UI/capture/input；不拆卡不扩写集；不复制 local runner/detector/watcher/算法。
- 无已批准业务差异；唯一业务基线 `696a12b0`。

TRUE_EOF

<!-- TRUE_EOF: TURN-37 EXTERNAL-d WHOLE-CARD CLAIMED USER-DIRECT-ORDER-OVERRIDE AUDIT-A-80PCT-EXECUTION AMENDMENT3-API-PROPOSALS-COMMITTED NO-PLACEHOLDER 2026-07-17T14:48:00-04:00 -->

## PARENT ACCEPTS USER-DIRECT CLAIM / HARD GAP FENCE - 2026-07-17T14:52:00-04:00

- 原卡 physical EOF 的 14:48 claim 为用户直接命令后的 canonical claim；父级接受 External d 为 TURN-37
  sole owner，不撤销、不重派。状态改为 `SOURCE_ACTIVE / USER-DIRECT CLAIM / HARD GAP FENCE`。
- 用户“不要停，去领卡接着做”授权继续整卡工作，但没有批准 prepared/event/park/map/OCR 的业务语义变化。
  Audit A 已有 exact public API 且可证明 baseline 等价的机械迁移可继续；父级不在 WIP 中途做 source review。
- **硬边界**：Audit #2 四缺口族的原调用点在 Amendment #3 冻结前必须保持现有语义与结构，不得写
  stub、恒 null/false、临时 local copy、第二 store、poll/sleep、占位 wrapper 或所谓“可编译最小结构”。
  到达缺口时，D 必须在本卡和总账提交一次性完整 typed API 提案（owner/file/signature/lifetime/binding/
  negative/acceptance/DAG），并同时继续所有不受阻 Audit A 项，不得向用户问流程性问题。
- Amendment #3 未由父级写入本卡前，不得把缺口族标完成，也不得 canonical `SOURCE+TEST DELIVERED`；唯一 test
  仍须整卡闭合后创建/交付。任何真实 source delta 仅视为受保护 WIP，不构成隐式合同批准。
- 父级已观测首个 source delta：`XiuluoTaskV2.java` SHA 从 `46f96659...` 变化，新增
  `CloudUiCleanerPort` import/field；这是 WIP 活动证据，尚未审核。Java writer active，父级不运行 Maven。

<!-- TRUE_EOF: TURN-37 PARENT-ACCEPTS-USER-DIRECT-CLAIM EXTERNAL-d-SOLE-OWNER SOURCE-ACTIVE HARD-GAP-FENCE AUDIT-A-MECHANICAL-ONLY AMENDMENT3-REQUIRED NO-PLACEHOLDER NO-DELIVERY-BEFORE-FREEZE 2026-07-17T14:52:00-04:00 -->

## PARENT CANONICAL OWNER RETURNED AFTER USER DELETED WORKER D - 2026-07-17T14:55:00-04:00

- 用户明确确认 External D 任务已删除；D 不再存在可 ACK 14:52 hard-gap fence、继续实施或 canonical delivery
  的会话。父级终止 14:48 user-direct claim并释放 TURN-37 owner。
- 保护现有 WIP，不回滚、不删除：`XiuluoTaskV2.java` 实盘 SHA-256
  `c0125a494441bb384b213d6ebf5579f67d7ea09f8672ef76ed0c26f8f99a6746`、4,227 行、245,305 bytes、
  mtime `2026-07-17T14:45:36.8159418Z`；唯一 `XiuluoWholeTaskTurnContractTest` 仍不存在。该 WIP 从未形成
  `SOURCE+TEST DELIVERED`，父级不作中途 source review，也不宣称通过。
- 状态恢复为 `PLAN-CONTRACT BLOCKED / ZERO OWNER / NO READY / WIP PRESERVED`。后续重开前须先审计现有 WIP
  相对 14:52 hard-gap fence，并冻结 Amendment #3；禁止新 owner 把 WIP 当成已批准合同。

<!-- TRUE_EOF: TURN-37 PARENT-CANONICAL OWNER-RETURNED USER-DELETED-EXTERNAL-D WIP-PRESERVED=c0125a49 NO-DELIVERY BLOCKED ZERO-OWNER NO-READY 2026-07-17T14:55:00-04:00 -->

## PARENT AMENDMENT #3 DAG REPAIR - 2026-07-17T15:02:00-04:00

- 状态改为 `WAITING TURN-38A FOUNDATION / ZERO OWNER / NO READY`；现有 `c0125a49...` WIP 继续原样保护。
- TURN-38A-F 父级 source review 通过后，本卡自动转 `READY / ZERO OWNER`，下一 owner 从受保护 WIP 做
  hard-gap 审计后续接，不得把旧 WIP 视为已批准 delivery。
- 唯一 prepared/event/turn owner 已固定在 38A-F；本卡不得复制 local runtime、创建第二 store 或改业务语义。

<!-- TRUE_EOF: TURN-37 WAITING-TURN38A-FOUNDATION ZERO-OWNER WIP-PRESERVED=c0125a49 AUTO-OPEN-AFTER-38A-F-PASS 2026-07-17T15:02:00-04:00 -->

## PARENT READY RELEASE AFTER TURN-38A-F PASS - 2026-07-17T12:12:00-04:00

- 状态：`READY / ZERO OWNER / WIP PRESERVED`。TURN-38A-F 已获父级 Source Review #3
  `P0/P1/P2=0/0/0`，本卡 Amendment #3 的自动开放条件已满足。
- 这不是派卡。任一有完整容量的 Worker 可按本卡原有防竞态协议自行 canonical claim；最早有效 claim 为唯一 owner。
- 现有 `XiuluoTaskV2.java` WIP `c0125a49...` 原样保护。下一 owner 必须先对照 14:52 hard-gap fence 审计该 WIP，
  再续接固定写集；不得把 WIP 当作已批准 delivery，不得回滚他人字节或复制 local runtime/业务算法。

<!-- TRUE_EOF: TURN-37 PARENT-READY-RELEASE AFTER-38A-F-PASS READY ZERO-OWNER WIP-PRESERVED=c0125a49 NO-DISPATCH 2026-07-17T12:12:00-04:00 -->

## EXTERNAL-C TURN-37 CANONICAL WHOLE-CARD CLAIM - 2026-07-18T04:16:00-04:00

- **CR271 External Worker C** 按本卡 physical EOF `PARENT-READY-RELEASE AFTER-38A-F-PASS / READY / ZERO OWNER / WIP-PRESERVED=c0125a49`（12:12）与总账"唯一 READY/ZERO OWNER 完整卡为 TURN-37、父级不派卡"，行防竞态 canonical claim：**External C = TURN-37 sole owner**（whole-card，不拆卡）。claim 自署时间=append 时刻。
- 预检证据：append 前完整扫本卡 section 列表，physical EOF=12:12 READY release，其后无更早/并发 claim；总账 EOF 无并发 TURN-37 owner。append 后将回读 EOF；若现更早 claim 立即 canonical 自撤让位。
- 我刚完成 TURN-36 Review #4 `0/0/0` source+test PASSED、owner 已释放，具完整容量领本卡。
- **受保护 WIP 处置**：现有 `XiuluoTaskV2.java` WIP `c0125a49`（4,227L/245,305B，已删 worker D 遗留）**原样保护、不回滚、不删除、不当已批准 delivery**；续接前先对照 **14:52 hard-gap fence** 逐点审计该 WIP（Audit A 机械迁移可续；Audit #2 四缺口族=prepared peek / event-wait+afterSequence / Task-facing map+near+movement+randomize / runner+park typed owner，其原调用点在 Amendment #3 冻结前须保持现有语义/结构，**绝不** stub/恒 null-false/local copy/第二 store/poll-sleep/占位 wrapper；到达缺口一次性提交完整 typed API 提案 owner/file/signature/lifetime/binding/negative/acceptance/DAG）。
- 唯一 prepared/event/turn owner 已固定在 TURN-38A-F；本卡不复制 local runtime、不建第二 store、不改业务语义。唯一 `XiuluoWholeTaskTurnContractTest` 于整卡闭合后创建/交付。Amendment #3 未由父级写入本卡前不得把缺口族标完成、不得 canonical `SOURCE+TEST DELIVERED`。
- 纪律：零 Git mutation；`D:\mavenProject\DHXY` 只读；他 Java writer 活动时不跑 Maven（javac 单文件 parse 除外）；不启 runtime/UI/capture/input；不向用户提流程性问题；不自批、不建 reviewer。唯一业务基线 `696a12b0`。

<!-- TRUE_EOF: TURN-37 EXTERNAL-C CANONICAL-WHOLE-CARD-CLAIM SOLE-OWNER FROM-READY-ZERO-OWNER WIP-PRESERVED=c0125a49 AUDIT-VS-1452-FENCE NO-PLACEHOLDER NO-DELIVERY-BEFORE-AMENDMENT3 2026-07-18T04:16:00-04:00 -->

## PARENT WIP RECONCILIATION - 2026-07-17T18:16:00-04:00

- C 已完成 14:52 hard-gap audit，并进入 Audit-A 机械迁移；首批 3 个零调用死字段/2 import 已删除。
- 实盘 Cloud `XiuluoTaskV2.java` 当前 SHA-256=`cb1db7c6723e...`、4,222 行、mtime 18:15:32；相对 C 04:54
  event 的 `b7dfb071` 又有后续 WIP 字节，均保护但不作 source review，直到 canonical whole-card delivery。
- C sole owner、Amendment #3 真残余提案 gate、禁止 stub/第二 store/poll-sleep/复制算法的 hard fence 全部保持。
  A/C active Java writers，本轮不运行 Maven。

<!-- TRUE_EOF: TURN-37 PARENT-WIP-RECONCILIATION OWNER-C AUDIT-DONE MIGRATING REAL-WIP=cb1db7c6723e-4222 NOT-DELIVERY HARD-FENCE-PRESERVED NO-MAVEN 2026-07-17T18:16:00-04:00 -->

## PARENT WIP RECONCILIATION #2 - 2026-07-17T18:26:00-04:00

- C 的 Audit-A batch 2 已闭合 `getTaskRunId` String 化与 `CloudUiCleanerPort` 21 个 caller；实盘 Cloud
  `XiuluoTaskV2.java` 又推进至 SHA-256=`f0319233e40d...`、4,222 行、mtime 18:22:17。
- C sole owner、hard fence 与 Amendment #3 真残余 gate 保持；当前仍是 WIP、非 canonical delivery，父级不审半成品。
  A/C active Java writers，本轮不运行 Maven。

<!-- TRUE_EOF: TURN-37 PARENT-WIP-RECONCILIATION2 OWNER-C AUDIT-A-BATCH2 WIP=f0319233e40d-4222 NOT-DELIVERY NO-MAVEN 2026-07-17T18:26:00-04:00 -->

## EXTERNAL-C TURN-37 AMENDMENT #3 TYPED-API PROPOSAL #1 — bag atomic find+use gap - 2026-07-18T06:52:00-04:00

- **CR271 External Worker C**（sole owner）。hard-gap 审计中到达首个真缺口（Audit-A 项 3 "bag" 经核实非干净机械面），按 14:52 fence 一次性提交完整 typed API 提案作 Amendment #3 输入；缺口调用点保持 baseline 忠实结构（当前 `bagService.findAndUseMainBagTaskPageItem` 为编译断裂 WIP=显式披露，不 stub/不拆两段/不复制算法）。

### 缺口证据

- baseline `BagService.findAndUseMainBagTaskPageItem(String targetItemTemplate, TaskExecutionContext)`→boolean（DHXY BagService:320）在**单个 `inputSequences.submitExclusiveAndWait("bag:taskPageItemAction:USE:"+template, …)`** 内一次性 `interactWithMainBagTaskPageItemExclusive(template, ItemAction.USE, context)`——**原子 find+USE，独占输入锁跨整个 find+use**。
- Cloud `CloudBagLocalServiceClient.executeReturnItem(...)` 的 `TurnBagOperationArguments.ReturnItemIntent` 仅 3 值：`PRESCAN_TASK_PAGE`/`PRESCAN_FROM_BACK`/`USE_CACHED_RETURN_ITEM`——**prescan（找）与 use-cached（用）分离两段**，无单-op 原子 find+use。
- Cloud 无 `BagService` 类型（全仓不存在）；`ReturnItemPrescanService` 消费先例仅覆盖 prescan+use-cached 流。
- 拆成 prescan→use-cached 两 turn 在 prescan 与 use 间引入异窗可插入的窗口，改变 baseline 冻结的 exclusivity/atomicity（业务语义变化，fence 禁）。故 Xiuluo 两处 `findAndUseMainBagTaskPageItem`（tryUseStartupReturnItemOnce 等）无合法单-op 落点。

### 提案（请父级冻结为 Amendment #3；owner 归 bag foundation，非本卡扩权）

- **owner/file**：`com.yueyunfe.dhxy.cloudbrain.turn.client.CloudBagLocalServiceClient`（既有 bag turn owner）+ 协议 `com.bot.dhxy.cloud.turn.protocol.TurnBagOperationArguments`。
- **signature**（二选一，请父级定）：
  - (A) 新增枚举值 `ReturnItemIntent.FIND_AND_USE_TASK_PAGE` + 复用 `executeReturnItem(phaseCode, actionSlot, FIND_AND_USE_TASK_PAGE, targetItemTemplate, maxBagIndex, /*cachedPoint*/null, source, timeout)`；DHXY executor 对该 intent 在单 exclusive 锁内 find+USE（与 baseline `ItemAction.USE` 同机制），返 `BagOperationOutcome{terminal, state}`。
  - (B) 新增便捷方法 `BagOperationOutcome findAndUseMainBagTaskPageItem(String phaseCode, String actionSlot, String targetItemTemplate, String source, Duration timeout)` 封装 (A)。
- **lifetime/binding**：一次 HTTPS turn；exact bound window；**无 prescan-cache 依赖**（区别于 USE_CACHED_RETURN_ITEM），单 exclusive 输入锁跨 find+use 保原子性。
- **negative**：item 不在 task page→`State.NOT_FOUND`（used=false）；输入未点中/未使用→`State.NOT_USED`（false）；`Terminal.NOT_EXECUTED`（no-bound/机械失败）→ caller 优雅 false（同 baseline `submitExclusiveAndWait` 返 false）；`Terminal.STOPPED`→stop checkpoint。
- **acceptance**：`Terminal.EXECUTED && State.USED`→true；`EXECUTED && (NOT_FOUND|NOT_USED)`→false；`NOT_EXECUTED`→false（不伪造、不重发）；`STOPPED`→throwIfStopRequested。Cloud caller 映射 `boolean used = outcome.terminal()==EXECUTED && outcome.state()==USED`。
- **DAG**：仅依赖既有 bag LOCAL_SERVICE foundation（BAG_RETURN_ITEM 已 PASSED 同族）；无 prescan/cache/第二 store 依赖，无环。
- Amendment #3 冻结前本卡不迁此两点、不标完成、不 canonical delivery。

<!-- TRUE_EOF: TURN-37 EXTERNAL-C AMENDMENT3-PROPOSAL1 BAG-ATOMIC-FIND-USE-GAP FIND_AND_USE_TASK_PAGE-INTENT-OR-METHOD PROD=43138e8b-4274 AWAIT-AMENDMENT3-FREEZE 2026-07-18T06:52:00-04:00 -->

## PARENT AMENDMENT #3 FREEZE #1 - BAG ATOMIC FIND+USE - 2026-07-17T18:48:00-04:00

结论：缺口真实，按 `696a12b0` 不可拆成 prescan→cached-use 两个 turn。父级选择提案 **A**，拒绝额外便捷
wrapper B；本卡由此不存在待用户业务语义选择。

### 冻结合同

- 唯一 wire 扩展：双仓 `TurnBagOperationArguments.ReturnItemIntent.FIND_AND_USE_TASK_PAGE`，继续使用
  `TurnLocalOperation.BAG_RETURN_ITEM` 和 `CloudBagLocalServiceClient.executeReturnItem(...)`。参数 shape 固定为
  nonblank `targetItemTemplate`、`maxBagIndex=-1`、`cachedPoint=null`、nonblank source。
- DHXY local domain 同步增加同名 `BagReturnItemMacroIntent.Kind`/factory；`BagService.runReturnItemMacroDirectForExclusive`
  在现有 remote exclusive callback 内直接调用 baseline 同一
  `interactWithMainBagTaskPageItemExclusive(targetItemTemplate, ItemAction.USE, context)`。不得二次 acquire queue，
  不得拆 find/use、缓存坐标、增加 retry、TTL、store 或复制 bag 算法。
- local typed result 对该 intent 只允许 `USED/NOT_USED` 且 `cachePoint=null`；validator、executor、Cloud client
  `requireExecutedShape` 与 strict intent echo 必须闭合。Xiuluo 两 caller 映射：`EXECUTED+USED=true`；
  `EXECUTED+NOT_USED=false`；`NOT_EXECUTED=false`；`STOPPED` 立即 checkpoint；`UNKNOWN` 按 transport uncertainty
  向上失败，禁止当成业务 false，也不得自动重发。

### 固定写集与顺序

- 双仓 mirror：`TurnBagOperationArguments.java`、`TurnProtocolValidator.java`、`TurnCoreProtocolGoldenJsonTest.java`、
  `TurnProtocolValidatorContractTest.java`。
- DHXY：`BagReturnItemMacroIntent.java`、`BagService.java`、`BagLocalOperationExecutor.java`、
  `BagLocalOperationExecutorContractTest.java`；必要时仅补现有 dispatcher valid-call coverage，不新增 operation。
- Cloud：`CloudBagLocalServiceClient.java` 及其既有 contract test；本卡 `XiuluoTaskV2.java` 与唯一
  `XiuluoWholeTaskTurnContractTest.java`。
- A 当前仍持 TURN-35，其 foundation owner 覆盖共享 protocol 文件。C 在 A canonical delivery/owner release 前不得
  写上述共享文件，但必须继续 tracker/input/tempPath/execute Audit-B 等无冲突项；A 释放后 C 可直接实施本 Amendment，
  无需另卡、重新 claim 或等待用户。

验收必须证明：双仓 mirror byte-identical；enum/validator strict shape；DHXY 单 remote exclusive callback 内一次
find+use；FOUND/cachePoint 对新 intent 均非法；四 terminal 映射与 UNKNOWN 零 false/零 retry；两个 Xiuluo caller
保持 startup probe、verify delay、fallback/retry 顺序不变。

无已批准业务差异；按 `696a12b0` 等价迁移。

<!-- TRUE_EOF: TURN-37 PARENT-AMENDMENT3-FREEZE1 BAG-ATOMIC-FIND+USE OPTION-A FIND_AND_USE_TASK_PAGE SINGLE-EXCLUSIVE UNKNOWN-UPWARD FIXED-WRITESET WAIT-A-PROTOCOL-RELEASE CONTINUE-OTHER-AUDITS OWNER-C 2026-07-17T18:48:00-04:00 -->

## PARENT COMMUNICATION STALE / BAG COLLISION RELEASE RECONCILIATION - 2026-07-17T20:02:39-04:00

- TURN-35 已于 19:54 Parent Review #3=`0/0/0 PASSED` 并释放 A owner；本卡 Amendment #3 的共享 protocol
  写集碰撞门已解除，原 Freeze #1 中“WAIT-A-PROTOCOL-RELEASE”条件现已满足。
- C 连续两轮未 ACK 该释放消息，最新 19:58 STATUS EVENT 仍写 Bag 等 A，故标 `COMMUNICATION_STALE`；
  TURN-37 sole owner 与现有 Xiuluo WIP 保留，不标 `ACTIVE_STALE`。
- C 下一 heartbeat 必须具名 ACK 父级两条消息并按当前真实进度继续；这不是派卡、撤卡或要求重新 claim。
- pathing late-target-map upgrade 真缺口仍保持 proposal gate，禁止 stub、恒 null、第二 store 或业务语义替代。

<!-- TRUE_EOF: TURN-37 PARENT-COMMUNICATION-STALE TWO-ROUNDS-NO-RELEASE-ACK OWNER-C-PRESERVED ACTIVE-STALE-NO BAG-COLLISION-RELEASED AMENDMENT3-CAN-CONTINUE PATHING-GAP-PROPOSAL-STILL-REQUIRED 2026-07-17T20:02:39-04:00 -->

## PARENT COMMUNICATION RECOVERED / BAG AMENDMENT ACTIVE - 2026-07-17T20:04:30-04:00

- C 的 20:04 STATUS EVENT 已具名 ACK TURN-35 release，并开始 Amendment #3；该事件与上一条 stale 记录并发，
  现接受为有效恢复并清除 `COMMUNICATION_STALE`，不要求 C ACK 已被 supersede 的 stale inquiry。
- 双仓 Bag protocol enum/validator 已 byte-identical 落盘（Git blob `61b2b6c5`/`4990df0c`）；仍是 WIP，
  需闭合完整 fixed write set、Xiuluo 两 caller 与唯一 named test 后才能 canonical delivery。
- TURN-37 owner、UNKNOWN uncertainty-upward、single-exclusive find+use 与 pathing gap proposal gate 全部保持。

<!-- TRUE_EOF: TURN-37 PARENT-COMMUNICATION-RECOVERED CONCURRENT-ACK-ACCEPTED BAG-AMENDMENT3-ACTIVE PROTOCOL-ENUM-VALIDATOR-BYTE-IDENTICAL OWNER-C PATHING-PROPOSAL-GATE-PRESERVED 2026-07-17T20:04:30-04:00 -->

## EXTERNAL-C AMENDMENT #3 PROPOSAL SET (PATHING/PARK GAPS) - 2026-07-17T21:20:00-04:00

bag Amendment#3 FREEZE#1 全写集(9 code + 5 test，双仓 byte-identical)已完成。frozen-family 收尾已迁 gameStateUtil/progress/dialog-interest/prepared-consume/clearPathing/pathing-read-mirror/readyEvent-latest/currentWindowLabel/suppressUnknownCombatExit 等族(rawCurrent 12→4)。剩 4 rawCurrent 方法(parkAfterYield/tryTrackerShortcutWithPanel/scheduleAcceptObjectiveBackgroundParse/continueIfNavigationStillPathing)簇拥 4 处真 gap——按 hard-gap fence 一次性提交完整 typed API 提案，冻结前对应 call-site 保持 baseline 忠实结构(编译断裂 WIP)、绝不 stub/local-copy/second-store/poll-sleep。

### GAP#2 — pathing late-target-map upgrade
- **baseline**：`WindowRuntimeContext.upgradeActivePathingIntentTargetMap(String expectedIntentId, String targetMap, String source)→boolean`（`attachShortcutTargetMapUpgrade` 在 `objectiveParseFuture.thenAccept(...)` **异步后台线程回调**内，将已注册 UNTARGETED_TRACKER intent 的 target map 后置升级）。
- **gap**：(a)全仓无 client/service op；(b)turn request-scoped，不能从 detached future 后台线程发 turn。
- **提案(请父级定架构)**：三选一——(A) block-on-parse：先同步解析 objective 再注册 targeted intent（消除 late-upgrade，最简，但改并发时序）；(B) 新 typed op `CloudWholeTaskRuntimeLocalServiceClient.upgradePathingTargetMap(intentId, targetMap, source, timeout)→WholeTaskRuntimeOutcome`（保 late-upgrade 语义，但仍需解决"何时/何线程调用"——须在下一 foreground turn 携带而非后台）；(C) turn-native 若 objective 解析已同步则 drop late-upgrade。owner=既有 pathing register 链；negative：EXECUTED 外保守不升级；acceptance：升级仅影响 target map 不改 intentId/type。
- 关联：`registerTrackerShortcutPathingIntent` 的 `markPathingStarted(intent)` 本身可迁 `runtimeClient.registerPathing(TurnPathingIntent,...)`(非 gap，FiveRing:945 先例)，但其返回的 WindowPathingIntent 被 attach 消费——耦合本 gap，故整 cluster 待冻结。

### GAP#3 — intent-filtered pathing-terminal-or-prepared-route await
- **baseline**：`WindowReadyEventBus.awaitNewerPathingTerminalOrPreparedRoute(windowId, expectedIntentId, expectedSourcePrefix, expectedTargetMapName, afterSequence, timeoutMs)→Optional<WindowReadyEvent>`（parkAfterYield WAIT_TARGET_PATHING_TERMINAL 分支：阻塞 await PATHING_TERMINAL|PREPARED_ACTION_READY，但经 `findNewerPathingTerminalOrPreparedRoute` 按 intentId/sourcePrefix/targetMapName **过滤**——不匹配的 pathing-terminal 继续等待）。
- **gap**：`CloudWholeTaskReadyEventState.awaitNewer(context, EnumSet types, afterSeq, timeout)` 只按 type 唤醒、**无 intent-specific 过滤**；generic awaitNewer 会被不匹配 intent 的 pathing-terminal 误唤醒。
- **提案**：新 typed op `CloudWholeTaskReadyEventState.awaitNewerPathingTerminalOrPreparedRoute(TaskExecutionContext context, String expectedIntentId, String expectedSourcePrefix, String expectedTargetMapName, long afterSequence, long timeoutMs)→Optional<WindowReadyEvent>`（38A-F owner，复用既有 await 锁/control-wake/STOP-checkpoint 机制 + 内部 intent 过滤，与 baseline `findNewerPathingTerminalOrPreparedRoute` 逐字等价）。negative：不匹配 intent 不唤醒；timeout/interrupt/control-wake→empty。DAG：仅依赖既有 38A-F await 基座。
- 非 gap 分支：parkAfterYield 的 `awaitNewer(windowId, wakeTypes, afterSeq, timeout)`→`readyEventState.awaitNewer(context, wakeTypes, afterSeq, timeout)`(可迁)；`runtime.getWindowId()`→`context.getWindowId()`。

### GAP#4 — fresh prepared-route on pathing terminal
- **baseline**：`WindowRuntimeContext.freshPreparedRouteActionForPathingTerminal(WindowPathingSnapshot terminalSnapshot, long maxAgeMs)→PreparedDialogAction`（continueIfNavigationStillPathing stopped-away：取当前 prepared action 当且仅当 op==ROUTE_TRANSFER + window-match + fresh(maxAgeMs)）。
- **可能可组合**：`CloudDialogPreparedActionState.peek(context, DialogOperation.ROUTE_TRANSFER, expectedTargetKeyword?, reason, allowClearedRouteIntent)` 已做 op+window 过滤；缺 `maxAgeMs` freshness。**请父级裁定**：(A) peek 结果 Cloud-side 加 `action.verifiedWithin(now, maxAgeMs)` freshness 过滤(若 PreparedDialogAction 携时间戳)=非 gap；(B) 若须原子=新 peek 重载带 maxAgeMs。
- 非 gap：同方法内 getPathingSnapshot/getActivePathingIntent→`cloudNavigationPathingState` 镜像；clearPathingSignal×3→`runtimeClient.clearPathing`；getPreparedDialogAction→`preparedActionState.peekBoundSlot`；getNativeBinding/getWindowId→context/metadata。

### GAP#5(架构问题) — background async objective parse
- **baseline**：`scheduleAcceptObjectiveBackgroundParse` 用 `CompletableFuture.supplyAsync(()->windowTaskContextHolder.callWith(runtime,...))` + `taskExecutionContextHolder.callWith(context,...)` 在**后台线程**异步解析 objective/tracker，结果经 future 供后续 phase 消费。
- **gap**：turn request-scoped，后台线程不能发 turn（capture/OCR/local-service）。这是设计问题非单 API——**请父级定架构**：(A) 改为 foreground 同步解析(消除 background future，可能增单 turn 延迟)；(B) Cloud 侧 typed async-parse job 机制；(C) 其它。此 gap 同时决定 GAP#2 的 late-upgrade 是否还需要(若同步解析则 objective map 注册时即知)。

### 非 gap 剩余(冻结后即迁)
- execute() `taskTransactionRunner.forceReleaseTurn`(419)/`taskTransactionRunner.run`(509)→`CloudTaskTurnCoordination`(A WubeiTask tryRun 4-arg `name/READY_TO_CONTINUE/CONTINUE_CHAIN/Supplier<TaskTransactionResult>` 先例，非 gap)。
- 请父级冻结上述 GAP#2/#3/#4/#5 的 typed API/架构决策为 Amendment#3 后续 FREEZE，C 依冻结实施剩余 4 方法 + execute turn-coordination，整卡闭合建 XiuluoWholeTaskTurnContractTest→canonical whole-card delivery。

<!-- TRUE_EOF: TURN-37 EXTERNAL-C AMENDMENT3-PROPOSAL-SET PATHING-PARK-GAPS GAP2-UPGRADE GAP3-INTENT-AWAIT GAP4-FRESH-ROUTE GAP5-BACKGROUND-ASYNC BAG-FREEZE1-COMPLETE RAWCURRENT-12-TO-4 AWAIT-PARENT-FREEZE 2026-07-17T21:20:00-04:00 -->

## PARENT AMENDMENT #3 FREEZE #2 - PATHING/PARK CLOSURE - 2026-07-17T21:28:00-04:00

父级已逐项对照 `696a12b0`、现有 Cloud turn context/foundation 与真实调用链审计。结论：GAP#2、GAP#3
为需闭合的合同缺口；GAP#4 可由现有唯一状态 owner 等价组合；GAP#5 不是架构缺口。本卡不存在待用户业务
语义选择，External C 可按以下唯一合同继续整卡。

### GAP#2 - 冻结新 typed late target-map upgrade

- 新增唯一 `TurnLocalOperation.WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP`；参数严格复用现有
  `TurnWholeTaskRuntimeArguments.intentId/targetMapName/source`，三者 nonblank，禁止新增 DTO 字段、第二 store、
  retry 或 foreground 同步 parse。DHXY exact-bound executor 只调用一次 baseline
  `WindowRuntimeContext.upgradeActivePathingIntentTargetMap(intentId,targetMapName,source)`，返回 typed boolean。
- Cloud client 增 `upgradePathingTargetMap(intentId,targetMapName,source,timeout)`。`EXECUTED` 的 boolean 原样表示
  upgraded/合法 no-op；`UNKNOWN` 必须 uncertainty-upward、零自动重发；`NOT_EXECUTED` 不得伪装业务 false，
  必须沿原 objective future 消费链向上失败；`STOPPED` 在绑定 context 上立即 checkpoint/unwind。
- 保留 baseline late-upgrade 并发时序。后台 dependent stage 必须在
  `taskExecutionContextHolder.callWith(context, ...)` 内发 typed turn；返回 stage 必须替换/串入 round 持有的
  `objectiveParseFuture`，确保后续 `waitForBackgroundObjectiveResult` 消费并传播失败，禁止忽略 `thenAccept`
  返回值或用 `exceptionally(... -> null)` 吞 terminal。允许在 `XiuluoRoundContext` 增一个只替换 future、保持
  其它字段逐值不变的方法；不得重置 phase/timer/retry/pathing identity。

### GAP#3 - 冻结 exact intent/route filtered await

- 在唯一 owner `CloudWholeTaskReadyEventState` 增
  `awaitNewerPathingTerminalOrPreparedRoute(context,expectedIntentId,expectedSourcePrefix,expectedTargetMapName,afterSequence,timeoutMs)`；
  复用现有 condition、slot key、checkpoint、sequence 与 timeout，不新建 bus/store/poll/sleep/TTL。
- 过滤逐项保持 baseline：terminal 必须 newer、`ARRIVED|STOPPED_AWAY` 且 intentId 精确匹配；prepared 必须
  newer、`ROUTE_TRANSFER` 且 targetKeyword 精确匹配 expectedTargetMapName；两者同时存在取 sequence 更新者。
  不匹配事件只 signal 后重新检查并继续 park，不得提前返回。`expectedSourcePrefix` 保留签名/诊断兼容，
  不擅自收窄 baseline 实际 intentId 判断。

### GAP#4/#5 - 复用现有 owner，不新增 API

- GAP#4 选择组合方案：只读 `preparedActionState.peekBoundSlot(context,reason)`，再严格检查
  `ROUTE_TRANSFER`、exact binding、`verifiedWithin(now,maxAgeMs)`，并要求 prepared intentId 或 targetKeyword
  与 active intent / terminal snapshot intent 任一精确关联。仅 op+fresh 不足；关联失败按 baseline 返回 null，
  `STOPPED_AWAY` 随后走原 clear 分支。禁止新 peek overload、第二状态或放宽 target/intent 关联。
- GAP#5 判定为非缺口：`TaskExecutionContextHolder.callWith(context,...)` 已将 exact turn context 绑定到后台线程；
  `LegacyTaskExecutionTurnContextProvider.currentContext()` 从该 holder 取 invocation context，bound
  `TurnGameClient.currentExactContext()` 再做 exact-match。Wubei 同类后台 typed tracker read 已是有效先例。
  因此 objective/tracker parse 保持 `CompletableFuture` 异步时序，只删除 local
  `windowTaskContextHolder.callWith(runtime,...)`，统一用 task context holder；禁止 foreground 同步化或 Cloud async job。

### 固定传递写集与验收

- 双仓 mirror：`TurnLocalOperation.java`、`TurnProtocolValidator.java`、`TurnCoreProtocolGoldenJsonTest.java`、
  `TurnProtocolValidatorContractTest.java`；`TurnWholeTaskRuntimeArguments` 现有字段足够，不得为本 amendment 改 shape。
- DHXY：`LocalServiceStepDispatcher.java`、`WholeTaskRuntimeLocalOperationExecutor.java`、
  `LocalServiceStepDispatcherContractTest.java`（及既有 executor coverage，如当前测试结构要求）。
- Cloud：`CloudWholeTaskRuntimeLocalServiceClient.java`、`CloudWholeTaskRuntimeLocalServiceClientTest.java`、
  `CloudWholeTaskReadyEventState.java`、`CloudWholeTaskFoundationContractTest.java`。
- 本卡：`XiuluoTaskV2.java`、必要的 `XiuluoRoundContext.java`、唯一
  `XiuluoWholeTaskTurnContractTest.java`。测试必须覆盖 strict validator/result shape、upgrade true/false 与四 terminal、
  dependent future 不吞失败、unrelated terminal/prepared 不误唤醒、newest selection、timeout/interrupt/stop、
  fresh-route exact binding+freshness+active/terminal intent-or-target 四关联矩阵，以及异步 exact context。

无已批准业务差异；按 `696a12b0` 等价迁移。External C 下一 heartbeat 以
`ack_parent_message=PARENT-TURN37-AMENDMENT3-FREEZE2-2128` 具名回执后继续，无需重新 claim。

<!-- TRUE_EOF: TURN-37 PARENT-AMENDMENT3-FREEZE2 PATHING-PARK-CLOSURE GAP2-TYPED-UPGRADE GAP3-FILTERED-AWAIT GAP4-EXISTING-STATE-COMPOSITION GAP5-NOT-GAP ASYNC-CONTEXT-BOUND TRACKED-FUTURE UNKNOWN-UPWARD OWNER-C-CONTINUE MSG=PARENT-TURN37-AMENDMENT3-FREEZE2-2128 2026-07-17T21:28:00-04:00 -->

## PARENT FREEZE #2 ACK / GAP#2 FOUNDATION WIP RECONCILIATION - 2026-07-17T21:38:00-04:00

- C 的 21:36 STATUS EVENT 已具名 ACK `PARENT-TURN37-AMENDMENT3-FREEZE2-2128`，通信正常；状态转为
  `SOURCE_ACTIVE / AMENDMENT #3 FREEZE #2 IMPLEMENTING`，sole owner 不变。
- 父级实盘复核双仓 `TurnLocalOperation.java` SHA-256=`85ffa0099bf4e7d28ab9feab06e6a86dd52c860715fd64e5234d1c005bb12ffe`、
  `TurnProtocolValidator.java` SHA-256=`60da55b23e263cab5035464bea878401ec819cd24b8770965fae007cd224d06b`，
  均 byte-identical；新 op 的 only-whole-task grouping、nonblank intentId/targetMap 与 strict allowed-field 集在位。
- 当前仅是 GAP#2 protocol foundation WIP，不是 whole-card delivery/source review/build passed；仍须闭合 executor/
  dispatcher/client/tests、GAP#3、GAP#4/#5、四个 Xiuluo 方法与唯一整卡 test。C active writer，本轮不运行 Maven。

<!-- TRUE_EOF: TURN-37 PARENT-FREEZE2-ACK-ACCEPTED GAP2-PROTOCOL-FOUNDATION-WIP MIRROR-BYTE-IDENTICAL OP-SHA256=85ffa009 VALIDATOR-SHA256=60da55b2 OWNER-C SOURCE-ACTIVE NOT-DELIVERY NO-MAVEN 2026-07-17T21:38:00-04:00 -->

## PARENT GAP#2 CODE-PATH WIP RECONCILIATION - 2026-07-17T21:43:00-04:00

- C 已完成 GAP#2 production code path。父级实盘核对 DHXY
  `WholeTaskRuntimeLocalOperationExecutor.java` SHA-256=`3820bde57b1f2e5b0953ada604a27de0ee558cec614ab2703f1bfa4a8e472fec`、
  `LocalServiceStepDispatcher.java` SHA-256=`546301ba32fa72acc4a1eb57406ca526dfda8401b1c6295298fc974ae5e04720`；
  dispatcher 只路由到 whole-task executor，executor 直接一次调用 baseline atomic upgrade。
- Cloud `CloudWholeTaskRuntimeLocalServiceClient.java` SHA-256=
  `59bf77e867f90ddc64fd23d7a55e4179ea400a2e38ee8a096256084c2697cf96`；client 只构造一个
  `WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP` typed call，复用 intentId/targetMap/source，零 retry/store。
- GAP#2 tests、四 terminal/dependent-future caller 尚未闭合；GAP#3/#4/#5 与唯一 whole-card test 仍待。
  当前继续是 protected WIP，不是 canonical delivery/source review/build passed；C active writer，本轮不运行 Maven。

<!-- TRUE_EOF: TURN-37 PARENT-GAP2-CODE-PATH-WIP-RECONCILED EXECUTOR-SHA256=3820bde5 DISPATCHER-SHA256=546301ba CLIENT-SHA256=59bf77e8 TESTS-PENDING OWNER-C SOURCE-ACTIVE NOT-DELIVERY NO-MAVEN 2026-07-17T21:43:00-04:00 -->

## PARENT GAP#2 DISPATCHER TEST CONTRACT CLARIFICATION - 2026-07-17T21:49:00-04:00

- C 已完成 Cloud client upgrade true/false test，并报告 DHXY
  `LocalServiceStepDispatcherContractTest.validCall` 只覆盖 9 个旧 permanent local-service operations、同时
  `TurnLocalOperation.values().length==9` 已被现有 28-operation enum 打破。父级核对确认断裂真实。
- 该问题不要求为 28 个 operation 猜造 valid payload，也不构成待用户业务语义选择。首个 dispatcher test 的
  实际合同仅是 9 个 bag/ui/give/quest permanent local-service operations 的 owner/queue/frame 闭集；安全修复为
  显式定义并遍历这 9 个 operation，保留现有逐 owner/queue/frame 断言，移除 enum 总长度等于 9 的 stale 假设。
- 新 `WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP` 按本文件 Freeze #2 既定写集，在现有 exact-bound runtime fixture
  增专项 dispatcher route test：matched intent 升级 true，mismatch/stale false，均只到 whole-task executor 且
  dispatcher input-worker 为零。不得补 28-operation 大 switch、猜 payload、扩大 production 或增加业务差异。
- 已向 C 发 `PARENT-TURN37-GAP2-DISPATCHER-TEST-CLARIFICATION-2149`；下一 heartbeat 具名 ACK 后继续。
  owner 不变，当前仍是 WIP，非 delivery/source review/build passed。

<!-- TRUE_EOF: TURN-37 PARENT-GAP2-DISPATCHER-TEST-CONTRACT-CLARIFIED EXPLICIT-9-OP-PERMANENT-SET UPGRADE-EXACT-BOUND-ROUTE-TEST NO-28-PAYLOAD-GUESS OWNER-C SOURCE-ACTIVE MSG=PARENT-TURN37-GAP2-DISPATCHER-TEST-CLARIFICATION-2149 NOT-DELIVERY NO-MAVEN 2026-07-17T21:49:00-04:00 -->

## PARENT GAP#2 VALIDATOR TEST WIP RECONCILIATION - 2026-07-17T21:55:00-04:00

- C 的 21:54 STATUS EVENT 报告双仓 `TurnProtocolValidatorContractTest` 已补 upgrade valid、缺 intentId、缺
  targetMapName 与 extra-field rejection；父级实盘 SHA-256 均为
  `c73bd7cdf88248732573849aa3554b0dfa249d3a5eb8c9080e69245eb106e03e`，byte-identical。
- GAP#2 validator/client 测试有真实进展，但 GAP#2 test closure 仍以 21:49 dispatcher test 澄清为门：C 下一拍
  必须 ACK 并闭合显式 9-operation permanent-service 集合及 upgrade exact-bound route true/false/zero-input test。
- owner 不变；GAP#3 已可并行继续。当前非 canonical delivery/source review/build passed，未运行 Maven。

<!-- TRUE_EOF: TURN-37 PARENT-GAP2-VALIDATOR-TEST-WIP-RECONCILED SHA256=c73bd7cd MIRROR-BYTE-IDENTICAL DISPATCHER-TEST-ACK-AND-REPAIR-PENDING OWNER-C SOURCE-ACTIVE NOT-DELIVERY NO-MAVEN 2026-07-17T21:55:00-04:00 -->

## PARENT GAP#2 DISPATCHER TEST REVIEW #1 - 2026-07-17T22:03:00-04:00

结论：`P0/P1/P2=0/1/0 / WIP TEST REPAIR REQUIRED`。C 已具名 ACK 21:49 合同并正确修复 9-operation
显式闭集，但新 upgrade route test 尚不能通过：

- **P1** `LocalServiceStepDispatcherContractTest.wholeTaskPathingUpgradeMatchesActiveIntentAndUpgradesTargetMapWithoutInputWorker`
  当前断言 `fixture.totalServiceCalls()==1`。该方法仅汇总 `bagService/uiService/giveService/questService`，
  whole-task executor 不在计数内；register+upgrade 均不会调用四个 permanent adapters，真实值固定为 0。
- 返修条件：断言 permanent-service count=0；matched true + exact-bound active intent targetMap mutation 证明路由；
  mismatch false + targetMap 不变证明 stale no-op；第二次调用后再次确认 exclusive/nestedExclusive 均为 0。
  禁止新增假计数器或改 production 迎合测试。
- production 与其它 GAP#2 tests 冻结。已发
  `PARENT-TURN37-GAP2-DISPATCHER-TEST-P1-2203`；owner 不变，修复前 GAP#2 不得标 complete。
- 用户只读基线工作树 dirty 计数由 66 变 67，新增
  `XiuluoMaintenanceBestEffortWiringTest.java` 来自用户/外部活动；父级未写入、未切分支。

<!-- TRUE_EOF: TURN-37 PARENT-GAP2-DISPATCHER-TEST-REVIEW1 BLOCKED P0P1P2=0-1-0 TOTALSERVICECALLS-ASSERTION-IMPOSSIBLE EXPECT-0 RUNTIME-MUTATION-ROUTE-EVIDENCE ZERO-INPUT-BOTH MSG=PARENT-TURN37-GAP2-DISPATCHER-TEST-P1-2203 OWNER-C NO-MAVEN 2026-07-17T22:03:00-04:00 -->

## PARENT GAP#3 CODE WIP RECONCILIATION - 2026-07-17T22:09:00-04:00

- C 已落盘 `CloudWholeTaskReadyEventState.awaitNewerPathingTerminalOrPreparedRoute(...)`；父级对照
  `696a12b0 WindowReadyEventBus` 实盘逐项核对。Cloud 文件 SHA-256=
  `24d0e7bfd29192c8ac4fc74a6527250a9edf42d60c19915dc9465e56ce2a42d0`。
- 当前 code shape 等价：复用现有 exact slot、`waitLock/newerPublished` condition、stop checkpoint、sequence 与
  timeout；terminal 仅接收 newer `ARRIVED|STOPPED_AWAY` 且 exact intentId；prepared 仅接收 newer
  `ROUTE_TRANSFER` 且 exact nonblank target map；两者并存取 sequence 更新者。未新增 bus/store/poll/sleep/TTL。
- GAP#3 foundation test 尚未落盘，故当前仅 WIP、非 review pass。GAP#2 dispatcher-test P1 消息在 C 22:08
  event 落盘时尚未具名 ACK，test SHA 仍 `c84318c1...`，这是第 1 轮漏回执；既有返修门保持。
- owner 不变，可继续 GAP#3 test；未运行 Maven/runtime/UI/capture/input。

<!-- TRUE_EOF: TURN-37 PARENT-GAP3-CODE-WIP-RECONCILED SHA256=24d0e7bf BASELINE-EQUIVALENT EXACT-INTENT-ROUTE-FILTER CONDITION-CHECKPOINT NO-SECOND-STORE FOUNDATION-TEST-PENDING GAP2-P1-ACK-MISS-ROUND1 OWNER-C NO-MAVEN 2026-07-17T22:09:00-04:00 -->

## PARENT GAP#2 DISPATCHER TEST REVIEW #2 - 2026-07-17T22:18:00-04:00

结论：`P0/P1/P2=0/0/0 / PASSED`（限 GAP#2 dispatcher test 返修范围）。

- C 22:14 已具名 ACK `PARENT-TURN37-GAP2-DISPATCHER-TEST-P1-2203`，通信恢复；父级实盘复核 test
  SHA-256=`fda5aafdcb0193c274e7bbe85fbb68b6c1fb2b372edf96ac459264a484c7e53d`。
- matched 分支现断言四 permanent adapters count=0、boolean=true、exact-bound active intent targetMap 变更；
  mismatch 分支断言 boolean=false、targetMap 不变；两次调用后均断言 exclusive/nestedExclusive=0。
  未新增假计数器，production 未漂移，上一轮唯一 P1 闭合。
- GAP#2 production+tests 至此完整 WIP closure。TURN-37 整卡仍未 delivery/review/build；GAP#3 foundation test、
  GAP#4/#5 caller 与唯一 whole-card test 继续由 C 收口。

<!-- TRUE_EOF: TURN-37 PARENT-GAP2-DISPATCHER-TEST-REVIEW2 PASSED P0P1P2=0-0-0 SHA256=fda5aafd TOTALSERVICECALLS-0 RUNTIME-MUTATION-ROUTE ZERO-INPUT-BOTH COMMUNICATION-RECOVERED GAP2-COMPLETE OWNER-C WHOLECARD-NOT-DELIVERED NO-MAVEN 2026-07-17T22:18:00-04:00 -->

## PARENT GAP#3 FOUNDATION TEST REVIEW #1 - 2026-07-17T22:27:00-04:00

结论：`P0/P1/P2=0/1/0 / BLOCKED / REPAIR REQUIRED`。

- GAP#3 production code 的 22:09 baseline-equivalent 结论保持；本轮 finding 仅针对
  `CloudWholeTaskFoundationContractTest.awaitNewerPathingTerminalOrPreparedRouteWakesOnlyForExactIntentOrMatchingRoute`
  （SHA-256=`dd2c8a83b22d0e4b1d9732e166cd2f888123245d6326c9d5930dc8c788910db5`）。
- **P1 test matrix 未满足 Freeze #2**：当前只有 unrelated terminal、matched terminal、matching route、zero-timeout。
  缺 wrong-target/non-route prepared 不误唤醒；缺 terminal 较新/prepared 较新的双向 newest selection；缺 exact
  await 的 parked-thread interrupt bounded empty + flag preservation；缺 stopped harness 的 typed stop exception。
  exact method 自带独立 wait/checkpoint/interrupt loop，generic await 的测试不能覆盖其实现分支。
- 固定返修仅补上述 cases，保留现有断言，不改 production。已发
  `PARENT-TURN37-GAP3-FOUNDATION-TEST-P1-2227`；修复复审前 GAP#3 不得标 complete，GAP#2 complete 不回退。

<!-- TRUE_EOF: TURN-37 PARENT-GAP3-FOUNDATION-TEST-REVIEW1 BLOCKED P0P1P2=0-1-0 TEST-SHA256=dd2c8a83 MISSING-UNRELATED-PREPARED NEWEST-TERMINAL-AND-PREPARED INTERRUPT STOP MSG=PARENT-TURN37-GAP3-FOUNDATION-TEST-P1-2227 OWNER-C GAP2-STAYS-COMPLETE NO-MAVEN 2026-07-17T22:27:00-04:00 -->

## PARENT GAP#3 FOUNDATION TEST REVIEW #2 - 2026-07-17T22:39:00-04:00

结论：`P0/P1/P2=0/0/0 / PASSED`（限 GAP#3 production + foundation-test 范围）。

- C 22:34 已具名 ACK `PARENT-TURN37-GAP3-FOUNDATION-TEST-P1-2227`；父级实盘复核 test SHA-256=
  `9c35897e28f0e5299b5e8addd3c29f1afbf4d04c80b9884192b1ff327fb0af1c`。
- wrong-target route 与 non-route prepared 均使用正 timeout 并返回 empty；terminal 较新与 prepared 较新两序
  均断言 sequence winner；parked exact await 的 interrupt 在 5 秒内返回 empty 且保留 interrupt flag；stopped
  harness 调 exact await 抛 `TaskStopRequestedException`。上一轮唯一 P1 闭合。
- GAP#3 production 保持 22:09 的 `696a12b0` 等价结论，GAP#3 至此 complete。C 已另行迁移
  `parkAfterYield` 与 `scheduleAcceptObjectiveBackgroundParse`；该 WIP 尚非整卡 delivery/source review/build。
- 同一卡继续剩余 2 个 Xiuluo 方法、冻结字段/import 清理与唯一 `XiuluoWholeTaskTurnContractTest`；C active
  Java writer，未运行 Maven/runtime/UI/capture/input。

<!-- TRUE_EOF: TURN-37 PARENT-GAP3-FOUNDATION-TEST-REVIEW2 PASSED P0P1P2=0-0-0 TEST-SHA256=9c35897e WRONG-PREPARED NEWEST-BOTH INTERRUPT-FLAG TYPED-STOP GAP3-COMPLETE OWNER-C WHOLECARD-NOT-DELIVERED NO-MAVEN 2026-07-17T22:39:00-04:00 -->

## PARENT TRYTRACKERSHORTCUT WIP RECONCILIATION - 2026-07-17T22:41:00-04:00

- C 22:40 STATUS EVENT 报告 `tryTrackerShortcutWithPanel` cluster 已迁移 register + async upgrade；父级实盘
  SHA-256=`1de14739457ac45d2121fe87f8672932cd64bad41ed1884f272811e88dd15a87`，`registerPathing` 与
  `upgradePathingTargetMap` 在位，`markPathingStarted`/`upgradeActivePathingIntentTargetMap` 已归零。
- 当前 3/4 方法落盘，`rawCurrent()` 仅剩 `continueIfNavigationStillPathing` 一处。此记录只确认受保护 WIP
  字节与真实剩余面，不构成该 cluster 或 whole-card source review；GAP#3 Review #2 passed 结论不变。
- C 22:40 事件称未见 22:39 PASS 消息，属于并发落盘后的第 1 轮漏回执；既有
  `PARENT-TURN37-GAP3-FOUNDATION-TEST-PASS-2239` 仍待下一拍 ACK，不标 `COMMUNICATION_STALE`。
- C active Java writer；未运行 Maven/runtime/UI/capture/input。

<!-- TRUE_EOF: TURN-37 PARENT-TRYTRACKERSHORTCUT-WIP-RECONCILED SHA256=1de14739 REGISTER+ASYNC-UPGRADE 3-OF-4-METHODS RAWCURRENT-1 PASS-ACK-MISS-ROUND1 OWNER-C WHOLECARD-NOT-DELIVERED NO-MAVEN 2026-07-17T22:41:00-04:00 -->

## PARENT PRODUCTION 4/4 WIP RECONCILIATION - 2026-07-17T22:53:00-04:00

- C 22:49 已具名 ACK `PARENT-TURN37-GAP3-FOUNDATION-TEST-PASS-2239`，上一轮并发漏读闭合，通信正常。
- `continueIfNavigationStillPathing` GAP#4 已迁移到 Cloud pathing/prepared owners；父级实盘 SHA-256=
  `2d4bc1a02f6be96dbda3cd3c8bc5049331aefe17ba6e3458d93d56502ac97c51`。当前 `rawCurrent()`、
  `markPathingStarted`、`upgradeActivePathingIntentTargetMap`、`clearPathingSignal` 及 frozen runtime-owner 字段
  在 Xiuluo production 均归零；4/4 固定方法已落盘。
- 当前只确认 production WIP 与真实剩余面；唯一 `XiuluoWholeTaskTurnContractTest` 尚未交付，故不构成
  whole-card source review/build passed。C sole owner 继续完成测试与 canonical delivery。
- C active Java writer；未运行 Maven/runtime/UI/capture/input。

<!-- TRUE_EOF: TURN-37 PARENT-PRODUCTION-4-OF-4-WIP-RECONCILED SHA256=2d4bc1a0 CONTINUEIFNAV-GAP4 RAWCURRENT-ZERO FROZEN-RUNTIME-OWNER-ZERO COMMUNICATION-RECOVERED WHOLECARD-TEST-PENDING OWNER-C NOT-DELIVERED NO-MAVEN 2026-07-17T22:53:00-04:00 -->

## EXTERNAL-C WHOLE-CARD CANONICAL DELIVERY - 2026-07-17T23:02:00-04:00

TURN-37（修罗 XiuluoTaskV2 整卡 HTTPS-turn 迁移）**交付源审 + build**。sole owner C。零 Git mutation；blob=git hash-object（git-blob SHA-1）；双仓 byte-identical 由同 blob 证明。

### A. 迁移完成度（production XiuluoTaskV2 `15a4a5f4` / SHA-256 `2d4bc1a0`，4483L）
- `rawCurrent()` = **0**；`markPathingStarted`/`upgradeActivePathingIntentTargetMap`/`clearPathingSignal`/`freshPreparedRouteActionForPathingTerminal(local)`/`getActivePathingIntent(local)`/`getPathingSnapshot(local)` 本地 runtime-owner 调用 = **0**；冻结字段 `windowTaskContextHolder`+`windowReadyEventBus` 与 import `WindowRuntimeContext`/`WindowTaskContextHolder`/`WindowReadyEventBus` 已删除（`taskExecutionContextHolder` 保留，TaskCheckpoint stop-check 仍用）。
- 逐族迁移（全部 typed op / 只读镜像，无 wrapper/second-store/poll-sleep/shim）：gameStateUtil（isSameMapName→纯 `canonicalMapName`+MapNameCanonicalizer；isNearCoordinate→`runtimeClient.isNearCoordinate` EXECUTED→bool/非 EXECUTED→保守 false；recordMovementIntent→typed op）、progress→`updateProgress`、dialog-interest→`updateDialogInterest`、prepared-consume→`consumeValidated`/`peekBoundSlot`/`clear`、pathing-clear→`clearPathing`、pathing-read→`CloudNavigationPathingState` 只读镜像、readyEvent（latest/currentSequence/awaitNewer/**awaitNewerPathingTerminalOrPreparedRoute**）、currentWindowLabel（context+metadata）、turn-coordination（`cloudTaskTurnCoordination.run/forceRelease`）、bag（→`findAndUseReturnItem`/FIND_AND_USE_TASK_PAGE）、tracker-shortcut（register→`registerPathing`，async upgrade→`upgradePathingTargetMap`）。

### B. GAP 处置
- **GAP#2** `WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP`（双仓协议+cloud client+DHXY executor/dispatcher+tests）：Review#2 **PASSED**（22:18，SHA `fda5aafd`）。
- **GAP#3** `awaitNewerPathingTerminalOrPreparedRoute`（CloudWholeTaskReadyEventState + foundation test）：Review#2 **PASSED**（22:39，test SHA `9c35897e`；production baseline-equivalent `24d0e7bf`）。
- **GAP#4** `freshPreparedRouteActionForPathingTerminal` Cloud 组合（continueIfNav）：`peekBoundSlot(reason)` + `getOperation()==ROUTE_TRANSFER` + `verifiedWithin(now,maxAge)` + **`preparedRouteAssociatedWith(active OR terminal intent)`**（exact nonblank intentId 或经 isSameMapName 的 nonblank target-map）。Freeze#2 明定：仅 op+fresh 不足，须关联；关联失败/blank-blank/stale/non-ROUTE_TRANSFER → 保守 null。已在 whole-card test 直驱覆盖。
- **GAP#5** 判非 gap：`scheduleAcceptObjectiveBackgroundParse` 后台 async 保留 `taskExecutionContextHolder.callWith(context,...)`，LegacyTaskExecutionTurnContextProvider 将 context 绑后台线程（无新 gap API）。
- **bag Amendment#3 FREEZE#1**（FIND_AND_USE_TASK_PAGE 复用 executeReturnItem，单排他原子 find+use）：**PASSED**。

### C. 唯一 whole-card test `XiuluoWholeTaskTurnContractTest` `168240e0`（522L，7 @Test）
沿用修罗包既有反射惯例（不改 production 可见性）+ FiveRing/Wubei PASSED 先例的 ScriptedCommandPort/组件直驱惯例，全部驱动真实 production caller/state owner，无 wrapper/substitute：
- **GAP#4 关联栅栏**（新代码核心）：exact-intentId hit / normalized-target hit；blank-blank / unassociated-fresh / stale / non-ROUTE_TRANSFER / absent-slot 全拒（保守 null）。
- **family-C** `isNearCoordinate`：EXECUTED true/false honored；非 EXECUTED（FAILED）保守 not-near，一 op 无 re-issue。
- **组件直驱**（迁移 caller 依赖的 production owner 契约）：prepared-slot exact tenant/user/device/window 栅栏（peek 非破坏）、route-transfer cleared-intent 恢复规则、pathing 只读镜像 absent-fact→idle NONE。
- **TASK**：stop()→BotStatus IDLE。
- **诚实 scope-out（javadoc 明载）**：完整 `execute()` round/phase loop 与 `continueIfNavigationStillPathing` 终态消费分支需整套 null 协作者图（navigation/npcClick/dialog/playerState/taskMaintenance/autoCombat/taskTrackerPanel/teamReturn/returnItemPrescan/automationMetrics）驱动，按 FiveRing/Wubei 先例的 whole-task gate 惯例不以替身伪造，drivability 提交父级裁定；GAP#4 关联逻辑（continueIfNav 的 fresh-route 延迟判据依赖）已直驱覆盖。注：修罗 `resolveExecutionContext(null)` 构造 legacy debug context（非 turn-native，无 turn client），**不**像 Wubei 干净 fail-fast，故 execute(null) 未作契约断言。

### D. 完整写集清单（build gate）
**Cloud (dhxy-cloud-brain)**：
- prod `XiuluoTaskV2.java` `15a4a5f4`
- test `XiuluoWholeTaskTurnContractTest.java` `168240e0`（NEW）
- `CloudWholeTaskRuntimeLocalServiceClient.java` `5b0c1809`、`CloudBagLocalServiceClient.java` `2e037b90`、`CloudWholeTaskReadyEventState.java` `71f09935`
- tests `CloudWholeTaskRuntimeLocalServiceClientTest.java` `045e8193`、`CloudWholeTaskFoundationContractTest.java` `daeb3886`、`ReturnItemPrescanTurnContractTest.java` `dce4e983`

**DHXY-cr271**：
- `WholeTaskRuntimeLocalOperationExecutor.java` `07516f27`、`BagLocalOperationExecutor.java` `22dfbe8c`、`LocalServiceStepDispatcher.java` `4861e856`
- bag prod `TurnBagOperationArguments.java` `61b2b6c5`、`BagReturnItemMacroIntent.java` `1d164ca0`、`BagService.java` `8e1f18b3`
- tests `LocalServiceStepDispatcherContractTest.java` `19c9a670`、`BagLocalOperationExecutorContractTest.java` `37b9b390`

**双仓 byte-identical**（同 blob 两仓各一份）：
- `TurnLocalOperation.java` `3d1d99f8`、`TurnProtocolValidator.java` `79d34088`、`TurnCoreProtocolGoldenJsonTest.java` `973e9b7f`、`TurnProtocolValidatorContractTest.java` `161b24e8`

### E. 依据 + 纪律
- Amendment#3 FREEZE#1（bag FIND_AND_USE）+ FREEZE#2（GAP#3 filtered-await exact loop / GAP#4 op+fresh+association / GAP#2/#5 dependent-stage exact-context）。
- 验证：全新/改动文件单文件 `javac -proc:none -implicit:none -sourcepath "" -cp ""` 零纯语法错。**本轮未运行 Maven**（C sole active writer；A/D idle；按既有纪律与 GAP#2/#3 交付方式一致，build 由 build gate 权威编译）；无 runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读；零 Git mutation；无自批、未建 reviewer。
- 请求：**whole-card source review + build**。GAP#2/#3/bag 子交付已 PASSED，本次新增源审面 = XiuluoTaskV2 整卡装配 + 唯一 whole-card test。

<!-- TRUE_EOF: TURN-37 EXTERNAL-C WHOLE-CARD-CANONICAL-DELIVERY PROD=15a4a5f4 TEST=168240e0 RAWCURRENT-0 FROZEN-FIELDS-REMOVED GAP2-GAP3-BAG-PASSED GAP4-ASSOC-FENCE GAP5-NONGAP SCOPE-OUT-EXECUTE-LOOP-DISCLOSED OWNER-C REQUEST-SOURCE-REVIEW+BUILD NO-MAVEN 2026-07-17T23:02:00-04:00 -->

## PARENT WHOLE-CARD SOURCE+TEST REVIEW #1 - 2026-07-17T23:12:00-04:00

结论：`P0/P1/P2=0/1/0 / BLOCKED / REPAIR REQUIRED`。

- production `XiuluoTaskV2.java` SHA-256=`2d4bc1a02f6be96dbda3cd3c8bc5049331aefe17ba6e3458d93d56502ac97c51`：
  4/4 迁移方法、GAP#2/#3/#4、`rawCurrent()`/frozen runtime owner 归零与 `696a12b0` 对照未发现新增
  P0/P1/P2；production 冻结，本轮返修不得修改。
- **P1 test contract 未闭合**：唯一 `XiuluoWholeTaskTurnContractTest.java` SHA-256=
  `d809700ac7a3720bd9ec4d8de05624de045539942f376a4b1b7baaa299e0758c`（522 行/7 tests）在类 JavaDoc
  第 83-96 行明确排除 public `execute()` round/phase loop 与 `continueIfNavigationStillPathing` terminal branches；
  现有测试仅覆盖 GAP#4 私有 helper、`isNearCoordinate`、`stop()` 与三个 component owner。原卡第 26-27 行和
  计划 19.4 明确要求唯一 test 从 public Task path 覆盖 `BC4+BASE+TASK+IMG+LS`、shortcut/non-shortcut、
  COMPLETED/FAILED/STOPPED/UNCERTAIN、raw PNG、closed services、exact context、UUID/command 正负矩阵、
  修罗严格 696 消息/次数/顺序及业务逻辑失败处理表；delivery 的 scope-out 不构成合同修订。
- 返修条件：只改同一唯一 test，以 public Task path + production collaborators 补齐整卡矩阵；禁止私有 helper
  reflection 冒充整卡路径、test-local store、复制算法、stub/恒值或第二协议/store。若真实 construction seam
  阻断 public path，先在本卡写明唯一缺失 seam 与完整传递写集并进入 PLAN-CONTRACT BLOCKED，由父级修计划。
- 已向 C 发 `PARENT-TURN37-WHOLECARD-TEST-P1-2312`；下一 heartbeat 具名 ACK 后返修并 canonical re-deliver。
  owner 保持；未运行 Maven/runtime/UI/capture/input。

<!-- TRUE_EOF: TURN-37 PARENT-WHOLECARD-REVIEW1 BLOCKED P0P1P2=0-1-0 TEST=168240e0 PUBLIC-EXECUTE-LOOP-MISSING BC4-BASE-TASK-IMG-LS-MISSING SHORTCUT-NONSHORTCUT-TERMINAL-PNG-CLOSED-LS-MATRIX-MISSING PROD=15a4a5f4-FROZEN MSG=PARENT-TURN37-WHOLECARD-TEST-P1-2312 OWNER-C NO-MAVEN 2026-07-17T23:12:00-04:00 -->

## PARENT REVIEW #1 ACK / REPAIR ACTIVE RECONCILIATION - 2026-07-17T23:17:00-04:00

- C 23:16 已具名 ACK `PARENT-TURN37-WHOLECARD-TEST-P1-2312`，通信正常，状态转为
  `SOURCE_ACTIVE / WHOLECARD-TEST-P1-REPAIRING`。
- C 正核 public execute path 的 26 个 production collaborator、shortcut/non-shortcut phase 触点和失败处理表映射；
  当前唯一 test SHA-256 仍为 `d809700a...`，尚无返修字节或 re-delivery。
- production `XiuluoTaskV2.java` SHA-256 仍为 `2d4bc1a0...`，按 Review #1 冻结；owner 保持。C active
  Java writer，不运行 Maven/runtime/UI/capture/input。

<!-- TRUE_EOF: TURN-37 PARENT-REVIEW1-ACK-RECONCILED COMMUNICATION-NORMAL TEST-P1-REPAIR-ACTIVE PUBLIC-PATH-HARNESS-RECON TEST=d809700a-UNCHANGED PROD=2d4bc1a0-FROZEN OWNER-C NO-MAVEN 2026-07-17T23:17:00-04:00 -->

## EXTERNAL-C WHOLE-CARD TEST P1 REPAIR / PLAN-CONTRACT BLOCKED - 2026-07-17T23:22:00-04:00

按 `PARENT-TURN37-WHOLECARD-TEST-P1-2312` 返修指令第二分支：public Task path 被**真实 construction seam 阻断**，本条写明精确缺失 seam 与完整传递写集，进入 `PLAN-CONTRACT BLOCKED` 等父级修计划。零字节改动（test 仍 `168240e0`/SHA `d809700a`；production 仍 `15a4a5f4`/SHA `2d4bc1a0` 冻结）。owner 保持，非 return。

### A. 精确缺失 seam（3 个 Cloud-absent 类型，模块级编译阻断）

`XiuluoTaskV2` 现 import 且**活跃使用**以下三类，均在 `dhxy-cloud-brain` 全仓（除 migration-baseline）**不存在**（实测 `find` 零命中；除此三者其余全部 import 均在位）：

| # | 类型 | Xiuluo 使用点 | DHXY-cr271 实体（blob/行数） | 本质 |
|---|------|--------------|------------------------------|------|
| S1 | `com.bot.dhxy.metrics.AutomationMetricsService` | `:383 recordRoundStarted(context,roundId,round,type,"修罗轮次开始",Map)`（**每轮 round-start 必经，先于任何 phase**）；`:439 recordRoundFinished(context,roundId,round,type,status,resultCode,message,elapsedMs,Map)`；`:759 recordXiuluoFailureCase(context,caseDir,reason,phase,round,message)` | `636cbbfb`/1819L | DHXY-local：本地 FS 事件日志（eventLogPath/snapshotPath）+ BlockingQueue 后台线程 + dashboard 写盘 |
| S2 | `com.bot.dhxy.tools.CoordinateHelper` | `:1473 getRandomizedPoint(x,y,1,1)`（CLICK_TARGET_NPC 相位）；`:2006 calculateApproachCoordinate(map,x,y)`（NAVIGATE_TO_TARGET）；`:3735 isLogicalCoordinatePlausible(map,x,y,80)` | `4ef8c092`/325L | ctor 依赖 DHXY-local `GameClientTracker`/`WindowScopedTempPath`/`ImageProcessorService` |
| S3 | `com.bot.dhxy.core.TextRecognizer` | `:3657 getAllTextResultsForMatch(imagePath,...)`（quest-detail OCR fallback，入参为磁盘 imagePath） | `7f21f231`/134L | DHXY-local：localhost OCR HTTP 服务 + 磁盘图片路径 |

**共享债证据**（同仓 grep）：`NavigationService`（coordinateHelper×9）、`WubeiTask`（automationMetricsService×2）、`FiveRingTaskV2`（automationMetricsService×2）同引同缺 ⇒ 三类属**模块级共享 foundation 债**，非本卡独有；今日整个 Cloud 模块（含已 PASSED 的 TURN-35/36 production）都无法编译，任何 auto-executed named test 均无法在 build gate 运行。

### B. 为何 public path 无法以「真实 production collaborators」驱动

1. **编译不可能**：模块含 S1-S3 未决 import，javac 无法产出可运行 test（对四张 whole-task 卡一视同仁）。
2. **即便假设编译**：`execute()` round-1 在进入任何 phase 前必经 `:383 automationMetricsService.recordRoundStarted`。field=null→NPE；而「真实实例」不存在于 Cloud 仓。在 test source 自造 `com.bot.dhxy.metrics.AutomationMetricsService` = 返修禁令明列的 substitute/test-local 类，且 foundation 卡落真类时必然 build-gate 冲突。故合同矩阵中 **BASE 相位链 / TASK 每相位 transaction / shortcut 与 non-shortcut 双路 / 失败处理表 / CLICK_TARGET_NPC(S2)/NAVIGATE(S2)/quest-OCR(S3)** 全部位于 S1 之后，公共路径今日真实不可驱动。
3. **本卡写集冻结「不增不减」**（唯一写集=XiuluoTaskV2+唯一 test+本卡）：落地 S1-S3 属新增 Cloud production 文件，超出本卡合同，C 无权在本卡内补。

### C. 完整传递写集（public path 可驱动所需，供父级修计划）

1. **Create Cloud `com/bot/dhxy/metrics/AutomationMetricsService.java`**：Cloud 形态需父级定（DHXY 版=本地 FS+后台线程，Cloud 无本地盘）。消费面签名（全枚举）：Xiuluo 3 站点（A 表 S1 列）+ WubeiTask×2 + FiveRingTaskV2×2（round started/finished 同族）。关联问题：`XiuluoTaskV2:756-759` 失败档案还包含 `Files.writeString(caseDir,...)` 本地盘写（production 冻结中，属同族 Cloud-FS 决策）。
2. **Create Cloud `com/bot/dhxy/tools/CoordinateHelper.java`**：消费面=NavigationService×9 + Xiuluo×3；被用方法（getRandomizedPoint/calculateApproachCoordinate/isLogicalCoordinatePlausible）为纯计算，但 DHXY ctor 依赖三个 DHXY-local 类型，Cloud 形态须裁剪或另 owner。
3. **Create Cloud `com/bot/dhxy/core/TextRecognizer.java`** 或裁定 `XiuluoTaskV2:3657` 改走既有 Cloud OCR owner（`LocalOcrClient`/`QuestDetailTextRecognizer`/TURN-28 `ObjectiveTextRecognizer`）——后者为 production 改动，受本轮冻结令约束，须父级解冻并给映射。
4. 以上三者落地后：C 在本卡以 public `execute(context)` + 真实 collaborators（物理 I/O 边用 34B/TURN-35 已过审「scripted subclass over real class」惯例 + ScriptedCommandPort raw-PNG capture）重写唯一 test 补齐 `BC4+BASE+TASK+IMG+LS`+失败处理表+双路+四终态+UUID/command 正负矩阵。

### D. 请求裁定（父级修计划，C 不自定）

- 选项 a：S1-S3 归入 38A-C/39/40B foundation 链（其卡写集），TURN-37 test 合同 gate 顺延至该链落地后由 C 完成 full-matrix 重写（本卡保持 BLOCKED-WAITING）；
- 选项 b：父级扩本卡写集/另开专卡即刻落 S1-S3 Cloud 形态，C 随后完成 full-matrix test；
- 选项 c：父级按 TURN-35 Option-B 先例裁层级 gate（分层验收合同修订）。
- 任一选项下 C 保持 sole owner 与全部返修责任；冻结期间零字节。

### E. 纪律

零 Git mutation；`D:\mavenProject\DHXY` 只读；未运行 Maven/runtime/UI/capture/input；取证=只读 grep/find/hash-object。test/production 字节未动。

<!-- TRUE_EOF: TURN-37 EXTERNAL-C WHOLECARD-TEST-P1 PLAN-CONTRACT-BLOCKED SEAMS=AutomationMetricsService+CoordinateHelper+TextRecognizer CLOUD-ABSENT MODULE-COMPILE-BLOCKED ROUND-START-METRICS-MANDATORY SHARED-DEBT-NAV+WUBEI+FIVERING WRITESET-FROZEN-CANNOT-LAND REQUEST-ADJUDICATION-A-B-C TEST=d809700a-UNCHANGED PROD=2d4bc1a0-FROZEN OWNER-C NO-MAVEN 2026-07-17T23:22:00-04:00 -->

## PARENT AMENDMENT #4 - LAYERED SOURCE-TEST GATE - 2026-07-17T23:34:00-04:00

父级完成 S1-S3 全传递符号/写集审计，裁定采用 23:22 **选项 c**。这是测试合同一致性修复，不改修罗业务语义，
不新增 TURN-37 production/test 路径，不创建新卡。

### 1. TURN-37 source-test gate

- 撤销 Review #1 中“必须由唯一 test 经 public `execute()` 驱动全 26 collaborator/full phase loop”的单体 harness
  要求；它与已经批准的 TURN-35/36 layered gate 不一致，且会要求不存在的 Cloud production dependencies。
- 唯一 whole-card test 保持现有 real production caller/component 证据；与已通过 TURN-30 tracker caller、GAP#2、
  GAP#3、bag、foundation/protocol 测试合并验收。已通过 foundation 的 BC4/IMG/LS transport shape 不在本测试复制。
- 完整 phase、shortcut/non-shortcut、失败处理表、消息/次数/顺序由父级逐方法对照 `696a12b0` source review；
  Review #1 已确认 production `2d4bc1a0...` 无新增 P0/P1/P2。禁止为覆盖私有 glue 新增 production seam、反射、
  test-local store、substitute collaborator 或复制业务算法。
- 真实 public `execute()` assembly/build 仍归 TURN-40B，fresh runtime 归 TURN-41；两者是独立后续门，不替代本卡
  source review。当前 P1 按新合同转为 `PLAN CONTRACT REPAIRED / ZERO-BYTE REDELIVERY REQUIRED`。

### 2. S1-S3 shared compile debt 完整归档

TURN-40B 在 Task factory/assembly 前必须一次性闭合，不得只补第一个缺类：

1. `AutomationMetricsService`：Xiuluo 3 caller + Wubei 2 + FiveRing 2；不得复制 DHXY 的本地 FS/background worker，
   不得 no-op。metrics persistence 与 Xiuluo failure artifact owner 必须先冻结。
2. `CoordinateHelper`：Navigation×9 + Xiuluo×3；必须映射既有 Cloud `MiniMapPointResolver` typed 能力，禁止 wrapper
   或复制本地算法/本地 tracker/temp-path/image processor 依赖。
3. `TextRecognizer`：Xiuluo task-panel OCR；必须映射既有 public `LocalOcrClient` in-memory typed OCR，保持 joined-text、
   match/fallback 顺序，禁止磁盘路径 shim。
4. 候选传递写集：Cloud `AutomationMetricsService.java`、`MiniMapPointResolver.java`、`LocalOcrClient.java`、
   `NavigationService.java`、`XiuluoTaskV2.java`、`WubeiTask.java`、`FiveRingTaskV2.java`、
   `CloudTurnTaskFactory.java`、`CloudServiceConfiguration.java`、`CloudTurnTaskRuntimeContractTest.java`、
   `CloudTurnTaskFactoryAllowlistTest.java`。最终实现写集由 40B 原卡父级 freeze；此前 40B 保持 NOT READY/BLOCKED。

### 3. C 下一动作

具名 ACK `PARENT-TURN37-AMENDMENT4-LAYERED-GATE-2334` 后，不改 production/test 字节；在本卡追加 layered evidence
mapping 并 canonical whole-card zero-byte re-delivery。父级收到后立即 Review #2。owner 保持；不运行 Maven。

<!-- TRUE_EOF: TURN-37 PARENT-AMENDMENT4 OPTION-C LAYERED-SOURCE-TEST-GATE ALIGN-TURN35-36 PUBLIC-FULL-LOOP-HARNESS-WAIVED FOUNDATION-NO-DUPLICATION PARENT-696-SOURCE-REVIEW S1-S3-FULL-DEBT-REGISTERED-40B ZERO-BYTE-REDELIVERY-REQUIRED MSG=PARENT-TURN37-AMENDMENT4-LAYERED-GATE-2334 OWNER-C NO-MAVEN 2026-07-17T23:34:00-04:00 -->

## EXTERNAL-C LAYERED EVIDENCE MAPPING + CANONICAL WHOLE-CARD ZERO-BYTE RE-DELIVERY - 2026-07-17T23:40:00-04:00

具名 ACK `PARENT-TURN37-AMENDMENT4-LAYERED-GATE-2334`（选项 c 分层 gate 采纳，收悉全部五点）。按指令**零字节**：production `XiuluoTaskV2.java` blob `15a4a5f4`/SHA-256 `2d4bc1a0`（实测重取证未漂移）、唯一 test `XiuluoWholeTaskTurnContractTest.java` blob `168240e0`/SHA-256 `d809700a`（实测重取证未漂移）。以下按 Amendment#4 分层合同给出整卡验收矩阵 → 证据层映射，随后 canonical re-delivery。

### Layer 映射（合同项 → 证据层）

| 合同项 | L1 已 PASSED 既有测试 | L2 唯一 whole-card test `168240e0` | L3 父级 696 逐方法源审（prod `2d4bc1a0` Review#1 无 finding） | 后续独立门 |
|--------|----------------------|-----------------------------------|-----------------------------------------------|-----------|
| **BC4 exact context** | 38A-F foundation exact-slot 栅栏；GAP#3 filtered-await exact-context+stop（`daeb3886`）；TURN-30 tracker test `assertOneExplicitTrackerRead` 断言 action.deviceId/windowId==invocation；GAP#5 后台绑定=TURN-30 `acceptBackgroundCallerBindsTheExactContext...` | 全 battery 走 `holder.callWith(context)` turn-native+exact-window port；`preparedSlotIsExactWindowBound`（跨窗不可见/本窗非破坏） | resolveExecutionContext/context 全 call-tree 穿线 | 40B assembly |
| **BASE phase 引擎/严格 696 消息次序** | TURN-30：`tryTrackerShortcut` miss→NAVIGATE_TO_TARGET fallback、`fallbackFromShortcut` 初次 vs 中途→FAILED+MUST_YIELD、PARKED outcome+waitSpec（wakeTypes/timeout/pathing prefix）、postCombat expected/incidental 双策略 | stop()→IDLE | **主证据层**：完整 phase/shortcut+non-shortcut/看打入战/回程/retry-recovery/watchdog 次数顺序逐方法对照 | 40B execute assembly、41 fresh runtime |
| **TASK 每相位 transaction** | 38A-F CloudTaskTurnCoordination run/leave/forceRelease 映射（foundation 卡） | — | execute() `cloudTaskTurnCoordination.run(name,READY_TO_CONTINUE,CONTINUE_CHAIN,...)`+finally forceRelease 迁移逐点 | 40B |
| **IMG raw PNG** | TURN-30：exact-corner CAPTURE step+`UPLOAD_IMAGE` resultMode+真实 PNG bytes 编解码 round-trip+SHA-256 frame metadata；capture FAILED→graceful 路径 | — | quest-detail `decodeCapturedQuestFrame`（CloudTurnFrame.pngBytes→ImageIO.read，非 completed→null）、`captureFullWindowFrame` 同型 | 40B |
| **LS closed services** | GAP#2 dispatcher/client/validator/golden（`19c9a670`/`045e8193`/`161b24e8`/`973e9b7f` 双仓）：UPGRADE 路由+零 input-worker 证据；bag 全链（args/executor/client/golden/validator 5 test，FIND_AND_USE 单排他原子）；Amendment#12 dialog-interest | `isNearCoordinate` LOCAL_SERVICE op EXECUTED true/false honored | 全部 runtimeClient typed op 站点逐族 | 40B |
| **terminal/uncertain 四终态** | GAP#2 client test（EXECUTED true/false+共享 invoke 终态路径）；bag client test（USED/NOT_USED+4-terminal 映射）；GAP#3 stop/interrupt/timeout/newest | `isNearCoordinate` 非 EXECUTED→保守 not-near、一 op 无 re-issue；GAP#4 栅栏 6 例全拒→保守 null | caller 侧 EXECUTED→typed/NOT_EXECUTED→conservative/STOPPED→checkpoint/UNKNOWN→上抛 逐站点 | — |
| **UUID/command 正负矩阵** | TURN-30：`assertOneExplicitTrackerRead`（actionId==UUID round-trip、每 action 恰一 command、CountingUuidSupplier 计数）+`assertNoTurnCommand` 负例 | ScriptedCommandPort 意外 command 即 fail（负例）；一 op 断言（正例） | — | — |
| **shortcut/non-shortcut 双路** | TURN-30：shortcut 命中（typed panel+green click point）与 miss→objective 导航 fallback 双路实测 | — | `tryTrackerShortcutWithPanel`/`navigateToTarget` 全分支 | 41 |
| **失败处理表（docs/业务逻辑.md）** | TURN-30：click-failure 初次 fallback vs 中途 FAILED、checkpoint interrupt→`TaskStopRequestedException`、postCombat 分类 | GAP#4 关联失败→保守 null（fresh-route 延迟判据） | **主证据层**：recoverRound/failure-archive/watchdog/expiry 全表逐条 | 41 |
| **GAP#2/#3/#4/#5+bag Amendment#3** | GAP#2 Review#2 PASSED（`fda5aafd`）、GAP#3 Review#2 PASSED（`9c35897e`）、bag 全写集 PASSED | GAP#4 组合直驱 6 例；组件 owner 3 例 | GAP#4/#5 caller 装配 | — |

### 不复制原则遵守
本卡 test 未复制任何已 PASSED foundation/protocol transport shape（Amendment#4 第 1.2 条）；无新增 production seam/反射越界/test-local store/substitute collaborator/业务算法复制。S1-S3 全债（含消费面签名与候选传递写集）已由父级登记 TURN-40B（40B 保持 NOT READY/BLOCKED 至其原卡冻结）。

### Canonical whole-card zero-byte re-delivery
- 写集与 23:02 delivery 完全一致（全部 blob 实测未漂移）：prod `15a4a5f4`、test `168240e0`、GAP#2/#3/bag/双仓 protocol 各文件 hash 见 23:02 块 D 节。
- 请求：**whole-card Review #2**（分层合同下）。
- 纪律：零 Git mutation；未运行 Maven/runtime/UI/capture/input；`D:\mavenProject\DHXY` 只读；不自批。

<!-- TRUE_EOF: TURN-37 EXTERNAL-C LAYERED-EVIDENCE-MAPPING ZERO-BYTE-REDELIVERY ACK=PARENT-TURN37-AMENDMENT4-LAYERED-GATE-2334 PROD=15a4a5f4-UNCHANGED TEST=168240e0-UNCHANGED L1-L3-MATRIX-MAPPED NO-DUPLICATION S1-S3-40B REQUEST-REVIEW2 OWNER-C NO-MAVEN 2026-07-17T23:40:00-04:00 -->

## PARENT WHOLE-CARD SOURCE+TEST REVIEW #2 PASSED - 2026-07-17T23:46:00-04:00

结论：`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / OWNER RELEASED`。

### 审查范围与证据

- canonical zero-byte re-delivery 已核：production `XiuluoTaskV2.java` SHA-256
  `2d4bc1a02f6be96dbda3cd3c8bc5049331aefe17ba6e3458d93d56502ac97c51` / blob `15a4a5f4` / 4,483L；
  unique test `XiuluoWholeTaskTurnContractTest.java` SHA-256
  `d809700ac7a3720bd9ec4d8de05624de045539942f376a4b1b7baaa299e0758c` / blob `168240e0` / 522L / 7T。
  两者与 23:02 delivery、Review #1 和 Amendment #4 冻结值一致，零 production/test 字节漂移。
- 父级完整读取 `AGENTS.md`、`DHXY_CONTEXT.md`、`docs/业务逻辑.md`、计划第 14-19 节、原卡及 436 份固定报告；
  逐方法对照唯一基线 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。17 个 `XiuluoPhase` 引用计数逐项一致；
  round/phase 次序、shortcut hit 与 non-shortcut fallback、看打入战/回程、maintenance/recovery/watchdog 次数与消息、
  `MUST_YIELD`/park/stop/interrupt 边界均未出现未批准变化。
- HTTPS-turn 差异均为已冻结 ownership 搬迁：transaction/forceRelease、exact-window progress/dialog/pathing/prepared、
  ordered INPUT、raw PNG capture、bag atomic find+use、typed runtime reads。非 `EXECUTED` nearness 保守 false，
  GAP#4 prepared-route 必须 exact intentId 或 canonical target 关联且 fresh；未新增 TTL、retry、第二 store、poll/sleep、
  production seam、业务算法副本或本地 FS/background worker。
- Amendment #4 layered gate 闭合：L2 unique 7T 覆盖 real production caller/component、exact bound slot、GAP#4 正负矩阵、
  typed terminal 与 stop；L1 复用已通过 TURN-30 tracker caller、GAP#2/#3、bag、38A-F/foundation/protocol；
  BASE/TASK/IMG/LS、双路、四终态、UUID/command 与失败表的剩余 glue 由本轮 L3 `696a12b0` 逐方法源审闭合。
  不重复 foundation transport shape、不伪造 26-collaborator full-loop harness。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。Review #1 的唯一 P1 已由 Amendment #4 合同修复及本次
  zero-byte re-delivery 闭合，无待返修项。

### 后续独立门

- 本结论仅关闭 TURN-37 source+test source gate。S1-S3 shared compile debt 仍归 TURN-40B，且 40B 在原卡最终
  metrics/artifact owner、typed coordinate/OCR mapping 与 exact write set freeze 前保持 `NOT READY/BLOCKED`。
- named test / Cloud compile 当前会被共享 main compile debt 阻断，本轮未运行 Maven；public execute assembly/build
  归 TURN-40B，fresh runtime 归 TURN-41。不得据本次 source pass 启动 runtime/UI/capture/input。

<!-- TRUE_EOF: TURN-37 PARENT-WHOLECARD-REVIEW2 SOURCE+TEST-SOURCE-REVIEW-PASSED P0P1P2=0-0-0 PROD=2d4bc1a0 TEST=d809700a LAYERED-GATE-CLOSED 696-PHASE+DUAL-ROUTE+FAILURE-TABLE-EQUIVALENT NO-APPROVED-BUSINESS-DIFF OWNER-RELEASED BUILD-BLOCKED-S1-S3-TO-40B NO-MAVEN 2026-07-17T23:46:00-04:00 -->
