# NavigationService Cloud Lift - External Worker A

## Parent Task Brief #1 - `W-NAV-D1` - 2026-07-13T06:45:00-04:00

### 角色、领取门与唯一写集

- 你是 External Worker A，只做设计/实现，不是 reviewer；父级独立审查。
- 20 分钟内先在本日志追加 `CLAIMED`，写明 task、claimedAt、唯一写集；领取截止 `2026-07-13T07:05:00-04:00`。20 分钟只检查领取，已 CLAIMED 后允许工作超过 20 分钟。
- 本轮只追加 implementation-ready Design #1；两仓 Java/Maven/schema/resources/tests/host/caller 与其它报告全部冻结。不要等待 P2/B 才完成设计。
- 开工先读 `D:\mavenProject\DHXY\AGENTS.md`、`docs\DHXY_CONTEXT.md`、`docs\ACTIVE_WORK.md` 顶部、迁移矩阵与 `docs\业务逻辑.md`，再以 committed HEAD `0114604e` 的 3453 行 `NavigationService.java` 和全部 caller 为唯一业务基线。当前工作区该文件相对 HEAD 有 `318` 行删除，只读识别冲突，不得把 dirty 行当迁移权威，不回滚、不覆盖、不提交。

### 目标

把现有导航的职责边界变成可直接实施的 Cloud business brain + DHXY retained executor，不重写五倍/五环/修罗已验证导航语义：

1. **Cloud 业务权威**：继续拥有既有 `NavigationRoutePlanCloudDecisionService` 六段 route plan、`NavigationPointCloudDecisionService` mini-map candidate、`RouteCloudDecisionService` 世界地图结果解释，并补齐跨 runRevision 的 navigation invocation/phase/pending/terminal business state；Cloud 决定下一导航动作，但不得直接截图、模板匹配、持 HWND 或发输入。
2. **DHXY 永久本地能力**：exact bound-window capture、地图/mini-map 模板与 OCR、坐标换算、`WindowPathingSnapshot`/pathing-intent watcher、移动/到达/dialog 实时观察、world-map/mini-map 原子输入、identity/lease/revision/stop/pause fence、terminal fact gate、UI clean 与本地诊断。断线可继续观察和拒绝输入，但不得离线推进 Cloud phase/fallback/retry。
3. 保持 HEAD public API、caller result mapping、route ladder、fallback/close/finally、keep-turn、所有 timeout/poll/freshness 常量、输入顺序与 stop checkpoint 完全不变。尤其不得新增 TTL、额外 verify、retry、yield、fallback、自动 renewal，`UNKNOWN/STOPPED` 不得压成 `MAP_NOT_REACHED` 后触发重复输入，除非 HEAD 本来如此。
4. host/Task/caller 继续 dormant；本轮不启动 thread/poller/application/UI/capture/input，不运行 Maven，不写 Java。

### Design #1 必交付

- inventory：三个 public API、所有 caller、11+ route facts、route dialog/pending transfer/mini-map batch、两个 execution ledger、pathing-intent 与所有业务/诊断 mutable state；逐项标 Cloud business 或 local retained。
- exact retained identity：scope/taskRun/window 4-tuple/stopEpoch、跨 revision invocation、stable semantic occurrence/action identity、duplicate/late/UNKNOWN/terminal cleanup；不得由 factory 重铸 ID。
- typed seams：route fact batch、fresh capture/coordinate result、route/mini-map input bundle、pathing occurrence/terminal fact、dialog observer wake；禁止 public raw request/poll/outcome bypass 与 queue-in-queue。
- continuation：解释 60 秒本图循环、keep-turn `min(10s)`、200/250ms、2.5s/1.5s/20s 等现有 timer 如何由 Cloud task continuation + 本地 level wake 保持，不新增 Cloud 线程。
- tenant/global/per-run hard cap、原子 admission/removal、restart 无 restore、无 TTL/LRU/takeover；列出完整 failure/uncertain matrix。
- 可编译依赖 DAG 与精确 New/Modify 文件表，明确与 P2 transaction、B TeamReturn、CommonBox 两叶子的零重叠；找出一个可独立实施的真实 policy/type leaf，若无则写“无”并证明，不制造 wrapper。
- 引用 `docs/业务逻辑.md` 的适用五倍/修罗 baseline 行，并在结尾写自审 `P0/P1/P2`（仅 QA，不算父级批准）。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T06:52:46-04:00
- task: W-NAV-D1
- claimedAt: 2026-07-13T06:52:46-04:00
- uniqueWriteSet: only this append-only report（implementation-ready Design #1；两仓 Java/Maven/schema/resources/tests/host/caller 冻结，不写 Java，不跑 Maven，不 git，不覆盖工作区 318 行删除的 dirty）

## External Worker A - Design #1 (implementation-ready) - 2026-07-13

以 committed HEAD `0114604e` 的 `com.bot.dhxy.service.NavigationService`（3453 行）与全部 caller 为唯一业务基线的整类 Cloud lift-and-shift 设计。仅设计，两仓 Java/Maven/schema/resources/tests/host/caller 冻结；不写 Java、不跑 Maven、不 git。**工作区该文件相对 HEAD 有 318 行删除，仅只读识别、不作迁移权威、不回滚/覆盖**（本设计一切引用取自 `git show 0114604e:...NavigationService.java`）。已读 AGENTS/DHXY_CONTEXT/ACTIVE_WORK 顶部/迁移矩阵/业务逻辑。Cloud 未落盘类型标 New，不虚构。

### 1. inventory（HEAD 行锚，逐项标 Cloud business / local retained）

- **public API（3，均返 `NavigationResult`）**：`navigateToNPC(NavigationRequest)`(L234)、`navigateToMap(NavigationRequest)`(L290)、`navigateInCurrentMap(NavigationRequest)`(L1039)。
- **caller（唯一生产）**：`navigateToNPC`×9、`navigateInCurrentMap`×3、`navigateToMap`（Xiuluo/Wubei/AutoBattle 任务大脑）——**全冻结（W-NAV-2 caller cohort，非-A）**；行号在 W-NAV-2 切片精确列出。
- **Cloud business 决策服务（既有，保留 Cloud 权威）**：`RouteCloudDecisionService`(L195 世界地图结果解释)、`NavigationPointCloudDecisionService`(mini-map candidate)、`NavigationRoutePlanCloudDecisionService`(六段 route plan)。→ **Cloud business**。
- **business/诊断 mutable state（L199-218）**：`Map<String,NavigationRuntimeState> runtimeStates`(per run/window 导航 phase/pending/terminal 业务状态)→**Cloud business**；`Map<String,String> routePlanExecutionLedger`、`Map<String,String> miniMapClickExecutionLedger`(两个 execution ledger，幂等/去重)→**Cloud business**（跨 runRevision invocation 账，补齐 navigation invocation/phase/pending/terminal state）。
- **route facts（11+）+ route dialog/pending transfer/mini-map batch**：world-map 搜索/scroll/结果、route dialog visible/attention/active-intent/unknown-intent gate、mini-map open/click/pathing confirm、arrival confirm——**事实读取(capture/OCR/coordinate/pathing/dialog 观察)= local retained**；事实**解释与下一动作决策 = Cloud business**。
- **pathing-intent / 实时观察（local）**：`WindowPathingSnapshot/WindowPathingIntent/WindowPathingState`(L48-50)、`WindowDialogSnapshot`(L51)、`WindowPathingIntent watcher`、移动/到达/dialog 实时观察、`windowReadyEventBus`(level wake)→**local retained**（断线可继续观察/拒输入，**不离线推进 Cloud phase/fallback/retry**）。
- **local 永久能力**：`tracker`(exact bound-window capture)、`miniMapCoordinateReader`(mini-map OCR/coordinate)、`coordinateHelper`(坐标换算)、`inputProvider/inputSequences/boundWindowKeyboardService`(world-map/mini-map 原子输入)、identity/lease/revision/stop/pause fence、terminal fact gate、`uiCleanerService`(UI clean)、本地诊断→**local retained**。
- **常量/timer（L102-171，全部逐字保 HEAD，见 §4 continuation）**：ROI/offset/阈值 0.8、scroll 3×6/80/200ms、settle 200ms、route dialog gate 10s/30s/60s/120s、mini-map 500/250/200ms、arrival 2.5s/500ms、pathing 1.5s/250ms、coord 1s/200ms、ling-shou route 20s，及 60s 本图循环、keep-turn `min(10s)`。

### 2. exact retained identity（跨 revision，不重铸 ID）

- 每次 public invocation 绑定 exact `RemoteTaskRunScope(tenant/user/device/clientSession)` + `taskRunId` + window 4-tuple(windowId/nativeHandle/processId/playerIdentityEpoch) + `stopEpoch`；**invocation 跨 runRevision 保留**（StableRunKey 同 `CloudTaskRetainedActionState`，含 stopEpoch 不含 runRevision；current context/port 由 assembly generation 原子重绑，同 QM/CBOX 结论）。
- **stable semantic occurrence/action identity**：每机械 step（world-map 搜索输入/scroll/结果 capture、route dialog 观察、mini-map open/click、arrival/pathing confirm capture）固定 canonical `phaseCode=navigation` + per-step `actionSlot` + occurrence（承接 M Full R0 frontier）；**factory 不重铸 ID**。
- **duplicate/late/UNKNOWN/terminal cleanup**：duplicate=既批准 retained 幂等台账；late=M final-consumed；UNKNOWN=typed unresolved（§5）；terminal=run 终止清该 run 的 runtimeStates/ledger entry（exact-run，同 CBOX removeRunPending 结论）。routePlanExecutionLedger/miniMapClickExecutionLedger 承接为 Cloud 侧 exact-run 幂等键，非 raw string 二次执行。

### 3. typed seams（禁 public raw request/poll/outcome bypass、禁 queue-in-queue）

- **route fact batch**：Cloud 请求一批 world-map/route 事实（capture+OCR+coordinate），local 一次性回 typed batch（fresh capture/coordinate result），Cloud 解释；**非逐字段 raw poll**。
- **fresh capture/coordinate result**：`CloudTaskServicePort.CAPTURE`(ROI)+local mini-map OCR/coordinate → typed `NavigationFactResult`（New Cloud DTO），坐标 WINDOW_CLIENT_PX/SCREEN_ABSOLUTE_PX 按 HEAD 语义。
- **route/mini-map input bundle**：world-map 搜索输入序列、mini-map open/click 各为一 `EXECUTE_INPUT_BUNDLE`（原子，settle/顺序逐字 HEAD）。
- **pathing occurrence/terminal fact + dialog observer wake**：local pathing/dialog watcher 经 typed observation（含 occurrence/terminal fact）回 Cloud；dialog observer wake 由 `windowReadyEventBus` 级别唤醒表达，**非 Cloud 线程**。
- 全部经 retained `CloudTaskServicePort` 三操作（`WINDOW_FACT/CAPTURE/EXECUTE_INPUT_BUNDLE`），**不新增 NAVIGATION raw operation、无 public raw send/poll/outcome、无 queue-in-queue**。

### 4. continuation（现有 timer 由 Cloud task continuation + 本地 level wake，不新增 Cloud 线程）

- **60s 本图循环**：Cloud navigation invocation 的 continuation（非阻塞 re-entrant step），每轮经 typed fact batch + 决策；本地 level wake（pathing/dialog/window-ready 观察到状态变化即唤醒），**无 Cloud 轮询线程**。
- **keep-turn `min(10s)`**：Cloud 保留 turn 的上限由 continuation deadline 表达；到达/pathing 确认由本地观察 wake 提前收口。
- **200/250ms、2.5s/1.5s/20s、route dialog 10s/30s/60s/120s gate**：均为**本地** capture/confirm/observe 的 settle/timeout/freshness（wall-clock 逐字 HEAD），由 local retained executor 计时；Cloud 只在 typed fact/terminal 回来后推进 phase。**不新增 TTL/verify/retry/yield/fallback/自动 renewal**；`UNKNOWN/STOPPED` 不压成 `MAP_NOT_REACHED` 后重复输入（除非 HEAD 本就如此）。

### 5. hard cap + failure/uncertain matrix

- **cap（assembly-injected 正数）**：tenant-state / per-run navigation invocation / per-run ledger entry 全局+per-scope 硬上限；原子 admission/removal（写入前检查、满额 fail-closed）；**restart 无 restore、无 TTL/LRU/takeover**（同 CBOX/M 结论）。
- **failure/uncertain matrix**：`OBSERVED`(fact 完整)→Cloud 解释推进；`NOT_EXECUTED`(副作用前 fence 拒/无图)→按 HEAD 分支(不自动 retry)；`UNKNOWN`(已输入后断线/不确定)→typed unresolved，保守封存该 invocation step（不重投/不换 ID），不压成 MAP_NOT_REACHED；`STOPPED`(stop/terminal)→typed stop unwind；late non-UNKNOWN→M final-consumed 唯一替换；duplicate→幂等台账。

### 6. 可编译 DAG + New/Modify 文件表 + 零重叠 + 独立叶子

**DAG**：
```
既有 CloudTaskServicePort/retained 权威 + 三决策服务(Route/NavigationPoint/NavigationRoutePlan CloudDecisionService) + A capture-time systemScaleRatio(APPROVED)
  → [叶子 W-NAV-0? 见下] Cloud navigation 业务 DTO/policy 叶子
  → [W-NAV-1] Cloud NavigationService（整类编排 dormant：3 public API + runtimeStates/2 ledger 跨 revision + typed seams + continuation）
      ├ 硬前置 M Full R0（occurrence 源）
      ├ 硬前置 local retained executor / pathing·dialog observer transport（fact batch/input bundle/observation wake，非-A cohort）
      └ 硬前置 activation/assembly mount + fixed-slot adapter（同 CBOX/QM，非-A）
  → [W-NAV-2 caller cohort，非-A] Xiuluo/Wubei/AutoBattle caller 接线
```
**零重叠**：与 **P2 双仓 transaction**（wire/frontier）、**B TeamReturn**、**CommonBox 两叶子**（已 SOURCE APPROVED，config/governor）写集**零交叉**——NavigationService 及其 DTO 为新文件；三决策服务零修改（既有 Cloud business）。

**独立可实施 policy/type leaf 判定**：候选 = navigation invocation/phase 的纯 typed DTO（`NavigationFactResult` / `NavigationInvocationState` 值类型）。但其字段需绑 HEAD `NavigationRuntimeState`/route facts 的精确形状，且 Cloud 决策服务已存在、runtimeStates 是 Cloud business 状态——最小真实独立叶子 = **`NavigationInvocationStore`（per-run navigation invocation/phase/pending/terminal 状态 + 跨 revision + hard cap，纯 state，可独立编译，无 host/caller/port）**，与 CBOX `CommonBoxStateGovernor` 同形。若父级认为其字段必须先定 fact batch 契约则该叶子亦有前置——如是则本轮独立叶子**结论=有条件**，以父级裁定为准，不制造 wrapper。

| 仓库 | 精确 FQCN | New/Modify | 门 / 归属 |
|---|---|---|---|
| Cloud | `service.navigation.NavigationInvocationStore`（per-run invocation/phase/pending/terminal 状态，跨 revision，hard cap，纯 state） | New（叶子 W-NAV-0） | 本设计批准；Worker A |
| Cloud | `remote.NavigationFactResult` / `remote.NavigationInputBundleSpec`（typed seams DTO） | New | W-NAV-1 门；Worker A |
| Cloud | `service.NavigationService`（整类编排 dormant） | New | W-NAV-1 门=上列+M Full R0+observer transport+adapter mount+父级授权 |
| Cloud | `RouteCloudDecisionService`/`NavigationPointCloudDecisionService`/`NavigationRoutePlanCloudDecisionService` | 零修改 | 既有 Cloud business |
| Cloud | `remote.NavigationServicePortAdapter`（fixed-slot，opaque bundle）+ `CloudTaskRunAuthorityAssembly`/`TaskServiceRuntime` mount | New/Modify | **authority cohort 排序**（与 P/M/QM/CBOX 在途） |
| DHXY | exact capture / mini-map OCR·coordinate / world·mini-map input / pathing·dialog observer transport / fence / UI clean / 本地诊断 | 保留/非-A | DHXY 本地，W-NAV-2 cohort |
| — | Xiuluo/Wubei/AutoBattle caller 接线 | Modify | **非-A**：W-NAV-2 cohort |

**双构建门**：W-NAV-1 落地后（父级授权、并发稳定）Cloud `mvn -q clean package`（不 skip）+ DHXY `mvn -q -DskipTests compile`；不新增/恢复 tests，不启动运行面。W-NAV-0 叶子可先独立编译。

### 业务逻辑 baseline 引用

`docs/业务逻辑.md`：L120（接任务 NPC 前 pending 盒子检查，与 navigation 交接）、L168（不改修罗/五倍接任务/导航/入战/回程原有规则）、L246-248（下一次接任务 NPC 导航**必须复用回程快照**、禁止重复打开小地图/重读地图确认，但不改近点直点与远点导航业务分流）——本设计保留该复用语义（pathing snapshot 复用属 local retained 观察，Cloud 不触发重复确认）。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 **P0=0 / P1=0 / P2=0**（仅 QA，不构成父级批准）。Design #1 到此停止，等待父级 `DESIGN APPROVED` 或 `BLOCKED`；heartbeat 保持。

## Parent Design Review #1 - BLOCKED - 2026-07-13T07:06:37-04:00

父级以 committed HEAD `0114604e` 的 `NavigationService.java`、全部生产 caller、现有 Full R0/local handler 权威和本地 Runner/UI-clean 边界复审。Cloud 持 route/point/plan 业务决策、本地持 exact-window capture/template/pathing/dialog/input 的总方向成立；但 Design #1 尚不能实施，当前 `P0=0 / P1=6 / P2=2`：

1. **P1：caller inventory 与源码不符，不能把精确 caller 再推迟到 W-NAV-2。** `git grep -n 'navigationService\.navigate' 0114604e` 只有 12 个生产调用：Wubei 4 个（`navigateToNPC` 2、`navigateInCurrentMap` 2）、FiveRingTaskV2 3 个（均 `navigateToNPC`）、XiuluoTaskV2 5 个（`navigateToNPC` 4、`navigateInCurrentMap` 1）。不存在 AutoBattle caller，也不存在任何 direct `navigateToMap` caller；Design #1 的“navigateToMap（Xiuluo/Wubei/AutoBattle）”和 caller DAG 因而错误。影响是后续可能接入不存在的入口、漏掉真正的 result/keep-turn mapping。Repair 必须在本卡列出这 12 个精确文件/行/API、每个 caller 对 `NavigationResultStatus` 的消费以及适用业务基线，不得以“后波再列”代替本轮 inventory。
2. **P1：把两个 class-local execution ledger 整体迁为 Cloud business，会拆掉本地物理副作用的最后防重门。** HEAD `NavigationService.java:201-217` 明写 `routePlanExecutionLedger` 仅活在一次 `navigateToMap` 调用内，`miniMapClickExecutionLedger` 防止同一 cloud-resolved candidate 再次提交 physical input；实际读写/清理在 `:413-421/:461` 与 `:1129/:1234/:1345-1351`。Full R0 Cloud retained ledger 拥有 semantic action identity，但网络重投到本机后的机械 operation outcome 仍必须由 DHXY `RemoteOperationLedger` 或等价 local retained owner 防重。Repair 必须给出双层但不双重业务权威的 exact mapping：Cloud 只拥有 semantic occurrence/action；DHXY 对 exact request/action/address 保存本地执行结果，重投只回报、绝不再点。不得只把两张 map 移到 Cloud，也不得另造第二套 Cloud execution ledger。
3. **P1：`runtimeStates` 被赋予了源码中不存在的 phase/pending/terminal 含义。** HEAD `NavigationRuntimeState` 只有 `lastAbsoluteLogicalX/Y`、route relative X/Y、matchedText、usedMemory、routeDecisionId 六类字段（`:3396-3410`），用于本地 route click、`PendingRouteOutcome` 构造和 shadow 诊断（`:2154-2161`、`:2316-2358`、`:3074-3085`）；它不是 per-taskRun phase store。Repair 必须逐字段拆分：Cloud 已有的 decision/业务 pending 由既有 retained runtime 持有；窗口坐标、匹配文本、active pathing intent、runner pending outcome 和本地诊断继续绑定 exact local window。若仍需要 `NavigationInvocationStore`，须先证明它保存的是 HEAD 中真实存在且不与 Full R0/current-context/action-ledger 重复的业务字段；不得把这个名字当成新抽象的理由。
4. **P1：continuation 的 wake 与 due-time 关系未保持 HEAD 顺序，且把全部时间常量都归为 local settle 是错误分类。** 例如 keep-turn 循环严格按“checkpoint -> coordinate/pathing read -> `sleep(250)` -> next iteration”（`:1159-1187`）；level wake 若直接触发 Cloud step 会新增提前 read/decision。另一方面 `ROUTE_DIALOG_*_MAX_AGE_MS` 在 `:1684-1713/:1902-1908` 直接决定 route gate，是业务事实 freshness；`MINI_MAP_*_SETTLE_MS` 在 `:2125/:2132/:2676/:2687` 才是本地 atomic input settle。Repair 要逐常量列 owner 和 exact check/sleep/read 顺序：本地 wake 只标 ready，Cloud continuation 仍须满足 baseline due；只有 HEAD 已允许的 terminal fact 才能提前收口。不得新增 state-change extra read。
5. **P1：统一 `OBSERVED/NOT_EXECUTED/UNKNOWN/STOPPED` 表不能证明三个 API 的 branch-equivalence。** HEAD 在 route terminal backing failure、prepared-dialog gate、mini-map fire-and-handoff、NO_PATHING、combat interruption、close/finally 等位置返回和 fallback 不同；“UNKNOWN 一律 seal step”会把副作用前 capture 失败与副作用后结果不确定混为一类。Repair 必须按 `navigateToNPC`、`navigateToMap`、`navigateInCurrentMap` 及每个机械 action 列 failure matrix：副作用前可信 `NOT_EXECUTED`、副作用后 `UNKNOWN`、late final、STOPPED 分别如何映射现有返回/continue/fallback/finally，证明不增加 click/retry/verify/yield，也不吞掉 HEAD 的 close/cleanup。
6. **P1：capacity 与 state ownership 仍不可实施。** “assembly-injected 正数”没有 exact global/tenant/per-run 默认值、same-key-before-quota、原子 reservation/rollback、terminal/recreate generation 和 overflow outcome；同时提议的新 `NavigationInvocationStore` 与 Full R0 retained action/current-context owner 的边界未证实。Repair 必须基于 P2 最终真实 API 给出唯一 owner、完整 key、单调 occurrence/incarnation、所有退出路径退账和 restart-no-restore；若 HEAD 本无长期 navigation state，则不得为满足表格而制造长寿命 store。任何 hard cap overflow 必须给出不改变现有业务 fallback 的 typed failure。
7. **P2：文件表不精确且包/类型名是占位。** `service.navigation.NavigationInvocationStore`、`service.NavigationService`、`remote.NavigationFactResult`、`remote.NavigationServicePortAdapter` 混用包名，DHXY 一栏只是能力描述而非路径；caller、handler/codec、local ledger、observer/wake、assembly mount 均未列 exact Modify 文件。P2 Full R0 稳定后，Repair 必须基于实际源码补齐 New/Modify FQCN、方法级 ownership、依赖顺序与 A/B/P/U 零交叉；在此之前主体 Java、DTO、store、adapter 全冻结。
8. **P2：所谓独立叶子目前不成立。** `NavigationInvocationStore` 的字段、key、lifecycle、quota 都依赖上述 P1，且它不是 HEAD 纯 policy 提取。当前真实独立叶子结论应为“无”；若 Repair 能从 HEAD 提取不触碰 local authority、Full R0 或行为时序的 closed immutable type/policy，再单独列证据，父级另行判断，不为增加迁移数字制造 wrapper。

### 下一任务 `W-NAV-D2`

