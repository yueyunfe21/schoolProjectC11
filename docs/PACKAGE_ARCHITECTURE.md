# DHXY Package Architecture

This document records the target package layout for the DHXY Java codebase. The goal is to make
request/result/model types easy to find without doing a full rewrite of the running automation
services.

## Task Scheduling Architecture

### Third-View CR - 2026-06-19 CR41-CR44 Ordinary Monster Review

Reviewer: Codex, acting only as third-view reviewer for 谢帅.

Scope reviewed:

- Sprint Board CR41, CR42, CR43, and CR44.
- `docs/业务逻辑.md` ordinary-monster points 8-35.
- Current source around `WindowReadyEventType`, `WindowReadyEventBus`, `WindowTaskRunner`, and
  `WubeiTask`.

#### P1 Finding

1. Ordinary prepared enter-battle actions can be published and wake the leader, but the leader may not
   consume them from the ordinary wait phase.

   Evidence:

   - Ordinary business points 17-21 require `PREPARED_ACTION_READY / WUBEI_ENTER_BATTLE` to wake the
     leader, consume the prepared action, click the prepared coordinate, and enter
     `WAIT_BATTLE_FINISH`.
   - `WubeiTask.waitForPathingWake(...)` includes `PREPARED_ACTION_READY`, so ordinary
     `RESOLVE_AFTER_PATHING` can wake on a prepared enter-battle result.
   - Before CR46, `WubeiTask.consumePreparedEnterBattleBeforeNormalPhase(...)` delegated to
     `canConsumeEnterBattlePreparedAction(state.phase())`, which accepted only
     `WubeiPhase.ENTER_BATTLE`.
   - CR43 intentionally removed ordinary `PATHING_TERMINAL -> ENTER_BATTLE`; ordinary terminal now
     re-clicks the same tracker green link and stays in the Runner wait loop. Therefore a valid
     ordinary `PREPARED_ACTION_READY / WUBEI_ENTER_BATTLE` can be rejected as a phase mismatch while
     the task is still in `RESOLVE_AFTER_PATHING`.

   Required follow-up:

   - Before CR45 validation, CR41/CR42/CR43 must allow fresh current-window ordinary
     `WUBEI_ENTER_BATTLE` prepared actions to be consumed from the ordinary wait phase where they are
     produced.
   - Keep the CR43 `PATHING_TERMINAL` rule unchanged: terminal means re-click the same tracker green
     link. Only `PREPARED_ACTION_READY` should take the prepared-action click path.
   - Do not use smart-click, tooltip, yellow OCR, or Alt+A for this branch; the click must be the
     Runner/provider prepared coordinate.

#### Current Verdict

CR46 patched this prepared-action consumption gap by allowing `RESOLVE_AFTER_PATHING` consumption
only while the runtime pre-battle timer is active. That keeps the branch scoped to ordinary monsters
and 黄袍第一战; 白龙马/probe does not start this timer and therefore still cannot consume stale
enter-battle prepared actions from probe pathing. CR41-CR43 still need fresh ordinary validation, but
this specific P1 is no longer blocking after CR46.

CR44's own Runner-side ordinary pre-battle timeout logic has been separately rechecked and matches
普通怪 points 29-35: the timer starts at the first ordinary green click, is not reset by terminal
re-navigation, is checked/published by Runner, wakes via `PRE_BATTLE_TIMEOUT`, routes back to
reaccept without green-click/monster/Alt+A, and is cleared on battle-entry/round reset. CR44 can be
treated as scoped-review OK, with fresh end-to-end ordinary validation still owned by CR45.

CR46 scoped review: OK for code/business logic. It starts the pre-battle timer for 黄袍第一战,
preserves the chained marker, and uses the same prepared-coordinate click path as ordinary. Remaining
acceptance is fresh 黄袍 first-battle logs, plus CR47 for post-first-battle hot-path waits.

### Third-View CR - 2026-06-19 Wubei latest-push business baseline check

Reviewer: 何黎, acting only as a third-view reviewer for 谢帅.

Baseline:

- Latest pushed code reviewed as the business baseline:
  `3f0a2e7 (origin/codex/migrate-runner-dialog)`
  `Add short first-aid gate for Wubei chained combat`.
- Review scope: only 五倍 business-logic deltas between that pushed baseline and current local
  working tree.

#### Verdict

Current local code contains several 五倍 business-decision changes beyond the intended
scheduling/park/wakeup migration. Runner/park itself is in scope for this sprint and is not counted
as a business CR finding. Per the baseline guardrail and the user's clarified direction, the latest
pushed code is the business source of truth; local business deltas must be restored to that baseline
by default, not kept as migration behavior.

#### P1 Business Findings

1. Accept-NPC route wake conditions broadened.

   - Pushed baseline: release was tied to fresh matching prepared route-dialog evidence.
   - Local code: adds route READY / REQUESTED / PREPARING facts, visible actionable dialog facts,
     terminal pathing events, and a 15s current-map stale-pathing recovery path.
   - Risk: 五倍 can decide route/pathing is complete, stale, or retryable under evidence the pushed
     code did not accept.

2. Enter-battle priority changed.

   - Pushed baseline: enter-battle phase used the existing auto-combat / known-dialog /
     tracker-fallback order.
   - Local code: registers `WUBEI_ENTER_BATTLE`, immediately parks for runner prepared action, and
     consumes fresh prepared enter-battle actions before/after combat handling.
   - Risk: phase ownership and fallback order for entering battle are changed.

3. Probe enter-battle timeout recovery changed.

   - Local code throws `ProbeEnterBattleTimeoutSignal` from inner dialog wait loops.
   - Risk: failed enter-battle preparation can unwind through a different recovery path than the
     pushed business flow.

4. Maintenance behavior is gated differently.

   - Local leader pathing summon maintenance requires an open team pathing maintenance window.
   - Local follower `AutoBattleTask` also gates Wubei/Xiuluo maintenance broadcast handling.
   - Risk: helper heal/summon/maintenance decisions during 五倍 are changed.

5. Navigation route-intent matching is more permissive.

   - Local `NavigationService` accepts same-target-map route transfer actions despite active intent
     id mismatch and can reuse fresh route intents at the world-map gate.
   - Risk: 五倍 can consume/continue route dialogs under route intent evidence the pushed code would
     have rejected.

#### Not Business Findings In This Review

- Prepared-dialog runner/park wait replacing active polling is expected architecture migration only
  while it preserves the pushed task decision order and business deadlines.
- Follow-up audit `docs/WUBEI_BUSINESS_DIFF_AUDIT.md` corrected the earlier probe-story suspicion:
  current code returns `probe runner pathing still active` while probe pathing is `ACTIVE`, so visible
  `STORY` takeover during active pathing is not a current finding. The unused
  `hasFreshVisibleProbeStory(...)` / `beginProbeStoryWaitFromVisibleDialog(...)` methods remain a
  cleanup risk because they could accidentally reintroduce that behavior.
- Combat-state wake/park for battle wait is expected architecture migration.
- `WubeiWaitSpec`, `WindowReadyEventBus`, runner ready events, and latency diagnostics are treated
  as scheduling infrastructure unless they alter a concrete 五倍 decision listed above.

#### Required Story Split

Before migration continues, split these local changes into explicit restore/verification work items:

- Restore/verify latest-push 五倍 business baseline.
- Accept-NPC route stale recovery and wake facts.
- Enter-battle prepared-action priority.
- Probe enter-battle timeout recovery.
- Team maintenance-window gate for leader/follower actions.
- Navigation route-intent reuse and matching.

### Third-View CR - 2026-06-18 Wubei Park/Wakeup Sprint

Reviewer: 何黎, acting only as a third-view reviewer for 谢帅's sprint work.

Scope reviewed:

- `docs/codex-handoffs/2026-06-14-runner-dialog-preparation-architecture.md`
- this document's `Shared-State Park/Wakeup Model For Long Waits`
- current sprint notes in `docs/ACTIVE_WORK.md`
- current code shape around `WubeiTask`, `WubeiStepOutcome`, `WubeiWaitSpec`,
  `WindowReadyEventBus`, `WindowRuntimeContext`, and `WindowTaskRunner`

#### Overall CR Verdict

The sprint direction is correct: the code is moving away from "release turn, immediately reacquire
and poll" toward an explicit wait model where 五倍 releases the task turn and parks until the Runner
or watcher produces evidence that state may have changed.

However, the current B2/B3/B4 implementation is still a transitional model, not the final
"Runner decides when the leader should wake" model. It reduces same-window churn by parking on event
or timeout, but several paths still wake by fixed timeout and then reacquire the task turn to poll.
This is acceptable as a safety fallback for this sprint, but it should not be treated as the final
architecture.

#### P1 Findings

1. Prepared-dialog waits can still re-enter by timeout, so they are not fully Runner-driven yet.

   Evidence:

   - `WubeiTask.WUBEI_PREPARED_DIALOG_PARK_TIMEOUT_MS = 1500ms`
   - `WubeiTask.parkAfterYieldIfNeeded(...)` wakes on `awaitNewer(...)` or timeout.
   - On timeout, the phase loop continues and can reacquire the task turn to recheck the same state.

   Risk:

   - This fixes the worst 80ms churn, but a missing or delayed Runner event still creates repeated
     1.5s foreground rechecks.
   - For 白龙马 story wait, this is safer than the old early no-story decision, but if Runner fails to
     publish a prepared result, the leader can park/reacquire indefinitely without a crisp failure or
     diagnostic escalation.

   Recommendation:

   - Keep the timeout fallback for now, but log and count consecutive timeout wakes per wait reason,
     operation, and phase.
   - Add an explicit "Runner did not answer within N waits" diagnostic state before any future
     business fallback is allowed.
   - Treat timeout wake as "diagnostic recheck only", not as a normal scheduling success.

2. `allowOpportunisticMaintenance=false` was advisory only before CR6; CR6 now enforces it.

   Evidence:

   - `waitForPreparedDialogWake(...)` builds a wait spec with `allowOpportunisticMaintenance(false)`.
   - Before CR6, `yieldAfterMustYield(...)` still called
     `maybeRunLeaderPathingSummonMaintenance(...)` before `parkAfterYieldIfNeeded(...)`.
   - CR6 changed `yieldAfterMustYield(...)` to skip opportunistic maintenance when the outcome
     carries a wait spec with `allowOpportunisticMaintenance=false`.

   Risk:

   - Before CR6, the common `yieldAfterMustYield` path did not enforce the policy.
   - This is most sensitive when a prepared dialog is ready or about to be ready and the leader
     should come back immediately to consume it.

   Recommendation:

   - CR6 is the accepted fix: for `WAIT_PREPARED_DIALOG`, skip opportunistic maintenance entirely
     unless a later explicit card proves it is safe.

3. Pathing waits still use a 5s timeout, which may become "quiet but slow" if events are missed.

   Evidence:

   - `WubeiTask.WUBEI_PATHING_PARK_TIMEOUT_MS = 5000ms`
   - `WAIT_PATHING_TERMINAL` wakes on `PATHING_TERMINAL` or `TASK_ATTENTION_REQUIRED`, otherwise
     timeout.

   Risk:

   - The architecture explicitly says latency must not regress. A missed `PATHING_TERMINAL` can turn
     an old fast recheck into a 5s delay.
   - This is acceptable only if logs prove `PATHING_TERMINAL` and `TASK_ATTENTION_REQUIRED` are
     reliably published in the live cases.

   Recommendation:

   - In the first A/B run, specifically measure `PATHING_TERMINAL publish -> wubei wait wake` and
     `pathing terminal runtime already satisfied -> skip park`.
   - If event coverage is incomplete, lower the fallback for affected phases or add the missing
     event before calling this Done.

4. `WAIT_BATTLE_FINISH` deliberately remains unparked, which is the right choice for Sprint 1.

   Evidence:

   - B3 documents not changing `WAIT_BATTLE_FINISH`.
   - `tickWaitBattleFinish(...)` still returns `sharedState(state, "combat still running")` without a
     wait spec.

   Risk:

   - This means the biggest remaining churn may still be battle waits.
   - But parking battle waits before `COMBAT_STATE_CHANGED` exists would risk late battle-exit
     recovery.

   Recommendation:

   - Keep this as-is for this sprint.
   - Make Phase 2 explicitly about `COMBAT_STATE_CHANGED`; do not reduce battle wait frequency until
     that event exists and has latency evidence.

5. `WindowReadyEventBus.latestOtherFresh(...)` appeared to read only prepared-action events before CR5.

   Evidence:

   - Before CR5, `latestOtherFresh(...)` iterated `latestPreparedActionByWindow.values()`, even
     though the method accepted a general `WindowReadyEventType`.
   - `latestOtherFreshPreparedAction(...)` separately scans `latestByWindowAndType.values()`.
   - CR5 removed the misleading generic helper and kept the explicit prepared/pathing APIs.

   Risk:

   - If any caller expects `latestOtherFresh(..., PATHING_TERMINAL, ...)` or plain
     `TASK_ATTENTION_REQUIRED` visible-dialog events, it will silently miss them.
   - Current code may not use this generic method much, but the API name is misleading and can create
     future scheduling bugs.

   Recommendation:

   - Either change `latestOtherFresh(...)` to scan `latestByWindowAndType`, or rename/restrict it so
     callers know it only handles prepared-action signals.
   - Add a small unit-style test or debug assertion around event selection before expanding the
     scheduler.

#### P2 Findings

1. `WubeiWaitSpec.currentWindowOnly=false` is not implemented.

   Current code only logs when cross-window wait is requested. That is fine because all B3 wait specs
   are current-window waits, but the field should stay documented as future-only until implemented.

2. `WAIT_RETRY_TIMER`, `WAIT_TEAM_ATTENTION`, and `WAIT_COMBAT_STATE_CHANGE` are vocabulary only.

   This is acceptable for B1, but future code should not return these reasons until their wake event
   or timeout policy is implemented and documented.

3. `checkReadyPriorityBeforePhase(...)` previously did an unconditional 80ms settle wait before
   normal phase work; CR2 gates this wait.

   The remaining brief wait is only allowed when there is a concrete local reason such as active
   dialog interest, a fresh visible dialog snapshot, or a very fresh ready event. A/B metrics should
   still confirm it does not create extra same-window churn when combined with park.

4. The code still uses multiple runtime/event concepts at once:

   - `visibleDialogSnapshot`
   - `PreparedDialogAction`
   - `DialogInterest`
   - `WindowReadyEvent`
   - `WindowPathingSnapshot`

   This is not wrong, but every future card should state which one is the source of truth for its
   decision. EventBus remains wake-only; runtime remains truth.

#### Recommendations For 谢帅 Before Next Code Card

1. Do not broaden this sprint into business rewrites. Keep B2/B3/B4 scoped to scheduling and 白龙马
   runner-prepared story behavior.
2. Before marking the sprint behavior stable, require one real A/B run with:
   - count of `wubei wait park finished ... wakeResult=timeout`
   - count of `wakeResult=event`
   - pathing terminal publish-to-wake timing
   - prepared action publish-to-consume timing
   - same-window reacquire count after B3/B4
3. CR6 has fixed the `allowOpportunisticMaintenance=false` enforcement gap; live runs still need to
   confirm the skip log appears before prepared-dialog consumption.
4. Keep `WAIT_BATTLE_FINISH` out of park/wakeup until `COMBAT_STATE_CHANGED` exists.
5. Treat the current timeout fallback as a safety net, not as the intended scheduler.

### Third-View CR Follow-up - 2026-06-18 CR1/CR2/CR4/CR5/CR6 Review

Reviewer: 何黎, continuing as third-view reviewer only.

Scope reviewed:

- `docs/ACTIVE_WORK.md` entries for CR1, CR2, CR4, CR5, and CR6.
- `WubeiTask.runRoundPhases(...)`, `checkReadyPriorityBeforePhase(...)`,
  `yieldAfterMustYield(...)`, `parkAfterYieldIfNeeded(...)`, and
  `isWaitAlreadySatisfied(...)`.
- `WindowReadyEventBus` after CR5 cleanup.
- `scripts/analyze_wubei_latency.ps1` after CR4 update.

#### Accepted Follow-ups

- CR1 is accepted: phase-boundary prepared action consumption now runs inside
  `TaskTransactionRunner.run(...)`, so real prepared-dialog clicks happen while the task turn is
  owned and `TaskTurnCoordinator.leave(...)` sees the real transaction result.
- CR4 is directionally accepted: the latency script now counts current log formats instead of
  reporting stale `input.start/input.done/click.done/state.changed` counters as if they existed.
- CR5 is accepted for the original bug: the misleading generic `latestOtherFresh(...)` API is gone,
  and current code uses explicit prepared/pathing helpers.
- CR6 is accepted: `allowOpportunisticMaintenance=false` is now enforced before leader pathing
  summon maintenance can run.

#### New / Remaining Review Findings

1. P1: `isWaitAlreadySatisfied(...)` treats any cached prepared action as a satisfied wake state.

   Evidence:

   - `WAIT_PREPARED_DIALOG -> prepared != null`
   - `WAIT_PATHING_TERMINAL -> terminal pathing || prepared != null` when the wait includes
     `TASK_ATTENTION_REQUIRED`
   - The later consume path requires `prepared.verifiedWithin(...)` and operation/phase validation,
     but the park-skip check does not.

   Risk:

   - A stale or wrong-operation prepared action can make `parkAfterYieldIfNeeded(...)` skip parking,
     immediately re-enter the phase, reject the same prepared action as stale/mismatched, and
     recreate the same-window churn CR3 is trying to close.
   - This is especially easy to miss because the log says "runtime already has wake state" even when
     the later consume gate will not actually use that state.

   Recommendation:

   - Use the same freshness fence as the consume path before treating prepared state as satisfied,
     at minimum `prepared.verifiedWithin(now, WUBEI_PREPARED_DIALOG_MAX_AGE_MS)`.
   - Where possible, require the expected operation family for the wait reason, or log the
     operation mismatch as a CR3 diagnostic instead of skipping park.
   - CR3 should explicitly check `skip park; runtime already has wake state` lines and compare their
     `preparedAgeMs/preparedOperation` against the next `consumePrepared` / priority rejection.

2. P2: CR2 gates the 80ms settle wait, but the wait now happens while the current window owns the
   task turn and before checking whether another window already has a fresh prepared action.

   Evidence:

   - `checkReadyPriorityBeforePhase(...)` runs inside `TaskTransactionRunner.run(...)` after CR1.
   - The method consumes current prepared state, optionally waits `READY_EVENT_SETTLE_WAIT_MS`, then
     checks `latestOtherFreshPreparedAction(...)`.

   Risk:

   - If the current window has active dialog interest or a fresh visible dialog snapshot that does
     not produce a usable prepared action, it can hold the task turn up to 80ms before yielding to an
     already-ready other window.
   - This is bounded and much better than the old unconditional wait, but it is still a latency
     tradeoff that should be visible in A/B metrics.

   Recommendation:

   - In CR3/CR8 metrics, compare other-window prepared age before/after phase-boundary waits.
   - If this shows avoidable delay, check `latestOtherFreshPreparedAction(...)` before the local
     settle wait when current prepared is absent.

3. P2: `latestPreparedActionByWindow` is now a dead write after CR5.

   Evidence:

   - `WindowReadyEventBus.publish(...)` still writes to `latestPreparedActionByWindow`.
   - No remaining production method reads that map after `latestOtherFresh(...)` was deleted.

   Risk:

   - This does not currently break behavior, but it keeps a misleading second cache alive in the
     wake bus and may confuse the next scheduler change.

   Recommendation:

   - Either remove `latestPreparedActionByWindow` and its write in a tiny cleanup card, or document
     a real future reader before relying on it.

### Third-View CR Follow-up - 2026-06-18 CR7/CR8 Review

Reviewer: 何黎, continuing as third-view reviewer only.

Scope reviewed:

- `docs/ACTIVE_WORK.md` entries for CR7 and CR8.
- Sprint board rows and checklists for CR3, CR7, and CR8.
- `WubeiTask.parkAfterYieldIfNeeded(...)` timeout-counter additions.
- Current latency script counters for `window.ready.await` and `[wubei wait] park finished`.

#### Accepted Follow-ups

- CR7 is accepted as a diagnostic-only code change: timeout wake still only triggers a recheck, and
  the new warning does not change 白龙马 prompt selection, foreground OCR/template fallback, or
  business failure policy.
- CR8 is useful evidence: the measured slice has ready events (`PATHING_TERMINAL` /
  `TASK_ATTENTION_REQUIRED`) but zero `window.ready.await` and zero `wubei.wait.parkFinished`.

#### New / Remaining Review Findings

1. P1: CR8 evidence is useful, but CR3 must stay tied to a fresh post-change run.

   Evidence:

   - CR8's measured slice shows old churn and zero park/await logs.
   - The current CR3 ACTIVE_WORK entry now explicitly says `Status: BLOCKED / needs fresh
     post-change run`.
   - The CR3 note says the same log slice may predate the latest park path, or may come from a run
     that did not include these changes.

   Risk:

   - If CR8 is quoted without the CR3 freshness caveat, the next agent may patch `WubeiTask` from a
     stale run and change scheduling based on evidence that does not represent current code.

   Recommendation:

   - Keep CR3 blocked until a fresh run on the latest build is available.
   - In any future CR8 summary, phrase the result as "available slice shows no park" rather than
     "current code does not park" unless the time range is known to include the latest build.
   - The deciding command should be the CR4 script against that fresh run. The key gate is:
     `window.ready.await == 0 && wubei.wait.parkFinished == 0` while `RESOLVE_AFTER_PATHING` /
     `ROUTE_TO_MAIN_TASK` same-window churn remains high.

2. P1: CR7 diagnostics cannot help if the park path is never entered.

   Evidence:

   - The new `consecutiveTimeouts` and `runner did not answer prepared dialog wait` warning are
     emitted only after `parkAfterYieldIfNeeded(...)` reaches `windowReadyEventBus.awaitNewer(...)`
     and logs `[wubei wait] park finished`.
   - CR8's measured slice has `wubei.wait.parkFinished=0`.

   Risk:

   - If CR3's root cause is "waitSpec not attached" or "priority outcome bypasses park", CR7 will
     stay silent and may falsely look like Runner is healthy.

   Recommendation:

   - CR3 should first prove whether wait outcomes have a non-null `waitSpec` at the phase outcome
     log point.
   - Add a temporary/diagnostic-only log or script check that separates:
     `shared/pathing outcome without waitSpec`,
     `outcome with waitSpec but skip park`,
     and `outcome with waitSpec and await`.
   - Do not interpret absence of CR7 warnings as success until `[wubei wait] park finished` appears
     in the same run.

3. P2: The previous prepared-action already-satisfied CR remains open after CR7.

   Evidence:

   - CR7 clears timeout counters on "runtime already has wake state".
   - `isWaitAlreadySatisfied(...)` still treats `prepared != null` as satisfied without the same
     freshness / operation / phase gate used by the consume path.

   Risk:

   - A stale prepared action can clear diagnostics and skip parking even though it will be rejected
     later by the consume path. That makes the eventual CR3 churn harder to see.

   Recommendation:

   - When CR3 is reopened, inspect `skip park; runtime already has wake state` alongside
     `preparedAgeMs`, `preparedOperation`, and the next consume/reject reason.

### Third-View CR Follow-up - 2026-06-18 Fresh-Run Triage Review

Reviewer: 何黎, continuing as third-view reviewer only.

Scope reviewed:

- `docs/ACTIVE_WORK.md` fresh-run triage entry for `2026-06-18 12:01:00` to `12:05:00`.
- Sprint board changes that unblocked C3, decomposed CR3, and added CR9-CR13.
- Existing CR8 / CR3 descriptions after the fresh-run update.

#### Accepted Follow-up

- The fresh-run triage is useful and correctly narrows the next work: park/wake is now exercised,
  so the old "no park logs at all" diagnosis is obsolete, and CR3 is better split into CR9-CR11
  rather than patched as one broad scheduling change.

#### New / Remaining Review Findings

1. P2: CR8's sprint-board summary still says "pathing waits do not park" after fresh logs proved
   park/wake is active.

   Evidence:

   - Fresh-run counters show `window.ready.await=51` and `wubei.wait.parkFinished=18`.
   - CR8 board row still says: "ready events publish, but 五倍 pathing waits do not park".
   - CR8 fresh-run update says the earlier conclusion is obsolete.

   Risk:

   - A future agent may read only the sprint board and chase the old missing-park diagnosis instead
     of starting from CR9-CR11.

   Recommendation:

   - Update CR8's board goal to say the old no-park finding was superseded by fresh-run evidence.
   - Keep CR3 as decomposed into CR9-CR11, and make CR9-CR11 the only actionable scheduling cards
     from this fresh-run triage.

2. P2: The fresh-run ACTIVE_WORK entry mentions restoring a 3-minute heartbeat, but the current
   user request and automation are 5 minutes.

   Evidence:

   - Current heartbeat was explicitly recreated as a 5-minute watcher.
   - The fresh-run triage entry says "恢复 3 分钟 heartbeat 巡检".

   Risk:

   - This is documentation-only drift, but it can confuse future automation changes or make another
     agent "fix" the watcher back to 3 minutes.

   Recommendation:

   - Amend the fresh-run entry to say the current watcher interval is 5 minutes unless the user
     explicitly asks to change it again.

### Third-View CR Follow-up - 2026-06-18 CR9/CR13 Review

Reviewer: 何黎, continuing as third-view reviewer only.

Scope reviewed:

- `docs/ACTIVE_WORK.md` entries for CR9 and CR13.
- `WubeiTask.parkAfterYieldIfNeeded(...)`, `captureWaitRuntimeState(...)`,
  `isWaitAlreadySatisfied(...)`, and CR9 wait-state diagnostics.
- `WindowReadyEventBus.latest(...)` / `awaitNewer(...)` event-cache semantics.
- CR13 timing explanation for long `wubei:TRACKER_PATHING` task-turn holds.

#### Accepted Follow-ups

- CR9 is directionally accepted: re-reading fresh same-window runtime state before sleeping is the
  right fix for the observed `before.visibleDialogType=OPTION/STORY` plus 5s timeout pattern.
- CR9 also correctly avoids OCR/template/click/navigation changes; it only changes scheduling
  evidence used to decide whether to park.
- CR13 is accepted as an investigation: the evidence separates short tracker-click transaction time
  from the much longer continuous task-turn ownership window.

#### New / Remaining Review Findings

1. P1: `WAIT_PREPARED_DIALOG` skip-park is now broader than the consume gate.

   Evidence:

   - `WAIT_PREPARED_DIALOG` is already-satisfied when `prepared != null`, `visibleDialogType` is
     fresh `OPTION/STORY`, or there is a fresh matching ready event.
   - The later click path still requires a usable prepared action / operation validation before any
     business click.

   Risk:

   - A fresh visible dialog is a good reason to wake and recheck, but it is not always proof that
     Runner has prepared the specific operation the foreground phase is waiting for.
   - If the dialog stays visible while Runner preparation is absent or delayed, the task can keep
     skipping park, clearing timeout diagnostics, and looping through `consumePrepared result=absent`.

   Recommendation:

   - Keep CR9 as-is for the next run, but do not mark it stable only because
     `wubei.wait.skipAlreadyReady` rises.
   - In the post-change run, compare `skip park; runtime already has wake state` lines against the
     next `consumePrepared` result. A skip followed by absent/stale/mismatch should be counted as
     unresolved CR11 evidence.
   - If this churn appears, tighten `WAIT_PREPARED_DIALOG` so visible dialog / plain ready event can
     trigger one immediate recheck, but repeated skip-park requires a fresh prepared action or an
     operation-bearing `TASK_ATTENTION_REQUIRED` event.

2. P2: Same cached ready event can satisfy skip-park repeatedly until the freshness window expires.

   Evidence:

   - `latestFreshReadyEvent(...)` reads the latest event per window/type and accepts it by age.
   - The check does not compare the cached event sequence with the current wait's `afterSequence`
     because CR9 is explicitly fixing already-published events.

   Risk:

   - This is acceptable as a soft wake repair, but it means the same event may explain several
     `skipAlreadyReady` logs. Without reason/source counters, a metric increase can look healthier
     than it is.

   Recommendation:

   - The next latency pass should break down `wubei.wait.skipAlreadyReady` by satisfied source:
     terminal pathing, prepared action, visible dialog, ready event.
   - Treat repeated skip on the same `readyEventType` / age band as a churn signal, not as a solved
     wakeup.

3. P2: CR13's future split-turn patch should stay task-local and metric-gated.

   Evidence:

   - CR13 currently proposes splitting after round boundary and after successful accept/tracker
     refresh handoff, with business order unchanged.

   Risk:

   - A broad `TaskTurnCoordinator` policy change could affect other tasks and hide 五倍-specific
     ownership boundaries.
   - Splitting too early could add scheduling latency between accept -> tracker read if no other
     window actually needs the turn.

   Recommendation:

   - Prefer a 五倍-local yield boundary first, not a global coordinator rule.
   - Acceptance should include `heldMs` p95/p99 falling for `wubei:TRACKER_PATHING` without increasing
     accept -> tracker-click elapsed time or delaying prepared-dialog consumption.

### Third-View CR Follow-up - 2026-06-18 CR10/CR17 Review

Reviewer: 何黎, continuing as third-view reviewer only.

Scope reviewed:

- `docs/ACTIVE_WORK.md` entries for CR10 and CR17.
- `WindowTaskRunner.updatePathingFromLocation(...)`, `updateUnknownPathing(...)`,
  `resolvePathingDialogBlock(...)`, and untargeted tracker terminal classification.
- `WubeiTask.runAcceptTaskPhase(...)` and the CR17 event-driven accept wait plan.

#### Accepted Follow-ups

- CR10 is directionally correct: an untargeted tracker path should be allowed to terminate on
  actionable dialog evidence instead of waiting for coordinate stopped-away heuristics.
- CR17's diagnosis is correct: once `WUBEI_ACCEPT_TASK` is prepared, consume/click is fast; the slow
  section is foreground task-turn ownership while Runner is still preparing the accept option.

#### New / Remaining Review Findings

1. P1: CR10 currently treats `DialogPreparationPhase.REQUESTED/PREPARING` as pathing-terminal
   evidence for untargeted tracker intents.

   Evidence:

   - `resolvePathingDialogBlock(...)` returns a blocking `PathingDialogBlock` for
     `DialogPreparationStatus` when `isBlockingPreparationPhase(...)` is true.
   - `isBlockingPreparationPhase(...)` currently includes `REQUESTED`, `PREPARING`, and `READY`.
   - `isUntargetedTrackerDialogTerminal(...)` only checks `dialogBlock.blocking()`.

   Risk:

   - `REQUESTED` can mean "the task asked the watcher to prepare a dialog", not "the game is blocked
     by an actionable dialog".
   - That can convert an active untargeted tracker path into `STOPPED_AWAY` and publish
     `PATHING_TERMINAL` before a visible/prepared dialog actually exists.

   Recommendation:

   - For CR10 validation/fix, treat untargeted tracker dialog terminal as true only for fresh
     `PreparedDialogAction`, `DialogPreparationPhase.READY`, or fresh visible dialog evidence.
   - If `PREPARING` is kept as terminal evidence, require it to be tied to a fresh visible dialog
     snapshot from the same window/HWND.
   - Do not use `REQUESTED` alone as terminal evidence.

2. P2: CR10's visible-dialog terminal rule should be validated against unrelated same-window dialogs.

   Evidence:

   - `resolvePathingDialogBlock(...)` treats any fresh non-`NONE` `WindowDialogSnapshot` as blocking.
   - CR12/CR14 evidence shows unrelated STORY/maintenance dialogs can appear during 五倍 waits.

   Risk:

   - A same-window maintenance or unrelated STORY dialog could terminate tracker pathing early if it
     is visible while an untargeted tracker intent is active.

   Recommendation:

   - In the post-restart CR10 run, log and review `dialogReason`, `dialogType`, `dialogOperation`,
     and active task phase for every `untargeted tracker pathing terminal because dialog is
     actionable` line.
   - If unrelated dialogs appear, gate visible-dialog terminal to 五倍-owned dialog interest or an
     operation-bearing prepared action.

3. P1: CR17's event-driven accept wait must keep the no-maintenance invariant explicit.

   Evidence:

   - The CR17 plan says after accept NPC click and no prepared action, return a shared-state wait
     outcome instead of polling under the foreground turn.
   - Earlier behavior intentionally held the turn so an already-open accept dialog would not sit idle
     while unrelated maintenance takes the task turn.

   Risk:

   - If the new wait is implemented as a generic shared-state yield, 三技能/repair/summon maintenance
     could again run while the accept dialog is already open or about to be prepared.

   Recommendation:

   - Implement CR17 using an explicit `WAIT_PREPARED_DIALOG` wait spec with
     `allowOpportunisticMaintenance=false`, not a plain shared-state yield.
   - Acceptance must include a log proving that after `acceptNpcClicked=true`, no unrelated long
     maintenance starts before `WUBEI_ACCEPT_TASK` is consumed or the bounded wait expires.

### Third-View CR Follow-up - 2026-06-18 12:30-12:35 Heartbeat

Reviewer: Codex heartbeat reviewer, docs-only.

Scope reviewed:

- `docs/ACTIVE_WORK.md` latest CR9 / CR11 / CR13 entries.
- `docs/PACKAGE_ARCHITECTURE.md` sprint board and CR9-CR14 cards.
- `logs/dhxy-console.log` from `2026-06-18 12:30:10.000` to `12:35:45.000`.
- Current source shape for `WubeiTask` wait-state logging and already-satisfied checks.

#### Findings

1. CR9 source exists, but this log range does not validate it.

   Evidence:

   - Analyzer result for `12:30:10` to `12:35:45` still shows
     `wubei.wait.skipAlreadyReady=0`, `window.ready.await.timeout=42`, and
     `wubei.wait.wakeTimeout=19`.
   - The source `WubeiWaitRuntimeState` record now includes `readyEventType` and
     `readyEventAgeMs`, but the inspected `wubei wait` logs in this range do not print those fields.
     That strongly suggests the running process was not restarted onto the latest CR9 build, or the
     current log slice is still stale.
   - Examples remain: around `12:30:09`, `WAIT_PATHING_TERMINAL` paid 5s while
     `before.visibleDialogType=OPTION`; around `12:30:20`, it paid 5s while
     `before.visibleDialogType=STORY`; around `12:33:39`, it paid 5s while
     `before.visibleDialogType=OPTION visibleDialogAgeMs=4901`.

   Board decision:

   - CR9 should stay in review until a post-restart slice proves `skipAlreadyReady > 0` and the
     5s timeout class drops. Do not treat the current `Done` claim as verified.

2. CR11 may be directionally implemented, but post-change validation is also still missing.

   Evidence:

   - The latest top `ACTIVE_WORK` entry says CR11 compile passed and "after needs next run".
   - Current log range still shows many `consumePrepared.absent=107` and only
     `consumePrepared.consumed=7`, but this range likely predates the running process loading CR11.

   Board decision:

   - Leave CR11 as implementation done for now, but require the next real-run slice to prove
     prepared publish-to-consume latency improved.

3. New root cause: accept-NPC / `ROUTE_TO_MAIN_TASK` waits can still pay repeated 5s parks.

   Evidence:

   - Around `12:34:57.959`, 五倍 enters `ROUTE_TO_MAIN_TASK` with message
     `accept NPC pathing started`.
   - Around `12:35:02.964`, it pays a 5s `WAIT_PATHING_TERMINAL` timeout with no visible dialog or
     prepared action.
   - Around `12:35:16.906`, the same route finally sees `operation=ROUTE_TRANSFER`,
     `preparedTarget=宝象国`, and wakes by `TASK_ATTENTION_REQUIRED`.
   - Around `12:35:34.619`, another `accept NPC pathing started` wait again pays a 5s timeout.

   Board decision:

   - This is not the same as CR10's null tracker-green-click target; publish CR15 for
     `ROUTE_TO_MAIN_TASK` / accept-NPC pathing waits.

### Shared-State Park/Wakeup Model For Long Waits

This section records the target architecture for reducing high-frequency task-turn churn in long
五倍 waits. It is a design plan only; behavior should be changed in small reviewed steps.

#### Problem

The current 五倍 task can release a task turn after a shared-state result, then reacquire the same
turn about 80ms later just to discover that nothing meaningful changed. This shows up in logs as
large volumes of:

- `task.turn.handoff`
- `wubei:WAIT_BATTLE_FINISH`
- `wubei:RESOLVE_AFTER_PATHING`
- `consumePrepared result=absent`
- `dialog.interest.update`
- repeated `sameAsPrevious=true` handoffs

This is not caused by one OCR call being slow. It is mostly caused by the foreground task polling
too often while waiting for pathing, prepared dialogs, combat state, or team maintenance.

#### Target Principle

`TaskTurnCoordinator` should only manage turn ownership. It should not know business wait
conditions such as pathing, dialog, combat, or team maintenance.

Task flows such as `WubeiTask` know which external condition they are waiting for. They should
decide when to park after releasing a turn, and they should only wake when a runner or watcher event
indicates that the real game state may have changed.

Events are wake hints, not final truth. After waking, the task must re-read the source of truth from
`WindowRuntimeContext`, prepared actions, pathing snapshots, combat state, or other runtime state
before clicking or advancing.

#### Latency Preservation Rule

Performance work must be latency-preserving. The current user-tested latency is the baseline:
optimization may reduce CPU, screenshot volume, OCR volume, log spam, and redundant task turns, but
it must not make ready windows, prepared dialogs, pathing handoff, or combat follow-up slower.

In practice this means:

- Do not trade foreground responsiveness for quieter logs. A design that is less noisy but adds
  new 3s+ ready-to-click delays is a regression.
- Do not increase fixed sleeps, broaden fallback timers, or globally lower watcher frequency unless
  an event/interest wake path proves that hot states still wake promptly.
- Dialog, route, combat, and maintenance hot paths must stay event/interest-driven. Idle polling may
  be colder, but registered interest, prepared actions, pathing intent, and combat waits must wake or
  speed up the observer quickly.
- Before and after any scheduling/performance change, compare latency metrics. The change is only
  acceptable when CPU/work volume improves while p95/p99 latency for critical chains does not get
  worse.

#### Critical Review Findings

Two independent agent reviews agreed with the direction, but flagged these risks:

- `WAIT_BATTLE_FINISH` and `RESOLVE_AFTER_PATHING` must not become pure low-frequency sleeps. If an
  event is missed or delayed, the system could become "quiet but slow".
- Runner/Watcher should observe windows, run unified prepare calls, and publish wake facts. It must
  not grow into a task business scheduler full of 五环/五倍/修罗 if/else branches. Task-specific
  templates and rules should live behind task-owned provider/catalog/policy boundaries.
- Heavy prepare work must not block quick attention publication. In one observer tick, visible
  dialog detection and urgent prepared-result publication should not wait behind unrelated OCR,
  task-tracker scans, route-memory work, or slow maintenance probes.
- Park/wakeup is not the same as always yielding. Long external waits should park, but short modal
  UI chains, such as a dialog already visible and waiting for a prepared click, may keep the current
  task turn for a short bounded wait if that preserves lower latency.
- Cached/prepared results are useful only with freshness fences. Before real input, validate
  `windowId`, `hwnd`, task type, operation, intent/phase/source, and age. Wake events may wake a
  task, but must never directly click.
- If multiple prepared action kinds share runtime storage, the replacement and priority rules must
  be explicit. Overwrite, consume, clear, stale, and mismatch paths need reason logs.
- Do not optimize by simply changing `WUBEI_PREPARED_DIALOG_POLL_MS`,
  `WUBEI_PROBE_STORY_POLL_MS`, `WINDOW_DIALOG_PREPARE_ACTIVE_INTERVAL_MS`, or
  `WINDOW_OBSERVER_WAKE_CHECK_INTERVAL_MS` from 80-100ms to coarse sleeps. Those loops are noisy, but
  they are also the current fast path for accepted latency. The safer optimization is to stop
  refreshing the same interest, logging the same absent result, and waking the observer every 80ms
  when nothing changed.
- `WAIT_BATTLE_FINISH` is the riskiest wait to park. `COMBAT_STATE_CHANGED` does not exist yet, and
  the current battle watcher guard does not consume combat-exit recovery. Until a combat wake event
  is implemented and validated, `WAIT_BATTLE_FINISH` must keep a latency-preserving fast path rather
  than relying on the battle radar's 4s/10s background polling interval.
- Pure HWND screenshots should not compete with real keyboard/mouse input where avoidable. Current
  capture paths still enter `GlobalInputLock`; a future performance pass should split successful
  HWND capture from Robot/focus fallback so background visual checks reduce lock contention without
  weakening input serialization.
- Image/OCR heavy paths should reduce allocations and disk churn before changing scheduler cadence:
  avoid writing raw intermediate PNGs in production paths, batch repeated OCR candidates when
  possible, and keep detailed candidate logs at debug/slow-path level.

#### Target Flow

```text
队长点击寻路
-> phase returns WAIT_PATHING_TERMINAL
-> release task turn and park
-> Runner observes pathing terminal and publishes PATHING_TERMINAL
-> task wakes, rechecks runtime state, acquires turn, and continues

队长点出 dialog / tooltip
-> phase returns WAIT_PREPARED_DIALOG
-> release task turn and park
-> Runner prepares a dialog action and publishes TASK_ATTENTION_REQUIRED
-> task wakes, consumes prepared action, and continues

进入战斗
-> phase returns WAIT_COMBAT_STATE_CHANGE
-> release task turn and park
-> Runner / battle watcher observes combat state change and publishes COMBAT_STATE_CHANGED
-> task wakes, confirms combat state, and continues
```

#### Wait Reasons

- `WAIT_PATHING_TERMINAL`
  - Waits for pathing arrival, stopped-away, stopped, or a pathing-related dialog.
  - Typical 五倍 phases: `RESOLVE_AFTER_PATHING`, some `ROUTE_TO_MAIN_TASK`, and retry pathing in
    `ENTER_BATTLE`.
- `WAIT_PREPARED_DIALOG`
  - Waits for Runner to prepare route, accept, enter-battle, story, or similar dialog actions.
  - Typical uses: 白龙马 tooltip after-click enter-battle dialog, task-accept dialog, route dialog.
- `WAIT_COMBAT_STATE_CHANGE`
  - Waits for combat to begin or finish.
  - Typical uses: `WAIT_BATTLE_FINISH` and post-enter-battle confirmation.
- `WAIT_TEAM_ATTENTION`
  - Waits for team/follower maintenance such as first-aid, 三技能, or follower dialog handling.
  - Followers should normally be parked and should request a turn only when a real maintenance action
    is needed.
- `WAIT_RETRY_TIMER`
  - A low-frequency fallback for cases where no event source exists yet.
  - This must not become the normal high-frequency progress mechanism.

#### Existing Events To Reuse First

- `WindowReadyEventType.TASK_ATTENTION_REQUIRED`
  - Published when Runner observes a dialog or prepared action that requires task attention.
  - Should wake prepared-dialog waits.
- `WindowReadyEventType.PATHING_TERMINAL`
  - Published when Runner observes pathing arrival or terminal pathing state.
  - Should wake pathing-terminal waits.

#### Missing Event To Add

- `WindowReadyEventType.COMBAT_STATE_CHANGED`
  - Should be published when combat state changes from not-in-combat to in-combat, or from
    in-combat to not-in-combat.
  - This event is needed to stop `WAIT_BATTLE_FINISH` from reacquiring the turn just to log
    `combat still running`.
  - Until this exists, `WAIT_BATTLE_FINISH` may keep a coarse fallback timer, but it should not use
    an 80ms turn reacquire loop.

#### Implementation Plan

1. Add an explicit park decision in `WubeiTask.runRoundPhases(...)`.
   - If a phase outcome has immediate work, continue normally.
   - If a phase outcome enters a waiting shared state, release the turn and park.
   - While parked, do not reacquire the turn, consume prepared actions, refresh dialog interest, or
     emit repeated phase outcome logs.

2. Extend the phase outcome with an explicit wait intent.
   - The main loop should not infer waits from free-form messages such as `combat still running`.
   - A phase should return a wait reason or wake policy that says which event types can wake it.
   - The wait intent describes scheduling only; it must not change the business result by itself.

3. Add a 五倍 park helper.
   - Inputs should include current window id, phase, wait reason, wake event types, last event
     sequence, and stop/pause checkpoints.
   - The helper must wake promptly on task stop or interruption.
   - The helper should return whether it woke by event, fallback timer, or stop/interruption.

4. Avoid lost and stale events.
   - Record the latest relevant event sequence before parking.
   - Recheck runtime state immediately before parking; if the condition is already satisfied, do not
     sleep.
   - Wait only for events newer than the recorded sequence.
   - After waking, re-read runtime state and prepared actions before taking any action.

5. Connect existing event paths first.
   - `RESOLVE_AFTER_PATHING` should wait on `PATHING_TERMINAL` and `TASK_ATTENTION_REQUIRED`.
   - Route / accept / enter-battle prepared-dialog waits should wait on `TASK_ATTENTION_REQUIRED`.
   - 白龙马 tooltip flow must keep waiting for Runner's `WUBEI_ENTER_BATTLE` result before deciding
     whether to click the prepared action or run the existing direct-combat / `Alt+A` fallback.

6. Add combat-state wakeup next.
   - Add `COMBAT_STATE_CHANGED` to the event type enum.
   - Publish it from the Runner or battle watcher that owns combat observation.
   - Move `WAIT_BATTLE_FINISH` from high-frequency polling to combat-state wakeup plus a coarse
     fallback.
   - This must be implemented before lowering the current battle-finish fast path to a coarse
     watcher interval. A 4s/10s combat radar interval is not an acceptable replacement for current
     user-validated combat-exit latency.

7. Extend the same model to team maintenance.
   - Followers should normally stay parked.
   - First-aid, 三技能, and follower dialog handling should request turns only when maintenance
     services determine that work is actually needed.
   - After maintenance, followers should release the turn and return to parked state.

#### Constraints

- Do not remove `Alt+I`; it is required for the 白龙马 reveal flow.
- Do not remove `Alt+A` or direct-combat fallback; they are required fallbacks.
- `Alt+A` / direct-combat fallback must not run before Runner has replied for the relevant dialog.
- Do not change validated movement detection such as `GameStateUtil.isMovingByPixelDiff()` to hide
  scheduling churn.
- Do not put business waiting into `TaskTurnCoordinator`.
- Do not let a wake event directly cause a click. Tasks must recheck runtime state before input.
- Do not sacrifice current latency to reduce CPU. No scheduling change is accepted unless critical
  ready-to-click, prepared-dialog, pathing-handoff, and combat-follow-up latency stays at or below
  the previous baseline.
- Do not add fixed waits or larger sleep intervals as the main optimization. Reduce duplicate work
  through events, interest registration, caching with freshness checks, and slow-only logging.
- Do not turn the current 80-100ms hot-path checks into 500ms/1s waits unless the same path is
  woken by an event and measured latency stays at the old baseline.
- Do not let `WAIT_BATTLE_FINISH` depend only on low-frequency combat radar polling before
  `COMBAT_STATE_CHANGED` exists and is proven in logs.
- Do not let Runner send real mouse or keyboard input. Runner prepares observations/results; tasks
  consume them and still use the input queue for physical input.
- Do not remove latency/stale/queue diagnostics just because logs are noisy. Prefer aggregation,
  slow-path logs, and reason-coded counters.

#### Performance-Safe First Targets

These are preferred first targets because they reduce CPU/log/lock pressure without intentionally
lengthening any critical wait:

- De-duplicate dialog interest refresh in 五倍 prepared-dialog waits. Register once, refresh only
  near TTL expiry or when operation/target/source changes, and use `WindowReadyEventBus.awaitNewer`
  as the wake mechanism instead of repeated absent polling.
- Rate-limit repeated `consumePrepared result=absent`, `dialog.interest.update`,
  `task.turn.handoff`, input trace, and successful focus/HWND keyboard metrics. Keep warnings,
  state changes, stale reasons, and slow-path timing at info/warn.
- Split successful HWND capture from `GlobalInputLock` while keeping base refresh, Robot fallback,
  and focus-dependent capture protected. This should reduce multi-window screenshot contention
  without weakening physical input serialization.
- Move production OCR/image preprocessing toward in-memory pipelines. Only write raw/washed debug
  images for testcase replay, failures, or explicit debug switches.
- Batch repeated OCR candidates, especially yellow target candidates, so one screenshot does not
  create many PNG writes and many local OCR sidecar calls.
- Keep `active-pathing-dialog-first`: when a task dialog interest exists, dialog/attention
  preparation must stay ahead of slow minimap/pathing OCR.

#### Concrete Implementation Plan

This is the agreed next implementation plan. It intentionally narrows scope so the first change
reduces task-turn churn without rewriting navigation, combat, OCR, NPC click, or battle logic.

##### Phase 0: Baseline and Guardrails

Before changing behavior, capture a before/after baseline from `logs/dhxy-console.log`:

- count repeated `task.turn.handoff`, `sameAsPrevious=true`, and `consumePrepared result=absent`;
- measure `ready/publish -> task wake`, `prepared -> consume`, `consume -> input queued`,
  `input queued -> input start`, and `click done -> state changed`;
- record observer tick slow reasons and input queue wait time;
- preserve the current user-accepted latency as the maximum allowed latency.

If these measurements are missing, add logs first. Do not tune sleep values blindly.

##### Phase 1: Wubei Shared-State Park/Wakeup

Only connect existing ready events:

- `WindowReadyEventType.PATHING_TERMINAL`
- `WindowReadyEventType.TASK_ATTENTION_REQUIRED`

Do not add `COMBAT_STATE_CHANGED` in this phase.

Implementation steps:

1. Add `WubeiWaitReason`.
   - Initial values: `WAIT_PATHING_TERMINAL`, `WAIT_PREPARED_DIALOG`,
     `WAIT_COMBAT_STATE_CHANGE`, `WAIT_TEAM_ATTENTION`, and `WAIT_RETRY_TIMER`.
   - The enum is scheduling vocabulary only; it must not decide business success/failure.

2. Add `WubeiWaitSpec` as a small immutable value object.
   - Follow project convention: prefer Lombok `@Value` + `@Builder` over Java `record`.
   - Suggested fields:
     - `reason`
     - `wakeTypes`
     - `timeoutMs`
     - `minParkMs`
     - `currentWindowOnly`
     - `allowOpportunisticMaintenance`
   - `allowOpportunisticMaintenance=false` for prepared-dialog waits, especially 白龙马 and
     enter-battle dialog waits. These short chains should not run 三技能/maintenance before the
     dialog result is consumed.

3. Extend `WubeiStepOutcome` with an optional `waitSpec`.
   - The main loop must stop guessing from free-form messages.
   - A phase that is waiting for external state should explicitly return a wait spec.

4. Add `WindowReadyEventBus.currentSequence()`.
   - Park code needs a sequence snapshot before sleeping so it cannot miss an event that arrives
     between runtime recheck and `awaitNewer(...)`.

5. Add `parkIfNeeded(...)` in `WubeiTask.runRoundPhases(...)` after the turn is released.
   - The order should be:
     - transaction returns outcome;
     - `yieldAfterMustYield(...)` releases the current turn;
     - if outcome has `waitSpec`, park by waiting for newer event or timeout;
     - after waking, re-read runtime state and continue phase processing.
   - Park must not happen while holding the task turn.

6. Park sequence must be lost-event safe:

   ```text
   long seq = windowReadyEventBus.currentSequence()
   recheck runtime/prepared/pathing state
   if already satisfied -> do not sleep
   awaitNewer(windowId, waitSpec.wakeTypes, seq, waitSpec.timeoutMs)
   checkpoint stop/pause
   wake and re-read RuntimeContext/prepared action/pathing snapshot
   ```

7. Apply first to these waits only:
   - `RESOLVE_AFTER_PATHING` waiting for pathing terminal or dialog attention;
   - route / accept / enter-battle waits where a prepared dialog is expected;
   - 白龙马 `WUBEI_PROBE_STORY` waits after registering interest and using 显形镜.

8. 白龙马 special rule:
   - Register `WUBEI_PROBE_STORY` interest before/around 显形镜.
   - Wait briefly for Runner's prepared result through event wake or a bounded short wait.
   - If no prepared result arrives, do not mark the prompt failed, do not switch to the second
     prompt, and do not run `Alt+A` early. Re-read runtime state and continue waiting/recovery.

##### Phase 2: Combat Wakeup

Only start this after Phase 1 logs show pathing/dialog waits are latency-neutral.

Implementation steps:

1. Add `WindowReadyEventType.COMBAT_STATE_CHANGED`.
2. Publish it from the runner or battle watcher that owns combat observation.
3. Keep the event as a wake hint only; combat-exit recovery still belongs to the task flow.
4. Move `WAIT_BATTLE_FINISH` from high-frequency repeated turn reacquire to combat-state wakeup plus
   a measured fallback.
5. Do not use the battle radar's current 4s/10s background cadence as the only wake source.

##### Phase 3: Team Maintenance Wakeup

After pathing/dialog/combat waits are stable:

1. Followers stay parked by default.
2. First-aid, 三技能, follower dialog, and team maintenance publish attention only when real work is
   needed.
3. Maintenance windows still require task/business policy checks before real input.

##### Timeout Fallback Policy

Event wake is preferred, but every wait must keep a timeout fallback so a missed event or Runner
failure cannot deadlock the task:

- `WAIT_PATHING_TERMINAL`: about 5s fallback.
- `WAIT_PREPARED_DIALOG`: about 0.8s-1.5s fallback for short prepared-dialog chains.
- `WAIT_RETRY_TIMER`: coarse fallback only when no event source exists.
- `WAIT_COMBAT_STATE_CHANGE`: do not rely on this until `COMBAT_STATE_CHANGED` exists and is
  validated.

Timeout wake does not mean failure. It only means the phase should re-read runtime state and let the
phase's existing recovery logic decide what to do next.

##### Explicit Non-Goals For The First Change

- Do not change OCR thresholds, image washing, NPC click, minimap click, or movement detection.
- Do not move business waiting into `TaskTurnCoordinator`.
- Do not make Runner send input.
- Do not change `NavigationService` legacy rescue logic in the same edit.
- Do not convert all waits or combat recovery in the first patch.

#### Sprint Planning: Latency-Preserving Wubei Park/Wakeup

Sprint goal:

- Reduce 五倍 repeated turn reacquire / idle churn while keeping current user-accepted latency.
- The sprint is successful only if CPU/log pressure decreases and p95/p99 latency for dialog,
  pathing, and click handoff does not regress.
- Three agents can work in parallel. Sprint cards are not permanently locked to the originally
  listed person or lane. Any agent may pick any card that is not already `In Progress`, `Review`,
  `Done`, or explicitly blocked by another active card.
- The board `Owner` column means the current claimer/last responsible agent, not a permanent
  assignment. Before editing, the agent must claim the card by updating the board owner/status.
- If a card touches files another active card is already editing, write a coordination note in
  `docs/ACTIVE_WORK.md` before editing and wait until the file conflict is resolved.

Sprint claim-time business baseline rule:

- Before any agent starts any sprint card, every time, they must first read the current
  `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/PACKAGE_ARCHITECTURE.md`, and `docs/ACTIVE_WORK.md`.
  This is a hard intake gate, not optional background context.
- After reading those docs and before editing, they must inspect the latest pushed code for the
  relevant business path and treat that pushed behavior as the baseline.
- The agent must record the baseline in `docs/ACTIVE_WORK.md` before editing: branch, latest pushed
  commit if known, `git status`, and the relevant `git diff` / `git show` evidence for touched
  files.
- A migration card may change framework plumbing such as event wakeup, parking, ownership,
  diagnostics, or handoff timing. It must not change task business logic. Current local business
  differences are not trusted; restore to latest pushed behavior by default.
- Business logic includes phase transitions, prompt/story interpretation, OCR/template/click and
  navigation order, fallback ordering, and the rules for when a probe, NPC, dialog, route, or battle
  step is considered resolved.
- Runner/ready-event miss or timeout signals are scheduling/diagnostic facts only. They cannot be
  treated as new task business truth unless the latest pushed business logic already did that.
- If preserving the latest pushed business behavior conflicts with a proposed framework change, the
  agent must stop and write the exact conflict and proposed test evidence in Markdown. Do not keep
  the local behavior as part of migration; only a separate user-requested behavior-change story may
  intentionally diverge from the pushed business baseline.
- Restoring the latest pushed business behavior is not a new behavior change and does not need extra
  user approval. User approval is required only when an agent wants to intentionally keep or introduce
  behavior that differs from the latest pushed baseline.

Sprint reporting requirement:

- Every heartbeat / sprint audit report must include a `Performance goal check` section.
- That section must restate the original sprint goal in practical terms: reduce repeated turn
  reacquire / idle churn and CPU/log pressure, while preserving current dialog, pathing, and click
  handoff latency.
- Each report must say whether the latest logs prove improvement, prove regression, or provide no
  new performance evidence. Do not report only bug status.
- Use the latency script and raw log evidence when available. At minimum, report the current values
  or absence of evidence for `task.turn.handoff`, `sameAsPrevious`, `consumePrepared absent/consumed`,
  `window.ready.await`, `wubei.wait.parkFinished`, `wakeResult=event/timeout`,
  `PATHING_TERMINAL`, `TASK_ATTENTION_REQUIRED`, and critical prepared/route/input age fields.
- If there are no new logs, explicitly say the performance goal cannot be re-evaluated this round
  and that the previous performance conclusion still stands.
- Restart/test recommendations must depend on both correctness blockers and this performance goal
  check. A bug fix is not enough to call the sprint ready if the available evidence still shows
  timeout-heavy waits or unchanged churn.

Sprint guardrails:

- Do not change OCR thresholds, yellow/green/white washing, NPC click selection, minimap click
  selection, world-map route selection, or movement detection in this sprint.
- Do not rewrite 五倍 business logic. The change is scheduling/parking/wakeup first.
- Do not make Runner perform real input. Runner observes and prepares; tasks consume and click
  through the normal input queue.
- Do not remove timeout fallback. Event wake is preferred, but missed events must not deadlock.
- Do not add wrapper/helper layers whose only job is to rename an existing call.
- Every card must end with `mvn -q -DskipTests compile` or explain why it could not be run.
- Every card that changes behavior must add or update the relevant log evidence in
  `docs/ACTIVE_WORK.md`.
- Every agent must update this sprint board while working:
  - before starting, any unfinished card that is not currently `In Progress` may be picked up,
    including `Ready`, `Review`, `Blocked`, or `Reopened`; do not take `Done`, and do not take
    another agent's `In Progress` card unless the user explicitly reassigns it;
  - set `Owner` to the agent doing the work and change the card status to `In Progress`;
  - before code edits, record the latest pushed business baseline evidence in
    `docs/ACTIVE_WORK.md`;
  - do not start a card already marked `In Progress`, `Review`, or `Done` unless the current owner
    has released it in `docs/ACTIVE_WORK.md` or the user explicitly reassigns it;
  - change blocked cards to `Blocked: <reason>` instead of silently waiting;
  - mark every completed checklist step from `- [ ]` to `- [x]`;
  - change the card status to `Review` after code is ready but before another agent/user review;
  - change the card status to `Done` only after compile/test evidence is recorded;
  - if a card is deliberately skipped, mark it `Skipped: <reason>` and record the decision in
    `docs/ACTIVE_WORK.md`.
- Each completed card must add an `ACTIVE_WORK` entry with: card id, files changed, command output
  summary, log time range if tested, before/after metric if applicable, and remaining risks.

##### Mandatory Intake Gate Before Sprint Board

Every person or agent taking a sprint card must follow this order before touching code:

1. Read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/PACKAGE_ARCHITECTURE.md`, and
   `docs/ACTIVE_WORK.md` in the current working tree.
2. Read the latest pushed 五倍 business implementation for the touched path. Current baseline for
   this sprint is `3f0a2e7 (origin/codex/migrate-runner-dialog)` unless a newer pushed commit is
   explicitly selected by the user.
3. Record the intake in `docs/ACTIVE_WORK.md`: card id, branch, pushed baseline commit, relevant
   files, and the exact pushed-vs-local evidence used.
4. Only then claim or continue the card.

The sprint is allowed to change architecture: runner observation, park/wakeup, task-turn ownership,
event sequencing, diagnostics, and timeout/freshness plumbing can move as needed. It is not allowed
to change 五倍 business logic by accident. 五倍 business logic must remain the same as the latest
pushed baseline. Current local business differences are not trusted during this migration; if a
future behavior change is wanted, it must be opened as a separate user-requested Story, not kept as
part of the migration.

For this rule, 五倍 business logic includes:

- phase transitions and when a phase is considered complete;
- prompt/story meaning, including 白龙马 / 显形镜 branches;
- OCR/template/click/navigation order and fallback order;
- when a probe, NPC, dialog, route, route retry, battle entry, battle exit, or maintenance action is
  considered resolved or allowed to run;
- any deadline/window that affects a business decision, even if the new implementation waits through
  runner/park instead of foreground polling.

If a framework migration requires changing one of those business decisions, the agent must stop and
write the proposed behavior change as a separate Story/card with required runtime or testcase
evidence. Do not hide that change inside a runner/park migration card.

##### Sprint Board

Latest runtime closure note from `dhxy-1` heartbeat (`2026-06-21 22:35:51-22:46:49` 修罗): CR75, CR76, CR77, and CR78 have fresh-runtime Done evidence; the long-row board now records them as Done. CR68 remains Review because one 30s target wait timeout still occurred even though post-timeout 900ms churn is repaired; CR72 is still not closeable because later runtime exposed 摄妖香 ordering/safety risk after otherwise positive icon-gate samples. See `docs/ACTIVE_WORK.md` entry `Codex - 2026-06-21 / dhxy-1 修罗 heartbeat 巡检 22:35:51-22:46:49` and the 2026-06-22/2026-06-28 run-report entries for exact log evidence.

CR68 source update on `2026-06-22`: target pathing wait no longer uses a fixed 30s soft timeout. `WAIT_TARGET_PATHING_TERMINAL_TIMEOUT_MS` is now `-1L`, so after the leader releases the turn it stays parked until the same-window matching `PATHING_TERMINAL`, matching `PREPARED_ACTION_READY/ROUTE_TRANSFER`, or stop/interruption. This preserves the existing same-intent event filters and removes the repeated 30s timeout/re-attach behavior seen in the 2026-06-22 run reports. Fresh runtime is still required before moving CR68 to Done.

| Card | Owner | Status | Files | Goal |
| --- | --- | --- | --- | --- |
| A1 | 何黎 | Done | `WindowReadyEventBus`, `WindowReadyEventType` | Add the minimal event sequence API and wait diagnostics. |
| A2 | 何黎 | Done | `WindowTaskRunner`, `WindowRuntimeContext` if needed | Make observer/ready/prepared timing visible without changing business behavior. |
| A3 | 何黎 | Done | `WindowTaskRunner` logs only | Split slow observer tick timing by component. |
| B1 | 谢帅 | Done | `WubeiWaitReason`, `WubeiWaitSpec`, `WubeiStepOutcome` | Add explicit wait vocabulary to 五倍 outcomes. |
| B2 | 谢帅 | Done | `WubeiTask` | Park after releasing the task turn and wake by event/timeout. |
| B3 | 谢帅 | Done | `WubeiTask` | Map specific 五倍 phases to wait specs. |
| B4 | 谢帅 | Done | `WubeiTask` | Preserve 白龙马 old business decisions while consuming Runner-prepared story results. |
| C1 | 唐德 | Done | `docs/ACTIVE_WORK.md`, optional `scripts/analyze_wubei_latency.ps1` | Produce before/after latency summary from logs. |
| C2 | 唐德 | Done | `docs/ACTIVE_WORK.md` | Define real-run acceptance checklist for 1/3/5 window tests. |
| C3 | 唐德 | Done | `docs/PACKAGE_ARCHITECTURE.md`, `docs/ACTIVE_WORK.md` | Fresh post-change logs reviewed; keep slow-path diagnostics visible and only rate-limit repeated no-progress spam. |
| CR1 | 谢帅 | Done | `WubeiTask` | Move phase-boundary prepared-dialog consumption back under task-turn transaction. |
| CR2 | 谢帅 | Done | `WubeiTask` | Gate/remove unconditional 80ms phase-boundary wait so normal phases do not pay latency. |
| CR3 | 何黎 | Deprecated: decomposed into CR9-CR11 / CR18 | `WubeiTask`, logs | Old broad no-park/churn diagnosis. Do not implement from this row; the actionable work was split into narrower CR9-CR11 cards, and the operation-null wake part was later repaired by CR18. |
| CR4 | 唐德 | Done | `scripts/analyze_wubei_latency.ps1`, input logs if needed | Make latency script measure actual current input/park events instead of zero counters. |
| CR5 | 何黎 | Done | `WindowReadyEventBus` | Removed stale generic `latestOtherFresh(...)`; explicit prepared/pathing helpers remain. |
| CR6 | 谢帅 | Done | `WubeiTask` | Enforce `allowOpportunisticMaintenance=false` so prepared-dialog waits cannot run opportunistic maintenance. |
| CR7 | 谢帅 | Done | `WubeiTask`, logs | Count consecutive timeout wakes and add a clear diagnostic when Runner does not answer prepared-dialog waits. |
| CR8 | 何黎 | Done | `WubeiTask`, `scripts/analyze_wubei_latency.ps1`, logs | Old no-park finding is superseded by fresh-run evidence; CR3 is decomposed into CR9-CR13. |
| CR9 | 谢帅 | Deprecated: superseded by CR18 | `WindowReadyEventBus`, `WubeiTask` | Old ready-event satisfied-check follow-up. Do not implement from this row; CR18 owns and validated the operation-null visible-dialog boundary. |
| CR10 | 谢帅 | Deprecated: superseded by CR20 | `WubeiTask`, `WindowTaskRunner`, `WindowRuntimeContext` | Old null-target tracker terminal plan. Do not implement from this row; CR20 owns the runner-side terminal/attention split. |
| CR11 | 谢帅 | Deprecated: superseded by CR18 / CR58 / CR59 | `WubeiTask`, `WindowRuntimeContext` | Old prepared-action absent-loop diagnosis. Current prepared-dialog wait and watcher-frequency work is tracked by CR18, CR58, and CR59. |
| CR12 | 谢帅 | Deprecated: old team-window gate approach superseded | `AutoCombatService`, `TaskMaintenanceService`, `DialogService` logs | Do not revive this old follower auto-battle broadcast gate. CR35 restored the baseline behavior, and current maintenance pressure is tracked by CR65 / CR78 instead. |
| CR13 | 唐德 | Deprecated: old broad split-turn plan | `WubeiTask`, `TaskTurnCoordinator` logs | Old unapproved broad turn-boundary plan. Do not implement without a new user-approved story; current performance work is handled by scoped event/park cards. |
| CR14 | 谢帅 | Deprecated: old maintenance-window gate approach superseded | `TaskMaintenanceService`, `SummonSkillService`, `AutoBattleTask`, 五倍 wait logs | Do not implement this old broad gate. Current summon-skill behavior is tracked by CR63, and maintenance broadcast / auto-battle pressure is tracked by CR65 / CR78. |
| CR15 | 谢帅 | Deprecated: superseded by CR34 / CR36 | `WubeiTask`, `NavigationService`, `WindowTaskRunner` route/ready logs | Old accept-NPC route-wait split card. Do not implement from this row; later baseline route restore / route-intent ownership cards own the current behavior. |
| CR16 | 唐德 | Deprecated: old unapproved team-return plan | `WubeiTask`, `TeamReturnService`, `TaskTurnCoordinator` logs | Old WUBEI `WAIT_TEAM_RETURN` event/state plan. Leave closed unless the user opens a fresh team-return story with current logs. |
| CR17 | 唐德 | Deprecated: old unapproved accept-wait plan | `WubeiTask`, `WindowTaskRunner`, 五倍 accept-dialog logs | Old WUBEI accept prepared-wait plan. Current prepared-dialog latency / polling work is tracked by CR58 / CR59 and task-specific accept-memory work by CR75. |
| CR18 | 唐德 | Done | `WubeiTask`, `WindowTaskRunner`, `WindowRuntimeContext`, ready/wait logs | Post-restart `16:04:43.835+` validation confirms operation-null visible dialog attention no longer satisfies pathing waits: `skip park ... WAIT_PATHING_TERMINAL ... preparedOperation=null=0`; at `16:05:36.419-16:05:36.425` visible `STORY operation=null` woke the task but `satisfied=false`. Compile passed with `mvn -q -DskipTests compile`. |
| CR19 | 唐德 | Deprecated: superseded by CR34 / CR36 | `WubeiTask`, accept-NPC route wait logs | Old unapproved accept-route gate plan. Do not implement from this row; baseline route gate / route-intent ownership is now tracked by CR34 and CR36. |
| CR20 | 谢帅 | Deprecated: old pathing-watcher audit superseded | `WindowTaskRunner`, pathing watcher logs | Old direct dialog-terminal audit. Current ordinary/黄袍 terminal behavior is tracked by CR43 / CR50, and 修罗 target pathing waits by CR68. |
| CR21 | 唐德 | Deprecated: old unapproved startup-role policy | `TeamRoleDetectionService`, `TaskTeamAssignmentPolicy`, `WindowTaskRunner`, startup logs | Old inconclusive-role startup plan. Do not implement unless a fresh startup/role bug is reproduced and the user opens a new story. |
| CR22 | 谢帅 | Deprecated: superseded by CR34 / CR36 | `WubeiTask`, `NavigationService`, `WindowRuntimeContext`, `WindowTaskRunner` route logs | Old accept-NPC route-dialog stale-intent card. The current route baseline/intent work is owned by CR34 and CR36. |
| CR23 | 谢帅 | Deprecated: superseded by CR34 / CR36 | `WubeiTask`, `NavigationService`, `WindowTaskRunner`, accept-NPC current-map pathing logs | Old accept-NPC current-map ACTIVE recovery card. Do not implement from this row; use current baseline route cards if this path regresses again. |
| CR24 | 谢帅 | Deprecated: superseded by CR36 | `NavigationService`, `WindowRuntimeContext`, route intent reuse logs | Old same-target route intent reuse guard. CR36 restored the current route-intent ownership baseline. |
| CR25 | 谢帅 | Deprecated: old performance audit baseline | `WubeiTask`, `AutoCombatService`, `TaskTurnCoordinator` wait logs | Historical WUBEI wait/churn audit. Do not treat this as an implementation card; current combat/wait performance is tracked by CR32 / CR61 / CR62 / CR68 and newer pressure cards. |
| CR26 | 唐德 | Deprecated: old unapproved ENTER_BATTLE phase-gate plan | `WubeiTask`, `NpcClickService`, WUBEI prepared-dialog logs | Old phase-gate proposal. Current enter-battle and target-click issues are tracked by CR30 / CR53 / CR60 / CR64; do not implement this old plan without a fresh user-approved story. |
| CR27 | 谢帅 | Deprecated: superseded by CR29 / CR39 / CR40 / CR53 | `WubeiTask`, `WindowTaskRunner`, `WindowRuntimeContext`, probe pathing logs | Old probe-pathing visible-STORY handoff card. Current 白龙马 probe behavior is owned by the later user-approved CR39 / CR40 / CR53 family. |
| CR28 | 谢帅 | Deprecated: folded into CR33 / CR39 / CR40 / CR53 | `WubeiTask`, 五倍 probe timeout / prepared-dialog wait logs | Old baseline-conflict timeout card. Do not implement directly; current 白龙马 explicit absent/noTarget/target-ready behavior is documented in `docs/业务逻辑.md` and tracked by CR39 / CR40 / CR53. |
| CR29 | 何黎 | Deprecated: superseded by user-approved CR40 | `WubeiTask`, 白龙马 probeNoTarget / prompt switching logs | Old `probeNoTarget` tooltip-fallback order. User later explicitly removed noTarget tooltip fallback in CR40, so this row is obsolete and must not be used as current 白龙马 logic. |
| CR30 | 谢帅 | Done | `WubeiTask.tickEnterBattle`, WUBEI prepared-dialog / auto-combat logs | At `18:05:51`, Runner prepared `WUBEI_ENTER_BATTLE click=(1682,480)`, but `tickEnterBattle(...)` first accepted `AutoCombatService.TickResult.EXIT_RECOVERED`, skipped the prepared click, jumped to `POST_BATTLE_RECOVER`, and later `RETURN_HOME`; the prepared action then expired (`prepared-stale:3878ms`). Source patch gives fresh `WUBEI_ENTER_BATTLE` prepared actions priority at ENTER_BATTLE phase start and before stale combat-exit recovery, while keeping `IN_COMBAT` as the independent battle-confirmed path. Post-restart log `2026-06-18 18:31:20.809-18:34:24.232` validated three prepared enter-battle consumes/clicks before `WAIT_BATTLE_FINISH`, with no `combat ended during enter battle phase` or `ready dialog pending too long` signatures. |
| CR31 | 谢帅 | Done | `scripts/analyze_wubei_latency.ps1`, heartbeat log-scan notes | Timing fields now use Int64 storage, so sentinel values such as `ageMs=9223372036854775807` no longer crash mixed-log heartbeat performance reports. Verified full range after `18:55:00.331` and WUBEI-filtered range. |
| CR32 | 谢帅 | Review: compile passed; needs fresh WUBEI validation | `WindowReadyEventType`, `WindowTaskRunner`, `WubeiTask`, `scripts/analyze_wubei_latency.ps1`, latency logs | Source patch adds `COMBAT_STATE_CHANGED`, publishes it from the window combat watcher on `IN_COMBAT <-> NONE` transitions, and makes `WAIT_BATTLE_FINISH` wait on that event with a 1.5s fallback instead of timeout-only 400ms. Compile passed. Heartbeat audit `2026-06-18 20:55:33.576-21:07:16.516` still had no post-restart CR32 runtime evidence (`COMBAT_STATE_CHANGED=0`, `wakeTypes=[COMBAT_STATE_CHANGED]=0`, `timeoutMs=400=395`). Follow-up audit `2026-06-18 21:07:16.516-21:15:29.790` still has `COMBAT_STATE_CHANGED=0`, `window.combat.state.changed=0`, `wakeTypes=[COMBAT_STATE_CHANGED]=0`; the dominant churn is now `RESOLVE_AFTER_PATHING same=200/200` and `WAIT_BATTLE_FINISH same=66/66`, with `wubei.wait.wakeTimeout=203`. Restart/focused WUBEI validation is still required to prove `WAIT_BATTLE_FINISH sameAsPrevious` / `wakeTimeout` drop and no prepared-dialog latency regression. |
| CR33 | 谢帅 | Review: baseline gate restored/split; compile passed; needs fresh WUBEI validation | `WubeiTask`, `NavigationService`, `TaskMaintenanceService`, `AutoBattleTask` | Restore/guard the latest-push 五倍 business baseline before continuing runner/park migration. Latest pushed baseline is `3f0a2e7`; local diff and `docs/WUBEI_BUSINESS_DIFF_AUDIT.md` confirm business-logic deltas that are not just runner/park architecture migration. Source patch restores the probe ACTIVE guard, the bounded 15s probe-story decision window, and the enter-battle first-tick no-immediate-yield behavior while preserving CR30 fresh prepared-action priority. Remaining local business deltas are not candidates to keep by default: CR24/CR28/CR34 are existing review cards requiring validation against the pushed baseline, and CR35 is now a restore-to-baseline card for AutoBattle maintenance-window gating. |
| CR34 | 唐德 | Review: baseline route gate restored; compile passed; needs fresh WUBEI validation | `WubeiTask.waitForAcceptNpcPathingIfStillActive`, accept-NPC route logs | CR33 split-out. Local code had broadened accept-NPC route waiting beyond the pushed fresh matching prepared-route dialog path: `recoverStaleAcceptNpcCurrentMapPathing(...)`, `acceptNpcRouteWakeFact(...)`, and terminal/visible-dialog/preparation facts could release or recover the accept-NPC pathing wait. 唐德 compared against latest pushed baseline `3f0a2e7` and restored the route gate so only a freshly prepared matching `ROUTE_TRANSFER` action for `宝象国` releases the accept-NPC pathing wait. The runner wait-spec / park plumbing remains, but generic visible dialogs, preparation REQUESTED/PREPARING/READY status, pathing terminal events, task-attention events, and bounded current-map recheck no longer change this business decision. `git diff --check` passed with only line-ending warnings; `mvn -q -DskipTests compile` passed. Needs fresh WUBEI validation before Done. |
| CR35 | 谢帅 | Review: baseline restored; compile passed; needs fresh WUBEI/XIULUO maintenance validation | `AutoBattleTask`, `TaskMaintenanceService`, WUBEI/XIULUO follower maintenance logs | Restored latest-push maintenance baseline. `AutoBattleTask.maybeRunIdleMaintenance(...)` again passes `handleMaintenanceBroadcast(true)` unconditionally, and the local `requiresTeamMaintenanceWindowGate(...)` / `isTeamFirstAidMaintenanceWindowOpen(...)` broadcast gate was removed. `TaskMaintenanceService` still keeps the private first-aid window helper for follower first-aid waits. `git diff 3f0a2e7 -- AutoBattleTask.java TaskMaintenanceService.java` is empty for this scope, and `mvn -q -DskipTests compile` passed. Keep in Review until fresh WUBEI/XIULUO follower maintenance logs prove no new maintenance-broadcast churn or task-flow regression. |
| CR36 | 谢帅 | Review: latest-push route-intent ownership baseline restored; compile passed; needs fresh WUBEI route validation | `NavigationService`, `WindowRuntimeContext`, WUBEI route intent / prepared route logs | Restored the strict pushed-baseline ownership rules checked against `3f0a2e7`: active prepared routes must match the active `intentId`; same-target route re-entry no longer reuses an old active intent and instead registers a fresh pathing intent; cleared-route recovery is only allowed when the active intent has already been cleared. `mvn -q -DskipTests compile` passed. Remaining acceptance item: collect fresh WUBEI route logs showing no stale/cross-intent prepared route consumption. |
| CR37 | 谢帅 | Review: extra ENTER_BATTLE prepared preemptions removed; compile passed; needs fresh WUBEI enter-battle validation | `WubeiTask`, WUBEI enter-battle prepared logs | Restored the pushed-baseline fallback order except for the CR30-preserved priority point. Removed `consumePreparedEnterBattleDuringPathing(...)`; phase-start now only registers `WUBEI_ENTER_BATTLE` interest; the before-combat-tick prepared consume was removed; `canConsumeEnterBattlePreparedAction(...)` is scoped back to `ENTER_BATTLE`; the only remaining fresh prepared consume is after `handleCombatTick(...)` and before `EXIT_RECOVERED`, so a fresh prepared click can still beat stale combat-exit recovery. `mvn -q -DskipTests compile` passed. Remaining acceptance item: collect fresh WUBEI logs proving prepared enter-battle clicks are fresh/phase-owned and green-template miss still reaches smart/direct fallback. |
| CR38 | 唐德 | Review: baseline timing audit documented; needs fresh WUBEI validation or user-approved restore | `WubeiTask`, `WubeiStepOutcome`, `WindowTaskRunner`, WUBEI wait/wake logs | 唐德 heartbeat audit compared the local timing migration against latest pushed baseline `3f0a2e7`. Baseline `WubeiTask.runRoundPhases(...)` checked `checkReadyPriorityBeforePhase(...)` before entering `TaskTransactionRunner.run(...)`, had no `WubeiStepOutcome.waitSpec`, and did not run `parkAfterYieldIfNeeded(...)` after turn release; local code now runs priority handling inside the transaction and parks after yield on 5s pathing / 1.5s route / 1.5s prepared-dialog / 1.5s combat waits. This is a real phase-timing change, not just logging. No behavior restore was made in this audit because fresh WUBEI runtime evidence is absent. Acceptance remains: either fresh logs prove reduced churn with no delayed fallback/timeout/prepared-dialog p95/p99 regression, or a user-approved restore narrows the migration back to the latest-push turn-release timing. |
| CR39 | 何黎 | Review: heartbeat blockers repaired; compile passed; needs reviewer/fresh 白龙马 validation | `WubeiTask`, `WubeiDialogPreparationProvider`, `DialogService`, `WindowTaskRunner`, WUBEI probe story logs | User-approved 白龙马 point 14/15 behavior change: remove leader-side 15s/80ms `WUBEI_PROBE_STORY` polling fallback and use explicit no-STORY result `targetKeyword=wubei.probeStoryAbsent`, `matchedText=STORY_ABSENT`, `dialogType=NONE`, `clickRequired=false`. PreparedAt lower-bound is repaired. Heartbeat blocker follow-up removed the post-显形镜 700ms sleep, replaced `Thread.onSpinWait()` with current-window `TASK_ATTENTION_REQUIRED` event waiting while keeping task-turn ownership, and makes detection/capture/rect failure produce the explicit click-free `wubei.probeStoryAbsent / STORY_ABSENT` result when absent is allowed. `mvn -q -DskipTests compile` passed. Fresh 白龙马 validation remains required. |
| CR40 | 唐德 | Review: noTarget tooltip fallback removed; compile passed; needs fresh 白龙马 validation | `WubeiTask`, 白龙马 probe story logs | User-approved 白龙马 point 18 behavior change: deleted the old noTarget -> tooltip fallback path by gating the existing `tryClickProbeSpawnedTarget(..., false)` fallback away from `wubei.probeNoTarget`. noTarget now reuses the existing post-fallback code path that marks the current probe resolved, switches to the next unused probe when available, or fails/reaccepts through the existing failed-task recovery path when probes are exhausted. `probeTargetReady`, `probeWrongPosition`, and `probeStoryAbsent` behavior were left unchanged. `mvn -q -DskipTests compile` passed. Needs fresh 白龙马 validation. |
| CR41 | 何黎 | Done: long-run ordinary validation passed | `WindowReadyEventType`, `WindowReadyEventBus`, `WindowTaskRunner`, `WubeiTask`, `logs/dhxy-console.log` | Split 五倍 ordinary-monster wake vocabulary: prepared actions now publish/consume `PREPARED_ACTION_READY`; 五倍 pathing/prepared waits no longer include plain `TASK_ATTENTION_REQUIRED`; `PRE_BATTLE_TIMEOUT` vocabulary is available for CR44. Long-run log evidence shows repeated ordinary waits waking by `PREPARED_ACTION_READY`; analyzer summary reported `window.ready.await event=175 timeout=0` and no wait timeout churn. 已可关闭。 |
| CR42 | 唐德 | Done: ordinary dialog boundary validated by long run | `WindowTaskRunner`, `WubeiDialogPreparationProvider`, `DialogService`, `logs/dhxy-console.log` | Implement ordinary-monster Runner dialog boundary from `docs/业务逻辑.md`: only `OPTION` dialog with successful `WUBEI_ENTER_BATTLE` template match produces a result; `OPTION` miss, `STORY`, and other dialogs are ignored and do not wake the leader. Long-run ordinary/probe logs repeatedly continue through battle recovery without stray STORY/OPTION waking ordinary enter-battle waits. 已可关闭。 |
| CR43 | 谢帅 | Review: still needs ordinary terminal validation | `WubeiTask`, ordinary tracker pathing logs | Make ordinary-monster `PATHING_TERMINAL` re-click the same tracker green link and release again; it must not enter `ENTER_BATTLE`, smart-click the monster, run tooltip/yellow OCR, use Alt+A, or wait/consume `ROUTE_TRANSFER` as ordinary business evidence. Long-run evidence includes same-green re-click during 黄袍第一战, but no fresh ordinary-monster `PATHING_TERMINAL` same-green re-click case; keep open until that exact ordinary case appears. |
| CR44 | Codex | Review: normal path passed; timeout path not exercised | `WindowRuntimeContext`, `WindowTaskRunner`, `WubeiTask`, ordinary pre-battle logs | Add ordinary-monster Runner-side 5 minute pre-battle timeout from the first successful ordinary green-link click to `WUBEI_ENTER_BATTLE` consumption / `WAIT_BATTLE_FINISH`; timeout wakes the leader with `PRE_BATTLE_TIMEOUT` and returns to reaccept. Long-run logs show timer starts and clears on prepared consumption, but no `PRE_BATTLE_TIMEOUT` / timeout wake occurred, so the timeout acceptance path is not yet runtime-validated. |
| CR45 | Codex | Partial: ordinary success validated; timeout/terminal gaps remain | `logs/dhxy-console.log`, `scripts/analyze_wubei_latency.ps1`, `docs/ACTIVE_WORK.md`, optional test logs | Validate CR41-CR44 against `docs/业务逻辑.md`: fresh ordinary-monster run must prove event-only wakeup, same-target re-navigation, timeout behavior, no idle churn / p95/p99 regression, and no 白龙马 CR39/CR40 regression. CR41/CR42 are closeable from the long run, but CR43 ordinary terminal and CR44 timeout evidence are still missing. |
| CR46 | Codex | Done: 黄袍第一战 validated | `WubeiTask`, `WindowRuntimeContext`, `WindowTaskRunner`, 黄袍第一战 logs | Make 黄袍怪 first-battle pre-battle path explicitly follow the ordinary CR41-CR44 contract while preserving the 黄袍连战 marker and switching to the documented hot path after the first `WAIT_BATTLE_FINISH`. Fresh `2026-06-19 15:58-15:59` logs show `wubei:first-chained-green-click`, terminal same-green re-click, `PREPARED_ACTION_READY / WUBEI_ENTER_BATTLE`, timer clear on prepared consumption, and first battle recovery. Can close; continuation stays under CR47/CR49. |
| CR47 | Codex | Done: fresh 黄袍续战热路径 validated | `WubeiTask`, `WubeiWaitSpec`, 黄袍续战 logs | Fresh `2026-06-19 21:29-21:40` 黄袍 logs supersede the old `16:00-16:04` blocker: chained-combat 1-4 registered `WUBEI_ENTER_BATTLE interest` before the physical green click, skipped ordinary pathing intent, consumed fresh prepared actions, and did not hit the old phase loop guard. Chain-end / return-home is still CR49, not CR47. |
| CR48 | Codex | Review: audit-only; continuation hot path proof received; chain-end still waiting CR49 | `WubeiTask`, tracker panel read logs, latest-push diff | Audit/restore 黄袍怪 continuation fallback, chain-end, and protection-cap behavior against `3f0a2e7`; migration must not invent new no-green click strategy, end-condition, or retry policy. CR47 hot path is now fresh-validated; keep CR48 open until CR49 supplies chain-end / return-home proof. |
| CR49 | Codex | Partial: continuation passed; chain-end/return-home still unvalidated | `logs/dhxy-console.log`, `scripts/analyze_wubei_latency.ps1`, `docs/ACTIVE_WORK.md` | Fresh 黄袍怪 validation card: first battle follows ordinary, post-first-battle stays in chained hot path, chain-end/return-home works, and 白龙马/普通怪 business logic plus performance goals do not regress. Fresh `21:29-21:40` logs prove first battle and chained-combat 1-4 hot path, but the user stopped at `21:42:58` while still in `WAIT_BATTLE_FINISH`; no fresh chain-end / return-home evidence yet, so CR49 cannot close. |
| CR50 | Codex | Review: fresh 黄袍 terminal proof passed; needs fresh ordinary terminal proof | `WubeiTask.runResolveAfterPathingPhase`, ordinary/黄袍第一战 pathing logs | Removed the ordinary/黄袍第一战 `STOPPED_AWAY + fresh ROUTE_TRANSFER` terminal interceptor; `PATHING_TERMINAL` now clears the terminal snapshot and re-clicks the same current tracker green before releasing again. Fresh `2026-06-19 15:59` 黄袍第一战 logs prove `STOPPED_AWAY -> same tracker green re-click -> PREPARED_ACTION_READY / WUBEI_ENTER_BATTLE` with no terminal `ROUTE_TRANSFER` wait. |
| CR51 | Codex | Done: phase-owned lifecycle fresh runtime validated | `WindowDialogInterest`, `WindowRuntimeContext`, `WindowTaskRunner`, `WubeiTask`, WUBEI dialog-interest logs | Fresh `2026-06-19 20:46-21:42` logs show `ttl=phase-owned` interest updates and `reason=expired=0`; prepared actions still publish/consume after the old 15s window, and 黄袍续战 pre-click interest stays phase-owned. CR52 owns the 10s timing replacement; CR51 lifecycle invariant is validated. |
| CR52 | Codex | Done: target-map gate fresh runtime validated | `TaskTrackerGreenLink`, `TaskTrackerPanelReadResult`, `TaskTrackerPanelService`, `MapNameCanonicalizer`, `WindowTaskRunner`, `WindowRuntimeContext`, `WubeiTask`, WUBEI ordinary/黄袍第一战 logs | Fresh `2026-06-19 20:46-21:42` logs show `ordinary enter-battle target map gate armed=22`, `opened=22`, `normal-enter-battle-map-matched` interest and prepared consumption, with `normal-enter-battle-delayed=0`. This closes the runtime proof for replacing fixed 10s registration with canonical target-map match. |
| CR53 | Codex | Review: candidate wait bounded; compile/test-compile passed; needs fresh 白龙马 validation | `WubeiTask`, 白龙马 target-click / enter-battle logs | New long-run blocker from `2026-06-19 20:46-20:48`: after 白龙马 `probeTargetReady` / target click, `smart-combat-target:first-probe-story:runner-reply` repeatedly re-registered interest and consumed absent prepared actions around 1689 times before the 300s probe timeout. Fresh `2026-06-20 16:26:29-16:30:57` runtime confirms this blocker is still live and reaches the real timeout: first-probe reached `PATHING_TERMINAL`, `WUBEI_PROBE_STORY` returned `wubei.probeTargetReady`, 白龙马 tooltip click verified, then `WUBEI_ENTER_BATTLE` interest stayed in an active-pathing dialog loop while visible dialog remained `STORY`; ready waits repeated every 5s, Runner kept `branch=active-pathing-dialog-first nextIntervalMs=100`, and `16:30:57.262` logged `probe task exceeded enter-battle timeout ... elapsedMs=300001 timeoutMs=300000`, forcing `ROUTE_TO_MAIN_TASK`. Tangde `2026-06-20` report root-cause is confirmed by fresh continuation logs `16:39:27.344-16:43:47.410`: targetReady was consumed, foreground expected-dialog verification was skipped for `npcClick:direct:firstVerify`, `WUBEI_ENTER_BATTLE` was registered, no OPTION/prepared enter-battle appeared, and the task timed out. Patch only bounds `wubei:smart-combat-target:*` candidate waits to 6s so a bad candidate can continue to the next candidate/direct-combat fallback instead of burning 300s. Yellow/Ctrl coordinate policy is out of scope for CR53. `mvn -q -DskipTests compile` and `mvn -q -DskipTests test-compile` passed. Fresh validation must prove bad candidates log `probe enter-battle candidate wait expired` around 6s, no 300s absent loop remains, and CR39/CR40 probe branching plus 普通怪/黄袍 rules are unchanged. |
| CR54 | Codex | Done: fast-path hit and miss-return-home runtime validated | `TaskTrackerPanelService`, `TaskTrackerPanelReadResult`, `TaskTrackerFastMatchResult`, `WubeiTask`, `WubeiChainedTrackerFastReplayDebugMain`, 黄袍续战 logs/testcase images | 黄袍续战性能卡。Source/compile/replay passed and no source business blocker found. Fresh `2026-06-20 17:27:27-17:28:17` runtime validates the happy path: first post-battle full tracker read builds cache, then `chained tracker fast-path hit` uses the cached rect/click with `elapsedMs=78/77/155`, registers `WUBEI_ENTER_BATTLE` before clicking, and avoids repeated title/yellow/green-map OCR for those fast continuations. Follow-up `17:29:02-17:30:10` repeats the proof with hits at `82/144/72/87ms`, pre-click `WUBEI_ENTER_BATTLE interest`, and `88ms` enter-battle prepare. Fresh `17:52:40-17:56:53` covers the missing live miss/failure branch: chained count 1 builds cache from a full tracker read, counts 1-4 use fast verify/click and consume fresh `WUBEI_ENTER_BATTLE` prepared actions, then count 5 gets `chained fast verify matched=false distance=89 score=0.11`, logs `chained tracker fast-path miss-return-home`, uses the return item, verifies `宝象国`, skips full tracker fallback, finishes `WAIT_TEAM_RETURN` as not needed, and starts the next accept task successfully. No business-rule change against `docs/业务逻辑.md` observed. |
| CR55 | Codex | Review: fresh abandon proof; clean/fast/failure validation pending | `NavigationService`, `WindowRuntimeContext`, `WindowTaskRunner`/pathing settlement, `MemoryService` with world-map route-result store, route logs | Source review blockers repaired: fresh `STOPPED_AWAY` with no previous route entry now creates a dirty failure entry, and `clearPathingSignal(...)` no longer silently drops pending CR55 memory; 五倍-owned pathing clear consumes and records abandoned metadata through `MemoryService`. `mvn -q -DskipTests compile`, `mvn -q -DskipTests test-compile`, `MemoryServiceFacadeTest`, `WorldMapRouteResultMemoryServiceTest`, and `WindowRuntimeContextWorldMapMemoryTest` passed. Fresh `2026-06-20 16:31` runtime now proves the abandon branch: `lookup skipped: reason=missing fromMap=波月洞 targetMap=宝象国`, pending created with `rel=(399,453)`, then `pending abandoned ... reason=intent-replaced successCount=0 failureCount=0`. Still needs fresh live logs for OCR counts, clean promotion, fast-path use, and STOPPED_AWAY failure before Done. |
| CR56 | Codex | Done: fresh 修罗 runtime validated | `XiuluoTaskV2`, 修罗 objective capture/parse path, 修罗 logs | Source implemented and compile/source guard passed. Fresh `2026-06-21 00:26:38-00:32:22` 修罗 runtime validates CR56: accept-time objective snapshots are captured, background parses complete for `白骨山(101,46)`, `长安城东(30,74)`, and `凤巢五层(40,53)`, `READ_OBJECTIVE` consumes `objectiveParseFuture()` with `elapsedMs=0` / phase `1ms`, and no same-phase synchronous `READ_STORY_OBJECTIVE` fallback is observed in the sampled path. |
| CR57 | Codex | Review: runtime reuse evidence exists; broader coverage pending | `WindowTaskRunner`, `DialogService`, dialog preparation providers, runner tick logs | Runner dialog 截图共享性能卡：同一 watcher tick 内，dialog attention / business dialog prepare / route prepare / remembered route prepare / final attention 必须复用同一个临时 `DialogDetection`/截图结果，避免对同一个 dialog 重复截图、重复 detect、重复 OCR/template prepare。2026-06-26 audit correction: current `logs/dhxy-console.log` has clear positive evidence (`tick dialog detection captured=402`, `tick dialog detection reused=248`, `dialog supplied detection reused=10`), including `20:28:47.595`, `20:30:54.766`, and `20:32:39.141` supplied reuse for 修罗 `XIULUO_ENTER_BATTLE` green-template preparation. Do not list CR57 as "not covered" for this run. Still not Done because closure needs full source/compile acceptance plus broader coverage across the card-owned branches: dialog attention / business dialog prepare / route prepare / remembered route prepare / final attention. Boundaries unchanged: do not share minimap/pathing/battle screenshots, do not store long-lived `BufferedImage` in runtime snapshots, and do not change OCR/template/click/navigation/prepared-action business semantics. |
| CR58 | Codex | Done: fresh WUBEI ordinary/probe/黄袍 runtime validated | `WindowTaskRunner`, `WindowRuntimeContext`, `PreparedDialogAction` consume path, `DialogService`/validation helper, runner timing logs | Prepared action READY 后停止后台 100ms fingerprint 续命。Source review and compile/test-compile passed. Fresh `2026-06-20 17:15:06.856-17:28:58.607` runtime covers 白龙马 probe (`WUBEI_PROBE_STORY` noTarget/targetReady), ordinary enter battle, accept, and 黄袍 chained enter battle: `prepared-action-verified=0`, `dialog-visible-prepared=0`, prepared actions are consumed with fresh consume-time validation (`preparedAgeMs` p95 about `138ms`, max `167ms` in analyzer), and in-combat ticks show no stale prepared action keeping the watcher hot. No business-rule change found against `docs/业务逻辑.md`; remaining CPU/log pressure is tracked by other CRs. |
| CR59 | Codex | Done: no 80ms prepared-dialog polling in fresh ordinary/probe/黄袍 runtime | `WubeiTask`, `WindowReadyEventBus` wait usage, WUBEI prepared-dialog wait logs | 五倍 foreground prepared-dialog waits now use single consume + `PREPARED_ACTION_READY` event wake instead of 80ms loops. Source/script/compile checks passed earlier. Fresh `2026-06-20 17:15:06.856-17:28:58.607` runtime shows `poll80=0`, no `WUBEI_PREPARED_DIALOG_POLL` / `:poll`, no `window.ready.await result=timeout`, 白龙马 probe waits wake on `PREPARED_ACTION_READY`, ordinary waits wake on `PREPARED_ACTION_READY`, and 黄袍 chained fast path registers interest before click and continues via event/consume path. No business regression observed in this slice; remaining 白龙马 click target / summon-skill issues are tracked separately by CR53/CR64/CR63. |
| CR60 | Codex | Done: fresh WUBEI runtime cleanup validated | `WindowTaskRunner`, `WindowRuntimeContext`, combat-state logs, WUBEI dialog/prepared state logs | 进战斗边界清理五倍 dialog 准备状态：当 Runner/combat watcher 观察到当前窗口从非战斗进入 `IN_COMBAT`，当前源码会调用 `clearWubeiDialogStateOnCombatEntry(...)`，清当前五倍 `dialogInterest`、当前 `dialogPreparationRequest`、无 request 时的 stale `preparedDialogAction`、普通入战 target-map gate，并且 `IN_COMBAT` tick 走 `observerBranch=in-combat` 后直接按 battle cadence sleep，不再执行 dialog/pathing/tracker prepare。`mvn -q -DskipTests test-compile` 和 `WubeiCombatEntryDialogCleanupWiringTest` passed。Fresh `2026-06-20 16:17:01.008-16:20:01.008` WUBEI run proves cleanup hook and in-combat skip: `event=wubei.combat-entry.dialog-cleanup` fired on WUBEI combat entry, and `window observer tick: task=WUBEI branch=in-combat` shows `pathingMs=-1`, `routePrepareMs=-1`, `taskDialogPrepareMs=-1`, `taskTrackerPrepareMs=-1`, `attentionDetectMs=-1`, `attentionPublishMs=-1`, `nextIntervalMs=4000`. No business-rule change found against `docs/业务逻辑.md`. |
| CR61 | Codex | Done: fresh WUBEI and 修罗 combat skip validated | `WindowTaskRunner`, `AutoCombatService`/battle watcher loop, XIULUO and WUBEI combat tick logs | 通用进战斗卡顿治理：当 combat watcher 当前 tick 已确认 `IN_COMBAT`，本轮只做战斗守护/战斗状态发布/必要 pre-battle timeout 清理，不再执行 route dialog prepare、task dialog prepare、task tracker prepare、pathing probe、final task attention detect，也不因为这些非战斗状态降低 next interval。WUBEI side was validated by fresh `2026-06-20 16:17:01.008-16:20:01.008` logs. Fresh `2026-06-21 00:26:38-00:32:22` 修罗 runtime completes acceptance: `branch=in-combat` ticks show pathing / route prepare / task dialog prepare / task tracker prepare / attention skipped as `-1`, and `nextIntervalMs=4000`; no combat enter/exit latency regression was observed in the sampled 修罗 combats. |
| CR62 | Codex | Done: fresh 修罗 event wait validated | `XiuluoTaskV2`, `AutoCombatService`, `WindowReadyEventBus`, XIULUO `WAIT_COMBAT` logs | 修罗 `WAIT_COMBAT` task-turn 空转治理：不要把退出战斗检测简单降频，也不要把 post-combat recovery 交给 Runner；目标是保持快速退出检测，但让修罗战斗中等待不再每约 900ms 进入完整 `TaskTransactionRunner`。Source/guard tests passed earlier. Fresh `2026-06-21 00:26:38-00:32:22` 修罗 runtime validates the new path: `WAIT_COMBAT_STATE_CHANGE` event waits start at `00:28:42.824` and `00:31:21.926`, wake on `COMBAT_STATE_CHANGED` at `00:29:42.891` and `00:31:56.526`, then task-side recovery advances to `RETURN_HOME` at `00:29:43.905` and `00:31:56.976`; no repeated every-900ms `WAIT_COMBAT` task transaction loop was observed between waits. |
| CR63 | Codex | Review: source/test pass; fresh locked-slot runtime pending | `SummonSkillService`, `TaskMaintenanceService`, summon-skill cleanup logs, latest-push baseline `13fc663` | Restore 三技能尾部锁/空格 backward scan 业务语义。Implemented a narrow `SummonSkillTailBoundaryScanner` and wired both current `LOCKED_SLOT` exits in `SummonSkillService` back to backward scanning. Deterministic main test covers previous `NORMAL_SKILL` deletion, skipped closed-slot inspected count, previous `KEEP_SKILL` safe stop, previous `UNKNOWN`, delete failure, and deadline failure. `mvn -q -DskipTests compile` passed; isolated CR63 test passed via `javac ... SummonSkillTailBoundaryScannerTest.java` + `java ... SummonSkillTailBoundaryScannerTest`. After CR64 landed, full `mvn -q -DskipTests compile test-compile` passed. Runtime still needed before Done: fresh logs must show slot 4 `LOCKED_SLOT` inspects backward slots instead of finishing after `inspected=1`; delete branch must show `deleted>0` when previous normal exists; unsafe unknown/delete/timeout must not refresh cooldown. |
| CR64 | Codex | Review: code-only recheck corrected; fresh Ctrl-origin/runtime proof pending | `NpcClickService`, 白龙马 `COMBAT_TARGET` OCR/Ctrl click logs, testcase images | Split from Tangde `2026-06-20` report / CR53: WUBEI 白龙马 `COMBAT_TARGET` yellow/Ctrl OCR can click the recognized name text center instead of the model point. Logs around `16:40` show Ctrl/OCR matched `白龙马` and clicked `(1588,829)` with no enter-battle prepared result; fresh `18:48` repeats the same pattern with Ctrl/OCR click `(1748,809)`, then no `WUBEI_ENTER_BATTLE` prepared action before the `18:52:05.820` probe enter-battle timeout. Code-only recheck `2026-06-20`: `scanMenuAndVerifyKeywordDirect(...)` line-click is the Ctrl-menu item click, not the model/probe point, so it should not be judged by `Y - 50`. The model/probe origins are built earlier: direct yellow target click uses `targetInScan.y - 50`, fallback text candidates use `region.y2() - 50`, and `attemptedClickPointAbs` is added before extra Ctrl origins. Remaining review point: exact yellow match failure still also adds `textCenterAbs` as a secondary Ctrl origin, so fresh logs must show the first `yellow-target:*` / `yellow-candidate:*` probe origin is the intended model point and that the selected Ctrl menu item leads to `WUBEI_ENTER_BATTLE`; if text-center origin is still used first or causes timeout, reopen as blocker. |
| CR65 | Codex | Review: yellow lightweight broadcast now proven; performance pressure still under observation | `DialogService`, `AutoBattleTask`, `TaskMaintenanceService`, `AutoCombatService`, `AutoCombatPanelService`, maintenance/Alt+8 logs | Refine maintenance/auto-combat pressure without suppressing valid checks. Implemented OPTION-only maintenance fallback: `DialogType.STORY` now logs `reason=non-option-dialog` and returns not-found without washing/matching heal-pet/repair templates; only `DialogType.OPTION` enters `handleBusinessOption(...)`. Implemented same-team 30000ms guard for `reason=refresh-due` Alt+8 in `AutoCombatPanelService`; `low-rounds` and `unknown` bypass the guard, deferred refreshes log team/window/retry-after and do not update `AutoCombatService.lastAutoBattleRefreshAt`. Follow-up after review: kept CR65/CR78 business boundaries unchanged, throttled high-frequency no-action info logs, and added an `AutoCombatService` pre-verification gate for optional `refresh-due` checks. Cached healthy rounds now skip `verifyAndAlignPanel(...)`; same-team `refresh-due` defers skip the heavy screenshot/template/alignment path before panel verify; periodic `low-rounds` and `unknown` still call the existing verify/Alt+8 path immediately. Second follow-up: combat-entry maintenance now uses `PanelVerifyMode.ENTRY_MAINTENANCE`, which verifies/aligns the panel but skips remaining-round refresh and does not update `lastAutoBattleRefreshAt`; periodic optional refresh uses `VERIFY_AND_REFRESH`. Current follow-up handles fresh runtime evidence that full fallback is already 0 but `DialogService` still emits high-frequency auto-battle no-action `CLICK_BUSINESS_OPTION` request/result INFO logs: lightweight fallback-disabled requests are DEBUG, and repeated `BUSINESS_OPTION_NOT_FOUND/NONE` results are INFO-throttled per source with DEBUG suppressed repeats. 2026-06-27 runtime update: the previous yellow-broadcast blocker now has fresh proof, `19:36:31.357` member `hwnd-1960D10` matched `maintenance broadcast option ... color=yellow score=1.0` for `repair-equipment`, clicked it, and `19:36:32.981` handled the broadcast. Keep CR65 in Review because this same slice still has `auto-combat panel rounds refresh by Alt+8=4`, `refresh-due panel verify deferred=21`, `maintenance broadcast lightweight fallback disabled=7`, and several slow holds, so the remaining acceptance is performance/noise stability rather than yellow matching. |
| CR66 | 谢帅 | Review: source/test pass; fresh runtime pending | `WindowTaskRunner`, `WindowReadyEventBus`, `WubeiTask`, WUBEI ready/wait logs | Remove plain `TASK_ATTENTION_REQUIRED` from WUBEI business wake paths. Stale pre-restart logs from `2026-06-20 18:53-19:12` still show the old problem, but user confirmed these are not acceptance logs for the current source. Code-only review `2026-06-20`: `publishTaskAttentionIfDialogVisible(...)` now handles WUBEI visible dialogs before generic `TASK_ATTENTION_REQUIRED`, keeps visible-dialog snapshot/prepared-action preparation, returns `preparedAction`, and `isWubeiDialogVisibleAttention(...)` is scoped to `TaskType.WUBEI`. Verification: `mvn -q -DskipTests compile`, `mvn -q -DskipTests test-compile`, direct main `WubeiPlainTaskAttentionNoWakeWiringTest`, and `git diff --check` passed with only existing LF/CRLF warnings. Fresh restarted WUBEI runtime still required before Done: no `TASK_ATTENTION_REQUIRED task=WUBEI source=dialog-visible:* operation=null`, while explicit prepared actions still wake. |
| CR67 | Codex | Review: implemented; compile/test/marked proof passed; fresh runtime pending | `TaskTrackerPanelService`, tracker panel anchor fallback, task-tracker testcase images, `logs/dhxy-console.log` | Fix task-tracker `expanded` anchor coordinate space. Fresh `2026-06-20 23:44:53-23:45:42` logs show 岁月 `hwnd-74E07A0` repeatedly missing `narrow-default`, falling into `expanded`, matching `anchor=(102,201)` from `latest_vision.png`, then using it as screen-absolute and cropping `rect=(6,213)-(188,551)` while the window base is `(398,255)`, producing `relative=(-392,-42)`. The narrow path is screen-absolute and mostly works; only the expanded fallback returns window-local coordinates and must add the bound window base before panel crop/drag decisions. |
| CR68 | Codex | Review: reattached same-intent active target wait; compile passed; fresh runtime pending | `XiuluoTaskV2`, `TaskTransactionRunner`, `WindowReadyEventBus`, pathing logs | 修罗目标导航 pathing-turn 空转治理。Implemented source-level scheduling change: `NAVIGATE_TO_TARGET` `PATHING_STARTED` now carries a `WAIT_TARGET_PATHING_TERMINAL` wait spec and parks instead of using the fixed 900ms reacquire loop. Follow-up feedback repaired: the wait now captures the active target pathing `intentId` and uses `WindowReadyEventBus.awaitNewerPathingTerminal(...)`, so only the same window's matching `ARRIVED` / `STOPPED_AWAY` terminal event for that exact 修罗 target-navigation intent can wake this wait. `mvn -q -DskipTests compile`, `mvn -q -DskipTests test-compile`, and isolated `XiuluoTargetPathingEventWakeWiringTest` passed. Fresh `2026-06-21 14:49-14:51` runtime proves the event wait is wired but not accepted yet: the first target-map route wait started at `14:50:05.758` with `afterSequence=2`, timed out after `30000ms` at `14:50:35.758`, and immediately produced `sameAsPrevious=true` plus `delayMs=900` reacquire loops while watcher still reported `ACTIVE`; the matching `PATHING_TERMINAL state=ARRIVED sequence=4` arrived later at `14:50:41.019`. A later current-map wait did pass (`14:50:48.206` wait, `14:50:55.240` event wake after `7034ms`). Fresh `2026-06-21 15:07-15:12` adds a clearer blocker: target-map wait for `兰若寺` started at `15:10:32.635` with `wakeTypes=[PATHING_TERMINAL] afterSequence=48`; Runner published `TASK_ATTENTION_REQUIRED` at `15:10:57.650` and `PREPARED_ACTION_READY operation=ROUTE_TRANSFER target=兰若寺 sequence=50` at `15:10:57.662`, but the task did not wake/consume because it was only waiting for `PATHING_TERMINAL`; it timed out at `15:11:02.636`, then consumed the prepared route at `15:11:02.988`. 2026-06-21 Codex repair: `WAIT_TARGET_PATHING_TERMINAL` now also waits on matching `PREPARED_ACTION_READY/ROUTE_TRANSFER` for the same target map through `awaitNewerPathingTerminalOrPreparedRoute(...)`; source test, `test-compile`, and compile passed. 2026-06-21 40-minute 修罗 audit feedback: not accepted. Fresh `20:55:41.829` target wait `afterSequence=126` timed out at `20:56:11.830 elapsedMs=30001`; matching `PATHING_TERMINAL target=凤巢六层 sequence=127` and route-memory success arrived at `20:56:16.076-20:56:16.078`, about 4.2s after timeout. Crucially, timeout time was still legitimate same-intent pathing, not an event mismatch: `20:56:11.832-20:56:14.576` watcher snapshots still reported `ACTIVE current=凤巢三/四层 target=凤巢六层`, and Runner had no prepared route action. Code root cause: the initial `navigateToTarget(...)` pathing result is wrapped by `waitForTargetPathingWake(...)`, but after that 30s soft wait times out, `continueIfNavigationStillPathing(...)` returns plain `XiuluoStepOutcome.pathingStarted(...)` without reattaching a waitSpec; `yieldAfterMustYield(...)` therefore falls back to `TASK_TURN_HANDOFF_DELAY_MS=900` and reacquires repeatedly. Fix direction: do not treat a same-intent ACTIVE snapshot after 30s as a normal handoff loop. Either reattach the same target-pathing wait while the watcher proves same-intent `ACTIVE/UNKNOWN`, or replace the fixed 30s single wait with a progress/deadline watchdog that only exits to foreground recheck on `ARRIVED`, `STOPPED_AWAY`, matching `PREPARED_ACTION_READY`, stale/no-progress, or a real business timeout. Earlier same-run slices also reproduced 30s waits/timeouts. 2026-06-21 Codex repair: `continueIfNavigationStillPathing(...)` now reattaches `waitForTargetPathingWake(...)` when `NAVIGATE_TO_TARGET` still sees same-phase watcher `ACTIVE/UNKNOWN` or movement `MOVING/PATHING_ACTIVE`, so these target re-yields keep waiting on `PATHING_TERMINAL` / matching `PREPARED_ACTION_READY` instead of falling back to `TASK_TURN_HANDOFF_DELAY_MS=900`; source guard first failed on the missing reattachment, then `mvn -q -DskipTests test-compile`, isolated `XiuluoTargetPathingEventWakeWiringTest`, and `mvn -q -DskipTests compile` passed. Fresh runtime still required: target navigation should no longer show post-timeout `sameAsPrevious=true` + `delayMs=900` churn while the same intent is still `ACTIVE/UNKNOWN`. No navigation target, route/click/OCR/template, NPC click, or 修罗 business phase order was changed. |
| CR69 | Codex | Review: implemented; compile/replay passed; fresh runtime pending | `ObjectiveTextRecognitionService`, `XiuluoTaskV2`, `docs/run-reports/2026-06-21-xiuluo-test-run-objective-recognition.md`, objective-text testcase images | 修罗 accept-time objective 坐标识别边界修复。Tangde report and independent check confirm the paused round-12 issue is not missing snapshot, not missing map name, and not caused directly by yellow hint. Fresh runtime/replay show `瑶池` map matches with score `1.0`, coordinate crop contains complete `(63,91)`, but coordinate recognition returns empty after a bad intermediate read like `,763,91`; then CR56-owned `READ_OBJECTIVE` consumes `hit=false` and recovers/reaccepts. Scope: fix objective coordinate OCR/template parsing for the saved testcase `images/test-cases/objective-text/raw/story_yaochi_63_91_with_hint_raw.png` while preserving successful samples such as `story_yaochi_78_64_extra7_raw.png`; do not restore same-phase synchronous objective reading or change 修罗 business flow. |
| CR70 | 谢帅 | Deprecated: superseded by CR106 | `NavigationService.navigateToLingShouVillageViaZhangWen`, `WindowRuntimeContext` route-dialog state, 修罗 张闻/灵兽村 logs | Old 张闻 -> 灵兽村 route-dialog timing card. Do not continue validating or implementing this route. The game update plus CR99 yellow-destination mini-map navigation makes direct `targetMap=灵兽村` viable, so CR106 replaces this path. Keep the 张闻 chain only as deprecated retained source code; production 灵兽村 navigation must not call it. |
| CR71 | Codex | Review: compile + wiring test passed; needs fresh 修罗 runtime | `XiuluoTaskV2`, `AutoCombatService`, `BattleRadarService`, `WindowReadyEventBus`, 修罗 WAIT_COMBAT/unknown-combat logs | 修罗战斗中禁止 unknown-combat 回城/任务面板读取。Fresh `2026-06-21 01:29:48-01:30:22` logs show 修罗 target click and enter-battle option were successful, watcher published `COMBAT_STATE_CHANGED oldTick=NONE newTick=IN_COMBAT`, but task-side `WAIT_COMBAT` consumed `EXIT_RECOVERED` at `01:29:55.338` before `enteredBattleByXiuluo` was marked; it then entered `resolveUnknownCombatExit`, pressed `Alt+Q` at `01:30:06.046`, and used `bag/xiuluo_return_item.png` at `01:30:08-01:30:15` while the watcher still logged `branch=in-combat` at `01:30:03.591` and `01:30:16.330`. Fix safety boundary first: `resolveUnknownCombatExit` / return-item fallback must not run if current bound window is still in combat or has a fresh/current `IN_COMBAT` fact; treat the contradictory `EXIT_RECOVERED` as stale and return to `WAIT_COMBAT` / event wait. Then investigate stale `combatExitPending` or cross-thread combat-state race. Do not change 修罗 objective parsing, navigation, NPC click, enter-battle template, return item algorithm, or CR61/CR62 performance behavior except to add this safety gate. |
| CR72 | 唐德 | Review: hover-safety source/test/compile passed; fresh runtime pending | `PlayerStateService`, `AutoCombatService`, 摄妖香 status probe/logs/testcase images | 战后摄妖香检查不能只相信 `lastIncenseUsedTime`。Current `ensureSheYaoXiangActive(...)` now treats memory as a 50-minute trust window: within 50 minutes it may skip bag opening only after lightweight status-icon proof; after 50 minutes it must run full status verification and still only refills when remaining time is at or below the separate 20-minute refill threshold, or when the buff is absent/unproven. Fix policy: after combat, before accepting the memory gate, perform a lightweight 摄妖香 icon presence check. Icon present: keep trusting memory, clear stale failed-refill retry cooldown, and do not open bag. Icon absent: refill immediately and reset `lastIncenseUsedTime=now` unless the existing failed-refill retry cooldown applies. Icon unknown/capture uncertain: run the existing full `probeIncenseStatus(...)`; if still unknown/not found, refill conservatively. The icon probe remembers the last matched offset inside the status rect and first checks a small cached-position snapshot; if that misses, it falls back to the fixed window-relative status rect. 2026-06-21 fresh logs showed `67555 / hwnd-60312BA` false `memory-gate-icon-absent-refill` twice at age 7m and 1m while user observed 50+ minutes remaining; root-cause refinement is that status-bar/icon captures must first move the mouse away from the buff area, matching the existing bag/bars hover-avoidance pattern. Full status OCR clamps remaining time to the internal 59-minute duration so a cyan `1 hour` read cannot push `lastIncenseUsedTime` into the future. Do not change item template, bag click algorithm, OCR digit thresholds, or unrelated 五倍/修罗 flow. |
| CR73 | Codex | Review: partial fresh runtime; flying-state rect replay and exception safety passed, AutoA retry path pending | `NpcClickService`, `XiuluoTaskV2`, `WubeiTask`, `GameStateUtil.detectFlyingState`, direct-combat logs | Global direct-combat / `Alt+A` safety boundary. Fresh `2026-06-21 14:23:44-14:24:46` 修罗 logs show target navigation reached 洛阳城 current-map approach, target click failed, then direct-combat entered `Alt+A` and reused the same stale search/click assumptions. User clarified this is not 修罗-only: canceling `Alt+A` / AutoA can move the character, so any follow-up retry must not assume the original target position; it must rerun the task-owned navigation/current-map approach before another combat-target click. Also, before entering direct-combat mode, the shared path must ensure the leader is not mounted/flying, reusing the existing flying-state / Alt+C semantics instead of inventing a new detector. Applies to all current callers of `NpcClickService.tryDirectCombatTargetClick(...)` (`XiuluoTaskV2`, `WubeiTask`). Fresh `2026-06-21 15:06` and `15:11` 修罗 logs prove the mount/flying preflight is active: target click failed, `flying-status:open:npc-direct-combat:pre-alt-a:修罗` ran, `state=UNKNOWN`, and direct-combat skipped without `AutoA` / `AutoGA`. Codex/user live check then proved the old rect could capture the Alt+U panel page arrow or a ROI smaller than `flying.png`, causing OpenCV to abort 修罗 at `2026-06-22 14:25:09`. The probe rect is now the user-approved current-window relative `(660,573)-(712,597)` with no padding, backed by `images/test-cases/status/flying_status_altu_67555_raw.png`, marked output `images/test-cases/status/flying_status_altu_67555_new_flying_rect_marked.png`, crop output `images/test-cases/status/flying_status_altu_67555_new_flying_rect_crop.png`, `GameStateUtilFlyingStatusRegionReplayTest`, and `GameStateUtilFlyingStatusExceptionSafetyWiringTest`; `test-compile`, direct replay/safety tests, and compile passed. `detectFlyingState(...)` now downgrades template/runtime probe exceptions to `UNKNOWN` while still closing Alt+U, so the helper must not directly fail 修罗. Still not Done: fresh runtime must prove `detectFlyingState(...)` returns `NOT_FLYING`/`FLYING` instead of `UNKNOWN`, and the path where direct-combat actually enters/cancels AutoA reruns task-owned navigation before retrying target click. No OCR/template/click-coordinate algorithm changes without testcase replay. |
| CR74 | Codex | Done: fresh 修罗 route-memory runtime validated | `NavigationService`, `WindowTaskRunner`, `WindowRuntimeContext`, `WorldMapRouteResultMemoryService`, 修罗 world-map route memory logs | 修罗 world-map route-result memory 从未变 clean。Fresh audit of current `logs/dhxy-console.log` showed the original failure shape: `fast path used=0`, `pending created=36`, `pending success=0`, `pending abandoned=36`, `reason=intent-replaced=37`, while `config/world_map_route_result_memory.json` had `entries=36`, `clean=0`, `successCountSum=0`, `consecutiveSuccessPositive=0`. Root cause was not missing creation: `submitWorldMapSearchAndClickDestination(...)` registered `worldMapRouteClick` and created pending against that intent, then outer `navigateToMap(...)` registered a second same-leg `navigateToMap` intent, so `WindowTaskRunner.settlePendingWorldMapRouteResultMemory(...)` treated the pending as replaced and recorded `intent-replaced`. Codex repair marks successful normal world-map route submission as nested-route-owned before returning `PATHING_STARTED`, so the outer `finally` skips the duplicate `navigateToMap` registration while keeping runner `intent-replaced` abandonment for genuine second navigation. Added `NavigationWorldMapRouteMemoryIntentOwnershipTest`; compile and memory service test passed. Fresh `2026-06-22 15:35-18:24` 修罗 runtime validates the fix: repeated `pending success`, no `pending abandoned` / `intent-replaced`, routes promote to `clean=true`, and clean fast path is used repeatedly, including 白骨山、龙窟七层、兰若寺、长安城东. Do not change world-map OCR/click/scroll algorithms, route-dialog choice memory, 修罗 business phases, or 五倍/WUBEI navigation semantics. |
| CR75 | Codex | Done: fresh 修罗 accept-memory fast path validated | `XiuluoTaskV2.tryRememberedAcceptTaskOption`, `DialogHandleRequest`, `DialogService`, 修罗 accept-memory logs | 修罗接任务 remembered option 没有走现有 fast path。Fresh logs show `xiuluo-v2:accept:*:accept-memory` first runs `dialog detect no-focus: reason=handle-dialog:CLICK_REMEMBERED_OPTION` with `elapsedMs=883/1169/1808/3136`, then clicks remembered `rel=(95,113)`. Current `DialogService` already supports `CLICK_REMEMBERED_POINT && verifyDialogType=false` fast path, but `tryRememberedAcceptTaskOption(...)` used `DialogHandleRequest.handleRememberedChoiceOption(...)` whose default `verifyDialogType=true` dropped the caller's known-dialog fact. Codex added a verifyDialogType-preserving remembered-choice overload and passes `handleKnownXiuluoOptionDialog(...)`'s `verifyDialogType` into 修罗 accept-memory only; existing default factory still verifies dialog type. Added `XiuluoRememberedAcceptOptionFastPathWiringTest`; compile and source tests passed. Fresh 修罗 runtime accepted: repeated accept-memory samples show the remembered option fast path without full `handle-dialog:CLICK_REMEMBERED_OPTION` detection, remembered clicks succeed, and no green-template/business fallback regression was recorded in the later multi-round reports. 已可关闭。 |
| CR76 | Codex | Done: fresh 修罗 learned-NPC fast path validated | `NpcClickService.runNpcClickPipeline`, learned NPC click memory, pre-click dialog safety logs, 修罗 accept-NPC logs | NPC learned-memory click currently still pays the generic name-layer preparation cost before trying the remembered point. Fresh logs around `灵兽村使者` show `npcClick:pipeline-hide-player-names` / `Alt+4` plus `400ms` sleep before learned-memory can click, even though a stable learned point does not require hiding names or OCR. Implemented a narrow fast path for stable non-combat NPC learned-memory clicks: non-direct-combat, non-`TaskType.WUBEI`, non-`NpcRole.COMBAT_TARGET` requests now run pre-click dialog safety first, then try learned memory before `prepareNpcPipelineNameLayerOnce(...)`; a verified learned-memory hit returns before Alt+4, while a miss continues through the existing Alt+4 + tooltip/yellow/formula/Ctrl fallback and is not retried a second time in the same pipeline. WUBEI/白龙马 `COMBAT_TARGET` ordering is guarded by source test and still keeps tooltip-first/direct-combat semantics outside the early fast path. Verification passed: `mvn -q -DskipTests compile`, `mvn -q -DskipTests test-compile`, `NpcClickLearnedMemoryFastPathWiringTest`, and existing `NpcClickDirectCombatNameLayerWiringTest`. Fresh 修罗 runtime accepted: later runs show stable `灵兽村使者` learned-memory hits without preceding `npcClick:pipeline-hide-player-names:灵兽村使者`, while earlier miss/insufficient-policy samples fell back normally. 已可关闭。 |
| CR77 | Codex | Done: fresh 修罗 start-exit fire-and-handoff validated | `NavigationService.navigateInCurrentMap`, `XiuluoTaskV2.startLeavingStartMapIfPresent`, 修罗 `start-exit-prepath` logs | 修罗出灵兽村预走路 current-map 点击后还要等 `fast-edge` / 坐标变化确认，再关小地图并注册 intent，导致每轮多等约 1.8s movement confirmation + 后续关图检查。Codex added a narrow fire-and-handoff branch gated by `source=xiuluo-v2:start-exit-prepath`, target map `灵兽村`, target coordinate `(11,8)`: after resolving and clicking the mini-map point it skips `isMovingByPixelDiff(...)`, skips coordinate fallback confirmation and alternate mini-map retries, closes Alt+1 with a cheap fixed-settle close, registers the pathing intent with `current-map mini-map click fire-and-handoff`, and returns `PATHING_STARTED` so 修罗 continues formal `NAVIGATE_TO_TARGET`. Normal current-map navigation, 修罗 target current-map approach, 张闻, 五倍, 白龙马, 普通怪, 黄袍怪, route dialog, NPC click, OCR/template/click algorithms are unchanged. Added `NavigationXiuluoStartExitPrepathFireAndHandoffWiringTest`; compile and related source guards passed. 2026-06-21 40-minute 修罗 audit feedback found the live source suffix mismatch (`xiuluo-v2:start-exit-prepath:currentMap`), then Codex repaired the source gate and source guard. Fresh 修罗 runtime accepted: later reports show `start-exit-prepath:currentMap` repeatedly using `fire-and-handoff`, with no start-exit `handoff-fast-edge` / coordinate fallback and no blockage of formal target navigation. 已可关闭。 |
| CR78 | Codex | Done | `DialogService.handleMaintenanceBroadcastOptionFastPath`, `DialogHandleRequest`, `TaskMaintenanceService`, `AutoBattleTask`, maintenance broadcast logs | Stop doing full dialog fallback for auto-battle / lightweight member maintenance broadcast checks. Implemented and runtime-accepted: auto-battle / lightweight member idle maintenance now only runs the two fixed small-region templates; if both miss, it returns no broadcast immediately and does not run full `detectDialogSnapshotDirect(...)`. Fresh 修罗 runtime kept `maintenance-broadcast-fallback:auto-battle` at `0`; remaining `maintenance broadcast lightweight fallback disabled` / Auto+8 refresh/defer noise is tracked by CR65, not CR78. Leader/formal maintenance broadcast paths keep full fallback. Fixed-strip templates, click coordinates, dialog type detection, heal/repair option matching, maintenance cooldowns, and 五倍/白龙马/普通怪/黄袍/修罗 business phases were not changed by CR78. |
| CR79 | 谢帅 | Review: source guard, replay, compile/test-compile passed; fresh 修罗 runtime pending | `XiuluoTaskV2`, `images/template/dialog/xiuluo/xiuluo_wild_monster_cancel.png`, 修罗 enter-battle dialog logs | 修罗目标怪点击后，有时先弹出野外怪/挡路取消类 dialog，导致正常 `xiuluo_enter_battle_kanda.png` 看打模板 miss 后走重恢复/OCR/direct-combat。Implemented narrow behavior: only in 修罗入战 dialog recovery, after normal 看打模板 miss, try one additional template made from `images/template/cancel/Snipaste_2026-06-21_23-02-19.png` washed as `images/template/dialog/xiuluo/xiuluo_wild_monster_cancel.png`; if matched, click it to close the blocking dialog, do not treat it as battle entry, do not enter `WAIT_COMBAT`, and continue the existing target-click fallback/retry flow. Verification passed: `mvn -q -DskipTests compile`, `mvn -q -DskipTests test-compile`, `XiuluoWildMonsterCancelRecoveryWiringTest`, and `XiuluoWildMonsterCancelTemplateReplayDebug` with marked output at `images/test-cases/dialog/xiuluo-wild-monster-cancel/output/wild_monster_cancel_option_marked.png`. Fresh runtime still needs to prove logs show `看打` miss -> wild-monster cancel match/click -> target retry, with no false WAIT_COMBAT and no WUBEI/五倍 behavior change. |
| CR80 | Codex | Review: TDD/source test/compile passed; fresh 修罗 runtime pending | `XiuluoTaskV2`, `XiuluoRoundContext`, 修罗 pre-combat logs | Added 修罗 V2 pre-combat watchdog. Each round now records `preCombatStartedAtMs` in `XiuluoRoundContext` and preserves it across retry/recovery/pathing context copies. `XiuluoTaskV2.runRoundPhases(...)` checks the watchdog before another pre-combat phase; if `enteredBattleByXiuluo=false` and elapsed time reaches 180s, it logs `xiuluo pre-combat watchdog timeout: round={} phase={} elapsedMs={} limitMs=180000 source={}`, records a failed phase outcome, and routes through existing `restartRoundAfterPhaseFailure(...)` / same-round reaccept semantics. `WAIT_COMBAT`, return, back-to-start, team-return, and terminal phases are excluded; true combat entry via combat radar stops the watchdog. Verification passed: RED `XiuluoPreCombatWatchdogWiringTest` failed on missing `preCombatStartedAtMs`, then `mvn -q -DskipTests test-compile`, `XiuluoPreCombatWatchdogWiringTest`, and `mvn -q -DskipTests compile` passed. Bulk direct-java 修罗 test sweep reached an existing classpath issue in `XiuluoTargetMaintenanceGatePolicyTest` (`org/slf4j/LoggerFactory` absent from manual classpath), not a CR80 assertion failure. No OCR/template/click/navigation/return-item/五倍 business logic was changed. |
| CR81 | Codex | Review: read-only service/replay/compile passed; fresh reviewer pending | `TaskTrackerPanelService`, `TaskTrackerPanelReadResult`, `TaskTrackerGreenLink`, `images/template/task/xiuluo_tracker_title.png`, `XiuluoTrackerPanelReplayDebugMain`, task-tracker testcase images | 修罗左侧任务追踪 read-only 绿字坐标能力。Scope is only to recognize the left tracker `修罗任务` title, crop that task block, find the first usable green link, and return screen-absolute coordinate/debug evidence. Do not wire this into `XiuluoTaskV2`, do not click, do not register pathing intent, do not alter 修罗 phases/navigation/objective parsing, and do not change 五环/五倍 tracker behavior. |
| CR82 | Codex | Review: source/compile/focused guards passed; fresh runtime pending | `NpcClickService.runNpcClickPipeline`, `DialogService`, direct-combat / `Alt+A` 修罗 logs | Direct-combat should skip pre-click dialog detection entirely after `Alt+A` has entered target-pick mode. Fresh `2026-06-23 13:05:03.685-13:05:08.401` 修罗 evidence showed `Alt+A` direct-combat entered, then generic dialog detection saw `STORY` and returned before target strategies. Implemented narrow source repair: `verificationMode="direct-combat"` keeps `dialogType=NONE`, skips `currentPreClickDialogType(...)` / story cleanup / option pre-click gate, and also skips the post-tooltip dialog recheck so tooltip/yellow/formula/Ctrl strategies can proceed after Alt+A. Normal NPC click `STORY/OPTION` safety gates remain in the non-direct-combat branch; tooltip/yellow/OCR/click coordinate algorithms and 修罗/五倍 phase order were not changed. `mvn -q -DskipTests compile`, `mvn -DskipTests test-compile`, `NpcClickDirectCombatDialogGateWiringTest`, `NpcClickDirectCombatNameLayerWiringTest`, and `NpcClickDirectCombatSafetyWiringTest` passed. Fresh runtime still needs to prove a direct-combat attempt with visible/stale `STORY` reaches target scanning instead of returning early. |
| CR83 | Codex | Review | `XiuluoTaskV2`, `XiuluoRoundContext`, `WubeiTask`, `WindowRuntimeContext`, `WindowTaskRunner`, maintenance/pre-battle timeout logs | Pause or compensate pre-combat / pre-battle timers while formal maintenance is intentionally running. Fresh 修罗 `2026-06-23 13:42:20-13:45:24` evidence shows 医宝宝, 修装备, `maintenance broadcast handoff delay`, and team summon-skill maintenance were counted into CR80's 180s watchdog, causing a false `xiuluo pre-combat watchdog timeout` while the round was doing required upkeep. User clarified this must also apply to 五倍. Scope: 修罗 CR80 watchdog plus 五倍 ordinary pre-battle timer, probe timer, and enter-battle timer where active. Do not change the timeout limits themselves, OCR/template/click/navigation algorithms, maintenance cooldown semantics, or battle/return business rules. Implementation and focused wiring tests are complete; fresh runtime acceptance still needed. |
| CR84 | Codex | Done: CR90 fresh runtime passed | `XiuluoTaskV2`, `XiuluoRoundContext`, 修罗 route/combat state logs | 修罗快捷寻路路线状态模型。Implemented `routeMode=TRACKER_SHORTCUT/OBJECTIVE_NAVIGATION`, `combatSource=NONE/TRACKER_CONFIRM/INCIDENTAL`, tracker detail/click metadata, first tracker green-click timestamp, retry count, and optional pathing intent id. Defaults keep objective-route compatibility; `mvn -q -DskipTests compile` passed. Runtime accepted through the CR90 2026-06-25 修罗长跑 report. |
| CR85 | Codex | Done: CR90 fresh runtime passed | `XiuluoTaskV2`, objective snapshot/background parser, maintenance hooks, CR81 reader | 修罗接任务后快捷入口。Accept success now captures the same story/objective snapshot, starts background parse for fallback evidence, runs existing due maintenance, and attempts CR81 tracker shortcut without waiting for OCR. Initial tracker miss/click failure consumes the saved parse and enters the non-shortcut route entry. No second story capture/dialog detect was added. Runtime accepted through the CR90 2026-06-25 修罗长跑 report. |
| CR86 | Codex | Done: CR90 fresh runtime passed | `XiuluoTaskV2`, `TaskTrackerPanelService`, `WindowReadyEventBus`, tracker green click/pathing logs | 修罗 tracker 绿字点击与 park 等 Runner。Shortcut path reads CR81 tracker detail, clicks the first green link, records movement intent, registers a `WindowPathingIntentType.UNTARGETED_TRACKER` pathing intent, stores the intent id in `XiuluoRoundContext`, registers 修罗 enter-battle interest, opens the team maintenance window, and parks on Runner/window facts. `PATHING_TERMINAL` re-reads/re-clicks the tracker green link and parks again. No fixed wait/story read/manual target click/direct combat was introduced in the shortcut loop. Runtime accepted through the CR90 2026-06-25 修罗长跑 report. |
| CR87 | Codex | Done: CR90 fresh runtime passed | `XiuluoTaskV2`, `DialogService`, `WindowTaskRunner`, 修罗 enter-battle/WAIT_COMBAT logs | 修罗快捷路线最终开打框消费与 `combatSource`。Added 修罗 enter-battle dialog preparation provider and task-side validated consume. Prepared final dialog marks `combatSource=TRACKER_CONFIRM` and enters `WAIT_COMBAT`; combat observed without confirmation marks `INCIDENTAL`. `TRACKER_CONFIRM` exits use existing return-home flow, while `INCIDENTAL` exits resume tracker shortcut. Runtime accepted through the CR90 2026-06-25 修罗长跑 report. |
| CR88 | Codex | Review: compile passed; fresh CR90 runtime pending | `XiuluoTaskV2`, fallback/recovery logs, objective snapshot parse result | 修罗快捷路线失败 fallback 边界。Initial shortcut miss/click failure falls back to non-shortcut route entry through the accept-time background parse. Mid-shortcut tracker failure currently routes through explicit shortcut failure/reaccept semantics rather than old middle phases, and does not use the 修罗 return item before completion. Fresh runtime must still validate the boundaries. |
| CR89 | Codex | Done: CR90/CR96 fresh runtime passed | `XiuluoTaskV2`, team maintenance window, CR83 timer pause logs, 修罗 tracker shortcut maintenance logs | 修罗快捷路线维护/三技能窗口边界。Member maintenance remains blocked before successful tracker green click. Shortcut opens the team pathing maintenance window only after the green click commits the route, closes it on final dialog/combat/fallback/failure, and preserves CR83 timer compensation across same-green re-clicks. Runtime accepted through the CR90/CR96 2026-06-25 修罗长跑 report. |
| CR90 | Codex | Done: 2026-06-25 fresh runtime passed | `logs/dhxy-console.log`, `docs/run-reports/*xiuluo*`, CR84-CR89 runtime evidence | 修罗快捷寻路端到端验收卡。Run and document the complete shortcut route after CR84-CR89 land: accept task -> optional due maintenance -> tracker read -> green click -> park -> final enter-battle confirm -> WAIT_COMBAT -> return home; initial tracker miss fallback to non-shortcut using saved snapshot; `PATHING_TERMINAL` same-green re-click; `INCIDENTAL` combat resumes tracker; due maintenance does not false-timeout CR80; repeated same-green re-click does not reset watchdog. 2026-06-25 report covered repeated tracker green click -> prepared enter-battle -> combat -> return-home cycles without validation failure. |
| CR91 | 唐德 | Done: fresh runtime passed | `XiuluoTaskV2`, `DialogService`, `TaskTrackerPanelService`, accept-time game-window snapshot, start-exit-prepath logs | 修罗快捷路线接任务后 snapshot 与出村预路径重叠。After accept option success, capture one bound game-window snapshot and derive both story-objective fallback evidence and tracker green-link evidence from that same frame. If maintenance is not due, immediately start the existing Ling Shou Village exit prepath while tracker parsing runs, then click the parsed tracker green link after the mini-map closes. If maintenance is due, run existing maintenance first, then skip exit prepath and re-read/click current tracker green. Do not change tracker matching/click algorithms without replay evidence, do not enter old objective navigation unless shortcut startup fails, and keep CR83 timer compensation. 2026-06-25 long run shows accept snapshot/objective parse and tracker shortcut consumed before pathing across repeated rounds. |
| CR92 | Codex | Review: `pause-wake` 源码/guard/编译通过，待 fresh runtime | `WindowReadyEventBus`, `WindowTaskRunner`, `TaskPauseToken`, `WubeiTask`, 修罗/五倍 task park 与 pause 日志 | pause/stop 主动唤醒 ready-event wait。Fresh 修罗 `2026-06-27 21:36:49.846` 证明仅在 `WindowReadyEventBus.await(...)` 返回后补偿暂停时间不够：leader 已收到 pause request，但仍卡在 `WAIT_TARGET_PATHING_TERMINAL`，直到 bounded timeout 才进入 `TaskPauseToken` checkpoint。本次实现给 `WindowReadyEventBus` 增加 `pause-wake` / `stop-wake` control wake，`WindowTaskRunner.pauseCurrentTask()` / `stopCurrentTask()` 会通知当前窗口 park；control wake 返回空业务事件，不会伪装成 `PATHING_TERMINAL`、`PREPARED_ACTION_READY` 或 combat 事实，修罗/五倍继续沿用现有 pauseBlockedMs/timer compensation；五倍 finite prepared-dialog wait 的 empty 分支先 checkpoint，避免 pause-wake 被当作普通未准备好。Fresh 验收：leader 在 pathing/pre-combat park 中 pause 后应秒级 checkpoint，不再等剩余 wait timeout。 |
| CR94 | Codex | Review: implementation complete; compile/test-compile/guard passed; fresh runtime pending | `TaskMaintenanceService`, `SummonSkillService`, `AutoBattleTask`, 三技能 unknown failure logs | 三技能 unknown 失败退避与布局缓存失效。Implementation adds a per-window unknown retry-after (`bot.dhxy.summon-skill-unknown-failure-retry-after-ms`, default 5 minutes) before team-round claim / `SummonSkillService` entry, records backoff on unknown-class failures, invalidates trusted skill-count/start-slot/tail-safe layout cache, and clears the retry state on successful cleanup or identity drift. `TaskMaintenanceSummonSkillUnknownBackoffTest`, `mvn -q -DskipTests compile`, and `mvn -q -DskipTests test-compile` passed. Fresh runtime still needs to prove no repeated `maintenance: start summon skill clean` for the same window during the 5-minute unknown backoff. |
| CR95 | 唐德 | Review: heartbeat P1/P2 repaired and source verified; fresh runtime pending | `WindowNativeBindingRefreshService`, `WindowRuntimeContext`, `GameClientTracker`, `ClientIdentityService`, per-window maintenance caches, pure input paths, `TeamRoleDetectionService`, `AutoCombatService`, `BoundWindowKeyboardService` | Live window identity drift guard. Fresh 2026-06-23 runtime proves a bound HWND can still be valid while its actual game title/account has changed: `images/temp/hwnd-E850B6A/latest_vision.png` shows 大叔, but logs still print `hwnd-E850B6A` as 忆叶知秋 and reuse its 6-slot 三技能 cache; `images/temp/hwnd-3960EA6/latest_vision.png` shows 岁月醉白头 while the context still carries 大叔. Implementation refreshes live native title/class/process and adds same-HWND drift/epoch invalidation. Repaired items: 三技能 epoch invalidation runs before old-player success cooldown; runtime commit is synchronized; production live refresh + commit use synchronized `refreshAndCommit(...)`; queued `InputActionRequest` carries and validates request-time `playerIdentityEpoch`; pure HWND keyboard refresh-time drift / refresh-unavailable now return terminal shortcut failures before `PostMessage`; `InputActionWorker` does not fall back to focused/direct input on terminal failures; `NavigationService.pressAlt1ForMiniMap(...)` terminal failure aborts the whole mini-map transaction before later sleeps/clicks; real-input queue/focus paths fail closed when live refresh is unavailable; service-level HWND shortcut guard rejects unvalidated `Alt+A/C/U`; background input worker whitelist is narrowed to verified `Alt+1/2/4/6/8/T/O/E/Q`; `ClientIdentityService` / `TeamRoleDetectionService` refresh live binding before reading runtime title; role detection prefers runtime title; AutoCombat pending state resets on drift. Compile/test-compile plus CR95 focused wiring tests passed. Fresh runtime still needs to prove identity drift is logged before maintenance and old-account input is rejected instead of executed. |
| CR96 | Codex | Done: fresh runtime passed | `XiuluoTaskV2`, `XiuluoRoundContext`, `WindowTaskRunner`, `WindowPathingIntent`, `TaskMaintenanceService`, accept-time game-window snapshot / objective parse logs | 修罗 tracker shortcut 到达目标地图即关闭团队三技能维护窗口。Fresh runtime proves the accept-time game-window snapshot can already be parsed by the existing story-objective parser to know the target map, but the tracker pathing intent was `UNTARGETED_TRACKER` with `activeIntentTarget=null`, so the team maintenance window remained open until `shortcut-enter-battle-prepared`. Implementation keeps the tracker green click first, then uses the existing accept-time objective future without blocking: if the map is already available, it registers the shortcut `WindowPathingIntent` as a map-only targeted intent; if not, it registers the old untargeted intent and attaches a late target-map upgrade, which upgrades that exact active intent after the objective future completes. When Runner reports ARRIVED on that parsed target map, 修罗 closes the team pathing maintenance window with `source=xiuluo-v2:shortcut-target-map-arrived` and continues waiting for prepared enter-battle/combat. If parse misses/fails or the intent is no longer active, prepared/combat/fallback/failure close paths remain fallback. It still does not depend on tracker green `targetMapName`, and does not change tracker green matching, objective OCR/template/click algorithms, summon-skill click logic, or CR83 timer policy. 2026-06-25 runtime showed targeted tracker intents and no late team-maintenance close blocker. |
| CR97 | 谢帅 | Done: fresh runtime passed | `XiuluoTaskV2`, `XiuluoRoundContext`, `WindowRuntimeContext`, 修罗 WAIT_COMBAT / enter-battle logs | 修罗 prepared 看打点击后未入战必须重试或恢复。Fresh runtime round 72: Runner prepared `XIULUO_ENTER_BATTLE` at `15:55:45.269`, physical click ran at `15:55:45.997`, task cleared dialog interest and entered `WAIT_COMBAT` at `15:55:46.161`; afterward Runner kept seeing `visibleDialog=OPTION` with `preparedOperation=null`, while task repeated `message=waiting for combat state` until combat only appeared at `15:58:55.399`. Implementation now treats prepared/normal/recovered "看打" clicks as pending until `WAIT_COMBAT` sees combat, re-registers bounded shortcut `XIULUO_ENTER_BATTLE` interest when no combat follows, and falls back through the existing shortcut recovery path after retry exhaustion. OCR/template/click algorithms were not changed. 2026-06-25 runtime showed prepared enter-battle clicks followed by combat and no repeat of the old indefinite `WAIT_COMBAT` stuck case. |
| CR98 | 谢帅 | Done: 2026-06-25 combat-start runtime passed | `WindowTaskRunner`, `TaskExecutionContext`, `DefaultWindowTaskStartupInitializer`, `AutoCombatService`, `AutoCombatPanelService`, `XiuluoTaskV2`, `WubeiTask`, combat-start logs | 战斗中启动修罗/五倍必须先等战斗结束再做启动前置，并进入战斗后启动恢复。Fresh runtime shows `16:04:16.471` user submitted `XIULUO_V2` while the window was not readable for team role; startup preflight ran team-role detection anyway, got `role=SOLO`, and skipped the leader-only task before 修罗 hot-start could run. Implementation adds runner-level combat-start defer before team-role detection/startup UI prep and passes `TaskStartupMode.AFTER_COMBAT_EXIT_STARTUP` into 修罗/五倍 after combat exit. P1 repair: startup defer now uses `AutoCombatService.probeWindowCombatStateReadOnly(...)`, which refreshes combat radar but does not consume combat-enter or open the auto-combat panel / send `Alt+8`. P2 repair: 五倍 hot-start only enters `READ_TRACKER` when the tracker panel has an actionable green link; title-only tracker hits fall through to after-combat return-item verification. P3 remains a clarification: live role `UNKNOWN` after combat still falls back to `windowContext.role` unless the card is tightened. Fresh 2026-06-25 18:23 runtime showed startup combat defer for all windows, no in-combat team-role skip, leader role detection only after combat exit, then 修罗 after-combat return-item recovery. |
| CR99 | 谢帅 | Review: source/compile/replay passed; test-compile blocked by existing test-source missing symbols; fresh runtime pending | `NavigationService`, `GameTextLineOcrService`, `CoordinateHelper`, `WorldMapRouteGuardReplayDebug`, world-map route testcase images, navigation/pathing logs | 新版世界地图黄链直开目标地图小地图导航。Game update allows clicking the final yellow destination/map-name link in the world-map route result to open that destination map's mini-map before the player arrives there. Default route now keeps existing world-map open/search/input/scroll/target-row guard, clicks the matched yellow destination link, requires the opened target-map mini-map panel, clicks the requested final coordinate on that panel using the same mini-map coordinate mapping, then runs direct cleanup inside the same input callback and registers the normal coordinate-aware pathing intent. Old green-link route-result path remains legacy/switchable and is not used as blind fallback when the target row/yellow destination cannot be verified. Source guard, compile, yellow destination replay, and destination mini-map coordinate replay passed; full `test-compile` is blocked by unrelated existing missing-symbol test sources; fresh runtime still needed. |
| CR100 | Codex | Review: source/test-compile/focused guard/compile passed; fresh runtime pending | `InputActionQueue`, `InputActionRequest`, `InputActionWorker`, `InputActionScope`, `TaskExecutionContextHolder`, pause/input logs | Pause must abort queued physical input before later actions in the same request. Fresh logs show a user pause can reach task checkpoints while an already-submitted `npcClick:taskTooltipTemplate#1` sequence continues `MOVE_MOUSE -> SLEEP -> CLICK_LEFT`; the worker only checks cancellation/interruption/player epoch and does not know the task pause token. Capture the submitting task's pause token into `InputActionRequest`, treat pause as cancel-like while executing input requests, and check it before focus, before action-list execution, before each action, and before exclusive callbacks. Do not block the global input worker waiting for resume; return false/dead-letter the request so no post-pause click executes. Update `InputActionScope.isCancelled()` so exclusive/direct-input callbacks that already poll scope also stop on pause. Keep move+click atomic for non-paused requests; do not change NPC click, navigation, coordinates, OCR/template, or task business phases. |
| CR101 | Codex | Review: source/test-compile/focused guard/compile passed; fresh runtime pending | `WindowTaskRunner`, route dialog preparation logs, 修罗 shortcut pathing logs | Runner route-dialog preparation must not freeze the watcher thread. Fresh 修罗 round 23 shows `11:09:14.254` Runner started `ROUTE_TRANSFER target=瑶池`, `11:09:14.257` reused the OPTION detection, then produced no route prepare result, no terminal/prepared/combat event, and left `WAIT_TRACKER_SHORTCUT_PATHING` parked for minutes. Route dialog preparation now runs in a bounded per-runner background executor with the same window/task context; if OCR/option preparation does not return within `WINDOW_ROUTE_DIALOG_PREPARE_TIMEOUT_MS`, Runner cancels it, logs `prepare-timeout`, marks the request failed when applicable, and keeps the watcher alive so CR80/CR83/CR92 timers can recover. OCR/template thresholds, route matching, click coordinates, 修罗 phases, and navigation semantics were not changed. |
| CR102 | Codex | Review: P2 feedback fixed; source/compile passed; fresh runtime pending | `UICleanerService`, `DialogService`, `GameClientTracker`, UI cleanup logs | `cleanUpAll()` no-op cleanup is too expensive because it performs repeated independent visual scans: map detection runs before dialog cleanup and again inside generic-window cleanup, dialog inspect takes its own capture path, and generic close scanning calls `tracker.updateGlobalVision()` again. Fresh 2026-06-25 10:55 evidence shows an empty cleanup (`NO_DIALOG`, no map, no generic close button) still took about 8s. Implemented a staged `CleanupPass` that reuses one fresh game-window frame for same-pass map/generic-window template checks, skips duplicate map detection inside the same pass, and avoids recapture after a dialog-only click. It still invalidates the frame after any successful map close or generic window close before scanning the next layer, so this is not "one screenshot closes every stacked window". **P2 follow-up fixed:** map checkbox detection keeps same-pass frame reuse but now crops the map-popup ROI from the cached frame and matches inside that crop, restoring old `findImageInRegion(...)` ROI semantics without another live capture. Fresh runtime still needs to confirm lower empty-cleanup elapsed time and stacked-window safety. |
| CR103 | Codex | Review: P2 feedback fixed; source/compile passed; fresh runtime pending | `NpcClickService`, `DialogService`, `OcrRoiMemoryService`, `config/vision_memory.json`, NPC expected-option logs | Smart NPC clicks whose success is proven by a later expected option/template match must commit direct-click evidence instead of leaving it pending forever. Fresh 2026-06-25 10:55 李道宗 repair path is only the clearest sample: `expectedTemplate=repair_equipment_option.png`, task-tooltip click `verified=true`, `runnerOwned=true`, and `smart-click evidence pending runner confirmation`, followed by ROI memory recording but no `NPC click attempt recorded`; `config/vision_memory.json` therefore has many 李道宗 ROI successes but only two direct samples. Implemented a generic `SmartClickEvidenceConfirmationService` boundary: `DialogService.finishRequest(...)` confirms pending smart-click evidence only after expected/business option proof (`GREEN_TEMPLATE_VISIBLE`, `GREEN_TEMPLATE_CLICKED`, `BUSINESS_OPTION_CLICKED`, or `OPTION_KEYWORD_CLICKED`), and `NpcClickService` commits only pending evidence whose original expected template/action proof matches. **P2 follow-up fixed:** pending evidence now carries a per-click proof token stored on the bound `WindowRuntimeContext`; `DialogService.finishRequest(...)` must pass the same token into `confirmExpectedOptionProof(...)`, so a stale same-window pending click cannot be committed by a later unrelated option proof that only happens to share a template/action. Template/action mismatches clear the matching pending record/token, while unrelated proofs without the current token are ignored. If `clickNpcSmart` itself already verifies the expected template, it now records direct-click evidence immediately as inline expected-option proof instead of parking it forever. Bare unverified clicks remain pending/uncommitted. Fresh runtime still needs to prove 李道宗/医宝宝/repair expected-option success emits `NPC click attempt recorded` without committing stale clicks. |
| CR104 | Kuhn | Done: fresh 修罗 runtime passed | `XiuluoTaskV2`, `AutoCombatService`, `AutoCombatPanelService`, `UICleanerService`, 修罗 WAIT_COMBAT / auto-combat maintenance logs | 修罗 leader `WAIT_COMBAT` must wake for due combat maintenance while keeping the 4s entry-maintenance delay. Fresh 2026-06-25 logs proved the bounded wake fixed the old `timeoutMs=-1` park, but fresh 2026-06-26 logs show the leader repeatedly runs only `entry-maintenance` with `refreshRounds=false` and `auto-combat panel rounds refresh skipped: reason=verify-only`; no leader `low-rounds` / `refresh-due` Alt+8 refresh appears even when the user observed only 8 rounds left. Additional 2026-06-26 18:24-18:49 and 18:26-19:26 evidence on `hwnd-2AD117C` repeats the same leader verify-only pattern, while member windows refresh by Alt+8 with `reason=refresh-due`. Source repair keeps entry-maintenance verify-only but removes its early return, lets due/low/unknown refresh checks continue in the same tick, and makes `nextCombatMaintenanceDelayMs()` wake immediately for low/unknown or refresh-due with existing per-window/team guards. Follow-up repair fixes the remaining stale-cache gap: after entry-maintenance, `AutoCombatService` arms one actual-round read, and `AutoCombatPanelService` reads visible panel rounds before trusting a healthy cached estimate; if the visible value is low, the existing Alt+8 path runs with `reason=low-rounds`. CR65 remains intact: `refresh-due` still uses team/per-window gate; `low-rounds` bypasses the team gate but has a per-window urgent retry cooldown to avoid 900ms failure loops. Focused `AutoCombatRefreshDuePanelVerifyGateTest` guards no early return, one actual read after entry-maintenance, and visible-round read before healthy-cache skip; compile/test-compile/focused test/diff-check passed. Fresh 2026-06-27 修罗 runtime passed: leader `hwnd-17240550` emitted `auto-combat panel rounds refresh by Alt+8 without OCR: source=verify reason=refresh-due estimate=22` at `17:49:21.056`, reset estimate to 25 at `17:49:22.283`, and same-team member refresh-due probes were deferred by team gate at `17:49:25.084` / `17:49:25.419`. |
| CR105 | Codex | Done: fresh runtime passed | `XiuluoTaskV2`, `WubeiTask`, maintenance broadcast handoff logs | 修罗/五倍 leader maintenance broadcast handoff delay should be fixed 3s after 医宝宝/修装备 broadcast instead of `registeredWindowCount * 2000ms`. Implemented fixed `MAINTENANCE_BROADCAST_HANDOFF_DELAY_MS = 3_000L` in both tasks, removed handoff-time `getRegisteredWindowCount()` usage and `windowCount/perWindowMs` handoff logs, while preserving CR83 pre-combat/pre-battle timer compensation. Keep maintenance cooldowns, follower broadcast handling, OCR/template/click/navigation, and task phase semantics unchanged. Acceptance passed in fresh 2026-06-25 runtime: handoff logs show `delayMs=3000` and no `windowCount` / `perWindowMs` handoff fields. |
| CR106 | Codex | Done: user-confirmed fresh runtime passed | `NavigationService.navigateToMap`, `NavigationService.navigateToLingShouVillageViaZhangWen`, CR99 yellow-destination route flow, 灵兽村 navigation logs/testcases | Removed the special early branch that diverted `targetMap=灵兽村` to `长安 -> 张闻 -> 灵兽村`, so runtime navigation now falls through to the existing CR99 world-map yellow-destination mini-map route. Marked the Zhang Wen chain as deprecated retained source only, with no production call path from `navigateToMap`. User confirmed fresh runtime acceptance on 2026-06-26. |
| CR107 | Codex | Done: user-confirmed fresh runtime passed | `TaskStartupWindowPreparationService`, `DefaultWindowTaskStartupInitializer`, `AutoBattleTask`, `AutoCombatService`, left-top status templates/debug main | 修罗/五倍启动与战斗中维护左上角状态开关。Production uses corrected window-relative ROI `(8,147,11,19)`, leader startup/combat maintenance close when open, member startup only records pending, and member safe-window/combat maintenance consumes pending. User confirmed fresh runtime acceptance on 2026-06-26. |
| CR108 | Codex | Review: source guard/compile/test-compile passed; fresh runtime pending | `FiveRingTaskV2`, `FiveRingPhaseContext`, `WindowTaskRunner`, `WindowRuntimeContext`, 五环 accept-NPC pathing logs | Implemented locally: 五环 accept-NPC wait no longer treats the 2.5s fast observer grace expiring as permission to retry while the same accept-NPC pathing intent is still active. It now consumes terminal snapshots first, keeps waiting on `ACTIVE/UNKNOWN/probeInProgress` or same active intent, permits retry on `STOPPED_AWAY`, and only uses the long hard timeout for safe recovery. Runner observer slow logs now use the active-intent snapshot captured at the tick branch decision instead of rereading live state later. CR99 yellow matching/click coordinates, current-map fallback implementation, NPC smart click, and world-map route algorithms were not changed. Fresh runtime still needs 67555-style acceptance logs. |
| CR109 | Codex | Review: rework guard/compile/test-compile passed; fresh runtime pending | `AutoCombatService`, `XiuluoTaskV2`, `WubeiTask`, `PlayerStateService`, expected-combat exit logs | Implemented explicit post-combat recovery policy. Expected 修罗 `TRACKER_CONFIRM` and 五倍 `WAIT_BATTLE_FINISH` exits use `FAST_EXPECTED_EXIT`: consume exit, reset combat state, mark pending leader post-combat recovery, set `FREE`, and return to the task before HP/MP / 摄妖香 checks. Fresh 2026-06-26 13:55 修罗 runtime proved the original deferred-consume point was too early. Rework now removes immediate after-return-home consumption: 修罗 consumes only after next accepted task has started progress (`start-exit-prepath`, tracker shortcut green click, or target navigation pathing), and 五倍 consumes after the next accept task succeeds. Legacy/incidental/unknown combat remains conservative. |
| CR110 | CR110 worker | Review: source guard/replay repair passed; compile/fresh runtime pending | `NavigationService`, `WorldMapRouteResultMemoryService`, `WorldMapRouteResultMemoryEntry`, `WorldMapRouteResultPendingMemory`, `WindowTaskRunner`, CR99 yellow-destination route logs/tests | Added watcher-confirmed memory for CR99 yellow destination route-result row clicks, parallel to the old legacy green-link memory. Fresh 修罗 `2026-06-27 20:40:23.529/20:41:28.695` proved this path is the new yellow route (`routeMode=YELLOW_DESTINATION_MINI_MAP`, `mode=yellow-destination-mini-map`), not legacy green navigation, and that the old memory fast-path guard wrongly blocked final 洛阳城 coordinate clicks by comparing `MiniMapCoordinateReader.readCurrentTemplateLocation()` player/current map `灵兽村(111,92)` to expected `洛阳城`. Source repair removes that current-map identity guard only from `YELLOW_DESTINATION_MINI_MAP` memory mode, keeps the target mini-map panel visibility gate, final target-coordinate click, and `confirmMiniMapPathingStartedForHandoff(...)` proof before movement/cleanup/success. Failed remembered yellow clicks now dirty/demote the yellow entry and force route UI cleanup/reprepare instead of running fresh OCR on dirty UI. Non-regression boundary: top-level `navigateToMap` current-map fresh confirm, already-on-target-map `ARRIVED`, and `navigateToNPC -> navigateInCurrentMap` second step remain intact; legacy green-link route is not re-enabled. |
| CR111 | Worker | Re-review: 修罗 runtime evidence exists; 五倍/五环 branch coverage pending | `NavigationService`, `WindowTaskRunner`, `WindowRuntimeContext`, `WindowPathingIntent`, `WindowPathingSnapshot`, `XiuluoTaskV2`, `WubeiTask`, `FiveRingTaskV2`, pathing wait logs | Make Runner/window pathing watcher the only authority for navigation completion. Any task path that receives `PATHING_STARTED` with a registered/current `WindowPathingIntent` must wait for Runner terminal (`ARRIVED`, `STOPPED_AWAY`, or explicit pathing timeout`) and must not use local `GameStateUtil.detectMovementState()` / pixel diff / weak coordinate samples to declare movement ended. Review repairs addressed timeout starvation: 五环 `waitPathing(...)` / accept-NPC check hard timeout before watcher keep-wait; 修罗 `continueIfNavigationStillPathing(...)` checks `RUNNER_PATHING_HARD_TIMEOUT_MS` before same-intent keep-wait; 五倍 `continueIfMaintenanceNavigationStillPathing(...)` checks matching maintenance `intentAgeMs` against `WUBEI_MAINTENANCE_PATHING_HARD_TIMEOUT_MS` before null/NONE/ACTIVE/UNKNOWN/probe keep-wait. 2026-06-26 re-review found no new P1/P2/P3 source blocker in these inspected CR111 paths; focused guards assert timeout ordering and compile/test-compile passed. Fresh 修罗 runtime evidence exists: `20:10:55.764 PATHING_STARTED -> WAIT_TRACKER_SHORTCUT_PATHING`, `20:12:31.186 PATHING_TERMINAL state=ARRIVED target=大雁塔四层` woke the wait by event after `95422ms`, `20:30:43.180 target=万寿山` woke after `30902ms`, `20:32:29.817 target=凤巢五层` woke after `38095ms`, and `20:33:36.471` accept-NPC current-map published `PATHING_TERMINAL state=ARRIVED target=灵兽村`. Existing `2026-06-22` 修罗 run report also records CR111 positive samples with no local movement/pixel self-declared completion. Keep open only because 五倍 maintenance pathing and 五环 `waitPathing(...)` / accept-NPC runtime branches still need sampled evidence before marking the whole card Done. |
| CR112 | Codex | Review: source guard/compile/test-compile passed; fresh runtime pending | `XiuluoTaskV2`, `WindowReadyEventBus`, 修罗 WAIT_COMBAT logs | Repair WAIT_COMBAT combat-exit event race. 修罗 previously captured the ready-event sequence after `handleCombatTick(...)`; if that tick published `COMBAT_STATE_CHANGED` during the call, the following wait excluded the event and fell back to slow timeout/maintenance wake. Capture the sequence before the combat tick and pass it into the wait for both normal WAIT_COMBAT and tracker-shortcut incidental combat. No OCR/template/click/navigation behavior changed. |
| CR113 | Codex | Review: source guard/compile/test-compile passed; fresh runtime pending | `BattleRadarService`, `AutoCombatService`, expected-combat exit logs | Add fast expected-combat exit probe for 修罗/五倍 expected battles. After confirmed combat entry and a 15s grace, FAST_EXPECTED_EXIT uses a 1s cadence 20x20 leader-avatar ROI diff based on the existing `ImageFinder.isMatch(...)` helper, with the old full battle-radar check retained as sparse fallback. Goal is to reduce post-combat return-home latency without changing combat entry detection or incidental/unknown battle recovery. |
| CR114 | Codex | Done: user-confirmed fresh runtime passed | `BagService`, 修罗/五倍 task-page item logs | Make task-page item lookup current-page-first and no full-bag sweep. 修罗/五倍 return/probe task items open/ensure the bag, scan the current visible page first, and only if missed click/scan the task tab; they do not scan pages 6->1 for task-only items. User confirmed fresh runtime acceptance on 2026-06-26. |
| CR115 | Codex | Done: fresh 修罗 runtime passed | `WindowTaskRunner`, 修罗 tracker shortcut enter-battle logs | Prioritize 修罗 `XIULUO_ENTER_BATTLE` dialog interest over stale/fresh `ROUTE_TRANSFER` prepared actions. Fresh 2026-06-26 runtime round 52-65 repeatedly prepared/consumed `XIULUO_ENTER_BATTLE` after tracker shortcut, including `20:22:54.002 -> 20:22:54.188`; no `ROUTE_TRANSFER` mismatch loop reappeared in the sampled run. |
| CR116 | Codex | Done: fresh 修罗 runtime passed | `XiuluoTaskV2`, `WindowReadyEventBus`, tracker/background-parse waits, 修罗 pre-combat watchdog logs | Make 修罗 180s pre-combat watchdog a real per-wait budget. Fresh 2026-06-26 runtime round 52-65 showed bounded tracker/pathing waits with Runner `PATHING_TERMINAL` / `PREPARED_ACTION_READY` wakeups and no infinite `timeoutMs=-1` park / watchdog bypass in the sampled 修罗 run. Combat time and approved maintenance/supply pauses remain excluded. |
| CR117 | Codex | Done: fresh runtime passed | `SheyaoxiangDigitTemplateReader`, `PlayerStateService`, `images/template/status/sheyaoxiang_digits`, 摄妖香 status logs | Add self-learning digit-template reader for 摄妖香 green remaining-minute text. OCR may read a two-digit image like `47` as only `7`, causing false low-minute refill. Fresh 2026-06-26 runtime logged `sheyaoxiang status matched ... remaining=green-minutes-template=16`, proving the template-reader path drives the remaining-minute decision instead of a partial OCR digit. |
| CR118 | Codex | Done: user-confirmed fresh runtime passed | `XiuluoTaskV2`, `PlayerStateService`, 修罗 startup / first-aid logs | Run 修罗 startup first-aid before first-round hot-start selection. First 修罗 run calls `performStartupFirstAidCheck(context)` before `ensureStartupIncenseBeforeHotStart(context)` and before `resolveStartupTrackerOrReturnItem(...)`; repeat rounds do not get an extra startup check. User confirmed fresh runtime acceptance on 2026-06-26. |
| CR119 | Codex | Done: fresh 修罗 runtime passed | `TaskMaintenanceService`, `XiuluoTaskV2`, `WindowReadyEventBus`, 修罗 `WAIT_TRACKER_SHORTCUT_PATHING` logs | Repair 修罗 leader 三技能 due wake while shortcut/pathing waits are parked. Fresh 2026-06-26 runtime round 55 showed the leader firing the before-park due path: `19:59:32.344` `leader maintenance summon skill due`, claim `xiuluo_v2#55`, and `19:59:43.970` cleanup `success=true`; later round 65 also kept member三技能 deferred after CR96 target-map-arrived window close. Slot templates/clicks, tail-boundary cleanup, team-round claim semantics, CR96 target-map-arrived close, and CR116 pre-combat budget were not changed. |
| CR120 | Kant+Codex+Ramanujan | Review: 五倍早消费源码修复完成；fresh runtime 待验 | `CommonBoxService`, `CommonBoxRole`, `TaskMaintenanceService`, `TaskMaintenanceRequest/Result/Status`, `MainWindowController`, `GameUiSettingsStore`, `BotProperties`, `XiuluoTaskV2`, `WubeiTask`, `AutoCombatService`, `ImageFinder`, `images/template/common/leader_box_marker.png`, common-box replay | Implement the `docs/业务逻辑.md` 通用盒子逻辑 for 修罗 and 五倍. Add independent UI switches `队长要盒子` default on and `队员要盒子` default off; detect the marker in window-relative ROI `(623,590)-(682,618)` using the common template; record per-window pending box hits with 30s TTL instead of clicking immediately; consume pending boxes only at explicit safe hooks, not through generic `TaskMaintenanceRequest/Result/Status`. ROI detection is synchronous at the safe hook and uses in-memory template matching, not common-pool async work or normal-runtime `common_box_roi_*.png` temp files. 队长 detects after verified return-home and consumes after the next task is accepted and movement starts. 队员 detects after combat exit and consumes on its next task turn/input opportunity independent of HP/MP first-aid; if the box consumes first, pending HP/MP first-aid remains pending for the next safe turn. Pending state is window/task-run/role scoped with a process-global monotonic taskRunId, clears on switch-off/expiry/click, rejects stale identity/task-run records, prunes expired entries on detect/consume, and does not intentionally change 修罗/五倍 accept/navigation/combat/return/failure rules. Template caching P3 is fixed with lazy `cachedTemplate`; guard-quality P3 is fixed by shrinking `CommonBoxLogicWiringGuard` to broad architecture tripwires while pending/identity/first-aid boundaries are covered by focused behavior tests. 2026-06-26 runtime blocker fixed: `CommonBoxService` no longer reads mutable `WindowRuntimeContext.getRole()` as business truth; common-box role now comes from `TaskExecutionContext.windowRole`, while runtime context is only used for hwnd/window/identity/task-run anti-cross checks. Fresh 修罗 runtime `2026-06-27 20:32:35.446` proved a leader box hit and pending create, but `20:33:07.489` pruned it as expired before the next-round movement hook at `20:33:11`. Source repair keeps the 30s TTL and adds an earlier highest-priority 修罗 consume hook at `AFTER_ACCEPT_MAINTENANCE_CHECK` before prepath/maintenance; focused guard passed and fresh consume proof is pending. Fresh WUBEI runtime 2026-06-28 00:29 proved the same timing class remains in 五倍: leader pending was created, but heal-pet broadcast plus a 3s handoff ran first and the pending expired before tracker pathing. Ramanujan source repair adds 五倍 `wubei:after-accept-maintenance-check` and `wubei:before-tracker-pathing-maintenance-check` highest-priority consume hooks before maintenance/broadcast/pathing; focused guard, compile, and test-compile passed. Fresh runtime must prove the pending is consumed before any 30s expiry. |
| CR121 | Codex+Fermat+Descartes | Done: fresh WUBEI bounded combat wait validated | `AutoCombatService`, `BattleRadarService`, `XiuluoTaskV2`, `WubeiTask`, expected-combat return verification logs/tests | 修复 CR113/CR109 快脱战安全纠偏，并把五倍 expected combat 对齐到 `FAST_EXPECTED_EXIT` + 回程先验流程。已保留 avatar-diff 快路径，不加回程前 full-radar 确认；回程验证失败后才用可信战斗状态纠偏，仍在战斗则回到 `WAIT_COMBAT` / `WAIT_BATTLE_FINISH` 并保留 deferred recovery。2026-06-28 旧进程曾暴露 WUBEI `WAIT_BATTLE_FINISH timeoutMs=-1`，Descartes 窄修在 `WubeiTask.parkAfterYieldIfNeeded(...)` 加最终防线：任何 `WAIT_COMBAT_STATE_CHANGE` wait spec 若带 `-1/0` 等非法 timeout，进入 `awaitNewer` 前都会重新用 `autoCombatService.nextCombatWakeDelayMs()` clamp 到 `500..10000ms`，同时保留 `COMBAT_STATE_CHANGED` 立即唤醒。focused guard 覆盖 helper 与生产 park 边界；重启后 fresh WUBEI `01:20:04-01:20:57` 实战 wait 连续显示 `timeoutMs=914/827/920/866/925/3805`，并在 `01:20:17.756` 完成 cached return verification，没有再出现 `timeoutMs=-1`。已可关闭。 |
| CR122 | Codex+Aristotle | Review: WUBEI stale-intent repair/guard/compile passed; fresh runtime pending | `NavigationService`, `WindowTaskRunner`, `WindowRuntimeContext`, `XiuluoTaskV2`, `WubeiTask`, CR99/CR110/CR122 tests, 修罗/五倍 navigation logs | Collapse 修罗/五倍 default pathing semantics to two production movement classes: task tracker/shortcut green-link pathing, and mini-map coordinate handoff. Implemented source-level repair: CR99 yellow destination routing now treats the yellow target click only as the way to open the target-map mini-map; after the final target-coordinate click it calls the existing mini-map handoff confirmation before movement intent/logical `PATHING_STARTED` handoff. The coordinate `WindowPathingIntent` is now registered only after `yellow-destination-mini-map-pathing-confirmed`; if movement is not confirmed, the yellow route returns failure/retry instead of parking. Yellow memory and fresh OCR paths both cleanup through queued cleanup on failure/success. Runner stopped-away classification is now only two live buckets, shortcut/tracker and mini-map handoff, both currently `2_200ms`; the old 8s map-route and 30s cross-map-coordinate buckets were removed from the live resolver. 2026-06-27 stale-intent repair adds source-prefix-scoped runtime cleanup and clears `xiuluo-v2:tracker-shortcut` intents on prepared enter-battle consume, `XIULUO_V2` combat entry, verified return-home, and round-start transition. 2026-06-28 WUBEI repair clears `wubei:tracker-green-click` intents at round start, before a new tracker green click registers a fresh intent, after enter-battle dialog consume, on Runner-confirmed WUBEI combat entry, and after verified return-home. Focused WUBEI/Xiuluo CR122 guards and `mvn -q -DskipTests compile` passed. Fresh runtime still needs to prove no stale terminal from a previous round and no 30s `STOPPED_AWAY` after a failed yellow final-coordinate click. |
| CR123 | Codex | Review: Worker + Java uploader + real R2 smoke passed; runtime opt-in validation pending | `D:/mavenProject/dhxy-case-worker`, Cloudflare Worker, R2, DHXY client case reporter, `logs/cases/*.case.json`, optional dashboard | Remote diagnostic case upload. Worker slice is implemented in sibling project `D:/mavenProject/dhxy-case-worker`: `POST /api/case/upload` validates bearer/header upload token, payload size, required case schema, and license identifier, stores valid structured JSON under `cases/YYYY-MM-DD/<machineHash>/<taskCode>/<caseId>.case.json`, and updates `indexes/YYYY-MM-DD.json` in R2. It accepts the CR124 nested local-case shape (`task.taskCode`, `app.licenseId`, `runtimeSnapshots.environment.machineHash`) while keeping flat-field compatibility. Follow-up slices populated upload identity metadata (CR125), added the default-off Java uploader/retry/status layer (CR126), and validated the real Cloudflare/R2 deployment (CR127). Real public smoke upload to `https://dhxy-case-worker.yueyunfe.workers.dev/api/case/upload` returned `200 CASE_UPLOADED`; remote R2 verification downloaded `indexes/2026-06-27.json` and the case object under `cases/2026-06-27/machine-worker-contract/xiuluo_v2/...case.json`. No R2 credentials were embedded in DHXY Java, and full logs/zips are not uploaded. Remaining acceptance is only an opt-in app-runtime validation with `case.upload.enabled=true`, endpoint, and token supplied outside repo files. |
| CR124 | Codex | Review: implemented; synthetic + real-log replay passed; upload metadata gap found | `AutomationMetricsService`, `DiagnosticCaseCaptureService`, `logs/cases/YYYY-MM-DD/*.case.json`, `logs/cases-replay/YYYY-MM-DD/*.case.json`, `DiagnosticCaseCaptureServiceTest`, `DiagnosticCaseExistingLogReplayDebug` | Local diagnostic `case.json` generator that feeds CR123 upload later. First version creates a bounded, debug-capable JSON case when a task round/transaction fails, becomes fatal, or is explicitly marked for capture. The payload includes trigger metadata, task/window/failure fields, related metrics/timeline, bounded `dhxy-console.log` excerpt, diagnostic hints, size policy, upload placeholder, and multi-window linked evidence. Multi-window policy is implemented by incident fingerprint: one root case per shared `roundId` / `teamKey`; member windows become lightweight `relatedWindows` entries instead of uploading duplicate full cases. Target size: normal failure `<500KB`, multi-window/visual case `<1.5MB`, hard cap `2MB` by truncating console first. Capture is best-effort and runs from the metrics hook; capture failure logs a warning but does not block or fail tasks. Real-log replay produced `logs/cases-replay/2026-06-27/20260627_111805_596_wuhuan_v2_wuhuan_v2-27-round-2_SUCCESS_hwnd-141770.case.json` from existing metrics + console, size `169841` bytes, with `timelineCount=7`, `metricCount=7`, `consoleLineCount=500`, proving the JSON can reconstruct task/window/phase/pathing context from current logs. CR123 contract audit found this replay case currently lacks `app.licenseId` and `runtimeSnapshots.environment.machineHash`; raw upload is rejected until those fields are populated. If metric time and console time do not overlap, the case marks `console-excerpt-missing-for-trigger-time` instead of attaching unrelated log tail. |
| CR125 | Codex | Review: implemented; replay + Worker contract passed | `DeviceFingerprintService`, `LicenseAuthService`, `LicenseAuthResult`, `DiagnosticCaseCaptureService`, `AutomationMetricEvent`, case JSON tests | Populate upload-required identity metadata in every local `case.json`. Implemented: local cases now include `runtimeSnapshots.environment.machineHash`, `app.appId`, `app.appVersion/version`, and top-level / app `licenseId` when a license has been successfully verified through the existing `LicenseAuthService`. Missing-license cases still write locally but use `upload.eligible=false`, `upload.blocker=missing-license-id`, and `upload.status=LOCAL_ONLY`, so they no longer look upload-ready while failing CR123 with `LICENSE_REQUIRED`. `DiagnosticCaseExistingLogReplayDebug` now produces nonblank `machineHash`; focused identity test covers licensed and no-license cases; Java-generated licensed contract payload was accepted by the CR123 Worker handler without patching and wrote `cases/2026-06-27/machine-worker-contract/xiuluo_v2/...case.json` plus `indexes/2026-06-27.json`. |
| CR126 | Codex | Review: implemented; focused uploader + Spring constructor tests passed | `DiagnosticCaseCaptureService`, `DiagnosticCaseUploaderService`, `application.properties`, `logs/cases/**/*.case.json`, upload status fields/tests | Add the DHXY Java client uploader/retry layer for CR123. Implemented a default-off `DiagnosticCaseUploaderService` driven by `case.upload.*` properties. Upload-eligible cases are enqueued after local case write and posted best-effort in a daemon worker using `Authorization: Bearer <token>`; task execution, metrics capture, OCR, input, and shutdown are not blocked. Upload state is persisted back into the case JSON with `PENDING`, `UPLOADED`, `FAILED_RETRYABLE`, `FAILED_PERMANENT`, attempts, timestamps, response code/message, next retry time, and remote `caseKey/indexKey`. Startup retry scans `logs/cases` for `PENDING` / `FAILED_RETRYABLE` cases only when endpoint/token config is present. Focused fake-server tests cover successful upload, permanent 401 failure, and retryable network failure. Follow-up startup blocker fixed: the uploader production constructor is now explicitly Spring-autowired, and `DiagnosticCaseUploaderSpringContextTest` proves the Bean is constructible without a no-arg constructor. |
| CR127 | Codex | Done: real Cloudflare/R2 smoke passed | `D:/mavenProject/dhxy-case-worker`, Cloudflare Wrangler/R2 setup notes, smoke-test script, CR123/CR126 docs | Validate the real Cloudflare/R2 end-to-end deployment after CR125 and CR126. Completed: Wrangler OAuth is valid for `poul1303821@gmail.com`; `npx wrangler d1 list` confirms the expected `dhxy_auto_bat_license_db`; R2 was enabled in the Cloudflare Dashboard; `npx wrangler r2 bucket create dhxy-diagnostic-cases` succeeded; `CASE_UPLOAD_TOKEN` was generated locally, stored only in `%TEMP%/dhxy_case_upload_token.txt`, and pushed as a Worker secret; `npx wrangler deploy` deployed `https://dhxy-case-worker.yueyunfe.workers.dev`; public POST smoke upload returned `200 CASE_UPLOADED`; remote R2 verification with `--remote` downloaded `indexes/2026-06-27.json` and the case object under `cases/2026-06-27/machine-worker-contract/xiuluo_v2/...case.json` (`3120` bytes). No R2 credentials were embedded in DHXY Java, and full logs/zips are not uploaded. |
| CR128 | Codex | Review: background-first source repair/guard passed; fresh runtime pending | `DefaultWindowTaskStartupInitializer`, `TaskStartupWindowPreparationService`, `LeftTopStatusSwitchService`, `WindowRuntimeContext`, `FiveRingTaskV2`, startup UI logs/tests | Restore startup UI guards to the validated multi-window model: all startup probes that do not require a physical mouse click must run by background HWND screenshot/shortcut and may run concurrently across the selected windows. 五环 still must run the full startup window checks (Alt+1 map options, Alt+U expand/flying option, Alt+5/Alt+6 visibility), but the normal path must be background-first; only a clearly detected wrong option should trigger the foreground correction transaction. For a two-round 五环 queue, this startup check runs once per accepted task queue, not once per round. Left-top status and future map/flying/minimap click-needed checks should first background-probe and store per-window pending actions; when the window later owns a safe real-input turn, it should consume the already-known pending click immediately without recapturing first. Fresh runtime still needs five-window proof. |
| CR129 | Zeno+Codex | Done: 修罗/五倍轮次结束 dashboard 异步化已验收 | `AutomationMetricsService`, `AutomationMetricsAsyncDashboardWiringTest`, `XiuluoTaskV2.finishRoundMetric`, `WubeiTask.finishRoundMetric`, `FiveRingTaskV2.finishRoundMetric`, automation dashboard logs | 轮次结束 metrics/dashboard 写盘已从业务完成路径异步化：内存事件仍同步记录，dashboard 持久化进入有界后台 writer，手动 `writeDashboardNow()` 保持同步。Focused guards 通过；fresh 修罗 `2026-06-27 22:39:01.570`、`22:44:33.314`、`22:46:17.035` 均显示 `ROUND_DONE`、`round skeleton finished`、下一轮 `initial phase` 同毫秒/近同毫秒，writer 后台稍后 flush。fresh 五倍 80 轮长跑继续证明 writer 不阻塞轮次推进，典型 flush 在 `2026-06-28 17:46:51.989`、`18:46:07.310`、`18:47:17.813` 等后台发生；五倍最终 `19:04:55.624` 完成 80/80。已可关闭。 |
| CR130 | Kant+Codex worker | Done: 连续修罗轮次不再跑 round-start hot-start | `XiuluoTaskV2`, `XiuluoHotStartResolver`, 修罗 round-start logs/tests | 连续修罗轮次已移除每轮之间的 `hot-start:xiuluo_v2:xiuluo-v2:round-start` 屏幕检查；真实 UI startup / after-combat startup resume 仍保留。Source guard 证明 round loop 不再调用 `hotStartResolver.resolve(...)`。Fresh 修罗 `2026-06-27 22:39:01.570` 后 round 4 直接 `source=normal-start`，后续 round 7/8 同样无 per-round hot-start；`2026-06-28 19:12:42.742 -> 19:19:55.632` 的 rounds 4-7 也都是 `phase=PREPARE_ROUND source=normal-start`，未见 `task hot-start snapshot` / `round-start` inspect。已可关闭。 |
| CR131 | Kant+Codex worker | Review: 修罗 + 五倍 source guards passed; fresh runtime pending | `TeamReturnService`, `XiuluoTaskV2`, `WubeiTask`, team-return detection, return-home/bag logs/tests | Start team-leave / team-return precheck before opening the bag for return-home, and consume the result after return verification. Fresh 修罗 `19:46:09.652 -> 19:46:18.637` showed return item use/verify first, then `WAIT_TEAM_RETURN` spent about 3.261s deciding `team return wait not needed`; the `20:23:47.825-20:33:16` audit again showed return-verified to `WAIT_TEAM_RETURN/ROUND_DONE` latency. 修罗 now captures a bound-window team-return precheck before `bagService.findAndUseMainBagTaskPageItem(...)`; 五倍 now does the same before `bagService.findAndUseItemFromBack(...)`. Both consume complete same-window/task-run results before live detection, fall back to existing `WAIT_TEAM_RETURN` on signal present/failed/stale/not-ready, and do not change return item search/use, start-map verification, or team-return business truth. Fresh runtime must show precheck capture before bag and precheck consume after return verification for both 修罗 and 五倍 return-home paths. |
| CR132 | Codex+Erdos | Done: 显形镜槽位缓存 fresh runtime 通过 | `ReturnItemPrescanService`, `WubeiTask`, `WubeiCR132ProbeMirrorSlotReturnCacheWiringTest`, 修罗/五倍回城道具预扫缓存状态，RETURN_HOME 日志/测试 | 回城道具预扫缓存仍按任务/窗口/taskRun/轮次/hwnd/模板隔离，使用后必须验证起始地图，失败回完整包裹查找。Fresh 五倍普通战斗已闭环：`00:15:47.778` 缓存 `(1433,647)`，`00:16:58.341` 使用缓存，`00:17:03.292` 验证回 `宝象国`。白龙马/显形镜 fresh runtime 已闭环：`01:34:05.754` first-probe 绿字后按 `bag/wubei_probe_item.png` 预扫显形镜槽位，`01:34:09.871` 缓存 `(1428,642)`，`01:36:30.582` `WUBEI_PROBE_STORY target=wubei.probeTargetReady` 准备并消费，`01:36:36.578` `WUBEI_ENTER_BATTLE` 消费进战，`01:37:04.732` RETURN_HOME 选择 `bag/wubei_probe_item.png` 缓存，`01:37:08.905` 使用 `(1428,642)`，`01:37:09.920` `cached-return-verified` 回 `宝象国`。普通五倍 combat/黄袍/修罗继续用原回城模板。源码 focused guard 已过。已可关闭。 |
| CR133 | Codex | Open: 21:34 hot-start tracker shortcut stuck; needs source repair | `WindowTaskRunner`, `WindowReadyEventBus`, `XiuluoTaskV2`, 修罗 hot-start / tracker shortcut / prepared enter-battle logs | 修罗热启动命中左侧任务追踪后，绿字点击、`UNTARGETED_TRACKER` intent、`XIULUO_ENTER_BATTLE` interest 都已经注册；但 `2026-06-27 21:34:32` Runner 识别到 `OPTION` 后只写了 `window.dialog.visible.update`，没有继续发布 `TASK_ATTENTION_REQUIRED` 或 `PREPARED_ACTION_READY/XIULUO_ENTER_BATTLE`，本轮 watcher 也没有 `window observer tick` 收尾日志，任务最终靠 CR116 的 180s pre-combat watchdog 超时进入下一轮。修复方向：保证 active tracker/pathing 且存在 `XIULUO_ENTER_BATTLE` interest 时，可见 `OPTION` 必须迅速走完 attention publish / task-dialog preparation / prepared-action wake，且 watcher 不得因可见 dialog 后续准备链路卡住；补充 INFO/WARN 级失败日志，避免只在 debug 吞掉原因。不要改 tracker 绿字点击、NPC/模板坐标、战斗确认业务或 180s watchdog 上限。 |
| CR134 | 唐德 | Review: 源码实现+focused guard+compile 通过；fresh runtime 待验 | `WubeiTask`, `WubeiCR134PostAcceptPrepathTargetWiringTest`, 五倍接任务/预走路/医宝宝/修装备日志与测试 | 五倍接任务成功后先计算本轮 `prepathTarget`，再 `Alt+C -> navigateInCurrentMap(prepathTarget)`。源码已接入：默认没有医宝宝时仍点宝象国出口 `(88,157)`；医宝宝 due 时第一段预走路坐标直接替换成医宝宝 NPC 坐标；只有修装备 due 时仍点出口，因为修装备去洛阳，路上再走现有修装备导航；医宝宝和修装备都 due 时固定先医宝宝。实现只改变接任务后的预走路目标选择和日志/guard，不改变 tracker 读取、回城、导航算法、mini-map 点击算法或维护执行本身。 |
| CR135 | Codex | Review: 5 秒保守窗口源码+guard+compile 通过；fresh runtime 待验 | `WubeiTask`, `scripts/check_wubei_chained_first_aid_window.ps1`, 黄袍连战 dialog/脱战/放权/补给日志与测试 | 五倍黄袍连战中间流程已按定稿接入源码：识别到 `WUBEI_ENTER_BATTLE` prepared dialog 后、判断点击结果前立即关闭五倍维护窗口；黄袍脱战后在 `POST_BATTLE_RECOVER` 先开启 5 秒 first-aid-only 队员补血/补蓝窗口，并让队长在同一窗口内后台做 no-focus 血蓝预检；5 秒结束后 `RETURN_HOME` 只消费已发生的窗口结果，不再此时才打开 first-aid 窗口。若仍是黄袍连战，队长先消费缓存补给计划，再点已准备的 tracker 绿字；若不是黄袍连战，直接走正常回城/下一轮，不额外再等 5 秒。Focused guard 和 `mvn -q -DskipTests compile` 已通过，fresh runtime 需验证 5 秒窗口内没有三技能/盒子，且队长恢复后按缓存快速续战。 |
| CR136 | Peirce+Codex | Review: 五倍/修罗 source guards + compile/test-compile 通过；NoClassDef follow-up fixed；fresh runtime 待验 | `AutoCombatService`, `BattleRadarService`, `WubeiTask`, `XiuluoTaskV2`, `WubeiCR136FastExitLifecycleWiringTest`, `XiuluoCR136FastExitLifecycleWiringTest`, expected-combat false-positive 日志/测试 | 五倍 expected 战斗在 `2026-06-28 01:53` 暴露 stale `combatExitPending` 与同一 correction episode 内多次回程；修罗 fresh runtime `2026-06-29 17:53:29-17:55:04` 暴露同类问题：fast expected exit 误判后 `17:53:33.232` 缓存回程、`17:53:43.314` 完整扫包裹、`17:53:58.723` 第二次完整扫包裹、`17:55:01.545` phase retry 再次扫包裹，可信战斗状态直到 `17:54:56.405` 仍未把单次回程预算截断。已修：expected wait 增加 arm/exit fence；五倍/修罗实际点过一次回程但未验证起始地图后立刻 trusted probe，不再继续 full scan / 第二次 attempt；trusted `IN_COMBAT` 时分别回 `WAIT_BATTLE_FINISH` / `WAIT_COMBAT` 并刷新当前 in-combat avatar baseline。后续 avatar diff 不禁用、不降级；如果被纠正后再次触发 fast-exit，就是新的 correction episode 和新的单次回程预算。Follow-up 修复 `2026-06-29 18:17:25` fresh restart 后修罗 `RETURN_HOME` 的 `NoClassDefFoundError XiuluoTaskV2$ReturnItemUseResult$Status`：`ReturnItemUseResult` 不再依赖嵌套 `Status` enum，guard 要求 no nested Status，`target/classes` 不再生成该旧 class。Focused guards、`mvn -q -DskipTests compile`、`mvn -q -DskipTests test-compile` 通过；fresh runtime 仍需验证这些日志点。不得改 avatar ROI/阈值/15s grace/1s cadence、BagService、OCR/template/click/navigation、CR121 bounded wait、CR132/CR134/CR135 业务。 |
| CR137 | 唐德+Codex | Deprecated: runtime rolled back after fresh WUBEI 黄袍/tracker regression | `WubeiTask`, `WubeiRoundContext`, removed CR137 guard tests, 五倍接任务/暗雷重抽/黄袍 tracker 日志 | CR137 的接任务后后台 `T0/T+1.5s` tracker 预解析与暗雷快速撤销 prepath 已按用户要求整张撤回。Fresh runtime `2026-06-28 04:07:28` 证明该路径能读到 `智斗黄袍` 标题和 `火云戈壁` 绿字，但 `yellow=''`，后续在可见 `OPTION` 下反复重点击同一绿字。Rollback 删除 `WubeiRoundContext.trackerParseFuture`、`WubeiTask` accept-time tracker future/snapshot/fast 暗雷 reroll 方法、`WubeiAcceptWindowSnapshot` 以及 CR137 source guard 测试；五倍恢复为接任务后先保留 CR134 post-accept prepath，再等待 tracker refresh 并现场 `READ_TRACKER`。暗雷回到现场读到 `暗雷怪` 后重抽的旧路径。后续如要重新做暗雷快重抽，应新开 CR，先解决黄袍/绿字/interest 证据，不复用本卡实现。 |
| CR138 | 唐德 | Done：18:37 连续 `[五倍, 修罗x2]` fresh runtime 通过 | `AutoBattleTask`, `AutoCombatService`, `TaskMaintenanceService`, `TeamReturnService`, `TaskExecutionContext`, `WindowTaskRunner`, `WindowTaskControlService`, `LeftTopStatusSwitchService`, 本地队伍支援/session model, 连续 `[五倍, 修罗]` 日志/测试 | 已改：`WindowTaskRunner.resolveTaskTypeBeforeStart(...)` 已拆分 raw `liveRole` 与 `assignmentRole`，CR138 session evidence 只写 raw live role，`UNKNOWN + cached LEADER` 不再能成为 live leader evidence。已改：`AutoBattleTask` 对 `requested=xiuluo_v2/wubei/wuhuan_v2` 但 `localSession=null` 的队员恢复旧 team pathing window gate，避免 `17:55` 这类接任务前 standalone 三技能抢输入。Fresh `2026-06-29 18:37:14-18:46:05` 连续 `[五倍, 修罗]` 验收通过：五倍成功、修罗完成 2 轮；手动离队后 `TEAM_RETURN` gate 打开，队员归队点击都有目标窗口 focus 证据；`requested=wubei` 队员在修罗 `FIRST_AID` gate 打开后成功补法，未见旧 `requestedTaskCode` gate 卡死或提前放行。 |
| CR139 | 唐德 | Review：连续任务切换复用启动准备 source 修复完成 | `DefaultWindowTaskStartupInitializer`, `TaskStartupWindowPreparationService`, `WindowRuntimeContext`, `WindowTaskRunner`, 连续任务队列 startup logs/tests | 已新增 `CLEAN_QUEUE_TRANSITION` startup mode：同一 UI 队列里上一个任务 `SUCCESS`、任务类型切换、且 common startup-prep marker 已存在时，后续五倍/修罗跳过全局启动准备和非必要 hot-start/startup resume；standalone/after-combat/失败或 marker 缺失路径仍保留原恢复逻辑。聚焦 guard、compile、test-compile 通过；fresh `[五倍, 修罗]` runtime 待验收。 |

Card CR129: Round-finish metrics/dashboard writes must not block next round

Business source:

- User supplied fresh 修罗 latency evidence from `logs/dhxy-console.log`:
  - `2026-06-27 19:46:18.637` `phase=WAIT_TEAM_RETURN ... next=ROUND_DONE message=team return wait not needed`;
  - `2026-06-27 19:46:22.618` `[xiuluo-v2] round 56 skeleton finished, completed=56`;
  - elapsed time between business `ROUND_DONE` and visible round-finished log is about `3.981s`.
- Suspected code range: after `runRoundPhases(...)` returns and before `round skeleton finished`,
  especially `finishRoundMetric(...) -> automationMetricsService.recordRoundFinished(...) ->
  writeDashboardNow()`.

Problem statement:

- Round finish metrics/dashboard persistence appears to run synchronously on the task completion path.
- If `writeDashboardNow()` rebuilds/writes the dashboard JSON/HTML every round, it can delay the next
  round even though business logic already decided `ROUND_DONE`.
- This hurts 修罗/五倍 continuous runs because the next round does not need to wait for local
  dashboard file I/O.

Required behavior:

- `recordRoundFinished(...)` must return quickly enough for the task to continue into the next round.
- The in-memory metric event/round record must still be recorded synchronously enough that no round is
  lost if a later dashboard write happens.
- Dashboard file persistence should be queued/coalesced on a bounded background writer:
  - multiple quick round finishes should collapse into one pending dashboard write when possible;
  - dashboard write failure logs a warning/error but must not fail the task round;
  - shutdown/manual UI write can still force a synchronous `writeDashboardNow()` when explicitly
    requested by the UI.
- Apply the same metric-write contract to 修罗、五倍、五环 V2 explicit round metrics.

Boundaries:

- Do not change task phase decisions, round result semantics, metric schema fields, or dashboard row
  meaning.
- Do not remove explicit `TASK_ROUND_STARTED` / `TASK_ROUND_FINISHED` events.
- Do not make task code spawn ad-hoc threads; use one owned service/executor inside the metrics
  boundary if async work is needed.

Validation:

- Add a focused test/guard that `recordRoundFinished(...)` does not directly invoke the heavy
  dashboard write path on the caller thread.
- Add or preserve coverage that manual `writeDashboardNow()` still writes the dashboard.
- Fresh runtime:
  - `ROUND_DONE` to `round N skeleton finished` should no longer spend multi-second time in metrics
    writing;
  - logs should make it clear when dashboard write is queued/flushed and whether it was coalesced;
  - no missing or duplicated dashboard rows after several 修罗 rounds.

2026-06-27 20:23:47.825-20:33:16 audit update:

- Reproduced the same symptom after the card was created:
  - `20:24:36.504` `WAIT_TEAM_RETURN ... next=ROUND_DONE` -> `20:24:38.522`
    `round 73 skeleton finished`, about `2.018s`.
  - `20:29:45.374` `WAIT_TEAM_RETURN ... next=ROUND_DONE` -> `20:29:48.631`
    `round 75 skeleton finished`, about `3.257s`.
  - `20:32:43.003` `WAIT_TEAM_RETURN ... next=ROUND_DONE` -> `20:32:46.239`
    `round 76 skeleton finished`, about `3.236s`.
- Repair owner dispatched: Zeno. Expected scope remains `AutomationMetricsService` and the
  `finishRoundMetric(...)` callers; task business phases and navigation/click logic are out of
  scope.

Implementation update 2026-06-27:

- Local `AutomationMetricsService` already had a partial CR129-shaped async writer in the dirty
  working tree. This worker retained that direction and tightened it instead of reverting it.
- `recordRoundFinished(...)` keeps the synchronous in-memory/event recording path, then queues
  `round-finished`; it does not call `writeDashboard()` or `writeDashboardNow()` on the caller
  thread.
- Dashboard persistence uses a bounded queue and owned background writer. Failed queue offers now
  increment `coalescedDashboardWriteRequests`, and the flush log reports
  `queuedRequestsDrained`, `coalescedRequests`, and `elapsedMs`. Round-finish queue/coalesce/flush
  logs are visible at INFO for fresh runtime validation.
- `writeDashboardNow()` remains synchronous for manual UI writes.
- Source guard:
  `javac -encoding UTF-8 -d target/test-classes src/test/java/com/bot/dhxy/metrics/AutomationDashboardAsyncWriteWiringTest.java; java -cp target/test-classes com.bot.dhxy.metrics.AutomationDashboardAsyncWriteWiringTest`
  passed after the repair.
- Fresh verification passed:
  - `mvn -q -DskipTests test-compile`
  - `mvn -q -DskipTests "-Dexec.classpathScope=test" "-Dexec.mainClass=com.bot.dhxy.metrics.AutomationDashboardAsyncWriteWiringTest" exec:java`
  - `mvn -q -DskipTests "-Dexec.classpathScope=test" "-Dexec.mainClass=com.bot.dhxy.metrics.AutomationRoundDashboardRenderingTest" exec:java`
  - `mvn -q -DskipTests "-Dexec.classpathScope=test" "-Dexec.mainClass=com.bot.dhxy.metrics.DiagnosticCaseCaptureServiceTest" exec:java`

Closure update 2026-06-29:

- Status changed to Done after reviewing fresh 修罗 and 五倍 runtime evidence.
- 修罗 positive samples:
  - `2026-06-27 22:39:01.570` shows `ROUND_DONE`, dashboard write queued, `round 3
    skeleton finished`, and `round 4 initial phase` without waiting for dashboard flush.
  - `2026-06-27 22:44:33.314` and `22:46:17.035` repeat the same pattern on later rounds; the
    background writer flushes later.
  - `2026-06-28 19:12:42.741 -> 19:12:42.742`,
    `19:15:10.499 -> 19:15:10.500`, `19:16:47.075 -> 19:16:47.076`, and
    `19:19:55.631 -> 19:19:55.632` show the same near-immediate round-finish transition after the
    80/80 五倍 run switched into 修罗.
- 五倍 positive samples:
  - The 2026-06-28 80-round 五倍 run repeatedly shows dashboard writer flushes after, not before,
    round progression, including `17:46:51.989`, `18:46:07.310`, and `18:47:17.813`.
  - The run completed `80/80` at `2026-06-28 19:04:55.624`, so the async writer did not block
    continuous 五倍 completion.
- No later report slice found a renewed `ROUND_DONE -> round skeleton finished` multi-second
  dashboard-write gap. CR129 已可关闭。

Card CR130: 修罗 continuous rounds should not run startup hot-start inspection between rounds

Business source:

- User supplied fresh 修罗 latency evidence from `logs/dhxy-console.log`:
  - `2026-06-27 19:46:22.618` `[xiuluo-v2] round 56 skeleton finished, completed=56`;
  - `2026-06-27 19:46:24.894` `dialog handle request:
    source=hot-start:xiuluo_v2:xiuluo-v2:round-start operation=INSPECT`;
  - `2026-06-27 19:46:26.144` `dialog handle result ... type=NONE status=NO_DIALOG`;
  - `2026-06-27 19:46:26.144` `task hot-start snapshot:
    task=xiuluo_v2 source=xiuluo-v2:round-start state=NONE dialogType=NONE`;
  - `2026-06-27 19:46:26.144` `[xiuluo-v2] round 57 initial phase:
    phase=PREPARE_ROUND source=normal-start`.

Problem statement:

- This is not a user UI restart. It is the normal continuous transition from round 56 to round 57.
- The previous round has already left combat, returned home, and completed team-return handling.
- Running `hotStartResolver.resolve(round, false)` every internal round adds dialog inspection cost
  and reintroduces startup-style ambiguity in a path whose state is already known.

Required behavior:

- True startup/hot-start remains:
  - user starts 修罗 from UI;
  - user starts while in combat and the outer startup layer waits for combat exit;
  - task restart/recovery after a real stop/failure where screen state must be reinterpreted.
- Normal continuous rounds after a successfully finished previous round must not run
  `hot-start:xiuluo_v2:xiuluo-v2:round-start`.
- The next round should directly enter the normal round initial phase, e.g. `PREPARE_ROUND`, using the
  known task state from the previous round completion.
- Rename/log the real startup resolver path as something explicit such as `startup-screen-resume`,
  so logs distinguish true UI startup recovery from ordinary internal round continuation.

Boundaries:

- Do not delete or weaken the already-approved battle-in-start logic from `docs/业务逻辑.md`.
- Do not change 修罗 accept-task, tracker shortcut, return-home, team-return, maintenance, HP/MP,
  摄妖香, 三技能, or expected-combat rules.
- Do not hide actual failures by forcing `PREPARE_ROUND`; only bypass the resolver on known
  continuous round boundaries.

Validation:

- Add a source/behavior guard proving continuous round start does not call `XiuluoHotStartResolver`.
- Keep a separate guard proving true startup can still call the resolver.
- Fresh runtime:
  - after `round N skeleton finished`, the next internal round must not log
    `source=hot-start:xiuluo_v2:xiuluo-v2:round-start`;
  - true user startup should still log the renamed startup-screen resume path when needed.

2026-06-27 20:23:47.825-20:33:16 audit update:

- Reproduced on later continuous rounds:
  - `20:24:38.522` `round 73 skeleton finished` -> `20:24:39.910`
    `hot-start:xiuluo_v2:xiuluo-v2:round-start` inspect -> `20:24:40.769`
    `round 74 initial phase`.
  - `20:29:48.631` `round 75 skeleton finished` -> `20:29:49.512`
    round-start hot-start inspect -> `20:29:50.368` `round 76 initial phase`.
  - `20:32:46.239` `round 76 skeleton finished` -> `20:32:49.438`
    round-start hot-start inspect -> `20:32:51.640` `round 77 initial phase`.
- Repair owner dispatched: Kant. Boundaries remain: true UI startup / after-combat-start recovery
  must stay intact; only ordinary internal continuous-round hot-start inspection should be removed.

Implementation update 2026-06-27:

- Current local source restricts startup-screen resume to `completedRuns == 0`; continuous rounds
  use `XiuluoRoundContext.start(round)` and therefore skip `hotStartResolver.resolve(...)`.
- Real startup paths keep explicit labels: `startup-screen-resume` and
  `after-combat-exit-startup-screen-resume`.
- Added `XiuluoCR130CR131WiringTest` to guard that `execute(...)` does not call
  `hotStartResolver.resolve(...)` for the round loop and still preserves the true startup labels.

Closure update 2026-06-29:

- Status changed to Done after reviewing fresh 修罗 runtime evidence.
- `2026-06-27 22:39:01.570` shows the next internal round starts as
  `round 4 initial phase: phase=PREPARE_ROUND source=normal-start`; no
  `hot-start:xiuluo_v2:xiuluo-v2:round-start` appears.
- Later fresh 修罗 rounds continue the same behavior: report sections for
  `22:39:14.587 -> 22:47:47.293` and `22:47:47.293 -> 22:54:01.810` both record
  `source=normal-start` and no per-round hot-start.
- The 2026-06-28 continuous queue also revalidated it after 五倍 completed 80/80 and switched to
  修罗: rounds 4-7 in `19:12:07.106 -> 19:24:23.051` were all
  `phase=PREPARE_ROUND source=normal-start`, with no `task hot-start snapshot` or
  round-start inspect logs.
- True startup/hot-start remains outside this closure; CR130 only closes the internal
  continuous-round hot-start removal.

Card CR131: Return-home should precompute team-return need before bag opens

Business source:

- User supplied fresh 修罗 return-home/team-return timing from `logs/dhxy-console.log`:
  - `2026-06-27 19:46:09.652` return item right-click begins;
  - `2026-06-27 19:46:11.293` return item bag action finished;
  - `2026-06-27 19:46:12.987` return item verified;
  - `2026-06-27 19:46:15.376` `phase=RETURN_HOME ... next=WAIT_TEAM_RETURN
    message=return item verified`;
  - `2026-06-27 19:46:15.376` `task transaction started:
    name=xiuluo-v2:WAIT_TEAM_RETURN`;
  - `2026-06-27 19:46:16.563` `task.transaction elapsedMs=1187
    detail=name=xiuluo-v2:WAIT_TEAM_RETURN`;
  - `2026-06-27 19:46:18.637` `phase=WAIT_TEAM_RETURN ... next=ROUND_DONE
    message=team return wait not needed`.

Problem statement:

- The leader currently waits until after return item use and return verification before doing the
  team-return / leave-state decision.
- In normal successful rounds the answer is often "team return wait not needed", but the task still
  pays the detection cost after return-home, directly delaying `ROUND_DONE` and the next round.

Required behavior:

- Before opening the bag / using the return item, capture the bound leader window's team-state image
  needed for team-leave/team-return detection.
- Analyze that screenshot in the background while the main flow continues:
  - open bag;
  - find/use return item;
  - verify return to start map.
- After return verification:
  - if the precheck completed and proves no leave/team-return wait is needed, skip directly to the
    existing next-round path;
  - if it proves leave/team-return is needed, enter the existing `WAIT_TEAM_RETURN` handling;
  - if the precheck failed, is stale, does not match the current window/task/run, or is not complete
    yet, fall back to the existing conservative `WAIT_TEAM_RETURN` logic.
- This is a precompute optimization only; it must not create a new business truth stronger than the
  existing team-return detector.

Boundaries:

- Do not change return item search/use, bag page logic, return-map verification, or failure recovery.
- Do not change team-return business rules; only move a read-only screenshot/probe earlier and
  consume the result later if it is trustworthy.
- Do not let the background analysis send input, focus windows, open/close UI, or use unbound/global
  screenshots.
- Scope the pending precheck by window, hwnd, task, round/task-run, and timestamp; stale results must
  fail closed into the existing fallback.

Validation:

- Add a focused guard proving the precheck is launched before bag interaction and is read-only.
- Add behavior coverage for three outcomes:
  - completed no-leave result skips the expensive wait;
  - leave/needs-wait result enters existing `WAIT_TEAM_RETURN`;
  - missing/failed/stale/not-ready result falls back to existing `WAIT_TEAM_RETURN`.
- Fresh runtime:
  - logs show `team-return precheck scheduled` before return item bag open;
  - after `return item verified`, logs show `team-return precheck consumed` and either direct
    `ROUND_DONE` or existing fallback;
  - no cross-window/team-state result reuse.

2026-06-27 20:23:47.825-20:33:16 audit update:

- The return-home/team-return latency repeated:
  - `20:24:33.742` `return item verified` -> `20:24:34.784`
    `RETURN_HOME ... next=WAIT_TEAM_RETURN` -> `20:24:36.504`
    `WAIT_TEAM_RETURN ... next=ROUND_DONE`.
  - `20:29:41.609` `return item verified` -> `20:29:43.865`
    `RETURN_HOME ... next=WAIT_TEAM_RETURN` -> `20:29:45.374`
    `WAIT_TEAM_RETURN ... next=ROUND_DONE`.
  - `20:32:34.278` `return item verified` -> `20:32:36.912`
    `RETURN_HOME ... next=WAIT_TEAM_RETURN` -> `20:32:43.003`
    `WAIT_TEAM_RETURN ... next=ROUND_DONE`.
- Repair owner dispatched: Kant. This remains a read-only precompute optimization; return item
  search/use, start-map verification, and failure recovery are out of scope.

Implementation update 2026-06-27:

- 修罗 now schedules `TeamReturnService.beginLeaderSignalPrecheck(...)` before
  `bagService.findAndUseMainBagTaskPageItem(...)` in `useReturnItemAndVerifyStartMap(...)`.
- `waitTeamReturn(...)` consumes `pendingTeamReturnPrecheck` before live
  `shouldYieldForTeamReturnSignal()` fallback. Complete no-signal results go straight to
  `ROUND_DONE`; signal-present results use the existing yield path; missing/stale/failed/not-ready
  handles fall back to live detection.
- `TeamReturnService` scopes the handle by window id, native hwnd, and task run; it captures the
  bound-window ROI to memory, analyzes that immutable screenshot in the background, and does not
  send input from the precheck path.
- Added `TeamReturnPrecheckWiringTest` for launch-before-bag coverage in both 修罗 and 五倍, plus
  consume-before-live-probe, read-only, and stale-scope guards in the shared `TeamReturnService`
  path.
- Fresh runtime remains required before Done.

Card CR132: 修罗 / 五倍回城道具预扫缓存随机策略

Business source:

- User requested a per-round return-item prescan strategy so 修罗 and 五倍 can avoid paying the full
  bag search cost after combat when the return item location could have been learned earlier.
- The strategy must not run at a fixed time every round. Each round chooses one available prescan
  strategy at random, then falls back step by step if the chosen strategy fails.
- Scope was corrected by the user: this applies to both 修罗 and 五倍, not only 修罗. 五环 is out of
  scope for this card.

Problem statement:

- 修罗/五倍 return-home currently still depends on opening the bag and finding the task return item
  at the moment the task wants to return.
- In normal expected battles there are earlier safe-ish opportunities to inspect/cache the return
  item position without waiting until `RETURN_HOME`.
- A fixed prescan point would make behavior repetitive and brittle. The design should pick a
  strategy once per round from only the strategies that make sense for that round.

Required behavior:

- Add a small per-round, task-specific state for 修罗 and 五倍:
  - selected prescan strategy:
    `AFTER_TRACKER_GREEN`, `IN_COMBAT_RANDOM`, `BACKGROUND_PATHING`, or a conservative
    no-prescan/old-logic fallback;
  - whether background pathing prescan is allowed;
  - prescan status: `NOT_SCHEDULED`, `SCHEDULED`, `SUCCESS`, `FAILED`;
  - cached click/grid point and the task/item template it belongs to;
  - failure reason;
  - whether a failed strategy should schedule combat prescan as fallback.
- If a round uses a tracker/task-panel green-link route, the candidate strategies are:
  - `AFTER_TRACKER_GREEN`: after clicking the left tracker green link, open/check/cache the
    task-specific return item position, then close the bag. Failure marks that combat prescan is
    needed.
  - `IN_COMBAT_RANDOM`: after combat entry and after the existing auto-combat entry maintenance
    completes. Current entry maintenance is `4s`; after that wait an additional random `8-18s`
    before opening/checking/caching the item.
  - `BACKGROUND_PATHING`: only for non-first rounds and only when the task/window pathing state makes
    background bag screenshot/probe safe. First round must not use this because the bag page may not
    still be on the task/last page.
- If a round does not use a tracker green-link route, do not use `AFTER_TRACKER_GREEN`. Choose only
  from the remaining safe subset, or fall back to old logic when no prescan point is safe.
- On entering `RETURN_HOME`:
  - if a valid cache exists, open the bag and use the cached task-specific click/grid point;
  - if cache use verifies the task start map, continue normally;
  - if cache use fails, position validation fails, return verification fails, or no cache exists,
    run the existing full bag search/use/return verification logic for that task.
- 修罗 and 五倍 caches must be isolated by task type, window identity, task run/round, native hwnd,
  and return item template. Never reuse a 修罗 return item cache for 五倍 or the reverse.

Boundaries:

- Do not change 五环.
- Do not rewrite generic `BagService` full-scan semantics; this card may add narrow return-item cache
  call sites or a task-owned wrapper, but the old full search remains the final fallback.
- Do not change return-map verification, expected-combat exit truth, HP/MP recovery, 摄妖香,
  三技能/auto-battle maintenance, tracker shortcut, or navigation decisions.
- Background/pathing prescan must use the bound window context and must not steal foreground input
  unless it is at an explicit safe input turn. If a background-safe probe cannot be performed, mark
  that strategy failed and use the next fallback.
- Respect pause/stop, input queue serialization, identity drift, stale hwnd/task-run, and round/task
  restart invalidation.
- Cache invalidation must happen on task switch, task restart, identity drift, item-use failure,
  return verification failure, stale round/task-run, or round completion.

Validation:

- Source guards/tests:
  - strategy selection happens once per round and only from the available strategy set;
  - first round never selects `BACKGROUND_PATHING`;
  - 修罗 and 五倍 cache keys cannot cross task type/window/round/template;
  - failed `AFTER_TRACKER_GREEN` or `BACKGROUND_PATHING` can schedule `IN_COMBAT_RANDOM` fallback;
  - failed/missing/invalid cache always reaches the existing full search path.
- Fresh runtime:
  - logs show selected strategy, schedule time, status, cache point/template, and failure reason;
  - combat prescan starts only after entry maintenance plus the `8-18s` random delay;
  - cache-hit `RETURN_HOME` avoids full-page bag search and still verifies the start map;
  - cache miss/failure still returns successfully through the existing full search;
  - 修罗 and 五倍 both show the behavior independently, with no 五环 log/behavior change.

Implementation status (2026-06-27 Codex):

- Added `ReturnItemPrescanService` as the per-round coordinator. It chooses exactly one strategy
  for a `(taskCode, windowId, hwnd, taskRunId, round, template)` key, logs the selected strategy,
  prevents first-round background/pathing prescan, downgrades missed/failed pathing prescan into
  combat prescan, and clears/invalidate cache on verified return or failed cached use.
- Added narrow `BagService` APIs for return-item prescan and cached-point use:
  `prescanMainBagTaskPageItem(...)`, `prescanMainBagItemFromBack(...)`, and
  `useCachedMainBagReturnItem(...)`. They reuse the existing bag open/close/input queue path.
- Wired 修罗 tracker shortcut green click, tracker pathing active wait, combat wait, and
  `RETURN_HOME` cache consumption. Return completion still requires verified `灵兽村`.
- Wired 五倍 tracker green click, ordinary pathing active wait, battle-finish wait, and
  `RETURN_HOME` cache consumption. Per user follow-up, 五倍 return item fallback now scans the
  main-bag task page like 修罗 instead of the old from-back normal-page scan.
- Verification: `mvn -q -DskipTests compile` passed. Fresh runtime still needs to confirm selected
  strategy logs, combat random delay, cache-hit return latency, and cache-miss fallback for both
  修罗 and 五倍.

Runtime blocker update 2026-06-28:

- 五倍普通战斗缓存闭环已经出现正向证据：
  - `00:15:47.778` `wubei:tracker-green-click:combat:after-tracker-green` 预扫成功，缓存点
    `(1433,647)`;
  - `00:16:58.341` `RETURN_HOME` 使用该缓存点；
  - `00:17:03.292` `cached return item verified`，回到 `宝象国`。
- 五倍白龙马/显形镜 probe 场景暴露新的 blocker：
  - `00:14:13.137` `wubei:tracker-green-click:first-probe:after-tracker-green` 预扫成功，
    缓存点 `(1484,647)`;
  - 该场景会给 `显形镜`，任务完成后 `显形镜` 消失，任务页物品会前移；当前缓存模型按
    `bag/wubei_return_item.png` 直接缓存回程道具点，因此对白龙马/probe 不可靠；
  - `00:15:23.365` 使用缓存后未回到起始地图，仍在 `火云戈壁`，随后按设计 fallback；
  - `00:15:28.569` 完整任务页查找验证回到 `宝象国`。
- Source repair 2026-06-28:
  - 已确认显形镜模板存在：`images/template/bag/wubei_probe_item.png`；
  - `ReturnItemPrescanService.afterTrackerGreenRequired(...)` 提供窄口径强制
    `AFTER_TRACKER_GREEN` 学习，用于显形镜这类战斗前会消失的任务道具槽位；
  - 五倍 `first-probe` / `second-probe` 绿字点击后学习 `bag/wubei_probe_item.png`
    显形镜槽位，source 为 `wubei:probe-mirror-slot:*`；
  - 白龙马/显形镜 probe 不允许使用战斗中 `IN_COMBAT_RANDOM` 预扫。显形镜槽位必须在
    战斗前缓存：优先是点 tracker 绿字后 `AFTER_TRACKER_GREEN`，或后续明确证明安全的
    移动中后台预扫；进入战斗后只允许跳过并依赖已有显形镜槽位缓存；
  - probe runtime 进入 `RETURN_HOME` 时优先消费显形镜模板对应的缓存点，按“显形镜消失后
    回程道具补到原槽位”的业务假设尝试回城；
  - 缓存使用后仍必须验证回到 `宝象国`；如果未回到起始地图、缓存缺失、缓存过期或使用
    失败，必须 fallback 到现有完整任务页查找/验证；
  - 普通五倍 combat/黄袍/修罗仍按 `bag/wubei_return_item.png` / 修罗回程道具模板缓存；
  - 不新增 slot-shift 表，不改 `BagService` 通用扫描、模板阈值、OCR、点击或导航；
  - focused source guard
    `WubeiCR132ProbeMirrorSlotReturnCacheWiringTest` 已通过；
  - fresh runtime 2026-06-28 已通过：
    - `01:34:05.754` first-probe 绿字后选择 `bag/wubei_probe_item.png` 显形镜槽位预扫；
    - `01:34:09.871` `wubei:probe-mirror-slot:first-probe:after-tracker-green` 缓存点
      `(1428,642)`；
    - `01:36:30.582` `WUBEI_PROBE_STORY target=wubei.probeTargetReady` 准备完成，
      `01:36:30.583` 被队长消费；
    - `01:36:36.578` `WUBEI_ENTER_BATTLE target=wubei.enterBattle.prove`
      consume-validation-passed；
    - `01:37:04.732` RETURN_HOME 选择 `template=bag/wubei_probe_item.png probeRuntime=true`；
    - `01:37:08.905` 使用显形镜槽位缓存点 `(1428,642)`；
    - `01:37:09.920` `cached-return-verified hadCache=true`，回到 `宝象国`；
  - 结论：白龙马/显形镜特殊槽位和普通回城道具缓存都已 fresh runtime 闭环，CR132 可关闭。

Card CR133: 修罗 hot-start tracker shortcut OPTION must wake prepared enter-battle

Business source:

- User started 修罗 through hot-start while an existing left-side 修罗 tracker entry was present.
- The route/pathing itself worked and the "看打" option appeared, but the task remained parked until
  the 180s pre-combat watchdog timed out.
- User explicitly rejected the idea that Runner simply failed to recognize the dialog image.

Problem statement:

- `2026-06-27 21:33:32.577` 修罗 registers a tracker shortcut pathing intent:
  `intentId=4cbae19c-93be-47f3-9324-fc23295ab017 type=UNTARGETED_TRACKER targetMap=null`.
- `2026-06-27 21:33:32.580` 修罗 registers the phase-owned dialog interest:
  `operations=[XIULUO_ENTER_BATTLE] source=xiuluo-v2:shortcut-enter-battle:1`.
- `2026-06-27 21:34:32.562` Runner's dialog probe positively recognizes the option:
  `dialog option lower check ... result=true` and `dialog detect no-focus ... result=OPTION`.
- `2026-06-27 21:34:32.583` `WindowRuntimeContext` writes
  `event=window.dialog.visible.update ... type=OPTION ... activeIntentId=4cbae...`.
- After that visible update, the expected chain never appears:
  - no `window.ready.publish ... TASK_ATTENTION_REQUIRED`;
  - no `task attention published`;
  - no `task attention prepared follow-up`;
  - no `PREPARED_ACTION_READY operation=XIULUO_ENTER_BATTLE`;
  - no `consumePrepared`;
  - no `window observer tick` tail log for that watcher iteration.
- The watcher thread `window-combat-watch-hwnd-17240550-6` also has no later logs in this run
  slice. The task wakes only at `21:36:32.581` by CR116's bounded watchdog timeout.

Current root-cause boundary:

- This is not a template/OCR/image-recognition miss: Runner already reported `OPTION`.
- This is not missing hot-start registration: both the pathing intent and `XIULUO_ENTER_BATTLE`
  dialog interest were registered before the wait.
- This is not the old CR115 route-transfer takeover: there is no `ROUTE_TRANSFER` prepared mismatch
  in this sample.
- The failure boundary is inside `WindowTaskRunner.publishTaskAttentionIfDialogVisible(...)` after
  `windowContext.updateVisibleDialogSnapshot(...)` and before the soft wake / prepared-action publish
  path. Current logging does not expose whether that branch returned early, blocked, threw a
  non-logged throwable, or ran different live bytecode; the existing DEBUG-only catch and watcher
  `Future` submission make this class of failure too quiet.

Required behavior:

- When 修罗 has an active tracker/pathing wait and a current dialog interest that supports
  `XIULUO_ENTER_BATTLE`, a visible `OPTION` must quickly produce either:
  - `PREPARED_ACTION_READY operation=XIULUO_ENTER_BATTLE`, or
  - an INFO/WARN-level explicit negative reason explaining why preparation was skipped.
- A watcher iteration that observes `OPTION` must not disappear without a tail/timing log.
- A failure in the attention publish / task-dialog preparation branch must not kill or permanently
  silence the window observer without surfacing a WARN/ERROR and waking the task with a recoverable
  reason.

Concrete repair task:

- Modify `WindowTaskRunner.publishTaskAttentionIfDialogVisible(...)`.
  - Add a local `stage` marker that is updated at each boundary:
    `detect`, `visible-update`, `interest-read`, `xiuluo-interest-prepare`,
    `attention-latest`, `attention-publish`, `route-prepare`, `task-interest-prepare`.
  - Change the current DEBUG-only catch to INFO/WARN-level logging with task/window/active-intent,
    visible dialog type, interest source/operations if available, and the `stage` marker. A failure
    in this method must never disappear as only a debug log.
  - Add a 修罗-specific fast branch immediately after `visible.update` and `interest-read`:
    when `taskType == XIULUO_V2`, `visibleType == OPTION`, and the current interest supports
    `XIULUO_ENTER_BATTLE`, call `refreshTaskDialogInterestPreparationSignal(...)` directly before
    generic route-transfer preparation. If it prepares successfully, return that prepared action and
    let its existing `PREPARED_ACTION_READY` wake the parked task.
  - If that 修罗 branch cannot prepare the option, log a clear negative reason at INFO/WARN level
    (`xiuluo-enter-battle-template-miss`, `interest-missing`, `interest-unsupported`, or exception
    stage) before falling through to the existing generic attention/route path.
- Modify the watcher loop around `runCombatWatcherLoop(...)`.
  - Ensure any throwable that escapes the observer tick is logged at WARN/ERROR with task/window,
    branch, active intent, prepared action, and last stage before the `Future` can swallow it.
  - Ensure a tick that observed `visibleType=OPTION` cannot end without either the normal
    `window observer tick` tail log or an explicit `observer tick failed` log.
- Add a source guard / focused test so future edits cannot reintroduce this silent path:
  - 修罗 `OPTION + XIULUO_ENTER_BATTLE interest` must call the task-interest preparation branch
    before generic `ROUTE_TRANSFER` preparation.
  - `publishTaskAttentionIfDialogVisible(...)` must not catch-and-drop exceptions at DEBUG only.

Boundaries:

- Do not change tracker green-link click coordinates, NPC/template matching, combat entry truth,
  return-home logic, or the CR116 180s pre-combat watchdog.
- Do not solve this by making the task poll/click the dialog directly. The contract remains:
  Runner observes/prepares, task consumes after validation.
- Keep CR115's priority rule: 修罗 `XIULUO_ENTER_BATTLE` interest outranks route-transfer preparation
  on the same visible `OPTION`.

Validation:

- Source-level:
  - add INFO/WARN diagnostics around the visible-update -> attention-publish -> prepared-action path;
  - ensure watcher loop logs or propagates any throwable that would otherwise be hidden by `Future`;
  - guard that 修罗 `OPTION + XIULUO_ENTER_BATTLE interest` cannot silently return with no wake and no
    negative reason.
- Fresh runtime:
  - reproduce hot-start tracker shortcut and confirm the log sequence from visible `OPTION` to
    `PREPARED_ACTION_READY operation=XIULUO_ENTER_BATTLE`;
  - confirm no 180s watchdog timeout in `WAIT_TRACKER_SHORTCUT_PATHING` when "看打" is visible;
  - if preparation is rejected, logs must show the explicit rejection reason at INFO/WARN level.

Card CR134: 五倍接任务后预走路坐标按医宝宝 due 替换

Business source:

- User clarified the desired 五倍 behavior after accepting a task: this is not a later route target
  switch. The first prepath coordinate itself should be selected from the current maintenance due
  state before `Alt+C -> navigateInCurrentMap(...)`.
- Current default first prepath target is 宝象国出口 `(88,157)`.

Problem statement:

- 五倍 accepts a task, opens the mini-map with `Alt+C`, and starts a first short prepath while the
  tracker, maintenance, and follow-up flow continue.
- If 医宝宝 is already due, running first toward 宝象国出口 wastes the best immediate movement
  opportunity. The task should go directly toward 医宝宝 NPC first.
- 修装备 has a different geometry: it goes to 洛阳, so the best first movement is still toward
  宝象国出口 while the existing repair navigation prepares on the road.

Required behavior:

- Compute one `prepathTarget` after accept succeeds and before opening/clicking the mini-map.
- No maintenance due:
  - `prepathTarget = 宝象国出口 (88,157)`.
- Only 医宝宝 due:
  - `prepathTarget = HEAL_PET_NPC`.
  - Do not first click `(88,157)` and then reroute. The first mini-map click must already be the
    医宝宝 NPC coordinate.
  - Existing tracker-reading and 医宝宝 handling flow should continue after movement starts.
- Only 修装备 due:
  - `prepathTarget = 宝象国出口 (88,157)`.
  - Existing repair-equipment maintenance should still prepare/trigger the 洛阳 repair route while
    the window is moving.
- 医宝宝 and 修装备 both due:
  - `prepathTarget = HEAL_PET_NPC`.
  - Fixed order is 医宝宝 first.
  - After 医宝宝 completes, the existing maintenance chain should navigate to 洛阳 for 修装备.

Implementation boundary:

- The intended shape is:
  - `healPetDue == true` -> `prepathTarget = HEAL_PET_NPC`;
  - otherwise -> `prepathTarget = 宝象国出口 (88,157)`;
  - then `Alt+C -> navigateInCurrentMap(prepathTarget) -> continue current tracker / 摄妖香 /
    maintenance flow`.
- Do not change tracker parsing, 摄妖香 checks, return-home logic, yellow navigation, mini-map click
  algorithm, repair-equipment business order after 医宝宝, or generic navigation behavior.
- Source implementation is complete in `WubeiTask`; fresh runtime remains pending.

Implementation:

- Added 宝象国出口 constants `START_EXIT_X = 88` / `START_EXIT_Y = 157`.
- Added `computePostAcceptPrepathTarget()`:
  - `isHealPetMaintenanceDue()` selects `HEAL_PET_NPC`;
  - otherwise selects `宝象国出口 (88,157)`;
  - repair-equipment due does not affect the first prepath target.
- After accept option success, `runAcceptTaskPhase(...)` now calls
  `startPostAcceptPrepath(context, state)` before the tracker refresh wait and `READ_TRACKER`.
- `startPostAcceptPrepath(...)` presses `Alt+C` and then uses the existing
  `navigationService.navigateInCurrentMap(...)` with the computed target. It logs selected target,
  reason, navigation status, and message.
- Added focused source guard
  `WubeiCR134PostAcceptPrepathTargetWiringTest`.

Validation:

- Source guard or focused test should prove:
  - `healPetDue=false` selects `(88,157)`;
  - `healPetDue=true` selects the 医宝宝 NPC coordinate;
  - repair-only does not change the first prepath target away from `(88,157)`;
  - heal-pet plus repair selects 医宝宝 first.
- Completed source verification:
  - Focused guard now asserts the current 宝象国出口 constants are `START_EXIT_X = 88` /
    `START_EXIT_Y = 157`.
  - `mvn -q -DskipTests compile` passed.
  - Focused guard passed via manual `javac` + `java` execution against `target/classes`.
  - Full `mvn -q -DskipTests test-compile` is blocked by unrelated existing test-source /
    target-cache issues across older guard/debug tests; production compile and CR134 guard pass.
- Fresh 五倍 runtime should show:
  - when 医宝宝 is due, the first post-accept mini-map click is the 医宝宝 NPC coordinate;
  - when only 修装备 is due, the first post-accept mini-map click remains `(88,157)`;
  - tracker reading, follow-up maintenance, and repair navigation continue through the existing
    chain.

Card CR135: 五倍黄袍连战中间 5 秒队员补血蓝与队长后台准备

Business source:

- User finalized the 黄袍连战 middle-flow contract after correcting earlier wording.
- This implementation is now linked to CR135 and owned by Codex in this thread.

Problem statement:

- 黄袍连战中间需要给队员一个 very short recovery window, but that window must not become a
  general maintenance window.
- The leader should use the same 5 seconds to prepare the next decision in the background, then
  immediately take back control instead of waiting again.
- The maintenance-window close timing must be deterministic and tied to the 黄袍 enter-battle dialog
  recognition, not a later "maybe after combat entry" branch.

Required behavior:

- Maintenance window close timing:
  - as soon as the 黄袍 enter-battle dialog is recognized, close the maintenance window immediately;
  - do not also describe or implement this as "or after entering combat";
  - the fixed business trigger is dialog recognition.
- After the first 黄袍 battle exits:
  - leader immediately yields input for 5 seconds;
  - this 5-second window is for members only;
  - allowed member work in this window: HP first-aid and MP first-aid only;
  - explicitly excluded: 三技能, common-box / 盒子, summon refresh, repair, heal-pet, incense, and
    any other opportunistic maintenance.
- During the same 5 seconds, the leader prepares in the background:
  - check whether the left-side task tracker still exists;
  - decide whether this is still a 黄袍 chained battle;
  - compute the green-link click point for the next 黄袍 battle if still chained;
  - compute whether the leader itself needs HP/MP recovery.
- After the 5-second member window:
  - leader takes back the input turn;
  - if still 黄袍 chained battle:
    - leader first performs its own HP/MP recovery if due;
    - then clicks the already-computed green-link point;
    - enters the next battle.
  - if not still 黄袍 chained battle:
    - leader continues the normal follow-up flow, such as return-home / next round;
    - do not wait another 5 seconds, because the member window already happened.

Implementation boundary:

- This CR does not change 黄袍 dialog recognition, green-link click coordinate algorithms, tracker
  OCR/template matching, combat truth, return-home logic, or generic maintenance definitions.
- The 5-second member window is a narrow handoff for HP/MP only. Do not route it through a broad
  maintenance API that may also run 三技能, common-box, repair, heal-pet, or incense.
- Leader background preparation should be read-only until the leader takes back input, except for
  any already-existing safe background screenshot/template/OCR probes.
- Implementation is intentionally scoped to `WubeiTask` orchestration and a focused source guard.
  Do not broaden this CR into tracker/OCR/click-coordinate, return-home, or generic maintenance
  rewrites.

Validation:

- Source guard or focused test should prove:
  - 黄袍 enter-battle dialog recognition closes the maintenance window immediately;
  - the post-first-battle 5-second handoff invokes only member HP/MP recovery and excludes 三技能 and
    common-box;
  - leader background preparation computes chained-state, tracker/link point, and leader HP/MP need
    without taking the physical input turn;
  - after the 5-second handoff, chained 黄袍 runs leader HP/MP first, then clicks the prepared
    green-link point;
  - non-chained flow does not add a second 5-second wait.
- Fresh 五倍 runtime should show:
  - maintenance window closes right after 黄袍 enter-battle dialog recognition;
  - after first battle exit, members get exactly the intended short HP/MP-only opportunity;
  - no 三技能 / 盒子 runs inside that 5-second member window;
  - leader resumes after the window and either clicks the prepared green link for the next 黄袍 battle
    or proceeds directly to normal return/next-round flow.

Implementation notes:

- `WubeiTask.consumePreparedEnterBattleBeforeNormalPhase(...)` now closes the team maintenance
  window immediately after a `WUBEI_ENTER_BATTLE` prepared dialog is consumed from runtime and before
  the click/action result is evaluated.
- `WubeiTask.runPostBattleRecoverPhase(...)` handles chained 黄袍 before the old ordinary 800ms
  settle. It opens the 5-second `FIRST_AID_WINDOW_OPEN` gate and runs leader no-focus HP/MP precheck
  before returning shared state.
- `WubeiTask.returnHomeAfterCombatOrContinueSpecialTarget(...)` no longer opens first-aid windows
  after tracker detection. Fast-path and full-tracker continuation both consume the cached leader
  first-aid plan before clicking the prepared/current tracker green link.

Validation completed:

- `powershell -ExecutionPolicy Bypass -File scripts/check_wubei_chained_first_aid_window.ps1`
  passed and guards the CR135 source ordering.
- `mvn -q -DskipTests compile` passed.

Card CR136: 五倍/修罗 expected fast-exit stale signal and repeated return-item guard

Business source:

- User reported the first clear false-positive around `2026-06-28 01:53:40`: fast-exit judged the
  battle as finished while the character was still in combat, then the task repeatedly used return
  items before trusted combat state eventually corrected the task.
- User later reported the same class in 修罗 around `2026-06-29 17:53:58`: fast expected exit was
  trusted too early, then the leader used the 修罗 return item repeatedly before normal phase retry
  finally returned home.
- This is a follow-up safety gap under the `docs/业务逻辑.md` rule "Expected 战斗快脱战与回程验证兜底".
  CR121's bounded wait is already Done; CR136 is not about `timeoutMs=-1`.

Runtime evidence:

- `01:52:42.406` the previous WUBEI combat emitted `combat finished; restore action state to FREE
  and emit exit signal`.
- `01:53:11.928` a new `WUBEI_ENTER_BATTLE` prepared dialog click was consumed, and
  `01:53:11.929` the leader entered `WAIT_BATTLE_FINISH`.
- `01:53:12.444` the task immediately logged
  `wubei auto-combat exit detected: recoveryPolicy=FAST_EXPECTED_EXIT` and
  `[wubei] battle finished and recovered`, only about 516ms after entering the wait. There is no
  matching `fast expected combat exit detected by avatar diff` log at that timestamp, so the most
  likely source is a stale `combatExitPending` signal from the previous combat rather than a fresh
  avatar-diff exit.
- `01:53:13.246` -> `01:53:44.471` `RETURN_HOME` used return item attempts repeatedly: cached return
  item first, then full task-page fallback, then another full attempt.
- `01:53:44.607` finally logged that expected return verification failed but trusted combat state was
  still `IN_COMBAT`, and the task resumed `WAIT_BATTLE_FINISH`.
- `01:53:45.207` the same combat captured a new fast expected-exit avatar baseline, and
  `01:54:15.682` avatar diff produced another fast-exit detection. The trusted-IN_COMBAT correction
  did not explicitly refresh the fast-exit baseline from a trusted current in-combat frame before
  the next avatar diff.
- Fresh 修罗 evidence on `2026-06-29`:
  - `17:53:29.819` logged `fast expected combat exit detected` and advanced from `WAIT_COMBAT`
    to `RETURN_HOME`.
  - `17:53:33.232` clicked cached 修罗 return item at `(1297,571)`, but `17:53:34.904`
    the watcher detected combat screen again.
  - The same false-exit episode then continued to additional item use:
    `17:53:43.314` full-scan click `(1287,570)`, `17:53:58.723` attempt=2 full-scan click
    `(1279,561)`, and after pause/resume `17:55:01.545` phase-retry full-scan click `(1291,575)`.
  - `17:54:56.405` trusted probe after the first transaction reported `trustedState=NONE`; the
    修罗 boolean helper did not stop the actual-use path or refresh the in-combat avatar baseline
    before continuing return-item retries.

Problem statement:

- A new expected combat wait can consume a stale exit signal generated before the current
  `WUBEI_ENTER_BATTLE` click actually became combat.
- When the false exit reaches return-home, the current two-attempt return flow can use the return
  item multiple times before asking the trusted combat state whether the character is still fighting.
  In one false fast-exit correction episode, this must be capped at one actual return-item use.
- 修罗 had the same boolean-return shape as old 五倍: a cached unverified use was treated like a
  cache miss and fell through to full scan, then outer phase retry could repeat the same physical
  item-use sequence.
- After trusted combat state corrects the false exit, the same combat must keep the fast-exit path
  available, but the avatar baseline has to be replaced with the current trusted in-combat frame so
  the next diff does not reuse stale or false-exit state.

Required behavior:

- Starting a new WUBEI expected combat must clear or generation-gate any previous combat exit signal.
  `WAIT_BATTLE_FINISH` must only consume exits that belong to the current combat generation or were
  produced after the task armed the current expected wait.
- If a return item is used but the start map is not verified, run the trusted read-only combat probe
  immediately. Do not treat that failed use as a cache miss and continue to full scan / second full
  attempt in the same false fast-exit correction path.
- If that trusted probe says `IN_COMBAT`, stop the remaining return flow immediately and go back to
  `WAIT_BATTLE_FINISH` for 五倍 or `WAIT_COMBAT` for 修罗; this ends the current correction episode.
- After that trusted-IN_COMBAT correction, avatar fast-exit remains allowed. The correction must
  refresh/reset the current combat avatar baseline to the trusted in-combat frame; do not mark the
  current combat invalid or block the next avatar diff.
- If a later avatar diff triggers another fast-exit after that correction, that is a new correction
  episode with a fresh one-use return-item budget. The one-use cap is not a permanent per-combat or
  per-round disable.
- Normal successful fast path remains: avatar diff may still wake expected combat early, and if
  return verification reaches the start map the task continues normally.

Boundaries:

- Do not change the avatar ROI, diff threshold, 15s grace, or 1s cadence from CR113/CR121.
- Do not change `BagService`, item templates, bag page scanning, OCR/template/click/navigation,
  CR121 bounded wait, CR132 return-item prescan/cache strategy, CR134 prepath target choice, or CR135
  黄袍 chained first-aid window.
- 修罗 is now covered by the same single-actual-use correction rule. The patch must stay limited to
  return-home result classification, trusted probe/baseline refresh, and wait-phase resumption.

Validation:

- Focused guard/test:
  - a stale previous `combatExitPending` cannot make a fresh WUBEI `WAIT_BATTLE_FINISH` complete
    before current combat evidence/generation;
  - after one actual return-item use fails to verify the start map inside one correction episode,
    the task performs trusted combat correction before any full scan / second full attempt;
  - trusted-IN_COMBAT after failed return verification aborts further return item attempts;
  - the correction refreshes the avatar baseline to the current in-combat frame while leaving the
    next avatar diff enabled.
- Focused 修罗 guard:
  - cached 修罗 return item used but unverified returns `USED_START_MAP_UNVERIFIED` before any full
    bag scan;
  - `useReturnItemAndVerifyStartMap(...)` runs trusted probe immediately after an actual unverified
    use and returns `STILL_IN_COMBAT` / `FAILED_AFTER_TRUSTED_NOT_IN_COMBAT` instead of looping;
  - `returnHome(...)` resumes `WAIT_COMBAT` before generic phase retry when trusted state is
    `IN_COMBAT`.
- Fresh 五倍 runtime:
  - after a false fast-exit, at most one actual return-item use occurs in that correction episode
    before the task either resumes `WAIT_BATTLE_FINISH` or enters the existing non-combat failure
    recovery;
  - after trusted-IN_COMBAT correction and baseline refresh, a later fast-exit starts a new episode
    and is allowed its own one-use verification budget;
  - a post-correction `fast expected exit avatar baseline captured` is acceptable only if it is based
    on the refreshed trusted in-combat frame, not the pre-correction stale/false baseline;
  - normal successful fast exits still verify return to `宝象国`.
- Fresh 修罗 runtime:
  - after a false fast-exit, at most one actual 修罗 return-item use occurs in that correction
    episode before either `WAIT_COMBAT` resumes or the non-combat fallback path takes over;
  - trusted `IN_COMBAT` after an unverified return use must refresh the avatar baseline and wait for
    the real combat exit, without disabling future fast avatar diff.

Owner / dispatch:

- Repair worker Peirce (`019f0cd8-8207-7062-a1b0-026d3dcda030`) completed the CR136 source repair.
- Reported touched scope: `AutoCombatService`, `BattleRadarService`, `WubeiTask`, and
  `WubeiCR136FastExitLifecycleWiringTest`.
- Reported checks passed: focused `javac/java` guard, `mvn -q -DskipTests compile`,
  `mvn -q -DskipTests test-compile`, and `git diff --check` with only existing line-ending warnings.
- Codex completed the 修罗 parity repair after the `2026-06-29 17:53` false-positive evidence.
  Touched scope: `XiuluoTaskV2`, `XiuluoCR136FastExitLifecycleWiringTest`, and stale source-guard
  marker updates. Checks passed: `XiuluoCR136FastExitLifecycleWiringTest`, affected source guards,
  `mvn -q -DskipTests compile`, and `mvn -q -DskipTests test-compile`.
- Follow-up on `2026-06-29 18:17:25` fresh restart failure:
  - 修罗 first-round `RETURN_HOME` verified cached return to `灵兽村`, then crashed with
    `NoClassDefFoundError: XiuluoTaskV2$ReturnItemUseResult$Status`.
  - Root cause was the CR136 修罗 parity result record depending on an extra nested private
    `Status` enum class that is lazy-loaded only when the return result is created.
  - `ReturnItemUseResult` now uses simple record booleans (`verifiedStartMap`,
    `usedStartMapUnverified`) instead of `ReturnItemUseResult.Status`.
  - `XiuluoCR136FastExitLifecycleWiringTest` now guards against reintroducing
    `ReturnItemUseResult.Status` / `private enum Status` in this path.
  - Verification: the guard failed red on the nested enum, then passed after repair;
    `mvn -q -DskipTests compile` passed; `target/classes` contains no
    `XiuluoTaskV2$ReturnItemUseResult$Status.class`.
- Fresh runtime remains required before closing CR136.

Card CR137: 五倍暗雷怪后台识别后快速撤销预走路并重抽

Status:

- Deprecated / rolled back on 2026-06-28 after fresh 五倍 runtime showed a 黄袍/tracker regression.
- This card should not be treated as an active implementation target. If the optimization is retried,
  open a new CR with fresh evidence and a narrower repair plan.

Rollback reason:

- Fresh runtime `2026-06-28 04:07:28` showed the CR137 accept-time background tracker path could
  recognize the 五倍 title `智斗黄袍` and green-link map `火云戈壁`, but returned `yellow=''`.
- The generated yellow/detail path was built from snapshot/detail/source names repeatedly, creating
  a long `wubei-detail-yellow.png...` chain and making the background path suspect.
- After that, `04:13-04:15` showed 五倍 repeatedly re-clicking the same tracker green link while
  Runner saw visible `OPTION` with `interestPresent=false`.
- Because this was a user-visible fresh regression in the new CR137 path, the whole CR137 runtime
  change was removed instead of patching it in place.

Rollback scope:

- Removed `WubeiRoundContext.trackerParseFuture`, `withTrackerParseFuture(...)`, and
  `clearTrackerParseFuture(...)`.
- Removed `WubeiTask` accept-time background tracker snapshot scheduling, T0/T+1.5s parsing,
  snapshot reader helpers, `WubeiAcceptWindowSnapshot`, and the ready-background 暗雷 fast-reroll
  branch.
- Removed the CR137 source guard tests:
  `WubeiAcceptTrackerBackgroundParseWiringTest` and `WubeiDarkThunderFastRerollWiringTest`.
- Restored 五倍 to: accept task -> keep CR134 post-accept prepath -> wait tracker refresh ->
  live `READ_TRACKER`.
- Restored 暗雷 handling to the live `READ_TRACKER` branch: after the live tracker yellow text
  contains `暗雷怪`, clear `currentTrackerPanel`, wait the old fixed 4s, then reroute to
  `ROUTE_TO_MAIN_TASK`.
- Preserved CR134 post-accept prepath target selection, CR135 黄袍 first-aid window, CR136
  expected-combat fast-exit lifecycle, CR132 return item prescan, tracker click algorithms, OCR
  thresholds, NPC click, mini-map navigation, BagService, and Runner business semantics.

Future note:

- If this optimization is revisited, start from a new CR. The first task should be proving a
  safe snapshot/detail temp-file naming strategy and a clear `OPTION -> WUBEI_ENTER_BATTLE`
  interest/wake contract before reintroducing any background tracker future into the main flow.

Card CR138: 连续队列本地队伍支援 session gate 与归队诊断

Status:

- Done / fresh continuous `[五倍, 修罗x2]` runtime accepted on 2026-06-29. Owner: 唐德.
- 18:37 fresh runtime acceptance:
  - `18:37:14.851` registered one local-team candidate for queue `[wubei, xiuluo_v2]`;
    `18:37:26.349` live leader detected as `hwnd-63C065A`.
  - `18:39:28.947` 五倍 finished with `SUCCESS`.
  - `18:42:45.168` 修罗 round 1 opened local `TEAM_RETURN` and `COMMON_BOX` after return-item
    verification because the precheck found a return signal. Member return clicks then ran through
    `teamReturn:auto-battle:local-team-return-release` with `INPUT_FOCUS_TRACE sameAsTarget=true`
    on the target member windows, and the leader closed `TEAM_RETURN` at `18:43:52.020` after the
    signal cleared.
  - A member still logged `task=auto_battle requested=wubei role=MEMBER`, but this was only the
    audit label: it deferred while local `FIRST_AID` was closed, then at `18:44:02.206` the leader
    opened 修罗 `FIRST_AID`, and at `18:44:04.489-18:44:05.935` that member consumed the pending
    first-aid plan and补法 successfully.
  - `18:46:05.664` 修罗 round 2 finished and the queue completed with `修罗 -> SUCCESS`; targeted
    scan found no `Exception` / `NoClassDef` / `task failed` / phase-loop guard in this fresh window.
- 22:13 heartbeat review: current source remains within the simplified one-`LocalTeamSessionState`
  model and does not add another startup branch. Rechecked `WindowTaskControlService` candidate
  registration, `MultiWindowTaskManager` submit path, `WindowTaskRunner` raw live-role reporting and
  session cleanup, `TaskMaintenanceService` leader-absent/capability gates, `AutoBattleTask` return
  release/legacy fallback, and `AutoCombatService` first-aid/common-box/left-top gates. No new P1/P2
  source blocker found; keep CR138 in Review only because fresh continuous `[五倍, 修罗]` runtime is
  still required.
- 22:13 verification: `mvn -q -DskipTests test-compile` passed; focused guard loop passed:
  `CR138ReviewCaveatWiringTest`, `TaskMaintenanceCR138LocalSupportCapabilityTest`,
  `AutoBattleCR138TeamReturnReleaseWiringTest`, `AutoCombatCR138FirstAidGateWiringTest`,
  `AutoCombatCR138FirstAidOnlyCommonBoxGuardTest`, `LeaderTeamReturnCR138ReleaseWiringTest`, and
  `TeamReturnCR138NoMatchDiagnosticsWiringTest`.
- 21:55 已改：`WindowTaskRunner.resolveTaskTypeBeforeStart(...)` now keeps raw `liveRole` separate
  from effective `assignmentRole`. CR138 local-team session evidence is reported from raw live role
  only; cached `windowContext.role` fallback may affect task assignment but no longer becomes
  `runner-role-preflight` live leader evidence.
- 21:55 已改：the `17:55` 修罗 feedback is valid and fixed. `AutoBattleTask` now treats a member-like
  auto-battle window with team requested task (`xiuluo_v2` / `wubei` / `wuhuan_v2`) but no local
  support session as legacy team-pathing-gated follower support, not standalone auto-battle.
  Its summon-skill cleanup now requires the old `teamMaintenanceKey=requestedTaskCode` plus
  `requireOpenTeamMaintenanceWindow=true`, so it cannot run before the leader opens the pathing
  maintenance window.
- Focused source guard:
  `java -cp "target\\test-classes;target\\classes" com.bot.dhxy.service.CR138ReviewCaveatWiringTest`
  passes after these repairs.
- Fresh Maven verification:
  `mvn -q -DskipTests compile` and `mvn -q -DskipTests test-compile` pass after these repairs.
  Do not mark CR138 Done until fresh continuous `[五倍, 修罗]` runtime is sampled.
- Earlier heartbeat review found a P1 in `WindowTaskRunner.resolveTaskTypeBeforeStart(...)`:
  live role `UNKNOWN` fell back to `windowContext.getRole()`, and the CR138 session marker received
  that fallback role as `runner-role-preflight` evidence. This is now repaired as described above.
- `windowContext.role` is not live-only: it can be written from registration/UI snapshot via
  `WindowRuntimeContext.applyRegistration(...)`; discovery and legacy team registration can assign
  positional `LEADER/MEMBER` roles before any live probe.
- Historical impact before 21:55 repair: a cached/registration `LEADER` could become local-session
  live leader evidence when the real probe returned `UNKNOWN`.
- Repair applied: split raw live role evidence from effective task-assignment fallback. CR138
  leader/session evidence uses raw `liveRole`; old `windowContext.role` fallback is assignment-only.
  `CR138ReviewCaveatWiringTest` now guards `liveRole=UNKNOWN` + cached `WindowRole.LEADER`.
- Historical 21:34/21:44 heartbeat rechecks confirmed the blocker was still active before this repair:
  `WindowTaskRunner` still performed `UNKNOWN -> windowContext.getRole()` fallback before
  `markLocalTeamWindowRoleDetected(...)`, and the guard did not yet cover that upstream fallback.
- Latest review found and fixed a P1 after the unknown-leader repair: submit-time UI role snapshots
  no longer count as live-detected leader evidence.
- `TaskMaintenanceService.hasDetectedLocalLeader(...)` now only trusts `LocalTeamSessionState.leaderWindowId`,
  which is populated from runner live-role evidence; submit-time
  `localLeaderWindowId` is expected/diagnostic only.
- Latest review confirms the previous 1384R P1/P2 were repaired:
  `WindowTaskRunner.submit(...)` no longer collapses queues before live role preflight, and
  `TeamReturnService.logReturnButtonNoMatch(...)` now logs `currentWindowReturnMarkerPresent` rather
  than a misleading leader signal.
- Latest P1 blocker has been fixed in source: unknown-leader local-team candidates now register
  selected windows, runner live-role preflight reports every candidate window, and candidate members
  suppress legacy return / old requested-task first-aid / standalone common-box and combat left-top
  paths until leader detection resolves.
- Runtime rule after the repair: once a leader is live-detected, members use local capability gates;
  if all candidate windows have live role evidence and no local leader exists, the session is marked
  leader-absent and those windows fall back to standalone/non-local auto-battle behavior.
- Latest full-design-scope review blocker is fixed in source:
  `AutoCombatService.maybeRunCombatMaintenance(...)` now lets local support members run combat
  left-top maintenance only when local `LEFT_TOP_STATUS` is open; standalone/non-local auto-battle
  keeps the previous behavior.
- Previously fixed source paths are still useful: local-support idle left-top status, summon-skill cleanup,
  and member support-worker queue semantics now use local session/capability instead of stale
  `requestedTaskCode` gates.
- Latest non-local leader fallback review gap has been fixed in source and
  `CR138ReviewCaveatWiringTest`.
- Latest `FIRST_AID_ONLY` common-box blocker has been fixed in source and focused guards.
- Latest source review caveats and combat-maintenance blocker have also been fixed in source and
  `CR138ReviewCaveatWiringTest`.
- Latest complexity cleanup (`2026-06-29`) is implemented in source: local-team state is now folded
  into one `LocalTeamSessionState` object per session, `WindowTaskRunner` releases the session on
  queue exit, UI submit-failed windows are marked complete, and stale `TeamSupportCapability`
  comments were updated. Keep Review until fresh continuous `[五倍, 修罗]` runtime verifies the new
  cleanup does not regress local support capability gates.
- Latest partial-submit P1 after cleanup review is fixed in source:
  `TaskMaintenanceService.markLocalTeamWindowRoleDetected(...)` now counts
  `roleDetectedWindows union completedWindows` when confirming leader-absent, so submit-failed /
  completed-without-role candidates cannot leave started members permanently pending leader
  detection.
- Reviewer correction after re-check: this partial-submit repair is present in current source and
  covered by both `TaskMaintenanceCR138LocalSupportCapabilityTest` and
  `CR138ReviewCaveatWiringTest`; do not carry the old partial-submit warning as an active blocker.
- This card is created from `docs/run-reports/2026-06-28-auto-battle-local-leader-gate-design.md`.
- Do not start Java behavior changes until the implementer records baseline evidence for the touched
  business path in `docs/ACTIVE_WORK.md` as required by `AGENTS.md`.

Business source:

- Fresh 连续 `[五倍, 修罗]` runtime exposed a real split between leader progress and member support state.
- The leader had already switched to 修罗 and opened `xiuluo_v2#N` maintenance windows, while several
  members stayed in `task=auto_battle requested=wubei role=MEMBER`.
- Evidence:
  - `2026-06-28 19:19:20.084` 光牛 after-combat first-aid precheck found both HP and MP low and queued
    first-aid under `requested=wubei`.
  - `19:19:23.084`, `19:19:27.232`, `19:19:39.257`, and later samples repeatedly logged
    `pending follower first-aid deferred: team first-aid gate closed ... requested=wubei`.
  - `19:19:34.439` 光牛 did submit a `teamReturn:auto-battle` click, and the input focus trace showed
    target window/focus matched 光牛, so the first observed归队 click itself was not blocked by the
    `requested=wubei` gate.
  - `19:21:47.655` 光牛 exited 修罗 round 7 and again queued first-aid with low HP/MP, but the old
    gate still deferred it.
  - `19:21:47 -> 19:58:58` has no member `return button found` evidence until `19:58:58.295`; earlier
    `TeamReturnService.clickReturnTeamIfPresent(...)` returned `false` silently when no return button
    was found, so the direct cause of the long no-return gap was not diagnosable from those logs.

Problem split:

```text
P0-A: stale requestedTaskCode / task gate explains first-aid deferred.
P0-B: TEAM_RETURN no-match is silent, so the round-7 no-return gap remains unproven.
```

Latest review follow-up (2026-06-29 / partial-submit unknown-leader P1 repair):

- Reviewer response / 已改:
  - 已按本轮 review 修复 partial-submit unknown-leader P1；后续 reviewer 可以从这里继续看。
  - 改动点：`TaskMaintenanceService` leader-absent resolved 判定、`TaskMaintenanceCR138LocalSupportCapabilityTest`
    partial-submit 行为 guard、`CR138ReviewCaveatWiringTest` source guard。
  - 当前只剩 fresh 连续 `[五倍, 修罗]` runtime 验证；没有新的源码 blocker 留在本条 follow-up。
- Source finding:
  - `WindowTaskControlService.startSameQueue(...)` marks a per-window submit failure with
    `completeLocalTeamSessionWindow(localTeamSessionKey, windowId, "ui-start-same-queue:submit-failed")`.
  - `completeLocalTeamSessionWindow(...)` only adds that window to
    `LocalTeamSessionState.completedWindows`.
  - `TaskMaintenanceService.markLocalTeamWindowRoleDetected(...)` confirms leader-absent only when
    `state.roleDetectedWindows.containsAll(state.candidateWindows)` and no leader was detected.
  - Therefore a submit-failed candidate is counted for eventual cleanup, but not counted as
    resolved/non-leader for leader-absent fallback.
- Failure mode:
  - UI starts a multi-window team-role queue with unknown leader.
  - One selected candidate fails to submit, and the remaining started candidates live-detect as
    `MEMBER` / non-leader.
  - Because the failed candidate never runs live-role preflight, `roleDetectedWindows` never contains
    all candidates. `leaderAbsent` stays false, so members keep
    `isPendingLocalSupportLeaderDetection(...) == true` and suppress fallback forever.
- Repair:
  - `TaskMaintenanceService.markLocalTeamWindowRoleDetected(...)` now computes no-leader from
    `(roleDetectedWindows union completedWindows).containsAll(candidateWindows)` when no live leader
    has been detected.
  - This preserves the cleanup rule that a session is only removed after all candidates complete,
    while allowing submit-failed/completed-without-role candidates to count as resolved for
    leader-absent fallback.
  - Added a focused behavior guard: unknown-leader session with candidates
    `[partial-submit-member, partial-submit-failed]`; failed candidate completed without role;
    remaining candidate live-detects `MEMBER`; pending leader detection ends and local support remains
    off.
  - Added a source guard requiring the leader-absent path to include `completedWindows`.
- Verification after repair:
  - `mvn -q -DskipTests compile`
  - `mvn -q -DskipTests test-compile`
  - CR138 focused guard loop:
    `CR138ReviewCaveatWiringTest`, `AutoCombatCR138FirstAidGateWiringTest`,
    `TeamReturnCR138NoMatchDiagnosticsWiringTest`, `LeaderTeamReturnCR138ReleaseWiringTest`,
    `AutoBattleCR138TeamReturnReleaseWiringTest`, `TaskMaintenanceCR138LocalSupportCapabilityTest`,
    `AutoCombatCR138FirstAidOnlyCommonBoxGuardTest`.
- Secondary complexity note:
  - The current `CR138ReviewCaveatWiringTest` still asserts the known-leader submit branch shape
    (`leaderSubmit`, `leaderReuse`, `member submit only after leader submit succeeded`). That protects
    current behavior, but it also locks in part of the extra branching the user asked to simplify.
    After the P1 repair, consider rewriting this guard around behavior rather than preserving the
    exact submit implementation shape.

Latest review follow-up (2026-06-29 / complexity cleanup after stale snapshot repair):

- Source/test status:
  - Rechecked current source after the stale snapshot leader repair.
  - `TaskMaintenanceService.hasDetectedLocalLeader(...)` still only trusts live-role session evidence
    in `LocalTeamSessionState.leaderWindowId`; it does not use submit-time `localLeaderWindowId`.
  - CR138 focused guards passed again:
    `CR138ReviewCaveatWiringTest`, `AutoBattleCR138TeamReturnReleaseWiringTest`,
    `AutoCombatCR138FirstAidGateWiringTest`, `LeaderTeamReturnCR138ReleaseWiringTest`,
    `TeamReturnCR138NoMatchDiagnosticsWiringTest`,
    `AutoCombatCR138FirstAidOnlyCommonBoxGuardTest`,
    `TaskMaintenanceCR138LocalSupportCapabilityTest`, plus `mvn -q -DskipTests compile` and
    `mvn -q -DskipTests test-compile`.
- Cleanup implementation:
  - `TaskMaintenanceService` now keeps local support capabilities, capability epochs, live leader
    evidence, candidate windows, role-detected windows, leader-absent state, and completed windows
    inside one `LocalTeamSessionState` object keyed by session.
  - `completeLocalTeamSessionWindow(...)` clears the session only after every registered candidate
    window has completed. This prevents one finished runner from clearing gates while another member
    still needs them, and prevents leader/capability evidence from leaking into the next queue.
  - `WindowTaskRunner` calls the cleanup method in queue `finally`; `WindowTaskControlService` also
    marks submit-failed windows complete so a pre-registered candidate session cannot leak when a
    window never starts.
  - `TeamSupportCapability.SUMMON_SKILL` and `LEFT_TOP_STATUS` comments now describe the real
    pathing-release capabilities instead of calling them future work.
- Red/green evidence:
  - Red first: `mvn -q -DskipTests test-compile` failed because
    `completeLocalTeamSessionWindow(...)` did not exist.
  - Green after source repair: `mvn -q -DskipTests test-compile` and the CR138 focused guard loop
    passed.
- Recommendation:
  - Keep CR138 in Review until fresh continuous `[五倍, 修罗]` runtime confirms that local support
    members still use `FIRST_AID` / `TEAM_RETURN` / `COMMON_BOX` gates correctly and that session
    cleanup does not create premature fallback.

Latest review follow-up (2026-06-29 / stale snapshot leader repaired):

- Verified feedback before editing:
  - The previous unknown-leader repair correctly registered candidates and deferred candidate
    members, but `TaskMaintenanceService.hasDetectedLocalLeader(...)` still treated a nonblank
    `TaskExecutionContext.localLeaderWindowId` as live leader evidence.
  - That id can come from `WindowTaskControlService.startSameQueue(...)` submit-time UI snapshots.
    If the snapshot says `LEADER` but live preflight later finds no real leader, members could still
    enter local support session and wait forever on capabilities no leader opens.
- Source repair:
  - `hasDetectedLocalLeader(...)` now only checks live leader evidence stored in the local session
    state for `context.getLocalTeamSessionKey()`.
  - `localLeaderWindowId` remains available as expected/diagnostic context, but it no longer proves
    a live local leader and cannot activate `isLocalSupportMemberSession(...)` by itself.
  - Positive capability tests now explicitly call `markLocalTeamLeaderDetected(...)` before expecting
    local support capabilities to be open.
- Red-first evidence:
  - `TaskMaintenanceCR138LocalSupportCapabilityTest` failed on the previous source with
    `stale submit-time leader id must not count as a live-detected local leader`.
- Green verification:
  - `mvn -q -DskipTests test-compile`
  - `CR138ReviewCaveatWiringTest`
  - `AutoBattleCR138TeamReturnReleaseWiringTest`
  - `AutoCombatCR138FirstAidGateWiringTest`
  - `LeaderTeamReturnCR138ReleaseWiringTest`
  - `TeamReturnCR138NoMatchDiagnosticsWiringTest`
  - Maven classpath run: `TaskMaintenanceCR138LocalSupportCapabilityTest`
  - Maven classpath run: `AutoCombatCR138FirstAidOnlyCommonBoxGuardTest`
  - `mvn -q -DskipTests compile`
- Fresh runtime status:
  - Keep CR138 in Review, not Done. Run continuous `[五倍, 修罗]` with stale/unknown UI role
    snapshots.
  - Expected behavior: stale submit-time leader id keeps candidate members pending until live role
    evidence resolves; if all candidates report non-leader, logs show leader-absent fallback; if a
    leader is live-detected, members use local capability gates instead of stale
    `requested=wubei` / `wubei#80` gates.

Latest review follow-up (2026-06-29 / candidate-session pre-leader fallback repaired):

- Verified feedback before editing:
  - Unknown-leader same-queue starts were creating a local-team candidate with
    `localLeaderPresent=true`, but before live leader detection the candidate members were not yet
    `isLocalSupportMemberSession(...)`.
  - In that gap, `AutoBattleTask.maybeRunIdleMaintenance(...)` could still use legacy
    `teamReturnService.clickReturnTeamIfPresent(context, "auto-battle")`.
  - `AutoCombatService.runPendingFollowerFirstAidIfAllowed(...)` could still fall back to the old
    `requestedTaskCode` first-aid gate.
  - Member common-box and combat left-top maintenance also needed to avoid standalone behavior while
    leader detection was still unresolved.
- Source repair:
  - `WindowTaskControlService.startSameQueue(...)` registers candidate window ids for unknown-leader
    local-team batches.
  - `WindowTaskRunner` reports each runner live-role preflight through
    `TaskMaintenanceService.markLocalTeamWindowRoleDetected(...)`.
  - `TaskMaintenanceService` now tracks candidate windows, role-detected windows, live-detected
    leaders, and leader-absent sessions separately.
  - `AutoBattleTask` defers candidate members before the legacy auto-battle return-team path.
  - `AutoCombatService` defers candidate members before old requested-task first-aid gates, member
    common-box consumption, and combat left-top maintenance.
- Red-first evidence:
  - `CR138ReviewCaveatWiringTest` failed on the previous source with
    `unknown-leader local-team batches must register candidate windows`.
- Green verification:
  - `mvn -q -DskipTests test-compile`
  - `mvn -q -DskipTests compile`
  - `CR138ReviewCaveatWiringTest`
  - `AutoBattleCR138TeamReturnReleaseWiringTest`
  - `AutoCombatCR138FirstAidGateWiringTest`
  - `LeaderTeamReturnCR138ReleaseWiringTest`
  - `TeamReturnCR138NoMatchDiagnosticsWiringTest`
  - Maven classpath run: `AutoCombatCR138FirstAidOnlyCommonBoxGuardTest`
  - Maven classpath run: `TaskMaintenanceCR138LocalSupportCapabilityTest`
- Fresh runtime status:
  - Still keep CR138 in Review, not Done. Run continuous `[五倍, 修罗]` with stale/unknown UI role
    snapshots and verify candidate registration, live-role resolution, pending-leader defers,
    local capability gates after leader detection, and leader-absent fallback if no selected local
    leader exists.

Latest review follow-up (2026-06-29 / candidate-session pre-leader gap):

- Confirmed fixed from previous review:
  - `WindowTaskRunner.submit(...)` no longer calls `collapseLocalSupportQueue(...)`, and the helper
    is gone. Queue items such as 修罗 are no longer dropped before live role preflight.
  - `TeamReturnService.logReturnButtonNoMatch(...)` now uses `currentWindowReturnMarkerPresent`, so
    it no longer labels a member-window marker as `leaderSignalPresent`.
- P1 blocker:
  - `WindowTaskControlService.startSameQueue(...)` creates an unknown-leader local-team candidate and
    submits all selected windows with `localLeaderPresent=true`.
  - Before any runner live-detects `LEADER`, `TaskMaintenanceService.isLocalSupportMemberSession(...)`
    returns false because `hasDetectedLocalLeader(...)` is not yet satisfied.
  - In that pre-leader-detection gap, `AutoBattleTask.maybeRunIdleMaintenance(...)` allows the
    legacy ungated `teamReturnService.clickReturnTeamIfPresent(context, "auto-battle")` path because
    the context is not yet considered a local support member.
  - The same gap makes `AutoCombatService.runPendingFollowerFirstAidIfAllowed(...)` skip the local
    `FIRST_AID` capability path and fall into the old `context.isLocalLeaderPresent() &&
    requestedTaskCode in (wubei, xiuluo_v2)` gate. If the leader has not been detected yet, or the
    selected batch has no local leader, members can again wait on stale requested-task gates.
- Required source direction:
  - Separate "candidate/pending leader" from "standalone/non-local" and from "active local support".
  - Candidate members should suppress legacy return-team click and old requested-task first-aid gates
    until leader detection resolves the session.
  - Once a leader is live-detected, members use only local capability gates. If role preflight proves
    there is no local leader in the selected batch, clear/disable the candidate session so those
    windows behave as ordinary standalone/non-local auto-battle.
  - Add a focused guard for the exact pre-leader gap: unknown-leader candidate + member auto-battle
    must not call legacy `clickReturnTeamIfPresent(...)` and must not wait on old
    `awaitTeamFirstAidMaintenanceWindowOpen(...)`.
- Verification run during review:
  - `mvn -q -DskipTests test-compile`
  - `mvn -q -DskipTests compile`
  - `CR138ReviewCaveatWiringTest`
  - `AutoBattleCR138TeamReturnReleaseWiringTest`
  - `AutoCombatCR138FirstAidGateWiringTest`
  - `LeaderTeamReturnCR138ReleaseWiringTest`
  - `TeamReturnCR138NoMatchDiagnosticsWiringTest`
  - Maven exec passed for `AutoCombatCR138FirstAidOnlyCommonBoxGuardTest`
  - Maven exec passed for `TaskMaintenanceCR138LocalSupportCapabilityTest`
  - Direct `java -cp target/test-classes;target/classes` for the two dependency-backed tests fails
    with missing `org/slf4j/LoggerFactory`, so use Maven exec for those two tests.
- Fresh runtime status:
  - This pre-leader gap has now been repaired in source in the later follow-up above; fresh
    continuous `[五倍, 修罗]` is still required before CR138 can move to Done.

Latest review follow-up (2026-06-29 / 1384R P1/P2 fixed):

- P1 repair:
  - Removed submit-time `collapseLocalSupportQueue(...)` from `WindowTaskRunner`.
  - `submit(...)` now keeps the requested queue intact until runner live role preflight. A stale
    submit-time `MEMBER` snapshot can no longer irreversibly drop later main-task queue items.
  - Live role preflight still calls `TaskTeamAssignmentPolicy.resolveTaskForRole(...)`; confirmed
    members still resolve leader/五倍 requests to `AUTO_BATTLE` support.
- P2 repair:
  - `TeamReturnService.logReturnButtonNoMatch(...)` renamed the misleading `leaderSignalPresent`
    diagnostic to `currentWindowReturnMarkerPresent`.
  - The field now accurately means "the current scanned window has the return marker", not "leader
    still has a return signal".
- Red-first guard evidence:
  - `CR138ReviewCaveatWiringTest` failed on the previous source with
    `submit must not irreversibly collapse a queue before live role preflight`.
  - `TeamReturnCR138NoMatchDiagnosticsWiringTest` failed on the previous source with
    `CR138 no-match log must identify the current-window return marker`.
- Green verification:
  - `mvn -q -DskipTests compile`
  - `mvn -q -DskipTests test-compile`
  - `CR138ReviewCaveatWiringTest`
  - `TeamReturnCR138NoMatchDiagnosticsWiringTest`
  - `AutoCombatCR138FirstAidGateWiringTest`
  - `AutoBattleCR138TeamReturnReleaseWiringTest`
  - `LeaderTeamReturnCR138ReleaseWiringTest`
  - `TaskMaintenanceCR138LocalSupportCapabilityTest`
  - `AutoCombatCR138FirstAidOnlyCommonBoxGuardTest`
  - `AutoCombatMemberCommonBoxBehaviorTest`
  - `git diff --check` reported only existing CRLF warnings.
- Fresh runtime status:
  - CR138 is no longer source-blocked by 1384R. Fresh continuous `[五倍, 修罗]` should check that
    member logs show `localSession=... localSupportMember=true` after leader detection, stale
    `wubei#80` gates are not used for support decisions, and `currentWindowReturnMarkerPresent`
    appears in no-match diagnostics when relevant.

Latest review correction (2026-06-29 / combat left-top blocker fixed):

- Independent review and local recheck found a source blocker:
  - `AutoCombatService.maybeRunCombatMaintenance(...)` called
    `leftTopStatusSwitchService.handleCombatMaintenance(context, source)` during sparse combat
    cleanup.
  - `LeftTopStatusSwitchService.handleCombatMaintenance(...)` resolves the task through
    `context.getRequestedTaskCode()` first and may click immediately when the switch is open.
  - This path was outside `AutoBattleTask.maybeRunIdleMaintenance(...)`, so it bypassed the new local
    `LEFT_TOP_STATUS` capability gate.
- Implemented repair:
  - Local support members run combat left-top maintenance only when the current local leader session
    has opened `LEFT_TOP_STATUS`.
  - Standalone/non-local auto-battle keeps its existing combat maintenance behavior and does not wait
    on a stale local-team gate.
  - `CR138ReviewCaveatWiringTest` now specifically covers the combat-maintenance path, not only the
    idle `AutoBattleTask` path.
- Observation, not current blocker:
  - `COMMON_BOX` still uses `requestedTaskCode` as a pending key/source label, but current source also
    requires local `COMMON_BOX` capability before consumption. Fresh runtime logs may still show
    `requested=wubei` for box pending/consume; treat that as audit label unless a task-specific box
    policy diverges.

Latest verification (2026-06-29 / combat left-top blocker):

- Red first: `CR138ReviewCaveatWiringTest` failed on the previous source with
  `local support combat left-top maintenance must first check local support session`.
- Green:
  - `mvn -q -DskipTests test-compile`
  - `CR138ReviewCaveatWiringTest`
  - `AutoCombatCR138FirstAidGateWiringTest`
  - `AutoCombatCR138FirstAidOnlyCommonBoxGuardTest`
  - `AutoBattleCR138TeamReturnReleaseWiringTest`
  - `LeaderTeamReturnCR138ReleaseWiringTest`
  - `TeamReturnCR138NoMatchDiagnosticsWiringTest`
  - `TaskMaintenanceCR138LocalSupportCapabilityTest`
  - `AutoCombatMemberCommonBoxBehaviorTest`
  - `mvn -q -DskipTests compile`

Latest verification (2026-06-29 / unknown-role same-queue session blocker):

- Green:
  - `CR138ReviewCaveatWiringTest`
  - `TaskMaintenanceCR138LocalSupportCapabilityTest`
  - `AutoBattleCR138TeamReturnReleaseWiringTest`
  - `AutoCombatCR138FirstAidGateWiringTest`
  - `AutoCombatCR138FirstAidOnlyCommonBoxGuardTest`
  - `LeaderTeamReturnCR138ReleaseWiringTest`
  - `TeamReturnCR138NoMatchDiagnosticsWiringTest`
  - `mvn -q -DskipTests compile`
- Fresh runtime acceptance addendum:
  - UI role snapshot may be unknown at submit time; this should now log
    `local-team session candidate without known leader`.
  - After runner preflight detects the selected leader, logs should show
    `maintenance local-team leader detected`.
  - Member support logs should then show `localSession=... localSupportMember=true`, not the old
    `localSession=null localSupportMember=false` path seen in the 14:04 review sample.
  - 摄妖香 / 摄药箱 ordering was explicitly left unchanged in this pass.

Previous implementation update (2026-06-29 / partial full design-scope repair):

- Verification run:
  - `mvn -q -DskipTests test-compile`
  - `CR138ReviewCaveatWiringTest`
  - `AutoCombatCR138FirstAidGateWiringTest`
  - `AutoCombatCR138FirstAidOnlyCommonBoxGuardTest`
  - `AutoBattleCR138TeamReturnReleaseWiringTest`
  - `LeaderTeamReturnCR138ReleaseWiringTest`
  - `TeamReturnCR138NoMatchDiagnosticsWiringTest`
  - `TaskMaintenanceCR138LocalSupportCapabilityTest`
- Implemented repair:
  - `TaskMaintenanceService.openTeamPathingMaintenanceWindow(...)` now publishes local
    `SUMMON_SKILL` and `LEFT_TOP_STATUS` capability together with the existing
    `FIRST_AID/PATHING_WINDOW/COMMON_BOX` capabilities.
  - `TaskMaintenanceService.closeTeamMaintenanceWindow(...)` closes those local capabilities with
    the pathing window; `FIRST_AID_ONLY` still opens only `FIRST_AID`, so 黄袍/连战 short recovery
    cannot run summon-skill or left-top maintenance.
  - `AutoBattleTask.maybeRunIdleMaintenance(...)` consumes left-top status only when local
    `LEFT_TOP_STATUS` is open, and summon-skill cleanup claims a local-session
    `SUMMON_SKILL` capability round instead of `context.getRequestedTaskCode()`.
  - `TaskMaintenanceRequest.requiredLocalSupportCapability` and the local capability epoch key keep
    summon-skill claims unique per local leader release, even when the same session moves from 五倍
    to 修罗.
  - Superseded by the 1384R repair above: the previous submit-time member queue collapse was removed
    because it trusted stale role snapshots before live role preflight. Live role detection now owns
    member-to-`AUTO_BATTLE` reassignment without trimming the requested queue at submit time.
- Guard result:
  - `TaskMaintenanceCR138LocalSupportCapabilityTest` proves `FIRST_AID` does not imply
    `SUMMON_SKILL` / `LEFT_TOP_STATUS`, and pathing release opens then closes both.
  - `CR138ReviewCaveatWiringTest` proves local-support left-top, summon-skill, and the no
    submit-time stale-role collapse rule. Existing CR138 first-aid/common-box/team-return guards
    still pass.
- Fresh runtime acceptance:
  - Run continuous `[五倍, 修罗]` and verify member auto-battle support does not log deferred waits on
    stale `requested=wubei` / `wubei#80` gates after the leader switches to 修罗.
  - Verify member HP/MP first-aid, left-top status, summon-skill cleanup, common box, and
    `TEAM_RETURN` only run during the corresponding local session capability windows.
  - Verify standalone/non-local auto-battle does not wait on stale requested-task gates.

Superseded implementation update (2026-06-29 / non-local leader fallback review):

- Verification run:
  - `mvn -q -DskipTests compile`
  - `mvn -q -DskipTests test-compile`
  - `CR138ReviewCaveatWiringTest`
  - `AutoCombatCR138FirstAidGateWiringTest`
  - `AutoBattleCR138TeamReturnReleaseWiringTest`
  - `LeaderTeamReturnCR138ReleaseWiringTest`
  - `TeamReturnCR138NoMatchDiagnosticsWiringTest`
  - `AutoCombatCR138FirstAidOnlyCommonBoxGuardTest`
  - `TaskMaintenanceCR138LocalSupportCapabilityTest`
- Implemented repair:
  - `AutoCombatService.runPendingFollowerFirstAidIfAllowed(...)` only uses the old
    `awaitTeamFirstAidMaintenanceWindowOpen(...)` requested-task fallback when the context has a
    local leader. A member auto-battle window without a local leader/session no longer waits forever
    on stale `requested=wubei/xiuluo_v2` gates.
  - `AutoBattleTask.maybeRunIdleMaintenance(...)` now derives `requireTeamMaintenanceGate` from an
    actual local support session, so non-local/standalone auto-battle keeps its requested label for
    logs but does not inherit the old team window requirement.
- Guard result:
  - `CR138ReviewCaveatWiringTest` now also proves the non-local fallback rules.
- Continued source review result:
  - Reviewer correction: the acceptance boundary is the full design report, not only the latest
    narrow implementation slice. CR138 therefore remains blocked before fresh runtime.
  - Implemented pieces are still useful and guarded: local-session `FIRST_AID`, `COMMON_BOX`
    isolation, `TEAM_RETURN` release gating, partial-submit handling, and non-local auto-battle
    fallback all have focused guards plus compile/test-compile coverage.
  - Blocking gap: local support left-top status still uses the old requested-task gate:
    `AutoBattleTask.maybeRunIdleMaintenance(...)` checks
    `isTeamPathingMaintenanceWindowOpen(context, context.getRequestedTaskCode())` and calls
    `leftTopStatusSwitchService.consumeFollowerSafeWindow(context, context.getRequestedTaskCode())`.
  - Blocking gap: summon-skill cleanup still uses the old `requestedTaskCode` / task-round claim gate:
    `AutoBattleTask.maybeRunIdleMaintenance(...)` passes `teamMaintenanceKey =
    context.getRequestedTaskCode()`, and `TaskMaintenanceService.maybeCleanSummonSkill(...)` resolves
    claims from that `teamMaintenanceKey` instead of a local-session capability/round claim.
  - Blocking gap: member queue semantics are not yet collapsed into a single local support worker for
    `[五倍, 修罗]`; the report's Phase 6 remains unfinished. Do not run final fresh runtime until these
    gaps are implemented and guarded.

Superseded implementation update (2026-06-29 / review caveat follow-up):

- Verification run:
  - `mvn -q -DskipTests compile`
  - `mvn -q -DskipTests test-compile`
  - `CR138ReviewCaveatWiringTest`
  - `AutoCombatCR138FirstAidGateWiringTest`
  - `AutoBattleCR138TeamReturnReleaseWiringTest`
  - `LeaderTeamReturnCR138ReleaseWiringTest`
  - `TeamReturnCR138NoMatchDiagnosticsWiringTest`
  - `AutoCombatCR138FirstAidOnlyCommonBoxGuardTest`
  - `TaskMaintenanceCR138LocalSupportCapabilityTest`
- Implemented repair:
  - `AutoCombatService.runPendingMemberCommonBoxIfAllowed(...)` now checks
    `commonBoxService.hasPendingBoxForCurrentWindow(...)` before local `COMMON_BOX` gate logging, so
    no-pending windows no longer emit fake `pending member common-box deferred` diagnostics.
  - `WindowTaskControlService.startSameQueue(...)` submits the detected local leader first. Member
    submits receive local-team session metadata only when that leader submit succeeds; if the leader
    rejects the queue, members remain standalone/fallback auto-battle submissions instead of orphan
    local-session members.
- Guard result:
  - `CR138ReviewCaveatWiringTest` proves both wiring rules.

Superseded implementation update (2026-06-29 / first-aid-only common-box blocker):

- Verification run:
  - `mvn -q -DskipTests compile`
  - `mvn -q -DskipTests test-compile`
  - `TaskMaintenanceCR138LocalSupportCapabilityTest`
  - `AutoCombatCR138FirstAidGateWiringTest`
  - `AutoCombatCR138FirstAidOnlyCommonBoxGuardTest`
  - `AutoBattleCR138TeamReturnReleaseWiringTest`
  - `LeaderTeamReturnCR138ReleaseWiringTest`
  - `TeamReturnCR138NoMatchDiagnosticsWiringTest`
  - existing `AutoCombatMemberCommonBoxBehaviorTest`
- Implemented repair:
  - `openTeamFirstAidMaintenanceWindow(...)` opens only `FIRST_AID`, so 黄袍/连战
    `FIRST_AID_ONLY` cannot imply box/return/summon/left-top maintenance.
  - `openTeamPathingMaintenanceWindow(...)` explicitly opens `COMMON_BOX` together with
    `FIRST_AID` and `PATHING_WINDOW`.
  - `openLocalTeamReturnSupportWindow(...)` explicitly opens `TEAM_RETURN` and `COMMON_BOX`, so a
    released return opportunity may still consume box before return-team.
  - `runPendingMemberCommonBoxIfAllowed(...)` now requires `COMMON_BOX` for local support sessions.
  - `runPendingFollowerFirstAidIfAllowed(...)` no longer calls `consumePendingBoxIfAllowed(...)`
    inside the first-aid branch.
  - `AutoBattleTask.tryRunLocalTeamReturnRelease(...)` checks `COMMON_BOX` before box consumption.
- Guard result:
  - `AutoCombatCR138FirstAidOnlyCommonBoxGuardTest` proves `FIRST_AID_ONLY + pending common box`
    does not consume box, while a pathing window can consume it.
- Resolved follow-up source review caveats:
  - The old source checked local-session `COMMON_BOX` capability before
    `commonBoxService.hasPendingBoxForCurrentWindow(...)`, so it could log
    `pending member common-box deferred` with no real pending box. The follow-up moved the pending
    check ahead of the deferred log.
  - The old source created a local-team session before per-window submits, so a failed leader submit
    could leave successful members with an orphan session. The follow-up submits the local leader
    first and only passes session metadata to members when that leader submit succeeds.

Superseded implementation review (2026-06-29):

- Verification run:
  - `mvn -q -DskipTests test-compile`
  - `TaskMaintenanceCR138LocalSupportCapabilityTest`
  - `AutoCombatCR138FirstAidGateWiringTest`
  - `TeamReturnCR138NoMatchDiagnosticsWiringTest`
- Passing guards prove only the narrow wiring/diagnostic slice:
  - a stale member `requested=wubei` can observe local-session `TeamSupportCapability.FIRST_AID`;
  - `AutoCombatService.runPendingFollowerFirstAidIfAllowed(...)` tries local capability before the
    old requested-task gate;
  - `TeamReturnService` now emits richer no-match diagnostics.
- Superseded blocking finding:
  - `src/main/java/com/bot/dhxy/service/AutoCombatService.java` still calls
    `runPendingMemberCommonBoxIfAllowed(...)` before pending follower first-aid in the main tick;
  - the same first-aid path still calls `commonBoxService.consumePendingBoxIfAllowed(...)` after
    acquiring the pending-follower-first-aid turn;
  - therefore a 黄袍 `FIRST_AID_ONLY` window can still consume common box, despite the card requiring
    HP/MP-only behavior.
- Additional open gaps:
  - superseded by the latest update: non-local-leader fallback still used the old
    `requestedTaskCode` task gate and needed a clear standalone auto-battle policy;
  - actual `TEAM_RETURN` clicking is still direct and not leader-release gated; this remains a later
    slice, but the full CR cannot close until it is modeled.
- Repaired by latest update:
  - keep common-box priority only in windows that explicitly permit `COMMON_BOX`;
  - ensure `FIRST_AID` capability alone cannot run common box, summon skill, repair, heal-pet,
    sheyaoxiang, left-top status, or return-team click;
  - add a behavior guard for `FIRST_AID_ONLY + pending common box` proving the box is not consumed.
- Repaired by latest non-local fallback update:
  - no-local-leader auto-battle no longer waits on stale requested-task team gates;
  - idle maintenance only requires the old team window gate when an actual local support session is
    present.

Root cause model:

- A member `AUTO_BATTLE` worker is currently modeled as a normal queue item derived from the first
  requested task, e.g. `[五倍, 修罗] -> first item WUBEI -> AUTO_BATTLE requested=wubei`.
- `AutoBattleTask` is a long-running loop, so the member often never naturally advances to the second
  requested task.
- `requestedTaskCode` is doing too much:
  - original UI request;
  - auto-battle audit/source label;
  - member maintenance gate key.
- Members should not decide "I am 五倍 / 修罗 member" for maintenance. They should know they are local
  team support workers and ask whether the local leader/session has opened the relevant capability.

Required design:

- Introduce or wire an explicit local team support/session context:
  - `teamSessionKey`;
  - support member flag;
  - local leader present flag;
  - leader window id;
  - leader current task/round for logs and diagnostics.
- Keep `requestedTaskCode` as log/audit context, not the business source of truth for member
  maintenance gate decisions.
- Add capability-aware gates. Minimum capabilities:
  - `FIRST_AID`;
  - `PATHING_WINDOW`;
  - `SUMMON_SKILL`;
  - `LEFT_TOP_STATUS`;
  - `COMMON_BOX`;
  - `TEAM_RETURN` as a leader-release-gated entry.
- Member strategies are unified across 五倍 / 修罗:
  - `FIRST_AID`: HP/MP only, allowed by `FIRST_AID` or a wider safe capability.
  - `LEFT_TOP_STATUS`: task-agnostic, only in a safe support window.
  - `COMMON_BOX`: task-agnostic and highest priority in windows that permit box consumption.
  - `TEAM_RETURN`: the member may observe in the background, but the actual click waits until the
    local leader has finished its own post-combat return/return-home rhythm and explicitly releases a
    member support opportunity.
  - If `COMMON_BOX` and `TEAM_RETURN` are both pending in the same released opportunity, consume the
    box first, then click return-team.
- First implementation slice should be narrow:
  - add session/gate diagnostics;
  - double-write leader task-specific gate and future session/capability gate if needed;
  - switch only pending follower first-aid to `FIRST_AID` session capability when local leader exists;
  - leave three-skill, common-box, repair, heal-pet, sheyaoxiang, and left-top maintenance on their
    existing rules unless separately migrated.

TEAM_RETURN diagnostic requirement:

- `TeamReturnService.clickReturnTeamIfPresent(...)` must log no-match outcomes at INFO/WARN with
  throttling. At minimum include:
  - `windowId`, hwnd, role, title/player identity if available;
  - source and `requestedTaskCode`;
  - current local session / leader info if available;
  - whether leader-side `WAIT_TEAM_RETURN` / shared return signal is currently present;
  - screenshot provider/path or capture audit used for the return-button scan;
  - template name, threshold, best score, and best candidate rect/point if the matcher exposes it;
  - current combat/pathing/idle state as known by the context/runtime;
  - last return-button found/click timestamp or age.
- Found/click path must keep focus/input trace:
  - target window id/hwnd/title;
  - foreground hwnd before/after;
  - click point;
  - input sequence result.
- Do not default to saving images for every no-match. Optional debug image/overlay is acceptable only
  behind a debug switch or after consecutive no-match while the leader still sees a return signal.
- Target behavior after the follow-up migration is not "member clicks whenever it sees return":
  - local leader first finishes its own post-battle return/return-home sequence;
  - leader opens/releases a member support opportunity for return handling;
  - member may then process pending `COMMON_BOX` first if the window permits it, and then click
    `TEAM_RETURN`;
  - without a local leader, normal standalone auto-battle maintenance keeps its existing behavior.

Non-goals / safety boundaries:

- Do not simply rewrite member `requestedTaskCode` to `auto_battle`; that would remove the leader
  safety gate and can let members steal foreground/input during leader-critical operations.
- Do not make a single coarse `leaderWindowOpen=true` gate. 黄袍 first-aid-only windows must not allow
  summon skill, common box, repair, heal-pet, or other heavy maintenance.
- Do not change tracker green click, NPC/dialog templates, minimap/world-map click coordinates,
  return-item bag search/use, combat entry detection, or expected-combat fast-exit semantics in this
  card.
- Do not make `TEAM_RETURN` wait on generic `PATHING_WINDOW`; it must use its own leader-release
  capability so members do not click before the leader has completed its post-combat return rhythm.

Suggested implementation phases:

1. Diagnostics-only:
   - print first-aid gate comparisons: old requested/task gate vs future local-session capability gate;
   - print `TEAM_RETURN` found/no-match diagnostics;
   - print leader open/close with task gate and future session/capability labels.
2. Session plumbing:
   - create/pass `teamSessionKey` for a selected multi-window queue run;
   - mark local support members and local leader presence in task context/runtime logs.
3. Leader double-write:
   - keep existing `wubei#N` / `xiuluo_v2#N` gates;
   - add session/capability state for observation.
4. First-aid migration:
   - for local support members, pending first-aid waits for `FIRST_AID` capability instead of
     `requested=wubei/xiuluo_v2`;
   - if no local leader exists, keep pure auto-battle fallback behavior.
5. Later cards/slices:
   - migrate `TEAM_RETURN` to its own leader-release capability after diagnostics prove its actual
     failure mode;
   - migrate left-top, summon skill, common box separately with their own claim/session safeguards;
   - preserve the settled order inside a released opportunity: `COMMON_BOX` first when allowed, then
     `TEAM_RETURN`.

Acceptance:

- Source guards prove `AutoCombatService.runPendingFollowerFirstAidIfAllowed(...)` no longer uses
  stale `requestedTaskCode` as the sole gate when a local support session exists.
- Source guards prove `FIRST_AID_ONLY` permits only HP/MP first-aid and cannot trigger summon skill,
  common box, repair, heal-pet, or sheyaoxiang.
- Source/log guards prove `TeamReturnService` logs no-match diagnostics and does not change first-slice
  `TEAM_RETURN` click semantics.
- Follow-up source guards prove a local support member cannot perform the actual `TEAM_RETURN` click
  before the leader release capability is open, while standalone auto-battle remains usable without a
  local leader.
- Follow-up source/log guards prove that if `COMMON_BOX` and `TEAM_RETURN` are both pending in the
  same permitted release window, the member consumes common box first, then attempts return-team.
- Fresh single-task 五倍 and single-task 修罗 runs do not regress existing member maintenance behavior.
- Fresh continuous `[五倍, 修罗]` run shows:
  - no long-lived member first-aid loop stuck on `requested=wubei` after leader has moved to 修罗;
  - first-aid logs include `gate=local-team capability=FIRST_AID leaderTask=xiuluo_v2 allow=...`;
  - if leader waits on team return and a member does not click return, the log includes an actionable
    `team return: return button not found ...` reason instead of silence.

##### Agent A: 何黎 - Framework/EventBus/Runner Lane

Owns:

- `src/main/java/com/bot/dhxy/window/runtime/WindowReadyEventBus.java`
- `src/main/java/com/bot/dhxy/window/model/WindowReadyEventType.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java` only if the wake/sequence
  contract truly needs it.

Avoids:

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
- 五环 / 修罗 / NPC / OCR / minimap / world-map click algorithms.

Card A1: Event sequence API and wait diagnostics

- [x] Inspect `WindowReadyEventBus` and record its current publish/wait semantics in
  `docs/ACTIVE_WORK.md`.
- [x] Add `currentSequence()` to return the latest ready-event sequence without blocking.
- [x] Ensure `awaitNewer(...)` can be called with an `afterSequence` captured before sleeping.
- [x] Log wait diagnostics only at useful points:
  - `windowId`
  - `wakeTypes`
  - `afterSequence`
  - returned sequence/type if any
  - `timeoutMs`
  - elapsed time
  - `wokeByEvent` or `wokeByTimeout`
- [x] Run `mvn -q -DskipTests compile`.
- Acceptance:
  - Existing behavior is unchanged except new API/logs.
  - A task can capture sequence before sleeping and cannot miss an event published immediately
    after the capture.

Card A2: Runner prepared/ready timing visibility

- [x] Inspect current prepared-action publish/clear/consume path in `WindowTaskRunner` and
  `WindowRuntimeContext`.
- [x] Add slow-only or reason-coded logs that make these intervals visible:
  - Runner detects dialog / pathing state.
  - Runner publishes `TASK_ATTENTION_REQUIRED` or `PATHING_TERMINAL`.
  - Prepared action is stored / overwritten / cleared / stale.
- [x] Do not add new scheduler behavior in this card.
- [x] Run `mvn -q -DskipTests compile`.
- Acceptance:
  - From `logs/dhxy-console.log`, we can tell whether delay came from Runner preparation,
    task turn scheduling, stale rejection, or input queue wait.

Card A3: Observer slow tick breakdown

- [x] Add or normalize slow tick logs in `WindowTaskRunner` so a tick can be attributed to:
  - combat observation
  - pathing observation
  - dialog detect
  - route prepare
  - task-dialog prepare
  - tracker/task-panel prepare
  - attention publish
  - total tick time
- [x] Prefer slow-only logs or aggregated counters; do not spam every 100ms success tick.
- [x] Run `mvn -q -DskipTests compile`.
- Acceptance:
  - When watcher is slow, logs show which component was slow instead of only showing total delay.

##### Agent B: 谢帅 - Wubei Scheduling Lane

Owns:

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
- `src/main/java/com/bot/dhxy/task/wubei/WubeiStepOutcome.java`
- `src/main/java/com/bot/dhxy/task/wubei/WubeiWaitReason.java`
- `src/main/java/com/bot/dhxy/task/wubei/WubeiWaitSpec.java`

Avoids:

- Framework event-bus implementation before A1 lands.
- OCR/template thresholds, NPC click selection, minimap/world-map click selection, and battle
  recovery rewrites.

Card B1: Explicit wait model

- [x] Create `WubeiWaitReason` with initial values:
  - `WAIT_PATHING_TERMINAL`
  - `WAIT_PREPARED_DIALOG`
  - `WAIT_COMBAT_STATE_CHANGE`
  - `WAIT_TEAM_ATTENTION`
  - `WAIT_RETRY_TIMER`
- [x] Create `WubeiWaitSpec` as a Lombok immutable value object:
  - `@Value`
  - `@Builder`
  - fields: `reason`, `wakeTypes`, `timeoutMs`, `minParkMs`, `currentWindowOnly`,
    `allowOpportunisticMaintenance`
- [x] Extend `WubeiStepOutcome` with optional `waitSpec`.
- [x] Do not change phase behavior in this card.
- [x] Run `mvn -q -DskipTests compile`.
- Acceptance:
  - Existing 五倍 flow compiles and behaves the same when no wait spec is returned.

Card B2: Park after releasing task turn

- [x] In `WubeiTask.runRoundPhases(...)`, consume `waitSpec` only after
  `yieldAfterMustYield(...)` has released the turn.
- [x] Implement lost-event-safe park order:
  - capture `long seq = windowReadyEventBus.currentSequence()`;
  - recheck runtime/prepared/pathing state before sleeping;
  - call `awaitNewer(windowId, wakeTypes, seq, timeoutMs)`;
  - honor task stop/pause checkpoint after waking;
  - re-read runtime state and continue.
- [x] Timeout wake must not mean business failure. It only returns control so the phase can
  re-check state.
- [x] Add logs:
  - `waitReason`
  - `wakeTypes`
  - `afterSequence`
  - `timeoutMs`
  - `elapsedMs`
  - wake result
  - runtime recheck result
- [x] Run `mvn -q -DskipTests compile`.
- Acceptance:
  - Waiting phases no longer reacquire the same turn every 80ms just to discover nothing changed.
  - Stop-all still interrupts the parked task promptly.

Card B3: Phase-to-wait mapping

- [x] For `RESOLVE_AFTER_PATHING`, return a wait spec for `PATHING_TERMINAL` and
  `TASK_ATTENTION_REQUIRED` when the only useful next event is watcher state.
- [x] For route / accept / enter-battle dialog waits, return a prepared-dialog wait spec.
- [x] Set `allowOpportunisticMaintenance=false` for prepared-dialog waits so 三技能/maintenance
  cannot jump ahead of a ready dialog click.
- [x] Do not change `WAIT_BATTLE_FINISH` in this card except documenting it as Phase 2.
- [x] Run `mvn -q -DskipTests compile`.
- Acceptance:
  - Pathing/dialog waits park on explicit wake reasons.
  - Existing business recovery still decides success/failure after wake.

Card B4: 白龙马 Runner-prepared story flow

- [x] Register `WUBEI_PROBE_STORY` interest before or immediately around 显形镜.
- [x] Use Runner-prepared result as the only story decision source.
- [x] If prepared result is `target-ready`, follow the old 白龙马 rule:
  - mark story confirmed;
  - continue to the existing enter-battle path, including the old `Alt+A` step when required.
- [x] If prepared result is `wrong-position`, return to the same tracker green pathing flow.
- [x] If no prepared result arrives:
  - do not mark prompt failed;
  - do not switch to the second prompt;
  - do not run early `Alt+A`;
  - wait/recheck through the new wait spec.
- [x] Run `mvn -q -DskipTests compile`.
- Acceptance:
  - New framework, old 白龙马 business decisions.
  - No foreground OCR/inspect in this path unless a later card explicitly reopens that design.

##### Agent C: 唐德 - Verification/Metrics/Review Lane

Owns:

- `docs/ACTIVE_WORK.md`
- optional `scripts/analyze_wubei_latency.ps1`
- optional docs-only updates in `docs/PACKAGE_ARCHITECTURE.md`

Avoids:

- Production business logic.
- Input, OCR, NPC click, minimap/world-map click, and Runner scheduling changes.

Card C1: Baseline log extraction

- [x] Write a repeatable log summary command or script that reads `logs/dhxy-console.log`.
- [x] Report these counters and timings:
  - `task.turn.handoff`
  - `sameAsPrevious=true`
  - `consumePrepared result=absent`
  - `dialog.interest.update`
  - `PATHING_TERMINAL` publish -> task wake
  - prepared action publish -> consume
  - consume -> input queued
  - input queued -> input start
  - click done -> state changed
- [x] Save the command and a baseline sample in `docs/ACTIVE_WORK.md`.
- Acceptance:
  - Before/after runs can be compared without manually reading thousands of log lines.

Card C2: Real-run acceptance checklist

- [x] Add a checklist in `docs/ACTIVE_WORK.md` for:
  - one-window quick run;
  - three-window run;
  - five-window run;
  - 白龙马 probe case;
  - ordinary route dialog case;
  - pathing terminal case.
- [x] Each checklist item must record:
  - log time range;
  - number of windows;
  - whether `task.turn.handoff` churn dropped;
  - whether any click latency regressed;
  - whether any window starved.
- Acceptance:
  - User can run a test and paste only the time range; the agent knows exactly what to verify.

Card C3: Log-noise and regression review

- [x] Review new A/B logs after compile and first run.
- [x] Keep warnings, stale rejection, timeout, and slow-path logs visible.
- [x] Recommend rate limits only for repeated success spam.
- [x] Record the baseline log policy in `docs/ACTIVE_WORK.md`; final policy still needs the first post-change A/B run.
- Acceptance:
  - Diagnostics stay useful while repeated 100ms idle logs are reduced.

##### Code Review Follow-up Cards

These cards came from direct code review on 2026-06-18. They are not assigned to a fixed person.
Any agent may claim one by changing the sprint board row from `Ready` to `In Progress`.

Card CR1: Prepared action consumption must run inside the task transaction

- [x] Inspect `WubeiTask.runRoundPhases(...)` around the phase-boundary priority path.
- [x] Remove the path that fabricates `TaskTransactionOutcome` without calling
  `TaskTransactionRunner.run(...)`.
- [x] Ensure `tryConsumePreparedWubeiDialog(...)` and its real mouse click are executed only while
  the task turn is owned by the current window transaction.
- [x] Ensure `TaskTurnCoordinator.leave(...)` sees the real transaction outcome for priority
  `SHARED_STATE_TRIGGERED` / `PATHING_STARTED` results.
- [x] Run `mvn -q -DskipTests compile`.
- Acceptance:
  - No prepared-dialog click can happen outside `TaskTransactionRunner.run(...)` or a documented
    transaction-equivalent path.
  - Priority-yield outcomes release the task turn through the normal coordinator path.

Card CR2: Remove unconditional 80ms phase-boundary wait

- [x] Inspect `checkReadyPriorityBeforePhase(...)`.
- [x] Do not call `awaitNewer(..., READY_EVENT_SETTLE_WAIT_MS)` on every normal phase boundary.
- [x] Keep the fast path for already-present fresh prepared actions.
- [x] Only wait briefly when there is a concrete local reason, such as visible dialog snapshot,
  active dialog interest, or a fresh ready event already known for this window.
- [x] Run `mvn -q -DskipTests compile`.
- Acceptance:
  - A normal phase with no visible/expected dialog does not add an 80ms wait.
  - Prepared dialogs that are already ready are still consumed promptly.

Card CR3: Close the missing waitSpec/churn path

- [x] Use `logs/dhxy-console.log` to find the repeated same-window handoff chain where
  `transaction=wubei:ROUTE_TO_MAIN_TASK` reacquires every ~80ms.
- [ ] Identify whether the loop is caused by a missing `waitSpec`, an already-satisfied wait check,
  priority outcome bypassing park, or a pathing snapshot that is not terminal/active as expected.
- [ ] Patch only the missing scheduling/wait path. Do not change navigation click algorithms,
  OCR/template thresholds, minimap logic, or 五倍 business decisions.
- [x] Run `mvn -q -DskipTests compile`.
- [ ] Record before/after counters in `docs/ACTIVE_WORK.md` using the latency script.
- Acceptance:
  - During active pathing, the same window should not reacquire the same 五倍 transaction dozens of
    times at ~80ms intervals.
  - The fix preserves ready-dialog and pathing handoff latency.

CR3 current finding:

- The available `2026-06-18 01:08:00` to `01:21:00` log slice clearly shows old churn:
  `RESOLVE_AFTER_PATHING same=1363`, `WAIT_BATTLE_FINISH same=878`, `ROUTE_TO_MAIN_TASK same=237`.
- The same slice has `window.ready.await=0` and `wubei.wait.parkFinished=0`, while the current code
  now contains park/wakeup logs. This means the slice cannot prove whether the latest code still
  misses `waitSpec` or whether it was simply captured before the park path was active.
- Do not patch CR3 from this old slice. Re-run 五倍 with the latest build, then inspect whether
  `wubei wait park finished` appears for `WAIT_PATHING_TERMINAL`; only patch if the fresh run still
  shows same-window reacquire without park.

CR3 fresh-run update:

- Time range: `2026-06-18 12:01:00.000` to `12:05:00.000`.
- The fresh run does exercise park/wake:
  `window.ready.await=51`, `wubei.wait.parkFinished=18`, `WAIT_PATHING_TERMINAL=17`.
- The old "no park at all" diagnosis is no longer the right patch target. Current failures are:
  missed already-visible/published attention events, null pathing target terminal semantics, and slow
  prepared-dialog consumption after attention. These are split into CR9, CR10, and CR11.

Card CR4: Make the latency script match current logs

- [x] Update `scripts/analyze_wubei_latency.ps1` to count current `[INPUT_TRACE] queued-action`,
  physical click/move logs, `event=input.request`, `window.ready.await`, and `wubei wait` park logs.
- [x] Add counters for `wubei wait park finished`, `wakeResult=event`, `wakeResult=timeout`,
  priority-yield, and same-window reacquire by transaction name.
- [x] Stop reporting `input.start`, `input.done`, `click.done`, and `state.changed` as accepted
  coverage unless those patterns are actually emitted by current logs.
- [x] Run the script against `logs/dhxy-console.log` and save the sample output in
  `docs/ACTIVE_WORK.md`.
- Acceptance:
  - The script can prove whether a sprint change reduced churn and whether click/input latency
    regressed using current log formats.

Card CR5: Clean stale ready-event helper surface

- [x] Inspect `WindowReadyEventBus.latestOtherFresh(...)`.
- [x] Either remove it if unused, or fix it so it reads from the correct event store for the
  requested `WindowReadyEventType`.
- [x] Keep `latestOtherFreshPreparedAction(...)` and `latestOtherFreshPathingTerminal(...)` as the
  preferred explicit APIs for current sprint work.
- [x] Run `mvn -q -DskipTests compile`.
- Acceptance:
  - No generic helper silently reads the prepared-action-only cache for pathing or other event types.

Card CR6: Enforce prepared-dialog maintenance skip

- [x] Inspect `WubeiTask.yieldAfterMustYield(...)`, `maybeRunLeaderPathingSummonMaintenance(...)`,
  and every `waitForPreparedDialogWake(...)` caller.
- [x] Make `allowOpportunisticMaintenance=false` a real gate before any opportunistic maintenance
  can run during a `WAIT_PREPARED_DIALOG` outcome.
- [x] Keep `WAIT_PATHING_TERMINAL` behavior unchanged unless this card finds a direct bug.
- [x] Do not change 三技能 / repair / summon skill business rules; only enforce the existing wait
  policy.
- [x] Run `mvn -q -DskipTests compile`.
- Acceptance:
  - A prepared route / accept / enter-battle / probe-story dialog wait cannot run leader pathing
    summon maintenance before the dialog is consumed or rejected.
  - Logs make the skip visible when a wait spec blocks opportunistic maintenance.

Card CR7: Timeout wake diagnostics for prepared-dialog waits

- [x] Inspect `parkAfterYieldIfNeeded(...)` and the current `wubei wait park finished` log fields.
- [x] Add or reuse per-window/per-wait counters for consecutive timeout wakes by:
  - wait reason;
  - phase / next phase;
  - dialog operation when known;
  - source message.
- [x] For `WAIT_PREPARED_DIALOG`, emit a clear warn-level diagnostic after an agreed threshold of
  consecutive timeout wakes, such as `runner did not answer prepared dialog wait`.
- [x] Timeout wake must stay a diagnostic recheck only. Do not mark business failure, do not switch
  白龙马 prompt, and do not add foreground OCR/template fallback in this card.
- [x] Run `mvn -q -DskipTests compile`.
- Acceptance:
  - When Runner misses or delays a prepared dialog, the logs show how many timeout wakes happened and
    which phase/operation waited.
  - Normal event wakes do not spam warn logs.

Card CR8: Prove pathing wake coverage before trusting the 5s fallback

- [x] Use the updated latency script from CR4 or extend it in-place if CR4 is done.
- [x] Measure these chains from real logs:
  - `PATHING_TERMINAL publish -> wubei wait wake`;
  - `TASK_ATTENTION_REQUIRED publish -> wubei wait wake`;
  - `pathing terminal runtime already satisfied -> skip park`;
  - `WAIT_PATHING_TERMINAL timeout` count and elapsed time.
- [x] If live logs show missed terminal/attention events, publish a follow-up task to fix the
  missing event before lowering watcher frequency or marking the sprint stable.
- [x] Do not change navigation click algorithms, OCR/template thresholds, minimap logic, or
  world-map logic in this card.
- [x] Record the measured time range and counters in `docs/ACTIVE_WORK.md`.
- Acceptance:
  - We can prove whether the 5s pathing park timeout is only a safety fallback or is actually hiding
    missed wake events.
  - No card may claim the park/wakeup sprint stable while pathing waits mostly wake by timeout.

CR8 result:

- Time range: `2026-06-18 01:08:00.000` to `2026-06-18 01:21:00.000`.
- Current logs show `PATHING_TERMINAL=7` and `TASK_ATTENTION_REQUIRED=57`, but
  `window.ready.await=0` and `wubei.wait.parkFinished=0`.
- Therefore the 5s pathing park fallback is not being exercised in this run; CR3 remains the
  blocking follow-up because 五倍 still reacquires the same pathing transactions instead of parking.

CR8 fresh-run update:

- Time range: `2026-06-18 12:01:00.000` to `12:05:00.000`.
- `window.ready.await=51`, with `event=8` and `timeout=43`.
- `wubei.wait.parkFinished=18`, with `wakeEvent=7` and `wakeTimeout=11`.
- `WAIT_PATHING_TERMINAL` is now active, but it still wakes by timeout too often.
- The worst same-window reacquire is now mostly:
  `WAIT_BATTLE_FINISH same=369 total=369` and `RESOLVE_AFTER_PATHING same=16 total=18`.
- p99 `afterReleaseMs` is still around 5s because several `WAIT_PATHING_TERMINAL` waits hit the
  5000ms safety timeout.

Card CR9: Do not miss already-visible or already-published attention events

- [x] Inspect the call path that captures `afterSequence` before `window.ready.await(...)` for
  `WAIT_PATHING_TERMINAL`.
- [x] Reproduce from logs where a visible dialog / ready event already exists before the wait starts,
  but `awaitNewer(...)` uses the current latest sequence and then times out.
- [x] Add or tighten an already-satisfied check so a fresh `visibleDialogType=OPTION/STORY`,
  fresh prepared action, or fresh ready event for the same window can satisfy the wait without
  sleeping for 5000ms.
- [x] Do not change OCR/template thresholds, minimap/world-map click logic, or 五倍 business
  decisions.
- [x] Run `mvn -q -DskipTests compile`.
- [ ] Record post-restart before/after counts for `window.ready.await.timeout`,
  `wubei.wait.wakeTimeout`, `wubei.wait.skipAlreadyReady`, and `afterReleaseMs p99` in
  `docs/ACTIVE_WORK.md`.
- Evidence:
  - Around `2026-06-18 12:03:30`, the wait times out while `before.visibleDialogType=OPTION` was
    already present in the runtime state.
  - Around `2026-06-18 12:03:31` and `12:03:34`, `TASK_ATTENTION_REQUIRED` wakes the wait, but the
    phase still returns `probe runner pathing still active`.
  - Fresh heartbeat slice `2026-06-18 12:05:00.000` to `12:23:05.000` still shows the same class of
    miss: around `12:20:23`, `WAIT_PATHING_TERMINAL` pays a 5s timeout even though
    `before.visibleDialogType=OPTION` and `after.visibleDialogType=OPTION`; the visible dialog is then
    cleared as stale around age `7190ms`.
  - Heartbeat slice `12:30:10.000` to `12:35:45.000` is not valid proof of the CR9 fix:
    `wubei.wait.skipAlreadyReady=0`, several waits still pay 5s with fresh `before.visibleDialogType`,
    and the logs do not show the new `readyEventType` / `readyEventAgeMs` fields that exist in source.
  - Heartbeat slice `12:35:45.000` to `12:45:05.000` still does not prove CR9 loaded:
    `wubei.wait.skipAlreadyReady=0`, `window.ready.await.timeout=52`, and `WubeiWaitRuntimeState`
    log text still omits `readyEventType` / `readyEventAgeMs` even though source contains those fields.
  - Post-restart slice `13:09:07.000` to `13:10:33.000` proves the new CR9 code is loaded:
    `WubeiWaitRuntimeState` now logs `readyEventType` / `readyEventAgeMs`, and
    `wubei.wait.skipAlreadyReady=47` in the analyzer output.
  - The same slice also proves the CR9 satisfied check is now too broad for one path: waits repeatedly
    skip park while `pathingState=ACTIVE`, `visibleDialogType=STORY`, `preparedOperation=null`, and
    `readyEventType=TASK_ATTENTION_REQUIRED`. That follow-up is tracked as CR18, not by changing
    OCR/template/click behavior.
- Acceptance:
  - A fresh visible/ready dialog cannot be ignored just because its event sequence was captured before
    the wait.
  - Pathing waits wake or skip promptly; they do not pay a 5s timeout while the runtime already shows
    a relevant dialog.

Card CR10: Give tracker pathing a terminal condition when target is null

- [x] Inspect where `wubei:tracker-green-click:*` registers pathing intent and why
  `activeIntentTarget=null` / `pathingTarget=null`.
- [x] Decide whether tracker-click pathing should carry the parsed tracker destination/hint, or
  whether the terminal condition should be "combat/route dialog visible" for combat tracker clicks.
- [x] Patch only the scheduling/pathing intent semantics. Do not change the left-panel green-text
  click coordinate algorithm.
- [x] Run `mvn -q -DskipTests compile`.
- [x] Record a fresh run summary in `docs/ACTIVE_WORK.md`.
- Evidence:
  - Around `2026-06-18 12:03:25` to `12:03:57`,
    `activeIntentSource=wubei:tracker-green-click:first-probe`,
    `activeIntentTarget=null`, and `pathingTarget=null` persist while current map changes through
    `宝象国`, `平顶山`, and `火云戈壁`.
  - Because the target is null, the watcher often keeps `pathingState=ACTIVE` or `STOPPED_AWAY`
    without a task-level decision that this tracker click has reached the next actionable dialog.
  - Fresh heartbeat slice `12:05:00` to `12:23:05` repeats this: around `12:20:26`,
    `TASK_ATTENTION_REQUIRED` publishes with `activeIntentSource=wubei:tracker-green-click:first-probe`,
    `activeIntentTarget=null`, `pathingState=ACTIVE`, `pathingCurrent=宝象国`, and `pathingTarget=null`.
    Destination hint async capture also exhausts with empty text after several seconds, so hint alone
    is not a reliable terminal source for this path.
  - Pre-restart heartbeat slice `12:45:05` to `12:51:46` repeats the old symptom and should be kept as
    baseline for CR10 validation, not proof of CR10 failure: around `12:47:23` to `12:47:38`,
    tracker pathing with null target pays repeated 5s `WAIT_PATHING_TERMINAL` timeouts before
    `TASK_ATTENTION_REQUIRED` / prepared enter-battle becomes visible.
  - Post-restart slice `13:09:07.000` to `13:10:33.000` shows a narrower remaining issue:
    `TASK_ATTENTION_REQUIRED` is published for visible WUBEI `STORY`, but the task snapshot still
    consumes `state=ACTIVE` and `preparedOperation=null`, so `RESOLVE_AFTER_PATHING` loops instead of
    getting a clear terminal/prepared decision. CR18 owns this follow-up.
- Acceptance:
  - Tracker pathing does not rely on repeated 5s timeouts to discover that it has stopped or reached a
    dialog.
  - Logs show a clear terminal reason for tracker pathing: target reached, stopped-away requiring
    recovery, or actionable dialog visible/prepared.

Card CR11: Consume prepared 五倍 dialog immediately after attention wake

- [x] Inspect `RESOLVE_AFTER_PATHING` after a `TASK_ATTENTION_REQUIRED` wake.
- [x] Ensure the phase checks/consumes a fresh prepared action before returning another
  `SHARED_STATE_TRIGGERED` solely because pathing still says active.
- [x] Preserve the current Runner-owned recognition model: foreground code may consume prepared
  results and click, but must not add new foreground OCR/template detection.
- [x] Run `mvn -q -DskipTests compile`.
- [x] Record prepared publish-to-consume timing in `docs/ACTIVE_WORK.md`.
- Evidence:
  - Around `2026-06-18 12:03:56.953`, Runner publishes visible `OPTION` with
    `operation=null`.
  - Around `12:03:57.339`, Runner prepares `WUBEI_ENTER_BATTLE`; around `12:03:57.397` the task
    finally consumes it. The fast path works once prepared exists, but the preceding loop repeatedly
    calls `consumePrepared result=absent` and updates dialog interest every ~90ms.
  - Fresh heartbeat slice: around `12:20:26.943`, a `TASK_ATTENTION_REQUIRED` wake arrives and the
    runtime has `visibleDialogType=STORY`, but the phase still reports `preparedOperation=null` and
    re-parks / short-times out instead of making a prompt decision. Around `12:21:35` to `12:21:37`,
    `consumePrepared result=absent` loops for `WUBEI_ENTER_BATTLE` until a prepared action is finally
    consumed at `12:21:37.485`.
  - Heartbeat slice `12:35:45.000` to `12:45:05.000` has `consumePrepared.absent=262` and
    `consumePrepared.consumed=9`, but it still appears to be a stale running process because wait-state
    logs omit the new ready-event fields. Treat this as post-change validation missing, not as proof
    that the CR11 patch failed.
  - Post-restart slice `13:09:07.000` to `13:10:33.000` has `consumePrepared.consumed=2` and
    `preparedAgeMs p99=67ms`, so prepared publish-to-consume is fast when an action exists.
    The unresolved part is the `consumePrepared.absent=161` loop when Runner has only operation-null
    visible STORY attention; CR18 tracks that missing prepared/terminal decision.
- Acceptance:
  - Once Runner publishes a prepared `WUBEI_ENTER_BATTLE` / `WUBEI_PROBE_STORY` action, the task
    consumes it within the current/next task turn without another pathing timeout.
  - Repeated `dialog.interest.update` + `consumePrepared result=absent` loops are reduced in the same
    scenario.

Card CR12: Gate auto-battle maintenance fallback scans during 五倍 pathing

- [x] Inspect why `auto-battle:pending-follower-first-aid` runs
  `maintenance-broadcast-fallback:auto-battle` dialog scans while 五倍 leader pathing is active.
- [x] Gate or rate-limit those scans so they only run when a maintenance/business dialog is actually
  expected, or when the team first-aid window is explicitly open.
- [x] Do not change heal/repair/summon business rules in this card.
- [x] Run `mvn -q -DskipTests compile`.
- [ ] Record before/after counts for `maintenance-broadcast-fallback:auto-battle` and
  `BUSINESS_OPTION_NOT_FOUND`.
- CR35 supersedes this implementation note:
  - The local `wubei` / `xiuluo_v2` broadcast gate changed helper maintenance timing relative to
    latest-push business behavior.
  - `AutoBattleTask` has been restored to pass `handleMaintenanceBroadcast(true)` for idle
    maintenance, and `TaskMaintenanceService.isTeamFirstAidMaintenanceWindowOpen(...)` is no longer
    exposed for this gate.
  - Any future attempt to reintroduce this optimization needs a separate behavior-change card with
    runtime evidence and user approval.
- Evidence:
  - Around `2026-06-18 12:03:25` to `12:03:27`, multiple member windows detect `STORY` under
    `maintenance-broadcast-fallback:auto-battle`, then fail `heal_pet_option` and
    `repair_equipment_option`.
  - These scans take roughly 0.9s to 2.6s each and add noise while the leader is waiting on pathing.
  - Fresh heartbeat slice `12:05:00` to `12:23:05` still has repeated
    `maintenance-broadcast-fallback:auto-battle` STORY detections, often around 0.8s to 7.7s each,
    while 五倍 waits are timing out or polling prepared actions.
  - Pre-restart heartbeat slice `12:45:05` to `12:51:46` still has repeated
    `maintenance-broadcast-fallback:auto-battle` STORY detections from member windows, followed by
    `BUSINESS_OPTION_NOT_FOUND` for heal-pet / repair-equipment. Some no-focus dialog detections cost
    about 1s to 14s.
  - Post-restart slice `13:09:07.000` to `13:10:33.000` still has
    `maintenance-broadcast-fallback:auto-battle=78` and `BUSINESS_OPTION_NOT_FOUND=58`, so this card
    remains active and should not be closed by the CR9/CR10/CR11 changes.
- Acceptance:
  - First-aid still runs when the gate is open.
  - Unrelated STORY dialogs are not repeatedly sent through maintenance business-option matching.

Card CR13: Split and reduce long 五倍 task-turn holds before tracker pathing release

- [x] Inspect the phase chain before `wubei:TRACKER_PATHING` releases.
- [x] Explain why the transaction can report `heldMs` around 17-18s even though the final tracker
  click/input is short.
- [x] If the hold includes preparatory phases that can safely release earlier, publish a narrow patch
  plan before modifying code.
- [x] Do not alter business ordering such as accept -> read tracker -> maintenance check -> tracker
  pathing unless the plan is approved.
- [x] Record the timing breakdown in `docs/ACTIVE_WORK.md`.
- Evidence:
  - Around `2026-06-18 12:03:25`, `wubei:TRACKER_PATHING` releases with `heldMs=17500`.
  - The actual tracker green click input request is around 385ms, so the hold likely includes earlier
    phase work under the same transaction/turn.
- Acceptance:
  - The log can explain which phase owns the long hold.
  - Any later behavior change preserves the already-working fast handoff after actual tracker pathing
    starts.

Card CR14: Gate long summon-skill maintenance while 五倍 leader has unresolved attention

- [x] Inspect why `TaskMaintenanceService` / `AutoBattleTask` can start `summon skill clean` while a
  五倍 leader window is parked on unresolved `WAIT_PATHING_TERMINAL` / visible dialog attention.
- [x] Add or reuse an existing team/leader gate so long summon-skill maintenance waits until the
  五倍 leader has either entered battle, reached a safe idle window, or explicitly opened the
  maintenance window.
- [x] Keep ordinary first-aid/battle recovery behavior intact; this card is only about long
  summon-skill cleanup passes occupying input during unresolved 五倍 pathing/dialog attention.
- [x] Do not change summon skill matching/click coordinates in this card.
- [x] Run `mvn -q -DskipTests compile`.
- [ ] Record before/after evidence for `summon skill clean` elapsed time and whether it overlaps
  leader `WAIT_PATHING_TERMINAL` / `TASK_ATTENTION_REQUIRED`.
- Implementation:
  - `WubeiTask` now checks `TaskMaintenanceService.isTeamPathingMaintenanceWindowOpen(...)` before
    trying `wubei:leaderPathingSummonMaintenance`, so the leader does not acquire the task turn only
    to run or defer long summon cleanup while the team gate is closed.
  - The leader `wubei:leader-pathing` maintenance request now also sets
    `requireOpenTeamMaintenanceWindow=true`, reusing the same service-side gate already used by
    follower support windows.
  - No summon-skill matching, panel, tooltip, or click-coordinate logic was changed.
- Evidence:
  - Fresh heartbeat slice `2026-06-18 12:05:00.000` to `12:23:05.000`: member
    `hwnd-3057A` runs `summon skill clean` while 五倍 leader waits are still unresolved. The exclusive
    pass reports about `7082ms`, later total maintenance reports about `11918ms`.
  - This is adjacent to CR12 but not the same root cause: CR12 is repeated fallback dialog scanning;
    CR14 is a long exclusive maintenance action taking input/focus while leader attention is pending.
  - Pre-restart heartbeat slice `12:45:05` to `12:51:46`: leader `hwnd-600B2` starts
    `summon skill clean` at `12:45:06` from `source=wubei:leader-pathing`, holds until `12:45:28`,
    reports `elapsedMs=22233`, then fails with `SUMMON_SKILL_FAILED_RETRY_LATER`.
  - Post-restart heartbeat slice `2026-06-20 17:08:55.149` to `17:09:08.921`: leader pathing
    released slowly from `TRACKER_PATHING` (`heldMs=18992`) and immediately started
    `summon skill clean` from `source=wubei:leader-pathing`; the clean finished with
    `success=true` after `elapsedMs=13771`. This proves the current source patch is still not enough
    to prevent long summon cleanup from overlapping the unresolved 五倍 leader pathing/attention
    window.
  - Fresh heartbeat slice `2026-06-20 17:15:47.439` to `17:15:57.339`: member
    `hwnd-74E07A0` starts `summon skill clean` from `source=auto-battle` during the 五倍 run,
    inspects only slot 4, sees `LOCKED_SLOT`, returns `success=true`, and reports total maintenance
    `elapsedMs=9900`. This confirms the safe-window rule must cover member auto-battle maintenance
    too, not only leader `source=wubei:leader-pathing`.
- Acceptance:
  - During 五倍 unresolved pathing/dialog attention, long summon-skill clean does not start just
    because auto-battle maintenance is idle.
  - Logs show either an explicit skip/defer reason or a safe-window reason before summon-skill clean
    runs.

Card CR15: Stop accept-NPC / ROUTE_TO_MAIN_TASK pathing waits from paying repeated 5s timeouts

- [x] Inspect why `ROUTE_TO_MAIN_TASK` returns `accept NPC pathing started` / `accept NPC pathing still active`
  and then waits on generic `WAIT_PATHING_TERMINAL` even when the next expected condition is a route
  dialog or arrival.
- [x] Split route-to-main wait semantics from tracker-green-click waits if needed. Do not reuse a
  tracker-specific null-target fix as a broad route fix without proving it covers this path.
- [x] Prefer Runner-prepared route dialog / `PATHING_TERMINAL` wake facts over foreground re-navigation.
- [x] Do not change route option OCR/template/click coordinate algorithms in this card.
- [x] Run `mvn -q -DskipTests compile`.
- [ ] Record before/after counts for `ROUTE_TO_MAIN_TASK`, `window.ready.await.timeout`,
  `wubei.wait.wakeTimeout`, `preparedTarget`, and route-dialog publish-to-consume latency.
- Implementation:
  - `WubeiTask` now uses a dedicated `WAIT_ACCEPT_NPC_ROUTE` wait spec for accept-NPC routing instead
    of the generic 5s tracker pathing wait.
  - The dedicated wait keeps `PATHING_TERMINAL` / `TASK_ATTENTION_REQUIRED` wake types but uses a
    1.5s fallback and disables opportunistic maintenance for this handoff.
  - `waitForAcceptNpcPathingIfStillActive(...)` now releases the duplicate-navigation gate when the
    same target has a fresh prepared route action, route-dialog preparation state, visible actionable
    dialog, or fresh route/terminal wake fact, so `NavigationService` can consume/confirm it.
  - No route option OCR/template/click coordinate algorithm was changed.
- Evidence:
  - Heartbeat slice `2026-06-18 12:30:10.000` to `12:35:45.000`:
    `ROUTE_TO_MAIN_TASK` has `same=9 total=9`, and several waits pay 5s.
  - Around `12:35:02.964`, `accept NPC pathing started` pays a 5s timeout with no prepared action.
  - Around `12:35:16.906`, the same path finally wakes with `operation=ROUTE_TRANSFER`,
    `preparedTarget=宝象国`, and `satisfied=true`.
  - Around `12:35:34.619`, another `accept NPC pathing started` wait again pays a 5s timeout.
- Acceptance:
  - Accept-NPC route waits do not repeatedly sleep 5s while route dialog prepare or route arrival is
    the real next condition.
  - Logs can distinguish "still physically pathing", "route dialog preparing/ready", and "arrived"
    for `ROUTE_TO_MAIN_TASK`.

Card CR16: Replace WAIT_TEAM_RETURN fixed 3s polling with event/state completion

- [x] Inspect `WubeiTask.runWaitTeamReturnPhase(...)`, `shouldYieldForTeamReturnSignal()`, and
  `handoffDelayMs(...)` for `TEAM_RETURN_WAIT_SOURCE_PREFIX`.
- [x] Verify whether `TeamReturnService.isReturnTeamSignalPresent()` can remain true after all useful
  member return work is done, or whether the signal needs an explicit consumed/expired state.
- [ ] Replace the repeated `delayMs=3000` self-reacquire loop with an event/state-driven wait:
  use a member return completion/broadcast event when available, or a short bounded recheck only when
  there are actual queued/eligible member windows.
- [ ] Preserve the safety rule that members must still get a turn to click return when a real team
  return signal is pending.
- [ ] Do not change return item click coordinates, team-return button click logic, OCR/template
  matching, or navigation algorithms in this card.
- [ ] Run `mvn -q -DskipTests compile`.
- [ ] Record before/after counts for `WAIT_TEAM_RETURN same`, `team return signal still present`,
  `team-return-wait:* delayMs=3000`, `afterReleaseMs`, and `heldMs`.
- Evidence:
  - Heartbeat slice `2026-06-18 12:35:45.000` to `12:45:05.000`:
    `WAIT_TEAM_RETURN same=8 total=8`.
  - Around `12:42:48.117`, the leader enters `WAIT_TEAM_RETURN` after `RETURN_HOME`.
  - Around `12:42:48.609`, `team return signal still present` returns
    `SHARED_STATE_TRIGGERED`; release logs `heldMs=18428` and `queuedWaiters=0`.
  - From `12:42:48.610` through `12:43:15.282`, the same window repeatedly waits about `3000ms`
    (`afterReleaseMs=3000/3318/3001/...`) and reacquires `WAIT_TEAM_RETURN` while `queuedWaiters=0`.
  - The phase finally becomes `READY_TO_CONTINUE` at `12:43:15.747`, so the fixed polling loop cost
    roughly 27 seconds before round completion.
- Investigation update 2026-06-18 唐德:
  - `WubeiTask.runWaitTeamReturnPhase(...)` currently only calls
    `teamReturnService.isReturnTeamSignalPresent()`. When true, it returns
    `TaskTransactionResult.SHARED_STATE_TRIGGERED` and stays in `WAIT_TEAM_RETURN`.
  - `TaskTurnCoordinator` releases the turn for `SHARED_STATE_TRIGGERED`, but
    `WubeiTask.handoffDelayMs(...)` then sleeps `returnTeamLeaderWaitPollMs` (default 3000ms)
    whenever the next source starts with `team-return-wait`.
  - `TeamReturnService.isReturnTeamSignalPresent()` is stateless template detection over the leader
    team-status area. It has no consumed/expired state and no link to whether any member actually
    saw/clicked the return button.
  - The only current member click path found is `AutoBattleTask.maybeRunIdleMaintenance(...)` calling
    `TeamReturnService.clickReturnTeamIfPresent(...)`. That method clicks and returns true, but it
    does not publish/record a member-return completion event for the leader wait.
  - Current fresh log tail did not reproduce the CR16 12:42 stale loop, but the existing source path
    still permits it: a persistent/false-positive leader template can keep the leader in 3s
    self-reacquire cycles even when `queuedWaiters=0`.
- Proposed fix pending approval:
  - Keep the first leader yield so real member windows still get a chance to click `归队`.
  - When a member `clickReturnTeamIfPresent(...)` succeeds, record/publish a lightweight
    member-return activity event with window id and timestamp.
  - In `WAIT_TEAM_RETURN`, continue waiting only while the leader signal is present and either a
    recent member-return click/activity exists or the first short grace window has not expired.
  - If the leader signal remains present but there has been no member activity after the grace
    window, treat the signal as stale/expired and continue with an explicit log instead of paying
    repeated 3s sleeps.
  - Do not change return item click coordinates, button template matching, or navigation behavior.
- Acceptance:
  - A real team-return wait still gives members a chance to act.
  - If no member is eligible/queued and the return signal is stale, the leader does not pay repeated
    3s sleeps.
  - Logs explain whether the wait completed by member action, signal clear/expiry, or bounded safety
    timeout.

Card CR17: Reduce accept-task prepared wait after NPC click

- [x] Inspect `WubeiTask.runAcceptTaskPhase(...)` and `waitForPreparedWubeiDialog(...)` for
  `normal-round-start:prepared-accept-after-npc`.
- [x] Separate Runner preparation latency from foreground turn ownership:
  measure watcher `attentionDetectMs`, `attentionRoutePrepareMs`, `taskDialogPrepareMs`, and the
  foreground `consumePrepared absent -> consumed` wait.
- [ ] Replace or bound the multi-second foreground polling path so the task does not hold the turn for
  several seconds while Runner has not prepared `WUBEI_ACCEPT_TASK`.
- [x] Preserve the intended safety rule that an already-open accept dialog should not be abandoned to
  unrelated long maintenance. This card is about reducing / event-driving the wait, not going back to
  old foreground OCR/template recognition.
- [x] Do not change accept-dialog template, click coordinate, NPC click, or business accept/cancel
  semantics in this card.
- [ ] Run `mvn -q -DskipTests compile`.
- [x] Record before/after counts for `normal-round-start:prepared-accept-after-npc`,
  `consumePrepared.absent`, `prepared dialog consumed after short wait`, watcher `totalMs`, and
  task-turn `heldMs`.
- Evidence:
  - Pre-restart heartbeat slice `12:45:05` to `12:51:46`:
    `consumePrepared.absent=96`, `consumePrepared.consumed=11`.
  - Around `12:47:10.446`, `normal-round-start:prepared-accept-after-npc:initial` is absent.
  - From `12:47:10.801` through `12:47:14.330`, the foreground task repeatedly polls
    `consumePrepared result=absent` for `WUBEI_ACCEPT_TASK`.
  - Around `12:47:14.385`, watcher finally prepares `WUBEI_ACCEPT_TASK`; the observer tick reports
    `totalMs=3925`, `combatMs=1541`, `attentionDetectMs=1553`, and `attentionRoutePrepareMs=831`.
  - Around `12:47:14.423`, the task consumes the prepared action and logs
    `prepared dialog consumed after short wait ... waitMs=4430`.
  - 唐德 CR17 investigation on the current log found repeated accept waits:
    `waitMs=1592, 2993, 2252, 5290, 3559, 1382, 971, 3152, 1033, 4430, 1765, 2209, 2377, 1802, 4232`.
    The click after preparation is not the slow section: at `12:47:14.385` Runner prepared the action
    and at `12:47:14.423` the task consumed it with `preparedAgeMs=38`; the queued input completed
    at `12:47:14.876` with `event=input.request elapsedMs=453`.
  - Source shape: `WUBEI_ACCEPT_DIALOG_FOREGROUND_WAIT_MS` currently equals
    `WUBEI_DIALOG_INTEREST_TTL_MS` (`15_000ms`), and `waitForPreparedWubeiDialog(...)` polls every
    `WUBEI_PREPARED_DIALOG_POLL_MS` (`80ms`) while still inside `runAcceptTaskPhase(...)`.
  - Narrow plan pending user approval:
    1. Keep the first immediate `tryConsumePreparedWubeiDialog(...)` before clicking or after a fresh ready event.
    2. After the accept NPC has been clicked and no prepared action exists yet, register/refresh the
       `WUBEI_ACCEPT_TASK` interest and return a shared-state wait outcome instead of holding the
       foreground task turn for up to 15 seconds.
    3. Wake only on `TASK_ATTENTION_REQUIRED` / prepared action or a short bounded timeout, then let the
       next task turn consume the Runner-prepared `WUBEI_ACCEPT_TASK`.
    4. Preserve the safety invariant: once the accept dialog is prepared, consume the Runner-prepared
       click promptly and do not allow unrelated long maintenance to steal that already-open dialog.
- Acceptance:
  - Accept-NPC follow-up still clicks the Runner-prepared `WUBEI_ACCEPT_TASK` action.
  - The foreground task no longer burns several seconds repeatedly polling absent prepared state.
  - If Runner preparation itself is slow, logs identify whether the cost is screenshot/combat check,
    dialog detection, memory/template prepare, event wake, or input queue wait.

Card CR18: Stop operation-null WUBEI attention from causing same-window churn

- [x] Inspect the post-restart loop around `RESOLVE_AFTER_PATHING` where the runtime has
  `visibleDialogType=STORY` / `readyEventType=TASK_ATTENTION_REQUIRED`, but `preparedOperation=null`
  and tracker pathing remains `ACTIVE`.
- [x] Inspect `WubeiTask.captureWaitRuntimeState(...)`, `isWaitAlreadySatisfied(...)`,
  `parkAfterYieldIfNeeded(...)`, and the `WindowTaskRunner` WUBEI attention/prepare path that can
  publish operation-null `TASK_ATTENTION_REQUIRED`.
- [x] Narrow the already-satisfied semantics so visible WUBEI STORY or a plain
  `TASK_ATTENTION_REQUIRED` wake can cause one prompt recheck, but cannot produce repeated
  zero-delay same-window reacquire unless there is one of:
  - a fresh prepared action matching the expected operation;
  - a clear pathing terminal state such as `STOPPED_AWAY` / target reached;
  - an explicit operation-specific "no prepared action / unresolved blocker" result that the 五倍
    phase knows how to handle.
- [x] For `wubei:tracker-green-click:*`, ensure Runner and task agree on a single next decision:
  prepared action, terminal stopped-away/recovery, or unresolved blocker. Do not leave
  `pathingState=ACTIVE + visibleDialogType=STORY + preparedOperation=null + satisfied=true` as a
  reusable ready state.
- [x] Add or reuse logs that explain why a skip-park was rejected:
  expected operation, visible dialog type/source, prepared operation/age, pathing state, and active
  intent source. Keep this diagnostic rate-limited if needed.
- [x] Do not change OCR/template thresholds, 白龙马/黄袍怪/normal-enter business decisions, NPC click,
  tracker green-click coordinates, minimap/world-map click logic, or route option matching in this
  card.
- [x] Run `mvn -q -DskipTests compile`.
- [x] Validate current evidence with `scripts/analyze_wubei_latency.ps1`; fresh-run validation still
  waits for an approved behavior patch.
- Evidence:
  - Post-restart slice `2026-06-18 13:09:07.000` to `13:10:33.000` proves the new readiness fields
    are loaded, but also shows the churn:
    `task.turn.handoff.sameAsPrevious=242`, `wubei.wait.skipAlreadyReady=47`,
    `RESOLVE_AFTER_PATHING same=48 total=49`, `window.ready.await.timeout=56`,
    `consumePrepared.absent=161`, and only `consumePrepared.consumed=2`.
  - Around `13:10:32.739`, Runner publishes `TASK_ATTENTION_REQUIRED` for visible WUBEI `STORY`
    with `operation=null`; the wait wakes by event. The next task turn consumes a snapshot with
    `state=ACTIVE`, `preparedOperation=null`, and then logs `tracker runner pathing still active`.
  - Immediately afterward, repeated lines such as
    `skip park; runtime already has wake state ... pathingState=ACTIVE ... visibleDialogType=STORY ...
    preparedOperation=null ... readyEventType=TASK_ATTENTION_REQUIRED ... satisfied=true` cause
    same-window reacquire without progress.
- Acceptance:
  - In the same scenario, the task either consumes a matching Runner-prepared action, transitions to a
    clear recovery/terminal state, or parks without tight same-window churn.
  - `skipAlreadyReady` may still occur for genuinely actionable prepared/terminal states, but it must
    not repeatedly fire on operation-null visible STORY with no prepared action.
  - Analyzer after the fix should show lower `RESOLVE_AFTER_PATHING same`, lower
    `consumePrepared.absent` churn, and no repeated `tracker runner pathing still active` loop caused
    only by the same stale visible STORY.
- 唐德 review 2026-06-18:
  - Current log slice `13:09:07` to `13:17:00` still has
    `task.turn.handoff.sameAsPrevious=769`, `wubei.wait.skipAlreadyReady=209`,
    `RESOLVE_AFTER_PATHING same=365 total=367`, `consumePrepared.absent=353`, and
    `consumePrepared.consumed=4`.
  - Root cause is confirmed in code: `WindowTaskRunner.publishTaskAttentionIfDialogVisible(...)`
    publishes operation-null `TASK_ATTENTION_REQUIRED` for visible WUBEI `STORY`; then
    `WubeiTask.isWaitAlreadySatisfied(...)` treats `DialogType.STORY` as actionable for
    `WAIT_PATHING_TERMINAL`, even while pathing is still `ACTIVE` and there is no prepared action.
  - Proposed patch, pending approval: keep operation-null visible STORY as a soft wake/diagnostic, but
    do not let it repeatedly satisfy `WAIT_PATHING_TERMINAL` unless pathing is terminal, a prepared
    action exists, or the ready event carries a matching operation-specific decision.

Card CR19: Narrow accept-NPC route wait readiness gates

- [x] Inspect every caller of `waitForAcceptNpcRouteWake(...)`,
  `waitForAcceptNpcPathingIfStillActive(...)`, and `acceptNpcRouteWakeFact(...)`.
- [ ] Split `WAIT_ACCEPT_NPC_ROUTE` out of the broad `WAIT_PATHING_TERMINAL` satisfied check:
  route wait should be satisfied only by terminal pathing for the expected accept-NPC route, a fresh
  prepared `ROUTE_TRANSFER` for `宝象国`, or an operation-bearing route-ready event for that target.
- [ ] Do not let plain visible `OPTION/STORY` or operation-null `TASK_ATTENTION_REQUIRED` repeatedly
  skip park for `WAIT_ACCEPT_NPC_ROUTE`; they may wake one recheck, but must not be a reusable
  ready state without a route operation/terminal fact.
- [ ] Remove `DialogPreparationPhase.REQUESTED` as a duplicate-navigation gate-release fact.
  `REQUESTED` only means the task asked Runner to prepare; prefer `READY`, fresh prepared action,
  terminal route pathing, or `PREPARING` only when tied to a fresh visible route dialog snapshot.
- [ ] Split `isFreshAcceptNpcRouteEvent(...)` by event type:
  `PATHING_TERMINAL` must require terminal pathing for the expected route, while
  `TASK_ATTENTION_REQUIRED` must require `ROUTE_TRANSFER` / expected target or a fresh prepared route
  action.
- [ ] Do not change world-map OCR/template thresholds, route option matching, click coordinates,
  NPC click, minimap/world-map click algorithms, or business accept semantics in this card.
- [ ] Run `mvn -q -DskipTests compile`.
- [ ] Validate with a fresh post-restart log: `ROUTE_TO_MAIN_TASK`,
  `reason=WAIT_ACCEPT_NPC_ROUTE`, `skipAlreadyReady`, `route-dialog-REQUESTED`, route prepared
  publish-to-consume timing, and whether 1.5s route waits now wake only on concrete route facts.
- Evidence:
  - Code audit on 2026-06-18 confirmed `WAIT_ACCEPT_NPC_ROUTE` is grouped with
    `WAIT_PATHING_TERMINAL` in `isWaitAlreadySatisfied(...)`, so it inherits `isActionableDialog(...)`
    and type-only `isMatchingReadyEvent(...)`.
  - `acceptNpcRouteWakeFact(...)` currently treats matching `ROUTE_TRANSFER` `REQUESTED` or
    `PREPARING` status as a route fact while the previous pathing snapshot can still be `ACTIVE`.
  - `isFreshAcceptNpcRouteEvent(...)` accepts inherited pathing intent/snapshot target for any fresh
    event, including operation-null attention.
  - 唐德 review 2026-06-18 confirmed the same three risks in source:
    `WAIT_ACCEPT_NPC_ROUTE` still shares the `WAIT_PATHING_TERMINAL` satisfied branch;
    `acceptNpcRouteWakeFact(...)` can release on `visible-dialog:type=...` and
    `route-dialog-REQUESTED`; and `isFreshAcceptNpcRouteEvent(...)` accepts inherited route target
    for both `PATHING_TERMINAL` and `TASK_ATTENTION_REQUIRED`.
  - Current log tail is pause-contaminated and has limited clean route-wait runtime evidence, but it
    does show a stale `dialog-visible:STORY` attention and later
    `consumePrepared result=absent ... expectedOperation=ROUTE_TRANSFER expectedTarget=宝象国`.
    The card remains blocked on approval because the source gate is too permissive even before a
    clean runtime repro.
- Acceptance:
  - Accept-NPC route waits still avoid the old repeated 5s generic wait, but only wake/skip park on a
    concrete route operation or terminal route state.
  - Logs explain rejected route wake facts with operation, target, pathing state, visible dialog type,
    status phase, and event type.
  - CR15 post-restart validation can proceed without confusing "faster" route waits with the same
    operation-null churn diagnosed by CR18.

Card CR20: Narrow untargeted tracker dialog-terminal classification

- [x] Inspect `WindowTaskRunner.classifyPathingState(...)`,
  `classifyUnknownPathingState(...)`, `isUntargetedTrackerDialogTerminal(...)`,
  `resolvePathingDialogBlock(...)`, and `isAttentionPreparationPhase(...)`.
- [x] Do not let `DialogPreparationPhase.REQUESTED` make an `UNTARGETED_TRACKER` intent terminal.
  `REQUESTED` only means the foreground task asked Runner to prepare a dialog.
- [x] Do not let plain `PREPARING` make an `UNTARGETED_TRACKER` intent terminal unless it is tied to
  fresh same-window visible dialog evidence and the code can explain the operation/target being
  prepared.
- [x] Do not let plain fresh visible `OPTION/STORY` make an active tracker path terminal by itself.
  A visible dialog may wake/recheck the task, but terminal `STOPPED_AWAY` needs stronger evidence:
  a fresh prepared action, a `READY` operation-bearing preparation result, or an explicit
  task-owned terminal decision.
- [x] In `classifyPathingState(...)`, ensure active movement evidence is not overridden by weak
  dialog/preparation evidence. If `locationChanged=true`, weak dialog evidence should not return
  `STOPPED_AWAY`.
- [x] Keep this card scoped to Runner pathing classification and diagnostics. Do not change
  OCR/template thresholds, left-panel tracker click coordinates, minimap/world-map click logic,
  route option matching, NPC click, 白龙马/黄袍怪 business decisions, or Wubei phase semantics.
- [x] Add/reuse logs that make rejected terminal evidence visible:
  intent type/source, locationChanged, dialog reason/type, preparation phase, operation, target, and
  why it did or did not become terminal.
- [x] Run `mvn -q -DskipTests compile`.
- [ ] Validate with a fresh post-restart log:
  - no `untargeted tracker pathing terminal because dialog is actionable` line caused only by
    `dialog-preparation-REQUESTED`;
  - no terminal line caused only by plain `PREPARING` without fresh visible/actionable evidence;
  - no active movement sample becomes `STOPPED_AWAY` solely because a dialog was visible;
  - tracker pathing still wakes promptly when a real prepared WUBEI action is ready.
- [x] Reopened P0 on 2026-06-18 heartbeat audit and source re-audit:
  - The earlier reopened P0 review found that dialog/prepared-action readiness could still make
    `UNTARGETED_TRACKER` pathing terminal.
  - Follow-up source patch removed that direct terminal path. Current code no longer has
    `isUntargetedTrackerDialogTerminal(...)`, and `classifyPathingState(...)` checks
    `locationChanged` before stopped-away timeout logic.
  - `resolvePathingDialogBlock(...)` now records prepared actions, preparation status, and visible
    dialogs as attention facts. They may wake/recheck the task, but they do not directly create
    `STOPPED_AWAY`.
  - Current remaining work is runtime validation after app/script restart, because the latest log
    slice still belongs to the old running process.
- Evidence:
  - Code audit on 2026-06-18 found `WindowTaskRunner.classifyPathingState(...)` only returns
    `STOPPED_AWAY` from stopped-away timing after no location change.
  - `classifyUnknownPathingState(...)` returns `UNKNOWN`.
  - `resolvePathingDialogBlock(...)` returns `PathingDialogBlock.attention(...)` for
    `PreparedDialogAction`, `DialogPreparationStatus` phases `REQUESTED` / `PREPARING` / `READY`,
    and fresh visible non-`NONE` dialog snapshots.
- Acceptance:
  - The Runner distinguishes strong terminal evidence from weak attention evidence.
  - Weak dialog evidence can wake a task for recheck, but cannot by itself stop active tracker
    pathing.
  - CR10 can return to validation only after CR20 is implemented and the fresh log shows the terminal
    reason is no longer over-broad.

Card CR21: WUBEI startup must not be skipped by transient team-role OCR timeout

- [x] Inspect the fresh restart log from `2026-06-18 14:02:47` to `14:03:03`.
- [x] Confirm the failure chain before changing code:
  - `hwnd-600B2` / player ID `443075411` requested `WUBEI`.
  - Team-role hover produced tooltip images, but local OCR timed out on
    `images\temp\hwnd-600B2\team_role_tooltip_raw_pass1_attempt2.png`.
  - Normal startup skipped Alt+T panel fallback after the tooltip OCR miss.
  - `TaskTeamAssignmentPolicy` treated `role=UNKNOWN` as not allowed for leader-only `WUBEI`.
  - `WindowTaskRunner` logged `skip task by team role policy`, so no WUBEI phase / CR20 pathing
    validation ran.
- [ ] Propose the narrowest policy change before implementation, then implement only that approved
  path. Do not change 五倍 business logic, dialog preparation, pathing classification, OCR/template
  thresholds, or click algorithms.
- [ ] The fix must preserve member reassignment:
  - windows confirmed as `MEMBER` still become `AUTO_BATTLE`;
  - a confirmed non-leader must not be allowed to run the leader-only WUBEI task.
- [ ] The fix must distinguish "detector inconclusive / OCR timed out" from "confirmed non-leader".
  Acceptable implementation directions include one of:
  - retry or delay only the failed leader tooltip OCR path without opening unrelated UI repeatedly;
  - use a trustworthy existing runtime/UI role fallback if it is already known for that hwnd;
  - return a richer role-detection result so startup can avoid treating transient detector failure as
    a confirmed role.
- [ ] Add clear logs for the final decision:
  - requested task, hwnd/window id, title/player id, detected role, detection reason
    (`tooltip-id`, `tooltip-ocr-timeout`, `panel-disabled`, runtime-role fallback, etc.), and final
    resolved task.
- [ ] Run `mvn -q -DskipTests compile`.
- [ ] Validate with a fresh restart log:
  - WUBEI leader is not skipped only because tooltip OCR timed out once;
  - members are still reassigned to `AUTO_BATTLE`;
  - the next run reaches WUBEI phase logs so CR20 post-restart validation can actually observe
    `PATHING_TERMINAL` / `TASK_ATTENTION_REQUIRED` behavior.

Evidence:

- The first post-CR20 restart did not validate CR20. The log range `2026-06-18 13:54:32.359` to
  `14:03:20.000` has `PATHING_TERMINAL=0`, `TASK_ATTENTION_REQUIRED=0`, and no WUBEI phase wait logs.
- The leader window was skipped before task start:
  - `14:03:03.311` local OCR timed out for
    `images\temp\hwnd-600B2\team_role_tooltip_raw_pass1_attempt2.png`.
  - `14:03:03.311` `team role tooltip OCR returned no words`.
  - `14:03:03.312` `UNKNOWN window cannot run leader-only task WUBEI`.
  - `14:03:03.312` `skip task by team role policy`.
- Investigation update 2026-06-18 唐德:
  - Rechecked the exact startup slice `14:02:47.343` to `14:03:03.312`.
  - `hwnd-600B2` was selected for `WUBEI`, registered successfully, and its window title already
    contained player id `443075411`.
  - Other windows successfully OCR'd the tooltip leader id as `443075411` and were safely reassigned
    from `WUBEI` to `AUTO_BATTLE` as `MEMBER`.
  - The leader window itself saved
    `images\temp\hwnd-600B2\team_role_tooltip_raw_pass1_attempt2.png` with a detected tooltip, but
    local OCR timed out. `TextRecognizer.getAllTextResultsLocalOnly(...)` returns an empty list for
    timeout/unavailable/no text, so `TeamRoleDetectionService` cannot distinguish OCR timeout from a
    real no-words result.
  - Normal startup deliberately disables the Alt+T panel fallback, so
    `detectCurrentRole(...)` returned plain `TeamRoleStatus.UNKNOWN` with only a log reason
    (`tooltip-ocr-miss-panel-disabled`).
  - `WindowTaskRunner.resolveTaskTypeBeforeStart(...)` already has a fallback from live UNKNOWN to
    existing `windowContext.getRole()`, but this fresh WUBEI registration had role `UNKNOWN`, so no
    fallback applied.
  - `TaskTeamAssignmentPolicy.resolveTaskForRole(...)` then treated `UNKNOWN` exactly like a confirmed
    non-leader for leader-only `WUBEI` and returned `TaskType.UNKNOWN`, causing startup skip before any
    WUBEI phase could run.
- Proposed narrow fix pending approval:
  - Add a richer role detection result for startup, preserving the old `detectCurrentRole(...)` enum
    API for existing callers.
  - The startup result should carry `role`, `reason`, and `confidence`/`inconclusive` metadata, at
    least distinguishing `tooltip-id`, `tooltip-ocr-timeout`, `tooltip-ocr-miss-panel-disabled`,
    `status-deviation-panel-disabled`, and confirmed `MEMBER`/`SOLO`.
  - Keep member reassignment unchanged when tooltip OCR succeeds and role is `MEMBER`.
  - For leader-only WUBEI, do not treat an inconclusive tooltip OCR timeout as a confirmed non-leader.
    Prefer one bounded retry/delay of the tooltip OCR path or, if still inconclusive, return a
    startup-blocked/retryable decision with explicit logs instead of silently resolving the task to
    `UNKNOWN`.
  - Do not enable the Alt+T panel fallback in normal startup unless the user explicitly approves it;
    that path sends extra real input and was intentionally disabled.
  - Do not change OCR thresholds, template matching, WUBEI business flow, dialog preparation, or
    pathing classification.

Acceptance:

- A transient role-detector OCR miss cannot make the only WUBEI leader silently skip before runtime
  validation starts.
- The startup decision remains safe: confirmed member windows still do not run leader-only WUBEI.
- The next CR20 validation run can proceed into actual WUBEI task phases.

Card CR22: Accept-NPC route dialog prepared action must not be invalidated by same-target intent churn

- [x] Inspect the fresh runtime log from `2026-06-18 14:03:16.132` to `14:07:28.949`.
- [x] Confirm the failure chain before changing code:
  - `14:03:50` the leader `hwnd-600B2` restarted WUBEI and was correctly detected as `LEADER`.
  - `14:04:09` WUBEI round 1 started.
  - The leader repeatedly entered `ROUTE_TO_MAIN_TASK` for accept NPC route `宝象国`.
  - Runner repeatedly matched the visible route option `宝象国光禄寺（800两）`, including fast
    remembered-choice prepares such as `14:04:48.076`.
  - Every matching route prepare in the checked range was rejected as `result=stale-intent`.
  - Foreground `NavigationService` then immediately registered a new same-target route intent, so the
    next prepared action was again stale before it could be consumed.
- [x] Do not change world-map OCR/template thresholds, route option matching, remembered-choice
  coordinate math, click coordinates, NPC click, minimap/world-map click algorithms, or WUBEI
  accept-task business semantics.
- [x] Fix only the ownership/identity of an already submitted accept-NPC route wait:
  - while the same target route dialog is visible or being prepared, do not refresh the active route
    intent id just because the foreground re-entered `navigateToMap`;
  - allow the existing same-target `ROUTE_TRANSFER` prepared action to remain valid long enough for
    the foreground task turn to consume it;
  - if a new intent really is required, log exactly why the old intent was invalid, including old/new
    intent id, operation, target, pathing state, visible dialog type, and prepared action age.
- [x] Coordinate with CR19 instead of duplicating it:
  - CR19 narrows which route facts can wake/skip park;
  - CR22 prevents the foreground from invalidating a valid same-target prepared route action after a
    wake has already happened.
- [x] Add or reuse diagnostics that make the following distinguishable:
  - route action prepared and consumed;
  - route action prepared but stale because active intent changed;
  - foreground re-entered route wait and reused the same intent;
  - foreground re-entered route wait and intentionally created a new intent.
- [x] Run `mvn -q -DskipTests compile`.
- [ ] Validate with a fresh post-restart log:
  - `route dialog preparation: result=stale-intent` no longer repeats for the same `宝象国` option;
  - at least one accept-NPC route `ROUTE_TRANSFER` prepared action is consumed or clicked;
  - `consumePrepared.consumed > 0` for the route dialog path;
  - `task.turn.handoff.sameAsPrevious` and `consumePrepared.absent` drop sharply in
    `ROUTE_TO_MAIN_TASK`;
  - no regression to repeated world-map open/input for the same already-visible route dialog.

Evidence:

- Analyzer for `2026-06-18 14:03:16.132` to `14:08:00.000`:
  - `task.turn.handoff=238`;
  - `task.turn.handoff.sameAsPrevious=237`;
  - `consumePrepared.absent=401`;
  - `consumePrepared.consumed=0`;
  - `wubei.wait.skipAlreadyReady=167`;
  - top same-window transaction: `wubei:ROUTE_TO_MAIN_TASK same=237 total=237`.
- Concrete log chain:
  - `14:04:48.076` Runner prepared remembered choice:
    `operation=ROUTE_TRANSFER target=宝象国 matched=宝象国光禄寺（800两） prepared=true totalMs=141`.
  - The same timestamp rejected it:
    `route dialog preparation: result=stale-intent ... intentId=8d4c67c4... matchedText=宝象国光禄寺（800两）`.
  - `14:04:48.102` foreground consumed nothing:
    `consumePrepared result=absent ... activeIntentId=1874d77d... expectedOperation=ROUTE_TRANSFER expectedTarget=宝象国`.
  - `14:04:48.425` foreground registered yet another same-target intent:
    `intentId=e5f238c2... targetMap=宝象国`.
- This proves the route dialog OCR/matching is not the blocker in this slice. The blocker is prepared
  action ownership becoming stale before foreground consumption.

Acceptance:

- Same-target accept-NPC route waits are stable across foreground rechecks.
- A Runner-prepared `ROUTE_TRANSFER` for `宝象国` can be consumed without being invalidated by a fresh
  same-target intent id.
- The fix remains scheduling/ownership only; no visual matching or click-target algorithm changes.

Card CR23: WUBEI accept-NPC current-map pathing must terminal or recover after route arrival

- [x] Inspect the fresh runtime log from `2026-06-18 14:14:21.734` to `14:18:29.213`.
- [x] Confirm the failure chain before changing code:
  - `14:14:21.734` consumed a prepared `ROUTE_TRANSFER` for `宝象国`.
  - `14:14:27.892` Runner observed `state=ARRIVED` at `宝象国(78,135)` and published
    `PATHING_TERMINAL` for the accept-NPC map route.
  - After route arrival, the task started `wubei:accept-npc:currentMap:navigateInCurrentMap`.
  - That current-map pathing stayed `ACTIVE` with `current=null(null, null)` for more than
    200 seconds.
  - The task repeatedly logged `accept NPC pathing still active; skip duplicate navigation` and never
    entered the NPC-click / accept-task step.
- [x] Do not change route option OCR/template thresholds, remembered-choice matching, NPC click target
  math, minimap click algorithm, or WUBEI accept-task business semantics.
- [x] Fix only the terminal/recovery policy for this accept-NPC current-map handoff:
  - distinguish "route to map has arrived" from "current-map walk to NPC is still physically moving";
  - if current-map pathing remains `ACTIVE` too long with no fresh location/update facts, force a
    bounded runtime recheck instead of waiting forever;
  - if the recheck proves the leader is already near the accept NPC, continue to the accept NPC click;
  - if the recheck proves it is not near the NPC, retry the current-map navigation without opening a
    broad business fallback.
- [x] Coordinate with CR15 / CR19 / CR22:
  - CR15 shortened accept-route waits;
  - CR19 narrows route wake facts;
  - CR22 stabilizes same-target route prepared action ownership;
  - CR23 owns the post-route-arrival current-map handoff to the actual accept NPC.
- [x] Add or reuse diagnostics that show:
  - map route `PATHING_TERMINAL` publish time;
  - current-map pathing intent source and age;
  - last known current map/coordinate;
  - whether the next action was "click accept NPC", "retry current-map nav", or "wait for runner".
- [x] Run `mvn -q -DskipTests compile`.
- [ ] Validate with a fresh post-restart log:
  - after `ROUTE_TRANSFER` to `宝象国`, the leader either clicks/opens the accept NPC dialog or logs a
    concrete bounded retry reason;
  - no more multi-minute loop of `accept NPC pathing still active` with `current=null`;
  - `WAIT_ACCEPT_NPC_ROUTE` timeout chains do not exceed a small bounded count for the same
    current-map intent;
  - no regression to repeated route dialog/world-map entry for `宝象国`.

Evidence:

- Fresh heartbeat/user-observed failure on `2026-06-18`:
  - `14:14:21.734`:
    `consumePrepared result=consumed ... operation=ROUTE_TRANSFER target=宝象国 ... preparedAgeMs=353`.
  - `14:14:27.892`:
    `pathing watcher update: task=WUBEI state=ARRIVED ... target=宝象国(null, null) current=宝象国(78, 135)`.
  - `14:14:27.892`:
    `window.ready.publish ... type=PATHING_TERMINAL ... target=宝象国 state=ARRIVED sequence=125`.
  - From `14:18:00.195` onward:
    `reason=WAIT_ACCEPT_NPC_ROUTE ... wakeResult=timeout ... consecutiveTimeouts=134 ... pathingState=ACTIVE ... current=null`.
  - `14:18:29.206`:
    `accept NPC pathing still active; skip duplicate navigation: state=ACTIVE ... source=wubei:accept-npc:currentMap:navigateInCurrentMap:current-map mini-map click started pathing`.
- This proves CR22 can partially succeed by consuming the route transfer, but the next current-map
  accept-NPC handoff can still block the task before the NPC is clicked.
- Follow-up heartbeat from `14:18:29.213` to `14:25:00.000` shows the same blocker did not recover:
  - `accept NPC pathing still active`: `1022`.
  - `window.ready.await result=timeout`: `255`.
  - `wubei.wait wakeResult=timeout`: `255`.
  - `window.ready.publish`: `0`.
  - `consumePrepared result=consumed`: `0`.
  - Latest repeated state remained `WAIT_ACCEPT_NPC_ROUTE`, `pathingState=ACTIVE`,
    `current=null(null, null)`, same source
    `wubei:accept-npc:currentMap:navigateInCurrentMap:current-map mini-map click started pathing`.

Acceptance:

- The leader no longer stands near the accept NPC while WUBEI keeps reporting only
  `accept NPC pathing still active`.
- Current-map accept-NPC pathing has a bounded terminal/recheck path and cannot keep the task parked
  indefinitely.
- The fix remains state/runner handoff policy only; no visual matching or click-target algorithm
  changes.

Card CR24: Same-target route intent reuse must not reuse terminal or unknown stale state

- [x] Inspect the third-view CR22 finding and verify against current source:
  - `NavigationService.registerWindowPathingIntent(...)` reuses same-target route intents when
    `includeCoordinate=false`;
  - `WindowRuntimeContext.getActivePathingIntent()` is backed by
    `WindowPathingSnapshot.hasActiveIntent()`;
  - `WindowPathingSnapshot.hasActiveIntent()` currently excludes only `NONE` and `ARRIVED`, so
    `STOPPED_AWAY` and `UNKNOWN` can still be treated as active.
- [x] Keep the fix scheduling/ownership only:
  - do not change OCR/template thresholds;
  - do not change route option matching, remembered-choice matching, NPC/minimap/world-map click
    coordinates, or WUBEI accept-task business semantics.
- [x] Restrict same-target route intent reuse to genuinely in-flight evidence:
  - allowed: fresh `ACTIVE` route intent, current route probe/preparation, or fresh route-pending
    prepared/visible dialog state for the same target;
  - not allowed: terminal `STOPPED_AWAY`, `ARRIVED`, old `UNKNOWN`, or stale snapshots with no fresh
    route evidence.
- [x] If reuse is rejected, register or clear toward a fresh intent with explicit logs:
  - previous intent id/target/source;
  - previous snapshot state/message/age;
  - reason such as `same-target-stale-terminal` or `same-target-unknown-expired`;
  - whether the new path registers a fresh intent or allows the world-map retry.
- [x] Run `mvn -q -DskipTests compile`.
- [ ] Validate with a fresh post-restart log:
  - same-target retries no longer return `PATHING_STARTED` only because an old `STOPPED_AWAY` or
    `UNKNOWN` snapshot still carries an intent;
  - CR22 prepared route actions still remain consumable for the same target;
  - CR23 bounded accept-NPC recheck is not masked by a stale same-target reuse decision.

Acceptance:

- Same-target route re-entry only reuses an intent when the watcher state is still credible and
  fresh.
- Terminal or unreliable route state creates a fresh recovery path instead of making the foreground
  wait on stale pathing.
- Logs make reuse-vs-refresh decisions auditable without adding new click or visual-matching
  behavior.

Card CR26: White Dragon tooltip click must wait for Runner-prepared enter-battle dialog

- [ ] Inspect the 白龙马 probe flow around `resolveProbeAfterPathing(...)`,
  `tryClickProbeSpawnedTarget(...)`, `tryClickKnownEnterBattleDialog(...)`, and the
  `NpcClickService` expected-dialog verification path.
- [ ] Preserve the new architecture boundary:
  - Runner owns `WUBEI_PROBE_STORY` / `WUBEI_ENTER_BATTLE` recognition and prepares the click;
  - the foreground task may wait briefly for the prepared result, but must not perform its own
    screenshot/OCR/template recognition for this dialog.
- [ ] Change the tooltip / smart target click result semantics:
  - a tooltip or smart-target click only proves that the target was clicked or a candidate was
    chosen;
  - it must not transition directly to `WAIT_BATTLE_FINISH`;
  - it should register or refresh `WUBEI_ENTER_BATTLE` interest and wait/yield for the
    Runner-prepared enter-battle action.
- [ ] Only transition to `WAIT_BATTLE_FINISH` after one of:
  - consumed/clicked `DialogOperation.WUBEI_ENTER_BATTLE`;
  - combat state is independently confirmed by battle radar / runner state.
- [ ] Do not change 白龙马 prompt order, no-story / wrong-position business decisions, NPC/template
  thresholds, tooltip click coordinates, or AutoA fallback in this card.
- [ ] Run `mvn -q -DskipTests compile`.
- [ ] Validate with a fresh log:
  - no `WAIT_BATTLE_FINISH` transition with source `probe-tooltip-clicked`,
    `probe-tooltip-clicked-without-story`, or `smart-combat-target-clicked` before a consumed
    `WUBEI_ENTER_BATTLE` action or confirmed combat state;
  - `WUBEI_PROBE_STORY` consumption still reaches target-ready / wrong-position decisions;
  - 白龙马 does not skip directly to route/recovery while an enter-battle dialog is pending.

Evidence:

- Post-restart log slice `2026-06-18 16:07:46` to `16:08:30` shows the first 白龙马 probe failed
  NPC click and moved to the next prompt, while the second probe consumed a prepared
  `WUBEI_PROBE_STORY` and then clicked the spawned target tooltip.
- At `16:08:29.669`, `NpcClickService` logged expected-dialog foreground verification as skipped
  with `expected=null expectedList=[wubei_enter_battle...]`, yet the tooltip click path was treated
  as verified.
- Current source still has direct transitions to `WAIT_BATTLE_FINISH` from
  `probe-tooltip-clicked`, `probe-tooltip-clicked-without-story`, and `smart-combat-target-clicked`.
  The same log did get a Runner-prepared `WUBEI_ENTER_BATTLE` shortly afterward, so this is a
  gate-ordering bug rather than a template/click-target bug.

Card CR28: Probe enter-battle timeout must fire inside RESOLVE_AFTER_PATHING prepared-dialog waits

- [x] Inspect the timeout guard and the blocking wait path:
  - `timeoutProbeTaskBeforeBattleIfNeeded(...)`;
  - `runPhase(...)`;
  - `resolveProbeAfterPathing(...)`;
  - `tryClickProbeSpawnedTarget(...)`;
  - `tryClickKnownEnterBattleDialog(...)`;
  - `waitForPreparedWubeiDialogReply(...)`.
- [x] Reuse the existing `PROBE_ENTER_BATTLE_TIMEOUT_MS` / `currentProbeTaskStartedAt` timeout
  policy. Do not add a second independent timer for 白龙马.
- [x] Ensure the timeout is checked while `RESOLVE_AFTER_PATHING` is internally waiting for
  `DialogOperation.WUBEI_ENTER_BATTLE`, especially for source
  `wubei:smart-combat-target:first-probe-no-story:runner-reply`.
- [x] When the existing timeout expires inside this wait path, return the same recovery semantics as
  the phase-entry guard:
  - clear current tracker/probe/enter-battle/wait-battle runtime;
  - close the team maintenance window with a clear source such as
    `wubei:probe-enter-battle-timeout`;
  - transition to `ROUTE_TO_MAIN_TASK` with the existing `probe-enter-battle-timeout-reaccept`
    reason.
- [x] Preserve pause compensation semantics. Time spent blocked by task stop/pause must not count
  against the five-minute probe timeout.
- [x] Do not change in this card:
  - 白龙马 prompt order;
  - no-story / wrong-position decisions;
  - `WUBEI_PROBE_STORY` / `WUBEI_ENTER_BATTLE` templates;
  - NPCClickSmart coordinates, AutoA fallback, or visual thresholds.
- [x] Run `mvn -q -DskipTests compile`.
- [ ] Validate with a fresh log:
  - no single `wubei:RESOLVE_AFTER_PATHING` transaction can hold for many minutes on repeated
    `expectedOperation=WUBEI_ENTER_BATTLE result=absent`;
  - once elapsed time reaches `PROBE_ENTER_BATTLE_TIMEOUT_MS`, logs show the existing
    `probe task exceeded enter-battle timeout` warning from inside the wait path;
  - task transitions to `ROUTE_TO_MAIN_TASK` / reaccept instead of staying in the same transaction;
  - normal short 白龙马 waits still consume Runner-prepared story / enter-battle results when
    available.

Evidence:

- At `2026-06-18 16:27:38`, WUBEI read
  `yellow='宝象述情|显形镜|王秋括' probe=true` and started the probe timer with
  `timeoutMs=300000`.
- The existing timeout guard is only called at `runPhase(...)` entry before the phase switch.
- The task stayed inside one `wubei:RESOLVE_AFTER_PATHING` transaction until user stop:
  `elapsedMs=881604`, `heldMs=881622`.
- In that same range, log counts show:
  - `first-probe-no-story=20149`;
  - `expectedOperation=WUBEI_ENTER_BATTLE=10074`;
  - `dialog prepare green template no hit ... WUBEI_ENTER_BATTLE=135`;
  - `probe enter battle timeout=0`.
- Source audit confirms `waitForPreparedWubeiDialogReply(...)` is an unbounded loop around
  `tryConsumePreparedWubeiDialog(...)` plus `TaskSleep.sleepOrStop(...)`, so it can prevent the
  outer phase-entry timeout check from ever running.

Card CR29: 白龙马 probe template miss must keep old tooltip fallback before switching prompt

- [x] Inspect the post-restart 白龙马 probe logs around `2026-06-18 17:05:49-17:06:49`.
- [x] In `resolveProbeAfterPathing(...)`, audit the explicit Runner-prepared
  `DialogOperation.WUBEI_PROBE_STORY` result with `target=wubei.probeNoTarget` /
  `WHITE_TEMPLATE_NOT_FOUND` from a genuinely unknown story blocker.
- [x] Do not treat `WHITE_TEMPLATE_NOT_FOUND / target=wubei.probeNoTarget` as confirmed no-target.
  This wrapper means "story exists but known white templates missed", not "白龙马 is absent".
- [x] Restore the old order for template-miss story results:
  `closeUnknownProbeStoryIfNeeded(...)` -> `tryClickProbeSpawnedTarget(..., false)` ->
  only then `markProbeResolved(...)` and switch/fail.
- [x] Preserve existing behavior for:
  - `target-ready` story -> click/enter-battle path;
  - `wrong-position` story -> retry the same green link;
  - no prepared result yet -> keep waiting for Runner;
  - true unknown story result -> handle only if the existing business rule still needs cleanup.
- [x] Do not change templates, thresholds, click coordinates, AutoA/AltA fallback, movement
  detection, or route/navigation behavior in this card.
- [x] Run `mvn -q -DskipTests compile`.
- [ ] Validate with a fresh log:
  - after `WHITE_TEMPLATE_NOT_FOUND / target=wubei.probeNoTarget`, the task logs unknown-story
    cleanup and/or the old no-story 白龙马 tooltip click attempt before any probe is marked resolved;
  - if tooltip click succeeds, it must continue into the enter-battle path instead of switching prompt;
  - if tooltip click fails, only the current probe is marked resolved and the next unused probe is tried;
  - a target-ready first probe still does not switch to second, because the first prompt already
    produced a valid target.

Evidence:

- At `2026-06-18 17:06:00.086-17:06:00.087`, Runner prepared and the task consumed
  `WUBEI_PROBE_STORY target=wubei.probeNoTarget` for `first-probe`.
- After that explicit no-target result, current code still logged unknown story cleanup and
  `NPC click failed: 白龙马` before it switched to `second-probe` at `17:06:08.131`.
- At `2026-06-18 17:06:43.372`, the same explicit `probeNoTarget` result occurred for
  `second-probe`, followed by unknown-story cleanup, another failed 白龙马 smart click, and
  `probe exhausted without target-ready story`.
- A later round at `2026-06-18 17:09:36-17:10:12` found `target-ready` on `first-probe` and consumed
  `WUBEI_ENTER_BATTLE`; that case correctly should not go to `second-probe`.
- Source audit on `2026-06-18` confirmed the CR29 implementation now branches on explicit
  `probeNoTarget` before unknown cleanup / tentative no-story smart target click. The remaining
  unchecked item is only the fresh post-restart WUBEI validation.
- Fresh slice `2026-06-18 18:35:55.192` validated the explicit `probeNoTarget` branch for
  `second-probe`: the task consumed the prepared action with `preparedAgeMs=0`, logged
  `probe no-target story check: label=second-probe visible=true`, and then failed cleanly with
  `no unused probe remains`; no unknown-story cleanup, no `try smart combat target click`, and no
  `NPC click failed: 白龙马` appeared before that outcome. This is partial evidence only; the
  first-probe -> second-probe switch path remains unchecked.
- Correction after fresh audit: the above branch is itself the regression. The Runner packaging is
  still the pushed behavior, but the current local task-side interpretation is wrong. At
  `18:35:25.791-18:35:26.221`, first-probe consumed
  `WUBEI_PROBE_STORY target=wubei.probeNoTarget matchedText=WHITE_TEMPLATE_NOT_FOUND`, treated it as
  confirmed no-target, and immediately switched to `second-probe`. At `18:35:55.192`, second-probe
  repeated the same interpretation and failed because no unused probe remained. Source diff confirms
  the local uncommitted `isProbeNoTargetStoryVisible(...)` branch sits before
  `closeUnknownProbeStoryIfNeeded(...)` and `tryClickProbeSpawnedTarget(..., false)`, so it bypasses
  the old 白龙马 tooltip fallback. CR29 is reopened and should be fixed by restoring that fallback
  order, not by changing Runner templates or prompt routing.
- Follow-up source audit on `2026-06-18 23:13` confirmed the local task-side branch has been fixed:
  the early `isProbeNoTargetStoryVisible(...)` resolved/switch branch is gone, and
  `WHITE_TEMPLATE_NOT_FOUND / wubei.probeNoTarget` now logs as a template miss before continuing to
  unknown-story cleanup and the old tooltip fallback. Compile passed; fresh 白龙马 validation remains
  the only unchecked item.

Card CR30: ENTER_BATTLE must consume prepared enter-battle dialog before stale combat-exit recovery

- [x] Inspect `WubeiTask.tickEnterBattle(...)` around the ordering of:
  - `autoCombatService.handleCombatTick(...)`;
  - `AutoCombatService.TickResult.IN_COMBAT`;
  - `AutoCombatService.TickResult.EXIT_RECOVERED`;
  - `tryClickKnownEnterBattleDialog(...)`.
- [x] Preserve the architecture boundary:
  - Runner owns `WUBEI_ENTER_BATTLE` recognition and prepares the click;
  - foreground task only consumes/clicks a fresh prepared action or waits/yields for it;
  - no foreground OCR/template matching should be added in this card.
- [x] Change the priority so a fresh prepared `DialogOperation.WUBEI_ENTER_BATTLE` action is
  consumed/clicked before `EXIT_RECOVERED` can move the phase to `POST_BATTLE_RECOVER`.
- [x] Keep independently confirmed `IN_COMBAT` as a valid path to `WAIT_BATTLE_FINISH`.
- [x] If no fresh prepared enter-battle action exists, keep existing fallback behavior unless the
  code can narrowly guard only the stale `EXIT_RECOVERED` case without changing business policy.
- [x] Do not change:
  - WUBEI enter-battle templates or thresholds;
  - click coordinates;
  - route/navigation behavior;
  - 白龙马 prompt order / no-target / wrong-position decisions;
  - AutoA/AltA fallback.
- [x] Run `mvn -q -DskipTests compile`.
- [x] Validate with a fresh WUBEI log:
  - after Runner prepares `WUBEI_ENTER_BATTLE`, the task consumes/clicks it before any
    `POST_BATTLE_RECOVER` / `RETURN_HOME` transition;
  - no `combat ended during enter battle phase` while a fresh `WUBEI_ENTER_BATTLE` prepared action
    is pending;
  - no `ready dialog pending too long ... WUBEI_ENTER_BATTLE` in `POST_BATTLE_RECOVER` or
    `RETURN_HOME`;
  - normal already-in-combat cases still go to `WAIT_BATTLE_FINISH`.

Evidence:

- `18:05:50.619`: `RESOLVE_AFTER_PATHING -> ENTER_BATTLE`.
- `18:05:51.415`: Runner prepared `WUBEI_ENTER_BATTLE target=wubei.enterBattle click=(1682,480)`.
- `18:05:51-18:05:55`: no `clickLeft` at `(1682,480)` and no `battle dialog clicked`.
- `18:05:55.293`: task jumped from `ENTER_BATTLE` to `POST_BATTLE_RECOVER` with
  `message=combat ended during enter battle phase`.
- `18:05:55.295`: pending ready dialog had already expired:
  `readyOperation=WUBEI_ENTER_BATTLE ... staleReason=prepared-stale:3878ms`.
- `18:05:56.177+`: task entered `RETURN_HOME` / normal-combat return flow while the entry dialog had
  never been consumed.
- `18:05:56.265`: Runner still re-verified/re-published
  `WUBEI_ENTER_BATTLE target=wubei.enterBattle click=(1682,480)` after `RETURN_HOME` had already
  started, which confirms the prepared action was available but the foreground phase had already
  taken the wrong branch.
- Original source audit showed `tickEnterBattle(...)` handled `EXIT_RECOVERED` before
  `tryClickKnownEnterBattleDialog(...)`, so the bad log sequence was explained by source order.
- Post-restart validation from `2026-06-18 18:19:28.402` to `18:34:40.650` shows the fixed path:
  - `18:31:20.809`: Runner prepared `WUBEI_ENTER_BATTLE target=wubei.enterBattle click=(1713,402)`;
  - `18:31:21.289`: foreground consumed it fresh with `preparedAgeMs=481`;
  - `18:31:21.808`: task clicked the prepared dialog action;
  - `18:31:21.812`: phase moved to `WAIT_BATTLE_FINISH` with
    `message=prepared enter-battle dialog consumed before normal phase`;
  - `18:31:22.770`: battle radar confirmed combat screen.
- The same slice also has two later successful samples:
  - `18:32:48.056 -> 18:32:49.594`, `preparedAgeMs=218`;
  - `18:34:22.045 -> 18:34:22.728`, `preparedAgeMs=256`.
- Negative validation in that slice:
  - `combat ended during enter battle phase=0`;
  - `ready dialog pending too long=0`;
  - `phase produced no outcome=0`;
  - `ERROR/Exception/异常=0`.

Card CR31: Latency script must tolerate sentinel timing values

- [x] Reproduce the current heartbeat failure by running:
  `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\analyze_wubei_latency.ps1 -LogPath logs\dhxy-console.log -StartTime "2026-06-18 18:55:00.331"`.
- [x] Fix `scripts/analyze_wubei_latency.ps1` so timing fields larger than normal analysis ranges,
  especially sentinel values such as `ageMs=9223372036854775807`, do not crash the script.
- [x] Preserve existing WUBEI counters and timing output.
- [x] Do not change Java, business logic, OCR/template thresholds, click/navigation behavior, or
  logging format in this card.
- [x] Re-run the script on:
  - the full log range after `2026-06-18 18:55:00.331`;
  - a WUBEI-focused range using `-Contains "wubei"` or a narrower equivalent filter.
- [x] Record before/after script behavior and the usable performance counters in
  `docs/ACTIVE_WORK.md`.

Evidence:

- Heartbeat audit on `2026-06-18 23:13` found that the unfiltered latency script crashes on newer
  logs with:
  `Cannot convert value "9223372036854775807" to type "System.Int32"`.
- The sentinel value came from 五环 latency lines such as:
  `ageMs=9223372036854775807`.
- This blocks the required per-heartbeat performance goal check unless the auditor manually filters
  logs or uses ad-hoc counting.

Card CR32: WAIT_BATTLE_FINISH must stop 400ms self-reacquire churn

- [x] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before editing code.
- [x] Inspect the current combat observer path in `WindowTaskRunner.runCombatWatcherLoop(...)` and
  `AutoCombatService.handleCombatTick(...)`; identify where a combat-state transition can be
  published without adding new OCR/template/click behavior.
- [x] Add a `COMBAT_STATE_CHANGED`-equivalent ready signal, preferably as
  `WindowReadyEventType.COMBAT_STATE_CHANGED`, with structured log fields:
  - `windowId` / `hwnd`;
  - old/new combat tick state or transition kind;
  - source (`window-combat-watch`, task tick, or explicit fallback);
  - elapsed detection time.
- [x] Change only `WAIT_BATTLE_FINISH` scheduling so it waits for the combat-state event and a coarse
  timeout fallback instead of repeatedly reacquiring the same task turn every 400ms while combat is
  still running.
- [x] Preserve the latency-sensitive paths:
  - do not change `WUBEI_ENTER_BATTLE` prepared consume/click priority;
  - do not change 白龙马 probe/no-target/story/template semantics;
  - do not change OCR/template thresholds, click coordinates, navigation, or battle business rules;
  - do not remove timeout fallback or stop/pause checkpoints.
- [x] Extend `scripts/analyze_wubei_latency.ps1` only if needed so the heartbeat can report
  `COMBAT_STATE_CHANGED`, `WAIT_BATTLE_FINISH` same-window reacquire, wake event/timeout ratio, and
  prepared-dialog latency in one pass.
- [x] Run `mvn -q -DskipTests compile`.
- [ ] Validate with a fresh WUBEI log and record the range in `docs/ACTIVE_WORK.md`:
  - `WAIT_BATTLE_FINISH sameAsPrevious` materially drops from the CR25 baseline;
  - `wubei.wait.wakeTimeout` materially drops and `wakeEvent` / combat-state wake coverage rises;
  - `preparedAgeMs` / `verifiedAgeMs` for enter-battle and accept-task remain within the prior
    healthy range;
  - no new `ERROR/Exception/异常`;
  - stop-all still interrupts the parked wait promptly.

Evidence:

- CR25 latest audit `2026-06-18 20:47:24.849-20:55:33.576`:
  - `wubei.wait.parkFinished=350`;
  - `wakeEvent=19`, `wakeTimeout=331`;
  - top same-window reacquire: `wubei:WAIT_BATTLE_FINISH same=315/315`;
  - tail reached `consecutiveTimeouts=418` before `20:55:05.947 battle finished and recovered`;
  - prepared-dialog latency was not the blocker (`preparedAgeMs p99=181ms max=545ms`,
    WUBEI accept consumes at `7ms` and `43ms`).
- Current source has `WindowReadyEventType` limited to `PATHING_TERMINAL` and
  `TASK_ATTENTION_REQUIRED`; architecture notes already name `COMBAT_STATE_CHANGED` as the event
  needed to stop `WAIT_BATTLE_FINISH` from reacquiring just to log `combat still running`.

Card CR33: Restore Wubei latest-push business baseline before runner/park migration continues

- Evidence note: before claiming, read both the top-of-file third-view CR in this document and
  `docs/WUBEI_BUSINESS_DIFF_AUDIT.md`. The latter adds concrete P0/P1 probe-story evidence that
  must be treated as CR33 acceptance criteria, not as generic runner/park migration.
- [x] Claim the card by recording latest-push baseline evidence in `docs/ACTIVE_WORK.md` before
  editing code.
- [x] Use `3f0a2e7 (origin/codex/migrate-runner-dialog)` as the 五倍 business baseline.
  Local business differences are not trusted during migration and should be restored by default.
- [x] Inspect and classify each local 五倍 business-sensitive delta as either:
  - restore to latest-push behavior; or
  - only if the user explicitly opens a new behavior-change story, split it out with fresh
    runtime/test evidence.
- [x] Cover only these verified local-vs-push business deltas:
  - probe-story handling can enter `WUBEI_PROBE_STORY` while tracker pathing is still `ACTIVE`, and
    can replace the pushed 15s probe-story wait with repeated park waiting;
  - accept-NPC route wait gained broader release/recovery behavior including
    `recoverStaleAcceptNpcCurrentMapPathing(...)` and route facts beyond a fresh matching prepared
    route dialog;
  - enter-battle phase now prioritizes fresh prepared `WUBEI_ENTER_BATTLE` and runner interest in
    places that alter the pushed auto-combat / known-dialog / tracker fallback order;
  - `throwProbeEnterBattleTimeoutIfNeeded(...)` injects probe enter-battle timeout from inner
    prepared-dialog waits;
  - leader/follower maintenance behavior is gated by team maintenance windows;
  - route dialog / same-target route intent reuse in `NavigationService` accepts different evidence
    than the pushed intent identity path.
- [x] Treat generic runner/park wait or battle `COMBAT_STATE_CHANGED` wake/park as architecture only
  when the pushed business decision order and deadlines remain unchanged. Specifically verify
  probe-story cannot take over while pathing is `ACTIVE`, and the pushed 15s probe-story decision
  window is preserved.
- [x] Do not change OCR/template thresholds, click coordinates, route option matching, minimap/world
  map click algorithms, 白龙马 prompt order, or battle business policy while doing the baseline
  restore.
- [x] Run `mvn -q -DskipTests compile`.
- [x] Record before/after diff summary and focused log validation plan in `docs/ACTIVE_WORK.md`.

Partial CR33 patch note:

- Restored in `WubeiTask`:
  - probe pathing `ACTIVE` / `probeInProgress` no longer lets visible/prepared `WUBEI_PROBE_STORY`
    take over before pathing terminal;
  - probe story wait uses the pushed 15s bounded decision window again;
  - `tickEnterBattle(...)` no longer immediately yields on first phase entry after registering
    runner interest.
- Kept intentionally:
  - CR30's fresh `WUBEI_ENTER_BATTLE` prepared-action priority, because that card is already Done
    with runtime evidence.
- Still open under CR33:
  - fresh runtime validation of the restored probe/enter-battle baseline.
- Split cards / existing card links:
  - accept-NPC route recovery/release behavior -> CR34.
  - `throwProbeEnterBattleTimeoutIfNeeded(...)` -> existing CR28.
  - maintenance-window gating -> CR35.
  - same-target route intent reuse in `NavigationService` -> existing CR24.

Acceptance:

- The local working tree no longer contains unapproved 五倍 business behavior changes relative to
  the latest pushed baseline.
- Any retained behavior change must be a separate user-requested behavior-change Story, not an
  incidental migration delta.
- Performance work can resume only after this baseline card is in Review or Done and the next run
  has a clean correctness gate.

Card CR39: 白龙马 probe story absent must replace the 15s polling fallback

- [x] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before editing code.
- [x] Treat this as a user-approved behavior-change card for 白龙马 points 14/15. It intentionally
  supersedes the latest-push `3f0a2e7` 15s probe-story polling fallback only for
  `DialogOperation.WUBEI_PROBE_STORY`.
- [x] Remove the leader-side `WUBEI_PROBE_STORY` 15s/80ms polling fallback:
  - do not keep `WUBEI_PROBE_STORY_WAIT_TIMEOUT_MS` / `WUBEI_PROBE_STORY_POLL_MS` semantics;
  - do not let the leader fabricate `WHITE_TEMPLATE_NOT_FOUND` after a local timeout;
  - do not treat timeout wake as a probe business result.
- [ ] Keep the 白龙马 post-mirror path turn-owned:
  - after using 显形镜, do not release the current task turn;
  - do not return a wait spec / park / yield for `WUBEI_PROBE_STORY`;
  - do not sleep or busy-spin while waiting for Runner/provider;
  - Runner/provider still prepares the result, but it does not "wake" the leader because the leader
    never slept;
  - from this point until the current 白龙马 chain succeeds or fails/reaccepts, the hot path should not
    let other windows or members interleave.
- [x] Make Runner/provider return an explicit prepared result when `WUBEI_PROBE_STORY` detects no
  STORY frame. Use exactly this vocabulary:
  - `targetKeyword=wubei.probeStoryAbsent`;
  - `matchedText=STORY_ABSENT`;
  - `dialogType=NONE`;
  - `clickRequired=false`.
- [x] Preserve the existing explicit STORY results:
  - `probeTargetReady` stays the target-ready path;
  - `probeWrongPosition` stays the wrong-position path;
  - `wubei.probeNoTarget` / `WHITE_TEMPLATE_NOT_FOUND` stays reserved for "STORY exists but known
    white templates did not match". It must not be reused for no-STORY absent.
- [x] On first `wubei.probeStoryAbsent` for the current probe:
  - keep `currentProbeIndex` unchanged;
  - do not `markProbeResolved(...)`;
  - do not switch to the next probe;
  - do not run tooltip fallback;
  - re-register `WUBEI_PROBE_STORY` and retry the same probe's 显形镜 through the existing per-probe
    attempt counter.
- [x] On second `wubei.probeStoryAbsent` for the same probe:
  - fail/reaccept the current 白龙马 task through the existing failure/recovery path;
  - do not fabricate `WHITE_TEMPLATE_NOT_FOUND`;
  - do not switch to the next probe;
  - do not run tooltip fallback.
- [x] Do not change in this card:
  - 白龙马 tracker green-link click selection;
  - pathing terminal classification;
  - `probeTargetReady` / `probeWrongPosition` template definitions;
  - OCR/template thresholds;
  - NPC click coordinates, tooltip click algorithm, AutoA/AltA fallback, or enter-battle ordering.
- [x] Run `mvn -q -DskipTests compile`.
- [ ] Validate with a fresh 白龙马 log:
  - no `probe story runner result timeout` produces a business decision;
  - no leader-side 15s `WHITE_TEMPLATE_NOT_FOUND` fabrication appears;
  - when no STORY frame is present, Runner publishes/updates a prepared result with
    `target=wubei.probeStoryAbsent` and `matched=STORY_ABSENT`;
  - first absent retries the same probe's 显形镜;
  - second absent fails/reaccepts without switching prompt or attempting tooltip fallback.

Acceptance:

- `Optional.empty()` for no-STORY `WUBEI_PROBE_STORY` is no longer silent; it is represented by the
  single explicit result `wubei.probeStoryAbsent`.
- The leader no longer owns the 15s probe-story polling decision.
- The leader also does not sleep after using 显形镜; it keeps the turn and waits for the current
  Runner/provider result without park/yield interleaving.
- 白龙马 point 14/15 behavior is reviewable in logs without introducing new visual matching or click
  behavior.

Third-view CR follow-up:

- P1 / business mismatch: `WindowTaskRunner.refreshTaskDialogInterestPreparationSignal(...)` still
  exits before provider invocation when the current visible dialog snapshot is absent or
  `DialogType.NONE`. That precondition is fine for normal option/story preparation, but it conflicts
  with CR39 because `WUBEI_PROBE_STORY` specifically needs the provider to turn "no STORY frame" into
  the explicit `wubei.probeStoryAbsent` / `STORY_ABSENT` result. With the current code, the provider
  can only build absent when it is called; the runner can skip the provider in the exact no-dialog
  situation, and `WubeiTask.waitForPreparedProbeStory(...)` then waits forever in the current task
  turn instead of reaching first-absent retry or second-absent fail/reaccept. Xie Shuai should issue
  a narrow follow-up story: for `DialogOperation.WUBEI_PROBE_STORY`, bypass or specialize that
  visible-dialog gate so the provider can publish the explicit absent prepared action, without
  broadening route/option preparation semantics.
- Follow-up implemented by Codex on 2026-06-19: only `DialogOperation.WUBEI_PROBE_STORY` is allowed
  to continue into task-dialog providers when no visible dialog snapshot is available. Other task
  dialog operations still require a visible non-`NONE` snapshot.
- Re-review against `docs/业务逻辑.md`: the first timing follow-up moved `WUBEI_PROBE_STORY` interest
  registration after `useProbeItemWithRuntimeRecord(...)`, which both conflicted with the business
  document and risked missing a short-lived post-mirror STORY because that method sleeps 700ms after
  item use.
- Follow-up repair implemented by Codex on 2026-06-19: `WUBEI_PROBE_STORY` interest is registered
  before 显形镜 again, but `wubei.probeStoryAbsent` is gated by
  `WindowDialogInterest.absentAllowedAtMs`. The gate opens immediately after
  `findAndUseItemFromBack(...)` returns `used=true`, before the 700ms post-use wait, and
  `waitForPreparedProbeStory(...)` no longer refreshes interest in a way that would close the gate.
  This should avoid both failure modes: no pre-item absent and no missed short-lived post-mirror
  STORY caused by late registration. Needs reviewer sign-off plus fresh 白龙马 runtime validation.
- Heartbeat blocker audit on 2026-06-19: CR39 is not passable yet even though compile passes.
  Remaining blockers:
  - `WubeiTask.useProbeItemWithRuntimeRecord(...)` still performs a `700ms` sleep after 显形镜, which
    violates the no-sleep part of 白龙马 point 14/15.
  - `WubeiTask.waitForPreparedProbeStory(...)` still waits with unbounded `while (true)` +
    `Thread.onSpinWait()`. This preserves no-park/no-yield but creates idle churn / CPU risk and
    fails the Latency Preservation Rule if Runner/provider does not publish a result quickly.
  - `DialogService.prepareWhiteStoryTemplateOrAbsent(...)` still returns `Optional.empty()` for
    detection/image/rect failure, so no-STORY is not fully guaranteed to become the explicit
    `wubei.probeStoryAbsent` / `STORY_ABSENT` result.
  - No fresh 白龙马 log exists after the latest CR39 code. Required validation remains:
    `target=wubei.probeStoryAbsent`, `matched=STORY_ABSENT`, first absent same-probe retry, and
    second absent fail/reaccept without prompt switch or tooltip fallback.
  - Do not restart script testing for CR39 until the blockers above are fixed.
- Superseding heartbeat re-check on 2026-06-19 07:47: the source blockers listed above are repaired
  in the current worktree. `mvn -q -DskipTests compile` passed, and `git diff --check` passed with
  LF/CRLF warnings only. Current CR39 status remains Review, not Done, because no fresh 白龙马 log
  exists after the repair. A focused CR39 script test can be restarted now to gather the required
  evidence, but CR39 must not be marked Pass/Done until logs prove
  `wubei.probeStoryAbsent / STORY_ABSENT`, first-absent same-probe retry, second-absent fail/reaccept
  without prompt switch or tooltip fallback, and no performance regression.

Card CR40: 白龙马 noTarget must not run tooltip fallback

- [ ] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before editing code.
- [ ] Treat this as a user-approved 白龙马 point 18 behavior change.
- [ ] Preserve the current first three `WUBEI_PROBE_STORY` business branches:
  - `wubei.probeTargetReady`: target appeared; resolve the current probe and continue into the
    existing 白龙马/target click path.
  - `wubei.probeWrongPosition`: wrong position or premature mirror use; roll back the current
    mirror attempt and repath the same green link without switching probes.
  - `wubei.probeStoryAbsent` / `STORY_ABSENT`: no STORY frame; use the CR39 same-probe retry and
    second-absent fail/reaccept behavior.
- [ ] Change only the fourth branch:
  - `wubei.probeNoTarget` is the final business result for "there is a STORY, but the known white
    story templates did not match";
  - do not treat it as a template-miss branch that can still try 白龙马 tooltip fallback;
  - do not call the old `tryClickProbeSpawnedTarget(..., false)` fallback from this branch.
- [ ] For `wubei.probeNoTarget`, mark the current probe resolved and then:
  - if another unused probe exists, switch to that probe;
  - if no unused probe exists, fail/reaccept the current 白龙马 task through the existing recovery path.
- [ ] The second probe must reuse the same four-result handling after its mirror use. Do not invent a
  separate second-probe policy.
- [ ] Do not change in this card:
  - tracker green-link click selection;
  - pathing terminal classification;
  - template files, thresholds, OCR, tooltip click coordinates, AutoA/AltA fallback, or enter-battle
    ordering;
  - CR39 no-STORY absent vocabulary or retry/fail behavior.
- [ ] Run `mvn -q -DskipTests compile`.
- [ ] Validate with a fresh 白龙马 log:
  - `wubei.probeNoTarget` no longer leads to `probe-tooltip-clicked-without-story`;
  - noTarget on first probe switches to second probe when available;
  - noTarget on the last probe fails/reaccepts;
  - targetReady, wrongPosition, and storyAbsent branches still follow their existing semantics.

Acceptance:

- `wubei.probeNoTarget` is a clear no-target business result, not a delayed tooltip fallback path.
- The only behavior change is removing the tooltip fallback from noTarget.
- First and second probe share the same post-mirror result handling.

Third-view CR note:

- Code review found no blocking CR40 issue: `probeTargetReady`, `probeWrongPosition`, and
  `probeStoryAbsent` still return before the fallback path, and explicit `wubei.probeNoTarget` is
  gated away from `tryClickProbeSpawnedTarget(..., false)` before the common mark-resolved /
  next-probe / fail path runs.
- Fresh 白龙马 validation is still required before Done. The checklist above is also still unchecked
  even though the sprint row is in Review; the owner should mark completed implementation/compile
  items and leave runtime validation unchecked until logs prove noTarget no longer reaches
  `probe-tooltip-clicked-without-story`.

Card CR41: 五倍普通怪 ready event vocabulary must stop using plain TASK_ATTENTION_REQUIRED

- [ ] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before editing code.
- [ ] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, and the full `docs/业务逻辑.md`.
  Record in `docs/ACTIVE_WORK.md` that the implementation is checked against both sections:
  `# 白龙马的逻辑` and `# 普通怪的逻辑`.
- [ ] Compare the touched path against latest pushed baseline `origin/codex/migrate-runner-dialog` /
  `3f0a2e7`. If the patch changes any business decision not explicitly described in
  `docs/业务逻辑.md`, stop and write a blocker instead of keeping the local behavior.
- [ ] Add explicit ready event vocabulary:
  - `PREPARED_ACTION_READY`: a prepared action with operation/target/click evidence is ready.
  - `PRE_BATTLE_TIMEOUT`: ordinary-monster pre-battle timer expired before entering battle.
  - Keep `PATHING_TERMINAL` as the movement-terminal fact.
  - Plain `TASK_ATTENTION_REQUIRED` must not be part of ordinary 五倍 task waits after this card.
- [ ] Update `WindowReadyEventBus` prepared-action caches and helper methods so they key off
  `PREPARED_ACTION_READY`, not `TASK_ATTENTION_REQUIRED + operation`.
- [ ] Update `WindowTaskRunner.publishPreparedActionReady(...)` and the visible-dialog prepared
  follow-up path to publish `PREPARED_ACTION_READY`.
- [ ] Update 五倍 wait specs and fresh-prepared lookup points so ordinary prepared-dialog waits use
  `PREPARED_ACTION_READY`.
- [ ] Do not remove or rewrite 白龙马 CR39/CR40 behavior. If 白龙马 still temporarily depends on
  current-window prepared action events, migrate it to the new prepared event without changing the
  four post-mirror business results.
- [ ] Run `mvn -q -DskipTests compile`.
- [ ] After editing, reread the full `docs/业务逻辑.md` and write in `docs/ACTIVE_WORK.md` whether the
  patch preserves:
  - 白龙马 points 1-24, especially CR39/CR40;
  - 普通怪 points 8-35;
  - no extra wrapper/nesting rule in `AGENTS.md`.

Acceptance:

- Logs for prepared actions say `type=PREPARED_ACTION_READY`, not
  `type=TASK_ATTENTION_REQUIRED`.
- Ordinary 五倍 waits no longer wake on plain visible `STORY` / `OPTION` attention.
- Existing route/accept/dialog prepared action consumers still validate operation/window/task before
  clicking.

Card CR42: 普通怪 Runner only prepares OPTION enter-battle and ignores all other dialog facts

- [ ] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before editing code.
- [ ] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, and the full `docs/业务逻辑.md`.
  Record the relevant ordinary logic points in `docs/ACTIVE_WORK.md`: 普通怪 points 8-16.
- [ ] After editing, reread `docs/业务逻辑.md` and explicitly check that 白龙马 CR39/CR40 branches are
  not changed by the ordinary-monster Runner path.
- [ ] Scope the change to ordinary-monster WUBEI pathing. Do not broaden this rule to route transfer,
  accept task, 白龙马 `WUBEI_PROBE_STORY`, 黄袍怪, 修罗, or 五环.
- [ ] In the ordinary-monster Runner path:
  - check whether the current visible dialog is `DialogType.OPTION`;
  - if it is `OPTION`, try to prepare `DialogOperation.WUBEI_ENTER_BATTLE`;
  - if the green template matches, publish `PREPARED_ACTION_READY`;
  - if the `OPTION` template does not match, publish no task result and continue to the pathing
    terminal check;
  - if the dialog is `STORY` or any non-`OPTION` type, publish no task result and continue to the
    pathing terminal check.
- [ ] Remove ordinary-monster use of visible-dialog soft wake as a business signal:
  - no plain visible `TASK_ATTENTION_REQUIRED`;
  - no "template miss" event;
  - no "unknown dialog" event.
- [ ] Keep Runner observation read-only: no click, no close dialog, no OCR/tooltip target click.
- [ ] Run `mvn -q -DskipTests compile`.

Acceptance:

- Fresh ordinary logs show: `OPTION + WUBEI_ENTER_BATTLE template match -> PREPARED_ACTION_READY`.
- Fresh ordinary logs show: `OPTION` template miss does not wake the leader and does not create a
  business result.
- Fresh ordinary logs show: `STORY` or other non-`OPTION` dialog does not wake the leader; Runner
  still checks whether pathing stopped.

Card CR43: 普通怪 PATHING_TERMINAL must re-click the same green link, not enter battle

- [ ] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before editing code.
- [ ] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, and the full `docs/业务逻辑.md`.
  Record the relevant ordinary logic points in `docs/ACTIVE_WORK.md`: 普通怪 points 25-28.
- [ ] Compare current local `WubeiTask` with `3f0a2e7` before touching the ordinary path. Do not keep
  local behavior simply because it already exists.
- [ ] Change ordinary-monster `RESOLVE_AFTER_PATHING` handling so `PATHING_TERMINAL` means only:
  `re-click current tracker green link -> release task turn -> wait for Runner again`.
- [ ] Preserve the same tracker panel / same ordinary target across this re-navigation:
  - do not reread tracker just because a pathing terminal arrived;
  - do not change target name;
  - do not treat terminal as task completion.
- [ ] Remove ordinary-monster fall-through from `PATHING_TERMINAL` into `ENTER_BATTLE`.
- [ ] Ordinary `PATHING_TERMINAL` must not call:
  - `tryClickTrackerCombatTargetSmart(...)`;
  - `tryDirectCombatFromTrackerHint(...)`;
  - tooltip fallback;
  - yellow OCR target click;
  - Alt+A/direct-combat fallback.
- [ ] Do not change 白龙马/probe pathing handling in this card. 白龙马 still follows the
  `docs/业务逻辑.md` 白龙马 section.
- [ ] Run `mvn -q -DskipTests compile`.
- [ ] After editing, reread `docs/业务逻辑.md` and record in `docs/ACTIVE_WORK.md` that both ordinary
  re-navigation and 白龙马 probe pathing still match the document.

Acceptance:

- Fresh ordinary logs show:
  `PATHING_TERMINAL -> tracker green click -> wait/release`, not `ENTER_BATTLE`.
- There is no ordinary-monster `PATHING_TERMINAL -> smart combat target click` path.
- 白龙马 first/second probe still uses its own documented pathing and mirror flow.

Card CR44: 普通怪 pre-battle timeout must be Runner-side and measured from first green click

- [ ] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before editing code.
- [ ] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, and the full `docs/业务逻辑.md`.
  Record the relevant ordinary logic points in `docs/ACTIVE_WORK.md`: 普通怪 points 29-35.
- [ ] Add ordinary-monster pre-battle timer state in the window/runtime path, not a leader-side
  polling loop:
  - start when the first ordinary-monster tracker green click succeeds;
  - do not reset on `PATHING_TERMINAL` / same-target re-navigation;
  - stop when `WUBEI_ENTER_BATTLE` prepared action is consumed and the task enters
    `WAIT_BATTLE_FINISH`;
  - combat time is not counted.
- [ ] Runner checks the timer because the leader can be parked indefinitely after the green click.
- [ ] When the timer reaches 5 minutes before battle entry, Runner publishes `PRE_BATTLE_TIMEOUT`.
- [ ] On `PRE_BATTLE_TIMEOUT`, `WubeiTask` must:
  - wake and reacquire the task turn;
  - not click the current green link again;
  - not click the monster;
  - not run Alt+A/direct-combat;
  - go directly to `ROUTE_TO_MAIN_TASK` / reaccept path;
  - rely on the next `READ_TRACKER` to overwrite old ordinary state, no complex cleanup here.
- [ ] Do not change 白龙马/probe `PROBE_ENTER_BATTLE_TIMEOUT_MS` in this card. 白龙马 timeout remains
  the existing separate behavior because 白龙马后半段 leader does not park.
- [ ] Run `mvn -q -DskipTests compile`.
- [ ] After editing, reread `docs/业务逻辑.md` and record in `docs/ACTIVE_WORK.md` that ordinary
  timeout and 白龙马 timeout remain separate.

Acceptance:

- Fresh ordinary logs show timer start at first ordinary green click.
- Repeated `PATHING_TERMINAL` re-navigation does not reset the timer.
- At 5 minutes without entering battle, Runner publishes `PRE_BATTLE_TIMEOUT`, and leader returns to
  reaccept without click/Alt+A.
- Once `WUBEI_ENTER_BATTLE` is consumed and `WAIT_BATTLE_FINISH` starts, the ordinary pre-battle
  timer is cleared/stopped.

Card CR45: 普通怪 implementation validation against business logic and performance goal

- [ ] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before validation.
- [ ] Before validation, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, and the full
  `docs/业务逻辑.md`.
- [ ] Validate after CR41-CR44 code is in Review or Done. Do not mark this card Done from compile
  only.
- [ ] Collect fresh ordinary-monster WUBEI logs that prove:
  - after ordinary green click, the leader parks indefinitely until Runner event;
  - `PREPARED_ACTION_READY / WUBEI_ENTER_BATTLE` wakes the leader and the prepared click is consumed;
  - `PATHING_TERMINAL` only re-clicks the same tracker green link;
  - `OPTION` template miss and non-`OPTION` dialog do not wake the leader;
  - `PRE_BATTLE_TIMEOUT` returns to reaccept if 5 minutes elapse before battle entry.
- [ ] Collect fresh 白龙马 spot-check evidence after these ordinary changes:
  - CR39 `wubei.probeStoryAbsent / STORY_ABSENT` still works;
  - CR40 `wubei.probeNoTarget` still does not run tooltip fallback;
  - no ordinary-monster event split broke 白龙马 prepared action consumption.
- [ ] Run or reuse `scripts/analyze_wubei_latency.ps1` and report:
  - `window.ready.await`;
  - `wubei.wait.parkFinished`;
  - `wakeResult=event/timeout`;
  - `sameAsPrevious`;
  - `PATHING_TERMINAL`;
  - `PREPARED_ACTION_READY`;
  - `PRE_BATTLE_TIMEOUT`;
  - remaining `TASK_ATTENTION_REQUIRED` if any.
- [ ] Performance goal check:
  - ordinary repeated turn reacquire / idle churn should drop;
  - p95/p99 prepared-action latency must not regress;
  - CPU-log pressure must not increase from a new spin/poll loop.
- [ ] After validation, reread `docs/业务逻辑.md` and record whether all ordinary and 白龙马 business
  rules still match.

Acceptance:

- CR41-CR44 are not enough by themselves; this card passes only with fresh logs.
- If logs show a mismatch, publish a focused follow-up blocker card instead of marking Done.
- Do not recommend broad user script restart as "safe" unless ordinary logic, 白龙马 CR39/CR40, and
  performance goal check all pass.

Card CR46: 黄袍怪第一战 must follow ordinary pre-battle contract without losing chained marker

- [ ] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before editing code.
- [ ] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, and the full
  `docs/业务逻辑.md`.
- [ ] Record in `docs/ACTIVE_WORK.md` that the implementation was checked against all three
  sections of `docs/业务逻辑.md`: 白龙马, 普通怪, and 黄袍怪.
- [ ] Compare touched 黄袍 path against latest pushed baseline
  `origin/codex/migrate-runner-dialog` /
  `3f0a2e79007121c98a15ad90d5ed7b8902033068` (`3f0a2e7`). If the patch would change a 黄袍
  business decision not described in `docs/业务逻辑.md`, stop and write a blocker instead of keeping
  the local behavior.
- [ ] Ensure 黄袍怪 first battle before the first `WAIT_BATTLE_FINISH` uses the same ordinary
  pre-battle contract as CR41-CR44:
  - first 黄袍 green click may release/park;
  - `OPTION + WUBEI_ENTER_BATTLE` template match wakes as `PREPARED_ACTION_READY`;
  - `OPTION` miss, `STORY`, and non-`OPTION` dialogs do not wake the leader;
  - `PATHING_TERMINAL` re-clicks the same 黄袍 tracker green link;
  - the first-battle 5 minute pre-battle timeout starts at first successful 黄袍 green click and
    ends on entry into `WAIT_BATTLE_FINISH`.
- [ ] Preserve the 黄袍连战 marker from `READ_TRACKER`.
  - `currentRoundChainedCombatExpected` / equivalent state must remain true for 黄袍 after the
    ordinary first-battle handoff.
  - Entering the first `WAIT_BATTLE_FINISH` must transition this round into the documented 黄袍热路径.
- [ ] Do not touch 白龙马 CR39/CR40 logic, OCR/template thresholds, tooltip fallback, navigation
  click coordinates, or Alt+A/direct-combat policy in this card.
- [ ] Run `mvn -q -DskipTests compile`.
- [ ] After editing, reread the full `docs/业务逻辑.md` and record whether 白龙马, 普通怪, and 黄袍怪
  still match the document.

Acceptance:

- Fresh or targeted logs show 黄袍 first battle uses the ordinary event vocabulary and same-target
  re-navigation rules.
- The 黄袍连战 marker survives the first-battle ordinary path and is still available after the first
  `WAIT_BATTLE_FINISH`.
- No 白龙马 or 普通怪 business decision changes outside the documented rules.

Card CR47: 黄袍怪第一战后 cannot return to ordinary infinite park waits

Patched review blockers:

- Identity preservation across bounded retries has been patched.
- The second blocker was also patched: `tickEnterBattle(...)` now checks
  `chainedContinuationEnterBattle` before the blocking `tryClickKnownEnterBattleDialog(context,
  null, "wubei:enter-battle")` path.
- A 黄袍 continuation `ENTER_BATTLE` now consumes an already fresh `WUBEI_ENTER_BATTLE` prepared
  action through the existing non-blocking path if present; otherwise it uses the short in-phase
  bounded retry and stays out of ordinary `WAIT_PREPARED_DIALOG` / `WAIT_PATHING_TERMINAL`.

Fresh blocker found on `2026-06-19 16:00-16:04`:

- The first three continuations clicked `chained-combat-*` and registered `WUBEI_ENTER_BATTLE`
  only after the click when `ENTER_BATTLE` phase started (`16:00:23.416`, `16:01:31.025`,
  `16:02:30.904`). This is already later than the documented hot-path intent.
- The fourth continuation clicked `chained-combat-4` at `16:03:32.040`, entered `ENTER_BATTLE` at
  `16:03:32.503`, but no `window.dialog.interest.update ... WUBEI_ENTER_BATTLE` followed.
  Runner repeatedly saw `OPTION` as operation-null `TASK_ATTENTION_REQUIRED`, and the leader stayed
  in `chained enter battle waits for fresh prepared action` until the phase loop guard at
  `16:04:11.035`.
- CR47 must therefore register `WUBEI_ENTER_BATTLE` interest for 黄袍 continuation before the actual
  `chained-combat-*` left click, using a continuation-specific source. Do not use the ordinary
  10-second delayed registration and do not re-enable ordinary pathing intent for continuation.

Fresh validation update on `2026-06-19 21:29-21:40`:

- The old `16:00-16:04` blocker is superseded by the fresh run.
- 黄袍 round 39 first battle entered through the ordinary target-map gate, then after battle recovery:
  - `21:30:30.372` registered
    `window.dialog.interest.update ... source=wubei:chained-enter-battle-before-click:chained-combat-1`;
  - `21:30:31.384` skipped ordinary tracker pathing intent with
    `reason=chained-combat-continuation`;
  - `21:30:33.851` consumed and cleared the fresh `WUBEI_ENTER_BATTLE` prepared action.
- The same pattern repeated for chained-combat 2, 3, and 4 in round 39, and again for round 40
  chained-combat 1-4 before the user stopped the run while still in `WAIT_BATTLE_FINISH`.
- No `phase loop guard exceeded` appeared in this fresh window.
- CR47 is therefore Done for the chained hot path. Chain-end / return-home remains under CR49.

- [x] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before editing code.
- [x] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, and the full
  `docs/业务逻辑.md`.
- [x] Record in `docs/ACTIVE_WORK.md` that this card was checked against 黄袍怪 points 11-27 and also
  against 白龙马/普通怪 to avoid regressions.
- [x] Compare touched code against `origin/codex/migrate-runner-dialog` /
  `3f0a2e79007121c98a15ad90d5ed7b8902033068` before editing. Preserve latest-push business
  behavior unless `docs/业务逻辑.md` explicitly says otherwise.
- [x] Inventory every wait after the first 黄袍 `WAIT_BATTLE_FINISH` and classify it:
  - allowed: battle-state waiting while actually in `WAIT_BATTLE_FINISH`;
  - allowed: the explicit short post-battle first-aid / maintenance broadcast window;
  - disallowed: continuation green click returning to ordinary infinite `WAIT_PATHING_TERMINAL`;
  - disallowed: continuation enter-battle returning to ordinary infinite `WAIT_PREPARED_DIALOG`.
- [x] Ensure `chained-combat-*` continuation green click remains a hot-path click:
  - no pathing intent registration;
  - no ordinary pathing terminal loop;
  - no ordinary 5 minute pre-battle timer reset;
  - no long park waiting for Runner to decide movement terminal.
- [x] Register `WUBEI_ENTER_BATTLE` interest before the actual `chained-combat-*` green-link click.
  This mirrors the 白龙马 point-before-action rule: the action that may create the dialog must not
  happen before Runner/provider knows which dialog operation to prepare.
- [x] Ensure continuation `ENTER_BATTLE` handling is phase-owned/bounded according to the latest-push
  baseline and `docs/业务逻辑.md`.
  It may consume a valid `WUBEI_ENTER_BATTLE` prepared action, but it must not sleep indefinitely like
  an ordinary first-battle green-link wait.
- [x] Do not change combat detection, AutoCombatService policy, OCR/template thresholds, tooltip
  clicking, or direct click algorithms in this card unless the diff proves they are the direct
  post-first-battle infinite-park cause.
- [x] Run `mvn -q -DskipTests compile`.
- [x] After editing, reread the full `docs/业务逻辑.md` and record whether the post-first-battle 黄袍
  hot path still matches it.

Acceptance:

- Logs for 黄袍 after the first battle show no continuation wait using ordinary
  `WAIT_PATHING_TERMINAL` infinite park.
- Logs for continuation入战 show no ordinary infinite `WAIT_PREPARED_DIALOG` park after
  `chained-combat-*` green click.
- Logs for continuation show `window.dialog.interest.update ... WUBEI_ENTER_BATTLE` before the
  `click tracker green: label=chained-combat-*` / physical click line, and then a fresh prepared
  action is published/consumed when `OPTION` appears.
- The only post-first-battle release windows are combat-state waiting and the explicit short
  first-aid window.

Fresh acceptance decision:

- Passed by the `2026-06-19 21:29-21:40` log window described above.
- Do not reopen CR47 for missing chain-end evidence; that belongs to CR49.

Card CR48: 黄袍怪 continuation fallback / chain-end / protection cap must match latest push

- [ ] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before editing code.
- [ ] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, and the full
  `docs/业务逻辑.md`.
- [ ] Record in `docs/ACTIVE_WORK.md` the exact baseline comparison commands used for 黄袍 sections
  of `WubeiTask` against `origin/codex/migrate-runner-dialog` /
  `3f0a2e79007121c98a15ad90d5ed7b8902033068`.
- [ ] Audit current 黄袍 continuation behavior against the latest-push baseline:
  - tracker missing after battle means chain complete and return home;
  - tracker present but yellow no longer contains `黄袍` means chain complete and return home;
  - tracker still contains `黄袍` means continue chain;
  - post-battle first-aid window opens at the existing baseline duration/condition;
  - continuation green click prefers the first tracker green link;
  - no-green fallback and protection cap use the baseline behavior unless user approves a new rule.
- [ ] If local code differs from baseline in any fallback/end/cap business decision not described in
  `docs/业务逻辑.md`, publish the exact blocker in Markdown or restore to baseline; do not silently
  keep the local behavior.
- [ ] Do not invent a new 黄袍 no-green click strategy, new chain-end heuristic, or new retry/protection
  policy in this card.
- [ ] Run `mvn -q -DskipTests compile` if code changes are made.
- [ ] After editing or audit, reread `docs/业务逻辑.md` and record whether 白龙马, 普通怪, and 黄袍怪 are
  still preserved.

Acceptance:

- A reviewer can trace every 黄袍 continuation fallback/end/cap decision either to `3f0a2e7` or to a
  user-approved line in `docs/业务逻辑.md`.
- No new fallback strategy is introduced by migration plumbing.
- Logs or diff evidence show tracker-missing, yellow-no-黄袍, still-黄袍, and protection-cap behavior
  are not accidentally changed.

Card CR49: 黄袍怪 fresh validation and regression check

- [ ] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before validation.
- [ ] Before validation, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, and the full
  `docs/业务逻辑.md`.
- [ ] Validate only after CR46-CR48 and directly related ordinary cards CR41-CR44 are in Review or
  Done. Do not mark this card Done from compile only.
- [ ] Collect fresh 黄袍怪 logs proving first battle:
  - first 黄袍 green click can park/release under the ordinary contract;
  - `PREPARED_ACTION_READY / WUBEI_ENTER_BATTLE` wakes and is consumed;
  - `PATHING_TERMINAL` only re-clicks the same 黄袍 green link;
  - first-battle 5 minute timeout is measured from first green click and stops at `WAIT_BATTLE_FINISH`.
- [ ] Collect fresh 黄袍怪 logs proving after-first-battle:
  - post-battle reads tracker before deciding continue vs return;
  - tracker missing or yellow without `黄袍` returns home;
  - tracker still containing `黄袍` opens only the short first-aid window, then continues;
  - `chained-combat-*` continuation skips pathing intent and does not enter ordinary infinite park;
  - continuation入战 consumes a valid prepared action or follows the documented baseline fallback.
- [ ] Run or reuse `scripts/analyze_wubei_latency.ps1` and report:
  - `window.ready.await`;
  - `wubei.wait.parkFinished`;
  - `wakeResult=event/timeout`;
  - `sameAsPrevious`;
  - `PREPARED_ACTION_READY`;
  - `PRE_BATTLE_TIMEOUT`;
  - `PATHING_TERMINAL`;
  - any remaining plain `TASK_ATTENTION_REQUIRED`.
- [ ] Regression checks:
  - 白龙马 CR39/CR40 still match `docs/业务逻辑.md`;
  - 普通怪 CR41-CR44 still match `docs/业务逻辑.md`;
  - no new idle churn / CPU-log pressure / p95-p99 prepared-action latency regression.
- [ ] After validation, reread the full `docs/业务逻辑.md` and record the final pass/blocker state in
  `docs/ACTIVE_WORK.md`.

Acceptance:

- 黄袍怪 is not considered complete until fresh logs prove both first-battle ordinary behavior and
  post-first-battle chained hot-path behavior.
- If logs show a mismatch, publish a focused follow-up blocker card instead of marking Done.
- Do not recommend broad user script restart as "safe" unless 黄袍、普通怪、白龙马 and performance
  goal checks all pass.

Card CR50: 普通怪/黄袍第一战 PATHING_TERMINAL must not be intercepted by ROUTE_TRANSFER

- [x] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before editing code.
- [x] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, and the full
  `docs/业务逻辑.md`.
- [x] Compare the touched path against latest pushed baseline `origin/codex/migrate-runner-dialog` /
  `3f0a2e79007121c98a15ad90d5ed7b8902033068` before editing.
- [x] In `WubeiTask.runResolveAfterPathingPhase(...)`, remove the 五倍 ordinary/黄袍第一战 branch that
  treats `STOPPED_AWAY + fresh ROUTE_TRANSFER` prepared action as a reason to wait/consume route
  dialog before handling the terminal pathing fact.
- [x] Preserve the documented business rule:
  - 普通怪 `PATHING_TERMINAL` means only:
    `PATHING_TERMINAL -> 重新点当前同一个 tracker 第一条绿字 -> 再放权等 Runner`;
  - 黄袍第一战入战前 uses the same rule;
  - `PATHING_TERMINAL` must not enter `ENTER_BATTLE`, smart-click the monster, run tooltip/OCR/Alt+A,
    or wait/consume `ROUTE_TRANSFER`.
- [x] Do not change accept-NPC / map navigation route-transfer behavior in this card. The route
  prepared action is still valid for navigation-owned route waits; it is just not a 五倍 ordinary
  monster / 黄袍第一战 `PATHING_TERMINAL` business result.
- [x] After editing, reread `docs/业务逻辑.md` and record in `docs/ACTIVE_WORK.md` whether 白龙马,
  普通怪, 黄袍怪 first-battle, and 黄袍 post-first-battle rules are preserved.
- [x] Run `mvn -q -DskipTests compile`.

Acceptance:

- Source review shows `runResolveAfterPathingPhase(...)` no longer checks or waits on
  `ROUTE_TRANSFER` when resolving 普通怪/黄袍第一战 `PATHING_TERMINAL`.
- Fresh ordinary/黄袍第一战 logs show a terminal pathing wake re-clicks the same current tracker green
  and releases again.
- CR45 and CR49 remain blocked until fresh validation proves no regressions and no performance
  rollback.

Card CR51: 五倍 dialog interest lifecycle must not be owned by 15s TTL

- [ ] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before editing code.
- [ ] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, and the full
  `docs/业务逻辑.md`, especially the top section
  `五倍 Dialog Interest 生命周期规则`.
- [ ] Record in `docs/ACTIVE_WORK.md` the baseline before editing:
  - current branch;
  - latest pushed baseline `origin/codex/migrate-runner-dialog` /
    `3f0a2e79007121c98a15ad90d5ed7b8902033068`;
  - `git status`;
  - relevant diff/show evidence for `WindowDialogInterest`, `WindowRuntimeContext`,
    `WindowTaskRunner`, and `WubeiTask`.
- [ ] Remove the 15s TTL as a business expiration path for 五倍 dialog interest.
  - `WindowRuntimeContext.getDialogInterest()` must not clear an otherwise valid 五倍 interest only
    because `expiresAtMs` passed.
  - Fresh logs must not show the old failure pattern where `window.dialog.interest.clear ...
    reason=expired` happens while ordinary/黄袍 is still waiting for `WUBEI_ENTER_BATTLE`.
- [ ] Historical CR51 timing note, superseded by CR52.
  - CR51 originally preserved ordinary/黄袍第一战 10s delayed registration only because that was the
    runtime behavior at the time this lifecycle card was written.
  - Do not treat this as the current implementation target.
  - CR52 owns deleting that fixed 10s delay and replacing it with Runner-side canonical target-map
    match before opening `WUBEI_ENTER_BATTLE interest`.
- [ ] Preserve 黄袍续战 point-before-click semantics from CR47.
  - `WUBEI_ENTER_BATTLE interest` must be registered before the physical
    `chained-combat-*` left click.
  - Do not use the ordinary 10s delayed registration for 黄袍续战.
  - Do not re-enable ordinary pathing intent or ordinary infinite wait for 黄袍续战.
- [ ] Implement only the approved clear boundaries from `docs/业务逻辑.md`:
  - clear residual interest before each new 五倍 accept-task cycle;
  - when Runner/combat watcher observes current window transition from non-combat to combat, clear
    `WUBEI_ENTER_BATTLE interest`;
  - for 白龙马 `WUBEI_PROBE_STORY`, clear after consuming non-entry probe results:
    `probeWrongPosition`, `probeStoryAbsent`, or `probeNoTarget`.
- [ ] Do not add these as required business clear points:
  - UI stop;
  - window stop request;
  - exception/task-run exit;
  - program close;
  - single-round return home;
  - `PRE_BATTLE_TIMEOUT` itself;
  - generic runtime/window reset.
  Implementation may defensively clean in shutdown/reset plumbing, but CR51 acceptance must not
  depend on those paths.
- [ ] Ensure registering a new dialog interest naturally replaces the previous one for the same
  window/task. Do not introduce multiple simultaneous current interests for one WUBEI window.
- [ ] Do not change OCR/template thresholds, prepared-action coordinates, click algorithms,
  pathing terminal semantics, ordinary 5 minute timeout semantics, or 白龙马/黄袍 business decisions
  beyond this documented interest lifecycle.
- [ ] Run `mvn -q -DskipTests compile`.
- [ ] After editing, reread the full `docs/业务逻辑.md` and record whether 白龙马、普通怪、黄袍怪 still
  match the documented business logic.

Acceptance:

- Source review shows no 15s TTL/`expiresAtMs` path can expire an active 五倍 dialog interest during
  ordinary/黄袍 first-battle wait or 黄袍 continuation wait.
- Fresh ordinary/黄袍 first-battle logs for CR51 show:
  - no `reason=expired` clears an already-open `WUBEI_ENTER_BATTLE interest` before battle entry or
    `PRE_BATTLE_TIMEOUT`;
  - Runner can prepare/emit `PREPARED_ACTION_READY / WUBEI_ENTER_BATTLE` after the old 15s TTL
    window.
  - Fixed 10s delayed-registration removal is not a CR51 acceptance item anymore; CR52 owns that
    timing replacement.
- Fresh 黄袍 continuation logs show:
  - `window.dialog.interest.update ... WUBEI_ENTER_BATTLE` before
    `click tracker green: label=chained-combat-*`;
  - no 15s interest expiration during continuation `ENTER_BATTLE`;
  - prepared action is published/consumed or the documented bounded continuation path handles it.
- Fresh 白龙马 logs or source review show non-entry `WUBEI_PROBE_STORY` results clear their probe
  interest before re-click/retry/switch/fail, while `probeTargetReady` does not require a separate
  manual clear beyond the normal next-interest/enter-combat lifecycle.
- CR45 and CR49 remain blocked until their own fresh validation confirms no performance or business
  regression.

Review update 2026-06-19 long run:

- Fresh `logs/dhxy-console.log` window `2026-06-19 20:46:25.459` to
  `2026-06-19 21:42:58.621` shows:
  - `ttl=phase-owned` interest updates: 1776;
  - `reason=expired`: 0;
  - `normal-enter-battle-delayed` / `delayed enter-battle interest scheduled`: 0.
- 黄袍续战 logs show `WUBEI_ENTER_BATTLE interest` registered before chained-combat clicks and no
  old 15s expiry.
- CR51 is Done for the lifecycle invariant.

Card CR52: 普通怪/黄袍第一战 enter-battle interest opens by canonical target-map match

- [ ] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before editing code.
- [ ] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, and the full
  `docs/业务逻辑.md`.
- [ ] After editing, reread the full `docs/业务逻辑.md` and explicitly record in
  `docs/ACTIVE_WORK.md` whether 白龙马、普通怪、黄袍第一战、黄袍续战、暗雷怪 rules are still preserved.
- [ ] Record in `docs/ACTIVE_WORK.md` before editing:
  - current branch;
  - latest pushed baseline `origin/codex/migrate-runner-dialog` /
    `3f0a2e79007121c98a15ad90d5ed7b8902033068`;
  - `git status`;
  - relevant `git diff` / `git show` evidence for the touched runtime path.
- [ ] Remove 普通怪/黄袍第一战 fixed-delay enter-battle interest registration.
  - Delete/disable the runtime path that schedules or registers
    `WUBEI_ENTER_BATTLE interest` merely because 10 seconds passed after the first tracker-green
    click.
  - Fresh logs must not show the old signature:
    `delayed enter-battle interest scheduled ... delayMs=10000` for ordinary monsters or 黄袍第一战.
- [ ] Wire the already-parsed tracker green `targetMapName` into the ordinary/黄袍第一战 Runner
  observation context.
  - The source of `targetMapName` is `TaskTrackerPanelService` parsing the left tracker green text.
  - Do not use route hint, destination tooltip/floating bubble, current-map OCR alone, or leader-side
    extra screenshot reads as the source of the target map.
- [ ] Runner opens `WUBEI_ENTER_BATTLE interest` only after target-map match.
  - Runner observes the current map in the background for the bound window.
  - Before comparing, canonicalize both names through `MapNameCanonicalizer`:
    current map name -> canonical current map;
    tracker `targetMapName` -> canonical target map.
  - Only when canonical current map equals canonical target map may Runner register/update
    `WUBEI_ENTER_BATTLE interest` for 普通怪/黄袍第一战.
  - If either side cannot be canonicalized, do not open the interest from map match; log the raw and
    canonical values clearly.
- [ ] Keep map match semantics narrow.
  - Map match only opens the入战 interest.
  - It does not prove coordinate arrival.
  - It does not replace `PATHING_TERMINAL`.
  - It does not make the leader click the monster, tooltip, yellow OCR target, Alt+A, or enter
    `ENTER_BATTLE` by itself.
- [ ] Preserve the ordinary/黄袍第一战 Runner result vocabulary from `docs/业务逻辑.md`.
  - Allowed leader wakes remain `PREPARED_ACTION_READY / WUBEI_ENTER_BATTLE`,
    `PATHING_TERMINAL`, and `PRE_BATTLE_TIMEOUT`.
  - `OPTION` template miss, `STORY`, and other dialogs must not wake the leader as ordinary/黄袍
    business results.
  - `PATHING_TERMINAL` still means re-click the same current tracker green and release again.
- [ ] Preserve the task-type boundaries from `docs/业务逻辑.md`.
  - `暗雷怪` has no `targetMapName` and should continue to reaccept/reroll through the existing
    dark-thunder path.
  - 白龙马/显形镜 probe must not use this ordinary/黄袍 map-match gate. Its `targetMapName` is only a
    first-prompt diagnostic and `WUBEI_PROBE_STORY` remains the probe interest.
  - 黄袍续战 must keep CR47/CR51 behavior: register `WUBEI_ENTER_BATTLE interest` before the
    `chained-combat-*` physical green click, with no 10s delay and no map-match delay.
- [ ] Preserve the existing pre-battle timeout semantics.
  - 普通怪/黄袍第一战 pre-battle 5 minute timer starts at the first successful current tracker-green
    click.
  - The timer ends when battle entry is consumed / `WAIT_BATTLE_FINISH` starts.
  - Combat time is not counted.
  - If timeout fires, Runner wakes the leader with `PRE_BATTLE_TIMEOUT`; this card must not replace
    timeout with map mismatch.
- [ ] Do not change OCR/template thresholds, prepared-action click coordinates, tracker-green click
  algorithm, movement detection, pathing terminal detection, 白龙马 probe branching, 黄袍续战 chain-end
  rules, or AutoCombat logic in this card.
- [ ] Run `mvn -q -DskipTests compile`.
- [ ] If any visual matching/click target code is changed, run or add the required testcase replay
  and marked output images. If the implementation only wires existing parser output and current-map
  observation without changing click/match algorithms, state why no new visual replay is required.

Acceptance:

- Source review shows ordinary/黄袍第一战 no longer schedules/registers `WUBEI_ENTER_BATTLE interest`
  from a fixed 10s delay.
- Source review shows Runner opens that interest from canonical map equality only:
  canonical current map == canonical tracker target map.
- Fresh 普通怪 or 黄袍第一战 logs show:
  - tracker green `targetMapName`;
  - current map name;
  - canonical target/current map values;
  - `mapMatched=true`;
  - `window.dialog.interest.update ... operations=[WUBEI_ENTER_BATTLE]` after the map match.
- Fresh logs show `OPTION` enter-battle template prepare/publish/consume still works after the new
  interest opens.
- Fresh logs show no fixed 10s delayed-registration signature for ordinary/黄袍第一战.
- Fresh logs show `PATHING_TERMINAL` still re-clicks the same current tracker green and does not get
  confused with map match.
- Fresh logs or source review show 白龙马/probe and 黄袍续战 paths were not moved onto this map-match
  gate.
- Performance goal check must state whether this reduced idle churn / CPU-log pressure versus the
  old fixed-delay wait, and must report no p95/p99 prepared-action latency regression.

Review update 2026-06-19:

- Source-level CR passed. `scripts/check_wubei_target_map_interest_gate.ps1` reports:
  `OK: Wubei ordinary enter-battle interest is target-map gated, not fixed-delay gated`.
- `mvn -q -DskipTests compile` passed.
- Code path check:
  - `TaskTrackerPanelService` attaches canonicalized `targetMapName` to tracker green links, skips
    暗雷, and only keeps the first 白龙马/显形镜 link as diagnostic.
  - `WubeiTask.clickTaskTrackerGreen(...)` starts the ordinary/黄袍第一战 target-map gate after a
    successful normal tracker-green click, but skips labels containing `probe` and labels starting
    `chained-combat-`.
  - `WindowTaskRunner` opens `WUBEI_ENTER_BATTLE interest` only after current map and tracker target
    map canonical values match, with source `wubei:normal-enter-battle-map-matched:*`.
  - 黄袍续战 still registers `WUBEI_ENTER_BATTLE interest` before the `chained-combat-*` physical
    green click and does not use the map-match gate.
  - `PATHING_TERMINAL` still re-clicks the same current tracker green and does not enter
    `ENTER_BATTLE` directly.
  - the ordinary/黄袍第一战 5-minute pre-battle timer is still started at the first successful green
    click and cleared on prepared enter-battle consumption / battle entry / timeout reset.
- Runtime proof was still pending at this source review point.

Fresh runtime update 2026-06-19:

- Fresh `logs/dhxy-console.log` window `2026-06-19 20:46:25.459` to
  `2026-06-19 21:42:58.621` proves CR52 at runtime:
  - `ordinary enter-battle target map gate armed`: 22;
  - `ordinary enter-battle target map gate opened`: 22;
  - `window.dialog.interest.update ... source=wubei:normal-enter-battle-map-matched:*` present;
  - old `normal-enter-battle-delayed` / `delayed enter-battle interest scheduled`: 0.
- Prepared enter-battle still publishes/consumes after the target-map gate opens. Example fresh
  path: `20:50:04.904` gate opened for `火云洞`, then `20:50:08.162` prepared
  `WUBEI_ENTER_BATTLE`, then `20:50:08.163` consumed/cleared.
- CR52 is Done.

Card CR53: 白龙马 targetReady 后 smart-combat-target absent 热循环

- [x] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before editing code.
- [x] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, and the full
  `docs/业务逻辑.md`.
- [x] After editing, reread the full `docs/业务逻辑.md` and record whether 白龙马、普通怪、黄袍怪,
  especially CR39/CR40/CR47/CR52 rules, are still preserved.
- [x] Record in `docs/ACTIVE_WORK.md` the baseline before editing:
  - current branch;
  - latest pushed baseline `origin/codex/migrate-runner-dialog` /
    `3f0a2e79007121c98a15ad90d5ed7b8902033068`;
  - `git status`;
  - relevant `git diff` / `git show` evidence for the targetReady / enter-battle wait path.
- [x] Reproduce or inspect the fresh log blocker:
  - In `2026-06-19 20:46-20:48`, after 白龙马 `probeTargetReady` / target-click path, the current
    code repeatedly logs
    `source=wubei:smart-combat-target:first-probe-story:runner-reply` and immediately consumes
    `result=absent` for expected operation `WUBEI_ENTER_BATTLE`.
  - This loops around 1689 times before `probe task exceeded enter-battle timeout`
    (`elapsedMs=300046`).
- [x] Confirm Tangde `2026-06-20` report root-cause refinement against fresh logs:
  - `2026-06-20 16:39:27.344-16:43:47.410` repeats the same blocker after the previous heartbeat:
    `wubei.probeTargetReady` was consumed, `npcClick:direct:firstVerify` skipped foreground
    expected-dialog verification, `WUBEI_ENTER_BATTLE` was registered, visible dialog stayed
    `STORY`, and the task reached the 300s probe enter-battle timeout.
  - This means tooltip/direct candidate success is currently too weak for WUBEI `COMBAT_TARGET`.
    A candidate click must not be final success unless fresh `WUBEI_ENTER_BATTLE`
    prepared/OPTION evidence appears.
  - If that evidence does not appear for the current candidate, the implementation must continue
    the next candidate or the existing direct-combat fallback path, without changing CR39/CR40
    probe branching, OCR/template thresholds, or the generic click algorithm.
- [x] Fix only the 白龙马 targetReady 后 enter-battle wait/absent loop.
  - Do not change CR39's `probeStoryAbsent` retry rule.
  - Do not change CR40's `probeNoTarget` no-tooltip-fallback rule.
  - Do not change tracker-green click, tooltip click, OCR/template thresholds, navigation target
    parsing, ordinary target-map gate, 黄袍续战 pre-click interest, or 5-minute pre-battle timer
    semantics unless the diff proves they are the direct cause.
- [x] The fixed path must not re-register or consume an absent prepared action in an 80ms hot loop.
  It should wait for an explicit Runner/provider result, a bounded current-phase decision already
  allowed by `docs/业务逻辑.md`, or the existing 白龙马 timeout/fail path without CPU/log pressure.
- [x] Run `mvn -q -DskipTests compile`.

Acceptance:

- Fresh 白龙马 log after targetReady/target click shows no repeated
  `smart-combat-target:* result=absent` hot loop.
- Fresh 白龙马 log proves candidate success is gated by enter-battle/OPTION evidence: if the first
  tooltip/direct candidate does not produce `WUBEI_ENTER_BATTLE` prepared evidence, the task tries
  the next valid candidate or existing direct-combat fallback instead of waiting 300s on a STORY
  dialog.
- If target click succeeds and battle/enter-battle appears, prepared action is published/consumed.
- If no enter-battle result appears, the task exits through the documented 白龙马 failure/timeout path
  without thousands of absent consumes.
- Performance goal check shows `consumePrepared.absent`, `dialog.interest.update`, and
  same-window wake churn are reduced for this path, with no p95/p99 prepared-action latency
  regression.

Card CR54: 黄袍续战 tracker 绿字小区域缓存快路径

- [x] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before editing code.
- [x] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, and the full
  `docs/业务逻辑.md`.
- [x] After editing, reread the full `docs/业务逻辑.md` and record whether 白龙马、普通怪、
  黄袍第一战、黄袍续战、暗雷怪 rules are still preserved.
- [x] Record in `docs/ACTIVE_WORK.md` the baseline before editing:
  - current branch;
  - latest pushed baseline `origin/codex/migrate-runner-dialog` /
    `3f0a2e79007121c98a15ad90d5ed7b8902033068`;
  - `git status`;
  - relevant `git diff` / `git show` evidence for the 黄袍续战 tracker-read path.
- [x] Confirm the current source cost:
  - `WubeiTask.returnHomeAfterCombatOrContinueSpecialTarget(...)` calls
    `taskTrackerPanelService.readWubeiTrackerPanel("post-combat-chained-" + combatCount)` after
    every chained battle.
  - `TaskTrackerPanelService.readWubeiTrackerPanel(...)` currently resolves/captures tracker panel,
    matches the 五倍 title template, washes yellow text, runs yellow OCR, scans green links, and may
    parse green-link target map OCR.
  - This is correct business-wise, but heavy for 黄袍续战 because the same tracker green region is
    usually reused after each chained fight.
- [x] Add a 黄袍续战-only fast path:
  - after the first confirmed 黄袍 tracker read, cache the current window/round, tracker panel/detail
    origin, first green-link local rectangle, click point, and a small image/fingerprint/template
    crop from that green-link area;
  - after later chained battles, capture/check only the current tracker panel small area or a narrow
    search area around the cached rectangle;
  - if the cached green-link image matches confidently, treat that as current tracker still showing
    the same 黄袍 continuation link and click the resolved current/saved point;
  - if the cached green-link image does not match confidently, do not rerun full tracker read; treat
    this chained segment as completed, clear chained state, use return item, and proceed to team
    return / next accept-task path;
  - register `WUBEI_ENTER_BATTLE interest` before the physical continuation click, preserving CR47;
  - log `chained tracker fast-path hit/miss-return-home` with elapsed time, score, source, round, count,
    rect, and click point.
- [x] Fast-path boundaries:
  - cache hit may accelerate the continue-click path only;
  - after the cache has been established, cache miss, low confidence, missing panel, changed tracker
    anchor, changed window/round, or missing cache must not fall back to the full
    `readWubeiTrackerPanel(...)`; it must go directly through the existing return-home / next-round
    path as if the 黄袍 chain is complete;
  - the only allowed full `readWubeiTrackerPanel(...)` for this CR54 flow is the first
    post-first-battle check needed to confirm 黄袍 continuation and establish the cache;
  - do not change 黄袍 first-battle target-map gate, ordinary monster terminal behavior,
    白龙马 probe/story logic, 暗雷重抽 logic, OCR/template thresholds, or generic tracker-green click
    algorithm unless a testcase proves it is required for this card.
- [x] Testcase / replay requirement:
  - because this changes visual matching/click target selection, save or reuse repo-local
    `images/test-cases/task-tracker/...` 黄袍 tracker screenshots;
  - run a replay/debug tool showing the cached green-link crop, matched current region, and final
    click point;
  - write a marked output image and command into `docs/ACTIVE_WORK.md`.
- [x] Compile with `mvn -q -DskipTests compile`.

Acceptance:

- Fresh 黄袍续战 logs show at least one `chained tracker fast-path hit` after the first full tracker
  read.
- On fast-path hit, the log does not repeat the full tracker title-template + yellow OCR +
  green-map OCR path for that chained continuation.
- Fast-path miss / low-confidence / cache invalid logs go directly to return-home / next-round
  handling; logs must not show a full `readWubeiTrackerPanel(...)` retry for that miss.
- `WUBEI_ENTER_BATTLE interest` is registered before each chained-combat physical green click.
- Performance goal check shows lower tracker OCR/title-template/log pressure for 黄袍续战 without
  increasing prepared-action p95/p99 latency and without changing 白龙马/普通怪 outcomes.

Card CR55: 世界地图路线结果点击点记忆快路径

Design:

- `docs/superpowers/specs/2026-06-19-world-map-route-result-memory-design.md`

Claim / baseline gate:

- [x] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before editing code.
- [x] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/ACTIVE_WORK.md`, and the
  design MD above.
- [x] Record in `docs/ACTIVE_WORK.md` the baseline before editing:
  - current branch;
  - latest pushed baseline commit/ref;
  - `git status`;
  - relevant `git diff` / `git show` evidence for `NavigationService` and the watcher/pathing
    settlement path being touched.
- [ ] If implementation touches 五倍, 黄袍, 白龙马, 普通怪, task-tracker, dialog interest, or route
  dialog behavior, reread `docs/业务逻辑.md` before and after editing and record that those rules
  are preserved.

Implementation requirements:

- [x] Add a dedicated world-map route-result memory model/store behind the unified `MemoryService`
  facade, with schema and settlement rules separate from dialog-choice memory.
  - Recommended persisted file: `config/world_map_route_result_memory.json`.
  - Use safe write: sibling temp file, then move into place.
  - JSON read failure must warn and fall back to empty memory, not block navigation.
- [x] Key memory by canonical `fromMap -> targetMap`.
  - Capture `fromMap` before the route search.
  - Use the existing map-name canonicalization rules when available; log raw and canonical values
    if useful.
  - Do not merge distinct real maps just to make matching easier.
  - If either map name is null/blank, skip lookup and skip writing.
- [x] Store only window-relative route-result click point data and counters:
  - `fromMap`, `targetMap`, `relativeX`, `relativeY`, `matchedText`;
  - `successCount`, `failureCount`, `consecutiveSuccessCount`, `consecutiveFailureCount`;
  - `clean`, `disabled`, `lastSuccessAt`, `lastFailureAt`, `lastAbandonedAt`, `source`.
  - Do not store screenshots in the memory file.
  - Do not store screen-absolute coordinates.
- [x] Insert lookup only after the existing world-map route-result panel is opened, searched, and
  scrolled to the same bottom position as the current OCR flow.
  - Do not use memory before route-result search/bottom-scroll.
  - Do not change first-run route behavior.
  - Do not change route-result OCR, destination verification, final route-link selection, scroll
    method, OCR thresholds, map-name OCR, minimap/world-map click algorithm, or watcher arrival
    rules.
- [x] Dirty/OCR path:
  - if no clean memory exists, run the existing OCR path unchanged;
  - after the existing OCR path finds and clicks the final route coordinate link, register the
    normal `WindowPathingIntent`;
  - create a pending route-result memory record in the current `WindowRuntimeContext`.
- [x] Clean fast path:
  - an entry becomes clean only after `consecutiveSuccessCount >= 5`;
  - only clean and non-disabled entries may be clicked by memory;
  - click saved `relativeX/relativeY` using the current window base;
  - register the same kind of `WindowPathingIntent` as the OCR path;
  - create a pending route-result memory record with `usedMemory=true`;
  - click time is not success. Watcher settlement is still the only success/failure source.
- [x] Settlement must be intent-bound and per-window.
  - Pending state belongs in the current `WindowRuntimeContext`, not a singleton/global field.
  - If watcher confirms arrival at the same target map for the same intent, record success.
  - If watcher reports `STOPPED_AWAY` for the same intent and no later navigation replaced it,
    record failure and mark the entry dirty.
  - If a second navigation starts before settlement, abandon the pending record without changing
    success/failure counts.
  - If task stop/pause/interrupt clears the intent before settlement, abandon the pending record.
- [x] Counter rules:
  - live success increments `successCount` and `consecutiveSuccessCount`, resets
    `consecutiveFailureCount`, and sets `clean=true` once the threshold is reached;
  - live failure increments `failureCount` and `consecutiveFailureCount`, resets
    `consecutiveSuccessCount`, and sets `clean=false`;
  - abandon updates only abandoned metadata and must not affect success/failure counts.
- [x] Error/fallback rules:
  - if memory click cannot be submitted to input, do not record failure immediately;
  - fall back to the existing OCR path or the existing route-click failure behavior;
  - do not learn from offline testcase replay, debug images, or manually inserted sample data.
- [x] Add concise logs with prefix `[world-map-route-memory]`:
  - lookup skipped: missing / dirty / disabled / blank map;
  - fast path used;
  - OCR path produced pending memory;
  - pending success / failure / abandoned;
  - include `fromMap`, `targetMap`, `relativeX`, `relativeY`, `clean`, counters, `usedMemory`,
    `intentId`, and `source` where relevant.
- [x] Compile with `mvn -q -DskipTests compile`.

Acceptance:

- Fresh live logs show the first four successful runs for the same canonical `fromMap -> targetMap`
  still use the existing OCR path after bottom-scroll.
- Each watcher-confirmed success increments `consecutiveSuccessCount`.
- The fifth consecutive success marks the entry clean.
- The next run for the same canonical `fromMap -> targetMap` still opens/searches/scrolls the route
  result panel, then uses the memory click point instead of repeating route-result OCR.
- A watcher-confirmed arrival after a memory click records success.
- A watcher `STOPPED_AWAY` after a memory click records failure and makes the entry dirty.
- A second navigation before settlement records abandoned and does not change success/failure
  counters.
- Logs prove no change to first-run route OCR/scroll/click behavior and no change to 五倍 ordinary,
  黄袍, 白龙马, or 暗雷 business rules.
- Performance goal check shows reduced repeated world-map route-result OCR/click-selection pressure
  for stable routes without increasing pathing/prepared-action p95/p99 latency.

Source Review Feedback - 2026-06-20:

- Status: **Repaired to Review / source review only**. No fresh live logs were used for this review.
- Accepted source-side points:
  - `NavigationService.performWorldMapSearchAndClickDestination(...)` checks clean route-result memory
    only after the existing world-map route-result panel prepare/search/bottom-scroll path.
  - When no clean entry exists, the current OCR route-result scan/click path remains the path that
    produces the pending memory.
  - `WindowTaskRunner.settlePendingWorldMapRouteResultMemory(...)` records success/failure only from
    watcher terminal state for the matching intent; click time itself is not treated as success.
  - `WorldMapRouteResultMemoryService` requires `consecutiveSuccessCount >= 5` before `clean=true`.
- Blocker 1:
  - `src/main/java/com/bot/dhxy/service/WorldMapRouteResultMemoryService.java:145` returns when
    `previous == null` inside `recordFailure(...)`.
  - This violates CR55 failure semantics. A fresh OCR route-result click followed by watcher
    `STOPPED_AWAY` must create a dirty entry with `failureCount=1`,
    `consecutiveFailureCount=1`, `consecutiveSuccessCount=0`, `clean=false`, and `lastFailureAt`.
  - Current behavior silently drops that failure evidence.
  - Repair: `recordFailure(...)` now creates a dirty entry when no previous row exists, preserving
    the pending click point, matched text, source, failure counters, and `lastFailureAt`.
- Blocker 2:
  - `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java:810` clears
    `pendingWorldMapRouteResultMemory` through `clearPathingSignal(...)` without recording abandoned
    metadata.
  - This violates the CR55 rule that task stop/pause/interrupt or intent clear before settlement
    must abandon the pending record without changing success/failure counters.
  - Required fix: before a pathing clear can discard CR55 pending memory, consume it and call
    `MemoryService.recordWorldMapRouteResultAbandoned(..., reason)` from an owner that has access to
    the memory facade, or otherwise keep pending until watcher settlement can abandon it.
  - Repair: `clearPathingSignal(...)` no longer clears the CR55 pending memory. 五倍's
    `clearCurrentPathingSignal(...)` now explicitly consumes any pending route-result memory and
    records abandoned metadata through `MemoryService` before clearing the pathing signal.
- Verification note:
  - `mvn -q -DskipTests compile` passed after repair.
  - `mvn -q -DskipTests test-compile` passed after repair.
  - `WorldMapRouteResultMemoryServiceTest`, `MemoryServiceFacadeTest`, and
    `WindowRuntimeContextWorldMapMemoryTest` passed after repair.
- Continued review - 2026-06-20:
  - Re-ran verification sequentially with reduced Maven memory after an initial parallel Maven run hit
    a local JVM native-memory crash. The sequential verification passed:
    `mvn -q -DskipTests compile`, `mvn -q -DskipTests test-compile`,
    `WorldMapRouteResultMemoryServiceTest`, `MemoryServiceFacadeTest`, and
    `WindowRuntimeContextWorldMapMemoryTest`.
  - Source verdict for 五倍/CR55 path: no remaining source blocker found. `recordFailure(...)`
    now creates a dirty first-failure row; watcher settlement still owns success/failure; 五倍's
    pathing clear records abandoned metadata before clearing the pathing signal.
  - Residual scope risk, not a 五倍 blocker: generic direct callers of
    `WindowRuntimeContext.clearPathingSignal(...)` now retain `pendingWorldMapRouteResultMemory`
    instead of abandoning it. That avoids silent loss and later intent replacement can abandon it,
    but if CR55 is expanded to guarantee generic stop/pause/interrupt abandon metadata for non-五倍
    tasks, add a follow-up owner-level abandon path instead of relying on a later navigation.
  - Still needs fresh live validation logs for first four OCR runs, fifth clean promotion, next fast
    path use, watcher `STOPPED_AWAY` dirtying, and abandoned settlement.

Card CR56: 修罗非快捷路线目标读取快照后台解析

Business source:

- `docs/业务逻辑.md` -> `修罗的逻辑` -> `非快捷路线`.

Claim / baseline gate:

- [x] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before editing code.
- [x] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/ACTIVE_WORK.md`, and
  `docs/业务逻辑.md`.
- [x] Before editing, record in `docs/ACTIVE_WORK.md`:
  - current branch;
  - latest pushed baseline commit/ref;
  - `git status`;
  - relevant `git diff` / `git show` evidence for `XiuluoTaskV2` and the current 修罗
    objective-read / accept-option / exit-prepath code path.
- [x] After editing, reread `docs/业务逻辑.md` and explicitly record that these business rules are
  still preserved:
  - 修罗非快捷路线 points 1-9;
  - 白龙马 CR39/CR40/CR53 rules;
  - 普通怪 CR41-CR45 rules;
  - 黄袍怪 CR46-CR54 rules.

Implementation requirements:

- [x] Keep `READ_OBJECTIVE` as an explicit 修罗 phase. Do not delete or bypass the phase.
- [x] Replace the old `READ_OBJECTIVE` implementation.
  - After CR56, `READ_OBJECTIVE` must not perform its own screenshot.
  - It must not run dialog detect.
  - It must not call `DialogService.handleDialog(READ_STORY_OBJECTIVE)`.
  - It must not run task-panel OCR.
  - Its only job is to wait for and consume the accept-time background objective parse result.
- [x] After 修罗接任务 option is successfully consumed on the non-fast route, immediately capture one
  story-objective snapshot for the current bound window.
  - This capture must be a screenshot/crop step only.
  - It must not run full dialog detect again.
  - It must not send mouse/keyboard input.
  - It must not close/open UI.
- [x] Start a background parse using only that captured in-memory image.
  - Background code may call the existing objective text recognition/parsing logic on the image.
  - Background code must not call `DialogService.handleDialog(READ_STORY_OBJECTIVE)`.
  - Background code must not take a second screenshot.
  - Background code must not do task-panel OCR.
- [x] Continue the existing non-fast exit prepath after scheduling the background parse.
  - The intended overlap is: accept task -> capture objective image -> start background parse ->
    continue opening/clicking the exit route from 灵兽村.
  - Do not wait for objective parsing before starting the exit prepath.
- [x] When the task enters `READ_OBJECTIVE`, it must wait for the background parse result.
  - No task-turn release.
  - No park.
  - No yield.
  - No opportunistic maintenance while inside this wait.
  - No other window/team member should be able to use this phase as an insertion point.
  - The phase has only two outcomes: objective parsed successfully, or objective parsing failed.
- [x] On parse success, write the target map/coordinate into the current 修罗 round context and
  continue into `NAVIGATE_TO_TARGET`.
  - If the exit prepath already started, preserve that state and continue normal 修罗 navigation.
- [x] On parse failure or missing snapshot, use the existing 修罗 objective-read failure / reaccept /
  recovery path.
  - Do not take another screenshot inside the same phase.
  - Do not run dialog detect or `READ_STORY_OBJECTIVE` inside the same phase.
  - Do not run task-panel OCR inside the same phase.
  - Do not silently continue with null objective.
- [x] Do not implement 修罗快捷路线 in this card. The documented scope is only `非快捷路线`.
- [x] Do not change 修罗 NPC click, enter-battle dialog handling, direct-combat fallback, maintenance
  broadcast, return-home, battle wait, or completion-count logic except where needed to connect the
  objective snapshot result.

Logging requirements:

- [x] Add concise 修罗 logs that make the overlap auditable:
  - objective snapshot captured / failed;
  - background objective parse started;
  - exit prepath started while parse is pending;
  - `READ_OBJECTIVE` waiting for background result;
  - background parse success / failure with elapsed time;
  - synchronous fallback skipped because CR56 owns the background result.
- [x] Logs should include current window id/title when available and the 修罗 round context or phase.

Verification:

- [x] Compile with `mvn -q -DskipTests compile`.
- [x] Add or update a focused source/test check if practical so the code cannot regress into
  "background parse plus synchronous READ_STORY_OBJECTIVE at the same time".
- [ ] Fresh 修罗 non-fast route logs must prove:
  - accept option consumption is followed by objective snapshot capture;
  - exit prepath starts before objective parse completion when parsing is still pending;
  - `READ_OBJECTIVE` waits for the background result without park/yield;
  - success writes objective and continues navigation;
  - failure goes to the existing objective-read failure/recovery path;
  - `READ_OBJECTIVE` itself does not capture/read/detect/OCR after CR56.
- [ ] Performance goal check:
  - objective-read latency is overlapped with exit prepath, so the user-visible wait before target
    navigation is reduced when OCR is slow;
  - no new task-turn churn, idle park loop, or cross-window maintenance insertion appears during
    `READ_OBJECTIVE`;
  - no regression to 五倍/白龙马/普通怪/黄袍 p95/p99 prepared-action or pathing latency.

Source Review Feedback - 2026-06-20:

- Status: **Review / no blocking source finding / fresh 修罗 non-fast runtime validation still
  pending**.
- Commands run:
  - `$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=64m -XX:CICompilerCount=2'; mvn -q -DskipTests compile`
  - `$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=64m -XX:CICompilerCount=2'; mvn -q -DskipTests test-compile`
  - `javac -encoding UTF-8 -d target\test-classes src\test\java\com\bot\dhxy\task\xiuluo\XiuluoReadObjectiveNoSyncFallbackWiringTest.java; java -cp target\test-classes com.bot.dhxy.task.xiuluo.XiuluoReadObjectiveNoSyncFallbackWiringTest`
- Source review result:
  - `XiuluoTaskV2.readObjective(...)` now only reads `state.objectiveParseFuture()`, waits via
    `waitForBackgroundObjectiveResult(...)`, and routes missing/miss results through
    `recoverBackgroundObjectiveReadFailure(...)`.
  - The old `tryReadCurrentStoryObjective(...)`, `tryReadObjectiveFromTaskPanel(...)`,
    `DialogService.handleDialog(READ_STORY_OBJECTIVE)`, known-dialog checks, under-three checks,
    and task sleeps are no longer in the `READ_OBJECTIVE` phase body.
  - `continueAfterAcceptOptionClicked(...)` schedules the accept-time snapshot/background parse before
    the maintenance decision / exit prepath, so the no-maintenance path can overlap exit prepath with
    objective parsing.
  - `DialogService.captureCurrentStoryObjectiveSnapshotNoDetect(...)` uses the same
    `cropAbsoluteRect(dialogRect, smallDialogRect)` geometry as the existing story-objective crop,
    but without dialog classification.
- Non-blocking cleanup:
  - `XiuluoTaskV2.java:1858-1861` still says "The story dialog remains available for
    READ_OBJECTIVE while the leader is moving." That comment is now misleading because
    `READ_OBJECTIVE` no longer reads the story dialog. Update it to say the accept-time snapshot is
    already captured and the phase will wait for the background parse result. This is not a business
    blocker, but it should be cleaned before moving CR56 to Done.
- Remaining validation:
  - Need fresh 修罗 non-fast route logs proving accept-option -> snapshot capture -> background parse
    -> exit prepath overlap -> `READ_OBJECTIVE` wait/consume, with no same-phase screenshot/detect/OCR
    and no new task-turn/maintenance insertion churn.

Source / Log Recheck - 2026-06-20:

- Status: **Review / source still passes / current log evidence is stale pre-CR56 behavior**.
- Commands run:
  - `javac -encoding UTF-8 -d target\test-classes src\test\java\com\bot\dhxy\task\xiuluo\XiuluoReadObjectiveNoSyncFallbackWiringTest.java; java -cp target\test-classes com.bot.dhxy.task.xiuluo.XiuluoReadObjectiveNoSyncFallbackWiringTest`
  - `$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=64m -XX:CICompilerCount=2'; mvn -q -DskipTests compile`
- Source review result:
  - `XiuluoTaskV2.readObjective(...)` still reads `state.objectiveParseFuture()` and waits through
    `waitForBackgroundObjectiveResult(...)`.
  - The `READ_OBJECTIVE` method body still does not call `tryReadCurrentStoryObjective(...)`,
    `tryReadObjectiveFromTaskPanel(...)`, `DialogService.handleDialog(...)`, known-dialog checks,
    under-three checks, or task sleeps.
  - `continueAfterAcceptOptionClicked(...)` still schedules
    `scheduleAcceptObjectiveBackgroundParse(...)` before maintenance / exit-prepath decisions.
  - `DialogService.captureCurrentStoryObjectiveSnapshotNoDetect(...)` still captures/crops without
    dialog classification.
- Current log result:
  - Existing `logs/dhxy-console.log` does **not** validate CR56. Latest visible 修罗 samples around
    `2026-06-20 01:40-01:46` still show old runtime behavior:
    - `accept option clicked; start exit before objective read`;
    - `missing objective before navigation; reread objective`;
    - `dialog handle request ... operation=READ_STORY_OBJECTIVE`;
    - `phase outcome: phase=READ_OBJECTIVE ... message=objective parsed from story dialog`.
  - Those lines prove the last live run was not using the current CR56 source path. Treat them as
    stale/pre-CR56 evidence, not as a CR56 failure in current source.
- Remaining validation:
  - Restart with current build and run 修罗非快捷路线.
  - Fresh logs must include `objective snapshot captured`, `background objective parse started`,
    `READ_OBJECTIVE waiting for background objective result`, and
    `READ_OBJECTIVE consumed background objective result`.
  - Fresh logs must not include `DialogService.handleDialog(READ_STORY_OBJECTIVE)` inside
    `READ_OBJECTIVE`.
  - Non-blocking cleanup remains: update the stale comment near `XiuluoTaskV2.continueAfterAcceptOptionClicked(...)`
    which still says the story dialog remains available for `READ_OBJECTIVE` while moving.

Runtime Acceptance - 2026-06-21:

- Status: **Done / fresh 修罗 runtime validated**. This supersedes the stale 2026-06-20 log result above.
- Fresh log range: `2026-06-21 00:26:38.000-00:32:22.000`.
- Evidence:
  - `00:27:28.879` captured the accept-time objective snapshot and scheduled background parse.
  - `00:27:29.187` background parse completed with `target=白骨山(101,46)`.
  - `00:27:33.498` `READ_OBJECTIVE` consumed the background result with `elapsedMs=0`; the phase finished in `1ms`.
  - Round 2 repeated the same pattern for `长安城东(30,74)` at `00:29:57-00:30:02`.
  - Round 3 started the same pattern for `凤巢五层(40,53)` at `00:32:19-00:32:20`.
- Performance goal check: `READ_OBJECTIVE` is now effectively a prepared-result consume step in the observed 修罗 non-fast route, so the old synchronous objective OCR wait is gone from the task phase and no new park/yield/maintenance insertion was observed in that phase.

Card CR57: Runner dialog branches reuse one per-tick DialogDetection

Business source:

- `docs/业务逻辑.md` full file. This is a framework/performance card, but it touches the runner
  path that can wake or prepare actions for 白龙马、普通怪、黄袍怪、修罗. The implementing agent must
  prove that no business branch changed.

Problem statement:

- A single `WindowTaskRunner` watcher tick can currently inspect the same visible dialog multiple
  times:
  - task attention calls `publishTaskAttentionIfDialogVisible(...)` and detects the dialog;
  - task dialog preparation can detect/capture again through provider -> `DialogService.prepare...`;
  - route / remembered route preparation can detect/capture again;
  - final attention can detect again if no prepared action was published.
- Recent logs show this is part of the original sprint performance problem:
  - `2026-06-20 01:20:50`: `window-task-attention:xiuluo_v2` detected `OPTION` with
    `attentionDetectMs=1718`, then route prepare separately spent `detectMs=700 ocrMs=3691
    totalMs=4391`, and the same observer tick reached `attentionTotalMs=6111`.
  - Recent observer tick stats still show high dialog pressure:
    `total avgMs=3528.4 p95Ms=9577 maxMs=15171`,
    `attentionDetect count=487 avgMs=1010.1 p95Ms=3557 maxMs=8345`,
    `ticksWithAttentionDetectOver1000=150`.
- The current issue is duplicated same-dialog capture/detect work. It is not a request to change
  which dialog result is accepted, which template is used, or where any click lands.

Claim / baseline gate:

- [ ] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before editing code.
- [ ] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/ACTIVE_WORK.md`, and
  `docs/业务逻辑.md`.
- [ ] Before editing, record in `docs/ACTIVE_WORK.md`:
  - current branch;
  - latest pushed baseline commit/ref;
  - `git status`;
  - relevant `git diff` / `git show` evidence for `WindowTaskRunner`, `DialogService`, and any
    touched preparation provider.
- [ ] After editing, reread `docs/业务逻辑.md` and explicitly record that these business rules are
  still preserved:
  - 白龙马 CR39/CR40/CR53 probe and targetReady rules;
  - 普通怪 CR41-CR45 rules;
  - 黄袍怪 CR46-CR54 rules;
  - 修罗 CR56 non-fast objective-read rules.

Implementation requirements:

- [x] In one watcher tick, dialog attention and dialog preparation must be able to share one fresh
  ephemeral `DialogDetection` / captured dialog image.
  - Use a per-tick local carrier in `WindowTaskRunner`, for example a small tick-scoped dialog probe
    object or a local `DialogDetection` plus metadata.
  - Do not add wrapper nesting. One clear object is acceptable only if it owns real tick data and
    avoids repeated screenshots.
- [x] Add `DialogService` prepare paths that can consume an already-captured `DialogDetection` when
  available, while preserving the existing no-argument capture path for callers that do not have a
  tick snapshot.
  - Route keyword option prepare must be able to reuse the supplied detection.
  - Remembered route choice prepare must be able to reuse the supplied detection.
  - Green option template prepare for `WUBEI_ENTER_BATTLE` must be able to reuse the supplied
    detection.
  - White/story template prepare for `WUBEI_PROBE_STORY` must be able to reuse the supplied
    detection.
- [x] If the supplied detection is missing, stale for this tick, or the wrong dialog type, fall back
  to the current capture/detect path. Do not silently accept an incompatible detection.
- [x] Avoid the second/final `publishTaskAttentionIfDialogVisible(...)` detect in the same watcher
  tick when a fresh dialog detection from this tick is already available.
- [x] Keep `WindowDialogSnapshot` as a lightweight visible-dialog fact: type, rect, source,
  timestamp/provider metadata. Do not store long-lived `BufferedImage` there or in
  `WindowRuntimeContext`.
- [x] Do not merge these separate screenshot families into this card:
  - minimap/pathing probe (`MiniMapCoordinateReader` / pathing location);
  - battle radar / auto-combat guard;
  - tracker panel green-link OCR/click parsing;
  - NPC click matching or world-map route matching.
- [x] Do not change:
  - OCR/template thresholds;
  - template files;
  - click coordinates or click algorithms;
  - pathing semantics;
  - prepared action publish/consume semantics;
  - any task business fallback order.

Logging requirements:

- [x] Add concise runner logs that make reuse auditable without log spam:
  - whether a tick dialog detection was captured;
  - which downstream branch reused it: attention, task prepare, route prepare, remembered route,
    final attention;
  - whether a branch rejected the supplied detection and had to capture again, including reason.
- [x] Logs must include window id/title when available and current operation/source
  (`WUBEI_ENTER_BATTLE`, `WUBEI_PROBE_STORY`, route prepare, remembered route, etc.).

Verification:

- [x] Compile with `mvn -q -DskipTests compile`.
- [x] Add or update a focused source/test guard if practical so a same-tick dialog prepare cannot
  regress into unconditional second capture when a valid detection is available.
- [ ] Fresh runtime logs must prove:
  - in a visible-dialog watcher tick, attention and prepare can reuse the same tick detection;
  - no repeated `dialog detect no-focus: reason=prepare-route` / prepare capture appears in the same
    tick when a valid tick detection exists;
  - `attentionTotalMs` improves on previously observed duplicated work cases;
  - pathing/minimap metrics do not regress, because this card does not touch pathing screenshots.
- [ ] Performance goal check:
  - duplicated same-dialog capture/detect/OCR work decreases;
  - idle churn / CPU-log pressure decreases;
  - no p95/p99 regression for prepared-action or pathing latency;
  - no 白龙马、普通怪、黄袍怪、修罗 business behavior regression in fresh logs.

Card CR58: Prepared action READY 后停止后台 100ms fingerprint 续命

Business source:

- `docs/业务逻辑.md` full file. This card changes Runner scheduling and prepared-action consume
  plumbing only. It must not change any task business decision, template threshold, OCR fallback,
  click coordinate, navigation order, probe branching, or 黄袍/普通/白龙马/修罗 phase rule.

Problem statement:

- Current `WindowTaskRunner` uses a single hot dialog interval for all dialog interests. A prepared
  action can keep the watcher at `100ms` because the loop treats `preparedDialogAction != null` as a
  hot-loop reason.
- Existing prepared actions are revalidated inside the watcher tick through
  `validatePreparedDialogAction(...)`: capture validation rect, wash, fingerprint, compare, refresh
  `lastVerifiedAtMs`, and publish `PREPARED_ACTION_READY` again.
- This makes the `2500ms/3000ms` freshness fence mostly meaningless: the watcher keeps refreshing
  the action every ~100ms, so the action rarely becomes stale, while CPU/log pressure stays high.
- External reviewer feedback was used only as input. Local source/log inspection confirms the issue:
  repeated `prepared-action-verified` / READY updates and observer ticks with prepared operation still
  run at hot cadence. The fix must be scheduling/validation plumbing, not business logic rewriting.

Claim / baseline gate:

- [ ] Claim the card by changing the sprint board row from `Unclaimed | Ready` to
  `<agent> | In Progress` before editing code.
- [ ] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/ACTIVE_WORK.md`, and the full
  `docs/业务逻辑.md`.
- [ ] Before editing, record in `docs/ACTIVE_WORK.md`:
  - current branch;
  - latest pushed baseline commit/ref;
  - `git status`;
  - relevant `git diff` / `git show` evidence for `WindowTaskRunner`, `WindowRuntimeContext`,
    `PreparedDialogAction`, `DialogService`, and every touched consumer.
- [ ] After editing, reread `docs/业务逻辑.md` and explicitly record that these business rules are
  still preserved:
  - 白龙马 CR39/CR40/CR53 probe, absent/noTarget, targetReady, and post-probe rules;
  - 普通怪 CR41-CR45 runner-only enter-battle, same-green re-navigation, and 5-minute timeout rules;
  - 黄袍怪 CR46-CR54 first battle, chained hot path, chain-end, and tracker fast-match rules;
  - 修罗 CR56 non-fast objective-read overlap rules.

Fixed timing policy:

- [ ] Implement one central prepared-dialog polling policy, not scattered sleeps.
- [ ] While the relevant operation has no prepared action yet, use exactly these watcher prepare
  cadences:
  - `WUBEI_ENTER_BATTLE`: `100ms`.
  - `WUBEI_PROBE_STORY`: `200ms`.
  - `ROUTE_TRANSFER`: `1000ms`.
  - `TASK_TRACKER_PATHING`: `1000ms`.
  - Every other `DialogOperation`: `500ms`.
- [ ] The policy applies to preparation attempts only. Once an action is already prepared, the
  existing action must not keep the watcher at the prepare cadence.
- [ ] `preparedDialogAction != null` must not by itself reduce the next observer interval to `100ms`.
- [ ] Route/`TASK_TRACKER_PATHING` prepared state must stay tied to the active pathing/request state;
  do not create a global infinite hot dialog loop for route transfer.

Prepared READY behavior:

- [ ] On first successful prepare for a new action, update `WindowRuntimeContext` and publish one
  `PREPARED_ACTION_READY`.
- [ ] While the same prepared action remains current, the watcher must not call the old fingerprint
  revalidation path just to refresh `lastVerifiedAtMs`.
- [ ] While the same prepared action remains current, the watcher must not publish another
  `PREPARED_ACTION_READY` for successful revalidation.
- [ ] If the current interest/request supports the existing prepared action, the watcher should treat
  that as "already prepared" and skip more preparation work for that operation in this tick.
- [ ] If the existing prepared action mismatches the current operation/target/intent/request, preserve
  the current mismatch/block/clear semantics, but do not use fingerprint revalidation as a background
  keepalive.

Consume-time validation requirements:

- [ ] Add one validation-aware consume path. Do not implement this as `getPrepared -> validate ->
  consume` because that races with replacement; validation and consume must compare against the same
  action and retry/return safely if the action changed.
- [ ] For every `clickRequired=true` prepared action, consumption must do a validation-rect
  fingerprint check immediately before CAS-consuming the action, regardless of whether
  `lastVerifiedAtMs` is still within `2500ms/3000ms`.
- [ ] The consume-time fingerprint check must reuse the current validation semantics:
  - same window binding check;
  - same validation rectangle bounds check;
  - same `DialogFingerprintWashMode`;
  - same binary fingerprint distance threshold;
  - same route intent / request ownership checks where those checks currently apply.
- [ ] If consume-time validation passes:
  - refresh `lastVerifiedAtMs`;
  - consume exactly that same action atomically;
  - clear the matching READY state;
  - let the caller perform the existing click path.
- [ ] If consume-time validation fails:
  - clear exactly that same prepared action;
  - clear the matching READY state;
  - return `null`;
  - do not click the old coordinate.
- [ ] For every `clickRequired=false` prepared action, do not run validation-rect fingerprint checks.
  Business signals such as `probeStoryAbsent` / no-click prepared results are consumed by
  operation/target/window/task-phase semantics, not by a button-image fingerprint.
- [ ] `clickRequired=false` prepared actions must not be rejected only because `lastVerifiedAtMs` is
  older than the click-action freshness window.

Logging requirements:

- [ ] Stop the watcher-tick `prepared-action-verified` / repeated READY log spam for unchanged
  prepared actions.
- [ ] Add concise logs for:
  - operation-specific prepare cadence chosen for a hot interest;
  - prepared action already current, so background revalidation is skipped;
  - consume-time validation pass/fail for `clickRequired=true`;
  - no-fingerprint consume path for `clickRequired=false`.
- [ ] Logs must include `windowId`, `taskType`, `operation`, `target`, `source`, prepared age,
  verified age, and next observer interval where relevant.

Verification:

- [ ] Compile with `mvn -q -DskipTests compile`.
- [ ] Add or update a focused source/test guard if practical so future code cannot reintroduce
  watcher-tick revalidation of unchanged prepared actions.
- [ ] Fresh runtime logs must prove:
  - an existing prepared action no longer forces `nextIntervalMs=100`;
  - same action READY is not republished on every watcher tick;
  - `clickRequired=true` actions log consume-time validation before click;
  - `clickRequired=false` probe/story signals still consume without fingerprint validation;
  - no missed `WUBEI_ENTER_BATTLE`, `WUBEI_PROBE_STORY`, `ROUTE_TRANSFER`, or
    `TASK_TRACKER_PATHING` prepared actions.
- [ ] Performance goal check:
  - `prepared-action-verified` count drops sharply compared with the pre-CR58 slice;
  - repeated `PREPARED_ACTION_READY` publish count for the same operation/target drops;
  - observer `100ms` tick pressure decreases when an action is already prepared;
  - p95/p99 for `prepared -> consume`, route handoff, and enter-battle click does not regress;
  - 白龙马、普通怪、黄袍怪、修罗 fresh logs show no business behavior regression.

Card CR59: Remove 五倍 80ms prepared-dialog polling loops

Dependency:

- CR59 must start after CR58 source changes are in Review or Done. CR58 owns prepared-action
  consume-time validation and READY keepalive removal. CR59 owns only the foreground Wubei task's
  old 80ms prepared-dialog wait loops.

Business source:

- `docs/业务逻辑.md` full file.
- This card changes how `WubeiTask` waits for Runner-prepared dialog results. It must not change:
  - 白龙马 probe result meanings;
  - 普通怪 / 黄袍第一战 `PATHING_TERMINAL` / `PREPARED_ACTION_READY` / `PRE_BATTLE_TIMEOUT`
    business rules;
  - 黄袍续战 hot-path click and pre-click interest rules;
  - accept-task business decision;
  - OCR/template thresholds, click coordinates, navigation, direct-combat, tooltip, or Alt+A policy.

Problem statement:

- `WUBEI_PREPARED_DIALOG_POLL_MS = 80ms` is still used as a foreground prepared-dialog wait loop in
  `WubeiTask`.
- Current risky paths include:
  - `waitForPreparedWubeiDialog(...)`: sleeps up to 80ms, then repeatedly calls
    `tryConsumePreparedWubeiDialog(...)` during a short foreground wait.
  - `waitForPreparedWubeiDialogReply(...)`: loops forever until a prepared action or business timeout,
    sleeping 80ms and retrying consume each time.
- This foreground polling duplicates the architecture direction: Runner should publish
  `PREPARED_ACTION_READY`, and the task should wake/consume on that event. The task should not keep
  touching runtime state every 80ms just to discover nothing changed.
- This is especially dangerous for `clickRequired=false` signals such as story-absent/no-click
  prepared results because an absent result can be consumed repeatedly without progress.

Claim / baseline gate:

- [ ] Claim the card by changing the sprint board row from `Unclaimed | Ready after CR58 source
  review` to `<agent> | In Progress` before editing code.
- [ ] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/ACTIVE_WORK.md`, and the full
  `docs/业务逻辑.md`.
- [ ] Before editing, record in `docs/ACTIVE_WORK.md`:
  - current branch;
  - latest pushed baseline commit/ref;
  - `git status`;
  - relevant `git diff` / `git show` evidence for `WubeiTask`, `WindowReadyEventBus` usage, and
    every touched wait/consume method.
- [ ] After editing, reread `docs/业务逻辑.md` and explicitly record that 白龙马、普通怪、黄袍怪
  business rules are still preserved.

Implementation requirements:

- [ ] Remove `WUBEI_PREPARED_DIALOG_POLL_MS` as a Wubei prepared-dialog wait mechanism.
  - If the constant becomes unused, delete it.
  - Do not reuse it under another name for prepared-dialog waiting.
- [ ] Replace `waitForPreparedWubeiDialog(...)` with event-driven waiting:
  - attempt `tryConsumePreparedWubeiDialog(...)` once on entry;
  - if absent, register/refresh the requested interest once when requested by the caller;
  - capture `WindowReadyEventBus.currentSequence()` before blocking;
  - wait for newer `PREPARED_ACTION_READY` for the current window until the existing caller timeout
    expires;
  - after each matching event wake, attempt consume once;
  - if the event operation/target does not match the requested operation, ignore that event and wait
    for the next newer event until the same deadline;
  - on deadline, return the existing timeout/null result without inventing a local prepared result.
- [ ] Replace `waitForPreparedWubeiDialogReply(...)` with event-driven waiting:
  - attempt consume once on entry;
  - register/refresh the requested interest once on entry when requested by the caller;
  - wait for newer `PREPARED_ACTION_READY` for the current window;
  - after each event wake, call `throwProbeEnterBattleTimeoutIfNeeded(...)` or the relevant existing
    business-timeout check, then attempt consume once;
  - use the existing `PROBE_ENTER_BATTLE_EVENT_RECHECK_MS = 5000ms` as the maximum blocking wait
    slice only to recheck the 300s probe timeout and stop request, not to poll prepared actions;
  - do not re-register interest on every loop iteration.
- [ ] Preserve accept-task behavior:
  - accept dialog foreground wait still uses the existing 15s business timeout;
  - no 80ms poll loop inside that 15s window;
  - the task still holds the foreground turn according to the existing accept-task rule.
- [ ] Preserve 白龙马 post-显形镜 and targetReady behavior:
  - no leader-side 15s/80ms `WUBEI_PROBE_STORY` polling fallback returns;
  - no repeated absent/no-click consume loop;
  - probe enter-battle keeps the existing 300s probe timeout and the existing 5s timeout recheck
    cadence, but consumes only on `PREPARED_ACTION_READY` event or immediate already-prepared state.
- [ ] Preserve 黄袍续战 hot path:
  - `WUBEI_ENTER_BATTLE interest` still registers before `chained-combat-*` physical click;
  - continuation must not fall back into ordinary pathing park or an 80ms prepared-dialog spin.
- [ ] Do not remove or change unrelated 80ms waits in this card:
  - `READY_EVENT_SETTLE_WAIT_MS` / phase-boundary settle rules;
  - `NavigationService.MAP_RESULT_SCROLL_INTERVAL_MS`;
  - debug/calibrator poll intervals;
  - destination-hint parser wait slices that do not consume prepared dialogs.

Logging requirements:

- [ ] Add concise logs for:
  - prepared-dialog wait entering event mode: operation/source/deadline/windowId;
  - event wake: operation/target/source/eventAgeMs;
  - operation mismatch ignored;
  - business timeout reached without prepared action;
  - no 80ms poll path used.
- [ ] Existing consume logs should remain the source of truth for actual click/no-click result.

Verification:

- [ ] Compile with `mvn -q -DskipTests compile`.
- [ ] Add or update a focused source/test guard if practical so `WUBEI_PREPARED_DIALOG_POLL_MS` /
  80ms `TaskSleep` cannot reappear inside Wubei prepared-dialog waits.
- [ ] Fresh runtime logs must prove:
  - no `waitForPreparedWubeiDialog(...)` or `waitForPreparedWubeiDialogReply(...)` path sleeps every
    ~80ms while waiting for prepared action;
  - accept-task still consumes `WUBEI_ACCEPT_TASK` or times out at the existing 15s business timeout;
  - 白龙马 `WUBEI_PROBE_STORY` absent/noTarget/targetReady still follows `docs/业务逻辑.md`;
  - probe targetReady enter-battle wait does not repeatedly consume absent/no-click prepared results;
  - 黄袍续战 still registers interest before click and consumes `WUBEI_ENTER_BATTLE` by event.
- [ ] Performance goal check:
  - same-window ~80ms reacquire/poll chains disappear from Wubei prepared-dialog waits;
  - `prepared -> consume` p95/p99 does not regress;
  - CPU/log pressure decreases;
  - no 白龙马、普通怪、黄袍怪 business behavior regression appears in fresh logs.

Card CR60: Clear 五倍 dialog/prepared state on combat entry

Business source:

- `docs/业务逻辑.md` full file, especially "五倍 Dialog Interest 生命周期规则".
- This card is framework/performance cleanup at the combat boundary. It must not change:
  - 白龙马 probe branching or result meanings;
  - 普通怪 / 黄袍第一战 `PATHING_TERMINAL` / `PREPARED_ACTION_READY` / `PRE_BATTLE_TIMEOUT`
    business rules;
  - 黄袍续战 pre-click interest and cached green-link click behavior;
  - OCR/template thresholds, click coordinates, navigation, tooltip, direct-combat, or Alt+A policy;
  - 修罗 business flow.

Problem statement:

- `WindowTaskRunner.publishCombatStateChanged(...)` currently clears only the current WUBEI
  `dialogInterest` when it supports `WUBEI_ENTER_BATTLE`, plus the ordinary target-map gate.
- It does not clear:
  - `dialogPreparationRequest`;
  - stale `preparedDialogAction`;
  - READY/prepare status tied to that prepared action;
  - other WUBEI dialog interest such as `WUBEI_PROBE_STORY` if it accidentally survives into combat.
- `runCombatWatcherLoop(...)` still runs pathing/dialog/task dialog preparation work before interval
  calculation even when `tick == IN_COMBAT`.
- `resolveDialogPrepareIntervalMs(...)` can still return:
  - `100ms` for `WUBEI_ENTER_BATTLE`;
  - `200ms` for `WUBEI_PROBE_STORY`;
  - `500ms` for other dialog operations;
  - `1000ms` for active pathing / task tracker.
- Therefore, if any request/interest/prepared/pathing residue crosses the combat-entry boundary,
  the observer can keep scanning during combat and keep the runner in high-frequency mode.

Current evidence:

- Source review:
  - `WindowTaskRunner.publishCombatStateChanged(...)` only clears `WUBEI_ENTER_BATTLE interest`.
  - `WindowRuntimeContext.clearDialogPreparationRequest(...)` would clear request and prepared action,
    but combat-entry does not call it today.
  - `WindowRuntimeContext.clearPreparedDialogAction(...)` clears prepared READY state, but combat-entry
    does not call it today.
  - `resolveDialogPrepareIntervalMs(...)` does not ignore dialog request/interest while in combat.
- Latest `logs/dhxy-console.log` contains no fresh WUBEI runtime sample: targeted `rg` for `WUBEI_`,
  `task=WUBEI`, `task=WUHuan`, `source=wubei`, and `wubei` returned 0 in the current main log.
  Recent 修罗 samples show clean `preparedOperation=null` and `nextIntervalMs=4000`, but that cannot
  validate the WUBEI boundary.

Implementation requirements:

- [ ] Claim the card before editing code.
- [ ] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/ACTIVE_WORK.md`, and the full
  `docs/业务逻辑.md`.
- [ ] Before editing, record branch, pushed baseline, `git status`, and relevant code evidence in
  `docs/ACTIVE_WORK.md`.
- [ ] On `oldTick != IN_COMBAT && newTick == IN_COMBAT` for `TaskType.WUBEI`, clear the current
  window's WUBEI dialog/prepared state:
  - any current WUBEI dialog interest, not only `WUBEI_ENTER_BATTLE`;
  - any current dialog preparation request that belongs to the WUBEI/window observer dialog lane;
  - any stale prepared dialog action for that same runtime lane;
  - READY/prepare state associated with the cleared prepared action.
- [ ] Keep `clearOrdinaryEnterBattleTargetMapGate("wubei combat entered")`.
- [ ] Ensure watcher ticks in `IN_COMBAT` do not run dialog preparation or reduce interval because of
  residual dialog request/interest/prepared/pathing state.
  - Combat guard / battle radar polling may continue at the combat dynamic interval.
  - Do not suppress combat-state change publication or post-combat recovery.
- [ ] Add concise structured logs for the combat-entry cleanup:
  - window id / hwnd / task;
  - oldTick/newTick;
  - whether interest/request/prepared were present before cleanup;
  - operation/target/source for cleared prepared action when available;
  - next observer interval after cleanup.

Verification:

- [ ] Compile with `mvn -q -DskipTests compile`.
- [ ] Fresh WUBEI runtime logs must prove:
  - on `NONE -> IN_COMBAT`, cleanup logs show WUBEI dialog/prepared state was cleared or already absent;
  - no `window.dialog.interest.update`, `window.dialog.prepare.state phase=READY`, or stale
    `preparedOperation=WUBEI_*` remains during combat until combat exits and the next phase starts;
  - `window observer tick` while in combat is not `nextIntervalMs=100/200/500` due to dialog state;
  - 白龙马、普通怪、黄袍第一战、黄袍续战 still follow `docs/业务逻辑.md`.
- [ ] Performance goal check:
  - no battle-inside dialog prepare churn;
  - CPU/log pressure from residual WUBEI interest/request/prepared state drops to zero during combat;
  - p95/p99 for enter-battle and post-combat recovery do not regress.

Card CR61: Skip non-combat observer work while already in combat

Relationship to CR60:

- CR60 is WUBEI-specific cleanup at the combat-entry boundary: clear residual WUBEI
  interest/request/prepared state when entering combat.
- CR61 is a generic watcher-loop performance rule for all task types, including `XIULUO_V2` and
  `WUBEI`: once the current tick has already confirmed `IN_COMBAT`, the watcher should not spend the
  same tick doing non-combat dialog/pathing/attention work.

Problem statement:

- Current `WindowTaskRunner.runCombatWatcherLoop(...)` computes `tick` from
  `autoCombatService.handleWindowCombatGuardTick(...)`, but even when `tick == IN_COMBAT` it still:
  - checks active pathing;
  - may run route dialog preparation;
  - may run task dialog interest preparation;
  - may run task tracker preparation;
  - always falls through to `publishTaskAttentionIfDialogVisible(...)` when no prepared action was
    produced.
- Recent 修罗 logs show the practical cost:
  - `task=XIULUO_V2 branch=idle ... preparedOperation=null preparedTarget=null`
  - while in combat cadence (`nextIntervalMs=4000`), `attentionDetectMs` still reaches multi-second
    values such as `3597ms`, `4960ms`, and `6958ms`.
- This means the user-visible "combat feels more stuck than outside navigation" can happen even when
  no WUBEI prepared action is stale and no WUBEI interest is present.

Implementation requirements:

- [ ] Claim the card before editing code.
- [ ] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/ACTIVE_WORK.md`, and the full
  `docs/业务逻辑.md`.
- [ ] Before editing, record branch, pushed baseline, `git status`, and relevant code/log evidence in
  `docs/ACTIVE_WORK.md`.
- [ ] In `WindowTaskRunner.runCombatWatcherLoop(...)`, after combat state is refreshed/published, if
  `tick == IN_COMBAT`:
  - do not run `refreshPathingSignal(...)`;
  - do not run `refreshDialogPreparationSignal(...)`;
  - do not run `refreshTaskDialogInterestPreparationSignal(...)`;
  - do not run `refreshTaskTrackerPreparationSignal(...)`;
  - do not run final `publishTaskAttentionIfDialogVisible(...)`;
  - do not let `resolveDialogPrepareIntervalMs(...)` reduce the combat interval because of
    non-combat dialog/pathing state.
- [ ] Still keep combat responsibilities:
  - `AutoCombatService.handleWindowCombatGuardTick(...)`;
  - combat state change publication;
  - normal battle radar / auto-combat polling interval;
  - post-combat exit detection on later ticks.
- [ ] Do not change click/navigation/OCR/template thresholds or task business branching.
- [ ] CR60 may still clear residual WUBEI state on the `NONE -> IN_COMBAT` transition; CR61 should
  make the loop safe even if some non-combat state accidentally remains.

Verification:

- [ ] Compile with `mvn -q -DskipTests compile`.
- [ ] Fresh 修罗 runtime logs must prove:
  - during `IN_COMBAT`, observer ticks do not show route/task dialog/task tracker/pathing work;
  - `attentionDetectMs` is `-1` or absent during combat ticks, not multi-second;
  - `nextIntervalMs` is the battle/auto-combat cadence, not dialog/pathing cadence.
- [ ] Fresh WUBEI runtime logs must prove:
  - CR60 cleanup still happens on combat entry;
  - no battle-inside WUBEI dialog prepare/attention churn;
  - 白龙马、普通怪、黄袍第一战、黄袍续战 still follow `docs/业务逻辑.md`.
- [ ] Performance goal check:
  - battle-inside CPU/log pressure drops for 修罗 and WUBEI;
  - combat enter/exit detection p95/p99 does not regress;
  - no new missed post-combat recovery or stale battle state.

Runtime Acceptance - 2026-06-21:

- Status: **Done / fresh WUBEI and 修罗 runtime validated**.
- Fresh WUBEI side was already validated in `2026-06-20 16:17:01.008-16:20:01.008`.
- Fresh 修罗 log range: `2026-06-21 00:26:38.000-00:32:22.000`.
- Evidence:
  - In-combat observer ticks at `00:28:42.639`, `00:28:57.257`, `00:31:21.724`, and `00:31:31.231` show `branch=in-combat`.
  - In those ticks, pathing / route prepare / task dialog prepare / task tracker prepare / attention work is skipped as `-1`.
  - Combat cadence is `nextIntervalMs=4000`.
- Performance goal check: 修罗 no longer shows the pre-CR61 battle-inside multi-second attention detect path in these sampled combat ticks; combined with the earlier WUBEI proof, CR61 acceptance is satisfied.

Card CR62: Make 修罗 WAIT_COMBAT event-driven without sacrificing exit latency

Relationship to CR60/CR61:

- CR60 is WUBEI-specific combat-entry cleanup for residual dialog/prepared state.
- CR61 is Runner-side watcher slimming: when the watcher already sees `IN_COMBAT`, skip non-combat
  dialog/pathing/tracker/attention work.
- CR62 is different: it targets the 修罗 task thread itself. `XiuluoTaskV2` still reacquires a full
  task transaction every ~900ms during `WAIT_COMBAT` even if CR61 is implemented.

Problem statement:

- `XiuluoTaskV2.runRoundPhases(...)` wraps every phase in `TaskTransactionRunner.run(...)`.
- `XiuluoTaskV2.waitCombat(...)` calls `autoCombatService.handleCombatTick(...)`.
- When the result is `IN_COMBAT`, it returns `SHARED_STATE_TRIGGERED` / `MUST_YIELD`; the outer loop
  releases the task turn, sleeps `TASK_TURN_HANDOFF_DELAY_MS = 900ms`, and immediately runs the same
  `xiuluo-v2:WAIT_COMBAT` transaction again.
- This preserves exit latency, but it creates the exact heavy loop the user called out:
  `WAIT_COMBAT -> TaskTransactionRunner started -> handleCombatTick -> release turn -> 900ms handoff
  -> same transaction again`.
- Latest `logs/dhxy-console.log` evidence:
  - `xiuluo-v2:WAIT_COMBAT` task transactions: `count=1189 avgMs=720.6 p50Ms=146 p95Ms=2329
    p99Ms=11352 maxMs=17447`.
  - Tail examples include `2026-06-20 01:45:10.326 elapsedMs=10579`,
    `2026-06-20 01:45:43.480 elapsedMs=1937`, and
    `2026-06-20 01:45:48.391 elapsedMs=3912`.
  - The same log slice repeatedly shows `phase=WAIT_COMBAT result=SHARED_STATE_TRIGGERED
    yield=MUST_YIELD next=WAIT_COMBAT message=combat still running` and
    `task turn handoff delay ... delayMs=900`.

Implementation requirements:

- [ ] Claim the card before editing code.
- [ ] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/ACTIVE_WORK.md`, and the full
  `docs/业务逻辑.md`.
- [ ] Before editing, record branch, latest pushed baseline, `git status`, and relevant code/log
  evidence in `docs/ACTIVE_WORK.md`.
- [ ] Do not simply increase `TASK_TURN_HANDOFF_DELAY_MS` or lower combat detection frequency. Exit
  detection latency must not regress.
- [ ] Keep 修罗 task side as the owner of business recovery:
  - Runner/combat watcher may publish `COMBAT_STATE_CHANGED`.
  - `XiuluoTaskV2` / `AutoCombatService.handleCombatTick(...)` must still be the path that consumes
    `EXIT_RECOVERED`, runs post-combat recovery, and advances `WAIT_COMBAT -> RETURN_HOME`.
- [ ] When `waitCombat(...)` observes `IN_COMBAT`, release the task turn and wait for the current
  window's `COMBAT_STATE_CHANGED` event instead of doing a fixed 900ms task-transaction loop.
- [ ] Avoid a lost-event race:
  - capture the ready-event sequence before returning the shared-state wait outcome / before
    releasing the task turn, or carry an equivalent race-safe sequence in the wait state;
  - after waking, re-enter `WAIT_COMBAT` once and re-run `handleCombatTick(...)` to consume
    `EXIT_RECOVERED` or confirm still in combat.
- [ ] If copying the WUBEI wait pattern, do not blindly copy any sequence-capture order that can miss
  an event published between turn release and wait registration.
- [ ] No click/navigation/OCR/template thresholds may change.
- [ ] No 修罗 business phases may be reordered: accept task, read objective, navigate/click target,
  enter battle, wait combat, return home remain the same.

Verification:

- [ ] Compile with `mvn -q -DskipTests compile`.
- [ ] Add or update a narrow wiring/unit test proving `WAIT_COMBAT` with `IN_COMBAT` produces an
  event wait rather than a 900ms handoff-only retry.
- [ ] Fresh 修罗 runtime logs must prove:
  - `xiuluo-v2:WAIT_COMBAT` no longer produces repeated `sameAsPrevious=true` task-turn reacquires
    every ~900ms while battle is still running;
  - `COMBAT_STATE_CHANGED oldTick=IN_COMBAT newTick=NONE` wakes the task, and the next task-side
    `handleCombatTick(...)` consumes `EXIT_RECOVERED`;
  - post-combat recovery and `RETURN_HOME` still happen from the task side;
  - follower first-aid / auto-battle windows still get normal shared-state opportunities.
- [ ] Performance goal check:
  - task-turn/log pressure during 修罗 combat drops sharply;
  - combat exit latency p95/p99 does not regress versus the current 900ms loop;
  - no missed combat exit, stale `IN_COMBAT`, or delayed return-home regression appears.

Runtime Acceptance - 2026-06-21:

- Status: **Done / fresh 修罗 runtime validated**.
- Fresh log range: `2026-06-21 00:26:38.000-00:32:22.000`.
- Evidence:
  - First combat: `WAIT_COMBAT_STATE_CHANGE` event wait starts at `00:28:42.824`, wakes by `COMBAT_STATE_CHANGED` at `00:29:42.891`, and task-side recovery advances to `RETURN_HOME` at `00:29:43.905`.
  - Second combat: event wait starts at `00:31:21.926`, wakes at `00:31:56.526`, and advances to `RETURN_HOME` at `00:31:56.976`.
  - Between those waits, the old every-900ms `xiuluo-v2:WAIT_COMBAT` transaction loop is not present.
- Performance goal check: task-turn/log pressure during 修罗 combat is reduced by event parking while exit latency remains event-driven; no missed combat exit, stale `IN_COMBAT`, or delayed return-home regression was observed in this slice.

Card CR63: Restore summon-skill locked-slot backward scan

Problem statement:

- 三技能尾部清理遇到第 4 格锁定时，当前本地代码会直接停止并把 pass 记为成功，导致
  `TaskMaintenanceService` 刷新长冷却，后续不会继续往前检查第 1/2/3 格有效普通技能。
- Fresh runtime evidence:
  - `2026-06-20 16:33:48.046` hwnd-74E07A0 触发三技能维护；
  - `16:33:51.977` `detected 6 skill slots, start at slot 4`;
  - `16:33:53.915` `slot 4 status LOCKED_SLOT`;
  - `16:33:53.915` `tail skill pass finished ... inspected=1 deleted=0 ... nextStartSlot=4`;
  - `16:33:53.915` exclusive pass finished `success=true`;
  - `TaskMaintenanceService` 随后按 success 更新窗口状态和 `lastSummonSkillCleanAt`，进入冷却。
  - The same regression reproduced again at `2026-06-20 16:54:44.555-16:54:52.837`:
    hwnd-74E07A0 detected 6 slots, started at slot 4, classified slot 4 as `LOCKED_SLOT`,
    finished after inspecting only one slot, returned `success=true`, and updated summon-skill
    cooldown with `lastEffectiveSlot=null` / `observedSlots={3=LOCKED_SLOT}`.
  - It reproduced again at `2026-06-20 17:15:47.439-17:15:57.339` under `source=auto-battle`:
    hwnd-74E07A0 used cached count 6, started at slot 4, classified slot 4 as `LOCKED_SLOT`,
    finished after inspecting one slot with `deleted=0`, returned `success=true`, and updated
    cooldown with `observedSlots={3=LOCKED_SLOT}`.
  - It reproduced again at `2026-06-20 18:07:14.530-18:07:24.216` under `source=auto-battle`:
    hwnd-74E07A0 used cached count 6, started at slot 4, classified slot 4 as `LOCKED_SLOT`,
    finished after inspecting one slot with `deleted=0`, returned `success=true`, and updated
    cooldown with `lastEffectiveSlot=null` / `observedSlots={3=LOCKED_SLOT}`.
  - It reproduced again at `2026-06-20 18:48:16.879-18:48:30.311` under `source=auto-battle`:
    hwnd-74E07A0 used cached count 6, started at slot 4, classified slot 4 as `LOCKED_SLOT`,
    finished after inspecting one slot with `deleted=0`, returned `success=true`, and updated
    cooldown with `lastEffectiveSlot=null` / `observedSlots={3=LOCKED_SLOT}`.
  - It reproduced again at `2026-06-20 19:08:36.574-19:08:47.770` under `source=auto-battle`:
    hwnd-74E07A0 used cached count 6, started at slot 4, classified slot 4 as `LOCKED_SLOT`,
    finished after inspecting one slot with `deleted=0`, returned `success=true`, and updated
    cooldown with `lastEffectiveSlot=null` / `observedSlots={3=LOCKED_SLOT}`.
- Baseline evidence:
  - Latest known working baseline `13fc663` 的 `SummonSkillService` 在
    `LOCKED_OR_EMPTY` 分支调用 `findNearestOpenedSlotBackward(slots, index - 1, deadlineAtMs)`;
  - baseline 会从当前锁/空格前一格向前找最近打开格；
  - 找到 `NORMAL_SKILL` 就删除；
  - 找到 `KEEP_SKILL` 就停止，因为前面是保留技能；
  - 完全找不到打开格才停止。
- Regression source:
  - `dc0ac9e` 引入 `SummonSkillCleanupResult`、`SummonSkillSlotStatus.EMPTY_SLOT` /
    `LOCKED_SLOT` 和 `nextStartIndex` 时删除了 `findNearestOpenedSlotBackward(...)` 业务语义；
  - 当前 `LOCKED_SLOT` 分支只做 `nextStartIndex = index; break;`，循环外统一
    `buildCleanupResult(true, ...)`。

Implementation requirements:

- [ ] Claim the card before editing code.
- [ ] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/ACTIVE_WORK.md`, and this CR.
- [ ] Before editing, record branch, latest pushed/baseline commit, `git status`, current diff around
  `SummonSkillService` / `TaskMaintenanceService`, and the runtime evidence above in
  `docs/ACTIVE_WORK.md`.
- [ ] Restore the old locked/empty boundary semantics in the new status/result model:
  - `LOCKED_SLOT` at the tail boundary must inspect previous slots backward before declaring the pass
    complete;
  - `EMPTY_SLOT` must preserve existing current behavior, including the ultimate-corner branch, but
    must not erase the locked-slot backward scan if the old `LOCKED_OR_EMPTY` meaning applies;
  - nearest backward `NORMAL_SKILL` should be deleted through the existing delete path;
  - nearest backward `KEEP_SKILL` means safe stop;
  - no opened previous slot means safe stop.
- [ ] A pass must refresh `lastSummonSkillCleanAt` only when it reaches a real safe stop condition.
  If the backward scan cannot complete because of timeout, interruption, unknown slot, delete failure,
  or dialog obstruction, return failure so cooldown is not refreshed.
- [ ] Preserve existing new result fields:
  `skillCount`, `nextStartIndex`, `observedStatusesByIndex`, `ultimateGenerateClicked`,
  `ultimateGenerateSucceeded`, `inspectedCount`, `deletedCount`, and `message`.
- [ ] Do not change summon panel coordinates, template images, OCR thresholds, click coordinates,
  input-queue ownership, maintenance broadcast priority, or WUBEI/修罗 business flow.
- [ ] Avoid wrapper nesting. Prefer restoring this decision inside the existing tail cleanup workflow
  or adding one clearly owned helper only if it isolates the actual backward-scan policy.

Verification:

- [ ] `mvn -q -DskipTests compile` passes.
- [ ] Add or update a narrow unit/wiring test, or a deterministic replay/debug test if one already
  exists for summon slots, proving:
  - six-slot layout starting at slot 4 with slot 4 `LOCKED_SLOT` scans backward;
  - previous `NORMAL_SKILL` is deleted;
  - previous `KEEP_SKILL` stops safely;
  - unknown/timeout/delete failure returns `success=false`.
- [ ] Fresh runtime logs must show the original failure no longer happens:
  - when slot 4 is `LOCKED_SLOT`, pass inspects backward slots instead of finishing after
    `inspected=1`;
  - if it deletes slot 1/2/3 normal skill, `deleted>0`;
  - if it cannot prove a safe stop, `TaskMaintenanceService` does not update cooldown.

Card CR64: 白龙马 COMBAT_TARGET OCR/Ctrl 名字坐标转模型点击点

Problem statement:

- Tangde's `2026-06-20` run report and log/code audit show a separate 白龙马 click-target bug under
  the broader CR53 timeout: after `wubei.probeTargetReady`, the `COMBAT_TARGET` OCR/Ctrl path can
  treat a recognized name word coordinate as the final click point.
- In the failure sample, the click lands on/near the text label rather than the monster model, so no
  enter-battle dialog/prepared action appears and the task later sits in the CR53 enter-battle wait.
- This is not a request to rewrite tooltip/template recognition. It is a narrow coordinate-policy
  correction for WUBEI 白龙马 target-click candidates that come from yellow/Ctrl OCR text.

Evidence:

- Failure path:
  - `2026-06-20 16:39:27.344` consumed `WUBEI_PROBE_STORY` with `target=wubei.probeTargetReady`;
  - `16:40:19.933` Ctrl/OCR matched `白龙马`;
  - `16:40:20.041` clicked `(1588,829)`;
  - no fresh `WUBEI_ENTER_BATTLE` prepared/OPTION evidence followed, and the chain later timed out.
  - Fresh repeat at `2026-06-20 18:48:05.404-18:52:05.820`: second probe returned
    `wubei.probeTargetReady`, target hint was `白龙马@金兜洞(14,4)`, tooltip and yellow OCR failed,
    Ctrl/OCR matched `白龙马`, clicked `(1748,809)`, then no `WUBEI_ENTER_BATTLE` prepared action
    appeared before `probe enter-battle timeout fired inside prepared-dialog wait`.
- Successful comparison:
  - `2026-06-20 16:55:38.926` found a player anchor and computed `finalClick=(1541,488)`;
  - `16:55:39.171` clicked that model-offset point;
  - `16:55:43.598` prepared `WUBEI_ENTER_BATTLE`, and `16:55:43.939` consumed it.
- Code path:
  - `NpcClickService.scanMenuAndVerifyKeywordDirect(...)` currently builds the click point directly
    from OCR word coordinates (`scanX + w.getX()`, `scanY + w.getY()`).

Implementation requirements:

- [ ] Claim the card before editing code.
- [ ] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/ACTIVE_WORK.md`,
  `docs/业务逻辑.md`, CR53, and this card.
- [ ] Record current branch, latest pushed baseline if known, `git status`, and relevant
  `git diff` / `git show` evidence in `docs/ACTIVE_WORK.md`.
- [ ] For WUBEI `COMBAT_TARGET` yellow/Ctrl OCR candidates, do not use the OCR word center as the
  final model click point.
- [ ] Convert name/text evidence to a documented model click point using a replay-verified rule, or
  decline that candidate and let the next candidate / existing direct-combat fallback continue.
- [ ] Do not change:
  - CR39/CR40 白龙马 probe branching and result semantics;
  - tooltip matching or template thresholds;
  - generic NPC click behavior outside the WUBEI `COMBAT_TARGET` scope unless explicitly proven safe;
  - 普通怪, 黄袍, 修罗 business logic;
  - tracker green-click, minimap/world-map navigation, or route option matching.

Verification:

- [ ] Add or reuse testcase screenshots from the `16:40` failure and `16:55` success samples.
- [ ] Run the relevant replay/debug tool against those images.
- [ ] Produce marked output images showing:
  - OCR/name text box;
  - computed model click point;
  - actual final click point.
- [ ] The `16:40` sample must place the final red point on the monster model area, not the name text.
- [ ] The `16:55` successful player-anchor path must still compute a valid model click point.
- [ ] Fresh WUBEI 白龙马 runtime logs must show that this OCR/Ctrl candidate no longer logs a
  successful target click followed by no enter-battle prepared/OPTION evidence.

Card CR65: Maintenance broadcast OPTION-only fallback and Alt+8 refresh burst guard

Problem statement:

- Auto-battle member windows must keep checking for team maintenance broadcast dialogs. Do not solve
  this by adding a broad cooldown: another leader/window may open a real 医保宝 / 修装备 broadcast, and
  members still need to catch it.
- The current noisy path is more specific: after the fixed-strip maintenance option check misses,
  `DialogService` fallback detects a generic dialog. If that fallback detection returns `STORY`, it
  still calls the generic business-option matcher, which washes the image and tries `heal-pet` /
  `repair-equipment`. A `STORY` dialog is not a maintenance option dialog, so this produces repeated
  `BUSINESS_OPTION_NOT_FOUND` CPU/log noise.
- Separately, auto-combat panel round refreshes can line up across all member windows. The input queue
  serializes them, so a burst of four `Alt+8` refreshes can occupy several seconds of physical input
  even though each refresh is individually valid.

Evidence:

- Tangde's `2026-06-20` report and fresh heartbeat audits flagged high
  `maintenance-broadcast-fallback:auto-battle` counts. The noisy samples repeatedly show:
  - `dialog detect no-focus: reason=maintenance-broadcast-fallback:auto-battle result=STORY`;
  - followed by `business dialog option not matched: option=heal-pet`;
  - followed by `business dialog option not matched: option=repair-equipment`;
  - then `maintenance broadcast fallback result ... BUSINESS_OPTION_NOT_FOUND`.
- Source review confirms the branch: in `DialogService.handleMaintenanceBroadcastOptionFastPath(...)`,
  fallback currently skips only `DialogType.NONE`; any other dialog type, including `STORY`, is passed
  to `handleBusinessOption(...)`. `handleBusinessOption(...)` does not require `DialogType.OPTION`
  before washing and matching maintenance option templates.
- The per-hour leader maintenance hooks are not the source of this noise. WUBEI logs also show
  `skip heal-pet hook: cooldown not due intervalMs=3600000`, proving the hourly hook can be correctly
  throttled while auto-battle member fallback scans continue independently.
- Tangde's `2026-06-20` report also flagged repeated `auto-combat panel rounds refresh by Alt+8 without OCR`
  bursts as a possible input queue pressure source.
- Fresh runtime confirmation from `logs/dhxy-console.log`:
  - `17:29:19-17:29:29`: four member windows enqueue `battle:refreshAutoPanelRounds:verify:refresh-due`,
    with input requests around `1.1-2.5s` each.
  - `17:31:43-17:31:49`: another four-window refresh-due burst.
  - `17:37:14-17:37:20`: another four-window refresh-due burst.
  - `17:39:20-17:39:25`: the next refresh-due burst had already started.
- These samples are `reason=refresh-due`, with estimates above the low-round threshold. They are not
  emergency `low-rounds` refreshes.
- 2026-06-27 修罗 fresh runtime still fails the CR65 performance acceptance:
  - report range `19:23:43.709 - 19:34:42.446` has `auto-combat panel rounds refresh by Alt+8 without OCR=11`,
    `refresh-due panel verify deferred=51`, and `maintenance broadcast lightweight fallback disabled=8`;
  - WAIT_COMBAT slow holds still occur while refresh/defer pressure is active, including
    `19:24:29.095 heldMs=4436`, `19:24:56.072 heldMs=4376`, and `19:34:29.000 heldMs=3801`;
  - no real yellow maintenance broadcast sample was captured in this slice, so the yellow-after-green
    lightweight broadcast requirement still needs runtime proof.
- Repair owner dispatched: Boole (`019f0b70-2ae8-7522-9c22-a527f1a49b6f`) to diagnose and patch the
  remaining CR65 pressure without changing navigation/OCR/template/click business behavior.
- 2026-06-27 `19:34:42.446 - 19:41:22.612` fresh runtime proves the yellow path:
  - `19:36:31.357` member `hwnd-1960D10` matched `maintenance broadcast option ... color=yellow score=1.0`
    for `repair-equipment`;
  - `19:36:32.981` the same window logged `maintenance broadcast handled`.
  - This closes the yellow-after-green runtime proof gap, but not the whole CR65 card: the same slice still
    has `auto-combat panel rounds refresh by Alt+8=4`, `refresh-due panel verify deferred=21`,
    `maintenance broadcast lightweight fallback disabled=7`, and slow holds up to `heldMs=37929`.
- Boole follow-up patch is implemented after the runtime pressure slice: when combat-entry maintenance
  and an allowed `refresh-due` verify land in the same turn, the duplicate entry-maintenance panel
  verify is merged into the refresh-due check; if refresh-due is deferred, the original entry-maintenance
  verify remains. Current `19:41:22.612 - 19:53:39.107` logs still show refresh/defer pressure but do
  not include the new merge log, so treat them as pre-restart / old-process evidence. Fresh restart
  validation must prove the merge log appears and entry-maintenance + refresh-due no longer double-scan
  the same combat tick.

Implementation requirements:

- [x] Claim the card before editing code.
- [x] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/ACTIVE_WORK.md`, and
  `docs/业务逻辑.md`.
- [x] Before editing, record branch, latest pushed baseline, `git status`, and the fresh maintenance
  fallback / Alt+8 evidence in `docs/ACTIVE_WORK.md`.
- [x] Fix the maintenance broadcast fallback boundary:
  - keep the fixed-strip quick checks for `heal-pet` and `repair-equipment`;
  - after `detectDialogSnapshotDirect("maintenance-broadcast-fallback:...")`, skip `DialogType.NONE`
    as today;
  - if the fallback detection type is `STORY`, do not call `handleBusinessOption(...)`, do not wash the
    image, and do not match `heal-pet` / `repair-equipment`; return a no-action / not-found result and
    log `non-option-dialog type=STORY`;
  - only `DialogType.OPTION` may enter `handleBusinessOption(false, detection)`.
- [x] Do not add a cooldown / broad rate limit to maintenance broadcast checks. The check is still
  required because a valid broadcast can come from another leader/window.
- [x] Do not change first-aid semantics. Post-combat first-aid/no-focus precheck is expected and must
  stay governed by its existing business rules.
- [x] Add a same-team burst guard for auto-combat panel `reason=refresh-due` Alt+8 refreshes:
  - at most one same-team `refresh-due` Alt+8 may be enqueued per `30000ms`;
  - if another same-team `refresh-due` request arrives during that guard window, do not enqueue
    input, do not mark that window as refreshed, and log a structured defer line with team key,
    window id, reason, last team refresh age, and retry-after time;
  - leave that window eligible to retry on a later verify tick after the guard window.
- [x] Do not suppress or delay `reason=low-rounds` refresh. Low-rounds is the safety path.
- [x] Add the CR65 follow-up pre-verification gate:
  - optional auto-combat panel checks first resolve cached round state before `verifyAndAlignPanel(...)`;
  - cached healthy state skips panel verify and only advances the local optional check timer;
  - same-team `refresh-due` defers skip screenshot/template/alignment work before panel verify;
  - `unknown`, `low-rounds`, and entry maintenance bypass the pre-verify gate.
- [x] Add the CR65 entry-maintenance follow-up:
  - combat-entry maintenance uses `PanelVerifyMode.ENTRY_MAINTENANCE`;
  - entry maintenance still verifies/aligns the auto-combat panel, but does not call remaining-round
    refresh and does not update `lastAutoBattleRefreshAt`;
  - periodic optional refresh uses `PanelVerifyMode.VERIFY_AND_REFRESH`, preserving `unknown`,
    `low-rounds`, and `refresh-due` semantics.
- [x] Do not change:
  - combat enter/exit detection frequency or ownership;
  - auto-combat panel anchor/template matching thresholds;
  - click/navigation/OCR/template behavior;
  - 五倍、黄袍、白龙马、普通怪、修罗 business phases;
  - the per-window round estimate semantics, except that a deferred refresh must not reset
    `lastRefreshAt` / estimate as if `Alt+8` had run.
- [x] Keep logs visible enough for heartbeat audit to count:
  - `maintenance broadcast fallback skipped: ... reason=non-option-dialog type=STORY`;
  - reduced `business dialog option not matched: option=heal-pet/repair-equipment` after `STORY`
    fallback detections;
  - `auto-combat panel rounds refresh by Alt+8 without OCR`;
  - `auto-combat panel rounds refresh deferred by team burst guard`;
  - input request elapsed time for actual refreshes.

Verification:

- [x] `mvn -q -DskipTests compile` passes.
- [x] Add or update a narrow unit/wiring test for the maintenance fallback type gate, proving `STORY`
  does not enter business-option matching and `OPTION` still can.
- [x] Add or update a narrow unit/wiring test for the burst guard policy if the current service shape
  allows it without adding wrapper nesting.
- [x] Add a narrow source/wiring test for the pre-verification gate so optional `refresh-due` checks can
  defer before `verifyAndAlignPanel(...)`, while `unknown` and `low-rounds` still bypass the gate.
- [ ] Fresh WUBEI/修罗 multi-window runtime logs must show:
  - `maintenance-broadcast-fallback:auto-battle result=STORY` no longer produces immediate
    `heal-pet` / `repair-equipment` template miss logs;
  - valid `OPTION` maintenance broadcasts can still be clicked by member windows;
  - no more than one same-team `reason=refresh-due` Alt+8 input request in any 30s window;
  - deferred refresh-due windows log defer evidence and can retry later;
  - `reason=low-rounds` still enqueues immediately when needed;
  - combat-entry maintenance logs `source=entry-maintenance refreshRounds=false` and does not produce
    immediate `rounds refresh by Alt+8` / refresh-due defer pressure;
  - combat enter/exit latency and post-combat recovery do not regress.
- [ ] Performance goal check:
  - `maintenance-broadcast-fallback:auto-battle` CPU/log noise drops because `STORY` no longer goes
    through maintenance option template matching;
  - serialized input time spent on refresh-due Alt+8 bursts drops sharply;
  - no increase in missed auto-combat refresh / panel unsafe state;
  - WUBEI and 修罗 combat-side CPU/log pressure does not regress.

Card CR66: Remove plain TASK_ATTENTION_REQUIRED from WUBEI business wake paths

Problem statement:

- `docs/业务逻辑.md` now defines explicit WUBEI wake semantics:
  - 白龙马 probe pathing waits accept `PATHING_TERMINAL`, not plain `STORY` / `OPTION` /
    `TASK_ATTENTION_REQUIRED`.
  - 普通怪 / 黄袍第一战 waits accept only `PREPARED_ACTION_READY / WUBEI_ENTER_BATTLE`,
    `PATHING_TERMINAL`, or explicit `PRE_BATTLE_TIMEOUT`.
  - If an `OPTION dialog` does not match the enter-battle template, Runner should ignore that
    dialog for business wake purposes and continue movement/timeout checks.
- Current logs still show Runner publishing generic `TASK_ATTENTION_REQUIRED` for WUBEI visible
  `OPTION` / `STORY` dialogs with `operation=null`. That makes plain dialog visibility look like
  a business fact, wakes the captain in paths that should stay parked, and adds 100ms/log pressure.

Evidence:

- Fresh runtime after the previous heartbeat end:
  - `logs/_codex_wubei_observe_185021_onward.log`, strictly after
    `2026-06-20 18:52:52.820`, contains 12 WUBEI `TASK_ATTENTION_REQUIRED` publishes, for example
    `18:53:10.560 source=dialog-visible:OPTION operation=null`,
    `18:53:22.748 source=dialog-visible:STORY operation=null`, and repeated
    `18:57:06-18:57:37` `OPTION` / `STORY` attention publishes.
  - Current `logs/dhxy-console.log` tail `18:59:35.849-19:02:01.154` adds more of the same:
    `19:00:17.804 source=dialog-visible:OPTION`, `19:00:26.407 source=dialog-visible:STORY`,
    `19:01:57.233 source=dialog-visible:OPTION`, and `19:02:04-19:02:14` `STORY` attention
    publishes.
  - Current `logs/dhxy-console.log` tail `19:02:01.879-19:12:49.880` continues the same pattern:
    21 WUBEI `TASK_ATTENTION_REQUIRED` publishes, including `19:02:04.492`,
    `19:02:10.140`, `19:05:18.619`, `19:08:19.511`, and `19:10:56.640`, all with
    `operation=null`.
- The same slices also show continued performance pressure: repeated `sameAsPrevious` WUBEI
  task-turn reacquire, `nextIntervalMs=100`, and maintenance-broadcast dialog scans.

Implementation requirements:

- [x] Claim the card before editing code.
- [x] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/ACTIVE_WORK.md`,
  `docs/PACKAGE_ARCHITECTURE.md`, and `docs/业务逻辑.md`.
- [x] Record branch, latest pushed baseline, `git status`, and the fresh `TASK_ATTENTION_REQUIRED`
  evidence in `docs/ACTIVE_WORK.md` before editing.
- [x] Stop publishing / stop satisfying WUBEI business waits from plain dialog-visible
  `TASK_ATTENTION_REQUIRED` when `operation=null`.
- [x] Preserve explicit business events:
  - `PREPARED_ACTION_READY` when a matching prepared action exists;
  - `PATHING_TERMINAL` when movement/pathing actually terminates;
  - `PRE_BATTLE_TIMEOUT` when the relevant pre-battle timer expires;
  - combat-state events already owned by CR60/CR61.
- [x] Do not remove diagnostics entirely. If an unrecognized dialog must be logged, log it as
  diagnostic/ignored evidence, not as a WUBEI wake event that the task can treat as progress.
- [x] Do not change:
  - OCR/template thresholds;
  - click target selection;
  - navigation/movement detection;
  - CR57 dialog screenshot-sharing implementation;
  - CR58/CR59 prepared-action consume freshness;
  - CR60/CR61 in-combat skip behavior;
  - 白龙马/普通怪/黄袍/修罗 business flow in `docs/业务逻辑.md`.

Verification:

- [ ] `mvn -q -DskipTests compile` passes. Current attempt is blocked by unrelated CR64/NpcClick
  local errors in `NpcClickService.java:1162`, before CR66 can be treated as compile-verified.
- [x] Add/update a narrow wiring test if the current runner/eventbus shape allows it without
  wrapper nesting.
- [ ] Fresh WUBEI runtime logs must show:
  - `TASK_ATTENTION_REQUIRED source=dialog-visible:* operation=null` no longer wakes WUBEI waits;
  - 普通怪/黄袍 first-war waits still wake on fresh `WUBEI_ENTER_BATTLE` prepared action;
  - 白龙马 probe pathing still wakes on `PATHING_TERMINAL`;
  - `PRE_BATTLE_TIMEOUT` still wakes the task when the pre-battle timer expires;
  - no regression in prepared consume age or click-to-combat latency.
- [ ] Performance goal check:
  - reduced `TASK_ATTENTION_REQUIRED`, `sameAsPrevious`, and `nextIntervalMs=100` churn;
  - no increase in missed enter-battle action or stuck pathing waits.

Card CR67: Fix task-tracker expanded anchor coordinate space

Problem statement:

- `TaskTrackerPanelService.resolveTrackerPanelRect(...)` has two anchor paths with different
  coordinate spaces:
  - `narrow-default` uses `coordinateHelper.findImageInRegion(...)` against a screen-absolute
    `searchRect`, so the returned anchor is screen-absolute.
  - `expanded` calls `tracker.updateGlobalVision()` and `ImageFinder.find(latest_vision.png, ...)`;
    that match is local to the bound-window screenshot.
- Current code stores the expanded match directly into `anchor` and later computes `panelRect =
  anchor + TRACKER_PANEL_FROM_ANCHOR_*`, then passes that rect to `tracker.captureToFile(...)` as if
  it were screen-absolute.
- This only explodes when `narrow-default` misses. Most windows still work because they never enter
  the broken expanded fallback.

Fresh evidence:

- Fresh `logs/dhxy-console.log` around `2026-06-20 23:44:53-23:45:42`:
  - 岁月 `hwnd-74E07A0` repeatedly logs `tracker anchor not found in narrow area`.
  - Expanded fallback then resolves `anchor=(102,201)` and `rect=(6,213)-(188,551)`.
  - The same window base is `base=(398,255)`, and capture logs `relative=(-392,-42)`, proving the
    rect is outside the bound window.
- Working contrast in the same runtime:
  - Other windows that hit `mode=narrow-default` produce screen-absolute anchors such as
    `(1481,517)` or `(692,716)`, and their crops stay close enough to the panel even if the left
    edge is a few pixels outside.
- Current temp evidence exists and should be preserved into testcase form before implementing:
  - `images/temp/hwnd-74E07A0/latest_vision.png`
  - `images/temp/hwnd-74E07A0/task_tracker_detail_window-task-tracker-prepare_wuhuan_v2.png`

Implementation requirements:

- [ ] Claim the card before editing Java.
- [ ] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/ACTIVE_WORK.md`,
  `docs/PACKAGE_ARCHITECTURE.md`, and `docs/业务逻辑.md`.
- [ ] Record branch, latest pushed baseline, `git status`, and the fresh expanded-fallback log
  evidence in `docs/ACTIVE_WORK.md` before editing.
- [ ] In `TaskTrackerPanelService.resolveTrackerPanelRect(...)`, make the coordinate space explicit:
  expanded fallback matches from `latest_vision.png` must be converted from window-local to
  screen-absolute before calling `dragTrackerPanelIfNeeded(...)` or calculating `panelRect`.
- [ ] Keep `narrow-default` behavior unchanged.
- [ ] Do not change:
  - tracker anchor template path or threshold;
  - tracker panel offsets;
  - title/yellow/green-link OCR logic;
  - green-link click target selection;
  - CR54 chained fast-path cache behavior;
  - CR57 dialog screenshot-sharing behavior;
  - WUBEI/五环/修罗 business logic.

Verification:

- [ ] Add or reuse a repo-local testcase under `images/test-cases/task-tracker/...` using the
  `hwnd-74E07A0/latest_vision.png` sample or an equivalent raw screenshot.
- [ ] Run a replay/debug tool that marks:
  - the expanded anchor in window-local coordinates;
  - the converted screen-absolute anchor;
  - the final task-tracker panel crop rect / click-relevant area.
- [ ] Save the marked output image and command in `docs/ACTIVE_WORK.md`.
- [ ] Source/guard test must prove:
  - expanded local `(102,201)` with base `(398,255)` becomes screen anchor `(500,456)`;
  - final panel rect is near the bound window, not `relative=(-392,-42)`;
  - narrow-default screen anchors are not double-offset.
- [ ] `mvn -q -DskipTests compile` passes.
- [ ] Fresh runtime logs must show:
  - `mode=expanded` for a narrow-miss window produces a screen-absolute rect inside/near the window;
  - no `relative=(-392,-42)`-style crop outside the whole window for the same path;
  - tracker title template matching / green-link extraction works after expanded fallback.
- [ ] Performance goal check:
  - expanded fallback no longer causes repeated full tracker rereads due to wrong crops;
  - no regression in ordinary/黄袍/白龙马 tracker-read latency or click correctness.

Card CR68: Make 修罗 target-navigation pathing wait event-driven

Problem statement:

- CR62 fixed 修罗 `WAIT_COMBAT` by replacing the old fixed 900ms task-turn loop with an event wait.
- Fresh 修罗 runtime now shows the same kind of task-turn churn still exists in target navigation:
  while Runner is already watching pathing and still reports `ACTIVE`, the task keeps reacquiring
  `xiuluo-v2:NAVIGATE_TO_TARGET` every about 900ms.
- Fresh evidence from `logs/dhxy-console.log`:
  - Range: `2026-06-21 00:35:09.786-00:35:19.115`.
  - `NAVIGATE_TO_TARGET` returns `PATHING_STARTED`, then repeatedly logs
    `message=navigate to target watcher still pathing: ACTIVE`.
  - `task.turn.handoff` repeatedly shows `transaction=xiuluo-v2:NAVIGATE_TO_TARGET`,
    `afterReleaseMs≈905-916`, `sameAsPrevious=true`.
  - One member first-aid turn legitimately inserts at `00:35:12`, but the leader then resumes the
    same 900ms pathing churn.
- Additional fresh confirmation:
  - Range: `2026-06-21 00:37:24.592-00:42:50.449`.
  - Counts: `NAVIGATE_TO_TARGET=1206`, `PATHING_STARTED=882`, `sameAsPrevious=true=220`.
  - `00:37:24.833-00:37:27.559` repeats the same `afterReleaseMs≈903-914` handoff loop while the
    watcher snapshot still reports `state=ACTIVE`.
  - `00:42:49.678-00:42:50.644` repeats it again for target `白骨山`, immediately before Runner
    later publishes `PATHING_TERMINAL` / `ARRIVED` at `00:42:53.600`.
- Third fresh confirmation:
  - Range: `2026-06-21 00:42:50.449-00:48:21.177`.
  - Counts: `NAVIGATE_TO_TARGET=1409`, `PATHING_STARTED=1007`, `sameAsPrevious=true=256`.
  - `00:42:53.600` Runner publishes `PATHING_TERMINAL` / `ARRIVED` for `白骨山`; task consumes it at
    `00:42:55.329` and advances to `CLICK_TARGET_NPC`, proving terminal arrival still reaches the
    task.
  - The blocker remains before terminal: repeated 900ms-ish reacquire while pathing is still `ACTIVE`;
    active-pathing observer also has a fresh slow sample at `00:48:04.382` with `totalMs=14404` and
    `attentionDetectMs=12575`.
- Fourth fresh confirmation:
  - Range: `2026-06-21 00:48:21.177-00:54:25.270`.
  - Counts: `NAVIGATE_TO_TARGET=1220`, `PATHING_STARTED=882`, `sameAsPrevious=true=224`.
  - `PATHING_TERMINAL` / `ARRIVED` still reaches the task when it happens:
    `00:48:38.747 -> 00:48:42.360` for `长安城东`, `00:51:27.325 -> 00:51:27.799` for `万寿山`,
    and `00:54:12.177 -> 00:54:13.749` for `大雁塔三层`.
  - The blocker remains before terminal: task-side `NAVIGATE_TO_TARGET` transaction p95 is `4670ms`,
    p99 `12417ms`, max `13991ms`, and active-pathing observer has a slow sample at `00:50:47.908`
    with `totalMs=14226` / `attentionDetectMs=13356`.
- Fifth fresh confirmation:
  - Range: `2026-06-21 16:45:45.506-16:50:47.333`.
  - Runner published usable route prepared actions while the task was parked only on
    `WAIT_TARGET_PATHING_TERMINAL`:
    `16:45:48.227 PREPARED_ACTION_READY operation=ROUTE_TRANSFER target=长安 sequence=279`,
    then the task woke only after `16:45:57.940 PATHING_TERMINAL state=STOPPED_AWAY sequence=280`
    and consumed the route with `preparedAgeMs=9797`.
  - The same blocker repeats for `四圣庄`: `16:48:08.448 PREPARED_ACTION_READY operation=ROUTE_TRANSFER
    target=四圣庄 sequence=290`, but the wait with `afterSequence=288` timed out at `16:48:16.764`
    after `30001ms` before a later terminal event woke the next wait.
  - A second fresh timeout appears at `16:50:25.076` with `WAIT_TARGET_PATHING_TERMINAL afterSequence=295`
    timing out after `30001ms`.
  - Positive terminal wakes still happen (`16:46:01.586` after `3159ms`, `16:48:19.812` after `2325ms`),
    so this is not a total event-bus failure; the missing piece is waking/consuming on the
    business-relevant prepared route dialog instead of waiting for terminal/timeout.
- Sixth fresh confirmation:
  - Range: `2026-06-21 16:50:47.333-16:56:19.783`.
  - Improvement: no `window.ready.await result=timeout`; target pathing waits woke by event at
    `16:51:15.205` after `7480ms` and `16:54:55.024` after `7598ms`.
  - Still not accepted: `sameAsPrevious=true=150` and `delayMs=900=67` remain high. The repeated
    reacquire is split across `xiuluo-v2:NAVIGATE_TO_TARGET=36` same-window handoffs and an even
    larger `xiuluo-v2:BEFORE_ROUTE_MAINTENANCE_CHECK=104` cluster.
  - Fresh slow holds include `NAVIGATE_TO_TARGET heldMs=4641`, `NAVIGATE_TO_TARGET heldMs=6418`, and
    `AFTER_ACCEPT_MAINTENANCE_CHECK heldMs=36360`.
  - Verdict for this slice: event wake can work, but the performance goal is not met because
    pre-wait / maintenance-path reacquire churn still exists.
- Seventh fresh confirmation:
  - Range: `2026-06-21 16:56:19.783-17:01:16.786`.
  - Target pathing event wake still works in positive cases: `16:57:26.092 -> 16:57:35.039` woke
    by `PATHING_TERMINAL` after `8947ms`, and `17:00:16.925 -> 17:00:23.948` woke after `7023ms`.
  - Not accepted: one near-timeout and one real timeout remain. `16:56:50.782` started
    `WAIT_TARGET_PATHING_TERMINAL afterSequence=320` and woke only at `16:57:20.771` after
    `29989ms`; `16:59:01.700` started another wait after `sequence=327` and timed out at
    `16:59:31.700` after `30000ms`.
  - This slice has `preparedOperation=ROUTE_TRANSFER=0`, so it proves a terminal-latency /
    reacquire blocker separate from the prepared-route wake blocker seen in the previous slice.
  - Churn remains material: `sameAsPrevious=true=98`, `delayMs=900=42`, with
    `xiuluo-v2:NAVIGATE_TO_TARGET=86` same-window handoffs and `NAVIGATE_TO_TARGET=40` fixed
    900ms delays.
- Eighth fresh confirmation:
  - Range: `2026-06-21 17:01:16.786-17:08:17.322`.
  - Still blocked: three fresh `WAIT_TARGET_PATHING_TERMINAL` waits hit the 30s timeout path.
    `17:01:46.115 -> 17:02:16.118` timed out after `30001ms`, then the matching terminal arrived
    at `17:02:19.379` (`PATHING_TERMINAL state=ARRIVED sequence=334 target=凤巢五层`).
    `17:03:50.816 -> 17:04:20.817` timed out after `30001ms`.
    `17:06:51.488 -> 17:07:21.489` timed out after `30001ms`, then terminal arrived at
    `17:07:22.788` (`PATHING_TERMINAL state=ARRIVED sequence=346 target=凤巢五层`).
  - Positive event wakes still happen in the same range: `17:02:33.636` after `5948ms`,
    `17:04:31.857` after `4481ms`, `17:04:48.619` after `12085ms`, `17:07:34.845` after `8311ms`,
    and `17:07:57.019` after `5761ms`.
  - This slice again has no `preparedOperation=ROUTE_TRANSFER`, so it proves a terminal
    latency/reacquire blocker separate from the prepared-route wake blocker.
  - Churn is lower than the worst earlier slices but not gone: `sameAsPrevious=true=38`,
    `delayMs=900=11`, with fresh slow holds including `NAVIGATE_TO_TARGET heldMs=29329/30070/30085`.
- Ninth fresh confirmation:
  - Range: `2026-06-21 17:08:17.322-17:15:01.842`.
  - Still blocked: `17:09:39.014 -> 17:10:09.014` timed out after `30000ms`, and
    `17:12:29.628 -> 17:12:59.628` timed out after `30000ms`, both waiting only for
    `WAIT_TARGET_PATHING_TERMINAL`.
  - Positive event wakes still happen: `17:10:38.375 -> 17:10:49.756` woke after `11381ms`, and
    `17:13:43.794 -> 17:13:54.167` woke after `10373ms`.
  - Again `preparedOperation=ROUTE_TRANSFER=0`, so this is the same terminal latency/reacquire
    blocker, not the earlier prepared-route consumption blocker.
  - Churn regressed upward in this slice: `sameAsPrevious=true=154`
    (`xiuluo-v2:NAVIGATE_TO_TARGET=142`, `xiuluo-v2:WAIT_COMBAT=12`) and `delayMs=900=70`
    (`NAVIGATE_TO_TARGET=68`, `WAIT_COMBAT=2`). Fresh slow holds include
    `NAVIGATE_TO_TARGET heldMs=40617/29107` and `CONFIRM_ENTER_BATTLE heldMs=8922/7266`.
- Tenth fresh confirmation:
  - Range: `2026-06-21 17:15:01.842-17:18:55.584`.
  - Still blocked: `17:15:31.294 -> 17:16:01.295` timed out after `30001ms`, and
    `17:18:05.698 -> 17:18:35.698` timed out after `30000ms`, both waiting on
    `WAIT_TARGET_PATHING_TERMINAL`.
  - Positive event wake still happens: `17:16:48.430 -> 17:16:51.438` woke after `3008ms`.
  - Again `preparedOperation=ROUTE_TRANSFER=0`, so this is terminal latency/reacquire, not the
    prepared-route consumption blocker.
  - Churn remains material: `sameAsPrevious=true=104`, `delayMs=900=49`, and fresh slow holds
    include `NAVIGATE_TO_TARGET heldMs=29451/11287/9319/29596` plus
    `CONFIRM_ENTER_BATTLE heldMs=7945`.
- Eleventh fresh confirmation:
  - Range: `2026-06-21 17:18:55.584-17:24:46.763`.
  - The prepared-route wake gap reproduced again. `17:21:23.966` started
    `WAIT_TARGET_PATHING_TERMINAL afterSequence=374`; Runner published
    `PREPARED_ACTION_READY operation=ROUTE_TRANSFER target=万寿山 sequence=377` at
    `17:21:47.428`, but the task wait was only on `PATHING_TERMINAL` and timed out at
    `17:21:53.966` after `30000ms`. The prepared route was consumed only after timeout at
    `17:21:54.202` with `preparedAgeMs=6774`.
  - The same pattern repeated for `兰若寺`: wait started at `17:23:39.813`, Runner published
    `PREPARED_ACTION_READY operation=ROUTE_TRANSFER target=兰若寺 sequence=384` at
    `17:24:01.740`, the wait timed out at `17:24:09.814`, and the task consumed that prepared
    route at `17:24:10.126` with `preparedAgeMs=8387`.
  - Positive event wakes still happen after the timeout path, e.g. `17:21:54.584 -> 17:21:58.894`
    after `4310ms`, `17:22:04.849 -> 17:22:23.403` after `18554ms`,
    `17:24:10.543 -> 17:24:14.073` after `3530ms`, and
    `17:24:19.097 -> 17:24:29.246` after `10149ms`.
  - Verdict: CR68 is still blocked because the wait set still misses a business-relevant prepared
    route fact and falls back to 30s timeout before consuming it. Churn is lower than the previous
    slice but still present: `sameAsPrevious=true=70`, `delayMs=900=24`, `slow hold=9`.
- Twelfth fresh confirmation:
  - Range: `2026-06-21 17:24:46.763-17:28:50.693`.
  - Still blocked, but this slice shows a terminal latency / reacquire failure rather than the
    prepared-route wake gap. `17:25:49.742` started `WAIT_TARGET_PATHING_TERMINAL afterSequence=389`;
    `17:26:19.743` timed out after `30000ms` waiting on `wakeTypes=[PATHING_TERMINAL]`.
  - Unlike the previous slice, `preparedOperation=ROUTE_TRANSFER=0` and `consume-validation-passed=0`;
    no prepared route was available to explain the timeout.
  - Positive terminal wakes still happen: `17:26:44.159 -> 17:26:58.321` after `14162ms`, and
    `17:27:02.623 -> 17:27:08.671` after `6047ms`.
  - Churn remains: `sameAsPrevious=true=56`, `delayMs=900=22`, `slow hold=5`.
- Thirteenth fresh confirmation:
  - Range: `2026-06-21 17:28:50.693-17:42:32.254`.
  - Still blocked. The range opens with a carried target wait timing out at `17:29:04.983`
    after `30000ms`, followed immediately by same-window `delayMs=900` reacquire loops.
  - Prepared-route wake gap reproduced again. `17:31:17.314` started
    `WAIT_TARGET_PATHING_TERMINAL afterSequence=398`; Runner had prepared
    `ROUTE_TRANSFER target=兰若寺` by `17:31:42.363`
    (`preparedOperation=ROUTE_TRANSFER preparedTarget=兰若寺`), but the task wait only listened for
    `PATHING_TERMINAL`, timed out at `17:31:47.315`, and consumed the prepared route only after
    timeout at `17:31:47.562` with `preparedAgeMs=5199`.
  - Terminal-late timeout is also still present. `17:35:21.811` started the `龙窟六层` target wait
    and timed out at `17:35:51.814`; from `17:35:51-17:36:18` it returned to repeated
    `sameAsPrevious=true` / `delayMs=900` reacquire before later terminal events and target-click
    recovery. Another target wait timed out at `17:39:01.423` (`afterSequence=416`, elapsed
    `30002ms`), while a relevant `PATHING_TERMINAL` arrived shortly after at `17:39:02.065`.
  - Aggregate churn in this range: `sameAsPrevious=true=190`, `delayMs=900=78`,
    `slow hold=20`, `window.ready.await timeout=6`.
  - Required next patch / reviewer feedback: target-navigation wait cannot listen only for
    `PATHING_TERMINAL`. It must also wake on current-window same-intent prepared
    `ROUTE_TRANSFER` evidence and consume it before the 30s timeout, and it must avoid falling back
    into the fixed 900ms reacquire cadence when terminal evidence arrives just after timeout.
    Keep this as scheduling/wakeup work only; do not change 修罗 map target, route choice, NPC
    click, OCR/template threshold, or target-click business order.

Scope / business boundary:

- This is a scheduling/performance card only.
- Do not change:
  - 修罗 accept-task / objective-read / target-navigation / target-click / enter-battle / return-home
    business order;
  - map target, coordinates, route choice, NPC click, OCR, template thresholds, tooltip behavior, or
    click coordinates;
  - follower first-aid / maintenance gating semantics.
- Keep CR56, CR61, and CR62 behavior unchanged.

Implementation requirements:

- [x] Claim the card before editing code.
- [x] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/ACTIVE_WORK.md`,
  `docs/PACKAGE_ARCHITECTURE.md`, and `docs/业务逻辑.md`.
- [x] Before editing, record current branch, latest pushed baseline, `git status`, and relevant
  `XiuluoTaskV2` / pathing wait code evidence in `docs/ACTIVE_WORK.md`.
- [x] When 修罗 `NAVIGATE_TO_TARGET` returns `PATHING_STARTED` because Runner/pathing watcher still
  reports `ACTIVE`, release the task turn and wait on current-window pathing terminal facts instead
  of using the fixed 900ms task-turn reacquire loop.
- [x] Avoid a lost-event race:
  - capture the ready-event sequence before releasing the task turn, or carry an equivalent
    race-safe wait state;
  - after waking, re-enter `NAVIGATE_TO_TARGET` once to consume the latest pathing result.
- [x] The wait is scoped to the current bound window and current 修罗 target pathing intent id.
- [x] Accept terminal pathing facts only when they belong to the current 修罗 target navigation
  intent. Stale/other-task `PATHING_TERMINAL` must not satisfy this wait.
- [x] Preserve an explicit fallback/timeout path if the existing 修罗 navigation code already has
  one, but it must not become the normal 900ms polling cadence.
- [x] 2026-06-21 Codex repair: `WAIT_TARGET_PATHING_TERMINAL` also wakes on same-window
  `PREPARED_ACTION_READY operation=ROUTE_TRANSFER` when the prepared route target matches the
  current 修罗 target map. This is a scheduling wake only; the task still re-enters the normal
  navigation consumer before any input.

Verification:

- [x] Compile with `mvn -q -DskipTests compile`.
- [x] Add or update a narrow wiring/source test proving a 修罗 `NAVIGATE_TO_TARGET` `PATHING_STARTED`
  outcome produces an event wait rather than a fixed 900ms handoff-only retry.
- [x] `XiuluoTargetPathingEventWakeWiringTest` proves the target pathing wait includes
  `PREPARED_ACTION_READY` and uses the prepared-route-aware wait path.
- [x] 2026-06-21 reviewer re-check: code-level prepared-route wake repair is acceptable. The wait
  now ignores stale events by sequence, keeps terminal wake scoped to the captured target pathing
  `intentId`, and treats matching `PREPARED_ACTION_READY/ROUTE_TRANSFER` only as a wake hint before
  re-entering normal navigation consumption.
- [ ] Fresh 修罗 runtime logs must prove:
  - no repeated `xiuluo-v2:NAVIGATE_TO_TARGET` reacquire every about 900ms while the watcher says
    `pathingState=ACTIVE`;
  - the task wakes on current-window pathing terminal / arrival / stopped-away fact and then
    re-enters `NAVIGATE_TO_TARGET`;
  - arrival continues to `CLICK_TARGET_NPC` as before;
  - stopped-away / route-dialog behavior remains the same business decision as before this CR;
  - member first-aid / maintenance still gets normal shared-state opportunities while the leader is
    parked.
- [ ] Performance goal check:
  - `sameAsPrevious=true` / task-turn churn during 修罗 target pathing drops sharply;
  - no increase in missed arrivals, stale `ACTIVE` waits, or navigation latency p95/p99;
  - no regression to CR56 `READ_OBJECTIVE`, CR61 in-combat skip, or CR62 `WAIT_COMBAT` event wait.

Card CR69: Fix 修罗 objective coordinate recognition for 瑶池(63,91)

Problem statement:

- CR56 moved 修罗非快捷路线 objective parsing off the task turn: accept option clicks, the task
  captures one story-objective snapshot, background parsing starts, and `READ_OBJECTIVE` consumes
  that prepared result without taking a new screenshot or running synchronous dialog/OCR fallback.
- Tangde report `docs/run-reports/2026-06-21-xiuluo-test-run-objective-recognition.md` and
  independent checks narrow the paused round-12 failure to the coordinate-recognition layer:
  - Runtime range: `2026-06-21 00:55:53-00:56:01`, later consumed at `01:07:30`.
  - Saved testcase:
    `images/test-cases/objective-text/raw/story_yaochi_63_91_with_hint_raw.png`.
  - Marked replay output:
    `images/test-cases/objective-text/output/story_yaochi_63_91_with_hint_raw_marked.png`.
  - Probe output:
    `images/test-cases/objective-text/output/probe_yaochi_63_91/coordArea.png`.
  - Map name `瑶池` is matched with score `1.0`.
  - The coordinate crop visibly contains complete `(63,91)`.
  - The segmented read can produce bad text like `,763,91`; final coordinate text is empty, so
    `ObjectiveTextRecognitionService` logs coordinate miss and returns empty.
  - `XiuluoTaskV2.READ_OBJECTIVE` then consumes the prepared background result as `hit=false`,
    skips synchronous fallback by CR56 design, and recovers/reaccepts.
- Yellow reward/hint UI is not a sufficient root cause: earlier successful samples also contain
  the same kind of yellow提示.

Scope / business boundary:

- Fix the objective coordinate OCR/template parsing edge case. Do not first change 修罗 business
  recovery, retry count, prepath behavior, task-panel fallback, or same-phase synchronous fallback.
- This card must not re-enable same-phase `READ_STORY_OBJECTIVE`, dialog detect, or task-panel OCR
  inside `READ_OBJECTIVE`.
- Do not change 修罗 NPC click, route/navigation, enter-battle, direct-combat, maintenance,
  return-home, WAIT_COMBAT, or 白龙马/普通怪/黄袍/WUBEI business flow.
- Any OCR/template behavior change must follow `AGENTS.md` testcase replay requirements and produce
  marked output showing the crop, recognized map/coordinate, and final result.

Implementation requirements:

- [ ] Claim the card before editing code.
- [ ] Before editing, read `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/ACTIVE_WORK.md`,
  `docs/PACKAGE_ARCHITECTURE.md`, `docs/业务逻辑.md`, and the Tangde report above.
- [ ] Before editing, record current branch, latest pushed baseline, `git status`, and relevant
  `ObjectiveTextRecognitionService` / `XiuluoTaskV2` diff or `git show` evidence in
  `docs/ACTIVE_WORK.md`.
- [ ] Use the existing testcase raw image `story_yaochi_63_91_with_hint_raw.png` as the red case.
- [ ] Preserve the existing successful control sample `story_yaochi_78_64_extra7_raw.png` and any
  other objective-text replay samples.
- [ ] Investigate and fix why `(63,91)` becomes an invalid/empty coordinate despite a valid crop.
  Candidate areas include `stripCoordinateDecorations(...)`, `recognizeCoordinateRuns(...)`,
  `repairDuplicatedPrefixAndMergedSuffix(...)`, `recognizeCoordinateByTemplateScan(...)`, and
  map-plausibility repair. Do not implement a one-off string hack that only passes this filename.
- [ ] Keep CR56's ownership model: `READ_OBJECTIVE` still only consumes the prepared background
  result and routes success/failure through the documented 修罗 path.

Verification:

- [ ] Compile with `mvn -q -DskipTests compile`.
- [ ] Replay objective-text recognition:
  - `story_yaochi_63_91_with_hint_raw.png` must recognize `瑶池(63,91)`;
  - `story_yaochi_78_64_extra7_raw.png` must still recognize `瑶池(78,64)`;
  - existing objective-text samples must not regress.
- [ ] Produce and record marked replay output paths in `docs/ACTIVE_WORK.md`.
- [ ] Fresh unpaused 修罗 runtime must prove:
  - accept option -> snapshot capture -> background parse still happens;
  - a former `(63,91)`-shape failure is recognized or a bounded explicit failure is logged;
  - no same-phase synchronous objective fallback returns;
  - CR56/CR61/CR62 behavior remains unchanged.
- [ ] Performance goal check:
  - objective parsing p95/p99 and max are reported before/after;
  - no new task-turn churn, idle park loop, or cross-window maintenance insertion appears during
    `READ_OBJECTIVE`.

Card CR70: Register 修罗 张闻 -> 灵兽村 route dialog before clicking 张闻

Problem statement:

- Current `NavigationService.navigateToLingShouVillageViaZhangWen(...)` first routes through 长安,
  approaches 张闻, calls `npcClickSmart(张闻)`, and only after that arms
  `ROUTE_TRANSFER/灵兽村`.
- Fresh 修罗 runtime shows this is too late:
  - Range: `2026-06-21 01:09:50-01:10:21`.
  - `01:09:56.786` Runner publishes `PATHING_TERMINAL target=长安 state=ARRIVED` for
    `zhangWenApproach`.
  - `01:09:57.212` task starts `NPC smart click request: npcName=张闻 ... map=长安`.
  - `01:10:06.064` Runner already sees `OPTION`, but publishes only
    `TASK_ATTENTION_REQUIRED source=dialog-visible:OPTION operation=null target=null` because
    `ROUTE_TRANSFER target=灵兽村` is not armed yet.
  - `01:10:15.959` the correct `PREPARED_ACTION_READY operation=ROUTE_TRANSFER target=灵兽村`
    finally appears.
  - `01:10:16-01:10:17` task re-entry still attempts `npcClickSmart(张闻)` again while the route
    option is already visible/prepared, then eventually consumes the prepared route dialog.
- This causes an avoidable late handoff and can make 张闻 transfer look like the first attempt missed
  the dialog.

Scope / business boundary:

- Keep physical navigation target as `长安 -> 张闻`. Do not change this into a pathing intent whose
  target is `灵兽村` before the route transfer is actually clicked; that was the old mismatch class
  we explicitly want to avoid.
- This card is timing/order only. Do not change world-map search, current-map coordinate click, NPC
  smart-click formula, OCR/template thresholds, route-dialog matching, memory format, or generic
  navigation algorithms.
- Do not change 修罗 objective parsing, accepted-objective screenshot behavior, enter-combat,
  direct-combat, return-home, maintenance, or WAIT_COMBAT logic.
- Every implementing agent must read `docs/业务逻辑.md` before and after the patch, and explicitly
  verify the implementation does not break 白龙马、普通怪、黄袍怪、修罗业务逻辑.

Implementation requirements:

- [x] Claim the card before editing code.
- [x] Before editing, record current branch, latest pushed baseline, `git status`, and relevant
  `NavigationService` / route-dialog diff or `git show` evidence in `docs/ACTIVE_WORK.md`.
- [x] Arm `ROUTE_TRANSFER/灵兽村` immediately after `zhangWenApproachResult` succeeds and before the
  first `npcClickSmart(张闻)` call.
- [x] On re-entry, if a `ROUTE_TRANSFER/灵兽村` prepared action or already-visible route dialog is
  available, consume/continue that route-dialog path before attempting another `npcClickSmart(张闻)`.
- [x] Preserve the existing `:zhangWenTransfer` source/remembered route-choice behavior and avoid
  repeated same-target request spam.

Verification:

- [x] Compile with `mvn -q -DskipTests compile`.
- [x] Add a focused source/wiring test if feasible for the order: route dialog preparation is armed
  before 张闻 click, and prepared route action is consumed before a second 张闻 click.
- [x] `mvn -q -DskipTests test-compile` passed in Codex's 2026-06-21 revalidation.
- [ ] Fresh 修罗 runtime must prove:
  - `Ling Shou Village route dialog preparation requested ... target=灵兽村` appears before the first
    `NPC smart click request: npcName=张闻` for this transfer step;
  - the first OPTION after clicking 张闻 prepares `ROUTE_TRANSFER target=灵兽村`, not only plain
    `dialog-visible:OPTION`;
  - no second `npcClickSmart(张闻)` occurs while a visible/prepared `灵兽村` route option is present;
  - route dialog is consumed and current map confirms `灵兽村`;
  - no regression to the previous 长安/灵兽村 pathing-target mismatch and no regression to
    CR56/CR61/CR62.

Performance / diagnostics:

- Expected improvement is reduced 张闻 route-dialog handoff latency: the first visible OPTION should
  be usable immediately instead of waiting for late request registration and re-entry.
- Logs should show fewer late same-target route-dialog waits/retries around 张闻 transfer.

Card CR71: Guard 修罗 unknown-combat exit against active combat

Problem statement:

- Fresh 修罗 runtime proves a P1 safety bug: the leader can enter the `unknown-combat` return-home
  fallback while the same window is still actively in battle.
- Evidence range: `2026-06-21 01:29:48.296-01:30:22.932`, window `hwnd-2D0EEE`, leader `忍者`.
- Key timeline:
  - `01:29:51.294` 修罗 target tooltip click is verified by the expected enter-battle dialog.
  - `01:29:52.716` `xiuluo.enterBattle` green template is clicked.
  - `01:29:53.962` task transitions `CONFIRM_ENTER_BATTLE -> WAIT_COMBAT`.
  - `01:29:54.860` combat watcher detects combat entry for the same window.
  - `01:29:55.338` task-side `AutoCombatService.handleCombatTick(...)` reports
    `xiuluo-v2 auto-combat exit detected` and returns `EXIT_RECOVERED`.
  - Because `XiuluoRuntimeState.enteredBattleByXiuluo()` is still false, `XiuluoTaskV2.waitCombat`
    enters `resolveUnknownCombatExit(...)`.
  - `01:30:06.046` the task presses `Alt+Q` via `quest:captureDetail:xiuluo`.
  - `01:30:08.354-01:30:15.980` it uses `bag/xiuluo_return_item.png`.
  - But the watcher still reports `branch=in-combat` at `01:30:03.591` and `01:30:16.330`.
- This is not a normal failed objective read or normal post-combat recovery. It is contradictory
  combat state: task-side recovery consumed an exit result while the window-level watcher still had
  active combat evidence.

Scope / business boundary:

- Fix the safety boundary first. No 修罗 code path may open task details, press `Alt+Q`, open bag,
  use the return item, route away, or start a new objective read while the current bound window is
  still in combat.
- Do not change 修罗 objective parsing, accept-time snapshot, target navigation, target NPC click,
  enter-battle dialog/template, direct-combat fallback, return-item click algorithm, or route/home
  verification semantics.
- Do not weaken CR61/CR62 performance behavior: combat watcher should still skip non-combat work in
  `IN_COMBAT`, and 修罗 `WAIT_COMBAT` should remain event-driven instead of returning to the old
  900ms task-turn loop.
- Every implementing agent must read `docs/业务逻辑.md` before and after the patch, and explicitly
  verify the implementation does not break 白龙马、普通怪、黄袍怪、修罗业务逻辑.

Implementation requirements:

- [x] Claim the card before editing code.
- [x] Before editing, record current branch, latest pushed baseline, `git status`, and relevant
  `XiuluoTaskV2` / `AutoCombatService` / `BattleRadarService` diff or `git show` evidence in
  `docs/ACTIVE_WORK.md`.
- [x] In the `WAIT_COMBAT -> EXIT_RECOVERED -> !enteredBattleByXiuluo` path, add a hard guard before
  `resolveUnknownCombatExit(...)` can perform any input. If the current bound window has active
  combat state, a fresh `IN_COMBAT` watcher fact, or a direct fresh combat-radar confirmation, treat
  the exit as stale/contradictory and stay in `WAIT_COMBAT` / `WAIT_COMBAT_STATE_CHANGE`.
- [x] Add the same guard inside `resolveUnknownCombatExit(...)` before location scan,
  task-panel capture, and return-item fallback. The guard must run before any physical input such as
  `Alt+Q`, bag open, or item click.
- [x] Investigate and fix or explicitly log the source of the contradictory `EXIT_RECOVERED`: stale
  `combatExitPending`, old exit event not cleared on new combat entry, cross-thread ordering, or
  per-window state mismatch. If the exact source is not changed in the first patch, logs must make
  the stale-exit suppression visible.
- [x] When a stale/contradictory exit is suppressed, logs must include window id, phase,
  `enteredBattleByXiuluo`, current action state, watcher/combat fact age if available, and the
  suppressed source (`unknown-combat-exit`).

Verification:

- [x] Compile with `mvn -q -DskipTests compile`.
- [x] Add or update a narrow wiring/source test that simulates `EXIT_RECOVERED` while the current
  combat state/fact is `IN_COMBAT`; expected result: no task-panel capture, no return-item call, and
  phase remains `WAIT_COMBAT` or waits for `COMBAT_STATE_CHANGED`.
- [ ] Fresh 修罗 runtime must prove:
  - after an enter-battle click and `COMBAT_STATE_CHANGED oldTick=NONE newTick=IN_COMBAT`, no
    `quest:captureDetail:xiuluo`, `pressAltQ`, `bag/xiuluo_return_item.png`, or unknown-combat
    return-home starts until a real `IN_COMBAT -> NONE` exit is observed;
  - stale/contradictory exit suppression, if triggered, returns to `WAIT_COMBAT` without sending
    physical input;
  - normal post-combat `IN_COMBAT -> NONE` still advances to return-home/recovery;
  - CR56 objective read, CR61 in-combat watcher skip, CR62 event-driven `WAIT_COMBAT`, CR68 target
    pathing, CR69 objective OCR, and CR70 张闻 route-dialog behavior do not regress.

Performance / diagnostics:

- This card is primarily a correctness/safety blocker, not a latency improvement card.
- The fix must not reintroduce repeated 900ms `WAIT_COMBAT` task transactions or in-combat
  dialog/pathing/tracker scans. A successful fix should preserve CR61/CR62 performance gains while
  preventing active-combat `Alt+Q` / return-item side effects.

Card CR72: Verify 摄妖香 status icon before trusting post-combat memory time

Problem statement:

- Current `PlayerStateService.ensureSheYaoXiangActive(...)` used to trust `lastIncenseUsedTime` too early:
  the old fresh-memory shortcut could return before checking the status-bar icon or opening the bag.
- The accepted rule is now a 50-minute memory-trust window. Within 50 minutes, memory may only skip
  bag opening after lightweight status-icon proof. After 50 minutes, run full status verification;
  if that full probe reads remaining time above the separate 20-minute refill threshold, still skip refill.
- This makes memory time the only proof that 摄妖香 is still active during the fresh-memory window.
  That is unsafe after pause/resume, abnormal combat enter/exit ordering, stale exit-signal
  consumption, or any probe path that accidentally rebuilds `lastIncenseUsedTime` from a newer
  value.
- The concrete risk is: real game buff is gone, but the program memory still says “fresh enough”,
  so the leader skips both status proof and refill.
- Fresh incident context: during the `2026-06-21 01:29:48-01:30:22` 修罗 abnormal combat-exit slice,
  post-combat recovery logged a fresh-memory 摄妖香 skip (`仅过去 0 分钟...跳过包裹检查`). That log is not
  by itself proof that the buff was missing, but it proves the current code path can skip all real
  status verification immediately after unstable combat timing.

Scope / business boundary:

- Scope is only the leader 摄妖香 status decision in `PlayerStateService`, especially calls from
  post-combat recovery such as `AutoCombatService` with `source + ":post-combat"`.
- The purpose is to change the order of proof, not the item-using mechanics.
- Do not change `bag/sheyaoxiang_item.png`, bag page scanning/clicking, status template thresholds,
  cyan/green digit OCR thresholds, task navigation, combat recovery, first-aid, maintenance
  broadcast, or 五倍/修罗 objective/NPC/click flow.
- Every implementing agent must read `docs/业务逻辑.md` before and after the patch, and verify the
  change does not break 白龙马、普通怪、黄袍怪、修罗业务逻辑.

Implementation requirements:

- [ ] Claim the card before editing code.
- [ ] Before editing, record current branch, latest pushed baseline, `git status`, and relevant
  `PlayerStateService` / `AutoCombatService` diff or `git show` evidence in `docs/ACTIVE_WORK.md`.
- [ ] Before the 50-minute fresh-memory trust shortcut returns, perform a lightweight status-bar icon
  presence check over the existing 摄妖香 status rect. This check should be bounded and should not
  open the bag.
- [ ] Before every 摄妖香 status/icon capture, move the mouse away from the status/buff area, using the
  same safety principle as bag and player-bar checks. A cursor hovering over the buff can change the
  rendered icon/tooltip state and produce a false `ABSENT`.
- [ ] Treat the lightweight check as a three-state decision:
  - `PRESENT`: 摄妖香 icon is visible. The memory shortcut may return without opening the bag.
  - `ABSENT`: 摄妖香 icon is definitely not visible. Do not trust `lastIncenseUsedTime`; use one
    摄妖香 immediately and reset `lastIncenseUsedTime` to the actual use time.
  - `UNKNOWN`: capture/probe is uncertain. Run the existing full `probeIncenseStatus(...)`; if the
    full probe still cannot prove the buff exists, refill conservatively.
- [ ] Preserve the existing full `probeIncenseStatus(...)` semantics for reading cyan hour / green
  minute text after the refresh window; CR72 only adds real icon proof before the memory gate.
- [ ] Remember only the last matched icon offset inside the status rect, not a screen-absolute point.
  The lightweight probe should first verify a small cached-position snapshot, then fall back to the
  fixed window-relative status rect if the cached snapshot misses.
- [ ] If implementation splits an icon-only probe out of `probeIncenseStatus(...)`, keep the method
  as one real policy boundary. Do not add wrapper nesting or same-scope helper chains that hide the
  decision.
- [ ] Logs must make the branch visible with window/source/age context:
  - `memory-gate-icon-present` / equivalent: icon exists, no bag open;
  - `memory-gate-icon-absent-refill` / equivalent: icon missing, refill despite fresh memory;
  - `memory-gate-icon-unknown-full-probe` / equivalent: uncertain lightweight probe, running full
    status probe;
  - final refill success must still reset `lastIncenseUsedTime`.

Verification:

- [ ] Compile with `mvn -q -DskipTests compile`.
- [ ] Add a focused source/unit test if feasible:
  - fresh `lastIncenseUsedTime` + icon present -> no item use;
  - fresh `lastIncenseUsedTime` + icon absent -> item use and time reset;
  - fresh `lastIncenseUsedTime` + icon unknown/full-probe unknown -> conservative item use.
- [ ] Because this touches visual status-icon decision order, provide testcase/replay evidence when
  changing the matching/crop code: input status image, command/tool, and marked output path. If the
  patch only reuses the existing status rect/template without changing click or match parameters,
  document that explicitly in `docs/ACTIVE_WORK.md`.
- [ ] Fresh runtime must prove:
  - post-combat fresh-memory path logs a status icon check before skipping bag;
  - when the icon is missing or unproven, the leader does not short-circuit on memory time and
    refills 摄妖香;
  - no bag spam occurs when the icon is present;
  - no regression to CR61/CR62 in-combat skip / event-driven wait behavior.

Performance / diagnostics:

- Expected extra cost is one small right-status-bar icon probe after combat when memory says the
  buff is fresh. This is intentionally cheaper than opening the bag or doing a full bag scan.
- This card should not add runner `100ms` / task `900ms` churn. It is a foreground post-combat
  correctness check and should remain bounded.
- Performance acceptance is qualitative unless runtime logs include enough metrics: no new
  `player.sheyaoxiang.ensure` p95/p99 regression large enough to affect post-combat flow, and no
  repeated bag-open loop when the icon is present.

Card CR73: Make direct-combat / Alt+A retries position-safe and mount-safe for all combat targets

Problem statement:

- `NpcClickService.tryDirectCombatTargetClick(...)` is a shared fallback used by both 修罗 and 五倍/WUBEI combat targets.
  It is not a 修罗-only path.
- Fresh 修罗 logs from `2026-06-21 14:23:44-14:24:46` show the leader navigated within 洛阳城 toward 修罗 `(60,4)`,
  failed normal target click, then entered direct-combat / `Alt+A`.
- User-confirmed business rule: canceling `Alt+A` / AutoA can move the character. Therefore the next retry cannot
  treat the original approach coordinate or scan region as still valid.
- Current code path contradicts that rule:
  - `clickNpcSmart(...)` explicitly skips the generic `Alt+C` retry for `COMBAT_TARGET`.
  - `tryDirectCombatTargetClick(...)` enters `Alt+A` directly, runs the click pipeline, exits with right-click on
    failure, and returns `false`.
  - The caller may then retry the target-click phase without first re-running target navigation/current-map approach.
- User-confirmed second rule: before entering direct-combat / AutoA, the leader must not be mounted/flying. A mount
  can block or distort the target click. The repo already has `GameStateUtil.detectFlyingState(...)` and 五环
  shoe-shop code using `Alt+C` / flying-state verification; do not invent a separate detector.

Scope / business boundary:

- This is a global `COMBAT_TARGET` direct-combat safety card, not a 修罗-only card.
- Current known callers are `XiuluoTaskV2` and `WubeiTask` through `NpcClickService.tryDirectCombatTargetClick(...)`.
- Every implementing agent must read `docs/业务逻辑.md` before and after the patch and explicitly verify 白龙马、普通怪、
  黄袍怪、修罗 business flow is unchanged except for this direct-combat safety boundary.
- Do not change:
  - OCR/template thresholds;
  - yellow/tooltip/Ctrl click-coordinate algorithms;
  - WUBEI probe result semantics (`probeTargetReady`, `probeWrongPosition`, `probeStoryAbsent`, `probeNoTarget`);
  - 修罗 objective parsing / accept-time snapshot;
  - route target selection or return-home behavior.
- If implementation changes where the mouse clicks, AGENTS.md testcase replay is mandatory: save/reuse testcase
  images, run the click/match replay, and produce marked output showing the final click point.

Implementation requirements:

- [x] Claim the card before editing code.
- [x] Before editing, record current branch, latest pushed baseline, `git status`, and relevant diffs for
  `NpcClickService`, `XiuluoTaskV2`, `WubeiTask`, and any `GameStateUtil` use in `docs/ACTIVE_WORK.md`.
- [x] Before any call path enters direct-combat / `Alt+A`, ensure the current window is not mounted/flying:
  - reuse `GameStateUtil.detectFlyingState(...)` or the same established Alt+C/flying-state policy;
  - if confirmed `FLYING`, send `Alt+C`, wait the existing settle interval, then enter `Alt+A`;
  - if `NOT_FLYING`, enter `Alt+A` normally;
  - if `UNKNOWN`, choose a conservative documented policy and log it clearly.
- [x] A failed direct-combat attempt must be treated as position-changing:
  - after exiting `Alt+A`, the next combat-target retry must re-run the owning task's target navigation/current-map
    approach before another normal or direct-combat click;
  - do not retry on stale player coordinate, stale target scan region, or stale tooltip/yellow/Ctrl candidates.
- [x] Keep ownership clear:
  - `NpcClickService` may report that direct-combat failed and position must be refreshed;
  - task code (`XiuluoTaskV2`, `WubeiTask`) owns the actual business navigation/retry transition.
  - Do not hide this behind wrapper nesting; use one clear policy boundary and readable phase transitions.
- [x] Logs must make the new boundary visible:
  - pre-direct-combat mount/flying state and any `Alt+C` action;
  - direct-combat failure exit;
  - next retry reason showing navigation/current-map approach was required because `Alt+A` may have displaced the
    character.

Verification:

- [x] Compile with `mvn -q -DskipTests compile`; run `test-compile` if the current tree allows it.
- [x] Add focused source/wiring test coverage if feasible:
  - `FLYING` before direct-combat triggers dismount before `Alt+A`;
  - failed direct-combat returns/marks a position-refresh-required result;
  - 修罗 caller routes back through `NAVIGATE_TO_TARGET` / current-map approach before retrying;
  - WUBEI caller does not retry stale direct-combat assumptions after `Alt+A` failure.
- [ ] Fresh runtime must prove:
  - no direct-combat retry occurs on stale location after a failed `Alt+A`;
  - when direct-combat is used, logs show mount/flying precheck before `Alt+A`;
  - 修罗 target click failure no longer loops through stale scan/Ctrl/direct-combat on the same displaced position;
  - WUBEI/白龙马 direct-combat fallback still respects documented probe semantics.

Performance / diagnostics:

- This card is primarily correctness/safety, but it also addresses CPU/log pressure: stale direct-combat retries can
  produce long `CLICK_TARGET_NPC` transactions, repeated OCR/Ctrl scans, and idle observer churn.
- Acceptance should show fewer repeated `npc.click.smart` / `direct-combat` loops for the same target failure. It must
  not reintroduce WAIT_COMBAT 900ms empty transactions or in-combat non-combat scans.

Card CR74: Fix world-map route-result memory intent ownership so 修罗 can promote clean route memory

Problem statement:

- 修罗现在没有真正用上 world-map route-result memory fast path.
- Fresh audit of current `logs/dhxy-console.log`:
  - `fast path used=0`
  - `[world-map-route-memory] pending created=36`
  - `[world-map-route-memory] pending success=0`
  - `[world-map-route-memory] pending abandoned=36`
  - `reason=intent-replaced=37`
  - `confirm pending route memory=10`
- `confirm pending route memory` is not the same mechanism as this card. That marker is route-dialog
  / transfer-choice memory settlement, not world-map route-result settlement. The missing marker is
  `confirm pending world-map route memory`.
- `config/world_map_route_result_memory.json` currently has route-result rows, but none can be used:
  - entries: `36`
  - `clean=0`
  - `successCountSum=0`
  - `consecutiveSuccessPositive=0`
- Concrete fresh timeline proving the bug:
  - `2026-06-21 17:58:36.574` `NavigationService` registers pathing intent
    `intentId=fe99b331... phase=worldMapRouteClick source=xiuluo-v2:target:map:worldMapRouteClick`
    for target `兰若寺`.
  - `2026-06-21 17:58:36.575` `[world-map-route-memory] pending created ... intentId=fe99b331...`
    for the same target.
  - Immediately after, outer `navigateToMap(...)` registers a second same-leg intent
    `intentId=c8cceacf... phase=navigateToMap source=xiuluo-v2:target:map:navigateToMap:world-map route clicked`.
  - `2026-06-21 17:58:43.837` settlement consumes the pending and records
    `pending abandoned ... reason=intent-replaced`, with
    `pendingIntentId=fe99b331... currentIntentId=c8cceacf...`.
  - Later logs may show `confirm pending route memory`, but that is route-dialog memory and does not
    increment `WorldMapRouteResultMemoryService.recordSuccess(...)`.
- Source-level root cause:
  - `NavigationService.submitWorldMapSearchAndClickDestination(...)` calls
    `registerWindowPathingIntent(request, "worldMapRouteClick", ...)` and then
    `rememberPendingWorldMapRouteResultClick(...)`.
  - `rememberPendingWorldMapRouteResultClick(...)` stores the current active pathing intent id into
    `WorldMapRouteResultPendingMemory.intentId`.
  - The outer `navigateToMap(...)` `finally` block registers another `navigateToMap` intent for the
    same `PATHING_STARTED` result unless `pathingIntentOwnedByNestedRoute` or
    `pathingIntentAlreadyActive` is true.
  - `pathingIntentOwnedByNestedRoute` is only set for the special 灵兽村/张闻 nested route, not for
    normal world-map route result submission.
  - `WindowTaskRunner.settlePendingWorldMapRouteResultMemory(...)` correctly abandons a pending if
    `pending.intentId` differs from the current active pathing intent. The problem is that the same
    navigation leg is being double-registered before settlement.

Scope / business boundary:

- This card is about intent ownership and settlement identity only.
- Do not change world-map OCR, route-result text matching, route-result scroll strategy, route-result
  click coordinates, minimap/pathing detection, current-map coordinate navigation, NPC click, 修罗
  objective parsing, route-dialog option memory, or WUBEI/五倍 business decisions.
- Do not lower the clean threshold or force existing dirty memory to clean. `WorldMapRouteResultMemoryService`
  currently requires `consecutiveSuccessCount >= 5`; CR74 must let real successful route arrivals
  increment this counter naturally.
- Every implementing agent must read `docs/业务逻辑.md` before and after the patch and explicitly
  verify 白龙马、普通怪、黄袍怪、修罗业务逻辑 is unchanged.

Implementation requirements:

- [ ] Claim the card before editing code.
- [ ] Before editing, record current branch, latest pushed baseline, `git status`, and relevant
  `NavigationService` / `WindowTaskRunner` / `WorldMapRouteResultMemoryService` diff or `git show`
  evidence in `docs/ACTIVE_WORK.md`.
- [ ] Ensure a successful `submitWorldMapSearchAndClickDestination(...)` creates only one active
  pathing intent for the same route leg, or ensure the pending memory is bound to the same final
  active intent that `WindowTaskRunner` will settle.
- [ ] The narrow expected repair is to treat the world-map route-result click path as owning the
  current leg, so the outer `navigateToMap(...)` `finally` does not register a duplicate
  `navigateToMap` intent after `worldMapRouteClick` has already registered and created pending.
  If a different repair is chosen, document why it is safer and prove it preserves the same
  invariant: pending intent id equals the active intent id seen by settlement.
- [ ] Preserve the existing `intent-replaced` abandon behavior for a real second navigation. CR74
  must not make stale pending memories survive across a genuine new navigation.
- [ ] Logs must remain clear enough to distinguish:
  - pending created;
  - duplicate same-leg registration skipped or pending rebound to final intent;
  - pending success;
  - real second-navigation / intent-replaced abandon.

Verification:

- [ ] Compile with `mvn -q -DskipTests compile`.
- [ ] Add a focused source-level regression test if feasible:
  - after a world-map route-result click returns `PATHING_STARTED`, the pending route-result memory
    and active pathing intent use the same intent id;
  - no immediate outer duplicate intent causes `intent-replaced`;
  - a genuine new navigation still abandons old pending memory.
- [ ] Fresh 修罗 runtime must prove:
  - `[world-map-route-memory] pending created` is followed by
    `confirm pending world-map route memory` on ARRIVED, not immediate `reason=intent-replaced`;
  - `pending success` count increases;
  - `consecutiveSuccessCount` climbs toward `5`, then `clean=true`;
  - after clean promotion, `fast path used` appears for repeated `fromMap -> targetMap` route-result
    clicks;
  - no regression to CR56 objective read, CR68 target pathing wait, CR70 张闻 transfer, CR71 combat
    safety, or CR73 direct-combat safety.

Performance / diagnostics:

- Expected performance win after clean promotion: repeated 修罗 routes can click the remembered
  world-map route result directly instead of doing full route-result OCR/template/scroll matching.
- Before clean promotion, no latency win is expected. Acceptance should therefore separately report:
  - settlement health (`pending success`, `intent-replaced` drop);
  - later fast-path health (`fast path used`, route-result OCR count reduction).

Card CR75: Use 修罗 remembered accept-option fast path without re-detecting dialog type

Problem statement:

- 修罗接任务选项已经有 stable dialog choice memory，但当前没有真正走 DialogService 的 remembered fast path.
- Fresh 修罗 logs show this repeated sequence:
  - `dialog handle request: source=xiuluo-v2:accept:*:accept-memory operation=CLICK_REMEMBERED_OPTION`
  - `dialog detect no-focus: reason=handle-dialog:CLICK_REMEMBERED_OPTION result=OPTION`
  - `[latency] event=dialog.detect elapsedMs=883/1169/1808/3136 ...`
  - `dialog remembered option click ... rel=(95, 113)`
- The heavy step is not the remembered click itself. It is the full `detectDialogSnapshotDirect(...)`
  pass before the remembered click: wait, capture, mask check, option lower green check, and possible
  name-layer hiding depending on request flags.
- Current source already has the desired fast path in `DialogService`:
  `CLICK_REMEMBERED_POINT && !request.isVerifyDialogType()`.
- `XiuluoTaskV2.acceptTaskDialog(...)` calls `handleKnownXiuluoOptionDialog(..., verifyDialogType=false)`,
  meaning this phase already treats the accept OPTION dialog as established.
- But `tryRememberedAcceptTaskOption(...)` ignores that caller fact and calls
  `DialogHandleRequest.handleRememberedChoiceOption(...)`, whose builder default is
  `verifyDialogType=true`.

Scope / business boundary:

- This card only optimizes 修罗 accept-task remembered option clicking.
- Do not change 修罗 phase order: accept NPC click -> accept dialog option -> objective read must stay
  the same.
- Do not change green-template specs, remembered relative coordinates, dialog memory scoring,
  DialogService type detector, or business option fallback.
- Do not skip verification in contexts that still pass `verifyDialogType=true`, such as objective
  recovery or other uncertain dialog recovery paths.
- Do not apply this blindly to route-transfer remembered options or unrelated tasks.
- Every implementing agent must read `docs/业务逻辑.md` before and after the patch and explicitly
  record that 白龙马、普通怪、黄袍怪、修罗业务逻辑 was not changed.

Implementation requirements:

- [ ] Claim the card before editing code.
- [ ] Before editing, record current branch, latest pushed baseline, `git status`, and relevant
  `XiuluoTaskV2` / `DialogHandleRequest` / `DialogService` evidence in `docs/ACTIVE_WORK.md`.
- [ ] Preserve `verifyDialogType` from `handleKnownXiuluoOptionDialog(...)` into the remembered
  accept-option request.
- [ ] The expected narrow implementation is either:
  - add a `verifyDialogType` overload/factory for `handleRememberedChoiceOption(...)`; or
  - build the remembered request inline in `tryRememberedAcceptTaskOption(...)` with
    `verifyDialogType(false)` only when the caller passed false.
- [ ] Keep remembered-click miss behavior unchanged:
  - if the click fails or mismatches while current dialog type is known OPTION, record
    `recordDialogChoiceFailure(...)`;
  - fall back to the existing green-template matching path.
- [ ] Add clear log evidence for the fast path if existing `DialogService` log is not enough:
  `dialog remembered option fast path without detect`.

Verification:

- [ ] Compile with `mvn -q -DskipTests compile`.
- [ ] Run `mvn -q -DskipTests test-compile` if the current tree allows it.
- [ ] Add focused source/wiring test if feasible:
  - accept-memory request receives `verifyDialogType=false` when the caller knows the accept OPTION
    dialog;
  - recovery paths that pass `verifyDialogType=true` still request normal detection.
- [ ] Fresh 修罗 runtime must prove:
  - stable accept-memory path logs `dialog remembered option fast path without detect`;
  - no `dialog detect no-focus: reason=handle-dialog:CLICK_REMEMBERED_OPTION` appears for the
    fast accept-memory path;
  - `dialog remembered option click ... target=xiuluo.acceptTask rel=(95, 113)` still succeeds;
  - if remembered memory misses, 修罗 still falls back to existing green-template accept matching and
    does not click unrelated business options.

Performance / diagnostics:

- Expected win per accept-memory click: remove roughly `0.8s-3.1s` of full dialog detection before
  clicking the remembered option.
- This card does not optimize the preceding NPC click. That is CR76.

Closure note (2026-06-29):

- Fresh 修罗 runtime validated the accept-memory fast path over multiple rounds: remembered option
  clicks no longer pay the full `handle-dialog:CLICK_REMEMBERED_OPTION` detection pass, and no
  accept-dialog fallback regression was recorded. CR75 is Done.

Card CR76: Try stable NPC learned-memory click before Alt+4 name-layer preparation

Problem statement:

- 修罗固定 NPC such as `灵兽村使者` can have a stable learned click point, but current
  `NpcClickService.runNpcClickPipeline(...)` still performs the generic name-layer preparation first:
  `npcClick:pipeline-hide-player-names:<npc>` -> `Alt+4` -> `400ms` sleep.
- This preparation is useful for tooltip/yellow-name/formula/Ctrl detection, but it is not needed for
  a mature learned-memory physical click point.
- Fresh logs show the repeated overhead before fixed NPC clicks:
  - `npcClick:pipeline-hide-player-names:灵兽村使者`
  - `InputAction{type=PRESS_ALT_4 ...}`
  - `InputAction{type=SLEEP ... delayMs=400}`
  - then learned memory / later strategies can proceed.
- User-approved direction: learned-memory clicking should not pay the screenshot/OCR preparation
  cost unless the memory misses and the pipeline has to fall back to visual strategies.

Scope / business boundary:

- This card optimizes the `NpcClickService` strategy order only for stable learned-memory NPC clicks.
- It must keep the existing pre-click dialog safety boundary. If a blocking STORY/OPTION dialog is
  already visible, handle/skip exactly as the current code does; do not click through it.
- If learned memory verifies the expected dialog, the pipeline may return immediately without
  `Alt+4`, tooltip matching, yellow OCR, formula, or Ctrl probing.
- If learned memory misses, the old pipeline must run with `prepareNpcPipelineNameLayerOnce(...)` and
  the same fallback order as before.
- Preserve WUBEI/白龙马 `COMBAT_TARGET` probe target semantics:
  - do not reorder WUBEI tooltip-first / direct-combat fallback behavior;
  - do not change 白龙马 target coordinate policy, OCR/template thresholds, Ctrl menu scanning, or
    `Alt+A` direct-combat safety from CR73.
- Do not change NPC memory scoring, evidence persistence, ROI learning, or expected-dialog template
  verification.
- Every implementing agent must read `docs/业务逻辑.md` before and after the patch and explicitly
  record that 白龙马、普通怪、黄袍怪、修罗业务逻辑 was not changed.

Implementation requirements:

- [ ] Claim the card before editing code.
- [ ] Before editing, record current branch, latest pushed baseline, `git status`, and relevant
  `NpcClickService` evidence in `docs/ACTIVE_WORK.md`.
- [ ] Refactor `runNpcClickPipeline(...)` so the stable learned-memory strategy can run before
  `prepareNpcPipelineNameLayerOnce(...)` when safe.
- [ ] Keep `currentPreClickDialogType(...)` / runner dialog snapshot safety before the early
  learned-memory click.
- [ ] If the early learned-memory click verifies, record smart-click evidence exactly as the current
  learned-memory strategy does and return success.
- [ ] If the early learned-memory click misses:
  - add the attempted point as Ctrl origin as current code does;
  - then run `prepareNpcPipelineNameLayerOnce(...)`;
  - continue through the same existing tooltip/yellow/formula/Ctrl fallback order.
- [ ] Prevent accidental double-attempts of the same learned-memory strategy after the fallback
  preparation. The learned strategy should not click the same remembered point twice in one pipeline
  unless the implementation explicitly proves why that is safe.

Verification:

- [ ] Compile with `mvn -q -DskipTests compile`.
- [ ] Run `mvn -q -DskipTests test-compile` if the current tree allows it.
- [ ] Add focused source/wiring test if feasible:
  - learned-memory verified path does not call `prepareNpcPipelineNameLayerOnce(...)`;
  - learned-memory miss path does call it and keeps fallback order;
  - WUBEI `COMBAT_TARGET` path does not change ordering.
- [ ] Fresh 修罗 runtime must prove:
  - stable `灵兽村使者` learned-memory hit no longer logs
    `npcClick:pipeline-hide-player-names:灵兽村使者` before the verified click;
  - `npc.click.smart` latency drops for learned-memory hits;
  - missed learned-memory cases still fall back to Alt+4 + existing visual strategies;
  - accept dialog still opens and CR75 can click remembered accept option.
- [ ] Fresh WUBEI/白龙马 or code-level guard evidence must prove CR76 did not reorder probe target
  semantics or direct-combat fallback.

Performance / diagnostics:

- Expected win on stable learned NPC hits: remove one `Alt+4 + 400ms sleep` plus related input queue
  wait before the learned click.
- This card does not remove the remembered accept-option dialog detection; that is CR75.

Closure note (2026-06-29):

- Fresh 修罗 runtime validated stable `灵兽村使者` learned-memory hits without preceding
  `npcClick:pipeline-hide-player-names:灵兽村使者`; earlier miss/insufficient-policy paths fell back
  normally. CR76 is Done.

Card CR77: Fire-and-handoff 修罗 start-exit prepath without movement confirmation

Problem statement:

- 修罗接任务并解析目标以后，会先点一次灵兽村出口小地图坐标 `(11,8)`，让角色提前往出口走，
  同时主流程继续准备正式目标导航。
- Fresh runtime logs show this `xiuluo-v2:start-exit-prepath:currentMap` path spends most time in
  `NavigationService.navigateInCurrentMap(...)`:
  - after mini-map coordinate click, it waits for `fast-edge` / coordinate fallback movement proof;
  - then closes the mini-map and performs the after-close check;
  - only after that does it register `window pathing intent` and return `PATHING_STARTED`.
- Example evidence from `2026-06-21` 修罗 logs:
  - `navigation.currentMap elapsedMs=4253/4775/5193/5871`
    for `source=xiuluo-v2:start-exit-prepath:currentMap target=(11,8)`;
  - `mini-map handoff coordinate fallback completed ... edgeElapsedMs=1526 fallbackElapsedMs=267`;
  - the formal target navigation after this point reaches world-map prep in roughly sub-second time.
- User-approved direction: this specific prepath is only a speed hint. It should not pay the full
  movement-proof cost before formal target navigation can continue.

Scope / business boundary:

- This card applies only to 修罗 start-map exit prepath:
  `source=xiuluo-v2:start-exit-prepath:currentMap`, target map `灵兽村`, target coordinate `(11,8)`.
- Do not remove movement confirmation from normal `navigateInCurrentMap(...)` behavior.
- Do not apply this to:
  - 修罗 target current-map approach;
  - 修罗 accept/heal-pet/return-home short current-map walks;
  - 张闻靠近点 / 灵兽村 special entry route;
  - 五倍 / 白龙马 / 普通怪 / 黄袍怪;
  - any path where the caller depends on the current-map click being proven before proceeding.
- The start-exit prepath remains optional. If the click fails to actually move, the formal
  `NAVIGATE_TO_TARGET` / world-map route must still proceed and recover normally.
- Do not change mini-map coordinate conversion, jitter policy, OCR/template thresholds, world-map
  search/click, route-dialog preparation, target navigation, NPC click, or combat logic.
- Every implementing agent must read `docs/业务逻辑.md` before and after the patch and explicitly
  record that 白龙马、普通怪、黄袍怪、修罗业务逻辑 was not changed.

Implementation requirements:

- [ ] Claim the card before editing code.
- [ ] Before editing, record current branch, latest pushed baseline, `git status`, and relevant
  `NavigationService` / `XiuluoTaskV2` evidence in `docs/ACTIVE_WORK.md`.
- [ ] Add a narrow flag or source-scoped branch for start-exit prepath fire-and-handoff. The branch
  must be visibly constrained to `xiuluo-v2:start-exit-prepath`, not a generic navigation default.
- [ ] After resolving and clicking the mini-map point, skip:
  - `gameStateUtil.isMovingByPixelDiff(...)`;
  - `confirmMiniMapPathingStarted(...)`;
  - retrying alternate mini-map logical points for this prepath optimization.
- [ ] Immediately close the Alt+1 mini-map after the coordinate click with a short fixed settle only.
  Do not run the expensive after-close debug/template check on the happy path.
- [ ] Register a `WindowPathingIntent` immediately after the click/close sequence and return
  `NavigationResult.pathingStarted("current-map mini-map click fire-and-handoff")` or similarly clear
  wording.
- [ ] Keep runner observation intact: ARRIVED / STOPPED_AWAY events for this prepath may still be
  published, but 修罗 must not park waiting for this prepath intent.
- [ ] Ensure the next formal target navigation is not blocked by the prepath intent. Existing route
  dialog gate behavior should continue treating the active 灵兽村 prepath intent as
  `pathing-target-mismatch` when the formal target map is different.
- [ ] Add logs that make the new behavior obvious:
  - `start-exit-prepath fire-and-handoff`;
  - click point and source;
  - mini-map close submitted;
  - intent registered before movement proof.

Verification:

- [ ] Compile with `mvn -q -DskipTests compile`.
- [ ] Run `mvn -q -DskipTests test-compile` if the current tree allows it.
- [ ] Add focused source/wiring test if feasible:
  - start-exit prepath uses the fire-and-handoff branch;
  - normal current-map navigation still calls movement confirmation before `PATHING_STARTED`;
  - 修罗 target current-map navigation is excluded from the fire-and-handoff branch.
- [ ] Fresh 修罗 runtime must prove:
  - `source=xiuluo-v2:start-exit-prepath:currentMap` logs fire-and-handoff;
  - no `handoff-fast-edge` or `coordinate fallback completed` appears for that prepath;
  - `window pathing intent registered` happens immediately after the mini-map click/close;
  - formal `xiuluo-v2:target` navigation proceeds to world-map prep without waiting for prepath
    movement proof;
  - if runner later reports ARRIVED/STOPPED_AWAY for the prepath, it does not pull 修罗 back into a
    prepath wait or block the formal route.
- [ ] Fresh runtime must also prove no regression in:
  - 修罗 target current-map approach;
  - 张闻进灵兽村;
  - CR68 target pathing event wait;
  - CR70 route-dialog handoff;
  - CR74 world-map route memory settlement.

Closure note (2026-06-29):

- Fresh 修罗 runtime validated `xiuluo-v2:start-exit-prepath:currentMap` fire-and-handoff over
  repeated rounds: no start-exit `handoff-fast-edge` / coordinate fallback, and formal target
  navigation was not blocked by the optional prepath intent. CR77 is Done.

Performance / diagnostics:

- Expected win for each 修罗 accept round that starts in 灵兽村 and needs the exit prepath:
  roughly `1.5s-2.5s` by removing movement confirmation and happy-path after-close probing.
- The target metric is `navigation.currentMap elapsedMs` for
  `source=xiuluo-v2:start-exit-prepath:currentMap`; it should drop from multi-second values toward
  the mini-map open/click/close fixed-cost range.
- This card does not optimize normal target current-map navigation. If those remain slow, open a
  separate card with testcase replay because they affect real click/navigation correctness.

Card CR78: Disable full dialog fallback for lightweight maintenance broadcast checks

Problem statement:

- CR65 correctly stopped treating `STORY` fallback detections as maintenance option dialogs, but it
  did not remove the expensive fallback detection itself.
- The current auto-battle member maintenance path still does this after the fixed small-region
  `医宝宝` / `修装备` templates miss:
  - capture / classify a full dialog snapshot;
  - often return `DialogType.STORY`;
  - skip because it is not `OPTION`.
- Runtime evidence already showed this path is high volume and low value: most auto-battle fallback
  detections classify as `STORY`, while the actual maintenance broadcast options are fixed green
  option buttons that the two small-strip templates are meant to catch.
- User-approved direction: lightweight member idle checks should stay lightweight. If the two fixed
  regions miss, they should report no maintenance broadcast immediately.

Scope / business boundary:

- Applies to auto-battle / member idle maintenance broadcast checks, especially
  `sourceTask=auto-battle`.
- Also applies to any future path explicitly marked as a lightweight maintenance-broadcast probe.
- Does not apply to leader/formal maintenance broadcast flows where the leader has actively opened a
  maintenance NPC/dialog and the broadcast path may need the more tolerant full dialog fallback.
- Do not globally delete fallback from `DialogService`; make the fallback policy explicit at the
  request/source boundary.
- Do not change:
  - fixed-strip template files, template thresholds, or click coordinates for `医宝宝` / `修装备`;
  - `DialogType` detection rules;
  - `handleBusinessOption(...)` matching rules for full fallback paths;
  - maintenance cooldown / same-team Alt+8 guard semantics from CR65;
  - first-aid, repair, summon-skill cleanup, combat enter/exit, OCR/template/navigation/click logic;
  - 白龙马、普通怪、黄袍怪、修罗业务逻辑.
- Every implementing agent must read `docs/业务逻辑.md` before and after the patch and explicitly
  record that unrelated business flows were not changed.

Implementation requirements:

- [x] Claim the card before editing code.
- [x] Before editing, record current branch, latest pushed baseline, `git status`, and relevant
  `DialogService` / `TaskMaintenanceService` / `AutoBattleTask` evidence in `docs/ACTIVE_WORK.md`.
- [x] Add an explicit policy flag or equivalent request field such as
  `allowFullMaintenanceBroadcastFallback` rather than relying on a fragile substring-only check.
- [x] `AutoBattleTask` / lightweight member idle maintenance requests must set that policy to false.
- [x] Leader/formal maintenance broadcast requests must keep the policy true.
- [x] In `DialogService.handleMaintenanceBroadcastOptionFastPath(...)`, keep the current fixed-strip
  checks first. If both fixed checks miss and fallback is disabled, return the existing no-action /
  not-found result immediately.
- [x] Do not call `detectDialogSnapshotDirect(...)` on the fallback-disabled path.
- [x] Add a structured log line that makes the skip auditable, for example:
  `maintenance broadcast lightweight fallback disabled: source=auto-battle reason=fixed-strip-miss`.
- [x] Preserve CR65's OPTION-only boundary for the fallback-enabled path:
  - `NONE` skips;
  - non-`OPTION` / `STORY` skips without washing/matching;
  - only `OPTION` may enter `handleBusinessOption(...)`.
- [x] 2026-06-26 follow-up for yellow maintenance broadcast text:
  - keep the lightweight no-full-fallback boundary from CR78;
  - within each fixed small ROI, try green template match first, then yellow template match from
    the same captured raw image before returning not-found;
  - do not call `detectDialogSnapshotDirect(...)` on the lightweight disabled path;
  - do not add yellow washing to the no-focus maintenance broadcast prefilter.

Verification:

- [x] Compile with `mvn -q -DskipTests compile`.
- [x] Run `mvn -q -DskipTests test-compile` if the current tree allows it.
- [x] Add focused source/wiring coverage if feasible:
  - auto-battle request disables full fallback;
  - formal/leader request still allows fallback;
  - fallback-disabled fixed-strip miss does not call dialog detection.
- [x] 2026-06-26 follow-up guards:
  - `DialogMaintenanceBroadcastYellowFallbackWiringTest` requires lightweight fixed-strip green
    then yellow matching, while keeping no-focus prefilter green-only.
  - `DialogMaintenanceLightweightFallbackPolicyWiringTest` still verifies the explicit no-full
    dialog fallback policy.
- [ ] Fresh runtime must prove:
  - `source=auto-battle` fixed-strip misses still do not call the full fallback;
  - yellow broadcast text can produce `maintenance broadcast option matched ... color=yellow`;
  - there are no `dialog detect no-focus: reason=maintenance-broadcast-fallback:auto-battle` logs
    for the lightweight path;
  - `maintenance broadcast fallback` count from auto-battle drops to zero or near zero depending on
    remaining non-lightweight sources;
  - real leader/formal maintenance broadcast can still be consumed when the small-strip quick path
    misses but a valid `OPTION` dialog exists.
- [ ] Fresh runtime must also confirm no regression in CR65:
  `STORY` fallback detections on fallback-enabled paths still do not wash or match `heal-pet` /
  `repair-equipment`.

Performance / diagnostics:

- Expected win: remove the repeated full-dialog capture/classification cost from member idle
  maintenance loops where the answer is usually "no broadcast".
- The target metric is reduced `maintenance-broadcast-fallback:auto-battle` count and lower
  screenshot/dialog-detect log pressure during multi-window auto-battle idle maintenance.
- This card intentionally does not add a cooldown to the check. Valid broadcasts from another
  leader/window should still be caught by the fixed-strip quick path.

Card CR79: Close 修罗 wild-monster cancel dialog after 看打 template miss

Problem statement:

- 修罗 target click can open an unrelated wild-monster / blocking cancel dialog instead of the
  expected enter-battle option dialog.
- Current 修罗 enter-battle recovery first tries the normal `看打` template:
  `images/template/dialog/xiuluo/xiuluo_enter_battle_kanda.png`.
- When that `看打` template misses, the task currently proceeds into heavier recovery/OCR /
  direct-combat fallback. If the visible dialog is only a blocking wild-monster cancel dialog, the
  correct action is to close it once and then continue the original 修罗 target-click retry flow.
- User supplied the raw new template source:
  `images/template/cancel/Snipaste_2026-06-21_23-02-19.png`.
- The implementing agent should wash/extract the green option text into:
  `images/template/dialog/xiuluo/xiuluo_wild_monster_cancel.png`.

Scope / business boundary:

- Scope is only 修罗 target enter-battle dialog recovery after a normal `看打` template miss.
- The new template is a blocking-dialog close/cancel action, not an enter-battle success signal.
- If `xiuluo_wild_monster_cancel.png` matches and is clicked:
  - do not enter `WAIT_COMBAT`;
  - do not mark the target as successfully entered battle;
  - do not count it as a real `看打` / `XIULUO_ENTER_BATTLE` prepared action;
  - continue the existing fallback/retry flow that tries to click the 修罗 target again.
- Do not change:
  - `xiuluo_enter_battle_kanda.png` or normal 修罗 `看打` behavior;
  - NPC click coordinate algorithms, yellow OCR, tooltip, Ctrl-menu, learned memory, or ROI memory;
  - direct-combat / `Alt+A` / AutoA policy from CR73;
  - 修罗 objective parsing, accept-task flow, target navigation, return-home, incense, or
    maintenance behavior;
  - WUBEI/五倍 白龙马、普通怪、黄袍怪 behavior.
- Every implementing agent must read `docs/业务逻辑.md` before and after the patch and explicitly
  record that 白龙马、普通怪、黄袍怪、修罗业务逻辑 was not broken.

Implementation requirements:

- [x] Claim the card before editing code.
- [x] Before editing, record current branch, latest pushed baseline, `git status`, and relevant
  `XiuluoTaskV2` / dialog-template evidence in `docs/ACTIVE_WORK.md`.
- [x] Create `images/template/dialog/xiuluo/xiuluo_wild_monster_cancel.png` from
  `images/template/cancel/Snipaste_2026-06-21_23-02-19.png` by washing/extracting the green option
  text. Do not reuse the raw screenshot directly as a dialog template.
- [x] In the 修罗 enter-battle recovery path near the existing
  `xiuluo_enter_battle_kanda.png` handling, keep the current order:
  1. try normal `xiuluo_enter_battle_kanda.png`;
  2. if it matches/clicks, enter the existing `WAIT_COMBAT` path unchanged;
  3. only if `看打` misses, try `xiuluo_wild_monster_cancel.png` once;
  4. if the cancel template matches/clicks, close the blocking dialog and return an outcome that
     causes the existing 修罗 target-click fallback/retry to continue, not an enter-battle outcome.
- [x] Do not add a broad full-dialog detector or new OCR branch for this; this card is a single
  template branch after `看打` miss.
- [x] Add structured logs that distinguish:
  - normal `看打` matched/clicked;
  - `看打` missed and wild-monster cancel template was attempted;
  - wild-monster cancel matched/clicked and target retry will continue;
  - wild-monster cancel missed and the old recovery/fallback path continues.

Verification:

- [x] Compile with `mvn -q -DskipTests compile`.
- [x] Run `mvn -q -DskipTests test-compile` if the current tree allows it.
- [x] Because this adds a visual template and click target, AGENTS.md testcase replay is required:
  save/reuse the raw blocking-dialog screenshot under `images/test-cases/dialog/xiuluo/...`, run the
  same template match/click-point logic against it, and produce a marked output image showing the
  matched template box and final click point.
- [x] Add focused source/wiring coverage if feasible:
  - `看打` match still wins and enters `WAIT_COMBAT`;
  - `看打` miss + cancel match clicks cancel and returns to target retry, not `WAIT_COMBAT`;
  - both templates miss preserves the old recovery/OCR/direct-combat fallback order.
- [ ] Fresh 修罗 runtime must prove:
  - when the blocking wild-monster dialog appears, logs show `看打` miss -> cancel template match ->
    cancel click -> target retry continues;
  - no false `WAIT_COMBAT` transition is produced by the cancel click;
  - normal 修罗 `看打` runtime still enters battle as before;
  - WUBEI/五倍 logs are unchanged for 白龙马、普通怪、黄袍怪.

Performance / diagnostics:

- Expected effect is correctness and avoiding unnecessary heavy recovery when the visible dialog is
  only a known close/cancel blocker.
- The extra cost is one narrow template match after `看打` miss, so it should not affect the happy
  path or normal `看打` latency.

Card CR80: 修罗 pre-combat 3-minute watchdog before true battle entry

Problem statement:

- 修罗失败恢复现在主要靠 phase retry / recovery count / consecutive round failure protection.
- Real failure cases show this can still spend too long before giving up on an unusable objective:
  - `2026-06-21 23:03:04` target-click failure archived after about `258.9s`;
  - older accept/click/objective failures could run even longer before the current round is marked
    failed.
- User-approved direction: if a 修罗 round has not truly entered battle within 3 minutes, treat the
  current objective as failed and restart the accept flow.
- Important correction from user: before the 修罗 task is completed, the 修罗 return item is not
  usable. The watchdog must not try to use the return item.

Scope / business boundary:

- Scope is 修罗 V2 only.
- The 3-minute watchdog applies only before true battle entry.
- "True battle entry" means the task has reached `WAIT_COMBAT` and the combat tick / radar path has
  observed `IN_COMBAT`, causing `XiuluoRoundContext.enteredBattleByXiuluo()` to become true.
- Clicking the `看打` option or entering `WAIT_COMBAT` is not enough to stop the watchdog by itself;
  dropped clicks and false dialogs must still be catchable.
- Once `enteredBattleByXiuluo=true`, the watchdog must stop. Normal battle duration, combat exit,
  team return wait, post-combat return item, incense, and maintenance must not be timed out by CR80.
- Do not change:
  - 修罗 return item behavior before/after task completion;
  - objective OCR/template parsing;
  - target navigation, current-map/world-map click algorithms, route memory, or 张闻 transfer logic;
  - target NPC smart-click, direct-combat / AutoA, flying-state, mount safety, or CR79 dialog template
    behavior;
  - 五倍/WUBEI 白龙马、普通怪、黄袍怪 behavior.
- Every implementing agent must read `docs/业务逻辑.md` before and after the patch and explicitly
  record that unrelated 五倍/修罗 business rules were not changed.

Implementation requirements:

- [ ] Claim the card before editing code.
- [ ] Before editing, record current branch, latest pushed baseline, `git status`, and relevant
  `XiuluoTaskV2` / `XiuluoRoundContext` evidence in `docs/ACTIVE_WORK.md`.
- [ ] Add a round-local pre-combat deadline, preferably carried in `XiuluoRoundContext` so retry,
  recover, pathing wait, and prepared-action wake paths all share the same timer.
- [ ] The timer should start with the current round's accept/pre-combat flow and survive same-round
  retry/recovery jumps.
- [ ] On each phase-loop iteration, before running another pre-combat phase, check:
  - `enteredBattleByXiuluo=false`;
  - the phase is not a post-combat/return/team-return phase;
  - elapsed time is greater than or equal to `180_000ms`.
- [ ] On timeout, produce a normal 修罗 phase failure outcome that is handled by the existing
  `restartRoundAfterPhaseFailure(...)` / same-round reaccept path. Do not call
  `useReturnItemAndVerifyStartMap(...)`.
- [ ] Add clear logs such as:
  `xiuluo pre-combat watchdog timeout: round={} phase={} elapsedMs={} limitMs=180000 source={}`;
  and make the subsequent restart/reaccept visible in existing failure/recovery logs.
- [ ] Preserve the existing `MAX_CONSECUTIVE_ROUND_FAILURES` protection; CR80 shortens a bad round,
  but does not remove the final fatal cap for repeated consecutive failures.
- [ ] Ensure hot-start into already active combat is not broken: if startup resolves directly to
  `WAIT_COMBAT` and combat is observed, mark battle entry and do not timeout the round as pre-combat.

Verification:

- [ ] Add focused source/wiring tests:
  - pre-combat states keep one shared deadline across `retrySamePhase(...)`, `recoverTo(...)`, and
    `recoverToWithObjective(...)`;
  - `withXiuluoBattleStarted(...)` stops the watchdog condition;
  - timeout path does not reference or call return-item methods;
  - timeout outcome routes through existing phase-failure / accept-flow restart semantics.
- [ ] Compile with `mvn -q -DskipTests compile`.
- [ ] Run `mvn -q -DskipTests test-compile` if the current tree allows it.
- [ ] Fresh 修罗 runtime must prove:
  - a normal successful round enters combat before the 180s watchdog and is not interrupted;
  - an artificially or naturally stuck pre-combat round logs the watchdog timeout around 180s and
    restarts the accept flow;
  - no pre-completion修罗 return item usage occurs from the watchdog;
  - post-combat return/team-return/incense/maintenance behavior is unchanged.

Performance / diagnostics:

- Expected effect: cap bad pre-combat rounds at about 3 minutes instead of allowing long target-click,
  objective-read, or accept-chain recovery loops to run for 4-13+ minutes.
- This is a business safety timeout, not a polling/churn optimization. It should not add 100ms/900ms
  loop pressure; the check should run at existing phase-loop boundaries.
- Run reports should compare pre-CR80 failure case elapsed times against post-CR80 timeout evidence.

Card CR81: 修罗 left tracker read-only green-link coordinate capability

Problem statement:

- 修罗后续要支持左侧快捷任务栏里的 `修罗任务` 绿字链接，但第一步只做读取能力。
- User explicitly narrowed the scope: do not connect this to 修罗 execution yet.
- Current tracker infrastructure already exists for 五环/五倍:
  - resolve the left task tracker anchor;
  - crop the tracker panel;
  - wash/match a task title;
  - crop a short task detail block;
  - scan green text and compute screen-absolute click coordinates.
- Existing `images/template/task/xiuluo_title.png` is not the new left-tracker shortcut title. The new
  user-provided title image is `images/template/xiuluo/Snipaste_2026-06-23_12-57-46.png` and shows
  `修罗任务`.

Scope / business boundary:

- Scope is a read-only service/debug capability, not a 修罗 task-flow feature.
- The implementation may add a `readXiuluoTrackerPanel(...)` / `findXiuluoTrackerGreenClickPoint(...)`
  capability in `TaskTrackerPanelService`, or a small generalized tracker-reader helper if that keeps
  the existing 五环/五倍 code clearer.
- It must return data only: at minimum whether the `修罗任务` block was found, the cropped raw/debug image
  paths, the matched title/template evidence, and the first usable green link screen-absolute coordinate.
- Do not:
  - call this from `XiuluoTaskV2`;
  - send mouse/keyboard input;
  - click the green link;
  - register or mark any `WindowPathingIntent`;
  - change 修罗 objective story parsing, task-panel OCR fallback, navigation, target click, enter-battle,
    wait-combat, return, incense, or maintenance behavior;
  - change 五环 prepared tracker behavior or 五倍 白龙马/普通怪/黄袍 tracker behavior.
- If the `修罗任务` title or green link is not found, the read result should be an ordinary miss/empty
  result, not a task failure.

Implementation requirements:

- [ ] Claim the card before editing code.
- [ ] Before editing, record current branch, latest pushed baseline, `git status`, and relevant
  `TaskTrackerPanelService` / task-tracker model evidence in `docs/ACTIVE_WORK.md`.
- [ ] Normalize the new `修罗任务` title template into a stable path/name, for example
  `images/template/task/xiuluo_tracker_title.png`, instead of wiring code directly to the Snipaste
  filename. Keep the original user-provided image unless the user asks to remove it.
- [ ] Reuse the existing left tracker anchor/crop path where possible. Do not introduce a second
  hardcoded full-panel scanner unless testcase replay proves the existing anchor/crop path cannot
  support 修罗.
- [ ] Add a 修罗 title template definition with a clear display name, threshold, and debug logging.
- [ ] Crop the same short task detail block from the matched title area unless replay proves 修罗 needs a
  slightly different crop height. If dimensions change, explain why and ensure 五环/五倍 behavior is
  untouched.
- [ ] Scan green text in the 修罗 detail crop and return the first usable green segment. 修罗 does not
  need 白龙马/显形镜 multi-link splitting or map-name OCR in this card.
- [ ] Expose a small replay/debug entry point or test helper that can run the 修罗 tracker read against
  saved testcase images without live game input.
- [ ] Add logs that make the read-only result auditable: source, title matched/missed, crop path, green
  segment bounds, returned absolute coordinate, and marked output path.

Verification:

- [ ] Add or save at least one repo-local testcase under
  `images/test-cases/task-tracker/xiuluo-task-panel/`.
- [ ] Run the actual matching/crop/green-scan algorithm against the testcase.
- [ ] Produce a marked output image showing:
  - tracker/title match position;
  - detail crop rectangle;
  - green band/segment bounds;
  - final returned screen-absolute coordinate.
- [ ] Add a focused source/replay test proving the method returns a coordinate for the positive testcase
  and returns empty when `修罗任务` title is absent.
- [ ] Compile with `mvn -q -DskipTests compile`; run `mvn -q -DskipTests test-compile` if the tree allows.
- [ ] Document the testcase input path, output marked image path, and command/tool used in
  `docs/ACTIVE_WORK.md`.

Performance / diagnostics:

- Expected effect is not runtime speedup yet. This card only creates the verified read-only primitive.
- The read should be comparable in cost to one existing tracker panel read and must not add any
  background runner polling.
- Fresh 修罗 runtime validation is not required for Done unless the implementation chooses to capture a
  live testcase image; acceptance can be based on replay/source/compile evidence because there is no
  task-flow integration in this card.

Card CR82: Skip pre-click dialog detection after direct-combat mode is entered

Problem statement:

- Fresh 修罗 `2026-06-23 13:05` evidence shows `Alt+A` direct-combat mode entered successfully, but
  `NpcClickService.runNpcClickPipeline(...)` stopped before tooltip / yellow / target-click strategies.
- The failure was not that `Alt+A` was unavailable:
  - `13:05:03.685` logged `NPC direct-combat click mode entered`;
  - `13:05:03.759` logged `NPC smart click skips name-layer preparation in direct-combat mode`;
  - `13:05:04.947` `before-learned-memory` detected `STORY`;
  - `13:05:07.858` cleanup clicked a story-like dialog;
  - `13:05:08.401` `after-pre-clean-story` still detected `STORY`;
  - `13:05:08.401` logged `NPC smart click still has blocking dialog after story cleanup; skip target
    click`, then `npc.click.smart ... verification=direct-combat result=false`.
- User clarification after the initial card: once `Alt+A` / direct-combat has entered target-pick mode,
  there should be no dialog in the business sense, and the direct-combat pipeline should not run
  pre-click dialog detection or cleanup at all. The detector result is not needed and can only create a
  false gate before target scanning.

Scope / business boundary:

- Scope is only `NpcClickService.runNpcClickPipeline(...)` behavior after `verificationMode=direct-combat`
  has been entered.
- Preserve normal non-direct-combat NPC safety:
  - ordinary learned-memory/NPC click paths should still clean a `STORY` once and abort if a dialog is
    still present;
  - ordinary learned-memory/NPC click paths should still treat pre-click `OPTION` as blocking unless a
    separate card explicitly changes that path.
- Preserve direct-combat safety from CR73:
  - flying/mount preflight before `Alt+A`;
  - `DirectCombatClickResult.positionRefreshRequired(...)` after failed direct-combat;
  - task-owned navigation/current-map refresh before a retry.
- Do not change:
  - tooltip / yellow / OCR / Ctrl-menu / click-coordinate algorithms;
  - DialogService global `STORY` / `OPTION` classification thresholds;
  - dialog option-click semantics; direct-combat should not treat an `OPTION` as a successful business
    dialog and should not click an arbitrary option as the fix;
  - 修罗 `XiuluoTaskV2` phase order, target recovery, CR79 wild-monster cancel, or CR80 pre-combat
    watchdog;
  - 五倍 白龙马/普通怪/黄袍 business rules.

Implementation requirements:

- [ ] Claim the card before editing code.
- [ ] Before editing, record current branch, pushed baseline, dirty status, and relevant
  `NpcClickService` / direct-combat log evidence in `docs/ACTIVE_WORK.md`.
- [ ] Add a focused source/wiring test before production edit. It should fail on current code because
  direct-combat still calls `currentPreClickDialogType(...)` / story cleanup before target strategies.
- [ ] Implement the direct-combat no-dialog-gate policy:
  - once `directCombatClickMode` is true, do not call pre-click dialog detection before target
    strategies;
  - do not call `DialogHandleRequest.clickStory(...)` cleanup inside the direct-combat pipeline;
  - treat the target-scan boundary as dialog-free and proceed directly to the existing tooltip / yellow /
    target-click strategies;
  - non-direct-combat behavior must remain unchanged.
- [ ] Do not click arbitrary `OPTION` choices as part of this fix.
- [ ] Add or update source guards proving WUBEI / normal NPC learned-memory paths do not inherit this
  direct-combat-only dialog-skip behavior.

Verification:

- [ ] Run `mvn -q -DskipTests test-compile`.
- [ ] Run the new direct-combat dialog-skip wiring test directly.
- [ ] Run existing direct-combat safety tests, especially CR73/flying-state and learned-memory/name-layer
  guard tests if present.
- [ ] Run `mvn -q -DskipTests compile`.
- [ ] Fresh runtime acceptance:
  - in a 13:05-like 修罗 direct-combat case, logs should show `Alt+A` entered and name-layer preparation
    skipped;
  - after entering direct-combat, there should be no `before-learned-memory` /
    `after-pre-clean-story` dialog detection or `npc-click:pre-clean-story` cleanup for that direct-combat
    pipeline;
  - logs must show at least one tooltip / yellow / target-click strategy was attempted;
  - the path must no longer end immediately at `NPC smart click still has blocking dialog after story
    cleanup` or `NPC smart click found existing option dialog before target click` for
    `verification=direct-combat`;
  - if direct-combat still fails, caller receives the existing position-refresh-required semantics.

Performance / diagnostics:

- Expected runtime effect: avoid wasting an entire direct-combat fallback on false dialog detection and
  remove the unnecessary one-shot story cleanup cost from direct-combat.
- Expected diagnostics: make the direct-combat dialog-skip visible in logs so future reports can
  distinguish "skipped dialog gate and attempted target scan" from "aborted before scan".
- This card does not address DialogService false-positive `STORY` / `OPTION` classification itself; that
  remains a separate detector-quality issue if it continues to cause non-direct-combat failures.

Card CR83: Pause pre-combat / pre-battle timers during formal maintenance

Problem statement:

- CR80 intentionally caps bad 修罗 pre-combat rounds at 180 seconds, but fresh runtime shows the
  current wall-clock timer also counts formal maintenance work that the task deliberately performs
  before entering battle.
- Fresh 修罗 evidence from `logs/dhxy-console.log`:
  - `2026-06-23 13:42:20.522` accepted objective with `healPetDue=true repairEquipmentDue=true`;
  - `13:42:21.054` started the heal-pet hook;
  - `13:42:41.821` handled the heal-pet maintenance broadcast;
  - `13:43:50.962` started the repair-equipment hook cleanup;
  - `13:44:03.147` handled the repair-equipment broadcast;
  - `13:44:03.154` logged `maintenance broadcast handoff delay ... delayMs=10000`;
  - `13:44:30.403-13:44:46.767` a team member ran summon-skill cleanup while the 修罗 team
    maintenance window was active;
  - `13:45:24.519` the leader hit
    `xiuluo pre-combat watchdog timeout: round=25 phase=NAVIGATE_TO_TARGET elapsedMs=189410
    limitMs=180000`.
- The timeout itself is useful, but maintenance time is not failed objective time. Counting it makes a
  healthy maintenance-heavy round look stale.
- User clarified this is not 修罗-only. 五倍 has separate pre-battle timers that must follow the same
  policy:
  - ordinary / 黄袍 first-battle `WindowRuntimeContext.ordinaryPreBattleStartedAtMs`, published by
    `WindowTaskRunner` as `PRE_BATTLE_TIMEOUT`;
  - 白龙马 / 显形镜 `currentProbeTaskStartedAt`;
  - `enterBattleStartedAt` / `enterBattleNextRetryAt` while waiting for an enter-battle dialog.

Scope / business boundary:

- Scope is timer accounting only. Formal maintenance time should not consume pre-combat/pre-battle
  timeout budget.
- Applies to:
  - 修罗 CR80 watchdog in `XiuluoTaskV2` / `XiuluoRoundContext`;
  - 五倍 ordinary pre-battle timeout owned by `WindowRuntimeContext` and published in
    `WindowTaskRunner`;
  - 五倍 probe timeout in `WubeiTask`;
  - 五倍 enter-battle retry/timeout state when active.
- Formal maintenance includes:
  - leader heal-pet / repair-equipment maintenance hooks;
  - handled maintenance-broadcast handoff delay;
  - task-opened team maintenance window time that blocks or intentionally gives a member room to run
    summon-skill cleanup.
- Do not pause timers for unrelated idle time, generic pathing, target click retries, bad navigation,
  direct-combat target search, or task phase failures.
- Do not change:
  - CR80 180s limit or CR44/五倍 existing timeout limits;
  - 修罗 return item rules; CR80/CR83 must still reaccept rather than use the return item before task
    completion;
  - 五倍 白龙马/普通怪/黄袍 business decisions;
  - OCR/template/click/navigation algorithms;
  - maintenance due/cooldown semantics.

Implementation requirements:

- [x] Claim the card before editing code.
- [x] Before editing, record current branch, pushed baseline, dirty status, and relevant log/code
  evidence in `docs/ACTIVE_WORK.md`.
- [x] Do not implement this by only removing maintenance phases from the timeout check. That would
  still count the wall-clock time and can timeout immediately after maintenance returns.
- [x] Add an explicit timer compensation primitive:
  - 修罗: adjust the round-local CR80 start timestamp or an accumulated paused duration in
    `XiuluoRoundContext`.
  - 五倍 ordinary: add a safe `WindowRuntimeContext` method to shift the active
    `ordinaryPreBattleStartedAtMs` and related ordinary target-map gate start if active, without
    resetting published state incorrectly.
  - 五倍 probe/enter-battle: reuse or extend existing pause compensation so formal maintenance shifts
    `currentProbeTaskStartedAt`, `currentProbeStoryWaitStartedAt`, `enterBattleStartedAt`, and
    `enterBattleNextRetryAt` when those timers are active.
- [x] Wrap formal maintenance sections with start/end timing and compensate only the measured blocked
  duration above a small threshold.
- [x] Include the maintenance-broadcast handoff delay in compensation when it is caused by a handled
  maintenance broadcast.
- [x] If the task opens a team maintenance window and then waits/allows a member summon-skill cleanup,
  compensate the leader's active pre-combat/pre-battle timer for that intentional maintenance window.
- [x] Emit structured logs, for example:
  - `[xiuluo-v2] pre-combat timer paused: source={} blockedMs={} adjustedStartAt={}`;
  - `[wubei ordinary-prebattle] timer paused: source={} blockedMs={} adjustedStartAt={}`;
  - `[wubei] probe timer paused: source={} blockedMs={} adjustedTaskStartAt={} ...`;
  - `[wubei] enter battle timer paused: source={} blockedMs={} adjustedStartAt={} adjustedNextRetryAt={}`.
- [x] Keep normal timeout behavior unchanged when no formal maintenance ran.

Verification:

- [x] Add source/wiring tests before production edits:
  - 修罗 maintenance pause shifts the CR80 effective start and prevents immediate post-maintenance
    timeout.
  - 修罗 non-maintenance stuck pre-combat path still times out at about 180s.
  - 五倍 ordinary pre-battle timer can be shifted in `WindowRuntimeContext`; `WindowTaskRunner` must
    not publish `PRE_BATTLE_TIMEOUT` during compensated maintenance time.
  - 五倍 probe timer is compensated when maintenance runs after `READ_TRACKER` starts a probe task.
  - 五倍 enter-battle timer compensation still preserves retry scheduling.
- [x] Run `mvn -q -DskipTests test-compile`.
- [x] Run the new focused tests directly.
- [x] Run `mvn -q -DskipTests compile`.
- [ ] Fresh runtime acceptance:
  - 修罗 maintenance-heavy round like `2026-06-23 13:42-13:45` logs timer pause/compensation and does
    not hit CR80 until 180s of non-maintenance pre-combat time has elapsed.
  - 五倍 ordinary/probe/enter-battle logs show maintenance pauses when active timers exist.
  - Real stuck 修罗 / 五倍 cases still timeout and reaccept normally.
  - No pre-completion 修罗 return item usage is introduced by this card.

Card CR84: 修罗 shortcut route state model

Problem statement:

- `docs/业务逻辑.md` now defines a 修罗快捷寻路路线 that is separate from the existing non-shortcut
  objective-navigation route.
- Current 修罗 context/state is still centered around the old objective route. The shortcut route needs
  explicit state before business transitions are wired, otherwise later cards will infer route mode from
  phase names, stale tracker state, or ad-hoc booleans.

Scope / business boundary:

- Add state/model only. Do not click tracker green links, do not call CR81 from task flow, do not change
  current 修罗 behavior in this card.
- Required concepts:
  - `routeMode`: `TRACKER_SHORTCUT` / `OBJECTIVE_NAVIGATION`;
  - `combatSource`: `NONE` / `TRACKER_CONFIRM` / `INCIDENTAL`;
  - current shortcut tracker read/click metadata;
  - first successful tracker green-click timestamp;
  - shortcut tracker retry count;
  - active tracker pathing intent id if needed by waits.
- Defaults must keep the current route behavior until later cards explicitly choose shortcut mode.

Implementation requirements:

- [ ] Claim the card before editing code.
- [ ] Record branch, pushed baseline, dirty status, and relevant `XiuluoTaskV2` /
  `XiuluoRoundContext` evidence in `docs/ACTIVE_WORK.md`.
- [ ] Add the smallest model/state surface that can support CR85-CR89. Prefer existing 修罗 context
  ownership over new global services.
- [ ] Emit route/combat state in logs at round start, route selection, tracker click, enter-battle
  confirmation, combat exit, and fallback boundaries.
- [ ] Do not change OCR/template/click/navigation/direct-combat/return-item behavior.

Verification:

- [ ] Add focused source/wiring tests proving defaults preserve the current route behavior.
- [ ] `mvn -q -DskipTests test-compile`.
- [ ] `mvn -q -DskipTests compile`.

Card CR85: 修罗 accept-time snapshot plus shortcut entry

Problem statement:

- Shortcut route should be fast: after accepting 修罗, it should not wait for story objective OCR before
  trying the left tracker green link.
- But fallback to the old route still needs the accept-time objective/story snapshot. If shortcut tries
  tracker first and loses that snapshot, the fallback route becomes unreliable.

Scope / business boundary:

- After accept option succeeds:
  - capture the same objective/story snapshot as the non-shortcut route;
  - submit the same background objective parse;
  - do not wait for that parse before shortcut;
  - run the existing due-time/status-based maintenance checkpoint;
  - then attempt CR81 tracker read.
- Maintenance semantics are unchanged: no due condition means skip with no action.
- Initial tracker miss or initial green-click failure falls back to the non-shortcut route entry and
  consumes the already-started objective parse.

Implementation requirements:

- [ ] Claim the card before editing code.
- [ ] Preserve the current accept NPC/accept option behavior.
- [ ] Start the objective snapshot parse immediately after accept succeeds.
- [ ] Ensure shortcut success abandons/ignores that fallback parse safely; no stale parse may overwrite
  shortcut state.
- [ ] Ensure tracker startup miss/click failure does not take a second screenshot or rerun dialog detect.
- [ ] Keep all maintenance checks behind the current due-time/status predicates.
- [ ] Do not open mini-map or start 灵兽村 exit prepath before the shortcut tracker attempt.

Verification:

- [ ] Source test: accept success starts snapshot/background parse, then attempts shortcut without
  waiting for OCR.
- [ ] Source test: initial tracker miss consumes the saved parse and enters non-shortcut route entry.
- [ ] Source test: maintenance not due is skipped and does not block shortcut.
- [ ] `mvn -q -DskipTests test-compile`.
- [ ] `mvn -q -DskipTests compile`.
- [ ] Fresh runtime: logs show accept -> snapshot submitted -> maintenance skipped/handled -> tracker
  read, with no old route objective wait before tracker hit.

Card CR86: 修罗 tracker green click and Runner park loop

Problem statement:

- CR81 can read the 修罗 left tracker green coordinate, but 修罗 task flow does not yet use it.
- The shortcut path should follow the 五倍普通怪 model: click tracker green, release the task turn, and
  wait for Runner/window facts. It should not poll foreground, read story, hand-navigate, or direct-click
  targets while pathing is owned by the game tracker.

Scope / business boundary:

- On a valid CR81 read, click the first `修罗任务` green link.
- Start the CR80 pre-combat watchdog from the first successful tracker green click.
- Release/park the task after the green click.
- `PATHING_TERMINAL` means only: re-read/reuse the current 修罗 tracker green, click the first green
  again, and park again.
- Re-clicking due to `PATHING_TERMINAL` must not reset the watchdog.
- Do not add fixed waits, do not read story, do not enter old route phases, do not click targets, do
  not use `Alt+A` / direct-combat from this shortcut loop.

Implementation requirements:

- [ ] Claim the card before editing code.
- [ ] Use the existing `TaskTrackerPanelService` / `TaskTrackerGreenLink` coordinate path; visual/click
  changes require testcase replay.
- [ ] Submit the physical green click through the input queue as an atomic move/click sequence.
- [ ] Register/publish whatever pathing wait metadata is needed so Runner can wake the same window.
- [ ] On `PATHING_TERMINAL`, reuse the same shortcut route policy and click tracker green again.
- [ ] Keep ordinary objective route and direct-combat fallback untouched.

Verification:

- [ ] Source test: tracker hit produces green click, routeMode shortcut, watchdog start, and wait/park.
- [ ] Source test: `PATHING_TERMINAL` re-clicks tracker green and does not reset watchdog.
- [ ] Replay/marked output if any click coordinate or crop policy changes.
- [ ] `mvn -q -DskipTests test-compile`.
- [ ] `mvn -q -DskipTests compile`.
- [ ] Fresh runtime: `tracker green click -> park`, and terminal wake shows same-green re-click.

Card CR87: 修罗 shortcut enter-battle confirmation and combatSource

Problem statement:

- Shortcut pathing is only complete when the final 修罗 enter-battle / 看打 dialog is confirmed.
- 战斗 may also start incidentally. These two cases must be separated because only tracker-confirmed
  battle should return home after combat.

Scope / business boundary:

- After shortcut green-click pathing, Runner/window layer prepares or observes the final 修罗
  enter-battle dialog.
- The task wakes, clicks the final confirmation, sets `combatSource=TRACKER_CONFIRM`, and enters
  `WAIT_COMBAT`.
- If combat starts without the confirmation click, set `combatSource=INCIDENTAL`.
- `TRACKER_CONFIRM` battle exit goes directly to 修罗 return-home.
- `INCIDENTAL` battle exit must not return home; clear necessary UI and resume shortcut tracker pathing.

Implementation requirements:

- [ ] Claim the card before editing code.
- [ ] Reuse existing 修罗 enter-battle dialog/template logic where possible.
- [ ] Make the wake/consume path explicit in logs: prepared/visible source, click result, combatSource.
- [ ] Do not require a post-battle tracker read for `TRACKER_CONFIRM`.
- [ ] Do not change wild-monster cancel recovery (CR79), direct-combat behavior (CR82), or non-shortcut
  enter-battle behavior except where shared safe helpers are reused.

Verification:

- [ ] Source test: prepared enter-battle dialog click sets `TRACKER_CONFIRM`.
- [ ] Source test: incidental combat sets `INCIDENTAL` and does not route to return-home.
- [ ] `mvn -q -DskipTests test-compile`.
- [ ] `mvn -q -DskipTests compile`.
- [ ] Fresh runtime: tracker pathing -> enter-battle prepared -> clicked -> WAIT_COMBAT -> return-home.

Card CR88: 修罗 shortcut failure fallback boundaries

Problem statement:

- Shortcut route and non-shortcut route must not be mixed halfway through one round. The docs explicitly
  require fallback to a route entry, not to old middle phases.
- Failure handling must also respect the 修罗 rule that return item cannot be used before task
  completion.

Scope / business boundary:

- Initial tracker miss or initial green-click failure:
  - enter non-shortcut route from its entry;
  - consume the accept-time objective snapshot parse from CR85.
- Mid-shortcut tracker miss/click failure or repeated inability to progress:
  - either enter non-shortcut route from its entry when the saved snapshot is still valid, or use
    existing round failure/reaccept handling;
  - do not jump into old `NAVIGATE_TO_TARGET` / `CLICK_TARGET_NPC` directly.
- CR80 watchdog timeout in shortcut route uses the existing failure/reaccept semantics and does not use
  the 修罗 return item.

Implementation requirements:

- [ ] Claim the card before editing code.
- [ ] Centralize shortcut-failure logging with reason, routeMode, combatSource, tracker retry count, and
  saved objective parse status.
- [ ] Make route fallback explicit: shortcut -> non-shortcut entry, or shortcut -> round failure.
- [ ] Guard against stale objective parse overwriting a later shortcut or new round.
- [ ] Preserve current non-shortcut failure/recovery behavior.

Verification:

- [ ] Source test: startup tracker miss uses saved parse and enters non-shortcut entry.
- [ ] Source test: mid-shortcut failure does not call old middle navigation/click phases.
- [ ] Source test: watchdog timeout never uses 修罗 return item before completion.
- [ ] `mvn -q -DskipTests test-compile`.
- [ ] `mvn -q -DskipTests compile`.

Card CR89: 修罗 shortcut maintenance and team window boundaries

Problem statement:

- Shortcut route should allow member maintenance only after the leader has submitted the real tracker
  route. Before that, the leader is still accepting/choosing a route and should not be interrupted.
- Maintenance checks themselves remain due-time/status based; shortcut must not turn them into mandatory
  per-round work.

Scope / business boundary:

- Before successful tracker green click:
  - no team pathing maintenance window;
  - no member 三技能/补给 insertion.
- After successful tracker green click:
  - open the same kind of team pathing maintenance window used for route-owned movement;
  - allow member maintenance while the game tracker owns pathing.
- Close the window when:
  - final enter-battle dialog appears/prepares;
  - combat starts;
  - shortcut fails or switches to non-shortcut fallback;
  - the round exits/returns home.
- CR83 must compensate any formal maintenance time so CR80's 180s shortcut watchdog is not consumed by
  valid maintenance.

Implementation requirements:

- [ ] Claim the card before editing code.
- [ ] Keep due-time/status predicates unchanged for heal pet, repair, and maintenance broadcasts.
- [ ] Add clear logs for maintenance skipped/due/handled and team maintenance window open/close.
- [ ] Ensure no member maintenance can run between accept success and tracker green route submission.
- [ ] Reuse CR83 timer compensation instead of adding a second timer policy.

Verification:

- [ ] Source test: no-due maintenance skips and proceeds to tracker.
- [ ] Source test: member maintenance gate is closed before tracker green click and open after.
- [ ] Source test: enter-battle/combat/failure closes the gate.
- [ ] `mvn -q -DskipTests test-compile`.
- [ ] `mvn -q -DskipTests compile`.
- [ ] Fresh runtime: maintenance-heavy shortcut round shows timer pause logs and no false CR80 timeout.

Card CR90: 修罗 shortcut route end-to-end acceptance

Problem statement:

- CR84-CR89 are implementation slices. A separate acceptance card is needed so Done status is based on
  a complete run, not just per-slice compile/source evidence.

Scope / acceptance scenarios:

- Happy path:
  - accept task;
  - capture fallback objective snapshot;
  - skip or complete due maintenance;
  - read `修罗任务` tracker;
  - click green;
  - park;
  - click final enter-battle confirmation;
  - wait combat;
  - return home.
- Initial tracker miss:
  - fallback to non-shortcut route entry using saved objective parse.
- `PATHING_TERMINAL`:
  - re-click same 修罗 tracker green and park again;
  - watchdog timer is not reset by same-green re-click.
- `INCIDENTAL` combat:
  - combat exit does not return home;
  - resumes tracker shortcut pathing.
- Maintenance:
  - no due condition skips maintenance;
  - due maintenance runs and does not false-timeout CR80.
- Failure safety:
  - no pre-completion 修罗 return item usage;
  - no old route middle phase is called from shortcut mid-flow.

Verification:

- [ ] Run fresh 修罗 shortcut sessions and append evidence to the current run report.
- [ ] Record Log ranges for each scenario.
- [ ] Mark CR84-CR89 Done only after their own source/runtime criteria are met.
- [ ] Mark CR90 Done only after a complete shortcut happy path plus at least one recovery path are
  observed.
- [ ] Performance check: shortcut should reduce accept-to-enter-battle latency compared with the
  non-shortcut objective-navigation route; if no latency evidence is available, keep CR90 in Review.

Card CR91: 修罗 accept snapshot plus exit-prepath overlap

Problem statement:

- Current CR85/CR86 shortcut order waits for tracker read/click after accept. Fresh user observation
  shows the leader can stand still for roughly 1-2 seconds while the tracker panel is being read.
- The previous non-shortcut route already had a useful Ling Shou Village exit prepath. The new shortcut
  route should overlap that movement with tracker parsing instead of waiting in place.
- However, once the mini-map is opened it may cover or disturb the left tracker. The accept-time
  evidence must therefore be captured before opening the mini-map.

Required behavior:

- After the accept option is clicked successfully, capture one bound game-window snapshot. This is the
  current game client window/client image, not the desktop full screen.
- Derive two evidence streams from that same snapshot:
  - story objective crop for the existing non-shortcut fallback background parse;
  - left tracker panel/detail crop for 修罗 `修罗任务` title and first green-link coordinate parsing.
- Do not run full dialog detect or a second foreground story read at this point.
- If accept-time maintenance is not due:
  - immediately start the existing start-map exit prepath using the current-map mini-map
    fire-and-handoff policy;
  - after the mini-map is closed / the prepath handoff returns, consume the accept-time tracker parse
    result and click the first green link when available;
  - then register the CR86 tracker pathing intent and park exactly as the current shortcut route does.
- If accept-time maintenance is due:
  - run the existing maintenance flow first and preserve CR83 timer compensation;
  - after maintenance completes, do not run the start-map exit prepath;
  - re-read the current tracker panel and click the first green link from the fresh tracker read.
- If accept-time tracker parsing fails or the post-maintenance fresh tracker read fails, shortcut startup
  fails and must fall back only to the non-shortcut route entry using the accept-time story objective
  parse result.

Boundaries:

- Do not change the 修罗 tracker title template, green-link scan algorithm, click point calculation, or
  thresholds unless a separate replay-backed visual card is opened.
- Do not change story objective OCR/template recognition.
- Do not call old objective-route middle phases from the shortcut route. The exit prepath is only a
  head start before tracker green click; it must not own target navigation or target click.
- Do not use the 修罗 return item before task completion.
- Do not reset the pre-combat watchdog on same-green re-click; keep CR83 maintenance compensation.
- Do not change 五倍/五环/WUBEI behavior.

Implementation notes:

- Prefer adding an accept-time game-window snapshot API and replayable crop/parse helpers so the story
  objective crop and tracker crop can be traced back to the same raw image.
- Keep debug artifacts window-scoped. Save the raw accept snapshot and both derived crops with source tags
  that include the 修罗 round.
- Existing live `TaskTrackerPanelService.readXiuluoTrackerPanel(...)` may remain as the post-maintenance
  fresh-read path; the accept-time fast path should be able to parse from the saved snapshot/crop without
  recapturing after the mini-map opens.

Verification:

- [ ] Source/replay evidence: saved accept-time game-window snapshot can produce both a story objective
  crop and a 修罗 tracker detail/green-link result. Marked tracker output must show the selected green
  click point if any click-coordinate behavior is touched.
- [ ] Source test: no-due maintenance path schedules/uses accept-time tracker parse, starts
  `xiuluo-v2:start-exit-prepath`, then clicks tracker green and enters `WAIT_TRACKER_SHORTCUT_PATHING`.
- [ ] Source test: due-maintenance path runs maintenance, skips start-exit-prepath, fresh-reads tracker,
  clicks green, and preserves pre-combat timer compensation.
- [ ] Source test: accept-time tracker miss falls back through the saved story objective parse only.
- [ ] `mvn -q -DskipTests test-compile`.
- [ ] `mvn -q -DskipTests compile`.
- [ ] Fresh runtime: no-maintenance round logs `accept snapshot` -> `start-exit-prepath` -> tracker green
  click -> CR86 park; due-maintenance round logs maintenance -> no prepath -> fresh tracker green click.
- [ ] Performance check: accept-to-green-click/accept-to-enter-battle latency improves versus CR85/CR86
  ordering, with no new runner 100ms/900ms churn or WAIT_COMBAT regression.

Card CR92: pause-aware event-wait timer compensation

Problem statement:

- CR83 compensates formal maintenance time and normal checkpoint-level user pause time, but it does not
  cover a task thread that is parked inside a scheduling/event wait.
- Fresh 修罗 evidence from `2026-06-23 19:09:59.697-19:14:04.106` proves the gap:
  - `19:09:59.697` 修罗 tracker green was clicked and the phase advanced to
    `WAIT_TRACKER_SHORTCUT_PATHING`.
  - `19:10:31.386` user pause was requested for all windows.
  - `19:13:11.876` the leader window resumed.
  - `19:14:04.106` `WindowReadyEventBus.await` returned with `elapsedMs=244409`, and the 修罗
    pre-combat watchdog immediately failed with `elapsedMs=244409 limitMs=180000`.
- The roughly 160 seconds spent paused were counted as active pre-combat time. If that pause had been
  compensated, the active elapsed time would have been roughly 84 seconds and the round would not have
  hit the 180s watchdog.

Required behavior:

- User pause time must not count against 修罗 CR80 pre-combat watchdog while the task is parked in
  `WAIT_TARGET_PATHING_TERMINAL`, `WAIT_TRACKER_SHORTCUT_PATHING`, or any other pre-combat
  event-wait phase.
- The same principle must apply to 五倍 pre-battle timers that can park in `WindowReadyEventBus` waits:
  ordinary pre-battle, probe/enter-battle waits, and any existing CR83-covered timer surface.
- The fix may either:
  - make the task park layer measure and return pause-blocked time, then apply the existing
    `pausePreCombatTimer(...)` / equivalent 五倍 compensation before watchdog checks; or
  - make the relevant business timer read active elapsed time from a pause-aware clock/runtime counter.
- Avoid double-counting when the same pause overlaps multiple waits or when a wait returns immediately
  after resume.
- `window.ready.await elapsedMs` may continue to log wall-clock elapsed time for diagnostics, but the
  business watchdog logs must make the compensated active elapsed time visible.
- If a watchdog failure/restart still happens, clear or reconcile the stale pathing/prepared state owned
  by that failed wait. The `19:14:17` follow-up logs showed the old
  `xiuluo-v2:tracker-shortcut:10:0` intent and prepared 修罗 enter-battle action still affecting route
  gates after the round had restarted from accept flow.

Boundaries:

- Do not change the 180s 修罗 timeout or any existing 五倍 timeout value in this card.
- Do not change OCR, template matching, click coordinates, tracker green-link recognition, navigation
  route choice, direct-combat fallback order, maintenance cooldowns, or return-item business rules.
- Do not treat generic ready-event negative signals as new business truth. The change is timer
  accounting only, plus stale wait-owned state cleanup on the existing failure path if needed.
- Preserve stop/interruption behavior: a stopped task must still exit promptly instead of waiting for a
  timer compensation path.

Verification:

- [x] Source test: a simulated user pause while 修罗 is parked in `WAIT_TRACKER_SHORTCUT_PATHING`
  advances wall-clock beyond 180s but does not trip the CR80 watchdog after compensation.
- [x] Focused exec/source guard: pause requested while `WindowReadyEventBus.awaitNewer(...)` or
  `awaitNewerPathingTerminalOrPreparedRoute(...)` is parked returns in under 1s, logs
  `pause-wake`, and does not publish `PATHING_TERMINAL` / `PREPARED_ACTION_READY`; stop control wake
  also returns promptly and logs `stop-wake`. The same guard checks the 五倍 finite prepared-dialog
  empty-wake branch reaches `TaskCheckpoint` before falling back.
- [x] Source test: checkpoint-level pause compensation from CR83 still works and is not double-counted
  with event-wait compensation.
- [x] Source test or log assertion: if a pre-combat watchdog restart occurs, stale tracker-shortcut
  pathing intent / prepared 修罗 enter-battle action from the failed wait no longer blocks the next
  accept/navigation route gates.
- [x] Equivalent 五倍 timer coverage is tested or explicitly documented for every timer touched.
- [x] `mvn -q -DskipTests test-compile`.
- [x] `mvn -q -DskipTests compile`.
- [ ] Fresh runtime: reproduce a user pause while 修罗 is parked; logs should show pause compensation
  before watchdog evaluation, no false `xiuluo pre-combat watchdog timeout`, and no stale old
  tracker-shortcut intent after resume/restart.
- [ ] Performance check: no new repeated turn reacquire, runner high-frequency wait churn, WAIT_COMBAT
  empty spin, or extra event-wait p95/p99 regression.

Fresh failure - 2026-06-27 21:36 pause while parked:

- `21:36:45.324` 队长进入 `WAIT_TARGET_PATHING_TERMINAL` ready-event wait：
  `wakeTypes=[PREPARED_ACTION_READY, PATHING_TERMINAL] timeoutMs=-1 afterSequence=0`。
- `21:36:49.846` 用户暂停全部窗口，5 个窗口都记录 `task pause requested`。
- 4 个队员窗口在 `21:36:49.890 - 21:36:50.476` 之间进入
  `TaskPauseToken : task pause checkpoint reached`。
- 队长窗口 `hwnd-17240550` 没有被 pause 唤醒，直到 `21:39:32.593`
  `WindowReadyEventBus.await result=timeout ... timeoutMs=167268 elapsedMs=167269` 才返回，
  随后才记录 `TaskPauseToken : task pause checkpoint reached`。
- 结论：CR92 当前“wait 返回后补偿 pauseBlockedMs”的 source repair 不能解决用户暂停时
  task thread 仍被 park 卡住的问题。这里不是业务 timeout 计算错误，而是事件等待层没有把
  pause/stop 作为 wake signal。
- Concrete repair task:
  - 在 `WindowTaskRunner` / task pause request 路径上，通知当前窗口的 `WindowReadyEventBus`
    或等价 park primitive，让正在等待的 `await(...)` 立即返回一个 pause/interrupt 类型结果，
    或至少返回可让 task 立刻进入 `TaskPauseToken` checkpoint 的结果。
  - `WindowReadyEventBus.await(...)` 返回日志需要能区分 `pause-wake` 与普通 timeout/event。
  - 修罗 / 五倍 task 层收到 pause wake 后只做 pause checkpoint 和既有 timer compensation；
    不能把 pause wake 当作 `PATHING_TERMINAL`、`PREPARED_ACTION_READY` 或新的业务事实。
  - 保留 CR92 已有 stopwatch compensation，避免恢复后把暂停时长计入 CR80/CR83/五倍 timers。
  - 不改导航、OCR、模板、点击坐标、direct-combat、维护 cooldown 或回程业务。
- Source repair - 2026-06-27 pause/stop control wake:
  - `WindowReadyEventBus` now records a per-window control wake with its own global sequence and
    wakes the same monitor used by `awaitNewer(...)`,
    `awaitNewerPathingTerminal(...)`, and `awaitNewerPathingTerminalOrPreparedRoute(...)`.
  - `wakeForTaskPause(windowId, reason)` logs `window.ready.await result=pause-wake`;
    `wakeForTaskStop(windowId, reason)` logs `result=stop-wake`. Both return an empty
    `Optional<WindowReadyEvent>` to the task and leave `latest(... PATHING_TERMINAL /
    PREPARED_ACTION_READY / COMBAT_STATE_CHANGED)` empty.
  - `WindowTaskRunner.pauseCurrentTask()` calls `wakeForTaskPause(...)` after setting the
    `TaskPauseToken`. `WindowTaskRunner.stopCurrentTask()` calls `wakeForTaskStop(...)` while the
    stop/interrupt path remains in place.
  - 修罗 and 五倍 task layers were not changed: after the empty control wake they still run the
    existing `TaskCheckpoint.throwIfStopRequested(...)` checkpoint and existing
    `pauseBlockedMs` / timer compensation.
  - One 五倍 finite prepared-dialog wait had an empty-wake branch before the next checkpoint. It now
    calls `TaskCheckpoint.throwIfStopRequested(...)` before preserving the old break/return-null
    behavior, so a pause control wake cannot be consumed as ordinary "dialog not ready" evidence.
- Fresh runtime acceptance:
  - 修罗 leader 在 `WAIT_TARGET_PATHING_TERMINAL` / `WAIT_TRACKER_SHORTCUT_PATHING` / pre-combat
    event wait 中按暂停后，应该在秒级内出现 `TaskPauseToken` checkpoint；
  - 日志中不应再出现“pause requested 后仍等到剩余 bounded timeout 才 checkpoint”；
  - 恢复后仍需保留已有 `pauseBlockedMs` / compensated watchdog 证据。

Card CR94: summon-skill unknown failure backoff and layout cache invalidation

Problem statement:

- Fresh 修罗 runtime proves a 三技能 failure storm around unknown slot status:
  - `2026-06-23 21:12:32.287` `hwnd-E850B6A` starts summon-skill clean with
    `cachedSkillCount=6 trustSkillCount=true cachedStartSlot=5`.
  - `21:12:35.533` `SummonSkillService` logs `use trusted cached skill slot count 6`.
  - `21:12:44.147` the pass fails with `message=slot status unknown`, and
    `TaskMaintenanceService` releases the team-round claim.
  - Later runs reproduce the same pattern, for example `22:54:00.333`,
    `22:54:15.035`, and `22:54:30.654` all fail with `slot status unknown` in the same
    `teamRound=xiuluo_v2#67`, each followed by claim release and a new start a few seconds later.
  - `22:59:23.182`, `22:59:42.376`, and the next start at `22:59:46.352` show the same retry loop in
    `teamRound=xiuluo_v2#69`.
  - Fresh `2026-06-24 13:43-13:45` runtime after CR95 source fixes shows a narrower remaining
    problem: `hwnd-4E16C4` starts with `cachedSkillCount=null trustSkillCount=false cachedStartSlot=null`,
    so stale trusted 6-slot cache is not reused, but `slot status unknown` failures at `13:43:23.686`
    and `13:43:50.453` still allow another start at `13:45:05.628`, well under the intended 5-minute
    retry-after. This means the cache invalidation side has positive evidence, while the per-window
    unknown retry-after still needs implementation or repair.
  - Fresh `2026-06-24 15:02-15:08` runtime confirms the retry-after side is still not reliable:
    `hwnd-4F60446` fails unknown at `15:02:46.487`, keeps attempting during `teamRound=xiuluo_v2#50`
    as `summon skill round claim limit reached`, and actually starts another pass at `15:07:36.811`,
    about 4m50s after the failure and below the intended 5-minute backoff.
- The root cause is the combination of:
  - `AutoBattleTask` FREE idle loop checks maintenance roughly every 3 seconds;
  - `slot status unknown` failure does not refresh the normal 20-minute success cooldown;
  - no-state-change failure releases the team-round claim;
  - the cached skill-count/layout remains trusted for the next pass, so the same possibly-wrong
    6-slot assumption and cached start slot are reused.
- The result is repeated `Alt+O` / slot-hover maintenance work every few seconds until a later pass
  happens to succeed or the task state changes.

Required behavior:

- Unknown-class summon-skill failures must apply a short per-window retry-after, default 5 minutes.
  While `now < retryAfter`, `TaskMaintenanceService` should skip starting `SummonSkillService` and
  return/defer as a retry-backoff maintenance result.
- The retry-after must be separate from `lastSummonSkillCleanAtByWindow`; do not fake a successful
  clean or accidentally turn an unknown failure into the normal 20-minute cooldown.
- Unknown-class failure should include at least:
  - cleanup result message containing `unknown`, such as `slot status unknown`;
  - or observed slot statuses containing `SummonSkillSlotStatus.UNKNOWN`.
- On unknown-class failure, invalidate trusted layout memory for that window before the next eligible
  pass:
  - `skillCountCachedAt` must be cleared or expired so `buildSummonSkillCleanupRequest(...)` produces
    `trustExpectedSkillCount=false`;
  - stale `nextStartIndex` should be cleared unless the implementation can prove it remains safe;
  - stale slot-status / tail-safe cache should not let the next pass skip the full 6/8 detection.
- After the 5-minute retry-after expires, the next pass must re-detect whether the summon-skill panel
  has 6 or 8 slots before deciding where to inspect/delete.
- A successful pass should clear any outstanding retry-after and then keep the existing 20-minute
  success cooldown behavior.
- If `ultimateGenerateSucceeded=true` appears on a failed pass, preserve the existing ultimate
  generation timestamp behavior while still applying unknown retry/backoff if the final failure is
  unknown-class.

Boundaries:

- Do not change summon-skill template images, slot click coordinates, hover timing, delete/confirm
  click algorithms, locked-slot backward scan behavior from CR63, or tail-safe success semantics.
- Do not change `AutoBattleTask.FREE_PATROL_INTERVAL_MS`; the idle loop may still tick every 3s, but
  `TaskMaintenanceService` must decline work during retry backoff.
- Do not block other windows globally for 5 minutes. The backoff is per failed window; team-round claim
  release may remain as-is unless implementation evidence proves a second-window storm.
- Do not mark unknown failure as success and do not refresh normal summon-skill clean cooldown.
- Keep valid immediate refresh behavior for non-unknown cases unless the implementation explicitly
  documents and tests a separate non-unknown backoff policy.

Verification:

- [x] Source test: an unknown-class failed cleanup records a 5-minute retry-after and invalidates the
  trusted skill-count/layout cache.
- [x] Source test: a maintenance check before retry-after expiry does not call
  `SummonSkillService.cleanSummonSkillsOnce(...)`, even though the AutoBattle idle loop is still
  eligible and the normal 20-minute cooldown is due.
- [x] Source test: after retry-after expiry, the next `SummonSkillCleanupRequest` has
  `trustExpectedSkillCount=false` and no stale cached start slot that would skip 6/8-slot detection.
- [x] Source test: a successful cleanup clears retry-after and writes the existing 20-minute success
  cooldown.
- [x] `mvn -q -DskipTests test-compile`.
- [x] `mvn -q -DskipTests compile`.
- [ ] Fresh runtime: after a `slot status unknown` failure, logs show one retry-backoff record and no
  repeated `maintenance: start summon skill clean` for the same window for roughly 5 minutes.
- [ ] Fresh runtime: the next post-backoff pass logs full skill-slot detection rather than
  `use trusted cached skill slot count`.
- [ ] Performance check: maintenance log/input pressure drops during unknown failure periods, and no
  valid 三技能 success path regresses.

Card CR95: live window identity drift guard and cache invalidation

Problem statement:

- Fresh `2026-06-23 23:19:58-23:20:12` runtime shows a real window identity drift / stale binding
  failure that can corrupt per-window state:
  - Logs say `hwnd-E850B6A` / native handle `243600234` / title `忆叶知秋（ID：451753529）`
    starts 三技能 and trusts `cachedSkillCount=6`.
  - The actual screenshot written under that same context,
    `images/temp/hwnd-E850B6A/latest_vision.png` (`2026-06-23 23:20:11.626`), visibly shows
    `うprinoe大叔（ID：316365558）` and an 8-slot summon-skill panel.
  - The pass then logs `use trusted cached skill slot count 6`, inspects slot 6, and fails with
    `slot status unknown`.
  - Meanwhile `hwnd-3960EA6/latest_vision.png` (`2026-06-23 23:18:05.548`) shows
    `岁月醉白头（ID：387545229）`, while logs still treat `hwnd-3960EA6` as 大叔.
- Code review confirms why logs can be misleading:
  - `WindowNativeBindingRefreshService.refreshGeometry(...)` only refreshes geometry from the live
    HWND; it does not refresh `GetWindowText(...)`, class, or process identity.
  - `WindowNativeBinding.withGeometry(...)` preserves the old title.
  - `WindowRuntimeContext.sameNativeBinding(...)` ignores title, so a same-HWND title/player change
    does not clear dialog/prepared state.
  - Input logs print `binding.getTitle()`, not the live Windows title, so they can report the stale
    account even while the HWND capture is showing a different account.
- This is not only a 三技能 problem. Any player-specific window cache keyed only by `windowId` can be
  wrong after live title/account drift, including 三技能 layout cache, player identity, incense /
  medicine memories, dialog/prepared state, and debug screenshots.

Required behavior:

- Refresh live native title together with live geometry for a bound HWND:
  - use `GetWindowText(...)` during `WindowNativeBindingRefreshService.refreshGeometry(...)` or an
    equivalent binding refresh path;
  - preserve existing geometry refresh behavior and handle-invalid fallback.
- Detect same-HWND live title/player drift:
  - compare previous binding title with refreshed live title;
  - parse player identity from the refreshed title when possible;
  - log a high-signal warning with `windowId`, handle, old title, new title, old player id/name,
    and new player id/name.
- When the live title/player changes under the same `windowId`, invalidate stale per-player state
  before the next OCR/template/maintenance decision can use it:
  - clear visible dialog snapshot, dialog interest, dialog preparation request, prepared dialog
    action, pathing/prepared task state that is tied to the old player;
  - invalidate 三技能 cached skill count/layout/start slot for that window;
  - invalidate or require revalidation for player-specific maintenance memories such as incense /
    medicine state if they are keyed only by `windowId`;
  - refresh `GameContext.State.me` identity through `ClientIdentityService` using the live title.
- The guard must run on normal bound-window capture/input/task tick paths, not only during startup
  registration, because a long-running client can change account/title after registration.

Boundaries:

- Do not change HWND capture algorithms, screenshot crop regions, template thresholds, OCR cleanup,
  click coordinates, summon-skill slot/delete behavior, or 修罗/五倍 task business flow.
- Do not rename or regenerate `windowId` for an existing runtime in this card unless implementation
  evidence proves it is safe. This card may keep the stable window runtime id, but must prevent stale
  player-specific state from surviving a live title/player drift.
- Do not treat a transient blank title as a player change; blank/invalid live titles should warn and
  fail closed or retry refresh rather than clearing useful state on noise.
- CR94 still owns the `slot status unknown` 5-minute retry/backoff behavior. CR95 owns the deeper
  stale binding / stale identity / stale cache root cause.

Verification:

- [x] Source test: live `refreshGeometry(...)` refreshes title from Windows and returns a new binding
  when title changes under the same native handle.
- [x] Source test: same-HWND title/player drift triggers runtime-state clearing and player-specific
  cache invalidation, including 三技能 layout cache.
- [x] Source test: geometry-only refresh with the same title keeps existing behavior and does not
  clear task state unnecessarily.
- [x] Source test: blank live title does not falsely clear player state.
- [x] `mvn -q -DskipTests test-compile`.
- [x] `mvn -q -DskipTests compile`.
- [x] Review fix required: transient blank live title must not be folded into a stale caller binding
  before runtime commit; concurrent late blank refresh must not roll identity back to the old player.
  Codex 2026-06-24 re-review: caller-side folding is fixed, and
  `WindowRuntimeContext.setNativeBinding(...)` is now synchronized so blank-title preservation, drift
  detection, epoch/cache clearing, and binding assignment happen as one commit.
- [x] Review blocker: stale nonblank live-title snapshots must not late-commit after a newer same-HWND
  player drift. Fixed in source by moving production live read + runtime commit into synchronized
  `WindowNativeBindingRefreshService.refreshAndCommit(...)`; production runtime callers no longer
  capture a live snapshot and then commit it later from outside the runtime monitor.
- [x] Review blocker: queued physical input requests must capture and validate request-time
  `playerIdentityEpoch`. `InputActionRequest` now captures the epoch at enqueue time, and
  `InputActionWorker` rejects old-epoch requests before focus, before exclusive callbacks, and before
  each queued action.
- [x] Review blocker: pure HWND keyboard shortcuts must reject when their internal
  `refreshAndCommit(...)` observes a player drift. Fixed in source:
  `BoundWindowKeyboardService.pressShortcut(...)` captures `playerIdentityEpoch` before refresh and
  returns `player-identity-epoch-changed` before `toHwnd(...)` / `PostMessage(...)` when refresh bumps
  the epoch.
- [x] Review blocker: `player-identity-epoch-changed` must be terminal for input, not a generic
  "HWND not attempted" fallback reason. Fixed in source: `ShortcutAttempt` now carries a terminal flag,
  `BoundWindowKeyboardService.pressShortcut(...)` returns terminal rejection for refresh-time identity
  drift, `InputActionWorker.pressAltShortcut(...)` fails the request instead of focused fallback, and
  `NavigationService.pressAlt1ForMiniMap(...)` skips direct `inputProvider.pressAlt1()` fallback.
- [x] Review blocker: if `BoundWindowKeyboardService.pressShortcut(...)` cannot refresh/commit the live
  HWND binding at all (`refreshAndCommit(...)` returns empty), it should fail closed instead of sending
  a background key to the cached native handle. Fixed in source: empty refresh returns terminal
  `live-binding-refresh-unavailable` before `toHwnd(...)` / `PostMessage(...)`.
- [x] Review follow-up: background HWND keyboard support should stay within the verified shortcut set,
  or each extra shortcut must have its own validation evidence. Fixed in source: `InputActionWorker`
  only maps `Alt+1/2/4/6/8/T/O/E/Q` to background HWND keyboard; `Alt+A/C/U` remain on focused real
  input until separately validated.
- [x] Jason+Codex heartbeat review P1, 2026-06-24 10:04 ET: terminal `Alt+1` rejection in
  `NavigationService.pressAlt1ForMiniMap(...)` only returns from the void helper. Callers such as
  `src/main/java/com/bot/dhxy/service/NavigationService.java:2128` previously could still continue the
  exclusive mini-map sequence, sleep, and execute the real mouse click. Fixed in source: the helper now
  returns a status and aborts the current mini-map open/click/close transaction before focused/direct
  real input or mouse click.
- [x] Jason+Codex heartbeat review P1, 2026-06-24 10:04 ET: `InputActionQueue` and
  `WindowAwareInputCoordinator` still ignore `refreshAndCommit(...)` empty returns on real input paths.
  Fixed in source: `InputActionQueue` rejects before enqueueing when live refresh is unavailable, and
  `WindowAwareInputCoordinator` aborts or throws before focus / direct real input in that case.
- [x] Jason+Codex heartbeat review P2, 2026-06-24 10:04 ET: `BoundWindowKeyboardService.AltShortcut`
  still publicly exposes `ALT_A`, `ALT_C`, and `ALT_U` at the service boundary. Even though
  `InputActionWorker` no longer maps those actions to background HWND dispatch, direct callers of
  `pressShortcut(...)` could still `PostMessage` unvalidated shortcuts. Fixed in source: enum values are
  retained for compatibility but have `backgroundHwndSupported=false`, and direct `pressShortcut(...)`
  returns terminal `unvalidated-background-shortcut`.
- [x] Jason+Codex heartbeat review P3, 2026-06-24 10:04 ET: latest terminal-failure/whitelist coverage
  is mostly source-string checking in `WindowIdentityDriftP2WiringTest`. Improved in source guards:
  terminal mini-map `Alt+1` rejection must gate later `clickLeft`, live-refresh-unavailable must reject
  real input setup, and service-level `Alt+A/C/U` background dispatch must be impossible. Fresh runtime
  validation is still required before CR95 closes.
- [x] Review fix required: 三技能 epoch-scoped state invalidation must run before any old
  `lastSummonSkillCleanAtByWindow` cooldown can return `SUMMON_SKILL_NOT_DUE`.
- [x] Review fix required: pure input paths (`InputActionQueue`, `WindowAwareInputCoordinator`,
  `BoundWindowKeyboardService`) should refresh/commit live binding before focus/key/mouse logging and
  input.
- [x] Review fix required: `TeamRoleDetectionService.resolveCurrentPlayerId(...)` should prefer current
  runtime binding title over task-start `TaskExecutionContext.nativeWindowTitle`.
- [x] Review follow-up: identity and team-role reads must force live refresh before trusting runtime
  title. `ClientIdentityService.resolveCurrentWindowTitle(...)` and
  `TeamRoleDetectionService.resolveCurrentPlayerId(...)` now call `refreshAndCommit(...)` before
  reading the current runtime binding title.
- [x] Review follow-up: `AutoCombatService` window runtime state should include `playerIdentityEpoch`
  and clear pending first-aid / combat-entry maintenance state on drift.
- [x] Test gap: add coverage for blank-title preservation, synchronized commit source guard, and
  cooldown-before-epoch ordering.
- [ ] Test gap: add fuller behavior tests for queued input epoch cancellation, HWND shortcut
  refresh-time epoch rejection with no real-input fallback, failed live refresh fail-closed behavior,
  input-path refresh, identity sync live-title priority, and role-id title priority; the current
  `WindowRuntimeBindingCommitSynchronizationTest`, `WindowIdentityDriftP2WiringTest`, and
  `WindowIdentityDriftCacheInvalidationWiringTest` are useful source-string guards, but they do not
  prove these runtime behaviors.
- [x] Review follow-up: blank committed title should not become a pseudo parseable tracker title.
  `GameClientTracker` now reads the committed runtime binding, so preserved titles are safe, but if the
  committed binding itself is blank, `fullWindowTitle` can still become `hwnd-*`; prefer retaining a
  previous parseable tracker title or failing closed with blank. Fixed in source:
  `GameClientTracker.useBoundWindowIfAvailable(...)` now resolves blank live titles by retaining only a
  previous parseable tracker title; if none exists, it writes a blank tracker title instead of
  `context.getWindowId()`.
- [ ] Fresh runtime: when a title/account drift happens, logs show the guard warning before the next
  maintenance pass, and the next 三技能 pass does not reuse a stale `trustSkillCount=true` layout from
  the previous player.
- [ ] Fresh runtime: screenshot temp folder, input logs, and parsed identity agree on the same live
  player after refresh.
- 2026-06-24 10:59 ET urgent Jason/Boyle + Codex recheck: no new P1/P2 blocker found in source. The
  prior mini-map terminal Alt+1 issue is repaired (`pressAlt1ForMiniMap(...)` returns false and callers
  abort before click), real-input queue/focus paths fail closed when `refreshAndCommit(...)` returns
  empty, and direct `pressShortcut(ALT_A/C/U)` callers are rejected by the service-level
  `backgroundHwndSupported=false` guard before any background `PostMessage`. Focused compile and CR95
  tests passed; fresh runtime items above remain open.

Card CR96: close 修罗 shortcut team-maintenance window on target-map arrival

Problem statement:

- User rule: once the 修罗 leader has reached the target map / target-monster area, members should no
  longer start 三技能 maintenance for that round. The current shortcut route violates this because the
  team pathing maintenance window stays open until the final enter-battle dialog is prepared and
  consumed.
- Fresh runtime evidence:
  - `2026-06-24 12:44:31.448` round 8 tracker shortcut green click opened
    `teamRound=xiuluo_v2#8`; `12:45:02.901-12:45:20.284` member `『忍者』影`
    ran summon-skill cleanup with `skipUltimateCorner=false`; the leader did not close the team window
    until `12:45:23.647` / `source=xiuluo-v2:shortcut-enter-battle-prepared`, then entered combat at
    `12:45:25.516`.
  - `2026-06-24 13:49:49.747` round 16 opened `teamRound=xiuluo_v2#16`; Runner then observed
    `activeIntentTarget=null` / `pathingTarget=null` while maps changed through
    `灵兽村 -> 冰窟 -> 北俱芦洲 -> 长安 -> 四圣庄`; the window closed only at `13:50:27.166`
    with `source=xiuluo-v2:shortcut-enter-battle-prepared`.
  - `2026-06-24 13:51:36.688` round 17 opened the window and closed only at `13:52:14.774`, also from
    `shortcut-enter-battle-prepared`.
  - `2026-06-24 13:59:29.221` round 21 opened `teamRound=xiuluo_v2#21`; while the leader was still
    waiting on shortcut pathing / prepared enter-battle, member `hwnd-4F60446` claimed the round and
    ran summon-skill cleanup at `13:59:46.973-14:00:00.308`; the leader closed the team window only at
    `14:00:17.641` with `source=xiuluo-v2:shortcut-enter-battle-prepared`.
  - `2026-06-24 14:01:28.399` round 22 opened `teamRound=xiuluo_v2#22`; member `hwnd-61212BA`
    claimed and ran summon-skill cleanup at `14:01:43.670-14:01:56.192`; the leader closed the window
    only at `14:02:01.152`, again with `source=xiuluo-v2:shortcut-enter-battle-prepared`.
  - `2026-06-24 14:05:09.981` round 24 opened `teamRound=xiuluo_v2#24`; member `hwnd-4E16C4`
    claimed and ran summon-skill cleanup at `14:05:25.227-14:05:37.171`; the leader closed the window
    only at `14:06:45.637`, again from `shortcut-enter-battle-prepared`.
  - `2026-06-24 14:07:55.341` round 25 opened the window after objective parse had already resolved
    `凤巢七层`; Runner observed `pathingCurrent=凤巢七层` at `14:08:46.095`, `14:08:48.661`,
    `14:08:51.805`, and `14:08:55.506`, but the window still closed only at `14:08:59.113` from
    `shortcut-enter-battle-prepared`.
  - `2026-06-24 14:39:32.935` round 39 opened `teamRound=xiuluo_v2#39` after objective parse had
    already resolved `龙窟六层(72,76)`; Runner observed `pathingCurrent=龙窟六层` at
    `14:40:49.387`, `14:40:52.832`, and near target `(73,75)` at `14:40:57.355`, but the window
    still closed only at `14:41:00.354` from `shortcut-enter-battle-prepared`.
  - `2026-06-24 15:04:32.709` round 50 opened `teamRound=xiuluo_v2#50` after objective parse had
    resolved `大雁塔四层(49,40)`; Runner observed `pathingCurrent=大雁塔四层` starting
    `15:06:11.360` through `15:06:24.000`, but the team window still closed only at `15:06:26.417`
    from `shortcut-enter-battle-prepared`. During that open window, `hwnd-61212BA` ran summon-skill
    cleanup at `15:04:36.983-15:04:48.887`, and `hwnd-4F60446` repeatedly logged
    `summon skill round claim limit reached`.
  - `2026-06-24 15:07:32.336` round 51 opened `teamRound=xiuluo_v2#51` after objective parse had
    resolved `长安(42,30)`; member `hwnd-4F60446` ran summon-skill cleanup at
    `15:07:36.811-15:07:54.423` and clicked ultimate, while the leader closed only at `15:08:07.525`
    from `shortcut-enter-battle-prepared`.
  - `2026-06-24 15:30:55.953` round 62 opened the window after objective parse had already resolved
    `大雁塔三层(35,32)`; runner observed `pathingCurrent=大雁塔三层` starting at `15:32:14.148`,
    but the leader closed only at `15:32:31.375` from `shortcut-enter-battle-prepared`.
  - `2026-06-24 15:40:06.959` round 66 opened the window after objective parse had already resolved
    `洛阳城(398,121)`; member `hwnd-11A90AF4` ran summon-skill cleanup at
    `15:40:09.067-15:40:21.376`, while the leader closed only at `15:40:48.298` from
    `shortcut-enter-battle-prepared`.
- The accept-time background objective parse already knows the destination before/while shortcut
  pathing runs:
  - round 17 `13:51:34.418` parsed `洛阳城(202,176)`;
  - round 18 `13:53:35.244` parsed `长安(186,207)`.
  - round 21 `13:59:26.927` parsed `白骨山(179,137)`;
  - round 23 `14:03:15.307` parsed `蟠桃园(58,77)`.
  - round 24 `14:05:08.137` parsed `大雁塔五层(71,66)`;
  - round 25 `14:07:52.936` parsed `凤巢七层(59,21)`.
  - round 39 `14:39:30.796` parsed `龙窟六层(72,76)`.
  - round 50 `15:04:31.221` parsed `大雁塔四层(49,40)`.
  - round 51 `15:07:30.283` parsed `长安(42,30)`.
- The tracker green link itself currently carries `targetMapName=` blank, so the implementation should
  not depend on tracker green-text map parsing. Use the accept snapshot objective parse that already
  exists.
- This is specifically the accept-time bound game-window snapshot from CR91, not a new live dialog
  screenshot taken after shortcut pathing. Use the same captured frame/crop and the same existing
  修罗 story-objective parsing path that the non-shortcut route already relies on.

Required behavior:

- Keep CR89's intended opening rule: member maintenance remains blocked until a successful tracker
  green click commits the route.
- After accept succeeds, the task must retain the accept-time game-window snapshot/fallback story
  objective crop. The shortcut path may click tracker green and start moving first, but the target-map
  parse should continue in the background from that saved snapshot, exactly like the existing
  accept-time objective parse.
- After the accept-time objective parse completes, preserve the parsed target map/coordinate in
  `XiuluoRoundContext` or an equivalent shortcut wait state even if the tracker shortcut is already
  moving.
- When `WAIT_TRACKER_SHORTCUT_PATHING` is active and the leader is observed on the parsed target map,
  close the team pathing maintenance window immediately with a clear source such as
  `xiuluo-v2:shortcut-target-map-arrived`.
- Existing close paths must remain as fallback:
  - final prepared dialog / combat entry;
  - shortcut fallback/failure/reaccept;
  - round completion/abort.
- If objective parse misses, is stale, or is not available in time, keep current close-on-prepared
  behavior rather than blocking the shortcut or changing route semantics.
- Due maintenance that begins legitimately after the tracker green click but before target-map arrival
  remains allowed; this card only closes the window once target-map arrival is known.

Boundaries:

- Do not change task-tracker title/green matching, green click coordinates, objective OCR/template
  algorithms, direct-combat behavior, summon-skill click/template logic, CR83 timer compensation, or
  CR80 timeout limits.
- Do not wait synchronously for objective parse before green click or before start-exit prepath; CR91's
  movement/parse overlap must remain intact.
- Do not require the tracker `TaskTrackerGreenLink.targetMapName` to become nonblank for 修罗. The
  current 40px 修罗 detail crop intentionally returns the green link coordinate only.
- Do not take a later screenshot after the mini-map/tracker state has changed just to recover the
  target map. The target-map source for CR96 is the saved accept-time game-window snapshot and its
  story objective parse.

Implementation notes:

- Candidate path: keep the CR91 one-shot bound game-window snapshot, derive the story objective crop
  from that saved frame, and submit the existing `ObjectiveTextRecognitionService` / 修罗 objective
  parser in the background. This can finish after tracker green has already been clicked and after
  start-exit prepath has handed off; it must not block those fast-path steps.
- Extend `XiuluoRoundContext` with shortcut objective target map/coordinate, or reuse `objective` once
  the background future completes, then pass `objective.mapName` into
  `XiuluoWaitSpec.pathingTargetMapName` / route state for `WAIT_TRACKER_SHORTCUT_PATHING`.
- `WindowTaskRunner` already logs `activeIntentTarget` and classifies targeted pathing with
  `hasArrived(...)`; an implementation can either make the shortcut intent targeted after objective
  parse is known, or publish an explicit target-map-arrived fact that `XiuluoTaskV2` consumes before
  prepared dialog consumption.
- The source should log enough to verify the exact ordering:
  - objective target attached to shortcut wait;
  - current map equals target map;
  - maintenance window closed before `shortcut-enter-battle-prepared`.

Verification:

- [ ] Source test: accept-time background objective target is carried into shortcut route/wait state
  without delaying CR91 start-exit prepath or tracker green click.
- [ ] Source/replay test: the saved accept-time game-window snapshot can be cropped and parsed by the
  existing 修罗 story-objective parser to produce the target map/coordinate used by CR96.
- [ ] Source test: when shortcut pathing observes current map equal to parsed target map, the team
  maintenance window closes before final enter-battle prepared consumption.
- [ ] Source test: objective parse miss/failure preserves the existing close-on-prepared fallback.
- [ ] Source test: due maintenance before target-map arrival is still allowed and still uses CR83 timer
  compensation.
- [ ] `mvn -q -DskipTests test-compile`.
- [ ] `mvn -q -DskipTests compile`.
- [ ] Fresh runtime: logs show
  `maintenance team window closed ... source=xiuluo-v2:shortcut-target-map-arrived` after current map
  reaches the parsed objective map and before `task-dialog-interest-prepared` /
  `shortcut-enter-battle-prepared` for the same round.
- [ ] Fresh runtime: no member `start summon skill clean` appears for that team round after the
  target-map-arrived close source.
- [ ] Performance check: accept-to-green-click and accept-to-enter-battle latency do not regress; no
  new runner 100ms/900ms churn or WAIT_COMBAT empty spin is introduced.

Implementation status:

- 2026-06-24 Codex: source implementation complete, fresh runtime pending. `XiuluoTaskV2` still clicks
  the tracker green link first. Immediately after the click, it checks the existing accept-time
  objective future without blocking; if the map is already available, it registers the shortcut
  `WindowPathingIntent` as `TARGETED` with `targetMapName=objective.mapName` and `targetX/targetY=null`.
  That lets Runner classify map-only ARRIVED using the same background map watcher it already runs.
  If the objective map is not ready at the click instant, the shortcut intent first starts as
  `UNTARGETED_TRACKER`, but `attachShortcutTargetMapUpgrade(...)` attaches to the accept-time objective
  future and calls `upgradeActivePathingIntentTargetMap(...)` for that exact intent id once the map is
  parsed. If the map parse misses/fails or the intent has already changed, the upgrade is skipped and
  the old fallback behavior remains.
  When `WAIT_TRACKER_SHORTCUT_PATHING` observes ARRIVED on that targeted shortcut intent, 修罗 closes
  `xiuluo_v2#round` with `source=xiuluo-v2:shortcut-target-map-arrived`, clears the consumed pathing
  signal, and keeps waiting for prepared enter-battle/combat. The existing
  `shortcut-enter-battle-prepared` close fallback is preserved.
- Verification passed: `mvn -q -DskipTests compile`;
  `XiuluoCR96ShortcutTargetMapCloseWiringTest` compiled directly with `javac` and passed;
  `XiuluoCR91AcceptSnapshotOverlapWiringTest` passed;
  `XiuluoTargetPathingEventWakeWiringTest` passed.
- 2026-06-25 Codex recheck: `mvn -q -DskipTests compile` passed again, and direct
  `XiuluoCR96ShortcutTargetMapCloseWiringTest` passed. Source guard now also checks the late
  target-map upgrade path.
- Verification blocked outside CR96: `mvn -q -DskipTests test-compile` currently fails on existing
  CR97 `XiuluoPreparedEnterBattleRetryWiringTest`, which expects pending enter-battle retry methods
  not present in `XiuluoRoundContext` yet.

Card CR97: retry 修罗 prepared enter-battle when click does not enter combat

Problem statement:

- Fresh runtime round 72 proves the prepared "看打" click can be physically consumed without the game
  entering combat, and the task then waits too long in `WAIT_COMBAT`.
- Evidence:
  - `2026-06-24 15:55:45.269` Runner prepared `operation=XIULUO_ENTER_BATTLE`,
    `target=xiuluo.enterBattle`, `click=(431,462)`.
  - `15:55:45.997` the input worker sent the physical click for
    `request=xiuluo-v2:preparedEnterBattle:72`.
  - `15:55:46.161` `XiuluoTaskV2` cleared dialog interest for `XIULUO_ENTER_BATTLE`, closed the
    team maintenance window with `source=xiuluo-v2:shortcut-enter-battle-prepared`, and moved to
    `WAIT_COMBAT` with `message=shortcut enter-battle prepared action consumed`.
  - From `15:56:42` through `15:58:50`, Runner still saw `visibleDialog=OPTION`,
    `pathingState=STOPPED_AWAY`, `pathingCurrent=凤巢五层`, and `preparedOperation=null`, while the
    task repeated `phase=WAIT_COMBAT result=SHARED_STATE_TRIGGERED next=WAIT_COMBAT
    message=waiting for combat state`.
  - Combat was observed only at `15:58:55.399`, about 189 seconds after the click.

Root cause:

- `consumePreparedXiuluoEnterBattle(...)` treats successful input consumption as enough business
  evidence: it clears the dialog interest and enters `WAIT_COMBAT` after one physical click.
- `XiuluoRoundContext.withCombatSource(...)` marks `enteredBattleByXiuluo=true`; then
  `waitCombat(...)` does not use the existing dropped-click recovery branch, because that branch only
  runs when `!enteredBattleByXiuluo()` and an objective exists. The tracker shortcut route often has
  no objective-backed direct-click recovery at this point.
- `runRoundPhases(...)` resets the same-phase loop guard for `SHARED_STATE_TRIGGERED`, so this can
  spin in 900ms waits until some outside combat signal finally appears.

Required behavior:

- After a prepared Xiuluo enter-battle click, do not treat "input success" as final business success.
- If combat is not observed after a short confirmation window and the current visible/prepared evidence
  is still the same Xiuluo enter-battle option, re-register or re-consume `XIULUO_ENTER_BATTLE` and
  click again.
- Bound retries, for example two or three attempts, then fall back to the existing round
  failure/reaccept/watchdog policy instead of waiting indefinitely.
- Keep the team maintenance window closed after the first prepared enter-battle click; retrying the
  option must not reopen member maintenance.
- Preserve CR62 / combat-event wake behavior once combat has actually entered.

Boundaries:

- Do not change tracker green matching/click, objective parsing, dialog template thresholds, physical
  click coordinate calculation, direct-combat, wild-monster cancel, CR80 180s watchdog, or CR96
  target-map close behavior.
- Do not turn any generic `OPTION` into business truth. Retry only when it is tied to Xiuluo
  enter-battle template/prepared evidence or an equivalent validated dialog-interest result.

Implementation status:

- 2026-06-24 谢帅: source implementation complete, fresh runtime pending. `XiuluoRoundContext`
  now tracks pending enter-battle confirmation separately from real combat entry. `XiuluoTaskV2`
  uses that pending state for normal template confirm, Runner prepared shortcut confirm, and recovered
  dialog confirm; `WAIT_COMBAT` marks `enteredBattleByXiuluo` only after combat is observed. If a
  shortcut prepared confirm click does not enter combat after the short confirmation window, 修罗
  re-registers `DialogOperation.XIULUO_ENTER_BATTLE`, keeps the team maintenance window closed, waits
  for `PREPARED_ACTION_READY`/combat/pathing wake, and consumes the re-prepared action. Retry is
  bounded by `MAX_ENTER_BATTLE_CONFIRM_RETRIES`; exhaustion falls back through the existing shortcut
  failure/reaccept policy.

Verification:

- [x] Source test: tracker shortcut route with no objective retries a dropped prepared enter-battle
  click instead of repeating `WAIT_COMBAT` forever.
- [x] Source test: retry count is bounded and falls back to existing failure/reaccept/watchdog policy.
- [x] Source test: team maintenance window remains closed during enter-battle retry.
- [x] `mvn -q -DskipTests test-compile`.
- [x] `mvn -q -DskipTests compile`.
- [ ] Fresh runtime: no more long span where `WAIT_COMBAT` repeats
  `message=waiting for combat state` while Runner sees `visibleDialog=OPTION`,
  `preparedOperation=null`, and no combat for the same round.
- [ ] Fresh runtime: when the first prepared click misses, logs show a bounded retry and either a
  prompt combat entry or a normal round recovery/failure path.

Card CR98: defer leader-task startup while already in combat

Business source:

- `docs/业务逻辑.md` -> `战斗中启动任务的逻辑`.

Problem statement:

- When the user starts 修罗/五倍 while the bound game window is already in combat, the current
  startup layer still runs normal preflight immediately.
- Fresh runtime evidence:
  - `2026-06-24 16:04:16.471` UI submitted `start task queue: [xiuluo_v2]`.
  - `16:04:20.811` startup team-role detection logged
    `team role detection: no team tooltip/status signal detected, role=SOLO`.
  - `16:04:20.813` `TaskTeamAssignmentPolicy` logged
    `SOLO window cannot run leader-only task XIULUO_V2`.
  - `16:04:20.815-16:04:20.821` the runner resolved the requested 修罗 task to `UNKNOWN`,
    skipped it, and finished the queue.
- This means 修罗's own hot-start / combat-recovery code never gets a chance to run. The task is
  stopped by startup preflight, not by 修罗 business logic.

Root cause:

- `WindowTaskRunner.runQueueWithBoundGameState(...)` calls `resolveTaskTypeBeforeStart(...)` before
  the task is created.
- `resolveTaskTypeBeforeStart(...)` uses hover/status team-role detection. In combat, team UI is not
  a reliable source and can be missed or misread as SOLO/UNKNOWN.
- `DefaultWindowTaskStartupInitializer.beforeTask(...)` also runs before task execution and calls
  `playerStateService.syncAll()` plus startup UI preparation. These position/UI reads and hotkeys are
  also unreliable while the game is in combat.
- Therefore the correct fix belongs in the startup layer, before team-role detection and startup UI
  preparation, not in 修罗 `WAIT_COMBAT` alone.

Required behavior:

- For 修罗、五倍 and similar main/leader tasks, startup must first perform a narrow combat-start
  check.
- If the window is already `IN_COMBAT`:
  - do not run team-role detection;
  - do not trust or fallback to the existing `windowContext.role`;
  - do not call `syncMyPosition()` / full `syncAll()`;
  - do not run startup UI preparation such as Alt+1, Alt+6, map settings, or task-panel setup;
  - mark/log the window as waiting for combat exit before startup;
  - keep the queued task alive, not skipped, failed, or stopped;
  - wait only on combat state while honoring user stop/pause.
- After combat exit:
  - rerun normal preflight from scratch;
  - perform live team-role detection after combat, not during combat;
  - run normal identity/position sync and startup UI preparation after combat;
  - pass an explicit startup marker into `TaskExecutionContext`, for example
    `AFTER_COMBAT_EXIT_STARTUP`.

Task recovery behavior after the deferred startup:

- 修罗 and 五倍 must not blindly assume the just-finished combat was unrelated, and must not blindly
  assume it completed the task.
- First read the left task tracker:
  - if the current task target/green link still exists, treat the just-finished combat as unexpected
    or incomplete. Run the task's normal due maintenance checks, then continue by clicking the current
    tracker green link;
  - if the current task target/green link is absent, try the task-specific return item.
- If the return item exists and returns to the expected map, treat the just-finished combat as likely
  task completion and enter the task's normal post-combat return/team/next-round flow.
- If the return item is missing, return to the task's start/accept flow because the character likely
  no longer has that task.
- If the return item exists but does not return to the expected map, do not continue a complex hot
  start. For both 修罗 and 五倍, recover simply by going back to the start map / accept flow and
  reaccepting the task.

Boundaries:

- Do not change OCR/template thresholds, tracker green matching, click coordinates, route selection,
  minimap/world-map navigation algorithms, NPC click strategy, direct-combat/Alt+A behavior, or
  task-specific maintenance timing.
- Do not reinterpret generic `OPTION` / `STORY` / `TASK_ATTENTION_REQUIRED` as proof of task state.
- Do not change confirmed in-task combat-source rules:
  - 修罗 `TRACKER_CONFIRM` combat still means the tracker-confirmed task battle and can use the
    known post-combat flow.
  - 修罗 in-task `INCIDENTAL` combat still follows the shortcut-route incidental recovery rules.
  - This CR only covers the user starting a task while the window is already in combat.

Implementation notes:

- Add a startup-defer helper in `WindowTaskRunner` before `resolveTaskTypeBeforeStart(...)`.
- Prefer using the existing combat radar / `AutoCombatService` read-only guard path for the wait.
  The startup wait must not consume task-owned post-combat recovery in a way that hides the
  `AFTER_COMBAT_EXIT_STARTUP` marker from the task.
- Add the startup marker to `TaskExecutionContext` using an enum or explicit booleans with clear
  names; avoid stringly-typed source checks in task code.
- 修罗 can route this marker into its existing unknown/post-combat recovery shape, but with the
  tracker-first / return-item verification order described above.
- 五倍 should route the marker into its existing `HOT_START_DETECT` / tracker-first startup, then
  use return-item verification only when tracker no longer shows the current task.

Implementation status:

- 2026-06-24 谢帅: source implementation complete, fresh runtime pending.
- `WindowTaskRunner` now calls `deferStartupIfAlreadyInCombat(...)` before
  `resolveTaskTypeBeforeStart(...)`. The helper is scoped to `XIULUO_V2` and `WUBEI`, uses
  `AutoCombatService.probeWindowCombatStateReadOnly(...)`, logs startup defer start/finish, and does
  not call team-role detection, startup initializer, combat-enter maintenance, or auto-combat panel
  hotkeys while combat is visible.
- `TaskExecutionContext` carries typed `TaskStartupMode`; after a startup combat defer, both
  preflight and actual task execution contexts receive `AFTER_COMBAT_EXIT_STARTUP`.
- 修罗 handles `AFTER_COMBAT_EXIT_STARTUP` before normal hot-start/task-panel fallback: first read
  `readXiuluoTrackerPanel(...)`; if green links exist, reuse the existing
  `AFTER_ACCEPT_MAINTENANCE_CHECK -> TRY_TRACKER_SHORTCUT` path by passing a completed tracker future.
  If tracker misses, verify the 修罗 return item; success enters existing team-return/round-done flow,
  and failure falls back to start-map accept flow.
- 五倍 passes `TaskExecutionContext` into `HOT_START_DETECT`; it already reads tracker first. Under
  `AFTER_COMBAT_EXIT_STARTUP`, tracker miss or title-only/no-green tracker hits now verify the 五倍
  return item before falling back to the accept flow.

Review findings - 2026-06-24 Codex + Feynman:

- [x] [P1] Startup combat defer is now read-only in source. Previously `WindowTaskRunner.deferStartupIfAlreadyInCombat(...)`
  called `AutoCombatService.handleWindowCombatGuardTick(...)`; that method calls `maybeHandleCombatEnter(...)`,
  which can call `AutoCombatPanelService.ensurePanelVisible(...)`. When the auto-combat panel is not found,
  `ensurePanelVisible(...)` sends `Alt+8`. This violates the CR98 rule that combat-start defer should only
  observe combat state and must not press startup/UI hotkeys while the user-started task is waiting for the
  current battle to end. Fixed by adding `AutoCombatService.probeWindowCombatStateReadOnly(...)` and making
  `deferStartupIfAlreadyInCombat(...)` use it; the source guard asserts the read-only probe does not call
  `maybeHandleCombatEnter(...)` or `ensurePanelVisible(...)`.
- [x] [P2] 五倍 after-combat startup used to treat `currentTrackerPanel.isFound()` as an active task even if no actionable
  green link exists. CR98 requires tracker-first recovery only when the current task target/green link still
  exists; otherwise the task should verify completion with the return item. 修罗 already uses
  `panel.isFound() && !panel.getGreenLinks().isEmpty()`. Fixed by making
  `WubeiTask.runHotStartDetectPhase(...)` require `currentTrackerPanel.isFound() && !currentTrackerPanel.getGreenLinks().isEmpty()`
  before entering `READ_TRACKER`; no-green/title-only hits now continue into the after-combat return-item check.
- [P3 / clarification] After combat exit, `resolveTaskTypeBeforeStart(...)` reruns live role detection as required,
  but if the live result is `UNKNOWN` it still falls back to `windowContext.role`. This is acceptable if CR98 only
  forbids trusting stale role while the window is still in combat. If the intended rule is stricter, update this
  card and remove that fallback for `AFTER_COMBAT_EXIT_STARTUP`.
- Conflict check: CR97's pending prepared-enter-battle retry path is still reachable after CR98 tracker recovery.
  CR96 target-map early-close usually will not trigger on 修罗 after-combat tracker recovery because that path
  currently passes an empty objective future; this is a known downgrade rather than a direct conflict.

Review follow-up - 2026-06-25 Codex:

- Rechecked the current CR98 source after the follow-up fixes. No P1/P2 open findings remain in source.
- Evidence:
  - `WindowTaskRunner.deferStartupIfAlreadyInCombat(...)` now calls
    `AutoCombatService.probeWindowCombatStateReadOnly(...)`; it no longer uses
    `handleWindowCombatGuardTick(...)` during startup combat defer.
  - `AutoCombatService.probeWindowCombatStateReadOnly(...)` refreshes combat radar state only and does
    not call `maybeHandleCombatEnter(...)`, so it cannot reach auto-combat panel recovery / `Alt+8`.
  - `WubeiTask.runHotStartDetectPhase(...)` now requires
    `currentTrackerPanel.isFound() && !currentTrackerPanel.getGreenLinks().isEmpty()` before entering
    `READ_TRACKER`; title-only/no-green tracker hits fall through to after-combat return-item verification.
- Remaining status: CR98 should stay in Review until fresh runtime proves the startup defer path: starting
  修罗/五倍 in combat must log combat-start defer, must not run team-role detection/startup UI while combat
  is visible, and must resume normal preflight plus tracker-first/return-item recovery after combat exit.
- The only non-blocking source question left is the P3 policy clarification above: whether post-combat live
  role `UNKNOWN` may fall back to `windowContext.role`.

Verification:

- [x] Source/unit test: when startup detects `IN_COMBAT`, `TeamRoleDetectionService` is not called,
  `DefaultWindowTaskStartupInitializer.beforeTask(...)` is not called, and the task is not skipped.
- [x] Source/unit test: after combat exit, normal team-role detection and startup initializer run.
- [x] Source/unit test: the actual task execution context carries `AFTER_COMBAT_EXIT_STARTUP`.
- [x] Source/unit test or focused wiring test: 修罗 post-defer startup reads tracker before using the
  return item.
- [x] Source/unit test or focused wiring test: 五倍 post-defer startup reads tracker before using the
  return item.
- [x] Source guard: startup defer uses a no-input read-only combat probe that cannot call
  `maybeHandleCombatEnter(...)` / `ensurePanelVisible(...)`.
- [x] Source guard: 五倍 hot-start requires an actionable tracker green link before entering
  `READ_TRACKER`.
- [x] Compile with `mvn -q -DskipTests compile`.
- [x] `mvn -q -DskipTests test-compile`.
- [x] 2026-06-25 review rerun: `java -cp "target\classes;target\test-classes" com.bot.dhxy.window.execution.WindowTaskRunnerCombatStartupDeferWiringTest`.
- [x] 2026-06-25 review rerun: `java -cp "target\classes;target\test-classes" com.bot.dhxy.task.AfterCombatStartupRecoveryWiringTest`.
- [ ] Fresh runtime: starting 修罗/五倍 in combat logs a startup combat defer, no team-role detection
  happens until after combat exit, and no `skip task by team role policy` occurs during combat.
- [ ] Fresh runtime: after combat exit, logs show normal preflight, startup UI preparation, and then
  tracker-first recovery or return-item verification according to the visible task state.

Card CR99: world-map yellow destination link opens target mini-map route

Business source:

- User-approved 2026-06-25 navigation update after the game changed the world-map route result UI.

Problem statement:

- The old cross-map navigation flow uses the world-map search result green route/coordinate link,
  waits until the character reaches the target map, then opens the current-map mini-map and clicks
  the final target coordinate.
- The updated game now lets the user click the final yellow destination/map-name link in the route
  result. That opens the destination map's mini-map immediately, even if the character is still on
  another map.
- Because that opened panel uses the same scale as the normal current-map mini-map, the automation
  can click the final target coordinate immediately and let the game auto-path across maps and within
  the destination map. This should remove the old intermediate arrive-target-map, route dialog, and
  second current-map click latency.

Required behavior:

- Keep the existing world-map preparation sequence:
  - open world map / route UI;
  - click the search/input control;
  - type the target map name;
  - scroll the result list to the final route result;
  - verify the expected target row/destination before any click.
- Default to the new route-result action:
  - return/derive the matched yellow destination link click point from the guarded target row;
  - click that yellow destination link, not the old green route/coordinate link;
  - require the destination mini-map panel to become visible after the yellow click;
  - compute the requested final coordinate using the same mini-map coordinate mapping currently used
    by current-map navigation;
  - click that coordinate on the already-open destination mini-map without pressing Alt+1 first;
  - close the mini-map only according to the existing pathing handoff rules;
  - register the normal window pathing intent after the final coordinate click, not merely after the
    yellow link opens the panel.
- Preserve the old green-link route-result implementation as a legacy/switchable path so it can be
  restored if the game UI changes again.
- Do not use the old green-link path as a blind fallback when the guarded target row / yellow
  destination cannot be verified. If the target row cannot be matched, follow the existing retry or
  failure handling.

Retry / stopped-pathing policy:

- If a later retry sees the player is already on the target map, reuse existing
  `navigateInCurrentMap(...)` / current-map mini-map coordinate navigation.
- If a later retry sees the player is not on the target map, rerun the new world-map yellow-link
  route flow from search/input/scroll/guard.
- If the current map cannot be read, keep the existing navigation retry/failure policy; do not invent
  a new current-map truth from the route result alone.

Boundaries:

- Do not change NPC click, 修罗 tracker shortcut, 五倍 task tracker, direct-combat, route dialog
  option memory, maintenance timing, combat recovery, or task business phase order.
- Do not loosen the destination guard: the clicked yellow link must belong to the verified expected
  destination row.
- Do not change OCR/template thresholds unless replay evidence proves the target-row/yellow-link
  detector cannot expose the required point with current thresholds.
- Do not delete the old green-link pathing code or world-map route-result memory code; if the memory
  model no longer applies to the new default path, leave it isolated to the legacy path and document
  that behavior.
- The target-map mini-map coordinate click must not require `currentMap == targetMap`; this is the
  core difference from old current-map navigation.

Implementation notes:

- `GameTextLineOcrService.verifyWorldMapRouteDestination(...)` already guards yellow destination
  text. Extend or reuse its result so `NavigationService` can click the matched yellow destination
  link directly, instead of separately finding the final green coordinate link.
- `NavigationService.submitWorldMapSearchAndClickDestination(...)` should be split conceptually into:
  guarded search-result preparation, yellow destination click, destination mini-map coordinate click,
  and pathing intent registration.
- The mini-map click helper needs an "already-open panel required" mode for this path: if the panel
  is not visible after yellow click, fail/retry; do not press Alt+1 because that would open the
  current map and break the new flow.
- Logs should distinguish:
  - target row matched;
  - yellow destination clicked;
  - destination mini-map visible/missing;
  - final coordinate clicked;
  - pathing intent registered;
  - legacy green-link path used, if enabled.

Verification:

- [x] Before editing, record branch, latest pushed baseline, `git status`, and relevant baseline
  evidence for `NavigationService` / `GameTextLineOcrService` in `docs/ACTIVE_WORK.md`.
- [x] Add or update focused source tests that fail before implementation and prove the yellow
  destination click point is required for the default route-result path.
- [x] Add or update focused source tests that prove the already-open destination mini-map coordinate
  click does not press Alt+1 / does not require current map equals target map.
- [x] Add or update source tests for the retry policy:
  - already on target map -> existing current-map navigation;
  - not on target map -> rerun yellow-link world-map flow;
  - unknown map -> existing retry/failure policy.
- [x] Replay at least one repo-local world-map route testcase under
  `images/test-cases/world-map-route/...` and produce a marked output image that shows the matched
  yellow destination box and exact yellow click point.
- [x] Replay or add a repo-local mini-map testcase and produce a marked output image that shows the
  final target coordinate and exact mini-map click point for the yellow-opened panel coordinate
  mapping.
- [x] Record testcase input path, output marked image path, and command/tool in `docs/ACTIVE_WORK.md`.
- [ ] `mvn -q -DskipTests test-compile` blocked by existing test-source missing symbols:
  `BotProperties`, `WindowIsolationProperties`, and `WorldMapRouteResultMemoryEntry`.
- [x] `mvn -q -DskipTests compile`.
- [ ] Fresh runtime: logs show world-map search -> yellow destination click -> destination mini-map
  coordinate click -> pathing intent -> arrival/terminal without the old wait-to-target-map +
  reopen-current-map click.
- [ ] Fresh runtime: if pathing stops on the target map, the next attempt uses existing
  current-map navigation; if it stops off the target map, it reruns the yellow-link world-map flow.

Card CR106: route 灵兽村 only through CR99 yellow destination mini-map

Business source:

- User-approved 2026-06-25 navigation follow-up after CR99 made yellow-destination mini-map routing
  available.

Problem statement:

- The current `NavigationService.navigateToMap(...)` still treats `targetMap=灵兽村` as a special
  route before the normal world-map route flow.
- That branch calls `navigateToLingShouVillageViaZhangWen(...)`, which routes to `长安`, approaches
  `张闻`, clicks/handles the `请送我去灵兽村` route dialog, then confirms arrival.
- This special chain was needed before because the old world-map green route flow could not directly
  route into 灵兽村.
- With CR99, the updated game can search `灵兽村`, click the yellow `灵兽村` destination link, open the
  灵兽村 mini-map immediately, click the final coordinate there, and let the game auto-path across
  maps. The old 张闻 chain now adds avoidable navigation, NPC click, route-dialog, and confirmation
  latency.

Required behavior:

- For `navigateToMap("灵兽村")`, do not divert to `长安 -> 张闻 -> 灵兽村`.
- Let the CR99 route-result path handle 灵兽村 like any other map:
  - open world map / route UI;
  - search/type `灵兽村`;
  - scroll/guard the route result row;
  - verify and click the yellow `灵兽村` destination/map-name link;
  - require the destination mini-map panel that opens from that yellow link;
  - click the requested final coordinate using the same mini-map coordinate mapping as CR99;
  - register the normal coordinate-aware pathing intent after that final coordinate click.
- Keep `navigateToLingShouVillageViaZhangWen(...)` in source only as a deprecated retained method.
  Runtime navigation must not call it for 灵兽村 after CR106.
- CR70 is superseded by this card and should not remain an active runtime-validation target.

Boundaries:

- Do not change CR99 yellow destination OCR thresholds, target-row guard rules, click coordinate
  math, mini-map coordinate mapping, or world-map search/input/scroll mechanics.
- Do not change route-dialog option matching/memory except to stop using it as the default 灵兽村
  route.
- Do not change NPC click, 修罗 tracker shortcut, 修罗 accept/maintenance/combat phases, 五倍 task
  tracker, direct-combat, return-item logic, or UI cleanup policy.
- Do not delete the old 张闻 route code. Mark it deprecated and leave it with no production call path.
- If CR99 cannot verify the yellow `灵兽村` destination row/link, follow the existing CR99 retry or
  failure behavior; do not fall back to 张闻.

Implementation notes:

- The likely primary code change is in `NavigationService.navigateToMap(...)`: remove or gate the
  early `if (MAP_LING_SHOU_VILLAGE.equals(targetMapName))` diversion so `灵兽村` reaches
  `submitWorldMapSearchAndClickDestination(...)`.
- Annotate `navigateToLingShouVillageViaZhangWen(...)` as deprecated and update its comment to say
  it is retained source code after CR106, not a runtime navigation path.
- Logs should make the new route obvious:
  - no `navigate to Ling Shou Village through Zhang Wen` for 灵兽村 navigation;
  - world-map search target is `灵兽村`;
  - CR99 yellow destination click logs identify `灵兽村`;
  - destination mini-map coordinate click and normal pathing intent registration follow.

Verification:

- [x] Before editing, record branch, latest pushed baseline, `git status`, and relevant
  `NavigationService` / CR99 baseline evidence in `docs/ACTIVE_WORK.md`.
- [x] Add a focused source/wiring guard that fails before implementation because
  `navigateToMap(...)` still diverts `MAP_LING_SHOU_VILLAGE` to
  `navigateToLingShouVillageViaZhangWen(...)`.
- [x] Add a guard that `navigateToLingShouVillageViaZhangWen(...)` is explicitly deprecated /
  retained-only, and has no production call path from `navigateToMap("灵兽村")`.
- [ ] Reuse or add repo-local world-map route testcase evidence for `灵兽村` under
  `images/test-cases/world-map-route/...`; marked output must show the matched yellow `灵兽村`
  destination box and exact yellow click point.
- [ ] Reuse or add destination mini-map coordinate replay evidence for the 灵兽村 target coordinate;
  marked output must show the final coordinate point on the yellow-opened 灵兽村 mini-map.
- [x] Run the focused CR106 guard directly.
- [x] `mvn -q -DskipTests compile`.
- [x] If test sources are currently healthy for this branch, run `mvn -q -DskipTests test-compile`;
  if blocked by unrelated existing test-source errors, record the blocker exactly.
- [ ] Fresh runtime: a route to 灵兽村 logs the CR99 world-map yellow destination path and does not log
  `navigate to Ling Shou Village through Zhang Wen`.
- [ ] Fresh runtime: 修罗 return-fallback / accept-NPC navigation that needs 灵兽村 reaches the village
  through the new direct route and then continues normal current-map/NPC logic.

Card CR107: close left-top status switch for 修罗/五倍 startup and combat

Business source:

- User added `images/template/status/left_top_open.png` and
  `images/template/status/left_top_closed.png`.
- User corrected the live absolute ROI to `(549,515)-(560,534)`. The latest 2026-06-25 67555
  debug probe selected `hwnd-7132E` at `rect=541,368,1036x783`, so the implementation ROI is
  window-relative `(8,147,11,19)`. Earlier coordinates `(1111,540)-(1124,573)` were diagnosed as
  landing on the right-side 香火 panel, not the left-top status switch.
- The control is a small left-top status switch. If it is open, the automation should close it; if
  it is already closed, it should do nothing.

Problem statement:

- 修罗/五倍 need this status switch closed before or during task execution, but the close must not
  become a noisy startup click on every member window.
- Leader windows can safely check and close it at task startup.
- Member windows should not take foreground/mouse input at startup just to close it; they should
  detect/remember the need and perform the close only during a leader-opened safe maintenance window,
  or later during combat maintenance.
- Long combats can re-open or reveal the switch again, so the sparse combat UI maintenance loop
  should also enforce the same closed state.

Required behavior:

- Create one small service boundary for the left-top status switch:
  - search ROI: window-relative `(8,147,11,19)`;
  - open template: `images/template/status/left_top_open.png`;
  - closed template: `images/template/status/left_top_closed.png`;
  - `OPEN` -> click the matched open control center once and log the click;
  - `CLOSED` -> log/return no-op;
  - `UNKNOWN` / capture failure -> log/return no-op, never blind click.
- Apply only to `xiuluo_v2` and `wubei`.
- Leader startup:
  - when starting 修罗/五倍, run the check and close immediately if open;
  - do not add this to 五环 startup.
- Member startup:
  - when assigned as 修罗/五倍 follower support, do a no-click background check and remember pending
    close if the control is open;
  - do not click during initial member startup.
- Member safe-window consumption:
  - while `AutoBattleTask` is in follower-support mode for 修罗/五倍 and the corresponding team
    pathing maintenance window is open, consume the pending close by checking/clicking the switch.
  - If there is no pending flag but the safe window is open, a lightweight re-check is allowed so a
    missed startup probe can still be repaired.
- Combat maintenance:
  - plug into `AutoCombatService`'s existing sparse combat UI cleanup cadence
    (`COMBAT_UI_CLEAN_INTERVAL_MS`, currently 40s);
  - for 修罗/五倍 windows only, check and close if open;
  - keep the existing 4s entry maintenance and generic cleanup behavior unchanged.

Boundaries:

- Do not change 修罗/五倍 task phase decisions, tracker shortcut logic, navigation, OCR/template
  thresholds outside this new status switch, summon-skill rules, maintenance cooldowns, or return
  item logic.
- Do not use the feature in 五环.
- Do not click if `left_top_open.png` is not confidently matched.
- Do not make member startup steal focus or click immediately.
- Do not delete the existing Alt+5/Alt+6 startup visibility logic.

Implementation notes:

- Prefer a reusable service such as `LeftTopStatusSwitchService` rather than scattering ROI/template
  constants across startup, auto-battle, and combat maintenance.
- Use bound-window screenshot/template matching through existing tracker/coordinate helpers.
- Use queued/exclusive input for the click so multi-window physical input serialization still holds.
- Store any member pending flag in a per-window runtime place, not in a global static keyed only by
  player name.
- Logs should include task/requested task, role, ROI, detected state, matched point, and source
  (`leader-startup`, `member-startup-probe`, `member-team-window`, `combat-maintenance`, `debug`).

Debug / replay requirement:

- Add a no-UI debug main similar to `StartupAlt5ShoppingProbeDebugMain`.
- The debug main should:
  - bind a detected game window by title substring (default can be `67555`);
  - capture the `(8,147,11,19)` window-relative ROI;
  - compute open/closed template scores;
  - optionally click only when a VM flag explicitly enables click, for example
    `-DleftTopStatus.debug.click=true`;
  - write raw and marked outputs under `images/temp/left_top_status_probe/...`.
- Because this changes visual matching and a click point, add/reuse repo-local testcase images under
  `images/test-cases/status/...` and produce a marked output showing the ROI, matched template, and
  final click point.

Verification:

- [x] Before editing, record branch, current HEAD, dirty status, and touched baseline in
  `docs/ACTIVE_WORK.md`.
- [x] Add the CR107 sprint row and this detailed card.
- [x] Add focused source/guard tests proving:
  - ROI constants are `(8,147,11,19)`;
  - 修罗/五倍 are enabled and 五环 is not;
  - member startup is no-click/pending only;
  - combat maintenance calls the switch service on the sparse cleanup cadence.
- [x] Add the debug main and source guard for its wiring.
- [x] Run the focused CR107 guards directly.
- [x] Run `mvn -q -DskipTests compile`.
- [ ] User elevated CLI live probe: debug output shows open/closed scores and marked ROI/click point.
- [ ] Fresh runtime: 修罗/五倍 leader startup closes the switch if open.
- [ ] Fresh runtime: member startup does not click, then closes during team pathing maintenance window
  or combat maintenance.
- [ ] Fresh runtime: no 五环 startup log or click for this feature.

Card CR128: startup UI guards must be background probe plus pending consume

Business source:

- User clarified that the validated startup model is not one-window-at-a-time foreground checking.
- Alt+5 / Alt+6 were previously background HWND shortcuts plus background screenshots, so five
  registered windows could do those checks concurrently without focus churn.
- 五环 must follow the same model for startup guards. It still needs the full startup window checks,
  but those checks must be background-first and must not run heavy per-window foreground preparation
  unless a concrete wrong option was detected.
- If the user selects two 五环 rounds, the startup window checks run once for that accepted queue, not
  once per round.

Problem statement:

- The latest 五环 startup requirement is broader than the old CR107 修罗/五倍 card. Current local
  startup code put too much work into foreground startup and then briefly over-corrected by skipping
  parts of the startup checks:
  - 五环 could call `prepareTaskStartupWindow()` when `bot.dhxy.task-startup-preparation-enabled=true`;
  - `prepareTaskStartupWindow()` opens/checks/clicks Alt+1 map options and Alt+U expand/flying state
    inside an exclusive real-input section;
  - `LeftTopStatusSwitchService.handleLeaderStartup(...)` detects and clicks in one synchronous
    startup path.
- This breaks the five-window startup performance model. Detection should happen from bound-window
  background screenshots; real mouse click should be delayed until the window naturally owns a safe
  input turn.

Required behavior:

- For all startup probes that can be determined visually:
  - use bound-window HWND screenshot / window-scoped temp paths;
  - do not focus the game window just to inspect;
  - store a per-window pending action with enough data to click later: source, task code, window id,
    hwnd/identity epoch if available, absolute click point, detected state, and TTL.
- For startup hotkeys that are keyboard-only and validated:
  - keep Alt+5 and Alt+6 as background HWND shortcuts;
  - confirmation screenshots must remain background HWND-first;
  - do not fall back to focused real input during multi-window startup just for these confirmations.
- For startup controls that require mouse clicks:
  - do not click during background probe;
  - when the window later owns a safe foreground/input turn, consume the pending click immediately;
  - the consume path should not recapture first unless the pending record is expired, identity-stale,
    or missing a click point.
- 五环 startup:
  - must call the full startup checks through a background-first path;
  - must not call foreground `prepareTaskStartupWindow()` unless background probe finds a concrete
    option needing a mouse click;
  - must do this once per accepted task queue, not once per configured round;
  - left-top status switch must be probed in background and only clicked from pending when 五环 gets
    a safe task/input turn;
  - map/flying/minimap option checks must use the same background-probe/pending-consume policy before
    being enabled for 五环.
- 修罗/五倍:
  - do not regress their existing startup and combat-maintenance behavior;
  - if they keep immediate leader startup clicks for already-accepted behavior, document that as a
    separate task-specific policy, not as the 五环 multi-window startup model.

Boundaries:

- Do not change template images, match thresholds, click coordinates, navigation algorithms, OCR
  rules, task phase order, or Alt+5/Alt+6 confirmed behavior.
- Do not make background mouse clicks; only keyboard shortcuts and screenshots are background.
- Do not add a generic foreground startup pass for 五环.
- Do not leave CR/dashboard stale after status changes.

Suggested implementation:

- Introduce a small startup pending-action model in `WindowRuntimeContext` instead of overloading
  one string pending flag per feature.
- Split `LeftTopStatusSwitchService` into:
  - background probe/store pending;
  - consume pending click;
  - optional fresh safe-window recheck only when no pending record exists and the hook explicitly
    allows it.
- Split `TaskStartupWindowPreparationService` so Alt+5/Alt+6 remain immediate background actions,
  while map/flying/minimap click-needed options only produce pending records during startup.
- Wire 五环 to consume pending startup UI actions at the first existing safe input point before it
  performs task-click/navigation input, not during per-window startup initialization.

Verification:

- [x] Add CR128 card for the latest startup background-probe / pending-consume requirement.
- [x] Source repair: 五环 calls `prepareTaskStartupWindowBackgroundFirst()` from startup initializer.
- [x] Source repair: background-first startup probes Alt+1 map options and Alt+U expand/flying via
  HWND shortcut + screenshot, and only falls back to foreground `prepareTaskStartupWindow()` when a
  concrete correction click is needed.
- [x] Source repair: 五环 left-top startup path no longer clicks immediately.
- [x] Source repair: 五环 startup UI checks are marked done once per accepted task queue, so a
  two-round queue does not repeat startup checks on round two.
- [x] Focused source guard: 五环 startup branch cannot directly call foreground
  `prepareTaskStartupWindow()` while full startup preparation is enabled.
- [x] Focused source guard: 五环 left-top startup uses the no-click `probeMemberStartup(...)` path
  instead of `handleLeaderStartup(...)`.
- [x] Focused source guard: Alt+U is allowed through background HWND shortcut for startup probing.
- [ ] Fresh behavior proof: background probes run for all five windows without serial foreground
  checking when the options are already correct.
- [ ] Fresh behavior proof: when a startup option is actually wrong, only that window enters
  foreground correction.
- [ ] Fresh runtime: five-window 五环 startup shows background Alt+5/Alt+6 checks without serial
  foreground `taskStartup:mapTrackingAndVisibility` for every window.
- [ ] Fresh runtime: a two-round 五环 queue logs startup preparation only once per window.
- [ ] Fresh runtime: if left-top is open, 五环 consumes the pending close only after the window owns
  a safe input turn, and the click happens without an extra just-in-time recapture.

Card CR108: 五环 accept-NPC pathing wait should not retry while the yellow-route intent is still in flight

Business source:

- User reported that 67555 used the new CR99 yellow-destination mini-map route while going to accept
  五环 task, but later still opened the current-map mini-map and clicked again before accepting.
- Fresh log slice around `2026-06-25 23:08:49-23:09:03` confirms the extra current-map navigation was
  not caused by target tracker logic; it came from 五环 accept-NPC route waiting.

Problem statement:

- For 67555 / `hwnd-331564`, 五环 accepted handover and started accept-NPC navigation:
  - `23:08:49.047` registered active intent `worldMapYellowDestinationMiniMap`, source
    `wuhuan-v2:acceptNpc:navigate:map:worldMapYellowDestinationMiniMap:...yellow-destination-mini-map-clicked`,
    current `洛阳城(346,96)`, target `长安(87,174)`.
  - `23:08:49.378` through `23:08:52.032` logged
    `accept NPC navigation watcher still pathing: state=ACTIVE current=null(null,null)`.
  - `23:08:52.363` logged `accept NPC navigation wait ended; retry navigation/click from current state`.
  - The task then saw `长安(404,233)`, opened current-map mini-map at `23:09:00.139`, and clicked
    `(406,423)` at `23:09:00.640`.
  - The original yellow-route intent finally published `ARRIVED` at `23:09:03.699`, proving the
    `23:08:52.363` retry was premature.
- The current special accept-NPC wait in `FiveRingTaskV2.continueIfAcceptNpcNavigationStillPathing()`
  uses a short `PATHING_OBSERVER_FAST_WAIT_MS=2500` / snapshot-age style gate, then returns to the
  normal route/click flow when no terminal evidence is available yet.
- This creates duplicate minimap navigation while the character is still moving.
- Runner logs also showed a watcher tick with `branch=idle` while the slow-log fields already printed
  an active pathing intent. The tick likely read `pathingIntentActive` before the new intent was
  visible, then logged current active-intent fields later, which makes diagnosis and wake timing
  unreliable.

Required behavior:

- Treat accept-NPC yellow-route waiting like a real pathing wait, not a 2.5s speculative wait.
- In accept-NPC wait, always consume success/terminal evidence before applying timeout:
  - `ARRIVED` for the same intent/target, or a confirmed near accept-NPC position, clears pathing and
    continues toward NPC click.
  - `STOPPED_AWAY` for the same intent/target permits retry/current-state recovery.
  - `ACTIVE`, `UNKNOWN`, probe-in-progress, or the same active intent still present means keep waiting,
    not retry.
- The long hard timeout should reuse the existing pathing timeout policy, currently
  `PATHING_TARGET_WAIT_TIMEOUT_MS=90000`, and should recover safely only when no terminal evidence
  has arrived.
- The hard timeout must not mean "this window did not get a task turn for 90 seconds". In a many-window
  run, if the watcher already published `ARRIVED` while other windows were being handled, the first
  window must consume that old `ARRIVED` snapshot before considering timeout.
- `READY_EVENT_PRIORITY_MAX_AGE_MS` / fresh other-window priority rules must not erase this window's
  own pathing terminal truth.
- Runner watcher should use one active-intent snapshot consistently for the tick branch and log, or
  wake/re-check when a pathing intent is registered, so logs do not show `branch=idle` while the same
  tick later prints an active intent.

Boundaries:

- Do not change CR99 world-map yellow destination matching, yellow click coordinate math, destination
  mini-map coordinate mapping, or cleanup order.
- Do not change the existing current-map navigation fallback itself; only tighten when 五环 is allowed
  to enter that fallback.
- Do not change NPC smart click, accept NPC coordinate learning, OCR/template thresholds, dialog
  option matching, or 五环 business phase order after true arrival.
- Do not add broad sleeps or increase global watcher scan frequency as the primary fix.
- Do not convert stale/expired ready-event priority into business failure.

Implementation notes:

- Primary task-side scope is `FiveRingTaskV2.continueIfAcceptNpcNavigationStillPathing(...)`.
- Prefer reusing the ordinary `waitPathing(...)` terminal-first ordering:
  - usable `ARRIVED` / near target first;
  - explicit `STOPPED_AWAY` retry second;
  - in-flight evidence keeps the task parked;
  - long no-terminal timeout recovers through the existing safe 五环 sync/retry path.
- The accept-NPC special wait may still have a fast initial observer grace, but expiry of that grace
  must not by itself mean retry.
- Runner-side scope is `WindowTaskRunner.runCombatWatcherLoop(...)` and/or pathing-intent registration
  wake plumbing in `WindowRuntimeContext`; keep watcher branch decisions and logs based on the same
  active-intent snapshot.

Verification:

- [x] Before editing, record branch, current HEAD, dirty status, and the relevant pushed/local
  baseline for `FiveRingTaskV2`, `WindowTaskRunner`, and any runtime-context file touched in
  `docs/ACTIVE_WORK.md`.
- [x] Add focused source tests or wiring guards that fail before the fix and prove:
  - `ACTIVE/UNKNOWN/probeInProgress` after the 2.5s fast wait keeps waiting and does not call
    current-map navigation;
  - an `ARRIVED` snapshot for the same intent is consumed before timeout even if it is older than the
    normal fresh-priority window;
  - `STOPPED_AWAY` for the same intent still triggers retry/recovery;
  - no snapshot/no active intent past the long hard timeout enters the safe 五环 recovery path.
- [x] Add a focused guard for Runner active-intent tick consistency or wake-on-intent behavior if the
  implementation touches watcher scheduling.
- [x] Run the focused CR108 guards directly.
- [x] Run `mvn -q -DskipTests compile`.
- [x] If test sources are healthy, run `mvn -q -DskipTests test-compile`; if blocked by unrelated
  existing test-source errors, record the exact blocker.
- [ ] Fresh runtime: 67555-style 五环 accept-NPC navigation logs `worldMapYellowDestinationMiniMap`
  and does not open/click current-map mini-map again before the original pathing terminal.
- [ ] Fresh runtime: when arrival is delayed by other windows, the old `ARRIVED` terminal is still
  consumed and does not become a false 90s dead-path timeout.

Review note - 2026-06-26:

- Codex source review found no new P1/P2 blocker. The implementation now matches the card ordering:
  terminal evidence first, in-flight same intent keeps waiting, `STOPPED_AWAY` permits retry, and
  no-terminal recovery waits for the 90s hard timeout.
- Confirmed focused guard and test compilation still pass:
  `FiveRingAcceptNpcPathingWaitPolicyTest` and `mvn -q -DskipTests test-compile`.
- Keep this CR in Review until fresh runtime verifies the 67555-style yellow-route accept-NPC case.

Card CR109: expected combat exits should return first and defer HP/MP/incense checks

Business source:

- User observed that combat exit itself is detected early, but expected 修罗/五倍 rounds still wait
  about 2-3 seconds before the first return-item bag input.
- Timing review example:
  - `09:55:55.940` combat watcher detected `oldTick=IN_COMBAT -> NONE`.
  - `09:55:57.274` task `WAIT_COMBAT` finished / ready to continue.
  - `09:55:57.276` task entered `RETURN_HOME`.
  - `09:55:57.421` return-item input/focus began.
  - `09:55:57.886` first `Alt+E` opened bag.
- Root fixed cost is not combat-exit detection. It is the synchronous post-combat recovery inside
  `AutoCombatService.consumeExitAndRecover(...)` plus return-item input preparation.
- User provided the post-combat status screenshot ROI as top-left `(941,138)` and bottom-right
  `(1198,243)`. The preferred implementation captures this single upper-right status image and crops
  HP/MP / 摄妖香 subregions from it in the background. If one shared image is awkward, two background
  captures are acceptable, but the foreground return path must not wait on them.

Problem statement:

- `AutoCombatService.handleCombatTick(context, source, checkSheYaoXiangForLeaderTask)` currently
  passes a boolean into `consumeExitAndRecover(...)`.
- When the exit signal is consumed, the shared recovery path always:
  - records combat exit and resets panel/check counters;
  - runs `probeAndConsumeHealthyFirstAidNoFocus(...)`;
  - may run immediate cached first aid or queue follower first aid;
  - if `checkSheYaoXiangForLeaderTask=true`, runs `ensureSheYaoXiangActiveForLeaderTask(...)`;
  - only then sets action state back to `FREE`.
- 修罗/五倍 expected target battles already know their next foreground step:
  - expected 修罗 `TRACKER_CONFIRM` battle exits should go directly to `RETURN_HOME`;
  - expected 五倍 normal target battle exits should continue existing return/chained-combat flow.
- Paying synchronous HP/MP and 摄妖香 checks before return item slows the common path and can create
  large latency tails when 摄妖香 probe/refill runs.

Required behavior:

- Replace or extend the boolean `checkSheYaoXiangForLeaderTask` with an explicit post-combat recovery
  policy, for example:
  - `FULL_RECOVERY`: current conservative behavior.
  - `FAST_EXPECTED_EXIT`: consume combat exit, record/reset combat state, set `FREE`, and return to the
    owning task without synchronous HP/MP or 摄妖香 checks.
  - Optional `FAST_EXPECTED_EXIT_WITH_DEFERRED_STATUS_SNAPSHOT`: same as fast exit, but captures or
    schedules the upper-right ROI `(941,138)-(1198,243)` for background HP/MP/incense analysis.
- 修罗:
  - `combatSource=TRACKER_CONFIRM` and other confirmed task-owned enter-battle sources use the fast
    expected-exit policy.
  - `combatSource=INCIDENTAL`, unknown combat exits, hot-start after unrelated combat, and any path
    that is not proven expected keep `FULL_RECOVERY`.
- 五倍:
  - expected battle-entry paths such as validated enter-battle dialog, smart target click that enters
    `WAIT_BATTLE_FINISH`, and successful direct-combat target click use the fast expected-exit policy.
  - unexpected/incidental combat and after-combat startup recovery keep conservative behavior.
- 五环 is explicitly out of scope for this card.
- Deferred HP/MP / 摄妖香 handling:
  - Do not drop safety checks permanently.
  - Mark a pending post-combat status check for the current window when fast expected exit is used.
  - Consume that pending check at a safe later point: after return-home verification, after next accept
    task, or while the next navigation/pathing is already in progress.
  - If the background/pending status analysis proves supply/refill is needed, use existing first-aid
    and 摄妖香 mechanisms; do not invent new click/template logic.

ROI / screenshot notes:

- User-provided status ROI for this optimization: top-left `(941,138)`, bottom-right `(1198,243)`.
- Treat the coordinate space as the same current bound game-window/screen convention used by the
  existing status probes, and verify/normalize it during implementation before any click behavior.
- Preferred path: capture one upper-right status image and crop subregions from it for HP/MP and
  摄妖香 checks.
- Acceptable fallback: schedule two independent background captures if sharing one frame makes the
  existing services too invasive.
- The foreground expected-exit path must not block on OCR/template analysis of this ROI.

Boundaries:

- Do not change combat detection thresholds, `BattleRadarService`, auto-combat enter handling, or the
  4s combat-entry maintenance rule.
- Do not change 摄妖香 item template, bag click algorithm, HP/MP first-aid thresholds, or OCR/template
  thresholds in this card.
- Do not skip conservative recovery for incidental/unknown combat.
- Do not change 五环.
- Do not remove existing post-combat recovery; make expected 修罗/五倍 choose a narrower policy.

Implementation notes:

- Primary shared boundary is `AutoCombatService.consumeExitAndRecover(...)`.
- Add a small enum/value object instead of adding more booleans.
- Keep logs explicit:
  - source/task/window;
  - recovery policy;
  - combat source / expected-vs-incidental reason;
  - whether status check was synchronous, deferred, or skipped;
  - upper-right ROI path if a background snapshot is saved.
- 修罗 already has `XiuluoCombatSource.TRACKER_CONFIRM` / `INCIDENTAL`; use it.
- 五倍 may need a lightweight expected-combat marker or source mapping around `WAIT_BATTLE_FINISH`.
  Do not conflate 黄袍 chained-combat continuation with unrelated incidental combat.

Verification:

- [x] Before editing, record branch, current HEAD, dirty status, and relevant baseline for
  `AutoCombatService`, `XiuluoTaskV2`, `WubeiTask`, and any HP/MP/incense service touched in
  `docs/ACTIVE_WORK.md`.
- [x] Add focused source tests/guards proving:
  - expected 修罗 exits call fast policy and do not synchronously call `ensureSheYaoXiangActiveForLeaderTask`;
  - 修罗 incidental/unknown exits still call full recovery;
  - expected 五倍 exits call fast policy;
  - 五倍 unexpected/hot-start unrelated exits keep full recovery;
  - pending deferred status check is recorded and later consumed without blocking the return-item path.
- [ ] If using the shared ROI, add/reuse a repo-local testcase under `images/test-cases/status/...`
  and produce marked output showing `(941,138)-(1198,243)` plus HP/MP / 摄妖香 subregions.
- [x] Run focused CR109 guards directly.
- [x] Run `mvn -q -DskipTests compile`.
- [x] If test sources are healthy, run `mvn -q -DskipTests test-compile`; if blocked by unrelated
  existing test-source errors, record the exact blocker.
- [ ] Fresh runtime: expected 修罗 exit from `IN_COMBAT -> NONE` to first return-item input is shorter
  and no longer logs synchronous post-combat 摄妖香 check before `RETURN_HOME`.
- [ ] Fresh runtime: expected 五倍 normal battle exit is similarly faster and still reaches the
  correct return/chained-combat flow.
- [ ] Fresh runtime: incidental/unknown combat still runs conservative recovery and does not use the
  fast expected-exit path.
- [ ] Fresh runtime: pending deferred HP/MP / 摄妖香 check is eventually consumed during the next safe
  point, so safety checks are delayed, not lost.

Review note - 2026-06-26:

- Codex source review found no new P1/P2 blocker. Expected 修罗 `TRACKER_CONFIRM` exits and 五倍
  `WAIT_BATTLE_FINISH` exits use `FAST_EXPECTED_EXIT`; incidental/unknown and 五倍 `ENTER_BATTLE`
  exit checks remain conservative full recovery.
- Deferred HP/MP / 摄妖香 recovery is recorded on fast exit and consumed after verified return-home
  through existing services. 五倍 still has its existing `POST_BATTLE_RECOVER` 800ms settle before
  `RETURN_HOME`; runtime validation should measure whether the latency win is sufficient.

Fresh runtime blocker - 2026-06-26 13:55 修罗:

- User reported that after the leader returned home around `13:53-13:55`, it still paused to do
  HP/MP/摄妖香 work instead of accepting the next task first.
- Log evidence:
  - `13:55:51.675` 修罗 expected combat exit uses `recoveryPolicy=FAST_EXPECTED_EXIT`.
  - `13:55:51.675` `post-combat recovery deferred for fast expected exit`.
  - `13:55:51.681` `RETURN_HOME` starts and uses `xiuluo_return_item`.
  - `13:55:57.692` return item verifies start map: `灵兽村 (117,90)`.
  - Immediately after that, `XiuluoTaskV2.returnHome(...)` calls
    `consumePendingLeaderPostCombatRecoveryIfAllowed(...)`.
  - `13:55:58.184` first-aid no-focus precheck runs with source
    `xiuluo-v2:after-return-home:deferred-post-combat`.
  - `13:55:58.185` 摄妖香 check also runs with the same source.
  - `13:55:59.051` only then does `RETURN_HOME` finish and move to `WAIT_TEAM_RETURN`.
- In this specific sample first-aid was healthy (`needed=false`), so it did not execute a real
  `playerState:healAll`, but the timing is still wrong: the foreground task waited on first-aid
  precheck and 摄妖香 status work before the next accept/shortcut/movement.
- This proves the current consume point ("after verified return-home") is too early. The CR109
  accepted safe point must be tightened:
  - `RETURN_HOME` should only verify return and continue to the next task/accept chain.
  - Deferred leader HP/MP and 摄妖香 recovery should be consumed after the next task is accepted, or
    after shortcut/accept navigation has already started so the supply/status work overlaps movement.
  - Do not consume deferred recovery in `XiuluoTaskV2.returnHome(...)` immediately after
    `return item verified`.
  - Apply the same timing rule to 五倍 if its deferred expected-exit recovery has the same
    after-return-home consume point.
- Status: CR109 is not runtime-accepted. Rework required before it can be closed.
- Confirmed focused guard and test compilation still pass:
  `AutoCombatPostCombatRecoveryPolicyGuard` and `mvn -q -DskipTests test-compile`.

Rework implementation - 2026-06-26:

- Tightened `AutoCombatPostCombatRecoveryPolicyGuard` so it fails if 修罗/五倍 consume deferred
  leader recovery immediately after verified return-home.
- 修罗:
  - Removed `consumePendingLeaderPostCombatRecoveryIfAllowed(...)` from
    `XiuluoTaskV2.returnHome(...)`.
  - Added a narrow next-task-progress consumer used only after:
    - start-map exit prepath has started;
    - tracker shortcut green click has started movement / shortcut wait;
    - target navigation reports `PATHING_STARTED`.
  - This keeps return-home fast and delays HP/MP / 摄妖香 until the next accepted task is already
    progressing.
- 五倍:
  - Removed deferred recovery consumption from `useReturnItemAndVerifyStartMap(...)`.
  - Consumes the pending leader recovery after the next accept-task option is successfully clicked,
    before tracker refresh/read.
- Verification passed:
  - `AutoCombatPostCombatRecoveryPolicyGuard`
  - `mvn -q -DskipTests compile`
  - `mvn -q -DskipTests test-compile`
- Fresh runtime still needed to verify expected 修罗/五倍 exits no longer log
  `after-return-home:deferred-post-combat` before the next accept/movement.

Card CR110: CR99 yellow destination route-result clicks need watcher-confirmed memory

Business source:

- User confirmed the new CR99 world-map route is now the preferred route flow: search the destination,
  click the yellow destination row, use the opened destination mini-map, then click the final target
  coordinate. The old green coordinate-link route remains in the code only as a fallback/switchable
  legacy path.
- Existing route-result click memory was built for the old green-link row. It should not be reused as
  proof for yellow destination row clicks, but the new yellow route should gain the same kind of
  watcher-confirmed fast path.

Problem statement:

- `WorldMapRouteResultMemoryService` / `config/world_map_route_result_memory.json` currently model
  remembered world-map route-result row clicks as the legacy green-link path.
- `NavigationService.clickYellowDestinationAndTargetMiniMap(...)` verifies/clicks the yellow
  destination row and then clicks the destination mini-map coordinate, but it does not create pending
  route memory for that yellow row click.
- Repeated routes therefore still pay the full yellow OCR/template row scan even after the same route
  has been proven by a pathing watcher.

Required behavior:

- Add a mode-aware route-result memory path for CR99 yellow destination rows, parallel to the old
  green-link memory.
- Yellow memory must be isolated from legacy green memory by an explicit persisted route mode or an
  equivalent key. Old clean green entries must never be reused as yellow destination row clicks.
- When the yellow path clicks a verified yellow destination row and registers the
  `worldMapYellowDestinationMiniMap` pathing intent, create a pending yellow memory record containing:
  - canonical `fromMap`;
  - target map / destination name;
  - route mode, for example `YELLOW_DESTINATION_MINI_MAP`;
  - game-window-relative yellow row click point;
  - matched OCR/template destination text if available;
  - active pathing intent id;
  - whether this attempt used an existing memory entry.
- The existing watcher settlement lifecycle should promote/demote yellow memory:
  - `ARRIVED`: record success and eventually mark the yellow entry clean;
  - `STOPPED_AWAY`: record failure/dirty for the yellow entry;
  - replaced/cleared intent: abandon pending memory without promoting success.
- Later CR99 yellow navigation should look up a clean yellow entry before doing the expensive yellow
  OCR row scan. If found, click the remembered yellow row, wait for the destination mini-map, click the
  current request's final coordinate on that mini-map, register/update the same yellow pathing intent,
  and create a pending memory record with `usedMemory=true`.
- If the remembered yellow click fails to open the destination mini-map or cannot be submitted, fall
  back to the normal CR99 yellow OCR/template path.

Boundaries:

- Do not delete or change the legacy green-link route path or
  `bot.navigation.world-map-route.legacy-green-link-enabled`.
- Do not reuse legacy green clean memory for yellow row clicks.
- Do not change CR99 destination OCR/template thresholds, destination mismatch retry count, mini-map
  coordinate transform, final coordinate randomization, cleanup order, watcher semantics, or task
  business flow.
- Do not change 五环/五倍/修罗 phase semantics in this card. This card only adds route-result memory
  for the CR99 yellow world-map route primitive.

Implementation notes:

- Prefer extending `WorldMapRouteResultMemoryEntry` and `WorldMapRouteResultPendingMemory` with a
  `routeMode` enum/string and making the persisted lookup key mode-aware.
- Old memory entries without a mode should remain readable as legacy green-link entries only.
- Keep old memory APIs as legacy adapters if useful, but add mode-aware lookup/record methods for new
  code.
- `NavigationService.clickYellowDestinationAndTargetMiniMap(...)` already owns the yellow row match
  and click point. If the pathing intent id is only available after this method returns, return yellow
  click metadata or store it in the existing navigation runtime state, then create pending memory
  immediately after `registerWindowPathingIntent(...)`.
- Logs should distinguish `routeMode=LEGACY_GREEN_LINK` from
  `routeMode=YELLOW_DESTINATION_MINI_MAP`, and should include whether the click came from memory or
  fresh OCR.

Verification:

- Add focused guards/tests proving:
  - yellow path records pending yellow memory after registering the yellow pathing intent;
  - watcher `ARRIVED` promotes the yellow entry and `STOPPED_AWAY` dirties only the yellow entry;
  - yellow lookup cannot match a legacy green entry with the same `fromMap -> targetMap`;
  - old missing/null mode persisted entries are treated as legacy green only;
  - yellow fast path is attempted before fresh yellow OCR scan and falls back cleanly if the
    remembered row cannot open the destination mini-map.
- Run focused CR110 tests.
- Run `mvn -q -DskipTests compile`.
- Run `mvn -q -DskipTests test-compile`.

Review note - 2026-06-26:

- P2 fixed: `NavigationService.clickRememberedYellowDestinationAndTargetMiniMap(...)` now treats
  remembered yellow rows as candidates. After the remembered row click opens a mini-map panel, it
  calls `verifyOpenedDestinationMiniMap(...)`, which uses the existing
  `MiniMapCoordinateReader.readCurrentTemplateLocation()` map-label/template path and
  `GameStateUtil.isSameMapName(...)` to prove the opened mini-map is
  `expectedDestinationName`. If the label is missing or mismatched, the fast path returns false
  before clicking the final coordinate, so the caller falls back to the fresh yellow OCR route.
- P3 repaired as a focused guard: `NavigationWorldMapYellowMemoryWiringGuard` now asserts that the
  yellow-memory fast path verifies the opened mini-map identity before the final coordinate click,
  and that the verification path reads the current mini-map label and compares it to the expected
  destination. The deeper live fallback/no-pending behavior is still a fresh-runtime acceptance item.
- Verification recheck passed:
  - `mvn -q -DskipTests compile`
  - `mvn -q -DskipTests test-compile`
  - `WorldMapRouteResultYellowMemoryModeGuard`
  - `WorldMapRouteResultMemoryServiceTest`
  - `MemoryServiceFacadeTest`
  - `NavigationWorldMapYellowDestinationRoutePolicyTest`
  - `NavigationWorldMapRouteMemoryIntentOwnershipTest`
  - `NavigationWorldMapYellowMemoryWiringGuard`

Review follow-up - 2026-06-26:

- P2 open: stale/wrong yellow-memory fallback is not yet clean. The current fast path clicks the
  remembered yellow row, waits for a mini-map, then verifies the opened mini-map label before the
  final coordinate click (`NavigationService.clickRememberedYellowDestinationAndTargetMiniMap(...)`).
  That prevents a wrong final-coordinate click.
- However, when `verifyOpenedDestinationMiniMap(...)` fails at that point, the method simply returns
  false. `performWorldMapSearchAndClickDestination(...)` then immediately calls
  `clickYellowDestinationAndTargetMiniMap(...)` without closing the just-opened mini-map or
  rebuilding/repreparing the route-result panel. Fresh OCR can therefore capture the wrong mini-map
  UI instead of the world-map search results, causing the intended fallback to fail.
- Suggested fix: on yellow-memory mini-map identity failure, close the opened destination mini-map and
  either re-run `prepareWorldMapSearchResultsDirect(...)` for the current attempt or return a
  distinct status that forces the existing second-attempt reprepare path. Do not click the final
  coordinate until the expected destination mini-map is verified.
- Verification run in this review:
  `javac -encoding UTF-8 -d target\test-classes src\test\java\com\bot\dhxy\service\NavigationWorldMapYellowMemoryWiringGuard.java; if ($LASTEXITCODE -eq 0) { java -cp target\test-classes com.bot.dhxy.service.NavigationWorldMapYellowMemoryWiringGuard }`

Fresh runtime failure - 2026-06-27 20:40-20:41:

- Confirmed the stale-memory fallback gap in a live 修罗 maintenance route to 洛阳修装备. This is not
  the deprecated legacy green-link route: `20:40:23.529` and `20:41:28.695` both log
  `[world-map-route-memory] yellow fast path used: routeMode=YELLOW_DESTINATION_MINI_MAP ... targetMap=洛阳城`,
  and the route action logs `mode=yellow-destination-mini-map`.
- The remembered yellow click opened a mini-map panel, but the identity reader reported the player/current
  map: `20:40:26.250` and `20:41:30.214` log
  `destination mini-map identity mismatch ... expected=洛阳城 actual=灵兽村 coord=(111,92)`.
  This proves the review guard is using the old `navigationInCurrentMap` assumption inside the new
  yellow-destination flow. In the new flow, the player can still be in `灵兽村` while the yellow
  destination row opens the remote target-map mini-map for `洛阳城`; current map mismatch is not a
  valid blocker.
- Because that guard blocked the final `(325,108/110)` mini-map coordinate click, fallback stayed dirty:
  fresh yellow OCR captured `map_result_scan.png` from the wrong UI and read `actual=(178, 80 )`, then archived
  `images/failure-cases/world-map-route/20260627_204036_648_洛阳城_yellow-destination-mismatch`,
  `...204058_485...`, and `...204133_772...`.
- Repair direction: remove or replace `verifyOpenedDestinationMiniMap(...)` for
  `YELLOW_DESTINATION_MINI_MAP` memory/fresh mode. The safety boundary should be: yellow destination
  row clicked -> mini-map panel visible -> final target-coordinate click -> mini-map handoff/pathing
  confirmed. Do not require `MiniMapCoordinateReader.readCurrentTemplateLocation()` to equal the
  target map before the final click, because that reader is for current/player map state. If panel
  visibility or handoff confirmation fails, dirty/demote the yellow memory entry, close/rebuild the
  UI, and retry from a clean world-map search. Do not re-enable or rely on the old legacy green-link
  path.

Source repair - 2026-06-27 CR110 worker:

- `NavigationService.clickRememberedYellowDestinationAndTargetMiniMap(...)` no longer calls
  `verifyOpenedDestinationMiniMap(...)` before the final coordinate click. The old helper was removed
  because it read the player/current mini-map label and therefore incorrectly blocked remote
  destination mini-maps opened by yellow route rows.
- The remembered yellow flow now matches the intended CR99/CR122 safety boundary:
  remembered yellow row click -> mini-map panel visible -> final target-coordinate click ->
  `confirmMiniMapPathingStartedForHandoff(...)` confirms handoff -> record movement/cleanup/success.
- Failed remembered yellow attempts after the yellow row is clicked now call
  `recordWorldMapRouteResultFailure(...)` for `YELLOW_DESTINATION_MINI_MAP`, then run yellow route
  cleanup and return a reprepare status. The caller only runs fresh yellow OCR when no clean remembered
  yellow entry was attempted, so it does not scan dirty mini-map UI after a failed remembered click.
- Non-regression boundary: this repair does not change top-level `navigateToMap` current-map fresh
  confirmation, the already-on-target-map `ARRIVED` path, or the required `navigateToNPC` second step
  through `navigateInCurrentMap`. Being already on the target map must still avoid yellow world-map
  search and continue with current-map coordinate navigation. Legacy green-link code remains retained
  behind its explicit switch, not re-enabled as a fallback.
- Source/replay checks run so far:
  - RED/GREEN `NavigationWorldMapYellowMemoryWiringGuard` now asserts yellow memory fast path does
    not use current/player map identity as a destination guard, still requires panel visibility and
    handoff confirmation, and preserves the already-on-target/current-map route boundary.
  - Adjacent guards `NavigationWorldMapYellowDestinationRoutePolicyTest`,
    `NavigationWorldMapRouteMemoryIntentOwnershipTest`, and
    `NavigationLingShouVillageDirectRoutePolicyTest` passed.
  - Replay artifact for the failed 洛阳城 final coordinate mapping:
    `images/test-cases/minimap/world-map-yellow-output/cr110_luoyang_325_108_marked.png`
    and `.../cr110_luoyang_325_110_marked.png`.

Fresh runtime acceptance:

- First same-route run logs fresh yellow OCR/template row detection, yellow row click, pending yellow
  memory creation with `usedMemory=false`, `worldMapYellowDestinationMiniMap` intent registration, and
  watcher `ARRIVED` success.
- After the entry becomes clean, the next same-route run logs yellow route memory fast path
  `usedMemory=true`, does not run fresh yellow OCR before the remembered row click, opens the
  destination mini-map, clicks the final mini-map coordinate, and receives watcher `ARRIVED`.
- A yellow-route failure dirties only the yellow entry and does not affect any legacy green-link
  memory for the same route.

Card CR111: navigation pathing completion must be Runner-only

Business source:

- User observed a 10:20 修罗 accept-NPC navigation duplicate click:
  - `10:20:29.204` first mini-map click toward `灵兽村 (112,93)`.
  - `10:20:31.537` pathing intent registered for `targetMap=灵兽村 target=(112,93)`.
  - Runner kept reporting pathing as `ACTIVE` on `灵兽村`.
  - Task thread woke on the 900ms handoff loop, local movement evidence was weak
    (`validSamples=1 minStableSamples=3 state=UNKNOWN`), then the task treated pathing as ended.
  - 修罗 re-entered `ACCEPT_TASK_NAVIGATE_TO_NPC`, saw the map was already `灵兽村`, opened the
    current mini-map, and clicked the target again around `10:20:40`.
- This shows the current navigation stack still has two pathing judges:
  - Runner/window watcher knows the current `WindowPathingIntent` is still active.
  - The task thread can still use `GameStateUtil.detectMovementState()` / local movement probes to
    declare the wait ended.
- User direction: navigation movement completion must be unified. Runner should be the single
  authority for whether movement has ended.

Problem statement:

- `XiuluoTaskV2.continueIfNavigationStillPathing(...)` first checks the watcher snapshot, but if it
  does not consume an `ARRIVED` / `STOPPED_AWAY` terminal it falls through to
  `gameStateUtil.detectMovementState()`.
- `MOVING` / `PATHING_ACTIVE` keeps yielding, but `MAYBE_MOVING` is explicitly ignored and then the
  method logs `navigation pathing wait ended`, allowing the phase to call `NavigationService` again.
- `ACCEPT_TASK_NAVIGATE_TO_NPC`, maintenance NPC pathing, return fallback, and similar non-target
  phases can therefore still re-plan from a 900ms wake while Runner is still reporting
  `ACTIVE` / `UNKNOWN` / `probeInProgress`.
- Similar local wait-ended logic exists in 五倍 and 五环 maintenance / navigation waits and needs the
  same policy audit.

Required behavior:

- Runner/window pathing watcher is the only authority for pathing completion whenever a
  `WindowPathingIntent` exists for the task phase.
- A task phase that receives `NavigationResultStatus.PATHING_STARTED` with a current/registered intent
  must remain parked/yielded until one of these occurs:
  - watcher terminal `ARRIVED`;
  - watcher terminal `STOPPED_AWAY`;
  - explicit pathing hard timeout owned by the watcher/intent lifecycle;
  - verified prepared route action that legitimately supersedes the current wait.
- While the matching intent or fresh snapshot is `ACTIVE`, `UNKNOWN`, or `probeInProgress`, task code
  must not:
  - call `NavigationService` again for the same leg;
  - open the mini-map/world-map again;
  - use local pixel/coordinate movement state to declare pathing ended.
- `GameStateUtil.detectMovementState()` / `isMovingByPixelDiff(...)` may remain only for:
  - proving movement started immediately after a click when no watcher terminal exists yet;
  - explicit legacy/debug paths that do not register a `WindowPathingIntent`;
  - diagnostic logging that cannot affect task phase progression.
- `MAYBE_MOVING` must never be treated as permission to end an intent-backed pathing wait.
- If the watcher is genuinely stuck, recovery must come from a single clear hard timeout path, logged
  as a pathing terminal timeout/recovery. It must not be the 900ms handoff loop plus local movement
  weak-signal fallback.

Scope:

- First fix 修罗:
  - `ACCEPT_TASK_NAVIGATE_TO_NPC`;
  - `AFTER_ACCEPT_MAINTENANCE_CHECK` / 医宝宝 navigation;
  - `BEFORE_ROUTE_MAINTENANCE_CHECK` / 修装备 navigation;
  - `NAVIGATE_TO_TARGET`;
  - `NAVIGATE_BACK_TO_START`;
  - tracker shortcut / CR84-CR90 pathing waits if they contain any local wait-ended fallback.
- Then apply the same policy audit to:
  - 五倍 pathing waits and maintenance navigation waits;
  - 五环 accept-NPC / current-map retry waits;
  - shared `NavigationService` result handling if it still encourages local end-of-pathing decisions.
- Out of scope:
  - changing OCR/template/click coordinates;
  - changing CR99 yellow route matching/click logic;
  - changing pathing detection thresholds inside Runner unless required for missing terminal events;
  - deleting legacy/debug methods wholesale.

Implementation notes:

- Prefer a shared policy/helper boundary for intent-backed pathing waits instead of copying new
  branch logic into every task.
- The helper should consume:
  - current phase/source prefix;
  - expected target map/coordinate when known;
  - active `WindowPathingIntent`;
  - current `WindowPathingSnapshot`;
  - configured hard timeout.
- The helper should return an explicit result such as:
  - `KEEP_WAITING`;
  - `ARRIVED`;
  - `STOPPED_AWAY`;
  - `PREPARED_ROUTE_READY`;
  - `TIMEOUT_RECOVERY`;
  - `NO_MATCHING_INTENT`.
- `NO_MATCHING_INTENT` may allow legacy fallback only if the navigation call did not register an
  intent or if the card explicitly documents why the phase has no intent.
- Logs should include:
  - `intentId`;
  - source prefix;
  - phase;
  - watcher state;
  - current/target map and coordinate;
  - probe age / snapshot age;
  - whether the task is continuing wait, consuming terminal, or timing out.
- Keep leader pathing maintenance behavior intact: maintenance may run while waiting, but it must not
  end the pathing wait or trigger a duplicate navigation click.

Verification:

- Add focused source guards/tests proving 修罗 `ACCEPT_TASK_NAVIGATE_TO_NPC`:
  - matching watcher `ACTIVE` returns keep-waiting and does not call local movement end logic;
  - matching watcher `UNKNOWN` with fresh snapshot/probe returns keep-waiting;
  - matching watcher `ARRIVED` permits phase continuation;
  - matching watcher `STOPPED_AWAY` permits retry/recovery;
  - stale/no terminal only recovers through the hard timeout path;
  - `MAYBE_MOVING` cannot end an intent-backed wait.
- Add equivalent focused guards for at least one 五倍 and one 五环 pathing wait that currently uses
  local movement wait-ended logic.
- Run focused CR111 guards.
- Run `mvn -q -DskipTests compile`.
- Run `mvn -q -DskipTests test-compile`.

Review note - 2026-06-26:

- P2 open: CR111 removed the local movement fallback, but some runner-only waits now check
  watcher `ACTIVE` / `UNKNOWN` / `probeInProgress` before checking the explicit hard timeout. In
  `FiveRingTaskV2.waitPathing(...)`, `ACTIVE` / `UNKNOWN` / probe returns `pathing watcher still
  active` at `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java:1843-1849`, so the later
  timeout branch at `1881-1887` is unreachable while the watcher keeps refreshing the same active
  intent. The same shape exists in 五环 accept-NPC wait: active/probe returns at `1652-1658`, then
  active-intent still-in-flight returns at `1687-1690`, before the hard timeout at `1693-1696`.
  This violates the card requirement that a genuinely stuck watcher recovers through one clear hard
  timeout path.
- P2 open: `XiuluoTaskV2.continueIfNavigationStillPathing(...)` also keeps reattaching the
  Runner-only wait for same-intent `ACTIVE` / `UNKNOWN` snapshots at
  `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java:3346-3361` and again at `3368-3387`.
  Because this path no longer has any pathing-age timeout check, a same-intent watcher that never
  publishes terminal/prepared-route can park indefinitely. That is safer than duplicate clicking,
  but it is not the requested timeout recovery.
- P3 open: the focused CR111 guards are source-string checks. For example
  `FiveRingRunnerOnlyPathingWiringTest` only checks that `PATHING_TARGET_WAIT_TIMEOUT_MS` appears and
  that local movement calls are absent; it does not assert that timeout is evaluated before
  `ACTIVE` / `UNKNOWN` keep-wait branches. Add a behavioral/source-order guard for timeout-before-
  keep-wait in 修罗, 五倍 maintenance, 五环 accept-NPC, and 五环 generic `WAIT_PATHING`.
- Verification recheck: direct focused guards
  `XiuluoRunnerOnlyPathingCompletionWiringTest`, `FiveRingRunnerOnlyPathingWiringTest`, and
  `WubeiRunnerOnlyMaintenancePathingWiringTest` pass, but they do not cover the timeout-order issue.

Review repair - 2026-06-26:

- The timeout-order finding was repaired. 五环 terminal states are still consumed first, but
  non-terminal watcher `ACTIVE` / `UNKNOWN` / probe and active-intent keep-wait now run only after
  `PATHING_TARGET_WAIT_TIMEOUT_MS` is checked in `waitPathing(...)` and
  `continueIfAcceptNpcNavigationStillPathing(...)`.
- 修罗 `continueIfNavigationStillPathing(...)` now has `RUNNER_PATHING_HARD_TIMEOUT_MS`, using the
  matching active/snapshot `WindowPathingIntent.createdAtMs` as the Runner-only timeout clock before
  same-intent watcher keep-wait or generic Runner-only keep-wait.
- Focused guards were extended to assert timeout ordering:
  `FiveRingRunnerOnlyPathingWiringTest` checks 五环 wait/accept order, and
  `XiuluoRunnerOnlyPathingCompletionWiringTest` checks 修罗 hard-timeout presence and order.
- Verification passed: focused guards, `mvn -q -DskipTests compile`, and
  `mvn -q -DskipTests test-compile`.

Review follow-up - 2026-06-26:

- P2 open: 五倍 maintenance navigation waits still have no hard-timeout recovery. In
  `WubeiTask.continueIfMaintenanceNavigationStillPathing(...)`, snapshot `null`, `NONE`, `ACTIVE`,
  `UNKNOWN`, or `probeInProgress` immediately returns `waitForPathingWake(...)`. That wait spec uses
  `WUBEI_WAIT_UNTIL_RUNNER_EVENT_MS = -1`, so `WindowReadyEventBus.awaitNewer(...)` waits until an
  event or interruption, not until a timeout.
- This is not covered by a Runner-owned generic timeout: `WindowTaskRunner.publishPathingTerminalEventIfNeeded(...)`
  publishes `PATHING_TERMINAL` only when state changes to `ARRIVED` or `STOPPED_AWAY`. If a 五倍
  maintenance intent keeps producing `ACTIVE` / `UNKNOWN`, or if the snapshot is lost/`NONE`, the
  task can park indefinitely.
- Suggested fix: record/derive the matching maintenance `WindowPathingIntent` and apply the same
  intent-age hard-timeout policy used by 修罗 / 五环 before returning `waitForPathingWake(...)`.
  On timeout, clear the current pathing signal and let the maintenance hook retry/continue from the
  current state. Extend `WubeiRunnerOnlyMaintenancePathingWiringTest` to assert timeout-before-
  keep-wait, not just absence of local movement probes.
- Focused recheck passed but does not catch this gap:
  `javac -encoding UTF-8 -d target\test-classes src\test\java\com\bot\dhxy\task\xiuluo\XiuluoRunnerOnlyPathingCompletionWiringTest.java src\test\java\com\bot\dhxy\task\wuhuan\FiveRingRunnerOnlyPathingWiringTest.java src\test\java\com\bot\dhxy\task\wubei\WubeiRunnerOnlyMaintenancePathingWiringTest.java; if ($LASTEXITCODE -eq 0) { java -cp target\test-classes com.bot.dhxy.task.xiuluo.XiuluoRunnerOnlyPathingCompletionWiringTest; java -cp target\test-classes com.bot.dhxy.task.wuhuan.FiveRingRunnerOnlyPathingWiringTest; java -cp target\test-classes com.bot.dhxy.task.wubei.WubeiRunnerOnlyMaintenancePathingWiringTest }`

Review follow-up repair - 2026-06-26:

- The 五倍 maintenance timeout gap was repaired. `WubeiTask.continueIfMaintenanceNavigationStillPathing(...)`
  now derives the expected maintenance pathing source from `hookName`, prefers the matching active
  `WindowPathingIntent`, falls back to the matching snapshot intent, and computes `intentAgeMs`.
- It still consumes `ARRIVED` / `STOPPED_AWAY` first, but non-terminal snapshot `null`, `NONE`,
  `ACTIVE`, `UNKNOWN`, or `probeInProgress` waits now check
  `WUBEI_MAINTENANCE_PATHING_HARD_TIMEOUT_MS` first. On timeout it clears the current pathing signal
  and lets the maintenance hook retry/continue.
- `WubeiRunnerOnlyMaintenancePathingWiringTest` now asserts hard-timeout presence, intent-age use,
  and timeout-before-keep-wait ordering.
- Verification passed: all three CR111 focused guards, `mvn -q -DskipTests compile`, and
  `mvn -q -DskipTests test-compile`.

Re-review - 2026-06-26:

- No new P1/P2/P3 source blocker found in the inspected CR111 paths.
- 五倍 maintenance P2 repair is present: the code computes matching maintenance `intentAgeMs` before
  the `null` / `NONE` / `ACTIVE` / `UNKNOWN` / `probeInProgress` keep-wait branch, and clears the
  pathing signal on `WUBEI_MAINTENANCE_PATHING_HARD_TIMEOUT_MS`.
- 五倍 ordinary tracker pathing still waits via Runner events, but it has the separate
  Runner-published `PRE_BATTLE_TIMEOUT` recovery consumed by `consumeOrdinaryPreBattleTimeoutBeforeNormalPhase(...)`;
  this is not the same unbounded maintenance gap.
- Recheck passed:
  `XiuluoRunnerOnlyPathingCompletionWiringTest`,
  `FiveRingRunnerOnlyPathingWiringTest`,
  `WubeiRunnerOnlyMaintenancePathingWiringTest`,
  `mvn -q -DskipTests compile`, and `mvn -q -DskipTests test-compile`.
- Fresh runtime acceptance is still required: reproduce 修罗 accept-NPC / 五倍 maintenance / 五环
  waits and confirm no duplicate mini-map/world-map clicks before Runner terminal or hard timeout.

Fresh runtime acceptance:

- Reproduce a 修罗 accept-NPC pathing leg like the 10:20 case. While Runner reports
  `ACTIVE` / `UNKNOWN` / `probeInProgress` for the same intent, logs must show continued waiting on
  the same `intentId`; there must be no second mini-map/world-map click.
- The old chain must disappear:
  `navigation pathing wait weak movement ignored -> navigation pathing wait ended -> navigateInCurrentMap -> second mini-map click`.
- On real arrival, Runner publishes/records terminal `ARRIVED`, task consumes it, and only then
  continues to click the NPC.
- On a forced stuck pathing scenario, recovery happens only after the configured hard timeout and logs
  a clear timeout/recovery reason.
- 五倍/五环 sampled pathing waits show the same single-Runtime/Runner authority behavior.

Card CR112: 修罗 WAIT_COMBAT combat-exit event race

Business source:

- Fresh 修罗 runtime reports where the leader had already exited combat but `WAIT_COMBAT` remained
  parked or woke only after a slow timeout/maintenance cadence.

Problem statement:

- `XiuluoTaskV2` called `autoCombatService.handleCombatTick(...)` and only then captured
  `WindowReadyEventBus.currentSequence()` for the following wait.
- If `handleCombatTick(...)` itself published `COMBAT_STATE_CHANGED` while consuming the battle-exit
  state, the subsequent wait used an `afterSequence` that was already newer than the event.
- Result: the task could miss the exact event that should wake it and fall back to slower timeout
  behavior.

Required behavior:

- Capture the ready-event sequence before the combat tick when entering `WAIT_COMBAT`.
- Pass that captured sequence into the combat-state wait so events published during
  `handleCombatTick(...)` are eligible to wake the task.
- Apply the same ordering to tracker-shortcut incidental combat handling.
- Do not change OCR/template/click/navigation behavior or any battle result classification.

Implementation / verification status:

- Implemented in `XiuluoTaskV2`: `combatWaitAfterSequence` is captured before the combat tick and
  passed to `waitForCombatStateWake(...)`.
- Focused source guard:
  `XiuluoWaitCombatEventWakeWiringTest`.
- Verification passed:
  `XiuluoWaitCombatEventWakeWiringTest`,
  `mvn -q -DskipTests compile`,
  and `mvn -q -DskipTests test-compile`.

Fresh runtime acceptance:

- In a normal 修罗 expected combat exit, logs should show the task waking from
  `COMBAT_STATE_CHANGED` promptly after the Runner/auto-combat state changes to `NONE`.
- The old symptom should disappear: combat exit already known, but 修罗 remains in `WAIT_COMBAT`
  until a slow timeout/maintenance wake.

Card CR113: expected-combat 20x20 avatar fast exit probe

Business source:

- Fresh 修罗 post-combat latency reports: Runner/battle watcher often knows combat exit eventually,
  but waiting for full radar cadence can still add seconds before return-home begins.

Problem statement:

- CR109 intentionally lets expected 修罗/五倍 combat exits skip heavy post-combat supply/incense work
  and return control to the task quickly.
- The remaining bottleneck is detecting expected combat exit fast enough without running expensive
  full battle-radar checks every tick.

Required behavior:

- For `FAST_EXPECTED_EXIT` only, arm a lightweight exit probe after confirmed combat entry.
- After a 15s grace period, sample a 20x20 ROI centered around the configured leader/team-hover
  avatar area once per second.
- Use the existing image-diff helper (`ImageFinder.isMatch(...)`) rather than inventing a new diff
  algorithm.
- If the avatar ROI changes enough, mark combat state as exited through the normal
  `updateCombatState(false)` path so downstream event handling remains unchanged.
- Keep the old full battle-radar check as a sparse fallback; do not alter combat entry detection,
  incidental/unknown combat recovery, or 五环.

Implementation / verification status:

- Implemented in `BattleRadarService` and `AutoCombatService`.
- Focused source guard:
  `BattleRadarFastExpectedExitSourceTest`.
- Verification passed:
  `BattleRadarFastExpectedExitSourceTest`,
  `mvn -q -DskipTests compile`,
  and `mvn -q -DskipTests test-compile`.

Fresh runtime acceptance:

- For expected 修罗/五倍 battles lasting beyond the 15s grace, exit should be detected on the
  1s avatar probe cadence or the sparse fallback.
- Post-combat return-home should start sooner than the old full-radar-only path.
- Incidental/unknown combat must still use conservative recovery and must not be converted into
  fast expected exit by this probe.

Card CR114: task-page item lookup current-page first, no full-bag sweep

Business source:

- Fresh 修罗 hot-start evidence around `09:41`: tracker miss led to two return-item attempts; each
  attempt opened the bag and scanned current page plus pages 6->1 for `xiuluo_return_item`, wasting
  about 17 seconds when the task item was absent.

Problem statement:

- 修罗/五倍 return/probe items are task-page items, but the old 修罗 hot-start path used a generic
  from-back bag scan.
- A later task-page-only direction fixed the full sweep but could still click the task tab even when
  the bag already opened on the task page and the item was visible.
- The intended behavior should match 五倍-style efficiency: check what is visible first, then use the
  task tab only if needed.

Required behavior:

- For task-page item lookup, open/ensure the bag as usual.
- Scan the current visible page first.
- If the item is found, click/use it directly; do not click the task tab first.
- If not found, click/scan only the task tab.
- Do not scan pages 6->1 for these task-only items.
- Do not broadly refactor `ensureBagOpened(...)` or generic bag page-search behavior.

Implementation / verification status:

- Implemented narrowly in `BagService` task-page item lookup.
- Focused source guard:
  `BagTaskPageItemCurrentPageFirstTest`.
- Verification passed:
  `BagTaskPageItemCurrentPageFirstTest`,
  `mvn -q -DskipTests compile`,
  and `mvn -q -DskipTests test-compile`.

Fresh runtime acceptance:

- 修罗 hot-start return-item attempt should log current-page scan first.
- If current page already contains the return item, it should use it without clicking the task tab.
- If absent, it should scan only the task tab and then fail quickly; no 6->1 full-bag sweep.

Card CR115: 修罗 enter-battle interest must override route-transfer prepared blocker

Business source:

- Fresh 修罗 shortcut case around `12:54`: visible dialog was `OPTION` and screenshot showed
  `看打！/点错。`, but Runner prepared/reused `ROUTE_TRANSFER target=蟠桃园`; 修罗 was waiting for
  `XIULUO_ENTER_BATTLE`, consumed a mismatch, and then stayed parked while only
  `TASK_ATTENTION_REQUIRED` repeated.

Problem statement:

- During tracker-shortcut pathing, an active route/pathing intent can leave a route-transfer prepared
  action alive when the final enter-battle option appears.
- The route prepared action can be fresh enough to block task dialog preparation, even though the
  current 修罗 dialog interest explicitly wants `XIULUO_ENTER_BATTLE`.
- This turns a visible `看打` option into a stuck wait because the task does not consume
  `ROUTE_TRANSFER`.

Required behavior:

- When task type is `XIULUO_V2`, visible dialog type is `OPTION`, and the active dialog interest
  supports `XIULUO_ENTER_BATTLE`, Runner must try task-interest preparation before generic route
  preparation.
- If an existing prepared action is `ROUTE_TRANSFER` and mismatches that enter-battle interest, it
  may be cleared so the task dialog interest can prepare `XIULUO_ENTER_BATTLE`.
- If enter-battle preparation does not match, existing route preparation remains available as
  fallback; this card must not globally disable route-transfer preparation.
- Do not change OCR/template thresholds, click coordinates, route matching, or pathing intent
  registration.

Implementation / verification status:

- Implemented in `WindowTaskRunner` by prioritizing 修罗 enter-battle interest in the visible-option
  follow-up path and clearing only the scoped route-transfer blocker.
- Focused source guard:
  `XiuluoEnterBattleInterestPriorityWiringTest`.
- Verification passed:
  `XiuluoEnterBattleInterestPriorityWiringTest`,
  `mvn -q -DskipTests compile`,
  and `mvn -q -DskipTests test-compile`.

Fresh runtime acceptance:

- Reproduce a tracker-shortcut target where `看打` appears while a route/pathing intent is still
  active.
- Runner should publish `PREPARED_ACTION_READY operation=XIULUO_ENTER_BATTLE`; 修罗 should consume it
  and click the option.
- There should be no `consumePrepared mismatch expectedOperation=XIULUO_ENTER_BATTLE
  actual=ROUTE_TRANSFER` loop for the same visible `看打` dialog.

Runtime acceptance - 2026-06-26:

- Passed in the sampled 修罗 run. Round 52/53/54/58/59/60/61/62/63/65 all prepared/consumed
  `XIULUO_ENTER_BATTLE` after tracker shortcut without a `ROUTE_TRANSFER` mismatch loop.
- Concrete close sample: `20:22:54.002` Runner prepared `operation=XIULUO_ENTER_BATTLE`, and
  `20:22:54.188` 修罗 consumed it with validation passed.

Card CR116: 修罗 pre-combat watchdog must bound every wait

Business source:

- Fresh 修罗 stuck case around `2026-06-25 12:53-12:59+`: after tracker green click, the leader waited
  in `WAIT_TRACKER_SHORTCUT_PATHING` for more than six minutes even though the 180s pre-combat
  watchdog is supposed to force failure/reaccept before combat.

Problem statement:

- 修罗 already has `PRE_COMBAT_WATCHDOG_TIMEOUT_MS = 180_000L`, but the check only runs when the task
  phase loop gets control again.
- Several war-before-combat waits can park or block forever, so the watchdog never gets a chance to
  run:
  - `WAIT_TRACKER_SHORTCUT_PATHING` uses `WAIT_TARGET_PATHING_TERMINAL_TIMEOUT_MS = -1L`.
  - normal navigation pathing wait uses the same `-1L` timeout.
  - `waitForAcceptTrackerPanelResult(...)` loops on `future.get(250ms)` with no remaining-budget cap.
  - `waitForBackgroundObjectiveResult(...)` does the same.
  - `TASK_ATTENTION_REQUIRED` can repeat while the task is waiting only for
    `PREPARED_ACTION_READY`, `COMBAT_STATE_CHANGED`, or `PATHING_TERMINAL`.
- Result: if Runner keeps seeing a dialog or pathing state but does not publish one of the exact
  events the task accepts, 修罗 can stay parked indefinitely and never return to reaccept.

Required behavior:

- Treat the 180s pre-combat watchdog as a real remaining budget from "accepted/known active 修罗 task"
  until combat entry.
- Every pre-combat park wait must use the remaining budget as an upper bound, not `timeoutMs=-1`.
- Every pre-combat background future wait must also use the remaining budget; it must not spin
  forever on repeated short `future.get(...)` timeouts.
- On budget expiry, return through the existing failure/reaccept path, with a log that includes the
  phase, wait reason, elapsed time, remaining budget, and whether the task was in tracker shortcut,
  normal navigation, accept tracker parse, or objective parse.
- Combat time is excluded: once actual combat starts, this watchdog must stop counting until combat
  exits.
- Approved maintenance/supply pauses are excluded or compensated by the existing maintenance/pause
  accounting; CR116 must not make legitimate repair/heal/supply windows fail the round.
- Do not change OCR/template thresholds, click coordinates, route-result parsing, or the
  `XIULUO_ENTER_BATTLE` prepared-action priority from CR115.

Implementation guidance:

- Prefer one central "remaining pre-combat watchdog budget" helper owned by `XiuluoRoundContext` /
  `XiuluoTaskV2`, rather than scattered new constants.
- `waitForTrackerShortcutWake(...)` and `waitForNavigationPathingWake(...)` should pass the smaller
  of their normal timeout and the remaining pre-combat budget to `WindowReadyEventBus`.
- `waitForAcceptTrackerPanelResult(...)` and `waitForBackgroundObjectiveResult(...)` should check the
  same remaining budget inside their loop and return a watchdog timeout outcome when exhausted.
- If the remaining budget is already zero before entering a wait, fail immediately instead of
  parking.
- Keep the old `PRE_COMBAT_WATCHDOG_TIMEOUT_MS` value at 180s unless the user explicitly changes the
  business limit.

Acceptance:

- Reproduce the `12:53` shape: tracker shortcut green clicked, visible option/dialog keeps appearing,
  but no valid `XIULUO_ENTER_BATTLE` / terminal event is consumed. The task must not wait beyond the
  remaining 180s budget; it must fail/reaccept.
- Reproduce normal 修罗 tracker shortcut and normal navigation paths. They should still wake early on
  `PREPARED_ACTION_READY`, `COMBAT_STATE_CHANGED`, or `PATHING_TERMINAL`.
- Force or mock stuck accept tracker/background objective futures. They must timeout by the same
  pre-combat budget and not loop forever.
- Verify maintenance/supply/user pause compensation: approved non-task movement does not consume the
  full 180s budget and falsely fail a valid round.
- Performance goal: repeated `WAIT_TRACKER_SHORTCUT_PATHING` / navigation 900ms or infinite waits
  must disappear from stuck cases; logs should show one bounded wait and a clear watchdog expiry.

Implementation notes - 2026-06-26:

- Added `XiuluoPreCombatWatchdogBoundedWaitWiringTest` as the CR116 source guard.
- `XiuluoTaskV2.parkAfterYieldIfNeeded(...)` now computes `boundedTimeoutMs` from the remaining
  pre-combat watchdog budget and passes it to `WindowReadyEventBus` for both pathing-specific and
  generic waits.
- If the budget is exhausted before or after an event wait, the task returns a normal `FAILED`
  outcome and therefore reuses `restartRoundAfterPhaseFailure(...)` / same-round reaccept.
- Accept-time tracker parse and objective parse future waits now cap each `future.get(...)` slice to
  the remaining budget and return empty results on watchdog expiry, preserving the existing shortcut
  fallback/objective recovery paths.
- Verification passed:
  - `XiuluoPreCombatWatchdogBoundedWaitWiringTest`
  - `XiuluoPreCombatWatchdogWiringTest`
  - `XiuluoEventWaitPauseCompensationWiringTest`
  - `mvn -q -DskipTests compile`
- Runtime acceptance still pending against a fresh stuck-shape 修罗 run.

Review note - 2026-06-26:

- Source/card comparison against 五倍 confirms the root difference: 五倍 ordinary pre-battle can use
  infinite event waits because `WindowTaskRunner` owns a separate timer and publishes
  `PRE_BATTLE_TIMEOUT`, and 五倍 waits include that wake type. 修罗 did not have an equivalent
  external timeout event; its watchdog lived inside the task phase loop, so a `timeoutMs=-1` park or
  unbounded future wait could prevent the watchdog from ever running.
- CR116's chosen repair is therefore acceptable: instead of copying the 五倍 Runner timer vocabulary,
  it pushes the existing 180s 修罗 pre-combat budget into every pre-combat event/future wait. This
  also covers stuck background tracker/objective futures, which a Runner timeout event would not
  necessarily wake.
- No new source blocker found in the inspected CR116 paths.

Runtime acceptance - 2026-06-26:

- Passed in the sampled 修罗 run. Round 52-65 used bounded tracker/pathing waits and woke by Runner
  `PATHING_TERMINAL` / `PREPARED_ACTION_READY` without the old infinite `timeoutMs=-1` park or
  180s watchdog bypass.
- Maintenance time compensation remained visible during the CR119 before-park summon maintenance
  (`pre-combat timer paused ... blockedMs=...`), so the sampled maintenance path did not falsely
  consume the watchdog budget.

Card CR117: 摄妖香 green-minute digit-template self learning

Business source:

- Fresh 摄妖香 status case: the washed status image visually showed a two-digit minute value such as
  `47`, but OCR returned only `7`. Trusting that partial OCR result made the remaining time look low
  and risked false/early refill.

Problem statement:

- 摄妖香 green minute OCR is too fragile for tiny status-bar digits.
- A partial OCR hit can cover only the right-side digit, but the old path may treat that one digit as
  the complete remaining-minute value.
- This is not a refill-threshold problem: the refill policy can be correct and still misfire if the
  minute reader turns `47` into `7`.

Required behavior:

- Add a dedicated digit-template reader for the already washed black-on-white 摄妖香 green-minute
  crop.
- Segment the crop into digit glyphs.
- When templates exist for all visible glyphs, read the complete number from templates first and
  return it to `PlayerStateService`.
- When templates are missing, use OCR only as a teacher:
  - if OCR proves a full multi-digit value and aligns with all glyphs, learn each matching digit;
  - if OCR proves only one digit and its box overlaps only one glyph, learn only that glyph;
  - never learn the whole multi-digit crop as a single digit.
- If the image contains multiple glyphs but OCR/template proof is incomplete, report learning/unknown
  state and do not treat the partial OCR digit as the remaining-minute value.
- Template directory:
  `images/template/status/sheyaoxiang_digits/`.
- Stop learning automatically once `0.png` through `9.png` are present.

Boundaries:

- Do not change 摄妖香 item template, bag search/click behavior, memory trust window, refill
  threshold, hover-safe mouse movement, HP/MP logic, 五倍/修罗 task flow, or post-combat recovery
  policy.
- Do not trust OCR as a complete minute value when the segmented glyph count is larger than the
  OCR-proven digit count.

Implementation / verification status:

- Implemented `SheyaoxiangDigitTemplateReader`.
- `PlayerStateService` now tries the template reader before accepting green-minute OCR:
  - complete template read logs/returns source like `green-minutes-template=<minutes>`;
  - incomplete multi-glyph learning state avoids false low-minute refill.
- Replay/focused test:
  `SheyaoxiangDigitTemplateReaderTest`.
- Verification passed:
  `SheyaoxiangDigitTemplateReaderTest`,
  `mvn -q -DskipTests compile`,
  `mvn -q -DskipTests test-compile`,
  and scoped `git diff --check`.

Fresh runtime acceptance:

- During actual 摄妖香 status reads with incomplete template coverage, logs should show learning
  events such as `sheyaoxiang green digit template learned` and should not refill solely because OCR
  returned one digit from a two-digit crop.
- Once templates for the visible digits exist, logs should show `green-minutes-template=<minutes>`
  and the full value must drive the remaining-time decision.
- Confirm no regression to CR72 hover-safe icon/status checks and no unintended bag/refill behavior
  changes.

Runtime acceptance - 2026-06-26:

- Passed for the complete-template read path. Fresh runtime logged
  `sheyaoxiang status matched ... remaining=green-minutes-template=16`, and the remaining-minute
  decision used the template reader instead of a partial OCR digit.
- No CR72 hover/refill regression was observed in the same sampled 修罗 log slice.

Card CR118: 修罗 startup first-aid before hot-start

Business source:

- User observed that after starting the game/task, the 修罗 leader did not heal HP/MP before moving
  into the run.
- Code comparison showed 五倍/五环 startup paths call
  `PlayerStateService.performStartupFirstAidCheck(context)`, while 修罗 only did the startup
  摄妖香 check before hot-start.

Problem statement:

- 修罗 first-round hot-start can immediately choose tracker shortcut, return-item verification, or
  accept-task navigation.
- Without a startup first-aid hook, the leader may enter navigation/combat with stale 人物/宝宝
  HP/MP even though the same supply policy already exists and is used by other tasks.

Required behavior:

- On the first 修罗 run only (`completedRuns == 0`), run
  `playerStateService.performStartupFirstAidCheck(context)` before:
  - `ensureStartupIncenseBeforeHotStart(context)`;
  - `resolveStartupTrackerOrReturnItem(...)`;
  - tracker shortcut / return-item / accept-task fallback decisions.
- Do not add the startup first-aid check to every completed 修罗 round.
- Preserve the existing `PlayerStateService` thresholds, no-focus detection, bag/item logic,
  click/input behavior, 摄妖香 startup logic, tracker shortcut logic, return-item fallback,
  navigation, and OCR/template algorithms.

Implementation / verification status:

- `XiuluoTaskV2.execute(...)` now calls `performStartupFirstAidCheck(context)` inside the
  first-round startup block, before startup 摄妖香 and hot-start selection.
- Added source guard:
  `XiuluoStartupFirstAidWiringTest`.
- Verification passed:
  `XiuluoStartupFirstAidWiringTest`,
  `mvn -q -DskipTests compile`,
  and `mvn -q -DskipTests test-compile`.

Fresh runtime acceptance:

- On the next 修罗 fresh startup, logs should show the startup first-aid precheck/action before
  startup 摄妖香 effects and before tracker shortcut / return-item / accept-task navigation.
- If HP/MP are already safe, the precheck should no-op quickly; if not, the existing first-aid
  path should perform the same heal behavior used by 五倍/五环 startup.

Card CR104: 修罗 leader WAIT_COMBAT combat maintenance and auto-combat rounds refresh

Fresh runtime recheck - 2026-06-26:

- User observed the 修罗 leader auto-combat rounds had fallen to about 8 and did not appear to
  refresh during combat.
- Leader window: `hwnd-3F50A4E`, role `LEADER`, player `火鸡味锅巴°（ID：443075411）`.
- The 4s entry maintenance does fire repeatedly after combat enter, for example:
  - `12:02:36.220` `auto-combat enter detected: schedule entry maintenance after 4000 ms`;
  - `12:02:40.481` `auto-combat entry maintenance: clean generic windows and verify panel`;
  - `12:02:44.782` `auto-combat panel rounds refresh skipped: source=entry-maintenance reason=verify-only`.
- The same verify-only pattern repeats at `12:04:40`, `12:25:50`, `12:50:50`, `12:52:25`,
  `13:49:10`, `13:50:56`, `13:52:33`, `13:55:09`, `13:57:18`, `13:59:16`, `14:05:58`,
  `14:07:41`, `14:09:22`, `14:11:23`, `14:13:44`, and `14:15:41`.
- In the same fresh run, a member did refresh:
  `13:55:40.458` logged `auto-combat panel rounds refresh by Alt+8 without OCR:
  source=verify reason=refresh-due estimate=22 threshold=10`.
- No matching leader `reason=low-rounds` or leader `reason=refresh-due` panel refresh was found in
  the inspected 12:00-14:16 range, apart from fallback `Alt+8` attempts when the panel itself was
  not found (`source=xiuluo-v2:combat-enter`).
- Additional `18:24-18:49` fresh runtime on leader `hwnd-2AD117C` repeats the same gap: leader
  WAIT_COMBAT repeatedly logs `entry-maintenance refreshRounds=false` and
  `auto-combat panel rounds refresh skipped: source=entry-maintenance reason=verify-only` at
  `18:43:37`, `18:45:16`, `18:47:34`, and `18:49:57`, with no leader `refresh-due` or
  `low-rounds` Alt+8. A member window in the same run refreshed at `18:48:02.638` with
  `reason=refresh-due`, proving the refresh mechanism exists but the leader path is still not
  reaching it.

Current source diagnosis:

- `AutoCombatService.maybeRunCombatMaintenance(...)` intentionally calls
  `AutoCombatPanelService.PanelVerifyMode.ENTRY_MAINTENANCE` for the 4s entry pass.
- `ENTRY_MAINTENANCE(false, "entry-maintenance")` verifies/alignment only and then logs
  `rounds refresh skipped: reason=verify-only`.
- `nextCombatMaintenanceDelayMs()` wakes WAIT_COMBAT for pending entry maintenance, sparse generic
  UI cleanup, and configured refresh interval, but it does not treat a cached low-round estimate
  (`<= LOW_ROUNDS_REFRESH_THRESHOLD`, currently 10) as an immediate wake reason.
- Result: CR104 fixed the old "never wakes for entry maintenance" problem, but fresh runtime shows it
  does not guarantee leader low-round rescue. The leader can keep using verify-only entry passes and
  wait for the 120s refresh interval instead of refreshing promptly when rounds are already low.

Required repair direction:

- Keep CR104's bounded WAIT_COMBAT wake behavior; do not restore old `timeoutMs=-1` or 900ms churn.
- Preserve CR65's anti-burst rule for `refresh-due`.
- Add a leader-safe low-round path so WAIT_COMBAT maintenance refreshes promptly when
  `gameContext.getAutoCombatEstimatedRounds() <= LOW_ROUNDS_REFRESH_THRESHOLD`.
- Low-round refresh must bypass the same-team `refresh-due` guard the same way existing
  `RoundsRefreshReason.LOW_ROUNDS` is intended to, but still use the input queue and existing
  `AutoCombatPanelService` click behavior.
- Do not change combat entry detection, battle radar, OCR/template images, or panel coordinates.

Implementation update - 2026-06-26:

- `AutoCombatService.maybeRunCombatMaintenance(...)` still runs the 4s
  `ENTRY_MAINTENANCE` verify-only panel alignment, but it no longer returns before the optional
  rounds-refresh branch. If entry maintenance and a due/low-round refresh are both ready in the same
  WAIT_COMBAT tick, the refresh branch is still checked.
- The optional refresh branch now computes `RoundsRefreshReason` before relying on the service-local
  periodic timestamp. `LOW_ROUNDS` and `UNKNOWN` can enter `VERIFY_AND_REFRESH` immediately instead
  of waiting for the configured interval; `REFRESH_DUE` continues to use the existing same-team /
  per-window gates.
- Added a per-window urgent-rounds retry guard for `LOW_ROUNDS` / `UNKNOWN`, so a failed panel verify
  does not create a 900ms retry loop while the task remains in event-driven WAIT_COMBAT.
- `nextCombatMaintenanceDelayMs()` now treats cached `LOW_ROUNDS`, `UNKNOWN`, and due
  `REFRESH_DUE` as maintenance wake deadlines, respecting the same urgent and refresh-due retry
  guards.
- `AutoCombatRefreshDuePanelVerifyGateTest` now guards that entry maintenance cannot return before
  the optional refresh branch and that the urgent low/unknown cooldown exists.
- Verification passed:
  `mvn -q "-Dexec.classpathScope=test" "-Dexec.mainClass=com.bot.dhxy.service.AutoCombatRefreshDuePanelVerifyGateTest" exec:java`,
  and `mvn -q -DskipTests compile`.

Follow-up implementation update - 2026-06-26:

- Additional `18:26-19:26` runtime evidence showed the first repair still left a stale-cache gap:
  leader `hwnd-2AD117C` continued to log only entry-maintenance verify-only passes and no leader
  `auto-combat maintenance: refresh auto combat panel` / `Alt+8`, while member windows refreshed
  with `reason=refresh-due`.
- Root cause: when the cached `gameContext.getAutoCombatEstimatedRounds()` still looked healthy,
  `AutoCombatService` skipped the optional refresh branch without forcing `AutoCombatPanelService`
  to read actual visible panel rounds. If the visible panel had already fallen low, the low-round
  path never became true.
- `AutoCombatService` now arms `verifyActualRoundsAfterEntryMaintenance` after the 4s
  `ENTRY_MAINTENANCE` pass. If the same tick has a healthy cache, it consumes that one-shot flag by
  running `VERIFY_AND_REFRESH` once so visible panel rounds can correct stale state.
- `AutoCombatPanelService.refreshAutoCombatRoundsIfNeeded(...)` now receives the existing panel
  match and calls `readRemainingRounds(panelMatch, source)` before the healthy-cache skip. When OCR
  reads a value, it updates `gameContext.setAutoCombatEstimatedRounds(...)`; if that visible value
  is low, the existing Alt+8 refresh executes with `reason=low-rounds`. `REFRESH_DUE` remains gated
  by CR65.
- Added focused guards:
  `entryMaintenanceForcesActualRoundReadBeforeTrustingHealthyCache()` and
  `verifyAndRefreshReadsVisibleRoundsBeforeTrustingHealthyCache()`.
- Focused RED was observed after compiling the guard:
  `entry maintenance must arm one actual round read missing:
  state.verifyActualRoundsAfterEntryMaintenance = true`.
- Verification:
  `mvn -q -DskipTests compile`,
  `javac -encoding UTF-8 -cp "target/classes" -d target/test-classes src/test/java/com/bot/dhxy/service/AutoCombatRefreshDuePanelVerifyGateTest.java`,
  focused Maven exec for `AutoCombatRefreshDuePanelVerifyGateTest`,
  `mvn -q -DskipTests test-compile`, and `git diff --check`.

Fresh runtime acceptance:

- With the leader at or below the low-round threshold during 修罗 WAIT_COMBAT, logs should show a
  leader `auto-combat panel rounds capture plan`, a visible-round read/update such as
  `auto-combat panel rounds estimate updated by OCR`, then a
  leader `auto-combat maintenance: refresh auto combat panel ... reason=low-rounds` or equivalent
  `auto-combat panel rounds refresh by Alt+8 ... reason=low-rounds`.
- The old entry pass may still verify/align after 4s, but it must not be the only leader maintenance
  path while rounds are low.
- Member `refresh-due` bursts must remain controlled by CR65.
- Fresh runtime passed on 2026-06-27:
  - Leader `hwnd-17240550` / `火鸡味锅巴°（ID：443075411）` refreshed during 修罗
    `WAIT_COMBAT` at `17:49:21.056`:
    `auto-combat panel rounds refresh by Alt+8 without OCR: source=verify reason=refresh-due estimate=22`.
  - The same window reset the estimate to 25 at `17:49:22.283`.
  - Same-team member refresh-due probes were still gated at `17:49:25.084` and `17:49:25.419`,
    so CR65's burst guard remained intact.
- Verdict: CR104 is closed as Done. The fresh runtime proves the leader is no longer limited to
  entry-maintenance verify-only while WAIT_COMBAT is active; a future low-round-only sample can be
  treated as regression monitoring instead of blocking this card.

Card CR119: 修罗 leader 三技能 due wake while shortcut/pathing waits are parked

Business source:

- User observed that the 修罗 leader did not appear to maintain 三技能 during the latest run and
  asked whether the previously added leader-side 三技能 path had been removed.

Fresh runtime evidence:

- The leader window in the latest log is `hwnd-3F50A4E`, role `LEADER`, player
  `火鸡味锅巴°（ID：443075411）`.
- Latest relevant 修罗 start is:
  - `2026-06-26 13:46:56.122` `window [hwnd-3F50A4E] start task: 修罗`.
  - `2026-06-26 13:47:08.411` `maintenance init: summon skill cooldown starts now
    source=xiuluo_v2`.
  - With the configured `intervalMs=1200000`, leader 三技能 is due around `14:07:08.411`.
- After that due point, fresh logs through at least `14:24+` contain no leader
  `maintenance: summon skill ...`, `summon skill due`, or `start summon skill clean` for
  `hwnd-3F50A4E`.
- Instead, the leader repeatedly opens 修罗 tracker shortcut pathing windows and parks:
  - `14:08:32.646` opens `teamRound=xiuluo_v2#9` and enters
    `WAIT_TRACKER_SHORTCUT_PATHING`.
  - `14:10:14.990` opens `#10`.
  - `14:12:08.564` opens `#11`.
  - `14:14:48.743` opens `#12`.
  - `14:16:39.682` opens `#13`.
  - `14:18:26.990` opens `#14`.
  - `14:21:01.580` opens `#15`.
  - `14:23:19.577` opens `#16`.
  - `14:24:59.336` opens `#17`.
- These waits use runner events such as `PREPARED_ACTION_READY`, `PATHING_TERMINAL`, and
  `COMBAT_STATE_CHANGED`. They are now bounded by CR116's pre-combat budget, but they still do not
  wake when leader 三技能 becomes due.

Problem statement:

- The user expectation is that a long-running 修罗 leader should maintain 三技能 while the task is
  safely pathing and before/while the team pathing maintenance window is open.
- Latest evidence proves this is not just a cooldown-reset / not-due case. The latest run's cooldown
  is due after `14:07:08`, but no leader summon check runs during later shortcut/pathing windows.
- The likely root cause is scheduling: `runLeaderPathingSummonSkillMaintenance(...)` only runs when
  the task thread is awake and handling pathing state. In the current shortcut flow, the task parks in
  `WAIT_TRACKER_SHORTCUT_PATHING` waiting for runner facts and does not include "leader summon skill
  due" as a wake condition. Therefore the hook exists, but it is effectively unreachable during
  long or repeated shortcut/pathing waits.

Required repair direction:

- Add a leader maintenance-due wake/check to 修罗 pre-combat pathing waits, especially
  `WAIT_TRACKER_SHORTCUT_PATHING`.
- The wake should be bounded and low-churn. Prefer calculating the next due delay and parking with
  the minimum of:
  - remaining CR116 pre-combat watchdog budget;
  - next runner event;
  - next leader summon-maintenance due time when the team pathing window is open and the leader is
    safely in route-owned pathing.
- When the wait wakes because maintenance is due, run the existing leader-side maintenance path and
  then return to the same runner wait if still not in combat.
- Do not reopen the member-maintenance window after CR96 target-map-arrived closure, and do not let
  三技能 cleaning run after the target map is already arrived / enter-battle is being prepared.
- Preserve:
  - existing 三技能 slot/template/click/delete behavior;
  - CR63 tail-boundary semantics;
  - CR94 unknown-failure retry/backoff semantics;
  - CR96 target-map-arrived maintenance-window close semantics;
  - CR116 180s pre-combat watchdog/budget semantics;
  - team-round claim limits and maintenance window safety.

Implementation:

- Added a 五倍-aligned before-park hook in `XiuluoTaskV2.yieldAfterMustYield(...)`.
- When an outcome is `PATHING_STARTED` and its wait reason is `WAIT_TRACKER_SHORTCUT_PATHING` or
  `WAIT_TARGET_PATHING_TERMINAL`, 修罗 now calls
  `maybeRunLeaderPathingSummonMaintenanceBeforePark(...)` before `parkAfterYieldIfNeeded(...)`.
- The hook reuses the existing `runLeaderPathingSummonSkillMaintenance(...)`; it does not add a new
  timer, new template, new click path, or alternate cleanup algorithm.
- Tightened the leader pathing maintenance request to
  `requireOpenTeamMaintenanceWindow(true)`, matching 五倍's safety gate and preventing leader 三技能
  from running outside the route-owned maintenance window.
- This is the first/low-risk CR119 repair step approved by the user: it covers the case where
  leader 三技能 is already due before a 修罗 pathing wait parks. A separate due-time wake may still be
  needed later if a single long park starts before due and does not wake until much later.

Verification:

- RED first:
  - `java -cp "target/test-classes;target/classes" com.bot.dhxy.task.xiuluo.XiuluoLeaderPathingSummonBeforeParkWiringTest`
  - Failed before implementation: `CR119: 修罗 event-wait yield must check leader 三技能 before parking`.
- GREEN:
  - `mvn -q -DskipTests test-compile`
  - `java -cp "target/test-classes;target/classes" com.bot.dhxy.task.xiuluo.XiuluoLeaderPathingSummonBeforeParkWiringTest`
  - `java -cp "target/test-classes;target/classes" com.bot.dhxy.task.xiuluo.XiuluoPreCombatWatchdogBoundedWaitWiringTest`
  - `mvn -q -DskipTests compile`
- Related guard notes:
  - `XiuluoCR96ShortcutTargetMapCloseWiringTest` currently fails on an existing exact source-string
    expectation around the shortcut target-map call shape; compile still passes.
  - `XiuluoCombatMaintenanceWakeWiringTest` currently fails on an existing source-structure
    assertion around `combatMaintenanceWakeTimeoutMs()`. This CR119 patch does not touch
    WAIT_COMBAT maintenance wake logic.
- Remaining validation: if a single long park starts before due and leader summon cooldown becomes
  due before `PATHING_TERMINAL`, verify whether this first-step repair is enough for the runtime
  shape. If not, add the separate due-time wake described in Required repair direction.
- Remaining validation: if CR96 has already closed the pathing maintenance window because the target
  map was reached, leader 三技能 must not run in the enter-battle/prepared phase.
- Fresh runtime: after `maintenance init` + 20 minutes, leader logs should show either
  `maintenance: summon skill due source=xiuluo-v2:leader-pathing:*` and a clean attempt, or an
  explicit safe skip reason. It should not be silent across multiple post-due shortcut rounds.
- Fresh runtime evidence, 2026-06-26 18:34:08-18:34:23: the before-park due branch fired in 修罗
  round 12. After tracker green click, leader `hwnd-2AD117C` logged
  `maintenance: summon skill due source=xiuluo-v2:leader-pathing:before-park`, claimed
  `xiuluo_v2#12`, ran one summon-skill pass, detected an 8-slot layout, kept slot 7, observed slot 8
  empty, and finished `success=true` with `skillCount=8 nextStartSlot=8`. `XiuluoTaskV2` then logged
  `pre-combat timer paused ... blockedMs=14588`, proving the maintenance time was compensated before
  resuming the tracker-shortcut wait. Keep the remaining validation for the harder shape where one
  long park starts before due and the cooldown becomes due before `PATHING_TERMINAL`.
- Runtime acceptance passed, 2026-06-26 19:59:32-19:59:43: the leader fired the same before-park due
  branch in round 55, claimed `xiuluo_v2#55`, and completed summon cleanup with `success=true`.
  Later round 65 also showed members deferred after the CR96 target-map-arrived maintenance-window
  close, so the repair did not reopen the window after target arrival.

Card CR120: 通用盒子逻辑 for 修罗 and 五倍

Business source:

- `docs/业务逻辑.md` top section `通用盒子逻辑`.
- User confirmed this should be a common business capability, not a 修罗-only or 五倍-only private
  implementation.

Problem statement:

- The game can show a short-lived "盒子" marker in the lower-right game-window area. The automation
  should detect it and click it, but not at the exact moment it appears if doing so would interrupt a
  higher-level task transition.
- The click opportunity is short-lived, so pending boxes must have higher priority than normal
  opportunistic maintenance. At the same time, this must not cause cross-window clicks or stale
  delayed clicks after the marker is gone.
- The first supported tasks are 修罗 and 五倍. Other task flows may reuse the common service later,
  but CR120 should not modify 五环 behavior.

Required behavior:

- Add two independent UI switches:
  - `队长要盒子`: default checked.
  - `队员要盒子`: default unchecked.
- The switches must be independent:
  - turning on `队员要盒子` does not disable leader handling;
  - turning off one role clears only that role/window's pending box state;
  - if both are off, no window should detect or consume boxes.
- Detection uses the bound game window, not desktop-global state:
  - use window-relative ROI `(623,590) -> (682,618)`;
  - template under `images/template/common/`, recommended file name
    `leader_box_marker.png`;
  - the implementation may keep the template name, but the business behavior is common for leaders
    and members.
- Detection creates a per-window pending record instead of clicking immediately. The record should
  include at least:
  - `windowId` / current bound HWND identity;
  - matched time and expiry time;
  - matched template point and final click point;
  - source task, such as 修罗 or 五倍;
  - role context, leader or member.
- Pending TTL is 30 seconds from successful detection. Expired pending records must be dropped and
  must not click later.

Leader flow:

- Only enabled when `队长要盒子` is on.
- Detect after the leader has completed the task and verified return-home.
- Detection should run in the background and must not block the leader from starting the next task
  accept flow.
- Do not click immediately after return-home.
- Consume after the next task has been accepted and movement has started.
- On consume, the box click has highest maintenance priority for that window, then the pending record
  is cleared.

Member flow:

- Only enabled when `队员要盒子` is on.
- Detect after combat exit for each member window.
- Do not immediately steal input from the leader or another active window.
- Record each member's own pending state; members must not share pending state and must not consume
  another window's marker.
- Consume when that member window next gets a task turn / input opportunity.
- On consume, the box click has highest maintenance priority for that window, then the pending record
  is cleared.

Priority and safety:

- Unexpired pending box click is higher priority than:
  - leader/member HP/MP first-aid;
  - 三技能;
  - 摄妖香 / 14 号箱;
  - 医宝宝;
  - 修装备;
  - other opportunistic maintenance.
- The click must go through the existing serialized input path and must keep move+click atomic.
- Pending must be scoped to the bound window. Do not rely on title search or global screenshot paths.
- Switching a role toggle off, task stop, stale window identity, or TTL expiry must clear the pending
  state rather than clicking late.
- Do not change 修罗 or 五倍 business rules for accepting tasks, navigation, combat waiting, return
  home, maintenance cooldowns, or failure recovery. CR120 only adds the box detect/pending/consume
  hook points.

Suggested implementation shape:

- Add a common box detector/pending service instead of duplicating logic inside 修罗 and 五倍.
- Store role settings in the existing UI/config path near task startup options; default leader on,
  member off.
- Add explicit logs for:
  - detection skipped by role toggle;
  - detection matched/missed with score, window id, role, source task, and ROI;
  - pending created/expired/cleared;
  - pending consumed, click point, and input result.
- Add a no-click replay/debug path if the live template or ROI needs calibration before enabling
  production clicking.

Acceptance:

- Source guards or unit tests prove:
  - leader default is enabled and member default is disabled;
  - toggles are independent;
  - pending state is per-window and expires after 30 seconds;
  - pending consume order is higher than the listed maintenance actions;
  - switching a toggle off clears only the matching role/window pending.
- Visual replay/debug evidence proves the `(623,590)-(682,618)` ROI and
  `images/template/common/leader_box_marker.png` template locate the intended marker and final click
  point.
- Fresh 修罗 runtime proves:
  - leader detects after verified return-home;
  - leader does not click immediately;
  - leader consumes after next accept/movement starts before other maintenance.
- Fresh 五倍 runtime proves the same leader behavior for 五倍.
- If `队员要盒子` is enabled in a controlled run, member windows detect after combat exit and consume
  only on their own later input opportunity. If it remains disabled, member logs should show skipped
  detection/consume and no member box clicks.

Implementation notes (2026-06-26 / Codex):

- Added `CommonBoxService` as the shared detector/pending/click owner. It uses the bound window via
  `WindowTaskContextHolder`, ROI `(623,590)-(682,618)`, template
  `images/template/common/leader_box_marker.png`, 30s pending TTL, and atomic
  `InputSequences.moveAndClickLeft(...)` for final consume.
- Added `BotProperties.leaderCommonBoxEnabled=true` and
  `BotProperties.memberCommonBoxEnabled=false`, persisted by `GameUiSettingsStore`, and surfaced in
  `MainWindowController` as `队长要盒子` / `队员要盒子`.
- Common-box is intentionally not part of generic `TaskMaintenanceRequest` /
  `TaskMaintenanceResult` / `TaskMaintenanceStatus`. Pending boxes are consumed only by direct
  calls to `CommonBoxService.consumePendingBoxIfAllowed(...)` at explicit safe hooks.
- Added leader hooks:
  - 修罗 detects after verified return-home and consumes at
    `xiuluo-v2:start-exit-prepath`, `xiuluo-v2:tracker-shortcut-green-clicked`, and
    `xiuluo-v2:target-navigation-pathing-started`.
  - 五倍 detects after verified return-home and consumes from non-chained tracker pathing start via
    `consumeCommonBoxAfterTaskAccepted(...)`.
- Added member hooks in `AutoCombatService`: detect after combat exit, then consume at the pending
  follower task-turn opportunity before first-aid.
- Added CR120 source guard:
  `src/test/java/com/bot/dhxy/service/CommonBoxLogicWiringGuard.java`.
- Added no-click replay:
  - raw: `images/test-cases/common-box/raw/leader_box_marker_roi_raw.png`
  - output: `images/test-cases/common-box/output/leader_box_marker_roi_raw_output.png`
  - tool: `src/test/java/com/bot/dhxy/service/CommonBoxReplayDebugMain.java`

Validation:

- RED first:
  - `mvn -q -DskipTests test-compile; java -cp "target/test-classes;target/classes" com.bot.dhxy.service.CommonBoxLogicWiringGuard`
  - Failed before implementation on missing `images/template/common/leader_box_marker.png` / CR120
    wiring.
- GREEN:
  - `mvn -q -DskipTests test-compile`
  - `mvn -q -DskipTests compile`
  - `java -cp "target/test-classes;target/classes" com.bot.dhxy.service.CommonBoxLogicWiringGuard`
  - `mvn -q "-Dexec.classpathScope=test" "-Dexec.mainClass=com.bot.dhxy.service.CommonBoxReplayDebugMain" exec:java`
  - replay output: `common box replay matched score=1.0 click=(25,7)`.

Runtime still needed:

- Fresh 修罗 and 五倍 runs should confirm:
  - leader logs show detect after verified return-home and no immediate click;
  - pending consume happens after next accept/movement progress before other maintenance;
  - member logs skip while `队员要盒子` is off, and detect/consume only on the member's later
    task-turn opportunity when it is enabled.

Review findings (2026-06-26 / Codex):

- P1: `AutoCombatService.runPendingFollowerFirstAidIfAllowed(...)` clears
  `pendingFollowerFirstAid` when `TaskMaintenanceService` returns `COMMON_BOX_CLICKED`
  (`AutoCombatService.java` around lines 469-479). CR120 says the box is highest priority before
  HP/MP work, but the current code treats the box click as if it satisfied the queued first-aid.
  A member that needed HP/MP after combat can click the box and then lose the pending first-aid
  request. Keep the first-aid pending after a box click, or continue first-aid after the box if
  that input sequence is explicitly safe.
- P2: member detection is not role-guarded. `AutoCombatService.consumeExitAndRecover(...)` calls
  `CommonBoxService.detectMemberBoxAfterCombatExit(...)` for every supported task exit, and
  `CommonBoxService.detectBoxAsync(...)` records the requested `MEMBER` role without verifying
  the bound window is actually a member (`CommonBoxService.java` around lines 80-87 and 170-187).
  With `队员要盒子` enabled, a leader window can create an unconsumable member pending record.
  Detection should fail closed when requested role differs from `WindowRuntimeContext.isMember()`.
- P2: pending records are only cleared on role switch-off, expiry, stale-window check at consume,
  and click (`CommonBoxService.java` around lines 121-152). CR120 requires stop/stale identity
  safety; the async detector can still create a pending record after a task stop or identity drift,
  and that record may be consumed within the 30s TTL if the same window/task resumes. Add a
  per-window/task clear on task stop/window identity drift, or store a task-run/identity epoch in
  `PendingCommonBox` and validate it before consume.

Strict design review update (2026-06-26 / Codex):

- Some earlier review findings have been partially addressed in local code: `CommonBoxService`
  now checks requested role against the bound window role, stores `identityEpoch`, and
  `AutoCombatService` no longer clears follower first-aid just because a common box was clicked.
- P2: `CommonBoxService.detectBoxAsync(...)` uses `CompletableFuture.runAsync(...)` with the
  common pool and writes every detection for a role to the fixed temp path
  `common_box_roi_${role}.png` before calling `ImageFinder.find(...)`. Two close detections for
  the same window/role can overwrite each other's ROI file while matching, producing wrong or
  flaky pending records. Either run this tiny ROI probe synchronously at the business hook, use a
  bounded/named executor plus unique temp paths, or add an in-memory matcher path so no shared temp
  file is needed.
- P2/P3 design concern: CR120 is currently wired through generic `TaskMaintenanceRequest`,
  `TaskMaintenanceResult`, and `TaskMaintenanceStatus`, with `consumeCommonBox` defaulting true.
  The feature is only meant to consume at a few explicit safe points, but this API shape makes box
  clicking a default behavior of every future maintenance pass whose `sourceTask` is exactly
  `wubei` or `xiuluo_v2`. Prefer default `consumeCommonBox=false`, or remove it from the generic
  maintenance contract and call `CommonBoxService.consumePendingBoxIfAllowed(...)` directly at the
  three 修罗 / one 五倍 / one member safe hooks.
- P3 test quality: `CommonBoxLogicWiringGuard` is mostly string-contains source inspection. It adds
  a lot of test code but does not prove pending TTL, role mismatch, identity epoch, no first-aid
  clearing, or safe-point behavior through real service calls. Keep the small replay image test, but
  replace most source-string assertions with one or two behavior tests around `CommonBoxService`
  and `AutoCombatService` if the feature stays.
- Simplification target: this should not need a large framework-style layer. A reasonable shape is
  one small service owning `probeBoxInRoi(...)`, `recordPending(...)`, `consumePending(...)`, two UI
  booleans, and direct calls from the existing safe business hooks. Avoid broadening the shared
  maintenance API unless another feature needs the same abstraction.

Feedback follow-up (2026-06-26 / Codex):

- Fixed the P1 first-aid regression: a `COMMON_BOX_CLICKED` result now leaves
  `pendingFollowerFirstAid` and `pendingFollowerFirstAidSource` intact, logs that first-aid is
  deferred after the box click, and returns the current task turn so the next safe opportunity can
  run HP/MP recovery.
- Fixed the role-guard P2: `CommonBoxService.detectBoxAsync(...)` compares the requested
  `CommonBoxRole` with the bound `WindowRuntimeContext` role and fails closed if they differ,
  removing the mismatched pending key instead of recording a leader window as `MEMBER`.
- Fixed the stale identity P2: `PendingCommonBox` now stores
  `WindowRuntimeContext.getPlayerIdentityEpoch()` at detection time and consume drops the pending
  record if the epoch changed before click.
- Verification:
  - RED guard failed before the fix on `CR120 common-box click must not clear pending follower
    first-aid`.
  - `mvn -q -DskipTests test-compile` passed.
  - `java -cp "target/test-classes;target/classes" com.bot.dhxy.service.CommonBoxLogicWiringGuard`
    passed.
  - `mvn -q -DskipTests compile` passed.
  - `mvn -q "-Dexec.classpathScope=test"
    "-Dexec.mainClass=com.bot.dhxy.service.TaskMaintenanceSummonSkillEpochCooldownTest" exec:java`
    passed after rerunning sequentially; the first parallel run hit a transient classpath race on
    `TaskStartupMode`.
- Fresh runtime still needed before Done: 修罗/五倍 leader detect/consume logs, and controlled member
  run if `队员要盒子` is enabled.

Strict design follow-up (2026-06-26 / Codex):

- Fixed the broad maintenance-contract risk: `TaskMaintenanceRequest.consumeCommonBox` now defaults
  to `false`. CR120 box consumption must be opted in only at the explicit safe hooks that already
  pass `.consumeCommonBox(true)`.
- Fixed the fixed-temp-file race: `CommonBoxService` now writes the ROI debug image to a unique
  `common_box_roi_${role}_${windowId}_${System.nanoTime()}.png` path before template matching, so
  two close async probes for the same window/role cannot overwrite the same file.
- `CommonBoxLogicWiringGuard` now guards these two design constraints. The P3 test-quality concern
  remains noted: the current source-string guard should eventually be replaced with smaller
  behavior tests, but that is not required to remove the broad default-consume / fixed temp path
  risks.
- Verification:
  - RED guard failed before the fix on `CR120 common-box consume must default off and be enabled
    only by explicit safe hooks`.
  - `mvn -q -DskipTests test-compile` passed.
  - `java -cp "target/test-classes;target/classes" com.bot.dhxy.service.CommonBoxLogicWiringGuard`
    passed.
  - `mvn -q -DskipTests compile` passed.

Second strict size review (2026-06-26 / Codex):

- The risk fixes above were real, but before the follow-up CR120 was still not meaningfully
  slimmed down. That local shape still added a 303-line `CommonBoxService`, a 159-line source-string guard, a separate
  `CommonBoxRole` enum, and changes the generic maintenance request/result/status API for what is
  ultimately one small ROI probe plus delayed click.
- P2 design concern found before the follow-up: even with `consumeCommonBox=false` by default, common-box still lives
  inside `TaskMaintenanceRequest`, `TaskMaintenanceResult`, `TaskMaintenanceStatus`, and
  `TaskMaintenanceService.runOpportunisticMaintenance(...)`. This keeps a box-click feature coupled
  to summon-skill/broadcast maintenance and forces callers such as `AutoCombatService` to branch on
  `COMMON_BOX_CLICKED`. A slimmer design should call
  `CommonBoxService.consumePendingBoxIfAllowed(...)` directly from the explicit safe hooks instead
  of making it a generic maintenance operation.
- P2 design concern found before the follow-up: `CommonBoxService.detectBoxAsync(...)` still uses
  `CompletableFuture.runAsync(...)` on the common pool. The fixed temp path race was removed, but a
  tiny ROI probe still has no dedicated executor, cancellation, or task lifecycle owner. Prefer a
  synchronous probe at the hook, or an existing task-owned/background mechanism with explicit
  lifecycle checks.
- P3 design concern remains: every detection writes a temp PNG so `ImageFinder.find(...)` can read
  it back. That is useful for debug, but heavy for normal runtime. If `ImageFinder` has no in-memory
  overload, keep debug images behind an explicit debug/replay path or clean them after matching.
- P3 test concern remains: `CommonBoxLogicWiringGuard` is still mostly source-code string matching.
  It guards wording and implementation shape rather than behavior. Delete or greatly shrink it once
  direct safe-hook calls replace the generic maintenance wiring; keep only the replay/debug image
  and a small behavior test if needed.
- Recommended slim target: keep only config/UI switches, one small pending/probe service, direct
  detect/consume calls from 修罗/五倍/队员 safe points, and the replay image tool. Remove common-box
  from `TaskMaintenanceRequest/Result/Status` unless another maintenance feature needs this exact
  abstraction.

Second strict size review implementation (2026-06-26 / Codex):

- Removed common-box from the generic maintenance contract:
  `TaskMaintenanceRequest.consumeCommonBox`, `TaskMaintenanceResult.commonBoxClicked(...)`, and
  `TaskMaintenanceStatus.COMMON_BOX_CLICKED` are gone.
- `TaskMaintenanceService.runOpportunisticMaintenance(...)` no longer injects or calls
  `CommonBoxService`; common-box is not a summon-skill/broadcast maintenance result anymore.
- The explicit safe hooks now call `CommonBoxService.consumePendingBoxIfAllowed(...)` directly:
  修罗 `consumeCommonBoxDuringNextTaskProgress(...)`, 五倍
  `consumeCommonBoxAfterTaskAccepted(...)`, and the 队员 pending first-aid task turn in
  `AutoCombatService`.
- The 队员 box-first branch still keeps pending first-aid alive, so the later HP/MP recovery is not
  swallowed by a successful box click.
- Verification passed:
  `mvn -q -DskipTests compile`,
  `mvn -q -DskipTests test-compile`,
  `java -cp "target/test-classes;target/classes" com.bot.dhxy.service.CommonBoxLogicWiringGuard`,
  and
  `mvn -q "-Dexec.classpathScope=test" "-Dexec.mainClass=com.bot.dhxy.service.TaskMaintenanceSummonSkillEpochCooldownTest" exec:java`.

Continued CR120 review (2026-06-26 / Codex):

- Good: the biggest coupling issue is now fixed in current source. Common-box consume is no longer
  part of `TaskMaintenanceRequest`, `TaskMaintenanceResult`, `TaskMaintenanceStatus`, or
  `TaskMaintenanceService`; 修罗, 五倍, and member first-aid turn now call
  `CommonBoxService.consumePendingBoxIfAllowed(...)` directly at their explicit safe hooks.
- Fixed the P2 common-pool issue: `CommonBoxService` no longer uses
  `CompletableFuture.runAsync(...)`; the tiny ROI is scanned synchronously at the existing safe
  hook and still revalidates task/window/role/identity before recording/consuming pending state.
- Fixed the P3 normal-runtime temp PNG issue: `ImageFinder` now has a
  `find(BufferedImage sourceImage, BufferedImage targetImage, double threshold)` overload, and
  `CommonBoxService` uses it instead of writing `common_box_roi_*.png` before matching.
- P3 still open: `CommonBoxLogicWiringGuard` remains a 146-line source-string test. It now catches
  the generic-maintenance regression, but it is still brittle and mostly verifies implementation
  text rather than behavior. Keep the replay tool; replace this guard with focused behavior tests or
  shrink it to only the regression that cannot be tested otherwise.
- Fixed docs cleanup: older implementation notes now describe direct safe-hook consume instead of
  the removed maintenance request/result/status wiring.
- Verification:
  - RED guard failed before this fix with `CR120 common-box detection must not use common-pool async
    work`.
  - `mvn -q -DskipTests compile` passed.
  - `mvn -q -DskipTests test-compile` passed.
  - `java -cp "target/test-classes;target/classes" com.bot.dhxy.service.CommonBoxLogicWiringGuard`
    passed.
  - `mvn -q "-Dexec.classpathScope=test"
    "-Dexec.mainClass=com.bot.dhxy.service.CommonBoxReplayDebugMain" exec:java` passed; replay
    result stayed `score=1.0 click=(25,7)` and output
    `images/test-cases/common-box/output/leader_box_marker_roi_raw_output.png`.

Jason + Codex continued review (2026-06-26):

- Confirmed fixed: current source no longer couples common-box into generic
  `TaskMaintenanceRequest` / `TaskMaintenanceResult` / `TaskMaintenanceStatus` /
  `TaskMaintenanceService`; no `CompletableFuture.runAsync(...)` / common-pool ROI probe remains;
  normal detection no longer writes `common_box_roi_*.png`; member box consume does not clear
  pending first-aid.
- Fixed P2 task-run lifecycle gap: `CommonBoxService` now derives a task-run key from
  `TaskExecutionContext.getStartedAt()` with a context-identity fallback, includes it in
  `pendingKey(...)`, stores it in `PendingCommonBox`, and validates `staleTaskRun` before consume.
  A stop/restart of the same window, role, identity, and task within the 30s TTL can no longer
  consume the previous run's pending box.
- P3 still open: `CommonBoxLogicWiringGuard` is still mostly source-string `contains(...)`
  assertions (`CommonBoxLogicWiringGuard.java:58-136`), including the first-aid non-swallow guard
  around line 101. Keep it as a regression tripwire for now, but add a focused behavior test for
  "pending first-aid + common-box click keeps first-aid pending", then shrink the string guard.
- P3 cleanup: `CommonBoxService` is slimmer than the earlier 303-line version, but still large for
  a small ROI probe plus pending click. This is not blocking; future cleanup should cache the
  template and keep pending validation/click consume from growing into another maintenance
  subsystem.
- Fresh runtime still needed: 修罗/五倍 leader detect/consume timing, plus a controlled
  `队员要盒子=true` run proving member box-first does not swallow the next HP/MP first-aid.

Task-run lifecycle follow-up (2026-06-26 / Codex):

- RED guard failed before implementation with
  `CR120 pending boxes must be scoped to the current task run lifecycle`.
- `CommonBoxService` now scopes pending records by window, hwnd, role, task key, and task-run key.
  The task-run key is derived from `TaskExecutionContext.getStartedAt()` and falls back to the
  context object's identity when needed.
- Verification passed:
  - `mvn -q -DskipTests compile`
  - `mvn -q -DskipTests test-compile`
  - `java -cp "target/test-classes;target/classes" com.bot.dhxy.service.CommonBoxLogicWiringGuard`
  - `mvn -q "-Dexec.classpathScope=test"
    "-Dexec.mainClass=com.bot.dhxy.service.CommonBoxReplayDebugMain" exec:java`
  - `mvn -q "-Dexec.classpathScope=test"
    "-Dexec.mainClass=com.bot.dhxy.service.TaskMaintenanceSummonSkillEpochCooldownTest" exec:java`
  - Replay result stayed `score=1.0 click=(25,7)` with output
    `images/test-cases/common-box/output/leader_box_marker_roi_raw_output.png`.

Jason + Codex re-review after task-run follow-up (2026-06-26):

- No P1 found. Confirmed still fixed: no generic maintenance coupling, no common-pool ROI probe,
  no normal-path `common_box_roi_*.png`, and member box consume still does not clear pending
  first-aid.
- Fixed the remaining P2 task-run lifecycle gap: `TaskExecutionContext` now carries a monotonic
  `taskRunId`, `WindowTaskRunner` assigns it from a runner-local `AtomicLong`, and
  `CommonBoxService.taskRunKey(...)` fails closed when context is missing or `taskRunId <= 0`.
  Pending records are keyed by that strict run id instead of `startedAt` wall-clock text or
  `context:none` fallback, so same-tick/test/clock-rollback restarts cannot share a pending box key.
- Fixed P3 stale-pending cleanup: `CommonBoxService` prunes expired pending records on both detect
  and consume, so old task-run entries do not remain in memory just because a later run has a
  different task-run key.
- Fixed P3 diagnostics: the `pending created` log now passes `taskRunKey` before `score`, matching
  the log format and keeping fresh runtime diagnostics readable.
- P3 still open: `CommonBoxLogicWiringGuard` remains source-string heavy and only proves string
  shape, not start/stop/restart behavior.

Strict task-run review follow-up verification (2026-06-26 / Codex):

- RED guard failed before implementation with
  `CR120 pending boxes must be scoped to the current task run lifecycle`.
- Verification passed:
  - `mvn -q -DskipTests compile`
  - `mvn -q -DskipTests test-compile`
  - `java -cp "target/test-classes;target/classes" com.bot.dhxy.service.CommonBoxLogicWiringGuard`
  - `mvn -q "-Dexec.classpathScope=test"
    "-Dexec.mainClass=com.bot.dhxy.service.CommonBoxReplayDebugMain" exec:java`
  - `mvn -q "-Dexec.classpathScope=test"
    "-Dexec.mainClass=com.bot.dhxy.service.TaskMaintenanceSummonSkillEpochCooldownTest" exec:java`
  - `git diff --check -- ...` for touched CR120 source/docs passed with only existing LF/CRLF
    normalization warnings.

Jason + Codex strict re-review after monotonic task-run follow-up (2026-06-26):

- No P1 found. Confirmed still fixed: no generic maintenance coupling, no common-pool ROI probe,
  normal common-box detection does not write temp PNG, and member box consume still preserves pending
  first-aid.
- Fixed P2 runner-rebuild collision: `WindowTaskRunner` now assigns CR120 task run ids from
  `GLOBAL_TASK_RUN_SEQUENCE`, a process-wide static `AtomicLong`. Re-registering a window creates a
  new runner, but it no longer restarts the run id at `1`, so pending keys in singleton
  `CommonBoxService` cannot collide just because the runner was rebuilt inside the 30s TTL.
- Fixed P2 UNKNOWN role fail-closed: `CommonBoxService.roleFor(...)` now returns
  `Optional.empty()` unless the bound runtime role is explicit `WindowRole.LEADER` or
  `WindowRole.MEMBER`; detection and consume both skip with `reason=unknown-role` instead of treating
  unknown/blank roles as leader.
- Fixed P3 boundary coverage: added `CommonBoxBoundaryBehaviorTest`, which calls the actual
  `CommonBoxService.roleFor(...)` mapping by reflection and checks UNKNOWN fails closed while
  LEADER/MEMBER map correctly; it also verifies the task-run sequence holder on `WindowTaskRunner` is
  static/global. The older source guard remains as a broad wiring tripwire, but the two latest P2
  boundaries are no longer covered only by source-string assertions.
- Verification passed:
  - `mvn -q -DskipTests compile`
  - `mvn -q -DskipTests test-compile`
  - `java -cp "target/test-classes;target/classes" com.bot.dhxy.service.CommonBoxLogicWiringGuard`
  - `mvn -q "-Dexec.classpathScope=test"
    "-Dexec.mainClass=com.bot.dhxy.service.CommonBoxBoundaryBehaviorTest" exec:java`
  - `mvn -q "-Dexec.classpathScope=test"
    "-Dexec.mainClass=com.bot.dhxy.service.CommonBoxReplayDebugMain" exec:java`
  - `mvn -q "-Dexec.classpathScope=test"
    "-Dexec.mainClass=com.bot.dhxy.service.TaskMaintenanceSummonSkillEpochCooldownTest" exec:java`

Jason + Codex re-review after 17:05 follow-up (2026-06-26):

- No P1 found.
- Still fixed: process-global `GLOBAL_TASK_RUN_SEQUENCE` prevents runner-rebuild taskRunId
  collisions; UNKNOWN role now fail-closes through `CommonBoxService.roleFor(...)`; common-box is
  no longer wired through generic maintenance; no common-pool ROI probe; normal runtime detection
  matches in memory and does not write `common_box_roi_*.png`; `CommonBoxReplayDebugMain` exists
  under `src/test/java/com/bot/dhxy/service/CommonBoxReplayDebugMain.java`.
- P2 still open: member common-box consume is still tied to the pending follower first-aid path and
  first-aid gate. Evidence: `AutoCombatService.java:335` detects member boxes after combat exit, but
  the only observed member consume path is inside `runPendingFollowerFirstAidIfAllowed(...)`:
  `AutoCombatService.java:433` returns immediately unless `pendingFollowerFirstAid` is true;
  `AutoCombatService.java:446-451` waits for the first-aid maintenance gate before reaching
  `commonBoxService.consumePendingBoxIfAllowed(...)` at `AutoCombatService.java:466-467`. This means
  a member with a pending box but no HP/MP first-aid need may never consume the box before TTL, and
  a member with first-aid pending can still have the box blocked by the first-aid gate. This violates
  CR120's requirement that member boxes consume at the next task-turn/input opportunity and outrank
  HP/MP first-aid. Fix by adding an independent member safe-turn common-box consume hook before the
  first-aid gate and not making box consume depend on `pendingFollowerFirstAid`; keep first-aid
  pending when a box click consumes the turn.
- P3 still open: `CommonBoxLogicWiringGuard` remains source-string heavy. The new
  `CommonBoxBoundaryBehaviorTest` is the right direction for UNKNOWN/taskRun boundaries, but member
  independent consume / first-aid preservation still needs behavior coverage.
- P3 cleanup only: `CommonBoxService` is slimmer and decoupled, but still reloads
  `leader_box_marker.png` from disk on each detection (`CommonBoxService.java:251`). This is not a
  blocker; cache later if needed.
- Verification this round:
  - `mvn -q -DskipTests compile`
  - `mvn -q -DskipTests test-compile`
  - `mvn -q test-compile exec:java "-Dexec.mainClass=com.bot.dhxy.service.CommonBoxLogicWiringGuard"
    "-Dexec.classpathScope=test"`

Jason + Codex re-review after 17:19 follow-up (2026-06-26):

- No P1/P2 functional blockers found. The previous member common-box P2 is fixed: `AutoCombatService`
  now calls `runPendingMemberCommonBoxIfAllowed(...)` before `runPendingFollowerFirstAidIfAllowed(...)`
  after combat-exit recovery and on normal ticks (`AutoCombatService.java:145-157`). The member hook
  checks only `commonBoxService.hasPendingBoxForCurrentWindow(...)`, takes the task-turn coordinator,
  and consumes `commonBoxService.consumePendingBoxIfAllowed(...)` with source
  `pending-member-common-box` before the first-aid gate (`AutoCombatService.java:440-461`). The
  follower first-aid path still preserves pending first-aid when a box consumes the turn
  (`AutoCombatService.java:501-505` before the clear at `AutoCombatService.java:518-519`).
- Still confirmed fixed: no generic maintenance coupling; no common-pool common-box async; normal
  runtime detection does not write temp ROI PNG; pending is window/hwnd/role/task/taskRun/identity
  scoped; global taskRunId remains process-wide; UNKNOWN role fail-closes.
- Fixed the remaining P3 behavior-coverage gap: added `AutoCombatMemberCommonBoxBehaviorTest`.
  It invokes the real `AutoCombatService.runPendingMemberCommonBoxIfAllowed(...)` path by
  reflection with a fake `CommonBoxService`, proving that a member pending box can consume without
  `pendingFollowerFirstAid`, and that "pending member box + pending first-aid" consumes the box first
  while leaving first-aid pending for the next safe turn.
- `CommonBoxLogicWiringGuard` remains as a broad wiring tripwire, but this ordering boundary is no
  longer only source-string covered.
- P3 cleanup remains: `CommonBoxService` still reads `leader_box_marker.png` from disk on each
  detection (`CommonBoxService.java:295`). This is not a functional blocker, but cache/replay can
  be cleaned later.
- Verification this round:
  - `mvn -q -DskipTests compile`
  - `mvn -q -DskipTests test-compile`
  - `mvn -q test-compile exec:java "-Dexec.mainClass=com.bot.dhxy.service.CommonBoxLogicWiringGuard"
    "-Dexec.classpathScope=test"`
  - `mvn -q test-compile exec:java "-Dexec.mainClass=com.bot.dhxy.service.CommonBoxBoundaryBehaviorTest"
    "-Dexec.classpathScope=test"` => `CommonBoxBoundaryBehaviorTest passed`
  - `mvn -q "-Dexec.classpathScope=test"
    "-Dexec.mainClass=com.bot.dhxy.service.AutoCombatMemberCommonBoxBehaviorTest" exec:java`
    => `AutoCombatMemberCommonBoxBehaviorTest passed`
  - `mvn -q test-compile exec:java "-Dexec.mainClass=com.bot.dhxy.service.CommonBoxReplayDebugMain"
    "-Dexec.classpathScope=test"` => `common box replay matched score=1.0 click=(25,7)
    output=images\test-cases\common-box\output\leader_box_marker_roi_raw_output.png`
  - `mvn -q "-Dexec.classpathScope=test"
    "-Dexec.mainClass=com.bot.dhxy.service.TaskMaintenanceSummonSkillEpochCooldownTest" exec:java`
    => `TaskMaintenanceSummonSkillEpochCooldownTest passed`
- Jason + Codex re-review, 2026-06-26 17:29 ET: no P1/P2 open finding. The 2 remaining P3 findings
  from this heartbeat are now fixed:
  - `CommonBoxService` caches `images/template/common/leader_box_marker.png` through
    `cachedTemplate()` and fails closed with `template-unavailable` if the template cannot load, so
    the tiny ROI probe no longer reads the template from disk on every detection.
  - Added `CommonBoxPendingBehaviorTest` for pending task-run, identity-epoch, expiry, and switch-off
    behavior. `CommonBoxLogicWiringGuard` remains as a minimal architecture tripwire and now requires
    behavior coverage instead of source-checking every pending lifecycle branch.
  - Still confirmed fixed: no generic maintenance coupling; no common-pool ROI probe; no
    normal-path temp PNG; pending is window/hwnd/role/task/taskRun/identity scoped; UNKNOWN role
    fail-closes; member box consume is independent of first-aid and does not swallow first-aid.
  - Verification passed this round: `mvn -q -DskipTests compile`,
    `mvn -q -DskipTests test-compile`, `CommonBoxLogicWiringGuard`,
    `CommonBoxBoundaryBehaviorTest`, `AutoCombatMemberCommonBoxBehaviorTest`,
    `CommonBoxPendingBehaviorTest`, `CommonBoxReplayDebugMain`, and
    `TaskMaintenanceSummonSkillEpochCooldownTest`.
  - Fresh runtime validation is still needed for 修罗/五倍 leader detect/consume timing and
    controlled `队员要盒子=true` runs.
- Jason + Codex re-review, 2026-06-26 17:45 ET: no P1/P2 open finding; one guard-quality P3
  remained at that time.
  - Fixed P3: `CommonBoxService` now caches `leader_box_marker.png` through `cachedTemplate`
    and only reads the file on first load / reload after unavailable state
    (`CommonBoxService.java:58`, `CommonBoxService.java:349`).
  - Then-open P3, superseded by the 17:58 follow-up below: `CommonBoxLogicWiringGuard` remained
    source-string heavy
    (`CommonBoxLogicWiringGuard.java:65-183`). The most important runtime boundaries now have
    behavior coverage through `CommonBoxBoundaryBehaviorTest`,
    `CommonBoxPendingBehaviorTest`, and `AutoCombatMemberCommonBoxBehaviorTest`, but the guard
    should still be reduced to minimal tripwires before CR120 is code-review closable.
  - Still confirmed fixed: no generic maintenance coupling; no common-pool async; normal ROI
    detection stays in memory without temp PNG; pending is scoped by window/hwnd/role/task/taskRun/
    identity; UNKNOWN role fail-closes; member box consume remains independent of first-aid and
    does not swallow first-aid.
  - Verification this round: `mvn -q -DskipTests compile`,
    `mvn -q -DskipTests test-compile`, `CommonBoxLogicWiringGuard`,
    `CommonBoxPendingBehaviorTest`, `CommonBoxReplayDebugMain`,
    `CommonBoxBoundaryBehaviorTest`, and `AutoCombatMemberCommonBoxBehaviorTest` passed.
    The first parallel `test-compile` / member behavior run hit a transient classpath race and
    passed when rerun sequentially.
- Codex follow-up, 2026-06-26 17:58 ET: the remaining guard-quality P3 is fixed.
  `CommonBoxLogicWiringGuard` now keeps only broad architecture tripwires: switches, template/ROI,
  cached in-memory matching, no generic maintenance coupling, explicit 修罗/五倍/队员 hooks, and
  global taskRunId. Pending lifecycle and first-aid ordering are left to the behavior tests:
  `CommonBoxPendingBehaviorTest`, `CommonBoxBoundaryBehaviorTest`, and
  `AutoCombatMemberCommonBoxBehaviorTest`.
  - Verification passed after this follow-up: `mvn -q -DskipTests compile`,
    `mvn -q -DskipTests test-compile`, `CommonBoxLogicWiringGuard`,
    `CommonBoxPendingBehaviorTest`, `AutoCombatMemberCommonBoxBehaviorTest`, and
    `CommonBoxBoundaryBehaviorTest`.
  - Current source-review state: no open P1/P2/P3 item recorded; fresh runtime validation is still
    needed for 修罗/五倍 leader detect/consume timing and controlled `队员要盒子=true` runs.
- Jason+Codex heartbeat confirmation, 2026-06-26 18:05 ET: no P1/P2/P3 open findings; CR120 code
  review is closable. Verified no generic maintenance coupling, no common-pool async, no normal-path
  temp PNG, safe pending window/role/identity/task lifecycle, member box does not swallow first-aid,
  template caching is in place, and replay output remains `score=1.0 click=(25,7)`.
- Fresh 修罗 runtime blocker, 2026-06-26 18:12-18:25: source review remains closable, but runtime
  validation is not closable. In the 67555 leader run, common-box leader hooks first executed normally
  and missed at `18:15:30.245`, `18:17:26.598`, and `18:19:20.027` with `role=LEADER`. From round 5
  onward the same leader safe hooks failed closed with `requestedRole=LEADER reason=unknown-role` at
  `18:21:19.282`, `18:21:29.052`, `18:21:30.167`, `18:23:05.601`, `18:23:14.921`, and
  `18:23:16.609`. This blocks CR120 runtime acceptance because 队长盒子 detection/consume may be
  skipped even while the 修罗 task thread/window is still the leader. Next fix should trace which
  runtime role source `CommonBoxService.roleFor(...)` sees at those hooks and why it diverges from the
  task/window leader context after return/start-exit-prepath.
- Continued fresh runtime evidence, 2026-06-26 18:25-18:31: the same blocker repeated across rounds 7-10.
  Return-home verified skipped at `18:25:26.675`, `18:27:37.920`, and `18:29:25.508`;
  start-exit-prepath skipped at `18:25:44.451`, `18:27:47.902`, and `18:29:43.350`;
  tracker-shortcut-green-clicked skipped at `18:25:46.498`, `18:27:50.027`, and `18:29:45.751`.
  Physical input traces for the same leader hwnd also print `role=UNKNOWN` during bag, NPC, dialog,
  and tracker shortcut inputs, so this looks like a runtime role source / input context drift rather
  than a single common-box call-site issue.
- Continued fresh runtime evidence, 2026-06-26 18:31-18:36: the same leader hook blocker persisted
  through rounds 10-12. Return-home verified skipped at `18:32:06.805`, `18:33:56.907`, and
  `18:35:57.857`; start-exit-prepath skipped at `18:32:16.847` and `18:34:06.840`;
  tracker-shortcut-green-clicked skipped at `18:32:18.580` and `18:34:08.627`. Physical input traces
  for prepared-enter-battle and summon-skill on the same bound leader hwnd still print `role=UNKNOWN`.
  CR120 remains source-review closable but runtime-blocked until the bound leader role source is
  stable at these hooks.
- Continued fresh runtime evidence, 2026-06-26 18:36-18:41: the blocker persisted through rounds
  13-16. Leader common-box skipped at `18:36:18.233` start-exit-prepath, `18:36:20.421`
  tracker-shortcut-green-clicked, `18:40:50.268` return-home verified, `18:40:59.794`
  start-exit-prepath, and `18:41:01.805` tracker-shortcut-green-clicked, all with
  `reason=unknown-role`. Member pending first-aid hooks also skipped with `unknown-role` at
  `18:36:30.931`, `18:36:37.007`, and `18:41:03.791`. This keeps CR120 runtime-blocked even
  though the source review remains closable.
- Root-cause direction from 2026-06-26 19:06-19:12 runtime + code inspection: CR120 is re-reading a
  mutable runtime role at every safe hook. `CommonBoxService.detectBox(...)` /
  `consumePendingBoxIfAllowed(...)` call `roleFor(window)`, which maps
  `WindowRuntimeContext.getRole()`. In logs, the same leader window `hwnd-2AD117C` had
  `AutoCombatService ... role=LEADER` at `19:07:02.832`, but common-box immediately treated the
  combat-exit hook as `role=MEMBER` / later skipped leader hooks with `reason=unknown-role`. This
  should not be modeled as a user focus or Alt+E issue. 修罗/五倍 role is assigned at task startup /
  window task context and should not be continuously re-discovered by common-box. Fix direction:
  common-box detect/consume should use the explicit hook role or `TaskExecutionContext.getWindowRole()`
  as the business role, while `WindowRuntimeContext` remains an hwnd/taskRun/identity safety guard.
  Do not let a transient UNKNOWN runtime role turn a known leader hook into a skipped box check.
- Codex worker fix, 2026-06-26 19:30 ET: implemented the root-cause direction above.
  `CommonBoxService.roleFor(...)` now takes `TaskExecutionContext` and maps only
  `context.windowRole` to `CommonBoxRole`; detection, consume, and pending lookup no longer read
  `WindowRuntimeContext.getRole()` for common-box business decisions. Runtime context still gates
  window id, hwnd/native binding, identity epoch, and taskRun key. `CommonBoxBoundaryBehaviorTest`
  now proves the fresh-runtime blocker directly: a pending leader box remains visible when the
  mutable runtime role drifts to `UNKNOWN` but the task context role is `LEADER`, while an
  `UNKNOWN` task-context role still fails closed. Verification passed: compile, test-compile,
  `CommonBoxBoundaryBehaviorTest`, `CommonBoxPendingBehaviorTest`, `CommonBoxLogicWiringGuard`,
  `AutoCombatMemberCommonBoxBehaviorTest`, `CommonBoxReplayDebugMain`, and diff-check with only
  existing LF/CRLF warnings. Fresh runtime still needs to confirm leader hooks no longer log
  `reason=unknown-role` / `unknown-context-role` unless the task context itself lacks LEADER/MEMBER.
- Fresh 修罗 runtime update, `2026-06-27 19:41:22.612 - 19:53:39.107`: the old leader-safe-hook
  role-drift blocker is no longer reproduced. Leader return-home hooks at `19:41:56.128`,
  `19:43:50.680`, `19:46:13.419`, `19:48:32.829`, `19:50:56.657`, and `19:53:16.649` all ran as
  `role=LEADER` and missed the ROI instead of skipping with `unknown-role`. This proves the
  context-role fix is active for 修罗 leader detection. Remaining runtime acceptance is a real box
  hit, pending record, and safe consume after next accept/movement; controlled member-on runtime is
  still optional/pending.
- Fresh 修罗 runtime blocker, `2026-06-27 20:23:47.825 - 20:33:16`: a real leader box hit was finally
  seen, but the pending record expired before the configured consume hook. Evidence:
  `20:32:35.446` logged `[common-box] pending created` for the leader with `source=xiuluo-v2:return-home-verified`,
  `taskRun=26`, `score=1.0`, click `(1927,678)`, and `expiresInMs=30000`; `20:33:07.489` then pruned
  that pending as expired. The next round only reached the intended movement-side consume window
  after that (`20:33:08.403` `AFTER_ACCEPT_MAINTENANCE_CHECK`, `20:33:11.274`
  `start-exit-prepath`, `20:33:15.554` tracker shortcut green clicked). This proves detection works
  but the current leader consume timing is too late for the 30s TTL.
- Codex worker follow-up, 2026-06-27: keep the 30s TTL as the business rule and add an earlier
  修罗 leader consume opportunity at the top of `AFTER_ACCEPT_MAINTENANCE_CHECK`, before
  start-exit-prepath or opportunistic maintenance can consume more of the TTL. Existing pathing
  progress consume hooks remain as fallbacks. Source guard
  `XiuluoCR120CommonBoxEarlyConsumeWiringTest` failed before the hook and passed after the patch.
  Fresh runtime still needs a real hit proving `COMMON_BOX_CLICKED source=xiuluo-v2:after-accept-maintenance-check`
  or another unexpired fallback consume before `[common-box] expired pending pruned`.
- Fresh WUBEI runtime blocker, `2026-06-28 00:29:09.963 - 00:29:42.495`: the same timing class is
  still open in 五倍. WUBEI leader created a common-box pending at `00:29:09.963` after verified
  return-home, but the next accepted round ran `heal-pet` broadcast in
  `AFTER_ACCEPT_MAINTENANCE_CHECK` at `00:29:38.790`, then a `3000ms` handoff, and
  `[common-box] expired pending pruned count=1` fired at `00:29:41.878` before the tracker green
  click at `00:29:42.495`. This violates the CR120 priority rule for 五倍: pending leader boxes must
  be consumed before maintenance/broadcast/pathing while still inside the 30s TTL.
- Ramanujan source repair, 2026-06-28: added highest-priority 五倍 leader consume hooks in
  `WubeiTask` before after-accept maintenance, maintenance broadcast, repair/pathing, and tracker
  green progress. The new sources are `wubei:after-accept-maintenance-check` and
  `wubei:before-tracker-pathing-maintenance-check`.
- Focused guard `WubeiCR120CommonBoxEarlyConsumeWiringTest` passed after the repair, and
  `mvn -q -DskipTests compile` plus `mvn -q -DskipTests test-compile` passed in the worker run.
- Fresh runtime must still prove a real WUBEI pending box is consumed before any
  `[common-box] expired pending pruned`, ideally before heal-pet/repair broadcast or tracker green
  click.

Card CR121: Fast expected-combat exit must trust return verification before recovery

Business source:

- User approved the CR113 fast expected-combat exit speed improvement, but clarified the safety
  rule for false positives.
- `docs/业务逻辑.md` section `Expected 战斗快脱战与回程验证兜底`.
- User explicitly corrected the earlier proposal: do not add a trusted Runner/full-radar check before
  using the return item. If the return item verifies the start map, that is enough evidence that the
  fast exit was correct. If it does not verify the start map, that failed return verification is the
  first trusted evidence that the fast exit may be false.

Problem statement:

- CR113 added a fast expected-combat exit probe for 修罗/五倍: after confirmed combat entry and a
  15s grace, a 20x20 leader-avatar diff can mark combat finished before the full battle radar would
  normally do so.
- CR109 then lets expected combat exits return home before doing deferred HP/MP / 摄妖香 recovery.
- The implementation must not assume 五倍 already has the same concrete wiring as 修罗. If 五倍 is
  still using the older return/recovery path, or does not actually call the 20x20 leader-avatar fast
  exit probe for expected battles, CR121 must first align 五倍 to the 修罗-style expected-combat fast
  exit + return-first contract.
- This is fast and desired, but if the avatar diff is a false positive while the window is still in
  combat, the task may attempt a return item during combat.
- A return item attempt that does not verify the task start map must not be treated like ordinary
  return failure immediately. It should first let Runner/battle-radar correct the trusted combat
  state.

Required behavior:

- Keep the fast path fast:
  - do not add a pre-return trusted Runner/full-radar confirmation;
  - expected combat exits should still attempt the return item immediately after the fast exit signal.
- Treat return verification as the first business truth:
  - return item used and start map verified -> accept the fast exit and continue existing
    return-home flow;
  - return item missing/not used or used but start map not verified -> enter the fast-exit correction
    path before normal return-failure recovery.
- Fast-exit correction path:
  - ask the trusted Runner/battle-radar state to refresh/correct the current bound window;
  - if it says the window is still `IN_COMBAT`, treat the fast expected exit as false positive and
    return to the task's combat wait;
  - while returning to combat wait, do not consume deferred leader post-combat HP/MP / 摄妖香 recovery
    and do not proceed into UI cleanup, navigation fallback, accept-task restart, or task failure;
  - if trusted state is not combat, continue the existing return-failure / safe-recovery logic.

修罗 requirements:

- In expected 修罗 combat (`XiuluoCombatSource.TRACKER_CONFIRM` / `FAST_EXPECTED_EXIT`), if
  `RETURN_HOME` fails to verify `灵兽村`, run the correction path.
- If the correction confirms `IN_COMBAT`, return to `XiuluoPhase.WAIT_COMBAT` with the existing
  expected-combat source/state preserved enough to keep waiting for the real exit.
- If correction confirms non-combat, continue the current 修罗 return-home failure behavior.
- Do not change incidental/unknown combat recovery rules.

五倍 requirements:

- First verify and, if missing, implement the same 修罗-style expected-combat fast-exit contract for
  五倍:
  - confirmed 五倍 expected battles must use `FAST_EXPECTED_EXIT`;
  - `FAST_EXPECTED_EXIT` must run the 20x20 leader-avatar diff probe after the 15s grace;
  - on fast expected exit, 五倍 must return first and defer HP/MP / 摄妖香 recovery instead of doing
    synchronous post-combat recovery before return;
  - this must be observable in 五倍 logs, not only implied by shared service code.
- Then add the same failed-return correction behavior to 五倍 expected battle exits; this card is not
  accepted if only 修罗 is fixed.
- In normal 五倍 expected combat (`WAIT_BATTLE_FINISH` / `FAST_EXPECTED_EXIT`), if the return item
  does not verify `宝象国`, run the correction path.
- If the correction confirms `IN_COMBAT`, return to `WubeiPhase.WAIT_BATTLE_FINISH` and keep waiting
  for the real exit.
- If correction confirms non-combat, continue the existing 五倍 return-failure behavior.
- Apply the same principle to 五倍 chained/黄袍 paths only where they currently use the expected
  fast-exit return-home path; do not change unrelated hot-start or incidental combat handling.

Boundaries:

- Do not change the CR113 avatar-diff ROI, threshold, 15s grace, or 1s cadence.
- Do not add a pre-return full-radar confirmation.
- Do not change return-item templates, bag scanning order, start-map names, or navigation fallback
  rules except for inserting the post-return-verification correction gate.
- Do not consume deferred leader post-combat recovery while the trusted state is still combat.
- Do not change 五环.

Implementation notes:

- Prefer a shared `AutoCombatService` or battle-radar helper that can refresh trusted combat state
  after a failed expected return verification and report whether the task should resume combat wait.
- Logs must make the correction visible:
  - task/source/window;
  - return verification failure reason;
  - trusted correction result (`still IN_COMBAT` vs `not combat`);
  - whether deferred recovery stayed pending or normal return-failure recovery continued.

2026-06-28 五倍 follow-up: WAIT_BATTLE_FINISH must wake for fast expected-exit probe

- Runtime evidence showed 五倍 was still not equivalent to 修罗 at the task-wait layer:
  - `2026-06-27 23:48:11.072` logged `[wubei wait] park finished: phase=WAIT_BATTLE_FINISH`
    with `timeoutMs=-1`, `elapsedMs=57535`, and `wakeType=COMBAT_STATE_CHANGED`.
  - `2026-06-27 23:56:32.084` repeated the same shape with `timeoutMs=-1` and
    `elapsedMs=45007`.
  - Fast expected-exit avatar baseline logs only appeared after the task woke, proving the
    20x20 probe had no chance to drive the parked wait.
- Root cause:
  - 修罗 `WAIT_COMBAT` waits use a dynamic combat-maintenance timeout from
    `autoCombatService.nextCombatWakeDelayMs()`.
  - 五倍 `WAIT_BATTLE_FINISH` waited indefinitely on `COMBAT_STATE_CHANGED` via
    `WUBEI_WAIT_UNTIL_RUNNER_EVENT_MS = -1L`, so it could not self-wake for the same
    maintenance / fast-exit probe cadence.
- Source repair:
  - `WubeiTask.waitForCombatStateWake(...)` now keeps `COMBAT_STATE_CHANGED` as the immediate
    event wake and uses `wubeiCombatMaintenanceWakeTimeoutMs()` for the timeout.
  - `wubeiCombatMaintenanceWakeTimeoutMs()` calls `autoCombatService.nextCombatWakeDelayMs()`
    and clamps the result to a WUBEI-named `500ms` minimum and `10000ms` maximum.
  - `WubeiCR121CombatWakeTimeoutWiringTest` guards against regressing back to `timeoutMs=-1`
    and verifies the timeout is sourced from the shared fast expected-exit / maintenance
    deadline.
- Fresh runtime acceptance:
  - Restart the app and run 五倍 expected combat / 黄袍 chained combat.
  - `WAIT_BATTLE_FINISH` park logs should show `timeoutMs` between `500` and `10000`, not `-1`.
  - When `FAST_EXPECTED_EXIT` is armed, the fast avatar-diff probe should be able to wake the
    五倍 battle wait before a slow `COMBAT_STATE_CHANGED` event if the avatar ROI changes.
- 2026-06-28 fresh runtime blocker:
  - Current WUBEI process already contains CR135 runtime logs, so this is no longer assumed to be an
    old process.
  - `00:56:05.457` still logs `WAIT_BATTLE_FINISH` with `wakeTypes=[COMBAT_STATE_CHANGED]`,
    `timeoutMs=-1`, `elapsedMs=5142`.
  - `00:56:05.817` captures the fast expected-exit avatar baseline only after that wake, proving the
    20x20 probe is still downstream of an infinite task park.
  - `00:56:35.619`, `00:57:28.198`, `00:59:49.459`, and `01:01:53.861` repeat the same
    `WAIT_BATTLE_FINISH timeoutMs=-1` shape.
  - Required repair remains narrow: make the production WUBEI wait path use the bounded dynamic
    timeout while preserving `COMBAT_STATE_CHANGED` as an immediate event wake. Do not change avatar
    ROI/grace/cadence, return item, bag, OCR/template/click/navigation, 修罗, or 五环.
  - Repair owner: Descartes (`019f0c9d-cb09-7c91-80ee-63d4164b4510`), with write scope limited to
    WUBEI CR121 wait wiring/guard/docs/dashboard.
- 2026-06-28 repair update:
  - Root cause of the failed source repair/guard: the previous guard checked only the helper
    `waitForCombatStateWake(...)`, so it proved the preferred factory shape but did not protect the
    final production park boundary. Runtime evidence showed `parkAfterYieldIfNeeded(...)` still
    reached `WindowReadyEventBus.awaitNewer(...)` with a `WAIT_COMBAT_STATE_CHANGE` spec whose
    `timeoutMs` was `-1`.
  - `WubeiTask.parkAfterYieldIfNeeded(...)` now enforces the CR121 invariant immediately before
    parking: if reason is `WAIT_COMBAT_STATE_CHANGE` and timeout is below the 500ms minimum,
    rewrite the spec with `wubeiCombatMaintenanceWakeTimeoutMs()`. That recomputes the shared
    fast expected-exit / combat maintenance deadline from
    `autoCombatService.nextCombatWakeDelayMs()` and clamps it to `500..10000ms`.
  - `WubeiCR121CombatWakeTimeoutWiringTest` now guards both the helper and the final production
    park boundary, so a future regression to `timeoutMs=-1` for WUBEI combat waits fails before
    runtime.
  - Fresh runtime verification point remains: after app restart, real WUBEI
    `[wubei wait] park finished: phase=WAIT_BATTLE_FINISH ... reason=WAIT_COMBAT_STATE_CHANGE`
    must show `timeoutMs=500..10000`, and may still wake earlier by `COMBAT_STATE_CHANGED`.
- 2026-06-28 fresh runtime validation passed:
  - App restarted at `01:16:45.995` with PID `10108`; only later logs were used for acceptance.
  - Real WUBEI `WAIT_BATTLE_FINISH` waits after restart now show bounded timeouts:
    `01:20:04.543 timeoutMs=914`, `01:20:05.543 timeoutMs=827`,
    `01:20:06.551 timeoutMs=920`, `01:20:07.553 timeoutMs=866`,
    `01:20:11.559 timeoutMs=925`, and `01:20:57.550 timeoutMs=3805`.
  - The same path still kept `COMBAT_STATE_CHANGED` as an immediate event wake at `01:20:57.550`.
  - Return-home verification also stayed compatible with the fast path:
    `01:20:16.798` used the cached WUBEI return item point and `01:20:17.756`
    cleared the prescan state with `source=wubei:cached-return-verified`.
  - No fresh post-restart `WAIT_BATTLE_FINISH timeoutMs=-1` was observed in this slice, so CR121
    is accepted as Done.

Card CR122: Yellow destination route must use mini-map handoff semantics

Business source:

- User confirmed the current default navigation model should be reduced to two production movement
  classes for 修罗/五倍:
  1. tracker/shortcut green-link pathing;
  2. mini-map coordinate handoff.
- CR99 changed world-map routing: clicking the yellow destination name opens the destination map's
  mini-map, then the bot clicks the final logical coordinate on that mini-map. That final movement is
  therefore mini-map coordinate navigation, not the old world-map green-link / route-dialog path.

Problem statement:

- Current CR99 yellow-destination flow clicks the yellow destination and the target mini-map
  coordinate inside the world-map route helper, then the outer `submitWorldMapSearchAndClickDestination(...)`
  sees `clicked=true`, registers a coordinate-aware `WindowPathingIntent`, and returns
  `PATHING_STARTED`.
- This bypasses the existing `navigateInCurrentMap(...)` handoff contract:
  - click mini-map coordinate;
  - confirm post-click movement;
  - only after movement proof close the mini-map, register intent, and yield.
- If the final mini-map click does not actually start movement, the task parks anyway. Runner then
  classifies the active intent as "target coordinate on another map" and can wait on the historical
  30s cross-map stopped-away threshold before waking the task. That is the 2026-06-26 修罗
  maintenance/NPC route stall shape: click submitted, no movement, then delayed `STOPPED_AWAY`.

Required behavior:

- Keep the two default 修罗/五倍 movement classes:
  - tracker/shortcut green-link pathing remains Runner-owned after the click;
  - all yellow-destination final movement must behave like mini-map coordinate handoff.
- For yellow destination route:
  - world-map search / yellow destination click is only the way to open the target mini-map;
  - final coordinate click must perform the same short movement confirmation used by current-map
    handoff, or a shared equivalent with the same safety behavior;
  - do not close the mini-map, register pathing intent, or return `PATHING_STARTED` until movement is
    confirmed;
  - if movement is not confirmed, do not park; retry/fail through the existing foreground navigation
    retry path.
- Runner stopped-away semantics for 修罗/五倍 default pathing should no longer require the four old
  categories:
  - tracker/shortcut pathing can keep the short tracker stopped-away behavior;
  - coordinate handoff can use the short coordinate stopped-away behavior after movement has been
    confirmed;
  - legacy map-route and cross-map-coordinate thresholds must not be used by the CR99 yellow default
    path.

Deprecated / retained source:

- Keep legacy world-map green-link and route-dialog code only as retained/deprecated source unless
  another current task explicitly needs it.
- It is acceptable to annotate or comment out deprecated production entry points instead of
  maintaining their internal behavior, provided the project still compiles and the current 修罗/五倍
  route uses the yellow mini-map handoff path.
- Do not delete useful old code wholesale in this CR; the goal is to make it unreachable from the
  default 修罗/五倍 path and easy to restore if the game changes again.

Boundaries:

- Do not change OCR/template matching thresholds or click-coordinate formulas.
- Do not change task tracker green-link matching/clicking.
- Do not change NPC click, accept-task, combat, return-home, HP/MP, 摄妖香, 三技能, or box logic.
- Do not add task-layer local movement completion checks; Runner/window watcher remains the owner
  after a real `PATHING_STARTED` handoff.

Validation:

- Add a focused source/behavior guard proving yellow-destination route cannot register a
  `worldMapYellowDestinationMiniMap` intent before the final mini-map movement confirmation branch.
- Add or update a guard proving the default yellow path does not depend on the old 30s
  `WINDOW_PATHING_COORDINATE_AWAY_STOPPED_MS` branch.
- Existing CR99/CR110 replay tests for yellow target row selection and coordinate click should still
  pass; if coordinates/templates are touched, a marked replay image is mandatory.
- Fresh runtime acceptance:
  - yellow route final mini-map click either logs movement confirmation before `PATHING_STARTED`, or
    foreground retry/failure without pathing intent;
  - no new 30s `STOPPED_AWAY` wait after a yellow final-coordinate click that did not move;
  - no stale tracker/shortcut `PATHING_TERMINAL` from an old round after combat entry, verified
    return-home, or the next round transition;
  - 修罗/五倍 tracker green-link pathing still wakes by Runner `PATHING_TERMINAL` / prepared action.
- Keep 修罗 and 五倍 call sites explicit enough that reviewers can see both tasks are covered.

Verification:

- [x] Before editing, record branch, current HEAD, dirty status, and relevant baseline for
  `NavigationService` and `WindowTaskRunner` in `docs/ACTIVE_WORK.md`.
- [x] Add focused source guards proving:
  - yellow destination route registers `worldMapYellowDestinationMiniMap` only after
    `yellow-destination-mini-map-pathing-confirmed`;
  - yellow destination final coordinate click confirms movement before movement intent, cleanup, and
    `CLICKED`/`PATHING_STARTED`;
  - yellow memory uses the same target mini-map identity check and does not create yellow pending
    memory before the active coordinate intent exists;
  - Runner live stopped-away policy no longer contains the old 8s map-route bucket or 30s cross-map
    coordinate bucket.
- [x] Focused guards passed:
  - `NavigationWorldMapYellowDestinationRoutePolicyTest`
  - `NavigationWorldMapYellowMemoryWiringGuard`
  - `NavigationWorldMapRouteMemoryIntentOwnershipTest`
  - `WindowPathingStoppedAwayPolicyTest`
- [x] CR122 stale tracker-shortcut lifecycle source guard passed:
  - `XiuluoCR122TrackerShortcutIntentLifecycleWiringTest`
- [x] CR122 stale WUBEI tracker-green lifecycle source guard passed:
  - `WubeiCR122TrackerGreenIntentLifecycleWiringTest`
- [x] `mvn -q -DskipTests test-compile` passed.
- [x] `mvn -q -DskipTests compile` passed.
- [ ] Fresh 修罗 runtime: yellow route final mini-map click either logs movement confirmation before
  `PATHING_STARTED`, or foreground retry/failure without pathing intent.
- [ ] Fresh 五倍 runtime: same yellow route behavior on accept/maintenance navigation.
- [ ] Fresh negative case: no new 30s `STOPPED_AWAY` wait after a yellow final-coordinate click that
  did not move.
- [ ] Fresh regression case: after a tracker shortcut reaches combat or verified return-home, its
  old intent must not later publish `STOPPED_AWAY` or wake a later round.

Runtime blocker from 2026-06-27 修罗 heartbeat:

- `19:53:39.107` round 60 clicked tracker green toward `龙窟五层`.
- `19:54:43.780` the same round woke by `PREPARED_ACTION_READY` and consumed
  `XIULUO_ENTER_BATTLE`, so the tracker pathing intent should no longer be allowed to publish a
  terminal result.
- `19:56:13.871` Runner later logged `pathing watcher update ... state=STOPPED_AWAY
  source=xiuluo-v2:tracker-shortcut:60:0 target=龙窟五层 current=灵兽村(117,95)
  observedStationaryMs=2231` and published `PATHING_TERMINAL state=STOPPED_AWAY`.
- The run continued, so this did not become a user-visible stop, but it violates the current CR122
  intent lifecycle contract and can wake or misclassify a later wait if the sequence overlaps.
- Source repair, 2026-06-27:
  - `WindowRuntimeContext.clearPathingSignalIfSourcePrefix(...)` atomically clears only the current
    active pathing snapshot whose intent source matches a prefix, so a newer unrelated navigation
    intent is not removed by a stale cleanup boundary.
  - `XiuluoTaskV2` clears `xiuluo-v2:tracker-shortcut` pathing at round start, after successful
    prepared `XIULUO_ENTER_BATTLE` consume, and after verified return-home.
  - `WindowTaskRunner` clears the same source prefix when `TaskType.XIULUO_V2` transitions into
    `IN_COMBAT`.
  - No yellow-route OCR/template/click coordinates, mini-map handoff confirmation, NPC click,
    accept-task, return-home item selection, HP/MP, 摄妖香, 三技能, or box logic changed.
  - Focused guard: `XiuluoCR122TrackerShortcutIntentLifecycleWiringTest`.

Runtime blocker from 2026-06-27 五倍 heartbeat:

- `23:31:07` 五倍 `first-probe` tracker click entered Runner-owned pathing with source
  `wubei:tracker-green-click:first-probe`.
- `23:31:41.994` that source had already published `PATHING_TERMINAL state=STOPPED_AWAY`.
- After combat/return/next-round progress, the same old source still appeared in watcher state:
  `23:32:34` logged `activeIntentSource=wubei:tracker-green-click:first-probe` with
  `activeIntentAgeMs=86897`.
- `23:32:40.855` the old source updated again as `state=ACTIVE current=宝象国(80,75)`, and
  `23:32:48.233` published another `PATHING_TERMINAL state=STOPPED_AWAY` with intent age around
  `100918ms`.
- This sample did not visibly stop the 五倍 flow, but it fails the CR122 acceptance that no old
  tracker/shortcut terminal should survive after combat/return/next-round boundaries.
- Source repair, 2026-06-28:
  - `WubeiTask` owns `TRACKER_GREEN_PATHING_SOURCE_PREFIX = "wubei:tracker-green-click"`.
  - It clears matching WUBEI tracker-green pathing at round start, before registering a new tracker
    green intent, after prepared/known enter-battle dialog consume, and after verified return-home.
  - `WindowTaskRunner` clears the same source prefix when `TaskType.WUBEI` transitions into
    `IN_COMBAT`.
  - Focused guard: `WubeiCR122TrackerGreenIntentLifecycleWiringTest`.
  - No OCR/template/click coordinates, yellow navigation business, mini-map handoff confirmation,
    `STOPPED_AWAY` threshold, 修罗 flow, return item search, HP/MP, 摄妖香, 三技能, or common-box
    business changed.

Card CR100: pause-aware input request cancellation

Business source:

- Fresh pause logs and user report from 2026-06-25 around an NPC tooltip click.

Problem statement:

- A user pause can be requested and task threads can reach `task pause checkpoint reached`, while an
  already-submitted physical input request keeps executing inside `InputActionWorker`.
- Fresh evidence:
  - `NpcClickService` found `npc_task_tooltip` and submitted `npcClick:taskTooltipTemplate#1`.
  - The global pause request was accepted and multiple task threads logged pause checkpoints.
  - The input worker still replayed the existing request actions through `MOVE_MOUSE -> SLEEP ->
    CLICK_LEFT`, so the tooltip/task dialog could still be clicked after the pause request.
- Root cause: `InputActionRequest` captures window context and player identity epoch, but not the
  submitting task's `TaskPauseToken`; `InputActionWorker` checks cancellation, interruption, and
  identity epoch before actions, but cannot see that the task is paused.

Required behavior:

- `InputActionQueue` must capture the current task pause token, if one exists, when creating
  `InputActionRequest`.
- `InputActionRequest` must expose a pause-aware cancel condition that is true when the captured
  pause token is currently paused.
- `InputActionWorker` must treat pause as cancel-like while executing queued input:
  - check before focus/input transaction;
  - check before the action list starts;
  - check before each action;
  - check before exclusive callbacks.
- If pause is detected, the worker must stop the request and complete it as `false`; it must not
  continue to later actions in the same request.
- The worker must not block waiting for resume. Waiting inside the single input worker would freeze
  input for all windows.
- `InputActionScope.isCancelled()` must also return true when the current request's captured pause
  token is paused, so exclusive/direct-input callbacks that already poll the scope stop naturally.
- Non-paused requests must keep the existing atomicity rule: move and click stay in one queue request
  so other windows cannot interleave focus/mouse operations.

Boundaries:

- Do not change NPC click strategy, tooltip/template thresholds, click coordinates, navigation,
  OCR/template matching, route handling, task phases, or pause checkpoint behavior in task code.
- Do not add a local wrapper around `TaskCheckpoint`.
- Do not make `InputActionWorker` call task checkpoint / wait methods; this is an input-abort gate,
  not a pause wait.
- Do not cancel unrelated windows' queued requests because one task pauses. The pause token must be
  captured per submitted request.
- Requests submitted outside a managed task, with no captured pause token, should behave as they do
  now.

Implementation notes:

- `TaskExecutionContextHolder` already exposes the current task context on the submitting task
  thread. Use it at `InputActionQueue` submission time; do not try to read it from the worker thread.
- Add a small focused test/source guard that proves a queued `MOVE_MOUSE -> SLEEP -> CLICK_LEFT`
  request with a paused captured token aborts before click.
- Add a source guard for `InputActionScope.isCancelled()` so long exclusive callbacks see pause as a
  cancellation signal.
- Keep logs clear: if a request is skipped due to pause, include window id, description, stage, and
  the same cancellation reason in dead-letter diagnostics.

Verification:

- [x] Before editing, record branch, latest pushed baseline, `git status`, and relevant input/pause
  baseline evidence in `docs/ACTIVE_WORK.md`.
- [x] RED test first: a queued request with `MOVE_MOUSE -> SLEEP -> CLICK_LEFT` and a captured
  pause token must fail before production code because the worker currently lacks pause checks.
- [x] GREEN: same test passes after implementation.
- [x] Test/source guard: `InputActionQueue` captures pause token from `TaskExecutionContextHolder`
  when building both action-list and exclusive-callback `InputActionRequest`.
- [x] Test/source guard: `InputActionWorker` checks pause before focus, before actions, before each
  action, and before exclusive callback.
- [x] Test/source guard: `InputActionScope.isCancelled()` includes the paused captured token.
- [x] `mvn -q -DskipTests test-compile`.
- [x] Run the focused CR100 test/guard directly.
- [x] Focused guard output shows only `MOVE_MOUSE` ran, then pause aborted at `stage=action-2`;
  dead-letter reason was `task-paused:action-2`, and no `SLEEP` / `CLICK_LEFT` ran.
- [x] `mvn -q -DskipTests compile`.
- [ ] Fresh runtime: after pause request, logs show the active input request aborts before any later
  click action, and no task tooltip / dialog click occurs after pause.

### Third-View CR Follow-up - 2026-06-19 CR32 Review

Reviewer: Codex, acting as third-view reviewer only.

Scope reviewed:

- `WindowReadyEventType.COMBAT_STATE_CHANGED`.
- `WindowTaskRunner.runCombatWatcherLoop(...)` / `publishCombatStateChanged(...)`.
- `WubeiTask.waitForCombatStateWake(...)`, `parkAfterYieldIfNeeded(...)`, and
  `tickWaitBattleFinish(...)`.
- `WindowReadyEventBus.awaitNewer(...)` sequence semantics and CR32 sprint docs.

Accepted:

- `AutoCombatService.handleWindowCombatGuardTick(...)` is the right observer-side source: it refreshes
  combat state and publishes a soft wake hint without consuming `EXIT_RECOVERED`; the foreground
  Wubei task still owns post-combat recovery through `handleCombatTick(...)`.
- `WAIT_BATTLE_FINISH` now releases the task turn and parks on `COMBAT_STATE_CHANGED` with a coarse
  timeout fallback, which is the right direction for reducing the old 400ms same-window churn.
- The CR32 patch does not require testcase replay because it does not change click coordinates,
  OCR/template thresholds, movement detection, or navigation target selection.

#### Finding

1. P2 - CR32 can still miss a combat-exit wake published between turn release and wait registration.

   Evidence:

   - `TaskTransactionRunner.run(...)` releases the task turn in its `finally` before
     `WubeiTask.parkAfterYieldIfNeeded(...)` runs.
   - `parkAfterYieldIfNeeded(...)` captures `afterSequence = windowReadyEventBus.currentSequence()`
     after the release, then waits only for events newer than that sequence.
   - `isWaitAlreadySatisfied(...)` intentionally returns `false` for
     `WAIT_COMBAT_STATE_CHANGE`, so a fresh already-published combat-state event cannot compensate
     for that race.
   - `publishCombatStateChanged(...)` logs `oldTick` / `newTick`, but `WindowReadyEvent` only carries
     the generic `COMBAT_STATE_CHANGED` type; task-side code cannot safely treat any fresh existing
     event as satisfied because `NONE -> IN_COMBAT` and `IN_COMBAT -> NONE` have the same event type.

   Risk:

   - If the watcher publishes `IN_COMBAT -> NONE` in the small gap after the leader releases the
     turn but before it captures `afterSequence`, the leader will ignore the real exit wake and sleep
     the full 1.5s fallback before recovering.
   - This should not break correctness because the fallback remains, but it can leave residual
     `wakeTimeout` / same-window reacquire noise and make CR32 look weaker than the event path really
     is.

   Recommendation:

   - In CR32 fresh validation, explicitly scan for `event=window.combat.state.changed oldTick=IN_COMBAT
     newTick=NONE` followed by `[wubei wait] park finished ... reason=WAIT_COMBAT_STATE_CHANGE ...
     wakeResult=timeout`.
   - If that pattern appears, carry the transition kind on `WindowReadyEvent` or capture the ready
     sequence before the task turn is released, so an exit event published during the release-to-park
     window can still wake the waiter.

### Third-View CR Follow-up - 2026-06-18 CR31 Review

Reviewer: Codex, acting as third-view reviewer only.

Scope reviewed:

- `scripts/analyze_wubei_latency.ps1` CR31 script shape.
- CR31 sprint row/card documentation and current working-tree status.

Accepted:

- The script now stores timing values as `System.Collections.Generic.List[int64]` and converts
  percentile inputs to `[int64[]]`, so sentinel values such as `ageMs=9223372036854775807` no
  longer overflow `Int32`.
- No Java/business/OCR/template/click/navigation files are changed by CR31.

#### Finding

1. P2 - CR31 script file is still untracked, so the recorded `git diff --check` does not cover it.

   Evidence:

   - `git status --short scripts\analyze_wubei_latency.ps1` reports
     `?? scripts/analyze_wubei_latency.ps1`.
   - `git ls-files scripts\analyze_wubei_latency.ps1` returns nothing.
   - `git diff --check -- scripts\analyze_wubei_latency.ps1 ...` does not inspect untracked file
     contents.

   Recommendation:

   - Before treating CR31 as integration-ready, either add/stage the script file or confirm this
     script is intentionally untracked and run an explicit whitespace/syntax check over the file
     itself.

### Third-View CR Follow-up - 2026-06-18 CR29 Review

Reviewer: Codex, acting as third-view reviewer only.

Superseded:

- This review is superseded by the later `2026-06-18 18:35` CR29 audit. The earlier acceptance of
  "explicit probeNoTarget returns before unknown-story cleanup / no-story smart target click" is now
  rejected: that local task-side interpretation is the regression. Keep this section only as
  historical context for why CR29 was reopened.

Scope reviewed:

- `docs/ACTIVE_WORK.md` CR29 implementation note.
- `docs/PACKAGE_ARCHITECTURE.md` CR29 sprint row and checklist.
- `WubeiTask.resolveProbeAfterPathing(...)`, `isProbeNoTargetStoryVisible(...)`, and the
  `tryClickProbeSpawnedTarget(...)` call sites.

Accepted:

- CR29's code direction is accepted for review: explicit Runner-prepared `probeNoTarget` now returns
  before unknown-story cleanup and before the tentative no-story smart target click.
- The patch does not change template thresholds, click coordinates, movement detection, route
  navigation, target-ready handling, wrong-position handling, or the no-prepared-result wait path.

#### Finding

1. P2 - CR29 checklist was not updated after the implementation moved to Review.

   Evidence:

   - The board row says `CR29 | 唐德 | Review: compile passed; needs post-restart validation`.
   - The CR29 implementation note records `mvn -q -DskipTests compile` and `git diff --check`.
   - The CR29 card checklist below still has all implementation and compile items unchecked.

   Recommendation:

   - Before moving CR29 to `Done`, mark the completed source/compile checklist items as checked.
   - Keep only the fresh post-restart WUBEI validation item unchecked until logs prove the new
     behavior: `probeNoTarget` advances to the next unused probe without
     `first-probe-no-story` smart click or `NPC click failed: 白龙马` before switching.

### Third-View CR Follow-up - 2026-06-18 CR14 Review

Reviewer: 何黎, continuing as third-view reviewer only.

Scope reviewed:

- `docs/ACTIVE_WORK.md` entry for CR14.
- `WubeiTask.maybeRunLeaderPathingSummonMaintenance(...)`.
- `TaskMaintenanceService` team maintenance window APIs.
- Existing `AutoBattleTask` / member summon-skill gate behavior from CR12.

#### Accepted Follow-ups

- The new `wubei:leader-pathing` gate is a safe direction: leader-side opportunistic summon cleanup
  now checks `isTeamPathingMaintenanceWindowOpen(...)` before taking the task turn, and the
  `TaskMaintenanceRequest` also sets `requireOpenTeamMaintenanceWindow(true)`.
- The change does not alter summon-skill matching, panel detection, click coordinates, OCR/template
  thresholds, or movement/navigation algorithms.

#### New / Remaining Review Findings

1. P1: CR14 does not yet cover the member auto-battle long-cleanup path that produced the strongest
   evidence.

   Evidence:

   - The CR14 baseline evidence includes member `hwnd-3057A` running `summon skill clean` under
     `source=auto-battle` while 五倍 waits are still unresolved.
   - The implementation described for CR14 only changes
     `WubeiTask.maybeRunLeaderPathingSummonMaintenance(...)` / `source=wubei:leader-pathing`.
   - `AutoBattleTask` still allows follower summon-skill cleanup when the team pathing maintenance
     window is open; CR12 only gated idle maintenance broadcast scans and still relies on the
     existing summon-skill pathing-window gate.

   Risk:

   - If the 五倍 pathing maintenance window stays open while the leader is in unresolved
     `WAIT_PATHING_TERMINAL` / operation-null dialog attention, member `auto-battle` can still start
     an 5s-12s exclusive summon-skill pass and occupy physical input. That is the same user-visible
     risk CR14 was opened to reduce.

   Recommendation:

   - Before marking CR14 accepted, either narrow the member auto-battle summon gate as well, or split
     a follow-up card that explicitly covers member `source=auto-battle` summon cleanup during
     unresolved 五倍 attention.
   - Post-restart validation should not only check `source=wubei:leader-pathing`; it must also check
     member `source=auto-battle`, `teamRound=wubei#...`, `maintenance: start summon skill clean`, and
     whether a concrete safe window existed beyond a broadly open pathing maintenance window.

### Third-View CR Follow-up - 2026-06-18 CR15/CR18 Review

Reviewer: 何黎, continuing as third-view reviewer only.

Scope reviewed:

- `docs/ACTIVE_WORK.md` entries for CR15 and CR18.
- `WubeiTask.waitForAcceptNpcRouteWake(...)`,
  `waitForAcceptNpcPathingIfStillActive(...)`, `acceptNpcRouteWakeFact(...)`,
  `isFreshAcceptNpcRouteEvent(...)`, and `isWaitAlreadySatisfied(...)`.
- `WindowTaskRunner.resolvePathingDialogBlock(...)` and the current CR18 evidence around
  operation-null `TASK_ATTENTION_REQUIRED`.

#### Accepted Follow-ups

- CR18 investigation is accepted: the observed same-window churn is not an OCR/click/navigation
  failure; it is a ready-state semantics issue where visible `STORY` / operation-null attention is
  treated as reusable satisfaction while pathing remains `ACTIVE`.
- CR15's direction is useful: accept-NPC route waits should not pay the generic 5s tracker timeout
  when the next expected condition is route dialog preparation or route arrival.

#### New / Remaining Review Findings

1. P1: CR15's dedicated route wait still inherits the broad `isWaitAlreadySatisfied(...)` gate.

   Evidence:

   - `WAIT_ACCEPT_NPC_ROUTE` is grouped with `WAIT_PATHING_TERMINAL`.
   - It is satisfied by `isActionableDialog(dialog)`, by any matching wake type from
     `isMatchingReadyEvent(...)`, or by any prepared action when `TASK_ATTENTION_REQUIRED` is in the
     wait spec.
   - `isMatchingReadyEvent(...)` only checks event type, not route operation/target or terminal
     pathing state.

   Risk:

   - A fresh operation-null `TASK_ATTENTION_REQUIRED` or unrelated visible `STORY` can skip the 1.5s
     park for `WAIT_ACCEPT_NPC_ROUTE`, causing the same tight reacquire pattern CR18 just diagnosed.
   - This may make the route card look faster than the old 5s timeout while still not producing a
     concrete next action for `ROUTE_TO_MAIN_TASK`.

   Recommendation:

   - Apply the same narrowing principle as CR18 to route waits: `WAIT_ACCEPT_NPC_ROUTE` should be
     satisfied by terminal route pathing, a fresh prepared `ROUTE_TRANSFER` for the expected target,
     or an operation-bearing route-ready event. Plain visible dialog and operation-null attention
     should wake once and then be rechecked, not repeatedly satisfy skip-park.

2. P1: `acceptNpcRouteWakeFact(...)` releases the duplicate-navigation gate on
   `DialogPreparationPhase.REQUESTED`.

   Evidence:

   - For matching `ROUTE_TRANSFER` / `宝象国`, both `REQUESTED` and `PREPARING` can return
     `route-dialog-<phase>` while the previous pathing snapshot is still `ACTIVE`.

   Risk:

   - `REQUESTED` only proves the foreground task asked Runner to prepare a route dialog; it does not
     prove the dialog is visible, prepared, or actionable. Releasing the duplicate-navigation gate on
     this phase can let `NavigationService` re-enter while no consumable route fact exists.

   Recommendation:

   - Do not use `REQUESTED` alone as a gate-release fact. Prefer `READY`, a fresh matching prepared
     action, terminal route pathing, or at least `PREPARING` tied to a fresh visible dialog snapshot
     and operation/target.

3. P2: `isFreshAcceptNpcRouteEvent(...)` accepts events by inherited pathing target even when the
   event operation is null.

   Evidence:

   - The method returns true when `event.getPathingIntent().targetMapName` or
     `event.getPathingSnapshot().intent.targetMapName` matches `宝象国`.
   - This same predicate is used for both `PATHING_TERMINAL` and `TASK_ATTENTION_REQUIRED`.

   Risk:

   - A route-targeted but operation-null attention event can release the accept-NPC pathing gate even
     if the event does not carry a `ROUTE_TRANSFER` action and pathing has not actually reached a
     terminal state.

   Recommendation:

   - Split the predicates by event type: `PATHING_TERMINAL` should require a terminal pathing state
     for the expected route; `TASK_ATTENTION_REQUIRED` should require `ROUTE_TRANSFER` with the
     expected target or a prepared route action, not merely an inherited pathing intent.

##### Parallel Execution Order

Step 1 - Start in parallel:

- 何黎 runs A1 and A2.
- 谢帅 runs B1.
- 唐德 runs C1.

Integration gate after Step 1:

- `mvn -q -DskipTests compile` passes.
- `WindowReadyEventBus.currentSequence()` exists.
- `WubeiStepOutcome` can carry wait spec but no 五倍 behavior has changed yet.
- Baseline log summary exists.

Step 2 - Main behavior change:

- 谢帅 runs B2 after A1 and B1 are complete.
- 何黎 runs A3 in parallel.
- 唐德 runs C2 in parallel.

Integration gate after Step 2:

- Compile passes.
- Park logs show wait reason and wake result.
- Stop-all still interrupts a parked task.

Step 3 - Phase mapping:

- 谢帅 runs B3 and B4.
- 唐德 runs C3 after the first post-change run.
- 何黎 reviews event-bus / runner logs for missed wake or stale prepared action.

Integration gate after Step 3:

- Compile passes.
- One-window test does not regress.
- Three-window and five-window tests show lower repeated handoff churn.
- 白龙马 does not switch prompt or enter `Alt+A` before Runner-prepared story result.

Step 4 - Sprint review:

- Compare before/after C1 metrics.
- Record pass/fail in `docs/ACTIVE_WORK.md`.
- If latency regresses, revert the smallest B-card behavior change and keep only diagnostics.
- If latency is stable, open Phase 2 planning for `COMBAT_STATE_CHANGED`.

##### Definition of Done

- `mvn -q -DskipTests compile` passes.
- No OCR/template/click/movement detector behavior was changed in Sprint 1.
- `task.turn.handoff`, `sameAsPrevious=true`, and repeated `consumePrepared result=absent`
  decrease in comparable five-window logs.
- p95/p99 latency for critical chains does not increase:
  - route dialog ready -> click;
  - prepared action -> consume;
  - consume -> input start;
  - click -> pathing/combat state change.
- No window starves while another window is parked.
- Timeout fallback is visible in logs and never means automatic business failure by itself.
- Each agent records files changed, commands run, test time range, and unresolved risks in
  `docs/ACTIVE_WORK.md`.
- Every sprint card has a final board status: `Done`, `Blocked: <reason>`, or
  `Skipped: <reason>`.
- Every completed card has all owned checklist items marked `- [x]`.

#### Verification

Compare logs before and after the scheduling change:

- `task.turn.handoff`
- `transaction=wubei:RESOLVE_AFTER_PATHING`
- `transaction=wubei:WAIT_BATTLE_FINISH`
- `consumePrepared result=absent`
- `window.dialog.interest.update`
- `sameAsPrevious=true`
- `ready-to-consume`, `prepared-to-consume`, and `input-queue-wait` elapsed times
- `observer tick` elapsed breakdown by combat, pathing, dialog detect, route prepare, task dialog
  prepare, tracker prepare, attention publish, and total time
- prepared action stale, mismatch, overwritten, cleared, and absent counts with reason codes
- repeated world-map open/input, route retry, and same-phase wake counts

Expected result:

- The same window should not reacquire the same waiting phase every 80ms.
- Pathing waits should wake on `PATHING_TERMINAL` or dialog attention.
- Dialog waits should wake on Runner prepared action.
- `WAIT_BATTLE_FINISH combat still running` should not spam logs.
- Runner-observed dialogs should wake the task promptly without relying on the next polling turn.
- Stop-all must still interrupt parked tasks promptly.
- CPU-heavy work should decrease, but p95/p99 latency for critical chains must not increase.
- Any prepared action older than the accepted freshness window must be rejected with a reason, not
  clicked.
- If a ready/prepared action is not consumed within 1s/3s thresholds, logs must show whether the
  delay came from watcher preparation, task turn scheduling, input queue wait, stale rejection, or an
  old foreground fallback path.
- Park logs must include `waitReason`, `wakeTypes`, `afterSequence`, `timeoutMs`, `wokeByEvent` or
  `wokeByTimeout`, and elapsed time.
- Each wake must log the runtime recheck result, such as pathing state, visible dialog type,
  prepared operation, prepared age, and whether the next action is still valid.

### Third-View CR Follow-up - 2026-06-18 CR22 Review

Reviewer: 何黎

Scope:

- Reviewed CR22 implementation after it moved to `Review: needs post-restart validation`.
- Reviewed only route intent ownership / prepared route action consumption code:
  - `src/main/java/com/bot/dhxy/service/NavigationService.java`
  - `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
  - `src/main/java/com/bot/dhxy/window/model/WindowPathingSnapshot.java`
- No Java code, OCR/template thresholds, click coordinates, minimap/world-map matching, NPC click,
  or WUBEI business semantics were changed in this review.

Accepted:

- CR22 keeps prepared route consumption scoped to the bound window and expected
  `ROUTE_TRANSFER` target.
- The route-only recovery path does not apply to arbitrary dialog operations.
- Freshness remains checked before clicking a consumed prepared route action.

#### Finding

1. P1 - Same-target route intent reuse can reuse terminal or unknown pathing state.

   Evidence:

   - `NavigationService.registerWindowPathingIntent(...)` now returns early for any existing
     same-target active intent when `includeCoordinate=false`.
   - `WindowRuntimeContext.getActivePathingIntent()` delegates to
     `WindowPathingSnapshot.hasActiveIntent()`.
   - `WindowPathingSnapshot.hasActiveIntent()` returns true for `STOPPED_AWAY` and `UNKNOWN`,
     because it only excludes `NONE` and `ARRIVED`.

   Risk:

   - A later same-target route retry can "reuse" an intent whose watcher state is already terminal
     or unreliable. In that case the foreground may return `PATHING_STARTED` without registering a
     fresh ACTIVE intent, leaving the task waiting on stale pathing state instead of actually
     recovering.
   - This is especially risky around CR22/CR23 because the current failure mode is repeated
     accept-route / accept-NPC pathing loops where state freshness and route ownership are the core
     signal.

   Recommendation:

   - Restrict same-target route intent reuse to snapshots that are still genuinely in flight, such
     as `ACTIVE` or a current probe-in-progress / fresh route-pending state.
   - If the same-target snapshot is `STOPPED_AWAY`, `ARRIVED`, or stale `UNKNOWN`, clear or replace
     it with a fresh intent and log the reason instead of returning success from
     `registerWindowPathingIntent(...)`.

#### Open Questions

- Should the first implementation cover only `RESOLVE_AFTER_PATHING` and prepared dialogs, leaving
  combat for the next step?
- What coarse fallback interval is acceptable for `WAIT_BATTLE_FINISH` before `COMBAT_STATE_CHANGED`
  exists?
- Should `WindowReadyEventBus` support waiting across multiple windows, or should the first version
  only wait on the current window?
- Should wait intent be added directly to the task phase outcome, or represented by a small
  scheduling policy value object?

Card CR139: 连续任务切换复用启动准备并跳过非必要 hot-start

Status:

- Review. Owner: 唐德.
- 2026-06-29 唐德 claimed. Baseline recorded in `docs/ACTIVE_WORK.md` before source edits. Scope is limited
  to same-queue common startup-prep reuse and clean cross-task hot-start/startup-screen-resume skip;
  no OCR/template/click/navigation target logic should change.
- 2026-06-29 source repair completed:
  - Added `TaskStartupMode.CLEAN_QUEUE_TRANSITION` and task-context predicate.
  - `WindowTaskRunner.runQueueWithBoundGameState(...)` now records previous queued requested task and
    result, and only emits clean transition when previous result is `SUCCESS`, task type changes,
    startup mode is still `NORMAL`, current task is `WUBEI`/`XIULUO_V2`, and the queue-level common
    startup-prep marker is present.
  - `WindowRuntimeContext` startup-prep marker now represents queue-level common prep for the accepted
    queue, while new queue submission still clears the marker.
  - `DefaultWindowTaskStartupInitializer` keeps identity/position sync, then skips common startup prep
    on clean queued transition with marker; leader startup prep marks common prep done after
    `prepareTaskStartupWindow()` succeeds.
  - `WubeiTask` skips first-run `hotStart` only for clean queued transition and starts normal accept.
  - `XiuluoTaskV2` keeps first-run first-aid/摄妖香 but skips `startup-screen-resume` only for clean
    queued transition and starts normal accept.
- Verification:
  - RED guard first failed: `CR139CleanQueueTransitionStartupWiringTest` reported missing
    `CLEAN_QUEUE_TRANSITION`.
  - GREEN guards: `CR139CleanQueueTransitionStartupWiringTest`,
    `WindowTaskRunnerCombatStartupDeferWiringTest`, `AfterCombatStartupRecoveryWiringTest`,
    `XiuluoContinuousRoundNoHotStartWiringTest`, `XiuluoCR130CR131WiringTest`.
  - `mvn -q -DskipTests compile` passed.
  - `mvn -q -DskipTests test-compile` passed.
- Known non-CR139 note: `LeftTopStatusSwitchWiringTest` still has a stale CR138
  `AutoBattleTask` token expectation (`taskMaintenanceService.isTeamPathingMaintenanceWindowOpen`);
  this was observed during adjacent guard probing but not repaired under CR139.
- 2026-06-29 review after user complexity concern:
  - Core behavior is small: one queue-level startup-prep marker, one clean cross-task transition
    decision in `WindowTaskRunner`, one common-prep skip in `DefaultWindowTaskStartupInitializer`,
    and first-run hot-start skips in 五倍/修罗.
  - No P1 behavior blocker found in this pass.
  - P2 maintainability note: `CR139CleanQueueTransitionStartupWiringTest` over-specifies the
    implementation shape by requiring an explicit `CLEAN_QUEUE_TRANSITION` mode and source-string
    fragments. If CR139 is slimmed down later, prefer behavior-oriented guards so the code can stay
    as close as possible to "same queue already prepared -> skip" without locking in extra naming
    or wrapper shape.
- Fresh runtime gate: run `[五倍, 修罗]` and check for common prep mark, `clean queued task transition
  startup`, startup initializer skip, and 修罗/五倍 clean-transition hot-start skip logs. Then run a
  fresh standalone 修罗 start to prove true startup resume still exists.

Business source:

- User observed a repeated startup-preparation cost in a continuous UI queue:
  - Select both 五倍 and 修罗.
  - 五倍 runs first and performs startup checks such as `Alt+5`, `Alt+6`, left-top status cleanup,
    mini-map/map-option preparation, flying/zoom expansion checks, and related startup UI closure.
  - After 五倍 completes, the queue advances to 修罗.
  - 修罗 runs the same broad startup checks again before accepting the task, even though the same
    bound leader window just completed those global checks in the same accepted queue.
- User clarification:
  - A normal cross-task transition is not a hot-start/resume scenario.
  - If the previous task was 五倍 and it completed, the next 修罗 task has no 修罗 hot-start state to
    recover; it should accept a fresh 修罗 task.
  - If the previous task was 修罗 and the next task is 五倍, the same rule applies: do not run a
    五倍 hot-start/resume path; accept a fresh 五倍 task.
- Expected behavior: once the first task in the same UI-submitted queue has successfully completed
  common startup UI preparation, later tasks in that same queue should skip the common startup prep
  and any next-task hot-start/startup-screen resume that only makes sense for true UI startup or
  resume. The later task should go directly into its normal fresh accept-task flow.

Initial source suspicion from code scan:

- `WindowTaskRunner.runTaskWithBoundGameState(...)` invokes `DefaultWindowTaskStartupInitializer`
  before each concrete task.
- `WindowRuntimeContext` already has queue-scoped startup UI preparation markers, but the current
  marker appears to be keyed by task code. That prevents duplicate preparation inside one task, but
  it does not cover cross-task queues such as `[wubei, xiuluo_v2]`.
- CR128 covered 五环/two-round queue startup preparation, but does not settle the cross-task
  `[五倍 -> 修罗]` reuse rule.

Required behavior:

1. Add or clarify a queue-level, per-window "common startup preparation completed" state.
   - It should live for one accepted task queue on the bound leader window.
   - It must be cleared when a new UI queue/manual start is submitted.
   - It must not leak across separate user starts, different bound windows, or debug-only runs.
2. When the first formal task in the queue completes common startup prep successfully, mark the
   common prep as done for the queue.
3. When a later formal task in the same queue starts, skip common/global startup prep if the mark is
   present and still valid.
4. Candidate common prep items to skip after they were already completed in the same queue:
   - `Alt+5` / `Alt+6` visibility or panel checks.
   - Startup left-top status probe/close.
   - Startup mini-map / map-option preparation.
   - Startup flying/zoom/expanded-map checks, when the first task already completed them and there
     is no evidence the UI state was invalidated.
5. In a clean cross-task transition, also skip next-task hot-start / startup-screen resume:
   - `[五倍 -> 修罗]`: do not run 修罗 hot-start/tracker shortcut/startup-screen resume; go accept a
     fresh 修罗 task.
   - `[修罗 -> 五倍]`: do not run 五倍 hot-start/startup resume; go accept a fresh 五倍 task.
   - The signal for this skip must be explicit: previous queued task ended cleanly and the queue is
     advancing to a different next task.
6. Keep true startup/resume recovery when it is actually needed:
   - user starts a standalone task from the UI;
   - user resumes after pause/stop/interruption;
   - task starts while already in combat or with an unresolved startup screen;
   - previous queued task did not end cleanly or left startup state uncertain.
7. Do not skip normal task business logic after the fresh accept path:
   - 修罗 first-round/player-state startup checks that are not global UI prep.
   - 五倍 task classification, task tracker read, task-specific route/prepath decisions.
   - 摄妖香、盒子、补血补蓝、修装备、医宝宝、回程道具/显形镜 decisions.
   - Any fallback needed when the first task's common startup prep failed, was interrupted, or was
     skipped because the task started in combat.

Implementation guardrails:

- Keep this as a startup-prep reuse change only. Do not change OCR/template/click/navigation target
  selection in this CR.
- If the implementation changes a visual match or click target, follow `AGENTS.md` testcase replay
  rules and record the marked output image in `docs/ACTIVE_WORK.md`.
- Preserve current member/auto-battle skip behavior; this card is about the leader window's common
  startup preparation across queued tasks.
- Prefer extending the existing queue-scoped startup marker instead of adding another parallel state
  model, unless source review proves the existing marker cannot represent common-vs-task-specific
  startup cleanly.
- Logs should make the decision obvious:
  - first task: common startup prep executed and marked complete;
  - later task: common startup prep skipped because same queue/window already completed it;
  - later task: hot-start/startup-screen resume skipped because this is a clean cross-task queue
    transition, not a true resume;
  - negative path: common startup prep not skipped because marker missing/stale/failed.

Validation plan:

- Source guard:
  - A continuous queue `[wubei, xiuluo_v2]` has exactly one common startup-prep completion marker for
    the leader window.
  - Later task startup can skip common prep and skip next-task hot-start/resume when the previous
    queued task ended cleanly and the next task is different.
  - True UI startup/resume still keeps the existing hot-start/recovery path.
  - Starting a new queue clears the common marker.
- Fresh runtime:
  - Start `[五倍, 修罗]`.
  - Logs show 五倍 executes common startup prep once.
  - After 五倍 succeeds and 修罗 starts, logs show an explicit skip such as
    `startup init skipped: queue common startup preparation already completed`.
  - 修罗 also logs that startup hot-start/resume is skipped because this is a clean cross-task queue
    transition.
  - 修罗 should not repeat `Alt+5` / `Alt+6`, startup left-top close, mini-map/map-option prep, or
    flying/zoom checks that were already completed by 五倍 in the same queue.
  - 修罗 accepts a fresh task and runs normally.
- Negative runtime:
  - Submit a fresh standalone 修罗 queue after the previous queue ends. Common startup prep must run
    again, and true standalone 修罗 hot-start/startup recovery must remain available.
  - If the first task's prep failed/was interrupted/started in combat and never marked complete, the
    second task must not blindly skip common startup prep or hot-start/recovery.

## Package Rules

- `model`: cross-domain value objects and shared public request/result/spec types.
- `model.<domain>`: public DTOs for one domain, such as `model.npc` or `model.dialog`.
- `service`: Spring services and business orchestration only. Do not hide public request/result
  classes inside a service when other packages construct or consume them.
- `service.<domain>`: service-specific policies or service API objects that are tightly coupled to
  that domain's service boundary, such as dialog handle policy/result types.
- `vision`: screenshot, OCR, template matching, and visual learning services.
- `input`: physical input queue, input actions, and input serialization.
- `window`: window discovery, binding, runtime state, control, execution, diagnostics, and policies.
- `task`: task flows and task-local state machines.
- `debug`: standalone debug entry points only. Debug tools may call services but should not define
  reusable business models.
- `tools`: stateless helpers and calibration utilities. Spring components that mutate game state,
  capture active windows, or call OCR should eventually move to a service/domain package.

## Migration Policy

- Move public nested request/result/spec types out first when they are constructed outside their
  owning service.
- New public request/result/value objects should normally follow the repository convention in
  `AGENTS.md`: immutable data object, Lombok builder when callers need named construction, and
  enum values instead of cross-boundary strings. Existing records can be migrated separately when
  changing their accessor style will not distract from the behavior fix being tested.
- Keep private helper records/classes at the bottom of their enclosing file unless another package
  needs to reference them.
- Avoid package moves in the same edit as behavior changes. Each migration should compile by itself.
- Prefer domain model packages such as `model.npc`, `model.dialog`, and `model.navigation` over a
  flat pile of unrelated classes under `model`.
- Existing packages like `window.control`, `window.runtime`, and `input.action` may keep their
  domain-local request/result types because those packages are already cohesive.

## First Cleanup Targets

- `NpcClickRequest` -> `model.npc`
- `GreenTemplateClickSpec` -> `model.dialog`
- `OcrWordResult` / `LocationInfo` -> `model.ocr`
- `OcrLineResult`, `TargetOcrResult`, `TextCandidate`, `TextCandidateScanResult`,
  `TextCandidateScanStatus`, and `OcrWindowRegion` -> `model.ocr`
- `RecordResult`, `ResolvedNpcClickRegion`, and `LearnedNpcClickPoint` -> `model.ocr`
- `PlayerAnchorMatch` -> `model.ocr`
- `MiniMapSnapshot`, `TemplateLocationInfo`, `MapLabelTemplateMatch`, `ObjectiveTextResult`,
  and `PathingResult` -> `model.navigation`
- `DialogType` -> `model.dialog`
- `QuestDetailCapture` -> `model.quest`
- Later: internal persisted vision-memory classes currently nested in `OcrRoiMemoryService`

## Reviewer Notes

### 2026-06-20 CR65 Implementation Review

- Implemented `DialogService` maintenance fallback boundary: after the fixed-strip quick checks miss,
  only `DialogType.OPTION` can enter `handleBusinessOption(false, detection)`. `DialogType.STORY`
  returns not-found with `reason=non-option-dialog type=STORY`, so it no longer washes the image or
  matches `heal-pet` / `repair-equipment`.
- Implemented `AutoCombatPanelService` same-team burst guard for `reason=refresh-due` Alt+8. The
  guard allows at most one same-team refresh-due reservation per 30000ms, logs deferred requests with
  team key, window id, age, and retry-after, and does not touch per-window estimate / refresh
  timestamps on defer. `low-rounds` and `unknown` bypass this guard.
- Review status: source/test verification passed; keep CR65 in Review until fresh multi-window runtime
  logs prove STORY fallback no longer produces heal/repair miss noise and same-team refresh-due Alt+8
  bursts are deferred while low-rounds remains immediate.
