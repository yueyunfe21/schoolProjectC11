# Cloud-safe `BaseTaskTemplate` 兼容层设计报告

## Internal Worker F - Design #1 - 2026-07-12

## 0. 角色、范围与停止门

- 角色：内部 Worker F，只负责设计；不是 reviewer，不创建其他 Agent。
- 本轮唯一写入是本报告。未修改 Java、Maven、resources、tests、CR 卡或 dashboard；未运行测试、Maven、应用、host、Task、poller、capture、OCR 或 input。
- 目标是设计最小 Cloud-native `BaseTaskTemplate`，让后续迁入的同步 Task 保留显式
  `execute(TaskExecutionContext)`、`beforeTask -> steps -> afterTask`、`TaskStepExecutor` retry、
  stop/pause typed unwind 与 `GameContext` 结果落态语义。
- 明确禁止复制或伪造本地 `WindowRuntimeContext`、`TaskExecutionContextHolder`、
  `TaskWindowRuntimeService`、窗口 geometry/title/focus/input authority。
- 本报告到此 Design #1 后停止。父级未明确写入 `DESIGN APPROVED` 前，不得实施。
- 业务裁定：**无已批准业务差异；按基线等价迁移。**

## 1. 基线与取证快照

### 1.1 两仓 exact context

| 仓库 | 分支 / revision | 状态与本切片相关事实 |
|---|---|---|
| DHXY | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` | 工作树有大量并行 dirty/untracked；本切片源文件 `BaseTaskTemplate.java`、`TaskStepExecutor.java`、`AutoBattleTask.java`、`TaskExecutionContext.java`、`TaskWindowRuntimeService.java`、`GameContext.java` 对 HEAD scoped diff 均为空 |
| Cloud Brain | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | 工作树有大量并行迁移；Cloud `BaseTaskTemplate.java` 不存在；`TaskStepExecutor.java`、Cloud-safe context/checkpoint/sleep、`GameContext.java` 均为当前未提交迁移材料 |

关键源码 SHA-256：

- DHXY `BaseTaskTemplate.java`：`C3F46EF31F44004BDE51B474CC9FE7CEBEF649EC91A3979C69376FFC56D52354`。
- DHXY/Cloud 当前 `TaskStepExecutor.java`：均为
  `CB7D963E433798E00F5D2C94DE58CF2887F662E2F7C759656B5C14406B5760DD`；Worker D 的 exact-copy 已获父级批准，但仍 dormant。
- DHXY/Cloud `GameContext.java`：均为
  `26B4A9A7963E4E4159D835CD3AF8E3A9EDEB2227A744F7E5C07E0E7877DAEEC9`。
- DHXY `AutoBattleTask.java`：`30E1765D3C36F086A50C8E90395A530E3BDD00A7E1F81BA7A9AD7CBCEDBF4637`。

### 1.2 已读业务/架构基线

- `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/业务逻辑.md`。
- `docs/ACTIVE_WORK.md` 顶部 CR271：Task/Service context、retained action port、Cloud-native image processor、same-process typed checkpoint/sleep 已关闭；A resume-confirm 与 B artifact/template 仍有独立门；host/Task cohort 持续 dormant。
- `docs/superpowers/specs/2026-07-12-service-migration-matrix.md`：核对 `GameTask`、`BaseTaskTemplate`、`TaskStepExecutor`、`GameContext`、`TaskTeamAssignmentPolicy` 及隐式状态/异常映射条目。
- `docs/业务逻辑.md` 的五倍/修罗总门：迁移不得新增 TTL、额外业务验证、retry、cleanup、park/yield、fallback 或改变 phase/输入/验证顺序。本设计只调整 Cloud lifecycle plumbing，不解释任何任务业务 outcome。

## 2. DHXY 源兼容面与调用者

### 2.1 `BaseTaskTemplate` 每个 public/protected 方法兼容表

| 源 API | 源语义 | 当前直接调用/覆盖 | Cloud Design #1 兼容结论 |
|---|---|---|---|
| `protected BaseTaskTemplate(GameContext, TaskStepExecutor)` | 保存状态容器与 step executor | `AutoBattleTask` 构造器 `super(...)` | **保留同签名**；不注入 holder/window/runtime/host authority |
| `public TaskRunResult execute()` | `execute(buildExecutionContext())`，可自行造本地兼容 context | `GameTask` 契约；`AutoBattleTask` 自己覆盖为 `execute(null)` | **保留签名但禁止执行**：立即抛 typed `MISSING_CONTEXT` unwind；绝不造 null/default context |
| `public TaskRunResult execute(TaskExecutionContext)` | resolve context；before；checkpoint；build/execute steps；after；异常收敛 | `WindowTaskRunner` 经 `GameTask` 调用；`AutoBattleTask` 覆盖 | **保留主入口**；只接受 authority assembly 已铸造的显式 context；typed lifecycle transition 必须原样上抛，不能变 `FAILED` |
| `public void stop()` | 日志并把 `GameContext` 置 `IDLE/FREE` | Cloud 当前无 caller；本地 runner 可调 `GameTask.stop()` | **保留接口方法但 fail-closed 禁用**；Cloud stop 只能经 coordinator lifecycle，方法抛明确 unsupported/authority exception，不伪装成已停止 |
| `protected List<TaskStep> buildSteps(context)` | 默认空列表 | 当前直接 subclass 未覆盖 | **原样保留**；空列表基线仍返回 `SKIPPED` |
| `protected void beforeTask(context)` | `BotStatus.RUNNING` | 当前直接 subclass 未覆盖 | **原样保留**；只写当前调用已绑定的 `GameContext.State` |
| `protected void afterTask(context,result)` | success/stopped/skipped -> `IDLE/FREE`；failed -> `ERROR` | 当前直接 subclass 未覆盖 | **原样保留**；typed pause/stale/completed 不调用该方法，避免伪装业务终态 |
| `protected TaskRetryPolicy getRetryPolicy(context,step)` | context policy 优先，否则 `none()` | `AutoBattleTask` 覆盖为 `none()` | **原样保留**；不引入 Cloud policy 决策或默认 retry |
| `protected TaskStepResult executeStep(context,name,action)` | 委托四参 overload | 当前 direct subclass 无调用 | **保留**；context 先走 strict explicit-context 门 |
| `protected TaskStepResult executeStep(context,name,action,retry)` | named step + executor | 当前 direct subclass 无调用 | **保留**；retry 数量/延时只来自既有 policy |
| `protected TaskStep namedStep(name,action)` | 适配 lambda 为 `TaskStep` | 当前 direct subclass 无调用 | **原样保留**；无 authority、无持久状态 |
| `protected void sleepSafely(context,millis)` | `TaskSleep.sleepOrStop` | `AutoBattleTask` 每轮调用 | **保留同签名**；只调用 Cloud explicit-context `TaskSleep`，null 正时长 fail-closed |
| `protected void logWindowContext(context)` | 记录 window/role/hwnd/geometry/title | `AutoBattleTask.execute(context)` 调用 | **保留方法名，缩窄内容**：只记 exact `windowId/role/nativeHandle identity/processId/taskRunId/revision/stopEpoch`；不读/记录 live title/geometry/focus |
| `protected boolean activateWindowIfReady(TaskWindowRuntimeService,context,name)` | ready 检查后 focus，本地窗口权威 | 当前 direct subclass/caller **零调用** | **删除，不提供 Cloud overload**；输入执行器在 retained typed action 内自行做 exact-window/focus fence，Task 无 standalone focus 权限 |
| `protected TaskExecutionContext resolveExecutionContext(context)` | null 时 `buildExecutionContext()` | `AutoBattleTask.execute(context)` 调用 | **保留方法名但改为 strict resolver**：非 null 原样返回；null 抛 typed `MISSING_CONTEXT`；不查 holder |
| `protected TaskExecutionContext buildExecutionContext()` | builder 填 task code/name/retry/start time | 仅源 `execute()` 与 `resolveExecutionContext()`；无 subclass 调用 | **删除**；Cloud context 只能由 package-private authority assembly 从 exact confirmed ACTIVE binding 创建 |

继承自 `GameTask` 的 `getTaskCode()`、`getTaskName()` 仍由 concrete Task 实现；本设计不新增任务 code/name 映射。

### 2.2 所有直接 subclass/caller

仓库扫描只发现一个直接 subclass：`AutoBattleTask extends BaseTaskTemplate`。

| `AutoBattleTask` 使用点 | 兼容影响 |
|---|---|
| 构造器 `super(gameContext, taskStepExecutor)` | 同签名，无改动 |
| `execute()` -> `execute(null)` | Cloud 迁移该 Task 时必须改成 fail-closed 无参入口或删除该 override；不能继续把 null 当 thread-local fallback。该 Task 本切片不迁、不改 |
| `execute(context)` -> `resolveExecutionContext` | strict resolver 保留源码调用形状；null 立即 typed unwind |
| `logWindowContext(context)` | 保留方法名，日志降为 powerless exact identity/revision |
| `sleepSafely(context, interval)` | 保留；pause/stale/stop 走 Cloud typed checkpoint |
| `getRetryPolicy(...)` override | 保留 `none()`，不改变自动战斗 retry 语义 |

间接调用链是 `DefaultTaskFactory -> ObjectProvider<AutoBattleTask>` 与
`WindowTaskRunner -> GameTask.execute(context)`，两者均是 DHXY 本地 runtime，不迁入本切片。Cloud 当前没有
`BaseTaskTemplate`、concrete Task 或 Task host caller。

## 3. Cloud `TaskExecutionContext` API 已有/缺失清单

### 3.1 已有且允许使用

| 类别 | 已有 API |
|---|---|
| 业务元数据 | `getTaskCode/Name`、`getRequestedTaskCode/Name`、`getWindowRole`、team session/leader/support flags、`getRetryPolicy`、`getStartupMode`、`getStartedAt` |
| exact run identity | `getScope`、String `getTaskRunId`、`getTaskType`、`getWindowId`、`getNativeWindowHandle`（仅 normalized identity text）、`getNativeWindowProcessId`、`getPlayerIdentityEpoch`、`getStopEpoch`、`getRunRevision` |
| typed lifecycle | `throwIfStopRequested`、fail-closed `isStopRequested`、只读 `revalidate` |
| retained mechanical facade | `getRemoteGameClient()`，只暴露带 opaque retained handle 的 `WINDOW_FACT`、`CAPTURE`、atomic input bundle |
| powerless helpers | `hasWindow`、`hasNativeWindow`、`hasLocalTeamSession`、startup-mode helpers、`getLogPrefix` |

context 的 public wrapper 构造参数 `CloudTaskServiceExecutionContext` 自身无 public constructor；真正 mint path 仍是
`CloudTaskRunAuthorityAssembly -> CloudTaskRunExecutionGate` 的 package-private exact binding snapshot。

### 3.2 缺失且不得 fake null/default

| 本地 API/字段 | Cloud 状态 | Design #1 处理 |
|---|---|---|
| `builder()` / 任意 metadata-only constructor | 故意缺失 | 不补；`execute()` null/无参 typed fail-closed |
| `TaskStopToken`、`TaskPauseToken`、`isPauseRequested()` | 故意缺失 | 不补 boolean；pause 只能通过 checkpoint `PAUSED` typed unwind |
| `WindowRuntimeContext` / getter | 故意缺失 | 不复制、不包装、不返回 null placeholder |
| `TaskExecutionContextHolder` fallback | Cloud 不存在 | 不新增 holder/ThreadLocal context；显式参数唯一来源 |
| native title/class name | 缺失 | 不补空串；若未来业务确需 live fact，必须使用 retained `WINDOW_FACT` typed action |
| geometry x/y/width/height、`hasNativeWindowGeometry()`、geometry text | 缺失 | 不补 0/`-`；坐标/区域来自具体 retained capture/input request 的 typed coordinate contract |
| focus/ready/activate | 不属于 context | 不新增；本地 executor 在 exact command fence 内拥有机械执行权 |
| 本地 numeric `long taskRunId` | Cloud 使用 String stable id | 不做 hash/parse/0 fallback；迁入 caller 必须适配 String identity |
| pause blocked milliseconds | Cloud checkpoint 固定返回 `0` 并 unwind pause | 不伪造累计时间；依赖 pause-compensated watchdog 的 Task 必须等待独立 persisted timer/rehydration 设计，不能靠 Base 激活 |

## 4. 方案比较与推荐

### 方案 A：完整 exact-copy 本地 `BaseTaskTemplate`（拒绝）

- 优点：源码最少思考，表面编译兼容。
- 致命问题：需要 Cloud context builder、`TaskWindowRuntimeService`、title/geometry/focus，且 null context 会成为伪造
  single-window 模式；typed transition 还会被 `catch (Exception)` 误映射为 `FAILED`。
- 结论：违反本切片 authority 边界，拒绝。

### 方案 B：最小 Cloud-native compatibility template（推荐）

- 新增一个同 FQCN `BaseTaskTemplate.java`，保留同步 Task 的显式 lifecycle/step helper API。
- 删除 `buildExecutionContext` 与 `activateWindowIfReady`；无参 `execute()` 保留 ABI 但 typed fail-closed；
  `resolveExecutionContext` 只接受 non-null authority context。
- `logWindowContext` 只投影已批准的 exact identity/revision；不调用 retained port 获取 title/geometry，因为诊断日志不能凭空
  mint 一个业务 action。真正 live fact 只能由 concrete Service 使用已持久化 opaque action handle 调 typed port。
- 定点修改 Cloud `TaskStepExecutor`：typed transition 先于 generic `Exception` 原样上抛；retry sleep 传当前 context。
- 不新增 host、factory、holder、window service、authority assembly 或持久 catalog。
- 优点：写集最小、同步 Task 调用形状可保留、typed lifecycle 不降级为业务失败、零本地窗口权威。
- 代价：无参旧 Task 不能运行；concrete Task 激活仍受 A resume-confirm、持久业务 state/action handle 与 host gate 约束。

### 方案 C：同时实现 Task host + GameContext/run-state rehydration（后置）

- 可一次解决 bean 装配、per-run `GameContext.State`、pause/resume 重建和 Task 激活。
- 当前没有 durable business phase/action catalog，A resume-confirm 尚未关闭，B artifact 与 Service cohorts 也未闭合；现在做会
  重叠 A/host authority，并把内存状态冒充可恢复状态。
- 结论：后置为独立批准切片；本轮绝不实施。

### 四个本地入口的明确去向

| 入口 | 去向 |
|---|---|
| `execute()` 无参 | 因 `GameTask` ABI 暂保留，但立即 typed `MISSING_CONTEXT`；未来 concrete Task 不得以它作为生产入口 |
| `buildExecutionContext()` | 删除；由 authority assembly/未来 activation owner 在 exact confirmed ACTIVE revision 创建 context |
| `logWindowContext()` | 保留为 powerless identity/revision 日志；live title/geometry 不查询、不伪造，确有业务需要时由 retained typed `WINDOW_FACT` action 承担 |
| `activateWindowIfReady(...)` | 删除且不替换为 public focus helper；exact input bundle 的本地 executor 自行完成必要 focus/fence，Cloud Task 无 standalone activate 权限 |

## 5. 推荐执行流与逐条基线等价证明

### 5.1 `BaseTaskTemplate.execute(context)`

推荐伪流程：

```text
strict non-null context
checkpoint exact current ACTIVE+confirmed before any GameContext mutation
log task + powerless exact identity/revision
try:
  beforeTask(context)
  checkpoint                         // 保留源位置
  steps = buildSteps(context)
  if null/empty -> SKIPPED            // 精确保留源行为，不新增 afterTask
  for step:
    checkpoint
    result = TaskStepExecutor.execute(context, step, policy)
    convert step result
    SUCCESS/SKIPPED -> continue
    otherwise afterTask(result), return result
  afterTask(SUCCESS), return SUCCESS
