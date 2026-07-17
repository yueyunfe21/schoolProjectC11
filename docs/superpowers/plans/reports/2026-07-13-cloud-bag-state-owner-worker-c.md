# Cloud Bag State Owner - External Worker C

## Parent Implementation Task #1 - `W-BAG-C0-IMP1` - 2026-07-13T17:07:00-04:00

External Worker C 负责已批准的 Bag state-core 独立波。先完整读取：

- `D:\mavenProject\DHXY\AGENTS.md`
- `D:\mavenProject\DHXY\docs\DHXY_CONTEXT.md`
- `D:\mavenProject\DHXY\docs\ACTIVE_WORK.md` 顶部 CR271
- `D:\mavenProject\DHXY\docs\superpowers\specs\2026-07-12-service-migration-matrix.md`
- `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-bag-service-worker-s.md` 全文，尤其 `Parent Source/Design Review #3 - SOURCE APPROVED / DESIGN APPROVED`
- DHXY committed HEAD `0114604e` 的 `BagService.java` 与相关 cache/session model
- 两仓最新 `git status`

### 领取门

必须在 `2026-07-13T17:27:00-04:00` 前于本文件真实 EOF 追加：

```text
## External Worker C - CLAIMED - <timestamp>
- task: W-BAG-C0-IMP1
- claimedAt: <timestamp>
- uniqueWriteSet: <逐文件列出下述两份 New Java + 本日志>
```

20 分钟只检查是否领取，不检查是否完成；已领取后可以持续工作。未领取时父级只重发给 C，不交给内部 Worker。

### 唯一 Java 写集

仅允许新建：

1. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\bag\CloudBagStateOwner.java`
2. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\bag\BagWorkflowState.java`

以及 append-only 写本日志。目标若已存在立即停止并报告，不覆盖。禁止修改 assembly/context/remote/wire/schema/BagService/host/caller/DHXY/tests/Maven/resources；不得触碰 External A 的 NpcClick、External B 的 TeamReturn、Internal AB 的 RX3 写集。

### 实现不变量

1. 严格实现 Bag 报告已父级批准的 `W-BAG-C0`，不得重开已批准设计；以 `0114604e` 为业务基线，工作区脏 `BagService.java` 不作权威。
2. `CloudBagStateOwner` 是未来由 authority assembly 单次持有的 tenant/user scoped owner；本波只实现 state-core，不接线。不得 static/ThreadLocal/JVM global/default state/第二 registry。
3. 保留批准的三张 Cloud cache 与 exact stable key、geometry stream、单调 observation sequence、`< / == / >` 接收规则、same-sequence exact replay、geometry generation 原子 invalidation。不得 TTL/LRU/takeover/retry/额外 probe。
4. owner lock 内不得 remote wait、capture、I/O、callback；observation handle 必须 owner-private、不可由业务 caller 铸造，并绑定 exact scope/taskRun/window/stopEpoch/runRevision/action identity/sequence。
5. `BagWorkflowState` 只持批准的 per-runtime business cursor、一次性随机决定和 opaque handle 引用；不复制 cache/generation owner，不持 HWND/Path/image/input queue，不读 ambient current。
6. 不改变 HEAD 页序、clamp、随机边界、cache 命中/失效、normal/direct 或 pause/stop/retry/fallback 语义。无已批准业务差异。
7. 不新增/恢复测试，不启动 application/server/host/Task/poller/UI/capture/input，不做 Git mutation。

### 交付门

在本文件真实 EOF 追加 `Implementation #1`，列出精确 diff、stable-key 表、并发/乱序矩阵、SHA-256 与 scope self-QA。Internal AB 正连续写共享 Cloud/DHXY Java 时不要并发执行 `mvn clean`；源码稳定后由父级安排独占 Cloud `mvn -q clean package`（不可 skip）并做唯一批准。Worker 自审不算 Approved。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-13T17:12:51-04:00

- task: W-BAG-C0-IMP1
- claimedAt: 2026-07-13T17:12:51-04:00
- uniqueWriteSet:
  1. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\bag\CloudBagStateOwner.java`（新建）
  2. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\bag\BagWorkflowState.java`（新建）
  3. `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-bag-state-owner-worker-c.md`（append-only 本日志）

## External Worker C - Implementation #1 - 2026-07-13T17:21:07-04:00

### 精确 diff（唯一 Java 写集，全部为 New，无任何既有文件修改）

| 文件 | New/Modify | 行数 | 内容 |
|---|---|---:|---|
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\bag\CloudBagStateOwner.java` | New（目标领取前不存在，已核对） | 493 | 三张 Cloud cache 唯一 owner + per-(scope,window) geometry stream + owner lock + 文件底部 owner-private opaque `BagBoundBaseObservationHandle`（private 构造，业务 caller 不可铸造） |
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\bag\BagWorkflowState.java` | New（目标领取前不存在，已核对） | 287 | 按 D4 修订：retained open-stage/page cursor/session-op ordinal、一次性随机决定 freeze-once、opaque handle 引用；不复制 cache/generation/sequence owner |
| 本日志 | append-only | — | 仅追加 CLAIMED 与本节 |

未触碰 assembly/context/remote/wire/schema/BagService/host/caller/DHXY/tests/Maven/resources；未触碰 External A（NpcClick）、External B（TeamReturn）、Internal AB（RX3）写集；无 Git mutation。

### stable-key 表（与批准 R1.1/D3-2/D3-3 对照）

| 状态 | exact key | value | lifetime / 失效 |
|---|---|---|---|
| `visiblePageCache` | `(RemoteTaskRunScope 完整四元, logicalWindowId, layoutKey)` | `Integer 0..4`（写入时校验，页 5 fail-closed 拒绝） | 无 TTL/LRU；跨 taskRun/binding 保留；仅 scope teardown 按完整 scope equality 删除 |
| `itemPageCache` | `(scope, logicalWindowId, layoutKey, canonicalTemplateId)` | `Integer 0..4`（同上校验） | 同上 |
| `mainBagAnchorCache` | `(scope, RemoteTaskRunWindow 完整 tuple, geometryGeneration)` | `MainBagAnchor(clientX, clientY, systemScaleRatio, geometryGeneration, observationSequence, BoundBaseGeometry)` | generation 前进时在同一 owner lock 内原子删除旧-generation 项；`readCurrentMainBagAnchor` 只按当前 generation 命中；无 TTL/二次验证 |
| geometry stream | `(scope, RemoteTaskRunWindow 完整 tuple)` | `nextObservationSequence / acceptedObservationSequence / acceptedBoundBase / geometryGeneration` | 跨 per-runtime/resume 保留；仅 scope teardown 删除 |
| observation handle | owner-private，绑定 `ownerInstanceId + scope + taskRunId + window tuple + stopEpoch + runRevision + actionRecordId + observationSequence` | — | private 构造仅 owner 铸造；字段全 private 不外露 |

teardown 面仅 `removeScopeState(RemoteTaskRunScope)`（owner lock 内完整 scope equality）；无无参 `clear()`、无按 windowId/taskRunId 清理、无跨 tenant 通道。

### 并发/乱序矩阵（`acceptBoundBaseObservation`）

| 场景 | 判定 | mutation |
|---|---|---|
| `revalidate()` 非 allowed / binding revision != context revision（stale/pause 后旧 revision） | `DISCARDED_FOREIGN_OR_STALE_CONTEXT` | 0 |
| handle 的 scope/taskRunId/window/stopEpoch/runRevision 任一 != 当前 exact context | 同上 | 0 |
| outcome actionRecordId != handle 绑定值，或 observedWindow != handle window（correlation 不等） | 同上 | 0 |
| scope teardown 后 stream 已不存在 | 同上 | 0 |
| `seq < accepted`（晚到旧 fact） | `DISCARDED_STALE_SEQUENCE` | 0 |
| `seq == accepted` 且 base 逐字段相等 | `IDEMPOTENT_REPLAY` | 0 |
| `seq == accepted` 且 base 不同（冲突 replay） | `DISCARDED_CONFLICTING_REPLAY` | 0 |
| `seq > accepted` 且 geometry 与 accepted base 相同 | `ACCEPTED_FENCE_ADVANCED` | 仅推进 fence（sequence/base），generation 不变 |
| `seq > accepted` 且 geometry 不同 | `ACCEPTED_GEOMETRY_ADVANCED` | 同一 lock 内原子：推进 fence → `geometryGeneration++`（`Math.incrementExact`）→ 删除同 exact scope/window 的旧-generation anchor；全部校验先于任何 mutation，无半更新 |
| 分配（`beginBoundBaseObservation`） | lock 内 `revalidate()` 必须 current-ACTIVE（否则 throw，不铸 handle）；sequence 严格递增 | 仅 `nextObservationSequence++` |

因此 N+1 先返回成为 fence 后，晚到的 N 必被 `DISCARDED_STALE_SEQUENCE`；resume 后旧 revision 即使携更大 sequence 也先被 exact-current 检查丢弃；新 revision 沿同一 stream 的下一 sequence 继续（owner 对象不随 runtime 重建）。owner lock 内只有 map/字段操作与 read-only `revalidate()`（"never waits or mutates lifecycle"，已核对其 JavaDoc）——无 remote wait、capture、I/O、callback。

### `BagWorkflowState` 合同要点

- 绑定 exact `(scope, taskRunId, window, stopEpoch)`（构造注入，不读 ambient current）；跨 runRevision 保留。
- open-stage cursor 固定四阶段 `INITIAL -> AFTER_ALT_E_FIRST -> AFTER_ALT_E_LATE_RENDER -> AFTER_ALT_E_SECOND`，只允许 +1 前进（无第三次 toggle）；page pass freeze-once（相同 order 幂等、不同 order fail-closed、页界 0..5 校验）；`session-op` ordinal 自 1 起，仅 final-consumption 前进——`UNKNOWN 不前进` 为调用合同并已写入 JavaDoc。
- 一次性随机决定 `freezePointOnce(occurrence, actionSlot, x, y)`：同 key 首写冻结、重投只读冻结值；更新 occurrence 原位覆盖 slot（结构性删除点，存储按固定 slot 名集合有界）；旧 occurrence fail-closed。无 TTL/LRU。
- 仅引用 owner-minted opaque handle（record/read/clear），不复制 sequence/generation；无 HWND/Path/image/input queue 字段；不 mint 任何 occurrence/session/request/action identity。

### SHA-256

| 文件 | SHA-256 |
|---|---|
| `CloudBagStateOwner.java` | `1b8c5379d5f75dc9dfc9f7e108e6dc12664c430055f0f22dfd23b5d5d7283821` |
| `BagWorkflowState.java` | `a9636934326d684da9810f47cf6bd24a563b7f20177c98f605c3af3c1ef09ddc` |

### scope self-QA（仅 QA，不构成 Approved）

1. 写集：仅上述两个 New 文件 + 本日志 append；领取前已核对两个目标路径不存在（`TARGET_DIR_ABSENT`）。
2. 无 static/ThreadLocal/JVM global/default state/第二 registry：两类均实例字段；owner 无 runtime/service lookup API。
3. 三张 cache/stream/`</==/>`/same-sequence exact replay/原子 generation invalidation 如上表；无 TTL/LRU/takeover/retry/额外 probe（全类无任何时间读取、重试或探测逻辑）。
4. handle owner-private（private 构造 + 全 private 字段 + `ownerInstanceId` 归属校验）；绑定 exact scope/taskRun/window/stopEpoch/runRevision/action identity/sequence。
5. `BagWorkflowState` 不持 cache/generation owner 副本；不改变 HEAD 页序/clamp/随机边界/cache 命中失效/normal-direct/pause-stop-retry-fallback 语义——本波不含任何编排，仅状态存储与冻结语义，页界与四阶段常量取自已批准设计。
6. 未新增/恢复任何测试；未启动 application/server/host/Task/poller/UI/capture/input；无 Git mutation。
7. 构建门禁遵守：Internal AB 在写共享 Java，本轮**未运行任何 Maven 命令**；仅用既有 `target/classes` 对两个新文件做只读 `javac` 冒烟（exit 0，未触碰共享构建产物），正式独占 Cloud `mvn -q clean package` 待父级安排并作唯一批准。
8. 无已批准业务差异；按 `0114604e` 基线等价迁移（依据 worker-s 报告 `Parent Source/Design Review #3 - SOURCE APPROVED / DESIGN APPROVED` 冻结合同实施）。

External Worker C 交付完毕，持续 5 分钟重读本日志等待父级 review；自审不算 Approved。

## Parent Source Review #2 - BLOCKED / `W-BAG-C0-IMP1-R2` Published - 2026-07-13T17:52:00-04:00

父级逐行复审 Repair #1 两个 New Java，并对照已批准 Bag D3 的 Full-R0 occurrence/retained-address 合同与当前
`TaskTransactionAction`、`CloudTaskServicePort.WindowFactAction` 实现。R1 已正确关闭 pending 覆盖、数值半更新与
template traversal；但 workflow mutation 仍可由 stale runtime 或业务 caller 直接推进，且 action 语义仍可被替换。
结论 **BLOCKED，P0=0/P1=3/P2=2**；不作 SOURCE APPROVED，不运行并发 Maven。

### P1-1：同一 opaque flow handle 跨 resume 复用后，旧 revision 仍可推进/结束当前流程

