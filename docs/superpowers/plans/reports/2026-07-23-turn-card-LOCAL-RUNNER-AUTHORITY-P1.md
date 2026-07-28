# LOCAL-RUNNER-AUTHORITY-P1

## Canonical Contract

- owner: `Codex implementation worker`
- state: `REPAIR REQUIRED / P0-P1-P2=0/2/2`
- client worktree: `D:\mavenProject\DHXY-cr271` / `thin-client-design`
- cloud worktree: `D:\mavenProject\dhxy-cloud-brain`
- read-only business baseline: `D:\mavenProject\DHXY`
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`

## User-Approved Boundary

1. `WindowObservationRunner` and its per-window runtime owner authoritatively maintain pathing and combat
   enter/exit temporal state. Fixed template matching may remain local; OCR, map/NPC recognition and business
   decisions remain Cloud-owned.
2. A fast expected-exit hit immediately notifies Cloud. Cloud enters `RETURN_HOME` and immediately issues the
   return-home action without waiting for a second proof.
3. If the return action does not verify the start map and trusted combat still reports `IN_COMBAT`, Cloud must
   remain in `RETURN_HOME`. It invokes exactly one local runtime operation that arms replay of the just-executed
   return command. Local state corrects to `IN_COMBAT`, retains the command and replays it directly after the
   true local combat exit; that exit must not request a replacement command from Cloud.
4. The retained replay is restricted to XIULUO_V2/WUBEI post-combat return-home. Stop, replacement task,
   taskRun change, window-id/HWND identity change or runner close clears it.
5. `FIND_AND_USE_TASK_PAGE` re-executes the same semantic macro. A cached screen-absolute click retains source
   HWND and source window origin; replay focuses that exact HWND, reads its current origin, applies only the
   translation delta and performs move+click in one atomic input sequence. Resize/DPI change is unsupported and
   fails closed.
6. Non-combat return failures retain the existing Cloud failure/navigation fallback.

## Business Baseline Evidence

- `docs/业务逻辑.md`, `Expected 战斗快脱战与回程验证兜底`:
  - fast expected exit immediately enters return-home;
  - no second full-radar proof may delay the fast path;
  - a failed return must use trusted combat correction before ordinary return failure handling.
- baseline `696a12b0`:
  - `AutoCombatService.handleCombatTick(...)` emits the fast expected exit immediately;
  - `XiuluoTaskV2.waitCombat(...)` immediately advances to `RETURN_HOME`;
  - return verification failure uses trusted radar correction.
- Approved business difference:
  - the baseline/document rollback to `WAIT_COMBAT` / `WAIT_BATTLE_FINISH` is intentionally replaced for this
    exact false-positive return episode. Cloud stays in `RETURN_HOME`; local runtime owns the correction and
    replay. All other decision, fallback and navigation behavior remains baseline-equivalent.

## Planned Write Set

- symmetric client/cloud protocol carrier and validator changes for one replay-arm runtime operation;
- client per-window retained return owner, cleanup hooks, local combat-exit trigger and translated cached click;
- Cloud XIULUO_V2/WUBEI trusted-correction branches and typed local runtime client invocation;
- durable status/architecture/migration documentation and generated dashboard data.

## Verification Gate

- no runtime, UI, live capture or physical input execution;
- no tests are created or run;
- client and Cloud Java compile must both exit `0`;
- every symmetric protocol carrier changed by this card must be byte-identical across repositories.

## Delivery And Parent Review

- Client delivered:
  - per-window retained command state in `WindowRuntimeContext`;
  - stable `combat-signal` transition trigger independent of the consumed Fast Exit interest;
  - exact-HWND translated cached click and semantic task-page macro replay;
  - stop/runtime/native identity cleanup.
- Cloud delivered:
  - retained identity on initial XIULUO/WUBEI return-item use;
  - one typed replay-arm operation after trusted `IN_COMBAT`;
  - `RETURN_HOME` remains current while waiting; replay wake performs map verification only.
- Parent findings: `P0=0 / P1=0 / P2=0`.
- Verification:
  - Client compile exit `0`;
  - Cloud compile exit `0`;
  - four modified shared protocol files byte-identical by SHA-256;
  - no runtime/UI/live capture/input and no Git mutation.
- Fresh runtime gate: exercise a false Fast Exit followed by trusted `IN_COMBAT`, observe exactly one arm,
  one local true-exit replay and no Cloud rollback; separately move the same-size window before cached replay
  and verify the translated click stays on the original item.

## Parent final review amendment

- Removed the duplicate Xiuluo known-combat arm branch and routed it through the single correction owner.
- Corrected the stale Wubei comment that still described the removed `WAIT_BATTLE_FINISH` rollback.
- Closed the final atomicity gap: a retained replay exception or exact-window/input failure clears the one-shot
  replay fail-closed and suppresses the fast-exit event, so Cloud cannot advance without the local action.
- Recompiled after the amendment: Client compile exit `0`; Cloud compile exit `0`.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 FINAL-AMENDMENT PARENT-SOURCE-REVIEW-PASSED P0-0-P1-0-P2-0 DUAL-COMPILE DTO-BYTE-IDENTICAL FRESH-RUNTIME-REQUIRED 2026-07-23 -->

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 PARENT-SOURCE-REVIEW-PASSED P0-0-P1-0-P2-0 DUAL-COMPILE DTO-BYTE-IDENTICAL FRESH-RUNTIME-REQUIRED 2026-07-23 -->

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 FINAL-AMENDMENT PARENT-SOURCE-REVIEW-PASSED P0-0-P1-0-P2-0 DUAL-COMPILE DTO-BYTE-IDENTICAL EXIT-EVENT-SUPPRESSED-ON-REPLAY-FAILURE FRESH-RUNTIME-REQUIRED 2026-07-23 -->

## Parent source re-review #2 - previous pass withdrawn

### P1-1 - incompatible run identities make replay unreachable

- Client `WindowTurnLoop.startObservationRunnerIfEligible()` passes
  `startRequest.startRequestId()` (`remote-turn-*`) to the observation runner.
- Cloud `CloudTurnTaskRuntime.start()` constructs business taskRunId as
  `startRequestId + ":" + index + ":" + code`.
- Retain and arm store the complete business taskRunId, while
  `WindowObservationSampler` calls `hasArmedReturnHomeReplay(...)` and
  `claimArmedReturnHomeReplay(...)` with the observation startRequestId.
- The exact string comparisons can never match. Required repair: freeze one explicit two-part identity
  contract; do not infer or parse suffixes ad hoc.

### P1-2 - replay failure has no complete terminal handoff

- `FastExpectedCombatExitProbe.sample()` sets `emitted=true` before returning the edge.
- The sampler suppresses that edge when replay fails; the coordinator has already claimed and then clears
  the retained command.
- Resetting only the probe or publishing only the edge is insufficient by itself: Cloud's armed
  `RETURN_HOME` marker must receive a typed replay failure/success terminal and own an explicit fallback.

### P2-1 - observation thread is synchronously blocked

- `WindowObservationSampler.collect()` calls the retained bag macro through
  `submitFrozenExactWindowExclusiveAndWait(...)`.
- Until that multi-step macro returns, the same window emits no combat/pathing/coordinate observation batch.
- Required repair: an asynchronous per-window replay state machine must preserve event/action ordering without
  running the macro on the sampler thread.

### P2-2 - missing contract tests

- No focused test covers observation/business run identity mapping, retain-arm-claim-replay, replay failure
  handoff, one-shot behavior, same-size translation, or HWND/size rejection.
- Dual compile and DTO hashes do not close these contracts.

### Clarification and separate incident

- The claim that a normal `NOT_USED` return reaches arm is not supported: correction is entered only from
  `USED_START_MAP_UNVERIFIED`. Arm failure after lifecycle clearing still needs a defined fallback.
- The 19:34 fresh run independently shows `shortcut incidental combat detected`, so expected-combat fast exit
  was never armed. That late classification race is separate from this retained replay card.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-REQUIRED P0-0-P1-2-P2-2 RUN-IDENTITY-MISMATCH REPLAY-FAILURE-HANDOFF-MISSING OBSERVER-BLOCKING TESTS-MISSING 2026-07-23 -->

## User boundary decision - universal local fast-exit detection

- Local Runner fast-exit mechanics are active for every locally observed combat generation. Detection is not
  conditional on Cloud classifying the combat as expected or incidental.
- Expected combat is established only by an explicit exact-window enter claim:
  1. the sanctioned local enter-battle template path successfully clicked; or
  2. the sanctioned Cloud fallback successfully clicked.
  The next local combat-visible edge consumes that exact taskRun/attempt/generation claim.
- A combat generation without that explicit claim is incidental. Local Runner still detects its enter and exit,
  but must not publish `FAST_EXPECTED_COMBAT_EXIT` or trigger Xiuluo/Wubei post-combat/`RETURN_HOME`.
- Ordinary combat-state observation may tell Cloud that incidental combat ended so its existing recovery can
  continue; that is not an expected fast-exit business edge and must not run the expected post-combat transition.
- Do not infer expected combat from pathing phase, parser terminal state, timing proximity, or movement alone.
- The 19:34 `shortcut incidental combat detected` incident is therefore part of this card's classification and
  publication boundary, not excluded from repair.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 USER-BOUNDARY UNIVERSAL-LOCAL-FAST-DETECTION EXPLICIT-EXPECTED-ENTER-CLAIM INCIDENTAL-NO-BUSINESS-EDGE REPAIR-REQUIRED 2026-07-23 -->

## Repair #1 whole-card source + test re-delivery

### Delivered behavior

- Froze two explicit identities end to end: `observationRunId=startRequestId` and the complete
  `businessTaskRunId`. Retain, arm, armed lookup, claim and replay now validate both identities together with
  task code, exact window id and HWND; no suffix parsing is used.
- Moved retained return-home replay off `WindowObservationSampler` onto one daemon single-thread executor per
  window. Success publishes `RETURN_HOME_REPLAY_SUCCEEDED` before the expected exit edge; execution failure and
  HWND/size rejection publish typed failure terminals so Cloud can wake and take its fallback.
- Preserved the approved correction boundary for Xiuluo/Wubei only: same HWND and size are mandatory, same-size
  window translation adjusts cached click coordinates, resize/DPI geometry changes fail closed, and arm failure
  degrades instead of throwing `TaskFatalException`.
- Local fast-exit mechanics now run for every visible combat generation. Only a local-template or successful
  Cloud-fallback explicit enter claim can bind the next exact generation and authorize
  `FAST_EXPECTED_COMBAT_EXIT`; moving/incidental/no-claim generations update local combat state but publish no
  expected business edge. Cloud no longer publishes the obsolete fast-exit observation interest.
- Green-chain schedule ownership also carries the explicit observation-run identity. Missing Cloud mapping
  returns typed `NOT_EXECUTED` instead of sending an identity-less schedule.

### Changed files

Client:

- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnBagOperationArguments.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnWholeTaskRuntimeArguments.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnLocalOperation.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationKeyEvent.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationKeyEventType.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationProtocolValidator.java`
- `src/main/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcher.java`
- `src/main/java/com/bot/dhxy/cloud/turn/local/WholeTaskRuntimeLocalOperationExecutor.java`
- `src/main/java/com/bot/dhxy/model/job/XiuluoGreenChainSchedule.java`
- `src/main/java/com/bot/dhxy/window/model/WindowExpectedCombatEnterClaim.java`
- `src/main/java/com/bot/dhxy/window/model/WindowRetainedReturnHomeReplay.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/observation/FastExpectedCombatExitProbe.java`
- `src/main/java/com/bot/dhxy/window/observation/DeferredReturnHomeReplayCoordinator.java`
- `src/main/java/com/bot/dhxy/window/observation/WindowObservationRunner.java`
- `src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java`
- `src/test/java/com/bot/dhxy/window/observation/FastExpectedCombatExitProbeTest.java`
- `src/test/java/com/bot/dhxy/window/observation/DeferredReturnHomeReplayCoordinatorContractTest.java`
- `src/test/java/com/bot/dhxy/window/runtime/LocalRunnerIdentityContractTest.java`

Cloud:

- the same seven shared protocol files listed above
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudBagLocalServiceClient.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudWholeTaskRuntimeLocalServiceClient.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudFastExpectedCombatExitCoordinator.java`
- `src/main/java/com/bot/dhxy/window/model/WindowReadyEventType.java`
- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
- `src/test/java/com/yueyunfe/dhxy/cloudbrain/observation/FastExpectedExitObservationContractTest.java`

### Verification

- Client:
  `mvn -q "-Dtest=FastExpectedCombatExitProbeTest,LocalRunnerIdentityContractTest,DeferredReturnHomeReplayCoordinatorContractTest,XiuluoKandaProductionChainContractTest" test`
  -> exit `0`.
- Client: `mvn -q -DskipTests compile` -> exit `0`.
- Cloud:
  `mvn -q "-Dtest=CloudFastExpectedCombatExitArmLifecycleTest,FastExpectedExitGateTest,FastExpectedExitObservationContractTest" test`
  -> exit `0`.
- Cloud: `mvn -q compile` -> exit `0`.
- Shared protocol SHA comparison: `7/7` byte-identical.
- No runtime, UI, capture or input action was started. Fresh-runtime acceptance remains a parent/user gate.

State: `WHOLE-CARD SOURCE+TEST RE-DELIVERED / AWAITING_PARENT_REVIEW`; owner retained. This worker does not
self-approve.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-1 WHOLE-CARD-SOURCE-TEST-REDELIVERED AWAITING-PARENT-REVIEW DUAL-COMPILE NAMED-TESTS-PASS DTO-7-OF-7-BYTE-IDENTICAL NO-RUNTIME OWNER-RETAINED 2026-07-23 -->

## Parent source review #3 - Repair #1 rejected

### P1-1 - replay failure terminal wakes but never transfers control to fallback

- `CloudFastExpectedCombatExitCoordinator.accept(...)` maps
  `RETURN_HOME_REPLAY_FAILED` and `RETURN_HOME_REPLAY_IDENTITY_REJECTED` to the same generic
  `RETURN_HOME_REPLAY_TERMINAL` ready event as success. It does not retain a typed, exact-run,
  one-shot terminal result for the task state machine.
- `XiuluoTaskV2.returnHome(...)` and
  `WubeiTask.returnHomeAfterCombatOrContinueSpecialTarget(...)` continue to identify the state by
  the unchanged `local-return-replay-armed` source. If the start map is still unverified after a
  failed/rejected terminal wake, both methods wait again instead of entering the existing Cloud
  return/navigation fallback.
- Result: the new terminal is observable but not actionable; a failed replay can remain in
  `RETURN_HOME` indefinitely. Repair must distinguish success from failure/rejection, fence and
  consume the terminal by exact business task run/window/task, and make failure/rejection enter a
  real Cloud fallback while success remains verify-only.

### Required companion proof

- A successful enter click whose attempt is later replaced or abandoned must not leave a stale
  local expected-combat claim capable of classifying a later incidental combat generation as
  expected. Repair #2 must either show the existing exact-attempt cleanup path with production
  evidence or add explicit cleanup and a behavioral test.
- Tests must execute behavior rather than only searching source text. Required coverage includes
  Xiuluo and Wubei failure/rejection fallback, success without duplicate return action, exact-run
  stale fencing and one-shot terminal consumption.

State: `REPAIR REQUIRED / P0-P1-P2=0/1/0`; owner retained for Repair #2.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 PARENT-REVIEW-3 REPAIR-2-REQUIRED P0-0-P1-1-P2-0 REPLAY-FAILURE-FALLBACK-NOT-CONSUMED STALE-EXPECTED-CLAIM-PROOF-REQUIRED OWNER-RETAINED 2026-07-23 -->

### P1-2 - claimed asynchronous replay survives stop or task replacement

- `DeferredReturnHomeReplayCoordinator.submitOnLocalExit(...)` claims the retained command before
  submitting it to a process-level per-window executor. Once claimed, the command is no longer
  controlled by the retained slot that `WindowRuntimeContext.resetRuntimeState(...)` and native
  rebinding clear.
- `executeReplay(...)` rechecks HWND and size, but it does not prove that the original observation
  run and business task run are still active immediately before input. A queued replay can therefore
  execute against the same HWND after stop, runner close or replacement task.
- Repair requires a cancellable exact-run in-flight lifecycle fence at queue time and immediately
  before physical input. Reset, stop, replacement and runner close must invalidate it. A behavioral
  race test must claim, replace/reset before executor execution, and prove zero input.

Updated state: `REPAIR REQUIRED / P0-P1-P2=0/2/0`; owner retained for Repair #2.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 PARENT-REVIEW-3 REPAIR-2-REQUIRED P0-0-P1-2-P2-0 FAILURE-FALLBACK-NOT-CONSUMED ASYNC-REPLAY-SURVIVES-STOP STALE-EXPECTED-CLAIM-PROOF-REQUIRED OWNER-RETAINED 2026-07-23 -->

## Repair #2 whole-card source + test re-delivery

### Delivered behavior

- Cloud now persists replay terminals as exact
  `(observationRunId, windowId, taskCode, businessTaskRunId)` typed one-shot state:
  `SUCCEEDED`, `FAILED`, and `IDENTITY_REJECTED` are no longer flattened into an indistinguishable wake.
  Xiuluo and Wubei consume that terminal in `RETURN_HOME`: success performs start-map verification only;
  failure/rejection enters the existing Cloud return-item/navigation fallback and never re-waits the armed branch.
- Client replay ownership now carries a UUID token plus lifecycle generation. Queue admission, pre-callback,
  and every existing `BagService` direct-input checkpoint re-evaluate the exact replay lifecycle through
  `InputActionScope.checkpoint() -> InputActionRequest.checkDetailedSafety() -> externalSafetyReason`.
  Stop/reset/replacement/runner close therefore cancel queued or running replay fail-closed without copying the
  bag algorithm.
- Async completion is identity-conditional: an old replay can complete only its exact token/generation and cannot
  clear a replacement run's retained slot. Per-window replay executors are removed and shut down after terminal
  completion or lifecycle clear.
- Replaced Xiuluo attempts clear an old unbound expected-enter claim for the same business run. Refreshing the
  same exact attempt preserves the claim. Allowed expected sources are frozen to a successful `local-template`
  click or successful `cloud-fallback` click.
- Local fast-exit mechanics run for every combat generation. A fast mechanical hit first settles the exact local
  generation and prevents duplicate ordinary-absent settlement. Only a generation carrying an exact expected
  enter claim publishes `FAST_EXPECTED_COMBAT_EXIT` or advances Cloud business state; incidental combat settles
  locally with zero expected event and zero return-home transition.

### Repair #2 changed files

Client:

- `src/main/java/com/bot/dhxy/input/InputSequences.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionRequest.java`
- `src/main/java/com/bot/dhxy/window/model/WindowRetainedReturnHomeReplay.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/observation/DeferredReturnHomeReplayCoordinator.java`
- `src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java`
- `src/test/java/com/bot/dhxy/input/action/InputActionReplayLifecycleCheckpointTest.java`
- `src/test/java/com/bot/dhxy/window/runtime/LocalRunnerIdentityContractTest.java`
- `src/test/java/com/bot/dhxy/window/observation/FastExpectedCombatExitProbeTest.java`
- `src/test/java/com/bot/dhxy/window/observation/LocalCombatSignalMechanicsTest.java`
- `src/test/java/com/bot/dhxy/window/observation/DeferredReturnHomeReplayCoordinatorContractTest.java`

Cloud:

- `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudFastExpectedCombatExitCoordinator.java`
- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
- `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudFastExpectedCombatExitArmLifecycleTest.java`

### Verification

- Client named tests:
  `mvn -q "-Dtest=FastExpectedCombatExitProbeTest,LocalCombatSignalMechanicsTest,LocalRunnerIdentityContractTest,DeferredReturnHomeReplayCoordinatorContractTest,InputActionReplayLifecycleCheckpointTest,XiuluoKandaProductionChainContractTest" test`
  -> exit `0`.
- Cloud named tests:
  `mvn -q "-Dtest=CloudFastExpectedCombatExitArmLifecycleTest,FastExpectedExitGateTest,FastExpectedExitObservationContractTest" test`
  -> exit `0`.
- Client `mvn -q -DskipTests compile` -> exit `0`.
- Cloud `mvn -q compile` -> exit `0`.
- Shared protocol SHA-256 comparison -> `7/7` byte-identical.
- `git diff --check` on the Repair #2 client and Cloud write sets -> exit `0` (client emitted only existing
  CRLF conversion warnings).
- No runtime, UI, capture, physical input, or Git mutation was performed.

State: `REPAIR #2 WHOLE-CARD SOURCE+TEST RE-DELIVERED / AWAITING_PARENT_REVIEW`; owner retained. This worker
does not self-approve.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-2 WHOLE-CARD-SOURCE-TEST-REDELIVERED AWAITING-PARENT-REVIEW NAMED-TESTS-PASS DUAL-COMPILE DTO-7-OF-7-BYTE-IDENTICAL INCIDENTAL-LOCAL-FAST-ZERO-BUSINESS-EDGE ASYNC-LIFECYCLE-CHECKPOINT-SAFE OWNER-RETAINED NO-RUNTIME 2026-07-23 -->

## Parent source review #4 - Repair #2 rejected

### P1-1 - Wubei replay failure does not force the promised Cloud return fallback

- `WubeiTask.returnHomeAfterCombatOrContinueSpecialTarget(...)` correctly consumes the typed replay
  terminal and computes `ReplayReturnAction.CLOUD_FALLBACK`, but that branch only logs the terminal.
- The actual return-item fallback remains guarded by `!currentRoundChainedCombatExpected`. A replay
  armed from `chained-combat-fast-miss-return-unverified` preserves
  `currentRoundChainedCombatExpected=true`; after a `FAILED` or `IDENTITY_REJECTED` replay terminal,
  execution therefore skips the return fallback and re-enters the chained-target tracker logic.
- This violates the Repair #2 contract that a failed/rejected retained return replay transfers
  control to the existing Cloud return-item/navigation fallback. It may retry/continue combat after
  the chain had already selected return-home.
- `CloudFastExpectedCombatExitArmLifecycleTest` currently tests only
  `decideReplayReturnAction(...)`; it does not execute the Wubei task branch, so the green test does
  not cover this regression.

Repair requirement:

- In Wubei, `ReplayReturnAction.CLOUD_FALLBACK` must force the appropriate existing return-home
  fallback irrespective of the stale chained-combat flag, without changing ordinary chained-combat
  decisions.
- Add a behavioral task-path test covering both `FAILED` and `IDENTITY_REJECTED` from an armed
  chained return correction, and prove success remains verify-only with no duplicate return action.

State: `REPAIR REQUIRED / P0-P1-P2=0/1/0`; owner retained for Repair #3.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 PARENT-REVIEW-4 REPAIR-3-REQUIRED P0-0-P1-1-P2-0 WUBEI-CHAINED-REPLAY-FAILURE-SKIPS-CLOUD-RETURN-FALLBACK OWNER-RETAINED 2026-07-23 -->

## Repair #3 source + test re-delivery

### Delivered behavior

- `WubeiTask.returnHomeAfterCombatOrContinueSpecialTarget(...)` now treats
  `ReplayReturnAction.CLOUD_FALLBACK` as an explicit override of the stale
  `currentRoundChainedCombatExpected` gate. `FAILED` and `IDENTITY_REJECTED` therefore enter the
  existing Cloud `useReturnItemAndVerifyStartMap(...)` path with source
  `local-return-replay-terminal-fallback`; they cannot fall through into the chained tracker.
- Ordinary chained-combat behavior is unchanged when no replay failure terminal exists.
- `SUCCEEDED` remains on the existing verify-only branch and never calls the return-item path.

### Repair #3 changed files

Cloud:

- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
- `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wubei/WubeiTaskTrackerTurnContractTest.java`

### Behavioral proof

- The new `chainedArmedReplayTerminalForcesFallbackWhileSuccessRemainsVerifyOnly` test invokes the
  production `WubeiTask.returnHomeAfterCombatOrContinueSpecialTarget(...)` method with an exact-run
  terminal consumed from the real `CloudFastExpectedCombatExitCoordinator`.
- Chained armed `FAILED` and `IDENTITY_REJECTED` both reach the production `useReturnItem(...)`
  mechanical boundary and perform zero full/fast chained-tracker reads.
- Chained armed `SUCCEEDED` reaches the position verify-only branch, does not reach
  `useReturnItem(...)`, and performs zero full/fast chained-tracker reads.
- Existing ordinary chained fast hit/miss/chain-end behavior was executed in the same named run.

### Verification

- Cloud focused task-path plus existing replay/observation contracts:
  `mvn -q "-Dtest=WubeiTaskTrackerTurnContractTest#chainedArmedReplayTerminalForcesFallbackWhileSuccessRemainsVerifyOnly+chainedFirstReadSeedsFastCacheAndLaterHitOrMissNeverFullRereads,CloudFastExpectedCombatExitArmLifecycleTest,FastExpectedExitGateTest,FastExpectedExitObservationContractTest" test`
  -> exit `0`.
- Client Repair #2 named family:
  `mvn -q "-Dtest=FastExpectedCombatExitProbeTest,LocalCombatSignalMechanicsTest,LocalRunnerIdentityContractTest,DeferredReturnHomeReplayCoordinatorContractTest,InputActionReplayLifecycleCheckpointTest,XiuluoKandaProductionChainContractTest" test`
  -> exit `0`.
- Client `mvn -q -DskipTests compile` -> exit `0`.
- Cloud `mvn -q compile` -> exit `0`.
- Shared protocol SHA-256 comparison -> `7/7` byte-identical.
- Both changed Cloud files contain zero trailing-whitespace lines.
- An exploratory whole-class run of `WubeiTaskTrackerTurnContractTest` is not claimed green: two
  unrelated existing tests currently fail because the tracker local-service test client is
  unavailable and an old `InputAction.moveMouse(...)` source marker no longer matches current code.
  The Repair #3 production-path method and its ordinary chained companion are independently green.
- No runtime, UI, capture, physical input, or Git mutation was performed.

State: `REPAIR #3 WHOLE-CARD SOURCE+TEST RE-DELIVERED / AWAITING_PARENT_REVIEW`; owner retained. This
worker does not self-approve.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-3 WHOLE-CARD-SOURCE-TEST-REDELIVERED AWAITING-PARENT-REVIEW WUBEI-CHAINED-REPLAY-TERMINAL-FALLBACK TASK-PATH-BEHAVIOR-PASS ORDINARY-CHAINED-UNCHANGED NAMED-TESTS-PASS DUAL-COMPILE DTO-7-OF-7-BYTE-IDENTICAL OWNER-RETAINED NO-RUNTIME 2026-07-23T23:30:52-04:00 -->

## Parent source review #5 - Repair #3 not yet accepted

### P2-1 - relevant Wubei contract class is still red

- Parent reran the complete relevant class:
  `mvn -q "-Dtest=WubeiTaskTrackerTurnContractTest" test`.
- Result: `Tests run: 5, Failures: 1, Errors: 1`.
- `firstGreenClickParkTerminalAndNoLegacyTrackerTransportStayFrozen` still asserts the obsolete
  source marker `InputAction.moveMouse(click.x, click.y)`.
- `postAcceptCallerBindsExactTurnContextAndNeverRetriesAnyTerminal` uses an obsolete fixture with no
  task-tracker local-service client and now terminates with
  `TaskFatalException: task-tracker local-service client is unavailable`.
- Repair #3's focused method is green and its production branch is directionally correct, but
  selecting only passing methods from a red directly-related test class is not an acceptable final
  gate. Determine whether each old assertion remains a valid current contract; update valid
  fixtures/assertions to the current Cloud/local-service boundary and remove only genuinely
  superseded source-string checks. Do not change production business behavior merely to satisfy an
  obsolete test.

Repair requirement:

- `WubeiTaskTrackerTurnContractTest` must pass as a complete class.
- Retain the new FAILED, IDENTITY_REJECTED and SUCCEEDED production-path coverage.
- Rerun the complete Repair #2/#3 named families and both compiles.

State: `REPAIR REQUIRED / P0-P1-P2=0/0/1`; owner retained for Repair #4.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 PARENT-REVIEW-5 REPAIR-4-REQUIRED P0-0-P1-0-P2-1 WUBEI-RELEVANT-CONTRACT-CLASS-RED OWNER-RETAINED 2026-07-24 -->

## Repair #4 test-contract re-delivery

### Test contract repairs

- Production business code was not changed in Repair #4.
- `postAcceptCallerBindsExactTurnContextAndNeverRetriesAnyTerminal` now constructs the current
  `CloudTaskTrackerLocalServiceClient` and injects it into `TaskTrackerPanelService`. Its scripted
  command port returns the current typed `TASK_TRACKER_PANEL_ABSENT` local-service result for the
  successful terminal.
- The same post-accept test now freezes current fail-closed semantics: `COMPLETED` returns an empty
  typed panel; `FAILED`, `STOPPED`, and `UNCERTAIN` propagate `TaskFatalException`. Every terminal
  still emits exactly one action/UUID against the exact device/window and never retries.
- The obsolete `InputAction.moveMouse(...)` source-string assertion was removed. The replacement
  invokes the production `clickTaskTrackerGreen(...)` task path with an exact bound
  `TaskExecutionContext` and asserts the recorded single action contains the ordered atomic bundle
  `MOVE_MOUSE -> WAIT(120ms) -> CLICK_LEFT(300ms)`, with identical move/click coordinates.
- Repair #3's chained armed `FAILED`, `IDENTITY_REJECTED`, and `SUCCEEDED` production-path coverage
  remains in the same class.

### Repair #4 changed files

Cloud test only:

- `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wubei/WubeiTaskTrackerTurnContractTest.java`

### Verification

- Parent-required complete related class:
  `mvn -q "-Dtest=WubeiTaskTrackerTurnContractTest" test`
  -> exit `0`; Surefire: `tests=5 failures=0 errors=0 skipped=0`.
- Cloud complete Repair #2/#3 named family:
  `mvn -q "-Dtest=WubeiTaskTrackerTurnContractTest,CloudFastExpectedCombatExitArmLifecycleTest,FastExpectedExitGateTest,FastExpectedExitObservationContractTest" test`
  -> exit `0`.
- Client complete Repair #2 named family:
  `mvn -q "-Dtest=FastExpectedCombatExitProbeTest,LocalCombatSignalMechanicsTest,LocalRunnerIdentityContractTest,DeferredReturnHomeReplayCoordinatorContractTest,InputActionReplayLifecycleCheckpointTest,XiuluoKandaProductionChainContractTest" test`
  -> exit `0`.
- Client `mvn -q -DskipTests compile` -> exit `0`.
- Cloud `mvn -q compile` -> exit `0`.
- Shared protocol SHA-256 comparison -> `7/7` byte-identical.
- Changed test file trailing-whitespace scan -> `0`.
- No runtime, UI, capture, physical input, production business edit, or Git mutation was performed.

