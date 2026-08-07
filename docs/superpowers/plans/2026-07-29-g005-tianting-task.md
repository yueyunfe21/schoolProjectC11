# G005 天庭任务(外部 worker 实施卡)

> **设计权威**:`docs/superpowers/specs/2026-07-28-tianting-task-design.md`(**必须先全文通读**,含决议表 Q1-Q13 / D1-D4、五张时序、phase 结构)。
> **业务权威**:`docs/天庭任务流程大MD.md`(设计与它不一致处,以设计 §12 决议表为准——已有多处用户明示覆盖)。
> 本卡只讲"怎么落地":Work Package、写集、DoD、验收。业务语义一律不在本卡重述,查设计。

## 0. 新工人须知(必读)

- **先读 `docs/云端迁移常见错误清单.md`**(十类反复踩的事实链断裂 + 提交前自查清单)。
- 仓库:客户端 `D:\mavenProject\DHXY-cr271`(捕获/输入/本地感知),云端 `D:\mavenProject\dhxy-cloud-brain`(业务决策)。`D:\mavenProject\DHXY` 是用户 IntelliJ 基线,**严禁改动**。
- 共享协议文件 `com/bot/dhxy/cloud/turn/protocol/**`(含 observation)**双仓 byte-identical**,改一处必须同步另一处。
- 编译:client `mvn -q compile`;cloud `mvn -q compile -DskipTests=false`(enforcer 要求该参数)。全树 testCompile 可能被写集外脏测试阻断 → 用 `junit-platform-console-standalone` 隔离编译运行本卡新增测试类(项目惯用技法,见 [[cr271-isolate-run-named-test-technique]])。
- **等待契约铁律**:park 只等事件;每个可能结局必有唤醒事件;一律有界(≤25s 循环再挂),禁止裸挂。
- **零新增独立截图**:所有观察走 G002 共享周期帧;turn 内显式捕获(Alt+U 状态条、Alt+4 整窗、面板帧)除外。
- 同构参考实现:`XiuluoTaskV2`(组队形态权威)、`WubeiTask`(队员归队/补给样板)、`XinshouTask`(最新最瘦骨架 + 事实驱动循环)、`WildBattleTask`(走位输入包 + 进战释放 turn)。

## 1. 不可偏离的架构决议(设计 §12 决议表摘要,细节查设计)

1. **必须组队**(D2):队长 `TIANTING` + 队员 `AUTO_BATTLE`,**无单窗口模式**,启动预检要求队伍(同修罗)。
2. **dialog 七张全部本地匹配**(修罗看打探针范式);仅本地全 miss 才发云端,云端固定返回第一行绿链。
3. **tracker 面板一趟出全部**(D3):title → task box → 绿链 → `anlei` 暗雷标记,全部在云端同一次面板分析内完成。
4. **常驻 dialog 探针四张**:`kaida`(必须第一)/`duoxie`/`zhuoyue`/`yaowang`;**`fengyao` 是条件模板**,仅 `duoxie` 点击后 ~1s 窗口内匹配,不入常驻集。
5. **暗雷怪路径无 dialog**(D4):点绿链仅导航,到达后靠自己(飞行检测 → 走位巡逻);常驻探针无 dialog 自然不命中,**不做任何互斥/注销机制**。
6. **Alt+A 直战只允许封妖符小任务**;整条点击链只允许出现一次 Alt+A(CR267 已把 Alt+A 从 clickNpcSmart 候选剥离,验收要断言)。
7. **摄妖香保持修罗/五倍原逻辑**(Q6):与暗雷怪不冲突。
8. **封妖符坐标点击必须携带 pathing intent**(source `tianting:fengyao-coord:<n>`),禁止裸点屏幕坐标后无事实可等。

## 2. Phase 结构(设计 §10)

### 2026-08-01 已决修复：寻路事实与回城物品

- 绿链或坐标点击的 Cloud turn **完成**后，Client 只登记同一 `intentId` 的 `WindowPathingIntent`；不得再运行一次性边缘像素/坐标条 proof 来决定是否登记。`WindowObservationRunner/WindowObservationSampler` 是唯一的移动、到达、停下与进战事实来源，Cloud 只消费其事实。
- 天庭是多小任务一轮，禁止在战斗中、寻路中或绿链点击后预扫/缓存回城道具。仅在 `RETURN_HOME` 最终阶段发起新鲜的本地 `FIND_AND_USE_TASK_PAGE` 匹配/使用；未回城时仍按既有清 UI、有限重试和步行回天宫兜底。修罗的预扫策略不迁入天庭。

```
PREPARE → ACCEPT_TASK → RUN_SUBTASKS → RETURN_HOME ─┐(回城后)
              ▲──────────────────────────────────────┘
   终态:FINISHED / FAILED / STOPPED
```

- `ACCEPT_TASK` / `RETURN_HOME` 是纯顺序过程,用 phase 承载重试预算、loop guard、`forceRelease` 兜底(修罗式)。
- **`RUN_SUBTASKS` 内部不切 phase**:采用新手式事实驱动优先级循环——
  `本地 dialog 命中 > 战斗 park > 引妖(战后) > pathing park > tracker 绿链 > STOPPED_AWAY 重点`,
  每次唤醒重看事实分流;**云端不得再维护一份"第几个小任务"的内存账**。
- 小任务内局部游标(四坐标已点集合、封妖符标记、循环前快照、多谢延迟窗口)放 `TiantingRoundContext` 字段,面板推进即作废。

## 3. Work Packages(按序,各自 DoD)

### WP1 注册骨架 + 组队预检

- **协议(双仓 byte-identical)**:`TurnTaskCode.TIANTING`;`TaskType`(cloud:code+显示名"天庭";client:+`singlePlayer=false`)。
- cloud:`CloudTurnTaskFactory` descriptor+provider、`CloudTurnRuntimeConfiguration` bean、`TurnProtocolValidator` 放行;新建 `task/tianting/` 包:`TiantingTask`(prototype `GameTask`,空主循环)+`TiantingPhase`+`TiantingRoundContext`+`TiantingStepOutcome`+`TiantingDialogCatalog`。
- cloud 分支接线:`TaskStartupCheckService`(**组队预检,无队伍即 blocked**)、`TaskTeamAssignmentPolicy`、`LeftTopStatusSwitchService.isSupportedTaskCode`、`CloudTaskStartupPreparationService:332`、`AutoCombatService:558` 队伍急救 gate 加 tianting。
- client:`WindowTaskControlService.toTurnTaskCode` switch、`MainWindowController` 任务入口 + 轮数字段、`BotProperties`、`GameUiSettingsStore`、`WindowRegistrationBatchBuilder` 队长/队员分派。
- **DoD**:双仓编译过;协议文件 diff 零差异;TIANTING 可从 UI 选中启动空转并正常停止;无队伍时启动被预检拦下。

### WP2 tracker 面板分析扩展(云端一趟出全部)

- cloud 纯算法 `com/yueyunfe/dhxy/cloudbrain/TaskTrackerPanelService.java`:
  - 新 title 模板常量 `tianting_title.png`(阈值 0.82,对照 `panel_title_yellow.png` 范式)+ `analyzePanel` 分支;
  - `detailHeight("tianting")` = **五倍的 block 高度取值**(Q7);
  - 白名单 `:1067` 加 `"tianting"`;
  - task box 内 `anlei.png` 匹配 → 暗雷标记随分析结果一并返回(D3);
  - 绿链提取复用现成 `findGreenBands` 链。
- cloud `CloudWholeTaskObserver`:TIANTING 分支发布 tracker ROI interest(对照 wuhuan 的 `(0,100) 280×604`,周期 1s)+ `prepareTaskInterest` 分支。
- **每次面板同步都重判暗雷**,不缓存跨小任务的标记。
- **DoD**:合成/回放面板帧的隔离测试:含 anlei 与不含 anlei 两组,断言 title 命中、task box 几何、绿链点、暗雷标记四项一次性正确。

### WP3 本地 dialog 探针(七张模板)

- client:对照修罗看打的本地探针链(`DialogService.LocalDialogTemplateMatch` / `WindowObservationSampler.sampleXiuluoLocalKanda`)新增 tianting 探针:
  - interest 下发后,在 G002 共享帧的大 dialog ROI (200,250,640×300) 上匹配;
  - **常驻四张**按优先级:`kaida` → `duoxie` → `zhuoyue` → `yaowang`;命中经输入队列**本地直接点击**(原子 move→wait→click),结果经观察面上报;
  - **`fengyao` 条件匹配**:仅在 `duoxie` 点击后开 ~1s 窗口时匹配(窗口由本地状态或云端 interest 参数控制,择一实现并在卡内注明);
  - `accept.png` 用于 ACCEPT_TASK 阶段(WP4);
  - 全 miss → dialog 帧上报云端(fallback 通道)。
- cloud:`TiantingDialogCatalog` 持有 spec 与优先级;fallback 走 `DialogOptionPolicy.FALLBACK_FIRST_OPTION`。
- **DoD**:隔离测试——合成帧含 kaida+zhuoyue 时只点 kaida;仅含 duoxie 时点 duoxie 且**未开窗口前不匹配 fengyao**;全空帧零动作零点击。

### WP4 主循环:PREPARE / ACCEPT_TASK / RUN_SUBTASKS 骨架

- **ACCEPT_TASK**(设计 §3 + S1):导航天宫(144,114)(`NavigationService.navigateToNPC` + `NavigationTurnYield` 放权)→ 到达事实 → NPC 点击李靖(arrival FIFO;李靖识别**零新增**,已在 BAILONGMA profile + 精确 OCR 兜底)→ 下发 accept dialog interest(本地探针)→ **本地匹配 accept 并本地点击** → **本地判成功:tracker 面板出现 `tianting_title`** → 上报成功 → 进 RUN_SUBTASKS;本地 miss → 云端 fallback 第一绿 → 重回 title 判定。
  - **title 未出现 ≠ 回城**:ACCEPT_TASK 段走 accept 重试/fallback(设计 §4 兜底说明)。
- **RUN_SUBTASKS**:事实驱动优先级循环(§2);park 唤醒集合 = `{PATHING_TERMINAL, 本地 dialog 命中/点击结果, COMBAT_STATE_CHANGED}`,有界 ≤25s。
- **tracker 观察链(WP2 只做了算法侧,这条链在本 WP 补齐)**——WP2 评审确认:算法认识 `"tianting"` 了,但生产上一次都进不去,因为唯一的 `analyzeFullWindow` 调用点硬编码 `"wuhuan"`。本 WP 须按五环同形补全:
  1. cloud `com/bot/dhxy/service/TaskTrackerPanelService`:新增 `prepareTiantingPathingLinkFromObservation(...)`(对照 `prepareWuhuanPathingLinkFromObservation`,taskCode 传 `"tianting"`);
  2. `toReadResult` 与 `model/tasktracker/TaskTrackerPanelReadResult` + `TrackerPanelWindowRelativeResult` 透传 `darkThunder` / `darkThunderScore`;
  3. `CloudWholeTaskObserver.probeTrackerPreparation` 加 TIANTING 分支(现首行即 `!= WUHuan_V2` 返回);
  4. **同批**恢复 `publishObservationInterests` 里的 `tianting-tracker` interest(WP2 已按评审撤回,避免每秒上传 280×604 却无人读);ROI 几何/周期同五环,建议起别名常量而非复用 `WUHUAN_TRACKER_*` 裸常量;
  5. prepared action 携带暗雷标记,供 §5 停下分流使用。
