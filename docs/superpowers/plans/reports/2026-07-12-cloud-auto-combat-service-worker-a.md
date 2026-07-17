# Worker A：`AutoCombatService` 整类迁云

## Parent Task Brief #1 - 2026-07-12

### 当前任务

外部 Worker A 的 resume executor-readiness 切片已获父级 `APPROVED，P0/P1/P2=0`。下一项复杂主线任务是设计
`AutoCombatService` 的整类 Cloud lift-and-shift：保持 public API、业务判断、调用顺序、retry/fallback/sleep/stop 语义，
把截图、窗口事实和原子输入 bundle 机械执行改为现有 retained-authority typed Service port。首轮只做 Design #1；父级明确
`DESIGN APPROVED` 前不得修改 Java/Maven/resources/tests。

### 强制基线

- 必读 `D:\mavenProject\DHXY\AGENTS.md`、`docs\DHXY_CONTEXT.md`、`docs\业务逻辑.md`、
  `docs\ACTIVE_WORK.md` 顶部 CR271、`docs\superpowers\specs\2026-07-12-service-migration-matrix.md`。
- 业务源码只以 DHXY HEAD `0114604e1ff5f15491d2910959c45252e893d04f` 的
  `src/main/java/com/bot/dhxy/service/AutoCombatService.java` 为权威；当前工作区该文件有用户/并行 dirty，不得把 dirty 差异
  带入迁移，不得覆盖或回滚。
- 阅读所有 HEAD caller、`GameContext`、Cloud `TaskExecutionContext`、`CloudTaskServiceExecutionContext`、
  `CloudTaskServicePort`、retained action ledger/handle、checkpoint/sleep 和已批准的 `TaskTurnCoordinator` 迁移矩阵条目。

### 不变量

1. 整类边界与 public 方法保持；每个业务条件、状态读写、日志关键字、按键/点击 bundle 顺序、sleep、retry/fallback 和
   success/failure 条件逐项与 HEAD 等价。无已批准业务差异。
2. Cloud 不得复制/引用 `WindowRuntimeContext`、holder、tracker、HWND 查找、`InputSequences`/`InputProvider`、本地
   `TaskPauseToken`、输入队列、JavaFX、Path/temp 文件或本地 `TaskTurnCoordinator` 权威。
3. 机械事实/截图/输入只能经 exact `TaskExecutionContext` 对应的 retained `CloudTaskServicePort`；稳定 requestId/actionId
   只能来自 retained ledger，UNKNOWN 重投必须 same bytes/id，未解析 UNKNOWN 不得 renewal。
4. 每次机械调用前必须使用现有 typed checkpoint/revalidation；pause/stale/completed/denied typed unwind，不得解释为业务
   FAILED/SUCCESS 或新增 retry。STOP 保持源语义。
5. `GameContext` 业务状态必须绑定未来 exact per-run State；当前 host/cohort dormant，不得用 singleton default state 冒充
   tenant/run authority，不得在本切片启动 Task。
6. 不新增 raw request/poll/outcome、TTL、takeover、后台线程、自动 retry、测试或新的业务配置；不接 server/endpoint/host
   启动路径。
7. 与 B artifact/template、F BaseTaskTemplate、G startup role gate 写集零交集；不得修改其报告或文件。

### Design #1 必交付

- `AutoCombatService` 全部 public/private 方法与 HEAD caller 矩阵，逐方法标注：纯业务、GameContext 状态、机械 fact、capture、
  input bundle、sleep/checkpoint、turn ownership。
- public API compatibility table：每个参数/返回值/异常/日志/状态副作用如何保持；需要 explicit context 的最小签名适配必须
  列出所有 caller 改动，不得藏 ThreadLocal/context holder。
- 机械调用映射：每个 Alt/key/click/focus/window read 对应哪种 retained action、稳定 handle owner、timeout、UNKNOWN/final
  处理和原子 bundle 边界。
- Task turn 语义迁云方案：不得复制本地 fair lock/输入 worker thread 权威；说明它由 Cloud per-run/cohort scheduler 还是
  retained input single-flight 承担，且不能改变源 yield/serialization 语义。
- exact Cloud/DHXY 文件写集、依赖/bean 构造图、future caller 激活顺序；优先只改 Cloud，若确需 DHXY wire 必须证明现有
  typed port 不足并列最小字段。
- tenant/window/revision、错窗、乱序、断线、UNKNOWN、pause/resume、stop、容量、运维、host dormant 风险矩阵；列所有
  未决 P0/P1/P2。
- 推荐分阶段实施波次，但不得拆成长期本地/云端双业务权威；开发可分依赖提交，生产只整体切换。

### Worker 协作规则

- 你是实现 Worker，不是 reviewer。只向本文件 append `External Worker A - Design #1`；父级批准前不改代码。
- 你不是独自在仓库工作；保护全部 dirty/untracked，不回滚、不覆盖、不提交、不改其它 Worker 写集。
- 不运行 Maven/测试，不启动 application/server/host/poller/UI/capture/OCR/input，不发送任何机械动作。
- 完成 Design #1 后保持自己的 5 分钟 heartbeat，持续读取本文件；看到父级 `BLOCKED` 就由同一 A 返修，看到
  `DESIGN APPROVED` 才实施，直到父级 Implementation `APPROVED` 或父级写入下一任务。

## External Worker A - Design #1 - 2026-07-12

依据：HEAD `0114604e` 的 `service/AutoCombatService.java`（1213 行，逐行通读）；`AGENTS.md`、`DHXY_CONTEXT.md`、`业务逻辑.md`（L22-39 standalone/队伍、L226-279 expected/fast-exit、L1016 战后补给短窗口放权等基线行）、`ACTIVE_WORK.md` CR271 顶部、迁移矩阵 L136-141/L398。工作区该文件 dirty，本设计未采信任何 dirty 差异。设计-only：未改 Java/Maven/resources/tests，无 Git/Maven 运行。

### 0. 总体结论

矩阵 L398 判定成立并经逐行验证：**AutoCombatService 本体零直接机械调用**——无截图、无输入、无 HWND、无文件；全部机械动作都在 7 个注入协作 Service 内。因此本类迁云是"纯业务状态机 + 每窗口状态键控 + turn 协调 + checkpoint"的等价搬运，机械面表现为**协作接口面**而非 port 调用点。本类自身不需要新增任何 wire 字段；只改 Cloud，DHXY 写集为空。

### 1. 全方法 × HEAD caller 矩阵

| 方法（HEAD 行号） | 类别标注 | HEAD callers（file:line） |
|---|---|---|
| `initializeForCurrentWindow()` :81 | GameContext 状态重置 + holder 键控 + team-phase invalidate（无输入） | AutoBattleTask:139；WubeiTask:384,1074,4105,4299；XiuluoTaskV2:2622,3926,4286,4595,5251；WindowTaskRunner:846 |
| `authorizeCombatDetectionAfterEnterBattleAction` :111 | 纯业务状态（CR252 探测授权 lease） | WubeiTask:1075,4108,4218；XiuluoTaskV2:2623,3927,4287,4596 |
| `revokeCombatDetectionAuthority` :123 | 纯业务状态 | XiuluoTaskV2:4805；WindowTaskRunner:849 |
| `handleCombatTick(ctx,src,boolean)` :204 | 委托 overload（legacy 政策映射） | AutoBattleTask:176；FiveRingTaskV2:2090 |
| `handleCombatTick(ctx,src,Policy)` :223 | 主 tick：checkpoint + 队员覆盖状态机 + radar 委托 + enter/exit 消费 + 恢复链 + GameContext 状态 | WubeiTask:4257,4427；XiuluoTaskV2:3723,4298 |
| `handleWindowCombatGuardTick` :348 | watcher 轻量 tick（radar 委托 + enter 处理，不消费 exit） | WindowTaskRunner:901 |
| `probeWindowCombatStateReadOnly` :378 | 只读探测（radar 委托） | WubeiTask:4880；FiveRingTaskV2:2311；XiuluoTaskV2:4815；WindowTaskRunner:714,729 |
| `probePausedWindowCombatStateReadOnly` :401 | 只读探测 + stop-token checkpoint（绕过 pause token） | WindowTaskRunner:868 |
| `getDynamicPollingIntervalMs` :430 | 纯委托 BattleRadar | AutoBattleTask:349；WindowTaskRunner:726,931 |
| `nextCombatMaintenanceDelayMs` :446 | 纯业务计时（timer 推导，读 BotProperties+GameContext） | 无外部 caller（被 :495 内部用）；public 签名保留 |
| `nextCombatWakeDelayMs` :495 | 纯业务计时 | WubeiTask:1209；XiuluoTaskV2:4616 |
| `hasPendingFollowerFirstAidForCurrentWindow` :514 | 纯状态读 + holder 键控 | AutoBattleTask:339 |
| `hasPendingLeaderPostCombatRecoveryForCurrentWindow` :522 | 纯状态读 + holder 键控 | XiuluoTaskV2:4847 |
| `consumeQueuedLeaderPostCombatFirstAidIfHead` :558 | 业务 + PlayerState/TaskMaintenance 委托（CR243 FIFO 头消费） | XiuluoTaskV2:3526 |
| `reportQueuedLeaderPostCombatFirstAidIfPending` :586 | 业务 + 委托（no-focus 探测上报） | XiuluoTaskV2:3522 |
| `refreshFastExpectedExitBaselineAfterTrustedInCombat` :714 | 状态 + radar 基线委托 | WubeiTask:4883；XiuluoTaskV2:4818 |
| `reconcileReturnHomeVerifiedCombatState` :736 | 业务对账（team-phase exit 广播 + 授权撤销 + 状态清） | WubeiTask:4585,4616 |
| `consumePendingLeaderPostCombatRecoveryIfAllowed` :777 | 业务 + PlayerState 委托 + checkpoint | WubeiTask:3343；XiuluoTaskV2:3528,4850 |
| private：`mayRunBattleRadar`:139、`memberLeaderCombatPhase`:158、`isMemberReadOnlyDegrade`:172、`legacyPostCombatRecoveryPolicy`:327、`maybeHandleCombatEnter`:597、`consumeExitAndRecover`:614、`runPendingMemberCommonBoxIfAllowed`:819（turn）、`runPendingFollowerFirstAidIfAllowed`:847（turn）、`shouldDeferFollowerFirstAid`:958、`safeTaskCode/RequestedTaskCode/Role`:975-985、`maybeRunCombatMaintenance`:987（timer+委托）、`cleanCombatUiForRole`:1089、`logRefreshDueDeferred`:1099、`state`:1117（holder 键控+epoch 漂移失效）、`currentPlayerIdentityEpoch`:1142、`currentWindowId`:1148 | 全部逐行等价保留 | — |
| nested：`TickResult`、`PostCombatRecoveryPolicy`、`AutoCombatRuntimeState`、`RefreshDuePanelVerifyDecision`、`RefreshDuePanelVerifyGate`（30s 团队 verify 闸） | 原样保留（含 CR242/CR243/CR252 全部注释语义） | MultiWindowTaskManager 仅类型装配引用，无方法调用 |

### 2. Public API compatibility table

