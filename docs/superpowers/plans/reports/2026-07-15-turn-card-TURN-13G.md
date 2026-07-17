# TURN-13G Worker Delivery Report

## CLAIMED

- claimedAt: `2026-07-15T18:36:33.4621558-04:00`
- worker: `CR271 TURN-13G implementation Worker`
- role: production/test implementer only; not reviewer
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- startDependsOn: `S=TURN-02R+TURN-13` (`SOURCE START GATE SATISFIED`)
- approvalDependsOn: `A=TURN-T01+TURN-T02 relevant` (`PENDING`; does not authorize Worker approval)
- Cloud branch observed before editing: `navigation-migration`
- repository state: both repositories contain protected pre-existing dirty/untracked work; no revert, overwrite, clean, stage, commit, or other Git mutation is permitted

### Exact frozen write set

Cloud production:

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClient.java` (create)
2. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnInvocationContext.java` (create)
3. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnInvocationContextProvider.java` (create)
4. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/LegacyTaskExecutionTurnContextProvider.java` (create)
5. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnInvocationResult.java` (create)
6. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnLocalServiceResult.java` (create)
7. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/CloudTurnActionFactory.java` (modify)

Cloud test:

8. `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClientContractTest.java` (create)

Durable report:

9. `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-13G.md` (create/append)

No DTO may be added outside these files; any card-local DTO is a nested record in the frozen files. All other production, tests, POMs, plans, cards, and reports are read-only to this Worker.

### Frozen implementation contract

- One public Cloud Task invocation creates exactly one UUID `actionId`.
- Exact `deviceId/windowId` come only from `TurnInvocationContextProvider`.
- Command execution uses only the existing `CloudTurnCommandPort.execute(...)` once.
- `COMPLETED` preserves the real outcome and optional exact raw frame.
- `FAILED`, `STOPPED`, `BUSY`, `DUPLICATE`, `TIMED_OUT_UNCERTAIN`, and `INTERRUPTED_UNCERTAIN` remain typed non-success results; none is converted to business success or `false`.
- No automatic retry or second command is allowed.
- Latest window metadata is read-only context input; this card creates no second cache.
- No owner, permit, session, ledger, TTL, durable workflow, or retry state may be introduced.
- Test profile: `BC4+BASE+EX+IMG`, including scripted COMPLETED/FAILED/STOPPED/UNCERTAIN outcomes, optional frame, one command call, stable actionId, raw PNG defensive copying, and zero second command.

Final Worker handoff may state only `SOURCE+TEST DELIVERED` with command evidence, or precise delivery evidence if an external compile/test prerequisite fails. Worker will not write `APPROVED` or a reviewer `BLOCKED` judgment.

## SOURCE+TEST DELIVERED

- deliveredAt: `2026-07-15T18:54:04.9775634-04:00`
- role: implementation Worker only; this section is not a review or card-approval verdict
- write-set check: exactly the seven frozen production paths, the one frozen test path, and this report were
  written; no production/test/POM outside the frozen set was changed
- baseline statement: `无已批准业务差异；本卡不改变 696a12b0 业务条件、顺序、retry、fallback、park 或 terminal 语义。`

### Delivered production behavior

1. `TurnGameClient` exposes capture, ordered closed-step execution, one permanent-local-Service invocation, and
   read-only latest-window metadata. Every valid action invocation passes through one private boundary that reads
   the exact provider context, obtains one UUID, builds one validated `TurnAction`, and calls
   `CloudTurnCommandPort.execute(...)` exactly once.
2. `TurnInvocationContext` retains exact nonblank `deviceId/windowId`; `LegacyTaskExecutionTurnContextProvider`
   projects only `TaskExecutionContext.getScope().deviceId()` and `getWindowId()` with no fallback or extra state.
3. `TurnInvocationResult` keeps command status separate from the real `TurnOutcome.Status`, checks exact
   action/device/window correlation, keeps COMPLETED outcome/frame pairs atomic, and defensively copies raw PNG
   bytes. It defines no business-success boolean.
4. `TurnLocalServiceResult` retains the invocation plus the real typed `LOCAL_SERVICE` step result. Missing outcome
   under BUSY/DUPLICATE/TIMED_OUT_UNCERTAIN/INTERRUPTED_UNCERTAIN remains missing rather than becoming `false`.
5. `CloudTurnActionFactory.action(...)` accepts one already ordered closed-step list; the existing `input(...)`
   method delegates to the same validator without changing prior callers.
6. No automatic retry, second command, metadata cache, owner, permit, session, ledger, TTL, thread, loop, durable
   workflow, runtime activation, capture, input, UI, or OCR was added.

### Contract-test coverage

`TurnGameClientContractTest` contains six fake-only tests covering `BC4+BASE+EX+IMG` at this card boundary:

- one fixed UUID generation, exact device/window propagation, one command call, stable returned actionId;
- exact ordered-step preservation through the generic payload entry;
- real COMPLETED, FAILED, STOPPED, and DUPLICATE_OR_UNCERTAIN outcome statuses;
- command BUSY, DUPLICATE_ACTION_ID, TIMED_OUT_UNCERTAIN, and INTERRUPTED_UNCERTAIN with null business outcome and
  zero implicit re-execution;
- typed local-Service step result with no second command;
- optional exact frame metadata/raw bytes and constructor/accessor defensive-copy behavior;
- latest metadata read through the exact context with zero UUID generation, zero action, and zero execute call.

### Command evidence

1. Required standard command, fresh after final source/test edits:

   - working directory: `D:\mavenProject\dhxy-cloud-brain`
   - command: `mvn -q -Dtest=TurnGameClientContractTest test`

   - exit code: `1`
   - tests reached/run: `0` (main-source compilation stopped the lifecycle first)
   - failures/errors: no Surefire test result was produced
   - external compile evidence begins at existing non-card paths:
     - `com/bot/dhxy/service/TaskTrackerPanelService.java`: missing `GameClientTracker`, `TextRecognizer`,
       `CoordinateHelper`, `OcrWindowScanService`, `WindowScopedTempPath`;
     - `com/bot/dhxy/task/wubei/WubeiTask.java`: missing `GameClientTracker`, `TextRecognizer`, `BagService`,
       `UICleanerService`, `TaskTransactionRunner`, `TaskTurnCoordinator`, window runtime classes, and metrics;
     - the same full-repository debt continues through existing `NavigationService`, `NpcClickService`,
       `DialogService`, `AutoCombatService`, `PlayerStateService`, `TeamReturnService`, and `SummonSkillService`.
   - the Maven diagnostics did not name a TURN-13G production or test path before javac stopped at the existing
     repository error set. This evidence does not convert the required Maven command into a pass.

2. Required applicable compile command:

   - working directory: `D:\mavenProject\dhxy-cloud-brain`
   - command: `mvn -q compile`

   - exit code: `1`
   - result: the same pre-existing full-repository missing-class cohort above; no TURN-13G path appeared in the
     emitted diagnostics.

3. Narrow source-closure diagnostic for the new gateway and every production dependency reachable from it:

   `javac --release 21 -encoding UTF-8 -cp <Maven dependency classpath> -sourcepath src/main/java -d target/turn13g-isolated/classes src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClient.java src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnInvocationResult.java`

   - exit code: `0`
   - this compiled `TurnGameClient`, context/result/local-result types, factory, command port/result/frame, and the
     referenced protocol closure.

4. Separate legacy-provider closure diagnostic:

   `javac --release 21 -encoding UTF-8 -cp <isolated classes + Maven dependencies> -sourcepath src/main/java -d target/turn13g-isolated/legacy-classes src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/LegacyTaskExecutionTurnContextProvider.java`

   - exit code: `1`
   - external dependency diagnostics only: existing `TaskExecutionContextHolder.java:53` calls absent
     `TaskExecutionContext.isPauseRequested()`, and implicit Lombok compilation of existing
     `TaskRetryPolicy.java:14,21` cannot resolve generated `builder()` in this narrow javac mode;
   - no diagnostic names `LegacyTaskExecutionTurnContextProvider.java` itself. No out-of-scope repair was made.

5. Narrow test compile and execution, used only to diagnose this card while the standard Maven lifecycle remains
   unavailable:

   - isolated `javac` of `TurnGameClientContractTest.java`: exit code `0`;
   - JUnit Platform `1.10.2`, selecting only
     `com.yueyunfe.dhxy.cloudbrain.turn.client.TurnGameClientContractTest`: exit code `0`;
   - tests found/started/successful/failed/errors: `6/6/6/0/0`;
   - containers found/started/successful/failed: `4/4/4/0`.

The isolated result is diagnostic evidence, not a substitute for the required standard Maven command. The parent
retains the full-repository Maven gate and the independent source/test review. This Worker makes no approval claim.

## PARENT SOURCE/TEST REVIEW #1 - REPAIR REQUIRED

- reviewedAt: `2026-07-15T19:02:37-04:00`
- reviewer: `CR271 parent / sole manager and final reviewer`
- verdict: `P0/P1/P2=0/2/0 / REPAIR #1`
- retained findings: one public invocation creates one UUID, calls `CloudTurnCommandPort.execute(...)` once, preserves
  typed command/outcome states, uses exact provider device/window context, and performs no automatic retry, session,
  ledger, TTL, thread, capture, input, or runtime activation.

