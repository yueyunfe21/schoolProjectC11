# Worker L：`BattleRadarService` Cloud lift-and-shift

## Parent Task Brief #1 - 2026-07-12

### 目标与阶段

为 AutoCombat W1 独立设计 `BattleRadarService` 整类 Cloud lift-and-shift。业务源码只以 DHXY HEAD `0114604e` 为权威，
保持战斗进入/退出、expected fast-exit、头像 diff、动态轮询、外部裁决、fallback/计数/时间与日志语义；截图和窗口事实只经
既有 retained typed Service port。首轮只追加 `Internal Worker L - Design #1`；父级 `DESIGN APPROVED` 前零 Java/Maven/
resources/tests。

### 必读与保护

- 完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/业务逻辑.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、迁移矩阵及
  A 的 `2026-07-12-cloud-auto-combat-service-worker-a.md` 最新全部材料。
- 只读核对 HEAD `BattleRadarService.java` 全类、全部 main caller、直接/间接截图与模板/OCR、`GameContext`、配置、timer、
  expected/fast-exit、external arbitration、pause/stop 路径；当前工作区 dirty 不能作为业务基线。
- 读取 Cloud 当前 `TaskExecutionContext`、`CloudTaskServicePort`、retained action state/ledger、H State owner、I properties、
  Cloud-native image processor 与 packaged template/artifact 边界；保护两仓全部 dirty/untracked。
- 你不是独自在仓库中。不得修改外部 A PAUSED observer 的 `remote/**`/DHXY wire，外部 B artifact 七文件，K current-slot/
  assembly/service-context 写集，已批准 J turn 文件，或其它 Worker 报告。

### 设计不变量

1. 全部 public/private 方法、caller、返回/异常、GameContext/state 副作用、timer 单位、模板顺序/阈值、进入退出判定、
   expected fast-exit 与日志关键字逐项等价；不新增 capture、verification、TTL、retry、cleanup、park/yield 或 fallback。
2. Cloud 不得复制 tracker、WindowRuntimeContext/holder、HWND、Robot/JNA、local Path/temp、InputSequences/InputProvider、
   TaskPauseToken、线程/poller 或本地 runner authority。
3. 每次机械 fact/capture 必须由 exact current TaskExecutionContext + trusted retained semantic action address 取得稳定 identity；
   UNKNOWN 只同字节重投，未解析/STOPPED/EXECUTED 不换新。不得开放 raw request/poll/outcome。
4. 图像解释只用 Cloud in-process canonical image/template owners；不得新建第二 template loader、任意 Path 或本地 OCR fallback。
5. PAUSED read-only observer 只作为 A W0 的未来 typed capability 依赖，不在本 L 修改/抢实现；普通业务 context 仍只 confirmed
   ACTIVE。host/Task/caller 全程 dormant。
6. 设计须给出可编译 dependency DAG，不以“未来接口”掩盖缺文件；若某 collaborator 未迁，列 final typed seam 与独立波次，
   生产只整体切换，不形成长期双脑。

### Design #1 必交付

- HEAD 全方法/caller/状态/timer/模板/截图/业务分支矩阵，标出 AutoCombat 实际依赖子集。
- public API compatibility 与最小 explicit-context 适配表；不得 ThreadLocal/ambient current。
- 每个 capture/fact 的 retained action address、原子 capture 边界、timeout、UNKNOWN/final 与图像 ownership/flush 映射。
- `GameContext.State`、per-run radar state、expected/fast-exit baseline、外部裁决与 dynamic polling 的 exact owner/恢复边界。
- Cloud exact 文件写集、constructor/bean/caller DAG、与 A/B/J/K 零交叉证明；首选设计批准后的小波次，不能先写占位 wrapper。
- pause/resume/stale/stop、错窗/乱序/断线、tenant、容量、内存/图像、restart、运维与 dormant 风险矩阵。
- P0/P1/P2、自审，以及批准后的 Cloud `mvn -q clean package` 门。自审不算批准。

### Worker 规则

- 只向本文件 append Design #1；父级批准前不改任何 Java/Maven/resources/tests。
- 不回滚、不覆盖、不提交，不运行 Maven/测试，不启动 application/server/host/Task/poller/UI/capture/OCR/input。
- 完成后停止并等待父级结论；BLOCKED 由同一 L 返修，不自行 review 或派 agent。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker L - Design #1 - 2026-07-12

### 1. 结论先行

本切片可以按“每个 `taskRun` 一个 Cloud `BattleRadarService` 实例 + 同一 run 的
`BattleRuntimeState` + H 的 `GameContext.State` 投影 + retained typed capture capability”迁移，保留
DHXY HEAD `0114604e` 的 14 个 public 方法签名和全部业务判定，不需要把 tracker、窗口 holder、HWND、
临时文件或本地 OCR 搬入 Cloud。

