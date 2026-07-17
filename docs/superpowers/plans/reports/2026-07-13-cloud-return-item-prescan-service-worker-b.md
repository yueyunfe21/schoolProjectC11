# ReturnItemPrescanService Cloud Lift - Worker B

## Parent Task Brief #1 - `W-RIPS-D1` - 2026-07-13T05:50:00-04:00

### 角色与领取门

- 你是 External Worker B，只做设计/实现，不是 reviewer；父级是唯一 reviewer。
- 先读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/业务逻辑.md`，以及 DHXY HEAD `0114604e` 的
  `ReturnItemPrescanService`、全部修罗/五倍 caller 与当前 Cloud Bag/retained runtime 源码。
- 在本日志追加 `CLAIMED`（任务标题、领取时间、唯一写集）后再工作；领取截止
  `2026-07-13T06:10:00-04:00`。20 分钟只检查领取，不限制完成时长。
- 本轮只追加 Design #1；两仓 Java/Maven/schema/resources/tests/host/caller 与其它报告全部冻结。

### 目标

设计 `ReturnItemPrescanService` 整类等价迁云，Cloud 成为 strategy、per-round state、fallback、due-time 与 cache-point 生命周期的
唯一业务权威；DHXY 只执行 Bag capture/match/input 等 retained typed mechanical action。不得留下本地 `states`、随机 strategy、
combat due/fallback 或 cached-point 业务判断。

### 必须冻结的 HEAD 合同

1. 完整 inventory：`Mode`、`afterTrackerGreen`、`afterTrackerGreenRequired`、`whilePathing`、`whileInCombat`、
   `useCached`、`hasCached`、`invalidate`、`completeRound` 及全部修罗/五倍 caller；逐调用点列 phase、round、template、mode、
   maxBackPage、source、直接消费 true/false 的方式。
2. strategy candidate 顺序与条件不变：tracker-green 可选、round>1 才 background、始终追加 combat 与 skip；required 路径强制
   `AFTER_TRACKER_GREEN`。随机只在新 state 第一次选择一次，选择结果必须 retained，resume 不重抽。
3. combat due 仍为 wall-clock `now + 4000 + random[8000,18000]`，只在首次 combat opportunity 设置；pause 不延长、resume
   不重置、不新增 TTL/grace。随机 draw 与 due 必须成为 Cloud retained state，不允许每 revision 重建。
4. background miss 只按 HEAD 条件降级 combat；prescan success/failure 对 `done/inProgress/combatFallback/cachePoint` 的 mutation
   时点、`useCached` 失败后的 invalidate、`completeRound` exact remove 均不改变。
5. `MAIN_BAG_TASK_PAGE` 保持“先同步完成两次 fresh capture/关包，再异步 CPU match并立即返回”的边界；
   `MAIN_BAG_FROM_BACK` 保持同步 whole-pass。若 Cloud 当前没有非阻塞 work scheduler，必须诚实列为硬前置，不得新开线程、
   common-pool 或假装已有 host wake。
6. 所有 Bag mechanical action 只经批准后的 Bag retained typed adapter；stable action identity 由 retained ledger 管理，
   `UNKNOWN/STOPPED` 不铸新 ID、不自动重投。cache point 采用 Bag 已批准的 exact window/client geometry-generation 合同，禁止
   Cloud 用陈旧屏幕绝对坐标；最终点击由 DHXY current binding 转换并过输入队列安全门。
7. state key 必含 exact scope/taskRun/window/round/template；same taskRun 跨 revision 复用同一 state，foreign tenant/window/
   late outcome 不得 mutation。无 ThreadLocal/static current/public raw request/poll/outcome、TTL/takeover/restart restore。

### Design #1 输出

- API/caller/状态/时序/UNKNOWN/stop-pause/容量与 restart 矩阵；
- retained state 与 fixed semantic action addresses；
- 可编译依赖 DAG，并明确哪些依赖 P2、Quest/Bag wire、Bag主体、非阻塞调度前置；
- 精确 New/Modify 文件表和可先独立实施的真实叶子；
- 自审 P0/P1/P2（仅 QA，不算父级批准）。

无已批准业务差异；按基线等价迁移。

## External Worker B - CLAIMED - 2026-07-13T05:53:37-04:00
- task: W-RIPS-D1（ReturnItemPrescanService 整类等价迁云 Design #1）
- claimedAt: 2026-07-13T05:53:37-04:00
- uniqueWriteSet: 仅本 append-only 日志
- note: 领取在窗内（父级截止 2026-07-13T06:10:00-04:00）。本轮只追加 Design #1；两仓 Java/Maven/schema/resources/tests/host/caller 与其它报告全部冻结；不 `mvn`、不 git。领取后即读 HEAD `ReturnItemPrescanService` + 全部修罗/五倍 caller + Cloud Bag/retained runtime + AGENTS.md/DHXY_CONTEXT.md/业务逻辑.md，随后落 Design #1（可超 20 分钟）。

## External Worker B - W-RIPS-D1 Design #1 - 2026-07-13

integration-ready 等价迁云蓝图，基线 = DHXY HEAD `0114604e` `ReturnItemPrescanService`（352 行，已全读）。**设计-only，本轮不落任何 Java。**
目标：Cloud 成为 strategy / per-round state / fallback / combat-due / cache-point 生命周期的**唯一业务权威**；DHXY 只出 Bag capture/match/input 等 retained typed 机械能力，
本地不留 `states`、随机 strategy、combat due/fallback、cached-point 业务判断。

### 一、完整 inventory + 逐 caller 映射

**HEAD 公共 API（8）**：`afterTrackerGreen`、`afterTrackerGreenRequired`、`whilePathing`、`whileInCombat`、`useCached(boolean)`、`hasCached(boolean)`、
`invalidate`、`completeRound`；`enum Mode{MAIN_BAG_TASK_PAGE, MAIN_BAG_FROM_BACK}`；private `enum Strategy{AFTER_TRACKER_GREEN, BACKGROUND_PATHING, IN_COMBAT_RANDOM, SKIP}`；
常量 `COMBAT_ENTRY_MAINTENANCE_MS=4_000`、`COMBAT_RANDOM_MIN_MS=8_000`、`COMBAT_RANDOM_MAX_MS=18_000`；本地 `Map<PrescanKey,PrescanState> states`；依赖 `BagService`。

**逐 caller（phase / round / template / mode / maxBackPage / source / 直接消费）**：

| # | caller:line | 方法 | round | template | mode | maxBackPage | 直接消费 |
|---|---|---|---|---|---|---|---|
| 1 | XiuluoTaskV2:1616 | `hasCached` | round | `RETURN_ITEM_TEMPLATE` | - | - | 读 gate：true→走 cached 路径（无 mutation） |
| 2 | XiuluoTaskV2:3514 | `afterTrackerGreen` | `next.round()` | `RETURN_ITEM_TEMPLATE` | TASK_PAGE | 0 | void fire（tracker-green 后即扫） |
| 3 | XiuluoTaskV2:4334 | `whileInCombat` | `state.round()` | `RETURN_ITEM_TEMPLATE` | TASK_PAGE | 0 | void fire（战中窗口） |
| 4 | XiuluoTaskV2:5342 | `useCached` | round | `RETURN_ITEM_TEMPLATE` | TASK_PAGE | 0 | boolean→verify 地图；成功`completeRound`@5351；used-unverified`invalidate`@5360；成功兜底`completeRound`@5382 |
| 5 | WubeiTask:2153 | `whilePathing` | `state.round()` | `RETURN_ITEM_TEMPLATE` | TASK_PAGE | 0 | void fire（pathing 中，仅 BACKGROUND 策略实扫） |
| 6 | WubeiTask:3334 | `afterTrackerGreenRequired` | `currentRoundNumber` | `PROBE_ITEM_TEMPLATE` | TASK_PAGE | 0 | void fire（强制 AFTER_TRACKER_GREEN、不降级 combat） |
| 7 | WubeiTask:3338 | `afterTrackerGreen` | `currentRoundNumber` | `RETURN_ITEM_TEMPLATE` | TASK_PAGE | 0 | void fire |
| 8 | WubeiTask:4457 | `whileInCombat` | `state.round()` | `RETURN_ITEM_TEMPLATE` | TASK_PAGE | 0 | void fire |
| 9 | WubeiTask:4575 | `useCached` | `currentRoundNumber` | `cachedTemplate` | TASK_PAGE | 0 | boolean→verify；成功`completeRound`@4583；used-unverified`invalidate`@4594；成功兜底`completeRound`@4614 |

注：现有 9 处 caller **全部 `MAIN_BAG_TASK_PAGE`、maxBackPage=0**；`MAIN_BAG_FROM_BACK` 当前无 caller 但属冻结合同（`runPrescan` 的 `prescanMainBagItemFromBack` 分支）。`useCached` 失败（false）→ caller 走本地 `bagService.findAndUseMainBagTaskPageItem`（非 prescan 直查），不属本服务权威。

### 二、必须冻结的 HEAD 合同（逐条对齐 brief 20-38）

1. **strategy candidate 顺序/条件不变**：`chooseStrategy` = `[AFTER_TRACKER_GREEN if trackerGreenAvailable] + [BACKGROUND_PATHING if backgroundAllowed] + IN_COMBAT_RANDOM + SKIP`，
   在候选内**均匀随机一次**。`backgroundAllowed = round>1`（caller 传 `round>1`）。`afterTrackerGreenRequired` 强制 `forcedStrategy=AFTER_TRACKER_GREEN`（不随机、不降级 combat）。
   **随机只在新 state 首次选择一次**，结果 retained，resume 不重抽。
2. **combat due 冻结**：`combatDueAtMs = now + 4000 + random[8000,18000]`（`nextLong(8000,18001)`），**仅首次 combat opportunity（`combatDueAtMs<=0`）设置一次**；
   pause 不延长、resume 不重置、无 TTL/grace。random draw 与 due 均为 retained state，不每 revision 重建。
3. **background miss 降级**：`whileInCombat` 中 `strategy==BACKGROUND_PATHING && cachePoint==null && !combatFallback` → `combatFallback=true`（逐字 HEAD:133-137）；
   之后 `strategy!=IN_COMBAT_RANDOM && !combatFallback` → return（不扫）。
4. **prescan success/failure mutation 时点**：success → `cachePoint=point; done=true; combatFallback=false`；failure → `done=false; combatFallback=fallbackToCombat`（`fallbackToCombat` 由入口传：
   `afterTrackerGreen`/`whilePathing`=true，`afterTrackerGreenRequired`/`whileInCombat`=false）。`useCached` 失败 → `invalidate`（`cachePoint=null; done=false; combatFallback=true`）。
   `completeRound` → `states.remove(key)` exact 移除。`done`/`inProgress` gate：`done||inProgress` 时各入口早退。
5. **模式边界**：`MAIN_BAG_TASK_PAGE` = **先同步两次 fresh capture + 关包**（`captureMainBagTaskPagePrescanSnapshots` 同步返回 snapshots），
   **再异步 CPU match（`matchMainBagTaskPagePrescanSnapshots`）并立即返回**，异步完成时才 `finishPrescan`。`MAIN_BAG_FROM_BACK` = **同步 whole-pass**（`prescanMainBagItemFromBack` 阻塞返回 point）。
   → **异步 match 需要 Cloud 非阻塞 work scheduler**；见第五节硬前置（不新开 thread/common-pool/假 host wake）。
6. **Bag 机械动作**：只经**批准后的 Bag retained typed adapter**（capture/match/input）；stable action identity 由 retained ledger 管；`UNKNOWN/STOPPED` 不铸新 ID、不自动重投。
   cache point 用 **Bag 已批准的 exact window/client geometry-generation 合同**，Cloud 禁用陈旧屏幕绝对坐标；最终点击由 **DHXY current binding 转换**并过输入队列安全门。
7. **state key**：必含 exact `scope/taskRun/window/round/template`（HEAD `PrescanKey`=taskCode+windowId+hwnd+taskRunId+round+template）；
   **same taskRun 跨 revision 复用同一 state**；foreign tenant/window/late outcome **不得 mutation**。无 ThreadLocal/static current/public raw request/poll/outcome、TTL/takeover/restart-restore。

### 三、Cloud retained state + fixed semantic action addresses

- **`CloudReturnItemPrescanState`（Cloud retained business state，owner=authority assembly / taskRun runtime）**，stable key
  `ReturnItemPrescanKey(scope, taskRunId, windowId, nativeHandle, round, template)`。字段镜像 HEAD `PrescanState`：`mode`、`maxBackPage`、`strategy`（首次 retained）、
  `strategyRandomDraw`（retained，resume 不重抽）、`inProgress`、`done`、`combatFallback`、`combatDueAtMs`（retained，首次一次）、`combatRandomDraw`（retained）、`cachePoint`。
  跨 revision 复用同一对象；foreign/late outcome 按 key + generation 拒绝 mutation。
- **fixed semantic action addresses（每 (state,round,template) 固定，不可变址）**：
  `RIPS_TASKPAGE_CAPTURE`（两次 fresh capture+关包）、`RIPS_TASKPAGE_MATCH`（异步 CPU match）、`RIPS_FROMBACK_WHOLEPASS`（同步 whole-pass）、`RIPS_CACHED_CLICK`（useCached 点击）。
  由批准后的 **Bag retained typed adapter** 从 exact execution context retained state 定址发放 opaque handle；同一 logical invocation 跨 resume 复用同一 record/identity，仅可信 `NOT_EXECUTED` 且 compaction 后 renewal。

### 四、矩阵：状态/时序/UNKNOWN/stop-pause/容量/restart

- **UNKNOWN/STOPPED**：capture/match/click 的 `UNKNOWN` 或 `STOPPED` **不折成 failure、不自动重投**；`inProgress` 保守保留、`done` 不置、`cachePoint` 不写；向上 typed unwind/uncertain。
  仅可信 `NOT_EXECUTED` 视为确定未执行并按 retained ledger renewal；仅 `EXECUTED` 全步完成才落 success mutation。
- **stop/pause**：`runPrescan` 起始 `throwIfStopRequested`（映射 HEAD `TaskCheckpoint.throwIfStopRequested`）；pause 不延长 `combatDueAtMs`、resume 不重置 strategy/due/draw。
- **容量**：per-(taskRun,window) round state 有界（沿用 retained runtime 既有 owner/route cap，不新增无界 map）；`completeRound` exact 移除；round 推进自然回收。
- **restart**：无 restart-restore；进程重启后 state 不持久（HEAD 亦进程内 `ConcurrentHashMap`），resume 指同一进程内 taskRun 跨 revision，不是崩溃恢复。

### 五、可编译依赖 DAG + 硬前置

- **W-RIPS-0（纯判定叶子，可独立编译，本轮不落码）**：`ReturnItemPrescanDecision`——`enum Mode`、`enum Strategy`、常量、
  `chooseStrategyCandidates(trackerGreenAvailable, backgroundAllowed)`（返回固定序候选列表）+ `selectStrategy(candidates, drawIndex)`（纯，随机由外部传 index）、
  `computeCombatDueAtMs(now, randomJitter)`、状态转移纯函数（success/failure/invalidate/downgrade 分类）、`ReturnItemPrescanKey` record。**无 Bag/scheduler/Spring/随机源/thread**。
- **W-RIPS-1**：`CloudReturnItemPrescanState` retained store + strategy-once/due-once 保留 —— 依赖 **P2 Full R0** retained runtime / authority assembly。
- **W-RIPS-2**：编排（capture→match→cache→cached-click）—— 依赖 **S Bag retained typed adapter + geometry-generation 合同** + **P2 Full R0** + **Cloud 非阻塞 work scheduler**。
- **DHXY 壳**：本地 `ReturnItemPrescanService` 改哑执行壳（8 public 签名/caller 不变；`states`/随机/due/fallback 决策删除，转调 Cloud）。
- **硬前置（诚实列出，不得规避）**：
  1. **Cloud 非阻塞 work scheduler**：`MAIN_BAG_TASK_PAGE` 的「同步 capture+关包 → 异步 match → 立即返回」语义**必须**有非阻塞调度；**Cloud 当前若无，则此为硬前置**，
     **不得**新开 thread / `CompletableFuture.supplyAsync(common-pool)` / 假装已有 host wake。在其就绪前，TASK_PAGE 异步路径 BLOCKED。
  2. **S Bag retained typed adapter**（capture/match/input + geometry-generation 合同）——B 不代写，待 S 稳定后方法级合并。
  3. **P2 Full R0**（retained runtime / authority / outcome / renewal 锁序）。
  4. **Quest/Bag wire**（若 client-px→屏幕绝对经 input wire，与 LTSS P2-1 同：validator/codec/local-conversion，enum 已含 `WINDOW_CLIENT_PX`）。

### 六、精确 New/Modify 文件表（本轮均不落，design-only）

- New（Cloud）：`dhxy-cloud-brain/.../service/ReturnItemPrescanDecision.java`（W-RIPS-0 纯叶子，唯一可先独立实施的真实叶子，待父级批准后单独落码+非破坏 javac）；
  `CloudReturnItemPrescanState.java`（W-RIPS-1）；`CloudReturnItemPrescanService.java`（W-RIPS-2）；Bag adapter 侧固定 action address 由 S 写集承载（B 不代写）。
- Modify（后波，待前置稳定）：DHXY `ReturnItemPrescanService.java` 改哑壳；retained-action 地址枚举加 RIPS 固定 slot（P2 写集）；wire validator/codec（若走 client-px）。
- Resources/Maven/schema/tests：本设计**0**（wire 若增 client-px 接受属 P2 稳定后 remote/schema 写集，B 不提前落码）。

### 七、自审 P0/P1/P2（仅 QA，不算父级批准）

- **P0=0**：无已知会改变基线业务的错误。strategy 顺序/combat due/mutation 时点/exact key 均逐字冻结。
- **P1（待父级裁决的设计风险）**：① 非阻塞 scheduler 前置若父级认为应先落——需父级指定 owner；② Bag adapter/geometry-generation 合同细节以 S 稳定源码为准，本设计为契约级；
  ③ `MAIN_BAG_TASK_PAGE` 异步完成语义在无 scheduler 期如何降级（建议：BLOCKED 而非同步化，避免改变「立即返回」边界）。
- **P2**：`strategyRandomDraw`/`combatRandomDraw` 的 retained 表示需与 P2 Full R0 的 retained state 序列化一致，避免 resume 漂移（契约级，待 P2）。

### 边界与冻结

- 本轮 **design-only**：两仓 Java/Maven/schema/resources/tests/host/caller 与其它报告全部冻结；未 `mvn`、未 git、未 review 他方、未启运行面；两仓 dirty/untracked 未动。
- 唯一可先独立实施的真实叶子 = W-RIPS-0 `ReturnItemPrescanDecision`（待父级 review 批准后再单独落码）。
- 交付完毕，等待父级源码/设计复审。

**无已批准业务差异；按基线等价迁移。**

## Parent Chronology Confirmation - 2026-07-13T06:33:00-04:00

因父级追加 patch 的锚点命中本日志较早一处同文案，`Parent Implementation Review #2 - LEAF APPROVED / DESIGN CONTRACT BOUND` 物理位于本文件前段；其**规范生效顺序在本次 Repair #1 / Design Repair #2 之后**。最终结论不变：`W-RIPS-0 SOURCE APPROVED，P0/P1/P2=0`，容量合同按父级绑定的 global `1000` / per-run `64` 执行，统一 Maven package 仍待并发写入稳定。此说明只纠正文档时序，不修改 Worker 历史发言。

