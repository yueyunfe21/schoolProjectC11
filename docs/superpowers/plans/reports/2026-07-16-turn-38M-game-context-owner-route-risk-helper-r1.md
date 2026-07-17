# CR271 TURN-38M GameContext owner 路线风险 helper PRECHECK R1

## 1. 角色、范围与结论边界

- 日期：2026-07-16。
- 角色：CR271 Internal 非实现、非 reviewer、非父级的独立路线风险 helper。
- 唯一问题：`CloudGameContextStateOwner` 是在 TURN-38C 改成 turn-native owner，还是保持旧文件不动、由 TURN-40B 的 `CloudTurnTaskRuntime` 直接承担 replacement，并把旧文件留给 TURN-44A。
- 本文只记录 `PRECHECK`、源码证据、两条路线的 exact consumer/write-set、DAG 与非绑定建议；不替父级冻结分类，不修改原卡、权威计划或 Java。
- 本轮没有运行 Maven、JUnit、application、server、runtime、Task、UI、capture 或 input，也没有执行 Git mutation。

## 2. 已读取权威材料与工作树快照

本轮完整读取或复核：

1. `D:/mavenProject/DHXY/AGENTS.md`。
2. `D:/mavenProject/DHXY/docs/DHXY_CONTEXT.md`。
3. `D:/mavenProject/DHXY/docs/ACTIVE_WORK.md` 顶部 CR271 当前区段。
4. `D:/mavenProject/DHXY/docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节。
5. `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-38M-authority-classification-preflight-helper.md`。
6. `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-40B-readiness-preflight-helper.md`。
7. Cloud 当前 `CloudGameContextStateOwner.java`、`GameContext.java`、`TaskExecutionContext.java`、`CloudTaskRunAuthorityAssembly.java`，以及直接编译闭包 `CloudTaskRunRetainedLifecycleActivationAdapter.java`、`CloudTaskRunCurrentContextSlot.java`、`RemoteTaskRunRoutes.java`、`CloudServiceConfiguration.java`、`CloudServiceHost.java`、`CloudBrainServer.java`。
8. 当前四个 real Task、`BaseTaskTemplate` 与所有 production mutable `GameContext` consumer 的引用，以及已存在的异步边界和 `stop()` 路径。

只读快照：

| 仓库 | branch | HEAD | 状态 |
|---|---|---|---|
| `D:/mavenProject/DHXY` | `thin-client-design` | `0114604e1ff5f15491d2910959c45252e893d04f` | 已有大量 modified/untracked；保持不动 |
| `D:/mavenProject/dhxy-cloud-brain` | `navigation-migration` | `3b988caa010254973e03342272e6d1d6a9685b01` | 已有大量 modified/untracked；保持不动 |

当前四个核心文件仍是上一轮记录的 untracked 快照：

| 文件 | 行数 | SHA-256 |
|---|---:|---|
| `remote/CloudGameContextStateOwner.java` | 487 | `8D5BBEFAC713DA2AD8FFF1C95E4A79701DF184EFFC8EA022FA4228B15E584DBF` |
| `core/GameContext.java` | 204 | `26B4A9A7963E4E4159D835CD3AF8E3A9EDEB2227A744F7E5C07E0E7877DAEEC9` |
| `runner/context/TaskExecutionContext.java` | 491 | `6D4E4A20A6FB4B6DBA6A59CB45E95DD39C78A0415B9B2A650D75F9704151D003` |
| `remote/CloudTaskRunAuthorityAssembly.java` | 472 | `A22AF1D212B0A1734FED546D44B413FDE2226FFE4ED9E88865EA8666A185D0E1` |

## 3. 权威 DAG 与协议约束

- 权威计划 `:1159-1163`：`38A -> 38M -> 38C -> 39 -> 40B`，且 40B 还依赖 40A、13H；40C 在 40B 后激活。
- 权威计划 `:1327-1334`：38M 先冻结五个 authority-bound state 的分类；38C 只实施保留重接行，删除行保持不动至 44A；父级必须先冻结全部新 context consumer 与 exact write set。
- 权威计划 `:1355-1362`：40B 只创建五个 runtime 文件，registry 只保留 current window runtime 与 last accepted `startRequestId/ack`，不持久化、不加 TTL、不自动 retry。
- 权威计划 `:1497`：`STATE` 必须 tenant/user 私有、device/window exact，pause/resume 保持同一状态，terminal/restart 释放，不加 TTL。
- 权威计划 `:1650,1660,1664`：38C 每个保留重接行须有独立 `*TurnStateTest`；40B 只有两个点名测试；38M 本身是零测试分类 pass。
- 协议和计划 `:96,1065` 禁止另建 session、owner、ledger、durable workflow、business TTL 或自动业务 retry。

因此，路线比较不能只问“哪一处能创建 `newState()`”；还必须同时满足当前编译闭包、未来真实投影入口、最小生命周期和禁止新增 authority 的约束。

## 4. 当前源码事实

### 4.1 旧 owner 是完整旧 authority，不是窄 State wrapper

`CloudGameContextStateOwner.java` 当前：

- `:27` 是 package-private `final class`，位于 `remote` 包；未来 `turn.runtime.CloudTurnTaskRuntime` 不能直接构造或引用。
- `:30-39` 持有全局/owner 配额、`RemoteTaskRunCoordinator`、entry map 和 owner usage map。
- `:59-103` 的 initial activation 要求 old first ACTIVE revision，并以 old scope/taskRun/taskType/window/identityEpoch/stopEpoch 建 key。
- `:115-159` 的 resume 依赖 old revision advance，换发 `StateActivationHandle`。
- `:173-220` 的 projection 通过 execution lock、handle generation 和 `activeProjectionCount` 后才调用 `GameContext.callWithState(...)`。
- `:231-278` 的 terminal release 依赖 exact old terminal binding，进入 `RELEASE_PENDING`，并允许相同 capability 以后重试释放。

这些字段不是 turn pause/stop metadata 的同义替换。把类名或参数改成 windowId 不能消除其中的 old session/revision/handle/release authority。

### 4.2 旧图已经编译耦合，但业务 projection 没有 production caller

直接生产引用闭包：

1. `CloudTaskRunAuthorityAssembly.java:48,73,131,237-239,269,331-366,405,463-469`：构造 owner，initial/resume/release，并把 activation handle 放进旧 runtime snapshot。
2. `CloudTaskRunRetainedLifecycleActivationAdapter.java:248-260`：terminal API 暴露 owner 的 result type，并调用 assembly 的 release path。
3. `CloudTaskRunCurrentContextSlot.java:69,221`：通过 nested `TaskServiceRuntime.hasStateActivationHandle()` 校验 initial/resume publication 时点。
4. `RemoteTaskRunRoutes.java:49-55` 与 `CloudBrainServer.java:86-90`：沿旧 route 构造 assembly，并单独 `new GameContext()`。

但全 production 搜索同时证明：

- `CloudTaskRunRetainedLifecycleActivationAdapter.activateInitial(...)` 没有外部生产调用者。
- `CloudGameContextStateOwner.callWithState(...)` 除自身定义外没有生产调用者。
- 所以真实状态是“旧图构造并持有 dormant State/handle”，不是“real Task stack 已投影到该 State”。

### 4.3 `GameContext.defaultState` 是当前真实回落路径

`GameContext.java:18-19` 是一个 singleton `defaultState` 加 `ThreadLocal.withInitial(() -> defaultState)`；只有 `:127-143` 的显式 `callWithState/runWithState` 才会临时绑定独立 State。未绑定线程会共享同一个默认对象。

当前 mutable behavior consumer 精确为：

- Task：`AutoBattleTask.java`、`WubeiTask.java`、`XiuluoTaskV2.java`、`FiveRingTaskV2.java`、`BaseTaskTemplate.java`。
- Service：`AutoCombatPanelService.java`、`AutoCombatService.java`、`BattleRadarService.java`、`NavigationService.java`、`PlayerStateService.java`、`TaskMaintenanceService.java`。

`TaskPauseResumeFingerprint` 只引用 enum，`TaskMaintenanceRequest` 只在 JavaDoc 提到类型，不是 mutable State consumer。

### 4.4 turn-native context 已经支持“同 runtime 暂停”，不需要 revision resume

`TaskExecutionContext.turnNative(...)` 保存 fixed host scope、exact device/window invocation、初始 metadata、Task metadata、诊断 taskRunId 与 bound `TurnGameClient`。`:385-410` 在同一 context 内轮询最新 exact metadata；pause 以 250ms checkpoint wait 保留当前调用栈，resume 只是同一循环继续，不创建 revision、handle 或新 context。

把 `GameContext.State` handle 塞进 `TaskExecutionContext` 会破坏其“powerless immutable context”边界，也会重写已经由 38A 占有的文件；当前证据不支持把它列为任一路线的必要 consumer。

### 4.5 当前 Spring 图尚未提供共享 `GameContext`

- `CloudServiceConfiguration.java:25-38` 只扫描 service 和 turn client；没有扫描 `com.bot.dhxy.core`，也没有显式 `GameContext` bean。
- `CloudServiceHost.java:39-65` 当前只注册 scope、storage、command port、template catalog。
- `CloudBrainServer.java:86-90` 给 old remote route 单独 `new GameContext()`；它不是未来 CloudServiceHost 中 Task/Service 使用的共享 bean。

所以无论选 A 或 B，40C 都必须保证 runtime 投影者与所有 Task/Service 注入的是同一个 host-local `GameContext` 实例。复用 server 里 old route 的对象或在 factory 临时 `new GameContext()` 都不能形成正确投影。

## 5. 路线 A PRECHECK：38C 将现文件改为 turn-native owner

### 5.1 新 consumer 及 API 可见性

若选择 A，唯一合理的新 production invocation consumer 应是未来的：

- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntime.java`

