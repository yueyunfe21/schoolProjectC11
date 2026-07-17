# CR271 TURN-22D1 Repair #1 public-fixture preflight

## 身份、范围与结论边界

- 身份：CR271 Internal helper，只做 TURN-22D1 Repair #1 的 test-only public-fixture 预检。
- 唯一写集：本报告。未改 Java、TURN-22D1 子卡、TURN-22 父卡、`ACTIVE_WORK.md` 或任何已有文档。
- 禁止项保持：未运行 Maven/JUnit/compile/package、runtime/application/server/Task/UI/capture/input，未做 Git mutation。
- 本报告只给 External A/父级一个可直接落码的最小替换配方；**不构成批准、阻断、review verdict 或卡片状态变更**。

## 必读与当前证据

已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271 最新段、
`2026-07-16-turn-card-TURN-22D1.md` 全文，以及当前：

- `TurnExecutionWindow.java` 全文；
- `TurnInputStepExecutor.java` 全文；
- `TurnInputStepExecutorContractTest.java` 全文；
- public resolver 所需的 `MultiWindowTaskManager.getRunner(...)`、
  `WindowNativeBindingRefreshService.refreshAndCommit(...)`、`WindowTaskRunner` public accessors；
- 已有 package-scoped test fixture `TurnContractFixtures.TestTaskManager`、
  `TurnContractFixtures.RecordingBindingRefreshService` 与 `TurnContractFixtures.bareRunner(...)`。

预检起始快照时间：`2026-07-16T09:33:14.155-04:00`。helper 读取期间 External A 于
`2026-07-16T09:34:10.764-04:00` 并发交付 Repair #1；随后已把子卡新增段和 695 行 current test 重新完整读完。

| 文件 | 当前 SHA-256 | 本 helper 权限 |
|---|---|---|
| `TurnExecutionWindow.java` | `a54b84e08ad65b16046be6683421126f061f40ab8473fdf2c19f25f70d0c0666` | production 只读 |
| `TurnInputStepExecutor.java` | `a64422b061021dcbcec51837ac9f68a59bf21d57984469703cd1a68c2963134e` | production 只读 |
| `MultiWindowTaskManager.java` | `b028260260b9d0927bbab1e980a4675e749110bcd887bb00325271d153d2598b` | production 只读 |
| `WindowNativeBindingRefreshService.java` | `8be2030bc4b6a5d25f7fdd163f5f61313a0d0dc1ea7998bad94ee3b284e6d091` | production 只读 |
| `TurnInputStepExecutorContractTest.java`（预检起始，604 行） | `dc18abd53ef8711ea8ab8e6a41f565cbab72a24f05b23a966a365e64c58f94ee` | External A owner；本 helper 只读 |
| `TurnInputStepExecutorContractTest.java`（并发交付后，695 行） | `f5a7992fc6566f00b56f4e7e21c8e66fcf328f519523e73d6858ae93042e7a81` | External A delivery；本 helper 只读 |

预检起始违规点是 contract test 的 `executionWindow(boolean)`：它导入
`java.lang.reflect.Constructor`，调用 `TurnExecutionWindow.class.getDeclaredConstructor(...)` 与
`constructor.setAccessible(true)`，再以 `runner=null` 直接构造 private production snapshot。该路径既违反冻结的
“不得 private-production reflection”，也没有证明测试对象真正通过 public runner lookup、一次 binding refresh/commit
及 production stop metadata 计算产生。

并发交付后的 current test 已改为调用 public `TurnExecutionWindow.resolveForAction(...)`，并在本文件新增
`ScriptedRefreshService`、`TestTaskManager`、`BareWindowTaskRunner`、`allocate(...)` 与 `findUnsafe()`。private
`TurnExecutionWindow` constructor reflection 已消失；但 current test 为此新增 `sun.misc.Unsafe`、
`java.lang.reflect.Field` 与 JDK `theUnsafe` 的 `setAccessible(true)`。External A 已在子卡明确披露该 residual。
下面给出的仍是用户指定的**最小**替换：复用仓内已有 package fixture，避免在本 named test 复制同一套 runner/
manager/refresh/allocation machinery。此对照不判断当前 delivery 通过或阻断。

