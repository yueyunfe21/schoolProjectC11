# TURN-13C Task Context Readiness Helper

- readiness: `READINESS_RISKS`
- role: non-binding readiness helper；这不是评审结论。
- parent fact: 按父级最新状态，TURN-13H 已复审通过，TURN-13C 已转 `READY`。
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- scope: 只回答五文件是否足够、provider 是否必须加入、非循环构造 API、测试验收。

## 1. 五个 production 文件够不够

**结论：不够。**

权威计划第 17 节当前只列五个文件：

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`
2. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/runner/context/TaskExecutionContextHolder.java`
3. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/runner/stop/TaskCheckpoint.java`
4. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/runner/stop/TaskCheckpointDecision.java`
5. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/runner/stop/TaskSleep.java`

证据：计划 `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md:1044-1052`。

这五个文件可以恢复 `696a12b0` 的 checkpoint/pause/stop/interrupt public surface，也可以在
`TaskExecutionContext` 内增加 turn-native 字段和 API；但它们不能让一个完全不构造
`RemoteTaskRun*` 的新 context 被现有 `TurnGameClient` 正确识别。硬约束来自只读 provider：

- `LegacyTaskExecutionTurnContextProvider.currentContext()` 在
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/LegacyTaskExecutionTurnContextProvider.java:25-30`
  固定执行 `context.getScope().deviceId()` 和 `context.getWindowId()`。
- 当前 `TaskExecutionContext.getScope()` 在
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java:122-125`
  固定返回 `RemoteTaskRunScope`。
- `RemoteTaskRunScope` 自身要求 tenant/user/device/clientSession 四元组；因此仅改五文件时，新 context 要么继续构造
  `RemoteTaskRunScope`，要么 provider 在第一次 turn 调用时失败。前者不满足“new context 无需
  `RemoteTaskRun*` 构造”，后者不满足可实施合同。

`NEEDS_PARENT_DECISION-01`：父级需明确把 TURN-13C production write set 从五文件扩为六文件；否则应明确将
“无 `RemoteTaskRun*` 构造的新 context”延期，不能同时声称两项目标均已满足。

当前五文件还存在确定的兼容缺口：

- Holder 在 `TaskExecutionContextHolder.java:51-54` 调用当前不存在的 `TaskExecutionContext.isPauseRequested()`。
- `TaskCheckpoint.java:19-32` 只有 explicit-context 一个 overload，而 Cloud 现有 Task/Service 已调用 holder overload
  和 explicit+holder overload。
- `TaskSleep.java:19-37` 没有基线 `boolean sleep(long)` 与 `throwIfStopRequested(...)`，而现有 Service 已广泛调用。
- 当前 null context 被转成缺失上下文异常；`696a12b0` 的 null context 是合法 legacy/debug 路径，只继续检查线程中断。

因此五文件本身确实必须写，但不足以完成 turn-native 新 context 的闭环。

## 2. 是否必须加入 LegacyTaskExecutionTurnContextProvider

**结论：必须加入，前提是父级坚持新 context 不构造任何 `RemoteTaskRun*`。**

推荐冻结的第六个 production 文件：

`D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/LegacyTaskExecutionTurnContextProvider.java`

该文件只做一处语义替换：

```java
return context.getTurnInvocationContext();
```

替换当前 `context.getScope().deviceId()` + `context.getWindowId()` 投影。它仍然只读当前 Holder，不缓存、不回退窗口、
不创建 identity、owner、session 或 lifecycle。

以下文件保持只读：

- `TurnInvocationContextProvider.java:6-15`：继续规定“当前 exact device/window，无 context 即抛错”。
- `TurnGameClient.java:27-50`：继续是现有唯一 `@Component` client。
- `TurnGameClient.java:62-137`：每次 action 一个 UUID、一次 command；`latestWindowMetadata()` 只读且不创建 action。
- `CloudServiceConfiguration.java:27-40,69-72` 与 `CloudServiceHost.java:53-57`：现有扫描、factory、command port 已足够。

**不需要新 bean，不需要第二个 client，也不应把 `TurnGameClient` 直接注入 Holder。** 直接注入会形成：

```text
TaskExecutionContextHolder
  -> TurnGameClient
  -> LegacyTaskExecutionTurnContextProvider
  -> TaskExecutionContextHolder