| 签名要素 | 保持方式 |
|---|---|
| 方法名/返回值/枚举/嵌套类型 | 全部逐字保留；`TickResult`/`PostCombatRecoveryPolicy` 常量与构造语义不变 |
| `TaskExecutionContext` 参数 | 用 Cloud 已迁同 FQCN `com.bot.dhxy.runner.context.TaskExecutionContext`（由 `CloudTaskServiceExecutionContext` 桥接）；`getTaskCode/getRequestedTaskCode/getWindowRole/getLogPrefix/getLocalTeamSessionKey/getLocalLeaderWindowId/isLocalLeaderPresent/getStopToken/throwIfStopRequested` 已由 `CloudTaskServiceMetadata` + typed checkpoint 覆盖，零语义差 |
| **唯一签名适配（explicit context，不藏 ThreadLocal）**：5 个隐式读 `WindowTaskContextHolder` 的方法 `initializeForCurrentWindow()`、`authorizeCombatDetectionAfterEnterBattleAction`、`revokeCombatDetectionAuthority`、`hasPendingFollowerFirstAidForCurrentWindow`、`hasPendingLeaderPostCombatRecoveryForCurrentWindow`、`getDynamicPollingIntervalMs`、`nextCombatMaintenanceDelayMs`、`nextCombatWakeDelayMs`、`refreshFastExpectedExitBaselineAfterTrustedInCombat` | 云端签名各追加首参 `TaskExecutionContext context`（其余参数不动）。**全部受影响 caller 行**即第 1 节矩阵所列（AutoBattleTask:139,339,349；WubeiTask:384,1074,1075,1209,4105,4108,4218,4299,4883；XiuluoTaskV2:2622,2623,3926,3927,4286,4287,4595,4596,4616,4805,4818,4847,5251；WindowTaskRunner:714,726,729,846,849,868,931）——这些 caller 均属后续 B 类任务大脑迁移切片，其云端版本在各自切片按此表补参；本地 HEAD 版本零改动（本切片不改 DHXY） |
| 日志 | 关键字/格式串/级别逐条保留（含 `[window=unknown]` 兜底、CR 注释文案） |
| 异常/中断 | `throwIfStopRequested` 与 `TaskCheckpoint.throwIfStopRequested(stopToken,…)`（:402 绕过 pause token 语义）映射到 Cloud typed checkpoint 的同名同语义入口；pause/stale/denied 走 typed unwind，不转译为业务结果 |
| 状态副作用 | `gameContext.setCurrentActionState(FREE)`（:644,:703,:755）等 GameContext 读写点逐一保留，绑定 per-run State（见 §5） |

### 3. 机械调用映射（本类间接机械 = 协作接口面）

本类无 Alt/key/click/focus/window read 直接调用点；机械面全部经协作 Service。逐调用点映射：

| 本类调用点 | 协作方法 | 机械实质 | Cloud 归宿 |
|---|---|---|---|
| :233,:287,:288,:291,:356,:380,:412,:499,:621,:622,:596,:599,:602,:629,:716,:756 | `BattleRadarService`（radar 扫描/信号/头像 diff/外部裁决 :247） | 战斗模板区域截图 + ROI（A 类：决策上云、截图经 port） | 云端 BattleRadar 迁移切片；对本类是**接口不变的云内同伴调用** |
| :611,:1022,:1082 | `AutoCombatPanelService.ensurePanelVisible/verifyAndAlignPanel/recordCombatExit`:629 | Alt+8 键 bundle + 面板模板/OCR + 拖拽 | 云端 AutoCombatPanel 迁移切片（其内部经 `CloudTaskServicePort` 的 retained WINDOW_FACT/CAPTURE/EXECUTE_INPUT_BUNDLE） |
| :565,:573,:591,:630,:651,:684,:693,:700,:805,:809,:815,:883,:895,:943 | `PlayerStateService`（no-focus 探测/缓存急救计划/摄妖香/resetCheckCounter） | 血条截图 + 右键补给 bundle | 云端 PlayerState 迁移切片 |
| :1093,:1096 | `UICleanerService.cleanUpAll/closeAllGenericWindows` | 关窗点击 bundle | 云端 UICleaner 迁移切片 |
| :101,:162,:559,:567,:574,:593,:661,:689,:746,:866,:873,:887,:896,:906,:907,:916,:922,:925,:1033,:1034,:1042 | `TaskMaintenanceService`（team phase/FIFO/capability 状态） | 纯状态权威（无机械） | 云端 Maintenance 状态 Service |
| :1036,:1047 | `LeftTopStatusSwitchService.handleCombatMaintenance` | 左上状态点击 bundle | 云端切片 |
| :636,:824,:834 | `CommonBoxService`（宝箱 pending 探测/消费） | 小 ROI 截图 + 点击 | 云端切片 |

对本类的约束仅一条：以上协作调用**顺序、条件、参数、次数逐点保持**；稳定 requestId/actionId、UNKNOWN same-bytes 重投、checkpoint-before-mechanical 均由各协作切片在其 port 调用处满足（本类每个含机械委托的路径入口已有 `throwIfStopRequested`，:226/:349/:379/:402/:698/:814/:988 保留即满足"每次机械调用前 typed checkpoint"的本类侧责任）。

### 4. Task turn 语义迁云

- 源语义（矩阵 L136/L152/L160/L170/L188）：fair ReentrantLock(true) 排队 + ThreadLocal 持有深度 + input-worker 线程空转豁免；本类用法仅两处模式：`enter(name); try{…} finally{ forceRelease(name); }`（:830/:843 common-box、:881/:903 与 :939/:954 first-aid），且 :558 依赖"leader 已持回合时不重入"。
- 方案：本类改依赖云端 **`CloudTaskTurnCoordination` 接口**（`enter(String)`/`forceRelease(String)`，blocking-fair 语义 + 幂等 forceRelease + 持有者重入等价），由矩阵既定的云端 TurnCoordination/LeaseService 切片实现（跨窗口回合仲裁上云；本地退化为租约执行器）。**不复制** fair lock/ThreadLocal/输入 worker 线程名判定到云端；也不由本切片实现该服务。yield/serialization 语义以源为准由该切片证明等价，本类只保证调用点形状不变。

### 5. 写集、bean 构造图与激活顺序

**Cloud 写集（全部 New，0 modify；DHXY 写集为空）**：
1. `com/bot/dhxy/service/AutoCombatService.java` — 整类等价迁移 + §2 签名适配；`runtimeStates` 保留 Map 形态但宿主为 **per tenant/user 的 `CloudServiceHost` 容器 bean**（DialogChoiceMemoryService 先例），键仍 windowId、`playerIdentityEpoch` 漂移失效逐行保留（epoch 源改为 `context` 显式提供，等 Worker H 的 per-run `GameContext.State` owner 落地后由该 owner 绑定 exact run，**不用 singleton default state 冒充 run authority——在 State owner 就绪前本类不注册、不实例化**）。
2. 协作接口 seams（仅当波次 1 时协作实现未迁完才需要；已迁完则直接依赖其云端类）：`BattleRadarService/AutoCombatPanelService/PlayerStateService/UICleanerService/TaskMaintenanceService/LeftTopStatusSwitchService/CommonBoxService` 的云端接口（各自切片交付实现）+ `CloudTaskTurnCoordination`。
3. `BotProperties.getAutoBattleRefreshIntervalMs` → 云端等价只读配置 bean（powerless，不新增业务配置项，取值语义同源）。

**bean 构造图**：`CloudServiceHost`（per tenant/user）→ 注册 per-run `GameContext.State` owner（Worker H）+ 7 协作 bean + `CloudTaskTurnCoordination` + 配置 bean → `AutoCombatService`（constructor 注入，`@RequiredArgsConstructor` 风格保留）→ callers（B 类任务大脑，各自切片）。

**激活顺序（开发可分波提交，生产整体切换，无长期双权威）**：W1 协作接口 + 配置 bean（dormant）→ W2 本类等价迁移（dormant，无 caller）→ W3 依赖切片就绪（radar/panel/player-state/maintenance/turn/State owner）→ W4 任务大脑 caller 切片按 §2 表接线 → 生产 cutover 一次性切换（本地 HEAD 行为在此前始终是唯一生产权威）。

### 6. 风险矩阵与未决项

| 风险 | 处置 |
|---|---|
| tenant/window/revision 错配 | 本类一切机械经协作层→port，三道 revision 闸 + exact window 元组已冻结生效；本类状态键 windowId+epoch 显式来自 context，epoch 漂移失效保留 |
| 错窗/乱序/断线/UNKNOWN | 全部由 port/broker/local fence 已批准合同承接；本类无 raw 请求路径 |
| pause/resume | :401 stop-only checkpoint 语义保留；resume 后旧 context typed unwind（已批准 resume reconcile-confirm 合同），不转业务 FAILED |
| 容量/运维 | `runtimeStates`/`RefreshDuePanelVerifyGate` 均有界（窗口数级）；host 容器内存随 run 释放由 State owner 切片界定 |
| host dormant | 本切片全程不注册/不实例化/不接 caller；W1-W2 交付后不可观测 |
| **P1（未决，跨切片依赖）** | 本类可实例化依赖：Worker H per-run State owner、7 协作 Service 云端形态、TurnCoordination/LeaseService、typed checkpoint（已冻结 ✅）。前三者未齐前 W2 保持 dormant 不装配 |
| **P2（未决）** | `System.currentTimeMillis` 业务计时保留于云端进程时钟——跨进程后 timer 基准与本地窗口事件解耦，fast-exit/entry-maintenance 时序容差需在 radar/panel 切片联调时复核 |
| **P2（未决）** | `BotProperties` 云端配置源的取值与本地一致性（同一用户配置双端下发）属配置切片 |
| P0 | 无 |

`无已批准业务差异；按基线等价迁移。`（已核 业务逻辑.md：expected/fast-exit L226-279、战后补给短窗口 L1016、standalone/队伍 L22-39 相关行为全部按 HEAD 逐行保留，无新增 TTL/重试/清理/门。）

Design #1 到此停止，等待父级 `DESIGN APPROVED` / `BLOCKED`；批准前不改 Java/Maven/resources/tests。Worker A 保持 5 分钟 heartbeat。

## Parent Design Review #1 - BLOCKED - 2026-07-12

HEAD 方法/caller/间接机械依赖盘点基本成立，特别是“本类无直接截图/输入，机械面在 7 个协作 Service”这一结论可保留。
但当前方案不能按所列写集编译并保持基线。父级结论：**BLOCKED，P0=0/P1=3/P2=1**。

### P1-1：暂停窗口只读 observer 语义没有 Cloud 能力，设计误称 `getStopToken` 已覆盖

- 证据：Design §2 声称 Cloud `TaskExecutionContext` 有 `getStopToken`，但当前 Cloud 类没有该 API，且是刻意删除本地
  stop/pause token。`probePausedWindowCombatStateReadOnly` 的 HEAD 语义正是用 stop-only checkpoint 绕过 pause，继续观察
  combat enter/exit；Cloud `throwIfStopRequested/isStopRequested` 对 PAUSED 会 typed unwind，现有 command broker/port 也不允许
  以普通 ACTIVE context 在 PAUSED 发起新的 capture/fact。
- 影响：直接迁移会在用户暂停时停止 radar observer，丢失战斗进入/退出证据，改变 resume 后业务判断；把它留本地又会保留
  本地战斗业务第二权威。
- 返修条件：Design Repair #1 必须单列“PAUSED read-only observer capability”前置切片：仅允许 exact scope/run/window/
  stopEpoch/current paused revision 的 WINDOW_FACT/CAPTURE，严禁 input/action renewal；Cloud coordinator/broker 与 DHXY handler
  都要有 typed read-only authorization/fence，STOP/terminal/session/window drift 拒绝。或者证明该 observer 可由既有 retained
  same-request 在 pause 前持续完成且完全等价；不得虚构 `getStopToken`。该前置未批准前 AutoCombat Java 冻结。

### P1-2：设计声称保留 public API，却给 9 个 public 方法改签名

- 证据：Parent Brief 要求整类 public API 保持；Design §2 对
  `initialize/authorize/revoke/hasPending*/getDynamicPollingInterval/next*Delay/refreshBaseline` 增加
  `TaskExecutionContext` 首参，并把 caller 修改后置到多个 Task/runner 切片。
- 影响：这不是整类 lift-and-shift，而是跨大量 caller 的 API 重写；会迫使未迁 caller 同步变更并形成长期双形态，且未解决
  host singleton `runtimeStates` 的 run/session 生命周期。
- 返修条件：优先采用**per exact taskRun 构造的 context-bound AutoCombatService**：由 non-mintable activation owner 注入一个
  immutable exact context，原无参 public 方法使用该 bound context；原本自带 context 的方法必须 exact-match bound context。
  不得 ThreadLocal/holder fallback。若认为任一签名必须改变，逐方法证明无法 context-bound，并把所有 caller 纳入同一原子
  cohort 写集，不能写“后续补参”。同时定义 runtimeStates/verify gate 在 stop/complete/host close 的 exact cleanup 与配额。

### P1-3：写集不是可编译闭包，7 个协作 Service、turn 与配置仍是条件占位

- 证据：§5 把 AutoCombatService 列为 New，同时写“协作接口 seams 仅当未迁完才需要”“W1 以后交付”；当前 Cloud 缺
  `BattleRadarService`、`AutoCombatPanelService`、`PlayerStateService`、`UICleanerService`、
  `TaskMaintenanceService`、`LeftTopStatusSwitchService`、`CommonBoxService`、Cloud turn 和等价 BotProperties 配置。
