CLAIMED

- task: `W-COUNT-XIULUO-TASK-EXECUTE-WHOLE-1`
- claimedAt: `2026-07-15T05:06:12-04:00`
- countUnit: `XiuluoTaskV2::execute(TaskExecutionContext)`
- requestedCountDelta: `+1`
- 唯一 Java 写集: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\xiuluo\XiuluoTaskV2.java`
- 报告写集: `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-15-cloud-xiuluo-task-execute-whole-count-unit-worker-i39.md`

## Implementation Result

- role: Internal implementation Worker I39; implementation only, not reviewer.
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.
- disposition: `BLOCKED_MISSING_TYPED_BOUNDARIES / countDelta=0`.
- Java result: 新建 active Cloud `XiuluoTaskV2.java`，完整保存 baseline 的 4205 行整类源码；未做业务改写。
- exactness: baseline Git blob 与 active Cloud Git blob 均为
  `a010a0f5b267b02e0b202c2addf4a8bcc2c9600f`，public/private command shell、phase loop、判断、
  delay、fallback、state、comments 与 `finally forceReleaseTurn` 均未丢失。
- blocker: active Cloud 缺少 16 个该整类直接依赖的类型，且现有 Cloud
  `TaskExecutionContext` 不提供 baseline 使用的 `getWindowRuntimeContext()`。这些缺口不能在唯一文件内用
  stub、wrapper 或假数据合法补齐，因此本轮不能闭合可达的 typed DHXY mechanics/terminal result。

## Baseline And Business Gate

1. 已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`；已读取 `docs/ACTIVE_WORK.md` 顶部 CR271、
   `docs/业务逻辑.md` 修罗基线、whole-Service 计划、迁移矩阵及两仓 `git status`。
2. `docs/业务逻辑.md` 明确修罗以 `696a12b0` 为行为权威；本轮没有新增 phase、retry、TTL、
   park/yield、fallback、验证次数或输入顺序。