但是，**当前代码不能直接开始 L 的 Java 实施**。静态核对得到 3 个独立 P1 前置门：

1. `RemoteGameCommandBroker` 每 owner 只永久保留 1000 个 request，
   `CloudTaskRunActionLedger` 永久保留 10000 个 action；DHXY `RemoteOperationLedger` 同样只增不删。
   雷达是 1s/4s/10s 的长期观察流，若每帧使用正确的新 identity，会在有限时间内耗尽；若复用旧
   identity，则只会得到旧截图。必须先有“最终观察已消费后的确定性退休/压缩”协议，不能用 TTL、
   LRU 或换 ID 掩盖。
2. A 的 PAUSED observer 尚未形成最终 typed capability；H 的 `CloudGameContextStateOwner` 当前只允许
   confirmed ACTIVE 投影，而 HEAD 的 paused observer 会更新雷达状态、`GameContext.ActionState` 和
   paused-observed exit 标记。必须先提供同一 `taskRun` 的受限 PAUSED 状态投影，不能另建第二份状态。
3. K 的 current context slot 尚未批准；当前 `CloudTaskServiceExecutionContext` 会随 revision 新建 retained
   action state。雷达跨 pause/resume 必须复用同一 run 的 action ledger、状态和 service instance，同时
   每次机械调用使用最新 confirmed ACTIVE context。

因此推荐依赖顺序为 `R0 观察账本退休协议 -> K Repair -> H/A PAUSED 投影衔接 -> B template 最终批准
-> L1 配置/识别叶子 -> L2 BattleRadarService`。L 不抢改这些前置波次的文件。

### 2. 基线与证据

- DHXY 业务权威：分支 `thin-client-design`，HEAD
  `0114604e1ff5f15491d2910959c45252e893d04f`；HEAD 的
  `src/main/java/com/bot/dhxy/service/BattleRadarService.java` 工作区 scoped clean。
- Cloud 当前分支 `navigation-migration`，HEAD
  `3b988caa010254973e03342272e6d1d6a9685b01`；目标
  `src/main/java/com/bot/dhxy/service/BattleRadarService.java` 尚不存在。
- 已核对 `docs/业务逻辑.md`：expected fast-exit 只可作提前结束优化，不能成为最终真值；15s/1s/4s、
  20x20、0.35 均冻结；不得新增 full-radar 确认、retry、TTL、park/yield 或 fallback。
- 已核对 5 张 Cloud packaged battle template 与 DHXY 源资源逐字节一致：
  `flag_battle.png`、`zhaohuan.png`、`chehui.png`、`nu.png`、`yuan.png`。
- `CoordinateHelper.getScaledRect()` 在该 HEAD 实际只做 `windowBase + offset`，不缩放宽高；因此 Cloud
  使用 `WINDOW_CLIENT_PX` 的同值 ROI 即与源矩形等价，不需要额外 GEOMETRY fact。

### 3. HEAD 方法、调用方与兼容矩阵

#### 3.1 public API（14/14 保持名字、参数、返回类型和异常传播）

| 方法 | HEAD 行 | AutoCombat | 其他 caller | Cloud 等价职责 |
|---|---:|---|---|---|
| `checkAndSyncCombatState()` | 77 | 正常 tick、expected fallback、paused observer | `NpcClickService`、`TaskHotStartService`、`XiuluoTaskV2` | 按固定 1-4 stage 更新 state/`GameContext` 并返回 remembered/transition 结果 |
| `checkFastExpectedCombatExitByAvatarDiff(String)` | 154 | expected wait | 无 | 15s 后按 1s cadence 比较 20x20 baseline，0.35 差异触发 exit |
| `armExpectedCombatExitWait(String)` | 216 | expected wait 前 | 无 | 原样保留同毫秒、unconsumed-enter、paused-observed 三条时序规则 |
| `refreshFastExpectedCombatExitAvatarBaseline(String)` | 242 | trusted IN_COMBAT correction 后 | 无 | 清旧 baseline；只有当前 IN_COMBAT 且 capture 成功才替换 |
| `nextFastExpectedCombatExitProbeDelayMs()` | 271 | watcher delay | 无 | 返回原有 signed ms；非 IN_COMBAT 为 `-1` |
| `shouldRunFullRadarForFastExpectedExitFallback()` | 291 | expected wait | 无 | 原 4s gate，调用即在到期时推进 timestamp |
| `applyExternalCombatStateVerdict(boolean,String)` | 350 | member follow leader | 无 | 不截图；复用同一 `updateCombatState` 进入/退出信号机 |
| `consumeCombatEnterSignal()` | 416 | normal combat enter | 无 | one-shot clear |
| `consumeCombatExitSignal()` | 430 | normal wait | 无 | one-shot clear 全部 exit 元数据 |
| `consumeCombatExitSignalForExpectedWait(String)` | 445 | expected wait | 无 | 原 timestamp/current-cycle 过滤后消费 |
| `discardStaleCombatExitSignalIfInCombat(String)` | 469 | tick 前校正 | 无 | IN_COMBAT 时清 stale exit |
| `discardCombatEnterSignalIfNotInCombat(String)` | 493 | watcher/return-home 校正 | 无 | 非 IN_COMBAT 时清 stale enter |
| `markCombatExitObservedDuringPause(String)` | 517 | PAUSED observer | 无 | 仅标记当前 pending exit 的 battle count；不自行截图 |
| `getDynamicPollingIntervalMs()` | 536 | watcher cadence | 无 | IN_COMBAT=4000，NAVIGATING/INTERACTING=2000，其余=10000 |