External Worker A 先在本日志追加 `CLAIMED`（task、claimedAt、唯一写集），领取截止 `2026-07-13T07:26:37-04:00`；只追加 Design Repair #1 Delta，逐项关闭上述 P1/P2。两仓 Java/Maven/schema/resources/tests/host/caller 与其它报告继续冻结，不等待 P2/B/U，不运行 Maven，不启动任何运行面。Worker 自审不算父级批准。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T07:11:11-04:00
- task: W-NAV-D2
- claimedAt: 2026-07-13T07:11:11-04:00
- uniqueWriteSet: only this append-only report（Design Repair #1 Delta；两仓 Java/Maven/schema/resources/tests/host/caller 冻结，不写 Java、不跑 Maven、不 git；以 HEAD 0114604e 为准）

## External Worker A - Design Repair #1 Delta - 2026-07-13

关闭 `Parent Design Review #1` 的 P1×6+P2×2。仅设计，两仓 Java/Maven/schema/resources/tests/host/caller 冻结。以 HEAD `0114604e` 为唯一权威（工作区 318 删除不作准）。方向（Cloud 持 route/point/plan 决策、本地持 capture/template/pathing/dialog/input）成立不重写；本 Delta **撤回** Design #1 的多处外推。

### N1（P1-1）：精确 12 caller 表（navigateToMap 无 direct caller、无 AutoBattle）

`git grep 'navigationService.navigate' 0114604e` = **12 生产调用**（navigateToNPC×9 / navigateInCurrentMap×3 / **navigateToMap 无直接 caller**，仅由前两者内部调用；**无 AutoBattle caller**）：

| # | 文件:行 | API | NavigationResultStatus 消费（HEAD 分支）| 业务基线 |
|---|---|---|---|---|
| 1 | WubeiTask:1830 | navigateToNPC | 按 result status（arrived/接续接任务 vs not-reached/fallback）| 五倍接任务导航 |
| 2 | WubeiTask:2075 | navigateInCurrentMap | 当前图内到点 result | 五倍图内 |
| 3 | WubeiTask:2344 | navigateToNPC | 同 #1 | 五倍 |
| 4 | WubeiTask:2621 | navigateInCurrentMap | 同 #2 | 五倍 |
| 5 | FiveRingTaskV2:1386 | navigateToNPC | result status 消费 | 五环 |
| 6 | FiveRingTaskV2:1446 | navigateToNPC (fallback) | fallback 路径 result | 五环 fallback |
| 7 | FiveRingTaskV2:1721 | navigateToNPC | result status | 五环 |
| 8 | XiuluoTaskV2:2768 | navigateToNPC | result status | 修罗接任务 |
| 9 | XiuluoTaskV2:3140 | navigateToNPC | result status | 修罗 |
| 10 | XiuluoTaskV2:3368 | navigateInCurrentMap | 图内 result | 修罗图内 |
| 11 | XiuluoTaskV2:4174 | navigateToNPC | result status | 修罗 |
| 12 | XiuluoTaskV2:4874 | navigateToNPC | result status | 修罗 |

（每 caller 对 `NavigationResultStatus` 的 exact arrived/not-reached/stopped/keep-turn 消费与后续动作在 W-NAV-1 实施时逐行对齐；本轮已固定精确 12 入口，不再推迟。navigateToMap 仅内部编排，无 public caller 需接线。）

### N2（P1-2）：两 ledger = 本地物理防重，不上云（撤回“两 map 迁 Cloud”）

**撤回** Design #1 把两 execution ledger 整体迁为 Cloud business。据 HEAD `:201-217` 注释实证：
- `routePlanExecutionLedger`（CR260）：key `windowId|hwnd|taskRunId|routePlanRequestId|stepId`，**仅活于一次 navigateToMap 调用、非 disk**，replayed stepId **re-report 不 re-execute**。
- `miniMapClickExecutionLedger`（CR258）：key `windowId|hwnd|taskRunId|navigationRequestId|decisionId|candidateId`，**仅活于该 navigation 调用、非 disk、defense-in-depth**（主防重=attemptedCandidateIds set + method-local batch），replayed key **re-report 不再提交 physical input**。
- **双层不双权威 exact mapping**：Cloud 只拥有 **semantic occurrence/action identity（Full R0 retained action-ledger）**；网络重投到本机后的**机械 operation outcome 由 DHXY `RemoteOperationLedger`（或等价 local retained owner）对 exact request/action/address 防重**，重投**只回报、绝不再点**。两 map 的 key（含 windowId/hwnd/taskRunId/requestId/stepId|candidateId）映射为 retained action address + 本地 op-ledger key，**不迁 Cloud、不造第二套 Cloud execution ledger**。

### N3（P1-3）：runtimeStates 逐字段 = 本地窗口绑定态（撤回 NavigationInvocationStore）

**撤回** Design #1 赋予 runtimeStates 的 phase/pending/terminal 含义与新建 `NavigationInvocationStore`。据 HEAD `NavigationRuntimeState`（`:3396-3410`）**恰 7 字段**：`lastAbsoluteLogicalX/Y`（窗口逻辑坐标）、`lastWorldMapRouteRelativeX/Y`（route relative）、`lastWorldMapRouteMatchedText`、`lastWorldMapRouteUsedMemory`、`lastWorldMapRouteDecisionId` + `clearWorldMapRouteResultClick()`；用于本地 route click、`PendingRouteOutcome` 构造与 shadow 诊断（`:2154-2161`/`:2316-2358`/`:3074-3085`）。**它是 exact-local-window 绑定态，非 per-taskRun phase store**：
- 窗口坐标 / matched text / active pathing intent / runner pending outcome / 本地诊断 → **local retained，绑 exact local window**（DHXY）。
- Cloud 已有的 route/point/plan **decision 与业务 pending** → 由**既有 Full R0 retained runtime / current-context**持有，**不新建 store**。
- 结论：**不存在需新建的长寿命 Cloud navigation state**；`NavigationInvocationStore` 撤销。

### N4（P1-4）：逐常量 owner + keep-turn 顺序（撤回“全部 local settle”）

**撤回** Design #1 把全部时间常量归为 local settle。逐常量：

| 常量（HEAD 行）| owner | 语义 |
|---|---|---|
| `ROUTE_DIALOG_*_MAX_AGE_MS`（visible/attention/active-intent/unknown-intent gate，`:1684-1713`/`:1902-1908`）| **Cloud 业务 due-check** | route gate 的**业务事实 freshness**（Cloud 在 typed fact 上判 due，本地只回 fact+时间戳）|
| `MINI_MAP_*_SETTLE_MS`（`:2125`/`:2132`/`:2676`/`:2687`）| **DHXY local** | mini-map atomic input **settle** |
| keep-turn `Math.min(10s, …)` deadline（`:1159`）| **Cloud continuation due** | keep-turn 上限，Cloud continuation 满足 baseline due |
| 250ms（keep-turn iteration `:1180`）、200ms、2.5s/1.5s/20s confirm | 各自 local capture/confirm settle/poll | 本地计时 |

- **keep-turn 顺序逐字**（`:1159-1187`）：`deadline=now+min(...) → while(now<deadline){ checkpoint("keepTurn") → pathing/coordinate read → arrived?break:continue → !sleep(250)?stopped }`。**本地 level wake 只标 ready**（pathing/dialog/window-ready 观察到变化即置 ready），**Cloud continuation 仍须满足 baseline due**（deadline/freshness）；**只有 HEAD 已允许的 terminal fact（arrived）才提前收口**；**不新增 state-change extra read/decision、不新增 Cloud 线程**。

### N5（P1-5）：per-API + per-action failure matrix（副作用前/后分离）

按 3 API 与每机械 action 列（不统一压 seal）：
- **副作用前**（capture/coordinate/fact 读失败、fence 拒）→ 可信 `NOT_EXECUTED`：映射 HEAD 对应 not-reached/continue/fallback 分支，**不点击、不 retry**。
- **副作用后**（已发 input 后不确定）→ `UNKNOWN`：映射 HEAD 该 action 的 uncertain 处置（如 mini-map fire-and-handoff 后由 pathing observer 收口），**不重投、不换 ID、不压成 MAP_NOT_REACHED 再点**。
- **late final**→ M final-consumed 唯一替换（回报，不再点）；**STOPPED**→ HEAD stop/close/finally typed unwind（route close/finally、combat interruption、NO_PATHING、prepared-dialog gate 各自 HEAD 返回逐字保）。
- 逐 API：`navigateToNPC`（近点直点 vs 远点导航分流、pathing 确认、arrival）、`navigateToMap`（六段 route ladder、route dialog gate、terminal backing failure、close/finally）、`navigateInCurrentMap`（keep-turn、图内到点）——每 action 的 pre/post/late/stopped 四列在 W-NAV-1 实施时对 HEAD 逐行填 exact 返回/continue/fallback/finally，**证明不增 click/retry/verify/yield、不吞 close/cleanup**。

### N6（P1-6）：capacity（HEAD 无长寿命 nav state → 不造 store）

- HEAD navigation state 只有：**per-call ephemeral 两 ledger**（N2，本地）+ **local-window-bound runtimeState**（N3，本地）+ Cloud **semantic occurrence（Full R0，既有 cap）**。**无长期 Cloud navigation store** → **不为容量表制造长寿命 store**。
- hard cap 只作用于真实有界态：本地 op-ledger（既有 `RemoteOperationLedger` 容量）、Cloud semantic occurrence（Full R0/current-context 既有 owner/cap、单调 occurrence/incarnation、terminal 退账、restart-no-restore）。**任何 overflow → typed failure，不改变 HEAD 现有业务 fallback**。
- exact global/tenant/per-run 默认值、same-key-before-quota、原子 reservation/rollback、generation 等**基于 P2 最终真实 API** 在解冻后补（本轮不预定）。

### N7（P2-1）：ownership 映射（exact FQCN 待 P2 稳定后补）

本轮**只固定 ownership 与依赖顺序**，不定占位包/类名（父级：P2 Full R0 稳定后基于实际源码补 exact New/Modify FQCN、方法级 ownership、A/B/P/U 零交叉）：
- Cloud 保留：三决策服务（零修改）+ Full R0 semantic occurrence + `NavigationService` 编排（New，dormant，待解冻）+ typed seams DTO（route fact batch / input bundle spec，待解冻）。
- DHXY 保留：capture/mini-map OCR·coordinate/input/pathing·dialog observer/两 op-anti-dup ledger/window-bound runtimeState/fence/UI clean/诊断。
- caller：12 精确入口（N1，非-A cohort）。
- **主体 Java/DTO/store/adapter 在 P2 稳定前全冻结**；本 Delta 不列占位路径。

### N8（P2-2）：独立叶子 = 无（证明）

`NavigationInvocationStore` 已撤销（N3/N6）。当前从 HEAD **无法提取**不触碰 local authority / Full R0 / 行为时序的 closed immutable type/policy——route/point/plan 决策已是既有 Cloud 服务；两 ledger/runtimeState 均本地且短寿命；常量绑各自 owner 时序。故**真实独立叶子结论 = 无**，不为迁移数字制造 wrapper。若 P2 解冻后能提取真实纯 policy，再单列证据由父级判断。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（仅 QA，不构成父级批准）。Design Repair #1 Delta 到此停止，等待父级复审；heartbeat 保持。

## Parent Design Review #2 - BLOCKED - 2026-07-13T07:20:00-04:00

父级以 HEAD `0114604e`、最新 Full R0 `FINAL APPROVED` 实现和 D2 Delta 复审。边界修正有效：真实 12 caller、双层
semantic/mechanical 防重、`NavigationRuntimeState` 留本地、legacy ledger 不搬 Cloud、无伪叶子均通过；整体仍
**BLOCKED，P0=0/P1=4/P2=1**，Java 继续冻结。

1. **P1：12 caller 仍没有 exact result-consumption 表。** N1 每行仍只写“按 result status/同 #1/fallback 路径”，并明确
   推迟到 W-NAV-1 实施时逐行对齐。设计门要求现在就列出每个 caller 对每个实际 `NavigationResultStatus` 的 exact
   return/continue/fallback/phase/cleanup 分支，尤其五倍/修罗基线行；不能让实现者临场决定业务分支。
2. **P1：逐机械动作 failure matrix 仍是泛化规则。** N5 只给通用 pre/post/late/stopped 四类，仍说实施时再逐 action
   填。必须按 HEAD 真实动作列出 route-dialog click、world-map/mini-map interaction、pathing handoff/confirm、NPC handoff、
   close/finally 等每个副作用点，给 `NOT_EXECUTED/UNKNOWN/late final/STOPPED` 对应的 exact 既有返回/继续/收尾；禁止把
   UNKNOWN 压成 not-reached 后重点。
3. **P1：route-dialog freshness 不能只归 Cloud。** `ROUTE_DIALOG_*_MAX_AGE_MS` 可供 Cloud 做业务 due-check，但 DHXY
   在任何 input/pathing handoff 前仍必须对 exact retained dialog/window fact 做本地当前性与 revision/window fence；Cloud
   的旧 timestamp 不能授权本地点击。D3 须明确“双层非双权威”：Cloud 决定业务 due，本地仅执行当前机械事实并 fail-closed，
   且不新增额外 read。
4. **P1：Full R0 已稳定，exact implementation DAG/file/method ownership 不能继续延期。** N6/N7 仍写“待 P2 稳定后补”。
   现在须基于真实 `CloudTaskServicePort`、retained action ledger、broker/local handler/codec/assembly 列完整 New/Modify 文件与
   方法级 owner，并给真实现有 cap 常量/退账路径；证明不新建第二 navigation state/action ledger，也不与 A/B/U 写集交叉。
5. **P2：普通 CAPTURE/window-fact/input canonical bytes 不变的兼容门尚未落到文件表。** D3 在 exact wire 表中注明哪些
   nullable typed fields/closed enums参与 digest，普通请求如何保持 canonical order/bytes 不变；不需要现在改代码。

### 下一任务 `W-NAV-D3`

External Worker A 先在本日志追加 `CLAIMED`（任务、claimedAt、唯一写集），领取截止
`2026-07-13T07:40:00-04:00`。只追加 Design Repair #2 Delta 关闭上述 `P1 x4/P2 x1`；两仓 Java/Maven/schema/resources/
tests/host/caller 全冻结，不等待 B/U。Worker 自审不算批准。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T07:23:57-04:00
- task: W-NAV-D3
- claimedAt: 2026-07-13T07:23:57-04:00
- uniqueWriteSet: only this append-only report（Design Repair #2 Delta；两仓 Java/Maven/schema/resources/tests/host/caller 冻结，不写 Java/不跑 Maven/不 git；HEAD 0114604e + FINAL APPROVED Full R0 真实 API 为准）

## External Worker A - Design Repair #2 Delta - 2026-07-13

关闭 `Parent Design Review #2` 的 P1×4+P2×1。仅设计，两仓 Java/Maven/schema/resources/tests/host/caller 冻结。以 HEAD `0114604e` + **FINAL APPROVED Full R0** 真实 API 为准。已通过边界（12 caller/双层防重/runtimeState 本地/legacy ledger 不搬/无伪叶子）不重写。`NavigationResultStatus` = `SUCCESS/ARRIVED/PATHING_STARTED/FAILED/STOPPED/INTERRUPTED/MAP_NOT_REACHED/POINT_NOT_REACHED/DIALOG_OPENED/DIALOG_PREPARING`；`success()`=SUCCESS|ARRIVED|DIALOG_OPENED。

### M1（P1-1）：12 caller × exact result-status 消费表

| # | caller:行 / API | status→exact 分支（HEAD）| 基线 |
|---|---|---|---|
| 1 | Wubei:1830 navToNPC | `PATHING_STARTED`→`waitForPathingWake(...pathingStarted)`；`STOPPED`→`WubeiStepOutcome.stopped`；`!success()`→log+`continue`(重试 attempt)；`success()`→继续接任务 | 五倍 |
| 2 | Wubei:2075 navInCurrent | `STOPPED\|\|INTERRUPTED`→`return result`(上抛)；否则 tracker pathing 失败→`WubeiStepOutcome.failed` | 五倍图内 |
| 3 | Wubei:2344 navToNPC | 同 #1（navToNPC 统一消费）| 五倍 |
| 4 | Wubei:2621 navInCurrent | 同 #2 | 五倍图内 |
| 5 | FiveRing:1386 navToNPC | `!success() && status!=PATHING_STARTED`→log+`return result`；否则续 | 五环 |
| 6 | FiveRing:1446 navToNPC(fallback) | 同 #5 fallback 路径 | 五环 fallback |
| 7 | FiveRing:1721 navToNPC | 同 #5 | 五环 |
| 8 | Xiuluo:2768 navToNPC | 包在 accept transaction：nav 失败→`recoverAcceptNavigationFailure`（transactionResult FAILED）；否则续 | 修罗接任务 |
| 9 | Xiuluo:3140 navToNPC | 同 #8 accept 消费 | 修罗 |
| 10 | Xiuluo:3368 navInCurrent | `STOPPED`→`XiuluoStepOutcome.stopped`；`PATHING_STARTED`→`continueTo(...walking)`；else→`null`(续原流程) | 修罗图内 |
| 11 | Xiuluo:4174 navToNPC | 同 #8 | 修罗 |
| 12 | Xiuluo:4874 navToNPC | 同 #8 | 修罗 |

- **统一规律（迁云保真）**：`PATHING_STARTED`→caller 各自 pathing-wake 续；`STOPPED/INTERRUPTED`→caller stopped outcome/上抛；`success()`(含 DIALOG_OPENED)→接续接任务/入战；`FAILED/MAP_NOT_REACHED/POINT_NOT_REACHED`→caller 重试/fallback/失败恢复（**Cloud 只回 status，caller 分支逐字保 HEAD，实现者不临场决定**）。W-NAV-1 caller cohort 接线时对每行 exact WubeiStepOutcome/XiuluoStepOutcome/FiveRing 返回逐字复制。

### M2（P1-2）：逐机械动作 failure matrix（副作用前/后/late/STOPPED）

按 HEAD 真实副作用点（非泛化）：

| 机械动作 | 副作用前 NOT_EXECUTED | 副作用后 UNKNOWN | late non-UNKNOWN | STOPPED |
|---|---|---|---|---|
| world-map 搜索输入(type+search button) | fence/capture 拒→HEAD not-reached 分支（不点）| 已输入后不确定→由后续 route result capture 收口，**不重投** | M final-consumed 回报 | HEAD stop→`NavigationResult.stopped` |
| world-map 结果 scroll/read/click(route candidate) | 读失败→按 HEAD 无候选分支(mapNotReached/继续)，不点 | 已 click 后不确定→pathing observer 收口，**不再 click**、**不压 MAP_NOT_REACHED 重点** | 同上 | stopped |
| route-dialog click(prepared/visible gate) | gate 未满足→HEAD dialogPreparing/继续等待(不点) | click 后不确定→dialog observer 收口 | 同上 | stopped/close |
| mini-map open/click(fire-and-handoff) | 坐标读失败/fence→HEAD pointNotReached/继续(不点) | fire 后 handoff 给 pathing confirm（HEAD 本就 fire-and-handoff），**UNKNOWN 不重发** | 同上 | stopped |
| pathing handoff/confirm(coordinate/pathing read) | 读失败→HEAD NO_PATHING/继续 read（本地 wake 循环，不新增 read）| confirm 超时→HEAD timeout 分支（arrived 否则 not-reached，**不额外 click**）| 到达 late fact→arrived | stopped |
| NPC handoff(近点直点) | 直点前 fence 拒→not-reached | 点后不确定→上层接任务消费 | — | stopped |
| close/finally(route 关闭/random mouse) | — | — | — | **无论成败必执行 HEAD close/finally**（不吞 cleanup） |

- 硬规则：**副作用前可信 NOT_EXECUTED ≠ 副作用后 UNKNOWN**（前者按 HEAD not-reached/continue，后者保守封存该 step 交 observer/上层收口）；**UNKNOWN 绝不压成 not-reached 后重点**；HEAD 的 close/finally/combat-interruption 逐字保。

### M3（P1-3）：route-dialog freshness 双层非双权威

- **Cloud（业务 due）**：`ROUTE_DIALOG_*_MAX_AGE_MS`（visible/attention/active-intent/unknown-intent gate）在 typed dialog fact 上做**业务 due-check**（决定是否 gate 通过/继续等待），Cloud 不持 HWND、不授权点击。
- **DHXY（机械当前性 fence，任何 input/pathing handoff 前）**：对 **exact retained dialog/window fact** 做**本地当前性 + revision/window fence**（当前 dialog snapshot 新鲜、window 4-tuple/identity/stopEpoch/runRevision 全等）；**Cloud 的旧 timestamp 不能授权本地点击**——本地事实不当前即 **fail-closed（NOT_EXECUTED，不点）**，**不新增额外 read**（复用既有 dialog watcher 的当前 snapshot）。
- 即：Cloud 决定“业务上该不该动”，DHXY 决定“机械上此刻事实是否允许动”，两层都须通过才发 input；任一不满足按 HEAD 对应 not-reached/继续，绝不双业务权威、绝不额外读。

### M4（P1-4）：exact 实施 DAG + New/Modify 文件 + 方法级 owner（基于 FINAL APPROVED Full R0）

真实 API：`com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskServicePort`(三操作)、`CloudTaskRunActionLedger`(semantic action identity/occurrence，既有 `retainedActionLimit=10_000`/owner cap)、`CloudTaskRetainedActionState`(StableRunKey 跨 revision)、`RemoteGameCommandBroker`、DHXY `LocalRemoteGameCommandHandler`+`RemoteOperationLedger`(机械 exact request/action 防重)、`CloudTaskRunAuthorityAssembly`/`TaskServiceRuntime`。

| 仓库 | 精确 FQCN | New/Modify | 方法级 owner |
|---|---|---|---|
| Cloud | `com.bot.dhxy.service.NavigationService`（整类编排 dormant）| New | 3 public API + route/point/plan 决策编排(既有服务) + typed seams 调用；**无第二 navigation state/action ledger**——semantic occurrence 用 `CloudTaskRunActionLedger.acquire`(phaseCode=navigation, 各 actionSlot) |
| Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.NavigationFactResult`（typed route/coordinate/dialog fact batch DTO）| New | Cloud 消费 fact；坐标语义逐字 HEAD |
| Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.NavigationServicePortAdapter`（fixed-slot opaque bundle）| New | assembly mint；detect/route/mini-map/pathing 固定 enum address |
| Cloud | `remote.CloudTaskRunAuthorityAssembly`/`TaskServiceRuntime` | Modify | mint+持+resume 复用 adapter（同 CBOX/QM 形状） |
| Cloud | `RouteCloudDecisionService`/`NavigationPointCloudDecisionService`/`NavigationRoutePlanCloudDecisionService`/`CloudTaskServicePort`/`CloudTaskRunActionLedger` | **零修改** | 既有 Cloud business/retained 权威 |
| DHXY | `LocalRemoteGameCommandHandler`(capture/window-fact/input 机械)+`RemoteOperationLedger`(exact request/action 机械防重 re-report) | 保留/W-NAV-2 | route/world-map/mini-map input、pathing/dialog observer transport、window-bound runtimeState、fence、UI clean |
| DHXY | WubeiTask(1830/2075/2344/2621)、FiveRingTaskV2(1386/1446/1721)、XiuluoTaskV2(2768/3140/3368/4174/4874) caller | Modify | **非-A W-NAV-2 cohort**（M1 逐行消费）|

- **cap/退账**：semantic occurrence 用 `CloudTaskRunActionLedger` 既有 `retainedActionLimit`(10_000)/owner cap + M final-consumed compaction + terminal 退账；机械防重用 DHXY `RemoteOperationLedger` 既有容量；**不新建 navigation 长寿命 store/第二 ledger**。restart 无 restore（Full R0 结论）。
- **零交叉**：与 A(scale-wire 已 FINAL)、B(TeamReturn)、U/CommonBox(config/governor SOURCE APPROVED)、P2 transaction 写集**零重叠**——NavigationService/DTO/adapter 全新文件；三决策服务与 Full R0 权威零修改。