该 runtime 应在 worker Task stack 外层调用 owner 的投影 API。Factory 只 materialize prototype，不应成为 State owner；registry 只管理 current runtime，不应再建一份 State map；`TaskExecutionContext` 与 business Task/Service 均保持透明间接 consumer。

由于 target 当前 package-private 且位于 `remote` 包，38C 至少必须把类和必要 constructor/projection API 变成 public。新增一个 adapter/interface 文件来绕过可见性不在 38C 或 40B 冻结写集中；reflection/string lookup 也不是可接受 consumer seam。

### 5.2 A 的最小“保编译”exact write-set

在当前 DAG 下，A 若不提前拆 old SCC，唯一能保住 Cloud 编译的最小集合是双 contract 过渡：

**TURN-38C production**

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudGameContextStateOwner.java`

**TURN-38C test，须由父级冻结 exact 名称；本 helper 建议候选**

2. `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudGameContextStateTurnStateTest.java`

这个最小集合必须暂时保留现有 constructor、`StateActivationHandle`、initial/resume/release/result API，使 assembly/adapter/slot 继续编译；同时另加 public turn-native API，供 40B 后续消费。

**TURN-40B downstream consumer**

3. `.../turn/runtime/CloudTurnTaskRuntime.java`：唯一直接调用新 owner API 的 production consumer。
4. `.../turn/runtime/CloudTurnTaskRuntimeContractTest.java`：验证真实 queue 与 State integration。

40B 的其余四个 production 文件和 factory test 仍属于整张 40B 卡，但不是 State owner 的直接 consumer。

**TURN-40C wiring consumer**

5. `.../host/CloudTurnRuntimeConfiguration.java`：提供同一个 host-local `GameContext` 给 Task/Service/runtime owner。
6. `.../host/CloudServiceHost.java`：加载该配置并维持 host lifecycle。

### 5.3 A 的 clean rewire 会扩成 old SCC 写集

若 38C 不接受双 contract，而是立即删除 old constructor/handle/revision/release API，则至少必须同卡修改：

1. `CloudGameContextStateOwner.java`
2. `CloudTaskRunAuthorityAssembly.java`
3. `CloudTaskRunRetainedLifecycleActivationAdapter.java`
4. `CloudTaskRunCurrentContextSlot.java`
5. `RemoteTaskRunRoutes.java`
6. `CloudBrainServer.java`

前三个是直接 type/method 编译闭包，slot 校验 state handle publication，routes/server 是 assembly 的 `GameContext` constructor chain。其测试编译闭包也必须重扫；当前 `LeftTopStatusTurnContractTest.java:603` 还反射 old activation method。

这会提前改写原计划留给 44A 的 old authority SCC，并与“38C 删除行和 old SCC 保持不动”的顺序相冲突。父级若选择这一变体，需要先显式修订计划和写集，不能把它描述为单文件 38C。

### 5.4 A 的 pause/resume/terminal 风险

- turn pause 必须保留同一个 owner object、同一个 State、同一个 runtime 与同一个 Task/context；不得调用当前 `activateResumed(...)`，不得生成 revision 或 replacement handle。
- 当前 owner 的 projection lock 若包住整次 `task.execute(...)`，pause 期间也一直持锁。控制线程或 terminal 线程再尝试投影/释放会等待 Task 返回；它不能作为唤醒 Task 的独立控制 authority。
- 当前 terminal API 需要 old STOPPED/COMPLETED binding、stopEpoch 和 runRevision；turn runtime 没有这些对象。新 path 必须只接受 runtime 自己的 exact terminal transition。
- 保留 `RELEASE_PENDING` 与“相同 capability 可重试”会继续维护一套 release lifecycle。它虽没有定时自动执行，但不属于最小 runtime cleanup。
- 若 owner 是 singleton window map，会与 `CloudTurnTaskRegistry` 的 current runtime map 重复；若每 runtime 各建 owner，则该类只是 runtime 外再包一层 State capability。

### 5.5 A 到 44A 的必需清理

若走双 contract，44M45M/44A manifest 必须同时冻结：

- 删除 assembly、retained adapter、current-context slot、old routes 等旧 SCC 时，重改 `CloudGameContextStateOwner.java`，移除 old constructor、old key/map/quota、activation handle、revision 与 terminal binding API。
- 明确该文件本体是继续保留还是迁出 `remote` 包；不能被 broad old-remote delete 误删，也不能永久保留半个 dormant authority。

因此 A 至少让同一文件经历 38C 和 44A 两次语义修改，并在 38C 到 44A 之间长期承载 dual authority surface。

## 6. 路线 B PRECHECK：40B runtime 直接拥有 replacement State，旧文件留 44A

### 6.1 推荐评估的 B 形状

这里的“runtime owner”应严格解释为：`CloudTurnTaskRuntime` 是已经获计划授权的 current in-memory runtime，它直接持有一个 `GameContext.State` 字段；不是再创建一个名为 owner/handle/session 的协作者。

- `CloudTurnTaskFactory` 不创建、不缓存、不投影 State。
- `CloudTurnTaskRegistry` 不建第二个 State map；runtime 已由 exact `deviceId + windowId` key 唯一索引。
- `TaskExecutionContext` 不持有 State/handle。
- 每个 accepted runtime 从与 Task/Service 相同的 `GameContext` 调用一次 `newState()`。
- worker 在真实 Task queue 调用栈外建立唯一 projection；pause/resume 保持该调用栈和 State；terminal 后丢弃 State 引用。

### 6.2 B 的 exact write-set

**TURN-38C**

- 对 `CloudGameContextStateOwner.java` 零写入；父级分类表把它指向 44A。

**TURN-40B production，保持权威计划冻结的五个新文件，不扩写集**

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskFactory.java`
2. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntime.java`
3. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRegistry.java`
4. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskStartResult.java`
5. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnControlPort.java`