- 影响：AutoCombatService 现在无法编译；为过编译临时造空接口会冻结错误合同、隐藏机械 UNKNOWN/final/turn 语义，后续
  实现时再次改 API。
- 返修条件：Design Repair #1 必须给出确定的 dependency DAG 和**每一波 exact 文件/方法合同**，不使用“if needed”。W0
  先实现哪些纯合同/配置/turn/paused-observer，W1/W2 分别何时可编译；任何 seam 必须是最终 Service public contract，不能
  是 temporary placeholder。明确 AutoCombatService 只有在全部 constructor dependency 类型存在且父级批准后才进入 Java
  写集。

### P2-1：墙钟来源与业务事件时间基准仍未闭合

- 证据：Design 只写“联调复核”，未列 fast-exit/radar/panel 事件究竟在 Cloud 收到时打点还是携带 client wall clock。
- 影响：若跨机器时钟直接比较，会改变 CR242/CR252 的 30s/fast-exit 窗口。
- 返修条件：所有业务 timer 统一在 Cloud 以 `System.nanoTime`/同进程 monotonic elapsed 建立，wire/client wall-clock 只做日志，
  不参与 timeout/窗口判定；逐字段列出迁移映射。不得新增或改变任何时长。

### Repair #1 验收门

同一 A 只追加 `External Worker A - Design Repair #1`：修复 paused observer 能力缺口、public API/context-bound 实例、可编译
dependency DAG 与 monotonic timer 映射。Java/Maven/resources/tests、host/cohort 继续冻结；不推翻已通过的方法/caller/
协作调用顺序盘点。**无已批准业务差异；按基线等价迁移。**

## External Worker A - Design Repair #1 - 2026-07-12

针对 `Parent Design Review #1 - BLOCKED`（P0=0/P1=3/P2=1）逐条返修。仅设计；Java/Maven/resources/tests、host/cohort 继续冻结。已通过的方法/caller/协作调用顺序盘点（Design #1 §1/§3）不推翻、不重开。

### R1（P1-1）：PAUSED read-only observer 前置切片（更正：撤回 `getStopToken` 误述）

更正：Cloud `TaskExecutionContext` 无 `getStopToken`，Design #1 §2 该句作废。HEAD `probePausedWindowCombatStateReadOnly` 的"stop-only checkpoint + 暂停期间继续 radar 观察"语义需要新的前置能力切片，**该切片未获父级批准前 AutoCombat Java 全程冻结**：

- **切片名**：`PAUSED read-only observation capability`（独立共享日志、独立父级门）。
- **合同**：仅 `WINDOW_FACT` 与 `CAPTURE` 两种 operation 可在 exact `(scope, taskRunId, window 四元组, stopEpoch, 当前 PAUSED runRevision)` 下以 typed `observation=PAUSED_READ_ONLY` 标记获得授权；`EXECUTE_INPUT_BUNDLE` 一律拒绝；retained action 照常 mint/bind/same-bytes 重投，但 PAUSED 期间 `renewAfterNotExecuted` 拒绝（未解析 UNKNOWN 不得换新，与既有冻结合同一致）。
- **三道闸对称扩展**：Cloud coordinator 增加只读授权分支（status==PAUSED 且 request.runRevision==当前 PAUSED revision 且带 PAUSED_READ_ONLY 标记时放行 fact/capture；STOP/terminal/session/window/revision drift 全部 typed 拒绝）；broker 入队/dispatch 双门同规则；DHXY handler pre-side-effect 门允许 registration.status==PAUSED 且 revision 精确相等的 fact/capture，输入路径不变（PAUSED 拒绝）。
- **等价性**：resume 后旧 observer context/handle 因 revision 前进照常失效（已冻结语义）；`markCombatExitObservedDuringPause` 的信号留存属 BattleRadar 切片业务态，不在本能力合同内。
- 已评估替代方案"pause 前 retained same-request 持续覆盖"：不等价——暂停期间需要新鲜捕获判定 enter/exit 边沿，同一 requestId 的 redelivery 只回放旧 terminal outcome，不能产生新观察。故必须新增该能力切片，不虚构 API。

### R2（P1-2）：context-bound per-run 实例，public API 逐字保持

撤回 Design #1 §2 的"9 方法加首参"方案，改为父级指定的 **per exact taskRun 构造的 context-bound AutoCombatService**：

- 云端 `AutoCombatService` 实例由 non-mintable activation owner（`CloudTaskRunAuthorityAssembly` 既有 package-private 运行时构造路径）在 run 激活时以 immutable exact `TaskExecutionContext` 构造注入；**全部 public 方法签名与 HEAD 逐字相同**。原隐式 holder 方法（initialize/authorize/revoke/hasPending*/getDynamicPollingInterval/next*Delay/refreshBaseline）读 bound context；原自带 context 参数的方法在入口做 `context == boundContext` 同一性校验，不匹配抛 typed `IllegalStateException`（fail-closed，不静默采用传入值）。无 ThreadLocal/holder fallback。所有 caller 调用形状零变化，"后续补参"表作废。
- **runtimeStates 生命周期**：per-run 实例使 HEAD 的 `Map<windowId,State>` 收敛为单个 per-instance `AutoCombatRuntimeState`（run 绑定唯一 window；语义等价证明：HEAD 键控=windowId + epoch 漂移重置；per-run context 的 window/epoch 不可变，epoch 漂移在 run 级已被三道闸拒绝，新 run=新实例=全新状态，正是 HEAD "invalidated by drift → reset" 的强化形式，无行为差）。实例随 run stop/complete/typed unwind 终结由 activation owner 释放；host close 释放容器内全部实例。无 host 级 singleton 可变状态。
- **`RefreshDuePanelVerifyGate`（30s 团队 verify 闸，跨窗口）**：唯一跨 run 共享态，收敛为 host 容器内的独立小 bean（键=teamKey/windowId 同 HEAD），配额=有界（≤注册窗口数），清理转移：run 终结清除该窗口贡献键、host close 全清；30s 窗口值与判定逻辑逐字保留。

### R3（P1-3）：可编译 dependency DAG（无 "if needed"，全部最终合同）

原则：每个 seam 都是对应协作 Service 的**最终 Cloud public contract**（= 其 HEAD public 签名 + R2 同款 context-bound 语义），非临时占位；`AutoCombatService` 只有在下列全部 constructor 依赖类型存在且父级批准后才进入 Java 写集。

**W0（前置能力/合同，各自独立切片与门）**
1. R1 的 PAUSED read-only observation capability（Cloud coordinator/broker + DHXY handler）。
2. `CloudTaskTurnCoordination`：`void enter(String transactionName)` / `void forceRelease(String transactionName)`，blocking-fair 排队 + 持有者重入等价 + 幂等 forceRelease（矩阵 L136 TurnCoordination/LeaseService 切片实现，仲裁上云）。
3. 云端只读配置 bean `CloudAutoBattleProperties`：`long getAutoBattleRefreshIntervalMs()`（取值语义=本地 BotProperties 同名项，单向下发，无新增配置项）。
4. Worker H per-run `GameContext.State` owner（已在途）。

**W1（7 个协作 Service 的最终合同，本类仅消费下列成员——即各切片必须交付的最小 public 面）**
- `BattleRadarService`：`checkAndSyncCombatState()`、`consumeCombatEnterSignal()`、`consumeCombatExitSignal()`、`consumeCombatExitSignalForExpectedWait(String)`、`discardStaleCombatExitSignalIfInCombat(String)`、`discardCombatEnterSignalIfNotInCombat(String)`、`armExpectedCombatExitWait(String)`、`applyExternalCombatStateVerdict(boolean,String)`、`checkFastExpectedCombatExitByAvatarDiff(String)`、`shouldRunFullRadarForFastExpectedExitFallback()`、`refreshFastExpectedCombatExitAvatarBaseline(String)`、`nextFastExpectedCombatExitProbeDelayMs()`、`getDynamicPollingIntervalMs()`、`markCombatExitObservedDuringPause(String)`
- `AutoCombatPanelService`：`ensurePanelVisible(String,int)`、`verifyAndAlignPanel(PanelVerifyMode)`、`recordCombatExit()`、static `resolveRoundsRefreshReason(Integer,long,long,long)`、enum `PanelVerifyMode{ENTRY_MAINTENANCE,VERIFY_AND_REFRESH}`、enum `RoundsRefreshReason{UNKNOWN,LOW_ROUNDS,REFRESH_DUE,…}`
- `PlayerStateService`：`probeAndConsumeHealthyFirstAidNoFocus(TaskExecutionContext,String)`、`performCachedFirstAidPlanNow(TaskExecutionContext)`、`hasPendingNoFocusFirstAidPlanForCurrentWindow()`、`ensureSheYaoXiangActiveForLeaderTask(String,TaskExecutionContext)`、`resetCheckCounter()`、enum `FirstAidNoFocusProbeResult{SUPPLY_NEEDED,UNKNOWN,HEALTHY}`
- `UICleanerService`：`cleanUpAll()`、`closeAllGenericWindows()`
- `TaskMaintenanceService`：`invalidateTeamCombatPhaseForLeader(String,String)`、`memberTeamCombatPhase(TaskExecutionContext)`、`MemberTeamCombatPhaseView{present,covered,leaderPaused,inCombat,epochId,absent()}`、`reportPostCombatFirstAid(TaskExecutionContext,PostCombatFirstAidReport,boolean,String)`、enum `PostCombatFirstAidReport{SUPPLY_NEEDED,UNKNOWN,HEALTHY}`、`isPostCombatFirstAidHeadWindow`、`hasPostCombatFirstAidQueueItem`、`completePostCombatFirstAidAttempt`、`isLocalSupportMemberSession`、`isLocalTeamSupportCapabilityOpen(TaskExecutionContext,TeamSupportCapability)`、`isPendingLocalSupportLeaderDetection`、`isLocalSupportMemberCandidate`、`isTeamFirstAidMaintenanceWindowOpen(TaskExecutionContext,String)`、`confirmTeamCombatPhaseExitedForLeader(TaskExecutionContext,String)`
- `LeftTopStatusSwitchService`：`handleCombatMaintenance(TaskExecutionContext,String)`
- `CommonBoxService`：`detectMemberBoxAfterCombatExit(TaskExecutionContext,String,String)`、`hasPendingBoxForCurrentWindow(TaskExecutionContext,String)`、`consumePendingBoxIfAllowed(TaskExecutionContext,String,String)`

（各成员合同即 HEAD 同名 public 签名；"ForCurrentWindow" 语义在各自切片同样按 context-bound 收敛，签名不变。）

**W2**：全部 W0+W1 类型存在后，`AutoCombatService` 整类等价迁移（context-bound，dormant，无 caller），单独批准后才写 Java。
**W3**：任务大脑 caller 切片按原调用形状接线（零补参）；生产一次性 cutover，无长期双权威。

依赖 DAG：W0.1→(W1.BattleRadar/Panel/PlayerState 的 paused-observer 路径)；W0.2→W2（turn 两处调用点）；W0.3→W2；W0.4→W2（GameContext 读写绑定）；W1.*→W2→W3。每波完成判据=该波全部文件编译进 Cloud `mvn -q clean package` 且父级批准。

### R4（P2-1）：单调时基逐字段映射

云端本类全部业务计时改用**同进程单调毫秒源**（`System.nanoTime()/1_000_000` 派生的 `monotonicMillis()`，注入式 LongSupplier 以保可测），wall-clock（client `observedAtEpochMs`、receipt 时间）只入日志，绝不参与窗口/超时判定；**所有时长常量与判定逻辑零改动**（4_000/40_000/30_000/10_000/30_000ms 与 `getAutoBattleRefreshIntervalMs`）。逐字段：

| HEAD 字段（System.currentTimeMillis 基） | 云端时基 |
|---|---|
| `lastAutoBattleRefreshAt`、`lastCombatUiCleanAt`、`pendingCombatEntryMaintenanceAt`、`lastRefreshDuePanelVerifyAttemptAt`、`lastRefreshDuePanelVerifyDeferredLogAt`、`lastUrgentRoundsPanelVerifyAttemptAt`、`combatDetectionAuthorizedAtMs` | monotonicMillis（per-instance，run 生命周期内自洽） |
| `RefreshDuePanelVerifyGate.lastVerifyByTeam` | monotonicMillis（host bean 同一进程源，跨 run 比较仍在同一单调轴） |
| `GameContext.getLastAutoCombatRefreshAt`/rounds 估计 | 由 Worker H per-run State owner 以同一单调源承载（本设计消费，不定义其存储） |
| 传给 `resolveRoundsRefreshReason(…, now)` 的 `now` | 同一 monotonicMillis（该静态法只做差值比较，等价） |

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。Design Repair #1 到此停止，等待父级复审；heartbeat 保持。

