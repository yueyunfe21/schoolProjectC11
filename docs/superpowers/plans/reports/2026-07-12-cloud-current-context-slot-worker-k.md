# Worker K：Cloud current task-run context slot

## Parent Task Brief #1 - 2026-07-12

### 目标与阶段

为 AutoCombat W0 实现前设计 non-mintable `CloudTaskRunCurrentContextSlot`：同一 taskRun 的 Service/runtime state 跨
pause/resume 保留，但每次业务调用只取得 exact current confirmed-ACTIVE revision context。该方向已在
`2026-07-12-cloud-auto-combat-service-worker-a.md` 的 Parent Design Review #3 获原则批准；本轮先补 exact capability、
线程安全与文件闭包。首轮只追加 `Internal Worker K - Design #1`，父级 DESIGN APPROVED 前零 Java。

### 必读与基线

- `D:\mavenProject\DHXY\AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、迁移矩阵。
- A 固定日志最新 Parent Design Review #3 与 PAUSED observer Parent W0 Design Review #1；本 K 不处理 PAUSED observer wire。
- Cloud `TaskExecutionContext`、`CloudTaskRunExecutionContext`、`CloudTaskServiceExecutionContext`、
  `CloudTaskRunExecutionGate`、`CloudTaskRunAuthorityAssembly`、`RemoteTaskRunCoordinator/Binding/Status`、H State owner。

### 设计不变量

1. 一 slot 固定一个 stable full key：scope 四元组 + taskRunId + taskType + window 四元组 + non-terminal stopEpoch。constructor/
   mint 非 public，只能由同一 authority assembly 的 activation owner 在 current confirmed ACTIVE 后创建。
2. public 业务面最多 `TaskExecutionContext current()`；每次 current 都必须在返回前执行一次 existing typed current-confirmed
   ACTIVE gate。PAUSED/stale/newer-unconfirmed/terminal 都 typed unwind，绝不回旧 context、null 或 fallback。
3. package-private `install(next)` 只接受同 stable full key、revision 严格前进、exact current confirmed ACTIVE 的 next；CAS 前
   一次 typed gate，旧/等 revision、identity drift、并发 loser 显式拒绝，无内部 retry。成功后旧 context 永久不可由 slot 返回。
4. package-private terminal close 只能凭 coordinator exact current STOPPED/COMPLETED binding；PAUSED、stale unwind、ordinary
   denied 不得 close。close 幂等且永久拒绝 current/install；不得接受 raw reason/string/status boolean 作为 authority。
5. slot 不保存/复制业务 state，不创建 ThreadLocal/holder/static current/map/TTL/takeover/线程/poller；只管理一个原子 current
   capability。restart 无恢复，必须由 activation 流重新建立。
6. 写集不得触碰 A PAUSED observer 双仓文件、B artifact 七文件、J turn 文件、I properties、DHXY Java/tests。优先 Cloud
   1 New + assembly 最小 Modify；若无需 assembly 修改即可真实限制 mint，需给源码证据，不能只靠注释。

### Design #1 必交付

- 当前 context/gate/assembly 可见性与 mint 调用图；推荐 public/package-private API 的精确签名。
- stable key 字段、initial/current/install/terminal close 的线性化点与 race 矩阵：并发 install、pause 与 current、resume install、
  stop/complete 与 install、close 重试、错 scope/window/revision。
- typed exception/outcome 复用方案，不解析 reason 文本；不得改变 coordinator lifecycle/revision/confirm 语义。
- exact Cloud 文件表、future AutoCombat/activation 构造图、容量/运维/restart 与 dormant 门。
- P0/P1/P2、自审、批准后实施/package 门。自审不算批准。

### Worker 规则

- 你是实现 Worker，不是 reviewer；只向本报告 append Design #1，批准前不改 Java/Maven/resources/tests。
- 你不是独自在仓库中；保护全部 dirty/untracked，不回滚/覆盖/提交，不修改其它 Worker 日志。
- 不运行 Maven/测试，不启动 application/server/host/Task/poller/UI/capture/OCR/input。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker K - Design #1 - 2026-07-12

本轮只做设计。已完整读取本报告 Parent Task Brief #1，并核对 `AGENTS.md`、完整 `docs/DHXY_CONTEXT.md`、
`docs/ACTIVE_WORK.md` 顶部 CR271、迁移矩阵 `TaskExecutionContext`/`TaskTurnCoordinator`/`AutoCombatService` 条目、
`docs/业务逻辑.md` 的 standalone/队伍边界、Expected 战斗快脱战与黄袍战后短窗口规则。A 固定日志已读到最新
`External Worker A - W0 Design Repair #1`；本设计只承接已批准的 `Parent Design Review #3` current-slot 约束，
不处理仍待父级复审的 PAUSED observer wire/identity 文件。

### 0. 基线与只读证据

- DHXY：branch=`thin-client-design`，HEAD=`0114604e1ff5f15491d2910959c45252e893d04f`，无 upstream；工作区大量
  dirty/untracked，含 dirty `AutoCombatService.java`。本 K 未采信、覆盖或回滚这些差异。
- Cloud：branch=`navigation-migration`，HEAD=`3b988caa010254973e03342272e6d1d6a9685b01`；当前 `remote/**` 与
  `com/bot/**` 迁移源码均为受保护 untracked，`git ls-tree HEAD` 对本设计核对的 context/gate/assembly/H owner 路径为空。
  因此 latest pushed Cloud 只能证明“目标路径不存在”；当前设计基线取 CR271 已批准的落盘源码与 A Review #3，不把它们
  误称为 pushed Java。
- 本轮核对时关键落盘 SHA-256：`CloudTaskRunAuthorityAssembly=76DADD9F...1077C`、
  `CloudTaskRunExecutionContext=17A01BA3...54C1`、`CloudTaskServiceExecutionContext=CF9C98E0...EC7F`、
  `CloudTaskRunExecutionGate=8BCF0125...D715`、`CloudGameContextStateOwner=8D5BBEFA...DBF`、
  `TaskExecutionContext=6B6A6810...20CE`。这些只是并行保护锚，不是提交/批准证据。
- 业务基线核对结论：slot 只移动 current context ownership，不改变 expected/fast-exit、战后补给、phase、fallback、
  retry、验证次数、输入顺序或 expiry。**无已批准业务差异；按基线等价迁移。**

### 1. 当前可见性与 mint 调用图

| 类型/入口 | 当前可见性 | 能力结论 |
|---|---|---|
| `CloudTaskRunExecutionContext` | `public final`；constructor `private`；`snapshotOf(binding)` package-private | 只能由同包 gate 从 binding 快照，不是 public mint 面 |
| `CloudTaskRunExecutionGate` | 顶层 package-private `final`；constructor/create/validate/classify 均 package-private | `createContext` 先 `find + authorize`，只 mint exact current confirmed ACTIVE；`validate/classifyCheckpoint` 是现有 typed gate |
| `CloudTaskServiceExecutionContext` | `public final`；constructor package-private；`runContext()`/retained state package-private | public 只能读 exact identity/metadata/typed port，不能自造 delegate |
| `TaskExecutionContext` | `public final`；wrapper constructor public，但参数 `CloudTaskServiceExecutionContext` 不可 public 构造 | public constructor 只能重包已有 capability；`throwIfStopRequested()` 已把 typed checkpoint 映射为 continue/STOP/transition unwind |
| `CloudTaskRunAuthorityAssembly` | 顶层 package-private `final`；constructor private；同 coordinator 进程内只允许一个 assembly | 当前唯一真实 mint 路径；`createTaskServiceRuntime` 同时组装 run context、service context、retained state、typed port |
| `CloudGameContextStateOwner` | package-private；initial/resume/release 均 package-private | 已批准先例：full stable key、单次最终 typed gate、exact terminal binding revalidate、STOP stopEpoch+1/COMPLETED stopEpoch 不变 |
| coordinator/binding/status | coordinator typed `find/authorize/classifyCheckpoint`；binding/status 为 immutable record/enum | terminal 判断可直接用 enum/binding equality，不需要解析 `reason` 文本 |

当前调用图：

```text
CloudTaskRunAuthorityAssembly.create(...)
  -> one coordinator + one ledger + one CloudTaskRunExecutionGate
  -> createTaskServiceRuntime(scope, taskRunId, metadata)
     -> executionGate.createContext(...)
        -> coordinator.find + authorize(current ACTIVE + confirmed revision)
        -> CloudTaskRunExecutionContext.snapshotOf(binding)
     -> new CloudTaskServiceExecutionContext(...)
     -> new TaskExecutionContext(non-mintable delegate)
     -> TaskServiceRuntime(TaskExecutionContext, retainedActionState)
```

`rg` 确认当前 main/test 没有 assembly/runtime 外部 caller，host/Task producer 仍 dormant。推荐在 assembly 内增加一个组合
activation 工厂，使 initial slot 与 initial `TaskServiceRuntime` 必然来自同一 assembly；不能只在任意业务 bean 里 `new slot`。

### 2. 方案比较与结论

1. **推荐：单个 `AtomicReference` slot + assembly-minted runtime capability。** Service/runtime state 整个 taskRun 保留；
   每次 `current()` typed gate；resume 只 CAS 安装新 revision；terminal exact binding 原子关闭。文件闭包最小、无 holder/map。
2. **不选：每 revision 重建 Service，再把 `AutoCombatRuntimeState` 放入第二 retained owner。** 会新增状态 owner、字段搬运和
   cleanup 合同，且容易与 H `GameContext.State` 混权；超过本 W0，A Review #3 已选 slot。
3. **禁止：ThreadLocal/static current/按 run Map/TTL takeover。** 会形成隐藏 ambient authority、恢复/清理新语义和容量 owner，
   直接违反 Parent Brief。

### 3. 推荐 API 与 same-assembly mint

新类型只有一个 public 业务方法；其余构造/推进/关闭均留在 `com.yueyunfe.dhxy.cloudbrain.remote` 包内：

```java
public final class CloudTaskRunCurrentContextSlot {
    CloudTaskRunCurrentContextSlot(
            CloudTaskRunAuthorityAssembly.TaskServiceRuntime initialRuntime);

    public TaskExecutionContext current();

    void install(CloudTaskRunAuthorityAssembly.TaskServiceRuntime nextRuntime);

    TerminalCloseResult closeTerminal(RemoteTaskRunBinding exactTerminalBinding);

    enum TerminalCloseResult {
        CLOSED,
        ALREADY_CLOSED
    }
}
```

assembly 最小增强的精确 package-private 面：

```java
CurrentContextSlotActivation createCurrentContextSlotActivation(
        RemoteTaskRunScope scope,
        String taskRunId,
        CloudTaskServiceMetadata metadata);

record CurrentContextSlotActivation(
        TaskServiceRuntime taskServiceRuntime,
        CloudTaskRunCurrentContextSlot currentContextSlot) {}
```

为“同一 assembly”做源码级约束，而非只写注释：

- assembly 新增一个私有 `AuthorityInstanceIdentity` 实例，内部绑定该 assembly 的 coordinator；其 constructor 只归 enclosing
  assembly，外部/业务代码不可 mint。
- package-private `TaskServiceRuntime` 增加该 opaque identity 组件；现有 `createTaskServiceRuntime` 是唯一生产者。
- slot constructor 从 `initialRuntime` 固定保存 identity/coordinator；`install(nextRuntime)` 首先要求 identity **对象同一**。
  因此其它 coordinator/assembly 的 runtime 即使复制 scope/run/window 文本也不能安装。
- `createCurrentContextSlotActivation` 在一个 assembly 调用内先造 initial runtime，再造 slot 并成对返回给 future trusted
  activation owner。AutoCombat 只拿 slot，不拿 identity、ledger、raw run context 或 retained mint 面。
- Java package 是现有 authority TCB；新增 API 不 public。`TaskExecutionContext` 的 public wrapper constructor 不削弱此边界，
  因为其 delegate 仍无 public constructor，且 install 接受带 assembly identity 的 `TaskServiceRuntime`，不接受裸 context。

### 4. Stable full key 与内部状态

slot constructor 从 initial context 一次固定以下 `StableRunKey`，后续永不改：

```text
RemoteTaskRunScope(tenantId, userId, deviceId, clientSessionId)
+ taskRunId
+ taskType
+ RemoteTaskRunWindow(windowId, nativeHandle, processId, playerIdentityEpoch)
+ nonTerminalStopEpoch
```

不把 role、requestedTaskCode、team metadata、phase/action、timer 或业务 State 塞入 key；这些不是本 slot 的 authority。
内部只持有：assembly identity、coordinator、stable key，以及
`AtomicReference<SlotState>`；`ActiveContext` 只含 current `TaskExecutionContext`，`ClosedContext` 只含 exact terminal
binding。private record/sealed helper 全放文件底部。无 state copy、map、ThreadLocal、static current、TTL、线程或 poller。

initial 只允许 first confirmed ACTIVE activation（当前 coordinator 首个 ACTIVE revision=`1`，与 H `activateInitial` 一致）；
constructor 在发布 reference 前对 initial context 执行一次 `throwIfStopRequested()`。这禁止丢失 slot 后在 resumed revision
偷偷重建，也不改变 coordinator revision；restart 必须走 replacement stop + 新 run activation。

