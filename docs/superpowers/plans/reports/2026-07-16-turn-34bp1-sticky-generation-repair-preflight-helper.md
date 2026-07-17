# CR271 TURN-34BP1 Sticky Generation Repair Preflight Helper

> 快照截止：`2026-07-16T11:20:22.9792130-04:00`  
> 角色：CR271 Internal helper；不是 implementation owner、reviewer、approver 或父级。  
> 结论边界：`PRECHECK_ONLY / CONDITIONAL_REPAIR_BOUNDARY`。本文不批准、不阻断 TURN-34BP1。

## 1. 只读边界与当前快照

本轮已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、权威计划第
14-19 节、HTTPS turn 协议、`docs/业务逻辑.md`、两仓 `git status`、TURN-34BP1 子卡至 physical true EOF，
以及当前 `TaskExecutionContext.java` / `TaskExecutionContextTurnContractTest.java` 全文。为确认公共接点，另只读
`TaskCheckpoint`、`TaskExecutionContextHolder`、`TaskCheckpointDecision/Outcome`、`TurnGameClient`、
`TurnWindowMetadata` 和 `TaskMaintenanceService` 的 checkpoint 调用点。

| Repo | Branch | 状态摘要 |
|---|---|---|
| DHXY | `thin-client-design` | 大量既有 tracked dirty 与 untracked；本 helper 不触碰 |
| Cloud | `navigation-migration` | 大量既有 tracked dirty 与 untracked；本 helper 不触碰 |

本 helper 初始取证时，TURN-34BP1 delivery 字节与当时子卡 true EOF 一致：

| File | Lines | SHA-256 |
|---|---:|---|
| `TaskExecutionContext.java` | 502 | `05bbfda35e5471748f754c3f1e0be9b3eddc7065fd09a0cb58c72ab1322b7d99` |
| `TaskExecutionContextTurnContractTest.java` | 829 | `2af2c0aefedf5eb3e837757632d9892d11b3be8772721c6d275baadd5bd63385` |

未修改 Java/POM/协议/卡片/其它报告，未运行 Maven/JUnit/compile/package，未启动 runtime/application/server/
Task/UI/capture/input，未执行 Git mutation。

### 1.1 取证期间的并发 owner 更新

本报告初稿完成后，子卡出现了新的有权父级/owner 记录：父级于 `11:15` 明确冻结 Repair #1 边界，External C
于 `11:21:17` 在 physical true EOF 领取返修。随后两份 Java 出现新的 active WIP；本 helper 在
`11:23:06-04:00` 复读时，子卡真尾仍是 `EXTERNAL-C REPAIR #1 CLAIMED`，不是新的
`SOURCE+TEST DELIVERED`。

这些并发字节不是本 helper 的修改，也不应回退。父级冻结的 Repair #1 恰好点名本报告预检的四个边界：
context-local monotonic latch、per-context synchronization、只有 title/HWND/process 置 sticky、同一 initial-A
context 的 A0->B->A' 三次 public checkpoint 与零 command/action/UUID 证据。下文因此继续记录**最小边界**，
不把正在变化的 owner WIP 当作 delivery snapshot，也不对其给出 review/approval/blocking 结论。

## 2. 条件性 A -> B -> A' 复算

若父级把冻结合同解释为“同一个 initial-A context 实际观察到 B 后，值重新等于 A 的 A' 仍不得通过”，当前
delivery 的逐次 equality 不保存历史：

```text
context initial = A
latest A0       = A 的独立等值对象 -> pass
latest B        = 同 device/window、native 三元组不同 -> WINDOW_MISMATCH
latest A'       = A 的另一个独立等值对象 -> 当前代码再次 pass
```

原因位于 `TaskExecutionContext.java:412-440`：每次只把 latest 的 `windowTitle/nativeHandle/processId` 与 initial
值比较；B 抛出异常后，context 内没有单调失效状态。

当前测试 `TaskExecutionContextTurnContractTest.java:443-457` 不是上述历史。它创建 initial-B context，只给该
context 一个 A' slot 并断言 mismatch；随后再建一个新的 initial-B context 证明 B pass。它没有让同一个
initial-A context 依次消费 A0、B、A'。本文只记录这项条件性机械差异，不给 severity 或审查结论。

## 3. 线程安全最小状态

