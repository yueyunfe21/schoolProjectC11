# CR271 TURN-34BP1 Replacement 精确 Implementation Preflight

> 快照截止：2026-07-16T10:01:47.6854584-04:00  
> 身份：CR271 Internal helper；不是源码 owner、reviewer 或最终裁决者。  
> 唯一写入：本报告。未修改 Java、POM、卡片、权威计划、`ACTIVE_WORK.md` 或其它文件；未运行 Maven/JUnit、runtime/application/server/Task/UI/capture/input；未执行 Git mutation。

## 1. 已完整读取的权威材料

1. `D:/mavenProject/DHXY/AGENTS.md` 全文。
2. `docs/DHXY_CONTEXT.md` 全文，含顶部 CR271 HTTPS turn 计划与测试门。
3. `docs/ACTIVE_WORK.md` 顶部最新 CR271 段；当前真尾明确旧 D assignment 已撤销，TURN-34BP1 为零 owner 的 replacement 小片。
4. 权威计划 `2026-07-15-https-turn-complete-migration-card-plan.md` 第 14-19 节全文。
5. HTTPS turn 协议 `2026-07-15-https-turn-thin-client-protocol-design.md` 全文。
6. `docs/业务逻辑.md` 全文，尤其业务基线门 `:213-224` 与修罗 `696a12b0` 失败/停止合同 `:1253-1299`。
7. TURN-34BP1 子卡全文及其最新 physical EOF。
8. Cloud 当前 `TaskExecutionContext.java` 491 行与 `TaskExecutionContextTurnContractTest.java` 753 行全文。
9. 为核实公共接点而只读了 `TaskCheckpoint.java`、`TaskExecutionContextHolder.checkpointIfPresent()`、`TaskCheckpointDecision.java`、`TaskCheckpointOutcome.java`、`TurnWindowMetadata.java`、`TurnGameClient.latestWindowMetadata()`、`CloudTurnCommandPort.latestWindowMetadata()` 及 `TaskMaintenanceService` 的首 checkpoint 调用点。

## 2. 两仓只读状态快照

| Repo | Branch / HEAD | Upstream | `status --porcelain -uall` |
|---|---|---|---|
| DHXY | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` | 未配置 | 44 个 tracked 状态行（43 modified、1 deleted）+ 658 个 untracked 行 |
| Cloud | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | 未配置 | 9 个 tracked modified 状态行 + 541 个 untracked 行 |

TURN-34BP1 两个目标仍精确等于子卡冻结快照：

| File | Git 可见性 | Lines | SHA-256 |
|---|---|---:|---|
| Cloud `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java` | untracked；HEAD 中不存在 | 491 | `6d4e4a20a6fb4b6dba6a59cb45e95dd39c78a0415b9b2a650d75f9704151d003` |
| Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/runner/context/TaskExecutionContextTurnContractTest.java` | 被 `.gitignore:15` 的 `src/test/` 忽略；HEAD 中不存在 | 753 | `d667d6958dbc38a6fccf2ba5e562cecd4ef60629df7a4cd55e347c9dbd9ed945` |

因此这两个文件没有可用的 `git show HEAD:<path>` pushed-file baseline；本小片的精确字节基准只能使用父级子卡冻结 SHA。业务语义仍受 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 约束。

## 3. 现有 Public Checkpoint 接点

1. `TaskExecutionContext.turnNative(...)`（production `:96-110`）已冻结 `initialTurnWindowMetadata`，并保存 exact-context bound `TurnGameClient`。
2. 目标公共入口已经存在：`TaskExecutionContext.throwIfStopRequested()`（`:259-271`）在 turn-native 路径进入 `checkpointTurnMetadata()`（`:385-410`），后者首先调用 `latestExactTurnMetadata()`（`:412-430`）。无需新增 public API 或 wrapper。
3. `TaskCheckpoint.throwIfStopRequested(...)` 的 explicit-context、holder、combined 三个 public overload 最终都调用上述公共入口；`TaskExecutionContextHolder.checkpointIfPresent()` 也是同一路径。它们均不需要修改。
4. `TaskMaintenanceService.runOpportunisticMaintenance(...)` 当前在 production `:581-585` 先执行已有 `checkpoint(context)`；该现有私有调用点在 `:983-986` 调用 `context.throwIfStopRequested()`，早于 `handleMaintenanceBroadcast(...)` 和后续 Dialog/Summon delegate。因此补强共享 context 后即可在首个 delegate 前生效，不需要碰 Service。
5. 当前缺口精确位于 `latestExactTurnMetadata()`：它只拒绝 missing metadata、device drift 和 logical window drift（`:416-428`），随后直接返回 metadata；`windowTitle`、`nativeHandle`、`processId` 尚未与 initial snapshot 比较。
6. `getNativeWindowTitle()`、`getNativeWindowHandle()`、`getNativeWindowProcessId()` 返回的是 initial immutable 值（`:142-162`），不是 latest fence；不能把这些 getter 当 checkpoint。

