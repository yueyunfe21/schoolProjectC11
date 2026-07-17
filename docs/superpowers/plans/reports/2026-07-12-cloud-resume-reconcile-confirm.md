# CR271 Cloud resume reconcile-confirm atomic contract

## Scope

- Goal: design the minimum cross-repository contract that binds a trustworthy DHXY local pause/resume reconcile fact to the
  exact Cloud `CONFIRM_EXECUTION` for the new ACTIVE revision, so no Cloud context or retained action can be rebuilt from an
  unverified/stale local window state.
- This is a **design-first** slice. The external worker may append design/review-repair material to this file only. No Java,
  Maven, resources, other docs, CR status, dashboard, tests, applications, server, host, poller, UI, capture, OCR, input, Git
  mutation, production cutover, credentials, paid services, or irreversible operations are allowed before parent
  `DESIGN APPROVED`.
- Repositories:
  - `D:\mavenProject\DHXY`
  - `D:\mavenProject\dhxy-cloud-brain`
- Protect every user/parallel in-flight change. Do not reset, checkout, clean, overwrite, stage, commit, push, or revert.

## Frozen prerequisites

- task-run lifecycle/revision, execution-context fences, stable request/action identity, retained-action typed Service port,
  Cloud-native image processor, and same-process typed checkpoint/sleep are frozen `APPROVED` with `P0/P1/P2=0`.
- Local DHXY remains the sole authority for `WindowRuntimeContext`, native HWND/registration, screenshot/input queues,
  `TaskPauseToken`, and local pause-resume observation. Cloud remains the sole authority for task-run lifecycle/revision,
  execution confirmation, context minting, retained action state, and business decisions.
- An old Cloud context/handle never revives after any revision transition. Resume may create a new context only after the new
  ACTIVE revision receives an exact local fact and confirmation through the approved contract.
- Host, Task and Service cohorts remain dormant. This slice must not activate or execute business work.
- No approved business difference. Phase/retry/fallback/click/navigation/sleep/stop/pause semantics remain baseline-equivalent.

## External Worker Design #1 requirements

Append exactly one section headed `## External Worker - Design #1 - 2026-07-12`, then stop for parent review. Cover all items:

1. Inventory the current Cloud confirm route/service/coordinator and DHXY registration/lifecycle handler/digest/strict-schema
   call path with exact files, visibility, data ownership and current atomicity gaps.
2. Define the smallest immutable typed reconcile fact for the exact scope/taskRun/taskType/window tuple, stopEpoch,
   runRevision, local registration revision/state, pause-resume fingerprint/result, observation time/provenance, and any
   required no-input/no-capture evidence. Do not expose HWND objects, local runtime objects, tokens, Paths or raw mutable maps.
3. Specify one atomic Cloud operation that validates and records the reconcile fact together with execution confirmation for
   the same current ACTIVE revision. A fact-only write or confirm-first/fact-later sequence is forbidden.
4. Give exact validation and rejection matrices for PREPARED/PAUSED/STOPPED/COMPLETED, stale/future revision, unconfirmed,
   scope/session/window/stopEpoch/taskType/registration mismatch, duplicate identical retry, conflicting retry, replacement
   session, disconnect/reconnect, message reordering, timeout and Cloud restart.
5. Show how package-private authority assembly may mint a new Service/Task context only after the atomic record succeeds;
   business code must not receive raw confirm/fact endpoints, coordinator, ledger, request factory or public context minting.
6. Preserve stable request bytes/IDs and digest coverage across Cloud/DHXY. State the exact DTO/schema/digest fields and how an
   identical transport retry remains idempotent while a conflicting retry is denied.
7. State how local DHXY produces the fact without changing existing local pause/resume business behavior. If the current
   `TaskPauseResumeReconciler` result is insufficient, identify the precise additive projection; do not copy local reconcile
   logic into Cloud and do not add a new business truth.
8. Cover tenant isolation, capacity/retention bounds, redaction/logging, metrics/alerts, crash consistency and operator recovery.
   If a durable backend is required for any claim, explicitly separate that dependency instead of pretending an in-memory map
   survives restart.
9. Provide 2-3 approaches with trade-offs and one recommendation. Prefer the smallest contract that closes resume activation
   safety without introducing a general WAL/catalog in this slice.
10. Provide exact proposed file list/visibility/diff budget and pseudocode/state sequence. No implementation before parent
    approval. Verification must use Cloud `mvn -q clean package` without skip and DHXY `mvn -q -DskipTests compile` if Java is
    later approved on both sides; do not add/restore/run DHXY tests.

## Parent gate

- Parent will append exactly one `Local Design Review` with explicit `DESIGN APPROVED` or `BLOCKED`.
- Any P0/P1/P2 keeps Java frozen and returns this same worker for a numbered design repair in this same append-only file.
- `DESIGN APPROVED` authorizes only its exact write set; implementation self-review never counts as parent approval.

## External Worker - Claimed (2026-07-12 19:36)

- 本 slice 已由 external worker 认领。当前仅进行只读设计核查：Cloud confirm 路由/coordinator、DHXY
  registration/lifecycle handler/digest/strict-schema 调用路径、`TaskPauseResumeReconciler` 现状盘点。
- 未修改任何 Java/测试/其它文档；未运行 Maven/host/poller/UI/capture/input；无 Git 操作。
- 若存在另一名已指派 worker，请 parent 在本文件写明，本认领即让位。

## External Worker - Design #1 - 2026-07-12

### 设计结论与基线

- 推荐实施 **方案 A：在既有 task-run lifecycle wire 内新增一个专用
  `RECONCILE_CONFIRM_EXECUTION` action，并把 immutable local reconcile fact 直接存成当前 ACTIVE revision 的唯一
  execution-confirmation record**。Cloud 在同一个 coordinator monitor 内完成全部 validate 和唯一 record write；不存在
  fact-only write，也不存在 confirm-first/fact-later。
- 本切片保持 process-local、dormant、fail-closed。Cloud 或 DHXY 进程重启后不声称恢复内存状态；durable backend/WAL、
  host transition catch、phase/action restore 和 cohort activation 都不在本设计内。
- 已核对 `docs/业务逻辑.md`：已验证回城快照不能因暂停恢复清除（L246-254）；五倍预算暂停时长必须补回
  （L853-855）；通用热启动 Policy 同时约束暂停恢复（L1115 起）；修罗 watchdog 维护/暂停补偿以及 STOP/暂停中断不当
  业务 FAILED（L1264-1266）。本设计只传递现有 reconcile 结论，不新增 TTL、截图/OCR、phase、retry、fallback、cleanup
  或输入顺序。`无已批准业务差异；按基线等价迁移。`
- Design #1 自审为 `P0=0 / P1=0 / P2=0`。生产启用仍有明确外部门：authenticated principal、durable
  rehydration owner/phase state、host typed transition handling；它们不阻止本 dormant contract 的后续实现，但阻止任何
  cohort cutover。本 worker 自审不构成父级 `DESIGN APPROVED`。

### 1. 当前调用路径、可见性与原子性缺口

#### Cloud Brain

| 文件/入口 | 当前 visibility / ownership | 当前行为与缺口 |
|---|---|---|
| `remote/RemoteTaskRunRoutes.java` | public constants | lifecycle 共用 `/api/cloud/remote/task-run`；无需新 route |
| `api/RemoteTaskRunEndpoint.java` | public inactive endpoint | strict unknown-field/schema 检查后直接调 coordinator；`CONFIRM_EXECUTION` 仅收 scope/taskRun/revision/window，无 request ID、digest、taskType、stopEpoch、registration 或 reconcile fact |
| `remote/RemoteTaskRunActionRequest.java` | public wire record | scope 仍是 dev bearer 下的 hint；无 reconcile DTO、requestDigest/idempotency identity |
| `remote/run/RemoteTaskRunCoordinator.java` | public synchronized lifecycle owner | `confirmExecution(...)` 只校验 current ACTIVE/scope/window/revision，并写 `Map<String,Long> confirmedExecutionRevisionByTaskRunId`；exact retry 幂等，但任何 ACTIVE（包括 resume 后）都可走 raw confirm，且无法证明 local reconcile |
| `remote/CloudTaskRunExecutionGate.java` | package-private | context mint/send 最终依赖 coordinator authorize；可复用，但确认记录必须升级为 typed record |
| `remote/CloudTaskRunAuthorityAssembly.java` | package-private | 唯一 ledger/gate/executor assembly，context/runtime mint 也为 package-private；当前 gate 只看 revision confirmation，不知道 reconcile fact |

Cloud 当前 concurrent atomicity 仅覆盖“一次 Long map 写”；它没有 local fact 可原子绑定。更严重的是 DHXY 生产代码中
`RemoteTaskRunApiClient.confirmExecution(...)` 只有声明/HTTP 实现，**没有调用者**，所以当前 dormant context 不能凭可信
local resume evidence mint。

#### DHXY

| 文件/入口 | 当前 visibility / ownership | 当前行为与缺口 |
|---|---|---|
| `task/pause/TaskPauseResumeReconciler.java` | public Spring service，本地事实 owner | 读取 `WindowRuntimeContext` 缓存事实，不截图/OCR/输入；产出 matched 或 hot-start fallback，并执行既有 timer compensation/volatile cleanup |
| `model/pause/TaskPauseResumeReconcileResult.java` | public immutable Lombok value | 只有 decision、pauseBlockedMs、boolean、free-text mismatch、timer/clear 列表；无 typed mismatch、before/after digest、observation provenance、Cloud binding |
| `cloud/remote/RemoteTaskRunRegistry.java` | public process-local registration owner | stable `TaskPauseToken` 与 exact binding 在同一 entry；PAUSED->ACTIVE publish 会唤醒 token，但 entry 没有 local registration revision，也不保留 previous PAUSED snapshot |
| `cloud/remote/RemoteTaskRunLifecycleService.java` | public lifecycle orchestrator | `resume()` 先 Cloud RESUME，再 apply ACTIVE 到 registry 并唤醒；没有等待 reconcile result，也没有调用 confirm API |
| `cloud/remote/RemoteOperationLedger.java` | public local idempotency owner | 可知 request future 是否完成，但 entry 未保存 scope/taskRun/window/runRevision，不能证明旧 revision input/capture 已 quiescent |
| `cloud/remote/LocalRemoteGameCommandHandler.java` | public mechanical handler | side effect 前校验 local runRevision；但已开始的 input bundle 明确在 pause 后沿原 request 继续，故仅 registration ACTIVE 不能证明旧 input 已结束 |
| `cloud/remote/HttpRemoteTaskRunApiClient.java` | public HTTP adapter | strict response tree；lifecycle body 无 digest，现有 confirm request 也没有稳定 requestId |
| `cloud/remote/RemoteProtocolDigests.java` | public canonical digest helper | 只覆盖 remote mechanical request/outcome，不覆盖 lifecycle action body |

当前 gap 不能用“resume 已返回 ACTIVE”掩盖：ACTIVE publication、local reconcile、旧机械 operation drain、Cloud confirmation
是四个不同时间点。只有最后三项组成一个 exact fact 后，Cloud 才可确认新 revision。

### 2. 方案比较

| 方案 | 做法 | 优点 | 缺点/裁决 |
|---|---|---|---|
| **A（推荐）专用 lifecycle action + 单一 typed confirmation record** | 扩展现有 endpoint/request/digest；coordinator 保存一份 `ExecutionConfirmationRecord(kind=RESUME_RECONCILED,fact,request identity)`，其存在即 confirmation | 最小 authority surface；复用现有 route、monitor、gate、quota；无双写事实；不引入 general WAL | process-local，Cloud restart 后只能 fail-closed；符合本 slice 冻结边界 |
| B 独立 `/resume-facts` endpoint/service，再调用原 confirm | fact service 与原 coordinator 分开 | 文件表面隔离 | fact write/confirm 必然出现双写和 bypass；需要分布式事务或补偿，违反 requirement 3，不推荐 |
| C durable DB/WAL transaction + rehydration catalog | binding/fact/confirmation/phase/action 同一 durable transaction | 可支持 Cloud crash recovery、审计和跨进程接管 | 实际是完整 rehydration backend，扩大到 catalog/ledger/host；当前 slice 明确不做，后续独立 CR |

方案 A 不把内存记录包装成 crash durability。它只关闭同一 Cloud 进程中的 resume activation safety；重启时丢失
coordinator binding 与 confirmation，所有新 context mint 失败。

### 3. 最小 immutable reconcile fact

共享 wire 类型建议命名 `RemoteResumeReconcileFact`（Cloud 为 public record，DHXY 为 `@Value @Builder
@Jacksonized`，JSON 字段逐项同名）。不发送 runtime object、HWND/JNA object、token、Path、图片、OCR 文本、mutable map
或 timer/cleared-state 列表。

```text
factVersion = 1
scope = {tenantId,userId,deviceId,clientSessionId}
taskRunId
taskType                         // exact original text; equals binding.taskType
window = {windowId,nativeHandle,processId,playerIdentityEpoch}
stopEpoch
runRevision                     // new current ACTIVE revision
resumedFromRunRevision          // exact immediately preceding PAUSED revision
localRegistrationRevision       // local registry entry monotonic revision
localRegistrationStatus = ACTIVE
localPauseRevision              // TaskPauseToken pause generation, > 0
cumulativePauseNanos            // monotonic token snapshot, non-negative
decision = CONTINUE_ORIGINAL_PHASE | FALLBACK_TASK_HOT_START
mismatchCode                    // typed projection of existing branch, never Cloud-inferred
beforeFingerprintDigest         // lowercase SHA-256, non-zero
afterFingerprintDigest          // lowercase SHA-256, non-zero
beforeCapturedAtEpochMs
observedAtEpochMs
provenance = LOCAL_TASK_PAUSE_RESUME_RECONCILER_V1
operationLedgerRevision
inFlightWindowFactCount = 0
inFlightCaptureCount = 0
inFlightInputCount = 0
freshCapturePerformed = false
freshInputPerformed = false
```

- `mismatchCode` 精确枚举现有 `mismatchReason(...)` 分支：`MATCHED`、`MISSING_BEFORE`、`MISSING_AFTER`、
  `WINDOW_CHANGED`、`PHASE_CHANGED`、`ACTION_STATE_CHANGED`、`PREPARED_ACTION_MISSING/NEW/CHANGED`、
  `VISIBLE_DIALOG_MISSING/NEW/CHANGED`、`PATHING_CHANGED`。现有 free-text reason 原样保留给本地日志/业务 caller，不上 wire。
- before/after fingerprint digest 覆盖现有 fingerprint 的 canonical projection：taskType/taskCode/windowId/native handle
  decimal text/phase/waitReason/actionState/preparedAction/visibleDialog/pathing/capturedAtMs。Cloud 只看 digest，不复制或重算
  local reconcile algorithm。
- `noPause()` 不可产 resume fact。matched 与 fallback 都是可信 reconcile outcome，均可记录；future activation owner 必须
  按 decision 选择原 phase 或既有 hot-start，当前 slice 不实现该 owner。
- “no capture”精确定义为 reconciler 为这次 fact **未发起 fresh capture/OCR**，不是宣称全局没有只读 watcher。
  “no input”更严格：local remote operation ledger 必须证明该 taskRun 所有旧 revision 的 WINDOW_FACT/CAPTURE/INPUT
  request future 均 terminal；这样已开始并在 pause 后继续的 input sequence 已真实退出。

### 4. Wire、稳定 ID、digest 与 strict schema

新增 action `RECONCILE_CONFIRM_EXECUTION`，仍走现有 lifecycle route。outer request 精确字段：

```text
contractVersion, action,
tenantId, userId, deviceId, clientSessionId,   // gateway principal hint；必须等于 fact.scope
requestId, requestDigest,
reconcileFact
```

- `requestId` 是 DHXY producer 首次构造 pending handoff 时生成并保留的 UUID；transport timeout/reconnect 只能重发同一
  immutable request object，不能重建 ID、observation time 或 fact。
- `requestDigest` 使用两仓完全相同的 canonical integral JSON：serialize NON_NULL typed request，移除
  `requestDigest` 本身，按 object key 排序、UTF-8、整数原值、枚举字符串、禁止 float/binary，SHA-256 lowercase hex。
  digest 覆盖上表所有 outer 字段和 fact 的全部字段，因此 scope/window/stop/revision/registration/decision/quiescence
  任一变化都会改变 bytes/digest。
- endpoint raw schema 对该 action 只允许上述字段；嵌套 fact/window/scope 使用 FAIL_ON_UNKNOWN_PROPERTIES、
  FAIL_ON_NUMBERS_FOR_ENUMS、禁止 scalar coercion/float-as-int。Cloud 先重算 digest，再进 coordinator。
- success response 在现有 binding 外增加 `RemoteResumeReconcileConfirmation`：`requestId`、`requestDigest`、
  `factDigest`、`taskRunId`、`runRevision`、`acceptedAtEpochMs`。DHXY strict response 必须逐字段等于 pending request/current
  binding。
- coordinator key 是 taskRunId/current revision，record 保存 requestId + requestDigest + factDigest + immutable fact +
  accepted binding。相同 ID/bytes/digest 重试返回原 receipt；同 ID 不同 digest、同 revision 不同 ID、同 factDigest 但
  不同 request identity 均 `IDEMPOTENCY_CONFLICT`，绝不覆盖 first accepted record。

### 5. Cloud 原子 operation 与 context mint

coordinator 将 `Map<String,Long> confirmedExecutionRevisionByTaskRunId` 收窄为 one-per-run typed
`ExecutionConfirmationRecord`。initial activate 可记录 `kind=INITIAL_REGISTRATION`；resume 必须先由 `resume(...)` 留下
`ResumeConfirmationRequirement(resumedFromRevision,newActiveRevision)`，且旧 `CONFIRM_EXECUTION` 对 RESUME requirement
明确拒绝，关闭 raw-confirm bypass。

```text
synchronized reconcileAndConfirm(scope, requestId, requestDigest, fact):
  verify canonical request digest before monitor
  existing = executionConfirmationByRun[taskRunId]
  if existing matches same requestId + digest + factDigest:
      return existing receipt                 // even if lifecycle later advanced
  if existing is for same revision with any different identity/bytes:
      reject IDEMPOTENCY_CONFLICT

  current = exact binding visible only to exact scope
  require current.status == ACTIVE
  require current revision == fact.runRevision
  require pending requirement == (RESUME, fact.resumedFromRunRevision, fact.runRevision)
  require exact taskType/window/stopEpoch/scope/session
  require registrationStatus ACTIVE and localRegistrationRevision > 0
  require pauseRevision > 0, non-NO_PAUSE decision, valid fingerprint digests/times
  require all three inFlight counts == 0 and no fresh input/capture

  record = ExecutionConfirmationRecord(RESUME_RECONCILED, request identity, fact, current)
  executionConfirmationByRun.put(runId, record) // the single authority write
  return record.receipt
```

事实和 confirmation 不在两个 map 分别写；typed record 的存在就是 confirmation。`authorize(...)`、checkpoint classifier
和 `CloudTaskRunExecutionGate.createContext(...)` 都只承认 record.runRevision 等于 current ACTIVE revision。pause/resume/
stop/complete revision advance 后旧 record 自动失效。

`CloudTaskRunAuthorityAssembly` 无需新增 public API：现有 package-private `createTaskServiceRuntime(...)` 继续先走
package-private execution gate。gate 在 atomic record 不存在时拒绝；成功后才可 mint new immutable context 和 retained
action state。future activation owner 也必须位于该 package，business Task/Service 只拿 `TaskExecutionContext` 与 opaque
action handle，永远拿不到 endpoint、coordinator、ledger、request factory、raw fact writer 或 context mint method。
本 slice 不调用 assembly，不启动 host。

### 6. Validation / rejection matrix

| 输入/当前状态 | 结果 | mutation / retry 语义 |
|---|---|---|
| current ACTIVE + exact pending RESUME + exact fact + zero in-flight | ACCEPTED | 单次 put fact-backed confirmation record |
| PREPARED | `INVALID_STATE` | 无写；initial activation 走原 action，不伪造 resume |
| PAUSED | `INVALID_STATE` | 无写；乱序 confirm-before-resume 可在 resume 后重发同一 pending request |
| STOPPED / COMPLETED | `TERMINAL` | 无写、不可恢复 |
| stale fact revision `< current` | `STALE_REVISION` | 无写；即使旧 fact 曾 accepted，只有 exact duplicate receipt 可回放，不能确认 current |
| future fact revision `> current` | `FUTURE_REVISION` | 无写 |
| ACTIVE but current revision unconfirmed | 正是允许候选 | 只有本 atomic action可把它变成 confirmed；不能先 confirm |
| ACTIVE 已由 exact record confirmed | exact retry 返回原 receipt | different request/fact 为 conflict |
| scope tenant/user/device mismatch | `NOT_FOUND`-style redacted denial | 不泄漏 binding/fact |
| clientSession mismatch / replacement session | `SESSION_CONFLICT`/redacted denial | 原 session fact 不可接管；走既有 replacement STOP recovery |
| taskRunId/taskType/window tuple/stopEpoch mismatch | typed `BINDING_MISMATCH` | 无写；window 任一 handle/process/player epoch 不同均拒绝 |
| local registration status 非 ACTIVE、revision <=0、previous status 非 PAUSED | `REGISTRATION_MISMATCH` | 无写 |
| pending Cloud resume requirement 缺失或 fromRevision 不同 | `RECONCILE_NOT_EXPECTED` | 防止 initial ACTIVE/raw confirm 冒充 resume |
| noPause、digest invalid、fresh input/capture=true、任一 in-flight >0 | `INVALID_FACT` / `LOCAL_NOT_QUIESCENT` | 无写；DHXY 应在本地 quiescence 未满足时不发送 |
| identical requestId + bytes/digest/fact | 返回原 accepted receipt | 不重新写、不更新时间 |
| same ID/different bytes，或 same revision/new ID | `IDEMPOTENCY_CONFLICT` | first record 保持不变 |

### 7. 乱序、断线、timeout 与 restart

- **消息乱序**：fact 先于 RESUME 到达时 Cloud 仍 PAUSED，拒绝且无写；RESUME 后相同 immutable request 可重发。旧
  pause cycle 的 fact 因 fromRevision/runRevision/localRegistrationRevision 不匹配拒绝。STOP/COMPLETE 优先终结。
- **response lost / HTTP timeout**：client 将状态视为 UNKNOWN，不生成新 request；同进程只重发 exact bytes。若 Cloud
  已接受则返回原 receipt；若未接受则正常执行。没有自动 retry 次数/业务 fallback 变更。
- **disconnect/reconnect（同 clientSession）**：内存 pending request 保留时 exact retry；若 session ID 改变，视为
  replacement session，不能确认旧 run。
- **DHXY restart**：local registry、pending request 与 operation ledger 都不 durable；新 clientSession 无权重造 fact，
  只能走既有 replacement discovery/STOP，再启动新 run。
- **Cloud restart**：coordinator binding、resume requirement、fact-backed confirmation 同时丢失；endpoint 返回 not found，
  gate/context mint fail-closed。要支持 crash continuation 必须把 binding + transition requirement + confirmation fact +
  phase/action catalog 放同一 durable transaction，这是后续 backend slice，不在此伪装。
- **wall-clock age**：observation time只做审计，不设 TTL。revision/session/registration/digest 已封闭 replay；新增 TTL 会
  改变基线时序且受到时钟漂移影响。

### 8. DHXY fact producer，不改本地业务真相

现有 reconcile result 不足，采用精确 additive projection：

