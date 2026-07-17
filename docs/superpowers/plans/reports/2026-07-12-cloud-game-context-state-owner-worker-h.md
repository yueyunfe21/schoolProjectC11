# Worker H：Cloud per-run `GameContext.State` 权威

## Parent Task Brief #1 - 2026-07-12

### 目标

设计 Cloud same-process per-taskRun `GameContext.State` owner，使迁入 Service/Task 的现有 `GameContext` API 在一个 exact
tenant/run 上绑定独立 State，pause/resume 同 run 保留业务状态，旧 revision 不能重新绑定，terminal 可确定释放。首轮只做
Design #1；父级 `DESIGN APPROVED` 前不得修改 Java/Maven/resources/tests。

### 必读

- `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/业务逻辑.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、迁移矩阵。
- 两仓 `GameContext`；Cloud `TaskExecutionContext`/`CloudTaskServiceExecutionContext`、authority assembly、coordinator、
  checkpoint、resume executor-readiness、`BaseTaskTemplate` 最新批准实现。

### 不变量

1. 不修改 `GameContext` 的 public API/字段默认值/reset 语义；不把 singleton `defaultState` 当多租户权威。
2. State owner 绑定 exact tenant/user/device/session/taskRun/window/stopEpoch；revision 变化后旧 activation handle 永久失效。
3. pause/resume 仍是同 taskRun，必须保留该 run 的业务 State；只有 current confirmed ACTIVE revision 可重新 bind。STOPPED/
   COMPLETED 后不能复活；释放策略必须有 exact lifecycle 证据。
4. 不声称 durable crash recovery：进程重启后 State 丢失必须 fail-closed，未来 durable business checkpoint 是独立门。
5. 不接受 public raw taskRunId/state mint，不使用 ThreadLocal holder 作为 authority；ThreadLocal 只允许在已授权同步调用栈内由
   `GameContext.callWithState/runWithState` 作临时投影，并保证 finally 清理。
6. 明确 global/tenant non-terminal/retained 硬配额、无 TTL/后台线程/静默淘汰；不得启动 Task/host。
7. 与 A AutoCombat、B artifact、G startup role gate 文件写集零交集；不得修改 BaseTaskTemplate/TaskStepExecutor。

### Design #1 必交付

- owner key、immutable activation handle、mint/revalidate/bind/release 状态机与锁/CAS时序。
- pause/resume/stop/complete/stale/错 scope/window/session/容量/崩溃矩阵。
- GameContext State 各字段在同 revision、跨 resume、terminal、restart 的保留/清理表。
- exact 文件写集、可见性、bean/authority 构造图、future Task/Service 调用方式；不得开放 public state map。
- 推荐方案、备选方案、P0/P1/P2 和实施/package 门。任何业务字段持久化或重置差异必须 BLOCKED。

### Worker 规则

- 你是实现 Worker，不是 reviewer。只向本文件 append `Internal Worker H - Design #1`；批准前不改代码。
- 保护全部 dirty/untracked，不回滚、不覆盖、不提交；不运行 Maven/测试，不启动 application/server/host/Task/poller/UI/
  capture/OCR/input。

## Internal Worker H - Design #1 - 2026-07-12

### 0. 角色、范围与停止门

- 本轮只完成 Cloud same-process per-taskRun `GameContext.State` authority 设计和两仓只读盘点；Worker H 不是 reviewer，
  本节自审不构成 `DESIGN APPROVED`。
- 本轮唯一写入是向本报告追加本节。未修改 Java、Maven、resources、tests、CR 卡、dashboard 或其它 Worker 报告；
  未运行 Maven/测试；未启动 application/server/host/Task/poller/UI/capture/OCR/input；未执行 Git mutation。
- 父级在本文件明确写入 `DESIGN APPROVED` 前，下面的 Java 写集只是候选，仍然全部冻结。
- 本设计只拥有 `GameContext.State` 的 same-process identity、保留、投影和释放，不拥有 Task phase、action ledger、
  startup role、artifact、机械窗口事实或 lifecycle transition。

### 1. 已读基线、两仓事实与 dirty 保护

已完整读取本文件 `Parent Task Brief #1`，并核对：

- `AGENTS.md`、`docs/DHXY_CONTEXT.md`；
- `docs/业务逻辑.md`，包括五倍/修罗默认按基线等价迁移、禁止自行新增 TTL/reset/cleanup/retry/fallback/验证，
  已验证回城快照跨 pause 保留、五倍 dialog interest 无 TTL、五倍/修罗 phase/fallback 顺序和修罗
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 基线门；
- `docs/ACTIVE_WORK.md` 顶部 CR271、CR271 当前卡片、
  `docs/superpowers/specs/2026-07-12-service-migration-matrix.md`；
- Cloud Task/Service context、retained typed Service port、same-process checkpoint、resume executor-readiness、
  Worker F 最新父级批准的 `BaseTaskTemplate`/`TaskStepExecutor` 实现；
- A AutoCombat、B artifact/template、G startup role gate 的当前 brief/设计与文件边界。

两仓只读快照：