若父级要求修复，最小 production 状态是一枚 **private、context-local、单调** 的 boolean，例如：

```java
private boolean turnNativeWindowGenerationInvalidated;
```

并把现有 `latestExactTurnMetadata()` 原位改为 `synchronized`；不新增 helper/wrapper。方法内部顺序固定为：

1. 读取一次 `latestWindowMetadata()`。
2. 保留既有 `missing -> device -> logical window` 检查及 typed outcome 顺序。
3. 比较 initial/latest 的 `windowTitle/nativeHandle/processId`。
4. 任一 native 项漂移时只做一次单调 `false -> true`。
5. 只要该位为 true，即抛现有
   `TaskCheckpointTransitionException(TaskCheckpointDecision.turnWindowMismatch())`。
6. 不提供 reset；A' 即使与 initial 值相等也仍返回 `WINDOW_MISMATCH`。

建议形状：

```java
private synchronized TurnWindowMetadata latestExactTurnMetadata() {
    // existing one-slot read + missing/device/window checks
    if (!initialTurnWindowMetadata.windowTitle().equals(metadata.windowTitle())
            || !initialTurnWindowMetadata.nativeHandle().equals(metadata.nativeHandle())
            || initialTurnWindowMetadata.processId() != metadata.processId()) {
        turnNativeWindowGenerationInvalidated = true;
    }
    if (turnNativeWindowGenerationInvalidated) {
        throw new TaskCheckpointTransitionException(
                TaskCheckpointDecision.turnWindowMismatch());
    }
    return metadata;
}
```

`synchronized` 让同一 context 的 latest read、比较和 latch 更新形成可审查的线性化点，并提供跨线程可见性；
pause 的 250ms sleep 在该方法外，不会持锁睡眠。先完成 checkpoint 的调用可线性化在 B 被观察之前；B 已被该
context 观察并置位后，后续调用不能复活。无需 `static` map、generation counter、metadata history、session、ledger、
TTL、retry 或第二 authority。

类级 JavaDoc 需要把“只保存 immutable 值”修正为“immutable 值加 powerless monotonic safety latch”，避免注释与
真实状态不一致。

## 4. 哪些 mismatch 应 sticky

本最小修复只覆盖冻结卡点名的 **native generation 三元组**，不得顺手扩大既有语义：

| 最新事实 | 本修复是否置 sticky 位 | 保持的结果 |
|---|---|---|
| `windowTitle` 漂移 | 是 | `WINDOW_MISMATCH` |
| `nativeHandle` 漂移 | 是 | `WINDOW_MISMATCH` |
| `processId` 漂移 | 是 | `WINDOW_MISMATCH` |
| metadata missing | 否 | 既有 `MISSING_BINDING` |
| `deviceId` 漂移 | 否 | 既有 `IDENTITY_OR_SESSION_MISMATCH` |
| logical `windowId` 漂移 | 否 | 既有 `WINDOW_MISMATCH`，但不扩大为本卡 generation latch |
| `windowRect` 改变 | 否 | 允许窗口移动/缩放，不属于 generation |
| `pauseRequested` 改变 | 否 | 保留可恢复的 pause 语义 |
| `stopRequested` / interrupt | 否 | 保留现有 stop/unwind 语义，不改造成 generation 状态 |

即使 latch 已置位，方法仍先执行既有 missing/device/window 分类；当 exact logical metadata 再次出现时，latch
继续拒绝 A'。若父级希望 device/window mismatch 也永久粘滞，那是超出本 A->B->A' native-generation 修复的
合同扩展，应先由父级明确，不应由实现者顺手加入。

协议没有显式 generation/sequence。若 A 直接变为所有字段完全等值的 A'，且 B 从未进入 latest slot，两文件内
无法识别；本修复只保证“context 已实际观察到 B”后的不可复活。

## 5. Public checkpoint 可达性

无需修改 Service、TaskCheckpoint 或 Holder：

1. `TaskExecutionContext.turnNative(...)` 在 production `:96-110` 冻结 initial metadata。
2. public `TaskExecutionContext.throwIfStopRequested()` 在 `:259-271` 进入
   `checkpointTurnMetadata()`，后者在 `:385-410` 首先调用 `latestExactTurnMetadata()`。