State: `REPAIR #4 WHOLE-CARD SOURCE+TEST RE-DELIVERED / AWAITING_PARENT_REVIEW`; owner retained. This
worker does not self-approve.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-4 WHOLE-CARD-SOURCE-TEST-REDELIVERED AWAITING-PARENT-REVIEW WUBEI-CONTRACT-CLASS-5-OF-5-GREEN CURRENT-LOCAL-SERVICE-FIXTURE ATOMIC-INPUT-BEHAVIOR-CONTRACT REPAIR-2-3-NAMED-FAMILIES-PASS DUAL-COMPILE DTO-7-OF-7-BYTE-IDENTICAL TEST-ONLY OWNER-RETAINED NO-RUNTIME 2026-07-24T00:22:17-04:00 -->

## Parent source review #6 - Repair #4 accepted

- Parent independently reviewed the Repair #4 fixture changes and the complete Repair #1-#4
  production/test write set. Final findings: `P0/P1/P2=0/0/0`.
- Local mechanical fast-exit now applies to expected and incidental combat generations. Incidental
  combat converges only local state and emits zero expected business edge; only exact successful
  `local-template` or `cloud-fallback` enter claims may publish
  `FAST_EXPECTED_COMBAT_EXIT`.
- Deferred return replay is exact-run/token/generation fenced, cancellable at every existing direct
  input checkpoint, identity-conditionally completed, and cannot clear or execute into a
  replacement run.
- Replay success remains verify-only. Failure and identity rejection transfer Xiuluo and Wubei,
  including Wubei chained-return correction, into the existing Cloud return fallback.
- Parent verification:
  - Cloud named family including complete `WubeiTaskTrackerTurnContractTest`: exit `0`.
  - Client Repair named family: exit `0`.
  - Client and Cloud compile: exit `0`.
  - Shared protocol SHA-256: `7/7` byte-identical.
  - `git diff --check` for the Repair #3/#4 Cloud files: exit `0`.
- No runtime, UI, capture or physical input was executed. Fresh-runtime acceptance remains required
  before claiming end-to-end runtime completion.

State: `SOURCE+TEST SOURCE REVIEW PASSED / P0-P1-P2=0/0/0 / OWNER RELEASED`.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 PARENT-REVIEW-6 SOURCE-TEST-SOURCE-REVIEW-PASSED P0-0-P1-0-P2-0 OWNER-RELEASED NAMED-FAMILIES-PASS DUAL-COMPILE DTO-7-OF-7-BYTE-IDENTICAL FRESH-RUNTIME-REQUIRED 2026-07-24 -->

## Fresh-runtime startup repair #5 - Spring constructor selection

- Fresh-runtime evidence at `2026-07-24 00:35:47-00:35:49`: Client exited before UI/task startup
  while creating `WholeTaskRuntimeLocalOperationExecutor`.
- Root cause: Repair #4 added the production
  `DeferredReturnHomeReplayCoordinator` constructor while retaining the package-private test
  compatibility constructor. With two constructors and no explicit injection candidate, Spring
  searched for a no-arg constructor and failed with
  `WholeTaskRuntimeLocalOperationExecutor.<init>() / No default constructor found`.
- Repair: mark the public five-argument production constructor `@Autowired`. No business,
  observation, input, replay or Cloud behavior changed.
- Regression: `WholeTaskRuntimeLocalOperationExecutorSpringWiringTest` asserts exactly one
  autowired constructor and its complete production dependency signature.
- Verification:
  - `WholeTaskRuntimeLocalOperationExecutorSpringWiringTest`,
    `XiuluoKandaProductionChainContractTest`, `LocalRunnerIdentityContractTest`: exit `0`.
  - Client `mvn -q -DskipTests compile`: exit `0`.
- No runtime/UI/capture/input was executed by the parent. A fresh Client restart is required to
  verify successful `ApplicationContext` startup before resuming end-to-end acceptance.

State: `STARTUP-WIRING REPAIR PASSED / FRESH RUNTIME RESTART REQUIRED`.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 STARTUP-REPAIR-5 SPRING-CONSTRUCTOR-SELECTION-P1 FIXED FOCUSED-TESTS-PASS CLIENT-COMPILE-PASS FRESH-RUNTIME-RESTART-REQUIRED 2026-07-24 -->

## Fresh-runtime parent review #7 - Repair #6 required

### P1-1 - pause/resume leaves the leader observation runner stopped behind a Cloud long wait

- Fresh runtime evidence for leader `67555 / hwnd-D1A1078`:
  - `00:53:31.461`: the exact local-kanda attempt clicked the enter-battle option.
  - `00:53:32.335`: the user pause checkpoint immediately suspended the leader observation runner.
  - `00:54:02.232`: the user resumed all five windows.
  - The four member observation runners restarted at `00:54:02`, but the leader runner did not restart until
    `00:55:51.761`, after the Cloud `WAIT_TRACKER_SHORTCUT_PATHING` long wait reached its 180-second bound.
- During that gap no leader `COMBAT_STATE_CHANGED` edge was sampled or published. Cloud remained parked in
  `WAIT_TRACKER_SHORTCUT_PATHING`; consequently the leader never entered its existing `AutoCombatService`
  maintenance path (no leader `Alt+8`) and no combat-exit edge advanced the Xiuluo round after battle.
- Root cause: `WindowTurnLoop.requestPause()` suspends observation immediately but does not cancel or otherwise
  wake an in-flight `turnClient.exchange(...)`. `requestResume()` only sets `resumeRequested`; the loop cannot
  validate the HWND and restart the retained runner until that blocking exchange returns. The existing
  `userPauseStopsObservationAndKeepsItStoppedUntilResumeValidation` test covers suspension only and has no
  blocked-exchange resume assertion.

Repair requirement:

- A pause must wake/cancel the in-flight long-wait exchange without terminating, replacing, or resetting the
  acknowledged loop.
- The pause checkpoint must still be published exactly once. Resume must revalidate the exact acknowledged
  HWND and restart the same retained `WindowObservationRunner` before normal Cloud turn waiting resumes.
- Preserve observation sequence, interests, unacknowledged typed facts, exact task-run identity, stop behavior,
  and all existing input checkpoints.
- Add a blocking transport contract proving `pause -> resume` restarts the leader runner promptly rather than
  waiting for the Cloud long-wait timeout, then proves a combat edge sampled after resume reaches the existing
  observation transport. Do not add a second combat or auto-battle state machine.

State: `REPAIR REQUIRED / P0-P1-P2=0/1/0`; owner retained for Repair #6.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 FRESH-RUNTIME-PARENT-REVIEW-7 REPAIR-6-REQUIRED P0-0-P1-1-P2-0 PAUSE-RESUME-BLOCKED-EXCHANGE-LEADER-OBSERVER-GAP OWNER-RETAINED 2026-07-24T01:05:00-04:00 -->

## Fresh-runtime Repair #6 source + test re-delivery

### Delivered behavior

- `WindowTurnLoop.requestPause()` now interrupts only a currently admitted Cloud
  `turnClient.exchange(...)`. It does not interrupt local action work or add a second physical-input
  cancellation policy.
- An `INTERRUPTED` transport result caused by that exact pause boundary is consumed as loop control:
  the worker clears only that control interrupt, retains the acknowledged loop and all uncertain
  transport state, and publishes the pause-bearing checkpoint turn exactly once.
- A pause that wins after pre-pause request construction but before transport admission sends no stale
  normal turn; the next iteration publishes the pause checkpoint.
- Resume still requires the live metadata to match the acknowledged title/HWND/PID. After successful
  validation it restarts the same retained `WindowObservationRunner` before entering the next normal
  Cloud long wait. The runner object, observer sequence, interests, unacknowledged typed events,
  sampler lineage and start-request/task-run identity are not replaced or reset.
- Hard stop, graceful stop, task replacement, input checkpoints and Cloud business logic are unchanged.
  No second combat or auto-battle state machine was added.

### Repair #6 changed files

- `src/main/java/com/bot/dhxy/cloud/turn/WindowTurnLoop.java`
- `src/test/java/com/bot/dhxy/cloud/turn/WindowTurnLoopObservationContractTest.java`

### Blocking transport contract

- `blockedExchangePauseResumePromptlyRestartsRetainedRunnerAndDeliversCombatEvent` uses a transport
  whose acknowledged normal turn blocks indefinitely.
- The test proves pause interrupts that old wait, emits one and only one pause-bearing request, and
  retains the same stopped runner.
- The resumed normal exchange then blocks again; while it is blocked the exact retained runner is
  already running. A post-resume `IN_COMBAT` typed event is delivered through the existing observation
  client with the unchanged `start-pause-resume` task-run identity.

### Verification

- Focused loop lifecycle contracts:
  `mvn -q "-Dtest=WindowTurnLoopObservationContractTest,WindowTurnLoopContractTest" test`
  -> exit `0`; Surefire `8 + 4` tests, `0` failures, `0` errors.
- Client compile:
  `mvn -q -DskipTests compile`
  -> exit `0`.
- `git diff --check` for the two Repair #6 source/test files -> exit `0` with only the existing
  LF-to-CRLF working-copy warning.
- An exploratory broader runner/replay family was not claimed green: its Repair #6 loop tests passed,
  but the unrelated existing
  `WindowObservationRunnerContractTest.samplerMapsExactDialogInterestIdentityAndEmitsOneTypedClear`
  fixture remains red because it expects `attempt-dialog` while the current dialog fact contains
  `null`. Repair #6 did not alter that dialog mapping or fixture.
- No Cloud source, runtime, UI, capture, physical input or Git mutation was performed.

State: `REPAIR #6 SOURCE+TEST RE-DELIVERED / AWAITING_PARENT_REVIEW`; owner retained. This
implementation does not self-approve.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-6 SOURCE-TEST-REDELIVERED AWAITING-PARENT-REVIEW BLOCKED-EXCHANGE-PAUSE-CANCEL SAME-RUNNER-RESUME-BEFORE-LONG-WAIT POST-RESUME-IN-COMBAT-DELIVERED PAUSE-CHECKPOINT-ONCE FOCUSED-12-OF-12-GREEN CLIENT-COMPILE-PASS OWNER-RETAINED NO-RUNTIME 2026-07-24T01:08:00-04:00 -->

## Parent source review #8 - Repair #6 rejected

### P1-1 - pause-bearing turn still carries the normal long-wait timeout

- `WindowTurnLoop.exchangeOnce(...)` still places the configured normal `waitTimeoutMs` into every
  `TurnRequest`, including the pause-bearing checkpoint request.
- The real `CloudTurnHttpHandler` uses `request.waitTimeoutMs()` for every non-start request; neither it nor
  `CloudTurnExchange` gives `pauseRequested=true` an immediate-return path.
- Therefore Repair #6 may interrupt the old Cloud long wait only to enter a new pause-bearing long wait for
  the same configured bound. The retained observation runner remains suspended until that request returns, so
  the fresh-runtime failure can persist.
- `PauseResumeBlockingTurnClient` masks this because its pause branch returns immediately regardless of the
  request timeout; it does not model the production handler.

Repair requirement:

- A pause-bearing checkpoint turn must carry `waitTimeoutMs=0`; normal and resumed turns keep the configured
  long-wait value.
- Strengthen the blocking transport contract so every positive-wait request blocks and only the zero-wait
  pause-bearing request returns immediately. Assert the exact pause request carries zero and the resumed normal
  request restores the configured positive wait.
- Retain all Repair #6 identity, sequence, uncertain-outcome, single-checkpoint and same-runner guarantees.

State: `REPAIR REQUIRED / P0-P1-P2=0/1/0`; owner retained for Repair #7.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 PARENT-REVIEW-8 REPAIR-7-REQUIRED P0-0-P1-1-P2-0 PAUSE-CHECKPOINT-MUST-BE-ZERO-WAIT PRODUCTION-HANDLER-PARITY OWNER-RETAINED 2026-07-24T01:15:00-04:00 -->

## Fresh-runtime Repair #7 source + test re-delivery

### Delivered behavior

- `WindowTurnLoop.exchangeOnce(...)` now derives the request wait from the exact turn metadata:
  a pause-bearing checkpoint carries `waitTimeoutMs=0`; every normal, start, resumed and stop-only
  request retains the configured positive long-wait value unless it is also pause-bearing.
- This makes the Repair #6 cancellation useful against the production handler: after the old normal
  long wait is interrupted, the pause checkpoint cannot enter another 180-second wait.
- All Repair #6 lifecycle guarantees remain unchanged: the acknowledged loop is not terminated,
  replaced or reset; pause is published once; resume validates the acknowledged title/HWND/PID,
  restarts the same retained runner before normal waiting, and preserves sequence, interests,
  unacknowledged typed events and exact task-run identity.

### Repair #7 changed files

- `src/main/java/com/bot/dhxy/cloud/turn/WindowTurnLoop.java`
- `src/test/java/com/bot/dhxy/cloud/turn/WindowTurnLoopObservationContractTest.java`

### Production-parity blocking contract

- `PauseResumeBlockingTurnClient` no longer returns merely because
  `window.pauseRequested=true`.
- Its acknowledged start request must retain a positive wait. Every later positive-wait request
  blocks indefinitely. Only a zero-wait request carrying the pause checkpoint returns immediately;
  zero-wait on a non-pause request fails the test.
- `blockedExchangePauseResumePromptlyRestartsRetainedRunnerAndDeliversCombatEvent` now asserts:
  the exact pause request has wait `0`, the resumed normal request restores a positive configured
  wait, that resumed request is blocked, the same retained runner is already running, and the
  post-resume `IN_COMBAT` typed event reaches the existing observation transport.

### Verification

- Focused lifecycle contracts:
  `mvn -q "-Dtest=WindowTurnLoopObservationContractTest,WindowTurnLoopContractTest" test`
  -> exit `0`; Surefire `8 + 4` tests, `0` failures, `0` errors.
- Client compile:
  `mvn -q -DskipTests compile`
  -> exit `0`.
- `git diff --check` for the two Repair #7 source/test files -> exit `0` with only the existing
  LF-to-CRLF working-copy warning.
- No Cloud source, runtime, UI, capture, physical input or Git mutation was performed.

State: `REPAIR #7 SOURCE+TEST RE-DELIVERED / AWAITING_PARENT_REVIEW`; owner retained. This
implementation does not self-approve.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-7 SOURCE-TEST-REDELIVERED AWAITING-PARENT-REVIEW PAUSE-ZERO-WAIT NORMAL-RESUME-POSITIVE-WAIT PRODUCTION-PARITY-BLOCKING-CONTRACT SAME-RUNNER-RESUME POST-RESUME-IN-COMBAT-DELIVERED FOCUSED-12-OF-12-GREEN CLIENT-COMPILE-PASS OWNER-RETAINED NO-RUNTIME 2026-07-24T01:20:00-04:00 -->

## Parent Source Review #9 - Repair #7 PASSED (2026-07-24)

### Fresh-runtime evidence reviewed

- Leader `67555 / hwnd-D1A1078` entered battle at `00:53:31`, then pause interrupted the
  observation exchange at `00:53:32`.
- Resume was requested at `00:54:02`, but the leader observation runner did not restart until
  `00:55:51`, after the Cloud `WAIT_TRACKER_SHORTCUT_PATHING` 180-second wait released.
- During that gap the leader produced no `COMBAT_STATE_CHANGED`; therefore the existing
  `XiuluoTaskV2.waitTrackerShortcutPathing()` auto-combat branch could neither refill `Alt+8`
  nor observe combat exit. The member runners resumed normally, which explains why only members
  sent `Alt+8`.

### Parent findings

- P0: `0`
- P1: `0`
- P2: `0`

Repair #7 closes the production-parity gap found in Review #8:

- a pause-bearing exchange is sent with `waitTimeoutMs=0`, so it cannot replace the interrupted
  long poll with another long poll;
- ordinary resumed exchanges retain the configured positive wait;
- resume restarts the same retained per-window observation runner before the next ordinary Cloud
  wait;
- stop remains dominant over pause, and no second auto-combat state machine was introduced.

### Parent verification