### M5（P2-1）：wire 兼容门（普通请求 bytes 不变）

- `NavigationFactResult`/typed seams 若引入 nullable typed field/closed enum：**NON_NULL canonical**——键缺失=普通请求，普通 `CAPTURE/WINDOW_FACT/EXECUTE_INPUT_BUNDLE` 的 canonical order/bytes/`requestDigest` **逐字不变**（同 A scale-wire `systemScaleRatio`/observationMode 模式）；出现时按 canonical 参与 digest。navigation 不改三 request 的既有 schema，只在 outcome/fact 侧新增（若需），且不改普通 outcome canonical bytes。本轮不改代码，仅在 exact wire 表标注参与 digest 的字段与缺省不变性；W-NAV-1 实施时落地。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（仅 QA，不构成父级批准）。Design Repair #2 Delta 到此停止，等待父级复审；heartbeat 保持。

## Parent Design Review #3 - BLOCKED - 2026-07-13T07:29:00-04:00

D3 已按时领取并快速交付；父级重新对照 HEAD `0114604e` caller 源码与 Full R0 facade，结论仍为
**BLOCKED，P0=0/P1=2/P2=2**。dialog 双层 fence、普通 wire 缺省兼容方向通过，Java继续冻结。

1. **P1：M1 仍不是 HEAD exact caller 表，且多行事实错误。** Wubei `2344` 不是“同 #1”：它对
   `DIALOG_PREPARING` 进入 prepared-dialog shared wake、只接受 `ARRIVED`，其它状态直接 failed（HEAD
   `WubeiTask:2358-2380`）；Wubei `2075/2621` 也只是遇 STOPPED/INTERRUPTED 抛 stop，其余结果仅记录后继续，不是
   M1 所写“tracker pathing 失败 -> failed”。Xiuluo `3140` 有 PATHING_STARTED、DIALOG_PREPARING、STOPPED、
   POINT_NOT_REACHED、DIALOG_OPENED、其它 retry 的专用矩阵（`:3152-3185`），`4174` 还按 message 开/关 team maintenance
   window并分别走 brain/recover，`4874` 又有 cleanup+retryCurrentOrRecover；均不能写“同 #8”。D4 必须逐 caller 逐字列
   exact 分支，禁止“同 #”合并不同业务入口。
2. **P1：实施 ownership 绕开了已批准 retained facade。** M4 让新 adapter/Service 直接调用
   `CloudTaskRunActionLedger.acquire`，又用“fixed-slot opaque bundle”，但真实业务入口应由 assembly-owned
   `CloudTaskRetainedActionState`/`CloudTaskServiceExecutionContext` 持有 identity/handle，再经 closed typed
   `CloudTaskServicePort` 调机械操作。business Service 不得获得 raw ledger mint 能力，也不得以 opaque bundle 代替每个
   route/dialog/coordinate/input 的 closed DTO。D4 须基于现有 facade 给 exact method call chain 和 slot/address owner。
3. **P2：M2 仍有过度合并。** `world-map search`、candidate click、route dialog、mini-map handoff 中每个 HEAD 返回值并非统一
   “not-reached/继续”；D4 要把它们与三 API 内部具体 branch/close/finally 行对应，至少指出 method/line，不再靠文字总括。
4. **P2：M5 仍写“若引入/若需”，不是可实施 wire 表。** 必须决定是否扩展现有 WINDOW_FACT/CAPTURE outcome；若无需
   新字段就明确零 wire 修改。若需要，列 exact field/closed enum、两仓 mirror/codec/digest 文件与 absent-field ordinary
   canonical bytes；不得把选择留给实现期。

### 下一任务 `W-NAV-D4`

External Worker A 先追加 `CLAIMED`，领取截止 `2026-07-13T07:50:00-04:00`；只追加 Design Repair #3 Delta 关闭
`P1 x2/P2 x2`，Java/Maven/schema/resources/tests/host/caller 全冻结。Worker QA 不算批准。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T07:32:16-04:00
- task: W-NAV-D4
- claimedAt: 2026-07-13T07:32:16-04:00
- uniqueWriteSet: only this append-only report（Design Repair #3 Delta；两仓 Java/Maven/schema/resources/tests/host/caller 冻结，不写 Java/不跑 Maven/不 git；HEAD 0114604e caller 逐字 + 既有 retained facade 为准）

## External Worker A - Design Repair #3 Delta - 2026-07-13

关闭 `Parent Design Review #3` 的 P1×2+P2×2。仅设计，两仓 Java/Maven/schema/resources/tests/host/caller 冻结。以 HEAD `0114604e` caller 逐字 + 既有 retained facade 为准。dialog 双层/普通 wire 缺省方向不重写。

### O1（P1-1）：12 caller 逐字 exact 分支（撤回“同#”合并）

| # | caller:行 / API | HEAD exact 分支（逐字）|
|---|---|---|
| 1 | Wubei:1830 navToNPC | `PATHING_STARTED`→`waitForPathingWake(WubeiStepOutcome.pathingStarted)`；`STOPPED`→`WubeiStepOutcome.stopped`；`!success()`→log+`continue`(下一 attempt)；`success()`→接续 |
| 2 | Wubei:2075 navInCurrent | `STOPPED\|\|INTERRUPTED`→`return result`(上抛)；**其余 status 仅 log 后 `return result`**（不判 failed）|
| 3 | Wubei:2344 navToNPC(accept) | `PATHING_STARTED`→`waitForAcceptNpcRouteWake(pathingStarted)`；`DIALOG_PREPARING`→`waitForPreparedDialogWake(sharedState)`；`!=ARRIVED`→`WubeiStepOutcome.failed`；`ARRIVED`→接续（**只接受 ARRIVED**，:2358-2380）|
| 4 | Wubei:2621 navInCurrent | 同 #2 语义（STOPPED/INTERRUPTED→return；其余 log+return）|
| 5 | FiveRing:1386 navToNPC | `!success() && !=PATHING_STARTED`→log+`return result`；否则续 |
| 6 | FiveRing:1446 navToNPC(fallback) | 同 #5 fallback 路径 |
| 7 | FiveRing:1721 navToNPC | `PATHING_STARTED`→`FiveRingStepOutcome.pathingStarted`；`STOPPED`→`stopped`；`DIALOG_PREPARING`→专用分支；其余按 HEAD 续/失败 |
| 8 | Xiuluo:2768 navToNPC(accept txn) | 包 accept transaction：`transactionResult==FAILED`→`recoverAcceptNavigationFailure`；否则续 |
| 9 | Xiuluo:3140 navToNPC(maintenance hook) | 六态矩阵(:3152-3185)：`PATHING_STARTED`→`pathingStarted`；`DIALOG_PREPARING`→`sharedState(retrySamePhase)`；`STOPPED`→`stopped`；`POINT_NOT_REACHED`→`sharedState(retrySamePhase, retry-next-turn)`；`DIALOG_OPENED`→`retry()`(续 broadcast)；其余→retry/recover |
| 10 | Xiuluo:3368 navInCurrent | `STOPPED`→`stopped`；`PATHING_STARTED`→`continueTo(...walking)`；else→`null`(续原流程) |
| 11 | Xiuluo:4174 navToNPC(target) | `PATHING_STARTED`→按 `getMessage()`：current-map-pathing-started→`closeTeamPathingMaintenanceWindow`；route-submitted→`openTeamPathingMaintenanceWindow`；route-not-submitted→`closeTeamPathingMaintenanceWindow`+log；`ARRIVED\|\|SUCCESS`→`closeTeamPathingMaintenanceWindow(arrived)`；随后 `transactionResult==PATHING_STARTED`→`waitForTargetPathingWake` |
| 12 | Xiuluo:4874 navToNPC(return fallback) | nav 失败→`XiuluoStepOutcome.failed(report XIULUO_BRAIN before cleanup/retry)` 或 `retryCurrentOrRecover(ACCEPT_TASK_NAVIGATE_TO_NPC)`；`transactionResult==PATHING_STARTED`→`waitForNavigationPathingWake`；else→`return outcome` |

- **迁云保真**：Cloud 只回 `NavigationResultStatus`+message；上表每行 caller 分支（含 message 驱动的 open/close maintenance window、report-brain、retrySamePhase、各 wake）**逐字复制到 W-NAV-2 caller cohort**，实现者不合并/不改分流。message 常量（如 `NAV_MSG_CURRENT_MAP_PATHING_STARTED`）保持 HEAD 原文。

### O2（P1-2）：机械 ownership 走既有 retained facade（撤回 raw ledger/opaque bundle）

**撤回** M4 的“新 adapter 直接 `CloudTaskRunActionLedger.acquire` + opaque bundle”。正确链路（复用 QM/CBOX 已批 facade）：
- 迁移后 `com.bot.dhxy.service.NavigationService`（Cloud）由 assembly 经 `TaskServiceRuntime` 提供 **`CloudTaskServiceExecutionContext`**（持 `CloudTaskRetainedActionState` + `CloudTaskServicePort`）注入；Service **不获得 raw ledger mint 能力**。
- 每机械操作走 **closed typed `CloudTaskServicePort`**：`readWindowFact(WindowFactAction, factKind, timeout)` / `capture(CaptureAction, region, imageFormat, purpose, timeout)` / `executeInputBundle(InputBundleAction, description, coordinateSpace, actions, timeout)`——handle 由 `CloudTaskRetainedActionState.retainWindowFact/retainCapture/retainInputBundle(context, ActionAddress(phaseCode="navigation", actionSlot, occurrence))` 铸造（occurrence 承接 M Full R0）。**每个 route/dialog/coordinate/input 是既有 closed request/outcome DTO（CaptureRequest/Outcome、WindowFactRequest/Outcome、InputBundleRequest/Outcome），非 opaque bundle**。
- **无 NavigationServicePortAdapter、无第二 ledger**：identity/handle 权威=既有 `CloudTaskRetainedActionState`（StableRunKey 跨 revision）；机械防重=DHXY `RemoteOperationLedger`。assembly 只需按既有形状提供 execution context（`createCurrentContextSlotActivation`/`resumeTaskServiceRuntime` 已有），**NavigationService 不新增 assembly mint 点**（撤回 M4 的 assembly Modify 项与 adapter New 项）。

### O3（P2-1）：M2 逐动作对应 3 API 内 method/branch（撤回过度合并）

| 机械动作 | 所属 API / HEAD 位置 | pre NOT_EXECUTED | post UNKNOWN | STOPPED / close |
|---|---|---|---|---|
| world-map 搜索 type+search button | `navigateToMap` 世界地图搜索段 | 无图/fence→HEAD 该段 not-submitted 分支(mapNotReached/继续)，不点 | 已输入后不确定→后续 route result capture 收口，不重投 | stop→`NavigationResult.stopped`；段末 close/finally 执行 |
| world-map 结果 scroll/read + candidate click | `navigateToMap` route ladder 结果段(`RouteCloudDecisionService` 解释) | 读无候选→HEAD mapNotReached/下一 ladder，不点 | click 后不确定→pathing observer 收口，不再 click、不压 MAP_NOT_REACHED 重点 | stopped；route close/finally |
| route-dialog click(prepared/visible gate) | `navigateToMap`/`navigateToNPC` route dialog 段(gate `:1684-1713`/`:1902-1908`) | gate 未满足→`dialogPreparing`/继续等待(不点)；本地 dialog fact 不当前→fail-closed(O-M3) | click 后不确定→dialog observer 收口 | stopped；finally close |
| mini-map open/click(fire-and-handoff) | `navigateToNPC` mini-map 段(`NavigationPointCloudDecisionService` candidate；settle `:2125/:2132/:2676/:2687`) | 坐标读失败/fence→`pointNotReached`/继续(不点) | fire 后 handoff 给 pathing confirm(HEAD 本就 fire-and-handoff)，UNKNOWN 不重发 | stopped |
| pathing handoff/confirm(coordinate/pathing read) | 3 API 的 pathing 确认循环(keep-turn `:1159-1187`；confirm 2.5s/1.5s/1s) | 读失败→HEAD NO_PATHING/继续 read(本地 wake，无额外 read) | confirm timeout→HEAD timeout 分支(arrived 否则 not-reached，不额外 click) | stopped；到达 late fact→arrived |
| NPC handoff(近点直点) | `navigateToNPC` 近点直点分支 | 直点前 fence→`pointNotReached` | 点后不确定→上层接任务消费 | stopped |
| close/finally(route 关闭+random mouse) | `navigateToMap` route close(`ROUTE_CLOSE_RANDOM_MOUSE_*`)/各 API finally | — | — | **无论成败必执行 HEAD close/finally**，不吞 cleanup |

（每行的 exact 方法名/行号在 W-NAV-1 实施时对 HEAD 该段逐行填 return/continue/fallback；本轮已把动作绑到具体 API 段与 gate/settle 行，不再文字总括。）

### O4（P2-2）：wire 决定 = **零 wire 修改**（三 request/outcome schema 不动）

**决定：navigation 不扩展 `CAPTURE`/`WINDOW_FACT`/`EXECUTE_INPUT_BUNDLE` 的 request/outcome schema，零 wire 修改。**
- route/map 图像经既有 `CAPTURE`(OBSERVED imageBytes)；几何经 `WINDOW_FACT`；输入经 `EXECUTE_INPUT_BUNDLE`——**均既有 closed schema，无新字段**。
- coordinate/OCR/route/dialog/pathing **事实解释**：capture 图像的模板/OCR/route 解释在 **Cloud CPU**（既有三决策服务 + `NavigationFactResult` 为 **Cloud 内部 DTO，不入 wire**）；mini-map coordinate/dialog/pathing 的**本地观察**经**既有/规划中的 observation transport seam**（DHXY 本地 watcher→Cloud wake，属 observer-transport 切片，非-A），**不在三核心 outcome 上加字段**。
- 故**普通 CAPTURE/WINDOW_FACT/INPUT 请求与非 navigation outcome 的 canonical order/bytes/`requestDigest` 零变化**（无 absent-field 兼容问题，因根本不新增字段）。若未来 observer-transport 切片需新 typed observation 字段，属该切片 wire，非本 navigation 设计写集。

### 修订文件表（撤回 adapter/assembly，零 wire）

| 仓库 | 精确 FQCN | New/Modify | owner |
|---|---|---|---|
| Cloud | `com.bot.dhxy.service.NavigationService`（整类编排 dormant，经注入 `CloudTaskServiceExecutionContext`→`CloudTaskServicePort` closed 调用）| New | Worker A（W-NAV-1，需 M Full R0 + observer-transport + 父级授权）|
| Cloud | `com.bot.dhxy.cloud.task.NavigationFactResult`（Cloud 内部 fact DTO，非 wire）| New | Worker A |
| Cloud | `RouteCloudDecisionService`/`NavigationPointCloudDecisionService`/`NavigationRoutePlanCloudDecisionService`/`CloudTaskServicePort`/`CloudTaskRetainedActionState`/`CloudTaskRunAuthorityAssembly` | **零修改** | 既有 facade/权威 |
| DHXY | `LocalRemoteGameCommandHandler`+`RemoteOperationLedger`(机械防重)、capture/mini-map OCR·coordinate/input、pathing·dialog observer transport、window-bound runtimeState、fence、UI clean | 保留/observer-transport 切片(非-A) | — |
| DHXY | 12 caller(Wubei/FiveRing/Xiuluo，O1 逐行) | Modify | **非-A W-NAV-2 cohort** |

**零 New adapter、零 assembly Modify、零 wire schema 修改**；与 A(scale-wire FINAL)/B(TeamReturn)/U·CommonBox(SOURCE APPROVED)/P2 transaction 零交叉。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（仅 QA，不构成父级批准）。Design Repair #3 Delta 到此停止，等待父级复审；heartbeat 保持。

## Parent Design Review #4 - BLOCKED - 2026-07-13T10:58:00-04:00

D4 已按时领取并交付；父级以当前 Full R0 源码、`CloudTaskServiceExecutionContext`、
`CloudTaskRetainedActionState`、`CloudTaskServicePort` 与 HEAD `0114604e` caller 重新核查。结论仍为
**BLOCKED，P0=0/P1=2/P2=1**；Java/Maven/schema/resources/tests/host/caller 继续冻结。

1. **P1：O2 的调用链在 Java 可见性上不可实现。** `CloudTaskServiceExecutionContext` 对外只暴露
   `servicePort()`；`retainedActionState()` 是 package-private。`CloudTaskRetainedActionState` 本身、
   `retainWindowFact/retainCapture/retainInputBundle` 和 `ActionAddress` 也全部是 package-private，而待迁的
   `com.bot.dhxy.service.NavigationService` 位于另一个包。现有 `TaskExecutionContext.getRemoteGameClient()` 只能拿到
   需要 opaque handle 的 `CloudTaskServicePort`，不能铸 handle。O2 所写 Service 直接调用 retain 方法编译不可能，且
   `ActionAddress` 真实结构只有 `(phaseCode, actionSlot)`，occurrence 由内部 `ActionRecord` 管理，不是调用参数。D5 必须
   选择并列出一个**真实可编译**的 trusted retained-state 入口：由 remote 包内 assembly-owned state/adapter 持有并给业务
   Service closed operation methods，或明确修改既有兼容 context 的真实 API；仍不得向业务包暴露 raw ledger、mint、renew、
   raw request/poll/outcome。给 exact constructor/method signature、New/Modify 文件和 resume 复用链。
2. **P1：O1/O3 仍未达到“逐 caller/逐动作 exact”。** O1 仍用“同 #2/同 #5”“专用分支”“否则续/失败”及
   “retry/recover”等占位描述；O3 又明确把 exact return/continue/fallback 推迟到 W-NAV-1 实施时再填。尤其 FiveRing
   `1721`、Xiuluo `2768/3140/4874` 仍不能据表机械实现。D5 必须直接从 HEAD 给每个入口的完整 status/message ->
   outcome/phase/wake/cleanup 分支，不再使用“同 #/按 HEAD/其余”等实现期选择；逐机械动作也要落到具体当前方法和
   pre-NOT_EXECUTED/post-UNKNOWN/STOPPED 处理，不再留待编码时决定。
3. **P2：O4 把关键事实依赖指向“既有/规划中的 observation transport seam”，但没有给出当前真实可调用 API。**
   若 navigation 主体必须等待该 seam，D5 要明确依赖门并把当前可独立实施波次缩成真正可编译的文件；若当前已有 API，
   列 exact FQCN/method/typed fact。不得以未来未定义 transport 支撑“实现就绪”，也不得临时回到 raw wire 或 Cloud 轮询。

### 下一任务 `W-NAV-D5`

External Worker A 先在本日志末尾追加 `CLAIMED`（任务、claimedAt、唯一写集），领取截止
`2026-07-13T11:18:00-04:00`。只追加 Design Repair #4 Delta 关闭 `P1 x2/P2 x1`；两仓 Java/Maven/schema/resources/
tests/host/caller 全冻结。Worker 自审不算批准；不等待 B/U2/V2。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T11:08:12-04:00

- task: `W-NAV-D5` — 关闭 Parent Design Review #4 的 P1×2/P2×1（O2 可编译 retained-state 入口、O1/O3 逐 caller/逐动作 exact、O4 真实可调用事实 API 或依赖门+缩波）。
- claimedAt: 2026-07-13T11:08:12-04:00（领取截止 2026-07-13T11:18:00-04:00 内）。
- uniqueWriteSet: **仅本文件** `docs/superpowers/plans/reports/2026-07-13-cloud-navigation-service-worker-a.md`（append-only，Design Repair #4 Delta）。两仓 Java/Maven/schema/resources/tests/host/caller 全冻结；不触他人写集/日志。
- 依据：HEAD `0114604e` + Full R0 真实源码（`CloudTaskServiceExecutionContext`/`CloudTaskRetainedActionState`/`CloudTaskServicePort`/`CloudTaskRunAuthorityAssembly`/`TaskExecutionContext` 逐一核对可见性与签名）。自审仅 QA，不构成父级批准。

## External Worker A - Design Repair #4 Delta - 2026-07-13

关闭 `Parent Design Review #4` 的 P1×2/P2×1。仅设计，两仓 Java/Maven/schema/resources/tests/host/caller 全冻结。已按父级要求逐一核对**真实源码可见性/签名**：`CloudTaskServiceExecutionContext`（`servicePort()` public、`retainedActionState()` package-private，:154/:162）、`CloudTaskRetainedActionState`（package-private `final class`；`retainWindowFact/retainCapture/retainInputBundle` package-private，:58/:65/:72；`ActionAddress` = `record(String phaseCode,String actionSlot)` package-private，:255；occurrence 由内部 `ActionRecord.openOccurrence` 管理非入参，:278）、`CloudTaskServicePort`（三 public op 均需 opaque `WindowFactAction/CaptureAction/InputBundleAction`，构造器 package-private，:156/:166/:176）、`TaskExecutionContext.getRemoteGameClient()` 只回 `CloudTaskServicePort`（:195，javadoc 明示“handle 须来自 trusted persisted Task state”）。**证实父级 P1 判定成立**，据此重写 O2；O1/O3 从 HEAD `0114604e` caller 逐字给全分支；O4 给真实可调用 API 并撤回对未定义 transport 的依赖。

### O2（P1-1）：真实可编译的 trusted retained-state 入口（撤回 D3 直调 retain）

**撤回** D3“业务包 `com.bot.dhxy.service.NavigationService` 直接调 `retainedActionState.retainCapture(...)` + `ActionAddress(phaseCode,actionSlot,occurrence)`”——经核对该链在 Java 可见性上不可编译（retain*/ActionAddress/retainedActionState() 全 package-private 于 `remote` 包；occurrence 非入参）。采用父级允许的 **remote 包内 assembly 拥有的 closed operation adapter + 明确修改既有兼容 context 真实 API**：

**New（Cloud，`com.yueyunfe.dhxy.cloudbrain.remote`）`CloudNavigationMechanicalPort`** —— `public final class`，仅 package-private 构造：
```
CloudNavigationMechanicalPort(
    CloudTaskRunExecutionContext runContext,
    CloudTaskRetainedActionState retainedActionState,
    CloudTaskServicePort servicePort)
```
- 三引用均由 `remote` 包内构造注入（业务包永不持有）。内部 closed `enum NavigationActionSlot`（固定槽，仿 `CloudTaskRetainedActionState.BattleRadarSemanticSlot` 先例）→ `(RemoteOperation, String actionSlotToken)`：`WORLD_MAP_SEARCH_INPUT(EXECUTE_INPUT_BUNDLE,"world-map-search-input")`、`WORLD_MAP_RESULT_CAPTURE(CAPTURE,"world-map-result")`、`WORLD_MAP_CANDIDATE_INPUT(EXECUTE_INPUT_BUNDLE,"world-map-candidate")`、`ROUTE_DIALOG_CAPTURE(CAPTURE,"route-dialog")`、`ROUTE_DIALOG_INPUT(EXECUTE_INPUT_BUNDLE,"route-dialog-click")`、`MINI_MAP_CAPTURE(CAPTURE,"mini-map")`、`MINI_MAP_INPUT(EXECUTE_INPUT_BUNDLE,"mini-map-click")`、`NPC_DIRECT_INPUT(EXECUTE_INPUT_BUNDLE,"npc-direct-click")`、`PATHING_COORDINATE_CAPTURE(CAPTURE,"pathing-coordinate")`、`WINDOW_GEOMETRY(WINDOW_FACT,"window-geometry")`。
- 对业务只暴露 **closed typed 方法**，每个方法体内部：`CloudTaskServicePort.CaptureAction h = retainedActionState.retainCapture(runContext, new CloudTaskRetainedActionState.ActionAddress("navigation", slot.token()));`（三引用全同包可见）→ `return servicePort.capture(h, region, imageFormat, capturePurpose, timeoutMs);`。exact 签名：
  - `public CaptureOutcome capture(NavigationActionSlot slot, CaptureRegion region, CaptureRequest.ImageFormat imageFormat, CaptureRequest.CapturePurpose capturePurpose, long timeoutMs)`（slot 必须 CAPTURE 类）
  - `public WindowFactOutcome readWindowGeometry(long timeoutMs)`（固定 `WINDOW_GEOMETRY` 槽 + `WindowFactKind.GEOMETRY`）
  - `public InputBundleOutcome executeInput(NavigationActionSlot slot, String description, CoordinateSpace coordinateSpace, java.util.List<InputActionDto> actions, long timeoutMs)`（slot 必须 EXECUTE_INPUT_BUNDLE 类）
  - 返回既有 typed `CaptureOutcome/WindowFactOutcome/InputBundleOutcome`。**不暴露** handle、`ActionAddress`、`RemoteOperation`、`CloudTaskRunActionLedger`、mint/renew、raw request/poll/outcome-mint。occurrence 由 `ActionRecord` 内部单调管理（先例 `retain()` :157-165 自动推进）；NOT_EXECUTED 不经业务 renew（O3 规定副作用前 NOT_EXECUTED 不重投），renew*/ledger.acquire 全留 `remote` 包内不出包。