production 修复本身不需要变化：`TurnInputStepExecutor.java` 当前 SHA 与 D1 delivery 一致；mouse path 已把同一完整
action list、exact `window.context()`/`window.binding()` 一次交给 frozen boundary，并保留 typed
`STOP_REQUESTED -> STOPPED`。Repair #1 应只替换 test fixture。

## 最小 public-resolver 替换

### 1. 相对 695 行 current delivery 的精确删改

在 `TurnInputStepExecutorContractTest.java` 当前交付上：

- 保留已新增的 `TurnAction` 与 `WindowRuntimeStatus` import；
- 删除仅供本地复制 fixture 使用的 `MultiWindowTaskManager`、`RunningTaskHandle`、`WindowTaskRunner`、
  `sun.misc.Unsafe`、`java.lang.reflect.Field` 与 `java.util.Optional` import；
- 删除类级 `UNSAFE` 字段，以及本地 `ScriptedRefreshService`、`TestTaskManager`、`BareWindowTaskRunner`、
  `allocate(...)`、`findUnsafe()` 完整块；
- 保留 `WindowNativeBindingRefreshService` import，因为 real-queue drift 用例仍直接实例化它；
- 不新增 reflection、source scan、sleep、worker、input provider fixture 或第二层 wrapper。

### 2. 唯一 helper 原地替换

保持现有方法名与返回型 `executionWindow(boolean)`，只替换方法体；不要新增 wrapper 层。最小可编译形状如下：

```java
private static TurnExecutionWindow executionWindow(boolean stopRequested) {
    WindowRuntimeContext context = new WindowRuntimeContext("window-7", new GameContext());
    WindowNativeBinding binding = new WindowNativeBinding(
            "12345", "game-window-7", "GameWindow", 88L, 137, 241, 10, 10);
    if (stopRequested) {
        context.setStatus(WindowRuntimeStatus.STOPPING);
    }

    TurnContractFixtures.BareWindowTaskRunner runner =
            TurnContractFixtures.bareRunner(context, false, false);
    TurnContractFixtures.TestTaskManager taskManager = new TurnContractFixtures.TestTaskManager();
    taskManager.putRunner("window-7", runner);
    TurnContractFixtures.RecordingBindingRefreshService refresh =
            new TurnContractFixtures.RecordingBindingRefreshService(binding);
    TurnAction action = new TurnAction(
            1,
            "fb68ba07-9cb7-47d2-bc7e-8ab31ae72555",
            "device-1",
            "window-7",
            List.of(waitStep(0, 1L)),
            false);

    TurnExecutionWindow window = TurnExecutionWindow.resolveForAction(action, taskManager, refresh);
    assertEquals(1, taskManager.getRunnerCalls(), "public resolver must look up the runner exactly once");
    assertEquals(1, refresh.calls, "public resolver must refresh and commit exactly once");
    assertSame(context, refresh.lastContext, "refresh must receive the registered exact context");
    assertSame(runner, window.runner());
    assertSame(context, window.context());
    assertSame(binding, window.binding());
    assertEquals(stopRequested, window.metadata().stopRequested());
    return window;
}
```

该方案使用仓内既有全内存 scripted collaborators：

- `TestTaskManager` 仅从内存 map 返回 exact runner，零注册、副作用与真实窗口查询；
- `RecordingBindingRefreshService` 仅把预置 binding commit 到传入 context 并返回 `Optional.of(binding)`，零 JNA/Win32；
- `bareRunner(...)` 是现有 test-only package fixture，只提供 context/current-task seam；本修复不再读取或打开
  `TurnExecutionWindow` 的任何 private constructor/member，也不要把它的 test allocation 逻辑复制进本测试。

