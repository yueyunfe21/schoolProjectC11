# Cloud TaskTransactionRunner - Internal Worker AA

> Append-only coordination log. Internal Worker AA 只设计/实现；父级线程是唯一 reviewer/approval owner。

## Parent Task Brief #1 - `W-TTR-D1` - 2026-07-13T14:46:00-04:00

先在本日志真实 EOF 追加 `CLAIMED`（task、claimedAt、uniqueWriteSet），随后只做 committed HEAD `0114604e` 的
`TaskTransactionRunner` Cloud lift Design #1。唯一写集为本日志；父级 DESIGN APPROVED 前两仓 Java/Maven/schema/resources/
tests/其它报告/CR/host/caller 全冻结。你不是唯一 Worker，保护所有 dirty/untracked，不回滚、覆盖或 Git mutation。

### 必读与事实锚

1. `D:\mavenProject\DHXY\AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、迁移矩阵；
2. DHXY HEAD `task/transaction/TaskTransactionRunner.java` 及同包全部 result/decision/yield/outcome 类型；
3. 全树全部 `run/runDynamic/runExclusive/forceReleaseTurn` caller，逐入口记录实际 result/yield/exception/stop 解释；
4. Cloud 已有 `CloudTaskTurnAuthority`、`CloudTaskTurnCoordination`、`CloudTaskServiceExecutionContext/Port`、retained action/
   current-slot/lifecycle；DHXY retained `InputActionQueue/InputSequences/InputActionScope/TaskPauseToken`；
5. 当前 External A/B 与 Internal Z 固定报告和两仓 git status，仅用于避让写集。

### 必须冻结的业务/机械合同

- HEAD `run`、`runDynamic`、`runExclusive` 的 enter/action/outcome/metrics/leave `finally` 顺序，null->FAILED、
  `TaskStopRequestedException`/interrupt->STOPPED、普通 RuntimeException/Error 继续抛；
- `runExclusive` 已在 input worker 时 direct，否则恰好一次 `submitExclusiveAndWait`，严禁 queue-in-queue；
- yield/expected/completed/result 对 `TaskTurnCoordinator.leave` 的原解释，不新增 retry、TTL、park、验证或 fallback；
- Cloud 只拥有业务 transaction/turn/result/yield phase；DHXY 永久拥有物理 input worker、exclusive owner、pause/stop/checkpoint、
  exact window/focus/input safety。Cloud 不得获得 callback、`Supplier`、InputProvider、ThreadLocal 或 raw queue API；
- 不把 metrics/log negative signal升级成业务真值，不因远程 UNKNOWN 返回伪 FAILED/STOPPED。

### Design #1 交付

1. 完整 public API、全部 caller、结果/异常/finally/metrics 时序矩阵；
2. 明确 `run`/`runDynamic` 的 Cloud business turn owner，以及 `runExclusive` 如何使用 DHXY retained whole-pass exclusive
   capability而不把 Java callback/业务代码送回本地；normal 与 already-on-input-worker 路径均须可达、无 queue-in-queue；
3. pause/resume/stop/UNKNOWN/late outcome、stable transaction/action identity、duplicate/replay/final consume、restart/cap 合同；
4. 与现有 `CloudTaskTurnAuthority`/Full R0/SummonSkill whole-pass 的唯一关系：复用或明确不能复用，禁止第二 turn/input owner；
5. exact New/Modify/No-Modify 文件表、依赖 DAG、可独立真实 leaf；无真实 leaf 就诚实写无，不造 wrapper/enum shell；
6. 每波 Cloud `mvn -q clean package`（不 skip）与触碰 DHXY 时 compile 门；host/Task/caller保持 dormant。

只做实施级 Delta 设计，不逐方法反编译、不写泛化论文、不改 Java。最后追加 self-QA P0/P1/P2（不构成批准）并停止等待父级。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Implementation #1 - `W-TTR-0`

- implementedAt: `2026-07-13T15:07:36-04:00`
- workerRole: `Internal Worker AA`（实现与 self-QA，不承担 review/approval）
- businessBaseline: DHXY committed `0114604e1ff5f15491d2910959c45252e893d04f`
- cloudBaseline: branch `navigation-migration`, committed `3b988caa010254973e03342272e6d1d6a9685b01`
- result: 仅实施父级批准的 `W-TTR-0`；未创建完整 runner，未接 assembly/lifecycle/caller/host。

### Exact source diff

1. Modify `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskTurnCoordination.java`
   - import 现有 `TaskTransactionOutcome`；
   - public authority contract 新增 `void leave(TaskTransactionOutcome outcome)`；
   - JavaDoc 明确 null outcome 是 exception path，释放全部 held depth。
2. Modify `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskTurnAuthority.java`
   - import 现有 `TaskTransactionOutcome/TaskTransactionResult/TaskYieldPolicy`；
   - 新增 authority `leave(handle, outcome)`，全部状态读写仍在现有 `stateLock` 下；
   - 新增单个 private `shouldYield(outcome)`，逐枚举复现 `0114604e`；
   - 现有 `releaseHeld` 原位增加 `outcome` 参数；force/cancel/rollback 全部显式传 `null`；
   - 现有 `ReleaseSnapshot` 原位携带 outcome，未新增类型；
   - outcome release 的 `ReleaseHistory` 改写真实 `outcome.name/result`，release log 改写真实
     `transaction/result/yieldPolicy`；
   - outcome 为空时进入原 force log 分支，现有 `forceRelease`、cancel、rollback 日志与释放语义不变；
   - 现有 `CloudTaskTurnHandle` 直接委托 `authority.leave(this, outcome)`。

两个目标文件在共享工作区原本属于 pre-existing untracked `remote/` 树，因此 `git diff` 不会生成 tracked unified diff；上表是
本轮 apply-patch 的完整方法级 Delta，没有以 add/overwrite 方式重建文件。实现后 SHA-256：

- `CloudTaskTurnCoordination.java`: `0D86D9FEE2FF5BC4FD9FA5A09BB8EEF16C26A9CE90B410BC1D9FEF8E02C6F883`
- `CloudTaskTurnAuthority.java`: `34E139B2B5631291E56B517F8D2583DD10D9FA18A0E78688B7B97561CB95713F`

### Six-branch baseline check

| Branch | Implemented result |
|---|---|
| 1. handle 没有 held turn | no-op；不创建 state、不释放其它 owner |
| 2. `outcome == null` | reason=`exception`，调用同一 `releaseHeld` 全深度释放；force-style null-outcome 诊断保持 |
| 3. `completed == false` | `shouldYield=true`，全深度释放，日志/历史携带真实 outcome |
| 4. completed 且 result 为 `STOPPED/FAILED/RETRYABLE_ERROR/TASK_FINISHED/PATHING_STARTED/SHARED_STATE_TRIGGERED` | 全部全深度释放 |
| 5. completed、result 可继续但 yield 非 `CONTINUE_CHAIN` | 全深度释放；覆盖 `MUST_YIELD/MAY_YIELD/RETRY_LATER/STOP_CHAIN` |
| 6. completed、result 可继续且 yield=`CONTINUE_CHAIN` | depth>1 精确减一；depth==1 保持；两者均记录真实 transaction/result/yield/depth |

`forceRelease` 仍无条件释放该 handle 的全部深度；本轮没有增加 retry、TTL、park、fallback、verification、owner 或 cap。

### Build gate

- command: `mvn -q clean package`
- repo: `D:\mavenProject\dhxy-cloud-brain`
- skip flags: 无（未使用 `-DskipTests` 或其它 test skip）
- result: `exit code 0`
- elapsed: `78.8s`
- artifact evidence:
  - `target/dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`, `120067667` bytes, timestamp `2026-07-13 15:06:44`
  - `target/original-dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`, `3165175` bytes, timestamp `2026-07-13 15:06:29`
- first package attempt: 工具短超时在约 `5s` 终止，不计门禁；随后完整重跑取得上述 exit `0`。

### Frozen-scope check

- 未创建 `TaskTransactionRunner.java`。
- 未修改 `CloudTaskRunAuthorityAssembly.java`、`CloudTaskRunRetainedLifecycleActivationAdapter.java`、DTO、tests、Maven、schema、
  resources、host、caller 或 DHXY Java。
- 未执行 Git mutation，未回滚/覆盖其它 dirty/untracked。
- 固定报告仅做本次 append-only Implementation #1 记录。

### Worker self-QA

- `P0`: `0` 个已知未解决项。没有第二 turn/input owner；没有 callback/ThreadLocal/UNKNOWN 业务映射。
- `P1`: `0` 个已知未解决项。六类 leave 分支、全深度 release、nested depth、真实 outcome history/log 均与批准块一致。
- `P2`: `0` 个已知未解决项。force/cancel/rollback 路径仍显式使用 null outcome，原 force 日志分支未改。
- verification: Cloud 完整 clean package exit `0`；本任务未获准新增测试，未修改 tests。
- status: `Implementation #1 published for parent review`；本 self-QA 不构成父级 approval，完整 `W-TTR-1` 仍未实施。