- `mvn -q "-Dtest=WindowTurnLoopObservationContractTest,WindowTurnLoopContractTest,LocalCombatSignalMechanicsTest,LocalRunnerIdentityContractTest,DeferredReturnHomeReplayCoordinatorContractTest,InputActionReplayLifecycleCheckpointTest,XiuluoKandaProductionChainContractTest" test`
  -> exit `0`.
- `mvn -q -DskipTests compile`
  -> exit `0`.
- No Cloud source change was required; no runtime, UI, capture or physical input was performed.

State: `SOURCE+TEST SOURCE REVIEW PASSED / P0-P1-P2=0/0/0 / OWNER RELEASED /
FRESH RUNTIME REQUIRED`.

Fresh acceptance must show: leader runner resumes within seconds after pause/resume; leader
receives `IN_COMBAT` and the existing auto-combat path can refill `Alt+8`; combat exit advances
the task without a `WAIT_TRACKER_SHORTCUT_PATHING` 180-second timeout.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 PARENT-REVIEW-9 SOURCE-TEST-SOURCE-REVIEW-PASSED P0-0-P1-0-P2-0 OWNER-RELEASED PAUSE-ZERO-WAIT SAME-RUNNER-RESUME LEADER-AUTO8-AND-COMBAT-EXIT-PATH-RESTORED FOCUSED-FAMILY-PASS CLIENT-COMPILE-PASS FRESH-RUNTIME-REQUIRED NO-RUNTIME 2026-07-24 -->

## Fresh-runtime Parent Review #10 - Repair #8 REQUIRED (2026-07-24)

Fresh runtime invalidates the combat-exit portion of Review #9 while confirming its pause/resume
and `Alt+8` portion:

- `01:16:22` all five observation runners resumed immediately.
- `01:16:23` leader `67555 / hwnd-1D315DA` successfully sent `Alt+8`.
- `01:16:24` the leader local runner closed the exact expected combat generation and cleared claim
  `aa25fb9d-2112-4f23-bf40-a882e10cf903`.
- Cloud nevertheless remained in repeated `WAIT_COMBAT / waiting for combat state`.
- Cloud later logged
  `discard stale expected combat-exit signal ... pendingBattleCount=1 ... armedAtMs=0`.

### P1 finding

`CloudFastExpectedCombatExitCoordinator` marks its binding lifecycle `ARMED`, but never installs the
same exact `expectedWaitId + combatGeneration` into `BattleRadarService`. The local exact event is
therefore unable to establish/consume the Cloud radar pending identity. A later full-radar exit is
recorded without an armed exact wait and is discarded as stale, leaving Xiuluo permanently in
`WAIT_COMBAT`.

### Required repair

- At the accepted exact fast-exit boundary, bind the event's exact `expectedWaitId` and
  `combatGeneration` into `BattleRadarService` before recording the local exit.
- Preserve all existing tenant/device/window/observation/business-run/task/claim fences and event
  idempotency. Do not accept incidental or mismatched events.
- Do not change navigation, NPC/dialog/template behavior, return-home ordering, or add a second
  combat state machine.
- Add a production-path contract proving a local exact exit advances from Cloud `IN_COMBAT` with
  no prior legacy radar arm, while mismatched identity remains a no-op and duplicate delivery is
  idempotent.
- Re-run the relevant Cloud fast-exit/AutoCombat/Xiuluo named families and Cloud compile.

State: `REPAIR REQUIRED / P0-P1-P2=0/1/0`; owner assigned for Repair #8.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 PARENT-REVIEW-10 FRESH-RUNTIME-FAILED REPAIR-8-REQUIRED P0-0-P1-1-P2-0 CLOUD-EXACT-WAIT-NOT-INSTALLED RADAR-ARMED-AT-ZERO WAIT-COMBAT-STUCK OWNER-ASSIGNED 2026-07-24 -->

## User Contract Correction - Repair #8 SUPERSEDED (2026-07-24)

The prior Repair #8 direction ("install an exact arm into Cloud BattleRadarService") is cancelled.
No source was written under that direction.

### Authoritative simple one-way contract

1. Cloud issues the current navigation/click action and then parks the Xiuluo/Wubei task phase.
2. The per-window Client runner is the sole authority for mechanical combat state:
   - after locally confirmed combat entry, publish one exact-run `IN_COMBAT` event;
   - after local fast or ordinary exit detection, publish one exact-run `EXITED` event.
3. Cloud only validates envelope identity/idempotency, updates the current task phase from that event,
   and wakes the parked task. Without a Client event, Cloud remains parked.

### Forbidden for Xiuluo/Wubei combat transition

- no Cloud screenshot/radar decision;
- no Cloud `armedAtMs`, expected-exit pending, battle-count or generation reconstruction;
- no second consumption of the same exit through `BattleRadarService`;
- no fallback that lets Cloud infer combat entry/exit;
- no deletion of a valid current-run Client event because another Cloud combat store disagrees.

Transport authentication, exact tenant/device/window/observation/business-task identity and event-id
deduplication remain infrastructure fences, not a second business state machine.

### Required implementation and tests

- Client runner publishes transition events only on local state edges, not every sample.
- Cloud observation ingress routes accepted current-run transition events directly to the existing
  parked-task wake mechanism.
- Xiuluo and Wubei `WAIT_COMBAT` consume that one event source and transition directly; their path
  must not call Cloud `BattleRadarService` to decide or consume combat exit.
- Existing local `Alt+8` action remains local execution; no new Cloud detector is introduced.
- Tests must prove: no event means indefinitely parked; exact `IN_COMBAT` wakes/updates once; exact
  `EXITED` wakes/advances once; duplicate/stale/wrong-window events are no-ops; no Radar method is
  touched by the Xiuluo/Wubei transition path.

State: `PLAN-CONTRACT CORRECTED / REPAIR #8 IMPLEMENTING`; owner assigned.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 USER-CONTRACT-CORRECTED REPAIR-8-SUPERSEDED SIMPLE-ONE-WAY CLIENT-RUNNER-SOLE-COMBAT-AUTHORITY CLOUD-PARK-UNTIL-EXACT-EVENT NO-CLOUD-RADAR NO-SECOND-CONSUMER OWNER-ASSIGNED 2026-07-24 -->

## Git-verified implementation baseline for Repair #8 (2026-07-24)

Parent re-audited repository history instead of extending the current coordinator design:

- `59b85e0bff22a1e282dfc3b592e47844646fbda9` is the latest committed client snapshot containing
  the user-validated local combat mechanics.
- `9aa987d15ab1a088ad9cf0eb64e2565d34a91714` (`修罗云端能跑`) is the commit where the Xiuluo
  local/cloud enter-battle race and `xiuluo_enter_battle_kanda2.png` path are directly evidenced.
- The current thin-client work already restored the local `kanda2` ROI/template constants from
  `59b85e0b`, but did not preserve the complete control flow.

### Semantics that must be ported, not redesigned

1. Xiuluo enter-battle retains the verified two producers:
   - local `kanda2` small-ROI matcher;
   - Cloud stop-static dialog verdict.
   Both carry the same attempt identity. The first valid prepared result wins the one-shot attempt
   claim; the later result becomes stale and performs no input.
2. Local combat detection retains the committed `BattleRadarService.checkAndSyncCombatState()`
   mechanics: three positive template stages, fail-closed capture behavior, repeated exit misses,
   and the readable-minimap exit confirmation. The fast avatar-diff path remains an additional early
   exit detector, not a replacement for the ordinary detector.
3. Only state-edge results cross the wire: exact-run `IN_COMBAT` and `COMBAT_EXITED`. Cloud parks
   and consumes these events directly; it does not reconstruct the detector's counters, arm state,
   battle count or generation.

Do not restore the old local `XiuluoTaskV2`, `WubeiTask`, `AutoCombatService` business orchestration,
or a second task state machine. Reuse/port only the verified mechanical detector semantics into the
current per-window observation runner and translate their edges into the existing observation
protocol.

State: `GIT BASELINE LOCKED / REPAIR #8 IMPLEMENTING`; owner retained.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 GIT-BASELINE-LOCKED REPAIR-8-IMPLEMENTING BASELINE-59B85E0B ORIGIN-9AA987D1 DUAL-ENTER-PRODUCER FIRST-VALID-WINS LOCAL-COMBAT-EDGE-AUTHORITY CLOUD-DIRECT-CONSUMER NO-LOCAL-BUSINESS-RESTORE OWNER-RETAINED 2026-07-24 -->

## Repair #8 canonical SOURCE+TEST delivery (2026-07-24 02:13 -04:00)

State: `AWAITING_PARENT_REVIEW`; owner retained; this delivery does not self-approve.

### Baseline method mapping

- Client `59b85e0b` `BattleRadarService.checkAndSyncCombatState()`:
  - stage 1 `flag_battle.png`, ROI `(974,630,51,20)`, threshold `0.85`;
  - stage 2 `zhaohuan.png OR chehui.png`, ROI `(927,302,100,225)`, threshold `0.80`;
  - stage 3 `nu.png AND yuan.png`, ROI `(456,62,123,39)`, threshold `0.80`;
  - ordinary exit requires two complete misses plus readable minimap coordinates;
  - unavailable capture/template/coordinate analysis remains fail-closed.
- Client `59b85e0b` fast avatar-diff semantics remain an additional early exit, not a replacement for ordinary exit.
- Existing current-architecture local kanda / Cloud stop-static same-attempt first-valid-wins path was preserved;
  no `XiuluoTaskV2`, `WubeiTask` or `AutoCombatService` business class was restored from an old commit.

### Delivered behavior

1. `WindowObservationSampler` is the per-window mechanical combat authority. It binds the current exact enter
   claim on first visible combat, emits deterministic exact-run `IN_COMBAT`, and emits deterministic
   `COMBAT_EXITED` on either the fast avatar edge or the ordinary 59b exit gate.
2. Incidental combat still converges locally but cannot publish Xiuluo/Wubei business edges without the exact
   current claim.
3. Ordinary exit uses a fresh `coordinate-strip` result accepted at or after the first miss. Cloud only answers
   stateless `readable` / `unreadable`; all counters and state transitions remain in the Client runner.
4. Cloud ingress fences tenant/device/window/observation-run/business-run/task/claim/generation and existing
   event-id deduplication, then directly updates the bound `GameContext` and publishes
   `COMBAT_STATE_CHANGED`.
5. Repeated exact entry with a different event id is also a no-op; exact exit is consumed once.
6. Xiuluo and Wubei combat waits use the Client edge state and indefinite event wake. Their target methods contain
   no `handleCombatTick` or Cloud radar transition.
7. Historical coordinator arm/rearm/reconcile methods are compatibility no-ops. The class has no
   `BattleRadarService` dependency and does not adjudicate combat.

### Full write set

Client `D:\mavenProject\DHXY-cr271`:

- `docs/ACTIVE_WORK.md`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationProtocolValidator.java`
- `src/main/java/com/bot/dhxy/window/observation/LocalCombatSignalMechanics.java`
- `src/main/java/com/bot/dhxy/window/observation/SpringObservationRunnerFactory.java`
- `src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java`
- `src/main/java/com/bot/dhxy/window/observation/WindowObservationRunner.java`
- `src/test/java/com/bot/dhxy/cloud/turn/WindowTurnLoopObservationContractTest.java`
- `src/test/java/com/bot/dhxy/window/observation/LocalCombatSignalMechanicsTest.java`
- `src/test/java/com/bot/dhxy/window/observation/WindowObservationRunnerContractTest.java`

Cloud `D:\mavenProject\dhxy-cloud-brain`:

- `src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationProtocolValidator.java`
- `src/main/java/com/bot/dhxy/service/AutoCombatService.java`
- `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/observation/CloudObservationHttpHandler.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudFastExpectedCombatExitCoordinator.java`
- `src/test/java/com/bot/dhxy/task/wubei/WubeiTaskTrackerTurnContractTest.java`
- `src/test/java/com/yueyunfe/dhxy/cloudbrain/observation/CloudObservationContractTest.java`
- `src/test/java/com/yueyunfe/dhxy/cloudbrain/observation/FastExpectedExitObservationContractTest.java`
- `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudFastExpectedCombatExitArmLifecycleTest.java`

### Verification

- Client focused named family:
  `LocalCombatSignalMechanicsTest,FastExpectedCombatExitProbeTest,WindowObservationRunnerContractTest,`
  `WindowTurnLoopObservationContractTest,LocalRunnerIdentityContractTest,`
  `XiuluoKandaProductionChainContractTest` = `44/44`, failures `0`, errors `0`.
- Cloud focused named family:
  `CloudObservationContractTest,FastExpectedExitObservationContractTest,`
  `CloudFastExpectedCombatExitArmLifecycleTest,FastExpectedExitGateTest,`
  `WubeiTaskTrackerTurnContractTest` = `29/29`, failures `0`, errors `0`.
- Client main compile: `mvn -q -DskipTests compile` -> exit `0`.
- Cloud main compile: `mvn -q -DskipTests=false compile` -> exit `0`.
- Shared `ObservationProtocolValidator.java` is byte-identical:
  SHA-256 `7FFC5E44701ACAFC62F82DC375A8D5921C5CC7B5E8C91F29FB73484A17EB210F`.
- `git diff --check` on the Repair #8 source/test write set reports no new whitespace error.

### Existing unrelated red evidence

- `AutoCombatServiceTurnContractTest`: `40` run, `3` failures and `6` errors in old supply-count,
  panel-capture and identity-metadata fixtures. Repair #8 does not modify those fixture contracts.
- `XiuluoTaskTrackerTurnContractTest`: three tests require absent
  `images/template/task/xiuluo_tracker_title.png`; one old harness has no tracker local-service client.
  These failures occur before the Repair #8 combat edge path.
- The broader `XiuluoWholeTaskTurnContractTest` also retains pre-existing route-fixture/reflection failures
  (`NavigationService` fixture null and removed `isNearCoordinate` signature).

No runtime, UI, capture, physical input or Git mutation was performed.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-8 CANONICAL-SOURCE-TEST-DELIVERED AWAITING-PARENT-REVIEW BASELINE-59B85E0B CLIENT-EDGE-AUTHORITY CLOUD-DIRECT-CONSUME XIULUO-WUBEI-PARK-NO-RADAR CLIENT-44-44 CLOUD-29-29 COMPILE-0 BYTE-IDENTICAL OWNER-RETAINED 2026-07-24T02:13:11-04:00 -->

## Parent SOURCE+TEST Review #11 - Repair #9 required (2026-07-24)

Verdict: `P0/P1/P2 = 0/1/0`; Repair #8 is not approved; owner retained.

### P1 - A claim arriving after the first visible combat sample never publishes `IN_COMBAT`

Evidence:

- `WindowRuntimeContext.registerExpectedCombatEnterClaim(...)` intentionally supports the Cloud-fallback
  race: when local combat is already visible it binds the arriving claim to the current
  `localCombatGeneration`.
- `WindowObservationSampler.observeLocalCombatTransition(...)` attempts
  `publishCombatEdgeIfClaimed(IN_COMBAT, ...)` only inside `if (!localCombatVisible)`, on the first
  visible sample.
- If that first sample precedes claim registration, `boundExpectedCombatClaim` is null and no event
  is emitted. Later visible samples skip the whole entry block, so the now-bound current-generation
  claim is never read and the exact `IN_COMBAT` edge is permanently lost. Cloud remains parked.
- `LocalRunnerIdentityContractTest.claimArrivingAfterVisibleEdgeBindsCurrentGenerationWithoutTimeGuessing`
  proves only that the context stores the late claim; it does not pass a later visible sample through
  `WindowObservationSampler` or prove that exactly one event is published.

Required repair:

1. While the same combat generation remains visible, permit a newly bound exact current-run claim to
   publish the deterministic `IN_COMBAT` event once.
2. Preserve edge semantics: do not create a new generation, restart the fast-exit baseline, or emit a
   second business entry for an already-published claim. Reuse deterministic event identity and the
   existing exact task/window/attempt fences.
3. Add a sampler-level production-path test for: first visible sample with no claim -> late exact claim
   registration -> next visible sample publishes one `IN_COMBAT` -> further visible samples publish
   none. Add stale/wrong-run negative coverage.
4. Re-run the focused Client family, relevant Cloud ingress/wake family, and both compiles before
   canonical re-delivery.

No runtime, UI, capture, physical input or Git mutation was performed by the parent review.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 PARENT-REVIEW-11 REPAIR-9-REQUIRED P0-0-P1-1-P2-0 LATE-CLAIM-IN-COMBAT-EDGE-LOST CLOUD-PARKS OWNER-RETAINED 2026-07-24 -->

## Repair #9 canonical SOURCE+TEST delivery (2026-07-24 02:34 -04:00)

State: `AWAITING_PARENT_REVIEW`; owner retained; this delivery does not self-approve.

### Repair

- `WindowObservationSampler` now records whether the current local combat generation has successfully
  published its exact business entry.
- First visible sample still exclusively owns generation creation, `WindowRuntimeContext` visible-state update
  and `FastExpectedCombatExitProbe.beginCombat(...)`.
- Every later visible sample may resolve a previously absent claim through
  `currentExpectedCombatEnterClaim(taskRunId, localCombatGeneration)`, but only while the generation's entry
  remains unpublished.
- The first valid late claim publishes the existing deterministic
  `combat-enter:{claimId}:{generation}` event and closes the publication gate. Further visible samples are no-ops.
- Stale-generation and wrong-observation-run claims remain rejected by the existing exact Context lookup.
- A generation with no published entry does not emit an orphan business `COMBAT_EXITED`.

### Repair #9 write set

- `D:\mavenProject\DHXY-cr271\src\main\java\com\bot\dhxy\window\observation\WindowObservationSampler.java`
- `D:\mavenProject\DHXY-cr271\src\test\java\com\bot\dhxy\window\observation\LocalCombatSignalMechanicsTest.java`
- `D:\mavenProject\DHXY-cr271\docs\ACTIVE_WORK.md`
- this card EOF

No Cloud source or shared protocol file changed in Repair #9.

### Focused production-path coverage

- `lateExactClaimPublishesEntryOnceWithinTheExistingVisibleGeneration`:
  first visible with no claim -> late current-run claim registration -> next visible publishes one exact
  `IN_COMBAT` with generation `1` -> third visible publishes nothing. A one-frame fast-probe fixture also proves
  the later visible samples do not restart the fast baseline.
- `staleGenerationClaimNeverPublishesEntry`: a same-run claim bound to generation `99` cannot satisfy current
  generation `1`.
- `wrongObservationRunClaimIsIgnoredAndDoesNotBlockLaterExactClaim`: wrong-run late claim is a no-op; replacing it
  with the current-run claim publishes exactly once in the unchanged generation.

### Verification

- Client focused named family:
  `LocalCombatSignalMechanicsTest,FastExpectedCombatExitProbeTest,WindowObservationRunnerContractTest,`
  `WindowTurnLoopObservationContractTest,LocalRunnerIdentityContractTest,`
  `XiuluoKandaProductionChainContractTest` = `47/47`, failures `0`, errors `0`.
- Cloud focused named family:
  `CloudObservationContractTest,FastExpectedExitObservationContractTest,`
  `CloudFastExpectedCombatExitArmLifecycleTest,FastExpectedExitGateTest,`
  `WubeiTaskTrackerTurnContractTest` = `29/29`, failures `0`, errors `0`.
- Client `mvn -q -DskipTests compile` -> exit `0`.
- Cloud `mvn -q -DskipTests=false compile` -> exit `0`.
- `git diff --check` on the Repair #9 source/test write set -> no new whitespace error.

No runtime, UI, capture, physical input or Git mutation was performed.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-9 CANONICAL-SOURCE-TEST-DELIVERED AWAITING-PARENT-REVIEW LATE-CLAIM-SAME-GENERATION-IN-COMBAT-ONCE STALE-WRONG-RUN-NOOP NO-BASELINE-RESTART CLIENT-47-47 CLOUD-29-29 COMPILE-0 OWNER-RETAINED 2026-07-24T02:34:25-04:00 -->

## Parent SOURCE+TEST Review #12 - Repair #10 required (2026-07-24)

Verdict: Repair #9 fixes Review #11 correctly, but the whole card remains
`P0/P1/P2 = 0/1/0`; owner retained.

### Closed finding

- The late-claim race is closed: a later visible sample resolves the exact claim and publishes the
  current generation's deterministic `IN_COMBAT` once.
- The implementation does not create a new generation or restart the fast probe, and it suppresses an
  orphan `COMBAT_EXITED` when no business entry was published.
- The new sampler tests cover exact late claim, stale generation, wrong observation run and subsequent
  exact replacement.

### P1 - Xiuluo/Wubei still invoke a second Cloud combat detector during return correction

Evidence:

- `XiuluoTaskV2.probeTrustedCombatStateAfterReturnVerificationFailure(...)` calls
  `AutoCombatService.probeWindowCombatStateReadOnly(...)`.
- `WubeiTask.probeTrustedCombatStateAfterReturnVerificationFailure(...)` calls the same method.
- `AutoCombatService.probeWindowCombatStateReadOnly(...)` directly calls
  `BattleRadarService.checkAndSyncCombatState()`.

This violates the authoritative one-way contract already written above: the Client runner is the sole
mechanical combat authority; Cloud must consume the exact Client edge and must not take another
screenshot/radar decision. These return-correction branches can overwrite or contradict the Client edge,
reintroducing the second combat state machine that Repair #8 was required to remove.

Required repair:

1. Remove Xiuluo and Wubei production-path dependence on
   `probeWindowCombatStateReadOnly(...)` / Cloud `BattleRadarService` for combat-state correction.
2. Preserve the agreed fast-exit correction behavior through the existing local retained return-home
   replay and exact Client transition/terminal events; Cloud may validate identity, park, consume and
   wake, but may not mechanically re-detect combat.
3. Add source/production-path contracts proving Xiuluo and Wubei return-correction paths do not invoke
   `BattleRadarService`, `probeWindowCombatStateReadOnly(...)`, `handleCombatTick(...)`, or another Cloud
   combat detector.
4. Re-run the focused Client/Cloud families and both compiles, then canonical re-deliver the whole card.

No runtime, UI, capture, physical input or Git mutation was performed by the parent review.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 PARENT-REVIEW-12 REPAIR-10-REQUIRED P0-0-P1-1-P2-0 REPAIR-9-FINDING-CLOSED CLOUD-RETURN-CORRECTION-RADAR-STILL-ACTIVE SECOND-COMBAT-STATE-MACHINE OWNER-RETAINED 2026-07-24 -->

## Repair #10 canonical SOURCE+TEST re-delivery (2026-07-24 02:48 -04:00)

State: `AWAITING_PARENT_REVIEW`; owner retained; this delivery does not self-approve.

### Baseline method mapping and repair

- Cloud `XiuluoTaskV2.useReturnItemAndVerifyStartMap(...)` and
  `WubeiTask.useReturnItemAndVerifyStartMap(...)` now map
  `USED_START_MAP_UNVERIFIED` directly to `STILL_IN_COMBAT`. They do not invoke
  `AutoCombatService.probeWindowCombatStateReadOnly(...)`, `BattleRadarService`,
  `handleCombatTick(...)`, or another Cloud combat detector.
- Cloud `resumeWaitCombatAfterTrustedReturnCorrection(...)` /
  `resumeWaitBattleAfterTrustedReturnCorrection(...)` retain the existing exact
  `armReturnHomeReplay(...)` identity fence and park in `RETURN_HOME`. True
  `NOT_USED` / `FAILED` retains the existing task failure/recovery path.
- The obsolete task-local probe/correction helpers and
  `FAILED_AFTER_TRUSTED_NOT_IN_COMBAT` state were removed from both production
  task paths. `AutoCombatService` remains available to unrelated legacy paths;
  Repair #10 does not expand its responsibility.
- Client `WindowObservationSampler` preserves the `59b85e0b` local mechanical
  exit authority. If an early fast-exit already consumed the exact business
  claim, the later true local exit can discover only an armed replay belonging
  to the same observation run and submit it without fabricating a second
  `COMBAT_EXITED`.
- `DeferredReturnHomeReplayCoordinator` still claims the exact task/window/HWND
  replay once and executes it asynchronously. A correction generation publishes
  only its typed replay terminal; when a business exit exists, success still
  precedes that edge.

### Repair #10 write set

Client:

- `D:\mavenProject\DHXY-cr271\src\main\java\com\bot\dhxy\window\runtime\WindowRuntimeContext.java`
- `D:\mavenProject\DHXY-cr271\src\main\java\com\bot\dhxy\window\observation\WindowObservationSampler.java`
- `D:\mavenProject\DHXY-cr271\src\main\java\com\bot\dhxy\window\observation\DeferredReturnHomeReplayCoordinator.java`
- `D:\mavenProject\DHXY-cr271\src\test\java\com\bot\dhxy\window\runtime\LocalRunnerIdentityContractTest.java`
- `D:\mavenProject\DHXY-cr271\src\test\java\com\bot\dhxy\window\observation\DeferredReturnHomeReplayCoordinatorContractTest.java`
- `D:\mavenProject\DHXY-cr271\docs\ACTIVE_WORK.md`
- this card EOF

Cloud:

- `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\xiuluo\XiuluoTaskV2.java`
- `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\wubei\WubeiTask.java`
- `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\observation\FastExpectedExitObservationContractTest.java`

No shared protocol file changed.

### Focused production contracts

- `returnCorrectionParksOnRetainedReplayWithoutCloudCombatDetection` checks both
  complete task sources and their correction methods: no trusted probe,
  `probeWindowCombatStateReadOnly`, `BattleRadarService`, `handleCombatTick`, or
  retired result state; both correction methods arm replay and park.
- `correctionExitCanDiscoverOnlyTheExactArmedObservationRun` proves stale
  observation identity cannot expose an armed replay.
- `correctionGenerationPublishesReplayTerminalWithoutInventingBusinessExit`
  fixes the terminal-only correction contract and the ordinary/fast local exit
  submission seam.

### Verification

- Client focused named family:
  `LocalCombatSignalMechanicsTest,FastExpectedCombatExitProbeTest,`
  `WindowObservationRunnerContractTest,WindowTurnLoopObservationContractTest,`
  `LocalRunnerIdentityContractTest,XiuluoKandaProductionChainContractTest,`
  `DeferredReturnHomeReplayCoordinatorContractTest`
  = `52/52`, failures `0`, errors `0`.
- Cloud focused named family:
  `CloudObservationContractTest,FastExpectedExitObservationContractTest,`
  `CloudFastExpectedCombatExitArmLifecycleTest,FastExpectedExitGateTest,`
  `WubeiTaskTrackerTurnContractTest,WubeiWholeTaskTurnContractTest`
  = `46/46`, failures `0`, errors `0`.
- Client `mvn -DskipTests=false compile` -> exit `0`.
- Cloud `mvn -DskipTests=false compile` -> exit `0`.
- Source scan confirms Xiuluo/Wubei contain no
  `probeTrustedCombatStateAfterReturnVerificationFailure`,
  `probeWindowCombatStateReadOnly`,
  `FAILED_AFTER_TRUSTED_NOT_IN_COMBAT`, `handleCombatTick`, or
  `BattleRadarService`.
- `git diff --check` reported no new whitespace error; existing CRLF notices
  remain workspace-wide.

An exploratory broader Cloud run also exposed pre-existing unrelated failures in
old `AutoCombatServiceTurnContractTest` fixtures and Xiuluo tests that require
missing template/injected navigation fixtures. The Repair #10 focused families
above were rerun independently and are green; those unrelated failures were not
modified or represented as this card's acceptance result.

No runtime, UI, capture, physical input or Git mutation was performed.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-10 CANONICAL-SOURCE-TEST-REDELIVERED AWAITING-PARENT-REVIEW CLOUD-RETURN-CORRECTION-NO-RADAR CLIENT-TRUE-EXIT-EXACT-REPLAY TERMINAL-ONLY-NO-DUPLICATE-BUSINESS-EXIT CLIENT-52-52 CLOUD-46-46 COMPILE-0 OWNER-RETAINED 2026-07-24T02:48:06-04:00 -->

## Parent SOURCE+TEST Review #13 - Repair #11 required (2026-07-24)

Verdict: Repair #10 closes Review #12's Cloud Radar finding, but the whole card remains
`P0/P1/P2 = 0/1/0`; owner retained.

### Closed finding

- Xiuluo and Wubei return-correction production paths no longer invoke
  `probeWindowCombatStateReadOnly(...)`, `BattleRadarService`,
  `handleCombatTick(...)`, or another Cloud mechanical combat detector.
- Cloud arms the exact retained return-home replay and parks; the Client runner
  owns the later true mechanical exit and may submit the replay terminal without
  inventing a second business `COMBAT_EXITED`.

### P1 - the claimed Client 52/52 acceptance family is timing-dependent

Parent independent execution of the exact seven-class Client family reported
`52 tests run / 2 failures`:

1. `WindowObservationRunnerContractTest.transportFailureRetainsKeyEventsAndIsNeverABusinessFact`
   failed at line 123: expected pending key-event count `0`, actual `1`.
2. `WindowObservationRunnerContractTest.suspendResumePreservesSequenceInterestsAndUnacknowledgedEvents`
   failed at line 189: expected pending key-event count `0`, actual `1`.

Running `WindowObservationRunnerContractTest` alone immediately afterwards
reported `10/10`, which confirms an order/scheduling-sensitive test gate rather
than a reproducible green acceptance family. The test's `awaitHandled(...)`
observes that the scripted transport has produced a response, but it does not
prove that the runner thread has already applied `acknowledgedEventIds` and
removed the retained event before the assertion reads `pendingKeyEventCount()`.

Required Repair #11:

1. Make the test fixture expose a deterministic "response applied by runner"
   synchronization point, or wait on an equivalent observable postcondition.
   Do not use arbitrary sleeps, retry loops that hide failures, or production
   timing changes.
2. Preserve the behavioral assertions: transport failures retain and resend the
   event; only an applied acknowledgement removes it; suspend preserves an
   unacknowledged event; resume applies the acknowledgement and clears it.
3. Do not modify Repair #10 production behavior unless a separately proven
   production defect is found and written back for parent review.
4. Re-run the full Client 52-test family three consecutive times, the Cloud
   46-test family once, and both compiles. All runs must exit `0` before
   canonical re-delivery.

No runtime, UI, capture, physical input or Git mutation was performed by the
parent review.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 PARENT-REVIEW-13 REPAIR-11-REQUIRED P0-0-P1-1-P2-0 REPAIR-10-SOURCE-FINDING-CLOSED CLIENT-ACK-APPLICATION-TEST-RACE FULL-FAMILY-NOT-REPRODUCIBLY-GREEN OWNER-RETAINED 2026-07-24 -->

## Repair #11 canonical SOURCE+TEST re-delivery (2026-07-24 03:02 -04:00)

State: `AWAITING_PARENT_REVIEW`; owner retained; this delivery does not self-approve.

### Repair

- Changed only
  `D:\mavenProject\DHXY-cr271\src\test\java\com\bot\dhxy\window\observation\WindowObservationRunnerContractTest.java`.
  No production source or Cloud file changed in Repair #11.
- `transportFailureRetainsKeyEventsAndIsNeverABusinessFact` now scripts one
  request after the acknowledgement. The first three requests must retain the
  event across two transport failures and the ack response; the fourth request
  must contain zero events. Construction of that fourth request can occur only
  after Runner has iterated and removed `acknowledgedEventIds`, so it is the
  deterministic response-applied synchronization point.
- `suspendResumePreservesSequenceInterestsAndUnacknowledgedEvents` uses the same
  post-ack request proof after resume. It still proves pause retention,
  monotonic sequence continuation, exact event resend on resume, and final ack
  removal.
- The fixture uses no arbitrary sleep, polling retry, timing change, or
  production test hook. Existing sleep in the unrelated stop-fence test was
  untouched.

### Verification

- Isolated `WindowObservationRunnerContractTest`: `10/10`, failures `0`,
  errors `0`.
- Full Client 52-test family, three independent consecutive Maven invocations:
  - run 1: `52/52`, failures `0`, errors `0`, exit `0`;
  - run 2: `52/52`, failures `0`, errors `0`, exit `0`;
  - run 3: `52/52`, failures `0`, errors `0`, exit `0`.
- Cloud 46-test family: `46/46`, failures `0`, errors `0`, exit `0`.
- Client `mvn -DskipTests=false compile` -> exit `0`.
- Cloud `mvn -DskipTests=false compile` -> exit `0`.
- Scoped `git diff --check` reported no new whitespace error.

The user's fresh Client process began at `00:48`, before Repair #10's `02:48`
canonical delivery. Its observations are therefore old-process evidence and
were not classified as a Repair #10 failure.

No runtime, UI, capture, physical input or Git mutation was performed.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-11 CANONICAL-SOURCE-TEST-REDELIVERED AWAITING-PARENT-REVIEW TEST-FIXTURE-ONLY ACK-APPLIED-BY-NEXT-REQUEST NO-SLEEP CLIENT-52-52-X3 CLOUD-46-46 DUAL-COMPILE-0 OLD-0048-PROCESS-PREDATES-0248-DELIVERY OWNER-RETAINED 2026-07-24T03:01:52-04:00 -->

## Parent SOURCE+TEST Review #14 - PASSED (2026-07-24)

Verdict: `P0/P1/P2 = 0/0/0`; Repair #11 and the whole-card source/test gate pass.
Owner released. Fresh runtime verification remains required.

### Review

- Repair #11 changes only `WindowObservationRunnerContractTest`; no Client
  production source or Cloud source changed after Repair #10.
- The post-ack request is a valid deterministic synchronization point: Runner
  cannot construct that request until the preceding response has returned and
  its acknowledged event ids have been removed.
- The tests retain the required behavior across transport failure and
  suspend/resume. No arbitrary sleep, polling retry, production timing hook or
  weakened assertion was introduced.
- Repair #10's source boundary remains intact: Client runner owns mechanical
  combat entry/exit and correction replay; Xiuluo/Wubei production paths contain
  no Cloud Radar/probe/`handleCombatTick` combat adjudication.

### Parent verification

- Client 52-test family: three consecutive independent runs, each exit `0`.
- Cloud 46-test family: exit `0`.
- Client compile: exit `0`.
- Cloud compile: exit `0`.
- No runtime, UI, capture, physical input or Git mutation was performed.

The Client process used by the `02:51` runtime began at `00:48`, before Repair
#10 was delivered at `02:48`; that old process is not evidence against the
reviewed source. End-to-end acceptance requires restarting both Client and Cloud
from the reviewed source and creating a new task run.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 PARENT-REVIEW-14 SOURCE-TEST-REVIEW-PASSED P0-0-P1-0-P2-0 OWNER-RELEASED CLIENT-52-52-X3 CLOUD-46-46 DUAL-COMPILE-0 FRESH-RUNTIME-RESTART-REQUIRED 2026-07-24 -->

## Fresh-runtime correction / Repair #12 (2026-07-24 03:27 -04:00)

State: `REPAIR DELIVERED / FRESH RUNTIME REQUIRED`.

The fresh `03:18-03:22` Xiuluo run disproved Parent Review #14's production
boundary conclusion. Client correctly generated the local combat exit at
`03:20:22.038`, but Cloud `CloudWholeTaskObserver.probeCombat(...)` continued
calling `BattleRadarService.checkAndSyncMechanicalCombatSignal(...)`. Cloud log
lines 342-499 then published `cloud-combat-watch:xiuluo_v2` and repeatedly kept
the task `IN_COMBAT` because the coordinate frame was unavailable. This was an
unreviewed Cloud observer side door, independent of the already removed calls
inside `XiuluoTaskV2` and `WubeiTask`.

Production repair:

- `CloudWholeTaskObserver` no longer invokes `probeCombat(...)` for
  `XIULUO_V2` or `WUBEI`.
- For those two tasks, mechanical combat entry/exit is now single-authority:
  the Client runner repeatedly performs local image matching and Cloud only
  consumes exact `IN_COMBAT` / `COMBAT_EXITED` events.
- Other observer duties and the Five-Ring path are unchanged.

Verification intentionally did not run tests at the user's request.
Cloud `mvn -DskipTests=false compile` completed with exit `0`; scoped
`git diff --check` passed. Acceptance requires a fresh Cloud process and fresh
task run, with no `cloud-combat-watch:xiuluo_v2` or
`cloud-combat-watch:wubei` log records.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-12 PRODUCTION-DELIVERED REVIEW-14-CORRECTED CLOUD-OBSERVER-RADAR-SIDE-DOOR-REMOVED XIULUO-WUBEI-CLIENT-LOCAL-MATCH-ONLY CLOUD-COMPILE-0 NO-TESTS-BY-USER-DIRECTION FRESH-RUNTIME-REQUIRED 2026-07-24T03:27:00-04:00 -->

## Fresh-runtime Repair #13 (2026-07-24 03:42 -04:00)

Latest run `remote-turn-3cd107ab-af62-4411-97c5-983afc4c314a` proved that Client
`COMBAT_EXITED` reached Cloud and woke `WAIT_COMBAT`, but the task immediately
parked again. The exact `IN_COMBAT` cleanup had returned `true` earlier while
the task was in `WAIT_TRACKER_SHORTCUT_PATHING`; the phase loop discarded that
return value, and the local-kanda branch then rewrote the round as pending
confirmation. Consequently `enteredBattleByXiuluo` remained false at exit.

Repair #13 propagates the confirmed entry into the effective round context and
prevents an already confirmed local-kanda entry from being downgraded to
pending. Cloud compile exit `0`; no tests were run by user direction. Fresh
Cloud restart and task run remain required.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-13 PRODUCTION-DELIVERED XIULUO-CONFIRMED-ENTRY-STATE-PROPAGATED NO-PENDING-DOWNGRADE CLOUD-COMPILE-0 NO-TESTS-BY-USER-DIRECTION FRESH-RUNTIME-REQUIRED 2026-07-24T03:42:00-04:00 -->

## Fresh-runtime Repair #14 - local pathing terminal restored (2026-07-24 03:55 -04:00)

State: `REPAIR DELIVERED / FRESH RUNTIME REQUIRED`.

Fresh run `remote-turn-231f07a7-bf29-42e1-a5ff-5455c77af726` confirms the
combat Fast Pass now advances `WAIT_COMBAT -> RETURN_HOME`. It also exposes a
separate production gap after returning home:

- Client registered NPC-navigation intent
  `118ae865-13c0-4e50-8723-ca39fee37bc8` as `ACTIVE`.
- No Client production call updated the snapshot to `ARRIVED` or
  `STOPPED_AWAY`; `updatePathingSnapshot(...)` had only its definition and a
  test call.
- Cloud therefore parked in `WAIT_TARGET_PATHING_TERMINAL` for `164.376s`.
  The pre-combat watchdog then incorrectly ended round 3. Round 4 freshly
  synchronized `灵兽村 (111,93)` and only then invoked and verified NPC click.

Repair #14 restores the baseline's local stopped-pathing authority without
restoring Client OCR or a second navigation state machine:

- `WindowObservationSampler` captures only the exact-HWND coordinate strip.
  Pixel changes retain `ACTIVE`; stable pixels for `2200ms` produce
  `STOPPED_AWAY` for the exact current intent.
- `CloudWholeTaskObserver` accepts that Client terminal and publishes it once
  under the existing exact-intent CAS. It does not require a Cloud position
  probe to re-decide whether the Client stopped.
- Cloud remains responsible for the subsequent fresh position sync, map/NPC
  semantics and NPC click.

Client and Cloud compile both completed with exit `0`. Tests were not run by
explicit user direction. Fresh acceptance requires restarting both processes
and observing `Local pathing terminal observed`, followed by
`local runner pathing terminal accepted`, fresh position sync and NPC click
without the 180-second watchdog.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-14 PRODUCTION-DELIVERED FAST-PASS-FRESH-CONFIRMED LOCAL-PATHING-STOPPED-AWAY-RESTORED EXACT-INTENT-CLOUD-CONSUME DUAL-COMPILE-0 NO-TESTS-BY-USER-DIRECTION FRESH-RUNTIME-REQUIRED 2026-07-24T03:55:00-04:00 -->

## Fresh-runtime Repair #15 - Runner combat state is the sole phase authority (2026-07-24 04:12 -04:00)

State: `REPAIR DELIVERED / FRESH CLOUD RESTART REQUIRED`.

The latest round of run
`remote-turn-231f07a7-bf29-42e1-a5ff-5455c77af726` disproved Repair #14's
statement that the Fast Pass had completed end to end:

- Client emitted exact `IN_COMBAT` sequence `4`, then exact `COMBAT_EXITED`
  sequence `5`.
- Cloud accepted both events, but Xiuluo was still in
  `WAIT_TRACKER_SHORTCUT_PATHING`. The exit wake caused the stale local-kanda
  click fact to move the phase to `WAIT_COMBAT` with `afterSequence=5`.
- Sequence `5` was therefore treated as already consumed and Cloud waited
  forever for a second exit that could not occur.

Root cause: Xiuluo still treated click/local-kanda/cleanup facts as combat-state
confirmation even though the exact Client Runner event is the sole combat
authority.

Repair #15 removes that second authority:

- exact Runner `IN_COMBAT` moves the effective round directly to
  `WAIT_COMBAT`; local cleanup remains retryable but cannot veto the state;
- local-kanda, prepared dialog, normal confirm, direct-click and recovered
  dialog completion only record/await input completion and cannot advance
  combat state;
- when exact `COMBAT_EXITED` has already followed the entry, the effective
  `WAIT_COMBAT` phase observes Client state `FREE` and immediately executes the
  existing post-combat path instead of parking after the exit sequence.

Cloud `mvn -DskipTests=false compile` completed with exit `0`. Tests were not
run by explicit user direction. Acceptance requires a fresh Cloud process and
must show exact Runner `IN_COMBAT -> COMBAT_EXITED` advancing through post-combat
without any local-kanda state confirmation or second exit wait.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-15 PRODUCTION-DELIVERED RUNNER-SOLE-COMBAT-AUTHORITY CLICK-FACTS-NON-AUTHORITATIVE EXIT-SEQUENCE-NOT-LOST CLOUD-COMPILE-0 NO-TESTS-BY-USER-DIRECTION FRESH-CLOUD-RESTART-REQUIRED 2026-07-24T04:12:00-04:00 -->

## Fresh-runtime Repair #16 - ordinary exit evidence is Runner-owned (2026-07-24 07:16 -04:00)

State: `PRODUCTION REPAIR DELIVERED / FRESH DUAL RESTART REQUIRED`.

Fresh run `remote-turn-26ace75c-0061-43d1-a9e2-a6fe093d54d8` confirms Repair
#15 correctly moved exact Runner `IN_COMBAT` sequence `2` directly from
`WAIT_TRACKER_SHORTCUT_PATHING` to `WAIT_COMBAT`. No `COMBAT_EXITED` followed.

The failure was an observation-interest ownership race:

- pause at `07:06:39` retained the same Runner lineage; resume at `07:07:14`
  re-established the in-combat mechanical baseline;
- the Fast Exit delay gate did not fire before the first absent combat sample;
- ordinary exit then required a coordinate-readability result after that miss;
- `CloudFastExpectedCombatExitCoordinator` added `coordinate-strip` on entry,
  while legacy `CloudWholeTaskObserver` removed the same interest whenever no
  pathing/legacy fallback was active. The Client consequently fell from
  `interests=2, rois=1` to `interests=1, rois=0`, leaving ordinary exit
  permanently fail-closed.

Repair #16 gives the evidence request to the sole local Runner:

- the first real absent combat sample captures exactly one bound-HWND
  `coordinate-strip` ROI;
- Cloud only returns `readable/unreadable`; it does not own the interest or
  decide combat state;
- the next absent sample may consume that fresh result and publish the exact
  `COMBAT_EXITED` edge;
- the Cloud combat coordinator no longer adds/removes coordinate interests.

Client and Cloud compile both completed with exit `0`. No tests were run by
user direction. Fresh acceptance requires restarting both processes and must
show a suspected-exit request with one coordinate ROI followed by exact
`COMBAT_EXITED`, then immediate post-combat advancement.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-16 PRODUCTION-DELIVERED RUNNER-OWNS-ORDINARY-EXIT-EVIDENCE CLOUD-READABILITY-ONLY INTEREST-RACE-REMOVED DUAL-COMPILE-0 NO-TESTS-BY-USER-DIRECTION FRESH-DUAL-RESTART-REQUIRED 2026-07-24T07:16:00-04:00 -->

## Parent legacy-code audit finding (2026-07-24)

State: `P1 FOUND / CLEANUP CONTRACT REQUIRED`.

The source boundary is not yet clean even after Repair #16. A production call-chain
audit found three distinct categories that must not be treated as one deletion:

1. **Live Xiuluo/Wubei violation (P1).** Both tasks call
   `NpcClickService.tryDirectCombatTargetClick(...)`. Its
   `combatClickVerifier()` invokes
   `BattleRadarService.checkAndSyncCombatState()` up to four times after the
   direct click. This is a real second Cloud combat authority and was missed by
   source-contract checks that only scanned the task classes themselves.
2. **Dead compatibility baggage.** `CloudFastExpectedCombatExitCoordinator`
   still exposes the no-op `arm`, `reconcileAfterCombatEnter`, `rearm` and
   `disarm` lifecycle plus `ArmStatus`; `AutoCombatService` still carries and
   calls that dead lifecycle. `NavigationService` also retains an unused
   `BattleRadarService` dependency.
3. **Old code still used by other tasks.** `AutoCombatService`,
   `BattleRadarService` and `CloudWholeTaskObserver.probeCombat(...)` remain
   active for Five-Ring/AutoBattle. They cannot be deleted wholesale as part of
   the Xiuluo/Wubei repair. Their fast-expected-exit subpath appears to have no
   live production caller after the local Runner cutover, but it must be
   separated from the still-live Five-Ring/AutoBattle radar behavior before
   removal.

Required repair direction:

- remove the direct-combat Cloud Radar verifier from the Xiuluo/Wubei path and
  park only on the exact Client Runner `IN_COMBAT` edge;
- remove the no-op Cloud arm compatibility API and its `AutoCombatService`
  state baggage;
- remove unused constructor dependencies;
- retain normal Cloud Radar behavior only for tasks that have not yet migrated,
  and add a transitive production-call boundary rather than another shallow
  task-source string scan.

No Java was changed during this audit.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 PARENT-LEGACY-AUDIT P1-LIVE-NPC-DIRECT-COMBAT-CLOUD-RADAR DEAD-ARM-COMPATIBILITY-BAGGAGE WUHuan-AUTOBATTLE-RADAR-STILL-LIVE CLEANUP-CONTRACT-REQUIRED NO-JAVA-CHANGE 2026-07-24 -->

## Parent Review #17 - single local minimap exit authority (2026-07-24 08:18 -04:00)

State: `SOURCE+TEST REVIEW PASSED / P0-P1-P2=0/0/0 / FRESH DUAL RESTART REQUIRED`.

The two prior exit paths failed for independent reasons:

1. the avatar-diff path sampled only while processing a combat-visible cycle,
   so the first exact non-combat frame could bypass it; its 15-second delay also
   contradicted the required immediate exit behavior;
2. the ordinary path required two combat misses plus a later Cloud
   coordinate-readability result, so a removed/stale observation interest left
   the Client permanently fail-closed in combat.

Repair #17 removes both competing authorities. While the local generation is
in combat, `WindowObservationSampler` checks the fixed normal-world minimap
anchor at `(196,65,20x22)` once per second. A visible anchor immediately closes
the generation and publishes exactly one claimed `COMBAT_EXITED`; no avatar
diff, miss counter, coordinate ROI upload or Cloud adjudication remains.

Deleted legacy surface:

- Client `FastExpectedCombatExitProbe` and its three obsolete contract tests;
- observation `FAST_EXPECTED_COMBAT_EXIT` event, dedicated interest identity
  fields/factory and validator branches in both repositories;
- Cloud `FastExpectedExitGate`, BattleRadar fast-exit probe/baseline/wait
  implementation, coordinate-strip automatic exit analysis and direct-click
  Radar verifier for Xiuluo/Wubei.

Normal Cloud Radar remains only for Five-Ring/AutoBattle, which are outside this
card's migrated authority boundary.

Verification:

- real saved frame:
  `images/test-cases/combat-exit/minimap-visible-world.png`;
- marked ROI:
  `images/test-cases/combat-exit/minimap-visible-world-marked.png`;
- production OpenCV `ImageFinder` replay at threshold `0.85`: Client focused
  `11/11`;
- Cloud observation focused family: `18/18`;
- Client and Cloud `test-compile`: both `BUILD SUCCESS`;
- shared `ObservationInterest`, `ObservationKeyEventType` and
  `ObservationProtocolValidator`: SHA-256 byte-identical across both repos.

No runtime, UI, live capture or physical input was executed. Fresh acceptance
must restart both processes and show local `IN_COMBAT -> COMBAT_EXITED` within
one sampling interval after the minimap anchor becomes visible, followed by
Cloud post-combat advancement with no Radar/readability fallback.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REVIEW-17 SOURCE-TEST-REVIEW-PASSED P0-0-P1-0-P2-0 SINGLE-LOCAL-MINIMAP-EXIT-AUTHORITY LEGACY-FAST-AND-CLOUD-READABILITY-DELETED CLIENT-11-11 CLOUD-18-18 DUAL-TEST-COMPILE-0 DTO-BYTE-IDENTICAL FRESH-DUAL-RESTART-REQUIRED 2026-07-24T08:18:00-04:00 -->

## Repair #18 - local dual-evidence tri-state (2026-07-24)

State: `PRODUCTION DELIVERED / CLIENT COMPILE 0 / FRESH CLIENT RESTART REQUIRED`.

User-approved evidence contract:

1. `minimap_visible_anchor` visible is positive normal-world evidence:
   `WORLD_CONFIRMED`, close the local generation and publish exact
   `COMBAT_EXITED` once.
2. If the mini-map anchor is not visible but any existing local combat stage is
   visible, the frame is `COMBAT_CONFIRMED`; remain in combat.
3. If neither positive detector is visible, or either required capture/template
   is unavailable without another positive detector, the frame is `UNKNOWN`;
   retain the current state and retry at the existing one-second cadence.
4. A template miss is never converted into the opposite business fact. No
   Cloud Radar, coordinate-readability request, miss counter, TTL or extra
   phase gate is reintroduced.

Implementation:

- `LocalCombatSignalMechanics.sampleMinimap()` now preserves
  `VISIBLE / ABSENT / UNAVAILABLE` instead of collapsing capture/template
  failure into boolean false.
- `WindowObservationSampler.observeLocalCombatTransition(...)` applies the
  explicit `WORLD_CONFIRMED / COMBAT_CONFIRMED / UNKNOWN` policy and logs only
  evidence-state changes.

Baseline check: `docs/业务逻辑.md` combat sections were reviewed. No additional
unapproved phase, retry, fallback, timing or Cloud authority was added.

Verification: Client `mvn -q -DskipTests compile` completed with exit `0`.
No tests, runtime, UI, live capture or physical input were run.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-18 PRODUCTION-DELIVERED LOCAL-DUAL-EVIDENCE TRI-STATE WORLD-CONFIRMED COMBAT-CONFIRMED UNKNOWN CLIENT-COMPILE-0 NO-RUNTIME FRESH-CLIENT-RESTART-REQUIRED 2026-07-24 -->

## Parent Finding #19 - leader auto-combat entry maintenance missing (2026-07-24)

State: `P1 CONFIRMED / REPAIR PLAN READY / USER APPROVAL REQUIRED`.

Fresh run `remote-turn-d101142f-12a6-4913-b408-0f26f6e7197d` proves:

- leader `hwnd-1D514EA / 67555` received the Client Runner combat lifecycle for
  every Xiuluo round from `10:51` through `11:10`;
- that leader emitted no `ALT_8`;
- all four `AUTO_BATTLE` member windows emitted successful background-HWND
  `ALT_8`, excluding the input queue and keyboard transport as the cause.

The migration lost one baseline side effect. The old Radar-owned enter edge
called `AutoCombatService.maybeHandleCombatEnter()`, which immediately opened
the auto-combat panel and scheduled delayed entry maintenance. The new
`CLIENT_RUNNER_EXIT` branch sees `GameContext=IN_COMBAT` but only invokes
ordinary maintenance. `initializeForCurrentWindow()` has already cleared the
entry pending timestamp, and team refresh throttling may then defer the
leader's ordinary panel refresh behind member activity.

The later maintenance loop is also disconnected. The current Cloud
`XiuluoTaskV2.waitForCombatStateWake()` parks with `timeoutMs(-1)`, while the
baseline uses `combatMaintenanceWakeTimeoutMs()`. With no combat edge during a
long fight, the task never wakes for the delayed entry check, sparse UI cleanup
or configured panel refresh.

Required repair:

- keep Client Runner as the sole combat authority;
- on the first exact Client `IN_COMBAT` of the current local combat generation,
  restore the existing one-shot panel open and delayed entry-maintenance
  schedule;
- restore the baseline maintenance-deadline wake while in combat; a timeout
  wake may run maintenance but must not adjudicate combat state;
- make repeated ticks idempotent and reset the one-shot on initialization and
  confirmed exit;
- do not restore Cloud Radar, image recognition, or another combat-state
  decision path.

No Java was changed during this investigation.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 FINDING-19 P1 LEADER-ALT8-MISSING CLIENT-IN-COMBAT-PRESENT MEMBER-HWND-ALT8-HEALTHY BASELINE-ENTER-SIDE-EFFECT-LOST REPAIR-PLAN-READY USER-APPROVAL-REQUIRED NO-JAVA-CHANGE 2026-07-24 -->

## Repair #19 delivery and parent review (2026-07-24)

State: `SOURCE REVIEW PASSED / P0-P1-P2=0/0/0 / FRESH CLOUD RESTART REQUIRED`.

Implemented in Cloud:

- `AutoCombatService` now restores the frozen combat-enter side effects on the
  first exact Client `IN_COMBAT` tick after battle initialization: one panel
  probe/`Alt+8`/re-probe and the existing delayed entry-maintenance deadline.
- The one-shot is idempotent within a battle and is re-armed by
  `initializeForCurrentWindow()` and confirmed combat exit.
- `XiuluoTaskV2` and `WubeiTask` no longer park forever during combat. Both use
  the baseline-bounded `500ms..10s` deadline derived from
  `AutoCombatService.nextCombatWakeDelayMs()`.

Authority review:

- Client Runner remains the sole combat fact producer.
- The `CLIENT_RUNNER_EXIT` branch contains no Cloud Radar call.
- A deadline wake only re-enters existing maintenance against the Client-synced
  `GameContext`; it does not capture, match or adjudicate combat state.

Verification:

- focused Client-runner enter contract: `1/1`, Maven `BUILD SUCCESS`;
- Cloud `mvn compile`: `BUILD SUCCESS`;
- source gates: Xiuluo dynamic wake `true`, Wubei dynamic wake `true`,
  Client branch without Radar `true`;
- `git diff --check` for the four touched Cloud files: exit `0`.

The full historical `AutoCombatServiceTurnContractTest` invocation is not
green: `38` run, `5` failures and `6` errors. The reported cases are existing
wall-clock scripted-maintenance assumptions and zero-dimension window fixtures;
the new exact behavior test passes independently. No production behavior was
weakened to satisfy those fixtures.

No runtime, UI, capture or physical input was executed. Restart Cloud before
fresh acceptance; Repair #19 itself does not require another Client restart.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-19 SOURCE-REVIEW-PASSED P0-0-P1-0-P2-0 CLIENT-RUNNER-ENTER-BOOTSTRAP-ONCE XIULUO-WUBEI-DYNAMIC-MAINTENANCE-WAKE NO-CLOUD-RADAR FOCUSED-1-1 CLOUD-COMPILE-0 FULL-HISTORICAL-FAMILY-5F-6E FRESH-CLOUD-RESTART-REQUIRED 2026-07-24T11:30:00-04:00 -->

## Repair #20 - production call-site closure and direct pathing wake (2026-07-24)

State: `SOURCE REPAIR COMPLETE / FRESH CLOUD RESTART REQUIRED`.

Fresh evidence:

- leader `hwnd-74A0D06` received exact Client `IN_COMBAT` and Cloud published
  `COMBAT_STATE_CHANGED sequence=28`, but no leader auto-combat entry or maintenance log followed;
- local Runner emitted pathing terminal for intent
  `7694b521-a385-4f41-8251-ba2cf5fe2052` at `14:43:58.672`, but the Cloud task timed out after
  about `164728ms` because the terminal still needed a second observer poll.

Root cause and repair:

1. Repair #19 restored the service-side `CLIENT_RUNNER_EXIT` behavior and bounded wait deadlines,
   but production `XiuluoTaskV2.waitCombat(...)` and
   `WubeiTask.tickWaitBattleFinish(...)` did not call that service while `IN_COMBAT`.
   Both call sites now invoke the existing maintenance path before parking. This restores the
   baseline first-entry panel bootstrap and subsequent maintenance without restoring Cloud Radar.
2. `CloudWindowObservationInbox` now exposes newly accepted typed pathing facts to the existing
   exact-run coordinator. The coordinator updates the existing `CloudNavigationPathingState` mirror
   and publishes one `PATHING_TERMINAL` for the exact task/window/intent. Duplicate terminal
   observations for the same intent are ignored.
3. `CloudWholeTaskObserver` keeps the existing Xiuluo stopped-static settlement but no longer
   republishes a Client terminal already delivered at ingress.

Verification:

- Cloud main compile: exit `0`;
- `WubeiTaskTrackerTurnContractTest`: `5/5`;
- the existing Xiuluo tracker family remains red before the changed branch because three fixtures
  cannot load `images/template/task/xiuluo_tracker_title.png` and one lacks the tracker local-service
  client. These failures were not hidden or repaired by changing production behavior.

No runtime, UI, capture or physical input was executed. Fresh acceptance requires a Cloud restart
and must show, for the leader window, `auto-combat enter detected`, panel bootstrap/`Alt+8` when
needed, delayed entry maintenance, and immediate pathing-terminal wake without the prior 164-second
observer delay.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-20 SOURCE-REPAIR-COMPLETE DIRECT-PATHING-INGRESS-WAKE XIULUO-WUBEI-CLIENT-RUNNER-MAINTENANCE CLOUD-COMPILE-0 WUBEI-5-5 XIULUO-OLD-FIXTURE-RED FRESH-CLOUD-RESTART-REQUIRED 2026-07-24 -->

## Repair #21 - heal-pet failure preserves the accepted objective (2026-07-24)

State: `SOURCE REPAIR COMPLETE / CLIENT+CLOUD COMPILE 0 / FRESH BOTH RESTART REQUIRED`.

Fresh evidence showed that Client Runner correctly emitted a terminal for
`xiuluo-v2:healPetNpc`, while the stale Cloud process remained in
`AFTER_ACCEPT_MAINTENANCE_CHECK` and eventually sent the phase through the
generic failure recovery. That recovery discarded the already accepted
objective and restarted the same round from `ACCEPT_TASK_*`, creating an
accept/heal-pet/reaccept loop.

The repaired contract is:

- each real Xiuluo round may start the heal-pet hook once after accepting the task;
- Client Runner still emits the existing stable terminal as soon as movement stops,
  with a `30s` hard terminal bound if coordinate capture or movement evidence never resolves;
- after the terminal, Cloud does not navigate to the healer a second time;
- NPC click, maintenance broadcast miss, or hook-local runtime failure ends only
  this round's optional maintenance attempt;
- the accepted objective is retained and the phase advances to
  `BEFORE_ROUTE_MAINTENANCE_CHECK`, then the normal Tracker/route/combat flow;
- no successful cooldown is written on failure, so the next real round may try
  heal-pet again;
- no heal-pet failure may call `restartRoundAfterPhaseFailure(...)` or return to
  `ACCEPT_TASK_*`.

Verification:

- Client `mvn -DskipTests=false compile`: `BUILD SUCCESS`;
- Cloud `mvn -DskipTests=false clean compile`: `BUILD SUCCESS`;
- no runtime, UI, capture or physical input was executed.

Fresh acceptance requires both Client and Cloud restart. In one exact round, a
failed heal-pet attempt must log `continue current accepted task` and advance to
the existing objective; any transition back to `ACCEPT_TASK_*` is a P1 failure.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-21 SOURCE-REPAIR-COMPLETE HEAL-PET-ONCE-PER-REAL-ROUND FAILURE-PRESERVES-OBJECTIVE NO-REACCEPT-LOOP CLIENT-COMPILE-0 CLOUD-CLEAN-COMPILE-0 FRESH-BOTH-RESTART-REQUIRED 2026-07-24T15:45:00-04:00 -->

## Repair #22 - pathing bound revision and Cloud startup wiring (2026-07-24)

State: `SOURCE REPAIR COMPLETE / CLOUD STARTUP SMOKE PASSED / FRESH BOTH RESTART REQUIRED`.

- The local Runner hard pathing bound is revised from `30s` to `60s` so long
  Five Ring routes are not prematurely classified. The existing `2.2s`
  stable-stop terminal remains unchanged and still wins immediately.
- Cloud startup failed before binding because
  `CloudXiuluoSummonSkillPreparedState` lived outside the host component-scan
  packages. Its `@Component` annotation therefore did not create a bean.
  `CloudTurnRuntimeConfiguration` now imports the existing state class
  explicitly; no second state store or replacement bean was introduced.
- Client compile and Cloud clean compile both passed.
- A direct isolated startup smoke test listened successfully on port `18082`
  and logged `DHXY cloud brain listening`; the smoke process was then stopped.
- The named activation test could not enter execution because four pre-existing
  test fixtures still instantiate the older `XiuluoTaskV2` constructor and fail
  during global `testCompile`. This was not hidden as a target-test result.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-22 SOURCE-REPAIR-COMPLETE PATHING-HARD-BOUND-60S STABLE-STOP-2P2S CLOUD-PREPARED-STATE-BEAN-WIRED STARTUP-SMOKE-18082-PASSED CLIENT-COMPILE-0 CLOUD-CLEAN-COMPILE-0 FRESH-BOTH-RESTART-REQUIRED 2026-07-24T15:53:00-04:00 -->

## Repair #23 - exact Xiuluo heal-pet live-test entry (2026-07-24)

State: `LIVE FLOW REACHED / PHYSICAL INPUT BLOCKED BY WINDOWS INTEGRITY / CLIENT+CLOUD STOPPED`.

The test entry `scripts/run-xiuluo-heal-pet-live-test.ps1` invokes the normal
`AutoBot -> XIULUO_V2` production flow for exactly one visible game window.
The test-only environment flag preserves the existing role-aware registration,
sets the genuine Cloud `BotProperties` bean to one run with maintenance due
immediately, and disables unrelated startup UI preparation. It does not copy a
heal-pet method, construct a synthetic Xiuluo phase, or introduce a second task
implementation.

Fresh run `remote-turn-570c68b1-7584-4236-8fe7-957262ab8ce3` proved:

- `skeleton started: maxRuns=1 maintenanceRunImmediatelyOnStart=true`;
- `startup tracker hit: source=startup-screen-resume links=1`;
- the real `heal-pet hook pre-NPC cleanup` and production navigation executed
  toward the Xiuluo healer at baseline coordinate `(116,70)`.

The live action could not complete because the Java process launched from the
ordinary terminal ran below the game process integrity level. Every HWND
shortcut returned Windows `lastError=5`, and physical cursor movement was also
denied; navigation therefore returned `POINT_NOT_REACHED`. This is a launch
privilege failure, not evidence that the heal-pet branch was skipped. The Client
and Cloud processes were stopped and port `18080` was released. Re-run the same
script from a terminal/IDE at the same integrity level as the game for visual
acceptance.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-23 LIVE-TEST-ENTRY REAL-XIULUO-FLOW STARTUP-TRACKER-HIT HEAL-PET-HOOK-REACHED TARGET-116-70 INPUT-BLOCKED-WIN32-ERROR-5 CLIENT-CLOUD-STOPPED 2026-07-24T16:11:00-04:00 -->

## Repair #24 - temporary manual-UI heal-pet acceptance mode (2026-07-24)

State: `READY FOR USER-RUN FRESH CLOUD RESTART`.

For the user's manual UI run only, the Cloud defaults temporarily set
`xiuluoMaintenanceRunImmediatelyOnStart=true` and
`taskStartupPreparationEnabled=false`. Starting `XIULUO_V2` from the existing
Client UI therefore skips unrelated `Alt+1/U/6` startup preparation and, when
the current Tracker already contains an accepted Xiuluo task, follows the real
`XiuluoTaskV2` hot-start path directly into its existing heal-pet maintenance
hook. No synthetic task phase or duplicate heal-pet implementation was added.

Cloud `mvn -DskipTests=false compile` passed. Fresh acceptance requires a Cloud
restart. Do not run other task types while these temporary global defaults are
active; restore both defaults after the manual verification.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-24 TEMP-MANUAL-UI HEAL-PET-IMMEDIATE STARTUP-PREP-DISABLED CLOUD-COMPILE-0 FRESH-CLOUD-RESTART-REQUIRED RESTORE-DEFAULTS-AFTER-ACCEPTANCE 2026-07-24T16:35:00-04:00 -->

## Repair #25 - Runner pathing terminal single-hop authority (2026-07-24)

State: `SOURCE REPAIR COMPLETE / CLIENT+CLOUD COMPILE 0 / FRESH BOTH RESTART REQUIRED`.

Fresh evidence:

- Client emitted `Local pathing terminal observed` at `16:17:34.524` for repair intent
  `173878cb-e41d-4eed-8231-a900aad4cf5b`;
- Cloud remained parked in `WAIT_TARGET_PATHING_TERMINAL` and reached the existing
  `180s` pre-combat watchdog;
- no repair `NPC_CLICK_SMART`/tooltip match ran, so the visible repair tooltip was not a
  template miss. The task never reached that phase.

Approved repair:

1. Client Runner remains the sole pathing-state authority. The temporary `60s` hard
   pathing timeout is removed; only the existing `2.2s` stable coordinate-strip verdict
   creates `STOPPED_AWAY`.
2. `CloudWholeTaskReadyEventState` registers the exact observation-run/business-run
   binding and receives accepted typed pathing facts directly from
   `CloudWindowObservationInbox`. An exact terminal is published once for the same
   tenant/device/window/run/intent.
3. `CloudFastExpectedCombatExitCoordinator` no longer installs a pathing listener,
   mirrors pathing, claims pathing terminal ids, or publishes pathing wakes. Its remaining
   combat/replay responsibilities are unchanged in this repair.
4. `CloudWholeTaskObserver` no longer polls typed pathing, reads coordinate frames to
   classify movement/arrival, updates a Cloud verdict, or republishes pathing terminals.
   Active pathing no longer requests a Cloud coordinate-strip ROI.
5. `CloudNavigationPathingState` remains only as an exact Client-fact compatibility cache
   for current Xiuluo/Wubei/Wuhuan consumers. It has no pathing decision authority.
6. Xiuluo's separate `RUNNER_PATHING_HARD_TIMEOUT_MS` clear branches are removed. The
   only timeout fallback retained is the existing whole pre-combat `180s` watchdog.

Business contract: no approved task-phase, OCR/template, click, navigation-order, retry,
or fallback-order difference. This repair changes only fact delivery and ownership.

Verification:

- Client `mvn -q -DskipTests compile`: exit `0`;
- Cloud `mvn -q -DskipTests=false compile`: exit `0`;
- no local tests were run under the repository's no-local-test rule;
- no runtime, UI, capture, or physical input was executed.

Fresh acceptance requires both Client and Cloud restart. After a new
`Local pathing terminal observed`, Cloud must immediately log one
`cloud.whole-task.ready.publish ... type=PATHING_TERMINAL` for the same intent and the
owning task must leave `WAIT_TARGET_PATHING_TERMINAL` without waiting for the periodic
observer or the `180s` watchdog.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-25 SOURCE-REPAIR-COMPLETE RUNNER-SOLE-PATHING-AUTHORITY DIRECT-INBOX-TO-READY-EVENT NO-COORDINATOR-PATHING NO-CLOUD-PATHING-OBSERVER NO-60S-HARD-TIMEOUT CLIENT-COMPILE-0 CLOUD-COMPILE-0 FRESH-BOTH-RESTART-REQUIRED 2026-07-24 -->

## Repair #26 - replacement lineage is one-shot (2026-07-24)

State: `SOURCE REPAIR COMPLETE / CLIENT COMPILE 0 / FRESH CLIENT RESTART REQUIRED`.

Fresh run `remote-turn-be264f75-adb4-4d70-92ee-db2550a50fb3` proved that Repair #25's
single-hop delivery worked for the first heal-pet intent
`f852c565-8e30-4aca-aa1c-73d58967a6b2`: Client emitted the local terminal and Cloud
published `PATHING_TERMINAL`. `NavigationService` then issued fallback intent
`058954b3-1c40-405a-ad03-d6c505342c44`. Client emitted its terminal at `17:14:50.996`,
but Cloud published no matching event and retained the intent as `ACTIVE`; consequently
the Xiuluo phase never reached `NPC_CLICK_SMART`.

Root cause was in `WindowObservationSampler.sampleCurrentPathingFact()`. After the first
`REPLACED(oldIntent)` fact introduced the new intent, every later fact for that same
intent incorrectly repeated the `REPLACED` transition. Cloud's forward-only lineage guard
correctly rejected the repeated replacement because the current intent was already the
new intent.

The Client now emits `REPLACED(oldIntent)` only for the first fact of a new intent.
Subsequent ACTIVE and terminal facts for that intent use `CURRENT`. No navigation,
maintenance, NPC-click, retry, OCR, template, or Cloud business policy changed.

Verification: Client `mvn -q -DskipTests compile` exited `0`. Fresh acceptance requires
a Client restart. For the fallback intent, logs must contain both
`Local pathing terminal observed` and a Cloud
`cloud.whole-task.ready.publish ... type=PATHING_TERMINAL` carrying the same intent;
the next Xiuluo step must then enter the existing heal-pet NPC attempt instead of
remaining in `runner-only pathing wait`.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-26 SOURCE-REPAIR-COMPLETE REPLACED-EDGE-ONE-SHOT SAME-INTENT-CURRENT TERMINAL-NO-LONGER-DROPPED CLIENT-COMPILE-0 FRESH-CLIENT-RESTART-REQUIRED 2026-07-24 -->

## Repair #27 - terminal-only Cloud coordinate recognition (2026-07-24)

State: `SOURCE REPAIR COMPLETE / CLIENT+CLOUD COMPILE 0 / FRESH BOTH RESTART REQUIRED`.

Fresh run `remote-turn-18832f76-5f87-4421-a50e-da30dc9cd25c` reached the heal-pet
NPC but opened the mini-map repeatedly. Every local terminal was accepted as
`STOPPED_AWAY current=(null,null)`, so the unchanged navigation candidate loop could
not prove arrival and consumed fallback offsets 1 through 7. After its 60-second timeout,
the next Xiuluo turn called `PlayerStateService.syncMyPosition()`, recognized `(117,69)`,
and immediately proved arrival near `(116,71)` before the existing NPC click succeeded.

The missing contract edge is now restored through the existing observation protocol:

1. movement sampling remains a local raw-pixel comparison and uploads no periodic coordinate frames;
2. once the Client Runner proves a terminal, its observation request attaches the already captured
   exact-HWND `coordinate-strip` ROI correlated to that intent;
3. Cloud recognizes that one ROI before accepting/publishing the same pathing fact and enriches only
   its current map/X/Y; it never changes the Client-owned terminal state;
4. the response carries the existing `analysisResults` ACK, so Client retries only after transport
   failure and stops uploading after a successful response.

No `NavigationService` candidate, tolerance, retry, NPC-click, phase, OCR/template, or input policy
changed. Client `mvn -q -DskipTests compile` and Cloud
`mvn -q -DskipTests=false compile` both exited `0`. No runtime/UI/capture/input was run.
Fresh acceptance requires both processes restarted and must show
`Pathing terminal coordinate resolved ... coord=(...)` before the matching
`PATHING_TERMINAL`; an in-tolerance first terminal must proceed without exhausting fallback offsets.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-27 SOURCE-REPAIR-COMPLETE TERMINAL-ONLY-COORDINATE-ROI CLOUD-RECOGNITION SAME-INTENT-ENRICHMENT ANALYSIS-ACK CLIENT-COMPILE-0 CLOUD-COMPILE-0 FRESH-BOTH-RESTART-REQUIRED 2026-07-24 -->

## Repair #28 - generic four-edge ROI clipping and stage continuation (2026-07-27)

State: `SOURCE REPAIR COMPLETE / FOCUSED TESTS + CLIENT COMPILE 0 / FRESH CLIENT RESTART REQUIRED`.

Fresh five-ring evidence showed four windows visibly in combat but no Client
`IN_COMBAT`, no Cloud `COMBAT_STATE_CHANGED`, and therefore no `Alt+8`.
The exact frame was preserved as
`images/test-cases/combat-entry/wuhuan-four-window-missed-entry.png`.

Root cause:

1. the first local combat ROI was `(974,630,51,20)` on a `1024x768` frame, so
   its requested right edge was `1025`;
2. the old crop contract rejected the complete ROI for that one-pixel overflow;
3. `LocalCombatSignalMechanics.sample()` returned unavailable immediately and
   never inspected the later selection/top stages, although replay scores on
   that same frame strongly matched their templates.

Approved program-wide screenshot contract:

- ROI cropping intersects the requested rectangle with the actual image on all
  four edges: left, top, right, and bottom;
- a partial overlap returns the visible clipped image; only no overlap fails;
- the rule applies to the generic bound-HWND capture entry and the Runner shared
  cycle-frame crop entry, not to one task or one combat template;
- Cloud operation validation and physical-input target validation remain strict
  because they validate commands, not image cropping.

The local multi-stage combat detector now records unavailable stages and
continues. A later visible stage wins. If no stage matches and any stage was
unavailable, the final result remains unavailable rather than incorrectly
declaring combat absent.

Verification:

- `LocalCombatSignalMechanicsTest#unavailableFlagDoesNotHideVisibleSelectionStage`
  passed;