- **证据：**`BagWorkflowState:110,150,171,195,252,270,296,318,377` 的 begin/advance/finish/ordinal/random/pending
  mutation 均不接 `TaskExecutionContext` 或 current-revision capability。R1 让 resume 复用同一 `OpenFlowHandle/PagePassHandle`，
  因此旧 runtime 与新 runtime 持有的是同一对象；对象同一性无法区分 stale revision。
- **影响：**pause/resume 后旧栈的 late `finally` 可 `finishOpenFlow/finishPagePass`，旧 completion 也可推进 stage、page、
  session ordinal 或清 pending，改变新 runtime 的 Alt+E/页序/动作次序。
- **返修条件：**每一个 mutation 都必须同时验证 exact stable run 与调用时 current ACTIVE `runRevision`。使用现有不可铸造
  `TaskTransactionAction`（whole workflow occurrence）或后续 Bag domain capability作为 stable provenance，并显式接收/验证
  exact current `TaskExecutionContext`；旧 context 必须零 mutation。final-consumed advance 还须绑定 exact child action/compaction
  publication，不能只靠方法名声称“AfterFinalConsumption”。

### P1-2：raw `long occurrence` / `String actionSlot` / 无参 ordinal advance 仍把身份推进权交给 caller

- **证据：**`beginOpenFlow(long)`、`freezePagePass(long,...)`、`freezePointOnce(long,String,...)` 与
  `advanceSessionOpOrdinalAfterFinalConsumption()` 都是 public。`actionSlot` 仅 nonblank，caller 可无限换名；occurrence 可跳号，
  ordinal 可在未 final-consumed 时直接推进。
- **影响：**错误重入可铸下一业务 occurrence、重抽随机点或制造第二 child slot；UNKNOWN/ACK-loss 时也能绕过 retained frontier。
- **返修条件：**删除 public raw occurrence/slot/无参 advance。workflow 以 non-mintable `TaskTransactionAction` 对象作 exact
  occurrence provenance；随机点以 exact retained child action handle/closed slot enum 冻结；session ordinal 的推进必须提交
  expected current member handle 与真实 compaction 证明，同对象 replay 幂等、foreign/stale 零 mutation。

### P1-3：任意 `WindowFactAction` 都可冒充 BOUND_BASE action

- **证据：**`CloudBagStateOwner:82-104` 只检查传入对象非空并绑定对象引用；`WindowFactAction` 的 semantic address、owner record、
  exact context 访问器均 package-private。该类无法证明它是本 Bag workflow 当前 `BOUND_BASE` slot，而不是 caller 持有的另一
  WINDOW_FACT handle。
- **影响：**capability substitution 可为错误 semantic action 分配 geometry sequence，并在 outcome 使用同一错误对象时通过
  correlation，污染 window geometry/cache authority。
- **返修条件：**allocation 必须由 `.remote` retained owner/Bag authority先验证 handle 的 exact owner/context/address/current
  record，再传入 domain-specific non-mintable permit；普通 Service 不得直接调用 raw allocation。不得扩大 `ActionHandle` 内部字段
  为 public。若该 verifier 依赖 AB 在途源码，C 只声明依赖并保持 dormant，不以 generic handle 口头保证代替验证。

### P2-1：所谓固定有界 slot 实际仍是开放 map key

`freezePointOnce` 用任意 `String actionSlot` 写 `frozenPointsBySlot`，没有 closed enum/长度上界；JavaDoc 的“fixed set”未由类型
系统落实。改为 closed slot 或 exact retained child-handle identity，不得以自由文本作跨 resume key。

### P2-2：共享 cache 的开放 key 与 teardown surface 仍需 authority 边界

`BagLayoutKey` public record + `CUSTOM` factory 可产生无限 tuple，`logicalWindowId` 也是 caller 字符串；三张无 TTL cache 仅靠
public `removeScopeState` 清理。返修须沿已批准 BagLayout canonical owner限定可进入 shared cache 的实际 layout 集，并把 scope
teardown 收到唯一 assembly lifecycle capability；不得让业务 Service 构造任意 scope/layout 执行跨 scope 清理。

### 当前任务 `W-BAG-C0-IMP1-R2`

External Worker C 仅修改自己新建的 `CloudBagStateOwner.java`、`BagWorkflowState.java` 并 append 本日志；不得触碰 AB/A/B/D/AE、
assembly/remote shared Java/schema/host/caller/tests/Maven。若必须依赖 AB 的 `TaskTransactionAction`，只按当前 public opaque type消费，
不得修改该文件。C 须在 `2026-07-13T18:12:00-04:00` 前于真实 EOF 追加 `CLAIMED`（task、claimedAt、上述唯一写集）；已领取可
工作超过 20 分钟。完成后列 exact signature delta、stale-revision/UNKNOWN/compaction 矩阵与新 SHA。Worker self-QA 不构成批准。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Source Review #1 - BLOCKED / `W-BAG-C0-IMP1-R1` Published - 2026-07-13T17:27:34-04:00

父级逐行审查两个 New Java，并对照本日志 task brief、Bag `Parent Source/Design Review #3` 与现有
`CloudTaskServicePort.WindowFactAction` retained handle。缓存 key/geometry `< / == / >` 主体方向正确，写集也未越界；
但当前 state-core 仍有 action authority 与跨 revision stale mutation，结论
**BLOCKED，P0=0/P1=3/P2=2**。本轮不作 Approved，也不以 standalone `javac` 替代 Cloud package。

### P1-1：public `beginBoundBaseObservation(..., String)` 允许业务 caller 自铸 action identity

- **证据：**`CloudBagStateOwner:64-91` 是 public 方法，只要持有任一 exact `TaskExecutionContext`，caller 就能传入
  任意 `actionRecordId` 字符串并令 owner 分配 sequence/handle。handle constructor 虽 private，但铸造入口和 identity
  选择仍是公开的。现有 `CloudTaskServicePort.WindowFactAction` 已是 constructor package-private 的 opaque retained
  action handle；批准合同明确 stable action identity 只能来自 retained frontier，业务 caller 不得 mint。
- **影响：**Service 重入或错误 caller 可换字符串制造第二个 observation occurrence，绕过 retained action ledger 的
  same-occurrence reuse/final-consumed 门；exact context 校验不能证明 action identity 合法。
- **返修条件：**删除 raw `String actionRecordId` 铸造面。allocation 必须接收并绑定现有 non-mintable retained
  `WindowFactAction`（或同等 assembly-minted opaque capability），不能由任意字符串/UUID/调用次数构造；handle 内以 opaque
  object/exact retained identity 做 correlation。说明 future package-private authority 如何调用，普通 BagService 不能绕过。

### P1-2：pending observation 可被覆盖，旧 outcome 可清掉新 handle

- **证据：**`BagWorkflowState:239-245` 的 `recordPendingBoundBaseObservation` 无条件覆盖字段；`:256-259` 的
  `clearPendingBoundBaseObservation()` 又不接 expected handle。若 H1 在飞、H2 被覆盖，H1 late completion/finally 会把 H2
  清为 null。
- **影响：**一个 run 可出现两个不可见 in-flight observation，或丢失当前 handle；随后 resume/retry 可能重新铸 action，
  破坏乱序 fence 与 UNKNOWN 不前进。
- **返修条件：**改为 exact CAS 合同：pending=null 时安装；同一 opaque handle 重放幂等；不同 handle 在前一 exact final
  consumption 前 fail-closed。consume/clear 必须带 expected handle 并做 object/exact identity 比对，旧 handle 只能 no-op/
  stale reject，绝不能清当前 handle。

### P1-3：跨 revision 共享的 open/page cursor 没有 flow generation，旧 runtime 可改写新流程

- **证据：**类合同要求同一 `BagWorkflowState` 跨 resume/revision 复用；但 `beginOpenFlow:84-88` 无条件把 stage 重置为
  INITIAL，`clearOpenFlow:115-119` 无 expected flow，`freezePagePass/clearPagePass:128-177` 也只有一个全局字段。
  旧 revision 在 unwind/finally 时可晚到，而新 revision 已继续或开始下一 flow。
- **影响：**旧 runtime 可清掉新 runtime 的 open/page pass，或把其 stage 倒退到 INITIAL；业务会多做 Alt+E、重扫页、
  改变 committed 顺序。
- **返修条件：**给 open flow/page pass 增 owner-minted opaque generation/handle（可由 retained occurrence 驱动，不能按
  时间/随机/调用次数重建）。begin exact replay 返回同 handle/同 cursor；advance/clear 都必须提交 expected handle 并拒绝
  stale generation。只有当前 flow final-consumed/terminal 后才能建立下一 generation；resume 复用当前 handle，不 reset。

### P2-1：shared cache key 仍接受任意无界字符串

`visiblePageKey/itemPageKey` 只做 trim/nonblank；public caller 可提供任意 `layoutKey/canonicalTemplateId/logicalWindowId`，
在跨 taskRun、无 TTL 的共享 owner 中形成无界 key。按批准合同把 layout 固定为 closed `MAIN_BAG/GIVE_BAG` 或 canonical
field tuple，并对 classpath template id 使用唯一 canonical validator/明确长度上界；不得把路径、任意诊断文本或 caller alias
当 key。

### P2-2：numeric/原子合同仍有边界缺口

`writeMainBagAnchor:249` 只检查 `>0`，会接受 `Double.POSITIVE_INFINITY`；public `MainBagAnchor` compact constructor
也不校验 finite scale、non-negative generation/sequence。另 `acceptBoundBaseObservation:141-147` 先写 accepted sequence/base，
再 `Math.incrementExact(geometryGeneration)`；溢出会留下半更新，违反类自身原子声明。Repair 须先计算并验证所有 next values，
再一次提交字段；scale 必须 finite 且 positive，record 自身也保持同一 invariant。

### 当前任务 `W-BAG-C0-IMP1-R1`

External Worker C 只修改以下两个自己新建的文件，并 append 本日志：

1. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\bag\CloudBagStateOwner.java`
2. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\bag\BagWorkflowState.java`

assembly/context/remote wire/schema/BagService/host/caller/DHXY/tests/Maven/resources 与 A/B/AB/D 写集继续冻结。C 须在
`2026-07-13T17:47:34-04:00` 前于真实 EOF 追加 `CLAIMED`（task=`W-BAG-C0-IMP1-R1`、claimedAt、上述唯一写集）。
领取后可工作超过 20 分钟；源码稳定后仍由父级安排独占 Cloud `mvn -q clean package`，Worker 不并发 clean。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-13T17:33:40-04:00

- task: W-BAG-C0-IMP1-R1
- claimedAt: 2026-07-13T17:33:40-04:00
- uniqueWriteSet:
  1. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\bag\CloudBagStateOwner.java`（修改，自建）
  2. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\bag\BagWorkflowState.java`（修改，自建）
  3. `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-bag-state-owner-worker-c.md`（append-only 本日志）

## External Worker C - Repair Implementation #1 (W-BAG-C0-IMP1-R1) - 2026-07-13T17:37:42-04:00

### 逐项关闭 Parent Source Review #1

| Review 项 | 修复（文件均为自建，写集未越界） |
|---|---|
| **P1-1 action identity 可自铸** | 删除 raw `String actionRecordId` 铸造面。`beginBoundBaseObservation(TaskExecutionContext, CloudTaskServicePort.WindowFactAction)` 现在必须接收既有 **non-mintable** retained `WindowFactAction`（构造 package-private，仅 retained frontier/assembly 可产），owner 只绑定其 **exact object** 于 handle；`acceptBoundBaseObservation(..., WindowFactAction outcomeFactAction, ...)` 以 `owned.retainedAction != outcomeAction`（对象同一性）作 correlation，字符串/UUID/调用次数都无法制造第二个 observation identity。JavaDoc 说明 future package-private Bag workflow authority 传递该 handle、普通 `BagService` 无法绕过 |
| **P1-2 pending 可覆盖/旧清新** | `recordPendingBoundBaseObservation`：CAS 合同——null 安装、同 handle 对象幂等、不同 handle 在前一 final consumption 前 `IllegalStateException` fail-closed。`clearPendingBoundBaseObservation(expected)`：必须提交 expected handle，对象同一性比对；stale/旧 handle → no-op 返回 false，绝不能清当前 handle；无参 clear 已删除 |
| **P1-3 open/page cursor 无 generation** | 新增 owner-minted opaque `OpenFlowHandle` / `PagePassHandle`（private 构造、绑定 retained occurrence，非时间/随机/调用次数）。`beginOpenFlow(occurrence)`：同 occurrence exact replay 返回同 handle、cursor 不重置（resume 不 reset）；当前 flow 未 final-consumed 时其它 occurrence fail-closed；occurrence ≤ lastFinished fail-closed（superseded）。`currentOpenStage/advanceOpenStage*` 必须提交 exact current handle，stale 拒绝零 mutation；`finishOpenFlow(handle)` stale → no-op false。`freezePagePass(occurrence, order)`/`currentPage/advancePage*/finishPagePass` 同一套合同（同 occurrence 且 order 相同才复用；不同 order fail-closed）。旧 runtime 晚到的 unwind/finally 既不能把新 flow 倒回 INITIAL，也不能清新 pass |
| **P2-1 无界 key** | layout 改为 closed `BagLayoutKey`（`Kind{MAIN_BAG,GIVE_BAG,CUSTOM}` + canonical 数值 tuple；两个 HEAD 身份携 `0114604e` 冻结几何 `MAIN_BAG(-299,16,312,208/29,32,35)`、`GIVE_BAG(359,276,308,206/681,292,35)`；custom 只能以 canonical field tuple 成 key）。`canonicalTemplateId`：唯一 canonical validator——长度 ≤160、closed 字符集 `[A-Za-z0-9/_.-]`、拒绝 `..`/反斜杠/绝对路径。`logicalWindowId` 长度 ≤128。路径/alias/诊断文本不可能成 key |
| **P2-2 数值/原子边界** | `writeMainBagAnchor` 与 `MainBagAnchor` compact constructor 均要求 `Double.isFinite && >0` 的 scale、非负 generation/sequence。`acceptBoundBaseObservation` accept 分支现在**先**计算并验证全部 next values（`Math.incrementExact(geometryGeneration)` 于任何字段写之前），再一次提交 sequence/base/generation/anchor 清除——溢出异常不会留下半更新 |