1. `TaskPauseResumeReconciler` 保留现有 `capture`、mismatch 顺序、timer compensation、volatile clear 和日志主体；只在
   同一分支同时生成 `TaskPauseResumeReconcileObservation`（typed mismatch、before/after canonical digest、timestamps、
   pause token revision/progress、provenance、freshCapture/input=false），挂到 result 新 optional 字段。旧 caller 继续只读
   原字段，行为不变。
2. `RemoteTaskRunRegistry.RegistryEntry` 新增 monotonic `localRegistrationRevision` 与 previous binding。首次 register=1；
   identical publication 不增；每个 accepted changed binding 增 1。只提供 package-private immutable resume snapshot，且
   要求 current ACTIVE、previous PAUSED、exact scope/window/stop/revision。
3. `RemoteOperationLedger` entry 加 powerless scope/taskRun/window/runRevision/operation identity 与 monotonic ledger
   revision；package-private snapshot 只报告该 run 所有未完成旧 revision operation count。它不取消、不等待、不执行输入。
4. 新 package-private `RemoteResumeReconcileConfirmService` 接受 explicit scope/taskRunId/observation：读取 exact registry
   snapshot -> 读取 ledger quiescence -> 再次验证同一 localRegistrationRevision 未变化 -> 构造并 retain immutable request
   -> 调 API。两次 registry 校验之间，resume 后新 revision 尚未 Cloud-confirm，无法产生新授权命令；旧 revision 新到
   request 即使 ledger claim，也会在 local revision pre-side-effect gate 拒绝。已在执行的旧 request 则必然被第一次
   ledger snapshot 计为 in-flight。
5. `RemoteTaskRunLifecycleService` 只新增 explicit `confirmResumedExecution(...observation)` orchestration method；现有
   `resume()`、三个业务 Task caller、pause token wait 和 hot-start分支均不自动联网、不改顺序。future remote pause
   controller 才可调用；当前 host/cohort dormant，因此本 slice 不产生运行副作用。

Cloud 不接收 raw mismatch reason、compensatedTimers、clearedVolatileState，也不重跑 fingerprint comparison。matched/
fallback 是 local reconciler 的既有真相；Cloud 只验证绑定与 provenance 后记录。

### 9. Tenant、capacity 与运维

- **隔离**：coordinator lookup 先 exact four-part scope；owner quotas 仍按 tenant/user/device，session 不可替换。日志不输出
  nativeHandle、fingerprint 内容、mismatch raw value、完整 tenant/user/device/clientSession；仅输出 action、status、
  revisions、windowId、request/digest/factDigest 前 8 位和 hashed owner correlation。
- **容量**：Cloud 每个 retained run 最多一个 current confirmation record + 一个 pending resume requirement，计入现有
  global 10,000 / owner retained 1,000 / owner non-terminal 64，不建独立无界 request ledger。terminal record 与 binding 同
  retention lifetime。DHXY 每个 registry entry 最多一个 pending request/observation，受 registry global/owner cap；同
  revision conflict 不追加历史。
- **metrics**：`resume_reconcile_confirm_total{outcome,reason}`、`resume_reconcile_retry_total{same,conflict}`、
  `resume_reconcile_local_quiescence_wait_ms`、`resume_reconcile_inflight_block_total{operation}`、
  `resume_reconcile_fact_age_ms`（观测，不做 gate）、`resume_reconcile_context_mint_denied_total{reason}`。
- **alerts**：tenant/session mismatch、idempotency conflict、future revision、confirmation without pending RESUME、capacity
  >80%、连续 local-not-quiescent、Cloud restart/not-found burst。不得自动 repair、takeover、重放 input 或跳 phase。
- **operator recovery**：先查 request/digest prefix、run/current/from/local registration revision 与 typed reject；不手工写
  map。same-process UNKNOWN 只允许 exact retry；任一进程重启或 session replacement 走 stop/restart run。需要跨重启
  恢复时必须先交付 durable backend，不允许 operator 伪造 fact。

### 10. 精确拟改文件、visibility 与 diff budget

#### Cloud Brain（4 new + 8 targeted modify，预计 `<=1,050` added lines）

| 文件 | visibility | budget | 责任 |
|---|---|---:|---|
| new `remote/RemoteResumeReconcileDecision.java` | public enum | <=25 | 两个 local baseline decision |
| new `remote/RemoteResumeMismatchCode.java` | public enum | <=45 | typed existing mismatch projection |
| new `remote/RemoteResumeReconcileFact.java` | public record | <=170 | exact immutable wire fact + validation |
| new `remote/RemoteResumeReconcileConfirmation.java` | public record | <=80 | immutable accepted receipt |
| modify `remote/RemoteTaskRunAction.java` | public enum | +1 | new action only |
| modify `remote/RemoteTaskRunActionRequest.java` | public record | +85/-0 | requestId/digest/fact accessors for new action |
| modify `remote/RemoteTaskRunActionResponse.java` | public record | +45/-0 | optional typed receipt with action-specific invariant |
| modify `remote/RemoteProtocolDigests.java` | public utility | +90/-0 | canonical lifecycle request/fact digest |
| modify `api/RemoteTaskRunEndpoint.java` | public inactive endpoint | +150/-0 | strict nested schema, digest check, typed error mapping |
| modify `remote/run/RemoteTaskRunCoordinator.java` | public lifecycle owner; private records/maps | +330/-20 | pending RESUME requirement, single fact-backed confirmation record, matrix/idempotency |
| modify `remote/CloudTaskRunExecutionGate.java` | package-private | +35/-5 | require exact typed confirmation record, no raw fact API |
| modify `remote/RemoteTaskRunErrorCode.java` | public enum | +6/-0 | typed conflict/stale/future/quiescence categories |

`CloudTaskRunAuthorityAssembly`、ledger、retained state、broker、command executor、routes、host、server、resources、pom、tests
保持 zero diff；assembly 通过已加固 gate 自然获得 mint fence。

#### DHXY（8 new + 11 targeted modify，预计 `<=1,650` added lines）

| 文件 | visibility | budget | 责任 |
|---|---|---:|---|
| new `model/pause/TaskPauseResumeMismatchCode.java` | public enum | <=45 | local typed projection |
| new `model/pause/TaskPauseResumeReconcileObservation.java` | public immutable value | <=120 | before/after digest、pause/provenance，不含 cloud authority |
| new `cloud/remote/RemoteResumeReconcileDecision.java` | public wire enum | <=25 | Cloud byte-equivalent enum |
| new `cloud/remote/RemoteResumeMismatchCode.java` | public wire enum | <=45 | Cloud byte-equivalent enum |
| new `cloud/remote/RemoteResumeReconcileFact.java` | public immutable wire DTO | <=190 | JSON field parity |
| new `cloud/remote/RemoteResumeReconcileConfirmation.java` | public immutable wire DTO | <=90 | strict receipt |
| new `cloud/remote/RemoteTaskRunRegistrationSnapshot.java` | package-private immutable value | <=90 | local registration revision + previous PAUSED binding |
| new `cloud/remote/RemoteResumeReconcileConfirmService.java` | package-private final | <=230 | exact snapshot/quiescence/request retention/API orchestration |
| modify `model/pause/TaskPauseResumeReconcileResult.java` | public immutable value | +20/-0 | optional additive observation |
| modify `task/pause/TaskPauseResumeReconciler.java` | public existing service | +120/-0 | typed code/canonical observation；保留现有主体 |
| modify `cloud/remote/RemoteTaskRunRegistry.java` | public owner; package-private snapshot API | +150/-10 | monotonic local registration revision/previous binding/double validation |
| modify `cloud/remote/RemoteOperationLedger.java` | public owner; package-private query | +130/-10 | retain operation fence and quiescence snapshot |
| modify `cloud/remote/RemoteTaskRunAction.java` | public enum | +1 | new action |
| modify `cloud/remote/RemoteTaskRunActionRequest.java` | public DTO | +30/-0 | request ID/digest/fact |
| modify `cloud/remote/RemoteTaskRunActionResponse.java` | public DTO | +20/-0 | receipt |
| modify `cloud/remote/RemoteProtocolDigests.java` | public utility | +100/-0 | lifecycle canonical digest parity |
| modify `cloud/remote/RemoteTaskRunApiClient.java` | package interface | +15/-0 | typed atomic call |
| modify `cloud/remote/HttpRemoteTaskRunApiClient.java` | public adapter | +170/-0 | stable request send + strict schema/receipt validation |
| modify `cloud/remote/RemoteTaskRunLifecycleService.java` | public orchestrator | +140/-0 | explicit dormant confirm method；现有 resume 不变 |

所有业务 Task、`TaskPauseToken`、`WindowRuntimeContext`、InputActionQueue/Worker、capture/OCR/input provider、poller/host/UI、
Maven/resources/tests、其它 docs/CR/dashboard 为 zero diff。若父级认为 19 个 DHXY 文件仍过宽，可把 wire DTO parity 与
producer 分成两个批准批次，但 atomic contract 不可删减 quiescence/registration revision 或退回双写。

### State sequence

```text
Cloud PAUSED r
  -> RESUME CAS
Cloud ACTIVE r+1, pendingRequirement=(RESUME, from=r, to=r+1), unconfirmed
  -> DHXY strict response + registry apply
DHXY local ACTIVE, localRegistrationRevision=g, previous=PAUSED r; stable token wakes
  -> existing TaskPauseResumeReconciler (unchanged decision order/side effects)
typed observation O
  -> exact registry snapshot g + remote operation ledger quiescence + exact registry recheck g
immutable request Q(requestId,digest,fact)
  -> HTTP timeout means UNKNOWN; retry Q byte-for-byte only
Cloud synchronized validate(Q) + one ExecutionConfirmationRecord put
  -> receipt C
Cloud gate sees current ACTIVE r+1 + exact RESUME_RECONCILED record
  -> future package-private activation owner may mint new context/retained state
  -> current slice: STOP, host/cohort remain dormant
```

### 批准后验证计划（本轮未执行）

```powershell
cd D:\mavenProject\dhxy-cloud-brain
mvn -q clean package

cd D:\mavenProject\DHXY
mvn -q -DskipTests compile

rg -n "RECONCILE_CONFIRM_EXECUTION|RemoteResumeReconcileFact|ExecutionConfirmationRecord" `
  D:\mavenProject\dhxy-cloud-brain\src\main\java `
  D:\mavenProject\DHXY\src\main\java
rg -n "TaskExecutionContextHolder|WindowRuntimeContext|TaskPauseToken|java.nio.file|Path|BufferedImage" `
  <两仓 RemoteResumeReconcileFact 文件>
rg -n "reason\(\)|contains\(|startsWith\(" `
  <Cloud coordinator/endpoint 新 classifier 段>
javap -classpath D:\mavenProject\dhxy-cloud-brain\target\classes -public `
  com.yueyunfe.dhxy.cloudbrain.remote.RemoteResumeReconcileFact `
  com.yueyunfe.dhxy.cloudbrain.remote.RemoteResumeReconcileConfirmation
git -C D:\mavenProject\dhxy-cloud-brain status --short -- pom.xml src/main/resources src/test
git -C D:\mavenProject\DHXY status --short -- pom.xml src/main/resources src/test
```

- Cloud package 必须无 skip、exit 0，记录现有 Surefire/JAR；DHXY 仅 compile with skip，不新增/恢复/运行 DHXY tests。
- 另做两仓 canonical fixture 的人工同字节/digest 对照、strict unknown-field 拒绝、public API/javap、source reachability、
  protected status/hash 扫描。因 no-local-test mode，不新增 source guard/unit/integration test。
- 不启动 application/server/host/poller/UI/capture/OCR/input，不做 Git mutation。Design #1 到此停止，等待父级
  `DESIGN APPROVED` 或 `BLOCKED`。

## Local Design Review #1 - BLOCKED - 2026-07-12

- 评审口径：以本文件第二个、完整结束于本节之前的 `External Worker - Design #1` 为最新有效材料；第一个同名
  Design #1 在 parent notes 并发插入时被截断，标记为 **superseded / non-authoritative**，不删除历史。
- 结论：专用 lifecycle action、coordinator 单一 typed confirmation record、same-byte retry 和 Cloud restart
  fail-closed 的方向成立；但当前设计仍有 `P0=0 / P1=2 / P2=1`，**BLOCKED，Java 继续冻结**。

### P1-1：把本地业务恢复决定写成 Cloud execution-confirmation fact，重新形成双重业务权威

- 证据：Design §3/§8 的 wire fact 包含 `decision=CONTINUE_ORIGINAL_PHASE|FALLBACK_TASK_HOT_START`、
  `mismatchCode` 和 before/after fingerprint digest；digest 又覆盖 phase、actionState、preparedAction、visibleDialog、
  pathing。Design §5 明确 future activation owner 按该 decision 选择原 phase/既有 hot-start。
- 源码事实：`TaskPauseResumeReconciler` 读取 `WindowRuntimeContext` 并执行 timer compensation/volatile cleanup；其
  `mismatchReason(...)` 决定 matched/fallback。`docs/业务逻辑.md` §通用任务类热启动 Policy 明确“从哪里继续”属于
  修罗/五倍/五环任务业务恢复规则，不是纯窗口注册事实。
- 影响：DHXY 将继续决定 Cloud Task 应继续原 phase 还是 hot-start，并把 phase/action/pathing 业务状态作为确认门；这
  违背最终 thin client 只保留窗口、截图、输入、UI 和安全拒绝，也让 Cloud 与 local 对恢复策略拥有双重权威。
- 返修条件：wire/Cloud confirmation record 必须只承载 **local executor readiness**：exact scope/taskRun/taskType/window/
  stopEpoch/from+to runRevision、local registration generation/current ACTIVE+previous PAUSED、pause-token mechanical
  generation（仅审计）、operation-ledger revision/zero in-flight 和本地机械 provenance。删除 decision、mismatch、
  fingerprint、phase/action/dialog/pathing/timer/clear 字段；不得修改 `TaskPauseResumeReconciler`、
  `TaskPauseResumeReconcileResult`、pause model 或任何业务 Task。Cloud-owned business checkpoint/rehydration owner 在后续
  独立门决定 phase/hot-start；本 slice 只证明本地 executor 对新 revision 可安全接令。

### P1-2：设计的唯一 fact producer 随本地业务 Task 迁走后不可达，合同无法实际确认任何 resume revision

- 证据：当前 `taskPauseResumeReconciler.capture/reconcileAfterPause` 的所有 main caller 仅在 `WuhuanTaskV2`、
  `WubeiTask`、`XiuluoTaskV2`。Design §8 仍要求这些 caller 先产生 observation，再由一个未指定的 future remote pause
  controller 调 explicit `confirmResumedExecution(...)`；同时业务 Task/host 均列为 zero diff/dormant。
- 影响：全量迁云后 local 不再运行这些业务 Task，因而没有 observation、没有请求，也没有 execution confirmation；
  Cloud context mint 永久 fail-closed，本设计不能解除下一 Service/Task cohort 的实际阻塞。
- 返修条件：选择并写死一个 **纯机械、可达** 的 producer/trigger，不依赖任何本地 Task 或 business reconciler。
  推荐：Cloud RESUME -> DHXY registry 发布 exact ACTIVE/previous PAUSED -> registry/operation-ledger 形成一条 per-run
  pending executor-readiness request；现有 dormant polling/lifecycle transport 在旧 revision operation drain 后重用同一
  retained request 发送。不得新增线程、poller、自动业务 retry 或 input/capture；必须给出 trigger 文件、并发时序、
  local-publish 后网络 timeout 的 exact-byte retry、terminal/revision/session 变化时的取消/清理及当前 host dormant 门。
  若选择同步 `resume()` 内确认，必须证明不会等待正在执行的旧 input、不会改变既有 resume 返回/暂停补偿语义；否则
  不得选该方案。

### P2-1：pending request 的唯一属主、容量和清理条件仍不确定

- 证据：Design 声称“每 registry entry 最多一个 pending request/observation”，但拟改表只给 registry previous binding/
  local generation，`RemoteResumeReconcileConfirmService` 又要 retain immutable request；未明确 request 实际存放在哪个
  对象、如何避免 service-level 无界 map、何时在 exact receipt/terminal/new revision/session replacement 时清除。
- 影响：same-byte retry 合同可能因对象重建丢失稳定 ID/bytes，或因未清理造成 per-run 内存残留；容量声明无法审核。
- 返修条件：Design Repair 必须指定唯一 owner（优先 registry entry 内的 package-private pending slot，或等价的现有有界
  owner）、CAS/lock 边界、one-per-run invariant、稳定对象复用和所有清理转移；不得新建无界旁路 map。

### Design Repair #1 的 B-PASS 门

1. 将合同改名/收窄为 executor readiness + atomic execution confirmation，不携带任何本地业务恢复决策。
2. 删除对 pause reconcile model/service/业务 Task 的拟改；给出不依赖业务 Task 的机械 producer 与现有 transport
   可达路径。
3. 保留 coordinator one-record atomicity、pending RESUME requirement、旧 raw confirm 对 resumed revision 的 bypass
   拒绝、exact scope/window/stop/revision/session 与 zero-in-flight 门。
4. 明确 one-per-run retained request 的 owner/lock/capacity/清理，timeout 只重交同一 object/bytes/id/digest。
5. 明确本 slice 成功只表示 local executor ready；没有 Cloud-owned durable business checkpoint/rehydration owner 时，
   host/Task/Service cohort 仍不得激活。
6. 同一 worker 只追加 `External Worker - Design Repair #1 - 2026-07-12`；不得再追加第三个 Design #1，不得改 Java、
   Maven、resources、tests 或其它文档。

`无已批准业务差异；按基线等价迁移。`

## External Worker - Design Repair #2 - 2026-07-12

### 返修范围与结论

- 本 Repair 只补 `READY -> SENDING` 锁外 HTTP 的 immutable send handle、exact-result CAS 和 stale-result discard。
  `Local Design Review #3` 已关闭的 producer、wire、digest、file table、pure executor-readiness、pending RESUME、Cloud
  one-record atomic confirmation、raw-confirm bypass、zero old-revision inflight 与 dormant gate 全部保持，不重新设计。
- 本版自审 `P0=0 / P1=0 / P2=0`，无剩余设计 blocker；自审不构成父级批准。Java 继续冻结。

### Immutable PendingSendHandle

`RemoteTaskRunRegistry.claimReadyForSend(exactPollScope, nowNanos)` 每次最多返回一个 immutable：

```java
record PendingSendHandle(
        long entryGeneration,
        long slotGeneration,
        String requestId,
        long toRevision,
        PendingExecutorReadiness.RetainedSend retainedSend) {
}
```

- `entryGeneration` 由 `RemoteTaskRunRegistry` 的 registry-wide monotonic counter 在既有 `mutationLock` 下分配；每个新
  `RegistryEntry` 只分配一次，entry removal/re-registration/session replacement 永不复用。counter 到 `Long.MAX_VALUE`
  时 fail-closed 拒绝新 entry，不回绕。
- `slotGeneration` 来自每个 entry 自己的 monotonic `nextSlotGeneration`，也只在 `mutationLock` 下、每次
  PAUSED->ACTIVE 建立新 pending slot 时 `Math.incrementExact`。同 binding 的幂等 publication 不增；PAUSE/new RESUME/
  supersede/terminal 不回退。溢出时不创建 slot并产生 capacity/safety alert，绝不复用旧 generation。
- `requestId`、`toRevision` 与 `retainedSend` 均取自当前 READY slot。`retainedSend` 是 materialize 时创建且之后不替换的
  exact request object/body/id/digest owner；handle 保存同一 object reference。
- claim 在 `mutationLock` 内同时验证 pollRequest exact tenant/user/device/clientSession、slot `READY`、机械
  `nextAttemptNotBeforeNanos <= nowNanos`，然后转 `SENDING` 并返回 handle。HTTP 仍在锁外。

### Exact-handle CAS APIs

registry 只新增/收窄以下 package-private result API；三个方法都在现有 `mutationLock` 内执行：

```text
markAccepted(PendingSendHandle handle, RemoteExecutorReadinessConfirmation receipt)
markUnknownForRetry(PendingSendHandle handle, long nextAttemptNotBeforeNanos, RetryReason reason)
markPermanentRejected(PendingSendHandle handle, RemoteTaskRunErrorCode code)
```

三者先执行同一个 `matchesCurrentSending(handle)`：

```text
entry still exists for retainedSend.taskRunId
&& entry.entryGeneration == handle.entryGeneration
&& current slot != null
&& current slot.state == SENDING
&& current slot.slotGeneration == handle.slotGeneration
&& current slot.requestId == handle.requestId
&& current slot.toRevision == handle.toRevision
&& current slot.retainedSend is the same object as handle.retainedSend
```

- **全部匹配**才允许 CAS-like 转移：
  - `markAccepted` 还必须先由 pump/client 验证 receipt 的 requestId/requestDigest/factDigest/taskRunId/toRevision 与
    retained send 完全一致；随后 `SENDING -> null`，释放 request body。
  - `markUnknownForRetry` 只做 `SENDING -> READY`，在同一 slot 写入 bounded mechanical
    `nextAttemptNotBeforeNanos`；retained object/body/id/digest 不变。
  - `markPermanentRejected` 只做 `SENDING -> null` 并记 typed reject metric。
- **任一不匹配**即返回 `STALE_HANDLE_IGNORED`：不清 slot、不改 state、不改 retry time、不触碰 retained object。只记录
  redacted `executor_readiness_stale_result_total{resultKind}` 与日志中的旧 entry/slot generation、旧 toRevision、requestId
  前8位；不输出当前新 slot 的 body/digest/scope。
- pump 不允许按 taskRunId 单独回写，也不允许失败后重新 lookup “current slot”再操作；所有 receipt/reject/timeout 必须
  携带最初 claim 返回的同一个 handle。

### Updated slot state machine

```text
entry(E), slot null
  -> AWAITING_DRAIN(S=nextSlotGeneration)
  -> READY(S, retainedSend, nextAttempt)
  -> claim -> SENDING(S) + immutable handle(E,S,requestId,toRevision,retainedSend)

SENDING(S) + exact handle + accepted receipt
  -> null
SENDING(S) + exact handle + timeout/5xx/UNKNOWN
  -> READY(S, SAME retainedSend, throttled nextAttempt)
SENDING(S) + exact handle + permanent reject
  -> null

any state + PAUSE/new RESUME/STOP/COMPLETE/session removal
  -> old slot removed; new resume creates S+1 (or a new entry E+1)

any late result + stale handle
  -> STALE_HANDLE_IGNORED; current slot unchanged
```

slot 的 current identity 是 `(entryGeneration, slotGeneration)`；`requestId/toRevision/retainedSend identity` 是额外防线，
防止错误 object 被同 generation API 使用。

### A/B late-response ordering

```text
t0  entry E7 creates slot A: S11, toRevision=r+1
t1  claim A -> handle HA=(E7,S11,idA,r+1,sendA); HTTP runs outside lock
t2  local PAUSE clears A
t3  next RESUME creates slot B: S12, toRevision=r+3, later READY/SENDING
t4  A response arrives
t5  markAccepted(HA, receiptA) checks current B against HA
    slotGeneration S12 != S11 (also id/toRevision/object differ)
    -> STALE_HANDLE_IGNORED; B remains byte-for-byte/state-for-state unchanged
t6  only handle HB=(E7,S12,idB,r+3,sendB) may transition B
```