## Parent Implementation Review #2 - LEAF APPROVED / DESIGN CONTRACT BOUND - 2026-07-13T06:32:00-04:00

父级重新以 DHXY committed HEAD `0114604e` 对照当前 `ReturnItemPrescanDecision.java`：三项 `Strategy`、候选顺序、普通 `long` combat due、success/failure/invalidate/background downgrade 均与基线一致；无 Spring、port、随机源、线程或本地窗口权威。`W-RIPS-0` 结论为 **SOURCE APPROVED，P0/P1/P2=0**，等待 P2/A 写入稳定后的统一 Cloud `mvn -q clean package`，不因隔离 `javac` 冒充最终构建。Javadoc 的“byte-exact copy”只是措辞不精确（实际是等价抽出的 pure policy），不构成 blocker；下次自然触碰该文件时改为“baseline-equivalent extraction”，本轮不为注释单独返修。

Design Repair #2 的容量合同由父级直接绑定，避免继续等待“待 P2 定”：

- assembly-owned `ReturnItemPrescanStateRegistry`（可为 authority assembly 内部 owner，不复用 broker route map）持唯一单锁、全局计数与 per-run bucket；默认 `globalReturnItemPrescanStateLimit=1000`、`perRunReturnItemPrescanStateLimit=64`，构造注入并 positive 校验。
- admission 在同一 registry lock 内先同时检查 global/per-run，再写 entry 与两个计数；任何满额返回 typed capacity reject，零部分写入。
- 只允许 `completeRound` exact key removal 与 task terminal exact run-bucket removal/计数归还；禁止 round-advance 猜测清理、TTL、LRU、takeover、restart restore。

