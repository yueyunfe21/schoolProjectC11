# Internal I14 - AutoBattleTask::tryRunLocalTeamReturnSelfCheck

## CLAIMED

- task: `W-COUNT-AUTO-BATTLE-RETURN-SELF-CHECK-1`
- claimedAt: `2026-07-15T03:05:10-04:00`
- role: Internal I14 implementation-only Worker; not a reviewer
- countUnit: `AutoBattleTask::tryRunLocalTeamReturnSelfCheck`
- countDelta: `+1` (claimed only; not applied)
- business baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- approved target contract: CR244 member-owned return marker Set semantics recorded in
  `docs/PACKAGE_ARCHITECTURE.md` and the service migration matrix
- only Java write set:
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\AutoBattleTask.java`
- only report: this file

## Result

`BLOCKED P1=1 / NO JAVA CHANGE / COUNT NOT ELIGIBLE`.

The existing typed TeamReturn member observation, CommonBox pending consumer, return click mechanics, and closed
terminals are present. The count unit itself is not present: active Cloud `AutoBattleTask` still contains
`tryRunLocalTeamReturnRelease`, and the CR244 coordination owner required by the replacement self-check is absent
from active Cloud `TaskMaintenanceService`. That missing owner/state cannot be implemented honestly inside the
single allowed task file.

## Current Progress

- `CLAIMED` is persisted in this fixed report with the exact count unit, delta, baseline, role, and write set.
- The real FREE-idle caller, current old release branch, target tri-state branch, CommonBox consumer, TeamReturn typed
  observation/click terminal, production port registration, and CR244 owner boundary have been read end to end.
- No gap remains in I14's understanding of the task-local edit. Java implementation is stopped before the first
  source change because the required shared coordination API/state is outside the exclusive write set.
- This is not a dependency on a hypothetical helper: the target method's first call must resolve leader/session
  applicability, and its `ABSENT/PRESENT/not-applicable` branches must call shared clear/mark operations that do not
  exist in active Cloud source.

## Active Caller Audit

The source-level public lifecycle is real and context-bound:

`AutoBattleTask::execute(TaskExecutionContext)`
-> `TaskExecutionContextHolder.callWith(exactContext)`
-> `runAutoBattlePatrol`
-> `while (BotStatus.RUNNING)`
-> `handleAutoCombatTick`
-> when `TickResult.NONE` and `ActionState.FREE`, `maybeRunIdleMaintenance`
-> first maintenance branch.

The first active branch currently calls `tryRunLocalTeamReturnRelease(context)`. There are zero active Cloud
definitions or callers of `tryRunLocalTeamReturnSelfCheck`. The active loop also lacks the CR242 same-tick
`EXIT_RECOVERED && FREE` idle-maintenance branch recorded in the matrix; adding that caller repair alone would
still leave this count unit incomplete because the CR244 coordination owner below is missing.

## Required Target Flow

The approved count-unit flow is closed only if all of these steps remain together:

1. Resolve an applicable confirmed local leader/member relation. Candidate-only, external-leader, all-member,
   all-leader, ambiguous, and self-leader relations are not applicable.
2. If attribution is no longer applicable, clear only this member's stale pending-return entry and continue lower
   priority idle maintenance.
3. Probe the current member marker as `UNKNOWN / ABSENT / PRESENT`.
4. `UNKNOWN`: preserve the pending-return Set exactly; no event, click, or new truth; return `false`.
5. `ABSENT`: clear this member's pending entry only after the confirmed observation; return `false`.
6. `PRESENT`: idempotently add this member window to the session Set, then attempt CommonBox pending consume before
   the existing TeamReturn click chain.
7. Return `consumedBox || clickedReturn`; `false` continues the existing maintenance order, while `true` ends this
   idle-maintenance pass.

No task-private Set, fallback leader inference, capability substitution, extra observation, TTL, retry, cleanup,
or fail-open result may replace those steps.

## Exact Blocker

Active Cloud `TaskMaintenanceService` has none of the CR244 state/API surface required by the count unit:

- no `LocalTeamSessionState.pendingReturnWindowIds`;
- no `TeamReturnCoordination` value;
- no `resolveTeamReturnCoordination(TaskExecutionContext)` leader-attribution read;
- no `markPendingTeamReturnWindow(...)` idempotent member add;
- no `clearPendingTeamReturnWindow(...)` attribution-independent stale/member remove;
- no leader-readable pending-return state/change publication owned by that Service.

The currently available `isLocalSupportMemberSession(context)` only answers a coarser support-session boolean. It
cannot identify the owning leader, cannot represent the member-owned pending Set, and cannot preserve the required
rule that a degraded attribution may remove a prior member entry without allowing a new add.

Implementing these semantics in `AutoBattleTask` would create a second task-local owner that the leader cannot read,
would lose session/window teardown cleanup and state-change publication, and would violate the explicit ban on new
owner/session state. Calling the CR244 methods from `AutoBattleTask` without their definitions would leave Java
uncompilable. Skipping mark/clear would turn marker observations into uncoordinated clicks and would not implement
the count unit. The required repair therefore belongs to the existing `TaskMaintenanceService` CR244 coordination
surface, which is outside I14's exclusive Java write set.

This blocker is distinct from the frozen TeamReturn leader wait/precheck work. I14 did not take over or modify that
External task, `TeamReturnService`, `TaskMaintenanceService`, shared transport, DHXY mechanics, or any caller owner.

## Exact Call, Impact, And Repair Conditions

Blocked target call sites and consequences:

| Target call in `tryRunLocalTeamReturnSelfCheck` | Active Cloud dependency | Impact while absent |
|---|---|---|
| `taskMaintenanceService.resolveTeamReturnCoordination(context)` | no method/value owner | cannot distinguish a confirmed local leader/member relation from candidate, external, ambiguous, or self-leader state |
| `clearPendingTeamReturnWindow(context, not-applicable/ABSENT source)` | no method and no shared Set | a prior member entry cannot be removed safely after attribution degrades or marker disappearance is confirmed |
| `markPendingTeamReturnWindow(context, PRESENT source)` | no method and no shared Set | leader cannot observe that this exact member still requires return; a click alone is not a completion fact |
| Set-change publication/leader-readable state | no active Cloud owner | the member self-check cannot coordinate with the CR244 leader gates; a task-private boolean would be invisible and stale |

Repair may resume only after the existing `TaskMaintenanceService` owner is source-stable with all of the following,
without I14 taking over that External work:

1. `LocalTeamSessionState.pendingReturnWindowIds` keyed by stable member `windowId`, scoped to the existing local-team
   session and cleaned by its existing lifecycle owner.
2. `resolveTeamReturnCoordination(context)` using confirmed local leader attribution, rejecting candidate-only,
   external, ambiguous, all-member/all-leader, and self-leader cases while preserving the approved paused-leader
   relation.
3. Idempotent `markPendingTeamReturnWindow` and attribution-independent `clearPendingTeamReturnWindow`; only a real
   Set change may publish the existing CR244 state-change signal.
4. A leader-readable pending count/state owned by the same session Service. No new task-local owner, second session,
   TTL, retry, cleanup policy, or inferred negative truth is acceptable.
5. The External TeamReturn wait/precheck blocker remains separately owned. Its repair must not alter the member
   marker `UNKNOWN/PRESENT/ABSENT` mapping or the already typed member click mechanics consumed here.

After those prerequisites exist, the remaining I14-only repair is bounded to `AutoBattleTask.java`: restore the
approved FREE/`EXIT_RECOVERED` caller timing, replace the old capability-release helper with the named self-check,
preserve `UNKNOWN -> false/no mutation`, `ABSENT -> clear/false`, `PRESENT -> mark -> approved CommonBox consume gate
-> TeamReturn click`, and retain `consumedBox || clickedReturn` so false continues the existing maintenance order.
The exact approved CommonBox gate must be copied from the durable CR244/baseline authority; I14 must not decide to
remove or invent that gate from a dirty local variant.

## Existing Typed Terminal Evidence

The downstream mechanics needed after the missing coordination decision are already source-visible:

- `TeamReturnService::probeMemberReturnMarker` reads the exact current context through
  `CloudTeamReturnPort.observeButton(..., "member-marker-probe")`.
- `OBSERVED/PRESENT` maps to `PRESENT`; `OBSERVED/ABSENT` maps to `ABSENT`; capture unavailable, template
  unavailable, mechanics failure, `NOT_EXECUTED`, type mismatch, unresolved terminal, and caught failure map to
  `UNKNOWN`. An interrupted wait restores the interrupt flag and returns `UNKNOWN`, so it cannot clear pending.
- `CloudTeamReturnPortAssembly` reads typed `TEAM_RETURN_BUTTON`; DHXY handles it under the exact binding through
  `TeamReturnButtonLocalObservationMechanics`. Capture failure is not collapsed into template absence.
- `CommonBoxService::consumePendingBoxIfAllowed` retains the existing 30-second pending owner and window/role/
  identity/taskRun stale gates. Only its closed executed click clears pending.
- `TeamReturnService::clickReturnTeamIfPresent` preserves first observation -> found timestamp -> incense check ->
  second fresh observation -> independent X/Y `[-3,+3]` -> one typed input bundle
  `CLICK_LEFT(150ms) -> SLEEP(500ms)` -> clicked timestamp only after `EXECUTED`.
- The TeamReturn and CommonBox port assemblies are registered in the current production
  `CloudServiceConfiguration`; no duplicate adapter or bean is needed for this count unit.

These typed leaves do not compensate for the missing CR244 leader/session Set authority.

## Baseline Comparison

- The active Cloud method still reflects the older `696a12b0` release shape: confirmed local support session ->
  zero-wait `TEAM_RETURN` capability -> optional `COMMON_BOX` capability consume -> TeamReturn click ->
  `consumedBox || clickedReturn`.
- The matrix's named count unit and CR244 card explicitly replace that release gate with member-owned tri-state
  self-check plus leader-attributed pending Set. This is a documented approved post-baseline behavior contract, not
  an implementation choice available to I14.
- I14 introduced no additional behavioral difference. The active Cloud source remains unchanged because applying
  only the task half would be neither `696a12b0`-equivalent nor CR244-complete.

## Scope QA

- Java changes: none.
- Report changes: this file only.
- Target Java SHA-256 remained
  `E13BFFF740570B9C7B833F7EDCE336BFFE39FB89E410B630FF2156B69410264A` during the audit.
- Source counts at handoff: `tryRunLocalTeamReturnSelfCheck=0`, `tryRunLocalTeamReturnRelease=2` in active Cloud
  `AutoBattleTask`; all six CR244 coordination state/API names listed above are absent from active Cloud
  `TaskMaintenanceService`.
- No build, Maven, javac, test, replay, source guard, runtime, application/server/host, Task/poller, UI, capture,
  input, or Git command was run.
- Dirty/untracked work in both repositories was preserved; no unrelated file was edited, reverted, deleted, moved,
  staged, or cleaned.
- `countDelta=+1` remains unapplied. This implementation Worker does not claim `Approved`.

Handoff state:
`BLOCKED P1=1 / MISSING TASKMAINTENANCE CR244 COORDINATION OWNER / PARENT SCOPE DECISION REQUIRED`.

## Parent Blocker Review #1 - 2026-07-15T03:10:00-04:00

父级独立搜索确认 active Cloud `AutoBattleTask` 仍只有 `tryRunLocalTeamReturnRelease`，而
`TaskMaintenanceService` 不存在 CR244 的 `pendingReturnWindowIds`、coordination resolver、member mark/clear 与
leader-readable state。仅改 `AutoBattleTask.java` 会造第二 owner 或留下不可编译调用。结论：
**P0=0/P1=1/P2=0，BLOCKED_BY_SHARED_OWNER / countDelta=0**。返修条件与 Worker 所列五项一致；不得在本单
新增 owner/session/TTL/retry，不计入待构建。