### P1-1 - Submitted steps are not correlated with returned step results

- Evidence: `TurnInvocationResult.from(...)` validates `actionId` and device/window at
  `TurnInvocationResult.java:62-74`, then accepts the outcome without comparing `action.steps()` with
  `outcome.stepResults()`. `TurnProtocolValidator.requireValid(TurnOutcome)` only validates each result internally;
  it does not know the submitted action. A valid outcome can therefore carry fewer results or a different step type
  while retaining the same action/window IDs.
- Impact: Cloud business code can consume a syntactically valid result for a different step shape, violating the
  frozen `EX` requirement for exact action correlation and potentially interpreting the wrong typed result.
- Repair condition: within `TurnInvocationResult.from(...)`, fail closed unless result count equals submitted step
  count and every result index/type equals the corresponding submitted step index/type. Add contract cases proving
  count mismatch and type mismatch are rejected, together with wrong actionId/device/window rejection. Do not add a
  retry, second command, fallback, session, or new state.

### P1-2 - The IMG test payload is not a decodable PNG

- Evidence: `TurnGameClientContractTest.pngFixture():254-258` returns only the eight-byte PNG signature followed by
  ASCII `TURN-13G`; it has no IHDR/IDAT/IEND chunks and cannot be decoded as the asserted `2x2` image.