## Parent Design Review #2 - BLOCKED - 2026-07-12

R1 的 PAUSED read-only capability 缺口识别、W0-W3 dependency DAG、public API 不加 context 以及 monotonic timer 映射均可
保留；但 R2 的 context/state 生命周期和 verify gate cleanup 仍会改变 HEAD 行为。结论：
**BLOCKED，P0=0/P1=2/P2=0**。

### P1-1：immutable revision context 与 same-run AutoCombat 状态保留互相矛盾

- 证据：R2 把 `AutoCombatService` 描述为“per exact taskRun”且注入 immutable exact `TaskExecutionContext`，同时又要求
  resume 后旧 context 永久失效。same taskRun resume 会生成更高 runRevision 的新 context；旧实例不能接受它。若按“typed
  unwind 后释放实例、新建实例”处理，`AutoCombatRuntimeState` 中 pending recovery、expected-exit watch、维护时间锚等会丢失；
  若继续复用旧实例，则所有新 revision 调用都会与 bound context 不匹配。
- 影响：pause/resume 可能丢战斗退出、补给与 maintenance 状态，或让 resumed run 永久不可调用，本地基线不等价。
- 精确返修条件：Repair #2 必须把“稳定 run identity”和“current revision context”分开。推荐一个由同 assembly
  non-mintable activation capability 持有的 **per-taskRun current-context slot**：只在 current confirmed ACTIVE revision
  原子替换 context；Service 实例和 `AutoCombatRuntimeState` 整个 taskRun 保留；无参 API 每次从 capability 取得 current
  context，自带 context 的 API 与 capability 当前值做 full exact compare。旧 revision 永久拒绝，resume 不丢 runtime state。
  也可给出等价的 retained runtime-state owner + per-revision Service 重建，但必须列 exact non-public owner/constructor/文件写集，
  不得 ThreadLocal、public raw rebind 或把 State 塞入 H 的 `GameContext.State` 未批准字段。

### P1-2：terminal cleanup 会削弱 HEAD 的跨 run 30 秒 verify guard

- 证据：HEAD `RefreshDuePanelVerifyGate.lastVerifyByTeam` 是 singleton Service 内 `ConcurrentHashMap`，没有 run-terminal
  cleanup；R2 改为“run 终结清除该窗口贡献键”。当同 team/window 很快启动下一 run 时，HEAD 仍会按 30 秒 guard defer，
  新设计会立即放行。
- 影响：改变面板验证频率和输入时序，属于未批准业务差异；同时 `teamKey` 与 `windowId` fallback 不是同一 ownership，按
  window terminal 清理也无法可靠定位 team key。
- 精确返修条件：删除 run-terminal cleanup。该 gate 保持 tenant host 生命周期内共享、原 key/原 30 秒判断/原时间值，
  仅 host close 整体释放；不得新增 TTL、逐 run 清理或窗口注销清理。若认为容量必须改变，另开明确行为 CR，不在迁移里顺手改。

Repair #2 只需修以上两点并重列 W0 current-context capability 与 W2 exact 文件/构造图；R1 paused observer 仍作为独立
未批准前置，W1 七个 collaborator 仍须先形成可编译最终合同。AutoCombat Java/Maven/resources/tests、host/cohort 继续冻结。
**无已批准业务差异；按基线等价迁移。**

## External Worker A - Design Repair #2 - 2026-07-12

针对 `Parent Design Review #2 - BLOCKED`（P0=0/P1=2/P2=0）只修两点并重列 W0 capability 与 W2 文件/构造图。已保留项不动：R1 PAUSED observer 独立前置切片、W0-W3 DAG、public API 零加参、monotonic timer 映射、W1 七协作最终合同。仅设计；Java/Maven/resources/tests、host/cohort 继续冻结。

### R1（P1-1）：stable run identity 与 current revision context 分离——per-taskRun current-context slot

采纳父级推荐方案：

- **新 W0 前置类型 `remote/CloudTaskRunCurrentContextSlot.java`**（Cloud, New）：
  - 构造 package-private，仅由 `CloudTaskRunAuthorityAssembly` 的 non-mintable activation capability 在 run 激活时铸造，一 taskRun 一 slot；host/business 代码不可自建。
  - `public TaskExecutionContext current()`：返回当前已安装的 confirmed-ACTIVE revision context；slot 已终结（stop/complete/close）时抛 typed terminal unwind。
  - package-private `install(TaskExecutionContext next)`：**原子替换**，仅接受同 taskRunId、同 window 元组、`next.runRevision > current.runRevision` 且来源于已批准 resume reconcile-confirm 合同产出的 confirmed-ACTIVE context（activation owner 是唯一调用方，其 mint 本就被该合同门禁）；旧/等值 revision、身份漂移、terminal 后 install 一律 typed 拒绝——旧 revision 永久拒绝。
  - package-private `close(reason)`：stop/complete/typed unwind 终结时置空；实例内存随 host close 释放。
- **`AutoCombatService` 生命周期改绑 slot 而非单个 context**：per-taskRun 实例构造注入 `CloudTaskRunCurrentContextSlot`（不再注入 immutable context）。实例与 `AutoCombatRuntimeState`（pending recovery、expected-exit watch、维护时间锚、探测授权、队员覆盖粘滞等全部字段）**整个 taskRun 存续**，pause/resume 零丢失。
  - 无参 public 方法：每次调用 `slot.current()` 取现行 context（revision 前进对本类透明；机械委托的 revision 正确性仍由三道闸权威保证）。
  - 自带 context 参数的 public 方法：入口与 `slot.current()` 做 **full exact compare**（scope 四元组、taskRunId、taskType、window 四元组、stopEpoch、runRevision 逐项相等），不匹配抛 typed `IllegalStateException`；绝不静默采用传入值，无 ThreadLocal、无 public raw rebind。
  - 等价性论证：HEAD 的 runtime state 按 windowId 常驻 singleton map、跨 pause/resume 天然保留；本方案 per-run 实例 + slot 换代 = 同一保留语义 + 更严的 revision 拒绝。epoch 漂移在 HEAD 触发 state reset，在云端表现为 run 级 fence 拒绝 + 新 run 新实例，行为面不放宽。不占用、不假设 Worker H `GameContext.State` 的任何未批准字段（本类仅经其已批准 read/write 面消费）。

### R2（P1-2）：撤销 verify gate 的 run-terminal 清理，保持 HEAD 跨 run 语义

- 撤回 Repair #1 中"run 终结清除该窗口贡献键"的设计。`RefreshDuePanelVerifyGate` 保持 HEAD 形态：nested `public static class` 不变；其共享实例注册为 **tenant/user host 生命周期 bean**（一 host 一实例），`ConcurrentHashMap` 原键语义（teamKey 空则 windowId 兜底）、原 30_000ms guard、原时间值判定逐字保留（时基按 R4 单调源）。
- 生命周期：仅 host close 随容器整体释放；**无 TTL、无逐 run 清理、无窗口注销清理**。同 team/window 快速重启下一 run 时 30s guard 照 HEAD defer。容量维持 HEAD 语义（键数≈team/window 数）；如未来需要容量策略，另开显式行为 CR，不在迁移顺手改。

### W0 / W2 重列（含构造图）

- **W0 追加**：`CloudTaskRunCurrentContextSlot`（上述）。W0 其余不变：PAUSED read-only observation 前置切片（独立门，未批准前 AutoCombat Java 冻结）、`CloudTaskTurnCoordination`、`CloudAutoBattleProperties`、Worker H State owner。
- **W2 写集（父级批准后才进入 Java）**：仅 `com/bot/dhxy/service/AutoCombatService.java`（New；含 nested TickResult/Policy/RuntimeState/Decision/Gate 全部原样）。无其它新文件；gate 共享实例是该 nested 类型的 host bean 注册，不新增类文件。
- **构造图**：`CloudServiceHost`(tenant/user) → 注册 W1 七协作 bean + `CloudTaskTurnCoordination` + `CloudAutoBattleProperties` + Worker H State owner + `AutoCombatService.RefreshDuePanelVerifyGate` host bean（host 生命周期）→ run 激活时 activation capability 铸造 `TaskExecutionContext` + `CloudTaskRunCurrentContextSlot` → 构造 per-taskRun `AutoCombatService`(7 协作, turnCoordination, gate bean, slot, monotonicMillis LongSupplier, properties) → W3 caller 切片按原调用形状接线。resume：activation owner 经 reconcile-confirm 合同 mint 新 context → `slot.install`（原子换代）→ 同一 Service 实例继续，状态零丢失。stop/complete：`slot.close` + 实例随 run 释放（gate bean 不动）。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。Design Repair #2 到此停止，等待父级复审；heartbeat 保持。

## Parent Design Review #3 - DESIGN APPROVED - 2026-07-12

Repair #2 已把 stable taskRun identity、current ACTIVE revision context 与 retained `AutoCombatRuntimeState` 分开，并撤销
run-terminal verify-gate cleanup，关闭上一轮两个 P1。结论：**DESIGN APPROVED，P0/P1/P2=0**。以下实现约束直接绑定，
不再要求 Repair #3：

1. `CloudTaskRunCurrentContextSlot` 必须是 public final capability type，但 constructor、`install`、terminal close 全非 public，
   只能由同 assembly activation owner 铸造/推进。slot 固定保存 full stable key：scope 四元组、taskRunId、taskType、window
   四元组、non-terminal stopEpoch；不得只比 taskRunId/window。
2. `current()` 每次返回前必须对已安装 context 执行现有 typed current-confirmed ACTIVE gate；PAUSED、stale、future/
   unconfirmed、STOPPED、COMPLETED、denied 原样 typed unwind，不能返回旧 ACTIVE context 给无参 Service API 继续。
3. `install(next)` 必须在 CAS 前验证 next 的 full stable key exact、revision 严格前进，并执行一次 current-confirmed ACTIVE
   typed gate；slot terminal 后不可 install。旧/等 revision 和 identity drift 均拒绝，不内部 retry。
4. Repair 文本中“stop/complete/typed unwind close”收窄为：**只有 exact current STOPPED/COMPLETED terminal evidence 才 close**。
   PAUSED、旧 revision stale unwind、普通 denied 均不得 close slot、Service 或 runtime state；它们只阻止调用，等待 confirmed
   resume install。terminal close 幂等并永久拒绝 current/install。
5. `AutoCombatService` 整个 taskRun 复用同一 slot + 同一 `AutoCombatRuntimeState`；无参 API 每次取 `slot.current()`，带 context
   API 与 slot current 做 full exact compare。不得 ThreadLocal/holder/public rebind。`RefreshDuePanelVerifyGate` 仅 tenant host
   close 释放，原 key/30 秒/跨 run 行为保持，无 TTL 或逐 run/window cleanup。
6. Java 分波门保持：先独立设计/批准并实现 W0 PAUSED read-only observer、current-context slot、turn/config/H owner 和 W1 七个
   最终 collaborator 合同；这些未齐前不得写 `AutoCombatService`。每波给出 exact 文件写集并 fresh package，host/caller cohort
   始终 dormant，生产只整体切换。

现允许 A 进入第一个尚未实现的 W0 前置设计；不得直接写 AutoCombat Java。A 下一材料应追加
`External Worker A - W0 PAUSED Read-only Observer Design #1`，列 Cloud/DHXY exact schema/digest/authorization/broker/local
handler 写集及 input 禁止矩阵；父级批准前该 W0 Java 仍冻结。**无已批准业务差异；按基线等价迁移。**

## External Worker A - W0 PAUSED Read-only Observer Design #1 - 2026-07-12

依据 `Parent Design Review #3 - DESIGN APPROVED` 的指令交付 W0 前置能力设计。仅设计；本 W0 Java 在父级批准前冻结。目标：让 exact paused run 在其**当前 PAUSED revision** 上获得仅限 `WINDOW_FACT`/`CAPTURE` 的 typed 只读观察授权（HEAD `probePausedWindowCombatStateReadOnly` 语义的云端能力底座），输入与身份换新全链路拒绝。

