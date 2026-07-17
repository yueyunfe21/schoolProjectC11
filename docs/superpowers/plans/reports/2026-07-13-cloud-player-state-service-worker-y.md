# Cloud PlayerStateService Migration - Internal Worker Y

## Parent Task Brief #1 - `W-PSS-D1` - 2026-07-13T12:07:00-04:00

### 目标

以 DHXY committed HEAD `0114604e` 的
`src/main/java/com/bot/dhxy/service/PlayerStateService.java` 为唯一业务基线，形成可直接拆实施波次的整类迁云合同。
这不是重做全局架构：已固定边界为 DHXY 保留 exact-window capture、HP/MP 像素判读、模板/OCR、输入队列、持续观察与
soft wake；Cloud 持补给/摄妖香业务 phase、计时、pending、fallback 与 outcome 解释。不得把本地 watcher 改成 Cloud
同步 capture 轮询，也不得让本地 fact 自行推进业务 phase。

### 唯一写集

- 仅本 append-only 报告。
- 两仓 Java/Maven/schema/resources/tests、A Navigation、B TeamReturn、U2 TaskTracker protocol、host/caller 全冻结。

### 必做材料

1. 先读 `D:\mavenProject\DHXY\AGENTS.md`、`docs\DHXY_CONTEXT.md`、`docs\业务逻辑.md`、迁移矩阵，以及
   `git show 0114604e:src/main/java/com/bot/dhxy/service/PlayerStateService.java`；不得把脏工作区当业务基线。
2. 给出 committed HEAD 的完整 public API + main caller 矩阵：每个调用点的输入、返回、false/null/UNKNOWN/STOPPED、
   fallback、phase/timer mutation，禁止“同上/实现时再看”。
3. 完整列出 mutable state/cache/timer/config 与 owner/lifetime：identity/location、first-aid plan、combat-exit window、
   incense、check counter、async/future/diagnostic；区分 wall-clock baseline 与只可做 Cloud 同进程 elapsed 的字段，不能顺手改时序。
4. 逐能力分类：`retain-local fact/executor`、`migrate-cloud business state/decision`、`existing typed capability`、
   `missing closed capability`。本地 HP/MP 像素分析与 exact-window capture 保留，Cloud 只能消费 typed health fact；
   输入仍必须走本地原子 input bundle。
5. 对每个 missing capability 给固定语义方法、允许参数、typed outcome、stable action identity/occurrence、
   `NOT_EXECUTED` 与 `UNKNOWN/STOPPED` 分流、consume-final 时点；不得暴露 raw request/poll/outcome/自由 action list。
6. 给 dependency DAG 与 exact 文件表，并只提取一个真正可独立编译、与 A/B/U2 零交叉的第一叶子波次；如果没有，
   明确写“无独立叶子”并给具体 blocker，不得造占位接口过编译。
7. 容量、租户隔离、pause/resume/current revision、terminal cleanup、断线恢复与运维诊断必须有真实 owner；
   不新增 TTL、retry、takeover、额外 probe、额外验证或业务清理。

### 交付门

立即先追加 `CLAIMED`（task、claimedAt、唯一写集），然后追加 `Design #1`。只读审查，不运行 Maven/tests/应用，
不做 Git mutation。Worker 自审只算 QA，父级将独立给出 `DESIGN APPROVED` 或 `BLOCKED`。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker Y - CLAIMED - 2026-07-13T14:01:29.1217335-04:00

- task: `W-PSS-Y3A-STATE-CORE-IMP1`
- claimedAt: `2026-07-13T14:01:29.1217335-04:00`
- uniqueWriteSet:
  - `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-player-state-service-worker-y.md`
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateGovernor.java`
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateOwner.java`
- scopeFence: 只实施 D2.2、D3.1-D3.9 与 Parent Design Review #3 已批准的 state core；不改 assembly/port/wire/service/caller/host/tests/Maven，不启动运行面，不做 Git mutation，不承担 reviewer/approval。

## Parent Implementation Task - W-PSS-Y3A-STATE-CORE-IMP1 - 2026-07-13T13:59:00-04:00（EOF 权威块）

Internal Worker Y 的 D3 已父级 DESIGN APPROVED。立即在本日志真实 EOF 追加 `CLAIMED` 后实施唯一最小 Cloud state-core 波；
唯一 Java 写集仅为以下两个当前不存在的新文件：

1. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateGovernor.java`
2. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateOwner.java`

实现必须逐项落实 D2.2、D3.1-D3.9 与 Parent Review #3：dedicated `10,000/1,000` hard cap、authenticated physical key、
same-epoch exact duplicate、PROVISIONAL/ACTIVE/RELEASE_PENDING 账本、different-epoch two-phase reservation/constructor/
atomic commit、old handle permanent stale、exact initial/resume/terminal projection、closed session release、单一锁序与有界 terminal
session marker。Governor 是唯一 map/index/quota/epoch/projection owner；Owner 只持单 physical state + execution lock，不持第二
index/quota/session map。不得新增第三个文件、public raw getter、ThreadLocal/default key、TTL/LRU/takeover/retry、I/O、线程或
placeholder API；不得修改 assembly/port/wire/service/caller/host/tests/Maven。

你不是唯一在代码库工作的 Worker：保护全部现有 dirty/untracked，不回滚、不覆盖他人编辑；若目标文件在领取前突然存在，
立即停下在日志报告冲突。完成后运行 Cloud `mvn -q clean package`（不可 skip），向本日志追加精确方法/不变量、构建结果与
self-QA；自审不构成父级批准。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker Y - CLAIMED - 2026-07-13T13:06:08.1091136-04:00

- task: `W-PSS-Y0-IMP1`
- claimedAt: `2026-07-13T13:06:08.1091136-04:00`
- uniqueWriteSet: New `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\PlayerFirstAidDecision.java`；以及仅append本固定日志 `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-player-state-service-worker-y.md`
- scope: 只实施 Parent Design Review #1 批准的Y0纯判定叶子；Y1-Y5、wire/schema/state/host/caller/tests全部冻结；不启动运行面、不做Git mutation；Worker不承担reviewer/approval。

## CLAIMED

- task: `W-PSS-D1`
- claimedAt: `2026-07-13T12:08:10.983-04:00`
- uniqueWriteSet: 仅 `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-player-state-service-worker-y.md`

## Design #1 - PlayerStateService 迁云实现前设计 - 2026-07-13T12:45:18.8382908-04:00

### 0. 交付边界、证据与基线锁定

- 本轮是 `W-PSS-D1` design-only。唯一真实写入仍是本 append-only 日志；两仓 Java、Maven、schema、resources、tests、host、caller 全部未改。
- DHXY 只读现场：当前分支 `thin-client-design`，`HEAD=0114604e1ff5f15491d2910959c45252e893d04f`。工作树已有大量他方 dirty/untracked；本 Worker 未切分支、未清理、未覆盖、未暂存、未提交。`AGENTS.md` 的默认 `dev` 分支要求与本任务“不得 Git mutation”冲突时，以父任务固定基线和禁止 Git mutation 为准，不做 checkout。
- Cloud 只读现场：当前分支 `navigation-migration`，已有他方 dirty/untracked；本 Worker 未触碰。下文未来文件表是设计所有权建议，不是本轮写集，也不是实施批准。
- 唯一业务源码基线：`git show 0114604e:src/main/java/com/bot/dhxy/service/PlayerStateService.java`，共 1669 行。当前 dirty `PlayerStateService.java` 不作为行为依据。
- 已完整读取：`AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/业务逻辑.md`、`docs/superpowers/specs/2026-07-12-service-migration-matrix.md`、Full R0 当前源码，以及 A Navigation、B TeamReturn、U2 TaskTracker、S BagService 的最新固定报告。
- `docs/业务逻辑.md` 已核对的适用合同：通用 session/FIRST_AID 所有权；五倍战后急救和黄袍第一战后固定窗口/先 probe 后消费顺序；五倍返程位置验证不加读；修罗使用文档指定的 pre-cloud 行为基线（文档标注 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的路径）；修罗 UNKNOWN 战斗退出与返程判断；本地 watcher 负信号不得升格为业务真值。本文只迁移所有权，不改变这些 caller 决策。
- 本设计不授权创建本地测试、回放图、source guard，也不授权运行 Maven/tests/应用或任何 capture/input/host/poller。

### 1. 必须冻结的业务不变量

1. `PlayerStateService` 的业务判断、调用顺序、返回折叠、fallback、检查次数和所有时间条件，以 `0114604e` 为唯一依据；不得把当前脏文件里的差异带入 Cloud。
2. exact-window title/capture/OCR/template/pixel analysis 和全部真实输入留在 DHXY；Cloud 只接收 typed fact，并拥有 GameContext mutation、急救计划/检查次数、香状态解释和调用顺序。
3. Cloud 不得收到原图、任意 Path、任意 ROI、任意点击坐标、任意 timeout、任意 action list、raw request/poll/outcome/ledger handle。
4. 缓存急救输入必须是一个本地 exclusive 原子事务：同一已绑定窗口内按固定目标顺序右键，每次 `100ms` click + `800ms` settle，最后至多一次安全移鼠标 + `300ms`；exclusive callback 内只用 direct `InputProvider`，不 nested queue。
5. `UNKNOWN` 不是 `false`、`null`、`HEALTHY`、`ALREADY_DONE` 或“未执行”；不得重发可能已发生的输入。只有有可信 compacted `NOT_EXECUTED` 且业务尚未 claim side effect 时，Full R0 才可对同 occurrence 使用 `attempt+1` 续行。
6. 不新增 TTL、定时 poll、自动 retry、takeover、额外截图/OCR/probe/位置验证、额外确认次数、terminal 业务清理或 fail-closed 业务规则。
7. 本类没有 async/future/watcher。迁云后仍由原 caller 发起同步语义调用；本地既有 watcher 只能发布 typed 观察/soft wake，不能独立推进 PlayerState phase、check counter、plan 或香状态。

### 2. committed public API 完整合同

下表逐项覆盖 `0114604e` 的全部 23 个 public 方法和 public enum。Cloud 主体保留同名/同参数/同返回的业务面；标为“本地兼容”的 dormant executor API 留在 DHXY facade，不为迁云凭空增加 Cloud caller。

| 基线 API | 输入及前置 | 精确返回、失败/UNKNOWN/STOPPED 语义 | 精确 state/timer/fallback | 最终归属 |
|---|---|---|---|---|
| `syncMyIdentity()` L132 | 无参数；读取当前绑定窗口；标题优先级固定为 `WindowTaskContextHolder.rawCurrent().nativeBinding.title -> tracker.fullWindowTitle -> tracker.locateWindow()` fallback | `void`。解析成功才更新角色字段；无标题/不可解析时不伪造 identity。迁云后的 transport `UNKNOWN/STOPPED` 必须阻塞/stop unwind，不能当作一次“无变化”成功 | 只 mutation 当前窗口 `GameContext.me`；不改 PlayerRuntimeState/phase/timer | local typed identity fact + Cloud GameContext commit |
| `syncMyPosition()` L148 | 无参数；同步调用 `LocationVisionService.scanCurrentLocation()` | 返回 fresh `LocationInfo`；全部 no-input reader miss 返回 `null`。`null` 不清空旧位置。transport `UNKNOWN/STOPPED` 不得映射 `null` | 非 null 才写 `me.currentMapName/x/y`；无 fallback 读、无 timer | local typed location fact + Cloud GameContext commit |
| `syncAll()` L180 | 无参数 | `void`；先 identity 后 position；position `null` 被忽略；任一步 STOPPED 都直接 unwind | 两步顺序固定；不加并行或第二次位置读 | Cloud orchestration，复用两个 closed fact call |
| `resetCheckCounter()` L190 | 当前窗口 identity epoch 状态 | `void`；无 capture/input | `checksDoneThisRound=0`，`lastCombatExitTime=System.currentTimeMillis()`；不清 plan、香、startup precheck | Cloud pure state mutation |
| `performStartupFirstAidCheck(ctx)` L206 | 入口 checkpoint；`ctx` 可 null | `void`。probe 为 `SUPPLY_NEEDED/UNKNOWN` 时调用缓存执行；缓存执行 false 只 warning，不再 probe、不调用 `healAll` | 先 `checks=0,lastCombatExit=0`；调用 `probeAndConsumeHealthy...("startup")`；HEALTHY 可把 checks 加到 1 | Cloud orchestration + local fact/action |
| `prepareStartupFirstAidNoFocus(ctx,source)` L233 | checkpoint；source 仅日志，null/blank 经 `safeReason`；调用点虽然传 null ctx，但 runner 已绑定 current execution authority | `void`；只读窗口，无真实输入。transport `UNKNOWN/STOPPED` 不得存业务 enum UNKNOWN；只有 local 已执行但 capture unreadable 才存 `FirstAidNoFocusProbeResult.UNKNOWN` | 先 `checks=0,lastCombatExit=0`；调用 probe-and-consume；把 enum 与 Cloud wall-clock commit 时间写 startup precheck；保留低血/本地 capture-unavailable 生成的 plan | Cloud state + local typed health fact |
| `performStartupFirstAidCheckFromPrecheckOrRun(ctx,maxAgeMs)` L252 | checkpoint；caller 可传任意 long，基线 fresh 条件是 `result!=null && (maxAgeMs<=0 || age<=maxAgeMs)` | `void`。fresh HEALTHY/ALREADY_DONE 直接 return；fresh SUPPLY/UNKNOWN 先消费缓存，false 才跑 foreground startup；stale/missing 直接跑 foreground startup；STOPPED unwind | 先计算 age（无时间为 `-1`，否则 `max(0,now-at)`），随后无条件清 precheck result/time，再分支；不恢复已清 precheck | Cloud pure state/orchestration；不新增 TTL |
| `probeAndConsumeHealthyFirstAidNoFocus(ctx,source)` L292 | 调用 raw probe；source 仅日志 | 返回四态 enum。只有 raw result HEALTHY 才“消费检查”；SUPPLY/UNKNOWN/ALREADY_DONE 原样返回 | HEALTHY：`checks++` 且清 pending plan；其它结果不加 checks；不改 combat timer | Cloud decision + local typed health fact |
| `probeFirstAidSupplyNoFocus(ctx)` L314 | checkpoint；当前 checks/窗口 base/config snapshot | `ALREADY_DONE` 当 checks>=1；base 不可用=`UNKNOWN` 且清 plan；capture 未得到但 base 已知=`UNKNOWN` 并缓存所有 enabled 目标；可读且有低条=`SUPPLY_NEEDED`；可读且无低条=`HEALTHY` | 目标固定顺序人物 HP、人物 MP、宝宝 HP、宝宝 MP。全 disabled=HEALTHY；至少一个 enabled readable 时忽略其它单条 UNKNOWN；所有 enabled 都 unreadable 才 conservative all-enabled UNKNOWN。低/健康算法和阈值归一固定，见 5.2 | local pixel fact + Cloud aggregate/plan |
| `hasPendingNoFocusFirstAidPlanForCurrentWindow()` L378 | 当前 window+identity state | 仅当 plan 非 null 且 targets 非空返回 true；in-flight plan 已从 pending 隐藏，因此返回 false | 无 remote call、无 mutation | Cloud pure state read |
| `performCachedFirstAidPlanNow(ctx)` L389 | 入口 checkpoint；随后立即取出并清 pending plan | 无/空 plan=false；plan base 任一 `-1`=false 且 plan 已清；valid plan=true，即使 exclusive callback 返回 false/中断也只 warning 后 true。transport UNKNOWN 时调用尚未完成，不得返回 false/true或重发 | valid plan 被 claim 后 fixed local action；exact terminal action（含 NOT_EXECUTED/STOPPED after claim）后 `checks++`；不复扫 bars；plan age 只日志 | Cloud claim/final commit + dedicated local executor |
| `areStatusBarsVisibleNoFocus(reason)` L477 | reason 仅日志；固定小 ROI no-focus capture | local capture null 时基线返回 false；可读时 `red+blue>=16 && (red>=4 || blue>=4)`。仅“已执行且 CAPTURE_UNAVAILABLE”可兼容折叠 false；transport UNKNOWN/STOPPED 不能折叠 false | 无 persisted state/timer/input | local typed visibility fact + Cloud boolean mapping |
| `healAll()` L534 | 无 ctx；whole-pass exclusive | `void`。初始 bars capture null 时 callback 返回 true并跳过；每个 enabled 目标二次确认失败就跳过；exclusive completed=false 只 warning | 固定目标顺序；不改 checks。每个疑似低条先等 350ms，再 fresh whole ROI confirm；输入与最后安全移鼠标在同 exclusive 内 | dormant DHXY local compatibility；不迁成自由 Cloud action |
| `healAll(ctx)` L567 | 前 checkpoint，调用 `healAll()`，后 checkpoint | `void`；前/后 STOPPED 均 unwind；内部 completed=false 仍只 warning | 不改 checks/timer | dormant DHXY local compatibility |
| `healPlayer()` L573 | 动态读取人物 HP/MP enabled+threshold | `void`；每项单像素 capture null/健康=false 被忽略；低条各自执行右键 | HP 后 MP；两项不是一个跨项 exclusive pass；不改 checks | dormant DHXY local compatibility |
| `healPet()` L586 | 动态读取宝宝 HP/MP enabled+threshold | `void`；每项单像素 capture null/健康=false 被忽略；低条各自执行右键 | pet HP 后 pet MP；不改 checks | dormant DHXY local compatibility |
| `ensureSheYaoXiangActive()` L599 | legacy 无 ctx | 返回 true 仅本次实际用香成功；quiet/no-use/fail-closed/item miss=false；STOPPED 不伪装 false | 走普通主包 `findAndUseItem`；无默认窗口 fallback | Cloud business facade；依赖 S BagService closed API |
| `ensureSheYaoXiangActive(ctx)` L609 | checkpoint；可开主包并输入 | true 仅实际用香；false 分支与上一行逐项相同 | quiet 先判；需要时只做一次 status fact；USE 才调用 Bag；success 写香 timer、报告语义并固定等 1000ms | Cloud decision/state + local status fact + Cloud BagService |
| `ensureSheYaoXiangActiveInOpenMainBag(mainBag,ctx)` L623 | `mainBag` 是 caller 已开的 exact session；不得二次 acquire/open/close | true 仅 session 内 `useItem` 成功；false 同 incense 其它分支；STOPPED unwind | 用香发生在 FiveRing callback 内且早于数鞋；session child operation 顺序不变 | Cloud PlayerStateService 依赖 Cloud BagService.MainBagSession；最终 caller cohort 同批切换 |
| `ensureSheYaoXiangActiveForLeaderTask(source)` L687 | legacy source、无 ctx | 当前窗口 member=false；leader=true/false 取决于实际 incense；无 current window 时按基线 legacy leader 兼容；STOPPED 不折叠 false | source 仅日志，不参与 action identity/timer | Cloud facade；无 ambient/global fallback，legacy 仅受 trusted invocation scope 约束 |
| `ensureSheYaoXiangActiveForLeaderTask(source,ctx)` L697 | 入口 checkpoint；精确 role fact | member 直接 false且不查香/包；leader/legacy 调 ensure；true 仅实际用香 | member skip 不改香 state/timer；source 仅日志 | Cloud role decision + incense flow |
| `checkAndHeal(name,relX,relY,expectRed)` L715 | 任意 caller 文本/相对点/颜色；默认 threshold=70 | capture null/健康/输入未完成=false；低像素并完成既有分支=true | 可能独立输入并移鼠标；不改 checks | dormant DHXY local compatibility；禁止暴露到 Cloud closed port |
| `checkAndHeal(name,relX,relY,expectRed,threshold)` L719 | 任意相对坐标；threshold 归一为 `<=40->30, <=60->50, else70` | 同上，但使用归一阈值 | 单点 capture；低色右键 `100+800ms`；按参数决定安全移鼠标 | dormant DHXY local compatibility；禁止自由坐标 wire |
| `FirstAidNoFocusProbeResult` L1663 | `SUPPLY_NEEDED/HEALTHY/ALREADY_DONE/UNKNOWN` | 这是业务观察结果，不承载 transport UNKNOWN/NOT_EXECUTED/STOPPED | 仅 local 已执行观察可生成前三态及业务 UNKNOWN | Cloud 保留同 enum；另设 transport disposition，禁止混用 |

补充：private `performFirstAidCheck(ignoreTimeInterval,ctx)` 在该 commit 无调用点，`HEAL_TIME_INTERVAL=5000` 只被它读取。不得据此新增 Cloud timer、scheduler 或 watcher。

### 3. committed main caller 完整矩阵

以下来自 `git grep` 对 `0114604e` 的全部 48 个生产调用点；没有用“同上”。行号均为 fixed baseline。

#### 3.1 AutoCombatService