- Impact: the test can pass raw-byte defensive-copy assertions while its SHA/dimensions metadata describe an image
  that never existed, so the card's `EX+IMG` acceptance profile has a false-positive path.
- Repair condition: make the test use a real deterministic decodable `2x2` PNG within the existing test file and
  assert decoded dimensions plus exact SHA/raw-byte preservation and defensive copying. No new fixture or write-set
  expansion is authorized.

### Frozen Repair #1 write set

Only these existing paths may be changed:

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnInvocationResult.java`
2. `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClientContractTest.java`
3. `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-13G.md`

All other production, tests, fixtures, POMs, documents, and Git metadata remain read-only. The original Worker must
append Repair #1 delivery evidence and rerun the named Maven test/compile truthfully; isolated execution remains
diagnostic only. This review is not card approval.

## REPAIR #1 DELIVERED

- deliveredAt: `2026-07-15T19:11:34-04:00`
- worker role: original TURN-13G implementation Worker; no reviewer or approval claim
- baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- repair write set used exactly:
  1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnInvocationResult.java`
  2. `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClientContractTest.java`
  3. this report

### P1-1 repair evidence

`TurnInvocationResult.from(...)` now calls `requireExactStepCorrelation(...)` after exact actionId and
device/window correlation. The boundary fails closed unless:

- `outcome.stepResults().size()` equals `action.steps().size()`; and
- every returned item at the same position has the exact submitted `index` and `type`.

The contract test adds five explicit rejection cases: result-count mismatch, result-type mismatch, wrong actionId,
wrong deviceId, and wrong windowId. Every case asserts one UUID generation, exactly one
`CloudTurnCommandPort.execute(...)` call, and one submitted action before the exception. No retry, second command,
fallback, metadata read, session, owner, permit, ledger, TTL, durable state, or replacement business result was
added.

### P1-2 repair evidence

The prior signature-plus-ASCII payload was replaced by an inline deterministic 84-byte PNG with complete PNG
chunks. The test now proves all of the following:

- `ImageIO.read(...)` decodes it successfully as exactly `2x2`;
- all four ARGB pixels equal the fixed source values;
- the raw bytes have the fixed SHA-256
  `31a38d5b7d06e5de1009ec59851b3654470f6fc84e0bde0320bc0084d2f8a3d3`;
- the outcome/frame metadata preserves that exact SHA and the returned frame preserves the exact raw bytes; and
- mutating both the constructor source array and an accessor-returned array cannot mutate a later returned frame.

