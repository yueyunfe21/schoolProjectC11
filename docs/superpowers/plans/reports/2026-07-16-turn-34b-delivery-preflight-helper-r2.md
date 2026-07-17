# CR271 TURN-34B Active Delivery Preflight Helper R2

- Role: `CR271 Internal helper` only; not implementation owner, reviewer, approver, or parent adjudicator.
- Assignment: read-only active-delivery preflight for External D / TURN-34B.
- Snapshot time: `2026-07-16T08:36:24.1967696-04:00`.
- Helper identity: current CR271 thread executing the user's explicit helper assignment; no separate platform sub-agent UUID is asserted or fabricated.
- Sole output: this report. No Java, card, plan, `ACTIVE_WORK`, dashboard, or other document was modified.
- Prohibitions observed: no Maven/JUnit/compile/package, no runtime/application/server/Task/UI/capture/input, and no Git mutation.

This is `PRECHECK_ONLY / NON_PARENT_APPROVAL`. Any risk or missing evidence below is an index for the parent and
External D; it is not a P0/P1/P2 finding, delivery acceptance, blocker ruling, or approval.

## 1. Authority and active-delivery snapshot

Read in full or in the applicable authoritative range:

- `AGENTS.md`, `docs/DHXY_CONTEXT.md`, and the current CR271 block in `docs/ACTIVE_WORK.md`;
- authoritative plan sections 14-19;
- `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`;
- `docs/业务逻辑.md`, including the summon-skill baseline and maintenance/STOP rules;
- TURN-34B fixed card through its physical true EOF `EXTERNAL-D CLAIMED`;
- TURN-34A through Parent Review #1 and the latest Repair #1 start directive;
- TURN-33 through Parent Review #5 and the accepted independent R1/R2 `2/2` pass;
- current Cloud `TaskMaintenanceService`, the four real caller classes, the six TURN-34A API call sites, and the
  `696a12b0` migration-baseline copy.

Read-only repository state:

- DHXY branch `thin-client-design`; Cloud branch `navigation-migration`.
- Both repositories contain extensive pre-existing dirty/untracked work and remain protected.
- `0114604e` and `3b988caa` are repository HEADs, not business baselines.
- Sole business authority for this card remains
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` plus `docs/业务逻辑.md`.

TURN-34B ownership and bytes at this snapshot:

- External D has a valid physical-EOF `EXTERNAL-D CLAIMED` at `2026-07-16T08:10:00-04:00`; it is the unique
  implementation owner.
- Frozen initial production SHA was
  `39AEF8085FDC8AFA0E0F51F8016C307E6F34AB407BAF30CCE52C6E88F14CD996`, 1,130 lines.
- Current production is 1,224 lines, mtime `2026-07-16T08:17:40.6760891-04:00`, SHA-256
  `963B028C4A753EFCC0263E402D6ABA310E51C2591ACA5E9717AFE92912A66BBC`.
- The sole named test
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TaskMaintenanceTurnContractTest.java` is still absent.
- Therefore External D has made real production progress, but there is no true-EOF `SOURCE+TEST DELIVERED` and no
  complete delivery yet. Mid-edit source bytes must not be parent-reviewed as a final delivery.

Upstream state that TURN-34B must bind without reimplementing:

- TURN-33: Parent Review #5 and independent R1/R2 are all `P0/P1/P2=0/0/0`; dual review is `2/2`, while named
  test/Cloud build remain pending. Its public boundary is still one call to
  `SummonSkillService.cleanSummonSkillsOnce(SummonSkillCleanupRequest)`.
- TURN-34A: production source has parent review pass; its named test is in Repair #1. TURN-34B must preserve the six
  TaskMaintenance APIs consumed by the reviewed production SHA and must not touch `AutoCombatService` or its test.
- TURN-22: final source/integration approval remains a later gate. TURN-34B owns capability state only and must not
  duplicate TeamReturn JSON, timing, click, queue, frame, or UUID mechanics.

## 2. Frozen 19-public-API surface

Current `TaskMaintenanceService.java:70-584` exposes exactly these 19 public methods. The named test must lock name,
parameter types, return type, and caller-visible behavior without using private-helper reflection as behavioral proof.

