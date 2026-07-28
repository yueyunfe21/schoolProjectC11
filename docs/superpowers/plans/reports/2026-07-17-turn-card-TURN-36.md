# CR271 / TURN-36 FiveRing Whole-Task HTTPS Turn Card

## PARENT FROZEN WHOLE-CARD SOURCE-START READY - 2026-07-17T01:10:00-04:00

- 状态：`WHOLE-CARD SOURCE-START READY / ZERO OWNER`。
- 类型：既有完整 `TURN-36` 父卡；禁止 tranche、fragment、子卡或多人共享写集。
- sourceDependsOn 已满足：`13C+14+15+23+28+32+34A`。
- approvalDependsOn：`TURN-26+TURN-27+TURN-T01/T02/T03/T04`、本卡父级 source/test-source review、
  唯一 named test 与 Cloud compile。
- 领取点 production：`FiveRingTaskV2.java` 2,775 行，SHA-256
  `287ff0ebe4f3cecf9820a10d2ffcbf0f7aed2a26beb7a5f510d92f540e8a4bdb`；唯一 test 当前不存在。

## 唯一完整写集

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`
2. Create `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wuhuan/FiveRingWholeTaskTurnContractTest.java`
3. 本固定报告只允许 claim/delivery/return/repair 追加；其余 production/test 全部只读。

## 整卡验收合同

- 完整迁移 FiveRing phase、prepare/买鞋、接任务、tracker/pathing/dialog、给物、战斗、完成 story 与多轮终止；
  保留 open-main-bag closed boundary、重试阈值、keep-turn/park 顺序。
- physical input/capture/OCR/local service 只能经现有 HTTPS turn 与四个 closed `LOCAL_SERVICE`；不得复制
  Navigation/Dialog/NpcClick mechanics，不得新增 facade、shim、第二 store、TTL 或自动 retry。
- `TURN-26/27` 保持 Task 当前 public caller signature；Worker 不修改 predecessor 或协议文件。
- 唯一 test 从 public Task path 覆盖 `BC4+BASE+TASK+IMG+LS`，包括完整 phase、prepared tracker、dialog story
  两种完成语义、open-main-bag、terminal/uncertain、exact context 与 UUID/command；禁止 private reflection/source guard。
- `TaskExecutionContext.builder()` 等当前缺失构造在本 Task 内迁到 bound turn-native entry，不加兼容 shim。
- 无已批准业务差异；五环唤醒/prepared-action 仅按 `docs/业务逻辑.md` 已确认边界实施。

## 自行领取协议

Worker 领取前重读三张 READY 卡 EOF 和 SHA；仅最早在本文件 EOF 追加 `EXTERNAL-X TURN-36 WHOLE-CARD
CLAIMED` 并回读确认唯一者为 owner。领取后负责完整 production/test/report/返修；Java writer 活动时不运行
Maven，稳定后只运行授权 named test 与适用 compile。

<!-- TRUE_EOF: TURN-36 PARENT-FROZEN WHOLE-CARD-SOURCE-START-READY ZERO-OWNER PROD=287ff0eb/2775 TEST=ABSENT APPROVAL-WAITS-26-27-T01-T04 NO-FRAGMENT NO-DISPATCH 2026-07-17T01:10:00-04:00 -->

## EXTERNAL-C TURN-36 WHOLE-CARD CLAIMED - 2026-07-17T01:19:30-04:00

- Implementation Worker：**CR271 External Worker C**（会话 `091df301`）。非 reviewer，不自批；本段不含 `APPROVED/CLOSED`。
  身份=External C（勿误标 B）。TURN-28 已于 00:32 Parent Review #3 `0/0/0` PASSED、owner 释放，C 当前无其它持卡，
  单卡合规、无双卡并持。
- 领取时间：`2026-07-17T01:19:30-04:00`。领取前重读三张 READY 卡 EOF+SHA：TURN-35=ZERO-OWNER（未领）、
  **TURN-36=本卡 ZERO-OWNER（领取目标）**、TURN-37=`EXTERNAL-d CLAIMED 01:15:00`（不碰）。claim 前实测本卡
  mtime `01:10:46`、5 section、唯一父级 TRUE_EOF（01:10:00）、无既有 CLAIMED；append 后立即回读 EOF 确认唯一 owner，
  若发现更早 claim 立即 canonical 自撤。
- 完整任务卡：既有完整父卡 `TURN-36`（FiveRingTaskV2 whole-task HTTPS turn 迁移）。我承担整卡 production/test/report/
  返修，直至父级 `SOURCE+TEST SOURCE REVIEW PASSED` 或 canonical whole-card `OWNER RETURNED`；不拆卡、不 tranche/
  fragment/子卡、不共享写集、不造 facade/shim/第二 store/TTL/自动 retry、不改 predecessor/协议文件。
- 唯一完整写集与领取点实测 SHA（与父级 01:10 冻结逐字一致）：
  | 文件 | 行数 | SHA-256(前16) | 备注 |
  |---|---:|---|---|
  | `dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java` | 2775 | `287ff0ebe4f3cecf` | production，领取点冻结 |
  | `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wuhuan/FiveRingWholeTaskTurnContractTest.java` | — | `ABSENT` | **新建**唯一 named test（BC4+BASE+TASK+IMG+LS） |
  | 本固定报告 `2026-07-17-turn-card-TURN-36.md` | — | append-only | 仅 claim/delivery/return/repair |
- 依赖核实：`sourceDependsOn = 13C+14+15+23+28+32+34A` 全部 source review PASSED（§16 注册表 01:10 传递审计确认已满足）；
  `approvalDependsOn = 26+27+T01/T02/T03/T04 + 父级 source/test review + named test + Cloud compile` 属**最终批准 gate**，
  不阻源码开工（父级 01:10 明示 26/27 只作 approval gate）。TURN-26 现于 B Build Repair #2（未通过），仅影响最终 approval，不影响本卡 source-start。
- 写集互斥核实：本卡唯一 production=`FiveRingTaskV2.java`、test=`FiveRingWholeTaskTurnContractTest.java`，与 D 的
  TURN-37（`XiuluoTaskV2.java`/Xiuluo test）、B 的 TURN-26（`DialogService.java`+dialog state+dialog 三 test）、TURN-35
  （`WubeiTaskV2` 写集，未领）零文件重叠（§18 R5 三文件互斥）。两仓其余 dirty/untracked 与他人半成品全部只读保护。
- 纪律：其它 Java writer 活跃期间不运行 Maven/JUnit/compile/package；不启 runtime/application/server/Task/UI/capture/input；
  零 Git mutation；只从当前字节增量编辑；稳定后只运行父级授权 named test 与适用 compile。
- 无已批准业务差异；按 `696a12b0` 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-36 EXTERNAL-C WHOLE-CARD CLAIMED SOLE-OWNER PROD=287ff0eb/2775 TEST=TO-CREATE ANTI-RACE-PRECHECKED session-091df301 2026-07-17T01:19:30-04:00 -->

## PARENT PLAN-CONTRACT AUDIT #1 - OWNER RETURN REQUIRED - 2026-07-17T01:32:26-04:00

- TURN-37 归还审计证明同一 `TaskTransactionRunner/WindowReadyEventBus/WindowRuntimeContext` 缺口也存在于
  FiveRing；01:10 把 TURN-26/27 仅列 approval gate 的裁决不完整。本卡立即转
  `PLAN-CONTRACT BLOCKED / EXTERNAL-C OWNER RETURN REQUIRED`。
- 当前 production 仍为领取点 2,775L/`287ff0eb...`，test 不存在。External C 必须停止实施并在本卡 canonical
  `WHOLE-CARD OWNER RETURNED / PLAN-CONTRACT BLOCKED`；父级不直接撤销 owner，也不允许第二 writer。
- 修正后的 source gate 为 `TURN-26+TURN-27`。后续只允许消费 TURN-26 prepared state、TURN-27 唯一
  exact-context Cloud pathing state和显式 `TaskExecutionContext`；不得在 Task 内重建本地 runtime/event bus/owner。
- transaction wrapper 仅属本地调度 plumbing：后续 Amendment 必须把每 phase 保持“恰好执行一次”，继续使用
  原 `TaskTransactionResult/TaskYieldPolicy` 做业务 gate；删除 wrapper 不得合并、跳过或新增 phase/yield/park。

<!-- TRUE_EOF: TURN-36 PARENT-PLAN-CONTRACT-AUDIT-1 BLOCKED EXTERNAL-C-RETURN-REQUIRED SOURCE-WAITS-TURN26-27 ZERO-BYTES-EXPECTED 2026-07-17T01:32:26-04:00 -->

## EXTERNAL-C TURN-36 WHOLE-CARD OWNER RETURNED / PLAN-CONTRACT BLOCKED - 2026-07-17T01:50:00-04:00

- Owner：**CR271 External Worker C**（会话 `091df301`）。遵父级 `PLAN-CONTRACT AUDIT #1 (01:32:26)` 指令，canonical
  归还整卡 TURN-36 owner，停止实施。本段确认接受 `PLAN-CONTRACT BLOCKED`；不自批、不留半成品、不加 shim 强推。
- 交付字节：**零 production / 零 test 字节**。`FiveRingTaskV2.java` 仍为领取点 `287ff0eb…`/2,775L（实测 SHA 未漂移），
  唯一 test `FiveRingWholeTaskTurnContractTest.java` 从未创建（ABSENT）。仅本固定报告 append。无 Git mutation、未运行 Maven。
- 独立复核（非继承 TURN-37 结论，我亲自审 `FiveRingTaskV2.java`）——四族本地协作者在本 Task 深度承载、turn-native
  `TaskExecutionContext` 均无落点，且合同禁 facade/shim/第二 store，故不可等价迁移：
  1. **PARK-WAKE 事件总线**：`WindowReadyEventBus windowReadyEventBus`（field 215；`windowReadyEventBus.await/current` 512 等），
     park/wake pathing watcher；turn-native context 只有 `isPauseRequested/isStopRequested/throwIfStopRequested`，无 ready-event 总线。归属未建的 TURN-27（exact-context Cloud pathing state）+ TURN-38 系列。
  2. **TRANSACTION-RUNNER**：`TaskTransactionRunner taskTransactionRunner`（56 处；`runPhaseWithoutTaskTurn`/`taskTransactionRunner.run`/`forceReleaseTurn` 每 phase 包裹 keep-turn/park/yield）；turn-native 无 task-turn 协调等价物。父级已明示删 wrapper 须保每 phase“恰好一次”并续用原 `TaskTransactionResult/TaskYieldPolicy` 业务 gate——须 Amendment 明确落点。
  3. **RUNTIME-MISC**：`context.getWindowRuntimeContext()`（263/296 `updateTaskRunProgress` 等，18 处）；turn-native context 不暴露 `getWindowRuntimeContext`。
  4. **CONTEXT-HOLDER**：`WindowTaskContextHolder windowTaskContextHolder`（field 214；`windowTaskContextHolder.rawCurrent()` 10 站点：507/850/931/987/1691/1798/1813/1872/2574/2614）取 `WindowRuntimeContext`；turn-native 无等价 holder。
- 80% 可映射部分（audit 保留，供修合同后复用，勿重审）：①直连物理输入 10 处 → `context.getTurnGameClient()` turn INPUT；
  ②直连 capture/OCR 5 处（`tracker.captureToMemory` 1338、`ImageFinder.isMatch` 1365、`tracker.refreshWindowState`+`getWindowBaseX/Y` 1464-1472）→ turn CAPTURE/MATCH+metadata；③`TaskExecutionContext.builder()`（2751）→ bound `turnNative(...)`（factory 已在 `TaskExecutionContext:108`）；
  ④Nav/Dialog/NpcClick/TaskTracker/AutoCombat/PlayerState + 4 closed LOCAL_SERVICE 保留 service 调用不复制 mechanics。
- 阻断归属与请求：source gate 应含 **TURN-26 prepared state + TURN-27 exact-context Cloud pathing state**（现 26 在 B Repair #2、27 blocked-by-26，均未通过），及父级对 transaction-wrapper/runtime-progress/context-holder 四族的统一 turn-native 落点 Amendment。**该墙对 TURN-35（Wubei）同样适用**（D 已就 TURN-37 提出、父级 AUDIT #1 已确认 FiveRing 同墙）。
- owner 已释放；在父级 canonical 重新指派 C 且发布修正合同/Amendment 前，我不再修改本卡与写集文件，仅按总账协议只读监控。
- 无已批准业务差异；按 `696a12b0` 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-36 EXTERNAL-C WHOLE-CARD OWNER RETURNED PLAN-CONTRACT-BLOCKED ZERO-BYTES 4-BLOCKED-FAMILIES(PARK-WAKE/TRANSACTION-RUNNER/RUNTIME-MISC/CONTEXT-HOLDER) 80PCT-MAPPABLE-AUDIT-PRESERVED SOURCE-WAITS-TURN26-27 SAME-WALL-TURN-35 ACK-PARENT-AUDIT-1 2026-07-17T01:50:00-04:00 -->

## PARENT ACCEPTS WHOLE-CARD RETURN - 2026-07-17T01:35:00-04:00

- 接受 External C canonical 零字节整卡归还；owner 已释放。
- 状态固定为 `PLAN-CONTRACT BLOCKED / ZERO OWNER`，等待 TURN-26/27 source pass 与本卡 Amendment #2。
- production/test 与领取点一致；不是 delivery，不做 source review，不运行 Maven，不创建 reviewer。

<!-- TRUE_EOF: TURN-36 PARENT-ACCEPTS-RETURN PLAN-CONTRACT-BLOCKED ZERO-OWNER SOURCE-WAITS-TURN26-27 2026-07-17T01:35:00-04:00 -->

## PARENT PLAN-CONTRACT AUDIT #2 - PREDECESSORS PASSED, RESIDUAL CONTRACT GAPS - 2026-07-17T14:24:00-04:00

- `TURN-26/27` source review 已通过，但原四族阻断没有被真实 API 全部消除；本卡继续
  `PLAN-CONTRACT BLOCKED / ZERO OWNER`，不是 READY，也没有 owner 可领取。
- prepared state 缺非破坏性 exact-bound read；FiveRing 的 prepared tracker/route 时效、stale clear 与
  completion 交叉验证不能改成提前 consume。pathing state 虽可读 snapshot/intent，却不提供 ready-event sequence、
  fresh-other-window priority 或 early wake。
- 仍无合法替代的 Task 调用包括 `GameStateUtil.confirmCurrentMapFresh/detectFlyingState/isNearCoordinate/
  recordMovementIntent` 与 `CoordinateHelper.getScaledRect/findImageInRegion/getRandomizedPoint`。这些包含进店、下坐骑、
  ROI 模板与 tracker 点击次序，不能在 Task 内抄一份，也不能以恒 null/fallback 强推。