| call site | 输入/返回使用 | exact fallback 与 phase/state/timer 影响 |
|---|---|---|
| L565 `hasPending...`，`consumeQueuedLeaderPostCombatFirstAidIfHead` | 无输入；false 表示当前没有可执行 plan | false 时把 FIFO head attempt 以 `NO_PLAN_TERMINAL`/success=false 完成并 return true，含义是“该 head 已处理”，不是补给成功；不 probe |
| L573 `performCached...(context)` | boolean 送入 queued leader attempt completion | true/false 均完成该 head；方法本身 return true 表示 head handled；valid plan 才使 checks++ |
| L592 `probeAndConsume...(context,source+":queued-leader-report")` | 四态映射为战后上报：SUPPLY->SUPPLY，UNKNOWN->UNKNOWN，HEALTHY/ALREADY_DONE->HEALTHY | 只上报，不在此处消费低血 plan；HEALTHY 可 checks++；不改变 caller phase |
| L630 `resetCheckCounter()`，`consumeExitAndRecover` | 已确认 combat exit 后调用 | checks=0、lastCombatExit=wall clock；必须保持“确认退出后”时点，不能由 watcher negative 提前触发 |
| L652 follower `probeAndConsume...(":post-combat")` | SUPPLY/UNKNOWN 都令 `needsSupply=true` | 修罗上报 FIFO 并设置 pending；非修罗只有 plan 实际存在才 pending；HEALTHY 清 pending；UNKNOWN 不变成 HEALTHY |
| L666 `hasPending...` | non-Xiuluo follower 判定 plan 是否真存在 | 无 plan 时不排 task turn；禁止因 probe enum UNKNOWN 直接伪造可点击 plan |
| L685 leader `probeAndConsume...(":post-combat")` | 修罗只报告；非修罗继续看 result | SUPPLY/UNKNOWN 进入 L693 缓存执行；HEALTHY/ALREADY_DONE 不执行；顺序固定 |
| L693 `performCached...` | 仅非修罗且 SUPPLY/UNKNOWN；false 只 warning | false 不 healAll、不复扫、不改变后续 incense phase；valid plan true/count++ |
| L700 `ensure...ForLeaderTask(":post-combat",context)` | 仅 recovery policy 要求时调用，返回被忽略 | member wrapper 会 skip；false 不阻止 post-combat 流程；success 才改 incense timer |
| L806 deferred non-Xiuluo `probeAndConsume...(":deferred-post-combat")` | 只在不再 IN_COMBAT 且允许消费时 | SUPPLY/UNKNOWN 后 L809；HEALTHY/ALREADY_DONE 不点击；禁止提前到 still-in-combat |
| L809 `performCached...` | false 只 warning | 不复扫；调用完成后流程继续 |
| L815 `ensure...ForLeaderTask(":deferred-post-combat",context)` | deferred recovery 被消费后调用，返回忽略 | false 不回退其它香路径；success 才写 timer |
| L883 Xiuluo follower FIFO `hasPending...` | head 已轮到时检查 | 无 plan：complete false/`NO_PLAN_TERMINAL`，清 caller pending，return false；不额外 probe |
| L895 Xiuluo follower `performCached...` | boolean 完成 FIFO attempt | 清 caller pending；return exact boolean；valid plan 自身 checks++ |
| L943 non-Xiuluo follower after-turn `performCached...` | false 清 pending并 return false；true 清 pending并 return true | 不 probe、不 fallback；caller 的 task-turn release 顺序不变 |

#### 3.2 TeamReturnService / startup / task callers

| call site | 输入/返回使用 | exact fallback 与 phase/state/timer 影响 |
|---|---|---|
| `TeamReturnService` L75 `ensureSheYaoXiangActive(context)` | 已第一次找到归队按钮后调用；boolean 只记 `incenseUsed` 日志 | 无论 true/false，都必须重新 fresh-find 归队按钮；按钮消失返回 false，否则点击并 true。香 false 不能中断归队，也不能复用第一次按钮坐标 |
| `AutoBattleTask` L137 `performStartupFirstAidCheck(context)` | execute 启动阶段 | 完成后继续 maintenance/combat loop；STOPPED unwind；缓存 false 仅 warning |
| `WubeiTask` L390 `performStartupFirstAidCheck(context)` | execute startup | 先急救，再 L391 香；不得交换 |
| `WubeiTask` L391 `ensure...ForLeaderTask("wubei:startup",context)` | return ignored | false 不阻止任务；success 才写香 timer |
| `WubeiTask` L2264 `probeFirstAidSupplyNoFocus(context)` | 黄袍第一战后固定窗口中 direct raw probe；结果只日志 | HEALTHY 不 consume check counter；SUPPLY/UNKNOWN plan 留给后续 L4859；不得改成 probeAndConsume，不得提前点击 |
| `WubeiTask` L4580 `syncMyPosition()` | cached return item 后验证 | null 或非目标 map 均视为未验证，进入既有 trusted combat correction；不得额外位置读 |
| `WubeiTask` L4611 `syncMyPosition()` | normal return item 后验证 | null/错 map 与 cached 分支相同地未验证并走既有 correction；只此一次读 |
| `WubeiTask` L4859 `performCached...(context)` | continued tracker click 前无条件调用，boolean 只日志 | true/false 都继续原 tracker click；必须保持“先消费 plan，再点击继续” |
| `FiveRingTaskV2` L894 `performStartupFirstAidCheckFromPrecheckOrRun(context,60000)` | PREPARE | 严格使用 60,000ms fresh gate；precheck 被清后按第 2 节 fallback；无新 probe |
| `FiveRingTaskV2` L1241 `ensure...InOpenMainBag(mainBag,context)` | `withMainBagOpen` session 内，用香在数鞋之前；boolean 写 `FiveRingSupplyCheck` | 当前代码不根据该 boolean 分支；不得让 false 关闭/重开包或跳过鞋计数 |
| `FiveRingTaskV2` L2045 `syncMyPosition()` | `syncAcceptNpcSetupPosition` 仅在 fresh runner location snapshot 缺失时调用一次 | null 使 near-NPC=false，交现有 navigation path；不得额外验证 |
| `FiveRingTaskV2` L2560 `resetCheckCounter()` | `releaseWindowCombatStateAfterWuhuanEvidence` 仅当 action state 原为 IN_COMBAT | 先 reset，再 L2566 probe；negative watcher 不可替代该 evidence gate |
| `FiveRingTaskV2` L2566 `probeAndConsume...(source+":post-combat")` | 战后 probe | SUPPLY/UNKNOWN 才 L2569；HEALTHY/ALREADY_DONE 不点击 |
| `FiveRingTaskV2` L2569 `performCached...` | false 只 warning | 不复扫、不 healAll，继续 L2574 |
| `FiveRingTaskV2` L2574 `ensure...ForLeaderTask(source+":post-combat",context)` | return ignored | false 不改五环 phase；success 才改香 timer |
| `XiuluoTaskV2` L443 `performStartupFirstAidCheck(context)` | 第一轮 startup | STOPPED unwind；缓存 false 仅 warning；不改变后续 round phase |
| `XiuluoTaskV2` L1087 `syncMyPosition()` | startup return guard 的 before-return | null 等同“未确认起始 map”，可触发现有 return item；不得用旧 GameContext 当 fresh fact |
| `XiuluoTaskV2` L1122 `syncMyPosition()` | after-return | null 保留未确认语义，写入既有 facts/log/result；不得补第二读 |
| `XiuluoTaskV2` L1932 `ensure...ForLeaderTask("xiuluo-v2:startup",context)` | startup incense guard | caller 无论 boolean 都设置 checked=true、pending=false；迁云不得让 false 保持 pending |
| `XiuluoTaskV2` L2724 `ensure...ForLeaderTask("xiuluo-v2:startup-prepare",context)` | prepareRound fallback | caller 无论 boolean 都更新 startup incense flags；不重试 |
| `XiuluoTaskV2` L3332 `syncMyPosition()` | maintenance retry cleanup diagnostics | null 令 same-map=false、near-target=false；仅诊断/既有分支，不额外导航真值 |
| `XiuluoTaskV2` L4139 `ensure...ForLeaderTask("xiuluo-v2:startup-before-target-nav",context)` | pending startup incense before target navigation | boolean 被忽略，随后 flags 固定 checked=true/pending=false；导航继续 |
| `XiuluoTaskV2` L4679 `syncMyPosition()` | resolve UNKNOWN combat exit | null 不能证明离开目标 map/距离；继续现有 task-panel/return resolution，不把 null 变成失败真值 |
| `XiuluoTaskV2` L5319 `syncMyPosition()` | one-shot startup return item 后 | null/错 map=false；不额外验证 |
| `XiuluoTaskV2` L5362 `syncMyPosition()` | cached return item path | null/错 map=unverified，进入既有 trusted combat correction；一次读 |
| `XiuluoTaskV2` L5393 `syncMyPosition()` | normal return item path | null/错 map=unverified，进入既有 correction；一次读 |
| `GameStateUtil` L200 `areStatusBarsVisibleNoFocus(reason)` | `likely=!coordinateReadable&&!barsVisible` | local 已执行 capture failure 按基线 bars=false，可支持 likely=true；transport UNKNOWN/NOT_EXECUTED 不得伪装 bars=false |
| `GameStateUtil` L435 `syncMyPosition()` | `confirmCurrentMap` 每轮既有 loop 一次 | null 只让该轮继续到既有 deadline/poll；不得由 PlayerState 增加读或改变 deadline；TaskSleep interrupt 返回 false |
| `DefaultWindowTaskStartupInitializer` L68 `syncMyIdentity()` | `debug_navigation_stress` only | identity 后返回 thread interruption 状态；不做位置读 |
| `DefaultWindowTaskStartupInitializer` L77 `syncAll()` | normal common UI startup | identity->position；position null 忽略；随后检查 interruption；不阻止 heavy startup 仅因位置 miss |
| `TaskStartupWindowPreparationService` L172 `prepareStartupFirstAidNoFocus(null,source)` | 五环 background-first UI probes 全 ready 后；显式 ctx=null，但 `beforeTask` 已持 exact window/execution authority | no-input store precheck；不得落入 `default`/无 run 状态；later FiveRing L894 消费同 window+identity 状态 |

#### 3.3 无生产 caller 的 public 面

- `healAll()`、`healAll(ctx)`、`healPlayer()`、`healPet()`、两个 `checkAndHeal` overload、无 ctx `ensureSheYaoXiangActive()`、无 ctx `ensureSheYaoXiangActiveForLeaderTask(source)` 在 `0114604e` 无外部生产调用。
- `ensureSheYaoXiangActive(ctx)` 有 TeamReturn caller；`ensureSheYaoXiangActiveInOpenMainBag` 有 FiveRing caller，不能因其相邻 overload dormant 而整体删除。
- dormant API 只保留兼容，迁云不得激活新 caller、不得把 arbitrary coordinate executor 变成远程通用输入 API。

### 4. mutable state/cache/timer/config 完整 owner 与 lifetime

#### 4.1 状态表

| 基线字段/对象 | baseline owner/key/lifetime | mutation/read 时点 | Cloud 真正 owner/key/lifetime |
|---|---|---|---|
| `GameContext.me` identity | 当前 `WindowRuntimeContext` 对应的 GameContext；角色/窗口绑定生命周期 | `syncMyIdentity` 成功才更新；失败保留旧值 | 既有 `CloudGameContextStateOwner` 的 exact run/window projection；final consume 内原子更新；window state key 见 10.1，禁止 global/default |
| `GameContext.me.currentMapName/x/y` | 当前窗口 GameContext；跨同窗口 task 调用保留 | fresh location 非 null 才三字段一起写；miss 不清旧值 | `CloudGameContextStateOwner` final consume；typed NOT_FOUND 返回 null但不 mutation |
| `runtimeStates` map | Spring singleton `ConcurrentHashMap<String,PlayerRuntimeState>`；key=current windowId，缺 context 才 `"default"`；process lifetime | 每次 `state()` 比较 `WindowRuntimeContext.playerIdentityEpoch`，epoch 变更即整对象替换 | New `CloudPlayerStateStateOwner`；key=`tenant/user/device/clientSession + logicalWindowId + playerIdentityEpoch`。不含 taskRun/runRevision/HWND；无 `default`。同 client session+window+epoch 跨 task 保留，Cloud 进程/会话 incarnation 结束即像本地进程重启一样丢失，不做持久化/恢复 |
| `playerIdentityEpoch` | state 内镜像；由 WindowRuntimeContext identity drift 增长 | mismatch 时清整组 PlayerRuntimeState | key 的必需维度；新 epoch 原子创建全新 state，旧 epoch 不复用、不迁数据；HWND/pid/binding generation 仍作为每次 action fence，不作为业务记忆 key |
| `checksDoneThisRound` | PlayerRuntimeState，default 0 | reset/startup=0；HEALTHY consume 或 valid cached plan terminal 后++；上限判断 1 | `CloudPlayerStateStateOwner`，同 window+epoch lifetime；terminal task 不清；所有 mutation 在该 state 单锁/原子 transition |
| `lastCombatExitTime` | PlayerRuntimeState，wall-clock epoch ms | reset 写 now；startup 写 0；仅 dormant private 5s gate读取 | Cloud wall-clock epoch；照写照保留，但不创建 scheduler/active timer；task terminal 不清 |
| `pendingNoFocusFirstAidPlan` | PlayerRuntimeState；最多 4 fixed targets | probe 替换/清；base unavailable清；capture unavailable且 base known建 conservative plan；performCached 入口立即 claim/隐藏 | Cloud owner 内 `PENDING/EXECUTION_IN_FLIGHT/none`，最多 4 固定目标。planId 来自 observation semantic address+canonical fact digest，不用 UUID/时间。task terminal不做业务清理；identity epoch变更清 |
| `FirstAidPlan.createdAtMs` | wall-clock epoch，仅日志 age，无 expiry | 创建 plan 时写 now；执行只算 age log | Cloud final-consume wall clock，仅诊断；不参与 TTL/freshness/action identity |
| `FirstAidPlan.baseX/baseY` | probe 当时 tracker window screen-absolute base | cached executor refresh成功用 fresh base，否则仍用 stored base | typed fact保存 `SCREEN_ABSOLUTE` base + exact binding；dedicated local executor必须保留 refresh-or-stored fallback；禁止 generic current-binding-only bundle |
| `startupFirstAidPrecheckResult` | PlayerRuntimeState，可跨 initializer->FiveRing prepare | prepare 写；consume 方法先清后分支 | Cloud owner同 window+epoch；不以 task terminal清。只允许同 trusted current run/window读取；null ctx 不表示 global |
| `startupFirstAidPrecheckAtMs` | wall-clock epoch | prepare probe完整消费后写；consumer按 caller maxAge算 age并先清 | Cloud final-consume wall clock；pause不补偿；maxAge仍只由 caller传入，不成为系统 TTL |
| `lastIncenseUsedTime` | PlayerRuntimeState，wall-clock epoch | item use true写 now；trusted remaining>0按 `now-(59m-clamp(remaining,1..59m))` 回推；quiet读取 | Cloud state wall-clock epoch；同 window+epoch跨 task；task terminal不清；identity drift清 |
| `nextIncenseRetryTime` | default 0；baseline 只有置 0写点，无非零 writer | existing recognizer会读，但实际永远不触发 nonzero retry gate | 字段保留并保持 0；不得新增 writer/RETRY_LATER schedule/TTL |
| `incenseIconOffsetX/Y` | default -1；Cloud decision icon box 时 `max(0,value)` | 只进入后续 request与日志，不决定点击 | Cloud diagnostics/state；typed local incense fact更新；不作为 remote ROI/click 参数 |
| health config | `BotProperties` 动态读取：四个 enabled + 四个 threshold | 每次 probe/heal 时读取；threshold `<=40->30, <=60->50, else70` | fact provider在每次实际观察时读取 local live config，并把 exact snapshot放 typed fact；Cloud不在 task start冻结旧 config，不允许 caller传任意 threshold wire |
| fixed first-aid geometry/color policy | class constants：人物 x 949..1020、宝宝 823..876、HP y85、MP y101、ROI 823,85,198x17、sample radius 2x1、higher +10、near margin 3%、confirm 350ms | local pixel classification/foreground executor | 保留 local；Cloud只读 typed per-bar classification+diagnostics。Y0 leaf只做四条 aggregate/plan policy |
| incense policy constants | duration 59m、refresh remaining 20m、quiet margin 2m -> quiet 37m；ROI 901,123,123,34；icon .85、digits .80 | quiet/remaining/use decision | 59/20/2m与 use decision在 Cloud；fixed ROI/template/OCR在 local fact provider；不得改阈值/先 cyan hour 后 green minute顺序 |
| `BagService.MainBagSession` | caller-owned local exclusive session child | FiveRing session内 use incense后继续 count shoes | 依赖 S 的 per-runtime Cloud `BagService.MainBagSession`/closed local capability；不保存进 PlayerState owner，不跨 runRevision伪造 |
| temp/debug image | `WindowScopedTempPath` 的香 raw PNG；capture audit/log | 当前 raw image会上传旧 decision service | final design仅 local window-scoped诊断，可选择按既有 debug开关落盘；不上传、不进 Cloud state，不跨窗口共用 |
| async/future | 无字段、无线程、无 future | 全部 API同步等待 | 不新增 PlayerState worker/future/poller。Full R0 pending handle属 remote infrastructure owner，不是业务 future，也不写进 PlayerState DTO |
| latency/diagnostic | `LatencyMetrics`、capture audit、source/reason、player/window/base/ROI | 只日志 | Cloud/local各自用 monotonic elapsed做同进程 latency；不把 monotonic值持久化或用于业务时间判断 |

#### 4.2 时间语义逐项冻结

| 时间 | 时间域 | 精确规则 |
|---|---|---|
| `lastIncenseUsedTime` / quiet | Cloud wall-clock epoch ms | `last>0 && since>=0 && since<37m` 时直接 false，零 remote status/Bag；pause/resume不移动时间 |
| trusted remaining back-compute | Cloud wall-clock epoch ms | remaining clamp 1..59m，`last=now-(59m-remaining)`；只有 typed PRESENT_REMAINING 正数可写 |
| `lastCombatExitTime` | Cloud wall-clock epoch ms | confirmed exit reset时写；startup清0；5s private gate保持 dormant |
| startup precheck age | Cloud wall-clock epoch ms | final consume写 at；consumer `max(0,now-at)`；`maxAge<=0`无限信任该非 null precheck；pause时间计入 age |
| plan created age | Cloud wall-clock epoch ms | 仅日志，无过期/自动清理 |
| 350/800/300ms | DHXY local TaskSleep/input callback elapsed | 分别 foreground confirm、每目标 click settle、一次安全移鼠标；不迁成 Cloud业务 timer |
| incense success后 1000ms | Cloud task调用顺序中的固定 TaskSleep | Bag返回 true、state success commit后执行一次并忽略 sleep boolean，保持基线；不调度、不重试、不作为 expiry |
| remote RTT/等待/consume-final latency | 各进程 monotonic elapsed | 仅 metrics/timeout diagnostics；绝不改变 phase、freshness、quiet、fallback或 occurrence |

Cloud 业务时间统一由 state owner注入的 wall-clock `Clock` 在对应 mutation 点取值，不信任 client任意 `nowMs`。这是 owner迁移，不授权更改任何 duration/比较符；不得以 runRevision、pause累计或 monotonic值重写这些 wall-clock条件。

### 5. local typed fact 与 baseline 判定的精确边界

#### 5.1 common observation envelope

所有 PlayerState local fact 必须带下列固定证据；缺一项即 closed validation 失败，不得让 Cloud 猜窗口：

- exact scope 中的 `taskRunId/taskType/windowId/stopEpoch`；`tenant/user/device/clientSession` 只来自 authenticated transport，不允许 payload 自报覆盖。
- observation 时的 `hwnd/processId/bindingGeneration/playerIdentityEpoch` 和 `ObservedWindowBinding`；必须与 command 的 window fence 匹配。
- operation 固定 enum、semantic address、request/outcome digest、`acceptedAtEpochMs/finishedAtEpochMs`。
- capture provider/audit、system scale ratio（若该 fact 使用 capture）；只传 enum/数值/尺寸，不传截图 bytes/path。
- `executionState` 与 fact status 分离。只有 `OBSERVED` 才允许 fact 非 null；非 OBSERVED 的所有 fact 字段显式 null。

#### 5.2 health fact 与 aggregate policy

DHXY 本地一次 exact-window bars capture 完成全部像素分析，产 `PlayerHealthFact`，不上传 ROI：

- fixed ROI：window-relative `(823,85,198,17)`；base 是 screen-absolute window origin，必须随 fact 返回。
- `PlayerSupplySettings` 固定八字段：人物/宝宝 HP/MP 的 enabled 和 normalized threshold。normalized 只能为 30/50/70；它是本地读取 live `BotProperties` 后的 typed snapshot，不是 Cloud 下发任意 config。
- 每条 `BarFact` 固定目标 enum `PLAYER_HP/PLAYER_MP/PET_HP/PET_MP`，并含 `enabled`、`HEALTHY/SUPPLY_NEEDED/UNKNOWN/SKIPPED_DISABLED`、`healthyColumns/totalColumns/observedPercent`、threshold/higher sample healthy counts、sample area pixels、固定 reason enum、sample RGB。Cloud 不重新读像素。
- 健康色保持：red=`r>150 && r>g+80 && r>b+80`；blue=`b>150 && g>120 && b>r+80`。
- 每列在 y 半径 1 内有健康色即 healthy column；threshold/higher point 在 x 半径2、y半径1 的 15 像素中至少2个健康色才 true。
- 条健康条件保持：`observedPercent>=threshold`，或 threshold sample healthy 且 `observedPercent>=threshold-3`；否则无论 higher/threshold 单点是否矛盾，都为 SUPPLY_NEEDED，并以 fixed reason 记录矛盾。
- transport 已执行但 fixed ROI capture 失败：fact status=`CAPTURE_UNAVAILABLE`，保留已知 base/settings/capture audit；这不是 transport UNKNOWN。base 已知时 Cloud 生成 conservative all-enabled plan并返回业务 UNKNOWN。
- tracker base unavailable：fact status=`BASE_UNAVAILABLE`，Cloud 清 pending plan并返回业务 UNKNOWN；不发 capture。
- ROI越界/某条 unreadable：该条 UNKNOWN。全 disabled -> HEALTHY；enabled 中一个都不可读 -> UNKNOWN + conservative all-enabled plan；只要至少一个 enabled readable，就忽略其它单条 UNKNOWN，只收集 readable SUPPLY_NEEDED；集合空=HEALTHY，非空=SUPPLY_NEEDED。
- plan targets固定顺序 `PLAYER_HP -> PLAYER_MP -> PET_HP -> PET_MP`，最多4个、不得重复。每个 target带 fact中的 normalized threshold仅用于日志/plan digest；实际点击相对点由本地 enum固定映射，Cloud不得传坐标。
- `ALREADY_DONE` 不由 local fact产生；它只由 Cloud state owner 在 capture前看到 `checks>=1` 时产生，因而零 remote call。