## CLAIMED - `W-TTR-D1`

- claimedAt: `2026-07-13T14:43:52-04:00`
- uniqueWriteSet: `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-task-transaction-runner-worker-aa.md`（append-only）
- workerRole: `Internal Worker AA`（只做设计/实现，不承担 review/approval）

## Design #1 - Cloud business transaction scope + retained exclusive session

- designedAt: `2026-07-13T14:56:50-04:00`
- baseline: DHXY committed `HEAD 0114604e1ff5f15491d2910959c45252e893d04f`
- scope: 仅设计 `TaskTransactionRunner` Cloud lift；本节没有修改 Java/Maven/schema/resources/tests/CR/host/caller。
- business contract: **无已批准业务差异；按 `0114604e` 基线等价迁移。**
- naming rule: 下文 `R-X1/R-X2/R-X3` 指通用 retained whole-pass exclusive 设计波，不是 Internal Z 正在实现的
  `SUMMON_SKILL_WHOLE_PASS` 专用 operation。

### 1. Baseline evidence and non-negotiable invariants

1. `TaskTransactionRunner.run(...)` 的 committed 顺序是：
   `turn.enter -> start log -> action -> TaskTransactionOutcome(completed=true) -> finish log -> finally latency log -> metrics -> turn.leave`。
   `turn.enter` 位于 `try/finally` 外，因此 enter 失败时没有 transaction metrics，也不得调用 leave。
2. `runDynamic(...)` 同序；callback 返回 `null` 时形成 `FAILED + fallbackYieldPolicy`，callback 返回对象但其中
   `result == null` 时构造器归一为 `FAILED`，其中 `yieldPolicy == null` 时归一为 `CONTINUE_CHAIN`。
3. `runExclusive(...)` 先 enter business turn。若当前确实位于唯一 input worker，则业务 callback 直接执行；否则只提交一次
   `submitExclusiveAndWait`，callback 内不得再次入队。队列未完成时形成 `completed=false`，线程已中断为 `STOPPED`，否则为
   `FAILED`。
4. 固定/动态/独占三条路径中，业务返回 `null` 均不能被解释为成功。`TaskStopRequestedException` 形成 `STOPPED`；
   被中断的 `RuntimeException` 形成 `STOPPED`；普通 `RuntimeException` 与 `Error` 必须原样重新抛出。
5. `finally` 中的 metrics/log 只记录已经发生的结果；它们的负信号不是新的业务真相。Cloud/DHXY transport 的
   `UNKNOWN`、pause handoff、stale generation 不能伪造成 `FAILED` 或 `STOPPED`。
6. `TaskTurnCoordinator.leave` 的 committed 规则保持不变：outcome 为空或 `shouldYield=true` 时释放全部深度；
   keep-turn 且 depth > 1 时仅减一；keep-turn 且 depth == 1 时保留；`forceReleaseTurn` 始终释放全部深度。
7. `shouldYield` 的 committed 规则保持不变：`completed=false` 必须释放；`STOPPED/FAILED/RETRYABLE/TASK_FINISHED/
   PATHING_STARTED/SHARED` 必须释放；其余仅 `CONTINUE_CHAIN` 可保留，`RETRY_LATER/MUST_YIELD` 释放。
8. Cloud 是业务 transaction/turn/result/yield 的唯一 owner；DHXY 只拥有 HWND/focus/physical input/input worker/
   pause-stop checkpoint 与 retained exclusive session。Cloud 不得收到或发送 `Supplier`、Java callback、
   `InputProvider`、`ThreadLocal` 或 raw queue 对象。

### 2. Complete committed caller inventory

| Caller at `0114604e` | API | Current result use | Cloud migration constraint |
|---|---|---|---|
| `WubeiTask:560` | `runDynamic` | callback 先执行 ready/phase，再应用 task policy；`STOPPED` 终止，`FAILED` recovery，path/shared 与 yield 使用现有分支 | Cloud caller 原位执行业务并提交同一 result/yield；runner 不新增 recovery/park/retry |
| `WubeiTask:463,684` | `forceReleaseTurn` | execute-finished/recovery 全深度释放 | 保留原调用点和 all-depth 语义 |
| `FiveRingTaskV2:468` | `run` | 非 outside phase；`STOPPED` 终止，`FAILED` 失败，既有 outcome 决定 yield | Cloud caller 原位执行 phase；固定 `CONTINUE_CHAIN` 不改 |
| `FiveRingTaskV2:3144` | `runExclusive` | accept-dialog whole pass；未完成返回 `NOT_ACCEPTED`；完成后主要读取现有 refs | 业务判断必须留 Cloud；仅截图/输入步骤走 retained exclusive capability；不得改现有 ref/outcome 判定 |
| `FiveRingTaskV2:3201` | `run` | checkpoint + initial NPC click；stop 由 checkpoint 抛出；返回 `reachedExpectedResult` | 不增加 read/verification；动作结果仍由相同 caller 判断 |
| `FiveRingTaskV2:3432,3444` | `run` | 两种 dialog cleanup；结果为 `RETRYABLE + RETRY_LATER`，caller 忽略 outcome | 不把 cleanup 结果升级为新 gate；异常仍跳过后续日志并上抛 |
| `FiveRingTaskV2:334,535,809` | `forceReleaseTurn` | execute-finished、outside-yield、outside-enter | outside phase 仍由 caller 合成 outcome；runner 不接管 outside phase |
| `XiuluoTaskV2:965` | `runDynamic` | cloud-brain commanded precombat loop；直接使用 callback result/yield，无第二次 task-policy | 保留 special-result/wait/yield reporting 与 local yield guard `8` |
| `XiuluoTaskV2:1688` | `runDynamic` | 主 phase、compensation、trace、task-policy；stop 终止，failed restart | 保留 exact phase/fallback/round restart；runner 不新增 cloud gate |
| `XiuluoTaskV2:532` | `forceReleaseTurn` | execute-finished 全深度释放 | 保留原调用点和 all-depth 语义 |