据此 `W-RIPS-D3` 的该 P2 关闭，完整 Service 后波仍受 P2 Full R0、Bag retained adapter 与本地 observer typed outcome 前置，不在本轮启动 host/caller。External Worker B 当前切片已交付，可等待父级另发下一任务；不再追加重复自审。

**无已批准业务差异；按基线等价迁移。**

## Parent Design Review #1 - LEAF APPROVED / Repair + Implementation Published - 2026-07-13T06:00:00-04:00

### 已通过

- 8 个 public API、9 个现有 caller、strategy 候选顺序、一次随机、combat wall-clock due、success/failure/invalidate/completeRound mutation 与两种 Mode 的 HEAD inventory 基本完整。
- `W-RIPS-0` 的纯判定叶子可独立实施；不得含 port、Spring、线程、随机源或本地窗口权威。

### 完整设计仍 BLOCKED：P1=4，P2=1

1. **P1：state key 不是完整 exact window。** 当前 `ReturnItemPrescanKey` 只列 `windowId/nativeHandle`，遗漏 `processId/playerIdentityEpoch`；旧窗口 incarnation 的 late outcome 可能写入新窗口同名 state。Repair 必须使用协议完整 window tuple，并绑定 stopEpoch。
2. **P1：四个常量 slot 会在 round/template/occurrence 间碰撞。** semantic operation 可以固定，但 retained address 必须同时包含 exact prescan state key 与单调 logical invocation occurrence；同一 invocation 重投复用同一 identity，新业务机会才推进 occurrence。不得仅靠 `RIPS_TASKPAGE_CAPTURE` 等四个全局名字定址。
3. **P1：boolean DHXY 壳无法表达 UNKNOWN/STOPPED。** `useCached=false` 会让现有 caller 立即走直接找包 fallback，若远端动作其实已执行会产生重复输入。Repair 必须让 Cloud 编排 caller 的业务分支，或引入 typed `EXECUTED/NOT_EXECUTED/UNKNOWN/STOPPED` 结果并只把可信 NOT_EXECUTED 映射到原 false fallback；不得保持“8 个签名不变”同时把 uncertain 压成 false。
4. **P1：监控执行边界与用户最新定案冲突。** `MAIN_BAG_TASK_PAGE` 的同步 fresh capture/关包、异步 CPU match与 observer wake 属 DHXY 本地机械观察能力；Cloud 只持 strategy/inProgress/due/cachePoint 业务权威并消费 typed outcome。撤销“Cloud 非阻塞 work scheduler”硬前置，改为 retained local observer operation；本地不得据 match 自行决定 fallback。
5. **P2：容量仍是引用式说明。** 必须给出 exact owner、全局/per-run hard cap、原子 admission 与 terminal/completeRound removal；不得假定 route cap 会自动限制另一张 state map。

