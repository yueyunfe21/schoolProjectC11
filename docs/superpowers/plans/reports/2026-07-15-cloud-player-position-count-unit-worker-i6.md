# W-COUNT-PLAYER-POSITION-WHOLE-1

## CLAIMED - 2026-07-15T01:34:00-04:00

- worker: `Internal Count Worker I6`
- role: implementation-only；不是 reviewer
- countUnit: `PlayerStateService::syncMyPosition`
- countDelta: `+1`（仅申报；当前不得更新 ledger）
- business baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- allowed Java write set: Cloud `PlayerStateService.java`；position-specific adapter 仅在既有 producer 可复用时允许
- report write set: 本文件
- frozen: `NavigationService` caller、DHXY Java、shared remote、incense/first-aid、其它 Service、host/config
- restrictions: 不运行 Maven/test/runtime/application/server/host/Task/poller/UI/capture/input；不执行 Git mutation

## Required Count Closure

本 countUnit 必须一次闭合：

1. Cloud `NavigationService:903` 在 `fromMap == null` 时调用 `playerStateService.syncMyPosition()`；
   `PlayerStateService.syncAll()` 也按 `syncMyIdentity() -> syncMyPosition()` 顺序调用。
2. `syncMyPosition` 通过 current exact `TaskExecutionContext` 请求 typed current-location observation。
3. DHXY 在该 exact registration/native binding 下运行既有 `LocationVisionService.scanCurrentLocation()`，不得在 Cloud
   直接 capture/OCR，也不得搜索其它窗口。
4. observed `LocationInfo(mapName,x,y)` 返回 Cloud 后，严格保持
   `setCurrentMapName -> setX -> setY` 更新顺序、日志和返回值。
5. local observation miss 保持 null/no-mutation；stop/interruption 必须按正常 checkpoint 语义退出。

## Baseline Method Map

`696a12b0` 的 `PlayerStateService.syncMyPosition()` 行为为：

- 初始化 latency 字段和 `LocationInfo info=null`，记录“请求雷达扫描当前位置”。
- 调用一次 `locationRadar.scanCurrentLocation()`；不在 Service 内自动 retry。
- `info != null`：取得 `context.getMe()`，依次写 mapName、x、y，更新 latency 字段并记录全局记忆日志。
- `info == null`：仅记录“雷达未能看清”，player state 不变。
- `finally` 始终记录 `player.position.sync` latency；最终原样返回 `info`。

当前 active Cloud `PlayerStateService.java:169-198` 仍逐项保留上述方法体和更新顺序，但其 observation collaborator
尚未迁成可达的 typed DHXY boundary。

## Exact Blocker Evidence

结论：`IMPLEMENTATION BLOCKED - REQUIRED TYPED PRODUCER ABSENT`。

1. active Cloud `PlayerStateService.java:27/70/178` 仍 import/inject/call
   `com.bot.dhxy.vision.LocationVisionService`；active Cloud source tree 中不存在
   `LocationVisionService.java`。因此这不是可运行的 Cloud->DHXY typed port，而是 whole-Service promotion 后留下的本地
   phantom dependency。
2. Cloud shared `WindowFactKind` 的 closed allowlist 只有 BINDING/GEOMETRY/FOCUS/STOP、UI/status、common-box、
   team-return、task-tracker 与 battle-radar 类型；没有 `CURRENT_LOCATION`/`LOCATION_INFO`。
3. Cloud sealed `WindowFact` 没有承载 `mapName/x/y` 的 location fact，`RemoteGameClientPort` 也没有
   position/current-location 专属方法。
4. DHXY `RemoteWindowFactKind` 与 `LocalRemoteGameCommandHandler.executeWindowFact` 同样没有 current-location
   case；现有本地 `LocationVisionService.scanCurrentLocation()` 虽可产生 `LocationInfo`，但没有被 exact registration
   remote handler 暴露。
5. 现有 `MiniMapLocationCloudDecision*` 是 DHXY 上传图像到旧 cloud-decision 的反向路径，不是 Cloud task 通过
   exact `TaskExecutionContext` 向 DHXY 读取当前位置的 typed fact/port；它不能闭合本 countUnit，也不能替代缺失
   producer。
6. 名称中含 `freshCurrentLocation*` 的现有字段只属于 `NAVIGATE_IN_CURRENT_MAP` macro 的请求元数据，不携带
   observed map/x/y，不能被误当 current-location fact。

## Why Allowed Files Cannot Repair It

