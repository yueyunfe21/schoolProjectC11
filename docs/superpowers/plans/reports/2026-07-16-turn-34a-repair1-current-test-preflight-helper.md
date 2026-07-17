# CR271 TURN-34A Repair #1 current-test preflight helper

## 1. Role, scope, and non-authority

- Role: TURN-34A Repair #1 current-test preflight helper only. I am not the implementation owner, reviewer,
  manager, approver, or delivery recipient.
- Purpose: give the parent a byte-current, mechanically checkable list of what the sole named test currently
  proves, what it does not prove, and which current assertions/fixtures can create false evidence.
- Read basis: `AGENTS.md`, `docs/DHXY_CONTEXT.md`, `docs/业务逻辑.md`, the authoritative HTTPS-turn plan,
  TURN-34A through its physical true EOF, Parent Review #1/Repair #1 conditions, parent-passed production,
  the current complete named-test bytes, `TaskExecutionContext` exact-metadata validation, `BattleRadarService`,
  `AutoCombatPanelService`, and all four real production Task callers.
- Only this report was written. No Java/card/other-document edit, no Maven/JUnit/compile/package/runtime/
  application/server/Task/UI/capture/input, and no Git mutation was performed.
- This is a point-in-time precheck, not a review result. It does not write `APPROVED`, `BLOCKED`, `CLOSED`, or
  `SOURCE PASSED` for TURN-34A.

## 2. Byte-current snapshot

Snapshot time: `2026-07-16T09:08:11-04:00`.

| Artifact | Byte-current evidence |
|---|---|
| TURN-34A fixed card | 35,683 bytes, 396 lines, SHA-256 `a5b02079a6fccdfda170d9cd99e4cf3349520a943ac1a4b1034689f60edf039f`, mtime `2026-07-16T08:47:09.9913278-04:00` |
| Card physical true EOF | `TURN-34A PARENT REPAIR-START-OBSERVED EXTERNAL-C-ACTIVE DEADLINE-CANCELLED 2026-07-16T08:46:17.085-04:00`; no `REPAIR #1 SOURCE+TEST DELIVERED` exists yet |
| Parent-passed production | `AutoCombatService.java`: 46,414 bytes, 852 lines, SHA-256 `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`, mtime `2026-07-16T06:29:17.7816908-04:00`; exactly matches Parent Review #1 production-passed SHA |
| Current sole named test | `AutoCombatServiceTurnContractTest.java`: 37,108 bytes, 763 lines, SHA-256 `60e49ed9c641801af81d02df968c66acdb7be4b18bd6f225bfe70ddd14a8bbc6`, mtime `2026-07-16T08:45:50.7610704-04:00` |
| Named-test byte form | UTF-8 without BOM, LF-only (`763` LF, `0` CRLF), final bytes `0x7D 0x0A` (`}\n`) |
| Declared executable tests | Exactly 17 `@Test` methods; the final one remains `frozenPublicSurfaceIsPresent` at test `:471-495` |

The production SHA is read-only under Repair #1. Any parent receipt with a different production SHA is outside
the frozen repair and must not be explained away as a test-only repair.

### Four real callers read at this snapshot

| Caller | SHA-256 | Frozen direct consumption points |
|---|---|---|
| `AutoBattleTask.java` | `e13bfff740570b9c7b833f7edce336bffe39fb89e410b630ff2156b69410264a` | `:137,141-149,162-163,280-287` |
| `FiveRingTaskV2.java` | `287ff0ebe4f3cecf9820a10d2ffcbf0f7aed2a26beb7a5f510d92f540e8a4bdb` | `:1847-1869`; sole AutoCombat call at `:1853` |
| `WubeiTask.java` | `dfde0ad08900f2553088a7d304556a2b5a754c4980305199db7b9c9035b720d7` | `:917-925,2777-2778,3595-3624,3756-3797,4161-4170` |
| `XiuluoTaskV2.java` | `46f9665999f644be63b7f27e772429e68190322fbde487641cbeff0f747f519a` | `:1827-1836,2060-2164,2247-2255,2433-2471` |

