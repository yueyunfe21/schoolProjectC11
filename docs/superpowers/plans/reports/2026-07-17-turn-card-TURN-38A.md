# TURN-38A Whole-Task Shared Foundation + Deferred Context Cleanup

## CANONICAL CARD

- status: `FOUNDATION SOURCE-START READY / ZERO OWNER / AMENDMENT-3 FROZEN`
- owner: `ZERO OWNER`
- sourceDependsOn: `TURN-13C + TURN-26 + TURN-27 + TURN-34C + TURN-40A` source review passed
- approvalDependsOn: `TURN-35 + TURN-36 + TURN-37 + TURN-38B1/B2/B3/B4 + TURN-38M`
- business baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- rule: 原卡 physical EOF 是 claim/delivery/return 唯一权威；总账不派卡。External C 可按 READY/ZERO OWNER
  canonical self-claim；领取后为本卡 sole owner，未 delivery/return 前不得第二 owner。

## Why This Card Is Open

原 DAG 把 35/36/37 设为 38A 前置，同时三张 Whole Task 又需要 38A/40B 才存在的 prepared/event/turn
Cloud owner，形成闭环。父级 Amendment #3 将同一整卡划为两个有序阶段：

1. `38A-F`：立即实现共享 foundation；通过后同时开放 TURN-35/36/37。
2. `38A-C`：待后继 caller 归零后再清 old retained authority；不阻塞 35/36/37 source start。

## 38A-F Exact Write Set

- Modify `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/dialog/CloudDialogPreparedActionState.java`
- Modify `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskTurnCoordination.java`
- Create `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudWholeTaskReadyEventState.java`
- Create `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudWholeTaskFoundationContractTest.java`
- Modify this canonical card only for claim/status/delivery evidence

No other production/test Java file is writable in 38A-F. The seven old context files belong to deferred 38A-C and
must not be modified during 38A-F.

## Frozen API And Behavior Contract

1. Prepared peek is exact-context and non-destructive. It uses the same scope and validation fence as
   `consumeValidated`; mismatch returns null without clearing or refreshing the canonical slot.
2. Turn coordination reuses the existing `CloudTaskTurnAuthority`. No second lock, queue, executor, store, retry,
   session, TTL, local input, or copied `TaskTransactionRunner` is allowed.
3. `CloudWholeTaskReadyEventState` is the sole Cloud event owner keyed by tenant/user/device/window. Its sequence is
   monotonic. It exposes publish/current/latest/latest-other-fresh-prepared/await-newer behavior.
4. `awaitNewer(afterSequence, timeout)` uses condition/signal wakeup. No poll/sleep. Timeout ends only that wait and
   does not expire stored facts. STOP remains higher priority than PAUSE; no new park/yield business condition.
5. Event production remains in TURN-35/36/37 at the exact `696a12b0` transition points. 38A-F does not start Tasks,
   infer movement/arrival, add watcher/detector, or modify protocol/HTTP/host/runtime activation.
6. Named test must exercise production public APIs for prepared positive/negative/non-consumption, exact scope,
   monotonic sequence, afterSequence, early wake, timeout, other-window exclusion, FIFO turn, forceRelease, and
   terminal cleanup. No test-local substitute owner.
7. `无已批准业务差异；按 696a12b0 等价迁移。`

## Start Snapshot

- `CloudDialogPreparedActionState.java`: SHA-256
  `169d4382df9381f8583fbcfe733b3ad5941c228173a796987f2462e6854ae940`, 11,113 bytes,
  mtime `2026-07-17T06:29:11.7801468Z`.
- `CloudTaskTurnCoordination.java`: SHA-256
  `0d86d9fee2ff5bc4fd9fa5a09bb8eef16c26a9ce90b410bc1d9fef8e02c6f883`, 1,317 bytes,
  mtime `2026-07-13T19:05:06.2103695Z`.
- `CloudWholeTaskReadyEventState.java`: absent.
- `CloudWholeTaskFoundationContractTest.java`: absent.
- `CloudTaskTurnAuthority.java` is read-only, SHA-256
  `aed690199c8fe3f5c9ee9094ebccbf5bec5c6cf762e22692b617d0ea58bdef1f`.

## Delivery Gate

