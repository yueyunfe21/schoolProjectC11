# Seven-Lane Delivery Preflight Helper H4

## Role And Boundary

- Role: seven-lane non-binding preflight / next-task helper H4; not manager, reviewer, or implementer.
- Baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`; no business difference is proposed.
- Actions: source/document inspection only. No Java edit, build, test, runtime, Task/UI, capture, input, or Git mutation.
- This report is the only write set.
- Current external write sets excluded from every candidate:
  - A: Cloud `AutoCombatService.java`.
  - B: Cloud `PlayerStateService.java` and `service/playerstate/CloudPlayerStateIncenseStatusPort.java`.
  - C: the exact 29-Java TaskTracker READ/MATERIALIZE/final-consumed scope.
  - D: Cloud `NpcClickService.java`.

## Pending-Pool Baseline And Dedup Method

`docs/ACTIVE_WORK.md` records 38 deduplicated units at 03:42. The later I22 parent entry adds
`CommonBoxService::isRoleEnabled`, so the observed pending-pool baseline for this preflight is 39. I21
`TaskStartupCheckService::checkFiveRing` remains outside that pool. None of the six candidates below is one of
those 39 units.

The scoped dedup command was run read-only for every exact candidate:

```powershell
$files = Get-ChildItem docs\superpowers\plans\reports -File -Filter '2026-07-15*count-unit*.md'
$files | Select-String -SimpleMatch '<exact countUnit>'
```

The same exact names were also checked in `docs/ACTIVE_WORK.md`. Results:

| Candidate | 2026-07-15 count-unit hit | Pending-pool duplicate |
|---|---:|---:|
| `AutoCombatPanelService::recordAutoPanelMissing` | 1 historical I20 claim; I20 changed assignment before source work | 0 |
| `DialogChoiceMemoryService::findUsableRoute` | 0 | 0 |
| `NavigationService::registerWindowPathingIntent` | 0 | 0 |
| `DialogService::handleDialog` | 0 | 0 |
| `WorldMapRouteResultMemoryService::findClean` | 0 | 0 |
| `BattleRadarService::updateCombatState` | 1 I19 evidence report; that unit stayed at `countDelta=0` | 0 |

## Immediate Internal Queue

### I23 Next - Auto-panel missing watchdog

- `countUnit`: `AutoCombatPanelService::recordAutoPanelMissing`
- `countDelta`: `+1`
- Exclusive Java write set: Cloud
  `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`.
- Compile shape after all writers stabilize: Cloud `mvn -q clean package`; no DHXY Java scope.
- Active caller chain:
  1. `AutoBattleTask:163 -> AutoCombatService.handleCombatTick:152 -> maybeHandleCombatEnter` reaches
     `ensurePanelVisible`; combat-maintenance callers also reach `verifyAndAlignPanel -> ensurePanelMatchVisible`.
  2. `AutoCombatPanelService:206-208` maps a closed input non-execution to
     `recordAutoPanelMissing(..., ":input-failed") -> null`.
  3. `AutoCombatPanelService:255-258` maps a second observed panel miss to
     `recordAutoPanelMissing(..., ":not-found-after-alt8") -> null`.
  4. `recordAutoPanelMissing:495-523` starts the streak, logs before ten minutes, rate-limits the existing
     attention signal after ten minutes, and closes by writing the current window warning/metric only when a
     current window exists.
- Count boundary: only the watchdog transition and warning terminal; do not recount `ensurePanelVisible`, panel
  observation, input bundle, or I23 alignment.
- Preflight: `CLEAR`.

### I24 Next - Route dialog memory read

- `countUnit`: `DialogChoiceMemoryService::findUsableRoute`
- `countDelta`: `+1`
- Exclusive Java write set: Cloud
  `src/main/java/com/bot/dhxy/service/DialogChoiceMemoryService.java`.
- Compile shape after all writers stabilize: Cloud `mvn -q clean package`; no DHXY Java scope.
- Active caller chain:
  1. `NavigationService:684 -> requestLingShouVillageRouteDialogPreparation:755` is the real route-preparation
     caller.
  2. `NavigationService:761-763 -> MemoryService.findUsableRouteDialogChoice:68-70 ->
     DialogChoiceMemoryService.findUsableRoute:175-177` performs the exact lookup.
  3. The route key remains `navigation|routeTransfer|from->target`; `findUsable -> findByKey:195-203` returns a
     value only for a present, usable entry.
  4. `NavigationService:765-776` closes both branches: remembered coordinates/text populate the preparation
     request, while an empty result writes null remembered fields and preserves the existing route fallback.
- Count boundary: read/reuse branch only. The matrix currently groups `findUsableRoute / recordRouteSuccess`, but
  active Cloud has no `recordRouteDialogChoiceSuccess` caller. Parent must explicitly permit the read half as one
  count unit or defer it until the write half becomes active; no synthetic write caller may be added.
- Preflight: `NEEDS_PARENT_DECISION`.

### I25 Next - Pathing intent registration

- `countUnit`: `NavigationService::registerWindowPathingIntent`
- `countDelta`: `+1`
- Exclusive Java write set: Cloud `src/main/java/com/bot/dhxy/service/NavigationService.java`.
- Compile shape after all writers stabilize: Cloud `mvn -q clean package`; no DHXY Java scope. Schedule only after
  the current Navigation pending-pool source snapshot has passed its unified build, otherwise it would invalidate
  that snapshot.
- Active caller chain:
  1. `navigateToNPC:216-244 -> navigateToMap:272` reaches `registerWindowPathingIntent:478` when the map leg
     returns `PATHING_STARTED` and no nested route owns the intent.
  2. `submitWorldMapSearchAndClickDestination:1538-1567` calls the same method for yellow destination + mini-map
     and legacy map-route clicks.
  3. `registerWindowPathingIntent:2697-2730` closes null request/current-window as `false`; otherwise it builds one
     target-map/optional-coordinate intent, calls `markPathingStarted` exactly once, logs the exact binding and
     returns `true`.
- Count boundary: intent ownership and closed boolean only; do not recount map search/click, current-map macro,
  `navigateToNPC`, or watcher settlement.
- Architecture risk: the matrix says pathing-intent registration is a DHXY-retained mechanic, while active Cloud
  still writes `WindowRuntimeContext` directly. Parent must decide whether this unit first moves registration to
  an existing typed local mechanic or remains deferred; a second intent owner is forbidden.
- Preflight: `NEEDS_PARENT_DECISION`.

## Mutually Exclusive Reserve Queue

### Reserve 1 - Dialog whole dispatcher

- `countUnit`: `DialogService::handleDialog`
- `countDelta`: `+1`
- Exclusive Java write set: Cloud `src/main/java/com/bot/dhxy/service/DialogService.java`.
- Active caller chain: `AutoBattleTask:208 -> TaskMaintenanceService.runOpportunisticMaintenance:578 ->
  handleMaintenanceBroadcast:599 -> DialogService.handleDialog:151`; the maintenance request enters the fast path
  at `:154-156`, then `finishRequest` returns a closed dialog result which TaskMaintenance maps at `:605-621` to
  handled/interrupted/failure/no-action.
- Compile shape: Cloud `mvn -q clean package`; no DHXY Java scope unless a missing typed mechanic is proven, in
  which case this one-file candidate must stop for rescoping.
- Risk: previous whole-dispatch inspection found reachable desktop capture/geometry in the maintenance path.
  Reissue only after proving the existing typed dialog observation and geometry boundaries cover every reachable
  branch without changing handler order, click order, fallback, or stop terminals.
- Preflight: `RISK`.

### Reserve 2 - Clean world-map route memory lookup

- `countUnit`: `WorldMapRouteResultMemoryService::findClean`
- `countDelta`: `+1`
- Exclusive Java write set: Cloud
  `src/main/java/com/bot/dhxy/service/WorldMapRouteResultMemoryService.java`.
- Active caller chain:
  1. `NavigationService:1581/1628 -> MemoryService.findCleanWorldMapRouteResult:88-95 ->
     WorldMapRouteResultMemoryService.findClean:56/68`.
  2. Missing, disabled, or dirty entries close as `Optional.empty`; only a clean enabled entry closes as present
     (`WorldMapRouteResultMemoryService:71-91`).
  3. The yellow destination path maps empty to `NOT_FOUND` and continues the existing OCR/search fallback; the
     present branch consumes the remembered relative point before the existing mini-map leg.
- Compile shape: Cloud `mvn -q clean package`; no DHXY Java scope.
- Count-boundary risk: this method is present in the matrix's Service inventory but is not currently a standalone
  method row. Parent must create one unique matrix row before dispatch or reject it as part of a larger route-memory
  unit; helper/facade methods are not separately countable.
- Preflight: `NEEDS_PARENT_DECISION`.

### Reserve 3 - Battle state transition core

- `countUnit`: `BattleRadarService::updateCombatState`
- `countDelta`: `+1`
- Exclusive Java write set: Cloud `src/main/java/com/bot/dhxy/service/BattleRadarService.java`.
- Active caller chain:
  1. `AutoBattleTask:163 -> AutoCombatService.handleCombatTick:150 ->
     BattleRadarService.checkAndSyncCombatState:65` is the ordinary production caller.
  2. Visible auto-flag/selection/top facts call `updateCombatState(true)` at `:71/:89/:107`; the repeated-miss plus
     readable-minimap exit gate calls `updateCombatState(false)` at `:130`.
  3. `updateCombatState:331-351` closes transition/no-transition as boolean, writes only `IN_COMBAT/FREE`, and
     emits the existing one-shot enter/exit state.
  4. `AutoCombatService:333/353` consumes those one-shot signals through
     `consumeCombatEnterSignal:385-391` and `consumeCombatExitSignal:399-407`.
- Compile shape: Cloud `mvn -q clean package`; no DHXY Java scope.
- Risk: I19 tied this same row to a missing FAST_EXPECTED_EXIT task caller. A reissue must either be explicitly
  limited to the already-active ordinary radar transition, or wait for the FiveRing/Xiuluo fast-policy caller;
  changing AutoBattle from full recovery to fast recovery is not allowed.
- Preflight: `NEEDS_PARENT_DECISION`.

## Dispatch Order

1. On I23 release, dispatch `AutoCombatPanelService::recordAutoPanelMissing` (`CLEAR`).
2. On I24 release, dispatch `DialogChoiceMemoryService::findUsableRoute` only after its count-row decision.
3. On I25 release, dispatch `NavigationService::registerWindowPathingIntent` only after its ownership decision and
   the existing Navigation pending snapshot's unified build.
4. Keep `DialogService::handleDialog` as the first ready reserve after its typed geometry audit.
5. Keep the two memory/radar reserves parked until their stated parent decisions; do not fill a slot with a
   helper-only or dormant caller.

Overall preflight: `RISK` because only the I23 next unit is immediately dispatchable without a count-boundary or
ownership decision. No build/test/runtime/input/Git action was performed.

## CURRENT QUEUE ONLY - 2026-07-15 Emergency Refresh

> H4 仅给非绑定排班事实，不作源码裁决、计数记账或实现认领。本节以当前源码和
> `docs/ACTIVE_WORK.md` 为准；排除了当前 A `AutoCombatPanelService::recordAutoPanelMissing`、C TaskTracker
> READ/MATERIALIZE 29-Java、D `AutoCombatService::initializeForCurrentWindow`、I33
> `AutoBattleTask::maybeRunIdleMaintenance`、B parked incense 写集，并排除了既有状态集合中的 exact countUnit。

### Immediate mutually-exclusive count queue

| Queue | countUnit | countDelta | Active caller and closed result | Unique Java write set | NO_CODE_CHANGE |
|---|---|---:|---|---|---|
| Q1 | `NavigationService::observeRoutePlanFacts` | `+1` | DHXY `NavigationService.navigateToMap:331-338 -> observeRoutePlanFacts:527`; 返回一个完整 `RoutePlanObservation`，其 snapshot/current-map/intent/geometry facts 在 `:338-354` 被同轮 route-plan request 直接消费。 | Cloud `src/main/java/com/bot/dhxy/service/NavigationService.java` | 否；Cloud 当前没有该方法，按矩阵独立行迁入且不修改 DHXY caller。 |
| Q2 | `LeftTopStatusSwitchService::handleLeaderStartup` | `+1` | DHXY `DefaultWindowTaskStartupInitializer:99-108 -> LeftTopStatusSwitchService.handleLeaderStartup:62`；unsupported 关闭为 `SKIPPED`，supported 经 `checkAndMaybeClose` 后关闭为 `SwitchActionResult`，initializer 随后继续 stop/startup gate。Cloud 同路径方法为 `:49-55`。 | Cloud `src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java` | 是；现有 Cloud 方法已保持单一 `SwitchActionResult` 终态。 |
| Q3 | `UICleanerService::cleanLightweightInterruptions` | `+1` | Cloud `SummonSkillService:181 -> CloudUiCleanerPort:52-60 -> LOCAL_MACRO`，DHXY `LocalRemoteGameCommandHandler:1296 -> UICleanerService:209-230`；最终关闭为 `HANDLED/NOT_EXECUTED/fatal terminal`，public consumer 得到 boolean。 | Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudUiCleanerPort.java` + DHXY `src/main/java/com/bot/dhxy/service/UICleanerService.java`（该双文件集合仅作本 countUnit 的完整边界，和其余五张无交集） | 是；当前 typed macro 已到真实 DHXY mechanic 和 closed boolean。 |
| Q4 | `ObjectiveTextRecognitionService::recognize(raw,source)` | `+1` | DHXY `XiuluoTaskV2:6330 -> ObjectiveTextRecognitionService.recognize(BufferedImage,String):106`；返回 `Optional<ObjectiveTextResult>`，caller 以 present/empty 关闭 objective 读取分支。Cloud `DialogService:1610-1621` 也直接消费同签名结果为 `STORY_OBJECTIVE_READ/NOT_FOUND`。 | Cloud new `src/main/java/com/bot/dhxy/vision/ObjectiveTextRecognitionService.java` | 否；Cloud 当前通过依赖可见该类但没有同路径源码，本单只建立该矩阵行的 Cloud 源码 owner。 |
| Q5 | `NpcClickService::pollFreshStoryBlockerEvent` | `+1` | DHXY `NpcClickService:388 -> pollFreshStoryBlockerEvent:509`；只接受 opt-in、同 task、序列更新的 fresh `STORY_DIALOG_VISIBLE`，关闭为 fresh `WindowReadyEvent` 或 `null`，caller 据此停止当前 smart-click 探测分支。 | Cloud `src/main/java/com/bot/dhxy/service/NpcClickService.java` | 否；Cloud 当前类没有该独立矩阵方法。 |
| Q6 | `ReturnItemPrescanService::useCached/hasCached/invalidate` | `+1` | DHXY `XiuluoTaskV2:1616 -> hasCached`；`XiuluoTaskV2:5342`、`WubeiTask:4575 -> useCached`；失败后 `XiuluoTaskV2:5360`、`WubeiTask:4594 -> invalidate`。`hasCached/useCached` 关闭为 boolean，invalidate 清 cache、重开 fallback 状态，caller 随即进入既有 return/fallback 分支。 | Cloud `src/main/java/com/bot/dhxy/service/ReturnItemPrescanService.java` | 是；当前 Cloud 方法组与 active DHXY caller 的 boolean/state 终态同形。 |

