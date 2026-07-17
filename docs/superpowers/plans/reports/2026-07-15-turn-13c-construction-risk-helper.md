# CR271 TURN-13C construction-risk helper

## 角色与边界

- 角色：TURN-13C 非绑定 construction-risk helper，不是 reviewer。
- 本轮只依据权威计划第 14-19 节、协议规格和用户点名的当前 Cloud 源码做静态预检。
- 未运行 Git、Maven、测试、runtime；未修改 Java/POM/Git。
- 本报告不作 reviewer 审查结论。

## 冻结问题的直接答案

**NEEDS_PARENT_DECISION**

按用户本轮明确的严格目标，即“可构造的 new `TaskExecutionContext` 本身不依赖任何
`RemoteTaskRun*` authority/type，同时精确绑定 scope/device/window/gateway/checkpoint source”，当前冻结的五个
production 文件不足。

原因不是五个文件里写不出一个重载构造器，而是现有跨文件合同仍把新路径锁回旧类型或隐式线程上下文：

1. `TaskExecutionContext` 当前唯一构造器接收 `CloudTaskServiceExecutionContext`；后者的两个 package-private
   构造器必须取得 old execution gate、action ledger、final-consumption coordinator、exclusive authority 和
   generation projection。沿用该 delegate 不能满足“new context 不依赖 old authority”。
2. `TaskExecutionContext.getScope()` 的公开返回类型是 `RemoteTaskRunScope`，`revalidate()` 的返回类型是
   `RemoteTaskRunAuthorization`。若“无 `RemoteTaskRun*`”指新 context 的类型合同也必须为零，这两个旧签名不能同时
   充当新路径的 scope/checkpoint API。
3. `TaskCheckpointDecision` 的 record component 仍是 `RemoteTaskRunStatus currentStatus`。直接让 turn-native
   checkpoint source 返回该 record，仍把新 source 的编译合同绑到 `RemoteTaskRun*`。
4. `LegacyTaskExecutionTurnContextProvider.currentContext()` 当前硬编码
   `context.getScope().deviceId()`。即使五文件内增加 turn-native `deviceId`，该 provider 也不会读取它；它不在
   TURN-13C 冻结写集内。
5. `TurnGameClient` 是 singleton `@Component`，每次 invocation 都重新从 provider/holder 解析 identity。把它存进
   context 并不自动得到“绑定到该 context 的 gateway”；正确性依赖调用时同一线程正由 holder 绑定同一 context。
   嵌套绑定或错误线程下，`contextA` 暴露的同一个 client 可以解析成 `contextB`，目前没有 expected-identity 校验。
6. 当前唯一看见的 production 构造调用在 `CloudTaskRunAuthorityAssembly`：初始与 resume 两处均为
   `new TaskExecutionContext(serviceContext)`。五文件可以新增一个可供测试调用的构造器，但不能在本卡内形成
   production new-context caller。权威计划又把真实 factory 放在 TURN-40B，因此父级必须明确本卡“可构造”是
   API/test constructible，还是要求已有 production caller；两者不是同一验收口径。

## 已确认的当前 compile 风险

- `TaskExecutionContextHolder.isPauseRequested()` 调用 `TaskExecutionContext.isPauseRequested()`，但当前
  `TaskExecutionContext` 没有该方法。这是允许材料中可直接确认的 `cannot find symbol` 风险，TURN-13C 至少必须补回
  该公开方法。
- `CloudTaskRunAuthorityAssembly` 的两处旧构造调用要求保留
  `public TaskExecutionContext(CloudTaskServiceExecutionContext)`；删除或替换该构造器会立即破坏旧 authority 路径
  的编译，且违反 TURN-13C “本卡不得删除 old authority”。
- Java 类型互引本身不是 compile cycle：`TaskExecutionContext -> TurnGameClient ->
  TurnInvocationContextProvider -> TaskExecutionContextHolder -> TaskExecutionContext` 可以编译。真正应避免的是把
  per-run `TaskExecutionContext` 变成 Spring bean，或让 holder 构造注入 `TurnGameClient`；后者会形成
  `holder -> client -> provider -> holder` 的 Spring constructor cycle。
- `ThreadLocal` 必须在真实 Task worker 线程内 `callWith` 后再调用 gateway；在提交线程先绑定、Task worker 后执行，
  provider 会看不到 context。该问题不能用 Spring 注入成功替代验证。

## 父级必须冻结的最小方案

推荐采用“双轨兼容、显式 turn-native binding”，不让新构造器接收
`CloudTaskServiceExecutionContext`、`CloudTaskRunExecutionGate`、旧 ledger、旧 authorization 或旧 scope DTO。