AutoCombat 对 14 个 public 方法全部有实际依赖；所以不能只迁“截图判断”子集。其余 caller 仅调用
`checkAndSyncCombatState()`：

- `NpcClickService.isCombatVisibleAfterDirectClick()`：最多 4 次探测，caller 自有 350ms sleep；雷达不得
  吸收或改变该重试。
- `TaskHotStartService.snapshot()`：雷达结果还要与 `GameContext.IN_COMBAT` 同时成立。
- `XiuluoTaskV2.isInCombatForTaskHotStart()`：同样双条件；`docs/业务逻辑.md` 的修罗基线保持不变。
- `NavigationService` 仅注入但 HEAD 无调用，不为“清理依赖”改它。

#### 3.2 private workflow

`markCombatSignalSeen`、`updateCombatState`、`onEnterCombat`、`onExitCombat`、3 个 current-cycle predicate、
`clearCombatExitPending` 保持一个类内的直接决策，不再套 wrapper。原 `state()` 的窗口 map 被替换为构造时
绑定的单一 per-run state；这是 ownership 搬迁，不改变任何分支。

### 4. HEAD 业务判定矩阵

#### 4.1 full radar stage 顺序（不可合并为一张大图）

| 顺序 | ROI（`WINDOW_CLIENT_PX`） | 模板/阈值 | HEAD 分支 | Cloud 结果映射 |
|---:|---|---|---|---|
| 1 | `(974,630,51,20)` | `flag_battle`, 0.85 | 命中立即 reset misses、IN_COMBAT、return true；capture miss/fail 均继续 | OBSERVED 后内存匹配；明确 capture unavailable 视同源 `findImageInRegion` 返回 null |
| 2 | `(927,302,100,225)` | `zhaohuan OR chehui`, 0.8 | capture fail 且 remembered IN_COMBAT 立即 keep true；否则匹配，命中立即 return true | 同一原子 capture 供两个模板依次 OR；不得为第二模板再截图 |
| 3 | `(456,62,123,39)` | `nu AND yuan`, 0.8 | capture fail 且 remembered IN_COMBAT 立即 keep true；否则匹配，双命中立即 return true | 同一原子 capture 供两个模板依次 AND；不得拆帧 |
| 4 | `(46,59,178,35)` | canonical minimap coordinate recognition | 仅 remembered IN_COMBAT 且连续 miss 达 2 才执行；不可读/异常继续 IN_COMBAT | 一次 strip capture 后调用 Cloud canonical `MiniMapRecognizer` 的 READ_COORDINATE 等价 seam；不建第二 OCR/template loader |

固定退出 miss 为 2。Stage 2/3 capture 失败时，源代码在非 IN_COMBAT 会继续读取该 window-scoped 文件，
理论上可能命中旧文件；这不是可迁移的业务权威，而是本地临时文件残留。L 设计不把 stale Path/图片复制为
第二权威，非 IN_COMBAT 的明确 capture failure 映射为本 stage miss 并继续。**这是唯一已识别的边缘行为差异候选，
实施前需父级明确判为“去除非权威残留、仍属基线等价”或单独记录批准；L 不自行放行。**

#### 4.2 expected fast-exit

- 仅 `GameContext.ActionState.IN_COMBAT` 执行；否则返回 false。
- baseline 缺失时捕获 `(teamHoverX-10, teamHoverY-10, 20, 20)`，保存图并把
  `lastFastExpectedExitProbeAtMs=now`；捕获失败不推进。
- `combatStartedAtMs` 缺失时设为当前 epoch ms；15,000ms 前不比；两次 fast probe 至少 1,000ms。
- 当前图捕获前，HEAD 会先推进 `lastFastExpectedExitProbeAtMs`；即 capture failure 也会推迟下次 1s，必须保留。
- `ImageFinder.isMatch(baseline,current,0.35)` 为 false 才 `updateCombatState(false)`。
- full-radar fallback 独立 4,000ms gate；fast probe 不替代最终雷达真值。
- hover 配置 `<=0` 时记录原日志并返回 null，不 clamp、不 fallback；baseline 为 644/91。