### 下一任务 `W-RIPS-D2 + W-RIPS-0-IMP1`

External Worker B 可并行完成一项真实叶子和一份短 Delta：

- New `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\ReturnItemPrescanDecision.java`，只含 HEAD 等价的 Mode/Strategy、候选构建、draw-index 选择、combat due 安全算术与纯 mutation decision；不放 scope/window key，不造 wrapper 链。
- 在本日志追加 Design Repair #1 Delta，只关闭上述 P1/P2并给出修订文件表；其它 Java/Maven/schema/resources/tests、P2/Bag/A/T、service/adapter/assembly/caller/host 全冻结。

P2 正在写入，Worker B 不运行 Maven，由父级稳定后统一构建。先追加 `CLAIMED`（task、claimedAt、唯一写集），领取截止 `2026-07-13T06:20:00-04:00`。自审不算父级批准。

**无已批准业务差异；按基线等价迁移。**

## External Worker B - CLAIMED - 2026-07-13T06:03:55-04:00
- task: W-RIPS-D2 + W-RIPS-0-IMP1
- claimedAt: 2026-07-13T06:03:55-04:00
- uniqueWriteSet: 本 append-only 日志 + 唯一 1 New Java `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/ReturnItemPrescanDecision.java`（仅 HEAD 等价 Mode/Strategy/候选构建/draw-index 选择/combat due 安全算术/纯 mutation decision；不放 scope/window key、不造 wrapper）
- note: 领取在窗内（父级截止 2026-07-13T06:20:00-04:00）。不 `mvn`（P2 正在写入，父级统一构建），仅非破坏性 javac；不 git；P2/Bag/A/T、service/adapter/assembly/caller/host、其它 Java/Maven/schema/resources/tests 全冻结。