`git grep` 未发现上述三类任务之外的真实 `run/runDynamic/runExclusive/forceReleaseTurn` caller。当前本地 caller、host 和
`TaskTransactionRunner` 均保持 dormant，不在本设计波改写。

### 3. Public Cloud API: scope instead of callback

新增 Cloud 原 FQCN：

`dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/transaction/TaskTransactionRunner.java`

它不是本地 runner 的 callback 复制版。它公开一个真实拥有 business-turn/exclusive 生命周期的 scope：

```java
public final class TaskTransactionRunner {
    public Transaction begin(
            String transactionName,
            TaskTransactionResult expectedResult,
            TaskYieldPolicy yieldPolicy);

    public Transaction beginDynamic(
            String transactionName,
            TaskTransactionResult expectedResult,
            TaskYieldPolicy fallbackYieldPolicy);

    public Transaction beginExclusive(
            String transactionName,
            TaskTransactionResult expectedResult,
            TaskYieldPolicy yieldPolicy);

    public void forceReleaseTurn(String reason);

    public final class Transaction implements AutoCloseable {
        public boolean actionAllowed();
        public TaskTransactionOutcome outcome();
        public TaskTransactionOutcome complete(TaskTransactionResult result);
        public TaskTransactionOutcome completeDynamic(
                TaskTransactionResult result,
                TaskYieldPolicy yieldPolicy);
        public TaskTransactionOutcome completeDynamicNullDecision();
        public TaskTransactionOutcome stop();
        public void fail(Throwable failure);
        @Override public void close();
    }
}
```

实施约束：

1. `Transaction` 是 non-mintable inner capability，实际持有一次 turn enter、日志/metric 状态，以及可选的 retained exclusive
   lease；不是只转发调用的 wrapper。外部不能构造、不能复制、不能跨 task-run 使用。
2. 不新增 operation/result/yield enum shell。继续直接使用已经迁到 Cloud 的
   `TaskTransactionResult/TaskYieldPolicy/TaskTransactionOutcome`。committed 的 `TaskTransactionDecision` callback record
   不复制；动态 caller 直接把原 decision 的两个字段交给 `completeDynamic`。
3. `begin/beginDynamic/beginExclusive` 均先 `turn.enter`；enter 失败时不创建 scope，也不写 transaction metric/leave。
4. `actionAllowed()` 只在 exclusive acquire 得到明确 `NOT_EXECUTED` 时为 false，此时 scope 已持有 committed 等价的
   `completed=false` outcome，caller 必须直接返回 `outcome()`，不得执行 Cloud 业务。普通和成功 acquired/joined scope
   恒为 true。
5. `outcome()` 在 `complete/stop` 前以及 `UNKNOWN/PAUSED/stale` 路径返回空；不得凭 transport 状态合成 outcome。
6. `complete` 只允许固定/独占 scope，`completeDynamic*` 只允许动态 scope；错误使用抛出 programming error，不转换业务
   结果。一个 scope 最多完成一次。
7. `complete(null)` 形成 `FAILED`；`completeDynamicNullDecision()` 形成 `FAILED + fallback`；
   `completeDynamic(null, null)` 形成 `FAILED + CONTINUE_CHAIN`，完全复现 committed 归一规则。
8. `stop()` 只供捕获精确 `TaskStopRequestedException` 或 committed 等价的 interrupted business execution 使用，形成
   `STOPPED`；pause、UNKNOWN、stale generation 不能调用它。
9. `fail(Throwable)` 不吞异常：它只按下文 exclusive 清理矩阵处理 lease，并把清理失败作为 suppressed failure 附着；
   caller 随后原样 rethrow 普通 `RuntimeException/Error`。这样 Cloud business exception 不会被 DHXY input callback 吞掉。
10. `close()` 只执行一次 committed finally 顺序：latency/finish-or-transition log -> transaction metric -> turn leave。
    有 outcome 时传原 outcome；无 outcome 时传 `null`，由 turn authority 释放。`close()` 不把未完成转成失败。
11. Cloud metric 直接写结构化 transaction metric 日志字段（task/window/name/expected/result/yield/completed/elapsed/transition）；
    本波不复制本地 `AutomationMetricsService`、不新增 metric service wrapper。日志/metric 不参与任何业务分支。

迁移 caller 使用同一个显式模板；runner 本身不接收业务 callback：

```java
TaskTransactionRunner.Transaction transaction = runner.beginDynamic(name, expected, fallback);
try (transaction) {
    TaskTransactionDecision decision = executeCloudBusinessPhase();
    if (decision == null) {
        return transaction.completeDynamicNullDecision();
    }
    return transaction.completeDynamic(decision.result(), decision.yieldPolicy());
} catch (TaskStopRequestedException stopped) {
    return transaction.stop();
} catch (RuntimeException | Error failure) {
    transaction.fail(failure);
    throw failure;
}
```

实际迁移不要求重新引入 `TaskTransactionDecision`；示例仅表达 committed 的两个返回量。pause/stale/UNKNOWN 的 typed
transition 必须在 `RuntimeException` 分支前单独上抛，scope 以 null outcome 关闭，由现有 lifecycle owner 接管。

### 4. Result, exception, finally and metric matrix

| Event | Cloud transaction outcome | Throw/return | Exclusive lease action | `close()` passes to leave |
|---|---|---|---|---|
| fixed/exclusive business returns non-null | `completed=true`, exact result/fixed yield | return outcome | self-acquired exact RELEASE；joined 不释放 parent lease | exact outcome |
| fixed/exclusive business returns null | `completed=true, FAILED`, fixed yield | return outcome | same as normal completion | exact outcome |
| dynamic decision null | `completed=true, FAILED`, fallback yield | return outcome | n/a | exact outcome |
| dynamic result null/yield null | `completed=true, FAILED, CONTINUE_CHAIN` | return outcome | n/a | exact outcome |
| exact `TaskStopRequestedException` | `completed=true, STOPPED`, configured/fallback yield | return outcome | exact ABORT/RELEASE；若 side-effect outcome UNKNOWN 则不得形成 STOPPED transaction outcome | exact outcome or null |
| interrupted business `RuntimeException` | committed 等价 `STOPPED`，仅当不存在 remote UNKNOWN | return outcome | exact cleanup | exact outcome or null |
| ordinary business `RuntimeException` | none | same instance rethrow | exact ABORT；cleanup failure suppressed | null |
| business `Error` | none | same instance rethrow | exact ABORT；cleanup failure suppressed | null |
| exclusive acquire definite `NOT_EXECUTED` | `completed=false`, interrupted=>`STOPPED`, else `FAILED` | `actionAllowed=false`, caller returns outcome | no lease | exact outcome |
| exclusive final `UNKNOWN` / late pending | none | typed uncertain transition | keep Full R0/R-X fence; no blind release/retry | null |
| PAUSED / generation handoff | none | typed lifecycle transition | park same R-X session/cursor when present | null |
| stale/completed lifecycle | none | typed lifecycle transition | current lifecycle owner performs exact cleanup | null |
| `turn.enter` failure | none | original typed failure | no exclusive acquire | no scope, no metric/leave |
| metric/log failure | never changes business result | log sink must be non-throwing/best effort | none | leave still runs in nested finally |
| `turn.leave` failure | never rewrites an already returned outcome | infrastructure failure logged/thrown only after cleanup policy | none | n/a |