现有 typed 投影足够复用：`TaskCheckpointDecision.turnWindowMismatch()` 返回 `TaskCheckpointOutcome.WINDOW_MISMATCH`，并由现有 `TaskCheckpointTransitionException` 负责栈展开。无需新增 enum、exception 或协议字段。

## 4. A -> B -> A' 的必要实现形状

`TurnWindowMetadata` 当前只有 `deviceId/windowId/windowTitle/nativeHandle/processId/windowRect/pause/stop`，没有显式 `bindingGeneration`。因此只在每次调用中比较三项 native 值会出现下面的错误：

```text
initial A -> latest B（拒绝）-> latest A'（三项值重新等于 A，旧 context 又会通过）
```

要满足子卡的“值相等 rebind 也不能复活旧 generation”，context 必须记住曾观察到 native generation drift。最小且不扩协议的办法是一个 private、单调、无对外能力的失效闩：只允许 `false -> true`，永不恢复。它不是 owner/session/ledger/TTL/retry 或业务状态。

建议在现有方法原位完成，不加 helper 层：

```java
private boolean turnNativeWindowGenerationInvalidated;

private synchronized TurnWindowMetadata latestExactTurnMetadata() {
    // 保留现有 latest read、missing、device、window 检查及其顺序。
    boolean nativeGenerationMismatch =
            !initialTurnWindowMetadata.windowTitle().equals(metadata.windowTitle())
                    || !initialTurnWindowMetadata.nativeHandle().equals(metadata.nativeHandle())
                    || initialTurnWindowMetadata.processId() != metadata.processId();
    if (nativeGenerationMismatch) {
        turnNativeWindowGenerationInvalidated = true;
    }
    if (turnNativeWindowGenerationInvalidated) {
        throw new TaskCheckpointTransitionException(
                TaskCheckpointDecision.turnWindowMismatch(),
                "turn-native window generation mismatch");
    }
    return metadata;
}
```

精确边界：

- 比较顺序仍是 missing -> device -> logical window -> title/HWND/process；不改变已有分类。
- `windowRect`、`pauseRequested`、`stopRequested` 不属于本卡 generation 三元组，不能顺手纳入相等判断。
- 方法仍先读取 latest slot；失效后再次收到 A' 也会实际消费该 slot，再由单调闩拒绝，测试可以真实证明 A' 没有复活。
- `synchronized` 只串行一次短 metadata read/compare；pause 的 250ms sleep 位于方法外，不会持锁睡眠。这样并发 checkpoint 可线性化为“B 被观察后，后续调用均拒绝”，且无需新增 import 或并发 helper。
- 类级说明应同步注明这一枚 powerless monotonic safety latch，避免继续声称 turn-native context 只含完全 immutable 数据。

## 5. 现有 A-B-A 测试槽位与最小测试增量

测试基础已经齐全：

- `turnCheckpointCoversActiveStopPauseAndIdentityFailures()`（test `:338-398`）已覆盖 exact、stop、pause、pause-to-stop、missing、device drift、window drift。
- `ScriptedCommandPort.metadataScript`（`:715-750`）是 FIFO `Deque<Optional<TurnWindowMetadata>>`；一次 public checkpoint 对应一次 latest metadata read，并保留最后一项。新增 checkpoint 用例无需 wall-clock race、server，也不需要反射 checkpoint 私有方法或失效闩。
- `assertTransition(...)`（`:567-581`）已经从 public `context.throwIfStopRequested()` 进入，并断言 typed outcome、零 command 和恰好一次 metadata read。

唯一 named test 文件内的最小增量：

1. 在现有 checkpoint test 后增加 title-only、HWND-only、process-only 三个 fresh harness；每个 scripted latest slot 只改一项，期望 `WINDOW_MISMATCH`。
2. 增加独立 A-B-A case：initial A；依次 script 一个 value-equal A、native 三元组不同的 B、以及新对象 A'；先断言 A 通过，再断言 B 拒绝，再断言 A' 仍拒绝。对 A 与 A' 同时断言 `equals=true`、object identity 不同，避免只测同一对象回放。
3. 扩充现有 `assertTransition`：除 `executeCalls=0` 外，再断言 `uuids.calls=0`、`actions.isEmpty()`；`metadataReads=1` 同时证明单次 drift checkpoint 没有内部 observation retry。
4. A-B-A case 精确断言 `metadataReads=3`、`executeCalls=0`、`uuids.calls=0`、`actions.isEmpty()`、script deque 已消费完。这里没有独立 retry counter；“每次 public checkpoint 恰好一次 metadata read + 零 UUID/command/action”是当前公共 seam 上最强的零 retry 证据。
5. 所有新增调用只使用 public `TaskExecutionContext.turnNative(...)` 与 `throwIfStopRequested()`；不新增 reflection、source scan、sleep/race 或第三个测试类。

### 5.1 既有 UUID fixture 与 `no reflection` 的合同张力

当前测试并非整文件零 reflection：