### 1. Wire/schema（双仓对称，digest 自动覆盖）

- 新枚举 `RemoteObservationMode { PAUSED_READ_ONLY }`（Cloud `remote/`、DHXY `cloud/remote/` 各一，New）。
- Cloud `RequestContext`（Modify）：新增**可空**组件 `observationMode`；非空时必须为 `PAUSED_READ_ONLY`。canonical mapper 为 NON_NULL：普通请求不携带该键，**全部既有 requestDigest 字节不变**；携带时自动进入 digest。
- Cloud `RemoteCommandEnvelope`（Modify）：透传 + 校验（仅 `WINDOW_FACT`/`CAPTURE` 可携带；`EXECUTE_INPUT_BUNDLE` 携带即构造期拒绝）。
- DHXY `RemoteGameCommand`（Modify）：新增可空 `String observationMode`；`RemoteProtocolDigests.computeRequestDigest`（Modify）仅在非空时写入 `context.observationMode`（与 Cloud NON_NULL canonical 字节一致）。
- DHXY `HttpRemoteCommandTransport.validateCommand`（Modify）strict schema：`observationMode` 出现时必须逐字等于 `PAUSED_READ_ONLY` 且 operation ∈ {CAPTURE, WINDOW_FACT}，否则 SCHEMA_MISMATCH；未出现时行为零变化。

### 2. Cloud 授权门（coordinator/broker，无 lifecycle 迁移语义变更）

- `RemoteTaskRunCoordinator.authorize / authorizeAndMarkDispatch`（Modify）：追加可空 `observationMode` 参数。
  - 为空：现行为逐字不变（ACTIVE + confirmed + revision 相等）。
  - `PAUSED_READ_ONLY`：在既有 scope/taskRunId/window 四元组/stopEpoch 全等前提下，要求 `binding.status == PAUSED` 且 `binding.runRevision == expectedRunRevision`（即**当前 PAUSED revision**，不看 confirmed map——paused revision 定义上未确认）；status 非 PAUSED（含 ACTIVE）→ typed 拒绝 `observation requires PAUSED`；stale/future revision、terminal、session/window drift → 既有 typed 拒绝原样。纯只读判定分支，零状态写。
- `RemoteGameCommandBroker`（Modify）：入队与 dispatch 两门把 `context.observationMode` 传入 coordinator；`EXECUTE_INPUT_BUNDLE` 携带 marker → 即时 `NOT_EXECUTED/INVALID_REQUEST`（协议层已拒，broker 防御性复判）。普通命令与 PAUSED→`TASK_RUN_PAUSED` 映射零变化。
- `CloudTaskRunExecutionGate`（Modify，package-private 面）：
  - 新增 package-private `createPausedObservationContext(scope, taskRunId)`：find 当前 binding，要求 status==PAUSED，快照（runRevision=paused revision）。仅 activation/observer 适配层（同 assembly）可铸造，公开面零变化。
  - fact/capture 构建器新增 observation 变体：写入 `observationMode=PAUSED_READ_ONLY`，逐次校验改走 paused 分支（同上判定）；**bind-or-verify 同字节、retained handle、requestId/actionId 规则与既有合同逐字一致**。输入构建器无 observation 变体（编译期不存在该路径）。
- Ledger 零修改。换新（renewal）禁止的执行点：唯一能调 package-private `renewAfterNotExecuted` 的 retained-state 适配层在调用前查 coordinator `find`，**binding.status==PAUSED 时对 observation 标记的 attempt 拒绝换新**（合同条款，随适配层切片实现）；未解析 UNKNOWN 不可换新的既有冻结规则不变。

### 3. DHXY 本地门（handler）

- `LocalRemoteGameCommandHandler`（Modify）：`classifyRemoteRun`/`requireRegistration` 增加 observation 分支——`command.observationMode==PAUSED_READ_ONLY` 且 operation ∈ {CAPTURE, WINDOW_FACT} 时，`registration.status==PAUSED` 且 `registration.runRevision == command.runRevision` 即按 ACTIVE 等价放行只读执行；revision 不等 → `TASK_RUN_MISMATCH`；STOPPED/terminal/session/window drift → 既有 typed 拒绝。普通命令的 PAUSED→`TASK_RUN_PAUSED` 与 pre-side-effect revision 门零变化。输入路径（含 worker-admission 围栏）不识别 marker——防御性再拒。
- Registry/OperationLedger/PollingLoop 零修改：observer 命令即普通 fact/capture 命令，走既有机械传输与幂等台账。

### 4. Input 禁止矩阵（逐门裁决）

| operation | marker | run 状态 | Cloud 构造期 | Cloud 入队门 | Cloud dispatch 门 | DHXY schema | DHXY 副作用前门 | input worker 准入 |
|---|---|---|---|---|---|---|---|---|
| WINDOW_FACT/CAPTURE | 无 | ACTIVE+confirmed+rev= | 放行 | 放行 | 放行 | 放行 | 放行 | n/a |
| WINDOW_FACT/CAPTURE | 无 | PAUSED | 放行 | TASK_RUN_PAUSED | TASK_RUN_PAUSED | 放行 | TASK_RUN_PAUSED | n/a |
| WINDOW_FACT/CAPTURE | PAUSED_READ_ONLY | PAUSED + rev=paused rev | 放行 | **放行（只读分支）** | **放行（只读分支）** | 放行 | **放行（只读分支）** | n/a |
| WINDOW_FACT/CAPTURE | PAUSED_READ_ONLY | ACTIVE / rev≠ / terminal / drift | 放行 | typed 拒绝 | typed 拒绝 | 放行 | typed 拒绝 | n/a |
| EXECUTE_INPUT_BUNDLE | PAUSED_READ_ONLY | 任意 | **构造期拒绝** | INVALID_REQUEST | INVALID_REQUEST | SCHEMA_MISMATCH | 拒绝 | 不可达 |
| EXECUTE_INPUT_BUNDLE | 无 | PAUSED | 放行 | TASK_RUN_PAUSED | TASK_RUN_PAUSED | 放行 | TASK_RUN_PAUSED | 已批准 revision 围栏 |
| 任意 renewal（observation attempt，run PAUSED） | — | — | 适配层拒绝（§2） | — | — | — | — | — |

### 5. Exact 写集汇总

- Cloud（2 New + 4 Modify）：New `remote/RemoteObservationMode.java`；Modify `remote/RequestContext.java`、`remote/RemoteCommandEnvelope.java`、`remote/run/RemoteTaskRunCoordinator.java`、`remote/RemoteGameCommandBroker.java`、`remote/CloudTaskRunExecutionGate.java`。（注：New 1 + Modify 5。）
- DHXY（1 New + 3 Modify）：New `cloud/remote/RemoteObservationMode.java`（如按既有风格用 String 常量则免；推荐 enum）；Modify `cloud/remote/RemoteGameCommand.java`、`cloud/remote/RemoteProtocolDigests.java`、`cloud/remote/HttpRemoteCommandTransport.java`、`cloud/remote/LocalRemoteGameCommandHandler.java`。（1 New + 4 Modify。）
- 禁碰确认：`TaskPauseToken`、`RemoteTaskRunLifecycleService`、`RemoteTaskRunRegistry`、`RemoteOperationLedger`、`RemoteCommandPollingLoop` 零修改；无新增线程/TTL/takeover/自动 retry/测试；host/cohort dormant。
- 验证（批准后）：Cloud `mvn -q clean package`（禁 skip）+ DHXY `mvn -q -DskipTests compile`；双仓 digest parity 用同一 observation 请求字节对拍。

### 6. 风险与边界

- 普通流量零影响：marker 缺省即旧字节、旧判定路径（NON_NULL canonical + 参数为空走原分支）。
- 观察不产生副作用：fact/capture 本身无输入；PAUSED revision 不可确认、不可 mint 业务 context（slot `current()` 的 confirmed-ACTIVE 门不受本能力影响，binding 约束 #2 保持）。
- resume 后：paused observation context/handle 因 revision 前进照常失效；观察期 UNKNOWN 留待 same-bytes 重投或保持未解析（不可换新）。
- P0/P1/P2：无已知未决（本 W0 范围内）。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。W0 Design #1 到此停止，等待父级批准；heartbeat 保持。

## Parent W0 Design Review #1 - BLOCKED - 2026-07-12

父级已对照当前 `CloudTaskRunExecutionContext`、`CloudTaskRunExecutionGate`、
`CloudTaskRetainedActionState`、`CloudTaskRunActionLedger`、coordinator/broker 两道门及 DHXY transport/handler。
PAUSED exact revision、fact/capture-only、Cloud enqueue/final-dispatch 与 DHXY pre-side-effect 三道 revision fence 的方向成立，
但当前设计尚未形成可安全实施的 capability/identity 闭环。结论：**BLOCKED，P0=0/P1=2/P2=2**。

### P1-1：PAUSED observer 复用 confirmed-ACTIVE execution context，能力类型未隔离

- 证据：Design #1 写集只新增 observation enum，没有独立 observer context；因此
  `createPausedObservationContext(...)` 只能返回现有 `CloudTaskRunExecutionContext`。该类当前明确表示
  “confirmed-ACTIVE binding”，同时是 `CloudTaskServiceExecutionContext`、retained state、ledger 和 input request builder 的
  通用参数。
- 影响：PAUSED read-only capability 与 ACTIVE Task/Service capability 在类型层混为一体，后续 observer adapter 可以误接完整
  Service/retained-action 构造图；即使 broker/local 最终会拒绝输入，也破坏已批准的“paused observer 不 mint 业务 execution
  context/slot”边界，并把安全性留给运行期偶然拒绝。
- 返修条件：新增真实独立、不可外部构造的 `CloudPausedReadOnlyObservationContext`（或等价专用类型），只暴露 exact
  scope/taskRun/taskType/window/stopEpoch/pausedRevision；只能构造 WINDOW_FACT/CAPTURE，不能传入
  `CloudTaskServiceExecutionContext`、`CloudTaskRetainedActionState`、input builder、current context slot 或 TaskExecutionContext。
  gate 的 active API 与注释保持 confirmed-ACTIVE 原义。

### P1-2：稳定 identity 与“PAUSED 禁止 renewal”没有落在本切片权威中

- 证据：当前 `CloudTaskRunActionLedger.acquire(...)`/`renewAfterNotExecuted(...)` 都接收
  `CloudTaskRunExecutionContext`，ledger 本身不校验 ACTIVE/PAUSED；Design #1 又明确 ledger 零修改，只承诺“未来适配层调用前
  find 并拒绝换新”。现有 gate 构建 fact/capture 仍要求 ledger-owned identity，但设计没有给专用 observer identity owner、
  stable business action key 生命周期或不可 renewal 的实际入口。
- 影响：W0 实现后要么没有合法调用链取得稳定 requestId/actionId/captureId，要么只能让未来同包 adapter 直接调用通用
  ledger，并依赖约定阻止 PAUSED NOT_EXECUTED 换新；这不满足 retained authority 必须自行强制稳定 identity/同字节重投的门禁。
- 返修条件：在本 W0 写集中落地专用 package-private observer retained state/identity API，仍由唯一共享 ledger 持有稳定 IDs 与
  bound request bytes，但 owner 必须结构性禁止 renewal，只允许同一 paused revision 的 exact retained request redelivery；
  resume/stop/complete 后旧 request 由 revision/status fence 永久拒绝。不得把禁令推迟到未来 caller convention。

### P2-1：DHXY 已声明 enum，却把 wire 字段设计为 raw String

- 证据：Design #1 同时新增 `RemoteObservationMode` enum，又把 `RemoteGameCommand.observationMode` 写成可空 String。
- 影响：Cloud/DHXY 类型合同可漂移，handler/schema/digest 需要重复字符串判定。
- 返修条件：DHXY DTO 使用 typed nullable `RemoteObservationMode`；strict schema 对未知枚举值明确 SCHEMA_MISMATCH，digest 写
  enum canonical name。

### P2-2：strict schema 没有关闭显式 null 与缺省的判定差异