- 绿链点击复用 `submitWuhuanTrackerGreenClick` 的命令形状(move→wait→click + `TurnPathingIntent(UNTARGETED_TRACKER)`)。
- **title 缺失兜底**:RUN_SUBTASKS 段任何一次 tracker 同步看不到 title → 走 RETURN_HOME(业务 §8 末条)。
- **DoD**:模拟事实序列的合同测试覆盖:接任务成功/失败-fallback、绿链点击→park→停下分流、title 缺失触发回城、park 唤醒三类事件各一次。

### WP5 暗雷分支(飞行检测 + 走位巡逻)

- 停下且当前小任务有暗雷标记时:
  1. 飞行检测:Alt+U → 本地截 ROI(660,573,52,24) → 帧 turn 回包 → 云判 `flying/unflying/unknown` → Alt+U 关(同一 turn 原子完成,复用 `detectFlyingStateTurn` 形状);`FLYING` → Alt+C;**`UNKNOWN` → 不取消,直接巡逻**。
  2. 走位巡逻(用户 07-29 重新定义,**不识别人物名字**,`findHorizontalPatrolPoints` 方案作废):取当前地图名(pathing 镜像,空则 `syncMyPosition` 兜底)→ 在 `TiantingGeometry` 的四张地图查表得两点 → **每秒一次左右交替右键点击**,直到 Client Runner 判 `IN_COMBAT`。
     - 输入包形状与"进战释放 turn"骨架照抄 `WildBattleTask.clickBundle`(`MOVE_MOUSE → CLICK_RIGHT(hold 100ms) → WAIT 1000ms` 原子不可拆)。
     - 坐标由实测屏幕坐标减 base(1317,187) 在 `TiantingGeometry` 一处换算;地图名**单向 contains + 最长名优先**;表外地图给 5 次预算后整轮 failed,不无限 park。
     - 用户给的四张地图坐标经复核**无需改动**:`(0,100) 280×604` 是 tracker 的**搜索框**,不是面板本身(真面板矩形在运行时由框内 anchor 算出,`TaskTrackerPanelService:166`),所以点落在搜索框内不等于被面板挡住。曾据此加过的"遮挡自检"是错的,已删除。
  3. 摄妖香照常在线。
- **DoD**:合同测试——地图查表命中/未命中、左右交替顺序正确(含负计数)、`IN_COMBAT` 即停且释放 turn、`UNKNOWN` 飞行态不发 Alt+C、短地图名不得继承长地图名的点。

### WP6 封妖符分支

> **2026-08-06 用户纠正（覆盖本节冲突旧文）：** 首次进入以 Dialog 中 `使用封妖符` 的成功点击
> `ACTION_FENGYAO` 为准；Tracker 是点击后才更新成封妖符，只负责确认/恢复。分支建立后优先检查
> `fengyaofu.png` anchor，anchor 已在时禁止点击 Tracker，只有不在时才点当前绿链一次重开。妖王幻影只允许
> tooltip，不读取或写入 NPC 记忆；下文所有 `MEMORY + TOOLTIP`、进战后存记忆以及无条件重开 Tracker 的旧描述均
> 由本条替代。物理 click dispatched 不是结果，必须等真实 `IN_COMBAT` 或未入战 story 已 Fast Click 后才能比较
> task box。

- **进入**:多谢分支点击 `fengyao` 的那一刻置位封妖符标记 + **四坐标游标清零** + 记录**循环前唯一一次** tracker 快照(`TrackerTaskBoxContentComparator.capture`)。
- **单坐标循环**(每坐标都过 anchor 门):
  1. **anchor 门**:面板帧上行 → 云端判 `fengyaofu.png` 在否(**ROI 见前置 P1,先常量占位**)。不在 → 点左侧绿链打开(**该点击不带 pathing intent**,有界等 anchor 出现,重试 ≤3)→ 复查。
  2. 坐标点击(hardcode 窗口相对坐标,**基点见前置 P2**),**携带 intent `tianting:fengyao-coord:<n>`**;
  3. park 等 `PATHING_TERMINAL` 停稳;
  4. `armDirectCombat` 预告 → **Alt+A** → fresh 捕获 → 帧回传 → **云端仅用 TOOLTIP 识别妖王幻影**
     （不读写记忆，**不做飞行检测**，不走黄字/紫字公式/Ctrl）→ 命中则下发点击。
  5. 三结局(每种都要有结果回传):
     - **进战** → 不存记忆 → 战后处理 → compare `UNCHANGED` 时先查 anchor，anchor 不在才点绿链重开，
       **跳过已点坐标**回循环；compare 变化 = 小任务结束。
     - **tooltip 未命中** → 失败回传 → 右键退出直战模式(`exitDirectCombatClickModeAfterFailure`)→ 导航回李靖重接。
     - **未进战** → story `advanceStoryDialog` Fast Click → compare(与循环前快照比):变 = 下一小任务;未变且有剩余坐标 = 下一坐标;未变且点尽 = 回李靖重接(Q4)。
- **DoD**:合同测试——四坐标按序各点一次不重复、战后重开不重置游标、anchor 不在时才点绿链、anchor
  已在时不点 Tracker、坐标点击携带正确 intent source、三结局分流正确、全链不读写 NPC 记忆、
  `dispatched=true` 不提前比较 task box、**整链只出现一次 Alt+A**。

### WP7 战后处理 + 回城

顺序**不得调换**(设计 §8,用户纠偏):

1. 战斗结束基础恢复 `recoverAfterClientCombatExit(ctx, "tianting", policy)`(摄妖香同修罗/五倍);
2. **先清引妖**(option dialog 挡最前,不清则队长后续全卡):脱战确认后固定等 `1` 秒，再做一次本地匹配 `yinyao.png`；**首次或任一次 miss 立即结束此步骤，不上传 dialog ROI、不等待、不重试**。仅本地命中并实际点击后才 compare tracker(基准 = 进战边沿快照)；未变时再次本地匹配且再次命中才点击，**累计实际点击 ≤5 次**，一旦变化立即进 3;
3. 查 title:消失 → RETURN_HOME;仍在 → 继续;
4. 点 tracker 绿链 → 触发下一小任务导航(移动开始);
5. **移动放权之后**才开队员补给:`taskMaintenanceService.openTeamFirstAidMaintenanceWindow` + `TeamReturnService` 死亡离队等归队(放权型等待,不死等;同修罗)。
- **RETURN_HOME**:`ReturnItemPrescanService` prescan(`whileInCombat`/`afterTrackerGreen`)→ `useCached("tianting/huicheng.png")` → 宏结果回传 → `syncMyPosition` → **`isSameMapName("天宫")` 验证**(2 次尝试,间隔 `cleanUpAll`,照抄修罗 `useReturnItemAndVerifyStartMap`)→ 成功:**`cleanUpAll` 大清理** → 回 ACCEPT_TASK;失败:导航回李靖 → 同样 `cleanUpAll` → 重接。
- **DoD**:合同测试——引妖在恢复之后、title 检查之前;补给窗口在绿链点击之后才开;引妖 5 次封顶;回城验证失败走导航兜底;`cleanUpAll` 只在回城/失败导航后各一次。

## 4. 写集

**双仓协议**:`TurnTaskCode`、`TaskType`(+ 各自 validator/映射测试)。

**cloud**:`task/tianting/**`(新包)、`CloudTurnTaskFactory`、`CloudTurnRuntimeConfiguration`、`cloudbrain/TaskTrackerPanelService`、`CloudWholeTaskObserver`、`SmartClickRecognizer`(WP5 巡逻点函数)、`TaskStartupCheckService`、`TaskTeamAssignmentPolicy`、`LeftTopStatusSwitchService`、`CloudTaskStartupPreparationService`、`AutoCombatService`(队伍急救 gate)。

**client**:`DialogService`/`WindowObservationSampler`(tianting 本地探针)、`WindowTaskControlService`、`MainWindowController`、`BotProperties`、`GameUiSettingsStore`、`WindowRegistrationBatchBuilder`。

**禁止**:改动 `D:\mavenProject\DHXY`;改动 `images/` 下任何图片;改动协议文件而不双仓同步。

## 5. 实施前置(用户后补,不阻塞开工)

| # | 项 | 处理 |
|---|---|---|
| ~~P1~~ | `fengyaofu` anchor 的 ROI | **已闭合**:屏幕 (1792,412)-(1887,451) = 窗口相对 (475,225) 95×39,已写入业务 MD §6 |
| ~~P2~~ | 四坐标的窗口相对换算基点 | **已闭合**:base=(1317,187);四坐标屏幕值 (1771,570)/(1901,570)/(1771,704)/(1903,713)。业务 MD §6 已按此重写——**初版那四个裸值当时没有配套基点、无法换算,已明确作废**(用户 07-29 决议) |
| — | ~~`findHorizontalPatrolPoints`~~ | **作废**:用户 07-29 改为按地图查表两点巡逻,不再识别人物名字;四张地图的巡逻点与同一 base 已写入业务 MD §4.1 |

**前置全部闭合,验收 4 不再 gated。** 换算集中在 `TiantingGeometry` 一处,改 base 是一行。

## 6. 验收