Business rules checked include `docs/业务逻辑.md:125-168` (CommonBox detection/priority), `:213-279`
(expected-combat fast-exit and trusted-return correction), Wubei combat wait/recovery at `:806-820`, and Xiuluo
tracker-confirm/incidental combat distinction at `:1207-1212`. No approved business difference exists.

## 3. What Repair #1 has actually added so far

The current bytes contain useful new scaffolding:

1. The harness wires a real production `BattleRadarService`; `PackagedTemplateAssets` supplies the real
   battle-flag test-frame source and the panel/player template collaborators at test `:499-505,652-693`.
2. `battleFlagTemplate`, `blankRoiPng`, `battleFlagRoiPng`, PNG encode/decode/SHA helpers, and
   `completedCapture` exist at test `:499-611`.
3. `ScriptedCommandPort` can queue replies and records every submitted `TurnAction` at test `:717-753`.

This scaffolding has added **zero new executable acceptance cases** after Parent Review #1:

- `battleFlagRoiPng`, `blankRoiPng`, and `completedCapture` each occur only at their own declaration.
- `enqueueCaptures` occurs only at its own declaration.
- No test calls `port.enqueue(...)`.
- The test contains zero references to `AutoBattleTask`, `FiveRingTaskV2`, `WubeiTask`, or `XiuluoTaskV2`.
- The test contains zero explicit `FAILED`, `STOPPED`, `DUPLICATE_OR_UNCERTAIN`, timeout-uncertain, or
  interrupted-uncertain scripted outcomes.
- It contains no UUID uniqueness assertion, no queued-reply exhaustion assertion, no `DRAG_LEFT`, and no
  executable assertion for `489`, `726`, `15_000`, `40_000`, or `10_000`.

Therefore helper existence must not be counted as positive capture, action, transition, timing, terminal, or
caller coverage.

## 4. Current coverage that is real or partially real

The following are the only presently defensible claims, subject to the named test later compiling and running:

| Current test | What it presently proves | Limit that must stay explicit |
|---|---|---|
| `missingHolderBindingFailsClosedWithZeroCollaboratorAction` (`:112-125`) | Missing holder fails before the recorded command port for initialize and two pending getters | Does not exercise a command-capable tick, wrong-scope context, or every collaborator |
| `sameLogicalKeyWithNewNativeFingerprintReplacesStateAndAbaDoesNotRevive` (`:177-211`) | A changed combined fingerprint replaces the deadline-bearing state and A-B-A does not revive A1 | Title/handle/PID all change together; omission of any one fingerprint component could still pass |
| `sameScopeResumeKeepsStateContinuity` (`:214-233`) | Same scope/fingerprint preserves the initialized maintenance deadline | Does not prove continuity of pending follower/leader work |
| `dynamicPollingKeepsBaselineIntervals` (`:238-260`) | `IN_COMBAT=4000`, `NAVIGATING/INTERACTING=2000`, FREE/default=`10000` | Direct service cadence only; not AutoBattle caller selection |
| `confirmedStopPropagatesFromEveryTickEntryWithZeroCommand` (`:267-290`) | Pre-command confirmed stop reaches both overloads, guard, and read-only entry with zero port execute | All branches stop before policy/action behavior; it proves neither overload mapping nor post-command terminal handling |
| `confirmedStopLeavesNoRuntimeStateBehind` (`:296-313`) | A pre-command stop does not create a deadline-bearing service state | Does not cover STOPPED outcome or interrupted uncertainty after command publication |
| `publicEnumsKeepBaselineValues` (`:390-397`) | Exact enum names/order | No enum behavior |
| `refreshDuePanelVerifyGateKeepsThirtySecondTeamSharing` (`:400-420`) | Different window on the same team is deferred before 30s; other team and exact 30s boundary are allowed | Does not assert exact `retryAfterMs`/age and does not drive maintenance integration |
| `postBaselineDhxyApisAndLegacyAuthorityAreAbsentFromCloudSurface` (`:437-468`) | Six post-baseline public method names are absent; three legacy types are not direct declared fields; holder is a direct field | Does not prove method-body references or the full frozen active-source zero-reference set |
| `frozenPublicSurfaceIsPresent` (`:471-495`) | Public methods with the listed parameter lists and gate method exist | Does not assert return types, record components, overload semantics, or behavior |

