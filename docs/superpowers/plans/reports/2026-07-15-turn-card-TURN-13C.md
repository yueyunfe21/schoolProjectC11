# CR271 TURN-13C - Turn-native Task execution context bridge

- card: `TURN-13C`
- state: `READY / PARENT BRIEF FROZEN`
- role boundary: implementation worker is not reviewer; only the CR271 parent may approve/block.
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- dependencies: `TURN-02R`, `TURN-13G`, `TURN-13H` source reviews passed.

## Parent preflight decision

The original five-file production write set is insufficient. Both non-binding helpers independently confirmed that
`LegacyTaskExecutionTurnContextProvider` still calls old `getScope().deviceId()`, so a context constructed without a
`RemoteTaskRunScope` cannot reach `TurnGameClient`. The parent therefore freezes the additive correction below.

This card is API/test constructible only. The first production Task factory/caller remains `TURN-40B`; no Task runtime,
queue, START/ACK endpoint, host activation or caller cutover is moved into this card.

## Frozen write set

Cloud production:

1. `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`
2. `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContextHolder.java`
3. `src/main/java/com/bot/dhxy/runner/stop/TaskCheckpoint.java`
4. `src/main/java/com/bot/dhxy/runner/stop/TaskCheckpointDecision.java`
5. `src/main/java/com/bot/dhxy/runner/stop/TaskSleep.java`
6. `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/LegacyTaskExecutionTurnContextProvider.java`
7. `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClient.java`

Cloud test:

8. `src/test/java/com/yueyunfe/dhxy/cloudbrain/runner/context/TaskExecutionContextTurnContractTest.java`

Process:

9. this report only.

Every other Cloud/DHXY production, test, POM, property, plan, document and Git path is read-only.

## Frozen implementation contract

1. Keep `public TaskExecutionContext(CloudTaskServiceExecutionContext)` and every existing public method/signature.
   Its legacy delegate behavior must remain unchanged; this card does not delete old authority.
2. Add a public static turn-native factory whose parameters are exactly the existing powerless values
   `CloudServiceScope`, `TurnInvocationContext`, `TurnWindowMetadata`, `CloudTaskServiceMetadata`, nonblank diagnostic
   `taskRunId`, and the existing singleton `TurnGameClient`. No parameter may be a `RemoteTaskRun*`, owner, session,
   ledger, permit or retained-action type.
3. The factory validates exact service scope, device/window identity, initial metadata identity, nonblank task identity,
   and `taskMetadata.taskCode() == task type`. It stores no old delegate and creates no bean/thread/loop/client transport.
4. Add `getTurnServiceScope()`, `getTurnInvocationContext()` and `getTurnGameClient()`. Legacy contexts derive the first
   two from their existing immutable delegate; a new context returns its frozen values.
5. `TurnGameClient.bind(TurnInvocationContext expected)` returns a non-bean, no-thread bound view reusing the same
   provider, action factory, command port and UUID supplier. Before UUID creation, command execution or metadata read,
   every bound public invocation must compare the provider's current identity with `expected` and fail before the port
   on missing/wrong-thread/wrong-nested context. It must not create a second Spring client/port/exchange or add retry.
6. `LegacyTaskExecutionTurnContextProvider` reads only `context.getTurnInvocationContext()`. It keeps Holder-only,
   no-cache, no-window-fallback behavior. Holder must not inject `TurnGameClient` or `ObjectProvider<TurnGameClient>`.
7. New-context checkpoints read only the bound client's latest in-memory `TurnWindowMetadata`. Empty metadata or
   device/window mismatch is a typed fail-closed transition; it is never ACTIVE and is not retried as a business action.
   STOP has priority. PAUSE preserves the `696a12b0` cooperative 250ms checkpoint cadence until resume/stop and returns
   actual blocked milliseconds. This loop creates no action, network retry, Task park/yield, TTL or replacement context.
8. Restore `isPauseRequested()` and the existing DHXY public `TaskCheckpoint`/`TaskSleep` overloads and interrupt
   behavior. Null explicit context remains valid for legacy/debug paths and only checks interruption. Interrupted flags
   are restored; STOP/interruption maps to `TaskStopRequestedException`.
9. `TaskCheckpointDecision` keeps its current canonical record/accessors for legacy callers. Only powerless turn
   factories may be added, using revision `-1` and null old status; do not change the record shape in this card.
10. Old-authority-only methods on a turn-native context (`getScope`, `revalidate`, old game/service clients and retained
    pending state) must throw a precise `IllegalStateException`. They must not return null/fabricated success, create a
    `RemoteTaskRunScope`, or fall back to a global window. Common task metadata/window helpers remain usable.

## Required named contract test

`TaskExecutionContextTurnContractTest` must directly cover:

- turn-native construction without creating any old scope/gate/authorization/owner/session/ledger;
- exact service scope/device/window/task metadata and all null/blank/mismatch fail-fast cases;
- Holder outside/inside/nested/exception restoration and provider exact identity;
- bound client exact context, one UUID/one command for each status, and pre-port rejection on missing/wrong context;
- ACTIVE, STOP, PAUSE/resume, pause-to-STOP, missing metadata and identity mismatch checkpoint behavior;
- baseline null/holder/explicit+holder checkpoint overloads, `TaskSleep` nonpositive/normal/interrupted behavior and
  interrupt-flag preservation;
- reflection/compile coverage that the legacy constructor and old public Task/Service signatures remain present;
- no host/application/Task/loop/thread/input/capture activation and no retry/TTL/second exchange.

Required parent-run commands after all Java writers are stable:

```text
mvn -q -Dtest=TaskExecutionContextTurnContractTest test
mvn -q -DskipTests compile
```

No isolated compiler or Surefire bypass may be used to hide shared compile debt.

**无已批准业务差异；按 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: TURN-13C parent brief -->

## CLAIMED - 2026-07-15 20:06:45 EDT

- owner: `CR271 TURN-13C implementation Worker (Codex)`
- role: implementation Worker, not reviewer
- exact write set:
  1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`
  2. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/runner/context/TaskExecutionContextHolder.java`
  3. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/runner/stop/TaskCheckpoint.java`
  4. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/runner/stop/TaskCheckpointDecision.java`
  5. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/runner/stop/TaskSleep.java`
  6. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/LegacyTaskExecutionTurnContextProvider.java`
  7. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClient.java`
  8. `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/runner/context/TaskExecutionContextTurnContractTest.java`
  9. `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-13C.md`
- all other Cloud/DHXY Java, test, POM, property, documentation, Git, target and runtime paths remain read-only.
- gates: no Maven/test/compile/runtime/application/server/Task/loop/UI/capture/input; parent runs named gates after all Java writers stabilize.

<!-- TRUE_EOF: TURN-13C implementation claim -->

## SOURCE+TEST SOURCES DELIVERED / PARENT REVIEW PENDING - 2026-07-15 20:27:54 EDT

### Production source delivered

- `TaskExecutionContext` now has mutually exclusive legacy-delegate and turn-native paths. The public
  `turnNative(CloudServiceScope, TurnInvocationContext, TurnWindowMetadata, CloudTaskServiceMetadata,
  String, TurnGameClient)` factory freezes powerless scope/identity/window/task values and stores an exact bound
  view of the existing singleton client. Common task/window metadata, including `getNativeWindowTitle()`, remains
  available from the frozen turn metadata; old authority-only APIs fail with precise `IllegalStateException` on the
  new path.
- Turn-native stop/pause checkpoints read only the bound client's latest in-memory metadata. Missing metadata,
  device mismatch and window mismatch produce powerless typed decisions with revision `-1` and null old status;
  STOP wins over PAUSE, and PAUSE retains the `696a12b0` cooperative 250ms cadence and returns measured blocked ms.
- `TaskCheckpoint` restores explicit-context, holder, explicit+holder and interruption public entry points with the
  baseline null-context and interrupt-flag semantics. `TaskSleep` restores `sleep(long)`, `sleepOrStop(...)` and
  `throwIfStopRequested(...)` without retry or a second wait/action.
- `LegacyTaskExecutionTurnContextProvider` now reads only `context.getTurnInvocationContext()` and retains
  Holder-only/no-cache/no-fallback behavior. Holder remains byte-preserved and has no client/ObjectProvider injection.
- `TurnGameClient.bind(expected)` creates only a non-bean bound view reusing the same provider/action factory/command
  port/UUID supplier. Every action/local-Service/metadata public invocation checks exact current identity before UUID
  allocation or port access; no second Spring client, exchange, retry, thread or lifecycle state was added.

### Named test source delivered

- Added the sole allowed `TaskExecutionContextTurnContractTest` with direct construction validation, exact scope and
  identity checks, Holder outside/nested/exception restoration, provider bridge, pre-port bound-client rejection,
  one UUID/one command for COMPLETED/FAILED/STOPPED/UNCERTAIN, ACTIVE/STOP/PAUSE/resume/pause-to-STOP/missing/mismatch
  checkpoints, baseline overload/null/interrupt/sleep behavior, legacy signature reflection, old-authority fail-closed
  checks and inert construction checks.
- Pause cases use scripted metadata on the JUnit thread; the test creates no helper thread and starts no host,
  application, server, Task, loop, UI, capture or input path.

### SHA-256

- `6d4e4a20a6fb4b6dba6a59cb45e95dd39c78a0415b9b2a650d75f9704151d003`
  `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`
- `3fa2729917449fbb75bf72614e46a223526ea2acb53dc96351886559192c6f3b`
  `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContextHolder.java` (byte-preserved)
- `3f92df4932c0d4c62556f121f7a056fc21cca9c52f0c0c9ade16b009d9a46e0f`
  `src/main/java/com/bot/dhxy/runner/stop/TaskCheckpoint.java`
- `cb59eeb4bec8cd4ef0a5aa3c0e770269f25bf77c0c4fe49095f6ff728db99de2`
  `src/main/java/com/bot/dhxy/runner/stop/TaskCheckpointDecision.java`
- `7942011cac9053edfdec6c57251758398261d85fc4488662198e92aa35a08c44`
  `src/main/java/com/bot/dhxy/runner/stop/TaskSleep.java`
- `96827e3179054df7878d45f9d56b7955f64dd91c25526e1b2afeb60167008a8b`
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/LegacyTaskExecutionTurnContextProvider.java`
- `a8f64d8dbb5f9ed2852975d518836e25af92073f9c818d5f7e9da7cf18056cb9`
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClient.java`
- `d667d6958dbc38a6fccf2ba5e562cecd4ef60629df7a4cd55e347c9dbd9ed945`
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/runner/context/TaskExecutionContextTurnContractTest.java`