- 证据：当前 transport 反序列化后只校验 DTO 字段，无法区分键缺失与 `"observationMode": null`；Design #1 却要求“出现时
  必须逐字等于 PAUSED_READ_ONLY”，同时 canonical digest 用 NON_NULL 把 null 当缺省删除。
- 影响：实现无法兑现其 strict-schema 声明，wire 可产生未被 digest 区分的非 canonical 表示。
- 返修条件：明确只接受“键缺失”或 canonical enum 值；用 raw response schema presence 检查、Jackson null-fail setter 或等价
  机制拒绝显式 null。同步修正精确写集数量（当前正文 Cloud/DHXY 两处标题计数均与实际清单不一致）。

Worker A 只追加 `W0 Design Repair #1`，先给出独立 observer capability、专用 retained identity/无 renewal 调用链、typed wire 与
canonical-null 规则及修正后的 exact 文件表；父级 DESIGN APPROVED 前仍不得改 Java。其余 AutoCombat Repair #2 继续保持已批准，
不回退。**无已批准业务差异；按基线等价迁移。**

## External Worker A - W0 Design Repair #1 - 2026-07-12

针对 `Parent W0 Design Review #1 - BLOCKED`（P0=0/P1=2/P2=2）逐条返修。仅设计；W0 Java 冻结。已保留项不回退：PAUSED exact revision、fact/capture-only、三道 fence、AutoCombat 主设计 Review #3 全部绑定约束。

### R1（P1-1）：独立不可构造的 `CloudPausedReadOnlyObservationContext`

- **新类型**（Cloud `remote/`，New）：`public final class CloudPausedReadOnlyObservationContext`，private 构造 + 仅 `CloudTaskRunExecutionGate` 的 package-private `snapshotPausedObservation(binding)` 工厂可铸造（find 当前 binding、要求 status==PAUSED 后逐字段快照）。只暴露只读访问器：scope 四元组、taskRunId、taskType、window 四元组、stopEpoch、`pausedRunRevision`，以及 wire 派生 `windowRef()/stopRef()`。
- **类型级隔离**（编译期不可误用）：它与 `CloudTaskRunExecutionContext` 无继承/转换关系；`CloudTaskServiceExecutionContext` 构造器、`CloudTaskRetainedActionState`、input builder、`CloudTaskRunCurrentContextSlot.install`、`TaskExecutionContext` 桥接均只收 `CloudTaskRunExecutionContext`——observer 类型在这些入口全部编译不过，"paused observer 不 mint 业务 execution context/slot" 由类型系统强制，不依赖运行期拒绝。
- gate 的 active API（`createContext/validate/new*Request`）与注释保持 confirmed-ACTIVE 原义零改动；observation 构建器是并列的独立方法族，仅收新类型。

### R2（P1-2）：observer retained identity 落在共享 ledger 的结构性权威内（本 W0 写集内落地）

- 撤回 Design #1 "ledger 零修改"。**Modify `remote/CloudTaskRunActionLedger.java`**：
  - `RetainedActionIdentity` 增加不可变 `observation` 标记与 `pausedRunRevision` 字段（普通身份为 false/-1，字节面不变）。
  - 新 package-private `acquireObservation(CloudPausedReadOnlyObservationContext ctx, RemoteOperation op, String key)`：op 仅 WINDOW_FACT/CAPTURE（编译/校验双限），键=taskRunId+canonical key，一次铸造稳定 requestId/actionId(/captureId)，记录 observation=true 与 ctx.pausedRunRevision；同键重取返回同 handle（同字节重投路径）。
  - **结构性禁令**：`renewAfterNotExecuted` 对 `observation==true` 的记录一律抛 typed `IllegalStateException`（禁令在唯一 ledger 权威内，不是 caller convention）；`bindOrVerifyRequest`/`recordOutcome` 的同字节/同 digest/幂等规则对 observer 记录逐字沿用。
  - observer 记录的 request 绑定 pausedRunRevision；resume/stop/complete 后旧 request 被 coordinator/broker/handler 的 revision/status fence 永久拒绝（既有机制，零新语义）。
- **Modify `remote/CloudTaskRunExecutionGate.java`**：observation 构建器签名收敛为 `(CloudPausedReadOnlyObservationContext, RetainedActionIdentity[observation==true 且 pausedRunRevision==ctx.pausedRunRevision], timeoutMs)`，构建前逐次重验 binding 仍 PAUSED 且 revision 相等（coordinator 只读分支），写入 `observationMode=PAUSED_READ_ONLY` 后走既有 digest/bind 流程。合法调用链闭环：gate 铸 ctx → ledger `acquireObservation` 铸稳定身份 → gate 构建 → executor 发送记录（executor 面按 W1 observer 适配切片消费，本 W0 只交付权威底座）。

### R3（P2-1）：DHXY typed enum

- DHXY `RemoteGameCommand.observationMode` 类型改为 **nullable `RemoteObservationMode` enum**（非 String）；`RemoteProtocolDigests` 非空时写 `enum.name()` canonical 文本（与 Cloud 同字节）；`HttpRemoteCommandTransport` strict schema 只判 operation 限定（enum 合法性由反序列化承担，见 R4）；未知枚举值在 Jackson 反序列化即失败 → 走既有 DESERIALIZATION typed 拒绝路径（SCHEMA_MISMATCH 等级）。

### R4（P2-2）：显式 null 关闭 + canonical 唯一表示

- DHXY `RemoteGameCommand.observationMode` 字段标注 `@JsonSetter(nulls = Nulls.FAIL)`：wire 上只接受**键缺失**（=无观察）或 canonical enum 值；`"observationMode": null` 在反序列化即 fail → typed 拒绝，杜绝"null 与缺省在 digest 中不可区分"的非 canonical 表示。Cloud 侧 `RemoteCommandEnvelope` 构造校验同规则（null 组件=缺省合法；显式 null 无法进入 record 非法态）。canonical digest 双侧继续 NON_NULL：缺省不写键，出现必为 enum name。

### R5：修正后的 exact 文件表（更正 Design #1 计数错误）

- **Cloud：2 New + 6 Modify**
  - New `remote/RemoteObservationMode.java`、New `remote/CloudPausedReadOnlyObservationContext.java`
  - Modify `remote/RequestContext.java`（nullable enum 组件，NON_NULL digest 零扰动）、`remote/RemoteCommandEnvelope.java`、`remote/run/RemoteTaskRunCoordinator.java`（authorize/authorizeAndMarkDispatch 只读分支）、`remote/RemoteGameCommandBroker.java`（两门透传 + input 携带即拒）、`remote/CloudTaskRunExecutionGate.java`（R1/R2）、`remote/CloudTaskRunActionLedger.java`（R2）
- **DHXY：1 New + 4 Modify**
  - New `cloud/remote/RemoteObservationMode.java`
  - Modify `cloud/remote/RemoteGameCommand.java`（typed nullable enum + Nulls.FAIL）、`cloud/remote/RemoteProtocolDigests.java`、`cloud/remote/HttpRemoteCommandTransport.java`、`cloud/remote/LocalRemoteGameCommandHandler.java`（PAUSED_READ_ONLY 只读分支：PAUSED + revision 相等 + fact/capture 放行，其余 typed 拒绝）
- 禁碰确认不变：`TaskPauseToken`、`RemoteTaskRunLifecycleService`、`RemoteTaskRunRegistry`、`RemoteOperationLedger`、`RemoteCommandPollingLoop` 零修改；无线程/TTL/takeover/自动 retry/测试；host/cohort dormant。input 禁止矩阵沿用 Design #1 §4，另加一行：`acquireObservation(op=EXECUTE_INPUT_BUNDLE)` → ledger 构造期拒绝。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。W0 Design Repair #1 到此停止，等待父级复审；heartbeat 保持。

## Parent W0 Design Review #2 - BLOCKED - 2026-07-12

Repair #1 已关闭“PAUSED observer 与 confirmed-ACTIVE context 同型”和“禁止 renewal 只靠未来 caller 约定”两个原始
方向性缺口；typed enum、显式 null fail-closed 与三道 revision fence 也保留成立。但当前 ledger 的真实对象模型仍无法按
所列文件表安全实现，且 wire 拒绝分类/协议工件尚未一致。父级结论：**BLOCKED，P0=0/P1=1/P2=2**。

### P1-1：`RetainedActionIdentity` 仍只有 ACTIVE context 槽，且同 key 的 ACTIVE/observer 冲突语义未闭合

- 证据：当前 `CloudTaskRunActionLedger.RetainedActionIdentity` 的 `context` 字段和 constructor 参数固定为
  `CloudTaskRunExecutionContext`（当前文件约 L331-L360）；`acquire/requireOwnedCurrent/renewAfterNotExecuted` 均直接读取
  `identity.context()`。`CloudTaskRetainedActionState` 也在 L120/L147/L180 用该 ACTIVE context 做引用相等与 owner 校验。
  Repair #1 只写“增加 observation 标记与 pausedRunRevision”，没有说明如何让独立
  `CloudPausedReadOnlyObservationContext` 成为 identity 的真实不可混淆 owner，也没有列出所有 ACTIVE caller 的兼容改法。
  同时 ledger key 仍是 `(taskRunId,businessActionKey)`；若 ACTIVE 与 PAUSED observation 复用 key，现有 record 只有 operation
  校验，会发生模式碰撞或把 observation 当 ACTIVE handle 返回。
- 影响：按当前文本实施要么无法编译，要么只能把 owner 降成 nullable/raw `Object`/双字段未约束对象；后者会把 ACTIVE
  Service port、renewal 与 PAUSED observer identity 再次混权。same-key 重取也无法证明是同一 paused capability/revision。
- 返修条件：Repair #2 必须明确一个可编译的 mutually-exclusive typed identity 形状，例如独立 ACTIVE/OBSERVATION
  identity subtype/record，或 constructor 强制二选一的两个 typed owner 槽并提供 active-only/observation-only 校验入口；禁止
  raw `Object`、cast、两个 owner 同时非空或都为空。逐项列出 `CloudTaskRetainedActionState`、gate、executor 对 ACTIVE
  `context()` 的保持方式。ledger 的 record/key 必须模式精确：同 business key 的 mode/operation/owner/revision exact 才幂等
  返回；ACTIVE/OBSERVATION 冲突必须 fail-closed。observer owner 等价必须按 immutable exact fields 或原 capability reference
  定义，不能因新建一个字段相同的 paused snapshot 意外 mint 第二套 ID。

### P2-1：未知 enum/显式 null 的现有 typed failure 是 `DESERIALIZATION`，不是 `SCHEMA_MISMATCH`

- 证据：DHXY `HttpRemoteCommandTransport.deserializePollResponse` 当前在 L201-L213 捕获
  `JsonProcessingException` 并构造 `FailureType.DESERIALIZATION`；`SCHEMA_MISMATCH` 只发生在 DTO 已成功反序列化后的结构校验。
  Repair #1 R3/R4 却写成“反序列化即失败 -> SCHEMA_MISMATCH 等级”。
- 影响：实现、运维指标和文档会对同一拒绝给出不同 typed code，无法按报告验收。
- 返修条件：二选一并写死：接受现有 `DESERIALIZATION` 作为 unknown/null 的 canonical fail-closed；或在 typed decode 前增加
  raw schema presence/value 检查并证明落到 `SCHEMA_MISMATCH`。不得只改文字声称现有 Jackson 路径会自动改分类。

### P2-2：协议 schema 未列入 exact 写集

- 证据：本能力新增 wire 字段、canonical digest 与 PAUSED-only 拒绝矩阵，但 R5 只列双仓 Java，遗漏
  `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`。
- 影响：双仓可编译也会留下协议文档与实际 digest/schema 不一致，后续 transport/handler 返修没有稳定合同。
- 返修条件：Repair #2 把协议 schema 文档加入唯一写集，定义 omission vs canonical enum、digest 字段、operation/status/revision
  拒绝规则及 unknown/null 的最终 typed failure 分类；仍不得新增测试、host/caller 或运行面。

Worker A 只追加 `External Worker A - W0 Design Repair #2`；Java/Maven/resources/tests 继续冻结。AutoCombat 主设计 Review #3
保持批准，不回退。**无已批准业务差异；按基线等价迁移。**

## External Worker A - W0 Design Repair #2 - 2026-07-12

针对 `Parent W0 Design Review #2 - BLOCKED`（P0=0/P1=1/P2=2）逐条返修。仅设计；Java/Maven/resources/tests 冻结。已成立项不回退：独立 observer context 类型、typed enum、显式 null fail-closed、三道 revision fence、主设计 Review #3 批准。

