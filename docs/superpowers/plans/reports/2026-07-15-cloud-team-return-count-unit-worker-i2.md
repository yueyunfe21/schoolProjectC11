# Cloud TeamReturn Count Unit - Internal Worker I2

CLAIMED | role=Internal Count Worker I2 (implementation only, not reviewer) | countUnit=TeamReturnService::waitForMembersReturnIfNeeded | countDelta=+1 | claimedAt=2026-07-15 EDT

## Result

`SERVICE/PORT CHAIN IMPLEMENTED; COUNT BLOCKED PENDING CALLER GATE AND PARENT BUILD`。

- 业务权威：`696a12b0:src/main/java/com/bot/dhxy/service/TeamReturnService.java`。
- Cloud：`navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01`。
- DHXY：`thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f`。
- 请求计数为 `+1`，本 Worker 不写 ledger；当前不能冒领增量，原因见“Caller Gate”。
- 未运行 Maven、test、runtime/application/server/Task/poller/UI/capture/input；等待父级统一 fresh build。

## Method Comparison

| public 路径 | `696a12b0` / 已批准语义 | 当前 Cloud 闭合 |
|---|---|---|
| `clickReturnTeamIfPresent` | 首次按钮观察 -> found timestamp -> `ensureSheYaoXiangActive` -> 第二次 fresh 观察 -> X/Y 独立 `[-3,+3]` -> `CLICK_LEFT(150)` -> `SLEEP(500)` -> clicked timestamp | 原顺序全部保留；按钮 fact 与点击 bundle 经 TeamReturn 专属 port，下发 exact context |
| `waitForMembersReturnIfNeeded` | 初检；默认 `120000ms` deadline / `3000ms` poll；循环入口判 deadline -> checkpoint -> sleep -> fresh signal read；消失/超时均返回 true | 顺序、默认值、返回值不变；initial/poll 使用稳定 slot 和 typed leader fact；无 poll index、retry、TTL、park |
| `isReturnTeamSignalPresent` | 当前队长信号命中才 true | 仅 `OBSERVED + PRESENT` 为 true；无 context、失败或未决不制造 PRESENT |
| `beginLeaderSignalPrecheck` | 开包前冻结观察，异步 handle；失败可 inconclusive | exact-window typed observation 在方法返回前固定；不可变结果进入 `CompletableFuture`；失败为 completed FAILED |
| `consumeLeaderSignalPrecheck` | missing/stale/not-ready/failed/consume-error 均 inconclusive；signal/no-signal 才 conclusive | scope、future、reason 与返回矩阵保留；消费不再读取第二次 fact |
| `probeMemberReturnMarker` | 当前 approved 三态：只有成功捕获分析 miss 才 ABSENT | `PRESENT/ABSENT` 只来自 closed observed fact；capture/template/mechanics/transport/interrupt 均 UNKNOWN；零输入 |

## Files

| 文件 | 动作 | 说明 |
|---|---|---|
| Cloud `src/main/java/com/bot/dhxy/service/TeamReturnService.java` | 修改 | 去除永久本地 capture/template/input 依赖，接 TeamReturn 专属 typed port；补齐 marker、wait/live/precheck/consume public 面 |
| Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTeamReturnPort.java` | 新增 | TeamReturn 专属 closed contract；明确 exact context、screen-absolute 坐标与 terminal/state |
| Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTeamReturnPortAssembly.java` | 新增 | 组装既有 `TEAM_RETURN_BUTTON` / `TEAM_RETURN_LEADER_SIGNAL` fact 和单个固定 InputBundle |
| Cloud `PlayerStateService.java` | 只读 | 仅调用现有 `ensureSheYaoXiangActive(context)`；未修改 |
| DHXY `service/teamreturn/*LocalObservationMechanics.java` | 只读复用 | exact binding capture、模板分析、五态结果；未修改 |
| DHXY `LocalRemoteGameCommandHandler.java` | 只读复用 | 已挂载两个 TeamReturn fact branch 和 screen-absolute 投影；未修改 |

## True Chain Evidence