- `TaskTransactionRunner.run/runExclusive/forceReleaseTurn` 与 event-bus 让渡语义尚无 typed Cloud owner；删 wrapper
  后直接连续 loop 会破坏 FiveRing 明确冻结的 outside-phase、公平让权和 watchdog 时序。
- `TURN-40B` 经 `39 <- 38A <- 35/36/37` 反向依赖本卡，不能补作前置而制造 DAG 环。须先冻结无环共享 owner、
  write set、public API 与 exact acceptance，再追加 Amendment #3。此前禁止 claim、shim、poll/sleep、第二 store
  或业务算法复制。

<!-- TRUE_EOF: TURN-36 PARENT-PLAN-CONTRACT-AUDIT-2 BLOCKED ZERO-OWNER TURN26-27-PASSED RESIDUAL-GAPS=PREPARED-PEEK+WAIT-YIELD+GAMESTATE+VISION NO-READY 2026-07-17T14:24:00-04:00 -->

## PARENT AMENDMENT #3 DAG REPAIR - 2026-07-17T15:02:00-04:00

- 状态改为 `WAITING TURN-38A FOUNDATION / ZERO OWNER / NO READY`。
- TURN-38A-F 已开放，唯一拥有 prepared peek、ready-event sequence/early wake 与 fair-turn coordination。
  38A-F 父级 source review 通过后，本卡自动转 `READY / ZERO OWNER`；FiveRing 的 map/flying/ROI/image/
  randomize 调用继续由本卡按冻结逐调用映射闭合，不再制造依赖环。
- 禁止提前 claim、poll/sleep、第二 store、复制 local runtime 或改变 `696a12b0` 阶段顺序。

<!-- TRUE_EOF: TURN-36 WAITING-TURN38A-FOUNDATION ZERO-OWNER NO-READY AUTO-OPEN-AFTER-38A-F-PASS 2026-07-17T15:02:00-04:00 -->

## PARENT READY RELEASE AFTER TURN-38A-F PASS - 2026-07-17T12:12:00-04:00

- 状态：`READY / ZERO OWNER`。TURN-38A-F 已获父级 Source Review #3 `P0/P1/P2=0/0/0`，本卡 Amendment #3
  的自动开放条件已满足。
- 这不是派卡。任一有完整容量的 Worker 可按本卡原有防竞态协议自行 canonical claim；最早有效 claim 为唯一 owner。
- 固定写集不变：`FiveRingTaskV2.java` + `FiveRingWholeTaskTurnContractTest.java` + 本报告 append-only；严格保持
  五环 phase/prepared/park 边界，不得复制 local runtime、创建第二 store 或占位语义。

<!-- TRUE_EOF: TURN-36 PARENT-READY-RELEASE AFTER-38A-F-PASS READY ZERO-OWNER NO-DISPATCH WRITESET-UNCHANGED 2026-07-17T12:12:00-04:00 -->

## EXTERNAL-C TURN-36 WHOLE-CARD CLAIMED (RE-CLAIM AFTER READY RELEASE) - 2026-07-17T12:33:00-04:00

- Implementation Worker：**CR271 External Worker C**（同身份连续接替会话）。非 reviewer，不自批；本段不含 `APPROVED/CLOSED`。C 当前无其它持卡（TURN-38A owner 已由父级 12:25 PLAN-CONTRACT REPAIR #4 释放；38A-C=DEFERRED/ZERO OWNER 不占用 C），单卡合规。
- 领取依据：本卡 12:12 `PARENT READY RELEASE`（38A-F PASSED 自动开放）+ 父级 12:27 `PARENT-TURN38A-PHASE-OWNER-RELEASE-DAG-REPAIR-EOF-REISSUE`（明示 C 容量足够时可自领 36/37）。领取前重读 READY 卡 EOF：TURN-35=A CLAIMED SOURCE_ACTIVE（不碰）、**TURN-36=本卡 READY/ZERO OWNER（领取目标）**、TURN-37=READY/ZERO OWNER 含 D 的 WIP `c0125a49`（不碰）。claim 前实测本卡 mtime 11:58:25、8 section、EOF=12:12 READY release、无既有 active claim（01:19 旧 claim 已 01:50 canonical 归还收口）；预检与本 append 两次独立调用；append 后回读 EOF，若见更早 claim 立即 canonical 自撤。
- 领取点实测：`FiveRingTaskV2.java` SHA-256 `287ff0ebe4f3cecf9820a10d2ffcbf0f7aed2a26beb7a5f510d92f540e8a4bdb`（与 01:10 冻结逐字节一致，零漂移）；唯一 test `FiveRingWholeTaskTurnContractTest.java` ABSENT。
- 完整任务卡：整卡 production/test/report/返修直至父级 `SOURCE+TEST SOURCE REVIEW PASSED` 或 canonical `OWNER RETURNED`；不拆卡、不共享写集、不造 facade/shim/第二 store/TTL/watcher/poll/sleep/自动 retry、不改 predecessor/协议文件、不复制 local runtime/Navigation/Dialog/NpcClick mechanics。
- 实施基座（本卡 01:50 归还审计 + Amendment #3 落点，勿重审）：①PARK-WAKE→`CloudWholeTaskReadyEventState`（publish/currentSequence/latest/latestOtherFreshPreparedAction/awaitNewer/clearTerminal）；②TRANSACTION-RUNNER→`CloudTaskTurnCoordination.run` default（每 phase 恰一次、原 TaskTransactionResult/TaskYieldPolicy gate）；③prepared 非破坏读→`CloudDialogPreparedActionState.peek`（consume 语义不变）；④pathing→TURN-27 `CloudNavigationPathingState` 只读镜像；⑤80% 映射：直连输入→turn INPUT、capture/OCR→turn CAPTURE/MATCH+metadata、`TaskExecutionContext.builder()`→bound `turnNative(...)`、Nav/Dialog/NpcClick/TaskTracker/AutoCombat/PlayerState+4 LOCAL_SERVICE 保留 service 调用；⑥map/flying/ROI/image/randomize 按 Audit #2 冻结逐调用映射闭合。严格 `696a12b0` phase/prepared/park/keep-turn/watchdog 顺序；五环唤醒/prepared-action 按 `docs/业务逻辑.md` 已确认边界。
- 纪律：A 为 active Java writer（TURN-35），**C 实施期间不运行 Maven/JUnit/compile**（写集互斥：`FiveRingTaskV2` vs `WubeiTask` 零重叠）；零 Git mutation；不启 runtime/application/server/Task/UI/capture/input；`D:\mavenProject\DHXY` 只读。
- **无已批准业务差异；按 696a12b0 等价迁移。**

TRUE_EOF

<!-- TRUE_EOF: TURN-36 EXTERNAL-C WHOLE-CARD RE-CLAIMED SOLE-OWNER PROD=287ff0eb-VERIFIED TEST=TO-CREATE FOUNDATION-LANDINGS-38AF+27-MIRROR ANTI-RACE-TWO-CALL 2026-07-17T12:33:00-04:00 -->

## PARENT AMENDMENT #4 - OTHER-WINDOW PATHING TERMINAL QUERY - 2026-07-17T12:49:00-04:00

- 批准 C 的最窄 additive proposal，关闭 `checkReadyPriorityBeforeOutsidePhase` 单站点合同缺口。
- TURN-36 写集增加且仅增加 Modify
  `com/yueyunfe/dhxy/cloudbrain/remote/CloudWholeTaskReadyEventState.java`；新增 public
  `latestOtherFreshPathingTerminal(TaskExecutionContext context, TaskType taskType, long maxAgeMs)`。
- exact 合同：复用既有 exact slot/lane binding；只选异窗、`PATHING_TERMINAL`、pathing state 为
  `ARRIVED`/`STOPPED_AWAY`、可选 taskType 匹配、age 未超 `maxAgeMs` 的事件；只读不删、不续期，按 sequence
  取最新。own-window、异 lane、超龄、非终态、taskType 失配必须返回 empty。
- 验收归 TURN-36 唯一 named test，必须从 production state 实例覆盖一项命中与上述全部负例；不重开或修改
  TURN-38A 已通过的历史测试，不新增第二 event store。
- 其余 TURN-36 写集与 `696a12b0` 业务条件/优先级不变。C 可立即实施并继续整卡，无需等待额外回复。

<!-- TRUE_EOF: TURN-36 PARENT-AMENDMENT-4 APPROVED ADD-READY-EVENT-STATE-METHOD latestOtherFreshPathingTerminal EXACT-NEGATIVE-MATRIX NO-SECOND-STORE CONTINUE 2026-07-17T12:49:00-04:00 -->

## EXTERNAL-C CONSOLIDATED ADDENDUM - FULL-PHASE TRANSITIVE DEPENDENCY AUDIT - 2026-07-17T13:58:00-04:00

全文件 recon 完毕（2,775L 逐段读毕，per 父级 12:23 指示一次性完整报告）。以下为全部剩余无落点/需裁决站点，按族给出 owner/API/binding/正负例/基线映射/test 归属；未列站点均有既定落点（38A-F 三 API、TURN-27 镜像、turn CAPTURE/MATCH/INPUT、service 调用保留、Amendment #4 已实施）。

### 族 A1：tracker intent 注册（自证有落点，申请确认非新 API）
- 站点：`registerTrackerPathingIntent`（2613-2627；callers 2547 绿链点击后/2602 prepared 快径点击后）——基线在 `inputSequences` 直点后注册 `UNTARGETED_TRACKER` intent（null target）。
- 落点=**TURN-27 Amendment #3 既有 `TurnGameClient.execute(steps, ev, timeout, TurnPathingIntent)`**：绿链点击 turn 化后同 action 携 `TurnPathingIntent(source, uuid, null, null, null, 0, "UNTARGETED_TRACKER")`→DHXY 本地 proof 登记→镜像回读。协议 nullable target 已支持。**申请父级确认此映射后族 A1 即无缺口**；`recordMovementIntent`（2546/2601）随族 C。

### 族 A2：无点击的显式 intent 注册（真缺口，1 站点）
- 站点：`markPathingStarted`（953）——鞋店入口导航 ARRIVED-from-cache 时无点击、纯注册 TARGETED(长安 130,130) intent 让本地 watcher 跑 door/dismount 处理。
- 无 action 可携 intent；镜像只读。**Proposal**（三选一）：(a) 允许该站点发一个零输入 WAIT-only turn action 携 intent（协议合法、DHXY proof 语义=坐标已在门口，需父级确认 proof 判定）；(b) 协议/executor 增 intent-only 注册 action 类型（大改，不推荐）；(c) 父级裁定该 cached-ARRIVED 分支的等价 Cloud 行为（如直接进入 door 确认 capture 循环，不经 watcher——业务语义变化需批准）。
- test：任一方案入 TURN-36 named test（cached-ARRIVED→door 处理路径正例+无 intent 泄漏负例）。

### 族 A3：watcher 终态消费/清除（5 站点）
- 站点：`clearPathingSignal`×5（878/1700/1717/1882/1897）——消费 ARRIVED/STOPPED_AWAY 防重复消费。镜像 forward-only 禁清除。
- **Proposal**：Task 内以 phase-context 记账等价实现——新增 `FiveRingPhaseContext.lastConsumedPathingIntentId`（Task 私有 record 字段，非第二 store：单值、随 phase state 流转、无跨窗/持久化），`isUsablePathingSnapshot` 增加 `!intentId.equals(lastConsumed)` 过滤；逐站点把 clear 替换为 markConsumed。正例=同 intent 终态只消费一次；负例=新 intentId 不被旧记账拦截。基线映射=clear 的唯一业务目的（防重复消费）逐条件等价，差异（本地 runtime 槽仍留终态直至新 intent）如实披露。
- test：TURN-36 named test 覆盖 consume-once/new-intent-passes。

### 族 A4：STOPPED_AWAY route prepared 关联读（3 站点）
- 站点：`freshPreparedRouteActionForPathingTerminal`×3（868/1708/1888）。
- **Proposal**：Task 内组合既有 API 等价实现：`cloudDialogPreparedActionState.peek(context, ROUTE_TRANSFER, null, reason, true)` + `verifiedWithin(PREPARED_ROUTE_DIALOG_CLICK_MAX_AGE_MS)` + 镜像 snapshot STOPPED_AWAY 前置；与基线方法体逐条件对齐（若基线含 intentId 关联条件，逐字段保留）。非新 API、非复制算法（组合两个已过审 read）。
- test：正例=终态+fresh route→延迟清理分支；负例=stale/无 route→正常消费分支。

### 族 B：runner UI 清理建议（1 组站点，协议缺字段）
- 站点：`isUiCleanupRecommended/getUiCleanupReason/clearPathingUiCleanupRecommendation`（1800-1806，accept NPC 点击前消费 runner 建议）。`TurnPathingSnapshot` 无对应字段。
- **Proposal**（二选一）：(a) 协议增 3 字段（uiCleanupRecommended/reason/recommendedAtMs）入 `TurnPathingSnapshot`+镜像映射——两仓 byte-identical 协议改动+golden test，写集需父级扩（跨 TURN-27 协议文件）；(b) 该站点降级为无条件 `closeAllGenericWindows()`（基线 else 分支已是无条件清理，差异仅少一条日志与 clear 回执）——行为近等价但属业务差异需父级批准。倾向 (b)（窄、无协议动作）。
- test：按裁决入 TURN-36 named test。

### 族 C：`recordMovementIntent`×2 + `gameStateUtil` 计算站点
- 与 TURN-35 QUESTION #2 族 4 完全同类；申请并入同一共享 foundation Amendment（TURN-27 卡已定 Navigation 为 `isSameMapName/isNearCoordinate/recordMovementIntent` 唯一 owner——Task 侧需 public 调用面）。站点：recordMovementIntent(2546/2601)、isNearCoordinate×3(1820/1834/2047)、confirmCurrentMapFresh×6、detectFlyingState×1(1031)。
- 冻结等共享裁决，不自造镜像。

### 族 D：披露项（无需裁决，delivery 记录）
- `runExclusive`→`coordination.run`（2321）：input 独占性由 turn 协议单队列承担，基线 exclusive-callback 语义差异披露。
- `TaskExecutionContext.builder`（2751 null-context debug 兜底）→turn-native fail-fast+javadoc（A 卡先例）。
- `updateTaskRunProgress`×2（264/297）+`markTaskIdle/Failed` gameContext 写：进度上报归 40B runtime 边界，披露处理方式待实施时定（保留 gameContext 写、progress 站点按 A 卡同类披露口径）。

**在父级逐族裁决前：族 A2/B/C 站点冻结；族 A1/A3/A4 若获确认即实施；所有其余站点（transaction×7、event-bus 3+priority 链、prepared peek/clear、turn INPUT/CAPTURE 机械族、completion/dialog/tracker service 链）立即开始批量迁移，不等待。**