0. **连通性专项审核(用户 07-29 指定,评审重点在此,不是代码风格)**:对照 `docs/云端迁移常见错误清单.md` 逐条套,每条跨端消息走三问——①**发出去对面认不认**(接收端有 case/分支?双仓枚举 byte-identical?validator 白名单放行?taskCode 字符串精确匹配?switch 没有 default 静默吞?)②**处理前旧状态清没清**(上一次的 pathing intent / claim / prepared action / interest / 缓存 / 事件序号会不会卡住新消息)③**动作前置条件满没满足**(上游真会产出吗?产出后有唤醒事件吗?每个结局都有事件、都有界吗)。交付须给"链路连通性表":启动 / 观察兴趣 / 面板分析 / 绿链点击 / dialog 探针 / 暗雷 / 战后 / 回城 / 停下释放,每条标**通 或 断在哪一行**。
1. 双仓编译过;共享协议 byte-identical;新增测试隔离运行全绿。
2. **组队实机**:队长 TIANTING + 队员 AUTO_BATTLE 起一轮,完成 6 小任务(覆盖暗雷 / 看打 / 多谢→卓越 / 封妖符至少各一次)→ title 消失 → 回城验证到天宫 → `cleanUpAll` → 重接。
3. **暗雷分支**:按地图查表取两点(不识别人物名字);左右交替走位触发进战;摄妖香保持在线;`UNKNOWN` 飞行态不误发 Alt+C;表外地图给预算后整轮 failed 而非无限 park。
4. **封妖符分支**:anchor 门每坐标生效;四坐标按序各点一次不重复;点击携带 intent(日志可见 `tianting:fengyao-coord:*`,**无永久 park**);快照只在进分支时记一次;tooltip 进战后记忆落盘、次轮走 MEMORY;**整链只出现一次 Alt+A**。
5. **战后链**:引妖先清且 ≤5 次;队员补给窗口在绿链点击后才开;死亡队员归队放权不死等。
6. 主循环空转期间客户端**无新增独立截图**;战斗期间零 tracker/dialog 输入。
7. 停止 / 异常 / 轮完成均不遗留 turn 占用(`forceRelease` 兜底)。

## Status

- 2026-07-29 **收官整体评审(WP1-WP7 全实施后)与返修**。评审给出十条链路连通性表、验收 0-7 逐条判定、业务 MD 差距清单与实机预期日志序列。两条 P1 都在**接缝**上而非决策层:
  1. **P1 天庭 dialog 点击事件在客户端发送前被自家协议校验器拒绝,且不可自愈,整窗观察面永久死锁**。`WindowObservationSampler` 用 12 参构造器给 `TIANTING_DIALOG_CLICKED` 带上了 `taskCode`/`businessTaskRunId`,而 `ObservationProtocolValidator` 要求这两个字段**恰好只在 expected-combat / replay 边沿出现**;校验发生在 HTTP 发送**之前**,抛 `REQUEST_CONTRACT`,而 key event **保留到被 ack 为止** → 从此每一次请求都在发送前抛,该窗口的 pathing / 战斗 / tracker / 全部 key event 停止上行且**永不恢复**。这不是天庭失效,是整窗观察面死锁——与 WP3 那条已修的漏枚举**同型同后果**。改用 8 参构造器(身份本来就由 observation-run 绑定解析,云端读取侧根本不看这两个字段)。
     - **测试把错误行为钉成了正确行为**(清单 E13 教科书例):`TiantingDialogProbeContractTest` 原断言 `assertEquals(TaskType.TIANTING.getCode(), event.taskCode())`。已反向,并新增 `theReportedClickPassesTheRealWireValidator`——把探针产出的事件塞进真实 `ObservationRequest` 跑生产 `ObservationProtocolValidator.requireValid`。**以后任何"上线即挂"的协议违规都会在这里变红,而不是在实机上变成静默停摆**。
  2. **P1 队员急救的门装了却没人开**。WP1 把 tianting 加进 `AutoCombatService` 的队伍急救 gate,但全仓只有 `WubeiTask` 调 `openTeamFirstAidMaintenanceWindow`,天庭零调用 → 队员每次战后都打 `deferred: team first-aid gate closed`,**永远补不上血蓝**;这比不接线更糟。已按 WP7 第 5 条接在**绿链点击成功之后**(移动放权那一刻,同修罗——早于此就等于用队员的窗口去等一个还没开始走的队长),并在腿停下时 `closeTeamMaintenanceWindow`;注入 `TaskMaintenanceService`(构造 14 参),窗口按 `roundNumber` 分轮。
  - **同批 P2**:①封妖符"任务框读不到"分支原**无预算**(外层 `stalledStopPasses` 只在 `AWAIT_DIALOG` 增,够不到这里)→ 面板被持续遮挡就是每 25s 一趟的无限循环,已复用 `FENGYAOFU_BASELINE_PASSES` 预算并在读到后清零;②`navigateToAcceptNpc` 被当作"回李靖重接"用却**不点他也不切 phase**(等于白走一趟),新增 `returnToAcceptNpcAndTalk`(到达后装 accept 探针 + 点李靖;**不能改切 ACCEPT_TASK**——该 phase 在 tracker 仍有任务时会短路直接弹回子任务循环,形成乒乓);③`pushStoryDialog` 原在每趟顶端无条件执行,会在 anchor 门之前把坐标对话框点掉形成"关掉→缺 anchor→重开→再关掉"循环,已移到业务指定的那个结局(到达后未进战)处;④引妖只点一次不比对,已按业务 §7.4 改为"点击 → 比对 task box → 未变再点,≤5";⑤删掉重复真理源 `TiantingPostCombatPlan`(与 `runPostCombat` 同序,且自带第二份 `YINYAO_MAX_ATTEMPTS`)及其测试、`idlePendingImplementation` 与两个 skeleton 常量。
  - 验证:cloud compile exit 0、client compile exit 0、protocol 双仓 `diff -r` 零输出;cloud G005 **109/109**、client 探针 **11/11**(含新增的真实校验器合同)。
  - **封妖符条件窗口已下沉到客户端(评审 P2-1,本轮一并实施)**。原设计由云端在收到 `多谢` 答复后下发 `TIANTING_FENGYAO` interest,但那条答复本身要花一个采样周期加两趟往返才到云端,而 `使用封妖符` 只在屏上约 1s——interest 到货时选项早没了,**WP6 在实机上会安静地永不触发**(降级到"未命中封妖符"分支,日志完全正常)。且换 interest 会把常驻四张顶掉(单槽替换)。现改为:客户端在**点掉 `duoxie.png` 的同一趟**里开一个 `TIANTING_FENGYAO_LOCAL_WINDOW_MS=2.5s` 的本地窗口(`WindowRuntimeContext.openTiantingFengyaoWindow`),窗口内在**armed 集之外额外**匹配 `fengyao`(共享帧与 fresh 复验两处同规则,否则复验会把命中当成"对话框变了"丢掉);云端**不再下发**该 interest,只记录时刻。窗口是唯一没有往返的位置。新增两条客户端合同:`answeringDuoxieOpensTheFengyaoWindowLocallyWithoutWaitingForTheCloud`(只装常驻四张也能答出 `fengyao.png`)、`fengyaoIsNotAnsweredOutsideItsWindow`。
    - **2026-08-05 G029 已替代本段的 `2.5s` 实现细节：** 观察循环包含同步 HTTP 往返，固定墙钟窗口会在下一帧前自行过期。当前规则为 DUOXIE 实际点击后建立 pending，直到 FENGYAO 实际点击、Dialog interest 清除或 task runtime 重置；不得恢复 `TIANTING_FENGYAO_LOCAL_WINDOW_MS` 或换成更大的超时。
  - **坐标出处已闭合(用户 07-29 决议)**:业务 MD 初版那四个裸值当时**没有配套基点、无法换算成窗口相对坐标,因此作废**;以用户重新实测的一组为准。业务 MD §6 已按此重写(四坐标屏幕值 + base(1317,187) + 窗口相对换算表 + anchor ROI),§4.1 同批补上四张地图的巡逻点与同一 base,§6 末尾"四坐标耗尽后未定义"也按 Q4 决议补成"回李靖对话重接"。**验收 4 不再 gated。** 另:base 若某天量错,降级路径仍安全——anchor 先匹配不上 → 门保持关闭 → 重开 3 次 → 回李靖重接,不会乱点坐标。