若 t2 同时发生 session replacement/entry removal，则旧 E7 被删除；新 entry 分配 E8（即使 taskRunId 文本意外相同），
HA 在 entryGeneration 第一项即失败。由此 session removal/new resume 后旧 handle 永不匹配。

### Pump / registry API update

不扩大上一版拟改文件集，仅细化其中三项：

| 已列文件 | Repair #2 精确增量 |
|---|---|
| `cloud/remote/PendingExecutorReadiness.java` | 增加 monotonic slotGeneration、stable retainedSend identity、state/nextAttempt；新增 immutable nested `PendingSendHandle`（若父级要求 top-level，可同文件 package-private record） |
| `cloud/remote/RemoteTaskRunRegistry.java` | registry-wide entry generation；`claimReadyForSend(exactPollScope,now)`；三个 exact-handle CAS result 方法；所有逻辑只用现有 `mutationLock` |
| `cloud/remote/RemoteExecutorReadinessPump.java` | claim 后只保留 handle；accepted/unknown/permanent 三种 typed outcome 分别调用对应 CAS API；stale 返回只计 metric，不重试/清新 slot |

`RemoteCommandPollingLoop` 仍按已批准设计在 IDLE 与 outcome-submitted 边界调用局部 typed try/catch；每个 boundary 最多
一条，只 claim 与 `pollRequest` exact four-part scope/session 匹配的 slot。readiness HTTP 使用既定 bounded max timeout；
unknown retry 保留 same bytes/id/digest 并应用 slot 中 mechanical not-before 节流。无 readiness exception 进入 poll loop 外层
catch，因此不停止 command poll；一次 readiness timeout 后必先进入下一轮 command poll，不能连续 drain readiness 队列而
饿死 command transport。

### Invariants / zero diff

- fact 仍只有 executor registration/quiescence mechanical fields；不恢复 decision、mismatch、fingerprint、phase/action/
  dialog/pathing/timer/clear，也不修改本地 reconciler/model/业务 Task。
- old revision 已开始 input 仍等 ledger future terminal；本 Repair 不 cancel、不重排、不改变 mid-bundle。
- Cloud pending RESUME、atomic accepted record、raw confirm bypass 拒绝、gate/context exact record read 均不变。
- 双仓 action/request/response/fact/receipt/digest/endpoint/coordinator/gate/client 文件表与 diff budget不变；本 Repair 不新增
  文件，不改 host/server/poller activation、pom/resources/tests/其它 docs。
- 成功仍只表示 local executor ready。Cloud durable business checkpoint、phase/action rehydration 与 continuation owner 后置；
  host/Task/Service cohort 保持 dormant。

### 批准后验证增量（本轮不执行）

```powershell
rg -n "PendingSendHandle|entryGeneration|slotGeneration|markAccepted|markUnknownForRetry|markPermanentRejected|STALE_HANDLE_IGNORED" `
  D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote
rg -n "claimReadyForSend|flushExecutorReadinessSafely" `
  D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote
```

- 静态复核三种 result path 都携带 original handle，且不存在仅按 taskRunId 清/ready slot 的 API；构造 A(r+1)/B(r+3)
  时序做人工 source walkthrough，确认 A 的三类迟到结果都返回 stale、B 不变。
- 保持既定后续门：Cloud `mvn -q clean package` 无 skip；DHXY `mvn -q -DskipTests compile`；不新增/恢复/运行 DHXY
  tests。本轮未运行 Maven/测试/应用/server/host/poller/UI/capture/OCR/input，未做 Git mutation。

Design Repair #2 到此停止，等待父级 `DESIGN APPROVED` 或 `BLOCKED`。

## Local Process Note #3 - Post-approval duplicate design material - 2026-07-12

- 第 1565 行 `Local Design Review #4 - DESIGN APPROVED` 是当前有效门禁；其后三个 `Design Repair #2`/Addendum
  均为 worker 并发处理旧输入产生的滞后重复材料，不回退、不替代批准，也不构成新 review 请求。
- 已再次中断同一 worker：禁止继续写任何 Design/Repair/Addendum；下一条有效外部材料只能是代码及双构建完成后的
  `External Worker - Implementation #1 - 2026-07-12`。批准文件集、绑定修正和 zero-diff 边界均以 Review #4 为准。

## Local Process Note #1 - Ownership confirmed - 2026-07-12

- 当前不存在第二名 external worker；本文件中 `External Worker - Claimed` 的作者就是本切片唯一设计 owner。
- 不得因上一 retained-action slice 的并行 ownership 经验继续 hold，也不等待其它 agent、heartbeat 或用户决定。
- 下一条有效材料必须直接是完整的 `## External Worker - Design #1 - 2026-07-12`，逐项覆盖本文件 10 条
  requirements 后停止等待父级 review；在此之前 Java/Maven/resources/tests 继续冻结。

## Local Read-only Evidence #1 - Current atomicity gap - 2026-07-12

- DHXY `RemoteTaskRunApiClient`/`HttpRemoteTaskRunApiClient` 已声明并实现 `confirmExecution(...)`，但
  `rg "\.confirmExecution\(" src/main/java` 为零调用；当前生产主路径没有向 Cloud 提交 execution confirmation。
- DHXY `RemoteTaskRunLifecycleService.resume(...)` 当前顺序是 Cloud `RESUME` 得到新 ACTIVE revision，然后
  `applyConfirmed(...) -> RemoteTaskRunRegistry.applyConfirmedBinding(...)` 在本地发布 ACTIVE；registry
  `publishTransition(...)` 会恢复稳定 `TaskPauseToken`。该路径没有 reconcile fact，也没有随后原子 confirm Cloud。
- Cloud `RemoteTaskRunEndpoint` 的 `CONFIRM_EXECUTION` 只接收 scope/taskRunId/expectedRevision/window，并直接调用
  synchronized `RemoteTaskRunCoordinator.confirmExecution(...)`；coordinator 仅把 `taskRunId -> confirmedRevision`
  写入内存 map，没有 fact、request identity/digest 或冲突重试记录。
- 因而当前安全状态是 host/cohort dormant + Cloud unconfirmed fail-closed；本切片不得把“API 已存在”误写成“确认链已
  接通”。Design #1 必须说明本地 ACTIVE 发布后的失败/重试收敛，以及为何 Cloud 只有在 fact+confirm 同一原子写成功
  后才能 mint 新 context。

## External Worker - Design #1 - 2026-07-12

### 设计结论与基线

- 推荐实施 **方案 A：在既有 task-run lifecycle wire 内新增一个专用
  `RECONCILE_CONFIRM_EXECUTION` action，并把 immutable local reconcile fact 直接存成当前 ACTIVE revision 的唯一
  execution-confirmation record**。Cloud 在同一个 coordinator monitor 内完成全部 validate 和唯一 record write；不存在
  fact-only write，也不存在 confirm-first/fact-later。
- 本切片保持 process-local、dormant、fail-closed。Cloud 或 DHXY 进程重启后不声称恢复内存状态；durable backend/WAL、
  host transition catch、phase/action restore 和 cohort activation 都不在本设计内。
- 已核对 `docs/业务逻辑.md`：已验证回城快照不能因暂停恢复清除（L246-254）；五倍预算暂停时长必须补回
  （L853-855）；通用热启动 Policy 同时约束暂停恢复（L1115 起）；修罗 watchdog 维护/暂停补偿以及 STOP/暂停中断不当
  业务 FAILED（L1264-1266）。本设计只传递现有 reconcile 结论，不新增 TTL、截图/OCR、phase、retry、fallback、cleanup
  或输入顺序。`无已批准业务差异；按基线等价迁移。`
- Design #1 自审为 `P0=0 / P1=0 / P2=0`。生产启用仍有明确外部门：authenticated principal、durable
  rehydration owner/phase state、host typed transition handling；它们不阻止本 dormant contract 的后续实现，但阻止任何
  cohort cutover。本 worker 自审不构成父级 `DESIGN APPROVED`。

### 1. 当前调用路径、可见性与原子性缺口

#### Cloud Brain

| 文件/入口 | 当前 visibility / ownership | 当前行为与缺口 |
|---|---|---|
| `remote/RemoteTaskRunRoutes.java` | public constants | lifecycle 共用 `/api/cloud/remote/task-run`；无需新 route |
| `api/RemoteTaskRunEndpoint.java` | public inactive endpoint | strict unknown-field/schema 检查后直接调 coordinator；`CONFIRM_EXECUTION` 仅收 scope/taskRun/revision/window，无 request ID、digest、taskType、stopEpoch、registration 或 reconcile fact |
| `remote/RemoteTaskRunActionRequest.java` | public wire record | scope 仍是 dev bearer 下的 hint；无 reconcile DTO、requestDigest/idempotency identity |
| `remote/run/RemoteTaskRunCoordinator.java` | public synchronized lifecycle owner | `confirmExecution(...)` 只校验 current ACTIVE/scope/window/revision，并写 `Map<String,Long> confirmedExecutionRevisionByTaskRunId`；exact retry 幂等，但任何 ACTIVE（包括 resume 后）都可走 raw confirm，且无法证明 local reconcile |
| `remote/CloudTaskRunExecutionGate.java` | package-private | context mint/send 最终依赖 coordinator authorize；可复用，但确认记录必须升级为 typed record |
| `remote/CloudTaskRunAuthorityAssembly.java` | package-private | 唯一 ledger/gate/executor assembly，context/runtime mint 也为 package-private；当前 gate 只看 revision confirmation，不知道 reconcile fact |

Cloud 当前 concurrent atomicity 仅覆盖“一次 Long map 写”；它没有 local fact 可原子绑定。更严重的是 DHXY 生产代码中
`RemoteTaskRunApiClient.confirmExecution(...)` 只有声明/HTTP 实现，**没有调用者**，所以当前 dormant context 不能凭可信
local resume evidence mint。

#### DHXY

| 文件/入口 | 当前 visibility / ownership | 当前行为与缺口 |
|---|---|---|
| `task/pause/TaskPauseResumeReconciler.java` | public Spring service，本地事实 owner | 读取 `WindowRuntimeContext` 缓存事实，不截图/OCR/输入；产出 matched 或 hot-start fallback，并执行既有 timer compensation/volatile cleanup |
| `model/pause/TaskPauseResumeReconcileResult.java` | public immutable Lombok value | 只有 decision、pauseBlockedMs、boolean、free-text mismatch、timer/clear 列表；无 typed mismatch、before/after digest、observation provenance、Cloud binding |
| `cloud/remote/RemoteTaskRunRegistry.java` | public process-local registration owner | stable `TaskPauseToken` 与 exact binding 在同一 entry；PAUSED->ACTIVE publish 会唤醒 token，但 entry 没有 local registration revision，也不保留 previous PAUSED snapshot |
| `cloud/remote/RemoteTaskRunLifecycleService.java` | public lifecycle orchestrator | `resume()` 先 Cloud RESUME，再 apply ACTIVE 到 registry 并唤醒；没有等待 reconcile result，也没有调用 confirm API |
| `cloud/remote/RemoteOperationLedger.java` | public local idempotency owner | 可知 request future 是否完成，但 entry 未保存 scope/taskRun/window/runRevision，不能证明旧 revision input/capture 已 quiescent |
| `cloud/remote/LocalRemoteGameCommandHandler.java` | public mechanical handler | side effect 前校验 local runRevision；但已开始的 input bundle 明确在 pause 后沿原 request 继续，故仅 registration ACTIVE 不能证明旧 input 已结束 |
| `cloud/remote/HttpRemoteTaskRunApiClient.java` | public HTTP adapter | strict response tree；lifecycle body 无 digest，现有 confirm request 也没有稳定 requestId |
| `cloud/remote/RemoteProtocolDigests.java` | public canonical digest helper | 只覆盖 remote mechanical request/outcome，不覆盖 lifecycle action body |

当前 gap 不能用“resume 已返回 ACTIVE”掩盖：ACTIVE publication、local reconcile、旧机械 operation drain、Cloud confirmation
是四个不同时间点。只有最后三项组成一个 exact fact 后，Cloud 才可确认新 revision。

### 2. 方案比较

| 方案 | 做法 | 优点 | 缺点/裁决 |
|---|---|---|---|
| **A（推荐）专用 lifecycle action + 单一 typed confirmation record** | 扩展现有 endpoint/request/digest；coordinator 保存一份 `ExecutionConfirmationRecord(kind=RESUME_RECONCILED,fact,request identity)`，其存在即 confirmation | 最小 authority surface；复用现有 route、monitor、gate、quota；无双写事实；不引入 general WAL | process-local，Cloud restart 后只能 fail-closed；符合本 slice 冻结边界 |
| B 独立 `/resume-facts` endpoint/service，再调用原 confirm | fact service 与原 coordinator 分开 | 文件表面隔离 | fact write/confirm 必然出现双写和 bypass；需要分布式事务或补偿，违反 requirement 3，不推荐 |
| C durable DB/WAL transaction + rehydration catalog | binding/fact/confirmation/phase/action 同一 durable transaction | 可支持 Cloud crash recovery、审计和跨进程接管 | 实际是完整 rehydration backend，扩大到 catalog/ledger/host；当前 slice 明确不做，后续独立 CR |

方案 A 不把内存记录包装成 crash durability。它只关闭同一 Cloud 进程中的 resume activation safety；重启时丢失
coordinator binding 与 confirmation，所有新 context mint 失败。

### 3. 最小 immutable reconcile fact

共享 wire 类型建议命名 `RemoteResumeReconcileFact`（Cloud 为 public record，DHXY 为 `@Value @Builder
@Jacksonized`，JSON 字段逐项同名）。不发送 runtime object、HWND/JNA object、token、Path、图片、OCR 文本、mutable map
或 timer/cleared-state 列表。

```text
factVersion = 1
scope = {tenantId,userId,deviceId,clientSessionId}
taskRunId
taskType                         // exact original text; equals binding.taskType
window = {windowId,nativeHandle,processId,playerIdentityEpoch}
stopEpoch
runRevision                     // new current ACTIVE revision
resumedFromRunRevision          // exact immediately preceding PAUSED revision
localRegistrationRevision       // local registry entry monotonic revision
localRegistrationStatus = ACTIVE
localPauseRevision              // TaskPauseToken pause generation, > 0
cumulativePauseNanos            // monotonic token snapshot, non-negative
decision = CONTINUE_ORIGINAL_PHASE | FALLBACK_TASK_HOT_START
mismatchCode                    // typed projection of existing branch, never Cloud-inferred
beforeFingerprintDigest         // lowercase SHA-256, non-zero
afterFingerprintDigest          // lowercase SHA-256, non-zero
beforeCapturedAtEpochMs
observedAtEpochMs
provenance = LOCAL_TASK_PAUSE_RESUME_RECONCILER_V1
operationLedgerRevision
inFlightWindowFactCount = 0
inFlightCaptureCount = 0
inFlightInputCount = 0
freshCapturePerformed = false
freshInputPerformed = false
```

- `mismatchCode` 精确枚举现有 `mismatchReason(...)` 分支：`MATCHED`、`MISSING_BEFORE`、`MISSING_AFTER`、
  `WINDOW_CHANGED`、`PHASE_CHANGED`、`ACTION_STATE_CHANGED`、`PREPARED_ACTION_MISSING/NEW/CHANGED`、
  `VISIBLE_DIALOG_MISSING/NEW/CHANGED`、`PATHING_CHANGED`。现有 free-text reason 原样保留给本地日志/业务 caller，不上 wire。
- before/after fingerprint digest 覆盖现有 fingerprint 的 canonical projection：taskType/taskCode/windowId/native handle
  decimal text/phase/waitReason/actionState/preparedAction/visibleDialog/pathing/capturedAtMs。Cloud 只看 digest，不复制或重算
  local reconcile algorithm。
- `noPause()` 不可产 resume fact。matched 与 fallback 都是可信 reconcile outcome，均可记录；future activation owner 必须
  按 decision 选择原 phase 或既有 hot-start，当前 slice 不实现该 owner。
- “no capture”精确定义为 reconciler 为这次 fact **未发起 fresh capture/OCR**，不是宣称全局没有只读 watcher。
  “no input”更严格：local remote operation ledger 必须证明该 taskRun 所有旧 revision 的 WINDOW_FACT/CAPTURE/INPUT
  request future 均 terminal；这样已开始并在 pause 后继续的 input sequence 已真实退出。

### 4. Wire、稳定 ID、digest 与 strict schema

新增 action `RECONCILE_CONFIRM_EXECUTION`，仍走现有 lifecycle route。outer request 精确字段：

```text
contractVersion, action,
tenantId, userId, deviceId, clientSessionId,   // gateway principal hint；必须等于 fact.scope
requestId, requestDigest,
reconcileFact
```

- `requestId` 是 DHXY producer 首次构造 pending handoff 时生成并保留的 UUID；transport timeout/reconnect 只能重发同一
  immutable request object，不能重建 ID、observation time 或 fact。
- `requestDigest` 使用两仓完全相同的 canonical integral JSON：serialize NON_NULL typed request，移除
  `requestDigest` 本身，按 object key 排序、UTF-8、整数原值、枚举字符串、禁止 float/binary，SHA-256 lowercase hex。
  digest 覆盖上表所有 outer 字段和 fact 的全部字段，因此 scope/window/stop/revision/registration/decision/quiescence
  任一变化都会改变 bytes/digest。
- endpoint raw schema 对该 action 只允许上述字段；嵌套 fact/window/scope 使用 FAIL_ON_UNKNOWN_PROPERTIES、
  FAIL_ON_NUMBERS_FOR_ENUMS、禁止 scalar coercion/float-as-int。Cloud 先重算 digest，再进 coordinator。
- success response 在现有 binding 外增加 `RemoteResumeReconcileConfirmation`：`requestId`、`requestDigest`、
  `factDigest`、`taskRunId`、`runRevision`、`acceptedAtEpochMs`。DHXY strict response 必须逐字段等于 pending request/current
  binding。
- coordinator key 是 taskRunId/current revision，record 保存 requestId + requestDigest + factDigest + immutable fact +
  accepted binding。相同 ID/bytes/digest 重试返回原 receipt；同 ID 不同 digest、同 revision 不同 ID、同 factDigest 但
  不同 request identity 均 `IDEMPOTENCY_CONFLICT`，绝不覆盖 first accepted record。

### 5. Cloud 原子 operation 与 context mint

coordinator 将 `Map<String,Long> confirmedExecutionRevisionByTaskRunId` 收窄为 one-per-run typed
`ExecutionConfirmationRecord`。initial activate 可记录 `kind=INITIAL_REGISTRATION`；resume 必须先由 `resume(...)` 留下
`ResumeConfirmationRequirement(resumedFromRevision,newActiveRevision)`，且旧 `CONFIRM_EXECUTION` 对 RESUME requirement
明确拒绝，关闭 raw-confirm bypass。

```text
synchronized reconcileAndConfirm(scope, requestId, requestDigest, fact):
  verify canonical request digest before monitor
  existing = executionConfirmationByRun[taskRunId]
  if existing matches same requestId + digest + factDigest:
      return existing receipt                 // even if lifecycle later advanced
  if existing is for same revision with any different identity/bytes:
      reject IDEMPOTENCY_CONFLICT

  current = exact binding visible only to exact scope
  require current.status == ACTIVE
  require current revision == fact.runRevision
  require pending requirement == (RESUME, fact.resumedFromRunRevision, fact.runRevision)
  require exact taskType/window/stopEpoch/scope/session
  require registrationStatus ACTIVE and localRegistrationRevision > 0
  require pauseRevision > 0, non-NO_PAUSE decision, valid fingerprint digests/times
  require all three inFlight counts == 0 and no fresh input/capture

  record = ExecutionConfirmationRecord(RESUME_RECONCILED, request identity, fact, current)
  executionConfirmationByRun.put(runId, record) // the single authority write
  return record.receipt
```

事实和 confirmation 不在两个 map 分别写；typed record 的存在就是 confirmation。`authorize(...)`、checkpoint classifier
和 `CloudTaskRunExecutionGate.createContext(...)` 都只承认 record.runRevision 等于 current ACTIVE revision。pause/resume/
stop/complete revision advance 后旧 record 自动失效。

`CloudTaskRunAuthorityAssembly` 无需新增 public API：现有 package-private `createTaskServiceRuntime(...)` 继续先走
package-private execution gate。gate 在 atomic record 不存在时拒绝；成功后才可 mint new immutable context 和 retained
action state。future activation owner 也必须位于该 package，business Task/Service 只拿 `TaskExecutionContext` 与 opaque
action handle，永远拿不到 endpoint、coordinator、ledger、request factory、raw fact writer 或 context mint method。
本 slice 不调用 assembly，不启动 host。

### 6. Validation / rejection matrix

| 输入/当前状态 | 结果 | mutation / retry 语义 |
|---|---|---|
| current ACTIVE + exact pending RESUME + exact fact + zero in-flight | ACCEPTED | 单次 put fact-backed confirmation record |
| PREPARED | `INVALID_STATE` | 无写；initial activation 走原 action，不伪造 resume |
| PAUSED | `INVALID_STATE` | 无写；乱序 confirm-before-resume 可在 resume 后重发同一 pending request |
| STOPPED / COMPLETED | `TERMINAL` | 无写、不可恢复 |
| stale fact revision `< current` | `STALE_REVISION` | 无写；即使旧 fact 曾 accepted，只有 exact duplicate receipt 可回放，不能确认 current |
| future fact revision `> current` | `FUTURE_REVISION` | 无写 |
| ACTIVE but current revision unconfirmed | 正是允许候选 | 只有本 atomic action可把它变成 confirmed；不能先 confirm |
| ACTIVE 已由 exact record confirmed | exact retry 返回原 receipt | different request/fact 为 conflict |
| scope tenant/user/device mismatch | `NOT_FOUND`-style redacted denial | 不泄漏 binding/fact |
| clientSession mismatch / replacement session | `SESSION_CONFLICT`/redacted denial | 原 session fact 不可接管；走既有 replacement STOP recovery |
| taskRunId/taskType/window tuple/stopEpoch mismatch | typed `BINDING_MISMATCH` | 无写；window 任一 handle/process/player epoch 不同均拒绝 |
| local registration status 非 ACTIVE、revision <=0、previous status 非 PAUSED | `REGISTRATION_MISMATCH` | 无写 |
| pending Cloud resume requirement 缺失或 fromRevision 不同 | `RECONCILE_NOT_EXPECTED` | 防止 initial ACTIVE/raw confirm 冒充 resume |
| noPause、digest invalid、fresh input/capture=true、任一 in-flight >0 | `INVALID_FACT` / `LOCAL_NOT_QUIESCENT` | 无写；DHXY 应在本地 quiescence 未满足时不发送 |
| identical requestId + bytes/digest/fact | 返回原 accepted receipt | 不重新写、不更新时间 |
| same ID/different bytes，或 same revision/new ID | `IDEMPOTENCY_CONFLICT` | first record 保持不变 |

### 7. 乱序、断线、timeout 与 restart

- **消息乱序**：fact 先于 RESUME 到达时 Cloud 仍 PAUSED，拒绝且无写；RESUME 后相同 immutable request 可重发。旧
  pause cycle 的 fact 因 fromRevision/runRevision/localRegistrationRevision 不匹配拒绝。STOP/COMPLETE 优先终结。
- **response lost / HTTP timeout**：client 将状态视为 UNKNOWN，不生成新 request；同进程只重发 exact bytes。若 Cloud
  已接受则返回原 receipt；若未接受则正常执行。没有自动 retry 次数/业务 fallback 变更。
