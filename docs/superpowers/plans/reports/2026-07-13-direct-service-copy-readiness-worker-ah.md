# 直接 Service 迁移就绪盘点

**角色：** Internal Worker AH  
**盘点范围：** DHXY `D:\mavenProject\DHXY`、Cloud `D:\mavenProject\dhxy-cloud-brain`  
**业务基线：** DHXY commit `0114604e1ff5f15491d2910959c45252e893d04f`  
**当前工作区：** DHXY 分支 `thin-client-design`；Cloud 分支 `navigation-migration`。两边均存在既有 dirty/untracked，本报告未回滚、覆盖、清理或提交。未运行 Maven，未启动应用、Task、UI 或输入运行面。

## 判定口径

- `DIRECT_COPY`：类本身没有窗口/截图/模板/OCR/物理输入；可按基线复制，但仍须满足其直接 Service 依赖的 Cloud 闭包。
- `COPY_WITH_SHARED_PORT`：仅指明确的物理动作点可替换为现有共享 `executeInputBundle(...)`；类内仍有本地观察时，不把整类误判为可复制。
- `LOCAL_RESIDENT`：整类依赖绑定窗口、截图/模板/OCR、宿主能力，或动作与本地观察交织；整类不直接复制到 Cloud。

Cloud 中按同名路径检查的目标均不存在：`src/main/java/com/bot/dhxy/service/<Class>.java`。下文的“目标不存在”均指该精确 Service 类，不否认 Cloud 已有相邻的 DTO、Decision 或机械端口。

## 逐类盘点

### 1. ClientIdentityService

- **源路径：** `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\ClientIdentityService.java`
- **Cloud 目标：** 不存在：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\ClientIdentityService.java`
- **相对 `0114604e` 的业务 diff：** 无。源文件在基线存在，当前未见该文件 diff。
- **构造依赖：** `GameClientTracker`、`WindowTaskContextHolder`、`WindowNativeBindingRefreshService`。
- **直接调用的本地能力：** 刷新当前绑定窗口标题；读取 `WindowRuntimeContext.nativeBinding.title`；tracker 标题缓存和 `locateWindow()` 回退；`WindowTitleIdentityParser` 解析身份。无输入、截图或 OCR。
- **分类：** `LOCAL_RESIDENT`。身份读取权威绑定在本地窗口标题。
- **实现时最小替换点：** 无。保留 `scanAndSyncIdentity(...)` 与 `resolveCurrentWindowTitle()`，调用方保持原有 `identityService.scanAndSyncIdentity(me)` 不变。

### 2. PlayerStateService

- **源路径：** `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\PlayerStateService.java`
- **Cloud 目标：** 不存在：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\PlayerStateService.java`
- **相对 `0114604e` 的业务 diff：** 有。当前 dirty diff 把 no-focus 血法截图移到 `probeFirstAidSupplyNoFocus(...)` 外层，并新增 `probeFirstAidSupplyFromBars(...)`；原基线先检查停止/本轮检查次数/窗口基点，再截图。当前 diff 还改变了该读取与停止检查的先后。迁移必须从 `0114604e` 源码取，不采用当前 dirty 版本。
- **构造依赖：** `GameContext`、`ClientIdentityService`、`LocationVisionService`、`GameClientTracker`、`InputProvider`、`InputSequences`、`CoordinateHelper`、`BagService`、`WindowTaskContextHolder`、`BotProperties`、`WindowScopedTempPath`、`SheyaoxiangStatusCloudDecisionService`。
- **直接调用的本地能力：** 绑定身份同步；位置 OCR；`GameClientTracker` 血法条截图；本地像素阈值/采样与确认截图；`InputProvider`/`InputSequences` 补给点击和鼠标移开；`BagService` 开包/用摄妖香；窗口基点与 scoped 临时图；`TaskCheckpoint`/`TaskSleep`。
- **分类：** `LOCAL_RESIDENT`。血法判定、no-focus 读取、确认读取和物理补给在同一 Service 中交织。
- **实现时最小替换点：** 不复制整类。保留 `syncMyIdentity()`、`syncMyPosition()`、`probeFirstAidSupplyNoFocus(...)`、`healIfUnhealthy(...)` 等本地观察；仅将现有 `performCachedFirstAidPlanNow(...)`、`healAll(...)`、`healIfUnhealthy(...)` 中已经形成的输入序列作为后续 `COPY_WITH_SHARED_PORT` 候选。`identityService`、`bagService`、`sheyaoxiangStatusCloudDecisionService` 调用保持原样。

