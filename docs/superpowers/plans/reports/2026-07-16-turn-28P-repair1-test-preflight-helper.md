# TURN-28P Repair #1 named-test preflight helper

- 时间：2026-07-16T05:26:40.9131549-04:00（America/New_York）。
- 角色：非绑定 test-preflight helper；只提供 `PRECHECK` 与可编译 harness 设计，不作通过或阻断判断。
- 权威卡片锚点：最新 `PARENT DELIVERY REVIEW #2`、其 `05:12` addendum，以及随后 true EOF
  `REPAIR #1 CLAIMED ADDENDUM P0P1P2=0/2/2`。
- 唯一写入：本报告。没有修改 Java、测试、原卡或计划。
- 本轮没有运行 Maven/JUnit/compile、runtime/application/server/Task/UI、capture/input，也没有执行 Git 命令或
  Git mutation；两仓 dirty/untracked 原样保护。

## 1. 最新字节基线

Repair #1 production 在本 helper 阅读期间由实现 Worker 并行更新；以下是 `05:26:40-04:00` 重新读取后的当前 API，
不是领取时旧字节：

| 文件 | 当前关键事实 | 读时 SHA-256 |
|---|---|---|
| `InputActionQueue.java` | 新增 public `submitFrozenExactWindowExclusiveAndWait(description, context, binding, playerIdentityEpoch, callback)`；冻结路径 enqueue 前不 refresh，interrupt/remove 失败后 cooperative cancel 并 join terminal result | `95572c202d1cff73732fecebfb7710aa07dc770a27940b3a85577c212031866e` |
| `InputActionRequest.java` | 冻结 request 显式保存 binding、identity epoch 与 `frozenExactWindow`；检查 windowId/HWND/process/rect/epoch | `2c23ca1d7163d2a42c3f05552357fbaFA9FA50E036F1B7B18CE6A3367329F595` |
| `InputActionWorker.java` | frozen request 在真实 worker 内走 `focusFrozenWindowInActiveTransaction(...)`，callback 仍由 `InputActionScope.callWith(...)` 包围 | `3b8bc23d5639d8ddb471aaf8456d4d4d650c1be661813999b0116e52c8b4fb2d` |
| `InputActionScope.java` | 未变；`checkpoint()` 可观察 request cancellation，pause 保持 wait 语义 | `6e5b3b42b44e75b6de0f4b623e4595454ee2eebbbaa09e21b590ad042b0ceab` |
| `InputSequences.java` | public frozen overload原样转发 Queue 的五参数冻结边界 | `fa2f17bfb8b0ab672e986abcadc7c316b0eff1d3c9781424f7839a1b0f06fdd2` |
| `WindowAwareInputCoordinator.java` | frozen focus 显式核 context/windowId/binding/epoch，直接 focus frozen binding，不调用 refresh | `4325fd1c7a428318ea0d27c4f7adcd8373e1005857ea1b884b61bb198c9332c4` |
| `TurnCaptureStepExecutor.java` | probe 已调用 frozen overload；mechanics 间使用 `InputActionScope.checkpoint()`；Ctrl UP 捕获 `Throwable` 后仍执行 release settle | `2f4c1f09b7a70c07c104e183151d526ddb6f0584c5b02ae39940a2e3630f4ddc` |

四个既有 named-test 文件在同一读点仍是 Repair 前字节；新的
`InputActionFrozenExclusiveContractTest.java` 尚不存在。因此现有同步 fake 仍只 override legacy
`submitExclusiveAndWait(...)`，而 production probe 已调用新 frozen overload。若只补断言而不改 override，测试会经
`super(null)` 落到 null queue，不能形成有效证据。

## 2. 最小真实 queue/worker harness

每个需要真实 mechanics ownership 的 test 直接手工装配现有 production 对象，不启动 Spring：

1. `WindowIsolationProperties` 同时设置 `isolationEnabled=true` 与 `inputFocusEnabled=true`。只开其中一个会让
   `isInputFocusActive()` 为 false，从而完全跳过最需要验证的 frozen focus 分支。
2. 创建真实 `WindowTaskContextHolder`、`TaskExecutionContextHolder`、`GlobalInputLock`、
   `WindowAwareInputCoordinator`、`InputActionQueue`、`InputSequences`、`InputActionWorker`。
3. `CountingRefreshService extends WindowNativeBindingRefreshService` 只计数并返回预置 binding。直接 frozen queue tests
   期望 `0`；完整 `LocalTurnActionExecutor` tests 只允许 action resolve 的第 `1` 次调用，若出现第 `2` 次立即抛
   `AssertionError`，同时最终再断言 calls 恰好为 `1`。