- **disconnect/reconnect（同 clientSession）**：内存 pending request 保留时 exact retry；若 session ID 改变，视为
  replacement session，不能确认旧 run。
- **DHXY restart**：local registry、pending request 与 operation ledger 都不 durable；新 clientSession 无权重造 fact，
  只能走既有 replacement discovery/STOP，再启动新 run。
- **Cloud restart**：coordinator binding、resume requirement、fact-backed confirmation 同时丢失；endpoint 返回 not found，
  gate/context mint fail-closed。要支持 crash continuation 必须把 binding + transition requirement + confirmation fact +
  phase/action catalog 放同一 durable transaction，这是后续 backend slice，不在此伪装。
- **wall-clock age**：observation time只做审计，不设 TTL。revision/session/registration/digest 已封闭 replay；新增 TTL 会
  改变基线时序且受到时钟漂移影响。

### 8. DHXY fact producer，不改本地业务真相

现有 reconcile result 不足，采用精确 additive projection：

1. `TaskPauseResumeReconciler` 保留现有 `capture`、mismatch 顺序、timer compensation、volatile clear 和日志主体；只在
   同一分支同时生成 `TaskPauseResumeReconcileObservation`（typed mismatch、before/after canonical digest、timestamps、
   pause token revision/progress、provenance、freshCapture/input=false），挂到 result 新 optional 字段。旧 caller 继续只读
   原字段，行为不变。
2. `RemoteTaskRunRegistry.RegistryEntry` 新增 monotonic `localRegistrationRevision` 与 previous binding。首次 register=1；
   identical publication 不增；每个 accepted changed binding 增 1。只提供 package-private immutable resume snapshot，且
   要求 current ACTIVE、previous PAUSED、exact scope/window/stop/revision。
3. `RemoteOperationLedger` entry 加 powerless scope/taskRun/window/runRevision/operation identity 与 monotonic ledger
   revision；package-private snapshot 只报告该 run 所有未完成旧 revision operation count。它不取消、不等待、不执行输入。
4. 新 package-private `RemoteResumeReconcileConfirmService` 接受 explicit scope/taskRunId/observation：读取 exact registry
   snapshot -> 读取 ledger quiescence -> 再次验证同一 localRegistrationRevision 未变化 -> 构造并 retain immutable request
   -> 调 API。两次 registry 校验之间，resume 后新 revision 尚未 Cloud-confirm，无法产生新授权命令；旧 revision 新到
   request 即使 ledger claim，也会在 local revision pre-side-effect gate 拒绝。已在执行的旧 request 则必然被第一次
   ledger snapshot 计为 in-flight。
5. `RemoteTaskRunLifecycleService` 只新增 explicit `confirmResumedExecution(...observation)` orchestration method；现有
   `resume()`、三个业务 Task caller、pause token wait 和 hot-start分支均不自动联网、不改顺序。future remote pause
   controller 才可调用；当前 host/cohort dormant，因此本 slice 不产生运行副作用。

Cloud 不接收 raw mismatch reason、compensatedTimers、clearedVolatileState，也不重跑 fingerprint comparison。matched/
fallback 是 local reconciler 的既有真相；Cloud 只验证绑定与 provenance 后记录。

### 9. Tenant、capacity 与运维

- **隔离**：coordinator lookup 先 exact four-part scope；owner quotas 仍按 tenant/user/device，session 不可替换。日志不输出
  nativeHandle、fingerprint 内容、mismatch raw value、完整 tenant/user/device/clientSession；仅输出 action、status、
  revisions、windowId、request/digest/factDigest 前 8 位和 hashed owner correlation。
- **容量**：Cloud 每个 retained run 最多一个 current confirmation record + 一个 pending resume requirement，计入现有
  global 10,000 / owner retained 1,000 / owner non-terminal 64，不建独立无界 request ledger。terminal record 与 binding 同
  retention lifetime。DHXY 每个 registry entry 最多一个 pending request/observation，受 registry global/owner cap；同
  revision conflict 不追加历史。
- **metrics**：`resume_reconcile_confirm_total{outcome,reason}`、`resume_reconcile_retry_total{same,conflict}`、
  `resume_reconcile_local_quiescence_wait_ms`、`resume_reconcile_inflight_block_total{operation}`、
  `resume_reconcile_fact_age_ms`（观测，不做 gate）、`resume_reconcile_context_mint_denied_total{reason}`。
- **alerts**：tenant/session mismatch、idempotency conflict、future revision、confirmation without pending RESUME、capacity
  >80%、连续 local-not-quiescent、Cloud restart/not-found burst。不得自动 repair、takeover、重放 input 或跳 phase。
- **operator recovery**：先查 request/digest prefix、run/current/from/local registration revision 与 typed reject；不手工写
  map。same-process UNKNOWN 只允许 exact retry；任一进程重启或 session replacement 走 stop/restart run。需要跨重启
  恢复时必须先交付 durable backend，不允许 operator 伪造 fact。

### 10. 精确拟改文件、visibility 与 diff budget

#### Cloud Brain（4 new + 8 targeted modify，预计 `<=1,050` added lines）

| 文件 | visibility | budget | 责任 |
|---|---|---:|---|
| new `remote/RemoteResumeReconcileDecision.java` | public enum | <=25 | 两个 local baseline decision |
| new `remote/RemoteResumeMismatchCode.java` | public enum | <=45 | typed existing mismatch projection |
| new `remote/RemoteResumeReconcileFact.java` | public record | <=170 | exact immutable wire fact + validation |
| new `remote/RemoteResumeReconcileConfirmation.java` | public record | <=80 | immutable accepted receipt |
| modify `remote/RemoteTaskRunAction.java` | public enum | +1 | new action only |
| modify `remote/RemoteTaskRunActionRequest.java` | public record | +85/-0 | requestId/digest/fact accessors for new action |
| modify `remote/RemoteTaskRunActionResponse.java` | public record | +45/-0 | optional typed receipt with action-specific invariant |
| modify `remote/RemoteProtocolDigests.java` | public utility | +90/-0 | canonical lifecycle request/fact digest |
| modify `api/RemoteTaskRunEndpoint.java` | public inactive endpoint | +150/-0 | strict nested schema, digest check, typed error mapping |
| modify `remote/run/RemoteTaskRunCoordinator.java` | public lifecycle owner; private records/maps | +330/-20 | pending RESUME requirement, single fact-backed confirmation record, matrix/idempotency |
| modify `remote/CloudTaskRunExecutionGate.java` | package-private | +35/-5 | require exact typed confirmation record, no raw fact API |
| modify `remote/RemoteTaskRunErrorCode.java` | public enum | +6/-0 | typed conflict/stale/future/quiescence categories |

`CloudTaskRunAuthorityAssembly`、ledger、retained state、broker、command executor、routes、host、server、resources、pom、tests
保持 zero diff；assembly 通过已加固 gate 自然获得 mint fence。

#### DHXY（8 new + 11 targeted modify，预计 `<=1,650` added lines）

| 文件 | visibility | budget | 责任 |
|---|---|---:|---|
| new `model/pause/TaskPauseResumeMismatchCode.java` | public enum | <=45 | local typed projection |
| new `model/pause/TaskPauseResumeReconcileObservation.java` | public immutable value | <=120 | before/after digest、pause/provenance，不含 cloud authority |
| new `cloud/remote/RemoteResumeReconcileDecision.java` | public wire enum | <=25 | Cloud byte-equivalent enum |
| new `cloud/remote/RemoteResumeMismatchCode.java` | public wire enum | <=45 | Cloud byte-equivalent enum |
| new `cloud/remote/RemoteResumeReconcileFact.java` | public immutable wire DTO | <=190 | JSON field parity |
| new `cloud/remote/RemoteResumeReconcileConfirmation.java` | public immutable wire DTO | <=90 | strict receipt |
| new `cloud/remote/RemoteTaskRunRegistrationSnapshot.java` | package-private immutable value | <=90 | local registration revision + previous PAUSED binding |
| new `cloud/remote/RemoteResumeReconcileConfirmService.java` | package-private final | <=230 | exact snapshot/quiescence/request retention/API orchestration |
| modify `model/pause/TaskPauseResumeReconcileResult.java` | public immutable value | +20/-0 | optional additive observation |
| modify `task/pause/TaskPauseResumeReconciler.java` | public existing service | +120/-0 | typed code/canonical observation；保留现有主体 |
| modify `cloud/remote/RemoteTaskRunRegistry.java` | public owner; package-private snapshot API | +150/-10 | monotonic local registration revision/previous binding/double validation |
| modify `cloud/remote/RemoteOperationLedger.java` | public owner; package-private query | +130/-10 | retain operation fence and quiescence snapshot |
| modify `cloud/remote/RemoteTaskRunAction.java` | public enum | +1 | new action |
| modify `cloud/remote/RemoteTaskRunActionRequest.java` | public DTO | +30/-0 | request ID/digest/fact |
| modify `cloud/remote/RemoteTaskRunActionResponse.java` | public DTO | +20/-0 | receipt |
| modify `cloud/remote/RemoteProtocolDigests.java` | public utility | +100/-0 | lifecycle canonical digest parity |
| modify `cloud/remote/RemoteTaskRunApiClient.java` | package interface | +15/-0 | typed atomic call |
| modify `cloud/remote/HttpRemoteTaskRunApiClient.java` | public adapter | +170/-0 | stable request send + strict schema/receipt validation |
| modify `cloud/remote/RemoteTaskRunLifecycleService.java` | public orchestrator | +140/-0 | explicit dormant confirm method；现有 resume 不变 |

所有业务 Task、`TaskPauseToken`、`WindowRuntimeContext`、InputActionQueue/Worker、capture/OCR/input provider、poller/host/UI、
Maven/resources/tests、其它 docs/CR/dashboard 为 zero diff。若父级认为 19 个 DHXY 文件仍过宽，可把 wire DTO parity 与
producer 分成两个批准批次，但 atomic contract 不可删减 quiescence/registration revision 或退回双写。

### State sequence

```text
Cloud PAUSED r
  -> RESUME CAS
Cloud ACTIVE r+1, pendingRequirement=(RESUME, from=r, to=r+1), unconfirmed
  -> DHXY strict response + registry apply
DHXY local ACTIVE, localRegistrationRevision=g, previous=PAUSED r; stable token wakes
  -> existing TaskPauseResumeReconciler (unchanged decision order/side effects)
typed observation O
  -> exact registry snapshot g + remote operation ledger quiescence + exact registry recheck g
immutable request Q(requestId,digest,fact)
  -> HTTP timeout means UNKNOWN; retry Q byte-for-byte only
Cloud synchronized validate(Q) + one ExecutionConfirmationRecord put
  -> receipt C
Cloud gate sees current ACTIVE r+1 + exact RESUME_RECONCILED record
  -> future package-private activation owner may mint new context/retained state
  -> current slice: STOP, host/cohort remain dormant
```

### 批准后验证计划（本轮未执行）

```powershell
cd D:\mavenProject\dhxy-cloud-brain
mvn -q clean package

cd D:\mavenProject\DHXY
mvn -q -DskipTests compile

rg -n "RECONCILE_CONFIRM_EXECUTION|RemoteResumeReconcileFact|ExecutionConfirmationRecord" `
  D:\mavenProject\dhxy-cloud-brain\src\main\java `
  D:\mavenProject\DHXY\src\main\java
rg -n "TaskExecutionContextHolder|WindowRuntimeContext|TaskPauseToken|java.nio.file|Path|BufferedImage" `
  <两仓 RemoteResumeReconcileFact 文件>
rg -n "reason\(\)|contains\(|startsWith\(" `
  <Cloud coordinator/endpoint 新 classifier 段>
javap -classpath D:\mavenProject\dhxy-cloud-brain\target\classes -public `
  com.yueyunfe.dhxy.cloudbrain.remote.RemoteResumeReconcileFact `
  com.yueyunfe.dhxy.cloudbrain.remote.RemoteResumeReconcileConfirmation
git -C D:\mavenProject\dhxy-cloud-brain status --short -- pom.xml src/main/resources src/test
git -C D:\mavenProject\DHXY status --short -- pom.xml src/main/resources src/test
```

- Cloud package 必须无 skip、exit 0，记录现有 Surefire/JAR；DHXY 仅 compile with skip，不新增/恢复/运行 DHXY tests。
- 另做两仓 canonical fixture 的人工同字节/digest 对照、strict unknown-field 拒绝、public API/javap、source reachability、
  protected status/hash 扫描。因 no-local-test mode，不新增 source guard/unit/integration test。
- 不启动 application/server/host/poller/UI/capture/OCR/input，不做 Git mutation。Design #1 到此停止，等待父级
  `DESIGN APPROVED` 或 `BLOCKED`。

## External Worker - Owner Disposition #1 - 2026-07-12

- 归属披露：本文件在 19:36 认领后出现了两份内容相同的 `External Worker - Design #1`（第 72 行与第 446 行，
  仅差一个尾部空行，diff 已核）。两份均非认领者本人逐字撰写；按 `Local Process Note #1` 的裁定，认领者为本切片
  唯一设计 owner，故在此做出唯一权威指定，避免 parent 对着两份重复文本审查。
- **Owner 指定：以第 446 行起的副本为权威 `External Worker - Design #1`**（其位置在 parent 的
  `Local Read-only Evidence #1` 之后，内容与该证据一致）；第 72 行起的早先副本作废，视为重复噪音，请 parent 忽略。
- Owner 复核范围（这是 QA，不构成也不替代父级 review）：
  - 结构覆盖：全部 10 项 requirements + state sequence + 批准后验证计划齐备；
  - 第 1 节 inventory 与 parent `Local Read-only Evidence #1` 相互印证（`confirmExecution` 零调用者、
    resume 先 RESUME 后本地 publish、coordinator 仅一次 Long map 写）；
  - 第 5/6 节深读：fact-backed `ExecutionConfirmationRecord` 为唯一权威写入、`ResumeConfirmationRequirement`
    关闭 raw-confirm 旁路、exact retry 回放 receipt、拒绝矩阵覆盖 PREPARED/PAUSED/终态/stale/future/各类
    mismatch/重复与冲突重试，与冻结前提（旧 context 不复活、fail-closed、无 WAL）一致；
  - 第 2/9 节方案比较与推荐（方案 A 最小契约）符合 requirement 9 的"最小闭合"约束。
- 状态：设计材料已齐，按 Parent gate 停止，等待父级唯一的 `Local Design Review`（DESIGN APPROVED / BLOCKED）。
  Java/Maven/resources/tests 继续冻结；本轮未做任何代码、构建或 Git 操作。owner 保持 5 分钟 heartbeat 监控本文件。

## External Worker - Design Repair #1 - 2026-07-12

针对 `Local Design Review #1 - BLOCKED`（P0=0/P1=2/P2=1）逐条返修。仅设计，未改 Java/Maven/resources/tests/其它文档，无 Git 操作。除本节明确替换的内容外，权威 Design #1 的 coordinator 单 record 原子性、pending RESUME requirement、raw-confirm 旁路拒绝、exact identity 与 zero-in-flight 门全部保留（B-PASS 3）。

### R1（P1-1）：合同收窄为 executor readiness，剥离一切本地业务恢复决策

- 合同改名：wire action 由 `RECONCILE_CONFIRM_EXECUTION` 改为 **`CONFIRM_RESUMED_EXECUTOR_READY`**；DTO 由 reconcile fact 改为 **`ResumeExecutorReadinessFact`**（B-PASS 1）。
- fact 唯一允许字段（全部为机械/注册事实，无任何业务语义）：
  1. exact `scope`（tenant/user/device/clientSession）、`taskRunId`、`taskType`、window 四元组、`stopEpoch`；
  2. `resumedFromRunRevision` + `newActiveRunRevision`（必须等于 Cloud pending RESUME requirement 的 from/to）；
  3. `localRegistrationGeneration`、`localRegistrationStatus=ACTIVE`、`previousLocalStatus=PAUSED`（来自 registry entry 既有状态发布，不新增业务判断）；
  4. `pauseTokenMechanicalGeneration`（只读审计字段，经由 registry 已持有的 token 引用读取；`TaskPauseToken` 本身零修改）；
  5. `operationLedgerRevision` 与 `oldRevisionInFlightCounts=0/0/0`（capture/fact/input 三类，取自 operation-ledger 既有完成记账）；
  6. 机械 provenance：`observedAtEpochMs`、`producer=REGISTRY_RESUME_PUBLISH`、fact 字节的 canonical digest。
- **删除**原 §3/§8 的 `decision`、`mismatchCode`、before/after fingerprint digest、phase/actionState/preparedAction/visibleDialog/pathing/timer/clear 全部字段。Cloud confirmation record 不承载"从哪里继续"；该决策属于后续独立门的 Cloud-owned business checkpoint/rehydration owner。
- **不修改** `TaskPauseResumeReconciler`、`TaskPauseResumeReconcileResult`、pause model、任何业务 Task（拟改文件表相应行删除）。本地 reconciler 继续按现状服务本地运行的任务；本合同与其完全解耦。

### R2（P1-2）：纯机械、可达的 producer/trigger（不依赖任何业务 Task）

采用 parent 推荐路径，并写死触发与时序：

- **slot 创建（trigger 文件 1：`cloud/remote/RemoteTaskRunRegistry.java`）**：`applyConfirmedBinding(...)` 在发布 PAUSED->ACTIVE 转换的同一把 entry 锁内，创建 per-run 唯一 `PendingExecutorReadiness` slot（fromRevision=旧 PAUSED revision，toRevision=新 ACTIVE revision，created 状态=AWAITING_DRAIN）。同一 entry 已存在 slot 时按"新 revision 替换旧 slot"处理（旧 slot 标记 CANCELLED_SUPERSEDED）。
- **drain 判定与请求冻结（trigger 文件 2：`cloud/remote/RemoteOperationLedger.java`）**：ledger 在既有 `complete(claim,outcome)` 收尾处（已有同步边界内）检查"该 run 旧 revision 三类 in-flight 是否归零"；归零且 slot 为 AWAITING_DRAIN 时，一次性观测 fact 字段、构造 immutable request（稳定 requestId=UUID 一次生成、canonical digest 一次计算）、CAS 置 slot=READY_TO_SEND 并存入该 request 对象。若 resume 发布时 in-flight 本就为零，registry 在 slot 创建后立即做同一冻结（同一 entry 锁内，复用同一私有方法）。
- **发送（trigger 文件 3：`cloud/remote/RemoteCommandPollingLoop.java`）**：既有 polling 线程在每个 poll 周期边界（收到 IDLE/命令处理完毕后、下次 poll 前）检查 registry 暴露的 READY_TO_SEND slot 列表，同步调用 `RemoteTaskRunApiClient.confirmResumedExecutorReady(request)` 发送。**不新增线程/poller/定时器**；polling loop 本就是 lifecycle 存续期间的常驻机械传输线程。
- **同步方案裁决**：不选 `resume()` 内同步确认——旧 revision input bundle 明确可在 pause 后继续执行，resume 时 in-flight 常非零，同步确认将阻塞或必然失败，且会改变既有 resume 返回语义（违反 parent 的先证条件），故排除。
- **exact-byte retry**：网络 timeout/5xx 后，slot 保持 READY_TO_SEND 并在下个 poll 周期重交**同一 request 对象**（同 id/bytes/digest）；收到 Cloud 明确拒绝码时按矩阵处置（见 R3 清理表）。传输层不做指数退避以外的任何改写；重试永不重建对象。
- **并发时序**：slot 状态机 `AWAITING_DRAIN -> READY_TO_SEND -> CONFIRMED | CANCELLED_*`，全部转移在 registry entry 锁内完成；ledger 的 drain 回调只做"读计数 + 请求 registry 执行转移"，锁序固定为 ledger 内部锁先释放、再取 entry 锁，避免嵌套。
- **host dormant 门**：以上路径只在既有 remote lifecycle 已被显式启用（现状为 dormant，未启用）时才有流量；本 slice 不启用它，实现后行为不可观测，直至独立激活门。

### R3（P2-1）：pending request 唯一属主、容量与清理

- **唯一 owner**：`PendingExecutorReadiness` 是 registry entry 内的 package-private 字段（一个 entry 最多一个），不新建任何 service 级 map；`RemoteResumeReconcileConfirmService` 从拟改表删除（其职责并入 registry slot + polling 发送点，避免第二属主）。
- **容量**：slot 数 <= registry entry 数，registry 本身已有界；CONFIRMED 后仅保留 receipt 摘要（requestId+digest+Cloud receipt id）于同一 entry，同样 one-per-run。
- **清理转移表**：
  | 事件 | slot 处置 |
  |---|---|
  | Cloud accepted / exact duplicate receipt | -> CONFIRMED，清 request 对象，留 receipt 摘要 |
  | run STOP/COMPLETE/终态发布 | -> CANCELLED_TERMINAL，全清 |
  | 更新的 resume（更高 toRevision）发布 | 旧 slot -> CANCELLED_SUPERSEDED，新 slot 建立 |
  | session replacement / registry entry 移除 | 随 entry 一并清除 |
  | Cloud 拒绝：STALE/FUTURE_REVISION、BINDING/REGISTRATION_MISMATCH、SESSION_CONFLICT、TERMINAL | -> CANCELLED_REJECTED（保留拒绝码审计），不重试 |
  | Cloud 拒绝：IDEMPOTENCY_CONFLICT | 不可能由本 slot 产生（对象复用保证同 bytes）；出现即 CANCELLED_REJECTED + 告警日志 |

### R4：拟改文件表（替换 Design #1 §10 的 DHXY 部分）

- Cloud Brain：不变（4 new + 8 modify），仅 DTO/action 名按 R1 更名，record 字段按 R1 收窄。
- DHXY 收窄为 **2 new + 5 targeted modify**（原 8 new + 11 modify 作废）：
  - New：`cloud/remote/ResumeExecutorReadinessFact.java`（immutable wire DTO）、`cloud/remote/PendingExecutorReadiness.java`（package-private slot 类型）。
  - Modify：`RemoteTaskRunRegistry.java`（entry 内 slot 字段与转移）、`RemoteOperationLedger.java`（drain 回调）、`RemoteCommandPollingLoop.java`（周期边界发送）、`RemoteTaskRunApiClient.java` + `HttpRemoteTaskRunApiClient.java`（新增 `confirmResumedExecutorReady`，strict schema + digest 覆盖 lifecycle body）。
  - **授权披露**：`RemoteTaskRunRegistry`、`HttpRemoteTaskRunApiClient`、（间接读取的）`RemoteTaskRunLifecycleService` 属早前会话级禁碰类。本设计将前两者列入拟改集；实施必须以 parent 的 `DESIGN APPROVED` 显式列名为唯一授权来源，未列名则不碰。`TaskPauseToken` 与 `RemoteTaskRunLifecycleService` 保持零修改（后者的 resume 语义不变，slot 由 registry publish 触发）。
- diff 预算更新：Cloud `<=1,050` 行不变；DHXY 由 `<=1,650` 收窄至 **`<=700`** 行。

### R5（B-PASS 5）：成功语义声明

本合同成功仅证明 **local executor 对新 ACTIVE revision 可安全接令**（注册一致、旧 revision 机械操作已排空、无新输入/截图在途）。它不恢复、不选择、不暗示任何业务 phase/hot-start；在 Cloud-owned durable business checkpoint/rehydration owner 建立之前，host/Task/Service cohort 一律不得激活。

`无已批准业务差异；按基线等价迁移。` 本 worker 自审 P0=0/P1=0/P2=0，不构成父级批准；按 Parent gate 停止，等待唯一的 `Local Design Review`。owner 继续 5 分钟 heartbeat。

