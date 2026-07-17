# A-3 首版云端运行状态边界（THIN_CLIENT_V1）

工件编号：A-3
状态：lift-and-shift 编码合同

本文只定义同步 `RemoteGameClientPort` 首版直迁需要的最小云端状态。首版允许使用进程内状态仓库和追加日志，不以 PostgreSQL、Redis、对象存储、完整灾备或物理 DDL 为开工门。未来持久化实现必须替换同一组接口和原子语义，不能改变任务业务决策。

---

## 1. 首版边界

### 1.1 必须进入云端的状态

- authenticated tenant/device identity；
- window registration、window id、incarnation、binding generation；
- task run、当前 task turn、turn revision；
- 同步 action request、action outcome、调用超时后的 UNKNOWN；
- request idempotency；
- pause/stop command 与 task status；
- connection generation、断线标记、重连对账结论；
- 支撑故障定位与可选恢复的 append-only runtime journal。

### 1.2 明确不作为首版前置门

- 完整 PostgreSQL schema、RLS policy SQL、分区和索引调优；
- durable outbox、跨区域同步副本、PITR、对象 GC；
- memory publication、公共记忆、quota/SLO/evidence manifest；
- 多 writer authority transfer、全平台回滚数据库模型；
- 把本地业务逻辑改造成新的云端 phase 或 retry 规则。

这些能力可以以后通过本文的 store/journal 接口加入。未实现它们不阻塞同步 `RemoteGameClientPort` 直迁。

## 2. 不变量

1. **云端业务权威**：task turn、命令判定、action 签发和 UNKNOWN 解除只由云端状态服务决定；客户端本地状态只作为重连证据，不形成业务 fallback。
2. **tenant 隔离**：tenant/device 来自认证 context，payload 不得选择 tenant namespace。所有查找至少使用 `(tenantId,deviceId,...)`。
3. **窗口隔离**：所有 task/action/command/outcome 使用完整 `WindowKey`，不同窗口不能共享 current turn、active action 或 stop/pause flag。
4. **一窗口一当前 run**：同一 `WindowKey` 最多一个非终态 `TaskRunState`。
5. **一 run 一当前 turn**：turn 只能按 `turnRevision` CAS 推进；旧 revision 的 outcome 或 command 不得覆盖新状态。
6. **一窗口一在途同步 action**：同一窗口存在未决 action 时不得签发第二个物理输入 action。
7. **UNKNOWN 不猜测**：调用超时、连接中断或 outcome 身份不完整时，不能推断 EXECUTED/NOT_EXECUTED，也不能自动重放。
8. **stop/pause 不改业务基线**：命令只改变调度许可和 checkpoint 结果，不改 prompt、OCR、点击、导航、fallback 或 phase 成功条件。

## 3. 身份和值对象

```text
AuthenticatedScope = (
  tenantId,
  deviceId
)

WindowKey = (
  tenantId,
  deviceId,
  windowRegistrationId,
  windowId,
  incarnation,
  bindingGeneration
)

TaskRunKey = (
  WindowKey,
  taskRunId
)

ActionKey = (
  TaskRunKey,
  turnRevision,
  actionId
)

RequestKey = (
  tenantId,
  deviceId,
  requestId
)
```

- `windowRegistrationId` 是云端注册记录身份。
- `windowId` 是客户端稳定窗口逻辑身份。
- 游戏进程重建必须增加 `incarnation`；同一进程重新绑定 HWND 必须增加 `bindingGeneration`。
- `taskRunId` 由云端生成或按 `H(tenantId,deviceId,windowRegistrationId,incarnation,startRequestId)` 确定生成。
- `actionId` 每次云端调用唯一；retry 不得生成新 actionId 后假装是同一次调用。

任何 API 的 payload identity 与认证 scope 不一致，返回 `SCOPE_MISMATCH`，不读取或修改状态。

## 4. 最小运行模型

### 4.1 `WindowRuntimeState`

```text
WindowRuntimeState = {
  key: WindowKey,
  connectionGeneration,
  connectionState: CONNECTED | DISCONNECTED | RECONCILING,
  activeClientSessionId?,
  activeTaskRunId?,
  lastConfirmedAt,
  revision
}
```

窗口注册、重连、binding generation 更新均按 `revision` CAS。旧 connection generation 的 request/outcome 只能记审计，不得推进 current state。

### 4.2 `TaskRunState`

```text
TaskRunState = {
  key: TaskRunKey,
  taskType,
  status: CREATED | RUNNING | PAUSE_REQUESTED | PAUSED |
          STOP_REQUESTED | SUCCEEDED | FAILED | STOPPED | CANCELLED,
  currentTurn: TaskTurnState,
  statePayload,
  createdAt,
  updatedAt,
  terminalAt?
}
```