Y0 pure policy 只拥有“4条 typed classification -> overall result/ordered target subset”这层，不复制 RGB/ROI/capture 代码。

#### 5.3 status-bars visibility fact

- local fixed ROI仍是 health bars ROI；一次 capture统计整图 red/blue。
- typed status：`VISIBLE`、`NOT_VISIBLE`、`CAPTURE_UNAVAILABLE`，另带 red/blue/total、尺寸与 audit。
- `VISIBLE` 精确条件是 `red+blue>=16 && (red>=4 || blue>=4)`。
- Cloud public compatibility仅把 `OBSERVED + CAPTURE_UNAVAILABLE` 映射成 baseline `false`；`NOT_EXECUTED/UNKNOWN/STOPPED` 不映射 false。

#### 5.4 identity/location fact

- identity local provider必须复用 committed title优先级和 `ClientIdentityService` 解析。允许的 typed status仅 `PARSED/TITLE_UNAVAILABLE/TITLE_UNPARSEABLE`；PARSED才带角色字段。实现时可让既有 service对一个非 GameContext 的临时 `PlayerCharacter` 扫描，再提取 typed fact，避免 local mutation；不得退到第一同名窗口。
- location local provider直接复用 `LocationVisionService.scanCurrentLocation()` 的一次完整 fallback链。typed status仅 `FOUND/NOT_FOUND`；FOUND带 map/x/y，NOT_FOUND无位置。Cloud只在 FOUND final consume时更新 GameContext。
- 两类 fact都必须绑定 exact window；不允许 title字符串或 location结果充当 action/window authority。

#### 5.5 incense status fact

local provider在固定 window-relative ROI `(901,123,123,34)` 内复用当前 Cloud recognizer的算法，最终只发 typed fact：

| local fact status | 证据 | Cloud baseline decision |
|---|---|---|
| `ICON_ABSENT` | buff template `.85` 无 match | USE_INCENSE |
| `PRESENT_REMAINING` + `CYAN_HOURS` | 先以 cyan digit `.80` 识别正整数小时 | remaining=`value*60m`；`<=20m` USE，否则 NO_ACTION |
| `PRESENT_REMAINING` + `GREEN_MINUTES_TEMPLATE` | cyan未接受后，green `.80` 识别正整数分钟 | remaining=`value*1m`；`<=20m` USE，否则 NO_ACTION |
| `PRESENT_REMAINING_UNKNOWN` | icon存在但两套数字都拒绝 | NO_ACTION，不补香 |
| `CAPTURE_UNAVAILABLE` | 本地固定 ROI capture已尝试但失败 | FAIL_CLOSED/false |
| `TEMPLATE_UNAVAILABLE` | buff或完整0..9 digit模板集缺失 | FAIL_CLOSED/false |
| `IMAGE_UNREADABLE` | 已取得frame但算法无法读取 | FAIL_CLOSED/false |

- fact可带 icon box（ROI-local）、remaining、source enum、recognized text、confidence与 fixed reason；不带 image bytes/base64/raw path。
- 顺序固定为 icon -> cyan hours -> green minutes；不得反转、追加 OCR fallback或额外 capture。
- 当前 DHXY `images/template/status` 与 Cloud resource字节核对：buff及0/1/2/4/5/6/7/8/9完全同 SHA-256；DHXY缺 `3.png`、`3_runtime_53_fragment.png`、`5_runtime_53_fragment.png`。未来 local recognizer波次必须把这3个 Cloud现有资源逐字节加入 DHXY，不能用不完整模板集声称等价；本轮未复制。
- 当前 `SheyaoxiangStatusCloudDecisionService` + raw `TICK/STATUS_IMAGE/OUTCOME` 是可复用的判定证据但不是最终 closed capability。cutover后 PlayerState不得继续上传图片或双跑旧链；旧类是否被其它路径使用需由最终 owner查明后另行退役，不在Y0删除。

#### 5.6 cached first-aid local executor

`PLAYER_FIRST_AID_EXECUTE_CACHED` 是 dedicated operation，原因是 generic `EXECUTE_INPUT_BUNDLE` 无法表达 baseline 的“worker内 refresh绑定，成功用fresh base，失败退stored base”而又不暴露任意 action list。

固定执行合同：

1. request仅包含 state-owner铸造的 plan digest、ordered target enum subset、stored screen-absolute base、playerIdentityEpoch；无 arbitrary description/coordinate/delay。
2. local handler先验证 current binding/window/epoch。fence不符则 exact NOT_EXECUTED；绝不换到其它窗口。
3. 进入唯一 `InputSequences.submitExclusiveAndWait`；callback内 refresh tracker。refresh成功且base有效用fresh base，否则用stored base。
4. target enum在本地映射固定 relative point；每个目标前后 `InputActionScope.checkpoint`，direct右键100ms，settle 800ms。
5. 至少一次已供给后只执行一次固定安全区随机移鼠标+300ms；安全区、避开 top-right `x>=761 && y<=147` 的规则与 committed代码一致。随机点只在首次真实执行中产生；Full R0 ledger/redelivery返回 retained outcome，不重抽、不重执行。
6. outcome带 `planDigest/baseResolution(REFRESHED/STORED)/actualBase/startedTargetOrdinal/lastCompletedTargetOrdinal/clickedTargetMask/mouseMovedAway/observedWindow`。这些是 typed diagnostics，不授权 Cloud基于部分点击追加补点。
7. 不在 callback里调用任何 remote port或 `submitAndWait`，避免 queue-in-queue deadlock。

### 6. capability 分类总表

| 能力 | retain-local fact/executor | migrate-cloud business state/decision | existing typed capability | missing closed capability |
|---|---|---|---|---|
| bound identity | exact binding/title优先级与解析 | 成功时 GameContext identity mutation | `WindowTaskContextHolder`、`ClientIdentityService` 本地已有；Full R0 binding fence已有 | typed `PLAYER_IDENTITY_OBSERVE` wire + fixed port method |
| current location | exact-window capture/OCR/template/fallback一次 | FOUND时 GameContext位置mutation，NOT_FOUND返回null | `LocationVisionService`本地已有；Full R0 capture/binding已有 | typed `PLAYER_LOCATION_OBSERVE` wire + fixed port method |
| first-aid bars | fixed ROI、scale、RGB/sample/threshold分类、capture audit | checks gate、四条aggregate、plan建立/清理、startup precheck | local tracker/capture与 BotProperties已有；Y0将提供pure aggregate | typed `PLAYER_FIRST_AID_OBSERVE` wire/port |
| status bars visible | fixed ROI pixel count | baseline bool折叠 | local capture已有 | typed `PLAYER_STATUS_BARS_OBSERVE` wire/port |
| cached first-aid input | refresh-or-stored base、exclusive direct clicks、fixed delays/safe move | plan claim/hidden、terminal后checks++与public true | `InputSequences/InputActionWorker/InputProvider`已有；Full R0 ledger已有 | dedicated `PLAYER_FIRST_AID_EXECUTE_CACHED`，不能用free bundle |
| foreground `healAll/healPlayer/healPet/checkAndHeal` | 整体保留local，包括350ms二次capture与任意legacy点 | 无现有生产业务caller，不创建Cloud decision | committed DHXY API已有 | 无；刻意不增加closed remote面，未来若出现Cloud caller须另开CR |
| incense image analysis | fixed ROI、icon/digit template算法和local diagnostics | quiet/remaining/use/fail-closed、timer/icon facts | 旧 Cloud recognizer算法与资源已有；Full R0 transport已有 | typed `PLAYER_INCENSE_STATUS_OBSERVE`；旧raw image facade不可复用为final API |
| incense item use | Bag local whole-pass/session input | PlayerState决定何时调用、消费boolean、success timer+1000ms | S BagService设计提供per-runtime closed `findAndUseItem/MainBagSession.useItem` | 无第二套PlayerState item protocol；以S完成为硬依赖 |
| leader/member | exact run/window role fact | member skip false；legacy trusted scope兼容 | TaskExecutionContext/current slot已有 | 无PlayerState专用wire；assembly必须注入exact role |
| check/plan/incense state | 无本地业务副本 | 全部 Cloud state owner | Full R0 retained action state和GameContext owner已有 | `CloudPlayerStateStateOwner/Governor` |
| phase/wake | local observer只软唤醒 | caller原phase/park/continue决定 | Full R0 action lifecycle/current revision已有 | receipt-driven ready/closed status query若尚未由owner落地，则为主体硬门，Y不得自行poll补齐 |

### 7. missing closed capability 的固定 API

#### 7.1 业务可见 facade

Cloud新增 per-runtime `com.bot.dhxy.service.PlayerStateService`，保留第2节 public API；构造只能由 `CloudTaskRunAuthorityAssembly` 完成，依赖 exact `TaskExecutionContext`、`GameContext`、`CloudPlayerStateStateOwner`、`CloudPlayerStatePort`、Cloud `BagService`。它不是 Spring singleton，不从 ThreadLocal/title search/global map找窗口。

DHXY committed `PlayerStateService` 在最终 caller cutover前继续原样服务本地；cutover cohort中只变成兼容壳/保留 dormant local APIs，不允许同时执行本地业务和Cloud业务。

#### 7.2 trusted closed port

public接口建议名 `com.yueyunfe.dhxy.cloudbrain.remote.CloudPlayerStatePort`；package-private实现由assembly创建并同时绑定 `RemoteTaskRunScope + exact window + stopEpoch + CloudPlayerStateStateOwner + CloudTaskRetainedActionState + CloudTaskServicePort`。业务只能调用以下 fixed methods：

```java
IdentityCallResult syncIdentity();
LocationCallResult syncPosition();
FirstAidProbeCallResult probeFirstAidSupply();
FirstAidProbeCallResult probeAndConsumeHealthyFirstAid();
StartupPrecheckCallResult prepareStartupFirstAid();
StatusBarsCallResult readStatusBarsForDirectCombatMode();
IncenseStatusCallResult readIncenseStatusForEnsure();
FirstAidExecutionCallResult executeClaimedCachedFirstAid(CachedFirstAidPlanLease lease);
```

约束：

- 方法无 `timeoutMs/source/ROI/title/hwnd/action list/raw payload/raw handle` 参数；fixed timeout/purpose/ROI均在port private常量内。
- `CachedFirstAidPlanLease` 是 public sealed opaque view、仅允许 remote package 的 package-private实现；无 public constructor/factory。只有 state owner 在 public API入口 checkpoint/gate通过后可 claim pending plan并铸造；业务caller不能自造target/base。
- 三个 first-aid观察方法可复用同一 local operation类型，但分别使用固定 action slot和固定 final mutation；不能接受任意 callback或任意“mode”字符串。
- port不暴露 raw `CloudTaskServicePort`、ledger、mint/renew、requestId/actionId、poll/outcome。result只包 closed disposition + 已在 consume-final内提交过的typed business result。
- port内部 final business commit固定调用其绑定的 state owner方法；不接收 public callback，避免任意对象/重复commit/port recursion。
- remote outcome pending时，Cloud task通过owner提供的 receipt-ready capability park/yield；不得业务线程sleep/poll。若当前源码仍无该capability，Y3/Y4保持硬blocked，不能以循环查询伪实现。

#### 7.3 wire closed union

新 `RemoteOperation.PLAYER_STATE` 只允许 `PlayerStateOperation` 六值：

```text
IDENTITY_OBSERVE
LOCATION_OBSERVE
FIRST_AID_BARS_OBSERVE
STATUS_BARS_OBSERVE
INCENSE_STATUS_OBSERVE
FIRST_AID_CACHED_EXECUTE
```

`PlayerStateRequest`：observation五类payload必须是只含 `operation` 的closed object；cached execute额外且仅含 `planDigest, targets, storedBaseX, storedBaseY, playerIdentityEpoch`。targets仅fixed enum、有序唯一、1..4；base明确 `SCREEN_ABSOLUTE`，无coordinateSpace自由选择。

`PlayerStateOutcome`：公共 `CommonOutcome` 后只允许一个与operation匹配的typed fact/action outcome；OBSERVED/EXECUTED时必须非null，其它execution state必须显式null。严格 unknown-field拒绝、enum数字拒绝、scalar coercion关闭；request/outcome canonical digest和双仓schema同一波更新。

#### 7.4 每个 operation 的 typed outcome

| operation | final成功 state | typed payload | baseline业务映射 |
|---|---|---|---|
| IDENTITY_OBSERVE | OBSERVED | 第5.4节 IdentityFact + observed binding | PARSED commit identity；两类miss不mutation，public void完成 |
| LOCATION_OBSERVE | OBSERVED | FOUND/NOT_FOUND + optional location + binding | FOUND返回info并commit；NOT_FOUND返回null保旧state |
| FIRST_AID_BARS_OBSERVE | OBSERVED | BASE_UNAVAILABLE/CAPTURE_UNAVAILABLE/BARS + settings/base/四bar facts | Cloud Y0 decision后返回四态、写/清plan；ALREADY_DONE在发请求前产生 |
| STATUS_BARS_OBSERVE | OBSERVED | VISIBLE/NOT_VISIBLE/CAPTURE_UNAVAILABLE + counts | exact兼容bool；transport状态不折叠 |
| INCENSE_STATUS_OBSERVE | OBSERVED | 第5.5节fact | Cloud quiet之后的一次status解释；fail closed false或Bag use |
| FIRST_AID_CACHED_EXECUTE | EXECUTED或exact terminal non-execution | 第5.6节action outcome | valid plan claim后public最终true/checks++，无补点/重试 |

### 8. stable semantic identity / occurrence / attempt

| facade call的remote step | phaseCode | actionSlot | occurrence规则 |
|---|---|---|---|
| `syncMyIdentity` | `player-state.identity` | `sync` | 每个已完成public invocation +1；UNKNOWN保持原 occurrence |
| `syncMyPosition` | `player-state.position` | `sync` | 每个已完成fresh读 +1；caller loop每轮是新 occurrence，port不得自行加读 |
| raw `probeFirstAidSupplyNoFocus` | `player-state.first-aid` | `probe-only` | checks<1且实际需观察时 +1；ALREADY_DONE零action |
| `probeAndConsumeHealthy...` / foreground startup内部probe | `player-state.first-aid` | `probe-consume-healthy` | 每次实际观察 +1；final commit决定是否checks++ |
| startup background precheck | `player-state.first-aid` | `startup-precheck` | 每次实际prepare +1；result/time同final写 |
| cached execute | `player-state.first-aid` | `execute-cached-plan` | 每个claimed plan仅1 occurrence；UNKNOWN永不换occurrence/重发 |
| status visibility | `player-state.status-bars` | `direct-combat-visible` | 每个caller读 +1 |
| incense status | `player-state.incense` | `observe-status` | quiet skip无action；每次真正status read +1 |

- Full identity仍是 `(scope,taskRunId,phaseCode,actionSlot,occurrence,attempt)`；`runRevision`只做current publication/fence，不进semantic address/request digest。
- `source/reason/task方法名/日志文本`不参与identity。canonical request首次invoke前冻结；同attempt重入只能读冻结bytes。
- `attempt`默认0。只有“业务尚未claim side effect + exact compacted NOT_EXECUTED证明零执行”的观察操作，才允许同occurrence `attempt+1`；这叫transport continuation，不是业务retry。
- planId=`SHA-256(stateKeyStableProjection + producing semanticAddress without runRevision + canonical health fact digest)`；`createdAtMs`不进planId。cached action requestDigest包含planId、fixed target序、stored base和epoch。
- Bag item use继续使用S BagService自己的semantic root/occurrence；PlayerState不铸造第二个输入action identity。成功/false通过该closed调用的final business result消费。

### 9. NOT_EXECUTED / UNKNOWN / STOPPED 与 consume-final

#### 9.1 common 分流

| 情况 | 是否有业务fact/side effect | 是否mutation/返回 | 是否可renew |
|---|---|---|---|
| gate在public checkpoint前发现STOPPED/foreign/stale revision | 无 | existing stop unwind；零state mutation，零remote request | 否 |
| PAUSED/current revision未获执行权，尚未claim plan/冻结业务调用 | 无 | park；public API未完成，不返回false/null/UNKNOWN | receipt-ready后恢复；不是新occurrence |
| observation exact NOT_EXECUTED，且retained compaction可信证明client未执行 | 无 | 不生成业务UNKNOWN/NOT_FOUND/CAPTURE_UNAVAILABLE；零business mutation | 仅同occurrence attempt+1，且只在原caller仍需要该调用时 |
| observation transport UNKNOWN/receipt pending/结果丢失 | 未知 | state/phase/return均不推进；同request/action/occurrence保持 | 绝不resend；等late exact outcome/receipt |
| observation OBSERVED + local `CAPTURE_UNAVAILABLE` | 确认capture调用已执行且没图 | 按baseline生成业务UNKNOWN/conservative plan或bars=false/incense fail-closed | 不renew、不额外probe |
| observation STOPPED | 无完整fact | stop unwind；不得存业务UNKNOWN或return null/false | 不自动renew |
| final business mutation抛异常/进程在mutation与ACK间不确定 | fact已到但commit不确定 | Full R0 `BUSINESS_CONSUMPTION_UNKNOWN`；禁止重复commit/伪ACK | 不重放mutation；人工/owner恢复门 |

#### 9.2 cached plan 的关键 claim 边界

为了逐字保持 baseline “checkpoint后立即清plan，queue completed=false仍checks++并返回true”，顺序必须是：

1. port/gate先完成入口stop/current校验；失败时plan仍PENDING，等价于 baseline outer checkpoint抛出。
2. state owner单锁检查：无/空plan -> false；bad stored base -> 清plan并false；valid plan -> 原子从PENDING转 `EXECUTION_IN_FLIGHT`、从`hasPending`隐藏并铸造lease。该claim就是baseline的“立即清plan”时点。
3. claim后任何 exact terminal结果，包括 local fence拒绝、NOT_EXECUTED、STOPPED、部分点击后interrupt、callback completed=false，都只记录diagnostic；final consume把in-flight清掉、`checks++`、public完成为true。不得restore plan、attempt+1或补剩余target。
4. claim后的 transport UNKNOWN 不清in-flight、不checks++、不返回true/false，也不重发；同clientSession等待原action late exact结果。因plan已隐藏，`hasPending`为false，符合baseline plan已清。
5. task不可在这个同步public invocation unresolved时正常推进到下一phase/正常terminal；stop必须促成本地exact STOPPED outcome再按第3步finalize。断线只park，不伪terminal。

#### 9.3 每类 final mutation 的原子时点

| final outcome | state owner在ACK/control publication之前的唯一commit |
|---|---|
| identity PARSED | 原子更新GameContext角色字段；miss只记录diagnostic，不清旧值 |
| location FOUND | 原子更新map/x/y并冻结public return；NOT_FOUND冻结null且不改旧位置 |
| probe-only health | 按Y0建立/替换/清plan并冻结四态；HEALTHY不加checks |
| probe-consume-health | 先按Y0处理plan；HEALTHY再checks++并确保plan null；其它不加checks |
| startup-precheck | 先`checks=0,lastCombatExit=0`的入口mutation；fact final时按probe-consume规则，再写result+precheckAt |
| status visibility | 冻结bool，无persistent state；只有已执行CAPTURE_UNAVAILABLE可冻结false |
| incense fact | 写icon offset；remaining>0回推last use；USE/NO_ACTION令nextRetry=0；再冻结use/no-use分支 |
| Bag incense true | 写lastIncenseUsedTime=Cloud wall now、nextRetry=0、冻结true；然后一次1000ms TaskSleep；false不写last-use并冻结false |
| cached action claimed terminal | 清in-flight、checks++、冻结true，无论completed mask |

business commit必须在 `consume*Final` 临界区完成，随后才允许 Full R0 ACK/receipt/control publication。state lock内只做确定性CPU/state mutation，禁止I/O、禁止递归port、禁止sleep；Bag/1000ms在相应remote final提交之后按public调用顺序执行。

### 10. capacity、tenant isolation 与 lifecycle owner

#### 10.1 state governor 与 key

新增 `CloudPlayerStateStateGovernor`，不把 PlayerState 状态塞进 Spring singleton/global map，也不借 `CloudTaskRetainedActionState` 当业务state：

- quota owner=`tenantId/userId/deviceId`；state physical index=`tenantId/userId/deviceId/clientSessionId/logicalWindowId`。
- entry内的 `playerIdentityEpoch` 是强version fence；epoch mismatch时在同一physical slot原子替换整份state，释放旧epoch内容，和 baseline `runtimeStates.compute(windowId)`替换一致。第4.1节写作“key含epoch”表示logical identity完整性，不表示同时保留多个旧epoch entry。
- state key不含 `taskRunId/runRevision/hwnd/pid`。taskRun有独立 projection；HWND/pid/bindingGeneration每次remote action都校验，但窗口移动、重绑不会把香/check memory复制到另一个窗口。
- 没有 exact authenticated scope/window/epoch时 typed capacity/scope failure；禁止退到 `"default"`、第一个窗口、空clientSession或跨tenant state。

#### 10.2 hard capacity

| resource | hard cap/owner | 满额语义 |
|---|---|---|
| PlayerState persistent window entries | global 10,000；每 `(tenant,user,device)` 1,000；同 physical key lookup/epoch replace 先于quota判断 | `PlayerStateCapacityException`/typed infrastructure refusal；remote request未创建，不能映射public false/null/UNKNOWN，也不能evict其它窗口 |
| active run projections into PlayerState state | 复用 coordinator 每owner nonterminal 64门；每 exact run/window最多1 projection | run prepare/assembly fail closed；不建半状态，不抢其它tenant |
| pending plan | 每state最多1；targets 0..4；in-flight最多1 | structural validation拒绝第二plan/重复target；正常probe仍按baseline替换pending，不排队积压 |
| retained remote requests | 既有 broker global 10,000、owner 1,000、owner pending 64 | 使用Full R0已有typed capacity，不自建unbounded map |
| retained actions | 既有 broker global 10,000、owner 1,000 | 同上；UNKNOWN仍占原slot，不能以重发绕quota |
| routes/queue | 既有 global route 1,000、owner 64、queue 64 | PlayerState不新增route/thread/queue |