`close()` 内必须用嵌套 `try/finally` 保证 metric sink 失败也不能跳过 leave；metric sink 实施为 non-throwing 后，表中
metric failure 仅是最后保险。finish log 只能在 outcome 已形成后写；transition/exception 路径单独写诊断，不冒充 finish。

### 5. Sole Cloud business-turn owner

复用已经 FINAL APPROVED 的 `CloudTaskTurnAuthority`，不创建第二个 coordinator/lock/map：

1. `CloudTaskTurnCoordination` 增加 `leave(TaskTransactionOutcome outcome)`；其 concrete handle 仍是 runner 唯一依赖。
2. `CloudTaskTurnAuthority` 在现有 lane/handle/held-owner/depth 上补齐 committed `leave/shouldYield`：
   null outcome 全释放；`completed=false` 全释放；上述 committed result/yield 表决定全释放；keep-turn depth > 1 减一；
   depth == 1 保持。`forceRelease` 继续全深度释放。
3. 不改变已批准 FIFO、公平性、owner identity、stop epoch/revision 校验和 caps：lane `10,000`、global contender
   `10,000`、single-lane waiter `64`。
4. `Transaction.close` 只调用 concrete handle；不通过静态 holder、ThreadLocal、task name 或 window title 重新查 owner。
5. 一个 `CloudTaskRunAuthorityAssembly` 只创建一个 turn authority；一个 current-context slot 只创建一个 concrete handle。
   resume 复用同一个 handle identity，并在新 runtime 可发布前完成 lifecycle signal + quiescence。

这两处 leave 补齐是唯一可以在通用 exclusive 前独立实施的真实 leaf `W-TTR-0`：它扩展既有真实 authority，而且 J 的
批准记录明确把 leave 留给后续 TaskTransactionRunner；它不是 dormant wrapper/enum shell。

### 6. `runExclusive`: Cloud business, DHXY physical exclusivity

#### 6.1 Sole input owner

1. 不复用 Internal Z 的 `SUMMON_SKILL_WHOLE_PASS` 专用 operation。该 operation 把 SummonSkill 完整业务 pass 放在 DHXY
   callback 内，若泛化给 runner 会直接违反“Cloud 留业务、不发送 Java callback”的边界。
2. 复用已批准的 `CloudTaskExclusiveInteractionState`（R-X0）以及待实现的通用 R-X1/R-X2/R-X3：
   - R-X1：DHXY `InputExclusiveSessionCoordinator` 与唯一 input worker/session lane；
   - R-X2：typed `EXCLUSIVE_INTERACTION` ACQUIRE/CAPTURE/INPUT_BUNDLE/REBIND/RELEASE/ABORT transport；
   - R-X3：Cloud `CloudTaskExclusiveInteractionAuthority`、retained state/ledger 和 service projection。
3. Runner 只能持有 R-X3 给出的窄 capability；它不能访问 local queue、worker、`InputProvider` 或专用 Summon operation。
4. exclusive transaction 内业务代码继续在 Cloud 顺序运行；截图和输入通过同一 retained session 的 typed steps 完成。
   整个 transaction 完成前 DHXY 不允许其他 session lane 插入物理输入。

#### 6.2 Normal path

顺序固定为：

`Cloud turn enter -> R-X ACQUIRE exact final -> Cloud business reads/decisions -> typed capture/input steps under same session ->
R-X RELEASE exact final -> construct TaskTransactionOutcome -> finish log -> metric -> Cloud turn leave`。

- ACQUIRE 只有 exact acquired 才允许业务开始。
- RELEASE 只有 exact released 才允许形成 `completed=true` outcome。
- definite `NOT_EXECUTED` 映射 committed 的 `completed=false`；UNKNOWN 保留 R-X fence，不形成 transaction outcome。
- runner 不自动 retry、TTL、extra verification、park、fallback 或 fail-closed business result。

#### 6.3 Committed already-on-input-worker equivalent

Cloud 没有 input-worker thread，因此不得复制 `Thread.currentThread().getName()` 判断。等价路径由 authority capability 证明：

1. normal caller 没有 capability，必须 ACQUIRE 一次。
2. 只有已经持有“同一个 active R-X session + 同一个外层 transaction turn”的 trusted parent scope，才可经 package-private
   `beginExclusiveWithin(parentTransaction, ...)` join。
3. join 不做第二次 ACQUIRE、不做第二次 local queue submit、不释放 parent lease；child 仍独立记录 transaction outcome/metric。
4. 只有 input session capability、没有精确 parent turn scope 时结构性拒绝；不得从 ambient context 猜测 owner。
5. 外部 public caller 看不到 join API，也不能自行制造 capability。这样 normal 和 direct 两条路径均可达，同时不产生
   queue-in-queue。

`beginExclusiveWithin` 是 `TaskTransactionRunner` 内 package-private 方法，不新增 facade/helper 文件。它复用同一个
`Transaction` ownership implementation，不形成 wrapper nesting。

### 7. Pause/resume/stop and lifecycle handoff

1. 等待 Cloud turn 时 pause/stop/revision change：沿用 `CloudTaskTurnAuthority.signalLifecycleChange` 的 typed cancellation；
   未 enter 就没有 transaction scope/metric/leave。
2. 已持有普通 Cloud turn 时 pause：caller checkpoint 抛 typed PAUSED；scope 以 null outcome leave，释放 business turn；
   current-context resume 必须在旧 handle quiescent 后发布新 runtime。
3. 已持有 retained exclusive session 时 pause：R-X 先 park 相同 session/cursor；旧 transaction 不产生业务 outcome并释放
   Cloud turn；新 runtime 先重新 enter 相同 business lane，再由 R-X REBIND 相同 session/generation 继续同一个 pass。
   这是 lifecycle handoff，不是 `MUST_YIELD/RETRY_LATER`，不得改变 phase 或 action occurrence。
4. stop：若 local action/abort 已得到 exact final，则按 committed checkpoint 形成 STOPPED；若 side-effect final 仍 UNKNOWN，
   task lifecycle 可进入停止，但 transaction result 仍为空，不能假造 STOPPED action outcome。
5. lifecycle adapter 在 resume install 前执行 turn signal/quiescence，并与 R-X parked-session handoff 使用同一 generation 事实；
   任一 handoff 失败时不得发布半装配 runtime。
6. terminal close 依次阻止新 transaction、等待/终止当前 scope、exact abort/release R-X owner、force-release turn，再关闭
   Full R0/final-consume owners。实际顺序由既有 retained lifecycle adapter 单点实现，不散落到 caller。

### 8. Identity, duplicate, replay, late final, restart and caps

1. `Transaction` capability 是同进程一次执行 scope identity，不是新的 transport idempotency key。`transactionName` 仅用于
   诊断，不能作为 owner 或 dedupe key。
2. 非 exclusive `run/runDynamic` 没有 local remote side effect，因此不新建 transaction ledger。pause 后的新 scope 是新的
  执行尝试，但 caller 的 task phase/occurrence 不变。
3. capture/input 等 mechanical action 的 stable identity 唯一复用 Full R0 的
   `(phaseCode, actionSlot, occurrence, attempt)`；exclusive whole-pass/session identity 唯一复用 R-X retained pass key。
4. 同一业务 occurrence 在 pause/resume/UNKNOWN 后必须重用同一 Full R0/R-X identity。runner 不增 attempt、不重发 action；
   只有现有 task policy 明确推进 attempt 时才变化。
5. duplicate request 由 Full R0/R-X sole ledger 返回同一状态；同一 final 只被 final-consume 一次。late final 仍落在原 identity，
   caller 取得 exact final 后才允许完成 transaction。
