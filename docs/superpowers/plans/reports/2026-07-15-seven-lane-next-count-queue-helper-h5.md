# H5 Seven-Lane Next Count Queue

> 角色：Next-Task Queue Helper H5，仅做非绑定提前排班。本文不是源码审查结论，不修改 ledger、CR、External 日志或实现状态。
>
> 基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`；当前 ledger `189/407`。
>
> 当前写集避让：A `AutoCombatPanelService`，B `DialogService.handle*`，C `AutoCombatService`，D `NavigationService`，I1 `TeamReturnService.clickReturnTeamIfPresent`，I4 `PlayerStateService.performStartupFirstAidCheck`，I6 `ClientIdentityService.scanAndSyncIdentity`；同时不占用等待统一构建的 `TaskMaintenanceService`、`SummonSkillService`、`BattleRadarService`、`LeftTopStatusSwitchService`、`CommonBoxService`。

## 排班结论

下列四张均按单一 `countUnit`、`countDelta=+1` 设计。只有对应“发单前置”满足时才可从队列取出；前置未满足时不得缩成 DTO、接口、handler 或源码盘点单。A/C 两张都可能需要共享 remote 文件，因此不得同时领取：先完成 A 的 tracker cohort，再释放共享文件给 C；D、I1 与二者 Java 写集互斥，可并行。

## External A 下一张主任务

### `W-COUNT-TASK-TRACKER-READ-WHOLE-1`

- `countUnit`: `TaskTrackerPanelService::read(TaskTrackerReadRequest)`
- `countDelta`: `+1`
- 真实 caller：`com.yueyunfe.dhxy.cloudbrain.DecisionEngine::trackerPanelRead`，当前在 `DecisionEngine.java:305` 调用 same-path `TaskTrackerPanelService.read(request)`。
- 完整整链：`DecisionEngine.trackerPanelRead` -> same-path Cloud `TaskTrackerPanelService.read` -> `RemoteOperation.TASK_TRACKER_READ` -> DHXY `LocalRemoteGameCommandHandler` -> `TaskTrackerPanelCaptureLocalMechanics` 的 exact-window capture/anchor/panel crop -> `TaskTrackerReadOutcome` -> Cloud 保留的 panel/detail 几何、绿链分割、fingerprint/cache、候选排序和 typed terminal。
- 现有 typed boundary：Cloud `TaskTrackerReadRequest`、`TaskTrackerReadOutcome`、`TaskTrackerMaterializeRequest/Outcome`、`WindowFactKind.TASK_TRACKER_PANEL_RECT`；DHXY `RemoteGameOperation.TASK_TRACKER_READ/TASK_TRACKER_MATERIALIZE_ACTION`、`RemoteTaskTrackerReadOutcomePayload`、`RemoteTaskTrackerPanelRectFact`、`TaskTrackerPanelCaptureLocalMechanics`。这些合同已经存在，但 handler/retained 路径仍以 dormant 分支拒绝执行。
- 唯一预计 Java 写集：
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngine.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRetainedActionState.java`
  - `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java`
  - `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/remote/RemoteOperationPayloadCodec.java`
  - `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/remote/RemoteProtocolDigests.java`
- 发单前置/风险：A 当前 `AutoCombatPanelService` 写集必须先释放；上述六文件需确认没有别的共享-wire writer。不得只启用 panel-rect fact，也不得把算法留在 DHXY。若 exact capture artifact 无法通过现有 `TASK_TRACKER_READ` 返回，则整单继续等待，不拆素材 DTO。
- 为什么可真实 `+1`：它从已有生产 caller 一次闭合到 exact-window 本地 capture mechanics，再回到 Cloud 全算法和 closed typed result；不是 panel rect、codec 或单方法源码存在性计数。

## External C 下一张主任务

### `W-COUNT-NPC-SMART-CLICK-WHOLE-1`

- `countUnit`: `NpcClickService::clickNpcSmart`
- `countDelta`: `+1`
- 真实 caller：Cloud `NavigationService.java:772` 的 `npcClickService.clickNpcSmart(ZHANG_WEN_NPC.toClickRequest(me))`；caller 只读，不纳入本单写集。
- 完整整链：`NavigationService` -> Cloud `NpcClickService.clickNpcSmart` -> Cloud FIFO/memory/strategy decision -> typed local player-anchor / yellow-target / task-tooltip / prepared-point mechanics -> DHXY single input queue 与 exact-window capture/template verification -> typed terminal -> Cloud FIFO outcome/fallback -> boolean terminal 返回原 caller。
- 现有 typed boundary：`CloudNpcPlayerAnchorPort`、`CloudNpcYellowTargetPort`、`CloudNpcTaskTooltipPort`、`CloudNpcPreparedPointPort` 及对应 `Npc*MacroCommand/Result`；普通 move+click 复用 ordered `InputBundle`，按键与 capture 交错的 Ctrl probe 必须作为一个 closed local macro。
- 唯一预计 Java 写集：
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NpcClickService.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudNpcPlayerAnchorPort.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudNpcYellowTargetPort.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudNpcTaskTooltipPort.java`
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudNpcPreparedPointPort.java`
  - 一份新建、同时实现四个端口的 Cloud transport adapter（固定放在 `com/yueyunfe/dhxy/cloudbrain/remote`，文件名由父级发单时锁定）。
- 发单前置/风险：必须等 A 的 tracker shared-wire cohort 完全释放；若四种现有 macro kind 仍未进入 shared allowlist/handler，本单必须把该共享接线纳入同一实现波并由父级重新锁定精确写集，不能先交四个接口实现。若共享文件当时仍冻结，则本单留在队列。
- 为什么可真实 `+1`：现有 Navigation caller 已可达；本单以一个 Service public terminal 覆盖完整 FIFO、四类本地事实/动作、输入安全和最终结果，不以任何 port/DTO 单独计数。

