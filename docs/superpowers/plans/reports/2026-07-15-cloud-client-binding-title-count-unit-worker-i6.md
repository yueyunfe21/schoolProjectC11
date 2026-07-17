# W-COUNT-CLIENT-BINDING-TITLE-WHOLE-1

## CLAIMED - 2026-07-15T01:25:00-04:00

- worker: `Internal Count Worker I6`
- role: implementation-only；不是 reviewer
- countUnit: `ClientIdentityService::resolveCurrentWindowTitle`
- countDelta: `+1`（仅申报；等待父级源码审查与统一 fresh build 后实际计数）
- business baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- allowed Java write set: Cloud `ClientIdentityService.java`
- report write set: 本文件
- frozen: caller、shared remote、DHXY Java、其它 Service、host/config
- restrictions: 不运行 Maven/test/runtime/application/server/host/Task/poller/UI/capture/input；不执行 Git mutation

## Baseline Contract

`696a12b0` 的 `resolveCurrentWindowTitle` 首要业务规则是：在多窗口任务上下文中优先使用当前绑定窗口的
native title。旧实现之后的 `GameClientTracker.getFullWindowTitle()/locateWindow()` 是同进程本地兜底；迁云后的真实
caller 已有 exact `TaskExecutionContext`，不得恢复 global tracker/title search，否则可能读取其它窗口。

本 countUnit 保持的可观察语义：

1. `scanAndSyncIdentity` 是唯一直接 caller；`me == null` 时它在调用 resolver 前返回。
2. exact current binding 提供 title 时，resolver 原样返回该 title，交既有 parser 处理。
3. context/fact/title 缺席时返回 null/blank，caller 保持原告警并且不修改 PlayerCharacter。
4. stop/interruption 必须退出当前业务栈，不得伪装成普通空 title。
5. 不新增 global tracker、窗口搜索、fallback、retry、TTL、owner 或第二套 binding 协议。

## Real Caller To Local Fact Closure

1. active Cloud `PlayerStateService.syncMyIdentity()` 取得 `context.getMe()`，原样调用
   `ClientIdentityService.scanAndSyncIdentity(me)`。
2. `scanAndSyncIdentity` 在非空 player 分支调用 private `resolveCurrentWindowTitle()`。
3. resolver 从注入的 `TaskExecutionContextHolder.current()` 取得当前 exact `TaskExecutionContext`；没有 current
   context 时返回 null，不 mint/default/搜索其它窗口。
4. resolver 通过该 exact context 的 `getGameClient().readWindowFact(...)` 发送唯一既有
   `WindowFactKind.BINDING`，稳定地址为 `client-identity/binding-title`。
5. DHXY `LocalRemoteGameCommandHandler.executeWindowFact` 的 `BINDING` 分支使用本命令已经校验的
   `BindingAccess.context()/binding()`，直接构造 `RemoteBindingFact`；title 精确来自
   `access.binding().getTitle()`，同时携带 exact windowId/nativeHandle/processId/playerIdentityEpoch/className。
6. Cloud wire 将既有 payload 还原为 `WindowFact.BindingFact`；resolver 仅在 `OBSERVED` 返回
   `BindingFact.title()`。
7. `CloudGameClient.readWindowFact` 对除 `UNKNOWN` 外的 terminal 使用既有 final-consumption closure；本
   countUnit 不新增任何 retained/ledger/session 机制。
8. 返回的 title 继续进入既有 `WindowTitleIdentityParser`；本 countUnit 不修改 parser、player mutation 或 caller
   日志顺序。

## Terminal Map

- `OBSERVED`：严格读取 `WindowFact.BindingFact.title()`；null/blank 保持 caller 的 absent/no-state 分支。
- `NOT_EXECUTED`：返回 null，闭合绑定事实不可用/超时等普通缺席。
- `UNKNOWN`：返回 null，不伪造 title，也不自动 retry。
- `STOPPED`：先用同一 exact context 调用 `TaskCheckpoint.throwIfStopRequested(...)`；若 terminal 与 current
  context 矛盾，则 `TaskFatalException`，不会继续 caller。
- `InterruptedException`：恢复 interrupt 位后走 exact-context `TaskCheckpoint`；若仍为 current ACTIVE，则以
  `INTERRUPTED_WHILE_CURRENT` typed transition 退出，不会映射为空 title。
- `EXECUTED`：显式拒绝；输入执行 terminal 不可冒充窗口观察事实。

## No-Code-Change Audit

父级 Source Review #2 后的 active Cloud `ClientIdentityService.java` 已完整满足本 countUnit，因此本任务选择
`NO_CODE_CHANGE`：

- 未修改 Cloud `ClientIdentityService.java`，避免对已批准的 identity chain 做重复编辑。
- 未修改 `PlayerStateService`、generic `WindowFact`、codec/transport、DHXY handler、其它 Service 或配置。
- `rg`/源码核对确认 resolver 不含 `GameClientTracker`、`WindowTaskContextHolder`、
  `WindowNativeBindingRefreshService`、`refreshAndCommit`、`getFullWindowTitle` 或 `locateWindow`。
- 只复用现有 `WindowFactKind.BINDING` 与 DHXY exact registration/native binding producer；没有新增第二 binding
  DTO、port、adapter、handler 或 fallback。

## Delivery

- status: `NO_CODE_CHANGE DELIVERED / COUNT PENDING PARENT REVIEW + UNIFIED FRESH BUILD`
- countUnit: `ClientIdentityService::resolveCurrentWindowTitle`
- countDelta: `+1`（仅申报，尚未更新 ledger）
- Java changed: `0`
- report changed: 本文件
- Maven/test/runtime/application/server/host: 按禁令未运行
- Git mutation: 未执行
- intentional business differences: `无已批准业务差异；按 696a12b0 当前绑定优先语义等价迁移`

## Parent Source Review #1 / Next Count Task - 2026-07-15T01:27:00-04:00

父级独立复核 active resolver、BINDING producer 与 terminal map：唯一 caller、exact current context、稳定 fact 地址、
DHXY registration/native-binding title、OBSERVED/absent/stop/interrupt/EXECUTED 分支均闭合；没有恢复 global tracker、
title search、locate fallback、retry 或第二 binding 协议。结论：
**P0=0/P1=0/P2=0，NO_CODE_CHANGE SOURCE APPROVED / COUNT PENDING BUILD。**
`countUnit=ClientIdentityService::resolveCurrentWindowTitle` 仅在统一 fresh build 通过当轮 `+1`。

下一任务另记固定报告 `docs/superpowers/plans/reports/2026-07-15-cloud-player-position-count-unit-worker-i6.md`：
`W-COUNT-PLAYER-POSITION-WHOLE-1`，`countUnit=PlayerStateService::syncMyPosition`，`countDelta=+1`。
一次闭合真实 `NavigationService:903/syncAll caller -> Cloud PlayerStateService position decision -> existing typed
current-location fact/port -> DHXY exact-window location observation -> closed LocationInfo/state mutation`，保留 696 的
current map/x/y 更新顺序、unavailable/fallback、日志和 stop 语义。唯一 Java 写集 Cloud `PlayerStateService.java` +
PlayerState position-specific adapter（仅必要时）+ 新报告；Navigation caller、DHXY、shared、incense/first-aid/其它
Service 冻结。现有真链完整可 NO_CODE_CHANGE；若 typed producer 缺失则精确 BLOCKED，不造 stub。父级源码审查 +
fresh build 同轮才 `+1`。
