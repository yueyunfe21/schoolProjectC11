# CR271 Cloud Checkpoint and Pause/Resume Rehydration Collaboration Log

> Append-only collaboration log. Do not edit or delete earlier entries. The one external worker may append only the requested design/repair/implementation sections. Parent approval is required before Java changes.

## Parent Brief #1 - 2026-07-12

### Context

- Repositories: `D:\mavenProject\DHXY` and `D:\mavenProject\dhxy-cloud-brain`; protect all dirty work and perform no Git mutation.
- Closed prerequisites with `P0/P1/P2=0`: task-run lifecycle/revision fences, Cloud Task/Service context, retained-action typed Service port, and Cloud-native `ImageProcessorService`.
- Local DHXY `TaskCheckpoint`, `TaskSleep`, `TaskExecutionContextHolder`, `TaskPauseToken`, and `TaskPauseResumeReconciler` are local runner/window/thread mechanics. They must not be copied into Cloud as authority.
- Cloud `TaskExecutionContext` currently exposes immutable scope/taskRun/window/stopEpoch/runRevision and read-only `revalidate()`. Any pause/resume/stop/complete transition increments runRevision; resume requires a new ACTIVE revision and fresh local execution confirmation, so an old context and its retained handles remain permanently stale.
- Host/task execution remains dormant. This slice designs the compatibility and rehydration boundary only; no Task/Service cohort activation yet.

### Design objective

Design the minimum Cloud checkpoint/sleep/rehydration contract that lets existing synchronous Task/Service code preserve its checkpoint call shape while distinguishing pause/resume rehydration from terminal stop/completion and other authorization failures. A resume must reconstruct a new exact context from trusted persisted business phase/action state without changing phase/retry/fallback/timing semantics or minting new action identity for an already-retained business action.

### Required design coverage

1. Complete compatibility inventory for local `TaskCheckpoint`, `TaskSleep`, `TaskExecutionContextHolder`, `TaskPauseResumeReconciler`, and all public `TaskExecutionContext` stop/pause APIs used by migrated classes. State what remains local, what gets a Cloud-safe counterpart, and what cannot compile unchanged.
2. Typed Cloud checkpoint outcomes/exceptions for: current ACTIVE+confirmed; PAUSED/current newer revision; resumed ACTIVE newer revision; STOPPED; COMPLETED; unknown/missing binding; scope/session/window/stopEpoch mismatch; unconfirmed current revision. Do not infer only from free-text reason when binding/status fields can decide.
3. Exact safe-point semantics. No ThreadLocal/current-run singleton, no local `TaskPauseToken`, no busy polling, no automatic retry, no new TTL. Explain sleep interrupted/stop behavior and how pause occurring during a synchronous sleep is observed without allowing mechanical actions.
4. Rehydration owner and state: which package-private authority creates the new context after resume+confirm; which persisted catalog stores task phase, retry/fallback counters, stable action address and IDs, last final/UNKNOWN outcome, and caller-required immutable metadata. Do not persist HWND objects, mutable `BufferedImage`, raw requests, bearer capability, local paths, or thread state.
5. Stable identity: old context/opaque action handles never revive. Rehydration may reconstruct a new handle only from the same persisted business action address/IDs and exact current run revision; UNKNOWN redelivery must reuse identical request bytes where still permitted, and final outcomes remain immutable.
6. Atomicity/crash recovery: phase checkpoint and retained-action catalog update ordering versus command registration/dispatch/outcome; prevent phase advance without durable action result, double dispatch, new-ID retry, and stale revision resurrection. State whether persistence exists today; do not claim durability if only in-memory.
7. Local `TaskPauseResumeReconciler`: preserve it locally for WindowRuntimeContext volatile timers/dialog/pathing reconciliation, or define a typed fact handoff. Cloud must not clear/compensate local caches directly. Explain how local reconcile result affects Cloud rehydration without becoming a second business brain.
8. Host lifecycle/reachability: all new code compile-ready/dormant; no host/poller/thread/UI/capture/input startup. Define the later activation owner but do not implement/start it in this slice unless parent separately approves.
9. Capacity, tenant isolation, cleanup/retention, observability, and exact denial/fail-closed behavior. No cross-tenant catalog keys and no unbounded orphaned action state without an explicit later operational gate.
10. Exact proposed files/visibility/diff budget, state machine/pseudocode, migration cohort gate, and verification: no new/restored tests; Cloud `mvn -q clean package` without skip; DHXY compile only if separately approved Java changes are unavoidable.

### Hard constraints

- First worker turn is design only. Append `## External Worker - Design #1 - 2026-07-12` here and stop.
- Do not modify Java/Maven/resources/other docs/CR status/dashboard before parent `DESIGN APPROVED`.
- Do not copy or expose local `WindowRuntimeContext`, `TaskPauseToken`, `TaskExecutionContextHolder`, HWND/geometry, input queue, screenshot, temp path, or runner authority in Cloud.
- Do not add automatic retry, TTL, takeover, lifecycle transition, public raw request/poll/outcome/completion, or host activation.
- Do not change business phase/retry/fallback/click/navigation/stop/pause timing semantics. `无已批准业务差异；按基线等价迁移。`
- No tests, applications, server/host/poller/UI/capture/OCR/input, production cutover, or Git mutation.

## External Worker - Design #1 - 2026-07-12

### 结论与风险

- 目标边界可行，但当前源码尚不具备可声称的 durable rehydration：`RemoteTaskRunCoordinator`、broker、
  `CloudTaskRunActionLedger`、`CloudTaskRetainedActionState` 全部只在内存 `HashMap` 中；
  `RemoteTaskRunRoutes.create(...)` 每次新建 coordinator/broker；`CloudServiceStorage` 只做 tenant/user 路径解析，
  没有 authority journal、事务或恢复装载。
- `P0=0 / P1=3 / P2=0`，Design #1 建议 **BLOCKED before Java**。三个 P1 都有明确父级决策点，见下文。
- 已核对 `docs/业务逻辑.md` 的五倍/修罗业务基线门、通用热启动 Policy、修罗基线
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 与“STOP/暂停中断不是业务 FAILED、暂停时长补偿 watchdog”规则。
  本设计只移动 checkpoint/rehydration ownership，不改变 hot-start 顺序、phase/retry/fallback、等待时长或动作顺序。
  `无已批准业务差异；按基线等价迁移。`

### P1 父级决策点

1. **P1-1 durable authority backend 缺失**
   - 选项 A（推荐）：本 CR 先批准一个单一、tenant-scoped、append-only authority journal，把 coordinator lifecycle、
     execution confirmation/reconcile fact、phase catalog、retained attempt、broker dispatch marker/outcome 放在同一事务/
     WAL 边界；journal 完成前 host/cohort 仍 dormant。
   - 选项 B：本 CR 只实现同进程 checkpoint classifier + schema interface，不实现/宣称 crash rehydration；所有 Task
     cohort 继续禁止激活，另开 durable authority CR。
   - 不可选：只把 phase 写 JSON、同时让 coordinator/broker/ledger 保持内存。那会在重启后丢失 run/action authority，
     却留下可误用的“已持久 phase”，可能重复输入或跳 phase。
2. **P1-2 resume confirmation 缺少 local reconcile fact**
   - 选项 A（推荐）：扩展现有 `CONFIRM_EXECUTION`，当 coordinator 当前 ACTIVE revision 是一次 RESUME 产生的新
     revision 时，必须同时携带 exact revision/window-bound `LocalPauseResumeReconcileFact`；coordinator 在同一同步块/
     journal transaction 中写 reconcile fact 与 confirmed revision。
   - 选项 B：新增独立 `CONFIRM_RESUME_EXECUTION` action。它更显式，但扩大 wire action surface。
   - 不可选：先普通 confirm、后异步上报 reconcile；两者之间会出现 confirmed ACTIVE 但本地 volatile state 未对账的
     机械动作窗口。
3. **P1-3 crash during synchronous sleep 的 timing authority**
   - 选项 A（推荐）：catalog 增加 `SleepContinuation`，在 sleep 前 durable 写总时长、已消费业务时长、coordinator
     pause-progress baseline；正常醒来写完成 safe-point。恢复只睡未消费 remainder，不用 TTL、不轮询。
   - 选项 B：进程 crash 时不恢复 mid-sleep，整个 run fail-closed STOPPED；同进程 pause/resume 仍可 rehydrate。
   - 不可选：crash 后无证据地重睡完整时长或直接跳过剩余时长；两者都会改变现有 timing 语义。

### 本地兼容 inventory

