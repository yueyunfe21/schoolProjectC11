# Cloud LeftTop Retained Pending State - Internal Worker X

## Parent Implementation Brief #1 - `W-LTSS-STATE-IMP1` - 2026-07-13T11:51:00-04:00

### 已批准依据

- 读取 `D:\mavenProject\DHXY\AGENTS.md`、`docs\DHXY_CONTEXT.md`、`docs\ACTIVE_WORK.md` 顶部、迁移矩阵，以及
  `docs\superpowers\plans\reports\2026-07-13-cloud-left-top-status-switch-worker-b.md` 的
  `Parent Design Review #3 - DESIGN APPROVED`。
- DHXY committed HEAD `0114604e` 的 `WindowRuntimeContext.leftTopStatusSwitchClosePending` 四个方法是行为基线。
- Full R0、`WINDOW_CLIENT_PX` 与 current-context slot 已稳定；本切片只落 Cloud retained pending owner，不接 capture/input、
  wire/schema、caller、host 或 DHXY 壳。

### 唯一写集

1. New `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudLeftTopStatusSwitchState.java`
2. Modify `...\remote\CloudTaskServiceExecutionContext.java`
3. Modify `...\remote\CloudTaskRunAuthorityAssembly.java`
4. Modify `...\com\bot\dhxy\runner\context\TaskExecutionContext.java`
5. 本 append-only 报告

不得修改其它文件。你不是唯一 Worker；保护两仓全部 dirty/untracked，不回滚、覆盖、清理、暂存或提交。

### 实现不变量

1. state 由 authority assembly 的单个 `TaskServiceRuntime` 持有；initial 创建一次，resume 必须复用**同一对象引用**，
   terminal runtime close 后随 runtime 释放。不得用 static/ThreadLocal/全局 map/default state，也不宣称 durable restart restore。
2. immutable stable key 精确包含初始 context 的完整 scope `(tenantId,userId,deviceId,clientSessionId)`、taskRunId、
   window `(windowId,nativeWindowHandle,windowTitle,windowClassName)` 与 stopEpoch；**不含 runRevision**。每次 mark/clear/
   consume/read 必须校验调用 context 的这些字段精确相等，允许同 logical run 的新 revision。
3. pending 行为按 HEAD 等价迁移：初始 null；`isPending` 看是否非 null；`mark(source)` 把 null/blank 规整为 `unknown`；
   `consume(source)` 原子 get-and-clear 并返回此前是否 pending；`clear(source)` 清空。source 仅诊断，不参与业务判断。
4. 线程安全用 state 自身单锁或等价原子边界；不得新增 thread/executor/poller/timer/I/O。
5. package-private state 不向业务包暴露 raw owner。`CloudTaskServiceExecutionContext` 只暴露 closed pending 操作；
   `TaskExecutionContext` 仅做同名直接 passthrough，不做 wrapper nesting、额外判断或第二份状态。
6. 不改变 lifecycle/pause/stop/confirm/revision、retained action、port、GameContext State 或 current-slot 语义；host/caller dormant。
7. 不新增/恢复 tests。完成后运行 Cloud `mvn -q clean package`（不可 skip），追加 Implementation #1：精确文件、
   initial/resume/terminal ownership 证明、方法语义、构建统计。自审只算 QA，父级将独立源码审查。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker X - Implementation #1 - 2026-07-13T11:59:27-04:00

### 精确实现 diff

- New `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudLeftTopStatusSwitchState.java`：package-private retained owner，单一 `AtomicReference<String>` marker；stable key 展开且精确比较 `tenantId/userId/deviceId/clientSessionId/taskRunId/windowId/nativeHandle/processId/playerIdentityEpoch/stopEpoch`，不含 `runRevision`。`mark` 将 null/blank 规整为 `unknown`；`isPending`、`consume(getAndSet(null))`、`clear` 均先校验当前 context 的同一 key；source 不参与 pending 判断。
- Modify `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskServiceExecutionContext.java`：两个 package-private 构造器接收相同的 state 引用；仅公开 `is/mark/consume/clearLeftTopStatusSwitchClosePending` 四个 closed operation，内部传入 exact `runContext`；raw owner 仍不向业务包公开。
- Modify `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskRunAuthorityAssembly.java`：initial activation 在 register 后仅创建一次 state，写入首个 `TaskServiceRuntime`；resume 只从 `previousRuntime.leftTopStatusSwitchState()` 传递同一引用给 next service/runtime。未新增 static、ThreadLocal、global map、thread、I/O 或 lifecycle/revision 分支。
- Modify `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\runner\context\TaskExecutionContext.java`：四个同名直接 delegate passthrough，无额外判断、状态或 wrapper chain。

### ownership 证明

- initial：`createCurrentContextSlotActivation` 在 `executionGate.createContext` 和 `actionLedger.registerRun` 后构造一次 `CloudLeftTopStatusSwitchState(runContext)`；该同一引用经 `CloudTaskServiceExecutionContext` 进入首个 `TaskServiceRuntime`。
- resume：`resumeTaskServiceRuntime` 从 `previousRuntime.leftTopStatusSwitchState()` 取得引用，并同时交给 next service context 与 next runtime；key 忽略 `runRevision`，而每次 operation 仍精确验证 scope/run/真实 `RemoteTaskRunWindow` 四字段/stopEpoch。
- terminal：state 没有 assembly static/global owner；它只由 slot runtime/context 对象图持有。既有 `closeAndReleaseTerminalTaskServiceRuntime` 仅通过 terminal handle 的 last runtime 做 terminal release/route retirement，因此 runtime 释放后 state 一并不可达；本切片不新增 cleanup、TTL 或 terminal 语义。