### 5. 线性化点与逐方法合同

#### initial

1. 校验 non-null、same-assembly runtime、revision=1，抽取 stable key。
2. **一次** `initialContext.throwIfStopRequested()`；只有 `CURRENT_ACTIVE_CONFIRMED` 继续。
3. constructor 完成前把 `ActiveContext(initial)` 写入 `AtomicReference`；对象由 assembly activation bundle 发布即为
   initial 线性化点。失败不产生 slot。

#### `current()`

1. 读取一次 state；closed 直接按保存的 STOPPED/COMPLETED typed outcome unwind。
2. active context 执行**一次**现有 `throwIfStopRequested()` typed current-confirmed ACTIVE gate。
3. 用 `state.compareAndSet(observed, observed)` 做无写 CAS 线性化/防换代检查；失败不重读重试：closed 则 typed terminal
   unwind，active changed 则显式 `ConcurrentModificationException`。
4. CAS 成功后返回该 context。任何在 install 后才线性化的 `current()` 都不能返回旧 context；与 install 重叠但先完成
   无写 CAS 的调用只可能线性化在 install 之前。绝不返回 null/fallback。

#### `install(nextRuntime)`

1. 校验 assembly identity 对象同一，next stable key 与固定 key 全等；错 scope/taskRun/taskType/window/stopEpoch 立即
   `IllegalArgumentException`，不探测或泄露其它 scope。
2. 读取 observed；closed 按保存 terminal typed unwind。要求 `next.revision > observed.revision`，旧/等 revision
   `IllegalStateException`。
3. CAS 前对 next context 执行**一次** `throwIfStopRequested()`；PAUSED、stale、future/unconfirmed、STOPPED、COMPLETED、
   denied 均沿现有 `TaskStopRequestedException` / `TaskCheckpointTransitionException` unwind。
4. `compareAndSet(observed, ActiveContext(next))` 是 install 线性化点。失败无内部 retry：closed 则 terminal unwind；其它
   install winner 则 `ConcurrentModificationException`。成功后 reference 不再含旧 context。

#### `closeTerminal(exactTerminalBinding)`

1. 只接受 typed `RemoteTaskRunBinding`，不接受 reason/String/status boolean。校验 scope/taskRun/taskType/window 与 stable key
   exact，且 terminal revision 严格大于最后安装 revision。
2. `STOPPED` 必须 `stopEpoch == nonTerminalStopEpoch + 1`；`COMPLETED` 必须 stopEpoch 不变；其它 enum 状态拒绝。
3. 调 `coordinator.find(scope, taskRunId)` 并要求返回值与传入 binding **record equality**；这复用 H owner 已批准的 exact
   terminal evidence 形状，不改 coordinator。terminal 状态不可恢复，因此此 read 后 evidence 稳定。
4. `state.getAndSet(ClosedContext(exactTerminalBinding))` 是 close 线性化点并具有 terminal-wins 语义：无论迟到 install 在它
   前后 CAS，最终都是 closed。首次返回 `CLOSED`；已保存同一 exact binding 的并发/重复 close 返回
   `ALREADY_CLOSED`。不同 binding 拒绝。close 不等待 active stack、不清 Service/runtime state；future activation owner 在
   slot closed 后再按 H owner 的 execution lock/release 合同释放对象。

### 6. Race 矩阵

| 竞态 | 允许的线性化结果 | 必须拒绝/保持 |
|---|---|---|
| 两个 concurrent install | 仅一个 CAS winner；另一个 `ConcurrentModificationException`，无内部 retry | 旧/等 revision、identity drift 永不安装；activation owner 如需继续只能显式发起新调用 |
| pause vs `current()` | pause 先于 typed gate：`PAUSED` unwind；无写 CAS 先于 pause：该调用线性化在 pause 前 | pause 后新调用不得拿旧 context；机械 port 仍有发送前 gate |
| resume install vs old `current()` | old gate 看到 newer revision 则 typed stale；若 old 无写 CAS 先完成，则 current 在线性化上先于 install | install CAS 后才线性化的 current 只能见 next 或显式 concurrent reject |
| stop/complete vs install | terminal 先于 next gate：typed terminal reject；next gate 先通过但 terminal 随后发生：install 即使短暂 CAS 成功，current 立即 terminal unwind，close 的 getAndSet 最终获胜 | terminal 永不因迟到 install 复活；无 fallback/自动换新 |
| close vs current | close 先：保存 terminal typed unwind；current 无写 CAS 先：该调用线性化在 close 前 | close 后新调用永久拒绝 |
| 并发/重复 close | exact same terminal binding：一个 `CLOSED`，其余 `ALREADY_CLOSED` | 不同/非 current/非 terminal binding 不得改变 state |
| 错 scope/task/window/stopEpoch | install 本地 stable-key 拒绝；close full key + coordinator exact equality 拒绝 | 不解析 authorization reason，不跨 scope 返回 binding |
| 旧/等/future/unconfirmed revision | 旧/等在 monotonic check 拒绝；future/unconfirmed 在 typed gate unwind | 不内部 retry，不把 negative signal 变成业务 truth |

### 7. typed exception/outcome 复用

- `current/initial/install` 的 lifecycle 分类只调用 `TaskExecutionContext.throwIfStopRequested()`，底层复用
  `TaskCheckpointDecision/TaskCheckpointOutcome`；STOP 用现有 `TaskStopRequestedException`，其它 transition 用现有
  `TaskCheckpointTransitionException`。不调用 `RemoteTaskRunAuthorization.reason()` 做分支。
- closed slot 根据保存 binding 的 enum 直接重放同一 typed terminal unwind：STOPPED -> `TaskStopRequestedException`；
  COMPLETED -> `TaskCheckpointTransitionException(TaskCheckpointOutcome.COMPLETED)`。
- stable-key/revision/authority token 错误是 capability programming error，使用 `IllegalArgumentException/IllegalStateException`；
  CAS loser 使用 `ConcurrentModificationException`。这些不转译为 Task FAILED/SUCCESS，也不触发 retry。
- close 使用 package-private `TerminalCloseResult`，形状对齐 H 的 `RELEASED/ALREADY_RELEASED`，但不复用 H 状态或修改 H。

### 8. Exact 文件闭包与禁碰表

批准后的 Cloud 写集固定为 **1 New + 1 Modify**：

| 动作 | 文件 | 内容 |
|---|---|---|
| New | `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskRunCurrentContextSlot.java` | public final slot、AtomicReference state、stable key、typed current/install/exact terminal close |
| Modify | `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\CloudTaskRunAuthorityAssembly.java` | private assembly identity、`TaskServiceRuntime` identity、combined slot activation factory/record |

零 DHXY 文件；零 Maven/resources/tests。明确不触碰 A PAUSED observer 双仓文件、B artifact 七文件、J
`CloudTaskTurnCoordination`、I `CloudAutoBattleProperties`、H `CloudGameContextStateOwner`、coordinator/binding/status、
execution gate/context/service context、host/server/endpoint/poller。若实现时发现必须扩大此表，立即停止并回本报告写 conflict，
不得顺手扩写集。

### 9. Future AutoCombat / activation 构造图

```text
initial confirmed ACTIVE revision 1
  -> assembly.createCurrentContextSlotActivation(...)
     -> initial TaskServiceRuntime + slot (same assembly identity)
  -> H.activateInitial(initialContext)
  -> construct one per-taskRun AutoCombatService(..., slot, one AutoCombatRuntimeState)
  -> publish runnable only after all three are ready

each business invocation
  -> slot.current() [one typed ACTIVE gate]
  -> H.callWithState(handle, same exact context, business stack)
  -> collaborator retained port performs its own pre-mechanical revalidation

pause
  -> old stack typed unwind
  -> retain slot + AutoCombatService/runtime state + H State; no install, no close

resume
  -> reconcile-confirm produces exact newer confirmed ACTIVE binding
  -> same assembly createTaskServiceRuntime(...) for new revision
  -> under future per-run activation lock: H.activateResumed -> slot.install(nextRuntime)
     -> swap retained action state -> publish runnable

STOPPED/COMPLETED
  -> stop new scheduling
  -> slot.closeTerminal(exact coordinator binding)
  -> H.releaseTerminal under its execution lock
  -> release per-run Service/runtime container; tenant-host RefreshDuePanelVerifyGate remains untouched
```

slot 不承担 cross-owner atomicity；future activation owner 必须用一个 per-run activation lock 完成 H handle、slot、retained state 的
组合发布，发布前 caller 不可达。该锁/owner 属后续 activation wire，不在本两文件实现，也不得由 slot 新建线程/holder。

### 10. 容量、运维、restart 与 dormant 门

- 每个已激活 non-terminal run 一个 slot，O(1)：一个 stable key、一个 assembly identity/coordinator 引用、一个 current
  context 或 terminal binding。无全局/tenant Map；数量受 coordinator 与 H 已有 global=10,000、owner retained=1,000、
  owner non-terminal=64 admission 上限间接约束，不新增第二套 quota/eviction。
- close 不删除业务 state；activation owner 释放 per-run object 后 slot 可 GC。没有 TTL、sweeper、takeover、后台 cleanup；
  若 owner 泄漏，沿既有 retained-run/host 运维告警处理，不让 slot 自己发明回收业务。
- restart 不序列化/恢复 slot。replacement session 不能 takeover 旧 binding；旧 run 走既有 fail-closed STOP，新 run revision 1
  由新 assembly activation 重建。不得从 coordinator retained binding 自动恢复 Service/runtime state。
- dormant：只新增 package type 与 assembly package-private factory；不加 Spring annotation/bean，不改
  `CloudBrainServer/CloudServiceHost`，无 caller/producer，不执行 Task、port、capture/input。仅构造 assembly/slot 也没有机械副作用。
- 运维诊断沿用 lifecycle/activation 日志；slot 本体不加高频日志和 reason 解析。future activation owner 可记录 scope 前缀、
  taskRunId 前缀、windowId、old/new revision 与 close status，但不得记录 HWND/raw identity secret 或改变分支。

### 11. P0/P1/P2、自审与批准后门

- P0=0。
- P1=0。same-assembly provenance 由 opaque assembly identity + internal runtime 强制；每次 current/install 有一次现有 typed
  ACTIVE gate；terminal close 使用 exact current binding，terminal 原子覆盖迟到 install。
- P2=0。并发 install loser、overlap current 的线性化、STOP stopEpoch+1、COMPLETED stopEpoch 不变、close retry、restart
  fail-closed 均已明确；不需要 coordinator/gate/H 修改。
- 自审检查：无占位符；public 面只有 `current()`；没有 reason 文本解析、null/fallback、内部 retry、map/TTL/thread/
  holder、PAUSED close 或业务 state copy；exact 文件表可由当前源码编译闭合。**本自审不构成批准。**
- 父级写入 `DESIGN APPROVED` 前继续零 Java/Maven/resources/tests。批准后 Worker K 只实施上述 Cloud 1 New + 1 Modify；
  不新增/恢复测试，不启动任何运行面；实施完成后按 CR271 Cloud Java 门执行 fresh `mvn -q clean package`，回写源码差异、
  可见性/caller/dormant 证据与构建结果。若父级改变 package 门，以父级最新明确指令为准。

Design #1 到此停止，等待父级 `DESIGN APPROVED` / `BLOCKED`。

## Parent Design Review #1 - BLOCKED - 2026-07-12

stable full key、same-assembly opaque identity、monotonic install、exact terminal close 与 `AtomicReference` 线性化方向成立。
但 Design #1 把每个 resume revision 的新 `TaskServiceRuntime` 连同一份新的 retained action state 放进 slot，破坏已冻结的
same-taskRun stable action identity/UNKNOWN same-bytes 续接合同。结论：**BLOCKED，P0=0/P1=1/P2=1**。

### P1-1：resume `install(nextRuntime)` 会切到新的 retained state，旧稳定 action handle 无法再由新 context/port 使用

- 证据：当前 `CloudTaskRunAuthorityAssembly.createTaskServiceRuntime` 每次都 new
  `CloudTaskServiceExecutionContext`；其 constructor 又每次 new `CloudTaskRetainedActionState` 和
  `CloudTaskServicePort`。`CloudTaskRetainedActionState` 文档/字段明确是“一 taskRun retained”，records map 应跨 revision 保留；
  `invoke/retain/renew` 又以 owner state、exact context 和 handle reference 校验。Design #1 的 slot 只保存/返回新 runtime 的
  `TaskExecutionContext`，future 图却写“swap retained action state”。这会让旧 handle 对新 port 报 owner/context mismatch，或迫使
  caller 重新声明同一业务动作。
- 影响：pause/resume 后未决 UNKNOWN、已绑定 request bytes 与稳定 requestId/actionId 无法在同一 retained state 上继续；重新
  `retain` 既可能被 ledger 的旧 context 拒绝，也可能诱使上层错误 mint/renew，直接破坏机械幂等与恢复安全。