### 3. QuestManagerService

- **源路径：** `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\QuestManagerService.java`
- **Cloud 目标：** 不存在：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\QuestManagerService.java`
- **相对 `0114604e` 的业务 diff：** 无。当前未见该文件相对基线 diff。
- **构造依赖：** `InputSequences`、`InputProvider`、`WindowAwareInputCoordinator`、`GameClientTracker`、`CoordinateHelper`、`GameContext`、`WindowScopedTempPath`。
- **直接调用的本地能力：** 任务面板 anchor/标签模板匹配；`isTextGlowing(...)` 的本地像素读取；详情 ROI 截图与 scoped 文件落盘；`Alt+Q`、tab 点击、普通点击、滚动、关面板输入；`TaskSleep`/`InputActionScope`。
- **分类：** `LOCAL_RESIDENT`；类内输入点可单独按 `COPY_WITH_SHARED_PORT` 处理，但整类包含观察与输入交织。
- **实现时最小替换点：** 保留 `activateTaskIfPresentDirect(...)` 和 `captureCurrentQuestDetailForTaskDirect(...)` 的本地观察/模板/截图顺序；输入替换候选仅是 `ensurePanel(...)` 的 `pressAltQ`、`selectCurrentTaskTab(...)` 的点击、`click(...)`、`scroll(...)`、`closePanel(...)` 各自现有 action list。不要把 `captureCurrentQuestDetailForTask(...)` 内的本地截图和业务分支复制成 Cloud 输入 bundle。

### 4. ReturnItemPrescanService

- **源路径：** `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\ReturnItemPrescanService.java`
- **Cloud 目标：** 不存在：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\ReturnItemPrescanService.java`；Cloud 已有相邻 `ReturnItemPrescanDecision`，但不是同名 Service。
- **相对 `0114604e` 的业务 diff：** 有。当前 dirty diff 新增 `SKIP` 抽签候选，并把 `MAIN_BAG_TASK_PAGE` 改为先抓快照、再 `CompletableFuture` 异步匹配；基线直接调用 `bagService.prescanMainBagTaskPageItem(...)`。两处都会改变策略选择或时序，迁移必须取 `0114604e` 版本。
- **构造依赖：** `BagService`；内部使用按任务/窗口/运行/轮次/模板组织的缓存数据。
- **直接调用的本地能力：** 本类无直接 `InputSequences`/截图调用；所有本地能力通过 `BagService.useCachedMainBagReturnItem(...)`、`captureMainBagTaskPagePrescanSnapshots(...)`、`matchMainBagTaskPagePrescanSnapshots(...)`、`prescanMainBagItemFromBack(...)` 间接完成。
- **分类：** `DIRECT_COPY` 候选，但当前未就绪，原因是 Cloud 没有 `BagService` 同名依赖闭包；不能只复制本类后假定可编译。
- **实现时最小替换点：** 从 `0114604e` 原样复制入口和 `bagService.*` 调用；不要在本类改策略、时机、缓存成功/失败顺序。若 BagService 的 Cloud 目标已先就绪，本类自身没有物理 action 需要替换；Service 间调用保持原封不动。

### 5. SummonSkillService

