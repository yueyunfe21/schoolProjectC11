# CR271 TURN-34BT next test-only tranche preflight helper

## 1. Role and output boundary

- This is an Internal readiness-helper report for parent freeze only.
- This helper is not the TURN-34B implementer, a reviewer, or a parent approver.
- The only write made by this helper is this report. Cloud/DHXY Java, TURN-34B/TURN-34BT1 cards,
  authority plan, `ACTIVE_WORK.md`, CR271, matrix/dashboard, Maven/runtime/input and Git state remain untouched.
- Suggested child labels below are decomposition labels only. They do not create a card or grant ownership.

## 2. Evidence snapshot

- Authority read: `AGENTS.md`, `docs/DHXY_CONTEXT.md`, the top CR271 section of `docs/ACTIVE_WORK.md`,
  authority-plan Sections 14-19, the HTTPS turn protocol, `docs/业务逻辑.md`, and the fixed TURN-34B and
  TURN-34BT1 cards.
- Current Cloud production:
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java`,
  1224 lines, SHA-256 `963b028c4a753efcc0263e402d6aba310e51c2591aca5e9717afe92912a66bbc`.
- Baseline authority: DHXY commit `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`; baseline blob for
  `TaskMaintenanceService.java` is `e93cfd01d9c282f98881a6311b8bb806bbc3e359`.
- The current file still has exactly 19 public methods. `runOpportunisticMaintenance`,
  `handleMaintenanceBroadcast`, and `maybeCleanSummonSkill` are baseline-identical after normalizing the sole
  exact-context call delta `summonSkillState(windowKey, context)` back to baseline
  `summonSkillState(windowKey)`.
- These current methods are byte-for-byte method-body equal to `696a12b0`: `initializeForTaskStart`,
  `beginTeamMaintenanceRound`, `openTeamPathingMaintenanceWindow`, `openTeamFirstAidMaintenanceWindow`,
  `closeTeamMaintenanceWindow`, `openLocalTeamReturnSupportWindow`, `closeLocalTeamReturnSupportWindow`, and
  `isTeamPathingMaintenanceWindowOpen`.
- At this observation point the sole target
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java` is absent. That agrees
  with the physical EOF of TURN-34BT1: BT1 must create the file and hand it off before any later tranche edits it.
- Business rows checked: local-team capability/session isolation; CommonBox priority over Summon and all other
  opportunistic maintenance; Summon static slot rules and `UNKNOWN` fail-closed meaning; maintenance only when
  the existing timing/condition says due; no new TTL/retry/cleanup/phase/fallback; STOP/pause is not business
  failure. No approved business difference is assumed.

## 3. Public surface allocation

All 19 signatures remain one production contract. The test tranches should allocate them as follows without
changing a signature:

| Surface | Public APIs | Test tranche owner |
|---|---|---|
| Exact context/API shape | all 19 signatures; especially the six TURN-34A APIs | TURN-34BT1 |
| Priority/result spine | `initializeForTaskStart`, `runOpportunisticMaintenance` | next child, suggested `TURN-34BT2` |
| Stateful Summon gates/claims/cache | the BT2 pair plus `beginTeamMaintenanceRound`, `openTeamPathingMaintenanceWindow`, `openTeamFirstAidMaintenanceWindow` | queued child, suggested `TURN-34BT3` |
| Capability sets and query semantics | `openTeamPathingMaintenanceWindow`, `openTeamFirstAidMaintenanceWindow`, `closeTeamMaintenanceWindow`, `openLocalTeamReturnSupportWindow`, `closeLocalTeamReturnSupportWindow`, `isTeamPathingMaintenanceWindowOpen`, `awaitTeamFirstAidMaintenanceWindowOpen`, `awaitLocalTeamSupportCapabilityOpen`, `isLocalTeamSupportCapabilityOpen` | queued child, suggested `TURN-34BT4` |
| Zero-production-caller session lifecycle | `registerLocalTeamSessionCandidate`, `markLocalTeamWindowRoleDetected`, `markLocalTeamLeaderDetected`, `completeLocalTeamSessionWindow` plus member-candidate queries | signature coverage in BT1; test-only state seeding only if parent explicitly freezes it for BT4 |

The three later labels all modify the same sole named test. They are strictly serial, never parallel.

## 4. Exact next tranche: suggested TURN-34BT2

