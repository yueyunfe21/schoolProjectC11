# 五环 CR227 八点：CR271 审计与落实计划

> 状态：`SOURCE DELIVERED / PARENT REVIEW P0/P1/P2=0/0/0 / FRESH RUNTIME NOT RUN`。
> 2026-07-23 已按本文件 reference ledger 落实第 1-7 点；第 8 点继续复用既有实现。
> 本状态只表示源码审查和 production compile 门通过，不表示已启动 runtime 或完成实机验收。
>
> 命名说明：用户当前工作基线的 CR227 是“五环基线后业务与性能回迁审计”；`DHXY-cr271` 内已有一个无关的“修罗 CR227”。为避免篡改后者，本计划不复用 CR 编号作为 CR271 内卡号。

## 1. 审计范围与事实

- 客户端权威 worktree：`D:\mavenProject\DHXY-cr271`，`thin-client-design`，`59b85e0b`；该工作区当前大量 dirty/untracked，审计全程只读。
- 配套 Cloud 业务源码：`D:\mavenProject\dhxy-cloud-brain`，`navigation-migration`，`3b988ca`；五环 phase machine 已在此仓的 `com.bot.dhxy.task.wuhuan.FiveRingTaskV2`，不能只在客户端补逻辑。
- 业务参考：用户已验证的 `9aa987d1`；不把 HTTPS/云端迁徙、云端 OCR、其他任务或新的 Tracker 点击算法混进本计划。
- 当前 CR271 架构边界：客户端负责 HWND、截图、输入、短期运行事实与 observation；Cloud 保留五环 phase、任务真值、dialog/Tracker 业务解释和 wake 消费。

## 2. 八点逐项审计

| # | 要恢复的语义 | CR271 当前证据 | 审计结论 |
|---|---|---|---|
| 1 | 五环接任务后的首段小地图导航 fire-and-handoff：点击、登记 intent、关小地图后立即放权，不等待本地移动像素证明。 | `NavigationService.isXiuluoStartExitPrepathFireAndHandoff(...)` 只匹配 `xiuluo-v2:start-exit-prepath:currentMap`；五环仍进入 `clickMiniMapLogicalPointForHandoff(...)` 和 `LocalPathingStartProofMechanics` 的移动证明。 | **缺失**。需为五环接任务首段增加窄条件，不泛化到其他导航。 |
| 2 | 等待同时消费 `PATHING_TERMINAL`、`PREPARED_ACTION_READY`、`TASK_TRACKER_NEGATIVE_READY`；negative 只能调度/重读。 | `FiveRingTaskV2.checkReadyPriorityBeforeOutsidePhase(...)` 当前等待/消费的是 `TASK_ATTENTION_REQUIRED`，并只对其他窗口的 prepared/terminal 做优先级让步；没有五环当前窗口的三类事件合同，也没有 `TASK_TRACKER_NEGATIVE_READY` 引用。 | **缺失**。需在 Cloud ready-event 消费与五环 wait contract 中补齐，不能把 negative 映射为完成。 |
| 3 | `STOPPED_AWAY` 按地图分流：非`大雁塔二层`清理未定向 Tracker intent 后直达 `SYNC_TASK_PANEL`；交付地图仍走给予物品/dialog。 | `FiveRingTaskV2.waitPathing(...)` 对全部 `STOPPED_AWAY` 都转 `HANDLE_DIALOG`；没有`大雁塔二层`判断。 | **缺失**。这是当前可见长等待的直接来源。 |
| 4 | 战斗进入和战斗恢复都清已消费五环 Tracker intent，battle radar 仍是战斗真值。 | `CloudWholeTaskObserver.probeCombat(...)` 只在 `WUBEI`、`XIULUO_V2` 战斗进入时发 cleanup；五环没有 entry cleanup。`FiveRingTaskV2.clearTrackerPathingIntentAfterCombatRecovery(...)` 只覆盖恢复侧。 | **缺失一半**。需补五环进入边界的同源 prefix cleanup，并保留已有恢复侧。 |
| 5 | 点击接任务 option 不等于已接；只有实际出现五环 Tracker title 后才确认。 | `FiveRingPhaseContext` 的注释已写出该规则；但 `acceptTask(...)`、`tryAcceptInitialTaskFromCurrentScreen(...)` 在 `TASK_ACCEPTED_NEEDS_SYNC` 后立即调用 `withTaskAccepted(...)`。 | **缺失且存在代码/注释矛盾**。需将 option click 保持为“待 title 确认”。 |
| 6 | 战斗中只比较缓存五环 Tracker 小 ROI 作快速恢复证据，且仅在可信战斗状态与 prepared/完成 guard 下使用。 | `FiveRingPhaseContext` 仍保留 `wuhuanTrackerCombatBaselineImage` 等字段；`FiveRingTaskV2` 没有 capture/compare 使用点。 | **缺失**。现为未接线的状态字段，不能当作已实现。 |
| 7 | 接任务时预热当前位置；无前台输入的 outside phase 在实际耗时工作前放权。 | `runPhases(...)` 已将 `ACCEPT_TASK`、`WAIT_PATHING`、`HANDLE_DIALOG`、`SYNC_TASK_PANEL` 放入 outside path；但 `releaseHeldTurnAfterOutsidePhaseYield(...)` 在 phase body 运行后才 release，上一短 phase 若保有 turn，昂贵读仍可能先占用。没有接任务位置 prewarm。 | **部分具备，仍需落实**。保留既有 outside 架构，只补提前释放与 prewarm。 |
| 8 | Tracker title 缺失时先做真实 dialog/完成模板判断，再决定结束或继续；negative 不能视为完成。 | `syncTaskPanel(...)` 在 tracker miss/no-green 时调用 `tryHandleAcceptReturnedDialogAfterTrackerMiss(...)`，后者检查 `already-has-task` option 和完成 story；其余情况重读/失败，不直接完成。 | **已有等价实现**。只需在第 2 点接入 negative 时复用该分支，禁止另造完成捷径。 |