## Local Design Review #2 - BLOCKED - 2026-07-12

- 评审口径：本轮只审 `External Worker - Design Repair #1`，并以当前 DHXY/Cloud 源码验证其触发链与拟改文件是否
  可实现。R1 已正确删除 local continue/hot-start、mismatch、phase/action/dialog/pathing fingerprint，且不再拟改
  pause reconciler/model/业务 Task；one-per-run registry slot 的方向也成立。但实现闭包仍有
  **`P0=0 / P1=2 / P2=1`，BLOCKED，Java/Maven/resources/tests 继续冻结**。

### P1-1：机械 producer 的三段调用链在当前对象图中仍未闭合，按 R2/R4 不能实现

- 证据：当前 `RemoteTaskRunRegistry` 只有一个全局 `mutationLock`，不存在 Repair 所称的 entry lock，也没有
  `RemoteOperationLedger` collaborator；`RemoteOperationLedger` 只保存 `(operation, requestId) -> digest/future`，entry
  不保存 scope/taskRun/window/runRevision，`complete(...)` 只完成 future，没有 registry callback；
  `RemoteCommandPollingLoop` 构造器只有 command transport/handler/pollRequest，没有 registry、ledger 或 task-run API
  client。R2 却同时要求 registry 在 slot 创建时查询 zero-inflight、ledger complete 时回调 registry、poll loop 扫描并
  发送，而 R4 没有给出任何可形成这三条引用的 assembly/constructor/API 与锁边界。
- 额外故障证据：当前 poll loop 的所有异常都落入外层 `catch (Exception)` 并永久停止循环。Repair 只写“timeout/5xx 后
  下个周期重交”，没有写出局部 catch/typed outcome，因此按现有控制流一次 readiness-confirm timeout 就会杀死命令
  polling，所谓下一周期不存在。
- 影响：resume 时本就 zero-inflight 的 slot 无法冻结；已有旧 operation drain 后也没有可达通知；即使勉强接到 poller，
  一次 HTTP 故障还会停止本地机械 transport。合同仍不能稳定产生 confirmation，Service/Task cohort 永久 fail-closed。
- 返修条件：只选择一条真实可构造的链并列出精确 constructor/API。推荐取消 ledger->registry 反向 callback：registry 在
  现有 `mutationLock` 下只创建 `AWAITING_DRAIN`；ledger 在 claim 时保存完整 powerless run identity 并提供按 exact
  scope/taskRun/window/oldRevision 的 immutable quiescence snapshot；poll loop 明确注入 registry+ledger+task-run API，
  在 IDLE 和 command outcome 提交后的固定边界做“snapshot -> registry expected generation/ledger revision CAS freeze ->
  send”。readiness hook 必须有自己的 typed try/catch，timeout/5xx 只保留同一 slot/request，不得进入终止 poller 的外层
  catch。若坚持 callback 方案，则必须同样列出 callback owner、constructor wiring、锁序和 zero-at-create 路径，不能再靠
  未存在的 entry lock/collaborator。

### P1-2：R4 的跨仓拟改文件表自相矛盾且漏掉现有 wire 必改类，无法发送新 action

- 证据：R1 明确删除 `RemoteResumeReconcileDecision` 与 `RemoteResumeMismatchCode`，R4 却说 Cloud“4 new + 8 modify
  不变”；Design #1 的四个 new 中仍包含这两个已禁止 enum。DHXY R4 又缩成 2 new + 5 modify，但当前
  `RemoteTaskRunAction` 没有 `CONFIRM_RESUMED_EXECUTOR_READY`，`RemoteTaskRunActionRequest` 没有 requestId/digest/fact，
  `RemoteTaskRunActionResponse` 没有 typed receipt，`RemoteProtocolDigests` 也只有机械 command/outcome digest；这四个
  现有类均未列入 Repair 写集。Cloud/DHXY confirmation receipt 的 wire 类型也未在新表中明确对应。
- 影响：若按 R4 的“批准文件”实现，新增 API client 方法既构造不出 action/fact body，也无法做同字节 digest 或解析
  receipt；若实现者自行恢复旧表，则会重新引入已禁止的 decision/mismatch 类型并越过父级授权。
- 返修条件：用一张完整的新 Cloud 表和一张完整的新 DHXY 表**整体替换** Design #1 §10/R4，不得写“不变”。逐个列出
  action enum、request/response、fact/receipt、canonical digest、strict endpoint/schema、coordinator/gate、registry/
  ledger/poller/client 及必要 constructor/assembly 文件；删除 decision/mismatch 文件。字段级给出双仓一一对应，确保
  requestId/digest/fact/receipt 的 canonical bytes、unknown-field 拒绝与 retry 同对象合同可实现。

### P2-1：slot 结果回写与发送容量缺少 stale-result fence，可能污染新 revision 或饿死 command poll

- 证据：Repair 允许 READY request 脱锁做同步 HTTP，同时 terminal/new resume/session replacement 可清除或替换 slot；
  但没有要求 response 回写时再次比较 slot generation/requestId/toRevision。它还说每个 poll 边界“检查 READY_TO_SEND
  slot 列表”，registry global capacity 为 10,000，未给 exact session 过滤、每周期发送上限或失败退避；串行 timeout 可
  长时间阻塞正常 command poll。
- 影响：旧请求迟到 receipt 可能把已 supersede 的新 slot 标为 CONFIRMED；大量 ready/timeout 请求可能让截图/输入命令
  无法被及时 poll，形成运维级饥饿。
- 返修条件：读取 request 时返回 immutable `(entryGeneration, slotGeneration, requestId, toRevision, request)` handle；
  receipt/reject 只在 exact handle 仍 current 时 CAS 回写，否则丢弃为 stale audit。poller 只处理与自身四元 scope/session
  匹配的 bounded batch（建议每周期 1 条），readiness HTTP 设明确 bounded timeout；failure 保留同一对象并采用机械发送
  节流，但不得改变业务 retry/fallback，也不得阻断下一次 command poll。

### Design Repair #2 的 B-PASS 门

1. 保留 R1 的纯 executor-readiness 边界，不恢复任何 local business decision/reconciler/Task 拟改。
2. 给出当前对象图可构造的唯一 producer 链、真实锁名、完整 identity ledger snapshot 与 poller 局部故障隔离。
3. 完整重列双仓精确文件表和 field/schema/digest parity，删除所有已作废 enum/file，不能靠实现者猜漏项。
4. 写清 slot handle stale-result CAS、exact-session bounded send 与 command-poll starvation 防护。
5. 保留 pending RESUME + one atomic confirmation record + raw-confirm bypass 拒绝 + exact binding/revision/zero-inflight；
   host/cohort/business rehydration 继续 dormant/后置。
6. 同一 worker 下一条只追加 `External Worker - Design Repair #2 - 2026-07-12`；仍不得改 Java/Maven/resources/tests、
   其它文档或 Git。

`无已批准业务差异；按基线等价迁移。`

## External Worker - Design Repair #1 - 2026-07-12

### 返修结论

- 合同正式收窄并改名为 **local executor readiness + atomic execution confirmation**。本地只证明 exact remote
  executor registration 已发布到新的 ACTIVE revision，且旧 revision 的 remote mechanical operations 全部 terminal；
  不提供、携带或暗示任何业务 continuation 决策。
- 保留权威 Design #1 中已获 review 认可的四项：Cloud coordinator one-record atomicity、pending RESUME requirement、
  raw `CONFIRM_EXECUTION` 对 resumed revision 的 bypass 拒绝、same-byte idempotent retry；其余业务 reconcile 内容全部
  由本 Repair 替换。
- 本版设计自审 `P0=0 / P1=0 / P2=0`，无剩余设计 blocker。Java 仍冻结；本段不构成父级批准。
  `无已批准业务差异；按基线等价迁移。`

### 1. Pure mechanical fact

新 action 固定为 `CONFIRM_RESUMED_EXECUTOR_READY`；共享 DTO 固定为 `RemoteExecutorReadinessFact`。fact 只含：

```text
factVersion = 1
scope = { tenantId, userId, deviceId, clientSessionId }
taskRunId
taskType
window = { windowId, nativeHandle, processId, playerIdentityEpoch }
stopEpoch
resumedFromRunRevision          // exact previous Cloud/local PAUSED revision
newActiveRunRevision            // exact current Cloud/local ACTIVE revision
localRegistrationGeneration     // registry-entry mechanical generation
localRegistrationStatus = ACTIVE
previousLocalRegistrationStatus = PAUSED
pauseTokenMechanicalGeneration  // audit only; no pause duration/business meaning
operationLedgerRevision
inFlightWindowFactCount = 0
inFlightCaptureCount = 0
inFlightInputCount = 0
observedAtEpochMs               // audit only; no TTL gate
provenance = DHXY_REMOTE_EXECUTOR_REGISTRY_V1
```

明确禁止字段：`CONTINUE_ORIGINAL_PHASE`、`FALLBACK_TASK_HOT_START`、decision、mismatch/reason、fingerprint、phase、
actionState、preparedAction、visibleDialog、pathing、objective、timer compensation、volatile clear、business retry/fallback、
图片/OCR/Path/runtime/token/raw map。`pauseTokenMechanicalGeneration` 只是 registry 已持有 token 的 generation 数值快照，
Cloud 不据此推导 pause 时长或业务状态。

以下全部从拟改集删除并 zero diff：`TaskPauseResumeReconciler`、`TaskPauseResumeReconcileResult`、全部
`model/pause`、Wuhuan/Wubei/Xiuluo 与其它业务 Task。Cloud-owned checkpoint/phase/hot-start 决策不读取本 fact。

### 2. 可达 mechanical producer 与明确 transport hooks

producer 不依赖本地业务 Task，固定链路如下：

```text
Cloud RESUME: PAUSED r -> ACTIVE r+1 (unconfirmed, pending RESUME requirement)
  -> DHXY RemoteTaskRunLifecycleService receives strict ACTIVE binding
  -> RemoteTaskRunRegistry.applyConfirmedBinding publishes exact ACTIVE(previous PAUSED)
  -> same registry mutation creates one AWAITING_DRAIN slot in that RegistryEntry
  -> existing RemoteCommandPollingLoop reaches a readiness hook
  -> pump reads old-revision quiescence from RemoteOperationLedger
  -> registry revalidates same entry/generation/revision and freezes one retained request
  -> polling loop sends it through existing HttpRemoteTaskRunApiClient
  -> Cloud atomically accepts fact-backed confirmation record
```

#### Hook A：IDLE poll boundary

在 `RemoteCommandPollingLoop.runLoop()` 中，`transport.poll(...)` 返回 `IDLE` 后、`waitForNextPoll(...)` 前调用
`flushExecutorReadinessSafely("idle-poll")`。这覆盖 resume 时本来就没有旧 operation，以及上次 confirm HTTP timeout 后
没有新 command 可触发的 exact retry。

#### Hook B：operation terminal boundary

在同一 `runLoop()` 中，`handler.handle(command)` 返回、outcome correlation 已校验且
`transport.submitOutcome(outcome)` 成功后，调用 `flushExecutorReadinessSafely("outcome-submitted")`。此时
`LocalRemoteGameCommandHandler` 已执行 `RemoteOperationLedger.complete(...)`；因此刚结束的旧 input/capture/fact future
已经 terminal。hook 在 outcome 提交后运行，不延迟或重排该 outcome。

`flushExecutorReadinessSafely` 每次最多 claim/send 一个 slot，并在内部捕获 readiness pump 的所有 transport/schema/
runtime exception：记录 typed warning/metric 后返回 poll loop。confirm HTTP timeout 可以阻塞至现有 lifecycle HTTP client
的 bounded timeout，但异常不得越过 helper、不得写 `lastFailure`、不得终止 polling loop；slot 回到 READY，下一次既有
poll boundary 重交同一对象。这里的重交是 wire idempotency convergence，不是业务 retry，不产生新线程、poller、timer、
input、capture 或 OCR。

`RemoteCommandPollingLoop` 继续只有原来的显式 daemon thread；readiness pump 是无线程、无 map、无 scheduler 的
package-private collaborator。poller/remote lifecycle 仍 dormant，当前不启动。

### 3. Registry-entry 唯一 owner、锁与稳定对象

#### 唯一 owner

`RemoteTaskRunRegistry.RegistryEntry` 在既有 `mutationLock` 保护下新增唯一 package-private
`PendingExecutorReadiness pendingReadiness`。任何 service/pump/transport 均不得保存第二份 pending map/list；pump 只拿
短生命周期 snapshot/claim。slot 数严格 `<= registry entries`，受现有 global/owner capacity 限制。

#### slot 状态机

```text
null
  -> AWAITING_DRAIN(requestId, exact binding, localGeneration)
  -> READY(retainedSend)
  -> SENDING(retainedSend)
  -> null                    // exact accepted/duplicate receipt

AWAITING_DRAIN|READY|SENDING
  -> null                    // terminal, newer revision, session replacement/removal

SENDING -> READY             // timeout/transport unknown; SAME retainedSend
READY|SENDING -> null        // explicit permanent typed rejection
```

- PAUSED->ACTIVE changed publication increments `localRegistrationGeneration` and creates slot with one UUID requestId。相同
  ACTIVE snapshot idempotent publication既不增 generation，也不替换 slot。
- pump 先从 registry 取 AWAITING candidate（无锁 snapshot），再在 ledger monitor 内取得 exact quiescence snapshot；
  两把锁绝不嵌套。随后回到 registry `mutationLock`，重新验证 entry、scope/session、current ACTIVE、previous PAUSED、
  from/to revision、window/stop/taskType、local generation 与 slot identity 均未变化，才 materialize。
- materialize 只执行一次：构造 immutable fact/request，计算 digest，并通过同一 ObjectMapper 把最终 body 序列化为
  exact UTF-8 JSON `String`。`PendingExecutorReadiness.RetainedSend` 保存 request object、exact body、requestId、digest；
  后续 timeout 不重新读取时钟/ledger/registry，不重新序列化、不重建 ID/digest/bytes。
- `claimReadyForSend` 在 `mutationLock` 内 CAS-like `READY -> SENDING` 并返回同一 retained object；并发 IDLE/outcome hook
  只有一个能 claim。HTTP 在锁外执行。
- exact success 或 Cloud `IDEMPOTENT_REPLAY` receipt 且 requestId/digest/factDigest 全相等：清 slot。timeout、连接断开、
  5xx/UNKNOWN：`SENDING -> READY`，保留同一 object。明确 permanent reject（terminal/session/binding/stale/future/
  idempotency conflict）：清 slot并告警，不自动生成新请求。
- PAUSE、下一次 RESUME/new revision、STOP、COMPLETE、terminal release、replacement-session STOP、registry entry remove
  都在 `mutationLock` 内先清旧 slot。clientSession replacement 无权读取旧 entry slot。

不保留独立 CONFIRMED local history；Cloud accepted record 是确认权威，local 收到 exact receipt 后释放 request object，
避免 per-run 残留。

### 4. Zero-inflight 与 mid-bundle 不变量

`RemoteOperationLedger.LedgerEntry` 增加 immutable mechanical fence：scope/session、taskRunId、window tuple、stopEpoch、
runRevision、operation；ledger revision 在 claim/terminal completion 时单调增加。read-only quiescence snapshot 对 exact run
统计所有 `runRevision < newActiveRunRevision` 且 future 未 terminal 的 WINDOW_FACT/CAPTURE/EXECUTE_INPUT_BUNDLE。

- 任一 count 非零：slot 保持 AWAITING_DRAIN，不 materialize、不发送、不 confirm。
- 已开始 input bundle 保持现有语义：pause token 可让同一 request 继续 mid-bundle；本设计不 cancel、interrupt、重排、
  截短或重放它。只有其 handler outcome 已冻结、ledger future 已 terminal 后，inFlightInputCount 才变为 0。
- resume 发布后，新 ACTIVE revision 仍 Cloud-unconfirmed，Cloud gate 不会 mint context/发送新 command；晚到旧 revision
  command 即使先 claim ledger，也会被 DHXY existing pre-side-effect runRevision gate 拒绝。pump 的二阶段
  ledger-snapshot + registry-revalidation 防止把已存在的旧 in-flight 漏掉。
- readiness 不读取窗口截图内容、dialog/pathing或业务缓存。CAPTURE count 归零只说明旧 remote capture handler 已 terminal，
  不解释截图结果。

### 5. Wire/digest 与 Cloud 单 record 原子门

outer request 字段固定为：

```text
contractVersion, action=CONFIRM_RESUMED_EXECUTOR_READY,
tenantId, userId, deviceId, clientSessionId,
requestId, requestDigest, executorReadinessFact
```

两仓 canonical digest 覆盖 outer（除 requestDigest 自身）和 fact 全字段：NON_NULL typed tree、object key 排序、UTF-8、
integral numbers、enum strings、禁止 float/binary/scalar coercion。endpoint strict unknown-field/nested-field gate 后重算 digest。

Cloud `resume(...)` 在 synchronized transition 中创建 exact
`PendingResumeRequirement(scope,taskRunId,taskType,window,stopEpoch,fromRevision,toRevision)`。execution confirmation 从
`Map<String,Long>` 改为 one-per-run `ExecutionConfirmationRecord`；对 resume，record 内含 immutable readiness fact、
requestId/digests、accepted binding/receipt，**record 本身就是 confirmation**，不存在 fact map + confirmed map 双写。

```text
synchronized confirmResumedExecutorReady(request):
  existing exact requestId+digest+factDigest -> return original receipt
  existing same run/revision but different identity/bytes -> IDEMPOTENCY_CONFLICT
  require current binding exists in exact four-part scope/session
  require current ACTIVE and pending RESUME requirement exists
  require fact taskRun/taskType/window/stopEpoch/from+to == requirement/current
  require local generation > 0, local ACTIVE, previous PAUSED
  require all three inFlight counts == 0 and provenance/version exact
  put one ExecutionConfirmationRecord(RESUMED_EXECUTOR_READY, fact, request identity)
  return receipt
```

旧 `CONFIRM_EXECUTION` 只允许 pending requirement kind `INITIAL_ACTIVATION`；若当前 revision requirement 是 RESUME，必须
`RECONCILE_REQUIRED` 拒绝，不能绕过 readiness fact。pause/next resume/stop/complete 使旧 confirmation record revision 与
current 不等，从而失效。

`authorize(...)`、checkpoint classifier、`CloudTaskRunExecutionGate.createContext(...)` 只读取这一份 current exact
accepted record。`CloudTaskRunAuthorityAssembly` 保持 package-private 且 zero diff；business code 仍拿不到 endpoint、
coordinator、ledger、pump、request factory、fact writer 或 context mint API。

### 6. Validation / ordering / disconnect matrix

| 场景 | Cloud/local 结果 | retry/cleanup |
|---|---|---|
| exact ACTIVE + pending RESUME + exact readiness + zero inflight | ACCEPT | one record put；exact receipt 清 local slot |
| PREPARED / PAUSED | INVALID_STATE | 无 Cloud 写；local slot 保留仅当 exact transition仍可能收敛 |
| STOPPED / COMPLETED | TERMINAL | 无写；local slot 清除 |
| stale toRevision | STALE_REVISION | 无写；slot permanent clear |
| future toRevision | FUTURE_REVISION | 无写；slot clear + alert |
| scope/tenant/user/device 不同 | redacted NOT_FOUND | 不泄漏 binding；slot clear |
| clientSession/replacement session 不同 | SESSION_CONFLICT | 不 takeover；走既有 replacement STOP |
| taskRun/taskType/window/stopEpoch/fromRevision 不同 | BINDING_MISMATCH | 无写；slot clear |
| local generation/status/previous status 不符 | REGISTRATION_MISMATCH | 无写；slot clear |
| 任一 in-flight >0 | local 不发送；Cloud 亦拒绝 LOCAL_NOT_QUIESCENT | AWAITING_DRAIN，等 operation terminal hook |
| exact same request/bytes/digest accepted 后重交 | original receipt | 无第二写 |
| same ID different bytes；same revision new ID | IDEMPOTENCY_CONFLICT | first record不变；slot clear + alert |
| readiness request 先于 Cloud RESUME | RECONCILE_NOT_EXPECTED | 无写；正常链不会 materialize；乱序永久拒绝 |
| response lost/timeout/5xx | UNKNOWN | SENDING->READY；下个既有 poll hook重交同对象 |
| same-session reconnect | exact retry | retained slot/object不变 |
| DHXY restart | slot/registry/ledger丢失 | 新 session不能重造；replacement STOP/new run |
| Cloud restart | binding/requirement/record全丢 | NOT_FOUND、mint fail-closed；需 stop/new run |

observation time 只用于日志，不设 TTL。消息乱序由 exact from/to revision、session 和 pending requirement 关闭；不增加业务
timeout、fallback 或 cleanup。

### 7. Threat、tenant、capacity 与 ops

- **tenant/session**：Cloud 先 exact four-part scope lookup；不匹配返回 redacted denial。DHXY pump 只处理 polling loop 固定
  `pollRequest` scope 下的 registry entries，不跨 session 扫描/发送。
- **forgery/replay**：strict schema + canonical digest + exact pending RESUME + one-per-revision first-writer record；旧 revision
  replay不确认 current。scope 仍为 dev bearer hint，production authentication middleware 是独立 activation prerequisite。
- **capacity**：DHXY slot <= registry entry cap，无旁路 map；poll hook 每次最多处理一个，避免一次 IDLE 扫描长时间占用。
  Cloud requirement + confirmation 各最多 one per retained run，沿用 global 10,000 / owner 1,000 / non-terminal 64。
- **logging/redaction**：只记录 action/status/from/to/local generation/counts、windowId、request/digest 前8位和 hashed owner；
  不记录 nativeHandle、完整 scope/session、exact JSON body。metrics：ready-created、blocked-by-operation、send-attempt、
  exact-replay、permanent-reject、poll-hook-failure、context-mint-denied。
- **alerts**：idempotency conflict、future revision、session mismatch、raw-confirm resume bypass、slot age（观测）、连续 HTTP
  timeout、capacity 80%、Cloud restart NOT_FOUND burst。operator 不手写 map、不伪造 fact、不强行 confirm；UNKNOWN 仅
  same-object retry，restart/session replacement 走 stop/new run。
- **crash consistency**：两侧均为 process-local。要跨重启恢复，必须把 lifecycle binding、pending RESUME、readiness record
  与后续 business checkpoint/action catalog 放入 durable transaction；本 slice 不宣称 durability。

### 8. 精确拟改文件 / visibility / diff budget

#### Cloud Brain：2 new + 8 targeted modify，预计 `<=900` added lines

| 文件 | visibility | budget | 内容 |
|---|---|---:|---|
| new `remote/RemoteExecutorReadinessFact.java` | public record | <=130 | pure mechanical wire fact |
| new `remote/RemoteExecutorReadinessConfirmation.java` | public record | <=75 | exact receipt |
| modify `remote/RemoteTaskRunAction.java` | public enum | +1 | new action |
| modify `remote/RemoteTaskRunActionRequest.java` | public record | +70/-0 | requestId/digest/readiness fact |
| modify `remote/RemoteTaskRunActionResponse.java` | public record | +40/-0 | action-specific receipt |
| modify `remote/RemoteProtocolDigests.java` | public utility | +85/-0 | lifecycle canonical digest |
| modify `api/RemoteTaskRunEndpoint.java` | public inactive endpoint | +145/-0 | strict schema/digest/action dispatch/errors |
| modify `remote/run/RemoteTaskRunCoordinator.java` | public owner; private records | +310/-20 | pending requirement + one confirmation record + matrix |
| modify `remote/CloudTaskRunExecutionGate.java` | package-private | +30/-5 | exact current record check |
| modify `remote/RemoteTaskRunErrorCode.java` | public enum | +8/-0 | stable typed rejection codes |