No current test validly proves a positive combat transition, a combat exit, `EXIT_RECOVERED`, a business action,
or a real Task caller branch.

## 5. Deterministic current-test defects to check before any receipt

These are source-level contradictions in the current bytes. They do not require Maven to identify. Parent receipt
should verify that each is gone before treating a later delivery as test-source complete.

### CT-1: logical-window isolation case cannot construct its second exact context

- Test `:132-134` uses invocation window `window-34a-second` but `window("0xB", 11L)` hardcodes metadata window
  `window-34a` through helper `:625-635`.
- Production `TaskExecutionContext.requireInitialWindowMetadata` rejects invocation/metadata window mismatch at
  `TaskExecutionContext.java:469-471`.
- The test therefore cannot reach the claimed isolation assertions. Even after fixing metadata, its two asserted
  pending values are both fresh/default `false`, so the case still needs a divergent state on A and an unchanged
  state on B to rule out a global/shared map.

### CT-2: device-isolation case cannot construct `otherDevice`

- Test `:158-159` uses invocation device `device-other` with metadata hardcoded to `device-34a`.
- Production rejects that mismatch at `TaskExecutionContext.java:465-467` before the loop at test `:161`.
- Tenant/user/device/window isolation should mutate one scope into a non-default observable state and prove each
  other scope remains fresh; initializing every scope and asserting the same `false` default is weak evidence.

### CT-3: both invalid-ROI uncertainty cases fail before reaching BattleRadar

- Test `:323-325` and `:340-342` construct a `0 x 0` `TurnWindowRect`.
- Production `TaskExecutionContext` requires positive initial rectangle dimensions at `:476-480`.
- Neither test reaches `probeWindowCombatStateReadOnly`, so neither proves conservative uncertainty or zero
  command. Use a valid exact window whose requested radar ROI is outside the window, or a typed uncertain command
  result, depending on the specific case being proved.

### CT-4: service wake-delay assertion tests the caller clamp at the wrong layer

- Test `:376-384` creates fresh state and directly asserts `nextCombatWakeDelayMs()` is in `500..10000`.
- Parent-passed production returns raw maintenance delay `0` for fresh state: `lastCombatUiCleanAt==0` makes
  `nextCombatMaintenanceDelayMs()` due now (`AutoCombatService.java:260-263,285-288`), and
  `nextCombatWakeDelayMs()` returns that raw `0` (`:301-313`).
- The `500..10000` clamp belongs to real callers Wubei `:917-925` and Xiuluo `:2247-2255`. The current assertion
  is therefore statically inconsistent with the frozen/passed production and does not prove either caller.

### CT-5: same-window refresh-gate assertion contradicts passed production and `696a12b0`

- Test `:424-431` expects a second reservation for the same team and same window at `+10ms` to be allowed.
- Parent-passed production `RefreshDuePanelVerifyGate` keys by team (window is only the fallback key) and defers
  every second reservation on that key with age `<30000ms` (`AutoCombatService.java:815-827`). The
  `migration-baseline/696a12b0` implementation has the same rule.
- No current true-EOF acceptance authorizes a behavior change here. Repair #1 must not change production to make
  this test pass; the test expectation must match the frozen 30-second team-key gate.

### CT-6: initialize asymmetry test never creates the state it claims to preserve

- `initializeClearsPendingWorkButKeepsDeferredLeaderRecovery` (`:354-372`) initializes a fresh state and then
  asserts leader pending is `false`.
- It never creates FAST `pendingLeaderPostCombatRecovery=true`; therefore it cannot prove that initialize clears
  refresh/clean/entry/follower/fast/expected/verify state while deliberately preserving deferred leader recovery.