### R1（P1-1）：互斥 typed identity——ACTIVE 类型零改动 + 独立 OBSERVATION 类型

采用"独立 subtype"方案（父级选项一），**完全不触碰现有 ACTIVE 对象模型**：

- **`RetainedActionIdentity` 逐字不动**：`context` 槽仍是 `CloudTaskRunExecutionContext`，`acquire/requireOwnedCurrent/renewAfterNotExecuted/bindOrVerifyRequest/recordOutcome` 的 ACTIVE 路径、以及 `CloudTaskRetainedActionState`（L120/L147/L180 的引用相等与 owner 校验）、gate、executor 的全部 ACTIVE caller **零兼容改动、原样编译**。
- **新增并列类型 `ObservationActionIdentity`**（ledger 内 nested `public static final class`，private 构造，仅 ledger 铸造）：不可变字段 = owner ledger 引用、`CloudPausedReadOnlyObservationContext observationCapability`（capability 引用本体）、taskRunId、operation（构造校验 ∈ {WINDOW_FACT, CAPTURE}）、businessActionKey、requestId/actionId(/captureId)、pausedRunRevision。与 `RetainedActionIdentity` 无继承/无转换：**`renewAfterNotExecuted(RetainedActionIdentity)` 在类型上就收不下 observation 身份——renewal 禁令由方法签名结构性强制**，无 raw Object、无 cast、无双槽。
- **ledger 存储与 key 模式精确**：观察记录存独立 `Map<ObservationKey, ObservationRecord>`，`ObservationKey=(taskRunId, businessActionKey, pausedRunRevision)`。
  - **模式冲突 fail-closed（双向）**：`acquireObservation` 先查 ACTIVE map 的 `(taskRunId,key)`，命中即 typed `IllegalStateException(MODE_CONFLICT)`；ACTIVE `acquire` 对称地先查观察 map（任意 revision 的同 business key），命中即拒。同 business key 永不跨模式复用或静默返回另一模式 handle。
  - **幂等重取的 exact 条件**：同 `(taskRunId,key,pausedRunRevision)` 且 `operation` 相等且 `record.observationCapability == ctx`（**引用相等**）→ 返回同一 handle（同字节重投路径）；operation 不等、revision 不等、或 capability 引用不同（含字段全同的新建 paused snapshot）→ typed 拒绝，**绝不意外铸第二套 ID**。新一轮 pause（新 pausedRunRevision）由 gate 铸新 capability + 新 key 元组，是真正的新观察动作，与旧记录互不污染；观察记录计入既有 retained 硬配额，无 TTL 无清理。
  - `bindOrVerifyRequest`/`recordOutcome` 增加 observation 重载（内部共用既有字节/digest/幂等私有路径）：同字节绑定、exact correlation、`unrecorded→UNKNOWN→单一 final` 转换逐字沿用；observation 终态集不含 EXECUTED（无输入）。
- **gate 观察构建器**：`(CloudPausedReadOnlyObservationContext ctx, ObservationActionIdentity id, timeoutMs)`，构建前校验 id 属绑定 ledger、`id.observationCapability() == ctx`（引用）、operation 匹配、coordinator 只读分支重验 PAUSED+revision 相等。executor/adapter 消费面属 W1 observer 切片，不变。

### R2（P2-1）：拒绝分类写死——采用现有 `DESERIALIZATION` 为 canonical

选定并写死（父级选项一）：未知 enum 值与显式 `"observationMode": null` 的 fail-closed 分类 = 现有
`HttpRemoteCommandTransport.deserializePollResponse`（L201-L213）捕获 `JsonProcessingException` 产生的
`FailureType.DESERIALIZATION`——`RemoteObservationMode` 未知常量由 Jackson enum 解析失败触发、显式 null 由字段
`@JsonSetter(nulls = Nulls.FAIL)` 触发，两者天然落入该既有路径，**不改分类代码、不另造检查层**。
`SCHEMA_MISMATCH` 保留给 DTO 成功反序列化后的结构校验：`observationMode` 出现于 `EXECUTE_INPUT_BUNDLE` →
`validateCommand` 的 SCHEMA_MISMATCH。运维/文档/验收统一按此两级分类，Repair #1 中"反序列化即失败 → SCHEMA_MISMATCH 等级"的表述作废更正。

### R3（P2-2）：协议 schema 文档入写集

唯一写集追加 **Modify `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`**（DHXY docs）：
- `RequestContext.observationMode`：可选字段；**键缺失=非观察请求（canonical 唯一缺省表示）**；出现时必须为 canonical 枚举文本 `PAUSED_READ_ONLY`；显式 null 与未知值 → 本地 `DESERIALIZATION` fail-closed（R2 分类），云端 record 构造拒绝。
- digest：字段属 context，NON_NULL canonical——缺省不写键、出现按 enum name 参与 requestDigest，双仓字节一致。
- operation 限定：仅 `WINDOW_FACT`/`CAPTURE` 可携带；`EXECUTE_INPUT_BUNDLE` 携带 → Cloud 构造期拒绝 / DHXY `SCHEMA_MISMATCH`。
- 授权矩阵：携带该标记的请求仅当 binding/registration `status==PAUSED` 且 `runRevision == 当前 PAUSED revision` 且 exact scope/window/stopEpoch 全等时放行（Cloud 入队门、dispatch 持锁门、DHXY 副作用前门三道对称）；ACTIVE/terminal/drift/revision 不等 → typed `TASK_RUN_MISMATCH` 族拒绝；不携带标记的请求语义零变化。
- 身份规则：observation 身份由 retained 权威独立铸造，禁止 renewal（结构性），同字节重投沿用既有幂等台账规则。

### R4：修正后的 exact 写集（替换 Repair #1 R5）

- **Cloud：2 New + 6 Modify**（不变）：New `remote/RemoteObservationMode.java`、`remote/CloudPausedReadOnlyObservationContext.java`；Modify `remote/RequestContext.java`、`remote/RemoteCommandEnvelope.java`、`remote/run/RemoteTaskRunCoordinator.java`、`remote/RemoteGameCommandBroker.java`、`remote/CloudTaskRunExecutionGate.java`、`remote/CloudTaskRunActionLedger.java`（R1 的 nested ObservationActionIdentity/观察 map/双向模式冲突/重载，ACTIVE 面零改）。
- **DHXY：1 New + 4 Java Modify + 1 Doc Modify**：New `cloud/remote/RemoteObservationMode.java`；Modify `cloud/remote/RemoteGameCommand.java`（typed nullable enum + Nulls.FAIL）、`cloud/remote/RemoteProtocolDigests.java`、`cloud/remote/HttpRemoteCommandTransport.java`、`cloud/remote/LocalRemoteGameCommandHandler.java`；Doc `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`（R3）。
- 禁碰/边界不变：`TaskPauseToken`、`RemoteTaskRunLifecycleService`、`RemoteTaskRunRegistry`、`RemoteOperationLedger`、`RemoteCommandPollingLoop` 零修改；无测试/host/caller/运行面；input 禁止矩阵沿用（另加 `acquireObservation(op=EXECUTE_INPUT_BUNDLE)` 构造期拒绝行）。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。W0 Design Repair #2 到此停止，等待父级复审；heartbeat 保持。

## Parent W0 Design Review #3 - DESIGN APPROVED - 2026-07-12

Repair #2 已关闭上一轮全部 P1/P2：ACTIVE `RetainedActionIdentity` 与现有 caller 零改，PAUSED observer 使用无继承/转换的
独立 typed identity 与独立记录；ACTIVE/OBSERVATION mode 冲突、capability 引用、operation 与 paused revision 均 exact；
unknown enum/显式 null 统一落现有 `DESERIALIZATION`，operation 组合错误才落 `SCHEMA_MISMATCH`；协议 schema 已纳入写集。
父级结论：**DESIGN APPROVED，P0/P1/P2=0**。现授权同一 A 按 R4 实施，以下约束绑定且不再要求 Repair #3：

1. `ObservationActionIdentity` 不得形成新的 public 业务 API；由于 enclosing ledger 为 package-private，应保持 nested 类型和所有
   mint/bind/outcome 方法仅 authority package 可达。ACTIVE identity/`CloudTaskRetainedActionState`/renewal 签名与行为零改。
2. retained hard cap 必须对 ACTIVE records + observation records 的**合计**原子计数；达到 10,000 fail-closed，无 silent
   eviction/TTL/cleanup。跨模式冲突检查覆盖 taskRunId+businessActionKey 的全部 retained revision，不能只查 exact paused key。
3. observation 同 key 幂等仅允许 exact same capability reference + operation + paused revision；不同 capability（即使字段相同）、
   不同 mode/operation/revision 全部拒绝。`renewAfterNotExecuted` 继续只有 ACTIVE identity overload，不能增加 observation overload。
4. broker enqueue/final-dispatch 与 DHXY pre-side-effect 三道门都必须 exact PAUSED/current revision/scope/window/stopEpoch；
   input marker 在构造、broker、schema、handler 全部拒绝。普通无 marker 请求的 canonical bytes/digest/authorization 不变。
5. 只允许 R4 的 Cloud 2 New + 6 Modify、DHXY 1 New + 4 Java Modify + protocol schema Doc；无 tests、host/caller/poller/
   registry/operation-ledger/TaskPauseToken 修改。完成后运行 Cloud `mvn -q clean package`（不可 skip）与 DHXY
   `mvn -q -DskipTests compile`，报告双仓 digest/schema parity 与无运行面证据。Worker 自审仍不算 Implementation Approved。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - W0 Implementation #1 - 2026-07-12

依据 `Parent W0 Design Review #3 - DESIGN APPROVED` 与其 5 条绑定约束实施。只写 R4 批准写集；业务源只认 HEAD `0114604e`；两仓在途改动零覆盖（每处修改前先读当前内容）。

### Exact 写集（Cloud 2 New + 6 Modify；DHXY 1 New + 4 Java Modify + 1 Doc Modify）

Cloud：
| 文件 | 改动 |
|---|---|
| New `remote/RemoteObservationMode.java` | enum `PAUSED_READ_ONLY` |
| New `remote/CloudPausedReadOnlyObservationContext.java` | public final、private 构造、package-private `snapshotOf`（仅接受 PAUSED binding）；与 `CloudTaskRunExecutionContext` 无继承/转换，Service context/retained-ACTIVE/input builder/context slot 各入口类型上收不下它 |
| Modify `remote/RequestContext.java` | + 可空 `RemoteObservationMode observationMode` 组件；构造校验 `EXECUTE_INPUT_BUNDLE` 携带即拒；wither 透传。缺省 null 时 NON_NULL canonical 不写键——普通请求 digest 字节零变化 |
| Modify `remote/RemoteCommandEnvelope.java` | + 字段透传 + 校验（仅 fact/capture 可携带） |
| Modify `remote/run/RemoteTaskRunCoordinator.java` | 新增独立只读观察门：`pausedObservationDenialReason(...)`（null=放行）与 `authorizePausedObservationAndMarkDispatch(...)` + 嵌套 `ObservationDispatchGateResult`。判定：exact scope 四元组（不匹配即 redacted 拒绝）/window 四元组/stopEpoch 全等 + `status==PAUSED` + `runRevision==pausedRunRevision`；不查 execution confirmation、零状态写。`RemoteTaskRunAuthorization` 的 allowed→ACTIVE 不变式未触碰（该类不在写集，普通门原样） |
| Modify `remote/RemoteGameCommandBroker.java` | 入队门与 dispatch 持锁门各加观察分支（marker 非空走观察门，拒绝 → `NOT_EXECUTED/TASK_RUN_MISMATCH`；普通命令路径与 PAUSED→TASK_RUN_PAUSED 映射逐字不变） |
| Modify `remote/CloudTaskRunExecutionGate.java` | package-private `createPausedObservationContext`（find + PAUSED 快照）；`newPausedObservationWindowFactRequest` / `newPausedObservationCaptureRequest`（capability 引用相等 + operation 匹配 + 逐次 coordinator 只读重验 + 写入 marker + 既有 digest/bind 流程）；ACTIVE 构建路径仅追加 `null` observationMode 参数，语义零变化 |
| Modify `remote/CloudTaskRunActionLedger.java` | 嵌套互斥类型 `ObservationActionIdentity`（private 构造、无 renewal 通路——`renewAfterNotExecuted` 签名只收 ACTIVE 类型）；独立 `observationRecords`（key=(taskRunId,key,pausedRunRevision)）+ `observationKeyUse` 跨模式 O(1) 索引；`acquireObservation`（op 限 fact/capture、幂等重取要求同 capability 引用+同 op+同 revision、字段相同的新 snapshot 拒绝）；双向模式冲突 fail-closed（ACTIVE acquire 查观察索引【覆盖全部 retained revision】、观察 acquire 查 ACTIVE map）；`retainedTotal()` 合计原子配额（ACTIVE+observation 同一 10,000 上限，两条 mint 路径同 monitor 检查，无 eviction/TTL）；`bindOrVerifyObservationRequest`/`recordObservationOutcome`（EXECUTED 结构性拒绝；UNKNOWN 未解析/单次 final 转换同 ACTIVE 合同）/`retainedObservationRequest` |