无已批准业务差异；按 696a12b0 等价迁移。

TRUE_EOF

<!-- TRUE_EOF: TURN-36 EXTERNAL-C CONSOLIDATED-ADDENDUM FULL-AUDIT-COMPLETE FAMILIES-A1-A4-B-C-D PROPOSALS-COMPLETE UNBLOCKED-MIGRATION-STARTS 2026-07-17T13:58:00-04:00 -->

## PARENT RESPONSE TO CONSOLIDATED ADDENDUM - 2026-07-17T12:54:07-04:00

- **A1 approved with the existing TURN-27 proof gate only.** Each successful tracker-green click turn may carry the
  exact nullable-target `TurnPathingIntent`; DHXY registers it only after that same action is `COMPLETED` and the local
  movement proof is positive. `COMPLETED` without proof, stopped/failed/uncertain, intent mismatch, and click failure
  must not report `PATHING_STARTED`. No intent-only or optimistic registration is approved.
- **A4 approved only with the complete baseline association fence.** After exact-window/HWND `peek` and the unchanged
  10s verification age, the route action must match the current terminal mirror intent by exact nonblank `intentId` OR
  normalized nonblank target map. Merely seeing any fresh `ROUTE_TRANSFER` action is insufficient. The three callers and
  both positive/negative association branches belong to the TURN-36 named test.
- **B option (b) approved as action-equivalent at this call site.** `cleanupUiBeforeAcceptNpcClick` already executes
  `closeAllGenericWindows()` in both baseline branches, so Cloud keeps exactly one unconditional cleanup call. It must
  not claim that the DHXY-local recommendation was observed or cleared, and must remove the recommendation-specific
  reason/age log. No protocol fields or second acknowledgement path are added.
- **A2 remains frozen.** A WAIT-only action cannot satisfy TURN-27's positive movement-proof contract. Intent-only
  registration or bypassing the watcher would change ownership/door-handling semantics and requires the shared
  foundation Amendment.
- **A3 remains frozen and its phase-local proposal is rejected.** Baseline `clearPathingSignal` clears both the exact
  pathing slot and pending transfer-choice memory; `lastConsumedPathingIntentId` would leave local authority stale,
  omit cleanup side effects, and create Task-local replacement state forbidden by the existing fence. The shared
  Amendment must define an exact intent-bound clear/consume command with mismatch no-op and terminal cleanup.
- **C remains frozen with TURN-35/37 in the shared foundation audit.** This includes movement intent, map/coordinate/
  flying owner calls, progress/runtime writes, and any watcher-side registration/clear command.
- **D is corrected:** `TaskExecutionContext` turn-native fail-fast is accepted, but `runExclusive -> coordination.run`
  is not a disclosure-only mapping. `CloudTaskTurnCoordination.run` explicitly does not own baseline input-worker
  exclusivity; per-action queue serialization can interleave another window between the two accept attempts. Keep that
  site unmigrated until the shared Amendment supplies an exact exclusive multi-action boundary. Progress and terminal
  runtime writes also remain frozen; they may not be silently dropped or deferred by implementation choice.
- C continues A1/A4/B and all other approved mechanical families immediately. No approved business difference;
  behavior remains equivalent to `696a12b0`. This response does not authorize delivery while the frozen families remain.

<!-- TRUE_EOF: TURN-36 PARENT-RESPONSE-CONSOLIDATED A1-APPROVED-PROOF-GATED A4-APPROVED-EXACT-ASSOCIATION B-UNCONDITIONAL-CLEANUP A2-A3-C-FROZEN RUNEXCLUSIVE-NOT-EQUIVALENT SHARED-FOUNDATION-PENDING CONTINUE-UNBLOCKED 2026-07-17T12:54:07-04:00 -->

## PARENT WIP REPAIR REQUIRED - PRE-RESPONSE RUNEXCLUSIVE MIGRATION - 2026-07-17T12:59:07-04:00

- C 的 14:10 STATUS EVENT 证明 consolidated response 并发到达前，`acceptInitialDialogAndTriggerPathing`
  已把 baseline `TaskTransactionRunner.runExclusive` 改为 `CloudTaskTurnCoordination.run`。该单点违反上节裁决：
  两次 accept attempt 的整段 input-worker 独占被缩成逐 action 排队，异窗可在 attempt 间插入。
- 精确返修：恢复/保留该方法对 baseline exclusive authority 的依赖与调用，不得以注释披露替代行为。
  其余原 baseline `run`×4 与 `forceReleaseTurn`×2 到 Cloud coordination 的映射可保留，不要求回退。
- 若当前 Cloud 写集无法表达 exclusive callback，该单点必须保持冻结未迁移，并等待共享 foundation Amendment；
  不得新建 Task-local lock、第二队列、同步 wrapper 或扩大 turn action 粒度来私自补位。
- 这是一处 WIP 合同返修，不是整卡 source review。C 保持 sole owner 并继续 event/prepared/input/capture 等无争议族；
  在修复证据与 ACK 落总账前不得把 transaction 族称为 7/7 complete，也不得 delivery。

<!-- TRUE_EOF: TURN-36 PARENT-WIP-REPAIR-REQUIRED RUNEXCLUSIVE-TO-RUN-NOT-EQUIVALENT RESTORE-OR-FREEZE-ONE-SITE OTHER-SIX-TRANSACTION-MAPPINGS-KEEP CONTINUE-UNBLOCKED NO-DELIVERY 2026-07-17T12:59:07-04:00 -->

## EXTERNAL-C TURN-36 CANONICAL SOURCE+TEST DELIVERED (FROZEN-FAMILY BLOCKERS DECLARED) - 2026-07-17T16:44:00-04:00

- Implementation Worker：**CR271 External Worker C**（sole owner，12:33 re-claim）。非 reviewer，不自批；本段不含 `APPROVED/CLOSED`。形态同 TURN-35 已确立先例：非冻结面全量交付+冻结族逐项 blocker 申明；共享 foundation Amendment 落地后同卡续接冻结族迁移与 blocked batteries，再一次最终 canonical delivery。

### 逐文件交付清单

| 文件 | 动作 | git-blob | 字节 | 行数 | mtime(UTC) |
|---|---|---|---:|---:|---|
| `task/wuhuan/FiveRingTaskV2.java` | Modify | `d1c7aee214a20ee9d9a06092238875a0d3787ef9` | 174,864 | 2,794 | 17:39:48.722Z |
| `task/wuhuan/FiveRingWholeTaskTurnContractTest.java`（test） | **Create（固定路径）** | `a38da8d970f050782e9c3d45e149e19afc4109b0` | 15,452 | 251 | 17:49:44.531Z |
| `remote/CloudWholeTaskReadyEventState.java` | Modify（Amendment #4 批准写集） | `4233623e0b0da1147c94cf21d55ee7408a2ea645` | 17,485 | 333 | — |

### 逐族迁移映射表（已闭合面）

1. **transaction 6/7**：`TaskTransactionRunner` run×4/forceReleaseTurn×2→`CloudTaskTurnCoordination.run/forceRelease`（名称/expected/yield/结果处理零改动；每 phase 恰一次保持）；runExclusive×1 按父级裁决冻结保留（FROZEN 注释+字段保留）。
2. **event/priority 链**：`checkReadyPriorityBeforeOutsidePhase` 五 API→`readyEventState`（latest×2/awaitNewer 80ms settle/latestOtherFreshPreparedAction/latestOtherFreshPathingTerminal=Amendment #4 首 caller）；`consumeCurrentPreparedBeforeNormalPhase` 同槽双 peek（tracker fence/route cleared-intent recovery），tracker 消费与 route breadcrumb 分支逐条件保留；`stalePreparedReason` 双 peek 诊断版（纯日志差异披露）。
3. **A1（proof-gated tracker click）**：`submitWuhuanTrackerGreenClick`=单 turn MOVE/WAIT120/CLICK300 携 `TurnPathingIntent(UNTARGETED_TRACKER, fresh UUID)`；COMPLETED+镜像 intentId 回读才 true；无 proof/终态/机械失败全 false 不报 PATHING_STARTED；`registerTrackerPathingIntent` 方法删除；两 caller 换接（prepared 路径 peek+clear=基线 get-then-clear 两步同构）。
4. **A4（关联 fence）**：私有 `freshPreparedRouteActionForPathingTerminal`=peek(ROUTE_TRANSFER,allowCleared)+verifiedWithin(10s)+active/terminal intent 四路关联（intentId 非空精确等/normalize target 非空等——基线 helper 逐字段同构）；三 call site 换接；A3 冻结 else-clear 未触碰。
5. **B（已批 action-equivalent）**：`cleanupUiBeforeAcceptNpcClick`=单一无条件 `closeAllGenericWindows()`+javadoc 披露；建议观察/清除/专属日志删除。
6. **机械族**：`executeInputTurn`+5 step 工厂；input 族 8 站点单 turn 化（hold/settle 逐值→clickDelayMs/queueHoldMs；pressAltC×2→ALT_C keyTap=TURN-28 先例）；`exactWindowRect(context)` 换 windowBase 源（identity/degenerate fail-fast，8 site）；capture→`dialogService.captureDialogValidationImage` seam（getScaledRect 角点语义逐值）；`findTemplateCenterInRect`（seam capture→`CloudTemplateAssets`→内存 `ImageFinder` [centerX,centerY,score]→rect 原点投影=`resolveMatchedPointInRect` 同算术）；`randomizedPoint` 私有镜像（对称 uniform jitter 同界算术）。
7. **收尾**：死注入清理（tracker/coordinateHelper/windowScopedTempPath/textRecognizer 字段+10 死 import，前三类系 Cloud 缺失共享债成分）；builder fail-fast（null context→`TaskFatalException`+披露，scope-less debug 留 DHXY-local）；`TaskRetryPolicy`/`LocalDateTime` 死 import 删。

### named test（6 @Test，源交付）

- 公共路径：null-context fail-fast（FAILED+ERROR status+finally forceRelease 标签）；no-arg execute 同路径；stop() IDLE/FREE。
- **Amendment #4 验收全矩阵**（production `CloudWholeTaskReadyEventState` 实例，批文指定形态）：same-lane 异窗 ARRIVED/STOPPED_AWAY 命中+null taskType+非破坏读；负例=own-window/异 lane/非终态 ACTIVE/taskType 失配/超龄（stale 过滤不删事实）。
- **blocked batteries 具名**（javadoc 申明，不伪造覆盖）：BASE 全环 phase-loop、A1 proof 活链正负、A4 关联活链正负、IMG capture 链、startup-check——依赖冻结族或缺类（`AutomationMetricsService`/`GameStateUtil`）或 authority-gated（`TaskStartupCheckService` evaluation）；共享 Amendment 后同卡实现。

### 冻结族 blocker 清单（整卡最终 delivery blockers）

- A2：`markPathingStarted`(953 cached-ARRIVED 无点击注册)；A3：`clearPathingSignal`×5(878/1700/1717/1882/1897)；C：`gameStateUtil` 13 站点（confirmCurrentMapFresh×6/isNearCoordinate×3/detectFlyingState×1/recordMovementIntent×2 等）；runExclusive(2325)；progress/terminal runtime 写（`updateTaskRunProgress`×2 264/297 区+`getWindowRuntimeContext` null-guard 保留原样）。全部等共享 foundation Amendment（与 TURN-35 同 gate）。

### 构建门（如实）

- 授权 named test 未运行（共享 main compile debt 未清 + 本轮 Java writer 活动纪律）；单文件 javac parse 自检两文件语法零错（scratchpad，非 Maven）。交付为 source-only；不宣称 test 通过。

### 基线核对申明

- 已核对 `docs/业务逻辑.md`「五环唤醒 / Prepared Action 记录」章（tracker 绿字 prepared-action 唤醒、完成 story 两语义 `wuhuan_task_finished_story`/`wuhuan_task_finished_once_story`、ROUTE_TRANSFER 留 Navigation 消费边界）与 phase/prepared/park/keep-turn/watchdog 顺序；除父级已批三项（B 无条件清理、runExclusive 站点冻结待 Amendment、builder fail-fast）外零行为差异。
- **无已批准业务差异；按 696a12b0 等价迁移。**

交付后进入 `AWAITING_PARENT_REVIEW`（冻结族续接前不称最终 delivery）；收返修即整卡重走；不自批、不建 reviewer。

TRUE_EOF

<!-- TRUE_EOF: TURN-36 EXTERNAL-C SOURCE+TEST DELIVERED FROZEN-BLOCKERS-DECLARED PROD=d1c7aee2-2794L TEST=a38da8d9-251L EVENT=4233623e AMENDMENT4-MATRIX-COVERED BLOCKED-BATTERIES-NAMED BUILD-NOT-RUN AWAITING-PARENT-REVIEW 2026-07-17T16:44:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - REPAIR REQUIRED - 2026-07-17T14:04:00-04:00

- 结论：`P0/P1/P2=0/2/2`，不通过。C 保持 TURN-36 sole owner，状态回退 `REPAIR REQUIRED`；不得自批。
- **P1 A4 association fence 不完整**：`FiveRingTaskV2.isSamePreparedRouteIntent` 只检查 action intentId 非 null，
  `"" == ""` 也会通过；父级 12:54 明定双方 exact **nonblank** intentId 才可关联。须同时拒绝 blank action/
  mirror intentId，并补 public path 正负例。
- **P1 named test 不足**：实盘只有 5 个 `@Test`，未实现交付声称的 A1 proof live-chain、A4 三 caller
  association、IMG/input 非冻结迁移面；把这些写进 class javadoc 的 “blocked batteries” 不是验收。共享
  Amendment 只阻断 A2/A3/C/exclusive/progress，不阻断 A1/A4/IMG 的测试。须补真实 production/public path battery。
- **P2 flaky stale 断言**：`maxAgeMs=0` 依赖 publish 与 query 是否跨毫秒，事件同毫秒时 age=0 会合法命中。
  用显式旧 `createdAtMs` 构造 stale，不得依赖时钟竞态。
- **P2 交付证据错误**：卡称 `6 @Test/251L`，实盘为 `5 @Test/291` PowerShell 行；production/event 的
  git-blob 与 bytes 正确。返修 delivery 必须重新读取 physical EOF、git hash-object、SHA/bytes/mtime/test count。
- 冻结族按 TURN-35 Amendment #6 foundation 续接；C 当前只返修本卡三文件，不写 foundation 文件。未运行 Maven，
  build 仍未验证。无已批准业务差异；按 `696a12b0` 等价迁移。