### 1. 五文件内的新 API 形状

在 `TaskExecutionContext.java` 内放置临时 nested value/API，避免另造顶层 DTO：

```java
public record TurnTaskScope(
        String tenantId,
        String userId,
        String stateRoot) {
}

public record TurnTaskIdentity(
        TurnTaskScope scope,
        String deviceId,
        String windowId,
        String taskRunId,
        String taskType,
        long runRevision) {
}

@FunctionalInterface
public interface TurnCheckpointSource {
    TaskCheckpointDecision checkpoint(TurnTaskIdentity expectedIdentity);
}

public TaskExecutionContext(
        CloudTaskServiceMetadata metadata,
        TurnTaskIdentity identity,
        TurnGameClient turnGameClient,
        TurnCheckpointSource checkpointSource)

public TurnTaskScope getTurnScope()
public String getDeviceId()
public TurnGameClient getTurnGameClient()
public boolean isPauseRequested()
```

约束：

- new constructor 对 scope/device/window/taskRunId/taskType/client/source 全部 fail-fast 非空、关键字符串非 blank。
- `metadata.taskCode()` 必须与 `identity.taskType()` exact 相等。
- checkpoint 每次只读取 source 一次，并把 exact `TurnTaskIdentity` 作为参数；返回 null、expected revision 不符或
  非 current 状态必须 fail-closed，不能映射成 success/false。
- `isPauseRequested()` 只把明确 `PAUSED` 当作 true；stale/missing/uncertain/completed 不得被当成普通 false。
- new context 上的 old client/service/revalidate/state-owner API 在迁移完成前只能明确 fail-closed，不能返回 null、
  fabricated authorization 或 fallback 到全局窗口。
- legacy constructor 保留原 delegate 行为；new constructor 不保留 delegate 引用。

`stateRoot` 的最终 Java 类型以及 `CloudTaskServiceMetadata` 是否允许由未来 TURN-40B factory 公开构造，在本轮允许的
只读材料中无法确认。父级若不允许 `String stateRoot` 或 metadata 构造不可达，应在发卡前冻结现有实际类型；不能让
worker 临场猜测。

### 2. 必须扩写的精确 production 文件

严格“new context 零 `RemoteTaskRun*` 类型依赖”至少需要把下列文件加入 TURN-13C 或一个紧邻且先于 business card
的纠偏卡：

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/LegacyTaskExecutionTurnContextProvider.java`

   精确修改：从
   `new TurnInvocationContext(context.getScope().deviceId(), context.getWindowId())`
   改为读取 new context 的 `getDeviceId()` 与 `getWindowId()`；不得 fallback 到 legacy/global identity。

2. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClient.java`

   父级需二选一冻结：

   - 最小但依赖 holder invariant：保留现 API，不扩写本文件；验收明确规定 client 只可在
     `holder.callWith(exactContext, ...)` 内调用，并把错线程/错嵌套 context 作为 fail-fast 测试。
   - 更强的 exact-gateway 合同：本文件新增 bound invocation API，例如
     `BoundTurnGameClient bind(TurnInvocationContext expected)`，每次调用先确认 provider 当前 identity 与 expected
     完全相同，再复用现有单 actionId/单 execute 逻辑。若选择此合同，本文件是必需扩写文件。

3. 若父级要求 TURN-13C 当场具有 production constructor caller，而非仅 public constructor + contract test，则还必须
   提前加入计划中的
   `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskFactory.java`。
   这会前移 TURN-40B 边界，不建议默认采用；推荐把 TURN-13C 验收明确写成“API/test constructible，零 runtime
   activation”，由 TURN-40B 成为第一个 production caller。

4. 若 `CloudTaskServiceMetadata` 当前不能从 `turn.runtime` 构造，必须扩写
   `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskServiceMetadata.java`
   以提供只读、无 authority 的公开 factory/builder；不得为绕过可见性复制第二套业务 metadata。此点需父级读取该
   文件后冻结。

此外，若父级要求 `TaskCheckpointDecision` 的公开 record shape 也立即零 `RemoteTaskRunStatus`，仅五文件无法安全完成：
旧 execution gate 的构造调用和所有 `currentStatus()` consumer 必须先做 exact reference manifest。已知至少会涉及
`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskRunExecutionGate.java`；
在未列全生产引用前不得改 record component 类型。推荐 TURN-13C 先增加并使用 turn-native decision API，同时保留旧
record shape，真正零引用留给 TURN-38A/39 的冻结清理。

## 必须保留的旧签名

TURN-13C 是 additive bridge，不是删除卡。至少保留下列源码签名及 legacy 行为：

