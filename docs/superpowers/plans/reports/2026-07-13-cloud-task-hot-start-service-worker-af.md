# Cloud TaskHotStartService lift-and-shift - Internal Worker AF

Append-only coordination log. Parent thread is the only manager/reviewer; Worker AF only designs/implements an explicitly approved slice.

## Parent Task Brief #1 - `W-HOTSTART-D1` - 2026-07-13T19:16:00-04:00

Design an implementation-ready Cloud lift for committed `0114604e` `TaskHotStartService` without changing business precedence.
Before writing, read `D:\mavenProject\DHXY\AGENTS.md`, `docs\DHXY_CONTEXT.md`, `docs\业务逻辑.md`, the top of
`docs\ACTIVE_WORK.md`, the latest migration matrix, both repository statuses, committed/current
`TaskHotStartService`/`TaskHotStartSnapshot`/`TaskHotStartScreenState`, current Cloud Task/Service context, BattleRadar and Dialog
typed fact seams. Treat every existing dirty/untracked file as another owner's work; do not revert, overwrite, clean or commit it.

### Required design output

1. Prove the exact `0114604e` precedence and null/default behavior: combat check and synchronized action state first; only a
   non-combat result proceeds to one dialog inspect; exact task/source normalization and snapshot mapping must remain unchanged.
2. Keep local permanent authority for battle observation, dialog capture/OCR/template interpretation and all input. Cloud may own only
   the business reduction from closed typed facts to the hot-start snapshot/next phase; no screenshots/images/raw OCR text cross the wire.
3. Give the smallest first **code** slice of 1-3 new Cloud files that is compile-ready now and has zero overlap with Internal AB RX3
   shared remote/ledger/broker/codec/digest/schema files. Prefer a pure immutable reducer/value contract over another facade layer.
4. Close the future typed fact acquisition order, exact per-run context/revision fences, UNKNOWN/NOT_EXECUTED/STOPPED handling,
   pause/resume and no-double-read rules without inventing TTL/retry/fallback/extra verification.
5. List exact New/Modify FQCNs, constructors/method signatures, dependency DAG, state/null truth table, capacity/tenant ownership and
   dormant activation boundary. No public raw request/poll/outcome endpoint, no second queue/ledger/thread/registry, no host/caller/Task.

### Delivery and boundaries

Append `CLAIMED task=W-HOTSTART-D1 claimedAt=<ISO> uniqueWriteSet=<this log>` first, then Design #1 at true EOF. Design-only:
Java/Maven/schema/resources/tests/host/caller are frozen until parent `DESIGN APPROVED`. Do not run application/server/Task/UI/capture/
input, Maven clean, or Git mutation. Worker self-QA is not approval.

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

CLAIMED task=W-HOTSTART-D1 claimedAt=2026-07-13T19:21:56.2058628-04:00 uniqueWriteSet=D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-13-cloud-task-hot-start-service-worker-af.md

## Design #1 - W-HOTSTART-D1 - 2026-07-13T19:38:00-04:00

### 1. 设计结论与基线证据

本设计只建立 Cloud 侧纯业务事实/归约契约，不接 host、不改 caller、不新增 wire，不启动任何运行路径。首批批准后可立即
实施 3 个 `com.bot.dhxy.task.hotstart` 新文件；`Modify=无`。这 3 个文件与 Internal AB 当前独占的
remote/retained-action/ledger/broker/codec/digest/schema、DHXY input queue/worker 写集为零交集，也不依赖 AB 尚未稳定的
新 generic-exclusive 类型。

已对照：

- DHXY 当前分支 `thin-client-design`，HEAD `0114604e1ff5f15491d2910959c45252e893d04f`；Cloud 当前分支
  `navigation-migration`，HEAD `3b988caa010254973e03342272e6d1d6a9685b01`。两仓均有大量并行 dirty/untracked，全部保护。
- `git show 0114604e:src/main/java/com/bot/dhxy/task/hotstart/TaskHotStartService.java` 与当前本地实现一致：先调用
  `battleRadarService.checkAndSyncCombatState()`，且仅当返回 `true` 与同步后的
  `GameContext.ActionState.IN_COMBAT` 同时成立时返回 `IN_COMBAT/NONE`；该分支绝不 inspect dialog。
