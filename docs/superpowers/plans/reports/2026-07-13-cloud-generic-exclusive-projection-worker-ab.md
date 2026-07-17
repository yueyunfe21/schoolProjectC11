# Cloud Generic Retained Exclusive Projection - Internal Worker AB

## Parent Task Brief #1 - `W-TTR-RX3-D1` - 2026-07-13T15:22:00-04:00

### 目标

为完整 `W-TTR-1`（Cloud `TaskTransactionRunner` + assembly/lifecycle）设计唯一缺失前置：generic retained exclusive
step projection。该 projection 必须允许 Cloud 业务继续在 Cloud 决策，只把精确机械 capture/input bundle 经既有
`RemoteGameClientPort` 下发；不得把 Cloud callback/业务 pass 下放 DHXY，也不得创建第二 turn/input/owner authority。

### 开工必读与事实基线

先完整读取：

1. `D:\mavenProject\DHXY\AGENTS.md`、`docs\DHXY_CONTEXT.md`、`docs\业务逻辑.md`；
2. 本日志与 `2026-07-13-cloud-task-transaction-runner-worker-aa.md` 真实 EOF，尤其 W-TTR-1 API/异常/finally 矩阵；
3. Internal Z 已冻结交付的 Cloud/DHXY whole-pass exclusive owner、current-context slot、retained ledger、R0 final-consume、
   local handler/input scope 源码；
4. DHXY HEAD `0114604e` 的 `TaskTransactionRunner`、`TaskTurnCoordinator`、`InputSequences` 与 transaction DTO；
5. 两仓最新 `git status`，保护全部 dirty/untracked，不回滚、不覆盖、不提交。

### 本轮唯一写集与门禁

- 唯一写集：仅本 append-only 日志。
- **Design #1 only**：父级 `DESIGN APPROVED` 前 Java/Maven/schema/resources/tests/host/caller 全冻结。
- 不启动 application/server/host/Task/poller/UI/capture/input，不新增/恢复 DHXY tests，不执行 Git mutation。

### Design #1 必答项

1. 逐方法证明为什么 Z 的专用 `SUMMON_SKILL_WHOLE_PASS` 不能直接复用，并指出可复用的唯一 authority/ledger/fence 基元；
2. 给出 generic projection 的 closed typed operation/state/handle 形状：stable semantic action identity、exact
   scope/taskRun/window/stopEpoch/runRevision/bindingGeneration、pause-resume 同一 in-flight identity、terminal/replacement invalidation；
3. 给出 acquire -> typed mechanical calls -> release/abort/unknown -> final-consume 的线性化与异常矩阵；`UNKNOWN`
   不得重发 physical action、不得伪装 `FAILED/STOPPED`，late final 必须复用 retained request/outcome；
4. 证明 local side-effect 前和 input-worker admission/checkpoint 的三道 fence；不得开放 raw request/poll/outcome public bypass；
5. tenant/run/global 容量、duplicate、restart/no-restore、orphan 与 cleanup 规则；不得新增 TTL/takeover/自动 retry；
6. 与 W-TTR-1 的 exact package-private seam：runner 如何取得 capability，assembly/lifecycle 如何创建、续代、失效；
7. 最小精确 New/Modify 文件表，必须优先原位扩展现有 Z authority/retained state，禁止第二 owner/map/wrapper/helper 链；
8. 依赖 DAG、与 External A/B 及其它切片的写集冲突、Cloud `mvn -q clean package` 门、独立 runtime 验收点；
9. 明确 `无已批准业务差异；按 0114604e 基线等价迁移`，并列出仍不能由代码静态证明的运行证据。

若当前 Z 结构无法在不重开第二 authority 的情况下承载 generic projection，必须写 `BLOCKED`、精确源码证据与最小缺失
seam，不得虚构 temporary API。Worker self-QA 只算 QA，不构成父级批准。

## Internal Worker AB - CLAIMED - 2026-07-13T15:33:24-04:00

- task=`W-TTR-RX3-D1`（Generic Retained Exclusive Projection Design #1）
- claimedAt=`2026-07-13T15:33:24-04:00`
- uniqueWriteSet=仅 `docs/superpowers/plans/reports/2026-07-13-cloud-generic-exclusive-projection-worker-ab.md`
- note=design-only；父级批准前 Java/Maven/schema/resources/tests/host/caller 全冻结；不运行 application/Task/UI/capture/input，不执行 Git mutation。Worker self-QA 不构成 Approved。

## Internal Worker AB - W-TTR-RX3-D1 Design #1 - 2026-07-13

### 0. 结论与基线

**结论：DESIGN COMPLETE，当前结构可原位泛化，不需要第二 authority/map；不是 `BLOCKED`。** 必要的最小新增执行 seam 是
DHXY 单个 `InputActionRequest` 的 retained-session mode：ACQUIRE 后同一现有 `InputActionWorker` 持有该 request，后续只接收闭合的
CAPTURE/INPUT_BUNDLE/RELEASE/ABORT step；它不是第二 input owner，也不另建 session registry。Cloud 继续原位使用 Z 的
`CloudTaskExclusiveInteractionAuthority.entries/liveByTenant/globalLive` 单一 owner/capacity 账本。

- DHXY business baseline：branch=`thin-client-design`，HEAD=`0114604e1ff5...`；已用
  `git show 0114604e:src/main/java/com/bot/dhxy/task/transaction/{TaskTransactionRunner,TaskTurnCoordinator}.java` 与
  `InputSequences.java` 核对 committed `run/runDynamic/runExclusive`、metric/finally/turn leave 语义。
- Cloud 只读锚：branch=`navigation-migration`，HEAD=`3b988caa...`；generic 设计以当前已冻结的 Z working-tree 源码为前置事实，
  不把 AB 的判断冒充 Z parent approval。
- 两仓均有大量他人 dirty/untracked；DHXY 的 `cloud/remote`、`InputActionQueue/Request/Scope/Worker` 与 Cloud 的
  `remote` 包均属于共享在途面。本任务没有回滚、覆盖、整理或写入其中任何文件。