- `TaskExecutionContext(CloudTaskServiceExecutionContext)`，供 `CloudTaskRunAuthorityAssembly` 初始/resume 两处调用。
- `getTaskCode()`、`getTaskName()`、`getRequestedTaskCode()`、`getRequestedTaskName()`。
- `getWindowId()`、`getWindowRole()`、`getNativeWindowHandle()`、`getNativeWindowProcessId()`。
- team/retry/startup/time getters、`getTaskRunId()`、`getTaskType()`、identity/stop/revision getters。
- `RemoteTaskRunScope getScope()` 与 `RemoteTaskRunAuthorization revalidate()` 仅作为 legacy compatibility API 保留；
  new path 不得以它们作为 turn-native source。
- `throwIfStopRequested()`、`isStopRequested()` 的 ACTIVE/STOPPED/transition 异常语义。
- `CloudGameClient getGameClient()`、`CloudTaskServicePort getRemoteGameClient()`，直到 TURN-39/删除卡按引用清单移除。
- left-top pending 四方法、`hasWindow()`、`hasNativeWindow()`、team/startup helpers、`getLogPrefix()`。
- Holder 的 `callWith`、`current`、`checkpointIfPresent`、`isPauseRequested`；嵌套恢复与 finally remove 行为不变。
- `TaskCheckpoint.throwIfStopRequested(TaskExecutionContext, String)`。
- `TaskCheckpointDecision` 当前 canonical record/accessors、`disposition()`、`missingContext()`、
  `interruptedWhileCurrent(long)`，直到旧 gate consumer 被精确迁移。
- `TaskSleep.sleepOrStop(TaskExecutionContext, long, String)`；非正数不 checkpoint、正数前后 checkpoint、interrupt
  status 恢复和异常类型不变。
- `TurnGameClient` 现有 public constructor，以及 `capture`、`execute`、`localService`、
  `latestWindowMetadata` 的签名、一次 invocation 一个 UUID、无自动 retry 语义。
- `TurnInvocationContextProvider.currentContext()` 的签名；无当前 Task thread context 时仍抛
  `IllegalStateException`。

## `TaskExecutionContextTurnContractTest` 必须断言

1. 不构造 `CloudTaskServiceExecutionContext`、old gate、ledger、assembly 或 authorization，即可直接构造 new context。
2. exact scope/tenant/user/stateRoot/device/window/taskRunId/taskType/revision round-trip；blank/null/mismatch 构造失败。
3. holder 绑定 new context 后，provider 返回 exact device/window；退出后 holder 为空，嵌套绑定恢复前一 context。
4. 通过 context 暴露的 gateway 发一个 scripted action，fake command port 只收到一次，action 的 device/window 与
   context 完全一致；错线程、未绑定、绑定另一 context 时必须在 port 调用前失败。
5. checkpoint source 每个 checkpoint 只调用一次，并收到 exact identity；ACTIVE 返回 0，PAUSED 保持既有
   park/transition 语义，STOPPED 抛 `TaskStopRequestedException`，stale/missing/uncertain/null 决策 fail-closed。
6. `isPauseRequested()` 对 PAUSED 为 true；ACTIVE 为 false；其余非明确状态不得伪装成普通 false。
7. `TaskSleep`：`millis <= 0` 零 source 调用；正常正等待前后各一次；interrupt 恢复 interrupt flag，并按第二次
   checkpoint 的 STOPPED/PAUSED/ACTIVE-current 结果保持原异常语义与 exact revision。
8. legacy 单参数 constructor 与上节全部旧 public signature 仍存在；legacy delegate 路径行为不变。
9. new context 调用仅属于 old authority 的 client/service/revalidate/state-owner API 时明确 fail-closed，绝不返回 null、
   fabricated success 或 global fallback。
10. 手工构造 `holder -> provider -> TurnGameClient -> new context` 成功，证明没有 Spring constructor cycle；禁止把
    context 注册为 singleton bean，禁止构造/测试时启动 host、Task、线程或 loop。

## 父级冻结建议

最小可实施路线是：扩写 `LegacyTaskExecutionTurnContextProvider.java`，明确 TURN-13C 只交付可由 named contract test
直接构造的 dual-path context，生产 caller 留在 TURN-40B；同时选择 holder-invariant gateway 或扩写
`TurnGameClient.java` 的 bound API。父级还需确认 `CloudTaskServiceMetadata` 构造可见性与
`TaskCheckpointDecision` 的本卡兼容口径。上述四点未冻结前，不应把五文件原写集直接发给 worker 猜接口。

PRECHECK_DELIVERED
