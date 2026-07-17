# Internal Count Worker I6 - ClientIdentityService::scanAndSyncIdentity

## CLAIMED

- task: `W-COUNT-CLIENT-IDENTITY-WHOLE-1`
- claimedAt: `2026-07-15T01:00:02-04:00`
- countUnit: `ClientIdentityService::scanAndSyncIdentity`
- countDelta: `+1`（仅申报；父级源码审查与统一 fresh build 通过后才实际记账）
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- 唯一 Java 写集: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\ClientIdentityService.java`
- 条件写集: identity-specific pure adapter only if truly needed；当前预检显示不需要。
- 冻结: `PlayerStateService`、generic `WindowFact`/shared remote、host/config、DHXY Java、其它 Service。

## Baseline Gate

- 已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、
  `docs/业务逻辑.md`、whole-Service 计划、迁移矩阵、两仓 `git status` 及父级 NPC Review #1。
- active Cloud `ClientIdentityService.java` 当前与 `696a12b0` 原类一致；本任务只在原 title-resolution 调用点
  替换本地 phantom，不改变 parser 或 player mutation policy。
- 两仓大量 dirty/untracked 均受保护；不回滚、覆盖、清理或提交任何并行改动。

## Real Caller

- active Cloud `PlayerStateService.syncMyIdentity()` 的真实链为：
  `context.getMe()` -> `identityService.scanAndSyncIdentity(me)` -> 原日志输出。
- caller 文件冻结，本 Worker不改调用条件、顺序或 player 对象来源。

## 696a12b0 Method / Branch Map

`scanAndSyncIdentity` 必须逐项保持：

1. `me == null`：只告警并返回，不读取窗口事实。
2. title 为 null/blank：只告警并返回，PlayerCharacter 不变。
3. `WindowTitleIdentityParser.parse(title)` 成功：严格依次写
   `gameServerName -> name -> id`，再记录三字段日志。
4. parse miss：只告警并返回，PlayerCharacter 不变。

只替换 private `resolveCurrentWindowTitle` 的本地 phantom：原
`WindowTaskContextHolder -> refreshAndCommit -> native binding -> tracker title -> locateWindow fallback`
在 Cloud 中改为当前 `TaskExecutionContext` 的既有 `RemoteGameClientPort`/`CloudGameClient`
`WindowFactKind.BINDING`。不增加 fallback、TTL、retry、owner 或业务状态。

## Existing Typed Closure

- Cloud `WindowFactKind.BINDING` 已存在。
- Cloud `WindowFact.BindingFact` 已 closed 携带
  `windowId/nativeHandle/processId/playerIdentityEpoch/title/className`；title 保持 optional blank 语义。
- DHXY `LocalRemoteGameCommandHandler.executeWindowFact` 的 `BINDING` 分支直接从本次 exact
  `BindingAccess.context()/binding()` 构造上述事实，title 来自当前 registration/native binding。
- 因而本任务无需新增 shared kind、codec、handler、adapter 或 host bean；只在 Service 原调用点消费现成 closed
  `WindowFactOutcome`。

## Implementation In Progress

- 待改文件仅 Cloud `ClientIdentityService.java`。
- Maven/test/runtime/Git mutation 按任务禁令不运行。

## Implementation #1 - 2026-07-15T01:04:00-04:00

### Java Change

唯一修改：
`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\ClientIdentityService.java`

- 删除 Cloud phantom 依赖：`GameClientTracker`、`WindowTaskContextHolder`、
  `WindowNativeBindingRefreshService` 及对应 runtime/native-binding 类型。
- 改为注入既有 `TaskExecutionContextHolder`，从当前 exact `TaskExecutionContext` 调用
  `getGameClient().readWindowFact("client-identity", "binding-title", WindowFactKind.BINDING, 120000)`。
- `OBSERVED` 严格读取 `WindowFact.BindingFact.title()`；没有自行拼装 title，也没有读取 global/tracker state。
- `NOT_EXECUTED/UNKNOWN/STOPPED`、无 current context 或 interrupted coordination 均折叠为 null title，随后走
  原 public 方法既有 blank/no-state 分支，不修改 PlayerCharacter、不新增 fallback/retry。
- `EXECUTED` 在 `WindowFactOutcome` 合同中本来不可构造；Service 仍显式拒绝该错误 terminal，避免把输入结果误当
  observation。
- `CloudGameClient.readWindowFact` 对非 UNKNOWN terminal 使用既有 final-consumption closure；本 Service 未新增
  owner、permit、session、ledger、TTL 或自动 retry。

### Real Caller Closure

1. `PlayerStateService.syncMyIdentity()` 取得现有 `context.getMe()`。
2. 原样调用 `identityService.scanAndSyncIdentity(me)`。
3. `ClientIdentityService` 在当前 task-run context 读取 `BINDING`。
4. DHXY 既有 handler 从 exact registration/native binding 产出 `BindingFact.title`。
5. Cloud 仅在 `OBSERVED` 解析 title；parse 成功后保持
   `setGameServerName -> setName -> setId` 原顺序，parse miss/blank/no-state 均不写字段。
6. public void terminal 原样返回 caller，caller 原日志顺序不变。

### Baseline Diff Audit

- `scanAndSyncIdentity` 的 null-state、blank-title、parse、三字段 mutation、parse-fail 分支逐行未改。
- `WindowTitleIdentityParser`、`WindowTitleIdentity`、`PlayerCharacter`、`PlayerStateService` 均未修改。
- 与 `696a12b0` 的差异只包括 import/constructor 依赖、private title resolver 和对应 JavaDoc。
- `rg` 确认 active Cloud `ClientIdentityService.java` 不再含
  `GameClientTracker/WindowTaskContextHolder/WindowNativeBindingRefreshService/refreshAndCommit/locateWindow`。
- 复用仓内既有 `BINDING` kind、fact、codec、handler；identity-specific adapter 未新增。

## Delivery

- status: `IMPLEMENTED / COUNT PENDING PARENT REVIEW + FRESH BUILD`
- countUnit: `ClientIdentityService::scanAndSyncIdentity`
- countDelta: `+1`（仅申报，尚未实际更新 ledger）
- Java changed: `1`（唯一允许文件）
- report changed: 本文件
- Maven/test/runtime/application/server/host: 按禁令未运行
- Git mutation: 未执行
- intentional business differences: `无已批准业务差异；按 696a12b0 等价迁移`

## Parent Source Review #1 / Repair Required - 2026-07-15T01:10:30-04:00

父级独立对照 `696a12b0`、active caller、`WindowFactOutcome` 和既有 Cloud fact consumer 后判定：
**BLOCKED，P0=0/P1=1/P2=0；countDelta 尚不可应用。**

- P1：`resolveCurrentWindowTitle` 把 typed `STOPPED` 与 `NOT_EXECUTED/UNKNOWN` 一起返回 null；
  `InterruptedException` 也仅恢复中断位后返回 null。随后 public 方法把它们当“标题为空”告警并正常返回，真实
  stop/interruption 因而可被身份同步吞掉，caller 会继续后续业务。对照仓内 `BattleRadarService`、
  `CommonBoxService`、`LeftTopStatusSwitchService` 的既有规则，`STOPPED` 必须先经
  `TaskCheckpoint.throwIfStopRequested(exactContext, ...)`，若 checkpoint 未抛则不得伪装成普通缺席终态。
- 可保留：exact current context、`BINDING` fact、OBSERVED title、原三字段写入顺序、blank/parse-fail 分支和
  单文件写集均成立；不要求新增协议、adapter、host bean、fallback、TTL 或 retry。

返修任务仍为 `W-COUNT-CLIENT-IDENTITY-WHOLE-1-R1`，countUnit/countDelta 不变。唯一写集仍为 Cloud
`ClientIdentityService.java` + 本报告：直接使用 `TaskCheckpoint` 处理 STOPPED 和线程中断；
`NOT_EXECUTED/UNKNOWN` 可继续映 null；错误 `EXECUTED` 继续拒绝。返修后提交 `Repair #1`，不运行
Maven/test/runtime/Git，等待父级复审与统一 fresh build。

