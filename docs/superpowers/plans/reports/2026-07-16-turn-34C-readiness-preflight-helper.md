# TURN-34C AutoBattleTask HTTPS Turn Readiness Preflight

## REPLACEMENT CLAIMED

- claimedAt: `2026-07-16T03:29:27-04:00`
- helperIdentity: `Codex current-thread TURN-34C non-binding readiness helper`
- roleBoundary: `PRECHECK evidence only; not reviewer; no APPROVED/BLOCKED authority`
- previousSession: `not_found; no prior conclusion assumed`
- uniqueWriteSet:
  - `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-34C-readiness-preflight-helper.md`
- allOtherPaths: `read-only`
- status: `REPLACEMENT CLAIMED / PRECHECK IN PROGRESS`

## Identity Correction

- correctedAt: `2026-07-16T03:31:00-04:00`
- platformAgentId: `019f69d1-c21e-7862-afbe-bd0d971b200a`
- nickname: `Franklin`
- correctionScope: `identity metadata only; unique write set and non-binding helper boundary unchanged`

## 1. Precheck Scope And Evidence

- completedAt: `2026-07-16T03:39:59.7714788-04:00`
- businessBaseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- planAuthority: `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md` sections 14-19
- protocolAuthority: `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`
- businessAuthority: `docs/业务逻辑.md`
- migrationEvidence: `docs/superpowers/specs/2026-07-12-service-migration-matrix.md`
- currentCloudSource: `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/AutoBattleTask.java`
- baselineBlob: `18bcd33322c5b1037087f40ba136b1bc9146dda9`
- currentCloudSourceSha256: `E13BFFF740570B9C7B833F7EDCE336BFFE39FB89E410B630FF2156B69410264A`
- currentCloudSourceLines: `294`
- repositorySnapshot:
  - DHXY branch `thin-client-design`; `80` dirty/untracked entries (`39` modified, `1` deleted, `40` untracked).
  - Cloud branch `navigation-migration`; `28` dirty/untracked entries (`9` modified, `19` untracked).
  - Cloud `AutoBattleTask.java` is currently untracked (`??`); it is existing shared work and must be incrementally preserved.
  - Proposed named test does not yet exist.

Read-only evidence included the complete current and baseline `AutoBattleTask`, all directly invoked Cloud services, both repository
statuses, the latest fixed-report true EOF for TURN-19/21/22/23/28P/33, TURN-34A/34B readiness reports, and the local baseline
factory/runner chain. No Java, configuration, plan, ACTIVE_WORK, CR, matrix, dashboard, test, or report other than this unique file was
modified. No Maven, JUnit, compile, runtime, application, server, Task, poller, UI, capture, or input command was run.

## 2. Authoritative Card Boundary

The audited execution registry fixes TURN-34C as:

- start dependencies: `TURN-19 + TURN-21 + TURN-22 + TURN-23 + TURN-34A + TURN-34B`;
- production ownership: one exclusive Cloud file, `AutoBattleTask.java`;
- named test: `task/AutoBattleTaskTurnContractTest` with profile `TASK` plus the default `BC4+BASE` gate;
- required behavior: exact startup first-aid, maintenance, team-return, left-top, and common-box order;
- wave order: TURN-34A and TURN-34B may run in parallel, while TURN-34C waits for both final public contracts.

The parent plan still assigns real Cloud task factory/runtime activation to TURN-40B. TURN-34C is the Cloud task orchestration cutover,
not the host, queue, lifecycle ingress, or UI activation card.

## 3. Dependency Gate Snapshot

