# Remote Task-Run 显式生命周期实施单

> 状态：实施中；Cloud coordinator/broker 与 Local registry/handler 已落地，生命周期客户端处于返修复审；
> 只支撑 CR271 全量云端直迁，不作为生产切换授权。

## 1. 目的

在 cloud Task 开始发送 `CAPTURE` / `WINDOW_FACT` / `EXECUTE_INPUT_BUNDLE` 前，先建立一条由用户启动、
绑定到精确 tenant/user/device/session/window 的 task run。客户端不得从首条 command 自动注册 run，云端也不得
在客户端 poller/registry 尚未 ready 时启动业务 Task。

## 2. 权威边界

- 云端：taskRunId、任务类型、run 状态、pause/stop revision、Task/Service 执行和 terminal 结果。
- 本地：已注册窗口/HWND、当前 client session、run allowlist、物理输入 safety gate、连接状态。
- 原子切换前，本地旧 Task 继续独立运行但 remote lifecycle 不激活；原子切换后，云端模式不再调用
  `WindowTaskRunner.submit(...)`、`taskFactory.createTask(...)` 或本地 `task.execute(...)`。

## 3. 最小状态

```text
PREPARED -> ACTIVE <-> PAUSED -> STOPPING -> STOPPED
    \------------------------------^    \-> COMPLETED
```

- `PREPARED`：云端已生成 binding，但禁止启动 Task、禁止 broker dispatch。
- `ACTIVE`：本地 registry 已登记、poller 已 ready，云端 Task 才可运行。
- `PAUSED`：同一 run 保留，禁止新物理 action；已 dispatch bundle 在下一个 worker step 使用该 run 的
  pause token 等待，resume 后继续同一 queue request，不通过停 poller或创建本地 Task 模拟。
- `STOPPING/STOPPED`：本地先 fail closed 置 inactive；旧 stopEpoch command 全部拒绝。`PREPARED`
  允许直接停止，用于本地注册失败、activate 前取消以及跨 client session 的旧 run 清理；该转换不运行 Task、
  不发送机械命令。
- `COMPLETED`：Task terminal；禁止复活，同一窗口可随后 prepare 新 run。

## 4. Binding

```text
RemoteTaskRunBinding {
  tenantId, userId, deviceId, clientSessionId,
  taskRunId, taskType,
  windowId, nativeHandle, processId, playerIdentityEpoch,
  stopEpoch, runRevision, status
}
```

tenant/user/device/session 来自认证 middleware；endpoint body 只能携带匹配提示，不能选择 scope。首版内存实现
仍须逐字段校验。taskRunId 由云端按 startRequestId 幂等生成，不能复用本地诊断 long。

## 5. API 与顺序

1. `prepare(startRequestId, taskType, windowBinding)`：幂等创建 `PREPARED`，不运行 Task。
2. 本地验证返回 binding，以 `PREPARED` 登记到 `RemoteTaskRunRegistry`，并确认该 session 的唯一 poller
   已 ready；本地不得在 activate 确认前伪造 `ACTIVE`。
3. `activate(taskRunId, expectedRevision)`：幂等转 `ACTIVE`；只有此后 cloud task host 执行。
4. activate timeout/响应丢失：保持本地 allowlist/poller，不创建第二 run；调用 `status(taskRunId)` 收敛。
5. `pause`：先请求该 run 的稳定 pause token，让已 dispatch bundle 在下一个输入边界 fail closed，再调用云端
   revision CAS；云端确认 `PAUSED` 后才更新本地 registration。pause 请求超时或失败时，本地 token 保持暂停，
   通过 `status(taskRunId)` 收敛，不能因为网络失败自行放行。
6. `resume`：顺序与 pause 相反，必须先由云端 revision CAS 确认 `ACTIVE`，再更新本地 registration 并唤醒
   同一 pause token；只继续同一 request，不换 actionId、不重放 bundle。
7. `stop`：本地第一步把 registry 置 `STOPPING` 并唤醒 pause wait 走 stop 拒绝，再幂等调用 cloud stop；
   失败时仍保持 inactive，只允许 status/stop 重试收敛。
8. cloud terminal 后本地 unregister；断线/UNKNOWN 未收敛时不 unregister、不授权下一输入 flight。
9. 相同 tenant/user/device 的新 client session 不得接管旧 run。它只能显式 fail closed 停止旧的 non-terminal
   run，释放窗口占用后使用新的 `startRequestId` prepare；旧 `startRequestId` 保持冲突，禁止静默复用。

