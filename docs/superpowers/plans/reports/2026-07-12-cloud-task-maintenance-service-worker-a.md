# Cloud TaskMaintenanceService Worker A Log

## Parent Task Brief #1 - 2026-07-12

### 目标

以 DHXY HEAD `0114604e` 的
`src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`（3136 行、当前 scoped clean）为唯一业务权威，设计该
Service 整类迁云的可编译、可分波但最终单权威方案。当前阶段 **Design only**；父级明确 `DESIGN APPROVED` 前不得修改
Java/Maven/resources/tests。

### 开工必读与基线

- 完整读取 `D:\mavenProject\DHXY\AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、
  `docs/业务逻辑.md` 中五倍/修罗/自动战斗维护、队伍、战后补给、召唤兽技能相关批准基线，以及迁移矩阵最新进度。
- 完整读取 HEAD `TaskMaintenanceService.java`，不得以工作区其它 dirty 业务文件替代 HEAD；盘点所有 caller，至少覆盖
  `AutoCombatService`、`AutoBattleTask`、`WubeiTask`、`XiuluoTaskV2`、`WindowTaskRunner`、
  `MultiWindowTaskManager`、`WindowTaskControlService`、`SummonSkillService`。
- 对照 Cloud 当前 `TaskExecutionContext`、`CloudTaskServiceExecutionContext`、per-run `GameContext.State` owner、
  config authority、typed Service port、startup/turn/current-slot 已批准合同；不得假设尚不存在的 public API。

### 硬边界

1. 3136 行 HEAD 的 public API、条件、优先级、队列顺序、计数、TTL/cooldown、fallback、日志语义、点击/确认顺序与
   WindowReadyEvent 的 soft-wake 含义必须逐项盘点。现有 TTL 是基线业务的一部分；不得新增/删除/重解释 TTL、retry、
   cleanup、park/yield 或 negative truth。
2. 不得把 `WindowRuntimeContext`、`WindowTaskContextHolder`、HWND、`CoordinateHelper`、`InputSequences`、
   `WindowReadyEventBus`、本地 `RuntimeDecisionShadowService` 或任何本地 runner/input authority 复制到 Cloud。
3. Service 中纯状态必须绑定 exact tenant/user/taskRun/window/stopEpoch/runRevision 与已批准 per-run/tenant owner；local-team
   session、leader/member、broadcast/first-aid/summon-skill 队列不得按裸 windowId 跨租户共享。列容量上限、terminal cleanup、
   restart fail-closed 与 durable/non-durable 边界。
4. screenshot/template/dialog/input/ROI/Point 等机械事实和动作只能经 retained-authority typed Service port；稳定
   requestId/actionId 只能由 retained ledger 管理，UNKNOWN same-bytes，不能开放 raw request/poll/outcome 或自动 renewal。
5. HEAD 中已有 cloud-decision/shadow wrapper 不得搬成 Cloud self-HTTP 或双业务权威；设计需指出应恢复为唯一 in-process
   policy 的部分，以及删除/保留 shadow diagnostics 的时机。
6. `WindowReadyEvent` 只能迁为 typed soft wakeup，不得把 ready-event 缺失/负信号升级为业务真值；host/Task/caller 保持
   dormant，生产只整体原子切换。
7. 与并行写集隔离：不得修改 B 的 `AutoCombatPanelService` 日志/未来文件、K 的 remote current-slot 1+7、M 的
   observation ledger/compaction、H/J/I/L 已冻结文件。发现必需重叠先写冲突，不得编码。

### Design #1 必交付

- 全部 public/private workflow、嵌套 DTO/enum、状态 map/monitor/queue、TTL/counter/timer 与 caller 兼容矩阵；标出纯状态、
  mechanical fact、capture/template、input bundle、soft wake、config、cloud-decision/shadow、checkpoint/sleep。
- 清晰的 authority 切分：per-run、per-window、per-team-session、per-tenant state 分别由谁拥有；stable key、创建/恢复/终止/
  清理、并发线性化、capacity/restart/tenant isolation 如何闭合，禁止 ThreadLocal/static default state。
- 每个机械调用的 retained typed port 形状与 checkpoint-before-mechanical；原子输入 bundle 必须保留 HEAD 操作顺序，
  不能把 unresolved/UNKNOWN 变成 false/success/fallback。
- 精确 Cloud/DHXY 文件 DAG、分波顺序、Spring/host dormant 图、API 兼容表、与现有 K/M/B/J/H 前置关系；任何需要
  wire/schema 的项必须列双仓字段、digest 与本地副作用前 fence。
- 租户隔离、错窗/乱序、pause/resume/stop、断线/UNKNOWN、capacity、restart、运维和验收风险矩阵，明确 P0/P1/P2。

### Worker 规则

- 你不是 reviewer，不写 APPROVED。只向本文件追加，不修改历史；父级 BLOCKED 后由同一 A 返修。
- 保护两仓全部 dirty/untracked，不回滚、不覆盖、不 Git mutation；不新增/恢复 DHXY tests，不启动任何运行面。
- 完成 Design #1 后停止编码并等待父级审查，同时保持每 5 分钟读取本文件。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - Design #1 - 2026-07-12

依据 HEAD `0114604e` 的 `service/TaskMaintenanceService.java`（git show 提取 3308 行，工作区 dirty 未采信）逐区通读与全量结构盘点；已核 `AGENTS.md`、`DHXY_CONTEXT.md`、`ACTIVE_WORK.md` CR271 顶部、`业务逻辑.md` 维护/队伍/战后补给/盒子 30s（L151-156）/战后短窗口放权（L1016）基线行、迁移矩阵。Design-only：未改 Java/Maven/resources/tests，无 Git/Maven 运行。

### 1. 全量盘点（public/嵌套/状态/机械/软唤醒/影子）

**Public API（40 个方法 + 嵌套类型，签名全表见 §5 caller 矩阵）**，按类别标注：
- **纯状态（大多数）**：first-aid FIFO 五方法（report/open/isHead/hasItem/complete/isCompleteAndClose/clearForWindow）、team round/window 八方法（begin/openPathing/openFirstAid/close/openLocalReturn/closeLocalReturn/isPathingOpen/isFirstAidOpen）、team return 四方法（resolve/markPending/clearPending/pendingCount）、combat phase 四方法（openForLeader/confirmExited/invalidate/memberView）、broadcast queue 五方法（open/consumeIfHead/isDrained/isInOpen + probe※）、local-team session 十二方法（attach/register×3/markRole/recordTooltip/isSupportMember/isCandidate/isPendingLeaderDetection/markLeaderDetected/markLeaderPaused/isLeaderPausedForMember/isCapabilityOpen/completeWindow/shouldSuppressIdle）、summon-skill 三方法（initializeForTaskStart/clearQueueForWindow/isCleanDueForCurrentWindow）。
- **机械（仅 3 个真实触点）**：`probeMaintenanceBroadcastPointForCurrentWindow` :1186（getScaledRect+findImageInRegion = ROI capture+模板匹配，只读）；`handleMaintenanceBroadcast` :2177（同 ROI 探测 :2187/2201 + `inputSequences.moveAndClickLeft` :2206，settle 150ms/delay 800ms）；`runOpportunisticMaintenance` :2110 经 `summonSkillService.cleanSummonSkillsOnce` :2383 的整链委托（属 SummonSkillService 自身切片）。`dialogService` 与两个 cloud-decision service **在 HEAD 无任何 `.` 调用点**（grep 全文仅 :2565 `runtimeDecisionShadowService.shadow` 一处）——见 §4。
- **软唤醒（仅 2 处发布，零消费）**：`TEAM_RETURN_STATE_CHANGED` :782、`MAINTENANCE_BROADCAST_QUEUE_CHANGED` :1171。
- **状态容器（20 组）**：summon-skill 5 map + queue/keys/monitor、team round 3 map、localTeamSessions + completed tombstones（TTL 2h + 256 上限 :57/:61）、summonSkillClaimsByTeamRound、first-aid queueByScope+monitor。**TTL/冷却全表为基线业务**（:53-:74：60s log、2h tail/count cache、90s lead、2h session tombstone、30s idle suppress、60s no-action log、ROI 260/381/378/413、click 150/800ms）——逐字保留，不增不删不重释。
- **key 语义**：`currentWindowKey` :2929 = context.windowId，holder 仅兜底；`postCombatFirstAidScopeKey` :417 = sessionKey+groupHash 或 `window:<id>`；team keys :3053-:3137 = explicit teamKey/sessionKey 归一 + round 递增。

### 2. Authority 切分（禁 ThreadLocal/static default）

- **per-tenant host bean（唯一实例宿主 = 既有 `CloudServiceHost` per tenant/user 容器）**持有全部跨窗口状态：local-team session 与 tombstones、team round/window/snapshot、broadcast queue、first-aid queueByScope、summon-skill queue/claims/cooldown。理由：这些状态按基线即为跨窗口队伍协调，不可 per-run 化；tenant host 容器使所有 windowId/sessionKey 键天然租户内隔离——**不存在裸 windowId 跨租户共享**（硬边界 3 闭合）。
- **per-run**：方法全部以显式 `TaskExecutionContext`（Cloud 同 FQCN 桥接版）为窗口/会话身份来源；`currentWindowKey` 的 holder 兜底改为 context-only（context 无窗口时保留 `DEFAULT_WINDOW_KEY` 分支，语义同 HEAD 去 holder）。无 ThreadLocal、无 static default state。
- **创建/终止/清理**：沿用 HEAD 显式清理 API（`clearSummonSkillQueueForWindow`/`clearPostCombatFirstAidForWindow`/`completeLocalTeamSessionWindow`，现由 WindowTaskRunner 调用）——W-TMS-2 波由 cohort/激活层在 run terminal 时按同一调用形状触发；tombstone TTL 2h+256 上限逐字保留；host close 整体释放。**restart fail-closed**：全部 in-memory，不声称跨进程恢复；durable 边界=无（与既有冻结原则一致）。
- **并发线性化**：保留 HEAD 的 monitor 粒度（summonSkillQueueMonitor、postCombatFirstAidMonitor、ConcurrentHashMap 语义）逐字迁移；容量上限=既有 tombstone 256 + 队列自然有界（窗口/队伍数级），summon-skill queue 无 HEAD 上限则不新增（容量变更须另开 CR）。
- **计时**：全部改注入式 `monotonicMillis` LongSupplier（同进程单调毫秒），TTL/冷却数值与判定零改动；wall-clock 只入日志（沿 AutoCombat 主设计 R4 已批准模式）。

### 3. 机械调用 → retained typed port 映射（checkpoint-before-mechanical）

| HEAD 触点 | Cloud 形状 |
|---|---|
| :1186/:2187 ROI 探测（getScaledRect+findImageInRegion） | typed checkpoint → WINDOW_FACT(GEOMETRY) 取窗口几何 → **cloud 纯函数**按 HEAD 缩放公式换算 ROI（不复制 CoordinateHelper 类，只迁其确定性缩放算式为 TMS 私有纯函数）→ CAPTURE(ROI) → 已批准 cloud-native image processor 模板匹配。retained 身份/同字节重投/UNKNOWN 不升级 false 均按既有 port 合同；probe 只读，无输入 |
| :2206 广播点击 | typed checkpoint → EXECUTE_INPUT_BUNDLE 单一原子 bundle：move + settle 150ms + clickLeft + delay 800ms（HEAD 顺序逐字，不拆分）；UNKNOWN 保持 unresolved，绝不转 false/success/fallback |
| :2383 召唤兽清理整链 | 同云协作调用 `SummonSkillService.cleanSummonSkillsOnce(request)`（该 Service 自身迁移切片交付，合同=HEAD public 签名；其内部机械自行经 port） |

### 4. cloud-decision/shadow 与软唤醒处置（硬边界 5/6）

- **恢复唯一 in-process policy**：HEAD 的 `CapabilityGateCloudDecisionService`/`MaintenanceThresholdCloudDecisionService` 注入后**零调用点**，`runtimeDecisionShadowService.shadow` 仅 :2565 一处 try-swallow 诊断——三者皆为迁移前对照脚手架。Cloud 版 TMS **不注入、不迁移**这三个依赖；本地决策分支（即批准基线）内联为唯一 policy；shadow 诊断在 TMS 迁移波删除（其存在意义随 Service 上云终止）。DHXY HEAD 侧原样不动直至整体原子切换——不产生 self-HTTP、不产生双权威。
- **软唤醒**：新契约 `CloudSoftWakeupPublisher`（typed：事件类型 + sessionKey/leaderWindowId scope；publish-only、best-effort），两处发布点一一映射；投递由 cohort 调度切片实现。ready-event 缺失/负信号不构成任何业务真值（与 HEAD 一致：本类只发布不消费）。

### 5. Caller 兼容矩阵与唯一签名适配

HEAD caller 全量（行号=各 caller 文件内）：AutoCombatService×21、AutoBattleTask×14、WubeiTask×19、XiuluoTaskV2×18（含 probe :3278、openPostCombatFirstAidQueue :3524、broadcast 四连 :3119-:3309）、WindowTaskRunner×11（含三个清理 API 与 session attach/role/tooltip/paused）、SummonSkillService×1（handleMaintenanceBroadcast :671）、MultiWindowTaskManager×0、WindowTaskControlService×0。
- **39/40 方法签名逐字保留**（均已显式 context 或显式 key，callers 调用形状零变化；caller 自身属 B 类任务大脑/runner 各自切片）。
- **唯一适配**：`probeMaintenanceBroadcastPointForCurrentWindow(String)` 无 context 参数且依赖 holder 定窗；TMS 为跨窗口 tenant bean，无法 context-bound（逐方法证明：一个实例服务多窗口，无单一 bound context 可用）。云端签名 → `probeMaintenanceBroadcastPointForCurrentWindow(TaskExecutionContext context, String sourceTask)`；**全部受影响 caller 唯一一处 = XiuluoTaskV2:3278**，纳入同一原子 cohort 写集（W-TMS-2），不留双形态。返回 `java.awt.Point`（JDK 类型，headless 安全）保持。

### 6. 文件 DAG / 分波 / dormant 图

- **前置（各自切片，全部已有门或进行中）**：typed Service port + retained ledger（已批准）✅；cloud-native image processor（已批准）✅；W0 PAUSED observer（已收口）✅；H per-run `GameContext.State` owner（TMS 的 gameContext 读写绑定）；I `CloudAutoBattleProperties` 同族 config authority（botProperties 维护阈值/开关取值同源单向下发）；K current-context slot（TaskExecutionContext 桥接）；SummonSkillService 云端合同切片（新前置，建议独立 worker）；`CloudSoftWakeupPublisher` 契约（可并入 cohort 调度切片）。J turn 与 TMS 无交集（本类零 turn 调用）。
- **W-TMS-1**：Cloud New `com/bot/dhxy/service/TaskMaintenanceService.java`（整类等价迁移：§2 authority + §3 port 映射 + §4 依赖收敛 + §5 签名；dormant，不注册、无 caller）+ New `CloudSoftWakeupPublisher` 契约（若未由 cohort 切片先行交付）。**DHXY Java 写集为空**；全部依赖类型存在且父级批准后才写 Java。
- **W-TMS-2**：caller 切片按 §5 接线（含 XiuluoTaskV2 唯一补参与 runner 清理职责移交 cohort）；生产一次性原子切换，无长期双权威。
- **写集隔离确认**：不触碰 B panel、K slot 1+7、M observation ledger/compaction、H/J/I/L 已冻结文件；SummonSkillService 前置若与在途切片重叠，先写冲突报告再动（当前无该切片）。

### 7. 风险矩阵与 P0/P1/P2

| 风险 | 处置 |
|---|---|
| 租户隔离 | 全部跨窗口状态收敛 tenant host 容器；key 不出容器 |
| 错窗/乱序/断线/UNKNOWN | 机械全经 port 三道闸；UNKNOWN unresolved 不转业务值；同字节重投 |
| pause/resume/stop | 方法为无阻塞状态读写（无长等待）；机械点前 typed checkpoint；resume 后旧 context 由既有 revision 门拒绝 |
| capacity/restart | tombstone 256+2h、队列自然有界、无新增上限/TTL；restart fail-closed 无恢复声称 |
| 运维 | no-action/deferred 日志节流常量逐字保留；shadow 诊断删除在 TMS 波（记录于切换清单） |
| **P1（未决，跨切片）** | SummonSkillService 云端合同切片不存在——W-TMS-1 编译依赖其类型；需父级开单（或指令改为接口先行由本切片交付其消费子集合同） |
| **P2（未决）** | `CloudSoftWakeupPublisher` 投递端归属（cohort 调度切片）未立项；TMS 侧仅 publish 合同，无投递不影响编译/等价性（best-effort 语义） |
| **P2（未决）** | broadcast ROI 缩放算式从 CoordinateHelper 内联为纯函数后，与本地端到端像素等价需在 image-processor 联调时对拍（算式逐字迁移，输入改 GEOMETRY fact） |
| P0 | 无 |

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=1（外部依赖立项）/P2=2（均已列并给出闭合路径），不构成父级批准。Design #1 到此停止，等待父级 `DESIGN APPROVED`/`BLOCKED`；heartbeat 保持每 5 分钟读取本文件。

## Parent Design Review #1 - BLOCKED - 2026-07-12

父级已对照 DHXY HEAD `0114604e`、当前 Cloud `TaskExecutionContext` / `CloudTaskServicePort` / H-K authority 源码及实际 caller 复核。结论：**BLOCKED，P0=0，P1=6，P2=1**。本轮仅审设计，未运行构建；A 不得开始 Java。

### P1-1：机械调用没有 retained action handle 所有权，当前方案无法按既有 port 编译或保证同字节重投

- 证据：Design #1 L86-L88 只写 `context -> port`；但当前 `TaskExecutionContext.getRemoteGameClient()` 明确说明 facade 不能声明/renew action，caller 还必须持有 retained Task state 产生的 opaque handle；`CloudTaskServicePort.capture(...)` 和 `executeInputBundle(...)` 分别强制接收 `CaptureAction` / `InputBundleAction`。
- 影响：TMS 的 GEOMETRY fact、两次模板 capture、点击 bundle 没有稳定 phase/actionSlot/occurrence/attempt owner；若 Service 临时 mint 或按调用重建 ID，会重开旧 P1 的新 ID 重试/UNKNOWN 误重投问题。
- 返修条件：逐机械步骤列出 exact retained address/handle、谁创建并跨调用保存、何时允许新 occurrence/可信 NOT_EXECUTED renewal、UNKNOWN 如何重交同一 request bytes。公共 TMS 方法不得接 raw ID/key；若需新增 trusted Task state，列入 DAG 与写集。

### P1-2：tenant bean 仍允许 stale/null/raw-key 状态写，未绑定 exact taskRun/revision authority

- 证据：L77 保留 `DEFAULT_WINDOW_KEY`；L98 把“显式 key”视为等价 authority。HEAD 还存在 `clear*ForWindow`、`invalidateTeamCombatPhaseForLeader`、`markLocalTeamLeaderPaused`、`attach/registerLocalTeamSession*`、`completeLocalTeamSessionWindow` 等无 context 的 public mutation。K/H 只在 current runtime projection 内提供 exact context/state，不会自动保护 tenant bean 的这些入口。
- 影响：旧 revision、错误 taskRun 或缺 window 的调用可在没有机械闸的情况下改写当前队伍 FIFO、broadcast、first-aid、combat phase 和 cooldown；`DEFAULT_WINDOW_KEY` 还会把多个无窗口调用合并到同一租户槽。
- 返修条件：删除 Cloud 的 null/default-window 业务路径；给全部 public mutation 做兼容矩阵，逐项选择 exact current `TaskExecutionContext` revalidate 或 assembly-only lifecycle permit，并绑定 scope/taskRun/window/stopEpoch/runRevision。无 context 清理只能由同 authority assembly 的 exact terminal transition 调用，不能保留 public raw window/session mutation bypass。

### P1-3：ROI 方案没有保留 HEAD 的两次 fresh capture 顺序与坐标合同

- 证据：HEAD `probeMaintenanceBroadcastPointForCurrentWindow` 和 `handleMaintenanceBroadcast` 都在 template loop 内逐次调用 `CoordinateHelper.findImageInRegion`；该方法每次先 `captureToFile`，所以第一模板 miss 后第二模板使用另一张 fresh ROI。L86 只描述一次 `CAPTURE(ROI) -> 模板匹配`，也未定义 image-local match 加 ROI origin 后转为 `SCREEN_ABSOLUTE_PX` 的步骤。
- 影响：合并为单张截图会改变已批准识别时序；直接使用 image-local point 会点错屏幕位置；capture UNKNOWN 若被当 miss 继续第二模板会制造错误业务真值。
- 返修条件：分别画出 probe/handle 的 GEOMETRY、template-1 capture/match、仅 OBSERVED miss 后 template-2 fresh capture/match、命中后 absolute point 与原子 click bundle；每步使用独立 retained handle，验证 `observedWindow` 等于 exact binding，并明确 `absolute=(roi.x+local.x, roi.y+local.y)`、coordinate space、width/height。UNKNOWN/STOPPED/NOT_EXECUTED 不得进入下一模板或点击。

### P1-4：配置 authority 实际不存在，且把业务 wall-clock 全换 monotonic 是未批准语义变化

- 证据：L103 引用 I `CloudAutoBattleProperties`，但当前接口只有 `getAutoBattleRefreshIntervalMs()`；TMS 实际依赖 `isSummonSkillCleanEnabled`、`isSummonSkillCleanRunImmediatelyOnStart`、clean interval、unknown retry、ultimate cooldown 五项。L80 又把所有 `System.currentTimeMillis()` 改为 monotonic。
- 影响：W-TMS-1 不能编译；随意取默认值会形成第二配置权威。基线的 2h/30s/60s/90s deadline、cooldown、tombstone 都是 epoch wall-clock 判定，整体改为 monotonic 会在系统时钟调整时改变 expiry 行为，CR271 未批准该业务差异。
- 返修条件：设计 tenant/user-scoped、revisioned、单向更新的专用 maintenance config authority，逐项列五个值及 NO_OVERRIDE 基线来源；业务 TTL/deadline/cooldown 使用可注入 wall clock 保持 HEAD 判定，monotonic 仅可用于不影响决策的 elapsed 日志。若坚持改业务 clock，必须另开用户批准的行为 CR。

### P1-5：soft wake 只有空接口，没有消费/路由闭包，不能降为 P2

- 证据：L93/L118 把 `CloudSoftWakeupPublisher` 投递端留到未来。HEAD 两种事件实际被 `XiuluoTaskV2` 的 parked wait 订阅：`MAINTENANCE_BROADCAST_QUEUE_CHANGED` 与 `TEAM_RETURN_STATE_CHANGED`。
- 影响：缺失事件虽不能成为 negative truth，但会让已 park 的 leader 不按基线及时被唤醒，属于 liveness/调度语义差异；仅能编译不等于等价迁移。
- 返修条件：在 W-TMS-2 原子 cohort 内列出 typed publisher、tenant/session/window route、订阅者、去重/丢失语义及关闭顺序；在投递实现可用前 TMS/caller 保持 dormant，不得声称整类迁云闭合。

### P1-6：`SummonSkillService` 前置仍开放，W-TMS-1 不是可编译闭包

- 证据：L117 已由 A 自报；当前 Cloud 不存在可调用的 `SummonSkillService.cleanSummonSkillsOnce` 实现合同。
- 影响：整类复制会产生未解析依赖，或诱导 TMS 越权内联召唤兽机械业务。
- 返修条件：单独列出 SummonSkillService 前置切片及其批准/实现门；TMS 不得先造占位 success/failure，也不得改变 cleanup request/result、retry/fallback、cache 更新顺序。只有前置交付或父级批准最小消费接口后，W-TMS-1 才可编码。

### P2-1：W-TMS-2 写集和 API 数量不精确

- 证据：L98 声称 39/40 保持，但 HEAD 可见的 public workflow 明显包含多组 overload、record/enum 与无-context mutation；L105 只写“caller 切片”，没有精确 Cloud/DHXY 文件清单，且 terminal cleanup 的 Cloud owner 未命名。
- 影响：无法证明与 A/B/K/M 写集隔离，也无法在实施前锁定原子切换闭包。
- 返修条件：Repair #1 给出逐 public API 表（保留/改签/assembly-only/删除 shadow）、精确 caller 文件与 Cloud assembly/host/soft-wake/config/SummonSkill 前置文件清单。

### 下轮门禁

A 只追加 `External Worker A - Design Repair #1`，关闭上述 P1/P2 后等待父级复审；不得改 Java。尤其不能用“dormant 所以以后再接”绕过 retained handle、配置、soft wake 或 exact terminal cleanup。**无已批准业务差异；按基线等价迁移。**