#### 4.3 transition、signal 与 timer

- 全部业务 timestamp 继续使用 `System.currentTimeMillis()` 和 epoch ms。不能换 `nanoTime`，因为
  `combatExitPendingAtMs` 与 `expectedCombatExitWaitArmedAtMs` 存在“同毫秒视为不可可靠排序”的业务规则。
- enter：`battleCount++`，记录 start，清 fast timestamps/baseline，丢弃旧 exit，置 enter pending。
- exit：FREE；保存“未消费 enter 后 exit”元数据；清 enter；置 exit pending + epoch + battleCount；清 fast state。
- `applyExternalCombatStateVerdict` 与图像雷达共用以上唯一 transition owner，不产生第二状态机。
- 日志保留 `[battle-radar]`、`battle radar detected combat enter` 及现有 source/battleCount/miss/
  timestamp 字段；新增仅可为 transport identity/outcome 的结构化诊断，不改变日志代表的业务含义。

### 5. Cloud API 与 explicit-context 适配

#### 5.1 推荐对象边界

`BattleRadarService` 是 **per-taskRun、跨 revision 保留** 的对象，不是 singleton，不持有 ambient current，也不持有
window id map。构造依赖最终为：

1. K 批准后的 `CloudTaskRunCurrentContextSlot`：每次机械调用/状态投影前读取 exact current confirmed ACTIVE
   `TaskExecutionContext`；旧 revision 调用 fail closed。
2. 同一 authority assembly 提供的 non-mintable `BattleRadarCapturePort`：只接受 typed slot + occurrence + ROI +
   timeout，内部才可用 `CloudTaskServicePort`；Service 无 raw request/poll/outcome、无任意 key 字符串。
3. H 的同一 per-run `GameContext.State` activation handle/projection capability。
4. B 最终批准的 `CloudTemplateAssets`，通过 canonical `TemplateId` 加载 5 张模板。
5. Cloud canonical `CloudMiniMapCoordinateReadability`，只解释已经 capture 的 strip image。
6. 最小 `CloudBattleRadarProperties`（仅 signed `teamHoverX/Y`）。

普通 ACTIVE caller 保留原 14 个方法签名，不给每个方法机械增加 context 参数；“explicit context”由构造时绑定
的 non-mintable per-run capability 和每次调用 slot revalidation 保证，而不是 ThreadLocal。A 的 PAUSED observer 使用独立
`PausedBattleRadarObserver` typed capability，只暴露 `checkAndSyncCombatState` + pause exit marker 所需组合操作，
不能调用其它 ACTIVE API、不能 input、不能 renewal。

#### 5.2 caller DAG（仅设计，不激活）

```text
CloudTaskRunAuthorityAssembly
  -> K current-context slot + retained per-run action owner
  -> H GameContext.State owner/activation
  -> BattleRadarCapturePort (typed, non-mintable)
  -> BattleRadarServiceFactory
       -> one BattleRadarService per taskRun
            -> AutoCombatService W1
            -> future migrated NpcClick/HotStart/Xiuluo callers

A W0 PausedObservationContext
  -> same run/state owner + PausedBattleRadarObserver
  -> no ACTIVE current slot, no input, no public raw capture
```

没有 host bean/caller 激活前，以上对象都保持 dormant；本 L 波次不启动 Task、线程或 poller。

### 6. retained capture identity 与 outcome

#### 6.1 typed semantic address

地址前缀固定为 `battle-radar`，slot 必须是 enum，不允许 caller 传任意字符串：

- `FULL_AUTO_FLAG`
- `FULL_SELECTION`
- `FULL_TOP_ICONS`
- `FULL_MINIMAP_COORDINATE`
- `FAST_AVATAR_BASELINE`
- `FAST_AVATAR_PROBE`
- `FAST_AVATAR_REFRESH`

每个 slot 在 per-run radar state 内有独立 non-negative occurrence。第一次构造得到稳定
requestId/actionId/captureId 和完整 bytes；UNKNOWN/超时只重交同一对象。只有 exact final outcome 已被业务调用消费并
完成 R0 的 retirement acknowledgement，才推进该 slot occurrence。STOPPED、stale revision、EXECUTED（对 capture 非法）
均不得换 ID。

#### 6.2 capture envelope

- format：PNG；purpose：`CLOUD_SERVICE_INPUT`；coordinate space：`WINDOW_CLIENT_PX`。
- 每个 capture transport timeout 统一 10,000ms，仅作为机械等待上限；不是业务 TTL，不新增 retry。父级若已有
  Cloud-wide capture timeout 常量，实施时复用，不再造第二配置。
- 每次 capture 由 exact scope/taskRun/window/stopEpoch/current runRevision 加 digest；broker enqueue、dispatch 和
  DHXY handler 三道 revision/window 门保持现有协议权威。