Delivery must list per-file SHA/bytes/mtime, exact public methods, named-test source matrix, all remaining blockers,
and `SOURCE+TEST DELIVERED`. Parent alone performs source review. Java writer activity forbids Maven; stable cohort
may run only the authorized named test and applicable compile. No runtime/application/server/Task/UI/capture/input.

TRUE_EOF

<!-- TRUE_EOF: CR271 TURN-38A FOUNDATION-SOURCE-START-READY ZERO-OWNER AMENDMENT3-FROZEN 2026-07-17T15:02:00-04:00 -->

## EXTERNAL-C TURN-38A-F WHOLE-CARD CLAIMED - 2026-07-17T11:08:00-04:00

- Implementation Worker：**CR271 External Worker C**（同身份连续接替会话；当前 CR271 唯一在线 Worker）。非 reviewer，不自批；本段不含 `APPROVED/CLOSED`。C 当前无其它持卡（TURN-27 已 10:18 Review #2 PASSED / owner released），单卡合规。
- 领取依据：父级定向消息 `PARENT-TURN38A-FOUNDATION-READY-AMENDMENT3-DAG-REPAIR`（15:02）+ 本卡 `FOUNDATION SOURCE-START READY / ZERO OWNER`。claim 前完整读卡（54 行、单一父级 TRUE_EOF 15:02、零既有 claim、mtime 11:02:26）；预检与本 append 为两次独立调用；append 后立即回读 EOF，若发现更早 claim 立即 canonical 自撤。
- capacity: `ENOUGH_WHOLE_CARD`。
- 承担范围：**38A-F 阶段整卡** production/test/report/返修，直至父级 `SOURCE+TEST SOURCE REVIEW PASSED` 或 canonical whole-card `OWNER RETURNED`。38A-C（七个 old context 文件清理）为后置阶段，本次不触碰。
- 领取点实测（与卡 Start Snapshot 逐字节一致）：
  | 文件 | 动作 | SHA-256 | 字节 |
  |---|---|---|---:|
  | `service/dialog/CloudDialogPreparedActionState.java` | Modify | `169d4382df9381f8583fbcfe733b3ad5941c228173a796987f2462e6854ae940` | 11,113 |
  | `cloudbrain/remote/CloudTaskTurnCoordination.java` | Modify | `0d86d9fee2ff5bc4fd9fa5a09bb8eef16c26a9ce90b410bc1d9fef8e02c6f883` | 1,317 |
  | `cloudbrain/remote/CloudWholeTaskReadyEventState.java` | Create | ABSENT | — |
  | `remote/CloudWholeTaskFoundationContractTest.java`（test） | Create | ABSENT | — |
  | `cloudbrain/remote/CloudTaskTurnAuthority.java` | 只读 | `aed690199c8fe3f5c9ee9094ebccbf5bec5c6cf762e22692b617d0ea58bdef1f` | — |
- 合同遵守：prepared peek 与 `consumeValidated` 同 scope/validation fence、mismatch 返 null 不清不刷新；复用既有 `CloudTaskTurnAuthority`（零第二 lock/queue/executor/store/retry/session/TTL/local input/copied TaskTransactionRunner）；`CloudWholeTaskReadyEventState` 唯一 Cloud event owner（tenant/user/device/window 键、单调 sequence、publish/current/latest/latest-other-fresh-prepared/awaitNewer）；`awaitNewer` condition/signal 零 poll/sleep、timeout 只结束该次等待不清事实、STOP>PAUSE 不新增 park/yield 业务条件；事件生产留在 35/36/37；不启 Task、不推断 movement/arrival、不加 watcher/detector、不改 protocol/HTTP/host/runtime activation。named test 覆盖第 6 条全矩阵、经 production public API、无 test-local substitute owner。
- 纪律：零 Git mutation；Java writer 活动期不运行 Maven；稳定后仅运行授权 named test 与适用 compile；不运行 runtime/application/server/Task/UI/capture/input；保护 D 的 TURN-37 WIP 与三工作区 dirty/untracked；`D:\mavenProject\DHXY` 只读。
- **无已批准业务差异；按 696a12b0 等价迁移。**

TRUE_EOF

<!-- TRUE_EOF: TURN-38A EXTERNAL-C WHOLE-CARD CLAIMED 38A-F SOLE-OWNER SNAPSHOT-VERIFIED PREPARED=169d4382 COORD=0d86d9fe CREATES-ABSENT ANTI-RACE-TWO-CALL 2026-07-17T11:08:00-04:00 -->