### 4.1 Mutually exclusive write set

1. Modify only Cloud
   `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java` created by BT1.
2. Append only the parent-created BT2 fixed card, if the parent creates one.
3. Read only `TaskMaintenanceService.java` at the parent-recorded handoff SHA and every other file.

BT2 may be claimed only after BT1 has written a physical-EOF delivery/owner release and the parent has recorded
the target test's starting SHA. Same-worker continuation still requires that explicit BT1-to-BT2 boundary. A
second writer cannot start while BT1 owns or edits the file.

### 4.2 Required harness reuse

- Extend BT1's real `TaskMaintenanceService` fixture and test-private scripted `DialogService` /
  `SummonSkillService`; do not create a second harness class/file.
- Reuse BT1's real `TaskExecutionContext.turnNative(...)`, exact metadata and counting `TurnGameClient` fixtures.
- Use one ordered event list (`dialog`, `summon`) plus delegate/action/UUID counters. Assertions must observe the
  real return/exception from `runOpportunisticMaintenance`; a manually fabricated maintenance result is not a
  substitute for the service call.
- Configure the new service instance as feature-enabled, positive interval, FREE, immediately due and
  `oneSummonSkillPerTeamRound=false` for the common happy fixture. This keeps BT2 focused on priority/result, not
  BT3 team/cache state.

### 4.3 Broadcast/Summon priority assertions

1. Both request flags false: exact `NO_ACTION`; Dialog=0, Summon=0, action/UUID=0.
2. Broadcast enabled and Dialog status `BUSINESS_OPTION_CLICKED`: exact broadcast-handled result and handled flag;
   Dialog=1, Summon=0. Capture and assert `sourceTask` and `allowFullMaintenanceBroadcastFallback` in the real
   `DialogHandleRequest`.
3. Broadcast status `FAILED`: exact `BROADCAST_FAILED`; Dialog=1, Summon=0.
4. Broadcast status `INTERRUPTED`: exact `INTERRUPTED`; Dialog=1, Summon=0; it is not business failure.
5. Any existing Dialog status outside `{BUSINESS_OPTION_CLICKED, FAILED, INTERRUPTED}`: broadcast returns no
   action. With Summon disabled in the request, final result is `NO_ACTION`; with Summon requested and eligible,
   events are exactly `[dialog, summon]`, each once.
6. Broadcast flag false and Summon flag true: Dialog=0 and the single typed
   `cleanSummonSkillsOnce(SummonSkillCleanupRequest)` delegate is called once.
7. No branch may invoke CommonBox, TeamReturn, a second Dialog pass, a second Summon pass, `TurnGameClient`, or
   generate an action/UUID in `TaskMaintenanceService` itself.

### 4.4 Deterministic front-gate assertions

Each row must make all earlier gates pass, make the named gate fail, and assert every later delegate/state action
is untouched. Paired failing inputs should prove order from the exact result/message, not only from zero calls.

| Gate order | Setup | Exact observable |
|---|---|---|
| 1 feature | feature disabled, even if interval/non-FREE also invalid | `SUMMON_SKILL_DISABLED`, message `summon skill maintenance disabled`, Summon=0 |
| 2 interval | feature enabled, interval `<=0`, even if non-FREE | `SUMMON_SKILL_DISABLED`, message `summon skill interval disabled`, Summon=0 |
| 3 FREE | positive interval, `requireFreeStateForSummonSkill=true`, action state not FREE | `SUMMON_SKILL_DEFERRED`, non-free message, Summon=0 |
| 4 due | call `initializeForTaskStart` with immediate-on-start false and a large positive interval | `SUMMON_SKILL_NOT_DUE`, Summon=0 |
| final checkpoint | first checkpoint passes; scripted exact metadata exposes STOP at the checkpoint immediately before delegate | the real stop transition propagates; Dialog/Summon/action/UUID=0; no false result |

No sleep, private-field mutation or wall-clock race is needed for these rows.

### 4.5 Direct result/exception projection assertions

1. Scripted success: exact `SUMMON_SKILL_CLEANED`, one delegate, `summonSkillAttempted=true`, and prior
   `GameContext.ActionState` restored when the delegate leaves it at `INTERACTING`.