- 返修条件：Design Repair #1 必须让一个 taskRun 的 `CloudTaskRetainedActionState` 跨 slot 全生命周期保持同一对象，并让每个
  新 revision 的 `CloudTaskServicePort`/`TaskExecutionContext` 绑定该同一 state 与新的 exact run context。给出可编译写集与 API：
  可对 `CloudTaskServiceExecutionContext` 增加 package-private existing-state constructor/factory，并由 assembly 仅在 initial
  创建 state、resume runtime 复用；或提出同等强类型方案。禁止 public rebind、raw state、ThreadLocal、重新声明业务 key、
  自动 renewal。slot 的 active state 如需保存 runtime，必须保证 `current()` 返回的 context/port 与 retained state 是同一 revision
  的组合快照；install CAS 不得只换 context 而漏换 matching facade。

### P2-1：`current()` 的 self-CAS 不能被描述为 coordinator lifecycle lease

- 证据：`compareAndSet(observed, observed)` 只能证明 slot reference 未被并发 install/close；typed gate 之后 coordinator 仍可能
  pause/stop，因此它不能保证“返回瞬间 lifecycle 永远 current”。现有安全合同依赖后续 Service/port 在副作用前再次 gate。
- 影响：若把 self-CAS 当 current authority lease，future caller 可能省略真正的 pre-mechanical revalidation。
- 返修条件：Repair #1 明确 self-CAS 仅是 slot-generation 线性化；`current()` 语义是“对 captured revision 完成一次 typed gate
  且 slot generation 未变”，不是跨 coordinator transition 的 lease。每次业务/机械使用仍按既有 checkpoint/port fence；不得
  因 slot 存在删任何现有 gate。可用 generation-stamped immutable state 替代 self-CAS，但不增加 retry。

Worker K 只追加 `Internal Worker K - Design Repair #1`，完整重列 expanded exact 文件表、initial/resume runtime 构造图与
retained-state reference invariants；父级 DESIGN APPROVED 前继续零 Java/Maven/resources/tests。J 已获设计批准但明确等待 K
Implementation APPROVED，不会抢写。**无已批准业务差异；按基线等价迁移。**

## Internal Worker K - Design Repair #1 - 2026-07-12

针对 `Parent Design Review #1 - BLOCKED` 的 P1=1/P2=1 定点返修。Design #1 已通过的 full stable key、same-assembly
opaque identity、monotonic install、exact terminal close、无 ThreadLocal/map/TTL/retry 约束全部保留；本 Repair 只修
same-taskRun retained action state/port 续接与 self-CAS 语义。仅设计，零 Java/Maven/resources/tests。

### 1. 当前源码事实与根因确认

父级证据成立，且实际闭包比“existing-state constructor”多两层：

```text
CloudTaskRunAuthorityAssembly.createTaskServiceRuntime
  -> new CloudTaskServiceExecutionContext(runContext, gate, port, ledger, metadata)
     -> new CloudTaskRetainedActionState(runContext, gate, ledger)
     -> new CloudTaskServicePort(runContext, newState, commandPort)
```

- `CloudTaskRetainedActionState.records` 文档和 key 均是一 taskRun retained owner，但当前 constructor 每 revision 新建对象。
- `ActionHandle` 同时保存 owner state、record、exact old context、ledger identity；`invoke` 要求 owner/record/context 都引用相等。
- ledger `RetainedActionIdentity` 也保存 exact context；`requireOwnedCurrent` 要求 identity.context 与 request-build context 引用相等。
- 已绑定 attempt 的 immutable request bytes 含旧 `runRevision`。resume 后不能用新 context 重建同 requestId，因为 digest 必然变化；
  但 broker 对已存在的相同 `requestId+digest` **先走 retained-request 分支，再做新请求 authorization**，因此 exact old bytes
  可安全读取 late resolution/原 UNKNOWN，且不会重新 dispatch。
- 未绑定 attempt 没有 request bytes/outcome，可以在不改变 requestId/actionId/captureId/attempt 的前提下，把其内部 exact
  context 指针推进到新 revision，再完成第一次 build；这不是 renewal 或重新声明业务 key。

所以只复用 state constructor 仍不可交付；必须同时让 state/handle/ledger/executor 区分“bound exact redelivery”和
“unbound same-identity first bind”。

### 2. Retained-state reference invariants

以下引用不变量在一个 taskRun 全生命周期固定：

1. slot initial runtime、所有 resume runtime 的 `retainedActionState` 必须 `==` 同一对象；install 用 reference equality 强制，
   不接受仅 taskRunId 相等的新 state。
2. 该 state 的 `records`、每个 `ActionRecord` 以及未 renewal 的 public opaque `ActionHandle` 跨 revision 保持同一对象；resume
   不重建 map、不重新 `retain(address)` mint key、不清 UNKNOWN/outcome/bound bytes。
3. 每个 revision 新建一个 exact `CloudTaskRunExecutionContext`、一个 `CloudTaskServiceExecutionContext`、一个
   `TaskExecutionContext` 和一个 `CloudTaskServicePort`；新 port 固定 `(new exact context, same retained state, same command
   executor)`，它们作为一个 immutable `TaskServiceRuntime` 被 slot CAS 整体安装，绝不只换裸 context。
4. **bound attempt**：ledger identity.context 与 bound request 均保持原 revision；requestId/actionId/captureId/attempt/digest/
   bytes 全不变。新 port 调同一 action 时只允许 exact payload/timeout 相等，并把 retained old request 交 broker 去重读取结果。
5. **unbound attempt**：仍无 request/outcome 时，ledger 可在第一次新 revision 调用内把同一个 identity 对象的内部 context
   从旧 revision 推进到 exact new context；全部 wire IDs 与 attempt 不变，随后走正常 gate/build/bind。context 只能严格前进，
   stable full key 必须相同。
6. **verified NOT_EXECUTED renewal**：继续只走现有 `renewAfterNotExecuted`，显式产生下一 attempt；UNKNOWN、未记录、
   EXECUTED/OBSERVED/STOPPED 均不能 renewal。本 Repair 不新增自动 renewal。
7. slot 不保存/复制业务 action state；它只保存含同一 state 引用的 current runtime generation。retained records 的唯一 owner
   仍是 `CloudTaskRetainedActionState`，wire identity/bytes/outcome 的唯一 owner 仍是共享 ledger。

### 3. 可编译 API 设计

#### 3.1 `CloudTaskServiceExecutionContext` existing-state constructor

保留当前 initial constructor（只在 first ACTIVE 创建 state），新增 package-private overload：

```java
CloudTaskServiceExecutionContext(
        CloudTaskRunExecutionContext runContext,
        CloudTaskRunExecutionGate executionGate,
        RemoteGameClientPort commandPort,
        CloudTaskRunActionLedger actionLedger,
        CloudTaskRetainedActionState existingRetainedActionState,
        CloudTaskServiceMetadata metadata);
```

overload 不 clone/rebind state；调用
`existingRetainedActionState.requireReusableBy(runContext, executionGate, actionLedger)`，要求 gate/ledger reference 同一、
stable full key 相同。随后创建 `new CloudTaskServicePort(runContext, existingState, commandPort)`。constructor 与 state 参数均
package-private；没有 public existing-state/rebind/raw getter。

#### 3.2 assembly initial/resume runtime API

Design #1 的 opaque `AuthorityInstanceIdentity` 与 runtime component 保留。精确 package-private 面：

```java
TaskServiceRuntime createTaskServiceRuntime(
        RemoteTaskRunScope scope,
        String taskRunId,
        CloudTaskServiceMetadata metadata); // initial only, new retained state

TaskServiceRuntime createResumedTaskServiceRuntime(
        TaskServiceRuntime previousCurrentRuntime,
        RemoteTaskRunScope scope,
        String taskRunId,
        CloudTaskServiceMetadata metadata); // same retained state

CurrentContextSlotActivation createCurrentContextSlotActivation(
        RemoteTaskRunScope scope,
        String taskRunId,
        CloudTaskServiceMetadata metadata);

record TaskServiceRuntime(
        AuthorityInstanceIdentity authorityIdentity,
        TaskExecutionContext taskExecutionContext,
        CloudTaskRetainedActionState retainedActionState) {}
```

resume factory 要求 previous runtime 的 authority identity 与 assembly 对象同一；先由现有 gate mint new exact confirmed-ACTIVE
run context，再用 existing-state overload 构造 matching service context/port。它不修改 state、不安装 slot、不 renewal action；
因此 concurrent install loser 的预构造 runtime 可直接丢弃，没有半迁移 state。

#### 3.3 slot API/active snapshot 修正

public/package-private 签名保持 Design #1，仅 `install` 的对象含义明确为完整 runtime：

```java
public TaskExecutionContext current();

void install(CloudTaskRunAuthorityAssembly.TaskServiceRuntime nextRuntime);

TerminalCloseResult closeTerminal(RemoteTaskRunBinding exactTerminalBinding);
```

private active state 为 generation-stamped immutable snapshot：

```java
private record ActiveRuntime(
        long slotGeneration,
        CloudTaskRunAuthorityAssembly.TaskServiceRuntime runtime) implements SlotState {}
```

install 在 typed gate 前要求：same assembly identity、same stable full key、strict newer revision，且
`nextRuntime.retainedActionState() == observed.runtime.retainedActionState()`；然后对 next context 做一次现有 typed ACTIVE gate，
最后 CAS `ActiveRuntime(g, oldRuntime) -> ActiveRuntime(g+1, nextRuntime)`。CAS 同时发布 new context、matching new port 和同一
retained state reference；不会出现“slot 已换 context，但 facade/state 仍是上一代”的 active snapshot。

#### 3.4 retained state：taskRun-scoped handle，不再把 resume 当 renewal

新增 package-private reference/stable-key 校验：

```java
void requireReusableBy(
        CloudTaskRunExecutionContext context,
        CloudTaskRunExecutionGate executionGate,
        CloudTaskRunActionLedger actionLedger);
```

现有 `retainWindowFact/retainCapture/retainInputBundle` 签名不变：新 revision 若 address 已存在且 operation 相同，直接返回
同一 `record.current` handle，不因 handle 最初 revision 不同而拒绝，也不调用 ledger.acquire。`invoke` 仍要求 owner state、
record.current 与 operation 引用 exact，但删除“handle.context 必须等于 port context”的错误条件；改为：port context 必须与 state
stable key 同一并通过现有 gate，具体 bound/unbound 处理交 ledger/executor。`renewAfterNotExecuted` 的权限、条件与 ID 规则不变。

#### 3.5 ledger typed invocation plan

新增 package-private、无 null/boolean fallback 的 typed plan：

```java
InvocationPlan prepareInvocation(
        RetainedActionIdentity identity,
        CloudTaskRunExecutionContext exactCurrentContext);

sealed interface InvocationPlan
        permits BuildCurrentRequest, RedeliverBoundRequest {}

record BuildCurrentRequest(RetainedActionIdentity identity)
        implements InvocationPlan {}

record RedeliverBoundRequest(
        RetainedActionIdentity identity,
        RemoteRequest retainedRequest) implements InvocationPlan {}
```

`prepareInvocation` 在 ledger synchronized 边界内先 `requireCurrentRecord`，再要求 exactCurrentContext 与 identity.context 的
scope 四元组/taskRunId/taskType/window 四元组/non-terminal stopEpoch 全同：

- `boundRequest != null`：返回原对象 `RedeliverBoundRequest`，不改 identity.context，不改 bytes/outcome/IDs。
- `boundRequest == null`：同时要求 recordedState/outcomeDigest 为空；若 context 引用不同，只允许 strict newer revision，并仅把
  `RetainedActionIdentity.context`（改为 ledger-only mutable/volatile 字段）推进到 new context；返回
  `BuildCurrentRequest`。requestId/actionId/captureId/attempt 原值不动。

`bindOrVerifyRequest`、`recordOutcome`、`renewAfterNotExecuted` 与 hard cap 不改语义。bound identity 永不做 context rewrite；
UNKNOWN 仍不可 renewal。

#### 3.6 opaque handle 与 executor

`CloudTaskServicePort` 三个 public 业务方法签名完全不变。package-private `ActionHandle` 不再保存第二份 final context；
`context()` 如内部仍需要，只投影 `identity.context()`，避免 handle 与 ledger identity 漂成两个 revision。constructor/newHandle
相应删除裸 context 参数；owner/record/identity/operation reference fence 全保留。

`CloudTaskRunCommandExecutor` 的三个现有 `RemoteGameClientPort` 方法签名不变，每次调用：

1. 先 `gate.validate(exactCurrentContext)`；只按 typed `allowed` 分支，不解析 reason 文本。这是 bound redelivery 新增且不可删除的
   pre-mechanical current gate；unbound build 后续仍会再经过原 `gate.new*Request`、broker enqueue/dispatch/local fence。
2. 调 `actionLedger.prepareInvocation(identity, exactCurrentContext)`。
3. `BuildCurrentRequest`：走现有 `gate.newWindowFactRequest/newCaptureRequest/newInputBundleRequest`，正常 bind/send/record。
4. `RedeliverBoundRequest`：按具体 request record 类型逐字段要求当前调用的 factKind，或 region/imageFormat/purpose，或
   description/coordinateSpace/actions，以及 timeoutMs 与 retained request 完全相等；不构建新 request、不换 digest。然后把
   retained request 原对象交 broker。broker 命中 retained `requestId+digest` 后只读取原 terminal/late resolution，不 redispatch；
   如进程内 ledger 不一致导致 broker miss，旧 revision authorization 会 fail closed 为 NOT_EXECUTED，仍不得产生机械副作用。