| # | Public API | Current real direct caller boundary |
|---:|---|---|
| 1 | `initializeForTaskStart(TaskExecutionContext,String)` | `AutoBattleTask`, `WubeiTask`, `XiuluoTaskV2` |
| 2 | `beginTeamMaintenanceRound(TaskExecutionContext,String,int,String)` | `WubeiTask`, `XiuluoTaskV2` |
| 3 | `openTeamPathingMaintenanceWindow(TaskExecutionContext,String,int,String)` | `WubeiTask`, `XiuluoTaskV2` |
| 4 | `openTeamFirstAidMaintenanceWindow(TaskExecutionContext,String,int,String)` | `WubeiTask` |
| 5 | `closeTeamMaintenanceWindow(TaskExecutionContext,String,int,String)` | `WubeiTask`, `XiuluoTaskV2` |
| 6 | `openLocalTeamReturnSupportWindow(TaskExecutionContext,String)` | `WubeiTask`, `XiuluoTaskV2` |
| 7 | `closeLocalTeamReturnSupportWindow(TaskExecutionContext,String)` | `WubeiTask`, `XiuluoTaskV2` |
| 8 | `isTeamPathingMaintenanceWindowOpen(TaskExecutionContext,String)` | `WubeiTask` |
| 9 | `awaitTeamFirstAidMaintenanceWindowOpen(TaskExecutionContext,String,long)` | `AutoCombatService` |
| 10 | `awaitLocalTeamSupportCapabilityOpen(TaskExecutionContext,TeamSupportCapability,long)` | `AutoCombatService`, `AutoBattleTask` |
| 11 | `isLocalSupportMemberSession(TaskExecutionContext)` | `AutoCombatService`, `AutoBattleTask` |
| 12 | `registerLocalTeamSessionCandidate(String,Collection<String>,String)` | no external production caller |
| 13 | `markLocalTeamWindowRoleDetected(TaskExecutionContext,String,String,String)` | no external production caller |
| 14 | `isLocalSupportMemberCandidate(TaskExecutionContext)` | `AutoCombatService` |
| 15 | `isPendingLocalSupportLeaderDetection(TaskExecutionContext)` | `AutoCombatService`, `AutoBattleTask` |
| 16 | `markLocalTeamLeaderDetected(TaskExecutionContext,String,String)` | no external production caller; internal call from API 13 |
| 17 | `isLocalTeamSupportCapabilityOpen(TaskExecutionContext,TeamSupportCapability)` | `AutoCombatService`, `AutoBattleTask` |
| 18 | `completeLocalTeamSessionWindow(String,String,String)` | no external production caller |
| 19 | `runOpportunisticMaintenance(TaskExecutionContext,TaskMaintenanceRequest)` | `AutoBattleTask`, `WubeiTask`, `XiuluoTaskV2` |

The four zero-external-caller lifecycle APIs remain public and unreachable from production outside this service. This
card must preserve their shape and single-scope behavior, but must not invent host/factory/runtime activation or claim
that a test-created caller proves production reachability.

## 3. TURN-34A six-API compatibility gate

The reviewed TURN-34A production directly consumes these six APIs:

1. `isPendingLocalSupportLeaderDetection` (`TaskMaintenanceService.java:419`, AutoCombat call sites around
   `AutoCombatService.java:485,535,654`);
2. `isLocalSupportMemberSession` (`:324`, AutoCombat `:492,523,645`);
3. `isLocalTeamSupportCapabilityOpen` (`:471`, AutoCombat `:493,646`);
4. `awaitLocalTeamSupportCapabilityOpen` (`:288`, AutoCombat `:524`);
5. `isLocalSupportMemberCandidate` (`:407`, AutoCombat `:541`);
6. `awaitTeamFirstAidMaintenanceWindowOpen` (`:250`, AutoCombat `:544`).

Delivery evidence must show:

- no signature, nullability, wait/timeout, interruption, leader-detection, candidate, or capability-open semantic drift;
- the six APIs do not dereference `DialogService` or `SummonSkillService`;
- External D did not edit TURN-34A production/test or add a compatibility wrapper;
- the final TURN-34B test source compiles against the reviewed TURN-34A production calls rather than replacing those
  calls with a copied fake surface.

## 4. Broadcast and Summon order acceptance

The current active source still shows the baseline top-level order at `TaskMaintenanceService.java:581-600`:

```text
checkpoint
  -> optional maintenance broadcast
     -> handled / BROADCAST_FAILED / INTERRUPTED: return immediately
  -> optional one Summon public delegate
  -> NO_ACTION
```

Required evidence in the sole named test:

- broadcast `BUSINESS_OPTION_CLICKED`, `FAILED`, and `INTERRUPTED` each short-circuit with Summon delegate count `0`;
- broadcast no-action permits exactly one Summon call only when `cleanSummonSkill=true`;
- request source and full-maintenance-fallback flag reach the production `DialogHandleRequest` unchanged;
- no third fallback, second observation, background work, or automatic retry is introduced.

