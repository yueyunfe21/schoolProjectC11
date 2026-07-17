# Worker J：Cloud task-turn coordination

## Parent Task Brief #1 - 2026-07-12

### 目标与阶段

为 AutoCombat W0.2 设计 Cloud-native `CloudTaskTurnCoordination` 最小最终合同，等价承接 DHXY HEAD
`TaskTurnCoordinator` 在 AutoCombat 两处 transaction 的公平排队、持有者重入和 `forceRelease` 行为。首轮只追加
`Internal Worker J - Design #1`；父级明确 `DESIGN APPROVED` 前不得修改 Java/Maven/resources/tests。

### 必读与基线

- `D:\mavenProject\DHXY\AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/业务逻辑.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、
  `docs/superpowers/specs/2026-07-12-service-migration-matrix.md`。
- 本报告与 `docs/superpowers/plans/reports/2026-07-12-cloud-auto-combat-service-worker-a.md` 最新 Parent Design Review #3。
- DHXY HEAD `0114604e` 的 `TaskTurnCoordinator` 全类、所有 main caller、`InputActionWorker` 相关豁免来源、stop/pause/
  interrupt 路径和日志。
- Cloud 当前 TaskExecutionContext/checkpoint、coordinator/assembly、retained action/Service port；不得把本地 input queue、
  HWND、ThreadLocal holder 或本地 runner 权威搬上云。

### 设计不变量

1. public 最终业务合同至少保持 A 已冻结的 `void enter(String transactionName)` 与
   `void forceRelease(String transactionName)` 调用形状；若基线还要求其它 public 成员，必须列全部 caller 和必要性，不能造
   temporary seam。
2. tenant/user/device/session/taskRun/window/revision ownership 必须来自不可外部构造的 current execution capability；错 scope、
   stale revision、pause/stop/terminal 必须 typed unwind，不能靠线程名、全局 current 或 raw string key。
3. 保持 HEAD 的公平 FIFO、同一业务持有者重入深度、exact release/forceRelease、interrupt/stop 行为和日志语义；不得新增 TTL、
   takeover、lease expiry、自动 retry、额外 park/yield 或业务超时。
4. 不复制本地 `ThreadLocal`/input-worker 线程名作为 Cloud authority。若 Cloud 需要 reentrancy identity，设计明确由 exact
   taskRun + transaction owner capability 提供，且说明同步调用栈与并发 continuation 的边界。
5. 无 host/server/endpoint/poller/线程池启动；能力保持 dormant。设计必须覆盖并发、公平、暂停/恢复、response loss、owner
   crash/restart、容量和运维 fail-closed。
6. 写集与外部 A 的 `remote/**` PAUSED observer、外部 B 的 artifact/host 文件、内部 I 的
   `com/bot/dhxy/config/CloudAutoBattleProperties*`、已批准 H State owner文件均不得重叠。

### Design #1 必交付

- HEAD 所有 caller、lock/reentry/release/forceRelease/interrupt/input-worker 豁免逐路径证据和 AutoCombat 实际依赖子集。
- 推荐 public interface、package-private owner/handle/queue 数据结构、full owner key、线性化点、锁序与 typed checkpoint 顺序。
- `enter` 成功/重入/等待中 pause-stop-interrupt、错误 release、forceRelease、同 tenant 多窗、跨 tenant、restart 的真值矩阵。
- exact Cloud 文件写集与 future assembly/AutoCombat 构造图；优先 new-only，assembly/host 接线后置，当前目标存在性须核查。
- 容量上限必须是结构安全门且不改变已 admitted 队列顺序；无 silent eviction。列 P0/P1/P2、自审与批准后 package 门。

### Worker 规则

- 你是实现 Worker，不是 reviewer；只向本文件 append Design #1，父级批准前零 Java。
- 你不是独自在仓库中；保护全部 dirty/untracked，不回滚、不覆盖、不提交，不修改其它 Worker 报告。
- 不运行 Maven/测试，不启动 application/server/host/Task/poller/UI/capture/OCR/input。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker J - Design #1 - 2026-07-12

本节只交付设计，不构成 reviewer 结论。当前未修改 Java/Maven/resources/tests，未运行 Maven/测试，未启动
application/server/host/Task/poller/UI/capture/OCR/input。设计只承接 AutoCombat W0.2 已冻结的 task-turn
协作面；不顺带迁移 `TaskTransactionRunner.leave(...)`、`tryRun(...)` 或本地 input queue。

### 0. 基线、dirty 保护与目标存在性

- DHXY 基线：`thin-client-design` / HEAD
  `0114604e1ff5f15491d2910959c45252e893d04f`，无 upstream；`git status --short` 有大量用户/并行
  dirty/untracked，全部保留。本报告自身位于未跟踪的 reports 目录中。
- 本业务路径以 `git show 0114604e:<path>` 为准。`TaskTurnCoordinator.java`、
  `TaskTransactionRunner.java` 对 HEAD 无 diff；工作区 `AutoCombatService.java` 有与本 W0.2 无关的
  `reportXiuluoLeaderFirstAidAfterVerifiedReturn(...)` 在途差异，`InputActionWorker.java` 有远程输入 deadline/
  safety 在途差异，均未采信、未触碰。
- Cloud 基线：`navigation-migration` / HEAD
  `3b988caa010254973e03342272e6d1d6a9685b01`；`src/main/java/com/bot/**`、`remote/**`、host 等均有
  并行未跟踪/dirty，全部保护。
- Cloud 当前存在并已读取：typed `TaskExecutionContext` / `TaskCheckpoint*`、
  `CloudTaskRunExecutionContext`、`CloudTaskServiceExecutionContext`、`CloudTaskRunExecutionGate`、
  `RemoteTaskRunCoordinator`、`CloudTaskRunAuthorityAssembly`、H 已批准的
  `CloudGameContextStateOwner` 和 I 已批准的 `CloudAutoBattleProperties*`。
- Cloud 当前**不存在**：`CloudTaskTurnCoordination.java`、`CloudTaskTurnAuthority.java`、
  `CloudTaskRunCurrentContextSlot.java`、Cloud `AutoCombatService.java`。其中 current-context slot 已由 Worker K
  独立领取；J 只消费 K 最终批准的 `current()`，不修改/复制 K 文件。
- 已核 `docs/业务逻辑.md`：等价迁移总门（当前文件约 L217 起）、通用盒子 L72-L168，尤其跟队队员必须等
  队长释放并实际取得 task turn 后消费（L137）、盒子优先于 first-aid（L143-L155）；以及 expected 战斗/
  战后短窗口规则。J 不新增 session-aware priority、TTL、重试、park、cleanup 或业务超时。
- 当前 `logs/dhxy-console.log` 可见 HEAD 既有 `[latency] event=task.turn.release` / slow-hold 格式；最新一条
  是历史修罗 STOPPED 释放（`heldMs=49381219`、`queuedWaiters=0`），不能作为 AutoCombat 并发验收样本，
  只用于确认现有日志字段语义。

**无已批准业务差异；按基线等价迁移。**

### 1. DHXY HEAD 全 caller 与源语义证据

#### 1.1 `TaskTurnCoordinator` public caller 闭包

| public 成员 | HEAD 直接 caller | 当前 W0.2 必要性 |
|---|---|---|
| `enter(String)` | `TaskTransactionRunner.run` :50、`runDynamic` :93、`runExclusive` :133；`AutoCombatService` :830/:881/:939 | **需要**；A 已冻结 public 形状 |
| `forceRelease(String)` | `TaskTransactionRunner.forceReleaseTurn` :173；`AutoCombatService` :843/:903/:954 | **需要**；A 已冻结 public 形状 |
| `leave(TaskTransactionOutcome)` | 仅 `TaskTransactionRunner` :68/:113/:163 | 本 W0.2 不需要；它依赖完整 result/yield 业务合同，提前加入会成为 temporary seam |
| `tryRun(String,Supplier<Boolean>)` | 仅 `WubeiTask.maybeRunLeaderPathingSummonMaintenance` :1520 | 本 W0.2 不需要；其非阻塞/必释放语义留给 Wubei Task 整体迁移切片 |

`TaskTransactionRunner.forceReleaseTurn(...)` 的 HEAD 主 caller 为 Wubei :463/:684、FiveRingTaskV2
:334/:535/:809、XiuluoTaskV2 :532。Wubei/Xiuluo 还通过 constructor 持有 coordinator；Xiuluo 当前没有
直接 coordinator 方法调用。以上路径解释了为什么最终完整 Task 迁移仍可能需要 `leave/tryRun`，但它们不是
AutoCombat W0.2 的批准范围，J 不先造无 caller API。

#### 1.2 AutoCombat 实际依赖子集

AutoCombat 有两类业务 transaction、三个 `enter -> finally forceRelease` 调用点：

1. member common-box：:830 获取，:843 全量释放；盒子继续保持 first-aid 前优先。
2. follower first-aid：修罗 FIFO head 分支 :881/:903，以及非 FIFO follower 分支 :939/:954。

三处均为 blocking acquisition；`transactionName` 只用于诊断，不参与 owner/reentry/release 判定；成功、false、
业务异常和 typed unwind 都由 `finally` 调用 `forceRelease`。leader queued first-aid :558 明确依赖“caller 已持有
task turn”，本方法本身不重入 coordinator；J 不改变该边界。

#### 1.3 HEAD lock/reentry/release/interrupt/input-worker 逐路径

- 唯一资源为 singleton `ReentrantLock(true)`；首次 `enter` 使用 `lockInterruptibly()`，fair queue 是窗口获取顺序
  权威。`SLOW_TURN_THRESHOLD_MS=3000` 只告警，不超时、不强释。
- 重入权威实际是 task thread 的 `ThreadLocal holdDepth`。depth>0 时 `enter` 只加一；它不以
  `transactionName` 区分 nested scope。`leave` 在 CONTINUE_CHAIN 时只降一层/保留；yield/exception 时
  `releaseAll`。`forceRelease` 无论深度多少都释放全部。
- `forceRelease` 的 reason 不要求等于 acquire transaction；depth<=0 静默 no-op，因此 A 所说“幂等
  forceRelease”与 HEAD 一致。错误线程不能释放别的线程持有的 lock；其 ThreadLocal depth 为 0。
- `enter` 等锁时被 interrupt：恢复 interrupt flag 后抛 `TaskStopRequestedException`；`TaskTransactionRunner`
  将 STOP 转为 transaction outcome，finally 再 `leave`。AutoCombat 的 direct enter 则由上层 typed stop 路径展开，
  仍依赖 finally force release。
- HEAD pause 是 cooperative：`WindowTaskRunner.pauseCurrentTask()` 只置 pause token/唤醒事件，不 cancel future；
  已阻塞在 `lockInterruptibly` 的 waiter 不会因 pause 单独醒来。Cloud Parent Brief 已明确收窄为 PAUSED/stale/
  terminal 必须 typed unwind；本设计按该 Cloud lifecycle fence 执行，不把 PAUSED 当业务 FAILED，也不在 resume
  后自动重试旧 `enter`。
- `InputActionWorker.start()` 的线程名固定为 `dhxy-input-action-worker`。本地 `enter/leave/tryRun` 在该线程
  空转，是因为 `TaskTransactionRunner.runExclusive` 已在 task thread 先持 task turn，再把 callback 交给唯一 input
  worker；callback 内若再按普通线程抢同一 lock 会死锁。`forceRelease` 没有显式 worker-name 分支，它在 worker 上
  因 depth=0 自然 no-op。
- Cloud retained action/Service port 只下发机械动作，本地 input worker 不回调 Cloud business coordinator。因此
  Cloud 不复制 thread name、`ThreadLocal`、input queue、HWND 或 holder；physical input serialisation 继续由本地
  机械执行层负责，Cloud turn 只决定业务窗口次序。

#### 1.4 日志等价边界

保留以下事件含义和 3000ms warning 阈值，elapsed 一律用单调纳秒源计算，不参与业务决策：

- `task turn waiting`
- `[latency] event=task.turn.handoff`：wait、前一 holder/reason/result、sameAsPrevious、前后 queued 数
- `task turn acquired` / `task turn acquired slowly`
- `[latency] event=task.turn.release` 与 normal/slow release

Cloud 日志追加 exact `tenantId/userId/deviceId/clientSessionId/taskRunId/windowId/runRevision/stopEpoch`，但不改变
既有 event 名。重入继续不额外刷 acquisition 日志；无 hold 的幂等 forceRelease 继续静默。

### 2. 方案比较与推荐

#### 方案 A（推荐）：assembly 级共享 FIFO authority + per-taskRun capability handle

- 一个 `CloudTaskTurnAuthority` 管理同进程所有 device lane；每个 taskRun 只拿到一个不可外部构造的 handle，
  AutoCombat 只看 public `CloudTaskTurnCoordination`。
- FIFO 由显式 ticket/deque 决定，owner 由 exact execution context + handle object identity 决定；Java thread 只提供
  interrupt 信号，不是 ownership。
- 能在不碰 broker/input/HWND 的前提下保留 blocking fairness、nested reentry 和 exact forceRelease；也能把 capacity、
  生命周期取消和日志放在一个 authority 内。

#### 方案 B（拒绝）：每 lane 一个 fair `ReentrantLock` + Cloud ThreadLocal

源码最短，但把本地偶然线程模型原样搬上云。同步调用换 executor thread、resume 新 revision 或 continuation 后，
ThreadLocal 要么丢 owner，要么把线程池线程误当 owner；仍需 thread-name 豁免，直接违反 Parent Brief。

#### 方案 C（拒绝）：以 retained input single-flight 代替 task turn

input single-flight 只串行一次机械 bundle，不能表达 AutoCombat 在读状态、检查 pending、更新 FIFO/业务 state 到发出
机械动作之间的 coarse turn，也不能保留“队长释放后队员按公平队列取得维护机会”。它会把业务仲裁错误地下沉到
本地 input queue，违反迁移矩阵 owner。

**推荐 A。** 它是当前最小 final contract，不为未来 `leave/tryRun` 预造接口；未来完整 Task turn 迁移可在同一 authority
上另开批准切片扩 public contract。

### 3. Public 合同与 non-public 类型

#### 3.1 唯一 public 业务合同

目标 package：`com.yueyunfe.dhxy.cloudbrain.remote`。

```java
public interface CloudTaskTurnCoordination {
    void enter(String transactionName);
    void forceRelease(String transactionName);
}
```

- 不增加 context 参数，保持 A 已批准 constructor collaborator 与 AutoCombat 调用形状。
- `transactionName` 允许 null，与 HEAD 一样仅作日志 reason；不 trim、不作为 key。
- `enter` 只可能正常返回“当前 exact owner 已持 turn”；pause/stale/future/unconfirmed/completed/denied 使用现有
  `TaskCheckpointTransitionException`，STOP 使用 `TaskStopRequestedException`。capacity 使用 implementation 内 typed
  `CloudTaskTurnCapacityException extends IllegalStateException`，AutoCombat 不吞、不转成业务 false。
- `forceRelease` 不做 current slot gate：这样 old ACTIVE stack 在 pause/stop/stale unwind 的 finally 中仍能 exact 释放
  自己已取得的 hold。它不能据新 revision 释放别人的 hold。

#### 3.2 package-private authority/handle 与 private queue model

`CloudTaskTurnAuthority.java` 内提供 package-private：

```java
final class CloudTaskTurnAuthority {
    CloudTaskTurnHandle createHandle(CloudTaskRunCurrentContextSlot currentContextSlot);
    void signalLifecycleChange(
            CloudTaskTurnHandle handle,
            TaskExecutionContext invalidatedContext);
    void requireQuiescent(CloudTaskTurnHandle handle);
}

final class CloudTaskTurnHandle implements CloudTaskTurnCoordination {
    // constructor、slot、authority、hold/wait state 全 package-private/private
}
```

同文件底部放 private/package-private 数据类型：

- `TurnLaneKey(tenantId, deviceId)`：当前协议没有 `inputLaneId`，W0.2 等价采用每设备唯一 lane。user/session 不进入
  资源 key，避免同一物理设备因 user/session 不同得到两把“合法”turn；tenant 进入 key，跨租户绝不互相阻塞。
- `TurnOwnerKey(scope四元组, taskRunId, taskType, window四元组, nonTerminalStopEpoch, runRevision)`：字段全部从
  K slot 返回的不可伪造 current `TaskExecutionContext` 投影；不接受 raw string owner key。
- `LaneState`：current holder handle 引用、held owner key、holdDepth、held-start、FIFO `ArrayDeque<WaitNode>`、
  last-release diagnostics。
- `WaitNode`：单调 ticket、exact handle/owner、原 transactionName、enqueue time、node `Condition`、状态
  `WAITING/GRANT_RESERVED/LIFECYCLE_CHANGED/INTERRUPTED`。
- `CloudTaskTurnCapacityException`：同文件 package-private typed structural rejection，不给业务代码 retry policy。

handle object identity 是第二层不可伪造 capability：即使 full owner fields 完全相同，由另一个错误 authority/handle 构造的
调用也不能重入或释放已有 holder。assembly 后续必须保证 exact taskRun 只铸造一个 handle。

### 4. Resource/owner、同步栈与 continuation 边界

1. **lane 粒度**：`tenantId + deviceId + implicit single input lane`。同 tenant/device 的所有窗口按一个 FIFO；同
   tenant 不同 device 独立；跨 tenant 独立。该粒度与本地“一设备一个 input worker + 一个 TaskTurnCoordinator”等价。
2. **holder 粒度**：full owner key + exact handle reference。user/clientSession/taskRun/window/revision 都是 owner，不能
   因 lane 共享而互相释放。
3. **reentry**：只有 lane holder handle reference 相同且 full owner key exact 相等才 `depth+1`。transactionName 不参与。
   同 taskRun resume 后 revision 前进，不是旧 hold 的 reentry。
4. **同步调用栈**：未来 activation adapter 必须在 H `CloudGameContextStateOwner.callWithState(...)` 的同一个同步
   projection 内调用 AutoCombat。H 的 per-run `executionLock` 已保证同一 taskRun 同时最多一个业务 stack；该 stack 内
   nested Service 调用共享 handle，允许 reentry。
5. **并发 continuation**：不得把 AutoCombat/handle 逃逸到 projection 外异步执行。新的 continuation 必须先经过
   activation owner + H execution lock，并在前一 stack 已 forceRelease 后作为 fresh acquisition。resume install/terminal
   close 前，activation owner 必须在没有该 run 业务 stack 可并发进入的边界调用 non-releasing
   `requireQuiescent(handle)`；旧 stack 若仍持有/等待 turn 就拒绝推进并报警，不能把新 continuation 伪装成 reentry，
   也不能 takeover/TTL 清理。
6. Java thread id/name 不进入任何 key。`Condition.await()` 的 thread interrupt 只负责把该 waiter 从队列撤销并触发 typed
   unwind。

### 5. `enter`、release 与线性化点

#### 5.1 首次成功/等待

1. 调用 `slot.current()`；K 合同在返回前完成 exact current-confirmed ACTIVE typed gate。由返回 context 构造
   `TurnLaneKey/TurnOwnerKey`，不在 authority lock 内调用 coordinator/slot。
2. interruptibly 获取 authority state lock。若 handle 已是 exact holder，走 §5.2；若 handle 已有 waiter，则视为
   concurrent-continuation structural violation，fail closed，不插第二个节点。
3. 新 lane/新 contender 先做全部 capacity check；失败发生在 lane/node/counter 写入之前。lane 空且 queue 空时直接写
   holder；否则按单调 ticket append tail。**queue append 是 admission 线性化点**。
4. append 后先释放 authority lock，立即对 captured context 做 typed checkpoint；失败则 exact 移除 node。若 transition
   已在 append 后发生，node 的 sticky lifecycle flag 保证进入 await 前也能看到；若发生在 append 前，本次 checkpoint
   直接看到旧 context 失效。通过后才进入 Condition loop。
5. waiter 只允许 queue head 在 holder 为空时变成 `GRANT_RESERVED`；new arrival 永远不能绕过已有 head。handoff 时
   admitted counter 不变。
6. grant reservation 后释放 authority lock，再以最初 captured context 执行现有 typed checkpoint，并再次要求
   `slot.current()` 与 captured full key exact；然后才向 caller 返回。若 gate 失败，重入只回滚本次 depth，新 holder
   则 exact 释放 reservation 并 signal 下一 head，再原样抛 typed unwind。
7. **成功 acquisition 线性化点**是 authority lock 内 holder 从 null 写成 exact handle/owner 的时刻；但 public
   `enter` 只有 post-grant typed gate 通过才返回。生命周期先发生时 reservation 会回滚；生命周期后发生时后续
   Service/retained port checkpoint 继续按 revision fence 拒绝旧 stack。

#### 5.2 Reentry

- `slot.current()` 先确认当前 revision；authority lock 内 exact handle + exact owner 命中后 depth 加一，不排队、不受
  queue capacity 阻挡，保持 fair lock holder reentry 可越过 waiters的 HEAD 语义。
- depth 写入后同样做 lock 外 post-grant typed gate；失败只撤销本次 `+1`，不替 outer scope 决定 release。
- owner key 不同（最常见为 resume revision 已前进）时绝不 reenter；旧 hold 未释放则新 owner 正常排尾并 fail-closed
  等待，不 takeover。

#### 5.3 Waiting 的 pause/stop/interrupt 无丢唤醒

- future activation/lifecycle owner 在 coordinator transition 方法**返回以后**调用 package-private
  `signalLifecycleChange(handle, invalidatedContext)`；参数是 transition 前该 stack 持有的不可伪造 exact context，不是
  raw binding/reason/status。authority 先在 turn lock 外对该 old context 执行现有 typed checkpoint：若仍为 current
  ACTIVE confirmed，拒绝 signal；只有得到 PAUSED/STOPPED/COMPLETED/stale/unconfirmed 等 typed unwind，才在 turn
  lock 内对 full owner key **逐字段相等**的现有 waiter 标记 `LIFECYCLE_CHANGED` 并 signal。它不释放 holder。
- “transition 先于 enqueue”由 enqueue 后的 captured-context gate 捕获；“enqueue 先于 transition”由 node 上 sticky
  lifecycle flag 捕获。不能只做一次非 sticky `signal()`，否则 pause race 会永久等锁。
- 迟到的 old-context signal 只能命中该 old context 的 exact owner key；resume 后 newer waiter 的 revision 不同，即使
  handle 相同也不能被取消。这样既不会因“必须仍是 current transition binding”而丢掉迟到但必要的旧 waiter 唤醒，也
  不能跨 revision 误伤。
- waiter 醒来后先移除自身/保持剩余 FIFO 顺序，再用 captured context 得到 PAUSED/STOPPED/COMPLETED/
  ACTIVE_NEWER_REVISION_* 等现有 typed outcome；不解析 authorization reason 文本，不自动 retry/resume/requeue。
- `InterruptedException`：先在 authority lock 内精确移除该 node、signal 新 head、恢复 interrupt flag；随后用 captured
  context checkpoint 分类。若已 STOP/PAUSED/stale，抛对应现有 exception；若仍 exact current ACTIVE，抛
  `TaskCheckpointTransitionException(TaskCheckpointDecision.interruptedWhileCurrent(revision))`。这与 Cloud
  `TaskSleep` 的现有 interrupt policy 一致。

#### 5.4 `forceRelease`

- 只在 authority lock 内比较 exact handle reference 与其 recorded held owner；命中即一次释放全部 depth、记录
  last-release、decrement admitted holder、signal FIFO head。**holder 清空是 release 线性化点**。
- 不读取 current slot/coordinator，因此 pause/stop/stale stack 的 finally 可释放它原先拿到的 exact revision hold。
- reason/transactionName 不要求等于 acquire name；这与 HEAD 相同。handle 无 hold 时静默 no-op；另一个 handle 即使
  full field 相同也不能释放当前 holder。
- lifecycle signal、slot close/install、capacity 或运维路径均不得自动 force release。没有 TTL、lease expiry、
  takeover、watchdog unlock 或 silent cleanup；3000ms 仍只告警。

#### 5.5 Revision advance/terminal 前 quiescence

- `requireQuiescent(handle)` 只拿 authority lock，检查该 handle 既不是任何 lane holder、也没有 admitted waiter；
  **该 lock 内只读判定是 quiescence 线性化点**。通过时零状态写，失败时抛 structural `IllegalStateException`，附 exact
  run/window/revision diagnostics，但不 release/cancel/reorder。
- future activation owner 只能在 H execution lock 已排除旧业务 stack、且尚未发布新 revision Service stack 的边界调用；
  然后才允许 K slot install 或 exact terminal close。这样 same handle 跨 resume 复用不会把 orphan old hold 当 reentry。
- 首次 activation 创建 handle 时天然 quiescent；普通 AutoCombat 调用不调用本方法，避免把它变成新的业务 gate。

### 6. Lock order 与线程安全

1. H `executionLock`（future caller 外层）可以包住业务 stack；turn authority 从不回调 H owner。
2. `slot.current()` / captured context checkpoint 会进入 `RemoteTaskRunCoordinator` monitor；它们必须在 turn authority
   state lock **外**执行。
3. enter 的顺序是 `H executionLock -> coordinator typed read（释放） -> turn state lock`；post-grant 时先释放
   turn lock，再做 coordinator typed read。不存在 `turn lock -> coordinator monitor`。
4. lifecycle 顺序是 coordinator transition 完成并释放 monitor后，先在 turn lock 外对 invalidated old context 做 typed
   classification，再 `signalLifecycleChange` 获取 turn lock；禁止在 coordinator synchronized callback 内 signal。
5. `forceRelease` 只拿 turn lock；日志字段在 lock 内 snapshot，实际 SLF4J 输出在 lock 外，避免日志 I/O 延长公平队列。
6. 每个 waiter 自己的 Condition 仅由同一 authority lock 管理；spurious wakeup 重新检查 node state/head/holder，不能
   改 ticket 或重新排尾。

### 7. 容量与保留策略

默认结构门与当前 `RemoteTaskRunCoordinator` 数量级对齐，但不成为业务配置：

- retained lane 上限 `10_000`；lane 保留 last-release diagnostics 到进程结束，不 silent eviction/reuse。
- 全局同时 admitted contenders（holder + waiters）上限 `10_000`。
- 每 lane admitted contenders 上限 `64`。

首次 lane/contender admission 在同一 state lock 内原子检查后写入。reentry 不新增 admission；waiter->holder handoff 不
重复计数；interrupt/lifecycle cancellation/forceRelease exact-once 减计数，underflow/overflow 立即 structural failure。
容量满只拒绝**新** contender，现有 holder和队列顺序完全不变；不踢 head、不删 oldest、不把 capacity 转成
AutoCombat false/retry。运维日志必须包含 quota dimension、lane、current count/limit，不打印可伪造 release token。

### 8. 真值矩阵

| 场景 | 结果/线性化 | 队列与 owner 后果 |
|---|---|---|
| lane 空、current ACTIVE confirmed | `enter` 成功 | 当前 handle 成 holder，depth=1 |
| same handle + full owner exact nested enter | 成功重入 | depth+1，现有 waiters 不插队 |
| same handle 但 revision/stopEpoch/window/scope 漂移 | typed/structural fail closed | 不重入、不改旧 hold |
| 另一窗口 enter | FIFO tail admission 后 blocking | holder 不变，按 ticket 等待 |
| PAUSED/stale/unconfirmed/terminal 在 enter 前 | `slot.current()` typed unwind | 零 admission |
| waiter 等待中 PAUSE | sticky lifecycle signal；`PAUSED` typed unwind | exact 移除该 node，剩余顺序不变 |
| waiter 等待中 STOP/COMPLETE | typed STOP/COMPLETED unwind | exact 移除，不自动 retry |
| waiter 等待中 interrupt、context仍 current | `INTERRUPTED_WHILE_CURRENT` typed unwind，interrupt flag 保留 | exact 移除并 signal head |
| holder 运行中 PAUSE/STOP | 后续 checkpoint unwind；AutoCombat finally forceRelease | lifecycle 不抢锁、不自动解锁 |
| same taskRun resume 新 revision | 旧 waiter/stack先 unwind；quiescence通过后 K install 新 owner | 不继承旧 depth；无旧 hold才可 fresh acquire |
| resume install/terminal close 时仍有 old hold/wait | structural fail closed + alert | 不推进、不强释、不 takeover |
| `forceRelease` by exact holder handle | 正常返回 | 全 depth 释放、signal head |
| 同 handle 重复 forceRelease / 从未持有 | 正常 no-op | 状态不变 |
| 其它 handle forceRelease | 正常 no-op，不可解别人 | holder不变 |
| same tenant + same device 多窗 | 同 lane FIFO | 保持本地全局 task-turn 竞争 |
| same tenant + different device | 独立 lane | 可并发，不共享物理输入资源 |
| cross tenant（即使 deviceId 文本相同） | 独立 lane | 不泄露、不互相阻塞 |
| 同 device 的 replacement clientSession | 新 session 是不同 owner、同 lane | 不能 reenter/takeover旧 holder |
| capacity 已满 | typed capacity rejection before write | 已 admitted FIFO 完全不变，无 eviction |
| enter response/continuation 在 grant 前丢失 | interrupt/cancel 移除 waiter | 不占 holder |
| grant 后 caller 未观察到返回但 stack正常 unwind | enter rollback或 finally exact release | 不自动重投 |
| Java stack 异常但 finally执行 | exact forceRelease | 后续 head继续 |
| Java stack/host 局部失控且 finally未执行 | fail-closed orphan hold + alert | 无 TTL/takeover；需停用该 authority/进程运维恢复 |
| Cloud 进程 restart | in-memory holder/queue/lane audit 全失 | 不声称恢复旧顺序；current coordinator/slot 也须由 activation 流重建，旧 run不可自动续持 |

“response loss”在 W0.2 是 same-process Java 调用，不存在 endpoint ACK/重投协议。未来若把 turn authority 拆成远程服务，
必须另开 durable idempotent grant/fencing 设计；不得把本 in-memory handle 假装成跨进程 lease。

### 9. Exact 文件写集与构造/激活图

#### 9.1 父级批准后的 W0.2 首波写集（new-only）

| 仓库/文件 | 操作 | 内容 |
|---|---|---|
| Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskTurnCoordination.java` | New | 唯一 public 两方法 final contract |
| Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskTurnAuthority.java` | New | package-private authority/handle、FIFO、owner/lane、capacity、typed lifecycle signal/logging |

DHXY 写集为空。首波不改 `CloudTaskRunAuthorityAssembly`、`CloudServiceHost`、
`RemoteTaskRunCoordinator`、`CloudTaskRunExecutionGate`、K slot、H State owner、I properties、A PAUSED observer
双仓文件、B artifact/host 文件、AutoCombat 或任何 caller。两个目标当前均不存在，适合 new-only；若实施前目标由并行
worker 创建，J 立即停止并回报，不覆盖。

#### 9.2 后置 assembly/AutoCombat 构造图（不属于首波写集）

```text
RemoteTaskRunCoordinator + RemoteGameCommandBroker
        -> CloudTaskRunAuthorityAssembly (唯一 process authority)
             -> existing execution gate / retained ledger / H State owner
             -> one CloudTaskTurnAuthority
             -> activation owner mints one K CurrentContextSlot per taskRun
             -> CloudTaskTurnAuthority.createHandle(slot) exactly once per taskRun
             -> per-taskRun AutoCombatService receives public CloudTaskTurnCoordination
```

- pause/stop/complete transition 返回后，future activation owner 对该 exact handle + transition 前不可伪造 old context 调
  `signalLifecycleChange`；resume 必须先让旧 H projection/turn waiter unwind，调用 `requireQuiescent(handle)` 证明无旧
  hold/wait，再按 K 批准合同 install 新 context。terminal close 前同样检查 quiescence；失败只阻止推进/报警，不强释。
- future assembly 必须保留 concrete handle，只把 interface 给 AutoCombat；不得把 create/signal 暴露给 host/business。
- host/server/endpoint/poller 不在本波接线。authority/handle 即使编译存在也无 constructor caller、无线程、无 I/O、
  无 desktop 副作用，保持 dormant。
- 当前 K slot 与 future activation owner 均尚不存在，所以 J Java 实施必须排在 K Implementation APPROVED 之后；不能
  自建 surrogate slot 或修改 assembly 抢先接线。

### 10. Crash、容量与生产 fail-closed 门

- 当前 `RemoteTaskRunCoordinator`、H State owner、action ledger 和本设计 authority 都是 same-process in-memory；W0.2
  只对这个 authority island 声称公平性。多 Cloud 实例若能同时服务同一 `(tenantId,deviceId)` 会产生双 holder，生产
  激活前必须由父级整体架构提供单 authority routing/fencing，或保持 cohort dormant。本设计不偷偷加入 sticky/Redis/PG
  临时权威。
- client/owner crash 后不得凭相同 windowId/HWND/session raw string 接管。old session holder 未正常 finally 时 lane
  fail closed；replacement session 只能走现有 task-run STOP/new activation 流，不能调用 J API强释旧 holder。
- 无 silent eviction、无 lane/history 自动清理。达到 retained lane上限后拒绝新 lane并报警；进程重启会丢失全部
  process-local audit，因此不能称为 durable recovery。
- 3000ms slow hold、queue depth、oldest waiter age、capacity rejection、orphan suspicion 都是日志/指标；任何 threshold
  都不得触发 unlock、reorder 或 retry。

### 11. P0/P1/P2 与自审

#### 已知问题分级

- P0：0（前提是 host/caller 继续 dormant）。
- P1：0 个设计内未决。以下是**激活硬门**而非允许带病运行的 P1：K slot 必须先获 Implementation APPROVED；future
  activation owner 必须落实 lifecycle sticky signal 与 H 同步 projection；同 device 单 authority/fencing 未证明前不得
  生产激活。
- P2：1 个后置集成风险：future assembly 若只把 interface 交给 AutoCombat、却不保留 concrete handle，就无法在
  pause/stop/complete 后做 exact-old-context signal，也不能在 resume install 前做 non-releasing quiescence gate。§9.2 已把
  “assembly 保存 handle + interface 投影”列为强制验收点。

#### 自审结果（不构成 reviewer approval）

- public 面只有 A 冻结的 `enter/forceRelease`；未预造 `leave/tryRun`。
- FIFO、reentry、reason 不参与 owner、all-depth forceRelease、slow warning 均按 HEAD；AutoCombat common-box优先和
  两类 first-aid 调用顺序不改。
- 未引入 ThreadLocal/thread name/global current/raw string key/input queue/HWND/local runner authority。
- pause/stale/stop/terminal 使用现有 typed checkpoint；interrupt 使用现有 Cloud `INTERRUPTED_WHILE_CURRENT` 模型；
  不解析 reason 文本。
- 无 TTL、takeover、lease expiry、自动 retry、额外业务 park/yield/timeout、业务 cleanup 或 silent eviction。
- J 首波与 A/B/H/I/K 写集零交叉；DHXY/host/assembly/caller 全部后置。

### 12. 父级批准后的实施/package 门

1. 必须先看到本报告父级明确 `DESIGN APPROVED`，并确认 Worker K slot 已实现/批准且目标文件仍不存在。
2. 重新记录两仓 HEAD/status/scoped diff；只创建 §9.1 两个 Cloud 文件，不修改报告外代码路径。
3. 按 no-local-test mode 不新增/恢复测试或 source guard；完成 Java 后运行 Cloud 启动路径要求的 fresh
   `mvn -q package`，不得启动 server/host/Task/poller/UI/capture/OCR/input。
4. source review 必查：所有 slot/coordinator typed reads 均在 turn lock 外；capacity write-before-check 为零；
   interrupt/lifecycle cancellation exact-once减计数；queue head不可被 new arrival越过；forceRelease不查 current slot。
5. package 通过后只追加本报告 Implementation #1，记录 exact diff/package 证据和仍待的 assembly/activation fresh-runtime
   门；不提交、不接生产。

`无已批准业务差异；按基线等价迁移。`

Worker J 自审完成，P0=0/P1=0/P2=1（后置 assembly concrete-handle retention 风险，已有强制门；不构成父级批准）。
Design #1 到此停止，等待父级 `DESIGN APPROVED` / `BLOCKED`；批准前继续零 Java/Maven/resources/tests。

## Parent Design Review #1 - DESIGN APPROVED - 2026-07-12

父级已对照 DHXY HEAD `TaskTurnCoordinator`、Cloud typed checkpoint 异常模型、H State owner 与当前 authority assembly。
推荐的 assembly 级 FIFO + per-taskRun exact handle 能保持 AutoCombat 三处 `enter -> finally forceRelease` 的 blocking fair、
holder reentry 与 all-depth forceRelease，不把 ThreadLocal、input-worker thread name 或本地 input authority 搬云。结论：
**DESIGN APPROVED，P0/P1/P2=0**。Worker 自述的后置 assembly P2 由下列绑定门关闭，不作为可带病激活项：

1. Java 实施必须等 Worker K 的 current-context slot **Implementation APPROVED**，且两个目标文件仍不存在；此前 J 只等待，
   不造 surrogate slot，不改 K/assembly/H/A/B 文件。
2. `signalLifecycleChange` 的 typed 分类必须显式分别捕获 `TaskStopRequestedException` 与
   `TaskCheckpointTransitionException.decision()`；不得解析 authorization reason。`TaskCheckpointTransitionException` 当前已有
   public `decision()`，STOP 类型本身即足够分类。classification、slot/current read 和日志输出都在 turn lock 外。
3. authority state lock 的首次获取与 waiter `Condition.await` 都必须 interruptible；任何 post-grant checkpoint/slot gate 失败
   都 exact 回滚本次 depth 或 holder reservation，并保持 admitted counter/FIFO exact-once。
4. same handle reentry 只在 future activation owner 持 H `executionLock` 的同步 projection 内合法。J 首波保持 dormant；未来
   assembly 必须同时保留 concrete handle 与给 AutoCombat 的 interface projection，并在 lifecycle transition 后调用 sticky
   signal、在 resume install/terminal close 前于同一 activation serialization 边界调用 non-releasing `requireQuiescent`。
   该集成门未落地前不得激活 caller/host，不能靠 TTL/takeover 解 orphan hold。
5. 批准写集仍严格为 Cloud 2 New：`CloudTaskTurnCoordination.java`、`CloudTaskTurnAuthority.java`；无 tests、DHXY、assembly、
   host 或 caller 修改。K 获批后由同一 J 实施并运行 fresh Cloud `mvn -q clean package`（不可 skip）。

当前只批准设计，不授权抢跑 Java。**无已批准业务差异；按基线等价迁移。**

## Internal Worker J - Implementation #1 - 2026-07-13

Worker K current-context slot 已获父级 Implementation APPROVED；实施前两项目标再次确认不存在。本轮严格按
Design #1 与 Parent Design Review #1 创建 Cloud 2 New，未覆盖任何并行 dirty/untracked，未修改现有 Java、DHXY、
Maven、resources、tests、assembly、host、caller 或 K slot。

### Exact files

- New `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskTurnCoordination.java`
  - public contract 仅 `enter(String)` / `forceRelease(String)`。
- New `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskTurnAuthority.java`
  - package-private authority/handle/capacity exception；显式 retained lane、holder、FIFO waiter 与 exact accounting。

### 关键不变量

- lane key 仅为 typed `tenantId + deviceId`；owner 为 typed scope 四元组、`taskRunId/taskType`、window 四元组、
  `stopEpoch/runRevision`，并叠加 exact handle object identity；`transactionName` 只用于诊断。
- same-handle 仅 full-owner exact 时重入；新 contender 显式 FIFO tail admission，head-only grant；state lock 首次获取和
  `Condition.await()` 均 interruptible。
- `signalLifecycleChange(...)` 在 turn lock 外分别捕获 `TaskStopRequestedException` 与
  `TaskCheckpointTransitionException.decision()`，只对 exact old-owner waiter 写 sticky cancellation，不释放 holder。
- admission 后 checkpoint、post-grant checkpoint 与 `slot.current()` exact gate 均在 turn lock 外；失败 exact 回滚本次
  reentry depth 或 holder reservation，cancellation/release 对 global/lane admitted counter exact-once。
- `forceRelease(...)` 不读取 current slot，只允许 exact handle 释放其 recorded hold，并一次清空全部 depth；无 hold 幂等
  no-op。`requireQuiescent(...)` 只读拒绝，不 release/cancel/reorder。
- retained lane/global admitted/per-lane admitted 容量分别为 `10000/10000/64`；全部 capacity check 在首次 state write 前，
  满额只抛 typed structural rejection，无 eviction/retry/cleanup。
- 所有 slot/coordinator typed read、typed classification 与 SLF4J 输出均在 turn state lock 外；未引入 ThreadLocal、raw
  string authority、TTL、takeover、lease expiry、自动 retry、host/caller/assembly 或运行入口。

### 自审与门禁

- scoped `git status` 仅显示上述两个 Cloud 文件为 new/untracked；固定 J 日志为本轮唯一文档追加。
- 静态 source review 已检查 public 面、forbidden authority、lock/await、typed gate、日志位置、FIFO head、capacity-before-write、
  cancellation/rollback/release accounting；本 Worker 不是 reviewer，本节不构成 approval。
- 按父级最新并行协调，本 Worker **未运行 Maven/测试/编译**，也未启动 application/server/host/Task/poller/UI/capture/
  OCR/input。父级将在 Worker J/M 同目录写入稳定后亲自运行 fresh Cloud `mvn -q clean package`；当前不声称 build ready、
  runtime ready 或可激活。
- 无已批准业务差异；按基线等价迁移。

## Parent Implementation Review #1 - APPROVED - 2026-07-13

父级已逐行复核 `CloudTaskTurnCoordination.java` 与 `CloudTaskTurnAuthority.java`，并在 Worker J/M 写入均稳定后亲自执行
Cloud `mvn -q clean package`。结论为 **Implementation APPROVED，P0/P1/P2=0**：

- public 面严格只有 `enter(String)` / `forceRelease(String)`；具体 handle、authority 与 capacity exception 均为
  package-private，不暴露 raw scope/window/revision 或自行 mint 入口；
- owner 使用 exact scope/taskRun/taskType/window/stopEpoch/runRevision + handle identity，lane 仅 typed tenant/device；
  transactionName 只做诊断；
- 首次 admission 使用 interruptible lock，FIFO head-only reservation，capacity check 在首次 retained/admission write 前；
  post-admission checkpoint、post-grant checkpoint 与 K slot exact owner gate 全在 state lock 外；
- waiter cancellation、grant rollback、reentry depth rollback、all-depth force release 与 global/lane admitted 计数均有 exact
  identity/generation/depth 校验；sticky lifecycle signal 不释放 holder，`requireQuiescent` 只读拒绝；
- typed stop/transition 分类分别捕获 `TaskStopRequestedException` 与
  `TaskCheckpointTransitionException.decision()`，没有 reason-string 解析、TTL、takeover、自动 retry、ThreadLocal、host 或 caller；
- fresh Cloud package：exit 0，Surefire `suites=4, tests=21, failures=0, errors=0, skipped=0`。本地 tests 未新增/恢复。

本切片仍保持 dormant；未来只有在 activation assembly 同时保留 concrete handle 与 interface projection、并按设计在 lifecycle
transition/install/terminal 边界调用 signal/quiescence 后，才可激活 caller。该后续接线门不否定本 2-New authority leaf 的批准。

**无已批准业务差异；按基线等价迁移。**
