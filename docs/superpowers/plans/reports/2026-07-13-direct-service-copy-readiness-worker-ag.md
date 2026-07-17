# Direct Service Copy Readiness：Internal Worker AG

日期：2026-07-13

## 范围与基线

- DHXY：`D:\mavenProject\DHXY`
- Cloud：`D:\mavenProject\dhxy-cloud-brain`
- DHXY 业务基线：`0114604e1ff5f15491d2910959c45252e893d04f`，即当前 `HEAD`。
- DHXY 当前分支：`thin-client-design`；Cloud 当前分支：`navigation-migration`。
- 当前工作区均有既有 dirty/untracked；本盘点未回滚、覆盖、清理、提交，也未运行 Maven 或启动任何运行面。
- Cloud 目标存在性按“同名 Service Java 类实际存在”判断，不把决策叶子、DTO、支撑类或报告中的计划文件算作目标已存在。
- 已核对 `docs/业务逻辑.md`：通用盒子逻辑（约第 69-168 行）、Expected 战斗快脱战与回程验证（约第 213-281 行）、修罗/五倍普通怪入战识别与 fallback 边界（约第 283-335 行）。本报告不改变这些规则。

## 总表

| Service | 源相对 `0114604e` 的业务 diff | Cloud 同名目标 | 分类 |
|---|---|---|---|
| `AutoCombatPanelService` | 无 | 否；仅有 `AutoCombatPanelDecision` 支撑叶子 | `COPY_WITH_SHARED_PORT` |
| `AutoCombatService` | 有；当前 dirty 改动 11 行新增、9 行删除 | 否 | `DIRECT_COPY` |
| `BagService` | 有；当前 dirty 新增 134 行并已有 caller 使用 | 否；仅有 Bag 状态支撑类 | `COPY_WITH_SHARED_PORT` |
| `BattleRadarService` | 无 | 否 | `LOCAL_RESIDENT` |
| `CommonBoxService` | 无 | 否 | `COPY_WITH_SHARED_PORT` |
| `DialogService` | 无 | 否；仅有对话相关模型/策略支撑 | `LOCAL_RESIDENT` |
| `GiveItemService` | 无 | 否 | `COPY_WITH_SHARED_PORT` |
| `LeftTopStatusSwitchService` | 无 | 否；已有 `LeftTopStatusDecision` 与模板匹配支撑 | `COPY_WITH_SHARED_PORT` |
| `NavigationService` | 有；当前 dirty 删除 318 行旧路径 | 否；已有导航决策/属性/解析支撑 | `LOCAL_RESIDENT` |
| `NpcClickService` | 有；当前 dirty 新增 2 行、删除 105 行请求模板元数据逻辑 | 否；已有 NPC 本地验证/扫描类型与 Cloud decision 支撑 | `LOCAL_RESIDENT` |

## 逐类盘点

### 1. AutoCombatPanelService

- 源路径：`src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`
- Cloud 目标：不存在同名 `AutoCombatPanelService.java`。已有相关支撑：`src/main/java/com/bot/dhxy/service/AutoCombatPanelDecision.java`。
- 基线 diff：无；`git diff 0114604e -- <path>` 为空。
- 构造依赖：`GameClientTracker`、`CoordinateHelper`、`InputSequences`、`WindowTaskContextHolder`、`AutomationMetricsService`、`GameContext`、`BotProperties`。
- 直接调用的本地能力：`tracker.updateGlobalVision()` / 最新截图路径与截图审计；`CoordinateHelper.findImageAbsoluteCoordinateByImagePath(...)` 模板定位；窗口基点读取；`InputSequences` 发送 Alt+8 与拖拽；窗口告警指标；`GameContext` 回合估算读写。
- 分类：`COPY_WITH_SHARED_PORT`。方法主体是面板显示、定位、对齐和刷新业务，物理动作与截图/模板匹配是共享端口替换面。
- 实现时最小替换点：保留 `ensurePanelVisible`、`verifyAndAlignPanel`、刷新分支的顺序、坐标、阈值和等待；仅替换 `updateGlobalVision + CoordinateHelper` 的截图/匹配调用，以及三处现有输入边界：`openAutoPanel` 的 `[pressAlt8, sleep]`、拖拽的 `[dragAndDrop, sleep(500)]`、刷新面板的 `[pressAlt8, sleep(1000)]`。不改 `AutoCombatService` 调用形状。