State-specific direct consumer 是 `CloudTurnTaskRuntime`；lifecycle consumer 是 `CloudTurnTaskRegistry` 与 `CloudTurnControlPort`。Factory 和 start result 不直接接触 State。

**TURN-40B tests，保持点名的两个文件**

6. `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntimeContractTest.java`
7. `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskFactoryAllowlistTest.java`

State isolation、pause continuity、terminal/new-start replacement 与 defaultState sentinel 均应进入 runtime contract test；不需要额外 38C State test，因为该旧文件不是保留重接行。

**TURN-40C production，使用计划已有激活写集**

8. `.../host/CloudTurnRuntimeConfiguration.java`：显式注册一个 host-local `GameContext` 并向 Task providers/runtime 提供同一实例。
9. `.../host/CloudServiceHost.java`：加载 runtime configuration。
10. `.../turn/CloudTurnHttpHandler.java`、`.../turn/CloudTurnRoutes.java`、`CloudBrainServer.java`、`CloudBrainApplication.java`：只负责 ingress、共享 exchange、host/runtime close order，不成为 State owner。

对应真实接线断言仍归计划点名的 `host/CloudTurnActivationContractTest.java`。

**TURN-44A**

- `CloudGameContextStateOwner.java` 与其 old assembly/adapter/slot SCC 一并删除；40B runtime 对它保持零引用。

