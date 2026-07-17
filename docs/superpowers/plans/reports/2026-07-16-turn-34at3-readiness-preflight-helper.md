# TURN-34AT3 Readiness Preflight Helper - FAST expected-exit deferred recovery

## 0. Role and verdict boundary

- Role: CR271 Internal readiness helper.
- This file is a read-only contract convergence report. It is **not** a reviewer opinion, approval, assignment,
  source delivery, build result, or permission to edit Java.
- Preflight result: **`CONTRACT CONVERGED / NOT YET CLAIMABLE`**.
- Recommended next smallest tranche: one test-only recovery handoff case in the existing
  `AutoCombatServiceTurnContractTest.java`.
- This helper changed only this report. It did not change Java, a CR/card, `docs/ACTIVE_WORK.md`, dashboard data,
  POM/resources, or any other report.
- No Maven/JUnit/compile/package, runtime/application/server/Task/UI/capture/input, or Git mutation was run.

The candidate is intentionally **not approved**. At the closing snapshot, AT1 parent Review #2 was
`P0/P1/P2=0/1/0 / REPAIR #2 REQUIRED`, AT2 did not yet have a fixed child card, and the shared test file had an
active/recent External C write history. A changed SHA or mtime is evidence of bytes changing, never evidence of
review, acceptance, or ownership release.

## 1. Authority fully read

The preflight read the following authorities to physical EOF before selecting the slice:

1. `D:\mavenProject\DHXY\AGENTS.md`.
2. `docs/DHXY_CONTEXT.md`.
3. The latest top CR271 block in `docs/ACTIVE_WORK.md`; that top block was older than the later AT1 card append and
   therefore is context, not a substitute for the child card's physical true EOF.
4. Sections 14-19 of `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md`.
5. `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`.
6. `docs/业务逻辑.md`, including `Expected 战斗快脱战与回程验证兜底` and the 修罗
   `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` baseline table.
7. The full current true-EOF contents of TURN-34A, TURN-34AT0, TURN-34AT1, the AT1 readiness helper, and the AT2
   readiness helper.
8. Current Cloud `AutoCombatService.java` and the complete current test snapshot, plus the preserved
   `696a12b0` `AutoCombatService.java`.
9. Current Cloud `AutoBattleTask.java`, `FiveRingTaskV2.java`, `WubeiTask.java`, and `XiuluoTaskV2.java`.
10. All six frozen Cloud `TaskMaintenanceService` APIs and their current bodies:
    `awaitTeamFirstAidMaintenanceWindowOpen`, `awaitLocalTeamSupportCapabilityOpen`,
    `isLocalSupportMemberSession`, `isLocalSupportMemberCandidate`,
    `isPendingLocalSupportLeaderDetection`, and `isLocalTeamSupportCapabilityOpen`.

Relevant authority conclusions:

- TURN-34A owns the single named test class with profile
  `HTTPS_TURN_CONTRACT_TEST_FAMILY / BC4+BASE + TASK+STATE`.
- Each new observation/input action is one closed action with a fresh UUID and one command. Terminal/uncertain or
  correlation-invalid results cannot become business success, false, miss, exit, or recovery.
- The strict business authority is `696a12b0`; there is no approved behavior difference.
- `docs/业务逻辑.md` says the expected-combat fast probe is only a shortcut. If trusted correction says the window
  is still in combat, deferred HP/MP/incense recovery must remain pending and the task returns to the combat wait.
- No new TTL, extra verification, retry, fallback, park/yield, cleanup, fail-closed rule, session, ledger, durable
  workflow, or pre-return full-radar wait is authorized.

## 2. Read-only snapshot ledger

### 2.1 Production, baseline, callers, and maintenance

Observed read-only on 2026-07-16 between 10:11 and 10:33 America/New_York:

| File | Lines | SHA-256 | mtime |
|---|---:|---|---|
| Cloud `src/main/java/com/bot/dhxy/service/AutoCombatService.java` | 852 | `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9` | `2026-07-16T06:29:17.7816908-04:00` |
| Preserved `migration-baseline/696a12b0/.../AutoCombatService.java` | 835 | `b4828408ce624b0f7c7b656cf73a76103f059371ad2be02598929e5aa328a24d` | `2026-06-30T01:43:39.0000000-04:00` |
| Cloud `task/AutoBattleTask.java` | 294 | `e13bfff740570b9c7b833f7edce336bffe39fb89e410b630ff2156b69410264a` | `2026-07-14T17:06:38.8512297-04:00` |
| Cloud `task/wuhuan/FiveRingTaskV2.java` | 2775 | `287ff0ebe4f3cecf9820a10d2ffcbf0f7aed2a26beb7a5f510d92f540e8a4bdb` | `2026-07-15T23:28:34.9873090-04:00` |
| Cloud `task/wubei/WubeiTask.java` | 4329 | `dfde0ad08900f2553088a7d304556a2b5a754c4980305199db7b9c9035b720d7` | `2026-07-15T22:54:45.7534169-04:00` |
| Cloud `task/xiuluo/XiuluoTaskV2.java` | 4225 | `46f9665999f644be63b7f27e772429e68190322fbde487641cbeff0f747f519a` | `2026-07-15T23:28:01.1227494-04:00` |
| Cloud `service/TaskMaintenanceService.java` | 1224 | `963b028c4a753efcc0263e402d6aba310e51c2591aca5e9717afe92912a66bbc` | `2026-07-16T08:17:40.6760891-04:00` |

Production is still exactly the TURN-34A frozen production anchor. None of these files is an AT3 write target.

### 2.2 Shared test file drift

The shared test file changed while this helper was reading it, as the prompt warned:

| Observation | Lines / tests | SHA-256 | mtime | Meaning |
|---|---:|---|---|---|
| Initial helper snapshot | 864 / observed WIP | `30e565cbe4a40957c118bacf1f28f9a267b64e872e095d59b7ebaa94ae7fb269` | `2026-07-16T10:11:39.6977141-04:00` | Concurrent AT1 write window; not an anchor |
| AT1 first delivery snapshot | 963 / 21 | `6be1f3bf0f7037aa34ac9bc95c8245b93e59a88a30966d95ffbc1a77fcb45c68` | `2026-07-16T10:16:09.0284871-04:00` | Parent later rejected with `0/2/0`; not an AT3 anchor |
| AT1 Repair #1 delivery snapshot | 1020 / 22 | `35116f19f57f170a4ca6e56fadf11d9047b76520a8f61f24b86fb63e11ec10a4` | `2026-07-16T10:26:47.9931857-04:00` | Parent Review #2 accepted the first repair points but required Repair #2; not an AT3 anchor |

The complete 1020-line snapshot was read. It contains the original 17 tests, the four AT1 tests, and AT1 Repair #1's
cross-invocation UUID test. It does not contain either AT2 method proposed by the AT2 readiness report, and it does
not contain the AT3 method proposed below.

### 2.3 Fixed-report true EOF state

- TURN-34AT0 true EOF: parent Review #2 passed `P0/P1/P2=0/0/0`, test-source passed, owner released.
- TURN-34AT1 true EOF at the 10:33 closing read: parent Review #2 is
  `P0/P1/P2=0/1/0 / REPAIR #2 REQUIRED`; External C retains the sole test-only owner. The remaining P1 requires the
  shared-service UUID sequence to include one trusted positive Stage-1 capture alongside the seven terminal cases,
  proving eight canonical, distinct IDs. SHA `35116f19...` is therefore rejected as an AT3 start anchor.
- TURN-34A's observed integration marker had not advanced to an AT1 pass. The child card's newer true EOF governs
  this readiness gate.
- AT1 readiness helper true EOF is exactly `TRUE_EOF PRECHECK_COMPLETE`.
- AT2 readiness helper true EOF is exactly `TRUE_EOF PRECHECK_COMPLETE`.
- `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34AT2.md` did not exist at the observed snapshot.

Therefore AT3 is a converged **next-card recipe**, not a card that External C may claim now.

## 3. Why recovery is the next smallest valuable slice

The recommended AT3 is the positive AT2 exit-to-recovery handoff, specifically FAST expected-exit deferred leader
recovery. It is the smallest non-overlapping slice for four reasons:

1. AT1 owns Stage-1 entry, exact one-command/UUID/raw-PNG shape, and first-capture terminal/uncertain behavior.
2. AT2 owns normal completed blank Stage1 -> Stage2 -> Stage3, two completed misses, readable-minimap exit, and the
   unreadable-minimap no-exit fork. AT3 should consume that accepted result rather than replay seven captures.
3. The highest-value unguarded business invariant is what happens after the positive AT2 exit: FAST must return
   `EXIT_RECOVERED`, defer leader recovery, preserve it through initialize, and refuse to consume it after a trusted
   correction says `IN_COMBAT`.