### SHA-256（R1 后）

| 文件 | 行数 | SHA-256 |
|---|---:|---|
| `CloudBagStateOwner.java` | 628 | `8dfcd1b0e7d6ac18d581783308a46edd03f13bc23f8e054a20faed0a46f3c980` |
| `BagWorkflowState.java` | 461 | `a026bd714f96cb2dc6537972affc712fa6280c4c9896001d557d3ef79debe3c8` |

### scope self-QA（仅 QA，不构成 Approved）

1. 写集：仅两个自建文件修改 + 本日志 append；assembly/context/remote wire/schema/BagService/host/caller/DHXY/tests/Maven/resources 与 A/B/AB/D 写集零触碰；无 Git mutation。
2. 已冻结主体未重开：三张 cache exact key、geometry stream、`</==/>` 接收、same-sequence exact replay、无 TTL/LRU/takeover/retry/probe、scope-equality teardown、owner lock 无 remote/capture/I/O/callback（`revalidate()` JavaDoc 明示 "never waits or mutates lifecycle"）全部保持。
3. 构建门禁：Internal AB 仍在写共享 Java，本轮未运行任何 Maven；仅对两个自建文件做只读 `javac` 冒烟（exit 0）。独占 Cloud `mvn -q clean package` 待父级安排并作唯一批准。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付完毕，持续 5 分钟重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #6 - SOURCE APPROVED / `W-BAG-C1-D1` - 2026-07-13T19:02:51-04:00

父级逐行复核 R5 两个实际源码并独立复算 SHA-256：`CloudBagStateOwner`
`EAFA0D7E4B98C6545A954867629603D402F3EBB10B4CC497F0130A24C4396AC1`，`BagWorkflowState`
`34EAD25E28BD640BDAEDCB51840940CA4D3009B896343A078FA981AD2BE5FFD8`。workflow 构造时保存 final exact
owner；record 路径经该 owner 的 instance method 先验证 `ownerInstanceId`，再验证完整 scope/taskRun/window/
stopEpoch/runRevision tuple，foreign owner 在首写前零 mutation。R4 的 permit、pending CAS、final clear 与 canonical
validator 均未回退。结论：**SOURCE APPROVED，P0=0/P1=0/P2=0**；Cloud package 待 AB 稳定后统一执行。

当前任务 `W-BAG-C1-D1`：External C 须在 `2026-07-13T19:22:51-04:00` 前于真实 EOF 追加
`CLAIMED task=W-BAG-C1-D1 claimedAt=<ISO> uniqueWriteSet=<本日志>`，随后只写 Bag authority assembly 接线
Design #1，Java 全冻结。设计须闭合：private-zero-factory owner 的唯一 trusted construction seam；non-mintable
composition permit 的真实 owner/可见性；同一 run 的 owner+workflow 原子创建、resume revision 更新、terminal/teardown
释放；exact current context 与 pending/final-consumed authority；容量与 tenant/scope retirement；closed 文件/方法表及
与 AB shared `.remote` 的顺序门。不得开放 public factory/raw owner getter，不得引入第二 registry/ledger/map/thread，
不得接 host/caller/Task。逾期只原样重发 C，绝不内部接管。self-QA 不算批准。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Source Review #5 - BLOCKED / `W-BAG-C0-IMP1-R5` - 2026-07-13T18:52:06-04:00

父级已直接审查 R4 后两个 Java 文件；结论：`P0=0 / P1=1 / P2=0`，**不批准**。

### P1-1 - pending custody 仍未绑定 exact owner instance

- 证据：`CloudBagStateOwner.java:408-423` 的 `describesExactRun(...)` 仍是 `static`，只比较
  `scope/taskRunId/window/stopEpoch/runRevision`，没有比较 `ownerInstanceId`；其注释还明确把 owner-instance
  校验推迟到 outcome consumption（`:405-406`）。
- 证据：`BagWorkflowState.java:81,91-103,519-539` 没有保存 exact `CloudBagStateOwner` 引用，安装 pending
  handle 时只能调用上述静态 tuple 校验。
- 影响：另一个 owner 实例只要为同一 stable-run tuple 铸出 handle，就能先占用本 state 的 pending 槽，阻塞
  正确 owner 的 handle；随后才在 consumption 抛 foreign-owner，已经造成 custody 污染/拒绝服务。R4 没有满足
  Review #4 要求的“foreign owner handle 在记录前零写入拒绝”。

### 精确返修条件

1. `BagWorkflowState` 构造时必须保存 exact `CloudBagStateOwner` 对象引用，生命周期内不可替换。
2. 把 `describesExactRun` 改成 owner-instance 方法（或等价的 owner-owned 零 mutation seam），先比较
   `this.ownerInstanceId == handle.ownerInstanceId` 的语义，再比较完整 stable-run/current-revision tuple。
3. `recordPendingBoundBaseObservation` 只能经所保存的 exact owner 完成校验；foreign owner/run/window/revision
   一律在写 pending 前 fail-closed。
4. 保持其余 R4 修复不回退；仅修改这两个既有 New 文件并向本日志追加 R5，shared `.remote`/assembly/host/
   caller/schema/tests/Maven 均冻结。当前 AB 仍写共享 Java，不运行并发 `mvn clean`。

### 领取门

External C 请在 `2026-07-13T19:12:06-04:00` 前于真实物理 EOF 追加：
`CLAIMED task=W-BAG-C0-IMP1-R5 claimedAt=<ISO> uniqueWriteSet=<上述两个 Java + 本日志>`。
20 分钟只检查领取，不检查完成；领取后允许持续返修超过 20 分钟。逾期只原样重发给 C，绝不内部接管。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Source Review #4 - BLOCKED / `W-BAG-C0-IMP1-R4` - 2026-07-13T18:38:00-04:00

父级逐行复核 R3 的两个实际源码文件。R3 已正确关闭 raw scope/window cache API、CUSTOM layout、
数值半更新、旧 revision 直接推进等历史问题；但“零 mint path”只让当前波 dormant，并未使未来接线
安全，且仍存在可直接清除 UNKNOWN custody 的当前 public mutation。结论：
**P0=0 / P1=4 / P2=1，BLOCKED，不作 SOURCE APPROVED。**

### P1-1 - public `create(Consumer)` 仍允许任意调用者创建第二 owner 并取得 teardown capability

- **证据：** `CloudBagStateOwner:88-95` 是 unrestricted public static factory；任何 Cloud 代码都能反复
  `create(cap -> ...)`，每次得到新 owner 和其有效 capability。JavaDoc 所称“future assembly calls once”
  只是约定，不是类型/组合边界。
- **影响：** 无法证明 authority assembly 是唯一 state owner；业务代码可建立第二套无 TTL cache/geometry
  registry，违反本任务的 single-owner/no-second-registry 不变量。
- **返修条件：** 本 dormant wave 删除 public factory/callback seam。owner 保持不可外部构造且无实例化路径，
  或提供一个当前源码中真实不可由业务代码获得的 assembly-owned construction permit；不得用“未来只调用
  一次”的注释代替结构性限制。scope teardown 接线留到有真实 assembly owner 的批准波。

### P1-2 - 两个 empty generic permit 可被跨 transition 替换，finish 路径甚至完全忽略 final identity

- **证据：** `VerifiedTransactionPermit:623-626` 与 `FinalConsumptionPermit:642-645` 都没有 owner/run/address/
  occurrence/transition 字段。`finishWorkflowTransaction:158-178`、`finishOpenFlow:263-278`、
  `finishPagePass:366-381` 仅 non-null 检查 final permit，随后完全不验证它属于哪个 child action；同一个
  generic permit 也可传给 stage/page/session/finish 任一方法。
- **影响：** 一旦未来 trusted factory 真正 mint permit，某个已 compact 的无关 Bag child action即可被
  substitution 到另一个 stage/pass/transaction，提前推进或结束 workflow；这正是 R2 要消除的 generic
  proof 问题，只是从 `instanceof` 换成了空 token。
- **返修条件：** permit 必须结构性绑定 owner instance、exact stable run、transaction occurrence、closed
  child slot/ordinal及允许的唯一 transition；state 对这些字段与当前 handle/cursor逐项验证。可拆为 closed
  transition-specific permit 类型。当前无 mint path可以继续 dormant，但类型本身必须让未来合法 permit
  不能跨方法/slot 复用。

### P1-3 - pending bound-base handle 可在没有 final-consumed 证明时被清空

- **证据：** `clearPendingBoundBaseObservation:499-509` 只需要 current context + exact handle 引用，不需要
  `FinalConsumptionPermit`，也不经 owner 的 accepted/final result；持有 handle 的业务 caller 可在 UNKNOWN、
  未 dispatch 或 ACK-loss 时直接 clear。
- **影响：** 随后可登记下一 observation，绕过“UNKNOWN 不前进/不 replacement”合同，并丢失旧 in-flight
  outcome 的 custody。
- **返修条件：** clear/retire 必须提交与该 exact observation handle/semantic address 绑定的 closed final
  permit；只有真实 final-consumed + compacted disposition 允许清除。UNKNOWN/未 final/foreign/stale 一律
  零 mutation；same permit replay 幂等。

### P1-4 - workflow 可登记另一 stable run 的 opaque observation handle

- **证据：** `recordPendingBoundBaseObservation:466-482` 只比较槽位 null/同引用；它不验证 handle 内的
  ownerInstanceId/scope/taskRunId/window/stopEpoch/runRevision 与本 state。`BagBoundBaseObservationHandle`
  字段全 private，`BagWorkflowState` 也没有 owner verifier 可调用。
- **影响：** 当前 run 可把另一个 run/window 的 handle 作为自己的 pending custody；后续 read/clear 的对象
  identity仍会通过，破坏租户/窗口/run 隔离与 late-outcome归属。
- **返修条件：** owner 提供 package-private、零 mutation 的 exact stable-run/owner verification seam，或由
  owner 自己原子登记到 state；record 前必须验证 owner instance + scope/taskRun/window/stopEpoch，且旧
  revision复用规则明确。不得开放 handle raw getters给业务 caller。

### P2-1 - MAIN_BAG client anchor 未校验坐标下界

- **证据：** `writeMainBagAnchor:318-345` 与 `MainBagAnchor:555-572` 校验 scale/generation，但允许负
  `clientX/clientY`；该值后续是本地 input/ROI 坐标基础。
- **返修条件：** 在唯一 compact/写入边界要求 clientX/clientY 非负；不新增额外截图或业务 probe。

### 下一任务

External Worker C 只在原两个自建文件定点返修并于真实 EOF 追加 `CLAIMED W-BAG-C0-IMP1-R4` 与
`Repair Implementation #4`。shared `.remote`、assembly、host、caller、schema、tests、Maven和其它 Worker
写集继续冻结；AB 写入期不跑 `mvn clean`。自审不算父级批准。

领取截止：`2026-07-13T18:58:00-04:00`。20 分钟只检查 C 是否在真实 EOF 追加 task/claimedAt/
uniqueWriteSet；已领取后允许持续返修，逾期仅在本日志记录并原样重发给 External C，绝不内部接管。

## Parent Source Review #3 - BLOCKED / `W-BAG-C0-IMP1-R3` Published - 2026-07-13T18:14:00-04:00

父级逐行复审 R2 两文件，而非复用 worker self-QA。stale-context CAS、pending expected-clear、数值原子提交与 template
validator仍成立；但 tenant access、final-consumption proof 与 domain permit没有形成结构性权威。结论
**BLOCKED，P0=0/P1=4/P2=2**，不得接 assembly/Service。

### P1-1：公开 cache API 接受 caller-mintable scope/window，不能提供租户隔离

- **证据：**`CloudBagStateOwner:194-296` 的 visible/item/anchor read/write均直接接 public
  `RemoteTaskRunScope`、logicalWindowId或 `RemoteTaskRunWindow`；这些 record可由任意 business caller构造。把 scope放进 map key只
  防碰撞，不认证“调用者只能访问自己的 scope”。
- **影响：**被注入 owner的 Service可读写另一 tenant/session/window 的 hint/anchor，形成跨租户污染与错误坐标。
- **返修条件：**所有业务 read/write从 exact current `TaskExecutionContext` 或 owner-minted scope-bound access handle派生 scope/window，
  不再公开接 raw scope/window；需要跨 taskRun保留 cache不等于允许 caller选择 tenant。exact context stale/foreign必须零 mutation。

### P1-2：`instanceof` 不是 final-consumption proof，finish 路径甚至没有 proof