- test `:623-632` 的既有 `client(...)` 用 reflection 调用 `TurnGameClient` 的 package-private 四参数 constructor，唯一目的就是注入 `CountingUuidSupplier`。
- production `TurnGameClient` 只有 public 三参数 constructor；它固定使用 `UUID::randomUUID`。本测试所在 package 无法无反射访问可注入 supplier 的四参数 constructor，`TurnGameClient` 又是 `final`。
- 因此，在冻结两文件写集内，可以做到的是：保留这段既有 fixture seam 不动，新增 drift/A-B-A 断言不增加任何 reflection，并且 checkpoint 全部通过 public `turnNative(...)`/`throwIfStopRequested()` 验证。
- 如果子卡的 `no reflection` 要求被解释为“整份 named test 必须移除所有 reflection”，则它与“直接计数并断言 UUID=0”及“只改这两个 Java 文件”无法同时满足。改用 public 三参数 constructor 只能证明零 command/action，不能直接观察 UUID supplier；要同时满足必须另开 `TurnGameClient` public/package test seam 或调整测试 package/write set。

本 helper 不替父级选择解释。最小实现可以继续的前提应明确为：`no reflection` 约束新增 checkpoint 验证不得反射 production 私有状态，既有 UUID 注入 seam 原样保留。若要求整文件零 reflection，应先纠正子卡合同，不能让 worker 临场扩写集。

## 6. 最小两文件修改清单

| File | 精确修改 | 明确不改 |
|---|---|---|
| `TaskExecutionContext.java` | 一枚 private monotonic invalidation bit；现有 `latestExactTurnMetadata()` 原位同步并增加 title/HWND/process 比较、sticky typed rejection；修正相关类注释 | public signatures、legacy branch、pause/stop cadence、device/window 顺序、DTO/enum、Service |
| `TaskExecutionContextTurnContractTest.java` | 三项独立 drift、A-B-A、零 UUID/command/action/额外 read 断言 | fixture 协议、production reflection seam、其它测试类、POM |

无需第三个 Java 文件，也无需修改 `TaskMaintenanceService`、`TaskCheckpoint*`、`TurnWindowMetadata`、`TurnGameClient`、protocol、POM、CR 卡或任一 DHXY Java 文件。

## 7. 编译与交付风险

1. **增量 Java 风险低：**建议形状只用现有 accessor、exception/factory 和 Java `synchronized`；不新增 import、泛型或 public API。三项 native 值的 initial 侧已在 construction 时验证 nonblank/positive。
2. **并发语义必须保留：**若只加普通逐次比较而不加 sticky bit，A-B-A 合同必然失败；若加 bit 但不做同步，两个线程可能在 B 失效边界上出现不可审计交错。
3. **可观测 generation 上限：**sticky bit 能封死“checkpoint 已实际观察到 B”之后的 A'。若设备事实从 A 直接跳到值完全相同的 A'，中间 B 从未进入 latest slot，当前协议没有 generation/sequence 字段，任何两文件实现都无法识别；这不是本卡测试所描述的 A-B-A 槽位。
4. **既有 reflection 合同风险：**严格整文件零 reflection 与直接 UUID 计数在冻结写集内冲突，见 5.1。不能靠删掉 UUID 断言、source scan 或 mock 掉 `TurnGameClient` 来伪装闭合。
5. **测试可见性风险高：**Cloud `.gitignore` 忽略整个 `src/test/`，普通 `git status` 不显示 named test 变化；production 目标也仍是 untracked。后续交接必须逐文件记录 line count/SHA，不能依赖 tracked diff 是否为空。
6. **仓库整体构建风险未知：**Cloud `pom.xml` 当前有未提交修改，且仓内存在大量并行 dirty/untracked production/test bytes。Surefire 的 `-Dtest=TaskExecutionContextTurnContractTest` 只筛执行类，不保证跳过其它 test-source 的 `testCompile`；无关 test compile debt 仍可能先阻断。
7. **本轮没有构建证据：**依用户限制未运行任何 Maven/JUnit/compile。后续稳定 writer 窗口的门仍应是点名 `TaskExecutionContextTurnContractTest` 和 Cloud compile；任何失败需区分本两文件增量与写集外首个真实错误。

## 8. Preflight 结论

当前冻结 SHA、public checkpoint 调用链、FIFO metadata fixture 和 typed `WINDOW_MISMATCH` 投影足以在严格两文件 Java 写集内实现“观察到 B 后永久拒绝 A'”的 TURN-34BP1 production fence。源码层未发现必须扩协议、改 Service、加 wrapper 或增加第三文件的阻断。唯一不可省略的实现要点是 monotonic generation invalidation；只做 title/HWND/process 当前值比较不满足 A-B-A。

测试侧存在一个必须由父级按上文 5.1 明确口径的既有 seam：若允许保留原有 UUID constructor reflection、禁止新增 checkpoint reflection，则两文件方案闭合；若要求整份 named test 零 reflection，则当前冻结合同需要先修订。本文不构成源码归属、review 结论或最终裁决。

**无已批准业务差异；按 `696a12b0`、exact-window generation 与最小 HTTPS JSON turn 等价迁移。**

TRUE_EOF PRECHECK_COMPLETE