仅修改 Cloud `PlayerStateService.java` 或新增 position-specific Cloud adapter，最多只能消费一个已经存在的 typed
producer；当前 shared kind/fact/codec 与 DHXY handler producer 均不存在。因为 shared remote 与 DHXY Java 在本任务
明确冻结，本 Worker不能：

- 扩展既有 generic `WindowFact` closed allowlist；
- 新增 mapName/x/y payload 与双侧 codec mapping；
- 在 DHXY exact binding handler 中调用既有 local location observation；
- 用 stub、null adapter、global tracker 或旧反向 cloud-decision 假装真链闭合。

## Precise Resume Condition

父级需先另派一个完整前置实现单，在**现有 generic WindowFact 协议内**增加唯一 closed current-location fact，而不是
建立第二协议：

- Cloud/DHXY closed kind 与 mapName/x/y fact/payload parity；
- DHXY handler 在命令已校验的 exact `BindingAccess`/window context 中调用既有
  `LocationVisionService.scanCurrentLocation()`；
- location hit -> typed `OBSERVED`，local miss/unavailable -> closed absent terminal，stop -> closed stop terminal；
- Cloud `RemoteGameClientPort`/mapper 可返回该 fact。

该 producer 经父级批准后，本 countUnit 才能恢复：在允许的 `PlayerStateService.java` + position-specific adapter 内把
`locationRadar.scanCurrentLocation()` 原调用点替换为 typed fact，并保持 map->x->y、null/no-mutation、latency 与 stop
语义不变。

## Delivery

- status: `BLOCKED - MISSING TYPED CURRENT-LOCATION PRODUCER`
- countUnit: `PlayerStateService::syncMyPosition`
- countDelta: `+1` 尚未申报完成、尚未更新 ledger
- Java changed: `0`
- report changed: 本文件
- Maven/test/runtime/application/server/host: 按禁令未运行
- Git mutation: 未执行
- intentional business differences: `无已批准业务差异；未以 stub/fallback 改写 696a12b0`

## Parent Blocker Review #1 / Replacement Count Task - 2026-07-15T01:36:00-04:00

父级独立核对 Cloud/DHXY `WindowFactKind`、handler 和 active `PlayerStateService`：current-location kind、
map/x/y fact、exact-binding producer 均不存在，旧 MiniMapLocation decision 是反向上传链，不能替代。结论：
**BLOCKED，P0=0/P1=1/P2=0**；`syncMyPosition` 不计数，I6 未造 stub 正确。

替换任务 `W-COUNT-PLAYER-FIRST-AID-CACHED-EXECUTE-1`；
`countUnit=PlayerStateService::performCachedFirstAidPlanNow`；`countDelta=+1`。一次闭合真实
`AutoCombatService:399/464/573/577 callers -> Cloud cached-plan/context state -> existing
PLAYER_STATE_FIRST_AID typed macro -> DHXY exact-window bars/input mechanics/single queue -> closed boolean/cache state`；
保留 696 plan identity、map/x/y captured base、PROBE/HEAL 顺序、four-bar mapping、STOPPED/UNKNOWN、成功消费与失败保留。
唯一 Java 写集 Cloud `PlayerStateService.java` + playerstate 专属 adapter（仅必要时）+ 本报告；caller、DHXY、
generic shared 12、incense/status、其它 Service 冻结。现有链完整可 NO_CODE_CHANGE 交证据；需冻结文件则精确 BLOCKED。
父级源码审查 + fresh build 通过同轮才 `+1`。

# W-COUNT-PLAYER-FIRST-AID-CACHED-EXECUTE-1

## CLAIMED - 2026-07-15T01:39:45.3913510-04:00

- worker: `Internal implementation Worker I6`
- role: implementation-only；不是 reviewer
- countUnit: `PlayerStateService::performCachedFirstAidPlanNow`
- countDelta: `+1`（仅申报；父级源码审查与统一 fresh build 通过后才可实际计数）
- business baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- allowed Java write set: Cloud `PlayerStateService.java`；playerstate 专属 adapter 仅在既有真链有精确缺口时允许
- report write set: 本文件
- frozen: 四个 `AutoCombatService` caller、DHXY Java、generic shared 12、incense/status、其它 Service、host/config
- restrictions: 不运行 Maven/test/runtime/application/server/host/Task/poller/UI/capture/input；不执行 Git mutation

## Existing Real Count Chain

当前源码已经存在一条非 stub 的完整链，且无需新增 adapter：