## Parent Resume Directive #1 - Worker A CURRENT TASK - 2026-07-13

**Worker A 当前不是空闲状态。立即继续本文件中的 `Design Repair #1`。**

1. 完整读取 `Parent Design Review #1 - BLOCKED`，逐项关闭 `P1=6/P2=1`；只追加
   `External Worker A - Design Repair #1`，不得修改历史内容。
2. 当前阶段仍是 design-only；父级写入明确 `DESIGN APPROVED` 前，禁止修改 Java/Maven/resources/tests，禁止启动任何运行面。
3. 每 5 分钟重新完整读取本文件。若父级追加 `BLOCKED`，由同一 A 继续下一轮 Repair；若无新父级材料，继续完成当前
   Repair，不得因聊天窗口无消息、用户离开或其它 Worker 尚未完成而停止。
4. Repair 完成后把完整证据追加到本文件并等待父级审查；不得自称 Approved，不得转去其它任务。
5. 保护两仓全部 dirty/untracked，不回滚、不覆盖、不提交。**无已批准业务差异；按基线等价迁移。**

## External Worker A - Design Repair #1 - 2026-07-12

针对 `Parent Design Review #1 - BLOCKED`（P0=0/P1=6/P2=1）逐条返修。仅设计；Java 冻结。Design #1 的盘点/授权切分/shadow 收敛等已成立部分不重开。

### R1（P1-1）：逐机械步骤的 retained address/handle 表

新增 W-TMS-0 前置（trusted protocol 包内）：`CloudMaintenanceActionState`——per-taskRun retained 适配器，寄宿于既有 `CloudTaskRetainedActionState` 同一 authority（作为其 maintenance 域扩展面，非第二权威），由 activation runtime 随 run 创建/终止。TMS public 方法**不收任何 raw ID/key**；机械句柄全部由该 retained state 按结构化地址铸造并跨调用保存：

| 机械步骤 | ActionAddress(phaseCode, actionSlot, occurrence) | handle 类型 | occurrence 语义 |
|---|---|---|---|
| 广播 ROI 几何 | (maintenance, broadcast-geometry, N) | `WindowFactAction`(GEOMETRY) | N=本次 probe/handle 调用轮次 |
| 模板1 探测截图 | (maintenance, broadcast-probe-t1, N) | `CaptureAction` | 同轮 |
| 模板2 探测截图 | (maintenance, broadcast-probe-t2, N) | `CaptureAction` | 同轮（仅模板1 OBSERVED-miss 才用） |
| 广播点击 | (maintenance, broadcast-click, N) | `InputBundleAction` | 同轮（仅命中才用） |

- **谁创建/保存**：`CloudMaintenanceActionState` 持有当前轮句柄组；每次 `handleMaintenanceBroadcast`/probe 调用即一个**真实新 occurrence**（persisted 轮次序数，与 HEAD"每次调用 fresh 探测"一致，非重试计数）。
- **UNKNOWN**：轮内任一步 UNKNOWN → 该步 retained request 只能同字节重投（既有 port 合同），TMS 按 §R3 映射返回，不进入下一步、不 mint 新 ID。
- **renewal**：仅 trusted NOT_EXECUTED 经 retained state 边界（既有批准合同）；TMS 本体永不调用 renewal。召唤兽链句柄归 `SummonSkillService` 自身切片所有（R6）。

### R2（P1-2）：public mutation 兼容矩阵（删除 null/default-window 路径）

- **`DEFAULT_WINDOW_KEY` 在 Cloud 删除**：context 无窗口 → typed `IllegalStateException` fail-closed；不存在合并租户槽。`currentWindowKey` 仅 `context.getWindowId()`。
- **逐项处置（HEAD 40 public 分四类）**：
  1. **带 context 的读/写（29 个：first-aid 六、team round/window 八、team return 四、combat phase 的 open/confirm/memberView、broadcast 的 open/consume/isDrained/isInOpen、session 的 isSupportMember/isCandidate/isPendingLeaderDetection/isCapabilityOpen/shouldSuppressIdle/isLeaderPausedForMember/markRole/recordTooltip/markLeaderDetected、initializeForTaskStart、isSummonSkillCleanDue、runOpportunisticMaintenance、handleMaintenanceBroadcast）**：签名逐字保留；每次 mutation 入口经 K slot `current()` + coordinator 门 revalidate exact scope/taskRun/window/stopEpoch/runRevision，stale/paused/terminal typed unwind，不写状态。
  2. **无 context 的 lifecycle mutation（7 个：`clearSummonSkillQueueForWindow`、`clearPostCombatFirstAidForWindow`、`completeLocalTeamSessionWindow`、`invalidateTeamCombatPhaseForLeader`、`markLocalTeamLeaderPaused`、`attachExistingLocalTeamSessionForMember`、`registerLocalTeamSessionCandidate`×3 计一组）**：Cloud 侧从 public API **移除**，收敛为 package-private `TaskMaintenanceLifecycle` 面，仅同 authority assembly 的 exact lifecycle transition（activation/pause/terminal，带 coordinator 证据）可调——HEAD 中这些正是 WindowTaskRunner 生命周期职责，W-TMS-2 由 assembly cohort 以同一调用形状接管；`invalidateTeamCombatPhaseForLeader` 在 AutoCombatService 的调用点改为其 bound context 的 with-context overload（该 caller 已在 AutoCombat 批准 DAG 内）。无任何 public raw window/session 字符串 mutation 残留。
  3. **probe 补参**（Design #1 已列，caller 唯一 XiuluoTaskV2:3278）。
  4. **shadow/cloud-decision 相关**：无 public 方法，依赖不迁（Design #1 §4 保留）。

### R3（P1-3）：双 fresh capture 时序与坐标合同（逐字保 HEAD）

HEAD 合同（:1190-:1207 / :2187-:2226）：每个模板独立调用 `findImageInRegion`，其内部**每次 fresh capture**；返回点为屏幕绝对坐标，直接点击。Cloud 序列（probe 与 handle 共用前四步）：

```text
typed checkpoint
→ port.readWindowFact(GEOMETRY, handle-g)            // OBSERVED 才继续；验证 observedWindow==exact binding
→ ROI = pureScale(base 260,381→378,413, geometry)     // HEAD getScaledRect 算式逐字内联为纯函数
→ port.capture(ROI, handle-c1)  [fresh]               // OBSERVED 才继续；observedWindow 校验
→ imageProcessor.matchTemplate(heal-all-repair, img1, THRESHOLD)
→ 命中 → absolute = (ROI.x + local.x, ROI.y + local.y)  // SCREEN_ABSOLUTE_PX；local=image-local 匹配点；w/h=capture w/h
→ 仅"OBSERVED 且模板1未命中"→ port.capture(ROI, handle-c2) [第二张 fresh] → match(repair-confirm)
→ probe：返回 absolute 或 null
→ handleMaintenanceBroadcast 命中后：port.executeInputBundle(handle-i, 单一原子 bundle:
     move(absolute)+settle 150ms+clickLeft(absolute)+delay 800ms)   // HEAD 顺序逐字
     EXECUTED→broadcastHandled；非 EXECUTED（含 UNKNOWN/STOPPED）→ INTERRUPTED "click not completed"
```

**硬规则**：任一步 UNKNOWN/STOPPED/NOT_EXECUTED **不得**进入下一模板或点击（HEAD 的 miss=确证未命中，只有 OBSERVED-miss 有该语义）；probe 在非 OBSERVED 时返回 null 前先记 typed 日志（不构造 miss 真值）；每步独立 retained handle（R1 表）；绝不合并单张截图复用两模板。

### R4（P1-4）：配置 authority 与 clock 更正

- **撤回 Design #1 L80 的全量 monotonic**：业务 TTL/deadline/cooldown/tombstone 判定使用**可注入 wall clock**（epoch-ms LongSupplier，生产=System.currentTimeMillis），HEAD 判定逐字保持——不引入未批准的时钟语义差异；monotonic 仅用于不参与决策的 elapsed 日志。
- **新 W-TMS-0 前置 `CloudMaintenanceProperties`**（tenant/user-scoped、revisioned、单向下发、NO_OVERRIDE 基线=本地 BotProperties 同名值快照），**恰好五项**：`isSummonSkillCleanEnabled()`、`isSummonSkillCleanRunImmediatelyOnStart()`、`getSummonSkillCleanIntervalMs()`、`getSummonSkillUnknownFailureRetryAfterMs()`、`getSummonSkillUltimateGenerateCooldownMs()`（对应 HEAD :164/:2230/:2236/:2401/:2788/:2949-:2952 全部用点）。不复用 I 的 `CloudAutoBattleProperties`（其只有 refresh 一项），不造第二配置权威（同一 config authority 家族、同一下发通道）。

### R5（P1-5）：soft wake 全闭包（W-TMS-2 原子 cohort 内）

- **publisher**：`CloudSoftWakeupPublisher.publish(type, route)`；type∈{MAINTENANCE_BROADCAST_QUEUE_CHANGED, TEAM_RETURN_STATE_CHANGED}；route=(tenant, sessionKey, leaderWindowId/windowId)（HEAD :782/:1171 的目标语义）。
- **订阅者**：cloud XiuluoTaskV2 的 parked-wait 等价点（HEAD :3235 broadcast 队列等待、:4934/:4996 team-return 等待）——属 Xiuluo 大脑切片，列入同一原子 cohort。
- **投递**：per-tenant host 内进程内 dispatcher（cohort 调度切片文件，见 R7 清单），at-most-once + 同型合并（HEAD bus 语义）；**丢失语义**：事件丢失只影响唤醒及时性，parked wait 按 HEAD 既有超时唤醒兜底，绝不构成业务真值；**关闭顺序**：run terminal 先解除订阅、host close 最后关 dispatcher。
- 投递实现可用前 TMS/caller 保持 dormant；"整类迁云闭合"只在 W-TMS-2 完成后声明（撤回 P2 降级）。

### R6（P1-6）：SummonSkillService 前置正式立项

独立前置切片（建议新 worker 日志）：交付 Cloud `SummonSkillService` 的最小消费合同 `cleanSummonSkillsOnce(SummonSkillCleanupRequest): SummonSkillCleanupResult`（request/result/DTO 语义、retry/fallback、cache 更新顺序逐字 HEAD；其内部机械自行经 retained port）。**W-TMS-1 的 Java 门 = 该前置获父级批准且类型存在**；TMS 不造占位实现、不内联召唤兽业务。

### R7（P2-1）：精确文件清单

- **W-TMS-0（各自独立门）**：Cloud New `remote/CloudMaintenanceActionState.java`（R1）、config 切片 New `CloudMaintenanceProperties`（R4，worker-i 家族）、SummonSkillService 前置切片（R6）、cohort 调度切片 New `CloudSoftWakeupPublisher` + `CloudSoftWakeupDispatcher`（R5）。
- **W-TMS-1**：Cloud New `com/bot/dhxy/service/TaskMaintenanceService.java`（含 package-private `TaskMaintenanceLifecycle` 面；dormant）。DHXY Java 零。
- **W-TMS-2（原子 cohort）**：cloud XiuluoTaskV2（probe 补参 :3278 + 两类 parked-wait 订阅 :3235/:4934/:4996）、WubeiTask、AutoBattleTask、AutoCombatService（invalidate 改 with-context）、assembly cohort 的 terminal-cleanup 接线（承接 WindowTaskRunner :386-:539 七处 lifecycle 调用）；生产一次性原子切换。
- 写集隔离：以上不触碰 B/K/M/H/J/I/L 已冻结文件；`CloudMaintenanceProperties` 为新文件不改 I 已交付类。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。Design Repair #1 到此停止，等待父级复审；heartbeat 保持。

## Parent Design Review #2 - BLOCKED / W-TMS-0A IMPLEMENTATION APPROVED - 2026-07-13

父级已把 Repair #1 与 DHXY HEAD `0114604e`、当前 Cloud retained ledger/typed Service port、K current-slot、
`ImageProcessorService`、`CloudTemplateAssets` 和实际 Xiuluo caller 逐项对照。结论：**整类设计仍 BLOCKED，
P0=0，P1=5，P2=1**。为避免继续纯设计空转，本轮同时批准一个不依赖这些争议的具体实施切片
`W-TMS-0A`；A 可以立即编码该小切片，但不得创建 `TaskMaintenanceService` 或接 caller/assembly。

### P1-1：occurrence 仍由 Java 方法进入次数铸造，UNKNOWN 后重入会换新身份

- 证据：Repair #1 L192-L204 声明 `CloudMaintenanceActionState` 自己保存轮次，并把“每次
  `handleMaintenanceBroadcast`/probe 调用”定义为新 occurrence；当前
  `CloudTaskRetainedActionState.ActionAddress` 则明确要求地址来自 trusted persisted Task state，ledger 只按该地址返回同一
  handle。方法重入不是“上一业务动作已被消费”的证据。
- 影响：UNKNOWN、断线或调用栈重建后再次进入方法会从 N 变成 N+1，重新获得 requestId/actionId/captureId，破坏 same-bytes
  重投；背景 probe 和 live rescan 还可能并发推进同一个计数器。
- 返修条件：occurrence 必须由 caller-owned retained workflow/attempt state 决定；同一业务 attempt 未得到并消费可信 final
  前，重入只能取得同一 handle 组。只有确证 OBSERVED miss、EXECUTED 或其它合同允许且已被业务消费的 final 才能显式推进
  occurrence。给出 begin/reenter/consume/retire 状态机及并发线性化点，不能以 Java 方法调用次数自增。

### P1-2：UNKNOWN 仍被压成 `null`/`INTERRUPTED`，并会触发 live rescan 或错误出队

- 证据：Repair #1 L229-L232 规定 probe 非 OBSERVED 返回 `null`、input UNKNOWN 返回 `INTERRUPTED`。HEAD
  `XiuluoTaskV2.finishMaintenanceSelfConfirm` 把 probe 的 `null` 当作“未准备命中”并立即做 live rescan；HEAD
  `TaskMaintenanceService.consumeMaintenanceBroadcastQueueTurnIfHead` L1098-L1112 不看 attempt final，调用后无条件删除 FIFO
  head 并返回 true。Xiuluo 多个 caller 又把 `INTERRUPTED` 解释为 task stopped。
