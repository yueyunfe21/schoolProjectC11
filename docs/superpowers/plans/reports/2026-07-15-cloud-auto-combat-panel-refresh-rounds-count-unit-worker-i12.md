# Internal I12 - AutoCombatPanelService::refreshAutoCombatRoundsIfNeeded

## CLAIMED

- task: `W-COUNT-AUTO-COMBAT-PANEL-REFRESH-ROUNDS-1`
- claimedAt: `2026-07-15T03:04:44-04:00`
- role: Internal I12 implementation-only Worker; not a reviewer
- countUnit: `AutoCombatPanelService::refreshAutoCombatRoundsIfNeeded`
- countDelta: `+1` (claim only; parent source review and fresh build decide acceptance/accounting)
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- only Java write set: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\AutoCombatPanelService.java`
- only report: this file
- concurrent exclusions preserved: External A/B/C/D own BattleRadar/Navigation/TaskTracker/AutoCombat; no file in those sets was edited.

## 当前进度

- `CLAIMED` 已落盘，唯一 Java 写集与唯一报告写集已锁定。
- active caller、`696a12b0` 方法体、typed panel observation 与 typed input terminal 已完成逐跳核对。
- 唯一 Java 修复已落盘：刷新输入从 Cloud-local `InputSequences` 改为当前 task-run 的 closed typed
  `CloudGameClient.executeInputBundle`，原 `Alt+8 -> 1000ms` 顺序及成功后 rounds reset 不变。
- 当前为 `IMPLEMENTED / STATIC CHAIN QA COMPLETE / PARENT GATES PENDING`；本 Worker 不记账、不自称 `Approved`。
- `countDelta=+1` 仍严格等待父级源码 review 与 fresh Cloud build 同轮裁决。

## Baseline And Workspace Gate

- Read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, the top CR271 material in `docs/ACTIVE_WORK.md`, the complete
  `2026-07-14-696a12b0-whole-service-first-migration.md` plan, the applicable AutoCombat/AutoCombatPanel rows and
  method inventory in `2026-07-12-service-migration-matrix.md`, and the applicable baseline gate in
  `docs/业务逻辑.md` before editing.
- Read-only workspace evidence showed both repositories already contain extensive dirty/untracked work. DHXY is on
  `thin-client-design`; Cloud is on `navigation-migration@3b988ca`. All pre-existing work was preserved.
- Baseline evidence was checked from both `git show 696a12b0:src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java`
  and `migration-baseline/696a12b0/.../AutoCombatPanelService.java`. Baseline Git blob is
  `bf63d2c78873afd8a0781d97f080a59b2b327942`; mirror SHA-256 is
  `72B5846D250E5389336ABD8CB416C0E0A5877F1EB39D4E970F032C1B838E4D67`.
- Applicable business rule: migration may move mechanics ownership only; it must not add TTL, verification,
  park/yield, retry, cleanup, fail-closed business truth, or change phase/fallback/input order.
- 无已批准业务差异；按基线等价迁移。

## Active Caller

The active production maintenance chain remains:

`AutoCombatService::handleCombatTick`
-> `maybeRunCombatMaintenance(context, source)`
-> `AutoCombatPanelService::resolveRoundsRefreshReason(...)`
-> the existing refresh-due/team gate or urgent-round per-window guard
-> `verifyAndAlignPanel(VERIFY_AND_REFRESH)`
-> typed `AUTO_COMBAT_PANEL` initial observation from the current task-run context
-> existing alignment/remaining-round read
-> `refreshAutoCombatRoundsIfNeeded(panelMatch, source)`
-> one typed input bundle
-> success-only round estimate/timestamp reset
-> boolean closed result back to `maybeRunCombatMaintenance`.

Active references were found at `AutoCombatService.java:172,621-730` and
`AutoCombatPanelService.java:88-103,295-376`. External D owns the caller file, so I12 read it only and made no caller
edit. Its refresh pressure, entry-maintenance merge, burst/urgent guards, state writes, and return continuation still
match the applicable `696a12b0` caller path apart from previously approved typed/local-Service substitutions.

## Implementation

The active method was not complete before I12: its final refresh action still called Cloud-local
`inputSequences.submitAndWait(...)`, so the real caller chain had no typed remote input terminal.

I12 changed only that existing action site in `refreshAutoCombatRoundsIfNeeded`:

1. The method still reads the cached estimate/timestamp/config, performs the one existing visible-round read, writes
   a visible round when present, and calls `resolveRoundsRefreshReason` in the same order.
2. A healthy `null` reason still logs and returns `false` before acquiring an execution context or sending input.
3. A non-null reason now obtains the current exact `TaskExecutionContext`; there is no default/global-window fallback.
4. The original single ordered action list is preserved exactly as `PRESS_ALT_8 -> SLEEP(1000ms)` and is submitted
   through `CloudGameClient.executeInputBundle` at stable address `auto-combat-panel/refresh-rounds`.
5. No action was split, reordered, duplicated, automatically retried, or wrapped in a new helper.
6. Only `EXECUTED` reaches the unchanged `recordAutoCombatRefresh(...) -> return true` branch. `NOT_EXECUTED`
   reaches the baseline input-failed warning and `false` result.
7. `STOPPED` uses the current context checkpoint and rejects a contradictory non-stop terminal; `OBSERVED` and
   `UNKNOWN` are rejected as transport/type failures rather than converted into successful refresh or round state.

## Typed Terminal

The refreshed action now closes through the existing typed transport:

`CloudGameClient.executeInputBundle`
-> retained action address for the current task run
-> remote `EXECUTE_INPUT_BUNDLE`
-> DHXY exact bound-window command handler
-> the existing single physical input queue
-> closed `InputBundleOutcome`
-> Cloud `ExecutionState` mapping
-> success-only `recordAutoCombatRefresh`
-> `boolean` result.

The terminal matrix is:

| terminal | service effect |
|---|---|
| `EXECUTED` | reset estimate to `25`, set refresh timestamp, return `true` |
| `NOT_EXECUTED` | preserve estimate/timestamp, log input failure, return `false` |
| `STOPPED` | current-context checkpoint transition; contradictory STOPPED is fatal |
| `OBSERVED` | reject wrong operation terminal |
| `UNKNOWN` | reject unresolved terminal; no reset and no automatic resend |

The panel observation preceding this method remains the already-wired typed `WindowFactKind.AUTO_COMBAT_PANEL`
path from `ensurePanelMatchVisible`. The baseline remaining-round OCR/read and its single-call position were left
unchanged by I12; this count unit changes only the final physical refresh execution boundary.

## Baseline Comparison

- Decision inputs and order unchanged: estimate -> last refresh -> now -> non-negative configured interval -> one
  visible-round read -> visible estimate update -> `resolveRoundsRefreshReason`.
- Reason priority unchanged: `UNKNOWN` for negative estimate, then `LOW_ROUNDS` at `<=10`, then `REFRESH_DUE`, else
  no refresh.
- Input order/delay unchanged: exactly one `Alt+8`, then exactly `1000ms` sleep.
- Success state order unchanged: set estimate `25`, set `lastAutoCombatRefreshAt(now)`, emit reset log, return `true`.
- Input-failure behavior unchanged for the closed `NOT_EXECUTED` equivalent: no state reset, warning, `false`.
- No new owner/session/TTL/retry/wrapper/state field/read/cleanup/phase transition/fallback was introduced.
- Active file SHA-256 after the scoped edit:
  `17F70569C2AF7C16B91BD333BED1C126E56697C999A8AF46ADD7271E6F85B6AE`.

## Scope QA

- Static method-scope assertions after the edit: `resolveRoundsRefreshReason=1`, `readRemainingRounds=1`,
  `executeInputBundle=1`, old direct refresh queue calls `=0`, refresh `PRESS_ALT_8=1`, refresh `SLEEP=1`,
  `recordAutoCombatRefresh=1`; the action/reset order assertion is true and `UNKNOWN` is explicitly closed.
- Java changes: only the direct refresh input block inside the approved Cloud
  `AutoCombatPanelService::refreshAutoCombatRoundsIfNeeded` method.
- Report changes: only this I12 report.
- No External A/B/C/D Java file was edited. No DHXY Java/shared contract/codec/handler/mechanics file was edited.
- Preserved the concurrent I7 typed visibility work and I11 no-code exit-count work already present in the same
  active class; no revert or overwrite was performed.
- Did not run Maven, tests, runtime, Task, UI, application/server/host, capture, OCR, input, or screenshot actions.
- Per the assignment, no Git mutation was performed: no commit, stage, checkout, reset, clean, delete, revert, branch,
  merge, or push. Read-only Git evidence commands were used for baseline/workspace comparison only.
- This is an implementation-worker handoff and does not claim `Approved`. Current state:
  `IMPLEMENTED / PARENT REVIEW + FRESH BUILD PENDING`; ledger remains unchanged until those parent gates pass.

## Parent Source Review #1 - 2026-07-15T03:10:00-04:00

父级独立复核 `AutoCombatPanelService:311-376`：原刷新决策、visible rounds 单读、reason 优先级、
`Alt+8 -> 1000ms` 顺序和 success-only 25 回合/timestamp 更新均保持；唯一动作改为 exact task context 的 typed
`executeInputBundle`，`NOT_EXECUTED` 保留 false，`STOPPED/OBSERVED/UNKNOWN` 不伪装成功且无自动重发。
结论：**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD**。fresh Cloud package 前不记账。