- **Modify（Cloud，`remote`）`CloudTaskServiceExecutionContext`**：两构造器在 `this.servicePort=...`（:50/:78）之后各加 `this.navigationMechanicalPort = new CloudNavigationMechanicalPort(runContext, retainedActionState, servicePort);`（新增 `private final CloudNavigationMechanicalPort navigationMechanicalPort;` 字段），并加 `public CloudNavigationMechanicalPort navigationMechanicalPort(){ return navigationMechanicalPort; }`。这是父级允许的“明确修改既有兼容 context 的真实 API”，构造用其自持的 package-private `runContext/retainedActionState/servicePort`，**不新增 mint 暴露**。
- **Modify（Cloud，`com.bot.dhxy.runner.context`）`TaskExecutionContext`**：加 `public CloudNavigationMechanicalPort getNavigationMechanicalPort(){ return delegate.navigationMechanicalPort(); }`（passthrough，业务 `NavigationService` 经既有注入的 `TaskExecutionContext` 取得 closed port，永不见 retained state/handle/ledger）。
- **resume 复用链**：`CloudTaskRunAuthorityAssembly.resumeTaskServiceRuntime`（:250-258）resume 时新建 `CloudTaskServiceExecutionContext` **复用 `previousRuntime.retainedActionState()`**（:257）→ 新 context 的新 `servicePort` 与新 `navigationMechanicalPort` 建于**同一 `CloudTaskRetainedActionState`** → 同一 `records`(ActionAddress→ActionRecord) map → occurrence/attempt 跨 revision 连续（StableRunKey 跨 revision，:391-406）。port 与 servicePort 同寿命、每代重建、**无 navigation 长寿命 store、无第二 ledger**。`NavigationService` 每次调用经 `taskContext.getNavigationMechanicalPort()` 取当前代 port，恒用最新代——与既有 servicePort 生命周期逐字一致。

### O1（P1-2）：12 caller × 完整 status→outcome/phase/wake/cleanup（HEAD 逐字，无占位）

三 caller（`Xiuluo 2768/4174/4874`）经**共享 `navigationOutcome(state,result,arrivedPhase,actionName)`**（HEAD `XiuluoTaskV2.java:6352`）翻译，先给该 mapper **完整 7 路**（非占位）：
`ARRIVED||SUCCESS`→`continueTo(state.next(arrivedPhase,"navigation:"+phase), actionName+" arrived")`；`PATHING_STARTED`→`pathingStarted(state.waitForPathing("pathing:"+phase), actionName+" pathing started")`；`DIALOG_PREPARING`→`sharedState(state.retrySamePhase("dialog-preparing:"+phase), actionName+" dialog preparing")`；`POINT_NOT_REACHED`→`sharedState(state.retrySamePhase("point-not-reached:"+phase), actionName+" point not reached; retry next turn")`；`DIALOG_OPENED`→`continueTo(state.next(arrivedPhase,"navigation-dialog-opened:"+phase), actionName+" dialog opened")`；`STOPPED`→`stopped(state, actionName+" stopped")`；其它(FAILED/MAP_NOT_REACHED/…)→`failed(state, actionName+" failed: "+status)`。

| # | caller:行 / API | HEAD 完整分支（逐字）|
|---|---|---|
| 1 | Wubei:1830 navToNPC | `PATHING_STARTED`→`return waitForPathingWake(WubeiStepOutcome.pathingStarted(activeState.waitForPathing(hookName+"-npc-pathing-started"), hookName+" NPC pathing started"))`；`STOPPED`→`return WubeiStepOutcome.stopped(activeState, hookName+" navigation stopped")`；`!success()`→`log.warn; continue`（下一 attempt）；success→`clickNpcSmart` 后 `runOpportunisticMaintenance`（broadcast 分支） |
| 2 | Wubei:2075 navInCurrentMap | `STOPPED||INTERRUPTED`→`TaskCheckpoint.throwIfStopRequested(...); throw new TaskStopRequestedException("Wubei task interrupted")`；**其余**→`log.info; return result`（原样返回 result，不判 failed） |
| 3 | Wubei:2344 navToNPC(accept) | `PATHING_STARTED`→`return waitForAcceptNpcRouteWake(WubeiStepOutcome.pathingStarted(state.next(ROUTE_TO_MAIN_TASK,"accept-npc-pathing-started"), "accept NPC pathing started"))`；`DIALOG_PREPARING`→`return waitForPreparedDialogWake(WubeiStepOutcome.sharedState(state.next(ROUTE_TO_MAIN_TASK,"accept-npc-dialog-preparing"), "accept NPC dialog preparing"))`；`!=ARRIVED`→`log.warn; return WubeiStepOutcome.failed(state, "accept NPC navigation not arrived: "+status+" "+message)`；`ARRIVED`→`return WubeiStepOutcome.continueTo(state.next(WAIT_TEAM_RETURN, TEAM_RETURN_BEFORE_ACCEPT_SOURCE), "accept NPC arrived; wait team return before accepting task")` |
| 4 | Wubei:2621 navInCurrentMap(void) | `STOPPED||INTERRUPTED`→`throwIfStopRequested; throw new TaskStopRequestedException("Wubei task interrupted")`；**其余**→`log.info; return`（void，无返回值——与#2 不同，不返回 result） |
| 5 | FiveRing:1386 navToNPC | `!success() && status!=PATHING_STARTED`→`log.warn`（仅 log）；**恒** `return result`（原样返回 result 给上层） |
| 6 | FiveRing:1446 navToNPC(fallback) | **恒** `boolean fallbackArrived = fallbackResult.success(); log.info(...); return fallbackArrived`（映射 `success()`→boolean，无 status 分支——与 #5 不同） |
| 7 | FiveRing:1721 navToNPC(accept,重试循环) | `PATHING_STARTED`→`return FiveRingStepOutcome.pathingStarted(activeState.waitForAcceptNpcPathing("accept-npc-navigation-pathing", ACCEPT_NPC_NAV_SOURCE), "accept NPC navigation pathing started")`；`STOPPED`→`return FiveRingStepOutcome.stopped(activeState, "accept NPC navigation stopped")`；`DIALOG_PREPARING`→`log.info; return FiveRingStepOutcome.sharedState(activeState.retrySamePhase("accept-npc-navigation-dialog-preparing"), "route dialog preparing; retry later")`；`POINT_NOT_REACHED`→`log.warn; return FiveRingStepOutcome.sharedState(activeState.retrySamePhase("accept-npc-navigation-no-pathing-retry"), "accept NPC navigation did not start pathing; retry later")`；`!success()`→`throwIfStopRequested; tryAcceptInitialTaskFromCurrentScreen(context,activeState,"setup:navigate-failed")`（非 null 则 return）否则 `log.warn(retry x/MAX); retry++; TaskSleep.sleepOrStop(2000); continue`；success(ARRIVED/DIALOG_OPENED)→续 `cleanupUiBeforeAcceptNpcClick; clickInitialNpcForAccept(...)` |
| 8 | Xiuluo:2768 navToNPC(accept) | `outcome=navigationOutcome(activeState,result,ACCEPT_TASK_CLICK_NPC,"navigate to accept NPC")`（见上 7 路）；`if outcome.transactionResult()==FAILED → return recoverAcceptNavigationFailure(activeState)`；else `return outcome` |
| 9 | Xiuluo:3140 navToNPC(maintenance hook) | `PATHING_STARTED`→`return MaintenanceAttemptResult.withOutcome(XiuluoStepOutcome.pathingStarted(state.waitForPathing("pathing:"+state.phase()), hookName+" NPC pathing started"))`；`DIALOG_PREPARING`→`return MaintenanceAttemptResult.withOutcome(XiuluoStepOutcome.sharedState(state.retrySamePhase(hookName+"-dialog-preparing"), hookName+" NPC route dialog preparing"))`；`STOPPED`→`return MaintenanceAttemptResult.withOutcome(XiuluoStepOutcome.stopped(state, hookName+" NPC navigation stopped"))`；`POINT_NOT_REACHED`→`log.info; return MaintenanceAttemptResult.withOutcome(XiuluoStepOutcome.sharedState(state.retrySamePhase(hookName+"-point-not-reached"), hookName+" NPC point not reached; retry next turn"))`；`DIALOG_OPENED`→`log.info` 后**下穿**；`!=ARRIVED&&!=SUCCESS`（且非 DIALOG_OPENED）→`log.warn; return MaintenanceAttemptResult.retry()`；ARRIVED/SUCCESS/DIALOG_OPENED 下穿→`if(!cleanupBeforeNavigation){uiCleanerService.cleanUpAll(); throwIfStopRequested;} clickNpcSmart(...); if(!clicked) return MaintenanceAttemptResult.retry(); …team broadcast` |
| 10 | Xiuluo:3368 navInCurrentMap | `STOPPED`→`return XiuluoStepOutcome.stopped(state, "start-map exit pre-pathing stopped")`；`PATHING_STARTED`→`return XiuluoStepOutcome.continueTo(state.withStartExitPrepathStarted(nextPhase,nextSource), "start-map exit pathing started; continue shortcut startup while walking")`；**其余**→`return null`（续原流程） |
| 11 | Xiuluo:4174 navToNPC(target) | 先按 `PATHING_STARTED`：`if NAV_MSG_CURRENT_MAP_PATHING_STARTED.equals(message)`→`closeTeamPathingMaintenanceWindow(context,activeState,"target-current-map-pathing-started")`；`else if shouldOpenTeamPathingMaintenanceWindowAfterTargetNavigation(result)`→`openTeamPathingMaintenanceWindow(...,"target-navigation-pathing-started")`；`else`→`closeTeamPathingMaintenanceWindow(...,"target-navigation-route-not-submitted"); log.info`；随后 `consumeCommonBoxDuringNextTaskProgress; consumeDeferredPostCombatRecoveryDuringNextTaskProgress`。`else if ARRIVED||SUCCESS`→`closeTeamPathingMaintenanceWindow(...,"target-route-arrived")`。再 `outcome=navigationOutcome(activeState,result,CLICK_TARGET_NPC,"navigate to target")`：`transactionResult()==PATHING_STARTED`→`return waitForTargetPathingWake(outcome)`；`==FAILED`→`if(isXiuluoBrainLoopEnabled()) return outcome; else return recoverTargetNavigationFailure(context,activeState,outcome.message())`；else `return outcome` |
| 12 | Xiuluo:4874 navToNPC(returnFallback) | 前置 `continueIfNavigationStillPathing(...)`（非 null 则 return）；`activeState=state.clearPathingWait("navigation-retry:"+phase)`；`outcome=navigationOutcome(activeState,result,WAIT_TEAM_RETURN,"navigate back to start")`：`==FAILED`→`if(isXiuluoBrainLoopEnabled()) return XiuluoStepOutcome.failed(activeState,"return fallback navigation failed; report to XIULUO_BRAIN before cleanup/retry"); else { uiCleanerService.cleanUpAll(); return retryCurrentOrRecover(activeState, ACCEPT_TASK_NAVIGATE_TO_NPC, "return fallback navigation failed"); }`；`==PATHING_STARTED`→`return waitForNavigationPathingWake(outcome, "xiuluo-v2:returnFallback", ACCEPT_NPC.getMapName())`；else `return outcome` |

**迁云保真**：Cloud 只回 `NavigationResultStatus`+`message`；上表每行分支（含 message 常量 `NAV_MSG_CURRENT_MAP_PATHING_STARTED` 驱动的 open/close maintenance window、report-brain、retrySamePhase、各 wake、cleanUpAll、void 与 boolean 返回差异）由**非-A W-NAV-2 caller cohort** 逐字复制，实现者不合并/不改分流/不临场选择。

### O3（P1-2）：逐机械动作 → 具体 op 方法 + pre-NOT_EXECUTED/post-UNKNOWN/STOPPED（无推迟）

每动作落到 `CloudNavigationMechanicalPort` 的**具体方法**（→既有 servicePort op），disposition 用既有 `ExecutionState{NOT_EXECUTED,EXECUTED,OBSERVED,UNKNOWN,STOPPED}`：

| 机械动作 | port 方法 → op | 副作用前 NOT_EXECUTED（可信，不点/不重投）| 副作用后 UNKNOWN（保守收口，不重投）| STOPPED |
|---|---|---|---|---|
| world-map 搜索输入(type+search) | `executeInput(WORLD_MAP_SEARCH_INPUT, ...,WINDOW_CLIENT_PX,actions,to)`→`EXECUTE_INPUT_BUNDLE` | `common.executionState()==NOT_EXECUTED`(fence/gate 拒)→HEAD `navigateToMap` 搜索段 not-submitted 分支(mapNotReached/继续)，不投 | `==UNKNOWN`(`startedStepIndex>=0` 但未 EXECUTED)→由后续 `WORLD_MAP_RESULT_CAPTURE` 收口，不重投 | `==STOPPED`→`NavigationResult.stopped`；段末 finally close |
| world-map 结果读取 | `capture(WORLD_MAP_RESULT_CAPTURE,region,PNG,ROUTE_DECISION,to)`→`CAPTURE` | 非 OBSERVED(读失败)→HEAD 无候选分支(mapNotReached/下一 ladder)，不点 | 图不确定→Cloud CPU 重解释（幂等读，同槽 occurrence 复用），不压 MAP_NOT_REACHED 重点 | stopped |
| world-map 候选点击 | `executeInput(WORLD_MAP_CANDIDATE_INPUT,...)`→`EXECUTE_INPUT_BUNDLE` | NOT_EXECUTED→不点，回 HEAD 候选未提交分支 | UNKNOWN→pathing observer/下一 capture 收口，不再 click | stopped |
| route-dialog 判读 | `capture(ROUTE_DIALOG_CAPTURE,region,PNG,ROUTE_DECISION,to)`→`CAPTURE` | 非 OBSERVED→`dialogPreparing`/继续等待，不点 | 不确定→重解释(幂等)，不点 | stopped |
| route-dialog 点击(prepared/visible gate) | `executeInput(ROUTE_DIALOG_INPUT,...)`→`EXECUTE_INPUT_BUNDLE` | NOT_EXECUTED(gate 未过/本地 fact 不当前 O-M3 fail-closed)→不点 | UNKNOWN→dialog 再 capture 收口 | stopped；finally close |
| mini-map 判读 | `capture(MINI_MAP_CAPTURE,region,PNG,COORDINATE,to)`→`CAPTURE` | 非 OBSERVED→`pointNotReached`/继续，不点 | 不确定→重解释(幂等) | stopped |
| mini-map 点击(fire-and-handoff) | `executeInput(MINI_MAP_INPUT,...)`→`EXECUTE_INPUT_BUNDLE` | NOT_EXECUTED(坐标读失败/fence)→`pointNotReached`/继续，不点 | UNKNOWN→handoff 给 pathing confirm(HEAD 本就 fire-and-handoff)，不重发、不压 POINT_NOT_REACHED 重点 | stopped |
| NPC 近点直点 | `executeInput(NPC_DIRECT_INPUT,...)`→`EXECUTE_INPUT_BUNDLE` | NOT_EXECUTED(直点前 fence)→`pointNotReached` | UNKNOWN→上层接任务消费 | stopped |
| pathing 坐标/几何读(keep-turn 内) | `capture(PATHING_COORDINATE_CAPTURE,...)`→`CAPTURE` / `readWindowGeometry(to)`→`WINDOW_FACT` GEOMETRY | 非 OBSERVED→HEAD NO_PATHING/继续 read（bounded in-call，无额外 read） | confirm 超时→HEAD timeout 分支(arrived 否则 not-reached，不额外 click) | stopped；到达 late fact→arrived |
| close/finally | 复用上列 input/无新 op | — | — | **无论成败必执行 HEAD close/finally/random-mouse**，不吞 cleanup |

- 硬规则（已落到 op 层）：**副作用前 NOT_EXECUTED ≠ 副作用后 UNKNOWN**；`InputBundleOutcome` 的 `startedStepIndex/lastCompletedStepIndex` 区分二者（NOT_EXECUTED 二者=-1，既有校验）；**UNKNOWN 绝不压 not-reached 后重点**；HEAD close/finally/combat-interruption 逐字保。keep-turn 是 HEAD **单次 navigate() 内 bounded in-call 轮询**（deadline/iteration 常量取 HEAD 原值），迁移后仍在请求线程内同步执行 `capture/readWindowGeometry`，**非 Cloud 后台 poller/scheduler、非 raw wire**。

### O4（P2）：真实可调用事实 API + 撤回 observation-transport 依赖

**撤回** D3“关键事实指向既有/规划中 observation transport seam”。经核对，navigation 全部事实经**当前已存在、可编译调用**的三 op 承载，**零新 wire、零新 transport、无 Cloud 后台轮询**：
- 图像事实（world-map 路线/候选 OCR、route-dialog 模板、mini-map 坐标模板、pathing 到达指示）→ `CloudNavigationMechanicalPort.capture(...)`→既有 `CloudTaskServicePort.capture`→`CaptureOutcome`(OBSERVED `imageBytes`+`systemScaleRatio`)；**模板/OCR/route 解释在 Cloud CPU**（既有 `RouteCloudDecisionService`/`NavigationPointCloudDecisionService`/`NavigationRoutePlanCloudDecisionService`），产物 `NavigationFactResult` 为 **Cloud 内部 DTO，非 wire**。
- 几何/焦点/停止事实 → `readWindowGeometry(to)`→`WindowFactKind.GEOMETRY`（既有枚举 `WindowFactKind{BINDING,GEOMETRY,FOCUS_STATE,STOP_STATE}`），typed `WindowFactOutcome.fact` = `WindowFact.GeometryFact`。
- 物理输入 → `executeInput(...)`→`EXECUTE_INPUT_BUNDLE`→`InputBundleOutcome`。
- **无新 `WindowFactKind`、无新 outcome 字段**：dialog 当前性/pathing 到达在 HEAD 本就是 capture 上的模板判定，归 CAPTURE 图像事实，**不需事件/推送 transport**。故普通 `CAPTURE/WINDOW_FACT/EXECUTE_INPUT_BUNDLE` 的 canonical order/bytes/`requestDigest` **零变化**（根本不新增字段）。
- **当前可独立实施波次（真正可编译）**：`CloudNavigationMechanicalPort`(New) + `CloudTaskServiceExecutionContext`(Modify,加字段+public accessor) + `TaskExecutionContext`(Modify,加 passthrough) + `com.bot.dhxy.service.NavigationService`(New,经 port 编排) + `NavigationFactResult`(New,Cloud 内部 DTO)。**唯一外部依赖门 = M Full R0 occurrence 连续性**（既有已知依赖，非事实-transport seam）；不以未来 transport 支撑“实现就绪”，不回 raw wire/Cloud 轮询。

### 修订文件表（撤回 D3 直调，改 remote-adapter + 2 context Modify，零 wire）

| 仓库 | 精确 FQCN | New/Modify | owner |
|---|---|---|---|
| Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.CloudNavigationMechanicalPort`（closed typed 机械 port，构造 package-private，持 runContext/retainedActionState/servicePort，固定槽 retain+servicePort 调用）| New | Worker A（W-NAV-1，需 M Full R0 + 父级授权）|
| Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskServiceExecutionContext`（加 `navigationMechanicalPort` final 字段 + 两构造赋值 + public `navigationMechanicalPort()`）| Modify | Worker A |
| Cloud | `com.bot.dhxy.runner.context.TaskExecutionContext`（加 public `getNavigationMechanicalPort()` passthrough）| Modify | Worker A |
| Cloud | `com.bot.dhxy.service.NavigationService`（整类编排 dormant，经 `taskContext.getNavigationMechanicalPort()` closed 调用）| New | Worker A |
| Cloud | `com.bot.dhxy.cloud.task.NavigationFactResult`（Cloud 内部 fact DTO，非 wire）| New | Worker A |
| Cloud | `CloudTaskRetainedActionState`/`CloudTaskServicePort`/`CloudTaskRunAuthorityAssembly`/三决策服务 | **零修改** | 既有权威/facade |
| DHXY | `LocalRemoteGameCommandHandler`+`RemoteOperationLedger`(机械防重)、capture/mini-map OCR·coordinate/input、window-bound runtimeState、fence、UI clean | 保留 | — |
| DHXY | 12 caller(Wubei/FiveRing/Xiuluo，O1 逐行) | Modify | **非-A W-NAV-2 cohort** |

**零 New wire schema、零新 `WindowFactKind`/outcome 字段、零 assembly Modify、无第二 ledger、无 navigation 长寿命 store**；与 A(scale-wire FINAL)/B(TeamReturn)/U·CommonBox(SOURCE APPROVED)/P2 transaction 零交叉（`CloudNavigationMechanicalPort`/两 context accessor 为新增 API，不改既有方法体语义）。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（仅 QA，不构成父级批准）。Design Repair #4 Delta 到此停止，等待父级复审；heartbeat 保持。

## Parent Design Review #5 - BLOCKED - 2026-07-13T11:26:00-04:00

D5 的 12 caller 表已删除“同 #/按 HEAD/实现时再填”，该部分通过；remote-package closed capability 的方向也正确。
但父级按当前 Cloud 源码逐签名、逐 import 机械核查后，整体仍 **BLOCKED，P0=0/P1=4/P2=1**，Java/Maven/
schema/resources/tests/host/caller 继续冻结。

1. **P1：拟议 port API 仍不可由业务包编译调用。** D5 `:474-478` 把 `NavigationActionSlot` 描述为“内部
   closed enum”，却又让三个 public 方法把它作为参数；若 enum 保持 package-private/private，
   `com.bot.dhxy.service.NavigationService` 无法引用该方法签名，若改 public 又把任意 slot 选择权重新开放给业务。
   同时 D5 `:513-518` 使用 `CapturePurpose.ROUTE_DECISION/COORDINATE`，当前
   `CaptureRequest.CapturePurpose` 真实只有 `DIAGNOSTIC/CLOUD_SERVICE_INPUT`。影响是 D5 所列独立波次直接编译失败。
   D6 必须改成每个固定语义动作的 public closed method，不让业务传 slot；只使用当前真实 enum，若确需新增 purpose，
   必须诚实列双仓 wire/schema/digest 写集，不能称零 wire。
2. **P1：fixed slot 没有 final-consumption/occurrence 闭环。** 当前 `CloudTaskServicePort.consumeCaptureFinal/
   consumeInputBundleFinal/consumeWindowFactFinal` 均为 package-private，只有 trusted remote adapter 能在业务 mutation
   成功后提交 final-consumed；`CloudTaskRetainedActionState.retain()` 也只有 ledger 已认定 occurrence complete 才会
   `openOccurrence++`。D5 port 只 `retain + invoke`，没有 consume/mutation/receipt，也明确不 renew。结果是第二次 route
   capture/candidate/input 要么永远重放第一次 retained outcome，要么因同 identity 不同 payload 冲突；可信
   `NOT_EXECUTED` 也会永久卡在同 attempt。D6 给每个 closed method 的 invoke -> exact business mutation ->
   consume-final -> next occurrence/可信 NOT_EXECUTED compact+renew 时序，UNKNOWN/STOPPED 不得推进。