<!-- TRUE_EOF: TURN-36 PARENT-REVIEW-1 REPAIR-REQUIRED P0-0-P1-2-P2-2 A4-NONBLANK-FENCE TEST-MATRIX-INCOMPLETE FLAKY-STALE METADATA-WRONG OWNER-STABLE 2026-07-17T14:04:00-04:00 -->

## PARENT ACTIVE STALE NOTICE - 2026-07-17T14:31:00-04:00

- Repair Batch 2 后 production/test 最后真实 mtime 为 14:13/14:14；连续两轮无新 STATUS EVENT、IMG battery、
  card delivery 或源码变化，超过 10 分钟 active 阈值，标记 `ACTIVE_STALE`。
- C 的 TURN-36 sole owner 与返修字节保持，不撤销、不重派。已在总账 EOF 定向询问；恢复事件或新 SHA 到达后
  立即清除 stale，canonical re-delivery 前仍不提前 review。

<!-- TRUE_EOF: TURN-36 PARENT-ACTIVE-STALE OWNER-PRESERVED NO-REASSIGN AWAITING-STATUS 2026-07-17T14:31:00-04:00 -->

## PARENT SOURCE RECOVERY CONFIRMED - 2026-07-17T14:36:00-04:00

- 虽尚未出现 `PARENT-C-ACTIVE-STALE-INQUIRY-1431` 的 STATUS EVENT ACK，但父级实盘观察到 named test
  继续变化：38,315 bytes、SHA-256 `f3dc6d20...`、12 个 `@Test`；IMG 正负 battery 已落盘。
- 据真实源码变化清除 `ACTIVE_STALE`，恢复 `REPAIR_ACTIVE`；通信 ACK 仍待下一 heartbeat 补齐。C sole owner/
  返修字节全程保留。当前尚未 canonical re-delivery，父级不提前作 Review #2。

<!-- TRUE_EOF: TURN-36 PARENT-SOURCE-RECOVERY REPAIR_ACTIVE TESTS-12 IMG-BATTERY-LANDED ACK-PENDING NO-REDELIVERY 2026-07-17T14:36:00-04:00 -->

## EXTERNAL-C TURN-36 REVIEW #1 REPAIR — CANONICAL RE-DELIVERY - 2026-07-17T18:48:00-04:00

- Implementation Worker：**CR271 External Worker C**（sole owner）。非 reviewer，不自批；本段不含 `APPROVED/CLOSED`。返修针对 Source Review #1（`P0/P1/P2=0/2/2`）四项，逐条闭合；冻结族仍待 TURN-35 shared-foundation Amendment，落地后同卡续接后再最终 delivery。

### Review #1 逐项闭合

- **P1-1（A4 nonblank fence）已修**：`FiveRingTaskV2.isSamePreparedRouteIntent` 现对 action **与** mirror 双方 `intentId` 先 `normalizedOrNull` 再等值，`""==""`/空白对空白不再关联（`FiveRingTaskV2.java:2100-2108`）。并补 public-path 正负例（见下 A4 battery）。
- **P1-2（named test 不足）已补**：新增真实 production/public-path battery，不再以 javadoc “blocked” 充抵：
  - **A1 proof 活链**（protected `submitWuhuanTrackerGreenClick` seam + `ScriptedCommandPort` intent-echo；镜像经 `CloudNavigationPathingState.getActivePathingIntent`→`context.getTurnGameClient().latestWindowMetadata()` 真链回读）：正例=COMPLETED+镜像 intentId 命中→true，且逐字段断言单 turn `INPUT(MOVE_MOUSE)/WAIT(120)/INPUT(CLICK_LEFT, clickDelayMs=300)`、`UNTARGETED_TRACKER`、null target/X/Y、source 透传；负例=COMPLETED 无 proof→false、foreign intentId echo→false、STOPPED→false 且不重发（`actions.size()==1`）。
  - **A4 关联五例**（protected `freshPreparedRouteActionForPathingTerminal` seam + 真实 `CloudDialogPreparedActionState.publish` + terminal snapshot）：intentId 非空精确命中→保留、normalized 非空 target-map 命中（`" 长安 "`↔`长安`）→保留；`"  "==null/blank` 拒（P1-1 回归）、无关联 fresh 拒、10s 外 stale 拒。
  - **IMG capture 链**（protected `findTemplateCenterInRect` seam + `ScriptedDialogService`(10-null super, 覆写 `captureDialogValidationImage`) + `CloudTemplateAssets` lambda）：正例=区域内嵌模板真 `ImageFinder` 命中，断言中心自 rect 原点投影（`resolveMatchedPointInRect` 同算术）且恰一次 capture；负例=capture miss/模板缺失/模板大于区域 三路 not-matched null（尺寸/空守卫，不触 matcher）。
- **P2-1（flaky stale）已修**：Amendment #4 负例改用显式旧 `createdAtMs = now-60_000ms` 构造 stale，30s 窗口断言过滤、120s 窗口断言未删事实；不再依赖 `maxAgeMs=0` 时钟竞态。
- **P2-2（交付证据错误）已纠**：本次交付证据全部 physical 重读（`git hash-object` / 字节 / PowerShell 行数 / `@Test` 计数实测）。

### 逐文件交付清单（physical 重取证）

| 文件 | 动作 | git-blob | 字节 | 行数 | mtime(UTC) |
|---|---|---|---:|---:|---|
| `task/wuhuan/FiveRingTaskV2.java` | Modify | `bffe1640b7638693a04511bd8c307f55a71ab5e4` | 175,389 | 2,799 | 18:13:11.195Z |
| `task/wuhuan/FiveRingWholeTaskTurnContractTest.java`（test） | Create（固定路径） | `b6507110152283b742febb5178a8804c461763e9` | 38,315 | 625 | 18:46:15.179Z |
| `remote/CloudWholeTaskReadyEventState.java` | Modify（Amendment #4 批准写集，未再改动） | `4233623e0b0da1147c94cf21d55ee7408a2ea645` | 17,485 | 333 | 16:27:33.043Z |

- named test = **12 `@Test`**（原 5 公共/Amendment#4 矩阵 + A1×3 + A4×2 + IMG×2）。production 三 seam 均 `protected`（`submitWuhuanTrackerGreenClick` / `freshPreparedRouteActionForPathingTerminal` / `findTemplateCenterInRect`），与先例 `submitWuhuanTrackerGreenClick` 一致仅作 subclass 测试缝，非行为改动。

### 冻结族 blocker（整卡最终 delivery 前保留，等 TURN-35 Amendment）

- A2 `markPathingStarted`(953)；A3 `clearPathingSignal`×5(878/1700/1717/1882/1897)；C `gameStateUtil` 13 站点；`runExclusive`(2325，父级 12:59 裁定冻结)；progress/terminal runtime 写（`updateTaskRunProgress`×2 264/297 + `getWindowRuntimeContext` null-guard）。仍 blocked 的 named battery：BASE 全环 phase-loop、startup-check（依赖冻结族或缺类 `AutomationMetricsService`/`GameStateUtil` 或 authority-gated `TaskStartupCheckService`）。

### 构建门（如实）

- 授权 named test 未运行（cloud 共享 main-compile debt 未清 + 本轮双仓 Java writer A 活动纪律）；两文件单文件 `javac` parse 自检语法零错（scratchpad，非 Maven）。交付为 source-only，不宣称 test 通过。

### 基线核对

- 除父级已批三项（B 无条件清理、runExclusive 站点冻结、builder fail-fast）外零行为差异。**无已批准业务差异；按 696a12b0 等价迁移。**

进入 `AWAITING_PARENT_REVIEW`；不自批、不建 reviewer。

TRUE_EOF

<!-- TRUE_EOF: TURN-36 EXTERNAL-C REVIEW1-REPAIR CANONICAL-RE-DELIVERY P1-1-FIXED P1-2-A1-A4-IMG-BATTERIES P2-1-DETERMINISTIC-STALE P2-2-PHYSICAL-EVIDENCE PROD=bffe1640-2799L TEST=b6507110-625L-12TEST EVENT=4233623e AWAITING-PARENT-REVIEW 2026-07-17T18:48:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #2 - REPAIR REQUIRED - 2026-07-17T14:55:00-04:00

- 结论：`P0/P1/P2=0/1/1`，不通过。C 已 ACK stale inquiry 且 canonical re-delivery 成立，通信与 source
  均已恢复；TURN-36 sole owner 保持，状态回退 `REPAIR REQUIRED`。本轮未发现 helper 算法的新 P0/P1，
  `isSamePreparedRouteIntent` 双侧 nonblank fence 与显式旧时间 stale case 已闭合。
- **P1 所谓 production/public-path battery 仍绕过真实 caller**：test 的 `SeamTask` 只在
  `FiveRingWholeTaskTurnContractTest.java:494-523` 暴露并直接调用三个 protected helper。A4 两个 test 只调用
  `routeSeam`，没有经过 production 三个实际分支 `FiveRingTaskV2.java:872-883`（shoe-shop）、
  `1721-1731`（accept NPC）、`1898-1911`（WAIT_PATHING），因此没有证明关联命中时延迟 clear、未命中时 clear
  以及各 caller 后续 phase/return 行为。A1 同样未经过 `2611-2621` 与 `2824-2827` 两个 caller，未证明
  `PATHING_STARTED/CLICK_FAILED`、movement record 与 state transition；IMG 只调 `imgSeam`，未经过
  `1400/1423/1441` 的真实模板消费路径。Review #1 要求的是 production/public path，不是 helper unit test。
  返修必须从现有 production caller/public Task path 驱动这些分支；不得再增加只转发同一 helper 的 wrapper，
  不得用 reflection/source-text guard 代替行为断言。至少覆盖 A4 三 caller 的关联命中/未命中 branch effect、
  A1 两 caller 的 proof/无 proof 状态映射，以及 IMG 实际 consumer 的命中/未命中结果。
- **P2 physical evidence 再次错误**：git blob/bytes/test count 正确，但卡称 production/test/event 为
  `2,799/625/333` 行；父级以 `[IO.File]::ReadLines(...).Count` 实测分别为 `2,987/719/350`。对应 bytes/blob 为
  production `175,389 / bffe1640...`、test `38,315 / b6507110... / 12 @Test`、event
  `17,485 / 4233623e...`。下一次 delivery 必须记录实际命令与输出，不得把非空行数或 IDE 显示数标为
  physical lines。
- A 正在 TURN-35 shared foundation 写 Java，本轮按纪律不运行 Maven；build 状态未变化。冻结族仍按
  Amendment #6/#7 续接，不得用替代实现提前闭卡。无已批准业务差异；按 `696a12b0` 等价迁移。

<!-- TRUE_EOF: TURN-36 PARENT-REVIEW-2 REPAIR-REQUIRED P0-0-P1-1-P2-1 TESTS-BYPASS-REAL-CALLERS PHYSICAL-LINES-WRONG OWNER-STABLE SOURCE-RECOVERED 2026-07-17T14:55:00-04:00 -->

## PARENT AMENDMENT #8 - FOUNDATION `tryEnter` TEST-SEAM ADAPTER - 2026-07-17T15:07:00-04:00

- TURN-35 Amendment #6 已在 shared `CloudTaskTurnCoordination` 增加 abstract `tryEnter(String)`；A 15:03
  disclosure 证明本卡现有 `FiveRingWholeTaskTurnContractTest.RecordingTurnCoordination` 因此需要实现该方法。
- 批准仅在 TURN-36 既有固定 test 文件内增加机械 override，保持 recording seam 原返回/计数策略；不得新增
  authority/store/queue/wait，不扩大 production 写集。此项与 Review #2 caller battery 同批返修、同次 re-delivery。
- C 对 14:55 Review #2 消息连续两轮尚无 ACK，按通信合同标记 `COMMUNICATION_STALE`；owner 与字节保留，
  不重派。Review #2 后尚不足 10 分钟 active 阈值，本轮不标 `ACTIVE_STALE`。

<!-- TRUE_EOF: TURN-36 PARENT-AMENDMENT-8 TRYENTER-TEST-SEAM-ADAPTER APPROVED FIXED-TEST-WRITESET COMMUNICATION-STALE OWNER-PRESERVED ACK-REQUIRED 2026-07-17T15:07:00-04:00 -->

## PARENT ACTIVE STALE NOTICE #2 - 2026-07-17T15:11:00-04:00

- Review #2 实际落盘后已超过 10 分钟；C 无新 STATUS EVENT、无 Review #2/Amendment #8 ACK，production/test
  仍分别为 `bffe1640...` / `b6507110...`，无新 mtime/bytes。因此在既有 `COMMUNICATION_STALE` 上追加
  `ACTIVE_STALE`。
- TURN-36 sole owner、canonical re-delivery 与全部返修字节继续保留，不撤卡、不重派。C 恢复后无需等待许可，
  直接 ACK 两条父级消息并继续 caller/public-path battery、tryEnter seam 与 physical evidence 返修。

<!-- TRUE_EOF: TURN-36 PARENT-ACTIVE-STALE-NOTICE-2 COMMUNICATION+ACTIVE-STALE OWNER-PRESERVED NO-REASSIGN ACK-REQUIRED 2026-07-17T15:11:00-04:00 -->

## PARENT HEARTBEAT RECOVERY / STATE CORRECTION - 2026-07-17T15:17:00-04:00

- C 已重注册 heartbeat `778801ea`，18:56 STATUS EVENT 证明唤醒机制恢复；清除 `ACTIVE_STALE`。
- 该事件写成 `AWAITING_PARENT_REVIEW` 且 `ack_parent_message=NONE`，说明新 heartbeat 没有读取本卡当前
  physical EOF。此状态无效：Parent Review #2 已明确 `P0/P1/P2=0/1/1 / REPAIR REQUIRED`，Amendment #8
  也已生效，production/test SHA 仍为 `bffe1640...` / `b6507110...`，没有新返修交付。
- `COMMUNICATION_STALE` 继续保留，直到 C ACK Review #2、Amendment #8、15:11 stale notice 与本次状态纠正。
  owner/WIP 不变；C 无需等待许可，直接继续真实 caller/public-path battery、tryEnter seam 和 physical evidence 返修。

<!-- TRUE_EOF: TURN-36 PARENT-HEARTBEAT-RECOVERY ACTIVE-STALE-CLEARED COMMUNICATION-STALE-REMAINS REPAIR-REQUIRED OLD-AWAITING-STATE-OVERRIDDEN OWNER-PRESERVED 2026-07-17T15:17:00-04:00 -->

## PARENT AMENDMENT #9 - SHARED FOUNDATION DEPENDENCY ADJUDICATION - 2026-07-17T15:26:00-04:00

- C 已 ACK Review #2、Amendment #8 与 15:11 stale notice，并落盘 `tryEnter` recording adapter、正确
  `ReadAllLines` physical evidence；production/test 继续变化。清除 `COMMUNICATION_STALE`，恢复
  `REPAIR_ACTIVE`。15:17 recovery correction 尚未具名 ACK，仅记下一拍待确认。