- Exact case: create a real FAST exit first, verify leader pending `true`, seed another resettable pending/deadline,
  call initialize, then verify leader remains `true` while the resettable state is cleared/reset.

## 6. Exact missing service cases from frozen acceptance

Every item below is still missing from the current 17 tests.

### A. Exact context and state

- `S-01`: valid, divergent tenant isolation.
- `S-02`: valid, divergent user isolation.
- `S-03`: valid, divergent device isolation with matching `TurnWindowMetadata`.
- `S-04`: valid, divergent window isolation with matching `TurnWindowMetadata`.
- `S-05`: fingerprint replacement when **only title** changes.
- `S-06`: fingerprint replacement when **only native handle** changes.
- `S-07`: fingerprint replacement when **only process ID** changes.
- `S-08`: A-B-A non-revival with real pending leader/follower work, not only a deadline.
- `S-09`: same-scope pause/resume continuity with real pending work.
- `S-10`: holder missing on an action-capable public tick gives zero UUID/command.
- `S-11`: method argument context A while holder/current exact context is B is rejected before collaborator action;
  no state/action may be charged to either scope.
- `S-12`: initialize asymmetric reset described in CT-6.

### B. Real radar transition and public tick semantics

- `R-01`: from FREE, a correlated raw PNG containing the committed `flag_battle.png` is consumed by real
  `BattleRadarService` through public `AutoCombatService.handleCombatTick`; result is `IN_COMBAT`, action state is
  `IN_COMBAT`, enter signal is consumed once, panel visibility path runs, and entry maintenance is scheduled near
  `now+4000ms`.
- `R-02`: full-radar stage priority is observable: positive auto flag short-circuits selection/top/minimap; a
  later-stage positive requires all earlier negative captures and no later capture.
- `R-03`: while IN_COMBAT, first complete signal miss stays `IN_COMBAT`; second complete miss plus a **readable
  minimap-coordinate frame** emits exit. Current helpers have no readable minimap fixture, so blank frames alone
  cannot reach this branch.
- `R-04`: owner `handleCombatTick` consumes the real exit exactly once and returns `EXIT_RECOVERED`; the next free
  tick without pending action returns `NONE`.
- `R-05`: boolean `false` maps to `FULL_RECOVERY`; boolean `true` maps to
  `FULL_RECOVERY_WITH_LEADER_INCENSE`; nullable policy `null` maps to `FULL_RECOVERY`. Current stop tests return
  before this mapping and are not evidence.
- `R-06`: `handleWindowCombatGuardTick` can update/consume enter for bootstrap but never consumes an exit signal;
  the owner tick can still consume that exit afterward.
- `R-07`: `probeWindowCombatStateReadOnly` sends only the required capture observation, does not consume enter/exit,
  does not run panel/recovery maintenance, and leaves owner consumption intact.
- `R-08`: unavailable/uncertain positive and negative observations preserve remembered `IN_COMBAT` and do not
  invent entry/exit; use valid exact contexts and typed command outcomes.

### C. Recovery ordering

- `P-01 FULL_RECOVERY`: exit records panel `-3` rounds/reset, resets first-aid counter, detects CommonBox, performs
  baseline first-aid behavior, does **not** run leader incense, sets FREE, and returns `EXIT_RECOVERED`.
- `P-02 FULL_RECOVERY_WITH_LEADER_INCENSE`: same ordered chain plus leader incense only after the stop checkpoint.
- `P-03 FAST_EXPECTED_EXIT`: exit detects CommonBox but performs no immediate first-aid/incense; clears follower
  pending, sets deferred leader pending, disarms FAST watch, sets FREE, and returns `EXIT_RECOVERED`.
- `P-04`: deferred leader consume while still IN_COMBAT returns false and keeps pending.
- `P-05`: at a FREE safe point, deferred leader pending is cleared **before** first-aid/checkpoint/incense; a stop or
  terminal during recovery cannot resurrect it, and a second consume cannot duplicate actions.