No external fixture or extra production/test file was added.

### Final command evidence after Repair #1

1. Required named Maven test:

   - working directory: `D:\mavenProject\dhxy-cloud-brain`
   - command: `mvn -q -Dtest=TurnGameClientContractTest test`
   - exit code: `1`
   - tests reached/run: `0`; no `TurnGameClientContractTest` Surefire report exists
   - Maven stopped during full main-source compilation at existing non-card files. The first stable error groups are
     `com/bot/dhxy/service/TaskTrackerPanelService.java` (missing `GameClientTracker`, `TextRecognizer`,
     `CoordinateHelper`, `OcrWindowScanService`, and `WindowScopedTempPath`) and
     `com/bot/dhxy/task/wubei/WubeiTask.java` (missing local services, transaction/runtime, metrics, and window
     classes). No TURN-13G source or test path appeared in the emitted diagnostics.

2. Applicable Cloud compile:

   - working directory: `D:\mavenProject\dhxy-cloud-brain`
   - command: `mvn -q compile`
   - exit code: `1`
   - result: the same existing full-repository missing-class cohort; no TURN-13G path appeared in the emitted
     diagnostics.

3. Isolated source/test diagnostic, not a substitute for either required Maven command:

   - fresh output directory: `target/turn13g-repair1-final`
   - selected production closure `javac --release 21`: exit code `0`
   - `TurnGameClientContractTest` `javac --release 21`: exit code `0`
   - JUnit Platform `1.10.2`, selecting only
     `com.yueyunfe.dhxy.cloudbrain.turn.client.TurnGameClientContractTest`: exit code `0`
   - tests found/started/successful/failed/aborted: `11/11/11/0/0`
   - containers found/started/successful/failed: `4/4/4/0`

Repair #1 is delivered for parent re-review. The standard Maven gates remain unpassed because they do not reach
this card through the existing full-repository compile debt; the isolated result is recorded only as diagnostic
evidence. This Worker does not mark the card approved or closed.

## PARENT SOURCE/TEST REVIEW #2 - REPAIR #2 REQUIRED

- reviewedAt: `2026-07-15T19:14:26-04:00`
- verdict: `P0/P1/P2=0/1/0 / REPAIR #2`
- closed from Review #1: the deterministic 84-byte PNG decodes as the asserted 2x2 pixels and has the asserted SHA;
  constructor/accessor copies are tested. COMPLETED result-count/type mismatch and action/device/window mismatch now
  fail closed after one command with no retry.

### P1-1 - Exact-step check expands frozen STOPPED/UNCERTAIN semantics

- Evidence: `TurnInvocationResult.from(...)` now calls `requireExactStepCorrelation(...)` for every non-null outcome
  at `TurnInvocationResult.java:69-77`; `:88-103` requires result count equal to submitted step count for every
  status. The canonical `outcome-duplicate-or-uncertain.json` intentionally has an empty `stepResults` list.
  TURN-01D's frozen parent decision states STOPPED and DUPLICATE_OR_UNCERTAIN retain only their existing
  no-`failedStepIndex` rule and must not gain new semantics.
- Impact: a valid typed duplicate/uncertain outcome for a non-empty action is converted into a local exception,
  defeating the required “preserve uncertain; do not map to success/false or retry” gateway behavior.
- Repair condition: require full submitted-step count/index/type correlation only for COMPLETED and FAILED outcomes.
  Preserve actionId/device/window correlation for every outcome, but do not add step-list shape requirements to
  STOPPED or DUPLICATE_OR_UNCERTAIN. Add a direct test that a canonical empty-step DUPLICATE_OR_UNCERTAIN outcome is
  returned typed after one UUID/one command with no retry. Keep the Review #1 PNG and mismatch coverage intact.

### Frozen Repair #2 write set

The same three Repair #1 paths only. No other production/test/fixture/POM/document/Git changes are authorized.
This correction restores the frozen wire semantics; it does not authorize a new retry, fallback, status, or state.

## REPAIR #2 DELIVERED

