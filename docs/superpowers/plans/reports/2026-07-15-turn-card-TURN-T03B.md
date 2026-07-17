# TURN-T03B - DHXY Executor / Loop / Guard Contract Tests

## CLAIMED - 2026-07-15T18:59:23.636-04:00

- Card: `TURN-T03B`
- Owner: `Nash / CR271 Worker B`
- Role: sole implementation owner; not reviewer. This report cannot write `APPROVED` or parent `BLOCKED`.
- State: `CLAIMED / IMPLEMENTATION IN PROGRESS`
- Parent prerequisite: TURN-T02 source review `P0/P1/P2=0/0/0`; Pasteur slot released.
- Coordination evidence: TURN-T03A true EOF records `P0/P1/P2=0/0/0 / TEST SOURCE REVIEW PASSED`, its owner released, and no T03B repair overlap.

### Frozen Baseline

- DHXY branch / HEAD: `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f`
- Cloud branch / HEAD (read-only): `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01`
- Claim baseline: all six owned test files and this report were absent immediately before this claim.
- Both repositories were already dirty/untracked. All pre-existing changes are protected; no revert, overwrite, cleanup, commit, or other Git mutation is authorized.

### Exact Write Set

Only these DHXY paths may be written:

1. `src/test/java/com/bot/dhxy/cloud/turn/LocalServiceExecutionContractTest.java`
2. `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`
3. `src/test/java/com/bot/dhxy/cloud/turn/WindowTurnLoopContractTest.java`
4. `src/test/java/com/bot/dhxy/cloud/turn/TurnLoopRegistryConcurrencyTest.java`
5. `src/test/java/com/bot/dhxy/cloud/turn/TurnModeGuardContractTest.java`
6. `src/test/java/com/bot/dhxy/cloud/turn/TurnConfigurationWiringContractTest.java`
7. `docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-T03B.md`

Production source, POM, `application.properties`, fixtures, TURN-T03A tests, every other test/document, the Cloud repository, and Git metadata are read-only.

### Frozen Acceptance Contract

- Profiles: `PG+EX+IMG+LX+LIFE`.
- `LocalServiceExecutionContractTest`: completed/failed typed JSON, optional Quest frame, illegal status/frame pairing rejection, and defensive copies.
- `LocalTurnActionExecutorContractTest`: ordered N-step execution; after failure remaining steps are `NOT_RUN`; `STOPPED` short-circuit; exact window correlation; at most one frame; later failure full-window evidence replaces prior unreturned success frame.
- `WindowTurnLoopContractTest`: matching ACK clears retained previous outcome/frame; failed or uncertain transport retains previous/actionId and never re-executes the action.
- `TurnLoopRegistryConcurrencyTest`: one live loop per window; start/stop/remove races cannot create a second loop; remove permanently retires the old loop.
- `TurnModeGuardContractTest`: local and remote start decisions are atomic under the same monitor and cannot both win for one window.
- `TurnConfigurationWiringContractTest`: inert bean construction starts zero thread, loop, application, server, Task, UI, capture, or input work.
- Tests use only fake/in-memory collaborators. No runtime/application/server/Task/UI/capture/input is started.

### Required Commands

Run separately from `D:/mavenProject/DHXY`:

1. `mvn -q -Dtest=LocalServiceExecutionContractTest test`
2. `mvn -q -Dtest=LocalTurnActionExecutorContractTest test`
3. `mvn -q -Dtest=WindowTurnLoopContractTest test`
4. `mvn -q -Dtest=TurnLoopRegistryConcurrencyTest test`
5. `mvn -q -Dtest=TurnModeGuardContractTest test`
6. `mvn -q -Dtest=TurnConfigurationWiringContractTest test`
7. `mvn -q -DskipTests compile`

Any shared `testCompile` debt will be reported with exact command, exit code, tests/failures/errors and first relevant diagnostics. It will not be bypassed and no out-of-scope test will be edited.

**No approved business differences; equivalent migration against baseline `696a12b0`.**

## PARENT TEST SOURCE REVIEW #3 - REPAIR #1 PASSED / MAVEN GATES PENDING

- reviewedAt: `2026-07-15T20:00:00-04:00`
- reviewer: `CR271 parent / sole manager and final reviewer`
- verdict: `P0/P1/P2=0/0/0 / REPAIR #1 TEST SOURCE REVIEW PASSED`
- Parent independently read the four repaired test sources and the directly covered production branches. The delivered
  SHA-256 values were recomputed and match the true EOF table exactly; the two read-only sibling tests also retain
  their recorded hashes.