### 2. AutoCombatService

- 源路径：`src/main/java/com/bot/dhxy/service/AutoCombatService.java`
- Cloud 目标：不存在同名 `AutoCombatService.java`。
- 基线 diff：有。当前 dirty 只改动战后队长首轮补给报告方法及其调用语义，11 行新增、9 行删除；不能把当前 dirty 方法名/分支当作 `0114604e` 业务依据。
- 构造依赖：`GameContext`、`BattleRadarService`、`AutoCombatPanelService`、`PlayerStateService`、`UICleanerService`、`TaskMaintenanceService`、`LeftTopStatusSwitchService`、`CommonBoxService`、`BotProperties`、`WindowTaskContextHolder`、`TaskTurnCoordinator`。
- 直接调用的本地能力：本类没有直接 `InputProvider`、`InputSequences`、截图或模板调用；通过上述 Service 协作者取得战斗观察、面板、玩家状态、UI 清理、维护、左上状态和盒子结果，并通过 `TaskTurnCoordinator` 执行任务轮次边界。
- 分类：`DIRECT_COPY`。本类是业务编排层，物理动作不在本类内。
- 实现时最小替换点：以 `git show 0114604e:<path>` 恢复/复制方法与分支作为唯一业务来源；只将构造依赖绑定到 Cloud 对应协作者。保留所有 Service-to-Service 调用、返回值、checkpoint、战斗进入/退出、战后维护和面板刷新顺序；当前 dirty 的 `reportXiuluoLeaderFirstAidAfterVerifiedReturn` 不纳入基线复制。

### 3. BagService

- 源路径：`src/main/java/com/bot/dhxy/service/BagService.java`
- Cloud 目标：不存在同名 `BagService.java`。已有支撑：`src/main/java/com/bot/dhxy/service/bag/BagWorkflowState.java` 及同目录的 Bag 状态支撑类型。
- 基线 diff：有。当前 dirty 新增 134 行，包含 `ReturnItemPrescanSnapshots`、捕获后异步匹配等 API；`ReturnItemPrescanService` 当前已经调用这些新增方法，因此不能视为 `0114604e` 的既有行为。
- 构造依赖：`InputSequences`、`InputProvider`、`GameClientTracker`、`CoordinateHelper`、`WindowScopedTempPath`、`WindowTaskContextHolder`。
- 直接调用的本地能力：独占输入段；Alt+E 开关包裹；鼠标移开/点击/右键；`TaskSleep` 停止感知等待；窗口截图到内存/文件；`ImageFinder` 模板匹配与多点扫描；`CoordinateHelper` 缩放、锚点、随机点击点；窗口隔离临时路径；包裹页签/格子几何计算。
- 分类：`COPY_WITH_SHARED_PORT`。业务方法可以保持在 Cloud，包裹 UI 观察和物理输入都必须落到现有共享能力边界。
- 实现时最小替换点：以 `0114604e` 的 `findItemPageIndex`、`withMainBagOpen`、`findAndSelectItem`、`findAndUseItem`、`findAndUseItemFromBack`、`MainBagSession` 为准；只替换 `submitExclusiveAndWait` 内的 Alt+E、移鼠标、页签点击、物品左/右键点击，以及 `captureToFile/captureToMemory + ImageFinder`。保留扫描页顺序、锚点/缩放公式、随机化、关闭包裹和返回值；当前新增异步 prescan API 不纳入本次直接复制基线。

### 4. BattleRadarService

- 源路径：`src/main/java/com/bot/dhxy/service/BattleRadarService.java`
- Cloud 目标：不存在同名 `BattleRadarService.java`。
- 基线 diff：无；与 `0114604e` 相同。
- 构造依赖：`GameClientTracker`、`CoordinateHelper`、`GameContext`、`WindowScopedTempPath`、`WindowTaskContextHolder`、`MiniMapCoordinateReader`、`TeamTaskProperties`。
- 直接调用的本地能力：战斗区域/顶部区域/头像区域截图；窗口缩放矩形；`ImageFinder` 模板判断；`MiniMapCoordinateReader`；窗口绑定状态；expected 战斗快脱战头像快照与差分。
- 分类：`LOCAL_RESIDENT`。这是持续战斗观察与状态纠正能力，不能把窗口截图、战斗探针和观察节奏直接复制为 Cloud Service。
- 实现时最小替换点：无 Service 复制替换点；保留本地 `checkAndSyncCombatState`、fast expected exit、battle enter/exit 信号和 `GameContext` 写入。Cloud 只接收现有结果，不在本类外重做截图、模板判定或观察顺序。