## EXTERNAL-C TURN-38A-F CANONICAL WHOLE-CARD SOURCE+TEST DELIVERED - 2026-07-17T11:25:00-04:00

- Implementation Worker：**CR271 External Worker C**（sole owner，11:08 claim）。非 reviewer，不自批；本段不含 `APPROVED/CLOSED`。

### 逐文件交付清单（git-blob SHA / 字节 / 行数 / mtime）

| 文件 | 动作 | git-blob | 字节 | 行数 | mtime(UTC) |
|---|---|---|---:|---:|---|
| `service/dialog/CloudDialogPreparedActionState.java` | Modify | `34ebc507e8881039bc0f678c89f08a5c7267bc9a` | 13,919 | 244 | 15:12:47.436Z |
| `cloudbrain/remote/CloudTaskTurnCoordination.java` | Modify | `25dd960e04898d18c60a86a982c8d55e2565c986` | 4,809 | 91 | 15:12:31.133Z |
| `cloudbrain/remote/CloudWholeTaskReadyEventState.java` | **Create** | `33562b3aa8f8de2084ee45376554460ca0068073` | 14,987 | 288 | 15:13:38.536Z |
| `remote/CloudWholeTaskFoundationContractTest.java`（test） | **Create** | `77f3cca2df1cf3e1e19635c340aa83279a7a7cba` | 26,206 | 427 | 15:18:08.766Z |
| `cloudbrain/remote/CloudTaskTurnAuthority.java` | 只读未触碰 | SHA-256 保持 `aed690199c8fe3f5c9ee9094ebccbf5bec5c6cf762e22692b617d0ea58bdef1f` | — | — | — |

写集外零触碰；38A-C 七个 old context 文件未动；无第二 store/lock/queue/executor/session/TTL/watcher/poll/sleep/local input/copied TaskTransactionRunner。

### Exact public methods

1. `CloudDialogPreparedActionState.peek(TaskExecutionContext, DialogOperation, String, String, boolean)`：非破坏 exact-context 读；复用 `consumeValidated` 同一 slotKey + 同一 `mismatchReason` pre-CAS binding fence（window/HWND/intent/operation/target）；absent/mismatch 返 null，任何路径不清除、不替换、不刷新、不触 validator；consume 语义与既有实现零改动。
2. `CloudTaskTurnCoordination.run(String, TaskTransactionResult, TaskYieldPolicy, Supplier<TaskTransactionResult>)`（default）+ private static `safeRun`：基线 `TaskTransactionRunner.run` 的 Cloud Task-facing 落点——enter→action→leave(typed outcome) 恰一次包裹；null 结果→FAILED、`TaskStopRequestedException`→STOPPED(completed=true)、interrupted runtime failure→STOPPED、其余异常传播且 finally `leave(null)`=authority 全深度释放；keep/release 决策完全由既有 authority `leave` 的 `TaskTransactionResult`/`TaskYieldPolicy` 语义承担；`runExclusive` 的本地 input-worker 耦合留在 DHXY 不迁。
3. `CloudWholeTaskReadyEventState`（唯一 Cloud event owner，tenant/user/device/window 键）：`publish`（exact-scope fence：event.windowId 缺省盖章 context、异窗拒绝不存储；单调全局 sequence；PREPARED_ACTION_READY+operation 入 prepared 槽；同锁 signalAll）；`currentSequence`；`latest`；`latestOtherFreshPreparedAction`（同 lane 异窗、operation 必需、freshness 只滤不删）；`awaitNewer(afterSequence,timeout)`（condition/signal 零 poll/sleep；park 前与每次唤醒后走既有 typed checkpoint→STOP 高于 PAUSE；timeout 只结束该次等待不清事实；0 timeout invalid；publish-park 竞态经锁内复查闭合）；`clearTerminal`（唯一移除路径，仅 exact slot）。事件生产留在 35/36/37，本 state 不启 Task、不观察窗口、不推断 movement/arrival、不改 protocol/HTTP/host/runtime activation。

### Named-test source matrix（`CloudWholeTaskFoundationContractTest`，16 用例）