- 2026-07-29 **WP6 封妖符分支实施 + fallback 通道 + 一轮连通性评审返修**。至此 WP1-WP7 全部实施完毕(WP6 之前只有决策层、生产零调用者)。
  - **落地**:进入是事件驱动(答 `duoxie` → 记窗口时刻 + 装 `TIANTING_FENGYAO` 探针;答 `fengyao` → `enterFengyaofu` 并 pin 任务框基线);`Facts` 加 `fengyaofu`、`decide` 在 `stopped` 块内先于 darkThunder 返回新 Action `RUN_FENGYAOFU`;`runFengyaofu` 一趟一动作(story 推进 → 基线 → 推进比对 → 待到达则进战 → 取下一坐标 → anchor 门 → 点坐标 + park);进战走 `tryDirectCombatTargetClick`,请求设 `TENTATIVE` 把流水线截断在 **memory + tooltip**(屏蔽黄字/紫字公式/Ctrl——妖王幻影无标定 profile);重开坐标对话框的绿链点击**不带 intent**(不引发移动,带了等于登记一条永远等不到终态的腿)。
  - **本地全 miss 的 fallback 通道**:宽限到期先 `dialogService.handleDialog(fallbackFirstOption("tianting"))`,`FALLBACK_CLICKED` 则本趟结束、下趟重估(Q12),否则才点绿链。
  - **评审 P1 返修(六条,每条都是"分支在生产上一步都走不到")**:
    1. `CloudFastExpectedCombatExitCoordinator.authorizedTask` 不含 tianting → `armDirectCombat` 恒 false → **Alt+A 一次都不按、点击一次都不发**,而结果被当成"已尝试":四坐标逐趟烧光 → 退出 → tracker 仍是封妖符 → 重进 → 游标清零 → 无限重复且日志无异常。已加 TIANTING;`enterFengyaofuCombat` 改为返回布尔,`skipped` 当失败结局分流。
    2. 坐标点击后**没有 park**,而 intent 注册发生在本趟 turn 回包**之后**、镜像"absent fact never overwrites"→ 下一趟毫秒级重入读到点击前的 `ARRIVED` → 立刻点下一个坐标,**第 1、3 号坐标被静默跳过**。改为点完必 `awaitSubtaskFact`,并新增 `fengyaofuAwaitingIndex` latch:到达后先打这一仗,再考虑下一坐标。
    3. `captureTaskBoxSnapshot` 取的是绿链**点击点周围 24×16 的验证小图**指纹(用途是"链还在原位吗"),既不覆盖 title 也不覆盖正文——两个小任务绿链首字形近似即哈希相同 → **恒判未推进**;面板轻微滚动 → 误判推进。已新增 `PreparedDialogAction.taskBoxFingerprint`(在 `prepareTrackerPathing` 同一帧上裁**整个 task box** 计算),分支改用它。
    4. 基线 pin 可能为 null(进分支那一刻 option dialog 正压着面板,其 ROI 与 tracker 搜索框重叠),而 null 被当作"未推进" → **推进判据结构性偏向永不触发**。改为:无基线不得点任何坐标,有界等待后 late-pin;`nowSnapshot == null` 一律"未回答",不等于"未变"。
    5. `tryDirectCombatTargetClick` 自带飞行检测,`UNKNOWN` 直接拒绝整次尝试——而卡对封妖符明写"不做飞行检测"、WP5 也已定案 UNKNOWN 放行。新增重载 `tryDirectCombatTargetClick(request, allowUnknownFlyingState)`,原方法委托 `false` 保持修罗/五倍行为**逐字不变**。
    6. 业务 §6"未触发进战 → story dialog Fast Click → compare"完全没实现,且该路径在分支内不可达(`RUN_FENGYAOFU` 先于 grace/RECLICK 返回)。已在分支内 compare 之前加 story 推进(`fallbackFirstOption` 的 `storyPolicy=IGNORE`,不会点 story,必须用 `clickStory`)。
  - **同批 P2**:记忆键补 `mapName/mapX/mapY`(否则 int 默认 0 通过"已知坐标"检查,四个坐标与不同地图**共用同一记忆键**,坐标 1 学到的点会在坐标 3 上重放,Alt+A 模式下点到什么打什么);整个 Alt+A 序列包进 `taskTurn.run`(原来在 turn 外,多秒序列可被别的窗口插入、可能把窗口留在 Alt+A 模式);anchor 门加 try/catch(捕获失败原会 `npcClickFatal` 掀掉整轮);快照读改走 `freshTrackerLink` 的 10s age 门;封妖窗口过期恢复加 `!dialogOutcomePending` 守卫并改用 `FENGYAO_PROBE_ARMED_MS=6s`(原用 1s 的**屏幕**窗口判**云端**探针存活,答复回到云端时通常已 >1s → 恰在进分支那趟先把常驻探针装回并打印"window closed unused",日志与实际相反);所有分支出口统一走 `leaveFengyaofu`(清 latch,否则下次进分支会读成"已在走向坐标 N")。
  - **自查另加一条**:客户端 dialog interest 是**单槽替换**(`WindowRuntimeContext.updateDialogInterest` 即 `set`),装 `封妖` 必然顶掉常驻四张且**无自过期**——窗口过了没用上,这个窗口就再也答不了任何战斗对话框,且以后真冒出 `使用封妖符` 会在毫不相干的腿里打开坐标对话框。已加过期恢复。
  - **评审已核实无问题**(留档):`TIANTING_FENGYAO` 下发链全通(validator 无操作名白名单 → `DialogOperation.valueOf` → `TiantingOptionSet.FENGYAO` → `matchFengyaoOption`);`TIANTING_DIALOG_ANSWERED` 上行 detail 与云端解析**字符串精确对齐**;**整链 Alt+A 只出现一次**(唯一在 `NpcClickService:1347`,direct-combat 模式跳过 Alt+4,`clickNpcSmart` 已剥离);`TENTATIVE → lightScan` 确实屏蔽黄字/公式/Ctrl 而保留 memory+tooltip;`fengyaofu.png` 双仓 md5 一致;`COMBAT_STATE_CHANGED` 发布不受 `authorizedTask` 限制(看打/暗雷的战斗边沿一直是通的)。
  - 验证:cloud `compile -DskipTests=false` exit 0、client `compile` exit 0、两仓 protocol 目录 `diff -r` 零输出;G005 隔离运行 **116/116**(新增 `TiantingFengyaofuBranchContractTest` 钉住 authorizedTask 含 TIANTING、严格重载仍在、task-box 指纹独立于链指纹、探针存活长于屏幕窗口、无基线不得开工)。
  - **(当时未闭合,均已在后续轮次闭合)**:四坐标/anchor ROI 的出处 → 用户 07-29 决议以新实测为准、初版裸值作废,业务 MD 与设计已同步重写;队员补给窗口 → 收官轮已接在绿链点击成功之后。

- 2026-07-29 **连通性专项评审(用户指定的新评审重点,见验收 0)与返修**。判据=`docs/云端迁移常见错误清单.md` 的十三类事实链断裂 + 三问(对面认不认 / 处理前旧状态清没清 / 前置条件满没满足)。十条链路给出连通性表,两条 P1 全属"发出去对面处理不了":
  1. **P1 事件排空是死代码,`TIANTING_DIALOG_ANSWERED` 在最主流路径上被永久丢弃**。`awaitNewer(..., 0L)` 中 **0 不是"非阻塞轮询"而是非法值**(`CloudWholeTaskReadyEventState` 明文契约,全仓另外 16 个调用点共享该语义),首次迭代必然 empty 直接返回。而 ready event **每 type 只有一个槽**、`findNewer` 只回"请求类型里序号最大的那一条",所以消费一条就把游标推过了它的兄弟——"客户端点看打 → 进战"这条最常见路径里,dialog 边沿排在战斗边沿前一位,**永久不可达**,`answeredOption` 永不设置,多谢→封妖入口拿不到唯一信号(清单 E5/E13)。修法:给 `CloudWholeTaskReadyEventState` 加**真正的非阻塞读** `pollNewer`(复用已有 `findNewer`,不改 `awaitNewer` 的共享语义);排空改为**逐 type 各用同一游标读一次**、按序号升序 latch、最后才推进游标。
  2. **P1 去李靖的路上把正在走的那条腿的 intent 删了,然后 park 在不可能到来的事件上**。`NavigationService` 在返回 `PATHING_STARTED` 之前就已把 **ACTIVE** intent 同步进云端镜像;`navigateToAcceptNpc` 尾部无条件释放 → 客户端 `hasActiveIntent()` 对 ACTIVE **完全放行** → 正在走的 intent 被清成 NONE → 终态观察者再无归因对象,`PATHING_TERMINAL` 永不发布 → 每跳白付 25s 死等(且 `PATHING_STARTED` 不计 accept 预算,不会 FAILED,只会一直慢)。修法:`releasePathingIntent` 开头 **ACTIVE 直接跳过**——"清 ARRIVED 残留"这个真实目的不变。
  3. **P2 引妖只装一次兴趣就走,5 次上限是死分支**(`yinyaoArmCount=0` 就在 `if (< 5)` 前一行)。而引妖 dialog 的 ROI (200,250,640×300) 与 tracker 搜索框 (0,100,280×604) **重叠**:没清掉的引妖框会让面板读不到 title,连出 3 条 `TASK_NOT_FOUND` → 误判一轮结束、提前回城还烧一张回城道具。改为**有界重试循环**:装兴趣 → 等 `TIANTING_DIALOG_ANSWERED`(2.5s/次)→ 命中即消费边沿并退出,≤5 次;从未命中则 warn 写明"若框还在会挡住 title"。
  4. **P2 每个小任务起点白付 ≥5s**:`clickAcceptNpc` 的定向点击留下 ARRIVED 镜像,进 RUN_SUBTASKS 首趟就把 `legStopped` 误置为 true。新增 `enterSubtasks(...)`:进子任务循环前释放 accept 段残留 + 清 `legStopped`/宽限计时。
  5. **P2 拿可能过期的镜像做不可逆动作**:`clearPathing` 客户端侧**无条件清、不校验 intentId/source**,而"这条 intent 是不是我的"原本判自云端镜像(最坏晚一个观察周期)。改为在同一 turn 内先 `readPathing` 取**客户端权威快照**核对 `tianting:` 前缀,读不到就不清。
  6. **P2 `readFlyingState` 把"turn 没拿到"当成"确认在地面"**:原先初值就是 `UNKNOWN`,而 `UNKNOWN` 按决议"不按 Alt+C 直接巡逻"——真在飞时会带着坐骑巡逻、右键点地不进战直到预算耗尽。改为 turn 未达成时返回 null(状态保持未读,下一趟重读)。
  7. **P2 陈旧绿链**:`peek` 本身不做 age 检查,而腿走起来后观察者停止刷新,link 会停在**战前那一帧**;战后 `legStopped=false` 的下一趟就会点它 → 走到上一个小任务的坐标(点击本身成功,日志完全正常)。新增 `freshTrackerLink(...)` 10s age 门,读事实与点击两处都走它。
  - 同批修:`useCached` 改走**不带 retained-replay 身份**的新重载(`TurnProtocolValidator` 的 retained 白名单只放行 XIULUO_V2/WUBEI,`tianting` 发过去必抛 `IllegalArgumentException` 整轮 FAILED;且只在 prescan 真预热成功后才触发,缓存没热时反而"看起来正常");prescan 补 `afterTrackerGreen` 第三个槽(服务每轮随机挑一个槽,只offer两个 → 抽到第三个的轮次永不预热);`executed=false` 时**重发同一 interest** 换新 claim key(客户端 claim 已烧、re-arm 条件是"选项从屏幕消失",而正因没点成选项还在 → 原本永远无人重试),≤3 次;删掉云端多余的 `huicheng.png` 副本(该模板由客户端 BagService 解析,双份必漂移;客户端 `images/` 原件未动);删掉无生产调用者的 `NpcClickService.matchesTemplateInWindowRegion`(与 WP6 同单再落)。
  - 评审已核实**无问题**的项(留档免重查):启动链/观察兴趣链/面板分析链/绿链点击链/暗雷命令链/战后边沿链/回城链 **均通**;turn 可重入不存在嵌套死锁;`maxRounds=1` 语义正确;`cleanUpAll` 的 phaseCode 根本不上线(`UI_CLEAN_ALL` 零参数);两仓 protocol 目录 `diff -r --strip-trailing-cr` 零输出,序列化走 `valueOf` 非 ordinal,`TIANTING_FENGYAO` 无 golden 风险且客户端惰性(云端零 install 点)。
  - 验证:cloud `compile -DskipTests=false` exit 0、client `compile` exit 0、observation 协议双仓 byte-identical;G005 隔离运行 **109/109**(新增 `TiantingWakeEventDrainTest` 钉住"排空必须有非阻塞读"、`TiantingDialogSummaryTest` 钉 `executed=`/选项名解析)。
  - **记账(当时)**:WP6(封妖符执行器)与 WP7 的"队员补给窗口"仍未实现 → 验收 4、5 暂 gated,不得按"决策层已交付"计。**两项均已在后续轮次实现,验收 4/5 不再 gated**;`TiantingPostCombatPlan` 作为重复真理源已在收官轮删除。
  - **待用户裁定**:`WindowRegistrationBatchBuilder.buildIndependentWindows` 给所有窗口同一 taskType + `WindowRole.UNKNOWN`、**不做**队长/队员分派(实际分派全靠云端 tooltip 角色检测),且 `CloudTurnTaskRuntime` 存在"`windowRole` 字面量为 LEADER 就把 UNKNOWN 升级"的通道——哪天客户端真填 role,单窗口选天庭的拒绝会被绕过。是否需要在客户端补真分派?