| 本地 API / 调用形态 | 当前语义 | Cloud 方案 | 是否可原样编译/激活 |
|---|---|---|---|
| `TaskExecutionContext.throwIfStopRequested()` | stop 前后检查；pause/identity suspension 阻塞；返回 blocked ms | Cloud 增加同签名方法；CURRENT 返回 `0`，pause/resume 抛 typed rehydration signal，STOPPED 抛 stop，COMPLETED/deny typed unwind | 调用形态可编译；消费 blocked-ms 的 caller 必须迁为 catalog pause compensation |
| `TaskExecutionContext.isStopRequested()` | 读 local stop token | coordinator typed snapshot，仅 STOPPED 为 true；不得把 pause/denial 当 stop | 可编译，需新增 Cloud 方法 |
| `TaskExecutionContext.isPauseRequested()` | 读 local pause token | typed snapshot 当前 status=PAUSED 才为 true | 可编译，需新增 Cloud 方法；不得用于等待 |
| `TaskCheckpoint.throwIfStopRequested(context,msg)` | explicit context + thread interrupt | 新 Cloud counterpart 只用 explicit context typed checkpoint；不读 ThreadLocal/thread interrupt | 可原样编译；返回值只在无 lifecycle change 时为 0 |
| `TaskCheckpoint.throwIfStopRequested(holder,msg)` | ThreadLocal current context | 不复制 holder；必须显式传 context | **blocked**，调用者需改签名/注入路径 |
| `TaskCheckpoint.throwIfStopRequested(context,holder,msg)` | 双 context 过渡策略 | Cloud 只保留 authoritative explicit context | **blocked**；修罗等调用点需移除 holder 参数但不改 checkpoint 位置 |
| `TaskCheckpoint.throwIfStopRequested(stopToken,msg)` | observer/local stop-only | Cloud 不暴露 stop token；observer 必须接收 typed read-only checkpoint view | **blocked**；例如 `AutoCombatService` stop-token overload 需改 explicit context |
| `TaskCheckpoint.throwIfInterrupted(msg)` | JVM thread interruption -> stop | Cloud 不把 thread flag当 lifecycle truth | **blocked**；未来 host 可在 sleep interrupt 后先 typed classify，再 fail-closed unwind |
| `TaskSleep.sleepOrStop(context,ms,msg)` | checkpoint、单次 sleep、checkpoint | Cloud counterpart 同调用形态；单次 interruptible sleep，前后 typed checkpoint | 可编译；pause during sleep 在后 checkpoint unwind，不继续机械动作 |
| `TaskSleep.sleep(ms)` | 无 context 的机械/legacy sleep | 保留本地；Cloud business cohort 禁止使用 | **blocked**，必须改为 explicit `sleepOrStop` 或保留类在 local cohort |
| `TaskExecutionContextHolder` 全 API | per-thread current context | 完整保留本地；Cloud 禁 ThreadLocal/current-run singleton | **不可复制**；`NavigationService`、`NpcClickService`、vision 等 holder caller 未改造前不得入 Cloud cohort |
| `TaskPauseToken` 全 API | monitor wait、pause revision、timer accounting | 完整保留本地 runner/input worker；Cloud lifecycle revision + catalog pause progress 替代 | **不可复制** |
| `TaskPauseResumeReconciler` | 读/清/补偿 `WindowRuntimeContext` volatile dialog/pathing/timers | 完整保留本地；只输出 bounded typed reconcile fact | **不可复制**；Cloud 不清 cache、不判断窗口事实 |
| context HWND/title/geometry/runtime getters | local window authority | 不补假 getter；机械事实只能 typed port/capture/fact | 使用这些 getter 的 caller 保持 blocked |

- 现有调用量只作 cohort 风险证据：`TaskCheckpoint.throwIfStopRequested` 约 155 处，`TaskSleep.*` 约 188 处，
  `TaskExecutionContextHolder` 约 38 处，reconciler 相关约 13 处。不能用“一次复制 utility”冒充完整兼容。
- 显式-context 主体候选：`BaseTaskTemplate`、`TaskStepExecutor`、`BagService`、五环/五倍/修罗部分路径。
  holder/raw-sleep/runtime getter 混用的 class 必须逐 class 过 cohort gate；本切片不迁业务 class。

### Typed checkpoint outcome / exception

建议 public powerless 类型放在 `com.bot.dhxy.runner.stop`，classifier/authority 保持 package-private：

```text
TaskCheckpointOutcome
  CURRENT_ACTIVE_CONFIRMED
  PAUSED_NEWER_REVISION
  RESUMED_ACTIVE_NEWER_REVISION_CONFIRMED
  ACTIVE_CURRENT_REVISION_UNCONFIRMED
  ACTIVE_NEWER_REVISION_UNCONFIRMED
  RESUME_RECONCILE_UNCONFIRMED
  STOPPED
  COMPLETED
  PREPARED_NOT_EXECUTABLE
  MISSING_BINDING
  SCOPE_OR_SESSION_MISMATCH
  WINDOW_MISMATCH
  STOP_EPOCH_MISMATCH
  TASK_TYPE_MISMATCH
  STALE_OR_FUTURE_REVISION
```

`TaskCheckpointDecision` 字段固定为：`outcome`、expected/current revision、expected/current stopEpoch、current status、
`executionConfirmed`、`reconcileConfirmed`、sanitized reason code。scope mismatch 对 public caller 只给枚举，不返回别的
tenant binding/window/phase。

Exception 映射：

- `CURRENT_ACTIVE_CONFIRMED`：不抛，返回 `0L`。
- `PAUSED_NEWER_REVISION`、`RESUMED_ACTIVE_NEWER_REVISION_CONFIRMED`：抛
  `TaskCheckpointTransitionException`（typed decision，`rehydrationRequired=true`），未来 host 必须把当前 stack unwind
  记为 `REHYDRATE`，不是 STOPPED/FAILED。
- `STOPPED`：抛现有 `TaskStopRequestedException`；future host 映射 STOPPED。
- `COMPLETED`：抛 typed transition exception，host 映射 COMPLETED，不执行 cleanup/retry。
- unconfirmed/missing/mismatch/stale：抛 typed fail-closed transition exception，host 映射 `DENIED`，不当业务 FAILED，
  不自动 retry/rehydrate。
- `PAUSED` 本身不阻塞 Cloud thread；只有 explicit lifecycle/confirm 事件可再次调用 rehydration owner。无 busy poll、
  无 monitor wait、无 TTL。

### 原子 classifier 状态机

classifier 必须在 coordinator 同一 monitor 内直接比较结构化 binding/confirmation，不解析 `reason` 文本：

```text
lookup taskRunId
  absent -> MISSING_BINDING
  scope/session mismatch -> SCOPE_OR_SESSION_MISMATCH (no binding disclosure)
  window tuple mismatch -> WINDOW_MISMATCH
  taskType mismatch -> TASK_TYPE_MISMATCH
  status STOPPED -> STOPPED
  status COMPLETED -> COMPLETED
  status PREPARED -> PREPARED_NOT_EXECUTABLE
  stopEpoch mismatch -> STOP_EPOCH_MISMATCH
  status PAUSED and currentRevision > expected -> PAUSED_NEWER_REVISION
  status ACTIVE and currentRevision == expected and !confirmed -> ACTIVE_CURRENT_REVISION_UNCONFIRMED
  status ACTIVE and currentRevision > expected and !confirmed -> ACTIVE_NEWER_REVISION_UNCONFIRMED
  status ACTIVE and currentRevision > expected and confirmed but resume reconcile absent
      -> RESUME_RECONCILE_UNCONFIRMED
  status ACTIVE and currentRevision > expected and both confirmed
      -> RESUMED_ACTIVE_NEWER_REVISION_CONFIRMED
  status ACTIVE and currentRevision == expected and confirmed -> CURRENT_ACTIVE_CONFIRMED
  otherwise -> STALE_OR_FUTURE_REVISION
```

- coordinator 要记录 revision transition cause（`ACTIVATE/PAUSE/RESUME/STOP/COMPLETE`），不能仅以“ACTIVE+revision
  newer”猜 resume。旧 context/handle 永不变 current；classifier 只告诉 host 应 unwind/rehydrate。
- ACTIVE resumed revision 在 local exact window confirm + reconcile fact 原子写入前绝不创建新 context/handle。

### Sleep safe-point

```java
sleepOrStop(context, millis, message):
    if millis <= 0: context.throwIfStopRequested(); return
    context.throwIfStopRequested()
    durableCatalog.beginSleep(run, phaseRevision, sleepSlot, totalMillis,
                              coordinator.pauseProgressBaseline())
    try one interruptible sleep of remaining business millis
    catch InterruptedException:
        preserve interrupt flag
        classify exact context
        throw STOPPED / REHYDRATE / DENIED typed exception; never return false-and-continue
    context.throwIfStopRequested()
    durableCatalog.completeSleep(... CAS same phase/sleep slot ...)
```

- pause occurring during sleep immediately makes all mechanical request gates stale; no input/capture can pass old revision。
  sleep 本身不轮询；自然醒来后的 checkpoint 发现 PAUSED/new revision 并 unwind。
- resume 不能继续旧 Java stack。new confirmed revision 从 catalog 的 same phase/sleep safe-point 重建；选项 P1-3A
  只睡 durable remainder。pause progress 用 coordinator monotonic accounting 补偿 business watchdog，不把 pause wall time
  算进现有 180s 等预算。
- plain `sleep(ms)` 不属于 Cloud business API。机械 local input worker 的 sleep 继续留 DHXY。

### Persisted phase/action catalog schema

建议一个 package-private `CloudTaskRunStateCatalog`，backend 必须具备 fsync/事务/WAL 语义，不接受业务层自由写：

