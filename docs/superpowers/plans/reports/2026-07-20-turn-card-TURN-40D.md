# TURN-40D - DHXY HTTPS Turn Activation

## Canonical Status

- Status: `READY / ZERO OWNER`
- Dependencies: `TURN-40A + TURN-40C + TURN-13` source gates satisfied.
- Baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.
- Claim rule: only a Worker able to complete this entire source+test card may append a canonical whole-card claim at
  physical EOF. The ledger does not assign this card.
- Business difference: `无已批准业务差异；按基线等价迁移`.

## Frozen Nine-Path Write Set

1. MODIFY `src/main/java/com/bot/dhxy/cloud/turn/WindowTurnLoop.java`
2. MODIFY `src/main/java/com/bot/dhxy/cloud/turn/TurnLoopRegistry.java`
3. MODIFY `src/main/java/com/bot/dhxy/cloud/turn/TurnConfiguration.java`
4. MODIFY `src/main/java/com/bot/dhxy/cloud/turn/TurnModeGuard.java`
5. MODIFY `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java`
6. MODIFY `src/main/java/com/bot/dhxy/window/control/WindowTaskStartRequest.java`
7. MODIFY `src/main/resources/application.properties`
8. CREATE `src/test/java/com/bot/dhxy/window/control/WindowRemoteTurnControlContractTest.java`
9. MODIFY `src/test/java/com/bot/dhxy/cloud/turn/WindowTurnLoopContractTest.java`

Current production SHA/lines at readiness freeze: `WindowTurnLoop=DB106E00/273L`,
`TurnLoopRegistry=37E44C74/81L`, `TurnConfiguration=DFE7E697/54L`, `TurnModeGuard=93FC4748/148L`,
`WindowTaskControlService=9DAAA315/484L`, `WindowTaskStartRequest=A0C38F1B/101L`,
`application.properties=59F90430/238L`. All seven are tracked-clean in the CR worktree; the test path does not exist.

## Frozen Lifecycle Contract

- Exact-window local/remote mutex is owned by the same `TurnModeGuard`; a local submit and remote loop create/start
  cannot win for the same window. Batch local start remains all-or-none at the guard boundary.
- Remote start carries one immutable `TurnTaskStartRequest` with stable nonblank `startRequestId`, ordered supported
  task codes and the existing queue failure policy. The exact request is retained and resent unchanged after uncertain
  transport until a matching `TurnTaskStartAck` is accepted; it never creates a second start intent.
- `SLEEP_COMPUTER` remains rejected. No session, durable ledger, TTL, automatic business retry or second protocol is
  introduced.
- Pause/resume only change the Cloud checkpoint flags carried by live metadata. The DHXY long-wait loop remains alive;
  pause does not park or pause local permanent-service mechanics and resume does not create a new task start.
- Stop publishes one stop-bearing lifecycle turn for the exact window and interrupts local in-flight wait/action as
  needed. It does not wait for or retry business work. Registry removal/unregister is permitted only after the loop is
  stopped; a running loop can never be silently removed.
- Start failure, transport failure and lifecycle cleanup preserve the exact prior acknowledgement/outcome state and
  leave no newly-created stopped registry entry. Failure cleanup cannot stop/remove another loop that won the race.
- All runtime/application activation remains inert until an explicit control call. No UI, startup auto-run, capture or
  physical input is added by this card.

## Test And Build Gate

- Required named family: `WindowRemoteTurnControlContractTest,WindowTurnLoopContractTest`.
- Required cases: exact local/remote mutex; stable ordered start request and matching ack; uncertain resend without
  second start; pause/resume metadata with long-wait alive; single-shot stop before unregister; duplicate/missing/running
  registry rejection; start and transport failure cleanup; no startup auto-run.
- Authorized command: `mvn -q -DskipTests=false -Dtest=WindowRemoteTurnControlContractTest,WindowTurnLoopContractTest test` from
  `D:\mavenProject\DHXY-cr271`.
- Build gate after the named test: `mvn -q -DskipTests compile`. Worker delivery must include exact test counts, exits,
  all eight path SHA/line/mtime values and a collision audit. Do not run runtime/server/Task/UI/capture/input.

<!-- TRUE_EOF: TURN-40D READY ZERO-OWNER FIXED-8-PATH WHOLE-CARD DEPENDENCIES-40A+40C+13-SOURCE-GATES-PASSED PROD-7-TRACKED-CLEAN TEST-1-NEW-NO-COLLISION EXACT-WINDOW-LOCAL-REMOTE-MUTEX STABLE-STARTREQUEST-UNTIL-MATCHING-ACK PAUSE-RESUME-CLOUD-CHECKPOINT-LONGWAIT-ALIVE SINGLE-SHOT-STOP-BEFORE-UNREGISTER FAILURE-CLEANUP NO-STARTUP-AUTORUN BASELINE-696 NO-BUSINESS-DIFF 2026-07-19T20:14:36-04:00 -->

---

## WHOLE-CARD CLAIM (TURN-40D) - 2026-07-19T21:03:00-04:00 - EXTERNAL-A

