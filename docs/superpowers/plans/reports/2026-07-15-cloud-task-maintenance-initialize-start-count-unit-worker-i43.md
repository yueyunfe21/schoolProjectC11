# I43 TaskMaintenance initialize-at-start count unit

`CLAIMED | task=W-COUNT-TASK-MAINTENANCE-INITIALIZE-START-1 | worker=Internal I43 | role=implementation-only | claimedAt=2026-07-15 | countUnit=TaskMaintenanceService::initializeForTaskStart | requestedCountDelta=+1 | writeSet=[D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java; this-report]`

## Status

DELIVERED_NO_CODE_CHANGE

## Implementation

- 结论：`NO_CODE_CHANGE / ACTIVE CALLER-TO-STATE-TO-FIRST-TICK CHAIN CLOSED`。
- 请求计数：`countDelta=+1`；本 Worker 只提交 count candidate，不修改 ledger，`countApplied=0`，等待父级唯一 reviewer 独立判定与统一构建。
- Java 写集没有发生修改。Cloud `TaskMaintenanceService.java` 审计前后 SHA-256 均为
  `39AEF8085FDC8AFA0E0F51F8016C307E6F34AB407BAF30CCE52C6E88F14CD996`。
- 无已批准业务差异；按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 基线等价迁移。

## Authority And Scope

1. 已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、当前 `docs/ACTIVE_WORK.md` 顶部 CR271、
   whole-Service-first 计划和 Service migration matrix；已核对 `docs/业务逻辑.md` 的五倍/修罗基线使用门、
   启动/热恢复与修罗 `696a12b0` 权威说明。
2. 两仓 `git status --short --branch` 显示 DHXY=`thin-client-design`、Cloud=`navigation-migration`，均为共享
   dirty 工作区。本 Worker 未回滚、覆盖、清理、提交或修改任何他人文件。
3. 迁移矩阵把 `TaskMaintenanceService` 定义为维护状态/冷却/队列编排 owner，并单列已交付的
   `runOpportunisticMaintenance` 等单位；它没有另写 `initializeForTaskStart` 方法行。本任务由 CR271 05:18
   明确发为独立 public state-producer countUnit。I43 只计算 startup 初始化到首个 tick 消费这一段，不重复计算
   I3 的 opportunistic-maintenance 决策链、I10/I13 的 summon helper/cache 单元或 I33 的 idle-maintenance 调用方。
4. 禁令遵守：未运行 Maven、测试、runtime/application/server/host/Task/poller/UI/capture/input；未修改
   runtime/runner/Task/host/transport/config；未新增 owner/permit/session/ledger/compaction/durable workflow、
   business TTL、auto retry 或 wrapper。

## Baseline Equivalence

1. authority mirror：
   `D:/mavenProject/dhxy-cloud-brain/migration-baseline/696a12b0/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`
   第 68-80 行。
2. active Cloud：
   `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`
   第 67-79 行。
3. 两个 `initializeForTaskStart(context, sourceTask)` 方法体逐行一致：
   - 先用 `currentWindowKey(context)` 取得窗口键；
   - `summonSkillCleanRunImmediatelyOnStart=true` 时只移除该窗口的 `lastSummonSkillCleanAt` 并返回；
   - 否则只把该窗口的 `lastSummonSkillCleanAt` 写为当前时间；
   - 两个分支保留原日志和顺序。
4. active 与 baseline 的完整 Service 差异只在 Cloud context plumbing：active 用
   `TaskExecutionContextHolder` 替代本地 `WindowTaskContextHolder/WindowRuntimeContext`。本 caller 显式传入
   `context`，因此 `currentWindowKey` 仍在第一分支直接读取 exact `context.windowId`，不会落到 default，也没有
   改变本 countUnit 的状态键或时序。
5. 特别保留 baseline 的窄初始化语义：该方法不清 `summonSkillUnknownRetryAfterByWindow`、tail-safe/count cache、
   team round、claim 或 not-due log。I43 没有把“启动即执行”扩大成新 cleanup，也没有增加 TTL、retry 或 gate。

## Active Caller To Closed Continuation

1. `AutoBattleTask.java:111-113` 解析 authority-minted `TaskExecutionContext`，再以
   `taskExecutionContextHolder.callWith(context, () -> runAutoBattlePatrol(context))` 绑定整次 patrol；该真实 active
   caller 不依赖新复制的 Wubei/Xiuluo Task 才成立。
2. `AutoBattleTask.java:116-137` 保留 `696a12b0` 顺序：startup check ->
   `BotStatus.RUNNING` -> startup first-aid -> `initializeForTaskStart(context, "auto-battle")` ->
   `autoCombatService.initializeForCurrentWindow()`。`initializeForTaskStart` 在首个 while/combat tick 前恰好调用一次。