catch TaskStopRequestedException:
  afterTask(STOPPED), return STOPPED
catch TaskCheckpointTransitionException:
  rethrow unchanged                   // pause/stale/completed/denied 不是业务 FAILED
catch Exception:
  afterTask(FAILED), return FAILED
```

最前一条 checkpoint 是 Cloud authority preflight，只验证 exact lifecycle/revision，不读取游戏画面、不改变业务 phase、
fallback、retry 或输入顺序。源位置的 before 后 checkpoint 仍保留。host dormant 期间该代码不可达。

### 5.2 `beforeTask/afterTask` 与 `GameContext`

| 路径 | DHXY 基线 | Cloud 等价 |
|---|---|---|
| before 正常 | `BotStatus.RUNNING` | 同一 enum、同一 setter、同一值 |
| 全部 step 成功或逐个 skipped 后完成 | `afterTask(SUCCESS)` -> `IDLE/FREE` | 完全相同 |
| step 返回 FAILED | `afterTask(FAILED)` -> `ERROR`，ActionState 不强制 FREE | 完全相同 |
| step 返回 STOPPED | `afterTask(STOPPED)` -> `IDLE/FREE` | 完全相同 |
| stop exception | catch -> `afterTask(STOPPED)` -> return STOPPED | 完全相同 |
| generic exception | catch -> `afterTask(FAILED)` -> return FAILED | 完全相同 |
| `buildSteps` null/empty | 源直接 return SKIPPED，**不调用 afterTask**，因此已执行的 before 可能仍为 RUNNING | 精确保留该既有行为；本切片不顺手修复 |
| lifecycle PAUSED/stale/newer/completed/denied | 本地 pause 原栈阻塞，源无 Cloud revision 概念 | Cloud typed unwind 原样上抛，绝不调用 FAILED/STOPPED after；未来 host 按 lifecycle 处理，不创造业务终态 |

`GameContext.java` 本轮不改。它当前 exact-source 带 `ThreadLocal<State>` 与共享 `defaultState`，但 Cloud 当前
`CloudServiceConfiguration` 只扫描 `com.bot.dhxy.service`，不会自动注册 `GameContext`/Task/template。未来 host 激活必须
为每个 exact task run 绑定独立 State，并在 finally 清除；在该 owner 与持久业务恢复门完成前，禁止激活 Task cohort。

### 5.3 `TaskStepExecutor` retry/sleep/stop

| 规则 | 源基线 | Cloud Design #1 等价证明 |
|---|---|---|
| retry policy 优先级 | override -> context -> `none()` | 不改 |
| attempt 计数 | `attemptedRetries=0`；`canRetry` 后先 `++` | 不改，不新增次数 |
| step null | SKIPPED | 不改 |
| step result null | SUCCESS | 不改 |
| step stop | catch `TaskStopRequestedException` -> STOPPED | 不改 |
| generic exception | policy 可重试则等待并重跑，否则 FAILED | 不改 |
| retry delay | 精确 `retryPolicy.delayMillis` | 数值/次数不改；只把已有 `context` 传给 Cloud `TaskSleep`，修复当前 null 导致的 typed fail-closed |
| sleep checkpoint | local sleep 前后 stop/pause checkpoint | Cloud sleep 前后 exact revision checkpoint；PAUSED/stale/stop 不继续下一 attempt |
| typed transition | 源无此 Cloud 类型 | 在 generic catch 前原样 rethrow；不消耗 retry，不返回 FAILED，不创建新 action id |

当前 exact-copy Cloud executor 的 `delayBeforeRetry(retryPolicy)` 内调用
`TaskSleep.sleepOrStop(null, positiveDelay, ...)`，而 Cloud `TaskSleep` 明确将 positive wait + null context 分类为
`MISSING_CONTEXT`。因此只新增 `BaseTaskTemplate` 不足以实现用户目标；推荐写集必须包含 executor 的机械适配：

```text
delayBeforeRetry(context, retryPolicy)
TaskSleep.sleepOrStop(context, delayMillis, ...)
catch TaskCheckpointTransitionException -> throw unchanged
```

这不改变 retry 决策、次数、delay、fallback 或日志顺序，只让已批准的 Cloud typed checkpoint 真正接入。

### 5.4 retained action `UNKNOWN/final` 不变量

- Base 和 step executor 不读取 broker/ledger，不 mint/renew action，不把 `UNKNOWN` 解释为 SUCCESS/FAILED，也不自动换 ID。
- generic retry 只能重新进入同一个 step；任何机械调用仍必须携带 trusted persisted action state 给出的同一个 opaque handle。
- `UNKNOWN` 重入只允许相同 requestId+digest+bytes 收敛；未解析 UNKNOWN 不可 renewal；仅 verified
  `NOT_EXECUTED` 可由 package-private trusted owner renewal；任一 non-UNKNOWN final immutable。
- 在 persisted action address/handle owner 尚未落地前，包含 capture/window fact/input 的 concrete Task 不得激活。

## 6. 风险矩阵

| 风险 | 失败形态 | 本设计控制 | 实现/激活门 |
|---|---|---|---|
| exact context/revision | null/default context 或旧 revision 推进 Task | 无参/null typed fail-closed；每个 checkpoint 读 coordinator structured decision | 禁 builder/holder fallback；源码审查无 fake 值 |
| pause 当前 revision | pause 被吞为 FAILED 或继续 retry | `PAUSED` transition 原样上抛，不 after/error、不消耗 retry | host 未来只 park；本切片不实现 host |
| resume stale | 旧 stack/handle 在新 ACTIVE revision 复活 | old context 永久 stale；`ACTIVE_NEWER_REVISION_CONFIRMED` -> rehydration-required unwind | A exact resume-confirm + future rehydration owner 完成前 dormant |
| newer unconfirmed | resume context 在 local executor 未 ready 时运行 | `ACTIVE_NEWER_REVISION_UNCONFIRMED` / current-unconfirmed -> DENIED | A slice 未关闭前不得激活 |
| STOPPED | stop 被映射 FAILED | `TaskStopRequestedException` -> STOPPED + `IDLE/FREE` | coordinator stop 唯一 request authority；`stop()` 禁用 |
| COMPLETED | old stack 把完成当失败或继续动作 | typed COMPLETED unwind，不调用 afterTask | host 丢弃旧 stack，不重启 |
| UNKNOWN action | retry 产生第二 action 或猜测结果 | Base 不解释；typed port + retained handle/final ledger 维持 same-byte redelivery | persisted action owner 未完成前 action Task dormant |
| final action | final 后重放/rollback UNKNOWN | Base 无 ledger capability；final immutable | 不新增 raw port/outcome completion API |
| `GameContext` 跨 run/tenant | singleton default State 被并发共享 | 本切片不装配/激活；未来每 exact run bind 独立 State 并 finally clear | 未有 run-state owner前 host gate=P0 |
| `GameContext` pause/resume | 旧 revision 状态覆盖新 revision | typed transition 不 after；未来 state publication 必须 exact run/revision CAS | durable/rehydration 独立切片 |
| 租户隔离 | 日志/状态混租户 | Base 不存 scope map，不记 tenant/user 明文；只记 exact run/window/revision diagnostics | host 必须按 authenticated scope 构造 authority assembly |
| 容量 | Task state/threads/action 无限增长 | 本写集无 map/queue/thread/storage；executor attempts 是栈内 policy-bounded local int | 复用 coordinator/action ledger quotas；Base 不新增 quota owner |
| 运维 | 无法区分业务失败与 lifecycle unwind | 日志分开记录 `TaskRunResult` 与 typed outcome/disposition/revisions | 不记录 title/geometry/raw image/tenant PII |
| host dormant | Spring scan 意外激活 Task/Input | 不改 `CloudServiceConfiguration`/`CloudServiceHost`/server；Base abstract，无 concrete Task | package 后做 reachability 扫描，确认无 Task bean/caller/thread |
| title/geometry/focus authority | Cloud 使用陈旧标题/坐标或主动抢焦点 | 对应 API 删除；日志不假造；动作只走 retained typed port | 禁 `TaskWindowRuntimeService`/holder/window runtime import |
| retry sleep | null context 让每个 delayed retry fail-closed | executor 把 exact context 传给 `TaskSleep` | 定点源码审查 + Cloud package |

## 7. 完整拟改文件清单与零交集证明

### 7.1 推荐最小写集

Cloud Brain 仅 `1 new + 1 modify`，DHXY Java/Maven/resources/tests 零改：

| 文件 | 类型 | 精确目的 |
|---|---|---|
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\template\BaseTaskTemplate.java` | New | Cloud-native 显式-context lifecycle/step compatibility；删除 local window/context mint authority；typed transition 不降级 |
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\template\TaskStepExecutor.java` | Modify | 仅增加 typed transition passthrough，并把 exact context 传给 retry sleep |
| `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-12-cloud-base-task-template-worker-f.md` | Design report | 当前文件；实施阶段只追加 Implementation 证据，不写其它流程文档 |

明确不改：`GameContext.java`、`TaskExecutionContext.java`、checkpoint/sleep 类型、`GameTask.java`、`TaskStep.java`、
任何 `remote/**`、`remote/run/**`、`host/**`、Cloud config/server/endpoint、concrete Task、Service、DHXY Java。

### 7.2 与并行所有权零交集

| Owner | 其当前写集 | 与 F 推荐 Java 写集 |
|---|---|---|
| A remote lifecycle/resume-confirm | Cloud `remote/**`、`remote/run/**`、endpoint/digest/error；DHXY `cloud/remote/**` registry/ledger/poller/client | **零交集** |
| B artifact/template | `host/CloudArtifactStore`、`ScopedPngArtifactStore`、`CloudTemplateAssets`、`PackagedTemplateAssets`、`CloudServiceConfiguration`（且正在返修写集） | **零交集** |
| E `TaskTeamAssignmentPolicy` | Cloud `com/bot/dhxy/task/startup/TaskTeamAssignmentPolicy.java` | **零交集** |

Worker D 的 `TaskStepExecutor` exact-copy 已完成并获批；F 的定点修改不是并行 ownership 交叉，而是 Base 激活前必需的
Cloud adaptation。实施必须由父级明确批准从“dormant exact-copy”进入“Cloud-safe compatible”状态，并保留 D 的
retry 数量/顺序/delay 业务基线。

## 8. P0/P1/P2 与实施/package 门

### 8.1 Design #1 当前结论

- `P0=0`：未提出 fake context/window authority、host activation 或 action bypass。
- `P1=0`：typed transition swallowing 与 null retry sleep 均已纳入同一最小写集，不留给后续隐式修补。
- `P2=0`：API 去向、direct subclass compatibility、零交集、日志与 dormant gate 已明确。
- Worker F 结论：**DESIGN READY FOR PARENT REVIEW**，不等于 `DESIGN APPROVED`。

### 8.2 实施阻断门

父级批准后，实施必须同时满足：

1. 写集严格为 §7 的 Cloud `1 new + 1 modify` 与本报告追加；发现并行修改即停止，不覆盖。
2. `BaseTaskTemplate` imports/源码中不得出现 `WindowRuntimeContext`、`TaskExecutionContextHolder`、
   `TaskWindowRuntimeService`、window title/geometry/focus/input provider 或 raw `RemoteGameClientPort`。
3. 无参/null context 不能执行、不能 builder、不能查 holder、不能造 retry/start time default。
4. `TaskCheckpointTransitionException` 在 Base 与 executor 两层都不能落入 generic FAILED/retry。
5. STOP 仍精确映射 `TaskRunResult.STOPPED`；PAUSED/stale/completed/denied 不映射任何 `TaskRunResult`。
6. retry attempt、`canRetry`、delay 数值、null-result、step-result 与日志顺序保持源基线；唯一机械差异是 context-aware sleep。
7. 不新增测试，不修改现有 tests；不创建/迁移/激活任何 Task、host、poller、capture/OCR/input。
8. 不修改 A/B/E 文件，不修改 authority assembly/context/checkpoint/sleep/GameContext。

### 8.3 Cloud package 门

实现后、父级 handoff 前必须：

- 从 `D:\mavenProject\dhxy-cloud-brain` 运行 fresh `mvn -q clean package`，不得 skip tests；这是 Cloud 启动路径构建门，
  不是新增测试授权。
- 记录 exit code、现有 suites/failure/error/skipped、JAR SHA-256。
- 静态复核：Cloud `BaseTaskTemplate` 无 local authority import；`TaskStepExecutor` 只有批准的 typed catch/context sleep diff；
  `CloudServiceConfiguration`/host/server/concrete Task 零 diff；main source 无 Base/concrete Task runtime caller。
- 不启动任何应用、Task、host/poller/UI/capture/OCR/input；fresh runtime 不属于本 dormant compatibility slice。

## 9. 父级决策点

请求父级只做二选一：

- `DESIGN APPROVED`：授权按 §7 的 Cloud `1 new + 1 modify` 实施，并执行 §8 package/静态门。
- `BLOCKED`：在本报告追加具体 P0/P1/P2、受影响方法和精确返修条件；Worker F 不自行扩写 Java 范围。

**无已批准业务差异；按基线等价迁移。**

## Parent Implementation Review #1 - APPROVED - 2026-07-12

父级已逐行复审批准写集并亲自运行 fresh Cloud package。结论：**APPROVED，P0=0/P1=0/P2=0**。

- `BaseTaskTemplate.execute(context)` 保持源日志 -> `beforeTask` -> checkpoint -> steps -> after 顺序，没有新增
  before-before gate；空 steps 仍直接 `SKIPPED` 且不调用 after。
- 无参/null context 只产生 typed `MISSING_CONTEXT`，没有 builder/holder/default fallback；两个 `executeStep` 委托前均
  strict resolve。
- `TaskStopRequestedException` 仍落 `STOPPED` + GameContext cleanup；`TaskCheckpointTransitionException` 在 Base 与
  executor 均先于 generic catch 原样上抛，不消耗 retry、不写 `FAILED`、不调用 after。
- `stop()` 精确保留源 `GameContext` 的 `IDLE/FREE` cleanup，不发起 lifecycle transition；本地窗口/focus/title/geometry/
  input authority imports 与 helper 已移除。
- `TaskStepExecutor` 仅在既有 checkStop 位置对 non-null step 强制 explicit context，并把同一 context 传给 retry sleep；
  null step、attempt 计数、canRetry、delay、日志与结果映射保持源基线。
- 父级 fresh `mvn -q clean package` exit 0：4 suites / 21 tests / 0 failures / 0 errors / 0 skipped；shaded JAR
  119,512,571 bytes，SHA-256 `BD3D6B7587F3FFB5B750631BBF51CE0579CB34BF1AABAD001F879B0967ECFE7A`。

本批准只关闭 dormant compatibility layer，不激活 concrete Task/host/cohort，也不代表 durable rehydration 已完成。
**无已批准业务差异；按基线等价迁移。**

## Parent Design Review #1 - DESIGN APPROVED - 2026-07-12

父级已对照 DHXY HEAD `BaseTaskTemplate`、Cloud `GameTask`、`TaskExecutionContext`、
`TaskCheckpoint`/`TaskSleep`/typed transition 与当前 `TaskStepExecutor`。推荐的 Cloud-native compatibility layer、
`1 new + 1 modify` 写集、explicit-context 唯一入口、typed transition passthrough、删除本地窗口/focus/context mint
authority 的方向成立。结论：**DESIGN APPROVED，P0/P1/P2=0**，但以下父级约束覆盖 Design #1 中两处提议并成为
实施的强制版本：

1. **不得新增 beforeTask 之前的 checkpoint。** 源基线的第一处 checkpoint 位于 `beforeTask(context)` 之后；Cloud
   必须保留该位置和调用顺序。`resolveExecutionContext` 只做 non-null exact-context 门，不额外读取窗口/业务事实，
   不把“更安全”变成未经批准的新验证顺序。
2. **`stop()` 保留源 GameContext cleanup 语义，不抛 unsupported/authority exception。** 它只把本 Task 的
   `GameContext` 置 `IDLE/FREE`，不产生 coordinator lifecycle 转换，因此不是第二 stop authority。真正 stop 请求仍只由
   coordinator/未来 host owner 发起；保留本地 cleanup 可避免未来 host finally 路径发生新的异常行为。
3. **所有实际 step 执行必须有 explicit context。** `BaseTaskTemplate.executeStep(...)` 在委托前使用 strict resolver；
   `TaskStepExecutor` 的既有 `checkStop` 位置改为调用 Cloud `TaskCheckpoint.throwIfStopRequested(context, ...)`，使 public
   executor 的 non-null step + null context 也 typed `MISSING_CONTEXT`，不得因 `if (context != null)` 绕过。null step 仍按
   源基线直接 `SKIPPED`，不创造副作用。
4. `TaskCheckpointTransitionException` 必须在 `TaskStepExecutor` 和 `BaseTaskTemplate` 中均先于 generic `Exception`
   原样上抛；不得消耗 retry、写 `FAILED`、调用 `afterTask` 或 mint 新 action。
5. retry 的次数、`canRetry` 顺序、delay 数值、日志顺序保持；唯一批准的机械适配是把当前 exact context 传给 Cloud
   `TaskSleep`，从而使用已批准的 typed lifecycle checkpoint。无参 `execute()` 以 typed `MISSING_CONTEXT` fail-closed，
   不 builder、不 holder fallback。

现授权同一 Worker F 只修改报告 §7 的两个 Cloud Java 文件并追加 Implementation #1；不得扩大到 GameContext、context、
checkpoint、remote、host、concrete Task 或 DHXY Java。完成后 Worker 跑 Cloud `mvn -q clean package`（不 skip），父级再做
源码复审和 fresh package。host/cohort 继续 dormant。**无已批准业务差异；按基线等价迁移。**

## Internal Worker F - Implementation #1 - 2026-07-12

### 实施状态与冻结基线

- 已按 `Parent Design Review #1 - DESIGN APPROVED` 的覆盖约束完成实施；未把 Design #1 中被父级否决的
  before-before checkpoint 或 throwing `stop()` 带入代码。
- 开工时 Cloud `BaseTaskTemplate.java` 不存在；Cloud `TaskStepExecutor.java` 仍为 Worker D 批准的 exact-copy，
  SHA-256 `CB7D963E433798E00F5D2C94DE58CF2887F662E2F7C759656B5C14406B5760DD`。
- 两仓既有 dirty/untracked 全部保留；未 reset/revert/checkout/clean/add/commit/push，未覆盖并行文件。
- 唯一 Java 写集：
  - new `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\template\BaseTaskTemplate.java`
  - modify `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\template\TaskStepExecutor.java`
- 唯一文档写入是本 Implementation #1 追加段。未修改 DHXY Java、Maven、resources、tests、CR 卡或 dashboard。

### `BaseTaskTemplate` 实现证据

- 保留源构造器、字段、`beforeTask/afterTask/buildSteps/getRetryPolicy`、两个 `executeStep`、`namedStep`、
  `sleepSafely`、step-result 转换和 `GameContext` 状态落点。
- `execute(TaskExecutionContext)` 顺序严格为日志 -> `beforeTask(context)` ->
  `context.throwIfStopRequested()` -> build/steps；**没有**在 `beforeTask` 前新增 checkpoint。
- 无参 `execute()` 通过 strict `resolveExecutionContext(null)` 抛
  `TaskCheckpointTransitionException(MISSING_CONTEXT)`；无 builder、holder 或 default context。
- 两个 `executeStep` 最终都在委托 executor 前经过 strict resolver；null context 不进入实际 step。
- `TaskStopRequestedException` 仍映射 `afterTask(STOPPED)` + `TaskRunResult.STOPPED`；
  `TaskCheckpointTransitionException` 在 generic `Exception` 前原样上抛，不调用 `afterTask`、不写 `FAILED`。
- `stop()` 精确保留源 cleanup：非空 `GameContext` 写 `BotStatus.IDLE` 与 `ActionState.FREE`，不抛新异常，
  不发起 coordinator transition。
- 删除本地 `buildExecutionContext`、`activateWindowIfReady` 及其 imports；`logWindowContext` 只记录 approved
  immutable `windowId/role/nativeHandle identity/processId/taskRunId/revision/stopEpoch`，不读取 title/geometry/focus。
- 源码中静态扫描无 `WindowRuntimeContext`、`TaskExecutionContextHolder`、`TaskWindowRuntimeService`、
  `WindowFocus`、`InputProvider`、raw `RemoteGameClientPort`、`buildExecutionContext` 或
  `activateWindowIfReady`。
- 最终源码 SHA-256：
  `DA50F5DAD1BB18F35FD9C2273FB72C92A8510B1D168CD270ABFB213BB210F9A9`。

### `TaskStepExecutor` 定点差异

相对 DHXY HEAD/Worker D exact-copy 只有以下机械适配：

1. import Cloud `TaskCheckpoint` 与 `TaskCheckpointTransitionException`。
2. null step 判断保持最前，仍日志后直接 `SKIPPED`，不做 context/checkpoint 副作用。
3. 实际 non-null step 的既有 `checkStop(context)` 位置改为
   `TaskCheckpoint.throwIfStopRequested(context, "任务步骤检查被中断")`；null context typed
   `MISSING_CONTEXT`，不再由 `if (context != null)` 绕过。
4. `TaskCheckpointTransitionException` catch 位于 generic `Exception` 前并原样上抛，不消耗 retry。
5. `attemptedRetries` 初始化、`canRetry` 检查、先递增再记日志、continue 顺序、max retries、delay 数值、
   step 执行/结果日志均不变；只把 `delayBeforeRetry` 改为接收当前 context，并调用
   `TaskSleep.sleepOrStop(context, delayMillis, ...)`。

最终源码 SHA-256：
`990B6DDACBF249BC9392B9A9682754133279483DF0D4E953DC45CE3BAE257442`。

### 二进制与 dormant 证据

- `javap -classpath target/classes -p` 确认 `BaseTaskTemplate` 为 abstract；public API 为两个 `execute` 与
  cleanup `stop`，protected API 包含 explicit-context lifecycle/step helpers；不存在
  `buildExecutionContext` 或 `activateWindowIfReady`。
- `TaskStepExecutor` 二进制 private `delayBeforeRetry` 参数为
  `(TaskExecutionContext, TaskRetryPolicy)`，两个 public execute 签名不变。
- Cloud main source 扫描无 concrete `extends BaseTaskTemplate`、构造点或 runtime caller；未修改
  `CloudServiceConfiguration`、host/server/endpoint、GameContext、context、checkpoint、remote、concrete Task。
- 未启动任何应用、host、Task、poller、UI、capture、OCR 或 input；未执行 fresh runtime。

### Fresh Cloud package 门

从 `D:\mavenProject\dhxy-cloud-brain` 执行：

```text
mvn -q clean package
```

- exit code：`0`
- wall time：`78.7s`
- Surefire：`4 suites / 21 tests / 0 failures / 0 errors / 0 skipped`
- shaded JAR：`target/dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`
  - bytes：`119512571`
  - SHA-256：`64FC30D483C3858C14E049684341D9821C763D6008497589A164FAA942DE6F31`
- original JAR：`target/original-dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`
  - bytes：`2643167`
  - SHA-256：`A0837D57A2D06740D75AB5A6DA446A8E2EF909978B6DF5BDFB6D09E420D57D74`
- 本轮未新增、修改或删除测试；package 仅运行仓库既有 suite。

### Worker 交付结论

批准的 Cloud `1 new + 1 modify` 已实施并通过 worker fresh package。当前材料等待父级源码复审；Worker F
不作为 reviewer，不宣称父级 Implementation APPROVED。host/cohort 继续 dormant。

**无已批准业务差异；按基线等价迁移。**