终态全集为 `SUCCEEDED | FAILED | STOPPED | CANCELLED`，终态不可迁出。`statePayload` 是迁移任务已有状态的 versioned opaque payload；首版云端不得借迁移重新解释业务字段。

### 4.3 `TaskTurnState`

```text
TaskTurnState = {
  turnRevision,
  phaseName,
  turnStatePayload,
  activeActionId?,
  dispatchAllowed,
  pauseRequested,
  stopRequested,
  lastOutcomeId?,
  updatedAt
}
```

- `turnRevision` 在每次已接受 command、action 签发、outcome 归并、UNKNOWN 标记和重连裁决时增加 1。
- `phaseName` 和 `turnStatePayload` 由被迁移任务定义；状态层只保证原子保存和版本检查。
- `activeActionId` 非空时 `dispatchAllowed=false`。
- `pauseRequested` 阻止签发下一 action；当前同步调用返回后在已有 checkpoint 边界转 `PAUSED`。
- `stopRequested` 阻止签发下一 action；当前同步调用返回或变 UNKNOWN 后，由任务已有 checkpoint 语义转 `STOPPED`。

## 5. Request、action 与 outcome

### 5.1 请求幂等

```text
RequestRecord = {
  key: RequestKey,
  requestKind: START_TASK | TASK_COMMAND | EXECUTE_ACTION | RECONNECT,
  requestDigest,
  responseDigest,
  responsePayload,
  createdAt
}
```

处理规则：

1. 在 tenant/device 分区锁内查 `RequestKey`。
2. 不存在时执行一次并保存 response。
3. 已存在且 digest 相同，返回原 response。
4. 已存在但 digest 不同，返回 `REQUEST_CONFLICT`。

### 5.2 同步 action

```text
ActionState = {
  key: ActionKey,
  windowKey: WindowKey,
  connectionGeneration,
  requestDigest,
  requestPayload,
  state: PREPARED | DISPATCHED | EXECUTED | NOT_EXECUTED | UNKNOWN,
  outcomeId?,
  outcomeDigest?,
  startedAt,
  completedAt?
}

ActionOutcome = {
  actionKey,
  outcomeId,
  connectionGeneration,
  result: EXECUTED | NOT_EXECUTED,
  outcomePayload,
  outcomeDigest,
  receivedAt
}
```

云端在一个窗口分区原子块内验证 current run/turn、connection generation、stop/pause、无 active action；随后创建 PREPARED action，把 turn.activeActionId 设为该 action，并在真正调用 `RemoteGameClientPort` 前改为 DISPATCHED。

同步调用返回后，云端再次进入同一窗口分区原子块：

- identity、generation、turn revision、activeActionId 全匹配时接受 outcome；
- 相同 outcomeId + digest 是幂等；相同 outcomeId 不同 digest 返回 `OUTCOME_CONFLICT`；
- 已被断线流程标 UNKNOWN 的 action 走 §7 迟到 outcome 规则；
- 接受后清 activeActionId、增加 turnRevision，再由原任务逻辑决定下一 phase/turn。

业务异常若明确证明调用未开始，可写 NOT_EXECUTED；只要无法证明，就写 UNKNOWN。

## 6. Pause 与 stop

`TASK_COMMAND` 值仅为 `PAUSE | RESUME | STOP | CANCEL`。

| 当前状态 | command | 状态层动作 |
|---|---|---|
| RUNNING | PAUSE | `pauseRequested=true`，禁止新 action；无 active action 时转 PAUSED |
| PAUSED | RESUME | 清 pause flag，转 RUNNING，允许任务从已存 turn 继续 |
| RUNNING/PAUSED/PAUSE_REQUESTED | STOP | `stopRequested=true`，禁止新 action；在 checkpoint 语义满足后转 STOPPED |
| 非终态 | CANCEL | 未开始物理输入时转 CANCELLED；有 active action 时先按 outcome/UNKNOWN 收口 |
| 终态 | 任意 command | 返回当前终态，不迁转 |

command 处理使用 `RequestKey + requestDigest` 幂等，并在窗口分区锁内检查 expected taskRunId 和 expected turnRevision。command 不直接调用客户端，也不跳过任务已有 stop/pause checkpoint。

## 7. 断线、UNKNOWN 与重连

### 7.1 断线原子动作

检测到 session 断线或同步调用超时后，在窗口分区锁内：

1. connectionState 改为 DISCONNECTED，connectionGeneration 增加 1。
2. 若 active action 为 DISPATCHED 且无可信 outcome，action 改 UNKNOWN。
3. 清 `activeActionId`，保持 action 历史与 task turn payload，不推进业务 phase。
4. `dispatchAllowed=false`；task 保持 RUNNING/PAUSE_REQUESTED/STOP_REQUESTED 当前语义，但不得继续调用客户端。
5. 追加 `WINDOW_DISCONNECTED` 和 `ACTION_UNKNOWN` journal event。