3. state write 是 exact-window closed mutation：active `TaskMaintenanceService.java:987-995` 对有窗口的显式 context
   直接返回 `context.windowId`；初始化只对 `lastSummonSkillCleanAtByWindow[windowId]` 做 remove/put，不跨窗口。
4. `AutoBattleTask.java:139-149` 的后续首个 patrol tick先跑 combat tick。非 `NONE` 时按 baseline sleep/continue；
   只有 `TickResult.NONE + ActionState.FREE` 才进入 `maybeRunIdleMaintenance`。I43 没有把 initialization 提前变成
   物理动作，也没有绕过 combat-first 顺序。
5. FREE tick 在 `AutoBattleTask.java:208-228` 构造既有 typed `TaskMaintenanceRequest` 并单次调用
   `runOpportunisticMaintenance`。`TaskMaintenanceService.java:646-657` 用同一 exact window key 读取
   `lastSummonSkillCleanAtByWindow`：
   - immediate=true 的 remove 让 `lastCleanAt=null`，现有 due 链继续；
   - immediate=false 的 put 让首 tick返回 typed `SUMMON_SKILL_NOT_DUE`，冷却从 task start 起算。
6. due 分支继续使用已存在的 typed terminal，不由 I43 重建：
   `TaskMaintenanceService.java:755` -> `SummonSkillService.cleanSummonSkillsOnce(request)` ->
   `SummonSkillService.java:194-224` -> exact current context 的
   `remoteGameClient.summonSkillWholePass().execute(intent)` -> closed
   `Executed/NotExecuted/Stopped/Unknown` 映射。maintenance 最终返回现有 `TaskMaintenanceResult`，调用方记录
   handled 结果后回到 `AutoBattleTask.java:149` 的下一次 poll。
7. 默认 active 配置仍为 `summonSkillCleanRunImmediatelyOnStart=false`、interval=20 分钟；控制面 authority getter
   原样提供这两个值。I43 未修改配置或默认值。

## Whole Caller Read-Only Check

- active `WubeiTask.java:364` 在其 startup supply/maintenance timer 初始化之后、首轮 while 之前调用同一方法；
  `:369` 后才进入第一轮并 `beginTeamMaintenanceRound`。该文件由 CR271 保全为 exact 696 blob，仅作 whole-task
  caller 对照，不作为 I43 唯一可达性证据。
- active `XiuluoTaskV2.java:330` 在设置 RUNNING 与 task-level maintenance timers 后、首轮 while 之前调用；
  `:337` 后才开始首轮。该文件同样只作 whole-task caller 对照。
- `rg` 在 active Cloud 只找到三个生产 startup callers：AutoBattle `:136`、Wubei `:364`、Xiuluo `:330`；
  没有 second initialize call、stub caller 或 test-only caller。

## Verification

- active/baseline `initializeForTaskStart` scoped body：exact match。
- active `TaskMaintenanceService.java` SHA-256：
  `39AEF8085FDC8AFA0E0F51F8016C307E6F34AB407BAF30CCE52C6E88F14CD996`，本 Worker Java change=`NONE`。
- active production chain checked through first-patrol continuation and existing typed summon terminal；
  typed boundary gap=`NONE`，missing write-set prerequisite=`NONE`，stub/wrapper=`NONE`。
- 因 External C 与其它 Internal Java writer 活动，按父级禁令未运行 Maven；统一 fresh Cloud package 由父级在
  writers 稳定后执行。

`DELIVERED_NO_CODE_CHANGE | task=W-COUNT-TASK-MAINTENANCE-INITIALIZE-START-1 | worker=Internal I43 | countUnit=TaskMaintenanceService::initializeForTaskStart | requestedCountDelta=+1 | countCandidate=+1 | countApplied=0 | javaChange=NONE | businessDifference=NONE | typedBoundaryGap=NONE | verification=STATIC_SOURCE_ONLY | parentSourceReview=PENDING | unifiedBuild=PENDING_BY_PARENT`

## Parent Source Review #1 - 2026-07-15T05:28:00-04:00

父级独立复核 active `AutoBattleTask:111-149,208-228`、`TaskMaintenanceService:67-79,646-657,987-995`
与 `696a12b0` 方法块。AutoBattle 是当前可达 caller；初始化在首 tick 前恰一次，按配置 remove/put exact
window cooldown key，随后既有 maintenance typed terminal 消费同一状态。没有把 Wubei/Xiuluo 的 blocked Task
当唯一 caller，也未重复计算下游 opportunistic/summon 单元。

结论：**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD**。该去重 count unit 进入待统一构建池；
fresh Cloud package 前 ledger 仍 `189/407`。无已批准业务差异；按 `696a12b0` 基线等价迁移。