4. `RecordingFocusService extends WindowFocusService` override `focusWithoutLock(binding)`，记录对象及 HWND/process/rect
   后返回 true；不接触 Win32。`RecordingInputProvider` 仅记录方法调用，不操作桌面。
5. worker 必须通过 public `InputActionWorker.start()` 消费真实 Queue，不能反射调用 private `handle`，不能手工调用
   callback。worker 是 daemon；每个 fixture 只启动一次，并用有界 latch 收口。
6. waiter 是独立普通线程，在 `windowHolder.runWith(context, ...)` 内调用 public frozen API 或完整
   `LocalTurnActionExecutor.execute(action)`。测试线程与 input worker 因而是两条真实线程。
7. 并发控制只用 JDK `CountDownLatch`、`AtomicReference/AtomicBoolean/AtomicInteger`、
   `CopyOnWriteArrayList` 和 `Thread.join(timeout)`；不引入 Awaitility/Mockito，不用生产 sleep。
8. 所有 latch 在 `finally` 中 release，并 interrupt/join waiter，避免失败路径留下非 daemon waiter。2 秒之类的
   bounded test wait 只防测试挂死，不得进入 production deadline/TTL。

现有大量同步 branch tests 可以保留，但其 fake 必须 override 精确的新方法
`submitFrozenExactWindowExclusiveAndWait(String, WindowRuntimeContext, WindowNativeBinding, long, Supplier<Boolean>)`，
并记录四项 snapshot 参数。legacy `submitExclusiveAndWait(...)` 应记录 forbidden call 或直接抛 `AssertionError`。
同步 fake 只保留 failure matrix；下面列出的并发/refresh/drift 合同必须由真实 worker harness 证明。

## 3. `InputActionFrozenExclusiveContractTest` 设计

新文件：
`D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java`。
使用 JUnit 5；不读取源码文本、不反射 private helper/常量。

### 3.1 `frozenExclusiveUsesRealQueueWorkerAndNeverRefreshes`

- waiter 调用当前 public frozen API，传 context、binding、enqueue 前读取的 epoch；真实 worker 执行 callback。
- 开启 focus active，断言 refresh calls=`0`、focus calls=`1`、callback calls=`1`、desktop fake calls 符合 callback
  明示动作。
- focus 收到的 binding 与 frozen binding 做 HWND/process/x/y/width/height 全字段比较；同时断言 worker scope 内 holder
  是同一 windowId。
- 返回后断言 callback 已退出，不能只断言 boolean。

### 3.2 `frozenExclusiveRejectsEveryExactIdentityDriftBeforeFocusCallbackOrInput`

- 一个 `@Test` 内循环独立 fixture，避免依赖额外 parameterized-test artifact。drift labels 至少为：
  `windowId`、`HWND`、`processId`、`x`、`y`、`width`、`height`、`playerIdentityEpoch`。
- 使用 test-only 可变 `WindowRuntimeContext` 子类分别 override getter，使每个 case 只漂移一个维度；否则普通
  `setNativeBinding()` 可能同时递增 epoch，导致“process/rect 检查实际缺失但被 epoch 检查遮住”的假通过。
- 先启动 waiter 并等真实 `queue.size()==1`，此时 request 已冻结但 worker 尚未 start；再只改变一个 getter，最后
  start worker。
- 每个 label 都断言：result=false、refresh=`0`、focus=`0`、callback=`0`、InputProvider=`0`。callback 内预置
  capture/Ctrl/MOVE counters，callback 为零即同时证明这些 mechanics 为零。

### 3.3 `interruptBeforeWorkerTakeRemovesFrozenRequestWithoutMechanics`

- 不启动 worker；waiter enqueue 后等 `queue.size()==1`，interrupt waiter。
- join 后断言 queue size=`0`、return=false、refresh/focus/callback/input 全零、waiter interrupt flag 在其返回点恢复。
- 这证明“尚未开工”是 remove + zero mechanics，而不是依靠 worker 后续清理。

### 3.4 `interruptAfterWorkerStartWaitsForCallbackFinally`

- callback 进入后 count down `callbackStarted`，在正常 body 的 checkpoint 观察 cancellation，并在 `finally` 记录
  `cleanup:start`、阻塞于 `allowCleanupFinish`、再记录 `cleanup:done`。
- main 在 callbackStarted 后 interrupt waiter；等 cleanup:start 后，在尚未 release cleanup latch 时断言 waiter 仍 alive、
  queue API 尚无返回值。
- release 后 join，严格断言 `cleanup:done < queue:return`，并以 `postReturnMechanics` counter 证明返回后没有 callback/
  input。此 test 证明 generic queue barrier；Ctrl 专属 barrier 由下一节证明。

## 4. `TurnCapturePixelChangeProbeContractTest` 设计

文件：
`D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java`。