6. `UNKNOWN` 保持 fence；禁止 runner 自行 retry、过期、清理、转 FAILED 或另开 session。
7. Cloud process restart 没有 durable restore，按现有 contract fail closed：新 broker instance 必须先 exact abort 旧 local owner，
   未确认前不启动同一 task-run。DHXY restart 导致 owner 消失时，Cloud 仍按 UNKNOWN/owner-lost contract 处理，不假造结果。
8. 不新增 transaction map/cap。并发上限继续由 turn authority caps、Full R0 ledger caps、R-X session caps 和 H execution lock
   共同约束；每个 runtime 同时只有其真实调用栈上的 scope。nested depth 由唯一 turn authority 计数。

### 9. Assembly ownership and publication order

`W-TTR-1` 在 R-X3 稳定后一次性装配完整 runner，不先提交半成品类：

1. `CloudTaskRunAuthorityAssembly` 创建唯一 `CloudTaskTurnAuthority`，在 current-context slot 已存在后创建 concrete turn handle。
2. `TaskServiceRuntime` 增加 package-private final ownership 字段：concrete turn handle、完整 `TaskTransactionRunner`、R-X3
   service projection。它们在 runtime 对外发布前全部构造完成。
3. 初始装配顺序：构造未发布 runtime/slot -> 创建 turn handle -> 创建 R-X projection -> 创建 runner -> 一次 attach 到未激活
   runtime -> H activation -> publication。attach 只能在 H activation 前执行一次，不暴露 public setter。
4. resume 顺序：旧 generation signal -> turn/R-X quiescence或park handoff -> 构造完整 next runtime/runner -> prepare H ->
   atomic publication -> activate H。所有可能失败的构造在 H activation 前完成。
5. `CloudTaskRunRetainedLifecycleActivationAdapter` 只保留 concrete owners 并协调 initial/resume/paused/terminal；不复制业务
   runner，不按 task name 反查 owner。
6. `CloudTaskRunCurrentContextSlot` 已能原子发布完整 `TaskServiceRuntime`，本设计不要求修改它。
7. `CloudTaskServiceExecutionContext/Port` 继续只暴露 typed service facts/actions；不把 runner、turn handle、R-X raw authority
   塞进 service port。未来 Cloud Task 构造器从 assembly runtime 得到 runner。

### 10. Exact New / Modify / No-Modify files

#### `W-TTR-0` - independent Cloud turn-leave leaf

| Repo | Action | Exact file | Change |
|---|---|---|---|
| `dhxy-cloud-brain` | Modify | `src/main/java/com/bot/dhxy/cloud/task/turn/CloudTaskTurnCoordination.java` | 增加 typed `leave(TaskTransactionOutcome)` |
| `dhxy-cloud-brain` | Modify | `src/main/java/com/bot/dhxy/cloud/task/turn/CloudTaskTurnAuthority.java` | 在唯一 authority 上补 committed leave/depth/shouldYield |

#### `W-TTR-1` - complete runner + assembly, only after R-X1/R-X2/R-X3

| Repo | Action | Exact file | Change |
|---|---|---|---|
| `dhxy-cloud-brain` | New | `src/main/java/com/bot/dhxy/task/transaction/TaskTransactionRunner.java` | callback-free scope、outcome/exception/finally/metric、exclusive capability orchestration |
| `dhxy-cloud-brain` | Modify | `src/main/java/com/bot/dhxy/cloud/task/authority/CloudTaskRunAuthorityAssembly.java` | sole turn authority/handle、runner、R-X projection 的原子装配与 runtime ownership |
| `dhxy-cloud-brain` | Modify | `src/main/java/com/bot/dhxy/cloud/task/lifecycle/CloudTaskRunRetainedLifecycleActivationAdapter.java` | turn quiescence 与 R-X pause/resume/terminal handoff |

`CloudTaskRunAuthorityAssembly.java` 与 lifecycle adapter 正在 Internal Z/其它 whole-pass 波的共享写域中；`W-TTR-1` 必须等
这些 owner 写卡稳定后基于最新 committed/accepted 内容重读并串行实施，不能覆盖 dirty work。

#### Explicit No-Modify in AA scope

- Cloud：现有 `TaskTransactionOutcome.java`、`TaskTransactionResult.java`、`TaskYieldPolicy.java`、
  `CloudTaskExclusiveInteractionState.java`、Full R0 ledger/broker/final-consume files、
  `CloudTaskRunCurrentContextSlot.java`、`CloudTaskServiceExecutionContext.java`、`CloudTaskServicePort.java`。
  R-X1/R-X2/R-X3 若需修改 service/execution-context，由其 owner 完成，W-TTR 不重复认领。
- DHXY：`TaskTransactionRunner.java`、`TaskTurnCoordinator.java`、`InputActionQueue/Request/Worker`、`InputSequences`、
  `InputActionScope`、`TaskPauseToken`、所有 remote ledger/poller/handler、所有 task caller。
- 两仓：Maven、schema、resources、tests、host/startup wiring、其它报告、CR/dashboard 全部冻结。

本设计阶段实际唯一写集仍只是本 append-only 日志。

### 11. Dependency DAG and implementation waves

```text
committed DHXY baseline 0114604e
        |
        +--> CloudTaskTurnAuthority FINAL APPROVED ----> W-TTR-0 (real leaf)
        |
        +--> Full R0 FINAL APPROVED -------------------+
        |                                              |
        +--> Internal Z dedicated Summon wave stable --+--> R-X1 -> R-X2 -> R-X3
                                                               |
                                                               v
                                                         W-TTR-1 atomic
                                                               |
                          +------------------------------------+--------------------+
                          v                                    v                    v
                 future Wubei caller                    future FiveRing       future Xiuluo
                          +------------------------------------+--------------------+
                                                               |
                                                        final host/cutover
```

- `W-TTR-0` 可立即独立实现，是唯一真实 leaf。
- 完整 runner body 不能在 R-X3 前作为“non-exclusive half runner”先落地；那会形成未装配 shell，并诱导第二次 public API
  改写。故 `W-TTR-1` 是 runner + assembly 的原子波。
- caller/Task/host 的迁移属于后续各自业务 CR；本设计只给出 API 和门禁，不擅自认领。

### 12. Compile/package and handoff gates

1. 每个 Cloud wave 独立执行 `mvn -q clean package`，不使用 test skip；失败不得交给下一波。
2. `W-TTR-0` package 成功后只证明 turn leave leaf 可编译，不宣称 runner/caller 已可运行。
3. `W-TTR-1` 必须在 R-X1/R-X2/R-X3 当前代码 package 成功且共享文件无未决 owner 后开始；完成后再次 Cloud clean package。
4. 本设计的 W-TTR 波不触碰 DHXY，因此没有 DHXY compile 命令。未来 R-X 或 caller 波一旦触碰 DHXY Java，按项目规则执行
   `mvn -q -DskipTests compile`；若该波属于显式测试例外，再额外执行指定 test 且不得 skip。
5. host、Task、caller 保持 dormant，直到 Wubei/FiveRing/Xiuluo 各自完成 baseline 对照和迁移；不得以 runner package 成功要求
   用户 fresh runtime。
6. fresh-runtime 验收点应查看 `logs/dhxy-console.log` 的同一 transaction name/expected/result/yield/completed/elapsed、
   Cloud turn owner/depth，以及 R-X stable pass/session/action identity；不新增本地测试或 replay。

## Implementation-ready EOF delta