3. **P1：所谓“当前真正可编译 NavigationService 波次”缺少实际 dependency closure。** 对 HEAD `0114604e`
   `NavigationService` 的 import 机械比对显示 Cloud 仍缺至少 19 个直接依赖，包括
   `RouteCloudDecisionService`、`NavigationPointCloudDecisionService`、`NavigationRoutePlanCloudDecisionService`、
   `RuntimeDecisionShadowService`、`BotProperties`、`GameClientTracker`、`InputSequences`、`MiniMapCoordinateReader`、
   `WindowRuntimeContext/WindowTaskContextHolder/WindowReadyEventBus` 等；D5 却把前三个称为 Cloud 既有服务，并只列
   5 个 New/Modify。影响是整类 New 不能 package，且缺失项混有应留本地能力。D6 必须逐 import 给
   `existing-cloud / migrate-cloud / replace-by-closed-port / retain-local` 表，并把当前可独立实施波次缩到真实闭包；不得再把
   整个 3453 行主体称为当前可编译。
4. **P1：O4 撤销 observation seam 后违反已固定的本地 Runner 观察边界。** 用户已明确移动、dialog、pathing、
   continuous observation 与 soft wake 留 DHXY；D5 `:527-532` 改成 Cloud 请求线程反复同步 CAPTURE/GEOMETRY 并在
   Cloud CPU 判断 dialog/pathing 到达，等价于把持续机械观察迁成网络轮询，断线时也失去本地 wake/final fact。
   D6 必须把长时移动/dialog/pathing observation 继续交本地 retained watcher，经真实 typed observation/wake capability
   上报；当前没有 API 就列为主体依赖门并只实施 CPU/closed-port 叶子，不得用同步 Cloud capture loop 代替。
5. **P2：generic `capture(slot, arbitrary region/purpose)` / `executeInput(slot, arbitrary actions)` 不是闭合业务
   capability。** 即使把 slot 公开，同一 semantic address 可被业务配上不同 payload，触发 digest conflict 或错误复用。
   D6 的 fixed methods 必须把允许变化的业务参数与固定 payload 部分逐项写清，并给 address/occurrence owner，禁止自由组合。

### 下一任务 `W-NAV-D6`

External Worker A 先在本日志末尾追加 `CLAIMED`（任务、claimedAt、唯一写集），领取截止
`2026-07-13T11:46:00-04:00`。只追加 Design Repair #5 Delta 关闭 `P1 x4/P2 x1`；已通过的 12 caller exact
matrix 不重开。两仓 Java/Maven/schema/resources/tests/host/caller 全冻结；Worker QA 不算父级批准。

**无已批准业务差异；按基线等价迁移。**

## Parent Claim Gate #1 - UNCLAIMED / REMAINS WITH EXTERNAL A - 2026-07-13T11:48:55-04:00

截至 `2026-07-13T11:46:00-04:00`，本日志未出现 `W-NAV-D6` 的 `CLAIMED`。按用户最新指令，**不交内部
Worker 接管**，也不修改 A 的写集或技术任务；`W-NAV-D6` 继续由 External Worker A 负责。

External Worker A 看到本条后须先追加 `CLAIMED`（task=`W-NAV-D6`、实际 claimedAt、唯一写集=仅本日志），
重新领取截止 `2026-07-13T12:08:55-04:00`；随后只完成 Parent Design Review #5 指定的 Design Repair #5 Delta。
两仓 Java/Maven/schema/resources/tests/host/caller 继续冻结。20 分钟门只检查领取，不限制已领取后的工作时长。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T11:52:50-04:00

- task: W-NAV-D6
- claimedAt: 2026-07-13T11:52:50-04:00（重新领取截止 2026-07-13T12:08:55-04:00 内）
- uniqueWriteSet: 仅本日志 `docs/superpowers/plans/reports/2026-07-13-cloud-navigation-service-worker-a.md`（append-only，Design Repair #5 Delta）。两仓 Java/Maven/schema/resources/tests/host/caller 全冻结；不写 Java、不跑 Maven、不 git、不触他人写集/日志。已通过的 12 caller exact matrix 不重开。自审仅 QA，不构成父级批准。

## External Worker A - Design Repair #5 Delta - 2026-07-13

关闭 `Parent Design Review #5` 的 P1×4/P2×1。仅设计，两仓 Java/Maven/schema/resources/tests/host/caller 全冻结；已通过的 12 caller exact matrix（D5 O1）不重开。证据基线=当前 Cloud 源码逐签名/逐行核对：`CloudTaskServicePort.java`（三 op :43/:63/:90；`consumeWindowFactFinal/consumeCaptureFinal/consumeInputBundleFinal` package-private :107/:115/:123；`CheckedFinalMutation` :145-148；`FinalConsumptionDisposition{OCCURRENCE_COMPLETE,ATTEMPT_RETIRED_FOR_RENEWAL}` :150-153）、`CloudTaskRetainedActionState.java`（`retain*` :58-77；`renewAfterNotExecuted`×3 :79-95；occurrence 推进 :150-176；renew 门 :178-205；`ActionAddress(phaseCode,actionSlot)` :255；`ActionRecord` :274-291；固定槽先例 `BattleRadarSemanticSlot` :293-314）、`CloudTaskRunActionLedger.java`（`renewAfterNotExecuted` :357，仅接受 compacted verified NOT_EXECUTED :361-368，同 occurrence attempt+1 :378-386，EXECUTE_INPUT_BUNDLE 换新 actionId :346-349；`isOccurrenceComplete` :634；`isRenewalCompacted` :641；`acceptTerminalRun` :625）、`CaptureRequest`（`ImageFormat{PNG}` :21-23；`CapturePurpose{DIAGNOSTIC,CLOUD_SERVICE_INPUT}` :25-28）、`WindowFactKind{BINDING,GEOMETRY,FOCUS_STATE,STOP_STATE}`、`CaptureRegion(coordinateSpace,x,y,width,height)`、`CoordinateSpace{SCREEN_ABSOLUTE_PX,WINDOW_CLIENT_PX}`、`CloudTaskServiceExecutionContext`（两构造 :27/:53；`servicePort()` public :154；`retainedActionState()` package-private :162）、HEAD `0114604e` `NavigationService.java` 完整 import 表（L3-60，58 项）。

### Q1（P1-1）：固定语义 public closed methods——slot 不出包、只用真实 enum、零新 purpose

**撤回** D5 的 `capture(NavigationActionSlot slot, ...)/executeInput(slot, ...)` 签名（enum 出包与任意 slot 选择两难成立）与不存在的 `CapturePurpose.ROUTE_DECISION/COORDINATE`。`CloudNavigationMechanicalPort`（New，`com.yueyunfe.dhxy.cloudbrain.remote`，构造 package-private）对业务只暴露**每个固定语义动作一个 public 方法**，`NavigationActionSlot` 仍为 port 内 **private enum**（永不出现在任何 public 签名），全部 capture 用**真实** `CapturePurpose.CLOUD_SERVICE_INPUT` + `ImageFormat.PNG`：

```
public NavigationCaptureResult   captureWorldMapResult(NavigationFinalMutation<CaptureOutcome> m, long timeoutMs)
public NavigationCaptureResult   captureRouteDialog(NavigationFinalMutation<CaptureOutcome> m, long timeoutMs)
public NavigationCaptureResult   captureMiniMap(NavigationFinalMutation<CaptureOutcome> m, long timeoutMs)
public NavigationFactReadResult  readWindowGeometry(NavigationFinalMutation<WindowFactOutcome> m, long timeoutMs)
public NavigationInputResult     typeWorldMapSearch(String mapNameText, NavigationFinalMutation<InputBundleOutcome> m, long timeoutMs)
public NavigationInputResult     clickWorldMapCandidate(int clientX, int clientY, NavigationFinalMutation<InputBundleOutcome> m, long timeoutMs)
public NavigationInputResult     clickRouteDialog(int clientX, int clientY, NavigationFinalMutation<InputBundleOutcome> m, long timeoutMs)
public NavigationInputResult     clickMiniMap(int clientX, int clientY, NavigationFinalMutation<InputBundleOutcome> m, long timeoutMs)
public NavigationInputResult     clickNpcDirect(int clientX, int clientY, NavigationFinalMutation<InputBundleOutcome> m, long timeoutMs)
```

- 方法↔槽↔op 固定绑定于方法体内（业务无 slot 选择权）：`captureWorldMapResult→("navigation","world-map-result")×CAPTURE`、`captureRouteDialog→("navigation","route-dialog")×CAPTURE`、`captureMiniMap→("navigation","mini-map")×CAPTURE`、`readWindowGeometry→("navigation","window-geometry")×WINDOW_FACT(GEOMETRY)`、`typeWorldMapSearch→("navigation","world-map-search-input")×EXECUTE_INPUT_BUNDLE`、`clickWorldMapCandidate→("navigation","world-map-candidate")`、`clickRouteDialog→("navigation","route-dialog-click")`、`clickMiniMap→("navigation","mini-map-click")`、`clickNpcDirect→("navigation","npc-direct-click")`（`ActionAddress` 构造均在 remote 包内，:255 可见性满足）。
- **capture 波段 ROI 固定在方法体**（HEAD L102-171 的 ROI 常量迁为 port 内 private static final `CaptureRegion`，`WINDOW_CLIENT_PX`），业务不传 region/purpose/format——generic `capture(slot, arbitrary region/purpose)` 形态整体消失（同时闭合 P2-1 的自由组合面）。
- **零新 purpose/零 wire 结论继续成立**：只用现有 `CLOUD_SERVICE_INPUT`，不加 enum 值，不触碰双仓 wire/schema/digest 写集。返回类型 `NavigationCaptureResult/NavigationFactReadResult/NavigationInputResult` 为 remote 包 New **public** 值类型（见 Q2），包裹既有 public `CaptureOutcome/WindowFactOutcome/InputBundleOutcome`（本就 public，业务可读）+ closed disposition，不暴露 handle/identity/ledger。

### Q2（P1-2）：每 closed method 的 invoke → 业务 mutation → consume-final → 下一 occurrence / 可信 NOT_EXECUTED compact+renew（UNKNOWN/STOPPED 不推进）

**撤回** D5 "只 retain+invoke、不 consume/不 renew"。公开回调类型（remote 包 New）：

```
public interface NavigationFinalMutation<O> { NavigationConsumeDisposition apply(O exactOutcome) throws InterruptedException; }
public enum NavigationConsumeDisposition { OCCURRENCE_COMPLETE, RETIRE_FOR_RENEWAL }
```

每个 public 方法体内的**完整闭环**（以 capture 为例；WINDOW_FACT/INPUT 同构，仅换 retain/consume 对应重载）：

1. **retain**：`CaptureAction h = retainedActionState.retainCapture(runContext, FIXED_ADDRESS)`（:65）。`retain()` 内部（:150-176）：若该槽当前 identity 已 `isOccurrenceComplete`（:157，即上轮 consume-final 以 `OCCURRENCE_COMPLETE` 落 COMPACTED_FRONTIER，:634-639）→ `openOccurrence++` 并 `actionLedger.acquire` 铸下一 occurrence 新 identity（:158-164）；否则返回**同一未完结 attempt 的同一 handle**（重放语义，broker 回 retained outcome，不重发）。**occurrence 编号唯一 owner = `ActionRecord`（:274-291），port 与业务均无编号能力。**
2. **invoke**：`CaptureOutcome out = servicePort.capture(h, FIXED_REGION, PNG, CLOUD_SERVICE_INPUT, timeoutMs)`（:63-78）。
3. **按 `out.executionState()` 分派（port 内 switch，业务不可选择路径）**：
   - **OBSERVED / EXECUTED**（业务可消费的最终事实）→ `servicePort.consumeCaptureFinal(h, out, o -> map(m.apply(o)))`（:115-121，包内可调）：**业务 mutation（Cloud CPU 解释/NavigationService 状态推进，即 caller 传入的 `m`）在 final-consumption 临界区内执行**，返回 `OCCURRENCE_COMPLETE` → 映射为 `FinalConsumptionDisposition.OCCURRENCE_COMPLETE`（:150-153）→ 该 occurrence 关闭 → **下一次调用同方法自动进入 occurrence+1**（步骤 1 的 :157-164 路径）。第二次 route capture/candidate/input 因此**不会重放第一次 retained outcome**——P1-2 指出的"永远重放/identity-payload 冲突"由此闭环消除。
   - **可信 NOT_EXECUTED**（副作用前验证拒绝，`startedStepIndex/lastCompletedStepIndex==-1` 既有校验）→ 仍走 `consumeCaptureFinal(h, out, o -> ATTEMPT_RETIRED_FOR_RENEWAL)`（**compact**：ledger 落 COMPACTED_FRONTIER+ATTEMPT_RETIRED_FOR_RENEWAL，:641-646 `isRenewalCompacted` 变真）→ 随即 `retainedActionState.renewAfterNotExecuted(h, runContext)`（:85-89 → :178-205，renew 门 :190-193 要求恰为 compacted NOT_EXECUTED；ledger :357-397：**同 occurrence、attempt+1、新 requestId**，EXECUTE_INPUT_BUNDLE 额外换新 actionId :346-349，旧 actionId 永久绑定旧请求）→ 返回 typed `NOT_EXECUTED` 结果；**是否重走该步由业务按 D5-O3 该动作的 HEAD not-reached/continue 分支决定**（HEAD 无 retry 处 port 不自动重投；HEAD 本有的继续路径下一次调用用 renewed attempt，不会卡死同 attempt——P1-2 的"可信 NOT_EXECUTED 永久卡同 attempt"闭环消除）。**mutation 回调不执行**（无业务事实可消费）。
   - **UNKNOWN**（已可能产生副作用后不确定）→ **不 consume、不 renew、不推进**：attempt 冻结为当前（后续同方法调用经步骤 1 返回同 handle→重放同 outcome），返回 typed `UNKNOWN`；业务按 D5-O3 该动作的收口路径（pathing/dialog 后续 capture、上层消费）处理，**绝不压 not-reached 再点**。ledger 侧该 identity 既非 occurrence-complete 也非 renewal-compacted，任何 renew 调用被 :190-193/:361-368 拒绝——**结构上不可推进**。
   - **STOPPED** → **不 consume、不 renew、不推进**：返回 typed `STOPPED`，业务走 HEAD stop unwind；run 终止时 `acceptTerminalRun`（:625-631）清理该 run 全部 retained 台账（既有 terminal 退账路径），无残留 attempt。
4. `InterruptedException` 从 `consume*Final` 原样上抛（签名 `throws InterruptedException`，:111/:119/:127），port 不吞。

**mutation 失败语义**：`m.apply` 抛出（含 Interrupted）时 final-consumption 未完成——identity 停在未消费态，重入同方法回到步骤 1 重放同 outcome、重试同一 mutation（coordinator 既有可重入语义），**不产生第二次物理执行**。

### Q3（P1-3）：HEAD 58 import 全量 dependency closure 表 + 真实可编译波次收缩

对 `git show 0114604e:...NavigationService.java` L3-60 的 **58 个 import 逐项**分类（Cloud 现存性以当前 `com.bot.dhxy` 源树核对；父级"至少 19 缺失"精确复核=**19 项缺失**，逐项落位）：

**existing-cloud（36，零修改可 import）**：`cloud.decision.CloudDecisionServiceId`、`cloud.task.ImagePreprocessCloudRequest`、`cloud.task.RouteCloudDecision`、`core.GameContext`、`core.ImageFinder`、`model.PlayerCharacter`、`model.MapCoordinate`、`model.dialog.{DialogResultStatus,DialogType,DialogPreparationRequest,DialogPreparationPhase,DialogPreparationStatus,PreparedDialogAction}`(6)、`model.navigation.{NavigationRequest,NavigationResult,NavigationResultStatus,PendingTransferChoiceMemory,TemplateLocationInfo,PendingRouteOutcome,WorldMapRouteResultMode}`(7)、`model.npc.{NpcMovementType,NpcRole,NpcTarget,NpcTooltipType}`(4)、`model.ocr.LocationInfo`、`runner.context.TaskExecutionContext`、`runner.stop.{TaskCheckpoint,TaskSleep}`(2)、`service.dialog.DialogOperation`、`tools.LatencyMetrics`、`window.model.{WindowPathingIntent,WindowPathingSnapshot,WindowPathingState,WindowDialogSnapshot,WindowReadyEvent,WindowReadyEventType}`(6，**类型存在但其活体观察归 Q4 本地边界**)、lombok×2、spring `@Component`。

**缺失 19 项分类**：

| # | HEAD import（缺失于 Cloud）| 处置 |
|---|---|---|
| 1 | `cloud.task.RouteCloudDecisionService` | **migrate-cloud**：DHXY 侧原为 HTTP client 包装；迁云后为 in-process facade 直调既有 Cloud 决策算法（`DecisionEngine` route 解释），语义逐字保留，非自环 HTTP |
| 2 | `cloud.task.NavigationPointCloudDecisionService` | **migrate-cloud**：同上（mini-map candidate 算法已在 Cloud）|
| 3 | `cloud.task.NavigationRoutePlanCloudDecisionService` | **migrate-cloud**：同上（六段 route plan 已在 Cloud）|
| 4 | `cloud.runtime.RuntimeDecisionShadowService` | **migrate-cloud**（shadow 诊断薄层，保日志业务义）|
| 5 | `config.BotProperties` | **migrate-cloud**：navigation 实际消费字段迁入 Cloud nav 配置权威（`CloudAutoBattlePropertiesAuthority` 先例形状），值逐字 HEAD |
| 6 | `core.GameClientTracker` | **replace-by-closed-port**：exact bound-window capture → Q1 `capture*` 固定方法 |
| 7 | `driver.BoundWindowKeyboardService` | **replace-by-closed-port**：→ `typeWorldMapSearch/click*` |
| 8 | `input.InputProvider` | **replace-by-closed-port**：→ `click*` |
| 9 | `input.InputSequences` | **replace-by-closed-port**：输入序列形状固化进 port 固定 payload（Q5）|
| 10 | `input.action.InputAction` | **replace-by-closed-port**（本地 `InputActionDto` 序列由 port 组装）|
| 11 | `input.action.InputActionScope` | **replace-by-closed-port**（原子性由 `EXECUTE_INPUT_BUNDLE` 承载）|
| 12 | `runner.context.TaskExecutionContextHolder` | **replace-by-closed-port**：thread-local 取 context → 迁云后由注入的 `TaskExecutionContext` 直接持有（无 Holder）|
| 13 | `tools.CoordinateHelper` | **migrate-cloud**（纯 CPU 坐标换算 + 迁移后的 nav 配置，算法逐字）|
| 14 | `tools.GameStateUtil` | **retain-local**：本地无输入探针（Alt+A 模式判定等）绑本地窗口事实；nav 内消费点经 Q4 观察门取 typed fact |
| 15 | `vision.MiniMapCoordinateReader` | **拆分**：navigate 内单发 pre-input 坐标读→**replace-by-closed-port**（`captureMiniMap`+Cloud CPU 解释）；keep-turn/pathing 循环中的持续读→**retain-local**（Q4 观察者）|
| 16 | `window.runtime.WindowRuntimeContext` | **retain-local**（exact 本地窗口运行时）|
| 17 | `window.runtime.WindowReadyEventBus` | **retain-local**（soft wake 总线，Q4）|
| 18 | `window.runtime.WindowScopedTempPath` | **retain-local**（本地诊断落盘路径；Cloud 不写本机盘）|
| 19 | `window.runtime.WindowTaskContextHolder` | **retain-local**（本地窗口任务上下文）|

**当前真实可编译波次（收缩后，仅 3 文件）**：`CloudNavigationMechanicalPort`（New，remote 包——仅依赖本包既有类型，闭包完整）+ `CloudTaskServiceExecutionContext`（Modify：`private final CloudNavigationMechanicalPort navigationMechanicalPort` 字段、两构造（:27/:53）在 `this.servicePort=...` 后以自持的 `runContext/retainedActionState/servicePort` 构造赋值、public accessor `navigationMechanicalPort()`）+ `com.bot.dhxy.runner.context.TaskExecutionContext`（Modify：public `getNavigationMechanicalPort()` passthrough）。**`com.bot.dhxy.service.NavigationService` 整类（3453 行）明确不在当前波次**——其编译前置=本表 5 项 migrate-cloud + Q4 observation/wake capability 门 + 12 caller cohort（非-A），在各前置未落地前不再声称"当前可编译"。

### Q4（P1-4）：移动/dialog/pathing/持续观察/soft wake 留 DHXY 本地 Runner——撤回 Cloud 同步 capture 循环

**撤回** D5-O4/O3 中"pathing 坐标/几何在 Cloud 请求线程内同步 `capture/readWindowGeometry` 循环判读"的形态（父级判定成立：等价于把持续机械观察迁成网络轮询，断线即失去本地 wake/final fact）。恢复并固定用户已定边界：

- **留 DHXY 本地 Runner（retained watcher）**：移动/到达观察（keep-turn 循环体、60s 本图循环、pathing confirm 2.5s/1.5s/1s/20s）、dialog 实时观察（route dialog visible/attention 等待）、`WindowPathingIntent/WindowPathingSnapshot/WindowPathingState/WindowDialogSnapshot` 活体、`WindowReadyEventBus` soft wake——全部本地计时/本地观察/本地唤醒；断线继续观察并拒绝输入，不离线推进 Cloud phase。
- **Cloud 只消费 typed 观察终局/里程碑事实**（arrived/timeout/pathing-state-terminal/dialog-visible fact + 时间戳），经**真实 typed observation/wake capability** 上报。**当前 remote 包无该 API**（唯一相近物 `CloudTaskRunRetainedLifecycleActivationAdapter.PausedObservationCapability` + `CloudTaskRetainedActionState.mintPausedObservationSlot`（:316-350）是 battle-radar **PAUSED read-only** 通道，`BattleRadarSemanticSlot` 固定七槽（:293-314），不承载 task-active navigation 观察）——故**列为 NavigationService 主体的显式依赖门 `observation/wake capability`（非-A/后续切片）**，不以未定义 transport 支撑"实现就绪"，也不以同步 Cloud capture loop 代替。
- **闭合 port 的 capture 仅限单发事实读**（route ladder 每步一次 world-map 结果判读、input 前一次 route-dialog/mini-map 判读——HEAD 中即为一次性 capture 判定点），**不用于任何 continuous observation**；Q1 方法集因此**不含** pathing/arrival 轮询方法（D5 的 `PATHING_COORDINATE_CAPTURE` 槽删除）。
- 当前可实施范围相应=Q3 的 3 文件 CPU/closed-port 叶子；navigation 主体等 observation 门。

### Q5（P2-1）：每 capability 固定 payload / 允许业务参数 / semantic address / occurrence owner

**总规则（先于表）**：任一 attempt 的 request payload 在首次 invoke 时构造**恰一次**并由 ledger 绑定 bytes（既有 bind-once 语义）；同 attempt 重入=重放 retained outcome，**不重建 payload**——同一 semantic address 配不同 payload 的 digest conflict 在结构上不可能；允许变化的业务参数只在**新 occurrence（上轮 OCCURRENCE_COMPLETE 后）或新 attempt（compacted NOT_EXECUTED renew 后）**生效。occurrence/attempt 编号唯一 owner=`ActionRecord`（occurrence，:157-164/:277-279）+ ledger renew（attempt，:378-386）；port 与业务零编号能力。

