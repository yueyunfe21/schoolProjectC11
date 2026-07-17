# Seven-Lane Next Count Queue Helper 2

> 角色：Next-Task Queue + Delivery Preflight helper；非 reviewer、非实现者。
> 基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`；ledger：`189/407`。
> 只读快照：2026-07-15 03:12 EDT。本文不发单、不认领、不记账；不使用终审结论词。

## 当前写集避让

- A：Cloud `BattleRadarService.java`；B：Cloud `NavigationService.java`。
- C：TaskTracker READ/MATERIALIZE 完整 21-Java 双仓写集（含 Cloud/DHXY remote shared 与 handler）。
- D：Cloud `AutoCombatService.java`；I12：Cloud `AutoCombatPanelService.java`；I13：Cloud
  `TaskMaintenanceService.java`；I14：Cloud `AutoBattleTask.java`。
- 下列四张主单的 Java 写集彼此不同，也不命中以上当前写集。备选只在对应主单不能发出时替换；标为
  `NEEDS_PARENT_DECISION` 的备选不得直接发单。

## A/B/C/D 下一张主单

| Lane | countUnit | countDelta | 非绑定状态 | 唯一 Java 写集 |
|---|---|---:|---|---|
| A | `TaskStartupCheckService::checkAutoBattle` | `+1` | `CLEAR_TO_QUEUE` | Cloud `task/startup/TaskStartupCheckService.java` |
| B | `BaseTaskTemplate::sleepSafely` | `+1` | `CLEAR_TO_QUEUE` | Cloud `task/template/BaseTaskTemplate.java` |
| C | `CommonBoxService::clearPendingForRole` | `+1` | `NEEDS_PARENT_DECISION` | Cloud `service/CommonBoxService.java` |
| D | `SmartClickEvidenceConfirmationService::confirmExpectedOptionProof` | `+1` | `CLEAR_TO_QUEUE` | Cloud `service/NpcClickService.java` |

### A - AutoBattle 启动角色门

- matrix 独立行：`service-migration-matrix.md:1010`。
- active caller：Cloud `AutoBattleTask.java:122` 在 authority-minted context 的真实 patrol 入口调用
  `taskStartupCheckService.checkAutoBattle(context)`；closed result 在 `:123-127` 直接决定 skip/continue。
- countUnit：Cloud `TaskStartupCheckService.java:51`；保持 member allow、leader skip、UNKNOWN 按配置 allow/skip，
  不增加实时角色读取、fallback 或 wrapper。
- 唯一写集：
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/startup/TaskStartupCheckService.java`。
- 未在当前 27 个源码待构建清单或固定报告中发现该 countUnit。
- 备选：`NpcClickService::clickNpcSmartWithOutcome`（matrix `:1099`，active caller
  `NavigationService.java:690 -> clickNpcSmart`）。`NEEDS_PARENT_DECISION`：完整 NPC typed transport 仍可能命中 C 当前
  shared write set，且必须先确认 `clickNpcSmart` 与 `clickNpcSmartWithOutcome` 不是同一 public count boundary 的重复命名。

### B - stop-aware patrol sleep

- matrix 独立行：`service-migration-matrix.md:1017`。
- active caller：Cloud `AutoBattleTask.java:143,149` 每个 patrol 分支都调用
  `BaseTaskTemplate.sleepSafely(context, getPollingIntervalMs(context))`；countUnit 定义在
  `BaseTaskTemplate.java:177`。
- 闭合边界：保留 `TaskSleep.sleepOrStop` 的 stop-aware/interrupt 语义，返回当前 loop，不新增 retry、timer owner 或
  background scheduler。
- 唯一写集：
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/template/BaseTaskTemplate.java`。
- 未发现该 countUnit 已进入源码待构建清单。
- 备选：`PlayerStateService::ensureSheYaoXiangActive`（matrix `:1372`；active caller
  `AutoCombatService.java:409,472` 与 `TeamReturnService.java:67`）。`NEEDS_PARENT_DECISION`：历史固定报告已确认
  incense-status typed observation 需要 generic shared lane；C 当前仍占 shared 文件，不能直接发单。

### C - CommonBox role-off pending cleanup

- matrix 独立行：`service-migration-matrix.md:1351`；countUnit 是 public
  `CommonBoxService.java:226 clearPendingForRole`，不是 private helper。
- active caller：同类真实 consume/detect 入口在 `CommonBoxService.java:115,260` 的 role switch-off 分支调用；closed
  terminal 是按角色清除 pending，随后原 caller 返回，不触发 observation/input。
- 唯一写集：
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/CommonBoxService.java`。
- `NEEDS_PARENT_DECISION`：虽然 matrix 单列且方法 public，但它位于已 source-pending 的 CommonBox consume/member-detect
  caller graph 内；父级必须先确认该 public cleanup 行尚未随既有单位计数，避免重复 `+1`。