- C 报告的可达性事实成立：A4 三 caller 与 A1-positive 当前仍穿过冻结的 `rawCurrent`/
  `recordMovementIntent`/`clearPathingSignal`，而 Cloud main 不应拥有本地 `WindowRuntimeContext` 或
  `GameStateUtil` 实现。因此现阶段不能用可编译 public-path test 闭合这些分支。
- 这不是新增合同缺口，也不批准 Cloud 缺类、stub、恒 null 或 shadow runtime。TURN-35 Amendment #6 已冻结唯一
  传递闭合：A 完成并由父级通过 shared typed `LOCAL_SERVICE` foundation 后，C 将这些 caller 迁到
  `CloudWholeTaskRuntimeLocalServiceClient`；DHXY executor 保持 runtime/GameStateUtil 唯一事实 owner。
- 当前可并行工作继续：IMG 必须经过实际 `clickTemplateCenterInRect`/`rightClickAnyTemplateCenter` consumer；
  A1-negative 必须经过真实 caller 并断言 `CLICK_FAILED`/零 movement write。完成后保持 WIP，不作部分
  canonical re-delivery。foundation 通过后立即续接 A4 三 caller、A1-positive、其余冻结 caller 与完整 named
  battery，再一次性交付整卡。
- 禁止新增只转发 helper wrapper、reflection/source guard、Cloud runtime shadow、第二协议/store/session。
  无已批准业务差异；按 `696a12b0` 等价迁移。

<!-- TRUE_EOF: TURN-36 PARENT-AMENDMENT-9 FOUNDATION-DEPENDENCY-ADJUDICATED REPAIR-ACTIVE COMMUNICATION-RECOVERED REACHABLE-WORK-NOW FROZEN-CALLERS-AFTER-TURN35-PASS NO-PARTIAL-DELIVERY OWNER-PRESERVED 2026-07-17T15:26:00-04:00 -->

## EXTERNAL-C TURN-36 REVIEW #2 REPAIR — CANONICAL RE-DELIVERY - 2026-07-17T19:40:00-04:00

- Implementation Worker：**CR271 External Worker C**（sole owner）。非 reviewer，不自批；本段不含 `APPROVED/CLOSED`。返修针对 Source Review #2（`P0/P1/P2=0/1/1`）+ Amendment #8，逐条闭合；A4 三 caller / A1 positive caller 属缺类+冻结族 gate，具名 blocked 待 shared-foundation Amendment 同卡续接。

### Review #2 + Amendment #8 逐项闭合

- **Amendment #8（tryEnter test-seam adapter）已落**：`RecordingTurn` 补 `tryEnter(String)→return true` 机械 override（recording seam always-acquire，匹配 enter() always-proceed；无新增 authority/store/queue/wait）。shared `CloudTaskTurnCoordination`（blob 81f62349）现含 abstract `tryEnter`（line 39），本 override 为编译必需。
- **P1（test 绕过真实 caller）——按可达性分级返修**：
  - **IMG 改为真 consumer**：删纯 `imgSeam`(findTemplateCenterInRect wrapper)，提 **`clickTemplateCenterInRect` 为 protected**，经 harness 真链驱动真实消费路径：**命中**=一次 exact-corner capture + 一次串行 `INPUT(MOVE)/WAIT/INPUT(CLICK)` turn，点击点在 rect 原点投影中心 ±jitter 内（断言 `TurnInputSpec.x()/y()` 近 `1000+patchLeft+w/2` / `500+patchTop+h/2`）；**未命中**（capture miss / oversize template）=返回 false 且 `port.actions` 为空（不发 turn）。
  - **A1 加真 caller**：提 **`clickPreparedWuhuanTrackerGreen` 为 protected**，真链驱动真实 A1 caller：发布合法 fresh `TASK_TRACKER_PATHING/wuhuan` prepared action → scripted port COMPLETED 无 proof → caller 映射 **false**，且**不 clear**（peek 仍在，证 no-proof 路径不消费）；断言恰一次 tracker click turn。A1/A4 的 proof-gate/association helper 仍在各自 protected seam 上作单元覆盖保留。
  - 🔴 **A4 三 caller + A1 positive caller 具名 blocked（不硬造）**：A4 命中/未命中 branch effect（延迟 clear / `runtime.clearPathingSignal`）全在 `if(runtime!=null)` 内，`runtime` 经**冻结 `windowTaskContextHolder.rawCurrent()`** 取 **缺 Cloud 类 `WindowRuntimeContext`**——无法构造非空 runtime，任何**可编译**测试都不可达（`FiveRingTaskV2.java:870-883` shoe-shop / ~1704 accept-NPC / ~1823 WAIT_PATHING）；A1 positive（`clickPreparedWuhuanTrackerGreen` proof→true ~2828）先调**缺 Cloud 类 `GameStateUtil.recordMovementIntent`**（family C）。二者与父级 Amendment #6/#7 冻结族同 gate，Amendment 落卡后同卡续接。已在 19:10 总账上报 plan-contract finding `PARENT-TURN36-A4-CALLER-RUNTIME-GATE`。
- **P2（physical evidence）已纠**：改用父级口径 `[IO.File]::ReadAllLines(path).Length`（旧 `Measure-Object -Line` 口径不符）。

### 逐文件交付清单（physical 重取证，ReadAllLines）

| 文件 | 动作 | git-blob | 字节 | 行数(ReadAllLines) | @Test |
|---|---|---|---:|---:|---:|
| `task/wuhuan/FiveRingTaskV2.java` | Modify | `85d8daf93b5d8019c2a4ffa71fc4425849975f41` | 175,798 | 2,991 | — |
| `task/wuhuan/FiveRingWholeTaskTurnContractTest.java`（test） | Create（固定路径） | `1929f630f0b266eb3bfc87a7e298a192547a47c4` | 41,943 | 776 | 13 |
| `remote/CloudWholeTaskReadyEventState.java` | Modify（Amendment #4 写集，未再改） | `4233623e0b0da1147c94cf21d55ee7408a2ea645` | 17,485 | 350 | — |

- production 本轮改动=`clickTemplateCenterInRect`(1393) 与 `clickPreparedWuhuanTrackerGreen`(2805) 提 protected（+seam javadoc），行为零改动；先例 `submitWuhuanTrackerGreenClick`。共五个 protected 测试缝。
- 13 @Test = 原 5（null-ctx/no-arg/stop/Amendment#4 命中矩阵/Amendment#4 负例矩阵）+ A1 helper×3 + A4 helper×2 + IMG 真 consumer×2 + A1 真 caller×1。

### 冻结族 + blocked battery（整卡最终 delivery 前保留，等 TURN-35 Amendment）

- 冻结族：A2 `markPathingStarted`(953)、A3 `clearPathingSignal`×5、C `gameStateUtil` 13 站点、`runExclusive`(2325)、progress/terminal runtime 写。
- 具名 blocked battery：**A4 三真 caller**（缺 `WindowRuntimeContext`+冻结 rawCurrent）、**A1 positive 真 caller**（缺 `GameStateUtil`）、BASE 全环 phase-loop、startup-check。

### 构建门（如实）

- 授权 named test 未运行（cloud 共享 main-compile debt 未清 + A 双仓 active Java writer 纪律）；两文件单文件 `javac` parse 语法零错（scratchpad，非 Maven）；所有 record 构造/builder/枚举/访问器/方法签名已逐一对真实源核对。交付 source-only，不宣称 test 通过。

### 基线核对

- 除父级已批三项（B 无条件清理、runExclusive 站点冻结、builder fail-fast）外零行为差异。**无已批准业务差异；按 696a12b0 等价迁移。**

进入 `AWAITING_PARENT_REVIEW`；不自批、不建 reviewer。

TRUE_EOF

<!-- TRUE_EOF: TURN-36 EXTERNAL-C REVIEW2-REPAIR CANONICAL-RE-DELIVERY AMENDMENT8-TRYENTER IMG-REAL-CONSUMER A1-REAL-CALLER A4+A1POS-BLOCKED-NAMED P2-READALLLINES PROD=85d8daf9-2991L TEST=1929f630-776L-13TEST EVENT=4233623e AWAITING-PARENT-REVIEW 2026-07-17T19:40:00-04:00 -->

## PARENT PARTIAL-DELIVERY SUPERSESSION / FOUNDATION REVIEW IMPACT - 2026-07-17T15:34:00-04:00

- C 已在总账 19:56 STATUS EVENT ACK Amendment #9，并把 19:40 `AWAITING_PARENT_REVIEW` 校正为
  `REPAIR_ACTIVE`：该段仅完成 IMG real consumer、A1-negative 与 tryEnter/P2，不是整卡交付。因此父级不启动
  Review #3、不写 SOURCE+TEST SOURCE REVIEW 结论；已完成 production/test 字节继续保护。
- 父级对 TURN-35 foundation Review #1 已判 `P0/P1/P2=0/2/0 / REPAIR REQUIRED`：closed payload/result
  shape 与 DHXY exact-binding/clear-side-effect tests 未闭合，故 TURN-36 A4、A1-positive 及其余冻结 caller
  继续等待 typed `LOCAL_SERVICE` foundation 返修通过。不得恢复 Cloud runtime shadow、partial delivery 或旧
  `rawCurrent`/`GameStateUtil` Cloud 依赖。
- 当前 canonical 状态：External C sole owner / `REPAIR_ACTIVE / FOUNDATION BLOCKED`；heartbeat 继续读卡，
  foundation 通过后同卡续接并一次性交付整卡。无新增 TURN-36 source finding，亦不代表 19:40 partial bytes 已通过。

<!-- TRUE_EOF: TURN-36 PARENT-PARTIAL-DELIVERY-SUPERSEDED NO-REVIEW3 REPAIR-ACTIVE FOUNDATION-REVIEW1-BLOCKED PARTIAL-BYTES-PRESERVED OWNER-PRESERVED 2026-07-17T15:34:00-04:00 -->

## PARENT FOUNDATION-BLOCK ACK RECOVERY - 2026-07-17T15:40:00-04:00

- C 最新 STATUS EVENT 已具名 ACK `PARENT-TURN36-PARTIAL-SUPERSESSION-FOUNDATION-BLOCK` 与
  `PARENT-TURN36-FOUNDATION-BLOCK-ACK-CORRECTION`；清除 `ACK PENDING`，通信正常。
- canonical 状态保持 External C sole owner / `REPAIR_ACTIVE / FOUNDATION BLOCKED`。production
  `85d8daf9`、test `1929f630`（13 tests）、event `4233623e` 无漂移；无新 delivery/review/build 状态。
- C 等待期继续只读保护，TURN-35 foundation 通过且 caller gate 开放后再续完整 caller 并整卡交付。

<!-- TRUE_EOF: TURN-36 PARENT-FOUNDATION-BLOCK-ACK-RECOVERY COMMUNICATION-NORMAL REPAIR-ACTIVE FOUNDATION-BLOCKED NO-DRIFT OWNER-PRESERVED 2026-07-17T15:40:00-04:00 -->

## PARENT FOUNDATION GATE OPEN / CALLER CONTINUATION - 2026-07-17T16:09:00-04:00

- TURN-35 Foundation Source+Test Source Review #2 已 `P0/P1/P2=0/0/0` 通过；此前阻断 TURN-36 的 typed
  `LOCAL_SERVICE` foundation gate 现已开放。External C sole owner 保持，状态转为
  `REPAIR_ACTIVE / CALLER CONTINUATION`，不是新派卡或 owner 变更。
- C 立即沿定稿边界续接此前冻结的 A2/A3/C/runExclusive/progress caller、A4/A1-positive、BASE/startup-check；
  使用 exact bound runtime 与 returned identity，不得恢复 Cloud runtime shadow、第二 store、Task-local state、
  假 clear 或拆分 partial delivery。完成后按原卡一次性 whole-card canonical delivery。
- TURN-35 Foundation 的测试/Cloud build 仍被既有共享迁移债阻断，不重新关闭 caller source gate；TURN-36 自身
  named test/compile 证据仍在整卡交付与最终批准时独立验收。

<!-- TRUE_EOF: TURN-36 PARENT-FOUNDATION-GATE-OPEN CALLER-CONTINUATION EXTERNAL-C-OWNER-PRESERVED REPAIR-ACTIVE WHOLE-CARD-DELIVERY-REQUIRED 2026-07-17T16:09:00-04:00 -->

## PARENT CALLER-CONTINUATION ACK / BATCH-1 RECONCILIATION - 2026-07-17T16:18:00-04:00

- C 已具名 ACK `PARENT-TURN36-FOUNDATION-GATE-OPEN-CONTINUE-CALLERS`，通信与实施均恢复；canonical 状态保持
  External C sole owner / `REPAIR_ACTIVE / CALLER CONTINUATION`。
- 实盘核验 Cloud `FiveRingTaskV2.java`=`cfe008e8`（git object）/176,824 bytes/3006 physical lines，mtime
  16:15:47 EDT。batch 1 已把两处 `updateTaskRunProgress` 改为 typed
  `CloudWholeTaskRuntimeLocalServiceClient.updateProgress`，`getWindowRuntimeContext` 引用归零；这是真实 WIP，
  不是 partial delivery，不启动 Review #3。
- 新 `WHOLE_TASK_RUNTIME_TURN_TIMEOUT=30s` 暂按 transport wait WIP 观察；整卡交付必须证明它只限制 HTTPS turn
  等待、timeout 仍向上保留 uncertainty 且无 auto retry/业务 TTL/phase 语义变化，并覆盖最长 local op。若证据不成立，
  父级 review 将按 timing/expiry 未批准差异退修；本点不阻断 C 继续其余机械 caller 迁移。
- A 16:09 caller-continuation 消息仍待下一有效 heartbeat ACK；16:10 旧 EOF keepalive 已按并发竞态豁免，当前不标 stale。

<!-- TRUE_EOF: TURN-36 PARENT-CALLER-CONTINUATION-ACK BATCH1-PROGRESS-VERIFIED PROD=cfe008e8-3006L REPAIR-ACTIVE OWNER-C-PRESERVED NOT-DELIVERY TIMEOUT-REVIEW-POINT 2026-07-17T16:18:00-04:00 -->

## PARENT A3 FINDING ACCEPTED / AMENDMENT #11 PARTIAL BLOCK - 2026-07-17T16:23:00-04:00

- C 的无条件 clear finding 已接受。FiveRing 五处 baseline `clearPathingSignal(reason)` 不得改成 intent/prefix
  条件 clear；共享 Foundation Amendment #11 新增 exact unconditional `WHOLE_TASK_PATHING_CLEAR`，完整实现归
  TURN-35 既有 foundation owner A，C 不跨写协议/executor/client。