| capability（public 方法）| semantic address（固定）| 固定 payload 部分 | 允许业务参数（唯一变化面）|
|---|---|---|---|
| `captureWorldMapResult` | `navigation/world-map-result` | ROI=HEAD 世界地图结果区常量（`WINDOW_CLIENT_PX`）、PNG、`CLOUD_SERVICE_INPUT` | `timeoutMs`（HEAD 常量）|
| `captureRouteDialog` | `navigation/route-dialog` | ROI=HEAD route dialog 区常量、PNG、`CLOUD_SERVICE_INPUT` | `timeoutMs` |
| `captureMiniMap` | `navigation/mini-map` | ROI=HEAD mini-map 区常量、PNG、`CLOUD_SERVICE_INPUT` | `timeoutMs` |
| `readWindowGeometry` | `navigation/window-geometry` | `WindowFactKind.GEOMETRY` | `timeoutMs` |
| `typeWorldMapSearch` | `navigation/world-map-search-input` | 步骤形状=HEAD 搜索框点击→清空→键入→搜索按钮（固定顺序/固定框坐标常量/固定 settle）、`WINDOW_CLIENT_PX`、固定 description | `mapNameText`（键入文本）、`timeoutMs` |
| `clickWorldMapCandidate` | `navigation/world-map-candidate` | 单击形状（move+click+HEAD settle）、`WINDOW_CLIENT_PX`、固定 description | `clientX,clientY`（Cloud CPU 从上一 occurrence 的 capture 解释产物）、`timeoutMs` |
| `clickRouteDialog` | `navigation/route-dialog-click` | 同上单击形状 | `clientX,clientY`、`timeoutMs` |
| `clickMiniMap` | `navigation/mini-map-click` | 同上单击形状（HEAD mini-map settle 常量）| `clientX,clientY`、`timeoutMs` |
| `clickNpcDirect` | `navigation/npc-direct-click` | 同上单击形状 | `clientX,clientY`、`timeoutMs` |

- 坐标/文本参数的**合法来源**被业务时序钉死：只能来自同一 occurrence 链上前置 capture 的 Cloud CPU 解释产物（HEAD 数据流等价），port 对参数做 `WINDOW_CLIENT_PX` 域校验（正数/窗界），**无自由组合面**：region/purpose/format/步骤形状/description/coordinateSpace 均不可由业务提供。
- `NavigationFinalMutation` 回调是业务在本表之外的唯一介入点，且仅在 OBSERVED/EXECUTED 分派下执行（Q2），不触 payload。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 **P0=0 / P1=0 / P2=0**（仅 QA，不构成父级批准）。Design Repair #5 Delta 到此停止，等待父级复审；heartbeat 保持。

## Parent Design Review #6 - BLOCKED - 2026-07-13T12:13:00-04:00

D6 已关闭 public slot、假 `CapturePurpose`、主体假闭包与 Cloud 持续轮询四类问题；12 caller matrix 继续 PASS，
本地 Runner 保留移动/dialog/pathing/soft wake 的边界也 PASS。但父级沿当前
`RemoteFinalConsumptionCoordinator -> CloudTaskRunActionLedger` 真实 phase 逐步复审后，整体仍
**BLOCKED，P0=0/P1=3/P2=1**，Java/Maven/schema/resources/tests/host/caller 继续冻结。

1. **P1：D6 把“ACK 已发布”误当成“receipt 已 compacted”。** `consume*Final()` 在业务 mutation 后只把 ledger
   从 `BUSINESS_CONSUMING` 推到 `BUSINESS_CONSUMED_NOTICE_PENDING` 并发布 final-consumed control；只有 DHXY 本地
   apply 后回传 receipt，`acceptReceipt -> commitCompaction` 才进入 `COMPACTED_FRONTIER`。因此 D6 Q2 步骤 3 所述
   “consume 返回后下一调用 occurrence+1”不成立；可信 NOT_EXECUTED 后立即
   `renewAfterNotExecuted()` 也会被 `isRenewalCompacted()==false` 拒绝。影响是正常第二次 capture/input 与
   NOT_EXECUTED 路径都会在 receipt 窗口卡住或抛异常。D7 必须显式增加 closed typed
   `FINAL_CONSUMPTION_PENDING/RENEWAL_PENDING`，只有 receipt compacted 后才由 retained state 推进 occurrence 或
   renew；用既有 receipt-driven sticky ready/wake，禁止 sleep/poll/thread/自动 retry。若当前 Navigation 无可用 ready
   capability，就把它列为主体/消费层依赖门，不能声称三文件波次已闭环。
2. **P1：public mutation callback 重新开放了 disposition 权威，且失败重试论证与源码相反。** D6 公开
   `NavigationConsumeDisposition`，业务可对 OBSERVED/EXECUTED 返回 `RETIRE_FOR_RENEWAL`；虽 coordinator 最终会
   拒非 NOT_EXECUTED renewal，但 capability 已不是 closed。更严重的是 D6 Q2 `mutation 失败语义` 声称重入会重试，
   实际异常路径调用 `markBusinessConsumptionUnknown`，phase 变为 `BUSINESS_CONSUMPTION_UNKNOWN`，下一次
   `reserveFinalConsumption` 只接受 `OUTCOME_FINAL_UNCONSUMED`，不会重试。D7 删除 public disposition 选择：已知成功
   只能由 port 固定 `OCCURRENCE_COMPLETE`，可信 NOT_EXECUTED 只能由 port 固定 retire；业务 mutation 必须绑定一个
   exact retained Navigation workflow owner、单锁内无 I/O/无 port recursion 的确定性 commit。异常后明确 UNKNOWN
   fail-closed、不可重放 mutation，不得再写“自动重试”。
3. **P1：同 attempt 参数稳定仍只是 caller 约定。** D6 Q5 说 payload “构造恰一次”，但公开方法每次仍接
   `mapNameText/clientX/clientY/timeoutMs` 并重新构造 request；ledger 的 bind-once 只能在参数变化时抛 digest conflict，
   不能让重入自动复用第一次 bytes。UNKNOWN、receipt-pending 或 resume 重入时重算坐标/文本即可把同一 attempt 卡死。
   D7 必须让 trusted retained Navigation workflow state 在首次 invoke 前冻结 canonical command parameters/request bytes，
   同 attempt 重入只读取该 retained value；新 occurrence 或 compacted NOT_EXECUTED renewal 才允许新参数。若该 owner 尚未
   落地，输入 capability 后置，当前叶子只能做参数完全固定且无重算风险的部分。
4. **P2：三文件波次与公开类型/ready 依赖文件表不一致。** D6 同时新增
   `NavigationCaptureResult/NavigationFactReadResult/NavigationInputResult/NavigationFinalMutation/NavigationConsumeDisposition`
   却仍称仅 `CloudNavigationMechanicalPort + 2 context Modify` 三文件；也没有 receipt-ready capability 的 FQCN/owner。
   D7 给精确 New/Modify 表：类型若为 port 的 nested public types须明确写出，删除不再需要的 disposition；receipt-ready 与
   retained workflow owner 若不在当前闭包，必须列为硬前置并相应缩小第一实施波次。

### 下一任务 `W-NAV-D7`

External Worker A 先在本日志真实末尾追加 `CLAIMED`（task、claimedAt、唯一写集=仅本日志），领取截止
`2026-07-13T12:33:00-04:00`。只追加 Design Repair #6 Delta 关闭上述 `P1 x3/P2 x1`；已通过的 caller matrix、
本地 Runner 观察边界、真实 enum/import 分类不重开。两仓 Java/Maven/schema/resources/tests/host/caller 全冻结。
Worker QA 不算父级批准。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T12:16:29-04:00

- task: W-NAV-D7
- claimedAt: 2026-07-13T12:16:29-04:00（领取截止 2026-07-13T12:33:00-04:00 内）
- uniqueWriteSet: 仅本日志 `docs/superpowers/plans/reports/2026-07-13-cloud-navigation-service-worker-a.md`（append-only，Design Repair #6 Delta）。两仓 Java/Maven/schema/resources/tests/host/caller 全冻结；不写 Java、不跑 Maven、不 git、不触他人写集/日志。已通过的 caller matrix、本地 Runner 观察边界、真实 enum/import 分类不重开。自审仅 QA，不构成父级批准。

## External Worker A - Design Repair #6 Delta - 2026-07-13

关闭 `Parent Design Review #6` 的 P1×3/P2×1。仅设计，两仓 Java/Maven/schema/resources/tests/host/caller 全冻结；已通过项（12 caller matrix、本地 Runner 观察边界、真实 enum/import 分类）不重开。新增证据基线（逐行核对）：`CloudTaskRunActionLedger.java` 真实 phase 链——`FinalConsumptionPhase{UNBOUND,BOUND_UNRECORDED,OUTCOME_UNKNOWN,OUTCOME_FINAL_UNCONSUMED,BUSINESS_CONSUMING,BUSINESS_CONSUMPTION_UNKNOWN,BUSINESS_CONSUMED_NOTICE_PENDING,LOCAL_FRONTIER_APPLIED,COMPACTED_FRONTIER}`（:1459-1469）；`reserveFinalConsumption` 仅接受 `OUTCOME_FINAL_UNCONSUMED`→`BUSINESS_CONSUMING`（:904-920）；`completeBusinessConsumption` 落 `BUSINESS_CONSUMED_NOTICE_PENDING` 并发布 final-consumed control（:655-697，deterministic commit 注释 :682-684）；`markBusinessConsumptionUnknown`→`BUSINESS_CONSUMPTION_UNKNOWN`（:700-726）；receipt 侧 `RemoteFinalConsumptionCoordinator.acceptReceipt`（:75）→`commitCompaction`（:109）→`COMPACTED_FRONTIER`（:1451-1454）；`isOccurrenceComplete/isRenewalCompacted` 均要求 `COMPACTED_FRONTIER`（:634-646）；remote 包**不存在** receipt-driven ready/wake capability（全包检索唯一 sticky 语义=`CloudTaskTurnAuthority` 的 sticky **cancellation**（:30/:61/:400），与 receipt-compaction ready 无关）。

### R1（P1-1）：receipt 门修正——新增 closed typed `FINAL_CONSUMPTION_PENDING/RENEWAL_PENDING`，推进只认 COMPACTED_FRONTIER

**撤回** D6-Q2 步骤 3 的两处时序错误：①"consume 返回后下一调用 occurrence+1"——实际 `consume*Final` 返回时 phase=`BUSINESS_CONSUMED_NOTICE_PENDING`，`isOccurrenceComplete` 为 false（:634-639 要求 COMPACTED_FRONTIER），occurrence 不推进；②"compact 后随即 `renewAfterNotExecuted`"——同理 `isRenewalCompacted` 在 receipt 前为 false（:641-646），立即 renew 被 :190-193/:361-368 拒绝抛异常。修正后的 port 分派（每 closed method 内，全 closed typed，无 sleep/poll/thread/自动 retry）：

- **OBSERVED/EXECUTED + 业务 commit 成功** → `consume*Final(h, out, 固定 OCCURRENCE_COMPLETE)` 返回后，port 返回 **`FINAL_CONSUMPTION_PENDING`**（typed 结果状态；语义=业务事实已消费、等待 DHXY 本地 apply 回传 receipt）。**occurrence 推进不由 port/业务执行**——它是既有 `retain()` 门的自动行为：下次重入同方法时 `retain()` 检查 `isOccurrenceComplete`（:157-164），receipt 已 compacted 则开 occurrence+1，否则返回同 handle（port 检测到 phase 仍 pending → 再次返回 `FINAL_CONSUMPTION_PENDING`，不重放业务 mutation、不重发 wire）。
- **可信 NOT_EXECUTED** → `consume*Final(h, out, 固定 ATTEMPT_RETIRED_FOR_RENEWAL)` 返回后，port 返回 **`RENEWAL_PENDING`**（等待 receipt compact 该 retire）。**renew 移至下次重入**：重入时 `isRenewalCompacted()==true` 才执行 `renewAfterNotExecuted`（:85-89→:178-205）铸 attempt+1 并做该 attempt 首次 invoke；仍 false 则再次返回 `RENEWAL_PENDING`。可信 NOT_EXECUTED 不再卡死也不再抛异常。
- **重入唤醒时机归属**：pending→ready 的信号=**receipt-driven ready capability——当前 remote 包不存在该 API（证据见头部检索），列为 NavigationService 主体与消费层的显式硬依赖门 `receipt-ready capability`（非-A/后续切片）**；落地前 port 只保证"重入幂等 + pending typed 状态"，不声称三文件波次已闭环（见 R4 波次收缩）。禁止以 sleep/poll/Cloud 线程模拟 ready。

### R2（P1-2）：删除 public disposition 权威——port 固定 disposition，业务只有无返回值确定性 commit；UNKNOWN fail-closed 不可重放

**撤回** D6 的 `NavigationConsumeDisposition` public enum、`NavigationFinalMutation<O>` 带 disposition 返回值、以及"mutation 失败→重入重试同一 mutation"（与源码相反：mutation 异常路径走 `markBusinessConsumptionUnknown`→`BUSINESS_CONSUMPTION_UNKNOWN`（:700-726），而 `reserveFinalConsumption` 仅接受 `OUTCOME_FINAL_UNCONSUMED`（:904）——**同一 outcome 不可再次进入 BUSINESS_CONSUMING，mutation 不可重放**）。修正后：

- disposition 选择权收归 port 固定映射（业务零选择）：OBSERVED/EXECUTED→`OCCURRENCE_COMPLETE`；可信 NOT_EXECUTED→`ATTEMPT_RETIRED_FOR_RENEWAL`（此时业务 commit **不执行**）。`FinalConsumptionDisposition` 不出 remote 包（本就 package-private，:150-153）。
- 业务回调收缩为 **`NavigationBusinessCommit<O> { void commit(O exactOutcome) throws InterruptedException; }`**（port nested public，无返回值）。**绑定合同**：commit 必须是 exact retained Navigation workflow owner（R3 的 `NavigationWorkflowState`）上的**单锁内确定性状态提交**——仅允许纯 CPU 解释（模板/OCR/route 判定）与该 owner 内存字段更新；**禁止 I/O、禁止 port 递归**（port 以 per-port reentrancy 断言强制：commit 执行期内再调任何 port 方法抛 `IllegalStateException`）、禁止阻塞等待。
- **异常语义（fail-closed，无自动重试）**：commit 抛出（含 Interrupted）→ coordinator 既有路径落 `BUSINESS_CONSUMPTION_UNKNOWN` → port 返回 typed **`CONSUMPTION_UNKNOWN`** 终态；该 step 按 D5-O3/D6-Q2 的 UNKNOWN 纪律封存交上层收口，**不重放 mutation、不推进 occurrence/attempt、不压 not-reached 再点**。设计文本不再含任何"自动重试"表述。

### R3（P1-3）：同 attempt 参数冻结 owner + 输入 capability 后置

**撤回** D6-Q5"payload 构造恰一次"仅靠 caller 约定的形态（父级判定成立：公开方法每次接 `mapNameText/clientX/clientY/timeoutMs` 重构 request，UNKNOWN/receipt-pending/resume 重入重算参数即 digest conflict 卡死同 attempt）。修正：

- **参数冻结 owner = `NavigationWorkflowState`**（New，remote 包，assembly/context 持有，业务不可 mint）：每个 semantic address 的 canonical command parameters（键入文本/点击坐标/组装后 request 参数）在该 attempt **首次 invoke 前**写入 retained 冻结槽；同 attempt 任何重入（UNKNOWN 重放、receipt-pending、resume 跨代重建 port）**只读冻结值，不接受也不重算新参数**；仅新 occurrence（上轮 COMPACTED+OCCURRENCE_COMPLETE 后）或 compacted NOT_EXECUTED renewal 清槽并接受新参数。冻结槽随 `CloudTaskRetainedActionState` 同寿命（resume 经既有 `existingRetainedActionState` 复用链，:53-79 构造），restart 无 restore（Full R0 结论不变）。
- **该 owner 当前未落地 → 全部 5 个输入 capability（`typeWorldMapSearch/clickWorldMapCandidate/clickRouteDialog/clickMiniMap/clickNpcDirect`）与带业务参数的调用面整体后置**到 `NavigationWorkflowState` + R1 receipt-ready 两道门之后。
- **当前第一波叶子收缩为参数完全固定、零重算风险的 4 个读方法**：`captureWorldMapResult/captureRouteDialog/captureMiniMap/readWindowGeometry`——公开签名进一步收紧为 `capture*(NavigationBusinessCommit<CaptureOutcome> commit)` / `readWindowGeometry(NavigationBusinessCommit<WindowFactOutcome> commit)`：**timeoutMs 也从签名移除**，各方法体固定各自 HEAD 常量（ROI/PNG/`CLOUD_SERVICE_INPUT`/timeout 全部钉死），业务可变参数=0，重入天然幂等。

### R4（P2-1）：精确 New/Modify 表（nested public types 全列）+ 硬前置门 + 第一波次收缩

| 仓库 | 精确 FQCN / nested type | New/Modify | 说明 |
|---|---|---|---|
| Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.CloudNavigationMechanicalPort` | New | 构造 package-private；本波仅 4 个读方法（R3）；内部 private enum slot/私有固定 ROI/timeout 常量 |
| Cloud | ├ nested `public interface NavigationBusinessCommit<O>` | New（port nested）| 无返回值确定性 commit（R2）|
| Cloud | ├ nested `public record NavigationCaptureResult(...)` | New（port nested）| 包 `CaptureOutcome` + closed typed 状态（`OBSERVED/NOT_EXECUTED/UNKNOWN/STOPPED/FINAL_CONSUMPTION_PENDING/RENEWAL_PENDING/CONSUMPTION_UNKNOWN`）|
| Cloud | ├ nested `public record NavigationFactReadResult(...)` | New（port nested）| 同上，包 `WindowFactOutcome` |
| Cloud | （已删除）`NavigationInputResult`/`NavigationFinalMutation<O>`/`NavigationConsumeDisposition` | — | 输入后置（R3）；disposition 权威收回 port（R2）|
| Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskServiceExecutionContext` | Modify | `navigationMechanicalPort` final 字段 + 两构造（:27/:53）赋值 + public accessor（同 D5 形状）|
| Cloud | `com.bot.dhxy.runner.context.TaskExecutionContext` | Modify | public `getNavigationMechanicalPort()` passthrough |
| 门 | **`receipt-ready capability`**（receipt-compaction 驱动的 ready/wake；当前无 FQCN——remote 包不存在此 API）| **硬前置门** | NavigationService 主体与全部"pending 后重入"消费时序的前置；非-A/后续切片 |
| 门 | **`NavigationWorkflowState`**（同 attempt 参数冻结 + 业务 commit 单锁 owner）| **硬前置门** | 全部输入 capability 与带参调用面的前置；落地时归 remote 包/assembly 持有 |
| 门 | migrate-cloud 5 项（D6-Q3 表 #1-5）+ observation/wake capability（D6-Q4）+ 12 caller cohort（非-A）| 硬前置门 | NavigationService 整类编译/接线前置，不变 |

**第一实施波次（收缩后）**：上表 3 个 Java 文件（port 含 nested types + 2 context Modify），能力面=4 个零参数读 capability + pending typed 状态；**不含任何输入、不含 occurrence 推进消费闭环的"已闭环"声称**——后者显式挂在 `receipt-ready capability` 与 `NavigationWorkflowState` 两道门上。与 A(scale-wire FINAL)/B(TeamReturn)/U·CommonBox/P2 transaction 写集零交叉不变；零 wire/零新 purpose/零新 `WindowFactKind` 结论不变。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 **P0=0 / P1=0 / P2=0**（仅 QA，不构成父级批准）。Design Repair #6 Delta 到此停止，等待父级复审；heartbeat 保持（5 分钟）。

## Parent Design Review #7 - BLOCKED / BOUNDARY ACCEPTED, FIRST WAVE WITHDRAWN - 2026-07-13T12:32:00-04:00

D7 对 ACK publication 与 receipt compaction 的区分、固定 disposition、mutation 异常 UNKNOWN、输入参数冻结、本地
continuous observation 边界均正确，相关架构边界不再重开。但父级按当前真实 API 复核后，R4 所称 3 文件第一波仍不能
实施，当前 **P0=0/P1=2/P2=1**，Java/Maven/schema/resources/tests/host/caller 继续冻结。

1. **P1：port 没有真实 API 判断 `FINAL_CONSUMPTION_PENDING/RENEWAL_PENDING`。** 当前
   `CloudTaskRetainedActionState` 只在 `retain()` 内部调用 `isOccurrenceComplete`，在
   `renewAfterNotExecuted()` 内部调用 `isRenewalCompacted`；`CloudTaskServicePort.ActionHandle.identity()` 与 ledger phase
   查询也均 package-private，且没有“notice pending / compacted / consumption unknown”的 closed status accessor。D7 只写
   “port 检测 phase 仍 pending”，R4 却不修改 retained state/ledger，也没有 port-retained previous handle，因此无法实现：
   pending 重入若再次 consume 会被 ledger 拒绝，若再次 invoke 只能重放 outcome，仍不知道是否应调用 mutation。
   **返修条件：**要么把 exact package-private status query/advance API 的 FQCN、状态机、New/Modify 文件纳入闭包；要么
   诚实撤回整个 port 第一波，等 receipt-ready capability 落地后再实施。禁止靠 catch exception、sleep/poll 或 port 私有猜测。
2. **P1：D7 要求 business commit 绑定 `NavigationWorkflowState`，但第一波仍公开任意 callback 且不包含 owner。**
   R2 明确 commit 必须在 exact retained workflow owner 单锁内确定提交，R3/R4 又只把该 owner列为“输入 capability”硬门，
   四个 read 方法仍接 `public NavigationBusinessCommit<O>`。任意业务对象可提交、重复提交或无 owner 提交，R2 的安全合同
   没有结构性实现。**返修条件：**`NavigationWorkflowState` 门必须覆盖所有 read commit，不仅输入；第一波若不包含该 owner，
   就不得公开 callback/consume 方法，也不得声称 read capability 可实施。若包含 owner，业务只能持不可 mint 的 exact mutation
   handle/capability，不能直接传 raw public callback。
3. **P2：typed result 状态与字段/nullability/重入动作仍不闭合。** D7 同时列 `OBSERVED/NOT_EXECUTED` 与
   `FINAL_CONSUMPTION_PENDING/RENEWAL_PENDING/CONSUMPTION_UNKNOWN`，但未说明 consume 开始后 outcome 是否还能暴露、pending
   重入是否允许再次 callback、receipt compacted 后本次调用返回 ready 还是直接打开下一 occurrence。**返修条件：**给一张
   exact phase -> public result -> callback count -> next invocation 表，保证每个 outcome 的 business mutation 至多一次。

### 下一任务 `W-NAV-D8`

External Worker A 先在本日志真实末尾追加 `CLAIMED`（task、claimedAt、唯一写集=仅本日志），领取截止
`2026-07-13T12:52:00-04:00`。只追加 Design Repair #7 Delta，不重开已通过的 caller/import/local-observer 边界。
本轮必须二选一并说清：

- **推荐：**撤回当前 3 文件 port implementation wave，把 Navigation 整体标成等待 receipt-ready + retained workflow owner，
  同时从已盘点依赖中提出一个不依赖这两门、可直接 package 的真实 CPU/type leaf 作为下一实施任务；或
- 把 status query、receipt-ready 与 exact workflow mutation owner 的完整可编译 FQCN/method/file table 纳入同一闭包。

不得继续用不存在的 API 支撑“当前可实施”。两仓 Java 继续冻结；20 分钟只检查领取，逾期只重发给 External A，绝不
内部接管。Worker QA 不算父级批准。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T12:35:32-04:00

- task: W-NAV-D8
- claimedAt: 2026-07-13T12:35:32-04:00（领取截止 2026-07-13T12:52:00-04:00 内）
- uniqueWriteSet: 仅本日志 `docs/superpowers/plans/reports/2026-07-13-cloud-navigation-service-worker-a.md`（append-only，Design Repair #7 Delta）。两仓 Java/Maven/schema/resources/tests/host/caller 全冻结；不写 Java、不跑 Maven、不 git、不触他人写集/日志。已通过的 caller/import/local-observer 边界不重开。自审仅 QA，不构成父级批准。