- **源路径：** `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\SummonSkillService.java`
- **Cloud 目标：** 不存在：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\SummonSkillService.java`；Cloud 已有 `SummonSkillCloudDecision`/`SummonSkillCloudRequest` 等相邻类型。
- **相对 `0114604e` 的业务 diff：** 无。当前未见该文件相对基线 diff。
- **构造依赖：** `GameClientTracker`、`CoordinateHelper`、`InputSequences`、`InputProvider`、`UICleanerService`、`DialogService`、`ObjectProvider<TaskMaintenanceService>`、`WindowScopedTempPath`、`ImageProcessorService`、`SummonSkillCloudDecisionService`、`WindowTaskContextHolder`。
- **直接调用的本地能力：** `Alt+O` 开技能面板；面板拖拽/技能页点击；槽位截图、模板匹配、颜色距离分类；hover 后 tooltip 截图/洗字；删除与确认点击；`UICleanerService`、`DialogService` 清理；`TaskSleep`/输入 worker 直连规则；scoped 临时图。
- **分类：** `LOCAL_RESIDENT`。大量动作前后必须读取本地像素/模板，不能整类直接复制。
- **实现时最小替换点：** 保留 `cleanTailNormalSkillsDirect(...)`、`maybeClickUltimateCorner(...)`、`inspectSkillSlotDirect(...)`、`captureAndWashYellowTipOnce(...)` 及其现有判断顺序。仅 `openSummonSkillPanel(...)`、`dragPanelIfNeeded(...)`、`deleteSkillAtSlot(...)` 内已经闭合的动作序列可作为后续 `COPY_WITH_SHARED_PORT` 候选；`uiCleanerService`、`dialogService`、`taskMaintenanceServiceProvider` 调用不改。

### 6. SystemPowerService

- **源路径：** `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\SystemPowerService.java`
- **Cloud 目标：** 不存在：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\SystemPowerService.java`
- **相对 `0114604e` 的业务 diff：** 无。当前未见该文件相对基线 diff。
- **构造依赖：** 无构造依赖。
- **直接调用的本地能力：** `ProcessBuilder` 启动 Windows `rundll32.exe powrprof.dll,SetSuspendState`。
- **分类：** `LOCAL_RESIDENT`。这是宿主电源执行器，不进入 Cloud Service 复制波次。
- **实现时最小替换点：** 无；保留 `sleepComputer(...)` 原实现和显式任务调用关系。

### 7. TaskMaintenanceService