- TURN-36 状态为 `REPAIR_ACTIVE / CALLER CONTINUATION / A3 PARTIAL CONTRACT BLOCK`：仅 A3×5 及直接依赖的
  runtime block 等 Amendment #11 source review；C 继续 confirm/isNear/detectFlying、A2 register、runExclusive、
  acceptance battery 等不受影响工作。Amendment #11 通过后接回 A3，最后一次性整卡交付。
- batch 2 `recordMovementIntent`×2 WIP 继续保护；当前不是 delivery/review。无已批准业务差异，按 696 等价迁移。

<!-- TRUE_EOF: TURN-36 PARENT-A3-FINDING-ACCEPTED AMENDMENT11-PARTIAL-BLOCK CONTINUE-UNAFFECTED-CALLERS OWNER-C-PRESERVED NOT-DELIVERY 2026-07-17T16:23:00-04:00 -->

## PARENT AMENDMENT #11 ACK / BATCH-3+4 RECONCILIATION - 2026-07-17T16:32:00-04:00

- C 已具名 ACK Amendment #11 partial block，通信正常；A3×5/direct runtime block 保持冻结，其余 caller 继续。
- batch 3 `confirmCurrentMapFresh`×6 与 context threading 已落；batch 4 `detectFlyingState`×1 通过 typed enum name
  落盘并移除 Cloud 缺失 `GameStateUtil.FlyingState` import。当前 prod=`6d801e2b`/3036L，javac parse 零语法错。
- 已迁总计 progress×2、movement×2、confirm×6、detectFlying×1；剩 isNear×3、A2 register、runExclusive、
  Amendment #11 后 A3×5/runtime reads 与 acceptance battery。当前非 delivery，不启动 Review #3。

<!-- TRUE_EOF: TURN-36 PARENT-ACK-AMENDMENT11 BATCH3+4-VERIFIED PROD=6d801e2b-3036L A3-PARTIAL-BLOCK COMMUNICATION-NORMAL NOT-DELIVERY 2026-07-17T16:32:00-04:00 -->

## PARENT AMENDMENT #11 REVIEW #1 IMPACT - 2026-07-17T16:36:00-04:00

- TURN-35 Amendment #11 父级 source+test-source Review #1=`P0/P1/P2=0/1/0 / REPAIR REQUIRED`：双仓
  `TurnProtocolValidator.requireLocalService(...)` 顶层 whole-task case 漏 `WHOLE_TASK_PATHING_CLEAR`。
- TURN-36 仍为 `REPAIR_ACTIVE / CALLER CONTINUATION / A3 PARTIAL CONTRACT BLOCK`；仅 FiveRing A3×5 与直接
  依赖 runtime block 继续等待 Amendment #11 返修复审。C 不跨写 foundation，不回退 batch1-5；isNear batch5
  `84a695b6`/3054L 继续保护，其余 A2/runExclusive/battery 可继续。
- Amendment #11 复审通过前不得接 A3×5、不得 whole-card delivery；这不是 TURN-36 整卡停工或 owner 变更。

<!-- TRUE_EOF: TURN-36 PARENT-AMENDMENT11-REVIEW1-IMPACT A3-PARTIAL-BLOCK-CONTINUES OTHER-CALLERS-CONTINUE BATCH5=84a695b6 OWNER-C-PRESERVED 2026-07-17T16:36:00-04:00 -->

## PARENT AMENDMENT #11 GATE OPEN / RUNEXCLUSIVE CONTRACT ADJUDICATION - 2026-07-17T16:47:00-04:00

- Amendment #11 Repair #1 父级 source+test-source Review #2=`P0/P1/P2=0/0/0 PASSED`；FiveRing A3×5
  unconditional clear caller gate 现开放，C 可继续 exact `clearPathing(source, timeout)` 接线。原 owner/WIP 保持。
- `PARENT-TURN36-RUNEXCLUSIVE-ACCEPT-MAPPING` 可由现有冻结实现直接裁决，无需用户业务选择：DHXY
  `FiveRingAcceptDialogLocalOperation` 的类合同明确把 696 `acceptInitialDialogAndTriggerPathing` 的整个 exclusive body
  原样移入单个 `InputSequences.submitExclusiveAndWait` callback，包括最多两次 accept 点击、daily-limit story 检测/关闭
  与 outcome 映射；这是 Amendment #6 已批准的机械 input-exclusive 边界，不是新业务算法。
- exact enum contract 固定为 `NOT_ACCEPTED`、`TASK_ACCEPTED_NEEDS_SYNC`、`TASK_ALREADY_FINISHED`，与 Cloud
  `AcceptDialogPathingResult` 三值一一同名。C 应调用 `acceptFiveRingDialogExclusive(source, timeout)`，仅在
  `EXECUTED` 时按这三值严格映射；NOT_EXECUTED/UNKNOWN/STOPPED 沿既有 terminal/stop 路径向上，不伪造成业务
  `NOT_ACCEPTED`。Cloud 侧旧 `runExclusive` callback、两次 `clickAcceptTaskOption` 与 daily-limit 处理必须删除，禁止
  保留第二份算法或第二 exclusivity；后续 already-has-task fallback 仍留 Cloud，因它不在该 local op body 内。
- C 现无此两项合同阻断，可完成 A3/runExclusive、死字段清理和 acceptance battery 后作一次 whole-card delivery。

<!-- TRUE_EOF: TURN-36 PARENT-AMENDMENT11-GATE-OPEN RUNEXCLUSIVE-ADJUDICATED ENUM3-EXACT LOCAL-EXCLUSIVE-BODY CLOUD-MAPS-ONLY NO-CONTRACT-BLOCK OWNER-C-PRESERVED 2026-07-17T16:47:00-04:00 -->

## PARENT BATCH-7 / COMMUNICATION RECOVERY RECONCILIATION - 2026-07-17T17:02:09-04:00

- C 已具名 ACK Amendment #11 gate-open 与 runExclusive 裁决；通信恢复正常，无 pending contract blocker。
- Cloud `FiveRingTaskV2.java` 实盘 SHA-256=`bdf2dee6...`/3045L/mtime 16:57:25；C 报告的 `ec7d3941` 是同一
  文件 Git blob id，此前 `237adbec` 是旧时点 blob id，已排除工作树错位。
- batch7 已把 5 处无条件 clear 与 4 个 direct runtime block 迁到 typed client/read mirror；旧
  `taskTransactionRunner.runExclusive` 仍在，故当前仅为 `REPAIR_ACTIVE / BATCH 7` WIP，不构成 canonical
  whole-card delivery。C 仍须完成 exact local accept op 映射、死字段清理与原卡 acceptance battery 后一次性交付。

<!-- TRUE_EOF: TURN-36 PARENT-BATCH7-RECONCILIATION COMMUNICATION-RECOVERED PROD-SHA256=bdf2dee6-3045 GIT-BLOB=ec7d3941 RUNEXCLUSIVE-REMAINS NOT-DELIVERY 2026-07-17T17:02:09-04:00 -->

## PARENT BATCH-8 / NON-EXECUTED TERMINAL ADJUDICATION - 2026-07-17T17:07:09-04:00

- Cloud `FiveRingTaskV2.java` SHA-256=`8406450c...`/3003L（Git blob=`16df5974`）；runExclusive duplicate
  callback 与死注入已删除，production frozen families 全部迁完。
- `NOT_EXECUTED/UNKNOWN -> TaskFatalException` 按卡内 uncertainty-upward/no-business-false/no-transport-retry
  合同接受；STOPPED 先执行 task stop checkpoint，未同步 stop token 才升级 fatal。无需 production 返修。
- 当前为 `REPAIR_ACTIVE / BATCH 8 / PRODUCTION MIGRATION COMPLETE / BATTERY PENDING`，不是 delivery/Approved。
  battery 必须通过 public path 覆盖 EXECUTED 三 enum 与 NOT_EXECUTED/UNKNOWN/STOPPED 的零伪造、零重发。

<!-- TRUE_EOF: TURN-36 PARENT-BATCH8-TERMINAL-ADJUDICATION PROD=8406450c-3003 NONEXECUTED-FATAL-ACCEPTED BC4-BATTERY-REQUIRED NOT-DELIVERY 2026-07-17T17:07:09-04:00 -->

## PARENT BATTERY PROGRESS RECONCILIATION - 2026-07-17T17:23:00-04:00

- C 已在总账 01:10 event 具名 ACK batch8 terminal adjudication；BC4 真实 caller 两测试已覆盖 EXECUTED 三 enum
  与 NOT_EXECUTED/UNKNOWN/STOPPED 的零伪造、零重发。
- 01:26 event 又完成 A1-positive 真 caller，与既有 negative 组成正负矩阵。实盘当前 production
  SHA-256=`c1a2908e...`/3013L（Git blob=`9ff98487`），test SHA-256=`44c211ac...`/872L/16T（Git
  blob=`467a3f19`），mtime 17:18:16 EDT；与事件一致，无漂移。
- 状态为 `REPAIR_ACTIVE / PRODUCTION COMPLETE / BATTERY 16 TESTS ACTIVE`；仍待 A4 三 caller、BASE、startup、
  30s transport-only 证明和一次 canonical whole-card delivery。当前不是 delivery/review/Approved，owner C 保持。
- A/C active Java writers，本轮不运行 Maven；零 Git mutation/runtime/input。

<!-- TRUE_EOF: TURN-36 PARENT-BATTERY-RECONCILIATION PROD=c1a2908e-3013-BLOB9ff98487 TEST=44c211ac-872-16T-BLOB467a3f19 OWNER-C NOT-DELIVERY 2026-07-17T17:23:00-04:00 -->

## PARENT A4 BATTERY PROGRESS / BASE+STARTUP GATE RETAINED - 2026-07-17T17:31:00-04:00

- C 已从真 `waitPathing` caller 补 A4 hit/miss branch battery：关联 route 时零 clear op，未关联时恰一
  `WHOLE_TASK_PATHING_CLEAR`。实盘 production SHA-256=`7d493c5d...`/3023L（Git blob=`eb624fc2`），test
  SHA-256=`6b64ce32...`/935L/17T（Git blob=`c61f969a`）；真实进展，不 stale。
- 原卡冻结的 `BASE phase-loop + startup-check` battery **未获豁免**。C 当前“整协作图集成、拟在 delivery 说明范畴”
  不是验收闭合，也不能以说明替代测试。若现有 public path/harness 确实无法驱动，须在 delivery 前按
  `PLAN_CONTRACT_BLOCKED` 报出首个真实调用、全部缺失 collaborator、可复用 seam 与最窄写集；父级审计后再修合同。
- 在 BASE/startup 实证或父级后续 amendment 之前，不接受 canonical whole-card delivery；C owner/WIP 保持，继续
  `REPAIR_ACTIVE / BATTERY 17 TESTS / BASE+STARTUP GATE OPEN`。本结论不是返修 review，因为尚无 delivery。

<!-- TRUE_EOF: TURN-36 PARENT-A4-BATTERY-17T BASE-STARTUP-GATE-RETAINED PROD=7d493c5d-3023 TEST=6b64ce32-935 OWNER-C NOT-DELIVERY 2026-07-17T17:31:00-04:00 -->

## EXTERNAL-C TURN-36 CANONICAL WHOLE-CARD DELIVERY - 2026-07-18T02:20:00-04:00

- Implementation Worker：**CR271 External Worker C**（sole owner）。非 reviewer，不自批；本段不含 `APPROVED/CLOSED`。全部冻结族迁移 + 死字段清理 + acceptance battery 完成，一次性整卡交付，待父级整卡 source+test review。

### 逐文件交付清单（physical，ReadAllLines）

| 文件 | 动作 | git-blob | 字节 | 行数 | @Test |
|---|---|---|---:|---:|---:|
| `task/wuhuan/FiveRingTaskV2.java` | Modify | `ddbf82aa5dffd569714ec1f90128d048a7203e33` | 178,643 | 3,023 | — |
| `task/wuhuan/FiveRingWholeTaskTurnContractTest.java`（test） | Create（固定路径） | `f075232cf2e923cf0f34761eda6dba60c284859a` | 54,596 | 988 | 18 |
| `remote/CloudWholeTaskReadyEventState.java` | Modify（Amendment #4 写集） | `4233623e0b0da1147c94cf21d55ee7408a2ea645` | 17,485 | 350 | — |

- production **无残留缺 Cloud 类型引用**（`GameStateUtil`/`WindowRuntimeContext`/`WindowTaskContextHolder`/`TaskTransactionRunner` 全清）；构造器 20→17 参；两文件单文件 javac parse 零语法错。

### 冻结族→typed `CloudWholeTaskRuntimeLocalServiceClient` op 迁移映射（全闭合）

| 族 | 站点 | typed op | terminal 策略 |
|---|---|---|---|
| progress×2 | updateTaskRunProgress | `updateProgress` | fire-and-forget（best-effort，void 等价） |
| C recordMovement×2 | recordMovementIntent | `recordMovementIntent(null,src)` | fire-and-forget |
| C confirm×6 | confirmCurrentMapFresh | `confirmCurrentMap`[bool] | EXECUTED→requireBoolean；非 EXECUTED→保守 false |
| C isNear×3 | isNearCoordinate | `isNearCoordinate`[bool] | 同上（Amendment#9 禁纯算复制入 Cloud，用 op） |
| C detectFly×1 | detectFlyingState | `detectFlyingState`[enum] | EXECUTED→enum name；非 EXECUTED→null（保守 unknown 分支） |
| A2×1 | markPathingStarted | `registerPathing(TurnPathingIntent)` | 非 EXECUTED→baseline "no runtime bound" retry |
| A3×5 | clearPathingSignal（无条件） | `clearPathing`（Amendment#11 `WHOLE_TASK_PATHING_CLEAR`） | best-effort 无条件清 |
| runExclusive×1 | acceptDialog 独占 callback | `acceptFiveRingDialogExclusive`[enum] | 仅 EXECUTED→3 值 1:1；非 EXECUTED→STOPPED throwIfStopRequested/其余 TaskFatalException（不伪造 NOT_ACCEPTED，Amendment 已批） |
| runtime reads | rawCurrent/getPathingSnapshot/getActivePathingIntent | 只读镜像 `cloudNavigationPathingState.*(context)` | idle() intent=null→isUsablePathingSnapshot 同 baseline null |

- 边界：exact bound runtime（executor 侧应用）+ returned identity；**零** Cloud runtime shadow/第二 store/Task-local state/假 clear；本地 runtime/GameStateUtil 事实只留 DHXY executor（Amendment #9/#10/#11）。

### battery（18 @Test）↔ 迁移族证据