3. public `TaskCheckpoint.throwIfStopRequested(context, ...)` 直接调用上述入口；holder overload 经
   `TaskExecutionContextHolder.checkpointIfPresent()` 到达同一路径；combined overload 保持 explicit 后 holder 的
   原顺序。
4. `TaskMaintenanceService.runOpportunisticMaintenance(...)` 在 `:581-585` 先执行现有 checkpoint；其
   `:983-986` 调 public `context.throwIfStopRequested()`，早于 `handleMaintenanceBroadcast` 和 Summon delegate。
5. `TurnGameClient.latestWindowMetadata()` 只读 command port metadata；该路径不调用 UUID supplier，也不执行
   `CloudTurnCommandPort.execute(...)`。

因此最小修复不需要新 public API、private checkpoint seam、Service wrapper、protocol 字段或第三个 Java 文件。

## 6. 精确 named-test 序列

将当前 `valueEqualRebindDoesNotReviveTheRetiredNativeGeneration` 改为同一个 initial-A harness 的真实三段历史：

1. 创建 `initialA`、独立对象 `observedA0`、native 三元组不同的 `generationB`、独立对象 `reboundA`。
2. 断言 `observedA0.equals(reboundA)` 且 `observedA0 != reboundA`。
3. 对同一 port 一次 script：`[A0, B, A']`。
4. 在同一个 holder binding 内，第 1 次调用 public
   `TaskCheckpoint.throwIfStopRequested(context, "generation A")`，断言返回 `0L`。
5. 第 2 次同一 public 调用，断言 `TaskCheckpointTransitionException`，decision 为
   `WINDOW_MISMATCH`。
6. 第 3 次同一 public 调用，断言仍为 `WINDOW_MISMATCH`；不得返回 `0L`。
7. 断言第三个 slot 确实被消费：`metadataReads == 3`、script deque 为空，并可用 `assertSame(reboundA,
   lastMetadata.orElseThrow())` 钉住 A'。

全序列必须同时断言：

```text
executeCalls == 0
actions.isEmpty()
uuids.calls == 0
metadataReads == 3
metadataScript.isEmpty()
```

`metadataReads == 3` 对应三次显式 public checkpoint，不是内部 retry；零 `executeCalls/actions/uuids` 证明在首个
delegate/actionId/command 前短路。

其余最小测试补强：

- 保留 exact、missing、device、logical window、title、HWND、process 独立 case。
- 单项 negative helper 除现有 typed decision、`executeCalls==0`、`metadataReads==1` 外，补
  `uuids.calls==0`、`actions.isEmpty()` 和单-slot exhaustion。
- exact pass case补 `uuids.calls==0`、`actions.isEmpty()`，不得只断言零 command。
- stop/pause、250ms cadence、legacy public surface 与既有 8 tests 不删除、不弱化。
- 新 A->B->A' 证明不反射 private checkpoint/latch，不做 source scan、wall-clock race、sleep 或 runtime。

当前 named test 为注入 `CountingUuidSupplier` 保留了冻结前已存在的 `TurnGameClient` constructor reflection seam。
本修复无需新增 reflection；若父级把“no reflection”解释为必须删除整文件既有 seam，则零 UUID 直接计数与两文件
写集存在合同张力，应由父级先明确，不能删掉 UUID 断言规避。

## 7. 条件性最小写集

若父级要求返修，技术上只需：

| File | 最小增量 |
|---|---|
| `TaskExecutionContext.java` | 一枚 private monotonic boolean；现有 latest 方法原位同步、置位和 sticky typed reject；同步类注释 |
| `TaskExecutionContextTurnContractTest.java` | 同一 initial-A context 的 A0->B->A' 三 slot/三 public call；零 UUID/action/command/read-retry 断言 |
| TURN-34BP1 子卡 | 由有权角色 append-only 记录返修领取/交付与最终 SHA |

不需要修改 `TaskMaintenanceService`、`TaskCheckpoint*`、`TaskExecutionContextHolder`、`TurnGameClient`、
`TurnWindowMetadata`、protocol、POM、DHXY Java 或第三个测试类。本 helper 不执行这些修改，也不形成返修指令。

**无已批准业务差异；按 `696a12b0`、exact-window generation 与最小 HTTPS JSON turn 等价迁移。**

TRUE_EOF PRECHECK_COMPLETE
