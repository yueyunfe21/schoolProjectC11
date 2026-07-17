# Worker M：持续观察账本 final-consumed retirement / compaction

## Parent Task Brief #1 - 2026-07-12

### 目标

为 CR271 / BattleRadar R0 设计跨两仓的有界持续观察账本退休协议。当前 Cloud
`RemoteGameCommandBroker.requestLedger`（owner 1000）、`CloudTaskRunActionLedger`（combined 10000）与 DHXY
`RemoteOperationLedger` 均保留历史且不删除；BattleRadar 1s/4s/10s capture 流若每次使用正确新 identity 会有限耗尽。
首轮只向本文件追加 `Internal Worker M - Design #1`；父级 `DESIGN APPROVED` 前零 Java/Maven/resources/tests。

### 必读与基线

- 完整读取 `D:\mavenProject\DHXY\AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、
  `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`、迁移矩阵与 L 固定日志最新全部材料。
- 只读核对两仓最新源码：Cloud `RemoteGameCommandBroker`、`CloudTaskRunActionLedger`、
  `CloudTaskRetainedActionState`、`CloudTaskRunCommandExecutor`、outcome endpoint/ack/digests/coordinator；DHXY
  `RemoteOperationLedger`、polling loop、transport、handler、outcome ack/digests/registry。记录当前 caps、key、状态迁移、
  UNKNOWN late-final、duplicate 与 restart 行为。
- DHXY HEAD `0114604e` 与 Cloud HEAD `3b988caa` 只是提交锚；保护两仓全部 dirty/untracked，当前 remote 源包含 A 在途
  observer 实现。不得用 HEAD 覆盖当前源码。

### 硬不变量

1. pending/unbound/dispatched/UNKNOWN 及可能迟到 final 的 exact request bytes 永不提前退休；UNKNOWN 只同字节重投，
   不换 requestId/actionId/captureId，不自动 retry。
2. 只有 Cloud exact business consumer 明确消费一个 final outcome 后，才可发出 final-consumed acknowledgement；transport
   收到 final 不等于业务已消费。ack 必须 exact scope/taskRun/operation/semantic address+occurrence/requestId/actionId/
   requestDigest/outcomeDigest，错租户/错窗/错 revision/乱序/重复全部 deterministic。
3. 压缩后旧 duplicate 仍不得被当作新命令执行。必须设计单调 occurrence/frontier 或等价可证明机制：frontier 以下请求
   deterministic duplicate/final-consumed reject；不能仅依赖有限 tombstone 集，也不能 TTL/LRU/静默 eviction。
4. Cloud broker ledger、Cloud action/observation ledger 与 DHXY operation ledger 的提交顺序、锁序、crash 窗口和幂等重放
   必须闭合；不得出现一侧已忘记而另一侧可重新执行。ACTIVE 与 PAUSED observation combined quota/mode conflict/no-renewal
   语义必须保留。
5. tenant/user/device/clientSession/taskRun/window/stopEpoch/runRevision 隔离与 enqueue/dispatch/local side-effect 三道门不削弱；
   不开放 raw request/poll/outcome/retire API 给业务 Service。
6. 不新增 durable 恢复声明。若当前 same-process authority 无法跨重启证明 frontier，明确 restart fail-closed/active-run 处置；
   不引入数据库、外部付费、凭据、生产切换。
7. 不改变任何 Task/Service 业务 phase、capture 数、timer、retry/fallback/click/navigation/stop/pause 语义。host/Task/caller
   全程 dormant。

### Design #1 必交付

- 三账本逐字段/逐 map/key/cap/state/lock/late-final inventory，以及当前耗尽时间上界。
- exact final-consumed ack 的 typed wire DTO、digest 公式、producer/consumer owner、重投与响应状态机；明确为什么普通
  outcome ack 不能替代。
- per-taskRun/semantic-slot monotonic occurrence 与 compacted frontier 数据模型；允许/拒绝矩阵覆盖：ack before final、
  UNKNOWN、late final、duplicate ack、out-of-order ack、gap、stale revision、pause/resume、stop/complete、wrong scope/window、
  broker/action/local ledger 缺项。
- Cloud 与 DHXY 原子提交/锁序/故障矩阵：每个 crash 点重启后如何 fail closed，不允许“先删一边再尽力删另一边”。
- bounded capacity 模型，说明 frontier/tombstone 的空间上界、owner/global 配额、终态 taskRun cleanup 与审计保留；不得
  TTL/LRU/自动 retry。
- exact 两仓文件写集、调用 DAG、与 A deadline Repair/K current-slot/B artifact/J turn/L1 零交叉或严格顺序证明。
- 建议最小实施波次；P0/P1/P2 与自审。自审不算批准。

### Worker 规则

- 只追加 Design #1，不改 Java/Maven/resources/tests，不运行 Maven/测试，不启动 server/host/Task/poller/UI/capture/input。
- 不创建 agent/reviewer，不修改其它 Worker 日志，不提交/回滚/覆盖。
- 完成后停止等待父级审查；BLOCKED 由同一 M 返修。

**无已批准业务差异；按基线等价迁移。**

## Parent Recovery Notice #1 - FULL-R0-DESIGN-R1 Reissued - 2026-07-13T04:18:00-04:00

Parent Review #5 在 `03:08` 发布后，原 Internal P 会话因重启已不可达，且本日志截至本节前没有 Repair #1 的新
`CLAIMED` 或交付。为避免把“已发单”误当“已领取”，父级按接管规则把同一设计返修原样交给 replacement Internal P2；
不修改 Review #5 的五项 P1/P2，也不扩大写集。

P2 必须先追加 `Internal Worker P2 - CLAIMED`（`FULL-R0-DESIGN-R1`、领取时间、唯一写集=仅本日志），随后只写
`Full R0 Design Repair #1 Delta`。Java/schema/resources/tests/Maven/runtime 继续冻结；A 最终 scale-wire 已 APPROVED，可按其
最终源码与 hash 更新合并点，但不得修改 A/B/R/host/Task/caller。Worker 自审只算 QA，父级仍是唯一 reviewer。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker M - Design #1 - 2026-07-12

### 0. 结论与推荐方案

本轮只做设计。已按 Parent Task Brief #1 完整读取固定材料并只读核对两仓当前落盘源码；未把 DHXY
`0114604e` 或 Cloud `3b988caa` 当作覆盖当前 dirty/untracked 的理由。当前 A 的 PAUSED observer 已在两仓落盘，
K current-slot 仍停在 Design Repair #3，M 不抢改任何文件。

推荐采用 **单调语义 occurrence + attempt 子序号、Cloud business-consumed notice、DHXY frontier apply receipt、
Cloud 最终双账本压缩** 的两阶段协议：

1. 每个机械请求在 request digest 内携带 typed `RemoteSemanticAddress(phaseCode, actionSlot,
   occurrence, attempt)`。`occurrence` 表示同一 semantic slot 的业务观察/动作序号；`attempt` 只承载 ACTIVE
   `NOT_EXECUTED` 后现有可信 renewal，observer 永远固定 `attempt=0` 且无 renewal。
2. 普通 outcome endpoint 只证明 Cloud transport/broker 接收了 terminal outcome。Cloud exact business consumer
   解释并应用该 final 后，才通过 typed Service port 显式提交 `FINAL_CONSUMED`；UNKNOWN、未绑定、未 dispatch、
   未记录、仍可能 late-final 的记录均不能提交。
3. Cloud 先保留全部三账本明细，仅把 exact `RemoteFinalConsumedAck` 作为控制项放入现有 client route poll。
   DHXY 在一个 ledger monitor 事务内验证 exact terminal、推进 slot frontier、删除本地 request/action 明细并生成 receipt。
4. DHXY 把 exact receipt POST 回 Cloud。Cloud 只有在 receipt 已证明 DHXY frontier 落盘到本进程内存后，才在同一
   retirement 临界区同时压缩 broker request/action 明细和 Cloud action/observation 明细，并推进 Cloud frontier。
   绝不先删 Cloud 再“尽力通知” DHXY。
5. 网络丢包只重投同一个 ack/receipt bytes。无 timer retry、无新 ID、无 TTL/LRU；显式下一次
   `finishFinalConsumption` 或 poller 重启只续送 retained control bytes。

对比方案：

- **推荐方案**：poll 返回 `FINAL_CONSUMED` typed control，receipt 使用独立 authenticated endpoint。它复用现有
  client-initiated 网络方向，不要求 DHXY 开 inbound server，也不把 retirement 伪装成第四种机械 operation。
- **不选独立 retirement long-poller**：会新增第二线程/第二 cadence/第二 route owner，与当前单 poller 的 dormant
  激活门交叉更大。
- **禁止有限 tombstone/TTL/LRU**：只能记住最近 N 个 request，无法证明更老 duplicate 不执行，违反硬不变量。

### 1. 只读基线与当前耗尽上界

- DHXY：`thin-client-design@0114604e1ff5f15491d2910959c45252e893d04f`；当前 `cloud/remote/**` 全部为
  untracked/dirty 迁移源码，完整保留。
- Cloud：`navigation-migration@3b988caa010254973e03342272e6d1d6a9685b01`；当前 `remote/**`、host 与
  `com/bot/**` 为在途源码，完整保留。
- Cloud broker 默认 retained request cap：global `10000`、tenant/user/device owner `1000`、owner pending
  `64`；input action cap global `10000`、owner `1000`；route global `1000`、owner `64`、queue `64`。
- Cloud action/observation ledger：每 authority assembly combined cap `10000`，ACTIVE + OBSERVATION 共用，永不删。
- DHXY operation ledger：无 cap，按实际请求线性增长，永不删；不会先拒绝，但会持续占内存。

按 L 冻结的 recurring capture 上限估算（不计一次性 baseline/refresh 额外帧）：

| 场景 | 最大 recurring capture 速率 | broker owner 1000 首次耗尽 | action combined 10000 首次耗尽 |
|---|---:|---:|---:|
| FREE/普通 10s full radar，Stage 1-3 全 miss | `3/10s` | `3333.3s = 55m33s` | `9h15m33s` |
| IN_COMBAT 4s full radar，Stage 1-4 全走 | `4/4s` | `1000s = 16m40s` | `2h46m40s` |
| expected fast-exit 1s avatar + 4s full radar Stage 1-4 | `1/s + 4/4s = 2/s` | `500s = 8m20s` | `5000s = 1h23m20s` |

所以最早硬失败由 broker owner cap 主导，固定 cadence 下最坏约 **8 分 20 秒**；即使只跑 10s 普通雷达，约
55 分 33 秒也必满。多窗口同时运行时 global 10000 只把 owner 数量放大，不修复单 owner 的有限耗尽。

### 2. 三账本逐 map/key/cap/state/lock inventory

#### 2.1 Cloud `RemoteGameCommandBroker`

| 字段/map | 当前 key/value | cap/生命周期 | 当前状态与问题 |
|---|---|---|---|
| `commandQueues` | `RemoteClientScope -> ArrayBlockingQueue<PendingCommand>` | route global 1000、owner 64、每 route 64；route 不删 | command terminal 时从 queue 移除，但 route 本身和 owner route usage 永留 |
| `requestLedger` | `LedgerKey(scope, operation, requestId) -> PendingCommand` | global 10000、owner 1000；永不删 | retained exact typed request、accepted time、deadline、terminal future、late resolution、delivered session |
| `inputActionLedger` | `ActionKey(tenant/user/device, windowId, actionId) -> (clientSessionId,requestId,requestDigest)` | global/owner 10000/1000；永不删 | 永久阻止 input actionId 换 request；普通 capture/fact 不入此 map |
| `inputFlights` | `(tenant/user/device,windowId) -> PendingCommand` | 同一 window 一条 input flight | 非 UNKNOWN final 释放；UNKNOWN 保留到 exact late non-UNKNOWN |
| `usageByOwner` | `(tenant,user,device) -> retained/pending/action/route counts` | checked increment/decrement | terminal 只减 pending；retained request/action/route 永不减 |

唯一全局锁是 `stateLock`。当前锁序注释为 broker `stateLock` 先于 coordinator pause guard；coordinator 不回调
broker。`PendingCommand` 自身 synchronized 只守 dispatch/deadline/future 字段。隐式状态：

```text
REGISTERED_UNDISPATCHED
  -> DISPATCHED
  -> TERMINAL_UNKNOWN (terminal future=UNKNOWN, lateResolution=null)
  -> TERMINAL_FINAL (terminal future 非 UNKNOWN，或 UNKNOWN + lateResolution 非 UNKNOWN)
```

- exact requestId+digest 重入：返回现有 terminal/late final，不 requeue、不 redispatch。
- same requestId different digest：`IDEMPOTENCY_CONFLICT`。
- timeout 在 dispatch 前为 `NOT_EXECUTED`，dispatch 后为 `UNKNOWN`；observation deadline 用 wall clock，ACTIVE pause
  仍按现有 pause progress 冻结。
- late final：只有 recorded terminal 是 UNKNOWN 时可写一次 `lateResolution`；同 outcomeDigest duplicate，异 outcome
  conflict。request 和 UNKNOWN exact bytes 均继续保留。
- restart：broker、queues、maps、frontier 均不存在；当前代码无 durable restore。

#### 2.2 Cloud `CloudTaskRunActionLedger` + `CloudTaskRetainedActionState`

| map/字段 | 当前 key/value | cap/锁 | 当前状态与问题 |
|---|---|---|---|
| `records` | `ActionKey(taskRunId,businessActionKey) -> ActionRecord` | ledger object monitor；与 observation combined 10000 | ACTIVE current identity、operation、bound request/digest、recorded state/digest |
| `observationRecords` | `(taskRunId,businessActionKey,pausedRevision) -> ObservationRecord` | 同一 monitor/combined cap | 独立 typed identity/capability；CAPTURE/FACT only；无 renewal |
| `observationKeyUse` | ACTIVE `ActionKey -> retained observation revision count` | 同一 monitor | O(1) cross-mode conflict；当前永不 decrement |
| retained state `records` | `ActionAddress(phaseCode,actionSlot,occurrence) -> ActionRecord(operation,current handle)` | state `synchronized` + per-record monitor；无独立 cap | 由 shared ledger cap 间接限；当前永不删，跨 occurrence 线性增长 |

ACTIVE attempt 当前状态：`UNBOUND -> BOUND/UNRECORDED -> UNKNOWN -> one exact FINAL`，或
`BOUND/UNRECORDED -> FINAL`。非 UNKNOWN final immutable；相同 state+digest 重报幂等。只有 verified
`NOT_EXECUTED` 可 renewal：同 business key，attempt+1；CAPTURE/FACT 保持 actionId，INPUT 换 actionId；requestId
始终换，CAPTURE 换 captureId。Observation 同样允许 `UNKNOWN -> one exact FINAL`，但任何 state 都无 renewal。

当前 identity 有内部 `attempt`，但 request wire 没有 semantic address/occurrence/attempt；因此 DHXY 无法构造可证明
frontier。Action record 只保存 state+outcomeDigest，不保存 exact typed outcome object；Service port 直接返回 outcome，
没有“业务已消费”的 capability/state。

restart：action ledger、retained state、bound bytes、outcome state 全丢。H/K 也明确不声明 durable rehydration。

#### 2.3 DHXY `RemoteOperationLedger`

| map/字段 | 当前 key/value | cap/锁 | 当前状态与问题 |
|---|---|---|---|
| `requests` | `(operation,requestId) -> LedgerEntry` | 无 cap；`monitor`；永不删 | digest、shared future、bound scope、taskRun/window/stopEpoch/runRevision/operation |
| `terminalRequests` | `Set<RequestKey>` | 无 cap；同 monitor | 与 `ledgerRevision` 同 monitor 发布 terminal 可见性 |
| `inputActions` | `(windowId,actionId) -> (requestId,requestDigest)` | 无 cap；永不删 | 永久防 input actionId reuse |
| `boundScope` | 单 `RemoteTaskRunScope` | constructor/poller bind 一次 | 一个 ledger 只服务一个 tenant/user/device/clientSession poll loop |
| `ledgerRevision` | monotonic long | claim 和 terminal 各 `incrementExact` | resume quiescence CAS 使用；无 compaction revision |

`claim` 在本地 lifecycle/window 副作用门之前执行；OWNER 插入 future，DUPLICATE 等同 digest 时等待同一 terminal，
不同 digest/action reuse fail closed。`complete` 在同 monitor 内先写 terminal set + ledgerRevision，再在锁外完成 future。
当前没有 late-resolution 字段：本地真实 handler 只产生一次 terminal；Cloud timeout 后到达的本地 final 仍保存在该 future，
由 outcome POST 成为 Cloud late resolution。restart 后上述全部丢；本地 registry 也丢，旧 Cloud command 会因无 exact
registration 在副作用前拒绝，但不得据此宣称可恢复旧 run。

当前 lock order：`RemoteOperationLedger.monitor -> RemoteTaskRunRegistry.mutationLock` 已被 readiness
`withCurrentSnapshot -> materializeReady` 使用；M 必须沿用，不能反转。

### 3. 新 typed semantic address 与 request/outcome binding

新增双仓同构值类型：

```text
RemoteSemanticAddress {
  phaseCode: canonical non-blank string,
  actionSlot: canonical non-blank string,
  occurrence: non-negative long,
  attempt: non-negative int
}
```

它进入 `RequestContext`，属于 typed request 本体并参与现有 `requestDigest`。`RemoteCommandEnvelope`/DHXY
`RemoteGameCommand` 必须携带，缺失/显式 null/非法数值均 strict fail-closed。普通业务 caller 不能直接构造；ACTIVE 地址来自
`CloudTaskRetainedActionState.ActionAddress + identity.attempt`，observer 来自独立 observation identity 且 attempt 固定 0。

slot key 固定为：

```text
tenant/user/device/clientSession/taskRunId
+ windowId/nativeHandle/processId/playerIdentityEpoch
+ stopEpoch
+ operation
+ phaseCode/actionSlot
```

`runRevision`、`observationMode` 和 `occurrence/attempt` 是 exact entry 字段，不从 stable slot key 删除验证；frontier 跨同
taskRun pause/resume 保留，但每个 detailed entry 仍精确记录原 revision/mode。operation 放在 slot key，现有同业务 key
换 operation 的冲突仍由 retained ledger 拒绝；不得因 frontier 拆 key 而放宽。

### 4. Frontier 模型：无 gap、支持可信 NOT_EXECUTED renewal

每个 slot 只保留一个常量空间 frontier：

```text
CompactedFrontier {
  completedOccurrence: long,             // 初始 -1
  openOccurrence: Long | null,            // 只能 = completedOccurrence + 1
  compactedThroughAttempt: int,            // open 时初始 -1
  lastAckDigest: sha256 | null,             // 只证明最新一次重投
  lastRequestId/actionId/requestDigest/outcomeDigest,
  lastRunRevision,
  lastObservationMode,
  lastDisposition
}
```

两种 disposition：

- `OCCURRENCE_COMPLETE`：业务已把该 final 作为本次观察/动作结局消费；推进
  `completedOccurrence=occurrence`，清 open attempt。Observation 只允许这一种。
- `ATTEMPT_RETIRED_FOR_RENEWAL`：仅 ACTIVE + verified `NOT_EXECUTED`；保持 occurrence open，推进
  `compactedThroughAttempt=attempt`。现有 `renewAfterNotExecuted` 只有在此双仓 receipt 已完成后才能 mint
  `attempt+1`，occurrence 不变。无自动 renewal/retry。

允许的下一 identity 只有：

1. 新业务 occurrence：`occurrence=completedOccurrence+1, attempt=0`，且无 open occurrence；
2. 可信 renewal：`occurrence=openOccurrence, attempt=compactedThroughAttempt+1`，且上一 disposition 为
   `ATTEMPT_RETIRED_FOR_RENEWAL`。

任何 occurrence/attempt gap、回退或并发第二 active detail 直接拒绝，不保存 pending gap set。这样 frontier 空间是
`O(semantic slots)`，不是 `O(request history)`；旧 duplicate 在 detailed entry 删除后仍会在三侧最前门被
`occurrence < frontier` 或 `attempt <= frontier` 确定性拒绝，永远不进入机械执行。

### 5. Exact final-consumed wire

#### 5.1 Cloud -> DHXY `RemoteFinalConsumedAck`

```text
RemoteFinalConsumedAck {
  contractVersion: 1,
  tenantId, userId, deviceId, clientSessionId,
  taskRunId,
  runRevision,
  observationMode?: PAUSED_READ_ONLY,       // 缺失为 ACTIVE，NON_NULL canonical
  window: WindowBindingRef,
  stopEpoch,
  operation,
  semanticAddress: RemoteSemanticAddress,
  requestId,
  actionId,
  captureId?: string,                       // CAPTURE 必填，其余缺失
  requestDigest,
  outcomeDigest,
  executionState,                           // 必须非 UNKNOWN
  outcomeCode,
  disposition: OCCURRENCE_COMPLETE | ATTEMPT_RETIRED_FOR_RENEWAL,
  ackDigest
}
```

公式：

```text
ackDigest = hex(SHA-256(JCS(ack 去掉 ackDigest 字段)))
```

JCS/UTF-8/整数/enum/null omission 与现有协议完全同规则。ack 不包含 image bytes；`outcomeDigest` 已绑定除 capture
bytes 外的 outcome，CAPTURE 的 `imageSha256` 已绑定图片。ack bytes 首次生成后 retained，重投不得重建时间戳或新 ID。

#### 5.2 DHXY -> Cloud `RemoteFinalConsumedReceipt`

```text
RemoteFinalConsumedReceipt {
  contractVersion: 1,
  tenantId, userId, deviceId, clientSessionId,
  taskRunId,
  semanticAddress,
  ackDigest,
  applyStatus: APPLIED | DUPLICATE_APPLIED | REJECTED,
  appliedCompletedOccurrence,
  appliedOpenOccurrence?: long,
  appliedThroughAttempt,
  code,
  message,
  receiptDigest
}

receiptDigest = hex(SHA-256(JCS(receipt 去掉 receiptDigest 字段)))
```

Cloud receipt response `RemoteFinalConsumedReceiptAck` 只有
`ACCEPTED_COMPACTED / DUPLICATE_COMPACTED / REJECTED`、`ackDigest`、`receiptDigest`、code/message。Cloud
只有前两态才算本次 handshake 完成。wrong scope/ackDigest/frontier/receiptDigest 全部 REJECTED，不推进任何 map。

#### 5.3 producer/consumer owner

- producer：`CloudTaskRunActionLedger` 首次记录的 exact non-UNKNOWN typed outcome object，由
  `CloudTaskRunCommandExecutor` 从真实 broker 返回并保存对象 reference；业务代码不能构造可消费 token。
- exact business consumer：迁入 Cloud 的具体 Service（L2 为 `BattleRadarService`）完成模板/OCR/状态分支和既有
  GameContext/radar state 更新后，调用 operation-specific `CloudTaskServicePort.finalConsumed(action, exactOutcome,
  disposition)`。Port 只接受同 handle + 同 exact recorded outcome object；不能接受 raw requestId/digest/string key。
- transport owner：broker 把 ack 作为 `RemoteCommandPollResponse.Status.FINAL_CONSUMED` 控制项投递；不是
  `RemoteOperation`，不会进入截图/事实/input handler。
- local consumer：`RemoteCommandPollingLoop` 调 `RemoteOperationLedger.applyFinalConsumedAck(...)`，成功后通过
  `RemoteCommandTransport.submitFinalConsumedReceipt(...)` 回执；不调用业务 Service。
- Cloud compaction owner：package-private `RemoteFinalConsumptionCoordinator` 只由同 authority assembly 注入
  ledger/broker/Service port；host/Service 无 broker、poll、outcome、remove/map 权限。

#### 5.4 普通 outcome ack 为什么不能替代

当前 `RemoteCommandOutcomeAck` 在 local POST terminal 时立即由 broker 返回，此时 Cloud business caller 可能尚未收到、
解码或解释 outcome；它最多证明 transport terminal accepted/duplicate。其 wire 只有 status/code/requestId/message，缺
tenant/user/device/session、window、runRevision、stopEpoch、operation semantic address、occurrence/attempt、actionId、
requestDigest、outcomeDigest，也没有 local frontier apply 与 Cloud compact receipt。因此：