1. **真实 public callers**：Cloud `AutoCombatService.java:399/:464/:573/:577` 均直接调用
   `playerStateService.performCachedFirstAidPlanNow(context)`；前两处消费 post-combat probe 产生的 plan，后两处在 follower
   task-turn 内先消费现有 plan，返回 false 才按基线重做一次 no-focus probe 后再次调用。
2. **Cloud plan identity / captured base**：`probeFirstAidSupplyNoFocus` 从同一次 typed probe terminal 读取
   `observedBaseX/observedBaseY`，按固定 four-bar 顺序构造 `FirstAidTarget`，并把该 ordered target list、创建时间和同帧 base
   保存为同一个 `FirstAidPlan`。`performCachedFirstAidPlanNow` 读取该 exact plan，并用 `plannedTargets(...)` 原顺序映射
   `name/relX/relY/threshold`，没有复扫 bars 或重算目标。
3. **typed Cloud boundary**：`CloudPlayerStateFirstAidPort.executeCachedPlan(...)` 构造 closed
   `PLAYER_STATE_FIRST_AID / EXECUTE_CACHED_PLAN` command；command 必须携带 plan base 与非空 ordered targets，且禁止夹带
   four-bar toggles。调用使用 current exact `TaskExecutionContext`，前后均执行 `TaskCheckpoint`。
4. **DHXY exact-window mechanics**：现有 DHXY handler 已按 exact registration/runRevision/native binding 校验，
   `EXECUTE_CACHED_PLAN` 在单次 `submitRemoteExclusiveAndWaitDetailed` callback 内执行，不嵌套 input queue。local mechanics
   优先刷新同一 binding 的 base，失败时保留 captured base；依 ordered targets 执行既有 right-click 与 settle delay，并返回
   `COMPLETED` 或 `INTERRUPTED`。
5. **closed terminal**：Cloud port 仅接受 operation 匹配的 typed EXECUTED result；`NOT_EXECUTED` 映射为空；
   `STOPPED/UNKNOWN` 经 `TaskFatalException`/checkpoint 正常退出，不自动 retry。Cloud Service 对 `COMPLETED` 判完成，其他
   non-fatal terminal 只写既有 warning。
6. **four-bar / PROBE / HEAL 保持**：同一 closed macro 仍只有 `PROBE_SUPPLY_NO_FOCUS -> HEAL_ALL /
   EXECUTE_CACHED_PLAN` 三种 operation；probe/heal terminal 的 fixed four-bar mapping 未被本 countUnit 修改。

## Baseline Branch / State Map

`git show 696a12b0:src/main/java/com/bot/dhxy/service/PlayerStateService.java` 对本方法的权威顺序是：

1. 入口 checkpoint；读取 `pendingNoFocusFirstAidPlan` 后**立即置 null**。
2. plan 缺失/targets 空：返回 false；plan 已清。
3. captured base 任一为 `-1`：warning 后返回 false；plan 已清。
4. valid plan：按 ordered targets 执行一次 exclusive local action；callback false/中断只记录 warning。
5. valid plan 无论 callback boolean 是否完成，均 `checksDoneThisRound++` 并返回 true；不会恢复已清 plan。

既有迁移记录也明确了同一合同：

- `2026-07-15-cloud-player-first-aid-count-unit-worker-i4.md`：Cloud 保留“先取并清 plan”，消费后计数并
  `return true`。
- `2026-07-13-cloud-player-state-service-worker-y.md`：valid plan 即使 callback false/中断仍 warning 后 true；
  `NOT_EXECUTED/STOPPED` after claim 也不恢复 plan。

当前 active Cloud `PlayerStateService.java:319-352` 正逐项保持上述行为：`:322-323` 先读并清；`:341-345` 调 typed
macro 并判断 COMPLETED；`:346-352` 未完成仅 warning，随后计次并返回 true。

## Exact Semantic Conflict

结论：`IMPLEMENTATION BLOCKED - ASSIGNMENT CONFLICTS WITH 696a12b0 BASELINE`。

本次任务同时要求“基线 696a12b0”以及“成功消费/失败保留”。后者若按通常语义实现，必须把
`pendingNoFocusFirstAidPlan = null` 从执行前移到 COMPLETED 分支，或在失败时写回 plan，并相应决定计数/返回值。这会产生
696a12b0 不存在的新行为：