所有cap采用same-key-before-quota、无eviction、无LRU、无TTL、无background cleanup。state锁只保护一个physical entry transition；全局/owner quota锁内不做I/O、capture、Bag或remote port调用。

#### 10.3 pause/resume/current revision

- initial assembly在 coordinator已经确认 exact scope/run/window/stopEpoch 后取得一个 state projection和一个per-runtime service/port；构造失败不发布current slot。
- pause只阻止新PlayerState调用进入remote side effect并让现有public invocation park；它不修改任何wall-clock业务timestamp、不清plan、不把watcher negative变为结果。
- resume创建新revision execution facade，但必须复用同一 `CloudPlayerStateStateOwner` entry、同一 `CloudTaskRetainedActionState`及原冻结request/handle。旧revision port因current-slot fence失效，不能commit或发布。
- `runRevision`不进入state key、semantic address、planId或requestDigest；只参与“谁可继续当前run”的authority校验。
- `prepareStartupFirstAidNoFocus(null,source)` 的 null只关闭显式TaskCheckpoint输入，不允许跳过current authority。baseline runner在 `beforeTask` 已建立executionContext；Cloud facade从assembly绑定对象取run，不从null参数推导无run。

#### 10.4 terminal cleanup

真实owner分工：

- `CloudTaskRunAuthorityAssembly/RemoteTaskRunCoordinator`：关闭current revision、run projection、port publication、retained request/action/receipt资源。
- `CloudPlayerStateStateGovernor`：normal task terminal只release该run projection引用；不清checks、combat-exit、pending/precheck、香timer/icon，因为baseline Spring service跨task保留这些字段。
- valid cached invocation unresolved时不得宣称normal terminal；stop要等exact STOPPED final并按9.2消费。transport disconnect只park。
- `WindowRuntimeContext`等价的identity drift：新epoch第一次访问时原子替换business entry；这是baseline已有清理，不是新增terminal cleanup。
- authenticated clientSession明确关闭、窗口注销或Cloud process incarnation结束：governor释放该session/window entries和quota；不迁到replacement session，不落盘恢复。这等价于本地进程/窗口runtime消失。

#### 10.5 disconnect/restart/replacement session

- 同一clientSession瞬时断线：run/state/request保留；本地transport恢复后交付原command或late exact outcome。Cloud不运行offline phase、不自增occurrence、不重探。
- outcome UNKNOWN：原action保持，禁止resend。只有本地ledger对原action的late outcome能关闭。
- replacement clientSession：旧session scope不能被新session takeover；旧run fail-closed停留/停止，由lifecycle owner处理。新session创建新state，不能继承旧香/plan/check，符合进程incarnation重启语义。
- Cloud restart：当前实现内存state无rehydration；不得从client上传自报cache恢复，也不得加TTL/recovery guess。若未来要DR，必须另开显式行为/持久化CR。

### 11. dependency DAG 与 exact future file table

#### 11.1 DAG

```text
G-FULL-R0 final-consume/compaction/current-revision authority
        + G-U2 TaskTracker protocol wave fully stable (shared wire files)
        + G-READY receipt-driven ready/closed status advance capability
                         |
Y0 PlayerFirstAidDecision pure leaf (independent of all gates) -----------+
                         |                                               |
                         v                                               |
Y1 PLAYER_STATE strict protocol + digest + dual-side dormant switch -----+
                         |
                         v
Y2 DHXY local typed fact provider + cached executor + incense recognizer/resources
                         |
                         v
Y3 CloudPlayerStateStateGovernor/Owner + closed CloudPlayerStatePort
                         |
                         +---- G-S BagService per-runtime closed API
                         |
                         v
Y4 Cloud per-runtime PlayerStateService + assembly/context lifecycle
                         |
                         +---- G-B TeamReturn caller owner
                         +---- task/AutoCombat/GameState caller cohorts
                         +---- startup/window host owner
                         v
Y5 cohort cutover/shadow-off; old raw incense upload path no longer used
```

- A Navigation：`0114604e` 仅注入 PlayerStateService，无方法调用；Y0-Y4不依赖也不修改A文件。最终task caller cohort可等待Navigation自己的门，但不能把它伪造成PlayerState core前置。
- B TeamReturn：L75是实际caller；B主体落地后才可把其依赖接到Cloud PlayerStateService。本Worker不改B文件。
- U2 TaskTracker：Y1会共享operation/envelope/digest/schema/switch文件，必须等待U2当前protocol wave和父级closure稳定后由owner一次分配；Y不得并写。
- S BagService：incense use和FiveRing `MainBagSession`是Y4硬依赖；不得自造第二套Bag wire绕过S。
- G-READY：若Full R0 owner尚未提供receipt-ready/closed status advance，Y3/Y4主体为明确blocked；不得Cloud poll/sleep模拟。Y0不受影响。

#### 11.2 Y0：唯一可独立实施叶子（本轮仅设计，未授权写）

| repo | exact file | change | 内容/依赖 |
|---|---|---|---|
| Cloud | `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\PlayerFirstAidDecision.java` | **New，唯一叶子文件** | 纯JDK、无Spring/Lombok/remote/state/caller；第12节完整算法 |

#### 11.3 Y1：strict protocol wave（等待U2/shared owner）

| repo | exact file | change |
|---|---|---|
| Cloud | `...\remote\PlayerStateOperation.java` | New：六值closed operation enum |
| Cloud | `...\remote\PlayerStateRequest.java` | New：sealed/record closed request，observation零业务参数，cached plan固定字段 |
| Cloud | `...\remote\PlayerStateOutcome.java` | New：closed fact/action union及nested typed records/enums |
| Cloud | `...\remote\RemoteOperation.java` | Modify：加入 `PLAYER_STATE` |
| Cloud | `...\remote\RemoteRequest.java` | Modify：permits `PlayerStateRequest` |
| Cloud | `...\remote\RemoteOutcome.java` | Modify：permits `PlayerStateOutcome` |
| Cloud | `...\remote\RemoteCommandEnvelope.java` | Modify：严格序列化PlayerState closed payload |
| Cloud | `...\remote\RemoteCommandOutcomeEnvelope.java` | Modify：exact keys/null matrix/typed parse，operation switch闭合 |
| Cloud | `...\remote\RemoteProtocolDigests.java` | Modify：request/outcome canonical bytes；不改变旧operation bytes |
| Cloud | `...\remote\RemoteFinalConsumedAck.java` | Modify：operation exhaustive validation闭合；无PlayerState自由attachment |
| Cloud | `...\remote\CloudTaskRetainedActionState.java` | Modify：固定PlayerState semantic slots/mint入口，仅trusted port可用 |
| Cloud | `...\remote\CloudTaskRunActionLedger.java` | Modify：`PLAYER_STATE` retained identity/action classification与final consume校验 |
| Cloud | `...\remote\RemoteGameClientPort.java` | Modify：package-internal fixed typed dispatch method |
| Cloud | `...\remote\CloudTaskRunCommandExecutor.java` | Modify：构造/提交/correlate `PlayerStateRequest/Outcome` |
| Cloud | `...\remote\CloudTaskServicePort.java` | Modify：package owner所需request/final-consume原语；不公开给business service |
| Cloud | `...\remote\CloudTaskRunExecutionGate.java` | Modify：PLAYER_STATE current/stop/pause operation gate |
| Cloud | `...\remote\RemoteGameCommandBroker.java` | Modify：operation route/closed payload/容量路径 |
| DHXY | `...\cloud\remote\RemotePlayerStateCommandPayload.java` | New：严格镜像command payload |
| DHXY | `...\cloud\remote\RemotePlayerStateOutcomePayload.java` | New：严格镜像fact/action outcome |
| DHXY | `...\cloud\remote\RemoteGameOperation.java` | Modify：加入 `PLAYER_STATE` |
| DHXY | `...\cloud\remote\RemoteGameCommand.java` | Modify：operation-specific payload validation |
| DHXY | `...\cloud\remote\RemoteGameOutcomeEnvelope.java` | Modify：typedPlayerState outcome encode/null matrix |
| DHXY | `...\cloud\remote\RemoteOperationPayloadCodec.java` | Modify：closed decode/encode，unknown field拒绝 |
| DHXY | `...\cloud\remote\RemoteProtocolDigests.java` | Modify：与Cloud逐字canonical parity |
| DHXY | `...\cloud\remote\RemoteOperationLedger.java` | Modify：observation/cached-side-effect retained分类；同action不重执行 |
| DHXY | `...\cloud\remote\RemoteFinalConsumedAck.java` | Modify：operation exhaustive closure |
| DHXY | `...\cloud\remote\HttpRemoteCommandTransport.java` | Modify：operation wire enum closure，不加poller |
| DHXY | `...\cloud\remote\LocalRemoteGameCommandHandler.java` | Modify：Y1只fail-closed dormant switch；真实handler在Y2接线 |
| DHXY docs | `D:\mavenProject\DHXY\docs\superpowers\specs\2026-07-12-thin-client-protocol-schema.md` | Modify：strict operation/request/outcome/digest/null matrix |

`...\remote` 在Cloud表中展开为 `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote`；DHXY表中展开为 `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote`。Y1必须由父级在U2静止后重新取diff并一次授权，不能按本设计自行开写。

#### 11.4 Y2：DHXY local mechanics（Y1后）

| exact file | change/owner |
|---|---|
| `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\playerstate\PlayerStateLocalMechanics.java` | New：identity/location/health/status/incense fixed observation + cached executor；Spring bean，依赖现有tracker/services/input |
| `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\playerstate\PlayerIncenseStatusRecognizer.java` | New：逐字等价第5.5算法；只返回typed fact |
| `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\PlayerStateService.java` | Modify：把现有像素/执行代码委托到同一mechanics，保持public本地兼容，避免双份算法；业务迁云cutover另在Y5 |
| `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\LocalRemoteGameCommandHandler.java` | Modify：注入mechanics，fixed operation分派；不直接new service、不nested input queue |
| `D:\mavenProject\DHXY\images\template\status\sheyaoxiang_digits\3.png` | New：从Cloud current resource逐字节同步，SHA-256 `366EF3D275D5E84644994B5655DF29CF31756C7C729C0167B99DC5CB2923EF5C` |
| `D:\mavenProject\DHXY\images\template\status\sheyaoxiang_digits\3_runtime_53_fragment.png` | New：逐字节同步，SHA-256 `9CB03DF25F53CCA0484BEFF26963DEBC8B279FB2252D596C275A05B7818B29DE` |
| `D:\mavenProject\DHXY\images\template\status\sheyaoxiang_digits\5_runtime_53_fragment.png` | New：逐字节同步，SHA-256 `7725356BFB5379BE158FADA3D77331F2513D75417B89C3AE9A10D942B0E9878C` |

Y2不改Maven，不上传图片，不创建测试；实施时仍须按AGENTS Java compile gate由获批实施者运行DHXY compile，但本design-only Worker本轮禁止运行。

#### 11.5 Y3/Y4：Cloud state/port/service/assembly（Y1/Y2/G-READY后）

| exact file | change/owner |
|---|---|
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateGovernor.java` | New：state quota/index/session cleanup |
| `...\remote\CloudPlayerStateStateOwner.java` | New：single-entry transitions、plan lease、fixed final commits |
| `...\remote\CloudPlayerStatePort.java` | New：第7.2节public closed facade + package-private implementation/nested result types |
| `...\remote\CloudTaskServiceExecutionContext.java` | Modify：持同revision bound PlayerState port/state projection；resume复用owner |
| `...\remote\CloudTaskRunAuthorityAssembly.java` | Modify：initial/resume/terminal mount/release；构造per-runtime service dependencies |
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\runner\context\TaskExecutionContext.java` | Modify：只提供closed PlayerState facade/current authority accessor，不暴露raw port |
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\PlayerStateService.java` | New：第2节完整Cloud业务API；依赖Y0/owner/port/BagService |

上表 `...\remote` 展开为Cloud remote绝对目录。若assembly现有构造仍缺G-READY，Y3可以先落state governor纯owner，但port/service不得以raw poll完成；父级应进一步拆波，而不是扩大Y0。

#### 11.6 Y5 caller/host cohort（不属于Y写集）

未来迁移owner必须逐项按第3节接入Cloud `PlayerStateService`，涉及的目标Cloud文件当前均不存在：`AutoCombatService`、`TeamReturnService`、`AutoBattleTask`、`WubeiTask`、`FiveRingTaskV2`、`XiuluoTaskV2`、`GameStateUtil`、`DefaultWindowTaskStartupInitializer`、`TaskStartupWindowPreparationService`。它们应跟各自A/B/task/host cohort创建，Y不得抢写。

DHXY同名caller在thin shell最终只保留local startup/transport/executor职责；不得一边本地调用旧PlayerState业务、一边Cloud重复调用。cutover必须按task cohort原子关闭旧raw incense上传链，不能逐call site双跑。

### 12. exact first leaf：`PlayerFirstAidDecision.java`

这是本设计唯一提请父级可单独批准的实施叶子；不是占位接口：

```java
package com.bot.dhxy.service;

public final class PlayerFirstAidDecision {
    public enum Result { HEALTHY, SUPPLY_NEEDED, UNKNOWN }
    public enum Target { PLAYER_HP, PLAYER_MP, PET_HP, PET_MP }
    public enum BarState { HEALTHY, SUPPLY_NEEDED, UNKNOWN, SKIPPED_DISABLED }

    public record Bar(boolean enabled, int normalizedThreshold, BarState state) {}
    public record Snapshot(Bar playerHp, Bar playerMp, Bar petHp, Bar petMp) {}
    public record PlannedTarget(Target target, int normalizedThreshold) {}
    public record Decision(Result result, List<PlannedTarget> targets) {}

    public static Decision decide(Snapshot snapshot) { ... }
}
```

精确实现规则：

1. constructor防御性验证非null；enabled bar threshold只能30/50/70；disabled必须 `SKIPPED_DISABLED`，enabled不能该state；返回targets `List.copyOf`。
2. 按四个record field固定顺序检查，绝不接收caller任意list/order/target名。
3. enabledCount=0 -> `HEALTHY, empty`。
4. readableCount=0（所有enabled均UNKNOWN）-> `UNKNOWN`，targets为所有enabled条，按固定顺序，带各自threshold。
5. readableCount>0 -> 只收集state=SUPPLY_NEEDED的enabled条；其它UNKNOWN忽略；targets空=`HEALTHY`，非空=`SUPPLY_NEEDED`。
6. 不产生ALREADY_DONE、不读check counter、不读图片/config/time、不建planId、不做I/O/log/input；这些都由后续owner/service拥有。

独立性证据：

- 只import `java.util.List/Objects/ArrayList`，可单文件编译；不依赖尚缺的wire、receipt-ready、Bag、assembly或caller。
- 只读检查时该绝对路径不存在；A Navigation、B TeamReturn、U2 TaskTracker报告的现有/批准写集均不含此文件，零交叉。
- 它是后续Cloud `PlayerStateService` 的直接production dependency，完整承载baseline aggregate policy，不是为了“过编译”的空DTO/adapter。
- 本轮没有创建它，也没有运行javac/Maven/tests。父级若批准实施，必须重新核对并行工作树后给唯一写集；本Worker当前没有该批准。

### 13. 运维诊断与证据 owner

#### 13.1 Cloud日志/metrics

每次fixed call记录：scope hash（不输出凭据）、taskRun前缀、taskType、windowId、playerIdentityEpoch、current runRevision（仅诊断）、phaseCode/actionSlot/occurrence/attempt、request/action digest前缀、call disposition、business result、elapsed。不得记录图bytes、完整token或raw path。

state governor记录：global/owner entry与run projection当前数/上限、same-key hit、epoch replacement、capacity拒绝、session release；不以silent eviction修复满额。

first-aid记录：settings snapshot、四bar typed state/percent/sample reason、overall result、planId前缀、target enum序、plan age、claim/final状态、clicked mask/baseResolution。UNKNOWN必须明确是 `TRANSPORT_UNKNOWN` 还是 `OBSERVED_CAPTURE_UNAVAILABLE -> BUSINESS_UNKNOWN`。

incense记录：quiet elapsed/remaining、typed status、remaining/source/icon offset/confidence、USE/NO_ACTION/FAIL_CLOSED、Bag boolean、timer mutation；`nextRetryTime`若出现非零必须P1告警，因为baseline无writer。

#### 13.2 DHXY日志/证据

- `logs/dhxy-console.log`：operation、windowId/player、HWND/pid/epoch、capture provider、scale、fixed ROI、typed fact、input queue request/action、click ordinals、stored/refreshed base、elapsed。
- `logs/tracker-coordinate.log`：只保留tracker/window geometry/capture诊断，不能替代业务console evidence。
- local status/health图片如既有debug需要，只能走 `WindowScopedTempPath`，标清window/operation；Cloud outcome绝不含路径/图片。
- fresh runtime验收点（未来，不是本轮运行请求）：同一window identity同步；location FOUND/NOT_FOUND不清旧值；health四种aggregate；capture unavailable conservative plan；cached refresh成功/失败stored fallback；queue non-complete仍true/count；五环60s precheck；TeamReturn香false后fresh button；Xiuluo null location与flags；多window action不串。不得因本design要求用户现在启动。

### 14. 自审、blocker 与交付结论

#### 14.1 明确 blocker

- Y1共享U2正在变动的strict protocol/envelope/digest/schema/switch写集，必须等父级确认U2稳定并重新分配；当前不得并写。
- Cloud主体需要receipt-driven ready/closed status advance来处理remote pending且不poll；若Full R0 owner尚未落地，它是Y3/Y4硬门。
- incense完整迁移依赖S BagService closed per-runtime API与 `MainBagSession`，不能由PlayerState绕过。
- caller cutover分属B TeamReturn及各task/host owner；Y只提供service，不抢写caller。
- 上述blocker都不阻止唯一Y0 pure leaf；Y0之后不得越门实施。

#### 14.2 self-QA（不是批准）

- API：23/23 public方法 + public enum已逐项列明；48/48 committed生产调用点已列输入、返回使用、fallback、phase/state/timer影响；无“同上/实现时再看”。
- state/timer：identity/location、plan/precheck/combat time、incense四字段、check counter、config、temp/diagnostic、async/future均有owner/lifetime；wall-clock与monotonic elapsed已分开。
- local/cloud：exact-window capture/OCR/template/pixel/input保留local；GameContext、aggregate/plan/check/incense业务状态与决定迁Cloud；dormant arbitrary executor不暴露remote。
- closed capability：fixed methods/allowed params/typed outcome/action identity/occurrence/attempt/NOT_EXECUTED/UNKNOWN/STOPPED/final consume均已定义；无raw request/poll/outcome/free action list。
- lifecycle：capacity、tenant隔离、pause/resume/current revision、terminal cleanup、disconnect/restart、ops diagnostics均有真实owner；无TTL/retry/takeover/额外probe/验证/业务cleanup。
- DAG：只提出一个真实独立叶子 `PlayerFirstAidDecision.java`；当前与A/B/U2零交叉，其余共享写集全部挂门。
- 本Worker未运行Maven/tests/app，未启动capture/input/host/poller，未做Git mutation；只追加本日志。self-QA只说明设计覆盖度，不构成 `DESIGN APPROVED`、reviewer approval或实施授权。

**无已批准业务差异；按基线等价迁移。**

Design #1 到此停止，等待父级独立审查；Internal Worker Y 不承担 reviewer/approval，也不自行实施Y0。

## Design #1 Append-Only QA Erratum - 2026-07-13T12:59:00-04:00

- 以同一条 authoritative `git grep` 在 `0114604e` 对全部23个 public方法名重新计数，生产调用点精确为 **46**，不是上文3节导语和14.2 self-QA误写的48。
- 第3节矩阵实际已经逐项覆盖这46个call site：AutoCombat 15、TeamReturn 1、AutoBattle 1、Wubei 6、FiveRing 7、Xiuluo 11、GameStateUtil 2、DefaultWindowTaskStartupInitializer 2、TaskStartupWindowPreparationService 1；合计46，无漏项、无额外伪caller。
- 因日志必须append-only，不回改历史行；本勘误明确取代上文两处“48/48”。最终self-QA结论应读作：**46/46 committed生产调用点已逐项覆盖**。
- 其它Design #1内容、blocker、唯一叶子、`无已批准业务差异；按基线等价迁移`结论不变。本勘误仍不是reviewer approval。

## Design #1 Append-Only QA Erratum #2 - exact API expansion - 2026-07-13T13:03:00-04:00

本段取代第2节中带引用式简写或队列返回表述不精确的对应单元格；日志append-only，因此不回改历史行。

### E2.1 `ensureSheYaoXiangActive` 三个入口的完整 false/stop 合同

- `ensureSheYaoXiangActive()`：委托context=null的普通主包路径。true只在 `bag/sheyaoxiang_item.png` 实际use返回true；以下逐项false：37分钟quiet命中、typed status给NO_ACTION、typed status fail-closed、status capture/template/image不可用、Cloud决定USE但Bag找不到/未使用item。Bag/runtime异常仍向外抛，不折叠false；入口无显式ctx checkpoint，但current run的transport STOPPED不得伪装false。
- `ensureSheYaoXiangActive(ctx)`：入口checkpoint；false集合精确为quiet、NO_ACTION、fail-closed、status不可用、Bag use=false。USE后先执行post-item checkpoint；该checkpoint stop时报告STOPPED并抛 `TaskStopRequestedException`。use=true且checkpoint通过后，写last-use/nextRetry、记录USED，再调用一次 `TaskSleep.sleep(1000)`且忽略其boolean；若仅在这最后1000ms被interrupt，基线仍返回true。
- `ensureSheYaoXiangActiveInOpenMainBag(mainBag,ctx)`：上述quiet/status/stop/true/false合同完全展开为：quiet=false；NO_ACTION=false；fail-closed/status不可用=false；`mainBag.useItem("bag/sheyaoxiang_item.png",null)` false则false；use true且post-item checkpoint通过则写timer/USED、固定sleep1000并true。差异仅是复用caller现有MainBagSession，不自行open/close/acquire。

### E2.2 两个 `checkAndHeal` overload 的完整返回合同

- `checkAndHeal(name,relX,relY,expectRed)` 固定把threshold=70传入五参数入口。它做一次scaled 1x1 capture；capture null=false；像素满足 `isHealthyColor`=false；像素不健康时：若当前就是input worker，pre-checkpoint失败=false，direct右键后800ms sleep或post-checkpoint失败=false，全部完成=true；若不是input worker，则提交固定click+800ms（base可用且要求move时再加safe move+300ms），**忽略 `submitAndWait` boolean并返回true**。因此queued分支true表示“像素判低且提交了既有补给序列”，不证明物理输入完成。
- `checkAndHeal(name,relX,relY,expectRed,threshold)` 先按 `threshold<=40 -> 30`、`<=60 -> 50`、其余 `70` 归一，再执行上一 bullet已经逐字展开的1x1 capture/健康判定/worker direct/queued-ignore-return合同；normalized threshold只影响日志和由caller预先算出的采样点，不新增二次capture。
- `healPlayer()` 按人物HP、人物MP顺序调用并忽略上述boolean；`healPet()` 按宝宝HP、宝宝MP顺序调用并忽略boolean。因此任一queued action未完成不会改变这两个void API的控制流或checks counter。

### E2.3 五环startup caller 的完整fallback

`FiveRingTaskV2:894` 传固定 `maxAgeMs=60_000`：调用先清读取到的precheck result/time；result不存在或age>60,000时调用 `performStartupFirstAidCheck`；fresh HEALTHY/ALREADY_DONE直接完成；fresh SUPPLY_NEEDED/UNKNOWN先 `performCachedFirstAidPlanNow`，其false才调用 `performStartupFirstAidCheck`。没有第四分支、额外probe或恢复已清precheck。

### E2.4 retained-action capacity 单元格展开

既有retained action容量是global 10,000、每owner 1,000；满额由Full R0 typed capacity路径拒绝新identity，不创建unbounded fallback map、不evict其它scope。已有UNKNOWN action继续占原slot等待late exact outcome；不得通过新actionId/occurrence重发来绕过quota。

本勘误后，最终QA不再以第2节L719单元格的“同上”或第10.2节retained-actions单元格的“同上”为合同；以E2.1-E2.4的完整文字为准。仍是 **46/46 caller、23/23 public API**，仍非reviewer approval。

## Design #1 Append-Only QA Erratum #3 - exact absolute-path manifest

- Erratum #2 标题中的 `2026-07-13T13:03:00-04:00` 是前写的时间标签笔误；文件系统记录该段实际append完成时间为 `2026-07-13T12:59:21.9107165-04:00`。不影响技术内容；本条按append-only纠正。
- 第11节中用 `...\remote` 加统一展开说明的表，为避免任何实施者误读，以下给出不含省略号的权威绝对路径manifest。它只展开既有范围，不新增文件或授权。

### E3.1 Y0 exact path（唯一独立叶子）

1. New `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\PlayerFirstAidDecision.java`

### E3.2 Y1 exact paths（shared protocol gate 后）

Cloud New：

1. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\PlayerStateOperation.java`
2. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\PlayerStateRequest.java`
3. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\PlayerStateOutcome.java`