- prepared positive/non-consumption：`peekReturnsMatchingActionWithoutConsumingIt`（peek 两次仍在→consume 成功→peek null）
- prepared negative：`peekMismatchReturnsNullWithoutClearingTheSlot`（operation/target mismatch 均不清槽）
- exact scope：`peekIsExactContextScoped`（异窗 null、本窗保留）+ `mismatchedWindowIdPublishIsRejectedByTheExactScopeFence`
- intent fence 同 consume：`peekIntentIdFenceMatchesConsumeRouteRecoveryRule`（route cleared-intent 规则不变）
- monotonic sequence：`publishMintsStrictlyMonotonicSequences`
- afterSequence + timeout 不清事实 + 0-timeout invalid：`awaitNewerHonorsAfterSequenceAndTimeoutWithoutExpiringFacts`
- early wake（condition/signal）：`publishWakesParkedWaiterEarlyThroughConditionSignal`（真实线程 park，publish 即醒，远早于 30s timeout）
- other-window exclusion：`otherWindowEventsAreExcludedFromLatestAndAwait` + `latestOtherFreshPreparedActionSeesOnlySameLaneOtherWindows`（own-window/异 lane 排除、freshness 只读）
- STOP>PAUSE：`stopRequestedUnwindsAwaitWithTheTypedStopException`（stopRequested metadata→typed `TaskStopRequestedException`）
- terminal cleanup：`terminalCleanupDropsOnlyTheExactWindowSlot`（exact slot 全清、他窗事实保留）
- FIFO turn 包裹/typed leave：`runWrapsOnePhaseWithEnterThenActionThenTypedLeave`（enter→action→leave 恰一次、outcome 逐字段、leave 收同一 outcome）
- 基线结果映射：`runFoldsNullResultToFailedAndStopExceptionToStopped`、`runFoldsInterruptedRuntimeFailureToStopped`
- 异常全深度释放：`runReleasesAllDepthOnAnUnhandledFailure`（传播 + `leave(null)`）
- forceRelease：`forceReleaseDelegatesToTheSameAuthorityHandle`
- 所有 prepared/event 用例使用真实 production owner（`CloudDialogPreparedActionState`/`CloudWholeTaskReadyEventState`）+ 真实 `TaskExecutionContext.turnNative` + 真实 `TurnGameClient`（metadata-only port，零 turn command）；无 reflection、无 test-local substitute owner。turn 边界用例经接口自身抽象操作的 recording seam 驱动 production default `run`。

### 剩余 blockers（如实列出）

1. **共享 main compile debt（写集外，同 TURN-27 记录同源）**：授权命令 `mvn -Dtest=CloudWholeTaskFoundationContractTest test` 于 11:20-04:00 在 main compile 阶段 BUILD FAILURE（530 sources；首批缺失符号同前：`TextCandidateScanStatus`、`GameClientTracker`、`TextRecognizer`、`com.bot.dhxy.metrics`、`com.bot.dhxy.driver`、`CoordinateHelper`、`GameStateUtil`、`WindowRuntimeContext` 等）；named test 未执行，交付为 source-only。**全量 javac 错误输出中零条指向本卡四文件**（本卡源码在 530 源批量编译中自身无错误报告）。未 stub/复制/skip 绕过。
2. **authority FIFO 直测覆盖缺口**：`CloudTaskTurnAuthority`（只读）handle 仅可经 `createHandle(CloudTaskRunCurrentContextSlot)` 铸造，而 slot 仅由 old-run `CloudTaskRunAuthorityAssembly.createCurrentContextSlotActivation` 产出（AuthorityInstanceIdentity 私有构造），激活归 TURN-40B。故本卡 named test 无法在不引入 test-local substitute owner 或整套 old-run assembly 的前提下直测真实 FIFO 排队；已按 boundary 语义覆盖 enter/leave/forceRelease 合同。**提案**（父级择一）：(a) 接受 FIFO 排队直测归 TURN-40B activation 测试（handle 可铸后）；(b) 修订合同允许 `CloudTaskTurnAuthority.java` 一处窄 additive seam（接受 `Supplier<TaskExecutionContext>` 的 package-private createHandle 重载）由本卡补测；(c) 父级指定其它落点。等待裁决期间本卡其余交付物不受影响。

**无已批准业务差异；按 696a12b0 等价迁移。**

交付后进入 `AWAITING_PARENT_REVIEW`；收到 P0/P1/P2 返修立即整卡返修重走交付；不自批、不建 reviewer。

TRUE_EOF