### 5.1 本地客户端收敛规则

- `prepare` 的幂等响应是首次 `PREPARED` 快照，不代表当前状态；客户端拿到 taskRunId 后必须调用一次
  `status` 获取最新 binding，再决定 register/activate，不能把旧 revision 覆盖到本地 registry。
- 首次启动只允许 `PREPARED -> local register -> poller ready -> ACTIVATE`。若 `status` 已是 `ACTIVE`，
  只能在同一 session、同一完整 binding 且本地 registry 已存在时收敛；不得用远端 ACTIVE 绕过本地 ready 门。
- activate 的 timeout/IO 属于结果不确定，客户端只查询同一个 taskRunId；查询到 ACTIVE 才本地确认，查询到
  PREPARED 则保持 PREPARED 并允许同 expectedRevision 重试 activate，绝不重新 prepare 新 run。
- pause 先请求稳定 token；远端失败或状态不确定时 token 保持暂停。只有确认 PAUSED 才更新 registration。
- resume 先远端 CAS；只有确认 ACTIVE 且 revision 前进，才更新 registration 并唤醒 token。
- stop 先以当前 canonical `stopEpoch` 本地 `beginStop`，只发布 `STOPPING`；该状态本身已在输入边界拒绝
  旧 command，不得预造云端尚不存在的新 epoch。随后调用远端 CAS：确认 `STOPPED` 时才接受
  `stopEpoch + 1`；若与云端自然完成并发并收敛到 `COMPLETED`，则接受原 stopEpoch 与前进 revision。
  失败或远端仍 ACTIVE 时本地继续 STOPPING，不能回退为 ACTIVE。
- 所有 wire response 必须复核 contractVersion、action、tenant/user/device/session、taskRunId、taskType、
  windowId/nativeHandle/PID/playerIdentityEpoch、revision/epoch 单调性；错误响应不得部分写 registry。
- 本切片只提供显式 API/service，不接 UI、不自动启动 poller、不创建本地或云端业务 Task。
- coordinator 的 `ACTIVE` 只是机械命令授权的必要条件，不能单独授权 broker，也不能直接启动或唤醒 Cloud Task host。未来 host 的
  execution start/resume 必须是本地已经应用 ACTIVE 后的独立确认步骤；activate/resume HTTP 响应尚在途时，
  云端不得产生首条/下一条 command。host pause/stop 也须与 lifecycle 状态同序收敛，禁止仅监听 enum 自动唤醒。

#### 5.1.1 本地 start reservation、恢复上下文与释放

- 每个 tenant/user/device/startRequestId 在发送 PREPARE 前先原子领取本地 reservation；exact duplicate 先于
  capacity，完整 session/taskType/window 不同则冲突。同 key 生命周期操作通过该 reservation 单飞，不允许
  一个并发重入把另一线程已发布的 ACTIVE 误判成“无 local registration”并进入 cleanup。
- reservation 设置全局与 tenant/user/device owner retained 硬上限，未决记录不使用 TTL、不静默淘汰、覆盖
  或复用。明确证明 PREPARE 未产生远端状态时可释放；一旦绑定 taskRunId，只在 terminal 被显式确认消费后释放。
- local registry 同样设置全局与 owner retained 硬上限。新 registration 在写入前检查，exact taskRunId 先于
  capacity；提供 terminal-only 的显式 release，拒绝删除 PREPARED/ACTIVE/PAUSED/STOPPING，释放时同步唤醒并
  删除 pause token、registration 与对应 start reservation。
- lifecycle failure 必须可选携带完整 recovery binding 与 `cleanupPending`。poller cleanup 已收敛 terminal 时
  `retryable=false, cleanupPending=false`；未决 STOPPING/remote non-terminal 时
  `retryable=true, cleanupPending=true`，调用方只能用同 startRequestId/taskRunId 继续 STATUS/STOP，不能换新
  startRequestId、ACTIVATE 或 takeover。
- Cloud typed `INTERNAL_ERROR` 不能证明 mutation 未生效，按 outcome uncertain 处理：有 taskRunId 的 mutation
  只查询同 run STATUS；PREPARE 只允许同 startRequestId 重入。任何路径都不自动重发 mutation。

### 5.2 Cloud Task execution 确认门

- 新增一个幂等 `CONFIRM_EXECUTION` wire action，参数沿用完整 scope、taskRunId 和 `expectedRevision`；它不改变
  task type、window、stopEpoch、业务 phase 或 Task 结果。