5. 两分支都把 broker 返回的 exact typed outcome 交现有 `recordOutcome`，使 UNKNOWN 可收敛为 exact late final。

`RemoteGameClientPort`、broker、wire/request DTO、coordinator/gate 的 public/package-private API 均不需要修改。

### 4. Initial / resume runtime 构造图

```text
INITIAL (revision 1, current confirmed ACTIVE)
  assembly.createCurrentContextSlotActivation(scope, run, metadata)
    -> gate.createContext
    -> CloudTaskServiceExecutionContext initial constructor
       -> new CloudTaskRetainedActionState       [唯一一次]
       -> new CloudTaskServicePort(r1, SAME_STATE, executor)
    -> TaskServiceRuntime(authority, TaskExecutionContext-r1, SAME_STATE)
    -> slot ActiveRuntime(generation=1, runtime-r1)
  -> H.activateInitial(context-r1)
  -> construct AutoCombatService(slot, one AutoCombatRuntimeState)

PAUSE
  old call stack typed unwind
  slot/runtime-r1/SAME_STATE/action records/UNKNOWN bytes 全保留
  不 install、不 close、不 renewal

RESUME (new exact confirmed ACTIVE revision rN)
  assembly.createResumedTaskServiceRuntime(previousRuntime, scope, run, same metadata)
    -> gate.createContext(rN)
    -> CloudTaskServiceExecutionContext existing-state constructor
       -> requireReusableBy(rN, same gate, same ledger)
       -> new CloudTaskServicePort(rN, SAME_STATE, same executor)
    -> nextRuntime(authority, TaskExecutionContext-rN, SAME_STATE)
  under future activation owner lock:
    -> H.activateResumed(previousHandle, context-rN)
    -> slot.install(nextRuntime) [CAS whole runtime snapshot]
    -> publish runnable

FIRST ACTION USE AFTER RESUME
  bound action    -> current rN gate -> exact old request redelivery -> retained/late outcome
  unbound action  -> current rN gate -> same identity context advances to rN -> first normal bind
  NOT_EXECUTED    -> only trusted explicit renewal may create next attempt
```

不再有 Design #1 的“swap retained action state”；activation owner 始终携带同一 state reference。resume factory 与 slot install
均不提前 mutate action records，因此任何 typed-gate/CAS 失败都不会留下半推进 retained state。

### 5. self-CAS / slot generation 的准确语义

Design #1 中任何可能把 self-CAS 读成 lifecycle lease 的措辞作废，替换为：

- `current()` 读取 `ActiveRuntime(g, runtime)`，对 captured `TaskExecutionContext` 完成一次 existing typed
  current-confirmed ACTIVE gate，再执行 `state.compareAndSet(observed, observed)`。
- 该 self-CAS 的唯一证明是：**typed gate 后到 self-CAS 线性化点之间，slot generation 没被 install/close 改变**。它不锁
  coordinator，不阻止 self-CAS 后立刻 PAUSE/STOP/COMPLETE，也不授权任何机械副作用。
- 返回语义仅为“captured revision 在本次检查时通过 typed gate，且 slot generation 未变”；不是 lease、pin、transaction、
  lifecycle snapshot guarantee。
- 每个业务入口继续使用既有 checkpoint；每个 port 调用继续使用 state current gate、executor/gate request-build gate、broker
  enqueue/final-dispatch 与 DHXY pre-side-effect fence。bound exact-redelivery 也先对**当前新 context**做 gate。不得因为 slot
  存在删除、合并或跳过任何现有 gate。
- self-CAS 失败仍显式 concurrent reject，无内部 retry。即使 self-CAS 成功后 lifecycle 变化，后续 gate 会 typed unwind；
  negative signal 不转为业务 SUCCESS/FAILED。

### 6. Expanded exact 文件表

批准后 Cloud 写集固定为 **1 New + 6 Modify**：

| 动作 | 文件 | 精确职责 |
|---|---|---|
| New | `remote/CloudTaskRunCurrentContextSlot.java` | generation-stamped whole-runtime slot、same-state install、exact terminal close |
| Modify | `remote/CloudTaskRunAuthorityAssembly.java` | same-assembly identity；initial/resume runtime factory；activation bundle；runtime 携同一 state |
| Modify | `remote/CloudTaskServiceExecutionContext.java` | package-private existing-state constructor；每 revision new matching port |
| Modify | `remote/CloudTaskRetainedActionState.java` | taskRun stable-key/ref reuse；existing handle 跨 revision；不再 exact-context owner 误拒绝 |
| Modify | `remote/CloudTaskRunActionLedger.java` | typed build/redeliver plan；unbound same-ID context advance；bound bytes/UNKNOWN 原样保留 |
| Modify | `remote/CloudTaskServicePort.java` | public API 不变；handle 去除重复 frozen context，继续 opaque owner/record/identity fence |
| Modify | `remote/CloudTaskRunCommandExecutor.java` | current gate + typed plan；bound payload exact 校验与 broker retained-request redelivery |

以上均位于
`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\remote\`。

明确零修改：`CloudTaskRunExecutionGate`、`RemoteGameClientPort`、`RemoteGameCommandBroker`、全部 request/wire DTO、
coordinator/binding/status、H State owner、A PAUSED observer 双仓文件、B artifact 七文件、J turn、I properties、host/server/
endpoint/poller、DHXY Java/Maven/resources/tests。若实现发现必须扩大 1+6，立即停止写 conflict，不能越表。

### 7. 容量、并发、restart 与行为边界

- same state/map 不按 revision 复制，容量反而保持一 taskRun 一份；ledger 10,000 hard cap、coordinator/H quotas 不变。typed
  invocation plan 只引用现有 identity/request，不建立第二索引、队列、TTL 或 cleanup。
- 同一 action 仍由 `ActionRecord` monitor 串行；bound redelivery 与 late `recordOutcome` 不并发改 attempt。unbound context advance
  在 ledger synchronized 内完成，且只发生在无 bytes/outcome 的 current identity。
- slot CAS loser 的 next runtime 尚未修改 same state，可安全丢弃；winner 发布 whole runtime。旧 port 若在 install 后调用，旧
  exact context 会在 state/executor gate typed stale；不会借同 state 绕过 revision。
- process restart 后 assembly ledger/state/slot 全不恢复；旧 retained request 也不被伪造。replacement stop + 新 run activation
  规则不变，无 durable rehydration/takeover。
- 不改变 coordinator lifecycle/revision/confirm；不新增业务 retry、key declaration、renewal、phase/fallback、timeout 或输入顺序。
  **无已批准业务差异；按基线等价迁移。**

### 8. P0/P1/P2、自审与批准后门

- P0=0。
- P1=0：一 taskRun 的 retained state/records/handle/UNKNOWN bytes 同对象保留；每 revision runtime 的 context/port 是 matching
  immutable snapshot；bound 与 unbound 两种续接均有强类型、可编译 owner 路径，无重新声明/自动 renewal。
- P2=0：self-CAS 仅定义 slot-generation 线性化；所有 coordinator/pre-mechanical gates 原样保留，明确不是 lifecycle lease。
- 自审：expanded 1 New + 6 Modify 覆盖 constructor、state owner checks、handle context、ledger identity/bound request 与 executor
  redelivery 的完整编译闭包；无占位符、null plan、reason 文本分支、新增 ThreadLocal/secondary map/TTL/thread/retry。
  自审不构成批准。
- 父级写入 `DESIGN APPROVED` 前继续零 Java/Maven/resources/tests。批准后只实施上述 1+6；不新增/恢复测试，不启动任何
  application/server/host/Task/poller/UI/capture/OCR/input；完成源码后按父级最新 Cloud package 门执行并回写证据。

Design Repair #1 到此停止，等待父级 `DESIGN APPROVED` / `BLOCKED`。

## Parent Design Review #2 - BLOCKED - 2026-07-12

Repair #1 已正确识别“一 taskRun retained state 必须跨 revision 同对象”，并把 slot generation 改为完整 runtime
`(context + matching port + same state)` 原子 CAS；bound old bytes 仅走 broker retained-resolution、unbound same-ID 仅在首次
bind 前推进 context 的方向也可保留。但当前 exact 写集与外部 A 已批准实施直接冲突，resume metadata 又可漂移。结论：
**BLOCKED，P0=0/P1=2/P2=1**。

### P1-1：K 把 A 正在实施的 `CloudTaskRunActionLedger` 列入写集，违反零交叉且没有定义两套 identity/cap 的合并

- 证据：K §6 把 `CloudTaskRunActionLedger.java` 列为 Modify，并要把 ACTIVE `RetainedActionIdentity.context` 改为
  mutable；外部 A 已在同一文件实施批准的独立 `ObservationActionIdentity`、observation map、ACTIVE/OBSERVATION mode
  conflict 与 combined 10,000 hard cap。K §6 却仍声称“A PAUSED observer 双仓文件零修改”，与文件表自身矛盾。
- 影响：两个 Worker 并发改同一 authority 文件会覆盖/破坏 observation no-renewal、combined quota 或 K 的 active
  build/redelivery plan；即使文本各自正确，也没有一份可审查的 merged ledger 状态机。
- 返修条件：K 不得在 A Implementation APPROVED 前实施。Repair #2 必须基于 A 最新落盘后的 ledger 形状重列 merged
  ACTIVE-only改动位点，明确 Observation identity/context/map/bind/outcome/no-renewal/mode-conflict/combined quota 全部保持；K 的
  mutable context 与 invocation plan 只能作用于 ACTIVE `RetainedActionIdentity`。若无法做到同文件顺序实施，改设计移除该
  overlap；不得继续声称零修改 A 文件。

### P1-2：resume factory 接受 caller 新传 `CloudTaskServiceMetadata`，允许 same taskRun 业务身份漂移

- 证据：§3.2 的 `createResumedTaskServiceRuntime(previousCurrentRuntime, scope, taskRunId, metadata)` 接收一份新 metadata，
  而 stable key/`requireReusableBy` 不包含 role、requested task、team session/leader/support、startup mode、retry policy、
  startedAt 等字段。§4 仅在示意图写“same metadata”，没有 authority 强制。
- 影响：resume 可在保留同一个 `AutoCombatRuntimeState`/retained action state 的同时切换角色、队伍或 retry/startup 业务语义，
  形成同 taskRun 内不可审计的业务状态注入；这不是 lift-and-shift 允许的 revision 更新。
- 返修条件：initial runtime 必须 retained exact immutable metadata；resume factory 不接受 caller-provided metadata，直接复用
  previous/slot retained metadata（或至少 reference/exact value equality 后仍使用原对象）。`TaskServiceRuntime`/service context
  需提供 package-private 强类型来源，不能从 public getters 重建。taskCode 仍与 coordinator taskType exact。

### P2-1：bound redelivery 的“broker miss”不得被泛化为可信 renewal 证据

- 证据：§3.6 写 broker miss 后旧 revision authorization 返回 `NOT_EXECUTED`，随后现有 state 可 renewal。same-process 正常
  顺序下，ledger 已 bound 而 broker miss 只可能表示调用尚未进入 broker；但任何 ledger/broker authority 不一致都属于结构故障，
  不能仅凭 stale authorization 推断历史动作未执行。
- 影响：若 future refactor/partial authority recovery 让 broker 丢 record 而 ledger/slot 仍在，旧动作可能已执行却被续发。
- 返修条件：Repair #2 把可 renewal 的 broker-miss 条件限定为同一 process/authority assembly 内可证明“bound 后从未进入
  broker”的 retained dispatch state；否则保持 UNKNOWN/structural fail-closed，不记录可 renewal `NOT_EXECUTED`。不得声称
  restart 可恢复；restart 继续整体丢弃 slot/state/ledger。

Worker K 只追加 `Internal Worker K - Design Repair #2`，先等待/读取 A 最新 Implementation 材料再给 merged exact table；
父级批准前继续零 Java/Maven/resources/tests。J 继续暂关，L 不受影响并行设计。
**无已批准业务差异；按基线等价迁移。**

## Internal Worker K - Design Repair #2 - 2026-07-12

针对 `Parent Design Review #2 - BLOCKED` 的 P1=2/P2=1 定点返修。K 已按要求等待，并完整读取 A 最新
`External Worker A - W0 Implementation #1`，不是只读其 Design：A report SHA-256=`64698B69...A487`；落盘
`CloudTaskRunActionLedger=45D2A26D...F138B`、`RemoteGameCommandBroker=5E6C3031...6BF62`、
`CloudTaskRunExecutionGate=0C55C9F6...83A4`。A 当前只有 worker 自审，尚无父级 Implementation APPROVED；因此本 Repair
可以设计 merged delta，但 K Java 仍被 A final approval 与本卡 DESIGN APPROVED 双门共同阻断。

