# Cloud TaskMaintenance Capability Gate Count Unit - Worker I8

`CLAIMED | task=W-COUNT-TASK-MAINT-CAPABILITY-GATE-1 | worker=Internal I8 | role=implementation-only | claimedAt=2026-07-15T01:50:54-04:00 | countUnit=TaskMaintenanceService::isLocalTeamSupportCapabilityOpen | countDelta=+1 | writeSet=[D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java; this-report]`

## Implementation

- 状态：`BLOCKED / NO_CODE_CHANGE`。`countDelta=+1` 本轮不能领取。
- Cloud `TaskMaintenanceService.java` 保持 SHA-256 `39AEF8085FDC8AFA0E0F51F8016C307E6F34AB407BAF30CCE52C6E88F14CD996`，未改 Java。
- `migration-baseline/696a12b0/.../TaskMaintenanceService.java:469-475` 与 active `TaskMaintenanceService.java:468-474` 的 gate 本体等价：null capability、无 local-team session、session state 不存在或 capability 未打开均返回 false；只有同 session 的 `state.capabilities.contains(capability)` 为 true 才放行。
- 无已批准业务差异；按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 基线等价核实。
- 没有把 task/role 推断改成 capability 真值，没有新增 owner/session/TTL/retry，也没有增加 checkpoint、turn 或输入。

## Active Caller Evidence

1. `AutoBattleTask.execute(TaskExecutionContext)` 在 `:111-113` 拒绝 context-free 调用并用同一个 context 覆盖 `TaskExecutionContextHolder.callWith(...)`；`:139-147` 在同一 patrol/run 内把该 context 传给 combat tick 与 idle maintenance。因此 caller 读取的 task/window/taskRunId/runRevision/role/session 都来自同一 authority-minted Cloud context。
2. `AutoBattleTask:197-205` 先要求 `isLocalSupportMemberSession(context)`、真实 MEMBER follower-support role、requested team task，再以 `LEFT_TOP_STATUS` 调用本 count unit；false 时不进入 `LeftTopStatusSwitchService.consumeFollowerSafeWindow`。
3. `AutoBattleTask:235-255` 先以 `TEAM_RETURN` 的 zero-wait await 调用本 count unit，再在 `:245-248` 独立检查 `COMMON_BOX`。TEAM_RETURN false 在 `:239-241` 直接返回；COMMON_BOX false 通过 Java 短路不调用 box consume。之后的 return-team 动作属于已经打开的独立 `TEAM_RETURN` capability，不是 COMMON_BOX false 分支产生的输入。
4. `AutoCombatService:476-517` 的 COMMON_BOX 路径在 `:491-498` 调用本 count unit；closed 时在 `:498` 返回，早于 `taskTurnCoordinator.enter(...)` (`:503`) 和 `commonBoxService.consumePendingBoxIfAllowed(...)` (`:507`)。
5. `AutoCombatService:520-589` 的 FIRST_AID 路径在 `:534-535` 经 `awaitLocalTeamSupportCapabilityOpen` 调用本 count unit（active `TaskMaintenanceService:291/300`）；closed/timeout 在 `:536-540` 返回，早于 turn enter (`:569`) 和 first-aid mechanics (`:573-577`)。
6. `AutoCombatService:665-685` 的 LEFT_TOP_STATUS 路径在 `:669-677` 调用本 count unit；closed 时只记录 deferred 日志，不调用 `LeftTopStatusSwitchService.handleCombatMaintenance(...)` (`:672`)。

## Typed Mechanics And Terminals

- TEAM_RETURN true branch：`AutoBattleTask:253-254 -> TeamReturnService.clickReturnTeamIfPresent`；`TeamReturnService:57-95` 通过 `CloudTeamReturnPort` 做两次 typed button observation 和一次 ordered click。`OBSERVED/PRESENT` 才点击；`NOT_EXECUTED -> false`；`STOPPED` 走 checkpoint；其它 unresolved terminal 抛 `TaskFatalException`。
- LEFT_TOP_STATUS true branch：`AutoBattleTask:205` 或 `AutoCombatService:672 -> LeftTopStatusSwitchService`；该服务 `:166-192` 用 `CloudLeftTopStatusPort.observe` 闭合 OPEN/CLOSED/UNKNOWN/CAPTURE_FAILED/transport terminal，`:212-232` 用 typed click 闭合 EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN。
- COMMON_BOX true branch：`AutoBattleTask:247-248` 或 `AutoCombatService:507-508 -> CommonBoxService.consumePendingBoxIfAllowed`；该服务 `:87-172` 复核 task/run/window/role/identity/expiry 后在 `:140-162` 调用 `CloudCommonBoxPort.click`，闭合 EXECUTED/NOT_EXECUTED/STOPPED/其它 fatal terminal。
- FIRST_AID true branch：`AutoCombatService:569-588` 才取得并 finally 释放 task turn，`PlayerStateService:319-352` 通过 `CloudPlayerStateFirstAidPort.executeCachedPlan` 消费既有 plan；本任务未改该只读链。

## Precise Blocker

当前 active Cloud 主源码没有任何生产调用可以建立本 gate 所依赖的 `localTeamSessions` 真状态：

- `registerLocalTeamSessionCandidate(...)` 只有 `TaskMaintenanceService:337` 定义；无外部 caller。
- `markLocalTeamWindowRoleDetected(...)` 只有 `:363` 定义；`markLocalTeamLeaderDetected(...)` 除 `:376` 的同类内部调用与 `:433` 定义外无 caller。
- `openTeamPathingMaintenanceWindow(...)`、`openTeamFirstAidMaintenanceWindow(...)`、`openLocalTeamReturnSupportWindow(...)` 只有 `:108/:141/:193` 定义；无外部 caller。
- 对应 close 与 `completeLocalTeamSessionWindow(...)` 同样只有定义，无 active producer/lifecycle caller。

因此 active source graph 中 `localTeamSessions` 不会注册 candidate/leader，也不会打开 capability；`isLocalSupportMemberSession(...)` 无法通过 `hasDetectedLocalLeader(...)`，本 count unit 的 true 分支不可达。下游 typed mechanics/terminal 虽已存在，不能替代缺失的 session/role/capability producer。

在唯一 Java 写集 `TaskMaintenanceService.java` 内强行闭合只能选择以下被禁止方案之一：调用 gate 时自行创建 session/leader、按 task/role 猜 capability、默认放行 capability，或改写 open/close 时机。这些都会改变 696 的 leader-open/session/capability 条件和顺序。真实修复需要 active Cloud task/runner lifecycle caller 写集接入现有 register/role/open/close/complete API；这些 caller 被本任务明确冻结，所以本 Worker 精确报告 `BLOCKED`，不造 churn/wrapper。

## Self Review

- 写集核对：只新增本报告；指定 Java 文件零改；未触碰 AutoBattleTask、AutoCombatService、TeamReturn、LeftTop、CommonBox、ports、DHXY 或 generic shared。
- 业务核对：未改变 696 session/role/capability 条件、顺序、fallback/state；无 owner/session/TTL/retry 增量。
- 验证边界：按指令未运行 Maven、test、runtime 或 Git；仅做 source graph、行级控制流和 SHA-256 核实。
- 本节仅为实现者自审，不构成 reviewer 结论或审批。

`BLOCKED | task=W-COUNT-TASK-MAINT-CAPABILITY-GATE-1 | countUnit=TaskMaintenanceService::isLocalTeamSupportCapabilityOpen | countDelta=0 | reason=active Cloud has callers and typed downstream terminals but no active session/role/capability producer lifecycle; frozen caller write set required`