- 本地只有在 exact-session poller healthy、registry 已成功发布同一 binding 的 `ACTIVE` 后才可发送确认；
  activate/resume 的 HTTP 成功本身不能替代本地发布。
- Cloud coordinator 仅在 binding 仍为 `ACTIVE` 且 `runRevision == expectedRevision` 时记录该 revision 已确认。
  同 revision 重试幂等；旧/future revision、scope/session/window 不匹配、PAUSED/terminal 一律 fail closed。
- execution 确认不增加 `runRevision`。确认记录按 retained taskRun 有界保存；任一 pause/resume/stop/complete
  造成 binding revision 或 status 改变后，旧确认因 revision 不相等自动失效，无需删除历史来腾容量。
- Cloud Task host 的首次启动和每次 resume，以及 broker 的最终 dispatch 门，均要求
  `confirmedExecutionRevision == current ACTIVE runRevision`。因此任何直接绕过 host 调用
  `RemoteGameClientPort` 的代码也不能在本地确认前发出首条/下一条 command。
- confirm 响应丢失时不 prepare 第二个 run、不自动重试物理动作；调用方只允许对同 taskRun、同 revision
  重试 `CONFIRM_EXECUTION`。若期间已 pause/stop，重试必须被拒绝并按 status 收敛。
- 本切片只落 coordinator/wire/local lifecycle 的确认能力与未激活 host gate，不创建或启动业务 Task，
  不接 UI，不启动 poller，不发送截图或输入。

#### 5.2.1 Cloud task-run 严格请求边界

- Gateway JSON parser 拒绝尾随 token；非 JSON、非 object 或未知 action 返回稳定 400，不回显 parser/exception
  内部消息。task-run endpoint 在映射 DTO 前安全读取已知 action。
- 已知 action 的 raw tree、DTO 构造和字段校验全部位于 typed error 边界内；缺失/null/类型错误、unknown field、
  非规范 scope/startRequestId/taskRunId/window 文本和 action 不适用字段错误均返回稳定
  `INVALID_REQUEST`，不能抛到 Gateway 形成 500。
- `taskType` 继续按既有合同原样保存，不使用 trim 改写。`playerIdentityEpoch` 在 primitive 映射前要求字段
  存在、非 null、为可转换 long 的整数；显式 0 合法。
- SESSION_CONFLICT、CAPACITY_EXCEEDED、CONFLICT 与 INTERNAL_ERROR 只返回稳定公开消息；具体 exception
  class/message 仅写服务端诊断，不进入 wire response，也不记录 bearer 或完整请求体。

## 6. Broker 门

`RemoteGameCommandBroker` 在入队和 dispatch 两处都核对：精确 scope/session/window/taskRun/stopEpoch、状态
ACTIVE、未 PAUSED/STOPPING。失败返回 `NOT_EXECUTED/TASK_RUN_MISMATCH` 或 `STOPPED/STOP_REQUESTED`，不生成
业务 retry。现有 request/action/input-flight 幂等规则继续生效。

## 7. 实现切片

1. Cloud：in-memory `RemoteTaskRunCoordinator`、typed DTO、prepare/activate/status/pause/resume/stop endpoint。
2. Cloud：broker 只接受 coordinator 授权的 run；server 注册 endpoint，但不自动创建业务 Task。
3. Local：`RemoteTaskRunApiClient`、`RemoteTaskRunLifecycleService`，装配现有 registry/transport/handler/poller。
   registry 同时持有每个 run 的 pause token；detailed input 将它传给现有 worker pause checkpoint。
4. Local UI：仅在全量业务 Task 已迁完的 remote mode 切换入口；开发期不接当前启动按钮。
5. Shadow：只比对云端 Task 决策输出，不允许本地和云端同时发送物理输入。

## 8. 验收门

- 未 prepare/未 activate/错 scope/错 window/旧 epoch 的 command 在副作用前拒绝。
- activate 响应丢失不会创建第二 run；stop 网络失败后本地仍拒绝输入。
- pause 不销毁 run、不启停 poller；中途 pause 不消耗 operation timeout，resume 不重放旧 actionId。
- 同一窗口至多一个非 terminal run；UNKNOWN/input flight 未解除时不得开始下一物理 action。
- 代码落地后 Cloud Brain `mvn -q package`、DHXY `mvn -q -DskipTests compile` 均通过。