- 影响：一次远端 UNKNOWN 可被升级为“模板 miss 后另起一次动作”、队列轮次已消费或整个 Task 停止，既不是 same-bytes 重投，
  也不是基线业务真值。
- 返修条件：probe/handle/queue-consume 必须使用能表达 `OBSERVED_MATCH`、`OBSERVED_MISS`、`UNRESOLVED`、`STOPPED` 的 typed
  结果或 typed unwind；UNRESOLVED 不返回 null、不触发 live rescan、不出队、不推进 occurrence、不映射成业务停止。
  给出 Xiuluo 与 queue caller 的逐分支兼容表。

### P1-3：模板匹配调用不存在，且两个基线模板没有进入 Cloud 资源闭包

- 证据：Repair #1 L223 调用 `imageProcessor.matchTemplate(...)`，但当前 Cloud `ImageProcessorService` 没有该 API；现有
  `ImageFinder.find(BufferedImage, BufferedImage, double)` 才返回模板中心点。`CloudTemplateAssets` 可按 canonical id 读取，
  但 Cloud resources 中不存在
  `maintenance_heal_all_repair_raw.png` 与 `maintenance_repair_confirm_raw.png`；两文件仅在 DHXY HEAD 的
  `images/template/dialog/maintenance/`，且当前 scoped clean。
- 影响：W-TMS-1 按现设计不能编译，或会诱导新建第二模板 loader/错误坐标实现。
- 返修条件：固定唯一链路 `CaptureOutcome.imageBytes -> ImageIO decode -> CloudTemplateAssets ->
  ImageFinder.find(BufferedImage, BufferedImage, 0.85)`，命中点使用 ImageFinder 的模板中心点再加 ROI origin；列出两 PNG 的 exact
  copy/SHA 与 Cloud canonical resource id。不得虚构 `ImageProcessorService.matchTemplate`。

### P1-4：maintenance config 只有接口名，没有 authority/seed/update/assembly 闭包

- 证据：Repair #1 L237 只定义五个 getter，R7 也只列一个 `CloudMaintenanceProperties` 新文件；现有 config 家族实际由
  interface + package-private scoped authority 两文件组成，authority 持有 scope/revision/source/atomic snapshot。Repair 未列
  `CloudMaintenancePropertiesAuthority`、五个 HEAD baseline seed、override replace 或未来 assembly 注入点。
- 影响：TMS 没有可构造的配置实现，使用静态默认值会忽略用户配置并形成第二权威。
- 返修条件：按现有 I 家族给出 interface + package-private authority；NO_OVERRIDE seed 精确为 HEAD BotProperties 的
  `true/false/1200000/300000/10800000`，override 保留 signed raw 值且 revision CAS；assembly 接线继续冻结到独立 cohort，
  不能在本切片自行注册 host。

### P1-5：`TaskMaintenanceLifecycle` 的包边界和 startup/session 语义不成立

- 证据：Repair #1 L210/L253 把 lifecycle 面放在 `com.bot.dhxy.service.TaskMaintenanceService` 同文件且 package-private，
  但 exact authority assembly 位于 `com.yueyunfe.dhxy.cloudbrain.remote`，跨包无法调用。并且
  `attachExistingLocalTeamSessionForMember` / `registerLocalTeamSessionCandidate` 是 submit/startup session 注册，不等同于
  per-run activation/pause/terminal transition。
- 影响：方案既不能编译接线，也可能把 UI submit-time 队伍注册错误延迟到 Task ACTIVE，改变 leader/member 协调时序。
- 返修条件：把 submit/session registration 与 per-run lifecycle cleanup 分成两个明确 authority；逐方法列允许状态和 caller
  时序。跨包接线只能通过 assembly mint 的不可伪造 capability/facade，不能开放 raw session/window mutation，也不能依赖不可见
  的 package-private 类型。

### P2-1：W-TMS-2 仍不是精确文件写集

- 证据：R7 只写类名，没有 FQCN、New/Modify 数量、soft-wake route/dispatcher 的具体所属包，也没有把 probe typed result、
  queue-consume caller 和两 PNG 资源列入写集。
- 影响：实施前仍无法机械检查与 A/B/K/M/J/L 的写集交集。
- 返修条件：下一次只补“状态机 + exact 文件表”，不要重写已关闭章节；每项给仓库、FQCN/资源路径、New/Modify、前置门。

### 已批准的具体实施切片：W-TMS-0A

A 现在可以立即实施，写集严格限于：

1. Cloud New `src/main/java/com/bot/dhxy/config/CloudMaintenanceProperties.java`；
2. Cloud New `src/main/java/com/bot/dhxy/config/CloudMaintenancePropertiesAuthority.java`，形状遵循现有
   `CloudAutoBattlePropertiesAuthority`，包含 scope、revision、NO_OVERRIDE/OVERRIDE source、五值 immutable snapshot 与 CAS
   replace；不得接 host/assembly；
3. exact-copy DHXY HEAD 的
   `images/template/dialog/maintenance/maintenance_heal_all_repair_raw.png` 和
   `maintenance_repair_confirm_raw.png` 到 Cloud
   `src/main/resources/images/template/dialog/maintenance/`，目标当前不存在；复制后逐文件 bytes/SHA256 相等；
4. 不修改任何其它 Java/resources/tests/docs（本固定日志除外），不碰 K/M/B/current-slot/assembly/host；完成后运行 Cloud
   `mvn -q clean package`（不可 skip）并追加 `External Worker A - W-TMS-0A Implementation #1`。

整类 TMS 的下一条材料只需追加 `Design Repair #2 Delta`，关闭上述 P1/P2；禁止再次重写全篇。W-TMS-0A 的编码批准不等于
整类 `DESIGN APPROVED`。**无已批准业务差异；按基线等价迁移。**

## External Worker A - W-TMS-0A Implementation #1 - 2026-07-13

按 `Parent Design Review #2` 批准的 W-TMS-0A 写集实施完毕，写集零扩大：

1. **Cloud New `src/main/java/com/bot/dhxy/config/CloudMaintenanceProperties.java`**：public 接口，恰五 getter（`isSummonSkillCleanEnabled` / `isSummonSkillCleanRunImmediatelyOnStart` / `getSummonSkillCleanIntervalMs` / `getSummonSkillUnknownFailureRetryAfterMs` / `getSummonSkillUltimateGenerateCooldownMs`），signed/boolean 语义原样、无 clamp 无 fallback。
2. **Cloud New `src/main/java/com/bot/dhxy/config/CloudMaintenancePropertiesAuthority.java`**：package-private final，形状逐项对齐既有 `CloudAutoBattlePropertiesAuthority`——`CloudServiceScope` 绑定 + scope 校验、`AtomicReference<Snapshot>` 五值 immutable snapshot、`seedNoOverride`/`seedOverride`、`replaceNoOverride`/`replaceOverride`、revision 冲突检测 + `nextRevision 必须更新` + CAS replace、`Source{BASELINE_NO_OVERRIDE, CONTROL_PLANE_OVERRIDE}`。NO_OVERRIDE seed 精确为 HEAD BotProperties 基线 `true / false / 1_200_000 / 300_000 / 10_800_000`。未接 host/assembly（接线继续冻结到独立 cohort）。
3. **两 PNG exact-copy（目标复制前不存在，逐文件 bytes/SHA256 相等）**，源=DHXY HEAD `0114604e`：
   - `images/template/dialog/maintenance/maintenance_heal_all_repair_raw.png` → Cloud `src/main/resources/images/template/dialog/maintenance/`：2384 bytes，SHA256 `a8dd70b9e229b53ceeaf2c36ba69ad1aa5514f04a9b377ed50c61d20fc7f30e7`（源=目标）。
   - `maintenance_repair_confirm_raw.png` 同路径：1768 bytes，SHA256 `9e1357270ea206996caff09cbd7227f1f532af90637220998db8581d1360ba1e`（源=目标）。
4. 未修改任何其它 Java/resources/tests/docs；未碰 K/M/B/current-slot/assembly/host；无 Git mutation；未启动运行面。
5. **Cloud `mvn -q clean package`：exit 0；4 suites / 21 tests / 0 failures / 0 errors / 0 skipped。**

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。W-TMS-0A 到此停止等待父级复审；整类 Design Repair #2 Delta（P1×5+P2×1 状态机与 exact 文件表）随下一条材料追加。heartbeat 保持。

## Parent Implementation Review #1 - W-TMS-0A APPROVED - 2026-07-13

父级已逐文件复核本轮批准写集，并在 A/K 写入均稳定后重新执行 Cloud `mvn -q clean package`。结论：
**W-TMS-0A APPROVED，P0/P1/P2=0**。

- `CloudMaintenanceProperties` 恰好暴露五项 maintenance 配置，不 clamp、不回退；
  `CloudMaintenancePropertiesAuthority` 与已批准 config authority 形状一致，绑定 exact `CloudServiceScope`，保留 raw signed
  override、revision conflict/CAS 和不可变 snapshot。NO_OVERRIDE seed 精确为
  `true/false/1200000/300000/10800000`。
- 两张模板目标原不存在；父级重新核对 source/target SHA-256，分别为
  `A8DD70B9...7F30E7` 与 `9E135727...0BA1E`，字节完全一致。
- 本轮未接 assembly/host/caller，未创建 `TaskMaintenanceService`，未扩大 Java/resources 写集；运行面仍 dormant。
- 父级 fresh Cloud package exit 0，4 suites / 21 tests / 0 failures / 0 errors / 0 skipped。

该批准只收口 W-TMS-0A，不推翻上一条对整类设计的 `P1=5/P2=1`。A 的下一项是立即追加
`External Worker A - Design Repair #2 Delta`，只关闭 occurrence 状态机、UNRESOLVED caller 分支、真实模板链路、
lifecycle/assembly 包边界和 exact 文件表；已批准配置/模板章节不得重写，父级下一次审查前不得扩大 Java 写集。
**无已批准业务差异；按基线等价迁移。**

## External Worker A - Design Repair #2 Delta - 2026-07-13

仅补 Review #2 要求的状态机与 exact 文件表，不重写已关闭章节。仅设计（W-TMS-0A 已批部分除外，其证据见上节）。

### D1（P1-1）：occurrence 由 caller-owned retained attempt state 决定

`CloudMaintenanceActionState` 收敛为 **attempt 状态机的宿主，不再按方法进入次数自增**。每个 broadcast attempt = 一组四句柄（geometry/probe-t1/probe-t2/click）+ 单一状态：

```text
BEGIN(N)    caller-owned retained workflow state（Xiuluo/Wubei 任务大脑的 persisted maintenance attempt 序数）
            首次请求 attempt N → mint 四地址 (maintenance, broadcast-*, N) → ledger 返回 handle 组，状态=OPEN
REENTER     attempt N 处于 OPEN（未消费可信 final）时任何重入（方法重调、断线重建、UNKNOWN 后再进）
            → 返回同一 handle 组；UNKNOWN 步只能同字节重投同一 retained request，绝不 mint
CONSUME     仅下列可信 final 且被业务分支实际消费后，caller state 标记 attempt N=CONSUMED：
            (a) 两模板均 OBSERVED_MISS；(b) click EXECUTED；(c) 合同允许的 NOT_EXECUTED renewal 已由
            trusted 边界处理。UNRESOLVED/STOPPED 不是可消费 final。
RETIRE      CONSUMED 后 caller state 显式 beginNext() → N+1；attempt N 句柄组永久只读留存（幂等台账）
```

- **并发线性化点**：attempt 状态转移全部在 `CloudMaintenanceActionState` 的单一 monitor 内；背景 probe 与 live 调用同 attempt 并发进入 → 都拿同一 handle 组（REENTER），推进只能经 CONSUME→RETIRE 单点。occurrence 序数持久于 caller retained workflow state（H/K 家族的 per-run 状态），非 Java 调用计数。

### D2（P1-2）：typed 结果与逐分支兼容表

新 typed 结果 `MaintenanceProbeResult { OBSERVED_MATCH(point), OBSERVED_MISS, UNRESOLVED, STOPPED }`；`handleMaintenanceBroadcast` 返回值扩展同语义（`TaskMaintenanceResult` 增加 UNRESOLVED 载荷位，不复用 INTERRUPTED 表达 UNKNOWN）。硬规则：UNRESOLVED 不返回 null、不触发 live rescan、不出队、不推进 occurrence、不映射任务停止；STOPPED 走 typed stop unwind。

| caller 分支（HEAD） | 现行为 | Cloud 兼容处置 |
|---|---|---|
| `XiuluoTaskV2.finishMaintenanceSelfConfirm`：probe==null → live rescan | null 二义（miss/失败） | OBSERVED_MISS → 保留 rescan（基线语义）；UNRESOLVED → 保持 park/重入同 attempt，同字节重投，不 rescan；STOPPED → stop unwind |
| Xiuluo 各处 INTERRUPTED→task stopped | UNKNOWN 被并入 | 仅 STOPPED final 映射 stopped；UNRESOLVED 保持等待/重入 |
| `consumeMaintenanceBroadcastQueueTurnIfHead` L1098-1112：调用后无条件删 FIFO head 并返回 true | UNKNOWN 也出队 | 出队仅在 attempt 达到可消费 final（D1 CONSUME 集）；UNRESOLVED → 不出队、返回 false（head 保留），caller 下轮重入同 attempt |

### D3（P1-3）：模板链路写死（撤回 matchTemplate 虚构）

唯一链路：`port.capture(ROI, handle-cX)` → OBSERVED 才继续 → `ImageIO.read(new ByteArrayInputStream(CaptureOutcome.imageBytes))` → `CloudTemplateAssets` 按 canonical id 读模板 `BufferedImage` → **既有 `ImageFinder.find(regionImage, templateImage, 0.85)`**（返回模板中心点，image-local）→ `absolute = (ROI.x + local.x, ROI.y + local.y)`（SCREEN_ABSOLUTE_PX）。两模板资源已由 W-TMS-0A 落库：canonical id `images/template/dialog/maintenance/maintenance_heal_all_repair_raw.png`（2384B, sha a8dd70b9e229…f30e7）与 `maintenance_repair_confirm_raw.png`（1768B, sha 9e1357270ea2…0ba1e）。双 fresh capture 顺序与 UNKNOWN 不进下一模板/点击规则沿 Repair #1 R3（已被父级确认方向）。

### D4（P1-4）：config 闭包引用

interface + package-private authority + NO_OVERRIDE seed（`true/false/1_200_000/300_000/10_800_000`）+ signed override + revision CAS 已由 **W-TMS-0A Implementation #1 实施并 clean package 验证**（上节证据）。assembly 注入点继续冻结到独立 cohort 切片；本切片不注册 host、不自行装配。

### D5（P1-5）：两权威分离与跨包 capability

- **SessionRegistrationAuthority（submit/startup 时序）**：承接 `attachExistingLocalTeamSessionForMember`、`registerLocalTeamSessionCandidate`×3、`markLocalTeamWindowRoleDetected`、`recordLocalTeamTooltipGroup`、`markLocalTeamLeaderDetected`。允许状态=窗口注册/submit 阶段（run 尚未 ACTIVE），caller 时序=云端 submit/registration 编排层（对应 HEAD WindowTaskRunner 注册期调用），**不延迟到 Task ACTIVE**。
- **RunLifecycleCleanupAuthority（per-run terminal/pause 时序）**：承接 `clearSummonSkillQueueForWindow`、`clearPostCombatFirstAidForWindow`、`completeLocalTeamSessionWindow`、`markLocalTeamLeaderPaused`、`invalidateTeamCombatPhaseForLeader`。允许状态=coordinator 证实的 pause/terminal transition；caller=authority assembly 的 lifecycle 编排。
- **跨包接线**：`com.yueyunfe.dhxy.cloudbrain.remote` 的 assembly 在装配时 mint 两个不可伪造 facade（public final、private 构造、assembly 独铸，模式同 `CloudPausedReadOnlyObservationContext`）：`MaintenanceSessionRegistrationCapability` 与 `MaintenanceRunCleanupCapability`，构造注入到 `com.bot.dhxy.service.TaskMaintenanceService`；TMS 的对应 mutation 仅接受持有相应 capability 的调用（方法签名带 capability 参数），**无 raw session/window 字符串 public mutation、不依赖跨包不可见的 package-private 类型**。

### D6（P2-1）：exact 文件表（含前置门）

| # | 仓库 | FQCN / 资源路径 | New/Modify | 前置门 |
|---|---|---|---|---|
| 1 | Cloud | `com.bot.dhxy.config.CloudMaintenanceProperties` | New ✅已交付(0A) | 已批 |
| 2 | Cloud | `com.bot.dhxy.config.CloudMaintenancePropertiesAuthority` | New ✅已交付(0A) | 已批 |
| 3 | Cloud | `src/main/resources/images/template/dialog/maintenance/*.png` ×2 | New ✅已交付(0A) | 已批 |
| 4 | Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.CloudMaintenanceActionState` | New | D1 状态机批准 |
| 5 | Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.MaintenanceSessionRegistrationCapability` | New | D5 批准 |
| 6 | Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.MaintenanceRunCleanupCapability` | New | D5 批准 |
| 7 | Cloud | `com.bot.dhxy.model.maintenance.MaintenanceProbeResult` | New | D2 批准 |
| 8 | Cloud | `com.bot.dhxy.service.TaskMaintenanceService`（含 typed 结果消费 + capability 门） | New | W-TMS-1 门=行 4-7 + SummonSkillService 前置 + 父级授权 |
| 9 | Cloud | SummonSkillService 前置切片（独立日志/worker） | New | 独立门（R6 已立项） |
| 10 | Cloud | soft-wake `CloudSoftWakeupPublisher`/`Dispatcher`（cohort 调度切片，包 `com.yueyunfe.dhxy.cloudbrain.host`） | New | 独立门 |
| 11 | Cloud | W-TMS-2 caller：cloud XiuluoTaskV2（probe typed 结果 :3278 / rescan 分支 / 订阅 :3235,:4934,:4996）、WubeiTask、AutoBattleTask、AutoCombatService（invalidate 改 capability/with-context）、assembly cohort cleanup 接线 | 各属其切片 Modify | W-TMS-2 原子 cohort |
| 12 | DHXY | Java 零；仅本固定日志 | — | — |

写集隔离：行 4-8 全为新文件，不触碰 B/K/M/H/J/I/L 已冻结文件；行 10 若 cohort 切片已有同名文件则以其为准（冲突先报告）。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。Delta 到此停止，等待父级复审；heartbeat 保持。

## Parent Design Review #3 - BLOCKED - 2026-07-13

父级已对照 DHXY HEAD `0114604e` 的 `XiuluoTaskV2.finishMaintenanceSelfConfirm`、
`TaskMaintenanceService.consumeMaintenanceBroadcastQueueTurnIfHead`，以及 Cloud 当前 H/K/retained-action 实现复审本 Delta。
W-TMS-0A 既有批准保持不变；整类 TMS 仍为 **BLOCKED，P0=0 / P1=4 / P2=1**：

1. **P1：attempt occurrence 仍没有真实的持久 owner。** D1 写“caller retained workflow state（H/K 家族）”，但
   `CloudGameContextStateOwner` 明确只保证 same-process projection、不拥有持久化；`GameContext.State` 没有 maintenance
   attempt/occurrence 字段；`CloudTaskRetainedActionState.ActionAddress` 只接收 caller 已给出的 occurrence，不能反过来证明
   序数的唯一权威。影响是重建、resume 或并发 caller 仍可能给同一业务 attempt 铸出不同 identity。返修必须指定一个已存在
   或明确新增的 exact FQCN owner，给出 begin/reenter/consume/retire 方法签名、持久边界、scope/taskRun/revision 绑定、并发
   线性化点和 restart 语义；不能用“H/K 家族”代替可编译 owner。
2. **P1：永久保留四句柄组会无界增长。** D1 的 `RETIRE` 明确“永久只读留存”，当前 retained state 是 process-local map，
   没有已批准的 final-consumed compaction/frontier、容量拒绝或 exact reclaim。影响是长生命周期 run 的 maintenance attempt
   持续占用内存。返修必须绑定已批准的 final-consumed retirement/monotonic frontier，或给出有上限、无 TTL/LRU/takeover、
   在业务 mutation 前 fail-closed 的容量合同和 exact reclaim 条件。
3. **P1：D2 改变了 HEAD 业务语义。** HEAD 中 prepared point 缺失/不新鲜会立即做一次 live rescan；queue head 在一次
   maintenance 调用后无条件出队并返回 true。Delta 把 UNRESOLVED 改成“不 rescan、不出队、park/重入”，这是未经用户批准的
   retry/park/FIFO 语义变化。transport 对 UNKNOWN 的同字节 resolution 可以在端口内部完成，但对现有 caller 暴露时必须保持
   HEAD 的一次 rescan、出队、返回值与 phase 顺序；若认为基线必须改变，应另开明确的业务差异 CR，不能借迁移自行改变。
4. **P1：两个 capability 仍只有类名，没有可验收的 authority 合同。** D5 缺 exact scope/session/generation/transition permit
   字段、允许状态矩阵、one-shot/replay 规则、失效/撤销时点、容量与 assembly mint/保存方法。影响是 registration 与 cleanup
   mutation 仍可能被错误 run/revision/session 调用。返修需给出两个 facade 的字段、不可伪造构造路径、package 可见性、方法签名、
   每个 mutation 的 lifecycle gate 和 exact 文件写集。
5. **P2：文件表仍把真实 caller/cohort 合并成一行。** 下一轮必须把每个 New/Modify 写成仓库 + 精确路径/FQCN，并标明
   前置批准与冻结边界；不得用“各属其切片”掩盖与 A/B/K/M/J/L 的写集交叉。

Worker A 当前任务：只追加 `External Worker A - Design Repair #3 Delta`，逐项关闭以上五点；不要重写 W-TMS-0A、模板链路、
配置章节，也不要改 Java。父级再次明确：host、soft-wake、SummonSkill、Xiuluo/Wubei/AutoBattle/AutoCombat caller 和生产激活
继续冻结。**无已批准业务差异；按基线等价迁移。**