### Static self-check, limits and gates

- Static self-check only: reread all eight source files; checked current Cloud callers for the three
  `TaskCheckpoint` forms, `TaskSleep.sleep(...)`, and `getNativeWindowTitle()`; checked that no write-set-external
  source, POM, property, Git or `target` path changed and no runtime path started.
- The frozen six-parameter factory has no independent `taskType` parameter. The new path therefore defines exact
  task type from validated `taskMetadata.taskCode()` and makes `getTaskType()` identical to that value; there is no
  second task-type input from which to fabricate a mismatch check.
- The legacy delegate model has no window-title field. Its newly restored `getNativeWindowTitle()` signature therefore
  fails closed on the legacy path instead of returning null/fabricated text; the turn-native path returns the frozen
  initial `TurnWindowMetadata.windowTitle()` required by Xiuluo/TeamReturn callers.
- Per parent instruction, **not run**: `mvn -q -Dtest=TaskExecutionContextTurnContractTest test`,
  `mvn -q -DskipTests compile`, any Maven/JUnit/isolated compiler command, runtime/application/server/Task/loop/UI/
  capture/input, or any Git mutation. These gates remain for the stable-writer parent cohort.

**无已批准业务差异；按 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: TURN-13C source and test source delivery -->

## PARENT SOURCE+TEST-SOURCE REVIEW #1 - 2026-07-15 20:33 EDT

- 父级逐文件独立复读七个 production 文件与完整 named test，并与
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的 `TaskCheckpoint` / `TaskSleep` 语义直接比较。
- 结论：`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。exact context identity、bound
  `TurnGameClient` port 前拒绝、ACTIVE/PAUSE/STOP/missing metadata、旧 public API 保留、STOP 优先与 250ms
  pause cadence 均符合冻结合同；未发现 retry、TTL、第二 exchange、线程、runtime 或旧 authority 回流。
- `getNativeWindowTitle()` 在 legacy delegate 没有标题数据时明确 fail-closed；turn-native 路径返回冻结的 exact
  window title。六参数 factory 以已校验的 `taskMetadata.taskCode()` 作为 task type，没有伪造第二个 task-type 输入。

### Parent-run Maven evidence

1. `mvn -q -Dtest=TaskExecutionContextTurnContractTest test`：exit 1，JUnit 运行数 0；在主源码编译阶段先被
   TURN-13C 写集外的 Cloud 旧 Service/Task 缺类阻断，首批包括 `TaskTrackerPanelService` 缺
   `GameClientTracker` / `TextRecognizer` / `CoordinateHelper` / `OcrWindowScanService` /
   `WindowScopedTempPath`，以及其他旧整类对 DHXY-only 类型的引用。
2. `mvn -q -DskipTests compile`：exit 1；Cloud enforcer `require-tests-enabled` 明确禁止 `skipTests=true`。
3. `mvn -q compile`：exit 1；与第 1 项相同的写集外主源码缺类债，未到达本卡 named test。

因此本卡释放源码 owner，状态为
`SOURCE+TEST SOURCE REVIEW PASSED / NAMED TEST+CLOUD COMPILE BLOCKED BY SHARED CLOUD COMPILE DEBT`；这不是
`CARD APPROVED/CLOSED`，也不是 TURN-13C 返修项。禁止为过门复制本地 runtime 或扩大本卡写集；后续业务切流卡
按已满足的 start dependency 继续滚动，构建债在相应 cutover cohort 真实消除后复验。

**无已批准业务差异；按 `696a12b0` 等价迁移。**

<!-- TRUE_EOF: TURN-13C parent source and test-source review -->
