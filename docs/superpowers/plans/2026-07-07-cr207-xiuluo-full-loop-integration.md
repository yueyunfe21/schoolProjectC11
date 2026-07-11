# CR207 修罗云脑完整闭环 integration gate

## 背景

Fresh runtime `2026-07-06 22:16:52` 证明单个 phase mapping 漏项会导致：

```text
actionOutcome accepted
-> cloud step rejected / empty phase
-> local execute gate: phase must parse to XiuluoPhase
-> xiuluo.brain.loop.failClosed
```

当前已有窄修复覆盖 `ACCEPT_TASK_NAVIGATE_TO_NPC + localOutcomeNextPhase=ACCEPT_TASK_DIALOG`
的 mapping，但这不足以要求用户再次 fresh runtime。

## 目标

补一个 fresh 前必须通过的完整修罗闭环 integration gate。测试要模拟一轮修罗从云脑 start 到 round done：

1. `PREPARE_ROUND`
2. `ACCEPT_TASK_NAVIGATE_TO_NPC`
3. 接任务近距离直点 shortcut 或普通点击路径
4. `ACCEPT_TASK_DIALOG`
5. 接任务后维护/读 tracker
6. tracker shortcut 或 objective fallback 路由
7. 点目标 / 确认入战
8. `WAIT_COMBAT`
9. `RETURN_HOME` / `WAIT_TEAM_RETURN`
10. `ROUND_DONE`

## 必测分支

- 至少一条完整 happy path：接任务近距离直点 `灵兽村使者` shortcut，继续走到 `ROUND_DONE`。
- 至少一条 route 分支：tracker shortcut 或 objective fallback 进入目标导航/点目标/入战。
- 测试必须能挡住“某一步 actionOutcome accepted，但下一步 step 返回空 phase/不可解析 phase”的回归。

## 图片 / fixture 要求

- 有真实视觉输入的节点要使用 repo 现有 testcase/case 图片或 logs case 作为 fixture。
- 至少覆盖 tracker/dialog/key visual node 中的可用图片；如果某个 phase 只能用 protocol outcome facts 模拟，测试名或注释必须明确写出这是 protocol-only boundary。
- 不要求真实点击游戏窗口，不启动真实客户端。

## 允许修改

- DHXY 测试代码：
  - `src/test/java/com/bot/dhxy/cloud/xiuluo/XiuluoBrainDevServerTest.java`
  - `src/test/java/com/bot/dhxy/task/xiuluo/*XiuluoBrain*Test.java`
  - 如需小型测试 helper，可放同包 test source。
- DHXY dev sidecar：
  - `src/test/java/com/bot/dhxy/cloud/dev/CloudDecisionDevServer.java`，仅在测试暴露真实缺口时按生产/external parity 修补。
- external cloud brain：
  - `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\XiuluoBrainProtocolTest.java`
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\DecisionEngine.java`，仅在测试 RED 暴露协议缺口时修补。

## 禁止修改

- 不改 OCR/template/click/navigation/NPC/dialog/image 算法。
- 不恢复本地 fallback，不把云端失败吞掉。
- 不改真实业务 phase 语义来迎合测试。
- 不 reset/revert/clean；工作区已有大量无关改动，必须绕开。

## TDD / 验证

Worker 必须先写 RED，记录失败原因，再最小 GREEN。

必跑：

```powershell
mvn -q "-Dtest=com.bot.dhxy.cloud.xiuluo.XiuluoBrainDevServerTest,com.bot.dhxy.task.xiuluo.XiuluoBrainAcceptTrackerWiringTest,com.bot.dhxy.task.xiuluo.XiuluoBrainRouteEnterBattleWiringTest,com.bot.dhxy.task.xiuluo.XiuluoBrainCR207BatchAWiringTest,com.bot.dhxy.cloud.xiuluo.XiuluoBrainCloudDecisionServiceTest" test
mvn -q -DskipTests compile
mvn -q -DskipTests test-compile
```

external:

```powershell
mvn -q -Dtest=XiuluoBrainProtocolTest test
```

## 文档

- 更新 `docs/ACTIVE_WORK.md` 和 CR207 卡片，记录 RED/GREEN、测试名、fixture 图片/边界。
- 若 CR207 行/卡状态变化，运行：

```powershell
node scripts/generate-cr-dashboard-data.js
```

## 完成门槛

- Worker 只交付本地 gate，不得声称 fresh 通过。
- 必须两名独立 reviewer 批准“完整闭环 gate 足以阻止此类 fresh 前错误”后，谢帅才能通知用户可 fresh runtime。