Cloud Modify：

1. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\RemoteOperation.java`
2. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\RemoteRequest.java`
3. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\RemoteOutcome.java`
4. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\RemoteCommandEnvelope.java`
5. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\RemoteCommandOutcomeEnvelope.java`
6. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\RemoteProtocolDigests.java`
7. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\RemoteFinalConsumedAck.java`
8. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskRetainedActionState.java`
9. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskRunActionLedger.java`
10. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\RemoteGameClientPort.java`
11. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskRunCommandExecutor.java`
12. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskServicePort.java`
13. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskRunExecutionGate.java`
14. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\RemoteGameCommandBroker.java`

DHXY New：

1. `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemotePlayerStateCommandPayload.java`
2. `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemotePlayerStateOutcomePayload.java`

DHXY Modify：

1. `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteGameOperation.java`
2. `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteGameCommand.java`
3. `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteGameOutcomeEnvelope.java`
4. `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteOperationPayloadCodec.java`
5. `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteProtocolDigests.java`
6. `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteOperationLedger.java`
7. `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteFinalConsumedAck.java`
8. `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\HttpRemoteCommandTransport.java`
9. `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\LocalRemoteGameCommandHandler.java`
10. `D:\mavenProject\DHXY\docs\superpowers\specs\2026-07-12-thin-client-protocol-schema.md`

### E3.3 Y2 exact paths（local mechanics gate 后）

1. New `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\playerstate\PlayerStateLocalMechanics.java`
2. New `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\playerstate\PlayerIncenseStatusRecognizer.java`
3. Modify `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\PlayerStateService.java`
4. Modify `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\LocalRemoteGameCommandHandler.java`
5. New `D:\mavenProject\DHXY\images\template\status\sheyaoxiang_digits\3.png`
6. New `D:\mavenProject\DHXY\images\template\status\sheyaoxiang_digits\3_runtime_53_fragment.png`
7. New `D:\mavenProject\DHXY\images\template\status\sheyaoxiang_digits\5_runtime_53_fragment.png`

### E3.4 Y3/Y4 exact paths（receipt-ready + Bag gates 后）

1. New `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateGovernor.java`
2. New `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateOwner.java`
3. New `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStatePort.java`
4. Modify `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskServiceExecutionContext.java`
5. Modify `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskRunAuthorityAssembly.java`
6. Modify `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\runner\context\TaskExecutionContext.java`
7. New `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\PlayerStateService.java`

E3 manifest取代第11节所有含省略号的路径展示；DAG、波次依赖、唯一Y0结论不变。仍未授权或实施上述任何文件。

## Parent Design Review #1 - BODY BLOCKED / Y0 LEAF APPROVED - 2026-07-13T13:05:00-04:00

父级对照 HEAD `0114604e` 的 23 个 public 面、46 个 production call site、当前 Full R0 request/digest/state owner、
`docs/业务逻辑.md` 与现有 first-aid aggregate 逐项复审。local capture/template/pixel/input 与 Cloud
GameContext/plan/check/incense 决策分界正确；Y0 四条 aggregate 规则逐字对应 baseline，可独立实施。主体 Y1-Y5 仍
**BLOCKED，P0=0/P1=1/P2=2**。

1. **P1：`runRevision` 被错误排除在 `requestDigest` 外。** 第8节和10.3写“runRevision不进requestDigest”，但当前
   `RequestContext` 明确规定 runRevision 受 requestDigest 覆盖，`RemoteCommandEnvelope` 也携带它；这是此前防止
   pause/resume/reconfirm 后旧 request 复活的三道 revision fence。影响：若按Y1实现会允许篡改/复活旧revision command。
   修正必须区分：runRevision **不进入 semantic address/occurrence/planId**，但必须进入每个 mechanical request 的
   `RequestContext`、wire envelope 和 canonical requestDigest，并在 Cloud enqueue/final dispatch/DHXY side-effect 前精确
   比对 current registration revision。
2. **P2：capacity exception 在精确文件表中无实现归属。** 第10.2引用不存在的 `PlayerStateCapacityException`，E3
   manifest 没列 New/nested owner。返修必须指定复用当前哪一个 typed capacity result，或把 exception 作为哪个精确文件
   的 package-private nested/top-level 类型；不得留一个无法编译的名字。
3. **P2：per-runtime service 仍接收外部 `TaskExecutionContext` 参数，却未写 exact-equality 规则。** 保留 baseline API
   可以，但 assembly-bound service 必须只使用自身 authority context；传入 ctx 只能被严格校验为同 scope/taskRun/window/
   stopEpoch/current revision，不能成为第二上下文权威，null 也不能退到 global/default。

### 已批准实施叶子 `W-PSS-Y0-IMP1`

Internal Worker Y 立即复用本日志，先追加 `CLAIMED`，唯一 Java 写集为 Cloud New
`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\PlayerFirstAidDecision.java`，另可追加本日志。
严格实施第12节：固定四字段顺序、30/50/70校验、disabled/enabled state不变量、all-disabled HEALTHY、all-enabled-
unreadable UNKNOWN+全部enabled targets、部分可读时忽略其它UNKNOWN且只收SUPPLY_NEEDED、`List.copyOf` 防御性输出。
不得接 remote/state/host/caller，不改其它文件，不新增tests，不启动运行面或Git mutation。完成后运行 Cloud
`mvn -q clean package`（不可 skip）并追加源码/构建证据；Worker自审不算父级批准。

主体返修另等待叶子源码审查后再派，不得在本波修改 wire/schema/state owner。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker Y - CLAIMED - 2026-07-13T13:06:41.8843533-04:00

- task: `W-PSS-Y0-IMP1`
- claimedAt: `2026-07-13T13:06:41.8843533-04:00`
- uniqueWriteSet: New `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\PlayerFirstAidDecision.java`；以及仅append本固定日志 `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-player-state-service-worker-y.md`
- authoritativeClaim: 本块位于 Parent Design Review #1 后的真实 EOF，明确取代此前因模糊patch锚点误插到旧日志位置的 `2026-07-13T13:06:08.1091136-04:00` 非权威记录；未在本块前创建或修改任何源码。
- scope: 只实施 Parent Design Review #1 批准的Y0纯判定叶子；Y1-Y5、wire/schema/state/host/caller/tests全部冻结；不启动运行面、不做Git mutation；Worker不承担reviewer/approval。

## Internal Worker Y - W-PSS-Y0-IMP1 SOURCE / BUILD EVIDENCE - 2026-07-13T13:11:01.4210738-04:00

### 实施源码

- 唯一Java写入：New `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\PlayerFirstAidDecision.java`；160行；SHA-256 `A5F301F9497AF1CDB30B6E38F7FB2A84BDDAEDF005DF75C39472E581DCE319D6`。
- 固定输入：`Snapshot(playerHp, playerMp, petHp, petMp)`；内部target顺序固定 `PLAYER_HP -> PLAYER_MP -> PET_HP -> PET_MP`，不接caller list/order。
- 构造不变量：所有record关键字段non-null；`normalizedThreshold`仅30/50/70；disabled bar必须 `SKIPPED_DISABLED`，enabled bar禁止该state。
- aggregate：全disabled=`HEALTHY + []`；所有enabled均UNKNOWN=`UNKNOWN + 全部enabled ordered targets`；至少一条readable时忽略其它UNKNOWN，只收readable `SUPPLY_NEEDED`；无低条=`HEALTHY + []`。
- 输出防御：`Decision` compact constructor对targets执行 `List.copyOf`；`PlannedTarget`再次校验target非null与30/50/70。
- 依赖边界：只import `java.util.ArrayList/List/Objects`；未接Spring、remote、state、host、caller、capture/input/clock；未新增tests。

### Fresh build gate

- cwd：`D:\mavenProject\dhxy-cloud-brain`
- command：`mvn -q clean package`
- 参数：未使用 `-DskipTests`、`-Dmaven.test.skip` 或其它skip。
- 结果：exit code `0`；wall time `94.1s`。
- Surefire：4个 `TEST-*.xml`；tests=21，failures=0，errors=0，skipped=0。
- compiled main class：`target/classes/com/bot/dhxy/service/PlayerFirstAidDecision.class`；SHA-256 `9083C8FF43044FDA6617B0C8ECFF5B7CDCD21AAB37FB93116AD2B72A0BD6C2AC`。
- compiled nested classes：`Bar`、`BarState`、`Decision`、`PlannedTarget`、`Result`、`Snapshot`、`Target` 均生成；`javap`确认唯一top-level public method为 `public static PlayerFirstAidDecision$Decision decide(PlayerFirstAidDecision$Snapshot)`。
- package产物：`dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`，119,937,322 bytes，SHA-256 `65405433890C34BAD0A9A0CAC42A595E537FE5B6B6976F527055C3F5F816CA18`；`original-dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`，3,036,062 bytes，SHA-256 `FE91096480127FA53E8422E558087EB0C2543C7AAD325FCE025000C3B8855A85`。

### Scope / handoff

- 除上述New Java和本固定日志append外，未修改wire/schema/state/host/caller/tests/Maven/resources；未启动任何运行面；未执行Git mutation。
- `W-PSS-Y0-IMP1` 源码与父级指定package gate已交付证据，现等待父级独立source review。Worker self-QA不构成 `SOURCE APPROVED`、`DESIGN APPROVED` 或任何reviewer approval。
- 主体Y1-Y5仍按 Parent Design Review #1 的P1=1/P2=2保持BLOCKED；本Worker未返修或触碰主体。

**无已批准业务差异；按基线等价迁移。**

## Parent Source Review #1 - FINAL APPROVED - 2026-07-13T13:14:00-04:00

父级逐行对照本日志第12节已批准合同与实际
`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\PlayerFirstAidDecision.java`：固定四字段/
target 顺序、30/50/70、enabled/disabled state 不变量、all-disabled HEALTHY、all-enabled-unreadable UNKNOWN+
全部 enabled targets、部分 readable 时忽略 UNKNOWN 且只收 SUPPLY_NEEDED、无低条 HEALTHY、`List.copyOf` 均精确
成立；无 remote/state/host/caller/capture/input/config/time 依赖，未新增 tests。

父级 fresh `mvn -q clean package` exit 0（100.3s）；Surefire 4 suites/21 tests、0 failures/0 errors/0 skipped；
shaded JAR 119,937,322 bytes，SHA-256
`11C77E2C4C511F7511EC1D4B06F653B25E98DBEA825014B89199C7BECED70685`。结论：
`W-PSS-Y0-IMP1` **FINAL APPROVED，P0=0/P1=0/P2=0**，Internal Y 可关闭。

PlayerState 主体 Y1-Y5 仍保持 Parent Design Review #1 的 `P1=1/P2=2`，本叶子批准不越过 wire/receipt-ready/
state-owner 门。**无已批准业务差异；按基线等价迁移。**

## Parent Task Brief #2 - `W-PSS-D2` - 2026-07-13T13:15:00-04:00

Internal Worker Y 复用本日志继续 PlayerState 主体 Design Repair #1。先在真实末尾追加 `CLAIMED`（task、claimedAt、
uniqueWriteSet=仅本日志）；本轮两仓 Java/Maven/schema/resources/tests、已通过 Y0、A/B/Z 写集、host/caller 全冻结。

只关闭 Parent Design Review #1 的三项：

1. 明确 `runRevision` 不进入 semantic address/occurrence/planId，但必须进入 mechanical `RequestContext`、两仓 wire 与
   canonical requestDigest，并由 Cloud enqueue/final dispatch/DHXY side-effect 三道门精确比对 current revision；逐个修正
   第8、9、10.3节的冲突陈述。
2. 删除无归属的 `PlayerStateCapacityException` 名称；对照当前 Full R0 真实 admission/capacity API，给一个可编译的 typed
   refusal/result 或在精确 New 文件表中定义 package-private owner，不得开放 raw ledger 或复用无关 quota。
3. 给所有保留 `TaskExecutionContext` 参数的 compatibility API exact-equality 合同：必须与 assembly-bound authority context
   的 scope/taskRun/window/stopEpoch/current revision 全等；null/错代/错窗 fail closed，不得回退 global/default，也不得把
   caller ctx 作为第二权威。

交付一个 Delta，不重写已通过 inventory/Y0/local-retained 分界；同步给修正后的 exact file/method manifest 和下一最小
可实施波次。若仍有门，只列一个最小 blocker、当前源码证据和 owner。结尾列 self-QA P0/P1/P2；Worker 不是 reviewer。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker Y - CLAIMED - 2026-07-13T13:16:51.9101643-04:00

- task: `W-PSS-D2`
- claimedAt: `2026-07-13T13:16:51.9101643-04:00`
- uniqueWriteSet: 仅 `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-player-state-service-worker-y.md`
- scopeFence: 仅关闭 Parent Design Review #1 的 `runRevision` mechanical digest/fence、capacity typed owner、
  `TaskExecutionContext` exact-equality 三项；两仓 Java/Maven/schema/resources/tests、已通过 Y0、A/B/Z 写集、
  host/caller 全冻结；不运行 Maven/tests/应用，不启动运行面，不做 Git mutation，不承担 reviewer/approval。

## Design Repair #1 Delta - `W-PSS-D2` - 2026-07-13T13:27:15.7055528-04:00

### D2.0 Delta 边界与当前源码依据

- 本 Delta **只**取代 Design #1 第8节中“`runRevision` 不进 requestDigest”、第9节对应的 stale-revision
  分流、第10.2节悬空的 `PlayerStateCapacityException`，以及第10.3节“`runRevision` 不进 requestDigest”与
  `prepareStartupFirstAidNoFocus(null, source)` 可由 Cloud service 接受 null 的陈述。其余已通过的 23 API/46 caller
  inventory、Y0、local-retained 分界、timer/state/fallback/NOT_EXECUTED-vs-UNKNOWN 基线不重写。
- 当前 Full R0 只读证据显示：Cloud `RequestContext` 已有 primitive `long runRevision`；Cloud
  `RemoteProtocolDigests.computeRequestDigest(RemoteRequest)` 只删除 `context.requestDigest`；Cloud
  `RemoteCommandEnvelope.from(RemoteRequest)` 已复制 `runRevision`。DHXY `RemoteGameCommand` 已有
  `Long runRevision`，`RemoteProtocolDigests.computeRequestDigest(RemoteGameCommand)` 已把它写入 canonical
  `context`。这些现有/并行文件本轮均冻结，本文只固定 PlayerState 后续必须沿用的合同。
- 当前 Full R0 capacity 只读证据：`RemoteTaskRunCoordinator.prepare` 对 run retained/nonterminal quota 抛
  `RemoteTaskRunCapacityException`；`RemoteGameCommandBroker.capacityOutcome` 对已形成的机械 request 返回
  `NOT_EXECUTED/BROKER_CAPACITY_EXCEEDED`；`CloudTaskTurnAuthority.java` 在同一源码文件末尾拥有
  package-private typed `CloudTaskTurnCapacityException`。三者都不是 PlayerState persistent-window state 的额度。
- 当前 Cloud `TaskExecutionContext` 已公开 immutable `scope/taskRunId/taskType/window tuple/stopEpoch/runRevision`
  getter，但没有 `equals`/exact-authority helper；`CloudTaskRunCurrentContextSlot.current()` 只返回通过 current-confirmed
  gate 的当前 generation。后续 service 不得用 caller 参数绕过该 assembly authority。

### D2.1 第8、9、10.3节统一修正：revision 的 semantic/mechanical 分域

#### D2.1.1 唯一合法归属

| identity/material | `runRevision` 是否进入 | 固定理由 |
|---|---:|---|
| `RemoteSemanticAddress(phaseCode,actionSlot,occurrence,attempt)` | 否 | occurrence/attempt 表达业务动作及可信续行，不表达 lifecycle generation |
| occurrence 分配/推进 | 否 | pause/resume 不凭空制造一次新业务调用 |
| PlayerState physical state key | 否 | state 按 authenticated client session + logical window + player identity epoch 跨 task/revision保留 |
| `planId` | 否 | 仍为 stable state projection + producing semantic address + canonical health fact；revision 变化不改业务计划身份 |
| mechanical `RequestContext.runRevision` | **是** | 每个 request 固定其创建时的 exact coordinator revision |
| Cloud `RemoteCommandEnvelope.runRevision` / DHXY `RemoteGameCommand.runRevision` | **是** | 两仓 wire 必须逐值传递，不允许由接收端猜测/补默认值 |
| canonical `requestDigest` | **是** | digest 绑定 request 创建时的 revision，阻止改写旧 request 令其在 resume 后复活 |
| final-consumed ACK 的 mechanical correlation | **是** | 沿用 Full R0 ACK 当前字段；它不改变 semantic address 或 planId |

canonical 公式固定为：

```text
requestDigest = SHA-256(
    RFC8785_Canonical_JSON(full typed RemoteRequest minus exactly context.requestDigest)
)
```

因此 `context.runRevision` 必须留在 hash tree；不得在 PlayerState 特判中删除、归零、改成 nullable omission，亦不得只
在 envelope 携带而不进 digest。Cloud 新 `PlayerStateRequest` 复用现有 `RequestContext`；DHXY 从 flat
`RemoteGameCommand` 重建同一 typed tree 时必须逐值放入 `context.runRevision`。payload 不重复放 revision。

#### D2.1.2 三道 current-revision fence（同一 request，三次独立重验）

| gate | 当前真实 owner/method | PlayerState 必须沿用的精确动作 | side effect 前结果 |
|---|---|---|---|
| Cloud enqueue | `RemoteGameCommandBroker.dispatchAndAwait` -> `authorizationRejection(PendingCommand)` -> `RemoteTaskRunCoordinator.authorize(...)` | digest 先验证；在 `registerAuthorizedCommandLocked`/route enqueue 前，把 exact `scope/taskRun/window/stopEpoch/context.runRevision` 与 current binding + current execution confirmation 比对 | active/current revision mismatch=`NOT_EXECUTED/TASK_RUN_MISMATCH`；PAUSED/terminal 保留现有 typed 映射 |
| Cloud final dispatch | `RemoteGameCommandBroker.selectCommandLocked` -> `RemoteTaskRunCoordinator.authorizeAndMarkDispatch(..., context.runRevision(), pending::tryDispatch)` | coordinator monitor 内完成 current authorization 与 dispatch marker 原子线性化；通过后才构造/发布 `RemoteCommandEnvelope` | mismatch 不下发，terminal outcome 写回 retained request；绝不把旧 revision command 发给 client |
| DHXY pre-side-effect | `LocalRemoteGameCommandHandler.executeOwnedCommand`/`requireRegistration`；input 另有 `workerAdmissionRevisionFence` | digest/ledger claim 后、每次 local capture/fact/action 前比对 local `RemoteTaskRunRegistration.runRevision == command.runRevision`；queued input 在 pause wait 后、focus/第一物理 step 前再做 one-shot worker-admission revision fence | 未开始任何本地动作时 mismatch=`NOT_EXECUTED/TASK_RUN_MISMATCH`；不得进入 local mechanics |