- transport accepted 不等于 business consumed；
- UNKNOWN 的普通 ack 之后仍可能有 late final；
- 普通 ack 无法证明应该推进哪个 semantic frontier；
- 普通 ack 无双阶段提交，直接据此删除会制造“一侧忘记、另一侧可重执行”。

所以普通 outcome ack 字节与语义保持不变，绝不被升级解释为 final-consumed。

### 6. 状态机与允许/拒绝矩阵

#### 6.1 Cloud attempt/retirement state

```text
UNBOUND
 -> BOUND_UNRECORDED
 -> OUTCOME_UNKNOWN
 -> OUTCOME_FINAL_UNCONSUMED
 -> BUSINESS_CONSUMED_NOTICE_PENDING
 -> LOCAL_FRONTIER_APPLIED
 -> COMPACTED_FRONTIER
```

`OUTCOME_UNKNOWN -> OUTCOME_FINAL_UNCONSUMED` 只接受 broker exact late non-UNKNOWN。任一 pending/unbound/dispatched/
UNKNOWN 状态都无 final-consumed API。Notice poll/receipt timeout停在 `BUSINESS_CONSUMED_NOTICE_PENDING`，保留 request、
typed outcome、bytes、IDs 和 notice bytes；下一次显式 finish 续送同 notice。

#### 6.2 矩阵

| 输入/场景 | 结论 | 状态变化 |
|---|---|---|
| ack before bound/final | `NOT_FINAL_REJECTED` | 无 |
| recorded UNKNOWN | `UNRESOLVED_REJECTED` | 无；exact bytes/late window 保留 |
| exact late final after UNKNOWN | 允许业务消费 | 先 `UNKNOWN -> exact final`，仍不自动 ack |
| duplicate ack，DHXY 尚未 apply | 返回/重投同 ack bytes | 不 mint、不改 occurrence |
| duplicate ack，DHXY 已 apply 且 frontier witness exact | `DUPLICATE_APPLIED` | 无 |
| ack occurrence/attempt 小于 frontier | `BELOW_FRONTIER` deterministic reject/duplicate-final-consumed | 绝不 claim/execute |
| ack 大于唯一 next | `GAP/OUT_OF_ORDER` | 无，不缓存 gap |
| attempt renewal disposition 但 outcome 非 ACTIVE NOT_EXECUTED | reject | 无 |
| observation 要求 renewal/attempt>0 | reject | 无，no-renewal 保持 |
| wrong tenant/user/device/session/taskRun | reject | 无，不泄露其它 scope |
| wrong window/nativeHandle/pid/epoch/stopEpoch | reject | 无 |
| ack revision 与 local detailed entry 不等 | reject | 无 |
| old revision exact terminal，当前 same stable run 已 resume | **允许只做 retirement** | 只压缩 exact old entry；不等于旧 command 可执行 |
| future revision 或当前 stable run identity drift | reject | 无 |
| pause/resume | frontier 与 retained state 同 taskRun 保留 | current command gate仍按 A/K；旧 PAUSED/ACTIVE bytes不复活 |
| STOPPED/COMPLETED exact final 已被 consumer 消费 | 允许 ack | receipt 后 compact；terminal cleanup 另见 §9 |
| broker missing，action detail仍在 | fail closed `BROKER_MISSING` | 不发 ack、不删 action |
| action/observation detail missing，broker仍在 | fail closed `ACTION_LEDGER_MISSING` | 不发 ack、不删 broker |
| DHXY local detail missing 且 frontier未覆盖 | `LOCAL_LEDGER_MISSING` | 不推进 frontier；Cloud全留 |
| Cloud detail已 compact，duplicate receipt命中 last witness | `DUPLICATE_COMPACTED` | 无 |

“old revision exact terminal 可退休”只是一条无副作用 GC 路径：必须与 local entry 原 revision 完全相等，且当前 registry
仍是 same stable taskRun/window/stopEpoch 或已 exact terminal。它不调用 ACTIVE/PAUSED command authorization，不会让旧
revision 业务执行。

### 7. 原子提交、锁序与 crash 矩阵

#### 7.1 Cloud 唯一锁序

K Repair #3 完成后，M 新增的 retirement 路径固定：

```text
per-action ActionRecord monitor
  -> RemoteFinalConsumptionCoordinator.retirementLock
    -> CloudTaskRunActionLedger monitor
      -> RemoteGameCommandBroker.stateLock
```

broker 不回调 action ledger；receipt endpoint 只在 broker `stateLock` 内标记 receipt future，不直接拿 action ledger。
正常 command 路径现有 record/gate/broker/action-ledger 调用不同时持有反向两把锁。Cloud final compact 在取得上述全部锁、
预验证三侧 exact witness 后执行不抛校验的 map mutation；其它线程看不到只删一半的 same-process 中间态。

K 的 slot `transitionLock`、H execution/owner locks、J turn lock 不参与 retirement。业务调用必须先在 K current slot + H
state projection 内完成业务消费，退出 H projection 后再进入 retirement，禁止形成
`H executionLock -> action record -> retirement` 与 resume 的反向环。

#### 7.2 DHXY 唯一锁序

```text
RemoteOperationLedger.monitor
  -> RemoteTaskRunRegistry.mutationLock（只做 exact same-run/terminal revalidate）
```

这与现有 readiness 顺序一致。`applyFinalConsumedAck` 在 monitor 内一次完成：验证 detailed terminal/frontier、写 frontier
witness、删除 `requests`/`terminalRequests`/exact `inputActions`、推进 `ledgerRevision`、写 retained receipt outbox。网络 POST
始终锁外；成功 ack 后再以 monitor CAS 清 receipt outbox。

#### 7.3 故障点

| crash/断线点 | 重启/重入结果 | 安全结论 |
|---|---|---|
| Cloud business 消费前 | 无 notice | 三账本全保留；不退休 |
| business state 已更新、notice 尚未登记 | 同进程异常 fail closed；进程 crash 后旧 run 不恢复 | 不重复执行；无 durable continuation 声明 |
| notice 已登记、DHXY 未收到 | Cloud保留明细+同 notice bytes | 下次显式 finish 重投；无新 ID |
| DHXY 收到，apply 前 crash | local frontier未推进 | Cloud不收 receipt，不 compact |
| DHXY apply 后、receipt POST 前 crash/断线 | local frontier + receipt outbox 保留于同进程 | poller显式重启续送同 receipt；Cloud仍全保留 |
| Cloud receipt endpoint 收到前网络丢 | 同上 | duplicate receipt deterministic |
| Cloud 已标 local-applied，compaction caller timeout | broker保留 receipt witness/action detail | 下一显式 finish 完成同一 compact |
| Cloud compact 后 response 丢 | local重投 receipt | Cloud frontier last witness 回 `DUPLICATE_COMPACTED` |
| Cloud process 在持双锁 mutation 中 crash | coordinator/broker/action/frontier全部同进程丢 | 整个 old run fail closed；没有一份 durable Cloud state可错误复活 |
| DHXY process restart | operation/frontier/registry全丢 | old taskRun无 registration，任何 command在副作用前拒绝；必须 STOP old run并建新 run |
| 双端 restart | 两边无恢复 | taskRunId不得复用；不宣称 durable frontier/rehydration |

没有任何故障点允许 Cloud 先忘记而 DHXY frontier 尚未应用。进程 crash 后的安全来自“旧 run 不恢复/不复用”，不是伪造
durable WAL。

### 8. Duplicate command 的三道 frontier 门

1. `CloudTaskRunActionLedger` acquire/renew 前：below/gap/并发 active occurrence 拒绝，业务层拿不到新 handle。
2. broker register 前：从 digest-bound semantic address 核 frontier；即使上层错误重交旧 request，也不入 queue。
3. DHXY handler claim 前：local frontier 核 occurrence/attempt；below frontier 返回 deterministic
   `NOT_EXECUTED/FINAL_CONSUMED`，绝不创建 OWNER claim或进入 window/input side-effect gate。

Cloud outcome endpoint 对携带已 compact semantic address 的迟到 duplicate outcome返回 deterministic
`DUPLICATE_FINAL_CONSUMED`，不把它当新 pending outcome。该判定只依赖 monotonic frontier，不依赖有限 requestId tombstone。

### 9. Bounded capacity、terminal cleanup 与审计

#### 9.1 空间上界

- 每 semantic slot：Cloud action side 1 个 frontier/当前 record；broker 1 个 frontier/当前 detailed request；DHXY 1 个
  frontier/当前 detailed request；每侧最多再有 1 个 pending ack/receipt witness。空间常量，与 occurrence 数无关。
- ACTIVE + OBSERVATION 继续共用 Cloud `10000` slot cap；一个 slot record无论跑多少 occurrence只计一份。
- broker global/owner `10000/1000` 改按“semantic slot record + 当前无地址 legacy retained record”计，pending仍 owner 64；
  input action map只保留当前未 compact attempt。route/global/queue cap与 A deadline语义不改。
- DHXY 新增与 broker owner 对称的 session-local slot cap `1000` 和 pending detail cap `64`；超限在 claim/side effect 前
  fail closed。它不是 eviction，且不得删旧 frontier换容量。
- unique phase/actionSlot 的无界滥用仍会在 slot cap fail closed；正常 BattleRadar 固定 7 slots，长期空间恒定。

#### 9.2 terminal taskRun cleanup

只有 coordinator/registry exact `STOPPED` 或 `COMPLETED`，且该 run 没有 unbound/pending/dispatched/UNKNOWN/
notice-pending/receipt-pending entry 时，才能删除该 run 的 slot frontiers和 receipt witnesses。顺序不要求跨进程事务，因为：

- terminal lifecycle fence永久禁止该 taskRun再 dispatch/执行；
- taskRunId全局不复用；
- 任一侧仍有 UNKNOWN 时 cleanup明确 `TERMINAL_BLOCKED_UNRESOLVED`，继续占 cap，不得 TTL 清除。

终态审计只在现有 retained terminal taskRun record挂一个 bounded summary：slot count、completed occurrence high-watermark
digest、unresolved count、cleanup status；不保留每 request tombstone。若现有 coordinator terminal record容量满，沿现有
fail-closed容量策略，不新增静默淘汰。

### 10. 调用 DAG

```text
trusted Task/radar state
  -> CloudTaskRetainedActionState retain typed ActionAddress
  -> CloudTaskServicePort.capture/fact/input
  -> CloudTaskRunCommandExecutor
  -> RemoteGameCommandBroker register/poll
  -> DHXY RemoteCommandPollingLoop
  -> LocalRemoteGameCommandHandler
  -> RemoteOperationLedger OWNER/duplicate + mechanical handler
  -> ordinary outcome POST
  -> broker terminal/late-final
  -> executor record exact typed outcome
  -> exact Cloud business Service interprets/applies final
  -> CloudTaskServicePort.finalConsumed(exact handle, exact outcome, disposition)
  -> RemoteFinalConsumptionCoordinator binds exact ack
  -> broker poll status FINAL_CONSUMED
  -> DHXY RemoteOperationLedger atomic frontier apply + receipt outbox
  -> HttpRemoteCommandTransport POST receipt
  -> broker exact receipt accept
  -> explicit Cloud finish path acquires retirement locks
  -> broker + action/observation detail compact + both Cloud frontiers advance
  -> next occurrence or trusted NOT_EXECUTED attempt becomes mintable
```

业务 Service始终看不到 raw request、poll、outcome endpoint、broker ledger、remove/frontier map；DHXY handler也不解释模板、
phase或 successor。

### 11. Exact 两仓文件写集

下面是本设计的完整实施闭包；任何扩大必须先回本报告写 conflict。M 不在本轮修改其中任何文件。

#### 11.1 Cloud Brain

**New（5）**

1. `remote/RemoteSemanticAddress.java`
2. `remote/RemoteFinalConsumedAck.java`
3. `remote/RemoteFinalConsumedReceipt.java`
4. `remote/RemoteFinalConsumedReceiptAck.java`
5. `remote/RemoteFinalConsumptionCoordinator.java`

**Modify（15）**

1. `remote/RequestContext.java`：digest-bound semantic address。
2. `remote/RemoteCommandEnvelope.java`：poll wire透传 address。
3. `remote/RemoteCommandPollResponse.java`：`IDLE/COMMAND/FINAL_CONSUMED` closed union。
4. `remote/RemoteProtocolDigests.java`：request + ack + receipt canonical digest。
5. `remote/OutcomeCode.java`：`FINAL_CONSUMED`/compacted duplicate typed code。
6. `remote/CloudTaskRunExecutionGate.java`：从 ledger identity投影 address，不接 caller raw字段。
7. `remote/CloudTaskRunActionLedger.java`：exact outcome object、occurrence/attempt frontier、consume/compact状态；A observer
   maps/no-renewal/combined cap冻结。
8. `remote/CloudTaskRetainedActionState.java`：slot frontier、consume/renew顺序、compact后删除 detail。
9. `remote/CloudTaskServicePort.java`：operation-specific exact `finalConsumed`，无 raw ID/key API。
10. `remote/CloudTaskRunCommandExecutor.java`：记录 exact outcome object/address，不解释业务。
11. `remote/RemoteGameCommandBroker.java`：delivery union、notice/receipt状态、broker frontier、双账本 compact primitive；A
    observation deadline/dispatch分支冻结。
12. `remote/RemoteTaskRunRoutes.java`：authenticated receipt endpoint，只暴露 opaque route。
13. `remote/CloudTaskRunAuthorityAssembly.java`：同 assembly注入唯一 retirement coordinator；基于 K最终 runtime，不另造 authority。
14. `CloudBrainServer.java`：新增 receipt route path参数；不启动新 server/线程/host。
15. `remote/RemoteCommandOutcomeEnvelope.java`：透传 digest-bound semantic address，使 compacted late duplicate可按 frontier
    deterministic ack；typed outcome业务字段不改。

`CommonOutcome.java` 只有在实现选择把 address放入 typed common而非 transport envelope时才会产生第16个 Modify；本设计
固定 **不选** 该扩大：address在 request digest与 transport outcome envelope中双绑定，outcomeDigest继续保持协议 v1公式不变。

#### 11.2 DHXY

**New（4）**

1. `cloud/remote/RemoteSemanticAddress.java`
2. `cloud/remote/RemoteFinalConsumedAck.java`
3. `cloud/remote/RemoteFinalConsumedReceipt.java`
4. `cloud/remote/RemoteFinalConsumedReceiptAck.java`

**Modify（12 Java + 1 doc）**

1. `RemoteGameCommand.java`：required semantic address。
2. `RemoteGameOutcomeEnvelope.java`：echo semantic address供 compacted late outcome分类。
3. `RemoteProtocolDigests.java`：request + ack + receipt canonical digest；普通 outcomeDigest公式不改。
4. `RemoteCommandPollResponse.java`：closed union control payload。
5. `RemoteCommandPollStatus.java`：新增 `FINAL_CONSUMED`。
6. `RemoteCommandTransport.java`：typed `submitFinalConsumedReceipt`。
7. `HttpRemoteCommandTransport.java`：strict decode/validate/receipt endpoint；unknown/null仍走现有 DESERIALIZATION分类。
8. `RemoteCommandPollingLoop.java`：控制项先 frontier apply，锁外 POST receipt；无第二线程/自动 retry。
9. `RemoteOperationLedger.java`：semantic detail、frontier、receipt outbox、atomic compact、terminal cleanup。
10. `LocalRemoteGameCommandHandler.java`：claim前 frontier门；现有三道 window/revision/side-effect门不削弱。
11. `RemoteOutcomeCode.java`：`FINAL_CONSUMED`/compacted duplicate typed code。
12. `RemoteCommandOutcomeAck.java`：**不改字段/语义**，只补注释明确非 business-consumed（若父级要求零 churn可从写集删除；
    实现不依赖此文件）。
13. `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`：semantic address、final-consumed wire/digest/frontier/
    restart fail-closed合同。

#### 11.3 不触碰

- A 其余 observer files、coordinator PAUSED authorization和 DHXY observation enum/schema分类逻辑；
- B artifact/template 7文件与 packaged assets；
- J `CloudTaskTurnCoordination*`；
- H `CloudGameContextStateOwner`；
- L1 `CloudBattleRadarProperties*`、`CloudMiniMapCoordinateReadability`；
- 任何 Task/Service业务 phase、resources/templates、Maven/tests。

### 12. 与 A/K/B/J/L 的顺序/零交叉证明

| owner | 交叉 | 硬门 |
|---|---|---|
| A deadline Repair | `RemoteGameCommandBroker` 直接交叉；A还冻结 `RequestContext`/Envelope/Gate/Ledger observer语义 | A 必须先 `Implementation APPROVED`；M实施前重读A最终hash，保留 wall-clock observer deadline和三道PAUSED门 |
| K current-slot | Assembly、ActionLedger、RetainedState、ServicePort、Executor、Broker 直接交叉 | K Repair #3先 DESIGN+Implementation APPROVED；M只基于K最终generation/runtime实施，顺序固定 `A -> K -> M` |
| B artifact | 零文件交叉 | B可并行，但M不调用artifact/path/template API |
| J turn | 零文件/锁交叉 | J可并行；retirement不取得turn lock、不改变yield/park |
| L1 | 零文件交叉 | L1已APPROVED，不改三叶子 |
| L2 BattleRadar | L2消费M新port/frontier合同 | 固定 `M Implementation APPROVED -> L2 Repair/Implementation`；L2不得自造ack/frontier |

M 开始 Java 前必须再次确认 A/K 报告均有明确父级 Implementation `APPROVED`，并记录最终 broker/ledger/assembly
hash；若 K 修改了上述 API，先回本报告 Repair，不用旧设计整文件覆盖。

### 13. 建议最小实施波次

1. **M1 双仓 semantic wire + digest**：只加 address/DTO/digest/closed poll union，receipt endpoint仍 dormant；双仓 compile。
2. **M2 Cloud state/frontier**：在 A+K最终源码上实现 action/observation + broker frontier、exact outcome token、retirement
   coordinator；不改 server/caller。
3. **M3 DHXY local frontier + control transport**：ledger atomic apply、poller control、receipt transport；随后接 Cloud route。
4. **M4 same-process integration closure**：只接 authority assembly/route，保持 host/Task/Service caller dormant；双仓 compile/package。
5. **L2 后续波次**：BattleRadar固定7 slot接入并逐 final消费；不在M波迁业务类。

每波 Java变更后按父级规则 Cloud fresh `mvn -q clean package`、DHXY `mvn -q -DskipTests compile`；M Worker本轮不运行。
no-local-test保持，不新增/恢复tests。

### 14. 风险、自审与父级决定点

| 风险 | 级别 | 处置 |
|---|---|---|
| A/K共享文件并发覆盖 | P1 | A->K->M硬顺序；hash+scoped diff；不得并行写 |
| business state更新后 notice失败 | P1 | action state记 `BUSINESS_CONSUMED_NOTICE_PENDING`，同 outcome不再重新解释；显式finish续送 |
| finite tombstone无法挡老duplicate | P1 | monotonic occurrence+attempt frontier，below-frontier三道拒绝 |
| local apply后Cloud未compact | P1 | Cloud全留，receipt exact重投；安全但暂占cap |
| restart误称可恢复 | P1 | 双端旧run fail closed/新taskRun；无DB/WAL/rehydration声明 |
| frontier unique-slot滥用 | P2 | combined slot cap，无eviction；capacity fail closed |
| old revision retirement被误当授权 | P1 | 只允许 exact local terminal GC，不经过command side-effect gate，不返回业务truth |
| ordinary outcome ack被误用 | P1 | API/DTO分离，普通ack无consumer调用面且文档明确禁止 |

自审：

- P0=0。
- 设计内未留未决语义 P1/P2；A/K是实施前外部顺序门，不是由M越权规避的事项。
- 没有 TTL/LRU、自动 retry、数据库、凭据、付费服务、生产切换或 durable claim。
- ACTIVE/PAUSED combined quota、cross-mode conflict、observer no-renewal、三道 revision/window/stop gate保持。
- 没有新增 capture、业务 verification、phase、timer、click/navigation、fallback、park/yield或业务 cleanup。
- **无已批准业务差异；按基线等价迁移。**

**Worker M 状态：Design #1 已提交，停止并等待父级 DESIGN REVIEW；未获批准前不实施。**

## Parent Design Review #1 - BLOCKED - 2026-07-12

父级已对照当前 Cloud action/observation ledger、broker route/poll/capacity、DHXY operation ledger/registry/poller 以及 M 的 wire/锁序/crash 矩阵复核。结论：**BLOCKED，P0=0，P1=4，P2=2**。锁序 `RemoteOperationLedger.monitor -> RemoteTaskRunRegistry.mutationLock` 与现有 readiness 路径一致，本项通过；其余如下。M 不得开始 Java。

### P1-1：business update 与 consumed notice 登记之间仍有可重复解释窗口

- 证据：L317-L321 规定 Service 先完成 GameContext/radar 更新，再调用 `finalConsumed`；状态机 L348-L359 只有进入 port 后才从 `OUTCOME_FINAL_UNCONSUMED` 转 `BUSINESS_CONSUMED_NOTICE_PENDING`。L427/L611 仅声称“fail closed/记 pending”，没有定义状态在业务 mutation 前如何取得。
- 影响：业务状态已部分或全部更新后，若线程异常发生在 `finalConsumed` 调用前，同一进程仍保留 `OUTCOME_FINAL_UNCONSUMED`，下一次调用可再次解释同 exact outcome；这不满足“exactly once business consumed”，对计数/FIFO/phase mutation 会重复生效。
- 返修条件：设计一个非 public、同 `ActionRecord` owner 的 consume transaction/permit，例如 `consumeFinal(action, exactOutcome, callback)`：在业务 callback 前原子进入 `BUSINESS_CONSUMING`，callback 不得持 broker/retirement 锁；成功后才进入 notice-pending，异常则进入不可自动重放的 `BUSINESS_CONSUMPTION_UNKNOWN` 并 fail closed。若具体 Service 采用幂等 applied marker，必须列 marker 与业务状态的同锁/同对象原子关系。不能继续用“先改业务、后登记”并宣称 exactly once。

### P1-2：PAUSED observation 没有 occurrence 权威，当前 identity 无法投影所述无-gap frontier

- 证据：L202-L203 说 observer address 来自“独立 observation identity”，但当前 `CloudTaskRunActionLedger.acquireObservation` 只按 `(taskRunId,businessActionKey,pausedRunRevision)` mint，identity 没有 phase/actionSlot/occurrence；ACTIVE 的 occurrence 则在 `CloudTaskRetainedActionState.ActionAddress`。L244-L250 又要求 occurrence 必须严格 `completed+1`、无 gap。
- 影响：不能证明不同 paused revision 对应同一 semantic slot 的哪个 occurrence，也不能防 caller 通过无限 businessActionKey 制造 slot；compaction 后 observation 的 cross-mode index、combined cap 与下一 occurrence mint 都没有 authority owner。
- 返修条件：Repair #1 明确 observation 的 canonical phase/actionSlot、occurrence owner、pausedRevision 到 occurrence 的映射和 mint API；caller 不能提供 raw occurrence。列出 observation record compact 时 `observationKeyUse`/combined quota 的原子减账与 frontier 保留规则，并证明 ACTIVE/PAUSED cross-mode conflict 仍双向 fail closed。

### P1-3：FINAL_CONSUMED control 没有独立的有界投递/公平性/重投状态机

- 证据：L81-L87/L322-L325 只说把 ack 放入“现有 client route poll”；现有 broker route 是容量 64 的 `ArrayBlockingQueue<PendingCommand>`，poll 只取 command，owner pending/route/queue 都有独立计数。设计未说明 control 是否占 command queue、队列已满时如何保留、command 与 control 的选择优先级，以及 response/receipt 丢失后的 exact send claim/CAS。
- 影响：把 control 塞入满 command queue会阻止 final consumption，slot 无法 compact/renew；不计入容量则可无界；无公平性可能饿死 command 或 control。DHXY receipt POST 失败会让当前 poller按既有 outer catch停止，若 restart 没复用同 ledger/outbox，receipt 丢失。
- 返修条件：定义 per-route bounded retained control lane（或证明与 command queue 共用仍无死锁），给出容量、计数、选择公平性、一个 ack/receipt 的 send-handle/CAS 状态、response loss/POST loss/显式 poller restart 路径；网络 I/O 全在锁外。必须明确 restart 复用同 `RemoteOperationLedger` 实例，仅进程内续送，不暗示进程重启恢复。