- 备选：`TeamReturnService::probeMemberReturnMarker`（matrix `:1394`）。`NEEDS_PARENT_DECISION`：I14 当前正在把
  `AutoBattleTask::tryRunLocalTeamReturnSelfCheck` 接到该 marker，需等 I14 EOF 后确认 active caller 已落地，且 marker
  observation 与 I14 public task decision 是两个独立 count boundary。

### D - smart-click proof commit boundary

- matrix 独立行：`service-migration-matrix.md:1105`。
- active caller：Cloud `DialogService.java:1562-1570` 从 `ObjectProvider<SmartClickEvidenceConfirmationService>` 取真实
  implementation 并调用 `confirmExpectedOptionProof(...)`；Cloud `NpcClickService.java:99` 实现该接口，方法定义在
  `NpcClickService.java:2271`。
- 闭合边界：只提交 sourceTask/actionKey/matchedText/proofToken/verificationStrength 证据到既有 smart-click memory；
  不重跑 NPC capture/input，不把 negative evidence 变成成功真值。
- 唯一写集：
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NpcClickService.java`。
- 未发现该 countUnit 已进入源码待构建清单；它与 C 当前 TaskTracker shared 写集无交集。
- 备选：`PlayerStateService::probeAndConsumeHealthyFirstAidNoFocus`（active caller
  `AutoCombatService.java:382,397,462`；定义 `PlayerStateService.java:248`）。`NEEDS_PARENT_DECISION`：matrix `:1373`
  单列的是 bars-probe 算法 cohort，而非此 public wrapper 的文字名称；需父级确认 parent row，且不能与当前 D
  `consumeExitAndRecover` 或已 pending cached-first-aid 重复计数。

## Delivery Preflight

### A dynamic polling - `CLEAR`

- delivery：`BattleRadarService::getDynamicPollingIntervalMs`，报告真实 EOF 已交 `NO_CODE_CHANGE` 证据。
- active chain：Cloud `AutoBattleTask.java:280-287 -> AutoCombatService.java:236-237 ->
  BattleRadarService.java:465`；matrix 独立行 `service-migration-matrix.md:1346`。
- 当前方法与 696 mirror `BattleRadarService.java:472` 分支逐值一致：`IN_COMBAT=4000`、
  `NAVIGATING/INTERACTING=2000`、`FREE/default=10000`。
- 风险注记：任务 brief 写过 `1000ms`，但该值属于 `FAST_EXPECTED_EXIT_PROBE_INTERVAL_MS`（当前 `:48`，696
  mirror `:56`），不属于本 countUnit。Worker 已如实指出，不能为满足 brief 新增 1000ms 分支。
- 非绑定结论：`CLEAR`；父级复核点仅为勘误 brief 的 `1000ms`，源码不应为此变更。

### D consume exit and recover - `CLEAR`

- delivery：`AutoCombatService::consumeExitAndRecover`，报告真实 EOF 已交 `NO_CODE_CHANGE` 逐跳证据；matrix 独立行
  `service-migration-matrix.md:1337`。
- active caller：Cloud `AutoCombatService.java:155`；countUnit 当前 `:345-414` 与 696 mirror `:344-413` 除行偏移外
  逐句一致。
- 顺序证据：consume exit `:351-355` -> clear expected/entry state `:358-360` -> record exit/reset `:361-362`
  -> CommonBox detect `:366-367` -> fast/full recovery 分支 `:368-406` -> stop/incense/FREE `:407-413`。
- 下游 BattleRadar、AutoCombatPanel record-exit、PlayerState first-aid、CommonBox 只作依赖；本 delivery 没有再次把这些
  private/helper 子链列为新 count unit，也未增加 TTL/retry/owner/session。
- 非绑定结论：`CLEAR`；父级复核点是确认本独立 matrix row 尚未随 `handleCombatTick` 整体计数。

## 边界声明

- 本文没有终审、发单、CLAIM 或 ledger 权限；`CLEAR/RISK` 仅为非绑定 preflight。
- 未运行 build/test/runtime/Git，未修改 Java、CR、ACTIVE_WORK、主计划、matrix 或任何固定 worker 报告。
- 无已批准业务差异；按 `696a12b0` 等价迁移。