## Repair #1 - 2026-07-15T01:18:00-04:00

已按 Parent Source Review #1 仅返修 Cloud `ClientIdentityService.java` 的 stop/interruption 终态：

- `InterruptedException`：先恢复线程 interrupt 位，再以本次读取使用的 exact
  `TaskExecutionContext` 调用 `TaskCheckpoint.throwIfStopRequested(...)`。若真实 stop/transition
  已发生，由既有 checkpoint 语义直接退出；若 context 仍为当前 ACTIVE，则抛
  `TaskCheckpointTransitionException(TaskCheckpointDecision.interruptedWhileCurrent(runRevision))`，
  不再返回 null title、也不会让 `PlayerStateService` 继续正常业务。
- `STOPPED`：从 `NOT_EXECUTED/UNKNOWN` 的 null 分支中独立出来，先调用同一 exact context 的
  `TaskCheckpoint.throwIfStopRequested(...)`；若 checkpoint 未确认 stop，则抛
  `TaskFatalException`，显式拒绝矛盾 terminal。
- `NOT_EXECUTED/UNKNOWN` 仍映射 null，保持原 blank/no-state 语义；`EXECUTED` 继续显式拒绝；
  `OBSERVED` title、parser、`gameServerName -> name -> id` 更新顺序均未改变。