Y2 加 `PLAYER_STATE` switch 时不能建立第四条旁路：五种 observation 必须先经过 handler 的
`requireRegistration` 再调用 exact-window mechanics；`FIRST_AID_CACHED_EXECUTE` 的 queue 提交必须由 handler 保留
`workerAdmissionRevisionFence` 作为 existing worker-admission guard。不能只在 Cloud 检查，也不能只在 queue 前检查一次。

#### D2.1.3 第9节 outcome 分流勘正

1. `requestDigest` 与 typed bytes（含 revision）不符：`NOT_EXECUTED/INVALID_REQUEST`，零 local side effect；这不是
   revision authorization 结果。
2. binding 仍 ACTIVE/current-confirmed、但 request revision 不是 current：enqueue、final dispatch 或 DHXY
   pre-side-effect gate 均是 `NOT_EXECUTED/TASK_RUN_MISMATCH`。它不是业务 `UNKNOWN`、`false`、`null` 或
   `FirstAidNoFocusProbeResult.UNKNOWN`。
3. current binding 已 PAUSED：沿用现有 normal command `NOT_EXECUTED/TASK_RUN_PAUSED`；current binding terminal：沿用
   `STOPPED/STOP_REQUESTED`。coordinator 故意先分类 lifecycle status 再比较 revision，PlayerState 不得为追求统一日志而
   覆盖这两个 Full R0 映射。
4. 只有 fence 明确证明 local action 尚未开始，才可称 `NOT_EXECUTED`。已 dispatch 且 capture/input 是否发生不确定时仍是
   `UNKNOWN`；input 已开始后发生 lifecycle/revision 变化，沿用现有 pause/stop/safety outcome，绝不能倒写成“旧 revision
   所以肯定没执行”。
5. exact compacted `NOT_EXECUTED` 仅允许第9.1节原已准许的 observation 在业务尚未 claim 时以**同 occurrence、
   `attempt+1`**续行；新 attempt 使用当时 current revision，因此有新的 immutable requestDigest。cached plan 一旦按9.2
   claim，任何 exact terminal（含 stale-revision `NOT_EXECUTED`/`STOPPED`）仍按原基线完成 consume、`checks++`、public
   true，不 renew、不恢复 plan。

#### D2.1.4 pause/resume 下 frozen request 的唯一处理

- pause/resume 不改 physical state key、semantic address、occurrence 或 planId；resume 产生新的 assembly-bound
  authority context/revision。
- **尚未构造 mechanical request** 的业务调用，恢复后只能由新 bound context 构造带新 revision 的 request。
- **已经冻结 request bytes** 的 attempt 永不原地改 `runRevision/requestDigest`。若尚未 dispatch，final-dispatch gate 以
  exact `NOT_EXECUTED` 关闭；若已经 dispatch，则等待该原 request 的 exact/late outcome，禁止用新 revision 重发。
- 旧 revision service/port 即使仍被某个 stack 持有，也因 bound current gate 与三道 mechanical fence失效，不能 mutation、
  commit 或 publication。新 revision facade 复用 persistent PlayerState entry；只复用 Full R0 retained action handle/bytes
  所允许的同一 attempt，不伪造 replacement request。

以上四小节分别取代原第8节 L385 的“request digest”半句、原第9节对 stale revision 未展开的行，以及原第10.3节
L460。修正后的第10.3节应读作：**`runRevision` 不进 state key/semantic address/occurrence/planId，但必须进入
RequestContext、两仓 command wire、canonical requestDigest，并在 enqueue/final dispatch/local side-effect 三门与
current revision 精确比对。**

### D2.2 第10.2节修正：capacity typed owner 与 isolation

#### D2.2.1 删除悬空类型并选择 dedicated owner

- 原名 `PlayerStateCapacityException` **删除且禁止实施**；不新增同名文件，不在任何签名继续引用它。
- 未来 New
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateGovernor.java`
  除 package-private `final class CloudPlayerStateStateGovernor` 外，在**同一源码文件末尾**定义第二个 package-private
  top-level `final class CloudPlayerStateCapacityException extends IllegalStateException`。Java 同 compilation unit 合法，
  manifest 无缺失的额外 `.java`。
- `CloudPlayerStateCapacityException` 精确携带：
  `Dimension dimension()`、`int current()`、`int limit()`、`String logicalWindowId()`；`Dimension` 仅两值
  `GLOBAL_PERSISTENT_WINDOW_ENTRIES`、`OWNER_PERSISTENT_WINDOW_ENTRIES`。message 只含 dimension/count/limit/
  window 与安全 scope-hash，不输出 raw tenant/user/session 凭据，不编码 retry/业务返回。
- governor 的 dedicated constants 为
  `DEFAULT_GLOBAL_PERSISTENT_WINDOW_LIMIT=10_000`、
  `DEFAULT_OWNER_PERSISTENT_WINDOW_LIMIT=1_000`，并有 package-private positive-limit constructor。数值虽与 Full R0
  retained run/broker 默认值相同，但**不 import、不引用、不扣减** coordinator/broker/action/route quota；这是独立
  PlayerState persistent-window resource 的账本。

#### D2.2.2 admission 顺序与失败面

`CloudPlayerStateStateGovernor.activateInitial(TaskExecutionContext exactCurrentContext)` 的 monitor 内顺序固定：

1. 先用 assembly 提供的 non-null bound context 做 current-confirmed checkpoint，并形成 authenticated physical key
   `(tenantId,userId,deviceId,clientSessionId,logicalWindowId)`、quota owner `(tenantId,userId,deviceId)` 与 exact
   `playerIdentityEpoch`；没有 exact 值立即 fail closed，不建 `default` key。
2. 先查 exact run projection；duplicate exact activation 返回同一个 opaque handle，不二次计数。
3. 再查 physical entry。entry 已存在时不消耗新 persistent-entry capacity；epoch 相同复用同一 owner，epoch 不同则在
   同一 physical slot 原子替换整份 `CloudPlayerStateStateOwner`，仍不增加 global/owner count。旧 epoch 不并存。
4. 只有 physical entry 不存在时检查 dedicated global/owner 两个 limit；任一满额即抛上述 typed exception，且在抛出前
   **零 entry、零 usage、零 projection、零 remote request/action identity 写入**。
5. 额度通过后才创建一个 `CloudPlayerStateStateOwner`、写 entry/usage，再创建该 exact run/window 的一个 opaque
   projection handle。任何后续构造失败由同一 provisional handle 的 rollback 路径撤销 projection；仅本次新建且仍无
   其它 projection 的 entry 才同时回滚 quota，不能 evict 其它窗口。

`activateResumed(previousHandle,newExactContext)` 只对相同 scope/taskRun/taskType/window/stopEpoch、严格更大且 current 的
revision 更新 handle generation，复用同一 physical owner，不走 persistent-entry quota。`releaseTerminal` 只释放 exact
run projection，normal task terminal 不删除 business entry；`releaseClientSession` 仅由 authenticated session/window
lifecycle owner 在零 active projection 后释放该 session 的 entries。全程无 TTL/LRU/后台清理/takeover。

active run projection **不自建第二套 64 quota**。它只接受已经通过 `RemoteTaskRunCoordinator` admission/current gate 的
context，并保持每 exact run/window 一个 projection；coordinator 的 `RemoteTaskRunCapacityException` 仍只表示 run
prepare 自己的 retained/nonterminal quota，不被 catch/relabel 成 PlayerState capacity。broker 的
`BROKER_CAPACITY_EXCEEDED` 仍只表示机械 request/route/action capacity，亦不复用。

typed PlayerState capacity failure只向 retained lifecycle activation/assembly 上抛；assembly 构造失败不发布
`CloudTaskRunCurrentContextSlot`、不构造 PlayerState port/service、不创建 remote request。business facade 永远看不到 raw
governor/map/usage/ledger，也不得把该 failure 折叠为 public false/null/UNKNOWN 或自动 retry。

### D2.3 `TaskExecutionContext` exact-equality 合同

#### D2.3.1 sole authority 与比较算法

Cloud per-runtime `com.bot.dhxy.service.PlayerStateService` 构造时保存唯一
`private final TaskExecutionContext authorityContext`。任何保留显式 `TaskExecutionContext suppliedContext` 参数的 Cloud
compatibility API，第一条可执行语句都调用一个真实复用边界：

```java
private TaskExecutionContext requireExactAuthority(TaskExecutionContext suppliedContext)
```

其行为固定为：

1. `suppliedContext == null` 或 `!authorityContext.hasExactAuthority(suppliedContext)`：在任何 state read/mutation、plan
   claim、Bag/port/mechanical request 前抛 `IllegalArgumentException`；不返回业务 false/null/UNKNOWN，不找 global/default/
   current ThreadLocal/第一个窗口。
2. 字段全等后只调用 `authorityContext.throwIfStopRequested()`，由**bound** context确认该 captured revision 仍是 current
   confirmed ACTIVE；两个字段相等但同时 stale 仍会 typed unwind。不得调用 supplied context 的 `revalidate/checkpoint`
   来取得第二份 authority decision。
3. helper 返回且后续所有 private helper/port/state/Bag 调用只传返回的 `authorityContext`；supplied object 在比较完成后
   不再读、不用于 role/window/stop/request 创建。

Cloud `TaskExecutionContext` 新增 powerless comparison：

```java
public boolean hasExactAuthority(TaskExecutionContext candidate)
```

它只做 immutable field equality，不 revalidate、不 mint、不返回 delegate/raw port。精确字段为：

| dimension | 必须相等的 getter |
|---|---|
| scope | `getScope()`；record equality覆盖 tenant/user/device/clientSession 四项 |
| task run | `getTaskRunId()`；另比较 `getTaskType()` 作为 binding integrity，不拿 caller task metadata做业务输入 |
| exact window | `getWindowId()`、`getNativeWindowHandle()`、`getNativeWindowProcessId()`、`getPlayerIdentityEpoch()` 全等 |
| stop generation | `getStopEpoch()` 全等 |
| current revision snapshot | `getRunRevision()` 全等；随后仍由 bound `throwIfStopRequested()`证明它现在仍 current |

不接受“windowId相同即可”、同 hwnd 不同 epoch、同 run 不同 clientSession、old revision、对象为 null；也不要求 Java
reference identity，因为同一 exact assembly snapshot 的 powerless compatibility view 可以是不同 wrapper。

#### D2.3.2 覆盖的 API 与 no-context 入口

Cloud-retained 的九个显式 context API 必须逐个在最外层执行上述合同：

1. `performStartupFirstAidCheck(TaskExecutionContext)`
2. `prepareStartupFirstAidNoFocus(TaskExecutionContext,String)`
3. `performStartupFirstAidCheckFromPrecheckOrRun(TaskExecutionContext,long)`
4. `probeAndConsumeHealthyFirstAidNoFocus(TaskExecutionContext,String)`
5. `probeFirstAidSupplyNoFocus(TaskExecutionContext)`
6. `performCachedFirstAidPlanNow(TaskExecutionContext)`
7. `ensureSheYaoXiangActive(TaskExecutionContext)`
8. `ensureSheYaoXiangActiveInOpenMainBag(BagService.MainBagSession,TaskExecutionContext)`
9. `ensureSheYaoXiangActiveForLeaderTask(String,TaskExecutionContext)`

其中一个 public API 调另一个 public API 时，内层可再次得到同一 bound context并通过 equality，但不得把原 supplied context
继续下传为 authority；实施可用 private bound-only workflow避免无意义的 public wrapper 链。无 ctx 的 Cloud 入口
`sync*`、state read/reset、`areStatusBarsVisibleNoFocus`、两个 incense legacy overload 直接 gate/use 构造时的
`authorityContext`，这是 per-runtime binding，不是 null fallback。

DHXY local-retained `healAll(TaskExecutionContext)` 及其 local executor family 保持 `0114604e` 本地合同，不进入 Cloud
assembly-bound service，因此本 Delta 不反向给它添加 Cloud equality/fail-closed 规则。若未来改变该 local-retained 分界，
必须另开行为/迁移批准。

基线唯一 production null caller
`TaskStartupWindowPreparationService -> prepareStartupFirstAidNoFocus(null, source)` 在 DHXY local path 继续原样；Cloud cohort
cutover 前，host owner 必须改为传 `CloudTaskRunCurrentContextSlot.current()` 所属的 exact TaskExecutionContext。该 caller 未改
之前禁止把它接到 Cloud service；**不得**在 Cloud service 内把 null 替换成自身 context来掩盖 caller wiring 缺口。此处取代
原第10.3节 L461。

### D2.4 修正后的 exact file/method manifest（只列本 Delta 增量）

E3.1-E3.4 的绝对路径仍有效；以下行对 revision/capacity/context 三项有更高优先级，且不授权本轮写源码。

#### D2.4.1 revision：existing invariant 与 Y1/Y2 方法落点

| exact file | status | exact field/method contract |
|---|---|---|
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\RequestContext.java` | Existing/frozen；补入 manifest 依赖，不是 Y1 New | `runRevision` 必填 non-negative；`withRequestDigest` 原值复制 |
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\RemoteProtocolDigests.java` | E3 Y1 Modify | `computeRequestDigest` 继续只删 `context.requestDigest`；新增 `withComputedRequestDigest(PlayerStateRequest)` 时不得删 revision |
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\RemoteCommandEnvelope.java` | E3 Y1 Modify | `from` 继续复制 `context.runRevision()`；`payload(RemoteRequest)` 仅新增 closed PlayerState payload branch |
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskRunExecutionGate.java` | E3 Y1 Modify | 新 `newPlayerStateRequest(...)` 必须调用 existing `newRequestContext(...)`，由 snapshot 写 revision并在 build 前 current validate |
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\RemoteGameCommandBroker.java` | E3 Y1 Modify | PlayerState 走 existing `dispatchAndAwait/authorizationRejection/selectCommandLocked`；禁止 operation-specific enqueue/dispatch bypass |
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\run\RemoteTaskRunCoordinator.java` | Existing/frozen；只读 authority | `authorize` + `authorizeAndMarkDispatch` 的 expected revision比对不改，不加 PlayerState quota |
| `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteGameCommand.java` | E3 Y1 Modify | existing `runRevision` wire字段保持 required/non-negative；PlayerState payload validation不能补默认 revision |
| `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteProtocolDigests.java` | E3 Y1 Modify | `computeRequestDigest` 继续 `context.put("runRevision", command.getRunRevision())`，PlayerState payload按Cloud同树合并 |
| `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\LocalRemoteGameCommandHandler.java` | E3 Y1 dormant switch + Y2 live branch | 新 `executePlayerState(...)` 必须在 existing `requireRegistration` 后进入 mechanics；cached input复用 `workerAdmissionRevisionFence` |
| `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote\RemoteTaskRunRegistry.java` | Existing/frozen；只读 authority | local current registration revision owner；PlayerState不得缓存第二份 revision |

`PlayerStateRequest/Outcome`、两仓 operation/payload/codec/schema union 的其余 E3 paths不变；revision 不需要第三个 DTO、
第二份 digest helper或 PlayerState-specific registry。

#### D2.4.2 capacity/state：exact New owner 与 later assembly methods

| exact file | status | exact type/method |
|---|---|---|
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateGovernor.java` | New | `activateInitial`、`activateResumed`、`requireCurrentOwner`、`releaseTerminal`、`releaseClientSession`；nested opaque `StateProjectionHandle`/release result；同文件 package-private top-level `CloudPlayerStateCapacityException` + `Dimension` |
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateOwner.java` | New | 每 physical entry唯一 state lock与第4节 fields/transitions；无 map/quota/raw port/public constructor |
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskRunAuthorityAssembly.java` | Later Modify | 构造唯一 governor；`createCurrentContextSlotActivation` 在 publication 前 acquire/attach projection且失败rollback；`resumeTaskServiceRuntime` 用 `activateResumed`；`closeAndReleaseTerminalTaskServiceRuntime` 只 release projection |
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskServiceExecutionContext.java` | Later Modify | 持同 revision closed PlayerState port/projection view；不暴露 governor/owner/capacity map |

不新增 `PlayerStateCapacityException.java`、`CloudPlayerStateCapacityException.java`、PlayerState quota config/schema/Maven
文件；exception 与 governor 同 compilation unit，limits由 governor constructor固定/注入。

#### D2.4.3 exact context：method owner

| exact file | status | exact field/method |
|---|---|---|
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\runner\context\TaskExecutionContext.java` | E3 Y4 Modify | 新 `public boolean hasExactAuthority(TaskExecutionContext candidate)`；只比较 D2.3.1 fields，不 revalidate/暴露delegate |
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\PlayerStateService.java` | E3 Y4 New | final `authorityContext`；private `requireExactAuthority`；D2.3.2九个入口逐个执行，后续只用 bound context |
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskRunAuthorityAssembly.java` | E3 Y4 Modify | 每 slot generation只构造一个 PlayerStateService，传该 generation 的 exact TaskExecutionContext；旧 service不复用于新 revision |

### D2.5 下一最小可实施波次（提请父级审查后另行授权）

推荐唯一下一波：`W-PSS-Y3A-STATE-CORE`，Cloud **仅 New 两个文件**：

1. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateGovernor.java`
2. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateOwner.java`

该波完整实现 D2.2 的 physical index、epoch replace、dedicated quota、typed capacity exception、opaque initial/resume/
terminal projection lifecycle，以及 owner 的第4节 pure in-memory fields/lock；不接 assembly/port/wire/service/Bag/caller，
不新增 public raw API，不创建测试。两文件只依赖 JDK、已通过 Y0 与当前 Full R0 immutable context/run records，可在当前
Cloud source closure中独立编译；实施者按届时父级授权重新核对并行写集并运行 Cloud `mvn -q clean package`，本 design-only
轮未运行。后续 integration 必须在一个 assembly 波中把 provisional projection rollback 与 slot publication一起接上，不能
以“owner已存在”声称主体可运行。

选择它而不是先写 wire/port 的原因仅是依赖顺序：它是真实 persistent-state/capacity owner，且不跨 A/B/Z/shared wire、
receipt-ready、Bag、host/caller 写集；不是空 DTO、占位 facade或第二 ledger。

### D2.6 唯一最小 blocker（不扩列）

**blocker：Full R0 receipt-ready capability（含 closed per-handle status query/advance + receipt-driven wake）尚无可调用
FQCN。** 当前源码证据：

- `CloudTaskRunActionLedger.isOccurrenceComplete(...)` 与 `isRenewalCompacted(...)` 仅是 package-private retained-state
  内部探针；`FinalConsumptionPhase` 是 private enum。
- `CloudTaskServicePort` 只有同步 `readWindowFact/capture/executeInputBundle` 与 opaque action handle，没有把
  `OUTCOME_PENDING/NOTICE_PENDING/COMPACTED/CONSUMPTION_UNKNOWN` 作为 closed status能力交给 migrated service，也没有
  receipt-driven ready signal。
- 因此它阻塞 Y3 port/Y4 service 在 pending 后无 poll 地 park/re-enter；不阻塞 D2.5 两文件 pure state core。禁止由 Y 用
  sleep/poll/线程、raw ledger getter或 exception guessing替代。

owner：**Full R0 remote receipt/final-consumption capability owner（非 Y，由父级分配）**。本 Delta 不预定其文件或 API，
避免与并行 R0/A/B/Z owner 争写。

### D2.7 Worker self-QA（不是 reviewer/approval）

- `P0=0`：未发现会授权 input/cross-tenant/raw-ledger 或改变 `0114604e` 业务顺序的设计缺口。
- `P1=0`（限本 Brief 三项）：revision 已明确进入 RequestContext/wire/digest并覆盖三门；旧 request不可改写复活；
  NOT_EXECUTED/UNKNOWN/PAUSED/STOPPED 分流未混用。
- `P2=0`（限本 Brief 三项）：悬空 exception 已删除并有 exact compilation-unit owner；dedicated quota不复用无关额度；
  九个 Cloud ctx API已有 field-exact + bound-current 合同，null/错代/错窗均 fail closed。
- 外部依赖：仅登记 D2.6 一个 blocker；不把它伪装成 self-QA finding，也不声称主体获批。
- 本轮写入仅本 fixed append-only 日志；两仓 Java/Maven/schema/resources/tests、Y0、A/B/Z、host/caller 未写；未运行
  Maven/tests/应用，未启动 capture/input/host/poller，未做 Git mutation。

**无已批准业务差异；按基线等价迁移。Internal Worker Y 交付 Design Repair #1，等待父级审查；本自审不构成批准。**

## Parent Design Review #2 - BLOCKED / Repair #2 Published - 2026-07-13T13:38:00-04:00

父级复审 D2。`runRevision` 已正确进入 `RequestContext`、两仓 wire 与 canonical digest，并覆盖 Cloud enqueue、
final dispatch、DHXY pre-side-effect 三道 current-revision fence；旧 frozen request 不原地改写。dedicated capacity owner、
typed exception owner及九个显式 context API 的 exact-authority 合同也已闭合。上述部分通过，不得重开。

整体仍 **BLOCKED，P0=0/P1=1/P2=0**：

1. **P1：epoch replacement 可在旧 owner 仍有 active run projection 时原位替换。** D2.2.2 第 3 步规定 physical entry
   已存在且 epoch 不同时直接替换整份 `CloudPlayerStateStateOwner`，但 physical key 不含 epoch，且第 5 步与
   `releaseTerminal` 又明确 projection 独立存活。这样旧 projection handle 仍指向旧 owner，而 governor physical index
   已改指新 owner；旧 run 后续 mutation/release 会与索引、projection/quota 账本分叉，可能跨 epoch 写旧状态或泄漏引用。
   当前已批准的 `CloudGameContextStateOwner.activateResumed/releaseTerminal` 都先持 exact entry/execution lock，并要求
   active projection 清零后才换代/删除，不能在 PlayerState owner 放宽。