- appendedAt: `2026-07-13T14:59:57-04:00`
- precedence: 本节覆盖上文与 package path、scope catch 顺序、当前 exclusive 前置状态冲突的描述；其余 Design #1 保持。

### A. Exact package correction: no bridge shell

当前 Cloud authority/assembly 全部位于同一个真实 package：

`com.yueyunfe.dhxy.cloudbrain.remote`

因此 `W-TTR-0/W-TTR-1` 的 exact files 修正为：

| Wave | Action | Exact Cloud file |
|---|---|---|
| `W-TTR-0` | Modify | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskTurnCoordination.java` |
| `W-TTR-0` | Modify | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskTurnAuthority.java` |
| `W-TTR-1` | New | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/TaskTransactionRunner.java` |
| `W-TTR-1` | Modify | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunAuthorityAssembly.java` |
| `W-TTR-1` | Modify | `src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunRetainedLifecycleActivationAdapter.java` |

`TaskTransactionRunner` 放在 authority package，直接持有 package-private `CloudTaskTurnHandle` 与未来 generic exclusive
projection；Cloud Task 可 import 这个 public runner，transaction DTO 仍 import 现有 `com.bot.dhxy.task.transaction.*`。
不新增 public bridge、adapter interface、holder、factory 或原 FQCN forwarding wrapper。

`CloudTaskTurnCoordination` 的 public API 精确增加：

```java
void leave(TaskTransactionOutcome outcome);
```

`CloudTaskTurnHandle.leave` 直接调用同一 `CloudTaskTurnAuthority.leave(this, outcome)`。authority 内只增加这一条真实状态
转移和 private `shouldYield` 判定，不创建新 map/owner/type。

### B. Correct Java control-flow template

上文把 `catch` 写在 try-with-resources 外会导致 Java 先执行 `close()`，再进入 catch；那会在 `stop()/fail()` 写入前以 null
outcome leave。实施时禁止使用该写法。精确模板是 catch 位于 resource body 内：

```java
TaskTransactionRunner.Transaction transaction =
        runner.beginDynamic(name, expected, fallback);
try (transaction) {
    try {
        StepOutcome step = executeCloudBusinessPhase();
        if (step == null) {
            return transaction.completeDynamicNullDecision();
        }
        return transaction.completeDynamic(
                step.transactionResult(), step.yieldPolicy());
    } catch (TaskStopRequestedException stopped) {
        return transaction.stop();
    } catch (TaskCheckpointTransitionException transition) {
        transaction.fail(transition);
        throw transition;
    } catch (RuntimeException failure) {
        if (Thread.currentThread().isInterrupted()) {
            return transaction.stop();
        }
        transaction.fail(failure);
        throw failure;
    } catch (Error failure) {
        transaction.fail(failure);
        throw failure;
    }
}
```

约束补齐：

1. `complete*/stop/fail` 都先写 scope terminal state，再由 resource `close()` 执行 metric -> leave。
2. `fail(TaskCheckpointTransitionException)` 只记录 typed transition；PAUSED handoff 不 ABORT parked session，terminal/stale cleanup
   由 retained lifecycle owner 完成。
3. exclusive `complete*` 在写 outcome 前取得 exact RELEASE final；RELEASE UNKNOWN 时抛现有 R-X typed uncertainty，scope 保持
   null outcome，`close()` 释放 Cloud turn但不解除 R-X fence。
4. exclusive `stop()` 在写 STOPPED 前取得 exact ABORT/RELEASE final；UNKNOWN 时同样不能写 STOPPED transaction outcome。
5. ordinary `RuntimeException/Error` 的 `fail` 尝试 exact ABORT；cleanup failure 只 `addSuppressed` 到原 failure，原对象仍上抛。
6. `beginExclusive` 在 turn enter 后若 ACQUIRE 抛 typed uncertainty，方法内部必须用与 `close()` 相同的 private finalization
   block 记录 null metric并 leave，然后原样抛出；不得因 scope 尚未返回而泄漏 turn。
7. `begin/beginDynamic` 在 turn enter 后的构造/start-log 异常也走同一 private finalization block。enter 自身失败仍无 finalization。
8. 一个 private `finalizeTransaction(...)` 同时服务正常 `close()` 与 begin-after-enter failure；这是一个真实 finally 边界，
   不再套 `prepare/handle/resolve` 一行 helper 链。

### C. Exact baseline metric projection

Cloud 日志保留 committed metrics 输入，不让新增诊断字段改变含义：

| Mode | outcome present | metric result | metric yield |
|---|---:|---|---|
| fixed | yes | `outcome.result` | configured fixed yield |
| fixed | no | `FAILED` | configured fixed yield |
| dynamic | yes | `outcome.result` | `outcome.yieldPolicy` |
| dynamic | no | `FAILED` | configured fallback yield |
| exclusive | yes | `outcome.result` | configured fixed yield |
| exclusive | no | `FAILED` | configured fixed yield |

`completed`、typed transition、UNKNOWN 可作为额外诊断字段，但不是 `TaskTransactionResult`，不能被 caller/task-policy读取。
metric writer 必须 non-throwing；`turn.leave` 仍放在其外层 `finally`。

### D. Single real prerequisite blocker and minimum leaf

**唯一 blocker：当前 Cloud 没有可供 generic TaskTransactionRunner 使用的 retained exclusive step capability。**

源码中现有 `CloudTaskExclusiveInteractionAuthority` 虽然同名，但它是 Internal Z 的专用实现：仅公开 package-private
`executeSummonSkillWholePass(...)`，业务 pass 在 DHXY 专用 operation 中完成，并带专用 Summon state/intent/result；它没有 generic
ACQUIRE/CAPTURE/INPUT_BUNDLE/REBIND/RELEASE/ABORT projection。W-TTR 复用它会把通用 Cloud business callback/决策重新送回
DHXY，属于明确禁止的业务 ownership 回退。因此它不能冒充 R-X3，也不能被 runner 调用。

该 blocker 不拆成多个泛化 blocker；Full R0、Cloud turn authority、transaction DTO 已存在，不再重复列前置。

**当前可立即实施的最小真实 leaf：`W-TTR-0`。**

- 只修改上述两个 Cloud turn 文件，补 `leave(TaskTransactionOutcome)` 与 committed `shouldYield/depth`。
- 它不依赖 generic exclusive，不激活 runner/caller/host，也不制造占位类。
- 实施后执行 `mvn -q clean package`；通过仅表示 turn leaf 完成。
- full `W-TTR-1` 的首个合法提交必须同时具备 generic retained exclusive projection 和 runner/assembly 接线；在该 projection
  不存在时，没有第二个诚实的 W-TTR leaf。不得先提交 callback runner、non-exclusive shell 或第二 input owner。

这不是“等待其它 Worker”的执行策略，而是父级可直接排程的最小 DAG：先落 `W-TTR-0`；generic exclusive owner 提供精确
projection 后，原子落 `W-TTR-1`。AA 本任务的冻结写集不授权实现这两个源码波。

### E. Final implementation checklist