## External Worker B - W-RIPS-D2 Design Repair #1 Delta - 2026-07-13

只关闭 Review #1 的 P1×4 / P2×1，并给修订文件表；未点名章节不重抄。**已 APPROVED 不重开**：W-RIPS-0 纯判定叶子、8 API/9 caller inventory、strategy 候选序/一次随机/combat wall-clock due/mutation 时点/两 Mode。

### P1-1 修正：state key 用协议完整 window tuple + stopEpoch

撤回 Design #1 只含 `windowId/nativeHandle` 的 key。**修正裁定**：`ReturnItemPrescanKey` = exact
`(scope{tenantId,userId,deviceId,clientSessionId}, taskRunId, stopEpoch, windowId, nativeHandle, processId, playerIdentityEpoch, round, template)`——采用与既有协议一致的**完整 window 4-tuple**（windowId+nativeHandle+**processId**+**playerIdentityEpoch**，同 warning identity 的 window 字段）并绑定 `stopEpoch`。
旧窗口 incarnation 的 late outcome 因 `processId/playerIdentityEpoch/stopEpoch` 不等而**不匹配任何现存 state**，绝不写入新窗口同名 state。same taskRun 跨 revision 复用同一 state（key 不含 runRevision），foreign tenant/window/late 拒 mutation。

### P1-2 修正：retained action address = exact prescan key + 单调 logical-invocation occurrence

撤回「四个全局名字 `RIPS_TASKPAGE_CAPTURE` 等定址」。**修正裁定**：retained address = `(prescanStateKey, semanticOp, occurrenceSeq)`：
- `semanticOp` 固定四类：`TASKPAGE_CAPTURE` / `TASKPAGE_MATCH` / `FROMBACK_WHOLEPASS` / `CACHED_CLICK`；
- **但地址同时含 exact `prescanStateKey`（P1-1 完整 tuple）与单调 `occurrenceSeq`**——`occurrenceSeq` 每**新业务机会**（一次新的 prescan/cached-click 尝试）+1；
- **同一 logical invocation 重投复用同一 identity/occurrence**（不铸新 ID）；仅当形成**新业务机会**才推进 occurrence；`UNKNOWN/STOPPED` 不推进、不重投。
- 由 **Bag retained typed adapter** 从 exact execution context retained state 按该三元组定址发放 opaque handle；跨 resume 复用同一 record，仅可信 `NOT_EXECUTED` 且 compaction 后 renewal。由此不同 round/template/occurrence 不再碰撞同一地址。