- `P-06`: follower initial `HEALTHY` creates no pending first-aid.
- `P-07`: follower initial `SUPPLY_NEEDED` queues pending; after gate, cached plan runs and clears pending.
- `P-08`: follower initial `UNKNOWN` queues pending; after gate, exactly one `probeFirstAidSupplyNoFocus` re-probe is
  allowed. `SUPPLY_NEEDED` permits exactly one further cached-plan attempt; `UNKNOWN` keeps pending; no loop/retry.
- `P-09`: CommonBox is checked/consumed before pending follower first-aid. If box succeeds, current tick returns
  `EXIT_RECOVERED` while first-aid remains pending for the next tick. Terminal/uncertain box must not fall through
  as box miss and run first-aid.
- `P-10`: local-support/team gates preserve pending and emit no first-aid action while closed; opening the frozen
  gate allows exactly the baseline pending action, with no new retry/TTL.

### D. FAST and maintenance timing

- `T-01`: FAST expected wait arms after stale-exit disposal; a pre-arm exit cannot be consumed as the new expected
  battle's exit.
- `T-02`: real avatar baseline is one `20x20` correlated capture; no changed-avatar exit before 15 seconds.
- `T-03`: after the 15-second gate, fast avatar probes run no more often than 1 second; diff ratio `>0.35` exits,
  unchanged/uncertain capture does not.
- `T-04`: full-radar fallback is allowed immediately once, then no more often than 4 seconds while FAST probing.
- `T-05`: trusted read-only `IN_COMBAT` refreshes the avatar baseline exactly once; `NONE`/uncertain does not.
- `T-06`: entry maintenance is due at `+4000ms`, not immediately; when due it performs generic clean then panel
  verification in frozen order.
- `T-07`: periodic generic clean is due at 40 seconds and not before.
- `T-08`: refresh reason priority remains `UNKNOWN -> LOW_ROUNDS(<=10) -> REFRESH_DUE`.
- `T-09`: refresh-due team-key guard is exactly 30 seconds, urgent per-window retry guard is exactly 30 seconds,
  and deferred-log throttle is exactly 10 seconds. The existing standalone gate test covers only part of the first.
- `T-10`: entry-maintenance/refresh-due merge does not issue duplicate panel verification.

### E. Panel geometry and action sequence

- `G-01`: use a real packaged panel template in a correlated full-window PNG; merely constructing
  `PackagedTemplateAssets` is not coverage.
- `G-02`: with snapshot rectangle `(left=100, top=50)`, target is screen-absolute `(589,776)` from offsets
  `(489,726)`.
- `G-03`: detected panel distance `<=20px` emits zero drag; distance `>20px` emits exactly one
  `DRAG_LEFT(start,target) -> WAIT(500)` command and then the baseline re-observation.
- `G-04`: panel open/refresh uses exact Alt+8 plus its frozen wait in one action; FAILED is a known failure,
  STOP/uncertain/correlation error is not converted to `false`/panel miss.

## 7. Exact four-caller receipt matrix

Direct `AutoCombatService` calls, copied switches, or mocked TickResult values do not satisfy these items. The real
caller must execute its production branch through an allowed public task/phase entry.

### AutoBattleTask

- `C-A1`: startup order reaches `initializeForCurrentWindow()` after startup first-aid and task-maintenance init.
- `C-A2`: every loop tick calls boolean overload with `false`.
- `C-A3`: `IN_COMBAT` and `EXIT_RECOVERED` both sleep/continue and cannot run idle maintenance that iteration.
- `C-A4`: `NONE + FREE` may run idle maintenance; `NONE + non-FREE` does not upgrade to a phase truth.
- `C-A5`: pending follower first-aid selects `500ms`; FREE without pending selects `3000ms`; otherwise the real
  dynamic interval is used.

### FiveRingTaskV2