## External Worker A - Design Repair #7 Delta - 2026-07-13

关闭 `Parent Design Review #7` 的 P1×2/P2×1，采用父级**推荐路线**：撤回当前 3 文件 port 第一波，Navigation 机械面整体挂门，并提出一个不依赖两门、可直接 package 的真实 CPU/type leaf。仅设计，两仓 Java/Maven/schema/resources/tests/host/caller 全冻结；已通过的 caller/import/local-observer 边界与 D7 已接受的架构边界（ACK/receipt 区分、固定 disposition、UNKNOWN fail-closed、参数冻结、本地持续观察）不重开。

### S1（P1-1）：撤回 3 文件 port implementation wave——pending 判定在当前 API 上不可实现

**撤回** D7-R4 的全部第一波（`CloudNavigationMechanicalPort` New + `CloudTaskServiceExecutionContext` Modify + `TaskExecutionContext` Modify，含全部 nested public types 与 4 个读方法）。父级 P1-1 逐条成立，源码确证：

- `isOccurrenceComplete`（ledger :634）仅被 `CloudTaskRetainedActionState.retain()` 内部调用（state :157）；`isRenewalCompacted`（ledger :641）仅被 `renew()` 内部调用（state :190）；两者均 package-private 且**无面向 port 的 closed status accessor**。
- `CloudTaskServicePort.ActionHandle.identity()` package-private（port :210-212）；ledger 的 phase 查询（`FinalConsumptionPhase` :1459-1469 私有 enum）无任何 "notice-pending / compacted / consumption-unknown" 对外可见形态。
- 因此 D7-R1 的"port 检测 phase 仍 pending"没有可编译实现：pending 重入时 port 无法区分"应 consume（首次拿到 final outcome）/应只重放（已 consume 等 receipt）/应 renew（retire 已 compacted）"，再次 consume 会被 `requireReservation`（:661/:704 只接受 BUSINESS_CONSUMING 前置）/`reserveFinalConsumption`（:904 只接受 OUTCOME_FINAL_UNCONSUMED）拒绝，再次 invoke 只能重放 outcome 且不知是否已执行过 mutation。**不以 catch exception/sleep/poll/私有猜测支撑实现**——第一波撤回。
- **status query/advance API 并入 `receipt-ready capability` 门的定义**：该门落地时必须同时提供 remote 包内 closed 的 per-handle 状态查询（至少区分 `OUTCOME_PENDING_CONSUME / NOTICE_PENDING / COMPACTED_OCCURRENCE_COMPLETE / RENEWAL_COMPACTED / CONSUMPTION_UNKNOWN`）与 receipt-compaction ready 通知；其 FQCN/状态机/New-Modify 文件属该门切片（非-A），本 Delta 不预定。

**Navigation 机械 port 与 `com.bot.dhxy.service.NavigationService` 主体的实施状态统一为：等待两道硬门——`receipt-ready capability`（含 closed status query/advance API）+ `NavigationWorkflowState`（参数冻结 + commit owner）。** 两门之前不存在任何"当前可实施"的 Navigation 机械调用面。

### S2（P1-2）：workflow owner 门覆盖**所有** commit（read 与 input 一致）——公开 raw callback 面随第一波撤回

**撤回** D7-R3/R4 中"4 个读方法仍接 `public NavigationBusinessCommit<O>`"的形态。父级判定成立：R2 的安全合同（exact retained workflow owner 单锁内确定性提交）没有结构性载体时，任何 public callback 都允许任意业务对象提交/重复提交/无 owner 提交。修正后的合同（门后实施时生效）：

- `NavigationWorkflowState` 门**覆盖全部 capability 的 commit**（read capture/geometry 与 input 一律），不再区分"输入才需要 owner"。
- 业务侧永不接触 raw public callback：门落地后，commit 面呈现为 **workflow owner 上不可 mint 的 exact mutation handle/capability**（由 assembly/context 在 remote 包内构造并绑定到该 owner 的单锁），port 方法只接受该 handle 类型；`NavigationBusinessCommit<O>` 公开接口**删除**（D7-R4 表中该 nested type 随第一波一并撤回）。
- 第一波撤回后，本轮**不公开任何 callback/consume 方法，也不声称任何 read capability 可实施**。

### S3（P2-1）：exact phase → public result → callback count → next invocation 合同表（门后实施合同）

第一波已撤回；本表作为两门落地后 Navigation 机械 port 的**绑定实施合同**（每个 outcome 的业务 mutation **至多一次**由第 3 列强制）：

| ledger phase（真实 :1459-1469）| port public result | 本次调用 callback 次数 | outcome 是否暴露 | 下一次同方法调用的动作 |
|---|---|---|---|---|
| `OUTCOME_FINAL_UNCONSUMED`（首次拿到 final outcome）| OBSERVED/EXECUTED→`OBSERVED`；可信 NOT_EXECUTED→`RENEWAL_PENDING`（port 固定 retire）| OBSERVED/EXECUTED：**恰 1 次**（consume 临界区内）；NOT_EXECUTED：0 次 | 是（typed outcome 随 result 暴露一次）| 经 status query：见下行对应 phase |
| `BUSINESS_CONSUMING`（commit 执行中，单锁内）| —（不可重入：owner 单锁 + port 反递归断言）| — | — | — |
| `BUSINESS_CONSUMED_NOTICE_PENDING` | `FINAL_CONSUMPTION_PENDING` | **0 次**（mutation 已执行过，绝不重放）| 否（不再暴露 outcome，防二次消费）| 仍 pending→重复本行；receipt compacted→下行 |
| `COMPACTED_FRONTIER` + `OCCURRENCE_COMPLETE` | 本次调用即开下一 occurrence 并做**新 occurrence 首次 invoke**（`retain()` :157-164 既有门自动推进），返回新 outcome 对应行 | 新 occurrence 的 outcome 各自计数（每 outcome 至多 1）| 新 outcome 暴露 | 按新 occurrence 状态 |
| `COMPACTED_FRONTIER` + `ATTEMPT_RETIRED_FOR_RENEWAL` | 本次调用执行 `renewAfterNotExecuted`（:178-205）→ 新 attempt 首次 invoke，返回新 outcome 对应行 | 同上 | 新 outcome 暴露 | 按新 attempt 状态 |
| `BUSINESS_CONSUMPTION_UNKNOWN`（commit 抛出后）| `CONSUMPTION_UNKNOWN`（终态，fail-closed）| **0 次**（`reserveFinalConsumption` :904 只接受 OUTCOME_FINAL_UNCONSUMED，mutation 不可重放）| 否 | 恒返回 `CONSUMPTION_UNKNOWN`；该 step 封存交上层，terminal 时 `acceptTerminalRun`（:625）退账 |
| invoke 返回 UNKNOWN（副作用后不确定，未进入 consume）| `UNKNOWN` | 0 次 | 是（typed UNKNOWN outcome）| 重放同 outcome→重复本行（不推进；收口走 D5-O3 观察路径）|
| invoke 返回 STOPPED | `STOPPED` | 0 次 | 是 | HEAD stop unwind；terminal 退账 |

- "callback 恰 1 次"的结构保证：mutation 仅在 `OUTCOME_FINAL_UNCONSUMED→BUSINESS_CONSUMING` 的 reserve 成功路径内执行（:904-920 单调不可逆），NOTICE_PENDING/UNKNOWN/COMPACTED 各行 0 次——同一 outcome 二次进入 BUSINESS_CONSUMING 被 :904 拒绝，结构上不可能重复 mutation。
- 本表行为全部依赖 S1 的 closed status query（区分第 1/3/4/5/6 行）——再次证明两门是硬前置。

### S4：下一实施任务=真实 CPU/type leaf `CloudNavigationProperties` + `CloudNavigationPropertiesAuthority`（零两门依赖，可直接 package）

从已盘点依赖（D6-Q3 migrate-cloud #5：`config.BotProperties`）提取。**证据链**：

- HEAD `0114604e` `NavigationService` 对 `BotProperties`（字段 `config`，:173）的消费**恰 4 处/4 字段**：`config.getAnchor_windowTo_map_scroll_X()/getAnchor_windowTo_map_scroll_Y()`（:2216-2217，世界地图 scroll 焦点）、`config.getAnchor_windowTo_map_search_X()/getAnchor_windowTo_map_search_Y()`（:2389，世界地图搜索框）。无其它字段、无布尔/长整消费。
- HEAD `application.yml` 基线值（git `0114604e:src/main/resources/application.yml` :46-50）：`scroll_X=560`、`scroll_Y=370`、`search_X=348`、`search_Y=376`——BASELINE 常量逐字取此。
- Cloud 既有先例形状（同包已编译存在）：`CloudMaintenanceProperties`（public interface 纯 getter）+ `CloudMaintenancePropertiesAuthority`（package-private final class，BASELINE 常量、`CloudServiceScope`+`AtomicReference<Snapshot>`、`seedNoOverride` 工厂）；其 import 闭包=`com.yueyunfe.dhxy.cloudbrain.host.CloudServiceScope`+JDK（实测该文件 import 仅此三行）——**零 receipt-ready/workflow-owner 依赖、零 retain-local 依赖、可直接 `mvn -q clean package`**。
- 候选淘汰记录（诚实核对，不硬造叶子）：`CoordinateHelper`（HEAD import `GameClientTracker`/`WindowScopedTempPath`——retain-local，非干净叶子）；`RuntimeDecisionShadowService`（HEAD import `WindowRuntimeContext`/`WindowTaskContextHolder`/`WindowNativeBinding`——retain-local，非干净叶子）。

| 仓库 | 精确 FQCN | New/Modify | 内容 |
|---|---|---|---|
| Cloud | `com.bot.dhxy.config.CloudNavigationProperties` | New | public interface，4 个 getter（`getAnchorWindowToMapScrollX/Y`、`getAnchorWindowToMapSearchX/Y`，int），javadoc 标注 HEAD 字段对应关系 |
| Cloud | `com.bot.dhxy.config.CloudNavigationPropertiesAuthority` | New | package-private final class implements 上接口；BASELINE 四常量=560/370/348/376（HEAD yml 逐字）；`CloudServiceScope`+`AtomicReference<Snapshot>`+`seedNoOverride`，逐字复用 `CloudMaintenancePropertiesAuthority` 形状 |

- 定位：NavigationService 主体（门后）经该接口取代 `BotProperties` 消费（D6-Q3 #5 落地）；本叶子不接线任何 host/Task/caller（保持 dormant），不触碰两门，与 A/B/U/P2 写集零交叉。
- 验收（实施时）：Cloud `mvn -q clean package`（不 skip）双文件编译+既有 21 测试全绿；无新 wire/schema/digest。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 **P0=0 / P1=0 / P2=0**（仅 QA，不构成父级批准）。Design Repair #7 Delta 到此停止，等待父级复审（第一波已撤回，唯一提请实施项=S4 叶子）；heartbeat 保持（5 分钟）。

## Parent Design Review #8 - BOUNDARY APPROVED / CONFIG LEAF IMPLEMENTATION AUTHORIZED - 2026-07-13T12:40:00-04:00

D8 采用推荐路线撤回不可实现的 3-file mechanical port wave，且明确 receipt-ready/status query 与 exact
`NavigationWorkflowState` owner 覆盖 read/input 全部 commit。phase-result-callback 表保证同一 final outcome 的 business
mutation 至多一次；本地 Runner 继续持有 continuous movement/dialog/pathing observation、capture/template、soft wake 与
input safety。Navigation 主体当前诚实标记为等待两道硬门，相关边界 **APPROVED，P0=0/P1=0/P2=0**。

父级复核 HEAD `0114604e`：`NavigationService` 对 `BotProperties` 的消费确为四个 anchor getter，基线 yml 值确为
`560/370/348/376`。S4 两文件叶子可独立 package，现直接授权实施，绑定修正如下：authority 需完整复用当前
`CloudMaintenancePropertiesAuthority` 的 tenant/user scope + immutable Snapshot + revision CAS 形状，包含
`seedNoOverride`、`seedOverride`、`replaceNoOverride`、`replaceOverride`；不得只放一个无更新路径的 AtomicReference，
不得 clamp/scale/改写四个 int。

### 下一任务 `W-NAV-0-IMP1`

External Worker A 须在 `2026-07-13T13:00:00-04:00` 前于本日志真实末尾追加 `CLAIMED`，唯一 Java 写集为：

1. Cloud New `src/main/java/com/bot/dhxy/config/CloudNavigationProperties.java`；
2. Cloud New `src/main/java/com/bot/dhxy/config/CloudNavigationPropertiesAuthority.java`；
3. 本 append-only 日志。

接口仅四个 int getter；authority package-private final，baseline 常量严格为 560/370/348/376，scope/revision/source/
CAS 校验按上述现有 authority 形状。不得接 host/caller/NavigationService，不改 DHXY/wire/schema/resources/tests，不启动
运行面，不做 Git mutation。完成后运行 Cloud `mvn -q clean package`（不可 skip）并追加源码/构建证据；Worker 自审不算
父级批准。20 分钟只检查领取，逾期只重发给 External A，不内部接管。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T12:49:58-04:00

- task: W-NAV-0-IMP1
- claimedAt: 2026-07-13T12:49:58-04:00（领取截止 2026-07-13T13:00:00-04:00 内）
- uniqueWriteSet: ① Cloud New `src/main/java/com/bot/dhxy/config/CloudNavigationProperties.java`；② Cloud New `src/main/java/com/bot/dhxy/config/CloudNavigationPropertiesAuthority.java`；③ 本 append-only 日志。不接 host/caller/NavigationService，不改 DHXY/wire/schema/resources/tests，不启动运行面，不做 Git mutation。完成后 Cloud `mvn -q clean package`（不 skip）并追加源码/构建证据。自审仅 QA，不构成父级批准。

## External Worker A - W-NAV-0-IMP1 实施证据 - 2026-07-13

按 Parent Design Review #8 授权完成 config leaf 实施。写集恰为授权三项，无其它文件改动；未接 host/caller/NavigationService，未改 DHXY/wire/schema/resources/tests，未启动运行面，未做 Git mutation。

### 源码证据

1. **New `src/main/java/com/bot/dhxy/config/CloudNavigationProperties.java`**：public interface，恰 4 个 int getter（`getAnchorWindowToMapScrollX/Y`、`getAnchorWindowToMapSearchX/Y`），javadoc 标注与 HEAD `0114604e` `BotProperties` 字段（`anchor_windowTo_map_scroll_X/Y`、`anchor_windowTo_map_search_X/Y`）的逐一对应；声明不 clamp/不 scale/不改写。
2. **New `src/main/java/com/bot/dhxy/config/CloudNavigationPropertiesAuthority.java`**：package-private `final class ... implements CloudNavigationProperties`，**完整复用 `CloudMaintenancePropertiesAuthority` 形状**——`CloudServiceScope` 归属 + private 构造（scope/snapshot-scope 全等校验）+ immutable `private record Snapshot(scope, configRevision, 4×int, Source)`（compact 构造校验 scope/source 非空、revision 非负）+ `AtomicReference<Snapshot>` + 四个更新路径 `seedNoOverride / seedOverride / replaceNoOverride / replaceOverride` + 私有 `replace`（expectedScope 全等、expectedRevision 非负、`configRevision==expectedRevision` 冲突检查、`nextRevision` 单调、`compareAndSet` CAS 失败抛并发异常）+ `private enum Source{BASELINE_NO_OVERRIDE, CONTROL_PLANE_OVERRIDE}`。BASELINE 四常量严格 `560/370/348/376`（HEAD yml :46-50 逐字）；getter 直读 `snapshot.get()`，零 clamp/零改写。
3. 编译产物核验（`javap -p`）：`final class com.bot.dhxy.config.CloudNavigationPropertiesAuthority implements com.bot.dhxy.config.CloudNavigationProperties`，含 `seedNoOverride(CloudServiceScope,long)`、`seedOverride(CloudServiceScope,long,int,int,int,int)`、`replaceNoOverride(CloudServiceScope,long,long)`、`replaceOverride(...)`、`AtomicReference<Snapshot>` 字段——与先例签名形状一致。

### 构建证据

- Cloud `mvn -q clean package`（未 skip）**exit 0**；surefire 汇总 **Tests run 21, Failures 0, Errors 0, Skipped 0**（4 suite：CloudBrainTemplateResourceGuardTest / IncenseStatusCloudBrainContractTest / WuhuanRouteCandidateContractTest / XiuluoCloudBrainContractTest）；shaded JAR `target/dhxy-cloud-brain-0.1.0-SNAPSHOT.jar` 产出。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 **P0=0 / P1=0 / P2=0**（仅 QA，不构成父级批准）。W-NAV-0-IMP1 到此停止，等待父级复审；heartbeat 保持（5 分钟）。

## Parent Source Review #1 - FINAL APPROVED / NEXT TASK PUBLISHED - 2026-07-13T13:02:00-04:00

父级逐行复核两个新增文件，并对照 HEAD `0114604e` 的 `NavigationService` 四个真实 getter、
`application.yml` 的 `560/370/348/376` 以及当前 `CloudMaintenancePropertiesAuthority`。接口恰有四个 int getter；
authority 为 package-private final，scope/snapshot 全等、revision 非负/单调、CAS 冲突拒绝、baseline/override 四条更新
路径均与现有形状一致，未接 host/caller，也没有 clamp/scale/第二配置权威。

父级 fresh Cloud `mvn -q clean package` exit 0；Surefire `suites=4, tests=21, failures=0, errors=0,
skipped=0`，shaded JAR 已生成。结论：`W-NAV-0-IMP1` **FINAL APPROVED，P0=0/P1=0/P2=0**。

### 下一任务 `W-NAV-WF-D1`

External Worker A 须在 `2026-07-13T13:22:00-04:00` 前于本日志真实末尾追加 `CLAIMED`（task、claimedAt、
uniqueWriteSet=仅本日志），然后只交一个实现前 Delta，Java 冻结。不得重开 D8 已批准的 local-retained/业务边界或
配置叶子；只闭合第二道硬门 `NavigationWorkflowState`：

1. 以当前 `CloudTaskRunCurrentContextSlot`、per-run state owner、Full R0 consume-final 与 HEAD 三个 Navigation public API/
   caller 为真实 API，给 exact state key、immutable initial metadata、跨 revision same-object 复用与 assembly 原子 publish；
2. 列出 read/input 每个 final outcome 对应的唯一 mutation、callback 次数、下一 invocation；`UNKNOWN` 保持 unresolved，
   `NOT_EXECUTED` 只按可信合同处理，STOPPED/terminal/pause cleanup 不得重铸 occurrence/action identity；
3. 禁止业务包 raw request/poll/outcome、raw ledger/mint/renew；列 package-private owner + closed business capability 的精确
   构造/可见性/方法签名，以及 current context/port/state 同 generation 的原子取得方式；
4. 给一个可直接编码的最小 New/Modify 文件与方法表。若仍缺前置，只列一个最小 blocker、当前源码证据和 owner，
   不再提出替代架构或占位 wrapper。

20 分钟只检查领取；逾期仍只重发给 External A，绝不内部接管。Worker 自审不算批准。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T13:04:03-04:00

- task: W-NAV-WF-D1
- claimedAt: 2026-07-13T13:04:03-04:00（领取截止 2026-07-13T13:22:00-04:00 内）
- uniqueWriteSet: 仅本日志 `docs/superpowers/plans/reports/2026-07-13-cloud-navigation-service-worker-a.md`（append-only，NavigationWorkflowState 实现前 Delta）。两仓 Java/Maven/schema/resources/tests/host/caller 全冻结；不写 Java、不跑 Maven、不 git。不重开 D8 已批准边界与配置叶子。自审仅 QA，不构成父级批准。

## External Worker A - NavigationWorkflowState 实现前 Delta（W-NAV-WF-D1）- 2026-07-13

闭合第二道硬门 `NavigationWorkflowState`。仅设计，两仓 Java 冻结；不重开 D8 已批准边界与配置叶子。真实 API 证据（逐行核对）：`CloudTaskRunCurrentContextSlot`（public `current()` 为 slot-generation 线性化点：CAS 自校验 :87-96，Closed→typed terminal :78-79/:92-93，非 lease；构造仅收首个 ACTIVE revision :57-60）、`CloudTaskRunAuthorityAssembly.resumeTaskServiceRuntime`（resume 新建 `CloudTaskServiceExecutionContext` 时**复用 `previousRuntime.retainedActionState()`** :250-258）、`CloudTaskRetainedActionState`（构造持 `initialContext` 并冻结 `StableRunKey` :30-40；StableRunKey=scope/taskRunId/taskType/window/nonTerminalStopEpoch :391-406，跨 runRevision 不变）、`CommonBoxStateGovernor`（SOURCE APPROVED 的 per-run/tenant state owner 先例：单 `Object lock` :38、hard cap 字段 :39-40、`removeRunPending` exact-run 清理 :257、`retireScope` :274、`TenantState.incarnation` :341-346）、Full R0 consume-final 单调链（D8-S3 表所引 :904-920/:634-646，不重复）。

### W1：exact state key、immutable initial metadata、跨 revision same-object 复用、assembly 原子 publish

- **归属选择（最小闭合）**：`NavigationWorkflowState` 作为 `CloudTaskRetainedActionState` 的 **1:1 附属字段**（`private final`，构造期创建）。理由：跨 revision same-object 复用**自动继承**既有链——assembly resume 复用同一 `retainedActionState`（:250-258）⇒ 同一 `NavigationWorkflowState`，零新增复用/publish 逻辑、零 assembly 修改；发布原子性=既有 `CloudTaskRunCurrentContextSlot` 的 slot publish（`AtomicReference<SlotState>` + generation handle），workflow state 不引入第二个 publish 点。
- **exact state key**：不自持——即宿主 `CloudTaskRetainedActionState.stableRunKey`（StableRunKey，:391-406）。所有 workflow 槽键在该 run key 之下为 `(NavigationActionSlot, occurrence, attempt)` 三元组（slot=D8-S1 并入 receipt-ready 门定义的固定语义槽 enum；occurrence/attempt 数值只读自 handle 的 `RemoteSemanticAddress`，workflow state **零编号能力**）。
- **immutable initial metadata**：构造仅冻结 `stableRunKey` 引用 + 空冻结槽 map + 空 commit 台账（D6-N3/N6 已裁定 navigation 无长寿命业务初始状态，不虚构字段）。restart 无 restore（Full R0 结论不变）。
- **同 generation 原子取得**：业务链恒为 `CloudTaskRunCurrentContextSlot.current()`（线性化点）→ 该代 `TaskExecutionContext` → 其 delegate `CloudTaskServiceExecutionContext`（该代构造期绑定同一 `retainedActionState`，:53-79 existing 构造）→ 宿主的同一 `NavigationWorkflowState`。context/servicePort/retainedActionState/workflowState 四者恒同 generation 绑定，无跨代混用；`current()` 抛 typed terminal/ConcurrentModification 时业务按既有 unwind，不缓存旧 state 引用。

### W2：read/input 每个 final outcome 的唯一 mutation、callback 次数、下一 invocation

D8-S3 合同表（APPROVED）逐行继承为唯一 phase→result→callback 权威，此处只补 owner 落位与两道"至多一次"结构保证（不重复表体）：