4. Remaining timing work already overlaps AT0's dynamic polling/wake/30-second gate and would require clock seams;
   maintenance would open six APIs and collaborator ordering; caller coverage would require full Task phase
   harnesses. Each is larger and should remain a later mutually exclusive tranche.

This is recovery-only. It does not claim to finish the three recovery policies, maintenance order, timing matrix,
or four caller matrix required by the TURN-34A parent.

## 4. Proposed frozen AT3 contract

### 4.1 Exact test count and name

Add exactly **one** `@Test` to the existing class:

```java
fastExpectedExitDefersLeaderRecoveryAcrossInitializeAndTrustedInCombat
```

No second AT3 test is needed. The single method closes one coherent state transition:

```text
IN_COMBAT + FAST expected wait
  -> accepted AT2 positive exit handoff
  -> EXIT_RECOVERED + FREE + deferred leader recovery pending
  -> initialize keeps deferred leader recovery
  -> trusted correction says IN_COMBAT
  -> safe-point consume returns false and keeps recovery pending
```

### 4.2 Test-only handoff seam

The test may add one private, bottom-of-file `PreparedCompletedExitRadar extends BattleRadarService` and the minimum
test-harness injection needed to pass it to the real production `AutoCombatService` constructor.

The seam is narrowly frozen as follows:

1. It is a scripted boundary for **the already accepted positive AT2 output**, not a copied radar reducer.
2. Start the production `GameContext` at `IN_COMBAT`.
3. `armExpectedCombatExitWait("xiuluo-v2")` records exactly one arm.
4. `checkFastExpectedCombatExitByAvatarDiff("xiuluo-v2")` records one call and returns false.
5. `shouldRunFullRadarForFastExpectedExitFallback()` records one call and returns true.
6. `checkAndSyncCombatState()` records one handoff, sets production `GameContext` to `FREE`, and makes exactly one
   fresh prepared-completed exit signal available. It performs no capture and contains no template/minimap logic.
7. `consumeCombatEnterSignal()` returns false.
8. `discardStaleCombatExitSignalIfInCombat("xiuluo-v2")` returns false because the handoff has already set `FREE`.
9. `consumeCombatExitSignalForExpectedWait("xiuluo-v2")` consumes the prepared signal exactly once.
10. `consumeCombatExitSignal()` must record zero calls and return false if unexpectedly reached.

The seam must not accept or construct `CloudTurnCommandResult`, `TurnOutcome`, a terminal status, raw PNG, ROI,
minimap fact, timer, or UUID. It must not copy `REQUIRED_COMBAT_EXIT_MISSES`, freshness timestamps, terminal
classification, correlation checks, or any production radar decision. AT2 owns all of those.

No production hook, private reflection, source scan, Mockito, wall-clock polling, sleep, or second test file is
allowed. If the class-level test JavaDoc's “real collaborators” wording becomes inaccurate for this one narrow
handoff seam, adjust only that wording in the same test file; do not disguise the seam as a real radar execution.

### 4.3 Exact public call path

Under the existing exact `TaskExecutionContextHolder.callWith(context, ...)` binding and existing leader/requested
Xiuluo metadata:

1. `AutoCombatService.initializeForCurrentWindow()` establishes a clean current-window state.
2. Set `GameContext.ActionState.IN_COMBAT` to represent the expected Xiuluo combat wait.
3. Call production
   `AutoCombatService.handleCombatTick(context, "xiuluo-v2", PostCombatRecoveryPolicy.FAST_EXPECTED_EXIT)`.
4. The production method arms expected-exit once, follows the scripted AT2 handoff, then calls its real
   `consumeExitAndRecover(...)` path.
5. The real path calls `AutoCombatPanelService.recordCombatExit()`,
   `PlayerStateService.resetCheckCounter()`, and `CommonBoxService.detectMemberBoxAfterCombatExit(...)`.
   Existing `LEADER` metadata prevents member CommonBox capture/input.
6. The real path clears follower pending state, sets deferred leader recovery and its source, disarms the fast
   watch, sets `FREE`, and returns `TickResult.EXIT_RECOVERED`.
7. Call production `initializeForCurrentWindow()` again and verify deferred leader recovery remains pending.
8. Set `GameContext.ActionState.IN_COMBAT` to represent the already completed trusted return-failure correction.
9. Call production
   `consumePendingLeaderPostCombatRecoveryIfAllowed(context, "xiuluo-v2:return-failed:trusted-in-combat")`.
10. Verify it returns false and leaves the same deferred recovery pending. It must not run first-aid, incense,
    CommonBox, maintenance, or another radar observation.