- **证据：**`BagWorkflowState:559-569` 只要对象是任意 `TaskTransactionAction/WindowFactAction/CaptureAction/InputBundleAction`
  就接受；未验证 owner、run、parent transaction、semantic slot、occurrence、current handle或 ledger compacted。
  `finishWorkflowTransaction:157`、`finishOpenFlow:260`、`finishPagePass:358` 完全不收 consumed proof。
- **影响：**另一 run/operation 的合法 opaque handle即可推进 page/stage/session ordinal，当前 context+handle又可提前 finish，绕过真实
  final-consumed/compaction并丢失 workflow state。
- **返修条件：**删除 `Object+instanceof` 白名单。每个 advance/finish接收 domain-specific non-mintable permit，permit必须由 retained
  authority在 exact parent/child/current semantic address/occurrence 已 final-consumed+compacted后铸造；state按 exact permit对象幂等，
  foreign/mismatched permit零 mutation。`beginWorkflowTransaction` 同样必须验证 transaction属于此 exact stable run。

### P1-3：`BoundBaseObservationPermit` 的“verified”仅靠注释，且跨包调用链不可实现

- **证据：**`CloudBagStateOwner:624` 的 package-private mint只接任意 generic `WindowFactAction`，自身不验证 owner/context/address/
  current record；任何 `com.bot.dhxy.service.bag` 类都能调用。真正掌握 retained internals的 `.remote` authority反而不能调用另包的
  package-private方法。
- **影响：**generic WINDOW_FACT capability仍可冒充 BOUND_BASE；未来接线只能放宽 visibility或继续靠口头约定。
- **返修条件：**给真实可编译 seam：由 `.remote` retained authority直接产出 domain-specific `BagBoundBaseAction/Permit`，或提供一个
  non-mintable verifier facade返回已验证 permit；普通 Bag Service不能调用 mint，permit必须绑定 exact current record与 BOUND_BASE
  semantic address。列出当前两文件可消费的 public类型，不得写“未来先验证再调用”。

### P1-4：public record constructor 仍可任意构造 `CUSTOM` layout

- **证据：**`CloudBagStateOwner:488` 是 public record；即使 `custom()` 在 :527 改 package-private，任何 caller仍可直接
  `new BagLayoutKey(Kind.CUSTOM, ...)`。
- **影响：**开放 tuple key面与“business只能 MAIN_BAG/GIVE_BAG”声明相反，容量可被任意 custom tuple撑满。
- **返修条件：**本波只暴露 closed MAIN_BAG/GIVE_BAG identity；若未来确需 custom，使用构造不可见的 sealed/final value加 trusted
  canonical owner，不得用 public record canonical constructor暴露 CUSTOM。

### P2-1：scope teardown capability 的 composition seam 同样跨包断裂

`mintScopeTeardownCapabilityOnce:346` 只对 service.bag包可见，而 lifecycle assembly在 `.remote`；当前唯一能 mint的是同包业务代码，
不是声称的 assembly。R3 给可编译 owner+teardown capability composition factory/bridge，保持 capability不向 Service暴露。

### P2-2：`revalidate()` “never waits” 的 JavaDoc与锁序声明不实

`TaskExecutionContext.revalidate()` 会进入 execution gate/coordinator synchronized路径，可能竞争 monitor。保留锁内 exact fence可以，
但注释必须改为“无 lifecycle mutation/remote I/O，可能短暂竞争 coordinator”，并明确 lifecycle不得持 coordinator/current-slot
transition lock再进入 Bag owner，避免 owner→coordinator与反向锁序。

### 当前任务 `W-BAG-C0-IMP1-R3`

External Worker C 仅修改自己新建的 `CloudBagStateOwner.java`、`BagWorkflowState.java` 与 append本日志；不得改 shared
`.remote` Java。若 P1-2/P1-3 所需 verified permit必须改 shared文件，本轮只把两 leaf收敛成**不暴露不安全 mutation API的
dormant value/core**并在日志列出明确依赖，禁止伪造 permit。C 须在 `2026-07-13T18:34:00-04:00` 前于真实 EOF追加
`CLAIMED`（task、claimedAt、uniqueWriteSet）；已领取可超过20分钟，不并发 Maven clean。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Source Review #2 - TRUE PHYSICAL EOF CONTROL POINTER / `W-BAG-C0-IMP1-R2` - 2026-07-13T17:53:00-04:00

本文件中 `Parent Source Review #2 - BLOCKED / W-BAG-C0-IMP1-R2` 完整正文因重复交付锚点误落在历史段前部；为保护
append-only 不删除或改写旧块。**该完整 Review #2 是最新父级结论，本真实物理 EOF 控制块固定领取门。**

- 结论：`BLOCKED，P0=0/P1=3/P2=2`。
- C 仅修改自己新建的 `CloudBagStateOwner.java`、`BagWorkflowState.java` 并 append 本日志；其它写集冻结。
- 只关闭：same-handle stale-revision mutation；raw occurrence/actionSlot/ordinal advance；generic WindowFactAction capability
  substitution；开放 slot/cache/teardown authority。
- 在 `2026-07-13T18:12:00-04:00` 前于真实 EOF 追加 `CLAIMED`（task=`W-BAG-C0-IMP1-R2`、claimedAt、
  uniqueWriteSet=上述两 Java + 本日志）。已领取可工作超过 20 分钟；不得并发 Maven clean。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-13T17:57:20-04:00

- task: W-BAG-C0-IMP1-R2
- claimedAt: 2026-07-13T17:57:20-04:00
- uniqueWriteSet:
  1. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\bag\CloudBagStateOwner.java`（修改，自建）
  2. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\bag\BagWorkflowState.java`（修改，自建）
  3. `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-bag-state-owner-worker-c.md`（append-only 本日志）
- 依赖声明：若使用 AB 的 `TaskTransactionAction`，仅按当前 public opaque type 消费，不修改该文件。

## External Worker C - Repair Implementation #2 (W-BAG-C0-IMP1-R2) - 2026-07-13T18:04:51-04:00

### exact signature delta（两个自建文件；写集未越界）

`BagWorkflowState`（全部 public mutation/read 均新增 exact `TaskExecutionContext` 门；raw `long occurrence` / `String actionSlot` / 无参 advance 全部删除）：

| R1 签名 | R2 签名 |
|---|---|
| —（无） | `beginWorkflowTransaction(TaskExecutionContext, TaskTransactionAction)`（新增：事务=非可铸 `TaskTransactionAction` 对象 provenance；同对象幂等、未完成时换事务 fail-closed、重放已完成事务 fail-closed；建立时结构性清 frozen points/ordinal） |
| —（无） | `boolean finishWorkflowTransaction(TaskExecutionContext, TaskTransactionAction)`（stale ctx/非当前事务 → 零 mutation no-op false；有未完成 flow/pass → fail-closed） |
| `beginOpenFlow(long)` | `beginOpenFlow(TaskExecutionContext, TaskTransactionAction)`（replay 返同 handle 不重置；每事务至多一个 open flow，完成后重开 fail-closed） |
| `currentOpenStage(OpenFlowHandle)` | `currentOpenStage(TaskExecutionContext, OpenFlowHandle)` |
| `advanceOpenStageAfterFinalConsumption(OpenFlowHandle, OpenStage)` | `advanceOpenStageAfterFinalConsumption(TaskExecutionContext, OpenFlowHandle, Object consumedChildActionProof, OpenStage)`（proof 必须是 retained 非可铸 capability；同 proof 幂等、绝不二次前进） |
| `boolean finishOpenFlow(OpenFlowHandle)` | `boolean finishOpenFlow(TaskExecutionContext, OpenFlowHandle)`（stale ctx/handle → 零 mutation false） |
| `freezePagePass(long, List)` | `freezePagePass(TaskExecutionContext, TaskTransactionAction, List)` |
| `currentPage(PagePassHandle)` | `currentPage(TaskExecutionContext, PagePassHandle)` |
| `advancePageAfterFinalConsumption(PagePassHandle)` | `advancePageAfterFinalConsumption(TaskExecutionContext, PagePassHandle, Object consumedChildActionProof)`（同 proof 幂等） |
| `boolean finishPagePass(PagePassHandle)` | `boolean finishPagePass(TaskExecutionContext, PagePassHandle)`（stale → 零 mutation false） |
| `currentSessionOpOrdinal()` | `currentSessionOpOrdinal(TaskExecutionContext, TaskTransactionAction)` |
| `advanceSessionOpOrdinalAfterFinalConsumption()` | `advanceSessionOpOrdinalAfterFinalConsumption(TaskExecutionContext, TaskTransactionAction, Object consumedMemberActionProof)`（无参版本已删除；同 proof 幂等） |
| `freezePointOnce(long, String, int, int)` | `freezePointOnce(TaskExecutionContext, TaskTransactionAction, FrozenPointSlot, int pageIndex, int, int)`（closed enum 12 值 + pageIndex ∈ [-1,5]，类型系统封闭 key 空间；事务切换时结构性清空） |
| `recordPendingBoundBaseObservation(handle)` | `recordPendingBoundBaseObservation(TaskExecutionContext, handle)` |
| `clearPendingBoundBaseObservation(expected)` | `clearPendingBoundBaseObservation(TaskExecutionContext, expected)`（stale ctx → 零 mutation false） |
| `OpenFlowHandle`/`PagePassHandle`（绑 long occurrence） | 同名 opaque handle 改绑非可铸 `TaskTransactionAction` 对象 |

`CloudBagStateOwner`：

| R1 签名 | R2 签名 |
|---|---|
| `beginBoundBaseObservation(ctx, CloudTaskServicePort.WindowFactAction)` | `beginBoundBaseObservation(ctx, BoundBaseObservationPermit)`（generic WINDOW_FACT handle 不再可直接进入 allocation） |
| `acceptBoundBaseObservation(handle, ctx, WindowFactAction, window, base)` | `acceptBoundBaseObservation(handle, ctx, BoundBaseObservationPermit, window, base)`（correlation=exact permit 对象） |
| —（无） | nested `BoundBaseObservationPermit`：private 构造 + **package-private** `mintVerifiedBoundBasePermit(WindowFactAction)`——只留给未来本包 Bag workflow authority，在 `.remote` retained owner 侧完成 exact owner/context/address/current record 验证后铸造（该 verifier 依赖 AB 在途源码：仅声明依赖、保持 dormant，未以 generic handle 口头保证代替验证；普通 Service 无法铸造/替换） |
| `removeScopeState(scope)` public | `removeScopeState(ScopeTeardownCapability, scope)` + **package-private** `mintScopeTeardownCapabilityOnce()`（单次铸造、对象同一性验证；teardown 收权到唯一 assembly lifecycle capability，业务 Service 不能构造任意 scope 清理） |
| `BagLayoutKey.custom(...)` public | `custom(...)` **package-private**（共享 cache 只接受 `mainBag()/giveBag()` 或未来本包 canonical layout owner 认可的 tuple） |

### stale-revision / UNKNOWN / compaction 矩阵

| 场景 | 行为 | mutation |
|---|---|---|
| 旧 revision（pause/resume 后旧栈）调用任何 begin/advance/freeze/record | `isExactCurrentRun` 内 `revalidate()` 失败或 binding revision != ctx revision → `IllegalStateException` fail-closed | 0 |
| 旧 revision late `finally` 调 `finishOpenFlow/finishPagePass/finishWorkflowTransaction/clearPending*` | stale ctx → 返回 false no-op（finally 安全），新 runtime 的 flow/pass/事务/pending 原样 | 0 |
| 旧 revision 持同一 opaque handle（R1 漏洞：对象同一性无法分 revision） | 对象同一性之外每次 mutation 都验 exact stable run + 当前 ACTIVE revision → 旧 revision 一律拦截 | 0 |
| foreign run（scope/taskRunId/window/stopEpoch 任一不同） | `isExactCurrentRun` 字段级不匹配 → 同上 | 0 |
| UNKNOWN outcome（member/page/stage 未 final-consumed） | 不调用 advance（合同）；即使误重放 begin/freeze → 同事务/同 handle/同 key 幂等返回冻结值，cursor/ordinal/随机点不变 | 0 |
| final consumption 后 compaction/replay 重复提交同一 proof 对象 | `proof == lastAdvanceProof` → 幂等（stage 需已 == 目标，否则 fail-closed）；ordinal/page 不二次前进 | 0 |
| caller 伪造 proof（任意对象/字符串/数字） | `requireRetainedProof` instanceof 白名单（`TaskTransactionAction`/`WindowFactAction`/`CaptureAction`/`InputBundleAction`）→ `IllegalArgumentException` | 0 |
| caller 试图跳事务/跳 occurrence | 事务对象由 retained frontier 铸造；未完成时换事务、重放已完成事务均 fail-closed | 0 |
| generic `WindowFactAction` 冒充 BOUND_BASE | allocation 只收 permit；permit 铸造 package-private 且前置 `.remote` 验证（依赖声明、dormant） | 0 |
| 任意 scope teardown | 无 capability 对象（单次铸造、owner 绑定）→ fail-closed | 0 |

### SHA-256（R2 后）

| 文件 | 行数 | SHA-256 |
|---|---:|---|
| `CloudBagStateOwner.java` | 715 | `b3617d80d7b620b6aedf7f9619c17374badf41ee442e9365222f31540f16d700` |
| `BagWorkflowState.java` | 641 | `122066c56ad53473060c239a3c50e2cbe98bf62354c554272d5e7a9a3fb04901` |