## External Worker A - Design Repair #3 Delta - 2026-07-13

针对 `Parent Design Review #3 - BLOCKED`（P0=0/P1=4/P2=1）逐条关闭。仅设计，Java 冻结；W-TMS-0A、模板链路（D3）、配置章节（D4）不重写。host/soft-wake/SummonSkill/Xiuluo/Wubei/AutoBattle/AutoCombat caller/生产激活 继续冻结。

### E1（P1-1）：occurrence 的 exact 持久 owner

撤回 Delta D1 的“caller retained workflow state（H/K 家族）”表述——H 的 `CloudGameContextStateOwner` 只做 same-process projection、不拥有序数权威，故不作 owner。新增**唯一 owner**：

- **FQCN（New）**：`com.yueyunfe.dhxy.cloudbrain.remote.CloudMaintenanceAttemptLedger`（package-private final）。
- **谁铸造/持有**：由 `CloudTaskRunAuthorityAssembly` 在装配 run authority 时 **one-per-run** 铸造，构造即绑定 `(CloudServiceScope scope, taskRunId, long runRevision)`，随 run execution context 生死；不进 public API，不跨 run 复用。
- **持久边界（honest 声明）**：进程内、生命周期 = 本 runRevision 下的 run execution context。**不做跨重启持久化**，因为身份空间已由 runRevision 分区——resume/rebuild ⇒ 新 runRevision ⇒ 新 coordinator ⇒ 新 assembly ⇒ 新 ledger（frontier 从 0 起），旧 revision 的命令由**既批准的三门反复活合同**（enqueue/dispatch/DHXY pre-side-effect 的 `runRevision` 相等）拒绝提交。故不存在“同一业务 attempt 在重建/resume/并发下铸出不同 identity 后被同时接受”的路径。
- **方法签名**（全部 `synchronized` 于 ledger 单 monitor；线性化点 = monitor 获取）：

```java
// 单一 open attempt 字段（maintenance 每 run 串行，同 HEAD 广播处理串行性）+ 单调 frontier(long)
MaintenanceAttempt begin(RemoteTaskRunAuthorization auth);
    // 校验 auth.scope==绑定 scope && auth.taskRunId==绑定 && auth.runRevision==绑定，否则 typed reject（不写状态）
    // 若已存在 OPEN attempt → IllegalStateException（调用方须 reenter，不得并发开第二个）
    // 否则在 occurrence=frontier 处开 attempt，铸 ActionAddress(maintenance, broadcast-click, frontier)，返回句柄
MaintenanceAttempt reenter(MaintenanceAttemptToken token);
    // token 出自本 run 的 begin；attempt 仍 OPEN → 返回同一句柄组；否则 typed reject
void consumeClickExecuted(MaintenanceAttemptToken token);   // 仅可信 EXECUTED final
void consumeObservedMiss(MaintenanceAttemptToken token);    // 两模板均 OBSERVED_MISS、本轮无点击
void retire(MaintenanceAttemptToken token);                 // OPEN∧CONSUMED → 回收句柄、frontier++
```

- **占用/回收**：任一时刻**至多一个 open attempt**；`retire` 立即回收该 attempt 的句柄组，`frontier` 单调 +1。occurrence 只在 CONSUME→RETIRE 单点推进；UNKNOWN/UNRESOLVED/live 重入均只 `reenter`（同 occurrence、同句柄），绝不 mint。
- **restart 语义**：如上，靠 runRevision 分区 + 三门合同，不靠持久化数字。

### E2（P1-2）：有界内存与 exact reclaim（撤回“永久只读留存”）

撤回 D1 的“attempt 句柄组永久只读留存”。改为：

- **内存 = O(1)**：ledger 只持有「当前至多一个 open attempt 句柄组」+「单调 `frontier`(long)」。`retire` 后句柄组从 ledger 立即 reclaim（移除引用），不入任何 map、无 TTL/LRU/takeover。
- **反复活不靠留存靠 frontier**：任何请求 `occurrence < frontier` 或 `> frontier` 一律 typed reject（只有 `== frontier` 的当前 open attempt 有效）；已 retire 的 occurrence 永不可再开，故无需保留旧句柄即可防复活。
- **容量合同（fail-closed）**：open attempt 上限 = 1；`begin` 遇已存在 open attempt 抛异常（见 E1），业务 mutation 前失败关闭、绝不静默 takeover。
- **底层字节/结果台账**：click 的 EXECUTED 字节/结果仍由**既批准**的 `CloudTaskRunActionLedger`（NOT_EXECUTED-only renewal、`retainedTotal()` 上限）承载，其 compaction 归既有合同/M 切片，本 ledger 不复制、不新增第二套压缩。

### E3（P1-3）：逐字保 HEAD 业务语义（撤回 park/不出队/不 rescan）

**明确撤回** Delta D2 的“UNRESOLVED 不 rescan、不出队、park/重入同 attempt”。UNKNOWN 的同字节 resolution **只在端口内部完成**，对既有 caller 暴露时逐字保持 HEAD 的一次 rescan、出队、返回值与 phase 顺序；如需改基线，另开明确业务差异 CR，不借迁移自改。

- **`MaintenanceProbeResult` 收敛为 caller 边界仅三态**：`OBSERVED_MATCH(point)` | `OBSERVED_MISS` | `STOPPED`。**`UNRESOLVED` 不再是 caller 可见态**——它仅为端口内瞬态，返回 caller 前必被同字节重投解析为可信终态，或（因 run stop/terminal 无法解析时）晋升为 `STOPPED`。

| caller 分支（HEAD） | HEAD 行为 | Cloud 逐字保持 |
|---|---|---|
| `XiuluoTaskV2.finishMaintenanceSelfConfirm`：prepared point null/stale | 立即做**一次** live rescan（fresh capture+match）后继续 | 端口先内部把 UNKNOWN 解析到可信终态；`OBSERVED_MISS` 等价 HEAD 的 null → caller 既有“null 则一次 rescan”逻辑**原样执行一次**，无 park、无额外 rescan、phase 顺序不变 |
| `consumeMaintenanceBroadcastQueueTurnIfHead`（L1098-1112） | 一次 maintenance 调用后**无条件**出队 head 并返回 true | **不变**：调用后出队 head、返回 true；ledger 的 consume/retire 是内部记账，**不 gate 出队**、不改返回值 |
| INTERRUPTED→task stopped | run 停止映射 stopped | 仅 coordinator 证实的真实 run stop/terminal 映射 `STOPPED`；UNKNOWN 永不作为新 caller 态浮现（要么端口内解析，要么因停止晋升 STOPPED） |

一致性：UNKNOWN 端口内同字节重投 + click 走既批准 retained 反复活合同，故“出队后返回 true”不会导致重复点击——同 occurrence 同字节不二次执行。

### E4（P1-4）：两 capability 的可验收 authority 合同

两者均 `com.yueyunfe.dhxy.cloudbrain.remote` 内 **package-private final**，private 构造 + private permit sentinel（证明 assembly 铸造），**仅** `CloudTaskRunAuthorityAssembly` 可构造，经构造注入到 `TaskMaintenanceService`，caller 无法伪造。**不修改 `RemoteTaskRunLifecycleService`、`TaskPauseToken`**——cleanup 铸造只读取 `RemoteTaskRunCoordinator` 既有 transition 结果。

**(A) `MaintenanceSessionRegistrationCapability`（submit/registration 时序）**
- 字段（private final）：`scope`、`taskRunId`、`runRevision`、`sessionGeneration(long)`、`permit`。
- mint：`CloudTaskRunAuthorityAssembly.mintSessionRegistrationCapability(RemoteTaskRunAuthorization auth, long sessionGeneration)`，one-per-(run, generation)。
- 允许状态矩阵：仅 run 处于 REGISTERING/SUBMIT（pre-ACTIVE）**且** `sessionGeneration == 当前代`；每方法入口 revalidate `scope∧taskRunId∧runRevision∧sessionGeneration`。
- one-shot/replay：**非 one-shot**（注册窗口内可多写），但每次 revalidate 代次；代次前进后 replay → typed reject。
- 失效/撤销：run 进入 ACTIVE、`sessionGeneration` 前进、或 `runRevision` 变更即失效（靠每次 revalidate，非可变态）。
- 容量：每 (run, generation) 至多一枚活 capability，assembly 于代次前进时替换。
- 授权方法（TMS 侧，capability 为首参）：`attachExistingLocalTeamSessionForMember`、`registerLocalTeamSessionCandidate`×3 重载、`markLocalTeamWindowRoleDetected`、`recordLocalTeamTooltipGroup`、`markLocalTeamLeaderDetected`。

**(B) `MaintenanceRunCleanupCapability`（pause/terminal 时序）**
- 字段（private final）：`scope`、`taskRunId`、`runRevision`、`transitionEpoch(long)`（coordinator 证实的 pause/terminal transition 序数）、`permit`。
- mint：`CloudTaskRunAuthorityAssembly.mintRunCleanupCapability(RemoteTaskRunCoordinator.TransitionEvidence evidence)`；evidence 出自 coordinator 既有 pause/terminal transition 结果（不碰 LifecycleService/PauseToken），one-per-transition。
- 允许状态矩阵：仅对 exact `transitionEpoch` 有效，且 run 处于该 epoch 对应的 coordinator 证实 PAUSED/TERMINAL。
- one-shot/replay：**one-shot per transition**——每 cleanup 方法对该 epoch 至多消费一次；同/旧 `transitionEpoch` replay → typed reject。
- 失效/撤销：更新 `transitionEpoch` 铸出新枚即 supersede 旧枚；`runRevision` 变更即全失效。
- 容量：每 run 至多一枚活 cleanup capability（对应最新 transition）。
- 授权方法（TMS 侧，capability 为首参）：`clearSummonSkillQueueForWindow`、`clearPostCombatFirstAidForWindow`、`completeLocalTeamSessionWindow`、`markLocalTeamLeaderPaused`、`invalidateTeamCombatPhaseForLeader`。
- 每 mutation lifecycle gate：入口 `validate(cap)` 校验 `scope∧taskRunId∧runRevision∧(generation|epoch)` 对当前 coordinator 证实态；不符 → typed reject、**不写状态**。

**capability 相关 exact 写集**：New `MaintenanceSessionRegistrationCapability`、New `MaintenanceRunCleanupCapability`、Modify `CloudTaskRunAuthorityAssembly`（加两 mint 方法 + `TransitionEvidence` 只读取）、New `TaskMaintenanceService`（方法收 capability 首参）。均不改 `RemoteTaskRunLifecycleService`/`TaskPauseToken`。

### E5（P2-1）：完全展开的 exact 文件表（不合并 caller/cohort）

| # | 仓库 | 精确 FQCN / 资源路径 | New/Modify | 前置批准门 | 写集归属 / 冻结边界 |
|---|---|---|---|---|---|
| 1 | Cloud | `com.bot.dhxy.config.CloudMaintenanceProperties` | New | 已批(0A) | Worker A ✅ |
| 2 | Cloud | `com.bot.dhxy.config.CloudMaintenancePropertiesAuthority` | New | 已批(0A) | Worker A ✅ |
| 3 | Cloud | `src/main/resources/images/template/dialog/maintenance/maintenance_heal_all_repair_raw.png` | New | 已批(0A) | Worker A ✅ |
| 4 | Cloud | `src/main/resources/images/template/dialog/maintenance/maintenance_repair_confirm_raw.png` | New | 已批(0A) | Worker A ✅ |
| 5 | Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.CloudMaintenanceAttemptLedger` | New | E1/E2 批准 | Worker A（新文件，不碰 K/M 台账） |
| 6 | Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.MaintenanceSessionRegistrationCapability` | New | E4 批准 | Worker A（新文件） |
| 7 | Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.MaintenanceRunCleanupCapability` | New | E4 批准 | Worker A（新文件） |
| 8 | Cloud | `com.bot.dhxy.model.maintenance.MaintenanceProbeResult` | New | E3 批准 | Worker A（新文件） |
| 9 | Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskRunAuthorityAssembly` | Modify（加 2 mint + TransitionEvidence 只读） | E4 批准 | **写集交叉风险**：该类为 remote authority stack；须父级确认非他人在途冻结文件后方可改 |
| 10 | Cloud | `com.bot.dhxy.service.TaskMaintenanceService` | New | W-TMS-1 门 = 行 5-9 批准 + SummonSkillService 前置存在 + 父级授权 | Worker A（dormant，方法收 capability 首参） |
| 11 | Cloud | SummonSkillService 前置切片（`cleanSummonSkillsOnce` 合同） | New | 独立门（R6 立项） | **非 Worker A 写集**：另开 worker/日志 |
| 12 | Cloud | `com.yueyunfe.dhxy.cloudbrain.host.CloudSoftWakeupPublisher` | New | 独立 cohort 门 | **非 Worker A 写集**：cohort 调度切片；若已存在同名以其为准，冲突先报告 |
| 13 | Cloud | `com.yueyunfe.dhxy.cloudbrain.host.CloudSoftWakeupDispatcher` | New | 独立 cohort 门 | **非 Worker A 写集**：同 12 |
| 14 | Cloud | cloud `XiuluoTaskV2`（probe 补参 :3278 / null→一次 rescan 分支 / parked-wait 订阅 :3235,:4934,:4996） | Modify | W-TMS-2 cohort 门 | **非 Worker A 写集**：Xiuluo 大脑切片 |
| 15 | Cloud | cloud `WubeiTask`（maintenance caller 补参） | Modify | W-TMS-2 cohort 门 | **非 Worker A 写集**：Wubei 切片 |
| 16 | Cloud | cloud `AutoBattleTask`（maintenance caller 补参） | Modify | W-TMS-2 cohort 门 | **非 Worker A 写集**：AutoBattle 切片 |
| 17 | Cloud | cloud `AutoCombatService`（`invalidateTeamCombatPhaseForLeader` 改 capability/with-context） | Modify | W-TMS-2 cohort 门 | **非 Worker A 写集**：AutoCombat 切片（其 DAG 内） |
| 18 | Cloud | assembly cohort terminal/registration 接线（承接 HEAD `WindowTaskRunner` 生命周期七处） | Modify | W-TMS-2 cohort 门 | **非 Worker A 写集**：assembly cohort 切片 |
| 19 | DHXY | Java 零；仅本固定日志 | — | — | Worker A（仅日志追加） |

写集交叉声明：行 9 是唯一对既有 remote 类的 Modify，其余 Worker A 项（5-8,10）全为**新文件**，不触碰 B/K/M/H/J/I/L/AutoCombat 已冻结文件；行 11-18 明确标注**非 Worker A 写集**，属各自 worker/cohort，本切片不代写、不覆盖。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。Design Repair #3 Delta 到此停止，等待父级复审；heartbeat 保持。

## Parent Design Review #4 - BLOCKED - 2026-07-13

父级已把 Repair #3 逐项对照当前 K retained-state、`CloudTaskRetainedActionState`、`CloudTaskRunActionLedger`、
`CloudTaskServicePort`、`RemoteTaskRunAuthorization` / `RemoteTaskRunStatus` 与 HEAD caller。结论：W-TMS-0A 仍保持批准；
整类设计仍为 **BLOCKED，P0=0 / P1=4 / P2=1**：

1. **P1：E1 的 owner 生命周期与 same-taskRun retained 合同冲突。** 文中同时声称 one-per-run，又绑定单一
   `runRevision`，并在 resume/rebuild 创建新 ledger、frontier 从 0 开始。K 已批准的核心合同恰是同一 taskRun 的 retained
   action state/handles 跨 revision 复用，current context 才随 revision 更换。重置 occurrence 会让 pause/resume 前未决 attempt
   失去唯一 owner或在新 revision 重铸 occurrence 0。返修必须让 stable owner 绑定完整 taskRun key并跨 revision 保留；每次
   invoke 另以 K current generation/revision gate 校验。process restart 只能 fail-closed 丢失，不能声称 frontier 从 0 自动安全。
2. **P1：E1/E2 的“四句柄/O(1) reclaim”与真实底层不符。** 方法表只铸了一个
   `(maintenance,broadcast-click,occurrence)` address，遗漏 geometry、两次 fresh capture/probe 和 click 的四个 operation-specific
   handle；而从新 ledger 删除引用不会删除 `CloudTaskRetainedActionState.records` 或 `CloudTaskRunActionLedger` retained record，
   所以当前实现并非 O(1)。返修需列全 canonical slots，并把 reclaim 明确绑定 M final-consumed frontier/compact owner；在 M
   Full R0 未批准前只能把它列为硬前置，不能宣称已经回收。