#### DHXY：4 new + 9 targeted modify，预计 `<=1,100` added lines

| 文件 | visibility | budget | 内容 |
|---|---|---:|---|
| new `cloud/remote/RemoteExecutorReadinessFact.java` | public immutable wire DTO | <=150 | Cloud field parity |
| new `cloud/remote/RemoteExecutorReadinessConfirmation.java` | public immutable wire DTO | <=85 | strict receipt |
| new `cloud/remote/PendingExecutorReadiness.java` | package-private final | <=150 | slot/state/retained exact body owner value |
| new `cloud/remote/RemoteExecutorReadinessPump.java` | package-private final, no thread/map | <=180 | ledger snapshot, registry revalidate/claim, one send |
| modify `cloud/remote/RemoteTaskRunRegistry.java` | public owner; package-private slot APIs | +190/-10 | local generation, previous binding, slot state/cleanup |
| modify `cloud/remote/RemoteOperationLedger.java` | public owner; package-private query | +120/-5 | operation fence/revision/quiescence counts |
| modify `cloud/remote/RemoteCommandPollingLoop.java` | public explicit loop | +65/-0 | IDLE/outcome hooks + fail isolation |
| modify `cloud/remote/RemoteTaskRunAction.java` | public enum | +1 | new action |
| modify `cloud/remote/RemoteTaskRunActionRequest.java` | public DTO | +30/-0 | request identity/fact |
| modify `cloud/remote/RemoteTaskRunActionResponse.java` | public DTO | +20/-0 | receipt |
| modify `cloud/remote/RemoteProtocolDigests.java` | public utility | +95/-0 | lifecycle canonical digest parity |
| modify `cloud/remote/RemoteTaskRunApiClient.java` | package interface | +12/-0 | retained atomic call |
| modify `cloud/remote/HttpRemoteTaskRunApiClient.java` | public adapter | +150/-0 | exact body send + strict response validation |

#### Explicit zero diff

- DHXY：`TaskPauseResumeReconciler`、`TaskPauseResumeReconcileResult`、全部 pause model/业务 Task、`TaskPauseToken`、
  `RemoteTaskRunLifecycleService.resume(...)`、`WindowRuntimeContext`、LocalRemoteGameCommandHandler、InputActionQueue/Worker、
  capture/OCR/input provider、host/config/UI、pom/resources/tests、其它 docs/CR/dashboard。
- Cloud：`CloudTaskRunAuthorityAssembly`、action ledger、retained state、broker、command executor、routes、host/server、pom/
  resources/tests、全部 business Task/Service。

### 9. 批准后验证计划（本轮不执行）

```powershell
cd D:\mavenProject\dhxy-cloud-brain
mvn -q clean package

cd D:\mavenProject\DHXY
mvn -q -DskipTests compile

rg -n "CONTINUE_ORIGINAL_PHASE|FALLBACK_TASK_HOT_START|mismatch|fingerprint|phase|preparedAction|visibleDialog|pathing|compensatedTimers|clearedVolatileState" `
  <两仓 RemoteExecutorReadinessFact/Confirmation 与 Cloud confirmation record 文件>
rg -n "CONFIRM_RESUMED_EXECUTOR_READY|PendingExecutorReadiness|flushExecutorReadinessSafely|ExecutionConfirmationRecord|PendingResumeRequirement" `
  D:\mavenProject\DHXY\src\main\java `
  D:\mavenProject\dhxy-cloud-brain\src\main\java
rg -n "new Thread|ExecutorService|Scheduled|Timer|TaskPauseResumeReconciler" `
  <readiness 拟改文件>
javap -classpath D:\mavenProject\dhxy-cloud-brain\target\classes -public `
  com.yueyunfe.dhxy.cloudbrain.remote.RemoteExecutorReadinessFact `
  com.yueyunfe.dhxy.cloudbrain.remote.RemoteExecutorReadinessConfirmation
git -C D:\mavenProject\dhxy-cloud-brain status --short -- pom.xml src/main/resources src/test
git -C D:\mavenProject\DHXY status --short -- pom.xml src/main/resources src/test `
  src/main/java/com/bot/dhxy/task src/main/java/com/bot/dhxy/model/pause
```

- 人工核对两仓 exact request JSON/body/digest fixture、strict unknown-field rejection、duplicate/conflict matrix、slot
  state transitions、poll hook exception containment、source reachability 与 protected hashes。按 no-local-test mode 不新增/
  恢复/运行 DHXY tests。
- 本 slice 成功只关闭 **local executor ready + atomic current-revision confirmation**。Cloud durable business checkpoint、
  phase/action rehydration、continuation choice 与 activation owner 仍是独立后续门；未完成前 host/Task/Service cohort 必须
  dormant。
- 本轮未运行 Maven/测试/应用/server/host/poller/UI/capture/OCR/input，未做 Git mutation。Design Repair #1 到此停止，
  等待父级 `DESIGN APPROVED` 或 `BLOCKED`。

## Local Process Note #2 - Concurrent append reconciliation - 2026-07-12

- `Local Design Review #2` 写入时，worker 的第一段、短版 `Design Repair #1` 已出现停止语句，parent 因而按当时文件
  EOF 复审；随后 worker 又在该 review 后追加了第二段、完整 `Design Repair #1`。因此 Review #2 对“producer/file-table
  不闭合”的 P1 结论只适用于短版中间态，现标记为 **superseded / non-authoritative**，不删除历史。
- 以下 `Local Design Review #3` 以第二段完整 Repair（从第二个同名 heading 到其明确停止语句）为最新有效材料；后续
  worker 不得再续写旧 heading，只能按最新 review 追加 `External Worker - Design Repair #2`。

## Local Design Review #3 - BLOCKED - 2026-07-12

- 完整 Repair 已关闭 Review #2 的两个中间态 P1：它改用现有 registry `mutationLock` + 新无状态 pump，ledger 保存
  exact run identity 并提供 quiescence snapshot，poller 在 IDLE/outcome 边界每次最多发送一条且局部捕获 confirm 故障；
  双仓 action/request/response/fact/receipt/digest/endpoint/coordinator/gate/client 文件表也已完整重列并删除
  decision/mismatch。纯 executor-readiness、pending RESUME、one-record confirmation、raw-confirm bypass 拒绝、旧 operation
  drain 与 host/business rehydration 后置均成立。
- 最新结论：**`P0=0 / P1=0 / P2=1`，BLOCKED，Java/Maven/resources/tests 继续冻结**。

### P2-1：锁外 HTTP 的迟到结果没有 exact slot-handle 回写门，可能清除下一 resume 的新 slot

- 证据：完整 Repair 定义 `READY -> SENDING` 后在 registry lock 外发 HTTP，同时 PAUSE/new RESUME/STOP/session
  replacement 可在锁内清旧 slot、建立新 slot；但 success/permanent-reject/timeout 的回写只写“清 slot”或
  `SENDING -> READY`，未要求携带并比较 `(entryGeneration, slotGeneration, requestId, toRevision, retainedSend)`。
- 时序：slot A(r+1) 已 SENDING -> Cloud 接受但 response 延迟 -> 本地 PAUSE/RESUME 建 slot B(r+3) -> A 的 success 或
  reject 返回。若回写只按 taskRunId/current slot 操作，会把 B 清除或改回 READY。Cloud 的 exact revision 门仍会阻止 A
  越权，所以这是 P2 可用性/收敛缺口，不是 P0/P1 authority bypass；但 B 丢失后 r+3 永远无法确认。
- 返修条件：`claimReadyForSend` 必须返回 immutable `PendingSendHandle(entryGeneration, slotGeneration, requestId,
  toRevision, retainedSend)`；`markAccepted`/`markUnknownForRetry`/`markPermanentRejected` 全部在 `mutationLock` 下只对
  exact current handle 做 CAS-like 转移。handle 已 stale 时仅记录 redacted stale-result metric/log，绝不触碰 current slot。
  table/API/状态机需明确 slotGeneration 单调来源及 session removal 后 handle 永不匹配。

### Design Repair #2 的 B-PASS 门

1. 只补 exact send-handle/result CAS 与 stale-result 丢弃；不得重开已关闭的 producer/wire/business 范围。
2. 保持每 poll boundary 最多一条、exact session、bounded timeout、同一 bytes/id/digest 与局部异常隔离。
3. 更新状态机、并发时序与 DHXY `PendingExecutorReadiness`/registry/pump API；拟改文件集不扩大。
4. 同一 worker 只追加 `External Worker - Design Repair #2 - 2026-07-12`，写完停止；父级批准前仍不得改 Java。

`无已批准业务差异；按基线等价迁移。`

## External Worker - Design Repair #2 - 2026-07-12

针对 `Local Design Review #2 - BLOCKED`（P0=0/P1=2/P2=1）逐条返修；仅设计，未改 Java/Maven/resources/tests，无 Git。归属说明：Review #2 与本节之间如存在任何再次以 "Design Repair #1" 为题的条目，均不属于本 owner 的应答材料，以 Review #2 的裁定与本 Repair #2 为准。B-PASS 1/5 先声明：R1 的纯 executor-readiness 边界原样保留——不携带任何 decision/mismatch/fingerprint/phase/dialog/pathing 字段，不拟改 pause reconciler/model/业务 Task；合同成功仅表示 local executor 对新 ACTIVE revision 可安全接令，Cloud-owned durable business checkpoint/rehydration owner 建立前 host/Task/Service cohort 不得激活。

### R1（P1-1）：可构造的唯一 producer 链（已对当前对象图逐点核实）

取消 ledger->registry 反向 callback；三个触点只保留一条单向链，全部构造/锁名以现源码为准：

1. **registry 只建 slot**：`RemoteTaskRunRegistry.applyConfirmedBinding(...)` 在现有全局 `mutationLock`（RemoteTaskRunRegistry.java:23，已核）内检测 PAUSED->ACTIVE 发布，创建 entry 内唯一 `PendingExecutorReadiness` slot（状态 AWAITING_DRAIN，记录 fromRevision/toRevision/entryGeneration/slotGeneration）。registry **不查询 ledger、不冻结、不发送**——zero-at-create 情形同样留给 poller 边界处理，因此不需要任何 registry->ledger 引用。
2. **ledger 提供 powerless quiescence snapshot**：`RemoteOperationLedger.claim(RemoteGameCommand)`（现签名已核）在现有 LedgerEntry 上补存 command 自带的 powerless identity（taskRunId/window 四元组/stopEpoch/runRevision/operation）；新增 synchronized 只读 `quiescenceSnapshot(taskRunId, oldRevision)`，返回 immutable 三类未完成计数。无任何回调、无新锁；调用方先取 snapshot（ledger monitor 内完成并返回不可变值），**之后**才进 registry `mutationLock`，锁序单向、永不嵌套。
3. **poller 固定边界消费**：`RemoteCommandPollingLoop` 的 public 构造器（RemoteCommandPollingLoop.java:25）追加注入 `RemoteTaskRunRegistry + RemoteOperationLedger + RemoteTaskRunApiClient` 三个 final collaborator。已核实 main 源码中该类**没有生产构造点**（rg 无 `new RemoteCommandPollingLoop`），故本 slice 不改任何 bootstrap/assembly 文件，构造器变更编译即安全；未来激活门负责真实装配。循环内在两个固定边界调用私有 `flushExecutorReadinessOnce()`：(a) poll 返回 IDLE 后，(b) command outcome 提交完成后。
4. **poller 局部故障隔离**：`flushExecutorReadinessOnce()` 整体包在自己的 `try { ... } catch (RemoteCommandTransportException | RuntimeException e) { log; /* slot 与 request 原样保留 */ }` 内，任何 readiness timeout/5xx/解析失败都不会到达 RemoteCommandPollingLoop.java:159 的外层 `catch (Exception)`，command polling 循环永不因 readiness 故障终止。
5. **边界内序列**：读 slot 的 immutable handle -> AWAITING_DRAIN 则取 ledger snapshot，三类计数全零时在 `mutationLock` 内 CAS 冻结（校验 entryGeneration/slotGeneration/registration 仍 ACTIVE/toRevision 仍 current），一次性观测 fact、生成稳定 requestId、算 canonical digest、构造 immutable request 存入 slot，置 READY_TO_SEND；READY_TO_SEND 则经 `RemoteTaskRunApiClient.confirmResumedExecutorReady(request)` 同步发送（bounded timeout，见 R3）。重试永远重交同一 request 对象。

### R2（P1-2）：双仓完整拟改文件表（整体替换 Design #1 §10 与 Repair #1 R4；无“不变”引用）

已作废并从一切写集中删除：`RemoteResumeReconcileDecision`、`RemoteResumeMismatchCode`（Cloud 与 DHXY 均不创建）。

**Cloud Brain（4 new + 8 modify，预算 <=1,100 added lines）**

| # | 文件 | New/Modify | 内容 |
|---|---|---|---|
| C1 | `remote/ResumeExecutorReadinessFact.java` | New | strict wire record：R1 允许字段全集 + `factDigest`；unknown/负值/空白拒绝 |
| C2 | `remote/RemoteTaskRunReceipt.java` | New | typed receipt：`taskRunId/confirmedRunRevision/receiptId/requestId/requestDigest/factDigest/recordedAtEpochMs` |
| C3 | `remote/run/ResumeConfirmationRequirement.java` | New | pending `(fromRevision,toRevision)`，由 resume 写入 |
| C4 | `remote/run/ExecutionConfirmationRecord.java` | New | one-per-run typed record：kind=INITIAL_REGISTRATION 或 RESUME_EXECUTOR_READY + request identity + fact + bound snapshot |
| C5 | `remote/RemoteTaskRunAction.java` | Modify | + `CONFIRM_RESUMED_EXECUTOR_READY` |
| C6 | `remote/RemoteTaskRunActionRequest.java` | Modify | 新 action 专属字段：`taskRunId/requestId/requestDigest/fact`（其余 action 禁带，strict） |
| C7 | `remote/RemoteTaskRunActionResponse.java` | Modify | 新 action 返回 `receipt`（C2），duplicate retry 回放同字节 receipt |
| C8 | `api/RemoteTaskRunEndpoint.java` | Modify | 新 action 的 unknown-field 白名单、canonical digest 先验、typed 拒绝码映射 |
| C9 | `remote/RemoteProtocolDigests.java` | Modify | + `computeTaskRunActionDigest(...)`：JCS(body 去 requestDigest)，与 DHXY 字节一致 |
| C10 | `remote/run/RemoteTaskRunCoordinator.java` | Modify | `resume()` 写 C3；`confirmExecution` 对存在 pending requirement 的 run 拒绝（关 raw-confirm 旁路）；新 synchronized `confirmResumedExecutorReady(...)`：Repair #1 R2 的验证矩阵 + 单次 C4 record write；`confirmedExecutionRevisionByTaskRunId` 收窄为 `Map<String,ExecutionConfirmationRecord>` |
| C11 | `remote/run/RemoteTaskRunValidation.java` | Modify | 新字段/digest/receipt 校验助手 |
| C12 | `remote/CloudTaskRunExecutionGate.java` | Modify | context mint 前要求 current ACTIVE revision 的 record 存在且 kind 合法；其余不动 |

**DHXY（3 new + 9 modify，预算 <=950 added lines）**

| # | 文件 | New/Modify | 内容 |
|---|---|---|---|
| D1 | `cloud/remote/ResumeExecutorReadinessFact.java` | New | C1 的逐字段镜像 |
| D2 | `cloud/remote/RemoteTaskRunReceipt.java` | New | C2 的逐字段镜像 |
| D3 | `cloud/remote/PendingExecutorReadiness.java` | New | package-private slot + immutable handle（见 R3） |
| D4 | `cloud/remote/RemoteTaskRunAction.java` | Modify | + 同名 enum 常量 |
| D5 | `cloud/remote/RemoteTaskRunActionRequest.java` | Modify | + `taskRunId/requestId/requestDigest/fact` |
| D6 | `cloud/remote/RemoteTaskRunActionResponse.java` | Modify | + `receipt` strict 解析 |
| D7 | `cloud/remote/RemoteProtocolDigests.java` | Modify | + 同一 canonical lifecycle-body digest（与 C9 字节一致） |
| D8 | `cloud/remote/RemoteTaskRunRegistry.java` | Modify | entry 内 slot 字段、generation 计数、全部状态转移（均在现有 `mutationLock`） |
| D9 | `cloud/remote/RemoteOperationLedger.java` | Modify | entry 补存 powerless identity + `quiescenceSnapshot(...)` |
| D10 | `cloud/remote/RemoteCommandPollingLoop.java` | Modify | 构造器注入 + 两个固定边界 + 局部 try/catch + bounded 发送 |
| D11 | `cloud/remote/RemoteTaskRunApiClient.java` | Modify | + `confirmResumedExecutorReady(request)` 接口方法 |
| D12 | `cloud/remote/HttpRemoteTaskRunApiClient.java` | Modify | 实现 D11：strict body/digest/receipt、bounded timeout、typed 拒绝码 |

**字段级双仓 parity**：wire body = `{contractVersion:1, action:CONFIRM_RESUMED_EXECUTOR_READY, tenantId,userId,deviceId,clientSessionId, taskRunId, requestId, requestDigest, fact{taskType, windowId,nativeHandle,processId,playerIdentityEpoch, stopEpoch, resumedFromRunRevision,newActiveRunRevision, localRegistrationGeneration, localRegistrationStatus:"ACTIVE", previousLocalStatus:"PAUSED", pauseTokenMechanicalGeneration, operationLedgerRevision, inFlightCaptureCount:0,inFlightFactCount:0,inFlightInputCount:0, observedAtEpochMs, producer:"REGISTRY_RESUME_PUBLISH", factDigest}}`；`requestDigest = sha256(JCS(body 去 requestDigest))`，两侧均按 C9/D7 的同一 canonical 序列化计算；response receipt 字段即 C2/D2。任一侧 unknown field、缺字段、digest 不符即拒绝；identical retry（同 requestId+digest+factDigest）回放原 receipt，同 ID 异字节 = `IDEMPOTENCY_CONFLICT`。

**授权披露**：`RemoteTaskRunRegistry`、`HttpRemoteTaskRunApiClient` 属早前会话级禁碰类，本表列入拟改；实施唯一授权来源是父级 `DESIGN APPROVED` 对本表的显式认可。`TaskPauseToken`、`RemoteTaskRunLifecycleService` 保持零修改（slot 由 registry publish 触发，resume 语义不变）。

### R3（P2-1）：slot handle、stale-result fence 与防饥饿

- **immutable handle**：读取即返回 `(entryGeneration, slotGeneration, requestId, toRevision, request)` 五元组快照。receipt/reject 回写在 `mutationLock` 内先比对五元组与 entry 当前值，全部相等才 CAS 置 CONFIRMED/CANCELLED_REJECTED；任一不等（已 supersede/terminal/replacement）即丢弃为 stale audit 日志，绝不污染新 slot。
- **exact-session 过滤 + bounded batch**：poller 只扫描 scope 四元组等于自身 poll 身份的 entry，每个边界最多处理 **1** 条 slot；readiness HTTP 使用固定 bounded timeout（10s，独立于 poll wait）。
- **防饥饿**：发送发生在 IDLE/outcome 边界之后、下一次 command poll 之前，单次最多一个 bounded HTTP；同一 slot 两次发送之间至少间隔一个完整 poll 周期（slot 记 lastAttemptCycle，机械节流，非业务 retry）。command poll 永不被 readiness 队列阻塞多于一次 bounded HTTP。
- 清理转移表沿用 Repair #1 R3，并加一行：**stale receipt/reject（handle 不匹配）-> 丢弃 + audit，不改 slot**。

### 收尾

B-PASS 5 已在篇首声明；coordinator one-record 原子性、pending RESUME requirement、raw-confirm 旁路拒绝、exact binding/revision/session/zero-in-flight 门全部保留（B-PASS 5 前半）。`无已批准业务差异；按基线等价迁移。` 本 worker 自审 P0=0/P1=0/P2=0，不构成父级批准；按 Parent gate 停止等待唯一 `Local Design Review`。owner 继续 5 分钟 heartbeat。

## Local Design Review #4 - DESIGN APPROVED - 2026-07-12

- 评审范围：完整 Design Repair #1 + Repair #2、当前双仓 lifecycle/registry/ledger/poller/API 类型和 Review #3 的
  stale-result gate。Repair #2 已明确 immutable send handle、slot generation、exact-handle CAS、stale receipt 丢弃、
  exact-session 每边界最多一条及 bounded HTTP；此前 pure executor-readiness、单向 producer、zero old-revision
  in-flight、pending RESUME、one-record confirmation、raw-confirm bypass 拒绝和 host/business rehydration 后置继续成立。
- 父级结论：以下三项由 parent 在本 review 直接作绑定修正，不再增加设计轮次；修正后
  **`P0=0 / P1=0 / P2=0，DESIGN APPROVED`**。

### 绑定修正（实施必须逐字遵守）

1. **双层 digest 无环定义**：`factDigest = sha256(canonical JCS(fact excluding factDigest))`；随后
   `requestDigest = sha256(canonical JCS(outer request excluding requestDigest, including the finalized factDigest))`。
   retry 保存并重交同一 finalized fact/request/body，不重新读取时钟或序列化。Cloud endpoint 按相同两步重算并同时
   比对 factDigest/requestDigest。
2. **typed rejection 文件补齐**：在 Repair #2 表之外，显式授权 Cloud 与 DHXY 各修改
   `remote/RemoteTaskRunErrorCode.java`，增加稳定代码 `RECONCILE_REQUIRED`、`STALE_REVISION`、
   `FUTURE_REVISION`、`BINDING_MISMATCH`、`REGISTRATION_MISMATCH`、`LOCAL_NOT_QUIESCENT`、
   `RECONCILE_NOT_EXPECTED`、`IDEMPOTENCY_CONFLICT`、`TERMINAL`；已有 `SESSION_CONFLICT` 复用。endpoint/client/pump
   只按 enum 分类 permanent/unknown，禁止解析 message 文本。最终写集为 Cloud **4 new + 9 modify**、DHXY
   **3 new + 10 modify**。
3. **可编译异常隔离**：不得写继承相关的 multi-catch
   `RemoteCommandTransportException | RuntimeException`。readiness hook 先单独 catch
   `RemoteTaskRunClientException`，按 `isOutcomeUncertain()` 与 typed remote code 做 READY/permanent-reject exact-handle
   回写；再 catch `RuntimeException` 只保留同一 request、记录 redacted warning 并返回 poll loop。若线程因显式 stop 被
   interrupt，保留现有 stop 退出语义，不吞 interrupt。
4. **quiescence 范围**：ledger query 必须带 exact scope/session/taskRun/window/stopEpoch，并统计所有
   `runRevision < newActiveRunRevision` 的未 terminal operation，而不是只按 taskRunId 或仅等于 previous revision。
5. **stale result**：accepted/unknown/permanent-reject 三种回写均须比较完整
   `(entryGeneration,slotGeneration,requestId,toRevision,retainedSend identity)`；不匹配只记审计，绝不触碰 current slot。

### 实施授权与门禁