### scope self-QA（仅 QA，不构成 Approved）

1. 写集：仅两个自建文件 + 本日志 append；`TaskTransactionAction`/`CloudTaskServicePort` 仅按当前 public opaque type 消费（零修改）；AB/A/B/D/AE、assembly/remote shared Java/schema/host/caller/tests/Maven 零触碰；无 Git mutation。
2. R1 已闭环项未回退：pending CAS、数值 finite/非负、原子 generation 提交、template canonical validator、`</==/>` 接收全部保留。
3. 构建门禁：未运行任何 Maven；仅对两个自建文件只读 `javac` 冒烟（exit 0）。独占 Cloud `mvn -q clean package` 待父级安排并作唯一批准。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付完毕，持续 5 分钟重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #5 - TRUE PHYSICAL EOF CONTROL COPY / `W-BAG-C0-IMP1-R5` - 2026-07-13T18:53:30-04:00

位于本文历史锚点处的完整 `Parent Source Review #5 - BLOCKED` 现由本 EOF 控制副本生效。父级结论仍为
`P0=0 / P1=1 / P2=0`：`CloudBagStateOwner.java:408-423` 的静态 `describesExactRun` 不比较
`ownerInstanceId`，而 `BagWorkflowState.java:81,91-103,519-539` 不保存 exact owner；同 stable-run tuple 的
foreign-owner handle 仍可先污染/占用 pending custody，未满足“记录前零写入拒绝”。

R5 只做：让 `BagWorkflowState` 构造时保存不可替换的 exact `CloudBagStateOwner`，由该 owner 的 instance
verification seam 同时校验 ownerInstanceId 与完整 run/window/revision tuple；pending 写入只能经该 exact owner。
其余 R4 修复不回退。External C 请在 `2026-07-13T19:12:06-04:00` 前于真实 EOF 追加
`CLAIMED task=W-BAG-C0-IMP1-R5 claimedAt=<ISO> uniqueWriteSet=<两个既有 Java + 本日志>`。领取后可工作超过
20 分钟；逾期只原样重发给 C，绝不内部接管。shared `.remote`/assembly/host/caller/schema/tests/Maven 冻结。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-13T18:21:30-04:00

- task: W-BAG-C0-IMP1-R3
- claimedAt: 2026-07-13T18:21:30-04:00
- uniqueWriteSet:
  1. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\bag\CloudBagStateOwner.java`（修改，自建）
  2. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\bag\BagWorkflowState.java`（修改，自建）
  3. `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-bag-state-owner-worker-c.md`（append-only 本日志）
- 本轮策略（按 R3 任务书）：不改 shared `.remote` Java；P1-2/P1-3 收敛为不暴露不安全 mutation API 的 dormant value/core，permit 类型零铸造路径（禁止伪造），依赖清单写入交付节。

## Parent Source Review #3 - TRUE PHYSICAL EOF CONTROL ACK / `W-BAG-C0-IMP1-R3` - 2026-07-13T18:22:00-04:00

父级确认：位于本文历史锚点处的完整 `Parent Source Review #3 - BLOCKED` 仍是本轮唯一控制审查，
其中 `P1=4 / P2=2`、源码证据、影响与六项精确返修条件全部有效。External Worker C 已于
`2026-07-13T18:21:30-04:00` 在真实 EOF 领取 `W-BAG-C0-IMP1-R3`，因此不重发、不重置领取窗，
继续严格限定为其自建两个 `service/bag` 文件与本 append-only 日志；shared `.remote`、assembly、host、
caller、schema、tests 与其他 Worker 写集保持冻结。R3 正式交付前，本块不构成源码批准。

## External Worker C - Repair Implementation #3 (W-BAG-C0-IMP1-R3) - 2026-07-13T18:24:30-04:00

（校正：上一 CLAIMED 块 claimedAt 手误写为 18:21:30，实际追加时刻为 18:17:47-04:00，均早于 18:34 截止；append-only 不改旧块，特此记录。）

### 逐项关闭 Parent Source Review #3

| Review 项 | 修复 |
|---|---|
| **P1-1 公开 cache API 接受 caller-mintable scope/window** | 三张 cache 与 anchor/geometry 的全部业务 read/write 改为只接收 exact 当前 `TaskExecutionContext`，scope/logicalWindowId/window tuple 一律**从 context 派生**（`readVisiblePage(ctx, layout)`、`writeItemPage(ctx, layout, templateId, page)`、`readCurrentMainBagAnchor(ctx)`、`writeMainBagAnchor(ctx, x, y, scale)` 等）；不再存在任何接受 raw `RemoteTaskRunScope`/`RemoteTaskRunWindow` 的 cache API。跨 taskRun 保留语义不变（key 仍为 scope+windowId+layout，无 TTL），但 caller 无法选择租户。stale/foreign context：读写一律 fail-closed 零 mutation |
| **P1-2 instanceof 非 proof、finish 无 proof** | 删除 `Object+instanceof` 白名单与 `requireRetainedProof`。所有 advance（stage/page/ordinal）与所有 finish（open flow/page pass/workflow transaction）都必须提交 nested non-mintable `FinalConsumptionPermit`；`beginWorkflowTransaction`/`beginOpenFlow`/`freezePagePass`/`freezePointOnce`/ordinal 读取都必须提交 nested non-mintable `VerifiedTransactionPermit`（其铸造语义=已验证 transaction 属于此 exact stable run）。**两种 permit 本波零铸造路径**（private 构造、无任何 factory）→ 全部 workflow mutation 收敛为 dormant，无法伪造（按 R3 任务书 fallback 条款）。state 对 permit 按 exact 对象同一性：replay 幂等、foreign/mismatched 零 mutation |
| **P1-3 permit "verified" 仅靠注释且跨包不可实现** | 删除 package-private `mintVerifiedBoundBasePermit`（该入口正是可被同包任意类伪造的洞）。`BoundBaseObservationPermit` 同样**零铸造路径**；allocation/acceptance 面完全 dormant。依赖清单见下节，不再写"未来先验证再调用"式口头保证 |
| **P1-4 public record 可构造 CUSTOM** | `BagLayoutKey` 改为 **enum**（`MAIN_BAG`/`GIVE_BAG`，携 `0114604e` 冻结几何字段）；无 CUSTOM、无任何公共构造面，key 空间由类型系统封闭。未来 custom layout 需其自己的批准波 |
| **P2-1 teardown capability 跨包断裂** | 删除 package-private mint。改为可编译的 owner+teardown 组合缝：`CloudBagStateOwner` **无公共构造器**，唯一入口 `public static create(Consumer<ScopeTeardownCapability>)`——创建时把唯一 capability 一次性交给创建方（未来 `.remote` assembly 可直接跨包调用该 public factory 并私藏 capability）；owner 实例此后不再暴露 capability，被注入 owner 的 Service 永远拿不到。`removeScopeState(capability, scope)` 按对象同一性验证 |
| **P2-2 revalidate JavaDoc/锁序不实** | 两文件注释更正为："`revalidate()` 无 lifecycle mutation、无 remote I/O，但可能短暂竞争 coordinator/execution-gate monitor"；并写明锁序规则：lifecycle/coordinator 代码不得持 coordinator/current-slot transition lock 进入 Bag owner/state（唯一顺序 owner→coordinator，无反向） |

### 依赖清单（P1-2/P1-3 verified-permit seam；本轮按 R3 任务书未改 shared `.remote`）

- 当前两文件**可消费**的 public 类型：`TaskExecutionContext`（getScope/getTaskRunId/getWindowId/getNativeWindowHandle/getNativeWindowProcessId/getPlayerIdentityEpoch/getStopEpoch/getRunRevision/revalidate）、`RemoteTaskRunScope`、`RemoteTaskRunWindow`、`RemoteTaskRunAuthorization`（含 `RemoteTaskRunBinding.runRevision()`）、`CloudTaskServicePort.WindowFactAction/CaptureAction/InputBundleAction`（public 类型但 accessors package-private）、`TaskTransactionAction`（同上）。这些类型**不能表达** owner/record/semantic address/occurrence/final-consumed+compacted 的验证——这正是本波不设任何 mint 的原因。
- 待未来批准波（shared `.remote` 写集或其批准的本包桥接）：
  1. `.remote` retained authority/ledger 产出 domain-specific verified Bag `BOUND_BASE` action/permit，或提供 non-mintable verifier facade（绑定 exact current record + BOUND_BASE semantic address）→ 在其写集内为 `BoundBaseObservationPermit` 添加 trusted factory；
  2. 同一 authority 验证 `TaskTransactionAction` 属于 exact stable run → 为 `VerifiedTransactionPermit` 添加 trusted factory；
  3. ledger final-consumed+compacted 观察 → 为 `FinalConsumptionPermit` 添加 trusted factory；
  4. `.remote` assembly 调用 `CloudBagStateOwner.create(...)` 并私藏 teardown capability（本轮已给出可编译 public 组合缝，无需再改本文件）。

### SHA-256（R3 后）

| 文件 | 行数 | SHA-256 |
|---|---:|---|
| `CloudBagStateOwner.java` | 698 | `ae1cfe98d4369cc3eb8133ffacaf3f6b95ca27101bbd43cab84c5f09b75d70ac` |
| `BagWorkflowState.java` | 674 | `dd9c47a8649ab9ace28469e28faa63077310d86d47103ed91705fe6cf835fa7c` |

### scope self-QA（仅 QA，不构成 Approved）

1. 写集：仅两个自建文件 + 本日志 append；shared `.remote` Java/schema/host/caller/tests/Maven 零触碰（`TaskTransactionAction` 本轮连 import 都已不再需要）；无 Git mutation。
2. R1/R2 已闭环项未回退：pending CAS（现加 ctx 门）、数值 finite/非负、原子 generation 提交、template canonical validator、`</==/>` 接收、stale-context 零 mutation 语义全部保留。
3. 构建门禁：未运行任何 Maven；仅对两个自建文件只读 `javac` 冒烟（exit 0）。独占 Cloud `mvn -q clean package` 待父级安排并作唯一批准。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付完毕，持续 5 分钟重读本日志等待父级复审；自审不算 Approved。

## External Worker C - CLAIMED - 2026-07-13T18:44:00-04:00

- task: W-BAG-C0-IMP1-R4
- claimedAt: 2026-07-13T18:44:00-04:00
- uniqueWriteSet:
  1. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\bag\CloudBagStateOwner.java`（修改，自建）
  2. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\bag\BagWorkflowState.java`（修改，自建）
  3. `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-bag-state-owner-worker-c.md`（append-only 本日志）

## External Worker C - Repair Implementation #4 (W-BAG-C0-IMP1-R4) - 2026-07-13T18:42:50-04:00

（校正：上一 CLAIMED 块 claimedAt 写为 18:44:00，实际追加时刻为 18:39:07-04:00，早于 18:58 截止；append-only 不改旧块，特此记录。）

### 逐项关闭 Parent Source Review #4

| Review 项 | 修复 |
|---|---|
| **P1-1 public `create(Consumer)` 可造第二 owner/capability** | 删除 public factory 与 callback seam。`CloudBagStateOwner` 现在**无任何实例化路径**（private 构造 + 零 factory）：业务代码既不能创建第二 owner，也不可能取得 `ScopeTeardownCapability`（仅 private 构造器内初始化）。scope teardown 接线按返修条件留待有真实 assembly owner 的批准波（其写集内加 trusted construction seam）；不再以注释代替结构性限制 |
| **P1-2 空 generic permit 可跨 transition 替换、finish 无 final identity** | 两种 permit 改为**结构化绑定**：`VerifiedTransactionPermit` 携 `stateInstanceId + scope + taskRunId + window + stopEpoch + transactionOccurrence`，begin 时逐字段验证；`FinalConsumptionPermit` 携 `stateInstanceId + 父 VerifiedTransactionPermit（对象）+ closed WorkflowTransition（7 值枚举）+ exact stepOrdinal + observationHandle`。每个 advance 验证 transition 专值 + 当前 cursor（stage=target.ordinal()、page=pageCursor、ordinal=sessionOpOrdinal）；**每个 finish（transaction/open flow/page pass）现在都必须提交并验证 final permit**（transition 专值 + 父事务对象 + stepOrdinal=-1），mismatch 一律零 mutation。合法 permit 从类型上无法跨方法/slot/事务/state 复用；仍零铸造路径（dormant，未伪造） |
| **P1-3 pending 可无证清除** | `clearPendingBoundBaseObservation(ctx, expected, FinalConsumptionPermit)`：必须提交绑定 `PENDING_OBSERVATION_CLEAR` transition + exact observation handle（对象同一性）+ 本 state 的 final permit——即只有真实 final-consumed+compacted disposition（由未来 authority 铸 permit 证明）可清除 custody；UNKNOWN/未 dispatch/ACK-loss/foreign/stale 一律零 mutation false；same permit replay 清除后幂等 no-op |
| **P1-4 可登记外 run 的 handle** | `CloudBagStateOwner` 新增 package-private 零 mutation verification seam `describesExactRun(handle, scope, taskRunId, window, stopEpoch, runRevision)`（enclosing class 读 private 字段，不向业务开放 raw getters）；`recordPendingBoundBaseObservation` 安装前验证 handle 描述本 exact stable run + 调用时 current revision，mismatch fail-closed 零 mutation。owner-instance 归属验证保留在 owner 自身（`acceptBoundBaseObservation` 的 `requireOwnedHandle` 对 foreign owner 抛出）——两文件间无 owner 引用接线前的最大可实现边界，已在 JavaDoc/本节声明 |
| **P2-1 anchor 坐标无下界** | `writeMainBagAnchor` 与 `MainBagAnchor` compact 构造器均要求 `clientX/clientY >= 0`（唯一写入/compact 边界，无新增截图或 probe） |