### 4.1 先修正现有 branch harness

- `RecordingInputSequences` override frozen overload，断言 context、binding、epoch 与 `TurnExecutionWindow` 一致，再同步
  执行 callback；保留当前 changed/unchanged/failure matrix。
- 所有成功 trace 增加 `legacyExclusiveCalls=0`。这只证明 caller 选对 API，不替代真实 Queue/Worker tests。

### 4.2 `waiterInterruptAfterCtrlDownWaitsForOneUpAndReleaseSettle`

新增专用 `RealWorkerProbeHarness`，其 `InputSequences` 若要记录 queue enter/return，只能 override frozen overload并调用
`super`；不得直接调用 callback。

确定性时序：

1. worker 完成 before capture 与 Ctrl DOWN，进入 `ctrlDownSettleMs=80` 的 fake wait；该 wait count down
   `downSettleEntered` 后等待 `allowDownSettleReturn`。此时 Ctrl DOWN 已真实从 fake 返回。
2. main 等 `downSettleEntered` 后 interrupt waiter，再 release down settle。worker 随后的 production checkpoint 必须看见
   request cancellation，在 MOVE 前退出 normal body。
3. finally 恰好调用一次 Ctrl UP；`ctrlUpSettleMs=100` fake count down `upSettleEntered` 后等待
   `allowUpSettleReturn`。
4. main 在 `upSettleEntered` 时断言 waiter 仍 alive、queue/Execution 均未 return、MOVE=`0`、after capture=`0`、UP=`1`。
5. release up settle 后 join；断言事件严格为
   `capture:before < key:DOWN < wait:80 < key:UP < wait:100 < queue:return < execution:return`。

最终 `Execution` 必须为 `STOPPED/STOPPED`、frame=null；before capture=`1`、after capture=`0`、MOVE=`0`、UP=`1`，
且所有 fake 在看到 `executionReturned=true` 后调用都会累加的 `postReturnMechanics` 必须为 `0`。

### 4.3 `nonRuntimeCtrlUpThrowableClosesAsReleaseUncertainty`

- 用同一真实 worker harness，让 keyboard fake 在 UP 抛 `AssertionError`（属于 `Throwable`，不是
  `RuntimeException`）；DOWN 成功。
- 断言 UP attempts=`1`，100ms release settle 仍调用一次，结果精确为
  `FAILED/CTRL_RELEASE_FAILED`，frame=null，绝无 `PIXELS_CHANGED/PIXELS_UNCHANGED`。
- 必须穿透 outer `InputActionWorker.catch(Throwable)`；同步 fake 会绕过父卡指出的 policy 接缝，不能作为本项唯一证据。

## 5. `LocalTurnActionExecutorContractTest` 设计

文件：
`D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`。

现有 `TurnContractFixtures.TestTaskManager/BareWindowTaskRunner/RecordingCaptureService` 可复用；只新增一个真实 Queue/
Worker probe fixture，不改其它 legacy/pointer-clear tests。

### 5.1 `pixelProbePublicActionRefreshesExactlyOnceAcrossResolveQueueWorkerAndFocus`

- 走真实 public `LocalTurnActionExecutor.execute(pixelProbeAction(...))`，不是直接构造 request。
- 与 action resolver 共用一个 `CountingRefreshService`：第 1 次返回 A，任何第 2 次调用立即失败。
- waiter enqueue 后 start worker，正常完成 changed 或 unchanged probe。
- 断言 refresh total=`1`、真实 queue submission=`1`、focus binding/capture binding/keyboard binding 均为 A、outcome 为真实
  completed probe code + 唯一 after frame。这样同时覆盖 production caller、queue 与 focus，不能只测新 API 本身。

### 5.2 `resolvedProbeDriftBeforeWorkerStartRunsNoCallbackOrMechanics`

- waiter 先进入完整 public action；等真实 queue size=`1` 后，说明 action 已 resolve 且 frozen request 已 enqueue。
- worker 尚未 start 时把 context 从 A 改为 B，再 start worker。
- 断言 resolver refresh仍恰好 `1`；worker 返回 typed failure/stop 后，focus、probe callback、capture、Ctrl、MOVE 均为 `0`。
- 另用 A -> B -> A 后再 start worker 的 case：最终 binding 字段恢复 A，但 epoch 已变化，仍必须零 callback/input；否则
  只证明 binding compare，没有证明 identity epoch。

### 5.3 `interruptedProbeOutcomeReturnsOnlyAfterCtrlUpSettle`