- 已读 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/业务逻辑.md`、AA/Z 日志真实 EOF 与 Parent Brief #1；本设计只迁移
  scheduling/ownership/projection/mechanical plumbing，不改变五倍/修罗或其它 Task 的 phase、prompt、OCR/template、click、
  navigation、fallback、retry、park/yield、verification 或 expiry 决策。

### 1. 源码证据：为什么专用 Z 不能直接当 generic projection

| 证据 | 当前源码事实 | 设计判定 |
|---|---|---|
| Cloud `CloudTaskExclusiveInteractionAuthority:165-277` | 唯一业务入口是 `executeSummonSkillWholePass(...)`；固定创建 `ActionAddress("summon-skill","whole-pass")`，调用 `commandPort.executeSummonSkillWholePass(...)` | 不能给 generic runner 使用；它没有 ACQUIRE/CAPTURE/INPUT_BUNDLE/REBIND/RELEASE/ABORT 的闭合投影 |
| Cloud `CloudSummonSkillWholePassCapability:32-49` | public capability 只接受 Summon 专用 `WholePassIntent(expectedSkillCount/trust/startSlot/skipCorner)` 并返回专用 result | 把它交给 runner 会把召唤兽业务参数伪装成通用 transaction contract |
| DHXY `LocalRemoteGameCommandHandler:704-831` | `SUMMON_SKILL_WHOLE_PASS` 在一个 `submitRemoteExclusiveAndWaitDetailed` callback 内直接执行 `summonSkillService.cleanSummonSkillsOnce(...)` | 业务 whole pass 位于 DHXY；直接复用会把 Cloud business callback/decision 再下放，违反 Parent Brief |
| DHXY `InputActionQueue:152-258` / `InputActionWorker:112-167` | 当前 exclusive callback 是一次排队、一次 worker admission、callback 完成才释放 worker | 可复用“单 worker 持有整个 exclusive pass”的机械事实，但不能复用 Summon business callback |
| Cloud `CloudTaskExclusiveInteractionState:23-29,108-306,453-464` | 已有 stable key、bindingGeneration、runRevision、nextStep 与 ACQUIRE/STEP/PAUSE/HANDOFF/RELEASE/ABORT/UNKNOWN 状态 | 这是 generic 状态基元；只需补 STEP/UNKNOWN 跨 pause 的合法 shape，不重开状态 owner |
| Cloud `CloudTaskExclusiveInteractionAuthority:20-31,593-632,673-707` | 已有 64/tenant、1000/global、一个 `entries` map、每 run 一个 `OwnerEntry.live/completed` 与 exact generation projection | 原位把 Summon-only `LiveInteraction` 收敛成单一闭合 variant；禁止再建 generic map |
| Cloud `CloudTaskRetainedActionState:62-123,156-190` + `CloudTaskRunActionLedger:170-288,638-656` | 已有 non-mintable CAPTURE/INPUT_BUNDLE handle、request byte binding、同一 occurrence、UNKNOWN 可被 exact late final 替换、final-consume reservation | 直接复用 Full R0 identity/ledger；control 只新增一种 non-renewable handle，不造第二 idempotency ledger |
| Cloud `RemoteGameCommandBroker:1086-1098` + `CloudTaskRunCommandExecutor:106-128,155-177` | retained same-byte request 只读 retained/late outcome，broker 明确 never requeues/redispatches | pause/UNKNOWN 后可解析 exact late final而不重放 physical action |
| DHXY `LocalRemoteGameCommandHandler:182-210` + `RemoteOperationLedger:95-119` | local ledger 在 current-registration admission 前先按 operation+requestId 查 exact digest；duplicate 直接等待同一 future | 即使原 request 的 runRevision 已跨 pause，exact duplicate 也只 join，不进入第二次 side effect |
| Cloud `RemoteFinalConsumptionCoordinator:22-112` | exact outcome object 经 reservation、checked mutation、ACK、local receipt/compaction 唯一收口 | generic control/step 必须沿用，不能“收到响应即释放” |

**可复用基元只有这些：**同一 Z authority/entry/quota、现有 immutable state policy、current-context slot/generation、Full R0
retained action ledger、broker exact duplicate/late-final、R0 final-consume、DHXY `RemoteTaskRunRegistry` 的单一 continuation handle、
现有单 `InputActionQueue/InputActionWorker` 和 stable pause token。Summon intent/result/local business callback 不可复用。

### 2. 单 owner 的 closed typed 形状

#### 2.1 Cloud 单一 session 记录

原位把 `OwnerEntry.live/completed` 的 Summon-only 类型改成一个闭合 variant，不增加 map：

```text
OwnerEntry.active : ActiveInteraction = SummonWholePass | GenericExclusive
OwnerEntry.completed : CompletedInteraction = SummonWholePass | GenericExclusive
```

`GenericExclusive` 只由 authority 构造，持有：

- existing `StableExclusivePassKey(scope,taskRunId,taskType,window,admissionStopEpoch,
  businessActionAddress,exclusiveSessionId)`；`runRevision` 明确不进入 stable identity；
- current `CloudTaskExclusiveInteractionState`、owner `Thread`、exact `GenerationProjection` lineage；
- 四类 package-private、non-mintable control action handle：固定 semantic slots
  `task-transaction-exclusive/acquire|rebind|release|abort`，每次新 session/rebind 只在前一 occurrence exact final-consumed 后由
  `CloudTaskRetainedActionState` 推进 occurrence；transactionName 永远只是诊断字段；
- 最多一个 `ActiveStep`：operation 只能是 CAPTURE 或 INPUT_BUNDLE，持有原 caller 的既有 opaque action handle、参数快照、
  retained request/outcome、request bindingGeneration/step；无 list/map、无并行 step；
- package-private nested `GenericSessionHandle`，仅供同 package runner 的 `Transaction` 持有；无 public raw id/getter。

同一 run 同时只能有一个 active variant。Summon 与 generic 互斥；若另一个 variant 已 live，fail closed，不能并建 owner。

#### 2.2 Wire closed types

- New `ExclusiveInteractionControlRequest`：`command={ACQUIRE,REBIND,RELEASE,ABORT}`、`exclusiveSessionId`、
  `bindingGeneration`、`step`、`priorRunRevision`（仅 REBIND）；context 继续携带 exact scope/taskRun/window/stopEpoch/runRevision/
  semantic address/requestId/digest。
- New `ExclusiveInteractionControlOutcome`：echo exact command/session/generation/step，并用 existing `CommonOutcome` 表达
  `EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN`；不加模糊字符串状态。
- New immutable `ExclusiveSessionStepRef(sessionId,bindingGeneration,step)`；`CaptureRequest` 与 `InputBundleRequest` 增 nullable
  closed ref。null 是原普通调用；non-null 必须由 authority 的 generic route 构造。digest/canonical JSON 必须包含其全部字段。
- New `RemoteOperation.EXCLUSIVE_INTERACTION_CONTROL`；CAPTURE/EXECUTE_INPUT_BUNDLE 仍是原 operation 和原 typed outcome，
  不复制 capture/input 业务 DTO。
- `RemoteGameClientPort` 保持 package-private；新增 control 方法及带 `ExclusiveSessionStepRef` 的 capture/input overload。
  `CloudTaskServicePort` 仍只接受既有 opaque `CaptureAction/InputBundleAction`，不公开 sessionId/request/poll/outcome ingress。

#### 2.3 DHXY 单一 local owner

- `RemoteTaskRunRegistry.RegistryEntry.inFlightExclusiveHandle` 仍是每 run 唯一 continuation owner；handle 原位增加
  `exclusiveSessionId/currentBindingGeneration/nextStep` 与一个 queue-minted opaque session reference，不增加 registry/map。
- ACQUIRE 用现有 `openInFlightExclusive` 铸造 exact handle；后续命令用新增的 package-private
  `requireInFlightExclusive(admissionSnapshot,command,stepRef)` 查同一 entry/handle，不能按 raw sessionId 全局搜索。
- `InputActionRequest` 增 retained-session mode 和容量为 **1** 的 step handoff；同一 request 被现有单 worker 从 ACQUIRE 持有到
  RELEASE/ABORT。没有新 executor/thread/poller；禁止 session step 内再次调用 `submit*`，所以不存在 queue-in-queue deadlock。
- CAPTURE 在 handler 线程执行，但必须先 reserve exact nextStep，并在 capture 前/后复验 handle+binding；该期间 worker 仍被
  session request 占有，因此其它窗口的 physical input 不可穿插。INPUT_BUNDLE 交给同一 request 的单槽 handoff，由同一 worker
  调现有 `execute(...)`；RELEASE/ABORT 只向该 request 发 terminal signal并等待 worker 确认退出。
- 原 Summon path 可继续作为 `SummonWholePass` variant 使用现有 callback；generic path绝不接受 Supplier/business callback。

### 3. 生命周期、pause/resume 与 replacement

1. **Initial ACTIVE：**existing assembly 的 `prepareInitialGeneration` 继续创建同一 `GenerationProjection`；runner 首次
   `beginExclusive` 才 retain ACQUIRE、分配 sessionId并发送 control。ACQUIRE exact final前不运行 Cloud business。
2. **PAUSED，no step in flight：**authority 在同一 entry 内把 ACTIVE park；DHXY registry 在同一 handle 上
   `CallbackActive -> CallbackPaused`，stable pause token 最后 request pause。worker 保持 session owner，不释放、不计 TTL。
3. **PAUSED，STEP_BOUND/UNKNOWN：**`CloudTaskExclusiveInteractionState.parkPaused/handoff` 必须原位允许 STEP_BOUND 和
   `UNRESOLVED_FENCE_HELD`，保留 `unresolvedFrom/nextStep/retained request`。不能把 pause 翻译成 ABORT/FAILED/STOPPED。
4. **Resume：**slot 先按现有 H/K 顺序发布下一 runtime，Cloud bindingGeneration +1；若旧 step still bound，先只接受/读取
   该 retained request 的 exact late final（broker 不 redispatch），final-consume 后才发送 REBIND。无旧 step则直接 REBIND。
   REBIND exact final后更新同一 local handle 的 generation，worker stable token 最后 resume；sessionId/action identity/step cursor不变。
5. **Old generation：**old runner/projection/servicePort 任一调用均因 slot generation、runRevision、bindingGeneration 或 owner thread
   不匹配 fail closed；不得 fallback 到普通 capture/input。
6. **Terminal/STOPPING：**Cloud `closeTerminal` 在 slot terminal close 前尝试 exact ABORT（现有 assembly 顺序
   `CloudTaskRunAuthorityAssembly:350-370` 已先 exclusive close再 slot/H/ledger retire）。若已有 UNKNOWN，不能另发 ABORT；保留
   unresolved fence/quota。DHXY terminal publication先 invalidate handle、resume token，worker checkpoint退出，随后 registry detach。
7. **Replacement：**taskRun/window/stopEpoch/entry generation 任一变化都不是 resume；旧 handle永久 invalid，新 run只能经正常
   lifecycle重新 acquire。禁止 old session takeover/new-generation alias。

### 4. 线性化、异常与 final-consume 矩阵

| 阶段 | 唯一 local 线性化点 | exact final 后 Cloud 转移 | `UNKNOWN`/异常规则 |
|---|---|---|---|
| ACQUIRE | worker 通过 one-shot admission、占有该 session request，registry `QueuedActive -> CallbackActive`；响应只能在两者完成后发出 | `ACQUIRED -> ACTIVE`；明确 `NOT_EXECUTED` -> unacquired terminal，runner `actionAllowed=false`；明确 STOPPED 只形成 typed stop | transport/timeout -> `UNRESOLVED_FENCE_HELD`；不运行 business、不另发 acquire |
| CAPTURE step | reserve exact nextStep 后，最后一次 bound-window/registry fence通过，紧接 `BoundWindowCaptureService.captureRegion` 调用 | exact typed outcome经 R0 checked mutation；非 UNKNOWN 才 `completeStep(nextStep+1)`，业务自行解释 captured/not-executed | capture 是否发生不明 -> 保留同 request/step；不得生成空图或 FAILED |
| INPUT_BUNDLE step | session worker 的 `tryStartStep` 在第一条 physical action 前；bundle保持原子，后续每 action/checkpoint再复验 | exact EXECUTED/NOT_EXECUTED/STOPPED 原样给业务，final-consume 后 nextStep+1；runner不替业务决定结果 | UNKNOWN 不重发 actions；只接受 broker retained request 的 exact late final |
| REBIND | registry mutationLock 内 exact paused-successor handle/generation CAS；token resume最后发生 | generation+1 可用；同 session/step cursor继续 | UNKNOWN 保留旧 local generation + Cloud fence；新 generation不得发 mechanical step |
| RELEASE | session worker确认 prior step已终结、写 RELEASED terminal并退出 request；此刻全局 worker owner释放 | exact final-consume后 session `RELEASED`；runner才允许写 transaction outcome | UNKNOWN 时 transaction outcome仍 null、turn可leave但 exclusive fence/quota保留 |
| ABORT | worker停止接收新 step，当前未开始step取消；已开始step先走checkpoint/typed终态，然后写 ABORTED并退出 | exact final-consume后 `ABORTED`；stop可写 STOPPED，普通异常仍原对象上抛 | cleanup UNKNOWN 不改写原异常；加 suppressed typed cleanup uncertainty，session fence保留 |
| late final | broker `acceptLateResolution` 对同 requestId+digest 的 retained outcome CAS；local duplicate只join同 future | authority 用原 `ActiveStep/control handle` 调同一 `RemoteFinalConsumptionCoordinator` | 不创建 request/action/attempt，不 timer retry，不 raw public poll |
| final-consume | Cloud `RemoteFinalConsumptionCoordinator.consumeFinal` 的 checked mutation -> exact ACK -> local receipt/compaction | occurrence完成后才释放 step/control detail；session release/abort完成后才释放 authority quota | ACK/compaction半提交沿 Full R0 保留 exact witness；不得提前清 quota |

普通 `RuntimeException/Error` 不转成业务 `FAILED`：runner按 AA 模板先 exact ABORT，cleanup failure只 suppressed，原异常重抛。
`TaskCheckpointTransitionException(PAUSED)` 只 park，不 ABORT。任何 UNKNOWN 都不是 `FAILED/STOPPED` 的证据。

### 5. 三道 local fence（所有副作用必须同时通过）

1. **F1 handler/ledger admission：**strict digest+closed payload decode -> `RemoteTaskRunRegistry.commandAdmissionSnapshot` ->
   `RemoteOperationLedger.claim` -> exact registration/bound window二次校验 -> same handle/session/generation/nextStep reserve。duplicate 在
   ledger 处返回同 future，不再进入 side effect。
2. **F2 input-worker admission：**stable pause wait之后、focus/第一物理动作之前，`InputActionWorker` 调
   `InputActionRequest.checkDetailedSafety` + registry `admitInFlightExclusive/checkInFlightExclusive`；必须匹配 tenant/user/device/
   clientSession/taskRun/taskType/window HWND/process/playerEpoch/stopEpoch、entry generation、sessionId、bindingGeneration、step。
3. **F3 immediate checkpoint：**INPUT_BUNDLE 在每个 `tryStartStep`、每个 action、focused fallback前后、分段 sleep 都复用
   `checkDetailedSafety`；direct-input callback 继续由 `InputActionScope.checkpoint:51-73` 复用同 request。CAPTURE 不伪装成
   input action，而是在 capture 调用紧前和返回紧后各做同一 registry+binding fence；post fence失败只能返回 typed UNKNOWN，
   不能发布图像为可信 fact。

任何 F1/F2/F3 stale/paused/terminal/mismatch 均不能走普通 queue fallback。Cloud public 面仍只有 opaque action handle + typed
Service call；`RemoteGameClientPort`、generic session handle、late-final accept、control action retain 全为 package-private。

### 6. 容量、duplicate、restart/no-restore、orphan/cleanup

- **Cloud：**保持 `1 live interaction/run`、`64 live/tenant`、`1000 live/global`（现有 authority 常量）；Summon+generic共用。
  Full R0/broker既有 retained/pending/input-flight caps继续叠加；control和session-bound INPUT计入 input-flight，不豁免。
- **DHXY：**保持 registry `10000 global/1000 owner`、operation ledger `1000 semantic slots/64 current details/64 receipt outbox`；
  每 session step handoff固定容量1、每 run一个 handle、全进程仍一个 input worker。超限明确 NOT_EXECUTED/CAPACITY，不等待、不重试。
- **Duplicate：**same operation+requestId+digest或同 semantic handle+same bytes只join/返回retained final；session/参数/step不同为
  idempotency/semantic conflict。completed session只可返回同 final，不可复活 owner。
- **Restart/no restore：**两侧 owner均 process-local，不反序列化 session。DHXY 重启丢失 owner时 Cloud只能收到 UNKNOWN并持 fence，
  不自动 acquire。Cloud incarnation变化已有 `RemoteOperationLedger:51-72` coordinated-restart fail-closed；poll loop在确认新 incarnation
  的同一错误路径显式调用 registry owner invalidation，resume token使旧 worker退出，但仍拒绝新命令，直到上层完成新的 lifecycle。
- **网络断连不是死亡证明：**同 incarnation 暂时断连时 local session保持，不TTL、不释放给其它窗口、不takeover；只接受 exact
  reconnect duplicate、terminal/stop或confirmed incarnation change。
- **Orphan cleanup：**明确 RELEASE/ABORT exact final释放；terminal publication即使 Cloud outcome未知也会让 local fence失效并退出
  worker，但 Cloud retained/quota继续保留到 exact late final+final-consume。没有 late final时永久 fail-closed；只能由显式协调重启/
  新 taskRun处理，不能后台定时清理。

### 7. W-TTR-1 的 exact package-private seam

- `TaskTransactionRunner` 仍按 AA EOF 放在 `com.yueyunfe.dhxy.cloudbrain.remote`。其 package-private constructor由
  `CloudTaskRunAuthorityAssembly.TaskServiceRuntime` 传入同一 `CloudTaskTurnHandle`、`GenerationProjection`、retained action state
  与 `CloudTaskExclusiveInteractionAuthority`；不注入 raw port/ledger/id。
- public `beginExclusive(name,expected,yield)` 不增加 raw参数。它在 `turn.enter` 后调用 authority package-private
  `acquireOrJoinGeneric(projection,retainedState,turnHandle)`；authority用固定 control semantic slots而不是 diagnostic `name` 铸造
  identity，返回 nested non-mintable `GenericSessionHandle` 给 `Transaction`。
- `CloudTaskServicePort.capture/executeInputBundle` 已持同一 authority/projection。generic live存在时，authority要求 exact current
  projection + owner thread + active transaction后附 `ExclusiveSessionStepRef`；其它线程/旧 generation必须拒绝，不能退回普通调用。
  无 generic live时保持原 ordinary mechanical path。
- `Transaction.complete*` 先取得 exact RELEASE final再写 outcome；`stop/fail`先 exact ABORT；PAUSED transition只park；close仍按AA
  `metric -> turn.leave`。package-private `beginExclusiveWithin(parentTransaction,...)`只能校验并join同一 handle/turn，不重新 acquire。
- initial/resume assembly每代创建新 runner；retained state和 authority entry不换。resume runtime发布后第一次 join先 resolve old bound step，
  再 exact REBIND；旧 runner随旧 projection失效。lifecycle terminal仍调用 assembly现有 `closeTerminal`顺序。
- RX3 本身 **不修改** assembly/lifecycle/runner；其交付后 AA 的 `W-TTR-1` 仍保持 1 New + 2 Modify：
  `TaskTransactionRunner.java`、`CloudTaskRunAuthorityAssembly.java`、`CloudTaskRunRetainedLifecycleActivationAdapter.java`。

### 8. 最小精确 New/Modify 文件表（父级批准后的目标，不是本任务写集）

#### Cloud RX3 core/protocol

| Action | Exact file | 责任 |
|---|---|---|
| Modify | `remote/CloudTaskExclusiveInteractionAuthority.java` | 单 entry闭合variant、generic acquire/step/rebind/release/abort/late-final、同quota |
| Modify | `remote/CloudTaskExclusiveInteractionState.java` | STEP_BOUND/UNKNOWN跨pause/handoff；exact cursor/generation transition |
| Modify | `remote/CloudTaskRetainedActionState.java` | package-private non-renewable control handle/固定slots |
| Modify | `remote/CloudTaskRunActionLedger.java` | 新control operation的closed handle/renewal禁令与终态校验；不加map |
| Modify | `remote/RemoteFinalConsumptionCoordinator.java` | control与Summon同为禁止attempt-renewal |
| Modify | `remote/CloudTaskServicePort.java` | ordinary vs exact generic route；public签名不暴露session/raw port |
| Modify | `remote/RemoteOperation.java` | 加 `EXCLUSIVE_INTERACTION_CONTROL` |
| Modify | `remote/RemoteRequest.java` / `remote/RemoteOutcome.java` | sealed permits新control type |
| New | `remote/ExclusiveInteractionControlRequest.java` | closed ACQUIRE/REBIND/RELEASE/ABORT request |
| New | `remote/ExclusiveInteractionControlOutcome.java` | exact control typed outcome |
| New | `remote/ExclusiveSessionStepRef.java` | immutable session/generation/step wire ref |
| Modify | `remote/CaptureRequest.java` / `remote/InputBundleRequest.java` | optional exact step ref；普通null canonical bytes不变 |
| Modify | `remote/RemoteGameClientPort.java` | package-private control与session-bound mechanical methods |
| Modify | `remote/CloudTaskRunCommandExecutor.java` | build/reuse retained control与step request；不redispatch retained |
| Modify | `remote/CloudTaskRunExecutionGate.java` | current context下构造/验证新closed request |
| Modify | `remote/RemoteGameCommandBroker.java` | exact request/outcome correlation、input-flight分类、late final |
| Modify | `remote/RemoteCommandOutcomeEnvelope.java` | exhaustive control outcome decode |
| Modify | `remote/RemoteProtocolDigests.java` | control/step ref canonical digest |

#### DHXY RX3 local mechanics

| Action | Exact file | 责任 |
|---|---|---|
| Modify | `cloud/remote/RemoteGameOperation.java` | 加 closed control operation |
| New | `cloud/remote/RemoteExclusiveInteractionControlCommandPayload.java` | strict control payload |
| New | `cloud/remote/RemoteExclusiveInteractionControlOutcomePayload.java` | strict control result |
| New | `cloud/remote/RemoteExclusiveSessionStepRef.java` | immutable local decode value |
| Modify | `cloud/remote/RemoteCaptureCommandPayload.java` / `RemoteInputBundleCommandPayload.java` | optional exact step ref |
| Modify | `cloud/remote/RemoteOperationPayloadCodec.java` | strict decode/encode与unknown-field拒绝 |
| Modify | `cloud/remote/RemoteProtocolDigests.java` | 与Cloud canonical bytes逐字段一致 |
| Modify | `cloud/remote/RemoteOperationLedger.java` | control/input capacity分类；复用same request/semantic frontier |
| Modify | `cloud/remote/RemoteTaskRunRegistry.java` | 原位泛化唯一 handle、step/generation CAS、restart invalidation；不加map |
| Modify | `cloud/remote/LocalRemoteGameCommandHandler.java` | closed control branch、session-bound capture/input；无business callback |
| Modify | `cloud/remote/RemoteCommandPollingLoop.java` | confirmed incarnation change时显式invalidate旧session owner；无TTL |
| Modify | `input/action/InputActionQueue.java` | enqueue retained session request、bounded step handoff、release/abort wait |
| Modify | `input/action/InputActionRequest.java` | retained-session mode/opaque handle/容量1状态；无第二queue owner |
| Modify | `input/action/InputActionWorker.java` | 同一worker request内执行step loop并复用现有execute/checkpoint |
| No Modify | `input/action/InputActionScope.java` | 直接复用现有checkpoint；不加wrapper |
| Modify | `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md` | 父级批准后同步closed wire；本Design波保持冻结 |

没有 New authority/registry/executor/thread/poller/facade/helper-chain；没有 test/host/Task/caller 文件。若实施发现 sealed switch还有
未列 exhaustiveness 编译点，必须先回卡补表，不得以临时 default 绕过。

### 9. 依赖 DAG、共享写集与门禁

```text
Z frozen whole-pass authority/registry/input baseline
        + Full R0 FINAL APPROVED
        + W-TTR-0 source approved
                     |
                     v
         W-TTR-RX3 parent DESIGN APPROVED
                     |
       +-------------+------------------+
       |                                |
       v                                v