- `NOT_EXECUTED` 或 typed `INTERRUPTED` 后，同一 caller/后续 safe window 可以再次执行同一 plan；
- invalid captured base 是否继续保留将改变 caller 的 false -> retry-probe 路径；
- `STOPPED/UNKNOWN` 抛出时若恢复 plan，resume 后也会新增一次执行机会；
- 若失败不计次或返回 false，还会改变 `AutoCombatService:399/:464/:573/:577` 的已有分支与 probe 次数。

这不是 ownership/typed-boundary 替换，而是 cache consume、retry 和 caller-visible boolean 的业务语义变化。任务没有明确写
“批准偏离 696a12b0 并采用 success-only consume”，且使用的是“保留”措辞；依业务基线门，本 Worker不能自行选择该变化。

## Precise Resume Options

父级需确认以下唯一一种口径后才能继续：

1. **按 696a12b0 等价迁移（推荐）**：将任务中的“失败保留”更正为“valid plan claim 后不恢复”；现有真链可按
   `NO_CODE_CHANGE` 交父级源码审查，仍只申报本 countUnit `+1`。
2. **批准 success-only consume 业务差异**：明确哪些失败必须保留（invalid base、`NOT_EXECUTED`、typed
   `INTERRUPTED`、`STOPPED/UNKNOWN`），以及失败时 `checksDoneThisRound` 和 boolean return 的新合同；父级把该差异写入
   对应 CR/任务后，本 Worker才可仅在 `PlayerStateService.java` 内实施，不能把 retry 放进 adapter。

## Delivery

- status: `BLOCKED - BASELINE / ASSIGNMENT SEMANTIC CONFLICT`
- countUnit: `PlayerStateService::performCachedFirstAidPlanNow`
- countDelta: `+1` 未申报完成、未更新 ledger
- Java changed: `0`
- report changed: 本文件
- Maven/test/runtime/application/server/host: 按禁令未运行
- Git mutation: 未执行
- intentional business differences: `无已批准业务差异；未把失败保留/自动重试写入 696a12b0 等价迁移`

## Parent Ruling Acknowledgment - 2026-07-15T01:45:50.8886458-04:00

父级已选择上文 Option 1，并把任务口径更正为严格按 `696a12b0` 等价迁移：valid cached plan 在方法入口
claim 后不恢复；`NOT_EXECUTED`、typed `INTERRUPTED`、invalid base、`STOPPED/UNKNOWN` 均不得新增恢复或自动 retry；
`checksDoneThisRound` 与 caller-visible boolean 保持 baseline。本 Worker 已接收该裁定，原 semantic blocker 解除。

## NO_CODE_CHANGE Implementation #1 - 2026-07-15T01:45:50.8886458-04:00

### 1. Real caller closure

Cloud `AutoCombatService` 四个真实 caller 已直接闭合到 active Cloud `PlayerStateService`：

- `:399`：post-combat no-focus probe 为 `SUPPLY_NEEDED` 时调用
  `performCachedFirstAidPlanNow(context)`；仅方法返回 false 才记录 plan unavailable。
- `:464`：deferred post-combat 路径保持相同调用与 false 分支。
- `:573`：pending follower 获得 task turn 后先消费现有 cached plan；返回 false 才进入既有 retry-probe caller 分支。
- `:577`：该 retry probe 再次得到 `SUPPLY_NEEDED` 时调用同一方法。这个 caller-owned 分支是 696 已有路径，
  本实现没有在 Service/adapter/transport 中新增 retry。

### 2. Cloud claim / clear / ordered plan closure

active Cloud `PlayerStateService.java:319-352` 已与 `696a12b0` 同序：

1. `checkpoint(taskContext)` 后从 current per-window runtime state 读取 exact
   `pendingNoFocusFirstAidPlan`。
2. `:322-323` 在任何后续分支前完成 claim：读取 plan 后立即把 pending slot 置 null；所有 terminal/exception 路径均不
   恢复。
3. absent/empty plan 返回 false；invalid captured base 写 warning 后返回 false；两者均保持已清状态。
4. valid plan 保留创建时的 ordered `FirstAidTarget` identity、`createdAtMs` 与同帧 captured base；
   `plannedTargets(...)` 只按原 list 顺序映射 `name/relX/relY/threshold`，不重排、不复扫 bars、不重算坐标。
5. typed macro 未完成只写 baseline warning；随后无条件 `checksDoneThisRound++` 并返回 true。因而
   `NOT_EXECUTED`/typed `INTERRUPTED` 不会被 caller 当作“未 claim”，也不会恢复 plan。

### 3. Closed typed macro closure

`CloudPlayerStateFirstAidPort.executeCachedPlan(...)` 已构造唯一 closed command：