### 当前任务 `W-PSS-D3`

同一 Internal Worker Y 只在本日志真实 EOF 追加 `CLAIMED` 与 Design Repair #2 Delta；两仓 Java/Maven/schema/resources/
tests、A/B/Z 写集、assembly/port/service/caller 全冻结。返修须给一张可直接编码的 epoch-transition 表，至少覆盖：

- same epoch + exact duplicate projection：返回同一 handle，零二次计数；
- different epoch + 任一 active/provisional/release-pending projection：fail closed，entry/index/quota 全零改动；
- different epoch + exact authenticated lifecycle 已证明旧 projection 为零：在 governor monitor 内原子替换 owner，
  count 不变，旧 handle/generation 永久失效；
- replacement 构造失败：旧 entry/owner/index/quota 必须原样保留；不得先删除再补写；
- `releaseClientSession` 与 epoch replacement 的竞态、锁顺序和 idempotent 结果。

必须明确谁提供“旧 projection 已归零”的可信证明；不得用 TTL/LRU/时间、窗口标题、默认 session 或业务调用猜测。
只修这一个 P1，revision/digest/exact-context/cap 数值与唯一 Full R0 blocker均保持冻结。Worker QA 不构成父级批准。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker Y - CLAIMED - 2026-07-13T13:40:33.8606367-04:00

- task: `W-PSS-D3`
- claimedAt: `2026-07-13T13:40:33.8606367-04:00`
- uniqueWriteSet: 仅 `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-player-state-service-worker-y.md`
- scopeFence: 仅追加 Design Repair #2 Delta，关闭 Parent Design Review #2 唯一 P1；已通过的 revision/digest/
  三道 fence/exact-context/cap 数值与唯一 Full R0 blocker冻结；两仓 Java/Maven/schema/resources/tests、A/B/Z、
  assembly/port/service/caller 全冻结；不运行 Maven，不启动运行面，不做 Git mutation，不承担 reviewer/approval。

## Design Repair #2 Delta - `W-PSS-D3` - 2026-07-13T13:45:22.8563093-04:00

### D3.0 唯一修复边界

- 本 Delta 只取代 D2.2.2 中“physical entry 已存在且 epoch 不同时直接替换 owner”的单步规则，以及 D2.4.2
  `activateInitial` 的单阶段方法形状。D2 已通过的 revision/digest/三道 fence、exact-context、dedicated cap
  `10,000/1,000`、typed capacity owner、九个 context API 与 D2.6 唯一 Full R0 blocker全部冻结，不复述、不改值。
- 新不变量：**physical index 中的旧 `CloudPlayerStateStateOwner` 只可在该旧 entry 的 PROVISIONAL、ACTIVE、
  RELEASE_PENDING projection 全部为零，且没有另一 epoch transition 时被替换。** 任一非零或账本不一致均 fail closed；
  不以 current window title、时间、TTL/LRU、默认 session、业务调用返回或 coordinator “当前似乎无 run”代替本地 projection
  账本证明。
- replacement 是 fresh identity epoch 的 baseline 整体重置：candidate owner从默认字段构造，不复制旧 checks/plan/precheck/
  incense/timer。只有最终 commit改 index；prepare/构造失败时旧 owner仍是唯一可见 owner。

### D3.1 可编码数据结构与唯一 zero-proof owner

`CloudPlayerStateStateGovernor` 在单一 `governorMonitor` 下拥有：

```text
physicalEntries: Map<PhysicalKey, PhysicalEntry>
sessions:        Map<ExactSessionKey, SessionRecord>
usageByOwner/globalPersistentEntries                  // D2 cap，数值冻结

PhysicalEntry {
  PhysicalKey physicalKey; QuotaOwnerKey quotaOwner;
  long playerIdentityEpoch;
  String entryNonce; long entryGeneration;
  CloudPlayerStateStateOwner owner;
  Map<ProjectionKey, ProjectionRecord> projections;
  int provisionalCount; int activeCount; int releasePendingCount;
  long projectionMutationVersion;
  EpochReplacementReservation replacement;            // nullable，不是旧 owner projection
}

ProjectionRecord { ProjectionPhase phase; StateProjectionHandle handle; ... }
ProjectionPhase = PROVISIONAL | ACTIVE | RELEASE_PENDING
SessionPhase    = OPEN | CLOSING | RELEASED
```

结构不变量在每次 monitor mutation 前后断言：

```text
projections.size == provisionalCount + activeCount + releasePendingCount
每个 ProjectionRecord.phase 与对应 counter 一致且 counter >= 0
replacement != null 时 replacement.oldEntryNonce/generation/epoch/owner
  必须仍逐项等于 physicalEntries[physicalKey]
RELEASED session 不得拥有 physical entry、projection 或 replacement
```

**可信“旧 projection 为零”证明的唯一 owner是 `CloudPlayerStateStateGovernor` 自己，不是 assembly/caller/coordinator。**
private `requireZeroOldProjectionsLocked(PhysicalEntry)` 仅在持有 `governorMonitor` 时成功，并生成不外泄的
`ProjectionZeroWitness(entryNonce,entryGeneration,projectionMutationVersion)`；成功条件必须同时为：projection map为空、
三个 counter全零、无旧 replacement reservation、session仍OPEN。witness只在同一 monitor临界区内立即用于建立
replacement reservation或 final commit，不能返回给 business/host，也不能跨解锁保存后信任。

zero 之所以可信，是因为 projection 只有三条删除路径：

1. 未 publication 的 `PROVISIONAL` 只能由 governor-minted exact preparation handle执行
   `rollbackInitialProjection`；foreign/stale handle不能删。
2. `ACTIVE` 只能由 exact terminal lifecycle 调用先转 `RELEASE_PENDING`。terminal evidence必须来自现有
   `CloudTaskRunCurrentContextSlot.closeTerminal -> CloudTaskRunAuthorityAssembly -> retained lifecycle adapter` 链，并与
   governor handle 的 scope/taskRun/taskType/window/stopEpoch/current generation逐项相符。
3. `RELEASE_PENDING` 只有在该 owner execution lock 已排空、exact terminal binding 再次确认后，才由
   `releaseTerminal` 从 map/counter删除。`releaseClientSession` 永不伪造这一步，也不能强删非零 projection。

coordinator/slot 提供“这个 run 已 exact terminal”的外部证据；**governor projection ledger提供“这个 physical owner 已零
projection”的内部证据**。两者缺一不可，且不新增第二 lifecycle authority。

### D3.2 epoch-transition 编码表

下表中的“零改动”包含 owner、entry、physical index、三个 projection counter、mutationVersion、global/owner quota全部不变。
no-entry admission继续使用冻结的 D2 capacity规则，不在本表重开。

| current physical entry | requested activation | old projection/transition state | required action | result与精确 mutation |
|---|---|---|---|---|
| same epoch | exact same `ProjectionKey` + same revision/generation | `PROVISIONAL` | 返回同一 `StateProjectionHandle`；phase/counter/version不变，不二次构造/计数 |
| same epoch | exact same key/revision/generation | `ACTIVE` | 返回同一 handle；零二次计数 |
| same epoch | exact same key/revision/generation | `RELEASE_PENDING` | 返回同一 opaque handle但保持 RELEASE_PENDING；该 handle在 mutation入口仍被拒绝，不复活为ACTIVE，零二次计数 |
| same epoch | new exact run projection | 三个 counter全零、session OPEN、无 transition | 在现有 owner 下建一个 PROVISIONAL record；只加 `provisionalCount` 和 mutationVersion，persistent entry/quota不变；publication 前 commit为ACTIVE，失败按 exact handle rollback |
| same epoch | different/non-duplicate projection | 任一 old projection非零 | fail closed；不得以 coordinator one-window约束代替检查；零改动 |
| different epoch | 任意 | `provisionalCount>0` | fail closed `EPOCH_REPLACEMENT_PROJECTION_PRESENT`；零改动 |
| different epoch | 任意 | `activeCount>0` | 同上；零改动，旧 run继续只指旧 owner |
| different epoch | 任意 | `releasePendingCount>0` | 同上；必须等 exact terminal release真正从 ledger删除，不能因“已开始release”提前换代 |
| different epoch | exact duplicate of an existing replacement preparation | old projection为零；同 requested context/candidate reservation | 返回同一 `EpochReplacementPreparation`/future handle seed；不构造第二 candidate，不改 count/index/quota |
| different epoch | conflicting replacement request | old projection为零但已有另一 reservation | fail closed `EPOCH_REPLACEMENT_IN_PROGRESS`；旧 entry与既有 reservation均不改 |
| different epoch | fresh exact authenticated activation | old projection为零、session OPEN、无 reservation | outside-lock构造 candidate；monitor内重新取得 zero witness并只登记 `EpochReplacementReservation`；旧 owner/index仍可见，quota不变 |
| different epoch | commit prepared replacement | reservation exact；old nonce/generation/epoch/owner/mutationVersion仍等于 witness；old projection仍零；session OPEN | governor monitor内一个 commit：写 candidate owner/new epoch/new nonce、`entryGeneration+1`，清 reservation，安装新 projection；persistent global/owner count保持逐值不变 |
| different epoch | session CLOSING/RELEASED，或 witness任一字段已变化 | 任意 | preparation/commit均 fail closed；candidate丢弃或 exact rollback；旧 entry/index/quota零改动 |

`StateProjectionHandle` 必须封装 governor instance identity、physical key、entry nonce、entry generation、projection key、
projection handle generation与run revision。每个 state read/mutation/release 都逐项比对当前 entry和 exact record。
replacement commit后，entry nonce/generation同时变化；所有旧 epoch handle即使仍被外部错误保留，也永久在第一道
`requireCurrentOwner` 失败，不能跨 epoch写旧 owner或release新 projection。

### D3.3 replacement 两阶段构造、commit 与 rollback

D2.4.2 的单一 `activateInitial` 细化为以下 package-private closed methods；不新增 public/raw owner入口：

```java
InitialProjectionPreparation prepareInitialProjection(TaskExecutionContext exactCurrentContext);
StateProjectionHandle commitInitialProjection(InitialProjectionPreparation preparation);
InitialProjectionRollbackResult rollbackInitialProjection(
        InitialProjectionPreparation preparation);
```

`InitialProjectionPreparation` 是 governor-minted opaque capability，内部区分 SAME_EPOCH_PROVISIONAL 与
EPOCH_REPLACEMENT；只向同包 future assembly 暴露构造 service所需的 closed owner view，不暴露 map/quota/任意 mutation。

different-epoch 顺序固定：

1. 先在不持有任何 PlayerState lock 时验证 non-null exact bound context；短暂进入 monitor只取 old entry identity/version
   snapshot后退出，**不删除/不改** old entry。
2. 在 monitor 外调用 pure `new CloudPlayerStateStateOwner(newEpoch, injectedClock)`。constructor只分配内存、固定默认字段，
   不做 I/O/remote/Bag/capture/input。constructor抛 `RuntimeException/Error` 时没有 reservation，旧 entry/owner/index/quota
   字节级不变，异常原样向 infrastructure activation 上抛。
3. 重入 governor monitor，重新检查 session OPEN、old entry逐字段仍等于 snapshot、三个 projection counter/map全零、无
   reservation；随后登记 reservation。reservation不替换 physical index、不改epoch/generation/quota，也不把 candidate记为
   旧 owner的 PROVISIONAL projection。
4. future assembly用 preparation完成所有仍可能失败的 port/service/runtime构造。任一失败在 `finally` 调
   `rollbackInitialProjection`：同一 monitor内只清 exact reservation/同epoch provisional；old entry仍从未离开 index。
   exact重复 rollback返回 `ALREADY_ROLLED_BACK`；foreign/stale capability fail closed。
5. 所有 failable construction完成后才 `commitInitialProjection`。commit在同一 monitor内重新执行 D3.2 全部 predicate，
   然后一次性 swap owner/epoch/nonce/generation、安装新 projection、清 reservation。`Math.incrementExact` 也必须在任何字段
   写入前先求值；overflow时旧 entry保持不变。
6. commit之后只允许现有模式中的 plain handle attach + unconditional volatile slot publication；不得再安排可能失败的
   constructor、quota、remote或business步骤。commit成功后禁止 rollback旧 owner，避免已发布新 generation倒退。

prepare-vs-rollback-vs-commit由 governor monitor线性化，三者只能有一个 terminal结果。candidate从未被 physical index
可见就失败时由GC释放；不需要“先删旧 entry再补回”的补偿路径。

### D3.4 projection release 与锁顺序

沿用 Full R0 `CloudGameContextStateOwner` 的形状，PlayerState锁规则固定为：

```text
retained lifecycle/slot transition permit（若调用路径持有，最外层）
    -> CloudPlayerStateStateOwner.executionLock
        -> exact current/terminal coordinator revalidation
            -> governorMonitor
```

- 允许先短暂持 governor monitor取得/标记 record，**但必须释放 monitor后**才等待 `executionLock`；不存在
  `governorMonitor -> executionLock` 的嵌套获取。
- state invocation/release取得 owner executionLock 后，做 bound current/terminal gate，再进入 governor monitor重验
  entry nonce/generation/record phase；这是唯一允许同时持有 owner lock与monitor的方向。
- governor monitor内禁止调用 coordinator/context checkpoint、owner constructor、port/Bag/capture/input/I/O/sleep，
  禁止等待另一个锁。owner state lock内禁止反调 governor以外的业务/remote collaborator。
- `releaseTerminal` 第一次调用在 monitor内把 exact ACTIVE record改为 RELEASE_PENDING并增加对应 mutationVersion；随后
  退出monitor、等待executionLock、重验terminal，再在 executionLock -> monitor 顺序下删除record并扣
  `releasePendingCount`。中断保留 RELEASE_PENDING；相同 capability + terminal binding可重入，不能被epoch replacement越过。
- record删除后若三个 counter归零，只有 governor可在 monitor内得到新的 zero witness；并不自动触发epoch replacement。

### D3.5 `releaseClientSession` race 与 idempotence

签名收紧为 closed capability，不接受裸 scope/default：

```java
SessionReleaseResult releaseClientSession(SessionReleaseHandle exactSessionHandle);
```

`SessionReleaseHandle` 由 governor在该 authenticated session首次 admission时铸造，含 governor identity、完整
`RemoteTaskRunScope`、session nonce；由 retained session lifecycle owner保存。`SessionReleaseResult` 三值：
`RELEASED`、`PENDING_PROJECTIONS`、`ALREADY_RELEASED`。foreign/stale handle抛 infrastructure error，不伪装幂等成功。

| monitor线性化时的状态 | `releaseClientSession` 动作 | result/后续 |
|---|---|---|
| session OPEN，所有 physical entries projection均零、无 replacement | 原子改CLOSING，移除该session所有entries/index，按实际entry数一次性扣global/owner quota，改RELEASED并在handle/session record保存terminal result | 首次 `RELEASED`；以后同handle `ALREADY_RELEASED` |
| session OPEN，任一 PROVISIONAL/ACTIVE/RELEASE_PENDING非零 | 只改session为CLOSING；entry/index/quota全不动，新activation/replacement从此fail closed | `PENDING_PROJECTIONS`；每次 terminal/provisional rollback继续正常收敛 |
| session OPEN，old projection为零但有未commit replacement reservation | release取得monitor后把reservation标CANCELLED并丢candidate，旧entry仍原样；随后按零projection路径完成session cleanup | `RELEASED`；并发commit看到CANCELLED/session非OPEN后fail closed |
| session CLOSING，仍有projection | 不重复扣数、不改entry | `PENDING_PROJECTIONS` |
| session CLOSING，最后一个projection刚由exact release删除 | `releaseTerminal` 在同一monitor调用 private `completeSessionReleaseLocked`，一次性移除entries/扣quota/标RELEASED | 原 pending handle之后观察 `ALREADY_RELEASED`；无需poll/thread/TTL |
| session RELEASED，同一个 exact handle | 零改动 | `ALREADY_RELEASED` |

竞态结果因此唯一：

- release先取得 monitor：session先CLOSING；prepared replacement被取消，尚未prepare的replacement直接拒绝。
- replacement commit先取得 monitor：它原子安装新 epoch projection；release随后只能标CLOSING并返回
  `PENDING_PROJECTIONS`，绝不删除新/旧 owner。待该projection exact terminal release后统一cleanup。
- terminal release与session cleanup同时发生：两者都在 governor monitor检查/扣账，`SessionPhase.RELEASED` 是一次性CAS式
  terminal marker；quota只在 `completeSessionReleaseLocked` 一处扣一次。
- exact duplicate session release以 retained `SessionReleaseHandle`/terminal result幂等；不用时间窗口、scope字符串猜重入。
  minimal RELEASED tombstone只由曾成功admit的authenticated session产生，随Cloud incarnation存在，不做TTL/LRU eviction。

### D3.6 对 D2 manifest 的唯一增量修正

未来
`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateGovernor.java`
仍是唯一 projection/index/quota/epoch owner；其 exact method manifest以 D3.3 的 prepare/commit/rollback、既有
`activateResumed`、`requireCurrentOwner`、`releaseTerminal` 与 D3.5 closed `releaseClientSession` 为准。
`CloudPlayerStateStateOwner.java` 仍只持每physical entry state/executionLock，不持 index/quota/session map。没有第三个
epoch registry、zero-proof DTO、public session getter或额外 Java文件。

D2.5 `W-PSS-Y3A-STATE-CORE` 两个 New 文件的候选波仍需父级复审后另行授权；本 Delta不授权落码，也不触碰已冻结
assembly/port/service/caller。D2.6 唯一 Full R0 receipt-ready blocker原文、owner与影响范围保持不变，不新增第二 blocker。

### D3.7 Worker self-QA（不是 reviewer/approval）

- `P0=0`：没有 raw scope/default/takeover/强删 projection；session cleanup不能伪造 terminal或跨epoch mutation。
- `P1=0`（限 Parent Review #2 唯一项）：different epoch必须 old projection三态全零；zero proof由 governor ledger提供；
  replacement直到 atomic commit前不替换index；旧 handle以nonce+generation永久失效。
- `P2=0`：constructor/downstream failure、commit/rollback race、release-pending、session release竞态、quota一次性扣账与锁顺序
  均有单一 owner和可编码 terminal结果，无悬空 boolean/类型。
- 已通过的 revision/digest/三道 fence/exact-context/cap数值未重开；唯一 Full R0 blocker未改写。自审不等于父级通过。
- 本轮除 fixed append-only 日志外零写入；未运行 Maven/tests/应用，未启动任何运行面，未做 Git mutation。

**无已批准业务差异；按基线等价迁移。Internal Worker Y 完成 Design Repair #2，停下等待父级复审。**

### D3.8 Append-only self-QA precision - 2026-07-13T13:47:59.7762199-04:00

本段只消除 D3.1/D3.3 的编码歧义，优先于其中“同一 predicate”式简写，不改变任何状态结论：

1. **prepare zero check** 调 `requireZeroOldProjectionsLocked(entry)`：要求 projection map/三counter全零、
   `entry.replacement == null`、session OPEN；成功后才登记该次 reservation。
2. **commit zero recheck** 不重新调用上一个“replacement必须null”的方法，而调用
   `revalidateZeroOldProjectionsLocked(entry, exactReservation)`：仍要求 projection map/三counter全零，但必须满足
   `entry.replacement == exactReservation`，并逐项匹配 reservation保存的 old nonce/generation/epoch/owner/
   projectionMutationVersion。任何其它/null reservation均 fail closed。witness/reservation均不离开 governor package，
   caller不能提交 boolean zero proof。
3. different-epoch commit在一个 monitor临界区内把 old owner换成candidate并安装该 exact run的 **ACTIVE** record：
   `activeCount` 从0到1、`projectionMutationVersion`精确加1；`provisionalCount/releasePendingCount`保持0；D3.2所称
   “count不变”专指 persistent-entry global/owner quota count逐值不变，不是 projection count不变。
4. session `CLOSING` 只禁止新 prepare/commit/resume/mutation；必须继续允许 exact
   `rollbackInitialProjection` 与 `releaseTerminal` 收敛已有 PROVISIONAL/ACTIVE/RELEASE_PENDING。最后一次删除后，
   `completeSessionReleaseLocked` 必须重新扫描该 session **全部** physical entries均为零且无 reservation，才一次性清理；
   不能只看触发删除的那个窗口。

经此精化，D3 self-QA仍为 `P0=0/P1=0/P2=0`（仅 Worker自审），且仍只等待父级复审，不构成批准。

### D3.9 Projection counter mutation table - 2026-07-13T13:48:37.8905477-04:00

为使 zero-proof 可逐行编码，所有 projection ledger mutation只能采用下表；每行都在 governor monitor内先验证结构不变量、
用 `Math.incrementExact/decrementExact` 先求新值，再一次提交：

| event | map/phase mutation | counter mutation | `projectionMutationVersion` |
|---|---|---|---|
| exact duplicate prepare/activate/release reentry | 无 | 无 | 不变 |
| same-epoch new preparation | insert `PROVISIONAL` | `provisional +1` | `+1` |
| same-epoch commit | `PROVISIONAL -> ACTIVE` | `provisional -1, active +1` | `+1` |
| exact provisional rollback | remove `PROVISIONAL` | `provisional -1` | `+1` |
| different-epoch reservation prepare/cancel | 只写/清 `entry.replacement`，不进projection map | 三counter不变 | 不变；reservation identity自身负责冲突检测 |
| different-epoch atomic commit | swap owner并insert `ACTIVE` | `active 0 -> 1`，其余保持0 | `+1` |
| terminal release begin | `ACTIVE -> RELEASE_PENDING` | `active -1, releasePending +1` | `+1` |
| terminal release final | remove `RELEASE_PENDING` | `releasePending -1` | `+1` |