2. Scripted known failure with no delete/ultimate state change: exact
   `SUMMON_SKILL_FAILED_RETRY_LATER`, attempted=true, one delegate, no internal second call. A second explicit
   service invocation may call the delegate again; that is caller retry, not automatic retry.
3. Scripted delegate terminal/uncertain/STOP channel: use the actual typed channel exposed by the frozen TURN-33
   public API. If that channel is an exception, assert the same exception escapes; do not encode it as a failure
   message. In every case there is no false success/false result, delegate count is one, action/UUID count remains
   zero at this boundary, and prior action state is restored by `finally` when still `INTERACTING`.
4. If the scripted delegate deliberately changes the action state away from `INTERACTING`, assert the service
   does not overwrite that newer state. This preserves the baseline guarded restoration branch.
5. All result cases assert one invocation maximum and no transport retry, second command, cleanup loop, or copied
   TURN-33 mechanics.

## 5. Queued stateful tranche: suggested TURN-34BT3

BT3 is not parallel work. Its only Java write is the same test after BT2 handoff. Parent freeze should require the
following exact branch matrix:

1. Unknown failure by existing message/`UNKNOWN` slot evidence records the configured retry-after gate; the next
   immediate explicit invocation returns `SUMMON_SKILL_DEFERRED` and does not call the delegate again.
2. A successful non-tail-safe pass seeds skill count/start slot. `initializeForTaskStart` with immediate-on-start
   true removes only the cooldown; the next delegate request must carry exact cached `expectedSkillCount`,
   `trustExpectedSkillCount=true`, and `startSlotIndex`.
3. A successful tail-safe pass, followed by the same public cooldown reset, must return
   `SUMMON_SKILL_NOT_DUE` from the fresh tail-safe cache with no second delegate.
4. Prior `ultimateGenerateSucceeded=true`, followed by public cooldown reset and a new explicit pass outside team
   claims, must set `skipUltimateCornerCheck=true` in the next typed request.
5. `oneSummonSkillPerTeamRound=true` with no round returns `SUMMON_SKILL_DEFERRED`, delegate=0.
6. A formal round that is only CLOSED or `FIRST_AID_WINDOW_OPEN` does not satisfy
   `requireOpenTeamMaintenanceWindow`; only `PATHING_WINDOW_OPEN` permits the delegate.
7. When `requiredLocalSupportCapability` is non-null, the existing local-capability epoch path is used; it does
   not silently switch to the formal pathing-window gate.
8. A successful same-window claim remains claimed; the next explicit same-round call returns
   `SUMMON_SKILL_ROUND_ALREADY_CLAIMED`, delegate count unchanged.
9. With max cleaners `<=0`, effective max is exactly one. Window A claims; window B in the same round receives
   `SUMMON_SKILL_ROUND_ALREADY_CLAIMED` and cannot delegate.
10. Known failure with no state change releases its owned round claim; a later explicit call may claim and invoke
    once. Failure with `deletedCount>0`, `ultimateGenerateClicked=true`, or
    `ultimateGenerateSucceeded=true` retains the claim. Parameterize those three state-change facts rather than
    copying mechanics.
11. Success sets cooldown; immediate next invocation is not due. Known failure does not invent a success cooldown.
12. The checkpoint immediately before action remains after claim/gates and before `GameContext.INTERACTING` and
    delegate. STOP there produces no delegate and no auto retry.

The fixed 2-hour expiration branch cannot be reached deterministically through the current public API without a
2-hour wait, private-state reflection, JVM instrumentation, or a production clock seam. BT1 forbids private
production reflection, and TURN-34B forbids a production test hook/new timer abstraction. Parent freeze should
therefore keep expiration simulation out of BT3 unless it explicitly chooses a narrow test mechanism; the worker
must not improvise reflection, sleep, `Unsafe`, or a production clock change. Fresh-cache semantics and the exact
2-hour production constants remain source-review evidence until that parent decision.

## 6. Queued capability tranche: suggested TURN-34BT4

BT4 is likewise serial on the sole test file. It should use public state/query APIs only and timeout `0` for await
checks; no waiter thread or wall-clock wait is needed.

1. Pathing open: exact set
   `{FIRST_AID, PATHING_WINDOW, COMMON_BOX, SUMMON_SKILL, LEFT_TOP_STATUS}` is open; `TEAM_RETURN` is closed;
   `isTeamPathingMaintenanceWindowOpen` is true. This is the first `5`.