- `WindowTurnLoopContractTest.java:58-108` now executes a real ROI `CAPTURE(UPLOAD_IMAGE)` plus input action, mutates
  only the request-local PNG passed to the uncertain exchange, then proves explicit restart sends the original
  decodable 2x2 pixels without another capture/input. The successful ACTION and following IDLE both carry the retained
  outcome/frame as expected, and the next request carries neither. This closes raw-frame retention, ACK clearing,
  actionId de-duplication and defensive-copy coverage without adding automatic retry.
- `TurnModeGuardContractTest.java:28-91,175-202` holds the local supplier inside the production monitor, then uses
  `ThreadMXBean` to require the remote contender's exact `BLOCKED` state and the local thread's lock-owner id before
  releasing it. This is bounded positive coordination rather than a sleep-only negative inference.
- `LocalTurnActionExecutorContractTest.java:58-115` records and asserts the exact production mechanics order
  `capture:roi -> input:submit -> capture:failure-evidence`, while retaining one full-window replacement frame and
  restoring the exact-window context.
- `TurnLoopRegistryConcurrencyTest.java:123-194,224-253` starts the old loop through real exchange entry, races
  stop/remove, accepts only the two production-legal remove outcomes, then unconditionally stops/removes and proves
  permanent retirement before creating the sole replacement. Every repaired helper/loop has bounded cleanup in
  `finally` and a final non-alive/non-running assertion.

The card is not Maven-approved: the six original named commands and applicable compile gates remain pending under the
parent-controlled stable-writer cohort. No isolated compiler/Surefire bypass is authorized.

**No approved business differences; equivalent migration against baseline `696a12b0`.**

## PARENT TEST SOURCE REVIEW #2 - SUPERSEDING REPAIR #1 REQUIRED

- reviewedAt: `2026-07-15T19:41:26-04:00`
- reviewer: `CR271 parent / sole manager and final reviewer`
- authoritative verdict: `P0/P1/P2=0/2/3 / REPAIR #1`
- This review supersedes Review #1's test-source `0/0/0`. The parent independently checked the non-binding helper
  risks against the frozen T03B acceptance and retained only the findings below. The eager HttpClient production P1
  is separately closed at T03A Repair #3 source level and is not assigned to this test worker.

### P1-1 - Loop retention test never carries a raw frame

- Evidence: both loop scenarios execute `TurnContractFixtures.clickAction(...)`; `assertPrevious(...)` at
  `WindowTurnLoopContractTest.java:103-109` explicitly asserts `optionalPng == null`.
- Impact: the frozen acceptance requires uncertain transport to retain previous outcome **and frame**, a successful
  ACK to clear both, and the request upload to be a defensive copy. Current test can pass if PNG is dropped, retained
  after ACK, or exposed for mutation.
- Repair condition: make the uncertain/restart scenario execute a real capture-upload action (it may also include the
  existing click), prove one physical execution, carry non-null decodable PNG on the uncertain request and restart,
  mutate one request's supplied byte array without corrupting later retained bytes, then prove accepted IDLE clears
  both outcome and PNG.

### P1-2 - Mode-monitor assertion is a scheduling false positive

- Evidence: `TurnModeGuardContractTest.java:57-63` counts `remoteAttempted` down before calling `startRemote`, then
  treats “remote did not finish within 100ms” as proof that it blocked on the same monitor. A descheduled remote thread
  satisfies that assertion without ever reaching the monitor.
- Impact: the core local/remote atomic-exclusion regression can pass even if the critical section is later split.
- Repair condition: before releasing the local supplier, deterministically observe the remote thread in
  `Thread.State.BLOCKED` on the held monitor (bounded wait with diagnostic failure), then retain the one-winner and
  supplier-count assertions.

### P2 repair conditions

1. `LocalTurnActionExecutorContractTest` currently proves ordered result records but not cross-collaborator mechanical
   order. Add one shared event log proving ROI capture -> input submission -> full-window failure-evidence capture in
   that exact order.
2. Every test that starts a loop/helper thread must use unconditional cleanup and bounded join/await in `finally`, then
   assert its owned thread/loop is no longer alive/running so a failed assertion cannot contaminate later tests.
3. Add one bounded concurrent stop/remove case after the loop has entered exchange. Whichever operation wins, finish
   cleanup, permanently retire the old loop and prove no second usable loop coexists.

### Frozen Repair #1 write set