### 7.2 首个迟到 outcome

UNKNOWN action 的首个身份完整、签名/会话验证通过、actionKey 与原 connection generation 匹配的 outcome 被原子记录为 `LateOutcomeResolution`。同 digest 重投幂等，不同 digest 返回 `LATE_OUTCOME_CONFLICT`。原 ActionState 的 UNKNOWN 历史不删除；resolution 是追加事实。

迟到 outcome 是否足以继续任务，由云端原任务逻辑结合重连证据决定；状态层不自行推进 phase。

### 7.3 重连门

重连必须提供 authenticated scope、WindowKey、new client session、客户端只读 ledger 摘要和 fresh window observation reference。云端执行：

1. 校验 registration/incarnation；进程已换则使用新 incarnation，禁止复用旧 action。
2. connectionState 改 RECONCILING，绑定新 session 和新 generation。
3. 对比云端 active/UNKNOWN action 与客户端 ledger 摘要；任何冲突保持暂停。
4. 调用任务已有 resync/观察入口得到云端裁决；客户端摘要不直接成为业务结果。
5. 裁决通过后改 CONNECTED；无 active action、无未裁决 UNKNOWN、无 stop/pause 阻塞时才恢复 dispatch。

## 8. 首版状态接口

实现可以是线程安全内存仓库，也可以是数据库 adapter；调用方只依赖以下语义接口：

```text
RuntimeStateStore
  registerOrRebindWindow(authScope, registrationRequest) -> WindowRuntimeState
  getWindow(WindowKey) -> WindowRuntimeState?
  getActiveTask(WindowKey) -> TaskRunState?
  createTask(RequestKey, requestDigest, WindowKey, initialTaskState) -> TaskRunState
  applyCommand(RequestKey, requestDigest, TaskRunKey, expectedTurnRevision, command) -> TaskRunState
  prepareAction(RequestKey, requestDigest, TaskRunKey, expectedTurnRevision, actionRequest) -> ActionState
  markDispatched(ActionKey, expectedTurnRevision, connectionGeneration) -> ActionState
  completeAction(ActionOutcome) -> TaskRunState
  markDisconnected(WindowKey, expectedConnectionGeneration, reason) -> DisconnectResult
  recordLateOutcome(ActionOutcome) -> LateOutcomeResolution
  beginReconnect(RequestKey, requestDigest, ReconnectEvidence) -> ReconcileState
  completeReconnect(WindowKey, expectedRevision, ResyncDecision) -> WindowRuntimeState

RuntimeJournal
  append(RuntimeEvent) -> JournalPosition
  flushThrough(JournalPosition) -> FlushResult
  replay(afterPosition) -> ordered RuntimeEvent stream
```

每个写接口同时接收 authenticated context；接口实现忽略 payload 中可伪造的 tenant/device。所有窗口写操作按 `WindowKey` 串行化，可以使用 per-window lock、striped lock 或单线程 actor。不同窗口可以并发。

## 9. 追加日志

首版 journal 事件全集：

```text
WINDOW_REGISTERED | WINDOW_REBOUND | WINDOW_DISCONNECTED | WINDOW_RECONNECTED
TASK_CREATED | TASK_TURN_CHANGED | TASK_COMMAND_ACCEPTED | TASK_TERMINATED
ACTION_PREPARED | ACTION_DISPATCHED | ACTION_OUTCOME_RECORDED | ACTION_UNKNOWN
LATE_OUTCOME_RECORDED | RECONNECT_STARTED | RECONNECT_DECIDED
```

每条事件包含 `eventId, eventSequence, occurredAt, AuthenticatedScope, WindowKey, taskRunId?, turnRevision?, actionId?, requestId?, priorDigest?, newDigest?, payloadVersion, payload`。同一窗口 eventSequence 严格递增；事件 append-only。

内存状态是首版在线读取源，journal 用于诊断和可选重建。若 append/flush 失败，涉及 action DISPATCH、outcome、UNKNOWN、command、turn 的写操作 fail closed；只读查询仍可用。未来 PostgreSQL/event-store 实现必须保留接口、身份、幂等与 CAS 语义。

## 10. 编码落地顺序

1. 定义上述 key/state/command/outcome 值对象与 `RuntimeStateStore` 接口。
2. 实现按 WindowKey 串行化的 in-memory store 和 append-only journal。
3. 让云端同步 `RemoteGameClientPort` 调用只通过 prepare → dispatched → outcome/UNKNOWN 路径。
4. 接入 current task turn、pause/stop command 和 request idempotency。
5. 接入 disconnect generation、late outcome 与 reconnect gate。
6. 首版稳定后再增加持久化 adapter；不得为等待完整 DDL 延迟 1-5 步。