### P1-4：semantic address 的 outcome echo 未形成完整完整性校验

- 证据：L541-L542 决定不把 address 放入 typed `CommonOutcome`/ordinary `outcomeDigest`，只放 transport outcome envelope；但 L538 只写“透传”，未要求 outcome endpoint 在 accepted/late/compacted duplicate 三条路径都把 echo address 与 pending digest-bound request address 精确比较。
- 影响：若只按 envelope address选 frontier、而 digest仍是 v1，错误或恶意 echo 可把一个真实 outcome 归到另一个 semantic slot，导致错误 compact 或错误 duplicate 分类。
- 返修条件：逐 endpoint 分支写验证顺序：scope/operation/requestId -> pending/compacted witness -> exact semantic address equality -> requestDigest/outcomeDigest；compacted late duplicate 必须同时匹配 retained frontier witness，不能仅凭 caller echo。若做不到，则 address 必须纳入 typed outcome/digest，并同步双仓 schema。

### P2-1：`落盘`措辞与实际 non-durable 内存状态冲突

- 证据：L83-L84、L430 使用“落盘”，L434-L439 又明确双端无 WAL/DB、process crash 全丢。
- 影响：会让实施者/运维误认为 frontier/receipt 可跨进程恢复。
- 返修条件：统一改为“同进程原子发布/retained in-memory”，在 wire、crash、terminal cleanup 三处明确 process restart 后不可恢复且旧 run 不复用。

### P2-2：删除无效的 comment-only churn

- 证据：L566-L567 已说明 `RemoteCommandOutcomeAck.java` 实现不依赖。
- 影响：扩大 dirty write set，增加与其它在途改动冲突，未提供协议强制力。
- 返修条件：从实施写集移除；禁止普通 outcome ack 升级为 business-consumed 的规则只写 schema/本报告与真正的 DTO/API 边界。

### 下轮门禁

M 只追加 `Internal Worker M - Design Repair #1`，先关闭四个 P1 与两个 P2；保持 `A -> K -> M -> L2` 顺序，A/K 未 Implementation APPROVED 前仍不得改 Java。Repair 必须给出 consume transaction、observation occurrence owner、control lane/outbox 状态机和 outcome-address 验证矩阵。**无已批准业务差异；按基线等价迁移。**

## Internal Worker M - Design Repair #1 - 2026-07-13

本段只修复 `Parent Design Review #1 - BLOCKED` 的四个 P1、两个 P2，并作为 Design #1 对应段落的规范性替代；
未在本轮修改 Java/Maven/resources/tests、未运行 build、未启动任何运行面。实现顺序仍固定
`A -> K -> M -> L2`，A/K 未获 `Implementation APPROVED` 前 M 不实施。

### R1. business-consumed 改为不可重放 consume transaction（关闭 P1-1）

Design #1 L317-L321 的“业务先更新、再调用 `finalConsumed`”路径撤回。唯一允许的业务入口改为同
`CloudTaskRetainedActionState.ActionRecord` owner 的 package-private API：

```text
consumeFinal(actionHandle, exactRecordedOutcome, callback) -> FinalConsumptionHandle
```

- `callback` 包含该 final 的全部业务解释和 mutation，包括计数、FIFO、phase、GameContext/radar state 更新；调用
  `consumeFinal` 前只允许无副作用读取，不能先写业务状态。
- `actionHandle` 必须是 ledger mint 的 exact handle；`exactRecordedOutcome` 必须与 ActionRecord 保存的 non-UNKNOWN
  typed outcome 为同一对象 reference，并再次校验 request/action/address/digests。raw requestId、digest、string key
  不能取得 consume permit。
- callback 返回 typed `OCCURRENCE_COMPLETE` 或 `ATTEMPT_RETIRED_FOR_RENEWAL`；后者仍只允许 ACTIVE + verified
  `NOT_EXECUTED`。返回 null/非法 disposition 按 callback 失败处理，不生成 notice。

ActionRecord 的规范状态机改为：

```text
OUTCOME_UNKNOWN
  -> OUTCOME_FINAL_UNCONSUMED
  -> BUSINESS_CONSUMING(consumeGeneration, reservedControlHandle)
       -> BUSINESS_CONSUMED_NOTICE_PENDING(exactAckBytes)
       -> BUSINESS_CONSUMPTION_UNKNOWN(consumeGeneration, failureClass)
  -> LOCAL_FRONTIER_APPLIED
  -> COMPACTED_FRONTIER
```

取得 permit 的原子步骤：

1. 按既定 Cloud 锁序取得 `ActionRecord -> retirementLock -> ActionLedger -> broker.stateLock`；验证 exact final、
   当前状态、frontier、scope/window/revision/stopEpoch，并先保留一个有界 control reservation。
2. 若 control retained budget 已满，返回 `CONTROL_CAPACITY`，不进入 `BUSINESS_CONSUMING`，callback **不执行**。
3. 同一临界区写入唯一 `consumeGeneration` 和 `BUSINESS_CONSUMING`，随后释放全部锁。
4. callback 在锁外执行；它不得持有/取得 broker 或 retirement 锁。业务 Service 可在 callback 内使用其既有 H/K
   业务状态锁，但退出 callback 后才进入 retirement 路径，不形成反向锁序。
5. callback 正常返回后，按同一锁序 CAS exact `consumeGeneration`，验证 disposition，生成并 retained 一次性的
   ack bytes，转 `BUSINESS_CONSUMED_NOTICE_PENDING`。lane 暂满只影响排队，不撤销已消费事实。
6. 任何从 callback 逃逸的 `RuntimeException`/`Error`/interruption，或返回后 CAS/decision 校验失败，都在 finally
   把仍匹配 generation 的记录转为 `BUSINESS_CONSUMPTION_UNKNOWN`，释放未使用的 control reservation；异常继续传播。

`BUSINESS_CONSUMING` 与 `BUSINESS_CONSUMPTION_UNKNOWN` 都是不可重放状态。后续同 action/outcome 的任何
`consumeFinal` 只返回 fail-closed `CONSUMPTION_IN_PROGRESS/CONSUMPTION_UNKNOWN`，绝不再次调用 callback；UNKNOWN
也不能 notice、compact、renew 或 terminal cleanup。这样同进程异常最多产生“业务 mutation 是否完成未知并阻断旧 run”，
不会把同 exact outcome 再解释一次。进程终止时内存状态全部消失，安全条件仍是旧 taskRun 不恢复、不复用，而不是恢复该事务。

### R2. PAUSED canonical slot 与 occurrence authority（关闭 P1-2）

#### R2.1 唯一 authority 与 mint API

`CloudTaskRetainedActionState` 的 per-slot ActionRecord 是 ACTIVE 与 PAUSED 共用的 semantic occurrence authority；
`CloudTaskRunActionLedger` 只消费 authority mint 的 opaque identity，不再从 `businessActionKey` 推导 slot/occurrence。

M 的 API 只接受由 K 最终 current-slot authority 产生的非 forgeable `CanonicalSemanticSlot` handle：

```text
CanonicalSemanticSlot {
  operation,
  phaseCode,
  actionSlot,
  modeOwner: ACTIVE | PAUSED_READ_ONLY
}

acquireObservation(pausedCapability, canonicalSlotHandle)
```

- handle 的构造器非 public；`phaseCode/actionSlot` 来自 authority 注册的 canonical slot，不接受 observer caller 的
  raw string。L2 后续只能把其固定七个 radar slot 映射到该 authority，不能按 capture/request 动态造 slot。
- caller 不提供 `occurrence`、`attempt`、`pausedRunRevision` 或新的 `businessActionKey`。revision 只从 A 的 exact
  `CloudPausedReadOnlyObservationContext` capability 读取，attempt 永远由 authority 固定为 0。
- slot key 仍包含完整 tenant/user/device/session/taskRun/window/stopEpoch/operation/phase/actionSlot；revision 与 mode
  是 detail 字段，不能绕过 A 的 current PAUSED capability、window/revision/stop 三道门。

每个 PAUSED slot record 只保留常量空间：

```text
modeOwner
completedOccurrence
currentObservation?: { pausedRunRevision, occurrence, attempt=0, exactIdentity }
lastPausedRunRevision
lastCompactedWitness
```

mint 规则：

1. exact same capability instance + same paused revision + same canonical slot 在 detail 尚存在时返回同 identity。
2. 第一次使用该 slot 时，authority 原子 mint `occurrence=completedOccurrence+1, attempt=0`；caller 无法选择数值。
3. 新 paused revision 只有在旧 detail final-consumed/compacted、A capability 为 current 且 revision 严格大于
   `lastPausedRunRevision` 时，才 mint 下一个 occurrence。revision 可以按 lifecycle 跳号，但 semantic occurrence
   始终只加 1，不产生 gap。
4. 同 revision 在 compact 后再次 acquire 返回 `OBSERVATION_ALREADY_COMPACTED`；较旧 revision 返回
   `STALE_PAUSED_REVISION`。不保留无界 revision map，只保留 current detail 与 frontier 的 last revision/witness。
5. observation 任何 renewal、attempt>0、并发第二 detail 都 fail closed。

#### R2.2 quota 减账、frontier 保留与 cross-mode

同一 observation compact 临界区仍使用已通过的锁序：per-slot ActionRecord monitor 在最外层，随后
`retirementLock -> CloudTaskRunActionLedger monitor -> broker.stateLock`。所有校验先完成，以下 mutation 块不抛校验异常：

1. 推进 ActionRecord 的 `completedOccurrence` 和 retained compacted witness；frontier/`modeOwner` 保留。
2. 删除 exact `observationRecords[taskRun + canonicalSlot + pausedRevision + occurrence]`。
3. 对 exact canonical slot 的 `observationKeyUse` 做一次 `-1`；只在值从 1 变 0 时删除计数项，underflow/缺项
   在预验证阶段 fail closed。
4. ACTIVE+PAUSED combined **detail quota** 同步减 1；owner/global retained **slot quota** 不减，因为 frontier 仍占
   一个 bounded slot，直到 §9 terminal cleanup。
5. broker exact detail、action/observation detail 与两侧 frontier 在同一 same-process retirement 临界区完成，不让其它线程
   观察到只减一侧 quota 的中间态。

`observationKeyUse` 只表示当前 observation detail 数，不再承担永久 mode authority。双向 cross-mode fail-closed 由 retained
slot frontier 的 `modeOwner` 保证：PAUSED slot 即使 detail compact、use count 已归零，ACTIVE acquire 仍因
`modeOwner=PAUSED_READ_ONLY` 被拒；反向同理。`modeOwner` 只在 exact terminal taskRun cleanup 与整个 slot frontier 一起删除，
因此 compact 不会把 PAUSED slot 偷换成 ACTIVE，也不会通过无限 businessActionKey 绕过 cap。

### R3. bounded control lane 与同进程 receipt outbox（关闭 P1-3）

#### R3.1 Cloud per-route control lane

FINAL_CONSUMED 不进入现有 command `ArrayBlockingQueue<PendingCommand>`，也不占 command pending 64。每个已认证
`RemoteClientScope` route 增加独立 retained control lane：

- per-route control lane capacity = `64`；
- owner/global retained control reservations = `1000/10000`；
- 每个 semantic detail 最多一个 reservation/ack；未排队的 notice 也计 retained control budget；
- command queue capacity、owner pending、global/owner route cap均保持 A 的现值；control 不创建绕过 route cap 的新 route。

业务 consume 前先取得 reservation，所以 callback 成功后不会产生无界孤儿 notice。lane 64 已满时，ack 保持
`RETAINED_NOT_QUEUED`，后续显式 `finishFinalConsumption` 再尝试 admission；不删除 detail、不自动定时重试。

每个 retained ack 的状态机：

```text
RESERVED
  -> RETAINED_NOT_QUEUED
  -> QUEUED
  -> SENDING(sendGeneration, ackDigest)
  -> QUEUED                         // route finally CAS；exact bytes 可再投
  -> LOCAL_APPLIED(receiptDigest)
  -> COMPACTED                      // coordinator 同步删 lane/detail

callback failure: RESERVED -> RELEASED + BUSINESS_CONSUMPTION_UNKNOWN
```

poll 使用 route-local availability signal 唤醒，但选择在 `broker.stateLock` 内完成；网络/JSON 写出全部在锁外。若 command
与 control 同时可取，route 保存 `nextLane` 并严格交替 `COMMAND -> CONTROL -> COMMAND`；某 lane 为空时取另一 lane，只有
实际 claim 才翻转。故持续 control 不饿死 command，持续 command 也不饿死 retirement。

control poll claim 从 lane 取 token并 CAS `QUEUED -> SENDING(sendGeneration)`，返回包含 opaque send handle 与首次生成的
exact ack bytes。route endpoint 在 response object 构造的 finally 调 `finishControlSend(handle)`：若 receipt 尚未 compact，
exact generation CAS 回 `QUEUED` 并把同 token 放回刚释放的 lane slot；若 receipt 已并发 compact，CAS no-op。服务端响应
在网络中丢失时 retained entry 从未删除，下一次显式 poll 仍得到同 bytes/ackDigest；没有 lease timer、时间戳重建或新 ID。

#### R3.2 DHXY bounded receipt outbox 与显式 restart

`RemoteOperationLedger` 的 session-local receipt outbox capacity 固定为 `64`，每 semantic slot 最多一个 exact receipt。
`applyFinalConsumedAck` 在同 monitor 内先预留 outbox slot；满时返回 `RECEIPT_OUTBOX_CAPACITY`，不推进 local frontier、不删
local detail。预留成功后才原子写 frontier witness、删除 exact local request/action detail并保存首次生成的 receipt bytes。

每个 receipt 状态：

```text
READY
  -> SENDING(sendGeneration, receiptDigest)
  -> removed                       // ACCEPTED_COMPACTED / DUPLICATE_COMPACTED
  -> READY                         // POST/response outcome uncertain
  -> REJECTED_RETAINED             // typed permanent reject；旧 run fail closed
```

poller 每轮最多发送一个 READY receipt，再执行一次普通 poll，形成 receipt/poll 1:1 公平；claim/CAS 在 ledger monitor 内，
HTTP POST 在锁外。POST 或 response 丢失时 exact handle CAS 回 READY 后让异常沿现有 outer catch 停止 poller，不增加 hidden
retry loop。父级/既有 lifecycle 只有显式再次调用同一 `RemoteCommandPollingLoop.start()` 才续送；该 loop 必须复用其构造时
持有的同一个 `RemoteOperationLedger` 实例，restart 第一轮先 claim retained READY receipt，不能 new ledger/outbox。

若 Cloud 已 compact 但 receipt response 丢失，同进程显式 restart 重投同 receipt，Cloud retained frontier witness 返回
`DUPLICATE_COMPACTED`，local CAS 删除 outbox。DHXY **进程重启**会丢 ledger/outbox，绝不称为 restart recovery；旧 taskRun
必须 fail closed/STOP，新 taskRunId 不复用。

### R4. outcome echo address 三路径完整性（关闭 P1-4）

`RemoteCommandOutcomeEnvelope.semanticAddress` 继续是 required transport echo；requestDigest 包含 address，普通 typed
`CommonOutcome` 与 protocol-v1 `outcomeDigest` 公式仍不扩大。安全前提改为 endpoint 绝不按 caller echo 选 frontier。

统一验证顺序：

```text
authenticated scope + operation + requestId
  -> server-owned pending detail OR compacted request witness lookup
  -> exact semanticAddress equality against that server-owned record
  -> exact requestDigest equality
  -> typed payload correlation + canonical outcomeDigest verification/equality
  -> branch-specific state transition
```

broker 保留 bounded `compactedRequestWitness`，每 semantic slot 只保留最新一个
`(scope, operation, requestId, semanticAddress, requestDigest, outcomeDigest, executionState, outcomeCode)`，与 slot frontier 同寿命；
索引 key 为服务端 `(scope, operation, requestId)`，不是 caller address。pending 与 compacted witness 同时命中属于内部不变量破坏，
直接 fail closed。

| endpoint 路径 | server-owned lookup | address/digest 要求 | 结果 |
|---|---|---|---|
| first accepted terminal | exact pending request | echo address = pending request address；requestDigest exact；typed outcome/outcomeDigest canonical exact | 只在全部通过后 `completeTerminal` |
| exact duplicate terminal | exact pending + recorded terminal | 先做同一 address/requestDigest 校验，再要求 outcomeDigest = recorded terminal | ordinary `DUPLICATE`，不改 frontier |
| late non-UNKNOWN after UNKNOWN | exact pending + recorded UNKNOWN | 先做同一 address/requestDigest/typed digest 校验，再 CAS 唯一 late resolution | `ACCEPTED_LATE_FINAL`；进入可 consume final，绝不自动消费 |
| compacted latest duplicate | 按 scope/operation/requestId 命中 retained compacted witness | echo address、requestDigest、outcomeDigest 必须同时等于 witness | `DUPLICATE_FINAL_CONSUMED`，不复建 detail |
| caller echo 指向其它 slot | pending/witness 的 server address 不等 | 在任何 terminal/late/frontier mutation 前 reject | `SEMANTIC_ADDRESS_MISMATCH` |
| 只有 caller address 可命中 frontier、无 requestId witness | 无 server-owned exact witness | 不接受 caller 自证；即使 occurrence below frontier也不称 exact duplicate | `UNKNOWN_COMPACTED_REQUEST`，三道 claim gate仍禁止执行 |
| 比 retained latest witness 更老的 compacted outcome | requestId witness已被后续 occurrence替换 | 无 exact server witness | fail-closed reject，不推进/回退 frontier |

因此 accepted、late-final、compacted duplicate 三条成功路径都先绑定 server-owned address；compacted 路径不能只凭 caller echo。
若未来需要对任意历史 compacted outcome返回“exact duplicate accepted”，必须另开协议变更把 address纳入 typed outcome/digest或引入
durable history；不在 CR271/M 范围内。

### R5. non-durable 术语修正（关闭 P2-1）

Design #1 L83-L84 与 L430 的“落盘”措辞撤回，统一替换为 **同进程原子发布 / retained in-memory**：

- wire：receipt 只证明 DHXY 当前 `RemoteOperationLedger` 实例已原子发布 frontier 与 retained in-memory outbox；不证明磁盘持久化。
- crash：同进程 poller restart 可复用同 ledger/outbox；任一 Java process restart 都丢失其 frontier、witness、send handle和 outbox，
  不执行 rehydrate，不恢复旧 run。
- terminal cleanup：只清当前进程 retained maps；taskRunId 永不复用，process restart 后旧 run 必须 STOP/fail closed，再创建新 run。

本设计无 WAL/DB/file persistence，也不对进程终止提供 exactly-once recovery。consume transaction 的 exactly-once 约束仅是
同进程内“callback 一旦 claim 就不可自动重放”；进程 crash 的安全仍来自旧 run 不复用。

### R6. 修正实施写集（关闭 P2-2）

Design #1 §11.2 的 `RemoteCommandOutcomeAck.java` comment-only 项完整移除，禁止为说明性注释制造 dirty churn。DHXY 写集
改为 **Modify（11 Java + 1 doc）**：原 1-11 Java 项保持，protocol schema doc 为第 12 项；普通 outcome ack 不升级为
business-consumed 的约束只写在 schema、本报告以及真正的 `RemoteFinalConsumed*` DTO/port 边界。

本 Repair 未引入额外文件：consume callback/handle 是 `CloudTaskServicePort`/ActionRecord 的 package-private nested contract；
canonical slot/frontier 使用 `CloudTaskRetainedActionState`；control send state使用 broker nested record；receipt send state使用
`RemoteOperationLedger` nested record。Cloud Design #1 的 New 5/Modify 15 保持，DHXY New 4/Modify 11 Java + 1 doc。

### R7. 修订后的调用顺序、故障结论与门禁

```text
exact recorded non-UNKNOWN outcome
  -> package-private consumeFinal claim + bounded control reservation
  -> BUSINESS_CONSUMING（释放 broker/retirement locks）
  -> callback 内完成全部 business mutation
  -> exact ack retained in-memory + bounded control lane
  -> fair poll + send-handle/CAS 投递
  -> DHXY atomic frontier/detail/outbox mutation
  -> receipt send-handle/CAS，HTTP 锁外
  -> Cloud exact receipt witness
  -> same-process broker/action simultaneous compact
  -> next occurrence / trusted ACTIVE NOT_EXECUTED renewal mintable
```

| 故障点 | retained 状态 | 结论 |
|---|---|---|
| consume claim 前 | `OUTCOME_FINAL_UNCONSUMED` | 无 business mutation，可稍后显式重试 claim |
| callback 中异常/部分 mutation | `BUSINESS_CONSUMPTION_UNKNOWN` | 同进程不可重放、不可 compact/renew；旧 run fail closed |
| callback 成功、lane 暂满 | exact ack `RETAINED_NOT_QUEUED` | bounded reservation已占；显式 finish admission，不重做 callback |
| control response 丢失 | ack 回到/保持 `QUEUED` | 下一 poll同 bytes；command/control严格交替 |
| local apply后 receipt POST失败 | frontier + READY outbox retained in-memory | 当前 poller停止；同实例显式 start续送 |
| Cloud compact后 receipt response丢失 | local READY outbox + Cloud compacted witness | restart重投得到 `DUPLICATE_COMPACTED` |
| 任一进程 restart | 该进程所有上述内存状态丢失 | 不恢复/不复用 old taskRun，不宣称 durable exactly-once |

父级六项 blocker closure：

| blocker | closure |
|---|---|
| P1-1 | `BUSINESS_CONSUMING` 在 callback 前原子取得；异常进入不可重放 UNKNOWN sink |
| P1-2 | retained action state 是 canonical occurrence owner；paused revision映射、quota减账、mode frontier完整定义 |
| P1-3 | 独立 64 control lane、1000/10000 retained cap、1:1 fairness、双端 send-handle/CAS、同实例显式 restart |
| P1-4 | pending/late/compacted 三路都先命中 server-owned request/witness再比较 exact address/digests |
| P2-1 | 全部 durable/落盘含义撤回，仅同进程 retained in-memory |
| P2-2 | `RemoteCommandOutcomeAck.java` 从写集移除 |

锁序、A 的 observer deadline/PAUSED 三道门、K current-slot generation、ACTIVE trusted NOT_EXECUTED renewal、observer
no-renewal、无 timer/TTL/LRU、无新增业务 read/verification/phase/park/yield 均不变。**无已批准业务差异；按基线等价迁移。**

**Worker M 状态：Design Repair #1 已追加，停止并等待父级 DESIGN RE-REVIEW；未获批准前不实施。**

## Parent Design Review #2 - BLOCKED - 2026-07-13

父级复核结论：Repair #1 已关闭原 Review #1 的 consume-before-mutation、address server-owned lookup、non-durable 术语和
comment-only churn；PAUSED frontier 与双端 send-handle 的大方向也成立。但 control lane 与 PAUSED slot mint 仍有三个可实现性
缺口。结论：**BLOCKED，P0=0，P1=3，P2=0**。K 仍未 Implementation APPROVED，M 继续 design-only；下一轮只补 delta，
不得重写全文。

### P1-1：control token 在 SENDING 时释放 lane 槽，finally 重排存在容量竞态

- 证据：R3.1 L827-L830 规定 poll claim 从 lane 取走 token，response finally 再把同 token 放回；同时 L801/L806-L807
  允许 lane 有空位时接纳新的 retained ack。claim 后 lane 从 64 变 63，新 ack 可占回第 64 格；此时 finally 对旧 SENDING
  token 的 requeue 会失败或越过容量。
- 影响：旧 ack 可能处于 `QUEUED`/retained 状态却没有 lane token，永久无法再次投递；若强行插入则突破 64 容量。
- 返修条件：SENDING 必须继续占有一个不可转让的 lane permit，admission capacity 计算包含 QUEUED+SENDING；或使用固定
  slot/ring entry 原地状态转换，claim 不释放物理容量。写出 claim、新 ack admission、finish send、receipt compact 四者的
  原子计数方程，任何交错都不能丢 token或超过 64。

### P1-2：`RETAINED_NOT_QUEUED` 没有可达的后续 admission 触发点

- 证据：R3.1 L806-L807 说 lane 满时 ack 留在 `RETAINED_NOT_QUEUED`，以后由“显式
  `finishFinalConsumption`”再尝试；但 callback 已经完成且 consume 不能重放，设计没有命名任何仍持有 exact consumption
  handle 的调用者，也没有 poll/receipt/route 事件负责推进该 retained ack。