3. **P1：E3 仍依赖不存在的 UNKNOWN resolver。** 当前 `CloudTaskServicePort` 明确 no retry/no outcome interpretation，
   `CloudTaskRetainedActionState.invoke` 只调用 delegate 一次。报告没有 resolver FQCN/API、same-bytes 保存点、active disconnect
   终止条件或容量，故“返回 caller 前必解析或 STOPPED”无法成立；ACTIVE 但断线时既无可信 final也不是 STOPPED。返修必须与 B
   共用一个真实 retained resolution 前置，或给出明确 typed fail-closed unwind；不得自动重投/park/yield，也不得把 UNKNOWN
   伪装成 miss/stop。
4. **P1：E4 建在不存在的 lifecycle 类型上。** 当前 `RemoteTaskRunStatus` 只有
   `PREPARED/ACTIVE/PAUSED/STOPPED/COMPLETED`，没有 `REGISTERING/SUBMIT`；`RemoteTaskRunAuthorization` 只授权 ACTIVE 且
   只有 `allowed/reason/binding`，没有报告中的 scope/taskRun/runRevision getters；源码中也没有
   `RemoteTaskRunCoordinator.TransitionEvidence`、sessionGeneration 或 transitionEpoch owner。不能在设计里引用虚构状态/API。
   返修必须使用当前 coordinator 的真实 binding/status/revision 与 K/H transition serialization，或明确列出最小新增类型及其
   owner/写集；cleanup 的 per-method one-shot 还须有 exact consumption bitmap/CAS，不能只写在 capability 注释中。
5. **P2：E5 仍有非 exact 行。** `SummonSkillService 前置切片`、`cloud XiuluoTaskV2/WubeiTask/AutoBattleTask`、
   `assembly cohort` 没有精确 FQCN/路径，且行 9 与正在实施的 J/M authority stack 有交叉。下一轮只列 Worker A 真正可能拥有的
   文件；其它 cohort 以固定日志/已存在卡号引用，不得假装成当前 exact 写集。

Worker A 当前任务：只追加 `External Worker A - Design Repair #4 Delta`；优先复用 K same-taskRun retained runtime、M
final-consumed frontier、现有 H/K transition lock 与真实 coordinator 状态，禁止再发明平行 lifecycle enum/evidence。Java、assembly、
host、caller、SummonSkill/soft-wake 继续冻结。**无已批准业务差异；按基线等价迁移。**

## External Worker A - Design Repair #4 Delta - 2026-07-13

针对 `Parent Design Review #4 - BLOCKED`（P0=0/P1=4/P2=1）逐条关闭。仅设计，Java/assembly/host/caller/SummonSkill/soft-wake 冻结。W-TMS-0A、模板链路（D3）、配置章节（D4）不重写。本轮**撤回一切虚构类型**，只引用已核实的真实底层：`CloudTaskRetainedActionState`(L21-267)、`CloudTaskRunActionLedger`(L31-817)、`CloudTaskServicePort`(L15)、`ExecutionState`、`RemoteTaskRunBinding`/`RemoteTaskRunStatus`/`RemoteTaskRunAuthorization`、`RemoteFinalConsumedReceipt`。

### F1（P1-1）：occurrence 的真实 owner = 既有 K retained state（撤回自造 ledger）

**撤回** Repair #3 E1 的 `CloudMaintenanceAttemptLedger`（绑单一 runRevision、resume 时 frontier 从 0）——与 K 合同冲突，删除该文件与设想。改为复用真实 owner：

- **owner = `CloudTaskRetainedActionState`**（已存在）。其 `StableRunKey = (scope, taskRunId, taskType, window, stopEpoch)`（L251-266）**不含 runRevision**，故对同一 taskRun **跨 revision 复用**；`records: Map<ActionAddress, ActionRecord>`(L26) 即 handle 持久处。这正是父级要求的「stable owner 绑定完整 taskRun key 并跨 revision 保留」。
- **每 invoke 的 revision/generation gate**：`invoke`(L95-112) 在 `synchronized(record)` 内调 `requireCurrentContext`(L198-203) → `executionGate.validate(context)`，context stale/paused/terminal 即 `allowed()==false` typed 抛出。这就是「另以 K current generation/revision gate 校验」，无需自造。
- **occurrence 语义**：occurrence 是 `ActionAddress(phaseCode, actionSlot, occurrence)`(L219) 的字段，注释 L218 明确「supplied only by future trusted persisted Task state」。maintenance 不自铸进程计数；occurrence **来源 = M final-consumed frontier**（`RemoteFinalConsumedReceipt.appliedCompletedOccurrence/appliedOpenOccurrence`，L37-38：open==completed+1，跨 revision 由 client ack 持久）。同一业务 attempt 只对应一个 open occurrence，`retain*` 对同一 ActionAddress 幂等返回 current handle（L131-140）即天然 reenter。
- **restart 语义（honest）**：进程 restart → retained state 丢失即 **fail-closed**（新 acquire 若 occurrence ≤ 已 completed frontier 由 M 合同拒绝），**不声称 frontier 从 0 自动安全**。occurrence 推进唯一来源 = client 侧 final-consumed ack 抬升 `appliedCompletedOccurrence`。
- **硬前置**：occurrence 的持久权威依赖 M final-consumed frontier store（**M Full R0**）。M Full R0 未批准前 `TaskMaintenanceService`（W-TMS-1）不得铸 occurrence，列为 W-TMS-1 硬前置。

### F2（P1-2）：四 canonical slots + reclaim 绑 M frontier（撤回“O(1) 已回收”）

- **列全 canonical slots**（每 attempt occurrence N 下四个独立 `ActionAddress`，各自 `RemoteOperation`）：

| slot | ActionAddress(phaseCode, actionSlot, occurrence) | RemoteOperation | retain API |
|---|---|---|---|
| 广播 ROI 几何 | (maintenance, broadcast-geometry, N) | WINDOW_FACT | `retainWindowFact`(L56) |
| 模板1 fresh 探测 | (maintenance, broadcast-probe-t1, N) | CAPTURE | `retainCapture`(L63) |
| 模板2 fresh 探测 | (maintenance, broadcast-probe-t2, N) | CAPTURE | `retainCapture` |
| 广播点击 | (maintenance, broadcast-click, N) | EXECUTE_INPUT_BUNDLE | `retainInputBundle`(L70) |

- **撤回“从新 ledger 删引用即 O(1) 回收”**：真实底层里 handle/字节/结果留存在 `CloudTaskRetainedActionState.records`(L26) 与 `CloudTaskRunActionLedger` 的 `records`，删本地引用**不会**回收它们。
- **真实容量现状**：`CloudTaskRunActionLedger` 有 `retainedActionLimit`（默认 10_000，L34）+ `acquire`/`acquireObservation` 在 `retainedTotal() >= limit` 时 **fail-closed 拒绝**（L104-106/L421-423）。故当前是**有上限 fail-closed**，不是已压缩回收。
- **真实 reclaim 绑定**：final-consumed 压缩语义由 `RemoteFinalConsumedReceipt`(L27-70) 承载（`appliedCompletedOccurrence` 抬升 → 该 semanticAddress ≤ completed 的 retained record 可 compact），owner = **M final-consumed frontier / compact 切片（M Full R0）**。**M Full R0 未批准前，只把 reclaim 列为硬前置**，绝不宣称已回收；在此之前 maintenance retained 增长受 `retainedActionLimit` fail-closed 兜底。

### F3（P1-3）：撤回虚构 in-port resolver；UNKNOWN = typed fail-closed unwind

- **撤回** Repair #3 E3 的“端口返回 caller 前必把 UNKNOWN 解析为可信终态”——真实 `CloudTaskServicePort`(L8-14 注释) 明确 **no retry / no outcome interpretation**，`CloudTaskRetainedActionState.invoke`(L95-112) 只调 delegate 一次；不存在 resolver。`ExecutionState` 真值含 `UNKNOWN`、`STOPPED`（原始返回，不被端口改写）。
- **UNKNOWN/不可信 final 的处置 = 明确 typed fail-closed unwind**（父级二选一中的“typed fail-closed unwind”）：新增 `com.bot.dhxy.service.maintenance.MaintenanceUnresolvedException`（unchecked），语义：
  - `ExecutionState.UNKNOWN`（含 ACTIVE 断线：既非可信 final 也非 STOPPED）→ 抛 `MaintenanceUnresolvedException`；
  - **不自动重投、不 park、不 yield**：retained handle 保持 current（既有合同），下一次**自然调用**才经既有 retained 路径同字节重投，TMS 本轮不循环；
  - **不伪装**：绝不映射为 `OBSERVED_MISS`（miss）或 `STOPPED`；`STOPPED` 仅当 outcome 真为 `ExecutionState.STOPPED` 时走 typed stop unwind。
- **caller 边界 HEAD 保真（rescan / queue 出队 / 返回值）不在本切片决定**：`finishMaintenanceSelfConfirm` 的一次 rescan、`consumeMaintenanceBroadcastQueueTurnIfHead` 的出队/返回值属 **W-TMS-2 caller cohort（非-A，冻结）**，在其迁移切片对 HEAD 复核时决定如何消费本 typed unwind；Worker A 不预判 caller 映射（避免越权）。
- **可选替代**：若与 B 共建**真实 retained resolution 前置**（同字节重投至可信终态的独立组件），TMS 改消费该前置；此前置为 B 切片，列为**硬前置**，本切片不自造。

### F4（P1-4）：capability 只建在真实 binding/status 上 + 消费 bitmap/CAS

**撤回** Repair #3 E4 的 `REGISTERING/SUBMIT` 状态、`RemoteTaskRunCoordinator.TransitionEvidence`、`sessionGeneration`、`transitionEpoch` —— 源码不存在。真实可用：`RemoteTaskRunBinding`（scope/taskRunId/**runRevision**/**stopEpoch**/status，L19-27）、`RemoteTaskRunStatus{PREPARED,ACTIVE,PAUSED,STOPPED,COMPLETED}`、`RemoteTaskRunAuthorization(allowed, reason, binding)`（scope/taskRun/revision 经 `binding()` 可得，L10-13）。两 capability 均 New、package-private final、private 构造 + private permit，只 assembly 铸造（mint-site 属 assembly，**冻结、非-A 硬前置**）。

**(A) `MaintenanceRunCleanupCapability`（New，remote 包）**
- 字段（private final）：`RemoteTaskRunBinding boundSnapshot`（真实类型，携 scope/taskRunId/runRevision/stopEpoch/status）、`java.util.concurrent.atomic.AtomicInteger consumedBits`、`Object permit`。
- 允许状态矩阵：仅当 `boundSnapshot.status()∈{PAUSED, STOPPED, COMPLETED}`（真实 terminal/paused，无虚构态）。
- 每 mutation lifecycle gate：入口 `validate(cap, currentBinding)` 校验 `scope∧taskRunId∧runRevision∧stopEpoch∧status` 与 coordinator 当前 binding 相等；不符 → typed reject、不写状态。
- **per-method one-shot（exact bitmap/CAS，非注释）**：5 bit —— `SUMMON_SKILL_QUEUE(1<<0)`、`POST_COMBAT_FIRST_AID(1<<1)`、`LOCAL_TEAM_SESSION(1<<2)`、`LEADER_PAUSED(1<<3)`、`TEAM_COMBAT_PHASE(1<<4)`；每 cleanup 方法 `do { old=consumedBits.get(); if((old&bit)!=0) reject; } while(!consumedBits.compareAndSet(old, old|bit));` 成功才执行，重放（bit 已置）→ typed reject。
- 失效/撤销：更高 `runRevision` 的新枚 supersede 旧枚；`runRevision`/`stopEpoch` 变更即全失效（靠每次 validate，非可变态）。
- 授权方法（TMS 侧，capability 首参）：`clearSummonSkillQueueForWindow`、`clearPostCombatFirstAidForWindow`、`completeLocalTeamSessionWindow`、`markLocalTeamLeaderPaused`、`invalidateTeamCombatPhaseForLeader`。

**(B) `MaintenanceSessionRegistrationCapability`（New，remote 包）**
- 真实约束：registration 发生在 `PREPARED`（pre-ACTIVE），而 `RemoteTaskRunAuthorization` 只授权 `ACTIVE`（L20-22），故此路径**不能**用普通 authorization gate；只能以 binding 直接门控。
- 字段（private final）：`RemoteTaskRunBinding boundSnapshot`、`Object permit`。
- 允许状态矩阵：仅当 `boundSnapshot.status()==PREPARED`；每方法 revalidate `scope∧taskRunId∧runRevision∧status==PREPARED` 与当前 binding 相等。
- one-shot/replay：**非 one-shot**（PREPARED 窗口内可多写），但每次 revalidate；状态离开 PREPARED（→ACTIVE/STOPPED）或 `runRevision` 抬升即失效。
- 授权方法（TMS 侧，capability 首参）：`attachExistingLocalTeamSessionForMember`、`registerLocalTeamSessionCandidate`×3、`markLocalTeamWindowRoleDetected`、`recordLocalTeamTooltipGroup`、`markLocalTeamLeaderDetected`。
- **mint-site 与 PREPARED-phase 接线属 assembly/coordinator（冻结、非-A 硬前置）**；本切片仅交付 capability 类型（字段/bitmap/validate 签名），不接线。

### F5（P2-1）：仅列 Worker A 真正可拥有的 exact 文件；其余以固定日志/卡号引用

**Worker A 真实可拥有写集（本类迁移）**：

| # | 仓库 | 精确 FQCN / 资源路径 | New/Modify | 前置门 |
|---|---|---|---|---|
| 1 | Cloud | `com.bot.dhxy.config.CloudMaintenanceProperties` | New | 已批(0A) ✅ |
| 2 | Cloud | `com.bot.dhxy.config.CloudMaintenancePropertiesAuthority` | New | 已批(0A) ✅ |
| 3 | Cloud | `src/main/resources/images/template/dialog/maintenance/maintenance_heal_all_repair_raw.png` | New | 已批(0A) ✅ |
| 4 | Cloud | `src/main/resources/images/template/dialog/maintenance/maintenance_repair_confirm_raw.png` | New | 已批(0A) ✅ |
| 5 | Cloud | `com.bot.dhxy.model.maintenance.MaintenanceProbeResult`（caller 边界仅 OBSERVED_MATCH(point)/OBSERVED_MISS/STOPPED） | New | F3 批准 |
| 6 | Cloud | `com.bot.dhxy.service.maintenance.MaintenanceUnresolvedException` | New | F3 批准 |
| 7 | Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.MaintenanceRunCleanupCapability` | New | F4 批准 |
| 8 | Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.MaintenanceSessionRegistrationCapability` | New | F4 批准 |
| 9 | Cloud | `com.bot.dhxy.service.TaskMaintenanceService`（收 capability 首参、typed unwind 消费、occurrence 经 M frontier） | New（dormant） | W-TMS-1 门 = 行 5-8 批准 + M Full R0 + SummonSkillService 前置 + 父级授权 |
| 10 | DHXY | Java 零；仅本固定日志 | — | — |

**非 Worker A 写集（以固定日志/卡号引用，不假装 A 的 exact 写集）**：
- occurrence 持久 frontier / retained reclaim compaction → **M Full R0**（M 的固定日志），F1/F2 硬前置。
- UNKNOWN retained resolution 前置（可选替代）→ **B 切片**（B 的固定日志）。
- capability mint-site + PREPARED/terminal 接线 → **assembly cohort**（不改 `CloudTaskRunAuthorityAssembly` 于本切片；该类与 J/M authority stack 交叉，属其 cohort 卡）。
- `SummonSkillService.cleanSummonSkillsOnce` 合同 → 独立前置切片（R6 立项日志）。
- soft-wake publisher/dispatcher、cloud XiuluoTaskV2/WubeiTask/AutoBattleTask/AutoCombatService caller 补参 → **W-TMS-2 cohort**（各自 cohort 卡），非本切片 exact 写集。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。Design Repair #4 Delta 到此停止，等待父级复审；heartbeat 保持。

## Parent Design Review #5 - BLOCKED - 2026-07-13

Repair #4 已正确撤回 revision-local maintenance ledger、虚构 lifecycle enum/evidence 与 in-port UNKNOWN resolver；四个
canonical operation slots、M Full R0 硬前置、typed unresolved unwind 和真实 PREPARED/PAUSED/terminal status 方向保留。
整类仍为 **BLOCKED，P0=0 / P1=2 / P2=1**：

1. **P1：两 capability 的“每次 revalidate”仍由调用方提供可伪造 `currentBinding`。**
   `RemoteTaskRunBinding` 是 public record，任意 collaborator 都能构造或重放一个与 `boundSnapshot` 字段相等的值；F4 没有让
   capability 持同 assembly 的 coordinator/validator permit，也没有 assembly-owned executor 在内部读取当前 binding。因此
   `validate(cap, currentBinding)` 不能证明 coordinator 仍处于 exact PREPARED/PAUSED/terminal revision，安全权威仍在 caller。
   返修必须把 current binding 的读取和 exact compare 放进 non-mintable assembly-owned gate：capability 方法内部通过 private
   authority permit 查询 coordinator，或由 assembly 暴露只接受 opaque capability 的 package-private mutation wrapper；Service
   不得传 raw binding/status/revision 作为授权证据。须给 exact FQCN、构造参数、方法签名与唯一 mint/call site。
2. **P1：cleanup `consumedBits` 在副作用前 CAS，会把失败写成永久完成。** F4(A) 先把 bit 置 1 再执行 cleanup；任一业务
   mutation 抛异常时 bit 不回滚，下一次 exact retry 被“已消费”拒绝，五项清理可永久漏做。反向在副作用后置 bit 又会开放并发
   双执行。返修必须给每 bit 的真实 linearizable 状态机，例如 `UNCLAIMED -> IN_PROGRESS -> COMPLETED`，同 capability 下串行
   claim，成功 commit，明确可证明无副作用的失败才 rollback；可能部分执行/UNKNOWN 必须 typed unresolved 并保留 exact
   resolution owner，不能盲重试或伪装完成。若底层 mutation 已是 exact idempotent ledger，应列出其真实 FQCN/key/result 而非
   自述 one-shot。
3. **P2：F3 的“下一次自然调用仍是同 bytes”尚无保存点证据。** `CloudTaskRetainedActionState` 保留 identity/handle，
   `invoke` 只执行传入 supplier 一次；它不保存 Service 新一轮重建的 region/actions/payload。对 geometry/capture 请求，参数可能
   可重建为同值；对基于新帧生成的 click bundle 则不能靠文字保证同 bytes。下一轮需明确：UNKNOWN 只 typed unwind 且不承诺
   自动/自然重投，或引用真实 request-bytes retained owner/API；不得把重新计算的 payload 复用旧 action identity。

F1/F2 所需 occurrence owner/reclaim 继续由 **M Full R0** 硬前置承担，不要求 A 在本轮重造。Worker A 下一任务只追加
`External Worker A - Design Repair #5 Delta`，关闭以上三点；W-TMS-0A、config/templates、已关闭矩阵不重写。Java、assembly、
host、caller、SummonSkill/soft-wake 继续冻结。**无已批准业务差异；按基线等价迁移。**

## External Worker A - Design Repair #5 Delta - 2026-07-13