### SHA-256（R4 后）

| 文件 | 行数 | SHA-256 |
|---|---:|---|
| `CloudBagStateOwner.java` | 714 | `4f7bc06f31fc5e458468249dfad9e682703e45e2175950970df9bf0055da2bf8` |
| `BagWorkflowState.java` | 843 | `15b963dbaefb27fa628387b6a8dbe1f2bd8cd80def7d3ac3da07e9400aa4690f` |

### scope self-QA（仅 QA，不构成 Approved）

1. 写集：仅两个自建文件 + 本日志 append；shared `.remote`/assembly/host/caller/schema/tests/Maven 零触碰；无 Git mutation。`java.util.function.Consumer`、`TaskTransactionAction` import 均已不需要。
2. R1-R3 已闭环项未回退：context+revision 全 mutation 门、pending CAS、原子 generation 提交、canonical validator、enum layout、`</==/>` 接收、closed slot enum、stale 零 mutation 全部保留。
3. 构建门禁：未运行任何 Maven；仅对两个自建文件只读 `javac` 冒烟（exit 0）。独占 Cloud `mvn -q clean package` 待父级安排并作唯一批准。
4. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付完毕，持续 5 分钟重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #5 - TRUE PHYSICAL EOF CONTROL COPY / `W-BAG-C0-IMP1-R5` - 2026-07-13T18:54:40-04:00

位于本文历史锚点处的完整 `Parent Source Review #5 - BLOCKED` 由本 EOF 控制副本生效。结论
`P0=0 / P1=1 / P2=0`：`CloudBagStateOwner.java:408-423` 的静态 `describesExactRun` 不比较
`ownerInstanceId`；`BagWorkflowState.java:81,91-103,519-539` 又不保存 exact owner，故同 stable-run tuple 的
foreign-owner handle 仍可在消费校验前污染并占用 pending custody。

R5 只允许修改上述两个 Java 与本日志：workflow 构造时保存不可替换的 exact owner，由该 owner 的 instance
verification seam 同时校验 ownerInstanceId 和完整 run/window/revision tuple，任何 foreign handle 在写 pending
前零 mutation 拒绝。其余 R4 修复不回退。请在 `2026-07-13T19:12:06-04:00` 前于真实 EOF 追加
`CLAIMED task=W-BAG-C0-IMP1-R5 claimedAt=<ISO> uniqueWriteSet=<两个既有 Java + 本日志>`；领取后不限完成时长，
逾期只原样重发 C、绝不内部接管。shared `.remote`/assembly/host/caller/schema/tests/Maven 冻结。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-13T18:54:01-04:00

- task: W-BAG-C0-IMP1-R5
- claimedAt: 2026-07-13T18:54:01-04:00
- uniqueWriteSet:
  1. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\bag\CloudBagStateOwner.java`（修改，自建）
  2. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\bag\BagWorkflowState.java`（修改，自建）
  3. `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-bag-state-owner-worker-c.md`（append-only 本日志）

## External Worker C - Repair Implementation #5 (W-BAG-C0-IMP1-R5) - 2026-07-13T18:54:59-04:00

### 逐条落实 Review #5 精确返修条件

| 条件 | 修复 |
|---|---|
| 1. 构造时保存 exact owner 引用，不可替换 | `BagWorkflowState` 新增 `private final CloudBagStateOwner stateOwner;`，构造器第一参数注入（`Objects.requireNonNull`），生命周期内 final 不可替换。owner 本波无实例化路径 → state 亦传递性 dormant，由未来 assembly 波在其批准写集内先构造 owner 再构造 state |
| 2. `describesExactRun` 改 owner-instance 方法、先比 ownerInstanceId | `CloudBagStateOwner.describesExactRun(...)` 由 `static` 改为**实例方法**（仍 package-private、零 mutation、不暴露 raw getter）；校验顺序：先 `ownerInstanceId.equals(handle.ownerInstanceId)`（owner-instance 语义），再完整 `scope/taskRunId/window/stopEpoch/runRevision` tuple。注释中"owner-instance 校验推迟到 consumption"的表述已删除 |
| 3. record 只经保存的 exact owner 校验、写前 fail-closed | `recordPendingBoundBaseObservation` 改为 `stateOwner.describesExactRun(...)`——另一 owner 实例即使为同一 stable-run tuple 铸出 handle，也会在写 pending **之前**被 fail-closed（`IllegalStateException`，零 mutation），custody 槽不可能被 foreign owner 先占/污染 |
| 4. 其余 R4 修复不回退、写集不越界 | R1-R4 全部修复保持（结构化 permit、closed transition、pending CAS、clear 需 final permit、无实例化路径、anchor 非负、canonical validator 等）；仅两个自建文件修改 + 本日志 append；shared `.remote`/assembly/host/caller/schema/tests/Maven 零触碰；无 Git mutation；未运行任何 Maven（AB 仍写共享 Java），仅只读 `javac` 冒烟（exit 0） |

### SHA-256（R5 后）

| 文件 | 行数 | SHA-256 |
|---|---:|---|
| `CloudBagStateOwner.java` | 717 | `eafa0d7e4b98c6545a954867629603d402f3ebb10b4cc497f0130a24c4396ac1` |
| `BagWorkflowState.java` | 856 | `34ead25e28bd640bdaedcb51840940ca4d3009b896343a078fa981ad2be5ffd8` |

### scope self-QA（仅 QA，不构成 Approved）

1. foreign-owner handle 现在在 record 阶段即零写入拒绝（Review #4 要求 + Review #5 P1-1 关闭）；owner 侧 `requireOwnedHandle` 在 consumption 的第二道防线保持。
2. 独占 Cloud `mvn -q clean package` 待父级安排并作唯一批准。
3. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付完毕，持续 5 分钟重读本日志等待父级复审；自审不算 Approved。

## Parent Source Review #6 - TRUE PHYSICAL EOF CONTROL COPY / `W-BAG-C1-D1` - 2026-07-13T19:04:30-04:00

本文历史第 295 行的完整 `Parent Source Review #6` 由本真实 EOF 控制副本生效：R5 两个 state-core
**SOURCE APPROVED，P0=0/P1=0/P2=0**，Cloud package 待 AB 稳定后统一执行。

External C 当前任务为 `W-BAG-C1-D1`，只写 Bag authority assembly 接线 Design #1，Java/Maven 全冻结；须闭合
private-zero-factory owner 的唯一 trusted construction permit、owner+workflow 原子创建、resume/terminal/teardown、
exact current context、pending/final-consumed authority、容量/scope retirement、closed 文件方法表及 AB 顺序门。
请在 `2026-07-13T19:24:30-04:00` 前于真实 EOF 追加
`CLAIMED task=W-BAG-C1-D1 claimedAt=<ISO> uniqueWriteSet=<本日志>`。不得开放 public factory/raw owner getter，
不得新增第二 registry/ledger/map/thread 或接 host/caller/Task；逾期只原样重发 C，绝不内部接管。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-13T19:08:57-04:00

- task: W-BAG-C1-D1
- claimedAt: 2026-07-13T19:08:57-04:00
- uniqueWriteSet: 仅 `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-bag-state-owner-worker-c.md`（append-only 本日志；Java/Maven/schema/resources/tests 全冻结，仅只读取证）

## External Worker C - W-BAG-C1-D1 Design #1 - 2026-07-13T19:14:30-04:00

本设计只写 Bag authority assembly 接线；Java/Maven/schema/resources/tests 全冻结，本轮唯一写入为本日志。
取证基线：SOURCE APPROVED 的两个 state-core（`CloudBagStateOwner` `EAFA...96AC1`、`BagWorkflowState`
`34EA...5FFD8`）、现有 `.remote` `CloudTaskRunAuthorityAssembly`（package-private final、私有构造、
`create(...)` 以 weak claim set 强制"一 coordinator 一 assembly"、`AuthorityInstanceIdentity` 私构造
provenance token、initial/resume/terminal 三条路径、`CloudTaskRetainedActionState.navigationWorkflowState()/
removeRunState()` 的 run 级退休先例、`CloudGameContextStateOwner` 的 global/owner/non-terminal 容量护栏）。
全部为只读取证。

### D1-1 private-zero-factory owner 的唯一 trusted construction seam

- 接线波（未来批准写集）在 `com.bot.dhxy.service.bag` 新增 package-private final
  `CloudBagAuthorityComposition`（组合桥，仅此一个新 bag 文件）：
  - `public static BagAuthorityInstallation composeFor(RemoteTaskRunCoordinator coordinator)`（public 是跨包
    可编译的必要条件；结构性限制见下），内部持 `private static final Set<RemoteTaskRunCoordinator> CLAIMED`
    （WeakHashMap weak claim set——与现有 assembly `CLAIMED_COORDINATORS` 同一已批准反重复模式，非业务状态）；
    同一 coordinator 第二次 compose 直接 `IllegalStateException`。
  - 返回 nested `BagAuthorityInstallation(CloudBagStateOwner owner, ScopeTeardownCapability teardownCapability)`
    ——capability 仅在组合时刻一次性交付调用方，owner 实例此后不再暴露它（R4/R5 已固化）。
  - 结构性唯一论证：接线波在 `CloudTaskRunAuthorityAssembly` 私有构造器内（`gameContextStateOwner` 同位）执行
    `composeFor(coordinator)`；assembly 的 `create(...)` 已对 coordinator 做一次 weak claim，而 broker/coordinator
    图只能经 `.remote` 受控构造获得——业务代码要拿到"未被 claim 的 coordinator"必须先构造整个 broker 图，该路径
    已被 assembly 独占。因此第二个 owner 的构造在 compose claim 处 fail-closed；"assembly 是唯一 owner"由两层
    claim 共同结构性证明，而非注释约定。
  - `CloudBagStateOwner` 在接线波内（其批准写集）把私有构造器接给该桥：同包 package-private static
    `constructForComposition()`，业务包外不可见，包内唯一调用点即桥，桥又被 coordinator claim 门看住。本波不改
    任何 Java。

### D1-2 non-mintable composition/permit 的真实 owner 与可见性

- 真实 owner：`CloudTaskRunAuthorityAssembly`。接线波为其新增两个 `private final` 字段：
  `CloudBagStateOwner bagStateOwner` 与 `CloudBagStateOwner.ScopeTeardownCapability bagTeardownCapability`
  （构造器内经 D1-1 组合桥一次取得）。**不新增任何 raw owner getter**；owner 只经 D1-3 的 activation 注入
  per-run `BagWorkflowState` 与（W-BAG-S0 波的）per-runtime `BagService`。
- 三类 bag permit（`BoundBaseObservationPermit`/`VerifiedTransactionPermit`/`FinalConsumptionPermit`）铸造权：
  `.remote` 新增 public final `CloudTaskBagWorkflowAuthority`，**构造器 package-private**（非可铸；assembly 每
  run 构造一个，1:1 绑该 run 的 `CloudTaskRetainedActionState` 与 `finalConsumptionCoordinator`）。它在包内可读
  `WindowFactAction`/`TaskTransactionAction` 的 owner/record/address/occurrence 与 ledger 的
  final-consumed/compacted 事实，完成 Review #2-#5 要求的全部验证后，调用接线波加到两个 bag 文件里的 trusted
  factory：`static X mint(CloudTaskBagWorkflowAuthority authority, <exact bindings>)`——public 但第一参数是
  `.remote` 非可铸 capability 实例，业务代码无法取得，故结构性不可伪造（与 `CloudTaskServicePort` 靠非可铸
  action handle 把门是同一已批准模式）。permit 内部绑定字段（stateInstanceId/父事务/closed transition/step
  ordinal/observation handle）与 state 侧逐项校验已在 R4/R5 落地，不回退。

### D1-3 同一 run 的 owner+workflow 原子创建 / resume / terminal

- 初始（`createCurrentContextSlotActivation`）：在 `actionLedger.registerRun(runContext)` 之后、slot 发布之前
  （既有"一切可失败步骤先于发布"段），由 `CloudTaskRetainedActionState` 新增的 `bagWorkflowState()` 懒一次构造
  `new BagWorkflowState(bagStateOwner, scope, taskRunId, window tuple, stopEpoch)`，连同该 run 的
  `CloudTaskBagWorkflowAuthority` 一起挂入 runtime；任何失败沿既有 `rollbackRunRegistration` 回滚——owner 级三张
  cache 无需回滚（该 run 尚未写入）。retained state 是**既有**每 run 状态 owner，故无第二 registry/map。
- resume（`resumeTaskServiceRuntime`）：沿既有路径复用 `previousRuntime.retainedActionState()`——同一
  `BagWorkflowState`/authority 原样保留（cache、事务、cursor、pending custody 不重置）；新 revision 只体现在新
  `TaskExecutionContext`，state 侧 `revalidate()`+tuple 校验（R2-R5）已保证旧 revision 零 mutation。assembly 不
  做任何 bag 状态 reset。