### 6.3 B 的 pause/resume/terminal 形状

1. Registry 接纳 exact window/start 后，runtime 创建一次 State；ack 只在 runtime 原子安装且 worker 成功启动后产生。
2. Worker 用该 State 包住真实 queue/Task invocation。Pause 在 `TaskExecutionContext` checkpoint 内等待，State 仍由同一 worker 投影；resume 不触发 factory、`newState()`、new context 或 generation change。
3. Stop metadata/interrupt 只请求 worker unwind。worker 仍是 State 的唯一业务写线程；不要让 HTTP/control 线程裸调 `activeTask.stop()`。
4. Worker `finally` 的建议顺序：完成 Task 自身 terminal cleanup -> 退出 holder projection -> 退出 `GameContext` projection -> 清 active Task/queue/context 引用 -> 标记 worker terminal -> registry 原子移除 current runtime。
5. 只有 current runtime 指针清除后，同 window 的新 start ID 才能创建新 runtime/new State。Registry 只留下 last accepted ID/ack。
6. Explicit server close 先 signal/interrupt，再等待 worker 走完同一 finally；不以 TTL、后台清扫或自动重启替代 join/cleanup。

这个形状没有额外 acquire/release handle，也不需要 terminal binding、stopEpoch、revision、quota 或 cleanup retry record。