- `LocalCombatSignalMechanicsTest#sharedFrameCropClipsAllFourEdges` passed;
- Client `mvn -q -DskipTests compile` exited `0`;
- marked replay:
  `images/test-cases/combat-entry/wuhuan-four-window-missed-entry-marked.png`.

Fresh acceptance requires only a Client restart. The same four-window scenario
must publish the local combat edge and restore existing `Alt+8` maintenance.
Cloud source was not changed.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-28 SOURCE-REPAIR-COMPLETE GENERIC-FOUR-EDGE-ROI-CLIP STAGE-UNAVAILABLE-CONTINUES LATER-VISIBLE-WINS FAIL-CLOSED-NO-MATCH FOCUSED-TESTS-0 CLIENT-COMPILE-0 FRESH-CLIENT-RESTART-REQUIRED 2026-07-27 -->

## Repair #29 - baseline recognized-location stop contract (2026-07-28)

State: `SOURCE FIX IMPLEMENTED / POLICY TEST 5/5 / CLIENT COMPILE 0 /
FRESH CLIENT RESTART REQUIRED`.

Fresh five-ring evidence:

- `67555 / hwnd-25F1CE8` clicked its Tracker green link at `00:12:14.200` and
  registered untargeted intent `ef238a52...`;
- the Client classified the first recognized location
  `大雁塔一层(128,82)` as `STOPPED_AWAY` at `00:12:36.061`;
