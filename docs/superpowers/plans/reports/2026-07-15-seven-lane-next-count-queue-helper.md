# Next Count Queue Helper - One Primary And One Backup

> 角色：Next Count Task Queue Helper；不是 manager/reviewer，不批准、不发单、不更新 ledger。
>
> 快照口径：按用户最新给定的 active 写集：External A=`BattleRadarService`、B=`PlayerStateService`、
> C=`LeftTopStatusSwitchService`、D=`AutoCombatService`；Internal I9=`CommonBoxService`、
> I10=`TaskMaintenanceService`；另一个 implementation lane 待排。
>
> 基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。本文只准备当前释放后的一个 primary 和一个
> backup；两者都不修改上述六个 Service 文件，不使用 generic shared 12，不把 DTO/helper/source-only/旁路 wire
> 当作计数完成。

## Queue Result

| Priority | Candidate | countUnit | countDelta | Status |
|---|---|---|---|---|
| Primary | `W-COUNT-NAVIGATION-CURRENT-MAP-ACTIVE-MACRO-2` | `NavigationService::navigateInCurrentMap` | `+1` | `READY` |
| Backup | `W-COUNT-AUTO-COMBAT-PANEL-RECORD-EXIT-1` | `AutoCombatPanelService::recordCombatExit` | `+1` | `READY`，发单前只核对未被其它 panel/exit 单合并计数 |

两张候选互斥：primary 只写 Cloud `NavigationService.java`；backup 只写 Cloud
`AutoCombatPanelService.java`。backup 只在 primary 因实时写集变化不能领取时替换，不与 primary 同时领取。

## Primary - `NavigationService::navigateInCurrentMap`

### Count Contract

- `countUnit`: `NavigationService::navigateInCurrentMap`
- `countDelta`: `+1`
- `status`: `READY`
- 适用原因：现有 active Cloud caller、field-complete typed macro、DHXY exact-window mechanics 和 closed result
  都已存在；父级此前唯一 blocker 是 active Cloud public 方法尚未实际调用该 macro。该缺口可在一个 Service 文件内闭合。

### Real Caller And Hop Evidence

1. **Real Cloud caller**：Cloud `NavigationService.java:232` 与 `:737` 直接调用 public
   `navigateInCurrentMap(...)`；不是 DHXY-only Task caller，也不是测试或 dormant handler。
2. **Cloud Service active entry**：Cloud `NavigationService.java:514` 的 public
   `navigateInCurrentMap(NavigationRequest)` 当前仍执行 Cloud 内 window/runtime/mini-map loop。父级已明确判定“wire
   存在但 active 方法未接入”，因此本单必须在此原入口完成替换。
3. **Existing typed boundary**：两仓已有 `LocalMacroKind.NAVIGATE_IN_CURRENT_MAP`、
   `NavigateInCurrentMapMacroCommand`、`NavigateInCurrentMapMacroResult`、request/outcome envelope、codec、digest 和
   handler 分支；不新增 kind、DTO、permit、codec 或 handler。
4. **Typed DHXY mechanics**：DHXY `LocalRemoteGameCommandHandler` 已在 exact registration/native binding 下恢复
   `NavigationRequest` 并调用本地 `NavigationService.navigateInCurrentMap(...)`。本地方法继续持有 60 秒 loop、
   mini-map candidate/input、click-confirm、pathing intent、keep-turn、stop checkpoint、`IN_COMBAT` 打断与 cleanup。
5. **Closed result**：现有 typed result 覆盖 `ARRIVED`、`PATHING_STARTED`、`STOPPED`、`INTERRUPTED`、
   `MAP_NOT_REACHED`、`POINT_NOT_REACHED`、`FAILED` 等终态，并可一对一映回原 Cloud `NavigationResult`，返回真实 caller。

完整链：

```text
Cloud NavigationService:232/:737 caller
  -> Cloud NavigationService.navigateInCurrentMap
  -> executeLocalMacro(NAVIGATE_IN_CURRENT_MAP)
  -> existing typed command/outcome transport
  -> DHXY exact-bound NavigationService mechanics
  -> typed NavigateInCurrentMapMacroResult
  -> original NavigationResult terminal
```

### Unique Write Set