- `C-F1`: watcher-active state marks combat observed and does not call task-owned recovery yet.
- `C-F2`: only after combat was observed and watcher becomes inactive does the caller invoke boolean `true`.
- `C-F3`: `IN_COMBAT` remains in shared pathing wait with observed flag retained.
- `C-F4`: `NONE` warns but, like `EXIT_RECOVERED`, advances to `SYNC_TASK_PANEL`; neither is upgraded to a different
  business fact.
- `C-F5`: no FiveRing `initializeForCurrentWindow()` call is introduced.

### WubeiTask

- `C-W1`: ENTER_BATTLE full+incense: `IN_COMBAT -> WAIT_BATTLE_FINISH`, `EXIT_RECOVERED ->
  POST_BATTLE_RECOVER`, `NONE` continues existing enter-battle resolution.
- `C-W2`: WAIT_BATTLE_FINISH FAST: `EXIT_RECOVERED -> POST_BATTLE_RECOVER`; `IN_COMBAT` performs existing prescan
  and parks; `NONE` retries ENTER_BATTLE only under the existing never-saw-combat/time gate.
- `C-W3`: caller wake clamp cases are raw `-1 -> 10000`, `<500 -> 500`, in-range unchanged, `>10000 -> 10000`.
- `C-W4`: return verification failure refreshes FAST baseline and resumes WAIT_BATTLE_FINISH only for trusted
  read-only `IN_COMBAT`; negative/uncertain signal does not.
- `C-W5`: tracker-green safe point calls deferred leader consume directly after CommonBox; its boolean is not used
  as a phase truth.

### XiuluoTaskV2

- `C-X1`: shortcut incidental check uses full+incense; only `IN_COMBAT` creates incidental WAIT_COMBAT.
- `C-X2`: `enteredBattleByXiuluo + TRACKER_CONFIRM` selects FAST; incidental/unknown selects full+incense.
- `C-X3`: `EXIT_RECOVERED` preserves three branches: incidental cleanup/resume shortcut, unknown resolver, and
  confirmed expected battle -> RETURN_HOME. `IN_COMBAT` preserves prescan/park/source update; `NONE` stays in the
  existing confirm/wait logic.
- `C-X4`: return verification failure refreshes baseline/resumes WAIT_COMBAT only for trusted `IN_COMBAT`.
- `C-X5`: next-task progress checks pending then consumes deferred recovery; the consume boolean is not phase truth.
- `C-X6`: caller wake clamp covers the same four boundary cases as Wubei.

## 8. Terminal, UUID, command, and source-gate cases still missing

- `E-01`: every action-capable branch records exact command order and action shape, not just a final call count.
- `E-02`: every published command has one nonblank fresh UUID; all action IDs in a multi-command scenario are
  unique; one metadata read is not counted as a command.
- `E-03`: every queued expected reply is consumed. Current port fails on extra commands but has no assertion that
  production emitted too few commands; an unconsumed reply can otherwise hide a missing action.
- `E-04`: `BUSY`, duplicate action ID, timeout-uncertain, and interrupted-uncertain emit no retry, compensation, or
  fabricated observation. Confirmed stop propagates where required.
- `E-05`: completed outcomes `FAILED`, `STOPPED`, and `DUPLICATE_OR_UNCERTAIN` retain their closed meaning and do
  not become panel miss, radar miss, exit, or recovery success.
- `E-06`: malformed action ID, device/window, step count/index/type/status, frame purpose/region/dimensions/SHA,
  or missing raw PNG fails closed with zero later command.
- `E-07`: same action is never resubmitted; transport retry count remains zero.
- `E-08`: active-source gate covers old holder/coordinator references in method bodies, old facade/fact/macro,
  direct input/capture, all seven old `BATTLE_RADAR_*` facts, and Summon authority. Current field reflection covers
  only three direct field types. Parent Repair #1 also forbids source-string scanning, so receipt must require a
  non-source-text proof (for example compiled dependency/bytecode evidence) rather than accepting the partial
  field reflection as the full source gate.

## 9. False-proof risks in the current fixture

