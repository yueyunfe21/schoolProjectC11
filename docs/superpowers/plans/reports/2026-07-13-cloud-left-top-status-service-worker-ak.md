# Internal Worker AK - Direct LeftTopStatusSwitchService Migration

## Parent Direct Implementation Task / `W-LTSS-DIRECT-IMP1` - 2026-07-13T20:20:00-04:00

直接实现，不写 Design。先完整读取 `D:\mavenProject\DHXY\AGENTS.md`、`docs/DHXY_CONTEXT.md`、
`docs/superpowers/plans/2026-07-13-direct-service-input-bundle-migration.md`、本报告，以及 committed
`0114604e` 的 `LeftTopStatusSwitchService.java`。你不是仓库中唯一 Worker；保护两仓全部
dirty/untracked，不回滚、覆盖、清理、重命名或提交他人改动。

目标：在 Cloud 新建同包同名 `com.bot.dhxy.service.LeftTopStatusSwitchService`，保留 baseline 的
public 方法、判断顺序、pending 语义、日志条件、点击 delay 和返回值；仅把本地 `detect(...)` 替换为
AJ 已批准的 `CloudGameClient.readWindowFact(..., LEFT_TOP_STATUS, timeoutMs)`，把原 move+click 替换为
D 的一个有序 `InputBundle`。不得新增 owner/permit/ledger/TTL/retry/线程/轮询/host/caller。

唯一 Java 写集：

- Cloud Modify `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`：只新增
  `getGameClient()`，原样返回 `delegate.gameClient()`；现有 `getRemoteGameClient()` 零变化。
- Cloud New `src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java`。
- 本报告 append-only。

实现约束：

- 以 `0114604e` 为业务逐行权威；保留 `handleLeaderStartup`、`probeMemberStartup`、
  `consumeFollowerSafeWindow`、`handleCombatMaintenance`、`isSupportedTaskCode`、`SwitchState`、
  `SwitchActionResult` 的签名和业务结果。
- Cloud 类按 per-run 使用，不建立 Spring singleton；允许构造器接收正 `timeoutMs`，不得在 Service 内发明
  timeout/TTL 默认值。每个 public 调用仍使用它收到的 exact `TaskExecutionContext`。
- `OPEN` 事实的屏幕绝对点按 baseline `move + sleep(100) + clickLeft(delay=150)`（以源码实际值为准）
  一次发送；非 OPEN、UNKNOWN、CAPTURE_FAILED 或非 EXECUTED 不点击，不自动重投。
- `readWindowFact` 的 stable phase/action slot 必须由原 source/task 调用语义确定，不能用随机 ID；
  `InterruptedException` 按 baseline detect 的异常语义处理，同时恢复线程 interrupt。
- 不修改 AJ 的 fact/handler、本地 DHXY、B mapper、D InputSequences、AI facade、assembly、host 或 tests；
  不新增/恢复测试。

完成后向本日志真实 EOF 追加 `Internal Worker AK - W-LTSS-DIRECT-IMP1 Implementation #1`，列出精确
文件、baseline 对照和编译状态。若 B/D 接缝仍未修复，只记录 `WAITING_DEPENDENCY`，不得扩大写集。
Java 稳定且依赖已齐时运行 Cloud `mvn -q compile`（不 clean）；父级统一跑 clean package。
自审只算 QA。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## CLAIMED

- 2026-07-13：Internal Worker AK 已认领 `W-LTSS-DIRECT-IMP1`；严格限定为报告所列两个 Cloud Java 文件与本报告 append-only，不提交、不触碰其他 Worker 写集。

## Internal Worker AK - W-LTSS-DIRECT-IMP1 Implementation #1

- 完成时间：`2026-07-13T20:29:45-04:00`。
- 精确 Java 写集：
  - Cloud `src/main/java/com/bot/dhxy/runner/context/TaskExecutionContext.java`：仅新增 `CloudGameClient` import 与 `getGameClient()`；方法原样返回 `delegate.gameClient()`，现有 `getRemoteGameClient()` 未改。
  - Cloud 新建 `src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java`：无 Spring 注解/单例，只接受调用方提供的正 `timeoutMs`。
- `0114604e` baseline 对照：
  - 原样保留 `handleLeaderStartup`、`probeMemberStartup`、`consumeFollowerSafeWindow`、`handleCombatMaintenance`、`isSupportedTaskCode`、`SwitchState`、`SwitchActionResult` 的 public 签名、支持任务判断顺序、source 字符串、pending mark/clear/consume 分支顺序、probe/click 日志触发条件及返回状态。
  - 本地 `detect(...)` 唯一替换为 exact context 的 `getGameClient().readWindowFact(...)`：stable phase 为 `left-top-status-switch:<safe(taskCode)>`，stable action slot 为 `<safe(source)>`，fact kind 为 `LEFT_TOP_STATUS`；无随机 ID、无重试/重投。
  - `OBSERVED` fact 原样映射 `OPEN/CLOSED/UNKNOWN/CAPTURE_FAILED`、scores 与屏幕绝对 click point；非 `OBSERVED` 映射不可点击 `UNKNOWN`。`InterruptedException` 映射 `CAPTURE_FAILED` 并恢复线程 interrupt。协议不传本地扫描路径/矩形，因此 Cloud 结果的 `rawPath` 为 `null`、probe 日志 `rect=-`，不参与任何 baseline 业务分支。
  - 仅 `OPEN + allowClick + point` 提交 D 的一个 `InputSequences.moveAndClickLeft(...)` bundle；动作顺序保持 `MOVE_MOUSE -> SLEEP(120ms) -> CLICK_LEFT(delay=250ms)`（以 `0114604e` 源码实际值为准）。D 对非 `EXECUTED` outcome 返回 `false`，本 Service 不重投。
