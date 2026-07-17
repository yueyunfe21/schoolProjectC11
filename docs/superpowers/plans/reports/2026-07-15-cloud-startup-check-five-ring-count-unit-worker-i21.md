# W-COUNT-STARTUP-CHECK-FIVE-RING-1

`CLAIMED | task=W-COUNT-STARTUP-CHECK-FIVE-RING-1 | worker=Internal I21 | role=implementation-only | claimedAt=2026-07-15T03:44:00-04:00 | countUnit=TaskStartupCheckService::checkFiveRing | requestedCountDelta=+1 | writeSet=[D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/startup/TaskStartupCheckService.java; D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-cloud-startup-check-five-ring-count-unit-worker-i21.md]`

## 固定边界

- 业务基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。
- 目标链：真实 `FiveRingTaskV2` startup caller -> `checkFiveRing` -> configuration gate / preflight role ->
  `shouldRunFiveRing` -> closed `TaskStartupCheckResult` -> caller continue/skip。
- 严格保持 baseline 配置门、角色判定、UNKNOWN 放行配置和 allow/skip 终态；不新增实时事实读取、
  owner/session/TTL/retry/wrapper。
- 本 Worker 不运行 build/test/runtime/Task/UI/capture/input，不执行 Git mutation；请求的 `+1` 仅可由父级源码
  审查与 fresh Maven 同轮应用。

## 当前状态

`BLOCKED | task=W-COUNT-STARTUP-CHECK-FIVE-RING-1 | countUnit=TaskStartupCheckService::checkFiveRing | requestedCountDelta=+1 | countDelta=0 | javaChange=NONE | reason=active Cloud 缺真实 FiveRing/startup caller 与 TaskStartupCheckService assembly，唯一 Java 写集无法闭合 caller 到终态`

## 基线与工作区证据

- 已读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、
  `docs/业务逻辑.md` 的适用五倍启动/热启动基线、696 whole-Service 计划、迁移矩阵及两仓状态。
- DHXY：branch=`thin-client-design`，HEAD=`0114604e1ff5f15491d2910959c45252e893d04f`；工作区有大量既有
  dirty/untracked，均未回滚、覆盖、清理或暂存。
- Cloud：branch=`navigation-migration`，HEAD=`3b988caa010254973e03342272e6d1d6a9685b01`；
  `src/main/java/com/bot/**` 为既有 untracked 迁移树，本 Worker 全部按受保护输入处理。
- Cloud 目标 Java 审查前 SHA-256：
  `289E3930E6CF3A935443A41CAEA02A70377AF9ECF10B521093AF56A0856638B1`；本 Worker 未修改 Java。
- 迁移矩阵将 `TaskStartupCheckService::checkFiveRing` 单列为 tier A count unit：配置门关闭直接 allow；否则
  使用角色事实，经 `shouldRunFiveRing` 决定 skip/allow。CR271 当前 ledger 在 fresh Maven 前不得提前记账。

## 逐跳证据

1. **真实 baseline/current DHXY caller 已闭合。** 当前
   `FiveRingTaskV2.execute(TaskExecutionContext)` 在 `FiveRingTaskV2.java:266-271` 调用本地主仓 Spring
   `taskStartupCheckService.checkFiveRing(context)`；blocked 时立即返回
   `checkResult.getBlockedResult()`，allowed 时继续设置 RUNNING 并进入轮次。`696a12b0` 的相同调用顺序一致。
2. **baseline configuration gate。** `696a12b0` 的 `TaskStartupCheckService.checkFiveRing` 先读
   `TeamTaskProperties.fiveRingRequiresLeader`；关闭时直接构造 allow，开启时才调用
   `TeamRoleDetectionService.detectCurrentRole(context)`。
3. **baseline policy。** `TeamRoleDetectionService.shouldRunFiveRing(role)` 保持：配置门关闭放行；门开启后
   LEADER 放行，UNKNOWN 仅在 `allowFiveRingWhenRoleUnknown=true` 时放行，MEMBER/SOLO skip。