Cloud RX3 core+wire            DHXY RX3 wire+registry+single-worker session
       |                                |
       +------------- atomic protocol --+
                     |
       Cloud clean package + DHXY compile
                     |
                     v
        AA W-TTR-1 runner+assembly+lifecycle atomic
                     |
              Cloud clean package
                     |
       later Task/caller activation + independent runtime evidence
```

- **Internal Z：直接冲突。** authority/state/retained state、handler/registry/ledger、InputActionQueue/Request/Worker 均以Z真实稳定
  源码为基线；RX3实施必须等Z父级收口且无active writer，不能并发改。
- **External A：无写集冲突。** 当前只写 DialogChoice resolver日志；其host/inventory blocker与RX3无关。
- **External B：当前 leaf 无直接冲突。** 当前获准只写 `BoundLeaderPrecheckCaptureCapability.java`、
  `LeaderPrecheckMechanics.java`及B日志；但其后续 mount 会触及 handler/lifecycle/operation protocol，必须在RX3后重锚，禁止与RX3并发。
- **AA：顺序依赖。** RX3不碰AA的runner/assembly/lifecycle三文件；RX3通过后AA再原子落W-TTR-1。
- **其它 Cloud state/service worker：**若占 `CloudTaskServicePort/RunActionLedger/Broker/Digest`，按文件owner串行；不得用merge覆盖。
- **构建门：**Cloud RX3完成后从 `D:\mavenProject\dhxy-cloud-brain` 运行 `mvn -q clean package`；DHXY Java完成后从
  `D:\mavenProject\DHXY` 运行 `mvn -q -DskipTests compile`。无local test新增/恢复；若父级另点名test，再按explicit-test exception执行。

### 10. 静态代码不能证明的独立 runtime 验收证据

1. 两个真实窗口：A session 在 capture -> input bundle -> release期间，B physical input只排队且窗口/坐标不串；日志包含
   tenant/taskRun/window/session/generation/step。
2. 分别在“step之间”和“input sleep/动作中”PAUSE；resume后sessionId/requestId/actionId/semantic occurrence不变、
   bindingGeneration只+1、旧step无第二次click。
3. 丢ACQUIRE/INPUT/RELEASE响应后注入exact late final：Cloud在late final前不产FAILED/STOPPED、不释放quota；final-consume后
   local receipt/Cloud compaction一致。
4. duplicate same-byte control/capture/input只join；改一字段得到typed conflict且无side effect。
5. STOPPING/terminal/replacement恰在F1、F2、F3各边界发生时，旧handle不再产生capture/input；worker释放或UNKNOWN fence保留，
   不能出现新owner takeover。
6. Cloud进程incarnation变化、DHXY进程重启、短网络断连三种场景分别证明：confirmed restart cleanup、no-restore UNKNOWN、
   same-incarnation无TTL保留。
7. tenant第65个live、global第1001个live、同run第二session、本地step lane第二个并发step均typed fail-closed且无自动retry。

### 11. Self-QA（不是 Approved）

- 已覆盖 Parent Brief #1 九项：专用不可复用证据、closed shape、stable identity、线性化/UNKNOWN、pause-resume、三道fence、
  tenant/run/global capacity、restart/orphan、W-TTR package-private seam、文件表、DAG/冲突、build与runtime gate。
- P0/P1/P2 self-QA=`0/0/0`，仅表示AB设计自检；**不构成 Parent DESIGN APPROVED，也不放行任何 Java/Maven/schema/resources/
  tests/host/caller。**

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Internal Worker AB - W-TTR-RX3-D2 Design Repair #1 Delta - 2026-07-13T15:57:18-04:00

本 Delta 只修 Parent Design Review #1 的 P1×4；Design #1 已通过的单一 authority/ledger/worker、Cloud business 不下放、
closed typed mechanical step 与三道 fence 结论保持。以下内容覆盖 Design #1 中“断连永久持有”“wire REBIND”“ACQUIRE等待
terminal”“fixed control slot自动推进 occurrence”的相反表述。

### D2-1：恢复 committed 120 秒 unpaused budget

源码锚：`InputActionQueue.await:365-466` 从 enqueue 后固定 `120s` 开始等待，`isPauseRequested()` 时只移动
`lastCheckMs`、不扣 `remainingWaitMs`；remote deadline 路径的 `InputActionRequest.remainingDeadlineNanos:363-393` 已用
`deadlineCompensationNanos + accountedPauseNanos` 保证累计 PAUSE 只补偿一次。

1. generic retained session 的 budget 固定为 `120_000ms`，从该唯一 `InputActionRequest` 成功 enqueue 的单调时钟时点开始，
   覆盖 queued -> admitted -> Cloud mechanical calls -> RELEASE/ABORT terminal 的整个 local exclusive ownership；不是配置项、
   不是新 TTL，也不得按 wire timeout重置。
2. request 使用 existing `deadlineNanos + deadlineCompensationNanos` 记账；handler admission waiter 与 worker session loop都只读
   `remainingDeadlineNanos(System.nanoTime())`，PAUSE 均调用现有 `compensatePause(PauseWaitSnapshot)`。`progressLock` 下的
   `accountedPauseNanos` 消除双 observer 重复补偿；ACTIVE断连/Cloud business计算时间照常扣减，PAUSED时间不扣。
3. worker 等待下一 typed step时用 `min(1s, remainingBudget)` 的 interruptible poll；budget归零即停止接收新 step，取消未开始
   step，令当前已开始step走既有 checkpoint收口，完成 session `terminal` 后释放唯一 input worker。无额外 timer/thread/executor。
4. budget expiry 后在同一 registry handle 留 immutable terminal snapshot，供下一个 exact step/RELEASE/ABORT返回同一结果；
   不重新 acquire、不复活 request。local worker已释放，但 Cloud authority/R0 detail只在 exact outcome+final-consume后释放。
5. Design #1 的“同 incarnation 断连永久持有 local worker”作废：断连若未 PAUSE，最多消耗剩余120秒 unpaused budget；到期
   local owner释放，Cloud仍按 delivery uncertainty持 `UNKNOWN` fence。不得把本地已释放推断为原 step未执行。

#### 120 秒 terminal/UNKNOWN 矩阵

| 到期/中断位置 | 可证明事实 | local closed outcome | frame/permit/ledger 收口 |
|---|---|---|---|
| enqueue前或 `queue.offer` 拒绝 | request从未可达worker | ACQUIRE `NOT_EXECUTED/QUEUE_REJECTED` | 不建session permit/frame；registry handle detach；ACQUIRE ledger写exact final供R0消费 |
| queued且admission尚未发生，remove/cancel在`progressLock`胜出 | worker未持有、step=-1 | ACQUIRE `NOT_EXECUTED/ADMISSION_TIMEOUT` | 同一request先完成rejected admitted，再terminal；释放queue/registry reservation，无frame |
| worker-before-admit安全/identity/focus失败 | F2未线性化、无mechanical step | ACQUIRE `NOT_EXECUTED/WORKER_ADMISSION_REJECTED` | worker `finally`补齐两future；permit/handle关闭；ledger exact terminal |
| admitted后idle、无current step | owner曾取得但当前无副作用不确定性，worker释放可证明 | 后续control返回 exact `SESSION_BUDGET_EXPIRED_RELEASED`；新step `NOT_EXECUTED` | terminal snapshot保留到同一session closed response；不伪造transaction FAILED/STOPPED |
| CAPTURE在调用前最后fence到期 | capture未调用 | 当前CAPTURE `NOT_EXECUTED/BUDGET_EXPIRED` | 无frame；session terminal，worker释放 |
| CAPTURE已调用但post-fence/返回未形成exact final | 可能已读到窗口，可信发布不成立 | 当前CAPTURE `UNKNOWN/BUDGET_EXPIRED_AFTER_START` | 若有frame立即flush且不发布；retained request/outcome保留，禁止重拍 |
| INPUT_BUNDLE第一physical action前 | `startedStepIndex < 0` | 当前INPUT `NOT_EXECUTED/BUDGET_EXPIRED` | action permit取消；session terminal，worker释放 |
| 任一physical action已开始而bundle未exact terminal | 可能已有点击/按键 | 当前INPUT `UNKNOWN/BUDGET_EXPIRED_AFTER_START` | checkpoint停止后续action；worker释放；原request/action identity与local ledger detail保留 |
| admission waiter被interrupt | `admitted/terminal`在同一progress lock给出胜者 | 未admit且未start=`NOT_EXECUTED`；已admit或可能start=`UNKNOWN`/retained exact admitted | re-interrupt；不把interrupt直接翻成STOPPED；worker依cancel/checkpoint完成terminal |

既往已经 exact final-consumed 的 step 不因后续 session budget到期回退为 UNKNOWN；只对“当前未形成 exact final”的 step按上表判断。
RELEASE/ABORT若看到 budget terminal，返回同一 terminal snapshot并证明 local owner已释放；若同时存在 unresolved current step，
release事实可exact，但 transaction仍须等该 step late final，不能据 release合成业务 outcome。

### D2-2：删除独立 REBIND，复用现有 lifecycle publication

1. wire control enum精确收缩为 `ACQUIRE | RELEASE | ABORT`；删除 `REBIND`、`priorRunRevision`、REBIND request/outcome、control
   semantic slot、handler branch、ledger分类与runtime验收项。没有替代的 resume command。
2. Cloud 继续使用 existing `CloudTaskExclusiveInteractionAuthority.prepareResumeGeneration` + current-context H/K handoff：同一 entry/
   session/action/step保留，Cloud projection `bindingGeneration`对 exact PAUSED -> ACTIVE successor只增1。
3. DHXY 唯一 local handoff 是 `RemoteTaskRunRegistry.publishTransition:871-880`。原位扩展
   `PreparedEntryTransition`，把 generic handle 的 `nextBindingGeneration` 与 next continuation snapshot一起在
   `prepareEntryTransition:950-978` 计算；仅 exact `CallbackPausedSnapshot(previous) -> CallbackActiveSnapshot(target)` 可+1。
4. `applyPreparedWithoutToken:884-904` 在同一 `mutationLock` 下先发布 registration、pending readiness、handle snapshot、
   bindingGeneration/step cursor；`publishTransition` 最后且只调用一次 `pauseToken.resume()`。stale predecessor仍invalidate+clear，
   不做第二CAS、不让worker先醒。
5. resume后的第一个 session-bound CAPTURE/INPUT携带 Cloud H/K projection的新 generation；F1只与 registry已发布值比较。
   old in-flight request继续按其 retained old generation收 late final，但不能铸新step。generation mismatch typed fail-closed，
   不自动retry、不fallback ordinary route。

### D2-3：同一 local request 的 admitted/terminal 双 completion

`InputActionRequest` 的 retained-session mode精确拥有两个 top-level、exact-once completion：

```text
CompletableFuture<SessionAdmission> admitted
CompletableFuture<InputActionExecutionResult> terminal   // 复用现有 result
```

- `SessionAdmission = ADMITTED | REJECTED_NOT_EXECUTED | ADMISSION_UNKNOWN`，携带同一 requestId/sessionId、reason、
  `startedStepIndex` snapshot；它不是wire outcome，也不对外提供 raw future。
- worker完成F2、进入同一个 `inputCoordinator.callInputTransaction`、focus/safety均通过且即将进入 session step loop时，在
  `progressLock` 内线性化 `ADMITTED`。此后worker继续持有同一 request，不完成terminal。
- 所有 worker-before-admit terminal路径必须在同一个 `completeTerminal(...)` 中先补齐非ADMITTED结果，再完成terminal；
  `finally`仍兜底两者，禁止 admitted 永不完成。
- `LocalRemoteGameCommandHandler` 的 ACQUIRE：open exact registry handle -> enqueue -> **只 await admitted** -> 立即返回closed
  ACQUIRE outcome，使 `RemoteCommandPollingLoop:166-174` 能 submit outcome并继续poll CAPTURE/INPUT/RELEASE。
- CAPTURE在poll thread使用同一 registry handle执行；INPUT把容量1的typed step交给已admitted worker并等该step completion；
  RELEASE/ABORT发terminal signal并且只有它们等待 `terminal`。不得在session worker里nested submit。

#### enqueue/admission竞态与资源所有权

1. `queue.offer=false`：request原子完成 rejected admitted + NOT_STARTED terminal；handler关闭 registry handle，operation ledger owner
   写ACQUIRE NOT_EXECUTED；没有frame、input permit或worker ownership。
2. admission 120s到期且 `queue.remove=true`：同上；budget completion是唯一胜者。
3. remove失败时不猜测：在 `progressLock` 请求cooperative terminal并读取 admitted胜者。ADMITTED已胜则ACQUIRE使用原exact admitted
   结果（若transport随后丢失，Cloud是UNKNOWN、local ledger保留ACQUIRED）；未admit且未start则NOT_EXECUTED；任何可能start只报UNKNOWN。
4. waiter interruption同上并恢复interrupt flag；不能把poll thread interruption直接映射业务STOPPED。worker/queue permit只由
   terminal owner释放，handler不得双close。
5. worker-before-admit Throwable/stop/stale：无frame且无physical action，rejected admitted + NOT_EXECUTED terminal；registry detach与
   ledger completion总执行。admitted后Throwable按started evidence为NOT_EXECUTED或UNKNOWN，永不FAILED伪装。

这样同步poll没有自锁：ACQUIRE响应发生于session terminal之前；后续命令仍由同一poll thread顺序交付给已持worker的request。

### D2-4：retained task/phase state提供稳定 transaction action handle

新增一个真实 public capability `TaskTransactionAction`（不是raw DTO/wrapper）：构造器package-private，持有同一
`CloudTaskRetainedActionState` provenance、`ActionAddress(phaseCode,actionSlot)`、**由上层明确给定的 occurrence**、对应record与
该occurrence的closed control child handles。它不暴露requestId/actionId/sessionId/ledger/advance API。

1. trusted retained task/phase state调用 package-private
   `declareExclusiveTransaction(context, actionAddress, explicitOccurrence)` 并保存返回handle；Task/caller只取得该non-mintable handle。
2. same address+same occurrence精确返回同一handle/record，即使session completed或delivery uncertain；不会按调用次数、线程、
   transactionName或UUID推进。
3. 请求 `occurrence+1` 只有在两个条件同时成立才接受：上层phase state显式推进其业务动作；上一handle的session terminal control
   已exact final-consumed/compacted。未完成、跳号、回退、payload不同全部fail closed。
4. authority只从handle取得稳定 parent semantic identity。ACQUIRE/RELEASE/ABORT child semantic slots由handle在声明时确定为
   `<actionSlot>:exclusive-acquire|release|abort`，共享parent occurrence；authority不得自行retain“fixed global slot”。release/abort
   是互斥terminal child，未选分支不产生wire request。
5. `exclusiveSessionId`仍只是同一live session的随机机械correlation，保存在handle对应record；它不参与“这是哪个业务动作”的
   判断，也不能触发new occurrence。
6. W-TTR API修正为：

```java
public Transaction beginExclusive(
        TaskTransactionAction action,
        String transactionName,
        TaskTransactionResult expectedResult,
        TaskYieldPolicy yieldPolicy);