#### 6.3 outcome 映射

| outcome | radar 行为 |
|---|---|
| `OBSERVED` + valid PNG + exact observed window | 解码、匹配；消费 final 后按 R0 协议推进 occurrence |
| `UNKNOWN` / transport timeout with unresolved command | 不更新 miss、timestamp、GameContext 或 occurrence；返回 remembered conservative result，并由同一业务调用路径重交同 bytes |
| verified `NOT_EXECUTED` / capture unavailable | 作为该 HEAD capture failure 分支；消费 final 后下一次业务观察才获新 occurrence，不在本方法自动 retry |
| `STOPPED` / stale context / wrong window | 抛 typed stop/stale 边界退出；不能伪装“未战斗” |
| malformed PNG / template missing | fail closed；IN_COMBAT 保持 true，FREE 不制造 enter；记录运维错误，不新增 fallback |

特别说明：UNKNOWN 不能“返回 false 后让 full radar 继续下一 stage”，否则会把多张不同时间的图拼成一次判定。
当前 stage 未解析时，本次 full radar 立即保守返回 remembered state。

### 7. 图像与 template ownership

- `CloudTemplateAssets` 是 packaged template 唯一入口；L 不写 classpath loader、`Path`、枚举或缓存目录。
- 每个 stage 只解码自己的 OBSERVED PNG。selection/top 的两个模板在同一张 image 上按原 OR/AND 顺序匹配。
- `ImageFinder` Cloud 文件与 DHXY HEAD blob 一致，复用其内存 `find/isMatch`，不改 threshold/math。
- transient capture 与每次 `loadTemplate` 返回图在判定后 `flush()`；fast avatar baseline 由 per-run radar state 唯一持有，
  替换/exit/terminal 时仅在不再可被业务读取后 flush。该操作只释放原对象 native pixels，不改变业务状态/时序。
- process crash 不尝试恢复 baseline 图。若 coordinator 保留 run 但内存状态丢失，activation 必须 fail closed 进入独立
  rehydration 门，不能暗建新 baseline 并假装同一 run 已恢复。

### 8. 状态、pause/resume 与恢复边界

#### 8.1 `BattleRuntimeState` 唯一 owner

由 per-run `BattleRadarService` 持有，不再 `ConcurrentHashMap<windowId,...>`；字段完整保留：

- `battleCount`
- `combatExitMisses`
- `combatStartedAtMs`
- `lastFastExpectedExitProbeAtMs`
- `lastFastExpectedFullRadarAtMs`
- `fastExpectedExitBaselineImage`
- `expectedCombatExitWaitArmedAtMs`
- enter/exit pending、pending timestamp/battleCount
- unconsumed-enter exit 两字段
- paused-observed exit 两字段
- R0 批准后新增的 7 个 capture occurrence 仅属 transport bookkeeping，不参与业务判定。

同一 run pause/resume 复用同一对象；revision 只更新机械 context。不能在 resume 时 reset battleCount、miss、signal、timer
或 baseline。terminal release 在确定不再有 ACTIVE/PAUSED observer 后释放图和 owner，不新增业务 signal。

#### 8.2 `GameContext.State`

所有 14 个方法及 private transition 都在 H 的同一 state projection/serialization 边界内执行，避免同一 run 的 watcher、
external verdict、paused observer 交叉修改。不得调用 `GameContext.defaultState` 或另建 ThreadLocal authority。

PAUSED 观察必须满足：同 scope/taskRun/window/stopEpoch，状态为 PAUSED，专用 observation identity，不能 renewal/input；
在同一个 state/radar lock 下完成 capture 解释、`updateCombatState` 与 `markCombatExitObservedDuringPause`。resume 后旧
PAUSED context 永久失效，ACTIVE 继续读取原 state。

#### 8.3 restart

HEAD 的 radar state 本来是内存态，本设计不伪造 durable recovery。Cloud 进程重启后：

- 不允许对 coordinator 中的后续 revision 调用 initial activation 重建空 state；
- 未有 checkpoint/rehydration 工件时，该 run 必须 fail closed，并由独立迁移门决定停止/重建；
- 不因重启清 retained remote operation ledger 后重放可能已 OBSERVED 的旧 capture。

### 9. 精确波次、文件写集与零交叉

#### R0：持续观察 final retirement/compaction（独立协议前置，非 L）

需由父级另派 remote/wire owner 设计 Cloud broker/action ledger 与 DHXY `RemoteOperationLedger` 的双侧协议。要求：

- pending/UNKNOWN 永久保留 exact bytes 与 late resolution；
- 只有 final outcome 被 exact consumer acknowledgement 后才能退休；
- 保留足够 tombstone 防止迟到 duplicate 被当新命令执行；
- bounded high-watermark，不能 TTL/LRU/静默 eviction/自动 retry。