### Matrix and de-dup evidence

- 独立矩阵行：`NavigationService::observeRoutePlanFacts`=`service-migration-matrix.md:1392`；
  `LeftTopStatusSwitchService::handleLeaderStartup`=`:1384`；
  `UICleanerService::cleanLightweightInterruptions`=`:1421`；
  `ObjectiveTextRecognitionService::recognize(raw,source)`=`:1435`；
  `NpcClickService::pollFreshStoryBlockerEvent`=`:1393`；
  `ReturnItemPrescanService::useCached/hasCached/invalidate`=`:1404`。
- Exact de-dup command used for every row:

```powershell
rg -n --fixed-strings '<exact countUnit>' docs/ACTIVE_WORK.md docs/superpowers/plans/reports -g '!**/*helper*.md'
```

- Result: all six exact countUnit strings returned zero matches. Therefore none is named in the current 44-unit build
  pool or a prior non-helper countUnit record. `TeamReturnService::probeMemberReturnMarker` was not queued because
  its exact boundary already appears in the I14 worker record.
- Write-set intersection: Q1/Q2/Q3/Q4/Q5/Q6 are six disjoint Java sets. Their union has zero intersection with the
  current A/C/D/I33/B parked sets listed above. Each queue item remains exactly one matrix countUnit and
  `countDelta=+1`; helper/DTO/port subparts are not separate increments.

No Java/build/test/runtime/input/Git action was performed.