- 影响：一次短暂 lane 满即可永久卡住 business-consumed notice、frontier、detail quota和后续 occurrence；retained control
  budget最终耗尽。该状态不是 fail-closed 的短暂阻断，而是无恢复路径的泄漏。
- 返修条件：定义唯一、确定且有界的 admission owner，例如每次 authenticated poll 在同 `stateLock` 内先从 bounded
  `RETAINED_NOT_QUEUED` FIFO 向空闲 fixed lane slot推进一个；该 FIFO/slot必须共用已预留的 owner/global budget，不新增 timer/
  thread/retry，也不扫描无界 map。或者让 reservation 从一开始就占 fixed lane slot，从结构上删除
  `RETAINED_NOT_QUEUED`。给出 no-progress 与 fairness 证明。

### P1-3：PAUSED canonical slot handle 的 mint 路径引用了 K 不存在且在 PAUSED 不可调用的能力

- 证据：R2.1 L734-L750 声称 `CanonicalSemanticSlot` 由 K current-slot authority 产生，但 K 当前唯一 public 业务面是
  `CloudTaskRunCurrentContextSlot.current()`；PAUSED 时该方法按 typed checkpoint unwind，不能给 observer 返回 ACTIVE runtime。
  K 的实现写集已冻结，M 的 New/Modify 表也没有 `CloudTaskRunCurrentContextSlot`，目前没有一个具体 factory 能从 A 的 exact
  PAUSED capability 与 retained Task runtime 安全取得 canonical slot。
- 影响：实现者只能重新开放 raw phase/actionSlot，或绕过 current slot直接抓内部 runtime，都会重建 forgeable occurrence/
  cross-mode authority；按当前写集无法编译出所述 API。
- 返修条件：把 factory 精确落在既有 authority assembly/retained state 写集内：由 assembly 持有的 non-mintable
  `SlotGenerationHandle` + A exact `CloudPausedReadOnlyObservationContext` 共同授权，只允许预注册固定 slot enum；校验同
  authority/stable taskRun/window/stopEpoch 和 exact paused revision后，返回 opaque canonical handle。不得调用 PAUSED 下的
  `current()`，不得修改 K slot public API，不得接收 raw string/occurrence。补 FQCN、方法可见性和 caller 保存者。

### 下轮门禁

M 只追加 `Internal Worker M - Design Repair #2 Delta` 关闭上述三项；K Implementation APPROVED 前仍零 Java。其它已关闭章节
不重开，A observer deadline/三道 PAUSED gate、K generation、普通 outcome digest、无 timer/TTL/LRU 全冻结。
**无已批准业务差异；按基线等价迁移。**

## Internal Worker M - Design Repair #2 Delta - 2026-07-13

本段只替换 Repair #1 的 control lane 物理槽语义与 PAUSED canonical slot factory seam；Review #1 已关闭章节不重写、
不重开。K 尚未 `Implementation APPROVED`，本轮继续零 Java/Maven/resources/tests、零 build、零运行面。

### D1. fixed-slot lane：SENDING 持有原 permit（关闭 Review #2 P1-1）

Repair #1 的“claim 取走 token、finally 再 requeue”模型撤回。每个 authenticated route 改为固定长度 `64` 的
`ControlSlot[64]`；slot 本身就是唯一物理 permit，不另建可丢失 queue token。一个 control entry 从 reservation 到 compact
始终原地占有同一个 `slotIndex + slotGeneration`：

```text
EMPTY
  -> RESERVED(controlReservationId)
  -> QUEUED(exactAckBytes, ackDigest)
  -> SENDING(sendGeneration, exactAckBytes, ackDigest)
  -> QUEUED                         // exact send-handle finish
  -> LOCAL_APPLIED(receiptDigest)   // receipt 可从 QUEUED 或 SENDING CAS 进入
  -> EMPTY                          // exact coordinator compact

callback failure: RESERVED -> EMPTY
```

`RESERVED`、`QUEUED`、`SENDING`、`LOCAL_APPLIED` 均为 occupied；特别是 `SENDING` 不释放 permit。poll 只做原地
`QUEUED -> SENDING`，新 ack admission 只可 CAS 一个 `EMPTY -> RESERVED`，所以 claim 后不存在“64 变 63”的可抢槽窗口。
`finishControlSend` 只在同 slot 原地 CAS `SENDING -> QUEUED`，不 offer/requeue、不可能因容量失败而丢 token。

令 route 内四类 occupied 数为 `R/Q/S/L`，空槽为 `E`，则始终：

```text
E + R + Q + S + L = 64
occupied = R + Q + S + L <= 64
ownerOccupied = sum(route occupied for owner) <= 1000
globalOccupied = sum(all route occupied) <= 10000
```

四个要求的原子计数变化均在 `broker.stateLock` 内：

| 操作 | 原地状态/计数方程 | permit 结论 |
|---|---|---|
| consume pre-claim reservation | `E--, R++`；owner/global `+1` | callback 前已独占 fixed slot；三层任一 cap 满则不执行 callback |
| poll claim | `Q--, S++` | occupied、owner/global 全不变；新 ack 不能占该 slot |
| new ack admission 与既有 SENDING 交错 | 只能选择另一个 EMPTY：`E--, R++` | 总和仍 64；不存在复用 SENDING permit |
| callback success | `R--, Q++` | exact bytes 写入同 slot；occupied 不变 |
| callback failure | `R--, E++`；owner/global `-1` | reservation 释放，ActionRecord 进入既定 consumption-unknown |
| send finish/response loss | exact handle `S--, Q++` | 同 slot 可再投；occupied 不变、无 requeue |
| receipt accepted | exact witness `Q--, L++` 或 `S--, L++` | receipt 与 route finally 竞态由 slotGeneration/sendGeneration CAS 决定 |
| receipt compact | `L--, E++`；owner/global `-1` | 只有 Cloud 两账本/frontier 同步 compact 后才释放 permit |

send handle 固定为 `(routeIdentity, slotIndex, slotGeneration, sendGeneration, ackDigest)`。receipt 先把 SENDING 改为
LOCAL_APPLIED 时，迟到的 send-finish 因 state/generation 不匹配 no-op；send-finish 先改回 QUEUED 时，receipt 再从 QUEUED
进入 LOCAL_APPLIED。两种交错都不改 occupied，也不产生第二 token。route cleanup 只有 command lane 空、64 个 control slot
全 EMPTY、且无 retained broker detail 时才允许，不能在 SENDING/LOCAL_APPLIED 时回收 route。

### D2. 结构性删除 RETAINED_NOT_QUEUED（关闭 Review #2 P1-2）

`RETAINED_NOT_QUEUED` 状态、其 FIFO 与“以后再 admission”调用面全部删除。consume transaction 在进入
`BUSINESS_CONSUMING` 前必须直接取得 route fixed slot 的 `EMPTY -> RESERVED`；无法取得时停在
`OUTCOME_FINAL_UNCONSUMED` 并返回 `CONTROL_LANE_CAPACITY`，业务 callback 尚未执行，故不存在“业务已消费但 notice 无 owner”的
中间态。成功 callback 只做同一 slot 的 `RESERVED -> QUEUED`，天然可达 poll。

fixed array 的唯一 admission owner 是 `RemoteFinalConsumptionCoordinator` 的 consume pre-claim 路径；它在既定
`ActionRecord -> retirementLock -> ActionLedger -> broker.stateLock` 内调用 broker package-private reservation primitive。
poll 不是 admission owner，只从 fixed slots 选择 QUEUED；receipt 只把 exact slot推进 LOCAL_APPLIED；compact 才释放 EMPTY。
没有第二 FIFO、无 map 扫描、无 timer/thread/retry。

每个 route 保留 `nextLane` 与 `controlCursor`：command/control 同时 ready 时仍严格 1:1 交替；选择 control 时从
`controlCursor` 开始最多检查固定 64 个 slot，claim 后 cursor 前进到下一 index。只要 authenticated poll 持续到达：

- 无 command 竞争时，每个 QUEUED control 在最多 64 次成功 poll selection 内被 claim；
- command 持续存在时，每两个 selection 至少一个 control，故每个 QUEUED control 在最多 128 次成功 selection 内被 claim；
- response/receipt 持续丢失只会把同 slot 原地 `SENDING -> QUEUED`，cursor 已前进，不会让一个 ack永久压住其它 ack。

若 client 完全不 poll，则没有网络协议可以制造进展；此时最多 64 个 route control occupied，owner/global 仍受
1000/10000 cap，后续 consume 在业务 mutation 前 fail closed，不泄漏无界 pending。该 no-progress 是明确的离线 bounded
backpressure，不是不可达 admission 状态。

### D3. PAUSED canonical factory 落在 assembly/retained-state（关闭 Review #2 P1-3）

#### D3.1 精确类型、FQCN 与可见性

不修改 `CloudTaskRunCurrentContextSlot` 的 public API，也不调用 PAUSED 下的 `current()`。M 实施时只在已列入写集的两个
现有文件增加 package-internal seam：

```text
com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskRunAuthorityAssembly
  package-private acquirePausedObservationSlot(
      CloudTaskRunCurrentContextSlot.SlotGenerationHandle expectedLastActiveGeneration,
      CloudPausedReadOnlyObservationContext exactPausedCapability,
      CloudTaskRetainedActionState.BattleRadarSemanticSlot fixedSlot)
  -> CloudTaskRetainedActionState.CanonicalObservationSlot

com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskRetainedActionState
  package-private enum BattleRadarSemanticSlot
  package-private static final class CanonicalObservationSlot
      private constructor
  package-private synchronized mintPausedObservationSlot(...)
```

`CloudTaskRunAuthorityAssembly` 是 factory 的唯一 package-private caller surface；真正 mint 仍由该 assembly exact runtime
持有的 `CloudTaskRetainedActionState` package-private 同步方法完成，方法首项必须校验 assembly private authority identity。
`CanonicalObservationSlot` 只暴露给同包
`CloudTaskRunActionLedger.acquireObservation(exactPausedCapability, canonicalSlotHandle)`，其 constructor 对 ledger、Service、host
全部不可见。

固定 enum 复用 L 已冻结的七个 slot，不接受扩展字符串：

```text
FULL_AUTO_FLAG
FULL_SELECTION
FULL_TOP_ICONS
FULL_MINIMAP_COORDINATE
FAST_AVATAR_BASELINE
FAST_AVATAR_PROBE
FAST_AVATAR_REFRESH
```

enum 内部固定 `phaseCode="battle-radar"`、`actionSlot=enum.name()`、`operation=CAPTURE`；没有 string/long constructor。
PAUSED factory 只允许四个 `FULL_*`，三个 `FAST_*` 在此入口 typed reject；ACTIVE 后续仍走其既有 trusted action path。
caller 永远不能传 raw `phaseCode`、raw `actionSlot`、`businessActionKey`、`occurrence`、`attempt` 或 paused revision。

#### D3.2 联合授权与 exact 校验顺序

K 已有 non-mintable `CloudTaskRunCurrentContextSlot.SlotGenerationHandle` private constructor，并已有 package-private
`taskServiceRuntime()`；M 只读该既有 handle，不给 slot 增加 getter/mint/PAUSED API。factory 按以下顺序 fail closed：

1. 从 `expectedLastActiveGeneration.taskServiceRuntime()` 取得 handle 封装的 exact K `TaskServiceRuntime`；要求
   `runtime.authorityIdentity()` 与当前 assembly 的 private `authorityIdentity` 引用相同。
2. 要求 runtime 的 `retainedActionState()` 是本次 mint 的 exact owner；其 ACTIVE context 的 scope/taskRunId/taskType/window/
   stopEpoch 与 `exactPausedCapability` 全字段相等。
3. 要求 `exactPausedCapability.pausedRunRevision() == Math.incrementExact(lastActiveRunRevision)`。因此旧 generation handle 在经历
   任一 resume 后不可能授权当前 pause；foreign/stale/跳 revision 全拒。
4. 直接调用本 assembly 已持有 coordinator 的 A-approved
   `pausedObservationDenialReason(scope, taskRunId, window, stopEpoch, pausedRunRevision)`；只有 null 才继续。该检查证明 capability
   仍是 exact current PAUSED binding，不经过 ACTIVE confirmation，也不调用 slot `current()`。
5. 校验 enum 是固定 PAUSED-allowed `FULL_*`，然后在 exact retained state monitor 内按 Repair #1 的 no-gap frontier mint
   `occurrence=completedOccurrence+1, attempt=0`；same capability reference + same enum + same revision 重取只返回同一 opaque handle。
6. opaque handle retained exact generation-handle reference、exact paused-capability reference、enum、minted occurrence、owner state；
   ActionLedger 再逐 reference/field校验后才能创建 observation identity。

`SlotGenerationHandle` 与 A capability 缺一不可：generation handle 单独只代表最后 ACTIVE runtime，不能授权 PAUSED；capability
单独只代表当前只读 lifecycle，不能选择 authority retained state或 mint semantic occurrence。

#### D3.3 handle 保存者与写集边界

handle 保存者固定为 K 已定义的 trusted lifecycle activation adapter：初值只来自
`CloudTaskRunAuthorityAssembly.CurrentContextSlotActivation.slotGenerationHandle()`，每次 resume 只用
`CloudTaskRunAuthorityAssembly.resumeTaskServiceRuntime(...)` 的返回值替换其 per-taskRun retained field。Business Service、L2
`BattleRadarService`、observer callback、host 都不保存/取得 generation handle；PAUSED observer adapter只向上述 assembly factory
提交它已保存的 latest handle、A exact capability和 fixed enum，并把返回的 opaque canonical handle交给 ledger。

M 不新增/修改 `CloudTaskRunCurrentContextSlot.java`，不改变 `current()`、resume/terminal public或 package-private签名；factory
实现只落在 Design #1 已有 Modify 的 `CloudTaskRunAuthorityAssembly.java` 与 `CloudTaskRetainedActionState.java`。由于两者与 K
直接交叉，实施硬门仍是 **K Implementation APPROVED 后才允许 M Java**；本 Delta 不预写 K source。

### Delta closure

| Review #2 blocker | closure |
|---|---|
| P1-1 | 64 fixed slots原地转换；SENDING继续占 permit；完整计数方程与 receipt/finish CAS 交错闭合 |
| P1-2 | 删除 RETAINED_NOT_QUEUED；consume pre-claim直接 RESERVED；无 timer/thread/FIFO/map scan |
| P1-3 | assembly package-private factory + retained-state package-private synchronized mint；K handle与A capability联合授权固定 enum |

Repair #1 其余已关闭合同、A observer deadline/三道 PAUSED gate、K generation、ordinary outcome digest、无 timer/TTL/LRU
全部冻结。**无已批准业务差异；按基线等价迁移。**

**Worker M 状态：Design Repair #2 Delta 已追加，停止并等待父级 DESIGN RE-REVIEW；K 未 APPROVED 前零 Java。**

### K Implementation gate update - 2026-07-13

父级共享日志 `2026-07-12-cloud-current-context-slot-worker-k.md` 已追加
`Parent Implementation Review #2 - APPROVED`：fresh Cloud package exit 0，K 结论为
`Implementation APPROVED，P0/P1/P2=0`，current-context slot 切片关闭。故本 Delta 上文“Ｋ未 APPROVED 前零 Java”只作为
本次设计修复期间的历史门禁，现由该父级批准明确解除；本轮仍按用户要求保持零 Java。

Review #2 三项 closure 不变且不重复展开：

1. `SENDING` 在 64 个 fixed control slots 中原地占有不可转让 permit，容量恒等式
   `E + R + Q + S + L = 64`；claim/finish/receipt/compact 均只做 exact slot generation CAS。
2. `RETAINED_NOT_QUEUED` 已结构性删除；唯一 admission owner 是 consume pre-claim 的
   `EMPTY -> RESERVED`，cap 满时 callback 前 fail closed，无 timer/thread/FIFO/map scan。
3. PAUSED canonical slot factory 只落在既有 `CloudTaskRunAuthorityAssembly`/`CloudTaskRetainedActionState` 写集，联合使用
   non-mintable `SlotGenerationHandle` + exact A PAUSED capability授权固定 enum；不调用 PAUSED `current()`、不修改 K public API、
   不接收 raw string/occurrence。

K 外部门禁虽已通过，M 的 Java 实施仍须等待本固定日志的父级 `DESIGN APPROVED`；本轮只追加该 gate update，未修改
Java/Maven/resources/tests，未运行 build，未启动任何运行面。**无已批准业务差异；按基线等价迁移。**

**Worker M 状态：Design Repair #2 Delta 保持提交，K Implementation gate 已通过，停止并等待父级 DESIGN RE-REVIEW。**

## Parent Design Review #3 - M0 APPROVED / FULL R0 BLOCKED - 2026-07-13

父级已复核 Repair #2 的 fixed-slot control lane、consume 前 `EMPTY -> RESERVED` admission、SENDING permit 计账与
no-gap frontier。该部分结构性删除 `RETAINED_NOT_QUEUED`，在 callback 前做 bounded fail-closed，且没有 timer/TTL/LRU/
takeover，方向通过。结论分两层：

### M0 DTO leaf - DESIGN APPROVED

允许同一 Worker M 立即实施一个互不交叉的纯 DTO 叶子，写集严格为 **8 New / 0 Modify**：

- Cloud `com.yueyunfe.dhxy.cloudbrain.remote`：`RemoteSemanticAddress`、`RemoteFinalConsumedAck`、
  `RemoteFinalConsumedReceipt`、`RemoteFinalConsumedReceiptAck`；
- DHXY `com.bot.dhxy.cloud.remote`：同名四类。

两仓字段、枚举、nullable 规则必须逐项同构并遵循本报告 §3/§5：semantic address 的 canonical phase/slot、non-negative
occurrence/attempt；ack 的 exact scope/taskRun/revision/window/stopEpoch/operation/ids/digests/non-UNKNOWN final/disposition；
receipt 的 APPLIED/DUPLICATE_APPLIED/REJECTED 与 monotonic frontier；receipt-ack 的
ACCEPTED_COMPACTED/DUPLICATE_COMPACTED/REJECTED。只做 immutable transport DTO 与构造校验，不接 broker/handler/digest/
schema/assembly/host，不新增或恢复 tests。目标若已存在立即停下，不覆盖。完成后运行 Cloud `mvn -q clean package` 和 DHXY
`mvn -q -DskipTests compile`，并追加 `Internal Worker M - M0 Implementation #1` 的 exact diff、双仓 wire 对照与构建证据。

### Full R0 - BLOCKED，P0=0 / P1=1 / P2=0

**P1：D3.3 依赖了并不存在的 retained lifecycle activation adapter。** 当前
`CloudTaskRunAuthorityAssembly.CurrentContextSlotActivation` 只是首次激活的返回 record；
`resumeTaskServiceRuntime(...)` 只返回新的 `SlotGenerationHandle`。源码中没有一个 assembly-owned per-taskRun adapter 保存并
原子替换该 handle，也没有可供 PAUSED observer 取得“latest exact handle”的 owner/API。影响是设计中的 PAUSED canonical
factory 无法形成可编译、不可伪造的调用链；若临时让 Service/host 保存 handle，会重新打开 authority bypass。

返修条件：追加 `Design Repair #3 Delta`，只补 exact adapter 的 FQCN、package visibility、authority identity、
initial activation/install/resume/terminal 方法、与 H/K transition permit/lock 的原子顺序、容量/restart/失效语义和精确文件写集；
observer 只能拿 opaque capability，不能拿 raw handle/string/revision。该 adapter 获父级批准前，Full R0 对 existing Java 的
Modify、wire/digest/schema/broker/handler/assembly 接线全部冻结。M0 的批准不代表 Full R0 通过。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker M - M0 Implementation #1 - 2026-07-13

- 完成严格 **8 New / 0 Modify**：Cloud `com.yueyunfe.dhxy.cloudbrain.remote` 与 DHXY
  `com.bot.dhxy.cloud.remote` 各新增 `RemoteSemanticAddress`、`RemoteFinalConsumedAck`、
  `RemoteFinalConsumedReceipt`、`RemoteFinalConsumedReceiptAck`；开工时八个目标均不存在。
- 双仓四类 JSON 字段名/顺序、nullable omission 与三组 enum 逐项同构；构造校验覆盖 canonical phase/slot/scope/ID、
  non-negative occurrence/attempt/revision/stop、version=1、CAPTURE-only `captureId`、非 UNKNOWN final、PAUSED no-renewal、
  SHA-256 digest 与 no-gap applied frontier。
- 自审确认 8/8 文件、property order 与 enum 一致；未接 Full R0、未新增 tests、未启动运行面。按父级协调 **未运行 Maven**；
  fresh Cloud package 与 DHXY compile 留给父级统一执行。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker M - Design Repair #3 Delta - 2026-07-13

- future package-private final FQCN 固定为
  `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskRunRetainedLifecycleActivationAdapter`，由 assembly private identity 唯一构造；
  有界 exact scope/taskRun entry 私存 latest K handle，只发 opaque activation/PAUSED capability。
- 方法固定 `activateInitial/installInitial/resume/acquirePausedObservation/closeTerminal`；observer 不取得 raw handle/string/revision。
  锁序固定 `adapter entry lock -> K transitionLock -> H ownerMonitor/executionLock`；initial 在 H 前完成可失败工作，resume 原子替换
  latest handle，terminal 可 exact retry并作废旧 capability；restart 不重建，fail closed。容量 global `10_000`/owner `1_000`，
  无 TTL/LRU/eviction/takeover。
- future 写集仅 **1 New** `CloudTaskRunRetainedLifecycleActivationAdapter.java` + **1 Modify**
  `CloudTaskRunAuthorityAssembly.java`；本轮零实施，Full R0 继续 BLOCKED，其余 Java/wire/schema/DHXY 均不改。

**无已批准业务差异；按基线等价迁移。**

## Parent M0 Implementation Review #1 - BLOCKED - 2026-07-13

父级已复核双仓 8 个新 DTO，并亲自执行 Cloud `mvn -q clean package` 与 DHXY
`mvn -q -DskipTests compile`；两者均 exit 0，Cloud Surefire 21/21 通过。但编译成功没有关闭以下 wire blocker。
结论：**BLOCKED，P0=0 / P1=2 / P2=0**：

1. **P1：`OutcomeCode` wire 枚举不闭合。** Cloud
   `com.yueyunfe.dhxy.cloudbrain.remote.OutcomeCode` 含 `BROKER_CAPACITY_EXCEEDED`，DHXY
   `com.bot.dhxy.cloud.remote.RemoteOutcomeCode` 不含该值；新 Ack/Receipt/ReceiptAck 分别直接使用这两个类型。因此 Cloud
   一旦对可信 final 写出该 code，本地无法反序列化，违反本切片“两仓 enum 逐值同构”的批准条件。返修必须在 DHXY
   `RemoteOutcomeCode.java` 补同名值并保持与 Cloud 顺序一致，随后重新做 enum 逐值 diff。
2. **P1：DHXY 三处 `Nulls.FAIL` 没有落到 Jacksonized builder 的可见 setter。** 源码把注解写在 outer value
   field；父级对 fresh `target/classes` 执行 `javap -v`，outer class 有 `JsonSetter/Nulls.FAIL`，但
   `RemoteFinalConsumedAck$...Builder` 的 `observationMode(...)` / `captureId(...)` 与
   `RemoteFinalConsumedReceipt$...Builder.appliedOpenOccurrence(...)` 均无 `JsonSetter` runtime annotation。`@Jacksonized`
   反序列化走 builder，故显式 JSON null 会与 key absent 合流，和 Cloud record 侧 fail-null 不同，也破坏报告宣称的唯一
   canonical nullable 表示。返修必须让这三个 builder property 的显式 null 真正 fail、key absent 仍允许；可采用本项目可编译的
   explicit builder setter 或等价 Jackson creator 方案，但不得放宽 Cloud 侧，也不得新增 test。完成后用 fresh 编译后的
   `javap -v` 给出三处 builder/creator 的 runtime `JsonSetter(nulls=FAIL)` 证据。