- 写集核对：仅修改允许的 Cloud `ClientIdentityService.java` 与本报告；未修改 caller、shared
  remote、DHXY Java、其它 Service 或配置。
- 按任务禁令未运行 Maven/test/runtime/application/server/host，也未执行 Git mutation。

交付状态：`REPAIR #1 DELIVERED / COUNT PENDING PARENT REVIEW + UNIFIED FRESH BUILD`。
countUnit 仍为 `ClientIdentityService::scanAndSyncIdentity`，countDelta 仍只申报 `+1`，尚未实际更新 ledger。
无已批准业务差异；按 `696a12b0` 等价迁移。

## Parent Source Review #2 / Next Count Task - 2026-07-15T01:19:00-04:00

父级独立复核 Repair #1：`STOPPED` 已从普通缺席分支剥离并先走 exact-context `TaskCheckpoint`，矛盾 terminal
再 fatal；`InterruptedException` 恢复中断位后采用仓内 `TaskSleep` 同型 checkpoint/unwind，不能再落入 blank-title
正常返回。`OBSERVED/NOT_EXECUTED/UNKNOWN/EXECUTED`、parser 和三字段更新顺序未漂移。

结论：**P0=0/P1=0/P2=0，REPAIR SOURCE APPROVED / COUNT PENDING BUILD。**
`countUnit=ClientIdentityService::scanAndSyncIdentity` 仅在统一 fresh Cloud package 通过当轮 `+1`；ledger 暂不动。

下一任务另记固定报告 `docs/superpowers/plans/reports/2026-07-15-cloud-client-binding-title-count-unit-worker-i6.md`：
`W-COUNT-CLIENT-BINDING-TITLE-WHOLE-1`，`countUnit=ClientIdentityService::resolveCurrentWindowTitle`，
`countDelta=+1`。一次闭合真实 `scanAndSyncIdentity caller -> resolveCurrentWindowTitle -> current exact context ->
typed BINDING fact -> DHXY registration/native-binding title -> closed observed/absent/stop terminal`，保留 baseline 优先使用
当前绑定标题的业务效果；Cloud 不得恢复 global tracker/title search/locate fallback，也不得新增 retry/TTL/owner。
唯一 Java 写集 Cloud `ClientIdentityService.java` + 新报告；caller、shared、DHXY、其它 Service 冻结。现有真链完整可
NO_CODE_CHANGE 交证据，不得造第二 binding protocol。父级源码审查 + fresh build 通过同轮才 `+1`。
