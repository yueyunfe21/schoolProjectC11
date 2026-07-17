# Cloud TaskMaintenance Summon Clean Count Unit - Worker I10

`CLAIMED | task=W-COUNT-TASK-MAINTENANCE-MAYBE-CLEAN-SUMMON-1 | worker=Internal I10 | role=implementation-only | claimedAt=2026-07-15T02:32:36-04:00 | countUnit=TaskMaintenanceService::maybeCleanSummonSkill | requestedCountDelta=+1 | writeSet=[D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java; this-report]`

## Implementation #2 - Scope Correction And Active-Chain Closure

### 交付结论

- 状态：`NO_CODE_CHANGE / READY_FOR_PARENT_SOURCE_REVIEW`。
- 本单要求闭合的是“真实 `runOpportunisticMaintenance` caller”，不是上一版报告误写的
  `AutoCombatService.maybeRunCombatMaintenance` caller。active Cloud 已存在完整真实链：
  `AutoBattleTask.runAutoBattlePatrol -> FREE idle -> maybeRunIdleMaintenance ->
  TaskMaintenanceService.runOpportunisticMaintenance -> maybeCleanSummonSkill ->
  SummonSkillService.cleanSummonSkillsOnce -> retained typed SUMMON_SKILL_WHOLE_PASS ->
  DHXY exact-window exclusive mechanics -> closed outcome -> Cloud result/cache/state/claim handling`。
- 唯一 Java 写集 `TaskMaintenanceService.java` 已完整实现该链，并与 `696a12b0` 的业务方法体等价；无需修改。
- `requestedCountDelta=+1` 保持不变；本 Worker 不执行 ledger、父级审查或统一 Maven 门，因此本报告只交付
  `countCandidate=+1`，当前 `countApplied=0`，不得把本 Worker 的自检写成 `Approved`。
- 上一版 `BLOCKED` 结论由本段明确作废：它审查了任务未要求的 combat-maintenance caller，不能继续作为本单 blocker。

### 读取与工作区基线

- 已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、
  `docs/superpowers/plans/2026-07-14-696a12b0-whole-service-first-migration.md`；已核对迁移矩阵中
  `TaskMaintenanceService`、`SummonSkillService`、`AutoBattleTask::maybeRunIdleMaintenance`、
  `runOpportunisticMaintenance/maybeCleanSummonSkill` 的职责、timer/cache/lock/fallback 条目。
- 已核对 `docs/业务逻辑.md` “召唤兽三技能维护 / 技能格静态边界识别”：6/8 布局、静态
  `LOCKED_SLOT/EMPTY_SLOT/OCCUPIED`、`UNKNOWN` fail closed，以及“不改变普通技能删除、终极角、冷却、
  CR145 队列、面板开关/确认点击”的约束。
- DHXY 当前分支 `thin-client-design@0114604e`，Cloud 当前分支
  `navigation-migration@3b988caa`；两仓均有大量既存 dirty/untracked。本 Worker 未回滚、覆盖或整理任何他人改动。
