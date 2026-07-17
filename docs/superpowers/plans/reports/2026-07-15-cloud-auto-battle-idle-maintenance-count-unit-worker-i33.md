# W-COUNT-AUTO-BATTLE-IDLE-MAINTENANCE-1 Worker I33 Report

CLAIMED

- task: `W-COUNT-AUTO-BATTLE-IDLE-MAINTENANCE-1`
- claimedAt: `2026-07-15T04:23:50-04:00`
- countUnit: `AutoBattleTask::maybeRunIdleMaintenance`
- countDelta: `+1`
- 唯一写集:
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\AutoBattleTask.java`
  - `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-15-cloud-auto-battle-idle-maintenance-count-unit-worker-i33.md`

## Implementation Result

- role: Internal implementation Worker I33; implementation only, not reviewer.
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.
- disposition: `NO_CODE_CHANGE / ACTIVE CHAIN SOURCE-COMPLETE / DELIVERED_FOR_PARENT_REVIEW`.
- requested count delta: `+1`; I33 did not mutate any ledger or apply the count.
- Java change: none. The only actual write is this report.
- active Cloud `AutoBattleTask.java` SHA-256 remained
  `E13BFFF740570B9C7B833F7EDCE336BFFE39FB89E410B630FF2156B69410264A`.

## Baseline Gate

1. Read `docs/DHXY_CONTEXT.md` before source inspection.
2. Read `docs/业务逻辑.md`; its baseline authority statement at `:1255` identifies
   `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` as the confirmed pre-cloud local business baseline.
   This count unit has no separate 五倍/修罗 phase row in that document, so no task-phase rule was inferred or
   changed.
3. Compared active Cloud `AutoBattleTask.java:182-233` with the preserved baseline source
   `D:\mavenProject\DHXY-local-baseline\src\main\java\com\bot\dhxy\task\AutoBattleTask.java:184-235`.
   An in-memory line-for-line `Compare-Object` over those exact method ranges returned
   `METHOD_EXACT_MATCH=true`.
4. Full-file hashes differ because the surrounding promoted Cloud class has import/layout adaptation; the scoped
   `maybeRunIdleMaintenance` method body, branch order, early returns, request fields, result logging, fallback and
   state behavior are exact.

`无已批准业务差异；按基线等价迁移`。

## Matrix Count Boundary

The authoritative method matrix has one dedicated row at
`docs/superpowers/specs/2026-07-12-service-migration-matrix.md:1194`:

| Included count unit | Active caller | Decision and continuation | Disposition |
|---|---|---|---|
| `AutoBattleTask::maybeRunIdleMaintenance` | `runAutoBattlePatrol` FREE branch | local team-return release -> pending leader gate -> ordinary return-team gate -> follower/local-support/pathing gates -> typed maintenance request/result -> next poll | `NO_CODE_CHANGE`, requested `+1` |

The following adjacent or downstream units are reachability evidence only and are excluded from I33's count:

- `AutoBattleTask::getPollingIntervalMs` (`matrix:1197`): only proves the next-poll continuation.
- `TaskMaintenanceService::runOpportunisticMaintenance` (`matrix:1411`): already delivered and parent source-approved
  by I3; this report does not recount its internal broadcast/summon decisions.
- `TeamReturnService::clickReturnTeamIfPresent` (`matrix:1414`): already delivered by the TeamReturn count unit.
- `CommonBoxService::consumePendingBoxIfAllowed` (`matrix:1369`): already delivered by the CommonBox count unit.

## Active Chain Evidence

1. **Production caller and FREE gate.** `AutoBattleTask.java:139-149` runs one combat tick. A non-`NONE` result
   sleeps and continues at `:142-145`; only `TickResult.NONE` plus
   `GameContext.ActionState.FREE` reaches `maybeRunIdleMaintenance(context)` at `:146-148`.
2. **Stop checkpoint.** `AutoBattleTask.java:182-183` enters the count unit and immediately checks the existing
   task stop token. No wrapper, owner, session, TTL, retry or new state is introduced.
3. **Local team-return release has first priority.** `AutoBattleTask.java:184-186` calls
   `tryRunLocalTeamReturnRelease` and returns from the idle pass on its closed boolean `true`.
   The helper at `:235-255` preserves the existing local-support/member capability gates, CommonBox-first order,
   then the existing TeamReturn call, returning `consumedBox || clickedReturn`. These two downstream services are
   cited only as terminal reachability and are not recounted.
4. **Pending leader detection gate.** If local release did not handle the tick,
   `AutoBattleTask.java:187-192` checks `isPendingLocalSupportLeaderDetection(context)`, preserves the existing
   structured log fields (`session/requested/role`), and returns without creating new business truth from a negative
   signal. `TaskMaintenanceService.java:416-424` confirms the predicate is the existing candidate + no detected
   leader + session state + not-leader-absent decision.
5. **Ordinary return-team gate.** `AutoBattleTask.java:193-196` calls
   `clickReturnTeamIfPresent(context, "auto-battle")` only outside a local-support member session and returns when
   the closed boolean is true. `TeamReturnService.java:55-100` proves the existing typed boundary: exact-window
   observations, refreshed observation, typed click outcome, `EXECUTED -> true`, `NOT_EXECUTED/miss -> false`,
   unresolved terminal -> fatal. I33 does not duplicate that child unit.
6. **Follower/local-support/pathing decisions.** `AutoBattleTask.java:197-207` derives, in baseline order,
   `followerSupportMode`, `localSupportSession`, supported requested team task, local-support gate, and legacy
   pathing gate. Only an open `LEFT_TOP_STATUS` local capability consumes the follower safe window, followed by the
   existing stop checkpoint.
7. **Typed request construction.** `AutoBattleTask.java:208-228` constructs one existing
   `TaskMaintenanceRequest` and calls `runOpportunisticMaintenance` exactly once. Preserved fields are:
   `sourceTask="auto-battle"`, broadcast handling `true`, full-dialog fallback `false`, summon cleaning `true`,
   one-per-round for either follower gate, requested task key only for the legacy pathing gate, open pathing window
   only for that legacy gate, and `SUMMON_SKILL` capability only for the local-support gate.
   `TaskMaintenanceRequest.java:37-69` is the closed Lombok value/builder boundary; every used builder symbol exists.
8. **Closed maintenance result.** `TaskMaintenanceService.java:578-597` normalizes/checkpoints, preserves
   broadcast-first short-circuit, then summon maintenance or typed `NO_ACTION`. Its downstream terminal paths return
   `TaskMaintenanceResult` values, including broadcast outcomes at `:599-621`, summon success at `:786-787`, and
   typed retry-later failure at `:792-796`. `TaskMaintenanceResult.java:17-58` closes status, handled flags and message.
   No null/untyped terminal gap was found.
9. **Result consumption and next poll.** `AutoBattleTask.java:229-232` preserves the handled-only structured log and
   otherwise returns normally from the void method. Control resumes at `runAutoBattlePatrol:149`, which performs the
   existing `sleepSafely(context, getPollingIntervalMs(context))`; the while loop then starts the next poll at
   `:139-141`. The polling method is continuation evidence only, not part of I33's count.

## Static Verification And Scope

- Verified the complete active caller-to-closed-result-to-next-poll chain by source inspection only.
- Verified the scoped active method is line-for-line identical to the preserved 696 baseline method.
- Typed boundary gap: none.
- Stub/wrapper/filler: none added.
- Business decisions, priority, early returns, logs, fallback and state: unchanged.
- Forbidden additions: no owner/session/TTL/retry behavior added.
- Dirty protection: no Java or unrelated file was written, reverted or normalized.
- Per instruction, I33 ran no build, test, runtime, Task/poller, application/server, UI/capture/input, or Git command.

`DELIVERED | task=W-COUNT-AUTO-BATTLE-IDLE-MAINTENANCE-1 | worker=I33 | countUnit=AutoBattleTask::maybeRunIdleMaintenance | requestedCountDelta=+1 | countApplied=0 | Java=NO_CODE_CHANGE | businessDifference=NONE | typedBoundaryGap=NONE | verification=STATIC_SOURCE_ONLY | parentReview=PENDING`

## Parent Source Review #1 - 2026-07-15T04:20:00-04:00

父级独立复核 `runAutoBattlePatrol` FREE caller、stop checkpoint、local team-return release、pending leader
gate、ordinary return-team、follower/local-support/pathing gates、完整 `TaskMaintenanceRequest` 字段、typed
`TaskMaintenanceResult` 消费与 next-poll continuation；并确认 countUnit 方法体与 preserved `696a12b0`
对应范围一致。未重算下游 TeamReturn/CommonBox/TaskMaintenance 单元。

结论：**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD**；
`countUnit=AutoBattleTask::maybeRunIdleMaintenance`，`countDelta=+1`。fresh Cloud package 通过前 hard
ledger 仍为 `189/407`。