## 7. 两路线共同的 `defaultState` 与线程风险

### 7.1 必须是同一个 `GameContext` bean

投影者使用 A owner 还是 B runtime 都不重要，只要 Task/Service 注入的是另一个 `GameContext` 实例，所有业务读取仍会落到另一个实例的 `defaultState`。40C 必须冻结“每个 fixed-scope host 恰好一个共享 bean”，并禁止 runtime/factory/server 各自 `new GameContext()`。

### 7.2 `GameContext.callWithState` 不支持 nested restore

`GameContext.callWithState/runWithState` finally 直接 `remove()`，不像 `TaskExecutionContextHolder.callWith(...)` 那样恢复 previous value。若外层 runtime projection 内再次 nested projection，内层退出会清掉外层绑定，后续调用悄悄回到 defaultState。

冻结条件应是：一个 Task worker stack 只有一个 GameContext projection layer；A/B 都不得叠 owner wrapper、runtime wrapper和 Task-local wrapper。若未来必须支持 nesting，应另立被父级授权的 `GameContext` API 修改，不可在 38C/40B 顺手改变。

### 7.3 普通 ThreadLocal 不跨异步线程

当前 source 已有 `CompletableFuture` 边界：

- `WubeiTask.java:2048-2053,3000-3015,3095`
- `XiuluoTaskV2.java:3135-3153,3179-3189`
- `TeamReturnService.java:282-286`