### 5. CommonBoxService

- 源路径：`src/main/java/com/bot/dhxy/service/CommonBoxService.java`
- Cloud 目标：不存在同名 `CommonBoxService.java`。
- 基线 diff：无；与 `0114604e` 相同。
- 构造依赖：`BotProperties`、`GameClientTracker`、`InputSequences`、`WindowTaskContextHolder`。
- 直接调用的本地能力：绑定窗口/角色上下文；固定 ROI 内存截图；`ImageFinder.find(...)` 模板命中；`InputSequences.moveAndClickLeft(...)` 原子移动点击；现有 pending 盒子记录与消费清除。
- 分类：`COPY_WITH_SHARED_PORT`。检测与消费是业务 Service，但截图、模板匹配和移动点击是共享端口替换面。
- 实现时最小替换点：保留 `detectLeaderBoxAfterReturnHome`、`detectMemberBoxAfterCombatExit`、`consumePendingBoxIfAllowed`、`hasPendingBoxForCurrentWindow`、`clearPendingForRole` 的调用关系和消费时机；仅替换 ROI capture、模板匹配和 `moveAndClickLeft`，不重排检测/消费顺序。

### 6. DialogService

- 源路径：`src/main/java/com/bot/dhxy/service/DialogService.java`
- Cloud 目标：不存在同名 `DialogService.java`。已有对话模型、策略和 `DialogPolicyCloudDecisionService`，但没有同名编排类。
- 基线 diff：无；与 `0114604e` 相同。
- 构造依赖：`InputSequences`、`InputProvider`、`GameClientTracker`、`CoordinateHelper`、`GiveItemService`、`WindowScopedTempPath`、`WindowTaskContextHolder`、`ObjectiveTextRecognitionService`、`ObjectProvider<SmartClickEvidenceConfirmationService>`、`DialogPolicyCloudDecisionService`、`ImageProcessorService`。
- 直接调用的本地能力：对话区域截图；`ImageFinder` 模板匹配；`ObjectiveTextRecognitionService` 目标文本识别；图像预处理与窗口临时路径；Alt+4 隐藏角色名；多种 move/click；独占输入回调中的直接鼠标动作；对话后截图验证；故事目标区域截图。
- 分类：`LOCAL_RESIDENT`。本类同时提供本地无焦点对话观察、模板/文本验证和输入后验证，不能把这些像素观察中间点拆成 Cloud 普通输入包。
- 实现时最小替换点：不复制本地 `detectDialogTypeNoFocus`、`detectDialogSnapshotNoFocus`、`captureCurrentStoryImage`、`validatePreparedDialogActionForConsume` 的观察实现；保留 `handleDialog` 与 `GiveItemService` 调用关系。需要共享端口时只替换已有独立输入边界：Alt+4 `[pressAlt4, sleep]`、已确认 click 的现有 atomic move+click；保留每个 click 前后当前截图/验证时点。

### 7. GiveItemService

- 源路径：`src/main/java/com/bot/dhxy/service/GiveItemService.java`
- Cloud 目标：不存在同名 `GiveItemService.java`。
- 基线 diff：无；与 `0114604e` 相同。
- 构造依赖：`InputSequences`、`InputProvider`、`CoordinateHelper`、`BagService`。
- 直接调用的本地能力：委托 `BagService.findAndSelectItem(...)`；按钮模板定位与随机点击点；普通路径 `[clickLeft, sleep(1000)]`；已在独占输入段内的直接点击路径；停止感知等待。
- 分类：`COPY_WITH_SHARED_PORT`。业务顺序短且输入边界清楚，但按钮定位仍依赖本地视觉能力。
- 实现时最小替换点：保留 `executeGive` 对 `BagService` 的调用、800ms 等待、按钮未找到分支和返回值；仅替换 `clickGiveButton` 的现有单 bundle，以及 `clickGiveButtonDirectForExclusive` 的既有独占段物理点击。Service 之间调用保持原封不动。

### 8. LeftTopStatusSwitchService