- 2026-07-29 **整体评审(覆盖 WP1-WP7 + dry run)与首轮返修**。评审给出端到端断链清单、验收七条逐条判定与实机预期序列;按其优先序修了前 5 项:
  1. **拆掉两个死循环**(评审判定"比未实现更糟"):`RUN_POST_COMBAT` 原本"保留 pending 并 park"→ 首战结束后永久占着窗口刷 warn;`RETURN_HOME` 原本 `idlePendingImplementation` 无限空转。两者改为 **fail-fast 交还窗口**并 error 日志写明未实现的是哪个 WP——未实现的阶段必须把窗口还回去,而不是占着。
  2. **决策优先级纠正**:`trackerLinkReady` 提到 `stopped` 之前。原次序下每个小任务的起点(停在上一个终点、下一条链已就绪)都会先落进 `AWAIT_DIALOG`、熬满 5s 宽限才以 `RECLICK` 点出去——每小任务白付 5s,且日志把正常推进记成异常重试。
  3. **`TIANTING_DIALOG_ANSWERED` 的模板名不再被丢弃**(评审指出与 WP4a 那条"暗雷标记算出又丢"同型):客户端已把模板名跨仓送到岸、observer 已写进 summary,消费端却降级成 boolean。现解析出 `answeredOption`,多谢分支/封妖符入口/Q12 重估三条路才有唯一入口。
  4. **探针 interest 生命周期**:新增 `clearDialogProbeInterest`,在**进战时**与**任务结束的 finally** 各拆一次。原实现装上就再不摘,战斗中/回李靖途中/任务停止后客户端仍每秒匹配并可能点击——这是验收 6 后半条唯一的结构缺口。
  5. 决策测试随优先级同步更新(新增"就绪的链不该被当作等对话框""暗雷不被过期链改道")。
  - 验证:双仓 compile + client test-compile 三项 exit 0;G005 云端 **72/72**、客户端 10/10。
- 2026-07-29 **WP4b 二轮返修 + WP5/WP6/WP7 决策层 + dry run**。
  - **WP4b 评审 P1 返修**:①绿链点击执行器落地(`clickTrackerLink` + `submitTrackerGreenClick`,照五环形状,携带 `UNTARGETED_TRACKER` pathing intent——否则窗口会移动却没有事实可等,循环 park 在永不到来的事件上);②**dialog 探针兴趣的下发点**落地(`installDialogProbeInterest`,随绿链点击同批下发并覆盖整条腿——WP3 的整条本地探针链此前在生产上是死的,因为没有任何代码打开这个总开关);③事件**逐条排空**(`awaitNewer` 只回最新一条,而"客户端点看打→进战"这条最主流路径里 dialog 边沿排在战斗边沿前一位,原实现会整条跳过);④negative 事件按 sequence **去重计数**(`latest` 会一直返回同一条陈旧事件,原实现会把接任务前那条必然出现的 `TASK_NOT_FOUND` 在三圈内累计成"title 消失"而中途回城);⑤accept 重试预算只统计**真实尝试**,走路等待不计(跨图去天宫会产生多次寻路终态,原实现会在人还在路上时耗光预算判 FAILED);⑥`RUN_POST_COMBAT` 不再静默清掉战斗退出边沿——那是一次性信号,WP7 未接线前保留 pending 并告警,而不是吞掉。
  - **WP5 决策层**:`TiantingDarkThunderPlan`(飞行读数→落地→解析巡逻点→左右交替巡逻→进战即停)。**UNKNOWN 飞行态明确不按取消键**——读数失败时按下坐骑键会把地面角色送上天,正是这一步要避免的状态。11 条测试。
  - **WP6 决策层**:`TiantingFengyaofuPlan`(anchor 门→坐标点击→等到达→点目标;四坐标按序各一次、战后不重置游标、点尽即 EXHAUSTED 不成环)。9 条测试。
  - **WP7 决策层**:`TiantingPostCombatPlan`(恢复→引妖≤5→title→绿链→**移动放权后**才开队员补给)。7 条测试钉死"引妖先于 title 检查"与"补给不得早于绿链"。
  - **Dry run 通过**:云端 compile、客户端 compile、客户端 test-compile 三项 exit 0;五个共享协议文件跨仓 diff 零差异;G005 云端全量 **70/70**、客户端 **10/10**。
- 2026-07-29 **WP4a/WP4b 评审返修**(此前两轮漏派评审,用户指出后补派;四条 P1 全修)。
  1. **`CloudWholeTaskObserver.isRelevant` 没加 TIANTING —— 观察面对天庭根本不启动**。`start()` 首行即此门,拦在 `publishObservationInterests` / `observeLoop` / `consumeClientClickEdges` 之前;而 `PATHING_TERMINAL`/`COMBAT_STATE_CHANGED`/`PREPARED_ACTION_READY` 的全部发布点都在这个 observer 内。后果是 WP4a/WP4b 的产出**在生产上一条都不执行**,park 必然坐满 25s。与 WP2 被打回的"分支正确却不可达"同型、只是上移一层。已补,并在 `CloudWholeTaskObserverPolicyContractTest`(该文件本就在逐个枚举任务码)加断言钉死。
  2. **暗雷标记在 observer 路径上被算出又丢弃**:`preparePathingLinkFromObservation` 只走 `buildPreparedAction`,从不经过 `toReadResult`,而 `PreparedDialogAction` 没有该字段——天庭唯一的消费者拿不到分流依据,暗雷小任务会走进 `AWAIT_DIALOG` 等一个永不出现的对话框。已给 `PreparedDialogAction` 加 `darkThunder`/`darkThunderScore` 并在 `buildPreparedAction` 里从同一次分析拷贝(标签与链接同源,避免把点击配到别的小任务的标签上)。
  3. **测试断言的是 observer 不走的那条转换**:上一轮"3/3 绿"是错误信号。已改为断言两条消费路径共同的源头(analysis 的标签与分数)+ 在途读者的 `toReadResult`;**observer 路径的那一次字段拷贝目前只有编译与评审保证,没有测试**——搭 `buildPreparedAction` 的夹具需要真实 `TaskExecutionContext` 与 image 服务,成本远超收益,如实记录而非造一条能过的假测试。
  4. **`AWAIT_DIALOG` 没有出口,把已拍板的 Q11 从状态机里删掉了**:`Facts.awaitingDialogSinceMs` 有字段有注释却零引用。已加 `RECLICK_TRACKER_LINK` + `DIALOG_GRACE_MS=5s`(宽限内仍等——对话框可能在到达后一拍才开;超时按修罗 STOPPED_AWAY 回 tracker 重点绿链),并加两条测试:宽限内/外分别走等待与重点、暗雷分支永不适用该宽限。
  - 验证:cloud compile exit 0;G005 回归 cloud **43/43**、client **10/10**。
  - **未修、留待后续**(评审 P2):`awaitSubtaskFact` 在 park 时刻才取 sequence 的丢事件窗口(应像新手那样把 sequence 提到循环外持有)、`dialogOutcomePending` 无生产者/清除契约、`RETURN_HOME` 单帧判定建议加连续 miss 计数、唤醒集合缺 `STORY_DIALOG_VISIBLE`、WP4b 全部产出目前仍无生产调用者(四个 phase 还是空转骨架)、tianting ROI 复用五环裸常量。
- 2026-07-29 **WP4a 完成(tracker 观察链闭合,还清 WP2 欠账)**。主循环(ACCEPT_TASK / RUN_SUBTASKS)仍未做,见下方 WP4b 待办。
  - `com/bot/dhxy/service/TaskTrackerPanelService`:把 `prepareWuhuanPathingLinkFromObservation` 的实现抽成带 taskCode 参数的私有方法,新增 `prepareTiantingPathingLinkFromObservation` 委托进去——两条路径共用同一段代码,不存在"天庭那份忘了同步"的可能。
  - `toReadResult` 透传 `darkThunder` / `darkThunderScore`;`TaskTrackerPanelReadResult` 加这两个字段。**`TrackerPanelWindowRelativeResult` 没加**——它在生产里零使用者,按 WP2 立的判准不给死代码加字段。
  - `CloudWholeTaskObserver.probeTrackerPreparation` 从"只认 WUHuan_V2"改为按任务分派 ROI key / prepare 方法 / negative 事件的 taskType+targetKeyword;WP2 撤回的 `tianting-tracker` interest **与它的读者同批恢复**。
  - `toReadResult` 由私有实例方法改为**包内静态**(它本就是纯函数),使转换可被直接断言——避免用 Unsafe 造实例(C lane 已有明确教训)。
  - 验证:cloud compile exit 0;新增 `TiantingTrackerPrepareChainTest` **3/3**——暗雷标记与原始分数确实抵达业务结果、无标记的小任务不被误标、**同一帧按 "wuhuan" 读必须 miss**(WP2 那个"分支正确却不可达"的洞压成一条断言:任务码传错时看起来像"这里没有天庭任务",而不是像 bug)。G005 全量回归:cloud **18/18**、client **10/10**。
