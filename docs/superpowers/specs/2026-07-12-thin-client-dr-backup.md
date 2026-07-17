# A-7 首版运行恢复边界（THIN_CLIENT_V1）

工件编号：A-7
状态：lift-and-shift 首版恢复合同

本文配套 A-3 的 in-memory state + append-only journal。首版不把完整 PostgreSQL 灾备、同步副本或跨区域 RPO=0 作为迁移开工门；恢复目标是“不猜测执行、不跨窗口串状态、断线后由云端重连裁决”。

---

## 1. 首版故障模型

| 故障 | 首版处理 |
|---|---|
| 单次同步调用超时 | action 记 UNKNOWN，窗口停止 dispatch，等待迟到 outcome 或重连 |
| client session 断线 | connection generation 换代；在途 action UNKNOWN；逐窗口重连 |
| 云端进程重启且 journal 完整 | replay journal 重建候选状态，所有活跃窗口仍先 RECONCILING |
| 云端进程重启且 journal 缺失/损坏 | 不恢复活跃执行；窗口保持暂停，重新注册、fresh observation、云端 resync |
| 单窗口状态冲突 | 只暂停该 WindowKey；其他窗口继续运行 |
| tenant/device identity 冲突 | 拒绝请求并审计，不尝试合并状态 |

客户端本地 task state、Redis、临时文件和日志都不能替代云端裁决。

## 2. Journal 最小耐久规则

- journal 按 authenticated tenant/device/window namespace 写入，payload 不能选择目录或 stream。
- 同一窗口 `eventSequence` 严格递增；eventId 唯一；事件 append-only。
- `ACTION_DISPATCHED` 必须在调用 `RemoteGameClientPort` 前 flush 成功。
- `ACTION_OUTCOME_RECORDED`、`ACTION_UNKNOWN`、`TASK_COMMAND_ACCEPTED`、`TASK_TURN_CHANGED` 在对应 API 返回成功前 flush 成功。
- flush 失败时禁止新 action；不得以“内存里已经改了”为由继续执行。
- journal 可使用本地 append file、云日志流或数据库 adapter。介质选择不是首版业务接口的一部分。

## 3. 进程内恢复

同一进程中发生 client 断线或调用异常时：

1. 按 WindowKey 取得串行化锁。
2. connection generation 增加 1，state 改 DISCONNECTED。
3. 在途 DISPATCHED action 无可信 outcome 时记 UNKNOWN。
4. 清 current turn 的 activeActionId，但不推进 phase，不创建 successor。
5. pause/stop flag 和 task turn payload原样保留。
6. flush disconnect/UNKNOWN journal 后释放锁。
7. 进入 A-3 重连门；裁决完成前该窗口 dispatchAllowed=false。

## 4. 云端进程重启

### 4.1 journal 可验证

1. 按 `(tenantId,deviceId,windowRegistrationId,incarnation,eventSequence)` 重放。
2. 校验 event sequence 连续、prior/new digest 链、payload version 和完整 WindowKey。
3. 重建 WindowRuntimeState、TaskRunState、current turn、request records、actions 与 late resolutions。
4. replay 终点仍为 PREPARED 的 action 可安全视为未 dispatch；终点为 DISPATCHED 且无 outcome 的 action必须 UNKNOWN。
5. 所有非终态窗口统一置 RECONCILING，增加 connection generation，不直接续跑。
6. 每个窗口完成 authenticated reconnect、client ledger 对账、fresh observation 与云端 resync 后单独开放。

### 4.2 journal 不完整或不可验证

- 不从最后一条可读事件猜测 task turn。
- 不把未知 action 标 NOT_EXECUTED，不自动重放旧 request。
- 对受影响 WindowKey 建立 `RECOVERY_REQUIRED` 占位状态并保持 dispatch 关闭。
- 要求重新注册/重连、fresh observation 和人工或任务已有 resync 裁决。
- 无法确认旧 taskRun 时终止旧 run 的恢复尝试，创建新 run 必须使用新 start requestId；旧 actionId 永不复用。

## 5. UNKNOWN 与迟到 outcome

- UNKNOWN 是历史事实，不被删除或改写成 EXECUTED/NOT_EXECUTED。
- 第一个 actionKey、原 connection generation、签名/会话身份全部正确的迟到 outcome 原子写入 LateOutcomeResolution。
- 相同 outcome digest 重投返回原 resolution；不同 digest 返回冲突并保持窗口暂停。
- resolution 只解除“是否收到可信结果”的不确定性；task phase 是否继续由云端已有任务逻辑结合 fresh observation 决定。

## 6. Pause/stop 恢复

- replay 或重连后 `stopRequested=true` 时禁止任何新 action，任务只可在已有 checkpoint 语义下收口 STOPPED。
- `pauseRequested=true` 或 status=PAUSED 时禁止新 action；只有幂等 RESUME command 可重新开放。
- 断线不能清除 stop/pause，重启不能把 PAUSED 自动改 RUNNING。
- 终态 task 不参与重连续跑。

## 7. 恢复接口

```text
RuntimeRecovery
  verifyJournal() -> JournalVerification
  rebuildCandidateState(verifiedEvents) -> RuntimeStateSnapshot
  markUnresolvedActionsUnknown(snapshot) -> RuntimeStateSnapshot
  requireReconnectForActiveWindows(snapshot) -> RuntimeStateSnapshot
  installRecoveredState(expectedEmptyStore, snapshot) -> InstallResult

ReconnectCoordinator
  begin(authScope, WindowKey, clientSession, clientLedgerDigest) -> ReconcileState
  attachFreshObservation(ReconcileState, observationRef) -> ReconcileState
  decide(ReconcileState, taskResyncDecision) -> CONNECTED | PAUSED | RECOVERY_REQUIRED
```

恢复实现只调用 A-3 `RuntimeStateStore`，不直接修改 task 业务 payload。未来增加 PostgreSQL snapshot、远端 event store 或备份时仍实现这些接口，不改变 UNKNOWN、generation、turn revision 和逐窗口开放规则。

## 8. 首版验收点

1. 两个窗口断线/重连时 state、action、stop/pause 不交叉。
2. 同 requestId 同 digest 重放返回原结果；不同 digest 冲突。
3. 同步调用超时后没有第二个 action，窗口进入 UNKNOWN/reconcile。
4. 迟到 outcome 同 digest 幂等，不同 digest 冲突。
5. journal replay 后 DISPATCHED 无 outcome 一律 UNKNOWN。
6. journal 损坏时没有自动续跑或本地业务 fallback。
7. stop/pause 在断线和进程重启后仍生效。
8. 每个窗口独立通过 fresh observation + 云端 resync 后才恢复 dispatch。

完整数据库 RPO/RTO、跨区恢复与对象备份属于后续基础设施增强，不阻塞以上验收与首版编码。