```text
CatalogKey
  tenantId, userId, deviceId, clientSessionId, taskRunId

RunStateRecord (schemaVersion, catalogRevision CAS)
  effectiveTaskCode, requestedTaskCode, task metadata digest
  windowBindingDigest (not HWND object/raw title/geometry)
  coordinatorRunRevision, stopEpoch, lifecycleStatus, transitionCause
  phaseCode, phaseOccurrence, phaseStatePayload (allowlisted immutable DTO only)
  retryCounters, recoveryCounters, fallbackCounters, loopGuardCounters
  consumedBusinessNanos / pauseProgressBaselineNanos / watchdog budget fields
  resumeMode = NONE | CONTINUE_ORIGINAL_PHASE | TASK_HOT_START
  approvedHotStartPolicyId (not a locally chosen next phase)
  immutable caller metadata required to rebuild CloudTaskServiceMetadata
  sleepContinuation? {phaseRevision, sleepSlot, totalMillis, consumedMillis,
                      pauseProgressBaselineNanos, state=STARTED|COMPLETED}
  actions[]

ActionAttemptRecord
  ActionAddress {phaseCode, actionSlot, occurrence}
  operation, attempt
  requestId, actionId, captureId? (stable retained IDs)
  requestDigest, canonicalRequestBytes/artifactRef
  dispatchState = DECLARED | REQUEST_BOUND | DISPATCH_REGISTERED
  executionState = UNRECORDED | UNKNOWN | NOT_EXECUTED | EXECUTED | OBSERVED | STOPPED
  outcomeCode, outcomeDigest, bounded typed outcome payload/artifactRef
  supersededByAttempt? (only verified NOT_EXECUTED renewal)
  phaseCommitRef?

LocalReconcileRecord
  exact runRevision, windowBindingDigest, catalogPhaseRevision
  decision = CONTINUE_ORIGINAL_PHASE | FALLBACK_TASK_HOT_START
  mismatchCode (enum), pauseProgressNanos, localReconcileGeneration
```

明确禁止持久化：`WindowRuntimeContext`、HWND/JNA object、mutable `BufferedImage`、Thread/interrupt/monitor state、
bearer capability、raw client request object、DHXY local path、opaque handle Java object。大 capture 结果只存 tenant-scoped
artifact ref + digest；catalog 不内嵌无界图像/base64。

### Stable ID / UNKNOWN / final / crash ordering

1. **Declare before expose**：ActionAddress + operation + stable IDs 先 durable commit，再构造 opaque handle。未 commit
   的 UUID 不得返回 caller。
2. **Bind before dispatch**：exact canonical request bytes/digest 先 durable bind。相同 attempt 永远只能读回同字节；
   changed payload/revision 拒绝。
3. **Register + dispatch marker atomic**：catalog `DISPATCH_REGISTERED` 与 broker durable registration 必须同 journal
   transaction/sequence；不能一个成功另一个丢失。
4. **Outcome before phase**：verified typed outcome durable commit 后，phase transition CAS 才能引用其 attempt/digest。
   phase advance 与 final outcome 最好同 transaction；否则 recovery 只允许 final->phase 补提交，禁止 phase->无 final。
5. **UNKNOWN**：`UNKNOWN` 仅可被 exact same request late final 替换一次；未 final 前不 advance phase、不 renew、
   不 new-ID retry。相同 revision 且 broker 仍允许时只重投 persisted identical bytes；resume 后 old revision 已 stale，
   因而 rehydration 返回 `BLOCKED_UNRESOLVED_ACTION`，等待 late final 或 explicit STOP，不 redispatch。
6. **Final immutable**：`EXECUTED/OBSERVED/STOPPED/NOT_EXECUTED` + digest 一旦 final，不可改。已 final action 不创建
   新 handle执行；rehydrator 只恢复业务结果/phase commit。
7. **NOT_EXECUTED renewal**：唯一允许的新 attempt。保留同一 ActionAddress，按现有规则 WINDOW_FACT/CAPTURE 可保留
   actionId、INPUT_BUNDLE 新 actionId；新 requestId/attempt 明确 supersede 旧 attempt。不是自动 retry，必须由原业务
   phase 的既有 retry/fallback 规则显式请求。
8. **Crash matrix**：
   - durable DECLARED、未 bind：同 IDs 恢复声明；在新 revision build 前必须由 journal 证明未 dispatch。
   - REQUEST_BOUND、未 registered：同 revision 可用同 bytes；新 revision 必须先得到 durable verified
     NOT_EXECUTED，再按规则 renewal。
   - DISPATCH_REGISTERED、无 outcome：恢复为 UNKNOWN，绝不猜 NOT_EXECUTED。
   - final、phase 未 commit：按 persisted final + predeclared transition intent 做一次 CAS phase commit。
   - phase commit 存在但 final 不存在：journal invariant violation，run fail-closed，不继续。

### Rehydration owner / pseudocode

唯一 owner：`com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskRunRehydrationOwner`，package-private，由
`CloudTaskRunAuthorityAssembly` 构造并持有；host/business code 不可 new、不可传自由 phase/action key。

```text
rehydrate(scope, taskRunId, expectedOldContextIdentity):
  checkpoint = coordinator.classifyCheckpoint(expectedOldContextIdentity)
  if PAUSED -> WAIT_PAUSED
  if STOPPED/COMPLETED -> terminal typed outcome
  if unconfirmed/mismatch/missing -> DENIED (no context)
  require RESUMED_ACTIVE_NEWER_REVISION_CONFIRMED
  load catalog by full CatalogKey under tenant lock/transaction
  verify taskType/windowDigest/stopEpoch/currentRevision/catalogRevision
  require exact LocalReconcileRecord for current revision
  if reconcile=FALLBACK:
      set resumeMode TASK_HOT_START only; future task executes approved hot-start Policy
      do not let local mismatchCode select a phase
  inspect every current ActionAttempt:
      UNKNOWN or dispatch-without-final -> BLOCKED_UNRESOLVED_ACTION
      final -> restore immutable result, never executable old handle
      DECLARED/proven NOT_EXECUTED -> reconstruct from same address/IDs or explicit renewal rule
  executionGate.createContext(current confirmed ACTIVE binding)
  rebuild CloudTaskServiceExecutionContext with SAME per-run retained state/catalog owner
  create NEW opaque handle objects tied to NEW exact context only where action state permits
  CAS catalog rehydratedRevision=currentRevision
  return READY(new TaskExecutionContext, typed handles, phase snapshot, pause compensation)
```

- old `TaskExecutionContext`、`CloudTaskRunExecutionContext`、`CloudTaskServicePort.ActionHandle` 永久 stale；never mutate/
  revive。`CloudTaskRunAuthorityAssembly.createTaskServiceRuntime` 当前每次 new retained state，必须改为由 per-run
  rehydration owner 复用 catalog-backed owner，不能创建第二 authority island。

### Local reconcile fact handoff

- DHXY `TaskPauseResumeReconciler` 原地保留并继续负责：比较本地 prepared dialog/visible dialog/pathing/action state，
  补偿/清理 local volatile timers/caches，返回现有 `CONTINUE_ORIGINAL_PHASE` 或 `FALLBACK_TASK_HOT_START`。
- wire fact 只含 bounded enum/digest/counters，不传 prepared action 坐标、dialog screenshot、path、cache object 或 HWND。
- coordinator 验证 authenticated full scope、exact taskRunId/current ACTIVE revision/exact window digest/catalog phase revision；
  stale/replayed fact 拒绝。fact 与 execution confirmation 原子提交。
- `CONTINUE_ORIGINAL_PHASE` 允许 rehydrator使用 persisted phase；`FALLBACK_TASK_HOT_START` 只授权调用当前任务已批准的
  hot-start Policy。Cloud 仍按业务文档固定顺序
  `战斗中 > dialog > 归队 > tracker > 回程 > 已保存上下文 > 接任务` 运行，不把 mismatchCode/negative local fact
  直接当 phase truth。
- local reconcile failure/missing 只导致 `RESUME_RECONCILE_UNCONFIRMED`，不自动 cleanup/retry/stop/phase advance。

### Host lifecycle / cohort gate

- 本切片所有新类 compile-ready/dormant。不得修改 `CloudBrainServer` 去启动 Task host，不新增 worker/poller/thread。
- later activation owner 必须是 authenticated task-run lifecycle/host assembly，顺序固定：RESUME -> local reconcile ->
  atomic confirm fact -> explicit `rehydrate(...)` -> invoke Task。它不能绕过 authority assembly。
- activation 前每个 Service/Task class 必须满足：无 holder、无 local runtime/HWND getter、无 raw `TaskSleep.sleep`、
  每个 phase/action address 已进入 catalog schema、所有 path/artifact blocker 已关闭。
- 五倍/修罗需额外核对 `docs/业务逻辑.md` 对应 phase/retry/fallback/hot-start 行；本设计不授权迁移主体。

### Tenant / capacity / retention / observability

- CatalogKey 使用完整 tenant/user/device/clientSession/taskRun；所有读取/事务先校验 authenticated scope。tenant/user
  filesystem root 之外，record 内仍校验 device/session，禁止跨 device/session scan。