## 3. 最小实施方案

### 3.0 锁定 reference ledger（禁止按文字重写）

所有下列行为的唯一业务 reference 是 `D:\mavenProject\DHXY` 的已验证提交
`9aa987d1`。实施者必须先对该提交的对应方法逐段做语义迁入，再适配 CR271 的
Cloud/local protocol；不得重新设计判断条件、source 名称、回退顺序或完成语义。

| 八点 | `9aa987d1` 的精确 reference | CR271 的迁入落点 |
|---|---|---|
| 1 | `NavigationService.isImmediateMiniMapFireAndHandoff(...)`：仅 `wuhuan-v2:acceptNpc:navigate:currentMap` 到长安 `(87,174)`、以及 `wuhuan-v2:shoe-shop-entry-exact-130-130:currentMap` 到长安 `(130,130)`；复用同文件 `clickMiniMapPointForFireAndHandoff(...)`、`closeMiniMapAfterFireAndHandoff(...)`。 | Cloud `com.bot.dhxy.service.NavigationService` 的 current-map 宏命令分支。保留两条 exact source/坐标条件，不接受“所有五环导航”泛化。 |
| 2 | `FiveRingTaskV2.checkReadyPriorityBeforeOutsidePhase(...)`：current wake 同时包含 `TASK_ATTENTION_REQUIRED`、`TASK_TRACKER_NEGATIVE_READY`、`PREPARED_ACTION_READY`，有 pathing intent 时再加 `PATHING_TERMINAL`；terminal 只回到 phase 消费事实。`WindowTaskRunner` 的 tracker-negative 发布点也必须对照。 | Cloud `FiveRingTaskV2` 与 `CloudWholeTaskReadyEventState`；客户端 observation/event bridge 只补齐同名事实传递，不得把 negative 转成 terminal business result。 |
| 3 | `FiveRingTaskV2.resolveStoppedAwayTrackerIntentBeforeSync(...)`、`isStoppedAwayWuhuanTrackerPathing(...)`、`clearStoppedAwayTrackerIntent(...)`，以及同提交的给物品地图 guard。 | Cloud `FiveRingTaskV2.waitPathing(...)` 和 tracker sync 前置分支。直接迁入 `大雁塔二层` 保留 dialog、其他地图清 `UNTARGETED_TRACKER` 后 sync 的现有细节。 |
| 4 | `FiveRingTaskV2.clearConsumedWuhuanTrackerPathingIntent(...)`，在 `waitPathing(...)` 的 combat-entry 与 combat-recovered 两个调用点。 | Cloud `FiveRingTaskV2` 同一战斗边界。优先直接迁此方法和两个调用点；不另造 observer-only 规则。 |
| 5 | `FiveRingTaskV2.readWuhuanTrackerTitleGate(...)` 与 `WuhuanTrackerTitleGate`；同提交把三个 accept-option 后的 `withTaskAccepted(...)` 移除，只有 title gate 命中才写 accepted。 | Cloud `FiveRingTaskV2`、`FiveRingPhaseContext`。保留 pending accept 到 `SYNC_TASK_PANEL` 的顺序。 |
| 6 | `FiveRingTaskV2.captureWuhuanTrackerCombatBaseline(...)`、`tryResolvePostCombatFromWuhuanTrackerRoiCandidate(...)`、`tryResolvePostCombatPositiveEvidence(...)`、`freshPreparedWuhuanTrackerGreenAction(...)`、`releaseWindowCombatStateAfterWuhuanEvidence(...)`；以及 `FiveRingPhaseContext` 的 ROI baseline 生命周期。 | Cloud `FiveRingTaskV2` + CR271 已有 tracker ROI capture/observation 通道。完整迁入 guard 链；禁止只复制像素 diff 或单独以 ROI 变化释放战斗。 |
| 7 | `FiveRingTaskV2.shouldReleaseTurnOnOutsidePhaseEnter(...)`、带 `releaseTurnOnEnter` 参数的 `runPhaseWithoutTaskTurn(...)`、`startAcceptSetupPositionPrewarm(...)`、`acceptSetupPositionFromPrewarmOrSync(...)`。 | Cloud `FiveRingTaskV2` 和现有 `CloudTaskTurnCoordination`。只复用已有 release API；不新增第二个 turn/position store。 |
| 8 | `FiveRingTaskV2.tryHandleAcceptReturnedDialogAfterTrackerMiss(...)`、`resolveFiveRingCompletionStoryOutcome(...)`、`mapTrackerNegativeStatus(...)`。 | 已等价存在于 Cloud `FiveRingTaskV2`；第 2 点接入 negative 后只复用这些现有分支，不得新建“negative=完成”。 |

