# Internal Worker BX - TeamReturn Button Fact Adapter

## 状态

- `CLAIMED`: 2026-07-14 07:27:23 -04:00
- `IMPLEMENTED / COMPILE_BLOCKED_BY_UNRELATED_WRITE`: 2026-07-14 07:29:24 -04:00
- 角色：Internal Worker BX，只做实现与自审，不充当 reviewer。
- Java 写集：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\TeamReturnService.java`
- 文档写集：本报告。

## 基线与工作区保护

- DHXY：分支 `thin-client-design`，HEAD `0114604e1ff5f15491d2910959c45252e893d04f`；工作区存在大量他人 dirty/untracked，未回滚、覆盖、清理或提交。
- Cloud：分支 `navigation-migration`，HEAD `3b988caa010254973e03342272e6d1d6a9685b01`；工作区存在大量他人 dirty/untracked，目标 Java 文件编辑前为 untracked。
- 目标方法编辑前确认不存在：`rg -n "findReturnTeamButton" TeamReturnService.java` 无命中。
- 目标 Java 编辑前 SHA-256：`1EE6F16D4065EF880B36A8CC329E2779A96EB94B802F3D58A2E7703838079395`。
- 已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部及指定 direct migration plan。

## 源码基线证据

- committed `0114604e` 的 `TeamReturnService.findReturnTeamButton()` 返回 `Point` 或 `null`，命中点语义为 screen-absolute center。
- 当前 Cloud `WindowFact.TeamReturnButtonFact` 仅在 `PRESENT` 时携带非空 `clickX/clickY`，并强制 `CoordinateSpace.SCREEN_ABSOLUTE_PX`。
- 当前 Cloud `TaskExecutionContext.getGameClient().readWindowFact(...)` 是已存在的 typed fact 入口。

## 实施与验证

- 在 `TeamReturnService` 新增独立稳定 action slot：`return-button-point`。
- 新增 private dormant `findReturnTeamButton()`；没有新增或修改 caller。
- 每次调用只通过当前 exact `context.getGameClient().readWindowFact(...)` 读取一次
  `WindowFactKind.TEAM_RETURN_BUTTON`，timeout 复用字段 `timeoutMs`。
- 仅 `outcome.common().executionState() == OBSERVED`、fact variant 为
  `WindowFact.TeamReturnButtonFact` 且 state 为 `PRESENT` 时返回
  `new Point(fact.clickX(), fact.clickY())`。其余 state、非 `OBSERVED`、类型不符与
  `InterruptedException` 均返回 `null`；中断分支执行 `Thread.currentThread().interrupt()`。
- 未发送输入，未新增 retry/loop/caller/owner/session/ledger/TTL/wrapper，未改现有 member/leader probe。
- dormant 证据：`rg -n "findReturnTeamButton\\(" TeamReturnService.java` 仅命中 private 定义 1 次。
- 输入缺失证据：目标文件无 `InputSequences`、`InputAction`、`executeInputBundle` 或
  `executeLocalMacro` 命中。
- 目标 Java 最终 SHA-256：`B29642F441ACBD5C2CD85E191545103DAAD412B9305176E221B21F4A371F283A`。
- 已运行 Cloud `mvn -q compile`（未 clean），exit code `1`。失败来自写集外并行文件：
  `TaskMaintenanceService.java:197` 与 `TaskMaintenanceService.java:231` 均为“找不到符号：变量 log”。
  `TeamReturnService.java` 未出现在编译诊断中。BX 未越界修改该文件；需其 owner 完成后由父级重跑
  consolidated compile。
- 未运行 application/server/host/Task/poller/UI/capture/input，未做任何 Git mutation。

## 自审

- `SELF-REVIEW`: 源码范围与 typed fact 映射符合任务约束，未发现本写集 P0/P1/P2。
- 编译门当前未通过，原因与证据如上；不得据此宣称 build ready。
- 本自审不构成 `Approved`，最终 reviewer 仍由父级承担。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Source Review #1 - 2026-07-14T07:32:00-04:00

**SOURCE APPROVED，P0/P1/P2=0；最终构建门待统一执行。** 父级复核
`TeamReturnService.java:201` 的完整 private adapter：每次调用恰读取一次
`TEAM_RETURN_BUTTON`，action slot 为独立稳定值 `return-button-point`；仅
`ExecutionState.OBSERVED + TeamReturnButtonFact + PRESENT` 返回 typed fact 已约束的
screen-absolute `Point`。ABSENT、三类 mechanics failure、非 OBSERVED、variant mismatch 与 interrupt 均返回
`null`；interrupt flag 被恢复。

该方法保持 dormant，无 caller、input、loop/retry、wrapper、owner/session/ledger/TTL，也未改现有 member/leader
probe。目标 Java SHA-256 为 `b29642f441acbd5c2cd85e191545103daad412b9305176e221b21f4a371f283a`，与 BX
交付一致。BX 的 `mvn -q compile` 被写集外且当时仍在连续写入的 `TaskMaintenanceService.java` 中间态阻断，
`TeamReturnService.java` 不在诊断中；此项不记为 BX 源码缺陷。待全部 writer 稳定后由父级执行 consolidated
Cloud `mvn -q clean package`，通过后再关闭实现槽。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**