<!-- TRUE_EOF: TURN-38A EXTERNAL-C 38A-F SOURCE+TEST DELIVERED PREPARED=34ebc507 COORD=25dd960e EVENTSTATE=33562b3a TEST=77f3cca2-427L AUTHORITY-UNTOUCHED-aed69019 BUILD-BLOCKED-SHARED-DEBT FIFO-DIRECT-COVERAGE-PROPOSAL AWAITING-PARENT-REVIEW 2026-07-17T11:25:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - 2026-07-17 11:35 EDT

结论：`P0/P1/P2 = 0/1/0`，`38A-F SOURCE+TEST SOURCE REVIEW NOT PASSED / REPAIR REQUIRED`。

### P1 - named test 没有覆盖合同要求的 production FIFO authority

- 证据：`CloudWholeTaskFoundationContractTest.runWrapsOnePhaseWithEnterThenActionThenTypedLeave`
  使用测试内 `RecordingTurn implements CloudTaskTurnCoordination`；其 `enter/leave/forceRelease` 只向
  `List<String>` 追加文本，没有调用 `CloudTaskTurnAuthority.createHandle(...)`，没有产生两个同 lane contender，
  也没有进入 authority 的 ticket、`waiters.addLast`、head reservation/grant/release 路径。
- 因而该用例只证明 default `run(...)` 的 `enter -> action -> leave` 调用顺序，不能证明计划第 17.3 节明确要求的
  `FIFO turn`。测试名称/交付说明中的“FIFO turn 包裹”把接口调用顺序误报成了 FIFO 行为覆盖。
- `CloudTaskTurnCoordination.run`、`CloudWholeTaskReadyEventState`、`CloudDialogPreparedActionState.peek` 与
  `696a12b0` 对照未发现额外 P0/P1/P2；本轮唯一返修项是缺失的真实 authority FIFO 覆盖。

### 精确返修条件

1. 保留当前三个 production 增量；不得复制第二套 lock/queue/authority，也不得用 test-local FIFO 替身。
2. 在同一 named test 家族内通过 production `CloudTaskTurnAuthority` 的真实 handle 路径建立至少三个同 lane
   owner，冻结并断言 grant 顺序、前一 holder release 后下一 waiter 才获 grant，以及 `forceRelease` 后队首继续。
3. 父级批准把本卡 test setup 写集扩展到既有 package-private old-run assembly/slot fixture；不批准向 production
   authority 增加 `Supplier<TaskExecutionContext>` 后门，也不把 required FIFO 验收推迟到 TURN-40B。若仍无法构造，
   先在总账给出所需最窄 test-only fixture 文件/符号清单，继续整卡返修。
4. 重新交付逐文件 SHA；共享 main compile debt可如实记录，但不得宣称未执行的 named test 已通过。

38A-F 在返修通过前不释放 TURN-35/36/37。

TRUE_EOF

<!-- TRUE_EOF: TURN-38A PARENT-REVIEW-1 P0-0-P1-1-P2-0 REPAIR-REQUIRED PRODUCTION-FIFO-AUTHORITY-TEST-MISSING 2026-07-17T11:35:00-04:00 -->

## EXTERNAL-C 38A-F REPAIR #1 CANONICAL WHOLE-CARD SOURCE+TEST DELIVERED - 2026-07-17T11:48:00-04:00

- 回应 `PARENT-TURN38A-F-REVIEW1-REPAIR-FIFO`（Review #1，P0/P1/P2=0/1/0）。唯一返修项=production FIFO authority 直测缺失，已闭合。

### P1 修复内容

按 Review #1 第 3 条批准的 test-setup 写集扩展，named test 新增 **production old-run assembly/slot fixture**（零 production 改动、零后门、零 test-local FIFO 替身）：`RemoteTaskRunCoordinator`（public）→ `RemoteGameCommandBroker`/`CloudTaskRunActionLedger`/`RemoteFinalConsumptionCoordinator`（同包 package-private 真实构造）→ `CloudTaskRunAuthorityAssembly.create` → 逐窗真实 lifecycle `prepare(PREPARED,rev0)` → `activate(ACTIVE,rev1)` → `confirmExecution(rev1)` → `createCurrentContextSlotActivation` 真实 slot → `CloudTaskTurnAuthority.createHandle`。新增两用例：