Only `LocalTurnActionExecutorContractTest.java`, `WindowTurnLoopContractTest.java`,
`TurnLoopRegistryConcurrencyTest.java`, `TurnModeGuardContractTest.java` and this report may change.
`LocalServiceExecutionContractTest`, `TurnConfigurationWiringContractTest`, every production/POM/fixture/other test,
Cloud repository, plan/document and Git metadata are read-only. Keep the current bounded fake/Unsafe seam; this repair
does not authorize a production abstraction refactor. Maven remains deferred while Java writers are active.

**No approved business differences; equivalent migration against baseline `696a12b0`.**

## SOURCE DELIVERED + TEST SOURCES DELIVERED / TEST EXECUTION BLOCKED / PARENT REVIEW PENDING

- Delivery time: `2026-07-15T19:22:30.729-04:00`
- Implementation owner: `Nash / CR271 Worker B`
- Parent review: pending. This section is an implementation delivery record only; it does not approve or parent-block the card.
- Scope result: six owned DHXY contract-test classes were created; no production, POM, configuration, fixture, TURN-T03A test, other document/test, Cloud file, or Git metadata was written.
- Runtime safety: all collaborators are fake/in-memory. No application, server, Task, UI, capture device, input device, or production runner was started.
- Business baseline: `无已批准业务差异；按 696a12b0 等价迁移`.

### Delivered Coverage

1. `LocalServiceExecutionContractTest`
   - Verifies completed/failed typed JSON, optional valid `QUEST_DETAIL` PNG, illegal status/frame-purpose/SHA/dimension pairings, and construction/read defensive copies.
2. `LocalTurnActionExecutorContractTest`
   - Verifies exact-window resolution; ordered N-step failure with later `NOT_RUN`; `STOPPED` short-circuit; single frame slot; and full-window `FAILURE_EVIDENCE` replacement of an earlier ROI capture.
   - Shared T03B fixtures allocate an inert `WindowTaskRunner` test double without invoking its production constructor, so no runner executor or lifecycle thread is created.
3. `WindowTurnLoopContractTest`
   - Verifies ACK clearing of previous outcome, actionId result caching, retained previous/actionId after uncertain transport, explicit restart behavior, no automatic retry, and no second physical execution.
4. `TurnLoopRegistryConcurrencyTest`
   - Verifies one loop per window, running-loop remove rejection, permanent retirement after remove, and one-winner start/remove races with no two usable loops.
5. `TurnModeGuardContractTest`
   - Verifies local submit and remote create/start share one monitor, only one mode can win, the losing local supplier is not called, and remote start requires the exact registered non-shutdown local runner.
6. `TurnConfigurationWiringContractTest`
   - Verifies direct inert bean construction creates no loop, runner lookup, capture, input, application, or new live thread; invalid properties fail before lifecycle wiring.

Profiles covered: `PG+EX+IMG+LX+LIFE`. Total delivered test methods: `16`.

### Required Command Results

All six point-test commands were rerun against the same final source snapshot. Maven failed during the shared `testCompile` phase before Surefire selected any requested class.

| Command | Maven exit | Tests | Failures | Errors | Result |
|---|---:|---:|---:|---:|---|
| `mvn -q -Dtest=LocalServiceExecutionContractTest test` | 1 | 0 | N/A | N/A | Shared `testCompile` blocker; Surefire not reached |
| `mvn -q -Dtest=LocalTurnActionExecutorContractTest test` | 1 | 0 | N/A | N/A | Shared `testCompile` blocker; Surefire not reached |
| `mvn -q -Dtest=WindowTurnLoopContractTest test` | 1 | 0 | N/A | N/A | Shared `testCompile` blocker; Surefire not reached |
| `mvn -q -Dtest=TurnLoopRegistryConcurrencyTest test` | 1 | 0 | N/A | N/A | Shared `testCompile` blocker; Surefire not reached |
| `mvn -q -Dtest=TurnModeGuardContractTest test` | 1 | 0 | N/A | N/A | Shared `testCompile` blocker; Surefire not reached |
| `mvn -q -Dtest=TurnConfigurationWiringContractTest test` | 1 | 0 | N/A | N/A | Shared `testCompile` blocker; Surefire not reached |
| `mvn -q -DskipTests compile` | 0 | N/A | N/A | N/A | DHXY main-source compile passed |

Shared blocker evidence, identical at the head of every point-test attempt:

- `src/test/java/com/bot/dhxy/service/SummonSkillStartIndexPolicyTest.java:19,25,36` calls missing `SummonSkillService.resolveStartIndex(...)`.
- `src/test/java/com/bot/dhxy/service/TaskMaintenanceSummonSkillUnknownBackoffTest.java:41,84` uses stale `TaskMaintenanceService` constructor arity.
- `src/test/java/com/bot/dhxy/service/TaskMaintenanceSummonSkillUnknownBackoffTest.java:150` uses stale `SummonSkillService` constructor arity.
- Compiler output continued with additional existing stale tests. None of the six T03B paths appeared in the emitted diagnostics, but the shared compilation failure means this delivery does **not** claim that the selected tests compiled or ran.
- No out-of-scope debt was edited and no alternate compiler/Surefire bypass was used.

### Final Test-Source SHA-256

| File | SHA-256 |
|---|---|
| `LocalServiceExecutionContractTest.java` | `9986230091B752455454269DBB75B9517292F71CDB7F7F1D2843A955EE39C84A` |
| `LocalTurnActionExecutorContractTest.java` | `A63BFCB72063A754DC52C642A62F4EB4121168A5572E941B575D4C1C562A374C` |
| `WindowTurnLoopContractTest.java` | `C70AB2916D5A8A1C1A48DC1ED268B639635BE660C3F37B71A60A4D0F6EA1B8B3` |
| `TurnLoopRegistryConcurrencyTest.java` | `49FABE0FA9DBD6F1868B19C5E4F37C9C46F85A17CA41CCD4EAAAFDD9AD3EFF68` |
| `TurnModeGuardContractTest.java` | `E3C7E70316EFA486C0F262EE241CB35182633F14052B05DA6D02E7289B950219` |
| `TurnConfigurationWiringContractTest.java` | `46C8FC6EC187C588B92680F366E02ECEDBD768291BA97D286AA5A80B0F814A17` |

### Handoff

- Source and test-source material is delivered within the frozen write set.
- Test execution remains precisely blocked by shared DHXY `testCompile` debt outside this card's ownership.
- Parent must independently review the six sources and decide the card outcome after the shared test gate can be run. This implementation owner remains non-reviewing and does not write `APPROVED` or parent `BLOCKED`.

## PARENT TEST SOURCE REVIEW #1 - TEST SOURCES PASSED / COVERED PRODUCTION REPAIR REQUIRED

- reviewedAt: `2026-07-15T19:29:44-04:00`
- reviewer: `CR271 parent / sole manager and final reviewer`
- test-source verdict: `P0/P1/P2=0/0/0 / TEST SOURCE REVIEW PASSED`
- covered-production verdict: `P0/P1/P2=0/1/0 / TURN-06+TURN-13 REPAIR REQUIRED`
- Parent independently read all six tests and their direct production branches. The tests execute the real
  `LocalServiceExecution`, `LocalTurnActionExecutor`, `WindowTurnLoop`, `TurnLoopRegistry`, `TurnModeGuard` and
  `TurnConfiguration` behavior using bounded fake mechanics. They directly cover typed local results/real PNG
  validation, N-step terminal ordering and failure-frame replacement, ACK/uncertain retention without re-execution,
  lifecycle retirement races, same-monitor local/remote exclusion and inert construction. No test-source bypass or
  write-set drift was found.
- The package-private `TurnContractFixtures` uses an inert `WindowTaskRunner` test double only because the production
  concrete constructor requires the complete desktop graph. Every method consumed by the production code under test
  is overridden; no real runner constructor, executor or desktop boundary is entered. This does not substitute for
  the direct production calls made by the six tests.

### P1-1 - HttpsTurnClient construction violates the zero-thread wiring contract

- Evidence: `TurnConfigurationWiringContractTest.java:27-54` snapshots live threads before creating the real
  `TurnClient` bean and requires no new thread. `TurnConfiguration.turnClient(...)` constructs
  `HttpsTurnClient`, whose constructor calls `HttpClient.newBuilder().build()` at `HttpsTurnClient.java:86-90`.
  The parent ran an isolated no-network JDK probe and observed exactly `[HttpClient-1-SelectorManager]` after
  `HttpClient.build()`.
- Impact: the frozen TURN-13/T03B acceptance “inert bean construction starts zero thread” is currently false. Once
  shared testCompile debt is repaired, this valid test is expected to fail even though the other five contracts may
  pass.
- Repair condition: retain one reusable HTTP/2 client per `HttpsTurnClient`, but create it thread-safely only on the
  first real exchange/template download. Construction must start zero thread. Do not create one client per request,
  add retry/fallback/second send, or alter parser/auth/timeout/business semantics. The existing T03B wiring test is the
  acceptance test and remains read-only during the production repair.