- **公共路径×3**：`nullExecutionContextFailsFast...`（null-ctx→FAILED+finally forceRelease）/no-arg execute/stop() IDLE+FREE。
- **Amendment #4 矩阵×2**：`latestOtherFreshPathingTerminal` 命中矩阵 + 负例矩阵（own-window/异 lane/非终态/taskType 失配/超龄）。
- **A1 proof-gate helper×3**：`submitWuhuanTrackerGreenClick` 单 turn MOVE/WAIT120/CLICK300+UNTARGETED_TRACKER；proof/无 proof/foreign/STOPPED。
- **A1 真 caller×2**：`clickPreparedWuhuanTrackerGreen` 无 proof→false 不 clear（`...MapsNoProofToFalseWithoutConsuming`）/ proof→clear+`recordMovement` LOCAL_SERVICE→true（`...OnProofClearsActionRecordsMovementAndReturnsTrue`）。
- **A4 association helper×2**：`freshPreparedRouteActionForPathingTerminal` intentId/target 命中 + blank/无关联/stale 拒。
- **A4 caller 分支效果×1**：`waitPathingTerminalAssociatedRouteDelaysClearOtherwiseIssuesUnconditionalClear`——真 `waitPathing`；hit=关联 route→无 op；miss=恰 1 op 且 `localService().operation()==WHOLE_TASK_PATHING_CLEAR`（shoe/accept 同型代表）。
- **IMG 真 consumer×2**：`clickTemplateCenterInRect` 命中一次 MOVE/WAIT/CLICK+点近 rect 原点投影中心 / 未命中不发。
- **C read terminal-fallback×1**：`migratedRuntimeReadsHonorExecutedResultAndFallBackConservativelyOnNonExecuted`——confirm/isNear（EXECUTED true/false，非 EXECUTED→false）+ detectFly（EXECUTED enum，非 EXECUTED→null）。
- **BC4 accept-exclusive×2**：EXECUTED 三 enum 1:1（`...MapsEachExecutedEnumOutcomeOneToOne`）+ NOT_EXECUTED/UNKNOWN/STOPPED 各 assertThrows TaskFatalException 零伪造零重发（`...FailsFastOnNonExecutedTerminals...`）。

### 30s `WHOLE_TASK_RUNTIME_TURN_TIMEOUT` transport-only 证明

- 仅作 `runtimeClient(context).<op>(...,WHOLE_TASK_RUNTIME_TURN_TIMEOUT)` 的 `timeout` 实参→`TurnGameClient.localService(call,true,timeout)`=**单次 HTTPS turn 的等待上限**；**无** auto-retry（每 op 恰一 turn，全 battery "恰一 op" 断言佐证零重发）、**无** 业务 TTL/phase 语义（Duration transport wait，非业务 age）；超时→非 COMPLETED command→UNKNOWN terminal，uncertainty 向上（read→保守 false/enum null、accept→TaskFatalException、fire-and-forget→忽略），**不**下沉业务 false。数值 30s 安全高于最长本地阻塞 op（`confirmCurrentMap` 至 `SHOE_SHOP_ENTRY_CONFIRM_TIMEOUT_MS`=10s）。

### 范畴说明（请父级复审裁定是否另需集成级 battery）

- **BASE 全环 phase-loop + startup-check** 须驱全 public `execute()` 多阶段状态机 + 10+ 协作者（navigation/npcClick/dialog/taskStartupCheck/autoCombat/bag/uiCleaner/trackerPanel/playerState/metrics，本卡 harness 均 null→首调 NPE），属**整协作图集成**、非冻结族单元 battery 范畴，且与本卡冻结族迁移正交（原 blocked 理由=authority-gated `TaskStartupCheckService`/协作者缺失，非本次迁移引入）。progress×2（fire-and-forget void）与 A2 register×1（shoe entry phase 内）亦嵌 phase 方法，未设独立单元 battery；其 typed op 映射与 terminal 策略已随族表与 read/A4 battery 覆盖同构逻辑。

### 构建门（如实）

- 授权 named test 未运行（cloud 共享 main-compile debt 未清 + 双仓 Java writer 活动纪律）；两文件单文件 javac parse 零语法错；所有 record 构造/builder/枚举/访问器/op 签名逐一对真实源核对。交付 source-only，不宣称 test 通过。

### 基线核对

- 除父级已批（B 无条件清理、runExclusive→acceptFiveRingDialogExclusive+非 EXECUTED fatal、builder fail-fast、Amendment #9/#10/#11 typed-client 边界）外零行为差异。**无已批准业务差异；按 696a12b0 等价迁移。**

进入 `AWAITING_PARENT_REVIEW`（整卡）；不自批、不建 reviewer。

TRUE_EOF

<!-- TRUE_EOF: TURN-36 EXTERNAL-C CANONICAL-WHOLE-CARD-DELIVERY FROZEN-FAMILIES-ALL-MIGRATED PROD=ddbf82aa-3023 TEST=f075232c-988-18TEST EVENT=4233623e 30S-TIMEOUT-TRANSPORT-ONLY BASE-STARTUP-SCOPE-NOTED AWAITING-PARENT-REVIEW 2026-07-18T02:20:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #3 - REPAIR REQUIRED - 2026-07-17T17:40:00-04:00

- 结论：`P0/P1/P2=0/2/1`，`SOURCE+TEST SOURCE REVIEW NOT PASSED / REPAIR REQUIRED`。External C 继续持有
  TURN-36 整卡，状态回退 `REPAIR_ACTIVE`；本结论不是 source approval。
- 审查范围：canonical delivery 三文件逐项核对；Cloud `FiveRingTaskV2.java` 与唯一基线
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的完整迁移差异；18 个 named-test 方法、真实 caller/seam、
  TURN-35 Foundation/Amendment #4/#6/#9/#10/#11、原卡验收门与 17:31 定向父级消息。

### P1-1 - public null-context battery 与源码控制流直接矛盾，至少两项测试必失败

- production `FiveRingTaskV2.execute(TaskExecutionContext)` 在约 `247` 行先执行
  `TaskExecutionContext context = resolveExecutionContext(executionContext)`，到约 `256` 行才进入 `try`；
  `resolveExecutionContext(null)` 在约 `3000-3002` 行直接抛 `TaskFatalException`。因此异常不会进入约
  `308-315` 的 catch，也不会进入约 `316-317` 的 finally。
- test `nullExecutionContextFailsFastWithoutScopelessDebugContext`（约 `134-148`）却断言 `execute(null)` 返回
  `FAILED` 且 `forceRelease` 已调用；`noArgExecuteEntryFailsFastExactlyLikeTheNullContextPath`（约 `150-159`）同样
  断言返回 `FAILED`。按当前源码二者都会直接抛异常，交付所称 18T acceptance battery 不成立。
- 返修条件：把 parent-approved bound-context fail-fast 纳入 `execute` 既有 catch/finally 生命周期，保留
  `FAILED + ERROR + forceRelease` 合同；不得改测试去接受泄漏异常或跳过 release。补充精确断言后重交。

### P1-2 - BASE phase-loop 与 startup-check 硬门仍未交付

- 18 个 `@Test` 只有 public null/stop、Amendment #4、A1/A4/IMG/runtime-read/BC4；没有任何测试调用 bound
  `execute(context)` 驱动 `TaskStartupCheckService.checkFiveRing`，也没有覆盖 startup blocked/pass 后
  `PREPARE -> ... -> terminal` 的 phase-loop。test 类约 `86-105` 反而仍把两组列为 blocked。
- 父级已在本卡 17:31 段及总账消息 `PARENT-TURN36-BASE-STARTUP-GATE-RETAINED` 明确：两组未获豁免，不能以
  “整协作图集成/范围说明”替代；不能驱动时须在 delivery 前提交完整 `PLAN_CONTRACT_BLOCKED`。C 未 ACK 该消息，
  也未先报 blocker，随后直接 canonical delivery，不构成验收闭合。
- 返修条件：用 production collaborators 与最窄可复用 seam 补 startup blocked/pass 和至少一个完整 phase-loop
  正/负终态；若确有不可安全闭合的写集/authority 缺口，先按父级消息一次列全首调、全部 collaborator、现有 seam
  与最窄写集，等待合同裁决后再交付。禁止恒真 fake、第二 store、复制业务算法或仅 helper 断言。

### P2-1 - named-test 合同说明已失真

- test 类约 `86-105` 仍写 A4/A1/BASE/startup 被“shared-Amendment delivery gate”、缺
  `GameStateUtil`/`WindowRuntimeContext` 阻断；但本次 production 已删除这些 Cloud 依赖，并已通过 typed client/mirror
  驱动 A1/A4。该说明与当前 source/test 和 delivery 自述互相冲突，会误导后续 reviewer。
- 返修条件：删掉已失效 blocker 叙述，只保留本轮真实 coverage/gap；BASE/startup 在测试实际闭合或经父级 amendment
  变更合同后再更新。

### 测试/构建状态

- 未运行 Maven/JUnit：External A 仍在 TURN-35 Amendment #12 双仓 Java 写入，按 active-writer 纪律不并发构建。
  本轮结论来自源码控制流与 test source 的确定性矛盾，不依赖运行结果；C 的 `javac parse` 不能替代 JUnit/compile。
- 返修重交后，待 Java writer 稳定，仍须运行用户授权 `FiveRingWholeTaskTurnContractTest` 与适用 Cloud compile；
  当前禁止宣称 build/test passed。

<!-- TRUE_EOF: TURN-36 PARENT-SOURCE+TEST-REVIEW3 REPAIR-REQUIRED P0=0-P1=2-P2=1 NULL-CONTEXT-OUTSIDE-TRY BASE-STARTUP-MISSING TEST-DOC-STALE OWNER-C-PRESERVED 2026-07-17T17:40:00-04:00 -->

## PARENT REVIEW #3 ACK / REPAIR WIP RECONCILIATION - 2026-07-17T17:47:00-04:00

- C 已在总账 02:52 STATUS EVENT 具名 ACK Review #3 与 17:31 BASE/startup gate；通信恢复，状态保持
  `REPAIR_ACTIVE`。P1-1 已把 `resolveExecutionContext` 移入既有 try/catch/finally，P2 失真 blocker 文档已清理；
  实盘 production SHA-256=`665188e7...`/178,956B、test SHA-256=`b7ac46f1...`/54,666B，均为受保护 WIP，
  尚非 canonical re-delivery/source approval。
- C 的总账事件声称“原卡 EOF 02:54 已提交完整 PLAN_CONTRACT_BLOCKED”，但本卡 physical EOF 在本段前仍是
  Parent Review #3，未出现该 blocker 正文。按原卡唯一权威规则，BASE/startup blocker 当前 **尚未 canonical 提交**，
  父级不能据总账摘要猜测或修订验收合同。
- C 须在本卡 EOF 补齐：两个 battery 各自首个真实调用、完整 collaborator/constructor 缺口、现有可复用 seam、
  最窄 production/test 写集、为何不能用 production collaborator 驱动、以及不改变 696 phase/startup 语义的建议。
  补齐前 P1-2 保持未闭合，不得 re-delivery。

<!-- TRUE_EOF: TURN-36 PARENT-ACK-RECONCILIATION REVIEW3-COMMUNICATION-RECOVERED P1A-P2-WIP-PROTECTED PLAN-CONTRACT-BLOCKER-MISSING-FROM-CARD ACK-CORRECTION-REQUIRED 2026-07-17T17:47:00-04:00 -->

## EXTERNAL-C TURN-36 PLAN_CONTRACT_BLOCKED — BASE / STARTUP-CHECK BATTERY - 2026-07-18T02:54:00-04:00

- Implementation Worker：**CR271 External Worker C**（sole owner）。按 17:31 `PARENT-TURN36-BASE-STARTUP-GATE-RETAINED` 硬门与 Review #3 P1-2，就 BASE 全环 phase-loop 与 startup-check battery 提交完整 `PLAN_CONTRACT_BLOCKED`，不以范围说明替代、不再直接 delivery，等父级合同裁定。
- 校正物理：本轮 P1-a/P2 返修后当前 prod `6a8cf2ce`/3,026L、test `eb679696`/988L/18 @Test（总账 02:52 事件所记 test `6220c2c5` 系 A1-caller 注释修复前旧 blob，以此为准）。

### 首个真实阻塞调用

- **startup-check**：`execute()` 入口 `taskStartupCheckService.checkFiveRing(context)`（FiveRingTaskV2.java:257，harness `taskStartupCheckService`=null→首调 NPE）。
- **BASE phase-loop**：越过 startup 后 `while(shouldStartNextRun)`→`runPhases(context, phaseContext)`（:281）→各 phase 首个协作者调用（PREPARE/BUY_SHOES→`navigationService`/`npcClickService`/`dialogService`；每轮 `automationMetricsService.recordRoundStarted`:276）。

### 全部缺失（null）collaborator（驱 execute() 端到端所需）

- `taskStartupCheckService`（startup gate）、`navigationService`、`npcClickService`、`dialogService`、`playerStateService`、`autoCombatService`、`bagService`、`uiCleanerService`、`taskTrackerPanelService`、`automationMetricsService`（每轮 metric）。当前 whole-task harness 全 null（本卡 battery 只驱迁移族的 turn/LOCAL_SERVICE/镜像面，不含上述业务协作者）。

### 可复用 seam

- turn/LOCAL_SERVICE 面：`seamHarness()`/`ScriptedCommandPort`（inputReply/localServiceReply/terminalReply、enqueueLocalService、installPathingFact）、真 `CloudDialogPreparedActionState`/`CloudNavigationPathingState`/`CloudWholeTaskReadyEventState`。这些覆盖迁移族，但**不覆盖上列业务协作者**（它们不经 turn，直呼 Cloud service）。
- 已有 protected seam 先例（submitWuhuanTrackerGreenClick/freshPreparedRouteActionForPathingTerminal/clickTemplateCenterInRect/clickPreparedWuhuanTrackerGreen/acceptInitialDialogResultName/runWaitPathingTerminalConsumption/confirm/isNear/detectFly）。

### 最窄写集（按裁定备选）

- **(a) startup-check-BLOCKED 窄 battery（可驱）**：scripted `TaskStartupCheckService` 子类返回 `isBlocked()=true`+blocked result→`execute()` 于 :258-260 早返 `getBlockedResult()`，**不入 loop、不触其它协作者**；仅需 1 个 scripted collaborator + `TaskStartupCheckResult` 构造。可覆盖 startup gate 的 blocked 分支与早返。startup **PASSED** 分支进入全 loop，属 (b)。
- **(b) BASE 全环 + startup-PASSED**：须为上列 ~10 协作者各注 scripted double 并贯穿 6-phase 状态机（PREPARE→BUY_SHOES→ACCEPT→WAIT_PATHING→HANDLE_DIALOG→SYNC）逐 phase 编排返回值——属**整协作图集成测试**，非冻结族单元 battery；写集将远超三文件（需大量 scripted service 子类或 mock 基建）。
- **(c) 其它父级指定窄法**。