4. **Cloud 方法内部策略存在。** Cloud `TaskStartupCheckService.java:29-43` 先读冻结 evaluation 的
   `fiveRingRequiresLeader`，门关闭返回 allow；门开启读取 evaluation 的 preflight `role`，调用同文件
   `shouldRunFiveRing`，返回 skip/allow。该 helper 在 `:89-95` 保持 LEADER/UNKNOWN 配置语义，没有实时截图、
   OCR、UI、capture 或 input 读取。
5. **Cloud preflight role 来源存在但仅是 dormant 定义。** `CloudStartupGateAuthority.bind(context)` 从完整
   `TaskExecutionContext.windowRole` 形成 immutable `StartupRoleFact`，`Evaluation.role()` 提供给 service；这没有
   在 `checkFiveRing` 内新增实时角色采集。
6. **closed result 存在。** Cloud `TaskStartupCheckResult` 与 `696a12b0` 同形：allow 关闭为
   `allowed=true/SUCCESS`，skip 关闭为 `allowed=false/SKIPPED`，caller 可通过 `isBlocked()` 与
   `getBlockedResult()` 继续或退出。
7. **但 active Cloud caller/assembly 缺失。** Cloud 全 `src/main/java` 搜索 `checkFiveRing` 只有定义自身；没有
   Cloud `FiveRingTask`/`FiveRingTaskV2`，没有 `new TaskStartupCheckService(...)`，也没有 Spring bean/production
   assembly 构造该 package-private service。类注释也明确写的是 future authenticated activation adapter。
   因此 Cloud 当前不能从真实 FiveRing startup caller 到达上述 configuration/preflight/policy/result 链。

## 阻断与修复方向

- **P1 / 写集外前置：**本 count unit 要成为可计数 active chain，必须由父级把真实 Cloud FiveRing task/startup
  caller、`CloudStartupGateAuthority.Evaluation` 的生产 assembly，以及 caller 对 closed allow/skip result 的
  continue/return 分支纳入同一完整写集，或先由其既有 owner 提供这些入口。
- 只改唯一允许的 `TaskStartupCheckService.java` 无法创造真实 caller；增加 public/static 入口或自建默认 evaluation
  会制造 wrapper/default policy/owner，绕过 authenticated preflight role，并违反本单禁止新增
  owner/session/TTL/retry/wrapper 的边界。复制/伪造 `FiveRingTaskV2` 更明显越界。
- 父级复验点：Cloud 全源码出现唯一真实 FiveRing startup caller；caller 使用同一 exact run 的 preflight role 与配置
  snapshot；门关闭直接 allow；门开启时 LEADER/UNKNOWN/MEMBER/SOLO 映射与 `696a12b0` 一致；skip 返回
  `SKIPPED`，allow 继续原 FiveRing phase；不存在实时二次角色读取、默认 owner、TTL、retry 或 wrapper。

## Changed Files

- Java：无。
- 报告：新增
  `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-cloud-startup-check-five-ring-count-unit-worker-i21.md`。

## 验证与交接

- 按任务禁令未运行 Maven/build/test/runtime/Task/UI/capture/input，也未执行 Git mutation。
- 未修改、恢复、删除、移动或清理任何既有 dirty/untracked 文件。
- `countDelta=+1` 未应用；父级审查和 fresh Maven 前不得记账，本 Worker 不主张 `Approved`。
- 无已批准业务差异；本 Worker 因真实 active caller 缺失而停止，未实施不完整或伪可达迁移。

Handoff state: `BLOCKED P1=1 / MISSING ACTIVE CLOUD FIVE-RING STARTUP CALLER AND ASSEMBLY / PARENT SCOPE DECISION REQUIRED / countDelta=0`。

## Parent Source Review #1 - 2026-07-15T03:47:00-04:00

父级独立搜索确认 blocker 成立：active Cloud `src/main/java` 中 `checkFiveRing` 只有定义，无 Cloud
`FiveRingTask/FiveRingTaskV2` caller，也没有 production assembly 构造该 package-private startup service；真实 caller
仍只在 DHXY。单文件内新增 public/static 自调用会伪造 active chain 并绕过 authenticated preflight role。
结论：**P0=0/P1=1/P2=0，BLOCKED / countDelta=0**。解锁条件为完整 FiveRing Task 迁 Cloud 时同单接入
startup gate；本轮不计数、不进构建池。