- owner: EXTERNAL-A (heartbeat `dea947fe`). Canonical whole-card self-claim per the card claim rule (ledger does not assign).
- anti-race evidence: at claim time the card physical EOF is `READY / ZERO OWNER` (TRUE_EOF 2026-07-19T20:14:36) with no prior WHOLE-CARD CLAIM section; this is the physically-earliest claim. If any physically-earlier claim is found on read-back, EXTERNAL-A canonically self-withdraws.
- capacity: `ENOUGH_WHOLE_CARD` (TURN-40C Cloud Activation just PASSED Review #2 / owner released; A idle-available).
- dependencies: 40A + 40C + 13 source gates satisfied (per card + ACTIVE_WORK 20:14).
- write-set acknowledged (exactly 8 DHXY-cr271 paths, no widening): MODIFY `cloud/turn/WindowTurnLoop.java`(DB106E00/273L)/`TurnLoopRegistry.java`(37E44C74/81L)/`TurnConfiguration.java`(DFE7E697/54L)/`TurnModeGuard.java`(93FC4748/148L)/`window/control/WindowTaskControlService.java`(9DAAA315/484L)/`window/control/WindowTaskStartRequest.java`(A0C38F1B/101L)/`resources/application.properties`(59F90430/238L); CREATE `test/.../window/control/WindowRemoteTurnControlContractTest.java` (absent). Readiness-freeze SHAs per card; will verify tracked-clean before writing.
- baseline: `696a12b0`; `无已批准业务差异；按基线等价迁移`. Zero Git mutation.
- next: read-back this EOF to confirm sole earliest owner; then read AGENTS.md/DHXY_CONTEXT/业务逻辑/plan §14-19 + verify 7 prod SHAs before implementing the frozen lifecycle contract (exact-window local/remote mutex, stable start-request resend until matching ack, pause/resume Cloud-checkpoint long-wait-alive, single-shot stop before unregister, failure cleanup, no startup auto-run) + the named contract test.

<!-- TRUE_EOF: TURN-40D WHOLE-CARD CLAIM EXTERNAL-A ANTI-RACE-EOF-READY-ZERO-OWNER PHYSICALLY-EARLIEST SELF-WITHDRAW-IF-EARLIER 8-PATH-7MODIFY-1CREATE DHXY-CR271 DEP-40A+40C+13-SATISFIED BASELINE-696 NO-BUSINESS-DIFF HEARTBEAT-dea947fe 2026-07-19T21:03:00-04:00 -->

## PARENT CLAIM RECOGNITION - 2026-07-19T20:25:07-04:00

- Parent read-back confirms External A is the physically-earliest and sole whole-card owner. This is a canonical
  Worker self-claim, not a parent assignment.
- State is `SOURCE ACTIVE / RECON`; the fixed eight-path write-set and frozen lifecycle contract are unchanged.
  External C observed the claim during anti-race precheck and did not compete. No Java or build change is present yet.

<!-- TRUE_EOF: TURN-40D PARENT-CLAIM-RECOGNITION EXTERNAL-A-SOLE-OWNER SOURCE-ACTIVE-RECON WORKER-SELF-CLAIM-NOT-ASSIGNED C-ANTI-RACE-NO-COMPETE FIXED-8-PATH NO-JAVA-OR-BUILD-CHANGE 2026-07-19T20:25:07-04:00 -->

## PARENT SOURCE OBSERVATION - 2026-07-19T20:40:06-04:00 - FIRST BATCH 2/8

- `WindowTurnLoop=569E9F01`/310L and `TurnLoopRegistry=5315553F`/109L are the first source increments. The other
  five production paths retain readiness hashes and the contract-test path remains absent.
- Preliminary review confirms the existing four-argument registry API remains, while the remote overload attaches one
  immutable request to a newly-created stopped loop before start. Uncertain transport retains the exact request; the
  existing request/response validator enforces matching startRequestId before attachment stops. No factory/second
  protocol/out-of-write-set source is introduced.
- This is WIP source observation, not whole-card delivery or final review. Remaining control/lifecycle/test/build gates
  are unchanged. Parent ran no Maven during active Java writing.

<!-- TRUE_EOF: TURN-40D PARENT-SOURCE-OBSERVATION FIRST-BATCH-2OF8 LOOP=569E9F01-310L REGISTRY=5315553F-109L SET-BEFORE-START IMMUTABLE-RESEND MATCHING-ACK OLD-CREATE-PRESERVED NO-FACTORY-OR-SECOND-PROTOCOL REMAINING-6-PENDING NOT-DELIVERY PARENT-NO-MAVEN 2026-07-19T20:40:06-04:00 -->

## PARENT SOURCE OBSERVATION UPDATE - 2026-07-19T20:40:40-04:00 - FIRST BATCH 3/8

- `TurnModeGuard=5DBB924D`/170L landed after the 2/8 snapshot. Its new overload preserves the existing entry point and
  reuses the same exact-window mode mutex, runner gate and start-failure cleanup while threading the immutable request.
- Current physical source is 3/8; the remaining four production paths keep readiness hashes and the new test is absent.
  This remains WIP, not canonical delivery/final review. Parent ran no Maven during active writing.

<!-- TRUE_EOF: TURN-40D PARENT-SOURCE-OBSERVATION-UPDATE FIRST-BATCH-3OF8 GUARD=5DBB924D-170L SAME-MUTEX-RUNNER-GATE-FAILURE-CLEANUP OLD-ENTRY-PRESERVED REMAINING-5-PENDING NOT-DELIVERY JAVA-WRITER-ACTIVE PARENT-NO-MAVEN 2026-07-19T20:40:40-04:00 -->

## PARENT PLAN-CONTRACT REPAIR - 2026-07-19T20:55:06-04:00

- Status: `PLAN-CONTRACT REPAIR / EXTERNAL A SOLE OWNER / 3 OF 8 WIP FROZEN`.
- Parent transitive audit checked baseline `696a12b0` `WindowTaskControlService.startSameQueue`, current
  `WindowTaskStartRequest` modes, `WindowTaskRunner.buildExecutionContext`, validator six-fact gate and current
  `WindowTurnLoop.stop/runLoop`. The proposed taskless defaults are not accepted: actual `windowRole`, leader,
  support and local-team facts cannot be replaced by `false/null/NORMAL` merely to satisfy validation.
- Fixed eight-path repair contract:
  1. Remote `SAME_TASK` resolves ordered queue/failure policy and per-window role/team authority using the same
     batch conditions as the 696 local path. Existing local-team authority is baseline state, not a new turn session.
  2. Remote `SELECTED_TASK` resolves each exact registered window's selected task and rejects missing/unsupported
     selections. Deprecated `DETECTED_ROLE` gains no new remote behavior in this card. `startupMode=NORMAL` is valid
     only as the truthful initial explicit-start fact; `UNKNOWN` role is valid only when the registered role is unknown.
  3. Pause/stop flags belong to the live `WindowTurnLoop` lifecycle owner. Do not create a control-side
     `Map<window,supplier>` or another registry/store. Resume must not mint a new start request.
  4. Remote stop must interrupt an in-flight wait/action as needed, then publish exactly one final turn whose live
     metadata has `stopRequested=true`; only after that exchange may the loop become stopped and be removed. The
     current immediate `stopRequested + interrupt + while exit` shape does not satisfy this gate.
- No business-semantic user choice remains: this is a baseline-equivalent contract repair inside the existing eight
  paths. Preserve the three landed files as WIP, stop further Java/Maven until the directed parent message is ACKed,
  then repair/re-deliver the whole card with the named test proving authority positive/negative cases and final stop.

<!-- TRUE_EOF: TURN-40D PARENT-PLAN-CONTRACT-REPAIR OWNER-A 3OF8-WIP-FROZEN REJECT-HARDCODED-6-AUTHORITY REJECT-SECOND-LIFECYCLE-MAP SAME-TASK-696-TEAM-FACTS SELECTED-EXACT-WINDOW DETECTED-ROLE-NO-REMOTE LOOP-OWNS-CHECKPOINT EXACTLY-ONE-STOP-BEARING-TURN-BEFORE-UNREGISTER FIXED-8-PATH NO-USER-SEMANTIC-CHOICE NO-MAVEN 2026-07-19T20:55:06-04:00 -->

## PARENT SOURCE-RACE OBSERVATION - 2026-07-19T21:00:06-04:00

- `TurnModeGuard` moved from `5DBB924D`/170L to `4AEF9A83`/198L at filesystem mtime
  `2026-07-19T20:55:07.693-04:00`, about one second after the repair message. This is accepted as a pre-message race,
  not post-ACK continuation; ownership and communication remain pending the first ACK round.
- The added `stopRemote` is protected WIP only. Its `loop.stop() -> awaitStopped -> registry.remove` path uses the
  current immediate loop stop, so it does not yet publish the required final `stopRequested=true` turn. Distinct path
  completion remains 3/8 and the entire WIP stays frozen under the preceding repair contract. No Maven was run.

<!-- TRUE_EOF: TURN-40D PARENT-SOURCE-RACE-OBSERVATION GUARD=4AEF9A83-198L MTIME=205507 PRE-MESSAGE-RACE ACCEPTED-AS-WIP NOT-POST-ACK-CONTINUATION STILL-3OF8 FINAL-STOP-TURN-MISSING FROZEN ACK-FIRST-ROUND-PENDING NO-MAVEN 2026-07-19T21:00:06-04:00 -->

## PARENT REPAIR ACK RECOGNITION - 2026-07-19T21:00:30-04:00

- External A's 21:43 skewed STATUS EVENT explicitly ACKs
  `PARENT-A-TURN40D-PLAN-CONTRACT-REPAIR-R1-20260719-2055` and withdraws the rejected hardcoded-authority,
  control-map and immediate-stop designs. Communication is healthy; no stale applies.
- Status is now `REPAIR ACTIVE / EXTERNAL A SOLE OWNER / 3 OF 8`. The repaired fixed-eight-path contract remains
  authoritative. Parent ran no Maven because Java repair is active.

<!-- TRUE_EOF: TURN-40D PARENT-REPAIR-ACK-RECOGNITION ACK=PARENT-A-TURN40D-PLAN-CONTRACT-REPAIR-R1-20260719-2055 COMMUNICATION-HEALTHY REPAIR-ACTIVE OWNER-A STILL-3OF8 FIXED-8-PATH NO-MAVEN 2026-07-19T21:00:30-04:00 -->

## PARENT WIP SOURCE OBSERVATION / STOP ACTION GATE - 2026-07-19T21:11:00-04:00

- `WindowTurnLoop=0DBB726B`/378L, mtime `21:10:43.176`; loop-owned pause/stop flags and the final stop-bearing
  exchange are now present. Distinct paths remain 3/8; this is WIP, not delivery/review.
- P1 WIP acceptance point: the final path clears interrupt and invokes `exchangeOnce()`, whose post-response guard
  checks hard stop/thread interrupt but not the live stop checkpoint. An `ACTION` response can therefore reach
  `actionExecutor.execute(...)` during stop. The final lifecycle turn must execute zero newly returned actions, then
  stop and unregister. The named test must prove single send, zero action execution and removal ordering.
- Directed message `PARENT-A-TURN40D-WIP-STOP-ACTION-GATE-20260719-2111` awaits ACK. A retains sole ownership and the
  fixed eight-path repair; no Maven/runtime was run by parent.

<!-- TRUE_EOF: TURN-40D PARENT-WIP-SOURCE-OBSERVATION STOP-ACTION-GATE LOOP=0DBB726B-378L STILL-3OF8 P1-WIP FINAL-STOP-SINGLE-SEND-ZERO-RETURNED-ACTION-EXECUTION-THEN-UNREGISTER MSG=PARENT-A-TURN40D-WIP-STOP-ACTION-GATE-20260719-2111 ACK-PENDING OWNER-A FIXED-8-PATH NO-MAVEN 2026-07-19T21:11:00-04:00 -->

## PARENT SOURCE-RACE UPDATE - 2026-07-19T21:13:00-04:00

- During the parent snapshot, loop advanced to `868E4BC5`/412L and guard to `E9FD87AE`/200L. `requestStop` and
  checkpoint/supplier flag union are now landed; distinct path count remains 3/8.
- The 21:11 stop-action acceptance point remains open on these bytes because final response dispatch still omits
  `stopCheckpoint`. Message `PARENT-A-TURN40D-WIP-STOP-ACTION-GATE-20260719-2111` remains the single pending ACK;
  no duplicate message or Maven/runtime run.

<!-- TRUE_EOF: TURN-40D PARENT-SOURCE-RACE-UPDATE LOOP=868E4BC5-412L GUARD=E9FD87AE-200L STILL-3OF8 STOP-ACTION-GATE-OPEN MSG-2111-ACK-PENDING NO-DUPLICATE-MESSAGE NO-MAVEN 2026-07-19T21:13:00-04:00 -->

## PARENT SOURCE OBSERVATION - 2026-07-19T21:16:00-04:00 - 4 OF 8

- `WindowTaskControlService=6E41D1ED`/489L has begun as import-stage WIP; `TurnModeGuard=44770301`/233L now includes
  live-loop pause/resume routing. Distinct source progress is 4/8.
- A is actively writing; this is not delivery/review. Stop-action message `...2111` remains first-round ACK pending;
  no duplicate message or Maven/runtime run.

<!-- TRUE_EOF: TURN-40D PARENT-SOURCE-OBSERVATION CONTROL=6E41D1ED-489L IMPORT-WIP GUARD=44770301-233L DISTINCT-4OF8 JAVA-WRITER-ACTIVE STOP-ACTION-MSG-2111-FIRST-ACK-ROUND-PENDING NO-MAVEN 2026-07-19T21:16:00-04:00 -->

## PARENT SOURCE-RACE UPDATE - 2026-07-19T21:17:00-04:00

- Control advanced during snapshot to `B7BE569E`/497L. The card remains 4/8 active WIP; all gates are unchanged.

<!-- TRUE_EOF: TURN-40D PARENT-SOURCE-RACE-UPDATE CONTROL=B7BE569E-497L DISTINCT-4OF8 GATES-UNCHANGED 2026-07-19T21:17:00-04:00 -->

## PARENT COMMUNICATION STALE - 2026-07-19T21:21:00-04:00

- External A's next two physical STATUS EVENTs after message `...2111` both used `ack_parent_message=NONE`; status is
  now `COMMUNICATION_STALE`. Source remains active at control=`898C2806`/498L, so no `ACTIVE_STALE` applies.
- A retains sole whole-card ownership and 4/8 WIP. Next event must ACK both `...2111` and stale message
  `PARENT-A-TURN40D-COMMUNICATION-STALE-20260719-2121`; stop-action acceptance remains open. No Maven/runtime.

<!-- TRUE_EOF: TURN-40D PARENT-COMMUNICATION-STALE TWO-NO-ACK-EVENTS=2153+2158 MSG-2111+2121-ACK-REQUIRED SOURCE-ACTIVE CONTROL=898C2806-498L OWNER-A DISTINCT-4OF8 NOT-ACTIVE-STALE STOP-ACTION-GATE-OPEN NO-MAVEN 2026-07-19T21:21:00-04:00 -->

## PARENT SOURCE-RACE UPDATE - 2026-07-19T21:22:00-04:00

- Control advanced to `3E2A0D06`/712L during synchronization. Source remains active, card remains 4/8, and the
  communication-stale/ACK/stop-action gates are unchanged.

<!-- TRUE_EOF: TURN-40D PARENT-SOURCE-RACE-UPDATE CONTROL=3E2A0D06-712L DISTINCT-4OF8 SOURCE-ACTIVE COMMUNICATION-STALE ACK-2111+2121-REQUIRED STOP-ACTION-GATE-OPEN 2026-07-19T21:22:00-04:00 -->

## PARENT STOP-ACTION SOURCE REPAIR OBSERVATION - 2026-07-19T21:27:00-04:00

- Loop=`19B69135`/417L now gates response dispatch on `stopCheckpoint` after validation/correlation and before any
  returned ACTION can reach executor. The source defect from message `...2111` is closed.
- Named single-send/zero-action/then-unregister proof is still absent, and A has not ACKed `...2111+2121`, so
  `COMMUNICATION_STALE` remains. Card stays 4/8 active WIP; no Maven/runtime.

<!-- TRUE_EOF: TURN-40D PARENT-STOP-ACTION-SOURCE-REPAIR LOOP=19B69135-417L SOURCE-DEFECT-CLOSED TEST-PROOF-PENDING COMMUNICATION-STALE ACK-2111+2121-PENDING DISTINCT-4OF8 SOURCE-ACTIVE NO-MAVEN 2026-07-19T21:27:00-04:00 -->

## PARENT PLAN-CONTRACT REPAIR R2 - 2026-07-19T21:41:00-04:00

- Status: `PLAN-CONTRACT REPAIR R2 / 5 OF 9 WIP FROZEN / COMMUNICATION_STALE / EXTERNAL A OWNER RETAINED`.
- The new control-package test (`44AD81C9`/200L) correctly owns mapping/authority tests but cannot directly reuse the
  package-private loop constructor and observable executor harness. A prose-only lifecycle disclaimer does not satisfy
  this card's stop test gate.
- Collision/dependency audit found existing `cloud/turn/WindowTurnLoopContractTest.java` tracked-clean at
  `E91B5E2A...`/273L. It already owns exactly the package-private constructor, scripted long-wait client and
  `TurnContractFixtures.actionHarness`; adding it is the smallest no-production-seam closure.
- Fixed write set is now nine paths: retain all prior eight and add MODIFY
  `src/test/java/com/bot/dhxy/cloud/turn/WindowTurnLoopContractTest.java`. The new control test keeps authority/control
  coverage; the existing loop test must prove interrupt wait -> exactly one stop-bearing exchange -> ACTION response
  causes zero executor/input submissions -> stopped -> unregister ordering.
- Authorized named family is now `WindowRemoteTurnControlContractTest,WindowTurnLoopContractTest`, followed by the
  existing compile gate. Current distinct progress is 5/9. A must ACK `...2111+2121+2141` before further Java/Maven.

<!-- TRUE_EOF: TURN-40D PARENT-PLAN-CONTRACT-REPAIR-R2 EXPAND-8TO9 ADD-WINDOWTURNLOOPCONTRACTTEST=E91B5E2A-273L-CLEAN KEEP-CONTROL-AUTHORITY-TEST=44AD81C9-200L LIFECYCLE-BEHAVIORAL-PROOF NAMED-FAMILY-2CLASSES DISTINCT-5OF9 WIP-FROZEN COMMUNICATION-STALE OWNER-A ACK-2111+2121+2141-REQUIRED NO-MAVEN 2026-07-19T21:41:00-04:00 -->

## EXTERNAL-A CONTRACT QUESTION - 2026-07-19T21:45:00-04:00 - TURN-40D

- author: `EXTERNAL-A` (sole owner). ack_parent_message: `PARENT-A-TURN40D-WIP-STOP-ACTION-GATE-20260719-2111`
  + `PARENT-A-TURN40D-COMMUNICATION-STALE-20260719-2121` (both also ACKed in the 21:45 ledger STATUS EVENT).
- STOP-ACTION GATE: IMPLEMENTED and already parent-confirmed. `WindowTurnLoop=19B69135`/417L now gates response
  dispatch on `stopCheckpoint` AFTER start-ack correlation and BEFORE IDLE/ACTION dispatch, so the single final
  stop-bearing turn (stopCheckpoint set, interrupt cleared) announces `stopRequested=true` and dispatches ZERO returned
  actions (no outcome/png retained). Parent 21:27 recorded "source defect from ...2111 is closed."

- QUESTION 1 — named zero-action proof placement (needs a write-set decision):
  The required proof "final stop sends once, executes zero returned actions, then unregisters" must OBSERVE that
  `LocalTurnActionExecutor.execute(...)` is never called on the stop turn. That observation needs an observable/fake
  executor + the package-private `WindowTurnLoop(...)` ctor + a scripted `TurnClient` + a real `TurnLoopRegistry`
  (for removal ordering) — ALL package-private to `com.bot.dhxy.cloud.turn`. The fixed path-8 test
  (`src/test/java/com/bot/dhxy/window/control/WindowRemoteTurnControlContractTest.java`, package `window.control`)
  cannot reach them: `WindowTaskControlService` can only be built with a real, FINAL `TurnModeGuard` ->
  `TurnLoopRegistry` -> `TurnLoopFactory` -> `LocalTurnActionExecutor` (final; 9 requireNonNull deps; all six
  step-executors TurnCaptureStepExecutor/TurnMatchStepExecutor/TurnInputStepExecutor/LocalServiceStepDispatcher/
  TurnOutcomeAssembler/LocalPathingStartProofMechanics are FINAL with deep hardware-bound graphs), and the observable
  harness `TurnContractFixtures` + the `WindowTurnLoop` ctor are package-private to cloud.turn. A genuine (no-Unsafe,
  no-giant-construction-fiction) executor-observing proof therefore cannot live in window.control. REQUEST one of:
    (a) [RECOMMENDED] widen the fixed write set by ONE test file to add
        `src/test/java/com/bot/dhxy/cloud/turn/WindowTurnLoopStopActionGateTest.java` carrying the named
        single-send / zero-action / then-unregister proof (scripted TurnClient returns ACTION on the stop turn +
        observable executor via the existing TurnContractFixtures harness + real TurnLoopRegistry for removal-ordering).
        window.control's WindowRemoteTurnControlContractTest keeps the authority proofs (below); OR
    (b) permit adding the named proof to the existing out-of-write-set `WindowTurnLoopContractTest` (cloud.turn); OR
    (c) specify an alternative observable you will accept within window.control given the constraints above.

- QUESTION 2 — three frozen paths need NO change under this design (confirm or direct):
    * `TurnConfiguration.java`: WindowTaskControlService is @Service and its new WindowNativeBindingRefreshService dep is
      itself @Service, so Spring auto-wires it; TurnModeGuard/registry/factory ctor signatures are unchanged (only new
      methods added). No bean wiring change needed.
    * `WindowTaskStartRequest.java`: remote entry points take (deviceId, Collection<windowId>, WindowTaskQueue) directly
      and mint an immutable TurnTaskStartRequest internally; the local WindowTaskStartRequest DTO is untouched.
    * `application.properties`: existing cloud.turn.* block already configures the long-wait client/loop; remote
      activation is inert until an explicit control call, so no new property is required.
  If you intend a specific change to any (e.g., route remote start through WindowTaskStartRequest with a deviceId, or a
  cloud.turn.remote toggle), name it and I will implement; otherwise I will deliver them as "no change required under
  this design" with this rationale.

- DELIVERED SO FAR (WIP, not canonical delivery; owner RETAINED; HOLDING for the Q1 write-set decision):
    * WindowTurnLoop=`19B69135`/417L  (stop-action gate + loop-owned pause/stop checkpoint + one-final-stop-turn + attach-until-ack)
    * TurnLoopRegistry=`5315553F`/109L (5-arg create overload attaching the start request pre-start)
    * TurnModeGuard=`44770301`/233L    (startRemote 4-arg + stopRemote/pauseRemote/resumeRemote exact-window lifecycle)
    * WindowTaskControlService=`A6AFC3CB`/717L (startRemoteSameTask/SelectedTask/startOneRemote + pause/resume/stopRemoteWindows
      + toTurnTaskCode(s)/toTurnFailurePolicy + RemoteTurnMetadataSupplier; mapping helpers + supplier package-private for the proof)
    * WindowRemoteTurnControlContractTest=`44AD81C9`/200L (CREATE): 9 tests — supported/unsupported task-code mapping,
      queue order + empty/unsupported rejection, failure-policy mapping, metadata authority projection (real baseline,
      team facts absent, startupMode NORMAL, loop-owned pause/stop, pathing null), context-role verbatim,
      missing/incomplete-binding rejection, exact-context refresh.
- VERIFICATION: main `mvn -o -DskipTests compile` exit0. Named test isolate-run (junit-platform-console-standalone vs
  target/classes + dependency classpath) = [9 tests successful][0 failed]. Aggregate `mvn -Dtest=... test` is BLOCKED
  by pre-existing out-of-card dirty test files (TaskMaintenanceService/SummonSkillService/TeamRoleDetectionService/
  NpcClick.../Dialog... etc. — not in this write set, not this card's defect); Maven test gate marked BLOCKED per the
  accepted isolate-run evidence technique.
- DISCIPLINE: zero Git mutation; write set = the five paths above (+ three no-change paths); DHXY frozen baseline
  untouched; dhxy-cloud-brain untouched; no runtime/server/UI/capture/input.

<!-- TRUE_EOF: TURN-40D EXTERNAL-A CONTRACT-QUESTION ACK-2111+2121 STOP-ACTION-GATE-DONE-19B69135-417L Q1-ZERO-ACTION-PROOF-NEEDS-CLOUD-TURN-TEST-WRITE-SET-WIDEN Q2-3-PATHS-NO-CHANGE(TurnConfiguration/WindowTaskStartRequest/application.properties) TEST-9OF9-ISOLATE-RUN MAVEN-AGGREGATE-BLOCKED-PREEXISTING-DIRTY OWNER-A-RETAINED HOLDING ZERO-GIT 2026-07-19T21:45:00-04:00 -->

## PARENT PHYSICAL-ORDER RECONCILIATION - 2026-07-19T21:45:15-04:00

- The preceding External A event is physically after parent R2. Its explicit ACK of `...2111+2121` clears
  `COMMUNICATION_STALE`; R2 message `PARENT-A-TURN40D-PLAN-CONTRACT-REPAIR-R2-20260719-2141` remains first-round
  ACK pending. A remains sole owner; status is `5 OF 9 WIP FROZEN / COMMUNICATION RECOVERED / R2 ACK PENDING`.
- Q1 is already ruled by R2: modify existing `cloud/turn/WindowTurnLoopContractTest.java` as path 9; do not create a
  second stop-gate test. Q2's no-change rationale is accepted as current WIP evidence, leaving configuration, start
  request and properties at clean bytes; all nine fixed paths remain in final audit scope.
- Reported compile exit 0 and 9/9 isolated control-test result are recorded as WIP evidence only. They do not replace
  the repaired named family `WindowRemoteTurnControlContractTest,WindowTurnLoopContractTest` and subsequent compile
  required after ACK `...2141`. Parent ran no Maven/runtime/input.

<!-- TRUE_EOF: TURN-40D PARENT-PHYSICAL-ORDER-RECONCILIATION COMMUNICATION-RECOVERED ACK-2111+2121-CLOSED R2-MSG-2141-FIRST-ACK-PENDING Q1-ANSWERED-BY-R2-USE-EXISTING-LOOP-TEST Q2-NOCHANGE-WIP-EVIDENCE DISTINCT-5OF9-WIP-FROZEN OWNER-A WIP-COMPILE-EXIT0-CONTROL-ISOLATE-9OF9 R2-NAMED-FAMILY-STILL-REQUIRED NO-PARENT-MAVEN 2026-07-19T21:45:15-04:00 -->

## PARENT R2 ACK / NINTH-PATH SOURCE OBSERVATION - 2026-07-19T21:55:00-04:00

- A's 21:51 physical STATUS EVENT ACKed `...2111+2121+2141`; R2 is accepted, communication is synchronized and the
  WIP freeze is released. A retains sole whole-card ownership.
- `WindowTurnLoopContractTest` advanced from clean `E91B5E2A`/273L to `0085BCB8`/334L. The added WIP test stays in
  the repaired path and exercises interrupted long wait, exactly one stop-bearing exchange, zero executor/input and
  capture work for an ACTION response, stopped loop, then registry removal ordering. Distinct progress is 6/9.
- This is source-active WIP, not canonical delivery or review. Java writer is active; parent ran no Maven/runtime/input.
  The two-class named family and compile remain pending after source stability.

<!-- TRUE_EOF: TURN-40D PARENT-R2-ACK-NINTH-PATH-OBSERVATION ACK-2111+2121+2141-CLOSED COMMUNICATION-SYNCED R2-ACCEPTED FREEZE-RELEASED OWNER-A LOOP-TEST=0085BCB8-334L FINAL-STOP-WIP-PROOF DISTINCT-6OF9 SOURCE-ACTIVE NOT-DELIVERY JAVA-WRITER-ACTIVE NO-PARENT-MAVEN 2026-07-19T21:55:00-04:00 -->

## EXTERNAL-A CANONICAL SOURCE+TEST DELIVERY - 2026-07-19T21:56:00-04:00 - TURN-40D (9-PATH, R2)

- author: `EXTERNAL-A` (sole owner). state: `CANONICAL WHOLE-CARD SOURCE+TEST DELIVERED / AWAITING_PARENT_REVIEW`.
- ack_parent_message: `...2111` + `...2121` + `PARENT-A-TURN40D-PLAN-CONTRACT-REPAIR-R2-20260719-2141` all ACKed
  (21:51). Parent EOF reconciliation 21:45:15 handled (R2 was sole pending ACK; now closed). No new A-directed message.
- baseline attestation: business baseline `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`; `D:\mavenProject\DHXY`(frozen)
  and `dhxy-cloud-brain` untouched by this card; zero Git mutation.

- 9-PATH DISPOSITION (sha256-first-8 / lines / mtime):
  1. MODIFY  `src/main/java/com/bot/dhxy/cloud/turn/WindowTurnLoop.java`            `19B69135`/417L  (stop-action gate on stopCheckpoint + loop-owned pause/stop checkpoint + one-final-stop-turn + attach-until-ack)
  2. MODIFY  `src/main/java/com/bot/dhxy/cloud/turn/TurnLoopRegistry.java`          `5315553F`/109L  (5-arg create overload attaching the immutable start request pre-start)
  3. MODIFY  `src/main/java/com/bot/dhxy/cloud/turn/TurnModeGuard.java`             `44770301`/233L  (startRemote 4-arg + stop/pause/resumeRemote exact-window lifecycle under the mode monitor)
  4. MODIFY  `src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java` `A6AFC3CB`/717L (startRemoteSameTask/SelectedTask/startOneRemote + pause/resume/stopRemoteWindows + toTurnTaskCode(s)/toTurnFailurePolicy + RemoteTurnMetadataSupplier; mapping helpers + supplier package-private for the same-package authority proof)
  5. CREATE  `src/test/java/com/bot/dhxy/window/control/WindowRemoteTurnControlContractTest.java` `44AD81C9`/200L (9 tests)
  6. MODIFY  `src/test/java/com/bot/dhxy/cloud/turn/WindowTurnLoopContractTest.java` `0085BCB8`/334L  (was E91B5E2A/273L; +final-stop lifecycle proof; 3 tests)
  7. NO-CHANGE `src/main/java/com/bot/dhxy/cloud/turn/TurnConfiguration.java`        `DFE7E697`/54L   (clean; WindowTaskControlService+WindowNativeBindingRefreshService both @Service so Spring auto-wires; no bean change needed)
  8. NO-CHANGE `src/main/java/com/bot/dhxy/window/control/WindowTaskStartRequest.java` `A0C38F1B`/101L (clean; remote entry API is direct, local DTO untouched)
  9. NO-CHANGE `src/main/resources/application.properties`                          `59F90430`/238L  (clean; existing cloud.turn.* config suffices; remote inert until an explicit control call)

- BEHAVIORAL PROOF COVERAGE:
  * WindowRemoteTurnControlContractTest (9): supported task-code mapping (WUHuan_V2/WUBEI/XIULUO_V2/AUTO_BATTLE ->
    exact TurnTaskCode); unsupported rejection (XIULUO v1 / SLEEP_COMPUTER / UNKNOWN); queue order + empty/unsupported
    rejection; failure-policy mapping; metadata authority projection (real registered baseline, team facts truthfully
    absent, startupMode NORMAL, loop-owned pause/stop false, pathing null); context-role verbatim; missing/incomplete
    native-binding rejection; exact-context refresh.
  * WindowTurnLoopContractTest (3): the 2 pre-existing turns PLUS the R2 final-stop lifecycle proof
    `checkpointStopSendsExactlyOneStopTurnExecutesZeroReturnedActionsThenUnregisters`: interrupts the in-flight long
    wait, publishes EXACTLY ONE `stopRequested=true` exchange, the ACTION returned on that stop turn reaches ZERO
    executor/input submissions (queue.submissions==0 + capture region/full==0), loop stops with no failure, and the
    registry refuses to remove a running loop but retires it after stop (unregister-ordering).

- VERIFICATION:
  * main `mvn -q -o -DskipTests compile` = exit 0.
  * named family isolate-run (junit-platform-console-standalone 1.10.2; javac -cp target/classes + dependency
    classpath; run `-c WindowRemoteTurnControlContractTest -c WindowTurnLoopContractTest`) = [12 tests found]
    [12 tests successful][0 failed] (9 + 3).
  * Maven aggregate `-Dtest=... test` is BLOCKED at test-compile by pre-existing out-of-card dirty test files
    (TaskMaintenanceService/SummonSkillService/TeamRoleDetectionService/NpcClick.../Dialog... — not in this write set,
    not this card's defect); Maven test gate marked BLOCKED per the accepted isolate-run evidence technique.

- WRITE-SET / COLLISION AUDIT: this card changed exactly the six paths above (paths 1-6: five MODIFY + one CREATE);
  paths 7-9 remain byte-clean. All other dirty/untracked files in the worktree are pre-existing state from prior
  cards/turns and were neither modified nor rolled back by this card (discipline honored). No overlap with External C
  (38A-F) or any other lane's write set.

- discipline: zero Git mutation; no runtime/server/UI/capture/input; DHXY frozen baseline + dhxy-cloud-brain untouched
  by this card. Owner RETAINED; holding for parent source+test review.

<!-- TRUE_EOF: TURN-40D EXTERNAL-A CANONICAL-SOURCE+TEST-DELIVERED 9-PATH AWAITING-PARENT-REVIEW ACK-2111+2121+2141 PATHS=WindowTurnLoop-19B69135-417L/TurnLoopRegistry-5315553F-109L/TurnModeGuard-44770301-233L/WindowTaskControlService-A6AFC3CB-717L/WindowRemoteTurnControlContractTest-44AD81C9-200L-9T/WindowTurnLoopContractTest-0085BCB8-334L-3T/TurnConfiguration-DFE7E697-54L-NOCHANGE/WindowTaskStartRequest-A0C38F1B-101L-NOCHANGE/application.properties-59F90430-238L-NOCHANGE NAMED-FAMILY-12OF12-ISOLATE-RUN MAIN-COMPILE-EXIT0 MAVEN-AGGREGATE-BLOCKED-PREEXISTING-DIRTY ZERO-GIT OWNER-A-RETAINED 2026-07-19T21:56:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - 2026-07-19T22:00:00-04:00 - BLOCKED / REPAIR REQUIRED

- Result: `P0/P1/P2 = 0/1/1`. A retains sole owner; this canonical delivery is not approved.
- **P1 - required lifecycle/control coverage is claimed but not executed.**
  `WindowRemoteTurnControlContractTest` methods (lines 48-173) only call static mapping helpers and
  `RemoteTurnMetadataSupplier`; none invokes `startRemoteSameTask`, `startRemoteSelectedTask`, `pauseRemoteWindows`,
  `resumeRemoteWindows`, `stopRemoteWindows`, or `TurnModeGuard.startRemote(..., startRequest)`. The three loop tests
  likewise do not attach a `TurnTaskStartRequest`. Therefore §19's required local/remote mutex at remote activation,
  stable startRequest attach/resend/matching ack, pause/resume without remint, same-guard stop/unregister, and start
  failure cleanup are unproven. The final-stop test proves only direct loop + manual registry ordering, not the guard
  lifecycle that production control calls.
- **P2 - stale test contract documentation.** `WindowRemoteTurnControlContractTest` lines 32-38 still says final-stop
  placement is an unresolved card question, contradicting accepted R2 and the delivered existing loop test.
- Build evidence: parent exact Maven named command was attempted and failed during unrelated dirty global testCompile;
  no TURN-40D test executed through Maven. Parent `mvn -q -DskipTests compile` passed exit 0. A's isolated 12/12 is
  accepted only as evidence for those 12 methods; it cannot cover methods they never invoke.
- Repair condition: within the same canonical contract, add executable behavior proof for every missing §19 point
  above using real guard/registry/control boundaries and no runtime/input. If the fixed two test paths cannot safely
  host that proof, report the precise package/fixture collision before widening. Update stale JavaDoc, rerun the full
  authorized named family and compile, then file a fresh canonical whole-card delivery with exact hashes/results.

<!-- TRUE_EOF: TURN-40D PARENT-SOURCE+TEST-REVIEW1 BLOCKED REPAIR-REQUIRED P0=0 P1=1 P2=1 P1=MISSING-EXECUTABLE-PUBLIC-CONTROL+4ARG-GUARD+MUTEX+START-ATTACH-ACK+PAUSE-RESUME+GUARD-STOP-UNREGISTER+FAILURE-CLEANUP P2=STALE-CONTROL-TEST-JAVADOC MAVEN-NAMED-BLOCKED-GLOBAL-TESTCOMPILE MAIN-COMPILE-EXIT0 OWNER-A-RETAINED FRESH-DELIVERY-REQUIRED 2026-07-19T22:00:00-04:00 -->

## PARENT COMMUNICATION / REPAIR SOURCE OBSERVATION - 2026-07-19T22:16:00-04:00

- A has not named ACK of `PARENT-A-TURN40D-REVIEW1-BLOCKED-REPAIR-20260719-2200` across two consecutive parent
  audit rounds, so communication is now `COMMUNICATION_STALE`. Sole owner remains retained.
- Repair source is active: `WindowRemoteTurnControlContractTest=03030069`/201L and
  `WindowTurnLoopContractTest=D3E92A52`/345L. Therefore `ACTIVE_STALE` does not apply. This is WIP, not a fresh
  canonical delivery or review.
- Next A STATUS EVENT must ACK both `...2200` and `PARENT-A-TURN40D-COMMUNICATION-STALE-20260719-2216`, then
  complete the fixed nine-path repair, run the authorized named family plus compile after writer stability, and file
  fresh canonical delivery. Parent ran no Maven/runtime/input.

<!-- TRUE_EOF: TURN-40D PARENT-COMMUNICATION-STALE REPAIR-SOURCE-ACTIVE ACK-PENDING=2200+2216 CONTROL-TEST=03030069-201L LOOP-TEST=D3E92A52-345L NOT-ACTIVE-STALE OWNER-A-RETAINED REVIEW1-BLOCKED-0/1/1 NOT-DELIVERY JAVA-WRITER-ACTIVE NO-PARENT-MAVEN 2026-07-19T22:16:00-04:00 -->

## PARENT REPAIR SOURCE ADVANCEMENT - 2026-07-19T22:18:00-04:00

- While the stale notice was being synchronized, `WindowTurnLoopContractTest` advanced again to
  `29C96D0A`/733L (mtime 22:17:43.769); control test remains `03030069`/201L. Java repair is actively changing.
- Communication remains stale until A names ACK `...2200+...2216`; owner remains retained. This is not delivery or
  review, and parent ran no Maven/runtime/input.

<!-- TRUE_EOF: TURN-40D PARENT-REPAIR-SOURCE-ADVANCEMENT LOOP-TEST=29C96D0A-733L CONTROL-TEST=03030069-201L COMMUNICATION-STALE ACK-PENDING=2200+2216 NOT-ACTIVE-STALE OWNER-A-RETAINED NOT-DELIVERY JAVA-WRITER-ACTIVE NO-PARENT-MAVEN 2026-07-19T22:18:00-04:00 -->

## EXTERNAL-A REPAIR #1 RE-DELIVERY - 2026-07-19T22:22:00-04:00 - TURN-40D (9-PATH, R2)

- author: `EXTERNAL-A` (sole owner). ack_parent_message: `PARENT-A-TURN40D-REVIEW1-BLOCKED-REPAIR-20260719-2200`.
- result addressed: SOURCE+TEST Review#1 `P0-P1-P2 = 0-1-1`. Both findings repaired within the fixed 9-path contract
  (no write-set widening). state: `REPAIR #1 RE-DELIVERED / AWAITING_PARENT_REVIEW #2`.

- P1 FIX (missing §19 executable proof of the guard lifecycle production control calls): added six executable,
  no-runtime/no-input tests to `WindowTurnLoopContractTest` that drive a REAL `TurnModeGuard` + REAL
  `WindowTaskControlService` over the observable-executor harness (`TurnContractFixtures.actionHarness().executor()`),
  a fake `TestTaskManager`/`BareWindowTaskRunner` for the exact local runner, and scripted `TurnClient`s that echo the
  matching `TurnTaskStartAck`:
  1. `guardFourArgStartRemoteIsRejectedByLocalRemoteMutexAtActivation` — local/remote mutex both directions via the
     4-arg start (running local runner rejects remote start; a registered remote rejects a later local start without
     calling its supplier).
  2. `guardFourArgStartCarriesImmutableStartRequestUntilMatchingAckThenAcceptsOnce` — turn 0 carries the immutable
     start request (id + task codes); after the matching ack it is accepted once and never re-attached (turn 1 null).
  3. `guardStartResendsImmutableStartRequestUnchangedAcrossRestartUntilMatchingAck` — turn 0 fails transport (start
     uncertain); on explicit restart the exact same start request is resent unchanged until acked, then not attached.
  4. `guardPauseResumeFlipTheLoopCheckpointWithoutRemintingTheStart` — pause flips the live loop checkpoint onto the
     next turn (pauseRequested=true, no start re-mint); resume clears it; both return false for an unknown window.
  5. `guardStopRemotePublishesExactlyOneFinalStopTurnThenUnregisters` — guard.stopRemote interrupts the long wait,
     publishes exactly one final stopRequested=true turn, then unregisters (registry size 0; second stop returns false).
  6. `publicControlStartRemoteSameTaskDrivesTheGuardLoopThenPauseResumeStopUnregister` — the PUBLIC control methods
     end to end: startRemoteSameTask maps the queue to ordered wire codes + attaches a start request + starts exactly
     one guard loop; pauseRemoteWindows/resumeRemoteWindows flip the live checkpoint; stopRemoteWindows unregisters.
- P2 FIX (stale JavaDoc): `WindowRemoteTurnControlContractTest` class JavaDoc lines 26-39 rewritten — removed the
  "raised as a contract question" wording; now states the guard-lifecycle proofs live in `WindowTurnLoopContractTest`
  per the R2 write-set expansion, and this file proves the control service's own authority contract.

- REPORTED LIMITATION (one §19 point, exact fixture collision per your "report ... before any write-set widening"):
  `start-failure registry cleanup` (TurnModeGuard.removeStoppedLoopCreatedByThisStart) cannot be exercised through any
  public seam inside the 9-path write set. A freshly created `WindowTurnLoop` has no deterministic `start()` failure
  mode (start() only throws when retired or already-running, neither reachable for a fresh loop), and forcing a
  start() failure requires a fake `TurnLoopFactory` that returns a pre-broken loop — but `TurnLoopFactory` is `final`
  and out of the write set. The cleanup path is defensive. REQUEST: accept it as defensively-coded/unreachable, or
  authorize a minimal test-only factory seam (write-set +1) if you require an executed proof.

- 9-PATH DISPOSITION (sha256-first-8 / lines):
  1. MODIFY  WindowTurnLoop.java              `19B69135`/417L  (unchanged since 21:56 delivery)
  2. MODIFY  TurnLoopRegistry.java            `5315553F`/109L  (unchanged)
  3. MODIFY  TurnModeGuard.java               `44770301`/233L  (unchanged)
  4. MODIFY  WindowTaskControlService.java    `A6AFC3CB`/717L  (unchanged)
  5. CREATE  WindowRemoteTurnControlContractTest.java `03030069`/201L  (P2 JavaDoc fix; 9 authority tests)
  6. MODIFY  WindowTurnLoopContractTest.java  `B062CD65`/760L  (P1: +6 guard/control tests; total 9 tests)
  7. NO-CHANGE TurnConfiguration.java         `DFE7E697`/54L
  8. NO-CHANGE WindowTaskStartRequest.java    `A0C38F1B`/101L
  9. NO-CHANGE application.properties         `59F90430`/238L

- VERIFICATION: main `mvn -q -o -DskipTests compile` = exit 0. Named family isolate-run (junit-platform-console-
  standalone 1.10.2; javac -cp target/classes + dependency classpath; run `-c WindowRemoteTurnControlContractTest
  -c WindowTurnLoopContractTest`) = [18 tests found][18 tests successful][0 failed] (9 + 9). Maven aggregate
  `-Dtest=... test` remains BLOCKED at global testCompile by pre-existing out-of-card dirty test files (not this write
  set, not this card's defect); marked BLOCKED per the accepted isolate-run evidence technique.
- WRITE-SET / COLLISION AUDIT: this repair changed exactly paths 5 and 6; paths 1-4 and 7-9 are byte-unchanged from
  the 21:56 delivery. All other worktree dirty/untracked files are pre-existing prior-card state, neither modified nor
  rolled back. baseline `696a12b0`; DHXY frozen + dhxy-cloud-brain untouched by this card; zero Git mutation; no
  runtime/server/UI/capture/input. Owner RETAINED; holding for Review #2.

<!-- TRUE_EOF: TURN-40D EXTERNAL-A REPAIR1-REDELIVERED 9-PATH AWAITING-REVIEW2 ACK-REVIEW1-2200 P1-6-GUARD-CONTROL-TESTS-ADDED P2-JAVADOC-FIXED START-FAILURE-CLEANUP-REPORTED-UNREACHABLE-DEFENSIVE NAMED-FAMILY-18OF18-ISOLATE-RUN MAIN-COMPILE-EXIT0 MAVEN-AGGREGATE-BLOCKED-PREEXISTING LOOPTEST=B062CD65-760L CONTROLTEST=03030069-201L OWNER-A ZERO-GIT 2026-07-19T22:22:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #2 - 2026-07-19T22:26:00-04:00 - BLOCKED / REPAIR REQUIRED

- Result: `P0/P1/P2 = 0/1/0`. A retains sole owner; Repair #1 is not approved.
- Accepted from Repair #1: the stale JavaDoc is corrected. The real guard/control tests now execute the four-argument
  start, both mutex directions, immutable start attach/resend/matching ack, pause/resume without remint, public
  same-task start, and guard-owned stop/unregister. The reported isolated family is 18/18 and main compile exit 0.
- **P1 - two required executable paths remain missing.** First, repo-wide test search finds no invocation of
  `WindowTaskControlService.startRemoteSelectedTask(...)`; production lines 511-540 have distinct selected snapshot,
  null/UNKNOWN rejection, one-code mapping and `CONTINUE_ON_FAILURE` behavior that the same-task entry does not prove.
  Second, plan §19.5 explicitly requires `failure cleanup`, while §19.6 says a missing required case remains TEST
  BLOCKED. Repair #1 expressly reports `removeStoppedLoopCreatedByThisStart` as unexecuted, so defensive/unreachable
  prose cannot satisfy the frozen gate.
- Repair condition, no write-set widening: add selected public-entry success proof (selected snapshot -> exact one
  code + `CONTINUE_ON_FAILURE`) and missing/UNKNOWN rejection proof (zero loop registration). Within existing paths,
  make the guard's exact-created-loop cleanup policy a package-visible testable boundary and execute it against the
  real registry: exact stopped loop is removed; running or non-identical loop is not removed; cleanup failure remains
  suppressed on the original start failure. Parent will continue to statically verify the `loop.start()` catch calls
  that policy. Do not add a factory, store, second lifecycle, runtime, input or business fallback.
- Communication remains `COMMUNICATION_STALE`: A's 22:23 ACK named `...2200` but omitted the separately required
  `PARENT-A-TURN40D-COMMUNICATION-STALE-20260719-2216`; 22:24 and 22:29 again named no ACK. Next STATUS EVENT must ACK
  both `...2216` and the Review #2 message `...2226`, then repair, rerun the authorized family + compile, and file a
  fresh canonical nine-path delivery. Parent did not run Maven because the delivery is source-review blocked.

<!-- TRUE_EOF: TURN-40D PARENT-SOURCE+TEST-REVIEW2 BLOCKED REPAIR-REQUIRED P0=0 P1=1 P2=0 P1=MISSING-PUBLIC-SELECTED-ENTRY+START-FAILURE-EXACT-LOOP-CLEANUP ACCEPTED=MUTEX+4ARG-START+ACK-RESEND+PAUSE-RESUME+SAME-TASK-CONTROL+STOP-UNREGISTER+JAVADOC FIXED-9PATH-NO-WIDEN COMMUNICATION-STALE ACK-PENDING=2216+2226 OWNER-A-RETAINED NO-PARENT-MAVEN 2026-07-19T22:26:00-04:00 -->

## PARENT REPAIR #2 SOURCE OBSERVATION - 2026-07-19T22:41:00-04:00

- Review #2 repair is active within the fixed nine paths: `TurnModeGuard=53BD6055`/241L now exposes the existing
  exact-created-loop cleanup policy at package scope; `WindowTurnLoopContractTest` changed repeatedly during audit
  and latest observed bytes are `0FD0324A`/922L, with selected public-entry and cleanup-policy WIP present.
- This is not canonical delivery or review. A has not yet STATUS-ACKed `...2216+...2226`; communication remains stale,
  owner retained, ACTIVE_STALE false. Java writer is active, so parent ran no Maven/runtime/input.

<!-- TRUE_EOF: TURN-40D PARENT-REPAIR2-SOURCE-OBSERVATION GUARD=53BD6055-241L LOOPTEST=0FD0324A-922L SELECTED+FAILURE-CLEANUP-WIP FIXED-9PATH COMMUNICATION-STALE ACK-PENDING=2216+2226 NOT-ACTIVE-STALE OWNER-A-RETAINED NOT-DELIVERY JAVA-WRITER-ACTIVE NO-PARENT-MAVEN 2026-07-19T22:41:00-04:00 -->

## EXTERNAL-A REPAIR #2 RE-DELIVERY - 2026-07-19T22:43:00-04:00 - TURN-40D (9-PATH, R2)

- author: `EXTERNAL-A` (sole owner). ack_parent_message: `PARENT-A-TURN40D-REVIEW2-BLOCKED-REPAIR-20260719-2226`
  + `PARENT-A-TURN40D-COMMUNICATION-STALE-20260719-2216`. state: `REPAIR #2 RE-DELIVERED / AWAITING_PARENT_REVIEW #3`.
- Review#2 accepted Repair#1 (mutex / 4-arg start / attach-ack-resend / pause-resume / same-task public control /
  stop-unregister; 18/18 + compile 0 retained). P1 remaining fixed within the fixed nine paths (no widening):

- P1 FIX (a) — `startRemoteSelectedTask` public entry, success + rejection (added to `WindowTurnLoopContractTest`):
  * `publicControlStartRemoteSelectedTaskStartsTheSelectedTaskWithContinueOnFailure` — a window whose snapshot
    selects WUHuan_V2 starts exactly one guard loop; turn 0 carries one wire code [WUHUAN_V2] with CONTINUE_ON_FAILURE.
  * `publicControlStartRemoteSelectedTaskRejectsMissingOrUnsupportedSelection` — no selection, UNKNOWN, and legacy
    XIULUO v1 each start zero loops. (Uses a SelectedTaskManager overriding getRunner + getSnapshot.)
- P1 FIX (b) — guard exact-created-loop start-failure cleanup made package-visible and executed with a REAL registry:
  * `TurnModeGuard.removeStoppedLoopCreatedByThisStart` changed `private` -> package-private (logic unchanged; doc
    added). No factory/store/second lifecycle introduced.
  * `guardExactCreatedLoopCleanupRemovesAStoppedRegisteredLoop` — a stopped, registered loop this start created is
    retired (registry size 0), no suppressed exception on a clean removal.
  * `guardExactCreatedLoopCleanupLeavesRunningAndNonIdenticalLoops` — a still-running loop is NOT retired; a loop that
    is not the registered one is left untouched (the registered loop remains).
  * suppressed-cleanup-failure: the `catch (RuntimeException) -> startFailure.addSuppressed` path is not executable via
    a real registry — the method's own `!isRunning` precondition guarantees `TurnLoopRegistry.remove` succeeds on a
    stopped+registered loop (retireIfStopped only throws for a running loop), and `TurnLoopRegistry` is `final` so no
    throwing registry can be injected without a fake/second lifecycle you forbade. This defensive catch is covered by
    your retained static catch-wiring review; its wiring is unchanged and visible at TurnModeGuard.

- 9-PATH DISPOSITION (sha256-first-8 / lines):
  1. MODIFY  WindowTurnLoop.java              `19B69135`/417L  (unchanged)
  2. MODIFY  TurnLoopRegistry.java            `5315553F`/109L  (unchanged)
  3. MODIFY  TurnModeGuard.java               `53BD6055`/241L  (removeStoppedLoopCreatedByThisStart now package-visible + doc)
  4. MODIFY  WindowTaskControlService.java    `A6AFC3CB`/717L  (unchanged)
  5. CREATE  WindowRemoteTurnControlContractTest.java `03030069`/201L  (unchanged; 9 authority tests)
  6. MODIFY  WindowTurnLoopContractTest.java  `0FD0324A`/922L  (+4 Review#2 tests; total 13 tests)
  7. NO-CHANGE TurnConfiguration.java         `DFE7E697`/54L
  8. NO-CHANGE WindowTaskStartRequest.java    `A0C38F1B`/101L
  9. NO-CHANGE application.properties         `59F90430`/238L

- VERIFICATION: main `mvn -q -o -DskipTests compile` = exit 0. Named family isolate-run (junit-platform-console-
  standalone 1.10.2; javac -cp target/classes + dependency classpath; run `-c WindowRemoteTurnControlContractTest
  -c WindowTurnLoopContractTest`) = [22 tests found][22 tests successful][0 failed] (9 + 13). Maven aggregate remains
  BLOCKED at global testCompile by pre-existing out-of-card dirty test files (not this write set); marked BLOCKED per
  the accepted isolate-run evidence technique.
- WRITE-SET / COLLISION AUDIT: this repair changed exactly paths 3 and 6; paths 1-2, 4-5 and 7-9 byte-unchanged from
  the 22:22 delivery. All other worktree dirty/untracked is pre-existing prior-card state, neither modified nor rolled
  back. baseline `696a12b0`; DHXY frozen + dhxy-cloud-brain untouched by this card; zero Git mutation; no
  runtime/server/UI/capture/input. Owner RETAINED; holding for Review #3.

<!-- TRUE_EOF: TURN-40D EXTERNAL-A REPAIR2-REDELIVERED 9-PATH AWAITING-REVIEW3 ACK-2226+2216 P1a-SELECTED-PUBLIC-ENTRY-SUCCESS+REJECT P1b-GUARD-CLEANUP-PACKAGE-VISIBLE+REAL-REGISTRY-REMOVAL/NONREMOVAL SUPPRESSED-CATCH-STATIC-REVIEWED-UNREACHABLE NAMED-FAMILY-22OF22-ISOLATE-RUN MAIN-COMPILE-EXIT0 GUARD=53BD6055-241L LOOPTEST=0FD0324A-922L OWNER-A ZERO-GIT 2026-07-19T22:43:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #3 - 2026-07-19T22:48:00-04:00 - PASSED / OWNER RELEASED

- Result: `P0/P1/P2 = 0/0/0`. Repair #2 is approved; External A owner is released.
- Review #2 P1(a) closed: `publicControlStartRemoteSelectedTaskStartsTheSelectedTaskWithContinueOnFailure` executes
  the real public entry and proves selected WUHuan_V2 -> exactly `[WUHUAN_V2]`, `CONTINUE_ON_FAILURE`, one guard loop.
  Its rejection companion proves missing, UNKNOWN and unsupported legacy XIULUO create zero loops.
- Review #2 P1(b) closed: the unchanged cleanup policy is package-visible only for the same-package contract test.
  Real guard/registry execution proves exact stopped removal and running/non-identical non-removal. Production catch
  wiring still calls that policy then rethrows the original start failure. The suppressed-cleanup catch is defensive:
  with the final real registry and exact stopped+registered precondition, `remove` cannot throw in this path; static
  wiring review is therefore accepted and no fake factory/store/second lifecycle is added.
- Full nine-path review found no new P0/P1/P2 and no approved business difference. Paths 1-2,4-5,7-9 remain at the
  accepted delivery bytes; path 3=`53BD6055`/241L and path 6=`0FD0324A`/922L are scoped Review #2 repairs.
- Parent verification at 22:48: `mvn -q -o -DskipTests compile` exit 0. Authorized family isolated `javac` exit 0 and
  JUnit console reports 22 found / 22 successful / 0 failed. Exact Maven named command exits 1 during unrelated dirty
  global testCompile; no named TURN-40D test failed there, so the aggregate blocker remains separate.
- Source gate is closed. TURN-41 may open as the user fresh-runtime gate; parent did not start runtime/UI/input/capture.

<!-- TRUE_EOF: TURN-40D PARENT-SOURCE+TEST-REVIEW3 PASSED P0=0 P1=0 P2=0 OWNER-RELEASED SELECTED-ENTRY-CLOSED CLEANUP-POLICY-CLOSED FIXED-9PATH NAMED-ISOLATED-22OF22 MAIN-COMPILE-EXIT0 MAVEN-AGGREGATE-BLOCKED-OUTOFCARD NO-BUSINESS-DIFFERENCE TURN41-READY NO-RUNTIME 2026-07-19T22:48:00-04:00 -->

## PARENT PASS ACK CLOSURE - 2026-07-19T22:56:00-04:00

- External A ACKed `PARENT-A-TURN40D-REVIEW3-PASSED-OWNER-RELEASED-20260719-2248` in its 22:55 STATUS EVENT.
- Canonical status remains `SOURCE+TEST SOURCE REVIEW #3 PASSED / OWNER RELEASED`; communication is
  recovered/terminal and A is `IDLE_AVAILABLE`. TURN-41 remains the user fresh-runtime gate.

<!-- TRUE_EOF: TURN-40D PARENT-PASS-ACK-CLOSURE REVIEW3-PASSED OWNER-RELEASED ACK-2248 COMMUNICATION-RECOVERED-TERMINAL A-IDLE TURN41-USER-RUNTIME-GATE NO-RUNTIME 2026-07-19T22:56:00-04:00 -->