DHXY：
| 文件 | 改动 |
|---|---|
| New `cloud/remote/RemoteObservationMode.java` | 镜像 enum |
| Modify `cloud/remote/RemoteGameCommand.java` | typed nullable `RemoteObservationMode observationMode` + `@JsonSetter(nulls = Nulls.FAIL)`（显式 null 反序列化即败）|
| Modify `cloud/remote/RemoteProtocolDigests.java` | 非空时 `context.put("observationMode", name())`——与 Cloud NON_NULL canonical 字节一致 |
| Modify `cloud/remote/HttpRemoteCommandTransport.java` | strict schema：marker + `EXECUTE_INPUT_BUNDLE` → `SCHEMA_MISMATCH`；unknown enum/显式 null 走既有 `DESERIALIZATION` 路径（R2 写死分类，无新检查层） |
| Modify `cloud/remote/LocalRemoteGameCommandHandler.java` | classify 尾部观察分支：marker=PAUSED_READ_ONLY 且非 input 且 registration `PAUSED` → ACTIVE-equivalent 只读放行（`requireRegistration` 的副作用前 revision 门继续强制 `command.runRevision == registration.runRevision` 即 paused revision）；marker+ACTIVE 或 marker+input → `TASK_RUN_MISMATCH`；普通命令 PAUSED→TASK_RUN_PAUSED 逐字不变 |
| Doc `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md` | RequestContext 加可选 `observationMode?` 键 + 新规则 7（omission/canonical/digest/operation 限定/三门授权矩阵/DESERIALIZATION vs SCHEMA_MISMATCH 分类/身份互斥与无 renewal） |

### 绑定约束逐条对照

1. `ObservationActionIdentity` 为 package-private ledger 的嵌套类型，mint/bind/outcome 全部 authority 包内可达，无新 public 业务 API；ACTIVE identity/`CloudTaskRetainedActionState`/renewal 签名与行为零改（编译原样）。
2. 合计配额：`retainedTotal()=records+observationRecords`，两条 acquire 在同一 synchronized monitor 检查 10,000 上限，fail-closed，无 eviction/TTL/cleanup；跨模式冲突用 `(taskRunId,businessActionKey)` 全 revision 索引，非仅 exact paused key。
3. 观察幂等重取 = exact 同 capability 引用 + 同 operation + 同 paused revision；其余（含字段全同新 snapshot）全拒；`renewAfterNotExecuted` 无 observation overload——类型系统级禁令。
4. 三道门对称 exact PAUSED/current revision/scope/window/stopEpoch（coordinator 观察门、broker 入队+dispatch、DHXY classify+副作用前 revision 门）；input marker 四层全拒（RequestContext 构造、Envelope 构造、DHXY schema、handler classify）。普通无 marker 请求 canonical 字节/digest/授权零变化（可空组件缺省不进 NON_NULL canonical，两侧 digest 重算路径均只在非空时写键）。
5. 写集即上表，无 tests/host/caller/poller/registry/operation-ledger/TaskPauseToken 修改。

### 双仓 digest/schema parity

- wire 键 `context.observationMode`：仅出现于携带请求；值=枚举 canonical 名 `PAUSED_READ_ONLY`。Cloud 侧 `RemoteProtocolDigests.computeRequestDigest` 经 Jackson NON_NULL valueToTree 自动含/略该键；DHXY `computeRequestDigest` 手动 `if(!=null) put(name())`——同键名、同值文本、同 canonical 排序，字节一致。无 marker 请求两侧均不写键，与 W0 之前的 digest 完全相同（回归零影响）。
- 显式 null / 未知值：DHXY 反序列化失败 → 既有 `DESERIALIZATION`；结构合法但 op 组合非法 → `SCHEMA_MISMATCH`（R2 分类写死）。

### 验证证据

- Cloud `mvn -q clean package`：exit 0；Surefire 4 suites / 21 tests / 0 failures / 0 errors / 0 skipped（无新增/恢复测试）。
- DHXY `mvn -q -DskipTests compile`：exit 0。
- `git diff --check`：DHXY exit 0、dhxy-cloud-brain exit 0，均无非 warning 行。
- 未启动 application/server/host/poller/UI/capture/OCR/input；未发 HTTP；无任何 Git mutation；两仓在途 dirty/untracked 零覆盖。
- 已知边界（如实披露）：(a) `@JsonSetter(Nulls.FAIL)` 依赖 Lombok `@Jacksonized` 将注解复制到 builder setter——编译已过，运行期行为按 no-local-test 模式留待源码复审确认；(b) 观察命令 dispatch 后的 operation deadline 沿用既有 broker pause 冻结语义（暂停期间 deadline 顺延），与普通命令一致，非本切片新增语义。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级 Implementation APPROVED）。W0 Implementation #1 到此停止，等待父级源码复审；heartbeat 保持。

## Parent W0 Implementation Review #1 - BLOCKED - 2026-07-12

### 结论

**BLOCKED，P0=0/P1=1/P2=0。** typed observer identity、combined quota/mode conflict、三道 PAUSED revision
fence、input 排除、双仓 digest/schema parity 均成立；DHXY generated builder 的 `observationMode(...)` 已由父级
`javap -v` 确认携带 `@JsonSetter(nulls=FAIL)`，显式 null 分类缺口关闭。父级 fresh Cloud
`mvn -q clean package` 为 4 suites/21 tests 全绿，DHXY `mvn -q -DskipTests compile` exit 0。

### P1：PAUSED observer 的 operation timeout 被 pause-progress 永久冻结

- 证据：`RemoteGameCommandBroker.java:177-187` 的观察 dispatch 仍调用 `pending::tryDispatch` 并传入当前
  cumulative pause baseline；`PendingCommand.tryDispatch` 在 `:900-912` 设置正常 operation deadline。随后
  `finishDeadlineTimeout` / `refreshPauseDeadlineLocked` 在 `:537-620` 对所有 dispatched request 无差别调用
  `applyPauseProgress`，而 `:920-933` 每次把持续 PAUSED 的增量继续加到 deadline。
- 影响：观察命令恰好只在 PAUSED 时合法，因此只要 run 继续暂停，10 秒 `timeoutMs` 永远不会到期；poller/客户端丢失或
  outcome 不返回时，该同步调用与 pending 额度可无限占用，直到 resume。Implementation 报告第 665 行也披露了该事实，
  但它不是普通命令可继承的语义：普通命令在 PAUSED 时不应消耗执行预算，observer 正是在 PAUSED 内执行。
- Repair #1：只在现有 `RemoteGameCommandBroker.java` 定点修复，带 `observationMode` 的 dispatched request 必须使用
  dispatch 后 wall-clock operation deadline，不应用 cumulative pause extension；无 marker 的 ACTIVE 请求继续保持既有
  pause-freeze 语义逐字不变。不得新增 retry/TTL、改 observer identity、改 coordinator/ledger/DHXY/schema，或扩大 caller/
  host。修复后追加 `External Worker A - W0 Implementation Repair #1`，并重跑双构建。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - W0 Implementation Repair #1 - 2026-07-12

针对 `Parent W0 Implementation Review #1 - BLOCKED`（P0=0/P1=1/P2=0）唯一 P1 定点修复，只改
`remote/RemoteGameCommandBroker.java` 两处（写集零扩大，coordinator/ledger/DHXY/schema/observer identity 零改动，无 retry/TTL，无 caller/host 扩大）：

1. `refreshPauseDeadlineLocked`：dispatched 请求若携带 `observationMode`，直接返回 false——不调用
   `withPauseProgress`、不做 `applyPauseProgress` 累计顺延；其 operation deadline 保持 dispatch 时刻设定的
   wall-clock 值。无 marker 的 ACTIVE 请求路径逐字不变（pause-freeze 语义保留）。
2. `finishDeadlineTimeout`：观察请求并入立即判定分支（与未 dispatched 同路），到期即
   `completeDeadlineTimeoutLocked` -> 既有 UNKNOWN/TIMEOUT 终态，释放 pending 额度与同步调用方；不再进入
   pause-progress 顺延分支。

语义结果：PAUSED 期间执行的观察命令，其 10s bounded timeoutMs 按 wall clock 正常到期（客户端失联/不回 outcome
时以 UNKNOWN 收敛，pending 配额随之释放）；普通命令在 PAUSED 时不消耗执行预算的既有语义零变化。UNKNOWN 后仍
只能 same-bytes 重投（观察身份无 renewal 通路），与既有幂等合同一致。

验证：Cloud `mvn -q clean package` exit 0（4 suites / 21 tests / 0 failures / 0 errors / 0 skipped）；DHXY
`mvn -q -DskipTests compile` exit 0；两仓 `git diff --check` exit 0 无非 warning 行。未启动任何运行面，无 Git
mutation，在途改动零覆盖。

**无已批准业务差异；按基线等价迁移。** Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。Repair #1 到此停止，等待父级复审；heartbeat 保持。

## Parent W0 Implementation Review #2 - APPROVED - 2026-07-12

### 审查范围与结论

父级只复审 Repair #1 获准修改的 Cloud
`remote/RemoteGameCommandBroker.java`，并重新沿 `poll -> PendingCommand.tryDispatch -> await ->
refreshPauseDeadlineLocked/finishDeadlineTimeout -> completeDeadlineTimeout` 追踪 operation deadline。结论为
**APPROVED，P0/P1/P2=0**，上轮唯一 P1 已关闭。

### 关闭证据

- `PendingCommand.tryDispatch` 仍只在 dispatch 时用 `timeoutMs` 设置 operation deadline；observer 没有新增 TTL、retry
  或 identity renewal。
- `refreshPauseDeadlineLocked` 对 `observationMode != null` 直接返回，不再读取或累计 pause progress；未标记的 ACTIVE
  请求仍完整进入原 `withPauseProgress -> applyPauseProgress` 路径，普通 pause-freeze 语义未变。
- `finishDeadlineTimeout` 对 observer 直接调用既有 `completeDeadlineTimeoutLocked`。已 dispatch 的 observer 到期后由
  `completeWaitFailure(TIMEOUT, ...)` 形成 `UNKNOWN/TIMEOUT`；deadline CAS 仍拒绝陈旧 deadline 值，终态账本和 pending
  释放路径未被旁路。
- Repair 写集没有扩到 coordinator、ledger、DHXY、schema、caller 或 host；运行面保持 dormant。

### 父级 fresh 构建

- Cloud `mvn -q clean package`：exit 0；4 suites / 21 tests / 0 failures / 0 errors / 0 skipped。
- 本 Repair 没有修改 DHXY Java；上一轮父级 DHXY compile 已通过，本轮不以未变化侧重复构建充当新证据。

**最终结论：APPROVED，P0=0/P1=0/P2=0。无已批准业务差异；按基线等价迁移。** W0 PAUSED observer
协议/实现切片收口；这不激活 AutoCombat caller、Task host、poller 或生产切换。

## Parent Next Task Handoff - 2026-07-12

外部 Worker A 的下一任务已写入固定日志：
`docs/superpowers/plans/reports/2026-07-12-cloud-task-maintenance-service-worker-a.md`。

A 继续作为实现 Worker，每 5 分钟读取新日志；先追加 `External Worker A - Design #1`，父级 `DESIGN APPROVED` 前零
Java。旧 observer 切片已关闭，不再修改本文件或其批准代码，除非父级明确重开。