1. 先以当时 committed Cloud HEAD 重读五个 exact files及 generic exclusive projection；禁止覆盖共享 dirty/untracked。
2. `W-TTR-0` 仅改两个 turn files；核对 null/incomplete/result/yield/depth/force-release 六类分支。
3. `W-TTR-1` 只新增一个 runner 类并修改 assembly/lifecycle 两处；不新增 DTO/enum/wrapper/holder。
4. runner public API 不含 callback/Supplier/InputProvider/raw ID；join API package-private且要求 exact parent scope。
5. 每个 begin 的 enter-after-failure 都 finalizes；enter failure 本身不 finalizes。
6. caller 的 stop/transition/runtime/error catch 必须在 resource body 内；不允许 close-before-outcome。
7. exclusive 只有 exact acquire 才 actionAllowed；只有 exact release 才 completed；UNKNOWN 永不转 FAILED/STOPPED。
8. pause 复用同一 R-X stable pass/action identity；resume 不新铸 action、不自动 retry。
9. metrics 按上表投影且不参与业务；leave 永远位于外层 finally。
10. Cloud 每波 `mvn -q clean package`；DHXY 未触碰不执行 compile；host/caller 继续 dormant。

## Self-QA - Internal Worker AA

- performedAt: `2026-07-13T14:59:57-04:00`
- role boundary: 仅自检设计完整性，不构成 review、approval 或 CR 完成结论。
- `P0`: `0` 个已知未解决项。设计没有把 Cloud business callback 下放 DHXY，没有第二 turn/input owner，没有把 UNKNOWN
  伪装为业务结果。
- `P1`: `0` 个已知未解决项。自检发现并在 EOF delta 修正了两项实施阻断：Cloud exact package/file path，以及
  try-with-resources 的 close-before-catch 顺序；最终文件表/API模板以 EOF delta 为准。
- `P2`: `0` 个已知未解决项。caller inventory、null normalization、exception rethrow、finally/metric、turn depth、
  pause/resume/stop、late-final/final-consume、restart/cap 与 compile gates 均已落到实施条目。
- external blocker: 仅 `D` 节所列 generic retained exclusive projection 缺失；它不被降级成 P2，也没有用专用
  `SUMMON_SKILL_WHOLE_PASS` 冒充。
- minimum leaf: `W-TTR-0`，exact two-file Cloud change；AA 未越权实施。
- changed artifact: 仅本 append-only 日志；Java/Maven/schema/resources/tests/其它报告/CR/host/caller 均未修改。
- handoff: Design #1 implementation-ready，停止并等待父级复审；本 self-QA 不计 reviewer approval。

## Parent Design Review #1 - W-TTR-0 DESIGN APPROVED / Implementation Published - 2026-07-13T15:06:00-04:00（真实 EOF 权威块）

父级对照 committed `TaskTurnCoordinator.leave/shouldYield/releaseAll` 与当前 Cloud
`CloudTaskTurnCoordination`、`CloudTaskTurnAuthority.enter/forceRelease/releaseHeld` 复审。EOF Delta 已修正真实 package 与
try-with-resources 的 close-before-catch 问题；完整 caller/result/exception/finally 矩阵没有把 callback、ThreadLocal、input worker
或 UNKNOWN 迁成 Cloud 业务真值。通用 retained exclusive projection 确实尚不存在，专用
`SUMMON_SKILL_WHOLE_PASS` 不能冒充；因此完整 `W-TTR-1` 继续 gated，但不阻止独立真实叶子 `W-TTR-0`。

结论：**W-TTR-0 DESIGN APPROVED，P0=0/P1=0/P2=0**。Internal Worker AA 立即实施且唯一写集为：

1. Modify `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskTurnCoordination.java`：
   新增 `leave(TaskTransactionOutcome outcome)`；
2. Modify `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskTurnAuthority.java`：
   handle 直接委托同一 authority；authority 在现有 `stateLock` 下保持基线规则：无 held no-op，`outcome==null` 或
   `shouldYield` 全深度释放，keep-turn 且 depth>1 只减一，depth==1 保持；`shouldYield` 精确复现 completed/result/yield 枚举矩阵。

实现绑定：outcome release 的 release-history/log 必须携带真实 transaction/result/yield，不得继续伪记 `force`；现有
`forceRelease` 语义与日志不变。不得新建 map/owner/type/helper 链，不改 assembly/lifecycle/runner/DTO/tests/host/caller/DHXY。
完成后运行 Cloud `mvn -q clean package`（不可 skip），追加精确 diff、六类分支与构建证据；Worker self-QA 不构成父级批准。
完整 `W-TTR-1` 仍等待 generic retained exclusive projection，当前不得创建 `TaskTransactionRunner` 半成品。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Implementation #1 - `W-TTR-0`（真实 EOF 权威重载）

- implementedAt: `2026-07-13T15:07:36-04:00`
- publishedAtEof: `2026-07-13T15:09:00-04:00`
- workerRole: `Internal Worker AA`（实现与 self-QA，不承担 review/approval）
- baseline: DHXY committed `0114604e1ff5f15491d2910959c45252e893d04f`；Cloud branch
  `navigation-migration`, committed `3b988caa010254973e03342272e6d1d6a9685b01`。
- append correction: 首次 Implementation 文本因非唯一 Markdown 锚误插到日志前部；为保护 append-only 未删除/搬动旧文本。
  本块位于父级批准块后的真实 EOF，是 Implementation #1 的权威交付记录。

### Exact two-file Delta

1. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskTurnCoordination.java`
   - import 现有 `TaskTransactionOutcome`；
   - 新增 `void leave(TaskTransactionOutcome outcome)`，null JavaDoc 定义为 exception/all-depth release。
2. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskTurnAuthority.java`
   - import 现有 transaction outcome/result/yield 三类；
   - authority 新增 `leave(handle, outcome)`，handle 直接委托；状态判断与 depth 变更全部在现有 `stateLock` 下；
   - 单个 private `shouldYield` 精确复制 `0114604e` completed/result/yield 矩阵；
   - 现有 `releaseHeld` 原位接收 outcome；force/cancel/rollback 传 `null`；
   - 现有 `ReleaseSnapshot` 原位携带 outcome，没有新增 map/owner/type；
   - outcome release 的 `ReleaseHistory` 和 release log 使用真实 `outcome.name/result/yieldPolicy`；
   - null outcome 继续走原 force log 分支，`forceRelease` 语义与日志未改。

共享工作区的 `remote/` 树在本轮前已是 untracked，因此 tracked `git diff` 为空；以上是本轮 apply-patch 的完整方法级 Delta。
实现后 SHA-256：

- `CloudTaskTurnCoordination.java`: `0D86D9FEE2FF5BC4FD9FA5A09BB8EEF16C26A9CE90B410BC1D9FEF8E02C6F883`
- `CloudTaskTurnAuthority.java`: `34E139B2B5631291E56B517F8D2583DD10D9FA18A0E78688B7B97561CB95713F`

### Six branches

| # | Exact branch | Result |
|---:|---|---|
| 1 | 无 held state | no-op，不创建状态、不碰其它 owner |
| 2 | `outcome == null` | reason=`exception`，全深度释放，保留 null/force 诊断语义 |
| 3 | `completed == false` | `shouldYield=true`，全深度释放，历史/日志写真实 outcome |
| 4 | completed + `STOPPED/FAILED/RETRYABLE_ERROR/TASK_FINISHED/PATHING_STARTED/SHARED_STATE_TRIGGERED` | 全深度释放 |
| 5 | completed + 可继续 result + yield 非 `CONTINUE_CHAIN` | 全深度释放，覆盖其它四个 yield 枚举 |
| 6 | completed + 可继续 result + `CONTINUE_CHAIN` | depth>1 减一；depth==1 保持；记录真实 transaction/result/yield/depth |

`forceRelease` 仍全深度释放；没有增加 retry、TTL、park、fallback、verification、cap 或第二 owner。

### Build evidence