- hard caps（值由父级/运维配置决定，不在本设计猜默认）：global/owner retained runs、actions per run、attempts per
  action、catalog bytes per run、request bytes、typed outcome bytes、artifact bytes、journal bytes。容量满在 declare 前
  fail-closed，不能部分 mint ID。
- 不新增 TTL。terminal cleanup/compaction 必须是显式 later operational gate：仅 terminal run、无 UNKNOWN、final+phase
  invariant 完整、审计 checkpoint 已生成时可归档；UNKNOWN/nonterminal 不自动删除。若未批准 retention/compaction
  配置，host activation 保持关闭，避免无界 orphan state。
- structured logs/metrics：tenant scope hash、taskRunId hash、expected/current revision、status/outcome、catalog revision、
  phaseCode、ActionAddress、attempt、executionState、request/outcome digest prefix、reconcile decision、elapsed；不记录
  bearer、raw request bytes、image、local path、HWND/title。
- 运维需检测：journal recovery failure、phase-without-final invariant、UNKNOWN age（只观测不 TTL）、orphan count、
  capacity utilization、reconcile missing、stale context attempts、duplicate/superseded handle attempts。

### 精确拟改文件 / visibility / diff budget

父级解决三个 P1 后，建议一次只批准下面一个 cohesive authority slice；未选 backend 前不写 Java：

| 文件 | visibility | 预算 | 作用 |
|---|---|---:|---|
| 新 `com/bot/dhxy/runner/stop/TaskCheckpointOutcome.java` | public enum | <=80 | typed state，不含 authority |
| 新 `.../TaskCheckpointDecision.java` | public immutable record | <=100 | sanitized typed decision |
| 新 `.../TaskCheckpointTransitionException.java` | public final | <=60 | 非 STOP typed unwind |
| 新 `.../TaskCheckpoint.java` | public final utility | <=110 | 仅 explicit context overload |
| 新 `.../TaskSleep.java` | public final utility | <=130 | checkpoint + one sleep + durable safe-point port |
| 改 `com/bot/dhxy/runner/context/TaskExecutionContext.java` | public compatibility view | +80/-0 | stop/pause/checkpoint methods；无 token/holder |
| 改 `remote/CloudTaskServiceExecutionContext.java` | public view, ctor package-private | +45/-5 | typed checkpoint delegate；接受 existing retained owner |
| 改 `remote/CloudTaskRunExecutionGate.java` | package-private | +70/-0 | 调 coordinator typed classifier |
| 新 `remote/LocalPauseResumeReconcileFact.java` | public wire DTO | <=110 | exact revision-bound bounded fact |
| 新 `remote/CloudTaskRunStateCatalog.java` | package-private | <=300 | schema + transactional interface；无 public write |
| 新 backend（父级选型后定名） | package-private | backend-specific <=700 | single durable journal/WAL |
| 新 `remote/CloudTaskRunRehydrationOwner.java` | package-private final | <=350 | 唯一 rehydration owner/state machine |
| 改 `remote/CloudTaskRunAuthorityAssembly.java` | package-private | +100/-20 | 持有 catalog/owner；禁止每次 new retained island |
| 改 `remote/CloudTaskRetainedActionState.java` | package-private | +160/-20 | new-context handle reconstruction，old handle stale |
| 改 `remote/CloudTaskRunActionLedger.java` | package-private | +220/-20 | durable restore/import invariants；不开放 public mint |
| 改 `remote/run/RemoteTaskRunCoordinator.java` | public lifecycle owner | +180/-20 | typed classifier、transition cause、atomic reconcile confirm |
| 改 `RemoteTaskRunActionRequest.java`、`RemoteTaskRunEndpoint.java` | public wire / endpoint | 合计 +100/-15 | 父级选择 P1-2 后接 exact fact；不加 raw completion |

- `RemoteTaskRunRoutes`/`CloudBrainServer`/host activation 默认零改；若父级选择真正 durable backend，必须另行批准
  storage root/authority assembly 的构造注入文件，不能在实现时顺手接线。
- 不修改 DHXY Java；local fact 生产/发送适配必须另开批准写集并跑 DHXY compile。当前 slice 只设计 handoff schema。
- 所有当前目标均为 Cloud untracked/in-flight 文件，实施前必须重验 hash/status。当前关键 SHA-256：
  `TaskExecutionContext` `10A099FA...D12`，`CloudTaskServiceExecutionContext` `CC3F3EB0...7110`，
  `ExecutionGate` `3E1CF903...BD7B`，`AuthorityAssembly` `D1E6C86B...FEBB`，
  `ActionLedger` `BF2B3A33...896E`，`RetainedActionState` `56927DEE...B5A9`，
  `Coordinator` `C0336832...965D`；完整 hash 已在本轮只读终端证据中核对。

### Verification plan（批准后；本轮不执行）

```powershell
cd D:\mavenProject\dhxy-cloud-brain
mvn -q clean package

rg -n "ThreadLocal|TaskPauseToken|WindowRuntimeContext|TaskExecutionContextHolder|HWND|InputAction|capture|HttpClient" `
  src/main/java/com/bot/dhxy/runner/stop `
  src/main/java/com/yueyunfe/dhxy/cloudbrain/remote
rg -n "new HashMap|UUID.randomUUID|UNKNOWN|NOT_EXECUTED|DISPATCH_REGISTERED|phaseCommitRef" `
  src/main/java/com/yueyunfe/dhxy/cloudbrain/remote
rg -n "CloudTaskRunRehydrationOwner|CloudTaskRunAuthorityAssembly|CloudTaskRunStateCatalog" src/main/java
javap -classpath target/classes -p com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskRunRehydrationOwner
javap -classpath target/classes -public com.bot.dhxy.runner.stop.TaskCheckpoint
javap -classpath target/classes -public com.bot.dhxy.runner.stop.TaskSleep
git status --short -- src/test pom.xml src/main/resources
git -C D:\mavenProject\DHXY status --short -- src/main/java pom.xml src/main/resources
```

- 无新/恢复测试；fresh Cloud package 不带 skip，记录 Surefire/JAR。静态审查必须证明：typed classifier 不解析
  free-text、old context/handle 不 revive、UNKNOWN 无 redispatch/new ID、phase 无 final 不 advance、journal restore 不
  跨 tenant、local fact 不选业务 phase、无 holder/pause token/thread singleton、无 server/host/poller 启动。
- 若父级另批 DHXY fact producer Java，才运行 DHXY `mvn -q -DskipTests compile`；否则 DHXY Java 必须 unchanged。
- 本轮未运行 Maven/测试/应用/server/host/poller/UI/capture/OCR/input，未执行 Git mutation。Design #1 到此停止，
  等待父级 `DESIGN APPROVED` 或 `BLOCKED`。

## Local Design Review #1 - BLOCKED - 2026-07-12

### 结论

- `P0=0 / P1=3 / P2=0`，Design #1 的风险判断正确，但当前实施范围过大，**BLOCKED before Java**。
- 父级不允许在同一切片同时新增 WAL/journal、改 coordinator/broker/ledger/action identity、扩 wire confirm、改
  DHXY fact producer、实现 rehydration owner 和迁 checkpoint utility。那会把已批准 authority 的多个不变量一次性
  重写，且无本地测试模式下缺少足够分段证据。

### 三个 P1 的父级决策

1. **P1-1 选择 Option B 并收窄**：本切片只实现 **same-process typed checkpoint classifier + explicit-context
   compatibility utility**，明确不实现、不宣称 process-crash/durable rehydration。不得新增
   `CloudTaskRunStateCatalog`、journal/WAL/backend、rehydration owner，也不得修改 action ledger/broker/retained state。
   Task host/cohort 继续 dormant；真正 durable authority 另开后续独立切片。
2. **P1-2 方向选择 Option A，但延期到下一跨仓切片**：未来 RESUME 产生的新 ACTIVE revision 必须把 exact
   `LocalPauseResumeReconcileFact` 与 execution confirmation 原子提交；本切片不扩 wire、不修改 endpoint/DHXY，
   只把“ACTIVE newer revision 即使 confirmed 也必须 unwind，不能自动继续旧 stack”编码进 classifier。
3. **P1-3 选择 Option B/不在本切片恢复 mid-sleep**：无 durable backend 时不得设计 `SleepContinuation` 或声称
   crash 后可补睡 remainder。当前只保留同进程一次 sleep 的前后 checkpoint；进程 crash recovery 未支持且 host
   activation 保持关闭。以后 durable 切片再决定 sleep continuation，不能现在无证据地重睡或跳过。

### Design Repair #1 精确范围

1. 修订标题为 `External Worker - Design Repair #1 - 2026-07-12`，目标改名为
   **Cloud same-process checkpoint compatibility**；所有 durable catalog/WAL/rehydration/action restore/wire fact 仅列后续
   gate，不得进入拟改文件。
2. coordinator 在同一 monitor 内提供 typed classifier，必须结构化区分：CURRENT_ACTIVE_CONFIRMED、PAUSED、
   ACTIVE_NEWER_REVISION（confirmed/unconfirmed 均旧 stack unwind）、ACTIVE_CURRENT_UNCONFIRMED、STOPPED、COMPLETED、
   PREPARED、identity/window/stopEpoch/taskType mismatch、missing binding、future revision。不得由 business utility 解析
   free-text `reason`。