1. **Unused helper risk:** helper declarations and real template construction are not executable coverage.
2. **Adaptive-ROI risk:** `completedCapture` derives metadata from whatever ROI production requested, and
   `battleFlagRoiPng` sizes itself to that request. Without first asserting the exact expected region/action shape,
   a wrong ROI can receive a perfectly matching synthetic frame and pass.
3. **Under-command risk:** `ScriptedCommandPort` detects extra commands but not missing commands unless the test
   asserts `replies.isEmpty()` and exact `actions.size()`.
4. **Default-state risk:** comparing fresh `false/0` values across scopes does not prove isolation. Mutate one scope
   into a non-default observable state first.
5. **Direct-state risk:** setting `GameContext.ActionState` directly may establish a test precondition, but cannot be
   cited as proof that a real template frame caused enter/exit.
6. **Zero-command risk:** zero-command stop/invalid-ROI cases cannot prove UUID freshness, exact action ordering,
   completed action handling, or terminal behavior.
7. **Hardcoded-role risk:** current `TASK_METADATA` is Xiuluo LEADER only. It cannot reach AutoBattle MEMBER follower
   deferral, local-support gates, or task/requested-task distinctions without a metadata factory.
8. **Mocked-caller risk:** a test-private copied caller switch or mocked AutoCombat return does not lock the four
   production caller branches.
9. **Reflection-surface risk:** method/enum/field reflection proves shape only, not overload mapping, return
   semantics, source dependencies, timing, or action behavior.
10. **Clock risk:** production timing uses `System.currentTimeMillis`; the current harness has no deterministic service
    clock. Broad sleeps or loose `>0` assertions can pass incorrect `4s/15s/40s` constants. Receipt should require
    tight observable deadlines/boundaries without private-state reflection or a production test hook.
11. **Incomplete-exit-frame risk:** blank auto/selection/top frames do not prove exit. Full exit also requires the
    second miss and a readable real minimap-coordinate PNG.
12. **Stale-fixture risk:** unused `EmptyTemplateAssets` at test `:756-762` still says every assertion is a
    zero-capture path, while Repair #1 is supposed to add positive captures. It must not be wired back into the
    harness or cited as positive-template evidence.

## 10. Parent receipt order

1. Re-read card physical true EOF. Receipt does not start until a real `REPAIR #1 SOURCE+TEST DELIVERED` line exists.
2. Re-hash production and require exact SHA `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`.
3. Re-hash/re-read the complete latest test bytes; do not use this intermediate `60e49e...` SHA as delivery.
4. Clear CT-1 through CT-6 by source inspection before spending a Maven slot.
5. Confirm new `@Test` methods actually invoke the positive fixture and that all queued replies/actions are exactly
   consumed and ordered.
6. Check every `S/R/P/T/G/C/E` item above against executable lines. A helper, comment, method name, reflection-only
   shape check, or direct state assignment is not a substitute.
7. Only after writer stability and parent permission, run the frozen named test and applicable Cloud compile/build.
   This helper did not run them and supplies no build evidence.

## 11. Precheck conclusion

- Parent-passed production is byte-stable.
- Repair #1 currently has useful positive-frame scaffolding but no new executable acceptance case.
- Six current test areas have deterministic construction/expectation defects before considering the much larger
  missing frozen matrix.
- All four real caller contracts, positive enter/exit, three recovery policies, CommonBox/first-aid ordering, FAST
  cadence, maintenance/panel geometry, UUID/terminal/correlation, and full source gate remain unproved in the
  current bytes.
- This report is a parent receipt checklist only and does not decide TURN-34A status.

<!-- PRECHECK_COMPLETE+TRUE_EOF: CR271 TURN-34A REPAIR-1 CURRENT-TEST PREFLIGHT HELPER NON-REVIEWER NON-OWNER SNAPSHOT=2026-07-16T09:08:11-04:00 TEST_SHA256=60e49ed9c641801af81d02df968c66acdb7be4b18bd6f225bfe70ddd14a8bbc6 PROD_SHA256=532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9 -->
