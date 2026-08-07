# G010 修罗维护固定补时与超时放弃跨端清场

## 目标

修复同一场 `2026-08-03 22:20:26-22:23:41` 暴露的四个相互连接但必须独立验收的问题：

1. 医宝宝/修装备没有按业务约定获得完整的额外入战预算。
2. Cloud watchdog 放弃旧轮后，Client 旧看打计划仍能继续匹配和点击。
3. Client 已识别并写入“大雁塔四层”，Cloud 当前任务的 `gameContext.me` 却仍保留“灵兽村”，两份位置状态没有同步。
4. 超时失败恢复直接回到普通接任务起点；旧位置恰好触发“NPC 在附近”的直点捷径，绕过本会 fresh sync 的导航入口，于是在错误地图点击灵兽村使者。

## 现场事实

- `22:20:26.899`：接受任务点击完成，基础 `180s` watchdog 开始。
- 当前代码实际只补回 `after-accept`、维护 broadcast 本体及固定 handoff 的短耗时；修装备导航和等待没有补回。
- `22:23:33.551`：Client 执行 Cloud 的 `WHOLE_TASK_RECOVERY_RESET`，日志确认 ACK 成功。
- `22:23:34.052-22:23:38.409`：旧 attempt 的 local-kanda schedule 仍继续 probe 并最终 claim。
- `22:23:21.754`：Client Runner 已把本窗口位置写为 `大雁塔四层 (24,65)`；因此第三项不是位置 OCR/识别未发生。
- `22:23:36.127`：新 accept 流程收到 `灵兽村使者` 的 NPC memory 候选，依据仍是陈旧
  `dialog_player:101_83`；角色实际位于大雁塔四层。
- `22:23:36.361`：新流程点击该错误地图上的缓存坐标 `(888,201)`。
- `22:23:38.987`：旧流程点击看打；`22:23:40.901` 才确认进入战斗。

## 固定预算规则

```text
基础预算                         180s
触发医宝宝且未加过额度           +30s
触发修装备且未加过额度           +60s
两者都触发                       270s 总预算
同一 hook 的导航重试/点击重试     不重复加额度
```

“触发”定义为当前 accepted round 正式进入该维护 hook，而不是 cooldown 探测、后台候选或成员广播。
额度跟随当前 round context；新接受任务重新开始基础预算和额度标记。

## 超时放弃合同

watchdog timeout 不能只清 pathing。Cloud 放弃旧 attempt 的同一 command-plane 决策必须原子完成：

1. 清除 exact attempt 的 pathing snapshot 与 observation pathing lineage。
2. 清除 exact attempt 的 local-kanda schedule、claim 和 expected-combat ticket。
3. 清除 exact attempt 尚未消费的 prepared/local action；不得清其他窗口或新 attempt。
4. Client 返回包含清除结果的 ACK；Cloud 收到成功 ACK 后才开始恢复。
5. 若清场前 exact attempt 已确认 `IN_COMBAT`，战斗事实优先，禁止并发启动 accept-NPC 动作。

## 恢复合同

放弃 accepted objective 后，不能直接 `XiuluoRoundContext.start(...)`，再让普通接任务入口使用旧
`gameContext.me` 决定是否直点 NPC：

1. Client pathing terminal / pre-combat coordinate 写入窗口位置后，必须把同一 exact-window 位置事实同步到 Cloud 当前 task state；不得只更新 Client `WindowRuntimeContext.gameState.me`。
2. 超时恢复必须先读取 fresh bound-window 位置；旧缓存不得参与“NPC 在附近”的直点判断。
3. 已在灵兽村且处于 NPC 直接点击范围，才允许使用接任务 NPC memory。
4. 不在起点时进入显式返程/导航恢复；可复用既有 `navigateToNPC(灵兽村使者)`，因为其 `navigateToMap` stale-cache guard 在没有可用 pathing snapshot 时会调用 `syncMyPosition()`，但恢复入口不得先被 stale near-NPC 捷径截走。
5. 确认回到起点后，才进入 `ACCEPT_TASK_CLICK_NPC`；不得在远端地图直接构造灵兽村 NPC memory 候选。

## 验收门