迁入前必须生成并保存以下只读 diff 证据：

```powershell
git -C D:\mavenProject\DHXY diff --unified=40 696a12b0 9aa987d1 -- src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java src/main/java/com/bot/dhxy/task/wuhuan/FiveRingPhaseContext.java src/main/java/com/bot/dhxy/service/NavigationService.java src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java
```

这份 diff 是业务实现来源；CR271 的 Cloud/local protocol 改动只允许改变调用边界，不能改变其中的业务分支。

### Wave A：定义五环等待与确认合同

1. 按 reference ledger 的 `checkReadyPriorityBeforeOutsidePhase(...)` 在 Cloud `FiveRingTaskV2` 的当前窗口 wait/priority 边界消费三类 ready event。
2. `PATHING_TERMINAL` 和 `PREPARED_ACTION_READY` 只唤醒并重新读取当前事实；`TASK_TRACKER_NEGATIVE_READY` 只进入 `SYNC_TASK_PANEL` 的 re-read 分支。
3. 将 `TASK_ACCEPTED_NEEDS_SYNC` 改为 pending-accept 状态，禁止 `withTaskAccepted(...)`；仅 `TaskTrackerPanelService.readWuhuanTrackerTitle(...)` 命中后写 accepted。
4. 把既有 `tryHandleAcceptReturnedDialogAfterTrackerMiss(...)` 作为 title/Tracker negative 后的唯一 dialog/完成兜底。

候选写集：