```text
LocalMacroKind.PLAYER_STATE_FIRST_AID
  + Operation.EXECUTE_CACHED_PLAN
  + planBaseX / planBaseY
  + non-empty ordered PlannedTarget list
```

- command constructor 对 cached operation 强制 captured base + 非空 ordered targets，并拒绝 four-bar toggles。
- port 从 current exact `TaskExecutionContext` 调 `CloudGameClient.executeLocalMacro(...)`，调用前后均使用
  `TaskCheckpoint`，不建立第二 context/global fallback。
- EXECUTED 必须携带 operation 匹配的 typed `PlayerStateFirstAidMacroResult`；`NOT_EXECUTED` 为 empty optional；
  `STOPPED/UNKNOWN` 走 fatal/stop unwind。port 不拥有 cache state，也不做 retry/restore。

### 4. DHXY exact-window single-exclusive mechanics closure

DHXY `LocalRemoteGameCommandHandler.executePlayerStateFirstAidMacro(...)` 已保持：

1. command 先经过 exact registration、runRevision、native binding 与既有 safety/deadline gate。
2. `EXECUTE_CACHED_PLAN` 与 `HEAL_ALL` 均只进入一次
   `inputActionQueue.submitRemoteExclusiveAndWaitDetailed(...)`；callback 内直接调用
   `PlayerStateFirstAidLocalMacroMechanics.executeCachedFirstAidPlanDirect(...)`，没有 queue-in-queue。
3. local mechanics 在该 exact binding 下刷新 base；刷新不可用时使用 plan captured base，不读取其它窗口/global tracker
   状态。它按 command 中 ordered target list 逐项执行既有 right-click/settle 序列，保持 physical input 单队列原子所有权。
4. callback 完整执行返回 typed `CachedPlanStatus.COMPLETED` 或 `INTERRUPTED`；admission/safety/stop 分支返回 closed
   non-EXECUTED/STOPPED terminal，不把本地动作或 retry 迁回 Cloud。

### 5. Closed terminal / baseline state result

- `EXECUTED + COMPLETED`：Cloud `completed=true`；plan 已 claim，计次并返回 true。
- `EXECUTED + INTERRUPTED`：Cloud `completed=false`，只 warning；plan 不恢复，仍计次并返回 true。
- `NOT_EXECUTED`：port 返回 empty，Service 只 warning；plan 不恢复，仍计次并返回 true。
- `STOPPED/UNKNOWN`：checkpoint/fatal 正常 unwind；因 claim 位于调用前，plan 不恢复；没有自动 retry。
- absent/empty/invalid-base：保持 baseline false 分支，且入口 claim 后不恢复；caller 是否执行其原有 probe 分支完全不变。

### 6. Delivery

- implementation status: `NO_CODE_CHANGE IMPLEMENTED - READY FOR PARENT SOURCE REVIEW`
- countUnit: `PlayerStateService::performCachedFirstAidPlanNow`
- countDelta: `+1`（仅申报；仍等待父级源码审查与统一 fresh build 后实际计数）
- Java changed: `0`；现有 active 真链已完整，无需新增 playerstate adapter
- report changed: 本文件
- frozen files: 未修改
- Maven/test/runtime/application/server/host: 按禁令未运行
- Git mutation: 未执行
- intentional business differences: `无已批准业务差异；严格按 696a12b0 valid-plan claim 后不恢复`

## Parent Source Review #1 - 2026-07-15T02:12:00-04:00

- verdict: `BLOCKED`
- severity: `P0=0 / P1=1 / P2=0`
- countUnit: `PlayerStateService::performCachedFirstAidPlanNow`
- countDelta: `+0`（仍为申报待审，不得计入 ledger）

### P1 - refreshed binding 可把 `(-1,-1)` 覆盖到有效 cached plan base

- 精确证据：DHXY `PlayerStateFirstAidLocalMacroMechanics.executeCachedFirstAidPlanDirect(...)`
  当前在 `:206-216` 只检查 `refreshed.isPresent() && refreshed.get().hasGeometry()`，随后无条件用
  `refreshed.get().getX()/getY()` 覆盖 plan 的 stored base。
- `WindowNativeBinding.hasGeometry()` 只证明 width/height 有效，不证明窗口左上角 `x/y != -1`；因此一次只刷新到
  尺寸、但位置仍为 `(-1,-1)` 的 binding 会把原本有效的 cached base 覆盖掉，后续 safe mouse point 和所有
  right-click 均从错误基点计算。