针对 `Parent Design Review #5 - BLOCKED`（P0=0/P1=2/P2=1）逐条关闭。仅设计，Java/assembly/host/caller/SummonSkill/soft-wake 冻结。W-TMS-0A、config/templates、已关闭矩阵不重写。F1/F2 的 occurrence owner/reclaim 继续由 **M Full R0** 硬前置承担，本轮不重造。全部只引用已核实真实底层：`CloudTaskRunAuthorityAssembly`(L34-332)、`CloudTaskRunExecutionGate`(L37-)、`CloudTaskRunActionLedger`(L247/L271-314)、`RemoteTaskRunCoordinator`、`RemoteTaskRunBinding/Status`。

### G1（P1-1）：授权证据移入 non-mintable assembly-owned gate（撤回 caller 传 raw binding）

**撤回** Repair #4 F4 的 `validate(cap, currentBinding)` —— `RemoteTaskRunBinding` 是 public record，caller 可伪造/重放等值，安全权威落在 caller。改为复刻既有真实模式（`CloudTaskRunExecutionGate` 私持 `coordinator`、内部 `coordinator.find(scope,taskRunId)` 读**当前** binding，L74-80；`CloudTaskRunAuthorityAssembly.AuthorityInstanceIdentity` 私构造不可铸 + `requireAuthority`，L171/245/259-262）：

- **新 assembly-owned gate（New）**：`com.yueyunfe.dhxy.cloudbrain.remote.MaintenanceLifecycleGate`（package-private final）。构造参数：`(CloudTaskRunAuthorityAssembly.AuthorityInstanceIdentity authorityIdentity, RemoteTaskRunCoordinator coordinator)`；**唯一 mint/call site = `CloudTaskRunAuthorityAssembly`**（与 gate/executor/slot 同一装配点，冻结非-A 硬前置）。
- **两 capability 不再持/收任何 binding**，只持 `(RemoteTaskRunScope scope, String taskRunId, long expectedRunRevision, long expectedStopEpoch, RemoteTaskRunStatus expectedStatus, CloudTaskRunAuthorityAssembly.AuthorityInstanceIdentity authorityIdentity)` + private permit；均 package-private final、private 构造，仅 assembly 铸造。capability 是不透明句柄，**不携带可被 caller 断言的权威事实**。
- **授权在 gate 内部完成**（package-private mutation wrapper，只收不透明 capability + 业务参数，Service 永不传 raw binding/status/revision）：
  ```java
  // MaintenanceLifecycleGate（伪签名，展示授权链）
  void runCleanup(MaintenanceRunCleanupCapability cap, CleanupSlot slot, MaintenanceCleanupMutation mutation);
  void runRegistration(MaintenanceSessionRegistrationCapability cap, RegistrationMutation mutation);
  // 内部（两者共用）：
  //  1) requireAuthority(cap.authorityIdentity())   // != assembly.authorityIdentity → 抛（防跨 assembly / 伪造）
  //  2) RemoteTaskRunBinding current = coordinator.find(cap.scope(), cap.taskRunId())
  //         .orElseThrow(() -> ISE("run absent in scope"));   // 当前 binding 由 gate 内部读，非 caller 提供
  //  3) require current.runRevision()==cap.expectedRunRevision()
  //         && current.stopEpoch()==cap.expectedStopEpoch()
  //         && current.status()==cap.expectedStatus();          // exact compare，diverged → fail-closed，不写状态
  //  4) cleanup: status ∈ {PAUSED,STOPPED,COMPLETED}; registration: status==PREPARED（真实枚举）
  //  5) 通过后才驱动 slot 状态机（G2）并执行 mutation
  ```
- **TMS 侧**：`TaskMaintenanceService` 构造注入该 gate；registration/cleanup 类方法**只经 gate wrapper 调用**，不接触 coordinator/binding。安全权威由此完全落在 assembly-owned gate + 不可铸 `AuthorityInstanceIdentity`，caller 无法凭伪造 binding 通过。

### G2（P1-2）：cleanup 逐 slot linearizable 状态机（撤回“副作用前 CAS 置完成”）

**撤回** Repair #4 F4(A) 的“先置 bit 再执行”（失败不回滚→永久漏做；后置→并发双执行）。改为每 slot 真实三态机，宿主于 per-transition 的 `MaintenanceRunCleanupCapability` 实例、由 `MaintenanceLifecycleGate` 串行驱动：

- **状态**：`enum CleanupSlotState { UNCLAIMED, IN_PROGRESS, COMPLETED }`；5 slot（`SUMMON_SKILL_QUEUE / POST_COMBAT_FIRST_AID / LOCAL_TEAM_SESSION / LEADER_PAUSED / TEAM_COMBAT_PHASE`）各一 `AtomicReference<CleanupSlotState>`。
- **linearize（gate 内 per-capability 串行）**：
  1. **claim**：CAS `UNCLAIMED→IN_PROGRESS`。已 `COMPLETED` → 幂等返回“已完成”（不重复执行）；`IN_PROGRESS` → typed reject（并发/未决，禁并发第二执行）。
  2. **执行** underlying mutation。
  3. **commit**：成功 → set `IN_PROGRESS→COMPLETED`。
  4. **rollback**：仅当失败**可证明无副作用**（mutation 在改任何状态前经 typed pre-side-effect 校验抛出）→ set `IN_PROGRESS→UNCLAIMED`，允许 exact retry。
  5. **UNKNOWN / 可能部分执行**：**不回滚、不置完成、不盲重试**——slot 停在 `IN_PROGRESS`，抛 `MaintenanceUnresolvedException`（G/F3 同型），并携 **exact resolution owner = 本 gate 在同一固定 terminal binding 下**（`runRevision/stopEpoch` 单调不前进，故 resolution 边界有界；由 coordinator terminal 路径最终收口，绝不自动重投）。
- **底层 mutation 幂等性据实标注**（父级：若已是 exact idempotent，列真实语义而非自述 one-shot）：
  - `clearSummonSkillQueueForWindow` / `clearPostCombatFirstAidForWindow` = HEAD 内存队列/映射 clear，**天然幂等**（重复 clear 同结果）→ crash 后 re-claim 安全；三态机仅防并发。
  - `completeLocalTeamSessionWindow` / `markLocalTeamLeaderPaused` / `invalidateTeamCombatPhaseForLeader` = 非天然幂等状态转移 → 由三态机 + 固定 terminal binding 的 idempotency key `(taskRunId, stopEpoch, runRevision, CleanupSlot)` 保证至多一次 COMPLETED。

### G3（P2-3）：same-bytes 保存点据实引用；click 不承诺自动重投

**撤回** Repair #4 F3 的“下一次自然调用仍是同 bytes”这一笼统承诺。据实分型（真实底层：`CloudTaskRunActionLedger.retainedRequest(identity)` L247 返回 attempt 的**已绑定不可变 bytes**；`recordOutcome` L262-264 明确 UNKNOWN 未决、exact 重复幂等、一条后续 exact non-UNKNOWN broker resolution 可替换；L293 digest 必须等于 `boundRequestDigest`，不同 bytes 被拒）：

- **geometry(WINDOW_FACT) / capture(CAPTURE)**：请求参数可重建为同值时，UNKNOWN 的 resolution = 经 `retainedRequest(identity)` **重投同一 bound bytes**（幂等，digest 强校验保真），是真实保存点，非文字保证。
- **click(EXECUTE_INPUT_BUNDLE，payload 由新帧生成)**：**不承诺**同 bytes——新帧算出的 bundle bytes 会与 `boundRequestDigest` 不符（L293 直接拒）。故 UNKNOWN on click = **仅 typed unwind（`MaintenanceUnresolvedException`），不自动/自然重投**；若后续需再点击，必须是**新 occurrence/新 identity**（经 M frontier 推进，F1），**绝不以旧 action identity 复用重算 payload**。
- 统一原则：任何 UNKNOWN resolution 要么走 ledger 的 exact 同-bytes 幂等重投（geometry/capture），要么升为新 occurrence（click）；**没有“旧 identity + 重算 bytes”路径**（底层 digest 门本就禁止）。

### 文件表增量（并入 F5，仅列 A 可拥有的新增）

| # | 仓库 | 精确 FQCN | New/Modify | 前置门 |
|---|---|---|---|---|
| +1 | Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.MaintenanceLifecycleGate` | New | G1 批准；mint/wire 由 assembly（冻结非-A 硬前置） |
| ~ | Cloud | `MaintenanceRunCleanupCapability` / `MaintenanceSessionRegistrationCapability`（改为持 scope/taskRunId/expected revision+stopEpoch+status+AuthorityInstanceIdentity，不持 binding；cleanup 内嵌 5×三态 slot） | New（改字段） | G1/G2 批准 |
| ~ | Cloud | `TaskMaintenanceService`（registration/cleanup 只经 `MaintenanceLifecycleGate`；UNKNOWN→typed unwind；click 无重投承诺） | New（dormant） | W-TMS-1 门（含 M Full R0 / SummonSkill 前置 / 父级授权，同 F5 行9） |

其余（assembly mint/wire、M Full R0 occurrence/reclaim、B retained-resolution 可选前置、SummonSkill 前置、W-TMS-2 caller cohort）仍以固定日志/卡号引用，非 A exact 写集（同 F5，不重列）。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。Design Repair #5 Delta 到此停止，等待父级复审；heartbeat 保持。

## Parent Worker Claim Protocol - 2026-07-13T01:37:59-04:00

从下一条父级任务或返修单开始，Worker A 必须在任务发布后 20 分钟内先向本固定日志追加
`## External Worker A - CLAIMED - <timestamp>`，写明领取的父级任务标题和唯一写集。`CLAIMED` 只证明任务已经被领取，
不表示完成或批准；领取后允许实际设计/实现超过 20 分钟，并继续按既有 heartbeat 读取本日志。仅当 20 分钟内没有
`CLAIMED` 回执时，父级才判定外部任务可能未唤醒/卡死，并把该切片交给内部 Worker 接管。不得用“20 分钟没有交付成果”
作为接管条件。

## Parent Design Review #6 - MAIN BLOCKED / W-TMS-0B IMPLEMENTATION APPROVED - 2026-07-13T01:43:56-04:00

Repair #5 已关闭 caller 伪造 raw binding 与“UNKNOWN 自动重投 click”的上一轮问题；`MaintenanceLifecycleGate` 内部读取
coordinator 当前 binding 的方向成立。整类主体仍为 **BLOCKED，P0=0 / P1=2 / P2=0**：

1. **P1：cleanup capability 还没有稳定 retained owner。** 当前只描述 capability 实例内 5 个 slot 状态，却没有说明
   assembly 对同一 `(scope,taskRunId,runRevision,stopEpoch,status)` 如何只铸一次并在重入时返回同一实例。若每次 terminal/
   PAUSED cleanup 调用重新铸 capability，slot 又从 `UNCLAIMED` 开始，所有 one-shot 保证失效。主体返修必须给出
   assembly-owned、same-transition same-instance 的 admission/lookup/terminal retirement 与 owner/global 有界容量；caller 不得
   自存可重建事实来重新 mint。
2. **P1：`IN_PROGRESS` 的未决 cleanup 没有真实 resolution 合同。** 对天然幂等 clear 与非幂等 session/leader/phase mutation
   不能共用“异常后一律永久停在 IN_PROGRESS”。前者须明确 same-key exact retry 如何收敛；后者须引用真实 retained
   mutation witness/result owner，证明部分执行后如何读回终态。当前没有该 API/FQCN，故 capability/gate/整类 TMS 暂不编码。

为避免继续空转，父级单独放行 **W-TMS-0B**，Worker A 现在可以直接编码，写集严格为 **Cloud 2 New / 0 Modify**：

1. `src/main/java/com/bot/dhxy/model/maintenance/MaintenanceProbeResult.java`：immutable typed result，且只有
   `OBSERVED_MATCH(point)` / `OBSERVED_MISS` / `STOPPED` 三态；MATCH 必须有 defensively-copied `java.awt.Point`，另两态
   point 必须为空；不包含 UNKNOWN，不含 retry/park/yield。
2. `src/main/java/com/bot/dhxy/service/maintenance/MaintenanceUnresolvedException.java`：unchecked typed unwind；仅承载
   canonical operation/reason/message/cause 诊断，不暴露 raw request/poll/outcome，不自行重试或改 task state。

`TaskMaintenanceService`、capability、gate、assembly、host、caller、SummonSkill、soft-wake 与 tests 全部冻结。Worker A 必须在
本任务发布时间后 20 分钟内先追加 `## External Worker A - CLAIMED - <timestamp>`（任务名 `W-TMS-0B`、写集 2 New），随后
实施并追加 `External Worker A - W-TMS-0B Implementation #1`。因共享 `target/` 可能有并发写入，本 Worker 不运行 Maven，
由父级在 A/B 写入稳定后统一执行 fresh Cloud `mvn -q clean package`。**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T01:45:57-04:00

领取父级任务 **W-TMS-0B**（Parent Design Review #6 单独放行切片）。唯一写集 = **Cloud 2 New / 0 Modify**：
- New `dhxy-cloud-brain/src/main/java/com/bot/dhxy/model/maintenance/MaintenanceProbeResult.java`
- New `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/maintenance/MaintenanceUnresolvedException.java`

CLAIMED 仅证领取，不表完成/批准。随后追加 `W-TMS-0B Implementation #1`。MAIN 整类（capability/gate/TMS）P1×2 仍 BLOCKED、继续冻结，不在本写集内。不运行 Maven（共享 target/ 并发写入，由父级统一 package）。heartbeat 保持。

## External Worker A - W-TMS-0B Implementation #1 - 2026-07-13T01:47:42-04:00

W-TMS-0B 实施完成，写集严格 **Cloud 2 New / 0 Modify**，无其它文件改动、无 Modify、无 tests、未碰 capability/gate/TMS/assembly/host/caller。

**1. New `com/bot/dhxy/model/maintenance/MaintenanceProbeResult.java`**
- immutable final class；`enum Kind { OBSERVED_MATCH, OBSERVED_MISS, STOPPED }` 恰三态，**无 UNKNOWN**。
- 工厂：`observedMatch(Point)`（`Objects.requireNonNull` + `new Point(located)` **入口防御拷贝**）、`observedMiss()`/`stopped()`（共享单例、point 为 null）。
- `point()` 返回 `Optional<Point>`：MATCH 出口再 `new Point(point)` **防御拷贝**，MISS/STOPPED 返回 `Optional.empty()`（另两态 point 必空，符合要求）。
- 无任何 retry/park/yield 字段或方法；提供 `kind()`/`isObservedMatch()`/equals/hashCode/toString（值语义）。

**2. New `com/bot/dhxy/service/maintenance/MaintenanceUnresolvedException.java`**
- `extends RuntimeException`（unchecked typed unwind）。
- 仅承载诊断：canonical `operation`、canonical `reason`、`message`、可选 `cause`（两构造器：含/不含 cause）。
- `requireCanonical` 强制 operation/reason 非空且无首尾空白；**不暴露 raw request/poll/outcome**，**不自行重试、不改 task state**。

**构建证据**：按父级指示**不运行 Maven**（共享 `target/` 可能并发写入，由父级在 A/B 写入稳定后统一 fresh `mvn -q clean package`）。本 Worker 仅做**隔离 javac 语法校验**（两文件仅依赖 JDK `java.awt.Point`/`Objects`/`Optional`，无项目依赖，编到 scratch 目录，不触碰共享 target/）：
```
javac -d <scratch> MaintenanceProbeResult.java MaintenanceUnresolvedException.java
JAVAC_OK → MaintenanceProbeResult.class / MaintenanceProbeResult$Kind.class / MaintenanceUnresolvedException.class
```
即语法/类型自洽通过；权威构建仍由父级统一 Maven package 判定。

W-TMS-0B 到此停止等待父级复审。MAIN 整类 P1×2（cleanup capability same-transition same-instance retained owner；IN_PROGRESS 真实 resolution witness/result owner API 缺失）仍 BLOCKED、继续冻结，待父级提供/授权后再出 Design Repair #6 Delta。heartbeat 保持。

## Parent Implementation Review #1 - W-TMS-0B APPROVED - 2026-07-13T01:50:24-04:00

父级已逐文件复核最新源码，结论为 **APPROVED，P0=0 / P1=0 / P2=0**（仅针对 W-TMS-0B）：

- `MaintenanceProbeResult` 恰有 `OBSERVED_MATCH / OBSERVED_MISS / STOPPED` 三态；MATCH 对 `Point` 入口、出口均做防御拷贝，另两态无 point；无 UNKNOWN/retry/park/yield。
- `MaintenanceUnresolvedException` 为 unchecked typed unwind，仅携带 canonical operation/reason、message、cause；不暴露 raw request/poll/outcome，不含重试或 task-state mutation。
- 写集严格为 Cloud 2 New / 0 Modify，未触碰冻结的 TMS/capability/gate/assembly/host/caller/tests。

父级 fresh Cloud package 将在当前并行 Java 写入稳定后统一执行；这不改变本次源码审查结论。MAIN 整类原 P1=2 仍开放，W-TMS-0B 的批准不得被解读为主体放行。**无已批准业务差异；按基线等价迁移。**

## Parent Task Brief - W-TMS-D6 Design Repair #6 - 2026-07-13T01:58:46-04:00

Worker A 的下一任务是继续闭合 `Parent Design Review #6` 的主体 P1=2。发布后 20 分钟内先追加
`## External Worker A - CLAIMED - <timestamp>`（任务名 `W-TMS-D6`、写集仅本日志），领取截止
`2026-07-13T02:18:46-04:00`；领取后允许设计超过 20 分钟。

本轮仍为 **设计/源码核对，Java 零修改**。必须完整读取 M 固定日志末尾已批准的
`CloudTaskRunRetainedLifecycleActivationAdapter` 合同及其实际 Implementation（落盘后再引用），并只使用两仓真实存在的
FQCN/API：

1. 给出 cleanup capability 的 assembly-owned stable tuple、same-transition same-instance admission/lookup、owner/global quota、
   resume/terminal retirement 与 exact retry；若实际 adapter API 仍不足，写出精确缺失方法/参数/返回值和最小下一代码叶子，
   不得虚构已经存在。
2. 把 5 个 cleanup slot 分为天然幂等与非幂等 mutation，逐项给出 `UNCLAIMED -> IN_PROGRESS -> COMPLETED` 的真实
   resolution witness/result owner、异常点与 exact retry 条件。不能把 UNKNOWN 自动重投，也不能让进程内异常一律永久卡死。
3. 输出可编译依赖 DAG、精确 New/Modify 文件表和下一最小可编码波次；W-TMS-0A/0B、config/templates 已批准内容不重开。

完成后只向本日志追加 `External Worker A - W-TMS-D6 Design Repair #6 Delta`；等待父级 review，批准前不改 Java、不运行 Maven、
不启动运行面、不做 git mutation。**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T02:02:29-04:00

领取父级任务 **W-TMS-D6**（Parent Design Review #6 主体 P1×2 闭合，Design Repair #6）。写集 = **仅本固定日志**（设计/源码核对，**Java 零修改**、0 New/0 Modify 代码）。
领取截止 2026-07-13T02:18:46-04:00，本回执在截止前。CLAIMED 仅证领取不表完成。
计划：完整读取 M 固定日志末尾已批准的 `CloudTaskRunRetainedLifecycleActivationAdapter` 合同 + 落盘 Implementation，只用两仓真实 FQCN/API；随后追加 `External Worker A - W-TMS-D6 Design Repair #6 Delta`。批准前不改 Java、不跑 Maven、不启动运行面、不 git。heartbeat 保持。