允许最小返修写集：既有 8 个 M0 DTO 中仅修改必要的 DHXY DTO，另允许 **1 Modify**
`src/main/java/com/bot/dhxy/cloud/remote/RemoteOutcomeCode.java`；Cloud M0 DTO、Full R0、broker/handler/digest/schema/assembly/host
继续冻结。返修后由父级重新运行双构建并复审；Worker 自审不算批准。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker M - M0 Implementation Repair #1 - 2026-07-13

本轮严格只修改父级批准的 3 个 DHXY 文件：

1. `RemoteOutcomeCode.java`：在 `STOP_REQUESTED` 与 `TIMEOUT` 之间新增 `BROKER_CAPACITY_EXCEEDED`。只读 enum diff 为
   Cloud `19` 项 / DHXY `19` 项，名称与顺序逐值完全一致（`SequenceEqual=true`）。
2. `RemoteFinalConsumedAck.java`：把 `@Jacksonized` 与 constructor `@Builder` 配对；显式定义 builder
   `observationMode(...)`、`captureId(...)`，两者均标注对应 property 的 `@JsonSetter(nulls=Nulls.FAIL)`。
3. `RemoteFinalConsumedReceipt.java`：同样把 `@Jacksonized` 与 constructor `@Builder` 配对；显式定义 builder
   `appliedOpenOccurrence(...)` 并标注 `@JsonSetter(nulls=Nulls.FAIL)`。

缺 key 时 Jackson 不调用上述 setter，builder 字段继续保持 null；显式 JSON null 则由 setter metadata fail。既有构造合同不变：
`captureId` 仅非 CAPTURE 可缺失，`appliedOpenOccurrence` 缺失时仍要求 `appliedThroughAttempt=-1`。

为避免写父级 `target/classes`，仅将这 3 个源文件隔离编译到系统临时目录供 `javap -v` 读取；isolated `javac` exit 0，未运行
Maven/tests。`javap -v` 证据：

- 两个 outer DTO 均有 runtime `JsonDeserialize(builder=...Builder.class)`；两个 builder 均有 runtime
  `JsonPOJOBuilder(withPrefix="", buildMethodName="build")`。
- `RemoteFinalConsumedAckBuilder.observationMode(...)`：runtime-visible
  `JsonSetter(value="observationMode", nulls=FAIL)`。
- `RemoteFinalConsumedAckBuilder.captureId(...)`：runtime-visible `JsonSetter(value="captureId", nulls=FAIL)`。
- `RemoteFinalConsumedReceiptBuilder.appliedOpenOccurrence(...)`：runtime-visible
  `JsonSetter(value="appliedOpenOccurrence", nulls=FAIL)`。

隔离 annotation processing 同时报告 Lombok 的 Jackson2/Jackson3 ambiguity warning，但 exit 0，且上述 `javap` 明确落出本项目
Jackson2 runtime annotation；父级 fresh Maven 构建与最终判断仍待统一执行。Cloud DTO、Full R0、broker/handler/digest/schema/
assembly/host/tests/运行面均未触碰。**无已批准业务差异；按基线等价迁移。**

## Parent M0 Implementation Review #2 - APPROVED - 2026-07-13T01:42:00-04:00

父级已对 Repair #1 做 fresh 源码与产物复审，结论为 **APPROVED，P0/P1/P2=0**，批准范围仅限 M0 跨仓 DTO 叶子：

- Cloud `mvn -q clean package` exit 0；Surefire 4 suites / 21 tests / failures 0 / errors 0 / skipped 0；
- DHXY `mvn -q -DskipTests compile` exit 0；
- Cloud `OutcomeCode` 与 DHXY `RemoteOutcomeCode` 均 19 项，名称和顺序逐项一致；
- fresh `target/classes` 的 `javap -v` 确认
  `RemoteFinalConsumedAckBuilder.observationMode(...)`、`captureId(...)` 与
  `RemoteFinalConsumedReceiptBuilder.appliedOpenOccurrence(...)` 均带 runtime-visible
  `JsonSetter(nulls=FAIL)`；缺 key 仍保留 builder 默认 null，显式 null 不再与 absent 合流；
- 批准写集外无新增代码改动，`git diff --check` 通过。

本批准不代表 Full R0 已实现或批准；Full R0 仍须先审 `CloudTaskRunRetainedLifecycleActivationAdapter` 的设计/实现及后续
broker/handler/digest/schema/frontier 接线。**无已批准业务差异；按基线等价迁移。**

## Parent Design Review #4 - LIFECYCLE ADAPTER IMPLEMENTATION APPROVED - 2026-07-13T01:48:00-04:00

Design Repair #3 的方向获批，父级将缺失的可编码合同固定如下；本批准只放行 lifecycle adapter，不放行 Full R0 的
broker/handler/digest/schema/frontier 接线。写集严格为 **Cloud 1 New + 1 Modify**：

1. New `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskRunRetainedLifecycleActivationAdapter`；
2. Modify `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskRunAuthorityAssembly`，只创建/持有该 adapter 并提供其所需的
   package-private assembly 调用，不改现有 lifecycle/coordinator/gate/H/K 语义。

实现不变量：

- adapter 由单一 assembly 的 private `AuthorityInstanceIdentity` 绑定；每个 stable
  `(scope,taskRunId,taskType,window,nonTerminalStopEpoch)` 仅一个 entry，保存 exact K slot、latest generation handle 与 initial
  immutable metadata；same-entry initial reentry 返回同一 opaque handle，不再次 activation；
- admission 在 adapter monitor 内先核 owner/global 容量（owner 1000、global 10000），再调用 assembly initial activation；失败
  不发布 entry。无 TTL/LRU/eviction/takeover；process restart 不恢复，旧 handle fail-closed；
- resume 在 adapter entry monitor 内验证 same owner/entry/latest generation，再调用现有
  `resumeTaskServiceRuntime(slot, latestGeneration)`；该调用内部的 K transition lock -> H activation -> slot publication 顺序保持，
  成功后 adapter 只原子替换 latest generation，旧 opaque handle 立即 stale；
- PAUSED capability 只能由 adapter 对 latest entry 调现有 execution gate 的 exact current PAUSED snapshot 铸造；capability 无
  public/raw scope、string、revision、slot/generation accessor。未来 trusted retained-state factory 只能经 adapter 的
  package-private validation 使用，不得让 Service/host 自行 mint；
- terminal close 在 entry monitor 内验证 latest opaque handle + exact terminal binding，调用现有
  `closeAndReleaseTerminalTaskServiceRuntime`；若抛出/interrupt，entry 保留供同 handle exact retry；仅 RELEASED/
  ALREADY_RELEASED 成功后 retirement，所有旧 capability 失效。不得提前删 entry、不得补偿性重建 H State；
- 不新增线程、host/caller/Spring bean/raw request/retry/test；不改 A/B/N 文件，不运行 application/server/Task/poller/UI/
  capture/input。Worker 自审不算批准。

内部接管 Worker 必须先追加 `CLAIMED`，再实施并追加 implementation 报告。共享 `target/` 构建留给父级统一执行。
**无已批准业务差异；按基线等价迁移。**

## Internal Worker P - CLAIMED - 2026-07-13T01:55:18-04:00

- 已领取 `Parent Design Review #4 - LIFECYCLE ADAPTER IMPLEMENTATION APPROVED`；`CLAIMED` 只表示领取，不表示实现完成或自审通过。
- 唯一代码写集：Cloud **1 New + 1 Modify**，即新增 `CloudTaskRunRetainedLifecycleActivationAdapter.java`，修改 `CloudTaskRunAuthorityAssembly.java`。

## Internal Worker P - Lifecycle Adapter Implementation #1 - 2026-07-13T02:16:00-04:00

### 精确写集与基线

- Cloud 基线为 `navigation-migration@3b988caa010254973e03342272e6d1d6a9685b01`；`remote/**` 仍是共享 untracked 在途源码，未用 HEAD 覆盖。开工前 assembly SHA-256 为
  `26BCFBEDC0ACA658A088AC2170F0D3A332B304CAFCE73579B6E171880F296F26`。
- **New** `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskRunRetainedLifecycleActivationAdapter.java`，最终 SHA-256
  `B4026D7FC54D2FEEF540EDD3F55296B9992DA7E03B4FDEF52EB42510C6F0A8FF`。
- **Modify** `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskRunAuthorityAssembly.java`，最终 SHA-256
  `DA10E954268326842970C5F84D945BF37CE2C5CF7CFA33D6B89AFE2CBED0560B`。只前置创建 private
  `AuthorityInstanceIdentity`、创建/持有唯一 adapter、提供 package-private accessor、identity 校验与 PAUSED gate seam；既有
  initial/resume/terminal K/H/coordinator 逻辑体未改。
- 除本固定报告追加外未修改任何其它文件；未整理、覆盖、回滚或提交其它 dirty/untracked。

### 状态机

```text
ABSENT
  -> INITIALIZING（仅 adapterMonitor 内可见的容量/entry 预留）
  -> RETAINED(initial handle generation 1)
  -> RETAINED(resume generation N，原 opaque handle stale)
  -> K terminal close -> H RELEASED | ALREADY_RELEASED
  -> RETIRED（移除 map/退账/清 K 与 PAUSED 引用）
```

- same stable tuple + equal initial immutable metadata 始终返回最初同一 opaque handle，不重复 initial activation；resume 后该原句柄按
  latest-generation identity 校验确定性 stale，不能通过 initial reentry takeover。
- initial activation 抛错时，在释放 adapter monitor 前撤销 `INITIALIZING` entry 与 owner/global 预留；没有可观察发布。
- resume 在 entry monitor 内先验证 adapter owner、exact entry、latest opaque generation，再调用既有
  `resumeTaskServiceRuntime(slot, latestGeneration)`；异常/interrupt 不改 entry，成功后在同 monitor 内替换 latest K generation、
  latest opaque handle 与 active revision，并使旧 PAUSED capability 失效。
- terminal 在 entry monitor 内验证 latest handle 与 exact scope/taskRun/taskType/window/revision/stopEpoch/status；既有 K/H 调用抛错或
  interrupt 时 entry 与 handle 原样保留供 exact retry。只有 `RELEASED`/`ALREADY_RELEASED` 才 retirement，不重建 H State。

### 并发、能力与容量不变量

- stable key 精确为 `(scope, taskRunId, taskType, window, nonTerminalStopEpoch)`；quota owner 为
  `(tenantId,userId,deviceId)`。global `10000`、owner `1000` 均在 assembly initial activation 前、adapter monitor 内检查并预留。
- initial 只持 adapter monitor；resume/PAUSED/terminal 串行于 per-entry monitor。resume/terminal 继续走现有
  `entry monitor -> K transitionLock -> H ownerMonitor/executionLock`；terminal 仅在 K/H 已返回并释放其锁后取得 adapter monitor退账。
  adapter monitor 路径不等待 entry monitor，因此没有新增反向锁环。
- `LifecycleActivationHandle` 与 `PausedObservationCapability` 均为 package-private opaque nested type，constructor/字段 private，零
  public/raw scope、string、revision、slot、generation accessor。PAUSED mint 与复验都从 latest entry 调 assembly identity-checked 的
  existing execution gate exact current PAUSED snapshot；future retained-state factory 只能走 adapter package-private validation seam。
- 无 TTL/LRU/eviction/takeover/restart recovery；无 timer/thread/host/caller/Spring bean/raw request/自动 retry；process restart 后不
  rehydrate，旧对象能力自然 fail closed。

### 自审与门禁

- 逐段复读 New/Modify 源码并核对 Parent Design Review #4；花括号 `62/62`、圆括号 `205/205`，New 文件无尾随空白、无非 ASCII、
  无超过 100 字符行；adapter 无 `public` 方法，两个 capability 无 accessor。
- 静态自审结论：P0=0、P1=0、P2=0；Worker 自审不构成父级批准。
- 按用户硬禁令未新增/恢复 tests，未运行 Maven/测试，未启动 application/server/host/Task/poller/UI/capture/input，未执行
  `git add/commit/reset/checkout`。共享 `target/` fresh package 留给父级统一执行，因此本报告不宣称 compile/package 已通过。

**无已批准业务差异；按基线等价迁移。**

**Worker P 状态：DONE，等待父级源码复审与统一构建。**

## Parent Lifecycle Adapter Implementation Review #1 - APPROVED - 2026-07-13T02:20:21-04:00

父级已逐方法复核 P 的 Cloud **1 New + 1 Modify** 并执行 fresh 构建，结论为
**APPROVED，P0=0 / P1=0 / P2=0**：

- initial admission 在 adapter monitor 内先做 owner/global `1000/10000` 配额预留，K/H 失败时原位撤销；同 stable tuple 与
  immutable metadata 的重入返回原 initial opaque handle，不重复 activation；
- resume 在 per-entry monitor 内校验 exact latest handle/generation，再沿既有 K transition lock -> H activate -> slot publish
  调用，成功后才替换 latest generation，旧 handle 与旧 PAUSED capability 均 stale；
- PAUSED capability 的 mint 与复验都经 assembly identity + execution gate 读取 exact current PAUSED snapshot，无 public/raw
  scope/string/revision/slot/generation accessor；
- terminal 先由 adapter/slot 校验 exact next revision/stopEpoch/status，slot 内又读取 coordinator exact current binding；K/H
  抛错或 interrupt 时 entry 保留，只有 `RELEASED/ALREADY_RELEASED` 后才退账退休；
- 未新增 TTL/LRU/eviction/takeover/restart restore/thread/host/caller/test，未改变既有 lifecycle/coordinator/H/K 迁移语义。

父级 fresh Cloud `mvn -q clean package` exit 0；Surefire `suites=4, tests=21, failures=0, errors=0, skipped=0`；shaded JAR
SHA-256 `DE29FD5AFD1C3DD2D4E2C33ECEF6F0CD3BF82F613E44284B0E7685CCF2EE870F`，并实际含 adapter 及其 opaque nested
capabilities。`git diff --check` 无新增 whitespace error。批准范围仅 lifecycle adapter；Full R0 的 frontier/broker/handler/
schema 接线仍未放行。**无已批准业务差异；按基线等价迁移。**

## Parent Task Brief - Internal P / Full R0 Reconciliation #1 - 2026-07-13T02:35:00-04:00

P lifecycle adapter 已父级源码与 fresh package APPROVED，原 Full R0 唯一 adapter blocker 已消失。现恢复同一 Internal Worker P，
只做 **Full R0 最终可实施 Delta**，Java/schema/resources/tests 零修改。先追加 `Internal Worker P - CLAIMED`，再完整读取本日志
Full R0 Design/Repair #1-#3、已落盘 adapter/assembly、M0 DTO，以及
`docs/superpowers/plans/reports/2026-07-13-cloud-capture-scale-wire-worker-q.md` 最新批准波次。

交付 `Internal Worker P - Full R0 Reconciliation #1`，必须：

1. 用已落盘 adapter 的真实 package-private API 替换旧设计中的 future/假设 API，逐条证明 PAUSED capability、initial/resume/
   terminal 与 final-consumed occurrence owner 的可编译调用链；不得新增 public raw handle。
2. 给出 Full R0 精确 Cloud/DHXY New/Modify 表、wire/schema/digest/route/broker/local-ledger/frontier 的原子实施顺序、锁点与
   bounded control-lane admission；UNKNOWN 不得业务消费，frontier 必须 no-gap，错 scope/session/window/revision 必须拒绝。
3. **不得与 A 的 Q-SCALE-WIRE-IMP1 并发改 Java。** 对 A 将修改的 DHXY/Cloud digest、capture DTO/envelope/broker/handler/schema
   明确标成“等 A 父级 APPROVED 后基于其最新内容合并”，本轮只产出合并后的预期方法级 diff，不改文件。
4. 不重开已批准 M0 DTO/lifecycle adapter，不接 host/caller/Service，不新增 TTL/LRU/takeover/retry/test，不运行 Maven或运行面。

完成后停止等待父级审查；自审不算批准。**无已批准业务差异；按基线等价迁移。**

## Internal Worker P - CLAIMED - 2026-07-13T02:37:55-04:00

- 已领取 `Parent Task Brief - Internal P / Full R0 Reconciliation #1`。
- 本轮唯一写集为本固定报告的 append-only Design Delta；Java/schema/resources/tests 零修改。
- `CLAIMED` 仅表示领取，不表示设计完成、实现完成或父级批准。

## Internal Worker P - Full R0 Reconciliation #1 - 2026-07-13T02:55:18-04:00

本节是 Full R0 Design #1、Design Repair #1、Repair #2 Delta、Repair #3 Delta 的最终规范性增量；只替换其中
future/假设 lifecycle API、过时写集与不可编译的 PAUSED owner 路径。既有 consume-before-mutation、server-owned outcome
lookup、fixed-slot control lane、retained in-memory、no-gap、no-renewal observation 等已关闭结论继续有效。

### 1. 已批准叶子、当前外部门与零重开边界

- M0 双仓 8 个 `RemoteSemanticAddress/RemoteFinalConsumed*` DTO 已由
  `Parent M0 Implementation Review #2` APPROVED，本 Delta 逐字段直接复用，零修改。
- `CloudTaskRunRetainedLifecycleActivationAdapter` 与 assembly 中 P 已落盘 seam 已由
  `Parent Lifecycle Adapter Implementation Review #1` APPROVED。本 Delta **不修改 adapter**，不新增 lifecycle handle
  accessor，不重新解释 initial/resume/PAUSED/terminal 状态机。
- Q-SCALE-WIRE 的外部 A 已在 Q 日志追加 `External Worker A - Q-SCALE-WIRE Implementation #1`，但截至本节落笔，
  尚无父级 `Implementation APPROVED`。因此下文所有 A 交叉文件均标记为：
  **等 A 父级 APPROVED 后基于其最新内容合并**；A 的 CLAIMED/Worker 自审/build 不能替代该门。
- 当前生产仍 dormant；本 Full R0 只形成 infrastructure/typed-port 闭包，不创建或接入 host、Task caller、具体
  business Service、Spring bean、application lifecycle 或新线程。

### 2. 真实 lifecycle adapter 到 occurrence owner 的可编译链

#### 2.1 initial / resume / terminal

唯一 package-private lifecycle owner 直接使用现有真实 API：

```text
CloudTaskRunAuthorityAssembly.retainedLifecycleActivationAdapter()
  -> CloudTaskRunRetainedLifecycleActivationAdapter.activateInitial(scope, taskRunId, metadata)
       -> LifecycleActivationHandle
  -> CloudTaskRunRetainedLifecycleActivationAdapter.resume(exactLatestHandle)
       -> replacement LifecycleActivationHandle
  -> CloudTaskRunRetainedLifecycleActivationAdapter.closeTerminal(
         exactLatestHandle, exactTerminalBinding)
       -> RELEASED | ALREADY_RELEASED
```

- `LifecycleActivationHandle` 仍是 adapter 的 package-private opaque nested type；constructor/fields private，零 raw getter。
- initial 同 stable tuple 重入仍返回原 initial handle；resume 后旧 handle 仍 stale；Full R0 不旁路 adapter 保存 K
  `SlotGenerationHandle`。
- assembly 在 `createCurrentContextSlotActivation(...)` 的既有 H 调用前，只为该 runtime 的 exact
  `CloudTaskRetainedActionState` 在 action ledger 中预留一个不可见 `INITIALIZING` run-state registration；所有 map/cap/
  duplicate 校验在 H 前完成。H 抛错时同调用栈撤销；H 成功后仅把 registration 标为 `RETAINED`，再按既有顺序 attach State
  handle / publish K slot，不增加可失败业务工作。
- resume 继续由 adapter 调现有 `resumeTaskServiceRuntime(slot, latestGeneration)`；新
  `CloudTaskServiceExecutionContext` 复用同一 retained state，并验证它仍注册在同 assembly/action-ledger/coordinator。
- terminal 仍先走现有 K close + H release。assembly 只有在 H 返回 `RELEASED/ALREADY_RELEASED` 后，才把 exact terminal
  binding 告知 final-consumption coordinator；该通知若 fail closed，异常返回 adapter，entry 保留，同 handle 重试会沿既有
  `ALREADY_RELEASED` 路径再次完成通知。未解决 detail/UNKNOWN 会保留 run registration，不能提前 cleanup。

#### 2.2 PAUSED capability，不再使用旧 D3 的 generation-handle 假设

旧 D3 的
`acquirePausedObservationSlot(SlotGenerationHandle, CloudPausedReadOnlyObservationContext, raw slot)` 全部撤回。唯一 factory
固定为 assembly 的 package-private 方法：

```text
CloudTaskRetainedActionState.CanonicalObservationSlot acquirePausedObservationSlot(
    CloudTaskRunRetainedLifecycleActivationAdapter.PausedObservationCapability capability,
    CloudTaskRetainedActionState.BattleRadarSemanticSlot fixedSlot)
```

可编译调用链固定为：

```text
adapter.acquirePausedObservation(exactLatestLifecycleHandle)
  -> opaque PausedObservationCapability
assembly.acquirePausedObservationSlot(capability, fixed enum)
  -> adapter.requireCurrentPausedObservationContext(capability)
  -> actionLedger.requireExactRetainedRunState(exact gate-minted PAUSED context)
  -> retainedState.mintPausedObservationSlot(capability, exact context, fixed enum)
  -> opaque CanonicalObservationSlot
```

- assembly 只消费 adapter 已存在的 `requireCurrentPausedObservationContext(...)`；不访问 capability private fields，不调用
  PAUSED 下的 K `current()`，不取得/暴露 generation。
- `BattleRadarSemanticSlot` 与 `CanonicalObservationSlot` 均为 package-private nested type；固定 enum 仍是七个 L slot。
  PAUSED factory 只接受四个 `FULL_*`，三个 `FAST_*` typed reject；constructor private，零 raw phase/actionSlot/occurrence/
  revision accessor。
- canonical handle 只证明 exact retained-state + fixed slot + exact opaque PAUSED capability，不在 factory 时占 detail quota或
  mint occurrence。首次真正 observation bind 时，在同 slot record 下再次走 coordinator PAUSED denial gate，才由 owner mint
  `completedOccurrence + 1, attempt=0`。若 resume 恰好发生在 factory 后、bind 前，stale capability 在 detail admission前拒绝；
  不留下孤儿 detail。bind 后再发生 lifecycle 变化，则既有 enqueue/dispatch/local 三道门产生 exact terminal，供正常
  final-consumed，不做隐式取消。
- same capability reference + same paused revision + same fixed slot 的未完成重取返回同 opaque canonical handle；同 revision
  compact 后返回 `OBSERVATION_ALREADY_COMPACTED`，旧 revision返回 `STALE_PAUSED_REVISION`。Observation 永无 renewal。

#### 2.3 final-consumed 的唯一 occurrence owner

`CloudTaskRetainedActionState.ActionRecord` 是每个 canonical semantic slot 的唯一 monotonic owner；caller 不再提供 occurrence。
现有 `ActionAddress` 规范性改为只含 canonical `phaseCode/actionSlot`，occurrence/attempt 全由 owner 产生。每个 record 常量空间
保留：

```text
modeOwner
completedOccurrence             // initial -1
openOccurrence                  // null or completedOccurrence + 1
compactedThroughAttempt         // open 前/无 open 为 -1
currentDetail/currentHandle
lastPausedRunRevision
lastCompactedWitness
```

ACTIVE retain 链为
`retainedState.retain*(context, ActionAddress) -> actionLedger.acquire(owner-minted
RemoteSemanticAddress) -> opaque Service action handle`；PAUSED 链使用上节 canonical handle。public
`RemoteSemanticAddress` 只是 wire DTO：任何 authority API 都不接受 caller 构造的 DTO 来 mint handle，因此它不是 public raw
capability。

`CloudTaskRunCommandExecutor` 当前已经把 broker 返回的真实 typed outcome object 原样传给
`CloudTaskRunActionLedger.recordOutcome(identity, outcome)`，无需 comment-only 修改。ledger 只需把该 exact object reference 与
state/digest 一起 retained。三个 public typed port 方法新增 operation-specific consume overload，形态固定为：

```text
CloudTaskServicePort.consumeWindowFactFinal(action, exactOutcome, mutation)
CloudTaskServicePort.consumeCaptureFinal(action, exactOutcome, mutation)
CloudTaskServicePort.consumeInputBundleFinal(action, exactOutcome, mutation)
```