1. `threeSameLaneOwnersAreGrantedInFifoOrderThroughTheProductionAuthority`：同 lane（tenant-38a/device-38a）**三个真实 owner handle**；owner1 持 turn 时两 waiter 依次 admitted（真实 ticket/`waiters.addLast`/condition await，线程实测 parked）并断言**零提前 grant**；owner1 `leave(MUST_YIELD)` 释放后**仅队首** owner2 获 grant（200ms settle 后断言 owner3 仍排队）；owner2 `forceRelease` 后队首继续→owner3 获 grant；冻结完整 grant 顺序 `[first, second, third]`；两线程 join 收尾。覆盖：grant 顺序、前一 holder release 后下一 waiter 才获 grant、forceRelease 后队首继续——全部经 production authority 真实 handle/contender/grant/release 路径。
2. `runDrivesTheProductionAuthorityAcquireAndRelease`：default `run(...)` 经**真实 handle** 驱动 production authority acquire→action→typed leave；MUST_YIELD 释放后同 lane 第二 real owner 立即可 enter（若 turn 未释放则必死锁），把边界方法与真实 authority 绑定验收。
- 既有 16 用例与三个 production 增量全部保留；`RecordingTurn` 仅继续用于结果映射（null→FAILED/stop→STOPPED/异常→leave(null)）语义用例，不再承担任何 FIFO 声明；class javadoc 同步纠正。

### 逐文件交付清单（Repair #1 后）

| 文件 | 动作 | git-blob | 字节 | 行数 | mtime(UTC) |
|---|---|---|---:|---:|---|
| `remote/CloudWholeTaskFoundationContractTest.java`（test） | Repair #1 修改 | `f243bcfbf19de0c7da034c1debd6897118b4f184` | 35,053 | 571 | 15:41:00.220Z |
| `service/dialog/CloudDialogPreparedActionState.java` | 未再改 | `34ebc507e8881039bc0f678c89f08a5c7267bc9a` | 13,919 | 244 | — |
| `cloudbrain/remote/CloudTaskTurnCoordination.java` | 未再改 | `25dd960e04898d18c60a86a982c8d55e2565c986` | 4,809 | 91 | — |
| `cloudbrain/remote/CloudWholeTaskReadyEventState.java` | 未再改 | `33562b3aa8f8de2084ee45376554460ca0068073` | 14,987 | 288 | — |
| `cloudbrain/remote/CloudTaskTurnAuthority.java` 及全部 fixture 生产类 | 只读零改动 | authority SHA-256 保持 `aed69019...` | — | — | — |

### 构建门（如实）

- 稳定后重跑唯一授权 `mvn -Dtest=CloudWholeTaskFoundationContractTest test`（11:44-04:00）：仍于 main compile 阶段 BUILD FAILURE（写集外共享迁移缺类，首批同前：`TextCandidateScanStatus` 等）；named test 未执行，**不宣称已通过**；javac 全量错误输出 0 条指向本卡四文件。未 stub/复制/skip 绕过。

**无已批准业务差异；按 696a12b0 等价迁移。**

交付后进入 `AWAITING_PARENT_REVIEW`；收到返修即整卡重走；不自批、不建 reviewer。

TRUE_EOF

<!-- TRUE_EOF: TURN-38A EXTERNAL-C 38A-F REPAIR1 SOURCE+TEST DELIVERED TEST=f243bcfb-571L PRODUCTION-FIFO-3-OWNERS+FORCERELEASE-HEAD-CONTINUES RUN-THROUGH-REAL-AUTHORITY PRODUCTION-UNCHANGED BUILD-BLOCKED-SHARED-DEBT AWAITING-PARENT-REVIEW 2026-07-17T11:48:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #2 - 2026-07-17 11:47 EDT

结论：`P0/P1/P2 = 0/0/1`，`38A-F REPAIR #1 SOURCE REVIEW NOT PASSED / REPAIR #2 REQUIRED`。

### 已闭合

- Review #1 的 production FIFO 缺口已闭合：fixture 经真实 coordinator lifecycle、old-run assembly/current slot
  与 `CloudTaskTurnAuthority.createHandle` 铸造三名同 lane owner；测试进入真实 ticket/waiter/grant/release 路径，
  覆盖 holder 未释放前零 grant、FIFO head、`forceRelease` 后下一 head 与 default `run` 实 authority 链。

### P2 - 失败路径会永久挂住 named test，且线程清理不完整