本轮检查到的这些具体 async body 当前主要做 tracker/OCR/pure result projection，并会选择性绑定 `TaskExecutionContextHolder` 或旧 window holder；它们没有继承 `GameContext` State。只要任何现有或后续间接调用触达 mutable `GameContext`，就会写共享 defaultState。

父级必须冻结二选一的线程规则：

- 推荐窄规则：GameContext mutable consumer 只能在 runtime worker 线程执行，async body 禁止触达它；或
- 显式 projection 规则：每个获准 async body 在 future 完成前由同一 runtime State 包住，terminal 必须等待，且需处理并发写 State。

后者会扩大 business Task/Service consumer 写集并改变并发审查面；当前证据更支持前者。

### 7.4 `stop()` 是已存在的跨线程陷阱

四个 real Task 的 `stop()` 都会写 `GameContext`：`AutoBattleTask.java:173-176`、`WubeiTask.java:328-330`、`XiuluoTaskV2.java:413-415`、`FiveRingTaskV2.java:322-324,2771-2773`；`BaseTaskTemplate.java:112-116` 也同样写 State。

若 40B control/HTTP 线程直接调用 `activeTask.stop()`：

- 未投影时会污染 defaultState；
- 以 A 当前 execution lock 投影时，可能等到长 Task 退出才获得锁，失去唤醒意义；
- 以 B 同 State 并发投影时，会让非线程安全 State 被 worker/control 同时修改。

因此父级需在 40B brief 明确：stop 的权威信号是 exact metadata + worker interrupt/checkpoint；Task `stop()` 若仍需要调用，应由 worker 在自己投影内执行，或先有单独审查过的线程安全方案。不得让 Worker自行选择调用线程。

### 7.5 两个必要 sentinel

无论路线如何，点名测试至少应证明：

1. 两个 exact window runtime 并发执行时，各自看到不同 State，Task/Service mutation 不交叉。
2. 测试线程预先写入的 `defaultState` sentinel 在 start、pause、resume、stop、terminal 和新 start 后完全不变；worker 退出后 `currentState()` 不残留旧 State。

## 8. 禁止新增 authority 对照

| 检查项 | A：双 contract 保编译 | A：提前 clean rewire | B：runtime 直持 State |
|---|---|---|---|
| 新的独立 owner surface | 是；现类被 public 化并成为 active collaborator | 是；即使去掉 old 字段，仍是 runtime 外的 owner object | 否；只有计划已授权的 current runtime 持资源字段 |
| session 语义 | old `RemoteTaskRunScope` path 保留至 44A；new path 必须禁止 | 可移除，但要求提前拆 old SCC | 无；tenant/user 来自 fixed host，device/window 来自 runtime key |
| ledger-like lifecycle map/handle | old entries/revision/handle/quota 继续存在；new path若复用即冲突 | 可移除，但写集扩张 | 无；registry 只有 current runtime + last ID/ack |
| durable workflow | 当前 owner 本身不落盘，但 dual lifecycle surface 长期存在 | 无 | 无；queue 只属于当前显式 start 的易失 runtime |
| TTL | 当前 owner 无 TTL；不得新增 | 不得新增 | 不得新增 |
| 自动 retry | 当前没有 scheduler，但 terminal API保留显式 release retry 能力 | 不得新增 | 无；Task/action/cleanup 都不自动重启 |
| 与 44A 删除边界 | owner 本体需二次修改并防误删 | old SCC 被迫提前改 | 旧文件可原样随 SCC 删除 |

## 9. DAG 路线比较

### 9.1 A 的真实顺序

`38M 父级冻结 A + public dual API/write-set`
` -> 38C 改 owner + 独立 TurnState test`
` -> 39 收口 active business facade`
` -> 40B runtime 成为第一个真实新 consumer`
` -> 40C 注入同一 GameContext 并激活`
` -> 44M45M 重扫 dual owner/old SCC`
` -> 44A 删除 old SCC 并清掉 owner 的 old half`