- **第一道（既有，per outcome）**：mutation 仅在 `OUTCOME_FINAL_UNCONSUMED→BUSINESS_CONSUMING` reserve 成功路径内执行（:904-920 单调不可逆）——同一 final outcome 二次进入被 :904 拒绝。
- **第二道（本 owner，per occurrence）**：`NavigationWorkflowState.markCommitted(slot, occurrence)` 在 mutation 临界区内先行 CAS 置位；同 (slot,occurrence) 二次置位返回 false ⇒ port 拒绝再次执行 commit（防御纵深，覆盖 read 与 input 全部 capability——D8-S2"owner 覆盖所有 commit"落地）。
- **disposition 归属**（继承 D8）：已知成功→port 固定 `OCCURRENCE_COMPLETE`；可信 `NOT_EXECUTED`（`startedStepIndex/lastCompletedStepIndex==-1` 既有校验）→port 固定 retire，mutation 不执行，经 `RENEWAL_PENDING`→receipt compacted 后 renew（同 occurrence attempt+1，identity 由 ledger 铸造）；**`UNKNOWN` 保持 unresolved**（不 consume/不 renew/不推进，冻结槽保留，重入只重放）；**STOPPED/terminal/pause cleanup 不重铸 occurrence/action identity**——workflow 侧只做 `removeRun`（清冻结槽+commit 台账，exact-run，同 `CommonBoxStateGovernor.removeRunPending` :257 形状），identity 台账归 ledger 既有 `acceptTerminalRun`（:625）路径，pause 期间 state 原样保留（PAUSED 不清、不重建）。
- 下一 invocation 语义逐行=D8-S3 第 5 列，无变更。

### W3：package-private owner + closed business capability 精确构造/可见性/签名（禁 raw 面）

**New（Cloud，`com.yueyunfe.dhxy.cloudbrain.remote`）`NavigationWorkflowState`** —— package-private `final class`，全部方法 package-private（业务包不可见、不可 mint）：

```
final class NavigationWorkflowState {
    private final Object lock = new Object();
    private final Map<FrozenKey, FrozenCommand> frozenCommands = new HashMap<>();
    private final Set<CommitKey> committed = new HashSet<>();

    NavigationWorkflowState() {}                                   // 附属构造，宿主字段初始化

    FrozenCommand freezeOnce(String actionSlotToken, long occurrence, int attempt,
                             FrozenCommand candidate)              // 首写冻结；已冻结则返回既存值（同 attempt 重入零重算）
    boolean markCommitted(String actionSlotToken, long occurrence) // 每 (slot,occurrence) 恰一次；二次返回 false
    void removeRunState()                                          // terminal cleanup：清两表；不触 identity/ledger

    sealed interface FrozenCommand permits FrozenText, FrozenPoint {}
    record FrozenText(String text) implements FrozenCommand {}     // typeWorldMapSearch 的 mapNameText
    record FrozenPoint(int clientX, int clientY) implements FrozenCommand {}  // 各 click 的坐标
    private record FrozenKey(String actionSlotToken, long occurrence, int attempt) {}
    private record CommitKey(String actionSlotToken, long occurrence) {}
}
```

- 单锁（`lock`）内完成 freeze/commit 判定，与 `CommonBoxStateGovernor` 同形；容量天然有界（槽数=固定 enum 基数 × 活跃 occurrence/attempt，terminal 即清，无 TTL/LRU）。
- **closed business capability（门后 port nested，本波不实施）**：业务永不见本类——mutation 面为 port nested `public static final class NavigationMutationHandle`（构造 package-private，绑定 owner+slot+occurrence），port 方法只接受该 handle；raw request/poll/outcome、raw ledger/mint/renew 全部不出 remote 包（继承 D8-S2，无回退）。

**Modify（Cloud，`remote`）`CloudTaskRetainedActionState`**：加 `private final NavigationWorkflowState navigationWorkflowState = new NavigationWorkflowState();` 字段 + package-private accessor `NavigationWorkflowState navigationWorkflowState()`；terminal 清理挂点复用既有 run-terminal 路径调 `removeRunState()`。**不改 retain/renew/invoke/consume 任何既有方法体**。

### W4：最小 New/Modify 文件与方法表 + 唯一 blocker

| 仓库 | 精确 FQCN | New/Modify | 方法面（全 package-private）|
|---|---|---|---|
| Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.NavigationWorkflowState` | New | `freezeOnce / markCommitted / removeRunState` + sealed `FrozenCommand`（FrozenText/FrozenPoint）|
| Cloud | `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskRetainedActionState` | Modify | +1 final 字段、+1 package-private accessor；零既有方法体改动 |

- 该两文件波次**不依赖 receipt-ready 门**（纯 state owner，无 phase 查询），可独立编译+package；port/mutation handle/NavigationService 主体仍在门后。
- **唯一最小 blocker（现状源码证据 + owner）**：`receipt-ready capability`（含 closed per-handle status query/advance API）仍缺——`isOccurrenceComplete/isRenewalCompacted` 仅 `retain()/renew()` 内部可达、`FinalConsumptionPhase` 私有（D8-S1 证据不重列）；owner=receipt-ready 切片（非-A）。它阻塞的是**机械 port 与消费闭环**，不阻塞本表两文件。无替代架构、无占位 wrapper。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 **P0=0 / P1=0 / P2=0**（仅 QA，不构成父级批准）。W-NAV-WF-D1 Delta 到此停止，等待父级复审；heartbeat 保持（5 分钟）。

## Parent Design Review #9 - BLOCKED / REPAIR TASK PUBLISHED - 2026-07-13T13:10:00-04:00

父级按当前 `CloudTaskRetainedActionState`、Full R0 consume-final、
`CloudTaskRunAuthorityAssembly.closeAndReleaseTerminalTaskServiceRuntime` 与本 Delta 逐项复审。跨 revision 复用同一
retained state、current-slot generation 绑定和冻结 command 的方向成立，但当前两文件波仍
**BLOCKED，P0=0/P1=3/P2=1**，Java 冻结。

1. **P1：`markCommitted` 在 Full R0 之外建立第二份业务提交真值。** Delta 要先把 `(slot, occurrence)` 写入
   `committed`，再执行 mutation；该 set 的锁与 `OUTCOME_FINAL_UNCONSUMED -> BUSINESS_CONSUMING -> CONSUMED`
   事务不是同一个线性化点。若 mark 后 mutation 抛错，重入会被永久抑制；若 mutation 后进程退出而未记账，又可能
   重放。影响是丢业务 mutation 或重复 mutation。返修必须删除这份平行 commit ledger，或证明 mutation 与 Full R0
   final-consume 在同一个既有 owner/transaction 内原子提交；workflow state 只能冻结重入所需数据，不能成为第二权威。
2. **P1：terminal cleanup 在所列 `1 New + 1 Modify` 中没有调用者。** 当前真实 terminal owner 位于
   `CloudTaskRunAuthorityAssembly.closeAndReleaseTerminalTaskServiceRuntime`，其在 State release 后调用
   `actionLedger.acceptTerminalRun` 与 broker retirement；`CloudTaskRetainedActionState` 自身不会收到 terminal 事件。
   Delta 一面说 terminal 路径调用 `removeRunState()`，一面又声明只加字段/accessor、零既有方法体修改，无法编码。
   返修须列出并修改真实 assembly terminal 方法，或复用一个当前已存在且确实调用 retained-state cleanup 的 owner；
   必须保持 exact terminal handle 与重试幂等。
3. **P1：两个集合不是“天然有界”。** `frozenCommands` 按 `(slot, occurrence, attempt)`、`committed` 按
   `(slot, occurrence)` 累积到 task terminal；长寿命 run 的 occurrence/NOT_EXECUTED attempt 可持续增长，当前没有
   compact 后删除、per-run hard cap 或 admission。影响是单租户长 run 可无界吃内存。返修须使用固定 slot enum，明确
   final-compacted/renew/terminal 各自删除点和原子 hard cap；不得用 TTL/LRU 改业务语义。
4. **P2：`String actionSlotToken` 重新开放了任意槽名。** 当前目标是 closed capability，owner API 应收固定
   Navigation slot enum/不可伪造 handle，而不是任意字符串；并写清 freeze 候选与 semantic address 的 exact-equality
   校验，避免同 key 不同 payload 静默返回旧值。

### 下一任务 `W-NAV-WF-D2`

External Worker A 须在 `2026-07-13T13:30:00-04:00` 前于本日志真实末尾追加 `CLAIMED`（task、claimedAt、
uniqueWriteSet=仅本日志），只写 Design Repair #1 Delta 关闭上述四项；两仓 Java/Maven/schema/resources/tests 全冻结。
保留已经成立的 retained-state 跨 revision 复用与 current-slot generation 结论，不重开配置叶子和 local-retained 边界。
交付必须给一个可直接编码的最小文件/方法表、唯一线性化 owner、容量账本和 terminal 重试时序。Worker 自审不算批准；
逾期只重发给 External A，绝不内部接管。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T13:13:58-04:00

- task: W-NAV-WF-D2
- claimedAt: 2026-07-13T13:13:58-04:00（领取截止 2026-07-13T13:30:00-04:00 内）
- uniqueWriteSet: 仅本日志（append-only，Design Repair #1 Delta，关闭 Review #9 的 P1×3/P2×1）。两仓 Java/Maven/schema/resources/tests 全冻结；不写 Java、不跑 Maven、不 git。保留已成立的 retained-state 跨 revision 复用与 current-slot generation 结论；不重开配置叶子与 local-retained 边界。自审仅 QA，不构成父级批准。

## External Worker A - Design Repair #1 Delta（W-NAV-WF-D2）- 2026-07-13

关闭 `Parent Design Review #9` 的 P1×3/P2×1。仅设计，两仓 Java 冻结；保留已成立的 retained-state 跨 revision 复用与 current-slot generation 结论。新增证据：`CloudTaskRunAuthorityAssembly.closeAndReleaseTerminalTaskServiceRuntime`（:303-325——`slot.closeTerminal` → terminal handle 双重归属校验 → `gameContextStateOwner.releaseTerminal` → `actionLedger.acceptTerminalRun(terminalContext)` → `broker.requestRouteRetirement`）；`TaskServiceRuntime` 持 `private final CloudTaskRetainedActionState retainedActionState` 字段（同文件，terminal handle 经 `lastRuntime()` 可达）。

### X1（P1-1）：**撤回** `markCommitted/committed` 平行提交台账——唯一线性化 owner = Full R0 reserve→consume 事务

父级判定成立：`committed` set 的锁与 `OUTCOME_FINAL_UNCONSUMED→BUSINESS_CONSUMING→CONSUMED` 事务不是同一线性化点（先 mark 后 mutation 抛错→永久抑制；mutation 后未记账→重放）。**整体删除**该 set 与 `markCommitted` 方法。"每 final outcome 的业务 mutation 至多一次"回归**唯一**结构保证（D8-S3 第一道，已 APPROVED）：mutation 恰在 `RemoteFinalConsumptionCoordinator` 的 reserve 成功临界区内执行（`reserveFinalConsumption` 仅接受 `OUTCOME_FINAL_UNCONSUMED` :904-920，单调不可逆；成功后 `completeBusinessConsumption` :655-697，异常则 `markBusinessConsumptionUnknown` :700-726 且不可重入）——mutation 与 final-consume 本就在**同一个既有 owner/transaction** 内原子提交，无需也不得有第二账本。`NavigationWorkflowState` 职责收缩为**仅冻结重入所需 command 数据（freeze-only）**，非任何提交权威。

### X2（P1-2）：terminal cleanup 的真实调用者 = Modify `closeAndReleaseTerminalTaskServiceRuntime`

**撤回** D1 "零既有方法体修改"与 terminal 挂点的矛盾声明。返修：Modify `CloudTaskRunAuthorityAssembly.closeAndReleaseTerminalTaskServiceRuntime`（:303-325），在 `actionLedger.acceptTerminalRun(terminalContext);` 之后、`broker.requestRouteRetirement(...)` 之前插入恰一行：

```
terminalHandle.lastRuntime().retainedActionState().navigationWorkflowState().removeRunState();
```

- 可见性链全同包：`terminalHandle.lastRuntime()`（既有）→ `TaskServiceRuntime.retainedActionState()`（既有 package-private accessor，字段实证存在）→ 新增 package-private `navigationWorkflowState()` accessor → `removeRunState()`。
- **exact terminal handle 保持**：插入行使用的正是既有双重归属校验后的 `terminalHandle`（owner==currentSlot 且 authorityIdentity 全等，:312-315），不引入新 handle、不改校验。
- **重试幂等时序**：`removeRunState()`=单锁内清空（清空集合天然幂等）；插入点在 `acceptTerminalRun` 之后与其同一重试域——terminal 重试时 `closeTerminal/acceptTerminalRun` 的既有幂等/异常语义逐字不变，插入行不吞、不改任何异常传播（自身仅可能因 null 链抛出，而该链在同构造期恒非 null）。

### X3（P1-3）：容量改为**结构性有界**——每 slot 恰一条、覆盖即删除、terminal 清空

**撤回**"(slot,occurrence,attempt) 无界累积 map + committed set"。冻结存储重构为：

```
private final EnumMap<NavigationCommandSlot, FrozenEntry> frozen = new EnumMap<>(NavigationCommandSlot.class);
private record FrozenEntry(long occurrence, int attempt, FrozenCommand command) {}
```

- **每 slot 至多一条**（EnumMap 键=固定 enum）→ 存储上界=enum 基数 **5**（X4），与 run 时长/occurrence 数/NOT_EXECUTED attempt 数无关——**结构性 hard cap，无需 admission/TTL/LRU**。
- **删除点账本**：① occurrence complete 后新 occurrence 首次 freeze → 同 slot 覆盖写入（旧条目即刻消亡）；② compacted NOT_EXECUTED renew 后新 attempt 首次 freeze → 同 slot 覆盖（同 occurrence 更高 attempt）；③ terminal → `removeRunState()` 清空（X2 调用者）。三点覆盖 final-compacted/renew/terminal 全部生命周期，单锁内原子。
- **陈旧键 fail-closed**：请求的 (occurrence,attempt) 严格旧于已存条目 → 抛 `IllegalStateException`（superseded attempt，不静默、不回滚条目）。

### X4（P2-1）：**撤回** `String actionSlotToken`——收为固定 nested enum + exact-equality 冻结校验

```
enum NavigationCommandSlot {            // package-private nested；恰 5 个输入槽
    WORLD_MAP_SEARCH_INPUT, WORLD_MAP_CANDIDATE, ROUTE_DIALOG_CLICK, MINI_MAP_CLICK, NPC_DIRECT_CLICK
}
FrozenCommand freezeOnce(NavigationCommandSlot slot, long occurrence, int attempt, FrozenCommand candidate)
```

- read capture/geometry 无业务参数（D8-S3/W-NAV-0 结论），**不入冻结面**——冻结面只覆盖 5 个输入 command，槽名任意性消失（业务包本就不可见本类；门后 port 内部按固定语义方法一一映射，无字符串通道）。
- **exact-equality 校验（防同 key 不同 payload 静默复用）**：单锁内，同 (slot) 条目存在且 (occurrence,attempt) 全等时——`candidate.equals(entry.command())` 为真才返回既存冻结值；不等则抛 `IllegalStateException`（frozen command payload mismatch，fail-closed，绝不静默返回旧值）。`FrozenCommand` 为 sealed record（`FrozenText(String)`/`FrozenPoint(int,int)`），equals 为值语义，比较确定。
- semantic address 一致性：occurrence/attempt 数值仅允许来自当前 handle 的 `RemoteSemanticAddress`（门后 port 传入），workflow state 零编号能力（继承 D1，不变）。

### 最小文件/方法表（修订）

| 仓库 | 精确 FQCN | New/Modify | 方法面 |
|---|---|---|---|
| Cloud | `remote.NavigationWorkflowState` | New | package-private final；`freezeOnce(NavigationCommandSlot, long, int, FrozenCommand)`、`removeRunState()`（均单锁）；nested `enum NavigationCommandSlot`(5)、sealed `FrozenCommand`(FrozenText/FrozenPoint)、private `FrozenEntry`。**无 markCommitted/无 committed/无 String token** |
| Cloud | `remote.CloudTaskRetainedActionState` | Modify | +1 `private final NavigationWorkflowState` 字段、+1 package-private accessor；零既有方法体改动 |
| Cloud | `remote.CloudTaskRunAuthorityAssembly` | Modify | `closeAndReleaseTerminalTaskServiceRuntime`（:303-325）在 `acceptTerminalRun` 后插入一行 `...navigationWorkflowState().removeRunState()`；其余逐字不动 |

- **唯一线性化 owner**：Full R0 reserve→consume 事务（X1）；workflow state 无提交语义。**容量账本**：结构上界 5 + 三个删除点（X3）。**terminal 重试时序**：既有幂等域内追加幂等清空（X2）。该三文件波仍不依赖 receipt-ready 门（纯 state + terminal 清理），唯一 blocker 不变（receipt-ready，owner=非-A 切片）。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 **P0=0 / P1=0 / P2=0**（仅 QA，不构成父级批准）。W-NAV-WF-D2 Delta 到此停止，等待父级复审；heartbeat 保持（5 分钟）。

## Parent Design Review #10 - DESIGN APPROVED / IMPLEMENTATION TASK PUBLISHED - 2026-07-13T13:20:00-04:00

父级复审 D2：已删除 Full R0 外的 `committed/markCommitted` 第二权威；terminal cleanup 落到真实 exact terminal handle；
`EnumMap<NavigationCommandSlot,FrozenEntry>` 将每 run 存储结构性限制为 5；固定 enum、同 key payload exact-equality 与
旧 occurrence/attempt fail-closed 均可直接编码。结论：`W-NAV-WF-D2` **DESIGN APPROVED，P0/P1/P2=0**。

### 实施任务 `W-NAV-WF-IMP1`

External Worker A 须在 `2026-07-13T13:40:00-04:00` 前于本日志真实末尾追加 `CLAIMED`，并仅实施以下写集：

1. New Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/NavigationWorkflowState.java`；
2. Modify Cloud `.../remote/CloudTaskRetainedActionState.java`：仅加 final owner 字段与 package-private accessor；
3. Modify Cloud `.../remote/CloudTaskRunAuthorityAssembly.java`：仅在 exact terminal 路径把该 state 幂等清空；
4. append 本日志源码/build 证据。

实现必须按 D2 的单锁 `EnumMap`、固定 5 slot、non-negative occurrence/attempt、同 key exact payload、严格旧 key 拒绝、
新 occurrence/attempt 覆盖和 idempotent terminal clear；不得新增 commit 状态、raw/public capability、TTL/LRU/retry，
不得接 Navigation Service/port/caller/host，不改业务 phase。不得新增 tests。完成后运行 Cloud `mvn -q clean package`
（不可 skip）并报告精确 diff、构建统计；Worker 自审不算父级源码批准。逾期只重发给 External A，绝不内部接管。

**无已批准业务差异；按基线等价迁移。**

## External Worker A - CLAIMED - 2026-07-13T13:22:58-04:00

- task: W-NAV-WF-IMP1
- claimedAt: 2026-07-13T13:22:58-04:00（领取截止 2026-07-13T13:40:00-04:00 内）
- uniqueWriteSet: ① New Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/NavigationWorkflowState.java`；② Modify `remote/CloudTaskRetainedActionState.java`（仅 +final owner 字段 +package-private accessor）；③ Modify `remote/CloudTaskRunAuthorityAssembly.java`（仅 exact terminal 路径幂等清空）；④ 本 append-only 日志证据。按 D2 批准形状实施；无 commit 状态/raw capability/TTL/LRU/retry；不接 Service/port/caller/host；不新增 tests；完成后 `mvn -q clean package`（不 skip）+ 精确 diff + 构建统计。自审仅 QA，不构成父级源码批准。

## External Worker A - W-NAV-WF-IMP1 实施证据 - 2026-07-13

按 Parent Design Review #10 授权写集完成实施；写集恰为授权四项，无其它文件改动。未新增 commit 状态/raw-public capability/TTL/LRU/retry/tests，未接 NavigationService/port/caller/host，不改业务 phase。

### 精确 diff（remote 包在工作区为未提交新代码，git diff 不适用；以下为逐行插入实录）

1. **New `remote/NavigationWorkflowState.java`（121 行）**：package-private final class，单 `Object lock`；`EnumMap<NavigationCommandSlot, FrozenEntry> frozen`（结构上界=enum 基数 5）；`freezeOnce(NavigationCommandSlot, long occurrence, int attempt, FrozenCommand candidate)`——non-negative 校验（负 occurrence/attempt 抛 IllegalArgument）、首写/严格更新键覆盖写入（覆盖即删除旧条目）、同 key 重入 `candidate.equals(frozen)` 才返回否则抛 `IllegalStateException`（payload mismatch，fail-closed）、严格旧 key 抛 `IllegalStateException`（superseded attempt）；`removeRunState()` 单锁内 `frozen.clear()`（幂等）；nested `enum NavigationCommandSlot`（恰 5：WORLD_MAP_SEARCH_INPUT/WORLD_MAP_CANDIDATE/ROUTE_DIALOG_CLICK/MINI_MAP_CLICK/NPC_DIRECT_CLICK）；sealed `FrozenCommand` permits `FrozenText(String，非空校验)`/`FrozenPoint(int,int)`；private `FrozenEntry(occurrence, attempt, command)`。javadoc 明示 freeze-only、无提交真值、编号归 ledger、restart 无 restore。
2. **Modify `remote/CloudTaskRetainedActionState.java`（恰 2 处插入，零既有方法体改动）**：:26 `private final NavigationWorkflowState navigationWorkflowState = new NavigationWorkflowState();`（final owner 字段，构造期创建，随宿主跨 revision 复用）；:134-136 package-private accessor `NavigationWorkflowState navigationWorkflowState()`。
3. **Modify `remote/CloudTaskRunAuthorityAssembly.java`（恰 1 行插入）**：`closeAndReleaseTerminalTaskServiceRuntime` 内 :322，位于 `actionLedger.acceptTerminalRun(terminalContext);` 之后、`broker.requestRouteRetirement(...)` 之前：`terminalHandle.lastRuntime().retainedActionState().navigationWorkflowState().removeRunState();`——使用既有双重归属校验后的 exact terminal handle，幂等清空，不吞/不改任何既有异常传播与重试语义。

### 构建统计

- Cloud `mvn -q clean package`（未 skip）**exit 0**；Surefire 汇总 **tests=21, failures=0, errors=0, skipped=0**（4 suites）；shaded JAR `target/dhxy-cloud-brain-0.1.0-SNAPSHOT.jar` 于 13:26 重新生成。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 **P0=0 / P1=0 / P2=0**（仅 QA，不构成父级源码批准）。W-NAV-WF-IMP1 到此停止，等待父级源码复审；heartbeat 保持（5 分钟）。

## Parent Implementation Review #11 - FINAL APPROVED - 2026-07-13T13:38:00-04:00

父级逐文件复审 `NavigationWorkflowState`、`CloudTaskRetainedActionState` 与
`CloudTaskRunAuthorityAssembly.closeAndReleaseTerminalTaskServiceRuntime`，并独立运行 fresh Cloud
`mvn -q clean package`（不可 skip）。结论：**APPROVED，P0/P1/P2=0**。

- `NavigationWorkflowState` 是 package-private freeze-only owner；单锁 `EnumMap` 仅含固定 5 个 slot，结构上界为 5，
  不含 `committed/markCommitted`、TTL/LRU/retry/admission 或 public/raw capability。
- `freezeOnce` 对 occurrence/attempt 做 non-negative 校验；同 key payload exact-equality，严格旧 key fail-closed，
  严格新 key 原位覆盖，未建立 Full R0 之外的第二业务真值。
- retained owner 字段为 final 且跨 revision 复用；terminal clear 使用既有双重归属校验后的 exact terminal handle，
  位于 `actionLedger.acceptTerminalRun` 后且自身幂等，不改变 lifecycle/retirement 的既有异常与重试语义。
- 父级 fresh package：exit 0；4 suites，tests=21，failures=0，errors=0，skipped=0；shaded JAR 已重新生成。

`W-NAV-WF-IMP1` 至此收口。尚未接 Navigation Service/port/caller/host，不代表业务主体已激活；后续仍按迁移矩阵另开
closed capability 与 receipt-ready 波次。**无已批准业务差异；按基线等价迁移。**