- 同一 worker 现在可按 Repair #2 两张表 + 上述双仓 error-code 文件实施；不得触碰明确 zero-diff 文件，不得新增/恢复
  tests，不得启动 application/server/host/poller/UI/capture/OCR/input，不得做 Git mutation。
- 保护两仓全部既有 dirty/untracked 改动；开工先逐项确认 7 个 new 目标不存在，21 个 modify 文件当前内容与设计所审
  版本一致。出现并行冲突立即停在本文件报告，不覆盖。
- 完成后追加 `External Worker - Implementation #1 - 2026-07-12`，列精确 diff、双仓 field/digest parity、producer/
  CAS 时序、no-business/no-start/no-test 证据；运行 Cloud `mvn -q clean package`（不 skip）及 DHXY
  `mvn -q -DskipTests compile`。worker 自审不算父级 `APPROVED`，实现后停住等待源码复审。

`无已批准业务差异；按基线等价迁移。`

## External Worker - Design Repair #2 - 2026-07-12

### 返修范围与结论

- 本 Repair 只补 `READY -> SENDING` 锁外 HTTP 的 immutable send handle、exact-result CAS 和 stale-result discard。
  `Local Design Review #3` 已关闭的 producer、wire、digest、file table、pure executor-readiness、pending RESUME、Cloud
  one-record atomic confirmation、raw-confirm bypass、zero old-revision inflight 与 dormant gate 全部保持，不重新设计。
- 本版自审 `P0=0 / P1=0 / P2=0`，无剩余设计 blocker；自审不构成父级批准。Java 继续冻结。

### Immutable PendingSendHandle

`RemoteTaskRunRegistry.claimReadyForSend(exactPollScope, nowNanos)` 每次最多返回一个 immutable：

```java
record PendingSendHandle(
        long entryGeneration,
        long slotGeneration,
        String requestId,
        long toRevision,
        PendingExecutorReadiness.RetainedSend retainedSend) {
}
```

- `entryGeneration` 由 `RemoteTaskRunRegistry` 的 registry-wide monotonic counter 在既有 `mutationLock` 下分配；每个新
  `RegistryEntry` 只分配一次，entry removal/re-registration/session replacement 永不复用。counter 到 `Long.MAX_VALUE`
  时 fail-closed 拒绝新 entry，不回绕。
- `slotGeneration` 来自每个 entry 自己的 monotonic `nextSlotGeneration`，也只在 `mutationLock` 下、每次
  PAUSED->ACTIVE 建立新 pending slot 时 `Math.incrementExact`。同 binding 的幂等 publication 不增；PAUSE/new RESUME/
  supersede/terminal 不回退。溢出时不创建 slot并产生 capacity/safety alert，绝不复用旧 generation。
- `requestId`、`toRevision` 与 `retainedSend` 均取自当前 READY slot。`retainedSend` 是 materialize 时创建且之后不替换的
  exact request object/body/id/digest owner；handle 保存同一 object reference。
- claim 在 `mutationLock` 内同时验证 pollRequest exact tenant/user/device/clientSession、slot `READY`、机械
  `nextAttemptNotBeforeNanos <= nowNanos`，然后转 `SENDING` 并返回 handle。HTTP 仍在锁外。

### Exact-handle CAS APIs

registry 只新增/收窄以下 package-private result API；三个方法都在现有 `mutationLock` 内执行：

```text
markAccepted(PendingSendHandle handle, RemoteExecutorReadinessConfirmation receipt)
markUnknownForRetry(PendingSendHandle handle, long nextAttemptNotBeforeNanos, RetryReason reason)
markPermanentRejected(PendingSendHandle handle, RemoteTaskRunErrorCode code)
```

三者先执行同一个 `matchesCurrentSending(handle)`：

```text
entry still exists for retainedSend.taskRunId
&& entry.entryGeneration == handle.entryGeneration
&& current slot != null
&& current slot.state == SENDING
&& current slot.slotGeneration == handle.slotGeneration
&& current slot.requestId == handle.requestId
&& current slot.toRevision == handle.toRevision
&& current slot.retainedSend is the same object as handle.retainedSend
```

- **全部匹配**才允许 CAS-like 转移：
  - `markAccepted` 还必须先由 pump/client 验证 receipt 的 requestId/requestDigest/factDigest/taskRunId/toRevision 与
    retained send 完全一致；随后 `SENDING -> null`，释放 request body。
  - `markUnknownForRetry` 只做 `SENDING -> READY`，在同一 slot 写入 bounded mechanical
    `nextAttemptNotBeforeNanos`；retained object/body/id/digest 不变。
  - `markPermanentRejected` 只做 `SENDING -> null` 并记 typed reject metric。
- **任一不匹配**即返回 `STALE_HANDLE_IGNORED`：不清 slot、不改 state、不改 retry time、不触碰 retained object。只记录
  redacted `executor_readiness_stale_result_total{resultKind}` 与日志中的旧 entry/slot generation、旧 toRevision、requestId
  前8位；不输出当前新 slot 的 body/digest/scope。
- pump 不允许按 taskRunId 单独回写，也不允许失败后重新 lookup “current slot”再操作；所有 receipt/reject/timeout 必须
  携带最初 claim 返回的同一个 handle。

### Updated slot state machine

```text
entry(E), slot null
  -> AWAITING_DRAIN(S=nextSlotGeneration)
  -> READY(S, retainedSend, nextAttempt)
  -> claim -> SENDING(S) + immutable handle(E,S,requestId,toRevision,retainedSend)

SENDING(S) + exact handle + accepted receipt
  -> null
SENDING(S) + exact handle + timeout/5xx/UNKNOWN
  -> READY(S, SAME retainedSend, throttled nextAttempt)
SENDING(S) + exact handle + permanent reject
  -> null

any state + PAUSE/new RESUME/STOP/COMPLETE/session removal
  -> old slot removed; new resume creates S+1 (or a new entry E+1)

any late result + stale handle
  -> STALE_HANDLE_IGNORED; current slot unchanged
```

slot 的 current identity 是 `(entryGeneration, slotGeneration)`；`requestId/toRevision/retainedSend identity` 是额外防线，
防止错误 object 被同 generation API 使用。

### A/B late-response ordering

```text
t0  entry E7 creates slot A: S11, toRevision=r+1
t1  claim A -> handle HA=(E7,S11,idA,r+1,sendA); HTTP runs outside lock
t2  local PAUSE clears A
t3  next RESUME creates slot B: S12, toRevision=r+3, later READY/SENDING
t4  A response arrives
t5  markAccepted(HA, receiptA) checks current B against HA
    slotGeneration S12 != S11 (also id/toRevision/object differ)
    -> STALE_HANDLE_IGNORED; B remains byte-for-byte/state-for-state unchanged
t6  only handle HB=(E7,S12,idB,r+3,sendB) may transition B
```

若 t2 同时发生 session replacement/entry removal，则旧 E7 被删除；新 entry 分配 E8（即使 taskRunId 文本意外相同），
HA 在 entryGeneration 第一项即失败。由此 session removal/new resume 后旧 handle 永不匹配。

### Pump / registry API update

不扩大上一版拟改文件集，仅细化其中三项：

| 已列文件 | Repair #2 精确增量 |
|---|---|
| `cloud/remote/PendingExecutorReadiness.java` | 增加 monotonic slotGeneration、stable retainedSend identity、state/nextAttempt；新增 immutable nested `PendingSendHandle`（若父级要求 top-level，可同文件 package-private record） |
| `cloud/remote/RemoteTaskRunRegistry.java` | registry-wide entry generation；`claimReadyForSend(exactPollScope,now)`；三个 exact-handle CAS result 方法；所有逻辑只用现有 `mutationLock` |
| `cloud/remote/RemoteExecutorReadinessPump.java` | claim 后只保留 handle；accepted/unknown/permanent 三种 typed outcome 分别调用对应 CAS API；stale 返回只计 metric，不重试/清新 slot |

`RemoteCommandPollingLoop` 仍按已批准设计在 IDLE 与 outcome-submitted 边界调用局部 typed try/catch；每个 boundary 最多
一条，只 claim 与 `pollRequest` exact four-part scope/session 匹配的 slot。readiness HTTP 使用既定 bounded max timeout；
unknown retry 保留 same bytes/id/digest 并应用 slot 中 mechanical not-before 节流。无 readiness exception 进入 poll loop 外层
catch，因此不停止 command poll；一次 readiness timeout 后必先进入下一轮 command poll，不能连续 drain readiness 队列而
饿死 command transport。

### Invariants / zero diff

- fact 仍只有 executor registration/quiescence mechanical fields；不恢复 decision、mismatch、fingerprint、phase/action/
  dialog/pathing/timer/clear，也不修改本地 reconciler/model/业务 Task。
- old revision 已开始 input 仍等 ledger future terminal；本 Repair 不 cancel、不重排、不改变 mid-bundle。
- Cloud pending RESUME、atomic accepted record、raw confirm bypass 拒绝、gate/context exact record read 均不变。
- 双仓 action/request/response/fact/receipt/digest/endpoint/coordinator/gate/client 文件表与 diff budget不变；本 Repair 不新增
  文件，不改 host/server/poller activation、pom/resources/tests/其它 docs。
- 成功仍只表示 local executor ready。Cloud durable business checkpoint、phase/action rehydration 与 continuation owner 后置；
  host/Task/Service cohort 保持 dormant。

### 批准后验证增量（本轮不执行）

```powershell
rg -n "PendingSendHandle|entryGeneration|slotGeneration|markAccepted|markUnknownForRetry|markPermanentRejected|STALE_HANDLE_IGNORED" `
  D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote
rg -n "claimReadyForSend|flushExecutorReadinessSafely" `
  D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote
```

- 静态复核三种 result path 都携带 original handle，且不存在仅按 taskRunId 清/ready slot 的 API；构造 A(r+1)/B(r+3)
  时序做人工 source walkthrough，确认 A 的三类迟到结果都返回 stale、B 不变。
- 保持既定后续门：Cloud `mvn -q clean package` 无 skip；DHXY `mvn -q -DskipTests compile`；不新增/恢复/运行 DHXY
  tests。本轮未运行 Maven/测试/应用/server/host/poller/UI/capture/OCR/input，未做 Git mutation。

Design Repair #2 到此停止，等待父级 `DESIGN APPROVED` 或 `BLOCKED`。

## External Worker - Design Repair #2 Addendum - Review #3 alignment - 2026-07-12

- 并发披露：上方 `External Worker - Design Repair #2` 主体是在 `Local Process Note #2` 与 `Local Design Review #3`
  可见之前、按 Review #2 条件并发起草的（与 parent 已两次对账的并发追加为同一情形）。按 Review #3 B-PASS 1
  （不得重开已关闭范围），特此收窄：
  - 该条目中 R1（producer 链）与 R2（双仓文件表）凡与已被 parent 采信的完整版 `Design Repair #1`（第二段同名
    heading）不一致之处，一律 **非权威、不生效**；producer/wire/business 范围维持完整版 Repair #1 所定基线
    （registry `mutationLock` + 无状态 pump、`READY -> SENDING` 状态机、既定文件表），不重开。
  - 该条目中 R3 是对 Review #3 唯一 P2 的实质应答，现按采信基线的 API 词汇精确重述如下，作为本 Repair #2 的
    唯一权威内容。

### P2-1 修复（exact send-handle CAS 与 stale-result 丢弃）

1. **claimReadyForSend**：pump 在 registry `mutationLock` 内领取发送权时，返回 immutable
   `PendingSendHandle(entryGeneration, slotGeneration, requestId, toRevision, retainedSend)`；同时 slot
   `READY -> SENDING`。handle 是值快照，锁外 HTTP 全程只持有 handle，不持有 slot 引用。
2. **结果回写全部经 handle CAS**：`markAccepted(handle, receipt)`、`markUnknownForRetry(handle)`、
   `markPermanentRejected(handle, code)` 三个转移都在 `mutationLock` 内执行，先逐项比较 handle 五元组与该 entry
   当前 slot 的 `(entryGeneration, slotGeneration, requestId, toRevision, retainedSend 对象同一性)`：
   - 全部相等且状态为 SENDING -> 执行转移（CONFIRMED 留 receipt 摘要 / SENDING->READY 保留同一 retainedSend /
     CANCELLED_REJECTED 留拒绝码）；
   - 任一不等或 slot 已不存在 -> **仅记录 redacted stale-result metric/log（不含 fact/receipt 明细），对当前
     slot 零操作**。Review #3 的时序（A 已 SENDING、response 迟到、期间 PAUSE/RESUME 建 B(r+3)）因此收敛：
     A 的迟到 success/reject 因 slotGeneration 不匹配被丢弃，B 完好，r+3 可正常确认。
3. **slotGeneration 单调来源**：每个 registry entry 持有 `long slotGenerationCounter`，仅在 `mutationLock` 内、
   每次 slot 创建/替换时 `++`；handle 记录领取时的值。entryGeneration 沿用 entry 既有代计数。session
   replacement/entry 移除后 entry 与两个 generation 一并消失，任何旧 handle 永不匹配（比较在"slot 已不存在"
   分支直接走 stale 丢弃）。
4. **发送语义不变**（Review #3 B-PASS 2 原样保留）：每 poll boundary 最多一条、exact session 过滤、bounded
   timeout、重试永远重交同一 retainedSend 的同 bytes/id/digest、confirm 故障局部 try/catch 隔离，绝不进入
   poller 外层 catch。
5. **状态机更新**：`AWAITING_DRAIN -> READY -> SENDING -> CONFIRMED | CANCELLED_TERMINAL |
   CANCELLED_SUPERSEDED | CANCELLED_REJECTED`；SENDING 出边全部由 handle-CAS 门守卫；timeout ->
   `markUnknownForRetry`（SENDING->READY，对象不重建）。拟改文件集不扩大：以上全部落在既定基线文件
   `PendingExecutorReadiness`（handle 与状态机）、`RemoteTaskRunRegistry`（claim/mark 三方法与 generation
   计数）、pump/poller 边界调用处之内。

`无已批准业务差异；按基线等价迁移。` 本 addendum 为 QA 级自审 P0=0/P1=0/P2=0，不构成父级批准；写完即停，等待
唯一 `Local Design Review`。owner 继续 5 分钟 heartbeat。

## External Worker - Design Repair #2 - 2026-07-12

### 返修范围与结论

- 本 Repair 只补 `READY -> SENDING` 锁外 HTTP 的 immutable send handle、exact-result CAS 和 stale-result discard。
  `Local Design Review #3` 已关闭的 producer、wire、digest、file table、pure executor-readiness、pending RESUME、Cloud
  one-record atomic confirmation、raw-confirm bypass、zero old-revision inflight 与 dormant gate 全部保持，不重新设计。
- 本版自审 `P0=0 / P1=0 / P2=0`，无剩余设计 blocker；自审不构成父级批准。Java 继续冻结。

### Immutable PendingSendHandle

`RemoteTaskRunRegistry.claimReadyForSend(exactPollScope, nowNanos)` 每次最多返回一个 immutable：

```java
record PendingSendHandle(
        long entryGeneration,
        long slotGeneration,
        String requestId,
        long toRevision,
        PendingExecutorReadiness.RetainedSend retainedSend) {
}
```

- `entryGeneration` 由 `RemoteTaskRunRegistry` 的 registry-wide monotonic counter 在既有 `mutationLock` 下分配；每个新
  `RegistryEntry` 只分配一次，entry removal/re-registration/session replacement 永不复用。counter 到 `Long.MAX_VALUE`
  时 fail-closed 拒绝新 entry，不回绕。
- `slotGeneration` 来自每个 entry 自己的 monotonic `nextSlotGeneration`，也只在 `mutationLock` 下、每次
  PAUSED->ACTIVE 建立新 pending slot 时 `Math.incrementExact`。同 binding 的幂等 publication 不增；PAUSE/new RESUME/
  supersede/terminal 不回退。溢出时不创建 slot并产生 capacity/safety alert，绝不复用旧 generation。
- `requestId`、`toRevision` 与 `retainedSend` 均取自当前 READY slot。`retainedSend` 是 materialize 时创建且之后不替换的
  exact request object/body/id/digest owner；handle 保存同一 object reference。
- claim 在 `mutationLock` 内同时验证 pollRequest exact tenant/user/device/clientSession、slot `READY`、机械
  `nextAttemptNotBeforeNanos <= nowNanos`，然后转 `SENDING` 并返回 handle。HTTP 仍在锁外。

### Exact-handle CAS APIs

registry 只新增/收窄以下 package-private result API；三个方法都在现有 `mutationLock` 内执行：

```text
markAccepted(PendingSendHandle handle, RemoteExecutorReadinessConfirmation receipt)
markUnknownForRetry(PendingSendHandle handle, long nextAttemptNotBeforeNanos, RetryReason reason)
markPermanentRejected(PendingSendHandle handle, RemoteTaskRunErrorCode code)
```

三者先执行同一个 `matchesCurrentSending(handle)`：

```text
entry still exists for retainedSend.taskRunId
&& entry.entryGeneration == handle.entryGeneration
&& current slot != null
&& current slot.state == SENDING
&& current slot.slotGeneration == handle.slotGeneration
&& current slot.requestId == handle.requestId
&& current slot.toRevision == handle.toRevision
&& current slot.retainedSend is the same object as handle.retainedSend
```

- **全部匹配**才允许 CAS-like 转移：
  - `markAccepted` 还必须先由 pump/client 验证 receipt 的 requestId/requestDigest/factDigest/taskRunId/toRevision 与
    retained send 完全一致；随后 `SENDING -> null`，释放 request body。
  - `markUnknownForRetry` 只做 `SENDING -> READY`，在同一 slot 写入 bounded mechanical
    `nextAttemptNotBeforeNanos`；retained object/body/id/digest 不变。
  - `markPermanentRejected` 只做 `SENDING -> null` 并记 typed reject metric。
- **任一不匹配**即返回 `STALE_HANDLE_IGNORED`：不清 slot、不改 state、不改 retry time、不触碰 retained object。只记录
  redacted `executor_readiness_stale_result_total{resultKind}` 与日志中的旧 entry/slot generation、旧 toRevision、requestId
  前8位；不输出当前新 slot 的 body/digest/scope。
- pump 不允许按 taskRunId 单独回写，也不允许失败后重新 lookup “current slot”再操作；所有 receipt/reject/timeout 必须
  携带最初 claim 返回的同一个 handle。

### Updated slot state machine

```text
entry(E), slot null
  -> AWAITING_DRAIN(S=nextSlotGeneration)
  -> READY(S, retainedSend, nextAttempt)
  -> claim -> SENDING(S) + immutable handle(E,S,requestId,toRevision,retainedSend)

SENDING(S) + exact handle + accepted receipt
  -> null
SENDING(S) + exact handle + timeout/5xx/UNKNOWN
  -> READY(S, SAME retainedSend, throttled nextAttempt)
SENDING(S) + exact handle + permanent reject
  -> null

any state + PAUSE/new RESUME/STOP/COMPLETE/session removal
  -> old slot removed; new resume creates S+1 (or a new entry E+1)

any late result + stale handle
  -> STALE_HANDLE_IGNORED; current slot unchanged
```

slot 的 current identity 是 `(entryGeneration, slotGeneration)`；`requestId/toRevision/retainedSend identity` 是额外防线，
防止错误 object 被同 generation API 使用。

### A/B late-response ordering

```text
t0  entry E7 creates slot A: S11, toRevision=r+1
t1  claim A -> handle HA=(E7,S11,idA,r+1,sendA); HTTP runs outside lock
t2  local PAUSE clears A
t3  next RESUME creates slot B: S12, toRevision=r+3, later READY/SENDING
t4  A response arrives
t5  markAccepted(HA, receiptA) checks current B against HA
    slotGeneration S12 != S11 (also id/toRevision/object differ)
    -> STALE_HANDLE_IGNORED; B remains byte-for-byte/state-for-state unchanged
t6  only handle HB=(E7,S12,idB,r+3,sendB) may transition B
```

若 t2 同时发生 session replacement/entry removal，则旧 E7 被删除；新 entry 分配 E8（即使 taskRunId 文本意外相同），
HA 在 entryGeneration 第一项即失败。由此 session removal/new resume 后旧 handle 永不匹配。

### Pump / registry API update

不扩大上一版拟改文件集，仅细化其中三项：

| 已列文件 | Repair #2 精确增量 |
|---|---|
| `cloud/remote/PendingExecutorReadiness.java` | 增加 monotonic slotGeneration、stable retainedSend identity、state/nextAttempt；新增 immutable nested `PendingSendHandle`（若父级要求 top-level，可同文件 package-private record） |
| `cloud/remote/RemoteTaskRunRegistry.java` | registry-wide entry generation；`claimReadyForSend(exactPollScope,now)`；三个 exact-handle CAS result 方法；所有逻辑只用现有 `mutationLock` |
| `cloud/remote/RemoteExecutorReadinessPump.java` | claim 后只保留 handle；accepted/unknown/permanent 三种 typed outcome 分别调用对应 CAS API；stale 返回只计 metric，不重试/清新 slot |

`RemoteCommandPollingLoop` 仍按已批准设计在 IDLE 与 outcome-submitted 边界调用局部 typed try/catch；每个 boundary 最多
一条，只 claim 与 `pollRequest` exact four-part scope/session 匹配的 slot。readiness HTTP 使用既定 bounded max timeout；
unknown retry 保留 same bytes/id/digest 并应用 slot 中 mechanical not-before 节流。无 readiness exception 进入 poll loop 外层
catch，因此不停止 command poll；一次 readiness timeout 后必先进入下一轮 command poll，不能连续 drain readiness 队列而
饿死 command transport。

### Invariants / zero diff

- fact 仍只有 executor registration/quiescence mechanical fields；不恢复 decision、mismatch、fingerprint、phase/action/
  dialog/pathing/timer/clear，也不修改本地 reconciler/model/业务 Task。
- old revision 已开始 input 仍等 ledger future terminal；本 Repair 不 cancel、不重排、不改变 mid-bundle。
- Cloud pending RESUME、atomic accepted record、raw confirm bypass 拒绝、gate/context exact record read 均不变。
- 双仓 action/request/response/fact/receipt/digest/endpoint/coordinator/gate/client 文件表与 diff budget不变；本 Repair 不新增
  文件，不改 host/server/poller activation、pom/resources/tests/其它 docs。
- 成功仍只表示 local executor ready。Cloud durable business checkpoint、phase/action rehydration 与 continuation owner 后置；
  host/Task/Service cohort 保持 dormant。

### 批准后验证增量（本轮不执行）

```powershell
rg -n "PendingSendHandle|entryGeneration|slotGeneration|markAccepted|markUnknownForRetry|markPermanentRejected|STALE_HANDLE_IGNORED" `
  D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote
rg -n "claimReadyForSend|flushExecutorReadinessSafely" `
  D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\cloud\remote
```

- 静态复核三种 result path 都携带 original handle，且不存在仅按 taskRunId 清/ready slot 的 API；构造 A(r+1)/B(r+3)
  时序做人工 source walkthrough，确认 A 的三类迟到结果都返回 stale、B 不变。
- 保持既定后续门：Cloud `mvn -q clean package` 无 skip；DHXY `mvn -q -DskipTests compile`；不新增/恢复/运行 DHXY
  tests。本轮未运行 Maven/测试/应用/server/host/poller/UI/capture/OCR/input，未做 Git mutation。

Design Repair #2 到此停止，等待父级 `DESIGN APPROVED` 或 `BLOCKED`。

## External Worker - Implementation #1 - 2026-07-12