All six standard named commands remain `exit 1 / selected tests 0`; source review does not convert that into a test
pass. Nash's test-source owner is released. Card approval still requires the covered-production repair plus the six
original named Maven commands executing successfully.

**No approved business differences; equivalent migration against baseline `696a12b0`.**

## REPAIR #1 SOURCE DELIVERED / MAVEN DEFERRED / PARENT RE-REVIEW PENDING

- deliveredAt: `2026-07-15T19:56:10.957-04:00`
- implementation owner: `Nash / CR271 Worker B`
- role boundary: implementation delivery only; no reviewer or approval claim is made here.
- frozen scope: only the four Repair #1 test sources and this report were written. Production, POM,
  fixtures, other tests/documents, Cloud and Git metadata remained read-only.

### Parent Review #2 Repair Closure

1. `WindowTurnLoopContractTest`
   - The uncertain/restart path now executes a real `CAPTURE(UPLOAD_IMAGE)` plus click action.
   - It proves the uploaded raw frame is non-null, decodable PNG data with the expected ROI pixels.
   - The first uncertain client step mutates its request-local byte array; the explicitly restarted loop still uploads
     the original bytes, proving retained-frame isolation.
   - The repeated `actionId` keeps capture and input at one physical execution. After accepted `IDLE`, the next request
     carries neither previous outcome nor PNG.
2. `TurnModeGuardContractTest`
   - Before releasing the local supplier, the test bounded-waits for the remote contender to reach
     `Thread.State.BLOCKED` and uses `ThreadMXBean` to prove the lock owner is the local winner thread.
   - The one-winner result, exactly one winning supplier invocation and zero losing-supplier invocation remain asserted.
3. `LocalTurnActionExecutorContractTest`
   - A shared mechanics event log now asserts exact order `capture:roi -> input:submit ->
     capture:failure-evidence` for the failure-replacement case.
4. Thread and loop cleanup
   - Every repaired scenario that starts a loop or helper thread now releases gates and performs bounded
     stop/await/join cleanup in `finally`, then asserts each owned loop/thread is not running/alive.
5. `TurnLoopRegistryConcurrencyTest`
   - A new bounded case races stop/remove only after the old loop entered exchange.
   - Both legal race outcomes converge to registry size zero, permanent retirement of the old loop and no second
     usable loop; a temporary replacement is proved to be the sole registered loop and is then retired as well.

The existing bounded fake/`Unsafe` seam was retained. No production abstraction was added, and no sleep-only
coordination was introduced.

### Final Repair #1 Source SHA-256

| File | SHA-256 |
|---|---|
| `LocalTurnActionExecutorContractTest.java` | `EC844090A3B76E80F3072727B14010244184FF90ECD4360039BD572CFCC861C7` |
| `WindowTurnLoopContractTest.java` | `C8B0B7A8CA15A9A8D017C57FD1C678FC402B721C69661F3A7A2DA29F4253BCBF` |
| `TurnLoopRegistryConcurrencyTest.java` | `0570424BD4C09CF5B50E694FAA35E5542E69F7ED149A15926ADC2FE6935327FF` |
| `TurnModeGuardContractTest.java` | `453B0A9CBB4B8A9D9643518FE8EA94D57374C5789C49E8732777F17A764F5621` |

Read-only SHA confirmation:

| File | SHA-256 |
|---|---|
| `LocalServiceExecutionContractTest.java` | `9986230091B752455454269DBB75B9517292F71CDB7F7F1D2843A955EE39C84A` |
| `TurnConfigurationWiringContractTest.java` | `46C8FC6EC187C588B92680F366E02ECEDBD768291BA97D286AA5A80B0F814A17` |

### Source Self-Check And Deferred Maven Gate

- Static source check: `12` repaired test methods total; braces balanced in all four files; trailing whitespace `0`;
  no `Thread.sleep`, `TimeUnit.*sleep`, `System.out`, `TODO` or `FIXME` match.
- Maven/test commands: **not run for Repair #1**. Parent froze Maven while Kuhn's active TURN-13H test writer and
  Kierkegaard's helper share the workspace/target boundary. No shared `target` path was touched.
- The named T03B Maven commands remain pending for the parent-controlled stable-writer gate; this delivery does not
  convert the earlier shared `testCompile` blocker into a pass.

**Parent re-review is pending. This worker does not self-approve or parent-block TURN-T03B.**

**No approved business differences; equivalent migration against baseline `696a12b0`.**