- exact command: `mvn -q clean package`
- cwd: `D:\mavenProject\dhxy-cloud-brain`
- skip: 无 `-DskipTests` 或其它 skip 参数
- final result: exit code `0`, elapsed `78.8s`
- artifacts:
  - `target/dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`, `120067667` bytes, `2026-07-13 15:06:44`
  - `target/original-dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`, `3165175` bytes, `2026-07-13 15:06:29`
- 工具短超时终止的首次约 5 秒尝试不计门禁；上述结果来自随后完整重跑。

### Scope and self-QA

- 未创建 `TaskTransactionRunner.java`；未修改 assembly/lifecycle/DTO/tests/Maven/schema/resources/host/caller/DHXY Java。
- 未执行 Git mutation，未回滚/覆盖共享 dirty/untracked；固定日志只追加记录。
- `P0=0`：无第二 owner、callback/ThreadLocal/UNKNOWN 业务映射。
- `P1=0`：六类分支、全释放、nested depth、真实 outcome history/log 已核对；日志 EOF 误定位已通过本权威 append 纠正，
  未改写旧日志。
- `P2=0`：force/cancel/rollback 全部显式传 null，仍走原 force 分支。
- status: `Implementation #1 published for parent review`；Worker self-QA 不构成父级 approval，完整 `W-TTR-1` 未实施。

## Parent Source Review #1 - BLOCKED / Implementation Repair #1 Published - 2026-07-13T15:12:00-04:00（真实 EOF 权威块）

父级逐行复核两文件与 DHXY committed `TaskTurnCoordinator.leave/shouldYield/releaseAll`。无-held no-op、null/incomplete/
terminal-result/non-continue 全深度释放、continue depth>1 减一/depth==1 保持、force/cancel/rollback null outcome 与真实 release
日志均正确；但当前仍 **BLOCKED，P0=0/P1=1/P2=0**：

1. **P1：父级批准绑定要求 release-history/log 同时携带真实 transaction/result/yield，当前 history 漏掉 yield。**
   `CloudTaskTurnAuthority.releaseHeld:543-548` 的 `ReleaseHistory` 只写 transaction/result；`ReleaseHistory:858-863` 也没有
   yield 字段，因此下一次 `logAcquired:699-712` 的 handoff history 无法证明上一 transaction 的 yieldPolicy。当前 release
   当场日志虽有 yield，仍只关闭了绑定的一半。

### 当前任务 `W-TTR-0-IMP1-R1`

Internal Worker AA 立即只修改 `CloudTaskTurnAuthority.java` 与本日志：在既有 `ReleaseHistory` 原位加入
`yieldPolicy`，outcome release 写 `outcome.yieldPolicy().name()`，null/force 写 `-`；`logAcquired` 的 handoff 诊断同步输出
`previousYieldPolicy`。不得改变 turn 决策、depth、forceRelease 或新增类型/helper/map；`CloudTaskTurnCoordination.java` 冻结。
完成后运行 Cloud `mvn -q clean package`（不可 skip）并追加精确 diff/构建证据。完整 W-TTR-1 仍冻结，Worker self-QA
不构成父级批准。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Implementation Repair #1 - `W-TTR-0-IMP1-R1`

- repairedAt: `2026-07-13T15:14:16-04:00`
- workerRole: `Internal Worker AA`（修复与 self-QA，不承担 review/approval）
- exact source write: 仅
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskTurnAuthority.java`
- report write: 本固定日志真实 EOF append-only。

### Exact diff

1. `releaseHeld(...)` 原有 `new ReleaseHistory(...)` 增加一个实参：
   `outcome == null ? "-" : outcome.yieldPolicy().name()`。
2. `ReleaseHistory` 原 record 原位增加 `String yieldPolicy` 字段；未新增 record/class/helper/map。
3. `logAcquired(...)` 增加：
   `String previousYieldPolicy = previous == null ? "-" : previous.yieldPolicy()`。
4. `task.turn.handoff` 原单条 latency log 在 `previousResult` 后增加
   `previousYieldPolicy={}`，并按相同位置传入 `previousYieldPolicy`。
5. 未修改 `leave/shouldYield/depth/forceRelease/releaseHeld` 决策；outcome release history 保存真实枚举名，所有 null/force/
   cancel/rollback history 保存 `-`。

Source evidence：

- repaired `CloudTaskTurnAuthority.java` SHA-256:
  `AED690199C8FE3F5C9EE9094EBCCBF5BEC5C6CF762E22692B617D0EA58BDEF1F`
- frozen `CloudTaskTurnCoordination.java` SHA-256:
  `0D86D9FEE2FF5BC4FD9FA5A09BB8EEF16C26A9CE90B410BC1D9FEF8E02C6F883`
- Coordination 哈希与 Implementation #1 完全相同，确认本 repair 未改该文件。

### Build evidence

最终门禁：

- command: `mvn -q clean package`
- cwd: `D:\mavenProject\dhxy-cloud-brain`
- skip flags: 无
- result: exit code `0`
- elapsed: `97.5s`
- artifacts:
  - `target/dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`, `120068594` bytes, `2026-07-13 15:14:04`
  - `target/dhxy-cloud-brain-0.1.0-SNAPSHOT-shaded.jar`, `120068594` bytes, `2026-07-13 15:13:55`
  - `target/original-dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`, `120068594` bytes, `2026-07-13 15:13:55`

首次 repair 门禁没有通过，不计成功证据：21 tests 中 2 个 unrelated `NoClassDefFoundError`，分别缺
`ObjectiveTextRecognizer` 与 `DecisionEngine$XiuluoTrackerDetailCrop`。失败时只读进程证据显示另一条同仓库
`mvn -q clean package`（PID `32448`）及其 surefire（PID `35900`）并发运行，存在共享 `target` clean/test 竞争；未修改这些
共享源码。并发构建结束后完整重跑同一非 skip 命令取得上述 exit `0`。

### Scope and worker self-QA

- `CloudTaskTurnCoordination.java` 与所有其它源码/Maven/schema/resources/tests/host/caller/DHXY 文件均冻结。
- 未创建或修改 W-TTR-1 runner/assembly/lifecycle；未等待或改写 Z 工作。
- 未执行 Git mutation，未回滚/覆盖 dirty/untracked。
- `P0=0/P1=0/P2=0`（Worker self-QA）：父级指出的 retained yield history 缺口已按原 record 原位补齐；turn 业务语义未变。
- status: `Implementation Repair #1 published for parent source re-review`；本 self-QA 不构成父级 approval。

## Parent Source Review #2 - SOURCE APPROVED / Final Build Pending - 2026-07-13T15:19:00-04:00（真实 EOF 权威块）

父级复核 `CloudTaskTurnAuthority` Repair #1 与冻结 `CloudTaskTurnCoordination`。`ReleaseHistory` 现原位保存真实
transaction/result/yield，null/force/cancel/rollback 保存 `-`；下一次 handoff latency 日志同步输出
`previousYieldPolicy`。`leave/shouldYield/depth/forceRelease`、其它 owner/map/type 与 W-TTR-1 均未改变。

结论：**SOURCE APPROVED，P0=0/P1=0/P2=0**。Worker 最终 package exit 0 可证明源码可构建；但首次失败已证实同仓库
并发 clean 会污染 `target`，故父级 FINAL BUILD 等 Internal Z 停止 Cloud 写入后独占 fresh
`mvn -q clean package` 再收口。本条不把 Worker self-QA 当批准，也不放行完整 W-TTR-1。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**
