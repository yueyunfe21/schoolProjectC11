# H6 Seven-Lane Next Count Queue

> 角色：H6 Next-Task Queue Helper，仅做非绑定提前排班；不是 reviewer，不作源码结论，不修改 Java、CR、External 日志或 ledger。
>
> 业务基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`；ledger 快照仍为 `189/407`。
>
> 仓库快照：DHXY `thin-client-design@0114604e`，Cloud `navigation-migration@3b988ca`；两仓既有 dirty/untracked 全部只读保护。
>
> EOF 观察时点：`2026-07-15 01:43 EDT`。后续父级若在固定日志追加替换单，以新的真实 EOF 为准，本报告不绑定实现槽。

## 排班事实校正

以下结论仅转录父级既有事实，不是 H6 的 reviewer 判断：

- C 的 `BattleRadarService::checkFastExpectedCombatExitByAvatarDiff` 与 I4 的
  `LeftTopStatusSwitchService::handleCombatMaintenance` 已有父级 `SOURCE APPROVED / COUNT PENDING BUILD` 记录。
- D 旧单 `NavigationService::navigateInCurrentMap` 已有父级 `BLOCKED P1=1` 记录：active Cloud 方法没有调用已存在的
  `NAVIGATE_IN_CURRENT_MAP` macro；旁路 wire/handler 存在不能替代 active caller chain。
- A `AutoCombatPanelService::alignPanelIfNeeded` 与 B `DialogService::handleDialog` 的 geometry 缺口均已被父级确认：前者缺
  drop-target window origin，后者 maintenance fixed strip 缺 cloud-safe window origin。

用户给定的“新波”在真实 EOF 后又发生了三次替换，因此当前排班事实如下：

| Lane | 用户给定新波 | 真实 EOF 当前单 | H6 起排点 |
|---|---|---|---|
| A | BattleRadar refresh baseline | `BattleRadarService::refreshFastExpectedCombatExitAvatarBaseline` 已于 `01:34:38` 交 NO_CODE_CHANGE 真链证据 | 等父级释放 A 后取下一单 |
| B | AutoCombat refresh facade | facade 因 Cloud 无真实 caller 被父级替换；`DialogService::detectDialogTypeNoFocus` 于 `01:42` 已领取 | 按 Dialog 当前单之后排，不重复 facade |
| C | LeftTop follower safe window | `LeftTopStatusSwitchService::consumeFollowerSafeWindow` 于 `01:41` 已交 NO_CODE_CHANGE 真链证据 | 等父级释放 C 后取下一单 |
| D | Dialog prepared validation | validation 已有父级源码结论；真实 EOF 于 `01:39` 续发 `CommonBoxService::hasPendingBoxForCurrentWindow` | 按 CommonBox 当前单之后排 |
| I1 | MapName canonicalize | `MapNameCanonicalizer::canonicalize` 已于 `01:36:54` 完成 62-key 基线返修 | 等父级释放 I1 后取下一单 |
| I4 | NpcClick clickNpcSmart | 当前源码检查确认五类 Npc mechanics 存在但 shared transport/production port 未接通 | 父级替换或整链接通并释放 I4 后取下一单 |
| I6 | PlayerState syncMyPosition | position 因无 current-location typed producer 被替换；当前 `performCachedFirstAidPlanNow` 的“失败保留”与 696 基线冲突 | 当前单先 `NEEDS_USER_DECISION`，释放后再取下一单 |

## 主队列写集

七张主任务的唯一业务 Java 写集依次为 `BattleRadarService`、`DialogService`、`AutoCombatService`、
`NavigationService`、`AutoCombatPanelService`、`CommonBoxService`、`PlayerStateService`，彼此互斥。Runner/Task 只能作为
只读 caller 证据，不得进入写集。每个备选只替换同 lane 主任务，不与主任务同时领取；启用备选前重新核对当时七线写集。

## A 下一张

### 主任务 `W-COUNT-BATTLE-RADAR-EXPECTED-EXIT-CONSUME-1`

- `countUnit`: `BattleRadarService::consumeCombatExitSignalForExpectedWait`
- `countDelta`: `+1`
- 真实 caller：Cloud `AutoCombatService::consumeExitAndRecover` 在 `AutoCombatService.java:352` 的
  `FAST_EXPECTED_EXIT` 分支直接调用；该分支由 public combat tick 图可达。
- Cloud Service：只接受 expected-wait arm 后产生的 exit pending；absent 返回 false，早于 arm 的陈旧 signal 清除后返回 false，
  fresh signal 单次清除后返回 true，保持 battle count/time/state 日志。
- 现有 typed DHXY mechanics/terminal：上游 `BATTLE_RADAR_AVATAR_BASELINE/PROBE` 与 full-radar facts 经 DHXY exact-window
  observation mechanics 产生 enter/exit state；本单位消费该 closed state，终态为 fresh=true、absent/stale=false。
- 唯一 Java 写集：Cloud `src/main/java/com/bot/dhxy/service/BattleRadarService.java`。
- 依赖和 blocker：A 当前 baseline-refresh 单先释放；发单前确认该矩阵单位尚未计数。不得增加第二次 observation、TTL 或 retry，
  不得把 negative fact 变成新的业务真值。

### A 备选 `W-COUNT-BATTLE-RADAR-EXPECTED-WAIT-ARM-1`

- `countUnit`: `BattleRadarService::armExpectedCombatExitWait`
- `countDelta`: `+1`
- 真实 caller：Cloud `AutoCombatService.java:136` 在 expected-exit wait 开始时调用。
- Cloud Service / typed terminal：记录 arm epoch 与当轮 enter 例外；后续既有 typed radar/avatar observation 只能让 arm 后的当轮
  exit 进入可消费 terminal，陈旧 pending 被隔离。
- 唯一 Java 写集：Cloud `BattleRadarService.java`。
- 依赖和 blocker：仅替换 A 主任务；必须证明 arm 与 consume 是两个未计数矩阵单位，不能把同一状态边界重复记账。

## B 下一张

### 主任务 `W-COUNT-DIALOG-GREEN-TEMPLATE-PREPARE-1`

- `countUnit`: `DialogService::prepareGreenTemplateOption`
- `countDelta`: `+1`
- 真实 caller：DHXY `XiuluoDialogPreparationProvider.java:44`、`WubeiDialogPreparationProvider.java:71` 与
  `XiuluoTaskV2.java:5562`；Runner/Task/Provider 全部只读，不进写集。
- Cloud Service：保持 template spec 顺序、nullable candidate 继续、supplied detection 同帧复用、miss keyword/action 构造与
  prepared-action identity，不改变 option fallback 顺序。
- 现有 typed DHXY mechanics/terminal：现有 green-template local macro 在 exact binding 下 capture/template-match，返回 closed
  prepared action/absent；后续点击与复用验证继续走既有 prepared-action typed mechanics。
- 唯一 Java 写集：Cloud `src/main/java/com/bot/dhxy/service/DialogService.java`；只有既有 dialog-green adapter 有精确 active 缺口时
  才纳入同一整链，generic shared 与 DHXY mechanics 只读。
- 依赖和 blocker：B 当前 `detectDialogTypeNoFocus` 先释放，且 D 不再占 Dialog。`NEEDS_USER_DECISION`：真实业务 caller 仍在
  DHXY Task/Provider，Cloud 当前未镜像这些 caller；若父级要求“Cloud caller 已落位”才计数，本单不得靠 wrapper 自调用补位。

### B 备选 `W-COUNT-DIALOG-ROUTE-KEYWORD-PREPARE-1`

- `countUnit`: `DialogService::prepareRouteKeywordOption`
- `countDelta`: `+1`
- 真实 caller：DHXY `WindowTaskRunner.java:2513` 调用三参 public overload；Runner 只读并排除实现写集。
- Cloud Service / typed terminal：target keyword -> existing option OCR words/image policy -> prepared click action；DHXY exact-window
  option observation 返回 match/absent closed terminal，Cloud 保留候选与 fallback 顺序。
- 唯一 Java 写集：Cloud `DialogService.java`。
- 依赖和 blocker：`NEEDS_USER_DECISION`，原因同主任务：Cloud caller 落点尚未出现；不得修改 Runner，也不得把 overload/helper
  本身当整链完成。

## C 下一张

### 主任务 `W-COUNT-AUTO-COMBAT-ENTER-MAINTENANCE-1`

- `countUnit`: `AutoCombatService::maybeHandleCombatEnter`
- `countDelta`: `+1`
- 真实 caller：Cloud `AutoCombatService.java:152/:202` 的两个 public tick 路径均调用该方法。
- Cloud Service：消费一次 combat-enter signal，设置 `pendingCombatEntryMaintenanceAt = now + 4s` 与
  `lastCombatUiCleanAt`，随后请求面板可见；没有 enter signal 时无状态变化。
- 现有 typed DHXY mechanics/terminal：enter signal 来自 BattleRadar typed facts；面板可见走既有
  `AUTO_COMBAT_PANEL` fact，miss 后仅允许原 `Alt+8 + 500ms` ordered InputBundle，经 DHXY exact binding/single queue 返回
  FOUND/NOT_FOUND/CAPTURE_FAILED 与 input terminal。
- 唯一 Java 写集：Cloud `src/main/java/com/bot/dhxy/service/AutoCombatService.java`；BattleRadar、AutoCombatPanel、DHXY、shared
  全部只读。
- 依赖和 blocker：C follower-safe 当前单先释放；A baseline-refresh 不再占 BattleRadar。不得把整个
  `handleCombatTick`、incense、rounds 或 task-turn 扩进本单位。

### C 备选 `W-COUNT-AUTO-COMBAT-DEFERRED-LEADER-RECOVERY-1`

- `countUnit`: `AutoCombatService::consumePendingLeaderPostCombatRecoveryIfAllowed`
- `countDelta`: `+1`
- 真实 caller：DHXY `WubeiTask.java:3343`、`XiuluoTaskV2.java:3526/:4835`；Task 只读。
- Cloud Service / typed terminal：pending + not-in-combat gate -> existing first-aid PROBE/cached-plan typed macro -> DHXY bars/input
  mechanics -> incense typed path -> closed boolean/state；保持 first-aid 与 incense 原顺序。
- 唯一 Java 写集：Cloud `AutoCombatService.java`。
- 依赖和 blocker：`NEEDS_USER_DECISION`：Cloud 尚无上述 Task caller，且 incense typed path 必须在发单时已闭合；不得迁 Task、
  新增 owner/session、或用自调用制造可达性。

## D 下一张

### 主任务 `W-COUNT-NAVIGATION-CURRENT-MAP-ACTIVE-MACRO-1`

- `countUnit`: `NavigationService::navigateInCurrentMap`
- `countDelta`: `+1`
- 真实 caller：Cloud `NavigationService.java:232/:737`；DHXY `WubeiTask.java:2075/:2621` 与
  `XiuluoTaskV2.java:3368` 也是基线 public callers，Task 只读。
- Cloud Service：在 active public 方法内实际发出 `NAVIGATE_IN_CURRENT_MAP` command，逐值传递 request 字段，并把 closed macro
  state/message 映射回原 `NavigationResult`；不得继续执行 Cloud 内的 window/runtime/minimap loop。
- 现有 typed DHXY mechanics/terminal：`LocalMacroKind.NAVIGATE_IN_CURRENT_MAP`、field-complete command 与十态 result 已存在；
  DHXY exact binding 还原 request 后运行既有 local Navigation mechanics，保留 ARRIVED/PATHING_STARTED/STOPPED/INTERRUPTED/
  MAP_NOT_REACHED/POINT_NOT_REACHED 等 closed terminal。
- 唯一 Java 写集：Cloud `src/main/java/com/bot/dhxy/service/NavigationService.java`；现有 macro command/result/codec/digest/handler、
  DHXY Navigation、Runner/Task 全部只读。
- 依赖和 blocker：D 当前 CommonBox 单先释放。必须以 active `executeLocalMacro` 调用为验收点；若现有 macro 无法承载某个
  696 request/result 字段，标 `NEEDS_USER_DECISION`，不得旁路、折叠 terminal 或扩成 contract-only 单。

### D 备选 `W-COUNT-TASK-TRACKER-WUBEI-READ-WHOLE-1`

- `countUnit`: `TaskTrackerPanelService::readWubeiTrackerPanel`
- `countDelta`: `+1`
- 真实 caller：DHXY `WubeiTask.java:2559/:2878/:4771`；Task 只读。
- Cloud Service / typed terminal：一次 tracker panel capture -> yellow title/OCR + green-link same-frame CPU policy -> closed
  `TaskTrackerPanelReadResult`；DHXY 已有 `TaskTrackerPanelRectLocalObservationMechanics` / capture mechanics 与
  `TASK_TRACKER_READ` typed payload family。
- 唯一写集：Cloud `TaskTrackerPanelService.java` + TaskTracker 专属 production port/assembly；DHXY mechanics 与 Task caller 只读。
- 依赖和 blocker：`NEEDS_USER_DECISION`。现有 DHXY handler 明确把 `TASK_TRACKER_READ` 标为 dormant，Cloud 同路径 Service 仍本地
  crop/OCR，且 Cloud WubeiTask caller 未落位；只有用户确认该落点可在一个 +1 整链内激活时才可替换 D 主任务。

## I1 下一张

### 主任务 `W-COUNT-AUTO-COMBAT-PANEL-ENSURE-VISIBLE-1`

- `countUnit`: `AutoCombatPanelService::ensurePanelVisible`
- `countDelta`: `+1`
- 真实 caller：Cloud `AutoCombatService.java:342` 的 combat-enter maintenance。
- Cloud Service：首次 panel observation -> miss 时唯一 `Alt+8 + waitAfterOpenMs` -> 第二次 observation -> found point 或 null，
  保持 missing watchdog 与 clear-on-found 顺序。
- 现有 typed DHXY mechanics/terminal：`WindowFactKind.AUTO_COMBAT_PANEL` -> DHXY exact-window
  `probeAutoCombatPanelFact` -> FOUND/NOT_FOUND/CAPTURE_FAILED；打开面板走 ordered InputBundle 与单输入队列。
- 唯一 Java 写集：Cloud `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`。
- 依赖和 blocker：I1 MapName 返修单先释放；本单位不需要 align 的 window origin，也不读取 rounds。两次 observation 必须都走
  typed fact，不得顺手修 align geometry。

### I1 备选 `W-COUNT-AUTO-COMBAT-PANEL-RECORD-EXIT-1`

- `countUnit`: `AutoCombatPanelService::recordCombatExit`
- `countDelta`: `+1`
- 真实 caller：Cloud `AutoCombatService::consumeExitAndRecover` 在 BattleRadar typed exit 被消费后调用。
- Cloud Service / typed terminal：typed BattleRadar exit terminal -> panel estimated rounds 按 696 固定减 3 并保持下界/刷新状态 ->
  后续 panel maintenance 读取同一 Cloud state；不新增观察或输入。
- 唯一 Java 写集：Cloud `AutoCombatPanelService.java`。
- 依赖和 blocker：发单前确认该业务状态单位未被 `verifyAndAlignPanel` 或 rounds-refresh 单合并计数；它只能作为完整 exit chain
  的方法单位，不能降格为 helper-only 证据。

## I4 下一张

### 主任务 `W-COUNT-COMMON-BOX-DETECT-CORE-1`

- `countUnit`: `CommonBoxService::detectBox`
- `countDelta`: `+1`
- 真实 caller：public `detectMemberBoxAfterCombatExit` 由 Cloud `AutoCombatService.java:366` 调用；public
  `detectLeaderBoxAfterReturnHome` 由 DHXY Wubei/Xiuluo task caller 调用，Task 只读。
- Cloud Service：保持 task/window/run/role/toggle/stop gates，执行一次 observation；仅 MATCHED 写 pending，negative terminal 不伪造
  pending，不增加二次 observation。
- 现有 typed DHXY mechanics/terminal：`CloudCommonBoxPort.observe` -> DHXY exact-window
  `CommonBoxLocalObservationMechanics` -> MATCHED/NOT_MATCHED/CAPTURE_FAILED/STOPPED/UNKNOWN closed result；消费端继续走既有
  ordered click bundle，但不属于本 detection countUnit。
- 唯一 Java 写集：Cloud `src/main/java/com/bot/dhxy/service/CommonBoxService.java`。
- 依赖和 blocker：I4 的 NpcClick 当前单须先由父级替换或释放，且 D 当前 `hasPendingBoxForCurrentWindow` 必须先释放 CommonBox
  写集；发单前核对 member-detect 已计边界，避免重复计算其 public entry。

### I4 备选 `W-COUNT-COMMON-BOX-LEADER-DETECT-1`

- `countUnit`: `CommonBoxService::detectLeaderBoxAfterReturnHome`
- `countDelta`: `+1`
- 真实 caller：DHXY `XiuluoTaskV2.java:4771` 与 `WubeiTask.java:4638`；Task 只读。
- Cloud Service / typed terminal：leader entry -> 同一 detect policy -> `COMMON_BOX` exact-window observation -> closed match/absent ->
  leader pending state；不包含 pending TTL 或 click retry。
- 唯一 Java 写集：Cloud `CommonBoxService.java`。
- 依赖和 blocker：`NEEDS_USER_DECISION`：该 public entry 与主任务 private core 的计数边界可能重叠，且 Cloud leader Task caller 尚未落位；
  父级必须先确认它是独立未计数单位，不能与主任务同时领取。

## I6 下一张

### 主任务 `W-COUNT-PLAYER-SYNC-IDENTITY-1`

- `countUnit`: `PlayerStateService::syncMyIdentity`
- `countDelta`: `+1`
- 真实 caller：Cloud `PlayerStateService::syncAll`；DHXY `DefaultWindowTaskStartupInitializer.java:68` 也是基线 caller，startup
  implementation 只读。
- Cloud Service：读取当前 `context.getMe()`，调用既有 `ClientIdentityService.scanAndSyncIdentity(me)`，保持 title parse、identity
  mutation、null/absent 与 stop 语义。
- 现有 typed DHXY mechanics/terminal：已存在 `WindowFactKind.BINDING` -> DHXY exact registration/native-binding title ->
  `ClientIdentityService` parser -> closed identity/no-change terminal。
- 唯一 Java 写集：Cloud `src/main/java/com/bot/dhxy/service/PlayerStateService.java`；ClientIdentity、BINDING fact/handler、DHXY 只读。
- 依赖和 blocker：I6 当前 cached-first-aid 单先完成 `NEEDS_USER_DECISION` 并释放；发单前确认本单位不会重复计算已完成的
  `ClientIdentityService::scanAndSyncIdentity` 或 title resolver。

### I6 备选 `W-COUNT-PLAYER-FIRST-AID-PROBE-CONSUME-1`

- `countUnit`: `PlayerStateService::probeAndConsumeHealthyFirstAidNoFocus`
- `countDelta`: `+1`
- 真实 caller：Cloud `AutoCombatService.java:382/:397/:462`。
- Cloud Service / typed terminal：existing no-focus PROBE -> DHXY exact-window four-bar mechanics -> HEALTHY/SUPPLY_NEEDED/UNKNOWN；
  HEALTHY 按基线消费/计数，SUPPLY_NEEDED 保存同帧 base + ordered cached plan，UNKNOWN 不伪装 healthy。
- 唯一 Java 写集：Cloud `PlayerStateService.java`；existing `PLAYER_STATE_FIRST_AID` port/macro/handler/mechanics 只读。
- 依赖和 blocker：仅替换 I6 主任务；必须保持 PROBE 一次、four-bar identity/order 与 plan capture base，不增加 retry，也不得采用
  当前 cached-first-aid 单尚未决的“失败保留”新语义。

## 非绑定取单规则

1. 任何 lane 只在真实 EOF 显示当前单已释放后领取下一张；本报告不发 CLAIM，不写实现状态。
2. 主任务七个 Service 写集互斥。备选只替换同 lane 主任务；替换时重新检查与其它六条的写集交集。
3. `NEEDS_USER_DECISION` 未消除前不得发单；不得用 Runner/Task 修改、自调用 wrapper、DTO/helper、contract-only、owner/session/
   ledger/TTL/retry 小单绕过。
4. 每张只能以一个既有 `countUnit`、`countDelta=+1` 闭合真实 caller -> Cloud Service -> typed DHXY mechanics -> closed terminal；
   Java/source-only、wire-only 或 helper-only 都不构成该单位完成。
5. 无已批准业务差异；所有候选均按 `696a12b0` 等价迁移。