本 Design 不虚构具体文件清单；必须由该 owner 在读取两侧 wire 后给出跨仓 exact write set。

#### K/H/A/B 前置（现有 owner）

- K：同一 run 跨 revision 复用 retained action state/current service instance。
- H+A：同一 state 的 ACTIVE/PAUSED 受限投影和 pause observer typed seam。
- B：`CloudTemplateAssets`/`PackagedTemplateAssets` 最新实现获父级批准。
- J 与 turn coordination 完全无依赖/无文件交叉。

#### L1：可独立批准的小叶子（预计 3 new）

1. `dhxy-cloud-brain/src/main/java/com/bot/dhxy/config/CloudBattleRadarProperties.java`
2. `dhxy-cloud-brain/src/main/java/com/bot/dhxy/config/CloudBattleRadarPropertiesAuthority.java`
3. `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudMiniMapCoordinateReadability.java`

properties 仅暴露 signed `teamHoverX/Y`；no-override baseline 为 644/91，scope/revision CAS 模式复用 I 的
`CloudAutoBattlePropertiesAuthority`，不复制整份 `TeamTaskProperties`。minimap seam 在 canonical package 内直接调用
既有 `MiniMapRecognizer` 与 READ_COORDINATE fallback/plausibility，不加载资源、不截图。

#### L2：主体（预计 1 new）

4. `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/BattleRadarService.java`

typed slot/retirement handle 应由 R0/K authority wave 定义，L2 只消费，不在 service package 再造 identity owner。L2 不修改
assembly、host、AutoCombat caller、A/B/J/K/H 文件。constructor/factory 装配待所有前置批准后由单独 activation wave 修改，
不在 L2 偷开 host。

上述写集与：A 的 PAUSED `remote/**`/DHXY wire、B artifact/template 7 文件、J turn 文件、K current slot/assembly/
service-context、H state owner均零交叉。

### 10. 风险矩阵

| 风险 | 级别 | 证据/影响 | 设计门 |
|---|---|---|---|
| 长期 capture 耗尽 retained request/action | P1 | broker owner request cap=1000、ledger=10000 且 never evicted；10s 三 stage 最坏约 55 分钟触顶 | R0 final-consumed retirement + tombstone，双侧落地后才实施 |
| PAUSED observer 产生第二份 radar/GameContext state | P1 | H 仅 confirmed ACTIVE；HEAD paused observer 会写 state | A/H 提供同-run restricted PAUSED projection；无此 seam 禁止接 W0 |
| resume 后换 retained ledger/service state | P1 | K 当前材料指出 service context 随 revision 新建 retained state | K Repair 必须先 APPROVED，slot 原子替换 context 而不替换 state |
| Stage 2/3 非战斗 capture failure 的 stale temp file | P2 | HEAD 可能继续读旧 window-scoped PNG；Cloud 禁止本地 Path/残留权威 | 父级明确“残留非权威”口径或单独批准；不得静默宣称完全相同 |
| template owner 尚处返修 | P2 | B 最新材料未获父级最终 APPROVED | L2 只依赖批准后的接口，不复制 loader |
| UNKNOWN 后拼接跨时刻 stage | P1 | 会产生源中不存在的混合帧判定 | unresolved stage 立即保守返回，同 bytes 重投，不推进 occurrence/miss |
| wrong tenant/window/revision | P1 | 可读错窗口并产生错误 enter/exit | exact context + broker enqueue/dispatch + DHXY handler 三道门；observed window 再核对 |
| baseline image 泄漏/跨 run | P2 | 长任务会反复 refresh/enter/exit | per-run owner；替换/terminal 后 flush；不共享 cache |
| restart 后空状态伪恢复 | P1 | battleCount/signal/timer/baseline 丢失会错消费 | 无 durable rehydration 时 fail closed，不调用 initial activation |
| dormant 代码被提前装入 host | P1 | 当前 caller/Task 尚未整批迁移 | L1/L2 不改 host/assembly/caller；独立 activation gate 后再接 |
| timer 改成 monotonic 导致同毫秒规则变化 | P2 | expected arm/exit 直接比较 epoch timestamp | 原样保留 `currentTimeMillis` 与 ms 单位 |
| 新增 capture/确认改变五倍/修罗 | P1 | 违反 `docs/业务逻辑.md` | stage 数、顺序、caller retry、fast/full cadence均冻结 |

### 11. 自审与父级验收点

#### 11.1 自审结果

- P0：0。
- P1：3 个开放实施前置（R0 容量、A/H PAUSED state、K retained state/current slot）。这些不是 L 可在
  `BattleRadarService` 内规避的问题；在关闭前不得批准 L2 Java。
- P2：2 个待父级口径（stale temp file 边缘行为、B template owner 最终批准）。
- 未发现需要修改 DHXY 业务基线的正常路径；无已批准业务差异。
- 本轮仅设计，未改 Java/Maven/resources/tests，未运行 Maven/测试，未启动任何运行面。

