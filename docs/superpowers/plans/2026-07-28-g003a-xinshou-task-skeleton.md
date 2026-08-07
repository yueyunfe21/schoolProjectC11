# G003a 新手任务：骨架 + 主循环 + title 节点（外部 worker 实施卡）

> **2026-07-29 权威合同冻结：**Runner 首发画面变化，Cloud 可经 `FrameDemand` 继续向 Client
> 索取阶段证据；普通事实和取证过程不唤醒主任务。只有 Cloud 写入唯一 `PreparedAction` 并发布
> `PREPARED_ACTION_READY` 才能唤醒。Cloud task 消费 action，Client input queue 只消费
> command。Dialog 优先于 Tracker，Client title 自主业务宏必须移除。业务流程仍以草案正文为准，
> 并保留 `lingyang_title.png` 的 task-owned `30s` 绿链单击锁。详细合同见草案 §1.1.2、§3.1。

> **2026-07-29 fresh runtime Repair：`SOURCE REPAIR IMPLEMENTED /
> FOCUSED TEST 93-93 / CLOUD RESTART REQUIRED / FRESH RUNTIME REQUIRED`。**
> `19:26` 运行中 Client 已上报 exact `STOPPED_AWAY`，Cloud 也已发布下一条 Tracker
> PreparedAction；前台却因终态仍携带 intent，把 `STOPPED_AWAY` 错当成 active 并永久留槽。
> 修复只把新手 Tracker 阻断条件收窄为精确 `WindowPathingState.ACTIVE`；
> `ARRIVED/STOPPED_AWAY/NONE` 允许消费。全局 pathing 模型及其他任务未改。
> Cloud 前台消费 + Observer named family `93/93`，main/test compile exit `0`。

