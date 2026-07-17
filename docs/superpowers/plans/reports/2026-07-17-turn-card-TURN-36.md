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