#### 11.2 Design APPROVED 的最低条件

1. 父级认可方法/caller/stage/state/timer 矩阵完整；
2. 父级将 R0、K、A/H、B 作为显式依赖而非“未来会有”；
3. 对 Stage 2/3 stale temp file 口径写出明确结论；
4. L1 exact write set 可先独立批准，L2 必须等待 3 个 P1 关闭；
5. 不以自审代替父级 review。

#### 11.3 批准后的验证门

- L1/L2 任一 Java 写入后，由父级运行 Cloud `mvn -q clean package`，不可 skip；
- 本 L 设计不触碰 DHXY Java；若 R0 独立波次触碰 DHXY Java，其 owner 另跑
  `mvn -q -DskipTests compile`；
- no-local-test 继续有效，不新增/恢复 tests；
- build 通过仍只代表静态门，host 保持 dormant，fresh runtime/生产切换属于后续独立验收。

**Worker L 状态：Design #1 已提交，等待父级 DESIGN REVIEW；未获批准前停止，不实施。**

## Parent Design Review #1 - L1 DESIGN APPROVED / L2 BLOCKED - 2026-07-12

### 结论

- **L1：DESIGN APPROVED，P0=0/P1=0/P2=0。** 放行下列三个 Cloud 新文件，且只放行这三个文件：
  1. `src/main/java/com/bot/dhxy/config/CloudBattleRadarProperties.java`
  2. `src/main/java/com/bot/dhxy/config/CloudBattleRadarPropertiesAuthority.java`
  3. `src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudMiniMapCoordinateReadability.java`
- **L2：BLOCKED，P0=0/P1=3/P2=0。** `BattleRadarService.java`、capture port、factory、assembly、host、caller
  和双仓 wire 继续冻结。

方法/caller/stage/timer/state inventory 与 DHXY HEAD `0114604e` 对齐：14 个 public API、Stage 1-4 顺序、
0.85/0.8、连续 miss=2、15s/1s/4s、20x20/0.35、epoch-ms 同毫秒规则均可作为后续实现基线。L 对
本地 HWND/Path/runner 权威的排除、per-taskRun 跨 revision 状态、同帧 OR/AND、dormant host 与 restart
fail-closed 的方向成立。

### L2 阻塞项

1. **P1：未解析 capture 被折叠成业务 boolean。** Design 第 241、246-247 行规定 `UNKNOWN` 返回
   remembered conservative result。remembered=`FREE` 时这就是 `false`；`NpcClickService`、AutoCombat 等 caller
   会把它当作“本次未检测到战斗”继续自己的点击/状态流程，而不是在同一业务调用边界重交原 bytes。这样既可能产生
   副作用，也无法兑现第 241 行声称的同 bytes 重投。返修必须把 unresolved/timeout 保持为 typed unresolved unwind：
   不返回任何可被 caller 当作战斗真值的 boolean，不推进 miss/timestamp/GameContext/occurrence，也不在 Service 内新增
   retry；重入后仍由 retained handle 提交完全相同 bytes。
2. **P1：持续观察退休协议尚未形成可实现合同。** Design 第 299-308、342 行正确识别 broker/request/action/local ledger
   的有限容量，但只有“ack+tombstone”原则，没有 exact wire、压缩 frontier、迟到 duplicate 判定、Cloud/DHXY 原子提交
   顺序和 crash 恢复合同。固定容量下，有限 tombstone 本身不足以证明旧 request 不会被再次当新命令。R0 必须先由独立
   remote/wire owner 给出跨仓设计与写集，并至少定义单调 occurrence/frontier、final-consumer ack、frontier 以下重投
   的确定性拒绝、UNKNOWN 保留和重启恢复；获父级批准并落地后才能放行 L2。
3. **P1：Stage 2/3 capture failure 存在未批准业务差异。** DHXY HEAD
   `BattleRadarService.java:89-96,106-113` 在 FREE 状态 capture 失败后仍读取 window-scoped 文件；Design 第 143-146、
   242、345 行改成明确 miss。旧文件不是可搬到 Cloud 的权威，这个判断成立，但“删除残留命中腿”仍会改变 enter 判定，
   不能写成“无业务差异”。L2 返修必须给出不引入 Path/第二权威的精确迁移口径，并把该差异单独取得批准后再实施；
   在此之前不得自行把 `NOT_EXECUTED` 映射为 stage miss。

### L1 实施绑定

- properties 只保留 signed `teamHoverX/Y`，no-override 基线严格取 HEAD `application.yml` 的 `644/91`；复用 I 的
  tenant/user-scoped immutable CAS 模式，不引入整份 `TeamTaskProperties`、Spring bean 或 caller。