- later exact-window frames show the same character continuing through
  `(66,82)` and `(56,74)`;
- Cloud correctly consumed the supplied terminal and clicked the same Tracker
  link again at `00:12:39.503`. The duplicate click was downstream evidence,
  not the source defect.

The migration had weakened the `696a12b0` / `9aa987d1` contract. Baseline
`WindowTaskRunner` compared successive recognized map/X/Y values: a first
location or any changed location remained `ACTIVE`, and only an unchanged
recognized location lasting `2200ms` could become `STOPPED_AWAY`. The migrated
sampler instead treated `2200ms` of local pixel stability plus one recognized
coordinate as a terminal.

Repair:

1. retain the approved `45x12` coordinate-digit movement ROI unchanged;
2. retain per-intent recognized map/X/Y and its changed timestamp;
3. the first result and every changed map/X/Y remain `ACTIVE`;
4. request a fresh recognized location at the baseline `2000ms` minimum cadence;
5. publish `STOPPED_AWAY` only when both local pixel stability and recognized
   location stability satisfy the existing `2200ms` boundary;
6. targeted CR142 `ARRIVED` remains first in the classifier and is unchanged.

No Cloud source, Five Ring recovery policy, Tracker click, navigation, NPC,
template, OCR algorithm, or physical-input behavior changed.

Verification:

- `WindowObservationPathingPolicyTest`: `5/5`;
- Client `mvn -q -DskipTests=false compile`: exit `0`;
- a combined historical observation run exposed five pre-existing fixture
  failures outside this repair: one stale repeated-`REPLACED` expectation and
  four shared-frame total-capture-count assertions. Production semantics were
  not changed to satisfy those stale fixtures.

Fresh acceptance: restart the Client. For the incident shape, the first
`大雁塔一层(128,82)` result must log `coordinateChanged=true` and remain
`ACTIVE`; later coordinate movement must reset the recognized-location clock
and no second Tracker click may occur.

<!-- TRUE_EOF: LOCAL-RUNNER-AUTHORITY-P1 REPAIR-29 BASELINE-RECOGNIZED-LOCATION-STOP FIRST-COORDINATE-ACTIVE CHANGED-COORDINATE-ACTIVE UNCHANGED-2200MS-TERMINAL KEEP-45X12-ROI POLICY-5OF5 CLIENT-COMPILE-0 FRESH-CLIENT-RESTART-REQUIRED 2026-07-28 -->