The current Summon gate order at `:627-799` must remain equivalent to `696a12b0`:

1. feature enabled;
2. configured interval positive;
3. request-required `FREE` state;
4. due gate;
5. existing unknown-failure interval;
6. existing 2-hour tail-safe and skill-count cache rules;
7. team round / required local capability / pathing-window gate;
8. duplicate and max-claim gate;
9. checkpoint before action;
10. save prior `GameContext.ActionState`, set `INTERACTING`;
11. build one cleanup request and call TURN-33 public delegate exactly once;
12. project result/exception to existing cache, cooldown, claim, and previous action state.

The named test must prove every zero-delegate gate in this order, then prove the eligible path invokes exactly one typed
TURN-33 delegate. It must not reproduce TURN-33 static-tail scan, five-delete loop, PNG/OCR/template/action/UUID mechanics.

## 5. Team capability and coordination evidence

Current production lines `111-213` retain the frozen capability sets:

- pathing open: exactly `FIRST_AID`, `PATHING_WINDOW`, `COMMON_BOX`, `SUMMON_SKILL`, `LEFT_TOP_STATUS`;
- weak first-aid open: exactly `FIRST_AID`;
- team close: exactly the same five pathing capabilities;
- return-support open/close: exactly `TEAM_RETURN + COMMON_BOX`.

The test must additionally lock one-per-round duplicate/max claim, known failure claim release, state-change claim retain,
capability epoch, leader conflict/absent detection, and all-candidate completion. TURN-34B only maintains capabilities;
it must not consume CommonBox or execute TeamReturn input.

## 6. Scope isolation and metadata fence

Frozen acceptance requires all existing context-bearing singleton state to be isolated by the existing
`tenantId/userId/deviceId/windowId` authority while retaining the legacy/null fallback. No owner, session, lease, ledger,
new TTL, compaction, or durable workflow may be added.

Positive evidence already visible in the active production bytes, but not yet accepted:

- `effectiveContext(...)` at `:993-998` makes a supplied context win over holder state;
- `currentWindowKey(...)` at `:1006-1014` prefixes the window key with tenant/user/device when turn authority exists;
- `summonSkillState(...)` at `:1033-1054` atomically replaces cached state on identity-token drift;
- `currentIdentityToken(...)` at `:1068-1115` avoids calling legacy-only identity epoch on the ordinary turn-native path.

Delivery/test evidence still required:

- same `windowId/task/round` under different tenant, user, or device must not share cooldown, unknown interval, summon
  cache, active formal round, pathing/first-aid state, or round claim;
- same-scope continuity remains intact;
- `A -> B -> A` native title/HWND/process drift does not revive stale A cache or claim state;
- explicit supplied context wins over an intentionally wrong holder context;
- legacy and null-context keys retain the pre-card behavior.

Static risk index for parent review after delivery, not a finding:

- `scopePrefix(...)` is currently visible in `currentWindowKey`, while `normalizeTeamKey(...)`/`teamRoundKey(...)` at
  `:1158-1196` still visibly derive formal round keys from the task key and round. The final test must prove that the
  active production bytes isolate formal round/window/claim state too; if they do not, the repair remains inside the
  one allowed production file.
- `scopePrefix`, `nativeHandleOrNull`, `nativeProcessIdOrNull`, and `identityTail` currently contain unavailable/fallback
  branches. The fixed card requires turn-native missing metadata or device/window/title/HWND/process drift to stop before
  Dialog/Summon delegate, action, or UUID. The final test must exercise those missing/mismatch paths, not merely inspect
  the generated identity string.

## 7. Terminal, uncertainty, STOP, and result projection

TURN-33 already owns exact action correlation and throws typed task exceptions for fatal command/correlation,
`DUPLICATE_OR_UNCERTAIN`, unconfirmed `STOPPED`, metadata drift, and transport uncertainty. TURN-34B must not catch and
convert those into `false`, `SUMMON_SKILL_FAILED_RETRY_LATER`, or success.

Required production-path assertions:

- initial checkpoint STOP reaches neither Dialog nor Summon;
- confirmed STOP from the delegate propagates as STOP, with no second delegate/action/UUID;
- fatal/uncertain/correlation defects propagate unchanged and never refresh success cooldown;
- a known mechanical failed result may use the existing failed/retry-later projection, but invokes no automatic retry;
- prior `GameContext.ActionState` is restored for success, known failure, fatal uncertainty, and STOP;
- success updates existing cooldown/cache exactly once;
- known failure with no delete/ultimate state change releases the round claim;
- delete/ultimate state change retains the claim;
- the existing configured unknown-failure interval remains business state, not transport retry or a newly invented TTL.