- `696a12b0` 权威基线 `PlayerStateService.performCachedFirstAidPlanDirect(...)` 明确要求
  `refreshed && refreshedBaseX != -1 && refreshedBaseY != -1` 才覆盖 stored base；否则保留 plan base 并记录 fallback。
- 影响：exact-window physical input 可能点击到错误坐标；当前真链不能通过父级源码门。

### 精确返修条件

1. 唯一 Java 写集扩为 DHXY
   `src/main/java/com/bot/dhxy/service/playerstate/PlayerStateFirstAidLocalMacroMechanics.java`；另只允许 append 本报告。
2. 仅把 refreshed binding 接受条件补为同时要求 `x != -1 && y != -1`；无效刷新继续使用 stored
   `plan.baseX()/baseY()`，保留现有 warning/fallback 语义。
3. 不修改 Cloud `PlayerStateService`、port、command/result、handler、caller，不新增 retry/restore/TTL/state。
4. 交付后父级复核本条件，再等待所有 Java writer 稳定后统一运行 DHXY compile 与 Cloud clean package；双门通过前
   ledger 保持 `189/407`。

无已批准业务差异；按 `696a12b0` 基线等价迁移。

## Repair #1 - 2026-07-15T02:11:29.3183835-04:00

- task: `W-COUNT-PLAYER-FIRST-AID-CACHED-EXECUTE-1-R1`
- role: implementation-only；不是 reviewer
- countUnit: `PlayerStateService::performCachedFirstAidPlanNow`
- countDelta: `+1`（仍仅申报；等待父级复审与统一 fresh build）
- repaired finding: Parent Source Review #1 `P1` refreshed binding invalid-origin overwrite

### Exact Source Repair

唯一 Java 修改位于 DHXY
`src/main/java/com/bot/dhxy/service/playerstate/PlayerStateFirstAidLocalMacroMechanics.java` 的
`executeCachedFirstAidPlanDirect(...)` refreshed-binding 接受条件：

```java
if (refreshed.isPresent()
        && refreshed.get().hasGeometry()
        && refreshed.get().getX() != -1
        && refreshed.get().getY() != -1) {
```

- 只有 refresh 返回 binding、geometry 有效且 `x/y` 均不是 `-1` 时，才允许用 refreshed base 覆盖
  `plan.baseX()/baseY()`。
- refresh absent、geometry 无效、`x == -1` 或 `y == -1` 均落入原 `else`，继续使用方法入口保存的 stored plan base，
  并保持原 `first-aid cached plan using stored base` warning/fallback。
- refreshed base 有效但坐标变化时，原 refresh info log、覆盖顺序、safe mouse-away、ordered right-click 与
  `COMPLETED/INTERRUPTED` terminal 均未改变。

### Scope / Semantic Check

- Java write set: 仅 `PlayerStateFirstAidLocalMacroMechanics.java`
- report write set: 仅 append 本固定报告
- untouched: Cloud `PlayerStateService`、四个 caller、Cloud port、DHXY handler、command/result、generic shared 12、
  incense/status、其它 Service、host/config
- retry/restore/TTL/state: 未新增；valid plan 入口 claim 后不恢复的 696 合同不变
- Maven/test/runtime/application/server/host/Task/poller/UI/capture/input: 按禁令未运行
- Git mutation: 未执行
- intentional business differences: `无已批准业务差异；按 696a12b0 refreshed base 有效性条件等价返修`

### Delivery

- status: `REPAIR #1 IMPLEMENTED - READY FOR PARENT SOURCE REVIEW`
- Parent Review #1 P1 repair condition: 已按精确条件闭合
- countDelta: `+1` 仍待父级源码复审与统一 fresh build 后实际计数

## Parent Source Review #2 - 2026-07-15T02:26:00-04:00

结论：**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD。** 父级独立走读并参考非绑定
preflight：refreshed binding 只有 `present && hasGeometry && x != -1 && y != -1` 才覆盖 stored plan base；absent、
geometry 无效、任一坐标为 `-1` 均保持 `plan.baseX/baseY` 与原 warning/fallback。后续 mouse-away、ordered target
right-click、固定 delay 和 COMPLETED/INTERRUPTED 映射未改；无 retry/restore/TTL/state 增量。

`countUnit=PlayerStateService::performCachedFirstAidPlanNow` 的 `countDelta=+1` 仍待 fresh DHXY compile 与 Cloud
clean package 同轮通过；ledger 暂为 `189/407`。无已批准业务差异；按 `696a12b0` 基线等价迁移。