3. 明确异常映射：CURRENT 不抛；STOPPED 仍抛 `TaskStopRequestedException`；PAUSED/ACTIVE_NEWER/COMPLETED 抛
   typed transition exception（分别标记 REHYDRATION_REQUIRED/COMPLETED）；unconfirmed/mismatch/missing/future/
   unexpected thread interruption 抛 DENIED transition。任何异常都不是业务 FAILED，不自动 retry/phase advance。
4. Cloud `TaskExecutionContext` 只增加与本地调用形态兼容的 `throwIfStopRequested()`、`isStopRequested()`、
   `isPauseRequested()`；返回 blocked ms 固定 0，因为本切片不阻塞 Cloud pause。不得增加 holder/token/runtime 假 getter。
5. Cloud `TaskCheckpoint` 只保留 explicit `TaskExecutionContext` overload；不得复制 holder、stopToken、
   `throwIfInterrupted` overload。Cloud `TaskSleep` 只保留 `sleepOrStop(context,...)`（以及必要的 private 单次 sleep），
   不公开 context-free `sleep(ms)`。pause during sleep 在后 checkpoint unwind；机械 port 的 revision gate 已阻止旧动作。
6. `InterruptedException` 必须恢复 interrupt flag后再做 exact typed classify；若 context 仍 CURRENT，不得把 JVM interrupt
   冒充 STOPPED，必须抛 typed DENIED/INTERRUPTED unwind。不得 busy poll、切片 sleep、自动 resume/retry、TTL。
7. 拟改范围应小且 Cloud-only：powerless outcome/decision/transition types、Cloud `TaskCheckpoint`/`TaskSleep`、
   `TaskExecutionContext`/`CloudTaskServiceExecutionContext`/execution gate/coordinator 的最小 classifier 接线。
   `AuthorityAssembly`、ledger、retained state、broker、routes/endpoint/host/server、DHXY Java、pom/resources/tests 零改。
8. 给出精确文件/visibility/diff budget、classifier pseudocode、API compatibility/cohort gate、构建/静态验证；
   `P0/P1/P2` 必须归零后才可批准实现。

`无已批准业务差异；按基线等价迁移。`








## External Worker - Design Repair #1 - 2026-07-12

### 修订结论

- 目标正式收窄为 **Cloud same-process checkpoint compatibility**。本切片只提供 coordinator-backed typed
  classifier、explicit-context `TaskCheckpoint`/`TaskSleep` 和 Cloud `TaskExecutionContext` 三个兼容方法。
- 父级已选定并后置：durable catalog/WAL/backend、process-crash rehydration、rehydration owner、action restore、
  local reconcile wire fact、DHXY producer、mid-sleep crash continuation。它们全部不进入本次拟改文件，也不在本切片
  声称已支持。
- 本版 `P0=0 / P1=0 / P2=0`，无剩余设计 blocker，等待父级复审。host/Task/Service cohort 继续 dormant；
  `无已批准业务差异；按基线等价迁移。`

### 最小不变量

1. coordinator 在现有同一 monitor 内按结构化 binding/confirmation 字段分类；business utility 不读取或解析
   `RemoteTaskRunAuthorization.reason()`。
2. old context 在任何 revision 变化后只可得到 typed unwind，永不继续旧 Java stack，永不创建 replacement context/
   handle。
3. `CURRENT_ACTIVE_CONFIRMED` 是唯一 continue 结果；所有其它 outcome 均 stop 或 typed unwind。
4. Cloud 不复制 `TaskExecutionContextHolder`、`TaskPauseToken`、`TaskStopToken` authority、`WindowRuntimeContext`、
   thread-local/current-run singleton 或 local reconciler。
5. 无等待 PAUSED、无 busy polling、无 sliced sleep、无自动 retry/resume、无 TTL、无 phase advance。
6. `TaskSleep.sleepOrStop` 只做一次同步 sleep；pause during sleep 由机械 port revision gate 立即阻止旧动作，并在
   sleep 后 checkpoint unwind。
7. JVM interrupt 永远不是 coordinator STOPPED 的替代事实。恢复 interrupt flag 后必须重新 typed classify；若仍
   CURRENT，则明确 DENIED/INTERRUPTED unwind。

### Typed outcome / decision

新增 public powerless enum：

```text
TaskCheckpointOutcome
  CURRENT_ACTIVE_CONFIRMED
  PAUSED
  ACTIVE_NEWER_REVISION_CONFIRMED
  ACTIVE_NEWER_REVISION_UNCONFIRMED
  ACTIVE_CURRENT_REVISION_UNCONFIRMED
  STOPPED
  COMPLETED
  PREPARED
  MISSING_CONTEXT
  MISSING_BINDING
  IDENTITY_OR_SESSION_MISMATCH
  WINDOW_MISMATCH
  STOP_EPOCH_MISMATCH
  TASK_TYPE_MISMATCH
  FUTURE_REVISION
  INTERRUPTED_WHILE_CURRENT
```

`TaskCheckpointDecision` 是 public immutable record，仅含：

```text
outcome
expectedRunRevision
currentRunRevision        // mismatch/hidden binding 时固定 -1，不泄漏别的 tenant state
currentStatus             // 可空；identity/missing 时空
executionConfirmed
disposition = CONTINUE | REHYDRATION_REQUIRED | STOPPED | COMPLETED | DENIED
```

- `disposition` 由 record constructor/switch 从 outcome 唯一派生，caller 不能自由指定。
- `PAUSED`、两种 `ACTIVE_NEWER_REVISION_*` 都是 `REHYDRATION_REQUIRED`。即使 newer ACTIVE 已 confirmed，本切片也
  必须 unwind，因为 local reconcile fact/rehydration owner 尚未实现。
- `STOPPED` 单独映射 STOPPED；`COMPLETED` 单独映射 COMPLETED；missing/unconfirmed/mismatch/future/interrupted
  全部 DENIED。

### Coordinator classifier 状态机

拟在 `RemoteTaskRunCoordinator` 增加一个 read-only synchronized `classifyCheckpoint(...)`。输入为 old context 已有的
exact scope、taskRunId、taskType、window tuple、stopEpoch、expected revision；输出只为 powerless decision。

```text
synchronized classify(expected):
  if expected scope/context missing -> MISSING_CONTEXT
  binding = bindingsByTaskRunId[taskRunId]
  if binding absent -> MISSING_BINDING
  if binding.scope != expected.scope -> IDENTITY_OR_SESSION_MISMATCH
  if binding.taskRunId/taskType mismatch -> IDENTITY_OR_SESSION_MISMATCH / TASK_TYPE_MISMATCH
  if binding.window != expected.window -> WINDOW_MISMATCH

  // Terminal status wins over stopEpoch so real STOP is not mislabeled as a mismatch.
  if status == STOPPED -> STOPPED
  if status == COMPLETED -> COMPLETED
  if status == PREPARED -> PREPARED

  if binding.stopEpoch != expected.stopEpoch -> STOP_EPOCH_MISMATCH
  if expected.revision > binding.revision -> FUTURE_REVISION
  confirmed = confirmedExecutionRevisionByTaskRunId[runId] == binding.revision

  if status == PAUSED -> PAUSED
  if status == ACTIVE && binding.revision > expected.revision:
      return confirmed
          ? ACTIVE_NEWER_REVISION_CONFIRMED
          : ACTIVE_NEWER_REVISION_UNCONFIRMED
  if status == ACTIVE && binding.revision == expected.revision:
      return confirmed
          ? CURRENT_ACTIVE_CONFIRMED
          : ACTIVE_CURRENT_REVISION_UNCONFIRMED
  return DENIED-compatible typed mismatch (never free-text inference)
```

- `PAUSED` 不要求 business utility 推断 revision cause；coordinator status 是权威。old context 原本只可能从 confirmed
  ACTIVE mint，因此 PAUSED 必然要求 unwind。
- ACTIVE newer 无论 confirmation 都要求 unwind。confirmed 只影响 diagnostics/outcome，不授权旧 stack 继续。
- scope/session mismatch 不返回 current revision/status/window；仅同 exact owner scope 时才填 current fields。
- classifier 不写 coordinator map，不 confirm execution，不改变 pause accounting，不 mint identity。

### 异常映射

`TaskExecutionContext.throwIfStopRequested()` 使用 decision.disposition 精确映射：

| Decision | 行为 | future host 语义 |
|---|---|---|
| `CURRENT_ACTIVE_CONFIRMED` | 返回固定 `0L` | 继续当前同步调用 |
| `STOPPED` | 抛现有 `TaskStopRequestedException("task run stopped")` | STOPPED；不是 FAILED |
| `PAUSED` | 抛 `TaskCheckpointTransitionException(decision)` | REHYDRATION_REQUIRED；本切片无 owner，不自动恢复 |
| `ACTIVE_NEWER_REVISION_CONFIRMED` | 同上 | REHYDRATION_REQUIRED；不能继续 old stack |
| `ACTIVE_NEWER_REVISION_UNCONFIRMED` | 同上 | REHYDRATION_REQUIRED，但 later activation 还须等待 confirm/fact |
| `COMPLETED` | 抛 typed transition exception | COMPLETED；不 cleanup/retry/phase advance |
| `ACTIVE_CURRENT_REVISION_UNCONFIRMED` | 抛 typed transition exception | DENIED |
| PREPARED/missing/mismatch/future | 抛 typed transition exception | DENIED，fail-closed |
| `INTERRUPTED_WHILE_CURRENT` | 抛 typed transition exception | DENIED/INTERRUPTED；绝不冒充 STOPPED |