> **2026-07-29 父级 Review #4：`SOURCE+TEST REVIEW PASSED /
> P0-P1-P2=0-0-0 / FRESH RUNTIME READY`。**
> Repair #4 只扩展既有 `CloudWindowObservationInbox` exact-current 原子提交、既有唯一
> `CloudDialogPreparedActionState` slot 的 run-owner terminal tombstone，以及对应生产合同；
> 未新增 coordinator、第二 queue/state machine、线程、timer 或刷新循环。Tracker 与 Dialog
> 决策现在在同一 Inbox monitor 下复核 exact current Dialog presence/ROI、Tracker ROI、
> Pathing、interest revision、taskRun/window/HWND；Dialog clear/block/publish 与非 Dialog
> publish 同次线性化；terminal 后同 run/HWND、旧 generation、旧 HWND 的晚到 retry 均被拒绝，
> 新 claim 可开启新 generation。前台 `XinshouTask` 仅监听 `PREPARED_ACTION_READY`，按事件
> exact `actionId` CAS 消费唯一 slot。父级复跑 Cloud `185/185`、Client `68/68`，均
> `0 failures / 0 errors / 0 skipped`；双仓 compile 与 `git diff --check` 均 exit `0`。
> 新 Cloud 并发合同测试已从 `.gitignore` 精确反忽略。未运行 runtime/UI/capture/input；
> 下一门禁是重启 Client/Cloud 后的用户 fresh run。

> **2026-07-29 父级 Review #3：`REPAIR REQUIRED / P0-P1-P2=0-3-1`。**
> 当前单线架构、Client `68/68`、Cloud `179/179` 和双仓 compile 均已通过，但并发终审仍发现
> 三个 P1：① Tracker 原子提交只复核 Tracker ROI，未把最新 Dialog/Pathing 事实纳入同一
> Inbox 提交栅栏；② Dialog 的旧分类仍可能在事实替换后晚到并 block/publish；③ terminal
> clear 后同一 run/HWND 的迟到 retry 仍可重新 publish。P2：新增
> `CloudXinshouPreparedActionObserverContractTest` 当前被 Cloud `.gitignore` 的 `src/test/`
> 规则排除，干净检出不可复现。返修只能扩展现有 Inbox 原子提交、现有 PreparedAction owner
> 墓碑和现有测试跟踪；不得新增 coordinator、第二 queue/state machine、线程、timer 或刷新循环。
> 修复并由父级复审到 `0/0/0` 前仍不得 fresh run。

> **2026-07-29 父级 Review #2：`REPAIR REQUIRED / P0-P1-P2=0-4-1`。**
> 单线架构已经落地，但 source review 仍发现五个具体缺口：领养 `30s` 释放仍被 `5s` ROI
> stale gate 阻断；active-green 最长一小时 park 不能及时进入 pause checkpoint；恢复清屏后仍可能
> 消费暂停前 action；未解析 Dialog 仍可能放行机械动作；interest revision 拒收的 ROI 仍会被
> Client 当作已 ACK。返修只允许修改现有 Observer、主循环、observation ACK/暂停采样和对应测试，
> 禁止新增 coordinator、queue、manager、线程、timer 或第二状态机。修复并复审到 `0/0/0`
> 前不得称 runtime-ready。

> 依据 `docs/新手任务流程草案.md`（业务权威，先通读）+ 用户决议（本卡为准）。草案两处按实物修正：§2.2 锚点=`esc_bot.png`（`mouse.png` 已作废，图保留不用）；确认按钮=`quedingguan_.png`（按实际文件名）。
> 拖拽按住会话与战斗策略覆盖**不在本卡**（G003b）。

## 0. 新工人须知（必读）

- **先读 `docs/云端迁移常见错误清单.md`**（十类反复踩的事实链断裂错误 + 提交前自查清单）。本卡实施过程中已多次踩中 E1/E2/E5/E9。

- 仓库：客户端=`D:\mavenProject\DHXY-cr271`（瘦客户端：捕获/输入/本地感知），云端=`D:\mavenProject\dhxy-cloud-brain`（业务决策）。`D:\mavenProject\DHXY` 是用户基线，**严禁改动**。
- 架构：云端任务类经 turn 协议下发动作；本地 runner 做感知（模板匹配/寻路事实/战斗信号）并经观察通道（`/api/v1/client/observation`）上报事实与事件；云端 park 等 ready 事件唤醒。参考同构实现：`XiuluoTaskV2`、`FiveRingTaskV2`（cloud）、`WindowObservationSampler`（client 感知）。
- 共享协议文件（`com/bot/dhxy/cloud/turn/protocol/**` 含 observation）**双仓必须 byte-identical**，改一处必须同步另一处。
- 编译：client `mvn -q compile`；cloud `mvn -q compile -DskipTests=false`（enforcer 要求该参数）。全树 testCompile 可能被写集外脏测试阻断：用 junit-platform-console-standalone 隔离编译运行本卡新增测试类（项目惯用技法）。
- **等待契约铁律**：park 只等事件；每个可能结局必有唤醒事件；一律有界（≤25s 循环再挂），禁止 `timeoutMs=-1` 裸挂。

## 1. 架构决议（用户拍板，不得偏离）

1. 25 张 `images/template/xinshou/` 模板**全部留在客户端本地**匹配（感知锚点范式，同看打探针）；不拷贝到 cloud 资源。
2. dialog 处理：本地只报"有 dialog"，云端算点击点经 prepared-action 通道发布，事件唤醒后执行（管线现成，参考 kanda/五环 accept 流）。
3. 绿链点击**零同步验证**：点击时记录（链接序号+当时游戏坐标），立即让出窗口；推进判定交给后台事实，下次唤醒时分流。

## 2. Work Packages（按序实施，各自有 DoD）

### WP1 任务注册骨架 + XinshouGeometry

- cloud：新 `TurnTaskCode.XINSHOU` + `CloudTurnTaskRegistry` 注册 + `XinshouTask` 类（暂只跑空主循环）；client：`TaskType` 枚举加 XINSHOU（对照 WUHUAN_V2 的两仓接线抄全）。
- cloud 新 `XinshouGeometry` 常量类：草案所有屏幕坐标按 `rel = abs - (991,369)` 换算为窗口相对值，原绝对值写注释。本卡涉及的换算结果（已算好，直接用）：
  - ESC ROI `(870,57)-(998,92)`；esc_bot ROI `(549,667)-(571,683)`；queding/confirm ROI `(569,365)-(729,497)`；
  - 领养 ROI `(469,592)-(572,621)`；寻人固定点 `(312,323)`；
  - 修复三件 `(491,604)`,`(542,595)`,`(595,581)`；三槽位 `(310,470)`,`(404,518)`,`(455,521)`；关窗点 `(672,220)`。
  - 游戏地图坐标（如 `(23,201)`）不换算。
- 任务参数：总有效时长 1h，暂停补偿（复用 `TaskPauseToken` pauseBlockedMs 记账）；恢复后先 `closeAllGenericWindows` 再重进主循环；模板阈值默认 0.85。
- DoD：双仓编译过；XINSHOU 可从 UI 启动并空转+正常停止。

### WP2 本地感知探针 + 事实通道

新观察兴趣 `xinshou-anchor`（geometry-free，采样周期 1s）。cloud `XinshouTask` 经 `CloudWholeTaskObserver.publishObservationInterests`（对照 COMBAT_SIGNAL 的发布方式，仅 XINSHOU 任务发布）。client `WindowObservationSampler` 新增 xinshou 探针 duty（对照 `LocalCombatSignalMechanics` 消费兴趣的方式，**从 G002 共享周期帧裁剪**，不得新增独立截图）：

- 匹配集 A（title，每周期）：`zhua_title/shanhuhaidao/guiqi/xunren/lunhui_title/jiao_wuzi_title/xiufu_title` 于 Tracker title 区（ROI 沿用 TaskTrackerPanelService 的 tracker 面板区域）；
- 匹配集 B（锚点）：`ESC.png` 于其 ROI、`esc_bot.png` 于其 ROI。`ESC.png` 保持原规则：**仅当 Tracker 不可见**时才探测/上报；只有 `esc_bot.png` 可与 Tracker 同时出现，必须每个周期独立探测/上报，不得被 Tracker 可见性过滤。
- 匹配集 C：`lingyang.png` 于其 ROI；
- 命中以 `ObservationFact` 上报：新 `ObservationFactType.XINSHOU_ANCHOR`，wireValue=模板名。协议改动=1 个 fact type + 1 个 interest key，`ObservationProtocolValidator` 双仓同步放行（照抄 COMBAT_SIGNAL 的校验形状）。
- cloud 侧 `CloudWholeTaskObserver` 把 XINSHOU_ANCHOR 事实写入任务可读的镜像（对照 combat-signal 镜像的消费方式），title 变化时发 ready 事件唤醒任务。
- DoD：隔离测试：探针在含/不含模板的合成帧上产出正确事实；validator 双仓 byte-identical。

### WP3 主循环 + 绿链状态机

`XinshouTask` 主循环（草案 §1.1 优先级）：

1. 有 option dialog（prepared-action 就绪）→ 点第一个选项；
2. 已停止+无 option+有绿链 → 读 Tracker 绿链（复用 cloud `TaskTrackerPanelService` 绿链读取），从上到下选择；点击后记录（序号,点击时游戏坐标）并 **MUST_YIELD 让出**；
3. 移动中 → park；
4. `ESC.png` 保持“无 Tracker”分支：无 Tracker 且 ESC 命中时按一次 ESC。`esc_bot.png` 是独立状态，可与 Tracker 共存；命中→按 ESC→若 dialog 出现，在 queding ROI 匹配 `quedingguan_.png` 点一次→继续匹配 `confirm.png` 点一次→循环至 `esc_bot` 消失。Tracker 同时存在时，不能因为其存在而压掉 `esc_bot` 分支。

park 唤醒集合（全部现成事件）：`PATHING_TERMINAL`（STOPPED_AWAY/ARRIVED，事实带坐标）、`PREPARED_ACTION_READY`、`COMBAT_STATE_CHANGED`、WP2 title 变化事件；有界 25s。

唤醒后分流（绿链状态机）：

- STOPPED_AWAY 且坐标==点击时坐标 → 该链接无效：多链接点下一条，单链接重试本条；
- STOPPED_AWAY 且坐标≠点击时坐标、无 dialog → 重点上一次有效链接；
- dialog → 优先级 1 处理；
- 进战 → 本卡内 park 等退战（策略覆盖在 G003b）。

推进成功判定=**整 Tracker 面板 hash 变化**（复用 TaskTrackerPanelService 16×16 二值指纹缓存），不比单条绿字。title 一次性消费表：任务内 consumed set，已消费 title 不再进专属处理。

- DoD：模拟事实序列的合同测试覆盖状态机四分支+面板 hash 推进判定。

### WP4 title 节点 + UICleaner 扩展

| 节点 | 实施要点（全部拼装现成组件）|
|---|---|
| 领养 | WP2 集 C 事实命中→turn 点击：先点匹配点正上方 200px，再点模板位置，等 500ms 回主循环 |
| 珊瑚海岛（一次性）| BagService 独占宏：开包裹 tab index=0（是 tab 不是格子）→匹配 `shengji.png` 右键一次→`closeAllGenericWindows` |
| 鬼泣 | 同上开包裹→`hailuo.png` 右键→在 DialogService 既有 dialog ROI 内匹配 `chuixiang.png` 左键（不做 dialog 识别）|
| 寻人 | NavigationService 至地图 `(23,201)` →NpcClickService 点"渔村村长"（黄名，禁紫名），严格顺序：①记忆点击②固定点 `(312,323)` ③黄字名识别；Tracker 消失=成功 |
| 交付物资 | option 后给予框内匹配 `wuzi.png` 左键选中→复用五环给鞋"给予"按钮定位点击（GiveItemService）→给予钮消失或面板 hash 变化=完成 |
| 修复（§10 全文照做）| 开包裹 `xiufu_item.png` 右键→`xiufu_opened.png` 未见则重试→三件×三槽位（坐标见 WP1）、每次放入后匹配 `xiufu_fangru_suc.png`=该件成功；三槽全失败对当前件 fallback 重选再试；三件毕匹配 `xiufu_alldone.png` 或等 title 变化（10s 未变整体重做）；title 变化后 1s 点 `(672,220)` 关窗 |

- UICleaner 扩展（client）：`closeAllGenericWindows()` 改为扫描 `images/template/cancel/` 目录全部模板（现仅 x1/x2/x3/npc_busy_cancel，补 x/x4/x5/x6/x7），无固定顺序。**影响所有任务，需单独跑一轮既有任务回归。**
- DoD：每节点合同测试（模拟事实→动作序列断言）；UICleaner 扩展有独立测试+回归说明。

## 3. 验收

1. 双仓编译过、共享协议文件 byte-identical、新增测试隔离运行全绿；
2. 真机：XINSHOU 启动后完成草案 §3/§5/§6/§7/§9/§10 各节点至少一次（§4/§8 依赖 G003b，遇战斗 park 等退战即可）；
3. 主循环空转期间客户端无新增独立截图（观察全走 G002 共享帧）；
4. 暂停/恢复：恢复后关窗重进主循环，1h 预算扣除暂停时长。

## Status

- 2026-07-29 无推进重证闭环：无 active pathing 时，Client Runner 每 tick 比较本地相邻
  Dialog/Tracker/title 事实；变化立即上传，连续 `10s` 无有效推进则复用同一 G002 共享帧，
  在一个 observation request 中重发当前 Dialog/Tracker，并携带
  `XINSHOU_NO_PROGRESS_REFRESH=no-progress`。Cloud 只在该标记与当前 ROI exact
  `observerSeq` 一致时允许同 hash 重新决策；普通重复 hash 仍去重。Cloud 的单动作优先级
  锁死为可执行/未解析 Dialog 优先，`STORY`/absent 后才处理 Tracker；Client 不做业务判断。
  父级终审 `P0/P1/P2=0/0/0`，Cloud focused `59/59`、Client focused `59/59`、
  双仓 compile、协议 SHA 与 `git diff --check` 通过。既存
  `TurnCoreProtocolGoldenJsonTest` 因旧快照缺 9 个历史 local operation 有 `2/7` 失败，
  不属于本卡。状态：`SOURCE+FOCUSED TEST REVIEW PASSED / FRESH RUNTIME READY`。

- 2026-07-29 `19:26` fresh runtime gate failed，已实施窄修复：Client 在
  `19:27:06.540` 已上报 exact `STOPPED_AWAY`，Cloud 也已生成 sequence `5` 的下一条
  Tracker PreparedAction；卡死发生在 `XinshouTask.consumeOnePreparedAction(...)`，
  其 `ACTIVE || hasActiveIntent()` 闸门把仍携带 intent 的 `STOPPED_AWAY` 错当成 active。
  现仅精确 `WindowPathingState.ACTIVE` 阻断 Tracker；`ARRIVED/STOPPED_AWAY/NONE`
  允许消费。不改全局 `hasActiveIntent()`，不增加新层。新增终态携带 intent 的生产合同，
  Cloud focused `93/93`、main/test compile exit `0`。状态：
  `SOURCE REPAIR IMPLEMENTED / CLOUD RESTART REQUIRED / FRESH RUNTIME REQUIRED`。

- 2026-07-28 绿链职责 owner 定稿（覆盖旧 WP3 单条状态机描述）：新手 Cloud 只调用 `findClickableTrackerGreenLinkBands(...)`，从一张 tracker panel 图中选择**唯一**带下划线的真实导航绿链；普通绿色文字和绿链数量不进入任务逻辑。`XINSHOU_TRACKER_LINK_CHAIN` 仅携带这一点；exact-window Client Runner 对同一点原子 `MOVE -> 80ms -> CLICK`，每次最多等待 `2500ms` 的新 dialog 或真实地图坐标变化，未生效立即本地重试，最多 3 次。单次失败不上传、不重读 tracker、不以 `STOPPED_AWAY` 重置序号；Cloud 只接收整个本地重试结束的机械结果。实机 gate：无 dialog 且未移动时，日志应直接显示同一点本地 `attempt=2`，中间不得出现第二次 cloud tracker read 或 `STOPPED_AWAY` 驱动的 cloud decision。

- 2026-07-28 建卡（实施级，外部 worker 可独立承接）。
- 2026-07-28 Codex 认领，状态：`IN PROGRESS`。WP1/WP2 已落地并通过两仓编译：`XINSHOU` 已注册；客户端 `xinshou-anchor` 每秒仅消费 G002 共享帧并上报 `XINSHOU_ANCHOR`；云端保存任务内镜像、标题变化发布 `XINSHOU_TITLE_CHANGED`，主循环以有界 25 秒 event park 消费该事件。共享 `TurnTaskCode` 与 `ObservationFactType` 已核对 byte-identical。
- 2026-07-28 业务修正：只有 `esc_bot.png` 可与 Tracker 共存，后续 WP3 不得由 Tracker 是否可见过滤它；`ESC.png` 仍保持“Tracker 不可见才探测/上报”的原前提。
- 2026-07-28 WP2 review repair：修复 P1“title 命中吞掉 `esc_bot`”。此前即使本地同帧识别到两者，观察 inbox 也会按 `factType` latest-wins 覆盖其中之一。现 `XINSHOU_ANCHOR` 每周期显式上报 primary 模板名或 `absent`，新增独立 `XINSHOU_ESC_BOT` 每周期上报 `present/absent`；两端镜像分别保存，`esc_bot` 不再被 title/Tracker 吞掉或因旧命中滞留。title/领养/ESC 保留单一 primary 优先级；`ESC.png` 仍只在无 title primary 时探测。已补 `XinshouAnchorLocalMechanicsTest`：同帧 `xunren.png + esc_bot.png` 断言两条本地锚点，纯空帧断言零锚点。title 全 miss 仅作为本新手流程的轻量“Tracker 不可见”近似，已在代码注释明确。
- 2026-07-28 WP2 DoD closed：隔离启动器 `junit-platform-console-standalone-1.10.2.jar` 已只选择执行 `com.bot.dhxy.window.observation.XinshouAnchorLocalMechanicsTest`，结果 `2 tests successful / 0 failed`。命令先以 Maven `dependency:build-classpath` 生成 test-scope classpath，再用 `java -jar ... execute --select-class ...` 运行；未启动 runtime/UI/capture/input。WP2 的探针合成帧测试与双仓编译门均已闭环，可放行 WP3。
- 2026-07-28 WP3 绿链主链完成：`XinshouTask` 已按 prepared option → combat park → `esc_bot` → pathing park → Tracker 绿链 → 无 Tracker ESC 的优先顺序消费事实。Cloud 同一面板帧同时生成绿链和 16×16 二值指纹；绿链点击是原子的 `MOVE -> 80ms -> CLICK`，成功后登记当前点击时地图坐标与 pathing intent，并立刻进入 `awaitingPathingTerminal`。该状态只等待本地 `PATHING_TERMINAL`，不会因为尚未来得及回报的 `NONE` 快照再次点同一条绿链。终态分流为：同坐标多链接换下一条、同坐标单链接重试、异坐标重试上次有效链接、面板 hash 改变重置到第 0 条。真实输入只发生在带 `xinshou:` source 的 prepared option 或经 Tracker replay 验证的绿链上。
- 2026-07-28 WP3 验证：新增 `src/test/resources/images/test-cases/xinshou/raw_tracker_green_links.png`（从已存在的 Tracker 原始帧归档）及 `XinshouTrackerPanelReplayTest`。回放产生 `D:\mavenProject\DHXY-cr271\images\test-cases\xinshou\marked_tracker_green_links_click.png`：红框为唯一可点击绿链、红点为真实下发点击中心。`junit-platform-console-standalone` 隔离运行 `XinshouTrackerStateMachineTest + XinshouTrackerPanelReplayTest` 结果 `6 tests successful / 0 failed`；Cloud `mvn -q -DskipTests=false compile` 通过。
- 2026-07-28 WP3 当前窗口反例补充：现场整窗图包含两个绿色 `南瓜`，只有下方候选带连续下划线。
  通用主入口 `ImageAlgorithms.findClickableTrackerGreenLinkBands(...)` 定义为“绿色候选末四行存在覆盖
  候选宽度至少 60%、且不少于 10px 的连续绿色线”，供后续 Tracker 导航绿链统一复用；
  普通绿色强调文字不进入点击链。规则只用于 `xinshou`，不改修罗/五倍/五环绿链算法。
  输入 `D:\mavenProject\DHXY-cr271\src\test\resources\images\test-cases\xinshou\raw_tracker_underlined_green_link.jpg`，
  输出 `D:\mavenProject\DHXY-cr271\images\test-cases\xinshou\marked_tracker_underlined_green_link.png`
  （橙框拒绝、绿框有效、红点点击）。隔离 `XinshouTrackerPanelReplayTest`=`1/1`，Cloud main compile
  exit `0`；全仓 testCompile 的旧 Observer/Navigation 失配不属于本项。
- 2026-07-28 23:46 fresh runtime finding，`P1 REPAIR REQUIRED`：Cloud 已解析并下发唯一绿链，
  但 Client 四次均在 `XinshouTrackerLinkChainLocalOperationExecutor.execute(...)` 首击前被
  `hasRecentDialogFrameObservation(3000)` 压制。该缓存由本地 `(200,250 640x300)` 结构探测在
  无 dialog 画面持续误报并刷新，日志同时证明 `intentId=null`；因此不是绿链坐标或下划线算法失败。
  修复边界：首击前不得使用旧/无因果关系的结构帧阻止正确绿链；只有点击时间之后的新移动或新 dialog
  才能终止本地重试。须把该现场帧加入 dialog negative replay，验证首击必执行、真实 dialog 仍能阻止
  后续重试。Cloud 的 `STORY_IGNORED` 误判另记诊断噪声，不是本轮零点击直接原因。
- 2026-07-28 本地模板恢复边界修正：`esc_bot`、`ESC.png`、`quedingguan_.png` 与 `confirm.png` 都是确定性本地模板动作，不交给 Cloud 下发。Client `XinshouLocalRecoveryHandler` 在同一 G002 共享帧命中后直接执行：`ESC` 通过精确 HWND 的后台 `PostMessage`，确认模板通过当前窗口输入队列原子 `MOVE -> CLICK`；同一可见锚点只提交一次，直到本地帧证明其消失。Cloud 只接收 `XINSHOU_RECOVERY_STATUS=<idle|submitted|waiting-local-change|input-failed>` 作为唤醒/诊断事实，协议中不再存在模板名或屏幕坐标传输，更不会由 Cloud 反向决定点击。新增 `XinshouLocalRecoveryHandlerTest`，与原本同帧 title+esc_bot 合成帧测试隔离执行共 `4 tests successful / 0 failed`；两仓编译均通过。真实 `esc_bot` 后 dialog 仍缺 raw screenshot，因此 fresh gate 为用户实机观察本地命中、`submitted` 状态和 UI 消失顺序；不得把 Cloud source/test 绿灯表述为该画面的实机验收。WP4 的导航、背包、修复、给予和全局 UICleaner 尚未开始。
- 2026-07-28 WP3 review repair（P2-1/P2-2）：`XinshouTrackerStateMachine` 不再用末条 `min(...)` 无限重试。每一次**真实成功提交**才由 `XinshouTask.clickTrackerLink(...)` 后的 `withSubmittedClick(currentMapX,currentMapY)` 写入点击时地图坐标并计数；状态机选下一条时保留上一次真实坐标，绝不在决策时预填或清空。连续 STOPPED_AWAY 同坐标按环绕索引选择下一条；当前 Tracker panel 的每条链接均提交过一次仍未转场时标记 `linksExhausted` 并 `PARK_IDLE`，直到 panel fingerprint 或其他真实事件改变，杜绝慢速死循环。新增覆盖“连续两次换链坐标基准不丢失”和“三链接整轮失败后稳定 park”的两条状态机测试。隔离执行 `XinshouTrackerStateMachineTest`：`7 tests successful / 0 failed`；Cloud `mvn -q -DskipTests=false compile` 通过。尝试 Maven 定向 `test` 时先被当前 Cloud 工作树既有 test-source 接口失配（`CloudWholeTaskObserverProductionHarnessTest` 等）挡在 test-compile，非本卡测试失败；该无关阻塞已记录，不得据此否定隔离 7/7 结果。
- 2026-07-28 WP4 started（UICleaner 子项）：Client `UICleanerService.closeAllGenericWindows()` 已从固定 `x1/x2/x3/npc_busy_cancel` 名单改为扫描 `images/template/cancel/` 目录的全部 PNG，因此 `x/x1..x7/npc_busy_cancel` 都成为候选；排序只为日志/测试可复现，不表达业务关闭优先级。目录不可读时 fail-closed，不猜坐标。新增 `UICleanerServiceTemplateCatalogTest`，isolated runner `1 test successful / 0 failed`；Client `mvn -q -DskipTests compile` 通过。它影响所有任务，仍需要用户在既有任务中 fresh runtime 回归一次 generic close；WP4 的 title 节点宏尚在实施。
- 2026-07-28 WP4 外部 review 返修中，状态：`REPAIR IN PROGRESS`，不得关卡。审查确认六个 P1：修复每件无条件双放、title 用单槽导致抖动重放、六视觉节点无 raw/marked replay 却接真实输入、测试仅 stub seam、宏同步阻塞 `dhxy-observe`、寻人经通用 NPC 链永久锁死。已完成的安全返修：`XinshouLocalTitleHandler` 改为持久 `consumedTitles`，领养从 title primary 链独立采样，title/领养可同帧；修复每件只有首轮三槽完全 miss 才重选，修复 title 必须先观察消失，随后 1 秒才关窗；宏运行体已移到 exact-window binding 下的专用串行 daemon worker 且不持有状态锁，采样线程只做短状态迁移；修复 title 的 10s 超时会同时清空 `runningTitle`，允许整轮重试；寻人不再压制 Tracker 读取/绿链状态机。所有 title 节点在 `images/test-cases/xinshou/raw_<node>.png` 和 `marked_<node>_click.png` 未同时存在时返回 `REPLAY_REQUIRED` 并拒绝真实输入。寻人已撤除不符合合同的 `clickNpcSmart`（它会走 tooltip/formula/Ctrl），待“记忆→固定点 `(312,323)`→黄字名 only”严格本地操作及 replay 另行实现。本轮验证事实：Client `mvn -q -DskipTests compile` exit 0；Cloud `mvn -q -DskipTests=false compile` exit 0；Client 定向 `XinshouLocalTitleHandlerTest`=5、`XinshouLocalRecoveryHandlerTest`=2，合计 **7 tests / 0 failures / 0 errors / 0 skipped**。此前“5/5”和“两个类共 6”均为错误记录，现已更正。仍未闭合：六节点真实 raw/marked replay、以真实生产服务 fake 驱动的每节点动作合同测试、严格寻人三步及 title 消失成功/有界重试、交付 completion 判定、UICleaner 既有任务 fresh runtime 回归。
- 2026-07-28 WP4 第二轮复核后继续返修：采用“纯代码先闭合、实机素材单列依赖”的策略，六节点继续 fail-closed。修复 `repairItems` 的错误优先级：先右键使用修复物品，若 `xiufu_opened.png` 未出现才再右键一次并复验；不再把第二次使用错误地绑在第一次背包操作失败上。replay gate 由 title 粒度收紧为物理步骤粒度：领养上方点/模板点、升级物品/关窗、海螺/吹响、物资/给予、修复物品使用/槽位/关窗都各自要求 `raw_<step>.png + marked_<step>_click.png`，任一步缺失均拒绝整段宏输入。Client compile、Cloud compile、7 个定向测试本轮再次均为 exit 0。仍不能关卡：这些 raw/marked 文件目前为 0，生产 executor 真实动作合同测试、严格寻人三步和所有交付完成判定仍未实现。
- 2026-07-28 用户决议 + Claude 接手收尾：**离线 replay 证据要求整体取消**（owner 拍板：模板即依据，验收=实机跑一遍；worker 援引的 AGENTS.md replay 规矩对本卡不适用）。已实施：①拆除 replay 门禁全链（hasReplayEvidence/REPLAY_REQUIRED/REPLAY_DIR 相对路径缺陷随之消失），六节点放行；②REPAIR 纳入 consumedTitles（成功即消费、10s 超时重做时释放），修复"title 抖动重放三件宏"结构缺口；③新增节点失败预算（3 次上限 + 5s*n 退避 + FAILED_EXHAUSTED 终态），消除 1Hz 无界重放；④xiufu_alldone 死分支改为真完成判定（命中即起 1s 关窗计时，不再等 title 变化）；⑤交付完成判定落地（GiveItemService 点击后轮询给予钮消失，6×500ms，未消失判失败）；⑥dialog ROI 纠正为 DialogService 大对话框区 (250,312,529×208)（吹响/物资同修）；⑦scheduleCloseRepairWindow 补 contextHolder 判空 + 关窗坐标常量化；⑧寻人节点实装（cloud XinshouTask：navigateInCurrentMap 至 (23,201)→到达后 SEEK_PERSON_POINT(312,323) 固定点点击→title 消失=成功（5s 验证窗）→失败复位有界重试 3 次；不走通用 smart-click 链，无 tooltip/formula/Ctrl/紫名）；⑨SmartClickRecognizer 越界改动已 git 还原（归 G003b 再议）；⑩XinshouGeometry 收敛为 cloud 实用常量（客户端本地节点坐标归客户端持有，注释互标）。双仓编译 0 错、协议目录 diff 零差异、客户端 xinshou 测试 10/10 绿（surefire 实跑）。UICleaner 既有任务回归并入用户实机验收一并进行。**剩余：用户实机新手号跑一遍即整卡验收**（行为不符看日志修坐标/模板）。
- 2026-07-29 Dialog 分类职责纠偏：用户明确要求新增 `DialogFrameClassifier` 只能替代旧 deviation mask，唯一职责是回答“完整 Dialog 框体是否存在”；不得用其动态框重新裁剪或改变 `OPTION / STORY / NONE` 分类。Cloud `DialogService` 已删除 `cropToDetectedDialogFrame(...)` 分类路径，结构 gate 命中后恢复提交基线的固定 `DIALOG_SMALL_*` ROI、`greenCount > 150`、多行 option 与 upper-story 判定顺序。P09 只保留为“框体存在”样本，不能宣称其已验证老 option 子判断；本次真实短“取消”现场帧归档为 `positive/P10_live_20260728_234539_short_cancel_option_dialog.png`，同一帧结构 gate 命中后老固定 option ROI 得到 `green=567` 并判为 `OPTION`。Cloud `mvn -q compile` exit 0；隔离执行 `DialogFrameClassifierReplayTest + DialogDetectionTurnContractTest` 为 **12/12 successful**。全仓 Maven testCompile 仍被既有 `CloudWholeTaskObserverProductionHarnessTest` 等旧签名失配阻断，与本修复无关。
- 2026-07-29 用户后续定稿（覆盖上一条“只回答有无”的限制）：分类器仍不得决定
  `OPTION/STORY` 语义，但必须返回真实物理框型 `SMALL/MEDIUM/LARGE/UNKNOWN`。
  现有样本测得三种外框高度中心约 `143/164/210px`；候选边必须至少有一边覆盖最大截图宽度
  的 `80%`，并选择最接近三种已测高度的完整上下边，避免大型框内部横线被误当底边。
  `MEDIUM/LARGE` 分类后完全沿用旧固定 ROI。`SMALL` 先用旧两行布局
  `cropTop=42`，未命中才用固定一行布局 `cropTop=24`；两次均复用原
  `countGreenPixelsHSV`、`greenCount > 150` 与多行 option 算法，不做滑动/动态扩张。
  真实回放：P09 两行 ROI `green=71`、一行 ROI `green=154`，第二套判
  `OPTION`；P10 第一套 `green=567`；P08 两套均 `green=0`。标记图位于
  `images/test-cases/dialog-frame-classification/replay-output/`，青框=真实外框、
  橙框=两行 lower、蓝框=一行 lower。Cloud main compile exit `0`；隔离执行
  `DialogFrameClassifierReplayTest + DialogDetectionTurnContractTest` 为 **14/14 successful**。
  全仓 Maven testCompile 仍被既有 Observer/Navigation 旧测试签名漂移阻断。
- 2026-07-29 SMALL ROI 视觉复核修正（覆盖上一条 `42/24` 边界）：P09 绿字实际位于
  small panel 偏移 `35..48px`。一行布局蓝线上沿改为 `34px`，位于“别打了！”上方且不再覆盖
  白字；两行布局黄线上沿改为 `49px`，位于该绿字下方。P09 两套计数为 `0/154`，P08 为
  `0/0`。P10 在 `49px` 以下只剩短“取消”一行，计数 `118`，因此仅 SMALL 两行布局采用
  `>100` 阈值；HSV 算法、SMALL 一行及 MEDIUM/LARGE 的 `>150` 阈值不变。Cloud main
  compile exit `0`，两类隔离测试再次 **14/14 successful**；标记图已覆盖更新。