### Scope and scoped baseline

- 实施依据：`Local Design Review #4 - DESIGN APPROVED`，`P0/P1/P2=0`。开工 HEAD：Cloud
  `3b988caa010254973e03342272e6d1d6a9685b01`，DHXY
  `0114604e1ff5f15491d2910959c45252e893d04f`。7 个批准 new target 开工均不存在；21 个 modify target 已逐项读取并记录
  SHA-256/行数后定点修改。两仓目标原本均位于已有 dirty/untracked 工作树，本轮未 reset/revert/覆盖其它在途内容。
- 仅写 Review #4 批准的 Cloud 4 new + 9 modify、DHXY 3 new + 10 modify，以及本日志；未改其它文件。

### Exact files and diff budget

Cloud Brain（4 new + 9 modify）：

| file | exact change |
|---|---|
| new `remote/ResumeExecutorReadinessFact.java` | 54 行；strict mechanical fact + zero-inflight predicate |
| new `remote/RemoteTaskRunReceipt.java` | 22 行；stable typed receipt |
| new `remote/run/ResumeConfirmationRequirement.java` | 12 行；exact from/to revision pending gate |
| new `remote/run/ExecutionConfirmationRecord.java` | 32 行；INITIAL/RESUME one-record authority |
| modify `remote/RemoteTaskRunAction.java` | +1；`CONFIRM_RESUMED_EXECUTOR_READY` |
| modify `remote/RemoteTaskRunActionRequest.java` | +15；requestId/requestDigest/fact |
| modify `remote/RemoteTaskRunActionResponse.java` | +7；binding/receipt exclusive result |
| modify `api/RemoteTaskRunEndpoint.java` | +67；strict fact schema、双 digest、typed dispatch/reject |
| modify `remote/RemoteProtocolDigests.java` | +16；fact excluding self + finalized outer request digest |
| modify `remote/run/RemoteTaskRunCoordinator.java` | +93；pending RESUME、raw-confirm deny、atomic accepted record |
| modify `remote/run/RemoteTaskRunValidation.java` | +13；exact mechanical binding helper |
| modify `remote/CloudTaskRunExecutionGate.java` | +2；明确 context 只读 coordinator accepted record |
| modify `remote/RemoteTaskRunErrorCode.java` | +9；Review #4 stable typed codes |

DHXY（3 new + 10 modify）：

| file | exact change |
|---|---|
| new `cloud/remote/ResumeExecutorReadinessFact.java` | 31 行；Cloud field parity |
| new `cloud/remote/RemoteTaskRunReceipt.java` | 19 行；Cloud receipt parity |
| new `cloud/remote/PendingExecutorReadiness.java` | 45 行；bounded slot/retained send/immutable handles |
| modify `RemoteTaskRunAction.java` | +1；new action |
| modify `RemoteTaskRunActionRequest.java` | +3；requestId/requestDigest/fact + stable toBuilder |
| modify `RemoteTaskRunActionResponse.java` | +1；receipt |
| modify `RemoteProtocolDigests.java` | +22；NON_NULL canonical parity + two-level digest |
| modify `RemoteTaskRunRegistry.java` | +187；entry/slot generations、materialize、claim、exact-handle CAS |
| modify `RemoteOperationLedger.java` | +119；bound exact identity、ledger revision、all older-revision snapshot/freeze |
| modify `RemoteCommandPollingLoop.java` | +89；IDLE/outcome hooks、one send、layered local catches |
| modify `RemoteTaskRunApiClient.java` | +2；typed readiness call |
| modify `HttpRemoteTaskRunApiClient.java` | +91；bounded existing HTTP path、strict digest/receipt/error code |
| modify `RemoteTaskRunErrorCode.java` | +9；Cloud enum parity |

### Producer, ordering, and authority

1. Cloud `RESUME` 在 coordinator synchronized 临界区写 exact `(fromRevision,toRevision)` pending requirement；存在该
   requirement 时 raw `CONFIRM_EXECUTION` 返回 typed `RECONCILE_REQUIRED`，不能旁路。
2. DHXY registry 在既有 `mutationLock` 发布 exact PAUSED->ACTIVE 时建立 entry 内唯一 `AWAITING_DRAIN` slot；PAUSE、
   STOPPING、terminal、entry removal 清 slot。`entryGeneration` registry-wide 单调，`slotGeneration` entry-local 单调且均
   `Math.incrementExact` fail-closed，不复用。
3. ledger 在 claim 保存 exact scope/session/taskRun/window/stopEpoch/runRevision/operation；snapshot 只读并统计所有
   `runRevision < newActiveRunRevision` 未 terminal CAPTURE/WINDOW_FACT/INPUT。poller 用 ledger revision monitor freeze 后才
   调 registry materialize，旧 input bundle 不取消、不重排，future terminal 前不能 READY。
4. materialize 一次性固化 fact/request object、requestId、factDigest、requestDigest；每个 IDLE 或 outcome boundary 最多
   claim/send 一条 exact poll scope/session request。retry 保留同一 retained object/id/digests，并有 mechanical not-before。
5. claim 返回 immutable `PendingSendHandle(entryGeneration,slotGeneration,requestId,toRevision,retainedSend)`。accepted、
   unknown、permanent-reject 全部在 `mutationLock` 下比较完整 handle，包含 retained object identity；stale A(r+1) response
   遇到 B(r+3) slot 时不匹配并返回 `STALE_HANDLE_IGNORED`，绝不清理/改写 B。
6. Cloud endpoint 先重算 `factDigest = SHA-256(JCS(fact excluding factDigest))`，再重算
   `requestDigest = SHA-256(JCS(outer excluding requestDigest, including finalized factDigest))`；coordinator 在一个 synchronized
   方法内验证 pending RESUME、exact scope/taskRun/type/window/stopEpoch/from+to revision/current ACTIVE/zero inflight，随后只写
   一个 `ExecutionConfirmationRecord` 并清 pending。identical retry 回放原 receipt；同 revision 异 request/fact 为 typed
   `IDEMPOTENCY_CONFLICT`。gate/authorize 只认该同一 map 中 current revision record。

### Wire parity and typed failures

- 双仓 fact 19 字段、receipt 7 字段、action/request/response enum 与 JSON 名称一一对应；canonical mapper 均排除 null，
  `factDigest` 明确排除自身，outer digest 覆盖 finalized fact。receipt 严格回验 taskRunId/revision/requestId/requestDigest/
  factDigest。
- Cloud/DHXY error enum 同步加入 `RECONCILE_REQUIRED`、`STALE_REVISION`、`FUTURE_REVISION`、
  `BINDING_MISMATCH`、`REGISTRATION_MISMATCH`、`LOCAL_NOT_QUIESCENT`、`RECONCILE_NOT_EXPECTED`、
  `IDEMPOTENCY_CONFLICT`、`TERMINAL`，复用 `SESSION_CONFLICT`。client/poller 只反序列化 typed error code，不解析 message。
- poll hook 先单独 catch `RemoteTaskRunClientException`：outcome-uncertain/5xx/INTERNAL_ERROR exact-handle 回 READY，typed
  permanent reject exact-handle 清 slot；再 catch `RuntimeException` 保留同一 request。不存在继承相关 illegal multi-catch，
  readiness 故障不进入 poll loop 外层 catch；显式 stop interrupt 不被吞掉。

### Verification evidence

- Cloud fresh gate：`mvn -q clean package`，exit `0`，66.7s；Surefire `4` reports / `21` tests / failures `0` /
  errors `0` / skipped `0`。fat JAR：`target/dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`，119,502,296 bytes，SHA-256
  `21576EE1C00B8F8A36941087B8B02BD698ED502D5147A6EABC14B4F7D5B0E6F4`；original JAR 2,632,988 bytes。
- DHXY fresh gate（digest parity 最终修改后重跑）：`mvn -q -DskipTests compile`，exit `0`，17.6s。按约束未运行、
  新增或恢复 DHXY tests。
- `javap` 已确认 Cloud fact/receipt/request/response public record API、DHXY fact/receipt/client public API，以及 package-private
  `PendingExecutorReadiness` 无 public 暴露；字段与批准 wire 一致。
- 静态扫描：批准文件内无 continuation/hot-start/mismatch/business phase/dialog/pathing/timer/volatile-clear/reconciler；无
  `getRemoteErrorMessage()` 分类、无 illegal multi-catch。7 个新合同文件无 filesystem/new-thread/executor/timer/self-HTTP；Cloud
  无 self-HTTP。DHXY 只复用现有 `HttpRemoteTaskRunApiClient` outbound lifecycle transport，无新 transport/client/thread/poller。
  main source 无 `new RemoteCommandPollingLoop(...)` 构造点，host/cohort 继续 dormant；唯一 reachability 是既有 dormant endpoint
  action 与 poll-loop 固定 hook。
- protected status 复核仍只显示开工前已有项：Cloud `pom.xml`、host、AuthorityAssembly/action ledger、既有 resource dirty；
  DHXY `XiuluoTaskV2.java` 与 `LocalRemoteGameCommandHandler.java` dirty/untracked。本轮未写这些文件，亦未改 pom/resources/
  src/test、AuthorityAssembly/ledger/retained/broker/routes/host/server、pause reconciler/model、业务 Task、DHXY 其它 Java/文档/
  CR/dashboard。
- 未启动 application/server/host/poller/UI/capture/OCR/input，未发 HTTP，未触碰本机文件系统业务路径，未做 Git add/commit/
  push 或其它 Git mutation。

### Blocker and review gate

- 实施自审：`P0=0 / P1=0 / P2=0`，当前无 build/static blocker；该自审不构成 parent `APPROVED`。
- 本 slice 只关闭 local executor mechanical ready + Cloud atomic current ACTIVE revision confirmation。durable business checkpoint/
  rehydration/phase continuation 仍后置，host/Task/Service cohort 继续 dormant。`无已批准业务差异；按基线等价迁移。`
- Implementation #1 到此停止，等待父级源码 review。

## Local Implementation Review #1 - BLOCKED - 2026-07-12

- 评审范围：`External Worker - Implementation #1` 的 Cloud 4 new + 9 modify、DHXY 3 new + 10 modify，及当前
  endpoint/coordinator、registry/ledger/poller/client 的实际调用链。父级 fresh Cloud
  `mvn -q clean package` exit `0`（70.0s，4 suites / 21 tests / 0 failures / 0 errors / 0 skipped，shaded JAR
  119,502,296 bytes，SHA-256 `8AE506019B83286376C29A6E3C2B5F5222C32A56CADEBF1C90DD29B5A183D511`）；
  DHXY `mvn -q -DskipTests compile` exit `0`（3.9s）。构建门通过，但源码仍有
  **`P0=0 / P1=1 / P2=3`，BLOCKED**。Worker A 只返修下列四点，不扩大业务范围。

### P1-1：outer request digest 权威只在 HTTP endpoint，public coordinator 可绕过并写入不可重放的确认记录

- 证据：Cloud `RemoteTaskRunEndpoint.java:229-238` 会重算 fact/request 双 digest；但真正写
  `ExecutionConfirmationRecord` 的 public `RemoteTaskRunCoordinator.confirmResumedExecutorReady(...)`
  在 `RemoteTaskRunCoordinator.java:317-375` 只检查 `requestDigest` 非空，并且在 `:332-340` 的 exact-retry
  分支之前没有验证 SHA-256 形状、fact digest 或由完整 outer request 重算 request digest。fact digest 的重算直到
  `:366-368`，outer digest 在 coordinator 内从未重算。`RemoteTaskRunCoordinator` 与该方法均为 public，未来
  same-process activation/host 代码可绕过 endpoint 直接调用。
- 影响：首个 direct caller 可把与 wire bytes 不相符的任意 digest 固化进 accepted record/receipt；随后 DHXY 保留的
  合法 same-byte retry 会变成 `IDEMPOTENCY_CONFLICT`，该 resumed revision 永久无法收敛。已有 record 的 direct retry
  还可在验证 fact 自身之前命中回放。安全权威因此不在 coordinator 原子写门内，违反 no-raw-confirm-bypass。
- 返修条件：在 coordinator synchronized 写门内部、任何 existing-record/idempotency 判断之前，验证
  `requestDigest` 为 SHA-256、重算并验证 `factDigest`，并对 exact scope/action/taskRunId/requestId/finalized fact 组成的
  完整 `RemoteTaskRunActionRequest` 重算 outer digest。也可把 coordinator API 收敛为接收完整 typed request，但 endpoint
  仍可保留重复的边界校验。任一不符 typed `INVALID_REQUEST`，不得读取/回放/写 confirmation record。

### P2-1：registry 接受 receipt 时漏验 taskRunId，exact receipt correlation 少一维

- 证据：DHXY `RemoteTaskRunRegistry.markAccepted(...)`（`RemoteTaskRunRegistry.java:405-418`）比对
  requestId/requestDigest/factDigest/toRevision，却未比对 `receipt.taskRunId` 与 retained request 的 taskRunId。HTTP client
  当前在 `HttpRemoteTaskRunApiClient.java:540-549` 做了该比对，但 registry 是清除 pending slot 的最终本地 CAS 门，不能
  假设所有当前/未来 `RemoteTaskRunApiClient` 实现都正确。
- 影响：错误 client/错误 receipt 在其余字段碰巧相关时可清除另一个 run 的 readiness slot，造成新 revision 永久不确认。
- 返修条件：`markAccepted` 在清 slot 前同时精确比对 receipt.taskRunId 与 retained request.taskRunId；不符必须
  `STALE_HANDLE_IGNORED`，不改变 slot。

### P2-2：ledger terminal publication 与 ledgerRevision 不是同一 monitor 原子事件

- 证据：DHXY `RemoteOperationLedger.complete(...)` 在 `RemoteOperationLedger.java:86-90` 先于 monitor 外执行
  `CompletableFuture.complete`，之后才进 monitor 增加 `ledgerRevision`；而 `quiescenceSnapshot(...)` 在
  `:94-119` 持 monitor 读取 `isDone()` 和 revision。并发 snapshot 可看到 outcome 已 terminal、计数为零，却仍携带旧
  ledgerRevision，并在 `withCurrentSnapshot(...)` 中 materialize readiness 后，complete 才补增 revision。
- 影响：ready fact 的 operationLedgerRevision 不能证明它覆盖了促成 zero-in-flight 的 terminal transition，审计/CAS
  代次与事实观察不一致。
- 返修条件：把“terminal 可见性”和 ledgerRevision 前进收敛为同一个 monitor-guarded 状态转换，使 snapshot 不可能观察
  `done=true` + 旧 revision；不得取消、重排或改变 input bundle 的既有完成语义。

### P2-3：批准设计要求独立固定 10s readiness timeout，实现仍复用任意 lifecycle requestTimeout

- 证据：Design Repair #1 R3 明确 readiness HTTP 使用固定 10s、独立于 poll wait；当前
  `HttpRemoteTaskRunApiClient.java:30,42-49,245` 只有调用方传入的任意正数 `requestTimeout`，
  `confirmResumedExecutorReady(...)` 没有专用上限。`RemoteCommandPollingLoop.flushExecutorReadinessOnce()` 是同步调用，
  因而配置为较长 lifecycle timeout 时会超过批准的 bounded send 门。
- 影响：一次 readiness 调用可长时间阻塞 command poll，破坏“每个边界最多一条且最多固定 bounded HTTP”的防饥饿保证。
- 返修条件：为 `CONFIRM_RESUMED_EXECUTOR_READY` 使用独立固定最大 10 秒的 HTTP request timeout（其它 lifecycle
  action 继续保持现有 timeout），不得新增自动 retry/thread/poller。

### 返修与复验门

1. Worker A 追加 `External Worker A - Implementation Repair #1 - 2026-07-12`，只列上述四点的精确 diff 与时序证明。
2. 继续保持双仓 wire 字段/digest parity、same retained bytes/id、exact handle CAS、pure mechanical fact、host/cohort dormant，
   不改业务 phase/retry/fallback/click/navigation/stop/pause 语义。
3. 重跑 Cloud `mvn -q clean package`（不可 skip）与 DHXY `mvn -q -DskipTests compile`；不新增/恢复 DHXY tests，
   不启动 application/server/host/poller/UI/capture/OCR/input，不做 Git mutation。

`无已批准业务差异；按基线等价迁移。`

## External Worker A - Implementation Repair #1 - 2026-07-12

针对 `Local Implementation Review #1 - BLOCKED`（P0=0/P1=1/P2=3）四点定点返修。只改下列 4 个已批准写集内文件，未扩大范围，未覆盖两仓任何在途改动（每处修改前先读当前内容），未改业务语义。

### P1-1 修复：digest 权威移入 coordinator synchronized 写门

- Cloud `remote/run/RemoteTaskRunCoordinator.java`：`confirmResumedExecutorReady` 签名收敛为接收完整 typed
  `RemoteTaskRunActionRequest`（新增 import `RemoteTaskRunAction`/`RemoteTaskRunActionRequest`）。在 synchronized 写门内部、
  **任何 binding/existing-record/idempotency 读取之前**，依次强制：action 必须为 `CONFIRM_RESUMED_EXECUTOR_READY`；
  `requiredRequestDigest()` 做 SHA-256 形状校验；`RemoteProtocolDigests.computeResumeFactDigest(fact)` 重算并核对
  factDigest；`RemoteProtocolDigests.computeTaskRunActionDigest(request)` 用完整 typed request 重算并核对 outer
  requestDigest。任一不符抛 typed `ConfirmationRejectedException(INVALID_REQUEST)`，不读取、不回放、不写 confirmation
  record。原方法尾部（旧 :366-368）的迟到 fact-digest 校验删除（已被写门前置校验覆盖）。direct same-process caller 与
  HTTP 路径现在过同一原子门，无法把与 wire bytes 不符的 digest 固化进 record，合法 same-byte retry 不再可能被污染为
  `IDEMPOTENCY_CONFLICT`。
- Cloud `api/RemoteTaskRunEndpoint.java`：调用点改为 `coordinator.confirmResumedExecutorReady(request)`；endpoint 原有
  双 digest 边界校验按 review 允许保留（重复校验，非权威）。

### P2-1 修复：registry 最终 CAS 门补 taskRunId 维度

- DHXY `cloud/remote/RemoteTaskRunRegistry.java` `markAccepted`：在既有
  requestId/requestDigest/factDigest/toRevision 比对之外，增加
  `handle.retainedSend().request().getTaskRunId()` 与 `receipt.getTaskRunId()` 精确相等比对；不符返回
  `STALE_HANDLE_IGNORED`，slot 零改动。最终本地 CAS 门不再信任任何 client 实现的前置 correlation。

### P2-2 修复：terminal 可见性与 ledgerRevision 同一 monitor 原子事件

- DHXY `cloud/remote/RemoteOperationLedger.java`：新增 monitor 守护的 `Set<RequestKey> terminalRequests`。
  `complete(...)` 顺序改为：`synchronized(monitor){ terminalRequests.add(claim.requestKey) 失败即抛 already-complete;
  ledgerRevision++ }`，然后在 monitor 外完成 waiter future（唤醒语义不变，双重 already-complete 抛出保留）。
  `quiescenceSnapshot(...)` 改为按 `terminalRequests.contains(key)` 判定 terminal，不再读 `future.isDone()`。
  由此 snapshot 不可能观察到 done=true + 旧 revision：terminal 发布与 revision 前进是同一临界区的单一状态转换。
  input bundle 的既有完成语义未取消、未重排（future 完成路径原样，仅移出快照可见性判定）。

### P2-3 修复：readiness 专用固定 10s HTTP timeout

- DHXY `cloud/remote/HttpRemoteTaskRunApiClient.java`：新增
  `READINESS_REQUEST_TIMEOUT = Duration.ofSeconds(10)`；`executeResponse` 对
  `CONFIRM_RESUMED_EXECUTOR_READY` 使用该固定上限（其余 lifecycle action 保持构造时配置的 requestTimeout），timeout
  异常消息随 effective timeout。未新增自动 retry/thread/poller；poll boundary 单条 bounded 发送的防饥饿门恢复成立。

### 时序证明要点

- P1-1：验证链 `action -> shape -> factDigest -> outer digest` 全部位于 coordinator monitor 内且先于
  `bindingsByTaskRunId.get` 与 existing-record 分支；因此"先回放后验证"与"绕过 endpoint 直写"两条路径都不存在。
- P2-2：`(terminal add + revision++)` 为单临界区；快照读取同一 monitor。future 完成在其后，仅影响 waiter 唤醒时刻，
  不影响快照判定。并发快照要么见 (not-terminal, revision R)，要么见 (terminal, revision >= R+1)，无第三种交错。

### 构建证据

- Cloud `mvn -q clean package`：exit 0；Surefire 4 suites / 21 tests / 0 failures / 0 errors / 0 skipped。
- DHXY `mvn -q -DskipTests compile`：首轮因 DHXY 侧 Lombok getter 风格（`getTaskRunId()` 而非 record 访问器）报一处
  编译错，修正同一行后重跑 exit 0。未新增/恢复/运行任何 DHXY tests。
- 未启动 application/server/host/poller/UI/capture/OCR/input；未发 HTTP；无任何 Git mutation。双仓 wire 字段/digest
  parity、same retained bytes/id、exact handle CAS、pure mechanical fact、host/cohort dormant 均保持。

`无已批准业务差异；按基线等价迁移。` Worker A 自审 P0=0/P1=0/P2=0（不构成父级 APPROVED）。Repair #1 到此停止，等待父级复审；heartbeat 保持运行。

## Local Implementation Review #2 - APPROVED - 2026-07-12

父级已对 Repair #1 的四个修改点逐段复审，并亲自完成 fresh 双构建。结论：**APPROVED，P0=0/P1=0/P2=0**。

- Cloud `RemoteTaskRunCoordinator.confirmResumedExecutorReady(...)` 现在以完整 typed request 为唯一参数；在 synchronized
  写门内、读取 binding/既有 confirmation 之前，依次校验 action、SHA-256 形状、重算 factDigest 与重算 outer
  requestDigest。same-process caller 与 HTTP caller 均不能绕过 digest 权威。
- Cloud endpoint 只保留重复边界校验，最终写入仍统一进入 coordinator 原子门；合法同字节 retry 的
  requestId/requestDigest/factDigest correlation 未改变。
- DHXY `RemoteTaskRunRegistry.markAccepted(...)` 已把 receipt.taskRunId 纳入 exact handle CAS；错 run receipt 不能清除
  当前 readiness slot。
- DHXY `RemoteOperationLedger` 在同一 monitor 内原子发布 terminal marker 并推进 ledgerRevision；quiescence snapshot
  只读该 marker，future 仅在锁外负责唤醒 waiter，不再产生 terminal=true/旧 revision 观察窗。
- DHXY readiness HTTP action 使用独立固定 10 秒 timeout，其余 lifecycle action 继续使用原配置；没有新增 retry、线程
  或 poller。
- 父级 fresh Cloud `mvn -q clean package` exit 0：4 suites / 21 tests / 0 failures / 0 errors / 0 skipped；shaded JAR
  119,507,069 bytes，SHA-256 `8D934E8FCF5B467B3D39014DC4F45D765051AF9315E02B3C7DF338B38B8DBBA8`。
- 父级 fresh DHXY `mvn -q -DskipTests compile` exit 0；按 no-local-test mode 未运行或新增 DHXY tests。

本批准只关闭 resume executor-readiness 原子确认实现切片，不激活 host、Task/Service cohort 或业务 rehydration。
**无已批准业务差异；按基线等价迁移。**