- 依赖状态：External B mapper / AJ `LEFT_TOP_STATUS` fact-handler / D `InputSequences` 接缝均已存在，无 `WAITING_DEPENDENCY`。
- 编译证据：在 `D:\mavenProject\dhxy-cloud-brain` 执行 `mvn -q compile`，`2026-07-13` 本次 Java 修改后退出码 `0`（耗时 `16.2s`，无编译输出）。按 no-local-test 规则未创建、恢复或运行测试。
- 自审：仅检查本 Worker 写集；未修改 DHXY Java、B/D/AJ、AI facade、assembly、host、tests，未提交、未回滚或清理任何 dirty/untracked。
- 无已批准业务差异；按 `0114604e` 基线等价迁移。

## Parent Source Review #1 - BLOCKED

- 时间：`2026-07-13T20:34:00-04:00`。
- 结论：`P0=0 / P1=1 / P2=0`，暂不批准。
- `P1` nullable execution context 兼容回归：`0114604e` 的
  `DefaultWindowTaskStartupInitializer` 明确注明 `executionContext` 在 task 尚未 fully attached 时可为
  `null`，且仍把该值传给 `probeMemberStartup(...)` / `handleLeaderStartup(...)`；baseline
  `LeftTopStatusSwitchService` 的日志和检测路径可容忍该值。新 Cloud 实现在
  `LeftTopStatusSwitchService.java:169` 无条件调用 `context.getGameClient()`，并在
  `probeMemberStartup(...)` / `consumeFollowerSafeWindow(...)` 的 pending 分支直接访问 context，导致
  supported task 的 legacy/early-startup 路径从可返回结果退化为 `NullPointerException`。
- 影响：未来原样迁移 startup initializer 时可在任务尚未附着阶段打断整个启动流程；这不是截图失败或
  `UNKNOWN`，而是新的调用方可见异常。
- 精确返修条件：保持 exact-context authority，不得为 null context 创建或猜测 remote scope；在现有流程内
  对 null context 显式返回不可点击的 `CAPTURE_FAILED`（或等价 baseline-safe 非异常结果），pending 读写必须
  对 null 安全，恢复 public JavaDoc 的 nullable 合同。不得新增 helper/wrapper/owner/retry，不改非 null 路径、
  phase/action slot、delay 或其他文件。返修后运行 Cloud `mvn -q compile`，向真实 EOF 追加 Repair #1。
- 已核对：全部已提交业务调用点正常传非 null context；`SwitchActionResult.rawPath()` 无外部消费者，Cloud
  置 `null` 属于本地路径不出 wire 的预期 ownership 变化，不另列 blocker。

## Internal Worker AK - W-LTSS-DIRECT-IMP1 Repair #1

- 完成时间：`2026-07-13T20:35:35-04:00`。
- 精确返修文件：Cloud `src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java`；`TaskExecutionContext.java` 与其他文件零变化。
- P1 修复：`detect(...)` 在 `context == null` 时直接返回 `CAPTURE_FAILED`、scores `-1.0`、无 click point/remote call；因此 supported legacy/early-startup 调用保留 probe 日志并返回 `clicked=false`，不 mint、猜测或访问任何 remote authority。
- Pending null-safe：`probeMemberStartup(...)`、`consumeFollowerSafeWindow(...)`、`clearPendingIfResolved(...)` 仅在 context 非 null 时读取或写入 pending；非 null 分支的判断及 mark/clear/consume source 字符串未变。
- JavaDoc：四个 public TaskExecutionContext 参数均恢复/明确 nullable legacy/early-startup 合同。
- 非 null 路径复核：`readWindowFact` phase `left-top-status-switch:<safe(taskCode)>`、action slot `<safe(source)>`、`LEFT_TOP_STATUS`、fact 映射、日志条件及 `MOVE_MOUSE -> SLEEP(120ms) -> CLICK_LEFT(delay=250ms)` 均未修改；未新增 helper/wrapper/owner/retry。
- 编译证据：在 `D:\mavenProject\dhxy-cloud-brain` 执行 `mvn -q compile`，退出码 `0`（耗时 `15.9s`，无编译输出）。按 no-local-test 规则未创建、恢复或运行测试。
- 未提交、未回滚、未清理 dirty/untracked；等待 Parent Source Review #2。

## Parent Source Review #2 - APPROVED

- 时间：`2026-07-13T20:37:00-04:00`。
- 结论：`P0=0 / P1=0 / P2=0`，`W-LTSS-DIRECT-IMP1` 源码批准。
- 父级逐行复核 Repair #1：null context 在任何 remote/pending 访问前稳定映射为不可点击
  `CAPTURE_FAILED`；`probeMemberStartup`、`consumeFollowerSafeWindow` 与 resolved pending 写入均已 null-safe，
  public nullable 合同恢复。该路径不 mint/猜测 scope，不触发输入，也不抛新 NPE。
- 非 null 路径仍使用 `left-top-status-switch:<taskCode>` + 原 source action slot、同一
  `LEFT_TOP_STATUS` fact、同一 pending 分支和 `MOVE_MOUSE -> SLEEP(120) -> CLICK_LEFT(250)` bundle；
  没有 owner/permit/ledger/TTL/retry/线程/host/caller 扩张。
- Worker 的 Cloud `mvn -q compile` 已 exit `0`。父级 fresh `mvn -q clean package` 留待 AL 共享 Java
  写入稳定后统一执行；这不回退本次源码批准。
- 无已批准业务差异；按 `0114604e` 基线等价迁移。