38C 完成后到 40B 之前，新 API 只能由测试直接调用，没有 production invocation consumer；old API 又必须为编译继续存在。这是 A 的主要时序债务。

### 9.2 B 的真实顺序

`38M 父级冻结旧 owner 指向 44A`
` -> 38C 对该文件零写入`
` -> 39 收口 active business facade`
` -> 40B runtime 创建/投影/释放唯一 State`
` -> 40C 注入同一 GameContext 并激活`
` -> 44A 删除 untouched old owner/authority SCC`

39 到 40B 之间不存在 production runtime 空洞：Cloud real Task runtime 本来就到 40B 才创建。B 不需要提前交付一个 dormant replacement API。

## 10. 非绑定路线建议

基于当前源码和冻结 DAG，本 helper **倾向路线 B 的“`CloudTurnTaskRuntime` 直接持一个 State”变体**，而不是 factory 新建 owner、registry 新建 State map，或再增加第六个 runtime production 文件。

证据理由：

1. B 完整落在已冻结的 40B 5+2 写集和 40C 激活写集内；A 要么长期双 API，要么提前扩写 old SCC。
2. B 把 State 的创建、pause continuity、terminal release 与唯一 current runtime 生命周期合并，不需要第二套 handle/revision/release authority。
3. B 不需要 public 化 `remote` 包的旧 owner，也不会让 44A 区分同一文件的“新 half/旧 half”。
4. B 能把隔离、defaultState sentinel、terminal/new-start replacement 放进既有 runtime contract test；A 还需要额外 38C 独立测试和 44A 二次清理。
5. 计划禁止新增 owner，而 runtime 对自身资源的直接持有不形成新的可寻址 authority；单独的 owner collaborator 则需要额外解释。

这不是父级分类决定。父级仍可选择 A，但在领取 38C 前至少应明确接受“双 contract 到 44A”或先修订计划以扩大 clean-rewire 写集，不能把两者混写成单文件等价迁移。

## 11. 建议父级冻结清单

1. 明确二选一：A 的现文件 active rewire，或 B 的 old file 留 44A/runtime direct State。
2. 若选 A，冻结 public API、38C 独立 test exact path、old API 的临时存续期，以及 44A 对同文件的二次清理；若要 clean rewire，先修改计划中的 old SCC 写集。
3. 若选 B，明确 State 只在 `CloudTurnTaskRuntime`，factory/registry/context 不另存 owner、handle 或 State map。
4. 40C 注册每 fixed-scope host 恰好一个共享 `GameContext`，Task、Service 与 runtime 使用 object-identical bean。
5. 冻结唯一 projection 边界，禁止 nested projection；退出必须清 ThreadLocal。
6. 冻结 async 线程规则，禁止未投影的 mutable GameContext consumer。
7. 冻结 pause/resume 保留同一 State，零 revision、零 replacement context、零 resume factory call。
8. 冻结 stop 信号、interrupt、可选 `Task.stop()` 的 exact 线程与先后；不得从 control/HTTP 线程裸写 State。
9. 冻结 terminal finally 与 registry removal 顺序；old worker 未解除 projection 前不得接纳同 window 新 runtime。
10. 冻结两个 sentinel：跨 window 隔离与 defaultState 全生命周期不变。
11. 明文禁止 session、ledger、独立 owner、durable queue/history、TTL、自动 Task/action/cleanup retry。
12. 44M45M/44A 对 old owner 的直接编译闭包做完整删除复扫，不制造中间假编译点。

## 12. 本 helper 操作确认

- 只新增本报告。
- 未修改 Java、计划、CR 卡、ACTIVE_WORK、dashboard 或其它报告。
- 未占用任何 Java 写集。
- 未还原、移动、暂存、提交或清理两仓既有 dirty/untracked 内容。
- 未运行任何被禁止的 build/runtime/input 操作。

PRECHECK_COMPLETE