Design #1/Repair #1 已通过的 stable full key、same-taskRun same retained-state reference、whole-runtime generation CAS、
bound old bytes / unbound same-ID 分流、exact terminal close和 self-CAS 非 lease 语义继续保留。本 Repair 只修三点：按 A
实际 Observation ledger 合并 ACTIVE plan、resume metadata 唯一来源、broker-miss 可证明性。

### 1. A 落盘后的 ledger 基线与不可覆盖区

A 的 current ledger 已形成两个互斥 authority mode：

```text
ACTIVE
  records<ActionKey, ActionRecord>
  RetainedActionIdentity
  acquire / requireOwnedCurrent / bindOrVerifyRequest / recordOutcome
  renewAfterNotExecuted

PAUSED OBSERVATION
  observationRecords<ObservationKey, ObservationRecord>
  observationKeyUse<ActionKey, count>
  ObservationActionIdentity (与 ACTIVE 无继承/转换)
  acquireObservation / bindOrVerifyObservationRequest
  recordObservationOutcome / retainedObservationRequest
  NO renewal overload

SHARED LEDGER MONITOR
  retainedTotal() = records.size() + observationRecords.size()
  combined hard cap = 10,000
  bidirectional ACTIVE/OBSERVATION key conflict
```

K 的 merged patch 只允许改 ACTIVE 半边和共享方法中不可避免的 ACTIVE 分支；以下 A 区域逐项冻结：

1. `ObservationActionIdentity` 字段/constructor/capability reference equality/operation/paused revision 全不改，context 永远 immutable。
2. `observationRecords`、`observationKeyUse`、`ObservationKey`、`ObservationRecord` 全不改，不加 ACTIVE plan 字段。
3. `acquireObservation`、`bindOrVerifyObservationRequest`、`recordObservationOutcome`、`retainedObservationRequest`、
   `requireObservationRecord` 全不改；Observation 继续结构性无 renewal。
4. `retainedTotal()` 继续 ACTIVE records + observation records 合计；ACTIVE `acquire` 的 `observationKeyUse` 检查和
   Observation `acquire` 的 `records.containsKey` 检查原样保留，combined quota 不拆、不重复计数。
5. A 在 broker 新增的 observation enqueue/final-dispatch 分支、PAUSED/current-revision gate 和普通 ACTIVE 分支均不改；K
   只增加一个 observationMode 必须为 null 的 ACTIVE retained-presence probe。

Repair #1 的“ A PAUSED 文件零修改”声明作废。ledger/broker 是**明确重叠、严格顺序实施**文件，不再伪称零交叉。

### 2. ACTIVE invocation plan 与 Observation ledger 的 merged 状态机

#### 2.1 ACTIVE-only identity/context 规则

只有 `RetainedActionIdentity.context` 改为 ledger-private mutable/volatile；`ObservationActionIdentity.observationCapability` 保持
final。ACTIVE context rewrite 只能发生在 `records` 当前 identity、`boundRequest == null`、`recordedState == null`、
`recordedOutcomeDigest == null`，且 next context full stable key exact、revision strictly newer 时；requestId/actionId/captureId/
attempt 不变。任何 Observation identity 在类型上都不能传入下述 ACTIVE plan API。

#### 2.2 ACTIVE dispatch evidence

只给 ACTIVE `ActionRecord` 增加：

```java
private ActiveDispatchState dispatchState = ActiveDispatchState.UNBOUND;

private enum ActiveDispatchState {
    UNBOUND,
    BOUND_NOT_ENTERED_BROKER,
    BROKER_ENTRY_MARKED
}
```

- 第一次 ACTIVE `bindOrVerifyRequest` 原子保存 bound request/digest 后设 `BOUND_NOT_ENTERED_BROKER`。
- 新 package-private `markActiveBrokerEntry(identity, exactBoundRequest)` 只允许
  `BOUND_NOT_ENTERED_BROKER -> BROKER_ENTRY_MARKED`；同 identity/request 的重复 mark 幂等，任何不同 request/digest/state 拒绝。
- `recordOutcome` 额外要求 `BROKER_ENTRY_MARKED`，所以可 renewal 的 NOT_EXECUTED 必须来自“已标记进入同 assembly broker”后的
  exact typed broker outcome；不能由 K 自己构造 outcome 或只看 stale authorization reason。
- renewal 后 replacement record attempt 仍在同 `ActionRecord`，清 bound/outcome 并把 dispatchState 复位 `UNBOUND`；A 的
  observation record 没有该 enum/字段/transition。

#### 2.3 merged typed invocation plan

Repair #1 的 ACTIVE-only API 保留但补 dispatch evidence，且名称显式 ACTIVE：

```java
ActiveInvocationPlan prepareActiveInvocation(
        RetainedActionIdentity identity,
        CloudTaskRunExecutionContext exactCurrentContext);

sealed interface ActiveInvocationPlan
        permits BuildActiveCurrentRequest, UseActiveBoundRequest {}

record BuildActiveCurrentRequest(RetainedActionIdentity identity)
        implements ActiveInvocationPlan {}

record UseActiveBoundRequest(
        RetainedActionIdentity identity,
        RemoteRequest retainedRequest,
        ActiveDispatchState dispatchState) implements ActiveInvocationPlan {}
```

`prepareActiveInvocation` 仍在 ledger 同一 synchronized monitor 内执行：先 `requireCurrentRecord`，再 full stable key/revision 校验；
unbound 才无换 ID 推进 ACTIVE context 并返回 build plan；bound 返回原 request + current ACTIVE dispatch state，绝不访问或转换
observation map/identity。该 API 没有 Observation overload，也不改变 combined cap。

### 3. Broker exact-presence proof 与 renewal 门

#### 3.1 broker package-private ACTIVE probe

在 A 已落盘的 `RemoteGameCommandBroker` 上只新增 read-only API：

```java
ActiveRetainedRequestPresence probeActiveRetainedRequest(
        RemoteClientScope scope,
        RemoteRequest exactRequest);

enum ActiveRetainedRequestPresence {
    EXACT_MATCH,
    MISSING,
    IDENTITY_OR_DIGEST_CONFLICT
}
```

方法首先要求 `exactRequest.context().observationMode() == null`，Observation request 结构性不可调用；随后在现有
`stateLock` 下按 `(scope, operation, requestId)` 查 `requestLedger`：不存在=`MISSING`；存在且 digest 相等=`EXACT_MATCH`；
其它=`IDENTITY_OR_DIGEST_CONFLICT`。只读，不 register、不 authorize、不 enqueue、不触碰 pending/usage/route，也不解析 reason。
broker retained history无 eviction/TTL，故同进程内 `EXACT_MATCH` 一旦成立不会回退为 MISSING。

#### 3.2 同 authority “未入 broker”的双证据

可证明未入 broker 必须同时满足：

1. ledger 当前 ACTIVE record 为 `BOUND_NOT_ENTERED_BROKER`；该 marker 在同 assembly ledger monitor 内由 first bind 设置，
   command executor 任何真实 broker 调用之前必须先 `markActiveBrokerEntry`。
2. **同一个 `CloudTaskRunCommandExecutor` 持有的同一个 broker 实例** probe 返回 `MISSING`；executor/ledger/broker 均由
   `CloudTaskRunAuthorityAssembly` 同一 opaque identity 构造，不能换 broker/ledger 做证明。
3. exact request reference/identity/digest 与 ledger bound object 全等；当前调用仍先对 new exact context 做 ACTIVE gate。

只有三项同时成立，executor 才可执行如下安全路径：

```text
markActiveBrokerEntry(exact old bound request)
  -> 调同 broker 的既有 typed method（不是直接 mint outcome）
  -> broker 对旧 revision 做正常 authorization
  -> 返回真实 NOT_EXECUTED（resume 后旧 revision）或正常首次执行结果（若尚未跨 revision）
  -> ledger.recordOutcome(exact typed broker result)
```

因此 renewal 仍只由既有 `recordOutcome -> recordedState==NOT_EXECUTED -> renewAfterNotExecuted` 开门；K 不增加第二 renewal API，
不伪造 NOT_EXECUTED，也不把 MISSING 本身当 outcome。

#### 3.3 其余 broker-miss 一律 structural fail-closed

| ledger dispatch state | broker probe | 处理 |
|---|---|---|
| `BROKER_ENTRY_MARKED` | `EXACT_MATCH` | exact old bytes retained-resolution；UNKNOWN 可等 late final |
| `BOUND_NOT_ENTERED_BROKER` | `MISSING` | 上述双证据成立；先 mark，再走真实 broker，结果才可记录/renew |
| `BROKER_ENTRY_MARKED` | `MISSING` | authority 结构不一致；不调用 broker、不记录 NOT_EXECUTED、不 renewal；已有 UNKNOWN 保持 UNKNOWN，未记录保持 unrecorded |
| `BOUND_NOT_ENTERED_BROKER` | `EXACT_MATCH` | 有 broker record 却无 entry marker；结构不一致，同样 fail-closed |
| 任意 | `IDENTITY_OR_DIGEST_CONFLICT` | 幂等冲突，structural fail-closed |

结构故障使用 package-internal typed `IllegalStateException`/明确日志中止当前调用，不自动 retry/rebuild/requestId renewal。restart 后
slot/state/ledger/broker 一起丢弃，不存在跨进程 presence proof 或恢复路径。

### 4. Initial immutable metadata 唯一来源

`CloudTaskServiceMetadata` 是 immutable record；initial activation 传入的**对象引用**固定为 taskRun retained metadata。为防
resume caller 注入新值，`TaskServiceRuntime` 保留强类型内部来源：

```java
record TaskServiceRuntime(
        AuthorityInstanceIdentity authorityIdentity,
        CloudTaskServiceExecutionContext serviceExecutionContext,
        TaskExecutionContext taskExecutionContext,
        CloudTaskRetainedActionState retainedActionState,
        CloudTaskServiceMetadata initialMetadata) {}
```

initial factory 仍接收 metadata，创建 service context 后把同一对象引用写入 runtime。resume factory **删除 metadata 参数**：

```java
TaskServiceRuntime createResumedTaskServiceRuntime(
        TaskServiceRuntime previousCurrentRuntime);
```

它只从 package-private runtime 取得：

- `previous.serviceExecutionContext().runContext()` 的 scope/taskRunId，交现有 gate mint new exact current context；不从 public
  `TaskExecutionContext` getters 重建 authority。
- `previous.retainedActionState()` 同一对象。
- `previous.initialMetadata()` 同一对象，原样传 existing-state constructor；不 clone、不 builder 重建、不 value-equal 替换。

new runtime 必须 `initialMetadata == previous.initialMetadata` 且 retained state/authority identity 同一。slot `install` 再要求
next metadata、state、authority identity 分别 reference equal observed runtime；taskCode 仍由
`CloudTaskServiceExecutionContext` constructor 与 coordinator `taskType` exact compare。role、requested task、team session/
leader/support、retry policy、startup mode、startedAt 在 same taskRun resume 全部不可漂移。

### 5. Revised initial/resume/runtime 图

```text
INITIAL revision 1
  assembly.createCurrentContextSlotActivation(scope, run, INITIAL_METADATA)
    -> initial service context
       -> new SAME_RETAINED_STATE (唯一一次)
       -> port(r1, SAME_RETAINED_STATE)
    -> runtime(authority, serviceContext-r1, taskContext-r1,
               SAME_RETAINED_STATE, INITIAL_METADATA)
    -> slot generation 1

RESUME revision rN
  assembly.createResumedTaskServiceRuntime(previousRuntime)   // 无 metadata 参数
    -> previous internal runContext supplies scope/run
    -> gate.createContext(rN)
    -> existing-state service context(rN, SAME_RETAINED_STATE, INITIAL_METADATA)
    -> port(rN, SAME_RETAINED_STATE)
    -> next runtime refs SAME authority/state/metadata
  -> H.activateResumed
  -> slot.install(nextRuntime) whole-runtime CAS

ACTIVE ACTION after resume
  -> current rN checkpoint/port gate
  -> prepareActiveInvocation
     unbound: same IDs first-bind at rN
     bound: exact old bytes + dispatch evidence
  -> broker exact-presence matrix
  -> only real broker outcome may change recorded state / permit renewal
```

slot self-CAS 仍只证明 slot generation 未变，不是 lifecycle lease；metadata/state reference equality 也不替代 current context、
request-build、broker enqueue/final-dispatch 或 DHXY pre-side-effect gates。

### 6. Merged expanded exact 文件表与顺序门

批准后的 K Cloud 写集为 **1 New + 7 Modify**：

| 动作 | 文件 | K delta | 与 A 关系 |
|---|---|---|---|
| New | `remote/CloudTaskRunCurrentContextSlot.java` | whole-runtime generation slot、same state/metadata install、terminal close | A 未触碰 |
| Modify | `remote/CloudTaskRunAuthorityAssembly.java` | authority identity；initial/resume factory；runtime retained metadata/state | A 未触碰 |
| Modify | `remote/CloudTaskServiceExecutionContext.java` | existing-state + initial metadata reference constructor | A 未触碰 |
| Modify | `remote/CloudTaskRetainedActionState.java` | same object/handle 跨 revision；ACTIVE invocation owner checks | A 明确保持原样，K 后续顺序改 |
| Modify | `remote/CloudTaskRunActionLedger.java` | **仅 ACTIVE** plan/context/dispatch evidence；Observation 全冻结 | **A 重叠，必须基于其 APPROVED 源顺序 patch** |
| Modify | `remote/CloudTaskServicePort.java` | public API 不变；ACTIVE handle 不重复冻结 old context | A 未触碰 |
| Modify | `remote/CloudTaskRunCommandExecutor.java` | current gate、ACTIVE plan、presence matrix、exact redelivery | A 未触碰 |
| Modify | `remote/RemoteGameCommandBroker.java` | 只读 `observationMode==null` ACTIVE exact-presence probe | **A 重叠，必须保留其 observation 双门** |

