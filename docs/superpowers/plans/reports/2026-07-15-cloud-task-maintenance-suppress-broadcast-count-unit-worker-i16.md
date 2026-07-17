# W-COUNT-TASK-MAINTENANCE-SUPPRESS-BROADCAST-1

`CLAIMED | task=W-COUNT-TASK-MAINTENANCE-SUPPRESS-BROADCAST-1 | worker=Internal I16 | role=implementation-only | claimedAt=2026-07-15T03:14:42-04:00 | countUnit=TaskMaintenanceService::shouldSuppressIdleMaintenanceBroadcast | requestedCountDelta=+1 | writeSet=[D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java; D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-cloud-task-maintenance-suppress-broadcast-count-unit-worker-i16.md]`

## 固定边界

- 业务基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。
- 目标链：真实 `AutoBattleTask` idle -> maintenance broadcast decision -> 30 秒 suppression cache -> closed boolean / maintenance continuation。
- 保持 baseline cache key、时间语义与更新点；不新增 owner/session/TTL/retry/wrapper。
- 不与已批准 `TaskMaintenanceService::runOpportunisticMaintenance` 或 I13 summon due/cache count unit 重复。
- 本 Worker 不运行 build/test/runtime/Task/UI/capture/input，不执行 Git mutation；`countDelta` 仅由父级源码审查与 fresh Maven 同轮应用。

## 当前状态

`BLOCKED | task=W-COUNT-TASK-MAINTENANCE-SUPPRESS-BROADCAST-1 | countUnit=TaskMaintenanceService::shouldSuppressIdleMaintenanceBroadcast | requestedCountDelta=+1 | countDelta=0 | javaChange=NONE | reason=真实 AutoBattle caller 与 tooltip-group producer/state 均不在 active Cloud，且必需写集被冻结`

## 基线与当前树证据

- 已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、
  `docs/业务逻辑.md` 的维护/召唤兽及五倍/修罗基线门、whole-Service 计划、迁移矩阵和两仓状态。
- DHXY：branch=`thin-client-design`，HEAD=`0114604e1ff5f15491d2910959c45252e893d04f`，
  status entries=`68`；本报告为新增 untracked 文件，DHXY 当前
  `TaskMaintenanceService.java`/`AutoBattleTask.java` 未显示路径级 status。
- Cloud：branch=`navigation-migration`，HEAD=`3b988caa010254973e03342272e6d1d6a9685b01`，
  status entries=`26`；active `TaskMaintenanceService.java` 与 `AutoBattleTask.java` 都是共享工作区既有
  untracked 文件，未回滚、覆盖或清理。
- Cloud active `TaskMaintenanceService.java` 审查前后 SHA-256 均为
  `39AEF8085FDC8AFA0E0F51F8016C307E6F34AB407BAF30CCE52C6E88F14CD996`；
  本 Worker 未修改 Java。
- `696a12b0` 的 `TaskMaintenanceService`/`AutoBattleTask` 均没有
  `shouldSuppressIdleMaintenanceBroadcast`；该独立规则来自后续用户批准的 CR212。迁移矩阵把它单列为
  `TaskMaintenanceService::shouldSuppressIdleMaintenanceBroadcast — 空闲广播抑制缓存(30s TTL)`，因此不能与
  已批准的 `runOpportunisticMaintenance` 或 I13 summon due/cache 合并计数。

## 真实链审查

1. DHXY 当前真实 idle caller 在 `AutoBattleTask.java:230-241`：先调用
   `!taskMaintenanceService.shouldSuppressIdleMaintenanceBroadcast(context)` 得到 closed boolean，再把结果写入
   `TaskMaintenanceRequest.handleMaintenanceBroadcast(...)`；false 只跳过空闲广播扫描，后续 summon maintenance
   仍继续。
2. DHXY 当前 cache owner 在 `TaskMaintenanceService.java:1681-1717`。cache namespace 是当前
   `localTeamSessionKey` 的 `LocalTeamSessionState`，entry key 是 `windowId`；identity match 保持
   `groupHash + leaderWindowId + leaderPlayerId`，时间使用 `System.currentTimeMillis()`，TTL 为 30 秒，只有确认
   同 tooltip group 的本地受控 leader 且当前窗口不是 leader 后才记录/刷新。
3. DHXY 当前 tooltip-group 生产入口是 `WindowTaskRunner.java:3867 ->
   TaskMaintenanceService.recordLocalTeamTooltipGroup(...)`。它提供 suppression 判断所需的 group hash、player/window
   绑定和本地 leader identity。
4. Cloud active `AutoBattleTask.java:182-228` 直接构造
   `.handleMaintenanceBroadcast(true)`；没有 suppression boolean caller。Cloud active
   `TaskMaintenanceService.java` 全仓搜索也没有 `recordLocalTeamTooltipGroup`、`windowTooltipGroupHash`、
   `tooltipGroupsByHash`、`leaderPlayerId`、`idleBroadcastSuppressCacheByWindow` 或
   `shouldSuppressIdleMaintenanceBroadcast`。
5. 因此只在唯一 Java 写集内复制 public 方法/cache record 会成为不可达 helper，而且没有真实 group/leader
   数据可判定；把 gate 内塞进 `runOpportunisticMaintenance` 则会改变 CR212 已批准的 AutoBattle 调用点、适用域和
   请求构造顺序。两种做法都不满足“一次闭合真实 caller -> decision -> cache -> continuation”。

## 阻断与修复方向

- **P1 / 写集外前置：**需要把 active Cloud `AutoBattleTask.maybeRunIdleMaintenance(...)` 纳入写集，恢复基线消费点；
  同时必须接入真实 tooltip-group/leader identity producer，或由其已经批准的迁移任务先提供同一 Cloud
  `TaskMaintenanceService` state owner。不能在 gate 内猜 leader、按 session/window 默认 suppress，也不能新增第二套
  owner/session/TTL/retry/wrapper。
- 父级复验点：确认 active Cloud 存在真实 group producer；确认 AutoBattle 在构造 maintenance request 前消费该
  closed boolean；确认 suppress=true 仅关闭 idle broadcast，maintenance continuation 不被截断；确认 cache
  key/30 秒/记录与清理点和批准实现一致。随后由父级完成源码审查与 fresh Maven，并在同轮决定是否应用 `+1`。

## 验证

- 按任务禁令未运行 build/test/runtime/Task/UI/capture/input，也未执行 Git mutation。
- 无已批准业务差异；本 Worker 未实施不完整迁移，未主张 count。

## Parent Blocker Review #1 - 2026-07-15T03:25:00-04:00

父级独立复核 blocker 成立：active Cloud `AutoBattleTask` 固定 `handleMaintenanceBroadcast(true)`，且 Cloud
`TaskMaintenanceService` 不含 tooltip-group/leader producer 与 30 秒 suppression cache；单改唯一写集只会产生
不可达 helper，内塞到 maintenance Service 又会改变 CR212 的调用点和请求构造顺序。结论：
**P0=0/P1=1/P2=0，BLOCKED_SHARED_LANE / countDelta=0**。须先由既有 coordination owner 闭合 producer/state，
再把真实 AutoBattle caller 与本 gate 作为同一 `+1` 整链重发；I16 本轮关闭释放实现槽。