- 明确非战斗后只调用一次
  `dialogService.handleDialog(DialogHandleRequest.inspect("hot-start:" + safeTaskCode + ":" + safeSource))`，随后只按
  `OPTION -> OPTION_DIALOG`、`STORY -> STORY_DIALOG`、`NONE -> NONE` 归约。
- `taskCode/source` 精确规则是 `null || isBlank()` 才替换为字面量 `"unknown"`；否则原字符串逐字符保留，不 trim、
  不 lower-case。dialog 返回对象或 `dialogType` 为 null 时，基线会异常；不得静默改成 `NONE/UNKNOWN`。
- `docs/业务逻辑.md` “通用任务类热启动 Policy”已核：固定顺序为
  `战斗中 > 当前任务 dialog > 队伍回归 > tracker > 回程道具 > 保存上下文 > 接任务入口`。本切片仅迁移前两层的
  screen snapshot，不扩展到后续 phase，也不改变五倍/修罗各自 dialog interest。
- 迁移矩阵 `TaskHotStartService` 行及隐式状态清单已核：battle radar 的截图/模板检测和
  `GameContext.currentActionState` 同步、dialog capture/OCR/template/input 永久留在本地；Cloud 只消费关闭后的 typed
  fact，不接收图片、ROI、OCR 文本、模板路径或点击候选。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

### 2. 方案选择

采用“两阶段纯归约”而不是一次性把 combat 与 dialog 一起传给 Cloud：

1. `begin(...)` 只消费一次关闭后的 combat observation。只有结论为明确非战斗时，返回
   `DIALOG_REQUIRED` 与唯一 inspect description；战斗、未执行、未知、停止都不会产生 dialog 请求资格。
2. `completeDialog(...)` 只接受上一步产生的 `DIALOG_REQUIRED` token 和一次关闭后的 dialog observation，输出最终
   `TaskHotStartSnapshot` 或 typed unresolved 结果。
3. 归约对象不持有图片、原始 OCR、模板、坐标、request/action/session id，不负责远端调度、等待、重试、TTL、final consume
   或 revision fence。

比较过但不采用：

- 单方法 `(combatFact, dialogFact) -> snapshot`：实现更短，但会允许 caller 在 combat 未决或已命中时预先读取 dialog，无法
  从类型路径上守住 combat-first/single-dialog-inspect。
- 本波直接增加 `TaskHotStartService` + remote fact wire：会碰 AB 的 `CloudTaskServicePort/RemoteOperation/ledger/codec/
  digest/schema`，且当前共享源码仍有 AB P1 返修在途，不满足零重叠和 dormant 门。

### 3. 父级批准后的首批精确代码切片

#### AF-1 - closed observation value

- **New FQCN:** `com.bot.dhxy.task.hotstart.TaskHotStartObservation`
- **文件:** Cloud
  `src/main/java/com/bot/dhxy/task/hotstart/TaskHotStartObservation.java`
- `final`、package-private、不可变；不是 Spring bean，不实现 remote interface。
- 精确嵌套枚举：

```java
enum Kind { COMBAT, DIALOG }
enum State { OBSERVED, NOT_EXECUTED, UNKNOWN, STOPPED }
```

- 精确字段：

```java
private final Kind kind;
private final State state;
private final Boolean radarCombat;
private final Boolean synchronizedActionStateInCombat;
private final DialogType dialogType;
```

- 唯一构造器与工厂签名：

```java
private TaskHotStartObservation(
        Kind kind,
        State state,
        Boolean radarCombat,
        Boolean synchronizedActionStateInCombat,
        DialogType dialogType);

static TaskHotStartObservation combatObserved(
        boolean radarCombat,
        boolean synchronizedActionStateInCombat);
static TaskHotStartObservation dialogObserved(DialogType dialogType);
static TaskHotStartObservation notExecuted(Kind kind);
static TaskHotStartObservation unknown(Kind kind);
static TaskHotStartObservation stopped(Kind kind);

Kind kind();
State state();
Boolean radarCombat();
Boolean synchronizedActionStateInCombat();
DialogType dialogType();
```