- **源路径：** `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\TaskMaintenanceService.java`
- **Cloud 目标：** 不存在：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\TaskMaintenanceService.java`；Cloud 已有维护阈值/能力门相关 Decision 类型，但不是同名 Service。
- **相对 `0114604e` 的业务 diff：** 无。当前未见该文件相对基线 diff。
- **构造依赖：** `BotProperties`、`GameContext`、`DialogService`、`SummonSkillService`、`WindowTaskContextHolder`、`CoordinateHelper`、`InputSequences`、`RuntimeDecisionShadowService`、`CapabilityGateCloudDecisionService`、`MaintenanceThresholdCloudDecisionService`；另有可选字段注入 `WindowReadyEventBus`。
- **直接调用的本地能力：** `TaskExecutionContext`/窗口上下文读取；`CoordinateHelper.findImageInRegion(...)` 维护广播 ROI 模板匹配；`InputSequences.moveAndClickLeft(...)`；窗口 ready-event 发布；队伍维护、召唤技能、对话相关的本地 Service 调用。
- **分类：** `LOCAL_RESIDENT`；只有 `handleMaintenanceBroadcast(...)` 的最终 `moveAndClickLeft(...)` 是 `COPY_WITH_SHARED_PORT` 候选。
- **实现时最小替换点：** 不复制整类。保持 `runOpportunisticMaintenance(...)` 的优先顺序和 `dialogService`/`summonSkillService` 调用不变；只记录 `handleMaintenanceBroadcast(...)` 的两个模板动作与现有等待值，后续替换该单个输入调用。

### 8. TaskTrackerPanelService

- **源路径：** `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\TaskTrackerPanelService.java`
- **Cloud 目标：** 不存在：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\TaskTrackerPanelService.java`；Cloud 已有 `TrackerPanelReaderCloudDecisionService` 及 tracker model，但不是同名 Service。
- **相对 `0114604e` 的业务 diff：** 无。当前未见该文件相对基线 diff。
- **构造依赖：** `GameClientTracker`、`CoordinateHelper`、`WindowScopedTempPath`、`InputSequences`、`MapNameCanonicalizer`、`ImageProcessorService`、`TaskClassifierCloudShadowService`、`TrackerPanelReaderCloudDecisionService`、`WindowTaskContextHolder`。
- **直接调用的本地能力：** tracker 面板截图；anchor/标题模板匹配；ROI 裁剪、黄/绿字处理、指纹计算、缓存命中；本地文件读写与标记图；仅 `dragTrackerPanelIfNeeded(...)` 发送拖拽输入。
- **分类：** `LOCAL_RESIDENT`。主体是本地观察/裁剪/坐标产生器；拖拽动作单独是 `COPY_WITH_SHARED_PORT` 候选。
- **实现时最小替换点：** 保留 `readWuhuanTrackerTitle(...)`、`readWubeiTrackerPanel(...)`、`readXiuluoTrackerPanel(...)` 的截图、裁剪、模板与指纹顺序；仅替换 `dragTrackerPanelIfNeeded(...)` 的现有 `dragAndDrop + sleep(500)` action list。`taskClassifierCloudShadowService` 与 `trackerPanelReaderCloudDecisionService` 调用保持原样。

### 9. TeamReturnService

- **源路径：** `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\TeamReturnService.java`
- **Cloud 目标：** 不存在：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\TeamReturnService.java`；Cloud 已有 `TeamReturnPolicyCloudDecision`，但不是同名 Service。
- **相对 `0114604e` 的业务 diff：** 无。当前未见该文件相对基线 diff。
- **构造依赖：** `CoordinateHelper`、`InputSequences`、`BotProperties`、`PlayerStateService`、`GameClientTracker`（`@Lazy`）。
- **直接调用的本地能力：** 归队按钮/队长信号区域截图；`ImageFinder` 模板匹配；队员最终点击输入；`TaskSleep` 轮询等待；`CompletableFuture` 异步预分析；窗口标题/路径状态读取。
- **分类：** `LOCAL_RESIDENT`。截图确认、队长等待和队员点击前复查交织在同一 Service；最终点击本身是 `COPY_WITH_SHARED_PORT` 候选。
- **实现时最小替换点：** 保留 `probeMemberReturnMarker(...)`、`beginLeaderSignalPrecheck(...)`、`consumeLeaderSignalPrecheck(...)` 和 `waitForMembersReturnIfNeeded(...)` 的本地观察/等待顺序；仅记录 `clickReturnTeamIfPresent(...)` 中现有 `clickLeft + sleep(500)`。`playerStateService.ensureSheYaoXiangActive(...)` 调用保持原样。

### 10. UICleanerService

- **源路径：** `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\UICleanerService.java`
- **Cloud 目标：** 不存在：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\UICleanerService.java`
- **相对 `0114604e` 的业务 diff：** 无。当前未见该文件相对基线 diff。
- **构造依赖：** `InputSequences`、`InputProvider`、`GameClientTracker`、`CoordinateHelper`、`GameStateUtil`、`DialogService`、`WindowScopedTempPath`、`GameContext`、`WindowTaskContextHolder`。
- **直接调用的本地能力：** 当前帧缓存/截图；关闭窗口模板匹配与图像预处理；`DialogService.handleDialog(...)` 的 inspect/story/fallback；`Alt+1`、X 按钮、X2-only 关闭及随机化点击；`TaskSleep`/`InputActionScope`。
- **分类：** `LOCAL_RESIDENT`。它是本地 UI 观察与清理执行器；其中单个关闭动作才是 `COPY_WITH_SHARED_PORT` 候选。
- **实现时最小替换点：** 保留 `cleanUpAll(...)`、`cleanLightweightInterruptions(...)`、`forceCloseDialog(...)` 的判断与模板顺序；仅记录 `closeMapWindow(...)`、`clickCloseButtonOnce(...)`、`closeMapSearchInputByX2Direct(...)`、`clickAbsolutePoint(...)` 的现有动作序列。`dialogService.handleDialog(...)` 调用保持原样。

