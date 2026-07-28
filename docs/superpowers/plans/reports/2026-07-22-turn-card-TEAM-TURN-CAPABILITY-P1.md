# TEAM-TURN-CAPABILITY-P1

- 状态：`SOURCE DELIVERED / COMPILE PASSED / FRESH RUNTIME REQUIRED`
- Owner：当前父级直接实施（用户未要求“走流程”）
- 严格只读基线：`D:\mavenProject\DHXY`，`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- Client：`D:\mavenProject\DHXY-cr271`
- Cloud：`D:\mavenProject\dhxy-cloud-brain`

## 事故与根因

- Fresh runtime 中，队长 `67555` 已启动修罗寻路后，队员 `443075411` 的战后补给获得全局输入队列并执行右键道具动作。
- 基线有两层现成控制：`TaskTurnCoordinator` 在 `PATHING_STARTED` 释放调度 turn；队员前台维护仍必须等待同一 local-team session 的显式 `TeamSupportCapability`。
- CR271 task-start metadata 将 `localTeamSessionKey/localLeaderPresent/localSupportMember` 固定写成 `null/false/false`。Cloud 虽通过 CR212 preflight 识别出 `MEMBER`，其 `TaskExecutionContext` 仍被标成 standalone，导致 `AutoCombatService` 绕过 `FIRST_AID` capability gate。

## 已批准修复

1. Client task-start 持续携带同批 `teamSessionKey` 和“该批存在本地队伍”的 authority envelope；不新增 lease、锁或 store。
2. Cloud 使用已经完成的 CR212 preflight 结果一次性投影最终上下文：`LEADER/MEMBER` 保留同批 session，只有 `MEMBER` 为 `localSupportMember=true`，`SOLO/UNKNOWN` 不建立本地队伍权限。
3. Cloud 将该最终角色登记到既有 `TaskMaintenanceService`，使队员必须等队长原有显式 capability 开放点。
4. 不修改 `PATHING_STARTED`、导航 phase、输入队列、公平锁、补给算法或维护窗口开放时机。

## 写集

- Client `WindowTaskControlService.RemoteTurnMetadataSupplier`
- 双仓共享 `TurnWindowMetadata` 注释（wire shape 不变且双仓字节一致）
- Cloud `CloudTurnTaskRuntime`
- Cloud `CloudTurnRuntimeConfiguration`

## 验证

- Client：`mvn -q -DskipTests compile`，exit `0`。
- Cloud：仓库 enforcer 拒绝 `-DskipTests`；改用 `mvn -q compile`，exit `0`。
- 未运行 runtime/UI/capture/input；按仓库 no-local-test 默认规则未新增、未运行测试。
- Fresh runtime 验收：队长未显式开放 `FIRST_AID` 时，队员日志必须为 capability deferred，且不得产生队员前台 input；队长现有开放点出现后才允许消费。

## 业务合同

- 已核对 `docs/业务逻辑.md` 顶部 local-team session/capability 条款，以及修罗普通怪“tracker 绿链后放权等待”条款。
- 无已批准业务差异；按基线等价迁移。