## External Worker A - W-TMS-D6 Design Repair #6 Delta - 2026-07-13

关闭 `Parent Design Review #6` 主体 P1×2 + 输出 DAG/文件表/下一波次。仅设计，Java 零修改。W-TMS-0A/0B、config/templates、已关闭矩阵不重开。

**取证边界（据实声明）**：M 固定日志 `2026-07-12-cloud-observation-ledger-compaction-worker-m.md` @1328-1357 已批准 `CloudTaskRunRetainedLifecycleActivationAdapter` 的**设计/实现合同**，但 @1359 Internal Worker P 刚 CLAIMED 去实施，**该类尚未落盘**（两仓 `find` 均无 `.java`）。故本 Delta 引用其**已批准合同**，凡涉及尚未落盘的实现或合同未含的方法，一律标为**硬前置/精确缺失 leaf**，不宣称已存在。

### H1（P1-1）：cleanup capability 的 same-transition same-instance retained owner = 复用已批准 adapter entry

已批准 adapter 合同（M 日志 @1339-1352）恰好提供 P1-1 缺的 owner，直接复用、不另造：

- **stable tuple**：adapter 对每个 `(scope, taskRunId, taskType, window, nonTerminalStopEpoch)` **仅一个 entry**（@1340），保存 exact K slot、latest generation handle、initial immutable metadata。这与 K `StableRunKey`（`CloudTaskRetainedActionState` L251-266）同构，即 cleanup capability 的稳定 owner tuple。
- **same-transition same-instance admission/lookup**：adapter same-entry reentry 返回**同一 opaque handle、不再 activation**（@1341）。cleanup capability 挂在同一 entry 下，per terminal/paused transition 只铸一次；同一 transition 重入经 adapter lookup 返回**同一 capability 实例**，5 个 slot 状态（H2）随实例保持——**修复 Review #6 P1-1“每次重铸→slot 从 UNCLAIMED 重置→one-shot 失效”**。
- **owner/global quota**：复用 adapter admission 的 owner `1000` / global `10000`（@1342/@1250），无 TTL/LRU/eviction/takeover；process restart 不恢复、旧 handle fail-closed（@1343）。cleanup capability 不新增第二套容量。
- **resume/terminal retirement**：resume 时 adapter 原子替换 latest generation，旧 opaque handle 立即 stale（@1346）→ 依附的旧 cleanup capability 同步失效。terminal close 仅 `RELEASED/ALREADY_RELEASED` 成功后 retirement、所有旧 capability 失效（@1351-1352）；抛出/interrupt 时 entry 保留供**同 handle exact retry**（@1351）。**caller 不得自存可重建事实重新 mint**——future trusted retained-state factory 只能经 adapter 的 package-private validation（@1348-1349）。

- **精确缺失 leaf（据实，不虚构已存在）**：已批准 adapter 方法仅 `activateInitial / installInitial / resume / acquirePausedObservation / closeTerminal`（@1248/@1333-1352），**不含 maintenance cleanup capability 的 mint**。要形成可编译不可伪造调用链，需在 adapter 上新增一个与 `acquirePausedObservation` 同型的最小方法（**当前不存在**）：

  ```java
  // 建议新增于 CloudTaskRunRetainedLifecycleActivationAdapter（package-private，remote 包）——尚不存在，属硬前置
  //   入参：latest entry 的 opaque activation handle（adapter 自持，不接受 raw scope/string/revision）
  //         + 期望的 terminal/paused 语义（PAUSED|STOPPED|COMPLETED，取自 adapter 对 latest entry 经既有
  //           execution gate 读到的 exact current snapshot，同 @1347 PAUSED capability 铸造路径）
  //   返回：opaque MaintenanceRunCleanupCapability（无 public/raw scope/string/revision/slot/generation accessor）
  MaintenanceRunCleanupCapability acquireMaintenanceCleanup(
          <opaque latest activation handle> latestHandle,
          RemoteTaskRunStatus expectedTerminalOrPaused);
  ```
  该方法 + `MaintenanceRunCleanupCapability` 均需 **P 先落盘 adapter → 父级批准该 adapter 扩展**后 A 方可编码；本 Delta 不预写、不假设已存在。Repair #5 的独立 `MaintenanceLifecycleGate` 相应**收敛为 adapter 的 package-private validation 面**（不再自持 coordinator 直读），与 @1348-1349 一致。

### H2（P1-2）：5 cleanup slot 分幂等/非幂等 + 真实 witness/result owner + 异常点 + exact retry

cleanup mutation 均为 HEAD 的**同进程内存状态转移**（无 broker/port，无 broker-UNKNOWN 语义）；witness/result owner = **TMS 自身在其 monitor 内读回的状态字段**（非虚构 API）。`UNCLAIMED -> IN_PROGRESS -> COMPLETED` 由 adapter entry 下的 capability 串行驱动，idempotency key = `(taskRunId, nonTerminalStopEpoch, runRevision, CleanupSlot)`。

| slot | HEAD mutation | 幂等性 | 三态 + 异常点 + exact retry |
|---|---|---|---|
| SUMMON_SKILL_QUEUE | `clearSummonSkillQueueForWindow` | **天然幂等**（清空队列） | claim `UNCLAIMED→IN_PROGRESS` → 同 monitor 内清空 → `COMPLETED`。witness=队列为空（读回）。pre-mutation 校验失败（provably 无副作用）→ rollback `UNCLAIMED`；same-key exact retry 收敛（再清=同结果）。 |
| POST_COMBAT_FIRST_AID | `clearPostCombatFirstAidForWindow` | **天然幂等**（清空映射） | 同上；witness=映射无该 window 项。exact retry 收敛。 |
| LOCAL_TEAM_SESSION | `completeLocalTeamSessionWindow` | 非幂等（session complete/tombstone TTL） | 设计为**单 monitor 原子转移**：要么完整应用（→`COMPLETED`，witness=session 处 completed/tombstone 终态读回），要么在改任何状态前 typed 失败（→rollback `UNCLAIMED`，exact retry）。at-most-once 由 idempotency key 保证；无跨步中间态，故**不产生 IN_PROGRESS 永久卡死**。 |
| LEADER_PAUSED | `markLocalTeamLeaderPaused` | 非幂等（leader paused 标记） | 同 LOCAL_TEAM_SESSION：原子转移，witness=leader paused 标记读回。 |
| TEAM_COMBAT_PHASE | `invalidateTeamCombatPhaseForLeader` | 非幂等（phase 失效） | 同上，witness=phase 失效态读回。 |

- **不把 UNKNOWN 自动重投**：这些 mutation 非 port 调用，无 broker-UNKNOWN；唯一“不确定”只可能是 JVM 级 fatal（如 OOM），那是**进程致命→restart fail-closed**（adapter 不恢复，@1343），不构成进程内永久 IN_PROGRESS。
- **不让异常永久卡死**：单 monitor 原子性使每次 claim 后**必**落 `COMPLETED`（成功）或 `UNCLAIMED`（pre-mutation 失败可 exact retry）二者之一；no blind retry。
- **诚实缺口**：若 TMS 整类迁移时发现某 non-idempotent cleanup 实为**跨步非原子**（无法单 monitor 原子化），则其“部分执行后读回终态”需要一个**当前不存在的跨步 mutation witness owner**——届时按精确缺失 leaf 上报，绝不虚构；本 Delta 依 HEAD 现状判定五者均可单 monitor 原子化。

### H3（P1-3）：可编译依赖 DAG + New/Modify 文件表 + 下一最小波次

**可编译依赖 DAG**（→ = 依赖/前置）：
```
M0 跨仓 DTO（RemoteSemanticAddress/FinalConsumedAck/Receipt/ReceiptAck，已 APPROVED @1312）
  → CloudTaskRunRetainedLifecycleActivationAdapter（M 设计 APPROVED @1328；Worker P 实施中，未落盘）── 硬前置①
      → adapter.acquireMaintenanceCleanup + MaintenanceRunCleanupCapability（缺失 leaf，需父级批 adapter 扩展）── 硬前置②
          → MaintenanceLifecycleGate（收敛为 adapter validation 面）+ cleanup 三态机（H1/H2）
              → TaskMaintenanceService（W-TMS-1）
                  ├ 硬前置③ M Full R0 frontier/reclaim（occurrence owner，M 日志 Full R0 仍 BLOCKED）
                  └ 硬前置④ SummonSkillService.cleanSummonSkillsOnce 前置切片
  已批准且独立可编译叶子：MaintenanceProbeResult / MaintenanceUnresolvedException（W-TMS-0B APPROVED @860）
```

**New/Modify 文件表**（仅列 A 可拥有；他仓/他 worker 以固定日志引用）：

| # | 仓库 | 精确 FQCN | New/Modify | 前置门 / 归属 |
|---|---|---|---|---|
| 1-4 | Cloud | 0A config×2 + PNG×2 | New | 已批 ✅ Worker A |
| 5-6 | Cloud | `model.maintenance.MaintenanceProbeResult` / `service.maintenance.MaintenanceUnresolvedException` | New | 已批(0B) ✅ Worker A |
| 7 | Cloud | `remote.MaintenanceRunCleanupCapability`（opaque，5 slot 三态，无 raw accessor） | New | 硬前置①②+父级批；Worker A（新文件） |
| 8 | Cloud | `remote.MaintenanceLifecycleGate`（收敛为 adapter package-private validation 面） | New | 硬前置①②+父级批；Worker A（新文件） |
| 9 | Cloud | `service.TaskMaintenanceService`（cleanup 经 capability+gate；probe 用 0B 结果；occurrence 经 M frontier） | New（dormant） | W-TMS-1 门=行7-8+硬前置③④+父级授权；Worker A |
| — | Cloud | `remote.CloudTaskRunRetainedLifecycleActivationAdapter`（+`acquireMaintenanceCleanup`）、`remote.CloudTaskRunAuthorityAssembly` | New/Modify | **非 A 写集**：M/P 切片（M 日志 @1333-1362）；A 只消费其 package-private 面 |
| — | Cloud | M Full R0 frontier/reclaim、SummonSkillService 前置、W-TMS-2 caller cohort、soft-wake | New/Modify | **非 A 写集**：各自固定日志/卡号 |

**下一最小可编码波次**：由于硬前置①（adapter 落盘）②（adapter cleanup 扩展获批）**均未满足**，**Worker A 在 MAIN 上当前无可编码叶子**——须等 (a) Worker P 落盘 adapter，(b) 父级批准 `acquireMaintenanceCleanup`+`MaintenanceRunCleanupCapability` 的 adapter 扩展。满足后，A 的**下一最小波次 = 行7-8**（cleanup capability + gate validation 面），再后才是行9（TMS，另需前置③④）。在此之前 MAIN 保持 design-only、Java 冻结。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。W-TMS-D6 Design Repair #6 Delta 到此停止，等待父级复审；heartbeat 保持。

## Parent Design Review #7 - W-TMS-D6 BLOCKED / Repair #7 Published - 2026-07-13T02:11:07-04:00

父级已对照已批准 lifecycle-adapter 合同与 DHXY HEAD `TaskMaintenanceService` 的真实 mutation 源码复审。A 已如实标出
adapter 尚未落盘且 `acquireMaintenanceCleanup(...)` 尚不存在，这一点正确；但主体仍为 **BLOCKED，P0=0 / P1=2 / P2=1**：

1. **P1：adapter entry 不是 cleanup transition owner，H1 仍从“same-entry”跳到了“same-transition same-instance”。** 已批准
   adapter 的 stable tuple 是 `(scope,taskRunId,taskType,window,nonTerminalStopEpoch)`，会跨越多个 runRevision/status；其合同只保证
   initial activation handle 的 same-entry 重入。当前建议签名只收 latest activation handle 与 expected status，没有定义由谁在 entry
   monitor 内读取 exact current binding、如何以 `(generation,runRevision,stopEpoch,status)` 唯一索引 cleanup capability、同 transition
   如何返回同一实例，以及下一 transition 如何作废旧实例。影响是 repeated PAUSED/resume/terminal 可复用错 capability 或重新铸造后
   重置五个 slot。返修必须引用 Worker P 最终落盘 API；若 API 仍无该能力，给出一个最小 adapter extension 的真实字段、锁点、
   exact transition key、lookup/mint/retire 签名与 owner/global 容量，不得把 stable entry 本身当作 transition witness。
2. **P1：H2 的“三个 non-idempotent mutation 可单 monitor 原子化”与 HEAD 源码不符。**
   `markLocalTeamLeaderPaused` 在 `TaskMaintenanceService.java:1261-1285` 先跨 `teamCombatPhaseByScope` invalidation，再逐个
   `LocalTeamSessionState` 加锁；`completeLocalTeamSessionWindow` 在 `:1954-2044` 跨 state monitor、session map、return publish、
   maintenance-broadcast monitor、first-aid monitor、summon queues 与 phase map；`invalidateTeamCombatPhaseForLeader` 在
   `:926-939` 对多个 phase entry 执行 remove。它们都不是一个 monitor 下的 all-or-nothing transition，也没有 H2 所称的
   `completed/tombstone/paused/phase` 统一 result-owner API。影响是异常发生在部分副作用后时，`UNCLAIMED` 回滚会重复事件，
   `IN_PROGRESS` 保留又无法收敛。返修须逐方法以真实代码说明：哪些操作本身可按同参数幂等收敛，哪些需要 retained
   mutation witness；若需要 witness，列出最小真实 owner/API 和 UNKNOWN/partial 的复验方式，不得再声称单 monitor 原子。
3. **P2：`adapter -> MaintenanceLifecycleGate -> mutation` 目前是两层同义授权包装。** H1 又说 gate 收敛成 adapter validation 面，
   H3 仍列两个新类型，但没有说明 gate 除转发 adapter 校验外拥有何种独立策略。按仓库 no-wrapper-nesting 规则，返修应把
   transition capability 的 mint/validate/slot 状态放进一个明确 owner；只有能列出不可合并的真实职责时才保留第二层。

**下一任务 `W-TMS-D7` 已发布。** Worker A 必须在 `2026-07-13T02:31:07-04:00` 前先追加
`## External Worker A - CLAIMED - <timestamp>`，写明领取 `W-TMS-D7`，唯一写集仍为本日志、Java 零修改。领取后允许设计超过
20 分钟。随后追加 `External Worker A - W-TMS-D7 Design Repair #7 Delta`，只关闭上述三点；必须先读取 Worker P 的最终
Implementation 材料与落盘源码，P 未交付时可以等待，但不得虚构 API。W-TMS-0A/0B 与已批准 config/templates 不重开；
assembly/adapter/TMS/host/caller/tests 继续冻结。**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T02:17:00-04:00

领取父级任务 **W-TMS-D7**（Parent Design Review #7 的 P1×2+P2×1 闭合）。写集 = **仅本固定日志**（设计/源码核对，**Java 零修改**）。领取截止 2026-07-13T02:31:07-04:00，本回执在截止前。CLAIMED 仅证领取不表完成。
计划：读取 Worker P 落盘的 `CloudTaskRunRetainedLifecycleActivationAdapter` 真实 API（P 最终 Implementation 未交付前，凡引用标为 P 在途/未批、不虚构）+ DHXY HEAD `TaskMaintenanceService` :926-939 / :1261-1285 / :1954-2044 三方法真实 mutation，据实逐条关闭 P1-1（adapter transition-owner extension 真实字段/锁点/transition key/签名）、P1-2（逐方法幂等 vs witness）、P2（授权层并入单 owner）。随后追加 `External Worker A - W-TMS-D7 Design Repair #7 Delta`。批准前不改 Java、不跑 Maven、不启动运行面、不 git。heartbeat 保持。

## External Worker A - W-TMS-D7 Design Repair #7 Delta - 2026-07-13

关闭 `Parent Design Review #7` 的 P1×2+P2×1。仅设计，Java 零修改。W-TMS-0A/0B、config/templates、已关闭矩阵不重开。

**取证边界**：已读 Worker P 落盘 `CloudTaskRunRetainedLifecycleActivationAdapter.java`（1-479 行，**P 在途/未获父级 Implementation 批准**，M 日志仍仅 @1359 CLAIMED）与 DHXY HEAD `TaskMaintenanceService.java` 真实方法（:926-939 / :1261-1285 / :1954-2044）。凡引用 adapter 未含能力，均标为**精确缺失 leaf**，不虚构已存在；凡 adapter 引用以“P 最终落盘/父级批准”为硬前置。

### J1（P1-1）：cleanup capability 是 transition-scoped owner，非 entry-scoped（照搬 adapter 真实 PAUSED 模式）

父级正确：stable tuple `(scope,taskRunId,taskType,window,nonTerminalStopEpoch)` 跨多 runRevision/status，only initial handle same-entry 重入（adapter :48-79）。**撤回** D6-H1“把 stable entry 当 transition witness”。改照 adapter 真实的 **transition-scoped** 模式——PAUSED capability 并非挂 entry，而是挂 `RetainedActivationEntry.latestPausedCapability` **单槽** + 绑 `(activationHandle, exactPausedContext[含 pausedRunRevision])`，resume 时置 null（:156），`acquirePausedObservation` same-snapshot 命中返回同实例（:178-183），`requireCurrentPausedObservationContext` 复验 generation+当前 snapshot（:201-226）。cleanup 照此：

**精确缺失 leaf（adapter 当前无此能力，需 P 最终落盘 + 父级批 adapter extension）——最小扩展：**
- **新字段**（挂 `RetainedActivationEntry`，与 `latestPausedCapability`:468 同构）：`MaintenanceRunCleanupCapability latestCleanupCapability;`（单槽）。
- **锁点**：`synchronized(entry)`（与 :171/:210/:247 同一 entry monitor）；`resume` 内（:151-156 旁）追加 `entry.latestCleanupCapability = null`（generation 前进即作废）；`closeTerminal` 退休块（:262-278）追加同样置 null。
- **exact transition key**：`(handleGeneration, runRevision, stopEpoch, status)`——**取自 coordinator 当前 binding，非 caller、非 entry 自身**。PAUSED 校验照 `requirePausedSnapshotMatches`（:317-330，`pausedRunRevision==latestActiveRunRevision+1`）；STOPPED/COMPLETED 照 `requireExactTerminalBinding`（:343-371）。
- **lookup/mint/retire 签名**：
  ```java
  // 新增于 CloudTaskRunRetainedLifecycleActivationAdapter（package-private）——尚不存在
  MaintenanceRunCleanupCapability acquireMaintenanceCleanup(
          LifecycleActivationHandle expectedLatestHandle,
          RemoteTaskRunBinding exactTransitionBinding);   // status ∈ {PAUSED,STOPPED,COMPLETED}
      // 内部：requireOwnedHandle + synchronized(entry) + requireLatestHandle(entry,expected)
      //   → 读 coordinator.find(entry.key.scope, taskRunId) 当前 binding，按 status 走 paused/terminal 校验
      //   → 组 transition key；若 entry.latestCleanupCapability 命中同 (activationHandle, transition key) → 返回同实例（same-transition same-instance）
      //   → 否则 mint 新 capability（含 5 slot tri-state，J2），存 entry.latestCleanupCapability
  RemoteTaskRunBinding requireCurrentCleanupTransition(MaintenanceRunCleanupCapability cap);
      // seam（与 requireCurrentPausedObservationContext :201 同构）：复验 owner==this、entry.phase==RETAINED、
      //   entry.latestCleanupCapability==cap、entry.latestHandle==cap.activationHandle、当前 binding 仍等 transition key；
      //   通过才返回 exact 当前 binding 供 mutation 使用；stale → typed reject
  ```
  嵌套 `static final class MaintenanceRunCleanupCapability`（与 `PausedObservationCapability`:401 同构）：private 持 owner/entry/activationHandle/transitionKey + 5 slot tri-state；**无 raw scope/text/revision/slot/generation accessor**。