- Cloud：`src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`
- Cloud：`src/main/java/com/bot/dhxy/task/wuhuan/FiveRingPhaseContext.java`
- Cloud：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudWholeTaskReadyEventState.java`（仅当现有查询无法表达当前窗口三类事件时）
- Cloud：对应 focused contract test。

### Wave B：恢复首段导航和 terminal 分流

1. 按 reference ledger 的两条 exact source/坐标条件，在 Cloud `NavigationService` 迁入五环首段 fire-and-handoff；复用现有 `clickMiniMapLogicalPointForFireAndHandoff(...)`，不改普通/修罗导航。
2. 按 `resolveStoppedAwayTrackerIntentBeforeSync(...)` 在 `waitPathing(...)` / tracker sync 前置路径恢复 `STOPPED_AWAY` 分流：
   - 非`大雁塔二层`且为残留 `UNTARGETED_TRACKER`：精确 clear intent，直接 `SYNC_TASK_PANEL`；
   - `大雁塔二层`：保留既有 `HANDLE_DIALOG`/给予物品路径。
3. 不以 event/negative 或 clear 成功替代到达、dialog、完成的业务真值。

候选写集：

- Cloud：`src/main/java/com/bot/dhxy/service/NavigationService.java`
- Cloud：`src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`
- Cloud：既有 pathing/五环 focused contract tests。

### Wave C：战斗边界与小 ROI 恢复

1. 按 reference ledger 的 `clearConsumedWuhuanTrackerPathingIntent(...)` 在 Cloud `FiveRingTaskV2` 自身的 combat-entry 与 combat-recovered 两个边界恢复 cleanup；不把这个业务规则改写成 observer-only 分支。
2. entry 与 recovery 都必须是幂等的，不清 battle radar 自己的战斗真值。
3. 按 reference ledger 的五个 ROI helper 迁入 baseline image capture、compare、prepared/完成 guard 和 release 顺序；客户端 observation/local mechanics 只提供当前窗口缓存 Tracker block ROI。
4. ROI 变化只能触发重新观察，不能单独宣布战斗结束。

候选写集：

- 客户端：`src/main/java/com/bot/dhxy/cloud/turn/local/tasktracker/TaskTrackerPanelCaptureLocalMechanics.java`（仅补现有 ROI capture 协议所需事实）
- 客户端：现有 observation payload/local operation 的最小对应类；实施前锁定精确文件，禁止新建第二状态机。
- Cloud：`CloudWholeTaskObserver.java`、`FiveRingTaskV2.java`、必要的现有 observation DTO/consumer。

### Wave D：性能收尾，不改业务决策

1. 在接受任务首次需要导航前请求一次现有位置 observation/prewarm，缓存只供本轮首个导航判断使用。
2. 对 outside phase，若 inherited coarse turn 存在且本 phase 没有即将发送输入，在运行 OCR/位置/Tracker 读之前释放；物理输入仍只经唯一 command/input 队列。
3. 不修改 title、绿链、NPC、dialog、导航目标、完成条件或重试顺序。

候选写集：

- Cloud：`FiveRingTaskV2.java`
- 如当前 turn coordination 没有“仅释放已持有 turn”的现有安全入口，先在现有 `CloudTaskTurnCoordination` 寻找等价 API；不能为方便新增 wrapper 链。

## 4. 验收与门禁

实施后才执行，不在本次审计执行：

1. 五环 focused source/contract tests：覆盖 8 个条目，特别是 accept pending/title confirm、三类 ready event、两类 `STOPPED_AWAY`、combat entry/recovery 和 ROI guard。
2. DHXY-cr271：`mvn -q -DskipTests compile`。
3. Cloud：`mvn -q compile`，以及用户明确授权的 named tests。
4. Fresh runtime：首段导航点击后立即放权；非交付 `STOPPED_AWAY` 不再有 `HANDLE_DIALOG` 长等待；`大雁塔二层`仍可处理给予/dialog；战后旧 intent 不会使 prepared action 过期；接任务无 title 时不提前确认。

## 5. 当前决定

- 需要落实：1、2、3、4、5、6、7。
- 已有等价语义：8；只作为 Wave A 的 negative-event 再读取落点，不重写。
- 任何实施必须先固定双仓精确写集，并逐段与 `9aa987d1` 比较。不得把本计划视为 HTTPS 迁徙批准或 runtime 测试授权。

## 6. 2026-07-23 实施与父级审核

实施 reference 固定为 `D:\mavenProject\DHXY@9aa987d1`，没有按计划文字重新设计业务。
本轮生产写集为：

- Cloud `src/main/java/com/bot/dhxy/service/NavigationService.java`
- Cloud `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java`
- Cloud `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`
- Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java`