- 预算测试：无维护 `180s`、仅医宝宝 `210s`、仅修装备 `240s`、两者 `270s`；重试不重复增加。
- 跨端连通性：timeout command -> Client exact-attempt clear ACK -> Cloud recovery，顺序不可颠倒。
- 负例：timeout 后旧 attempt 的 probe、claim、click 均为零。
- 位置桥：Client 收到 `PATHING_COORDINATE_RESOLVED` 后，Cloud 当前 task state 必须可读到同一地图/X/Y；不得出现 Client=大雁塔、Cloud=灵兽村。
- 地图门：角色在大雁塔时不得提交“灵兽村使者”NPC memory 点击，必须先回起点。
- 恢复 phase：timeout 后首个恢复输入必须属于返程/导航，不得属于接任务 NPC 直点；fresh 位置确认到灵兽村后才开放直点捷径。
- 竞态：timeout 与迟到 `IN_COMBAT` 同时发生时，只能进入战斗等待或清场恢复之一，不能两条输入链并发。
- 双仓 compile 和 focused contract tests 通过后，重启 Client+Cloud 做 fresh runtime。

## 交付与父级终审（2026-08-04）

### 已落地

1. `XiuluoRoundContext` 保存当前 accepted round 的一次性维护预算标记；医宝宝增加 `30s`、修装备增加
   `60s`，同一 hook 重试不重复增加，基础/单项/双项预算为 `180/210/240/270s`。
2. `WHOLE_TASK_RECOVERY_RESET` 支持 exact `taskRunId + round + attemptId`：Client 在同一原子区清除该
   attempt 的 pathing、observation lineage、local-kanda schedule/claim/progress、expected/pending combat
   ticket 和尚未消费的 prepared action/job，并返回逐槽结构化 ACK。旧 identity 完整 no-op；同 attempt 已确认
   `IN_COMBAT` 时返回 `combatAlreadyConfirmed`，战斗分支获胜。
3. Client 将 `PRE_COMBAT_COORDINATE_RESOLVED` 与 exact `PATHING_COORDINATE_RESOLVED` 形成可重发的
   `POSITION_SAMPLE`；Cloud 只在 tenant/device/window/run/observerSeq 全部命中当前 active task 时更新该任务的
   `GameContext.me`，错误窗口、错误 run 和 stale seq 均拒绝。
4. phase failure、loop guard 与目标导航放弃统一进入 `routeToAcceptNpc`；恢复入口先 fresh sync，且恢复态禁止
   stale near-NPC 直接点击，随后复用已有 `navigateToNPC(灵兽村使者)`。

### 父级审核结论

- `P0/P1/P2 = 0/0/0`。
- 双仓生产编译：Client `mvn -o -q -DskipTests compile` 通过；Cloud `mvn -o -q compile` 通过。
- 双仓共享协议文件 SHA-256 一致：`TurnExactAttemptRecoveryResetAck`、
  `TurnWholeTaskRuntimeArguments`、`TurnWholeTaskRuntimeResult`、`TurnProtocolValidator`、
  `ObservationPositionValue`。
- Worker 隔离副本定向测试：exact reset/协议 Client `5/5`，位置桥 Client `1/1`，恢复合同 Cloud `4/4`，
  位置桥 Cloud `1/1`，全部通过。
- 父级直接在当前 worktree 运行 Maven 定向测试时，Client 被无关测试缺
  `LocalPathingStartProofMechanics` 阻断；Cloud 被既存旧 `DialogService` 构造器、旧协议枚举和其他测试漂移
  阻断。生产编译成功，且隔离副本中的 G010 定向用例均已执行通过；这些既存 testCompile 问题不归入 G010。

### Fresh runtime 门

必须同时重启 Client 和 Cloud。重点观察：

1. `limitMs` 只出现 `180000/210000/240000/270000`，相同维护 hook 重试不再增长。
2. timeout 后先出现 exact-attempt reset ACK，再开始 accept recovery；ACK 后旧 attempt 的
   `local-kanda probe/claim/click` 必须为零。
3. Client 识别远端地图坐标后，Cloud 日志必须出现同 window/run 的 current task position update。
4. 非灵兽村超时恢复的首个业务输入必须是返程/导航；fresh 确认已在灵兽村 NPC 范围后才允许 memory 直点。
5. timeout 与迟到 `IN_COMBAT` 竞态只能出现 `WAIT_COMBAT` 或清场恢复之一。
