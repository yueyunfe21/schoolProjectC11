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