2. First-aid-only open on a fresh session/round: only `{FIRST_AID}` is open;
   `awaitTeamFirstAidMaintenanceWindowOpen(..., 0)` is true and pathing query is false. This is `1`.
3. Team close after pathing open closes exactly the first five. Seed `TEAM_RETURN` as well and prove it remains
   open. This is the second `5`; do not add reference counting or ownership.
4. Local return-support open opens exactly `{TEAM_RETURN, COMMON_BOX}` and no other capability. Return-support
   close closes exactly those two. This is `2`.
5. Preserve the baseline overlap semantics: if pathing and return support are both open, closing return support
   removes `COMMON_BOX` even while other pathing capabilities remain; closing team maintenance leaves
   `TEAM_RETURN` untouched. A test must not "repair" this with a new lease/reference count.
6. `isLocalTeamSupportCapabilityOpen` and `awaitLocalTeamSupportCapabilityOpen(..., 0)` must agree for open/closed
   states and stay session-isolated.
7. To exercise the true local-member capability branch without reflection, BT4 may need test-only calls to the
   existing public candidate/leader APIs. Such calls do not create a production host/factory/runtime caller, but
   the parent should state this permission explicitly because BT1 only checks those four zero-production-caller
   APIs by signature and does not activate them.

## 7. Why test-source can proceed before TURN-22's final gate

1. TURN-22 Repair #3 writes Cloud `TeamReturnTurnContractTest.java` plus DHXY
   `TurnInputStepExecutor.java`/`TurnInputStepExecutorContractTest.java`. BT2/BT3/BT4 write only Cloud
   `TaskMaintenanceTurnContractTest.java`; the Java write sets are disjoint.
2. Current `TaskMaintenanceService` never calls `TeamReturnService`, emits TeamReturn JSON/input, consumes a
   CommonBox, or creates an action/UUID for return support. Its return-support APIs only add/remove the existing
   `TEAM_RETURN+COMMON_BOX` enum capabilities.
3. Authority-plan Sections 14, 16 and 18 plus the top CR271 note explicitly separate source start from the final
   source/review/build gate when the frozen upstream production API exists and remaining work is in disjoint test
   or integration files.
4. These tranches run no Maven/JUnit/compile and start no runtime/input. TURN-22 remains a final integration,
   source-review and build dependency for TURN-34B; it is not evidence that this disjoint test source must sit idle.
5. The actual immediate serialization dependency is BT1/BT2/BT3/BT4 sharing one test file, not TURN-22. Parent must
   enforce true-EOF handoff between those tranches.

## 8. Prohibitions for every later tranche

- No modification to `TaskMaintenanceService.java`, `AutoCombatService`, `AutoBattleTask`, any Task, TURN-33
  `SummonSkillService`, TURN-22 TeamReturn code/test, protocol/client/result, models, POM/config/resources, or DHXY.
- No second test class, nested production hook, clock/sleeper abstraction, source scan, private-production
  reflection, Mockito, Spring context, HTTP, application/server/Task start, UI, OCR, capture, or desktop input.
- No real CommonBox/TeamReturn consumption and no reproduction of TURN-33 static-tail, delete, ultimate, PNG/OCR,
  click, cleanup, action or UUID mechanics.
- No new timer/TTL, retry, cleanup, fail-closed rule, claim owner/session/lease/ledger/compaction/durable workflow,
  or altered priority/capability overlap semantics.
- No Maven/JUnit/compile/package while writers are active; no runtime/application/server/input; no Git mutation.
- A failing assertion against the current production WIP is evidence for parent-directed production repair. The
  test-only worker must not weaken the assertion, fabricate a result, or edit production outside a newly frozen
  write set.

## 9. Parent handoff checklist

- Re-read TURN-34BT1 physical EOF and target test SHA immediately before freezing the next child.
- Record exactly one owner for the sole test file and require BT1 delivery/release before BT2 claim.
- Freeze BT2 first; BT3 and BT4 above are queued decomposition, not concurrent assignments.
- Preserve production SHA `963b028c...` as read-only unless the parent separately reopens TURN-34B production.
- Keep TURN-22 as the final source/integration/build gate while allowing the disjoint serial test-source chain.
- Record: `无已批准业务差异；按 696a12b0 基线等价迁移`.

TRUE_EOF PRECHECK_COMPLETE