1. 成员 click 的真实 caller 已存在：Cloud `AutoBattleTask.java:194,253` -> Cloud `TeamReturnService` -> `CloudTeamReturnPortAssembly` -> `CloudGameClient` typed fact/InputBundle -> DHXY `LocalRemoteGameCommandHandler.java:832-841` -> TeamReturn 专属 local mechanics -> closed fact/terminal -> Service boolean。
2. exact-window 证据：handler 在 registration/binding gate 后以同一 `BindingAccess.context()` 调用同一 `access.binding()` 的 mechanics；PRESENT 的 client point 在 `LocalRemoteGameCommandHandler.java:968-1029` 用该 binding 原点转换为 `SCREEN_ABSOLUTE_PX`。
3. member/leader mechanics 逐项保留 `PRESENT/ABSENT/CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/MECHANICS_FAILED`，失败不伪装 ABSENT；按钮点击只有一个有序 bundle：`CLICK_LEFT(150) -> SLEEP(500)`。
4. leader wait 的 Service 到 DHXY 终态链已闭合：`wait-initial` / `wait-poll` -> TeamReturn port -> `TEAM_RETURN_LEADER_SIGNAL` -> exact-window mechanics -> typed terminal -> 原 120s/3s 循环决策。

## Caller Gate

- scoped `rg` 证明 Cloud 当前没有 `waitForMembersReturnIfNeeded`、`beginLeaderSignalPrecheck`、`consumeLeaderSignalPrecheck`、`probeMemberReturnMarker` 的任务 caller；`waitForMembersReturnIfNeeded` 在 DHXY 当前树也为零 caller。
- DHXY 的真实 precheck caller 仍在 `WubeiTask.java:2283,4588,4619`，member marker caller 在 `AutoBattleTask.java:286`，但对应 Cloud caller 尚未迁入；这些 Task 文件不在 I2 写集。
- `docs/superpowers/specs/2026-07-12-service-migration-matrix.md:3186-3199` 要求 reachable public caller，并规定写集外前置必须 `BLOCKED`。因此本 Worker 不能把“public 方法存在”冒充 `countDelta=+1` 已完成；父级需先裁决/补齐 caller 写集，再统一 build 和原子计数。
- precheck 当前可保持“返回前固定 exact-window observation + future/inconclusive 消费”，但已挂载 fact 在 DHXY 内同步完成模板分析；`696a12b0` 的本地像素分析与 return-item 并行度仍是父级源码审查点。Dormant `LeaderPrecheckFrameRegistry` 未激活，避免越权新增 owner/session/ledger。

## Scoped Checks

- 三个 Cloud 写入文件花括号分别 `170/170`、`18/18`、`14/14`，尾随空白均为 0；no-index diff check 仅报告 Windows LF/CRLF 提示，无 whitespace error。
- Cloud Service 的本地 `GameClientTracker/ImageFinder/CoordinateHelper/InputSequences/BufferedImage/ImageIO` 等残留引用为 0。
- 最终 SHA-256：Service `32CE892E...B8A5`，port `70991400...FFCD`，assembly `12974D2E...5FFC`；`PlayerStateService` 只读 SHA `6954F8EF...738B`。
- 未修改、回滚、清理、暂存、提交或推送任何共享文件；未运行 Maven，待父级统一 build。

**无已批准业务差异；按基线等价迁移。上述 caller/异步机械门尚未通过，故本 Worker 不宣称 count 已增加。**

## Parent Source Review #1 - BLOCKED - 2026-07-15T00:54:19-04:00

- **P1=1：countUnit 没有真实 active Cloud caller。** `waitForMembersReturnIfNeeded`、leader precheck consume 与
  member marker 当前只在 Service 内存在；真实基线 caller 仍在未迁入 Cloud 的 `WubeiTask` / local
  `AutoBattleTask`，不满足 hard count gate。
- **P1=1：leader precheck 时序尚未等价。** `696a12b0` 先冻结 capture，再让像素分析与 return-item 并行；当前
  typed fact 在方法返回前同步完成模板分析。它改变了可观察并行时序，不能在没有用户选择的情况下默认为等价。

结论：**P0=0 / P1=2 / P2=0，BLOCKED / NEEDS_USER_DECISION；COUNT NOT ELIGIBLE。** 当前源码保留、不得
回滚，但本单不进入构建计数队列。返修条件：先迁入一个真实 caller；对 precheck 只允许二选一后继续：保留本地异步
frame mechanics，或明确批准同步 typed observation 的时序差异。用户未选择前不猜测、不另造 owner/session/registry。