零修改：A 的其余 Cloud/DHXY/schema 文件、`CloudTaskRunExecutionGate`、`RemoteGameClientPort`、coordinator、全部 wire/request
DTO、H owner、J turn、I properties、B artifact、host/server/endpoint/poller、DHXY Java/Maven/resources/tests。

**实施顺序硬门：**

1. A 固定报告必须先出现父级 `Implementation APPROVED`；若 A 被 BLOCKED/返修，K 继续等待并以 A 最新批准源码重新核表。
2. K 即使先获 DESIGN APPROVED，也不得在 A final approval 前改 ledger/broker；绝不与 A heartbeat/返修并发写同文件。
3. K 实施开始前重新记录 A approved ledger/broker hash，逐段读取 current source；只在其上加 ACTIVE delta，禁止用 pre-A
   文件、整文件替换或 checkout 覆盖。
4. 实施后 diff 必须证明 Observation types/maps/methods、cross-mode conflict、combined cap、no-renewal 和 broker observation
   enqueue/final-dispatch 分支未改变；任何 A 行发生非必要差异即停止，不交构建。

若 merged API 编译需要扩大 1+7 或触碰 A 其它文件，先写 conflict 等父级，不得顺手补。

### 7. P0/P1/P2、自审与批准后门

- P0=0。
- P1=0：A Observation 与 K ACTIVE invocation plan 已在同一 ledger 状态机中分区列明；共享 monitor/cap/conflict 保持，重叠
  文件实施顺序锁定为 A Implementation APPROVED 后；resume 只复用 initial metadata 对象，无 caller metadata 面。
- P2=0：broker miss 只有 `BOUND_NOT_ENTERED + same broker MISSING` 双证据才可进入真实 broker NOT_EXECUTED 路径；其余
  MISSING/conflict 保持 UNKNOWN/unrecorded 并 structural fail-closed，不产生 renewal authority。
- 自审：merged 1 New + 7 Modify 覆盖 slot/runtime/context/state/ACTIVE ledger/handle/executor/broker proof 的编译闭包；无
  Observation renewal、combined quota 拆分、metadata clone/public rebind、reason 文本判断、自动 retry、TTL/takeover/restart
  recovery。自审不构成批准。
- 父级 DESIGN APPROVED 与 A 父级 Implementation APPROVED 前继续零 Java/Maven/resources/tests。双门满足后 K 只实施
  1+7；不新增/恢复测试，不启动 application/server/host/Task/poller/UI/capture/OCR/input；构建门以父级最新指令为准。

**无已批准业务差异；按基线等价迁移。** Design Repair #2 到此停止，等待父级 `DESIGN APPROVED` / `BLOCKED`。

## Parent Design Review #3 - BLOCKED - 2026-07-12