## 可立即按依赖顺序迁移的 cohort

按当前静态证据，十个同名 Cloud Service 均不存在，因此没有一个可以在不补直接依赖闭包的情况下“复制后立即编译”。可立即排队的具体顺序如下：

1. **Cohort 0：本地保留基座。** `ClientIdentityService`、`SystemPowerService`、`TaskTrackerPanelService`、`UICleanerService` 保持 DHXY 本地；它们提供窗口标题、宿主电源、截图/模板/清理等本地能力，不创建同名 Cloud Service。
2. **Cohort 1：首个 `DIRECT_COPY` 候选。** `ReturnItemPrescanService`。前置是 Cloud 侧先具备它直接调用的 `BagService` 方法闭包；迁移源必须取 `0114604e`，并保留所有 `bagService.*` 调用。
3. **Cohort 2：局部输入 bundle 候选。** `QuestManagerService`、`TaskTrackerPanelService`、`TeamReturnService`。先保留各自截图/模板/等待逻辑，只处理报告中点名的闭合输入序列；`TeamReturnService` 依赖 `PlayerStateService` 的原调用关系，不能提前改调用形状。
4. **Cohort 3：玩家状态后再处理队伍归队。** `PlayerStateService` 先以 `0114604e` 为源盘点；完成本地观察与补给动作边界后，再接 `TeamReturnService` 的最终点击 bundle。当前 PlayerState dirty diff 不得带入迁移。
5. **Cohort 4：互相直接调用的一组。** `SummonSkillService` 与 `TaskMaintenanceService` 放在同一依赖波次；前者保留本地槽位观察/删除动作，后者只处理已确认的维护广播输入点。两者之间现有 Service 调用必须原封不动。

**结论：** 当前最接近直接复制的是基线版 `ReturnItemPrescanService`，但它受 `BagService` Cloud 依赖闭包阻塞；其余九类均应按本报告标注的本地能力边界处理，不能按同名类整类复制。

## Parent Readiness Review #1 - APPROVED WITH OWNERSHIP CORRECTION - 2026-07-13T19:52:00-04:00

父级已复核十类的基线 diff、依赖闭包和本地能力证据；作为直接迁移就绪清单，P0/P1/P2=0。
最终所有权按用户已确认的简化路线解释：

- `ClientIdentityService` 的窗口身份、`SystemPowerService` 的宿主电源、`UICleanerService`、continuous watcher
  与截图/模板/OCR 实现永久留 DHXY。
- `PlayerStateService`、`QuestManagerService`、`SummonSkillService`、`TaskMaintenanceService`、
  `TaskTrackerPanelService`、`TeamReturnService` 的业务调用形状仍迁 Cloud；报告中的 `LOCAL_RESIDENT`
  表示其机械观察方法留本地并通过类型化事实/宏调用，不表示放弃 Cloud 业务 facade。
- 只有“按键按下/释放之间仍需本地 capture/template/OCR”或同一 exclusive callback 内不可拆的流程才是
  `LOCAL_MACRO`；普通“先取本地事实，再发一个固定点击束”继续由 Cloud 按基线顺序编排。

不再为这些 Service 引入专属 owner/permit/ledger/TTL/retry。下一步是直接实现共享输入兼容层及首个
Service cohort。**无已批准业务差异；按 `0114604e` 基线等价迁移。**