```

`NEEDS_PARENT_DECISION-02`：父级需冻结六文件写集。若第六文件仍被判为只读，则本卡只能交付 legacy-context adapter，
不能交付满足题设的新 context。

## 3. 非循环构造 API

推荐冻结为“client 进入非 Spring bean 的 context，Holder 不依赖 client”：

```java
public static TaskExecutionContext turnNative(
        CloudServiceScope serviceScope,
        TurnInvocationContext invocationContext,
        TurnWindowMetadata initialWindowMetadata,
        CloudTaskServiceMetadata taskMetadata,
        String taskRunId,
        TurnGameClient turnGameClient)

public CloudServiceScope getTurnServiceScope()
public TurnInvocationContext getTurnInvocationContext()
public TurnGameClient getTurnGameClient()
```

构造和运行依赖固定为：

```text
Spring singleton TurnGameClient -> TurnInvocationContextProvider -> Holder
future TURN-40B Task factory -> 取现有 singleton TurnGameClient -> TaskExecutionContext.turnNative(...)
Holder.callWith(context, action) -> 只绑定/恢复同一个 context
provider.currentContext() -> context.getTurnInvocationContext()
```

这样 Holder 没有 `TurnGameClient`、`ObjectProvider<TurnGameClient>` 或第二 ThreadLocal client，Spring bean 图无回环；
新 context 的 factory 参数也没有任何 `RemoteTaskRun*` 类型。TURN-40B 只负责调用 factory，不在 TURN-13C 提前做
Task factory、queue、start/ack 或 caller cutover。

旧 API 兼容必须按以下边界冻结：

- 保留现有 `public TaskExecutionContext(CloudTaskServiceExecutionContext)`；legacy context 的全部旧行为不变。
- 保留当前所有 public 方法名、参数和返回类型，包括 `getScope()`、`revalidate()`、`getGameClient()`、
  `getRemoteGameClient()`、revision 与 pending-state API；不得改 Task/Service 调用签名。
- 新 context 只保证共同 metadata 与新增 turn-native API。只属于 old authority 的旧方法在新 context 上应明确抛出
  `IllegalStateException`，不得伪造 `RemoteTaskRunScope`、clientSession、authorization、ledger 或 old client。
- 若父级要求上述 old-authority 方法在新 context 上也能正常工作，则这六文件方案仍不够，必须等 caller cutover 或扩大
  模型/write set。

`NEEDS_PARENT_DECISION-03`：父级需确认“旧 public signature 兼容”是“legacy 构造路径行为不变 + 新构造路径不伪造
old authority”，而不是要求新 context 继续提供 old authority 行为。

checkpoint 精确语义冻结：

- `TaskExecutionContext.throwIfStopRequested(): long`、`isStopRequested(): boolean`、
  `isPauseRequested(): boolean` 保持。
- 新 context 只读同一 `TurnGameClient.latestWindowMetadata()`；metadata 的 device/window 必须与 immutable
  `TurnInvocationContext` 完全相等。
- stop 优先于 pause；stop 和线程中断都抛 `TaskStopRequestedException`，不得变成业务失败。
- pause 只在 checkpoint 阻塞；按 `696a12b0` 的 250ms cadence 观察 resume/stop，resume 后返回实际 blocked millis；
  不 park/yield task turn，不创建 action，不加 TTL 或业务 retry。
- `TaskCheckpoint` 恢复四个基线 API：explicit context、holder、explicit+holder、`throwIfInterrupted(String)`；null
  context 仅检查 interrupt。
- `TaskSleep` 恢复 `boolean sleep(long)`、`sleepOrStop(...)`、`throwIfStopRequested(...)`；中断标志必须恢复，
  `sleepOrStop` 映射为 `TaskStopRequestedException`。
- `TaskCheckpointDecision` 保留现有 canonical record/public shape；turn 缺失或 identity mismatch 如需 typed unwind，只允许
  新增无权 factory，revision=`-1`、status=`null`，不得构造 run/session/owner。

`NEEDS_PARENT_DECISION-04`：`latestWindowMetadata()` 在首个 accepted request 前合法返回 empty
（`TurnGameClient.java:116-126`）。推荐 empty 立即产生 typed `MISSING_BINDING` unwind，且不映射业务失败、不等待、不
轮询造 TTL；若父级要求其它行为，必须先明确，因为“当作 active”与“等待首帧”都会新增未冻结语义。

## 4. 精确测试验收

权威计划第 19 节将唯一 test write set 定为：

`D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/runner/context/TaskExecutionContextTurnContractTest.java`

依据：计划 `:1344-1347` 的 `C_TEST` 别名和 `:1385-1394` 的 TURN-13C 行。不得改到 production mirror 的
`src/test/java/com/bot/...`；测试只使用 fake/scripted 对象，不启动 application/server/Task，不触发真实输入、截图或 UI。

该类必须逐项断言：

1. **无 RemoteTaskRun 构造**：`turnNative(...)` 的参数类型不含任何 `RemoteTaskRun*`；构造测试不创建 scope、
   coordinator、authorization、owner、session 或 ledger。
2. **exact scope/identity**：tenant/user 与 device/window 分别逐字段相等；空白和 mismatch 被拒绝；无 fallback window。
3. **provider 闭环**：Holder 外调用 provider 抛错；Holder 内返回同一个 context 的 exact identity；nested `callWith`
   正常/异常退出都恢复 outer identity，最外层退出后清空。
4. **单 client、无 Spring 环**：Holder 的字段和构造参数不含 `TurnGameClient`/`ObjectProvider<TurnGameClient>`；
   `context.getTurnGameClient()` 与传入实例 `assertSame`；不创建第二 bean/client。
5. **旧 public surface**：反射确认旧 constructor 与所有旧 public 方法仍存在且返回类型不变；另确认
   `GameTask.execute()`、`GameTask.execute(TaskExecutionContext)`、`GameTask.stop()` 未变。
6. **checkpoint ACTIVE**：exact metadata、pause=false、stop=false 时返回 `0`；`isStopRequested=false`、
   `isPauseRequested=false`；command execute count=`0`。
7. **checkpoint STOP**：stop=true 时三条入口均抛 `TaskStopRequestedException`；pause+stop 同时为 true 时 stop 优先；
   不产生业务失败、不执行 action。
8. **checkpoint PAUSE/RESUME**：pause=true 时 worker 到 checkpoint 后保持阻塞；同一 fake metadata 改为 pause=false 后
   同一 context 恢复，返回 blocked millis；task metadata、gateway identity 与对象 identity 均未重建。
9. **pause 中 STOP**：暂停等待期间改为 stop=true，立即以 `TaskStopRequestedException` 退出；没有 action、retry、TTL、
   park/yield 或 replacement context。
10. **interrupt**：checkpoint 前已中断、pause wait 中断、`TaskSleep.sleep` 中断都保留 interrupt flag；前两者及
    `sleepOrStop` 统一映射 `TaskStopRequestedException`；`sleep(long)` 自身返回 false。
11. **null/overload 基线**：null explicit context 且线程未中断返回 `0`；holder null 返回 `0`；explicit+holder 返回两次
    checkpoint 的 blocked millis 总和；四个 `TaskCheckpoint` public signature 精确存在。
12. **TaskSleep 基线**：非正 millis 立即成功/返回；正值只 sleep 一次；`sleepOrStop` 前后各 checkpoint 一次，不新增
    retry 或第二次业务等待。
13. **missing/mismatch**：empty metadata 按父级选定语义；返回不同 device/window 必须在任何 action 前 typed unwind，
    且不能回退到另一窗口。
14. **BC4**：在 Holder 绑定的新 context 中，经同一个 `getTurnGameClient()` 分别脚本化 COMPLETED、FAILED、STOPPED、
    DUPLICATE_OR_UNCERTAIN；每次 public invocation 恰好一个 actionId、一次 execute、exact device/window，状态原样保留，
    无额外 action/retry。
15. **排除项**：测试不得创建或断言 owner/session/ledger/TTL/business retry；不得删除或要求零引用 old authority；不得
    修改 Task/Service caller。old authority 删除属于 TURN-38A，Task runtime 构造属于 TURN-40B。

推荐冻结写集：上述五个 production 文件 + `LegacyTaskExecutionTurnContextProvider.java` + 唯一测试类。除此之外全部只读。

本 helper 未运行 Maven、runtime、Task、UI、capture 或 input；未修改 Java、测试、计划、ACTIVE_WORK、PACKAGE、矩阵或 Git。

<!-- TRUE_EOF: TURN-13C readiness helper -->