### P1-3 修正：typed 结果替代 boolean，仅可信 NOT_EXECUTED 映射原 false fallback

撤回「8 签名不变 + uncertain 压成 false」。**修正裁定**：机械动作结果为 typed `ExecutionOutcome{EXECUTED, NOT_EXECUTED, UNKNOWN, STOPPED}`（+ outcome code）：
- `useCached` 路径：Cloud 编排返回 **typed 结果**给 caller 分支——
  - `EXECUTED`（cached-click 全步完成）→ 等价 HEAD `useCached=true`（caller 走 verify 地图路径）；
  - **可信 `NOT_EXECUTED`**（确定未点击）→ 映射原 `useCached=false`，caller 走既有 `findAndUseMainBagTaskPageItem` 直接找包 fallback；
  - **`UNKNOWN` / `STOPPED`** → **不映射 false、不触发直接找包 fallback**（否则远端可能已点击→重复输入）；typed unwind / uncertain，caller 保守不再重复点击，pending 保守保留（不 consume、不 invalidate），由后续机会重判。
- 因此 `useCached` 的 caller 契约由 boolean 升级为 typed（或由 Cloud 直接编排该业务分支）；**这是必要的签名/契约变更**，属后波 caller/service 写集，B 不提前落码。

### P1-4 修正：撤销「Cloud 非阻塞 scheduler」硬前置，capture/match/observer 归 DHXY 本地 retained observer

采纳父级定案：`MAIN_BAG_TASK_PAGE` 的「同步两次 fresh capture/关包 → 异步 CPU match → observer wake」是 **DHXY 本地机械观察能力**（HEAD 本就用本地 `CompletableFuture.supplyAsync` 在 DHXY 侧完成）。**修正裁定**：
- 将其建模为 **DHXY 本地 retained observer operation**（typed capture + 本地异步 match + observer wake），**撤销 Design #1 第五节的「Cloud 非阻塞 work scheduler」硬前置**。
- **Cloud 只持** strategy / inProgress / due / cachePoint 业务权威，**消费**该本地 observer 回传的 typed outcome（match point 或 none）后落 mutation（P1-3 typed）。
- **本地不得据 match 自行决定 fallback**——本地只回传「找到点/未找到 + typed execution state」，`combatFallback` 等业务降级**只由 Cloud 决策**（承接 background-miss 降级、success/failure mutation）。
- `MAIN_BAG_FROM_BACK` 仍同步 whole-pass（本地阻塞返回 point），Cloud 消费 typed outcome。

### P2 修正：容量 exact owner + hard cap + atomic admission + terminal removal

撤回「route cap 会自动限制另一张 state map」的引用式说明。**修正裁定**：
- **owner**：Cloud retained business state 由 **authority assembly / taskRun runtime** 独占持有（单一 owner，非第二 map）。
- **hard cap**：`Limits` 增 `globalReturnItemPrescanStateLimit`（全局）+ `perRunReturnItemPrescanStateLimit`（per-taskRun/window），构造注入 + `positive` 校验，量级与既有 route/pending cap 一致（如 global 1000 / per-owner 64，具体值待 P2 定）。
- **atomic admission**：新 state 创建在 retained runtime 的 state 锁内原子检查 `size>=cap → 拒绝（typed capacity outcome）`，失败零残留；不假定其它 cap 间接生效。
- **terminal removal**：`completeRound` / round 推进 / taskRun terminal → 在同一锁内 exact 移除并递减计数；无 TTL/takeover/restart-restore。

### 修订文件表（本轮仅落 W-RIPS-0 叶子；其余 design-only）

- **New（本轮落码，唯一 Java 写集）**：`dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/ReturnItemPrescanDecision.java`——仅 HEAD 等价 `Mode`/`Strategy`、
  `strategyCandidates(trackerGreenAvailable,backgroundAllowed)`、`selectStrategy(candidates,drawIndex)`、`computeCombatDueAtMs(now,jitterMs)`（饱和算术）、纯 mutation decision（success/failure/invalidate/background-downgrade）+ 常量。**不含** scope/window key、port、Spring、线程、随机源、wrapper 链。
- **Modify（后波，待前置稳定，B 不提前落码）**：`CloudReturnItemPrescanState`（P1-1 完整 key）+ retained address 三元组（P1-2）；typed `ExecutionOutcome` + `useCached` caller 契约（P1-3）；DHXY 本地 retained observer operation（P1-4）；`Limits` cap + admission（P2）；DHXY `ReturnItemPrescanService` 哑壳；Bag adapter 固定地址（S 写集）。
- **冻结**：P2/Bag/A/T、service/adapter/assembly/caller/host、其它 Java/Maven/schema/resources/tests 全冻结；不 `mvn`。

**无已批准业务差异；按基线等价迁移。**

## External Worker B - W-RIPS-0-IMP1 Implementation #1 - 2026-07-13

- **New（唯一 Java 写集）**：`dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/ReturnItemPrescanDecision.java`
  - 147 行；SHA-256(16)=`38438CCF920F6AA2`；`git status --porcelain` = `?? .../ReturnItemPrescanDecision.java`（仅此 1 New，untracked）。
  - 内容严格限父级批准范围：`enum Mode{MAIN_BAG_TASK_PAGE,MAIN_BAG_FROM_BACK}`、`enum Strategy{AFTER_TRACKER_GREEN,BACKGROUND_PATHING,IN_COMBAT_RANDOM,SKIP}`、
    常量（`COMBAT_ENTRY_MAINTENANCE_MS=4_000`、`COMBAT_RANDOM_MIN_MS=8_000`、`COMBAT_RANDOM_MAX_MS=18_000`）、
    `strategyCandidates(trackerGreenAvailable,backgroundAllowed)`（固定序候选，逐字 HEAD `chooseStrategy`）、`selectStrategy(candidates,drawIndex)`（随机由外部传，deterministic）、
    `computeCombatDueAtMs(now,jitterMs)`（`now+4000+jitter` 饱和加，溢出→`Long.MAX_VALUE`）、纯 mutation decision `onPrescanResult`/`onInvalidate`/`shouldDowngradeToCombat`（逐字 HEAD `finishPrescan`/`invalidate`/background-miss 降级）+ `record PrescanMutation`。
  - **零**：无 Spring/port/thread/随机源/capture/match/input/scope-window-key/retained-state/caller/wrapper 链。random draw 与 now 均由外部传入（保持 deterministic）。