为同步 `FiveRingTaskV2` 新增的同窗口异步预热上下文依赖，仅更新以下既有测试构造调用：

- `FiveRingTaskTrackerTurnContractTest.java`
- `FiveRingWholeTaskTurnContractTest.java`
- `FiveRingCombatRecoveryCleanupContractTest.java`
- `CloudTurnTaskRuntimeContractTest.java` 中的 FiveRing 构造调用
- `CloudTurnTaskFactoryAllowlistTest.java` 中的 FiveRing 构造调用

父级逐点结论：

1. 两条五环 current-map source/坐标严格限定为长安 `(87,174)` 与 `(130,130)`，点击后登记 intent、关闭小地图并立即 handoff。
2. observer 使用 typed tracker result；positive 发布 prepared action，same-frame negative 发布 `TASK_TRACKER_NEGATIVE_READY`。五环只用 negative 唤醒并转 `SYNC_TASK_PANEL` 重读，不把 negative 当完成。
3. 非交付地图的快速分流同时要求 `STOPPED_AWAY`、`UNTARGETED_TRACKER` 和五环 source prefix；`大雁塔二层`仍保留 dialog/give 流。
4. combat entry 与 recovery 均清理已消费的五环 tracker intent，不替代 battle radar 真值。
5. accept option 点击保持 pending；只有实际 Tracker title/Tracker 正证据才写 accepted。
6. 小 ROI 只作为 candidate，随后要求 fresh prepared/fresh typed prepare、完成弹窗或可信战斗探测；negative 不是正证据，单独 ROI diff 不释放战斗。
7. outside phase 在耗时工作前释放继承 turn；cleanup 后以同一 `TaskExecutionContext`/window 异步预热位置，accept 仅消费同窗口且未过期结果，否则同步读取。
8. `tryHandleAcceptReturnedDialogAfterTrackerMiss(...)` 等既有第 8 点路径未重写。

门禁：

- `D:\mavenProject\DHXY-cr271`：`mvn -q -DskipTests compile`，exit `0`。
- `D:\mavenProject\dhxy-cloud-brain`：`mvn -q compile`，exit `0`。
- Cloud `mvn -q test-compile`：五环相关 test-compile 错误已清零；仍被两个写集外既有修罗构造错误阻塞：
  `CloudTurnTaskRuntimeContractTest.java:1018`、`CloudTurnTaskFactoryAllowlistTest.java:186`。
- 未启动 runtime/application/Task/poller/UI/capture/input 测试，fresh runtime 验收仍待用户另行授权。

## 7. 2026-07-24 Fresh Runtime #1

- 结论：`BLOCKED`，本轮没有进入 CR227 七项业务逻辑的 fresh runtime 验收。
- 五个窗口均完成地图/位置启动检查，但在 `FiveRingPhaseContext.start(...)` 创建初始 phase 时统一失败：
  `NoClassDefFoundError: com/bot/dhxy/task/wuhuan/FiveRingPhase`，
  根因链为 `ClassNotFoundException: com.bot.dhxy.task.wuhuan.FiveRingPhase`。
- Cloud 监听 `18080` 的 Java 进程创建于 `2026-07-24 10:45:40`；缺失的
  `target/classes/com/bot/dhxy/task/wuhuan/FiveRingPhase.class` 直到
  `2026-07-24 11:24:31` 才由 Maven 编译产生。当前 JVM 启动时使用的是不完整/过期编译输出。
