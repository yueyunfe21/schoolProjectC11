# Cloud TaskMaintenance Count Unit - Worker I3

`CLAIMED | worker=Internal Count Worker I3 | role=implementation-only | countUnit=TaskMaintenanceService::runOpportunisticMaintenance | countDelta=+1 | claimedAt=2026-07-15T00:20:00-04:00`

## 交付结论

- 状态：`SOURCE CLOSED`；本计数单位的现有真实链已闭合，`countDelta=+1`。
- 业务权威：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`，Git blob `e93cfd01d9c282f98881a6311b8bb806bbc3e359`。
- 本次只把 Cloud `TaskMaintenanceService` 残留的本地 `WindowTaskContextHolder/WindowRuntimeContext` 身份读取换成同一 Cloud `TaskExecutionContext`：显式参数优先，合法 null 兼容路径再读 `TaskExecutionContextHolder.current()`，最后才保持 baseline 的 `default/epoch=0/unknown-log` fallback。
- `runOpportunisticMaintenance` 本体、broadcast-first 优先级、`BROADCAST_FAILED/INTERRUPTED` 短路、summon gate/claim/cooldown/cache/backoff、cleanup result、`GameContext.ActionState` finally 恢复均未改变。
- 无已批准业务差异；按基线等价迁移。
- 按父级并行写入约束，本 Worker 未运行 Maven、测试、runtime/application/server/Task/poller/UI/capture/input；待父级统一 build。

## Baseline Method Map

以下为 `696a12b0` 的完整顶层方法表，共 `47` 个；scoped 对账结果为 active `47`、missing `0`、added `0`。

| baseline lines | visibility | methods / responsibility | 本次去向 |
|---|---|---|---|
| 68-194 | public | `initializeForTaskStart`, `beginTeamMaintenanceRound`, `openTeamPathingMaintenanceWindow`, `openTeamFirstAidMaintenanceWindow`, `closeTeamMaintenanceWindow`, `openLocalTeamReturnSupportWindow` | 原样保留：启动 cooldown、round、pathing/first-aid/return capability 开关 |
| 208-338 | public | `closeLocalTeamReturnSupportWindow`, `isTeamPathingMaintenanceWindowOpen`, `awaitTeamFirstAidMaintenanceWindowOpen`, `awaitLocalTeamSupportCapabilityOpen`, `isLocalSupportMemberSession`, `registerLocalTeamSessionCandidate` | 原样保留：窗口关闭、等待/唤醒、session 候选 |
| 364-469 | public | `markLocalTeamWindowRoleDetected`, `isLocalSupportMemberCandidate`, `isPendingLocalSupportLeaderDetection`, `markLocalTeamLeaderDetected`, `isLocalTeamSupportCapabilityOpen` | 原样保留：角色探测、leader/session/capability 状态机 |
| 478-560 | private/public | `hasDetectedLocalLeader`, `openLocalTeamSupportCapability`, `closeLocalTeamSupportCapabilities`, `completeLocalTeamSessionWindow`, `isTeamFirstAidWindowOpen` | 原样保留：capability epoch、完成窗口与 first-aid gate |
| 579 | public | `runOpportunisticMaintenance` | 本计数单位；原控制流不变，继续调用 `normalize -> checkpoint -> handleMaintenanceBroadcast -> maybeCleanSummonSkill` |
| 600-625 | private | `handleMaintenanceBroadcast`, `maybeCleanSummonSkill` | 原样保留：broadcast terminal 映射；summon 全部门、claim、cache、cooldown、finally 与结果映射 |
| 800-918 | private | `logSummonSkillNotDue`, `buildSummonSkillCleanupRequest`, `updateSummonSkillWindowState`, `isSummonSkillTailSafeCacheExpired`, `isSummonSkillTailSafeCacheFresh`, `findLastConfirmedEffectiveSlotIndex`, `isEffectiveSummonSkillSlot`, `isUnknownSummonSkillFailure`, `invalidateSummonSkillLayoutCache` | 原样保留：日志节流、四字段 cleanup request、2h cache、slot/UNKNOWN 语义 |
| 939-981 | private | `releaseSummonSkillRoundClaimIfOwned`, `hasSummonSkillStateChange`, `normalize`, `checkpoint` | 原样保留：无状态变化才释放 claim、request 默认值、stop checkpoint |
| 987-1027 | private | `currentWindowKey`, `summonSkillState`, `currentPlayerIdentityEpoch`, `logPrefix` | 等价 Cloud 身份适配：window/identity/log 取同一 explicit-or-bound task context；cache drift 逻辑不变 |
| 1031-1099 | private | `resolveTeamRoundKey`, `resolveLocalSupportCapabilityRoundKey`, `normalizeTeamKey`, `pruneOlderTeamRoundClaims`, `teamRoundKey` | 原样保留：team/capability round key 与旧 round claim/state 清理 |

两个 private state type `SummonSkillWindowState`、`LocalTeamSessionState` 字段零改动；`activeTeamRoundByKey`、`teamMaintenanceWindowStateByRound`、`localTeamSessions`、`summonSkillClaimsByTeamRound` 等整体协调状态仍由同一 Service 持有。

## 真链证据

1. Active Cloud `AutoBattleTask.java:113` 用共享 `TaskExecutionContextHolder.callWith(context, ...)` 覆盖整次 patrol；`:208` 把同一个显式 context 调入 `TaskMaintenanceService.runOpportunisticMaintenance`。
2. Active Cloud `TaskMaintenanceService.java:578-596` 保留 baseline 优先级；`:624-797` 保留召唤兽 due/free-state/team round/capability/claim/cache/unknown-backoff/finally 全链，`:755` 仍调用只读协作者 `SummonSkillService.cleanSummonSkillsOnce(request)`。
3. Active Cloud `SummonSkillService.java:172-227` 从同一 holder 取得 exact context，四字段构造 `WholePassIntent`，`:206` 单次调用真实 `context.getRemoteGameClient().summonSkillWholePass().execute(intent)`，并分别处理 `Executed/NotExecuted/Stopped/Unknown`；该文件本次未修改。
4. 本次身份适配使 `TaskMaintenanceService` 不再依赖 Cloud 不存在的 `WindowTaskContextHolder/WindowRuntimeContext`。显式 run context 直接提供 `windowId/playerIdentityEpoch/logPrefix`；null 兼容路径仍消费同线程 holder，不引入 global/default-first 假身份。
5. `AutoCombatService` 对 session/capability/first-aid API 的既有 caller 全部保留；47 个 baseline 方法名与 active 一致，因此本 unit 没有截断 `TaskMaintenanceService` 的整体协调图。

## 文件表

| file | action | evidence |
|---|---|---|
| `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java` | Modify | 仅 import/constructor collaborator、summon state context 穿线和三个 identity/log helper；active SHA-256 `39aef8085fdc8afa0e0f51f8016c307e6f34ab407baf30cce52c6e88f14cd996` |
| `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-cloud-task-maintenance-count-unit-worker-i3.md` | Add | 本 Worker 唯一报告 |
| `SummonSkillService.java` | Read only | SHA-256 前缀 `2ee437f1b82470da`；现有 whole-pass typed terminal 直接复用 |
| `PlayerStateService.java` | Read only | SHA-256 前缀 `6954f8ef93083536`；未修改 |
| `LeftTopStatusSwitchService.java` | Read only | SHA-256 前缀 `615ac8b4d7fc90d5`；未修改 |

未新增 TaskMaintenance 专属 model/port/assembly：现有 `TaskExecutionContext`、共享 holder 和 `CloudSummonSkillWholePassCapability` 已提供真实 producer，新增第二套端口或 assembly 只会复制 authority，不是闭合本 unit 的必要条件。

## Scoped Checks

- `git diff --no-index --check migration-baseline/.../TaskMaintenanceService.java active/.../TaskMaintenanceService.java`：无 whitespace error；仅 PowerShell/Git 的 LF-to-CRLF 工作树提示。
- baseline/active 方法清单：`47 / 47`，missing `0`，added `0`。
- scoped no-index diff：只出现本报告所述 context plumbing；未改 `runOpportunisticMaintenance` 的业务分支或任何 frozen collaborator。
- 未运行 Maven 或测试；待父级在 Java writers 稳定后统一执行 Cloud package/build gate。

`DELIVERED | countUnit=TaskMaintenanceService::runOpportunisticMaintenance | countDelta=+1 | businessDifference=NONE | buildGate=PARENT_PENDING`

## Parent Source Review #1 - SOURCE APPROVED / COUNT PENDING BUILD - 2026-07-15T00:32:00-04:00

父级独立对照 Cloud migration-baseline `696a12b0` 与 active 文件。47 个 baseline 方法全部保留，无 missing/added；
`runOpportunisticMaintenance`、broadcast-first、summon due/claim/cooldown/cache/backoff/result/finally 主体无业务差异。
唯一源码差异是把 Cloud 不存在的 `WindowTaskContextHolder/WindowRuntimeContext` 身份读取替换为同线程
`TaskExecutionContext` / `TaskExecutionContextHolder`，显式 context 优先，null 兼容仍保持 default/epoch0/unknown
fallback；未引入 global-first、额外 gate、retry、TTL 或状态转换。

结论：**P0=0 / P1=0 / P2=0，SOURCE APPROVED。** `countUnit=TaskMaintenanceService::runOpportunisticMaintenance`
已满足源码门，但 B 与其它 writers 仍在连续写入，当前禁止并发构建。因此计数状态为
`COUNT PENDING BUILD`，ledger 暂保持 `189/407`；所有 Java 稳定后的 fresh Cloud `mvn -q clean package` 通过当轮，
必须把 ledger 原子改为 `190/407`。若构建失败，本 unit 退回原 I3 修复并保持不计数。
