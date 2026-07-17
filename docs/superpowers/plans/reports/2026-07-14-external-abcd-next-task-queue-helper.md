# W-ABCD-NEXT-QUEUE-1

## CLAIMED

- task: `W-ABCD-NEXT-QUEUE-1`
- claimedAt: `2026-07-14T13:16:44-04:00`
- role: Internal Worker CN（External A/B/C/D 下一任务编排 helper；不是 reviewer，不下 APPROVED/BLOCKED）
- unique write set: append-only `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-14-external-abcd-next-task-queue-helper.md`
- read-only scope: `696a12b0` 基线、whole-service 计划、迁移矩阵、Cloud 最新编译失败类别、A/B/C/D 固定日志真实 EOF、两仓 `git status`。
- constraints: 不改 Java/POM/主文档；只形成四路互斥直接实现候选及备用队列；落点歧义项标 `NEEDS_USER_DECISION`；最终发单和审查由父级负责。

## Candidate Queue #1

- preparedAt: `2026-07-14T13:21:40-04:00`
- compile delta: fresh Cloud `mvn -q clean package` exit `1`；`BotProperties` 与 `SheyaoxiangDigitTemplateReader` 错误已消失，剩余为 desktop/window/capture/OCR/input collaborator 与永久本地 `BagService` / `GiveItemService` 类型。

### External A

- task id: `W-696-LEFT-TOP-STATUS-TYPED-ADAPT-1`
- 精确互斥写集: Cloud `src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java`；append-only A 固定日志 `docs/superpowers/plans/reports/2026-07-13-cloud-npc-click-service-worker-a.md`。
- 前置: active blob 仍为 `a46fde69e7d11bca315b75600fd737ef7f924912`；现有 `WindowFactKind.LEFT_TOP_STATUS`、`WindowFact.LeftTopStatusFact`、`CloudGameClient` 与 ordered `InputBundle` 均不变且可用；领取时该 Service 无其它 writer。
- 验收: 完整保留 `696a12b0` 全方法、OPEN/CLOSED/UNKNOWN/CAPTURE_FAILED 分支、pending 状态、顺序/delay/log；仅在原模板观察点改读 typed fact、原点击点改一个 ordered bundle；移除该类对 desktop tracker/template/input/window-runtime collaborator 的编译依赖；不改 remote/schema/其它 Service；scoped diff/check 干净，父级 fresh Cloud package 归因。
- status: `READY`

### External B

- task id: `W-696-AUTO-COMBAT-PANEL-TYPED-ADAPT-1`
- 精确互斥写集: Cloud `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`；append-only B 固定日志 `docs/superpowers/plans/reports/2026-07-13-cloud-team-return-service-worker-b.md`。
- 前置: active blob 仍为 `bf63d2c78873afd8a0781d97f080a59b2b327942`；现有 `WindowFactKind.AUTO_COMBAT_PANEL`、`WindowFact.AutoCombatPanelFact`、`CloudGameClient` 与 ordered `InputBundle` 可用；领取时该 Service 无其它 writer。
- 验收: 完整保留 `696a12b0` 回合估算、缺失看门狗、刷新/拖拽条件、状态更新、顺序/delay/fallback/log；仅把原面板观察换 typed fact，把原 Alt/拖拽物理序列换等序 ordered bundle；移除该类 desktop capture/template/input collaborator 编译依赖；不改 remote/schema/其它 Service；scoped diff/check 干净，父级 fresh Cloud package 归因。
- status: `READY`

### External C

- task id: `W-696-COMMON-BOX-TYPED-ADAPT-1`
- 精确互斥写集: Cloud `src/main/java/com/bot/dhxy/service/CommonBoxService.java`；append-only C 固定日志 `docs/superpowers/plans/reports/2026-07-13-input-bundle-worker-c.md`。
- 前置: active blob 仍为 `195c1dbfef052ddaf87ff40c6c85cba862be91f6`；现有 `WindowFactKind.COMMON_BOX`、`WindowFact.CommonBoxFact`、`CloudGameClient` 与 ordered `InputBundle` 可用；领取时该 Service 无其它 writer。
- 验收: 完整保留 `696a12b0` pending-by-key 身份、探测/点击分离、阈值、过期/消费条件、返回值、顺序/delay/log；仅把原 ROI/template 探测换 typed fact、原点击换一个 ordered bundle；不得新增 owner/TTL/retry，仅保留基线既有状态；移除该类 desktop capture/template/input collaborator 编译依赖；不改 remote/schema/其它 Service；scoped diff/check 干净，父级 fresh Cloud package 归因。
- status: `READY`

### External D

- task id: `W-696-TEAM-RETURN-TYPED-ADAPT-1`
- 精确互斥写集: Cloud `src/main/java/com/bot/dhxy/service/TeamReturnService.java`；append-only D 固定日志 `docs/superpowers/plans/reports/2026-07-13-cloud-return-item-prescan-state-worker-d.md`。
- 前置: active blob 仍为 `286c5a85f01d010e883f8c4321ea1793776c932f`；现有 `TEAM_RETURN_BUTTON` / `TEAM_RETURN_LEADER_SIGNAL` typed facts、`CloudGameClient` 与 ordered `InputBundle` 可用；领取时该 Service 无其它 writer。
- 验收: 完整保留 `696a12b0` 队长/队员分支、按钮/leader signal 判定、退出矩阵、顺序/delay/fallback/state/log；仅把原窗口观察换对应 typed fact、原物理点击换等序 ordered bundle；移除该类 desktop capture/template/input/window-runtime collaborator 编译依赖；不改 remote/schema/其它 Service；scoped diff/check 干净，父级 fresh Cloud package 归因。
- status: `READY`

## Backup Queue

### Backup 1

- task id: `W-696-TASK-TRACKER-PANEL-RECT-TYPED-ADAPT-1`
- 精确互斥写集: Cloud `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java`；父级指定的单一 External 固定日志。
- 前置: active blob 仍为 `ad46ec861758737944dda82d784335a9405242f3`；现有 `TASK_TRACKER_PANEL_RECT` fact 可用；不得把绿链分割、fingerprint/cache、候选排序、分类与结果构造迁回 DHXY。
- 验收: 只替换 baseline panel-rect 定位调用点为 typed fact，保留所有 Cloud 算法与后续调用顺序；删除被替换点的 tracker/template/window-runtime 依赖，不碰尚未闭合的 capture/OCR 边界；scoped diff/check 干净并使 javac 错误集合单调减少。
- status: `READY`

### Backup 2

- task id: `W-696-CLIENT-IDENTITY-BINDING-TYPED-ADAPT-1`
- 精确互斥写集: Cloud `src/main/java/com/bot/dhxy/service/ClientIdentityService.java`；父级指定的单一 External 固定日志。
- 前置: active baseline exact；现有 `BINDING` fact 与 `WindowTitleIdentityParser` 可用，但当前 BINDING fact 仅返回 exact binding title，尚未证明等价覆盖 baseline 的 refresh + tracker-cache + locate-window fallback 顺序。
- 验收: 用户先选择是否允许以 exact binding title 取代后两级 fallback；若不允许，先另建保持完整优先级的 local typed fact。未获选择前零 Java。
- status: `NEEDS_USER_DECISION`

### Backup 3

- task id: `W-696-PLAYER-STATE-BAG-INCENSE-TYPED-ADAPT-1`
- 精确互斥写集: Cloud `src/main/java/com/bot/dhxy/service/PlayerStateService.java`；父级指定的单一 External 固定日志。
- 前置: `CloudBagUseIncensePort` 源码先经父级审查；现有 `BAG_USE_INCENSE` 可覆盖普通入口，但 baseline `BagService.MainBagSession` 开包独占段语义尚无等价 typed 表示。
- 验收: 用户先决定 open-main-bag 连续段是扩展 closed local macro，还是允许改为完整 BAG_USE_INCENSE 宏；未获选择前不得删除/改签 `MainBagSession` 路径。
- status: `NEEDS_USER_DECISION`

## Handoff

- 本报告仅为编排建议；未修改 Java/POM/主文档，未对任何 Worker 下发任务，未作 reviewer 判断。
- 最终发单、领取截止、源码审查与 `APPROVED/BLOCKED` 均由父级执行。

## Candidate Queue Repair #1

- preparedAt: `2026-07-14T13:32:46-04:00`
- scope: 本节取代 `Candidate Queue #1` 的 External B / D 候选；External A / C 保留为可直接发单项。本节只是编排建议，不作 reviewer 结论。
- compile-delta constraint: 不把现有 typed fact 尚未覆盖的 rounds capture/OCR、metrics、leader precheck、pathing、`PlayerStateService` 或本地 `BagService` 依赖伪装成已闭合边界。

### External A

- task id: `W-696-LEFT-TOP-STATUS-TYPED-ADAPT-1`
- 精确互斥写集: Cloud `src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java`；append-only A 固定日志 `docs/superpowers/plans/reports/2026-07-13-cloud-npc-click-service-worker-a.md`。
- 前置: active baseline blob 保持 `a46fde69e7d11bca315b75600fd737ef7f924912`；`LEFT_TOP_STATUS` typed fact、`CloudGameClient` 与 ordered input bundle 可用；父级发单前确认该 Service 无其它 writer。
- 验收: 保留 `696a12b0` 全方法、OPEN/CLOSED/UNKNOWN/CAPTURE_FAILED 分支、pending 状态、顺序/delay/log；仅替换原观察与点击调用点，不改 remote/schema/其它 Service，不宣称其它 Service 完成。
- status: `READY`

### External B

- task id: `W-696-TASK-TRACKER-PANEL-RECT-TYPED-ADAPT-1`
- 精确互斥写集: Cloud `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java`；append-only B 固定日志 `docs/superpowers/plans/reports/2026-07-13-cloud-team-return-service-worker-b.md`。
- 前置: active baseline blob 保持 `ad46ec861758737944dda82d784335a9405242f3`；现有 `TASK_TRACKER_PANEL_RECT` fact 提供 `WINDOW_CLIENT_PX` panel rectangle/anchor/score；父级发单前确认该 Service 无其它 writer。
- 验收: 只把 `696a12b0` 的 panel-rect 定位调用点替换为 typed fact；绿链分割、fingerprint/cache、候选排序、分类、结果构造及后续调用顺序全部继续留在 Cloud 且不改行为；不碰尚未闭合的 capture/OCR 边界，不宣称 `TaskTrackerPanelService` 整类完成，也不增加整类完成计数。
- status: `READY`

### External C

- task id: `W-696-COMMON-BOX-TYPED-ADAPT-1`
- 精确互斥写集: Cloud `src/main/java/com/bot/dhxy/service/CommonBoxService.java`；append-only C 固定日志 `docs/superpowers/plans/reports/2026-07-13-input-bundle-worker-c.md`。
- 前置: active baseline blob 保持 `195c1dbfef052ddaf87ff40c6c85cba862be91f6`；`COMMON_BOX` typed fact、`CloudGameClient` 与 ordered input bundle 可用；父级发单前确认该 Service 无其它 writer。
- 验收: 保留 `696a12b0` pending-by-key 身份、探测/点击分离、阈值、过期/消费条件、返回值、顺序/delay/log；仅替换原 ROI/template 探测与点击调用点，不新增 owner/TTL/retry，不改 remote/schema/其它 Service。
- status: `READY`

### External D

- task id: `W-696-TEAM-RETURN-MEMBER-BUTTON-TYPED-ADAPT-1`
- 精确互斥写集: Cloud `src/main/java/com/bot/dhxy/service/TeamReturnService.java`；append-only D 固定日志 `docs/superpowers/plans/reports/2026-07-13-cloud-return-item-prescan-state-worker-d.md`。
- 前置: active baseline blob 保持 `286c5a85f01d010e883f8c4321ea1793776c932f`；现有 `WindowFact.TeamReturnButtonFact` 的 PRESENT 分支已经严格携带 `SCREEN_ABSOLUTE_PX` `clickX`/`clickY`/finite `matchScore`；`TaskExecutionContext.getGameClient()` 已提供 `readWindowFact(...)` 与 `executeInputBundle(...)`；父级发单前确认该 Service 无其它 writer。
- 验收: 仅改 `clickReturnTeamIfPresent(TaskExecutionContext, String)`：第一次以独立稳定 slot 读取 `TEAM_RETURN_BUTTON`；PRESENT 时保持 `lastReturnButtonFoundAtByWindow` 时间戳与原日志；原样调用 `playerStateService.ensureSheYaoXiangActive(context)`；第二次用另一个独立稳定 slot 做 fresh `TEAM_RETURN_BUTTON` 读取；保持按钮消失日志/返回；对第二次 screen-absolute 点维持 X/Y 各自均匀 `[-3, 3]` 随机偏移；用第三个稳定 slot 发送原顺序 bundle `CLICK_LEFT(x,y,150ms)` 后 `SLEEP(500ms)`；保持 `lastReturnButtonClickedAtByWindow` 时间戳、返回值与全部现有日志参数。
- 验收边界: transport `STOPPED/UNKNOWN` 按现有 Cloud 调用约定退出/上抛且不自动 retry；非 PRESENT fact 走对应原 no-match/disappeared 路径，不伪造坐标。禁止触碰 leader wait/precheck/pathing、`PlayerStateService`/`BagService` 实现、其它 `TeamReturnService` 方法、remote/schema；本任务只闭合 member-button 这一条窄链，不宣称 `TeamReturnService` 整类完成。
- status: `READY`

### Superseded Candidates

- `W-696-AUTO-COMBAT-PANEL-TYPED-ADAPT-1`: 当前 `AUTO_COMBAT_PANEL` fact 不覆盖 baseline rounds capture/OCR 与 metrics，不能作为 full adapt `READY`；本轮不发单。
- `W-696-TEAM-RETURN-TYPED-ADAPT-1`: 当前 facts 不覆盖 leader precheck/pathing/`PlayerStateService`/`BagService`，不能作为 full adapt `READY`；由上述 member-button 窄任务取代。
- 最终发单、领取门、源码审查和任何 `APPROVED/BLOCKED` 结论仍由父级负责。

## Parent Queue Review #1 - 2026-07-14T13:36:00-04:00

**QUEUE APPROVED FOR DISPATCH。** 本结论只批准排班，不是对未来 Java 的源码批准：

- A `LEFT_TOP_STATUS`、B `TASK_TRACKER_PANEL_RECT`、C `COMMON_BOX`、D `TEAM_RETURN member-button`
  四个 Repair #1 候选的写集互斥，发布前 active blob 均经父级重算并与报告值/`696a12b0` 一致。
- 父级已把四份完整 direct-implementation brief 写到 A/B/C/D 固定日志真实 EOF。D 因历史重复锚点，第一次
  插入旧段已明确作废；`13:36` 的 TRUE EOF reissue 才是权威发单。
- 首版 AutoCombatPanel full adapt 与 TeamReturn full adapt 正式 superseded，不得被后续 heartbeat 当作待领任务。
- 本 helper 继续维护备用队列；任何本地/Cloud 落点歧义仍必须标 `NEEDS_USER_DECISION`。

## W-ABCD-NEXT-QUEUE-2 CLAIMED

- task: `W-ABCD-NEXT-QUEUE-2`
- claimedAt: `2026-07-14T13:44:51-04:00`
- role: Internal Worker CN（External A/B/C/D 下一任务编排 helper；不是 reviewer，不下 `APPROVED/BLOCKED`）
- unique write set: append-only `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-14-external-abcd-next-task-queue-helper.md`
- frozen current External write sets: Cloud `LeftTopStatusSwitchService.java`、`TaskTrackerPanelService.java`、`CommonBoxService.java`、`TeamReturnService.java`。
- frozen Navigation X2 cohort: Cloud `NavigationService.java` 及其未来 closed-local-macro caller/wire/schema/handler 写集；本轮候选不得触碰。
- constraints: 只基于最新 Cloud compile delta 与 `696a12b0` 准备四个互斥直接实现候选及至少两个备用；existing fact 未闭合整类时只能提窄边界；本地/Cloud 落点可选项标 `NEEDS_USER_DECISION`；不改 Java、A/B/C/D 日志或主文档。

## Candidate Queue #2 - External B Immediate Replacement

- preparedAt: `2026-07-14T13:55:28-04:00`
- purpose: B 的 `W-696-TASK-TRACKER-PANEL-RECT-TYPED-ADAPT-1` 已由父级 supersede 且 Java 零改动；本节先提供一项不依赖未落 fact、可立即发给原 B 的直接实现单，其余三路候选与备用队列随后另行追加。
- exclusion check: 与当前 A `LeftTopStatusSwitchService.java`、C `CommonBoxService.java`、D `TeamReturnService.java`、已 supersede 的 `TaskTrackerPanelService.java`、Navigation X2 的 `NavigationService.java` 及未来 caller/wire/schema/handler 写集均无交集；也不触碰 Internal CL 的 `pom.xml`。

### External B - Immediate READY

- task id: `W-696-TMS-CLOUD-CONTEXT-IDENTITY-ADAPT-1`
- exact file write set when parent dispatches:
  - Cloud `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\TaskMaintenanceService.java`
  - append-only B 固定日志 `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-team-return-service-worker-b.md`
- frozen outside write set: 其它 Java、remote/schema、POM、A/C/D 日志、主文档全部冻结。
- baseline/current evidence: `696a12b0:src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java` Git blob 为 `e93cfd01d9c282f98881a6311b8bb806bbc3e359`；排班核验时 active Cloud `git hash-object` 同为 `e93cfd01d9c282f98881a6311b8bb806bbc3e359`，即尚未发生 Cloud 适配漂移。
- existing dependency reachability: Cloud 已存在并可注入 `com.bot.dhxy.runner.context.TaskExecutionContextHolder`；其 `current()` 返回现成 `TaskExecutionContext`。后者已真实提供 `hasWindow()`、`getWindowId()` 与 `getPlayerIdentityEpoch()`，本任务不需要任何新 `WindowFact`、remote contract、schema 或 local macro。
- exact call points:
  - imports/constructor field: 删除本地 `WindowRuntimeContext`、`WindowTaskContextHolder` 两个 import，把 `windowTaskContextHolder` 字段类型换成现有 Cloud `TaskExecutionContextHolder`；保留 Spring/Lombok 构造注入形态。
  - `currentWindowKey(TaskExecutionContext)`（baseline 约 `987-995`）：继续保持“显式 `context` 且 `hasWindow()` 优先”；仅把 fallback 的 `rawCurrent()->WindowRuntimeContext.getWindowId()` 换为 `taskExecutionContextHolder.current()->TaskExecutionContext.getWindowId()`，空/blank 仍回落 `DEFAULT_WINDOW_KEY`。
  - `summonSkillState(String)` 唯一调用点（baseline 约 `650`）及 helper（约 `997-1018`）：把调用方已有的同一个 `TaskExecutionContext` 传入状态 helper，使 window key 与 identity epoch 来自同一 exact context；不得从另一个隐式 snapshot 拼接 identity。
  - `currentPlayerIdentityEpoch()`（baseline 约 `1021-1025`）：改为显式 context 优先、Cloud holder current fallback、无 context 仍为 `0L`；只读取 `TaskExecutionContext.getPlayerIdentityEpoch()`。
- acceptance:
  - 完整保留 `696a12b0` 的全部 public/private 方法、maintenance 判断、调用顺序、delay、cooldown、cache invalidation、fallback、state、返回值与日志；仅替换上述本地 runtime-context 读取边界。
  - `currentWindowKey` 的 explicit-context precedence、blank 过滤和 `DEFAULT_WINDOW_KEY` 不变；identity epoch 漂移时清理 `lastSummonSkillCleanAtByWindow`、`lastSummonSkillNotDueLogAtByWindow`、`summonSkillUnknownRetryAfterByWindow` 并重置 `SummonSkillWindowState` 的顺序不变。
  - 移除该类对 `com.bot.dhxy.window.runtime.WindowRuntimeContext` / `WindowTaskContextHolder` 的所有编译依赖；不得新增 fact、owner、permit、session、ledger、TTL、retry 或 wrapper 链。
  - 仅交 scoped diff/check 与调用点证据；按当前父级并发构建规则不由 B 启动 Maven/test/runtime，统一构建由父级负责。
- status: `READY`
- queue note: 这是排班 helper 的可实施性建议，不是源码 reviewer 的 `APPROVED/BLOCKED` 结论；最终发单、领取门与实现审查仍由父级完成。

## Candidate Queue #2 - Candidate Repair #1

- preparedAt: `2026-07-14T14:02:59-04:00`
- scope: 本节撤销上一节 External B immediate candidate 的 `READY` 标记；这是排班前置核验修正，不是对任何 Worker 源码的 reviewer 结论。

### Withdrawn Candidate

- task id: `W-696-TMS-CLOUD-CONTEXT-IDENTITY-ADAPT-1`
- prior status: `READY`（撤销）
- corrected status: `NO_READY`
- producer evidence: Cloud 全树 `rg "callWith(" src/main/java` 只命中 `TaskExecutionContextHolder.callWith(...)` 自身声明与无关的 `InputActionScope.callWith(...)`；没有任何 assembly、endpoint、dispatcher、Task 或 Service 调用 `TaskExecutionContextHolder.callWith(...)`。因此 `TaskExecutionContextHolder.current()` 当前没有实际 producer。
- contract evidence: baseline/current exact `TaskMaintenanceService.java` 的 `currentWindowKey(TaskExecutionContext)` 明确允许显式 `context` 不存在，并在约 `987-995` 通过本地 `WindowTaskContextHolder.rawCurrent()` 取当前 window；`summonSkillState(String)` 在约 `997-1018` 又通过同一本地 runtime 取 identity epoch。直接换成无 producer 的 Cloud holder 会让合法的 `context == null` 路径静默落到 `DEFAULT_WINDOW_KEY` / epoch `0L`，丢失原窗口隔离与 identity-drift cache invalidation，不能作为等价迁移发单。
- related false-dependency evidence: 当前 `CloudBagUseIncensePort` 与 `CloudUiCleanerPort` 也都从同一个无 producer 的 `TaskExecutionContextHolder.current()` 取 context；它们的类型存在不能证明调用时 context 可达，不能拿来补救本候选。
- dispatch instruction: 父级不要把上一节任务发给 B；active `TaskMaintenanceService.java` 保持 blob `e93cfd01d9c282f98881a6311b8bb806bbc3e359` 不动，等待真实 caller/context producer 前置另行闭合。

### External B Replacement Search Result

- frozen exclusions rechecked: A `LeftTopStatusSwitchService.java`、C `CommonBoxService.java`、D `TeamReturnService.java`、`TaskTrackerPanelService.java`、Navigation X2 的 `NavigationService.java` 与未来 caller/wire/schema/handler、Internal CL `pom.xml` 均未纳入候选。
- remaining compile-dependency scan: 排除上述写集后，当前失败 Service 的缺失类型只落在以下类别：
  - zero-producer context/runtime: `WindowTaskContextHolder`、`WindowRuntimeContext`；
  - desktop authority: `GameClientTracker`、`InputProvider`、`CoordinateHelper`、`WindowScopedTempPath`、window binding refresh/ready bus；
  - capture/OCR authority: `TextRecognizer`、`GameTextLineOcrService`、`MiniMapCoordinateReader`、`OcrWindowScanService`、`LocationVisionService`；
  - scheduling/local-Service authority: `TaskTurnCoordinator`、永久本地 `BagService` / `GiveItemService`。
- typed reachability check: 剩余候选中，`AutoCombatService`、`BattleRadarService` 与 `PlayerStateService` 的 window-scoped state 都依赖隐式 current-window producer；其公开 no-context 入口不能用 default/global state 等价替代。`ClientIdentityService` 的 title fallback 需要 refresh -> tracker cache -> locate-window 完整 producer 链。`DialogService` / `PlayerStateService` 的永久本地 Service 调用也没有可由单文件、现有显式 caller context 闭合的 typed boundary。
- pure CPU check: 最新 compile delta 中 `SheyaoxiangDigitTemplateReader` 已消失；当前剩余缺失 import 没有另一个可 byte-exact 搬入且不持有 desktop/window/capture/OCR/input/scheduling authority 的纯 CPU helper。
- TaskTracker note: `TASK_TRACKER_PANEL_RECT` 仍为 `DEFERRED/contract prerequisite`；现有 fact 实例对 `com.bot.dhxy.service.TaskTrackerPanelService` 不可达，且缺 exact binding offset 与同帧 capture artifact，禁止重新标 `READY`。
- replacement status: `NO_READY`
- required next prerequisite: 先由父级选择并落地一个真实、显式、caller 可达的 context/fact/local-macro producer，或解除一个当前互斥写集；在此前提出现前，本 helper 不伪造 External B 的直接实现单。
- scope confirmation: 本节只追加排班报告；未修改 Java/POM、A/B/C/D 固定日志或主文档，未运行 build/test/runtime，也未作 `APPROVED/BLOCKED` 判断。

## Candidate Queue #2 - TaskTracker A/B/D Cohort Feasibility #1

- preparedAt: `2026-07-14T14:19:55-04:00`
- requested cohort: A 只改 Cloud `TaskTrackerPanelService` 原调用点；B 只做 Cloud closed command/result/facade/wire；D 只做 DHXY mirror/codec/handler/exact-window mechanics。
- status: `NO_READY`

### 唯一最小前置

先落地一个**真实 caller-reachable、显式携带现有已授权 `TaskExecutionContext` 的 Cloud Task 调用点**，再把该 exact context 传入 `TaskTrackerPanelService` 的 live 调用图。当前 baseline live public API 与 `resolveTrackerPanelRect(String)` 都没有 context；Cloud 全树没有 `TaskExecutionContextHolder.callWith(...)` producer；现有 `DecisionEngine.trackerPanelRead(JsonNode)` 只调用旧 CPU reader request 路径，也不处于 task-run context。故 A 在单一 Service 文件内既拿不到 `context.getGameClient()`，也不能用空 holder/default window/epoch 伪造调用权威。父级需先迁入/指定一个真正拥有 `TaskExecutionContext` 的 Task caller，并把该 caller 文件纳入前置写集；除此之外不需要新 owner、TTL、retry 或 dormant artifact materialize 流程。

### 已核实、待前置闭合后可直接发单的三路文件表

- A（deferred）: Cloud `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java`。仅把 `resolveTrackerPanelRect` 原位置改为显式 context 调用 B facade；其后 title/template、绿链分割、fingerprint/cache、候选排序、分类、结果构造全部留 Cloud。不得使用 holder fallback。
- B（deferred）: Cloud new `TaskTrackerPanelCaptureMacroCommand.java`、`TaskTrackerPanelCaptureMacroResult.java`、`CloudTaskTrackerPanelCapturePort.java`；modify `LocalMacroKind.java`、`LocalMacroCommand.java`、`LocalMacroRequest.java`、`LocalMacroOutcome.java`、`RemoteCommandEnvelope.java`、`RemoteCommandOutcomeEnvelope.java`、`RemoteProtocolDigests.java`。合同新增 closed kind `TASK_TRACKER_PANEL_CAPTURE`；command 仅携 `source`；EXECUTED typed result 为 closed `state` + PNG `imageBytes` + `imageSha256` + `width/height` + screen-absolute `absoluteLeft/absoluteTop`，非 CAPTURED 状态不得携图像/原点；外层只接受既有 `EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN`，零自动 retry。
- D（deferred）: DHXY new `RemoteTaskTrackerPanelCaptureMacroCommandPayload.java`、`RemoteTaskTrackerPanelCaptureMacroResultPayload.java`、`service/tasktracker/TaskTrackerPanelCaptureLocalMechanics.java`；modify `RemoteLocalMacroKind.java`、`RemoteLocalMacroCommandPayload.java`、`RemoteLocalMacroResultPayload.java`、`RemoteOperationPayloadCodec.java`、`RemoteProtocolDigests.java`、`LocalRemoteGameCommandHandler.java`。mechanics 必须在 exact binding/context 下依次执行 baseline narrow anchor search -> expanded full-window search -> 必要时一个 ordered `DRAG_AND_DROP + SLEEP(500)` -> 按 baseline anchor/offset 在同一次 mechanics 中 capture panel PNG，并返回 screen-absolute origin；handler 在 remote input queue 外调用该 self-queued mechanics，保持 stop/runRevision/terminal 分流。

### 调度与验收边界

- B/D 的 generic LocalMacro 文件与未来 Navigation X2 cohort 有文件交集；当前没有活跃 X2 writer，父级串行解除冻结、先完成本 cohort 源码/构建门、再按扩展后的 closed switch 重发 X2，足以解决文件排程冲突，但**不能**替代上述 context-bearing caller 前置。
- 当前 A LeftTop 与 D TeamReturn 已释放；C CommonBox R1 写集不与上述 deferred 文件表相交。
- 不激活既有 `TASK_TRACKER_READ` / artifact owner / ledger / materialize；不把 title/绿链/fingerprint/cache/排序/分类迁回 DHXY；不新增 owner/TTL/retry。
- 本节只给排班可行性与最小前置，不改 Java/A-D 日志/主文档，不作 reviewer 结论；父级在真实 caller 前置落地前不得把三路标成 `READY`。

## Candidate Queue #3 - Post Whole-Adapt Backup Schedule

- preparedAt: `2026-07-14T14:30:17-04:00`
- current-wave authority: 父级已经直接发布四个互斥整类任务：A `BattleRadarService.java` whole adapt、B `NpcClickService.java` whole adapt、C `PlayerStateService.java` whole adapt、D `DialogService.java` whole adapt。四个 Worker 的当前唯一 Java 写集均为各自这一个 Cloud Service 文件；本 helper 停止为本轮另选或替换任务。
- release rule: 任一整类任务出现返修材料时，原 Worker 继续修自己的同一文件，不把任务转派给其他 Worker；只有父级确认该 Worker 已交付或明确释放写集后，才从下列备用队列发下一单。本节是排班建议，不作源码 reviewer 的 `APPROVED/BLOCKED` 裁决。

### Backup 1 - Navigation X2 Closed Local-Macro Cohort

- task family: `W-696-NAV-X2-CLOSED-MACRO-COHORT-1`
- status: `DEFERRED_CONTEXT_PREREQUISITE`
- trigger: A/B/C/D 当前 whole-adapt 写集全部稳定后，由父级先指定一个真实 caller-reachable、显式携带已授权 `TaskExecutionContext` 的 Navigation 调用链；不得继续依赖当前零 producer 的 `TaskExecutionContextHolder.current()`。此前置闭合后，下面四路文件表可直接串成一个实质代码 cohort。
- existing contract: 复用已经真实落盘的 `LOCAL_MACRO`、`CloudGameClient.executeLocalMacro(...)`、DHXY `LocalRemoteGameCommandHandler`、single input queue 和 `EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN` terminal；不重开 `UI_CLEAN`，而是新增一个 closed Navigation route-panel sequence kind，把 baseline 外层 direct-input sequence、X2 close、成功后的 mouse-away 保持在同一个本地独占段。
- caller evidence: Cloud `NavigationService` 的三个已知 P1 点分别位于 `prepareWorldMapSearchResultsDirect(...)` 内 stale-panel close、route click 后 `closeMapSearchInputAfterRouteClick(...)`、confirmed-arrival 后 `closeMapSearchInputAfterRouteDialog(...)`。它们当前在/由 `submitExclusiveAndWait(...)` direct callback 调用，而 DHXY `UI_CLEAN/CLOSE_MAP_SEARCH_INPUT_BY_X2` 又会取得 remote exclusive queue，形成 queue-in-queue；这正是本 cohort 的唯一修复范围。
- terminal matrix: 外层只接受既有 `EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN`。`EXECUTED` 必须带 operation-matching typed result；prepare 分支映射原 `prepared` boolean，route-click 分支保持“物理 click 已发出即原路径成功，X2 close 只作原日志/鼠标移开条件”，dialog-close 分支映射原 `closed` boolean；`NOT_EXECUTED` 映射各原 boolean false/no-op；`STOPPED/UNKNOWN` 终止，不自动 retry。
- A lane exact write set after release:
  - Cloud `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NavigationService.java`
  - 只替换上述三个 X2 P1 原调用点；完整保留 `696a12b0` 的 route 判断、attempt 次数、click 顺序、250/500ms delay、fallback、movement intent、返回值和日志。
- B lane exact write set after release:
  - Cloud new `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/NavigationRoutePanelMacroCommand.java`
  - Cloud new `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/NavigationRoutePanelMacroResult.java`
  - Cloud new `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudNavigationRoutePanelPort.java`
  - Cloud modify `LocalMacroKind.java`, `LocalMacroCommand.java`, `LocalMacroRequest.java`, `LocalMacroOutcome.java`, `RemoteCommandEnvelope.java`, `RemoteCommandOutcomeEnvelope.java`, `RemoteProtocolDigests.java`
- C lane exact write set after release:
  - DHXY new `src/main/java/com/bot/dhxy/cloud/remote/RemoteNavigationRoutePanelMacroCommandPayload.java`
  - DHXY new `src/main/java/com/bot/dhxy/cloud/remote/RemoteNavigationRoutePanelMacroResultPayload.java`
  - DHXY modify `RemoteLocalMacroKind.java`, `RemoteLocalMacroCommandPayload.java`, `RemoteLocalMacroResultPayload.java`, `RemoteOperationPayloadCodec.java`, `RemoteProtocolDigests.java`
- D lane exact write set after release:
  - DHXY modify `src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java`
  - DHXY modify `src/main/java/com/bot/dhxy/service/NavigationService.java`
  - handler 在 remote input queue 外调用 local `NavigationService` 的一个真实 closed sequence entry；该 entry 自己拥有原 direct-input sequence，不复制/缩短中间 mechanics，也不嵌套第二次 queue。
- acceptance: 四路文件表彼此互斥；command/result closed operation 与 nullability 对称；nested canonical digest 和 flat terminal payload 对称；三个 P1 点均不再从 input-worker callback 发起第二次 remote macro；不得新增 owner/permit/session/ledger/TTL/retry/dormant artifact。Cloud 与 DHXY 的 fresh build 由父级在 writers 稳定后统一执行。

### Backup 2 - TaskTracker Context/Capture Prerequisite

- task family: `W-696-TASKTRACKER-CONTEXT-CAPTURE-PREREQUISITE-1`
- status: `NO_READY`
- unique minimum prerequisite: 先迁入或指定一个真实 Task caller，使它持有已授权 `TaskExecutionContext`，并把该 exact context 显式传入 `TaskTrackerPanelService` live public call graph。现有 `DecisionEngine.trackerPanelRead(JsonNode)` 只是旧 request/CPU reader 路径；Cloud 全树没有 `TaskExecutionContextHolder.callWith(...)` producer，因此 holder/default window/epoch 不得作为前置替代。
- capture contract required after context exists: DHXY exact-window mechanics 必须在一次调用中执行 baseline narrow anchor search -> expanded full-window search -> 必要时一个 ordered drag + 500ms -> 同一帧 panel capture；typed terminal 返回 PNG bytes、SHA-256、width/height、screen-absolute origin 与 closed state。Cloud 继续拥有 title/绿链分割/fingerprint/cache/候选排序/分类/结果构造。
- dispatch note: context producer 未落地前，不重新发 A/B/D TaskTracker cohort，不激活现有 dormant `TASK_TRACKER_READ`/artifact owner/ledger/materialize，不把 `TASK_TRACKER_PANEL_RECT` 的类型存在误当 caller 可达。

### Backup 3 - ClientIdentity Placement Decision

- task family: `W-696-CLIENT-IDENTITY-PLACEMENT-DECISION-1`
- status: `NEEDS_USER_DECISION`
- ambiguity: baseline `ClientIdentityService.resolveCurrentWindowTitle()` 的行为是 exact bound runtime title -> tracker cached/full title -> `locateWindow()` fallback，并在 fallback 中保留 binding refresh。这里既可把整段窗口机械解析留 DHXY 后回传 typed identity fact，也可由 Cloud 保留 identity 解析/同步、DHXY 只回传严格保持该 fallback 顺序的 resolved-title fact；两种落点都会影响窗口绑定权威归属，不能由 helper 代选。
- option L (local mechanics): DHXY 保留 bound-title/cache/locate/refresh 整段并返回 `{state,title,windowId,nativeHandle,playerIdentityEpoch}`；Cloud `ClientIdentityService` 只消费 typed terminal 并同步 `PlayerCharacter`。优点是 desktop authority 不出本地，代价是 fallback 顺序整体成为一个本地边界。
- option C (cloud policy): DHXY 分别提供 exact binding title、tracker cache title、locate/refresh typed outcome，Cloud `ClientIdentityService` 原顺序逐项选择。优点是 fallback policy 留 Cloud，代价是合同与 round trip 更多，且必须证明三种 fact 都有真实 producer。
- decision gate: 父级向用户展示两案并取得明确选择前，不给 A/B/C/D 发 ClientIdentity Java 单；无论选择哪案，都必须保留 `696a12b0` 的优先级、null/blank 处理、refresh 与最终未解析 terminal，不新增 retry/TTL/owner。

### Backup Order

1. 当前 A/B/C/D whole-adapt 有返修：原 Worker 原文件返修，备用队列不抢占。
2. 当前写集释放且 Navigation 的显式 context caller 已闭合：按 A caller / B Cloud contract / C DHXY wire / D local handler-mechanics 四路发 `W-696-NAV-X2-CLOSED-MACRO-COHORT-1`。
3. TaskTracker 只有在真实 context caller 前置落地后进入三路 capture cohort；此前保持 `NO_READY`。
4. ClientIdentity 等用户选择 option L/C；此前保持 `NEEDS_USER_DECISION`。

- scope confirmation: 本节只追加本 queue 报告；未修改 Java/POM、A/B/C/D 固定日志或主文档，未运行 build/test/runtime，也未作 reviewer 结论。

## Candidate Queue #4 - Release-Triggered Queue After Four Whole-Service Claims

- preparedAt: `2026-07-14T14:43:34-04:00`
- current-wave freeze: A `BattleRadarService.java`、B `NpcClickService.java`、C `PlayerStateService.java`、D `DialogService.java` 四个 whole-Service 唯一 Java 写集均已真实领取；它们交付或返修前，本 helper 不为其改派、不触碰对应文件。
- compile delta: 父级最近一次 fresh Cloud compile 的 7 个失败 Service 中，上述四个正由当前波处理；未被当前波占用的失败 Service 只剩 `ClientIdentityService`、`NavigationService`、`TaskTrackerPanelService`。
- release rule: 任一当前 Worker 获父级 `SOURCE APPROVED` 并明确释放写集后，优先发下面唯一 `READY` 单；其余候选必须满足各自真实前置后才可升级，不能为了填满四路伪造 producer、holder 或 fact caller。

### Immediate READY - TeamReturn Leader Live Fact Boundary

- task id: `W-696-TEAMRETURN-LEADER-LIVE-FACT-ADAPT-1`
- status: `READY`
- exact Java write set:
  - Cloud `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\TeamReturnService.java`
  - 被父级选中的已释放 External Worker 只向自己的固定日志 append `CLAIMED/Implementation`；其它 Java、remote/schema、POM、A/B/C/D 其余日志与主文档冻结。
- baseline/current evidence:
  - `696a12b0:src/main/java/com/bot/dhxy/service/TeamReturnService.java` Git blob 为 `286c5a85f01d010e883f8c4321ea1793776c932f`。
  - active Cloud 文件当前 hash 为 `24108d1e25d4effc3ccd9d09b1c73a92ddd672f2`，并已包含父级 SOURCE APPROVED 的 member-button typed-fact 波；本任务只在该在途文件上继续最小 Phase 4 适配，不覆盖既有改动，也不宣称整类完成。
- real producer evidence: DHXY `LocalRemoteGameCommandHandler.handleWindowFact(...)` 的 `TEAM_RETURN_LEADER_SIGNAL` 分支已真实通过 exact `BindingAccess.context()` 调用 `TeamReturnLeaderSignalLocalObservationMechanics.observe(access.binding())`；closed fact 为 `PRESENT/ABSENT/CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/MECHANICS_FAILED`，`PRESENT` 携 screen-absolute `signalX/signalY/matchScore`。这不是只存在类型而没有 producer 的 dormant fact。
- caller reachability:
  - `clickReturnTeamIfPresent(TaskExecutionContext,String)` 已是现有 context-bearing public 入口；其 first read 为非 `PRESENT` 时真实调用 `logReturnButtonNoMatch(context,source)`，后者当前仍在约 `320` 行调用本地 `isReturnTeamSignalPresent()`，因此至少一条 public caller -> typed producer 真链可立即闭合。
  - `waitForMembersReturnIfNeeded(TaskExecutionContext,String)` 自身也持有 exact context；其初次检测与 baseline poll loop 的复检可共用同一 typed boundary。Cloud 当前没有该 wait public API 的外部 Task caller，故本任务只计单文件 boundary 适配，不计 whole-Service 或 Task caller 完成。
- exact implementation boundary:
  - 只把 `waitForMembersReturnIfNeeded` 的初检、每轮复检，以及 `logReturnButtonNoMatch` 的 leader marker diagnostic 读取替换为 `context.getGameClient().readWindowFact(..., WindowFactKind.TEAM_RETURN_LEADER_SIGNAL, 120_000L)`。
  - phase/action address 使用稳定 closed token，例如 `team-return-leader-signal/wait-initial`、`team-return-leader-signal/wait-poll`、`team-return-leader-signal/member-no-match-diagnostic`；同一个 `wait-poll` slot 的 terminal final-consumption 后由现有 retained state 自动推进 occurrence，不用 poll index、TTL、自动 retry 或新 owner。
  - 保留 baseline public `isReturnTeamSignalPresent()`、precheck public/private 图和其它 desktop 代码不动；本单不删除 API、不顺手重构整类。
- terminal matrix:
  - `OBSERVED + PRESENT` -> `true`；`OBSERVED + ABSENT/CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/MECHANICS_FAILED` -> `false`，保持 baseline `findImageInRegion(...) == null` 的 no-signal 语义。
  - `NOT_EXECUTED` -> `false`，与已 SOURCE APPROVED 的 member-button读取边界保持一致；先保留 stop checkpoint。
  - `STOPPED` 先走原 stop checkpoint；`UNKNOWN`、fact kind/type 不匹配或其它 unresolved terminal -> `TaskFatalException`，禁止静默压成 `ABSENT`，禁止自动重发。
- acceptance:
  - `696a12b0` 的初检 -> timeout/deadline -> stop checkpoint -> `TaskSleep.sleep(pollMs)` -> 复检 -> disappeared/timeout log 与返回值顺序逐项不变；不得新增/删除一次检测或 sleep。
  - `clickReturnTeamIfPresent` 的先检测 -> ensure 摄妖香 -> 再检测 -> click bundle 路径和 found/click timestamps 不变；本任务仅改 no-match diagnostic 中的 leader marker 来源。
  - scoped diff 只含该 Cloud Service 与选中 Worker 的 append-only 日志；不新增 owner/permit/session/ledger/TTL/retry，不运行 Worker 自己的 Maven/test/runtime，父级统一源码审查与构建。

### Conditional Follow-Up - AutoCombat Caller Threading

- task id: `W-696-AUTOCOMBAT-WHOLE-ADAPT-CALLER-FOLLOWUP-1`
- status: `CONDITIONAL_NOT_READY`
- exact future Java write set: Cloud `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\AutoCombatService.java` only; baseline blob `b1c2d48e89ed6b2ca90b1639df841dd7a97d691a`。
- trigger: A 的 `BattleRadarService` 与 C 的 `PlayerStateService` 都获得父级 `SOURCE APPROVED`，且交付 API 明确接受调用方已有的 exact `TaskExecutionContext` 或不需要新增隐式 holder。只有父级核对实际签名后，本候选才可升级为 `READY`。
- intended call points: `handleTaskCombatTick`、`handleWindowCombatGuardTick`、`probeWindowCombatStateReadOnly`、`consumeExitAndRecover`、`consumePendingLeaderPostCombatRecovery` 和 pending first-aid 路径中现有 BattleRadar/PlayerState 调用；只线程化同一个 caller context，保持 radar 顺序、enter/exit signal consumption、recovery policy、delay、fallback 与 state update。
- rejection gate: 若 A/C 交付仍依赖无 producer holder、default window/epoch，或要求修改 current A/C Service 文件、remote/schema/owner，则保持 `CONDITIONAL_NOT_READY`，不得先发单。

### Remaining Compile-Failure Queue

#### NavigationService

- task family: `W-696-NAV-X2-CLOSED-MACRO-COHORT-1`
- exact primary Cloud Service: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NavigationService.java`；baseline blob `7857018df5c728f508cb58f1bb738081eec8356d`。
- status: `NO_READY`
- evidence: public `navigateToNPC(NavigationRequest)` / `navigateInCurrentMap(NavigationRequest)` 仍无 explicit `TaskExecutionContext`，Cloud `TaskExecutionContextHolder.callWith(...)` 仍无真实 producer；三个 X2 点又位于/进入现有 exclusive input callback。现有 UI_CLEAN 类型存在不能提供授权 context，也不能消除 queue-in-queue。
- unique minimum prerequisite: 先迁入或指定一个真实 caller-reachable Task，使 exact authorized `TaskExecutionContext` 显式进入 Navigation live public graph；之后才能按 Queue #3 已列四路文件表发 closed route-panel macro cohort。此前不以 default context、holder 或拆散 X2 物理序列伪装 `READY`。

#### TaskTrackerPanelService

- task family: `W-696-TASKTRACKER-CONTEXT-CAPTURE-PREREQUISITE-1`
- exact primary Cloud Service: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\TaskTrackerPanelService.java`；baseline blob `ad46ec861758737944dda82d784335a9405242f3`。
- status: `NO_READY`
- evidence: `TASK_TRACKER_PANEL_RECT` 有真实 DHXY producer，但 live Service 没有 authorized context caller，fact 又不携 narrow -> expanded -> necessary drag 后同帧 panel PNG artifact；只给 rect 不能供 title/绿链/fingerprint/cache/排序/分类算法继续执行。
- unique minimum prerequisite: 一个 explicit context-bearing live caller，加一个返回 PNG bytes/SHA-256/尺寸/screen-absolute origin/typed terminal 的 exact-window closed capture mechanics；两者缺一不可。不得激活 dormant artifact owner/ledger/materialize，也不得把算法搬回 DHXY。

#### ClientIdentityService

- task family: `W-696-CLIENT-IDENTITY-PLACEMENT-DECISION-1`
- exact primary Cloud Service: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\ClientIdentityService.java`；baseline blob `06db63306f163c4d51c1d7208e155f96b6fe8877`。
- status: `NEEDS_USER_DECISION`
- decision unchanged: baseline exact bound title -> tracker cached/full title -> `locateWindow()` + binding refresh 可整体作为 DHXY local mechanics 回传一个 typed identity terminal，也可拆成多个真实 typed facts 让 Cloud 保留选择顺序。两案改变窗口绑定 authority 落点，必须由用户选；父级取得选择前不向任何 Worker 发布 Java 单。

### Queue #4 Dispatch Order

1. 任一 A/B/C/D 当前 whole-adapt 被父级要求返修：原 Worker 继续自己的原 Service 文件，本 queue 不抢占。
2. 任一 Worker `SOURCE APPROVED` 并释放：立即可发 `W-696-TEAMRETURN-LEADER-LIVE-FACT-ADAPT-1`，因为其唯一 Java 文件与当前四个 whole-adapt、Navigation X2、TaskTracker 和 ClientIdentity 全部互斥，且 typed producer 已真实可达。
3. A 与 C 都 `SOURCE APPROVED` 后，父级先核对其真实 API；只有 explicit context 条件满足，才把 AutoCombat follow-up 升为 `READY`。
4. Navigation、TaskTracker 继续等待各自唯一最小前置；ClientIdentity 继续等待用户 L/C 落点选择。没有前置就保持 `NO_READY/NEEDS_USER_DECISION`，不为占满 Worker 伪发任务。

- scope confirmation: 本节仅 append 本 queue 报告；未修改任何 Java/POM、A/B/C/D 固定日志或主文档，未运行 Maven/test/runtime，未执行 Git mutation，也未作 reviewer 的 `APPROVED/BLOCKED` 裁决。

## Candidate Queue #5 - BattleRadar Fact Wave Successors

- preparedAt: `2026-07-14T15:08:07-04:00`
- role boundary: 本节只是 Next-Task Queue Helper 的排班建议，不是源码审查，不产生 `APPROVED/BLOCKED` 结论；最终发单、源码判断与构建归父级。
- evidence read: 已读取 whole-service 计划、migration matrix、两仓 `git status`，并核到四份固定日志物理 EOF：A `5282` 行、B `7250` 行、C `4355` 行、D `4772` 行。
- current freeze:
  - A `W-696-BATTLE-RADAR-WHOLE-ADAPT-1-R1`: Cloud `src/main/java/com/bot/dhxy/service/BattleRadarService.java`。
  - B `W-696-NPC-CLICK-WHOLE-ADAPT-1`: Cloud `src/main/java/com/bot/dhxy/service/NpcClickService.java`。
  - C `W-696-BATTLE-RADAR-CLOUD-FACT-1`: Cloud `WindowFactKind.java`、`WindowFact.java`、`WindowFactOutcome.java`、`RemoteCommandOutcomeEnvelope.java`。
  - D `W-696-BATTLE-RADAR-DHXY-FACT-1`: DHXY `RemoteWindowFactKind.java`、三个 `RemoteBattleRadar*Fact.java`、`BattleRadarLocalObservationMechanics.java`、`LocalRemoteGameCommandHandler.java`。
- scheduling rule: 当前任务若收到返修，原 Worker 先继续原写集。只有父级明确释放该 Worker 后才发下面后继单；三个 create-new 目标已核实当前不存在。`READY_PREREQUISITE_AFTER_RELEASE` 只表示可以直接写实质 prerequisite 代码，不表示完整 Service 已闭合或可增加计数。

### External A Successor - TeamReturn Leader Live Fact Boundary

- task id: `W-696-TEAMRETURN-LEADER-LIVE-FACT-ADAPT-1`
- status: `READY_AFTER_A_RELEASE`
- exact Java write set:
  - Cloud `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\TeamReturnService.java`
- baseline/current anchors:
  - `696a12b0` blob: `286c5a85f01d010e883f8c4321ea1793776c932f`。
  - current Cloud hash: `24108d1e25d4effc3ccd9d09b1c73a92ddd672f2`；必须在其上保留已经落下的 member-button typed chain，不得用 baseline 整文件覆盖。
- existing producer and caller reachability:
  - DHXY handler 已有真实 `TEAM_RETURN_LEADER_SIGNAL` case，以 exact `BindingAccess.context()` 调 `TeamReturnLeaderSignalLocalObservationMechanics.observe(binding)`；closed state 为 `PRESENT/ABSENT/CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/MECHANICS_FAILED`，`PRESENT` 带 screen-absolute `signalX/signalY/matchScore`。
  - Cloud `waitForMembersReturnIfNeeded(TaskExecutionContext,String)` 的初检和 poll 复检都持有显式 context；`clickReturnTeamIfPresent(TaskExecutionContext,String)` 的 no-match diagnostic 也持有同一 context。不存在 holder/default-window 前置。
- direct implementation:
  - 只把 `waitForMembersReturnIfNeeded` 的初检、每轮复检和 `logReturnButtonNoMatch` 的 leader-marker diagnostic 读改为 `context.getGameClient().readWindowFact(... TEAM_RETURN_LEADER_SIGNAL ...)`。
  - 保留 public `isReturnTeamSignalPresent()`、leader snapshot precheck、pathing/PlayerState/Bag 等其余方法原样；本单不宣称整类完成。
- terminal and acceptance:
  - `OBSERVED+PRESENT -> true`；`OBSERVED+ABSENT/CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/MECHANICS_FAILED -> false`；`NOT_EXECUTED -> false`；`STOPPED` 走原 checkpoint；`UNKNOWN`、类型/kind 不匹配和中断不得压成 absent。
  - 初检 -> deadline -> checkpoint -> `TaskSleep.sleep(pollMs)` -> 复检 -> disappeared/timeout 的次数、顺序、delay、返回值和日志不变；不新增 owner/session/ledger/TTL/retry。

#### External A Backup - AutoCombatPanel Consumer

- task id: `W-696-AUTO-COMBAT-PANEL-CONTEXT-FACT-ADAPT-1`
- exact future Java write set: Cloud `AutoCombatPanelService.java` + `AutoCombatService.java`。
- status: `NO_READY`
- evidence: `AUTO_COMBAT_PANEL` 有真实 DHXY producer，但 fact 只含 panel/green-marker 坐标和 source，不含 remaining-rounds capture/OCR、metrics 更新所需结果；`AutoCombatPanelService` 的 baseline public API 又不接 `TaskExecutionContext`。只改一个文件会继续依赖无 producer holder，改 public 签名则破坏 baseline API。取得 caller-reachable explicit context 且扩充 rounds typed observation 前不得发单，不能把“面板可见”冒充整类闭合。

### External B Successor - TaskTracker Exact-Window Capture Mechanics Prerequisite

- task id: `W-696-TASKTRACKER-PANEL-CAPTURE-LOCAL-MECHANICS-1`
- status: `READY_PREREQUISITE_AFTER_B_RELEASE`
- exact Java write set:
  - create-new DHXY `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\tasktracker\TaskTrackerPanelCaptureLocalMechanics.java`
- baseline anchor: `696a12b0` `TaskTrackerPanelService.java` blob `ad46ec861758737944dda82d784335a9405242f3`；机械权威为 baseline `resolveTrackerPanelRect`、`expandedVisionAnchorToScreenAnchor`、`dragTrackerPanelIfNeeded`。
- real local dependencies: DHXY 已有 `BoundWindowCaptureService`、`WindowNativeBinding`、`ImageFinder`、`InputSequences/InputAction` 和单一 input queue；create-new 目标当前不存在。本单不读 dormant `TASK_TRACKER_READ`，不碰 artifact owner/ledger/materialize。
- direct implementation:
  - 一个 exact-binding 入口完整执行 narrow anchor search -> miss 时 expanded full-window search -> anchor 越安全界时一个 ordered `DRAG_AND_DROP -> SLEEP(500)` -> 在同一次 mechanics 调用内 capture 最终 panel PNG。
  - 本地 closed result 仅承载 mechanical state、PNG bytes、SHA-256、width/height、screen-absolute `absoluteLeft/absoluteTop`；非 captured state 不带图像或原点。title、绿链分割、fingerprint/cache、候选排序、分类和结果构造一律不进入本类。
- acceptance:
  - 保留 baseline anchor template `images/template/task/wubei_tracker_anchor.png`、`0.82` 阈值、narrow/expanded 顺序、安全界、拖拽目标与 `500ms` delay；不得增加重试、TTL、owner 或第二状态机。
  - 本单只是 caller/capture contract 的真实本地 prerequisite，不改 `TaskTrackerPanelService`、`DecisionEngine`、remote enum/codec/digest/handler，也不宣称 TaskTracker 整类完成。

#### External B Backup - TaskTracker Cloud Consumer/Wire

- task id: `W-696-TASKTRACKER-CONTEXT-CAPTURE-CLOUD-ADAPT-1`
- exact future write set: Cloud `TaskTrackerPanelService.java` + closed capture command/result/port + generic local-macro wire files。
- status: `NO_READY`
- evidence: Cloud live same-path Service 仍没有 explicit `TaskExecutionContext` caller，`DecisionEngine.trackerPanelRead(JsonNode)` 只走旧 CPU request；`TaskExecutionContextHolder.callWith(...)` 仍零 producer。generic local-macro files又与当前 C/D BattleRadar共享层在途排程相邻。只有真实 context caller、上面的 local mechanics、双侧合同/handler 都落地后才可发本单；不得以现有 rect fact 代替 PNG artifact。

### External C Successor - PlayerState First-Aid Closed Local Mechanics Prerequisite

- task id: `W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1`
- status: `READY_PREREQUISITE_AFTER_C_RELEASE`
- exact Java write set:
  - create-new DHXY `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\playerstate\PlayerStateFirstAidLocalMacroMechanics.java`
- baseline anchor: `696a12b0` `PlayerStateService.java` blob `096d8917b0372422b3ed141300419f9b71c1392c`；mechanics 来源为 `captureBarsSnapshot*`、mouse-obstruction clear、`findSupplyTargetsFromSnapshot`、`isSupplyNeededFromSnapshot`、`checkAndHealFromSnapshot`、`isHealthyColor`、`healIfUnhealthy`。
- real local dependencies: exact DHXY window binding/capture、`BotProperties` supply flags/thresholds、single input queue/direct input worker、`TaskSleep` 均已存在；create-new 目标当前不存在。该连续段含 capture -> 判断 -> 350ms -> 二次 capture -> right-click -> 800ms，符合 closed local macro 的既定边界。
- direct implementation:
  - 接收 exact binding 和 closed first-aid intent（四目标 enable/threshold），原样执行 cursor obstruction 检查、必要 move-away、一次 bars snapshot、higher-threshold 反证、`350ms` 二次确认、目标原位右键 `100ms` + `800ms` settle。
  - 返回 local typed mechanical result：各目标 healthy/supply-needed/executed/capture-failed/stopped，以及实际采样/点击坐标；不在本类保存 cooldown、task phase、队伍策略或跨调用状态。
- acceptance:
  - HP/MP 颜色公式、sample radius、higher-threshold `+10`、threshold normalization、目标顺序、二次确认和 click/sleep 顺序与 `696a12b0` 一致；输入始终在单一队列/已持有 worker 内完成。
  - 不改现有 `PlayerStateService`、Cloud remote/schema、handler 或 current BattleRadar fact 文件；不新增 owner/session/ledger/TTL/retry。本单是后续 PlayerState typed macro 的本地真实 prerequisite，不计整类完成。

#### External C Backup - PlayerState Whole-Service Consumer

- task id: `W-696-PLAYERSTATE-WHOLE-ADAPT-2`
- exact future Java write set: Cloud `src/main/java/com/bot/dhxy/service/PlayerStateService.java` only。
- status: `NO_READY`
- evidence: first-aid mechanics 只闭合血条连续段；location、摄妖香 icon/remaining OCR、`MainBagSession` 续用仍没有 caller-reachable typed producer/closed terminal。须先完成 first-aid 双侧合同/handler，并分别闭合 location 与 incense typed observation/macro，才能回原 C 做整类一对一适配；不得接入旧 `CloudPlayerStateStateOwner/Governor`。

### External D Successor - Dialog Give-Item Closed Local Mechanics Prerequisite

- task id: `W-696-DIALOG-GIVE-ITEM-LOCAL-MECHANICS-1`
- status: `READY_PREREQUISITE_AFTER_D_RELEASE`
- exact Java write set:
  - create-new DHXY `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\dialog\DialogGiveItemLocalMacroMechanics.java`
- baseline anchor: `696a12b0` `DialogService.java` blob `d7b1c71e5f4b1ac8615060896efdfbd76852cfa4`，精确来源为 `tryGiveItemFromCurrentOptionDialog` 的 input-worker direct 段；`GiveItemService.executeGiveDirectForExclusive(...)` 为现有 public 本地入口。
- real local dependencies: DHXY `CoordinateHelper.findGreenTextInRegion`、`InputProvider`、`TaskSleep`、永久本地 `GiveItemService` 均真实存在；create-new 目标当前不存在。本单只实现 direct mechanics，不修改当前 D 持有的 handler。
- direct implementation:
  - exact bound context 下按 baseline 执行：在 small-dialog rect 以 `images/template/dialog/maintenance/dialog_opt_give.png` / `0.85` 找绿字 -> `getRandomizedPoint(...,20,5)` -> left click `150ms` -> sleep `800ms` -> 同一已持有 input-worker 独占段调用 `executeGiveDirectForExclusive(item,knownBagIndex)`。
  - closed local result 逐项保留 `GIVE_ITEM_FAILED/GIVE_OPTION_NOT_FOUND/INTERRUPTED/GIVE_ITEM_DONE`；类内不选择 Dialog policy、task phase、fallback 或 retry。
- acceptance:
  - 禁止内部再提交 input queue，禁止把 `GiveItemService` 复制到 Cloud，禁止独立 `GIVE_ITEM` wire；该 mechanics 未来只能作为完整 Dialog closed local macro 的一个连续分支接入。
  - 不改 `DialogService`、remote enum/codec/digest、handler 或 BattleRadar 当前写集；本单不宣称 Dialog 整类闭合。

#### External D Backup - Dialog Whole-Service Consumer

- task id: `W-696-DIALOG-WHOLE-ADAPT-2`
- exact future Java write set: Cloud `src/main/java/com/bot/dhxy/service/DialogService.java` only。
- status: `NO_READY`
- evidence: give-item mechanics 只解决一条独占分支；完整 Dialog 还缺 dialog-detection、OCR line/word 与 green/story click closed macro 三类合同。三类双侧 producer/terminal 未齐前，单文件 whole-adapt 仍会伪造结果或把 OCR/template authority迁 Cloud，不能发单。

### Queue #5 Dispatch And Backup Order

1. 当前 A/B/C/D 任一出现返修：原 Worker 先修当前唯一写集，Queue #5 不改派。
2. A 释放后可先发 TeamReturn leader live fact 单；它是本 queue 唯一可直接修改现有 Cloud Service 且 caller/producer 已闭合的后继单。
3. B/C/D 各自释放后，可分别发 TaskTracker capture、PlayerState first-aid、Dialog give-item 三个 create-new 本地 mechanics prerequisite；三者互斥，也不触碰当前 BattleRadar/NpcClick 写集或 generic wire/handler。
4. 四个 backup 均保留当前状态门：AutoCombatPanel/TaskTracker/PlayerState/Dialog 的完整 Cloud consumer 未满足各自前置时不得发；ClientIdentity 仍沿用上一队列的 `NEEDS_USER_DECISION`，本节不擅自选择落点。
5. 任一 prerequisite 后续接 wire 时，父级须重新按当时真实 EOF 和 dirty status 拆互斥文件表；本 helper 不预占当前 C/D generic 文件。

- scope confirmation: 本节只 append 固定 queue helper 报告；未改 Java/POM、A/B/C/D 固定日志、whole-service 计划、migration matrix 或主文档，未运行 Maven/test/runtime，未执行 Git mutation，也未作 reviewer 裁决。

## Candidate Queue #6 - B/D Earliest-Release Successors

- preparedAt: `2026-07-14T15:23:16-04:00`
- role boundary: 本节只是 Next-Task Queue Helper 的后继排班建议，不是 reviewer 结论，不产生
  `APPROVED/BLOCKED`，也不替父级释放 Worker 或发布任务。
- evidence read: 已读取 `696a12b0` whole-service 权威计划、migration matrix、两仓 `git status`、
  Candidate Queue #5，以及四份固定日志物理 EOF。当前 EOF 为 A `5387` 行、B `7362` 行、C `4466`
  行、D `4823` 行。
- current ownership gate:
  - A 已于 `15:21:11` 领取 `W-696-BATTLE-RADAR-WHOLE-ADAPT-1-R2`，继续独占 Cloud
    `BattleRadarService.java`；不得给 A 发后继，不得把 R2 交给别人。
  - B 的 `W-696-TEAMRETURN-LEADER-LIVE-FACT-ADAPT-1` 已交付，父级尚未在真实 EOF 写释放结论；若父级要求
    Repair，B 继续原 Cloud `TeamReturnService.java` 写集，下面 B 队列不发。
  - C 已领取 `W-696-PLAYERSTATE-FIRST-AID-LOCAL-MECHANICS-1`，继续独占 DHXY
    `PlayerStateFirstAidLocalMacroMechanics.java`；不得给 C 发后继。
  - D 的 `W-696-BATTLE-RADAR-DHXY-FACT-1` 已交付，父级尚未在真实 EOF 写释放结论；若父级要求 Repair，
    D 继续原 6 个 DHXY Java 文件，下面 D 队列不发。
- target preflight: 下列四个 create-new Java 目标均已核实当前不存在，且 `git status --short -- <targets>`
  无输出。四项彼此不重叠，也不触碰 A/C 当前文件、B/D 待审文件、Navigation X2、TaskTracker 现有
  `TaskTrackerPanelRectLocalObservationMechanics.java` 或任何 generic remote/schema/handler 文件。
- status semantics: `READY_PREREQUISITE_AFTER_*_RELEASE` 仅表示父级明确释放对应 Worker 后可直接写真实机械
  prerequisite；不代表完整 Service 已闭合、不增加 same-path 计数。

### External B Primary - TaskTracker Exact-Window Same-Frame Capture Mechanics

- task id: `W-696-TASKTRACKER-PANEL-CAPTURE-LOCAL-MECHANICS-1`
- status: `READY_PREREQUISITE_AFTER_B_RELEASE`
- exact Java write set:
  - create-new DHXY
    `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\tasktracker\TaskTrackerPanelCaptureLocalMechanics.java`
- baseline anchor: `696a12b0` `TaskTrackerPanelService.java` blob
  `ad46ec861758737944dda82d784335a9405242f3`；机械来源限于 `resolveTrackerPanelRect`、
  `expandedVisionAnchorToScreenAnchor`、`dragTrackerPanelIfNeeded` 及最终 panel capture。
- existing reachable dependencies: DHXY 已有 `BoundWindowCaptureService`、`WindowNativeBinding`、`ImageFinder`、
  `InputSequences/InputAction` 和单一 input queue，不需要新增 holder producer、fact producer、remote enum 或
  dormant artifact materializer。
- direct implementation:
  - 一个 exact-binding 入口依次执行 narrow anchor search -> miss 时 expanded full-window search -> anchor 超出
    safe area 时一个 ordered `DRAG_AND_DROP -> SLEEP(500)` -> 在同一次 mechanics 调用内 capture 最终 panel。
  - closed local result 只返回 mechanical state、panel PNG bytes、SHA-256、width/height 与 screen-absolute
    `absoluteLeft/absoluteTop`；非 captured state 不带 image/origin。
  - title、绿链分割、fingerprint/cache、候选排序、分类和结果构造继续留 Cloud，本单不修改或宣称完成
    `TaskTrackerPanelService`。
- acceptance:
  - 保留 baseline template `images/template/task/wubei_tracker_anchor.png`、阈值 `0.82`、narrow/expanded
    次序、安全边界、drag 目标与 `500ms` delay；最终 origin 与 PNG 必须来自 drag 后同一帧。
  - 不新增 retry/TTL/owner/session/ledger，不触碰现有 rect-only mechanics、remote wire、handler、Cloud Service
    或 DecisionEngine。

#### External B Backup - NPC Ctrl-Probe Closed Local Mechanics

- task id: `W-696-NPC-CTRL-PROBE-LOCAL-MECHANICS-1`
- status: `READY_PREREQUISITE_AFTER_B_RELEASE`
- exact Java write set:
  - create-new DHXY
    `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\npc\NpcClickCtrlProbeLocalMacroMechanics.java`
- baseline anchor: `696a12b0` `NpcClickService.java` blob
  `74d9b26b76b84052718d5679529f7ffeb46e3273`；caller segment 为
  `clickNpcByCtrlMenuScan` 的 single `submitExclusiveAndWait` callback。
- existing reachable dependencies: baseline/local `NpcClickService` 已真实使用 `InputSequences`、`InputProvider`、
  bound capture、`ImageFinder`、OCR、`TaskSleep` 与现有 NPC local value types；本地 continuous mechanics 的
  authority 无落点歧义，不需要 Cloud context holder 或任何尚未落地的 fact producer。
- direct implementation:
  - 在一个已持有 input-worker 的 closed 入口内原样执行 before capture -> `holdCtrl` -> `sleep(80)` ->
    screen-absolute move -> `sleep(280)` -> after capture -> `ImageFinder.isMatch(...,0.05)` change check ->
    原 OCR/fuzzy keyword scan 与必要 click/verify -> `finally releaseCtrl` -> `sleep(100)`。
  - 输入期间禁止嵌套 submit，Ctrl 按下/释放之间不得跨网络；返回 closed mechanical result，保留
    found/not-found/interrupted、screen-absolute click point、scan region 与 baseline reason/log 所需字段。
- acceptance:
  - probe origin/offset 次序、clamp、scan rect、OCR/fuzzy 匹配、verifier、stop checkpoints、delay、fallback 和
    terminal 映射与 baseline 不变；本单只落真实本地 prerequisite，不改 Cloud/DHXY `NpcClickService`、
    remote/schema/handler，也不宣称整类闭合。

### External D Primary - Dialog Exact-Window Detection Mechanics

- task id: `W-696-DIALOG-DETECTION-LOCAL-MECHANICS-1`
- status: `READY_PREREQUISITE_AFTER_D_RELEASE`
- exact Java write set:
  - create-new DHXY
    `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\dialog\DialogDetectionLocalMechanics.java`
- baseline anchor: `696a12b0` `DialogService.java` blob
  `d7b1c71e5f4b1ac8615060896efdfbd76852cfa4`；机械来源限于
  `detectDialogSnapshotDirect`、`captureDialogSnapshot`、`hasDialogMask`、`hasOptionInLowerHalf`、
  `hasStoryInUpperHalf` 与 capture 前已有 hide-player-names sequence。
- existing reachable dependencies: DHXY 已有 exact window binding/capture、`InputSequences`、`ImagePreprocessor`/
  `ImageProcessorService`、scaled coordinate helper 与 scoped debug path；全部是既定本地 authority，不依赖
  D 当前 BattleRadar handler 写集或尚未落地的 Cloud fact。
- direct implementation:
  - 一个 exact-binding closed 入口保持 baseline hide-player-names 输入与 settle 后只 capture 一帧；在该帧上按
    原顺序执行 dialog mask stddev、lower-half green option 与 upper-half thin-white/green row-pattern 分类。
  - 返回 local typed detection：closed mechanical state、dialog type、screen-absolute dialog rect、同帧 PNG
    bytes/SHA/width/height 及原计数/row metrics；不得在本地选择业务 option、story policy、fallback 或 task phase。
- acceptance:
  - mask/green/story ROI、阈值、判断优先级、single-frame invariant、hide-player-names 时序与 baseline 一致；
    capture/image 失败必须显式 closed state，不能伪成 `NONE`。
  - 不改 `DialogService`、permanent-local `GiveItemService`、remote enum/codec/digest/handler，不新增
    owner/session/ledger/TTL/retry，本单不宣称 Dialog 整类完成。

#### External D Backup - Dialog Give-Item Closed Local Mechanics

- task id: `W-696-DIALOG-GIVE-ITEM-LOCAL-MECHANICS-1`
- status: `READY_PREREQUISITE_AFTER_D_RELEASE`
- exact Java write set:
  - create-new DHXY
    `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\dialog\DialogGiveItemLocalMacroMechanics.java`
- baseline anchor: 同一 `DialogService` blob；连续段为 `tryGiveItemFromCurrentOptionDialog`，现有 public local
  terminal 为 `GiveItemService.executeGiveDirectForExclusive(item, knownBagIndex)`。
- existing reachable dependencies: `CoordinateHelper.findGreenTextInRegion`、`InputProvider`、`TaskSleep` 与永久本地
  `GiveItemService` 均真实存在；不需要独立 `GIVE_ITEM` wire 或 holder/fact producer。
- direct implementation:
  - exact bound context 下在 small-dialog rect 以
    `images/template/dialog/maintenance/dialog_opt_give.png` / `0.85` 找绿字 ->
    `getRandomizedPoint(...,20,5)` -> left click `150ms` -> sleep `800ms` -> 同一已持有 input-worker 段调用
    `executeGiveDirectForExclusive`。
  - closed result 原样表达 `GIVE_ITEM_FAILED/GIVE_OPTION_NOT_FOUND/INTERRUPTED/GIVE_ITEM_DONE`；禁止内部再提交
    input queue，禁止把 GiveItem 复制 Cloud，禁止在本类加入 policy/fallback/retry。
- acceptance: 点击坐标空间、随机半径、hold/delay、调用顺序与 status 映射逐项对齐 baseline；只创建该 mechanics
  文件，不改 Dialog/remote/handler/BattleRadar 写集，不宣称完整 Dialog 已闭合。

### Queue #6 Release And Conflict Rules

1. A/C 保持当前原 Worker 门：A R2 或 C first-aid 有任何返修，仍由原 Worker在原写集内处理；Queue #6 不抢单。
2. B/D 只有在父级于各自固定日志明确写出释放后才可发主单；若当前交付被要求 Repair，先完成 Repair，
   主单与备选均继续排队。
3. B 主单、B 备选、D 主单、D 备选四个 Java 目标彼此互斥；同一 Worker 每次只领主单或备选之一，
   不并行占两单。优先顺序为 B TaskTracker capture、D Dialog detection；备选仅在主单因新 dirty 冲突或父级
   排程不宜时使用。
4. 后续接 Cloud contract/wire/handler 必须另开新文件表并重新核对当时 dirty status；本 queue 不预占 generic
   remote/schema/handler，也不把 create-new mechanics 当作 caller-ready 完整链。
5. `ClientIdentityService` 的 local/cloud fallback authority 仍为 `NEEDS_USER_DECISION`，本节没有擅自选择；
   Navigation X2 仍按已登记 cohort 冻结，不被上述任务触碰。

- scope confirmation: 本节只 append 固定 queue helper 报告；未改 Java/POM、A/B/C/D 固定日志、whole-service
  计划、migration matrix 或主文档，未运行 Maven/test/runtime，未执行 Git mutation，也未作 reviewer 裁决。

## Candidate Queue #7 - Post-Current Four-Way Direct Implementation Queue

- preparedAt: `2026-07-14T15:39:49-04:00`
- role: Next-Task Queue Helper only；本节只提供父级可选的后继文件表，不作 reviewer 裁决，不向 A/B/C/D 发单。
- authority: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` whole-Service 业务基线、当前 whole-service
  plan、migration matrix、两仓完整 status，以及四份固定日志物理 EOF。
- true-EOF delta: 用户消息中的队列状态已继续推进。A 的 BattleRadar R2 已由父级释放，A 当前 EOF 已换成
  `W-696-NPC-CTRL-PROBE-LOCAL-MECHANICS-1`；B 的 TeamReturn R1 已释放并已领取
  `W-696-TASKTRACKER-PANEL-CAPTURE-LOCAL-MECHANICS-1`；C first-aid mechanics 仍是已交付待父级处理；D 仍在
  `W-696-BATTLE-RADAR-DHXY-FACT-1-R1` 原 Worker 返修门。下列后继因此以这四个**最新当前任务**释放为发单门，
  不重复抢占 A/B 已经拿到的 Npc/TaskTracker 文件。

### Queue #7 Current Frozen Write Sets

1. A 当前冻结：DHXY
   `src/main/java/com/bot/dhxy/service/npc/NpcClickCtrlProbeLocalMacroMechanics.java`。
2. B 当前冻结：DHXY
   `src/main/java/com/bot/dhxy/service/tasktracker/TaskTrackerPanelCaptureLocalMechanics.java`。
3. C 当前冻结：DHXY
   `src/main/java/com/bot/dhxy/service/playerstate/PlayerStateFirstAidLocalMacroMechanics.java`。
4. D 当前冻结：DHXY
   `src/main/java/com/bot/dhxy/service/battleradar/BattleRadarLocalObservationMechanics.java`。
5. 下列七个 create-new 目标在本轮盘点时均不存在且 scoped status 为空；唯一 existing target 是 Cloud
   `TaskStepExecutor.java`，当前为 active untracked 文件，未出现在四路当前写集内，发单前仍须由父级显式 reserve。

### External A Successor - Real Task-Step Context Producer

- task id: `W-696-TASK-STEP-CONTEXT-BINDING-PRODUCER-1`
- status: `READY_AFTER_A_CURRENT_RELEASE_AND_PARENT_FILE_RESERVATION`
- exact Java write set:
  - modify Cloud
    `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\template\TaskStepExecutor.java`
- baseline/current anchors:
  - `696a12b0` DHXY `TaskStepExecutor.java` blob
    `9adf202207c9b0a413516819c494f1da67679cd8`；active Cloud file blob at inventory time
    `4d89bca4766831f14fb5f4509a53dbfb0ddc068d`。
  - reachable callers already exist: `BaseTaskTemplate.execute(...)` and `runSingleStep(...)` call
    `taskStepExecutor.execute(context, step, ...)`；existing `TaskExecutionContextHolder.callWith(...)` is present, while
    Cloud source has no actual producer binding a step invocation today.
- direct implementation:
  - inject the existing Spring `TaskExecutionContextHolder` into this component and replace only the actual
    `step.execute(context)` invocation with `holder.callWith(context, () -> step.execute(context))`.
  - keep the existing stop checkpoint, retry count/delay, exception mapping, result logging and loop order byte-for-behavior;
    no default context, epoch `0`, owner/session/ledger/TTL/retry addition, or wrapper chain.
- acceptance:
  - inside every reachable TaskStep invocation, `holder.current()` is the exact argument context and is restored on return,
    exception, stop and retry; null context must not silently create a default binding.
  - scoped diff is this one file only. This closes the **TaskStep execution scope producer only**; it must not be described as
    a whole-task producer for `beforeTask/buildSteps/afterTask` or as complete BattleRadar caller closure.

#### External A Backup - Player-State Status-Bar Visibility Local Observation

- task id: `W-696-PLAYERSTATE-BARS-VISIBILITY-LOCAL-MECHANICS-1`
- status: `READY_AFTER_A_CURRENT_RELEASE`
- exact Java write set:
  - create-new DHXY
    `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\playerstate\PlayerStateStatusBarsVisibilityLocalObservationMechanics.java`
- baseline anchor: `696a12b0` `PlayerStateService.java` blob
  `096d8917b0372422b3ed141300419f9b71c1392c`，只抽取 `areStatusBarsVisibleNoFocus`、
  `captureBarsSnapshotNoFocus` 与 `isHealthyColor` 的机械段。
- direct implementation / acceptance:
  - exact binding 下只 capture baseline status-bar strip；逐像素保持 red/blue 判定，保持
    `red+blue>=16 && (red>=4 || blue>=4)`；返回 closed `CAPTURED_VISIBLE/CAPTURED_NOT_VISIBLE/
    CAPTURE_UNAVAILABLE/MECHANICS_FAILED` 与 red/blue/size metrics。
  - 不移动鼠标、不治疗、不更新 PlayerState/GameContext、不新增 retry/TTL；capture 异常不能伪成 not-visible。
    本单与 C current first-aid 文件互斥，且不宣称 PlayerState 整类完成。

### External B Successor - Incense Status Exact-Window Observation

- task id: `W-696-PLAYERSTATE-INCENSE-STATUS-LOCAL-MECHANICS-1`
- status: `READY_AFTER_B_CURRENT_RELEASE`
- exact Java write set:
  - create-new DHXY
    `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\playerstate\PlayerStateIncenseStatusLocalObservationMechanics.java`
- baseline anchor: same `PlayerStateService.java` blob；机械来源限于 `probeIncenseStatus`、
  `probeIncenseIconPresenceInRect`、`cropSheyaoxiangMatchedColumn`、`readSheyaoxiangRemainingTime`、
  `readSheyaoxiangRemainingMinutesGreen` 与 cyan/green pixel rules。既有本地
  `SheyaoxiangDigitTemplateReader`、capture/template/image APIs 是实际 producer；不依赖 holder fallback。
- direct implementation:
  - exact binding + caller-supplied status rect/optional cached probe rect，按 baseline 顺序 capture -> icon template
    `0.85` -> matched column -> cyan hour digits -> green minute fallback/template reader；所有图像在当前调用 flush。
  - 返回 closed local observation：mechanical state、present/absent、screen-absolute icon point、optional remainingMs/
    source/diagnostics。caller 保留 cache/cooldown、是否使用香、Bag macro 与 state update。
- acceptance: ROI、阈值、cyan-first/green-fallback 顺序、时间单位与 failure matrix 对齐 baseline；无输入、无 Bag
  调用、无跨调用 cache/owner/session/TTL/retry，不修改 C first-aid 或 Cloud PlayerStateService。

#### External B Backup - First/Last Green-Band Dialog Click Mechanics

- task id: `W-696-DIALOG-GREEN-BAND-LOCAL-MECHANICS-1`
- status: `READY_AFTER_B_CURRENT_RELEASE`
- exact Java write set:
  - create-new DHXY
    `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\dialog\DialogGreenBandOptionLocalMacroMechanics.java`
- baseline anchor: `696a12b0` `DialogService.java` blob
  `d7b1c71e5f4b1ac8615060896efdfbd76852cfa4`，只抽取 `clickGreenOption(rect, reason, first)`。
- direct implementation / acceptance:
  - 在已持有 input worker 内对 caller-supplied screen-absolute dialog rect capture 一帧 ->
    `findGreenTextBands` -> `pickGreenTextBand(first)` -> band 中点 -> `getRandomizedPoint(...,12,3)` -> left click
    `150ms`；finally flush，同帧返回 band/absolute click/closed terminal。
  - 显式拒绝非 input-worker；禁止 nested submit、业务 first/last 选择、fallback/retry/TTL；capture failure 与
    no-band 分开，不修改 DialogService/remote/handler。

### External C Successor - Dialog Same-Frame Detection Mechanics

- task id: `W-696-DIALOG-DETECTION-LOCAL-MECHANICS-1`
- status: `READY_AFTER_C_CURRENT_RELEASE`
- exact Java write set:
  - create-new DHXY
    `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\dialog\DialogDetectionLocalMechanics.java`
- baseline anchor: same `DialogService.java` blob；来源限于 `detectDialogSnapshotDirect`、
  `captureDialogSnapshot`、`hasDialogMask`、`hasOptionInLowerHalf`、`hasStoryInUpperHalf` 与既有
  hide-player-names sequence。
- direct implementation:
  - exact binding 下保持 optional hide names/原 settle，只 capture 一帧；mask stddev、lower-half green option、
    upper-half thin-white/green row pattern 全部在该帧按 baseline 优先级求值。
  - closed result 返回 state、dialog type、screen-absolute dialog rect、同帧 PNG bytes/SHA/width/height 与原 metrics；
    本地不选 option、story policy、fallback 或 task phase。
- acceptance: ROI/阈值/判断顺序/single-frame invariant 对齐 baseline；capture missing 与 mechanics exception 分离；
  不改 DialogService、remote/schema/handler，不新增 owner/session/TTL/retry。

#### External C Backup - Dialog Give-Item Closed Local Mechanics

- task id: `W-696-DIALOG-GIVE-ITEM-LOCAL-MECHANICS-1`
- status: `READY_AFTER_C_CURRENT_RELEASE`
- exact Java write set:
  - create-new DHXY
    `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\dialog\DialogGiveItemLocalMacroMechanics.java`
- baseline anchor: same blob，连续段为 `tryGiveItemFromCurrentOptionDialog`；永久本地
  `GiveItemService.executeGiveDirectForExclusive` 是实际 terminal producer。
- direct implementation / acceptance:
  - 已持有 input worker 内 small-dialog `dialog_opt_give.png`/`0.85` -> randomized `(20,5)` -> click `150ms`
    -> sleep `800ms` -> direct GiveItem 调用；closed 映射保持 `GIVE_ITEM_FAILED/GIVE_OPTION_NOT_FOUND/
    INTERRUPTED/GIVE_ITEM_DONE`。
  - 禁止 nested queue、独立 GIVE_ITEM wire、Cloud GiveItem、policy/retry/TTL；只创建该文件。

### External D Successor - Story-Dialog Advance Local Mechanics

- task id: `W-696-DIALOG-STORY-ADVANCE-LOCAL-MECHANICS-1`
- status: `READY_AFTER_D_CURRENT_REPAIR_RELEASE`
- exact Java write set:
  - create-new DHXY
    `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\dialog\DialogStoryAdvanceLocalMacroMechanics.java`
- baseline anchor: same `DialogService.java` blob，严格抽取 `handleStoryDialog` /
  `fastClickStoryDialogDirect` 的 local direct branch。
- direct implementation / acceptance:
  - 显式要求已持有 input worker；保持 pre-sleep `600+random.nextInt(100)` -> scaled large-dialog rect ->
    centerX / bottom-`round(40/scale)` -> randomized `(30,10)` -> click `150ms` -> post-sleep
    `600+random.nextInt(100)`。
  - 返回 closed executed/stopped/mechanics-failed 与 actual screen-absolute click；不检测 dialog、不决定何时推进、
    不 nested submit、不新增 retry/TTL，不修改 D current BattleRadar 文件。

#### External D Backup - Ordered Green-Template Dialog Mechanics

- task id: `W-696-DIALOG-GREEN-TEMPLATE-LOCAL-MECHANICS-1`
- status: `READY_AFTER_D_RELEASE_AND_DIALOG_DETECTION_PREREQUISITE`
- exact Java write set:
  - create-new DHXY
    `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\dialog\DialogGreenTemplateOptionLocalMacroMechanics.java`
- baseline anchor: same blob，严格抽取 `handleGreenTemplateOptionDirect` 的一次 capture/一次 wash/ordered specs 段。
- prerequisite: 若 intent 要求 verify dialog type，必须由已落地的 `DialogDetectionLocalMechanics` 在同一本地调用中提供
  exact type；在该 prerequisite 之前不得把本备选提前标成可发，也不得把缺失 detection 当作 `OPTION`。
- direct implementation / acceptance:
  - caller-ordered specs -> one dialog capture -> one green wash -> 每 spec `ImageFinder.find(...,0.85)`，miss 的
    `-1.0` 只作 diagnostics；首个 hit 用 spec offset/radius 随机化并 click `150ms`，返回 actionKey/template/
    relative+absolute point；all-miss、capture-failed、type-mismatch 分离。
  - 禁止在本地重排业务优先级、nested submit、owner/session/TTL/retry；只创建该文件。

### Queue #7 Release, Mutual-Exclusion And Decision Rules

1. A/B 已有新当前任务，C/D 仍有原门；任何 Repair 都优先回原 Worker/原写集。只有父级在对应固定日志明确释放
   当前任务后，才可从本节给该 Worker 发主单或备选之一。
2. 七个 READY Java 文件表彼此互斥，且均与四个 current frozen write set 互斥；同一 Worker不能同时领主单和
   备选。A primary 的 existing untracked `TaskStepExecutor.java` 必须在发单瞬间再次查 owner/status 并由父级 reserve。
3. 推荐顺序：A context producer -> C dialog detection；B incense 与 D story 可并行。D green-template 备选必须等
   C detection prerequisite，不得靠复制 detection 或默认 `OPTION` 绕过。
4. `ClientIdentityService` 的 title/tracker fallback authority 仍是 `NEEDS_USER_DECISION`，本 queue 不预留其 Java
   写集；Navigation X2 cohort 继续冻结；B 当前 TaskTracker capture 与 A 当前 Npc Ctrl mechanics 均不重复入队。
5. 所有任务都是 current whole-Service 的真实 producer/mechanics prerequisite，不计整类完成；后续 Cloud caller/
   typed wire 必须另列互斥文件表。无已批准业务差异；按 `696a12b0` 基线等价抽取。

- scope confirmation: 本节只 append 固定 next-task helper 报告；未改任何 Java/POM、A/B/C/D 固定日志、计划、
  migration matrix 或主文档，未运行 Maven/test/runtime，未执行 Git mutation，也未作 reviewer 结论。

## Candidate Queue #8 - Four Delivered Slices Post-Review Queue

- preparedAt: `2026-07-14T15:45:55-04:00`
- role boundary: Next-Task Queue Helper only；不复审四份交付、不写通过/不通过结论、不替父级发单。
- authority read: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 四阶段 whole-Service 路线、最新
  migration matrix、Queue #7 与 A/B/C/D 固定日志物理 EOF。
- current delivered-but-not-released files:
  - A: DHXY `src/main/java/com/bot/dhxy/service/npc/NpcClickCtrlProbeLocalMacroMechanics.java`。
  - B: DHXY `src/main/java/com/bot/dhxy/service/tasktracker/TaskTrackerPanelCaptureLocalMechanics.java`。
  - C: DHXY `src/main/java/com/bot/dhxy/service/playerstate/PlayerStateFirstAidLocalMacroMechanics.java` R1。
  - D: DHXY `src/main/java/com/bot/dhxy/service/battleradar/BattleRadarLocalObservationMechanics.java` R1。
- release rule: 下列每路主单/备选都只能在父级处理该路最新交付并于原固定日志明确释放后发布；若父级要求原
  Worker 返修，原交付文件优先，Queue #8 不抢占、不内部接管。

### External A Next - Bind Exact Context At Reachable Task-Step Invocation

- task id: `W-696-TASK-STEP-CONTEXT-BINDING-PRODUCER-1`
- status: `READY_AFTER_PARENT_RELEASE_AND_FILE_RESERVATION`
- unique Java write set:
  - modify Cloud
    `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\template\TaskStepExecutor.java`
- baseline methods/lines:
  - DHXY `696a12b0` `TaskStepExecutor.execute(context,step,overrideRetryPolicy)`，method `20-61`，实际
    `step.execute(context)` 调用点 `35`；current Cloud `BaseTaskTemplate.execute(...)` / `runSingleStep(...)`
    的 reachable callers 为 `54-75` / `151`。
- concrete implementation:
  - 注入现有 Spring `TaskExecutionContextHolder`；只把调用点改为
    `holder.callWith(context, () -> step.execute(context))`。
  - 保持原 stop checkpoint、日志、null-result 映射、catch 顺序、retry count/delay 与返回值；不得创建 default
    context、epoch `0` fallback、wrapper chain，亦不得新增 owner/permit/session/ledger/TTL/auto retry。
- dependency/release conditions:
  - A 当前 Npc Ctrl mechanics 已由父级释放；父级发单前再次 reserve 这个 existing active Cloud 文件，确认没有
    新 Worker 写它。现有 holder bean 和 `BaseTaskTemplate -> TaskStepExecutor` caller 均已存在，不依赖新 fact。
- acceptance gate:
  - 每次 reachable step 内 `holder.current()` 恰为传入 context，normal/exception/stop/retry 返回后恢复前值；null
    context 不得静默绑定默认值。
  - scoped change 只有该文件；明确只闭合 **TaskStep execution scope**，不宣称 `beforeTask/buildSteps/afterTask`
    已有 whole-task producer，也不据此单独计完整 Service。

#### External A Backup - First/Last Green-Band Dialog Click Mechanics

- task id: `W-696-DIALOG-GREEN-BAND-LOCAL-MECHANICS-1`
- status: `READY_AFTER_PARENT_RELEASE`
- unique Java write set:
  - create-new DHXY
    `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\dialog\DialogGreenBandOptionLocalMacroMechanics.java`
- baseline methods/lines: `696a12b0` `DialogService.clickGreenOption(rect,reason,first)` `2486-2513`。
- concrete behavior:
  - 在已持有 input-worker 内，对 caller-supplied screen-absolute rect capture 一帧 ->
    `findGreenTextBands` -> `pickGreenTextBand(first)` -> band center -> randomized `(12,3)` -> left click `150ms`；
    finally flush，同帧返回 band、actual absolute click 与 closed terminal。
- dependency/release conditions: 只依赖既有 DHXY capture/ImagePreprocessor/CoordinateHelper/InputProvider；不依赖 A
  当前 Npc 文件、TaskTracker、remote producer 或新 fact。
- acceptance gate: 非 input-worker 显式拒绝；capture-failed 与 no-band 分离；禁止 nested submit、本地决定 first/last、
  fallback/retry/TTL；只创建该文件。

### External B Next - Dialog Same-Frame Detection Mechanics

- task id: `W-696-DIALOG-DETECTION-LOCAL-MECHANICS-1`
- status: `READY_AFTER_PARENT_RELEASE`
- unique Java write set:
  - create-new DHXY
    `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\dialog\DialogDetectionLocalMechanics.java`
- baseline methods/lines: `696a12b0` `DialogService.detectDialogTypeNoFocus` `1506-1549`、
  `detectDialogSnapshotDirect` `1550-1598`、`captureDialogSnapshot` `1638-1688`、
  `hasOptionInLowerHalf/hasDialogMask/hasStoryInUpperHalf` `1689-1760`。
- concrete behavior:
  - exact binding 下保持 optional hide-player-names 与原 wait；只 capture 一帧，在该帧按 baseline 优先级执行 mask
    stddev、lower-half green-option、upper-half thin-white/green row-pattern 分类。
  - closed local result 返回 mechanical state、dialog type、screen-absolute dialog rect、同帧 PNG bytes/SHA/
    width/height 与原 metrics；Cloud caller 保留 option/story/business/fallback 决策。
- dependency/release conditions: B 当前 TaskTracker capture mechanics 先由父级释放；本单只依赖已有 exact-window
  capture、image preprocess/processor 与 local input primitive，不依赖 TaskTracker wire/context producer。
- acceptance gate: ROI、阈值、判断顺序、single-frame invariant、hide-name 时序逐项对齐 baseline；capture missing
  与 mechanics exception 分离；不改 DialogService/remote/schema/handler，不新增 retry/TTL/owner/session。

#### External B Backup - Dialog Give-Item Closed Local Mechanics

- task id: `W-696-DIALOG-GIVE-ITEM-LOCAL-MECHANICS-1`
- status: `READY_AFTER_PARENT_RELEASE`
- unique Java write set:
  - create-new DHXY
    `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\dialog\DialogGiveItemLocalMacroMechanics.java`
- baseline methods/lines: `696a12b0` `DialogService.tryGiveItemFromCurrentOptionDialog` `1350-1382`。
- concrete behavior:
  - 已持有 input-worker 内 small-dialog `dialog_opt_give.png` / `0.85` -> randomized `(20,5)` -> left click
    `150ms` -> sleep `800ms` -> 永久本地 `GiveItemService.executeGiveDirectForExclusive(item,index)`。
  - closed status 保持 `GIVE_ITEM_FAILED/GIVE_OPTION_NOT_FOUND/INTERRUPTED/GIVE_ITEM_DONE`。
- dependency/release conditions: 既有 local GiveItem direct API 是实际 terminal producer；不需要独立 GIVE_ITEM wire、
  holder 或新 fact。
- acceptance gate: 坐标/阈值/hold/delay/调用顺序与 status 一一对齐；禁止 nested queue、Cloud GiveItem、
  owner/TTL/retry；只创建该文件。

### External C Next - Incense Status Exact-Window Observation

- task id: `W-696-PLAYERSTATE-INCENSE-STATUS-LOCAL-MECHANICS-1`
- status: `READY_AFTER_PARENT_RELEASE`
- unique Java write set:
  - create-new DHXY
    `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\playerstate\PlayerStateIncenseStatusLocalObservationMechanics.java`
- baseline methods/lines: `696a12b0` `PlayerStateService.probeIncenseStatus` `1002-1055`、
  `probeIncenseIconPresence*` `1056-1122`、`cropSheyaoxiangMatchedColumn` `1123-1166`、
  remaining-time readers/pixel rules `1167-1297`。
- concrete behavior:
  - exact binding + caller-supplied status rect/optional cached probe rect；保持 capture -> icon template `0.85` ->
    matched column -> cyan hour digits -> green minute fallback / `SheyaoxiangDigitTemplateReader` 的顺序。
  - 返回 mechanical state、present/absent、screen-absolute icon point、optional remainingMs/source/diagnostics；caller
    继续拥有 cache/cooldown、是否使用香、Bag macro 与 state update。
- dependency/release conditions: C current first-aid R1 先由父级释放；既有本地 digit helper、capture/template/image
  API 是真实 producer，不依赖 holder fallback，不修改 first-aid 文件。
- acceptance gate: ROI、阈值、cyan-first/green-fallback、时间单位、图像 flush 与 failure matrix 对齐 baseline；零
  input/Bag 调用/跨调用 cache；禁止 owner/session/ledger/TTL/retry。

#### External C Backup - Player-State Status-Bar Visibility Observation

- task id: `W-696-PLAYERSTATE-BARS-VISIBILITY-LOCAL-MECHANICS-1`
- status: `READY_AFTER_PARENT_RELEASE`
- unique Java write set:
  - create-new DHXY
    `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\playerstate\PlayerStateStatusBarsVisibilityLocalObservationMechanics.java`
- baseline methods/lines: `696a12b0` `PlayerStateService.areStatusBarsVisibleNoFocus` `388-414`、
  `captureBarsSnapshotNoFocus` `703-708`、`isHealthyColor` `940-949`。
- concrete behavior: exact binding 下只 capture baseline bars strip，逐像素保持 red/blue 分类与
  `red+blue>=16 && (red>=4 || blue>=4)`；返回 visible/not-visible/capture-unavailable/mechanics-failed 及
  red/blue/size metrics。
- dependency/release conditions: 无输入、无 holder/fact producer；与 current first-aid 文件互斥。
- acceptance gate: capture exception 不得伪成 not-visible；不得移动鼠标、治疗、更新 PlayerState/GameContext 或
  增加 retry/TTL；只创建该文件。

### External D Next - Story-Dialog Advance Local Mechanics

- task id: `W-696-DIALOG-STORY-ADVANCE-LOCAL-MECHANICS-1`
- status: `READY_AFTER_PARENT_RELEASE`
- unique Java write set:
  - create-new DHXY
    `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\dialog\DialogStoryAdvanceLocalMacroMechanics.java`
- baseline methods/lines: `696a12b0` `DialogService.handleStoryDialog` / `fastClickStoryDialogDirect` `1771-1789`。
- concrete behavior:
  - 已持有 input-worker；pre-sleep `600+random.nextInt(100)` -> scaled large-dialog rect -> centerX /
    bottom-`round(40/scale)` -> randomized `(30,10)` -> left click `150ms` -> post-sleep
    `600+random.nextInt(100)`。
  - closed result 只表达 executed/stopped/mechanics-failed 与 actual screen-absolute click；caller 决定何时推进 story。
- dependency/release conditions: D current BattleRadar R1 先由父级释放；只依赖 existing CoordinateHelper/
  InputProvider/TaskSleep，不依赖 BattleRadar、new fact 或 remote wire。
- acceptance gate: 两次随机 delay、scale math、random radius、click hold 与顺序逐项保持；非 input-worker 拒绝，
  禁止 nested submit、dialog detection/business policy、owner/TTL/auto retry。

#### External D Backup - Story-Objective Same-Frame Capture Mechanics

- task id: `W-696-DIALOG-STORY-OBJECTIVE-LOCAL-OBSERVATION-1`
- status: `READY_AFTER_PARENT_RELEASE`
- unique Java write set:
  - create-new DHXY
    `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\dialog\DialogStoryObjectiveLocalObservationMechanics.java`
- baseline methods/lines: `696a12b0` `DialogService.captureCurrentStoryImage` `2389-2412`、
  `captureCurrentStoryObjectiveSnapshotNoDetect` `2413-2444`、
  `cropStoryObjectiveFromWindowSnapshotNoDetect` `2445-2469`。
- concrete behavior:
  - exact binding 下按 baseline rect capture 当前 story frame，并从该同一 frame 裁 objective ROI；返回 closed state、
    screen-absolute source/objective rect、objective PNG bytes/SHA/width/height。不得在本地 OCR、分类 objective、选择
    route 或推进 dialog。
- dependency/release conditions: 现有 local exact-window capture/image encode 足够；不依赖 D current BattleRadar、
  Dialog detection 或新 typed fact。
- acceptance gate: crop 边界、same-frame ownership、coordinate space、flush 与 unavailable/exception terminal 明确；
  零 input/业务决策/owner/session/TTL/retry，只创建该文件。

### Queue #8 Mutual Exclusion, Deferred Boundaries And Handoff

1. 八个候选 Java 写集彼此互斥，并与 A/B/C/D 四个 delivered-but-not-released 文件互斥；每路一次只发主单或
   备选之一。发单前父级仍须重查真实 EOF 和目标 owner/status。
2. 推荐主单并行顺序：A task-step context producer、B dialog detection、C incense observation、D story advance。
   四者没有共享 generic remote/schema/handler 文件。
3. B current TaskTracker capture 交付不等于 caller-ready：其 Cloud/DHXY closed wire 仍须等 exact context producer
   与独立互斥文件表后另开 cohort；本节没有激活 dormant artifact/materialize/owner/ledger 流程。
4. A current Npc Ctrl mechanics 仍需后续 caller/typed boundary；D BattleRadar caller integration 依赖 exact context
   producer。两者都不能仅凭 create-new mechanics 或本 helper 报告计完整 Service。
5. `ClientIdentityService` 的 local/cloud title fallback 仍为 `NEEDS_USER_DECISION`；Navigation X2 保持原 cohort；
   本节不擅自选择、不预占其文件。
6. 所有候选均禁止新增 owner/permit/session/ledger/compaction/durable workflow/business TTL/auto retry。
   无已批准业务差异；按 `696a12b0` 基线等价抽取/接线。

- scope confirmation: 本节只 append 固定 next-task helper 报告；未改 Java、A/B/C/D 日志、主计划或矩阵，
  未运行 Maven/test/runtime/Git，也未作 reviewer 结论。

## Candidate Queue #9A - External A Whole AutoBattle Lifecycle Context Producer

- preparedAt: `2026-07-14T16:22:48-04:00`
- helper role: Next-Task Queue Helper only；本节是候选实施 brief，不是 reviewer 的
  `APPROVED/BLOCKED` 结论。
- task id: `W-696-AUTOBATTLE-WHOLE-LIFECYCLE-CONTEXT-PRODUCER-1`
- status: `READY_AFTER_A_CURRENT_RELEASE_AND_PARENT_FILE_RESERVATION`
- target value: 一次恢复 `696a12b0` 的完整 `AutoBattleTask` 公共执行生命周期，并让现有
  `BattleRadarService` typed fact consumer 在整次 task/step 生命周期内取得 caller-supplied
  `TaskExecutionContext`；不依赖无 producer 的裸 `TaskExecutionContextHolder.current()` fallback。

### Exact Java Write Set (3 Files)

1. modify Cloud
   `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\AutoBattleTask.java`
2. modify Cloud
   `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\template\BaseTaskTemplate.java`
3. modify Cloud
   `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\template\TaskStepExecutor.java`

上述三文件与当前 B `DialogDetectionLocalMechanics`/`ImagePreprocessor`、C
`PlayerStateFirstAidLocalMacroMechanics`、D `DialogStoryAdvanceLocalMacroMechanics` 写集互斥；也不触碰 A
当前 `NpcClickCtrlProbeLocalMacroMechanics`。发单前父级只需确认 A 当前单已释放，并为这三文件建立唯一 reservation。

### 696a12b0 And Current Anchors

- baseline `696a12b0:src/main/java/com/bot/dhxy/task/AutoBattleTask.java`：constructor `62`、
  `execute()` `103`、`execute(TaskExecutionContext)` `117-163`、combat tick `164`、`stop` `175`、idle
  maintenance `184-236`、local team return `237-264`、support-mode wait `265-281`、poll interval
  `282-292`、retry policy `293-296`。这是整类范围，不得只搬 public shell。
- current Cloud `AutoBattleTask.java:1-44` 只是 dormant pure-leaf partial：不继承
  `BaseTaskTemplate`，没有 constructor、Spring bean、execute/stop 或 Service lifecycle。
- baseline `BaseTaskTemplate.execute(TaskExecutionContext)` 从约 `35` 开始，保持
  `beforeTask -> checkpoint -> buildSteps -> ordered step loop -> afterTask/terminal catch`；baseline
  `TaskStepExecutor.execute` 约 `14-64`，真实 `step.execute(context)` 在约 `33`。
- current Cloud `BaseTaskTemplate.execute(TaskExecutionContext)` 在约 `54-93`，current
  `TaskStepExecutor.execute(..., overrideRetryPolicy)` 从约 `20` 开始，真实 attempt call 仍在约 `33`。
- existing consumer anchors：current `AutoCombatService.java:150,201,225` 调用
  `BattleRadarService.checkAndSyncCombatState()`；current `BattleRadarService.java:479-506` 通过
  `context.getGameClient().readWindowFact(...)` 消费已存在的 `BATTLE_RADAR_*` typed fact terminal。

### Reachable Public Chain And Concrete Implementation

1. 以 `GameTask` 当前公共合同 `AutoBattleTask.execute(TaskExecutionContext)` 作为显式 authorized entry，
   完整恢复 696 的 constructor、所有 dependencies、execute/stop、idle maintenance、team-return、support-mode 与
   polling/retry 方法。保持每个判断、调用顺序、delay、fallback、日志与 state update；只做当前 Cloud 类型/import
   所需的机械适配。
2. 给 `AutoBattleTask` 注入现有 `TaskExecutionContextHolder`。在
   `execute(TaskExecutionContext)` 完成现有 exact-context validation 后，用
   `holder.callWith(context, ...)` 包住原有完整 lifecycle body；不拆新 wrapper，不 mint context，不把
   context-null 转成 default/epoch=0。因为 AutoBattle 覆盖 template execute，这一层不能只依赖 base binding。
3. 给 `BaseTaskTemplate` 注入同一现有 holder，并在
   `execute(TaskExecutionContext)` 的 validation 后包住完整 before/steps/after/catch 生命周期；保持
   beforeTask 后 checkpoint、step 顺序、afterTask 时点和 typed transition passthrough 不变。
4. 给 `TaskStepExecutor` 注入同一 holder；只在每次真实 `step.execute(context)` attempt 周围建立 nested
   same-context binding。retry 次数、retry delay、checkpoint、catch 顺序、`TaskStepResult` 映射和日志均原样。
5. 由此形成真实链：
   `AutoBattleTask.execute(context)` -> `AutoCombatService.handleCombatTick(...)` ->
   `BattleRadarService.checkAndSyncCombatState()` -> `TaskExecutionContextHolder.current()` ->
   `context.getGameClient().readWindowFact(BATTLE_RADAR_*)` -> 已有 DHXY fact producer/handler ->
   `WindowFactOutcome` typed terminal。既有 terminal 语义仍为 `OBSERVED` 返回 fact、`NOT_EXECUTED` 返回空事实、
   `STOPPED` 走 checkpoint/fatal、其它 terminal fail，不在本单改写。

### Acceptance Gate

- `AutoBattleTask` 对 `696a12b0` 做整类 public/private method inventory；constructor dependencies、全部方法、
  分支、调用次序、sleep/poll interval、fallback、stop/state/log 均无遗漏，不能保留当前 44 行 partial 语义。
- explicit context 在 `beforeTask` 前已绑定，并覆盖 success/skipped/failed/stopped/typed-transition/exception 的整个
  unwind；每一层在 normal、exception 和 retry 后精确恢复 previous holder value，不能泄漏到下一 task/revision。
- `execute()` 仍拒绝无 authority context；不得恢复 local ThreadLocal/default context，不得把缺 context 静默降级为
  `epoch=0`。
- `TaskStepExecutor` 的 attempt 次数、delay、checkpoint、异常分类和 result mapping 与当前/baseline 一致；新增
  binding 不能构成新的 retry、TTL 或 lifecycle decision。
- 静态核验上述 AutoBattle 所调用的 current Cloud public API 均已存在：startup check、first aid、maintenance
  init/opportunistic maintenance、combat initialize/tick、team-return support、common-box、left-top switch、dynamic
  poll interval。实施者遇到签名不一致只能报告 blocker，不能扩写第四个文件。
- 父级在其他 Java writers 稳定后运行 fresh Cloud compile/package；本 helper 不运行构建、不把 compile success
  代替整类对照。

### Dependencies, Exclusions And Risks

- dependency/release: A 当前 Npc Ctrl-probe 文件先由父级释放；三文件 reservation 后可直接发单，无需新增
  fact、schema、handler、holder producer 或 runner/host 改动。
- public reachability boundary: 本单闭合的是当前 `GameTask.execute(TaskExecutionContext)` 公共执行入口到
  AutoBattle whole lifecycle 再到既有 BattleRadar typed terminal。它不启动 runtime，也不声称已有 startup/runner
  dispatch 被修改；这符合本轮 runner/host 冻结。
- compile risk: 当前 44 行 partial 被整类替换后可能揭示机械签名差异；已逐项确认其直接调用的 current Cloud API
  名称存在，但任何额外缺口必须回父级，不得越界修改 Service 或 generic wire。
- Navigation/Npc exclusion: holder producer 会让它们将来在同一 lifecycle 中获得 context，但本单不触碰其
  local boundary，也不宣称 Navigation/Npc whole Service 闭合。BattleRadar typed fact 链已实际存在，故仅把它列为
  本单可验收 consumer。
- 禁止新增 owner/permit/session/ledger/compaction/durable workflow/business TTL/auto retry；禁止修改业务顺序、
  retry、delay、fallback；禁止触碰 runner/host/tests。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #22 - tooltip / prepared-action R2 / OCR-words R1 / player-anchor rebase

记录时间：`2026-07-14T21:48:30-04:00`。本节仅提供 non-binding 后继排班，不作源码审查结论。
当前 reservation 按四份固定日志真实 EOF 锁定：

- A：New DHXY `src/main/java/com/bot/dhxy/service/npc/NpcClickTaskTooltipLocalMacroMechanics.java`；
- B：Cloud `service/DialogService.java`、Cloud `remote/DialogPreparedActionValidationMacroCommand.java`、DHXY
  `cloud/remote/RemoteDialogPreparedActionValidationMacroCommandPayload.java`；
- C：DHXY `src/main/java/com/bot/dhxy/service/dialog/DialogOptionOcrWordsLocalObservationMechanics.java` R1；
- D：New DHXY `src/main/java/com/bot/dhxy/service/npc/NpcClickPlayerAnchorLocalObservationMechanics.java`。

Queue #21 的 A player-anchor primary 已被 D 当前任务消费，标记
`SUPERSEDED_BY_D_PLAYER_ANCHOR_ASSIGNMENT`，不得再派给 A。以下 primary 都只在各自当前文件 source release 后生效；
凡写 generic remote family 的任务必须取得单一 shared slot，不能并发发单。

### External A primary / backup

- primary=`W-696-NPC-TASK-TOOLTIP-FULL-CALLER-CHAIN-1`；状态=
  `READY_AFTER_A_TOOLTIP_SOURCE_RELEASE_AND_SHARED_SLOT`。A 当前 tooltip mechanics 只读，不在后继写集内。
- Cloud 写集：New `NpcTaskTooltipMacroCommand.java`、`NpcTaskTooltipMacroResult.java`、
  `CloudNpcTaskTooltipPort.java`；Modify `LocalMacroKind.java`、`LocalMacroCommand.java`、
  `LocalMacroRequest.java`、`LocalMacroOutcome.java`、`RemoteCommandOutcomeEnvelope.java`、
  `RemoteProtocolDigests.java`、`service/NpcClickService.java`。
- DHXY 写集：New `RemoteNpcTaskTooltipMacroCommandPayload.java`、
  `RemoteNpcTaskTooltipMacroResultPayload.java`；Modify `RemoteLocalMacroKind.java`、
  `RemoteLocalMacroCommandPayload.java`、`RemoteLocalMacroResultPayload.java`、
  `RemoteOperationPayloadCodec.java`、`RemoteProtocolDigests.java`、`LocalRemoteGameCommandHandler.java`。
- baseline=`696a12b0:NpcClickService.java:1147-1260,176-216,1433-1468`；reachable chain=
  `clickNpcSmartWithOutcome -> Cloud tooltip strategy -> typed port -> DHXY handler -> released tooltip mechanics ->
  typed terminal -> Cloud strategy/memory outcome`。完整保留 region/点顺序、`0.82/36`、
  move/150/click150/1200/verify、record point `Y+90` 与 learned ROI。
- terminal 保留 `VERIFIED/CLICK_NOT_VERIFIED/NOT_FOUND/BINDING_UNAVAILABLE/TEMPLATE_UNAVAILABLE/
  INTERRUPTED/MECHANICS_FAILED`，并严格映射既有四态 envelope；验收 public caller 可达、payload/digest closed、
  点击证据不丢失、Cloud 继续拥有 NPC/strategy/fallback。
- backup=`W-696-NPC-CTRL-MENU-WHOLE-LOCAL-MACRO-1`；状态=
  `READY_AFTER_TEXT_RECOGNIZER_PROVIDER_PARITY`；唯一 New
  `service/npc/NpcClickCtrlMenuWholeLocalMacroMechanics.java`，锚点 `NpcClickService:303-585`。必须一次闭合
  Ctrl-hold/hover/before-after change/yellow OCR/click/verify/release；现有 local-only OCR 尚未证明等价 baseline
  provider fallback，技术门解除前零 reservation。

### External B primary / backup

- primary=`W-696-DIALOG-WHITE-STORY-TEMPLATE-FULL-CHAIN-1`；状态=
  `READY_AFTER_B_PREPARED_ACTION_R2_SOURCE_RELEASE_AND_SHARED_SLOT`。
- Cloud 写集：New `DialogWhiteStoryTemplateMacroCommand.java`、`DialogWhiteStoryTemplateMacroResult.java`、
  `CloudDialogWhiteStoryTemplatePort.java`；Modify generic six
  `LocalMacroKind/LocalMacroCommand/LocalMacroRequest/LocalMacroOutcome/RemoteCommandOutcomeEnvelope/
  RemoteProtocolDigests` + `service/DialogService.java`。
- DHXY 写集：New `RemoteDialogWhiteStoryTemplateMacroCommandPayload.java`、
  `RemoteDialogWhiteStoryTemplateMacroResultPayload.java`；Modify generic five
  `RemoteLocalMacroKind/RemoteLocalMacroCommandPayload/RemoteLocalMacroResultPayload/RemoteOperationPayloadCodec/
  RemoteProtocolDigests` + `LocalRemoteGameCommandHandler.java`；released white-story mechanics 只读。
- baseline=`696a12b0:DialogService` white-story prepared-option lifecycle；reachable chain=
  `prepareWhiteStoryTemplate* -> Cloud DialogService -> typed port -> handler -> exact same-frame mechanics ->
  typed terminal -> PreparedDialogAction/absent/miss`。terminal 保留 `MATCHED/STORY_MISS/STORY_ABSENT/
  CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/MECHANICS_FAILED`；验收 caller-order、`0.85`、single-frame artifact、
  screen-absolute point、strict canonical/digest，且本地不做 option/fallback 业务选择。
- backup=`W-696-DIALOG-GREEN-TEMPLATE-OPTION-FULL-CHAIN-1`；状态=
  `ALTERNATIVE_AFTER_B_R2_SOURCE_RELEASE`。同一 shared family 的互斥替代，只读 released green mechanics，
  使用 Green 专用 command/result/port/payload；不得与 primary 同时 reservation。

### External C primary / backup

- primary=`W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1`；状态=
  `READY_AFTER_C_OCR_WORDS_R1_SOURCE_RELEASE_AND_SHARED_SLOT`。C 当前 OCR-words mechanics 与 released OCR-image
  mechanics 均只读。
- Cloud 写集：New `DialogOptionOcrImageMacroCommand.java`、`DialogOptionOcrImageMacroResult.java`、
  `CloudDialogOptionOcrImagePort.java`、`DialogOptionOcrWordsMacroCommand.java`、
  `DialogOptionOcrWordsMacroResult.java`、`CloudDialogOptionOcrWordsPort.java`；Modify generic six +
  `service/DialogService.java`。
- DHXY 写集：New `RemoteDialogOptionOcrImageMacroCommandPayload.java`、
  `RemoteDialogOptionOcrImageMacroResultPayload.java`、`RemoteDialogOptionOcrWordsMacroCommandPayload.java`、
  `RemoteDialogOptionOcrWordsMacroResultPayload.java`；Modify generic five + handler。
- baseline=`696a12b0:DialogService.java:1792-1895`、`GameTextLineOcrService.java:120+`；reachable chain=
  `Cloud Dialog caller -> image port 恰一次 capture -> immutable same-frame green/yellow PNG -> green words port ->
  Cloud alias/keyword decision -> miss 时才用同一 yellow bytes 调 words port -> Cloud merge/fallback/action`。
  terminal 分开 image/capture invalid、OCR unavailable、words/no-words，不建立 retained local artifact/session。
- 验收 green-first、same-frame authority、caller-order boxes/坐标、两次 operation 的 strict payload/canonical、Cloud
  保留颜色选择/alias/keyword/fallback/click 决策。
- backup=`W-696-TASKTRACKER-PANEL-CLOUD-WHOLE-CLASS-1`；状态=
  `READY_AFTER_EXPLICIT_TASK_CONTEXT_AND_PANEL_CAPTURE_OCR_PORT`；只改 Cloud `TaskTrackerPanelService.java` 并保留
  green-chain/fingerprint/cache/sort/classification/result。真实 context producer 与 panel artifact contract 未闭合前
  零 reservation，不把算法下沉 DHXY。

### External D primary / backup

- primary=`W-696-NPC-PLAYER-ANCHOR-FULL-CALLER-CHAIN-1`；状态=
  `READY_AFTER_D_PLAYER_ANCHOR_SOURCE_RELEASE_AND_SHARED_SLOT`。D 当前 player-anchor mechanics 只读。
- Cloud 写集：New `NpcPlayerAnchorMacroCommand.java`、`NpcPlayerAnchorMacroResult.java`、
  `CloudNpcPlayerAnchorPort.java`；Modify generic six + `service/NpcClickService.java`。
- DHXY 写集：New `RemoteNpcPlayerAnchorMacroCommandPayload.java`、
  `RemoteNpcPlayerAnchorMacroResultPayload.java`；Modify generic five + handler。
- baseline=`696a12b0:NpcClickService.java:2505-2531,2865-2996,3132+`；reachable chain=
  `Cloud NpcClick player-anchor branch -> typed port -> handler -> released player-anchor mechanics ->
  CAPTURED/NO_PURPLE_BLOB/... -> Cloud OCR/provider fallback/map formula/candidate verdict/click`。验收 Alt+4/capture
  顺序、default-mask 分支、purple threshold/blob bounds、PNG/SHA/dimensions/screen rect、owner 与 terminal 严格透传。
- 本地只产生 typed image/fact，不持有玩家名 OCR fallback、候选判定、公式、点击或 strategy。
- backup=`W-696-NAVIGATION-X2-CLOSED-MACRO-FULL-CHAIN-1`；状态=
  `READY_AFTER_SHARED_RELEASE_AND_X2_QUEUE_BOUNDARY`。X2、successful mouse-away 与 surrounding direct input 必须
  在同一 closed macro，禁止 exclusive callback 内二次排队；queue boundary 未闭合前零 reservation。

### Queue #22 shared serialization / ambiguity discipline

- A/B/C/D primary 都会修改各仓 generic enum/codec/digest/handler family，文件表有意重叠但只能顺序实施。
  推荐 shared slot 顺序：`B white-story -> A tooltip -> D player-anchor -> C option-OCR`；父级每次只能发其中一单，
  上一单 source release 后再发下一单。
- backup 全部是 non-reserving alternative；A Ctrl-menu、C TaskTracker、D Navigation X2 都有明确技术门，不能为填槽
  伪写 READY。`ClientIdentityService` 落点仍为 `NEEDS_USER_DECISION`，不自动排入任何 Worker。
- 四个当前 reservation 与四个 primary 的专用 New 文件零交集；shared generic family 仅按上述串行规则取得。
  永久本地 `BagService/UICleanerService/GiveItemService/QuestManagerService` 不迁 Cloud。
- 不新增 owner/permit/session/ledger/compaction/durable workflow/business TTL/auto retry。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #19 - post-R2/R3 / 20-file / yellow-target release wave

记录时间：`2026-07-14T21:06:16-04:00`。本节仅是 non-binding 排班建议，不作源码审批。真实 EOF 已核：
A 的 OCR-image R3、C 的 white-story R2、D 的 yellow-target Implementation #2 均已交付待父级释放；B 仍持有
prepared-action validation 20-file full chain。四个 primary 只有各自 release gate 满足后才可发单。

### 硬冻结与排程规则

- D 当前 `src/main/java/com/bot/dhxy/service/npc/NpcClickYellowTargetLocalObservationMechanics.java` 及其内联
  yellow candidate closure **不进入 #19 任一 primary/backup 写集**；不再拆 candidate helper/DTO，也不转交别路。
- B 当前及下一 full-chain 独占两仓 local-macro enum/command/result/codec/digest/handler 与 Cloud
  `DialogService`。A/C/D 只写各自一个 create-new 大型 mechanics 类，不碰 shared wire。
- 基线 blob：`NpcClickService.java=74d9b26b76b84052718d5679529f7ffeb46e3273`、
  `DialogService.java=d7b1c71e5f4b1ac8615060896efdfbd76852cfa4`、
  `GameTextLineOcrService.java=c6ed06033b5864e3af4d634f5089e78882cd2103`。

### External A primary - task-tooltip 完整连续本地宏

- task=`W-696-NPC-TASK-TOOLTIP-WHOLE-LOCAL-MACRO-1`；状态=`READY_AFTER_A_R3_SOURCE_RELEASE`。
- 唯一 Java 写集：New DHXY
  `src/main/java/com/bot/dhxy/service/npc/NpcClickTaskTooltipLocalMacroMechanics.java`（目标已核不存在）。
- 696 anchors=`NpcClickService:1147-1260,176-216,1433-1468`；未来 caller 为 Cloud
  `NpcClickService.clickNpcByTaskTooltipTemplate` 原位置。command 只接 Cloud 已决定的 caller-order regions、tooltip
  template key 与 verifier mode；本地不决定 NPC/strategy/fallback。
- 完整 mechanics：exact binding -> 逐 caller-order region 按 `threshold=0.82/minDistance=36` 找全部 match -> score
  顺序逐点 -> 在同一个 remote exclusive input 段 direct `move/150ms/click hold 150ms/1200ms` -> exact-window
  dialog/battle verify -> 首个 verified 返回；全部耗尽才返回 miss。禁止 queue-in-queue，record point 仍为 match
  center `Y+90`，learned ROI 仍为 `[-150,-100,+150,+200]`。
- terminal=`VERIFIED/CLICK_NOT_VERIFIED/NOT_FOUND/BINDING_UNAVAILABLE/TEMPLATE_UNAVAILABLE/INTERRUPTED/
  MECHANICS_FAILED`；仅 VERIFIED 携 verified screen point/record point/learned ROI，前两种 click terminal 携 attempts。
- 验收：region/match 顺序、阈值、去重距离、150/150/1200ms、原子 move+click、verify 次数、坐标空间、stop
  出口逐 696 对照；后续唯一门是 B 释放 shared wire 后把该宏接回 Cloud caller。
- backup=`W-696-NPC-PLAYER-ANCHOR-IMAGE-WHOLE-OBSERVATION-2`；状态=
  `READY_AFTER_A_R3_AND_B_IMAGEPREPROCESSOR_RELEASE`；唯一 New
  `service/npc/NpcClickPlayerAnchorLocalObservationMechanics.java`。只闭合 fresh capture -> baseline default-mask prep ->
  purple wash/blob evidence；Cloud 保留玩家名 OCR、地图公式、candidate 选择与点击，不能把 local-only OCR 当 provider
  parity。backup 与 primary 二选一，不同时占用 A。

### External B primary - white-story template 完整双端真链

- task=`W-696-DIALOG-WHITE-STORY-TEMPLATE-FULL-CHAIN-1`；状态=
  `READY_AFTER_B_CURRENT_20_FILE_CHAIN_AND_C_R2_SOURCE_RELEASE`。
- Cloud Java 写集：New `remote/DialogWhiteStoryTemplateMacroCommand.java`、
  `remote/DialogWhiteStoryTemplateMacroResult.java`、`remote/CloudDialogWhiteStoryTemplatePort.java`；Modify
  `remote/LocalMacroKind.java`、`LocalMacroCommand.java`、`LocalMacroRequest.java`、`LocalMacroOutcome.java`、
  `RemoteCommandOutcomeEnvelope.java`、`RemoteProtocolDigests.java`、`service/DialogService.java`。
- DHXY Java 写集：New `cloud/remote/RemoteDialogWhiteStoryTemplateMacroCommandPayload.java`、
  `RemoteDialogWhiteStoryTemplateMacroResultPayload.java`；Modify `RemoteLocalMacroKind.java`、
  `RemoteLocalMacroCommandPayload.java`、`RemoteLocalMacroResultPayload.java`、`RemoteOperationPayloadCodec.java`、
  `RemoteProtocolDigests.java`、`LocalRemoteGameCommandHandler.java`。C mechanics 只读，不在 B 写集。
- 696 anchors=`DialogService:449,924-999`；reachable chain=`prepareWhiteStoryTemplate* public caller -> Cloud
  DialogService.verifyWhiteStoryTemplate -> typed port/transport -> DHXY handler -> frozen C same-frame mechanics ->
  terminal -> PreparedDialogAction/absent/miss`。
- envelope=`EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN`；typed terminal=`MATCHED/STORY_MISS/STORY_ABSENT/
  CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/MECHANICS_FAILED`。验收 caller-order templates、`0.85` first-hit、same-frame
  PNG/SHA/dimensions/rect、screen-absolute point、strict flat payload/canonical digest 和原 public 返回逐 696 保持。
- backup=`W-696-DIALOG-GREEN-TEMPLATE-OPTION-FULL-CHAIN-1`；状态=
  `ALTERNATIVE_AFTER_B_CURRENT_20_FILE_CHAIN`；沿用同一 shared modify family，改用 Green command/result/port/payload
  新类并只读 frozen green mechanics。primary/backup 共享 wire，必须二选一顺序实施。

### External C primary - Dialog option 单 variant OCR 完整本地观察生命周期

- task=`W-696-DIALOG-OPTION-OCR-WORDS-WHOLE-OBSERVATION-1`；状态=
  `READY_AFTER_A_R3_AND_C_R2_SOURCE_RELEASE`。
- 唯一 Java 写集：New DHXY
  `src/main/java/com/bot/dhxy/service/dialog/DialogOptionOcrWordsLocalObservationMechanics.java`（目标已核不存在）。
- 696 anchors=`DialogService:1792-1895`、`GameTextLineOcrService.readDialogOptionWords:120+`；真实 producer 为现有
  `TextRecognizer.getAllTextResultsLocalOnly(String)`。每次 command 恰携一个 `GREEN/YELLOW` variant 与对应 immutable
  PNG/SHA/dimensions/rect，不能在本地自行请求第二 variant。
- 完整 mechanics：strict evidence 校验 -> window-scoped artifact -> 恰一次 local OCR -> caller-order immutable
  image-local word boxes -> closed terminal；Cloud 保留 green-first、green miss 后才 yellow、alias/fallback/action/click。
- terminal=`WORDS/NO_WORDS/OCR_UNAVAILABLE/INVALID_IMAGE/MECHANICS_FAILED`；WORDS/NO_WORDS 都证明 OCR sidecar 已真实
  响应，provider/sidecar failure 不得伪装 NO_WORDS。验收单 variant/单 OCR、坐标 bounds、temp ownership、defensive
  bytes、异常 closed mapping；后续唯一门是 B shared wire 释放后接 Cloud `DialogService` 原 OCR caller。
- backup=`W-696-DIALOG-OPTION-OCR-FULL-CHAIN-1`；状态=`READY_AFTER_C_PRIMARY_AND_B_SHARED_RELEASE`；把本 primary
  作为只读 mechanics，新增两仓 OCR command/result/port/payload 并修改 shared wire + Cloud `DialogService`，一次闭合
  public caller。该 backup 是后续 shared owner 单，不能与 B primary/backup 并行。

### External D primary - prepared screen-point click/verify 完整连续本地宏

- task=`W-696-NPC-PREPARED-POINT-CLICK-VERIFY-WHOLE-MECHANICS-1`；状态=
  `READY_AFTER_D_YELLOW_TARGET_SOURCE_RELEASE`。
- 唯一 Java 写集：New DHXY
  `src/main/java/com/bot/dhxy/service/npc/NpcClickPreparedPointLocalMacroMechanics.java`（目标已核不存在）；不修改、
  不复制、不拆分 D yellow-target 文件或 candidate closure。
- 696 anchors=`NpcClickService:176-238,1896,2018,3032-3055`；未来 callers 是 Cloud 已完成 candidate/公式/策略后
  产生的 learned/yellow/player-anchor screen point，明确不承接 A 的 tooltip region/template loop。
- 完整 mechanics：exact binding + input-worker authority -> direct atomic move/150ms/click hold 150ms -> caller-specified
  first wait -> exact-window dialog/battle verifier -> 仅按 command 中 696 原有 `0/1` retry 做第二次 move/click/1000ms/
  verify -> terminal；禁止 queue-in-queue、自动 retry 或本地 candidate 决策。
- terminal=`VERIFIED/NOT_VERIFIED/BINDING_UNAVAILABLE/NON_INPUT_WORKER/INTERRUPTED/MECHANICS_FAILED`；仅前两态携
  attempts/last verifier evidence。验收原子输入、等待值、retry 上限、verifier 次数、screen-absolute point、interrupt
  flag 与所有 exit；后续唯一门是 B shared wire 释放后接 Cloud prepared-point callers。
- backup=`W-696-NAVIGATION-X2-CLOSED-MACRO-FULL-CHAIN-1`；状态=
  `READY_AFTER_B_SHARED_RELEASE_AND_X2_QUEUE_BOUNDARY`。必须把 X2 + successful mouse-away + surrounding direct input
  包成一个 closed local macro 后才可发，不得在 exclusive callback 内再排队；前置未满足时零 reservation。

### #19 互斥与发布顺序

| 路 | primary 写集 owner | 与当前/其它 primary 的关系 |
|---|---|---|
| A | New `NpcClickTaskTooltipLocalMacroMechanics.java` | 不碰 A R3、B shared、C R2、D yellow |
| B | White-story Cloud 10 + DHXY 8 | 仅在当前 20 文件释放后独占 shared wire；只读 C mechanics |
| C | New `DialogOptionOcrWordsLocalObservationMechanics.java` | 只读 A evidence/现有 OCR producer；不碰 B wire |
| D | New `NpcClickPreparedPointLocalMacroMechanics.java` | 不改/复用 yellow candidate 写集；不碰 A/C/B files |

- A/C/D 的 create-new targets 均已核为不存在，三者可在各自释放门满足后与 B 后续并行；B 是唯一 shared-wire
  writer。backup 均是非 reservation alternative，只有对应 gate 满足且 parent 选择后才发。
- `TaskTrackerPanelService` caller/context/panel artifact 前置未形成完整真链，本轮不伪标 READY；
  `ClientIdentityService` 继续 `NEEDS_USER_DECISION`；永久本地四 Service 不进入 Cloud。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #18 - D yellow closure 归位后的互斥后继

记录时间：`2026-07-14T20:52:12-04:00`。本段仅提供父级后续 Implementation 排班材料，不作源码裁决。

### Queue #17 C 候选作废与当前冻结

- Queue #17 的 C 候选 `W-696-GAME-TEXT-CANDIDATE-WHOLE-LOCAL-MECHANICS-1` 状态改为
  `SUPERSEDED_BY_D_SCOPE_CLARIFICATION_1`，不再为 C 预留任何文件，也不得改派给 C。
- D 当前 `W-696-NPC-YELLOW-TARGET-WHOLE-LOCAL-OBSERVATION-1` 在唯一文件
  `src/main/java/com/bot/dhxy/service/npc/NpcClickYellowTargetLocalObservationMechanics.java` 内联
  `696a12b0:GameTextLineOcrService.findYellowTextCandidateResult` 的 exact strict-yellow shape closure；该 closure、
  default mask、skip 与排序均归 D 当前单，不再拆 helper/DTO。
- 当前继续冻结：A `DialogOptionOcrImageLocalObservationMechanics.java` R1、B prepared-action validation 双仓
  20 文件、C `DialogWhiteStoryTemplateLocalObservationMechanics.java` R1、D 上述 yellow-target 单文件。
- B 当前独占 shared local-macro enum/codec/digest/handler。故下一波只有 B 能排一条完整双端 caller chain；A/C/D
  排的是各自完整、可逐 `696a12b0` 验收的 local mechanics lifecycle，并明确唯一后续 shared-wire/caller gate，
  不把 mechanics 前置冒充 Service 整类完成。

### A primary - prepared point 点击、重试与验证完整 mechanics

- task=`W-696-NPC-PREPARED-POINT-CLICK-VERIFY-WHOLE-MECHANICS-1`；状态=
  `READY_AFTER_A_OCR_IMAGE_R1_RELEASE`。
- 唯一 Java 写集：create-new DHXY
  `src/main/java/com/bot/dhxy/service/npc/NpcClickPreparedPointLocalMacroMechanics.java`。
- baseline anchors=`NpcClickService.executeMoveClickAndVerify:176-216`、
  `executeClickAndVerifyDirect:218-238`、`clickNpcByPlayerAnchorFormula:3011-3055`。公开 mechanics entry 接收
  Cloud 已选定的 screen-absolute point、first wait、closed verifier mode 与 baseline 0/1 retry budget；本地不计算
  candidate、anchor 公式或策略。
- 完整链=`typed command -> exact binding/input-worker guard -> direct move -> 150ms -> click hold 150ms -> wait ->
  DialogDetectionLocalMechanics/BattleRadarLocalObservationMechanics -> baseline optional retry -> typed terminal`；
  禁止 queue-in-queue，保留 stop/interrupt 与 1000ms retry wait。
- terminal=`VERIFIED/NOT_VERIFIED/BINDING_UNAVAILABLE/NON_INPUT_WORKER/INTERRUPTED/MECHANICS_FAILED`；验收
  move+click 原子性、150/150/1000ms、最多一次 retry、exact binding、attempt/evidence defensive copy。
- 唯一后续门：B 释放 shared wire 后，由 Cloud `NpcClickService` 原 prepared-point 调用点接 closed contract；在此之前
  本单只计完整 local mechanics prerequisite。
- backup=`W-696-NPC-PLAYER-ANCHOR-IMAGE-WHOLE-OBSERVATION-2`，状态=
  `ALTERNATIVE_ONLY_IF_D_PRIMARY_NOT_ISSUED`；唯一文件为
  `service/npc/NpcClickPlayerAnchorLocalObservationMechanics.java`，与 D primary 二选一，不得同时预留。

### B primary - white-story template 完整双端 caller chain

- task=`W-696-DIALOG-WHITE-STORY-TEMPLATE-FULL-CHAIN-1`；状态=
  `READY_AFTER_B_VALIDATION_CHAIN_AND_C_WHITE_STORY_R1_RELEASE`。
- Cloud Java 写集（3 New + 7 Modify）：
  1. `src/main/java/com/bot/dhxy/cloud/remote/DialogWhiteStoryTemplateMacroCommand.java`
  2. `src/main/java/com/bot/dhxy/cloud/remote/DialogWhiteStoryTemplateMacroResult.java`
  3. `src/main/java/com/bot/dhxy/cloud/remote/CloudDialogWhiteStoryTemplatePort.java`
  4. `src/main/java/com/bot/dhxy/cloud/remote/LocalMacroKind.java`
  5. `src/main/java/com/bot/dhxy/cloud/remote/LocalMacroCommand.java`
  6. `src/main/java/com/bot/dhxy/cloud/remote/LocalMacroRequest.java`
  7. `src/main/java/com/bot/dhxy/cloud/remote/LocalMacroOutcome.java`
  8. `src/main/java/com/bot/dhxy/cloud/remote/RemoteCommandOutcomeEnvelope.java`
  9. `src/main/java/com/bot/dhxy/cloud/remote/RemoteProtocolDigests.java`
  10. `src/main/java/com/bot/dhxy/service/DialogService.java`
- DHXY Java 写集（2 New + 6 Modify）：
  1. `src/main/java/com/bot/dhxy/cloud/remote/RemoteDialogWhiteStoryTemplateMacroCommandPayload.java`
  2. `src/main/java/com/bot/dhxy/cloud/remote/RemoteDialogWhiteStoryTemplateMacroResultPayload.java`
  3. `src/main/java/com/bot/dhxy/cloud/remote/RemoteLocalMacroKind.java`
  4. `src/main/java/com/bot/dhxy/cloud/remote/RemoteLocalMacroCommandPayload.java`
  5. `src/main/java/com/bot/dhxy/cloud/remote/RemoteLocalMacroResultPayload.java`
  6. `src/main/java/com/bot/dhxy/cloud/remote/RemoteOperationPayloadCodec.java`
  7. `src/main/java/com/bot/dhxy/cloud/remote/RemoteProtocolDigests.java`
  8. `src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java`
- reachable chain=`prepareWhiteStoryTemplate* public caller -> Cloud DialogService.verifyWhiteStoryTemplate -> typed
  port/contract -> existing remote command path -> DHXY handler -> frozen C mechanics -> typed terminal -> Cloud PreparedDialogAction`。
  baseline anchors=`DialogService:449`、`:924-981`；保留 same-frame evidence、0.85 threshold、caller-order template、
  target/fallback/timestamp 顺序。
- terminal envelope=`EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN`；typed state=
  `MATCHED/STORY_MISS/STORY_ABSENT/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/MECHANICS_FAILED`。验收两仓 closed
  kind、strict codec/canonical digest、flat terminal、PNG/template evidence 与 public caller 真可达。
- backup=`W-696-DIALOG-GREEN-TEMPLATE-OPTION-FULL-CHAIN-1`；状态=
  `ALTERNATIVE_AFTER_B_VALIDATION_CHAIN`。它与 primary 共享同一 transport 文件族，只能顺序二选一，不能并发。

### C primary - 单 variant Dialog option OCR words 完整 observation lifecycle

- task=`W-696-DIALOG-OPTION-OCR-WORDS-WHOLE-OBSERVATION-1`；状态=
  `READY_AFTER_C_WHITE_STORY_R1_AND_A_OCR_IMAGE_R1_RELEASE`。
- 唯一 Java 写集：create-new DHXY
  `src/main/java/com/bot/dhxy/service/dialog/DialogOptionOcrWordsLocalObservationMechanics.java`；只读复用 A frozen
  image evidence，不修改 A 文件或 B shared wire。
- baseline anchors=`DialogService.processOptionsWithOCRDetailed:1792-1895`、
  `GameTextLineOcrService.readDialogOptionWords:120+`。公开 observation entry 每次只接一个 closed `GREEN/YELLOW`
  variant 的 PNG bytes/rect，校验 PNG/SHA/尺寸，写 window-scoped 临时图，调用现有 local OCR 恰一次，并返回
  caller-order image-local immutable word boxes。
- 完整链=`typed image command -> evidence validation -> scoped artifact -> one local OCR pass -> ordered boxes -> typed
  terminal`。Cloud 后续仍保留 green-first、仅 miss 才 yellow、alias/fallback/action/click；本地不得接 target keyword、
  匹配 alias、选择 fallback 或点击。
- terminal=`WORDS/NO_WORDS/OCR_UNAVAILABLE/INVALID_IMAGE/MECHANICS_FAILED`；验收单 variant/单 OCR、坐标边界、
  defensive copy、artifact flush、provider failure 不伪装视觉 miss。
- 唯一后续门：B 释放 shared wire 后接 Cloud `DialogService` 原 OCR 调用点；此前只计完整 local observation
  prerequisite。
- backup=`W-696-TASK-TRACKER-PANEL-CLOUD-WHOLE-CLASS-1`；状态=
  `READY_AFTER_EXPLICIT_TASK_CONTEXT_AND_PANEL_CAPTURE_PORT`；仅允许 Cloud `TaskTrackerPanelService.java` 整类算法
  cohort。当前 caller context/capture producer 未闭合，不预留文件、不提前发单。

### D primary - NPC player-anchor image 完整 observation lifecycle

- task=`W-696-NPC-PLAYER-ANCHOR-IMAGE-WHOLE-OBSERVATION-1`；状态=
  `READY_AFTER_D_YELLOW_TARGET_RELEASE`。
- 唯一 Java 写集：create-new DHXY
  `src/main/java/com/bot/dhxy/service/npc/NpcClickPlayerAnchorLocalObservationMechanics.java`。
- baseline anchors=`NpcClickService.prepareNpcOcrScanImage:2505+`、
  `calculatePlayerAnchorFormulaPoint:2865-2996`、`extractPurpleBlobAnchor:3132+`。公开 observation entry 完整执行
  fresh exact-window capture -> baseline default-mask image prep -> purple wash/foreground cleanup -> connected purple
  blob evidence，并返回 image-local ordered boxes/centers 与 capture origin；不做玩家名 OCR、地图 delta/公式、candidate
  选择、点击或验证。
- terminal=`CAPTURED/NO_PURPLE_CANDIDATE/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/INTERRUPTED/MECHANICS_FAILED`；
  验收单 fresh frame、mask/threshold/component/order byte-for-byte 对照 696、screen origin 明确、全部 owned image flush、
  空候选与 capture failure 分离。
- 唯一后续门：B 释放 shared wire 后由 Cloud `NpcClickService` 的 player-anchor fallback 原调用点接 typed result；此前
  只计完整 local observation prerequisite。
- backup=`W-696-CLIENT-IDENTITY-WHOLE-BOUNDARY-1`；状态=`NEEDS_USER_DECISION`，不预留文件。identity title/name
  parse 留本地还是迁 Cloud 仍有两种合法落点，父级需先向用户列影响后才能发单。

### #18 互斥与发布门

| 路 | primary 唯一写集 | 与当前及其它 primary 的关系 |
|---|---|---|
| A | 新 `NpcClickPreparedPointLocalMacroMechanics.java` | 不碰 A 当前 OCR image、D yellow/player-anchor、B shared、C OCR words |
| B | 上述 Cloud 10 + DHXY 8 shared integration 文件 | 当前 B 20 文件与 C white-story 释放后，下一波唯一 shared-wire owner |
| C | 新 `DialogOptionOcrWordsLocalObservationMechanics.java` | 只读 A evidence；不改 A/B/D 文件 |
| D | 新 `NpcClickPlayerAnchorLocalObservationMechanics.java` | 不碰 D 当前 yellow 单文件或 A prepared-point 文件 |

- A/C/D primary 在各自释放门满足后可并行；B primary 启动后，A/C/D 仍冻结所有 remote enum/codec/digest/handler
  与 Cloud `DialogService`。A backup 与 D primary 冲突，已明确为二选一；B primary/backup 同样二选一。
- 永久本地 `BagService/UICleanerService/GiveItemService/QuestManagerService` 不进入 Cloud；没有新增
  owner/permit/session/ledger/compaction/durable workflow/business TTL/auto retry。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## URGENT READY_NOW A/D - Two Released-Worker Whole Mechanics Cards

- 本段是 Next-Task Queue Helper 的紧急发单建议，不是源码审批结论。当前 B/C 独占 PlayerState
  双仓 shared enum/codec/digest/handler/Service 文件；以下 A/D 各只 create-new 一个完整大类，两张卡
  彼此及与 B/C 零文件交集。

### External A - `W-696-DIALOG-STORY-OBJECTIVE-CAPTURE-WHOLE-MECHANICS-1` - READY_NOW

- exact Java write set：仅 create-new DHXY
  `src/main/java/com/bot/dhxy/service/dialog/DialogStoryObjectiveCaptureLocalMechanics.java`；command/result/state 全为
  该类底部 immutable nested types，不拆 DTO/helper，不改 `DialogService` 或 shared wire/handler。
- `696a12b0` anchors：`DialogService.handleStoryObjective` `:1444-1469`、
  `cropStoryObjectiveImage` `:1472-1478`、`captureCurrentStoryImage` `:2389-2404`、
  `captureCurrentStoryObjectiveSnapshotNoDetect` `:2413-2433`、
  `cropStoryObjectiveFromWindowSnapshotNoDetect` `:2445-2467`、debug ownership `:2470-2484`。
- complete lifecycle：`DETECT_AND_CAPTURE_STORY_OBJECTIVE` 在 `DialogDetectionLocalMechanics` 同一 STORY frame
  上裁小框；`CAPTURE_STORY_OBJECTIVE_NO_DETECT` 一次 exact-window 大框 capture后裁小框；
  `CROP_STORY_OBJECTIVE_FROM_WINDOW_SNAPSHOT` 只从 caller-supplied immutable PNG+窗口绝对原点裁剪，零新 capture。
  返回 PNG bytes/SHA-256/dimensions/screen-absolute `left/top`；本地不识别地图/坐标、不构造业务结果。
- actual producers/caller：现有 `BoundWindowCaptureService.captureRegion` + exact `WindowNativeBinding`、
  `DialogDetectionLocalMechanics.detectDialog`、`ImagePreprocessor.cropAbsoluteRect/cropCopy/saveImage`、
  `WindowScopedTempPath` 全部真实可达；baseline public `DialogService.handle(...) -> handleStoryObjective(...)`
  和 `XiuluoTaskV2 -> cropStoryObjectiveFromWindowSnapshotNoDetect(...)` 是真实 caller。
- terminal：`CAPTURED`仅它携带图像字段；`NOT_STORY/CAPTURE_UNAVAILABLE/CROP_FAILED/
  BINDING_UNAVAILABLE/INTERRUPTED/MECHANICS_FAILED` 的图像字段全显式空。该链零 input，在 queue 外
  exact-context 调用，零 retry/TTL。
- acceptance：对照上述五段逐一核 small-dialog 几何、同帧不重截、latest+history debug 命名、
  PNG SHA/尺寸重算、每个 `BufferedImage` 唯一 flush owner 和 exhaustive terminal；不增减 capture/crop/save 次数。
- dependency/risk/唯一后续 gate：本轮代码可独立完整实现；B/C 释放 shared wire 后，另一单仅增
  `DIALOG_STORY_OBJECTIVE_CAPTURE` 两仓 mirror + Cloud `DialogService` 原调用点消费 terminal。本单不预占
  gate，不伪称 same-path 整类已完成。
- backup：`W-696-AUTO-COMBAT-PANEL-VISIBILITY-ALIGN-WHOLE-MECHANICS-1` /
  `READY_NOW_LOCAL_MECHANICS_PREREQUISITE`；唯一写集 create-new
  `service/autocombat/AutoCombatPanelVisibilityLocalMacroMechanics.java`，只覆盖 baseline
  `AutoCombatPanelService:69-157,269-320` 的 find→Alt+8一次→re-find→必要 drag→re-find；不读 rounds、
  不碰已知 rounds contract blocker，Cloud 继续拥有 refresh/state 判断。

### External D - `W-696-DIALOG-GREEN-TEMPLATE-OPTION-WHOLE-MECHANICS-1` - READY_NOW

- exact Java write set：仅 create-new DHXY
  `src/main/java/com/bot/dhxy/service/dialog/DialogGreenTemplateOptionLocalMacroMechanics.java`；以一个完整大类内嵌
  immutable intent/spec/result/state，不改 `DialogService`/`ImagePreprocessor`/shared wire/handler。
- `696a12b0` anchors：`DialogService.handleGreenTemplateOption` `:2153-2165`、
  `prepareGreenTemplateOption` `:2167-2281`、`handleGreenTemplateOptionDirect` `:2283-2378`。必须保留
  caller-supplied spec 顺序、optional OPTION verify、一帧 capture、一次 dialog-option wash、每模板 `0.85`
  顺序首命中、caller-supplied X/Y random offset、screen-absolute click 及 `150ms` 的原顺序。
- complete lifecycle：`MATCH_ONLY`在 exact-bound 帧上返回首命中 spec/模板/absolute+dialog-relative point+
  washed PNG/SHA，零 input；`MATCH_AND_CLICK`复用同一 capture/wash/match，在已持有的单一 input-worker
  callback 内 direct `InputProvider.clickLeft(...,150)`，禁止 queue-in-queue。本地只执行 Cloud caller 给定的
  ordered specs，不决定业务优先级/对话 fallback/是否 GiveItem。
- actual producers/caller：`DialogDetectionLocalMechanics` 提供 optional OPTION type+同帧 PNG/absolute rect；
  `BoundWindowCaptureService`、`ImagePreprocessor.washDialogOptionTemplateTextToBlackAndWhite`、
  `ImageFinder.find(BufferedImage,BufferedImage,threshold)`、`CoordinateHelper.resolveMatchedPointInRect/
  getRandomizedPoint`、`InputProvider` 均已存在。baseline public preparation callers 为
  `XiuluoTaskV2/WubeiDialogPreparationProvider/XiuluoDialogPreparationProvider -> DialogService.prepareGreenTemplateOption`，
  direct caller 为 `DialogService.handleDialog -> handleGreenTemplateOption`。
- terminal：`MATCHED/CLICKED`仅携带 matched spec/template/coordinates/PNG evidence；
  `NOT_OPTION/NOT_FOUND/CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/BINDING_UNAVAILABLE/INTERRUPTED/
  NON_INPUT_WORKER/MECHANICS_FAILED` 不携带虚假点位。不对 miss/failure 自动换 spec 顺序、重截或重点。
- acceptance：对照 `:2153-2378` 确认 verify=false 零多余 detection，verify=true 复用 detection frame；每个
  template 在评估点加载并在同迭代 flush；首命中 short-circuit、threshold/offset/random/click delay/日志证据
  不变；所有 image/template 唯一 owner，零 owner/permit/session/ledger/TTL/retry。
- dependency/risk/唯一后续 gate：本轮完整类仅依赖现有本地 producer，可直接实现。B/C 释放
  shared wire 后，另一单增 `DIALOG_GREEN_TEMPLATE_OPTION` closed command/result 两仓 mirror，Cloud
  `DialogService` 仍传入原 ordered specs 并消费 terminal。`MATCH_AND_CLICK` 必须由 handler 单次 remote-exclusive
  callback 调用；`MATCH_ONLY` 在 queue 外调用。
- backup：`W-696-NPC-PLAYER-ANCHOR-FORMULA-WHOLE-MECHANICS-1` /
  `READY_AFTER_OCR_PROVIDER_PARITY`；唯一写集 create-new
  `service/npc/NpcClickPlayerAnchorFormulaLocalMacroMechanics.java`，对应 baseline
  `NpcClickService:998-1049,2865-3049,3106-3188` + `LocationVisionService:278-344`。当前仅有
  `TextRecognizer.getAllTextResultsLocalOnly`，baseline `getAllTextResultsForMatch` 的 provider fallback 未闭合；未明确该机械
  provider 替换前不标 READY_NOW，不得用紫色 blob-only 偷换原 OCR→blob fallback 顺序。

### Mutual Exclusion / Known Blockers

- A 仅创建 `DialogStoryObjectiveCaptureLocalMechanics.java`；D 仅创建
  `DialogGreenTemplateOptionLocalMacroMechanics.java`；彼此零交集，且不触碰 B 的 DHXY PlayerState 9 Java、
  C 的 Cloud PlayerState 10 Java、D 刚释放的 Cloud TaskMaintenance/SummonSkill、A 已释放 NPC mechanics。
- TaskTracker caller-context、Navigation X2 shared-handler、ClientIdentity placement、TeamReturn precheck 时序、
  AutoCombatPanel rounds contract 均明确排除；A backup 仅做 panel visibility/align，不把 rounds 伪写为 READY。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #13 - Post-Current-Wave Whole Reachable Cohorts

- preparedAt: `2026-07-14T18:10:00-04:00`；本段仅是 Next-Task Queue Helper 的排班建议，不作源码裁决。
- true-EOF reservations checked: A=`DHXY service/npc/NpcClickCtrlProbeLocalMacroMechanics.java`；
  B=`DIALOG_DETECTION` 18-file Cloud/DHXY cohort；C=`Cloud DialogChoiceMemoryService.java +
  WorldMapRouteResultMemoryService.java + MemoryService.java + host/CloudServiceConfiguration.java`；
  D=`Cloud service/AutoCombatService.java`。以下四个首选写集与这些占用及彼此均零交集。
- baseline authority: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`；永久本地
  `BagService/UICleanerService/GiveItemService/QuestManagerService` 均不进入任何 Cloud 写集。

### External A Next - TaskMaintenance To Summon Whole-Pass Reachable Chain

- task id/status: `W-696-TASK-MAINTENANCE-SUMMON-WHOLE-PASS-CHAIN-2` / `READY_AFTER_A_CURRENT_RELEASE`。
- exact Java write set (Cloud 2 Modify):
  `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`（baseline blob
  `e93cfd01d9c282f98881a6311b8bb806bbc3e359`）；
  `src/main/java/com/bot/dhxy/service/SummonSkillService.java`（baseline blob
  `d8afb9e2f97aba9522393bd9a21d0cc4c48ed324`）。现有 remote capability/handler 全部只读。
- 696 entry/whole lifecycle: `TaskMaintenanceService.runOpportunisticMaintenance(...)` 约 `579-805`，尤其
  round/capability/claim gates、`buildSummonSkillCleanupRequest`、`:756 cleanSummonSkillsOnce` 与 finally 中
  cache/cooldown/unknown-backoff/state；`SummonSkillService.cleanSummonSkillsOnce*` 约 `153-213` 及完整 pass
  result 构造。不得缩成一次 boolean helper。
- reachable caller: `AutoBattleTask:208` 已经由 `BaseTaskTemplate:61`、`TaskStepExecutor:40` 和
  `AutoBattleTask:113` 的真实 `TaskExecutionContextHolder.callWith(...)` 覆盖，随后保持
  `AutoBattleTask -> TaskMaintenanceService -> SummonSkillService` 原调用图。
- typed local boundary: 使用现有实际 producer
  `TaskExecutionContext.getRemoteGameClient().summonSkillWholePass().execute(WholePassIntent)`；四 intent 字段
  `expectedSkillCount/trustExpectedSkillCount/startSlotIndex/skipUltimateCornerCheck` 与九字段 cleanup value
  一一映回 baseline model。不得修改/扩展现有 retained/exclusive/ledger 实现，也不得新增 retry。
- terminal/acceptance: `Executed` 才允许进入原 success/state-update 分支；`NotExecuted` 保持未成功且不写
  clean timestamp；`Stopped` 沿现有 checkpoint/stop unwind；`Unknown` 保持未决、零自动重发且不得伪成
  普通失败后触发新 pass。逐项对照 team claim、window cache、ultimate-success-before-failure、finally 恢复
  `GameContext.ActionState`、日志和 delay 顺序；完整 public caller 到 typed terminal 可达后才算该 cohort 完整。
- dependencies/risk: 只等 A 当前单释放；B/C/D 当前文件不是依赖。若现有 whole-pass capability 被父级判定为
  dormant 而非生产 producer，本任务须降为 `READY_AFTER_CAPABILITY_ACTIVATION`，不能回退本地输入类。
- backup: `W-696-TASKTRACKER-WHOLE-CLOUD-ALGORITHM-2 / READY_AFTER_TASK_CALLER_AND_PANEL_MECHANICS`，
  写集仅 Cloud `TaskTrackerPanelService.java` + new `CloudTaskTrackerPanelMechanicsPort.java`；详见 D backup 的
  context/same-frame 前置，不得用 dormant `TASK_TRACKER_READ` 冒充 producer。

### External B Next - TeamReturn Whole Typed-Fact Closure

- task id/status: `W-696-TEAMRETURN-WHOLE-TYPED-FACT-CLOSURE-2` / `READY_AFTER_B_CURRENT_RELEASE`。
- exact Java write set (Cloud 1 complete large Service Modify):
  `src/main/java/com/bot/dhxy/service/TeamReturnService.java`，baseline blob
  `286c5a85f01d010e883f8c4321ea1793776c932f`。虽为单文件，但覆盖该 650 行 Service 的全部 member click、
  leader wait、async precheck/consume、scope fence、timestamps/logging 生命周期，不是单方法壳。
- 696 entry: `clickReturnTeamIfPresent:65-100`、`waitForMembersReturnIfNeeded:102-137`、
  `beginLeaderSignalPrecheck:151-173`、`consumeLeaderSignalPrecheck:180-224` 及其全部 private finder/scope/log
  helpers。member 可达 caller 为 `AutoBattleTask:196,255`；leader caller 基线为
  `WubeiTask:1787,3868`、`XiuluoTaskV2:2523,2860`，后两者未进 active Cloud 时只完成 Service 边界，
  不虚称 task caller 已迁入。
- typed local boundary/real producer: 仅消费已实际接线的 `TEAM_RETURN_BUTTON` 与
  `TEAM_RETURN_LEADER_SIGNAL` facts（DHXY handler `LocalRemoteGameCommandHandler:803-812` 调用 exact-binding
  local mechanics），member click 继续用原 `click+500ms` ordered `InputBundle`。删除/替换 Service 内残留
  tracker/ImageFinder/capture/OCR authority与二次诊断 capture，不改变业务判断。
- behavior/terminal: member 保持检测 -> `ensureSheYaoXiangActive` -> 再检测 -> `+-3` 随机点 -> click ->
  found/click timestamps；leader 保持 initial read、120s wall-clock、3s poll、消失即继续；precheck 保持
  async begin、exact window/taskRun scope consume 和 inconclusive 分支。live read 的
  `OBSERVED+PRESENT/other OBSERVED/NOT_EXECUTED/STOPPED/UNKNOWN` 必须逐支沿当前 committed mapping，禁止把
  unresolved terminal 变成新 retry。
- acceptance: 全 public/private inventory 一一有去向；active member caller 真可达；两 fact producer 均在
  DHXY handler 实际分支而非 enum-only；Cloud Service 不再 import HWND/capture/image/input implementation；
  timestamps、throttled logs、wait/poll/delay 与 state 无漂移。
- dependency/risk: `ensureSheYaoXiangActive` 仍受 PlayerState typed boundary 进度约束，但不得把该业务判断移回
  DHXY。未迁入 Wubei/Xiuluo task caller 前，leader precheck 只能标 Service-ready，不能计整条 task 完成。
- backup: `W-696-NAVIGATION-WHOLE-ACTION-SITE-EXTRACTION-2 / READY_AFTER_NAV_FACT_CONTRACT`，写集 Cloud
  `NavigationService.java` + new `CloudNavigationMechanicsPort.java`；必须把 60s loop/candidate/keep-turn/
  watcher/state 留 Cloud，只在原 action sites 接 typed coordinate/pathing facts 与 ordered bundles，严禁复用
  `NAVIGATE_IN_CURRENT_MAP` whole-Service local macro 下沉业务循环。

### External C Next - PlayerState Whole Cloud Business Integration

- task id/status: `W-696-PLAYERSTATE-WHOLE-CLOUD-BUSINESS-CHAIN-2` /
  `READY_AFTER_PLAYERSTATE_OBSERVATION_CONTRACT`；当前不是立即发单项。
- exact Java write set (Cloud 1 Modify + 1 New):
  `src/main/java/com/bot/dhxy/service/PlayerStateService.java`（baseline blob
  `096d8917b0372422b3ed141300419f9b71c1392c`）；new
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudPlayerStateLocalMechanicsPort.java`。C 当前四个 Memory
  文件、B shared wire和 DHXY mechanics 全只读。
- 696 entry/whole lifecycle: `syncAll:191-199`、startup/first-aid `217-505`、incense
  `506-678`、health/supply snapshot `679-999`、incense observation/cache `1002-1330` 与全部 state records。
  caller 为 D 释放后的 `AutoCombatService`、`AutoBattleTask`、`TeamReturnService` 与
  `TaskMaintenanceService`；“是否治疗/是否吃香”、阈值、20-minute decision、cache/state update 均留 Cloud。
- typed boundary: 未来 contract 必须调用已落 DHXY
  `PlayerStateFirstAidLocalMechanics`/`PlayerStateIncenseStatusLocalObservationMechanics`，返回 closed snapshot/
  observation terminal；使用香物理动作复用现有 `CloudBagUseIncensePort`，永久本地 `BagService` 不上云。
  Cloud port 只做 exhaustive mapping，不产生 TTL/retry/业务选择。
- terminal/acceptance: first-aid 保持 per-target capture 后紧邻原输入、healthy/unhealthy/unknown 区分；incense
  保持 cached `PRESENT/UNKNOWN` 早退、仅 `ABSENT` full fallback、remaining `>20min` 不使用、成功后才写
  last-used/cache。`CAPTURE_UNAVAILABLE/OCR_UNAVAILABLE/MECHANICS_FAILED/STOPPED/UNKNOWN` 不可合并为
  “需要吃香”或自动重读。所有 public overload/private graph、delay、Bag callback 时点与 state mutation 对照 696。
- dependencies/risk: 当前 `WindowFactKind` 没有 PlayerState observation，现有 local mechanics 也尚无 producer
  wire；父级须先在 B 释放后安排独立 closed contract，且不得激活旧 PlayerState owner/session/ledger。另有
  `ClientIdentityService` 落点歧义，`syncMyIdentity` 的最终闭合必须等待用户选择，不能在本单擅自决定。
- backup: `W-696-NPC-CLICK-WHOLE-CLOUD-CALLER-2 / READY_AFTER_A_CTRL_WIRE_AND_OTHER_NPC_MECHANICS`，写集
  Cloud `NpcClickService.java` + new `CloudNpcClickLocalMechanicsPort.java`；只有 Ctrl、yellow-name、tooltip、
  learned-point、formula 与 verify 的 producer 全部真实落地后才能实施，candidate order/offset/delay/fallback
  全留 Cloud，不能仅接 A 的 Ctrl helper 就宣称整类完成。

### External D Next - AutoCombatPanel Whole Cloud Boundary

- task id/status: `W-696-AUTOCOMBAT-PANEL-WHOLE-CLOUD-BOUNDARY-2` /
  `READY_AFTER_D_RELEASE_AND_ROUNDS_OBSERVATION`；当前不是立即发单项。
- exact Java write set (Cloud 1 Modify + 1 New):
  `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`（baseline blob
  `bf63d2c78873afd8a0781d97f080a59b2b327942`）；new
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudAutoCombatPanelPort.java`。D 当前
  `AutoCombatService.java` 只读，B shared wire/C Memory/A NPC mechanics 均不触碰。
- 696 entry/whole lifecycle: `verifyAndAlignPanel*`、`ensurePanelVisible`、panel locate/drag、
  `resolveRoundsRefreshReason`、round refresh、missing/runtime state、`recordCombatExit` 与
  `TeamRefreshDueBurstGuard` 全 public/private graph；reachable caller 为 D 当前
  `AutoCombatService:267-275,628-727`，待 D 释放后保持原调用签名与顺序。
- typed boundary: 现有实际 `AUTO_COMBAT_PANEL` fact 仅可承担 panel visibility/anchor，ordinary Alt+C、drag、
  refresh 继续用 ordered `InputBundle`；round digits 必须先有 exact-window same-frame typed OCR/metrics
  observation producer。Cloud 保留 refresh-reason、low/unknown/due、30s team fairness、missing/cache 决策。
- terminal/acceptance: visibility fact、round observation与每个 input bundle 分别 exhaustive 处理
  `OBSERVED/EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN`；不得用 visibility 推断 rounds，不得在 unknown 时 refresh，
  不得新增 retry/TTL。逐行核对开 panel wait、drag offsets、round red wash/OCR结果、refresh click顺序、
  last-refresh/missing state与 caller fallback。
- dependencies/risk: 当前 fact 没有 rounds PNG/OCR/metrics，因此在 producer 落地前必须保持
  `READY_AFTER...`；只修改 Service/port 不能伪闭合整类。D 当前 `AutoCombatService` 必须先释放并保持只读。
- backup: `W-696-CLIENT-IDENTITY-WHOLE-BOUNDARY-1 / NEEDS_USER_DECISION`，候选写集 Cloud
  `ClientIdentityService.java` + new `CloudClientIdentityPort.java`；现有 `BINDING` fact 确有 title producer，
  但“三级本地 title fallback 留 DHXY”与“Cloud 解析并写 PlayerCharacter/identity epoch”均可行，必须由用户先选，
  helper 不替用户决定。

### Queue #13 Dispatch Summary

- immediate after current-file release: A `TASK-MAINTENANCE-SUMMON-WHOLE-PASS-CHAIN-2`；B
  `TEAMRETURN-WHOLE-TYPED-FACT-CLOSURE-2`。两者现有 typed producer 均已在实际调用/handler 分支可达。
- gated, never fake READY: C 等 PlayerState observation contract + identity decision；D 等 rounds observation +
  current AutoCombat release。TaskTracker、Navigation、NpcClick 仅作带明确 producer 前置的 backups。
- four primary write sets: A=`TaskMaintenanceService+SummonSkillService`；B=`TeamReturnService`；
  C=`PlayerStateService+CloudPlayerStateLocalMechanicsPort`；D=`AutoCombatPanelService+CloudAutoCombatPanelPort`；
  彼此及当前 A/B/C/D reservation 零交集。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Queue Repair #12 - Cloud Business Ownership Restored

- preparedAt: `2026-07-14T17:42:00-04:00`；本段只提供可执行排班 brief，不是源码审查结论。
- supersession note: 父级已否决上方 #11A/#11C 的本地 Service lifecycle 方案；历史不删除。本段明确恢复
  “Cloud 持有 Service 业务判断，DHXY 只持有 closed capture/template/OCR/input mechanics”边界。
- source read: 已全量读取 `696a12b0` 的 `NpcClickService`（3375 行）、`TaskTrackerPanelService`（1643 行）、
  `SummonSkillService`（1037 行），当前双仓对应源码、TaskTracker/SummonSkill typed contract/handler，以及
  3131 行迁移矩阵和 311 行 whole-service 计划。当前 B 独占 Dialog 18 files；D 当前 Navigation 两文件只在
  本段 D 候选开始前由父级确认释放。

### External A - NPC Ctrl-Probe Closed Local Mechanics Prerequisite

- task id/status: `W-696-NPC-CTRL-PROBE-CLOSED-MECHANICS-2` / `READY_IMPLEMENTATION_PREREQUISITE`。
- unique Java write set (1, DHXY only): Modify
  `src/main/java/com/bot/dhxy/service/npc/NpcClickCtrlProbeLocalMacroMechanics.java`。
  `DHXY NpcClickService.java`、Cloud `NpcClickService.java`、B shared wire/handler 一律冻结。
- baseline anchors: `696a12b0:NpcClickService.java` 的 `clickNpcByCtrlMenuScan(...)` 连续 local callback
  （约 `303-460`）及 `buildCtrlMenuScanRect(...)`（约 `462`）。本 mechanics 只执行 Cloud 已选定的**一个**
  probe command，不建立/排序 origin、offset ring、candidate 或 fallback，不决定下一次 probe。
- closed command/result: 在同一 mechanics 文件内形成 immutable `CtrlProbeCommand`（screen-absolute test point
  与 scan rect、targetKeyword、closed verifier operation、immutable dialog template list）和
  `CtrlProbeResult`（terminal、screen-absolute click/scan coordinates、diagnostic reason）。构造器必须拒绝
  空 keyword、倒置/空 rect、部分 verifier payload；不得携带 Service、callback、Function/Supplier 或 Cloud 对象。
- complete local flow: exact binding 门 -> before frame -> Ctrl down -> 80ms -> move -> 280ms -> after frame ->
  `ImageFinder.isMatch(...,0.05)` change gate -> capture-to-file -> yellow wash -> 已批准
  `TextRecognizer.getAllTextResultsLocalOnly(path)` -> baseline fuzzy/tag match -> first hit move -> 100ms -> click ->
  800ms verify -> 最多一次 baseline retry/1000ms -> dialog 或 battle local verify -> every exit `finally` Ctrl up +
  100ms。必须运行在唯一 input worker，零 queue-in-queue，capture/click/verify 全程同一 exact binding。
- terminal matrix: `FOUND` 仅代表 click 后 verifier 成功；`NOT_FOUND` 只代表无 change/无 OCR match/verify miss；
  `OCR_UNAVAILABLE` 不伪成视觉 miss；`BINDING_UNAVAILABLE` 在输入前或 exact capture 失败；`INTERRUPTED` 保留
  stop/interrupt；`MECHANICS_FAILED` 表达本地异常。后四类均不在本地挑下一个 candidate，也不自动重发。
- acceptance: 当前不存在的 `getAllTextResultsForMatch(...)` 必须彻底消失，改用 local-only Optional terminal；
  baseline `80/280/100/800/1000ms`、`0.05`、点击 hold、verify 次数与 Ctrl finally 一项不变；全文件不得新增
  origin list/ring/fallback/business cache。交付是完整 mechanics prerequisite，不计整类迁云完成。
- future single wiring gate: B 释放 shared wire 后，由另单增加 `NPC_CTRL_PROBE` closed command/result mirror，
  Cloud `NpcClickService` 保留完整 candidate loop并逐 probe 调用该 mechanics；本单不预占这些文件。
- risk/dependency: 只读依赖 C 已落 `TextRecognizer/OcrWordResult`、现有 `DialogDetectionLocalMechanics` 与
  `BattleRadarService` local verifier；若 verifier API 与当前源码不符，只在本报告报最小缺口，不改其文件。
- backup: `W-696-NPC-YELLOW-TARGET-CLOSED-OBSERVATION-MECHANICS-1` /
  `READY_IMPLEMENTATION_PREREQUISITE`；create-only
  `src/main/java/com/bot/dhxy/service/npc/NpcClickYellowTargetLocalObservationMechanics.java`，完整执行 caller-supplied
  screen rect 的 Alt+4/capture/wash/local OCR 并返回全部 typed word/shape observations；不点击、不排序候选、不改
  DHXY Service，未来由 Cloud yellow-target strategy 决定匹配与 fallback。

### External C - Whole Cloud TaskTrackerPanelService Algorithm Cohort

- task id/status: `W-696-TASK-TRACKER-PANEL-WHOLE-CLOUD-ALGORITHM-1` /
  `READY_AFTER_B_TASK_TRACKER_MECHANICS_CONTRACT`；不能在当前 B reservation 内伪报可运行。
- unique Java write set (2, Cloud only, after prerequisite):
  1. Modify `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java`.
  2. Create `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskTrackerPanelMechanicsPort.java`.
  不改任何 DHXY Service、Task、runner/host/test、generic wire/handler，也不启用 dormant artifact owner/ledger。
- why not READY now: 当前 `RemoteOperation.TASK_TRACKER_READ` 虽有 request/outcome DTO，但
  `LocalRemoteGameCommandHandler` 在 command admission、owned execution 和 switch 三处明确返回
  “dormant/unsupported/cannot execute”；`CloudTaskRetainedActionState` 也明确无 handle。现有
  `TASK_TRACKER_PANEL_RECT` fact 只有 rect，不能保证 expanded-search/necessary-drag 后的**同一帧** panel bytes，
  也没有 baseline OCR word terminal。因此现有 producer 不足，禁止用 rect+后续 capture 冒充同帧结果。
- minimum prerequisite after B release: 一个单独、直接实现的
  `TASK_TRACKER_PANEL_MECHANICS` closed local-macro cohort，复用已批准
  `TaskTrackerPanelCaptureLocalMechanics`，只提供两种 mechanical operation：
  `LOCATE_DRAG_CAPTURE_PANEL` 返回 exact binding 下 narrow -> expanded -> necessary drag -> same-frame PNG、
  screen-absolute origin/size/SHA；`OCR_REGION` 返回 caller-selected image/rect 的 ordered local OCR word boxes。
  可返回各 template 的 raw id/score/point，但不得在 DHXY 选择 task/classification/link。该 prerequisite 才修改
  B 释放后的 sealed macro contract、DHXY mirror/codec/handler；不新增 owner/session/ledger/TTL/retry。
- baseline algorithm inventory retained in Cloud: 全部 public API
  `findWuhuanNextGreenClickPoint/prepareWuhuanPathingLink/readWubeiTrackerPanel/readXiuluoTrackerPanel/
  findXiuluoTrackerGreenClickPoint/read*FromSnapshot/resolveXiuluoTrackerGreenClickPoint/
  prepareWubeiChainedTrackerFastAction/verifyWubeiChainedTrackerFastAction`，以及约 `225-1568` 的 title/detail
  crop、绿链分割、glyph/segment、fingerprint/cache、candidate 排序、五倍/修罗分类、click-point 与结果构造。
- implementation: live paths 用新 port 获取 panel frame/origin 和按需 OCR words；移除 Cloud 对
  `GameClientTracker/InputSequences/TextRecognizer` 的 authority，保留 `ImagePreprocessor` 纯 CPU 算法和 replay
  APIs。Cloud 决定 title/template candidate、detail rect、link segmentation、map text interpretation、cache hit、
  action/result；DHXY 只执行 panel locate/drag/capture/OCR，不返回业务 classification。
- public reachability: 当前 active Cloud 尚无 `FiveRingTaskV2/WubeiTask/XiuluoTaskV2` whole caller，因此本单
  只闭合整类 public Service surface，不宣称 task runtime 已可达；未来 Task whole-class promotion 必须原签名调用
  这些 public APIs。不得为制造“可达”而修改 runner/host或复制本地 Task 片段。
- terminal matrix: `CAPTURED` 必须有 non-empty PNG、matching SHA/dimensions、screen-absolute origin；
  `PANEL_NOT_FOUND` 保持 baseline empty/not-found；`CAPTURE_UNAVAILABLE`、`OCR_UNAVAILABLE`、`INTERRUPTED`、
  `MECHANICS_FAILED` 分开映射，不得生成 cache hit/link/result，也不得自动 retry。transport
  `NOT_EXECUTED/STOPPED/UNKNOWN` 无 typed payload，UNKNOWN 不重发。
- acceptance: 对照 1643 行 baseline 做 public/private inventory；除 live mechanical seams 外，method graph、
  thresholds、template order、green split/glyph rules、fingerprint distances、candidate sort、cache mutation、
  replay behavior/result fields逐项相同；Cloud 中零 HWND/capture/input/OCR authority，DHXY 中零分类/排序/cache。
- risk/dependency: prerequisite contract 必须先由父级在 B 释放后单独发单并稳定；若只落 panel PNG 而无按需
  OCR terminal，本整类仍不可发单。现有 dormant TASK_TRACKER_READ 不得被顺手激活。
- backup: `W-696-TASK-TRACKER-PANEL-MECHANICS-CONTRACT-1` / `READY_AFTER_B`；直接实施上述最小双仓
  closed macro（contract/mirror/codec/handler + approved mechanics），验收止于 typed panel/OCR terminal，绝不把
  title/classification/fingerprint/cache 下沉 DHXY；完成后立即解锁本 C 整类任务。

### External D - TaskMaintenance To SummonSkill Whole-Pass Production Chain

- task id/status: `W-696-TASK-MAINTENANCE-SUMMON-WHOLE-PASS-CHAIN-1` / `READY_IMPLEMENTATION`；D 的
  Navigation 两文件经父级释放后可立即发单。
- unique Java write set (2, Cloud only):
  1. Modify `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`.
  2. Modify `src/main/java/com/bot/dhxy/service/SummonSkillService.java`.
  `CloudSummonSkillWholePassCapability`、全部 remote DTO/authority、DHXY handler/Service 均只读；不碰 B/A/C 写集。
- existing producer proof: `TaskExecutionContext.getRemoteGameClient().summonSkillWholePass()` 已公开专用 capability；
  `LocalRemoteGameCommandHandler.executeSummonSkillWholePass(...)` 已真实处理 command，并调用本地完整
  SummonSkill mechanics，返回 `EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN` 与完整 cleanup value；不是 dormant wire。
- baseline anchors/reachable chain: `696a12b0:TaskMaintenanceService` 的 summon queue/claim/cooldown 主链，当前
  约 `700-880`：public maintenance caller -> fairness/team-window gates -> build four-field
  `SummonSkillCleanupRequest` -> `SummonSkillService.cleanSummonSkillsOnce` -> cleanup result -> cache/cooldown/state/
  final TaskMaintenanceResult。A 已落 `AutoBattleTask` whole context producer可达该链。
- implementation: 为 `SummonSkillService` 增加/改成 explicit `TaskExecutionContext + SummonSkillCleanupRequest`
  production entry，将 expected count/trust/start index/skip ultimate 四字段逐值映为 `WholePassIntent`，单次调用
  capability并逐字段还原 `SummonSkillCleanupResult`；`TaskMaintenanceService` 在原调用点传已有 exact context。
  queue claim、team gates、deadline/cooldown、unknown backoff、state mutation、日志和 finally action-state恢复仍在 Cloud，
  顺序不变。不得通过 holder default/epoch=0，也不得修改本地 mechanics。
- terminal matrix: `Executed(CleanupValue)` 无损映射 success/count/next index/slot-status map/ultimate flags/
  inspected/deleted/message；`NotExecuted` 映未尝试失败且不写 success cache；`Stopped` 走现有 task stop/interrupt
  路径；`Unknown` 保留不确定失败、不得自动重发，随后只允许 baseline TaskMaintenance 已有 unknown-backoff 处理；
  InterruptedException 恢复 interrupt并走同一 stop路径。
- acceptance: 从 public maintenance entry 到 typed local terminal 全链可达；四 intent 字段和九 cleanup 字段
  one-to-one；claims/capability window/previous action state/cache update/unknown backoff 的先后与 696 相同；零新
  retry/TTL/owner/session/ledger，零 remote/handler 改动。本 cohort只宣称 production cleanup chain完成，不把
  debug-only open-panel/count APIs冒充整类进度。
- dependency/risk: B 同时编辑 handler，但 D 不触该文件；父级发单时冻结 B 对
  `SUMMON_SKILL_WHOLE_PASS` 既有分支，统一验收需等 B handler写入稳定。若 capability 无法由 explicit context获得，
  只报该真实前置，不回退 holder default或改 remote authority。
- backup: `W-696-AUTO-COMBAT-PANEL-WHOLE-CLOUD-BOUNDARY-1` /
  `READY_AFTER_B_AUTO_PANEL_ROUNDS_OBSERVATION`；write set 为 Cloud
  `service/AutoCombatPanelService.java` + create `remote/CloudAutoCombatPanelPort.java`。现有
  `AUTO_COMBAT_PANEL` fact足够 visibility/anchor，InputBundle 足够 Alt+8/drag，但 rounds capture/OCR 与 metrics terminal
  仍缺；必须等 B 释放 shared wire后先补 closed rounds observation，不能在 DHXY 保留 refresh decision。

### Repair #12 Mutual Exclusion And Counting

- A 仅 DHXY Ctrl mechanics；C 仅两个 Cloud TaskTracker files且须等待 prerequisite；D 仅两个 Cloud
  TaskMaintenance/SummonSkill files。三者与 B Dialog 18 files、当前 D Navigation files及彼此均无 Java 写集交集。
- A 是完整 local mechanics prerequisite、C 是整类 Cloud algorithm cohort、D 是完整 production caller chain；
  均不是 DTO/helper/单方法壳，也都不因计划或 prerequisite单独增加整类迁移计数。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #10B - Dialog Detection Closed Local-Macro Whole Chain

- preparedAt: `2026-07-14T17:08:00-04:00`；角色仅为 Next-Task Queue Helper，本节不是 reviewer 的
  `APPROVED/BLOCKED` 结论，也未向 External B 发单。
- task id/status: `W-696-DIALOG-DETECTION-CLOSED-MACRO-CHAIN-1` /
  `CANDIDATE_READY_AFTER_B_R2_PARENT_RELEASE`。
- target: 复用 B R2 已交付的 exact-window `DialogDetectionLocalMechanics`，一次闭合
  Cloud `DialogService` public detection caller -> typed `LOCAL_MACRO/DIALOG_DETECTION` -> DHXY exact
  binding/capture/必要 Alt+4 mechanics -> typed terminal -> Cloud `DialogDetection` 的真实双仓链；不把
  HWND/capture/OpenCV/input authority 搬到 Cloud，不新增 owner/session/ledger/TTL/retry。

### Exact Write Set (18 Java Files)

Cloud create-new (3):
1. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/DialogDetectionMacroCommand.java`
2. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/DialogDetectionMacroResult.java`
3. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogDetectionPort.java`

Cloud modify (7):
4. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroKind.java`
5. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroCommand.java`
6. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroRequest.java`
7. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroOutcome.java`
8. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteCommandOutcomeEnvelope.java`
9. `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteProtocolDigests.java`
10. `src/main/java/com/bot/dhxy/service/DialogService.java`

DHXY create-new (2):
11. `src/main/java/com/bot/dhxy/cloud/remote/RemoteDialogDetectionMacroCommandPayload.java`
12. `src/main/java/com/bot/dhxy/cloud/remote/RemoteDialogDetectionMacroResultPayload.java`

DHXY modify (6):
13. `src/main/java/com/bot/dhxy/cloud/remote/RemoteLocalMacroKind.java`
14. `src/main/java/com/bot/dhxy/cloud/remote/RemoteLocalMacroCommandPayload.java`
15. `src/main/java/com/bot/dhxy/cloud/remote/RemoteLocalMacroResultPayload.java`
16. `src/main/java/com/bot/dhxy/cloud/remote/RemoteOperationPayloadCodec.java`
17. `src/main/java/com/bot/dhxy/cloud/remote/RemoteProtocolDigests.java`
18. `src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java`

Read-only prerequisite, explicitly outside the write set: DHXY
`src/main/java/com/bot/dhxy/service/dialog/DialogDetectionLocalMechanics.java` after B R2 parent release. The
generic broker/gate/transport and `RemoteCommandEnvelope` remain unchanged because the existing `LocalMacroRequest`
and `RemoteGameClientPort.executeLocalMacro(...)` already carry a closed typed command generically.

### Baseline Anchors And Reachable Caller

- `696a12b0:DialogService.java:129` `handleDialog(...)`; its initial snapshot call at about `172` and later
  direct snapshot calls keep their original order and arguments.
- Complete public detection lifecycles at baseline lines `1506-1547`:
  `detectDialogTypeNoFocus(reason)`, both overloads with `hidePlayerNames/waitBeforeCaptureMs`, and
  `detectDialogSnapshotNoFocus(...)`; private shared implementation is `1550-1596`.
- Local mechanics mirrors baseline `1558-1584`: optional pre-wait -> optional Alt+4 + 500 ms settle -> one
  exact-window dialog-frame capture -> mask -> lower-half option -> upper-half story, returning the same captured
  frame for downstream same-tick reuse. Command fields are exactly `source`, `hidePlayerNames`,
  `waitBeforeCaptureMs`; no phase or retry field is added.
- Actual Cloud caller chain after A whole-context release:
  `AutoBattleTask/BaseTaskTemplate` exact context -> `NpcClickService` or `TaskMaintenanceService` ->
  `DialogService.handleDialog(...)`/public detection API -> `CloudDialogDetectionPort` -> existing
  `RemoteGameClientPort.executeLocalMacro(...)` -> existing DHXY poll/handler -> local mechanics -> typed result.

### Closed Result And Terminal Matrix

- `EXECUTED + CAPTURED`: result carries `dialogType` (`NONE/OPTION/STORY`), screen-absolute
  `dialogLeft/top/right/bottom`, PNG bytes (JSON base64), SHA-256, frame width/height, mask stddev, option/story
  pixel counts, and flattened text-line stats (`matched/qualifyingRows/maxWhitePixelsInRow/maxClustersInRow/maxSpanInRow`).
  Cloud verifies SHA-256 and dimensions, decodes once, and rebuilds `DialogDetection(type, rect, null, image)`.
- `EXECUTED + CAPTURE_UNAVAILABLE/PRE_CAPTURE_INTERRUPTED/NON_INPUT_WORKER/MECHANICS_FAILED`: every image,
  rect/type/metric field is explicitly null; Cloud maps to baseline `DialogDetection.none()` and never invents a
  dialog or repeats the command. `PRE_CAPTURE_INTERRUPTED` remains baseline's no-detection result, not a new retry.
- transport `NOT_EXECUTED`: no typed result; stale identity/admission/binding stays a structured no-detection and
  is not resubmitted. transport `STOPPED`: no typed result and propagates the existing task-stop path. transport
  `UNKNOWN`: no typed result, logs/fails conservative and never retries because Alt+4 may already have run.
- DHXY handler runs `hidePlayerNames=false` outside the input queue under exact `WindowTaskContextHolder.callWith`.
  For `hidePlayerNames=true`, it uses the existing single `submitRemoteExclusiveAndWaitDetailed` callback and calls
  `detectDialog(...)` directly on the input worker, so Alt+4 + settle + capture is one closed local macro and no
  queue-in-queue deadlock is possible.

### Acceptance, Dependencies, And Exclusions

- B R2 must first be parent-released with the local mechanics signature/result unchanged; A AutoBattle context
  producer must be released before runtime reachability, but B protocol implementation may compile independently.
- Both sealed command/result allowlists, strict request/outcome codecs, flat terminal payload, canonical nested
  digest, and `withCommon` reconstruction must include exactly one `DIALOG_DETECTION` variant; all other macro
  digests must remain byte/canonical equivalent.
- Source review must compare every baseline public detection overload and all `detectDialogSnapshotDirect` callers;
  `reason/hidePlayerNames/waitBeforeCaptureMs`, random default wait, call order, one-frame reuse, `NONE` semantics,
  logs and image ownership/flush rules cannot change.
- This candidate fully closes the Dialog detection public lifecycle and the detection stage used by
  `handleDialog`; it does not claim unrelated option OCR/template clicks or story-advance input are complete.
- Mutual exclusion at dispatch: reserve all 18 files as one atomic cohort. It does not touch C incense mechanics,
  D Navigation X2 files, A AutoBattle three files, runner/host/tests, permanent-local Bag/UICleaner/GiveItem/
  QuestManager Services, or dormant artifact/retained flows.
- No ordinary click is added here. Any future ordinary Dialog click remains an ordered `InputBundle`; only the
  Alt+4/capture interleaving is inside this closed local macro.
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

- scope confirmation: Candidate Queue #9A 只 append 本固定 helper 报告；未改 Java、A/B/C/D 日志、主文档或
  计划，未运行 Maven/test/runtime/Git，未作 reviewer 结论。按用户要求，本轮不等待或继续编排 B/C/D。

## Candidate Queue #9D - Navigation Current-Map Whole Local-Macro Lifecycle
- preparedAt: `2026-07-14T16:45:29-04:00`；helper 建议，不是 reviewer 结论。
- task id/status: `W-696-NAV-CURRENT-MAP-CLOUD-CALLER-2` / `READY_AFTER_D_R3_RELEASE`；D 已释放，可直接实施。
- exact Java write set (2): create Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudNavigateInCurrentMapPort.java`；modify Cloud `src/main/java/com/bot/dhxy/service/NavigationService.java`。
- mutual exclusion: 不触碰 A `AutoBattleTask/BaseTaskTemplate/TaskStepExecutor`、B `DialogDetectionLocalMechanics/ImagePreprocessor`、C 四个 OCR/incense 文件或 D 已交付的 `DialogStoryAdvanceLocalMacroMechanics`；双仓 shared wire/handler 冻结。
- baseline anchor: `696a12b0` `NavigationService.navigateInCurrentMap(NavigationRequest)`（约 `513-674`）完整 public lifecycle；请求与返回结构分别为 `NavigationRequest` / `NavigationResultStatus`。
- existing closed chain: Cloud `NavigateInCurrentMapMacroCommand` 14 字段 + `NavigateInCurrentMapMacroResult` 全 10 status -> `LOCAL_MACRO/NAVIGATE_IN_CURRENT_MAP` -> DHXY `LocalRemoteGameCommandHandler.executeNavigateInCurrentMapMacro` -> 本地 baseline `NavigationService.navigateInCurrentMap` -> flat typed terminal。
- implementation: 新 port 从当前 exact `TaskExecutionContext` 构造 14 字段 command，单次调用 `executeLocalMacro`；Cloud `NavigationService` 保留 null/target-coordinate 前置判断后，把原完整 current-map mechanics 段替换为 port，并逐值还原 `NavigationResult` message/status。
- caller reachability: 既有 Cloud Task/Service 对 `NavigationService.navigateInCurrentMap(request)` 的调用不改签名；A #9A whole-lifecycle producer 落地后，holder 提供 caller-supplied exact context，不允许 default/epoch=0 fallback。
- terminal acceptance: `EXECUTED` 必须无损映射 `ARRIVED/PATHING_STARTED/SUCCESS/FAILED/STOPPED/INTERRUPTED/DIALOG_PREPARING/MAP_NOT_REACHED/POINT_NOT_REACHED/DIALOG_OPENED`；`NOT_EXECUTED/STOPPED/UNKNOWN` 不得伪成成功或自动重发。
- parity acceptance: 14 个 request 字段逐一相等，尤其 random radius、keep-turn、arrival tolerance、fresh location 三元组/time/phase-bound；DHXY handler 继续在 input queue 外调用本地 Service，原内部 ordered input/watcher/delay/fallback/state 不变。
- release/build gate: 可与 A 实现并行但 runtime 验收等待 #9A producer；scoped diff 仅两文件，禁止 owner/permit/session/ledger/TTL/retry、runner/host/tests 或 shared protocol 改动。
- Dialog story note: 新建其完整双仓 macro 在 5 文件上限内不可闭合，故本轮不伪造；本候选使用已存在且无落点歧义的较大完整链。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #10B EOF Canonical Registration

- canonical task: `W-696-DIALOG-DETECTION-CLOSED-MACRO-CHAIN-1`；状态仅为 helper
  `CANDIDATE_READY_AFTER_B_R2_PARENT_RELEASE`，不是审批结论，尚未发单。
- full implementation brief: 本报告 `## Candidate Queue #10B - Dialog Detection Closed Local-Macro Whole Chain`
  段；其 18-file 双仓 write set、baseline caller、closed result、terminal matrix、依赖和验收整体构成
  本条，不得拆成 DTO/helper 小单。
- Cloud reservation (10): 3 New
  `DialogDetectionMacroCommand/DialogDetectionMacroResult/CloudDialogDetectionPort` + 7 Modify
  `LocalMacroKind/LocalMacroCommand/LocalMacroRequest/LocalMacroOutcome/RemoteCommandOutcomeEnvelope/RemoteProtocolDigests/DialogService`。
- DHXY reservation (8): 2 New
  `RemoteDialogDetectionMacroCommandPayload/RemoteDialogDetectionMacroResultPayload` + 6 Modify
  `RemoteLocalMacroKind/RemoteLocalMacroCommandPayload/RemoteLocalMacroResultPayload/RemoteOperationPayloadCodec/RemoteProtocolDigests/LocalRemoteGameCommandHandler`；
  B R2 的 `DialogDetectionLocalMechanics` 是 parent-release 后只读依赖。
- chain: Cloud public detection/`handleDialog` detection stage -> existing generic `executeLocalMacro` ->
  `DIALOG_DETECTION` -> exact binding；`hidePlayerNames=true` 才在单一 exclusive input callback 内执行
  Alt+4+settle+capture，false 在 queue 外 capture -> typed terminal -> verified PNG/rect/type -> Cloud
  `DialogDetection`。
- terminal: `EXECUTED/CAPTURED` 带完整图像、绝对 rect、type、hash/dimensions/metrics；四个非捕获 mechanics
  state 带显式 null 字段并映射 baseline `none()`；`NOT_EXECUTED/STOPPED/UNKNOWN` 无 typed result、零自动重发。
- exclusions: 不碰 A AutoBattle、C incense、D Navigation、runner/host/tests、永久本地四 Service、任何
  owner/session/ledger/TTL/retry。无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #11A/#11C - Immediate Whole Local-Lifecycle Cohorts

- preparedAt: `2026-07-14T17:29:29-04:00`；仅为 Next-Task Queue Helper 的发单建议，不是
  `APPROVED/BLOCKED` 结论。
- current exclusions: B 独占 `DIALOG_DETECTION` 18-file shared wire；D 独占 Cloud
  `NavigationService.java` 与 `CloudNavigateInCurrentMapPort.java`。A 已批准的
  `AutoBattleTask/TaskMaintenanceRequest/BaseTaskTemplate/TaskStepExecutor` 与 C 已批准的
  `OcrWordResult/TextRecognizer/SheyaoxiangDigitTemplateReader/PlayerStateIncenseStatusLocalObservationMechanics`
  全部只读。以下两项彼此及与上述集合完全互斥，不改 generic wire/handler。

### #11A External A - NPC Ctrl-Probe Whole Continuous Local Lifecycle

- task id/status: `W-696-NPC-CTRL-PROBE-WHOLE-LOCAL-LIFECYCLE-1` /
  `CANDIDATE_READY_LOCAL_PREREQUISITE`；可立即发单，但不声称 Cloud caller 已接线或整类迁移完成。
- exact Java write set (2, DHXY only):
  1. Modify `src/main/java/com/bot/dhxy/service/NpcClickService.java`.
  2. Modify `src/main/java/com/bot/dhxy/service/npc/NpcClickCtrlProbeLocalMacroMechanics.java`.
- baseline anchors: `696a12b0:NpcClickService.java` 的全部
  `clickNpcByCtrlMenuScan(...)` overload/主流程（约 `303-460`）、`buildCtrlMenuScanRect(...)`（约 `462`）及
  两个真实 caller（约 `1046,1124`）。必须保持候选 origin/offset/ring 顺序、screen-absolute clamp、
  before-capture -> Ctrl down 80ms -> move 280ms -> after-capture -> `0.05` change gate -> wash/OCR/fuzzy ->
  first match click 100ms -> dialog/battle verify -> `finally` Ctrl up 100ms 的完整顺序。
- reachable chain: 现存 public smart-click/click-NPC entry -> `NpcClickService` baseline candidate loop ->
  `NpcClickCtrlProbeLocalMacroMechanics.probe(...)` -> exact bound capture + approved local-only OCR + direct input
  while already on the one input worker -> closed `CtrlProbeLocalResult` -> original caller success/fallback branch。
  本单不得留下 callback/Function seam，也不得在 exclusive callback 内再次 submit input queue。
- implementation: 修掉旧 mechanics 对不存在 OCR API 的依赖，仅读取已批准
  `TextRecognizer.getAllTextResultsLocalOnly(...)` 与既有 `ImagePreprocessor`；把 origin/ring/scan-rect 建立、
  stop checkpoint、一次 probe typed mapping 接回 `NpcClickService` 的完整 candidate loop。Service 不再并存第二套
  Ctrl mechanics，普通 first-shot/menu fallback、随机顺序、日志与 stop/fallback 时点原样保留。
- terminal matrix: mechanics `FOUND` 仅在菜单命中点击且 dialog/battle verifier 成功时映 public success；
  `NOT_FOUND` 继续 baseline 下一个 offset/origin；`INTERRUPTED` 立即结束并保留 interrupt/stop；
  `BINDING_UNAVAILABLE` 走原 capture-unavailable/fallback，不伪成 miss。`clickX/Y=-1` 仅允许非 `FOUND`，
  scan rect 始终 screen-absolute；异常不得触发新增 retry。
- acceptance gate: 对照上述 baseline 全部 overload/caller，确认 Ctrl 每条退出路径均在 `finally` 释放、
  capture 与点击使用同一 exact binding、零 tracker-global fallback、零 queue-in-queue；完整 candidate loop 的
  分支/offset 次序/delay/fuzzy threshold/dialog-battle verifier/fallback 一项不减。scoped diff 仅两文件，
  不碰 B/D/shared wire、runner/host/tests，不新增 owner/permit/session/ledger/TTL/retry。
- dependency/risk: C 已批准 OCR 文件只能只读；旧 mechanics 曾因不存在的 OCR API 与外置 callback 而不可闭合，
  本单必须同时修 mechanics 和真实 Service caller，不能只修一个类。后续唯一接线门是在 B 18-file reservation
  父级释放后，另单增加一个 `NPC_CTRL_PROBE` typed local-macro variant 并替换 Cloud `NpcClickService` 对应调用点；
  本单不抢该 shared wire。

### #11C External C - PlayerState Incense Whole Local Business Lifecycle

- task id/status: `W-696-PLAYERSTATE-INCENSE-WHOLE-LOCAL-LIFECYCLE-1` /
  `CANDIDATE_READY_LOCAL_PREREQUISITE`；可立即发单，但不声称 Cloud observation wire 已闭合。
- exact Java write set (1 complete large Service, DHXY only): Modify
  `src/main/java/com/bot/dhxy/service/PlayerStateService.java`。已批准
  `service/playerstate/PlayerStateIncenseStatusLocalObservationMechanics.java` 只读，不得修改。
- baseline anchors: `696a12b0:PlayerStateService.java` 的全部
  `ensureSheYaoXiangActive` overload/private lifecycle（约 `509-696`）、leader public caller（约 `697-722`）、
  `probeIncenseStatus`（`1002-1054`）、`probeIncenseIconPresence`（`1056-1068`）和
  `probeIncenseIconPresenceInRect`（`1083-1116`）。Bag item-use callback、memory-trust gate、cached-present/unknown
  早退、absent full fallback、20-minute refresh decision、state timestamp/cache 更新顺序全部保留。
- reachable chain: 现存 public `ensureSheYaoXiangActive*` callers -> whole `PlayerStateService` incense business
  lifecycle -> approved `probeIncenseIconPresence(...)` / `probeIncenseStatus(...)` typed local operations ->
  existing Bag item-use callback（永久本地 `BagService`）-> original boolean result/state update。仅替换视觉 mechanics，
  “是否吃香”继续由 Service baseline business chain决定，不下沉到 mechanics。
- implementation: constructor 注入既有 mechanics；删除 Service 内重复 capture/template/OCR 私有实现并在原调用点
  做 exhaustive typed mapping。presence `PRESENT/ABSENT/UNKNOWN` 分别映原 present/absent/unknown 分支；status
  `REMAINING_TIME_FOUND` 携 screen-absolute icon/offset/remainingMs，`OCR_UNAVAILABLE` 与
  `ICON_PRESENT_TIME_UNREADABLE` 保留 icon-present 语义，`TEMPLATE_ABSENT/CAPTURE_UNAVAILABLE/MECHANICS_FAILURE`
  分别回到原 absent/unavailable/failure 路径，不得合并为普通 miss。
- terminal/business matrix: cached `PRESENT` 或 `UNKNOWN` 不发 full probe；仅 `ABSENT` full fallback；trusted memory
  保留原免读窗口；remaining `>20min` 不使用，`<=20min` 才走原 item-use；使用成功才更新 last-used/cache，失败
  不写成功状态。capture/OCR/mechanics failure 不自动重读、不新增 cooldown/TTL/retry。
- acceptance gate: 所有 `ensureSheYaoXiangActive*` public overload 与 leader/open-main-bag caller 均可达同一
  typed mechanics；对照 baseline 核 capture 次数、cached gate、阈值 `0.85`、cyan->green、时间单位、Bag callback
  时点、日志/state mutation/fallback。Service 中不得残留第二套 incense 图像算法；scoped diff 仅该大型 Service，
  不碰四个 C-approved 文件、B/D/shared wire、runner/host/tests，不新增 owner/permit/session/ledger/TTL/retry。
- dependency/risk: 本单是 B shared handler 冻结期间仍可完成的完整本地 caller/business/mechanics prerequisite；
  后续唯一接线门是在 B reservation 释放后，将这两种 typed observation 加入一个 closed PlayerState observation
  command/result，并让 Cloud `PlayerStateService` 消费同一 terminal matrix。该后续单必须把 business decision 留 Cloud，
  不得把永久本地 `BagService` 搬云或复用本单为长期本地业务权威。

### Mutual Exclusion And Scope

- #11A 只占 DHXY `NpcClickService` + Ctrl mechanics；#11C 只占 DHXY `PlayerStateService`。两者互斥，
  且与 B 18 files、D 两个 Navigation files、A/C 刚批准文件均零交集。
- 两项均是完整连续 lifecycle prerequisite，不是 DTO/helper/单方法壳；其完成不增加整类迁云计数，直到各自
  唯一 Cloud typed wiring gate 经父级另行发单、源码审查与统一构建通过。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Queue Repair #12 - AUTHORITATIVE TRUE EOF REGISTRATION

- authoritativeAt: `2026-07-14T17:46:00-04:00`；本段是 `Queue Repair #12` 的真实 EOF 排班锚。
  完整 implementation briefs 位于本报告同名 `## Queue Repair #12 - Cloud Business Ownership Restored`
  段（首次 append 因重复基线句命中旧位置，历史保留）；以下注册其可直接发单边界，不作源码审查裁决。
- A canonical task: `W-696-NPC-CTRL-PROBE-CLOSED-MECHANICS-2` /
  `READY_IMPLEMENTATION_PREREQUISITE`。唯一 Java 写集为 DHXY
  `service/npc/NpcClickCtrlProbeLocalMacroMechanics.java`；完成单 probe 的 exact-binding
  Ctrl down/capture/OCR/click/dialog-or-battle verify/finally Ctrl up closed mechanics与 immutable command/result。
  严禁修改 DHXY/Cloud `NpcClickService`、建立 candidate loop或抢 B shared wire。terminal 为
  `FOUND/NOT_FOUND/OCR_UNAVAILABLE/BINDING_UNAVAILABLE/INTERRUPTED/MECHANICS_FAILED`，本地零 retry/下一候选。
- A future gate/backup: B 释放后另单把该 command/result 接给 Cloud `NpcClickService`，candidate/fallback 仍在
  Cloud。backup=`W-696-NPC-YELLOW-TARGET-CLOSED-OBSERVATION-MECHANICS-1`，create-only DHXY
  `NpcClickYellowTargetLocalObservationMechanics.java`，只返回 caller-selected rect 的 typed OCR observations，
  不点击、不分类。
- C canonical task: `W-696-TASK-TRACKER-PANEL-WHOLE-CLOUD-ALGORITHM-1` /
  `READY_AFTER_B_TASK_TRACKER_MECHANICS_CONTRACT`。唯一 Java 写集为 Cloud
  `service/TaskTrackerPanelService.java` + create `remote/CloudTaskTrackerPanelMechanicsPort.java`；保留 1643 行
  baseline 全 public/private 算法图、绿链/glyph/fingerprint/cache/排序/分类/结果构造和 replay APIs在 Cloud。
- C minimum dependency: 当前 `TASK_TRACKER_READ` 在 DHXY handler 与 Cloud retained state均明确 dormant，rect fact
  也不提供 drag 后同帧 PNG/OCR terminal，故不能伪 READY。B 释放 shared wire后先实施一个 closed
  `TASK_TRACKER_PANEL_MECHANICS` macro：`LOCATE_DRAG_CAPTURE_PANEL` 返回 PNG/SHA/screen-absolute origin，
  `OCR_REGION` 返回 ordered OCR boxes；DHXY 不选择 title/task/link/cache。backup 即
  `W-696-TASK-TRACKER-PANEL-MECHANICS-CONTRACT-1 / READY_AFTER_B`，不得激活 dormant artifact owner/ledger。
- D canonical task: `W-696-TASK-MAINTENANCE-SUMMON-WHOLE-PASS-CHAIN-1` /
  `READY_IMPLEMENTATION`（父级先释放 D Navigation 两文件）。唯一 Java 写集为 Cloud
  `TaskMaintenanceService.java` + `SummonSkillService.java`；existing explicit context -> existing public
  `summonSkillWholePass()` capability -> DHXY handler真实 producer -> exact cleanup terminal。四 intent 字段与九
  cleanup 字段一一映射；TaskMaintenance queue/team gate/cache/cooldown/unknown-backoff/state/finally顺序保持 696。
  `NOT_EXECUTED/STOPPED/UNKNOWN` 不写 success且零自动重发，不改任何 remote/handler 文件。
- D backup: `W-696-AUTO-COMBAT-PANEL-WHOLE-CLOUD-BOUNDARY-1` /
  `READY_AFTER_B_AUTO_PANEL_ROUNDS_OBSERVATION`；Cloud `AutoCombatPanelService.java` + new
  `CloudAutoCombatPanelPort.java`。visibility/input producer已存在，但 rounds capture/OCR terminal仍缺，未补前不得发单。
- mutual exclusion: A 仅一个 DHXY NPC mechanics；C 仅两个 Cloud TaskTracker files且等待 prerequisite；D 仅两个
  Cloud TaskMaintenance/SummonSkill files。与 B Dialog 18 files、当前 D Navigation files及彼此均零写集交集。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #13 - AUTHORITATIVE TRUE EOF REGISTRATION

- registeredAt: `2026-07-14T18:10:00-04:00`；完整文件表、696 anchors、caller、terminal、依赖和验收位于本报告
  `## Candidate Queue #13 - Post-Current-Wave Whole Reachable Cohorts`（约 `:1073`）。该完整段因历史重复
  基线句被 append 工具命中旧位置；不删除/搬移历史，以本段作为本轮真实 EOF 排班锚。本段仍只是 helper 建议。
- current reservations excluded: A=`DHXY NpcClickCtrlProbeLocalMacroMechanics.java`；B=`DIALOG_DETECTION`
  18 files；C=`Cloud Memory 3 Services + CloudServiceConfiguration`；D=`Cloud AutoCombatService.java`。
- A primary: `W-696-TASK-MAINTENANCE-SUMMON-WHOLE-PASS-CHAIN-2` /
  `READY_AFTER_A_CURRENT_RELEASE`；Cloud `TaskMaintenanceService.java + SummonSkillService.java`，现有 actual
  `summonSkillWholePass()` producer/handler只读，闭合 `AutoBattleTask -> maintenance -> whole-pass terminal`。
- B primary: `W-696-TEAMRETURN-WHOLE-TYPED-FACT-CLOSURE-2` /
  `READY_AFTER_B_CURRENT_RELEASE`；Cloud `TeamReturnService.java` 整类，复用 handler 已实际生产的
  `TEAM_RETURN_BUTTON/TEAM_RETURN_LEADER_SIGNAL` facts 与 ordered click bundle，移除整类残留 desktop authority。
- C primary: `W-696-PLAYERSTATE-WHOLE-CLOUD-BUSINESS-CHAIN-2` /
  `READY_AFTER_PLAYERSTATE_OBSERVATION_CONTRACT`；Cloud `PlayerStateService.java` + new
  `CloudPlayerStateLocalMechanicsPort.java`；first-aid/incense business 决策留 Cloud，永久本地 Bag 只经既有 use port。
  当前没有实际 observation producer wire，且 identity 落点待用户选择，因此不得提前标 READY。
- D primary: `W-696-AUTOCOMBAT-PANEL-WHOLE-CLOUD-BOUNDARY-2` /
  `READY_AFTER_D_RELEASE_AND_ROUNDS_OBSERVATION`；Cloud `AutoCombatPanelService.java` + new
  `CloudAutoCombatPanelPort.java`；现有 fact 只覆盖 visibility/anchor，rounds PNG/OCR/metrics producer 未落，
  因此不得以 visibility 伪闭合整类。
- backups: A=`TASKTRACKER-WHOLE-CLOUD-ALGORITHM-2/READY_AFTER_TASK_CALLER_AND_PANEL_MECHANICS`；
  B=`NAVIGATION-WHOLE-ACTION-SITE-EXTRACTION-2/READY_AFTER_NAV_FACT_CONTRACT`（60s loop 必须留 Cloud）；
  C=`NPC-CLICK-WHOLE-CLOUD-CALLER-2/READY_AFTER_A_CTRL_WIRE_AND_OTHER_NPC_MECHANICS`；
  D=`CLIENT-IDENTITY-WHOLE-BOUNDARY-1/NEEDS_USER_DECISION`。
- primary mutual exclusion: A 两 Cloud Service；B 一个 Cloud Service；C 一个 Cloud Service+一个独有 new port；
  D 一个 Cloud Service+一个独有 new port；四组彼此和当前 A/B/C/D 写集均零交集。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Queue #13 D Idle-Gap Resolution - Current TRUE EOF Reconciliation

- observedAt: `2026-07-14T18:18:00-04:00`；本段仅登记可执行排班建议，不作源码审批。读取 D 固定日志真实
  EOF 后确认：父级已于 `18:15` 发布下列直接实现单，因此 D 已有实质 reservation，不得再并发发第二单。
- task: `W-696-TASK-MAINTENANCE-SUMMON-WHOLE-PASS-CHAIN-1`；状态=
  `READY_NOW_FOR_EXTERNAL_D / ALREADY_PARENT_ISSUED / WAITING_CLAIM`。
- exact Java write set（两文件，完整 Service cohort）：Cloud
  `src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`、
  `src/main/java/com/bot/dhxy/service/SummonSkillService.java`。baseline blobs 分别为
  `e93cfd01d9c282f98881a6311b8bb806bbc3e359`、`d8afb9e2f97aba9522393bd9a21d0cc4c48ed324`。
- `696a12b0` anchors: `TaskMaintenanceService.runOpportunisticMaintenance -> maybeCleanSummonSkill ->
  buildSummonSkillCleanupRequest` 与 `SummonSkillService.cleanSummonSkillsOnce*` 的完整 whole-pass；不得缩成
  DTO/helper 或单方法适配。
- reachable caller: active Cloud `AutoBattleTask:208 -> TaskMaintenanceService.runOpportunisticMaintenance`；该
  task 在 `AutoBattleTask:113` 由真实 `TaskExecutionContextHolder.callWith(context, ...)` 绑定 exact context。
- actual typed producer: existing `CloudTaskServicePort.summonSkillWholePass()` /
  `CloudSummonSkillWholePassCapability`，DHXY handler 当前真实调用本地
  `SummonSkillService.cleanSummonSkillsOnce(...)`；本单只消费该现有链，不改 handler/schema/shared wire。
- implementation/terminal: 四个 intent 字段与 cleanup result 全字段一一映回 baseline；仅 `EXECUTED` 进入原
  success/timestamp/cache 分支，`NOT_EXECUTED` 不写成功，`STOPPED` 沿原 stop unwind，`UNKNOWN` 保持未决且
  零自动重发。完整保留 capability/team/round gates、slot 顺序、delay、ultimate-first、cooldown/backoff、
  `GameContext.ActionState` finally 与日志/state mutation。
- acceptance: public caller 到两个完整 Service 再到 typed terminal 可达；逐方法/分支/delay/fallback/state 对照
  `696a12b0`；无 HWND/capture/OCR/input authority 新入 Cloud；不新增 owner/permit/session/ledger/TTL/retry。
- mutual exclusion: 与 A 的 DHXY Ctrl mechanics、B 的 Dialog 三文件 repair、C 的 Memory 三 Service +
  `CloudServiceConfiguration`、已释放的 D `AutoCombatService` 均零写集交集，因此是当前真实 READY_NOW 大单。
- backup correction: `TeamReturnService` 虽已有 `TEAM_RETURN_BUTTON/TEAM_RETURN_LEADER_SIGNAL` 实际 producer 和
  active member caller，但 baseline `begin/consumeLeaderSignalPrecheck` 仍要求 capture-time immutable frame +
  background analysis；现有 live fact 不能等价保持该时序，故只能列
  `READY_AFTER_TEAM_RETURN_PRECHECK_TYPED_CONTRACT`，不得作为本次 D 即时整类单伪 READY。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Focused TASK_TRACKER_PANEL_MECHANICS B/C Split - NOT READY_NOW

- recordedAt: `2026-07-14T18:35:12-04:00`；本段仅为排班 helper 结论，不作审批。聚焦结论：**不能把 B/C
  同时标成 READY_NOW**。唯一阻塞是 Cloud 真实 caller 没有可用的 task-run context producer：当前唯一 caller
  `DecisionEngine.trackerPanelRead()`（`:298-306`）手工构造 same-path Service（`:64-66`），既不在
  `BaseTaskTemplate/TaskStepExecutor -> TaskExecutionContextHolder.callWith(...)` 内，也没有可信
  `TaskExecutionContext/CloudGameClient` 参数；全树亦无第二个 `com.bot.dhxy.service.TaskTrackerPanelService` caller。
  因而 C 即使落完 contract/port，也只能得到不可达接口；从 JSON request 猜/default `epoch=0` 会破坏 exact
  `scope/window/taskRun/runRevision` fence，不能作为实现路径。
- `696a12b0` mechanics 对照：`:744-803` 必须保持 narrow `(6,196)-(207,551)` + anchor `0.82` -> miss 后一次
  expanded full-window capture/search -> anchor 超过 `(164,353)` 才 ordered drag 到 `(104,221)` + `500ms` ->
  以 anchor `(-96,+12,+86,+350)` 计算 rect -> drag 后同一次 mechanics 调用捕获 panel PNG 和 screen-absolute
  origin。`:886-923` 每个 Cloud 已准备 OCR crop 调一次 `getAllTextResultsForMatch`；provider 为 HYBRID 时顺序是
  local 一次、仅 local 缺失/空/不匹配时 Baidu 一次，无额外 retry；返回 boxes 按 `top,left` 稳定排序。绿链切分、
  OCR crop/mask、fingerprint/cache、候选排序、分类和结果构造全部留 Cloud。
- B future exact DHXY write set（仅在 Cloud caller prerequisite 闭合后发单）：create
  `cloud/remote/RemoteTaskTrackerPanelMacroCommandPayload.java`、
  `cloud/remote/RemoteTaskTrackerPanelMacroResultPayload.java`、
  `service/tasktracker/TaskTrackerPanelOcrLocalMechanics.java`；modify
  `cloud/remote/RemoteLocalMacroKind.java`、`RemoteLocalMacroCommandPayload.java`、
  `RemoteLocalMacroResultPayload.java`、`RemoteOperationPayloadCodec.java`、`RemoteProtocolDigests.java`、
  `LocalRemoteGameCommandHandler.java`。现有 `TaskTrackerPanelCaptureLocalMechanics.java` 只读复用；
  `LOCATE_DRAG_CAPTURE_PANEL` 在已有 remote-exclusive queue 内 direct 调 mechanics，禁止 queue-in-queue；
  `OCR_REGION` 在 queue 外 exact-context 调用，接收 Cloud prepared PNG/SHA，执行上述 provider 顺序并回 ordered boxes。
- C future exact Cloud write set（同一 prerequisite 后可与 B 并行，物理文件零交集）：create
  `remote/TaskTrackerPanelMacroCommand.java`、`remote/TaskTrackerPanelMacroResult.java`、
  `remote/CloudTaskTrackerPanelMechanicsPort.java`；modify `remote/LocalMacroKind.java`、
  `LocalMacroCommand.java`、`LocalMacroRequest.java`、`LocalMacroOutcome.java`、
  `RemoteCommandOutcomeEnvelope.java`、`RemoteProtocolDigests.java`、
  `service/TaskTrackerPanelService.java`。`DecisionEngine.java/CloudBrainServer.java` 不得被本单用来伪造 context；
  最小前置是让真实 task lifecycle 以 authority-minted `TaskExecutionContext` 可达 same-path Service，再由 port 读取
  holder 并调用 `context.getGameClient().executeLocalMacro(...)`。
- typed terminal：transport 仅 `EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN`；`UNKNOWN` 零自动重发。locate typed state
  为 `CAPTURED/ABSENT/CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/MECHANICS_FAILED`，仅 `CAPTURED` 携带
  `panelPngBytes/panelSha256/panelWidth/panelHeight/absoluteLeft/absoluteTop`；OCR typed state 为
  `OCR_COMPLETED/OCR_UNAVAILABLE/MECHANICS_FAILED`，仅完成态携带 image-local immutable ordered boxes
  `text/x/y/left/top/width/height/score`。非匹配 variant 字段显式 null，PNG 必须重算 SHA/尺寸。
- B backup=`NO_SAFE_READY_NOW_BACKUP_IN_FOCUSED_SCOPE`；本地 capture mechanics 已完整存在，再发 helper/DTO-only
  是零价值，handler/codec 单边又没有真实 Cloud caller。C backup=`NO_SAFE_READY_NOW_BACKUP_IN_FOCUSED_SCOPE`；
  当前 TaskTracker 唯一 caller 即上述无 context 的 DecisionEngine，且 D 已占
  `TaskMaintenanceService/SummonSkillService`。按用户要求不扩展扫描其它 Service，也不伪造 READY。
- 解除条件（唯一）：父级先落一个非 host/runner 的真实 caller，把 authority-minted
  `TaskExecutionContext` 带入 same-path `TaskTrackerPanelService` 生命周期；解除后上述 B/C 文件表天然跨仓互斥，可
  并行直接实现。无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #14A - External A READY_NOW Dialog Story Objective Capture Whole Mechanics

- preparedAt: `2026-07-14T19:16:00-04:00`；仅是 Next-Task Queue Helper 的 Implementation brief，不是源码
  `APPROVED/BLOCKED` 结论。task id=`W-696-DIALOG-STORY-OBJECTIVE-CAPTURE-WHOLE-MECHANICS-1`，排班状态=
  `READY_NOW_LOCAL_MECHANICS_PREREQUISITE`。
- 唯一 Java 写集（1 个 create-new 完整大类，command/result/state 均为该类底部的 immutable nested
  types，不拆 DTO/helper 小单）：DHXY
  `src/main/java/com/bot/dhxy/service/dialog/DialogStoryObjectiveCaptureLocalMechanics.java`。目标当前不存在；
  不改 `DialogService`/任何 remote enum/codec/digest/handler/任何当前 A/B/C/D 文件。
- `696a12b0` anchors：`DialogService.handleStoryObjective` `:1444-1469`（同一 detection frame 上裁小
  STORY 框）、`cropStoryObjectiveImage` `:1472-1478`、`captureCurrentStoryImage` `:2389-2404`、
  `captureCurrentStoryObjectiveSnapshotNoDetect` `:2413-2433`、
  `cropStoryObjectiveFromWindowSnapshotNoDetect` `:2445-2467`、debug image ownership `:2470-2484`。
  完整复制这三种连续 mechanics，不增减 capture/crop/save 次数或改变 image flush ownership。
- 现有真实 prerequisite/producer（全部只读）：`BoundWindowCaptureService.captureRegion(...)` 接受 exact
  `WindowNativeBinding`；已有 `DialogDetectionLocalMechanics.detectDialog(...)` 产出 detection type/image/absolute rect；
  `ImagePreprocessor.cropAbsoluteRect/cropCopy/saveImage`已存在；`WindowScopedTempPath` 产出 window-scoped debug
  path。本单不需要 holder producer、OCR sidecar、shared handler 或尚未落地 observation contract。
- 完整 local lifecycle/operation：
  `DETECT_AND_CAPTURE_STORY_OBJECTIVE`（exact-bound detection→仅 STORY 接受→复用该帧裁小框）；
  `CAPTURE_STORY_OBJECTIVE_NO_DETECT`（一次大框 capture→小框 crop）；
  `CROP_STORY_OBJECTIVE_FROM_WINDOW_SNAPSHOT`（caller 给定 immutable PNG + screen-absolute window origin，零新 capture）。
  三种都返回小框 PNG bytes/SHA-256/width/height/screen-absolute `left/top`，不返回不透明本地 Path
  作为跨边界 authority。
- terminal matrix：`CAPTURED`仅其携带 PNG/SHA/dimensions/absolute origin；`NOT_STORY`、
  `CAPTURE_UNAVAILABLE`、`CROP_FAILED`、`BINDING_UNAVAILABLE`、`INTERRUPTED`、`MECHANICS_FAILED`
  全部图像字段显式空。该链无 input，在 input queue 外 exact-context 调用；零自动 retry/TTL。
- baseline caller reachability/future owner：`DialogService.handle(...) -> handleStoryObjective(...)` 是真实 public
  Service lifecycle；现有 `XiuluoTaskV2` 还直接调用 snapshot crop。本单只把这一整段 desktop
  mechanics 做成可调用 closed 类；目标地图/坐标 OCR、分类、`DialogResult` 构造和 fallback 仍由未来
  Cloud `DialogService` 按原调用点所有。
- 后续唯一 wire gate：当 B/C 当前 `PLAYER_STATE_FIRST_AID` shared enum/codec/digest/handler 写集被父级释放后，
  另一单增 `DIALOG_STORY_OBJECTIVE_CAPTURE` closed local-macro 两仓 mirror，并在 Cloud `DialogService`
  原 `handleStoryObjective` 调用点消费上述 terminal。本单不预占该 gate，不伪称 same-path 整类已闭合。
- 完整性验收：对照上述五段 baseline，核验 small dialog rect/窗口原点/裁剪几何、同帧不重截、
  debug latest+history 规则、所有 BufferedImage 单一 owner/flush 和三 operation exhaustive switch；类内不识别
  objective 文本、不决定业务结果，不新增 owner/permit/session/ledger/TTL/retry。
- 互斥证明：本单仅创建上述 dialog 类；B 当前 9 个 DHXY PlayerState/wire 文件、C 当前
  10 个 Cloud PlayerState/wire 文件、D 当前 Cloud `TaskMaintenanceService/SummonSkillService`、A 即将领取的
  NPC yellow-target mechanics 均零文件交集；TaskTracker/Navigation X2/ClientIdentity/TeamReturn precheck/
  AutoCombatPanel rounds 均不在本单。
- backup：`W-696-DIALOG-PREPARED-ACTION-VALIDATION-WHOLE-MECHANICS-1` / `READY_AFTER_IMAGE-FINGERPRINT-PRIMITIVE`；
  唯一写集为 create-new `service/dialog/DialogPreparedActionValidationLocalMechanics.java`，对应 baseline
  `DialogService:1132-1234`。当前本地 `ImagePreprocessor` 没有 baseline fingerprint build/distance 全 API，未先补这一
  纯 CPU primitive 前不标 READY_NOW，禁止用 Cloud image processor 回调打断 local atomic validation。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## URGENT READY_NOW A/D - TRUE EOF CANONICAL REGISTRATION

- canonical note：完整 Implementation briefs 已追加于本报告 `## URGENT READY_NOW A/D - Two Released-Worker
  Whole Mechanics Cards` 段（约 `:1073`）；因历史重复基线句导致首次 append 命中中段，不删历史，
  以本段作为真实 EOF 排班权威。本 helper 不作源码审批。

### A canonical READY_NOW

- task=`W-696-DIALOG-STORY-OBJECTIVE-CAPTURE-WHOLE-MECHANICS-1`；exact write set=仅 create-new DHXY
  `src/main/java/com/bot/dhxy/service/dialog/DialogStoryObjectiveCaptureLocalMechanics.java`（完整大类，immutable nested
  command/result/state）。
- baseline/caller=`696a12b0 DialogService:1444-1478,2389-2484`；真实 public 链是
  `DialogService.handle -> handleStoryObjective`，`XiuluoTaskV2` 亦直接调 snapshot crop。
- producer/lifecycle=已有 `BoundWindowCaptureService` + exact `WindowNativeBinding` +
  `DialogDetectionLocalMechanics` + `ImagePreprocessor.cropAbsoluteRect/cropCopy/saveImage` +
  `WindowScopedTempPath`；完整实现 detect-and-same-frame-crop / no-detect-single-capture-crop /
  caller-snapshot-zero-capture-crop 三 operation。
- terminal=`CAPTURED(PNG,SHA,width,height,absoluteLeft,absoluteTop)`；`NOT_STORY/CAPTURE_UNAVAILABLE/
  CROP_FAILED/BINDING_UNAVAILABLE/INTERRUPTED/MECHANICS_FAILED` 图像字段显式空。零 input/retry/TTL。
- acceptance/gate=逐段对照同帧不重截、small-rect 几何、debug latest+history、PNG 重算与单一
  image owner/flush；不做 objective OCR/分类/结果构造。唯一后续 gate 是 B/C 释放 shared wire 后增
  `DIALOG_STORY_OBJECTIVE_CAPTURE` 两仓 mirror + Cloud `DialogService` 原调用点。
- backup=`W-696-AUTO-COMBAT-PANEL-VISIBILITY-ALIGN-WHOLE-MECHANICS-1` /
  `READY_NOW_LOCAL_MECHANICS_PREREQUISITE`；唯一 create-new
  `service/autocombat/AutoCombatPanelVisibilityLocalMacroMechanics.java`，仅覆盖 baseline `:69-157,269-320`
  find→Alt+8 once→re-find→necessary drag→re-find，明确排除 rounds read/refresh contract。

### D canonical READY_NOW

- task=`W-696-DIALOG-GREEN-TEMPLATE-OPTION-WHOLE-MECHANICS-1`；exact write set=仅 create-new DHXY
  `src/main/java/com/bot/dhxy/service/dialog/DialogGreenTemplateOptionLocalMacroMechanics.java`（完整大类，immutable nested
  intent/spec/result/state）。
- baseline/caller=`696a12b0 DialogService:2153-2378`；public preparation callers=
  `XiuluoTaskV2/WubeiDialogPreparationProvider/XiuluoDialogPreparationProvider -> prepareGreenTemplateOption`，
  direct chain=`DialogService.handleDialog -> handleGreenTemplateOption`。
- producer/lifecycle=已有 `DialogDetectionLocalMechanics`/`BoundWindowCaptureService`/
  `ImagePreprocessor.washDialogOptionTemplateTextToBlackAndWhite`/`ImageFinder.find`/
  `CoordinateHelper.resolveMatchedPointInRect/getRandomizedPoint`/`InputProvider`；实现 optional OPTION verify→一帧
  capture→一次 wash→caller-order templates `0.85` first-hit→randomized point→MATCH_ONLY 或在已持有的
  input-worker 内 direct click `150ms`。
- terminal=`MATCHED/CLICKED`携带 spec/template/relative+absolute point/PNG evidence；`NOT_OPTION/
  NOT_FOUND/CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/BINDING_UNAVAILABLE/INTERRUPTED/NON_INPUT_WORKER/
  MECHANICS_FAILED` 无虚假点位。零自动换序/重截/重点/retry/TTL。
- acceptance/gate=核验 verify=false 零多余 detection、verify=true 复用 detection frame、template 评估点加载/
  flush、顺序 short-circuit、threshold/offset/random/click delay 和单一 image owner。业务 spec 顺序/fallback/GiveItem
  决策不下沉。唯一后续 gate 是 shared wire 释放后增 `DIALOG_GREEN_TEMPLATE_OPTION` mirror + Cloud
  `DialogService` 原调用点；MATCH_AND_CLICK 单次 remote-exclusive，MATCH_ONLY queue 外。
- backup=`W-696-NPC-PLAYER-ANCHOR-FORMULA-WHOLE-MECHANICS-1` /
  `READY_AFTER_OCR_PROVIDER_PARITY`；唯一 create-new
  `service/npc/NpcClickPlayerAnchorFormulaLocalMacroMechanics.java`，anchors=`NpcClickService:998-1049,
  2865-3049,3106-3188 + LocationVisionService:278-344`。当前仅有 local-only OCR，baseline provider fallback 未闭合，
  故不伪 READY，不得改成 blob-only。

- mutual exclusion：A/D 各只新建上述一类，彼此及 B DHXY PlayerState 9 Java / C Cloud PlayerState
  10 Java 零交集；不碰 TaskTracker context、Navigation X2 handler、ClientIdentity placement、TeamReturn
  precheck 或 AutoCombatPanel rounds。无已批准业务差异；按 `696a12b0` 基线等价迁移。

## External B Next Queue - READY_NOW Auto-Combat Panel Visibility/Alignment Whole Mechanics

- helper scope：仅为 B 当前 PlayerState DHXY 9-file 写集被父级释放后的 Implementation 发单建议，
  不是源码审批。task id=`W-696-AUTO-COMBAT-PANEL-VISIBILITY-ALIGN-WHOLE-MECHANICS-1`，排班状态=
  `READY_NOW_LOCAL_MECHANICS_PREREQUISITE`。
- exact Java write set：仅 create-new DHXY
  `src/main/java/com/bot/dhxy/service/autocombat/AutoCombatPanelVisibilityLocalMacroMechanics.java`。这是一个完整
  大类，operation/intent/match/result/state 全为类底部 immutable nested types；不拆 DTO/helper，不改
  `AutoCombatPanelService`、`RemoteAutoCombatPanelFact`、任何 shared enum/codec/digest/handler。目标当前不存在。
- `696a12b0` anchors：`AutoCombatPanelService.verifyAndAlignPanel` `:69-88`、`ensurePanelVisible/
  ensurePanelMatchVisible` `:90-131`、`alignPanelIfNeeded` `:133-156`、`findAutoCombatBox` `:269-320`。
  必须完整保留 fallback-anchor-first→green-marker-second 匹配顺序、阈值、marker/panel offset、一次 Alt+8+
  caller wait、一次复查、distance `>20` 才 drag、drag `500ms` 后一次复查及原 drag-target fallback。
- complete local lifecycle：
  1. `OBSERVE_PANEL`：一次 exact-binding 全窗帧，先 panel-anchor template，miss 才绿色 mask+green-marker
     template，返回 screen-absolute panel center/green marker/template width/detection source；零 input。
  2. `ENSURE_VISIBLE`：首次 miss 时要求已在 input worker，direct `pressAlt8`→仅执行 caller-supplied baseline
     wait→仅一次 re-observe；不自动重试。
  3. `ENSURE_VISIBLE_AND_ALIGN`：复用 ensure 结果，仅在与 `windowOrigin + baseline target offset`距离
     `>20px` 时 direct drag→`500ms`→一次 re-observe；re-observe miss 保留 baseline drop-target fallback terminal。
- actual producer/caller：现有 `BoundWindowCaptureService.captureRegion(WindowNativeBinding,...)`、
  `ImageFinder.find(BufferedImage,BufferedImage,threshold)`、`ImagePreprocessor.countGreenPixelsHSV`、
  `InputProvider.pressAlt8/dragAndDrop`、`TaskSleep`、`WindowScopedTempPath` 都是真实本地 producer。真实
  same-path callers 为 `AutoCombatService:613 -> ensurePanelVisible`、`:1024/:1084 -> verifyAndAlignPanel`；
  Cloud 业务 caller 继续决定 mode/refresh/state，本类不读 `GameContext`。
- terminal matrix：`FOUND/FOUND_AFTER_OPEN/ALIGNED/ALIGNED_WITH_DROP_TARGET_FALLBACK` 携带完整
  screen-absolute match fields；`NOT_FOUND/CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/BINDING_UNAVAILABLE/
  NON_INPUT_WORKER/INTERRUPTED/MECHANICS_FAILED` 所有 point 字段显式空。`OBSERVE_PANEL` queue 外；
  后两 operation 在已持有的单一 remote-exclusive callback 内 direct input，禁止 queue-in-queue。
- acceptance：逐段对照 `:69-156,269-320`，核验两模板顺序/阈值、坐标空间、Alt+8/wait/
  re-capture 次数、drag gate/target/500ms/fallback、template/frame 单一 owner/flush 与 exhaustive operation/terminal。
  不复制 tracker-global screenshot authority，不增 owner/permit/session/ledger/TTL/retry。
- 唯一后续 gate：C 的 Cloud PlayerState shared wire 释放后，另一单增
  `AUTO_COMBAT_PANEL_VISIBILITY_ALIGN` closed command/result 两仓 mirror + handler + Cloud
  `AutoCombatPanelService` 原调用点。该 gate 只传可见性/对齐 terminal；rounds PNG/OCR/metrics contract
  仍明确阻塞，本单不读 rounds、不做 refresh reason/state/timestamp，不伪称整个
  `AutoCombatPanelService` 已闭合。
- mutual exclusion：本单只创建上述 auto-combat-panel mechanics，与 A 的
  `DialogStoryObjectiveCaptureLocalMechanics.java`、C 的 10 个 Cloud PlayerState/shared 文件、D 的
  `DialogGreenTemplateOptionLocalMacroMechanics.java`、B 刚交付的 DHXY PlayerState/shared 9 文件均零交集。

### B backup

- task=`W-696-NPC-PLAYER-ANCHOR-FORMULA-WHOLE-MECHANICS-1`；排班状态=
  `READY_AFTER_OCR_PROVIDER_PARITY`，不是本轮 READY_NOW。exact write set=仅 create-new DHXY
  `src/main/java/com/bot/dhxy/service/npc/NpcClickPlayerAnchorFormulaLocalMacroMechanics.java`（完整大类）。
- anchors/caller=`NpcClickService:998-1049,2865-3049,3106-3188` +
  `LocationVisionService:278-344`；真实 caller 为 public smart-click pipeline 的 player-anchor fallback，Cloud 必须继续
  所有 strategy/candidate/fallback 顺序。预期 terminal 为 `VERIFIED/CLICK_NOT_VERIFIED/PREDICTION_UNAVAILABLE/
  OUTSIDE_WINDOW/BINDING_UNAVAILABLE/INTERRUPTED/MECHANICS_FAILED`。
- blocker/解除门：现有真实 OCR producer 仅 `TextRecognizer.getAllTextResultsLocalOnly`，而 baseline 该路径使用
  `getAllTextResultsForMatch` provider fallback 后才进入紫色 blob fallback。在父级明确等价 provider 替换前，
  禁止把 local-only miss 或 blob-only 写成 READY。解除后验收 capture→purple wash→OCR-name fragment/
  compensation→blob fallback→公式点→atomic click+verify 的原顺序，零新 retry。

- 排除项：TaskTracker caller context、Navigation X2 shared handler、ClientIdentity placement、TeamReturn
  precheck 和 AutoCombatPanel rounds 均未被伪标 READY。无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue Rebase #14 - A 已消费原 B 主单后的四路后继

记录时间：`2026-07-14T19:52:08-04:00`。本段仅是 non-binding 排班材料，不是源码审批。

- A 已于 `19:49:24` 领取 `W-696-AUTO-COMBAT-PANEL-VISIBILITY-ALIGN-WHOLE-MECHANICS-1`，因此上文
  “External B Next Queue” 的同名候选从 B 队列撤销并视为 **A 当前 reservation**，不得重复发给 B。
- B/C/D 已分别于 `19:47/19:52/19:52` 交付 PlayerState DHXY R1-S1、PlayerState Cloud R1、Dialog green
  template R1；以下后继都以父级释放原写集为领取前置，不抢当前复核/返修。
- 本段四个 primary 均只 create-new 一个完整大类，目标经只读 `Test-Path` 核为不存在；不改 shared remote
  enum/codec/digest/handler，不碰永久本地 `BagService/UICleanerService/GiveItemService/QuestManagerService`。

### A next primary - AutoCombatPanel rounds whole observation

- task=`W-696-AUTO-COMBAT-PANEL-ROUNDS-WHOLE-OBSERVATION-1`；状态=`READY_AFTER_A_CURRENT_RELEASE`。
- 唯一 Java 写集：create-new DHXY
  `src/main/java/com/bot/dhxy/service/autocombat/AutoCombatPanelRoundsLocalObservationMechanics.java`；command/result/
  terminal 作为类底部 immutable nested types，不依赖或修改 A 当前 visibility 类。
- `696a12b0` anchors=`AutoCombatPanelService.readRemainingRounds:322-394`、`countBlackPixels:395-409`、
  `washRoundRedDigits/isAutoCombatRoundRedPixel:430-457`；可达业务链为
  `verifyAndAlignPanel -> refreshAutoCombatRoundsIfNeeded -> readRemainingRounds`。本地类只接收 panel center、
  nullable green marker、green template width、detection source，并完整执行 marker 优先/center fallback scan rect、
  exact-bound capture、4x red-digit wash、black-pixel count、local OCR、首个 `\\d{1,2}` 解析和证据输出。
- 真实 producer：`BoundWindowCaptureService.captureRegion`、`TextRecognizer.getAllTextResultsLocalOnly`、
  `WindowNativeBindingRefreshService`、`WindowScopedTempPath`、JDK image APIs；零新 fact/holder/owner。
- terminal=`ROUNDS_READ/NO_DIGITS/CAPTURE_UNAVAILABLE/OCR_UNAVAILABLE/BINDING_UNAVAILABLE/MECHANICS_FAILED`；
  仅 `ROUNDS_READ` 携 rounds，观察 terminal 携 capture rect、red-pixel count、OCR text 与 defensive PNG evidence。
  refresh reason、estimated rounds、Alt+8、timestamps/metrics/state 全留 Cloud。
- 验收门：逐项核 scan rectangle 两分支、4x wash/RGB predicate、OCR 拼接/regex、raw/washed owner/flush、
  screen-absolute 坐标与零输入；唯一后续接线门是 shared wire 释放后把该 terminal 接回 Cloud 原调用点。
- backup=`W-696-NPC-PLAYER-ANCHOR-FORMULA-WHOLE-MECHANICS-1`；状态=`READY_AFTER_OCR_PROVIDER_PARITY`；
  唯一目标 `service/npc/NpcClickPlayerAnchorFormulaLocalMacroMechanics.java`。现有 producer 只有 local-only OCR，
  不等价于 696 的 provider fallback，故当前不伪 READY；解除后整段验 capture→purple wash→OCR→blob fallback→
  formula→atomic click+verify。

### B new substantial primary - prepared-action validation whole mechanics

- task=`W-696-DIALOG-PREPARED-ACTION-VALIDATION-WHOLE-MECHANICS-1`；状态=`READY_AFTER_B_CURRENT_RELEASE`。
- 唯一 Java 写集：create-new DHXY
  `src/main/java/com/bot/dhxy/service/dialog/DialogPreparedActionValidationLocalMechanics.java`；这是完整 capture→wash→
  fingerprint→distance validation 大类，不是 DTO/helper 单。
- `696a12b0` anchors=`DialogService.captureDialogValidationImage:1143-1157`、
  `validatePreparedDialogActionForConsume:1158-1222`、`preparedDialogFingerprintMaxDistance:1224-1229`、
  `washPreparedValidationCrop:1231-1249`；真实 caller 包括 `tryConsumePreparedRememberedRouteOption` 及
  `XiuluoTaskV2/WubeiTask/NavigationService` 的 prepared-action consume 路径。
- 实施：command 携 exact screen-absolute rect、wash mode、expected fingerprint、Cloud 选定的 maxDistance；本地以
  exact HWND fresh geometry 单次 capture，按 696 精确实现各 wash mode 与 fingerprint/distance CPU 算法并返回证据。
  不在本地选择 action、target、阈值策略或 fallback，不发送输入。
- terminal=`VALIDATED/FINGERPRINT_MISMATCH/CAPTURE_UNAVAILABLE/INVALID_RECT/BINDING_UNAVAILABLE/MECHANICS_FAILED`；
  成功/不匹配均携 current fingerprint、distance、maxDistance、capture evidence，失败不得伪造 fingerprint。
- 验收门：一 capture、wash mode 穷尽、fingerprint 位宽/距离与 8/16 caller-supplied gate、image owner/flush、
  exact binding 和无业务 fallback；未来唯一接线门是 shared wire 释放后新增 validation operation 并替换 Cloud
  `DialogService` 原 capture 调用点。
- backup=`W-696-DIALOG-ROUTE-KEYWORD-OCR-WHOLE-MECHANICS-1`；状态=`READY_AFTER_OCR_PROVIDER_PARITY`；
  唯一目标 `service/dialog/DialogRouteKeywordOptionLocalMacroMechanics.java`。当前 `TextRecognizer` 明确仅 local-only，
  未覆盖 696 route OCR provider/fallback 接受域；未闭合前不得以空 OCR 或单 provider 改写两次 attempt/650ms/候选顺序。

### C next primary - white-story template whole observation

- task=`W-696-DIALOG-WHITE-STORY-TEMPLATE-WHOLE-OBSERVATION-1`；状态=`READY_AFTER_C_CURRENT_RELEASE`。
- 唯一 Java 写集：create-new DHXY
  `src/main/java/com/bot/dhxy/service/dialog/DialogWhiteStoryTemplateLocalObservationMechanics.java`；spec/intent/evidence/
  result/terminal 均为类内 immutable types。
- `696a12b0` anchors=`DialogService.verifyWhiteStoryTemplate:451-499`、
  `prepareWhiteStoryTemplate*:935-1031`、miss/absent prepared-result 构造 `:1032-1097`。本地只做 supplied detection
  优先、否则 exact detect/capture、同帧 thin-white wash、caller-order template `0.85` first-hit 和 screen-absolute
  match point；Cloud 保留 operation/targetKeyword、STORY_MISS/STORY_ABSENT 业务 action 构造与 fallback。
- 真实 producer：`DialogDetectionLocalMechanics`、`BoundWindowCaptureService`、`ImageFinder`、`CoordinateHelper`、
  `WindowNativeBindingRefreshService`；696 缺失的 thin-white wash 作为本完整大类 private CPU 段原样实现，不改
  `ImagePreprocessor`，不拆 helper 单。
- terminal=`MATCHED/STORY_MISS/STORY_ABSENT/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/MECHANICS_FAILED`；
  `MATCHED` 携 spec/template、relative+absolute point、raw/washed PNG evidence，其余不得带虚假点位。全程零输入。
- 验收门：supplied/no-supplied capture 次数、STORY type gate、模板原序/0.85/first-hit、同帧坐标、每图单一 owner/
  flush；唯一后续接线门是 shared wire 释放后接 Cloud `prepareWhiteStoryTemplate*`，Cloud 继续构造业务 result。
- backup=`W-696-OBJECTIVE-TEXT-RECOGNITION-WHOLE-CLOUD-ALGORITHM-1`；状态=
  `READY_AFTER_TEMPLATE_AND_MAP_PLAUSIBILITY_PREREQUISITE`；目标为完整大型 Cloud
  `src/main/java/com/bot/dhxy/vision/ObjectiveTextRecognitionService.java`。当前 objective template resource authority、
  map-coordinate plausibility collaborator 与 task-context producer 未闭合，故不预排为 READY，也不把算法下沉本地。

### D next primary - NPC yellow-target whole local image mechanics

- task=`W-696-NPC-YELLOW-TARGET-WHOLE-LOCAL-OBSERVATION-1`；状态=`READY_AFTER_D_CURRENT_RELEASE`。
- 唯一 Java 写集：create-new DHXY
  `src/main/java/com/bot/dhxy/service/npc/NpcClickYellowTargetLocalObservationMechanics.java`；完整覆盖 yellow-target
  local image lifecycle，但明确不是 `NpcClickService` 整类完成。
- `696a12b0` anchors=`NpcClickService.tryYellowTargetStrategy:1052-1081`、
  `clickNpcByYellowTargetName:1933-2048`、`findYellowTextFallbackCandidates:2436+`。本地按 caller 给定 exact
  scan region 完成 fresh binding→single capture→yellow wash→ordered connected/shape fallback candidates→raw/washed
  PNG evidence；Cloud 继续持有 NPC name normalization/matching、candidate-region loop、是否点击、联合 player-anchor、
  verify/fallback 顺序。
- 真实 producer：`BoundWindowCaptureService`、`WindowNativeBindingRefreshService`、
  `ImagePreprocessor.washYellowTextToBlackAndWhite` 与 JDK/OpenCV 现有 image primitives；不依赖不存在的 Cloud holder/fact。
- terminal=`CAPTURED/NO_YELLOW_CANDIDATE/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/INTERRUPTED/MECHANICS_FAILED`；
  `CAPTURED` 携 scan rect、ordered shape candidates、raw/washed evidence；零 OCR business verdict、零 input。
- 验收门：区域/坐标空间、yellow polarity/cleanup、candidate order、单帧/flush、无 target-name 决策；唯一后续接线门
  是 shared wire 释放后传图像观察到 Cloud 原 `tryYellowTargetStrategy`，由 Cloud OCR/匹配后另发 ordered click bundle。
- backup=`W-696-CLIENT-IDENTITY-WHOLE-BOUNDARY-1`；状态=`NEEDS_USER_DECISION`，不预留 Java 文件。窗口标题解析可
  全本地后上送 typed identity，也可 Cloud 解析本地 raw title；两案都可且用户尚未选，故不得擅自发单或占 shared wire。

### 四路 ownership / conflict gate

| 路 | primary 唯一新文件 | 当前/其它路交集 |
|---|---|---|
| A | `AutoCombatPanelRoundsLocalObservationMechanics.java` | 与 A 当前 visibility 类不同；与 B/C/D 零交集 |
| B | `DialogPreparedActionValidationLocalMechanics.java` | 不碰 B/C PlayerState shared 六类文件；与 C/D 新类不同 |
| C | `DialogWhiteStoryTemplateLocalObservationMechanics.java` | 不碰 Cloud PlayerState 或 shared wire；与 B/D 新类不同 |
| D | `NpcClickYellowTargetLocalObservationMechanics.java` | 不碰 green-template 类、A auto-combat 或 B/C shared wire |

- 四个 primary 均不修改既有 `LocalRemoteGameCommandHandler`、remote enum/codec/digest/schema；这些未来接线必须等
  B/C 当前 shared wave 被父级释放后另行互斥排程。backups 的目标也互不相同，未满足前置者不占写集。
- 未新增 owner/permit/session/ledger/compaction/durable workflow/business TTL/auto retry；无已批准业务差异；
  按 `696a12b0` 基线等价迁移。

## Candidate Queue Rebase #15 - 一条完整双端链 + 三条连续 mechanics

记录时间：`2026-07-14T20:04:28-04:00`。本段仅供父级后续发单，不是 APPROVED/BLOCKED 裁决。

### 当前 reservation 与强制先后

- A 当前独占 DHXY `service/autocombat/AutoCombatPanelVisibilityLocalMacroMechanics.java`；其 Implementation 已交，
  父级释放前不得由任何后继修改。
- B 当前 R2 独占 DHXY `PlayerStateFirstAidLocalMacroMechanics.java`、
  `RemotePlayerStateFirstAidMacroResultPayload.java`；C 当前 R2 独占 Cloud
  `PlayerStateFirstAidMacroResult.java`；D 当前 R2 独占 DHXY
  `DialogGreenTemplateOptionLocalMacroMechanics.java`。各自父级释放前，后继只排队、不抢原 Worker 返修。
- **先后规则**：A/C/D 的 primary 均只 create-new 独立大类，可在各自当前文件释放后并行；B 的 primary 是
  唯一 shared-wire owner，必须等 B/C PlayerState R2 均被父级释放且没有其它 remote writer 后再领取。B 领取后，
  A/C/D 仍不得修改任何 remote enum/codec/digest/handler，直到 B 完整双端链交付。

### A primary - rounds observation 完整连续 mechanics

- task=`W-696-AUTO-COMBAT-PANEL-ROUNDS-WHOLE-OBSERVATION-1`；状态=`READY_AFTER_A_VISIBILITY_RELEASE`。
- 唯一 Java 写集：create-new DHXY
  `src/main/java/com/bot/dhxy/service/autocombat/AutoCombatPanelRoundsLocalObservationMechanics.java`；完整实现
  `696a12b0 AutoCombatPanelService:322-457` 的 panel-match→scan rect 两分支→single capture→4x red wash→
  black-pixel count→local OCR→首个 `\\d{1,2}` terminal，不修改 A 当前 visibility 类。
- reachable owner：Cloud `verifyAndAlignPanel -> refreshAutoCombatRoundsIfNeeded -> readRemainingRounds` 继续持有
  refresh reason、estimate/state/timestamp/Alt+8；本地只消费 screen-absolute center/nullable marker/template width。
- terminal=`ROUNDS_READ/NO_DIGITS/CAPTURE_UNAVAILABLE/OCR_UNAVAILABLE/BINDING_UNAVAILABLE/MECHANICS_FAILED`；
  `ROUNDS_READ` 携 rounds，所有观察结果携 rect/redPixels/OCR text/defensive evidence，不携业务 refresh verdict。
- 验收：marker branch 与 center fallback、4x RGB predicate、OCR 拼接/regex、capture 数、owner/flush、零 input、
  无 retry/TTL；后续 wire 接线不得与本任务混做。
- backup=`W-696-NPC-PLAYER-ANCHOR-FORMULA-WHOLE-MECHANICS-1`，状态=
  `READY_AFTER_OCR_PROVIDER_PARITY`；目标仅 `NpcClickPlayerAnchorFormulaLocalMacroMechanics.java`。当前 local-only
  OCR 不覆盖 696 provider fallback，故未解除前不发单。

### B primary - Dialog prepared-action validation 完整双端 reachable chain

- task=`W-696-DIALOG-PREPARED-ACTION-VALIDATION-FULL-CHAIN-1`；状态=
  `READY_AFTER_B_C_SHARED_RELEASE`。这是 #15 至少一条必须一次闭合的完整
  `Cloud public caller -> DialogService -> typed port/contract -> DHXY handler/mechanics -> typed terminal -> caller result`
  真链，不以 DTO/helper 或单个 mechanics 计交付。

#### B exact Cloud Java write set（10）

1. create `com/yueyunfe/dhxy/cloudbrain/remote/DialogPreparedActionValidationMacroCommand.java`
2. create `com/yueyunfe/dhxy/cloudbrain/remote/DialogPreparedActionValidationMacroResult.java`
3. create `com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogPreparedActionValidationPort.java`
4. modify `com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroKind.java`
5. modify `com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroCommand.java`
6. modify `com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroRequest.java`
7. modify `com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroOutcome.java`
8. modify `com/yueyunfe/dhxy/cloudbrain/remote/RemoteCommandOutcomeEnvelope.java`
9. modify `com/yueyunfe/dhxy/cloudbrain/remote/RemoteProtocolDigests.java`
10. modify `com/bot/dhxy/service/DialogService.java`

#### B exact DHXY Java write set（9）

1. create `com/bot/dhxy/cloud/remote/RemoteDialogPreparedActionValidationMacroCommandPayload.java`
2. create `com/bot/dhxy/cloud/remote/RemoteDialogPreparedActionValidationMacroResultPayload.java`
3. create `com/bot/dhxy/service/dialog/DialogPreparedActionValidationLocalMechanics.java`
4. modify `com/bot/dhxy/cloud/remote/RemoteLocalMacroKind.java`
5. modify `com/bot/dhxy/cloud/remote/RemoteLocalMacroCommandPayload.java`
6. modify `com/bot/dhxy/cloud/remote/RemoteLocalMacroResultPayload.java`
7. modify `com/bot/dhxy/cloud/remote/RemoteOperationPayloadCodec.java`
8. modify `com/bot/dhxy/cloud/remote/RemoteProtocolDigests.java`
9. modify `com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java`

#### B baseline / reachability / terminal

- `696a12b0` anchors=`DialogService.captureDialogValidationImage:1143-1157`、
  `validatePreparedDialogActionForConsume:1158-1222`、distance gate `:1224-1229`、wash switch `:1231-1249`。
  现有 reachable callers 不改签名：`tryConsumePreparedRememberedRouteOption`、`XiuluoTaskV2`、`WubeiTask`、
  `NavigationService` 均继续调用 Cloud 同一 public method。
- Cloud 保留 action null/clickRequired/missing fingerprint/invalid rect gate，按 operation 计算 baseline maxDistance
  `8/16`，仅把 exact rect、washMode、expected fingerprint、maxDistance、reason 发给 port；VALIDATED 后由 Cloud
  用自己的 wall-clock 刷新原 action `lastVerifiedAtMs`。DHXY 不选择 action/target/fallback/timestamp。
- DHXY mechanics 在 queue 外用 exact HWND fresh geometry 单次 capture，完整执行 YELLOW/GREEN/WHITE/default wash、
  binary fingerprint 与 distance；不发送输入。command/result 双侧 closed constructor、request/outcome digest 与 flat
  payload key set必须一致。
- transport terminal：`EXECUTED` 必须携匹配 macroKind 的 typed result；`NOT_EXECUTED` 映射原 null；
  `STOPPED/UNKNOWN` 按现有 port 规则中止，不自动重发。typed state=
  `VALIDATED/FINGERPRINT_MISMATCH/CAPTURE_UNAVAILABLE/INVALID_RECT/BINDING_UNAVAILABLE/MECHANICS_FAILED`；
  仅前两态携 current fingerprint/distance/maxDistance，VALIDATED 才返回 refreshed action，其余保持原 null 语义。
- 完整性验收：四个现有 caller 可达、Service public/private 判断顺序不变、单 capture、wash/fingerprint bit parity、
  8/16 gate、terminal exhaustiveness、两仓 exact key/digest parity、handler 真调用 mechanics、无 desktop authority 进入
  Cloud、无输入/owner/session/ledger/TTL/retry。
- backup（shared 尚未释放时）=`W-696-DIALOG-PREPARED-ACTION-VALIDATION-WHOLE-MECHANICS-1`，状态=
  `READY_NOW_NON_SHARED_BACKUP`；只 create 上述 DHXY mechanics 大类，先闭合完整 capture→wash→fingerprint→distance
  生命周期。之后 full-chain 主单必须把该文件改为 frozen read-only，并仅实施余下 18 文件，避免重复实现。

### C primary - white-story template 完整连续 observation

- task=`W-696-DIALOG-WHITE-STORY-TEMPLATE-WHOLE-OBSERVATION-1`；状态=`READY_AFTER_C_R2_RELEASE`。
- 唯一 Java 写集：create-new DHXY
  `src/main/java/com/bot/dhxy/service/dialog/DialogWhiteStoryTemplateLocalObservationMechanics.java`；完整覆盖
  `DialogService:451-499,935-1097` 的 supplied detection 优先、STORY gate、same-frame thin-white wash、caller-order
  template `0.85` first-hit、relative/screen-absolute match 与 defensive evidence。thin-white CPU wash 内聚于本大类，
  不另拆 helper、不修改 `ImagePreprocessor`。
- Cloud 后续继续构造 operation/targetKeyword、STORY_MISS/STORY_ABSENT prepared action；本地 terminal 仅
  `MATCHED/STORY_MISS/STORY_ABSENT/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/MECHANICS_FAILED`，全程零 input。
- 验收：supplied/no-supplied capture matrix、同帧、模板原序/阈值/first-hit、point space、owner/flush；不碰 B shared
  文件，未来 wire 必须在 B full chain 释放后另排。
- backup=`W-696-OBJECTIVE-TEXT-RECOGNITION-WHOLE-CLOUD-ALGORITHM-1`，状态=
  `READY_AFTER_TEMPLATE_MAP_AND_CONTEXT_PREREQUISITE`；完整 Cloud 大类目标存在性已核为空，但 template authority、
  map plausibility collaborator、task-context producer 未闭合，故当前不伪 READY。

### D primary - NPC yellow-target 完整连续 image mechanics

- task=`W-696-NPC-YELLOW-TARGET-WHOLE-LOCAL-OBSERVATION-1`；状态=`READY_AFTER_D_R2_RELEASE`。
- 唯一 Java 写集：create-new DHXY
  `src/main/java/com/bot/dhxy/service/npc/NpcClickYellowTargetLocalObservationMechanics.java`；完整覆盖
  `NpcClickService:1052-1081,1933-2048,2436+` 中可下沉的 exact scan region→fresh binding→single capture→yellow
  wash→ordered shape candidates→raw/washed evidence 连续机械段。
- Cloud 保留 NPC name normalization/OCR 业务匹配、candidate-region loop、联合 player-anchor、是否点击、verify/fallback；
  本地 terminal=`CAPTURED/NO_YELLOW_CANDIDATE/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/INTERRUPTED/
  MECHANICS_FAILED`，零 target verdict、零 input。
- 验收：yellow polarity/OpenCV cleanup、candidate order、坐标空间、单帧与 image owner/flush；未来 typed image wire
  在 B full chain 释放后另排，不与本单混做。
- backup=`W-696-CLIENT-IDENTITY-WHOLE-BOUNDARY-1`，状态=`NEEDS_USER_DECISION`，不预留文件；raw window title
  在本地解析或上送 Cloud 解析均可，用户未选前不得擅自实施。

### #15 全局互斥与交付定义

| 路 | primary ownership | 与其它路关系 |
|---|---|---|
| A | 仅新 `AutoCombatPanelRoundsLocalObservationMechanics.java` | 不改 A 当前 visibility；不碰 B shared/C/D |
| B | 上述双仓 19 文件，唯一 shared-wire owner | 必须排在 B/C R2 释放后；期间冻结 A/C/D 所有 remote 改动 |
| C | 仅新 `DialogWhiteStoryTemplateLocalObservationMechanics.java` | 与 B 的 DHXY validation mechanics 为不同文件 |
| D | 仅新 `NpcClickYellowTargetLocalObservationMechanics.java` | 与 D 当前 green class、A auto-combat、B/C 均不同 |

- B primary 只有在 caller→Service→port→transport→handler→mechanics→typed terminal→Service return 全部可达时才算该
  cohort 交付；只建 DTO/port 或只编译不算完成。A/C/D 只按完整连续 mechanics 验收，不冒充对应 Service 整类完成。
- 无新增 owner/permit/session/ledger/compaction/durable workflow/business TTL/auto retry；无已批准业务差异；
  按 `696a12b0` 基线等价迁移。

## Candidate Queue Rebase #16 - 当前四路交付后的大 cohort 预排

记录时间：`2026-07-14T20:21:51-04:00`。本段只是 Next-Task Queue Helper 的非绑定排班建议，
不构成源码 APPROVED/BLOCKED，也不抢当前 Worker 的返修或父级审查。

### 当前 reservation 与启动门

- A 的 `AutoCombatPanelRoundsLocalObservationMechanics.java` 已交付、待父级审查；D 的
  `DialogGreenTemplateOptionLocalMacroMechanics.java` R3 已交付、待父级审查。两文件释放前只排队。
- C 已领取 `DialogWhiteStoryTemplateLocalObservationMechanics.java`；B 已领取
  `W-696-DIALOG-PREPARED-ACTION-VALIDATION-FULL-CHAIN-1` 的双仓 19 文件 shared-wire 写集。
- B 当前报告的唯一技术前置是 DHXY `ImagePreprocessor` 缺四个 committed CPU 方法；该前置由父级调整当前 B
  写集后处理。本段不绕过、不复制到其它类，也不让 A/C/D 触 shared wire。

### A primary - NPC yellow-target 完整连续本地图像观察

- task=`W-696-NPC-YELLOW-TARGET-WHOLE-LOCAL-OBSERVATION-2`；状态=`READY_AFTER_A_ROUNDS_RELEASE`。
- 唯一 Java 写集：create-new DHXY
  `src/main/java/com/bot/dhxy/service/npc/NpcClickYellowTargetLocalObservationMechanics.java`。
- `696a12b0` anchors=`NpcClickService.tryYellowTargetStrategy:1052-1081`、
  `clickNpcByYellowTargetName:1933-2048`、`findYellowTextFallbackCandidates:2436+`。完整机械段为 caller 给定
  screen-absolute scan rect -> exact binding/fresh geometry -> single capture -> baseline yellow wash/OpenCV cleanup ->
  ordered connected/shape candidate extraction -> raw/washed PNG evidence。
- Cloud `NpcClickService` 保留 candidate-region loop、NPC 名称 normalization/OCR 匹配、player-anchor 联合策略、
  click/verify/fallback 顺序；本地不判断目标、不点击。typed terminal=`CAPTURED/NO_YELLOW_CANDIDATE/
  CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/INTERRUPTED/MECHANICS_FAILED`。
- 验收：scan rect/坐标空间、yellow polarity/cleanup、candidate order、单 capture、image owner/flush 与 696 逐段一致；
  后续唯一接线门是 B shared wire 释放后给 Cloud 原 yellow strategy 增加 typed observation port。
- backup=`W-696-NPC-PLAYER-ANCHOR-FORMULA-WHOLE-MECHANICS-1`；状态=
  `READY_AFTER_OCR_PROVIDER_PARITY`。696 的 provider fallback 尚未由现有 local-only OCR 覆盖，前置未满足前不发单。

### C primary - NPC player-anchor 完整连续图像观察

- task=`W-696-NPC-PLAYER-ANCHOR-IMAGE-WHOLE-OBSERVATION-1`；状态=
  `READY_AFTER_C_WHITE_STORY_RELEASE`。
- 唯一 Java 写集：create-new DHXY
  `src/main/java/com/bot/dhxy/service/npc/NpcClickPlayerAnchorLocalObservationMechanics.java`。
- `696a12b0` anchors=`NpcClickService.tryPlayerAnchorFormulaStrategy:998+`、
  `prepareNpcOcrScanImage:2505+`、`calculatePlayerAnchorFormulaPoint:2865-2996`、
  `extractPurpleBlobAnchor:3132+`。本类只闭合 exact scan rect -> fresh binding -> single capture -> baseline purple
  wash/foreground cleanup -> ordered purple-blob anchor candidates -> raw/washed evidence 的完整连续机械生命周期。
- Cloud 保留玩家名 OCR/匹配、地图坐标差、公式/tune/bounds、候选选择、点击与验证 fallback；本地不做公式、
  不下目标 verdict、不输入。typed terminal=`CAPTURED/NO_PURPLE_CANDIDATE/CAPTURE_UNAVAILABLE/
  BINDING_UNAVAILABLE/INTERRUPTED/MECHANICS_FAILED`。
- 验收：696 purple predicate/cleanup、候选原序、screen-absolute point、单帧、每图单一 owner/flush；未来接线只在
  shared wire 释放后追加 typed image observation，不修改 DHXY `NpcClickService` 的业务 loop。
- backup=`W-696-OBJECTIVE-TEXT-RECOGNITION-WHOLE-CLOUD-ALGORITHM-1`；状态=
  `READY_AFTER_TEMPLATE_MAP_AND_CONTEXT_PREREQUISITE`。template authority、map plausibility collaborator 与真实
  task-context producer 尚未同时闭合，故不伪标 READY。

### D primary - Dialog option OCR 图像准备完整连续 mechanics

- task=`W-696-DIALOG-OPTION-OCR-IMAGE-WHOLE-OBSERVATION-1`；状态=`READY_AFTER_D_GREEN_R3_RELEASE`。
- 唯一 Java 写集：create-new DHXY
  `src/main/java/com/bot/dhxy/service/dialog/DialogOptionOcrImageLocalObservationMechanics.java`。
- `696a12b0` anchors=`DialogService.processOptionsWithOCRDetailed:1792+`、
  `buildPreparedDialogAction:1898+`、`selectOcrFallbackOption:2134+`。完整机械段严格执行 supplied detection
  frame/rect 优先，否则 exact-window fresh capture；随后按 696 顺序生成 dialog-option/green 与 yellow 两种 OCR
  图像变体，并返回 raw/green/yellow PNG evidence + screen-absolute rect。
- Cloud 保留 OCR words 解释、target alias/name 匹配、fallback option、prepared-action/result 构造与是否点击；本地不选
  option、不构造 action、不输入。typed terminal=`CAPTURED/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/
  INVALID_SUPPLIED_FRAME/MECHANICS_FAILED`。
- 真实现有依赖为 `BoundWindowCaptureService`、`WindowNativeBindingRefreshService` 及 DHXY 既有
  `ImagePreprocessor.washDialogOptionTemplateTextToBlackAndWhite`/`washYellowTextToBlackAndWhite`；不依赖 B 当前
  缺失的 green/thin-white/fingerprint 新表面。验收 supplied/no-supplied capture matrix、图像顺序/尺寸、坐标与 flush。
- backup=`W-696-CLIENT-IDENTITY-WHOLE-BOUNDARY-1`；状态=`NEEDS_USER_DECISION`，不预留文件。raw window title
  在本地解析或送 Cloud 解析两案均可，用户未选前不发单。

### B next primary - white-story template 完整双端 reachable chain

- task=`W-696-DIALOG-WHITE-STORY-TEMPLATE-FULL-CHAIN-1`；状态=
  `READY_AFTER_B_CURRENT_FULL_CHAIN_AND_C_WHITE_STORY_RELEASE`。这是当前 B 19 文件链完成后的下一条完整
  `public caller -> Cloud DialogService -> typed port/contract -> DHXY handler/mechanics -> typed terminal -> Cloud result`
  真链；C 已批准 mechanics 到时只读复用。

#### B next exact Cloud Java write set（10）

1. create `com/yueyunfe/dhxy/cloudbrain/remote/DialogWhiteStoryTemplateMacroCommand.java`
2. create `com/yueyunfe/dhxy/cloudbrain/remote/DialogWhiteStoryTemplateMacroResult.java`
3. create `com/yueyunfe/dhxy/cloudbrain/remote/CloudDialogWhiteStoryTemplatePort.java`
4. modify `com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroKind.java`
5. modify `com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroCommand.java`
6. modify `com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroRequest.java`
7. modify `com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroOutcome.java`
8. modify `com/yueyunfe/dhxy/cloudbrain/remote/RemoteCommandOutcomeEnvelope.java`
9. modify `com/yueyunfe/dhxy/cloudbrain/remote/RemoteProtocolDigests.java`
10. modify `com/bot/dhxy/service/DialogService.java`

#### B next exact DHXY Java write set（8）

1. create `com/bot/dhxy/cloud/remote/RemoteDialogWhiteStoryTemplateMacroCommandPayload.java`
2. create `com/bot/dhxy/cloud/remote/RemoteDialogWhiteStoryTemplateMacroResultPayload.java`
3. modify `com/bot/dhxy/cloud/remote/RemoteLocalMacroKind.java`
4. modify `com/bot/dhxy/cloud/remote/RemoteLocalMacroCommandPayload.java`
5. modify `com/bot/dhxy/cloud/remote/RemoteLocalMacroResultPayload.java`
6. modify `com/bot/dhxy/cloud/remote/RemoteOperationPayloadCodec.java`
7. modify `com/bot/dhxy/cloud/remote/RemoteProtocolDigests.java`
8. modify `com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java`

#### B next reachability / terminal / acceptance

- `696a12b0` anchors=`DialogService.verifyWhiteStoryTemplate:449-499`、public
  `prepareWhiteStoryTemplate*:924-999` 与 miss/absent result construction `1000-1097`；真实 caller 继续调用同一 Cloud
  public API，不改变签名。Cloud 只把 supplied detection/spec 原序/operation context 发 port，并保留
  STORY_MISS/STORY_ABSENT prepared-action、target keyword、fallback 和 timestamps。
- DHXY handler 用 exact context 调 C 的 frozen
  `DialogWhiteStoryTemplateLocalObservationMechanics`；transport terminal=`EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN`，
  typed state=`MATCHED/STORY_MISS/STORY_ABSENT/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/MECHANICS_FAILED`。
  `MATCHED` 携 template/spec、relative + screen-absolute point 和 defensive image evidence；失败态不得伪造点位。
- 验收：public caller 可达、same-frame supplied/no-supplied matrix、template caller order/0.85/first-hit、两仓 closed
  constructor/key/digest parity、handler 真调用 mechanics、Cloud 业务构造顺序不变、无 desktop authority 进入 Cloud。
- backup=`W-696-DIALOG-GREEN-TEMPLATE-OPTION-FULL-CHAIN-1`；状态=
  `READY_AFTER_B_CURRENT_FULL_CHAIN_AND_D_GREEN_RELEASE`。它复用 D frozen green mechanics，使用同一 shared-wire 文件族
  的 green command/result/port + Cloud `DialogService` 接线；与 white-story primary 是顺序二选一，绝不并发。

### #16 ownership / conflict proof

| 路 | 后继 primary ownership | 并发/先后 |
|---|---|---|
| A | 仅新 `NpcClickYellowTargetLocalObservationMechanics.java` | A rounds 释放后；不碰 B/C/D |
| C | 仅新 `NpcClickPlayerAnchorLocalObservationMechanics.java` | C white-story 释放后；不碰 shared wire |
| D | 仅新 `DialogOptionOcrImageLocalObservationMechanics.java` | D green R3 释放后；不碰 shared wire |
| B | 上述双仓 18 文件；唯一 shared-wire owner | 当前 B 19 文件链和 C white-story 均释放后 |

- A/C/D 三个新类彼此不同，且不修改 B 当前或下一单的 enum/codec/digest/handler/Cloud `DialogService`；B 启动后
  继续冻结其它路所有 shared remote 文件。B backup 与 B primary 共享写集，只能二选一顺序执行。
- 永久本地 `BagService/UICleanerService/GiveItemService/QuestManagerService` 不进入 Cloud；无新增
  owner/permit/session/ledger/compaction/durable workflow/business TTL/auto retry。
- A/C/D 的交付只算完整 local mechanics prerequisite，不冒充对应 Service 整类完成；B 必须以完整双端可达链验收。
  无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #17 - Dialog 大链在途时的下一波互斥队列

记录时间：`2026-07-14T20:40:00-04:00`。本段仅为父级后续发单准备 Implementation brief，不作源码裁决。

### 当前占用与释放条件

- A 已交 `DialogOptionOcrImageLocalObservationMechanics.java` 首版，父级释放前后继不得修改该文件。
- B 正独占 prepared-action validation 双仓 20 文件（原 19 文件 + DHXY `ImagePreprocessor.java`）；所有 shared
  local-macro enum/codec/digest/handler 与 Cloud `DialogService` 继续只归 B。
- C 的 `DialogWhiteStoryTemplateLocalObservationMechanics.java` 首版已有五项精确返修条件，R1 仍只锁该文件。
- D 已领取 yellow-target mechanics；真实 EOF 已说明当前目标文件尚未创建，缺少 696 shape-candidate producer。
  后继 D 任务必须等该 producer 与原任务均由父级释放，不能用 OCR word box 冒充 shape candidate。

### A next primary - NPC prepared-point 点击与验证完整 closed mechanics

- task=`W-696-NPC-PREPARED-POINT-CLICK-VERIFY-WHOLE-MECHANICS-1`；状态=
  `READY_AFTER_A_DIALOG_OPTION_IMAGE_RELEASE`。
- 唯一 Java 写集：create-new DHXY
  `src/main/java/com/bot/dhxy/service/npc/NpcClickPreparedPointLocalMacroMechanics.java`。
- `696a12b0` anchors=`NpcClickService.executeMoveClickAndVerify:176-216`、
  `executeClickAndVerifyDirect:218-238`、`clickNpcByPlayerAnchorFormula:3011-3055`。command 只携 Cloud 已选定的
  screen-absolute point、first-wait、closed `DIALOG/BATTLE/EITHER` verifier mode 与 baseline retry count；本地不产生
  candidate、不算公式、不选策略。
- complete mechanics：必须在唯一 input worker 内 direct 执行 move -> 150ms -> click(hold 150ms) -> first wait ->
  exact-window local verify；仅按 command 中 696 原有 0/1 retry 执行 retry move/click/1000ms/verify，所有 exit 保持
  stop/interrupt 与坐标归属。真实现有观察者为 `DialogDetectionLocalMechanics` 和
  `BattleRadarLocalObservationMechanics`，禁止 queue-in-queue。
- terminal=`VERIFIED/NOT_VERIFIED/BINDING_UNAVAILABLE/NON_INPUT_WORKER/INTERRUPTED/MECHANICS_FAILED`；仅
  `VERIFIED/NOT_VERIFIED` 携 attempts 与最后 verifier evidence。验收 150/150/1000ms、重试次数、move+click 原子性、
  exact binding、terminal exhaustiveness；未来 wire 只负责传 prepared point，Cloud 保留全部业务策略。
- backup=`W-696-NPC-PLAYER-ANCHOR-IMAGE-WHOLE-OBSERVATION-2`；状态=
  `READY_AFTER_A_RELEASE_AS_ALTERNATIVE`；只 New
  `service/npc/NpcClickPlayerAnchorLocalObservationMechanics.java`，完整做 fresh capture -> purple wash -> bounded
  blob evidence，Cloud 保留玩家名 OCR、地图公式、candidate 选择与点击；不冒充完整 OCR->blob fallback。

### C next primary - 696 yellow text shape-candidate 纯 CPU mechanics 闭包

- task=`W-696-GAME-TEXT-CANDIDATE-WHOLE-LOCAL-MECHANICS-1`；状态=
  `READY_AFTER_C_WHITE_STORY_R1_RELEASE`。该 cohort 直接提供 D 当前 yellow-target 所缺的真实 shape producer，
  不是 DTO/helper-only 小单。
- 精确 DHXY Java 写集（4 New）：
  1. `src/main/java/com/bot/dhxy/vision/GameTextLineCandidateLocalMechanics.java`
  2. `src/main/java/com/bot/dhxy/model/ocr/TextCandidate.java`
  3. `src/main/java/com/bot/dhxy/model/ocr/TextCandidateScanResult.java`
  4. `src/main/java/com/bot/dhxy/model/ocr/TextCandidateScanStatus.java`
- 权威为 `696a12b0:vision/GameTextLineOcrService.java:182+ findYellowTextCandidateResult` 及其完整 private closure：
  yellow-NPC mask、nearby shadow、connected components、line merge/penalty/score/order、washed image 与 overlay；三个
  model blob 分别为 `5fb5d8dc832961e213ac612b332b1ff2cbd8698c`、
  `4c9e57cf39fd9f5dab28ea21670ba49e0fc68c00`、`b34b631a25272d648e836fb5f44be0f34b3fee1a`。
- 本 mechanics 只接 caller-owned `BufferedImage`/optional debug paths，返回 image-local、score-sorted immutable
  candidates；零 capture/OCR/input/target-name verdict。D 后续只把 caller crop origin 加到 box/click point，Cloud 仍做
  名称/OCR 匹配、region loop、player-anchor、click/verify/fallback。
- 验收：所有阈值、component filters、merge/order/score 与 696 一致；owned mask/overlay flush；空图、无候选、异常
  terminal 分开；不得调用 Cloud `CloudImageProcessor`，不得恢复依赖 provider fallback 的 OCR methods。
- backup=`W-696-GAME-TEXT-LINE-OCR-WHOLE-LOCAL-SERVICE-RESTORE-1`；状态=
  `READY_AFTER_TEXT_RECOGNIZER_PROVIDER_PARITY`；目标为完整 `GameTextLineOcrService` + `OcrLineResult/
  TargetOcrResult/TextCandidate*` 六文件闭包。当前 `TextRecognizer` 只有 local-only API，未覆盖 696
  `getAllTextResultsForMatch` provider fallback，前置未定前不发该 backup。

### D next primary - 单 variant Dialog OCR typed local observation

- task=`W-696-DIALOG-OPTION-OCR-WORDS-WHOLE-OBSERVATION-1`；状态=
  `READY_AFTER_D_YELLOW_RELEASE_AND_A_IMAGE_RELEASE`。
- 唯一 Java 写集：create-new DHXY
  `src/main/java/com/bot/dhxy/service/dialog/DialogOptionOcrWordsLocalObservationMechanics.java`；只读复用 A frozen
  image evidence，不修改 A 文件或 B shared wire。
- `696a12b0` anchors=`DialogService.processOptionsWithOCRDetailed:1792-1895`、
  `GameTextLineOcrService.readDialogOptionWords`。每次 command 只指定一个 closed `GREEN/YELLOW` variant 与该 variant
  PNG bytes/rect；mechanics 校验 PNG/SHA/尺寸，写 window-scoped 临时图，调用现有
  `TextRecognizer.getAllTextResultsLocalOnly` 恰一次，并返回 caller-order image-local word boxes。
- Cloud 后续严格保留 green-first -> alias 判断 -> 仅 miss 才请求 yellow -> alias/fallback/action/click 的 696 顺序；
  本地不接 target keyword、不匹配 alias、不选 fallback、不点击，也不额外读取另一 variant。
- terminal=`WORDS/NO_WORDS/OCR_UNAVAILABLE/INVALID_IMAGE/MECHANICS_FAILED`；`WORDS/NO_WORDS` 均证明 sidecar
  已响应，只有 `WORDS` 携 immutable boxes。验收单 variant 单 OCR、坐标不越界、temp path window-scoped、bytes
  defensive copy、无 provider miss 伪装视觉 miss。
- backup=`W-696-AUTO-COMBAT-PANEL-VISIBILITY-ROUNDS-FULL-CHAIN-1`；状态=
  `READY_AFTER_B_SHARED_RELEASE`。它顺序复用已释放的 visibility/rounds mechanics，新增双仓 closed contract/port/
  handler 并接 Cloud `AutoCombatPanelService` 原调用点；因与 B shared 文件同族，只能在 B 当前及下一 shared 单后排。

### B next - white-story template 完整 caller integration

- task=`W-696-DIALOG-WHITE-STORY-TEMPLATE-FULL-CHAIN-1`；状态=
  `READY_AFTER_B_PREPARED_ACTION_CHAIN_AND_C_WHITE_STORY_R1_RELEASE`。
- 精确写集沿用 #16：Cloud 3 New（command/result/port）+ 7 Modify（四个 LocalMacro envelope、
  `RemoteCommandOutcomeEnvelope`、digest、`DialogService`）；DHXY 2 New payload + 6 Modify（kind/command/result/
  codec/digest/handler）。C 的 white-story mechanics 只读，B 不修改其文件。
- reachable chain=`prepareWhiteStoryTemplate* public caller -> Cloud DialogService.verifyWhiteStoryTemplate ->
  Cloud port/contract -> LocalMacro transport -> DHXY handler -> C mechanics -> typed terminal -> Cloud miss/absent/
  matched PreparedDialogAction result`。terminal、same-frame evidence、0.85/caller-order template 与两仓 exact key/digest
  gate均按 #16，不改变 target/fallback/timestamp 顺序。
- backup=`W-696-DIALOG-GREEN-TEMPLATE-OPTION-FULL-CHAIN-1`；状态=
  `READY_AFTER_B_PREPARED_ACTION_CHAIN`。D green mechanics 已是 frozen dependency；backup 与 white-story 使用同一
  shared file family，只能二选一顺序实施，不能并发。

### #17 互斥证明与顺序

| 路 | primary 唯一写集 | 与当前/其它路关系 |
|---|---|---|
| A | 新 `NpcClickPreparedPointLocalMacroMechanics.java` | 不碰 B shared、C/D 当前类 |
| C | 新 candidate mechanics + 3 个缺失 model | 不碰 D yellow 目标类；先交 producer 后 D 可续原任务 |
| D | 新 `DialogOptionOcrWordsLocalObservationMechanics.java` | 只读 A image evidence；不碰 B/C 写集 |
| B | #16 双仓 18 文件 shared integration | 当前 20 文件链结束且 C R1 释放后独占 shared wire |

- A/C/D primary 可在各自释放门满足后并行；B next 启动时其它路继续冻结 enum/codec/digest/handler/Cloud
  `DialogService`。B primary/backup 与 D backup 都是 shared-wire 顺序候选，不得并发。
- 永久本地 `BagService/UICleanerService/GiveItemService/QuestManagerService` 不迁 Cloud；没有新增
  owner/permit/session/ledger/compaction/durable workflow/business TTL/auto retry。落点仍有两案的
  `ClientIdentityService` 继续标记 `NEEDS_USER_DECISION`，本节不发单。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #18 - TRUE EOF control record

记录时间：`2026-07-14T20:52:12-04:00`。本段是 Queue #18 的真实 EOF 排班索引；此前同名详细 body 因
append 定位命中旧结尾而不在 EOF，本段重申全部有效 reservation，父级以本段为最新控制记录。

### 强制 supersession 与当前门

- Queue #17 C 的 `W-696-GAME-TEXT-CANDIDATE-WHOLE-LOCAL-MECHANICS-1` =
  `SUPERSEDED_BY_D_SCOPE_CLARIFICATION_1`：零 C reservation，永不转交 C。
- D 当前 yellow-target 单文件内联 `696a12b0 GameTextLineOcrService.findYellowTextCandidateResult` exact closure；
  A OCR-image R1、B validation 20 文件、C white-story R1、D yellow-target 文件在父级释放前均冻结。
- B 当前独占 shared local-macro enum/codec/digest/handler。下一波 B 排完整双端 caller chain；A/C/D 排完整
  continuous local mechanics lifecycle，并各自只有一个后续 caller/wire gate，不计 Service 整类完成。

### A primary / backup

- primary=`W-696-NPC-PREPARED-POINT-CLICK-VERIFY-WHOLE-MECHANICS-1`，
  `READY_AFTER_A_OCR_IMAGE_R1_RELEASE`；唯一写集 New DHXY
  `src/main/java/com/bot/dhxy/service/npc/NpcClickPreparedPointLocalMacroMechanics.java`。
- anchors=`NpcClickService:176-238,3011-3055`；链=`prepared screen point -> exact binding/input-worker ->
  move/150ms/click hold 150ms/wait -> existing dialog/battle verifier -> baseline optional retry/1000ms -> terminal`；
  本地不选 candidate/公式/策略，禁止 queue-in-queue。
- terminal=`VERIFIED/NOT_VERIFIED/BINDING_UNAVAILABLE/NON_INPUT_WORKER/INTERRUPTED/MECHANICS_FAILED`；后续唯一门是
  B 释放 shared wire 后接 Cloud `NpcClickService` 原 prepared-point caller。
- backup=`W-696-NPC-PLAYER-ANCHOR-IMAGE-WHOLE-OBSERVATION-2`，
  `ALTERNATIVE_ONLY_IF_D_PRIMARY_NOT_ISSUED`；唯一文件
  `service/npc/NpcClickPlayerAnchorLocalObservationMechanics.java`，与 D primary 二选一。

### B primary / backup

- primary=`W-696-DIALOG-WHITE-STORY-TEMPLATE-FULL-CHAIN-1`，
  `READY_AFTER_B_VALIDATION_CHAIN_AND_C_WHITE_STORY_R1_RELEASE`。
- Cloud exact 写集：New `DialogWhiteStoryTemplateMacroCommand.java`、
  `DialogWhiteStoryTemplateMacroResult.java`、`CloudDialogWhiteStoryTemplatePort.java`；Modify `LocalMacroKind.java`、
  `LocalMacroCommand.java`、`LocalMacroRequest.java`、`LocalMacroOutcome.java`、`RemoteCommandOutcomeEnvelope.java`、
  `RemoteProtocolDigests.java`、`service/DialogService.java`。
- DHXY exact 写集：New `RemoteDialogWhiteStoryTemplateMacroCommandPayload.java`、
  `RemoteDialogWhiteStoryTemplateMacroResultPayload.java`；Modify `RemoteLocalMacroKind.java`、
  `RemoteLocalMacroCommandPayload.java`、`RemoteLocalMacroResultPayload.java`、`RemoteOperationPayloadCodec.java`、
  `RemoteProtocolDigests.java`、`LocalRemoteGameCommandHandler.java`。
- reachable chain=`prepareWhiteStoryTemplate* -> Cloud DialogService.verifyWhiteStoryTemplate -> typed port -> transport ->
  DHXY handler -> frozen C mechanics -> typed terminal -> PreparedDialogAction`；anchors=`DialogService:449,924-981`。
- envelope=`EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN`；typed state=
  `MATCHED/STORY_MISS/STORY_ABSENT/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/MECHANICS_FAILED`；验收 same-frame、0.85、
  caller-order、strict codec/canonical digest、flat terminal 与 public caller 可达。
- backup=`W-696-DIALOG-GREEN-TEMPLATE-OPTION-FULL-CHAIN-1`，`ALTERNATIVE_AFTER_B_VALIDATION_CHAIN`；与 primary
  共用 shared 文件族，只能顺序二选一。

### C primary / backup

- primary=`W-696-DIALOG-OPTION-OCR-WORDS-WHOLE-OBSERVATION-1`，
  `READY_AFTER_C_WHITE_STORY_R1_AND_A_OCR_IMAGE_R1_RELEASE`；唯一写集 New DHXY
  `src/main/java/com/bot/dhxy/service/dialog/DialogOptionOcrWordsLocalObservationMechanics.java`。
- anchors=`DialogService:1792-1895`、`GameTextLineOcrService.readDialogOptionWords:120+`；链=`one GREEN/YELLOW
  immutable PNG/rect -> SHA/size validation -> window-scoped artifact -> one local OCR -> ordered image-local boxes ->
  terminal`。Cloud 保留 green-first、miss 后 yellow、alias/fallback/action/click。
- terminal=`WORDS/NO_WORDS/OCR_UNAVAILABLE/INVALID_IMAGE/MECHANICS_FAILED`；后续唯一门是 B 释放 shared wire 后接
  Cloud `DialogService` 原 OCR caller。
- backup=`W-696-TASK-TRACKER-PANEL-CLOUD-WHOLE-CLASS-1`，
  `READY_AFTER_EXPLICIT_TASK_CONTEXT_AND_PANEL_CAPTURE_PORT`；前置未闭合时零文件 reservation。

### D primary / backup

- primary=`W-696-NPC-PLAYER-ANCHOR-IMAGE-WHOLE-OBSERVATION-1`，
  `READY_AFTER_D_YELLOW_TARGET_RELEASE`；唯一写集 New DHXY
  `src/main/java/com/bot/dhxy/service/npc/NpcClickPlayerAnchorLocalObservationMechanics.java`。
- anchors=`NpcClickService:2505+,2865-2996,3132+`；链=`fresh exact-window capture -> default-mask prep -> purple
  wash/foreground cleanup -> ordered connected-purple-blob evidence -> terminal`。Cloud 保留玩家名 OCR、地图公式、
  candidate 选择、click/verify/fallback。
- terminal=`CAPTURED/NO_PURPLE_CANDIDATE/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/INTERRUPTED/MECHANICS_FAILED`；
  后续唯一门是 B 释放 shared wire 后接 Cloud `NpcClickService` player-anchor fallback caller。
- backup=`W-696-CLIENT-IDENTITY-WHOLE-BOUNDARY-1`，`NEEDS_USER_DECISION`；identity parse 的本地/Cloud 落点仍有
  两个合法方案，因此零文件 reservation。

### EOF 互斥结论

- A New `NpcClickPreparedPointLocalMacroMechanics.java`；C New `DialogOptionOcrWordsLocalObservationMechanics.java`；
  D New `NpcClickPlayerAnchorLocalObservationMechanics.java`，三者彼此及当前四路均零文件交集。
- B 是下一波唯一 shared-wire owner；A/C/D 不改 remote enum/codec/digest/handler 或 Cloud `DialogService`。
  A backup 与 D primary、B primary 与 B backup 都是明确二选一，不同时预留。
- 永久本地 `BagService/UICleanerService/GiveItemService/QuestManagerService` 不进入 Cloud；不新增
  owner/permit/session/ledger/compaction/durable workflow/business TTL/auto retry。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #19 - TRUE EOF control record

记录时间：`2026-07-14T21:06:16-04:00`。详细 implementation brief 见本报告较前同名 #19 body；因首次
append anchor 命中旧结尾，本段作为真实 EOF 最新权威排班索引，不删除或改写历史。

- 当前 release gate：A OCR-image R3、C white-story R2、D yellow-target Implementation #2 已交付待父级释放；
  B prepared-action validation 20-file chain 仍在实施。未释放前不发下一单。
- yellow 硬冻结：`NpcClickYellowTargetLocalObservationMechanics.java` 及其内联 candidate closure 不属于下列任一
  primary/backup 写集，不拆 helper/DTO、不转交其它 Worker。

### A next / backup

- primary=`W-696-NPC-TASK-TOOLTIP-WHOLE-LOCAL-MACRO-1`，
  `READY_AFTER_A_R3_SOURCE_RELEASE`；唯一 New
  `DHXY service/npc/NpcClickTaskTooltipLocalMacroMechanics.java`。完整闭合 caller-order region/template match
  `0.82/36` -> remote-exclusive direct move/150/click150/wait1200 -> dialog/battle verify -> first verified/exhausted
  terminal；Cloud 保留 NPC/strategy/fallback，未来唯一 shared-wire gate。
- backup=`W-696-NPC-PLAYER-ANCHOR-IMAGE-WHOLE-OBSERVATION-2`，
  `READY_AFTER_A_R3_AND_B_IMAGEPREPROCESSOR_RELEASE`；唯一 New
  `DHXY service/npc/NpcClickPlayerAnchorLocalObservationMechanics.java`，只做 capture/default-mask/purple blob evidence，
  不把玩家名 OCR、公式、candidate 决策或 click 下沉本地。

### B next / backup

- primary=`W-696-DIALOG-WHITE-STORY-TEMPLATE-FULL-CHAIN-1`，
  `READY_AFTER_B_CURRENT_20_FILE_CHAIN_AND_C_R2_SOURCE_RELEASE`；Cloud 3 New+7 Modify、DHXY 2 New+6 Modify，精确
  文件表在详细 #19 body。闭合 public `prepareWhiteStoryTemplate* -> Cloud DialogService -> typed port/transport ->
  DHXY handler -> C same-frame mechanics -> terminal -> PreparedDialogAction/absent/miss`。
- backup=`W-696-DIALOG-GREEN-TEMPLATE-OPTION-FULL-CHAIN-1`，
  `ALTERNATIVE_AFTER_B_CURRENT_20_FILE_CHAIN`；与 primary 共用 shared modify family，只能二选一顺序实施。

### C next / backup

- primary=`W-696-DIALOG-OPTION-OCR-WORDS-WHOLE-OBSERVATION-1`，
  `READY_AFTER_A_R3_AND_C_R2_SOURCE_RELEASE`；唯一 New
  `DHXY service/dialog/DialogOptionOcrWordsLocalObservationMechanics.java`。单 GREEN/YELLOW immutable evidence ->
  strict validation -> window-scoped artifact -> 恰一次现有 local OCR -> ordered boxes/closed terminal；Cloud 保留
  green-first、miss 后 yellow、alias/fallback/action/click。
- backup=`W-696-DIALOG-OPTION-OCR-FULL-CHAIN-1`，`READY_AFTER_C_PRIMARY_AND_B_SHARED_RELEASE`；只读 primary
  mechanics，再由单一 shared owner 一次接两仓 command/result/port/payload/handler 与 Cloud public caller。

### D next / backup

- primary=`W-696-NPC-PREPARED-POINT-CLICK-VERIFY-WHOLE-MECHANICS-1`，
  `READY_AFTER_D_YELLOW_TARGET_SOURCE_RELEASE`；唯一 New
  `DHXY service/npc/NpcClickPreparedPointLocalMacroMechanics.java`。只接 Cloud 已选择的 screen point，完整闭合
  exact binding/input-worker -> atomic move/click -> verifier -> 696 原有 0/1 retry -> typed terminal；不读写或重做
  yellow candidate closure，也不承接 A tooltip loop。
- backup=`W-696-NAVIGATION-X2-CLOSED-MACRO-FULL-CHAIN-1`，
  `READY_AFTER_B_SHARED_RELEASE_AND_X2_QUEUE_BOUNDARY`；X2、successful mouse-away 与 surrounding direct input 必须同一
  closed macro，前置未满足时零 reservation。

### TRUE EOF mutual exclusion

- primary write owners：A New tooltip class；B white-story 18-file shared chain；C New OCR-words class；D New
  prepared-point class。四者文件交集为零；A/C/D targets 已核不存在，且均不改 remote shared family。
- B 是唯一 shared-wire writer；所有 backup 均为 non-reserving alternative。TaskTracker caller/panel-artifact 前置仍未
  闭合，不伪 READY；ClientIdentity 继续 `NEEDS_USER_DECISION`；永久本地四 Service 不上 Cloud。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #20 - current reservations rebase

记录时间：`2026-07-14T21:11:13-04:00`。本节按四份固定日志真实 EOF 重排，属于 non-binding implementation
queue，不作源码审批：A 已于 `21:08:46` CLAIMED prepared-point mechanics；C 已 CLAIMED white-story R3；D 的
yellow-target R1 已发布待 CLAIMED；B 的 prepared-action validation 20-file chain 仍在途。

### 当前硬 reservation

- A 独占 `DHXY service/npc/NpcClickPreparedPointLocalMacroMechanics.java`；Queue #19 把该文件排给 D 的条目从本节起
  `SUPERSEDED_BY_A_CLAIM_20260714_210846`，不得再交 D 或任何 backup。
- C 当前只改 `DialogWhiteStoryTemplateLocalObservationMechanics.java`；D 当前只可改
  `NpcClickYellowTargetLocalObservationMechanics.java`。两文件及其算法闭包在 parent release 前冻结。
- B 当前 20-file family（两仓 enum/command/result/codec/digest/handler、Cloud `DialogService`、DHXY
  `ImagePreprocessor`）仍由 B 独占。A/C/D primary 不修改这些文件。

### External A release-after primary / backup

- primary=`W-696-NPC-TASK-TOOLTIP-WHOLE-LOCAL-MACRO-1`；状态=
  `READY_AFTER_A_PREPARED_POINT_SOURCE_RELEASE`；唯一 Java 写集 New DHXY
  `src/main/java/com/bot/dhxy/service/npc/NpcClickTaskTooltipLocalMacroMechanics.java`（已核目标不存在）。
- baseline=`696a12b0:NpcClickService.java:1147-1260,176-216,1433-1468`。完整 continuous mechanics：Cloud
  command 已决定 caller-order regions/template/verifier mode -> exact binding -> 按 `0.82/minDistance=36` 找全部
  match -> score 顺序逐点 -> 同一 remote-exclusive 段 direct `move/150ms/click hold 150ms/wait1200ms` -> 现有
  dialog/battle verify -> first verified 或 exhausted terminal。record point `Y+90`、learned ROI
  `[-150,-100,+150,+200]` 保持；本地不决定 NPC/strategy/fallback，禁止 queue-in-queue。
- terminal=`VERIFIED/CLICK_NOT_VERIFIED/NOT_FOUND/BINDING_UNAVAILABLE/TEMPLATE_UNAVAILABLE/INTERRUPTED/
  MECHANICS_FAILED`；验收 region/match 顺序、阈值、input delays、verify 次数、screen-absolute 坐标与 owner/interrupt。
- backup=`W-696-NPC-PREPARED-POINT-FULL-CALLER-CHAIN-1`；状态=
  `READY_AFTER_A_CURRENT_MECHANICS_AND_B_SHARED_RELEASE`。只读 A 当前 mechanics，Cloud New
  `NpcPreparedPointMacroCommand/Result/CloudNpcPreparedPointPort` + Modify local-macro envelopes/digest/
  `service/NpcClickService`；DHXY New mirror command/result payload + Modify local-macro envelopes/codec/digest/handler。
  一次接回 learned/yellow/player-anchor prepared-point callers；backup 为后续 shared-owner alternative，不能与 B/C
  shared full-chain 并行。

### External B release-after primary / backup

- primary=`W-696-DIALOG-WHITE-STORY-TEMPLATE-FULL-CHAIN-1`；状态=
  `READY_AFTER_B_CURRENT_20_FILE_CHAIN_AND_C_R3_SOURCE_RELEASE`。
- Cloud 精确写集：New `remote/DialogWhiteStoryTemplateMacroCommand.java`、
  `DialogWhiteStoryTemplateMacroResult.java`、`CloudDialogWhiteStoryTemplatePort.java`；Modify
  `remote/LocalMacroKind.java`、`LocalMacroCommand.java`、`LocalMacroRequest.java`、`LocalMacroOutcome.java`、
  `RemoteCommandOutcomeEnvelope.java`、`RemoteProtocolDigests.java`、`service/DialogService.java`。
- DHXY 精确写集：New `cloud/remote/RemoteDialogWhiteStoryTemplateMacroCommandPayload.java`、
  `RemoteDialogWhiteStoryTemplateMacroResultPayload.java`；Modify `RemoteLocalMacroKind.java`、
  `RemoteLocalMacroCommandPayload.java`、`RemoteLocalMacroResultPayload.java`、`RemoteOperationPayloadCodec.java`、
  `RemoteProtocolDigests.java`、`LocalRemoteGameCommandHandler.java`；C R3 mechanics 只读。
- reachable chain=`prepareWhiteStoryTemplate* public caller -> Cloud DialogService.verifyWhiteStoryTemplate -> typed
  port/transport -> DHXY handler -> C same-frame mechanics -> MATCHED/STORY_MISS/STORY_ABSENT/CAPTURE_UNAVAILABLE/
  BINDING_UNAVAILABLE/MECHANICS_FAILED -> PreparedDialogAction/absent/miss`；envelope 仍
  `EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN`。验收 `0.85`/caller-order、single-frame artifact、坐标/evidence、strict
  codec/canonical digest 与 696 public return。
- backup=`W-696-DIALOG-GREEN-TEMPLATE-OPTION-FULL-CHAIN-1`；状态=
  `ALTERNATIVE_AFTER_B_CURRENT_20_FILE_CHAIN`；同一 shared modify family + Green 专用 command/result/port/payload，
  只读既有 green mechanics；与 primary 二选一，不并发。

### External C release-after primary / backup

- primary=`W-696-DIALOG-OPTION-OCR-WORDS-WHOLE-OBSERVATION-1`；状态=
  `READY_AFTER_C_WHITE_STORY_R3_SOURCE_RELEASE`；唯一 Java 写集 New DHXY
  `src/main/java/com/bot/dhxy/service/dialog/DialogOptionOcrWordsLocalObservationMechanics.java`（已核目标不存在）。
- baseline=`696a12b0:DialogService.java:1792-1895`、
  `GameTextLineOcrService.java:120+ readDialogOptionWords`；现有真实 producer=
  `TextRecognizer.getAllTextResultsLocalOnly(String)`。每次 command 只接一个 `GREEN/YELLOW` variant 的 immutable
  PNG/SHA/dimensions/rect；strict validate -> window-scoped artifact -> 恰一次 local OCR -> caller-order immutable
  image-local boxes -> closed terminal。Cloud 保留 green-first、miss 后 yellow、alias/fallback/action/click。
- terminal=`WORDS/NO_WORDS/OCR_UNAVAILABLE/INVALID_IMAGE/MECHANICS_FAILED`；验收单 variant/单 OCR、provider failure
  不伪装视觉 miss、坐标 bounds、temp/image owner、defensive bytes 与异常 closure。
- backup=`W-696-DIALOG-OPTION-OCR-FULL-CHAIN-1`；状态=`READY_AFTER_C_PRIMARY_AND_B_SHARED_RELEASE`。只读 C
  mechanics，新增两仓 OCR command/result/port/payload 并由一个 shared owner 修改 envelopes/codec/digest/handler 与
  Cloud `DialogService`，一次闭合 public caller；不得与 B primary/backup 或 A shared backup 并发。

### External D release-after primary / backup

- primary=`W-696-NPC-PLAYER-ANCHOR-WHOLE-LOCAL-OBSERVATION-1`；状态=
  `READY_AFTER_D_YELLOW_R1_SOURCE_RELEASE`；唯一 Java 写集 New DHXY
  `src/main/java/com/bot/dhxy/service/npc/NpcClickPlayerAnchorLocalObservationMechanics.java`（已核目标不存在）。
- baseline=`696a12b0:NpcClickService.java:2505-2531,2865-2996,3132+`。这是完整本地连续 mechanics prerequisite：
  `prepareAlt4=true` 时在一个 local macro 内执行 baseline Alt+4 + exact-window capture；随后按
  `skipDefaultOcrMask` 做 default-mask prep、内聚 696 exact purple wash、connected-purple-blob evidence 与同帧
  immutable PNG/SHA/dimensions/rect。不得调用/修改 A prepared-point 类，也不得读取、复制或拆分 D yellow candidate
  closure；Cloud 保留玩家名 OCR/provider fallback、地图公式、candidate verdict 与 click/verify。
- terminal=`CAPTURED/NO_PURPLE_BLOB/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/INTERRUPTED/MECHANICS_FAILED`；
  CAPTURED/NO_PURPLE_BLOB 携同帧 prepared evidence，只有 CAPTURED 携 blob anchor。验收 Alt+4/capture 顺序、默认
  mask 条件、purple 阈值/blob bounds、screen mapping、raw/prepared owner、零 OCR/业务 verdict/auto retry。
- backup=`W-696-TASKTRACKER-PANEL-CLOUD-WHOLE-CLASS-1`；状态=
  `READY_AFTER_EXPLICIT_TASK_CONTEXT_AND_PANEL_CAPTURE_PORT`。只改 Cloud `TaskTrackerPanelService.java` 保留完整
  green-chain/fingerprint/cache/sort/classification/result 算法；caller/context/artifact contract 未闭合前零 reservation，
  不把算法下沉 DHXY。`ClientIdentityService` 仍 `NEEDS_USER_DECISION`，不作为自动替代。

### Queue #20 mutual exclusion and fallback discipline

| 路 | primary 唯一写集 owner | 与当前/其它路关系 |
|---|---|---|
| A | New `NpcClickTaskTooltipLocalMacroMechanics.java` | 不碰 A 当前 prepared-point、C/D 当前、B shared |
| B | White-story Cloud 10 + DHXY 8 | 当前 20 files 释放后才独占 shared；只读 C R3 |
| C | New `DialogOptionOcrWordsLocalObservationMechanics.java` | 不碰 C R3、A current、D yellow、B shared |
| D | New `NpcClickPlayerAnchorLocalObservationMechanics.java` | 不碰 A prepared-point、D yellow 或 B `ImagePreprocessor` |

- 四个 primary 文件交集为零；A/C/D create-new targets 均已核不存在。A 当前 prepared-point reservation 只属于 A，
  Queue #20 没有任何 D primary/backup 复用该文件。
- backups 是 non-reserving alternatives；凡涉及 shared family 均等待 B 当前链释放并由单一 Worker 顺序实施。
  TaskTracker 前置不足和 ClientIdentity 落点歧义均明确保留，不伪 READY。
- 永久本地 `BagService/UICleanerService/GiveItemService/QuestManagerService` 不迁 Cloud；不新增
  owner/permit/session/ledger/compaction/durable workflow/business TTL/auto retry。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #21 - tooltip / validation-R1 / option-OCR / yellow-R2 rebase

记录时间：`2026-07-14T21:38:01-04:00`。本节仅提供 non-binding 后继排班，不作源码结论。当前 reservation
按固定日志真实 EOF 与父级最新分配锁定：A=New `NpcClickTaskTooltipLocalMacroMechanics.java`；B=prepared-action
validation R1 五文件；C=New `DialogOptionOcrWordsLocalObservationMechanics.java`；D=同一
`NpcClickYellowTargetLocalObservationMechanics.java` R2。

### 当前 reservation 精确排除

- A 当前 tooltip 新类、C 当前 OCR-words 新类、D 当前 yellow-target 同文件不进入任何 #21 primary/backup 写集。
- B R1 当前五文件：DHXY `DialogPreparedActionValidationLocalMechanics.java`、
  `RemoteDialogPreparedActionValidationMacroCommandPayload.java`、
  `RemoteDialogPreparedActionValidationMacroResultPayload.java`；Cloud
  `DialogPreparedActionValidationMacroCommand.java`、`DialogPreparedActionValidationMacroResult.java`。整条 validation
  chain 尚未 release 前，其 generic shared family 也继续冻结，C/D 不抢写。

### External A primary / backup

- primary=`W-696-NPC-PLAYER-ANCHOR-WHOLE-LOCAL-OBSERVATION-1`；状态=
  `READY_AFTER_A_TOOLTIP_SOURCE_RELEASE`；唯一 Java 写集 New DHXY
  `src/main/java/com/bot/dhxy/service/npc/NpcClickPlayerAnchorLocalObservationMechanics.java`（目标已核不存在）。
- baseline=`696a12b0:NpcClickService.java:2505-2531,2865-2996,3132+`。完整 continuous mechanics：command 携
  exact binding、scan rect、`prepareAlt4/skipDefaultOcrMask`；需要 Alt+4 时在同一 local macro 内完成按键与 fresh
  capture，再做 default-mask、内聚 696 exact purple wash、connected-purple-blob 与同帧 PNG/SHA/dimensions/rect。
  Cloud 保留玩家名 OCR/provider fallback、地图公式、candidate verdict、click/verify；不调用 A 当前 tooltip 类。
- terminal=`CAPTURED/NO_PURPLE_BLOB/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/INTERRUPTED/MECHANICS_FAILED`；
  验收 Alt+4/capture 次序、mask 条件、purple 阈值/blob bounds、screen mapping、image owner、零业务选择/retry。
- backup=`W-696-NPC-CTRL-MENU-WHOLE-LOCAL-MACRO-1`；状态=
  `READY_AFTER_TEXT_RECOGNIZER_PROVIDER_PARITY`；唯一 New
  `service/npc/NpcClickCtrlMenuWholeLocalMacroMechanics.java`，锚点 `NpcClickService:303-585`。必须完整闭合
  Ctrl-hold -> hover -> before/after change -> yellow OCR target -> direct click/verify/release；当前 local-only OCR 不等价
  696 `getAllTextResultsForMatch` provider fallback，前置未补齐时零 reservation，不伪 READY。

### External B primary / backup

- primary=`W-696-DIALOG-WHITE-STORY-TEMPLATE-FULL-CHAIN-1`；状态=
  `READY_AFTER_B_VALIDATION_R1_AND_C_WHITE_STORY_RELEASE`。
- Cloud 写集：New `DialogWhiteStoryTemplateMacroCommand.java`、`DialogWhiteStoryTemplateMacroResult.java`、
  `CloudDialogWhiteStoryTemplatePort.java`；Modify `LocalMacroKind.java`、`LocalMacroCommand.java`、
  `LocalMacroRequest.java`、`LocalMacroOutcome.java`、`RemoteCommandOutcomeEnvelope.java`、
  `RemoteProtocolDigests.java`、`service/DialogService.java`。
- DHXY 写集：New `RemoteDialogWhiteStoryTemplateMacroCommandPayload.java`、
  `RemoteDialogWhiteStoryTemplateMacroResultPayload.java`；Modify `RemoteLocalMacroKind.java`、
  `RemoteLocalMacroCommandPayload.java`、`RemoteLocalMacroResultPayload.java`、`RemoteOperationPayloadCodec.java`、
  `RemoteProtocolDigests.java`、`LocalRemoteGameCommandHandler.java`；white-story mechanics 只读。
- chain=`prepareWhiteStoryTemplate* -> Cloud DialogService -> typed port/transport -> DHXY handler -> same-frame
  mechanics -> MATCHED/STORY_MISS/STORY_ABSENT/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/MECHANICS_FAILED ->
  PreparedDialogAction/absent/miss`；验收 `0.85` caller-order、single-frame artifact、screen point、strict payload/digest。
- backup=`W-696-DIALOG-GREEN-TEMPLATE-OPTION-FULL-CHAIN-1`；状态=
  `ALTERNATIVE_AFTER_B_VALIDATION_R1`；同一 generic modify family，改用 Green 专用 command/result/port/payload，
  只读既有 green mechanics；与 primary 二选一。

### External C primary / backup

- primary=`W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1`；状态=
  `READY_AFTER_C_OCR_WORDS_SOURCE_RELEASE_AND_B_SHARED_RELEASE`。这是两操作但单一 public lifecycle 的完整双端链，
  不修改 C 当前 mechanics 或 A 已释放的 OCR-image mechanics。
- Cloud New 6：`DialogOptionOcrImageMacroCommand.java`、`DialogOptionOcrImageMacroResult.java`、
  `CloudDialogOptionOcrImagePort.java`、`DialogOptionOcrWordsMacroCommand.java`、
  `DialogOptionOcrWordsMacroResult.java`、`CloudDialogOptionOcrWordsPort.java`；Modify generic six
  `LocalMacroKind/Command/Request/Outcome/RemoteCommandOutcomeEnvelope/RemoteProtocolDigests` +
  `service/DialogService.java`。
- DHXY New 4：`RemoteDialogOptionOcrImageMacroCommandPayload.java`、
  `RemoteDialogOptionOcrImageMacroResultPayload.java`、`RemoteDialogOptionOcrWordsMacroCommandPayload.java`、
  `RemoteDialogOptionOcrWordsMacroResultPayload.java`；Modify generic five
  `RemoteLocalMacroKind/CommandPayload/ResultPayload/RemoteOperationPayloadCodec/RemoteProtocolDigests` + handler。
- baseline=`DialogService:1792-1895`、`GameTextLineOcrService:120+`；chain=Cloud caller -> image port 恰一次 capture
  并返回同帧 green/yellow immutable bytes -> words port 跑 green -> Cloud alias/keyword 判定 -> 仅 miss 才把同一
  yellow bytes 发 words port -> Cloud merge/fallback/action/click。无需 retained local artifact/session，保持 green-first
  与同帧 authority；terminal 分开 capture/image-invalid/OCR-unavailable/words/no-words，envelope 仍四态。
- backup=`W-696-TASKTRACKER-PANEL-CLOUD-WHOLE-CLASS-1`；状态=
  `READY_AFTER_EXPLICIT_TASK_CONTEXT_AND_PANEL_CAPTURE_OCR_PORT`；只改 Cloud `TaskTrackerPanelService.java`，完整保留
  green-chain/fingerprint/cache/sort/classification/result；caller/context/OCR artifact 前置未闭合时零 reservation。

### External D primary / backup

- primary=`W-696-NPC-TASK-TOOLTIP-FULL-CALLER-CHAIN-1`；状态=
  `READY_AFTER_A_TOOLTIP_SOURCE_RELEASE_AND_B_SHARED_RELEASE`；A tooltip mechanics 只读，D 不修改该当前 reservation。
- Cloud New `NpcTaskTooltipMacroCommand.java`、`NpcTaskTooltipMacroResult.java`、`CloudNpcTaskTooltipPort.java`；
  Modify generic six `LocalMacroKind/Command/Request/Outcome/RemoteCommandOutcomeEnvelope/RemoteProtocolDigests` +
  `service/NpcClickService.java`。DHXY New mirror command/result payload；Modify generic five
  `RemoteLocalMacroKind/CommandPayload/ResultPayload/RemoteOperationPayloadCodec/RemoteProtocolDigests` + handler。
- chain=`clickNpcSmartWithOutcome -> Cloud NpcClickService tooltip strategy -> typed port/transport -> DHXY handler ->
  A complete tooltip mechanics -> VERIFIED/CLICK_NOT_VERIFIED/NOT_FOUND/... -> Cloud strategy outcome/memory evidence`；
  Cloud 保留 NPC/strategy/fallback，验收 `0.82/36`、region/match 顺序、input delays/verify、record point/ROI、digest。
- backup=`W-696-NAVIGATION-X2-CLOSED-MACRO-FULL-CHAIN-1`；状态=
  `READY_AFTER_SHARED_RELEASE_AND_X2_QUEUE_BOUNDARY`。X2、successful mouse-away 与 surrounding direct input 必须同一
  closed macro，禁止 exclusive callback 内二次排队；技术前置未满足时零 reservation。

### Queue #21 concurrency discipline

- A primary 是唯一不写 shared wire 的即时后继，可与一个 shared full-chain 并行。
- B/C/D primary 各自都是真实完整 caller chain，但共用 generic enum/codec/digest/handler，必须由父级按单一 shared
  writer 串行选择，建议顺序 `B white-story -> D tooltip -> C option-OCR`；不能为了同时占满 Worker 并发发三单。
- 所有 primary/backup 均避开当前 A tooltip、B R1 五文件、C OCR-words、D yellow-target reservation；future chain
  只读已释放 mechanics。ClientIdentity 继续 `NEEDS_USER_DECISION`；永久本地四 Service 不迁 Cloud。
- 不新增 owner/permit/session/ledger/compaction/durable workflow/business TTL/auto retry。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #22 - canonical EOF rebase

记录时间：`2026-07-14T21:48:30-04:00`。这是 Queue #21 后的 canonical non-binding 排班；先前误插在
Queue #18/#19 之间的同号草稿由本节替代，历史不删除。本节不作源码审查结论。

### Reservations and supersession

- A 当前唯一 Java：New DHXY `service/npc/NpcClickTaskTooltipLocalMacroMechanics.java`。
- B 当前三 Java：Cloud `service/DialogService.java`、Cloud
  `remote/DialogPreparedActionValidationMacroCommand.java`、DHXY
  `cloud/remote/RemoteDialogPreparedActionValidationMacroCommandPayload.java`。
- C 当前唯一 Java：DHXY `service/dialog/DialogOptionOcrWordsLocalObservationMechanics.java` R1。
- D 当前唯一 Java：New DHXY `service/npc/NpcClickPlayerAnchorLocalObservationMechanics.java`。
- Queue #21 的 A player-anchor 候选已由 D 当前分配消费，记为
  `SUPERSEDED_BY_D_PLAYER_ANCHOR_ASSIGNMENT`。以下后继均不得触碰上述四组 reservation。

### A primary + backup

- primary=`W-696-NPC-TASK-TOOLTIP-FULL-CALLER-CHAIN-1`；门=
  `READY_AFTER_A_TOOLTIP_SOURCE_RELEASE_AND_SHARED_SLOT`；A 当前 mechanics 只读。
- Cloud：New `NpcTaskTooltipMacroCommand/Result`、`CloudNpcTaskTooltipPort`；Modify generic
  `LocalMacroKind/Command/Request/Outcome/RemoteCommandOutcomeEnvelope/RemoteProtocolDigests` +
  `service/NpcClickService.java`。DHXY：New mirror command/result payload；Modify generic
  `RemoteLocalMacroKind/CommandPayload/ResultPayload/RemoteOperationPayloadCodec/RemoteProtocolDigests` + handler。
- 696 anchor=`NpcClickService:1147-1260,176-216,1433-1468`；闭合
  `clickNpcSmartWithOutcome -> Cloud tooltip strategy -> typed port -> handler -> tooltip mechanics -> typed terminal ->
  Cloud strategy/memory`。保持 `0.82/36`、region/point 顺序、move/150/click150/1200/verify、Y+90、learned ROI；
  七态 terminal 不折叠，Cloud 保留 NPC/strategy/fallback。
- backup=`W-696-NPC-CTRL-MENU-WHOLE-LOCAL-MACRO-1`；门=
  `READY_AFTER_TEXT_RECOGNIZER_PROVIDER_PARITY`；唯一 New
  `service/npc/NpcClickCtrlMenuWholeLocalMacroMechanics.java`，anchor=`NpcClickService:303-585`。完整闭合
  Ctrl-hold/hover/before-after/yellow OCR/click/verify/release；provider parity 未证明前零 reservation。

### B primary + backup

- primary=`W-696-DIALOG-WHITE-STORY-TEMPLATE-FULL-CHAIN-1`；门=
  `READY_AFTER_B_PREPARED_ACTION_R2_SOURCE_RELEASE_AND_SHARED_SLOT`。
- Cloud：New `DialogWhiteStoryTemplateMacroCommand/Result`、`CloudDialogWhiteStoryTemplatePort`；Modify generic six +
  `service/DialogService.java`。DHXY：New mirror command/result payload；Modify generic five + handler；released
  white-story mechanics 只读。
- 696 chain=`prepareWhiteStoryTemplate* -> Cloud DialogService -> typed port -> handler -> same-frame mechanics ->
  MATCHED/STORY_MISS/STORY_ABSENT/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/MECHANICS_FAILED ->
  PreparedDialogAction/absent/miss`。验收 caller-order、`0.85`、single-frame artifact、screen point、strict canonical/digest；
  本地不做 option/fallback 业务选择。
- backup=`W-696-DIALOG-GREEN-TEMPLATE-OPTION-FULL-CHAIN-1`；门=`ALTERNATIVE_AFTER_B_R2_SOURCE_RELEASE`；
  同一 shared family 的互斥替代，使用 Green 专用 command/result/port/payload 并只读 released green mechanics。

### C primary + backup

- primary=`W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1`；门=
  `READY_AFTER_C_OCR_WORDS_R1_SOURCE_RELEASE_AND_SHARED_SLOT`；OCR-image/OCR-words mechanics 均只读。
- Cloud：New image command/result/port + words command/result/port；Modify generic six + `service/DialogService.java`。
  DHXY：New image/words mirror command/result payload 共四文件；Modify generic five + handler。
- 696 anchors=`DialogService:1792-1895`、`GameTextLineOcrService:120+`；闭合
  `Cloud caller -> 一次 image capture 得同帧 green/yellow PNG -> green words -> Cloud alias/keyword -> miss 才用同一
  yellow bytes 跑 words -> Cloud merge/fallback/action`。terminal 分开 image/capture invalid、OCR unavailable、
  words/no-words；验收 green-first、same-frame、word order/坐标与 strict canonical，零 retained local session。
- backup=`W-696-TASKTRACKER-PANEL-CLOUD-WHOLE-CLASS-1`；门=
  `READY_AFTER_EXPLICIT_TASK_CONTEXT_AND_PANEL_CAPTURE_OCR_PORT`；只改 Cloud `TaskTrackerPanelService.java`，保留
  green-chain/fingerprint/cache/sort/classification/result；真实 context producer 与 panel artifact 未闭合前零 reservation。

### D primary + backup

- primary=`W-696-NPC-PLAYER-ANCHOR-FULL-CALLER-CHAIN-1`；门=
  `READY_AFTER_D_PLAYER_ANCHOR_SOURCE_RELEASE_AND_SHARED_SLOT`；D 当前 mechanics 只读。
- Cloud：New `NpcPlayerAnchorMacroCommand/Result`、`CloudNpcPlayerAnchorPort`；Modify generic six +
  `service/NpcClickService.java`。DHXY：New mirror command/result payload；Modify generic five + handler。
- 696 anchors=`NpcClickService:2505-2531,2865-2996,3132+`；闭合
  `Cloud player-anchor branch -> typed port -> handler -> player-anchor mechanics -> CAPTURED/NO_PURPLE_BLOB/... ->
  Cloud OCR/provider fallback/map formula/candidate/click`。验收 Alt+4/capture、mask、purple blob、PNG/SHA/dimensions/
  screen rect、owner/terminal；本地不持有 OCR fallback、公式、candidate 或 click 决策。
- backup=`W-696-NAVIGATION-X2-CLOSED-MACRO-FULL-CHAIN-1`；门=
  `READY_AFTER_SHARED_RELEASE_AND_X2_QUEUE_BOUNDARY`；X2/mouse-away/surrounding direct input 必须同一 closed macro，
  禁止 exclusive callback 内二次排队；queue boundary 未闭合前零 reservation。

### Shared-family serial gate

- 四个 primary 均写两仓 generic enum/codec/digest/handler family，因此不是并发四单。建议唯一顺序：
  `B white-story -> A tooltip -> D player-anchor -> C option-OCR`；每次只发一个 shared writer，source release 后再发下一单。
- backups 全是 non-reserving alternatives。A provider parity、C TaskTracker context/artifact、D Navigation X2 queue
  boundary 均是明确技术门；`ClientIdentityService` 仍 `NEEDS_USER_DECISION`，不自动排单。
- 永久本地 `BagService/UICleanerService/GiveItemService/QuestManagerService` 不迁 Cloud；不新增
  owner/permit/session/ledger/compaction/durable workflow/business TTL/auto retry。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #23 - canonical A/C release-first rebase

记录时间：`2026-07-14T21:53:37-04:00`。本节是 Queue #22 后的 canonical non-binding 排班，只给父级发单材料，
不作源码审查或裁决。真实 EOF 状态：A tooltip Implementation #1、C OCR-words R1 已交付待父级处理；B 已领取
prepared-action R2 三文件；D 已领取 player-anchor New mechanics。

### Atomic shared-slot rule

- A/C 下一单均为完整 reachable caller chain，均会写两仓 generic local-macro family，故二者是“可直接发出但
  原子互斥”，不是可并发两单。A 或 C 哪一路先获得父级 source release，父级即可把该路标为 `READY` 并原子授予
  `SHARED_LOCAL_MACRO_SLOT`；另一条同时标为 `WAITING_SHARED_SLOT`。第一条 release 后，第二条无需重做排班即可发出。
- B 当前 R2 仅占 Cloud `DialogService` + 两仓 prepared-action command 三文件，D 当前只占 player-anchor New mechanics；
  它们都不取得新的 shared slot。B/D 后继只有在当前任务 release 且 slot 空闲时才从 `WAITING` 转 `READY`。

### External A - direct next task

- primary=`W-696-NPC-TASK-TOOLTIP-FULL-CALLER-CHAIN-1`；状态=
  `READY_ON_A_SOURCE_RELEASE_IF_SHARED_SLOT_FREE`，否则 `WAITING_SHARED_SLOT`。A 当前 tooltip mechanics 只读。
- Cloud 精确写集：New `NpcTaskTooltipMacroCommand.java`、`NpcTaskTooltipMacroResult.java`、
  `CloudNpcTaskTooltipPort.java`；Modify `LocalMacroKind.java`、`LocalMacroCommand.java`、
  `LocalMacroRequest.java`、`LocalMacroOutcome.java`、`RemoteCommandOutcomeEnvelope.java`、
  `RemoteProtocolDigests.java`、`service/NpcClickService.java`。
- DHXY 精确写集：New `RemoteNpcTaskTooltipMacroCommandPayload.java`、
  `RemoteNpcTaskTooltipMacroResultPayload.java`；Modify `RemoteLocalMacroKind.java`、
  `RemoteLocalMacroCommandPayload.java`、`RemoteLocalMacroResultPayload.java`、
  `RemoteOperationPayloadCodec.java`、`RemoteProtocolDigests.java`、`LocalRemoteGameCommandHandler.java`。
- chain=`clickNpcSmartWithOutcome -> Cloud tooltip strategy -> typed port -> DHXY handler -> released tooltip mechanics ->
  VERIFIED/CLICK_NOT_VERIFIED/NOT_FOUND/BINDING_UNAVAILABLE/TEMPLATE_UNAVAILABLE/INTERRUPTED/MECHANICS_FAILED ->
  Cloud strategy/memory outcome`。验收 `696a12b0:NpcClickService:1147-1260,176-216,1433-1468` 的 region/point
  顺序、`0.82/36`、move/150/click150/1200/verify、Y+90、learned ROI 与零 retry；本地不选 NPC/strategy/fallback。
- backup=`W-696-NPC-CTRL-MENU-WHOLE-LOCAL-MACRO-1`；状态=
  `WAITING_TEXT_RECOGNIZER_PROVIDER_PARITY`；唯一 New `service/npc/NpcClickCtrlMenuWholeLocalMacroMechanics.java`。
  provider parity 未闭合前不作为 READY 替代。

### External C - direct next task

- primary=`W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1`；状态=
  `READY_ON_C_SOURCE_RELEASE_IF_SHARED_SLOT_FREE`，否则 `WAITING_SHARED_SLOT`。C OCR-words R1 与 released
  OCR-image mechanics 均只读。
- Cloud 精确写集：New `DialogOptionOcrImageMacroCommand.java`、`DialogOptionOcrImageMacroResult.java`、
  `CloudDialogOptionOcrImagePort.java`、`DialogOptionOcrWordsMacroCommand.java`、
  `DialogOptionOcrWordsMacroResult.java`、`CloudDialogOptionOcrWordsPort.java`；Modify generic six +
  `service/DialogService.java`。
- DHXY 精确写集：New `RemoteDialogOptionOcrImageMacroCommandPayload.java`、
  `RemoteDialogOptionOcrImageMacroResultPayload.java`、`RemoteDialogOptionOcrWordsMacroCommandPayload.java`、
  `RemoteDialogOptionOcrWordsMacroResultPayload.java`；Modify generic five + `LocalRemoteGameCommandHandler.java`。
- chain=`Cloud Dialog caller -> image port 恰一次 capture 得同帧 green/yellow immutable PNG -> green words port ->
  Cloud alias/keyword -> miss 才把同一 yellow bytes 送 words port -> Cloud merge/fallback/action`。验收
  `696a12b0:DialogService:1792-1895` 的 green-first、same-frame、caller-order word boxes、颜色/alias/target/fallback
  全留 Cloud；terminal 区分 capture/image invalid、OCR unavailable、words/no-words，零 retained local session。
- backup=`W-696-TASKTRACKER-PANEL-CLOUD-WHOLE-CLASS-1`；状态=
  `WAITING_EXPLICIT_TASK_CONTEXT_AND_PANEL_CAPTURE_OCR_PORT`；真实 caller/artifact 前置未闭合时不伪写 READY，
  `TaskTrackerPanelService` 的 green-chain/fingerprint/cache/sort/classification/result 不下沉本地。

### External B/D completion queue

- B primary=`W-696-DIALOG-WHITE-STORY-TEMPLATE-FULL-CHAIN-1`；状态=
  `WAITING_B_R2_SOURCE_RELEASE_AND_SHARED_SLOT`。完整 Cloud `DialogService -> typed port -> DHXY handler -> released
  white-story same-frame mechanics -> MATCHED/STORY_MISS/STORY_ABSENT/...`；写 generic family，故排在 A/C 后。
  backup=`W-696-DIALOG-GREEN-TEMPLATE-OPTION-FULL-CHAIN-1`，同一 shared family 的二选一替代。
- D primary=`W-696-NPC-PLAYER-ANCHOR-FULL-CALLER-CHAIN-1`；状态=
  `WAITING_D_PLAYER_ANCHOR_SOURCE_RELEASE_AND_SHARED_SLOT`。完整 Cloud `NpcClickService -> typed port -> handler ->
  released player-anchor mechanics -> CAPTURED/NO_PURPLE_BLOB/... -> Cloud OCR/formula/candidate/click`；本地只产
  typed image/fact。backup=`W-696-NAVIGATION-X2-CLOSED-MACRO-FULL-CHAIN-1`，状态=
  `WAITING_SHARED_RELEASE_AND_X2_QUEUE_BOUNDARY`。

### Canonical dispatch order

- 第一优先：A/C 中先获得 source release 的一路；第二优先：另一条 A/C；随后 B/D 按各自当前交付完成时间入队。
  每个时刻只允许一个 shared writer。若父级需要四路同时开工，本队列没有诚实的第二条无 shared 冲突完整链，
  不以 DTO/helper/单方法壳填槽。
- `ClientIdentityService` 继续 `NEEDS_USER_DECISION`；永久本地四 Service 不迁 Cloud。禁止新增
  owner/permit/session/ledger/compaction/durable workflow/business TTL/auto retry。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #24 - C shared owner -> B white-story -> D anchor -> A tooltip

记录时间：`2026-07-14T22:08:42-04:00`。本节是 Queue #23 后的 canonical non-binding 排班，不作源码审查或
裁决。状态依据四份固定日志真实 EOF与父级最新调度：B prepared-validation 已 source release；C 已领取完整
option-OCR same-frame chain并独占 `SHARED_LOCAL_MACRO_SLOT`；D 持有 player-anchor 同文件 R1。A 的父级最新状态为
tooltip 同文件 R2；helper 复读时 A 物理 EOF 仍停在前一轮同文件交付，但唯一 Java reservation 不变，不影响本排班。

### Shared family exact ownership

- Cloud shared family：`remote/LocalMacroKind.java`、`LocalMacroCommand.java`、`LocalMacroRequest.java`、
  `LocalMacroOutcome.java`、`RemoteCommandOutcomeEnvelope.java`、`RemoteProtocolDigests.java`。
- DHXY shared family：`cloud/remote/RemoteLocalMacroKind.java`、`RemoteLocalMacroCommandPayload.java`、
  `RemoteLocalMacroResultPayload.java`、`RemoteOperationPayloadCodec.java`、`RemoteProtocolDigests.java`、
  `LocalRemoteGameCommandHandler.java`。
- 当前 owner 仅 C。后继 canonical 次序固定为 `C current option-OCR -> B white-story -> D player-anchor -> A tooltip`。
  上一单 source release 前，后一单状态只能是 `WAITING_SHARED_SLOT`；不得并发改上述十二文件。

### External A - after same-file R2

- primary=`W-696-NPC-TASK-TOOLTIP-FULL-CALLER-CHAIN-1`；状态=
  `WAITING_A_TOOLTIP_R2_SOURCE_RELEASE_AND_SHARED_SLOT`。A 当前
  `service/npc/NpcClickTaskTooltipLocalMacroMechanics.java` 只读，完整 caller chain 排在 D 之后。
- 专用 Cloud 写集：New `remote/NpcTaskTooltipMacroCommand.java`、`NpcTaskTooltipMacroResult.java`、
  `CloudNpcTaskTooltipPort.java`；Modify `service/NpcClickService.java`；另取得上述 Cloud shared family。
- 专用 DHXY 写集：New `cloud/remote/RemoteNpcTaskTooltipMacroCommandPayload.java`、
  `RemoteNpcTaskTooltipMacroResultPayload.java`；另取得上述 DHXY shared family；不得修改 released mechanics。
- chain/验收=`clickNpcSmartWithOutcome -> Cloud tooltip strategy -> typed port -> handler -> tooltip mechanics ->
  VERIFIED/CLICK_NOT_VERIFIED/NOT_FOUND/BINDING_UNAVAILABLE/TEMPLATE_UNAVAILABLE/INTERRUPTED/MECHANICS_FAILED ->
  Cloud strategy/memory`；保持 `696a12b0:NpcClickService:1147-1260,176-216,1433-1468` 的 `0.82/36`、region/point
  顺序、move/150/click150/1200/verify、Y+90/learned ROI、零 retry与本地零业务选择。
- backup=`W-696-NPC-CTRL-MENU-WHOLE-LOCAL-MACRO-1`；状态=
  `WAITING_TEXT_RECOGNIZER_PROVIDER_PARITY`；唯一 New
  `service/npc/NpcClickCtrlMenuWholeLocalMacroMechanics.java`。provider fallback 未等价前不转 READY。

### External C - current owner release handoff

- current primary=`W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1`；状态=`IN_FLIGHT_SHARED_OWNER`；其领取日志中的
  Cloud 13 + DHXY 10 Java 是当前唯一 shared reservation。本 helper 不增改 C 写集。
- C 交付并 source release 后的唯一 dispatch 动作是把完整 shared family 交给 B；不得在 C 名下插入 DTO/helper/
  单方法任务。C 后继 substantial primary=`W-696-TASKTRACKER-PANEL-CLOUD-WHOLE-CLASS-1` 仍为
  `WAITING_EXPLICIT_PANEL_CAPTURE_OCR_PORT`，真实 artifact/terminal 前置闭合后才可发。
- backup=`ClientIdentityService` placement，状态=`NEEDS_USER_DECISION`；OS title/parser 整类留本地或 Cloud 消费
  typed title fact 两案未由用户选择，不自动占 Worker。

### External B - authoritative task immediately after C

- primary=`W-696-DIALOG-WHITE-STORY-TEMPLATE-FULL-CHAIN-1`；状态=
  `READY_ON_C_SOURCE_RELEASE`。这是 C 释放 shared slot 后父级可直接发布给 B 的唯一下一 shared 实质单。
- 专用 Cloud 写集：New `remote/DialogWhiteStoryTemplateMacroCommand.java`、
  `DialogWhiteStoryTemplateMacroResult.java`、`CloudDialogWhiteStoryTemplatePort.java`；Modify
  `service/DialogService.java`；另独占 Cloud shared family。
- 专用 DHXY 写集：New `cloud/remote/RemoteDialogWhiteStoryTemplateMacroCommandPayload.java`、
  `RemoteDialogWhiteStoryTemplateMacroResultPayload.java`；另独占 DHXY shared family；released white-story
  mechanics 只读。
- chain/terminal=`prepareWhiteStoryTemplate* -> Cloud DialogService -> typed port/transport -> handler -> same-frame
  mechanics -> MATCHED/STORY_MISS/STORY_ABSENT/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/MECHANICS_FAILED ->
  PreparedDialogAction/absent/miss`。验收 696 caller order、`0.85`、single-frame image、screen-absolute point、strict
  codec/canonical/digest，且本地不做 option/fallback 决策。
- backup=`W-696-DIALOG-GREEN-TEMPLATE-OPTION-FULL-CHAIN-1`；状态=`ALTERNATIVE_SAME_SHARED_SLOT`；专用 Green
  command/result/port/payload + 同一 shared family，与 white-story 二选一，不能并发或拆小叶子。

### External D - after R1 and B completion

- primary=`W-696-NPC-PLAYER-ANCHOR-FULL-CALLER-CHAIN-1`；状态=
  `WAITING_D_PLAYER_ANCHOR_R1_SOURCE_RELEASE_AND_B_CHAIN_RELEASE`。D 当前
  `service/npc/NpcClickPlayerAnchorLocalObservationMechanics.java` 只读；B white-story 释放 shared slot 后才转 READY。
- 专用 Cloud 写集：New `remote/NpcPlayerAnchorMacroCommand.java`、`NpcPlayerAnchorMacroResult.java`、
  `CloudNpcPlayerAnchorPort.java`；Modify `service/NpcClickService.java`；另独占 Cloud shared family。
- 专用 DHXY 写集：New `cloud/remote/RemoteNpcPlayerAnchorMacroCommandPayload.java`、
  `RemoteNpcPlayerAnchorMacroResultPayload.java`；另独占 DHXY shared family；不得重开 R1 mechanics。
- chain/terminal=`Cloud NpcClick player-anchor branch -> typed port -> handler -> player-anchor mechanics ->
  CAPTURED/NO_PURPLE_BLOB/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/INTERRUPTED/MECHANICS_FAILED -> Cloud
  OCR/provider fallback/map formula/candidate/click`。验收 `696a12b0:NpcClickService:2505-2531,2865-2996,3132+`
  的 Alt+4/capture、mask、purple wash/blob、PNG/SHA/dimensions/screen rect/owner；业务算法全留 Cloud。
- backup=`W-696-NAVIGATION-X2-CLOSED-MACRO-FULL-CHAIN-1`；状态=
  `WAITING_X2_QUEUE_BOUNDARY_AND_SHARED_SLOT`。X2/mouse-away/surrounding direct input 必须同一 closed macro，
  queue boundary 未闭合前不转 READY。

### Dispatch and no-filler gate

- C release 时只发 B white-story；B release 时只发 D player-anchor；D release 时才发 A tooltip。A/D 同文件返修
  提前完成只改变各自 prerequisite，不改变 shared 次序。
- 四个 primary 的专用 New 文件互不重叠；`DialogService` 仅 C/B 顺序写，`NpcClickService` 仅 D/A 顺序写；shared
  family 全程单 writer。任何技术门未闭合时宁可保持 `WAITING`，不以 DTO/helper/zero-Java 小叶子填空。
- 永久本地 `BagService/UICleanerService/GiveItemService/QuestManagerService` 不迁 Cloud；不新增
  owner/permit/session/ledger/compaction/durable workflow/business TTL/auto retry。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #25 - released A/D prerequisites, C-to-B instant handoff

记录时间：`2026-07-14T22:28:07-04:00`。本节是 Queue #24 后的 canonical non-binding 排班，不作源码审查或
裁决。A tooltip R2 与 D player-anchor R2 的本地 mechanics prerequisite 均已 parent-released；B prepared-action
链也已 release。C option-OCR full same-frame chain 仍是唯一 `IN_FLIGHT_SHARED_OWNER`。

### Shared family and immutable order

- Cloud shared：`remote/LocalMacroKind.java`、`LocalMacroCommand.java`、`LocalMacroRequest.java`、
  `LocalMacroOutcome.java`、`RemoteCommandOutcomeEnvelope.java`、`RemoteProtocolDigests.java`。
- DHXY shared：`cloud/remote/RemoteLocalMacroKind.java`、`RemoteLocalMacroCommandPayload.java`、
  `RemoteLocalMacroResultPayload.java`、`RemoteOperationPayloadCodec.java`、`RemoteProtocolDigests.java`、
  `LocalRemoteGameCommandHandler.java`。
- canonical 次序保持 `C option-OCR -> B white-story -> D player-anchor caller -> A tooltip caller`。每一时刻仅一名
  shared writer；前项 source release 后，父级可在同一调度轮瞬时发布下一项，不插 filler、不等待其它非重叠工作。

### C - current shared owner and release gate

- current=`W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1`；状态=`IN_FLIGHT_SHARED_OWNER`。
- 专用 Cloud 写集：New `DialogOptionOcrImageMacroCommand.java`、`DialogOptionOcrImageMacroResult.java`、
  `CloudDialogOptionOcrImagePort.java`、`DialogOptionOcrWordsMacroCommand.java`、
  `DialogOptionOcrWordsMacroResult.java`、`CloudDialogOptionOcrWordsPort.java`；Modify `service/DialogService.java`；
  加 Cloud shared 六文件。
- 专用 DHXY 写集：New `RemoteDialogOptionOcrImageMacroCommandPayload.java`、
  `RemoteDialogOptionOcrImageMacroResultPayload.java`、`RemoteDialogOptionOcrWordsMacroCommandPayload.java`、
  `RemoteDialogOptionOcrWordsMacroResultPayload.java`；加 DHXY shared 六文件。
- 验收门：`696a12b0:DialogService:1792-1895` 的一次 same-frame green/yellow capture、green-first、green miss 才
  yellow、word order/坐标、Cloud alias/keyword/merge/fallback/action、strict image/words terminal 与 canonical 全闭合。
- C release 动作不是新 Java 任务：立即把 shared slot 授予 B。C 后继 TaskTracker whole class 仍
  `WAITING_EXPLICIT_PANEL_CAPTURE_OCR_PORT`；`ClientIdentityService` 为 `NEEDS_USER_DECISION`，不得拿来填槽。

### B - authoritative instant task on C release

- primary=`W-696-DIALOG-WHITE-STORY-TEMPLATE-FULL-CHAIN-1`；状态=`READY_ON_C_SOURCE_RELEASE`。父级在 C release
  同轮可直接把以下完整双仓写集发给 B，无额外 prerequisite。
- 专用 Cloud：New `remote/DialogWhiteStoryTemplateMacroCommand.java`、
  `DialogWhiteStoryTemplateMacroResult.java`、`CloudDialogWhiteStoryTemplatePort.java`；Modify
  `service/DialogService.java`；独占 Cloud shared 六文件。
- 专用 DHXY：New `cloud/remote/RemoteDialogWhiteStoryTemplateMacroCommandPayload.java`、
  `RemoteDialogWhiteStoryTemplateMacroResultPayload.java`；独占 DHXY shared 六文件；released
  white-story local mechanics 只读。
- chain/验收=`prepareWhiteStoryTemplate* -> Cloud DialogService -> typed port/transport -> handler -> same-frame
  mechanics -> MATCHED/STORY_MISS/STORY_ABSENT/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/MECHANICS_FAILED ->
  PreparedDialogAction/absent/miss`；保持 caller order、`0.85`、single-frame image、screen-absolute point、strict
  payload/codec/canonical/digest，本地零 option/fallback 业务选择。
- backup=`W-696-DIALOG-GREEN-TEMPLATE-OPTION-FULL-CHAIN-1`；状态=`ALTERNATIVE_SAME_SHARED_SLOT`；专用 Green
  command/result/port/payload + 同一 shared family，与 white-story 二选一，不能并行。

### D - prerequisite released, waits only for B shared release

- primary=`W-696-NPC-PLAYER-ANCHOR-FULL-CALLER-CHAIN-1`；状态=`WAITING_B_CHAIN_RELEASE`。D 的
  `service/npc/NpcClickPlayerAnchorLocalObservationMechanics.java` 已 release 并只读，不再有 mechanics prerequisite。
- 专用 Cloud：New `remote/NpcPlayerAnchorMacroCommand.java`、`NpcPlayerAnchorMacroResult.java`、
  `CloudNpcPlayerAnchorPort.java`；Modify `service/NpcClickService.java`；独占 Cloud shared 六文件。
- 专用 DHXY：New `cloud/remote/RemoteNpcPlayerAnchorMacroCommandPayload.java`、
  `RemoteNpcPlayerAnchorMacroResultPayload.java`；独占 DHXY shared 六文件；released mechanics 只读。
- chain/验收=`Cloud NpcClick player-anchor branch -> typed port -> handler -> local observation ->
  CAPTURED/NO_PURPLE_BLOB/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/INTERRUPTED/MECHANICS_FAILED -> Cloud
  OCR/provider fallback/map formula/candidate/click`；保持 696 Alt+4/capture、五 mask、purple wash/blob、PNG/SHA/
  dimensions/screen rect/native owner，所有业务算法留 Cloud。
- backup=`W-696-NAVIGATION-X2-CLOSED-MACRO-FULL-CHAIN-1`；状态=
  `WAITING_X2_QUEUE_BOUNDARY_AND_SHARED_SLOT`；X2/mouse-away/surrounding direct input 未闭合同一 macro 前不转 READY。

### A - prerequisite released, waits only behind D

- primary=`W-696-NPC-TASK-TOOLTIP-FULL-CALLER-CHAIN-1`；状态=`WAITING_D_CHAIN_RELEASE`。A 的
  `service/npc/NpcClickTaskTooltipLocalMacroMechanics.java` 已 release 并只读，不再有 mechanics prerequisite。
- 专用 Cloud：New `remote/NpcTaskTooltipMacroCommand.java`、`NpcTaskTooltipMacroResult.java`、
  `CloudNpcTaskTooltipPort.java`；Modify `service/NpcClickService.java`；独占 Cloud shared 六文件。
- 专用 DHXY：New `cloud/remote/RemoteNpcTaskTooltipMacroCommandPayload.java`、
  `RemoteNpcTaskTooltipMacroResultPayload.java`；独占 DHXY shared 六文件；released mechanics 只读。
- chain/验收=`clickNpcSmartWithOutcome -> Cloud tooltip strategy -> typed port -> handler -> tooltip mechanics ->
  VERIFIED/CLICK_NOT_VERIFIED/NOT_FOUND/BINDING_UNAVAILABLE/TEMPLATE_UNAVAILABLE/INTERRUPTED/MECHANICS_FAILED ->
  Cloud strategy/memory`；保持 `0.82/36`、region/point 顺序、move/150/click150/1200/verify、Y+90/ROI、零 retry，
  Cloud 保留 NPC/strategy/fallback。
- backup=`W-696-NPC-CTRL-MENU-WHOLE-LOCAL-MACRO-1`；状态=
  `WAITING_TEXT_RECOGNIZER_PROVIDER_PARITY`；provider fallback 未等价前不转 READY。

### Queue #25 no-filler / placement gate

- C release 后只发 B；B release 后只发 D；D release 后只发 A。四项专用 New 文件互不重叠，`DialogService`
  仅 C/B 串行，`NpcClickService` 仅 D/A 串行，shared family 全程单 writer。
- 若前项尚未 release，后项保持 `WAITING`；不得用 DTO/helper/zero-Java 小叶子制造占用。
- `ClientIdentityService` 继续 `NEEDS_USER_DECISION`。永久本地
  `BagService/UICleanerService/GiveItemService/QuestManagerService` 不迁 Cloud；不新增
  owner/permit/session/ledger/compaction/durable workflow/business TTL/auto retry。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #26 - C R1 后的 B -> D -> A 完整链实施单

记录时间：`2026-07-14T23:00:25-04:00`。本节只做 non-binding next-task queue 编排，不作源码裁决。
四份固定日志复读结果：C 已于 `2026-07-14T23:00:00-04:00` 领取
`W-696-DIALOG-OPTION-OCR-FULL-SAME-FRAME-CHAIN-1-R1`；父级在 `22:54:03` 记录的三项 P1 要求仍是
supplied-frame SHA、raw/green/yellow closed availability/RAW 路由和 detection-rect exact fresh capture。C 继续独占
`SHARED_LOCAL_MACRO_SLOT`。B、D、A 的本地 prerequisite 均已 source release，但后序主单仍严格固定为
`B white-story full chain -> D player-anchor caller full chain -> A tooltip caller full chain`。

### Queue #26 shared family（全程单 writer）

- Cloud 六文件：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/LocalMacroKind.java`、
  `LocalMacroCommand.java`、`LocalMacroRequest.java`、`LocalMacroOutcome.java`、
  `RemoteCommandOutcomeEnvelope.java`、`RemoteProtocolDigests.java`。
- DHXY 六文件：`src/main/java/com/bot/dhxy/cloud/remote/RemoteLocalMacroKind.java`、
  `RemoteLocalMacroCommandPayload.java`、`RemoteLocalMacroResultPayload.java`、
  `RemoteOperationPayloadCodec.java`、`RemoteProtocolDigests.java`、
  `LocalRemoteGameCommandHandler.java`。
- C R1 获得父级 source release 前，上述十二文件和 Cloud `DialogService.java` 继续归 C；B 不得提前领取。
  B release 后只交 D，D release 后只交 A。后三单不得拆成 DTO、enum、codec、helper 或 caller 小单。

### B 完整实现单 - `W-696-DIALOG-WHITE-STORY-TEMPLATE-FULL-CHAIN-1`

**发布状态与 shared 前置**

- 状态：`READY_IMMEDIATELY_AFTER_C_R1_SOURCE_RELEASE`。唯一前置是父级明确释放 C R1 的 25 Java 写集并把
  `SHARED_LOCAL_MACRO_SLOT` 转交 B；B 既有 prepared-action validation 写集已释放。
- 发布时必须整单发布并整单交付：
  `public prepareWhiteStoryTemplate* caller -> Cloud DialogService business orchestration -> typed local port/transport ->
  DHXY exact-context handler -> released DialogWhiteStoryTemplateLocalObservationMechanics -> closed terminal ->
  Cloud PreparedDialogAction/empty decision`。不得只交 command/result/port 或只接 handler。

**精确互斥写集（Cloud 10 + DHXY 8 + B 固定日志）**

- Cloud New：
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\DialogWhiteStoryTemplateMacroCommand.java`、
  `DialogWhiteStoryTemplateMacroResult.java`、`CloudDialogWhiteStoryTemplatePort.java`。
- Cloud Modify：Cloud shared 六文件；
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\DialogService.java`。
- DHXY New：
  `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteDialogWhiteStoryTemplateMacroCommandPayload.java`、
  `RemoteDialogWhiteStoryTemplateMacroResultPayload.java`。
- DHXY Modify：DHXY shared 六文件。只读、不修改
  `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\dialog\DialogWhiteStoryTemplateLocalObservationMechanics.java`。
- Append-only：B 固定日志
  `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-team-return-service-worker-b.md`。
- 排他证明：不触 C option-OCR 两个 mechanics/四个专用 payload/六个专用 Cloud contract；不触 D player-anchor、
  A tooltip mechanics；`DialogService` 与 shared family 只有 C release 后才转 B。

**`696a12b0` 对照方法范围**

- `DialogService.verifyWhiteStoryTemplate:449-497`：supplied frame 的同帧 thin-white wash、按 caller specs 原序、
  阈值 `0.85`、首个有效命中、relative 与 screen-absolute point。
- `DialogService.prepareWhiteStoryTemplate*:924-1019`：三个 public overload、supplied STORY/NONE 复用、恰一次 fresh
  detection fallback、STORY gate、match/miss/absent 分支及 public return。
- `DialogService.buildWhiteStoryAbsentPreparedAction:1021-1052`、
  `buildWhiteStoryMissPreparedAction:1054-1086`：target keyword、matched text、rect center、WHITE wash mode、
  `clickRequired=false`、timestamps/source/debug path。
- `DialogService.usableSuppliedStoryDetection:1616-1634`：仅 STORY 或 absentAllowed 下 NONE 可复用；其它 supplied
  detection 走原 fresh fallback。当前 Cloud public API 签名和 caller 次序不得改变。

**typed mechanics、terminal 与 Cloud decision 验收不变量**

- command 必须闭合携带 caller source/operation context、`absentAllowed`、caller-order `WhiteTemplateSpec`，以及
  supplied detection 的 closed type/rect/frame evidence；无 supplied frame 时由 released mechanics 恰一次 fresh detection，
  不建立 retained session/artifact owner。
- DHXY handler 必须在 exact `WindowRuntimeContext` 下恰一次调用 released mechanics；local state 只允许
  `MATCHED/STORY_MISS/STORY_ABSENT/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/MECHANICS_FAILED`。
- `MATCHED` 必须携 nullable template name、nonblank template path、relative point、screen-absolute point、同帧 rect/
  PNG/SHA/dimensions；其余 state 零 matched payload。两仓 constructor、sealed permits、flat key set、codec、request/
  outcome canonical digest 必须 field-for-field 对称；binary bytes 不直接进入 canonical JSON，SHA 覆盖 bytes。
- Cloud 保留全部业务决定：`MATCHED` 才构造原 matched `PreparedDialogAction`；`STORY_MISS` 仅按原
  `missTargetKeyword` 选择 miss action 或 empty；`STORY_ABSENT` 仅按原 `absentTargetKeyword/absentMatchedText` 选择
  absent action 或 empty；capture/binding/mechanics unavailable 不得在本地变成新的业务真值、retry 或 fail-closed gate。
- 保持 caller order、`0.85`、single authoritative frame、first-hit、point 坐标、fingerprint/debug source、timestamps 和
  fallback 次序；本地零 option/target/fallback/action 选择，零 input，零新增 TTL/retry/owner/session/wrapper。

**20 分钟 CLAIMED 模板**

父级发布时写明 `issuedAt=<ISO>`、`claimBy=<issuedAt+20m>`；B 必须在截止前于 B 固定日志真实 EOF 追加：

`CLAIMED | task=W-696-DIALOG-WHITE-STORY-TEMPLATE-FULL-CHAIN-1 | claimedAt=<ISO> | writeSet=[Cloud New DialogWhiteStoryTemplateMacroCommand.java,DialogWhiteStoryTemplateMacroResult.java,CloudDialogWhiteStoryTemplatePort.java; Cloud Modify LocalMacroKind.java,LocalMacroCommand.java,LocalMacroRequest.java,LocalMacroOutcome.java,RemoteCommandOutcomeEnvelope.java,RemoteProtocolDigests.java,DialogService.java; DHXY New RemoteDialogWhiteStoryTemplateMacroCommandPayload.java,RemoteDialogWhiteStoryTemplateMacroResultPayload.java; DHXY Modify RemoteLocalMacroKind.java,RemoteLocalMacroCommandPayload.java,RemoteLocalMacroResultPayload.java,RemoteOperationPayloadCodec.java,RemoteProtocolDigests.java,LocalRemoteGameCommandHandler.java; B-fixed-log] | claimBy=<ISO>`

只把按时追加的 CLAIMED 视为写集领取；不得因 20 分钟到点内部接管或拆单。

**不扰动主序的备用候选**

- `W-696-TASKTRACKER-PANEL-CLOUD-WHOLE-CLASS-1`，状态=
  `WAITING_EXPLICIT_TASK_CONTEXT_AND_PANEL_CAPTURE_OCR_PORT`；未来唯一 authored 写集仅 Cloud
  `src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java` + 被分配 Worker 的固定日志，shared family 与
  `DialogService/NpcClickService` 全部只读。真实 context producer、panel capture/OCR artifact 与 terminal 未闭合前
  **零 reservation、不得发布**；因此它不会插入或改写 B -> D -> A 主序。

### D 完整实现单 - `W-696-NPC-PLAYER-ANCHOR-FULL-CALLER-CHAIN-1`

**发布状态与 shared 前置**

- 状态：`READY_IMMEDIATELY_AFTER_B_WHITE_STORY_SOURCE_RELEASE`。D 的
  `NpcClickPlayerAnchorLocalObservationMechanics.java` 已 release；唯一剩余前置是 B 完整 white-story 链被父级明确
  source release 并把 `SHARED_LOCAL_MACRO_SLOT` 转交 D。
- 必须闭合：`Cloud NpcClickService player-anchor strategy call site -> typed port/transport -> DHXY exact-context handler ->
  released local observation mechanics -> closed terminal/evidence -> Cloud OCR/provider fallback -> map formula/candidate ->
  existing prepared-point click/verify -> strategy result/memory`。不得把本地 observation、DTO/wire 或 Cloud caller 拆单。

**精确互斥写集（Cloud 10 + DHXY 8 + D 固定日志）**

- Cloud New：
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\NpcPlayerAnchorMacroCommand.java`、
  `NpcPlayerAnchorMacroResult.java`、`CloudNpcPlayerAnchorPort.java`。
- Cloud Modify：Cloud shared 六文件；
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NpcClickService.java`。
- DHXY New：
  `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteNpcPlayerAnchorMacroCommandPayload.java`、
  `RemoteNpcPlayerAnchorMacroResultPayload.java`。
- DHXY Modify：DHXY shared 六文件。只读、不修改
  `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\npc\NpcClickPlayerAnchorLocalObservationMechanics.java`。
- Append-only：D 固定日志
  `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-return-item-prescan-state-worker-d.md`。
- 排他证明：B release 前不触 shared；D 期间 A 不触 shared/Cloud `NpcClickService`；不触 yellow-target、prepared-point、
  tooltip mechanics 或 C/B 专用 contract/payload。

**`696a12b0` 对照方法范围**

- `NpcClickService.runNpcClickPipeline:778-934` 与 `tryPlayerAnchorFormulaStrategy:998-1050`：player-anchor 仍在
  yellow strategy 后、Ctrl fallback 前；known-coordinate、first region、outside-window、Ctrl-origin/immediate Ctrl fallback
  顺序不变。
- `NpcClickService.prepareNpcOcrScanImage:2505-2531`：仅 default full-window 且
  `skipDefaultOcrMask=false` 时应用五块既有 mask；tight region/skip 分支原样。
- `NpcClickService.calculatePlayerAnchorFormulaPoint:2865-3001`：identity gate、`prepareAlt4`、capture、stop fences、
  same-frame purple preparation、OCR provider matcher/recheck、purple blob fallback、logical-to-physical formula、Y `-50`、
  tune 与 null fallback。
- `NpcClickService.extractPlayerAnchorMatchFromWords:3106-3115`、
  `extractPurpleBlobAnchor:3132-3189`：玩家名 OCR anchor 优先，blob 仅为同帧 fallback；dark-pixel/shape bounds 与
  screen-absolute mapping 不变。

**typed mechanics、terminal 与 Cloud decision 验收不变量**

- command 只携 exact binding 对应的 caller-decided window-relative scan rect、`prepareAlt4`、
  `skipDefaultMask`；不得下沉 player identity、OCR target、map location/formula、candidate verdict、click/verify/fallback。
- DHXY handler exact-context 恰一次调用 released `observe`；local terminal 只允许
  `CAPTURED/NO_PURPLE_BLOB/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/INTERRUPTED/MECHANICS_FAILED`。
  `CAPTURED/NO_PURPLE_BLOB` 均携 raw/prepared same-frame evidence 与 scan rect，只有 `CAPTURED` 携 screen-absolute blob；
  其它 terminal 零 evidence/blob。
- 保持需要时 Alt+4 -> settle 400 -> fresh exact capture、capture 后/洗图前 interruption fences、default-mask 五块、
  HSV purple wash、blob bounds、PNG magic/SHA/dimensions/rect containment、screen-absolute anchor、BufferedImage/OpenCV
  owner 恰释放；不新增 capture、read、retry 或 stop gate。
- Cloud 必须先在 returned prepared evidence 上执行原玩家名 OCR provider matcher/recheck，再仅在 OCR anchor 缺失时使用
  typed blob；随后执行原 map formula、candidate/click/verify 与 strategy/memory。`NO_PURPLE_BLOB` 不是业务终局，
  unavailable/interrupted/mechanics terminal 也不得变成新的 NPC/fallback 真值。
- 两仓 kind/command/result/envelope/codec/digest 严格对称，terminal 不折叠；image bytes 由 SHA 覆盖且不直接进入
  canonical JSON；零 retained local session、零本地 OCR/公式/candidate/click 决策。

**20 分钟 CLAIMED 模板**

父级发布时写明 `issuedAt=<ISO>`、`claimBy=<issuedAt+20m>`；D 必须在截止前于 D 固定日志真实 EOF 追加：

`CLAIMED | task=W-696-NPC-PLAYER-ANCHOR-FULL-CALLER-CHAIN-1 | claimedAt=<ISO> | writeSet=[Cloud New NpcPlayerAnchorMacroCommand.java,NpcPlayerAnchorMacroResult.java,CloudNpcPlayerAnchorPort.java; Cloud Modify LocalMacroKind.java,LocalMacroCommand.java,LocalMacroRequest.java,LocalMacroOutcome.java,RemoteCommandOutcomeEnvelope.java,RemoteProtocolDigests.java,NpcClickService.java; DHXY New RemoteNpcPlayerAnchorMacroCommandPayload.java,RemoteNpcPlayerAnchorMacroResultPayload.java; DHXY Modify RemoteLocalMacroKind.java,RemoteLocalMacroCommandPayload.java,RemoteLocalMacroResultPayload.java,RemoteOperationPayloadCodec.java,RemoteProtocolDigests.java,LocalRemoteGameCommandHandler.java; D-fixed-log] | claimBy=<ISO>`

只检查领取，不以截止时间要求交付；逾期仍由父级在 D 真实 EOF 重发原整单，不拆分、不内部接管。

**不扰动主序的备用候选**

- `W-696-NAVIGATION-X2-CLOSED-MACRO-FULL-CHAIN-1`，状态=
  `WAITING_X2_QUEUE_BOUNDARY_AND_QUEUE26_RELEASE`。X2、successful mouse-away 与 surrounding direct input 必须先被
  证明可在同一 closed local macro 中执行，且禁止 exclusive callback 内二次排队。Queue #26 的 A caller 链 release
  前零 reservation；未来另行列精确 contract/write set，不能借备用项触碰当前 shared family，因此不与主序并发。

### A 完整实现单 - `W-696-NPC-TASK-TOOLTIP-FULL-CALLER-CHAIN-1`

**发布状态与 shared 前置**

- 状态：`READY_IMMEDIATELY_AFTER_D_PLAYER_ANCHOR_CHAIN_SOURCE_RELEASE`。A 的
  `NpcClickTaskTooltipLocalMacroMechanics.java` 已 release；唯一剩余前置是 D 完整 player-anchor caller 链被父级明确
  source release 并把 `SHARED_LOCAL_MACRO_SLOT` 转交 A。
- 必须闭合：`clickNpcSmartWithOutcome/current public caller -> Cloud NpcClickService normal-tooltip strategy -> typed
  port/transport -> DHXY exact-context handler inside the serialized input worker -> released tooltip mechanics -> closed
  terminal/payload -> Cloud NpcClickStrategyResult + recordSmartClickEvidence/memory -> existing later fallback`。
  不得只交 DTO/helper/wire，也不得重写 released mechanics。

**精确互斥写集（Cloud 10 + DHXY 8 + A 固定日志）**

- Cloud New：
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\NpcTaskTooltipMacroCommand.java`、
  `NpcTaskTooltipMacroResult.java`、`CloudNpcTaskTooltipPort.java`。
- Cloud Modify：Cloud shared 六文件；
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NpcClickService.java`。
- DHXY New：
  `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteNpcTaskTooltipMacroCommandPayload.java`、
  `RemoteNpcTaskTooltipMacroResultPayload.java`。
- DHXY Modify：DHXY shared 六文件。只读、不修改
  `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\npc\NpcClickTaskTooltipLocalMacroMechanics.java`。
- Append-only：A 固定日志
  `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-npc-click-service-worker-a.md`。
- 排他证明：D release 前不触 shared/Cloud `NpcClickService`；不触 player-anchor、yellow-target、prepared-point、
  Ctrl mechanics 或 B/C Dialog contract/payload。A 是 Queue #26 最后一名 shared writer。

**`696a12b0` 对照方法范围**

- `NpcClickService.executeMoveClickAndVerify:176-216`：move -> 150ms -> click hold 150ms -> first wait 1200ms ->
  verifier；tooltip caller `maxRetries=0`，恰一次 verify、零 retry。
- `NpcClickService.runNpcClickPipeline:778-934`、`tryNormalTooltipStrategy:984-996`：非五倍时 normal-tooltip 的原
  strategy 位置、返回语义、后续 dialog/yellow/player-anchor/Ctrl fallback 次序不变。
- `NpcClickService.clickNpcByTaskTooltipTemplate:1147-1260`：tooltip-disabled/no-region、caller regions 原序、每 region
  `0.82/36` 全匹配原序、first verified 停、clicked misses 全耗尽、NOT_FOUND 与 click miss 分离。
- `NpcClickService.recordSmartClickEvidence:1285-1401`：click sample、tooltip-derived ROI evidence、pending/confirmed
  memory 写入条件不变。
- `NpcClickService.directNpcPointFromTooltipCenter:1433-1448`、
  `tooltipLearnedRoiFromTooltipCenter:1450-1469`：record point 为 screen-absolute center Y+90；learned ROI 为
  window-relative `[-150,-100,+150,+200]` 并 clamp `1024x768`。

**typed mechanics、terminal 与 Cloud decision 验收不变量**

- Cloud 仍决定 tooltip eligibility、template path、caller-order screen-absolute regions、NPC/strategy/fallback 与 verifier
  业务；command 不得让 DHXY 重新选 NPC、region、strategy 或 fallback。
- handler 必须在既有 remote-exclusive/input-worker 段 direct 恰一次调用 released mechanics，绝不 queue-in-queue；
  mechanics 的 entry context binding、post-capture empty/nonempty binding/geometry 重验均保持。
- typed status 只允许
  `VERIFIED/CLICK_NOT_VERIFIED/NOT_FOUND/BINDING_UNAVAILABLE/TEMPLATE_UNAVAILABLE/INTERRUPTED/MECHANICS_FAILED`。
  前两态必须 `clickProduced=true` 且携 record point/learned ROI；视觉 miss/availability terminal 不得伪造 click/payload；
  interruption/mechanics failure 保留实际 `clickProduced`，不得折叠成 NOT_FOUND。
- 保持 threshold/dedup、region/point 原序、同一 serialized sequence 内
  `move -> sleep150 -> clickLeft hold150 -> sleep1200 -> verify`、first verified 停、零 retry、Y+90/ROI、
  exact binding/frame owner 与 verify 次数。两仓 constructor/key/codec/canonical/digest 对称，无新增 retry/TTL/session/
  owner/wrapper/checkpoint。
- Cloud terminal mapping 必须恢复原 `NpcClickStrategyResult`：VERIFIED 结束该 strategy；CLICK_NOT_VERIFIED 写原 click
  evidence 后继续既有 fallback；NOT_FOUND 仅视觉 miss；binding/template/interrupted/mechanics 作为 unavailable/failure
  证据进入既有 strategy flow，不得改写 NPC 选择、memory gate 或后续 fallback 顺序。

**20 分钟 CLAIMED 模板**

父级发布时写明 `issuedAt=<ISO>`、`claimBy=<issuedAt+20m>`；A 必须在截止前于 A 固定日志真实 EOF 追加：

`CLAIMED | task=W-696-NPC-TASK-TOOLTIP-FULL-CALLER-CHAIN-1 | claimedAt=<ISO> | writeSet=[Cloud New NpcTaskTooltipMacroCommand.java,NpcTaskTooltipMacroResult.java,CloudNpcTaskTooltipPort.java; Cloud Modify LocalMacroKind.java,LocalMacroCommand.java,LocalMacroRequest.java,LocalMacroOutcome.java,RemoteCommandOutcomeEnvelope.java,RemoteProtocolDigests.java,NpcClickService.java; DHXY New RemoteNpcTaskTooltipMacroCommandPayload.java,RemoteNpcTaskTooltipMacroResultPayload.java; DHXY Modify RemoteLocalMacroKind.java,RemoteLocalMacroCommandPayload.java,RemoteLocalMacroResultPayload.java,RemoteOperationPayloadCodec.java,RemoteProtocolDigests.java,LocalRemoteGameCommandHandler.java; A-fixed-log] | claimBy=<ISO>`

只检查领取；未按时领取时只在 A 真实 EOF 重发同一整单，不把任一 shared/DTO/caller 子集交给其它 Worker。

**不扰动主序的备用候选**

- `W-696-NPC-CTRL-MENU-WHOLE-LOCAL-MACRO-1`，状态=
  `WAITING_TEXT_RECOGNIZER_PROVIDER_PARITY`；未来唯一 Java 写集为 DHXY New
  `src/main/java/com/bot/dhxy/service/npc/NpcClickCtrlMenuWholeLocalMacroMechanics.java` + 被分配 Worker 固定日志，
  anchor=`696a12b0:NpcClickService:303-585`，必须整段闭合 Ctrl-hold/hover/before-after/yellow OCR/click/verify/release。
  provider fallback 等价性未闭合前零 reservation；它不写 shared family/Cloud `NpcClickService`，不会插入主序。

### Queue #26 placement / dispatch / no-filler gate

- 三个主单的 Service 落点没有未决歧义：`DialogService`、`NpcClickService` 的业务编排继续在 Cloud，exact-window
  capture/image/OCR primitive/physical input mechanics 与 fixed delays 留 DHXY；这与迁移矩阵 settled boundary 一致。
- `ClientIdentityService` 仍标 `NEEDS_USER_DECISION`：raw title 在 DHXY 解析后上送 typed identity，或上送 raw title
  由 Cloud 解析，两案均可；本 helper 不替用户选，也不给它预留文件。
- C R1 release 时只发 B 全链；B release 时只发 D 全链；D release 时只发 A 全链。backup 均为零 reservation，
  不能作为等待期间 filler，不能抢 shared slot，不能改变主序。
- 实施 Worker 保护当前两仓 dirty/untracked，不回退他人改动；按父级发单要求交 scoped diff/SHA/基线逐项对照。
  本 queue helper 本轮不运行 build/test/runtime/Git mutation；实际 Java writer 的后续编译门由父级在所有 shared writer
  稳定后统一安排。
- 无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #27 - four non-shared service-specific tranches

记录时间：`2026-07-14`。本节仅做 next-task queue 编排，不作源码审查或最终裁决。按父级最新指令，四路先完成
完整链中全部 service-specific non-shared 部分；两仓 generic `LocalMacro` shared 12 文件与 C 当前 R2 的 7 文件均
冻结，后续只由一名 shared integrator 一次注册 kind/permit/request/outcome/envelope、codec/digest 与 handler dispatch。

### 全局冻结集与计数口径

- Cloud generic shared 6 文件冻结：`LocalMacroKind.java`、`LocalMacroCommand.java`、`LocalMacroRequest.java`、
  `LocalMacroOutcome.java`、`RemoteCommandOutcomeEnvelope.java`、`RemoteProtocolDigests.java`。
- DHXY generic shared 6 文件冻结：`RemoteLocalMacroKind.java`、`RemoteLocalMacroCommandPayload.java`、
  `RemoteLocalMacroResultPayload.java`、`RemoteOperationPayloadCodec.java`、`RemoteProtocolDigests.java`、
  `LocalRemoteGameCommandHandler.java`。
- C 当前 R2 的 7 文件冻结：Cloud `CloudDialogOptionOcrImagePort.java`、
  `DialogOptionOcrImageMacroCommand.java`、`DialogOptionOcrImageMacroResult.java`；DHXY
  `LocalRemoteGameCommandHandler.java`、`RemoteDialogOptionOcrImageMacroCommandPayload.java`、
  `RemoteDialogOptionOcrImageMacroResultPayload.java`、`DialogOptionOcrImageLocalObservationMechanics.java`。
- 下列 tranche 只形成 service-specific command/result/port/payload 与明确列出的 Cloud caller；在 shared integrator
  完成前不可达或不可编译属于已知接线缺口，**不计完整 caller-to-terminal 链，不增加 `189/407`**。不得由各 Worker
  私自补 shared 注册、codec、digest 或 handler。

### External B - full white-story non-shared tranche

- task=`W-696-DIALOG-WHITE-STORY-TEMPLATE-NON-SHARED-TRANCHE-1`。
- 唯一 Java 写集（6）：Cloud New
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/DialogWhiteStoryTemplateMacroCommand.java`、
  `DialogWhiteStoryTemplateMacroResult.java`、`CloudDialogWhiteStoryTemplatePort.java`；Cloud Modify
  `src/main/java/com/bot/dhxy/service/DialogService.java`；DHXY New
  `src/main/java/com/bot/dhxy/cloud/remote/RemoteDialogWhiteStoryTemplateMacroCommandPayload.java`、
  `RemoteDialogWhiteStoryTemplateMacroResultPayload.java`。另仅 append B 固定日志。
- `696a12b0` 对照：`DialogService.verifyWhiteStoryTemplate:449-497`、
  `prepareWhiteStoryTemplate*:924-1019`、`buildWhiteStoryAbsentPreparedAction:1021-1052`、
  `buildWhiteStoryMissPreparedAction:1054-1086`、`usableSuppliedStoryDetection:1616-1634`。
- 依赖证据：`DialogWhiteStoryTemplateLocalObservationMechanics.java` 已 source release，作为只读 terminal/mechanics
  权威；Cloud `DialogService` 当前不在 C R2 七文件中。验收 caller order、`0.85`、supplied/fresh single-frame、
  nullable template name、match/miss/absent mapping、relative/screen-absolute point 与 evidence 合同；不改本地 mechanics。
- 互斥：不触 C 七文件、shared 12、A tooltip/prepared-point、C yellow-target、D player-anchor 文件。

### External A - tooltip + prepared-point ten-contract tranche

- task=`W-696-NPC-TOOLTIP-PREPARED-POINT-NON-SHARED-CONTRACTS-1`。
- 唯一 Java 写集（10）：Cloud New
  `NpcTaskTooltipMacroCommand.java`、`NpcTaskTooltipMacroResult.java`、`CloudNpcTaskTooltipPort.java`、
  `NpcPreparedPointMacroCommand.java`、`NpcPreparedPointMacroResult.java`、`CloudNpcPreparedPointPort.java`，均位于
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/`；DHXY New
  `RemoteNpcTaskTooltipMacroCommandPayload.java`、`RemoteNpcTaskTooltipMacroResultPayload.java`、
  `RemoteNpcPreparedPointMacroCommandPayload.java`、`RemoteNpcPreparedPointMacroResultPayload.java`，均位于
  `src/main/java/com/bot/dhxy/cloud/remote/`。另仅 append A 固定日志。
- `696a12b0` 对照：tooltip=`NpcClickService:1147-1260,176-216,1433-1469`；prepared-point=
  `executeMoveClickAndVerify:176-216` 及四个真实 caller 的 `maxRetries=0/1` 输入域。
- 依赖证据：`NpcClickTaskTooltipLocalMacroMechanics.java` R2 与
  `NpcClickPreparedPointLocalMacroMechanics.java` R1 均已 source release；两者只读。验收两组 command/result/port/payload
  字段与 terminal 一一对应，tooltip 保持 `0.82/36`、Y+90/ROI、零 retry，prepared-point 保持 0/1 retry、
  clickProduced 与 verify terminal，不在本 tranche 修改 Cloud `NpcClickService.java`。
- 互斥：10 个文件均为专用 New；不触 B/C/D 专用类型、C 七文件或 shared 12。

### External C - yellow-target five-contract tranche

- task=`W-696-NPC-YELLOW-TARGET-NON-SHARED-CONTRACTS-1`；只有 C 当前 R2 source release 后才可领取，不能与
  自己的 option-OCR R2 并写固定日志或工作树。
- 唯一 Java 写集（5）：Cloud New
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/NpcYellowTargetMacroCommand.java`、
  `NpcYellowTargetMacroResult.java`、`CloudNpcYellowTargetPort.java`；DHXY New
  `src/main/java/com/bot/dhxy/cloud/remote/RemoteNpcYellowTargetMacroCommandPayload.java`、
  `RemoteNpcYellowTargetMacroResultPayload.java`。另在 R2 释放后仅 append C 固定日志。
- `696a12b0` 对照：`NpcClickService.runNpcClickPipeline:778-934` 的 yellow-first strategy、
  `prepareNpcOcrScanImage:2505-2531` 的 default-mask/skip 分支，以及
  `GameTextLineOcrService.findYellowTextCandidateResult` 的 strict-yellow/shadow/component/line/gap/score/sort 闭包。
- 依赖证据：`NpcClickYellowTargetLocalObservationMechanics.java` R2 已 source release并只读；command/result 必须保留
  prepared same-frame evidence、候选原序、screen-absolute mapping 与 unavailable/interrupted/mechanics terminal，
  不把 NPC target/OCR/click/verify/fallback 决策搬到 DHXY。
- 互斥：五个专用 New 与 C 当前七文件名称零交集，也不触 A/B/D 专用类型或 shared 12。

### External D - player-anchor five-contract tranche

- task=`W-696-NPC-PLAYER-ANCHOR-NON-SHARED-CONTRACTS-1`。
- 唯一 Java 写集（5）：Cloud New
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/NpcPlayerAnchorMacroCommand.java`、
  `NpcPlayerAnchorMacroResult.java`、`CloudNpcPlayerAnchorPort.java`；DHXY New
  `src/main/java/com/bot/dhxy/cloud/remote/RemoteNpcPlayerAnchorMacroCommandPayload.java`、
  `RemoteNpcPlayerAnchorMacroResultPayload.java`。另仅 append D 固定日志。
- `696a12b0` 对照：`NpcClickService.tryPlayerAnchorFormulaStrategy:998-1050`、
  `calculatePlayerAnchorFormulaPoint:2865-3001`、`extractPlayerAnchorMatchFromWords:3106-3115`、
  `extractPurpleBlobAnchor:3132-3189` 与 `prepareNpcOcrScanImage:2505-2531`。
- 依赖证据：`NpcClickPlayerAnchorLocalObservationMechanics.java` R2 已 source release并只读；验收
  `CAPTURED/NO_PURPLE_BLOB/CAPTURE_UNAVAILABLE/BINDING_UNAVAILABLE/INTERRUPTED/MECHANICS_FAILED`、raw/prepared
  same-frame evidence、scan rect、screen-absolute blob、PNG/SHA/dimensions。Cloud OCR-first/blob-fallback、map formula、
  candidate/click/verify 均不在本 tranche 下沉；本 tranche 不修改 Cloud `NpcClickService.java`。
- 互斥：五个专用 New 与 A 的 tooltip/prepared-point、C yellow-target、B white-story、C 当前七文件及 shared 12
  全部零交集。

### 后续 single shared integrator gate

- 四路 source release 后，由**单一 shared integrator**统一修改 shared 12，并按固定顺序注册 white-story、
  tooltip、prepared-point、yellow-target、player-anchor 的 kind/permit/request/outcome/envelope、两仓 codec/digest 与
  DHXY handler dispatch；同一 integrator 同轮补 Cloud `NpcClickService` 的 tooltip/prepared-point/yellow/player-anchor
  caller 映射。B 已在本 tranche 修改的 `DialogService` 不得被重写，只做 shared 接线复核。
- integrator 必须逐项核对两仓 constructor、flat key set、canonical digest、terminal 不折叠和 released mechanics
  恰一次调用；在该步完成并通过后续统一编译门前，Queue #27 四项都只是 non-shared 可复用材料，不宣称完整迁移。
- 四路保护两仓 dirty/untracked，不回退他人改动；不新增 owner/permit/session/ledger/compaction/durable workflow/
  business TTL/auto retry。无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Candidate Queue #28 - full caller-chain closures only

记录时间：`2026-07-15`。本 Queue 取代 #27 的小合同切片排法；后续只发能一次闭合
`public caller -> Cloud Service -> typed boundary -> DHXY mechanics -> closed terminal -> Cloud result/next decision`
的整单。禁止再发 DTO、enum、port、payload、codec、digest、handler 或单 caller 小单。

### External B primary - atomic four-NPC shared integration

- **task**：`W-696-NPC-FOUR-FAMILY-ATOMIC-SHARED-INTEGRATION-1`。
- **writeSet**：B 完成并 source-release 当前 19-Java white-story 后，单独独占 Cloud generic shared 6、DHXY generic
  shared 6、Cloud `NpcClickService.java`，以及 tooltip、prepared-point、yellow-target、player-anchor 四组现有专用
  command/result/port/payload；只允许为四个 port 的真实 transport implementation/assembly 新增必要文件。B 固定日志
  append-only；四组 released mechanics 只读。
- **dependency**：当前 B white-story 19-Java 全链 source release；A tooltip+prepared、C yellow、D player-anchor R1
  均获父级 source approval；shared 12 从 white-story 清洁转交且无未决 repair。
- **conflict**：本任务执行期间 A/C/D 不得触 shared 12、Cloud `NpcClickService` 或四组专用合同；四组必须同一
  Worker、同一 reservation、同一交付，禁止按 DTO/种类拆单。
- **acceptance**：一次闭合四条 public caller 到 closed terminal；统一完成 kind/permits/request/outcome/envelope、
  两仓 codec/canonical digest、DHXY exact-context handler dispatch、四个 typed port 实现及 Cloud `NpcClickService`
  strategy/fallback/memory 映射；terminal 不折叠、mechanics 恰一次调用、`696a12b0` 顺序/阈值/retry/坐标不变。
- **status**：`BLOCKED_CURRENT_B_WHITE_STORY_AND_A_C_D_R1_PARENT_APPROVAL`；前置全清后直接 `READY`，不得插 filler。

### External B backup - DialogService whole-class closure

- **task**：`W-696-DIALOG-SERVICE-WHOLE-CALLER-CHAIN-CLOSURE-1`。
- **writeSet**：Cloud `DialogService.java` 及其 service-specific typed dialog contracts/port assembly；DHXY 已批准
  dialog detection/option/white-story mechanics 与专用 payload/handler branch；不触 NPC 四组 shared reservation。
- **dependency**：white-story source release；option-OCR 与 dialog-detection 现有 source-approved 材料可用。
- **conflict**：与 B primary 二选一发布；若需要改 generic shared 12，状态转 `BLOCKED`，不得抢 NPC 原子任务。
- **acceptance**：全部 public dialog caller 经 Cloud policy 到 typed local mechanics 和 terminal，再回到 prepared action/
  dialog result；保留 STORY/OPTION/NONE、fallback、supplied/fresh frame 与点击顺序。
- **status**：`ALTERNATIVE_AFTER_WHITE_STORY`；具体 port implementation/assembly 落点不唯一时
  `NEEDS_USER_DECISION`。

### External A primary / backup

- **task**：primary=`W-696-BATTLE-RADAR-WHOLE-INTEGRATION-CLOSURE-1`；backup=
  `W-696-AUTO-COMBAT-SERVICE-WHOLE-CALLER-CHAIN-1`。
- **writeSet**：primary 仅 Cloud `BattleRadarService.java`、其 source-approved `BATTLE_RADAR_*` typed contracts/port/
  assembly 与对应 DHXY radar mechanics handler；backup 仅 Cloud `AutoCombatService.java` 及其 service-specific typed
  collaborators/assembly。两者均不触 generic shared 12、`NpcClickService`、`DialogService`、`PlayerStateService`、
  `NavigationService`；A 固定日志 append-only。
- **dependency**：primary 需现有 BattleRadar R2/contracts source approval；backup 需 BattleRadar、PlayerState、panel、
  maintenance typed terminals 全部可调用。
- **conflict**：primary 与 backup 二选一；B NPC 原子任务期间仍可并行，但发现需写 shared 12 即停止。
- **acceptance**：primary 闭合 AutoCombat caller -> Cloud radar state machine -> typed capture mechanics -> enter/exit/
  fast-exit terminal；backup 闭合 public combat tick 到 radar/panel/supply mechanics terminal 与恢复结果，不留本地编排。
- **status**：primary=`READY_AFTER_A_R1_PARENT_RELEASE`；backup=`BLOCKED_TYPED_COLLABORATOR_CLOSURE`。

### External C primary / backup

- **task**：primary=`W-696-PLAYER-STATE-SERVICE-WHOLE-CALLER-CHAIN-1`；backup=
  `W-696-DIALOG-SERVICE-WHOLE-CALLER-CHAIN-CLOSURE-1-C`。
- **writeSet**：primary 仅 Cloud `PlayerStateService.java`、HP/MP/identity/location/incense service-specific typed
  contracts/port/assembly 与对应 DHXY observation/input mechanics handler；backup 采用 B backup 的 Dialog 整类写集，
  但仅在 B 明确放弃且 source release 后转交。C 固定日志 append-only。
- **dependency**：primary 需四目标血法、身份、位置、香状态 typed producer 与 terminal source approval；backup 需 B
  white-story release 且 B 未领取 Dialog whole-class。
- **conflict**：不触 B shared 12/NpcClick、A BattleRadar/AutoCombat、D Navigation；backup 零 reservation直到正式转交。
- **acceptance**：primary 闭合 AutoBattleTask/AutoCombat public caller -> Cloud PlayerState decision -> typed local
  observation/input -> closed terminal -> Cloud state/result，保留四目标顺序、阈值、exclusive 补给与 quiet-period 语义。
- **status**：primary=`BLOCKED_TYPED_PRODUCER_APPROVAL`；backup=`ALTERNATIVE_AFTER_B_RELEASE`；身份/位置 authority
  或 port assembly 落点不唯一时 `NEEDS_USER_DECISION`。

### External D primary / backup

- **task**：primary=`W-696-NAVIGATION-SERVICE-WHOLE-CALLER-CHAIN-1`；backup=
  `W-696-NAVIGATION-X2-CLOSED-MACRO-INTEGRATION-1`。
- **writeSet**：primary 仅 Cloud `NavigationService.java`、route/minimap service-specific typed contracts/port/assembly
  与 DHXY navigation mechanics handler；backup 仅三个 X2 caller、其一个 closed X2+mouse-away+surrounding-input
  mechanics/typed boundary/handler。均不触 B shared 12/NpcClick/Dialog、A combat、C player-state；D 日志 append-only。
- **dependency**：primary 需现有 Navigation source-approved contracts/mechanics 与 terminal-fact gate 可用；backup 需
  X2 exclusive queue boundary 的单宏落点获明确批准。
- **conflict**：primary/backup 二选一；禁止 exclusive callback 内二次排队，禁止拆 X2、mouse-away、外围 input。
- **acceptance**：primary 闭合 navigate public caller -> Cloud route/minimap plan -> typed local executor -> pathing/
  arrival terminal；保持 60s loop、候选顺序、keep-turn、identity/lease/stop 与 terminal-fact gate。backup 三 caller 同宏闭合。
- **status**：primary=`READY_AFTER_D_R1_PARENT_RELEASE`；backup=`BLOCKED_X2_QUEUE_BOUNDARY`；X2 handler/queue
  ownership 落点仍有多案，标 `NEEDS_USER_DECISION`。

### Dispatch gate

- B white-story 未 release 前不发布 B primary；A/C/D R1 未获父级 source approval，不把对应材料接入任何完整链。
- 同时可运行的主单写集必须互斥；backup 全部零 reservation。任何完整链出现落点歧义即
  `NEEDS_USER_DECISION`，不得退回小合同切片。无已批准业务差异；按 `696a12b0` 基线等价迁移。