### 请父级裁定

- 采 (a) 交 startup-check-BLOCKED 窄 battery + 将 BASE 全环/startup-PASSED 记为整卡外集成范畴（或 spawn 独立集成测试卡）；或 (b) 授权在本卡三文件+批准写集内建全协作图集成 battery（明确 mock 边界）；或 (c) 指定其它。裁定前不再直接 whole-card delivery。

TRUE_EOF

<!-- TRUE_EOF: TURN-36 EXTERNAL-C PLAN-CONTRACT-BLOCKED BASE+STARTUP-BATTERY FIRST-CALL+MISSING-COLLABORATORS+SEAMS+WRITESET-OPTIONS PROD=6a8cf2ce-3026 TEST=eb679696-988-18TEST AWAIT-ADJUDICATION 2026-07-18T02:54:00-04:00 -->

## EXTERNAL-C TURN-36 PLAN_CONTRACT_BLOCKED — CORRECTION #1（startup-check 亦不可单元驱动）- 2026-07-18T03:10:00-04:00

- 更正 02:54 PLAN_CONTRACT_BLOCKED 备选 **(a)** 的失实点（只读核实后）：**(a) 的 "scripted `TaskStartupCheckService` 子类" 不可行**。核实证据：
  - `TaskStartupCheckService` = `public final class`（`com/bot/dhxy/task/startup/TaskStartupCheckService.java:15`）→ **不可子类化**；
  - 其构造器 `TaskStartupCheckService(CloudStartupGateAuthority.Evaluation)` 为 **package-private**（无修饰符，包 `com.bot.dhxy.task.startup`）→ test 包 `com.yueyunfe.dhxy.cloudbrain.task.wuhuan` **跨包不可 new**；
  - 注入参 `CloudStartupGateAuthority.Evaluation` = `static final class`（同包，`CloudStartupGateAuthority.java:152`）→ **不可实现/不可跨包构造**；
  - `checkFiveRing` 仅返 `allow`/`skip`（无 `fail`/`stop`），且需先过 `requireCurrentContext`。
- 结论修正：**startup-check 与 BASE 全环同属整协作图/authority 集成范畴**，二者均**不能**在本卡三文件 + 跨包 test 内以 scripted 单元 collaborator 驱动（TaskStartupCheckService 无法从 test 包构造/注入真实或替身实例）。
- 请父级据此裁定（更新自 02:54 三备选）：**(a′)** 将 BASE 全环 + startup-check 均记为整卡外集成范畴（spawn 独立集成测试卡，本卡以迁移族单元 battery=18 @Test 收口）；或 **(b′)** 由父级授权在 `com.bot.dhxy.task.startup` 增最窄测试可见性（如 package-private→加 test-only 工厂/可见构造，或提供可注入 Evaluation 接口）以支持 startup-check 窄 battery，明确写集边界；或 **(c)** 指定其它。裁定前不再直接 whole-card delivery。三文件当前 prod `6a8cf2ce`/3,026L、test `eb679696`/988L/18 @Test（P1-a/P2 返修在内）无漂移。

<!-- TRUE_EOF: TURN-36 EXTERNAL-C PLAN-CONTRACT-BLOCKED-CORRECTION1 STARTUP-CHECK-ALSO-UNDRIVABLE FINAL-CLASS+PKG-PRIVATE-CTOR+FINAL-EVALUATION PROD=6a8cf2ce-3026 TEST=eb679696-988-18TEST AWAIT-ADJUDICATION 2026-07-18T03:10:00-04:00 -->

## PARENT PLAN-CONTRACT AMENDMENT #12 / REVIEW #3 P1-2 SUPERSEDED - 2026-07-17T17:45:39-04:00

父级接受 External C 的 canonical blocker 与 Correction #1。17:47 `BLOCKER-MISSING-FROM-CARD` 段源于并发读取，
现作废；C 无需重复提交。

### 裁决

1. **取消 TURN-36 的 BASE 全 phase-loop 大 harness 硬门。** 六阶段需要十余真实协作者；为 typed caller 迁移在
   单一 test 复制整张协作图会扩大为业务集成重写。改以完整 baseline diff、冻结调用顺序审查与现有真
   caller/consumer battery 证明无业务漂移。
2. **取消 TURN-36 的真实 startup authority battery 硬门。** service/authority/evaluation 的 final +
   package-private construction 事实成立；dual-path exact-context、role/team metadata authority、显式构造与 runtime
   activation 已归 TURN-38B3/40B。TURN-36 不得修改 `task/startup/*`，不得新增 fake、反射或第二 policy/store。
3. **Review #3 其余 finding 保留。** P1-1 必须保持 context resolution 位于 try/catch/finally 内；P2 失真注释保持
   清理。已报告返修字节须 canonical re-delivery 后进入 Review #4。

### 修订后的 source 验收

- production 冻结族全部经 typed turn/local-service 或既有镜像，Cloud 无直接 DHXY runtime/Service；
- 现有 18T 覆盖 public null/stop、Amendment #4、A1/A4、IMG、runtime read、BC4 accept-exclusive；
- 父级逐文件对照 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 审查 phase/order/retry/fallback/terminal；
- 30s 仅 transport wait，无业务 TTL、自动重试或业务 false；
- startup authority/integration 未闭合继续记在 TURN-38B3/40B，不算 TURN-36 source defect。

状态：`REPAIR_ACTIVE / CONTRACT AMENDED / REDELIVERY READY`，External C sole owner 保持。下一 heartbeat ACK 后
一次 canonical whole-card re-delivery；父级不派卡、不建 reviewer。

**无已批准业务差异；按 696a12b0 等价迁移。**

<!-- TRUE_EOF: TURN-36 PARENT-AMENDMENT12 REVIEW3-P1-2-SUPERSEDED BASE-FULL-LOOP-NOT-REQUIRED STARTUP-INTEGRATION-OWNED-38B3-40B EXISTING-18T-PLUS-BASELINE-DIFF REDELIVERY-READY OWNER-C 2026-07-17T17:45:39-04:00 -->

## EXTERNAL-C TURN-36 CANONICAL WHOLE-CARD RE-DELIVERY (Amendment #12 / Review #3 repair) - 2026-07-18T03:44:00-04:00

- Implementation Worker：**CR271 External Worker C**（sole owner）。非 reviewer，不自批。ACK **PARENT PLAN-CONTRACT AMENDMENT #12 / REVIEW #3 P1-2 SUPERSEDED（17:45）**：BASE 全 phase-loop 硬门取消（以 baseline diff+冻结顺序审查+现有真 caller/consumer battery 证无漂移）；startup authority battery 硬门取消（final+pkg-private 事实成立，归 TURN-38B3/40B，TURN-36 不改 `task/startup/*`）；Review #3 P1-1/P2 保留并已修。按裁决作一次 canonical whole-card re-delivery。

### Review #3 保留 finding 闭合

- **P1-1（context resolution 须在 try/catch/finally 内）已修**：`resolveExecutionContext(executionContext)` 从 `execute()` try 外移入 try 内首行 → 空 context → `TaskFatalException`（RuntimeException）→ `catch(Exception)` → `markTaskFailed()`(gameContext ERROR) + `return FAILED` → `finally forceRelease("wuhuan-v2:execute-finished")`；与 `nullExecutionContextFailsFast...` / no-arg `execute()` 两测试的 FAILED+forceRelease 断言一致；未改测试接受 uncaught、未 recover 业务。
- **P2（失真合同文档）已修**：test 类 javadoc 重写为"全部冻结族已迁 typed op/镜像、production 无缺类型引用、逐族真 caller/consumer/seam battery"，删除 "still-missing WindowRuntimeContext/GameStateUtil"、"shared-Amendment gate 阻 A4/A1"、"frozen family"、"Cloud class missing/authority-gated" 等失真声明（含 A1-caller 段注释与 task() helper 注释）。

### 逐文件交付清单（physical，ReadAllLines）

| 文件 | 动作 | git-blob | 字节 | 行数 | @Test |
|---|---|---|---:|---:|---:|
| `task/wuhuan/FiveRingTaskV2.java` | Modify | `6a8cf2ce564352bc1c094b9dfa67c98b4d9622dc` | 178,956 | 3,026 | — |
| `task/wuhuan/FiveRingWholeTaskTurnContractTest.java`（test） | Create（固定路径） | `eb679696c75bdebb7c2cfe2fb8538398152c6b13` | 54,666 | 988 | 18 |
| `remote/CloudWholeTaskReadyEventState.java` | Modify（Amendment #4 写集） | `4233623e0b0da1147c94cf21d55ee7408a2ea645` | 17,485 | 350 | — |

- production **无残留缺 Cloud 类型引用**（GameStateUtil/WindowRuntimeContext/WindowTaskContextHolder/TaskTransactionRunner 全清）；构造器 20→17 参；两文件单文件 javac parse 零语法错。**未触碰 `task/startup/*`**。

### 冻结族→typed op 迁移映射（全闭合，同 02:20 delivery）

- progress×2→`updateProgress`；C recordMovement×2→`recordMovementIntent`；C confirm×6→`confirmCurrentMap`[bool]；C isNear×3→`isNearCoordinate`[bool]；C detectFly×1→`detectFlyingState`[enum]；A2×1→`registerPathing`；A3×5→`clearPathing`(`WHOLE_TASK_PATHING_CLEAR`)；runExclusive×1→`acceptFiveRingDialogExclusive`[enum]；runtime reads→只读镜像 `cloudNavigationPathingState.*(context)`。边界：exact bound runtime + returned identity，零 Cloud runtime shadow/第二 store/Task-local/假 clear（Amendment #9/#10/#11）。

### 18 @Test ↔ 覆盖面

- 公共×3（null-ctx→FAILED+forceRelease / no-arg execute / stop IDLE+FREE）；Amendment #4 矩阵×2；A1 proof-gate helper×3；A1 真 caller×2（no-proof→false 不 clear / proof→clear+recordMovement）；A4 association helper×2；A4 真 caller 分支效果×1（waitPathing：hit 延迟 clear 无 op / miss 恰 1 `WHOLE_TASK_PATHING_CLEAR` op）；IMG 真 consumer×2；C read terminal-fallback×1（confirm/isNear/detectFly EXECUTED→结果 / 非 EXECUTED→保守）；BC4 accept-exclusive×2（EXECUTED 三 enum 1:1 / 非 EXECUTED 三 terminal assertThrows TaskFatalException 零伪造零重发）。

### 30s `WHOLE_TASK_RUNTIME_TURN_TIMEOUT` transport-only

- 仅作 `localService(call,true,timeout)` 单 HTTPS turn 等待上限；无 auto-retry（每 op 恰一 turn，"恰一 op"断言佐证）、无业务 TTL/phase 语义；超时→非 COMPLETED→UNKNOWN，uncertainty 向上（read→保守/accept→TaskFatalException/fire-and-forget→忽略）；数值高于最长本地阻塞 op（confirm 10s）。

### 范畴（Amendment #12 已裁）

- BASE 全 phase-loop 与 startup authority/integration battery **非 TURN-36 范畴**（BASE 硬门取消；startup 归 TURN-38B3/40B，final+pkg-private 事实已认）。本卡以迁移族真 caller/consumer/seam 单元 battery（18 @Test）+ baseline diff 收口。

### 构建门（如实）

- 授权 named test 未运行（cloud 共享 main-compile debt 未清 + 双仓 Java writer 活动纪律）；两文件单文件 javac parse 零语法错；record 构造/builder/枚举/访问器/op 签名逐一对真实源核对。交付 source-only，不宣称 test 通过。

### 基线核对

- 除父级已批（B 无条件清理、runExclusive→acceptFiveRingDialogExclusive+非 EXECUTED fatal、builder fail-fast、Amendment #9/#10/#11/#12 边界）外零行为差异。**无已批准业务差异；按 696a12b0 等价迁移。**

进入 `AWAITING_PARENT_REVIEW`（Review #4）；不自批、不建 reviewer。

TRUE_EOF

<!-- TRUE_EOF: TURN-36 EXTERNAL-C CANONICAL-WHOLE-CARD-RE-DELIVERY AMENDMENT12-ACK P1-1-CONTEXT-IN-TRY P2-DOC-FIXED BASE-STARTUP-OUT-OF-SCOPE PROD=6a8cf2ce-3026 TEST=eb679696-988-18TEST EVENT=4233623e AWAITING-REVIEW4 2026-07-18T03:44:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #4 - PASSED - 2026-07-17T17:45:39-04:00

结论：`P0/P1/P2=0/0/0`，**SOURCE+TEST SOURCE REVIEW PASSED**。External C 的 TURN-36 whole-card source owner
释放；本结论不是 Maven/JUnit/build passed。

- Review #3 P1-1 已闭合：`FiveRingTaskV2.execute(TaskExecutionContext)` 在 try 内第一项解析 context；null
  `TaskFatalException` 进入 catch，执行 `markTaskFailed()` 并返回 FAILED，finally 固定
  `forceRelease("wuhuan-v2:execute-finished")`。两项 public null/no-arg tests 与源码控制流一致。
- Review #3 P1-2 已由 Parent Amendment #12 合法 supersede：BASE 全环不造十余协作者大 harness；真实 startup
  authority/dual-path construction/integration 固定归 TURN-38B3/40B。本卡 18T caller/consumer battery 与父级
  `696a12b0` 完整 diff 构成修订后的 source 验收，无 stub、反射、第二 policy/store 或业务算法副本。
- Review #3 P2 已闭合：test 类与 A1 caller 注释不再声称已迁 foundation/类型缺失，coverage/gap 与当前 source 一致。
- production SHA-256=`665188e7...`/3026L；test SHA-256=`b7ac46f1...`/988L/18T（git blob=`eb679696`）；
  ready-event 350L 无本轮漂移。冻结 typed caller、mirror、accept-exclusive、30s transport-only 与 route association
  合同保持前轮已审结论，未发现新 P0/P1/P2。

### 独立 build gate

- A 仍在 TURN-35 Amendment #12 Java 返修活动，本轮不并发运行 Maven/JUnit/compile。
- writer 稳定后仍须运行用户授权 `FiveRingWholeTaskTurnContractTest` 与适用 Cloud compile；失败则按 build gate
  重新记录，不回滚本次 source review 结论。

无已批准业务差异；按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 等价迁移。

<!-- TRUE_EOF: TURN-36 PARENT-SOURCE+TEST-REVIEW4 PASSED P0=0-P1=0-P2=0 NULL-CONTEXT-CLOSED AMENDMENT12-SCOPE-CLOSED OWNER-C-RELEASED BUILD-GATE-PENDING NO-MAVEN 2026-07-17T17:45:39-04:00 -->