- `CloudWholeTaskFoundationContractTest.runDrivesTheProductionAuthorityAcquireAndRelease` 第 459-462 行在 JUnit
  主测试线程直接调用 `second.enter(...)`。若 `run(...MUST_YIELD...)` 未释放 lane，这个断言路径没有 timeout，
  不会给出失败结论而会无限阻塞整个 named test/Maven 进程。
- `threeSameLaneOwnersAreGrantedInFifoOrderThroughTheProductionAuthority` 第 417-444 行启动两个非 daemon waiter，
  但 release/join 不在 `finally`；任一中间断言失败都会遗留等待线程，同样可能阻止测试进程结束。
- 第 338 行断言说明仍写“FIFO turn wrap”，与 Repair #1 已声明 `RecordingTurn` 不承担 FIFO 证明相矛盾。

### Repair #2 精确条件

1. 所有可能阻塞的 real-authority acquire 必须放入可观测 worker/future，并使用明确 deadline；超时应断言失败，
   不得在 JUnit 主线程无界 `enter`。
2. 两个 FIFO waiter 与 run-release probe 必须在 `finally` 中尽最大努力 `forceRelease`/countDown/interrupt/join，
   并把 worker 异常传回测试线程；任何断言失败都不得遗留非 daemon 线程。
3. 把第 338 行说明改为仅描述 default boundary 的 enter/action/typed-leave 顺序，不再称其为 FIFO 证明。
4. production 四文件继续冻结；只修改同一 named test，重新 canonical delivery。

TRUE_EOF

<!-- TRUE_EOF: TURN-38A PARENT-REVIEW-2 P0-0-P1-0-P2-1 REPAIR2-REQUIRED BOUNDED-AUTHORITY-TEST-CLEANUP 2026-07-17T11:47:00-04:00 -->

## EXTERNAL-C 38A-F REPAIR #2 CANONICAL WHOLE-CARD SOURCE+TEST DELIVERED - 2026-07-17T12:05:00-04:00

- 回应 Review #2（P0/P1/P2=0/0/1）与 `PARENT-TURN38A-F-REVIEW2-REPAIR-BOUNDED-TEST`。仅修改同一 named test；production 四文件继续冻结零改动。

### P2 修复内容（逐条对应 Review #2 精确条件）

1. **有界 acquire**：新增 `enterWithinDeadline(handle, name, timeoutMs)` helper——所有可能阻塞的 real-authority `enter` 均移入可观测 worker 线程 + `CountDownLatch.await(deadline)`；超时→interrupt worker→bounded join→`fail(...)` 明确断言失败；worker 异常经 `AtomicReference<Throwable>` 回抛测试线程。`runDrivesTheProductionAuthorityAcquireAndRelease` 的 release probe 与 FIFO 用例的首个 acquire 均改经此 helper，JUnit 主线程零无界 `enter`。
2. **finally 清理**：两用例全部包 try/finally——`releaseAndJoin(waiter)`（countDown→interrupt 残留 authority wait→bounded join）+ `bestEffortForceRelease` 逐 handle（不持有时按合同 no-op，catch 不掩盖原始失败）；`TurnWaiter` 增 `failure` 引用，worker 全身 try/catch(Throwable) 捕获并于成功路径 `assertNull` 回传断言；任何断言失败路径均不遗留非 daemon 线程或被持 lane。
3. **措辞纠正**：`runWrapsOnePhaseWithEnterThenActionThenTypedLeave` 断言消息改为"per-phase boundary order only; FIFO behavior is proven against the production authority separately"，不再称 FIFO 证明。
4. production 冻结确认：`34ebc507`/`25dd960e`/`33562b3a` 与 authority `aed69019` 全部未动。

### 交付清单（Repair #2 后）

| 文件 | 动作 | git-blob | 字节 | 行数 | mtime(UTC) |
|---|---|---|---:|---:|---|
| `remote/CloudWholeTaskFoundationContractTest.java`（test） | Repair #2 修改 | `6292706a5713188fd36b8e481a4f2277e2ca7cbe` | 39,133 | 649 | 15:51:47.821Z |
| 三 production 增量 + authority | 冻结零改动 | `34ebc507`/`25dd960e`/`33562b3a`/SHA-256 `aed69019` | — | — | — |

### 构建门（如实）

- 重跑唯一授权 `mvn -Dtest=CloudWholeTaskFoundationContractTest test`（11:55-04:00）：仍 main compile BUILD FAILURE（写集外共享迁移缺类同前）；named test 未执行、不宣称通过；javac 全量错误 0 条指向本卡四文件。