`mutation` 只返回 typed `OCCURRENCE_COMPLETE` 或 `ATTEMPT_RETIRED_FOR_RENEWAL`，不接收 raw ID/address/digest；后者只允许
ACTIVE + exact `NOT_EXECUTED`。PAUSED counterpart 保持 package-private 并只接受 `CanonicalObservationSlot`。所有入口必须：

1. 验证 action/canonical handle owner、current generation、exact recorded outcome **reference**、request/address/digests；
2. `UNKNOWN`、unbound、unrecorded、already consuming、consumption-unknown 一律在 callback 前拒绝；
3. 在 callback 前取得 fixed control slot reservation，再原子进入 `BUSINESS_CONSUMING(consumeGeneration)`；
4. callback 在 Cloud retirement/broker locks 外完成全部业务 mutation；正常返回才生成并 retained 一次 exact ack bytes；
5. callback 抛 `RuntimeException/Error/InterruptedException` 或 post-callback CAS 不确定，进入不可重放
   `BUSINESS_CONSUMPTION_UNKNOWN`，不重做 callback、不 compact、不 renewal。

### 3. no-gap frontier 与 exact 双阶段 retirement

每侧 semantic slot key 一致：

```text
tenant/user/device/clientSession/taskRunId
+ taskType/windowId/nativeHandle/processId/playerIdentityEpoch/nonTerminalStopEpoch
+ operation/phaseCode/actionSlot
```

revision、mode、occurrence、attempt 是 exact detail 字段并逐项验证，不从 stable key 消失。允许的唯一新 detail：

| 当前 frontier | 唯一允许 next | 其它输入 |
|---|---|---|
| 无 open | `occurrence=completed+1, attempt=0` | below/gap/并发第二 detail拒绝 |
| open 且上一 compact disposition 为 renewal | 同 occurrence，`attempt=compactedThroughAttempt+1` | 跳 attempt/回退拒绝 |
| PAUSED observation | owner-minted next occurrence，`attempt=0` | renewal/attempt>0拒绝 |

`OCCURRENCE_COMPLETE` 在 DHXY apply 时推进 completed 并清 open；
`ATTEMPT_RETIRED_FOR_RENEWAL` 保持 open occurrence、推进 compactedThroughAttempt。Cloud 仅在 exact receipt 后镜像同一 frontier。
任何一侧都不缓存 gap set。

完整状态机：

```text
BOUND_UNRECORDED
  -> OUTCOME_UNKNOWN
  -> OUTCOME_FINAL_UNCONSUMED
  -> BUSINESS_CONSUMING
  -> BUSINESS_CONSUMED_NOTICE_PENDING
  -> LOCAL_FRONTIER_APPLIED
  -> COMPACTED_FRONTIER

BUSINESS_CONSUMING failure -> BUSINESS_CONSUMPTION_UNKNOWN
```

ack 从 server-owned request detail + exact recorded non-UNKNOWN outcome构造；receipt 从 local detailed terminal + apply 后 frontier
构造。普通 outcome ack 仍只表示 transport accepted，不升级为 business-consumed。Cloud 只有 receipt
`APPLIED/DUPLICATE_APPLIED` 且 ack/receipt/frontier witness 全 exact 时，同时 compact broker detail、action/observation detail并
推进 Cloud frontier；`REJECTED` 不释放 control slot或任何 detail。

校验顺序固定为 authenticated scope/session -> server-owned request or latest compacted witness -> semanticAddress ->
requestDigest -> typed outcome/outcomeDigest -> exact state transition。wrong tenant/user/device/clientSession/taskRun、wrong
window/nativeHandle/pid/playerIdentityEpoch/stopEpoch、wrong operation/revision/mode/occurrence/attempt、future/gap全部在 mutation
前 reject。旧 revision 只有命中 exact retained terminal detail时可做无副作用 retirement；不能重新获得 command authorization。

### 4. bounded control lane、锁序与 progress

Cloud 每个既有 authenticated route 持有固定 `ControlSlot[64]`；不创建第二 route/FIFO：

```text
EMPTY -> RESERVED -> QUEUED -> SENDING -> QUEUED
                                  \-> LOCAL_APPLIED -> EMPTY
callback failure: RESERVED -> EMPTY
```

`RESERVED/QUEUED/SENDING/LOCAL_APPLIED` 全部占原 slot permit。恒等式始终
`E + R + Q + S + L = 64`；owner/global occupied 上限分别 `1000/10000`。consume pre-claim 在业务 callback 前同时检查
route empty slot、owner、global 三层 cap并原地 `EMPTY -> RESERVED`；任一满则保持
`OUTCOME_FINAL_UNCONSUMED`，callback 不执行。结构上不存在 `RETAINED_NOT_QUEUED`。

poll 只做同 slot `QUEUED -> SENDING`；send finish 做 exact generation `SENDING -> QUEUED`；receipt 可从 QUEUED 或 SENDING
进入 LOCAL_APPLIED，迟到 finish 因 generation/state mismatch no-op。command/control 同时 ready 时按 route `nextLane` 1:1
交替；control cursor 最多扫描固定 64 slots。route-local bounded availability signal只唤醒现有 long poll，不启动线程或 timer。

Cloud 唯一新锁序：

```text
CloudTaskRetainedActionState.ActionRecord monitor
  -> RemoteFinalConsumptionCoordinator.retirementLock
    -> CloudTaskRunActionLedger monitor
      -> RemoteGameCommandBroker.stateLock
```

broker poll/outcome/receipt 只持 `stateLock`；receipt endpoint 先在 broker 内产出 opaque accepted handle并释放
`stateLock`，再调用 coordinator，禁止 broker -> coordinator/ledger 反向回调。K transition lock、H owner/execution lock 与 J turn
lock不进入 retirement；initial run-state reservation 在 H 前释放 ledger lock，terminal notification 在 H 返回后才进入
retirement。

DHXY 只持 `RemoteOperationLedger.monitor` 做 frontier/detail/outbox原子 mutation；HTTP POST、handler window/registry gate均在锁外。
既有 readiness 的 ledger -> registry 顺序不反转。terminal cleanup 每个 poll turn最多检查一个 bounded candidate：先在 ledger
取 opaque taskRun candidate、锁外读 registry terminal/absent snapshot、再回 ledger exact-generation CAS cleanup；不扫描无界 map。

### 5. A 最新实现的强制合并点

以下 8 文件在本轮全部零修改。即使 A 已写 Implementation #1，也必须等待 Q 日志父级
`Implementation APPROVED`，然后重读最终内容/hash，再按下表做方法级合并：

| A 文件 | Full R0 批准后预期 delta |
|---|---|
| DHXY `RemoteCaptureOutcomePayload` | **无 R0 delta**；原样保留 A 的 `systemScaleRatio` 字段顺序/null合同 |
| DHXY `LocalRemoteGameCommandHandler` | 保留 A 的唯一-frame scale bracket；只在 digest/payload通过后、任何 window/input副作用前处理 ledger frontier claim status，并让所有 terminal outcome echo command semanticAddress |
| DHXY `RemoteProtocolDigests` | 保留 A 的 finite binary64 canonicalizer逐字；request canonical tree加入 required semanticAddress，新增 ack/receipt digest/verify；普通 outcome公式不另改 |
| DHXY protocol schema | 在 A 已批准 scale/closed-capture/JCS内容上追加 semantic address、poll control、ack/receipt/frontier/restart合同；不覆盖 `5.2` |
| Cloud `CaptureOutcome` | **无 R0 delta**；保留 A 的 scale validation/`withCommon` |
| Cloud `RemoteCommandOutcomeEnvelope` | 保留 A 的 capture exact-key/type/null reconstruction；outer envelope增加 required semanticAddress echo，`toTypedOutcome()` 不把它塞入普通 outcomeDigest |
| Cloud `RemoteProtocolDigests` | 保留 A 的 finite binary64 canonicalizer逐字；request tree加入 semanticAddress并新增 ack/receipt digest/verify |
| Cloud `RemoteGameCommandBroker` | 在 A 的 OBSERVED `observedWindow` exact correlation与 synthetic scale-null基础上合并 semantic frontier、compacted witness、fixed control slots与 receipt primitive |

### 6. Full R0 最终精确写集

M0 8 DTO、P adapter、A-only capture DTO 以及 `CloudTaskRunCommandExecutor` 均不在 Full R0 delta 写集。

#### 6.1 Cloud Brain：1 New + 14 Modify

**New（1）**

1. `remote/RemoteFinalConsumptionCoordinator.java`：consume generation、retirement lock、exact ack/receipt双账本原子 compact；
   package-private，零 host/thread。

**Modify（14）**

1. `remote/RequestContext.java`：required `RemoteSemanticAddress`；`withRequestDigest` 原样保留。
2. `remote/RemoteCommandEnvelope.java`：flat command透传 address；`from(RemoteRequest)` 只读 server context。
3. `remote/RemoteCommandPollResponse.java`：`IDLE/COMMAND/FINAL_CONSUMED` closed union，control payload用已批准 M0 Ack。
4. `remote/RemoteProtocolDigests.java`：**等 A 父级 APPROVED 后基于其最新内容合并** request/ack/receipt digest。
5. `remote/OutcomeCode.java`：与 DHXY 同位增加 `FINAL_CONSUMED`；M0 enum字段继续同构。
6. `remote/CloudTaskRunExecutionGate.java`：ACTIVE request从 ledger identity投影 address；PAUSED builder只接受 opaque canonical
   slot/identity并在 bind 前重验 exact PAUSED，不接 raw occurrence/revision。
7. `remote/CloudTaskRunActionLedger.java`：run-state registration、semantic detail/frontier、exact outcome reference、
   consume/compact状态、compacted request witness、observation减账；combined cap/no-renewal保持。
8. `remote/CloudTaskRetainedActionState.java`：`ActionAddress` 去 caller occurrence，slot ActionRecord成为 occurrence owner；
   fixed enum/canonical PAUSED handle、consume callback与 compact/renew gate。
9. `remote/CloudTaskServicePort.java`：三个 operation-specific exact final-consume overload与 typed disposition；无 raw ID API，
   不接具体 Service caller。
10. `remote/CloudTaskServiceExecutionContext.java`：initial/resume constructors传同 assembly final coordinator，并验证 retained
    state registration；public facade字段零扩大。
11. `remote/RemoteGameCommandBroker.java`：**等 A 父级 APPROVED 后基于其最新内容合并** broker frontier、64 fixed slots、
    1000/10000 control admission、fair poll、receipt/compacted-witness primitive。
12. `remote/RemoteTaskRunRoutes.java`：poll delivery finally完成 exact send handle；在现有
    `outcomePath + "/final-consumed-receipt"` 增 authenticated receipt endpoint。由既有 path派生，故不改
    `CloudBrainServer`/host。
13. `remote/CloudTaskRunAuthorityAssembly.java`：保留全部 P adapter API；增加唯一 final coordinator、initial hidden run-state
    registration/rollback、PAUSED opaque factory、accepted terminal notification。既有 K/H transition语义不改。
14. `remote/RemoteCommandOutcomeEnvelope.java`：**等 A 父级 APPROVED 后基于其最新内容合并** required semanticAddress echo与
    server-owned correlation输入；保留 A capture strict schema。

#### 6.2 DHXY：0 New + 11 Java Modify + 1 schema Modify

1. `cloud/remote/RemoteGameCommand.java`：required semanticAddress；transport strict validation缺失/null拒绝。
2. `cloud/remote/RemoteGameOutcomeEnvelope.java`：required semanticAddress echo；terminal builder只复制 command值。
3. `cloud/remote/RemoteProtocolDigests.java`：**等 A 父级 APPROVED 后基于其最新内容合并** request/ack/receipt digest。
4. `cloud/remote/RemoteCommandPollResponse.java`：command/control payload closed union。
5. `cloud/remote/RemoteCommandPollStatus.java`：增加 `FINAL_CONSUMED`。
6. `cloud/remote/RemoteCommandTransport.java`：typed `submitFinalConsumedReceipt(...)`。
7. `cloud/remote/HttpRemoteCommandTransport.java`：派生
   `OUTCOME_PATH + "/final-consumed-receipt"`；strict receipt POST/ack correlation；网络不确定不内部重试。
8. `cloud/remote/RemoteCommandPollingLoop.java`：每轮最多先送一个 READY receipt，再 poll一次；FINAL_CONSUMED只走 local
   apply，不进 handler；HTTP均锁外，同实例显式 `start()` 才续送。
9. `cloud/remote/RemoteOperationLedger.java`：session slot cap 1000、current detail cap 64、receipt outbox 64、no-gap frontier、
   atomic ack apply/detail删除/outbox发布、one-candidate terminal cleanup。
10. `cloud/remote/LocalRemoteGameCommandHandler.java`：**等 A 父级 APPROVED 后基于其最新内容合并** pre-side-effect frontier
    claim status；below-frontier返回 `NOT_EXECUTED/FINAL_CONSUMED`，gap/wrong identity fail closed；A scale顺序不动。
11. `cloud/remote/RemoteOutcomeCode.java`：与 Cloud 同位增加 `FINAL_CONSUMED`。
12. `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`：**等 A 父级 APPROVED 后基于其最新内容合并** Full R0
    wire/digest/frontier/control/restart章节。

明确不改：M0 八 DTO、`CloudTaskRunRetainedLifecycleActivationAdapter.java`、`CloudTaskRunCommandExecutor.java`、
`CloudBrainServer.java`、`RemoteCommandOutcomeAck.java`、DHXY registry/lifecycle/host/caller、任何 Task/具体 Service、resources/
tests/Maven。

### 7. broker / local-ledger 精确原子行为

- broker register 前按 server-derived slot key验证 frontier；below/compacted current直接 deterministic
  `FINAL_CONSUMED`，gap/future拒绝，不入 command queue。当前 detail最多一条。
- handler `RemoteOperationLedger.claim(...)` 在任何 registration/window/input/capture副作用前做同一 frontier判断；OWNER 才插
  detailed request。错 scope/session/window/revision/address不创建 future或input action reservation。
- local `applyFinalConsumedAck(...)` 先验证 exact detailed terminal与 M0 ack digest，再预留 outbox（64）；预留失败不推进
  frontier、不删 detail。成功后在一个 monitor transition推进 frontier、删除 exact request/terminal/input detail、增加
  `ledgerRevision`、保存首次生成的 exact receipt bytes。
- receipt send `READY -> SENDING -> removed/READY/REJECTED_RETAINED`。POST/response不确定时 exact generation回 READY并让
  当前 poller停止；没有 inner retry。Cloud `ACCEPTED_COMPACTED/DUPLICATE_COMPACTED` 才删除 outbox。
- Cloud receipt compact在完整预验证后，以不抛校验的 mutation同时：推进 state/action/broker frontiers，删除 exact
  action/observation/broker/current input detail，更新 latest compacted request witness，释放 fixed control permit并退 owner/
  global control usage。任何一步不 exact则全留。
- latest compacted outcome duplicate只按 server index
  `(scope,operation,requestId) -> exact latest witness` 接受；更老 witness被下一 occurrence替换后无 exact server证据，fail
  closed，不依赖 caller address自证。

### 8. 原子实施顺序与切换门

Full R0 Java 实施必须是一个双仓原子 cohort；不得把任何中间态交给 runtime：

1. Q 日志出现父级 A `Implementation APPROVED` 后，记录 A 八文件最终 hash并重读全部内容；若 parent要求返修，先等返修批准。
2. 先落双仓 required semanticAddress、poll closed union与同位 `FINAL_CONSUMED` enum；M0 DTO零改。
3. 在 A 最终 canonicalizer上合并 request + ack + receipt digest，并在 A 最终 schema上 append Full R0合同。
4. Cloud 落 retained-state occurrence owner、action-ledger frontier/exact outcome与
   `RemoteFinalConsumptionCoordinator`；随后 assembly只接 package-private dormant seam。
5. 在 A 最终 broker上合并 server frontier、latest witness、fixed control lane与 receipt primitive；再接
   `RemoteTaskRunRoutes` 派生 endpoint，不改 host。
6. DHXY 落 local frontier/detail/outbox，再在 A 最终 handler上接 claim结果；不得改 A capture scale bracket。
7. 最后接 transport/poller control union与 receipt send；仍不创建/启动 poller，不接 caller/Service。
8. 所有源文件稳定后由父级/后续获批 implementation owner执行双仓 fresh compile/package；本 Worker 本轮不构建。

`contractVersion` 保持 1，所以 required semanticAddress 与 final-consumed control不支持 mixed-version rolling。生产/host/caller
必须继续 dormant或 quiesced，直到两仓同一源码 cohort完成构建；禁止用缺省 address、忽略字段、`1.0` scale fallback或跳 digest
做兼容。

### 9. terminal、容量与 restart 边界

- Cloud action semantic slots combined cap仍为 10000；broker semantic slot owner/global仍为 1000/10000；command pending owner
  64、route queue 64、route owner/global cap不变；control另有 route 64 + owner/global 1000/10000。
- DHXY每 session semantic slots 1000、current details 64、receipt outbox 64。capacity拒绝发生在副作用或 business callback前；
  frontier/control slot都不为新请求 eviction。
- exact STOPPED/COMPLETED且无 unbound/pending/dispatched/UNKNOWN/CONSUMING/notice/outbox/detail时，才删该 taskRun 的
  frontiers/run registration/route。UNKNOWN 与 CONSUMPTION_UNKNOWN永久阻断该 old run cleanup，等待 exact late final或人工
  终止进程；没有 TTL/LRU。
- 任一 Java process restart丢其 retained maps/frontiers/outbox/handles；不 rehydrate、不恢复旧 taskRun、不复用 taskRunId。
  同进程 poller显式 restart只复用原 `RemoteOperationLedger` 与 exact receipt bytes。无 takeover、WAL、数据库或 durable
  exactly-once声明。

### 10. 自审与本轮门禁

- 逐项复核 Parent Brief 1-4：真实 adapter API、opaque PAUSED链、initial/resume/terminal、occurrence owner、两仓精确写集、
  A 合并门、atomic顺序、锁序、control容量、UNKNOWN/no-gap/wrong-identity拒绝均已闭合。
- 自审 `P0=0 / P1=0 / P2=0`；本自审不构成父级 DESIGN APPROVED。
- 本轮除本固定报告的 CLAIMED 与本节 append-only内容外，Java/schema/resources/tests零修改；未运行 Maven/tests，未启动
  application/server/host/Task/poller/UI/capture/input，未执行 git mutation。
- 不新增 TTL/LRU/eviction/takeover/retry/test；不改变 lifecycle/coordinator/H/K/J、capture scale、业务 phase/ROI/template/
  click/navigation/fallback/park/yield语义。

**无已批准业务差异；按基线等价迁移。**

**Worker P 状态：Full R0 Reconciliation #1 已追加，停止并等待父级 DESIGN REVIEW；A 父级 Implementation APPROVED 前零 Java。**

## Parent Full R0 Design Review #5 - BLOCKED / Repair #1 Published - 2026-07-13T03:08:00-04:00

父级已按当前落盘 assembly/adapter、broker/routes、DHXY handler/ledger/poller 与 A 最新 scale-wire 逐项复核。P 已正确关闭
旧 generation-handle 假设、给出 owner-minted occurrence/no-gap frontier、fixed control admission、A 交叉文件合并门和完整双仓
写集；M0 DTO 与 lifecycle adapter 批准不重开。但最终可实施链仍有以下缺口，结论为
**BLOCKED，P0=0 / P1=3 / P2=2**，A 当前也因 JCS formatter 返修未 Implementation APPROVED，故 Full R0 继续零 Java。

1. **P1：route 与 authority assembly 仍是两个无法相接的构造岛。** 当前
   `RemoteTaskRunRoutes.create(...)` 在 `:48-50` 自行 new `RemoteTaskRunCoordinator`/`RemoteGameCommandBroker`，只返回
   opaque routes；`CloudTaskRunAuthorityAssembly.create(broker, gameContext)` 是另一个 package-private 工厂，当前 main 下零调用。
   Reconciliation 又规定唯一 `RemoteFinalConsumptionCoordinator` 由 assembly 拥有，但 receipt endpoint 只拿到 routes 内部
   broker，写集还排除了 `CloudBrainServer`/真实 assembly creation。影响是 endpoint 无法到达与 Service port/action ledger 同一
   final coordinator；若各自 new，则形成双重权威。返修必须给出一个具体、可编译、非 late-setter 的构造图：同一 owner 一次
   创建 coordinator、broker、action ledger、final coordinator、assembly 与 routes；route 只拿同一实例的 package-private
   receipt ingress。若需要修改 `CloudBrainServer` 或新增 dormant bundle factory，必须诚实列入写集；不得 public getter broker/
   ledger，不得可选 attach/takeover/第二 coordinator。
2. **P1：DHXY wrong taskRun/window/revision 仍会先占 ledger detail。** 当前
   `LocalRemoteGameCommandHandler.java:147-160` 在 `executeOwnedCommand` 的 registration/window/revision fence 前调用
   `RemoteOperationLedger.claim(command)`。P 只写“claim 前拒绝错 scope/session/window/revision”，但 ledger 不持
   `RemoteTaskRunRegistry`，也未给 registration snapshot/generation 入参或锁序。影响是已认证但 stale/wrong command 可在被
   handler 拒绝前填满 64 detail cap。返修必须固定真实顺序和签名：锁外取得 exact immutable registration/binding generation
   snapshot，ledger claim 在插入前验证该 snapshot 与 command 全 tuple/revision；插入后、机械副作用前仍保留现有第二次
   registration/window fence。说明 registry mutation 与 ledger monitor 的锁序，禁止在 ledger 锁内调用 registry/window/I/O。
3. **P1：只写“任一进程 restart 丢本方 map”没有处理单边 restart。** DHXY 单独重启时 Cloud 的 pending/UNKNOWN/
   CONSUMING/run registration 不会消失；Cloud 单独重启时 DHXY detail/receipt outbox/taskRun registration 也不会消失。
   当前又禁止 TTL/LRU、只允许进程终止清理，因此一侧重启可永久占满另一侧 64/1000/10000 cap，且“taskRunId 不复用”并
   不会自动清旧账。返修需明确 authenticated process/session incarnation 与 fail-closed retirement：旧 incarnation 绝不复活或
   takeover，但对端必须能把旧 run 移入 bounded orphan/quarantine accounting，并给出唯一人工/协调重启收口点；UNKNOWN/
   BUSINESS_CONSUMPTION_UNKNOWN 不得被误判执行结果。若唯一支持的恢复就是“双侧协调重启”，必须写成强制运维门、启动拒绝
   与容量后果，不能继续声称普通单边 restart 已闭合。
4. **P2：control readiness 没有落到现有阻塞原语。** broker 当前在
   `RemoteGameCommandBroker.java:196-198` 阻塞 `commandQueue.poll(timeout)`；“route-local bounded availability signal”没有
   类型、状态或 wake/permit 算法。返修需选定同 route union queue 或 exact route signal，并给出 signal 发布、丢失唤醒、超时、
   command/control 1:1 fairness 的方法级步骤；不得新 thread/poller/timer。
5. **P2：consume callback 与 terminal 检查缺可编译边界。** 设计声称 callback 可抛
   `InterruptedException`，但未定义可抛 checked exception 的 nested functional interface；同时 terminal notification 需要判断
   一个 run 的 unresolved records，却未说明在 10,000 action cap 下如何 bounded 扫描。Repair 列出该 package-private typed
   callback 签名，并让 run registration 维护 O(1) unresolved/cap counters，terminal 不做全表扫描。

### 下一任务：`FULL-R0-DESIGN-R1`

恢复同一 Internal Worker P，只向本固定日志追加一个短 `Full R0 Design Repair #1 Delta`，逐条关闭上述 5 项；
Java/schema/resources/tests/Maven/runtime 继续冻结。不得重写已批准 M0/lifecycle adapter，不碰 A 的 5 文件 JCS 返修、B warning、
R SummonSkill、host/Task/caller；A 父级 `Implementation APPROVED` 前仍不得实施 Full R0。Delta 必须更新最终构造图、精确写集、
双仓 admission 方法顺序、restart 运维合同、control signal 与 callback/counter API。Worker 自审只算 QA。

**无已批准业务差异；按基线等价迁移。**

## Parent Recovery Notice #2 - Canonical EOF Reissue - 2026-07-13T04:23:00-04:00

本节是 `Parent Recovery Notice #1` 的尾部可见副本，不改变 Review #5 的任何结论。原 Internal P 会话在重启后不可达，
且 Review #5 后没有新 CLAIMED；replacement Internal P2 接管 `FULL-R0-DESIGN-R1`。