- `TaskCheckpointTransitionException` 是 public final runtime exception，只携 powerless decision；不带 replacement
  context/handle/authority。它不是 `TaskStopRequestedException` 子类，避免 PAUSED/COMPLETED 被现有 task catch 误记为
  STOPPED。
- 未来 host 必须在 generic business exception/FAILED 之前捕获 transition exception，并按 disposition 处理；host
  未实现前 cohort 不激活。

### Explicit-context API

#### Cloud `TaskExecutionContext`

只新增以下三个 public 方法，不增加 holder/token/runtime/window-geometry 假 getter：

```java
public long throwIfStopRequested(); // CURRENT -> 0; otherwise typed throw
public boolean isStopRequested();   // exact classifier outcome == STOPPED
public boolean isPauseRequested();  // exact classifier outcome == PAUSED
```

- boolean probe 不改变 state、不等待；除精确 STOPPED/PAUSED 外均 false。真正动作仍在 port gate 再次 revision validate。
- blocked-ms 固定 `0L`。本切片不阻塞 Cloud pause，也不伪造 pause duration；消费 blocked-ms 做 watchdog compensation 的
  五倍/修罗 caller 在 durable/reconcile 后续切片前保持 cohort blocked。

#### Cloud `TaskCheckpoint`

public API 只有：

```java
public static long throwIfStopRequested(TaskExecutionContext context, String message);
```

- null context -> typed `MISSING_CONTEXT/DENIED`，不作为 legacy no-op。
- 方法只委托 explicit context；`message` 仅作安全诊断，不用于解析 lifecycle。
- 明确不提供 holder overload、context+holder overload、stopToken overload、`throwIfInterrupted`。

#### Cloud `TaskSleep`

public API 只有：

```java
public static void sleepOrStop(TaskExecutionContext context, long millis, String interruptedMessage);
```

- 不公开 context-free `sleep(long)`；必要的单次 sleep helper 为 private。
- 为保持本地调用语义，`millis <= 0` 直接 return；正数路径为 checkpoint -> 一次 `Thread.sleep(millis)` -> checkpoint。
- 无 `SleepContinuation`、无 crash remainder、无 pause-duration return。

### Sleep interrupt 精确语义

```text
sleepOrStop(context, millis, message):
  if millis <= 0: return
  TaskCheckpoint.throwIfStopRequested(context, message)
  try Thread.sleep(millis) exactly once
  catch InterruptedException:
      Thread.currentThread().interrupt()
      try TaskCheckpoint.throwIfStopRequested(context, message)
          // only possible return means exact CURRENT_ACTIVE_CONFIRMED
          throw transition(INTERRUPTED_WHILE_CURRENT, DENIED)
      catch TaskStopRequestedException/TaskCheckpointTransitionException:
          throw unchanged
  TaskCheckpoint.throwIfStopRequested(context, message)
```

- interrupt 后 classify 若 STOPPED，抛 stop；若 pause/newer/completed/denied，保留 classifier typed transition；若仍
  CURRENT，构造 `INTERRUPTED_WHILE_CURRENT` typed DENIED。全路径 interrupt flag 已恢复。
- 不切片 sleep、不轮询 coordinator、不自动重睡、不自动 rehydrate/retry。
- pause during sleep 不中断 sleep 也不允许运行旧动作：并发机械请求的 gate 已因 revision 改变 fail-closed；sleep
  自然结束后后置 checkpoint unwind。

### Compatibility / cohort gate

| caller 形态 | 编译 | same-process 行为 | cohort gate |
|---|---|---|---|
| explicit `context.throwIfStopRequested()` | 可 | CURRENT continue；其它 typed unwind | 可作为后续候选，但 host transition catch 未实现前不激活 |
| `TaskCheckpoint(context,msg)` | 可 | 同上 | 同上 |
| `TaskSleep.sleepOrStop(context,...)` 且 context 非 null | 可 | 一次 sleep + 前后 classifier | 同上 |
| `sleepOrStop(null,...)` | 可但运行 DENIED | 不再 legacy no-op | caller 改为显式 context 前 blocked |
| holder/context+holder checkpoint | 不可 | 不提供 Cloud API | caller 去 holder/显式传 context 前 blocked |
| stopToken/throwIfInterrupted | 不可 | 不提供 Cloud API | caller 改 typed context 前 blocked |
| raw `TaskSleep.sleep` | 不可 | 不提供 Cloud API | business class 改 explicit sleepOrStop 前 blocked |
| 消费 pause blocked-ms / local reconcile | 仅调用签名可编译 | pause 会 unwind，不能在旧 stack 得到 blocked ms | 五倍/修罗这些 phase 点等后续 durable+fact+owner slice |
| local runtime/HWND/geometry getter | 不可 | 不补假能力 | 保持 local 或 typed port adapter 完成后再迁 |

- 本切片不迁任何 Task/Service，不声称 pause 后可继续业务 phase。它只让未来 host 能明确区分为什么 old invocation
  必须 unwind。
- future activation 最低门：host typed transition catch、local reconcile fact 原子 confirm、new context/handle owner、
  approved phase state source全部完成。durable crash gate另行处理。

### 精确 Cloud-only 文件 / visibility / diff budget

| 文件 | visibility | diff budget | 精确内容 |
|---|---|---:|---|
| 新 `com/bot/dhxy/runner/stop/TaskCheckpointOutcome.java` | public enum | <=70 lines | 上述 16 个 powerless outcome |
| 新 `.../TaskCheckpointDecision.java` | public record | <=120 lines | decision + nested public `Disposition`；派生 disposition/factory |
| 新 `.../TaskCheckpointTransitionException.java` | public final | <=45 lines | immutable decision carrier；非 stop subtype |
| 新 `.../TaskCheckpoint.java` | public final utility | <=65 lines | 唯一 explicit-context overload |
| 新 `.../TaskSleep.java` | public final utility | <=95 lines | 唯一 public sleepOrStop + private one-shot sleep |
| 改 `com/bot/dhxy/runner/context/TaskExecutionContext.java` | public compatibility view | +55/-0 | 三个 public compatibility methods + private decision mapping |
| 改 `remote/CloudTaskServiceExecutionContext.java` | public view；ctor package-private | +20/-0 | package-owned typed checkpoint delegate |
| 改 `remote/CloudTaskRunExecutionGate.java` | package-private | +20/-0 | read-only classifier forwarder |
| 改 `remote/run/RemoteTaskRunCoordinator.java` | public lifecycle owner | +120/-0 | one synchronized structured classifier；零 mutation |

总计：5 个新文件、4 个定点修改，预计 <=610 added lines、0 behavioral deletion。实现前重验所有 untracked/in-flight
hash/status，只在当前内容上 patch。

### 明确零改范围

以下全部 **zero diff**：

- `CloudTaskRunAuthorityAssembly`
- `CloudTaskRunActionLedger`
- `CloudTaskRetainedActionState`
- `RemoteGameCommandBroker`
- `RemoteTaskRunRoutes`
- `RemoteTaskRunEndpoint` / `RemoteTaskRunActionRequest` / wire action enum
- `CloudServiceHost` / configuration / storage
- `CloudBrainServer` / application
- 所有 DHXY Java/Maven/resources
- Cloud `pom.xml` / resources / `src/test`

不新增 catalog/WAL/backend/rehydration owner/action restore/reconcile fact/SleepContinuation，不改 public raw poll/outcome/
completion，不启动任何 runtime。

### 验证计划（批准实现后；本轮不执行）

```powershell
cd D:\mavenProject\dhxy-cloud-brain
mvn -q clean package

rg -n "reason\(\)|getReason|contains\(|startsWith\(" `
  src/main/java/com/bot/dhxy/runner/stop `
  src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/run/RemoteTaskRunCoordinator.java
rg -n "ThreadLocal|TaskExecutionContextHolder|TaskPauseToken|WindowRuntimeContext|TaskStopToken" `
  src/main/java/com/bot/dhxy/runner/stop/TaskCheckpoint.java `
  src/main/java/com/bot/dhxy/runner/stop/TaskSleep.java
rg -n "CloudTaskRunStateCatalog|Journal|WAL|RehydrationOwner|LocalPauseResumeReconcileFact|SleepContinuation" src/main/java
rg -n "class TaskCheckpoint|class TaskSleep|class TaskCheckpointTransitionException|classifyCheckpoint" src/main/java

javap -classpath target/classes -public com.bot.dhxy.runner.stop.TaskCheckpointOutcome
javap -classpath target/classes -public com.bot.dhxy.runner.stop.TaskCheckpointDecision
javap -classpath target/classes -public com.bot.dhxy.runner.stop.TaskCheckpointTransitionException
javap -classpath target/classes -public com.bot.dhxy.runner.stop.TaskCheckpoint
javap -classpath target/classes -public com.bot.dhxy.runner.stop.TaskSleep

git status --short -- src/test pom.xml src/main/resources
git -C D:\mavenProject\DHXY status --short -- src/main/java pom.xml src/main/resources
```