- 源路径：`src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java`
- Cloud 目标：不存在同名 `LeftTopStatusSwitchService.java`。已有 `src/main/java/com/bot/dhxy/service/LeftTopStatusDecision.java` 和 `src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudLeftTopTemplateMatcher.java`。
- 基线 diff：无；与 `0114604e` 相同。
- 构造依赖：`GameClientTracker`、`CoordinateHelper`、`WindowScopedTempPath`、`WindowTaskContextHolder`、`InputSequences`。
- 直接调用的本地能力：按窗口缩放计算 ROI；窗口截图到临时文件；OpenCV `Imgcodecs/Imgproc/Core` 双模板评分；窗口基点换算；`InputSequences.moveAndClickLeft(...)`。
- 分类：`COPY_WITH_SHARED_PORT`。开关状态判定可对应已有 Cloud 支撑，点击仍需共享输入边界。
- 实现时最小替换点：保留 `resolveState` 的状态/阈值判断和 `handle*` 的 task-code 门；仅替换 `tracker.captureToFile + OpenCV` 的输入来源/匹配调用，以及 OPEN 状态下的原子 move+click。闭合、未知、捕获失败均继续保持不点击。

### 9. NavigationService

- 源路径：`src/main/java/com/bot/dhxy/service/NavigationService.java`
- Cloud 目标：不存在同名 `NavigationService.java`。已有 `NavigationRoutePlanCloudDecisionService`、`NavigationPointCloudDecisionService`、`RouteCloudDecisionService`、`NavigationRoutePlanResolver` 和 `CloudNavigationProperties` 等支撑。
- 基线 diff：有。当前 dirty 删除 318 行旧的 local ladder/world-map attempt 方法；即使这些方法带 `Deprecated` 标记，也不能把当前删除结果当作 `0114604e` 的直接复制源。
- 构造依赖：`BotProperties`、`GameContext`、`GameClientTracker`、`InputProvider`、`InputSequences`、`MiniMapCoordinateReader`、`GameStateUtil`、`CoordinateHelper`、`UICleanerService`、`DialogService`、`NpcClickService`、`WindowScopedTempPath`、`PlayerStateService`、`BattleRadarService`、`WindowTaskContextHolder`、`TaskExecutionContextHolder`、`BoundWindowKeyboardService`、`WindowReadyEventBus`、`MapNameCanonicalizer`、`MemoryService`、`RuntimeDecisionShadowService`、`RouteCloudDecisionService`、`NavigationPointCloudDecisionService`、`NavigationRoutePlanCloudDecisionService`。
- 直接调用的本地能力：窗口绑定与基点；小地图/世界地图截图、模板匹配、坐标换算和 OCR/目标解析；Alt+1/Alt+2；Ctrl+A、文本输入、Enter；滚动；地图结果/路线对话/小地图的原子输入段；pathing/dialog/window-ready 观察；`UICleanerService`；本地导航结果与 arrived/pathing 观察。
- 分类：`LOCAL_RESIDENT`。导航业务决策已有 Cloud 支撑，但窗口路径观察、地图像素、坐标和连续移动确认仍是本地能力。
- 实现时最小替换点：不移动本地 pathing、dialog、movement、arrival 观察；保留 `navigateToNPC`、`navigateToMap`、`navigateInCurrentMap` 的三个 public API 与所有 Service 调用。只在现有物理边界替换：地图打开/搜索/输入/结果点击、route-dialog click、小地图打开/点击/关闭、清理关闭动作；每个当前 `submitExclusiveAndWait` callback 保持同一连续输入段，不能把中间已有的 capture/匹配/确认顺序改写。

### 10. NpcClickService