3. 目标文件由 `git show 696a12b0:src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
   完整迁入，不引用当前 DHXY dirty 文件作为行为来源。

`无已批准业务差异；按基线等价迁移`。

## Exact Blocker Evidence

### 1. 写集外类型缺失

active Cloud 缺少以下 16 个 baseline import：

- local observation/input: `GameClientTracker`、`TextRecognizer`、`CoordinateHelper`、`GameStateUtil`、
  `ObjectiveTextRecognitionService`、`WindowScopedTempPath`；
- 永久本地 Service: `BagService`、`QuestManagerService`、`UICleanerService`；
- task/turn/runtime: `TaskTransactionRunner`、`TaskTurnCoordinator`、`MultiWindowTaskManager`、
  `WindowReadyEventBus`、`WindowRuntimeContext`、`WindowTaskContextHolder`；
- diagnostics: `AutomationMetricsService`。

这不是仅删 import 即可解决：目标类字段在 `XiuluoTaskV2.java:252-280` 直接持有这些协作者。

### 2. execute 主链仍要求本地 runtime 对象

- `XiuluoTaskV2.java:331-342` 在任务入口更新 `WindowRuntimeContext` 的 run progress，并在每轮开始清理
  tracker-shortcut pathing intent；
- `:399-400` 在每轮结束再次更新 progress；
- `:2403` 在回城确认后继续传递真实 `WindowRuntimeContext`；
- active Cloud `TaskExecutionContext` 只暴露 immutable task/window identity、checkpoint 和 typed
  service port，不存在 baseline 所需的 `getWindowRuntimeContext()`。

删除这些调用会改变 runner-visible state 与 pathing 清理顺序；伪造 runtime 会引入第二状态权威，均不在批准范围。

### 3. 永久本地动作尚未以本任务可消费的 closed boundary 接通

- `XiuluoTaskV2.java:699-2923` 有 21 个 `UICleanerService` 调用；
- `:2832`、`:2881` 直接调用 `BagService.findAndUseMainBagTaskPageItem`；
- `:3373` 直接调用 `QuestManagerService.captureCurrentQuestDetailForTask`；
- `:1655`、`:2213`、`:3339` 仍直接调用 `InputSequences`；
- `:3173-3180` 直接刷新/读取本地 tracker 并 capture；`:3417`、`:3519` 直接执行本地 OCR。

Cloud 已有通用 `CloudUiCleanerPort` 和 BAG return-item macro 等协议块，但本任务没有获批的 action
identity/slot/terminal 适配，也没有可在本文件内等价替代 QuestManager、tracker capture/OCR、window-ready
event 与 turn transaction 的完整 typed contract。把这些调用改成常量成功、空 Optional 或日志会破坏基线。

### 4. terminal/finally 不能脱离 turn owner 伪闭合

- baseline `execute` 的 phase transaction 依赖 `TaskTransactionRunner` 与 `TaskTurnCoordinator`；
- `finally` 调用 `taskTransactionRunner.forceReleaseTurn("xiuluo-v2:execute-finished")`；
- active Cloud 仅有 transaction enum/value types，没有两个执行协作者。

因此源码虽完整保存，当前 active Cloud 仍不能证明
`execute -> rounds/brain command shell -> typed DHXY mechanics -> TaskRunResult -> finally release`
的真实可达闭环。

## Impact And Precise Repair Condition

- impact: 若现在记 `+1`，只证明整类证据镜像存在，不能证明它由 Spring 构造、execute 可达、每个本地动作
  走 typed DHXY mechanics，或 finally 能释放真实 task turn。
- repair condition: 由父级为上述缺口建立互不重叠的、已批准 shared boundary，至少包括：
  1. Cloud task-run progress/pathing/event/turn transaction 的单一现有权威适配；
  2. 永久本地 Bag/UI clean/QuestManager 的 closed typed macro/result；
  3. tracker capture/OCR/window-scoped temp/input 的 typed DHXY mechanics；
  4. 与 `TaskExecutionContext` 一致的 checkpoint、taskRun/runRevision 和 terminal mapping。
- 上述边界稳定后，重新领取同一 countUnit，在本文件内只替换原调用点并逐段证明 baseline 顺序不变；
  父级源码审查与 fresh Maven 门通过后才可应用 `countDelta=+1`。
- 禁止通过复制本地 runtime 类到 Cloud、增加 stub、空实现、第二 owner/session、TTL/retry 或删减 phase
  来解锁。

## Scope And Verification

- target before: absent.
- target after: untracked new file, 4205 lines, 243717 bytes, blob
  `a010a0f5b267b02e0b202c2addf4a8bcc2c9600f`，与 baseline byte-exact。
- dirty protection: 未回滚、覆盖、清理或提交任何既有 dirty/untracked；DHXY Java 未修改。
- per instruction: 未运行 build、test、runtime、Task/poller、application/server、UI/capture/input 或 Git mutation。
- count applied by I39: `0`。

`DELIVERED | task=W-COUNT-XIULUO-TASK-EXECUTE-WHOLE-1 | worker=I39 | countUnit=XiuluoTaskV2::execute(TaskExecutionContext) | requestedCountDelta=+1 | disposition=BLOCKED_MISSING_TYPED_BOUNDARIES | countDelta=0 | Java=BASELINE_EXACT_WHOLE_CLASS_PRESERVED | blob=a010a0f5b267b02e0b202c2addf4a8bcc2c9600f | businessDifference=NONE | parentReview=PENDING`

## Parent Source Review #1 - 2026-07-15T05:10:00-04:00

父级独立 `git hash-object` 确认 active/baseline blob 均为
`a010a0f5b267b02e0b202c2addf4a8bcc2c9600f`，完整 Task 源码保全 **APPROVED**；但 task-run
progress/pathing/event/turn、永久本地 Bag/UI-clean/QuestManager、tracker capture/OCR/input 与合法
TaskExecutionContext projection 均未闭合。结论 **P0=0/P1=4/P2=0，
BLOCKED_MISSING_TYPED_BOUNDARIES / countDelta=0**。保留整类，不得复制本地 runtime 或伪造 terminal；
shared boundaries 稳定后在同一 `XiuluoTaskV2::execute` 单逐原调用点适配。