- minimap seam 只接受已解码的 coordinate-strip `BufferedImage`，在 canonical package 内复用现有
  `MiniMapRecognizer.recognize(...)` + READ_COORDINATE OCR fallback；不得截图、加载第二套模板、写 Path、保存 debug 文件
  或开放任意 operation。
- 三文件保持无 producer/host/caller，零 tests，零 Maven/model/resource 修改。实施完成后追加 `Implementation #1`，列出
  精确 diff 与自审；父级再做源码审查并运行 fresh Cloud `mvn -q clean package`（不 skip）。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker L - Implementation #1 - 2026-07-12

### 实施范围

严格按 `Parent Design Review #1` 的 L1 批准边界，仅在 `dhxy-cloud-brain` 新增以下三个文件：

1. `src/main/java/com/bot/dhxy/config/CloudBattleRadarProperties.java`
   - 19 行，534 bytes；SHA-256
     `e86d2572d2fe105750491c0ce86d3884dec44a5f33fdd12e47ebb4322fd3d986`。
   - 只读暴露 signed `teamHoverX/teamHoverY`；不 clamp、不 fallback、不复制其余
     `TeamTaskProperties`。
2. `src/main/java/com/bot/dhxy/config/CloudBattleRadarPropertiesAuthority.java`
   - 146 行，4949 bytes；SHA-256
     `f0d74c5db1d501e9c7502bf67d94d5a02ff35b646624c2b1efb5149f9c81b6fb`。
   - package-private tenant/user-scoped authority；完整复用 I 的 immutable `Snapshot` +
     `AtomicReference.compareAndSet` + expected/next revision 约束。
   - no-override 基线严格为 HEAD `application.yml` 的 `644/91`；override/replace 对 X/Y
     不做正数校验，保留 signed 值。
3. `src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudMiniMapCoordinateReadability.java`
   - 33 行，1372 bytes；SHA-256
     `cf782cd0c0970e6cf2bf14fd997375719b8a0bcfb3ae39633c496a9f9d9d19ac`。
   - public pure in-memory seam，仅接收 caller-owned `BufferedImage`。
   - 先调用 canonical `MiniMapRecognizer.recognize(...)`；坐标 miss 后调用既有
     `recognizeByOcr(...)`，按 READ_COORDINATE 的 `requiresMapName=false` 语义接受 coordinate-only
     OCR，再复用 `MiniMapPointResolver.ocrFallbackCoordinatePlausible(...)`。
   - 不截图、不持有/flush caller image、不读写 Path、不加载第二套模板、不保存 debug、不暴露任意
     operation。

### 边界核对

- 三个目标在实施前均不存在，本轮均为 new file，无覆盖。
- 未新增 Spring bean/configuration/producer/caller，未修改 Maven/resources/tests。
- 未触碰 L2 `BattleRadarService`、capture port/factory、assembly/host/caller。
- 未触碰 A/B/K/J/H 或 DHXY wire；没有回滚、提交或 Git mutation。
- `CloudMiniMapCoordinateReadability` 不接触 tracker、WindowRuntime/HWND、Task、线程、输入或运行面。

### 自审

- P0=0，P1=0，P2=0（仅针对获批 L1 写集）。
- properties 的两项业务值保持 signed，baseline=644/91；scope/revision/CAS 与 I authority 同构。
- minimap seam 没有第二 loader/OCR 路径；template recognition、coordinate-only OCR fallback 和
  plausibility 均直接委托 canonical owner。
- L2 的三个 P1 仍保持 BLOCKED，本次未尝试规避或提前实现。
- **按父级实施绑定，本 Worker 未运行 Maven、compile、package 或 tests；fresh Cloud
  `mvn -q clean package` 由父级源码审查阶段执行。**

**Worker L 状态：Implementation #1 已提交，停止并等待父级源码审查。**

## Parent L1 Implementation Review #1 - APPROVED - 2026-07-12

父级复核三个新文件，结论：**APPROVED，P0/P1/P2=0。** properties 保留 signed `int`、baseline `644/91`、
tenant/user scope 与 immutable revision CAS；minimap seam 只消费 caller-owned `BufferedImage`，依次复用 canonical
`MiniMapRecognizer.recognize`、coordinate-only OCR fallback 与既有 plausibility guard，无 Path/截图/第二 loader/debug/
任意 operation。全仓仅三文件自身命中，无 bean/producer/caller，L2 三项 P1 未被绕过。

父级 fresh Cloud `mvn -q clean package` exit 0，4 suites/21 tests，failures/errors/skipped 均 0；JAR SHA-256
`3C0261F0D1DD56A056AD28F89D01B9AFB349B771B7A4FFD8CA39F76D68729606`。L1 收口但不增加同路径计数，仍为
`185/407`；L2 继续 BLOCKED，host/Task/caller dormant。

**无已批准业务差异；按基线等价迁移。**