- 复用第 4.2 节 latches，但 waiter 调用完整 `LocalTurnActionExecutor.execute`，并在方法返回后记录 `outcome:return`。
- action 使用 `fullWindowFailureEvidence=false`，避免额外 failure capture 混入“返回后无 mechanics”判定。
- 在 UP settle 被 gate 时断言 `ExecutedTurn` 仍为 null；release 后断言
  `key:UP < wait:100 < queue:return < outcome:return`，outcome 为 STOPPED、无 completed probe code、无 frame，
  `postReturnMechanics=0`。

现有 `ProbeInputSequences` 同样要 override frozen overload；legacy overload应明确 forbidden。现有同步
`pixelProbeFailureEvidenceIsCapturedOnlyAfterCtrlRelease...` 可保留为 failure-evidence 顺序回归，但不能替代上述双线程 test。

## 6. Cloud probe-specific uncertainty tests

### 6.1 `TurnGameClientContractTest`

沿用真实 `TurnGameClient + CloudTurnActionFactory + ScriptedCommandPort + CountingUuidSupplier`，新增两个 probe action cases：

1. `pixelProbeCommandUncertaintyUsesOneUuidOneCommandAndNoBusinessResult`：分别脚本化
   `timedOutUncertain(action.actionId())` 与 `interruptedUncertain(action.actionId())`；每个 case 都提交
   `List.of(pixelChangeProbeStep())`，断言 UUID calls=`1`、execute calls=`1`、actions size=`1`、submitted/result actionId
   为同一 fixed UUID、metadata reads=`0`、outcome/frame=null。不得调用 client 第二次来“确认”。
2. `pixelProbeDuplicateOrUncertainOutcomeUsesSameActionIdWithoutCompletedEvidence`：port 返回 command
   `COMPLETED`，但真实 outcome status 为 `DUPLICATE_OR_UNCERTAIN`、stepResults empty、frame null；断言同一 actionId、
   一 UUID、一 command、零 metadata read/retry、无 completed probe code/frame。

### 6.2 `TurnCapturePixelChangeInvocationContractTest`

新增 `probeUncertaintyRejectsCompletedCodeAndAfterFrameIndependently`，必须拆成两个独立 invalid payload：

- code-only：`DUPLICATE_OR_UNCERTAIN` outcome 携带 `PIXELS_CHANGED` 或 `PIXELS_UNCHANGED` step result，frame null；
  `TurnInvocationResult.from(...)` 必须 fatal throw。
- frame-only：同 status 使用 canonical empty stepResults，但携带一份完全有效、SHA/metadata/ROI/尺寸均匹配的 after raw PNG；
  同样必须 fatal throw。

不能只构造“completed code + frame”一个组合，因为当前 `TurnInvocationResult.requireCaptureCorrelation(...)` 会先因 code
抛错，从而掩盖 frame-only uncertainty 是否被接受。frame 样例也不能故意使用错 SHA/PNG，否则测试会为无关原因通过。

## 7. 假阳性防线与范围

- 不允许新 tests 只反射 `isFrozenExactWindowCurrent()`、`sameExactWindow()`、`probeCheckpoint()` 或扫描源码字符串；每个
  核心断言都必须由 public caller -> real Queue -> real Worker 行为产生。
- 真实 focus path 必须 active，但由 recording focus fake 截断 Win32；真实 capture/keyboard/mouse 全部是内存 fake。
- `queue.size()==1` 是“request 已冻结、worker 未取走”的 barrier；drift 必须发生在该点之后，才能证明 worker gate。
- waiter interrupt 必须发生在 worker 已进入 callback 后；否则只覆盖 remove-before-start，不能证明 cleanup barrier。
- 返回后零 mechanics 使用 `postReturnMechanics` 原子计数，不用额外 sleep 猜测竞态。
- 不新增 production/test deadline semantics、TTL、retry、session、owner、ledger、compaction 或 durable workflow；不改
  protocol/JSON，不触碰 `TeamReturnService`、`NpcClickService`、Task/caller。
- frozen overload 的 production caller保持 `TurnCaptureStepExecutor`；legacy Queue/InputSequences callers不因本测试迁移。

## 8. 精确 named-test 写集

1. Create `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/input/action/InputActionFrozenExclusiveContractTest.java`。
2. Modify `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java`。
3. Modify `D:/mavenProject/DHXY/src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`。
4. Modify `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnCapturePixelChangeInvocationContractTest.java`。
5. Modify `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClientContractTest.java`。

本 helper 未写这些测试，也未执行它们。实现 Worker 应以交付时最新 production 字节适配上述 exact public method；父级后续
运行 named tests/compile 与 review gate 时，以真实事件/latch 断言为准，不接受同步 callback fake 或 private helper 反射替代。

PRECHECK_COMPLETE

<!-- TRUE_EOF: TURN-28P REPAIR-1 TEST-PREFLIGHT HELPER PRECHECK_COMPLETE 2026-07-16T05:26:40.9131549-04:00 -->