```

runner先验证handle属于当前 retained task state/current projection，再enter/acquire-or-join；authority只消费该handle。package-private
`beginExclusiveWithin(parent, action, ...)`还必须是同一个handle对象/record。ordinary begin API不变。

### D2 wire/API/file-table Delta（覆盖 Design #1 对应行）

| Action | Exact file | D2精确变化 |
|---|---|---|
| New | Cloud `remote/TaskTransactionAction.java` | non-mintable stable phase/action/explicit occurrence capability；无raw id/advance |
| Modify | Cloud `remote/CloudTaskRetainedActionState.java` | existing records owner内显式declare/same-occurrence reuse/+1 gate；删除authority自动control occurrence |
| Modify | Cloud `remote/CloudTaskExclusiveInteractionAuthority.java` | 消费TaskTransactionAction；control仅ACQUIRE/RELEASE/ABORT；删REBIND；120s terminal/UNKNOWN接纳 |
| Modify | Cloud `remote/CloudTaskExclusiveInteractionState.java` | resume仍由H/K handoff；删除REBIND-bound语义/第二resume命令假设 |
| Modify | Cloud `remote/ExclusiveInteractionControlRequest.java` / `ExclusiveInteractionControlOutcome.java` | command enum仅ACQUIRE/RELEASE/ABORT；删priorRunRevision/REBIND shape |
| Modify | Cloud `remote/RemoteOperation.java`、`RemoteRequest.java`、`RemoteOutcome.java`、`RemoteGameClientPort.java`、`CloudTaskRunCommandExecutor.java`、`CloudTaskRunExecutionGate.java`、`RemoteGameCommandBroker.java`、`RemoteCommandOutcomeEnvelope.java`、`RemoteProtocolDigests.java` | exhaustive wire仍按D1，但无REBIND branch/slot；same retained outcome不redispatch |
| Modify | DHXY `cloud/remote/RemoteTaskRunRegistry.java` | lifecycle publication内原子推进generic bindingGeneration；handle保存budget terminal snapshot；无REBIND API/map |
| Modify | DHXY `input/action/InputActionRequest.java` | fixed120s pause-compensated session deadline；admitted+terminal exact-once；容量1step lane |
| Modify | DHXY `input/action/InputActionQueue.java` | enqueue/awaitAdmission/terminateAndAwaitTerminal分离；竞态矩阵与单一budget |
| Modify | DHXY `input/action/InputActionWorker.java` | F2后complete admitted；同request session loop；deadline terminal释放worker；finally补齐双completion |
| Modify | DHXY `cloud/remote/LocalRemoteGameCommandHandler.java` | ACQUIRE只等admitted；CAPTURE/INPUT使用same request；RELEASE/ABORT等terminal；删REBIND branch |
| Modify | DHXY `cloud/remote/RemoteGameOperation.java`、control payload/codec/digest/ledger | control仅ACQUIRE/RELEASE/ABORT；budget terminal closed outcome；无second lifecycle command |
| Modify | `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md` | 父级批准后删除REBIND并登记三control、step ref、budget terminal code；本D2不写 |
| Modify in W-TTR-1 | Cloud `remote/TaskTransactionRunner.java` | `beginExclusive`必须传TaskTransactionAction；无name/call-count identity |

Design #1 其余 file table不变。retained phase-state/caller的具体handle声明属于后续各Task activation wave；本RX3/W-TTR波不修改
任何Task/caller，也不得用authority默认handle临时顶替。

### D2 修订后的关键验收点

- ACTIVE且不PAUSE的断连：enqueue起120秒后worker释放；Cloud保持UNKNOWN，不重发physical action。
- PAUSE跨越30秒：budget剩余量不减；resume publication先更新registration/snapshot/bindingGeneration，token只resume一次；wire无REBIND。
- ACQUIRE outcome已提交而terminal未完成时，poll loop可继续取得CAPTURE/INPUT/RELEASE；不存在同步自锁。
- queue reject、admission timeout/interruption、worker-before-admit failure均能看到admitted与terminal两个closed completion及零frame/
  permit泄漏；after-start timeout只报UNKNOWN。
- same TaskTransactionAction重放只返回同session/final；phase state未显式+1时不能新执行；显式+1但前一occurrence未final-consumed也拒绝。

### D2 Self-QA（不构成 Approved）

- Parent P1-1：fixed 120s unpaused + pause compensation + timeout UNKNOWN矩阵已闭合。
- Parent P1-2：wire/lifecycle/API/file table中的独立REBIND已全部删除，resume唯一归existing lifecycle publication/H/K。
- Parent P1-3：同一request admitted/terminal双completion、同步poll可达性及enqueue/admission失败资源收口已闭合。
- Parent P1-4：stable semantic address+explicit occurrence由retained task/phase state提供non-mintable handle；runner/authority不再推断。
- self-QA=`P0=0/P1=0/P2=0`，仅供父级复审；Java/Maven/schema/resources/tests/host/caller仍全部冻结。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Design Review #1 - BLOCKED / Design Repair #1 Published - 2026-07-13T15:52:00-04:00（真实物理 EOF 权威块）

父级对照 committed `TaskTransactionRunner.runExclusive`、当前 `InputActionQueue.await`、同步
`RemoteCommandPollingLoop -> LocalRemoteGameCommandHandler` 与 Z 的 `RemoteTaskRunRegistry.prepareEntryTransition` 复审。
原位复用单一 authority/ledger/worker、Cloud business 不下放、三道 fence 与 closed typed step 的方向成立；当前仍
**BLOCKED，P0=0/P1=4/P2=0**，不得据此改 Java：

1. **P1：设计删除了 committed `runExclusive` 的 120 秒非暂停时间预算。** `InputActionQueue.await:370-443` 对 legacy
   exclusive request 固定 120 秒，PAUSE 只冻结扣减；Design #1 却写成无 TTL、断连时 local session 永久持有单一全局
   input worker。这不仅改变 expiry 语义，也可让一个断连窗口永久饿死其它窗口。Repair 必须保留 exact 120 秒 unpaused
   wait budget 与 pause compensation；超时只能终止 local session/释放 worker，并按是否可能已有副作用映射 UNKNOWN，不能
   重发 action、不能伪造 NOT_EXECUTED/FAILED/STOPPED。
2. **P1：单独的 wire `REBIND` 是第二 pause/resume 权威。** 当前
   `RemoteTaskRunRegistry.prepareEntryTransition:950-978` 已在唯一 `mutationLock` 下把同一 handle 从 paused snapshot 发布为
   successor ACTIVE registration/generation，`applyPreparedWithoutToken:887-904` 完成赋值后才由
   `publishTransition:871-880` 最后 `pauseToken.resume()`。Design #1 再要求 RESUME 后发送 REBIND 并由 REBIND 最后 resume
   token，会出现双 CAS/双 resume 或 worker 先醒后再 rebind。Repair 必须删除独立 REBIND control，原位扩展现有 lifecycle
   publication 为 generic handle 的唯一 local generation handoff；Cloud projection 继续沿 H/K handoff，不增加第二生命周期命令。
3. **P1：ACQUIRE 没有闭合 polling-loop 可达性。** `RemoteCommandPollingLoop:166-174` 同步执行 `handler.handle` 后才提交
   outcome并继续 poll；现有 `InputActionQueue.submitExclusiveAndWait:301-315` 又等待 request terminal completion。若 ACQUIRE
   直接复用该等待，polling loop 无法取得后续 CAPTURE/INPUT/RELEASE，形成自锁。Repair 必须给同一 request 的两个明确
   completion：one-shot `admitted` 与 final `terminal`；handler ACQUIRE 只等 admitted 后立即回 closed outcome，worker继续持有
   request；RELEASE/ABORT 才等待 terminal。写清 enqueue rejection、admission timeout/interruption、worker-before-admit failure
   的 frame/permit/ledger/UNKNOWN 结果，不得新增线程或第二 queue owner。
4. **P1：fixed control slot + authority 自动 occurrence 不能证明稳定业务动作身份。** 当前 API 把 `name`定为诊断字段，所有
   generic transaction 又共用固定 acquire/release slot；上一 session final-consumed 后，重复调用无法区分“同一业务动作重放”
   与“下一次合法 transaction”。自动推进 occurrence 会让 delivery-uncertain 重放铸新物理动作，永久返回 completed 又会
   阻止下一个合法动作。Repair 必须由 retained task/phase state 提供 non-mintable typed transaction action handle（稳定
   semantic address + occurrence）；runner/authority 只消费该 handle，不能从调用次数、线程、name 或随机 UUID 推断。只有该
   handle 的前一 occurrence exact final-consumed 且上层明确推进新业务动作后才可铸下一 occurrence。

### 当前任务 `W-TTR-RX3-D2`

原 Internal Worker AB 只追加 Design Repair #1 Delta，唯一写集仍仅本日志；Java/Maven/schema/resources/tests/host/caller 全冻结。
只关闭上述四项，并同步精确 wire（删除 REBIND）、local admitted/terminal API、稳定 action-handle seam、120 秒 unpaused
deadline/UNKNOWN 矩阵与最小文件表。不要重写已通过的单一 authority/ledger/fence 结论。Worker self-QA 不构成父级批准。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Internal Worker AB - W-TTR-RX3-D2 Design Repair #1 Delta - 2026-07-13T15:57:18-04:00（真实物理 EOF 唯一权威副本）

说明：同标题 Delta 因通用 append 锚点命中较早的重复基线句，被插入 Parent Review #1 之前。为保护 append-only，不删除或
改写旧块；**仅本物理 EOF 副本是 D2 当前权威交付。** 本 Delta 只关闭 Parent P1×4，不重写已通过的单一 authority/ledger/
worker、Cloud business不下放、closed typed step与三道fence。

### P1-1 CLOSED：committed 120 秒 unpaused budget

- 源码基线保持 `InputActionQueue.await:365-466`：从唯一 session request 成功 enqueue 起固定 `120_000ms`；budget覆盖
  queued -> admitted -> mechanical steps -> RELEASE/ABORT terminal。它不可配置、不可按wire timeout/step重置。
- budget由同一 `InputActionRequest` 的 monotonic `deadlineNanos`持有；admission waiter与worker只读
  `remainingDeadlineNanos`。PAUSE复用 `compensatePause(PauseWaitSnapshot)`，在 `progressLock` 下按累计pause nanos只补偿一次；
  ACTIVE断连/Cloud计算照常扣，PAUSED不扣。
- worker等step时只做有界interruptible poll；budget归零停止接收新step、checkpoint当前step、完成terminal并释放现有单一
  input worker。无timer/thread/executor/第二queue owner。
- Design #1“同incarnation断连永久持有worker”作废：未PAUSE断连最多耗尽剩余120秒；local owner随后释放，Cloud仍按
  delivery uncertainty保留UNKNOWN/R0 fence，不自动重发。

| 120秒/中断位置 | exact outcome | 资源收口 |
|---|---|---|
| enqueue拒绝，或queued且remove/cancel在admit前胜出 | ACQUIRE `NOT_EXECUTED` | 双completion闭合；registry/queue permit释放；零frame/step；ledger写exact final |
| worker-before-admit safety/identity/focus失败 | ACQUIRE `NOT_EXECUTED` | `startedStepIndex=-1`；finally补齐admitted+terminal；零frame/physical action |
| admitted后idle且无current step | terminal snapshot=`SESSION_BUDGET_EXPIRED_RELEASED`；新step `NOT_EXECUTED` | worker已释放；snapshot留在同一handle供next control读取 |
| CAPTURE最后pre-fence前到期 | CAPTURE `NOT_EXECUTED` | 不调用capture、无frame |
| CAPTURE已调用但post-fence/exact final未成立 | CAPTURE `UNKNOWN` | frame存在则flush且不发布；原retained request保留，禁止重拍 |
| INPUT第一physical action前到期 | INPUT `NOT_EXECUTED` | action未开始，session terminal |
| INPUT任一action已开始但bundle无exact final | INPUT `UNKNOWN` | checkpoint阻止后续action；原request/action detail保留，禁止重放 |
| waiter interruption | progress lock证明未admit/未start才`NOT_EXECUTED`；已admit或可能start=`UNKNOWN` | 恢复interrupt；不伪造FAILED/STOPPED；terminal owner唯一释放 |

已exact final-consumed的旧step不因session后续到期回退UNKNOWN。RELEASE/ABORT可exact证明worker已释放；若current step仍
unresolved，transaction仍等该step late final，不能拿release事实合成业务outcome。

### P1-2 CLOSED：删除 wire REBIND，lifecycle publication 是唯一 handoff

- control wire精确为 `ACQUIRE | RELEASE | ABORT`；删除 `REBIND`、`priorRunRevision`、其request/outcome、semantic slot、handler/
  codec/digest/ledger branch与runtime验收，不创建替代resume命令。
- Cloud只用existing `prepareResumeGeneration` + H/K handoff：same entry/session/action/step保留，exact PAUSED -> ACTIVE successor时
  projection bindingGeneration +1。
- DHXY只扩展existing `RemoteTaskRunRegistry.PreparedEntryTransition`：`prepareEntryTransition:950-978`在同一`mutationLock`为
  exact `CallbackPausedSnapshot(previous) -> CallbackActiveSnapshot(target)`计算next generic bindingGeneration；stale仍invalidate。
- `applyPreparedWithoutToken:884-904`先一次性写registration/readiness/handle snapshot/bindingGeneration/step cursor；
  `publishTransition:871-880`最后且仅一次 `pauseToken.resume()`。无第二CAS/双resume/worker先醒后绑定。
- resume后new step携Cloud H/K新generation并只与已发布local value比较；old retained in-flight step仍可收其old-generation late final，
  但不能铸新step。mismatch fail closed，不retry、不ordinary fallback。

### P1-3 CLOSED：同一 request 的 admitted/terminal 双 completion

```text
InputActionRequest.sessionAdmitted : CompletableFuture<SessionAdmission>
InputActionRequest.result          : CompletableFuture<InputActionExecutionResult>  // terminal
```

- worker通过F2、进入同一input transaction、focus/safety通过且即将进入session loop时，在`progressLock` exact-once完成
  `ADMITTED`；worker继续持request，不完成terminal。
- 每个worker-before-admit terminal路径在同一个terminal transition内先补齐
  `REJECTED_NOT_EXECUTED`（或无法证明未start时`ADMISSION_UNKNOWN`），再完成terminal；finally兜底两future，禁止悬空。
- handler ACQUIRE顺序：open exact registry handle -> enqueue -> **只等 admitted** -> 立即回closed ACQUIRE outcome；同步
  `RemoteCommandPollingLoop:166-174`随后可继续poll CAPTURE/INPUT/RELEASE。CAPTURE用same handle；INPUT交容量1 step lane并等step
  completion；RELEASE/ABORT发terminal signal且只有它们等待terminal。worker内不nested submit。
- `queue.offer=false`或remove-before-admit胜出：rejected admitted + NOT_STARTED terminal，关闭registry handle/operation-ledger owner，
  零frame/permit泄漏。remove失败不猜：progress lock读取胜者；ADMITTED已胜则保留exact admitted（若response丢失，Cloud UNKNOWN、
  local ledger仍ACQUIRED），未admit且未start才NOT_EXECUTED，可能start只能UNKNOWN。
- admission timeout/interruption恢复interrupt并走同一原子结果；handler不得双close，worker/terminal owner唯一释放permit。

因此ACQUIRE response先于session terminal，poll loop不会自锁；仍只有一个poll thread、一个queue worker、一个request owner。

### P1-4 CLOSED：retained task/phase state提供 stable transaction action handle

- New `TaskTransactionAction`是真实non-mintable capability：public type、package-private constructor；持有同一
  `CloudTaskRetainedActionState` provenance、`ActionAddress(phaseCode,actionSlot)`、**上层显式 occurrence**、record及closed control
  child handles；不暴露requestId/actionId/sessionId/ledger/advance。
- trusted retained task/phase state显式调用package-private
  `declareExclusiveTransaction(context,address,explicitOccurrence)`并保存handle。same address+occurrence永远返回同handle/record，
  包括delivery uncertain/completed replay；不能按调用次数、线程、name、UUID自动推进。
- 只有上层phase state明确提交下一业务动作，且previous session terminal control已exact final-consumed/compacted，才接受
  `occurrence+1`。未完成、回退、跳号、payload不同均fail closed。
- handle声明时确定parent identity和三个mechanical child slots
  `<actionSlot>:exclusive-acquire|release|abort`，共享explicit occurrence；release/abort互斥，未选分支不发wire。authority只消费
  handle，不再retain fixed global slot。UUID仅是同live session correlation，不是业务身份。
- W-TTR API改为：

```java
public Transaction beginExclusive(
        TaskTransactionAction action,
        String transactionName,
        TaskTransactionResult expectedResult,
        TaskYieldPolicy yieldPolicy);