- **owner/global 容量**：**不新增计数**——cleanup capability 每 entry 至多一枚活（`latestCleanupCapability` 单槽），随 entry 生死，已受 adapter 既有 owner1000/global10000（:90-94）约束。
- **retire/作废**：resume 前进 generation → 旧 cleanup capability 立即 stale（置 null）；`closeTerminal` RELEASED/ALREADY_RELEASED 退休 entry → capability 失效。repeated PAUSED/resume/terminal 不会复用错 capability（每次按当前 binding 的 transition key 校验），也不会重置 5 slot（same-transition 命中返回同实例）。

### J2（P1-2）：逐方法据 HEAD 真实代码分幂等 vs 需 witness（撤回“单 monitor 原子”）

**撤回** D6-H2“三个 non-idempotent 可单 monitor 原子化”。据真实源码：

| 方法（HEAD 行） | 真实 mutation 结构 | 幂等性判定 | 三态 + 异常/retry |
|---|---|---|---|
| `invalidateTeamCombatPhaseForLeader` :926-939 | 单 `teamCombatPhaseByScope.entrySet().removeIf(by leaderWindowId)` | **天然幂等收敛**：再跑移除同 leader 全部 phase，二次无匹配即 no-op | claim→执行→COMPLETED；异常（provably 无副作用，如入口 normalize 失败）→UNCLAIMED，same-key exact retry 收敛。witness=该 leader 无 phase（读回 map）。 |
| `markLocalTeamLeaderPaused` :1261-1285 | 先 `invalidateTeamCombatPhaseForLeader`，再遍历 `localTeamSessions` 逐个 `synchronized(state)` 置 `group.leaderPaused`/`state.localLeaderPaused = paused` | **同参幂等收敛**（多 monitor 但每步=boolean 置目标值；`changed`-gated publish：重试时值已达目标→`changed=false`→**不重发事件**） | 同上；retry 收敛且不重复 soft-wake 事件。witness=相关 boolean 达 paused 目标 + leader 无 phase（读回）。 |
| `completeLocalTeamSessionWindow` :1954-2044 | `synchronized(state)` 跨 pendingReturn(+publish)、`maintenanceBroadcastQueueMonitor`(+publish)、`localTeamSessions.remove`、`markLocalTeamSessionCompleted`（跨 completedLocalTeamSessions、snapshot/claims map、summon queue、`postCombatFirstAidMonitor`、capabilities、boolean、pendingReturn clear(+publish)、queue remove(+publish)、`teamCombatPhaseByScope` removeIf） | **非收敛**：完成守卫 `completedLocalTeamSessions.put`（:2003，markLocalTeamSessionCompleted 首行）置于终态清理**起点**；若在 put 之后、多步 tail（各 removeIf/clear/publish）未完成时抛异常→入口 `isCompletedLocalTeamSession` 短路（:1955）→tail **永不补完**，UNCLAIMED 回滚又会重复已发事件 | **需真实 retained mutation witness**（HEAD 无）。见下精确缺失 leaf。 |
| `clearSummonSkillQueueForWindow` / `clearPostCombatFirstAidForWindow` | 内存队列/映射 clear（D6-H2 已列） | 天然幂等 | exact retry 收敛，witness=空 |

**completeLocalTeamSessionWindow 的精确缺失 witness leaf（不虚构已存在）**：HEAD 用 `completedLocalTeamSessions.put`（早置）作“已完成”守卫，但它标记的是**清理开始**而非**清理完成**，故无法区分“已 finalize”与“put 后半途失败”。最小真实 witness = 在 `LocalTeamSessionState`（或 completed 记录）上新增一个**与早置守卫分离的 `cleanupFinalized` 标志**，仅在 markLocalTeamSessionCompleted 全部 tail 步骤（均个体幂等：prefix removeIf、map clear、guarded publish 在空集/`queue==null` 时跳过→不重复）完成后置位；retry 时若“守卫已置 && `cleanupFinalized` 未置”→重跑幂等 tail 至收敛。该字段/owner **当前不存在**，属 TMS 迁移期需父级批准的最小新增（非本 Delta 编码，标为 W-TMS-1 前置）。**不把 UNKNOWN 自动重投**（这些非 port 调用，无 broker-UNKNOWN），**不声称单 monitor 原子**，**不让异常永久卡死**（幂等三方法经 exact retry 收敛；completeLocalTeamSessionWindow 经 `cleanupFinalized` witness 收敛）。

### J3（P2）：删除第二层，授权/slot 状态并入单一 owner（no-wrapper-nesting）

**撤回** D6 的独立 `MaintenanceLifecycleGate`。既然 adapter 的 `acquireMaintenanceCleanup`/`requireCurrentCleanupTransition` 已完成 mint+validate，且 `MaintenanceRunCleanupCapability`（adapter 嵌套）已持 5 slot tri-state 与 mutation seam，独立 gate 除转发 `requireCurrentCleanupTransition` 外**无不可合并的独立职责**——按仓库 no-wrapper-nesting 规则**合并**：
- **唯一 owner = adapter 嵌套的 `MaintenanceRunCleanupCapability`**：持 slot tri-state；其 package-private mutation 方法内部调 `adapter.requireCurrentCleanupTransition(this)` 复验后，对 exact 当前 binding 执行对应 cleanup（幂等三方法直接执行；completeLocalTeamSessionWindow 经 `cleanupFinalized` witness）。
- `TaskMaintenanceService` 直接持 capability 引用调其 mutation 方法，**无中间 gate 类**。文件表删除 `MaintenanceLifecycleGate`。

### 修订文件表（仅 A 可拥有；他仓/worker 以固定日志引用）

| # | 仓库 | 精确 FQCN | New/Modify | 前置门 / 归属 |
|---|---|---|---|---|
| 1-6 | Cloud | 0A config×2+PNG×2、0B MaintenanceProbeResult+MaintenanceUnresolvedException | New | 已批 ✅ Worker A |
| 7 | Cloud | `remote.CloudTaskRunRetainedLifecycleActivationAdapter`（+`acquireMaintenanceCleanup`/`requireCurrentCleanupTransition`/`latestCleanupCapability`/嵌套 `MaintenanceRunCleanupCapability`） | Modify | **P 最终落盘+父级批 adapter extension**（硬前置）；A 只能在放行后编码其扩展 |
| 8 | Cloud | `service.TaskMaintenanceService`（cleanup 直接经 capability mutation；`cleanupFinalized` witness 字段；probe 用 0B；occurrence 经 M frontier） | New（dormant） | W-TMS-1 门=行7+M Full R0+SummonSkill 前置+父级授权 |
| — | Cloud | `remote.CloudTaskRunAuthorityAssembly`（持/建 adapter 已由 M/P Modify） | Modify | **非 A 写集**：M/P 切片 |
| — | — | M Full R0 frontier/reclaim、SummonSkillService 前置、W-TMS-2 caller cohort、soft-wake | — | **非 A 写集**：各自固定日志 |

**已删除**：独立 `MaintenanceLifecycleGate`（J3 合并）、独立 `MaintenanceRunCleanupCapability` 顶层类（改为 adapter 嵌套）。

**下一最小可编码波次**：仍受硬前置阻塞——(a) Worker P 交付 adapter 最终 Implementation 并获父级批准，(b) 父级批准行7 的 adapter extension（`acquireMaintenanceCleanup` 等）。满足后 A 编码行7 扩展；行8 另需 M Full R0 + SummonSkill 前置 + `cleanupFinalized` witness 批准。此前 MAIN 保持 design-only、Java 冻结。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。W-TMS-D7 Design Repair #7 Delta 到此停止，等待父级复审；heartbeat 保持。

## Parent W-TMS-0B Final Build Review - APPROVED - 2026-07-13T02:23:51-04:00

父级 fresh Cloud `mvn -q clean package` exit 0；Surefire `4 suites / 21 tests / 0 failures / 0 errors / 0 skipped`，shaded
JAR 实际含 `MaintenanceProbeResult` 与 `MaintenanceUnresolvedException`。结合上一轮 source review，W-TMS-0B 最终结论为
**IMPLEMENTATION APPROVED，P0/P1/P2=0**。这不解冻 TMS 主体。**无已批准业务差异；按基线等价迁移。**

## Parent Design Review #8 - W-TMS-D7 BLOCKED / Simplified Repair #8 Published - 2026-07-13T02:23:51-04:00

D7 已正确撤回“单 monitor 原子”并识别 `completeLocalTeamSessionWindow` 的早 tombstone 行为；P lifecycle adapter 现也已父级
Implementation APPROVED。但当前方案仍 **BLOCKED，P0=0 / P1=2 / P2=0**，且父级明确收缩设计，停止继续制造 cleanup ledger：

1. **P1：跨 package 调用闭包不可编译。** proposed nested `MaintenanceRunCleanupCapability` 是
   `com.yueyunfe.dhxy.cloudbrain.remote` 的 package-private opaque type/method；目标
   `com.bot.dhxy.service.TaskMaintenanceService` 既不能声明该类型，也不能调用其 package-private mutation。若让 remote capability
   反向依赖 `com.bot.dhxy.service` 或接任意 callback，又会把业务 mutation 塞入 authority 层。返修不得再让 capability 穿过
   package 边界，也不得新增 public raw handle；Service 入口只接现有 public `TaskExecutionContext`/typed collaborator，由同 assembly
   内部调用者在进入 Service 前完成 exact lifecycle validation。
2. **P1：`cleanupFinalized` witness + exact retry 是未经批准的业务差异。** HEAD 对异常后的 partial cleanup 没有恢复 ledger，AGENTS
   明确禁止迁移顺手新增 retry/cleanup/fail-closed。迁移必须保持：每个既有 cleanup 方法按原调用点执行一次；set/clear/remove 的
   正常重复仍按 HEAD 自然幂等，异常原样上抛，不由迁移层自动重试、补偿或重放 tail。删除 cleanup capability tri-state、
   `cleanupFinalized` 与 transition idempotency key；不要“修复”基线从未承诺的 JVM 异常恢复。

**父级固定的简化方向：** lifecycle adapter 保持 P 已批准源码零修改；TMS 迁移只用现有 exact
`TaskExecutionContext.revalidate()/checkpoint` 边界与原 cleanup 方法体，调用编排留在 future trusted activation owner，且每个原
caller 事件保持一次调用。机械 capture/input 才使用 retained action identity；纯进程内 cleanup 不进入 action ledger。

**下一任务 `W-TMS-D8` 已发布。** Worker A 必须在 `2026-07-13T02:43:51-04:00` 前先追加
`## External Worker A - CLAIMED - <timestamp>`，写明领取 `W-TMS-D8`，唯一写集仍为本日志、Java 零修改；领取后允许超过
20 分钟。只需追加一个短 Delta：删除 adapter extension/capability/witness，给出原五 cleanup 方法如何由 public
`TaskExecutionContext` 入口保持一次调用、异常原样传播，以及修订后的最小文件表/DAG。不得重开已批准 0A/0B；P adapter、
assembly、host/caller/tests 继续冻结。**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T02:25:56-04:00

领取父级任务 **W-TMS-D8**（Parent Design Review #8 简化方向，闭合 P1×2）。写集 = **仅本固定日志**（设计/源码核对，**Java 零修改**）。领取截止 2026-07-13T02:43:51-04:00，本回执在截止前。CLAIMED 仅证领取不表完成。
计划：据 P 已批准 adapter 源码（零修改）+ 现有 public `TaskExecutionContext` 真实 revalidate/checkpoint API，给短 Delta：删除 adapter extension/nested capability/tri-state/cleanupFinalized/transition idempotency key；五 cleanup 方法经 public TaskExecutionContext 入口保持一次调用、异常原样传播、编排留 future trusted activation owner；机械 capture/input 才用 retained action identity，纯进程内 cleanup 不进 action ledger。随后追加 `External Worker A - W-TMS-D8 Design Repair #8 Delta`。批准前不改 Java、不跑 Maven、不启动运行面、不 git。heartbeat 保持。

## External Worker A - W-TMS-D8 Design Repair #8 Delta - 2026-07-13

按父级 Design Review #8 简化方向关闭 P1×2。仅设计，Java 零修改。删除上轮 cleanup capability 机制；P lifecycle adapter 保持零修改。0A/0B、config/templates、已关闭矩阵不重开。真实 API 已核对：Cloud `com.bot.dhxy.runner.context.TaskExecutionContext`（public final）有 `revalidate():RemoteTaskRunAuthorization`(:183)、`throwIfStopRequested()`/`isStopRequested()` 经 `checkpointDecision()`(:151/167)、`getRemoteGameClient():CloudTaskServicePort`(:195) 及 scope/taskRunId/windowId/runRevision/stopEpoch getters。

### K1（P1-1）：删除跨 package capability；Service 入口只接 public TaskExecutionContext

**撤回并删除** D6/D7 的：adapter extension（`acquireMaintenanceCleanup`/`requireCurrentCleanupTransition`/`latestCleanupCapability`）、嵌套 `MaintenanceRunCleanupCapability`、顶层 capability、`MaintenanceLifecycleGate`。理由如父级：remote 包 package-private opaque type 无法被 `com.bot.dhxy.service.TaskMaintenanceService` 声明/调用；反向依赖或 callback 会把业务 mutation 塞进 authority 层。**P adapter 保持已批准源码零修改。**

改用现有边界（与其它已迁移 Cloud Service 方法同形）：
- 五个 cleanup 方法留在 `com.bot.dhxy.service.TaskMaintenanceService`，签名只接**现有 public** `com.bot.dhxy.runner.context.TaskExecutionContext`（两仓共享的 public 类型，可跨 package 声明/调用）或其 typed getter 值，**不接 raw window/session 字符串 public mutation、不接任意 callback、不新增 public raw handle**。exact window/scope 由 `context.getWindowId()/getScope()/getLocalTeamSessionKey()` 等派生。
- **lifecycle validation 在进入 Service 前由同 assembly 内部调用者（future trusted activation owner，属 adapter/assembly cohort，冻结、非-A）完成**；Service 方法体内用既有 `context.revalidate()`（stale/paused/terminal → `allowed()==false` typed 上抛）+ `throwIfStopRequested()`/checkpoint 作标准边界。无第二层包装、无 remote→service 反依赖。

### K2（P1-2）：删除 witness/tri-state/retry；每方法原调用点执行一次，异常原样上抛

**撤回并删除** D7 的 `cleanupFinalized` witness、slot tri-state（UNCLAIMED/IN_PROGRESS/COMPLETED）、transition idempotency key、exact retry。理由如父级 + AGENTS：HEAD 对异常后 partial cleanup 无恢复 ledger，迁移禁止顺手加 retry/补偿/fail-closed。

迁移合同：
- 五个 cleanup 方法（`clearSummonSkillQueueForWindow`、`clearPostCombatFirstAidForWindow`、`completeLocalTeamSessionWindow`、`markLocalTeamLeaderPaused`、`invalidateTeamCombatPhaseForLeader`）**方法体逐字保持 HEAD**，各按原调用点**执行一次**。
- set/clear/remove 的正常重复仍按 HEAD **自然幂等**（不额外声明、不加守卫）；**异常原样上抛**，迁移层不自动重试、不补偿、不重放 tail、不“修复”基线从未承诺的 JVM 异常恢复。
- 纯进程内 cleanup **不进 action ledger**；仅机械 capture/input（probe/click）经 `context.getRemoteGameClient()`（`CloudTaskServicePort`）用 retained action identity。

### 修订最小文件表 / DAG

| # | 仓库 | 精确 FQCN | New/Modify | 前置门 / 归属 |
|---|---|---|---|---|
| 1-6 | Cloud | 0A config×2+PNG×2、0B MaintenanceProbeResult+MaintenanceUnresolvedException | New | 已批 ✅ Worker A |
| 7 | Cloud | `com.bot.dhxy.service.TaskMaintenanceService`（5 cleanup 方法接 public TaskExecutionContext、HEAD 体一次调用、异常原样上抛；probe/click 经 CloudTaskServicePort retained identity；纯 cleanup 不进 ledger） | New（dormant） | W-TMS-1 门 |
| — | Cloud | `remote.CloudTaskRunRetainedLifecycleActivationAdapter` / `CloudTaskRunAuthorityAssembly` | **零修改/非-A** | P 已批准，冻结 |
| — | — | activation owner 对 cleanup 的一次调用编排、M Full R0（机械 occurrence）、SummonSkillService 前置、W-TMS-2 caller cohort、soft-wake | — | **非-A**：各自固定日志 |

**已删除写集**：adapter extension、`MaintenanceRunCleanupCapability`、`MaintenanceLifecycleGate`、`cleanupFinalized`、tri-state、transition idempotency key。

**可编译 DAG**：0A/0B/W0（APPROVED）+ P adapter（APPROVED，零修改）→ `TaskMaintenanceService`（W-TMS-1）。W-TMS-1 门 = 纯 cleanup 部分只依赖现有 public `TaskExecutionContext`（已具备）；机械 capture/input 部分另需 M Full R0（occurrence 源）+ SummonSkillService 前置 + activation owner 一次调用编排（非-A）+ 父级授权。此前 MAIN 保持 design-only、Java 冻结。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级批准）。W-TMS-D8 Design Repair #8 Delta 到此停止，等待父级复审；heartbeat 保持。

## Parent Design Review #9 - W-TMS-D8 APPROVED / Scale Wire Implementation Handoff - 2026-07-13T02:34:00-04:00

父级已对照当前 Cloud `TaskExecutionContext`、P lifecycle adapter 与 DHXY HEAD 五个 cleanup 方法复审。D8 已完整删除
未经批准的 cleanup ledger/capability/tri-state/`cleanupFinalized`/自动重试，并恢复“原调用点一次调用、异常原样传播”的
lift-and-shift 边界。结论：**DESIGN APPROVED，P0/P1/P2=0**。

实现时有一条绑定澄清，不构成开放问题：`TaskExecutionContext.revalidate()` 返回
`RemoteTaskRunAuthorization`，本身不抛 stale 异常；未来 W-TMS-1 必须在每个 public mutation 边界调用现有
`throwIfStopRequested()`，或显式检查 `revalidate().allowed()` 后走现有 typed transition unwind，严禁忽略返回值。纯内存
cleanup 不进入 action ledger，也不新增 retry/补偿。W-TMS-1 主体仍等待 M Full R0、SummonSkill 主体与 trusted activation
caller 闭合，本轮不创建半成品 `TaskMaintenanceService`。

为避免 Worker A 空等，父级将其下一项实质实现切片切换为 **`Q-SCALE-WIRE-IMP1`**。唯一需求与写集在
`docs/superpowers/plans/reports/2026-07-13-cloud-capture-scale-wire-worker-q.md` 的最新 Parent Design Review 中；A 必须在
`2026-07-13T02:54:00-04:00` 前到该日志追加 `External Worker A - CLAIMED`，之后允许实施超过 20 分钟。A 从此刻起不再修改
本 TMS 日志，直至父级重新发布 TMS 任务；TMS Java/host/caller 继续冻结。**无已批准业务差异；按基线等价迁移。**
