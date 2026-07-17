# Internal I11 - AutoCombatPanelService::recordCombatExit

## CLAIMED

- task: `W-COUNT-AUTO-COMBAT-PANEL-RECORD-EXIT-1`
- claimedAt: `2026-07-15T02:37:19-04:00`
- role: Internal I11 implementation-only Worker; not a reviewer
- countUnit: `AutoCombatPanelService::recordCombatExit`
- countDelta: `+1` (claimed only; parent source review and the unified fresh build gate remain external)
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- only Java write set: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\AutoCombatPanelService.java`
- only report: this file

## Baseline And Workspace Gate

- Read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, the CR271 material in `docs/ACTIVE_WORK.md` and
  `docs/PACKAGE_ARCHITECTURE.md`, the whole-Service plan, the service migration matrix, and
  `docs/业务逻辑.md` baseline rules before deciding whether a Java edit was needed.
- DHXY snapshot: `thin-client-design@0114604e1ff5f15491d2910959c45252e893d04f` with extensive existing
  dirty/untracked work. Cloud snapshot: `navigation-migration@3b988caa010254973e03342272e6d1d6a9685b01` with extensive existing
  dirty/untracked work, including the active untracked Cloud Service tree. All were preserved.
- The repository mirror
  `D:\mavenProject\dhxy-cloud-brain\migration-baseline\696a12b0\src\main\java\com\bot\dhxy\service\AutoCombatPanelService.java`
  and read-only `git show 696a12b0:...` evidence agree on the applicable baseline.
- No approved business difference; behavior-equivalent migration from `696a12b0`.

## Implementation Result

`NO_CODE_CHANGE`.

The active Cloud implementation already closes this count unit. Editing the Java file would create churn and risk
overwriting the concurrent `ensurePanelVisible` migration in the same class.

- Active `recordCombatExit()` normalized method SHA-256:
  `ED5C942ED3CE20789004423D0F489559EBB4FBDDF31C981AB57D5BDB6824BBA2`.
- `696a12b0` mirror `recordCombatExit()` normalized method SHA-256:
  `ED5C942ED3CE20789004423D0F489559EBB4FBDDF31C981AB57D5BDB6824BBA2`.
- Exact method-text comparison: `true`.
- Active and baseline constants remain `DEFAULT_ESTIMATED_ROUNDS=25`,
  `LOW_ROUNDS_REFRESH_THRESHOLD=10`, and `ESTIMATED_ROUNDS_PER_COMBAT=3`.

## Real Caller To Closed Continuation

The active production chain is:

`AutoBattleTask::runAutoBattlePatrol`
-> `handleAutoCombatTick`
-> public `AutoCombatService::handleCombatTick`
-> typed `BattleRadarService::checkAndSyncCombatState`
-> confirmed `IN_COMBAT -> FREE` transition emits one `combatExitPending`
-> `AutoCombatService::consumeExitAndRecover`
-> `consumeCombatExitSignalForExpectedWait` or `consumeCombatExitSignal`
-> only when the one-shot signal returns `true`, `AutoCombatPanelService::recordCombatExit`
-> `GameContext` estimated-round state update
-> existing later `AutoCombatService::maybeRunCombatMaintenance` reads the same state through
   `resolveRoundsRefreshReason`
-> existing `verifyAndAlignPanel(VERIFY_AND_REFRESH)` / panel refresh continuation.

BattleRadar observations are closed typed `WindowFactOutcome` values from the current task's game client. Only
`OBSERVED` facts enter the radar state machine; `NOT_EXECUTED` is no observation, a confirmed stop follows the
checkpoint path, and other terminals fail closed. The full-radar exit requires two misses plus a readable minimap;
the expected-exit path consumes only an exit produced after the current arm boundary.

The active `AutoCombatService::consumeExitAndRecover` method is also text-exact to `696a12b0`:

- active/baseline normalized SHA-256:
  `97168CA15EB93F883BCE0529D497619888F281022AA8E1199373ED0024E992C5`;
- exact method-text comparison: `true`.

It preserves the baseline order: consume confirmed exit, clear expected-wait/entry-maintenance state,
`recordCombatExit`, reset the player check counter, log exit context, then continue common-box and recovery logic.

## Count And State Semantics

- Full-tree reference scan found exactly one production call and one method definition for `recordCombatExit()`.
- The call is dominated by `if (!consumedExit) return false`; absent or stale signals cannot decrement rounds.
- Both BattleRadar consume methods clear their pending signal before returning `true`, so repeated ticks cannot
  decrement again for the same confirmed exit.
- When the estimate is positive, the method applies exactly `Math.max(0, estimatedRounds - 3)`, writes the same
  per-task bound `GameContext.State`, then emits the unchanged `before/after/decrement` log.
- Unknown state (`-1`) and zero remain unchanged, matching the baseline. The lower bound remains zero.
- `alignPanelIfNeeded` only updates panel alignment state and never calls `recordCombatExit`.
- `verifyAndAlignPanel` delegates to panel alignment and later round maintenance; it never calls
  `recordCombatExit`.
- `refreshAutoCombatRoundsIfNeeded` may replace the estimate with a visible round read or reset it to `25` after
  the existing refresh action. It does not subtract `3`, so it is not a duplicate combat-exit count.
- The other estimate writes are the existing visible-round synchronization and refresh reset. No second exit
  decrement, owner/session/TTL/retry/wrapper, observation, input, cleanup, or state field was added.

## Scope And Verification

- Java changes: none.
- Report changes: this file only.
- No runtime, Task, poller, UI, capture, input, automated test, source guard, Maven build, or application/server
  command was run, as explicitly required by the assignment.
- No commit, checkout, reset, clean, delete, revert, staging, or other Git write operation was performed.
- This implementation-worker report does not claim `Approved`. Current handoff state:
  `NO_CODE_CHANGE / REAL CHAIN EVIDENCED / PARENT REVIEW AND UNIFIED FRESH BUILD PENDING`.

## Parent Source Review #1 - 2026-07-15T02:47:00-04:00

父级独立复核 active `AutoCombatService.consumeExitAndRecover:345-367 -> recordCombatExit:396-404`，并对照
`696a12b0`：只有 closed one-shot exit signal 为 true 才执行；正数回合恰 `Math.max(0, before-3)`，未知/零不动，
后续 refresh/align 不重复扣减。结论：
**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD**。`countDelta=+1` 仍待 fresh Cloud package；
ledger 暂为 `189/407`。无已批准业务差异；按基线等价迁移。本 Worker 可关闭并续派新单。