P2 必须先追加 `## Internal Worker P2 - CLAIMED - <timestamp>`，写明领取时间与唯一写集=仅本日志，然后追加
`Full R0 Design Repair #1 Delta`，逐条关闭 Review #5 的 P1×3/P2×2。Java/schema/resources/tests/Maven/runtime 冻结；
不得改 A/B/R/host/Task/caller。父级仍是唯一 reviewer，Worker 自审不构成批准。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker P2 - CLAIMED - 2026-07-13T04:25:05-04:00

- 任务：`FULL-R0-DESIGN-R1`。
- 领取时间：`2026-07-13T04:25:05-04:00`。
- 唯一写集：仅本固定日志
  `docs/superpowers/plans/reports/2026-07-12-cloud-observation-ledger-compaction-worker-m.md` append-only。
- Java/schema/resources/tests/Maven/runtime 全部冻结；不修改 A/B/R/host/Task/caller，不做 review 或 Approved。

本回执只确认任务已在领取门内接手，不代表设计返修已完成。

## Internal Worker P2 - Full R0 Design Repair #1 Delta - 2026-07-13T04:35:40-04:00

本 Delta 只关闭 `Parent Full R0 Design Review #5` 的 `P1 x 3 / P2 x 2`，不重写已批准的 M0 八个
DTO 或 `CloudTaskRunRetainedLifecycleActivationAdapter`。实现基线改为 A 已在 Q 日志获得父级
`FINAL APPROVED` 的 scale-wire 最终源码；父级 fresh Cloud package 为 `21/21`，shaded JAR SHA-256 为
`52AC214B7B82A9397F538F31C9F461D80E677F42C7E0382871E597DB4DAAFAD7`。旧文中“等待 A
Implementation APPROVED”的条件至此消失，但本 Worker 仍不实施 Java/schema。

### 1. 单一、可编译的 authority 构造图（关闭 P1-1）

`RemoteTaskRunRoutes` 从纯 static utility 改为 Cloud host 唯一持有的 opaque authority bundle；唯一 public
视图仍只有 `List<CloudApiRoute> routes()`，不暴露 broker、ledger、coordinator、assembly 或 final coordinator。
其 `create(..., GameContext)` 在一个方法内按固定顺序且只执行一次：

```text
RemoteTaskRunRoutes.create(paths, sharedGameContext)
  -> new RemoteTaskRunCoordinator()
  -> new RemoteGameCommandBroker(coordinator, cloudIncarnationId)
  -> new CloudTaskRunActionLedger()
  -> new RemoteFinalConsumptionCoordinator(coordinator, broker, actionLedger)
  -> CloudTaskRunAuthorityAssembly.create(
         coordinator, broker, actionLedger, finalCoordinator, sharedGameContext)
  -> finalCoordinator.receiptIngress()             // package-private opaque capability
  -> PollEndpoint(broker)
  -> OutcomeEndpoint(broker)
  -> FinalConsumedReceiptEndpoint(receiptIngress)
  -> RemoteTaskRunEndpoint(coordinator)
  -> immutable routes
```

`CloudTaskRunAuthorityAssembly.create(...)` 不再自行 new ledger/final coordinator；它逐项以 reference equality
校验 `broker.taskRunCoordinator()==coordinator`、final coordinator 的 broker/ledger/coordinator 与入参同实例，随后
保留现有 one-coordinator-one-assembly claim。`ReceiptEndpoint` 只持 `ReceiptIngress`，不能取得 assembly/ledger/broker；
ordinary outcome endpoint 仍只持 broker。`CloudBrainServer` 仅新增一个强引用字段保存该 opaque bundle，并把
`bundle.routes()` 加入 gateway；不取得任何 authority getter，也不启动 Task/Service。这样不存在 late setter、optional
attach、第二 coordinator 或第二 final coordinator，且 assembly、HTTP ingress 与未来 Service port 位于同一对象图。

### 2. DHXY ledger admission 前的 immutable generation snapshot（关闭 P1-2）

`RemoteTaskRunRegistry` 增加私有构造、package-private 只读的
`CommandAdmissionSnapshot`。snapshot 持有 exact `RegistryEntry` identity、`entryGeneration` 与一次 volatile 读取取得的
immutable `RemoteTaskRunRegistration`；registration 本身包含 exact tenant/user/device/clientSession/taskRun/window/
nativeHandle/processId/playerIdentityEpoch/stopEpoch/runRevision/status。snapshot 不持锁、不含 live HWND 对象，也不允许
caller 构造。

真实方法顺序固定为：

```text
LocalRemoteGameCommandHandler.handle(command)
  -> strict requestDigest + typed payload decode
  -> taskRunRegistry.commandAdmissionSnapshot(clientSession, command)   // lock-free immutable read
  -> operationLedger.claim(command, snapshot)                           // ledger monitor only
       1. exact retained duplicate/compacted witness classification
       2. absent detail 才验证 snapshot provenance + full tuple + exact revision/mode
       3. frontier/gap + detail/input-action + 64/1000 cap
       4. OWNER insert
  -> taskRunRegistry.isCurrent(snapshot, command)                        // lock-free second generation read
  -> existing requireBoundWindow/refreshAndCommit
  -> existing requireRegistration exact fence
  -> operation side effect；input worker 仍保留既有 admission revision fence
```

`claim(RemoteGameCommand, CommandAdmissionSnapshot)` 在任何新 detail/action reservation 前验证 snapshot 与 command 的
所有可表达 tuple；不匹配返回 typed `TASK_RUN_MISMATCH/WRONG_WINDOW` claim status，绝不占 detail。已 retained 的 exact
duplicate 先按 server/local witness 返回，不重插、不执行副作用；不同 digest 仍 conflict。OWNER 后若 snapshot 已失效，handler
产生 `NOT_EXECUTED` terminal 并完成刚建立的 exact detail，不能继续碰窗口。live binding 刷新仍在 claim 后的第二 fence，不能
在 ledger monitor 内执行。

本 admission 路径无嵌套锁：registry snapshot/revalidate 仅做 ConcurrentMap + volatile read；ledger 只持自己的 monitor。
因此不存在 registry-under-ledger，也不存在 ledger-under-registry；`claim` 内禁止调用 registry、window、refresh、queue 或 I/O。
既有 resume-readiness 的已批准逻辑不由本 Delta 重写；Full R0 新增的 frontier/receipt 路径同样只能传 immutable value/opaque
handle，不能向 ledger monitor 注入会回调 registry 的 lambda。

### 3. 单边 restart 明确不支持：authenticated incarnation + 强制协调重启门（关闭 P1-3）

本设计选择“强制双侧协调重启”，不宣称 orphan takeover/rehydration。具体门不是口头 SOP：

1. DHXY `clientSessionId` 被提升为该 DHXY process 的 authenticated incarnation：进程启动只 mint 一次，进程内 poller
   restart 必须复用；新进程必须使用新值。Cloud `RemoteTaskRunCoordinator.OwnerUsage` 同时 retained 首个
   `clientSessionId`；同 tenant/user/device 的不同 session 在该 Cloud process 仍有任何 retained usage 时，PREPARE、
   FIND/STOP replacement 均以现有 typed `SESSION_CONFLICT` 拒绝，且不建立新 run/route/detail。
2. authority bundle 启动时 mint 一次不可空 `cloudIncarnationId`。Cloud `RemoteCommandPollResponse` 的 IDLE、COMMAND、
   FINAL_CONSUMED 三个 variant 都 required echo 该值。DHXY `RemoteOperationLedger.bindCloudIncarnation(...)` 首次绑定后
   永不换绑；`RemoteCommandPollingLoop` 在解释 status/command/control 前先校验，变化即把本进程置为
   `COORDINATED_RESTART_REQUIRED` 并停止当前 loop，handler `claim` 也在该状态下 fail closed。
3. 生产激活顺序强制为：authenticated zero-side-effect poll handshake -> exact incarnation bind -> lifecycle prepare/activate ->
   Task host。host/caller 当前仍 dormant；未实现这道启动顺序前严禁切换。网络暂断后连回同一 Cloud incarnation 可继续；
   incarnation 改变不能自动继续。
4. DHXY 单独重启会产生新 client session，存活 Cloud 因 retained owner fence 拒绝；Cloud 单独重启会产生新 cloud
   incarnation，存活 DHXY 在处理任何新 payload 前停止。唯一收口是运维同时停止两 JVM，确认旧进程终止后一起以两枚新
   incarnation 启动；没有在线 reset API、TTL、quarantine eviction、takeover 或 taskRunId 复用。
5. 存活侧的 pending、UNKNOWN、BUSINESS_CONSUMING、BUSINESS_CONSUMPTION_UNKNOWN、outbox、frontier 与 witness
   继续计入原 64/1000/10000 cap，绝不被解释成执行/未执行结果，也不因新 session 释放。容量耗尽只会继续 fail closed；
   这正是迫使协调重启而非静默复活旧账的可见后果。

`cloudIncarnationId` 由已认证 poll response 携带，不进入 ordinary request/outcome digest，也不修改已批准 M0 ack/receipt；
receipt 打到重启后的空 Cloud 只会因无 server witness 被拒绝。schema 必须明确 body scope 仍需生产 auth 替换/验证，单靠
caller 自报 session/incarnation 不构成认证。

### 4. 现有 long-poll 的 bounded/coalesced wake 与 1:1 fairness（关闭 P2-1）

broker 的 route value 改为 `RouteState`：既有 `ArrayBlockingQueue<PendingCommand>(64)`、固定
`ControlSlot[64]`、`ArrayBlockingQueue<RouteWake>(1)`、`nextLane`。wake queue 是 level-triggered/coalesced signal，
不是 control permit；唯一 permit 仍是 fixed slot。没有新 thread/poller/timer。

方法级算法：

1. command `offer` 成功或 control `RESERVED/SENDING -> QUEUED` 后，在 `stateLock` 内完成状态 mutation，再调用非阻塞
   `routeWake.offer(WAKE)`；queue 已满表示已有 wake，不能丢 readiness。
2. `poll` 每轮先在 `stateLock` 内调用 `selectReadyLocked(route)`：按 `nextLane` 先查 preferred lane，再查另一 lane；control
   cursor 最多扫描 64，command 只 poll 一项并走既有 final dispatch fence。
3. 只有真正返回一个 COMMAND 或 FINAL_CONSUMED 时才把 `nextLane` 翻到另一 lane；authorization rejection/terminal cleanup/
   spurious wake 不翻。两 lane 同时持续 ready 时，交付序列严格 C-F-C-F；一侧空时另一侧可连续前进。
4. 成功选取后若任一 lane 仍 ready，退出 `stateLock` 前再次 `routeWake.offer(WAKE)`，所以并发 long-poll 也不会由一个
   consumer 吞掉合并 token。无 ready 时释放锁，再用现有请求 deadline 对 `routeWake.poll(remainingNanos)` 阻塞；timeout/null
   返回 IDLE。publisher 若发生在“检查后、阻塞前”，容量 1 token 会保留并立即唤醒；发生在阻塞后则直接唤醒。
5. control `QUEUED -> SENDING` 返回 exact generation handle；route endpoint `finally` 调 finish：仍为同 generation 且未收到
   receipt 时 `SENDING -> QUEUED` 并 signal，receipt 已推进为 LOCAL_APPLIED 时 finish no-op。不存在 busy loop 或 unbounded
   wake permits。

### 5. checked consume callback 与 O(1) run accounting（关闭 P2-2）

`CloudTaskServicePort` 内新增 package-private nested contract（不形成 public raw completion API）：

```java
@FunctionalInterface
interface CheckedFinalMutation<O extends RemoteOutcome> {
    FinalConsumptionDisposition apply(O exactOutcome) throws InterruptedException;
}

enum FinalConsumptionDisposition {
    OCCURRENCE_COMPLETE,
    ATTEMPT_RETIRED_FOR_RENEWAL
}
```

三个 package-private operation-specific seam 精确为：

```text
consumeWindowFactFinal(WindowFactAction, WindowFactOutcome,
    CheckedFinalMutation<WindowFactOutcome>) throws InterruptedException
consumeCaptureFinal(CaptureAction, CaptureOutcome,
    CheckedFinalMutation<CaptureOutcome>) throws InterruptedException
consumeInputBundleFinal(InputBundleAction, InputBundleOutcome,
    CheckedFinalMutation<InputBundleOutcome>) throws InterruptedException
```

它们先验证 exact outcome object reference/handle/address/digests，预留 fixed control slot并进入
`BUSINESS_CONSUMING(generation)`，释放 ActionRecord/retirement/ledger/broker locks 后才调用 mutation。正常返回才按 exact
generation retained ack 并进入 NOTICE_PENDING；`InterruptedException`、`RuntimeException`、`Error` 或 post-callback
generation 不确定都先记 `BUSINESS_CONSUMPTION_UNKNOWN`、释放未发布 reservation，再原样抛出；绝不重放 callback。

`CloudTaskRunActionLedger.RunRegistration` 为每个 exact stable run 在 ledger monitor 下维护：

```text
semanticSlotCount
currentDetailCount
unresolvedCount
unknownCount
consumingCount
noticePendingCount
controlOccupiedCount
```

每个 mint/bind/outcome/consume/control/receipt/compact transition 在同一 ledger mutation 用 `Math.addExact/subtractExact`
增减，并同时更新既有 owner/global counters；负数、`controlOccupied>currentDetail` 或 unresolved 分类和不等于 detail 时立即
fail closed。`terminalReady(taskRunId, exactTerminalBinding)` 只做一次 Map lookup 与上述整数比较：
`currentDetailCount==unresolvedCount==unknownCount==consumingCount==noticePendingCount==controlOccupiedCount==0`；
不扫描 10,000 action records。`semanticSlotCount` 只在 exact terminal-ready cleanup 时用于 O(1) quota 退账；现有 cap 数值
不变，不新增 TTL/LRU。

### 6. 修订后的精确写集与实施门

相对 Reconciliation #1，只新增为关闭本轮 blocker 必需的文件：Cloud `CloudBrainServer.java`、
`remote/run/RemoteTaskRunCoordinator.java`；DHXY `RemoteTaskRunRegistry.java`。因此 Full R0 最终写集更新为：

- Cloud Brain：`1 New + 16 Modify`。New 仍只有 `RemoteFinalConsumptionCoordinator.java`；原 14 Modify 全保留，另加
  `CloudBrainServer.java`（只保留 opaque bundle 强引用/取 routes）与 `remote/run/RemoteTaskRunCoordinator.java`
  （owner client-session incarnation fence）。
- DHXY：`0 New + 12 Java Modify + 1 schema Modify`。原 11 Java 全保留，另加
  `RemoteTaskRunRegistry.java`（opaque immutable admission snapshot/current-generation recheck）。
- M0 八 DTO、lifecycle adapter、A 的 `RemoteCaptureOutcomePayload`/`CaptureOutcome`、
  `CloudTaskRunCommandExecutor`、B/R 代码、Task/具体 Service、resources/tests/Maven 仍冻结；A 最终 JCS/scale 实现只能方法级
  合并，不能覆盖。

实施仍是一个双仓原子 cohort：先记录 A 最终源码 hash并重读最新 dirty tree，全部 Java/schema 写入稳定后由父级/获批
implementation owner fresh 运行 Cloud `mvn -q clean package`（不 skip）与 DHXY
`mvn -q -DskipTests compile`。本 P2 只追加本日志，未运行构建、未启动 application/server/host/Task/poller/UI/capture/input，
未执行 Git mutation。

Worker P2 自审：Review #5 五项均有真实 FQCN、方法顺序、锁边界、restart 收口、wake 算法与 checked API；
`P0=0 / P1=0 / P2=0` 仅为 QA，不构成父级 DESIGN APPROVED。

**无已批准业务差异；按基线等价迁移。**

## Parent Full R0 Design Review #6 - PARTIAL PASS / Repair #2 Published - 2026-07-13T04:43:40-04:00

父级对照当前 `RemoteTaskRunRoutes`、`CloudTaskRunAuthorityAssembly`、Cloud broker/coordinator 与 DHXY
registry/handler/ledger 复审。Repair #1 已关闭 Review #5 的 authority 构造岛、claim 前 immutable generation snapshot、单边 restart
合同、checked consume callback 和 O(1) run accounting；这些部分通过且后续不得重开。`cloudIncarnationId` 选择强制双侧协调重启，
没有把 UNKNOWN/CONSUMPTION_UNKNOWN 伪装成结果，也没有引入 takeover/TTL。当前仅剩 control wake 的一个可实施性缺口，结论为
**BLOCKED，P0=0 / P1=1 / P2=0**。

1. **P1：消费 coalesced wake 后，authorization reject/terminal cleanup 可能把仍 ready 的 lane 永久搁置。** Repair #1 第 4 节
   只规定“成功选取后若任一 lane 仍 ready”才 `routeWake.offer(WAKE)`，同时又明确 authorization rejection 不翻 lane。当前 broker
   可能 poll 出一个 stale command，经 final dispatch gate 终结后队列仍有下一 command/control；若本轮已取走唯一 wake token且没有新的
   producer，下一 long-poll 会在 ready work 已存在时阻塞到 timeout。Repair 必须把规则收敛为：每次取得 wake/进入 selector后，在
   `stateLock` 内循环处理无效/terminal candidate；在**任何**退出 stateLock 的路径（成功返回、authorization reject、terminal cleanup、
   control generation mismatch、spurious token）统一重算两 lane readiness，只要任一 ready 就幂等 `offer(WAKE)`。只有真正返回
   `COMMAND/FINAL_CONSUMED` 才翻 `nextLane`；reject/cleanup 不翻但也不丢 level signal。还须说明 route terminal remove 与持有旧
   `RouteState` 的在途 poll：要么 cleanup 仅在无在途 poll/ready 时移除，要么旧 poll 能观察 retired 并重新查当前 route，禁止新 route
   有 work 而旧 poll 睡到 timeout。

### 下一任务：`FULL-R0-DESIGN-R2`（短 Delta）

恢复同一 Internal Worker P2，只在本日志追加领取与一个短 Repair #2 Delta，关闭上述 P1×1；Review #6 已通过的四个大项、M0 DTO、
lifecycle adapter、A scale-wire 均冻结，不得重开。Java/schema/resources/tests/Maven/runtime 继续冻结。Delta 完成后等待父级最终设计复审。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker P2 - CLAIMED - 2026-07-13T04:43:52-04:00

- 任务：`FULL-R0-DESIGN-R2`。
- 领取时间：`2026-07-13T04:43:52-04:00`（P2 当前工作时钟）。
- 唯一写集：仅本固定日志
  `docs/superpowers/plans/reports/2026-07-12-cloud-observation-ledger-compaction-worker-m.md` append-only。
- 本轮只关闭 Review #6 唯一 control-wake P1；已通过的 authority/admission/restart/callback/accounting 不重开。
- Java/schema/resources/tests/Maven/runtime 全部冻结；不做 reviewer 或 Approved 判断。

本回执只确认任务已领取，不代表 Repair #2 已完成。

## Internal Worker P2 - Full R0 Design Repair #2 Delta - 2026-07-13T04:46:32-04:00

本 Delta 只替换 Repair #1 第 4 节的 selector 退出与 route retirement 规则；Review #6 已通过的
authority/admission/restart/callback/accounting、M0 DTO、lifecycle adapter 与 A scale-wire 全部冻结。

### 1. selector 的单一 level-wake 退出纪律

`RouteState` 在既有 `stateLock` 下维护 `inFlightPolls`、`retirementRequested` 与 `retired`。poll 取得 map value 后必须先持
`stateLock` 检查 `retired == false` 并递增 `inFlightPolls`，该注册覆盖本次 selector、等待 wake 及最终 response/异常退出；看到
`retired` 的预读 poll 不得等待旧 `routeWake`，只能释放锁后重新查当前 route。

每次消费 wake 或直接进入 selector，都在 `stateLock` 内循环处理 stale command、authorization reject 后的 terminal cleanup、
control generation mismatch 与 spurious token；无效 candidate 清理后立即继续选择，不先回到 blocking wait。final dispatch authorization
仍在既有权威边界执行；reject 回到同一 selector 循环清理，既不翻 lane，也不吞掉其它 ready work。

所有离开 `stateLock` 的路径共用一个 `finally`：从 command queue 与 fixed control slots **重新计算**
`commandReadyLocked/controlReadyLocked`，只要任一为 true 就幂等 `routeWake.offer(WAKE)`，再 unlock。该 finally 覆盖成功返回、
authorization reject、terminal cleanup、generation mismatch、spurious token、deadline/IDLE、interrupt 与异常；`routeWake.poll(...)`
返回 null 后也必须重新入锁完成这次重算，才能返回 IDLE。只有真实交付 `COMMAND` 或 `FINAL_CONSUMED` 才翻 `nextLane`；其余路径
不翻 lane。于是 token 被任一 poll 消费后，只要 level 仍 ready，就必然在该 poll 离开 selector 前补回容量 1 的 wake。

### 2. terminal route removal 与旧 `RouteState` poll 的竞态收口

terminal cleanup 只设置 `retirementRequested=true`，不得当场从 route map 删除。唯一 `tryRetireLocked` 条件为：既有
`terminalReady(...)` 成立、`inFlightPolls==0`、两 lane 重新计算均不 ready，且没有 SENDING/occupied control；条件不全则保留同一
`RouteState` 并按第 1 节重设 wake。poll 的 outer `finally` 先在 `stateLock` 内递减 `inFlightPolls`、重算/re-arm，再尝试 retirement。

满足条件时在锁内先置 `retired=true`，unlock 后只执行 `routes.remove(routeKey, exactOldState)` 条件移除；不得持 `stateLock` 调 map
compute。所有 producer/poll 在 mutation 或注册前都在锁内检查 `retired`：若 producer 先入锁并发布，ready 会阻止 retirement；若
retirement 先发生，producer 不得写旧 state，而是在锁外条件移除/重查并向当前 state 发布、signal。因为置 `retired` 时
`inFlightPolls` 必为 0，任何已注册旧 poll 都不可能在旧 wake 上睡眠；仅预读旧引用但尚未注册的 poll 会看到 `retired` 并重查。
因此不会出现旧 state 被移除、新 state 已有 work，而旧 poll 仍等到 timeout 的窗口。

Worker P2 自审：Review #6 唯一 P1 已给出单一 unlock/finally、全退出路径 level re-arm 与无旧 poll 的 retirement 协议；
`P0=0 / P1=0 / P2=0` 仅为设计 QA，不构成父级 DESIGN APPROVED。Java/schema/resources/tests/Maven/runtime 未触碰，未运行构建。

**无已批准业务差异；按基线等价迁移。**

## Parent Full R0 Design Review #7 - DESIGN APPROVED / Implementation Published - 2026-07-13T04:50:20-04:00

父级复审 Repair #2 的 selector/rearm 与 route retirement。无效 command/control 在同一 selector 循环清理；每个 stateLock 退出路径均
重算两 lane 并补回容量 1 level wake；真正交付才翻 lane。`inFlightPolls + retirementRequested + retired` 使 route 仅在 terminal-ready、
无在途 poll、无 ready/SENDING/control 时条件移除，旧引用在写入或等待前均会发现 retired 并重查。Review #6 唯一 P1 已关闭。

**Full R0 最终设计结论：DESIGN APPROVED，P0/P1/P2=0。** Review #5/#6 已通过的单一 authority 构造、claim 前 immutable
registration generation snapshot、强制双侧协调重启、checked consume callback、O(1) accounting 与 M0/lifecycle/scale-wire 冻结不重开。

### 下一任务：`FULL-R0-IMP1`