### 构建结果

- 执行目录：`D:\mavenProject\dhxy-cloud-brain`
- 命令：`mvn -q clean package`
- 结果：exit code `0`，实际耗时 `117.5s`；quiet build 无错误输出。未新增、恢复或单独运行测试；未启动 application/server/host/task/poller/UI/capture/input；未 git add/commit。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker X - CLAIMED

- task: `W-LTSS-STATE-IMP1`
- claimedAt: `2026-07-13T11:52:15-04:00` (America/New_York)
- uniqueWriteSet: 本 append-only 报告；New `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudLeftTopStatusSwitchState.java`；Modify `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskServiceExecutionContext.java`、`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskRunAuthorityAssembly.java`、`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\runner\context\TaskExecutionContext.java`

## Internal Worker X - CONTRACT BLOCKER - 2026-07-13T11:52:15-04:00

- severity: P1 / implementation blocked before Java edits.
- evidence: Parent Implementation Brief #1 requires the immutable stable key and every pending operation to compare `(tenantId,userId,deviceId,clientSessionId,taskRunId,windowId,nativeWindowHandle,windowTitle,windowClassName,stopEpoch)`. The actual Cloud binding is `RemoteTaskRunWindow(windowId,nativeHandle,processId,playerIdentityEpoch)` at `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/run/RemoteTaskRunWindow.java`; `CloudTaskRunExecutionContext` and `CloudTaskServiceExecutionContext` project only that tuple. Repository-wide search finds no Cloud `windowTitle` or `windowClassName` field, and `CloudTaskServiceMetadata` does not provide them.
- impact: The allowed four Java modifications cannot construct or compare the required title/class portions. Adding them requires a frozen protocol/binding schema change outside this task's unique write set. Omitting them or substituting unrelated process/identity fields would silently weaken the approved exact-key invariant.
- required decision: approve a narrowly scoped follow-up to carry title/class through the authoritative remote run window/context path, or explicitly revise the approved stable key to the existing authoritative window tuple. No Java implementation has been started; no dirty/untracked files were changed outside this append-only report.

## Parent Contract Resolution #1 - RESUME IMPLEMENTATION - 2026-07-13T11:54:00-04:00

X 的 P1 证据成立：Parent Brief #1 错把旧讨论中的 title/class 四元组写成了当前 Cloud 合同。当前权威窗口类型是
`RemoteTaskRunWindow(windowId,nativeHandle,processId,playerIdentityEpoch)`；approved design 中的 abstract `window`
必须解释为这一个真实权威 tuple，不得扩 wire，也不得从 title/class 另造第二权威。

据此只修正 Brief 不变量 2：immutable stable key 精确包含完整 scope、taskRunId、当前真实
`RemoteTaskRunWindow` 四字段与 stopEpoch，仍不含 runRevision；每次操作按该 tuple 精确校验，resume 允许 revision 变化并
复用同一对象。其它不变量与唯一写集全部不变。该修正关闭 blocker，P0/P1/P2=0；同一 X 立即继续
`W-LTSS-STATE-IMP1` Java 实现和 Cloud package，不再提交 Design。

**无已批准业务差异；按基线等价迁移。**

## Parent Source Review #1 - SOURCE APPROVED / FRESH BUILD PENDING - 2026-07-13T12:02:30-04:00

父级已对四个授权 Java 文件逐行复审，并与 DHXY committed HEAD `0114604e` 的
`WindowRuntimeContext.leftTopStatusSwitchClosePending` 四方法对照：

- `CloudLeftTopStatusSwitchState` 只持一个 `AtomicReference<String>`；null/blank source 规整、pending read、
  atomic consume 与 clear 语义和 HEAD 等价。
- stable key 精确使用当前真实权威 `RemoteTaskRunWindow(windowId,nativeHandle,processId,playerIdentityEpoch)`，
  连同完整 scope、taskRunId、stopEpoch 校验；没有 runRevision，因此同 logical run resume 可复用同一对象，
  其它 run/window/epoch 无法读写。
- initial 在 assembly 只创建一次；resume 同时把 `previousRuntime.leftTopStatusSwitchState()` 传入新 service
  context 与新 runtime；没有 static、ThreadLocal、global map、I/O、thread 或第二权威。
- 两层 context 只暴露四个 closed passthrough；没有 raw owner、wire、input/capture、lifecycle 或 caller 激活。

源码结论：**SOURCE APPROVED，P0=0/P1=0/P2=0**，无需返修。X 报告的 Cloud
`mvn -q clean package` 已 exit 0；由于 U2 已开始独立协议写集，父级最终 `FINAL APPROVED` 暂只等待所有 Cloud
写入稳定后的 fresh `mvn -q clean package`，不把并发构建窗口误当成代码 blocker。X 可关闭，不再保留实现槽。

**无已批准业务差异；按基线等价迁移。**