```

runner先校验handle属于current retained state/projection，再enter/acquire-or-join；package-private nested join还要求same handle record。

### D2 最小 file/wire Delta（覆盖 D1 对应行）

| Action | Exact file(s) | D2责任 |
|---|---|---|
| New | Cloud `remote/TaskTransactionAction.java` | stable phase/action/explicit occurrence capability |
| Modify | Cloud `remote/CloudTaskRetainedActionState.java` | existing records内explicit declare/same reuse/+1 final-consumed gate；无新map |
| Modify | Cloud `remote/CloudTaskExclusiveInteractionAuthority.java`、`CloudTaskExclusiveInteractionState.java` | 消费handle；三control；120s terminal/UNKNOWN；resume仅H/K |
| Modify | Cloud `remote/ExclusiveInteractionControlRequest.java`、`ExclusiveInteractionControlOutcome.java` | enum仅ACQUIRE/RELEASE/ABORT；删REBIND/priorRunRevision |
| Modify | Cloud `RemoteOperation/RemoteRequest/RemoteOutcome/RemoteGameClientPort/CloudTaskRunCommandExecutor/CloudTaskRunExecutionGate/RemoteGameCommandBroker/RemoteCommandOutcomeEnvelope/RemoteProtocolDigests` | exhaustive三control wire与retained late-final；无REBIND branch |
| Modify | DHXY `cloud/remote/RemoteTaskRunRegistry.java` | lifecycle publication内推进generation；same handle terminal snapshot；无REBIND API/map |
| Modify | DHXY `input/action/InputActionRequest.java`、`InputActionQueue.java`、`InputActionWorker.java` | fixed120s pause compensation；admitted/terminal；容量1step；单worker释放 |
| Modify | DHXY `cloud/remote/LocalRemoteGameCommandHandler.java` | ACQUIRE等admitted；RELEASE/ABORT等terminal；same request mechanics；删REBIND |
| Modify | DHXY `RemoteGameOperation`、control payload、`RemoteOperationPayloadCodec`、`RemoteProtocolDigests`、`RemoteOperationLedger` | 三control closed outcome与budget terminal；无second lifecycle command |
| Modify after approval | `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md` | 删除REBIND；登记三control、step ref、120s terminal codes |
| Modify in W-TTR-1 | Cloud `remote/TaskTransactionRunner.java` | beginExclusive强制TaskTransactionAction |

其余D1 file table不变。具体Task/phase state与caller声明handle属于后续activation wave；本RX3/W-TTR波不改Task/caller，也不得用
authority默认handle过渡。Java/Maven/schema/resources/tests/host/caller当前全部冻结。

### D2 验收与 Self-QA（不是 Approved）

- 120s ACTIVE断连释放worker但Cloud UNKNOWN；PAUSE时budget冻结；after-start timeout不伪造NOT_EXECUTED/FAILED/STOPPED。
- resume publication先写全部successor字段、token只resume一次；wire全树无REBIND。
- ACQUIRE outcome已提交而terminal未完成时poll loop可继续取得后续命令；所有pre-admit失败双completion且零frame/permit泄漏。
- same TaskTransactionAction重放不新执行；phase未显式+1不新铸，显式+1但previous未final-consumed也拒绝。
- Parent P1×4 self-QA=`0 remaining`；仅供父级复审，**不构成 DESIGN APPROVED**。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Design Review #2 - DESIGN APPROVED / Implementation Published - 2026-07-13T16:05:00-04:00（真实物理 EOF 权威块）

父级对照 committed `InputActionQueue/InputActionRequest` deadline/pause accounting、同步 polling loop、registry
`prepare/apply/publish` 顺序与 retained action record 复审 D2。四项 blocker 均已闭合：固定 120 秒 unpaused budget
保持且 timeout 后按 started evidence 只产 NOT_EXECUTED/UNKNOWN；wire REBIND 完全删除；同一 request 的 admitted/
terminal 双 completion 解除 poll-loop 自锁；stable business occurrence 由 retained task/phase handle 显式提供，authority/
runner 不再从调用次数、线程、name 或 UUID 推断。

结论：`W-TTR-RX3-D2` **DESIGN APPROVED，P0=0/P1=0/P2=0**。批准不代表运行激活，Task/caller/host 继续冻结。

### 当前实施任务 `W-TTR-RX3-IMP1`

原 Internal Worker AB 直接实施 D1 §8 文件表，并以本 D2 file-table Delta 覆盖冲突项。唯一代码写集就是两表列出的
Cloud/DHXY remote、DHXY `InputActionRequest/InputActionQueue/InputActionWorker` 与 protocol schema；不得修改
`TaskTransactionRunner`（仍归后续 W-TTR-1）、任何 Task/Service caller、host/config/resources/tests 或本地业务实现。

硬门：

1. control enum 只有 `ACQUIRE/RELEASE/ABORT`，全树不得残留 REBIND branch；普通 capture/input 的 null session-ref
   canonical bytes/digest 必须不变；两仓 closed payload/digest/strict codec 字段逐项一致；
2. 只有一个 registry/ledger/input queue/worker；不得新增线程、poller、TTL/LRU/takeover/自动 retry；120 秒只使用本地
   monotonic deadline并按现有 PAUSE progress 精确补偿；
3. ACQUIRE handler 只等 admitted；RELEASE/ABORT 才等 terminal；任何失败路径补齐两个 completion 并按 started evidence
   映射 NOT_EXECUTED/UNKNOWN，frame/permit/registry/ledger 不泄漏；
4. stable `TaskTransactionAction` 由 retained state 铸造、同 address+occurrence exact reuse，`occurrence+1` 只在上层显式推进且
   previous terminal final-consumed/compacted 后接受；本波无 Task/caller，不得增加默认 handle；
5. 以 Internal Z 已 FINAL APPROVED 的 whole-pass authority/registry/input 源码为基线原位扩展，不覆盖、不回退；B TeamReturn
   mount 设计不得并发落共享 Java；
6. 完成后运行 Cloud `mvn -q clean package`（不可 skip）与 DHXY `mvn -q -DskipTests compile`，在真实 EOF 追加 exact
   write set、跨仓 wire/digest parity、关键竞态矩阵与双构建统计。Worker self-QA 不构成父级源码批准。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## RESUMED / CLAIMED - W-TTR-RX3-IMP1 - 2026-07-13T16:40:51-04:00

- `task=W-TTR-RX3-IMP1`
- `claimedAt=2026-07-13T16:40:51-04:00`
- `worker=Internal Worker AB`；从当前在途源码继续，不重做、不清理、不回滚、不覆盖任何并行 dirty/untracked，不提交。
- `uniqueWriteSet=` 本固定日志；D1 §8 Cloud RX3 core/protocol 精确文件表（以 D2 删除 `REBIND`、新增
  `TaskTransactionAction.java` 的 delta 为准）；D1 §8 DHXY RX3 local mechanics 精确文件表中的
  `cloud/remote/*` 指定文件、`InputActionRequest.java`、`InputActionQueue.java`、`InputActionWorker.java`；以及
  `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`。除上述逐项批准文件外零写入。
- 冻结确认：`TaskTransactionRunner`、任何 Task/Service caller、host/config/resources/tests、`InputActionScope` 与本地业务实现
  均不修改；不启动 application/Task/UI/capture/input/runtime，不做 Git mutation。

## PROGRESS - W-TTR-RX3-IMP1 - 2026-07-13T18:46:20-04:00

- `worker=Internal Worker AB`；继续实施，尚未提交正式交付，也不构成父级 Approved。
- 当前已写入的批准文件：Cloud `CloudTaskExclusiveInteractionAuthority.java`、
  `CloudTaskExclusiveInteractionState.java`、`CloudTaskRetainedActionState.java`、
  `CloudTaskRunActionLedger.java`、`RemoteFinalConsumptionCoordinator.java`、
  `CloudTaskServicePort.java`、`RemoteOperation.java`、`RemoteRequest.java`、`RemoteOutcome.java`、
  `ExclusiveInteractionControlRequest.java`、`ExclusiveInteractionControlOutcome.java`、
  `ExclusiveSessionStepRef.java`、`CaptureRequest.java`、`InputBundleRequest.java`、
  `RemoteGameClientPort.java`、`CloudTaskRunCommandExecutor.java`、`CloudTaskRunExecutionGate.java`、
  `RemoteGameCommandBroker.java`、`RemoteCommandOutcomeEnvelope.java`、
  `RemoteProtocolDigests.java`、`TaskTransactionAction.java`；DHXY `RemoteGameOperation.java`、
  `RemoteExclusiveInteractionControlCommandPayload.java`、
  `RemoteExclusiveInteractionControlOutcomePayload.java`、`RemoteExclusiveSessionStepRef.java`、
  `RemoteCaptureCommandPayload.java`、`RemoteInputBundleCommandPayload.java`、
  `RemoteOperationPayloadCodec.java`、`RemoteProtocolDigests.java`、`RemoteOperationLedger.java`、
  `RemoteTaskRunRegistry.java`、`LocalRemoteGameCommandHandler.java`、
  `RemoteCommandPollingLoop.java`、`InputActionRequest.java`、`InputActionQueue.java`、
  `InputActionWorker.java`；以及 protocol schema 与本固定日志。未触碰批准表外文件。
- 已落核心：仅 `ACQUIRE/RELEASE/ABORT`、stable `TaskTransactionAction`、同一 retained request 的
  admitted/terminal 双 completion、容量 1 step lane、单 registry/ledger/queue/worker、120 秒 monotonic
  unpaused budget + cumulative pause compensation、generic CAPTURE/INPUT 三道 fence、UNKNOWN late-final 与
  terminal owner-release 协调；普通 null session-ref 仍走既有 envelope 路径。
- 当前编译状态：DHXY 在 latest late-final cursor 修订后已运行中间门
  `mvn -q -DskipTests compile`，exit=`0`，wall=`27.9s`。Cloud 在最新 broker/control 收口后的正式构建尚未运行；
  Cloud `mvn -q clean package`（不 skip）与 DHXY 最终 `mvn -q -DskipTests compile` 都必须等共享 Java 完全稳定后
  重新执行，不能以此前中间 compile 替代。
- 剩余步骤：完成终止 publication / started-evidence / pause-resume / late-final cursor 静态竞态审计并修正批准范围内
  缺口；核对两仓 wire 字段、strict codec、canonical digest 与普通 null session-ref parity；做冻结文件与
  `REBIND`/线程/queue/map 静态扫描；运行双构建；最后只在真实 EOF 追加 Implementation #1 exact write set、
  wire/digest parity、竞态矩阵与构建统计。全程不启动 runtime，不做 Git mutation。

## Parent In-Flight Source Review #1 - BLOCKED / `W-TTR-RX3-IMP1-R1` - 2026-07-13T19:11:00-04:00

父级在 AB 正式交付前主动静态审查当前共享源码，发现 **P0=0/P1=1/P2=0**。AB 继续原任务定点修复，修复后再交
Implementation #1；当前不得跑最终双构建。

### P1-1 - RELEASE/ABORT 把任意 worker terminal snapshot 无条件伪装成成功执行

- **证据：** `LocalRemoteGameCommandHandler.java:1106-1129` 调用
  `terminateRetainedSessionAndWait(...)` 后，不检查 `terminalResult.status/safetyReason/started`，无条件把 RELEASE 映射
  `RELEASED`、ABORT 映射 `ABORTED`，并返回 `RemoteExecutionState.EXECUTED`、`RemoteOutcomeCode.OK`、
  `ownerReleased=true`。但 `InputActionQueue.java:472-496` 在 request 已 terminal 时会直接返回既有 result；
  `InputActionWorker.runRetainedSession:210-248` 可因 deadline、stop、错窗、identity/safety gate 或 interruption 在收到
  terminal signal 前以 false 结束，`InputActionExecutionResult` 因而可能是 `NOT_STARTED`、`PARTIALLY_COMPLETED` 或
  `STARTED_UNKNOWN`，并非 `COMPLETED`。
- **影响：** Cloud 会把实际未消费 RELEASE/ABORT signal 或不确定终止误记为已执行；retained action/final-consumed
  账本随后可错误推进业务 terminal/compaction。虽然 input worker 的物理 owner 已释放，`ownerReleased` 与
  `mechanicalStatus/executionState` 仍是两个不同事实，不能互相替代。
- **精确返修条件：** handler 必须按 returned terminal snapshot 分类，并保持 `ownerReleased=true`（worker 已终结）：
  `COMPLETED` 才映射 RELEASED/ABORTED + EXECUTED + OK；`NOT_STARTED` 映射 STOPPED（明确 stop safety）或
  NOT_EXECUTED（其余可证明未开始）；`PARTIALLY_COMPLETED/STARTED_UNKNOWN` 映射 UNKNOWN。outcome code须与既有
  safety reason/transport code闭合，不能统一 OK。保留 exact snapshot 后再 close handle；不得重新提交 terminal、不得
  新增 retry/queue/thread，也不得改变普通 input bundle与既有 pause语义。Cloud/DHXY outcome strict matrix和 digest必须
  继续一致。

其余已扫描项：未发现 `REBIND`；Cloud 只使用既有 broker route/command queue，本地保留既有单 input worker/queue与
polling loop，未发现第二执行线程/第二 ledger。该局部结论不代表其余 RX3 已批准。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## PROGRESS - W-TTR-RX3-IMP1 - 2026-07-13T19:07:43-04:00

- 已改文件仍严格限于获批表：Cloud remote/control/authority/retained-state/action 文件，DHXY remote 与
  `InputActionRequest` / `InputActionQueue` / `InputActionWorker`，protocol schema，以及本固定日志；未触碰
  `TaskTransactionRunner`、Task/Service caller、host/config/resources/tests。
- 本轮已关闭待审 F1：generic CAPTURE/INPUT 在本地 cursor 尚未 reserve 前的失败统一为 `UNKNOWN`；只有已
  reserve 且可推进的 local cursor 才能产生可消费的非 `UNKNOWN` final。local cursor 已由 N 推进到 N+1 时，
  同一 session/step N 的 late terminal control 仍可凭 completed-cursor 证据终结；第二次 generic occurrence 的
  `ACQUIRE` 使用当前非负 lifecycle generation，不再硬编码 generation 0。
- retained worker 的 public terminal publication 已延后到 input transaction 完整 unwind 之后；F3/started
  evidence 读取内部 terminal snapshot，避免等待同一 request 的 deferred public completion 自锁。
- 当前编译缺口：Cloud 最新源码中间 `mvn -q compile` 已通过，exit=`0`，wall=`18.7s`；DHXY 在上述 F1
  收口前最近一次 `mvn -q -DskipTests compile` 通过，exit=`0`，wall=`24.5s`，但 F1 最新改动尚未重新编译。
  因而共享树当前**尚未声明 stable / clean-ready**，其他 worker 不应运行 `clean`。
- 剩余：先重新执行 DHXY compile；完成 wire 字段、strict codec、canonical digest、普通 null session-ref、
  三道 fence、pause/resume、late-final/terminal 竞态、单 registry/ledger/queue/worker 与冻结面静态审计；稳定后
  再依次执行 Cloud `mvn -q clean package`（不 skip）和 DHXY `mvn -q -DskipTests compile`，最后追加正式
  Implementation #1。此处仅为 worker progress，不构成父级 Approved。

## HALTED_BY_SIMPLIFICATION - W-TTR-RX3-IMP1 - 2026-07-13T19:35:25-04:00

- `worker=Internal Worker AB`；按用户最新架构收缩指令立即停止 RX3 继续实现、静态审计与构建。普通 Service 后续不再
  新增 per-Service owner/permit/ledger/exclusive 状态机，只保留共享 `RemoteGameClientPort`、结构化
  `InputBundle` 与通用错窗/重复执行安全层。本 worker 不对下列在途文件做清理、回滚、覆盖或改写，也不做 Git mutation。
- **Cloud 新建（4）：**
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/ExclusiveInteractionControlRequest.java`、
  `ExclusiveInteractionControlOutcome.java`、`ExclusiveSessionStepRef.java`、`TaskTransactionAction.java`。
- **Cloud 修改（17）：**
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskExclusiveInteractionAuthority.java`、
  `CloudTaskExclusiveInteractionState.java`、`CloudTaskRetainedActionState.java`、
  `CloudTaskRunActionLedger.java`、`RemoteFinalConsumptionCoordinator.java`、`CloudTaskServicePort.java`、
  `RemoteOperation.java`、`RemoteRequest.java`、`RemoteOutcome.java`、`CaptureRequest.java`、
  `InputBundleRequest.java`、`RemoteGameClientPort.java`、`CloudTaskRunCommandExecutor.java`、
  `CloudTaskRunExecutionGate.java`、`RemoteGameCommandBroker.java`、`RemoteCommandOutcomeEnvelope.java`、
  `RemoteProtocolDigests.java`。
- **DHXY 新建（3）：**
  `src/main/java/com/bot/dhxy/cloud/remote/RemoteExclusiveInteractionControlCommandPayload.java`、
  `RemoteExclusiveInteractionControlOutcomePayload.java`、`RemoteExclusiveSessionStepRef.java`。
- **DHXY 修改（12）：**
  `src/main/java/com/bot/dhxy/cloud/remote/RemoteGameOperation.java`、`RemoteCaptureCommandPayload.java`、
  `RemoteInputBundleCommandPayload.java`、`RemoteOperationPayloadCodec.java`、`RemoteProtocolDigests.java`、
  `RemoteOperationLedger.java`、`RemoteTaskRunRegistry.java`、`LocalRemoteGameCommandHandler.java`、
  `RemoteCommandPollingLoop.java`、`src/main/java/com/bot/dhxy/input/action/InputActionRequest.java`、
  `InputActionQueue.java`、`InputActionWorker.java`。
- **文档修改（2，含本次唯一追加目标）：**
  `docs/superpowers/specs/2026-07-12-thin-client-protocol-schema.md`、
  `docs/superpowers/plans/reports/2026-07-13-cloud-generic-exclusive-projection-worker-ab.md`。
- **停止时已知状态：** 父级 P1-1 定点返修已写入 `LocalRemoteGameCommandHandler`，按
  `COMPLETED / NOT_STARTED / PARTIALLY_COMPLETED|STARTED_UNKNOWN` 分类，并将 `ownerReleased` 与机械执行结果分离；
  用户转述父级已确认该方向正确。但正式 `Implementation #1` 未发布，wire/strict-codec/canonical-digest、普通
  null-session-ref、三道 fence、pause/resume、late-final/terminal 与容量/单 owner 竞态矩阵尚未完成最终静态签字，
  共享树也未声明 stable / clean-ready；这些旧 RX3 验收项现由架构简化决定后续取舍。
- **构建事实：** 曾运行中间 Cloud `mvn -q compile`，exit=`0`，wall=`18.7s`；曾多次运行中间 DHXY
  `mvn -q -DskipTests compile`，停止前最新一次（含 P1-1、generation 与 pre-reservation F1 修订）exit=`0`，
  wall=`28.3s`。未运行最终 Cloud `mvn -q clean package`，未运行停止后的 DHXY final compile；未启动 runtime、
  application、Task、UI、capture 或 input，未运行测试。
- `status=HALTED_BY_SIMPLIFICATION`；本块仅记录停止现场，不构成 Implementation 交付、源码批准或父级 Approved。