## 8. Named-test source and compile-risk preflight

Current fact: `TaskMaintenanceTurnContractTest.java` is missing, so none of the frozen test-source evidence exists yet.
The final file must directly instantiate production `TaskMaintenanceService`; no Spring/HTTP/runtime/Task/UI/input/capture
and no production test hook.

Available compileable patterns already present in the repository:

- the Lombok-generated production constructor takes exactly
  `BotProperties, GameContext, DialogService, SummonSkillService, TaskExecutionContextHolder`;
- TURN-34A's test already instantiates it directly at `AutoCombatServiceTurnContractTest.java:554`;
- a scripted `DialogService` subclass can call the existing 14-argument constructor with test nulls and override
  `handleDialog`, as demonstrated by `SummonSkillTurnContractTest.java:1624-1641`;
- a scripted `SummonSkillService` subclass can call its public four-argument constructor with non-null test-private
  holder/assets/dialog/cleaner collaborators and override only `cleanSummonSkillsOnce`;
- real turn-native context construction and `TaskExecutionContextHolder.callWith(...)` are already demonstrated by
  `AutoCombatServiceTurnContractTest.java:521-575`.

Compile/test risks the delivery must close:

1. The new test path is under Cloud `src/test/`, which is ignored by the current Cloud `.gitignore`; physical existence,
   SHA, retention, and explicit delivery evidence must be checked directly rather than inferred from `git status`.
2. Null Dialog/Summon collaborators are valid only for the six non-delegating 34A APIs. Broadcast/Summon tests must use
   actual scripted non-null collaborators or they will prove only NPE behavior.
3. Scripted turn-native metadata must match invocation device/window and have a positive window rectangle; invalid context
   construction cannot serve as a metadata-fence test.
4. Tests must use production public APIs and typed scripted result/exception seams. Reflection may lock API shape, but it
   cannot replace priority, scope, metadata, terminal, or projection behavior tests.
5. The Cloud repository has known shared main-compile debt outside this card. A future Maven failure before `testCompile`
   must be recorded as a shared cohort blocker with `Tests run=0`; it must not be misreported as this named test failing.
6. No Maven command is allowed while External C/D or any other Java writer is active. The later parent-authorized command
   remains `mvn -q -Dtest=TaskMaintenanceTurnContractTest test`, followed by the applicable Cloud compile/build gate.

## 9. Delivery evidence checklist for the parent

External D's one true-EOF delivery should include final SHA/line evidence for exactly:

1. `TaskMaintenanceService.java`;
2. `TaskMaintenanceTurnContractTest.java`;
3. this card's append-only delivery record.

Parent source/test-source review should then check, in order:

- final bytes did not modify any caller, model, context/protocol, POM, TURN-33/34A/22 file, DHXY file, or third test;
- all 19 public shapes and the six TURN-34A APIs remain exact;
- baseline broadcast-before-Summon order and every Summon zero-delegate gate are production-path tested;
- one eligible due maintenance calls one typed TURN-33 delegate, with zero duplicated TURN-33/TURN-22 mechanics;
- five/one/five/two capability sets and formal/local coordination remain exact;
- tenant/user/device/window and native-fingerprint isolation covers every context-bearing map, not only per-window cache;
- missing/drift metadata, supplied-context precedence, STOP, fatal/uncertain, known failure, and state restoration are tested;
- there is no automatic retry, second command, new owner/session/ledger/TTL/background queue/durable workflow;
- only after parent source/test-source pass should two independent reviewers and the stable-writer test/compile gates run.

Business conclusion for this preflight only:

`无已批准业务差异；按 696a12b0、docs/业务逻辑.md 与最小 HTTPS turn 合同等价迁移。`

PRECHECK_COMPLETE

<!-- TRUE_EOF: CR271 TURN-34B ACTIVE-DELIVERY-PREFLIGHT HELPER-R2 PRECHECK_COMPLETE NON-REVIEWER NON-OWNER NON-PARENT-APPROVAL SNAPSHOT=2026-07-16T08:36:24.1967696-04:00 PROD_SHA256=963B028C4A753EFCC0263E402D6ABA310E51C2591ACA5E9717AFE92912A66BBC NAMED_TEST=MISSING -->