- `FiveRingPhase.java` 与 `FiveRingPhaseContext.java` 当前均为受保护的 untracked 源文件；未回滚、覆盖、
  清理或提交。Maven 编译后两个 class 均已存在，但发生过失败类解析的现有 Cloud JVM 必须重新启动后
  才能进行下一轮 fresh runtime。
- 业务影响：`PREPARE` phase 从未开始，因此没有打开包裹，也没有执行后续五环动作；本轮不能据此评价
  CR227 七项业务修复是否通过。
- 下一门禁：重新启动使用当前 `dhxy-cloud-brain` 编译输出的 Cloud 进程后，再执行 fresh runtime；
  必须先看到任务进入 `PREPARE`，再继续验证本计划第 4 节的运行时验收点。

## 8. 2026-07-24 Fresh Runtime #2：摄妖香与 prepared-action 调度返修

- 本轮五个窗口均进入 `PREPARE`，但只有首窗使用摄妖香。根因不是包裹识别：异步
  `FiveRingIncenseContinuation` callback 没有 task ThreadLocal，却调用无显式窗口参数的
  `PlayerStateService` API，五窗共享 `"default"` 状态；首窗的 quiet period 抑制了后四窗。
- Tracker 后台准备链实际存在：`CloudWholeTaskObserver` 会发布
  `PREPARED_ACTION_READY`。但普通 `PREPARE` 使用公平 FIFO 的阻塞 `run(...)`，首窗持有
  turn `46533ms` 时后四窗已排队；prepared 窗口不能越过这些未准备 waiter，动作在
  `2500ms` freshness 内未消费并 stale clear。
- 精确返修：
  1. 摄妖香 continuation 全链携带 exact `TaskExecutionContext`，窗口状态不再由 callback
     ThreadLocal 推断。
  2. 五环普通粗粒度 phase 使用既有非阻塞 `tryRun(...)`；忙时不入 FIFO、保持同 phase
     并以 `180ms` 退让。prepared Tracker consumer 保持阻塞 `run(...)`，成为当前持有者
     后的真实 waiter。
  3. 普通 phase 尝试领取前也优先让步给其他窗口的 fresh prepared action。
  4. 不修改全局 `CloudTaskTurnAuthority`、五环 phase 语义、包裹原子会话、Tracker/NPC
     点击算法或物理输入序列。
- 门禁：
  - Cloud `mvn -q -DskipTests=false compile`，exit `0`。
  - 摄妖香 exact-window 回归 `1/1`。
  - turn authority 的 busy try/no-queue 与 FIFO waiter 合同 `2/2`。
- 边界：当前只有 Tracker 绿链有后台 prepared-action producer。NPC 点击坐标仍在前台
  `NpcClickService` 中计算；“NPC 也必须后台算好”尚未实现，不能把本次调度返修解释为
  已覆盖 NPC。该能力需要单独冻结 exact target-interest、prepared payload、失效/重验证
  与 testcase replay 写集。
- Git 最新复核：重新 fetch 后，DHXY 远程最新业务分支
  `origin/navigation-migration@a65c5db3`（包含 `9aa987d1`）以及本机更新的已提交快照
  `59b85e0b` 均未实现接任务 NPC 后台 producer。两者仍在
  `clickInitialNpcForAccept(...)` 取得前台 turn 后调用 `NpcClickService.clickNpcSmart(...)`，
  该调用内部才做 `Alt+4`、截图和候选计算。最新版只由
  `WindowTaskRunner.refreshTaskTrackerPreparationSignal(...)` 后台准备五环 Tracker 绿链。
  `59b85e0b` 的 prepared-point mechanics/DTO 与 Cloud stranded port 只是未接线迁徙部件，
  不能作为“最新版已有 NPC 后台预计算”的依据。
- 下一 Fresh 门禁：重启 Cloud 后，五窗必须各自独立判断摄妖香；prepared Tracker action
  必须在 freshness 内优先取得下一 turn，日志不得再显示其被普通 `PREPARE` FIFO waiter
  压到过期。NPC 后台预计算另行验收。