| 仓库 | 分支 / HEAD | dirty/untracked 事实 | 本轮处理 |
|---|---|---|---|
| DHXY | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` | 配置、文档、输入、Service、Task、window 与整个 `cloud/remote/**` 存在大量用户/并行 dirty/untracked；本报告本身也在 untracked reports 树中 | 全部保护；只追加本报告 |
| Cloud Brain | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | `pom.xml`、server/engine/gateway 有 dirty，`com/bot/**`、`remote/**`、`host/**`、endpoint 与资源有大量并行 untracked | 全部保护；本轮零写入 |

源码基线事实：

1. 两仓当前 `com.bot.dhxy.core.GameContext.java` SHA-256 均为
   `26B4A9A7963E4E4159D835CD3AF8E3A9EDEB2227A744F7E5C07E0E7877DAEEC9`，Cloud 副本与 DHXY HEAD-clean
   源逐字节一致。public API、字段默认值、null normalization 和 `resetRuntimeState()` 语义不能改。
2. 当前 `GameContext` 是一个 Spring `@Component`，内部有 singleton `defaultState` 和
   `ThreadLocal<State>`；`callWithState/runWithState` 会 `finally remove()`。这些只是兼容投影工具，当前没有 Cloud
   per-run State owner，`defaultState` 绝不能作为多租户运行权威。
3. Cloud `TaskExecutionContext -> CloudTaskServiceExecutionContext -> CloudTaskRunExecutionContext` 已从 coordinator
   投影 exact scope/taskRun/taskType/window/stopEpoch/runRevision；构造链不可由 public metadata 单独铸造。
4. `CloudTaskRunExecutionGate.createContext` 只允许 current confirmed `ACTIVE` revision；`context.throwIfStopRequested()`
   只有 current confirmed ACTIVE 可继续，PAUSED/stale/newer/completed/denied 均 typed unwind。
5. coordinator 的实际 revision 是 PREPARED `r0`、初次 ACTIVE `r1`；pause/resume 各自 `+1 revision` 且 stopEpoch
   不变；STOPPED 为 `revision+1, stopEpoch+1`；COMPLETED 为 `revision+1, stopEpoch` 不变。
6. resume executor-readiness 只确认本地 registry/operation-ledger 的机械 drain 与 exact resumed revision；它不携带、
   不恢复、也不能决定 `GameContext.State` 或任何 Task phase。
7. Worker F 已批准实现保留 `BaseTaskTemplate` 的源 `before -> checkpoint -> steps -> after`、retry/delay/result 与
   `GameContext` 落态；本 H 不能在 Base 内再加 wrapper/checkpoint，也不能修改 `TaskStepExecutor`。
8. 当前 Cloud authority assembly/Task/host 没有 runtime caller，concrete Task cohort 仍 dormant；因此可先实现 dormant
   owner，但不能据此声称 Task 可启动。

**业务差异结论：** 无已批准业务差异；按基线等价迁移。本设计不持久化任何业务字段，不改变任何字段默认值、
reset、phase、keep-turn/park、retry/fallback、expiry、输入或验证顺序。

### 2. 方案比较与推荐

#### 方案 A：assembly-owned per-run entry + opaque revision activation handle（推荐）

- 一个 `CloudTaskRunAuthorityAssembly` 只构造一个 package-private State owner；owner 以 exact run key 持有一个
  `GameContext.State`，每个 confirmed ACTIVE revision 只铸造一个不可伪造 activation handle。
- pause 只让旧 handle 因 coordinator revision 变化而失效，State entry 不 reset、不复制、不释放；resume confirmed 后用
  前一 handle CAS 到新 revision handle，继续投影同一个 State 对象。
- Task 顶层同步调用由 owner 在 per-entry execution lock 内执行 `gameContext.callWithState(state, action)`；嵌套 Service
  直接继承同一同步调用栈，不进行第二次 owner bind。
- 优点：保持现有 `GameContext` API 和业务字段语义；跨 run 并发、同 run 串行；无 public map/raw state；可以明确
  terminal release 与 capacity。

#### 方案 B：每个 `TaskExecutionContext`/revision 新建一个 State（拒绝）

- 实现看似最小，但 pause/resume 每次 revision 都会得到空 State，丢失 `me`、combat/action、task progress 与
  auto-combat 计数；或者需要复制字段，形成未经批准的 reset/copy/merge 业务规则。
- 它也会让旧 revision State 和新 revision State 同时存在，无法定义哪个是权威，直接违反 same taskRun 保留要求。

#### 方案 C：把 State 放入 durable catalog/WAL（后置，不在本切片）

- 真正 crash recovery 需要字段 schema/version、原子 checkpoint、Task phase/action 一致性和恢复裁决；当前 coordinator、
  action ledger、broker 都是 process-local，不能只序列化 `GameContext.State` 冒充 durable recovery。
- 本切片明确不实现。未来 durable backend 必须另开批准设计，并逐字段决定持久化/恢复语义。

`defaultState`、裸 `ConcurrentHashMap<String, State>`、public `stateFor(taskRunId)`、业务层自己调用 `bindState`，以及把
ThreadLocal/holder 当 current-run authority，均不属于备选方案，直接禁止。

### 3. Exact owner key、entry 与不可伪造 handle

#### 3.1 Stable `GameStateRunKey`

每个 State entry 的 immutable key 固定为：

```text
GameStateRunKey
  scope.tenantId
  scope.userId
  scope.deviceId
  scope.clientSessionId
  taskRunId
  taskType
  window.windowId
  window.nativeHandle
  window.processId
  window.playerIdentityEpoch
  nonTerminalStopEpoch
```

- `scope` 是完整 `RemoteTaskRunScope`，session 不折叠；quota owner 才使用 tenant/user/device 三元组。
- `window` 是完整 `RemoteTaskRunWindow` tuple，不只用 `windowId`。
- `nonTerminalStopEpoch` 是 PREPARED/ACTIVE/PAUSED 整个 run 的稳定 stop epoch。pause/resume 不变；STOPPED terminal
  evidence 必须是 `key.stopEpoch + 1`，COMPLETED 必须仍等于 `key.stopEpoch`。
- `taskType` 一并进入 key，防止同 run 的 effective Task 漂移；它必须与 existing coordinator binding 及
  `metadata.taskCode` 的批准合同相同。
- key 只能从 exact coordinator binding/context 投影；不接受 public raw tenant/taskRunId/window/stopEpoch 参数组合。

#### 3.2 Owner entry

owner 的 private entry 至少包含：

```text
key
entryNonce                    // process-random, never exposed as raw constructor input
GameContext.State state       // exactly one object for this taskRun
EntryPhase RESERVED | RETAINED | RELEASE_PENDING
currentActiveRevision
activationGeneration
currentActivationHandle
activeProjectionCount         // diagnostic/assertion; execution lock makes it 0 or 1
terminalBinding               // only while exact release is pending
ReentrantLock executionLock   // serializes State mutation for this run only
```

owner 本身有 process-random `ownerInstanceId`。map、entry、State、quota counter、lock 和 handle constructor 全部 private；
不提供枚举、lookup、snapshot State、remove-by-id 或 public state map。

#### 3.3 Immutable activation handle

`StateActivationHandle` 是 owner 的 package-private nested final class，private constructor，字段固定为：

```text
ownerInstanceId
entryNonce
GameStateRunKey key
runRevision
activationGeneration
```

- handle 不包含 `GameContext.State` 引用，不提供 getter 把 raw key/state 交给业务代码。
- owner 每次初始 activation 或 confirmed resume 都把 entry generation `+1` 并铸造新 handle；旧 handle 即使仍在内存中，
  也因 generation/revision 不再等于 entry current handle 而永久失效。
- exact 同 revision 重入返回 entry 已保存的同一 current handle，不创建第二 generation，不打断正在运行的调用。
- owner 还使用一个同样不可伪造的 `InitialStateReservation`。它只由 current-process PREPARED binding 建立，绑定
  ownerInstanceId/entryNonce/key/preparedRevision；它不能执行 Task，只能在初次 ACTIVE confirmed 后换成 activation handle，
  或在 PREPARED stop 后用于 terminal release。

### 4. Mint、revalidate、bind、pause/resume、release 状态机

#### 4.1 Initial reservation 与 activation

```text
coordinator PREPARE returns exact PREPARED r0
  -> owner.reserveInitial(preparedBinding)
       - coordinator.find exact equality outside owner locks
       - exact duplicate entry first; then atomic capacity admission
       - new GameContext.State only after all hard quotas pass
       - entry = RESERVED, no Task authority

coordinator ACTIVATE r1 + initial CONFIRM_EXECUTION
  -> assembly creates exact TaskServiceRuntime/context
  -> owner.activateInitial(reservation, context)
       - context/key/r1 exact match
       - current confirmed ACTIVE typed check
       - RESERVED -> RETAINED CAS
       - mint one r1 activation handle
```

若 PREPARE 后 startup role gate 拒绝、local registration 失败或 activation 未完成，coordinator 仍按既有路径 STOP；
owner 只凭 exact STOPPED binding 释放 reservation。它不把失败解释成业务结果，也不重试 lifecycle。

#### 4.2 Authorized synchronous projection

唯一 State 投影入口为 package-private：

```text
<T> T callWithState(StateActivationHandle handle,
                    TaskExecutionContext context,
                    Supplier<T> action)
```

线性化顺序：

1. 校验 handle ownerInstanceId/entryNonce/key/revision/generation 与 context 的 exact identity；不返回 raw State。
2. 不持 owner monitor 地获取该 entry 的 `executionLock`，从而等待旧同步栈完全退出，但不阻塞其它 run。
3. 在锁内重新校验 map 仍指向同一 entry、未 `RELEASE_PENDING`、handle 仍 current。
4. 调用 `context.throwIfStopRequested()` 做结构化 current confirmed ACTIVE 检查；不解析 reason，不 mint replacement。
5. owner monitor 内做最后一次 exact CAS，`activeProjectionCount 0 -> 1`；这一点是 State bind 的线性化点。
6. 调用 `gameContext.callWithState(entry.state, action)`。`GameContext` 自己在 finally 清 ThreadLocal；owner finally 将
   projection count `1 -> 0` 并释放 execution lock。

coordinator 可以在线性化点之后接受 pause/stop；这与既有 cooperative checkpoint 模型一致：当前同步栈到下一已有
checkpoint typed unwind。owner 不在 Base 的 `beforeTask` 前插入新的业务 checkpoint，也不改变 Base 内已批准顺序。

禁止 owner bind 嵌套：一个 Task 顶层调用只 bind 一次，Task 调用的同步 Service 直接读取同一投影。异步 callback、future、
新线程不继承 ThreadLocal；它必须由 future trusted host 在自己的顶层入口重新提供 exact handle/context 并调用 owner。
业务 Task/Service 永远不直接拿 owner/handle，也不能自行 bind。

#### 4.3 Pause/resume 保留同一个 State

```text
ACTIVE rN handle -> coordinator PAUSED rN+1
  old context/handle: checkpoint denied; wrapper finally clears projection
  owner entry: retained unchanged; no reset, no copy, no release, no TTL

coordinator RESUME ACTIVE rM + executor-readiness confirmation
  -> new exact TaskServiceRuntime/context rM
  -> owner.activateResumed(previousCurrentHandle, newContext)
       - previous handle must still identify entry.currentActivationHandle
       - new context key equal, stopEpoch equal, revision strictly greater
       - current confirmed ACTIVE typed check
       - acquire executionLock, so old stack has fully unwound
       - CAS generation/revision and mint rM handle over the SAME State object
```

owner 不复制 coordinator 的 PAUSED 状态机，也不把 runner/readiness negative signal 变成业务事实。若 resume 已 ACTIVE 但
未 confirmation、旧栈尚未退出、previous handle 丢失、owner entry 缺失或 exact key 漂移，activation fail-closed；不新建
State、不回退 defaultState、不从字段默认值“热恢复”。

#### 4.4 Stale rejection

以下任一项不匹配都在 `GameContext.callWithState` 之前拒绝：owner instance、entry nonce、full key、current handle identity、
activation generation、runRevision、exact context identity、current confirmed ACTIVE classifier。拒绝不改变 State、quota、
coordinator、action ledger 或业务 phase。

#### 4.5 Terminal release

release 只接受 owner 铸造的 current activation handle（从未激活的 PREPARED 可接受 initial reservation）和 exact
coordinator terminal binding：

```text
COMPLETED:
  same scope/run/taskType/full window
  terminal.revision > retained revision
  terminal.stopEpoch == key.nonTerminalStopEpoch

STOPPED:
  same scope/run/taskType/full window
  terminal.revision > retained revision
  terminal.stopEpoch == key.nonTerminalStopEpoch + 1
```

并且 `coordinator.find(scope,taskRunId)` 必须仍逐字段等于该 terminal binding。release 顺序固定为：

1. coordinator exact terminal evidence 在 owner lock 外验证；
2. owner monitor 内 CAS `RETAINED/RESERVED -> RELEASE_PENDING`，立即拒绝所有新 bind，并把 owner non-terminal usage
   减一；
3. 不持 owner monitor 地获取 entry executionLock，等待正在运行的同步栈 finally 清理；
4. future host 如需保留现有 `BaseTaskTemplate.stop()` cleanup，只能通过这个 package-private terminal cleanup slot 在同一
   State 投影下调用；owner 自己不增加 `resetRuntimeState()`、字段清空或业务 cleanup；
5. 无论该既有 cleanup 是否需要记录异常，release finally 都移除 map entry、释放 retained/global quota、清掉 owner 对
   State 的最后引用并释放 lock。State 不进入池、不复用给下一个 run。

exact terminal release 重试在 entry 已不存在时返回 `ALREADY_RELEASED`；它不重建 tombstone/State。错误 scope/window/
session/revision/terminal stopEpoch 不能利用 absent entry 得到成功。若 release 线程中断在等待 execution lock，entry 保持
`RELEASE_PENDING` 且占 retained quota，调用方只能用同一 exact capability 重试；没有后台清理或静默淘汰。

### 5. 锁、CAS 与并发时序

#### 5.1 权威锁

- `ownerMonitor`：保护 entry map、quota usage、entry phase、current handle/generation、projection count 和 terminal marker；
  只做 O(1) 内存操作，绝不在其中执行 Task、sleep、I/O、coordinator 调用或等待 execution lock。
- per-entry `executionLock`：只串行同一个 taskRun 的 State 投影/activation/release；不同 run 可并发。
- coordinator 继续使用自己的 synchronized monitor；State owner 不修改 coordinator，不成为 lifecycle authority。

#### 5.2 唯一锁序

```text
coordinator read/classify returns and releases its monitor
  -> ownerMonitor short CAS (never wait/I/O)
  -> acquire entry.executionLock WITHOUT ownerMonitor
  -> coordinator typed revalidate while executionLock is held
  -> ownerMonitor final CAS
  -> GameContext.callWithState business stack
```

- future lifecycle caller 必须先让 coordinator transition 方法返回，再调用 owner；禁止 coordinator monitor 内回调 owner。
- release 先在 ownerMonitor 标记后释放 monitor，再等 executionLock；bind/activation 持 executionLock 时可以短暂进入
  ownerMonitor。因此没有 `ownerMonitor -> executionLock` 与 `executionLock -> ownerMonitor` 同时持有的锁环。
- map admission、generation update、release marker 和 quota tuple 都在 ownerMonitor 内做 compare-and-set；不用两个独立
  Atomic counter 伪装联合事务。
- `GameContext.State` 本身不是 thread-safe；per-entry executionLock 是它唯一的跨调用并发保护。owner 不依赖
  ThreadLocal 判断 authority，ThreadLocal 只存在于已授权同步调用栈的 `GameContext.callWithState/runWithState` 投影中。

### 6. Hard quota、保留与运维

默认硬配额与 coordinator 当前默认值对齐，但由 State owner 独立原子计账：

| 配额 | 默认硬上限 | 计数口径 |
|---|---:|---|
| global retained State entries | `10_000` | `RESERVED + RETAINED + RELEASE_PENDING` 全部 entry |
| tenant/user/device retained State entries | `1_000` | 同一 quota owner 的全部 retained entry，跨 client session 合计 |
| tenant/user/device non-terminal State entries | `64` | 未收到 exact terminal release evidence 的 entry |

- exact duplicate reservation/activation/release 先于 capacity 检查，幂等请求不重复计数。
- 新 reservation 在 ownerMonitor 内一次检查三项额度；任一满额，在 `newState()` 和 map 写入前 fail-closed。
- `RELEASE_PENDING` 已从 non-terminal count 扣除，但在真正移除前仍占 global/owner retained，避免卡住的旧栈被容量
  淘汰后又访问 State。
- 不按时间过期，不设 TTL，不启动 sweeper/background thread，不做 LRU/FIFO eviction，不复用 terminal State，不因内存
  紧张静默 reset。容量满只拒绝新 initial reservation，并记录结构化计数/owner hash/run prefix/status。
- 不记录 tenant/user 明文到普通日志；诊断至少含 taskRunId prefix、windowId、revision、stopEpoch、entry phase、
  activation generation、global/owner retained/non-terminal count 和拒绝类型。

### 7. `GameContext.State` 字段生命周期表

owner 对所有字段一律“新 run 用现有默认值；same run pause/resume 原对象保留；terminal 释放对象；restart 不恢复”。
owner 不调用 setter 做额外归一化，不调用 `resetRuntimeState()` 作为 lifecycle 动作。

| State 字段 | 新 initial reservation | 同 revision / 正常调用 | pause / confirmed resume | STOPPED / COMPLETED release | process restart |
|---|---|---|---|---|---|
| `me` | `new PlayerCharacter()`；其 `name/id/gameServerName/currentMapName=null`、`x/y=0` 沿现有构造语义 | existing Service/Task 原样读写同一对象或 `setMe` | 保留同一引用和全部身份/地图/坐标；不重读、不 reset、不复制 | 只丢弃 owner 引用；不新增 `setMe(null)` 或字段清洗 | 不恢复；禁止用 default `PlayerCharacter` 冒充旧 run |
| `botStatus` | `IDLE` | `BaseTaskTemplate.before/after/stop` 与现有业务原样写 | 保留当前值；owner 不擅自写 `PAUSED/RUNNING` | existing terminal cleanup 可按原逻辑写；随后整个 State 释放 | 不恢复 |
| `currentActionState` | `FREE` | 原业务原样写 `FREE/NAVIGATING/INTERACTING/IN_COMBAT/TASK_VERIFYING` | 原值保留；owner 不把 pause 当 `FREE`，不把 resume 当新战斗事实 | existing cleanup 可按原逻辑写 `FREE`；随后释放 | 不恢复 |
| `currentTaskName` | `""` | 原业务原样读写 | 原值保留 | 不新增清空；随 State 释放 | 不恢复 |
| `currentTaskProgress` | `0` | 原业务原样读写 | 原值保留 | 不新增归零；随 State 释放 | 不恢复 |
| `autoCombatEstimatedRounds` | `-1` | 原 `AutoCombatPanelService`/future A 迁移逻辑原样读写 | 原值保留，不能因 revision 变化刷新 | 不新增 `-1` reset；随 State 释放 | 不恢复 |
| `lastAutoCombatRefreshAt` | `0L` | setter 仍只执行现有 `Math.max(0L, value)` | 原值保留；不补偿、不 TTL、不改时钟语义 | 不新增 `0L` reset；随 State 释放 | 不恢复 |

现有 `resetRuntimeState()` 仍只在原业务明确调用时重置其六个 runtime 字段，且仍不重置 `me`；State owner 不改变或
扩展该语义。任何未来要求“某字段 resume 时重建/terminal 时持久化/重启时恢复”的提议都是新的业务/DR 设计，当前
必须 `BLOCKED` 并另行批准。

### 8. Failure / lifecycle 矩阵

| 场景 | Owner 判定与动作 | 禁止行为 |
|---|---|---|
| initial current confirmed ACTIVE | reservation exact CAS 成一个 activation handle；同 revision 幂等返回同 handle | 不创建第二 State/handle generation |
| PAUSED | old context typed unwind；entry 原样 retained | 不 reset、不 release、不返回 pause boolean 给业务继续 |
| resumed ACTIVE 已 confirmed | previous current handle + new exact context CAS 到新 handle；复用同一 State | 不 clone、不从 defaultState 恢复 |
| resumed ACTIVE 未 confirmed | activation denied，entry retained | 不把 readiness pending/miss 当业务失败或 current |
| stale/older handle | owner generation/revision 或 coordinator classifier 拒绝 | 不投影 State、不 mint replacement |
| future revision | typed denied | 不猜当前 revision |
| wrong tenant/user/device/session | key 不匹配且不披露 entry | 不按 taskRunId 单独 lookup |
| wrong windowId/HWND/process/identityEpoch | full window tuple 不匹配 | 不按 windowId 宽松匹配 |
| wrong taskType | key/context 不匹配 | 不把 requestedTaskCode 当 effective taskType |
| STOPPED | exact stopEpoch+1/revision terminal evidence后 release | 不继续/复活，不把 stop 当 FAILED |
| COMPLETED | exact same stopEpoch/revision terminal evidence后 release | 不重新 activate |
| terminal release 与旧栈并发 | 先 RELEASE_PENDING 拒新 bind，再等 per-entry lock；旧栈 finally 后释放 | 不强删正在使用的 State |
| capacity full | admission 前拒绝，existing run/State 不变 | 不 TTL/evict/reset 旧 run |
| owner/Cloud process restart | 新 ownerInstanceId、空 map；所有旧 handle invalid，resume activation 因 entry/previous handle 缺失拒绝 | 不从 coordinator binding、defaultState、metadata 或磁盘猜 State |
| crash during ACTIVE/PAUSED | State 丢失；无 continuation | 不声称 durable crash recovery |
| crash during release | 进程内对象全部丢失；下次启动仍 fail-closed | 不声称 terminal cleanup durable |

当前 coordinator 也为 process-local，故进程重启后不存在可合法继续的同一 owner graph。若未来 coordinator 先获得
durable replay，H owner 仍必须拒绝 replayed ACTIVE/PAUSED binding：`activateResumed` 强制要求同一 ownerInstanceId 的
previous handle/entry。只有未来单独批准的 durable business-state rehydration owner 才能改变这条门。

### 9. Visibility、authority/bean 构造图与 future 调用图

#### 9.1 Visibility

- `CloudGameContextStateOwner`：package-private final；constructor 只由 package-private
  `CloudTaskRunAuthorityAssembly` 调用。
- 所有 key/entry/reservation/activation/release result 为 private 或 package-private nested immutable type；所有 handle
  constructor private。
- `GameContext.State`、map、quota、lock 不出 owner；business Task/Service 只继续使用现有 injected `GameContext` API。
- 不新增 public raw `(taskRunId, State)`、`stateFor`、`bind`、`release(String)`、enumeration、Spring registry 或管理 endpoint。

#### 9.2 Authority/bean 构造图

```text
future trusted Cloud composition (still dormant)
  -> one injected GameContext instance
  -> CloudTaskRunAuthorityAssembly.create(broker, gameContext)
       -> existing coordinator (from broker; assembly uniqueness claim unchanged)
       -> existing one action ledger / execution gate / command executor
       -> NEW one CloudGameContextStateOwner(coordinator, gameContext)

same assembly
  -> exact TaskServiceRuntime(context + retainedActionState)
  -> package-private state reservation/activation capability
  -> future trusted activation adapter
       -> migrated Task/Service constructed with the SAME GameContext instance
```

`GameContext` 不由 State owner `new` 第二份，也不由每 run 创建 Spring bean。future composition 必须把同一实例同时交给
assembly owner 和 migrated Task/Service；Task host 不得宽扫描出另一份 `GameContext`。本切片不修改
`CloudServiceConfiguration` 或 host，因此 bean/caller 仍不可达。

#### 9.3 Future initial Task/Service 调用图

```text
thin-client role fact -> G assignment/startup inputs
  -> effective taskType fixed before PREPARE
  -> coordinator.prepare
  -> stateOwner.reserveInitial(PREPARED exact binding)
  -> coordinator.activate + initial execution confirmation
  -> assembly.createTaskServiceRuntime(exact scope/run, metadata)
  -> G startup role gate (no GameContext authority, no State mutation)
  -> stateOwner.activateInitial(reservation, runtime.context)
  -> stateOwner.callWithState(handle, runtime.context, () -> task.execute(runtime.context))
       -> approved BaseTaskTemplate.before/checkpoint/steps/after
       -> migrated Service calls use same synchronous GameContext projection
       -> A AutoCombat mechanical facts/capture/input use runtime retained typed Service port
       -> B artifact API, if later approved, receives exact context but never owns State
```

#### 9.4 Future pause/resume/terminal 图

```text
pause revision -> existing checkpoint typed unwind -> callWithState finally clears projection
  -> owner entry remains retained unchanged

resume revision + executor-readiness confirmed
  -> create new exact runtime/context
  -> activateResumed(previousHandle,newContext) -> same State, new handle
  -> callWithState(newHandle,newContext,...)

Task success -> existing afterTask State mutation -> coordinator.complete -> releaseTerminal
coordinator STOP -> old stack STOPPED/cleanup or terminal cleanup slot -> releaseTerminal
```

没有 host transition catcher/activation adapter 时，上图不可达，所有 concrete Task/Service cohort 继续 dormant。

### 10. 父级批准后的最小文件写集与零交集

推荐 Cloud-only `1 new + 1 modify`；DHXY Java/Maven/resources/tests 零改：

| 文件 | New/Modify | 精确职责 |
|---|---|---|
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudGameContextStateOwner.java` | New | package-private per-run key/entry/quota/lock、initial reservation、revision handle、authorized projection、resume CAS、terminal release；nested types 全非 public |
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskRunAuthorityAssembly.java` | Modify | 只把同一 injected `GameContext` 和唯一 State owner 纳入现有 one-coordinator assembly，并提供 package-private dormant orchestration/access；不暴露 public capability，不启动 host |
| `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-12-cloud-game-context-state-owner-worker-h.md` | Append | Design/批准后 implementation 证据唯一报告 |

明确 zero diff：

- 两仓 `GameContext.java`、`PlayerCharacter.java`；
- Cloud `TaskExecutionContext`、`CloudTaskServiceExecutionContext`、`CloudTaskRunExecutionContext`、
  `CloudTaskRunExecutionGate`、`RemoteTaskRunCoordinator`、checkpoint/sleep 全部文件；
- Worker F 的 `BaseTaskTemplate.java`、`TaskStepExecutor.java`；
- Worker A 当前 `AutoCombatService` 整类迁云写集及 retained port/action ledger/broker/endpoint/wire；
- Worker B 的 `host/CloudArtifactStore`、`ScopedPngArtifactStore`、`CloudTemplateAssets`、
  `CloudArtifactCapacityGovernor`、`CloudServiceStorage`、`CloudServiceConfiguration`、`PackagedTemplateAssets`；
- Worker G 的 `task/startup/TaskStartupCheckService.java`、`CloudStartupGateAuthority.java`；
- 所有 host/server/routes/poller/concrete Task/Service、DHXY Java/Maven/resources/tests、Cloud pom/resources/tests。

当前 A AutoCombat 的报告尚无 Design #1 exact file table；H 实施前必须重读 A 最新追加。若 A 届时声明修改
`CloudTaskRunAuthorityAssembly.java` 或本 H new target 已出现，H 立即停在本报告记录冲突，等待父级重新分配；不得覆盖、
合并猜测或扩大文件集。

### 11. P0/P1/P2、自审与实施/package 门

#### 11.1 Worker H 设计自审

- `P0=0`：没有 defaultState/ThreadLocal authority、public state mint、错 tenant/window 宽松 key、terminal revive、
  host/Task 激活或 durable recovery 声明。
- `P1=0`：pause/resume 同 State、old handle 永久失效、terminal in-use release、restart fail-closed、联合 quota 与锁序均有
  明确线性化点。
- `P2=0`：visibility、bean/authority graph、future callers、exact write set 和并行零交集已列出。
- 以上只是 Worker 自审；状态为 **READY FOR PARENT DESIGN REVIEW**，不是批准。

#### 11.2 任何一项出现即 BLOCKED，不得实施

1. 父级要求 State 字段写磁盘、重启恢复、resume merge/copy/default fallback；这必须另开 durable business-state CR。
2. 需要改变 `GameContext` public API、字段默认值/null normalization、`resetRuntimeState()` 或 `PlayerCharacter` 语义。
3. 需要在 Base/TaskStepExecutor 增加 wrapper/checkpoint，或改变 before/after/retry/result 顺序。
4. 需要从 runner/readiness negative signal 推导业务 State、phase、成功/失败、cleanup 或 reset。
5. A/B/G/F 最新写集与 H 两个 Java 文件发生重叠，或 new target 已由并行 Worker 创建。
6. 无法维持 package-private owner/handle、exact full key、no-public-map 和 no-host-activation。

#### 11.3 父级 `DESIGN APPROVED` 后的实施门

1. 开工前重读本报告、A/B/G 最新追加和两仓 scoped status/hash；只在当前 dirty 上 patch，不回滚/覆盖。
2. Java 写集严格为 Cloud `1 new + 1 modify`；不新增测试，不改 existing tests/pom/resources。
3. 源码审查必须证明：无 public handle constructor/raw state map/defaultState authority；owner 无 holder/background thread/
   TTL/eviction/I/O；coordinator 调用不在 ownerMonitor 内；State 投影必经 finally clear。
4. 完成 Java 后按仓库 Java compile/package gate运行 fresh Cloud `mvn -q clean package`（无 skip），记录现有 suites/
   failures/errors/skipped 与 JAR；这不是本轮 Design #1 的执行授权。
5. 不启动 application/server/host/Task/poller/UI/capture/OCR/input；不做 fresh runtime；host/concrete Task cohort 继续 dormant。

**Worker H 结论：** 推荐方案 A。它只建立 same-process per-run `GameContext.State` authority，保持同 taskRun pause/resume
原对象，按 exact revision handle 拒绝 stale，凭 exact terminal evidence 确定释放，并在 restart 时明确 fail-closed；不冒充
durable crash recovery。等待父级在本文件给出 `DESIGN APPROVED` 或带 P0/P1/P2 的 `BLOCKED`。

**无已批准业务差异；按基线等价迁移。**

## Parent Implementation Review #2 - APPROVED - 2026-07-12

父级已复核 H3 最新 `CloudGameContextStateOwner.java`，并对当前 Cloud 工作树运行 fresh
`mvn -q clean package`。第一次构建在 surefire 期间遇到同目录并发 `clean`，表现为随后立即重新出现的多批
`target/classes` `NoClassDefFoundError`；确认没有 Maven 进程后重新执行同一命令，第二次 exit 0，4 suites / 21 tests，
failures=0、errors=0、skipped=0。产物 `dhxy-cloud-brain-0.1.0-SNAPSHOT.jar` 119568824 bytes，SHA-256
`A2C6B7D1AECAFFA80F5ED9E3AB049C24BBAD7339308B28145EA89FC136CDBFFB`。

源码结论：

1. `activateInitial`、`activateResumed`、`callWithState` 分别只保留一次最终
   `TaskExecutionContext.throwIfStopRequested()`；`context.revalidate()` 与 `authorizeCurrent` 调用均为 0，锁前只比较
   immutable full identity。
2. terminal release 仍先校验 owner-minted handle 与 coordinator exact current STOPPED/COMPLETED binding；通过后 entry 缺失
   稳定返回 `ALREADY_RELEASED`，存在时仍验证 nonce/current handle/terminal marker，quota 只释放一次。错误 owner/key/status/
   revision/stopEpoch 没有被吞。
3. assembly SHA 保持不变；无 public raw State/handle/map，无 PREPARED reservation、ThreadLocal authority、TTL/线程/淘汰/I/O/
   durable recovery/host 激活。

结论：**APPROVED，P0/P1/P2=0**。H3 两项返修均关闭；same-process per-run `GameContext.State` owner 可作为后续 dormant
Task/Service cohort 的依赖，但不代表 host/Task 可启动，也不增加同路径迁移计数。**无已批准业务差异；按基线等价迁移。**

## Parent Design Review #1 - DESIGN APPROVED - 2026-07-12

父级已核对两仓 `GameContext`、current confirmed context/assembly、coordinator revision/stopEpoch、checkpoint、
resume executor-readiness 与 Worker F 的 `BaseTaskTemplate`。same-process per-run 单一 State、opaque revision handle、
same-run pause/resume 保留、terminal exact release、restart fail-closed、无 defaultState/ThreadLocal authority 的方向成立。
结论：**DESIGN APPROVED，P0/P1/P2=0**，但以下父级收窄覆盖 Design #1 的 reservation 细节：

1. **删除 PREPARED reservation。** 不新增 `InitialStateReservation`、`RESERVED` phase，也不在 PREPARED 时创建
   `GameContext.State`。PREPARED 尚无业务执行需要 State；在 PREPARE 后并发 STOP 的窗口提前建 State 会制造无必要的
   orphan/release 分支。
2. 初次入口固定为 package-private `activateInitial(TaskExecutionContext exactCurrentContext)`：只有 future activation
   owner 已取得 current confirmed ACTIVE context 后，owner 才在同一 `ownerMonitor` 内做 full-key duplicate 检查、三项
   hard quota admission、`GameContext.newState()`、`RETAINED` entry 与首个 immutable activation handle。exact duplicate
   返回同一 handle；容量满在创建 State 前 fail-closed，future activation owner 必须阻止 Task 启动。
3. entry phase 只保留 `RETAINED | RELEASE_PENDING`。terminal release 只接受 current activation handle + exact current
   coordinator terminal binding；不存在 PREPARED release capability。pause 只保留 entry，confirmed resume 必须持 previous
   current handle 并在 execution lock 下对同一 State 铸新 generation；旧 handle 永久失效。
4. `callWithState`、terminal release、quota、restart、full scope/run/task/window/stopEpoch key、same `GameContext` instance、
   finally 清 ThreadLocal 与无 TTL/线程/淘汰/持久恢复声明，均按 Design #1 保持。owner/handle/map/State 不得 public。
5. 实现写集严格为 Cloud `CloudGameContextStateOwner.java` 1 new + `CloudTaskRunAuthorityAssembly.java` 1 modify；不得修改
   `GameContext`、context/coordinator/gate、Base/executor、host/concrete Task、A/B/G 文件或任何测试。当前 A 只能消费 H 的
   future owner，不得并行修改 assembly。

旧内部 Worker H 会话因桌面重启已不存在；父级将使用新的内部实现 Worker 继承上述批准写集。完成后运行 fresh Cloud
`mvn -q clean package`（不 skip）并向本文件追加 `Implementation #1`；父级源码/build 复审前不计迁移数，不激活
host/Task。**无已批准业务差异；按基线等价迁移。**

## Parent Implementation Review #1 - BLOCKED - 2026-07-12

父级已复核 H2 两个 Java 文件并运行统一 fresh Cloud package。结构、可见性、full key、无 PREPARED reservation、same-State
resume、terminal exact binding、quota/lock/no-host/no-durable 边界均成立；构建 exit 0，4 suites / 21 tests 全绿。
源码仍有两个 P2，结论：**BLOCKED，P0=0/P1=0/P2=2**。

### P2-1：`callWithState` 在业务投影前重复读取 coordinator 五次

- 证据：`callWithState` 第 185 行先 `authorizeCurrent`；第 203 行再 `throwIfStopRequested`；第 204 行再次
  `authorizeCurrent`。而 `authorizeCurrent` 第 297-305 行自身又执行一次 `throwIfStopRequested + revalidate`。一次调用因此
  产生 3 次 typed checkpoint + 2 次 authorization read，超过批准设计的单次最终 current gate。
- 影响：增加无必要的 coordinator 竞争和状态观察窗口，也违反迁移不得顺手增加验证/read 的基线纪律。
- 返修条件：pre-lock 只用 immutable context fields 与 handle/full key 做本地比较；取得 execution lock 后只执行一次
  `context.throwIfStopRequested()` 作为 current-confirmed ACTIVE typed gate，再用 context immutable identity 完成 ownerMonitor
  final compare。不得随后再 `revalidate()`。`activateInitial/activateResumed` 同理：允许锁外本地 identity precheck，只有最终
  线性化位置一次 typed current gate，不形成 wrapper 内重复 coordinator read。

### P2-2：terminal release 成功后的 exact 重试不幂等

- 证据：`releaseTerminal` 第 252 行先 `requireEntry(handle)`；entry 在首次成功调用第 280 行移除后，完全相同的
  handle + exact current terminal binding 重试会在此抛异常。第 269-270 行的
  `ALREADY_RELEASED_BY_CONCURRENT_CALL` 只覆盖已取得 entry 的并发等待者，不覆盖 response-loss/调用方重试。
- 影响：future activation owner 无法把 terminal cleanup 当稳定幂等操作，成功释放后的重试被误报失败。
- 返修条件：`requireOwnedHandle + requireCurrentTerminal` 通过后，在 ownerMonitor 内若 full-key entry 已不存在，直接返回
  `ALREADY_RELEASED`；handle 由当前 owner 私有构造且 terminal binding 仍须是 coordinator exact current，故不需 public
  tombstone/map。entry 存在时仍必须 current handle exact；错误 owner/key/terminal 不得被吞。重命名过窄的 concurrent-only
  enum 文案，并保持 quota exact-once。

只允许新的内部 H3 修改 `CloudGameContextStateOwner.java`；`CloudTaskRunAuthorityAssembly.java`、其它 Java/tests/docs 写集
均冻结（除本报告追加）。返修后父级重新运行 fresh Cloud package；通过前 H 不收口、不激活 host/Task，也不改变 G 已批准
计数 `185/407`。**无已批准业务差异；按基线等价迁移。**

## Internal Worker H2 - Implementation #1 - 2026-07-12

### 写集

- New：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudGameContextStateOwner.java`
- Modify：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunAuthorityAssembly.java`
- 未修改其它 Java、tests、resources、pom 或 DHXY 文件；未回滚/覆盖既有 dirty/untracked，未执行 Git mutation。

### 关键线性化点

- 无 PREPARED reservation、`InitialStateReservation` 或 `RESERVED` phase；`activateInitial` 仅在 current confirmed ACTIVE
  初始 revision 下，于同一 owner monitor 内完成 full-key duplicate、三项 quota admission、`GameContext.newState()`、
  `RETAINED` entry 与首 handle；容量满在建 State 前 fail-closed。
- full key 绑定完整 scope/taskRun/taskType/window/stopEpoch；pause 保留同一 State，confirmed resume 必须持 previous current
  handle，在 per-run execution lock 下等待旧栈退出后只递增 generation/revision，旧 handle 永久失效。
- `callWithState` 做 exact handle/context/current gate，execution lock 内经 typed checkpoint + coordinator revalidate，owner
  monitor 最终 `activeProjectionCount 0 -> 1`；只用同一个 injected `GameContext.callWithState` 投影，finally 清 ThreadLocal
  并做 `1 -> 0`。
- terminal release 只接受 current handle + coordinator exact current STOPPED/COMPLETED binding；先
  `RETAINED -> RELEASE_PENDING` 拒绝新 bind，再等待旧栈 finally 后移除 entry 与释放 retained/global quota。无 TTL、后台清理
  或静默淘汰。
- assembly 显式接收同一个 injected `GameContext`，仅构造一个 package-private dormant owner；main 下无 create caller，
  未注册/启动 host 或 Task。

### SHA-256

| 文件 | bytes | SHA-256 |
|---|---:|---|
| `CloudGameContextStateOwner.java` | 22775 | `956B7EECA9E73D02A2E754FE1A668925A7057959EBD5DFC8E2B45E5C0B416135` |
| `CloudTaskRunAuthorityAssembly.java` | 7753 | `76DADD9F95112660FD5E2B8A5C443EDC1452CEF5C11BC866390FE063D261077C` |

### 禁用项与未触碰范围

- owner/handle/map/State 无 public mint 或 raw lookup；无 `defaultState`/ThreadLocal authority、holder、TTL、thread/executor、
  eviction、filesystem/I/O、durable recovery、host activation；phase 仅 `RETAINED | RELEASE_PENDING`。
- 未触碰 `GameContext`、context/coordinator/gate、Base/executor、A/B/G 文件、DHXY Java、tests/resources/pom。
- 按父级并行约束未运行 Maven、javac 或测试；未启动 application/server/host/Task/poller/UI/capture/OCR/input。

Worker H2 自检仅为 QA，不是 reviewer approval。实现停止，等待父级源码与 fresh package 复审。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker H3 - Implementation Repair #1 - 2026-07-12

### 返修结论

- 只修改 Cloud `CloudGameContextStateOwner.java`，关闭 `Parent Implementation Review #1` 的两个 P2；
  `CloudTaskRunAuthorityAssembly.java` 与其它 Java 全部冻结。
- `activateInitial`、`activateResumed`、`callWithState` 的锁前检查现在只读取 immutable
  `TaskExecutionContext` identity，按完整 scope/taskRun/taskType/window/stopEpoch 和 revision 与 owner handle/key 比较。
- 三个入口分别只在各自最终 admission/resume/projection 边界执行一次
  `context.throwIfStopRequested()`；删除 `authorizeCurrent` 及所有 `context.revalidate()`，不增加 coordinator read。
- `releaseTerminal` 仍先通过 owner-minted handle 校验和 exact current STOPPED/COMPLETED binding 校验；随后在
  `ownerMonitor` 内若 full-key entry 已不存在，幂等返回 `ALREADY_RELEASED`。entry 存在时仍严格校验 entryNonce、
  current handle identity/generation/revision、terminal marker，并保持 non-terminal/retained/global quota exact-once。
- `ALREADY_RELEASED_BY_CONCURRENT_CALL` 已重命名为覆盖 response-loss 与并发重试的 `ALREADY_RELEASED`；错误 owner、
  full key、terminal status/revision/stopEpoch/current coordinator binding 均未放宽。

### 精确 diff

1. 删除 `RemoteTaskRunAuthorization` import、`authorizeCurrent(...)` helper 和已无调用的
   `GameStateRunKey.from(RemoteTaskRunBinding)`。
2. 新增 `GameStateRunKey.from(TaskExecutionContext)`，从 context immutable getters 重建完整
   `RemoteTaskRunWindow(windowId,nativeHandle,processId,playerIdentityEpoch)` 与 full run key。
3. `activateInitial`：本地验证 revision/key，唯一 typed gate 后在 owner monitor 内完成 duplicate/quota/newState/handle
   admission；不再 revalidate。
4. `activateResumed`：锁前本地验证 previous key 与 strictly newer revision，取得 execution lock 后唯一 typed gate，
   owner monitor 内重验 current handle 并为同一 State 铸造新 generation。
5. `callWithState`：锁前本地验证 exact key/revision，取得 execution lock 后唯一 typed gate，owner monitor 内重验
   entry/current handle/projection count 后投影 State。
6. `releaseTerminal`：exact terminal 验证后先按 full key lookup；absent 返回 `ALREADY_RELEASED`，present 仍须 nonce 和
   current handle exact；等待 execution lock 后的并发已释放路径返回同一枚举。

### SHA-256 与静态扫描

| 文件 | bytes | SHA-256 |
|---|---:|---|
| `CloudGameContextStateOwner.java` | 22174 | `8D5BBEFAC713DA2AD8FFF1C95E4A79701DF184EFFC8EA022FA4228B15E584DBF` |

- `context.throwIfStopRequested()`：`3` 处，分别属于 initial/resume/projection；
  `context.revalidate()`：`0`；`authorizeCurrent`：`0`。
- `coordinator.find(...)`：`1` 个源码调用点，仅保留在 `requireCurrentTerminal` 的 exact current terminal 校验；
  没有新增 coordinator read。
- `ALREADY_RELEASED`：两条返回路径 + 一条 enum；旧 concurrent-only enum：`0`。
- 禁用项扫描无命中：没有新增 Thread/Executor/Scheduler/TTL/eviction/filesystem/I/O/public raw State 或 public handle。

### 未触碰范围

- `CloudTaskRunAuthorityAssembly.java` 保持 7753 bytes，SHA-256
  `76DADD9F95112660FD5E2B8A5C443EDC1452CEF5C11BC866390FE063D261077C`。
- 未修改其它 Cloud Java、tests、resources、pom、DHXY Java、CR/ACTIVE_WORK/迁移矩阵/dashboard 或其它 Worker 报告；
  未回滚/覆盖既有 dirty/untracked，未执行 Git mutation。
- 按父级指令未运行 Maven、javac 或测试；未启动 application/server/host/Task/poller/UI/capture/OCR/input。

H3 返修完成并停止，等待父级源码审查与统一 fresh Cloud package。本节是 Worker QA 证据，不构成 Approved。

**无已批准业务差异；按基线等价迁移。**