This mirrors the real Xiuluo caller chain without instantiating the Task:

```text
XiuluoTaskV2.waitCombat
  -> handleCombatTick(..., "xiuluo-v2", FAST_EXPECTED_EXIT)
  -> return verification fails
  -> trusted radar result is IN_COMBAT
  -> resume WAIT_COMBAT
  -> do not consume deferred leader recovery
```

### 4.4 Exact state assertions

The new test must directly assert all of the following:

1. The first production tick returns `TickResult.EXIT_RECOVERED`.
2. `GameContext` is `FREE` immediately after the accepted exit.
3. `hasPendingLeaderPostCombatRecoveryForCurrentWindow()` is true.
4. `hasPendingFollowerFirstAidForCurrentWindow()` is false.
5. A subsequent `initializeForCurrentWindow()` leaves leader recovery true and follower recovery false.
6. With trusted state reset to `IN_COMBAT`,
   `consumePendingLeaderPostCombatRecoveryIfAllowed(...)` returns false.
7. Leader recovery remains true after that false return; follower recovery remains false.
8. Seam counts are exact: one arm, one fast probe, one full-fallback decision, one prepared exit emission, one
   expected-exit consume, zero normal-exit consumes; the one-shot prepared signal is no longer available.

Do not reflect the private pending source or internal timestamps. Public behavior and seam counts are sufficient.

### 4.5 Exact command, UUID, and terminal assertions

AT3 introduces **no new observation or input action**. Its exact closed-action contract is therefore zero, not one:

1. `ScriptedCommandPort.executeCalls == 0`.
2. `actions`, `timeouts`, `results`, and initially empty `replies` all remain empty.
3. The emitted action-ID collection is empty; expected UUID count is exactly zero.
4. No `CloudTurnCommandResult.Status` or `TurnOutcome.Status` exists in this test path.
5. There is no retry, replay, resend, fallback command, compensation, second action, or local-service command.

The test should reuse the existing port/UUID helpers where practical, for example asserting the existing canonical
ID collector has expected size zero, rather than inventing a second UUID reducer.

This zero-command boundary is deliberate: AT1 already proves terminal/uncertain first-capture cases, and AT2 must
prove the positive and unreadable-minimap radar forks with exact commands. AT3 consumes only AT2's trusted positive
business signal. A BUSY, duplicate, timeout, interrupted, FAILED, STOPPED, uncertain, missing-frame, unreadable, or
correlation-invalid result must never be fed into the prepared-completed seam.

## 5. `696a12b0` equivalence statement

The selected assertions lock behavior that is line-for-line semantically present in the preserved baseline:

- Baseline `initializeForCurrentWindow()` is at line 81; current Cloud is at line 82. Both clear refresh/clean/
  entry/follower/fast/expected/verify state and deliberately do **not** clear deferred leader recovery.
- Baseline `consumeExitAndRecover(...)` is at line 344; current Cloud is at line 345. Under
  `FAST_EXPECTED_EXIT`, both consume the expected-wait exit, record combat exit, reset the player check counter,
  run the existing CommonBox hook, clear follower pending, set deferred leader pending/source, disarm fast watch,
  set `FREE`, and return the shared recovered result.
- Baseline `consumePendingLeaderPostCombatRecoveryIfAllowed(...)` is at line 441; current Cloud is at line 442.
  Both return false before clearing pending when the trusted state is `IN_COMBAT`.
- `docs/业务逻辑.md` requires exactly that false-positive correction: return to `WAIT_COMBAT`/
  `WAIT_BATTLE_FINISH`, keep deferred HP/MP/incense work pending, and wait for the real exit.

The test exercises the current production methods directly. The only migration differences on this path are the
already frozen exact-context owner and Cloud ports; they do not alter the decision or ordering above.

Required card wording:

`无已批准业务差异；按 696a12b0 基线等价迁移。`

## 6. External C self-unlock predicate

External C may self-unlock AT3 on a later heartbeat only when **all** of these machine-readable facts are true:

1. TURN-34AT1 physical true EOF contains a parent `TEST-SOURCE REVIEW PASSED` with
   `P0/P1/P2=0/0/0`, accepted test SHA, and owner release. External C's Repair #1 delivery alone is insufficient.
2. A fixed TURN-34AT2 child card exists, freezes only the two AT2 tests from the AT2 readiness report, and reaches
   parent `TEST-SOURCE REVIEW PASSED` with `P0/P1/P2=0/0/0`, final test SHA, and owner release.