- 2026-07-29 **WP3 完成(本地 dialog 探针)**。
  - client 新增 `window/observation/TiantingDialogLocalMechanics`(纯匹配,可隔离测试):dialog ROI `(200,250,640×300)`、阈值 0.85、raw ROI × raw 模板(同 修罗 看打,模板本就是实拍裁图,洗字只会丢信息);**常驻四张** `kaida→duoxie→zhuoyue→yaowang`;`fengyao`/`yinyao`/`accept` 各有独立入口,**不在常驻集内**(封妖符只在多谢点击后的窗口里问,引妖属战后步骤,accept 属 ACCEPT_TASK 段)。模板缺失时该项只是不命中,不会把下一优先级顶上来。
  - client `WindowObservationSampler.sampleTiantingDialogProbe`:仅当任务下发 `TIANTING_COMBAT_OPTION` 探针兴趣时才动作——**兴趣未发布前完全惰性**(与 WP2 撤回的 interest 相反:那是有生产者无消费者会白烧带宽,这里是消费者等生产者,零成本)。命中后按 修罗 同款守卫在**新鲜帧上复验**(共享帧可能已过一周期,对着已关闭的 dialog 点会打到后面的东西),复验模板不一致即放弃;点击后经观察面上报 `TIANTING_DIALOG_ACTION`=`<模板名>:<clicked|click-failed>`,**不传屏幕坐标**。
  - **评审返修(P1,三条都是上线即挂级)**:
    1. **`ObservationDialogOperation` 漏加 `TIANTING_COMBAT_OPTION`**——interest 的 operations 每周期原样镜像上行,`ObservationProtocolValidator` 对每个 operation 做 `valueOf`,不认识就在**客户端发送前**抛 `REQUEST_CONTRACT`;而 runner 的毒化抑制只处理 HTTP 状态类,**不会自愈**。WP4 一装 interest,该窗口的 pathing/战斗/tracker/key event 全部停止上行——不是天庭失效,是整窗观察面死锁。已两仓同步补(byte-identical 已校验)。
    2. **重复点击**——interest 按设计"移动全程有效"不会因点击而清,而原实现没有任何一次性语义:dialog 关闭前的几百毫秒里会被连点数次,多余的点落在已经切换的战斗界面上。已补 `WindowRuntimeContext.tryClaimTiantingDialogOption/clearTiantingDialogOptionClaim`(claim 放 **context** 而非采样器,采样器中途重建也不会重答;claim 在**复验之后、点击之前**取,复验失败不白烧 claim,输入抛异常仍消耗 claim);无任何选项在屏时清 claim = 唯一的重新武装点。另补 `isDue` 1s 节流(与 kanda 同步调),避免每周期整窗重绘 + 点击。
    3. **点了没上报**——原走 `facts`(尽力而为,发送失败即丢),点击却是不可逆的物理动作。已改走 **key event**(新增 `ObservationKeyEventType.TIANTING_DIALOG_CLICKED`,两仓 byte-identical),沿用 runner 的未确认重发。
  - **评审返修(P2)**:补 `isExpired` 检查(云端漏发 clear 时不至于永久 armed)、窗口绑定 + 玩家身份 epoch 双重围栏(重绑/换角色不替他人点击)、分数与完整日志(命中模板/分数/点击坐标,阈值可实机标定)、`matchFengyaoOption/matchYinyaoOption/matchAcceptOption` 三个零调用者已删除(随 WP4/WP6/WP7 各自调用点再落地,同 WP2 的"有生产者无消费者"判准)。
  - 协议:`ObservationFactType.TIANTING_DIALOG_ACTION`、`ObservationKeyEventType.TIANTING_DIALOG_CLICKED`、`ObservationDialogOperation.TIANTING_COMBAT_OPTION`(三者两仓 byte-identical 已校验)、`DialogOperation.TIANTING_COMBAT_OPTION`(两仓各自包内同名新增)。
  - 验证:双仓编译 exit 0、client test-compile exit 0;**11/11**——`TiantingDialogLocalMechanicsTest` 5 条改为**测混淆而非自证**(四张模板逐一单独上屏必须各自认出自己;看打与卓越同屏出看打;封妖单独在屏时常驻探针够不到;引妖/接任务这两张真实天庭模板不得被战斗轮询认领;null 帧不炸;命中点断言的是模板**中心**±1 而非包围盒);新增 `TiantingDialogProbeContractTest` 5 条覆盖此前零测试的采样器层(**同一 dialog 连续三周期只点一次**、dialog 关闭后新 dialog 可再答、点击以 retained key event 上报且带模板名/executed、别的任务的 interest 不驱动本探针、过期 interest 停止点击)。
  - **既有红(非本次引入,已取证)**:`WindowObservationKandaContractTest` 全红,NPE 在 `collectBound` 首行的 `refreshSharedCycleFrame` → `CoordinateHelper.getScaledRect` → null tracker;HEAD 的 `collectBound` 同样无条件调用、HEAD 的该测试同样传 `new CoordinateHelper(null, null)`,与 WP3 无关(天庭新测试给 CoordinateHelper 传了可用 tracker 才能跑起来)。
  - 归 WP4:云端在 ACCEPT_TASK/RUN_SUBTASKS 里发布该探针兴趣、消费 `TIANTING_DIALOG_CLICKED` 唤醒 park、**全 miss 时把 dialog 帧上行走 `FALLBACK_FIRST_OPTION`**(WP3 只交付"常驻四张匹配+点击",fallback 上行与封妖条件窗口随各自调用点落地——卡的 WP3 定义据此收窄)。
- 2026-07-29 **WP2 完成(tracker 面板一趟出全部)**。
  - cloud 资源新增 `src/main/resources/images/template/tianting/{tianting_title,anlei,fengyaofu}.png`(从客户端仓复制;云端面板分析需要本地可读的模板副本,`images/` 原件未动)。
  - 纯算法 `cloudbrain/TaskTrackerPanelService`:新增 `TIANTING_TASK_KEY_TRACKER` + title 模板常量 + `analyzePanel` 分支(阈值 0.82);`normalizeTaskCode` 白名单加 `tianting`;`detailHeight` 保持默认 65(=五倍块高,Q7)并补注释说明为何不用修罗的 40;**新增 `matchesDarkThunder(detail)`**——在同一次已裁好的 task box 上匹配 `anlei.png`(0.82),结果随 `PanelAnalysis.darkThunder()` 一并返回,不需要第二次探测再与本帧配对。
  - **测试抓到的真缺口**:链接提取 switch 原本没有 `tianting` 分支,落进 `default -> List.of()`,天庭永远拿不到绿链。已补 `case "tianting" -> wuhuanTrackerLinks(...)`(与五环同形:单条导航链 + 末尾 `(x/y)` 进度尾不点)。
  - **评审返修(P1)**:①`tianting-tracker` interest **已撤回**——它有生产者无消费者(`probeTrackerPreparation` 首行即 `!= WUHuan_V2` 返回),而 WP1 已让 TIANTING 可启动,留着会每秒上传 280×604 PNG 无人读;interest 与其读者同批上线,已写进 WP4 清单。②WP2 的"一趟出全部"目前**只在算法层成立**:生产唯一的 `analyzeFullWindow` 调用点硬编码 `"wuhuan"`,天庭分支进不去;完整透传链(prepare 方法 / ReadResult 字段 / observer 分支 / prepared action)已逐条列入 WP4,不得当作"读个字段"。
  - **评审返修(P2)**:暗雷阈值 0.82→**0.90**(误差代价不对称:漏判走 dialog 可恢复,误判进无兜底的巡逻死循环);`matchesDarkThunder` 改为 `darkThunderScore` 返回原始分并新增 `PanelAnalysis.darkThunderScore()`,实机可据日志标定而非猜;`detailHeight` 由默认分支改为显式 `case "tianting"` + 独立常量(防他人调别的任务高度时静默改天庭)。
  - 验证:cloud compile exit 0;`TiantingTrackerPanelAnalysisTest` **5/5**——含/不含 anlei 两组(title 命中、box 几何、绿链点在 box 内、标记 true/false)、**五环 title 不得被读成天庭**(比"空面板"更贴近真实混淆风险)、**进度尾不点**(两段绿:目的地名 + 窄的 `(1/3)`,断言 link 的 `maxX` 停在尾段左侧——只断言中心点会在"两段被吞成一条"时也通过)、**标记分差 >0.3** 作为阈值可标定性的守卫。G005 全量回归 **15/15**。
- 2026-07-29 **WP1 完成(经一轮对抗性评审返修)**。
  - 落地:双仓 `TurnTaskCode.TIANTING` + `TaskType.TIANTING`(协议 diff 零差异)、validator/runtime maxRuns 规则、cloud `task/tianting/` 五件套骨架、工厂 descriptor+provider、运行时 bean、client 三处 switch + `tiantingMaxRuns` + UI 接线。
  - **评审 P1 返修(关键)**:真正的生产准入门是 `CloudTeamRolePreflightService.assign`,而非 `TaskTeamAssignmentPolicy`(后者在 cloud main 零调用者)。首版只改了后者 → 单窗口选天庭时 tooltip 失败判 UNKNOWN → 直接放行,"必须组队"形同虚设。已在 `assign` 内把 TIANTING 与 WUBEI/XIULUO_V2 同列拒绝;测试重定向到 `assign`,并新增"天庭与五倍逐角色同门"断言防单独放松。
  - **评审 P1 守卫返修**:`CloudTurnTaskFactoryAllowlistTest`(7→8 provider、`values().length` 7→8、新增 TIANTING 实例/descriptor 断言)、`CloudTurnTaskRuntimeContractTest`(构造器补 provider)、两仓 `TurnCoreProtocolGoldenJsonTest`(枚举名单补 TIANTING)。
  - **评审 P2 补齐**:`LeftTopStatusSwitchService` + `LeftTopStatusDecision`(两处重复实现同改)、`AutoCombatService` 队员急救 gate 加 tianting。
  - **评审 P2/P3 结构修正**:`TiantingRoundContext` 补 `taskBoxSnapshot`(封妖符"只记一次"快照的结构归属)与 `fengyaoWindowOpenedAtMs`(多谢后 ~1s 条件窗口),去掉与 phase 重复的 `taskAccepted`;`runPhase` 终态改为 fail-fast;`execute` 补 stop-race 的 RuntimeException 兜底。
  - 验证:cloud `compile -DskipTests=false` exit 0、client `compile`/`test-compile` exit 0;协议两文件跨仓 diff 零差异;隔离运行 **cloud 10/10**(天庭注册合同 6 + 工厂 allowlist 4)、**client 映射合同 2/2**;golden 枚举顺序用独立程序在两仓直接验证一致。
  - **既有红(非本卡引入,已取证)**:①两仓 `TurnCoreProtocolGoldenJsonTest` 各 2 条失败——另一会话的新手工作给 `TurnLocalOperation` 加了 4 个 `XINSHOU_*` 值但未更新 golden 名单/typed-argument union(去掉本卡改动后同样失败);②client `WindowRemoteTurnControlContractTest` 4 条失败——HEAD 里 `SLEEP_COMPUTER` 早已映射而测试仍断言其应被拒,另两条是 role 投影,均与任务码无关。两者都超出本卡写集,未擅自修改。
  - 待用户裁定:`SLEEP_COMPUTER` 到底是否属于"远程 turn 支持"的契约(决定删哪条断言)。
- 2026-07-29 建卡(实施级,外部 worker 可独立承接)。设计已定稿冻结,13 条开放问题 + D1-D4 全部拍板闭合,外部评审一轮 5 项必修已修复。
- **卡号说明**:G003 已被两处占用(动作队列架构卡 / 新手任务 a-b 卡),G004 = 野外战斗;本卡取 **G005**。G003 撞号问题待用户裁定,与本卡无关。

## 2026-08-01 01:07 运行回归：绿链未起步被错误当作已到达

- 窗口 `hwnd-61F0D0C`（`67555`）在 `01:07:20.180` 已真实左键点击天庭绿链 `(238,313)`；
  `01:07:23.068` Client 记录 `local pathing start proof negative; no registration`，没有为该点击注册
  `PATHING_STARTED`/active intent。
- 随后不是收到 `PATHING_TERMINAL`，而是 Cloud 现有 `legStarted=false -> legStopped=true` 语义把
  “未证明启动”直接等同“目的地在脚下”。`01:07:29.570` 清掉 `TIANTING_COMBAT_OPTION` interest（reason
  `tianting:dark-thunder`），`01:07:30` 开始 `Alt+U`、`Alt+C`，`01:07:33.939` 起开始暗雷巡逻右键。
  因此用户观察到的“还没到目的地就被打断去做暗雷”属实。
- P1 根因：`TiantingTask.legStarted` 将 start-proof 负结果压成二值 false，调用处把 false 当作
  “已经到达”；这个推论只在已知目标确实脚下时成立，不能适用于普通绿链点击失败、起步延迟或 start-proof
  漏检。