工厂关闭非法组合：`OBSERVED+COMBAT` 才能携带两个 combat Boolean；`OBSERVED+DIALOG` 才能携带
`DialogType`；三个非 observed state 的 payload 必须全 null。`dialogObserved(null)` 仍以 `NullPointerException` 失败，保持
基线“不把 null 当 NONE”的语义。两个 combat primitive 精确表达基线 `checkAndSyncCombatState()` 返回值和同步后 action state
是否等于 `IN_COMBAT`，Cloud 不重放或替代本地同步副作用。

#### AF-2 - typed reduction value

- **New FQCN:** `com.bot.dhxy.task.hotstart.TaskHotStartReduction`
- **文件:** Cloud
  `src/main/java/com/bot/dhxy/task/hotstart/TaskHotStartReduction.java`
- `final`、package-private、不可变；不是 endpoint/outcome envelope，不进入 shared codec。
- 精确状态：

```java
enum Status { COMPLETE, DIALOG_REQUIRED, NOT_EXECUTED, UNKNOWN, STOPPED }
```

- 精确字段、私有构造器与可见方法：

```java
private final Status status;
private final String taskCode;
private final String source;
private final TaskHotStartSnapshot snapshot;
private final String dialogInspectionSource;

private TaskHotStartReduction(
        Status status,
        String taskCode,
        String source,
        TaskHotStartSnapshot snapshot,
        String dialogInspectionSource);

Status status();
String taskCode();
String source();
TaskHotStartSnapshot snapshot();
String dialogInspectionSource();
```

仅 `TaskHotStartReducer` 可通过 package-private static factories 构造：`COMPLETE` 必须只有非 null snapshot；
`DIALOG_REQUIRED` 必须只有规范化后的 task/source 与精确 `hot-start:<task>:<source>`；三个 unresolved 状态 snapshot 和
inspection source 必须为 null。它不增加 retry/renew/expiry 语义。

#### AF-3 - pure two-stage reducer

- **New FQCN:** `com.bot.dhxy.task.hotstart.TaskHotStartReducer`
- **文件:** Cloud
  `src/main/java/com/bot/dhxy/task/hotstart/TaskHotStartReducer.java`
- `public final` 无状态类，私有构造器；不加 `@Service/@Component`，不注入任何 collaborator。
- 精确签名：

```java
private TaskHotStartReducer();

public static TaskHotStartReduction begin(
        String taskCode,
        String source,
        TaskHotStartObservation combatObservation);

public static TaskHotStartReduction completeDialog(
        TaskHotStartReduction pending,
        TaskHotStartObservation dialogObservation);
```

`begin` 在方法内部且仅一次执行精确 `isBlank -> "unknown"` 规范化。`completeDialog` 要求 `pending.status()` 必须为
`DIALOG_REQUIRED`、observation kind 必须为 `DIALOG`；非法阶段/类型立即失败，不产生 snapshot，也不触发任何 fallback。
因为两个参数类型是 package-private，外部 package 不能把 reducer 当公共 raw API 使用；未来同包 Cloud hot-start adapter
是唯一自然 caller。

### 4. 依赖 DAG 与零重叠证明

```text
DialogType (Cloud 已有 exact copy)
        |
        v
TaskHotStartObservation (AF-1)
        |
        v
TaskHotStartReducer (AF-3) ---> TaskHotStartReduction (AF-2)
        |                              |
        +------------------------------+
                       |
                       v
        TaskHotStartSnapshot / TaskHotStartScreenState
                 (Cloud 已有 exact copy)
```

- `New=上述 3 个 Cloud 文件`；`Modify=无`。
- 不 import `com.yueyunfe.dhxy.cloudbrain.remote.*`，不读写 AB 的 retained state/ledger/broker/codec/digest/schema。
- 不修改 DHXY、本地 `TaskHotStartService`、`BattleRadarService`、`DialogService`、任何 Task/host/caller/resource/test。
- 不新增 queue/thread/executor/scheduler/map/registry/cache/endpoint/controller。

### 5. 精确 state/null 真值表