- fresh Cloud `mvn -q clean package` 必须无 skip、exit 0，记录现有 Surefire/JAR；不新增/恢复测试。
- source/javap 必须证明：utilities 无 holder/token/runtime overload；transition exception 不是 stop subtype；coordinator
  classifier 是 synchronized read-only；business utility 无 free-text reason parsing；public API 无 replacement context/handle。
- scoped diff 必须只有上述 9 个 Cloud 文件；zero-diff 列表逐项复核。DHXY 不编译，因为零改。
- 不运行 application/server/host/poller/UI/capture/OCR/input，不做 Git mutation。

### 八条返修条件对照

1. **目标收窄**：满足；仅 same-process typed checkpoint compatibility。
2. **结构化 classifier**：满足；CURRENT/PAUSED/newer confirmed/unconfirmed/current unconfirmed/terminal/mismatch/missing/
   future 全覆盖，无 free-text inference。
3. **异常映射**：满足；CURRENT continue、STOPPED stop、pause/newer/completed typed、其余 DENIED；不当业务 FAILED。
4. **Context 三方法**：满足；blocked ms 固定 0，无 holder/token/runtime 假 getter。
5. **Explicit utilities**：满足；Checkpoint 一个 overload，Sleep 一个 public method，无 raw sleep。
6. **Interrupt**：满足；恢复 flag 后 exact classify，CURRENT -> INTERRUPTED DENIED；无 poll/retry/TTL。
7. **Cloud-only small scope**：满足；9 文件；authority/ledger/retained/broker/routes/endpoint/host/server/DHXY/tests 零改。
8. **文件/预算/cohort/验证**：满足；本版 `P0/P1/P2=0`。

- 本轮未运行 Maven/测试/应用/server/host/poller/UI/capture/OCR/input，未执行 Git mutation。Design Repair #1
  到此停止，等待父级 `DESIGN APPROVED` 或新的 `BLOCKED`。

## Local Design Review #2 - DESIGN APPROVED - 2026-07-12

- 结论：经下列父级安全修订后，`P0=0 / P1=0 / P2=0`，Design Repair #1 **DESIGN APPROVED**。
- 认可范围：same-process typed classifier、explicit-context `TaskCheckpoint`/`TaskSleep` 与 Cloud context 兼容；
  不实现/宣称 durable/process-crash rehydration，不改 authority assembly/ledger/retained/broker/wire/endpoint/host/DHXY。

### 父级强制修订（实现必须逐字遵守）

1. Cloud `TaskExecutionContext` 本切片只新增 `throwIfStopRequested()` 与 `isStopRequested()`，**不新增
   `isPauseRequested()`**。当前业务候选没有 Cloud-safe pause boolean consumer；暴露该方法会让 PAUSED/newer revision
   的 old stack 有机会只读 boolean 后继续。
2. `isStopRequested()` 不是普通宽松 probe：`CURRENT_ACTIVE_CONFIRMED -> false`，`STOPPED -> true`；其它所有 outcome
   （PAUSED、ACTIVE newer、COMPLETED、unconfirmed、mismatch、missing、future）必须抛相同 typed transition exception。
   因而任何 old/stale/denied stack 都不能通过 boolean 检查继续纯业务 phase mutation。
3. `TaskCheckpointDecision.Disposition` 增加/保留精确 `PAUSED`：PAUSED outcome 映射 PAUSED，而不是
   `REHYDRATION_REQUIRED`；仍暂停时 future host 只能 park/unwind，不能尝试重建。仅
   `ACTIVE_NEWER_REVISION_CONFIRMED` 可标 `REHYDRATION_REQUIRED`；
   `ACTIVE_NEWER_REVISION_UNCONFIRMED` 映射 `DENIED`（未来可改名 pending-confirm，但不得视为 ready）。
4. 所有 disposition 在本切片都只用于 unwind 诊断；host transition catch/park/rehydration 尚未实现，所有 Task/Service
   cohort 继续 dormant。不得因 classifier 完成而启动业务。

### 放行写集

- 允许同一 worker 仅实施 Design Repair #1 的 5 个新文件 + 4 个定点修改，并应用上述三项 API/mapping 修订。
- `TaskCheckpoint` 只有 explicit context overload；`TaskSleep` 只有 public `sleepOrStop(context,...)`；interrupt 恢复
  flag 后 exact classify，CURRENT 时构造 `INTERRUPTED_WHILE_CURRENT/DENIED`。
- 明确 zero-diff：authority assembly、action ledger、retained state、broker、routes/endpoint/wire、host/server、
  DHXY Java、pom/resources/tests。不得新增 catalog/journal/rehydration/fact/sleep continuation。
- 实现后 fresh Cloud `mvn -q clean package`（无 skip）并追加源码 diff、classifier 结构化证据、public API/javap、
  zero-diff/无测试/无运行副作用证据；worker 自审不算父级 Approved。

`无已批准业务差异；按基线等价迁移。`

## External Worker - Implementation #1 - 2026-07-12

### 实施结论与边界

- 已严格实施父级 `Local Design Review #2 - DESIGN APPROVED` 放行的 same-process typed checkpoint
  classifier、explicit-context `TaskCheckpoint`/`TaskSleep` 和 Cloud context 兼容入口。
- 本 worker 自审：`P0=0 / P1=0 / P2=0`，当前无源码或构建 blocker；**自审不构成 Approved**，本切片保持
  dormant，等待父级复审。
- 未实现或宣称 durable/process-crash rehydration、catalog/WAL/journal、rehydration owner、action restore、local
  reconcile fact、DHXY producer、mid-sleep crash continuation。`无已批准业务差异；按基线等价迁移。`

### Scoped 基线与精确 diff

- 实施前 Cloud：branch `navigation-migration`，HEAD `3b988caa010254973e03342272e6d1d6a9685b01`；仓内已有
  `pom.xml`、server、host、remote、image/navigation 等大量并行 dirty/untracked 内容，全部保留。
- 实施前 DHXY：branch `thin-client-design`，HEAD `0114604e1ff5f15491d2910959c45252e893d04f`；已有 Java、配置、
  文档和 cloud remote 等并行 dirty/untracked 内容，全部保留。
- 5 个新 Cloud 文件（实施前均不存在）：
  - `com/bot/dhxy/runner/stop/TaskCheckpointOutcome.java`：21 行，16 个 structured outcome；SHA-256
    `7D4846EFD924D2D8037380ED33888BCBC773D2ABC9602ECB4EAAF1689FDF1A2E`。
  - `.../TaskCheckpointDecision.java`：76 行，5-fact immutable record + 纯派生 `disposition()`；SHA-256
    `9C65C10977C5CFCFFCC4CB7CB6D7D705FCAF0F75F2428819A8DB9C20FFEB9E93`。
  - `.../TaskCheckpointTransitionException.java`：31 行，final typed unwind，非 stop subtype；SHA-256
    `1D88DD417A73E1CCBE84078C941F2FD34177B31EC5A0AA65246422DE84571248`。
  - `.../TaskCheckpoint.java`：33 行，唯一 explicit-context overload；SHA-256
    `517130018D2074E096086101F04C469027D993C351052CCC148B6BC94FD19B91`。
  - `.../TaskSleep.java`：42 行，唯一 public `sleepOrStop` + private one-shot sleep；SHA-256
    `0487D50A56F15D7F015580638F5126D923373F650F7218E7C368BB193790ADCB`。
- 4 个定点修改，无既有行为删除：
  - `TaskExecutionContext.java`：`+35/-0`，仅新增 4 imports、`throwIfStopRequested()`、
    `isStopRequested()`；SHA-256 `10A099...770D12 -> 6B6A68...B20CE`。
  - `CloudTaskServiceExecutionContext.java`：`+10/-0`，仅新增 typed `checkpointDecision()` delegate；
    SHA-256 `CC3F3E...27110 -> CF9C98...19EC7F`。
  - `CloudTaskRunExecutionGate.java`：`+14/-0`，仅新增 package-private structured classifier forwarder；
    SHA-256 `3E1CF9...9BBD7B -> 9DDAD8...F98527E`。
  - `RemoteTaskRunCoordinator.java`：`+108/-0`（classifier 位于 759-848，含 imports/helper），新增一个
    synchronized read-only classifier；SHA-256 `C03368...C965D -> 0F7401...AA359`。
- 实际总量 `+370/-0`，5 个新文件和 4 个修改文件均低于 Design Repair #1 的逐文件预算，总量低于 `+610`。

### Classifier mapping

