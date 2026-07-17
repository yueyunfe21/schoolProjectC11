HELPER CLAIMED

# TURN-34A External C implementation launch-preflight (R2)

- Snapshot: `2026-07-16T05:02:41.999-04:00`.
- Role: External C launch helper only; not TURN-34A implementer, reviewer, manager, or approver.
- Result: `PRECHECK_COMPLETE / LAUNCH PACKET READY / TURN-34A NOT CLAIMED`.
- Current launch state: **closed**. During final verification TURN-33 advanced to parent decision
  `FRESH_STATIC_RESCAN MAX_DELETE_5`, but remains `REPAIR REQUIRED`: Leibniz has not appended a new
  `REPAIR #1 SOURCE+TEST DELIVERED`, and the parent has not written a repaired
  `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED` decision.
- This report is non-binding until the parent releases TURN-34A. It does not create the TURN-34A card,
  assign ownership, approve source, or authorize early edits.

## 1. Exact launch gate

Authority: the plan registry says `TURN-34A | PLANNED | S=19+20+21+23+24+33` and assigns
`AutoCombatService` all baseline public callers as a one-production-file owner. `S=` is the source-start
gate; dependency named tests/builds may remain in the later writer-stable cohort, but they still gate final
card approval.

| Dependency | Latest parent source decision | Frozen input for 34A |
|---|---|---|
| TURN-19 | `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED` | exact bound client before port; `MOVE -> WAIT120 -> CLICK -> WAIT250`; one UUID/command; zero retry |
| TURN-20 | `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED` | panel visibility/round/drag/refresh semantics; known failure mappings; `500/1000ms`; canonical OCR; zero retry |
| TURN-21 | `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED` | CommonBox current title/process/HWND fence, 30s pending, `MOVE -> WAIT80 -> CLICK -> WAIT120` |
| TURN-23 | `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED` | first aid/incense typed actions, pre-UUID HWND/process fence, cached `48x34` then full `123x34` business fallback |
| TURN-24A | `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED` | four-stage radar, two misses+minimap, `15s/1s/4s`, `20x20`, `0.35`, RGB tolerance `15`, confirmed STOP propagation |
| TURN-33 | `P1-2 CONTRACT DECIDED / REPAIR #1 REQUIRED` | fresh static rescan after each real delete, shared max-5 budget, fifth stops and sixth UUID/command is zero; **source gate not satisfied** and no AutoCombat/Summon coupling is authorized |

External C may receive and claim TURN-34A only after all of these are true:

1. The TURN-33 card true EOF contains a new parent-authored decision explicitly stating
   `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED` for the latest Repair bytes.
2. The parent marks TURN-34A `READY` and freezes/creates its fixed implementation card. A worker
   `SOURCE+TEST DELIVERED`, a partial repair, a helper opinion, or dependency build-pending text is not the
   TURN-33 source-release signal.
3. External C reads the then-current bytes and appends its own real `CLAIMED` marker to that fixed TURN-34A
   card before any source/test edit. This helper has not done so.

## 2. Exact ownership after release

Production write set, exactly one file:

- `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/AutoCombatService.java`

Named-test write set, exactly one file:

- `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java`

Fixed implementation report, only after the parent creates/freezes it:

- `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34A.md`

No other production, test, fixture, helper, resource, config, POM, protocol, DHXY, caller, plan, card, or
dashboard file belongs to External C. In particular, do not edit `TaskMaintenanceService` (TURN-34B),
`task/AutoBattleTask` (TURN-34C), Wubei/FiveRing/Xiuluo tasks (TURN-35/36/37), host/runtime activation
(TURN-40), or any TURN-19/20/21/23/24/33 file. Work with all existing dirty/untracked bytes; do not revert,
move, stage, or clean them.

## 3. Production byte baseline and required ownership move

Current Cloud file evidence:

- SHA-256: `80380B8D65EAA4230886AD233DFBD49D8BED91F44F54BCFEA7AFE2B45BB5632D`.
- Baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`, blob
  `b1c2d48e89ed6b2ca90b1639df841dd7a97d691a`.
- The only current Cloud-vs-696 business-visible diff is the already-closed UI-cleaner port:
  `UICleanerService.closeAllGenericWindows()` became
  `CloudUiCleanerPort.closeAllGenericWindows("auto-combat", exact-slot)` at the same two baseline positions.
  Preserve it.
- The file still imports `TaskTurnCoordinator`, `WindowRuntimeContext`, and `WindowTaskContextHolder`; none
  exists in Cloud main source. It also wraps CommonBox and follower-first-aid calls in
  `TaskTurnCoordinator.enter/forceRelease` and keys state through a local window holder with
  `"default"/epoch=0` fallbacks. Those are the one-file migration targets.

Required one-file implementation shape:

1. Replace `WindowTaskContextHolder` with the existing Spring `TaskExecutionContextHolder`. Every public
   no-context method must resolve the currently bound exact `TaskExecutionContext`; missing binding fails
   before any collaborator action. Never fall back to `"default"`, `0`, the first window, or a global title.
2. Build the logical state key from the current context's exact
   `tenantId + userId + deviceId + windowId`, using `getTurnServiceScope()` and
   `getTurnInvocationContext()`.
3. Store the immutable initial native fingerprint
   `windowTitle + nativeHandle/HWND + processId` inside that logical key's runtime state. On a new exact
   context with the same logical key but a different fingerprint, atomically replace the state. Do not put
   the fingerprint in the map key alone: an `A -> B -> A` rebind must not resurrect A's old pending/deadline
   state. Do not call turn-native `getPlayerIdentityEpoch()`; it delegates to removed legacy authority and
   throws.
4. Do not add an AutoCombat metadata re-read. Command-time latest identity validation remains owned by the
   already-frozen typed collaborators. AutoCombat owns orchestration and immutable-context state only.
5. Remove `TaskTurnCoordinator` imports, field, enter/release wrappers, and transaction-name plumbing.
   Call the existing typed collaborators synchronously in the same source order. Do not replace the wrapper
   with a session, owner, lease, lock protocol, ledger, retained invocation, queue, or cross-action authority.
6. Keep `RefreshDuePanelVerifyDecision`, `RefreshDuePanelVerifyGate`, and
   `reserveIfAllowed(String,String,long)` public. They have no external production caller, but API shrinking
   is not part of 34A. Preserve the existing 30s team-sharing decision and its direct-call semantics; the
   production caller must pass the exact current window id and must not use a default state key.
7. Any new key/fingerprint value type must be a private nested type at the bottom of the same file. Do not
   create a new production helper or wrapper layer.

## 4. Baseline public surface and every caller

The 34A public surface is the `696a12b0`/current Cloud surface below. Keep signatures and enum values exact.

| Public surface | Current production caller/consumer |
|---|---|
| `TickResult { NONE, IN_COMBAT, EXIT_RECOVERED }` | AutoBattle, FiveRing, Wubei, Xiuluo phase logic |
| `PostCombatRecoveryPolicy { FULL_RECOVERY, FULL_RECOVERY_WITH_LEADER_INCENSE, FAST_EXPECTED_EXIT }` | Wubei and Xiuluo; boolean overload maps to the first two |
| `initializeForCurrentWindow()` | AutoBattle `:137`; Wubei `:351,788,3447,3624`; Xiuluo `:2052,2229,2793,2803`; no current Cloud FiveRing call |
| `handleCombatTick(context,source,boolean)` | AutoBattle `false` at `:163`; FiveRing `true` at `:1853` |
| `handleCombatTick(context,source,policy)` | Wubei full+incense `:3595`, fast `:3756`; Xiuluo shortcut `:1828`, phase call `:2063` |
| `handleWindowCombatGuardTick(context,source)` | no current Cloud caller; baseline DHXY `WindowTaskRunner` watcher; retain public and exact |
| `probeWindowCombatStateReadOnly(context,source)` | Wubei `:4164`; Xiuluo `:2436`; baseline runner hot-start |
| `getDynamicPollingIntervalMs()` | AutoBattle `:287`; baseline runner watcher |
| `nextCombatMaintenanceDelayMs()` | no external caller; `nextCombatWakeDelayMs()` consumes it |
| `nextCombatWakeDelayMs()` | Wubei `:918`; Xiuluo `:2248` |
| `hasPendingFollowerFirstAidForCurrentWindow()` | AutoBattle `:281` |
| `hasPendingLeaderPostCombatRecoveryForCurrentWindow()` | Wubei/Xiuluo safe-point checks, including Xiuluo `:2468` |
| `refreshFastExpectedExitBaselineAfterTrustedInCombat(source)` | Wubei `:4167`; Xiuluo `:2439`, only after trusted `IN_COMBAT` |
| `consumePendingLeaderPostCombatRecoveryIfAllowed(context,source)` | Wubei `:2777`; Xiuluo `:2471`; callers do not promote its boolean into phase truth |
| public `RefreshDuePanelVerifyDecision`, `RefreshDuePanelVerifyGate`, `reserveIfAllowed(...)` | class-internal refresh-due path only; keep public shape |

Caller phase contracts that the named test must execute, not merely describe:

| Caller | Exact contract |
|---|---|
| AutoBattle | startup first aid/maintenance then initialize; each loop checkpoints then calls boolean `false`; any result other than `NONE` sleeps/continues; pending follower delay `500ms`, `FREE` delay `3000ms`, otherwise dynamic delay |
| FiveRing | calls boolean `true` only after combat was previously observed and watcher reports inactive; `IN_COMBAT` stays in shared wait; `NONE` logs but follows the same `SYNC_TASK_PANEL` continuation as `EXIT_RECOVERED`; 34A must not add a task-side initialize call |
| Wubei | full+incense enter phase: `IN_COMBAT -> WAIT_BATTLE_FINISH`, `EXIT_RECOVERED -> POST_BATTLE_RECOVER`, `NONE` continues existing resolver; fast wait: exit recovers, in-combat prescans/parks, `NONE` returns to enter only under the existing never-seen-combat retry; trusted return correction refreshes fast baseline only for `IN_COMBAT`; wake delay clamp `500..10000ms` |
| Xiuluo | shortcut full+incense only treats `IN_COMBAT` as incidental wait; tracker-confirm expected battle selects FAST, otherwise full+incense; exit follows incidental/unknown/expected existing branches; `IN_COMBAT` prescans/parks; `NONE` preserves retry/fallback; trusted return correction only for `IN_COMBAT`; wake clamp `500..10000ms` |
| Baseline WindowTaskRunner | hot-start uses read-only probe and clamps dynamic sleep `500..4000ms`; watcher initializes then uses guard; guard/read-only never consume exit. Cloud host activation remains TURN-40, not 34A |

### Explicit non-surface

Current DHXY added six APIs after `696a12b0`; they have real local callers but are not in the Cloud baseline
and are not authorized for 34A:

- `authorizeCombatDetectionAfterEnterBattleAction`
- `revokeCombatDetectionAuthority`
- `probePausedWindowCombatStateReadOnly`
- `consumeQueuedLeaderPostCombatFirstAidIfHead`
- `reportXiuluoLeaderFirstAidAfterVerifiedReturn`
- `reconcileReturnHomeVerifiedCombatState`

Do not migrate their CR243/252 authority, pause probe, queue/head, report, or return-reconcile semantics. If
the parent wants any of them, it requires a separate approved behavior difference plus caller write set;
External C must stop instead of folding them into this one-file card.

## 5. Dynamic delay, enter, exit, and recovery baseline

### Dynamic/radar

- `getDynamicPollingIntervalMs()` remains `IN_COMBAT=4000ms`, `NAVIGATING/INTERACTING=2000ms`,
  `FREE/default=10000ms`. Caller-specific `500/3000` and `500..10000`/`500..4000` rules remain outside the
  service exactly as listed above.
- Full radar order remains auto flag `(974,630,51x20)`, selection `(927,302,100x225)`, top
  `(456,62,123x39)`, then, only for remembered combat after two misses, readable minimap
  `(46,59,178x35)`. Unavailable evidence conservatively remains `IN_COMBAT`.
- FAST remains `15,000ms` arm gate, `1,000ms` probe, `4,000ms` full fallback, avatar ROI `20x20`, pixel
  diff `>0.35`, RGB tolerance `15`. Do not add a probe or shorten the full-radar fallback.
- `nextCombatMaintenanceDelayMs()` is the minimum remaining entry/clean/panel deadline;
  `nextCombatWakeDelayMs()` is the minimum of maintenance and an armed fast-probe deadline; `-1` keeps its
  existing no-deadline meaning.

### Enter/exit

1. Every tick begins at the existing `context.throwIfStopRequested()` checkpoint. `null` policy maps to
   `FULL_RECOVERY`; boolean `false/true` maps exactly to full/full+leader-incense.
2. FAST arms expected-exit once. While remembered `IN_COMBAT`, avatar fast probe runs first; full radar only
   follows a fast miss when the `4s` fallback is due. Other policies use full radar.
3. A consumed enter signal schedules maintenance at `now+4000ms`, resets the generic-clean baseline, and
   ensures the panel visible with the existing `500ms` wait. A radar-confirmed in-combat state discards stale
   exit before exit consumption.
4. Exit consumption uses a fresh expected-wait signal for FAST and ordinary exit for full policies; then
   clears expected/entry state, records panel rounds `-3`, resets first-aid counter, and detects member
   CommonBox.
5. FAST exit clears follower pending, sets deferred leader recovery, disables fast state, sets `FREE`, and
   performs no immediate leader first aid/incense.
6. Full follower recovery preserves the exact reassigned-auto-battle split:
   `SUPPLY_NEEDED/UNKNOWN -> pending`, `HEALTHY -> clear`; other follower paths consume cached plan only for
   `SUPPLY_NEEDED`, while `UNKNOWN` logs without invented success. Then checkpoint; only full+incense runs
   leader incense; finally set `FREE`.
7. After exit, and also on later pending ticks, action priority is `CommonBox -> follower first aid`. A
   successful box click leaves first aid for the next tick. Consumed exit always returns `EXIT_RECOVERED`;
   a later pending action that actually runs also returns `EXIT_RECOVERED`; otherwise combat returns
   `IN_COMBAT`, and free/no-action returns `NONE`.

### Pending/recovery/maintenance

- `initializeForCurrentWindow()` resets refresh/clean/entry, follower pending, fast/expected flags, and
  verify flag, but deliberately does **not** clear deferred leader recovery.
- Follower first aid runs only while `FREE`; team waits remain at most `3000ms`; cached plan is first, only
  one baseline re-probe is allowed, `UNKNOWN` keeps pending, other results clear it.
- Deferred leader consume returns false when absent or still in combat. At an allowed safe point it clears
  pending **before** first aid/cached-plan work, then checkpoint, then incense, and returns true. Do not move
  the clear or make caller boolean a new phase fact.
- Maintenance reason priority remains `UNKNOWN -> LOW_ROUNDS(<=10) -> REFRESH_DUE`. Entry `+4s` performs
  generic clean then panel verify and may merge a due refresh. Periodic `40s` performs generic clean then
  left-top gate/action. Refresh-due team guard and urgent per-window retry remain `30s`; deferred log throttle
  remains `10s`. Panel target stays `(left+489, top+726)` and drag occurs only for distance `>20px`.
- Existing CommonBox pending `30s` and panel `30s` timing are baseline business rules. Add no second TTL,
  expiry, retry window, cleanup cadence, or verification.

## 6. Frozen collaborator boundaries

AutoCombat remains an orchestrator. It must call the already-migrated production services, not inject
`TurnGameClient`, construct an HTTPS action, crop a frame, match a template, run OCR, or send input itself.

| Owner | AutoCombat-consumed surface |
|---|---|
| TURN-19 | `LeftTopStatusSwitchService.handleCombatMaintenance(...)` |
| TURN-20 | panel `ensurePanelVisible`, `verifyAndAlignPanel`, `recordCombatExit`, `resolveRoundsRefreshReason` |
| TURN-21 | CommonBox detect/has-pending/consume, with CommonBox before first aid |
| TURN-23 | reset counter, no-focus first-aid probe, cached plan/one re-probe, incense |
| TURN-24A | arm/fast/full radar, stale-exit discard, enter/exit signal consume, dynamic/wake delay, trusted baseline refresh |
| closed UI cleaner | `closeAllGenericWindows("auto-combat", exact-slot)` at the same two positions |

Freeze these six `TaskMaintenanceService` calls while TURN-34B owns that file:

- `isPendingLocalSupportLeaderDetection(context)`
- `isLocalSupportMemberSession(context)`
- `isLocalTeamSupportCapabilityOpen(context, capability)`
- `awaitLocalTeamSupportCapabilityOpen(context, capability, timeoutMs)`
- `isLocalSupportMemberCandidate(context)`
- `awaitTeamFirstAidMaintenanceWindowOpen(context, teamMaintenanceKey, timeoutMs)`

TURN-33 is only the source/architecture launch gate. `AutoCombatService` has zero direct Summon dependency;
do not add `SummonSkillService`, whole-pass capability, Summon authority, cooldown, or cleanup calls.

## 7. HTTPS mechanics and terminal contract

- Each explicit business observation/input/local-service operation remains one closed typed action with one
  fresh UUID and one command. A baseline re-observe/re-probe is a new business action and therefore a new
  UUID. The same command is never resent.
- AutoCombat must not combine different business actions into a retained session, and must not split an
  already-frozen collaborator input bundle. The physical queue ownership remains inside DHXY's execution of
  that one command.
- Metadata lookup itself allocates no UUID. Pre-command missing/mismatched exact binding or confirmed preflight
  stop remains zero UUID/zero command in the owning typed port.
- Preserve each collaborator's known `FAILED` mapping. Confirmed `STOPPED` propagates the task stop;
  `DUPLICATE_OR_UNCERTAIN`, timeout uncertainty, malformed metadata/correlation, bad frame/SHA/dimensions, or
  interrupted-uncertain must not become `NONE`, `false`, probe miss, exit, or recovery success.
- Do not catch a terminal exception to issue a compensation command. Transport retry count is zero. Business
  fallbacks are only the exact baseline full-radar fallback, one first-aid re-probe, panel re-observe after
  open/drag, and cached-then-full incense status read.
- Pause waits only at existing checkpoints and preserves phase, pending flags, counters, deadlines, and UUID
  count. Do not add a checkpoint, park/yield, loop guard, cleanup, retry, session, owner, ledger, TTL, lease,
  or durable workflow.

## 8. Named-test contract

The only test is `AutoCombatServiceTurnContractTest`; profiles are default `BC4+BASE` plus `TASK+STATE`.
Use only in-test fakes, scripted ports, or loopback. No real runtime, UI, capture, input, server, or desktop
action.

Mandatory proof style:

1. Instantiate production `AutoCombatService`; bind real turn-native contexts through production
   `TaskExecutionContextHolder.callWith(...)`; call its public production methods. Do not prove behavior only
   by reflecting constants, invoking a private helper, testing a copied reducer/DTO, or scanning source.
2. `TASK` coverage must execute the relevant current production caller phase/tick for AutoBattle, FiveRing,
   Wubei, and Xiuluo with scripted outcomes. If narrow reflection is unavoidable to reach an existing private
   phase entry or seed a timestamp, the final assertion must still run through that production caller/public
   AutoCombat path; reflection alone is not acceptance evidence.
3. `STATE` coverage must interleave tenant/user/device/window contexts; verify same-scope pause/resume
   continuity, cross-scope isolation, same logical scope native fingerprint replacement, and `A -> B -> A`
   no stale resurrection. Missing holder and wrong scope must produce zero collaborator action.
4. Keep the two public enums, every baseline public method, the public record/gate, and public gate method in
   coverage. Assert all six DHXY post-baseline APIs remain absent from Cloud. Do not add a second source guard
   or production test hook.

Minimum executable matrix:

| Group | Direct production-path assertions |
|---|---|
| Initialize/overloads | reset asymmetry including retained deferred leader; boolean false/true and null policy mapping |
| Full radar/FAST | call order, conservative unavailable, `15s/1s/4s`, full fallback only when due; rely on frozen TURN-24A pixel mechanics rather than copy them |
| Dynamic delay | exact `4000/2000/10000`; AutoBattle `500/3000/dynamic`; Wubei/Xiuluo `500..10000`; runner-equivalent `500..4000`; both minimum-deadline APIs and `-1` |
| Enter/exit | `+4s`, stale-exit discard, rounds `-3`, all three recovery policies and exact `TickResult` |
| Priority | CommonBox before first aid; successful box defers first aid; no-new-exit pending action returns `EXIT_RECOVERED` |
| Follower | reassigned `SUPPLY_NEEDED/UNKNOWN/HEALTHY`, cached plan, exactly one re-probe, UNKNOWN remains pending |
| Deferred leader | combat keeps pending; safe point clears before first aid/checkpoint/incense; caller boolean creates no phase truth |
| Maintenance | reason priority; `4s/40s/30s/10s`; merged entry refresh; target `(489,726)` relative to latest rect; drag threshold `>20` |
| Guard/read-only | no exit-signal consumption, no recovery/input, never `EXIT_RECOVERED`; trusted `IN_COMBAT` is the only fast-baseline refresh trigger |
| Caller phases | exact AutoBattle/FiveRing/Wubei/Xiuluo table in section 4, including `NONE` negative-signal behavior |
| One UUID/command | each action-capable branch uses the production typed collaborator/scripted turn path; assert fresh UUID per command, exact count/order, and no duplicate/compensation |
| Terminal/correlation | known failure mapping, confirmed STOP, uncertain, wrong action/window/step/frame/ROI/SHA/dimension/decode, `FAILED -> NOT_RUN`, zero retry |
| Source gate | in this same named test only, assert AutoCombat active source has no old holder/coordinator, facade/fact/macro/direct input/capture, or Summon authority reference |

Writer-stable execution command, after parent permission:

```text
mvn -q -Dtest=AutoCombatServiceTurnContractTest test
```

External C must also satisfy the applicable Cloud compile/package gate before delivery, but must follow the
parent's concurrent-writer timing. This helper ran neither command and claims no test/build result.

## 9. Zero-reference and stop-work gates

The final `AutoCombatService` active source must have zero direct references to:

- `TaskTurnCoordinator`, `WindowTaskContextHolder`, `WindowRuntimeContext`
- old remote game facade/capability/fact/macro/input-bundle authority
- `GameClientTracker`, local screenshot/temp path, Java Robot, keyboard/mouse/input queue APIs
- seven old `BATTLE_RADAR_*` facts and old panel/CommonBox/left-top facts
- Summon whole-pass/capability/exclusive authority

Keep `GameContext`, `TaskExecutionContextHolder`, frozen typed services/ports, business enums, and pure Cloud
decision/result types; those are not old authority.

External C must stop and report a conflict, without expanding the write set, if any of these occurs:

- TURN-33 lacks the explicit parent source-pass true EOF or changes again after release.
- A compile/reference issue appears to require editing a second production file, copying a local runtime type,
  changing a caller, or changing one of the six TaskMaintenance APIs.
- A proposed route changes a condition, priority, signal consumption, phase transition, action count/order,
  delay, fallback, checkpoint, pending-clear point, or terminal mapping from section 5.
- A test can pass only through copied logic, a private helper/constant assertion, a test-only production API,
  or a fake that bypasses the production orchestration/caller under test.
- Any route would add retry/session/owner/ledger/TTL/lease, a new probe/read, a new cleanup/verification, or a
  direct AutoCombat command.

## 10. Handoff verdict

This preflight has enough exact evidence to become External C's implementation brief immediately after the
TURN-33 parent source gate and fixed-card READY/CLAIM sequence. It is not an approval of TURN-33 or TURN-34A,
does not open the gate, and does not authorize edits now.

No Java, test, original card, authoritative plan, ACTIVE_WORK, Maven/runtime, or Git state was changed by this
helper. The only written file is this report.

HELPER CLAIMED
PRECHECK_COMPLETE

<!-- TRUE_EOF: TURN-34A LAUNCH-PREFLIGHT HELPER R2 HELPER_CLAIMED / PRECHECK_COMPLETE / NOT_CLAIMED / GATE_CLOSED -->