**无已批准业务差异；按 696a12b0 等价迁移。**

交付后进入 `AWAITING_PARENT_REVIEW`；收到返修即整卡重走；不自批、不建 reviewer。

TRUE_EOF

<!-- TRUE_EOF: TURN-38A EXTERNAL-C 38A-F REPAIR2 SOURCE+TEST DELIVERED TEST=6292706a-649L BOUNDED-ENTER-DEADLINE FINALLY-CLEANUP WORKER-FAILURE-PROPAGATION WORDING-FIXED PRODUCTION-FROZEN BUILD-BLOCKED-SHARED-DEBT AWAITING-PARENT-REVIEW 2026-07-17T12:05:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #3 PASSED - 2026-07-17T12:12:00-04:00

- 结论：`P0/P1/P2 = 0/0/0`，`TURN-38A-F SOURCE+TEST SOURCE REVIEW PASSED`。
- Repair #2 逐项闭合 Review #2：所有可能阻塞的 production-authority `enter` 均通过
  `enterWithinDeadline` 的 worker + deadline 执行；超时会 interrupt、bounded join 并明确失败，worker 异常会回传
  测试线程。FIFO waiter 与 run-release probe 均有 `finally` 清理，包含 release、interrupt、bounded join 与
  best-effort force release；成功路径断言 waiter 异常为空。RecordingTurn 断言也已改为只证明 boundary order。
- 复核实盘 test SHA-256=`d76185627fd9c364ea5bafcea96887d6bb6eb97198ac02fbcbbbf3234546cb6d`，
  39,133 bytes / 649L，mtime `2026-07-17T15:51:47.821Z`；production 三增量与 authority 保持冻结。
- 构建状态独立保留为 `BUILD BLOCKED BY SHARED MAIN-COMPILE DEBT`：授权 named test 因写集外缺类未进入
  test execution，不能宣称测试通过；该共享债不改变本轮 source review 的 `0/0/0`。
- 依 Amendment #3，38A-F source gate 已满足，TURN-35/36/37 同时开放为 `READY / ZERO OWNER`。本卡 owner
  不释放：External C 继续持有 TURN-38A，后置 38A-C cleanup 等 35/36/37 与 caller gate 后再执行。

<!-- TRUE_EOF: TURN-38A PARENT-REVIEW-3 PASSED P0-0-P1-0-P2-0 FOUNDATION-F-SOURCE-GATE-PASSED 35-36-37-READY C-RETAINS-38A-FOR-CLEANUP BUILD-BLOCKED-SHARED-DEBT 2026-07-17T12:12:00-04:00 -->

## PARENT PLAN-CONTRACT REPAIR #4 - PHASE OWNER RELEASE - 2026-07-17T12:25:00-04:00

- 发现并修复原合同依赖环：原文一处要求 `38A-C` 等 35/36/37 与 38B/38C caller 归零，另一处又要求
  先完成 38A-C 才开放 38B，导致 External C 持有已通过阶段空等、38B 永远不能促成 caller 归零。
- `38A-F` 已由 Parent Review #3 `P0/P1/P2=0/0/0` 完成并关闭；External C 对该阶段的 canonical owner
  **现已释放**。C 不再持有 TURN-38A，可从 TURN-36/37 等 `READY / ZERO OWNER` 原卡自行防竞态领取完整卡。
- `38A-C` 改为独立后置 cleanup phase：当前状态 `DEFERRED / ZERO OWNER / NOT READY`。只有 35/36/37 与
  38B/38C 的 old-authority caller 真实归零、test ownership 冻结后，父级才在本卡追加 READY；届时由任一有容量
  Worker 重新 canonical claim，不预留给 C，也不阻塞 C 当前容量。
- 38A-C 原七文件写集、无业务差异边界与 cleanup 验收不变；本修复只改 DAG/owner 生命周期，不改 Java。

<!-- TRUE_EOF: TURN-38A PARENT-PLAN-CONTRACT-REPAIR-4 38A-F-CLOSED OWNER-RELEASED 38A-C-DEFERRED ZERO-OWNER NOT-READY WAIT-CALLERS-ZERO C-FREE-TO-CLAIM-READY-CARD 2026-07-17T12:25:00-04:00 -->