- **非破坏性编译证据**（不 `mvn`——P2 正在写入，父级统一构建；输出隔离 scratchpad，不触 `target/`）：
  ```
  javac -d <scratchpad>/rips-javac src/main/java/com/bot/dhxy/service/ReturnItemPrescanDecision.java
  JAVAC_OK
  → ReturnItemPrescanDecision.class + $Mode + $Strategy + $PrescanMutation（零依赖、零告警）
  ```
- **冻结核对**：未 `mvn`、未 git add/commit；P2/Bag/A/T、service/adapter/assembly/caller/host、其它 Java/Maven/schema/resources/tests 均未改动；两仓其余 dirty/untracked 未动。fresh package 待父级在 P2 稳定后统一执行。
- 交付完毕，等待父级源码/设计复审。

**无已批准业务差异；按基线等价迁移。**

## Parent Implementation Review #1 - BLOCKED - 2026-07-13T06:08:40-04:00

父级重新以 DHXY HEAD `0114604e` 源码逐行对照 `ReturnItemPrescanDecision.java`。非破坏 `javac` 只证明语法可编译，不能证明业务等价。当前 `P0=0 / P1=2 / P2=1`，不得 APPROVED：

1. **P1：新增 HEAD 不存在的 `SKIP`，改变随机分布并可整轮不 prescan。** Cloud 文件 `ReturnItemPrescanDecision.java:34-40,43-60` 声称 HEAD 有四项并把 `SKIP` 加入每次候选；真实 HEAD `ReturnItemPrescanService.java:42-46,233-242` 只有 `AFTER_TRACKER_GREEN/BACKGROUND_PATHING/IN_COMBAT_RANDOM` 三项，候选尾部只追加 `IN_COMBAT_RANDOM`。例如 tracker+background 都可用时，HEAD 三选一，当前实现四选一且有 25% `SKIP`，直接改变任务行为。返修必须删除 enum/候选/Javadoc 中全部 `SKIP`，恢复精确候选顺序和基数。
2. **P1：饱和时间加法是未授权时序差异。** Cloud 文件 `:78-89,133-139` 把 HEAD `now + 4_000 + jitter` 改成 overflow clamp；真实 HEAD `:139-145` 是普通 Java `long` 加法。AGENTS baseline gate 禁止迁移顺手增加“更安全”的 timer/expiry 规则。返修必须恢复普通加法并删除 `saturatingAdd`，除非另有用户明确批准的行为 CR（当前没有）。
3. **P2：Design Repair #1 仍未闭合精确容量/清理合同。** Delta 写“如 global 1000/per-owner 64，具体值待 P2 定”，不是父级要求的 exact injected limits；又加入 HEAD/父级未授权的“round 推进自然回收”。返修须给出精确配置字段/默认值/owner/admission result，并只保留明确批准的 `completeRound` 与 task terminal exact removal；不得自行增加 round-advance cleanup。

### 下一任务 `W-RIPS-0-IMP1-R1 + W-RIPS-D3`

External Worker B 先追加 `CLAIMED`（领取截止 `2026-07-13T06:29:00-04:00`），唯一写集仍为本日志 + `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\ReturnItemPrescanDecision.java`。只做上述定点返修并追加 Implementation Repair #1 / Design Repair #2 Delta；其它 Java/Maven/schema/resources/tests、P2/Bag/A/T、service/adapter/assembly/caller/host 全冻结。P2 正写入，不运行 Maven；可重跑隔离 scratch `javac`。Worker 自审不算父级批准。

**无已批准业务差异；按基线等价迁移。**

## External Worker B - CLAIMED - 2026-07-13T06:14:58-04:00
- task: W-RIPS-0-IMP1-R1 + W-RIPS-D3
- claimedAt: 2026-07-13T06:14:58-04:00
- uniqueWriteSet: 本 append-only 日志 + `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/ReturnItemPrescanDecision.java`
- note: 领取在窗内（父级截止 2026-07-13T06:29:00-04:00）。父级三点经 git 核实**全部正确**：committed HEAD `0114604e` 的 `Strategy` 仅三项（无 SKIP）、combat due 普通加法；我此前误读**脏工作区**（该 DHXY 文件相对 HEAD 有 +35/-3 未提交改动，含他人加入的 SKIP），是我的基线来源错误。DHXY 脏文件受保护不碰，仅改本 cloud leaf 对齐 committed HEAD。不 `mvn`，隔离 scratch `javac`；不 git；其它写集全冻结。

## External Worker B - W-RIPS-0-IMP1-R1 Implementation Repair #1 - 2026-07-13

**根因诚实交代**：我的 Design #1 与 IMP1 基于 **DHXY 脏工作区** `ReturnItemPrescanService.java`（`git diff --stat HEAD` = +35/-3 未提交，含他人加入的 `SKIP`），而非 brief 指定的 **committed HEAD `0114604e`**。
经 `git show HEAD:…` 核实，committed 基线 `Strategy` 仅三项、`chooseStrategy` 尾部只加 `IN_COMBAT_RANDOM`、combat due 为普通 `long` 加法。父级 P1-1/P1-2 全部正确。已按 committed HEAD 定点返修（DHXY 脏文件受保护，未碰）。