| 阶段 | typed fact | payload | 归约结果 | dialog 行为 |
|---|---|---|---|---|
| combat | `OBSERVED` | `radar=true`, `actionInCombat=true` | `COMPLETE`；snapshot=`IN_COMBAT/NONE` | `NOT_EXECUTED`；不得 inspect |
| combat | `OBSERVED` | 其余 3 种 boolean 组合 | `DIALOG_REQUIRED` | 仅允许随后一次 inspect |
| combat | `NOT_EXECUTED` | 全 null | `NOT_EXECUTED`；无 snapshot | 不 inspect，不 retry |
| combat | `UNKNOWN` | 全 null | `UNKNOWN`；无 snapshot | 不 inspect，不 retry/fallback |
| combat | `STOPPED` | 全 null | `STOPPED`；无 snapshot | 不 inspect |
| dialog | `OBSERVED` | `OPTION` | `COMPLETE`；`OPTION_DIALOG/OPTION` | 已消费唯一 fact，不再读 |
| dialog | `OBSERVED` | `STORY` | `COMPLETE`；`STORY_DIALOG/STORY` | 已消费唯一 fact，不再读 |
| dialog | `OBSERVED` | `NONE` | `COMPLETE`；`NONE/NONE` | 已消费唯一 fact，不再读 |
| dialog | `NOT_EXECUTED` | 全 null | `NOT_EXECUTED`；无 snapshot | 不补读、不 retry |
| dialog | `UNKNOWN` | 全 null | `UNKNOWN`；无 snapshot | 不把 unknown 当 NONE，不补读 |
| dialog | `STOPPED` | 全 null | `STOPPED`；无 snapshot | 不补读 |

Null/非法组合：

- `combatObservation == null`、`pending == null`、`dialogObservation == null`：立即 `NullPointerException`，无 snapshot。
- `dialogObserved(null)`：立即 `NullPointerException`；与基线 switch null 同为失败，绝不归约为 `NONE`。
- `completeDialog` 收到非 `DIALOG_REQUIRED` pending、非 `DIALOG` kind 或非法 payload：
  `IllegalStateException/IllegalArgumentException`，无 wire、无 fallback。
- combat 命中时 reducer 根本不需要 dialog observation；不会读取、检查或验证 dialog 参数，保持基线短路。

### 6. 未来 acquisition/ledger 接线约束（本波不实施）

后续独立 activation 设计必须在 AB shared seam 稳定后另行批准；AF 首批 3 文件不预占这些写集。接线时：

1. 一个 retained hot-start parent occurrence 下只有两个有序 child slot：`combat-observation`，以及仅在
   `DIALOG_REQUIRED` 后声明的 `dialog-observation`。不得一次提交两条，也不得创建第二 ledger/queue。
2. 本地 combat child 仍在绑定窗口调用一次原 `checkAndSyncCombatState()` 并立即读取同步后的 action state，关闭为两个
   boolean；原截图/template、runtime state 和 `GameContext` 写回永久留本地。
3. 本地 dialog child 只执行一次原 `DialogHandleRequest.inspect(exactDescription)`；关闭结果只携带 `DialogType`，不上传
   screenshot/ROI/OCR/template/path/click point。不得改为 cached visible dialog、第二次 verify 或“先 classification 再 inspect”。
4. remote `ExecutionState.OBSERVED/NOT_EXECUTED/UNKNOWN/STOPPED` 由未来同包 adapter 一对一映射为本设计
   observation state；`EXECUTED` 对 observation 非法。必须先经 existing exact final-consumption fence，再调用 reducer。
5. wrong tenant/window/taskRunId/stopEpoch/runRevision、PAUSED、stale、late final 均由 existing
   `CloudTaskRunCurrentContextSlot/CloudTaskServiceExecutionContext/ledger` 处理；reducer不能持有或重建这些身份。
6. 任何 `UNKNOWN/NOT_EXECUTED/STOPPED` 都不产生 snapshot，也不授权下一 fact。AF 不新增 TTL、超时、重试、fallback、
   verification count、expiry 或 fail-closed business rule；既有 lifecycle 若允许同 occurrence attempt renewal，是否 renewal
   仍完全由其已有上层权威决定，不由 hot-start reducer决定。