由同一 Internal Worker P2 实施 Repair #1 §6 的最终双仓原子写集：Cloud `1 New + 16 Modify`、DHXY `0 New + 12 Java Modify +
1 schema Modify`。开工先重读两仓最新 dirty tree 与 A scale-wire 最终源码/hash；只做方法级合并，不覆盖 A/B/R/S 或用户在途改动。
不得实施 B warning transport、具体 Task/Service、host activation/caller；不得新增/恢复 tests。先在本日志追加 CLAIMED 与最终逐文件写集，
再修改代码/schema。完成后追加 Implementation #1、scoped diff 与双构建证据：Cloud `mvn -q clean package`（不 skip）和 DHXY
`mvn -q -DskipTests compile`。不启动 application/server/host/Task/poller/UI/capture/input，不做 Git mutation，不作 Approved 判断。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker P2 - CLAIMED - 2026-07-13T04:50:28-04:00

- 任务：`FULL-R0-IMP1`。
- 领取时间：`2026-07-13T04:50:28-04:00`（P2 当前工作时钟）。
- 实施形态：Repair #1 §6 最终双仓原子 cohort；只做方法级合并，保护用户及 A/B/R/S 全部在途改动。
- 禁区：B warning、具体 Task/Service、host activation/caller、tests、运行面与 Git mutation。

### FULL-R0-IMP1 最终逐文件写集

Cloud Brain `1 New + 16 Modify`：

- New：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteFinalConsumptionCoordinator.java`。
- Modify：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RequestContext.java`。
- Modify：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteCommandEnvelope.java`。
- Modify：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteCommandPollResponse.java`。
- Modify：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteProtocolDigests.java`。
- Modify：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/OutcomeCode.java`。
- Modify：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunExecutionGate.java`。
- Modify：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunActionLedger.java`。
- Modify：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRetainedActionState.java`。
- Modify：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServicePort.java`。
- Modify：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServiceExecutionContext.java`。
- Modify：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteGameCommandBroker.java`。
- Modify：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteTaskRunRoutes.java`。
- Modify：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunAuthorityAssembly.java`。
- Modify：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/RemoteCommandOutcomeEnvelope.java`。
- Modify：`src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudBrainServer.java`。
- Modify：`src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/run/RemoteTaskRunCoordinator.java`。

DHXY `0 New + 12 Java Modify + 1 schema Modify`：

- Modify：`src/main/java/com/bot/dhxy/cloud/remote/RemoteGameCommand.java`。
- Modify：`src/main/java/com/bot/dhxy/cloud/remote/RemoteGameOutcomeEnvelope.java`。
- Modify：`src/main/java/com/bot/dhxy/cloud/remote/RemoteProtocolDigests.java`。
- Modify：`src/main/java/com/bot/dhxy/cloud/remote/RemoteCommandPollResponse.java`。
- Modify：`src/main/java/com/bot/dhxy/cloud/remote/RemoteCommandPollStatus.java`。
- Modify：`src/main/java/com/bot/dhxy/cloud/remote/RemoteCommandTransport.java`。
- Modify：`src/main/java/com/bot/dhxy/cloud/remote/HttpRemoteCommandTransport.java`。
- Modify：`src/main/java/com/bot/dhxy/cloud/remote/RemoteCommandPollingLoop.java`。
- Modify：`src/main/java/com/bot/dhxy/cloud/remote/RemoteOperationLedger.java`。
- Modify：`src/main/java/com/bot/dhxy/cloud/remote/LocalRemoteGameCommandHandler.java`。
- Modify：`src/main/java/com/bot/dhxy/cloud/remote/RemoteOutcomeCode.java`。
- Modify：`src/main/java/com/bot/dhxy/cloud/remote/RemoteTaskRunRegistry.java`。
- Schema Modify：`docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`。

M0 八 DTO、lifecycle adapter、A-only capture DTO、`CloudTaskRunCommandExecutor`、B/R/S 文件、Task/具体 Service、resources/tests/Maven
均不在写集。领取完成后先重读两仓 dirty tree 与 A scale-wire 最终源码/hash，再开始任何源码修改。

## Internal Worker P2 - Implementation #1 - 2026-07-13T06:16:08-04:00

`FULL-R0-IMP1` 已按 Parent Full R0 Design Review #7 批准的双仓原子 cohort 落地。本节只报告实现与 Worker QA，
不作 `Approved` 判断。

### 1. 基线与写集保护

- 开工复读两仓 dirty tree：DHXY `thin-client-design@0114604e1ff5f15491d2910959c45252e893d04f`，Cloud
  `navigation-migration@3b988caa010254973e03342272e6d1d6a9685b01`；两仓已有大量 dirty/untracked，全部保留，未 reset、
  checkout、add、commit 或覆盖。
- A scale-wire 的批准证据仍为 Q 日志中的 shaded JAR
  `52AC214B7B82A9397F538F31C9F461D80E677F42C7E0382871E597DB4DAAFAD7`。本轮在 A 最新源码上方法级合并：
  Cloud/DHXY canonical floating 分支都仍唯一调用
  `org.erdtman.jcs.NumberToJSON.serializeNumber(double)`；两仓 `pom.xml` 未由本 Worker 修改。
- 实际写集与 CLAIMED 清单一致：Cloud 17/17 文件存在（1 New + 16 Modify），DHXY 13/13 文件存在
  （12 Java Modify + 1 schema Modify）。未修改 M0 八 DTO、lifecycle adapter、A-only capture DTO、
  `CloudTaskRunCommandExecutor`、B/R/S 专属文件、具体 Task/Service、resources 或 tests。

### 2. Cloud 实现

- required `RemoteSemanticAddress(phaseCode,actionSlot,occurrence,attempt)` 已进入 `RequestContext`、command envelope、
  request digest 与 outer outcome correlation；普通 typed outcome digest 仍按 A 批准公式，不把 outer address 塞进 common digest。
- `CloudTaskRetainedActionState + CloudTaskRunActionLedger` 成为 occurrence/attempt 唯一 owner：每个 stable run 自持
  semantic frontier/current detail/observation/detail counters；ACTIVE/PAUSED mode 双向互斥、无 gap、PAUSED 无 renewal；
  terminal-ready 时 O(1) 删除整 run bucket并精确退 semantic-slot quota。
- `RemoteFinalConsumptionCoordinator` 在 fixed control permit 成功后执行 checked business callback；正常返回才发布 retained
  final-consumed ack，异常进入不可重放 consumption-unknown。ack/receipt 对 exact request/address/outcome/frontier 双阶段收口。
- broker 的 semantic slot 使用完整 scope/taskRun/window/stopEpoch/operation/phase/slot key；入队、final dispatch、outcome、
  duplicate witness、receipt compact 都先命中 server-owned detail/witness再比较 caller echo。per-run broker bucket保留最新 compacted
  witness，64 control slots及 owner/global cap fail-closed。
- command/control 同 route 1:1 公平；selector 所有退出路径重算并 re-arm level wake；route retirement 要求 terminal-ready、
  无 ready/SENDING/control 且 `inFlightPolls==0`，使用 `routes.remove(key, exactOldState)` 防旧 poll/新 route 竞态。
- `RemoteTaskRunRoutes` 组装唯一 coordinator/broker/action-ledger/final-coordinator/assembly 对象图；Cloud server只强引用该
  opaque bundle并挂载批准的 transport ingress，没有启动具体 Task、Service host或 caller。owner client-session 与
  `cloudIncarnationId` 实施强制协调重启门。

### 3. DHXY 实现

- command/outcome DTO required echo semantic address；strict transport 校验 address/revision/mode、closed
  `IDLE/COMMAND/FINAL_CONSUMED` payload 与 required cloud incarnation。receipt POST 使用既有 outcome path派生 endpoint，
  网络不确定不在 transport 内重试。
- polling loop 每轮最多先发送一个 retained FIFO receipt，再做一次 poll；FINAL_CONSUMED只进入本地 ledger，不进入机械
  handler。receipt permanent reject 保留 `REJECTED_RETAINED`，uncertain 回 READY 交外层下一轮；每轮最多检查一个 terminal
  cleanup candidate。
- local ledger 以 per-run bucket持有 semantic frontier/current detail/receipt outbox；ack apply 在一个 monitor mutation 中验证
  exact tuple、推进 frontier、删除 request/input detail并发布 bounded receipt。terminal/absent registry observation 在 ledger锁外，
  exact-generation CAS 后 O(1) 删除 run bucket并退 quota。
- `RemoteTaskRunRegistry` 提供不可伪造 immutable admission snapshot/current-generation recheck；handler 在 ledger admission 前取
  snapshot，claim 后、任何截图/fact/input副作用前再次验证，input worker原有 revision fence继续保留。没有
  registry-under-ledger 或 ledger-under-registry。
- schema 在 A 的 closed capture/systemScaleRatio/JCS 内容上只追加 semantic address、20项同位 outcome enum、poll control、
  ack/receipt/frontier/restart/cleanup合同；`contractVersion` 保持 1，明确不支持 mixed-version忽略字段。

### 4. scoped diff / wire QA

- `git diff --check` 对批准写集无 whitespace error（仅 Git 的未来 LF/CRLF 提示）。两仓 `src/test` scoped status均为空；
  target Java 扫描无 `TODO/FIXME/UnsupportedOperationException/placeholder/not implemented`。
- 双仓 `OutcomeCode` 枚举逐项同位，共 20 项；`RemoteSemanticAddress` JSON order 4/4 完全一致；
  `RemoteFinalConsumedAck` 21/21、`RemoteFinalConsumedReceipt` 15/15、`RemoteFinalConsumedReceiptAck` 5/5 property order完全一致。
- 双仓 request digest都包含 required semantic address，ack/receipt digest都严格只排除自身 digest字段；双仓 JCS floating
  generator均为 A 批准的 `NumberToJSON.serializeNumber(value)`。当前 digest源码 SHA-256：Cloud
  `1e76581549a29295d7acd5fc579d69b2cec1b8760dc7c4d0cab57c954dd52872`，DHXY
  `e5f69f4b0c91bd705696eb0e6a7d5db0724e55330a9da9a83bdb1ffabea124a7`。
- 由于两仓 remote tree在父级基线中整体仍为 untracked，Git index不能提供这些文件的逐行 baseline diff；CLAIMED 节已列出
  全部精确路径，本轮另逐文件核对 17+13 路径存在、bytes与 SHA-256，并以 clean build作为可编译闭包证据。

### 5. fresh 构建证据

- Cloud：`mvn -q clean package`，`exit 0`，耗时 68.5s；Surefire 4 suites / 21 tests，failures=0、errors=0、
  skipped=0。新 shaded JAR：`target/dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`，119851760 bytes，SHA-256
  `6C9D7CBF3BF9D9420A3F5AB29C83DA295CD0D20809646FCC17244071F7B1A7CB`。
- DHXY：`mvn -q -DskipTests compile`，`exit 0`，耗时 2.9s。按 no-local-test规则未创建、恢复或运行 DHXY tests。
- 未启动 application/server/host/Task/poller/UI/capture/input，未发送截图或输入，未执行生产切换、凭据、不可逆删除或
  Git mutation。

Worker P2 QA：实现与构建材料已交父级复审；本 Worker **不作 Approved 判断**。

**无已批准业务差异；按基线等价迁移。**

## Parent Implementation Review #1 - BLOCKED - 2026-07-13T06:33:00-04:00

父级独立复核 Full R0 双仓实现与 Worker 构建材料。wire 枚举、semantic address、local admission snapshot、input worker revision fence、local receipt outbox 与 coordinated restart 主路径成立，不重开；但 Cloud 最终消费与 receipt compaction 仍有两处跨账本半提交，当前 `P0=0 / P1=2 / P2=0`，不得 FINAL APPROVED，也不得据此统一构建收口：

1. **P1：业务 callback 成功后存在无法回滚的 `BUSINESS_CONSUMING/RESERVED` 或“action 已改账、control 未发布”半状态。** `RemoteFinalConsumptionCoordinator.java:47-64` 只在 callback/`requireDisposition` 抛错时调用 `failConsumption`；`buildAck` 与 `completeBusinessConsumption` 位于 catch 之外。`CloudTaskRunActionLedger.java:655-684` 又先把 action/observation 改为 `BUSINESS_CONSUMED_NOTICE_PENDING`、先改 consuming/notice/control 计数，最后才调用 broker publish。ACK digest 构造、二次校验或 broker publish 任一步失败，reservation/control slot 会泄漏或两边状态分叉；后续重入既不能安全 unknown，也可能永久占 quota。返修必须把 ACK 构造失败纳入 unknown+release，并把 publish+ledger phase/counter 变成“全部 prevalidate/precompute 后的不可抛提交”；失败前零 mutation，失败后 exact reservation 必须落 `BUSINESS_CONSUMPTION_UNKNOWN` 且 control reservation 被释放。不得让 `markBusinessConsumptionUnknown` 再撞上已经改成 NOTICE_PENDING 的半状态。
2. **P1：receipt 接收/compaction 先后修改 broker 与 action ledger，失败被永久 `REJECTED`，可形成不可恢复分叉。** `RemoteFinalConsumptionCoordinator.java:77-105` 先 `broker.acceptFinalConsumedReceipt` 把 slot 置 `LOCAL_APPLIED`，再 `actionLedger.commitCompaction`；所有 `IllegalStateException` 最终都返回 permanent `REJECTED`。而 `CloudTaskRunActionLedger.java:743-813` 在自己的再次校验中先调用 `broker.compactFinalConsumedControl`，随后才改 frontier/detail/counters。于是 action 二次校验失败会留下 broker `LOCAL_APPLIED`；broker compact 成功后 action mutation 若失败则 broker 已删除 detail/control，action 仍 current。DHXY `RemoteCommandPollingLoop.java:220-229` 对 permanent reject 会把 receipt outbox 保留为拒绝态，不再形成可恢复闭环。返修须提供一个 exact receipt transaction：两侧先只读 prevalidate/precompute；一旦开始 commit，broker/action 的 mutation 必须是不会再抛的确定序列。任何已进入 `LOCAL_APPLIED` 但尚未双侧 compact 的异常必须向 transport 表达 retryable/uncertain（HTTP 非永久拒绝）并让同一 receipt 重投可继续完成；不得把内部半提交伪装为 client `INVALID_REQUEST/REJECTED`。

### 返修验收条件 `FULL-R0-IMP1-R1`

- 只由原 Internal Worker P2 返修现有批准写集，不扩大到 A/B/T、业务 Service、host/caller/tests；保持 `contractVersion=1` 与现有 wire 字段/枚举/digest 不变，除非修复确有不可避免的 closed internal status（优先不改 wire）。
- 给出两条故障注入式时序说明：`callback success -> ack/publish failure` 与 `LOCAL_APPLIED -> action compaction failure -> same receipt retry`，逐步证明没有 quota/control/detail 泄漏、没有重复 business mutation、没有永久 reject 半状态。
- 写入稳定后 Worker 重跑 Cloud `mvn -q clean package`（不 skip）与 DHXY `mvn -q -DskipTests compile`；父级随后仍会 fresh 重跑。Worker 自审不算 Approved。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker P2 - CLAIMED - 2026-07-13T06:36:31-04:00

- 任务：`FULL-R0-IMP1-R1`。
- 领取时间：`2026-07-13T06:36:31-04:00`。
- 返修范围：只关闭 Parent Implementation Review #1 的 `P1 x 2`；已通过的 wire、semantic address、local admission/input fence、receipt outbox 与 coordinated restart 不重开。
- 预期唯一 Java 写集：Cloud
  `RemoteFinalConsumptionCoordinator.java`、`CloudTaskRunActionLedger.java`、`RemoteGameCommandBroker.java`；
  DHXY Java、schema、wire/digest/version、A/B/T、业务 Service、host/caller/tests 全冻结。唯一文档写集为本固定日志 append-only。
- 约束：保护两仓全部 dirty/untracked，不回滚、不覆盖、不提交；不启动任何运行面。

本回执只确认返修已领取，不代表完成或 Approved。

## Internal Worker P2 - Implementation Repair #1 - 2026-07-13T07:05:25-04:00

`FULL-R0-IMP1-R1` 已只在 CLAIMED 的 Cloud 三文件写集内完成；本节是实现与 Worker QA，**不作 Approved 判断**。

### 1. 精确写集与冻结面

- `RemoteFinalConsumptionCoordinator.java`：9115 bytes，SHA-256
  `017FE057D3F4E0783732F1B37C951E721141123550690B1804C4E56EFC7A23FA`。
- `CloudTaskRunActionLedger.java`：83178 bytes，SHA-256
  `47FAD3EE799EB267312CD6E4E694589A8889122497FD6CD6B576E1124E15C4DB`。
- `RemoteGameCommandBroker.java`：110245 bytes，SHA-256
  `46A4C8BC60CC75FEB783B1FC13236C0E1C6EF6DC1B8A3C5253599F03AE2C5DC2`。
- DHXY Java/schema、两仓 wire DTO、digest、`contractVersion`、A/B/T、业务 Service、host/caller/tests 均未修改；
  `src/test` scoped status 为空，三文件 trailing-whitespace 扫描为 0。两仓既有 dirty/untracked 全部保留，无回滚、覆盖或 Git mutation。

### 2. P1-1：callback / ACK / publish 原子收口

- `consumeFinal` 现在把 `requireDisposition`、ACK 构造与 `completeBusinessConsumption` 放在同一失败兜底内；ACK digest 构造或
  publication prevalidation 任一步异常都进入 exact `failConsumption`。
- ledger 在任何 phase/counter mutation 前先算出并校验 projected consuming/notice/control 计数，再从 broker 取得 exact
  `ControlPublication`；提交段只赋 `retainedAck + NOTICE_PENDING + projected counters`，随后执行无二次校验/无算术的 QUEUED publish。
  ledger monitor 未释放前 control 仍为 RESERVED，poll 不可见；publish 后方法才返回，不存在可观察的 NOTICE_PENDING 未发布窗口。
- UNKNOWN 路径同样先预计算 ledger 计数与 broker `ControlClearPlan`；提交只把 exact reservation 置
  `BUSINESS_CONSUMPTION_UNKNOWN`、写 projected counters、清 RESERVED slot及其 global/owner quota。cancel 路径也改为先算后写。

**故障注入时序 A：`callback success -> ack/publish failure`**

1. business callback 只执行一次；此时 action=`BUSINESS_CONSUMING`、control=`RESERVED`，control quota 已有且尚不可投递。
2. 若 ACK 构造失败，或 ACK/control/计数 prevalidation 失败，ledger 尚未写 NOTICE_PENDING；catch 进入 exact failure transaction。
3. failure transaction 先验证 UNKNOWN projected counts及 RESERVED clear plan，随后不可抛提交 UNKNOWN + control clear；
   consuming--、unknown++，broker global/owner control quota 各精确退 1，不留 RESERVED、NOTICE_PENDING或 QUEUED。
4. UNKNOWN 是不可自动重放终态，因此不会再次调用已成功的 business callback。若 prevalidation 全通过，则 NOTICE_PENDING 与
   broker QUEUED 在同一锁序事务中提交，提交段没有新的校验/算术失败点。

### 3. P1-2：LOCAL_APPLIED 到双 compact 可续办事务

- receipt 的永久 `REJECTED` 现在只允许发生在 broker 首次 LOCAL_APPLIED 之前。若 exact receipt 已处于 LOCAL_APPLIED或已有
  compacted witness，任何后续内部异常抛 `ReceiptCompactionUncertainException`，不会生成永久 receipt REJECTED；现有 DHXY
  transport 因无 2xx receipt ack而保留 READY并重投同一 bytes。
- broker 对 LOCAL_APPLIED 的重复提交只接受 record-equal 的原 receipt；不同 receipt 在任何 mutation 前拒绝。compact 前一次性
  prevalidate/precompute detail/request/action、frontier、owner/global/control计数、terminal cleanup和route retirement。
- commit 在 broker `stateLock` 内先落 broker core与 exact compacted witness，并置
  `compactionAwaitingActionCommit`；随后执行已预计算、无校验/无算术的 action-ledger commit；只有 action成功才清 marker并执行
  terminal cleanup/route retirement。marker期间 terminal cleanup与下一 semantic admission都被阻断，witness不会被清掉或覆盖。
- action current + broker duplicate 已成为合法续办态：broker严格核对 scope/taskRun/address/request/outcome/ack/receipt witness后，
  执行原 action commit；action duplicate + broker duplicate只做 exact witness校验，不重放 business mutation。

**故障注入时序 B：`LOCAL_APPLIED -> action compaction failure -> same receipt retry`**

1. 首次 receipt 通过 action只读计划与broker校验后，slot变为 LOCAL_APPLIED；从此异常只能向transport表现为 uncertain。
2. broker commit先移除自己的 current detail/control、写frontier/witness并精确退core计数，同时保留run bucket且marker=true。
3. 在 action commit 边界注入异常：action detail/frontier/counters仍为 current；broker witness与一致core计数保留，terminal cleanup和
   下一 semantic admission均被marker阻断；coordinator抛uncertain，不返回永久REJECTED。
4. 同一 receipt 重投：action重建同一 current plan；broker由 exact witness返回 duplicate acceptance；同一持锁事务完成action
   frontier/detail/counters，marker=false后才执行已预计算的broker terminal cleanup/route retirement。
5. receipt入口不经过 `consumeFinal`，因此business callback不重复；control/detail/quota各只退一次，最终两账本同为compacted。

### 4. scoped QA 与强制构建

- Cloud：`mvn -q clean package`，exit 0，68.3s；Surefire 4 suites / 21 tests，failures=0、errors=0、skipped=0。
- shaded JAR：`target/dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`，119869581 bytes，SHA-256
  `4C81F22DDA86F7981902CBA9D0D06F1050CBAE03FBB1E92B62A58A6732D2F10F`。
- DHXY：`mvn -q -DskipTests compile`，exit 0，3.9s；按 no-local-test 规则未创建、恢复或运行 DHXY tests。
- 未启动 application/server/host/Task/poller/UI/capture/input，未发送截图或输入，未执行生产切换、凭据、不可逆删除或提交。

Worker P2 自审：Parent Review #1 的 `P1 x 2` 已按上述事务边界关闭，当前 `P0=0 / P1=0 / P2=0` 仅为 Worker QA；
仍等待父级独立源码复审与 fresh 构建，不构成 `Approved`。

**无已批准业务差异；按基线等价迁移。**

## Parent Implementation Review #2 - FINAL APPROVED - 2026-07-13T07:18:00-04:00

父级独立复审 `FULL-R0-IMP1-R1` 最新三文件与双仓 transport 链路，结论：**FINAL APPROVED，P0=0/P1=0/P2=0**。

- callback/ACK/publish：`RemoteFinalConsumptionCoordinator.consumeFinal` 在同一失败兜底内完成 disposition、ACK 与
  `completeBusinessConsumption`；`CloudTaskRunActionLedger:655-724` 先预计算 ledger 计数并取得 broker publication/clear
  plan，再提交 `NOTICE_PENDING + QUEUED` 或 `BUSINESS_CONSUMPTION_UNKNOWN + clear reservation/quota`。broker control 在
  publication 前仍为不可 poll 的 `RESERVED`，提交段仅赋值与 level-wake，不存在可观察半发布窗口。
- receipt/compaction：`RemoteFinalConsumptionCoordinator:75-116` 在 broker 已 `LOCAL_APPLIED` 或存在 compacted witness 后，
  后续内部异常只抛 `ReceiptCompactionUncertainException`，不返回永久 `REJECTED`。`RemoteGameCommandBroker:362-394`
  先写 broker core + exact witness + `compactionAwaitingActionCommit`，再执行预计算 action commit；同一 receipt 重投可由
  broker duplicate witness 续办 action commit，business callback 不会重进，quota/detail/frontier 只退一次。
- 端到端失败归类：Cloud `RemoteTaskRunRoutes:125-139` 不吞上述 runtime exception；gateway 只把 JSON 错误转成 400，
  因此不确定压缩不会产生 2xx receipt ACK。本地 `HttpRemoteCommandTransport:138-164,166-216` 将断连/非 2xx 变成
  transport failure，`RemoteCommandPollingLoop:210-244` 将 IO/timeout/5xx/反序列化等标成 delivery uncertain，保留原
  receipt bytes 供后续同 ID 重投，只有明确 4xx 合同拒绝才永久拒绝。
- 父级 fresh 验证：Cloud `mvn -q clean package` exit 0，4 suites / 21 tests，failures=0、errors=0、skipped=0；shaded
  JAR 119869581 bytes，SHA-256 `6E06C0FD105E27EFF2E3E855DD2C46D0E590D41A49E13FC51E329E6560E9CEB4`。
  DHXY `mvn -q -DskipTests compile` exit 0；未新增、恢复或运行 DHXY tests。

本切片现已闭合；host/caller 仍 dormant，未启动任何 application/server/Task/poller/UI/capture/input，未执行生产切换或
Git mutation。**无已批准业务差异；按基线等价迁移。**