任何 map/phase 与预期不符、counter underflow/overflow或 version overflow都在首次字段写入前 fail closed，entry/index/quota原样。
本表不新增 projection quota，亦不改变 D2 persistent-entry cap。

## Parent Design Review #3 - DESIGN APPROVED - 2026-07-13T13:54:00-04:00

父级按 `CloudTaskRunAuthorityAssembly` / `CloudGameContextStateOwner` 当前 publication、terminal-release 与锁序复审
`W-PSS-D3`。D3 已关闭 Review #2 的唯一 P1：可信 zero proof 只由 governor 自身 projection map、三类 counter、
`projectionMutationVersion` 与 exact replacement reservation 共同形成；different epoch 在任一
PROVISIONAL/ACTIVE/RELEASE_PENDING 存活时零改动拒绝，candidate 在 monitor 外构造，commit 在同一 monitor 内重验
old nonce/generation/epoch/owner/version/reservation 后才原子替换 owner 并安装 exact ACTIVE projection。旧 handle 通过
governor identity、entry nonce/generation 与 projection key 永久失效；persistent-entry global/owner quota 在 replacement
中逐值不变。`releaseClientSession` 的 OPEN/CLOSING/RELEASED、reservation cancel、terminal release 与一次性 quota 清账
也已有唯一线性化结果，且锁序固定为 lifecycle permit -> owner executionLock -> governor monitor。

结论：**DESIGN APPROVED，P0/P1/P2=0**。以下两点是绑定实施条件，用于消除编码歧义，不要求 Worker 再交一轮文字返修：

1. D3.5 的 RELEASED session marker 不得形成第三个无上界账本。实现只能在当前 coordinator incarnation 的 exact
   quota-owner/session 记录内保留一个 terminal marker，并把其基数纳入已批准的 `10,000/1,000` dedicated hard cap；
   exact `SessionReleaseHandle` 重入读该 marker，不能另建未计费 tombstone map、TTL/LRU 或 takeover。
2. `completeSessionReleaseLocked` 只在全 session entries/projections/reservations 均为零时执行一次；任何 constructor、
   overflow、foreign/stale handle 或 session-phase 校验失败都发生在首次字段写入前，不能靠补偿删除恢复旧 owner。

下一可实施波仍仅为 D2.5 的两个 Cloud New 文件；assembly/port/wire/service/caller 继续冻结，须由父级另行发单并在落码后
fresh `mvn -q clean package`。Worker self-QA 不计批准，本条为唯一父级结论。

**无已批准业务差异；按基线等价迁移。**

## Parent Implementation Task - W-PSS-Y3A-STATE-CORE-IMP1 - 2026-07-13T14:00:00-04:00（EOF 权威块）

说明：同标题任务曾因 append 锚点过宽误插入旧历史位置；**仅本真实 EOF 块是当前任务权威**。Internal Worker Y 的 D3
已父级 DESIGN APPROVED。立即在本日志真实 EOF 追加 `CLAIMED` 后实施唯一最小 Cloud state-core 波；唯一 Java 写集仅为
以下两个当前不存在的新文件：

1. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateGovernor.java`
2. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateOwner.java`

实现必须逐项落实 D2.2、D3.1-D3.9 与 Parent Review #3：dedicated `10,000/1,000` hard cap、authenticated physical key、
same-epoch exact duplicate、PROVISIONAL/ACTIVE/RELEASE_PENDING 账本、different-epoch two-phase reservation/constructor/
atomic commit、old handle permanent stale、exact initial/resume/terminal projection、closed session release、单一锁序与有界 terminal
session marker。Governor 是唯一 map/index/quota/epoch/projection owner；Owner 只持单 physical state + execution lock，不持第二
index/quota/session map。不得新增第三个文件、public raw getter、ThreadLocal/default key、TTL/LRU/takeover/retry、I/O、线程或
placeholder API；不得修改 assembly/port/wire/service/caller/host/tests/Maven。

你不是唯一在代码库工作的 Worker：保护全部现有 dirty/untracked，不回滚、不覆盖他人编辑；若目标文件在领取前突然存在，
立即停下在日志报告冲突。完成后运行 Cloud `mvn -q clean package`（不可 skip），向本日志追加精确方法/不变量、构建结果与
self-QA；自审不构成父级批准。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker Y - CLAIMED (true EOF authority) - 2026-07-13T14:01:29.1217335-04:00

- task: `W-PSS-Y3A-STATE-CORE-IMP1`
- claimedAt: `2026-07-13T14:01:29.1217335-04:00`
- uniqueWriteSet:
  - `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-player-state-service-worker-y.md`
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateGovernor.java`
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateOwner.java`
- appendOnlyCorrection: `2026-07-13T14:01:29.1217335-04:00` 的首个 CLAIMED 因宽锚点误插入日志前部历史重复块；不删除、不改写该记录。本块位于 `14:00:00-04:00` 权威任务后的真实 EOF，是本次实施领取的唯一权威记录。
- scopeFence: 只实施 D2.2、D3.1-D3.9 与 Parent Design Review #3 已批准的 state core；不改 assembly/port/wire/service/caller/host/tests/Maven，不启动运行面，不做 Git mutation，不承担 reviewer/approval。

## Internal Worker Y - DELIVERY / SELF-QA - `W-PSS-Y3A-STATE-CORE-IMP1` - 2026-07-13T14:23:18.6572804-04:00

### 1. 精确源码交付

仅新建父级授权的两个 Cloud 文件：

1. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateGovernor.java`
   - 1713 行，SHA-256 `CAF5F7F881BF01E7F3F5844DE65705DBA6328CDB686B2F1481C77EDD569BC4FA`。
   - package-private `final CloudPlayerStateStateGovernor`；精确 lifecycle 方法为
     `prepareInitialProjection`、`commitInitialProjection`、`rollbackInitialProjection`、`activateResumed`、
     `requireCurrentOwner`、`releaseTerminal`、`releaseClientSession`。
   - 同 compilation unit 的 package-private `final CloudPlayerStateCapacityException` 精确提供
     `dimension/current/limit/logicalWindowId`；`Dimension` 仅
     `GLOBAL_PERSISTENT_WINDOW_ENTRIES`、`OWNER_PERSISTENT_WINDOW_ENTRIES`。
2. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateOwner.java`
   - 460 行，SHA-256 `2011362BBB6723DE4AA9A1CFE5828025A48D3DDC3D8DCB85225CFE5CA95D2D75`。
   - 仅持一个 `playerIdentityEpoch`、注入 `Clock`、一个 `ReentrantLock` 与 baseline check/plan/precheck/
     incense 字段；不持 map/index/quota/session/remote collaborator。
   - 已实现 reset/startup check、pending -> in-flight lease -> exact terminal complete、startup precheck
     store/consume-first-clear、37 分钟 incense quiet、trusted remaining 回推、icon offset 与 used timer transition；
     `nextIncenseRetryTime` 无非零 writer。

### 2. D2.2 / D3.1-D3.9 / Parent Review #3 实施不变量

- physical key=`(tenant,user,device,clientSession,logicalWindowId)`；quota owner=`(tenant,user,device)`；
  `playerIdentityEpoch` 只作 physical entry 强 version，不进 key；无 null/default/ThreadLocal/window-title fallback。
- dedicated hard cap 固定 global `10,000`、owner `1,000`；same physical key/duplicate projection 先于 quota；
  不引用 coordinator/broker/action/route capacity，不建第二套 64 projection quota，不做 eviction/TTL/LRU/retry。
- Governor 是 `physicalEntries/sessions/usageByOwner`、entry epoch/nonce/generation、projection map/三 counter/
  `projectionMutationVersion` 与 replacement reservation 的唯一 owner；每次 lifecycle mutation 前后执行结构与计费不变量检查。
- same-epoch exact duplicate返回同一个 preparation/handle，零二次计数；新 preparation 精确
  `PROVISIONAL +1/version +1`，commit 精确 `PROVISIONAL -> ACTIVE`、`provisional -1/active +1/version +1`，
  rollback 精确 remove/`provisional -1/version +1`。
- different epoch 只在旧 map 与 `provisional/active/releasePending` 全零、session OPEN、无 reservation 时允许；
  candidate owner/nonce 在 monitor 外构造，monitor 内保存 old nonce/generation/epoch/owner/version reservation；
  commit 重验 exact reservation 后用新 `PhysicalEntry` 单次替换并安装 exact ACTIVE projection，persistent quota逐值不变。
  任一旧 projection、冲突 reservation、constructor/overflow/session-phase/stale capability 均在首次账本写入前 fail closed。
- replacement commit 后 entry nonce 与 generation 同时变化，旧 epoch handle 在 `requireCurrentEntryLocked` 永久 stale；
  candidate/下游失败只清 exact reservation或 provisional，旧 owner/index/quota不做“先删后补”补偿。
- exact resume 在旧 owner execution lock 排空后执行 bound current gate，再只提升 handle generation/run revision；
  owner、physical entry、projection counters与 persistent quota不变，旧 revision handle永久 stale。
- exact terminal 首次在 monitor 内 `ACTIVE -> RELEASE_PENDING`、`active -1/releasePending +1/version +1`，
  退出 monitor 后等待 owner execution lock，二次校验 coordinator exact STOPPED/COMPLETED binding，再
  remove/`releasePending -1/version +1`；中断保留 RELEASE_PENDING，同 capability可幂等重入。
- 唯一锁序为外部 lifecycle/slot permit（未来 assembly）`-> owner.executionLock -> context/coordinator revalidation -> governorMonitor`；
  代码中不存在 `governorMonitor -> executionLock` 等待，monitor 内无 I/O/port/capture/input/sleep/owner constructor。
- `releaseClientSession` 只接受 governor-minted exact `SessionReleaseHandle`，线性化结果仅
  `RELEASED/PENDING_PROJECTIONS/ALREADY_RELEASED`；OPEN -> CLOSING 后拒绝新 prepare/commit/resume/mutation，
  但 exact provisional rollback/terminal release继续收敛；reservation被 exact cancel，最后一次删除会扫描全 session。
- RELEASED marker 只保留在同一 `sessions` exact record，不另建 tombstone map；cleanup用一个计费 marker替换该 session
  的 N 个 physical-entry units，因此 marker 基数始终纳入同一 `10,000/1,000` hard cap并随 governor/coordinator incarnation存活。

### 3. Fresh build 与产物证据

- cwd：`D:\mavenProject\dhxy-cloud-brain`
- command：`mvn -q clean package`（未使用任何 skip）
- started：`2026-07-13T14:20:03.8209195-04:00`
- finished：`2026-07-13T14:21:45.8115070-04:00`
- elapsed：`101964 ms`
- exit code：`0`
- Surefire：`4` suites，`21` tests，`0` failures，`0` errors，`0` skipped。
- `target/classes/.../CloudPlayerStateStateGovernor.class` SHA-256
  `1555E4F44830EA1571A654F4CCC756A080FFA203548F4C9AA26FE6B073F9F4BC`；
  `CloudPlayerStateStateOwner.class` SHA-256
  `93B32941F16B738EDCBFCFD05BDDDB26CB48CFE11ACE37E6BB9508013022753E`；
  `CloudPlayerStateCapacityException.class` 已由同一 source unit生成。
- packaged jar：`target/dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`，120015321 bytes，SHA-256
  `44F01028D363E6DCD6989987D2ABF85E602666B5E18275AC6A9FB94015ADB4E2`。

### 4. Scope 与 Worker self-QA（不构成父级批准）

- 精确源码写集仅上述两个 New Java；唯一其它写入仅本 fixed append-only 日志。未修改 assembly/port/wire/service/
  caller/host/tests/Maven/schema/resources，未启动 capture/input/host/poller/应用，未做 Git mutation、暂存或提交。
- `P0=0`：无跨 scope/default/raw map/强删 live projection/运行面输入能力；无已批准业务差异。
- `P1=0`：不同 epoch 必须 old projection三态全零；zero proof、reservation、atomic replacement、old-handle stale、
  terminal release与session cleanup均由一个 governor monitor线性化，旧 owner不会在 projection存活时被替换。
- `P2=0`：typed cap owner、三 counter/version算术、constructor/overflow前置、单锁序、幂等 terminal/session result、
  bounded RELEASED marker与 Owner baseline fields均已落为可编译实现。
- 本段仅为 Internal Worker Y 的 source/build/self-QA 交付，**不构成 reviewer、SOURCE APPROVED 或父级批准**；等待父级独立审查。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Source Review #1 - BLOCKED P1=1/P2=0 - 2026-07-13T14:29:30-04:00（真实 EOF 权威块）

父级独立审查两个新增 Cloud 文件及 Worker 构建证据。`CloudPlayerStateStateOwner` 的基线字段、锁边界与纯状态
职责未发现 P0/P1/P2；Governor 的 physical key、quota owner、epoch replacement、session release 与 terminal
收敛总体符合已批准设计。但 `CloudPlayerStateStateGovernor.commitInitialProjection(...)` 存在一个会重新发出 stale
handle 的 P1，当前切片不能批准：

- **证据：**`CloudPlayerStateStateGovernor.java:161-175` 在 preparation 已是 `COMMITTED` 时，分别在第一次和第二次
  monitor 区直接返回 `preparation.committedHandle`；第一次早退甚至发生在
  `preparation.exactContext.throwIfStopRequested()` 之前。`activateResumed(...)` 在同文件 `:263-273` 会把
  `ProjectionRecord.handle` 替换为严格更新 revision/generation 的新 handle，但不会改
  `preparation.committedHandle`。
- **影响：**initial commit 的延迟/幂等重放可以在 pause/resume 后重新拿到旧 H1；terminal/replacement 后也可绕过
  current-entry/current-record 校验返回历史 capability。旧 revision 本应永久 stale，却可能被未来 assembly 重新装入或
  延迟到业务调用时才失败，破坏“激活 API 自身只返回 current handle”的合同。
- **精确返修条件：**只修改 `CloudPlayerStateStateGovernor.java` 与本日志。`COMMITTED` 重入不得无条件返回缓存 handle；
  必须先执行原 preparation 的 exact-context stop/revision fence，并在 `governorMonitor` 内证明当前 physical entry、entry
  nonce/generation、projection record、`record.preparation == preparation`、ACTIVE phase、
  `record.handle == preparation.committedHandle` 全部仍 exact，才允许幂等返回。resume 后 handle 已替换、terminal 已转态/
  移除、epoch replacement 或 session close 后均须 fail closed，不能返回旧 handle。保持无新 wrapper/权威/测试/业务差异。
- **复验门：**Worker 追加 Repair #1、重跑 Cloud `mvn -q clean package`（不可 skip），父级再做源码与 fresh build 审查。

结论：`BLOCKED`，P0=0/P1=1/P2=0。Worker self-QA 与原 package 绿不构成批准。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Internal Worker Y - CLAIMED Repair #1 - 2026-07-13T14:36:11.7731667-04:00

- task: `W-PSS-Y3A-STATE-CORE-IMP1-REPAIR1`
- claimedAt: `2026-07-13T14:36:11.7731667-04:00`
- uniqueWriteSet:
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateGovernor.java`
  - `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-player-state-service-worker-y.md`
- scopeFence: 仅关闭 Parent Source Review #1 的 COMMITTED preparation stale-handle P1；Owner/assembly/port/wire/其他源码与 tests/Maven冻结；不新增 wrapper/authority/业务差异，不做 Git mutation。

## Internal Worker Y - Repair #1 DELIVERY / SELF-QA - 2026-07-13T14:39:16.7106145-04:00

### 1. 唯一源码 diff

- 修改文件仅：
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudPlayerStateStateGovernor.java`。
- preimage：1713 行，SHA-256
  `CAF5F7F881BF01E7F3F5844DE65705DBA6328CDB686B2F1481C77EDD569BC4FA`。
- postimage：1732 行，SHA-256
  `B5E17B474C11EC6D2FBBD0B01814E78D807CA4E47982A2D51B1597FD1702F713`。
- 精确方法：仅 `commitInitialProjection(InitialProjectionPreparation)`；未新增/删除/改签任何方法、helper、wrapper、
  authority、field、enum 或 nested type。
- 精确删除：第一个 monitor 内的
  `if (preparation.phase == COMMITTED) return preparation.committedHandle`；第二个 monitor 内同一无条件 cached-handle return。
- 精确新增：第二个 monitor 的 COMMITTED 分支先绑定 `committedHandle/currentEntry/currentRecord`，并逐项要求：
  1. `committedHandle != null` 且 `physicalEntries[preparation.physicalKey] != null`；
  2. current entry 的 `physicalKey/entryNonce/entryGeneration` 与 committed handle exact；
  3. committed handle 的 `projectionKey/runRevision` 与 preparation exact；
  4. current record存在，且 `record.preparation == preparation`、`record.phase == ACTIVE`、
     `record.handle == committedHandle`；
  5. current entry 的 exact session仍 `OPEN`；
  6. 以上全部成立才 `return committedHandle`，任一不成立抛 infrastructure `IllegalStateException`。
- `CloudPlayerStateStateOwner.java` 零修改；post-build SHA-256仍为
  `2011362BBB6723DE4AA9A1CFE5828025A48D3DDC3D8DCB85225CFE5CA95D2D75`。

### 2. 精确执行与竞态时序

COMMITTED preparation 重入固定为：

```text
requireOwnedPreparation
  -> governorMonitor: verify invariants；仅保留 ROLLED_BACK 原错误分流，不返回 COMMITTED
  -> preparation.exactContext.throwIfStopRequested()  // exact stop/revision fence
  -> governorMonitor: verify invariants
       -> prove current entry identity
       -> prove current ACTIVE projection/preparation/handle identity
       -> prove exact session OPEN
       -> only then idempotently return the same committed handle
```

- immediate exact duplicate：context仍 current、record仍 ACTIVE且仍指同 handle，返回原 handle，保持既有幂等合同。
- pause/resume：旧 exact context在 fence fail closed；即使 transition恰在 fence后线性化，monitor看到
  `record.handle != preparation.committedHandle`，激活 API 不再返回 H1。
- terminal begin/final：旧 context fence fail closed；竞态窗口内 monitor分别看到 phase已非 ACTIVE或 record已移除。
- later epoch replacement：current entry nonce/generation或 record identity不再匹配，fail closed。
- client-session CLOSING/RELEASED：`requireOpenSessionLocked` 或 current-entry存在性检查 fail closed；不会从 session close后重发缓存 handle。
- fence 与 monitor之间发生的任一 lifecycle transition都由第二段 current-entry/current-record/session重验捕获；未新增第二份
  revision authority，也未修改 resume/terminal/replacement/session 的原线性化 owner。

### 3. Fresh build 证据

- cwd：`D:\mavenProject\dhxy-cloud-brain`
- command：`mvn -q clean package`（不可 skip，实际未使用任何 skip）
- started：`2026-07-13T14:37:19.0027216-04:00`
- finished：`2026-07-13T14:38:46.1793600-04:00`
- elapsed：`87153 ms`
- exit code：`0`
- Surefire：`4` suites，`21` tests，`0` failures，`0` errors，`0` skipped；按父级/用户冻结条件未新增或修改测试。
- compiled `CloudPlayerStateStateGovernor.class` SHA-256：
  `029DEBAE6F652BCC8F68F54198E974E3B6C8378ECF3C294DF42CE3B2CD156954`。
- packaged `target/dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`：120015548 bytes，SHA-256
  `503DC296EE4AAFBC0E9ACF5E963E8B798A5D5E3A024860F35C560B325EEF48BA`。

### 4. Scope / Worker self-QA（不是父级批准）

- 唯一 Java 写入为 Governor 上述单方法；唯一其它写入为本 fixed append-only 日志。Owner/assembly/port/wire/
  service/caller/host/tests/Maven/schema/resources均未修改；未启动运行面，未做 Git mutation、暂存或提交。
- `P0=0`：无新增 capability/public raw getter/跨 scope路径，无 input/capture/remote side effect变化。
- `P1=0`（仅 Parent Source Review #1）：COMMITTED 重入已先过 exact-context fence，并由同一 governor monitor证明
  current entry + ACTIVE exact record + exact cached handle + OPEN session；stale handle不再由激活 API重发。
- `P2=0`：无新 wrapper/helper/authority/测试；PREPARED 首次 commit、ROLLED_BACK 分流、quota/projection counter/
  replacement/terminal/session状态机均未改动。
- **无已批准业务差异；按 `0114604e` 基线等价迁移。** 本 Worker self-QA 与 fresh package 证据不构成
  reviewer、SOURCE APPROVED 或父级批准，现等待父级独立复审。

## Parent Source Review #2 - SOURCE APPROVED / FINAL BUILD PENDING - 2026-07-13T14:43:00-04:00（真实 EOF 权威块）

父级逐行复审 Repair #1。`commitInitialProjection(...)` 已删除两处 COMMITTED 无条件早退；现在先执行原
`exactContext.throwIfStopRequested()`，再在 `governorMonitor` 内同时证明 current physical entry 的 nonce/generation、
preparation projection/revision、`record.preparation` identity、ACTIVE phase、cached handle identity 与 session OPEN。
因此 immediate exact duplicate 仍幂等返回同一 handle，而 resume 更换 handle、terminal 转态/移除、epoch replacement 或
session close 均 fail closed；fence 与 monitor 之间的竞态也由第二段 current-record proof 截住。唯一 Java diff 未新增
wrapper/authority/测试，Owner 与其它 governor 状态机零改动。

源码结论：`SOURCE APPROVED，P0/P1/P2=0`。Worker 的 Cloud package 4 suites/21 tests 已绿；因 Internal Z 正在同一 Cloud
main source set 连续完成 SummonSkill 原子波，父级不在写入期并发 `clean`。本切片的 `FINAL APPROVED` 只等待 Z 稳定后父级
统一 fresh Cloud `mvn -q clean package`；Y 无需再改源码，可结束原会话。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**