- terminal（`closeAndReleaseTerminalTaskServiceRuntime`）：在 `acceptTerminalRun` 与 navigation
  `removeRunState()` 同位追加 `retainedActionState().removeBagWorkflowState()`（run 级退休：释放
  `BagWorkflowState`/authority/未消费 permit 引用）。**三张 page/anchor cache 与 geometry stream 按 R1.1 存续，
  不随 run terminal 清除。**

### D1-4 exact current context 与 pending/final-consumed authority

- exact context 唯一来源：runtime 的 `TaskExecutionContext`（slot 原子发布）；workflow 全部方法显式接收它，
  state 侧 `revalidate()` 已把 stale/foreign 收敛为零 mutation。无 ambient current、无 ThreadLocal。
- `VerifiedTransactionPermit`：authority 验证 `TaskTransactionAction` 的 owner/record/address 属于本 run 后
  mint，绑定 stateInstanceId + stable-run tuple + occurrence。
- `FinalConsumptionPermit`：authority 仅在 ledger/finalConsumptionCoordinator 观察到 exact child semantic
  address + occurrence 已 final-consumed（且 compaction 处适用）后 mint，绑定 closed transition + step ordinal
  （`PENDING_OBSERVATION_CLEAR` 另绑 exact observation handle）——R4 的"同 permit 幂等、跨 slot 不可复用"由字段
  绑定 + state 校验共同保证。
- `BoundBaseObservationPermit`：authority 验证 WindowFactAction 是本 workflow 当前 `BOUND_BASE` semantic slot
  的 current record 后 mint；owner 的 begin/accept 与 R5 的 exact-owner record 校验不变。

### D1-5 容量与 tenant/scope retirement

- 容量（写入侧 admission，护栏非业务）：接线波在 `CloudBagStateOwner` 写路径加
  GLOBAL_RETAINED_LIMIT / OWNER(scope)_RETAINED_LIMIT（对 visible/item/anchor 条目与 geometry stream 计数，沿
  `CloudGameContextStateOwner` 的 incrementExact + 超限 `IllegalStateException` fail-closed 先例）；**无 TTL/LRU/
  eviction**，R1.1 hint lifetime 不变。
- scope retirement：唯一入口仍是 capability 门的 `removeScopeState(capability, scope)`；触发点绑定既有 terminal
  路径中 `broker.requestRouteRetirement(clientScope, taskRunId)` 所属的同一 scope 生命周期语义——接线波在
  coordinator/broker 宣告该 clientScope 最终退休（其既有 seam）时由 assembly（capability 持有者）调用；本设计不
  发明新的 scope 生命周期事件。business Service 永远拿不到 capability。

### D1-6 closed 文件/方法表与 AB 顺序门

| 波次 | 文件 | New/Modify | 精确方法级 delta |
|---|---|---|---|
| 接线波 | `com.bot.dhxy.service.bag.CloudBagAuthorityComposition` | New | `composeFor(RemoteTaskRunCoordinator)` + nested `BagAuthorityInstallation` + weak claim set |
| 接线波 | `com.bot.dhxy.service.bag.CloudBagStateOwner` | Modify | 加 package-private `constructForComposition()`；三 permit trusted factory `mint(CloudTaskBagWorkflowAuthority, ...)`；写路径容量护栏。其余零改动 |
| 接线波 | `com.bot.dhxy.service.bag.BagWorkflowState` | Modify | 两 permit trusted factory `mint(CloudTaskBagWorkflowAuthority, ...)`。其余零改动 |
| 接线波 | `.remote.CloudTaskBagWorkflowAuthority` | New | public final、package-private 构造；verify + mint 三类 permit；1:1 绑 run retained state |
| 接线波 | `.remote.CloudTaskRunAuthorityAssembly` | Modify | 两 private final 字段 + 构造器 compose；initial 挂 workflow/authority；terminal 加 `removeBagWorkflowState()`；scope 退休处调 `removeScopeState` |
| 接线波 | `.remote.CloudTaskRetainedActionState` | Modify | `bagWorkflowState()` 懒一次持有 + `removeBagWorkflowState()`（navigation 同构） |
| W-BAG-S0（另卡） | `com.bot.dhxy.service.BagService` 等 | New | 本设计不授权，仅声明消费上述 seam |
| 全部 | host/caller/Task/schema/tests/resources | 0 修改 | 保持 dormant |

**顺序门：**(1) `.remote` 五文件与 AB 的 Full R0/R-X 在途写集重叠——接线波必须等 AB 相关文件 FINAL
APPROVED/hash 稳定后按文件级顺序合并，C 不预认领 AB 写集；(2) bag capture 的 `CapturePurpose` delta 仍按 Bag
报告 Review #3 的 "P2 Full R0 -> Quest optional artifactIntent -> Bag CapturePurpose" 次序，与本接线波互不越
序；(3) AB 写共享 Java 期间不运行并发 `mvn clean`；接线波落地后由父级独占 `mvn -q clean package` 作唯一构建
批准。

### 禁止面自查（本设计不引入）

public factory/raw owner getter（compose 的 public 面被双层 coordinator claim 结构性看住，owner/capability 无
getter）；第二 registry/ledger/map/thread（复用 retainedActionState/既有 ledger；weak claim set 为已批准反重复
模式）；host/caller/Task 接线（全部 dormant）。

### self-QA（仅 QA，不构成批准）

1. 六项闭合点（construction seam、permit owner/可见性、原子创建/resume/terminal/teardown、context 与 permit
   authority、容量与 retirement、文件方法表、AB 顺序门）均有落点且以现有已批准模式为据。
2. 本轮零 Java/Maven/schema/resources/tests 写入；唯一写入为本日志 append；无 Git mutation；未运行 Maven。
3. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 Design #1 完毕，持续 5 分钟重读本日志等待父级 review；自审不算 Approved。

## Parent Design Review #6 - BLOCKED / `W-BAG-C1-D2` - 2026-07-13T19:25:35-04:00

父级对照实际包可见性、coordinator 构造面与 route retirement 实现复审 D1。结论：
**BLOCKED，P0=0/P1=3/P2=1**；本轮不得修改 Java。

### P1-1 - composition seam 跨包不可编译；放宽为 public 又可绕过唯一 owner

- **证据：** D1 764-778 把 `CloudBagAuthorityComposition` 定义为
  `com.bot.dhxy.service.bag` 的 package-private class，却要求
  `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskRunAuthorityAssembly` 调它；public static method 不能让一个
  package-private 顶层类跨包可见。若把类/installation 改 public，`RemoteTaskRunCoordinator.java:34,64-68`
  又公开允许任意业务代码 `new RemoteTaskRunCoordinator()`，随后调用 `composeFor(new coordinator)` 即可获得第二
  owner/capability；weak claim 只保证“每个传入 coordinator 一次”，不保证“进程唯一 assembly owner”。此外
  `constructForComposition()` package-private 对整个 bag 包开放，不是唯一调用点的结构性证明。
- **影响：** 当前表无法编译；简单 public 化则重开第二业务权威与 teardown capability 泄漏。
- **精确返修条件：** 改为由真实 assembly 唯一可铸的、跨包可编译的 non-mintable composition permit。
  `CloudBagStateOwner` 的创建入口必须要求并验证该 exact permit/assembly identity；不得仅接收 public coordinator，
  不得返回 public raw owner+capability installation 给任意调用方。列出 exact FQCN、visibility、constructor/factory
  signature 和 same-assembly identity check，并证明第二 owner 在编译面或运行 identity gate 被拒。

### P1-2 - 三类 permit 的“trusted factory”仍允许合法 authority 引用绕过验证

- **证据：** D1 787-795、842-846 仅写 public `mint(CloudTaskBagWorkflowAuthority authority,
  <exact bindings>)`，未列 factory 内调用的 exact authority verification API/identity，也未说明 Service/runtime
  是否持有 authority 引用。只验证参数 non-null 或 class identity，无法证明 window-fact/transaction/final-consumed
  record 正是 authority 已核验的同一条记录。
- **影响：** 持有合法 authority 对象的业务代码可把伪造 bindings 交给 public factory，绕过 ledger/current-record
  门，产生未授权 cache/transaction mutation。
- **精确返修条件：** mint 必须消费 authority 内部验证后产生的不可伪造 proof，或只由 authority 的
  verify-and-mint 方法构造；permit 必须绑定 exact assembly identity、owner instance、run tuple、semantic address/
  occurrence/record generation/finality proof。给出每类 permit 的 closed 方法签名与错 owner/stale record/old revision
  零 mutation 矩阵，不能靠“唯一调用点”注释。

### P1-3 - 把 per-run route retirement 误当 clientScope 最终退休，直接违背 cache 生命周期

- **证据：** D1 808-811 明确三张 cache 应跨 task-run terminal 存续；但 832-835 又要在
  `broker.requestRouteRetirement(clientScope, taskRunId)` seam 调 `removeScopeState`。实际
  `CloudTaskRunAuthorityAssembly.java:368-372` 对每个 terminal run 都调用它，
  `RemoteGameCommandBroker.java:165-182` 只标记该 run/route retirement，没有“此 clientScope 永久结束且无后续
  run”的证明或 callback。
- **影响：** 每次任务结束都会错误清空本应跨 run 保留的 Bag hints，改变 HEAD cache 语义；反之若不清理，
  当前容量账又没有真实 scope 退休释放点。
- **精确返修条件：** 不得挂 per-run terminal seam。要么明确本波 scope retirement 保持 dormant，并说明有界
  容量耗尽时的既定运维门；要么复用/新增经父级批准的 exact client-session/scope terminal authority，且只有证明
  scope 永久退休后才能消费 teardown capability。不得发明 TTL/LRU/每-run 清理。

### P2-1 - initial failure/terminal retirement 的 owner 状态回滚表不完整

- **证据：** D1 799-803 只称“该 run 尚未写入”，但 lazy workflow/authority 已被 retained state 持有；
  terminal 808-810 又只 remove 引用，未列 initial 构造在 slot publish 前后失败时 exact remove/幂等路径。
- **返修条件：** 补 PREPARED/registered/not-published/published/resume/terminal 的引用所有权表，说明每个失败点
  谁清理 workflow/authority、是否复用同对象、重复 terminal 是否幂等；不得新增第二 registry。

External C 当前任务 `W-BAG-C1-D2`：须在 `2026-07-13T19:45:35-04:00` 前于真实 EOF 追加
`CLAIMED task=W-BAG-C1-D2 claimedAt=<ISO> uniqueWriteSet=<本日志>`，随后只写 Design Repair #1 Delta；
Java/Maven/schema/resources/tests/host/caller 全冻结。逾期只原样重发 C，绝不内部接管。Worker self-QA 不构成批准。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## External Worker C - CLAIMED - 2026-07-13T19:30:49-04:00

- task: W-BAG-C1-D2
- claimedAt: 2026-07-13T19:30:49-04:00
- uniqueWriteSet: 仅本 append-only 日志 `D:\mavenProject\DHXY\docs\superpowers\planseports�6-07-13-cloud-bag-state-owner-worker-c.md`（Java/Maven/schema/resources/tests/host/caller 全冻结，仅只读取证）

## External Worker C - W-BAG-C1-D2 Design Repair #1 Delta - 2026-07-13T19:33:00-04:00

（校正：上一 CLAIMED 块的 uniqueWriteSet 行因 printf 反斜杠转义显示乱码；实际唯一写集就是本 append-only 日志
`docs/superpowers/plans/reports/2026-07-13-cloud-bag-state-owner-worker-c.md`，Java/Maven/schema/resources/
tests/host/caller 全冻结。append-only 不改旧块，特此记录。）

本 Delta 只替换 Design #1 被 `Parent Design Review #6` 阻塞的 P1-1/P1-2/P1-3 与 P2-1；D1 已通过的部分
（exact context 唯一来源、resume 复用同一 retained state、无第二 registry/thread、AB 顺序门、无 TTL/LRU/
eviction）不重开。本轮仍只写本日志，不改任何 Java。取证：`RemoteTaskRunCoordinator` 构造器确为 **public**
（`RemoteTaskRunCoordinator.java:31`）、`CloudTaskRunAuthorityAssembly` 为 package-private final 且私有构造、
`AuthorityInstanceIdentity` 私构造 provenance token、`CloudTaskRunActionLedger.rollbackRunRegistration(...)`
存在、terminal 路径每 run 调用 `broker.requestRouteRetirement(...)`。

### R1（关闭 P1-1）：跨包可编译、由 assembly 唯一可铸的 non-mintable 构造 permit

撤回 D1 的 bag 包 package-private 桥 `CloudBagAuthorityComposition`（顶层 package-private 类跨包不可编译，是 D1
的实错）与 package-wide `constructForComposition()`。最终结构：

- `.remote` 新增 `public final class CloudBagOwnerConstructionPermit`，**构造器 package-private**。它是跨包可编译
  的 non-mintable token：`com.bot.dhxy.service.bag` 可以把它作为方法参数类型引用（public 类型），但**只有
  `com.yueyunfe.dhxy.cloudbrain.remote` 包内代码能构造它**（构造器 package-private）。permit 内部持有对铸造它的
  assembly 的 `AuthorityInstanceIdentity`（该类型是 `.remote` 内 package-private，permit 与 assembly 同包，可作
  字段），并暴露一个 package-private accessor 供后续 `.remote` 侧 authority 校验同一 assembly。