| Dependency | Latest on-disk evidence | TURN-34C consequence |
|---|---|---|
| TURN-19 | Parent source/test-source review `P0/P1/P2=0/0/0`; build cohort pending. | Final left-top typed contract is source-stable, but its named-test/build evidence remains part of the composed completion gate. |
| TURN-21 | Parent Repair #1 review `P0/P1/P2=0/0/0`; build cohort pending. | Final CommonBox typed contract is source-stable and keeps exact identity plus `MOVE/WAIT80/CLICK/WAIT120`. |
| TURN-22 | Parent review `P0/P1/P2=0/1/0`; queue-owned click delay/hold prerequisite not yet landed. | TURN-34C must not bind to or normalize the current interim TeamReturn click semantics. |
| TURN-28P | Replacement implementation is active under Locke; it supplies the generic queue-owned click timing needed by TURN-22. | Transitive gate only; TURN-34C must not touch protocol/executor mechanics. |
| TURN-23 | Parent Repair #1 review `P0/P1/P2=0/0/0`; build cohort pending. | Player startup first-aid/incense typed behavior is source-stable. |
| TURN-33 | Replacement implementation is active under Faraday. | TURN-34A/34B cannot consume an interim Summon surface. |
| TURN-34A | Readiness evidence exists, but no implementation card delivery exists. It consumes 19/20/21/23/24A/33. | `AutoCombatService` final public behavior and state ownership must be reread before TURN-34C is dispatched. |
| TURN-34B | Readiness evidence exists, but no implementation card delivery exists. It consumes 21/22/23/26/33. | `TaskMaintenanceService` final result/terminal/team-capability behavior must be reread before TURN-34C is dispatched. |

The direct start dependency set is therefore not yet source-stable as a whole. This report completes only the readiness analysis; it is
not evidence that the TURN-34C implementation may start now.

## 4. Baseline Equivalence Findings

The current Cloud task already preserves the core `696a12b0` source order. Its only material adaptation is Cloud context binding:

1. `AutoBattleTask.java:112-113` rejects a missing explicit context and binds the exact context through
   `TaskExecutionContextHolder.callWith(...)` for the whole patrol lifecycle.
2. `:122-137` keeps startup order exactly:
   `checkAutoBattle -> botStatus RUNNING -> performStartupFirstAidCheck -> initializeForTaskStart -> initializeForCurrentWindow`.
3. `:139-150` keeps the loop decision exactly: stop checkpoint, one AutoCombat tick, every non-`NONE` result sleeps/continues,
   only `NONE + FREE` enters idle maintenance, then one poll sleep.
4. `:182-232` keeps idle order exactly:
   local team-return release -> pending local-leader defer -> standalone return-team -> optional left-top -> opportunistic maintenance.
5. `:235-255` keeps local TEAM_RETURN release exactly: require local member session and open TEAM_RETURN capability; when COMMON_BOX
   is open, consume pending box before calling return-team; either successful action short-circuits the outer idle pass.
6. `:263-271` keeps follower support true only for role `MEMBER` with non-null requested task different from `auto_battle`.
7. `:280-287` keeps polling priority `pending first-aid=500ms`, otherwise `FREE=3000ms`, otherwise AutoCombat dynamic interval.
8. `:290-292` keeps `TaskRetryPolicy.none()`.
9. `:173-176` keeps stop behavior `botStatus=IDLE` and `actionState=FREE`.

No direct `TurnGameClient`, action factory, UUID supplier, raw-PNG decoder, OCR, template matcher, local input provider, screenshot
provider, old fact/macro API, or four-local-Service call appears in `AutoBattleTask.java`. The task is already an orchestrator over typed
Cloud services; lower cards own explicit JSON actions and local mechanics.

### Baseline conflict that must remain excluded

The migration matrix's older method inventory mentions `tryRunLocalTeamReturnSelfCheck`/CR244 and an expanded self-check memory path.
That method is absent from both the fixed `696a12b0` source and current Cloud source. The authoritative baseline method is
`tryRunLocalTeamReturnRelease`. TURN-34C must not import the later self-check, marker-memory, extra probe, retry, or fallback merely
because the historical matrix still names it.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## 5. Real Caller And Reachability Evidence

### Existing local baseline caller

The runnable local chain is concrete:

```text
WindowTaskRunner
  -> DefaultTaskFactory.createTask(windowContext, TaskType.AUTO_BATTLE)
  -> ObjectProvider<AutoBattleTask>.getObject()
  -> GameTask.execute(executionContext)
  -> AutoBattleTask.execute(executionContext)
```

Evidence: local `DefaultTaskFactory.java:35-46` routes `AUTO_BATTLE` to the prototype provider; local
`WindowTaskRunner.java:608` creates the task and `:766` invokes `task.execute(executionContext)`.

### Current Cloud reachability

Cloud source search finds no production reference to `AutoBattleTask` outside the class itself. Cloud currently has `TaskType.AUTO_BATTLE`
and protocol task-code types, but no production `DefaultTaskFactory`, task registry, or queue runtime creating this class. References outside
the class are contract-test source scans only. The parent plan explicitly leaves the real Cloud factory/runtime to TURN-40B.

Therefore TURN-34C may prove a real production task class and its internal typed-service caller chain, but it must not claim runtime
activation. It must not add a factory, host, route, lifecycle listener, startup auto-run, Spring application wiring, or UI command. The current
package-private `CloudStartupGateAuthority`/`TaskStartupCheckService` assembly is likewise a later startup/activation concern; TURN-34C
must not widen its one-file production set to manufacture a singleton startup gate.

## 6. Exact Mutex Write Set Recommendation

After all direct start dependencies are source-stable, the implementation card should own exactly:

### Production

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/AutoBattleTask.java`

### Named test

1. `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/AutoBattleTaskTurnContractTest.java`

### Fixed implementation report

1. `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34C.md`

The fixed implementation report does not exist at this precheck timestamp and must be created/frozen by the parent before dispatch. Every
other production/test/report path is read-only. In particular, TURN-34C must not edit AutoCombatService, TaskMaintenanceService,
PlayerStateService, TeamReturnService, CommonBoxService, LeftTopStatusSwitchService, startup authority, BaseTaskTemplate, context, protocol,
TurnGameClient, action factory, host, task factory, POM/config/resources, DHXY, or dependency tests.

This write set is file-level mutually exclusive with TURN-34A, TURN-34B, TURN-28P, and TURN-33. Its logical dependency on 34A/34B still
requires those writers and parent source reviews to finish first. The worker must preserve the current untracked Cloud file byte history and
edit it incrementally; replacement or checkout from the baseline would overwrite shared work.

## 7. Frozen Cloud Decision And Local Mechanics Boundary

TURN-34C should own only Cloud orchestration decisions:

- startup admission result consumption;
- startup first-aid/maintenance/combat initialization order;
- combat tick result branch;
- whether idle maintenance is eligible;
- local-team capability order and follower-support classification;
- exact maintenance request fields;
- polling interval selection;
- stop/retry behavior.

It must delegate mechanics through final typed services:

- PlayerState for startup first-aid/incense;
- AutoCombat for radar/combat recovery and downstream UI decisions;
- TeamReturn for observation/incense/click;
- CommonBox for pending consume;
- LeftTopStatusSwitch for observation/click;
- TaskMaintenance for dialog/Summon coordination.

The thin client receives only the explicit JSON action chosen downstream and executes the stated capture/input/wait/local-service mechanics.
Raw PNG returns to Cloud when requested. Any local template match is only an explicit protocol option, never an implicit AutoBattle decision.
TURN-34C must not add local OCR, local task phase decisions, a direct screenshot, a direct input call, a direct permanent-local-Service call,
automatic transport retry, owner, permit, session, ledger, TTL, compaction, or durable workflow.

## 8. Required Call-Order Acceptance Matrix

| Case | Required production behavior | Forbidden drift |
|---|---|---|
| Missing context | `execute()` and `execute(null)` fail before startup collaborator, action, or UUID; no default/epoch-zero context is minted. | Thread-local/default window fallback or success result. |
| Exact context lifetime | The supplied context is the holder current context for the entire startup/loop/delegate call and is cleared after return/throw. | Replacing taskRun/runRevision/window identity or leaking holder state. |
| Startup denied | Return the exact `TaskStartupCheckResult.blockedResult`; startup first-aid, maintenance init, AutoCombat init, and loop calls are all zero. | Converting skip/stop/failure into success or running startup side effects first. |
| Startup allowed | Exact order: gate, RUNNING state, startup first-aid, maintenance init with `auto-battle`, AutoCombat init, first loop checkpoint/tick. | Reordering first-aid after maintenance/combat or adding a second startup probe. |
| Tick `IN_COMBAT` | No idle maintenance; one poll sleep then next tick. | Calling TeamReturn/CommonBox/left-top/opportunistic maintenance in the same iteration. |
| Tick `EXIT_RECOVERED` | Same task-level branch as every non-`NONE`: no idle pass, one poll sleep. | Treating it as `NONE`, success, or a new task phase. |
| Tick `NONE`, state non-FREE | Skip idle maintenance and sleep once. | Forcing maintenance while another action owns the window. |
| Tick `NONE`, state FREE | Run the exact idle chain once, then sleep once unless a stop exception exits. | Looping/retrying an idle collaborator inside the same tick. |
| Local member, TEAM_RETURN closed | Zero CommonBox and TeamReturn release calls; continue to the later idle gates. | Waiting, opening capability locally, or inventing a fallback marker probe. |
| Local member, TEAM_RETURN open | Check COMMON_BOX capability; if open, consume pending box first; call return-team second; `box || return` short-circuits later maintenance. | Return-team before CommonBox, skipping return after box, or adding CR244 self-check memory. |
| Pending local leader detection | After the release attempt fails, defer the rest of idle maintenance. | Standalone return-team, left-top, or Summon while leader identity is pending. |
| Standalone return-team | Only non-local-support sessions call standalone `clickReturnTeamIfPresent`; a true result short-circuits. | Treating a local member as standalone or issuing an extra click. |
| Follower mode | True only for `MEMBER` and requested task non-null/different from `auto_battle`. | Inferring from task name, session presence, or UNKNOWN role. |
| Local follower left-top | Call once only when local support + follower mode + LEFT_TOP_STATUS capability open; checkpoint immediately afterward. | Clicking for closed capability, unsupported requested task, or retrying a terminal result. |
| Maintenance request | Exact fields: source `auto-battle`; broadcast=true; full fallback=false; cleanSummon=true; one-per-round for local/legacy follower gate; legacy key only for non-local supported requested task; local required capability only `SUMMON_SKILL`. | New flags, new business fallback, changed capability, or direct Summon call. |
| Poll interval | Pending follower first-aid wins at `500ms`; otherwise FREE is `3000ms`; otherwise use final AutoCombat dynamic interval. | Combining/summing intervals or adding a retry backoff. |
| Stop | `stop()` writes IDLE+FREE; context stop at a checkpoint propagates without another collaborator/action. | Catching stop as normal failure/success or performing cleanup actions not in baseline. |
| Retry | `getRetryPolicy` is always `none`. | Context retry policy, automatic HTTP retry, or task-level replay. |

## 9. JSON, Raw PNG, UUID, And Terminal Acceptance Matrix

TURN-34C is the orchestrator, not an action encoder. Its named test should prove composition and zero direct action authority; each dependency
named test remains responsible for its exact JSON/frame bytes.

| Task branch | TURN-34C assertion | Composed dependency gate | Terminal projection at task layer |
|---|---|---|---|
| Startup role gate | Zero JSON action, PNG, UUID, or local mechanics. | Startup gate/context tests. | Exact blocked result returns; stop/context mismatch propagates. |
| Startup first-aid | Exactly one `performStartupFirstAidCheck(context)` call, no task-built action. | TURN-23 `PlayerStateTurnContractTest`: raw PNG capture, exact identity, explicit Bag local action where required, one UUID per action, zero transport retry. | Typed fatal/stop/uncertain must propagate; later startup calls remain zero. |
| AutoCombat tick | Exactly one `handleCombatTick(context,"auto-battle",false)` per iteration. | TURN-34A plus TURN-19/20/21/23/24A named tests: Cloud OCR/business and closed typed actions. | Preserve `NONE/IN_COMBAT/EXIT_RECOVERED`; exceptions/stop are not booleanized or retried. |
| CommonBox consume | At most one service call in the release branch, before TeamReturn. | TURN-21: exact-window observation, raw PNG where applicable, one explicit click command/UUID and strict correlation. | false continues to TeamReturn; true participates in short-circuit; uncertain/fatal propagates. |
| TeamReturn | At most one task-level service call per applicable branch. | TURN-22 after TURN-28P: observation PNG and explicit click JSON, each explicit action with its own UUID, same queue `150ms` click delay + `500ms` hold, zero transport retry. | false continues; true short-circuits; unresolved/uncertain is not success and is not reissued. |
| Left-top | At most one service call after exact capability gates. | TURN-19: Cloud image decision and exact single click command/UUID with strict terminal/correlation. | Result is not converted into a new task phase; thrown stop/fatal propagates and no maintenance follows. |
| Opportunistic maintenance | Exactly one final 34B public call with the frozen request. | TURN-34B composed with TURN-26 Dialog and TURN-33 Summon tests; Cloud owns OCR/business, each explicit action closed with a fresh UUID. | Preserve final `TaskMaintenanceResult`; handled only affects logging at this task layer; thrown terminal uncertainty is not retried. |

The named test must also enforce a scoped source boundary for `AutoBattleTask.java`: zero direct references to `TurnGameClient`,
`CloudTurnActionFactory`, UUID generation, raw image processing, OCR/template matching, input/screenshot providers, old fact/macro APIs, and
the four permanent local services. It must not duplicate dependency image fixtures or reproduce dependency decision mappers.

## 10. Named Test Design Recommendation

`AutoBattleTaskTurnContractTest` should directly exercise the production task with test-private scripted collaborators and a deterministic
sleep seam; it must not start Spring, a host, HTTP, a task runtime, or physical mechanics, and must not add a production helper merely for
testing. Existing package-private startup evaluation may be assembled inside the test by reflection/test-private construction if necessary;
production startup authority must remain outside this card.

At minimum the single named class should cover:

1. explicit-context rejection and holder cleanup on normal return and exception;
2. startup denied and startup allowed exact sequence;
3. startup first-aid failure/stop short-circuit;
4. all three AutoCombat tick results and FREE/non-FREE idle eligibility;
5. local TEAM_RETURN closed/open, COMMON_BOX closed/open, box false/true, return false/true call order;
6. pending leader defer and standalone return path separation;
7. follower-mode truth table for role/requested task;
8. left-top capability truth table and post-left-top checkpoint;
9. exact `TaskMaintenanceRequest` field matrix for standalone, local follower, and legacy non-local follower;
10. maintenance handled/no-action/failure-status behavior without task-level retry;
11. polling priority `500/3000/dynamic` without real sleeping;
12. `stop()` state reset and stop-token propagation;
13. source/API boundary and zero direct action/UUID/raw-image authority;
14. zero activation claim: the test constructs the task directly and does not pretend a Cloud factory/host already exists.

Future authorized command for this exact named family:

```text
mvn -q -Dtest=AutoBattleTaskTurnContractTest test
```

After all Java writers stabilize, the applicable Cloud source gate remains `mvn -q clean compile`; broader package/test execution remains a
parent/user gate. This helper ran neither command.

## 11. Risks And Parent Freeze Points

1. **Dependency-shape risk:** TURN-34A/34B are not implemented. Starting 34C now would force the worker to guess their final signatures,
   state identity, terminal mapping, and Summon/TeamReturn behavior.
2. **TURN-22 timing risk:** current TeamReturn JSON expresses waits that the existing DHXY executor does not yet hold in one input queue
   transaction. TURN-34C must wait for TURN-28P and the TURN-22 repair, not mask the problem with a task delay.
3. **Historical-matrix drift:** CR244 self-check text is later than the fixed 696 baseline. Importing it would add a probe/memory/fallback
   and violate the no-unapproved-business-difference rule.
4. **Activation illusion:** no current Cloud production factory calls AutoBattleTask. A unit test or Spring annotation is not runtime
   reachability; TURN-40B owns that proof.
5. **Startup assembly risk:** `TaskStartupCheckService` is package-private-authority-bound and not a normal singleton bean. TURN-34C must
   not solve that by widening its write set; the startup state/direct-caller and activation cards own it.
6. **Untracked-source risk:** current Cloud AutoBattleTask is untracked shared work. A baseline copy or file replacement would erase the
   exact-context adaptation already present.
7. **Terminal flattening risk:** boolean-return collaborators contain strict terminal behavior internally. Catching all exceptions in
   AutoBattleTask or mapping uncertainty to false would silently create a business fallback.
8. **Test duplication risk:** repeating lower-level PNG/JSON mappers inside the task test can pass while production ports drift. TURN-34C
   should test orchestration and rely on the named dependency tests for action bytes.
9. **Loop test risk:** using real `TaskSleep` would make unit tests slow/flaky. Use only a test-private deterministic sleep seam and a
   scripted one-iteration stop; do not add a production retry/clock abstraction.
10. **Scope expansion risk:** adding a factory, host, lifecycle, protocol, DTO, wrapper, or new service to make this card look runnable
    would overlap TURN-38B3/40B and violate the one-file production boundary.

## 12. Executable Dispatch Brief

The parent can copy the following into the future implementation card after all direct start dependencies have final source gates:

```text
TURN-34C implements the Cloud AutoBattleTask orchestration cutover against baseline
696a12b0ffb8aa21f7d5dee841a65cecd78be9f7. Before CLAIMED, reread the final TURN-19/21/22/23/34A/34B reports and their actual
production/test source; do not bind to interim APIs. Preserve both repositories' dirty/untracked files and incrementally edit the existing
untracked Cloud AutoBattleTask. Exact write set is only Cloud AutoBattleTask.java, Cloud task/AutoBattleTaskTurnContractTest.java, and this
card's fixed report. Keep the supplied TaskExecutionContext bound for the whole lifecycle; preserve exact startup order, non-NONE loop
short-circuit, FREE-only idle maintenance, TEAM_RETURN -> COMMON_BOX-before-return order, pending-leader defer, standalone return path,
follower/left-top/team capability gates, exact TaskMaintenanceRequest fields, 500/3000/dynamic polling, stop state, and retry=none. Do not
restore CR244 self-check. AutoBattleTask must own Cloud orchestration only and must have zero direct TurnGameClient/action/UUID/raw-PNG/OCR/
template/input/screenshot/old-fact-macro/permanent-local-Service authority. Do not add factory/host/runtime/startup authority or modify any
dependency. Add one production-exercising named test covering the frozen acceptance matrix; run only that authorized test and applicable
Cloud compile when writers are stable. Record no approved business difference: equivalent migration only.
```

## PRECHECK_COMPLETE

- result: `TURN-34C readiness evidence, dependency graph, exact mutex write set, acceptance matrix, risks, and dispatch brief complete`
- implementationDispatchNow: `NO; wait for final source gates of TURN-22, TURN-34A, and TURN-34B, including their transitive active work`
- authorityBoundary: `evidence and scheduling input only; parent remains the sole manager/final reviewer`
- filesWrittenByFranklin: `only this fixed readiness report`
- testsOrRuntimeExecuted: `none`

<!-- TRUE_EOF: TURN-34C READINESS PRECHECK_COMPLETE Franklin 019f69d1-c21e-7862-afbe-bd0d971b200a 2026-07-16T03:39:59.7714788-04:00 -->