- 源路径：`src/main/java/com/bot/dhxy/service/NpcClickService.java`
- Cloud 目标：不存在同名 `NpcClickService.java`。已有 `NpcClickSmartCloudDecisionService`、`NpcClickSmartCloudRequest`、`NpcClickSmartCloudSession`，以及 `service/npc` 下的本地扫描/验证类型。
- 基线 diff：有。当前 dirty 删除了目标模板、黄色模板、glyph metadata 的构造与发送逻辑（2 行新增、105 行删除）；这些请求输入变化属于当前 dirty，不作为 `0114604e` 基线复制依据。
- 构造依赖：`InputSequences`、`InputProvider`、`GameClientTracker`、`GameStateUtil`、`BattleRadarService`、`DialogService`、`WindowScopedTempPath`、`WindowTaskContextHolder`、`TaskExecutionContextHolder`、`ImageProcessorService`、`NpcClickSmartCloudDecisionService`、`WindowReadyEventBus`。
- 直接调用的本地能力：绑定窗口截图与坐标基点；Alt+4 场景准备；普通候选的 atomic `[moveMouse, sleep(150), clickLeft(hold=150), sleep(1500)]`；Ctrl 独占段内 `holdCtrl → sleep(80) → moveMouse → sleep(280) → click/releaseCtrl`；菜单区域截图、模板匹配、OCR/关键词验证；Dialog/BattleRadar 验证；Alt+A 直接战斗入口。
- 分类：`LOCAL_RESIDENT`。Cloud 可承接 smart-click 的业务编排，但 Ctrl 按住期间的扫描、菜单匹配和本地点击后验证必须留在本地连续能力中。
- 实现时最小替换点：保留 `clickNpcSmart`、`clickNpcSmartWithOutcome`、`tryDirectCombatTargetClick` 的 public 调用与候选顺序；保留 Ctrl probe 的单一独占边界和全部 delay；只将已有普通候选/direct-combat 的独立输入列表接到共享端口，并保留本地 Ctrl probe、模板/OCR、Dialog/BattleRadar 验证，不把这些步骤拆成新的 Service 调用链。

## 可立即按依赖顺序迁移的 cohort

以下顺序只按这 10 个 Service 的现有构造/调用依赖排列，不改变 Service 之间调用，也不表示 Cloud 目标已经存在：

1. **Cohort A：独立基础面**：`AutoCombatPanelService`、`BagService`、`BattleRadarService`（本地保留）、`CommonBoxService`、`LeftTopStatusSwitchService`。这些类不依赖本表中其它 Service；除本地保留的 `BattleRadarService` 外，其余类的共享截图/输入替换点需分别按上表处理。
2. **Cohort B：包裹交互叶子**：`GiveItemService`，前置为 `BagService`；只处理按钮定位和已有点击边界。
3. **Cohort C：战斗业务编排**：`AutoCombatService`，前置为 `AutoCombatPanelService`、`BattleRadarService`、`CommonBoxService`、`LeftTopStatusSwitchService`，以及其余构造依赖的对应 Cloud 类；复制源必须回到 `0114604e`。
4. **Cohort D：对话本地能力**：`DialogService`，前置为 `GiveItemService`；本地观察/验证保留，Cloud 只接已有策略结果和共享输入边界。
5. **Cohort E：NPC 点击**：`NpcClickService`，前置为 `DialogService`、`BattleRadarService`；Ctrl probe 与本地验证先保持本地连续执行。
6. **Cohort F：导航**：`NavigationService`，前置为 `DialogService`、`NpcClickService`、`BattleRadarService`；地图/小地图输入段和 pathing 观察按现有边界迁移。

结论：当前没有一个 10 类同名 Cloud Service 已经落盘；“可立即”指可以按上述基线、依赖和替换点进入下一迁移波，不指可以启动运行面。报告仅完成直接迁移就绪盘点，未提出或实施额外的业务控制机制。

## Parent Readiness Review #1 - APPROVED WITH OWNERSHIP CORRECTION - 2026-07-13T19:52:00-04:00

父级已复核基线、同名目标存在性、构造依赖和输入/观察边界；作为迁移依赖清单，P0/P1/P2=0。
报告中的 `LOCAL_RESIDENT` 只批准为“机械能力常驻 DHXY”，不批准据此把整个业务 Service 排除出 Cloud：

- `BattleRadarService` 的 continuous watcher、截图、模板和移动/战斗事实留本地；Cloud 业务只消费类型化事实。
- `DialogService`、`NpcClickService`、`NavigationService` 的 public 业务编排和 Service-to-Service 调用仍迁 Cloud；
  输入中夹着 capture/template/OCR 的 callback 才整体收敛为一个本地宏。
- `UICleanerService`、窗口身份/绑定、宿主电源属于用户明确的本地保留能力，不强造同名 Cloud 实现。
- 其余没有输入中途观察的物理序列直接变成一个有序 `InputBundle`；不新增 per-Service owner/permit/ledger。

因此本报告用于排依赖和定位替换点，不作为“整类留本地”的最终所有权表。下一步直接落共享输入兼容层和
首个 Service cohort，不再追加 Design #N。**无已批准业务差异；按 `0114604e` 基线等价迁移。**
