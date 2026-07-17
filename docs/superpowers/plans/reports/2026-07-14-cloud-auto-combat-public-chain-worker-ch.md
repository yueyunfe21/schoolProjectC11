# Cloud AutoCombat Public Chain - Internal Worker CH

CLAIMED | task=W-AUTOCOMBAT-FULL-PUBLIC-CHAIN-IMP1 | claimedAt=2026-07-14T09:49:43.3755843-04:00 | writeSet=[D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\AutoCombatService.java, D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-14-cloud-auto-combat-public-chain-worker-ch.md]

## Implementation status

- status: `PARTIAL / BLOCKED_BY_MISSING_COLLABORATOR_API`
- implementation worker: `Internal CH` (not reviewer)
- business baseline: DHXY committed `0114604e1ff5f15491d2910959c45252e893d04f` `AutoCombatService`
- business contract: `无已批准业务差异；按基线等价迁移`
- Java write: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\AutoCombatService.java`
- result: implemented every requested public definition whose committed semantics can be expressed through the currently landed Cloud collaborator APIs. The remaining entry points are intentionally absent instead of returning invented terminal values or omitting committed phases.

## Public definitions landed

Target public chain definitions implemented in this pass:

1. `authorizeCombatDetectionAfterEnterBattleAction(String)`
2. `revokeCombatDetectionAuthority(String)`
3. `getDynamicPollingIntervalMs()`
4. `nextCombatMaintenanceDelayMs()`
5. `nextCombatWakeDelayMs()`
6. `hasPendingFollowerFirstAidForCurrentWindow()`
7. `hasPendingLeaderPostCombatRecoveryForCurrentWindow()`
8. `consumeQueuedLeaderPostCombatFirstAidIfHead(TaskExecutionContext, String)`
9. `reportQueuedLeaderPostCombatFirstAidIfPending(TaskExecutionContext, String)`
10. `consumePendingLeaderPostCombatRecoveryIfAllowed(TaskExecutionContext, String)`

Supporting public definitions retained/closed in the same class:

- explicit constructor over the exact Cloud collaborator graph
- `RefreshDuePanelVerifyDecision`
- `RefreshDuePanelVerifyGate.reserveIfAllowed(String, String, long)`
- `TickResult`
- `PostCombatRecoveryPolicy`

Requested definitions not emitted because one or more mandatory committed collaborator calls are not available:

1. `initializeForCurrentWindow()`
2. `handleCombatTick(TaskExecutionContext, String, boolean)`
3. `handleCombatTick(TaskExecutionContext, String, PostCombatRecoveryPolicy)`
4. `handleWindowCombatGuardTick(TaskExecutionContext, String)`
5. `probeWindowCombatStateReadOnly(TaskExecutionContext, String)`
6. `probePausedWindowCombatStateReadOnly(TaskExecutionContext, String)`
7. `refreshFastExpectedExitBaselineAfterTrustedInCombat(String)`
8. `reconcileReturnHomeVerifiedCombatState(TaskExecutionContext, String, String, String)`

## Real call graph

- detection authorization/revocation -> per-window `AutoCombatRuntimeState` -> committed logs
- dynamic polling -> `BattleRadarService.getDynamicPollingIntervalMs()`
- maintenance delay -> `CloudAutoBattleProperties.getAutoBattleRefreshIntervalMs()` -> `GameContext` round facts -> `AutoCombatPanelService.resolveRoundsRefreshReason(...)`
- wake delay -> maintenance delay -> `BattleRadarService.nextFastExpectedCombatExitProbeDelayMs()` when the committed runtime flag is armed
- queued leader first aid consume -> `TaskMaintenanceService.isPostCombatFirstAidHeadWindow(...)` -> `GameContext` combat guard -> `PlayerStateService.hasPendingNoFocusFirstAidPlanForCurrentWindow()` / `performCachedFirstAidPlanNow(...)` -> `TaskMaintenanceService.completePostCombatFirstAidAttempt(...)`
- queued leader first aid report -> `PlayerStateService.probeAndConsumeHealthyFirstAidNoFocus(...)` -> committed result mapping -> `TaskMaintenanceService.reportPostCombatFirstAid(...)`
- deferred leader recovery -> `GameContext` combat guard -> queue-mode decision -> `PlayerStateService.probeAndConsumeHealthyFirstAidNoFocus(...)` / `performCachedFirstAidPlanNow(...)` -> `TaskExecutionContext.throwIfStopRequested()` -> `PlayerStateService.ensureSheYaoXiangActiveForLeaderTask(...)`
- runtime ownership -> `TaskExecutionContext.windowId/playerIdentityEpoch` -> `ConcurrentHashMap<String, AutoCombatRuntimeState>`; identity drift recreates only the committed per-window AutoCombat state

`AutoCombatPanelService`, `PlayerStateService`, `TaskMaintenanceService`, and `BattleRadarService` are called through their landed Cloud APIs. The committed AutoCombat class does not directly call `TeamReturnService` or `SummonSkillService`; those collaborators remain owned by `TaskMaintenanceService`. No direct calls were invented. The currently blocked combat-maintenance entry path therefore cannot yet reach `LeftTopStatusSwitchService`, `CommonBoxService`, or the `TaskMaintenanceService`-owned return/summon collaborators without skipping mandatory phases.

## Exact external gap

The single blocking integration gap is: **the current Cloud collaborator graph does not yet expose the complete committed AutoCombat observation/team-phase/UI-clean closure**.

Exact missing surface required by committed `0114604e`:

- `BattleRadarService.checkAndSyncCombatState(...)`
- `BattleRadarService.checkFastExpectedCombatExitByAvatarDiff(...)`
- `BattleRadarService.refreshFastExpectedCombatExitAvatarBaseline(...)`
- `TaskMaintenanceService.invalidateTeamCombatPhaseForLeader(...)`
- `TaskMaintenanceService.confirmTeamCombatPhaseExitedForLeader(...)`
- `TaskMaintenanceService.memberTeamCombatPhase(...)`
- `TaskMaintenanceService.isLocalSupportMemberSession(...)`
- `TaskMaintenanceService.isLocalTeamSupportCapabilityOpen(...)`
- `TaskMaintenanceService.isPendingLocalSupportLeaderDetection(...)`
- `TaskMaintenanceService.isLocalSupportMemberCandidate(...)`
- a closed host-local UI-clean operation equivalent to committed `UICleanerService.cleanUpAll()` / `closeAllGenericWindows()`; the current Cloud `LocalMacroKind` has no such macro
- a paused read-only observation handoff that can supply the committed radar verdict without running normal task checkpoints or local capture in this class

Without that surface, adding the eight absent public definitions would necessarily skip committed calls/phases, manufacture an observation result, or return an invented `TickResult`; all three are forbidden by the assignment and by the no-approved-business-difference baseline.

## Baseline differences

- Intentional Cloud ownership substitution: committed `WindowTaskContextHolder` lookup is projected from the constructor-bound `TaskExecutionContext` (`windowId`, `playerIdentityEpoch`). No per-service owner/session/ledger/TTL was added.
- Intentional Cloud configuration substitution: committed local `BotProperties` read is served by `CloudAutoBattleProperties`.
- Intentional construction substitution: collaborators are constructor-injected; no Spring/Task/host wiring was added in this worker scope.
- The landed method bodies preserve committed branch order, delay constants, keep/return values, first-aid fallback order, checkpoints, and log semantics.
- Incomplete migration difference: the eight public entry points listed above remain absent solely because their mandatory external closure is not landed. No terminal/no-op behavior was substituted.
- No capture, template matching, OCR, HWND, local input, retry, TTL, or automatic host/caller wiring was introduced.
- `无已批准业务差异；按基线等价迁移` for every implemented method.

## Verification

- source SHA-256: `350DB7A26ABD769D29F1497644C9B4295B80DB0D48E048C40A8991946F304C78`
- source size: `517` lines / `26556` bytes
- compile command: `mvn -q compile`
- compile project: `D:\mavenProject\dhxy-cloud-brain`
- compile exit: `0`
- compile output: empty (successful quiet compile)
- final compile verified at: `2026-07-14T10:17:22.1397549-04:00`
- final `PlayerStateService.java` collaborator snapshot: lastWrite=`2026-07-14T10:15:10.6640841-04:00`, bytes=`78774`; the five called public signatures were re-read immediately before the final compile
- tests: not created and not run, per assignment
- runtime/server: not started, per assignment
- git: no branch switch, stage, commit, clean, reset, or other mutation performed

## Parent Source Review #1 - 2026-07-14T10:14:00-04:00

**PARTIAL SOURCE APPROVED / BLOCKED，P0=0/P1=1/P2=0。** 父级以 committed
`0114604e` 的 `AutoCombatService` 为业务权威，逐项核对当前 Cloud 源码。已落的授权/撤销、polling/wake
deadline、pending 查询、leader first-aid FIFO report/consume 与 deferred recovery 共 10 个 public API 可以
保留；这些方法的条件顺序、时间常量、first-aid fallback、stop checkpoint 与日志语义未发现新的业务差异，
Cloud 配置/显式 `TaskExecutionContext` 只是既定 ownership seam。

唯一 **P1** 是本任务要求的“完整非 host public orchestration chain”尚未形成。当前 Cloud
`AutoCombatService.java:164-358` 只有上述支撑 API；`initializeForCurrentWindow`、两个
`handleCombatTick`、`handleWindowCombatGuardTick`、两个 read-only probe、
`refreshFastExpectedExitBaselineAfterTrustedInCombat`、`reconcileReturnHomeVerifiedCombatState` 共 8 个
committed public 入口仍不存在，Cloud 全树也没有 `AutoCombatService` caller。影响是当前类不能驱动 combat
enter/exit、in-combat maintenance、paused read-only observation、UI clean 或 verified-return correction；已落
helper 即使可编译也不构成 `public caller -> same-path Service -> typed local primitive/terminal result` 真链，
因此不得增加 `189/407`。

精确返修条件：保留本轮 10 个已落 API，不回滚、不造 stub/固定终态；原 Internal CH 继续负责余下 8 个入口。
先消费 External B 正在落的 `TaskMaintenanceService` team-phase/session API；battle observation 必须消费 DHXY
continuous watcher 的 typed verdict，不得把 capture/template/HWND 搬入 Cloud；`UICleanerService` 继续永久
留本地，只能经一个 closed local operation 调用。External D 当前拥有 shared remote/local-macro 写集，CH 不得
并发修改该写集；依赖稳定后再由父级开放精确 adapter 写集。全部 8 个入口、真实 caller 与 typed local terminal
链闭合后重交 Implementation Repair，并运行 Cloud `mvn -q compile`（不 clean）。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Internal CH - Implementation Repair Progress #1 - 2026-07-14T10:23:31-04:00

Status: `REPAIR IN PROGRESS / 12 PUBLIC API LANDED / 6 ENTRY POINTS BLOCKED`.
This section supersedes the earlier 10-public/8-missing implementation snapshot; Parent Source Review #1 is
retained verbatim as the repair authority.

### Newly consumed B APIs

External B's `W-TMS-REMAINING-COORDINATION-PUBLIC-CHAIN-IMP2` source is now stable at SHA-256
`F95F12B6EA508E493402AF1E915C6F5E2A7C8FD5B3CE449CD850802BB47A9F39`. Internal CH re-read and used the
landed definitions directly:

- `invalidateTeamCombatPhaseForLeader(String, String)`
- `confirmTeamCombatPhaseExitedForLeader(TaskExecutionContext, String)`
- `memberTeamCombatPhase(TaskExecutionContext)`
- `isLocalSupportMemberSession(TaskExecutionContext)`
- `isLocalTeamSupportCapabilityOpen(TaskExecutionContext, TeamSupportCapability)`
- `isPendingLocalSupportLeaderDetection(TaskExecutionContext)`
- `isLocalSupportMemberCandidate(TaskExecutionContext)`

No B-owned file was modified.

### Newly landed public entry points

1. `initializeForCurrentWindow()` now performs the committed timestamp/pending/member-coverage reset and then
   calls B's real `invalidateTeamCombatPhaseForLeader(currentWindowId(), "auto-combat-initialize")`.
2. `reconcileReturnHomeVerifiedCombatState(...)` now preserves the committed order: capture `before` -> B's real
   leader phase exit confirmation -> revoke detection authority -> clear expected/fast/entry flags -> keep false
   when already not in combat -> set `FREE` -> discard stale enter signal -> committed log -> return true.

Total landed requested public definitions are now 12. The six still absent are the two `handleCombatTick`
overloads, `handleWindowCombatGuardTick`, both read-only probes, and
`refreshFastExpectedExitBaselineAfterTrustedInCombat`.

### Private closure landed in this repair

- `mayRunBattleRadar`
- `memberLeaderCombatPhase`
- `maybeHandleCombatEnter`
- `consumeExitAndRecover`
- `runPendingMemberCommonBoxIfAllowed`
- `runPendingFollowerFirstAidIfAllowed`

These bodies preserve committed `0114604e` conditions, branch/phase order, return values, task-turn enter/release,
first-aid queue terminal handling, common-box-before-first-aid ordering, fast-exit deferred recovery, delays, and
logs. They call the real Cloud `BattleRadarService`, `AutoCombatPanelService`, `TaskMaintenanceService`,
`PlayerStateService`, `CommonBoxService`, and `CloudTaskTurnCoordination` APIs. No capture/template/OCR/HWND/input
mechanic was added to this class.

### Exact committed graph still awaiting adapters

- main tick: checkpoint -> arm expected-exit wait -> member leader phase -> leader typed verdict **or**
  watcher-owned self verdict -> enter handling -> stale-exit discard -> exit/recovery -> member box -> follower
  first aid -> in-combat maintenance -> `TickResult`.
- guard tick: checkpoint -> authorization gate -> watcher-owned verdict refresh -> enter handling -> read-only
  `IN_COMBAT/NONE` return without exit consumption.
- active read-only probe: checkpoint -> watcher-owned verdict refresh -> `IN_COMBAT/NONE`; no signal consumption or
  input.
- paused read-only probe: stop-only checkpoint -> authorization gate -> watcher-owned verdict refresh -> paused
  exit marker -> committed diagnostic -> `IN_COMBAT/NONE`; no input.
- fast-exit baseline refresh: arm per-window fast-watch flag -> consume a real watcher/closed-local-operation
  baseline-refresh terminal result; no fixed boolean.
- in-combat maintenance private closure: checkpoint -> refresh-pressure decision -> delayed entry UI clean -> panel
  verify -> sparse UI clean -> B session/capability gate -> left-top maintenance -> refresh gate/retry -> panel
  refresh. The two role-specific UI-clean calls must use the pending closed local operation.

Battle observation will consume only the DHXY continuous watcher's typed verdict. `UICleanerService` remains local
and must be reached only through the pending closed local operation owned by External D's shared remote/local-macro
write set. Internal CH did not modify or duplicate that write set and did not create a stub/fixed terminal result.

### Intermediate compile

- source SHA-256: `3F156F5EC74785041CADF2A73A1550210E138DC639D3DFB407BE4E77EF8440FE`
- source size: `820` lines / `45015` bytes
- command: `mvn -q compile`
- project: `D:\mavenProject\dhxy-cloud-brain`
- exit: `0` (quiet output)
- purpose: signature/integration check after consuming stable B and CG APIs; final compile remains required after the
  watcher/UI adapter and all six remaining entries land
- tests/runtime: none created, run, or started
- business difference: `无已批准业务差异；按基线等价迁移`

## Parent Source Review #2 - `W-AUTOCOMBAT-FULL-PUBLIC-CHAIN-IMP1` - 2026-07-14T10:36:30-04:00

**PARTIAL SOURCE APPROVED / BLOCKED，P0=0/P1=1/P2=0。** 父级复核 committed `0114604e` 与当前源码：
`initializeForCurrentWindow` 的时间戳/pending/member coverage 清理和 leader phase invalidation 顺序可保留；
`reconcileReturnHomeVerifiedCombatState` 的 phase exit -> authority revoke -> watch/pending 清理 -> 已非战斗 false ->
FREE -> discard stale enter -> log/true 顺序可保留。新增六个 private closure 的 branch/return/first-aid FIFO/
common-box-before-first-aid/turn release 结构也可保留，未发现新 TTL/retry/fallback。

当前唯一 **P1** 仍是完整入口未闭合：源码尚缺 `handleCombatTick` 两 overload、
`handleWindowCombatGuardTick`、`probeWindowCombatStateReadOnly`、`probePausedWindowCombatStateReadOnly`、
`refreshFastExpectedExitBaselineAfterTrustedInCombat` 六个 definitions（五个 unique method names），也没有真实 caller。
Internal CI 正直接实施 `BattleRadarService` 三个 typed observation 入口；External D 正返修 shared local macro，后续
还需 closed UI-clean operation。CH 不得在依赖稳定前造固定 verdict、跳过 UI phase 或修改 CI/D 写集。

请 CH 在本日志追加 `SUSPENDED_WAITING_CI_D_TYPED_ADAPTERS` 并停止 Java 写入；CI/D 各自父级通过后恢复同一
CH，按报告已列 exact graph 一次补齐六个入口。当前 12 个 landed public definitions 可保留，但 AutoCombat 完整链
不计入 `189/407`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Internal CH Suspension - 2026-07-14T10:37:08.0510484-04:00

`SUSPENDED_WAITING_CI_D_TYPED_ADAPTERS`

- 保留 Parent Source Review #2 已通过的 12 个 public definitions 与六个 private closure。
- 本次暂停记录新增 Java：`0`；自 Review #2 起未再修改任何 Java。
- 等待 Internal CI 的 `BattleRadarService` typed observation public chain 经父级通过。
- 等待 External D 的 shared local-macro 返修及后续 closed UI-clean operation 经父级通过。
- 恢复前不补六个入口、不造固定 verdict、不跳过 UI phase、不修改 CI/D 写集。
- `无已批准业务差异；按 0114604e 基线等价迁移`。

## Parent Strategy Reset #1 - `W-696-AUTOCOMBAT-WHOLE-1` - 2026-07-14T11:09:00-04:00

恢复 CH，但原 12-method retained block 仅作复用材料。唯一任务：用
`git show 696a12b0:src/main/java/com/bot/dhxy/service/AutoCombatService.java` 对 Cloud 同路径做整类等价迁移；
全部 initialization/tick/BattleRadar/first-aid FIFO/deferred recovery/deadline/cleanup 与 private closure 必须存在。
本地 battle fact/UI-clean 只在原调用点换 typed operation，不新增 host/thread/retry。唯一写集仍为 Cloud
`AutoCombatService.java` 与本报告。交付全方法 disposition、local substitution 表、SHA 与非 clean compile。

## Internal CH Resume Claim - 2026-07-14T11:15:03.0697172-04:00

`CLAIMED | task=W-696-AUTOCOMBAT-WHOLE-1 | claimedAt=2026-07-14T11:15:03.0697172-04:00 | writeSet=[D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\AutoCombatService.java, D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-14-cloud-auto-combat-public-chain-worker-ch.md]`

- implementation worker: `Internal CH` (not reviewer)
- whole-class business baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- DHXY snapshot: `thin-client-design@0114604e1ff5f15491d2910959c45252e893d04f`; existing dirty/untracked files are protected and outside this write set
- Cloud snapshot: `navigation-migration@3b988caa010254973e03342272e6d1d6a9685b01`; target Java is already untracked from the prior CH pass, and every other dirty/untracked file is protected
- constraints: no Git mutation, no tests/runtime, no shared remote/schema/other Service/DHXY source edits
- business contract checked: `docs/业务逻辑.md` fast-exit/verified-return rules and Xiuluo `696a12b0` baseline table; `无已批准业务差异；按基线等价迁移`

## Internal CH Sequence Halt - 2026-07-14T11:21:21.4849049-04:00

`HALTED_BY_WHOLE_COPY_SEQUENCE`

- 用户批准的新顺序：先将 `696a12b0` 的全部 32 个 `service/**` 完整原样放入 Cloud，再删除永久本地 Service，再按编译错误补 Cloud -> 本地边界，最后统一抽离其余动作。
- 当前 active Cloud Service Java 已立即冻结；本次恢复领取后新增 Java 写入为 `0`，未回滚、删除或覆盖已有代码。
- 当前文件：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\AutoCombatService.java`
- 最后写入时间：`2026-07-14T10:22:01.3384531-04:00`
- 当前 SHA-256：`3F156F5EC74785041CADF2A73A1550210E138DC639D3DFB407BE4E77EF8440FE`
- 当前尺寸：`820` lines / `45015` bytes
- 本轮未运行 compile/clean/tests，未启动 runtime，未执行 Git mutation；等待父级新任务。