## External D 下一张主任务

### `W-COUNT-MAP-NAME-CANONICALIZER-WHOLE-1`

- `countUnit`: `MapNameCanonicalizer::canonicalize`
- `countDelta`: `+1`
- 真实 caller：Cloud `NavigationService::canonicalMapName`（`NavigationService.java:2752`）以及 Cloud `TaskTrackerPanelService` 的 tracker map-text 分支（`TaskTrackerPanelService.java:907`）。本单只读这两个 caller，不修改当前 D/A 写集。
- 完整整链：typed local navigation/tracker observation 提供原始地图文字 -> Cloud `MapNameCanonicalizer.canonicalize` 按 `696a12b0` 的 exact/fuzzy/ambiguity 阈值纠名 -> caller 保持原分支 -> 既有 `NavigationResult` 或 tracker typed terminal。Navigation 机械终端继续使用 `LocalMacroKind.NAVIGATE_IN_CURRENT_MAP`；tracker 机械终端在 A 主任务闭合后使用 `TASK_TRACKER_READ`。
- 现有 typed boundary：`NavigateInCurrentMapMacroCommand/Result`、`LocalMacroRequest/Outcome`；另一入口复用 A 的 `TaskTrackerReadRequest/Outcome`。Canonicalizer 本身只拥有 Cloud 纯算法，不新增本地 matcher。
- 唯一预计 Java 写集：
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/MapNameCanonicalizer.java`
  - Cloud 已有地图名字权威资源文件（父级发单前必须锁定当前实际路径；只允许一个既有 resource 文件，不新建 Java helper）。
- 发单前置/风险：D 当前 Navigation Implementation #2 先完成父级处理；A 的 tracker 路径尚未闭合时，本单仍可先以 Navigation caller 验收，但不得修改 `NavigationService.java`。必须确认 Cloud 资源内含基线 `maps.json` key 与 `TRANSFORM_ONLY_MAP_NAMES`；不得继续读取 DHXY `images/template/map_label`，也不得改动距离阈值、次优差值或 ambiguous 返回原文语义。
- 为什么可真实 `+1`：它清除 Cloud Service 对 DHXY 本地模板目录的隐式依赖，并把真实 Navigation caller 的地图文字完整送入 Cloud 算法后回到 closed navigation terminal；不是搬常量或补资源清单。

## Internal I1 下一张主任务

### `W-COUNT-WUBEI-REQUIRED-RETURN-PRESCAN-WHOLE-1`

- `countUnit`: `ReturnItemPrescanService::afterTrackerGreenRequired`
- `countDelta`: `+1`
- 真实 caller：基线 `WubeiTask::clickTaskTrackerGreenAtPoint`，在 tracker green click 成功并注册 pathing intent 后调用 `afterTrackerGreenRequired`（DHXY 当前约 `WubeiTask.java:3334`）。该调用顺序、probe-only 分支与普通 return-item 分支必须原样保留。
- 完整整链：Cloud Wubei tracker-green caller -> Cloud `ReturnItemPrescanService.afterTrackerGreenRequired` 的 per-round strategy/state -> existing `LOCAL_MACRO/BAG_RETURN_ITEM` -> DHXY `BagService` exact-window scan/template match/input queue -> `BagReturnItemMacroResult` -> Cloud cache/fallback state -> caller继续原有 common-box/first-aid 顺序。
- 现有 typed boundary：Cloud `BagReturnItemMacroCommand/Result`、`LocalMacroKind.BAG_RETURN_ITEM`、`LocalMacroRequest/Outcome`；DHXY 对应 payload/codec/digest、`LocalRemoteGameCommandHandler` 与 `BagService` local mechanics 已是 closed terminal。
- 唯一预计 Java 写集：
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`（该 active Cloud caller 文件当前不存在时，必须按 `696a12b0` 完整类 promotion 后再做同单原调用点适配，禁止只建一个抽空 caller/helper）
  - `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/ReturnItemPrescanService.java`
- 发单前置/风险：I1 当前 TeamReturn 写集先释放；active Cloud 若仍没有完整 WubeiTask caller，则本单保持等待，不能把调用塞进 `AutoBattleTask`、TaskTracker Service 或 runner。此单不得改 Bag/UICleaner/GiveItem/QuestManager 的本地归属，不得新增 strategy、TTL、retry 或自动 fallback。
- 为什么可真实 `+1`：它要求真实 Wubei caller 和既有 BAG closed macro 在同一任务内可达，最终结果回写 Cloud prescan state；不会把 ReturnItem DTO、macro schema 或孤立 Service 方法当作完成。

## 并行取单顺序

1. D 当前 Navigation 处理完成后，可先取 `W-COUNT-MAP-NAME-CANONICALIZER-WHOLE-1`；它不写 Navigation。
2. A 当前写集释放且 shared tracker 文件无人占用后，取 `W-COUNT-TASK-TRACKER-READ-WHOLE-1`。
3. I1 当前 TeamReturn 写集释放后，只有 active Cloud 完整 WubeiTask caller 可同单 promotion 时才取 ReturnItem 单。
4. C 等 A 释放 shared remote 文件后取 NpcClick 单；若 shared allowlist/handler 仍有 writer，继续排队，不拆零计数前置。

以上仅为非绑定队列。父级发单前仍需按真实 EOF、实时 dirty 写集和最新 caller 可达性重新锁定文件表。