- `CloudBagStateOwner`（bag 包）删除 `create(Consumer)` 与 `constructForComposition()`；改为
  `public static CloudBagStateOwner forAssembly(CloudBagOwnerConstructionPermit permit)`。签名 public 是跨包
  可编译的必要条件，但**结构性不可绕过**：owner 唯一实例化路径要求一个 `CloudBagOwnerConstructionPermit` 实参，
  而该 permit 的构造器 package-private 在 `.remote`，业务代码无法取得 → 无法调 `forAssembly` 造第二 owner。
  owner 私有构造器内保存 permit 携带的 assembly identity 引用（final，不可替换），并把 `ScopeTeardownCapability`
  一次性交回 permit（`permit.acceptTeardownCapability(cap)`，package-private，供 assembly 取回后私藏）。
- 编译面 + 运行 identity gate 双证明：
  1. **编译面**：`CloudBagOwnerConstructionPermit` 构造器 package-private → 任何 `.remote` 外类无法 `new`；
     `.remote` 内唯一构造点是 `CloudTaskRunAuthorityAssembly` 私有构造器（`gameContextStateOwner` 同位）。
  2. **进程唯一**：assembly 本身经现有 `create(...)` 的 coordinator weak-claim 保证进程内一 coordinator 一
     assembly；permit 只在该唯一 assembly 的构造器铸一次；因此 owner 进程唯一，且 `new RemoteTaskRunCoordinator()`
     再 compose 的路径不存在——已无 `composeFor(coordinator)` 入口，业务代码即使 new 一个 coordinator 也拿不到
     permit。
  3. **运行 gate**：owner 保存 permit 的 assembly identity；R2 的 authority 与 owner/state 交互时校验同一 identity，
     跨 assembly 引用零 mutation 拒绝。

exact FQCN / visibility / signature：

| FQCN | visibility | 关键成员 |
|---|---|---|
| `com.yueyunfe.dhxy.cloudbrain.remote.CloudBagOwnerConstructionPermit` | `public final class` | 构造器 `CloudBagOwnerConstructionPermit(AuthorityInstanceIdentity)` **package-private**；`AuthorityInstanceIdentity authorityIdentity()` package-private；`void acceptTeardownCapability(ScopeTeardownCapability)` / `ScopeTeardownCapability teardownCapability()` package-private |
| `com.bot.dhxy.service.bag.CloudBagStateOwner#forAssembly` | `public static` | 入参 `CloudBagOwnerConstructionPermit`（不可铸）；返回 owner；私有构造器保存 identity 并回交 teardown capability |

### R2（关闭 P1-2）：verify-and-mint —— authority 内部派生 bindings、proof 不可铸

撤回 D1 的 public `mint(CloudTaskBagWorkflowAuthority authority, <exact bindings>)`（持 authority 的业务代码可注入
伪造 bindings）。最终：三类 bag permit 只能由 authority 的 verify-and-mint 内部产出，caller 只交 raw retained
handle，绝不能交 bindings。

- `.remote` 新增 `public final class CloudBagPermitMintProof`，**构造器 package-private**（只有 authority 所在的
  `.remote` 能造）。bag 包 permit 工厂签名改为需要该 proof：
  - `CloudBagStateOwner.BoundBaseObservationPermit.mint(CloudBagPermitMintProof, <canonical bindings>)`；
  - `BagWorkflowState.VerifiedTransactionPermit.mint(CloudBagPermitMintProof, <canonical bindings>)`；
  - `BagWorkflowState.FinalConsumptionPermit.mint(CloudBagPermitMintProof, <canonical bindings>)`。
  三者 public static（跨包可编译）但都需 `CloudBagPermitMintProof`；业务代码拿不到 proof → 无法造任何 permit。
- `.remote.CloudTaskBagWorkflowAuthority`（public final、构造器 package-private、assembly 每 run 1:1 绑该 run 的
  `CloudTaskRetainedActionState`/`finalConsumptionCoordinator`/`AuthorityInstanceIdentity`）只暴露 verify-and-mint
  方法，**签名只接受 raw retained handle，不接受 bindings**：
  - `mintBoundBaseObservationPermit(WindowFactAction rawWindowFactAction)`：在包内读该 action 的 owner/record/
    address/current generation，验证它正是本 workflow 当前 `BOUND_BASE` semantic slot 的 current record；通过后
    **authority 自身派生 canonical bindings**（owner instance、run tuple、semantic address、record generation），
    `new CloudBagPermitMintProof(...)` 后调 `BoundBaseObservationPermit.mint(proof, canonicalBindings)`。
  - `mintVerifiedTransactionPermit(TaskTransactionAction rawTransaction)`：验证 transaction 的 owner/record/
    address/occurrence 属于本 run；派生 stateInstanceId+stable-run tuple+occurrence 后 mint。
  - `mintFinalConsumptionPermit(<retained child action handle>, WorkflowTransition, expected step ordinal[, exact
    observation handle])`：只在 ledger/`finalConsumptionCoordinator` 观察到该 exact child semantic address+
    occurrence 已 final-consumed（compaction 处适用）后 mint；permit 绑定 closed transition + step ordinal
    （`PENDING_OBSERVATION_CLEAR` 另绑 observation handle）。
- caller 唯一入口是 authority 的 verify-and-mint（读内部 record 自派生 bindings），无法把伪造 bindings 送进 permit
  factory；proof 不可铸，故 permit 不可绕过 authority。

每类 permit 的错 owner / stale record / old revision 零 mutation 矩阵（验证发生在 authority verify 阶段，失败即
不造 proof → 不造 permit → state 侧后续 R4/R5 的绑定校验再作第二道门）：

| permit | authority verify 拒绝条件（不铸） | state 侧第二道门（既有 R4/R5，不回退） |
|---|---|---|
| BoundBaseObservation | action owner≠本 run authority / record 非 current BOUND_BASE slot / generation 过期 | owner.begin/accept 校验 exact-owner + scope/taskRun/window/stopEpoch/runRevision + permit 对象同一性 |
| VerifiedTransaction | transaction owner/record/address 不属本 run / occurrence 不匹配 | state 校验 stateInstanceId + stable-run tuple；未完成时换事务 fail-closed |
| FinalConsumption | 目标 child 未 final-consumed/未 compacted / address·occurrence 不符 | state 校验 stateInstanceId + 父事务对象 + closed transition + step ordinal（+observation handle），同 permit 幂等、跨 slot 零复用 |

### R3（关闭 P1-3）：scope retirement 本波保持 dormant，不挂 per-run terminal seam

撤回 D1-5 在 `broker.requestRouteRetirement(clientScope, taskRunId)` seam 调 `removeScopeState` 的接线。证据确认该
seam 对**每个 terminal run** 触发（`CloudTaskRunAuthorityAssembly.java:368-372`），而 broker 只标记该 run/route
退休，无"该 clientScope 永久结束且无后续 run"的证明/callback（`RemoteGameCommandBroker` 相应实现）。因此：

- 本波 scope retirement **保持 dormant**：assembly 取得并私藏 `ScopeTeardownCapability`，但**不在任何 per-run
  terminal 路径消费它**；三张 page/anchor cache 与 geometry stream 按 R1.1 跨 task-run terminal 存续，HEAD cache
  语义不变。
- 有界容量耗尽的既定运维门：owner 写路径 admission 达到 `GLOBAL_RETAINED_LIMIT`/`OWNER(scope)_RETAINED_LIMIT`
  时 `IllegalStateException` fail-closed（与 `CloudGameContextStateOwner` 既有护栏同构，非业务状态、无 TTL/LRU/
  eviction）；释放只能由未来经父级批准的 **exact client-session/scope terminal authority** 波接入——该波须提供
  "scope 永久退休且无后续 run"的证明，之后才允许消费 `removeScopeState(capability, scope)`。本 Delta 只声明该
  dormant 依赖，不发明新的 per-run 清理或 TTL。

### R4（关闭 P2-1）：owner/workflow 引用所有权与失败/终态回滚表

lazy `BagWorkflowState`/`CloudTaskBagWorkflowAuthority` 由**既有** per-run `CloudTaskRetainedActionState` 持有
（与 `navigationWorkflowState` 同一持有者，无第二 registry）。三张 cache/geometry 归 assembly 级单一
`CloudBagStateOwner`，不随 run 生命周期回滚。

| 阶段/失败点 | workflow/authority 引用所有权与清理 | owner 三张 cache/geometry |
|---|---|---|
| PREPARED（`createCurrentContextSlotActivation` 内，registerRun 之前失败） | retained state 尚未注册、bag workflow 尚未 lazy 构造；无引用需清 | 该 run 未写入，无需回滚 |
| registered-not-published（registerRun 之后、slot publish 之前失败） | lazy bag workflow/authority 若已构造，随 `actionLedger.rollbackRunRegistration(runContext)` 连同 retained state 整体丢弃（同 navigation 回滚语义）；不单独 remove | 未写入，无需回滚 |
| published（slot 原子发布成功） | bag workflow/authority 随 runtime 存活；owner 注入完成 | 正常按 exact-context 门读写 |
| resume（`resumeTaskServiceRuntime`） | 复用 `previousRuntime.retainedActionState()` → **同一** bag workflow/authority 对象，不新建、不 reset；新 revision 仅体现于新 `TaskExecutionContext` | 存续；旧 revision 经 `revalidate()` 零 mutation |
| terminal（`closeAndReleaseTerminalTaskServiceRuntime`） | 在 `acceptTerminalRun` 与 `navigationWorkflowState().removeRunState()` 同位调 `retainedActionState().removeBagWorkflowState()`：释放 workflow/authority/未消费 permit 引用 | **存续（R1.1）**，不随 run terminal 清除 |
| 重复 terminal（interrupted 后同一 exact terminal binding 重试） | `removeBagWorkflowState()` 幂等：引用已 null → no-op；与既有 State terminal 重试路径一致 | 无变化 |

不新增第二 registry/map/thread；失败路径全部复用既有 `rollbackRunRegistration`/terminal 幂等语义。

### 修订后 closed 文件/方法表（替换 D1-6 相应行）

| 波次 | 文件 | New/Modify | 精确 delta |
|---|---|---|---|
| 接线波 | `.remote.CloudBagOwnerConstructionPermit` | New | public final；构造器 package-private(AuthorityInstanceIdentity)；identity/teardown-capability package-private accessor |
| 接线波 | `.remote.CloudBagPermitMintProof` | New | public final；构造器 package-private |
| 接线波 | `.remote.CloudTaskBagWorkflowAuthority` | New | public final、构造器 package-private；三 verify-and-mint（只收 raw retained handle）；1:1 绑 run retained state |
| 接线波 | `com.bot.dhxy.service.bag.CloudBagStateOwner` | Modify | 删 `create(Consumer)`；加 `forAssembly(CloudBagOwnerConstructionPermit)`；三 permit factory 改 `mint(CloudBagPermitMintProof, canonicalBindings)`；写路径容量护栏 |
| 接线波 | `com.bot.dhxy.service.bag.BagWorkflowState` | Modify | 两 permit factory 改 `mint(CloudBagPermitMintProof, canonicalBindings)` |
| 接线波 | `.remote.CloudTaskRunAuthorityAssembly` | Modify | 构造器内铸 `CloudBagOwnerConstructionPermit` → `CloudBagStateOwner.forAssembly(...)` 取 owner+私藏 capability；两 private final 字段；initial 挂 workflow/authority；terminal 加 `removeBagWorkflowState()`；**不挂 scope teardown** |
| 接线波 | `.remote.CloudTaskRetainedActionState` | Modify | `bagWorkflowState()` 懒一次持有 + `removeBagWorkflowState()`（navigation 同构） |
| 全部 | host/caller/Task/schema/tests/resources | 0 修改 | dormant |

顺序门不变（AB `.remote` 文件 FINAL/hash 稳定后按文件级合并；CapturePurpose 次序；不并发 `mvn clean`；父级独占
`mvn -q clean package` 作唯一构建批准）。

### 禁止面自查（本 Delta 不引入）

无 public factory 造第二 owner（`forAssembly` 需不可铸 permit）；无 raw owner/capability getter；无第二
registry/ledger/map/thread（复用 retainedActionState/既有 ledger；permit 反重复靠 assembly 唯一性）；无 per-run
清理/TTL/LRU（scope retirement dormant）；不接 host/caller/Task。

### self-QA（仅 QA，不构成批准）

1. P1-1：跨包可编译（permit/proof 为 `.remote` public 类型）+ 不可绕过（构造器 package-private，仅 assembly
   铸）双证明；已删不可编译的 bag 桥与 package-wide 工厂。
2. P1-2：verify-and-mint 只收 raw retained handle、authority 自派生 bindings、proof 不可铸；给出三类 permit 的
   拒绝/零 mutation 矩阵。
3. P1-3：scope retirement dormant，不挂 per-run terminal seam；容量耗尽 fail-closed，释放留待经批准的 scope
   terminal authority 波。
4. P2-1：给出 PREPARED/registered-not-published/published/resume/terminal/重复 terminal 的引用所有权与清理表，
   幂等且无第二 registry。
5. 本轮零 Java/Maven/schema/resources/tests 写入；唯一写入为本日志 append；无 Git mutation；未运行 Maven。
6. 无已批准业务差异；按 `0114604e` 基线等价迁移。

External Worker C 交付 Design Repair #1 Delta 完毕，持续 5 分钟重读本日志等待父级复审；自审不算 Approved。