**BLOCKED，P0=0/P1=1/P2=0。** A Observation ledger 的冻结区、ACTIVE-only dispatch evidence、同 broker presence
matrix、initial metadata 引用复用与 A-first 顺序门均成立；上一轮三个 blocker 已关闭。但 revised runtime/install 图仍未满足
父级要求的 context/port/retained state/**GameContext State handle** 同一 generation 原子发布。

### P1：H state activation 与 slot install 之间可裂开

- 证据：第 872-877 行的 `TaskServiceRuntime` 不含 H 的 `StateActivationHandle`；第 912-919 行顺序是先创建 next runtime，
  再 `H.activateResumed`，最后才 `slot.install(nextRuntime)`。`slot.install` 仍是 generation CAS，故它可以在 H 已把唯一 State
  推进到新 revision 后失败。
- 影响：CAS 失败时 slot 仍发布旧 runtime/context/port，而 H owner 已只接受新 revision handle；同一 taskRun 的业务
  runtime 与 `GameContext.State` authority 永久裂开，旧 slot 不再可执行，新 state handle 又未被 current slot 发布。仅靠
  reference equality 或事后异常无法回滚 H 的 monotonic activation。
- Repair #3：`TaskServiceRuntime` 必须包含 exact current H `StateActivationHandle`；resume transition 必须由一个
  assembly-owned 原子/串行边界完成，使 observed slot generation 验证、H `activateResumed` 与 next runtime publish 不存在
  可失败的中间窗口。可采用 slot transition permit/lock：先锁定 expected generation，锁内调用 H activation，再以不可失败的
  exact commit 发布包含新 state handle 的完整 runtime；任何 H 之前的验证失败零写，任何 H 异常保持旧 generation。不得用
  “先 H 后普通 CAS”、补偿回滚或第二份 State。写集若需扩大到 H owner，先报告冲突，不得直接改。

K 继续只做 Design Repair #3，A 未获父级 Implementation APPROVED 前仍零 Java。
**无已批准业务差异；按基线等价迁移。**

## Internal Worker K - Design Repair #3 - 2026-07-12

针对 `Parent Design Review #3 - BLOCKED` 唯一 P1 定点返修。已完整读取新审查段，并复核当前
`CloudGameContextStateOwner.activateResumed` 落盘源码（SHA-256=`8D5BBEFA...DBF`）。结论：**不需要修改 H owner 即可
闭合**；H 当前 package-private typed API 已提供“旧 projection 退出后、同一 State monotonic 产生 next handle”的原子段，缺失的
只是 slot/assembly 必须在 H 调用前锁定 exact generation，并在 H 成功后使用不可失败 commit 发布该 handle。

A 当前仍处于 `Parent W0 Implementation Review #1 - BLOCKED`（observer operation deadline P1）；因此 Repair #2 的 A-first
顺序门继续有效。K 只有设计文本，零 Java/Maven/resources/tests。

### 1. H 当前源码为何无需修改

`activateResumed(previousHandle, nextContext)` 的实际顺序是：

1. 校验 handle owner、same full `GameStateRunKey`、strict newer revision；失败时未写 owner。
2. `ownerMonitor` 下确认 entry/current handle/RETAINED；释放 monitor，仍未写。
3. `executionLock.lockInterruptibly()` 等旧 projection stack 退出；interrupt 时未取得锁或未写。
4. 锁内对 next context 执行唯一 final typed current-confirmed gate；失败未写。
5. `ownerMonitor` 下再次确认 entry/current handle/activeProjectionCount=0；计算 generation 并先构造新
   `StateActivationHandle`。以上任何异常均发生在 owner 字段写入前。
6. 最后三个 plain assignment 连续写 `entry.currentRevision`、`entry.activationGeneration`、`entry.currentHandle`，随后直接 return；
   `finally` 只 unlock execution lock。普通成功路径不存在“写了 H 后又抛业务异常”的分支。

因此 assembly 可以把 H 返回视为单一 commit fact：抛出/interrupt = H 保持 previous handle；正常返回 = H 已完整切到 returned
handle。无需给 H 加 callback、rollback、第二 State 或新方法。

### 2. `TaskServiceRuntime` 纳入 exact H handle

Repair #2 的 runtime 改为 package-private static final class；每个**已发布** runtime 必须同时包含五组 exact reference：

```java
static final class TaskServiceRuntime {
    private final AuthorityInstanceIdentity authorityIdentity;
    private final CloudTaskServiceExecutionContext serviceExecutionContext;
    private final TaskExecutionContext taskExecutionContext;
    private final CloudTaskRetainedActionState retainedActionState;
    private final CloudTaskServiceMetadata initialMetadata;
    private CloudGameContextStateOwner.StateActivationHandle stateActivationHandle;
}
```

`stateActivationHandle` 是唯一 pre-publication write-once 字段：runtime 及其 matching service context/port/state/metadata 全部在 H
调用前预分配，但 runtime 不可达；H 成功返回后 transition permit 只做一次 plain reference attach，再通过 slot
`AtomicReference.set` safe-publish。发布后不再修改，业务/activation 读取到的 runtime 始终有 non-null exact handle。

不用 immutable record 的原因是避免 H 成功后再分配一个 record 形成理论/普通失败点；这不是 public mutable rebind。attach 方法
private/package-private 且只接 H 返回类型，没有 raw revision/state setter；未绑定 runtime 只能存在于持锁 permit 内，不能由
`current()`、host、Service 或 caller 获得。

initial runtime 同样先预分配全部非 H 字段，再调用 `H.activateInitial(initialContext)`，成功后 plain attach + initial slot publish；
`CurrentContextSlotActivation` 返回的首代 runtime 已包含 exact initial `StateActivationHandle`。

### 3. 删除普通 install，改用 non-mintable generation handle + transition permit

Repair #1/#2 的 package-private `slot.install(nextRuntime)` **删除**，避免任何 caller 绕过 H。slot 保留 public `current()` 和
package-private exact terminal close，并新增仅 authority package 可达的非铸造类型：

```java
static final class SlotGenerationHandle {
    private final CloudTaskRunCurrentContextSlot owner;
    private final String slotNonce;
    private final long slotGeneration;
    private final ActiveRuntime exactActiveRuntime;
}

private record ActiveRuntime(
        long slotGeneration,
        CloudTaskRunAuthorityAssembly.TaskServiceRuntime runtime) implements SlotState {}
```

generation handle constructor private，由 slot initial publish/commit 唯一产生；expected 验证要求 owner reference、slot nonce、generation、
`ActiveRuntime` reference 四项 exact。generation monotonic increment，不存在 ABA 或 raw long 猜测面。

assembly 对 future trusted activation owner 只开放一个 package-private resume 入口：

```java
SlotGenerationHandle resumeTaskServiceRuntime(
        CloudTaskRunCurrentContextSlot slot,
        SlotGenerationHandle expectedGeneration) throws InterruptedException;
```

`CurrentContextSlotActivation` 初始 bundle 包含 slot + initial generation handle；resume 成功返回 next generation handle。caller 不传
previous runtime、metadata、retained state 或 H handle，全部从 expected handle 锁定的 exact active runtime 内取得。

### 4. Slot transition permit / lock 状态机

slot 新增一个 instance `ReentrantLock transitionLock`。它只串行 resume transition 与 terminal close；`current()` 继续使用
AtomicReference + typed gate + self-CAS，不取得该锁，避免 H execution stack 内再次 `current()` 造成锁逆序。

permit 是 private/non-mintable、线程绑定且不离开 assembly 调用栈：

```text
ACTIVE(g, runtime-g)
  -- begin(expectedHandle-g), lockInterruptibly --> PERMIT_HELD_PRE_H
  -- any pre-H failure/interrupt -------------> ACTIVE(g, runtime-g) + unlock
  -- H throws/interrupt ----------------------> ACTIVE(g, runtime-g) + unlock
  -- H returns handle-(gH+1) -----------------> COMMIT_ONLY
  -- plain attach + state.set(preallocated g+1)-> ACTIVE(g+1, runtime-g+1) + unlock

ACTIVE(any) -- exact terminal close under same lock --> CLOSED(exact terminal binding)
CLOSED      -- resume begin ------------------------> typed permanent reject before H
```

`PERMIT_HELD_PRE_H`/`COMMIT_ONLY` 只存在于持锁栈上，不写入 public slot state；AtomicReference 对外仍只有 ACTIVE/CLOSED。

#### 4.1 begin 与所有可失败工作都在 H 之前

`beginResumeTransition` 使用 `transitionLock.lockInterruptibly()`；锁内完成：

1. expected generation handle 的 owner/nonce/generation/active-runtime reference exact 校验；closed/stale/concurrent resume 在此拒绝。
2. 从 observed runtime 取得 same assembly identity、internal runContext scope/taskRunId、same retained state、same initial metadata、
   previous exact H handle；不从 public getter 重建。
3. `executionGate.createContext` mint next current confirmed-ACTIVE context，构造 existing-state service context/new matching port/new
   TaskExecutionContext；执行 full stable key、strict revision、same state/metadata/authority reference 与 typed gate 校验。
4. 预分配“尚未绑定 H handle”的 next `TaskServiceRuntime`、`ActiveRuntime(g+1,nextRuntime)` 和 next
   `SlotGenerationHandle`；计算 generation 用 `Math.incrementExact`。所有 constructor/null/overflow/metadata/taskType 错误都在 H 前。
5. permit 保存 observed active、prepared next active、prepared next generation handle 与当前 thread；此后不再做可能拒绝的验证。

以上失败统一 abort permit、unlock，slot/H 都保持 generation g；没有 state/retained metadata/action record 写入。

#### 4.2 H 调用与不可失败 exact commit

assembly 在 permit 持锁期间调用：

```java
StateActivationHandle nextStateHandle = gameContextStateOwner.activateResumed(
        observedRuntime.stateActivationHandle(),
        preparedNextRuntime.taskExecutionContext());
```

- H 抛 `InterruptedException`、typed transition 或 `IllegalStateException`：根据 §1，H owner 未写；assembly abort/unlock，slot 仍 g。
- H 正常返回：permit 进入 `COMMIT_ONLY`。从这一行到 publish 之间**禁止** gate、CAS、allocation、exact compare、interrupt check、
  logging、callback 或任何可抛业务方法。
- commit 仅执行预分配对象上的 `preparedNextRuntime.stateActivationHandle = nextStateHandle` plain assignment，随后
  `state.set(preallocatedNextActiveRuntime)` unconditional volatile publish，再标记 permit committed 并 unlock。expected generation 已被
  transition lock 固定，故不使用 CAS，也没有 concurrent loser 分支。
- next generation handle 也是 H 前预分配，commit 后直接返回；H 成功后无对象创建。异步 interrupt flag 在 COMMIT_ONLY 不检查，
  必须先 publish 一致 generation，再由后续 checkpoint 处理。

这满足：pre-H 验证失败零写；H 异常旧 generation；H 成功后 exact commit 无普通可失败窗口。禁止补偿/回滚 H，因为根本不产生
需要回滚的 split。

### 5. 锁序与并发矩阵

#### 5.1 唯一锁序

```text
resume:
  slot.transitionLock
    -> coordinator monitor（createContext/typed gate，方法返回即释放）
    -> H.ownerMonitor（H 前置读取，释放）
    -> H.executionLock
      -> H.ownerMonitor（exact handle commit）
    -> slot unconditional publish

normal business:
  slot.current() [lock-free AtomicReference/self-CAS]
    -> H.callWithState executionLock
      -> business may再次 slot.current() [仍 lock-free]

terminal:
  slot.transitionLock -> exact coordinator terminal read -> slot CLOSED -> unlock
  then H.releaseTerminal（slot lock 已释放）
```

任何路径都不在持有 H executionLock/ownerMonitor 时获取 `transitionLock`；`current()` 刻意不锁。故不存在
`slot lock -> H lock` 与 `H lock -> slot lock` 环。ledger/broker/action locks 不参与 resume permit。

#### 5.2 并发结果

| 竞态 | 结果 |
|---|---|
| 两个 resume 同 expected handle | 第一方持 permit；第二方随后取得锁时 expected active reference/generation 已 stale，在 H 前拒绝 |
| close 先于 resume | CLOSED under lock；resume 在 H 前永久拒绝 |
| resume 先于 close | resume H+unconditional publish 完成并 unlock；close 再按 next runtime/current terminal binding 关闭，不裂代 |
| current 与 resume | current 不阻塞 H；旧 context typed gate 若已 stale 则 unwind；若 self-CAS 在 publish 后执行则失败；publish 前成功者线性化在旧代且后续 port/H gate 仍保护 |
| old H projection 与 resume | H.activateResumed 等 executionLock；旧业务内可 lock-free current 并因 lifecycle stale unwind，释放 projection 后 H 才推进 |
| terminal lifecycle 发生在 H final gate 前 | H typed gate 抛出，slot/H 保持旧 generation，随后 close |
| terminal 发生在 H 成功附近 | H 成功则 slot 必定先 publish matching next runtime；close 在 permit 后关闭。slot/H 不会一新一旧 |

### 6. Interrupt / exception 路径

| 位置 | 处理 | slot/H 后态 |
|---|---|---|
| 等 `transitionLock` 被 interrupt | `lockInterruptibly` 直接抛出 | slot g / H previous，零写 |
| permit 内 createContext/constructor/stable key/overflow/gate 失败 | abort + unlock，原异常上抛 | slot g / H previous，零写 |
| H 等 executionLock 被 interrupt | H `InterruptedException` 上抛；permit abort | slot g / H previous |
| H final typed gate/handle exact 检查失败 | H 在 owner assignment 前抛；permit abort | slot g / H previous |
| H 正常返回后 thread interrupt flag 被设置 | COMMIT_ONLY 不观察 interrupt，先 plain attach + publish | slot g+1 / H next；后续 checkpoint 再 unwind |
| commit 后 caller 日志/后续 activation 失败 | generation 已一致发布，不回滚；上层按 current typed state处理 | slot g+1 / H next |

不捕获并伪装 H exception，不把 interrupt 转业务 FAILED/SUCCESS，不在 H 成功后执行补偿。进程级 fatal failure不做 same-process
恢复；restart 仍整体丢弃 slot/state/ledger，并走既有 replacement STOP + 新 run。

### 7. Revised exact write set

不需要扩大到 H owner。Repair #2 的 K Cloud 写集保持 **1 New + 7 Modify**，职责更新如下：

| 动作 | 文件 | Repair #3 后职责 |
|---|---|---|
| New | `remote/CloudTaskRunCurrentContextSlot.java` | transition lock、opaque generation handle/permit、预分配 resume state、unconditional commit、terminal serialization |
| Modify | `remote/CloudTaskRunAuthorityAssembly.java` | `TaskServiceRuntime` 纳入 exact H handle；initial prepublish；唯一 resume orchestration API |
| Modify | `remote/CloudTaskServiceExecutionContext.java` | existing-state + retained initial metadata constructor |
| Modify | `remote/CloudTaskRetainedActionState.java` | same taskRun state/handle 跨 revision ACTIVE invocation checks |
| Modify | `remote/CloudTaskRunActionLedger.java` | A-approved ledger 上仅 ACTIVE plan/dispatch evidence；Observation 冻结 |
| Modify | `remote/CloudTaskServicePort.java` | public API 不变；ACTIVE handle 不重复冻结 old context |
| Modify | `remote/CloudTaskRunCommandExecutor.java` | current gate、ACTIVE plan、presence matrix、exact redelivery |
| Modify | `remote/RemoteGameCommandBroker.java` | observationMode=null ACTIVE exact-presence probe；A observer 分支冻结 |

明确 **0 Modify**：`CloudGameContextStateOwner.java`、H 报告/handle internals、A 其余双仓文件、execution gate、coordinator、
RemoteGameClientPort、wire DTO、H/J/I/B 写集、host/server/endpoint/poller、DHXY Java/Maven/resources/tests。若实现发现 plain attach+
publish 之外仍需 H callback/API，视为写集冲突，必须先回本报告并保持 Java 冻结，不能直接扩大。

### 8. 双门冻结、自审与结论

- P0=0，P1=0，P2=0。唯一 P1 通过 exact generation permit + H handle-in-runtime + preallocation + unconditional publish 闭合；
  不再存在“先 H 后普通 CAS”。
- 无第二 State、无 H rollback/补偿、无 public raw handle/rebind、无 retry/TTL/takeover；self-CAS 仍仅 slot-generation 线性化，
  coordinator/port/H gates 均保留。
- 自审不构成批准。K 父级 `DESIGN APPROVED` 与 A 父级 `Implementation APPROVED` 仍是 Java 双门；A 当前 deadline P1
  BLOCKED，所以即使 K 设计先通过也不得实施。
- 双门满足后仍只实施 revised 1 New + 7 Modify；不新增/恢复测试，不启动 application/server/host/Task/poller/UI/capture/
  OCR/input，构建门以父级最新指令为准。

**无已批准业务差异；按基线等价迁移。** Design Repair #3 到此停止，等待父级设计复审。

## Parent Design Review #4 - DESIGN APPROVED - 2026-07-12

### 审查结论

父级复核了 Repair #3、当前 `CloudGameContextStateOwner.activateResumed` 源码及 slot/assembly 的既有合同。
结论为 **DESIGN APPROVED，P0/P1/P2=0**。上轮 H activation 与 slot publish 可裂开的唯一 P1 已被
assembly-owned transition lock、exact generation handle、H handle-in-runtime、全量 preallocation 和 unconditional publish
闭合。

### 源码依据与实施硬门

- H `activateResumed` 在 `executionLock` 内先完成 final typed current gate、entry/current handle/zero projection、generation
  overflow 与新 handle 构造，最后才连续写三个 owner 字段并直接返回；interrupt/typed gate/overflow/constructor 异常都发生在
  H owner 写入前。因而 H 正常返回可作为唯一 commit fact，不需要修改 H owner、补偿或第二 State。
- slot 的 `transitionLock` 在 H 前锁定 owner/nonce/generation/exact active-runtime reference；所有 context、port、metadata、
  generation 与 runtime 对象均须在 H 前构造/验证。H 返回后只允许 direct plain attach + `AtomicReference.set` volatile publish；
  此段不得再调用 null-check、gate、CAS、allocation、logging、callback、interrupt check 或其它可能抛出的 helper。
- `TaskServiceRuntime.stateActivationHandle` 只能在不可达的 prepared runtime 上由 assembly nestmate 直接写一次；published
  runtime 必须 non-null handle，发布后不可变。不得提供 package-wide raw setter/rebind。
- resume 与 terminal close 共用 transition lock；`current()` 保持 lock-free typed gate + self-CAS。terminal close 必须保留
  exact terminal typed evidence，并使 activation owner 能以最新 generation/runtime 中的 H handle 完成或重试 H release；不得
  从 closed slot 重新 mint runtime。
- A 的 W0 observer Repair #1 已在最新父级 Review #2 获 `APPROVED，P0/P1/P2=0`，因此 K 的 A-first 双门现在满足。
  K 可按 revised **1 New + 7 Modify** 实施，但必须基于 A 已批准的 ledger/broker 源码，Observation 分支、combined quota、
  mode conflict 与 no-renewal 全部冻结。

### 实施与验收

同一 Worker K 现在实施报告第 1194-1211 行写集；不得修改 H owner、DHXY、host/server/endpoint/poller/tests。完成后追加
`Internal Worker K - Implementation #1`，列精确文件/关键行、A overlap 保留证据与 P0/P1/P2 自审。父级随后源码复审并运行
fresh Cloud `mvn -q clean package`；自审不算批准。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker K - Implementation #1 - 2026-07-12

依据 `Parent Design Review #4 - DESIGN APPROVED` 实施；开始前已确认 A 固定报告的
`Parent W0 Implementation Review #2 - APPROVED`，双门满足。实施直接基于 A-approved current source：
`CloudTaskRunActionLedger` 前置 SHA-256=`45D2A26D704790537DA979C29862CA04238434AE93CC42C5C8C90D20554F138B`，
`RemoteGameCommandBroker` 前置 SHA-256=`E901E4E97C8493C31FC8F1F9F057879E78BE56C75D0202682597C7E1D92E0CD9`；
后者已包含 A observer deadline Repair #1。未使用 pre-A 文件或整文件覆盖。

### 1. Exact 写集与关键行

本轮严格为批准的 Cloud **1 New + 7 Modify**：

| 动作 | 文件 | 关键行与实现 |
|---|---|---|
| New | `remote/CloudTaskRunCurrentContextSlot.java` | `current()` L76；exact generation permit L120；全量 pre-H prepare L152；exact terminal close L203；opaque generation handle L320；stable full key L447 |
| Modify | `remote/CloudTaskRunAuthorityAssembly.java` | initial activation bundle L121；H initial 后仅 attach+publish L152-154；唯一 resume API L166；H resume 后仅 attach+publish L224-227；terminal close + 可重试 H release L240；H handle-in-runtime L278-329 |
| Modify | `remote/CloudTaskServiceExecutionContext.java` | initial constructor L27；existing-state constructor L49；same state reuse validation + new revision-matching port L68-69 |
| Modify | `remote/CloudTaskRetainedActionState.java` | same authority/stable-key `requireReusableBy` L40；new port context gate + same handle invoke L95；existing action address 跨 revision 返回同 handle L122；verified NOT_EXECUTED renewal 权限不变 L151；stable key L251 |
| Modify | `remote/CloudTaskRunActionLedger.java` | ACTIVE-only invocation plan L184；exact broker-entry marker L218；marker-required real outcome L271；renew 后 dispatch evidence reset L370；ACTIVE dispatch enum L687；Observation identity 仍独立 L702 |
| Modify | `remote/CloudTaskServicePort.java` | public 三方法签名不变；三类 opaque handle L104/L114/L124；handle 只保留 owner/record/identity/operation，不再冻结第二份 old context L133 |
| Modify | `remote/CloudTaskRunCommandExecutor.java` | WINDOW_FACT/CAPTURE/INPUT 各先 current ACTIVE gate，再 typed plan L51/L95/L144；bound payload/timeout exact 比较；presence matrix L181 |
| Modify | `remote/RemoteGameCommandBroker.java` | read-only ACTIVE exact-presence probe L142；`observationMode == null` 结构门；`EXACT_MATCH/MISSING/IDENTITY_OR_DIGEST_CONFLICT` typed enum L162 |

最终文件 SHA-256：

```text
CloudTaskRunCurrentContextSlot.java    EABBF847F3508690D2F4AB7B617E2786C6FD2A2D50FFA1CF486F734269B61A39
CloudTaskRunAuthorityAssembly.java     26BCFBEDC0ACA658A088AC2170F0D3A332B304CAFCE73579B6E171880F296F26
CloudTaskServiceExecutionContext.java  20C6B387E5E52B2D36431D052F27D26273AEE938434C0FF2FB878C1959995383
CloudTaskRetainedActionState.java      E75251EAB2D7CA36484D53E33AE66E01E9452FDCCA8259900154276659815EFA
CloudTaskRunActionLedger.java          6E1D10CDCA12FE463E4ACE0D01468223942B1BA11B5E64CF3323138ACEEF2F36
CloudTaskServicePort.java              718A9365BEDE274CEFC4262C94E7DE34409B7CBFFCC02C9F5CD4635D604F232E
CloudTaskRunCommandExecutor.java       77A3D436B704E7D0E98570CF560682AD6C5880D6953C93D88612FBE4D8A76164
RemoteGameCommandBroker.java           233AB76C61A12470E79EEED7C670A6F6D974B14E1703437B2EDDBBDDD66F40D9
```

### 2. Slot/H generation 闭合证据

- `TaskServiceRuntime` 为 assembly nested package-private final class，保留 exact authority、service context、task context、
  same retained state、initial metadata 与 H `StateActivationHandle`。H 字段无 setter/rebind；源码仅有 initial L153 与 resume
  L226 两个 enclosing-assembly direct write。两者都发生在 prepared runtime 尚未 publish 时。
- initial 路径在 H 前完成 context/state/port/runtime/slot/generation handle/activation bundle 全部构造；
  `activateInitial` 正常返回后源码连续只有 `runtime.stateActivationHandle = initialStateHandle` 与
  `publication.set(preparedInitialState)`，然后返回预分配 bundle。
- resume 在 `transitionLock.lockInterruptibly` 后 exact 校验 slot owner/nonce/generation/active-runtime reference；新 context、
  matching port、runtime、next `ActiveRuntime`、next generation handle、`Math.incrementExact` 与所有 reference/stable-key/typed
  gate 均在 H 前完成。任一失败/interrupt 只 unlock，slot/H 保持旧 generation。
- `activateResumed` 正常返回到 publish 之间源码连续只有
  `nextRuntime.stateActivationHandle = nextStateHandle` 与 `publication.set(preparedNextState)`；没有 null-check/helper/log/CAS/
  allocation/callback/interrupt check。finally 只在 publish 后 unlock。不存在先 H 后普通 CAS、补偿回滚或第二份 State。
- `current()` lock-free：captured context 先 typed checkpoint，再 self-CAS；self-CAS 只证明 slot generation 在 gate 后未变，
  不是 coordinator lifecycle lease。Service state、executor、request build、broker 与 DHXY 既有后续门均未删除。
- terminal close 与 resume 共用 transition lock；只接受 coordinator exact current STOPPED/COMPLETED binding。closed state 保存
  最新 whole runtime/H handle 与 exact terminal binding；H release 若 interrupt，可用同一 binding/handle 重试，slot 永久 closed。

### 3. same-taskRun retained action 与 broker-miss 证据

- initial 只创建一次 `CloudTaskRetainedActionState`；resume existing-state constructor 复用同一 reference，并复用 initial
  `CloudTaskServiceMetadata` 同一对象。slot prepare 再以 reference equality 强制 authority/state/metadata 不漂移；每 revision
  只新建 context、matching port 与 facade。
- retained state 的 records/ActionRecord/opaque handle 跨 revision 同对象；handle 不再保存第二份 frozen context。old port 仍在
  `invoke` 的 exact context gate fail stale；new port 可用同一 handle。ACTIVE ledger 仅在 identity unbound、无 bytes/outcome 且
  revision strict newer 时推进其 context，wire requestId/actionId/captureId/attempt 不变；bound request/context/bytes 不改。
- first bind 原子标记 `BOUND_NOT_ENTERED_BROKER`；真实 broker 调用前必须变为 `BROKER_ENTRY_MARKED`；`recordOutcome` 只接受
  latter。renewal 仍只能由真实 broker typed `NOT_EXECUTED -> renewAfterNotExecuted` 开门，UNKNOWN/未记录/其它状态无新通路。
- bound redelivery matrix：`BROKER_ENTRY_MARKED+EXACT_MATCH` 读取 broker retained resolution；
  `BOUND_NOT_ENTERED_BROKER+MISSING` 才先 mark 再进入同 executor 持有的同 broker，让真实 outcome 决定能否 renewal；
  `BROKER_ENTRY_MARKED+MISSING`、`BOUND_NOT_ENTERED+EXACT_MATCH` 与 digest/identity conflict 均抛 structural
  `IllegalStateException`，不进 broker、不记录 NOT_EXECUTED、不 renewal，已有 UNKNOWN/未记录状态原样保留。

### 4. A overlap 冻结证据

实施前后对 A-owned fragment 做 UTF-8 SHA-256 指纹，以下全部逐字相同：

```text
ledger observation fields       E561EA6DC6B661BE3C8B0AE5611D5FFCD91C82C11E7F15BB836BD3E7F7715A4E
ledger observation methods      C4CBDCE065B442E78F55103426CC0D02349877FEF8280B45080BE62860A569C2
ledger combined retainedTotal   ABAFAE5494981CFF18673CE400D03555A8F22B80B3ACC603EC892652D8D5CE44
ledger observation types        1F5AFE47405EFCB78E553495CF9EF7E211901E126A134001400090A4791FB97F
broker observation dispatch     FCF095815A515BBACE4ED9EBE07AD91224ED8AABB66CDF3F6BD724EA4AA65C56
broker observation enqueue      24A5247A624D4695062D9A744D1A7DE6E57E87356346814B185F93D56D114E9B
broker observer deadline repair 78B7026C7730F22F873D0018849C80B1AC12969F7C4E3B05310EA4D1626AAD27
```

因此 `ObservationActionIdentity`/observation maps/methods、双向 mode conflict、ACTIVE+Observation combined 10,000 cap、
Observation no-renewal、broker PAUSED enqueue/dispatch 双门及 wall-clock deadline Repair #1 均未改变。K 新 probe 明确拒收
任何 `observationMode != null` request。

### 5. 冻结范围、验证与自审

- `CloudGameContextStateOwner.java` **0 Modify**，SHA-256 仍为
  `8D5BBEFAC713DA2AD8FFF1C95E4A79701DF184EFFC8EA022FA4228B15E584DBF`；
  `CloudTaskRunExecutionGate.java` 亦未改，SHA-256=`0C55C9F698672AB45C9B01921ECEED6148165C42B62BDD2A7B783C8E4F8383A4`。
- 零 DHXY、Maven/resources/tests、host/server/endpoint/poller 改动；未回滚/覆盖/提交任何 dirty/untracked；未启动
  application/server/host/Task/poller/UI/capture/OCR/input，未发送机械动作。
- 按父级 Review #4，fresh Cloud `mvn -q clean package` 由父级源码复审后执行；K 本轮未运行 Maven/测试，不把旧构建当证据。
- 只读源码断言通过：slot public 业务面只有 `current()`；无普通 `install`；H handle direct write 恰为两处；两段 post-H
  attach+publish 形状 exact；无 Observation renewal overload；combined quota 仍为两 map 合计；8 个文件无行尾空白。
- 自审：**P0=0/P1=0/P2=0**。P1 重点复核 H/slot 无可失败 split、same state/metadata 跨 revision 与 broker-miss renewal
  authority；P2 重点复核 self-CAS 非 lifecycle lease、terminal retry、Observation 冻结与 public/package 可见性。该结论仅为
  Worker K 自审，**不构成 Implementation APPROVED**。

**无已批准业务差异；按基线等价迁移。** Implementation #1 到此停止，等待父级源码复审与 fresh package 门。

## Parent Implementation Review #1 - BLOCKED - 2026-07-13

父级已复核 K 的 1 New + 7 Modify 当前源码。initial/resume 的 H activation 后只剩 plain handle attach + volatile publish、
same retained action state/metadata 跨 revision 复用、broker-miss fail-closed matrix 与 Observation 冻结均成立；但 terminal
close 仍有一个 generation 漏洞。结论：**BLOCKED，P0=0，P1=1，P2=0**。暂不运行 fresh package，先由同一 K 做单点返修。

### P1-1：terminal close 只要求 revision 变大，旧 slot generation 可跨过 pause/resume 直接关闭

- 证据：`CloudTaskRunCurrentContextSlot.requireExactCurrentTerminal` 当前仅检查
  `terminal.runRevision() <= context.getRunRevision()`；只要 coordinator 的 terminal binding 是当前 exact binding，任意更大 revision
  都会通过。coordinator 的每次 pause/resume/stop/complete 都精确 `Math.incrementExact(current.runRevision())`，所以从一个确实
  current 的 ACTIVE slot 到 terminal 只允许**下一 revision**，不能跳过多个 lifecycle revision。
- 影响：若 activation adapter 漏掉一次 resume publish、持有旧 slot generation，或 resume 失败后 coordinator 又进入 terminal，
  revision 1 的 slot 可接受 revision 4/6 terminal，随后释放旧 H handle并把 stale context/port/state 冒充为最新 generation 收口，
  破坏 K 本切片要建立的 matching runtime invariant。
- 精确返修：只修改
  `CloudTaskRunCurrentContextSlot.requireExactCurrentTerminal`，要求
  `terminal.runRevision() == Math.incrementExact(context.getRunRevision())`；保留 exact scope/taskRun/taskType/window、STOPPED
  stopEpoch、COMPLETED stopEpoch、coordinator exact-current 和幂等 retry 全部现状。不得修改 H owner、ledger/broker/executor/
  Service port/DHXY/tests。追加 `Internal Worker K - Implementation Repair #1` 后，父级再源码复审并运行 fresh Cloud
  `mvn -q clean package`。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker K - Implementation Repair #1 - 2026-07-13

依据 `Parent Implementation Review #1 - BLOCKED` 的唯一 P1 做精确单点返修。

### 1. 源码变更

- 仅修改 `remote/CloudTaskRunCurrentContextSlot.java` 的
  `requireExactCurrentTerminal`（当前 L235-270）。terminal revision 判定由“任意更大”收紧为：

```java
terminal.runRevision()
        == Math.incrementExact(context.getRunRevision())
```

  实际 fail-closed 条件为对应的 `!=`。因此 active slot generation 只能接受 coordinator 单步产生的 exact next terminal
  revision；跨过 pause/resume 的更高 terminal revision 在任何 close publish 前拒绝。`Math.incrementExact` 的 overflow 也发生在
  terminal state 写入前。
- exact scope/taskRun/taskType/window、STOPPED next stopEpoch、COMPLETED retained stopEpoch、coordinator exact-current record
  equality、terminal close 幂等 retry 与 transition lock 顺序全部保持原样。

### 2. 精确范围与冻结证据

- slot 修改前 SHA-256=`EABBF847F3508690D2F4AB7B617E2786C6FD2A2D50FFA1CF486F734269B61A39`，修改后
  SHA-256=`FC597BA1786AFD70E28327884C033CC1DCDD4A11223F02FE53662929B6FE4A55`。只读反向替换新两行后，SHA-256 精确恢复为
  修改前值，证明源码仅有该条件替换。
- 其余 K 写集哈希均与 Implementation #1 一致：assembly=`26BCFBED...F296F26`、service context=`20C6B387...995383`、
  retained state=`E75251EA...15EFA`、ledger=`6E1D10CD...F2F36`、port=`718A9365...232E`、executor=`77A3D436...76164`、
  broker=`233AB76C...40D9`。
- H `CloudGameContextStateOwner.java` 仍为 `8D5BBEFA...DBF`，execution gate 仍为 `0C55C9F6...83A4`；H attach/publish、
  ACTIVE ledger/broker presence matrix、Observation/no-renewal/combined quota/mode conflict、Service port 与 DHXY 全部冻结。
- 零 Maven/resources/tests、host/server/endpoint/poller 改动；未运行 Maven/测试，未启动任何运行面，未回滚、覆盖或提交并行改动。

### 3. 自审

- **P0=0/P1=0/P2=0**：唯一 terminal revision P1 已按父级指定条件闭合；没有扩大 API、写集或业务语义。
- 该结论仅为 Worker K 自审，**不构成 Implementation APPROVED**。等待父级源码复审及其 fresh Cloud package 门。

**无已批准业务差异；按基线等价迁移。**

## Parent Implementation Review #2 - APPROVED - 2026-07-13

父级复核 `CloudTaskRunCurrentContextSlot.requireExactCurrentTerminal` 的最终源码，确认 terminal fence 现为
`terminal.runRevision() == Math.incrementExact(context.getRunRevision())`。旧 ACTIVE slot 无法再接受跨过
pause/resume/reconfirm 后的更高 terminal revision；scope/taskRun/taskType/window、STOPPED next stopEpoch、COMPLETED
retained stopEpoch、coordinator exact-current equality 和 close 幂等 retry 均保持。

父级在 A/K 写入稳定后重新执行 Cloud `mvn -q clean package`：exit 0，4 suites / 21 tests / 0 failures / 0 errors /
0 skipped。结论：**Implementation APPROVED，P0/P1/P2=0**。K 切片收口，Worker K 可停止；释放的内部槽应恢复已获
DESIGN APPROVED 的 Worker J task-turn implementation，不再给 K 派新改动。

**无已批准业务差异；按基线等价迁移。**