3. Disk `AutoCombatServiceTurnContractTest.java` SHA exactly equals that AT2 parent-accepted final SHA. Any later
   mtime/SHA drift closes the gate until the drift is reviewed and anchored.
4. Cloud production `AutoCombatService.java` still equals
   `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`.
5. No active writer owns the shared test file.
6. A fixed TURN-34AT3 child card has been materialized at physical true EOF with the exact one-test contract in
   this report, and its initial test SHA equals the AT2 accepted final SHA.

In compact form:

```text
AT3_UNLOCK = AT1_PARENT_PASS_0_0_0
          && AT2_PARENT_PASS_0_0_0
          && TEST_SHA == AT2_ACCEPTED_SHA
          && PROD_SHA == 532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9
          && NO_ACTIVE_TEST_WRITER
          && AT3_FIXED_CARD_TRUE_EOF
```

Once and only once that predicate is true, External C can append its canonical AT3 claim and produce a real test
increment within the first five-minute window without asking for a new business choice. This preflight does not
itself satisfy or approve any term of the predicate.

## 7. Future AT3 write set and preservation gate

When the fixed card eventually opens, the exact implementation write set should be:

1. `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\service\AutoCombatServiceTurnContractTest.java`.
2. The future append-only `TURN-34AT3` child card.

The worker must preserve every parent-accepted AT0/AT1/AT2 test and assertion. Production, four Task callers,
`TaskMaintenanceService`, protocol/client/action factory/command port, POM/resources, all other tests, TURN-34A,
AT0/AT1/AT2 fixed reports, this preflight, `ACTIVE_WORK`, and dashboard data are read-only for AT3 implementation.

## 8. Explicit exclusions

AT3 must not include any of the following:

1. AT1 Stage-1 flag matching, terminal/uncertain matrices, raw-PNG correlation, or positive-command UUID coverage.
2. AT2 Stage1 -> Stage2 -> Stage3 capture sequences, two-miss counter, coordinate ROI, readable/unreadable minimap,
   or either AT2 exit fork.
3. Successful deferred-recovery consumption, clear-before-work, first-aid probe/input, or incense command. Those are
   a later recovery tranche with non-zero action and terminal contracts.
4. `FULL_RECOVERY` or `FULL_RECOVERY_WITH_LEADER_INCENSE`.
5. Member CommonBox, follower first-aid, CommonBox-before-first-aid ordering, or one re-probe.
6. FAST `15s/1s/4s`, enter `+4s`, maintenance `4s/40s/30s/10s`, panel `(489,726)/>20px`, or any wall-clock wait.
7. Direct coverage or modification of any of the six `TaskMaintenanceService` APIs. They remain frozen and belong
   to a later maintenance slice; this leader/no-pending path must not need them.
8. Instantiating or modifying `AutoBattleTask`, `FiveRingTaskV2`, `WubeiTask`, or `XiuluoTaskV2`; no caller phase
   assertion is claimed by this service-only handoff test.
9. New behavior truth from a runner/ready-event negative signal, new verification, TTL, retry, cleanup, park/yield,
   session, ledger, durable workflow, or changed phase/fallback order.
10. A second production file, second test file, production test hook, private reflection, copied reducer, source
    guard/scan, Mockito, sleep, runtime, capture, or physical input.

## 9. Deferred verification commands

These commands are written only as the future stable-writer verification target. This helper did not run them:

```powershell
Set-Location D:\mavenProject\dhxy-cloud-brain
mvn -q '-Dtest=AutoCombatServiceTurnContractTest#fastExpectedExitDefersLeaderRecoveryAcrossInitializeAndTrustedInCombat' test
mvn -q -Dtest=AutoCombatServiceTurnContractTest test
mvn -q -DskipTests compile
```

The method-only command proves the new AT3 slice; the full named class preserves AT0/AT1/AT2; compile is the Java
gate. They may run only under the applicable parent stable-writer/build authorization and never from this helper.

## 10. Preflight conclusion

The smallest non-duplicative AT3 is one zero-turn recovery handoff test:
`fastExpectedExitDefersLeaderRecoveryAcrossInitializeAndTrustedInCombat`. It directly locks the
`696a12b0` FAST-defer invariant and the user-approved trusted-`IN_COMBAT` false-positive correction while leaving
AT1 terminal work, AT2 radar work, successful recovery actions, timing, maintenance, and caller matrices to their
own later slices. Current source drift or External C delivery does not approve or unlock it; only the predicate in
section 6 can do so.

TRUE_EOF PRECHECK_COMPLETE