- deliveredAt: `2026-07-15T19:17:42-04:00`
- worker role: original TURN-13G implementation Worker; no reviewer or approval claim
- repair write set used exactly:
  1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnInvocationResult.java`
  2. `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClientContractTest.java`
  3. this report

### Frozen status semantics restored

`TurnInvocationResult.from(...)` still correlates exact actionId and exact device/window for every returned outcome.
Full submitted-step count/index/type correlation is now invoked only when `outcome.status()` is `COMPLETED` or
`FAILED`. `STOPPED` and `DUPLICATE_OR_UNCERTAIN` gain no new step-list shape rule; their protocol validation remains
the already-frozen validator behavior, including the no-`failedStepIndex` rule.

The new direct contract case submits a non-empty WAIT action and scripts a canonical
`DUPLICATE_OR_UNCERTAIN` outcome with `failedStepIndex=null` and `stepResults=[]`. It verifies:

- one UUID generation and exactly one `CloudTurnCommandPort.execute(...)` call;
- the exact non-empty submitted action is preserved;
- zero metadata reads and no retry/second command/fallback;
- a typed `CloudTurnCommandResult.Status.COMPLETED` invocation containing the real
  `TurnOutcome.Status.DUPLICATE_OR_UNCERTAIN` outcome;
- null `failedStepIndex`, canonical empty step results, and no frame.

Repair #1's deterministic decodable PNG, fixed SHA/raw-byte/copy assertions, COMPLETED count/type mismatch
rejections, and wrong actionId/device/window rejections remain intact. No status, retry, fallback, state, fixture,
or additional file was added.

### Final command evidence after Repair #2

1. Required named Maven test:

   - working directory: `D:\mavenProject\dhxy-cloud-brain`
   - command: `mvn -q -Dtest=TurnGameClientContractTest test`
   - exit code: `1`
   - tests reached/run: `0`; no `TurnGameClientContractTest` Surefire report exists
   - Maven stopped during full main-source compilation at the existing non-card
     `TaskTrackerPanelService.java` and `WubeiTask.java` missing-class cohorts. No TURN-13G production or test path
     appeared in the emitted diagnostics.

2. Applicable Cloud compile:

   - working directory: `D:\mavenProject\dhxy-cloud-brain`
   - command: `mvn -q compile`
   - exit code: `1`
   - result: the same existing full-repository missing-class cohort; no TURN-13G path appeared in the emitted
     diagnostics.

3. Isolated source/test diagnostic, not a substitute for either required Maven command:

   - fresh output directory: `target/turn13g-repair2-final`
   - selected production closure `javac --release 21`: exit code `0`
   - `TurnGameClientContractTest` `javac --release 21`: exit code `0`
   - JUnit Platform `1.10.2`, selecting only
     `com.yueyunfe.dhxy.cloudbrain.turn.client.TurnGameClientContractTest`: exit code `0`
   - tests found/started/successful/failed/aborted: `12/12/12/0/0`
   - containers found/started/successful/failed: `4/4/4/0`

Repair #2 is delivered for parent re-review. The standard Maven gates remain unpassed because the lifecycle does
not reach this card through the existing full-repository compile debt; isolated execution is diagnostic only. This
Worker does not mark the card approved or closed.

## PARENT SOURCE/TEST REVIEW #3 - REPAIR #2 PASSED

- reviewedAt: `2026-07-15T19:20:31-04:00`
- reviewer: `CR271 parent / sole manager and final reviewer`
- verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / REQUIRED MAVEN+CLOUD COMPILE BLOCKED`
- Parent independently re-read `TurnInvocationResult.from(...)`. Exact actionId/device/window correlation remains
  mandatory for every real outcome, while full submitted-step count/index/type correlation is now applied only to
  `COMPLETED` and `FAILED`. `STOPPED` and `DUPLICATE_OR_UNCERTAIN` retain the frozen no-failed-index semantics without
  a new step-list shape rule.
- Parent independently re-read the new canonical uncertain test. A non-empty WAIT action receives
  `DUPLICATE_OR_UNCERTAIN` with empty `stepResults`, remains a typed real outcome, and records exactly one UUID and one
  command with no metadata read, retry, fallback or second command. Repair #1's decodable 2x2 PNG pixel/SHA/copy
  assertions and mismatch rejection cases remain intact.
- The exact Repair #2 write set is clean. Required named Maven and Cloud compile remain blocked before this card by
  existing full-repository missing-class cohorts; isolated `12/12` is diagnostic only. No Maven cohort was rerun while
  other Java writers were active.
- The implementation owner is released and TURN-13H is dependency-ready. No further TURN-13G repair is requested.

**No approved business differences; equivalent migration against baseline `696a12b0`.**