| structured outcome | derived disposition | context 行为 |
|---|---|---|
| `CURRENT_ACTIVE_CONFIRMED` | `CONTINUE` | `throwIfStopRequested -> 0L`；`isStopRequested -> false` |
| `STOPPED` | `STOPPED` | throw API 抛 `TaskStopRequestedException`；boolean API 精确返回 `true` |
| `PAUSED` | `PAUSED` | 两个 context API 均抛 `TaskCheckpointTransitionException` |
| `ACTIVE_NEWER_REVISION_CONFIRMED` | `REHYDRATION_REQUIRED` | typed unwind；本切片不 rehydrate |
| `ACTIVE_NEWER_REVISION_UNCONFIRMED` | `DENIED` | typed unwind；不视为 ready |
| `COMPLETED` | `COMPLETED` | typed unwind |
| `ACTIVE_CURRENT_REVISION_UNCONFIRMED` | `DENIED` | typed unwind |
| `PREPARED`、missing、identity/session/window/stopEpoch/taskType mismatch、`FUTURE_REVISION` | `DENIED` | typed fail-closed unwind |
| `INTERRUPTED_WHILE_CURRENT` | `DENIED` | interrupt flag 已恢复后的 typed unwind，绝不冒充 STOPPED |

- 分类顺序为 valid context -> binding -> exact scope/run -> taskType -> window -> STOPPED/COMPLETED/PREPARED ->
  stopEpoch -> future revision -> PAUSED -> ACTIVE newer confirmed/unconfirmed -> ACTIVE current confirmed/unconfirmed。
- identity/session mismatch 与 missing binding 固定返回 `currentRunRevision=-1`、`currentStatus=null`，不泄漏其它 tenant
  状态。classifier 只读 `bindingsByTaskRunId` 与 `confirmedExecutionRevisionByTaskRunId`，不写 map、不 confirm、不 mint。
- `TaskCheckpointDecision` 不暴露可传 disposition 的构造器；`disposition()` 完全由 outcome switch 派生。

### API / visibility / ownership

- `javap -public` 证明：
  - `TaskCheckpoint` 仅有 `public static long throwIfStopRequested(TaskExecutionContext,String)`。
  - `TaskSleep` 仅有 `public static void sleepOrStop(TaskExecutionContext,long,String)`。
  - `TaskExecutionContext` 仅新增 `public long throwIfStopRequested()` 与
    `public boolean isStopRequested()`；**无 `isPauseRequested()`**。
  - `TaskCheckpointDecision` canonical constructor 只有 outcome/revisions/status/confirmed 五个事实参数；
    `disposition()` 为派生 accessor。
  - `TaskCheckpointTransitionException extends RuntimeException`，不是 `TaskStopRequestedException` 子类。
- authority ownership 链固定为 `TaskExecutionContext -> CloudTaskServiceExecutionContext -> package-private
  CloudTaskRunExecutionGate -> synchronized RemoteTaskRunCoordinator`。没有 holder/token/thread-local/raw identity 或
  replacement context/handle API。
- reachability 扫描只发现上述九文件内部 delegate 链；没有 Task/Service caller 使用新 utility，cohort 继续 dormant。

### Interrupt / sleep

- `millis <= 0` 直接返回；正数只执行 checkpoint -> 一次 private `Thread.sleep(millis)` -> checkpoint，无切片、poll、
  retry、TTL 或 pause wait。
- 捕获 `InterruptedException` 后先 `Thread.currentThread().interrupt()`，再对 exact context 分类：STOPPED 保持 stop；
  PAUSED/newer/completed/denied 保持 typed transition；若仍 CURRENT，则构造
  `INTERRUPTED_WHILE_CURRENT/DENIED`。
- pause during sleep 不产生 continuation 或剩余时长；机械 port revision gate 继续 fail-closed，自然醒来后旧 stack unwind。

### Zero-diff / 静态扫描

- protected Cloud scoped status 在实施前后相同：既有 `M pom.xml`、`M CloudBrainServer.java`、untracked endpoint/host/
  authority/ledger/retained/broker/routes/resource 均为并行在途内容；本 worker 未修改它们。
- 明确 zero diff：`CloudTaskRunAuthorityAssembly`、`CloudTaskRunActionLedger`、`CloudTaskRetainedActionState`、
  `RemoteGameCommandBroker`、routes/endpoint/wire、host/server、Cloud `pom.xml`/resources/`src/test`、全部 DHXY
  Java/Maven/resources。
- 九文件扫描：无 `isPauseRequested`、holder、pause token、window runtime、catalog/journal/WAL、rehydration owner/fact、
  `SleepContinuation`；无 filesystem/HTTP/host/route 引用。
- classifier 源码段扫描无 `reason()`/`getReason`/`contains`/`startsWith`，没有 free-text lifecycle inference。
- 未新增、恢复或修改测试；DHXY `src/main/java`/`pom.xml`/resources 的 scoped status 与实施前完全一致。

### Fresh package / Surefire / JAR

- 最终 fresh 命令：`cd D:\mavenProject\dhxy-cloud-brain; mvn -q clean package`，**无 skip**，exit `0`，耗时
  `62.3s`。源码排版预算收窄后重新 clean/package，证据对应最终源码。
- Surefire：4 suites，21 tests，failures `0`，errors `0`，skipped `0`。这些均为仓内既有测试；本 worker 未新增/
  恢复测试。Surefire dumpstream 记录 Windows 不同盘根的 Boot Manifest-JAR absolute-path hint，但未造成 test/package
  failure，非本切片 blocker。
- executable JAR：`target/dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`，119,489,377 bytes，SHA-256
  `C53E14510A90C3C3BA60BD407C6ACE921D75EE91B3BACA63DCEF236A38F81680`；`jar tf` 确认包含
  `TaskCheckpoint*`、`TaskSleep.class` 与 `TaskExecutionContext.class`。
- 未运行 DHXY Maven/测试或任何 application/server/host/poller/UI/capture/OCR/input；未执行 Git mutation。

### Blocker / handoff

- 实施范围内 `P0=0 / P1=0 / P2=0`，无剩余 implementation blocker。
- durable state、wire reconcile fact、rehydration/host transition catch、action restore 和 crash sleep continuation 仍按父级
  决策后置；在这些后续 owner 完成并批准前，所有业务 cohort 必须保持 dormant。
- External Worker Implementation #1 到此停止，等待父级独立复审；本段不把 worker 自审记作 `Approved`。

## Local Implementation Review #1 - APPROVED - 2026-07-12

- 结论：父级逐文件源码审查、结构化边界扫描、public API 字节码检查和独立 clean package 均通过；
  `P0=0 / P1=0 / P2=0`，External Worker Implementation #1 **APPROVED**。
- 分类权威仍在 synchronized `RemoteTaskRunCoordinator.classifyCheckpoint(...)`：先校验完整 context、binding、
  exact scope/run/taskType/window，再区分 terminal、stopEpoch、future revision、PAUSED 与 ACTIVE revision/confirm；
  classifier 只读两个现有 map，没有 `put/remove`、文本 reason 解析或 lifecycle/action mint。
- 布尔绕过已封闭：`TaskExecutionContext.isStopRequested()` 仅
  `CURRENT_ACTIVE_CONFIRMED -> false`、`STOPPED -> true`；PAUSED、newer revision、未确认、terminal complete、
  future/missing/mismatch 全部抛 `TaskCheckpointTransitionException`。public API 中无 `isPauseRequested()`。
- disposition 映射精确：PAUSED 仅为 `PAUSED`，只有 `ACTIVE_NEWER_REVISION_CONFIRMED` 为
  `REHYDRATION_REQUIRED`，newer-unconfirmed 与其它拒绝均为 `DENIED`。这些 disposition 本轮只有诊断/unwind
  含义，不提供 host transition、park 或 context 重建。
- `TaskCheckpoint` 只有 explicit-context overload；null context 为 typed `MISSING_CONTEXT/DENIED`。
  `TaskSleep` 只有 public `sleepOrStop(context,millis,message)`，正数路径仅一次 `Thread.sleep`，interrupt 先恢复
  flag 再 exact classify，CURRENT 时构造 `INTERRUPTED_WHILE_CURRENT/DENIED`；无 raw sleep、holder/token overload、
  轮询、自动重睡、自动 retry 或 sleep continuation。
- 9 个批准文件当前 SHA-256 与 worker 实施记录逐项一致；authority assembly、action ledger、retained state 的
  SHA-256 仍分别为 `D1E6C86B...FEBB`、`BF2B3A33...896E`、`56927DEE...B5A9`，未被本切片改写。
- 父级 fresh `mvn -q clean package` exit 0（62.4s）；Surefire 4 suites / 21 tests，failures 0、errors 0、
  skipped 0。shaded JAR 119,489,377 bytes，父级本轮 SHA-256
  `DA5D951FA5DA3E1FB8185F1AEDFDB4E6899C0452A6F4B77A96A50CE6287C3547`。
- DHXY Java/Maven/resources 零改，故本切片不运行 DHXY compile；现有 DHXY scoped dirty 集与开工基线一致。
  未新增/恢复测试，未启动 server/host/poller/UI/capture/OCR/input，未执行 Git mutation。
- 本批准只关闭 **same-process typed checkpoint/sleep compatibility**。durable crash rehydration、resume reconcile
  fact + execution confirmation 原子跨仓合同、mid-sleep crash continuation 和业务 cohort activation 仍未实现；
  host/Task/Service cohort 必须继续 dormant。

`无已批准业务差异；按基线等价迁移。`