- Modify only:
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NavigationService.java`
- Read-only evidence only: existing macro command/result, remote envelopes, codec/digest/handler, DHXY
  `NavigationService`, callers and this repository's business documents.
- Forbidden: the six active Service files named in the snapshot, generic shared 12, Runner/Task, new adapter/helper,
  new owner/session/TTL/retry, or a second navigation state machine.

### Dependencies

- 发单前确认没有其它 writer 占用 Cloud `NavigationService.java`。
- 现有 `NAVIGATE_IN_CURRENT_MAP` command/result 必须继续承载当前 request 的全部字段；若发现字段缺失，立即
  `BLOCKED`，不得扩 generic shared 或折叠 terminal。
- 保持 `696a12b0` request validation、stop、候选/点击确认、intent、keep-turn、delay/fallback、cleanup 和结果映射。

### Acceptance

1. Active Cloud public method实际调用现有 `executeLocalMacro(...NAVIGATE_IN_CURRENT_MAP...)`；不能只引用 handler 存在性。
2. Cloud 方法不再直接运行 window/runtime/mini-map/capture/input loop。
3. request 字段逐值进入 existing typed command；所有 closed macro terminal 明确映回原 `NavigationResult`。
4. `NOT_EXECUTED`、`STOPPED`、`UNKNOWN` 不伪装成到达/未到达业务真相；stop/checkpoint 语义保持。
5. 无 generic shared 12、DHXY Java、Runner/Task 或其它 Service 修改。
6. 父级源码审查与适用 fresh Maven 门通过同轮，ledger 才执行 `before -> before + 1`。

## Backup - `AutoCombatPanelService::recordCombatExit`

### Count Contract

- `countUnit`: `AutoCombatPanelService::recordCombatExit`
- `countDelta`: `+1`
- `status`: `READY`；发单前确认本单位未被 `verifyAndAlignPanel`、rounds refresh 或其它 exit 单合并计数。
- 启用条件：primary 因实时 `NavigationService.java` 写集冲突不能领取，且当前 AutoCombatPanel writer 已释放。

### Real Caller And Hop Evidence

1. **Real Cloud caller**：Cloud `AutoCombatService.java:361` 在 trusted combat-exit 被消费后直接调用
   `autoCombatPanelService.recordCombatExit()`；caller 只读，不纳入写集。
2. **Typed DHXY mechanics authority**：该 caller 的 exit 只来自 Cloud `BattleRadarService` 对 existing
   `BATTLE_RADAR_*` typed facts 的消费；facts 由 DHXY exact-window BattleRadar observation mechanics 产生，不以
   runner negative signal 或方法定义伪造退出。
3. **Cloud Service state unit**：`AutoCombatPanelService.java:396` 的 `recordCombatExit()` 按 `696a12b0`
   把 estimated rounds 固定减 `3`，保持下界和同一 per-window state；不触发额外 observation、输入或 refresh。
4. **Closed continuation**：更新后的 estimated rounds 由既有 panel maintenance/refresh policy 读取；方法以 closed
   void/state continuation 返回 `consumeExitAndRecover`，后续 recovery 顺序不变。

完整链：

```text
AutoBattleTask tick
  -> AutoCombatService.consumeExitAndRecover
  -> typed BattleRadar exit fact/state from DHXY mechanics
  -> AutoCombatPanelService.recordCombatExit
  -> estimated-rounds state (-3, bounded)
  -> existing recovery/panel-maintenance continuation
```

### Unique Write Set

- Modify only if an exact baseline gap exists:
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`
- 若 active 方法已等价完整，允许 `NO_CODE_CHANGE` 逐跳证据交付；不得制造无意义源码 churn。
- Read-only: `AutoCombatService` caller、BattleRadar typed facts/mechanics、DHXY、generic shared 和其它 Service。

### Dependencies

- 当前 AutoCombatPanel implementation lane 必须先释放。
- 发单前由父级确认 `recordCombatExit` 是独立未计 count unit；若已被其它 panel/exit countUnit 覆盖，则本 backup
  标为 `NEEDS_USER_DECISION`，不得重复记账。
- 不增加 observation、TTL、retry、refresh、输入或新的 rounds owner。

### Acceptance

1. 真实 caller 只在 trusted typed combat-exit 后调用一次；无 exit 不调用。
2. estimated rounds 严格按基线减 `3` 并保持既有下界/state owner；不改变 refresh 时机。
3. 不新增 DHXY observation/input；上游 exit 继续由 existing typed BattleRadar mechanics 闭合。
4. 不修改 `AutoCombatService`、六个 active Service 文件、generic shared 12、DHXY、Runner/Task。
5. 父级确认独立计数边界、源码审查与适用 fresh Maven 门通过同轮，ledger 才执行 `before -> before + 1`。

## Non-Binding Handoff

- 推荐领取顺序：primary；只有 primary 因实时写集冲突不可领取时才检查 backup。
- 本报告不构成 `Approved`、`CLAIMED`、发单或 ledger 变更。
- 未运行 build/test/runtime/Git；未修改 Java、External/Internal 固定日志、CR 或主计划。
- 无已批准业务差异；按 `696a12b0` 等价迁移。