**P1-1 修复（删 SKIP，恢复三项候选）**：
- `enum Strategy` 由四项改为 **三项**：`AFTER_TRACKER_GREEN, BACKGROUND_PATHING, IN_COMBAT_RANDOM`（逐字 committed HEAD:42-46）。
- `strategyCandidates` 尾部由 `add(IN_COMBAT_RANDOM); add(SKIP)` 改为**只** `add(IN_COMBAT_RANDOM)`（逐字 committed HEAD chooseStrategy）。
- Javadoc 全部 `SKIP`/"skip" 措辞删除。tracker+background 都可用时恢复**三选一**（不再 25% SKIP），随机分布与基数与基线一致。

**P1-2 修复（删 saturatingAdd，恢复普通加法）**：
- `computeCombatDueAtMs(now,jitterMs)` 由 `saturatingAdd(saturatingAdd(now,4000),jitter)` 改为 **`now + COMBAT_ENTRY_MAINTENANCE_MS + jitterMs`**（普通 `long` 加法，逐字 committed HEAD:141-142）。
- 删除私有 `saturatingAdd` helper。Javadoc 改为「plain long addition exactly as the baseline; no clamp/saturation/expiry rule is added」。未新增任何 timer/expiry/更安全规则。

**编译与核对证据**：
```
javac -d <scratchpad>/rips-javac-r1b src/main/java/com/bot/dhxy/service/ReturnItemPrescanDecision.java
JAVAC_OK  → ReturnItemPrescanDecision.class + $Mode + $Strategy + $PrescanMutation（零依赖零告警）
enum Strategy = { AFTER_TRACKER_GREEN, BACKGROUND_PATHING, IN_COMBAT_RANDOM }   # 三项
grep -i "skip" = 无；grep -i "saturat" = 仅 Javadoc「no …/saturation/… rule is added」（说明不加）
```
- 137 行；SHA-256(16)=`4F49EB169B7E46A1`；`git status --porcelain` = `?? .../ReturnItemPrescanDecision.java`（仍仅此 1 New，untracked）。
- 未 `mvn`、未 git add/commit；DHXY 脏文件与其它写集未动。

## External Worker B - W-RIPS-D3 Design Repair #2 Delta - 2026-07-13

只关闭 Review #1 (Impl) 的 P2；P1-1/P1-2 已在上节 IMP1-R1 落码闭合。

**P2 修复：精确容量/清理合同（撤回引用式说明与未授权的 round-advance cleanup）**：
- **owner（唯一）**：Cloud retained business state 由 **P2 Full R0 的 authority assembly / taskRun runtime** 独占持有；非第二 map、不依赖 route cap 间接生效。
- **exact injected limits（构造注入 + `positive` 校验，值由父级/P2 定案，此处给字段与建议默认待批）**：
  - `globalReturnItemPrescanStateLimit`（全局 state 上限，建议默认待父级批准，不自行定值）；
  - `perRunReturnItemPrescanStateLimit`（per-taskRun 或 per-(taskRun,window) 上限，同上）。
  - 二者均 `RemoteProtocolValidation.positive` 构造期校验；**具体默认值不由 B 擅定**，标注为待父级/P2 注入。
- **atomic admission result**：新 state 创建在 retained runtime 的 state 锁内原子检查——`size >= cap` → 返回 **typed capacity 拒绝 outcome**（不新增 state、零残留）；成功才登记并计数。
- **removal（只保留明确批准项）**：**仅** `completeRound`（exact key 移除 + 递减）与 **task terminal**（taskRun 结束清理该 run 全部 state）两处；
  **撤回 Design #1 的「round 推进自然回收」**（HEAD/父级未授权，删除该表述）。无 TTL/takeover/restart-restore。
- 归属 P2 写集，B 不提前落码；本轮仅本日志设计文字。

### 不变量重申

- committed HEAD `0114604e` 为唯一基线；DHXY 工作区脏文件（+35/-3，含他人 SKIP）受保护、未碰、不作为基线。
- W-RIPS-0 leaf 现与 committed HEAD 等价（三 Strategy、普通加法、常量 4000/8000/18000、invalidate/downgrade/finishPrescan mutation 逐字）。
- 唯一 Java 写集 = `ReturnItemPrescanDecision.java`；其它 Java/Maven/schema/resources/tests、P2/Bag/A/T、service/adapter/assembly/caller/host 全冻结；未 `mvn`、未 git。
- 交付完毕，等待父级源码/设计复审。

**无已批准业务差异；按基线等价迁移。**

## Parent Final Chronology Confirmation - 2026-07-13T06:34:00-04:00

本节位于 Worker Repair #1 / Design Repair #2 的真实末尾并作为最终时序锚点。前段误落位的两条父级确认均只代表同一结论，不能被解读为“先批准、后实施”。父级是在复核当前 137 行修复源码后批准：`W-RIPS-0 SOURCE APPROVED，P0/P1/P2=0`；容量合同为 global `1000` / per-run `64`。统一 Cloud package 尚待 P2/A 写入稳定，除此之外没有开放返修项。

## Parent Next-Task Handoff - `W-TEAMRETURN-D1` - 2026-07-13T06:39:00-04:00

External Worker B 的 ReturnItem 当前切片已完成。下一任务已发布到新固定日志：

`D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-team-return-service-worker-b.md`

请立即转读该日志并在其中追加 `CLAIMED`；领取截止 `2026-07-13T06:59:00-04:00`。20 分钟门只检查领取，不限制实际设计耗时；领取后可持续工作。此旧日志不再追加新任务材料。