- 修复方向待用户确认：将结果拆为 `STARTED / NOT_PROVEN`，只有本地 Runner 对本 intent 发布
  `PATHING_TERMINAL` 后才允许进入 `RUN_DARK_THUNDER`；`NOT_PROVEN` 保留当前绿链的重试资格并重新准备，
  不得清 dialog interest、不得 `Alt+U/Alt+C` 或巡逻。需要补此连接性合同：绿链点击后无 intent 时零暗雷输入。

## 2026-08-02 13:19 运行故障：未完成子任务被 watchdog 强制回城

- 现场 Cloud 日志（窗口 `hwnd-61F0D0C`）证明回城前并未完成任务：最后一帧事实仍为
  `titlePresent=true`、`pathingActive=true`。随后 `TiantingTask.runSubtasks` 的子任务 watchdog 在
  `watchedMs=205322` 触发，并明确记录 `returning home to re-accept the round`。
- 因此这不是回城道具识别自行误触发：主循环已经错误切进 `RETURN_HOME`；该 phase 的既有两次新鲜匹配/使用
  才连续产生 `return item attempt 1/2`、`2/2`，表现为“任务未做完却一直回城”。
- **P1 结论**：`SUBTASK_UNCHANGED_TIMEOUT_MS` 不能把“子任务面板尚未变化/寻路尚在进行”解释成整轮完成或可重接。
  天庭业务规则中，进入 `RETURN_HOME` 的唯一完成依据必须是已读取任务面板且天庭 title 真实消失；子任务超时
  只能保持当前轮的重试/恢复路径，绝不能使用回城道具。
- 待修复验收：构造 `titlePresent=true` 且 watchdog 超时的事实，断言不会进入 `RETURN_HOME`、不会下发
  `FIND_AND_USE_TASK_PAGE`；title 消失时仍保留原有回城链路。

## 2026-08-01 天庭李靖面板次数校正：红字二次裁剪错误

- 现场：`19:59:57`/`20:00:01` 已由 Client 成对上传完整 `640×300` 李靖 dialog ROI，但
  `first-ocr.txt`、`fresh-ocr.txt` 均为空，UI 继续显示本地次数。OCR sidecar 对应记录
  `The text detection result is empty`。
- 根因：Cloud `TiantingDailyCompletionCalibrator` 又把完整 dialog ROI 固定裁成 `y=92..133` 的红字行；
  实机原图中的“今日已完成天庭任务：54次”实际位于 `y=142..155`。该次误裁生成近全黑 mask，fail-close
  正确拒绝了写回，但不应继续维护此布局假设。
- 用户裁决并已实施：直接 OCR 客户端已上传的**完整 dialog ROI**，不再按红色或固定行坐标裁剪。保留两帧同值、
  完整文案 `今日已完成天庭任务：N次` 和不递减三道写回门；不影响本地 accept 物理点击。
- 验证：Cloud `mvn -q compile` 已通过；尚待 fresh runtime，要求同一次李靖 accept 后日志出现
  `天庭 daily count calibrated from 李靖 accept panel`，并核对 UI 与当日红字一致。

## 2026-08-04 05:48 运行故障：封妖符到点后误判任务推进，跳过 `Alt+A`

- 现场窗口 `hwnd-F99187E`，任务运行 `remote-turn-06016d34-cbf2-4db3-b4ca-82aa18390709`。
  `05:48:30` 已真实点击封妖符第 1 个坐标，source=`tianting:fengyao-coord:0`；`05:48:44` 本地 Runner
  发布 `PATHING_TERMINAL/STOPPED_AWAY`，位置为广寒宫 `(26,58)`，说明坐标点击、移动和到达链均正常。
- Cloud `logs/local-stack-cloud.err.log:15898-15922` 随后明确记录：先进入 `RUN_FENGYAOFU`，再输出
  `封妖符 sub-quest advanced; leaving the branch after 1 coordinate(s)`，紧接着切为
  `RECLICK_TRACKER_LINK`。Client 从 `05:48:46` 起也只出现 `tracker-green-click:reclick`，没有任何
  `Alt+U`、`Alt+A` 或妖王幻影点击动作。因此这不是输入失败或异常退出，而是正常分支做出了错误业务决策。
- **P1 根因**：Cloud `TiantingTask.runFengyaofu` 在处理 `fengyaofuAwaitingIndex >= 0` 的已到达坐标之前，
  先用 `!nowSnapshot.equals(taskBoxSnapshot)` 做整框二进制指纹的严格相等判断。任何同一小任务内的像素、抗锯齿、
  绿链显示或布局变化都被解释成“小任务已推进”，并调用 `leaveFengyaofu` 清掉待战坐标。当前代码因此永远到不了
  后面的 `pushStoryDialog -> enterFengyaofuCombat`。
- **同一任务内容的直接证据**：branch baseline 对应的 PreparedAction `f9656a7f-...`（Cloud 日志 `15832`）异步
  OCR 在 `15859` 读为 `天庭任务【常规玩法】|请按照封妖符上所示地图前往|查探|四大妖王【天庭任务】`；到达后
  current PreparedAction `ed17787e-...`（`15915`）异步 OCR 在 `15949` 读出的正文和行结构相同。两次 task box
  都是 `209x78`，绿链点击点也同为 `(362,299)`。因此这不是任务完成后的文字变化，而是同一语义画面产生的
  binary fingerprint 差异；OCR 仅把同一标题括号分别识别成 `【`/`[`，也反证两帧栅格并非逐像素一致。
- 当前日志没有打印 branch baseline fingerprint，也没有保存这两张 task-box 原图，所以已经无法从这次运行恢复
  具体翻转了哪些 bit 或计算准确 Hamming distance；只能从 `equals` 分支和相同 OCR 正文证明“语义未变、strict
  fingerprint 已变”。修复时必须补 baseline/current/distance 与原图证据，不能再只打印最终布尔裁决。
- 该顺序违反 `docs/天庭任务流程大MD.md` 第 162-177 行：点击坐标、移动、停下后必须先进入外层 `Alt+A` 并点击
  妖王幻影；只有完成战斗处理后才允许用 tracker 变化判断小任务是否结束。
- 修复边界（用户已批准并实施）：
  1. `fengyaofuAwaitingIndex >= 0` 时，Runner 的当前 intent 已给出 `STOPPED_AWAY` 后必须先消费到达并执行直战，
     tracker 指纹不得抢先结束分支。
  2. 后续需要判断任务框变化时，不再用 exact string equality 或 OCR 关键词代表变化。复用同一 prepared tracker
     frame 裁出的完整 task box，保持原分辨率与原 RGB，不洗背景、不洗色、不缩放，直接计算归一化相关系数；
     `score >= 0.80` 为 `UNCHANGED`，低于 `0.80` 才是 `CHANGED`。无法读取、格式错误或尺寸不一致为
     `INDETERMINATE`，保持 fail-closed。
  3. fresh runtime 验收必须看到 `fengyao-coord:0 -> STOPPED_AWAY -> Alt+U/Alt+A -> 妖王幻影点击`；在首次直战
     尝试之前不得出现 `tracker-green-click:reclick`。
- 本轮新增的 turn action evidence 已经成功补足“具体执行了什么”的证据；任务框比较现已补齐
  `score/threshold/dimensions/reason` 和 baseline/current 短 hash，关闭原 E18 观测缺口。

### 2026-08-04 原图比较实施结果

- `TrackerTaskBoxContentComparator` 新增完整 RGB 快照与 normalized correlation；`TaskTrackerPanelService` 从已捕获的
  同一 task-box ROI 直接生成快照，未增加任何 HWND capture、网络 OCR 或后台图片预处理。
- 既有 20 张天庭 task-box 原图实验：21 组同内容最低 `0.8993`，169 组不同内容最高 `0.5524`；用户最终选择
  `0.80`，当前 190 组全部正确分离。联系人图为
  `images/temp/analysis-taskbox-raw-contact-sheet.png`。
- `runFengyaofu` 已把 pending coordinate 分支移动到任务框比较之前；到点后先执行直战。天庭小任务计数、
  暗雷战后连续性与三分钟 task-box watchdog 也统一使用同一 `0.80` 原图判据，避免另留 strict-equals 真理源。
- 比较日志输出 `result/score/threshold/dimensions/reason`，baseline/current 只输出尺寸、字节数与短 hash，禁止把
  完整 RGB payload 打进日志。
- Cloud 编译门：`mvn -q -DskipTests=false "-Dmaven.test.skip=false" compile`，exit `0`。未启动 runtime、
  application、UI、capture 或 input；fresh runtime 仍按上面的第 3 条验收。

## 2026-08-04 06:54 运行故障：封妖符首坐标延迟约 44 秒，下坐骑后协议校验终止

- 现场窗口 `hwnd-F99187E`，任务运行 `remote-turn-9cbe0650-497f-48ba-904b-2b6efe67e44f`。
  `06:55:42.694` 客户端已本地命中并点击 `fengyao.png`；封妖符坐标框在 `06:55:51.132`、
  `06:56:08.040`、`06:56:14.274` 的完整 dialog ROI 中持续可见，但第 1 个坐标直到
  `06:56:26.776` 才物理点击。输入队列本身只耗时 `2340ms`，不是排队 30 多秒。
- 延迟由三段串联产生：
  1. `fengyao.png` 已点击后，Cloud 仍因旧绿链 intent 为 `pathingActive=true` 进入 `PARK_PATHING`，完整等待
     `25s` slice；旧 intent 的 `STOPPED_AWAY` 到 `06:56:00.754` 才在 Client 分类，Cloud 到
     `06:56:08.479` 才消费并清理。
  2. 封妖符 branch 进入时没有已钉住的 task-box baseline，之后又等待 `10784ms`，直到
     `06:56:19.316` 才出现 `PREPARED_ACTION_READY` 并记录 `task-box baseline pinned late`。
  3. anchor 截图 action 从 `06:56:19.441` 到 `06:56:24.308` 又耗时约 `4.87s`，随后坐标输入耗时
     `2.34s`。从 `fengyao.png` 点击到坐标点击总计约 `44.08s`。
- **P1：成功点击 `fengyao.png` 已经是旧绿链到达并打开对话框的更强事实，不能继续等同一旧 intent 的
  `PATHING_TERMINAL`；否则固定 `25s park` 会直接进入关键路径。封妖符 baseline 也不得在坐标框已经出现后
  “late pin”再立即与自己比较。修复需在专属答复成功时终结旧 intent，并从进入分支时已有的 tracker frame
  钉住 baseline；不得新增截图或延迟。**