### 7. pause/resume 与 no-double-read

- 若 pause 发生在 combat fact final-consume 前：旧 revision 不可归约；existing lifecycle 处理 typed unwind。AF 不重发。
- 若 combat 明确非战斗已 final-consume 且 `DIALOG_REQUIRED` 已保存在既有 retained task/phase state 后 pause：resume 在新
  revision 只重校验同一 parent occurrence 并继续尚未声明/尚未完成的 dialog child，不重新读取 combat。
- 若 dialog 已 final-consume 后 pause：resume 只可重做纯 `completeDialog` 归约或读取已保存 snapshot，不重新 inspect。
- 任一 child 为 `UNKNOWN` 时，同一 occurrence 不因 resume、poll 或 reducer 再调用而产生第二次机械读取；是否由更上层显式
  新业务 occurrence 重新开始，不属于本 CR，也不能由 AF 自动决定。
- reducer 的纯函数可重复计算同一已关闭 fact，但“重复计算”不等于第二次 capture/OCR；机械 child action 仍由 existing
  ledger 的 stable occurrence/final-consumed 规则唯一化。

### 8. capacity / tenant ownership

- AF-1/2/3 全部无状态：retained entry=`0`、retained bytes（除当前调用栈对象）=`0`、线程=`0`、队列=`0`、map/registry=`0`、
  定时器=`0`，因此无独立 per-tenant/global capacity 或 eviction。
- tenant/window/taskRun/stopEpoch/runRevision 所有权继续属于 existing exact `CloudTaskServiceExecutionContext` 与 retained
  state；未来 parent/child action 只计入既有 action ledger/broker capacity，不新增旁路配额。
- reducer 输入不含 tenant 可伪造字段；future adapter 只能从 non-mintable current execution context 取得 scoped capability。

### 9. dormant activation boundary

- 首批 3 文件无 Spring 注解、无 static initializer 副作用、无 host/assembly/controller/endpoint/caller 引用；即使编译进入
  Cloud jar，也没有运行入口。
- 本地现有 `TaskHotStartService` 与所有任务 caller 继续原样执行，首批代码对当前 runtime 行为为零影响。
- 真正激活至少还需后续独立批准：closed combat/dialog fact protocol 或既有 typed fact seam 扩展、两仓 strict codec/digest
  parity、本地 mechanics handler、Cloud retained parent state/service adapter、host/caller cutover。该波必须重新做 AB 写集
  协调和双仓构建，不能从本 Design #1 推定获批。

### 10. 实施门与 runtime 验收点

父级只有在本日志追加明确 `DESIGN APPROVED` 后，AF 才可新增 AF-1/2/3。批准后的本切片只运行 Cloud Java compile/package
门，不运行应用/Task/UI/capture/input，不创建本地测试；当前 Design #1 阶段不运行 Maven。首批代码验收只检查：精确 source
parity、三文件写集、编译、无 Spring/remote import/queue/thread/registry。fresh runtime 必须等未来完整 activation 波，重点看
`logs/dhxy-console.log` 中每个 hot-start occurrence 为 combat 一次、明确非战斗时 dialog 一次，且 combat 命中时零 dialog；
不能以本纯切片编译代替该运行验收。

### 11. Self-QA（不是批准）

- 基线优先级：combat-first 已由两阶段 API 和 `DIALOG_REQUIRED` 唯一路径关闭。
- single-dialog-inspect：首批不读 dialog；未来只能由一个 retained dialog child 提供一次关闭事实。
- 本地永久权威：battle screenshot/state sync 与 dialog capture/OCR/template/input 均未迁走。
- AB 冲突：New 3、Modify 0；remote/ledger/broker/codec/digest/schema 零写入。
- 新业务语义：未增加 retry/TTL/fallback/verification/expiry/cleanup/park；null 不归一为 NONE。
- P0/P1/P2 self-QA=`0/0/0`；**只表示 AF 设计自检，不构成 Parent DESIGN APPROVED。**

状态：`WAITING_PARENT_DESIGN_REVIEW`。Java/Maven/schema/resources/tests/host/caller 继续冻结。