边界说明：`TurnContractFixtures.bareRunner(...)` 内部仍集中复用仓内既有 test-only `Unsafe` allocation；因此该最小
方案消除的是**本 named test 新复制的 residual**及所有 private-production reflection，而不是声称整个 test source tree
不存在 `Unsafe`。若未来另设“全 test tree 禁 Unsafe”门，需要独立公共 runner fixture/POM/production seam 决策；本
helper 没有该授权，也不把它升级成当前卡 verdict。

`stopRequested=true` 不能再直接塞进手造 `TurnWindowMetadata`。应在 resolver 前公开设置
`context.setStatus(WindowRuntimeStatus.STOPPING)`；public `resolveForAction(...)` 随后通过
`isStopRequested(runner, context)` 生成 metadata，保留原 `stopAndInvalidWaitShortCircuitWithoutCreatingInput` 语义。

使用 `List.of(waitStep(0, 1L))` 只为给 resolver 一份合法、纯内存且不执行的 action shape；resolver 不执行 step，
因此不会产生 WAIT、线程、输入或时间依赖。

## 必须保留的 assertions

不要删除或弱化现有旧用例。尤其保留 TURN-22D1 三组验收：

1. `frozenSubmissionCarriesTheExactWindowAndRestoresTheCallersSentinelContext`
   - production executor 返回 `COMPLETED/OK`；
   - sentinel context、sentinel binding、`windowId`、player epoch 在 executor 返回后原对象恢复；
   - 外层 scope 退出后 holder 为空；
   - frozen submission 恰好一次；
   - boundary 收到与 public resolver 结果 `assertSame` 的 context 与 binding；
   - submission 内观察到 exact `window-7 / HWND 12345 / pid 88 / rect 137,241,10,10 / player epoch`；
   - action list 精确为 `CLICK_LEFT(delay=150) -> SLEEP(500)`。
2. `typedStopFromTheFrozenBoundaryMapsToStoppedRatherThanQueueFailure`
   - typed `STOP_REQUESTED` 映射为 `STOPPED/STOPPED`；
   - caller thread 未被伪造 interrupt；
   - submission 恰好一次，零 retry/replay。
3. `valueEqualRebindDriftIsTypedFailureAndNeverEntersTheInputQueue`
   - `A -> B -> A'` 后 binding 字段相同但对象不同；
   - production executor 返回 `FAILED/INPUT_QUEUE_FAILED`；
   - real queue size 保持 `0`，即 provider/input 不可达；
   - rejected path 后 holder 仍为空。

同时保留既有七种 mouse mapping、single-click `150/500` 同 request、mouse sequence 原子顺序、background keyboard、
unsupported/failure、pre-stop/invalid wait 与 interruptible WAIT assertions。Repair #1 只改变 snapshot 的构造来源，
不得改变这些输入/输出合同。

## 落码后静态复核点

- 按上述最小共享 fixture 方案，`TurnInputStepExecutorContractTest.java` 本文件内应为
  `getDeclaredConstructor=0`、`setAccessible=0`、`sun.misc.Unsafe=0`、`java.lang.reflect.Field=0`；这些仅用于人工
  源码复核，不要求新增 source-guard test，也不冒充全 test tree 零 Unsafe。
- `TurnExecutionWindow.resolveForAction(` 应由 `executionWindow(boolean)` 直接调用一次。
- production 四文件 SHA 必须与上表相同；本 helper 不授权 production 修改。
- External A 落码后仍按父卡要求交付；本 helper 未运行、未声称 named test/compile 通过。

## Post-write production SHA confirmation

本报告创建完成后已只读复算四个 production 文件；四个 SHA 与上表逐字一致。production 本轮零写入，
`TurnExecutionWindow.java` / `TurnInputStepExecutor.java` / `MultiWindowTaskManager.java` /
`WindowNativeBindingRefreshService.java` 均保持只读。

TRUE_EOF PRECHECK_COMPLETE