- 到点后的直战前置检查本身成功：`06:57:05.641/07.573` 两次 `Alt+U`，确认 `FLYING` 后
  `06:57:07.941` 执行 `Alt+C` 下坐骑。但随后 `NpcClickService.tryDirectCombatTargetClick` 调用
  `registerExpectedCombatEnterClaim` 时，双仓 `TurnProtocolValidator` 的
  `WHOLE_TASK_EXPECTED_COMBAT_ENTER_CLAIM` 白名单没有 `TIANTING`，立即抛出
  `IllegalArgumentException`。因此没有后续 `Alt+A`/妖王幻影点击；Client 到 `06:58:08.732` 才收到
  FAILED 终态，是远端失败传递又拖了约 60 秒。
- **P1：把 `TIANTING` 纳入该 operation 的双仓 byte-identical 协议合同，并补直战 claim 的合同门；不能仅在
  `CloudFastExpectedCombatExitCoordinator.authorizedTask` 放行，因为本次证明协议校验仍会在下一层拒绝。**
- 启动摄妖香结论：本轮启动仅执行 `startup first-aid check`，随后 `startup resume: found=true` 直接进入已存在的
  天庭 tracker。没有包裹 action、没有摄妖香日志，队长 `bag_scan*.png` 时间仍停在 `05:08/05:46`。
  当前实现只在真实脱战后的 `FULL_RECOVERY_WITH_LEADER_INCENSE` 检查摄妖香；是否把队长摄妖香检查加入天庭
  hot-start 是独立业务修改，须经用户确认。
- Fresh gate：同一轮必须看到 `fengyao.png clicked -> 立即进入坐标 anchor/点击（不出现 25s PARK） ->
  STOPPED_AWAY -> flying 判定/必要时 Alt+C -> expected-combat claim accepted -> Alt+A/妖王幻影点击 -> IN_COMBAT`；
  启动摄妖香若获批准，则在第一次 tracker 绿链点击前必须出现一次队长摄妖香检查证据。

### 2026-08-04 用户批准的修复范围

#### 封妖符坐标到达后的直战坐骑策略

- 用户最新确认：封妖符妖王幻影只允许可见 tooltip，不读取或写入记忆，也不开放黄字、人物锚点公式或 Ctrl 菜单；坐标到达后
  不需要检查坐骑，也不允许为了直战执行 `Alt+C`。
- 正确顺序固定为：新坐标 intent 到达并收到 `PATHING_TERMINAL` -> 直接登记 expected-combat -> `Alt+A` ->
  tooltip 点击妖王幻影。天庭暗雷的飞行确认、必要时下坐骑和巡逻流程不受影响。
- 当前二参 `tryDirectCombatTargetClick(request, true)` 的 `true` 只允许 `UNKNOWN` 状态继续，确认
  `FLYING` 仍会下坐骑；实现必须把该参数改成显式跳过整段飞行探测，而默认入口保持原行为。

##### 实施结果

- 已把二参布尔语义改为 `skipFlyingCheck`。封妖符传 `true` 后，不再执行 `Alt+U` 截图判定，也不会执行
  `Alt+C`；随后仍按原顺序登记 expected-combat、进入 `Alt+A` 并走既有点击管线。
- 一参默认入口仍调用二参 `false`；生产代码中只有天庭封妖符使用 `true`，因此修罗、五倍及其他直战调用方的
  坐骑前置没有改变。天庭暗雷分支未进入该入口，也未修改。
- Cloud production compile 通过。全仓 Maven 定向测试被本卡外旧测试夹具的构造器/协议枚举漂移阻断；本卡新增
  合同已单独 `javac` 编译并通过 JShell 执行，输出 `SINGLE_CONTRACT_TEST_PASSED`。
- Fresh runtime 验收更新为：`STOPPED_AWAY -> expected-combat -> Alt+A -> tooltip -> IN_COMBAT`；该链中
  不得出现封妖符直战的 `Alt+U` 或 `Alt+C`。

- 状态：`APPROVED FOR IMPLEMENTATION`。本次仍属于 G005，不另建卡；由 worker 实施，父级 Codex 独立 review。
- 修复 1（封妖符首坐标关键路径）：Client 成功本地点击 `fengyao.png` 后，该事实已经证明旧 tracker 绿链已到达并打开
  专属对话框。Cloud 必须立即结束/清理该旧绿链 intent，禁止继续进入旧 intent 的 `25s PARK_PATHING`。封妖符
  task-box baseline 必须复用进入分支时已有的 tracker frame；baseline 缺失不得阻塞坐标遍历，不得在坐标框出现后再
  等待约 `10s` 做 late pin。复用既有 anchor/坐标点击算法、ROI、模板、阈值与原子输入，不新增截图延迟，不修改点击位置。
- 修复 2（直战 claim 协议）：双仓 byte-identical 的 `TurnProtocolValidator` 必须把 `TIANTING` 加入
  `WHOLE_TASK_EXPECTED_COMBAT_ENTER_CLAIM` 白名单，并补双仓协议/链路合同，证明下坐骑后能够登记 claim 并继续既有
  `Alt+A`/妖王幻影直战入口。
- 修复 3（启动摄妖香）：天庭 fresh start 与 hot start 都必须在队长第一次 tracker 绿链点击前执行一次既有队长摄妖香
  检查/维护。只允许队长执行，只复用现有摄妖香识别与维护服务；不得让队员开包，不得加入回程道具预扫描，不得在每次
  循环重复执行。
- 代码验收：定向合同测试和两仓适用 Maven 编译门通过；协议镜像保持 byte-identical；不运行 runtime/application/UI/
  capture/input 测试。fresh runtime 最终仍以本节上一段完整时间线为准，并额外要求启动日志在第一条 tracker click 前
  出现一次 leader incense check。

### 父级首轮 review（2026-08-04）

- 结论：`REPAIR / P1=2`。两仓生产 `compile` 已由 worker 报告通过；validator SHA-256 已由父级核为
  `CDFF1D120B3E185EE4A9DC1816784AFA9BA998DC892D104A982718225C509F26`，镜像一致。
- **P1-1：** `consumeWake` 已调用 `releasePathingIntent(..., true)`，但忽略清理返回值。Client clear 若失败，代码仍进入
  封妖符分支，下一轮旧 `pathingActive` 仍可再次选中 `PARK_PATHING`。返修必须把“清理成功”设为进入分支的强条件；
  失败只能立即有界重试或 fail-fast/回接任务，禁止重新进入 25 秒等待，也禁止只清 Cloud mirror 伪造成功。
- **P1-2：** 两仓协议已放行 `TIANTING`，但没有新增专属正反例合同证明
  `WHOLE_TASK_EXPECTED_COMBAT_ENTER_CLAIM` 对 `TIANTING` 接受、对无关 task 拒绝。返修须补 byte-identical 合同；若全仓
  testCompile 被旧写集外夹具阻断，须记录精确类/行并用既有隔离技法运行本次新增测试。

### 父级终审（2026-08-04）

- 结论：`SOURCE/CONTRACT REVIEW PASSED / P0/P1/P2=0/0/0 / FRESH RUNTIME REQUIRED`。
- 首轮两个 P1 已闭合：`releasePathingIntent(..., true)` 的 Client clear 返回值成为进入封妖符分支的强条件；失败时在
  `readFacts` 与 `PARK_PATHING` 之前 fail-fast，不会再把旧腿带回 25 秒等待。双仓新增 byte-identical 的 claim 正反例
  合同，证明 `TIANTING` 被接受、无关 task 被拒绝。
- 终审追加发现并返修：第一版在 baseline 已存在时仍会在第一坐标前执行当前 task-box 抓取/比较。现已改为第一坐标
  **无条件**跳过 `captureTaskBoxSnapshot/compareRaw`，直接走既有 anchor 与坐标点击；baseline 只从第二坐标开始用于
  `0.8` 整框推进判断，后续 INDETERMINATE 仍按原有有界 fail-closed 处理。未改模板、ROI、阈值、坐标或原子输入。
- 启动摄妖香已接在 `performStartupFirstAidCheck` 之后、`startupTrackerShowsQuest` 和任意 tracker click 之前；复用
  `PlayerStateService.ensureSheYaoXiangActiveForLeaderTask` 的 support-member gate，并以 task-run guard 保证 fresh/hot
  start 整次运行仅一次。
- 两仓 `TurnProtocolValidator.java` SHA-256 均为
  `CDFF1D120B3E185EE4A9DC1816784AFA9BA998DC892D104A982718225C509F26`；两仓新增合同 SHA-256 均为
  `DB7615B3C7E0410E7E607D05F195A557BB2678B3038B15165A8C32F26CB8FB9E`。父级重跑 Client/Cloud production compile
  均 exit `0`；worker 隔离合同 Cloud `9/9`、Client validator `2/2`，终审返修后的封妖符合同 `7/7`。
- Maven 定向 test 入口被本卡外 testCompile 欠账阻断，未误报为绿：Client 是旧测试仍引用已删除的
  `LocalPathingStartProofMechanics`；Cloud 包含旧构造器/枚举测试（首批为 `CloudTurnTaskFactoryAllowlistTest`、
  `TurnProtocolValidatorContractTest` 等）。未修改这些写集外文件，也未运行 runtime/application/UI/capture/input。
- Fresh runtime 仍按前述完整链验收：启动摄妖香早于首绿链且仅一次；`fengyao.png clicked` 后无旧腿 25 秒 park、无首
  坐标 task-box capture/late pin，直接 anchor/第 1 坐标；随后 `STOPPED_AWAY -> claim accepted -> Alt+A/妖王幻影
  -> IN_COMBAT`。封妖符该链不得执行 `Alt+U` 或 `Alt+C`；暗雷的坐骑流程不受影响。

### 2026-08-06 G029 坐标与目标结果门返修

- `ACTION_FENGYAO` 建立分支后，只要当前既不在战斗、也没有有效 pathing，封妖符分支必须先于 stopped/普通
  Tracker 决策取得窗口 ownership；只有 `fengyaofu.png` anchor 缺失时才允许点一次绿链重开坐标框。
- 坐标点击沿用 G014 的 exact intent 真实移动证明。仅登记 intent 或输入 `dispatched` 都不算起步；Runner 未写入
  该 intent 的 `movementObservedAtMs` 时，不写已访问坐标，清掉无移动 intent，只检测并 Fast Click STORY，然后
  重试同一坐标。
- 妖王幻影直战使用 `TOOLTIP_ONLY`。点击发出后保留 pending 坐标：Runner 真实进入并退出战斗，或未入战 STORY
  被物理点击实际清除，二者之一成立后才允许比较完整 task box。STORY 尚未出现、检测不明或点击未提交时继续等待；
  不得用 `dispatched=true`、画面时间差或下一帧布局变化替代业务结果。
- 用户要求本轮只完成源码与文档返修，保持五窗 STOPPED，不运行 Maven、合同、runtime、application、UI、capture
  或 input。后续编译与 fresh 必须另获明确授权。