- 业务基线：DHXY `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。

### 源码与基线对照

1. baseline mirror：
   `migration-baseline/696a12b0/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`，
   1123 行，SHA-256 `4BEAFFD08314F694B41A841DFF236C4CE00DC335CBE75DE74A9F667A53803EDA`。
2. active Cloud：`src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`，1130 行，SHA-256
   `39AEF8085FDC8AFA0E0F51F8016C307E6F34AB407BAF30CCE52C6E88F14CD996`。
3. `git diff --no-index` 的完整差异只有 Cloud context ownership 适配：
   - import/injection：`WindowTaskContextHolder + WindowRuntimeContext` 替换为 `TaskExecutionContextHolder`；
   - `summonSkillState(windowKey)` 改为 `summonSkillState(windowKey, context)`；
   - `currentWindowKey`、`currentPlayerIdentityEpoch`、`logPrefix` 从当前 authority-minted
     `TaskExecutionContext` 读取，context 缺席时才读 Cloud holder。
4. `runOpportunisticMaintenance`、`handleMaintenanceBroadcast`、`maybeCleanSummonSkill`、四字段 cleanup request、
   cooldown/cache/claim helper 和所有分支均无 diff。没有 phase、判断、delay、fallback、state、TTL、retry 或顺序差异。
5. `AutoBattleTask` 的真实 caller 在 active 与 `696a12b0` 相同：active `:111-113` 把整次任务绑定到 exact
   `TaskExecutionContextHolder.callWith`；`:139-148` 只有 `combatResult == NONE` 且 ActionState 为 `FREE` 才进入
   idle maintenance；`:182-228` 组装并调用 `runOpportunisticMaintenance`。

### Active 逐跳链与 Gate

1. **真实 caller / FREE gate**
   - `AutoBattleTask.java:139-148`：非 `NONE` combat result 立即 sleep/continue；仅 FREE idle 调
     `maybeRunIdleMaintenance`。
   - `:182-207`：stop checkpoint、local-team return、pending leader detection、standalone return、follower/session
     与左上角 capability 前置保持原优先级。
   - `:208-228`：request 固定 `sourceTask=auto-battle`、broadcast-first、
     `allowFullMaintenanceBroadcastFallback=false`、`cleanSummonSkill=true`；local support 或 legacy team follower
     才启 one-per-round，并分别携 `SUMMON_SKILL` capability 或 team pathing window gate。

2. **入口 / broadcast 优先级**
   - `TaskMaintenanceService.java:578-597`：`normalize -> checkpoint -> handleMaintenanceBroadcast`；broadcast
     handled、failed 或 interrupted 均先返回，只有未消费 broadcast 且 `cleanSummonSkill=true` 才进入本 count unit。

3. **config / action / cooldown**
   - `:624-646`：按原顺序检查 enabled、interval > 0、可选 FREE action-state gate；各自返回 closed
     `SUMMON_SKILL_DISABLED` 或 `SUMMON_SKILL_DEFERRED`，未产生输入。
   - `:648-669`：以 exact window key + player identity epoch 取状态，先判正常 clean interval，再判既有
     UNKNOWN retry-backoff；没有新增或删除 delay/backoff。

4. **cache**
   - `:670-693`：保留既有 2h tail-safe cache expiry/fresh 分支；fresh 分支刷新原 clean timestamp并返回
     `SUMMON_SKILL_NOT_DUE`。
   - `:816-840`：保留 2h skill-count cache trust 与 ultimate cooldown，构造
     `expectedSkillCount/trustExpectedSkillCount/startSlotIndex/skipUltimateCornerCheck` 四字段 request。
   - `:843-935`：成功才更新 skill count、next start、observed slots、tail-safe 与 ultimate success；UNKNOWN
     failure 才失效 layout cache，未把 UNKNOWN 当成功。

5. **team-round / capability / claim**
   - `:694-720`：one-per-round 时先要求可解析 round；local-support 路径要求 `SUMMON_SKILL` capability，legacy
     路径要求 `PATHING_WINDOW_OPEN`。两条 gate 不互相替代。
   - `:721-740`：同窗口重复 claim 与 claim 上限均 closed 返回；只有全部 gate 通过才添加当前 window claim。
   - `:1038-1067`：round key 按 local capability epoch 或 team key + active round 解析，不新增 owner/session/TTL。

6. **INTERACTING try/finally / closed result**
   - `:743-760`：最后 checkpoint 后保存 previous state，置 `INTERACTING`，构造 cleanup request，并恰一次调用
     `summonSkillService.cleanSummonSkillsOnce(cleanupRequest)`。
   - `:761-784`：finally 内成功才清 UNKNOWN backoff、更新 cache 并刷新 clean timestamp；失败但绝技生成成功只记
     原 cooldown；UNKNOWN 才记录原 backoff 并失效 layout cache；state 仍为 `INTERACTING` 才恢复 previous state。
   - `:786-796`：成功返回 `SUMMON_SKILL_CLEANED`；失败且无技能状态变化时按 `:938-963` 释放 team-round claim；
     删除/绝技点击等已有状态变化时保留 claim，最后返回 `SUMMON_SKILL_FAILED_RETRY_LATER`。

### Existing Approved Typed Exact-Window Mechanics

1. Cloud `SummonSkillService.java:172-225` 从 active holder 取得 exact context，把四个 intent 字段原样写入
   `WholePassIntent`，恰一次调用 `context.getRemoteGameClient().summonSkillWholePass().execute(intent)`；
   `Executed` 映 cleanup value，`NotExecuted` 映失败，`Stopped/Unknown/interrupt` 直接 typed unwind，不自动重发。
2. Cloud `SummonSkillService.java:233-257` 把 success、skillCount、nextStart、slot statuses、ultimate clicked/
   succeeded、inspected/deleted count、message 逐字段映回 `SummonSkillCleanupResult`。
3. Cloud `CloudTaskExclusiveInteractionAuthority.java:792-913` 在当前 generation/binding 上保留固定
   `ActionAddress("summon-skill", "whole-pass")`，同 intent 才允许复用，transport runtime 进入 UNKNOWN fence；
   `:1039-1093` 只 final-consume `EXECUTED/NOT_EXECUTED/STOPPED`，明确拒绝 UNKNOWN。
4. DHXY `LocalRemoteGameCommandHandler.java:2655-2696` 在 exact task-run/window admission 下打开既有 in-flight
   exclusive handle，经一次 `submitRemoteExclusiveAndWaitDetailed` 在 input worker 内调用本地
   `SummonSkillService.cleanSummonSkillsOnce`，不存在 queue-in-queue。
5. DHXY handler `:2710-2783` 闭合 `EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN`；执行后 exact binding 漂移变
   UNKNOWN，finally 总关闭 in-flight handle。approved 本地 Summon mechanics 保留 6/8 布局、静态槽扫描、删除/
   绝技/锁定回扫、UNKNOWN fail closed、40s deadline 与最多删除 5 个的既有顺序。

### Scope QA（实现者自检，不是 reviewer 结论）

- Cloud `TaskMaintenanceService.java`：`NO_CODE_CHANGE`，审计前后 SHA-256 均为
  `39AEF8085FDC8AFA0E0F51F8016C307E6F34AB407BAF30CCE52C6E88F14CD996`。
- 唯一报告：本文件更新为 Implementation #2，明确 supersede 旧错误 blocker。
- 未修改写集外 Java/Markdown/config/resource；未制造 stub、wrapper、owner/session/TTL/retry 或新 gate。
- 未执行 Git mutation；未运行 runtime/application/server/host/Task/poller/UI/capture/input。
- 按用户明确限制，未运行 tests 或 build/Maven；因此不声称 compile/package 通过。
- 无已批准业务差异；按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 基线等价迁移。

`DELIVERED_NO_CODE_CHANGE | task=W-COUNT-TASK-MAINTENANCE-MAYBE-CLEAN-SUMMON-1 | countUnit=TaskMaintenanceService::maybeCleanSummonSkill | requestedCountDelta=+1 | countCandidate=+1 | countApplied=0 | javaChange=NONE | parentSourceReview=PENDING | unifiedBuild=PENDING_BY_PARENT | workerApprovedClaim=NONE`

## Parent Count Boundary Review #1 - 2026-07-15T02:47:00-04:00

父级独立复核确认源码行为无缺陷，但迁移矩阵把
`runOpportunisticMaintenance/handleMaintenanceBroadcast/maybeCleanSummonSkill` 明确定义为一个计数单元；其中
`runOpportunisticMaintenance` 已 `SOURCE APPROVED / COUNT PENDING BUILD`。本次 private helper 使用相同 caller、请求和
typed Summon whole-pass terminal，再计一次会重复。结论：
**P0=0/P1=1/P2=0，COUNT BOUNDARY BLOCKED / countDelta=0**；无需返修 Java，本 Worker 可关闭。
