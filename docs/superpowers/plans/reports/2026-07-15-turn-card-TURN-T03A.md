# TURN-T03A Worker C Report

## CLAIMED

- Card: `TURN-T03A` - DHXY client/cache/capture/match/input contract test debt.
- Role: CR271 Worker C; implementation owner for this test-only slice, not reviewer.
- Status: `CLAIMED / TEST IMPLEMENTATION IN PROGRESS`.
- Scope amendment: this card is the first five tests split from `TURN-T03`; the remaining six executor/loop/mode tests belong to `TURN-T03B` and are out of scope.
- Workspace transport HEAD observed before editing: `0114604e1ff5f15491d2910959c45252e893d04f` on branch `thin-client-design`.
- Business baseline remains `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`; this test-only card changes no business or production behavior.
- Both repositories were already dirty/untracked before this claim. Every pre-existing change is protected; no reset, checkout, clean, commit, staging, or other Git mutation is allowed.

## Exact Write Set

1. `src/test/java/com/bot/dhxy/cloud/turn/HttpsTurnClientContractTest.java`
2. `src/test/java/com/bot/dhxy/cloud/turn/TurnTemplateCacheContractTest.java`
3. `src/test/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutorContractTest.java`
4. `src/test/java/com/bot/dhxy/cloud/turn/TurnMatchStepExecutorContractTest.java`
5. `src/test/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutorContractTest.java`
6. `src/test/resources/cloud-turn/v1/frame-2x2.png`
7. `docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-T03A.md`

No production source, Maven model, application configuration, TURN-T03B test, Cloud repository file, CR card, plan, matrix, or dashboard is in this Worker write set.

## Frozen Acceptance Scope

- `HttpsTurnClientContractTest`: loopback HTTP only; JSON and raw PNG multipart shape, exact authorization/path/correlation, `401`/`409`/`5xx`/network uncertainty typed failure, exactly one request and no retry.
- `TurnTemplateCacheContractTest`: SHA hit without download, missing/stale template refresh, ETag/hash identity, atomic replacement, invalid download preserves the old cache, and path traversal rejection.
- `TurnCaptureStepExecutorContractTest`: fake exact-HWND capture only; non-zero real `left/top`, absolute ROI origin, unscaled pixels/dimensions, bounds rejection, and capture failure.
- `TurnMatchStepExecutorContractTest`: fake capture plus local deterministic template; hit/miss absolute coordinates, match-only never clicks, `onMatch=CLICK` only returns click intent, optional evidence frame, and invalid template/hash/region failures.
- `TurnInputStepExecutorContractTest`: fake queue/keyboard only; closed input mapping and order, supported background key delivery, unsupported key typed failure with zero foreground/input-queue fallback, wait validation, and stop short-circuit.
- Tests must not start the application, server, Task runtime, real screenshot, OCR, mouse, or keyboard path.
- Before running tests, Worker C must re-read final TURN-40A protocol files/report and adapt only these tests when the final shared contract affects them. Worker C must not edit TURN-40A production or tests.
- If a test exposes a production defect, this Worker records precise P-risk evidence and expected repair in this report; it does not modify production.

## Required Verification

Run each named test separately from `D:/mavenProject/DHXY` and record command, exit code, tests run, failures, and errors:

1. `mvn -q -Dtest=HttpsTurnClientContractTest test`
2. `mvn -q -Dtest=TurnTemplateCacheContractTest test`
3. `mvn -q -Dtest=TurnCaptureStepExecutorContractTest test`
4. `mvn -q -Dtest=TurnMatchStepExecutorContractTest test`
5. `mvn -q -Dtest=TurnInputStepExecutorContractTest test`
6. `mvn -q -DskipTests compile`

Final Worker state may be only `TEST DELIVERED / PARENT REVIEW PENDING` or `BLOCKED EVIDENCE`; this Worker must not write `APPROVED` or `BLOCKED` as a reviewer judgment.

## Final TURN-40A Protocol Re-read

- Re-read occurred after TURN-40A production delivery and before the first test command.
- Parent evidence was then present in the authoritative plan and CR271 card:
  `TURN-40A | SOURCE REVIEW PASSED / TEST+CLOUD BUILD PENDING` and
  `P0/P1/P2=0/0/0` at `2026-07-15 18:33 EDT`.
- Final DHXY protocol includes `pauseRequested` plus `stopRequested`, optional
  `TurnTaskStartRequest` on `TurnRequest`, and correlated optional `TurnTaskStartAck` on `TurnResponse`.
- The compatibility constructors used by these five Foundation tests remain part of the final production contract.
  Worker C made no production or TURN-40A changes.

## TEST DELIVERED / PARENT REVIEW PENDING

### Delivered Test Scope

| File | Contract evidence |
|---|---|
| `HttpsTurnClientContractTest` | one JSON POST, exact path/auth/non-zero window origin, raw PNG multipart without Base64, and one-attempt `401`/`409`/`503`/uncertain-network failures |
| `TurnTemplateCacheContractTest` | SHA hit, missing/stale single download, exact ETag/hash, atomic replacement, invalid PNG preservation, traversal rejection |
| `TurnCaptureStepExecutorContractTest` | fake HWND full/ROI capture, real `left=137/top=241`, no scaling, bounds rejection, empty capture failure |
| `TurnMatchStepExecutorContractTest` | deterministic hit/miss, absolute result coordinates, match-only no click, `CLICK` as intent only, optional evidence image, invalid hash rejection |
| `TurnInputStepExecutorContractTest` | six mouse forms in one queue request each, background-only `KEY_TAP`, typed rejection of `KEY_DOWN`/`KEY_UP`/`TEXT_INPUT` and unvalidated key, queue/background failure, stop/wait validation |
| `frame-2x2.png` | 134-byte four-color PNG, SHA-256 `8096256effcd6a69c3e60db84561a741e2558e460be85447cb12d50fe48ce5c3` |

No test started the application, Cloud server, Task runtime, real capture, OCR, mouse, or keyboard. The match test used only the repository-bundled OpenCV DLL against in-memory PNG pixels.

### Required Lifecycle Commands

All five exact commands were executed separately. Each exited `1` before the selected test ran because Maven compiles the repository's entire `src/test/java` tree during `testCompile`.

| Command | Exit | Selected tests | Selected failures/errors |
|---|---:|---:|---:|
| `mvn -q -Dtest=HttpsTurnClientContractTest test` | 1 | 0 | not executed; shared `testCompile` failed |
| `mvn -q -Dtest=TurnTemplateCacheContractTest test` | 1 | 0 | not executed; shared `testCompile` failed |
| `mvn -q -Dtest=TurnCaptureStepExecutorContractTest test` | 1 | 0 | not executed; shared `testCompile` failed |
| `mvn -q -Dtest=TurnMatchStepExecutorContractTest test` | 1 | 0 | not executed; shared `testCompile` failed |
| `mvn -q -Dtest=TurnInputStepExecutorContractTest test` | 1 | 0 | not executed; shared `testCompile` failed |

The first stable out-of-scope errors are:

- `SummonSkillStartIndexPolicyTest.java:19/25/36` calls missing `SummonSkillService.resolveStartIndex(...)`.
- `TaskMaintenanceSummonSkillUnknownBackoffTest.java:41/84` uses a stale `TaskMaintenanceService` constructor.
- The full compiler output also reports stale legacy tests for `NpcClickService`, `DialogService`,
  `XiuluoRoundContext`, and `TeamRoleDetectionService` signatures.

These files are outside TURN-T03A's write set. Worker C did not edit, hide, skip, move, or delete them, and does not claim the required lifecycle gate is green.

### Isolated Target Evidence

To distinguish T03A test correctness from the unrelated shared test debt, Worker C built the normal Maven test dependency classpath, compiled only the five owned sources with `javac --release 21`, and ran each already-compiled class through the same Maven Surefire `3.2.5` goal. This did not weaken assertions or change the POM.

| Direct Surefire command | Exit | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| `mvn -q -Dtest=HttpsTurnClientContractTest surefire:test` | 0 | 6 | 0 | 0 | 0 |
| `mvn -q -Dtest=TurnTemplateCacheContractTest surefire:test` | 0 | 5 | 0 | 0 | 0 |
| `mvn -q -Dtest=TurnCaptureStepExecutorContractTest surefire:test` | 0 | 4 | 0 | 0 | 0 |
| `mvn -q -Dtest=TurnMatchStepExecutorContractTest surefire:test` | 0 | 5 | 0 | 0 | 0 |
| `mvn -q -Dtest=TurnInputStepExecutorContractTest surefire:test` | 0 | 5 | 0 | 0 | 0 |

Owned-source isolated compilation: exit `0`. Total isolated target execution: `25` tests, `0` failures, `0` errors, `0` skipped.

### Main Compile Gate

- Command: `mvn -q -DskipTests compile`
- Exit: `0`
- Tests: not run by this command, as intended.

### Scope And Review Handoff

- Git status for the exact write set contains only the five new tests, one new fixture, and this new report.
- No production file, POM, T03B test, Cloud repository file, or Git metadata was modified by Worker C.
- No production defect was exposed by the 25 isolated target tests.
- Parent review must decide how the pre-existing shared `testCompile` debt is repaired before treating the five required lifecycle commands as passed. This Worker does not self-approve.

## PARENT TEST SOURCE REVIEW #1 - 2026-07-15 18:47 EDT

- Review authority: parent Codex; Worker self-report and isolated Surefire results were not used as approval.
- Verdict: `P0/P1/P2=0/3/0`.
- Status: `TEST SOURCE REPAIR #1 REQUIRED / REQUIRED MAVEN COMMANDS BLOCKED`; this is not card approval.
- Preserved evidence: the parent independently read all five test classes and the corresponding production
  boundaries, verified the fixture as a 134-byte `2x2` PNG with SHA-256
  `8096256EFFCD6A69C3E60DB84561A741E2558E460BE85447CB12D50FE48CE5C3`, and confirmed scoped
  `git diff --check` is clean. The 25 isolated passes are useful diagnostic evidence only; all five authoritative
  Maven test commands still reached zero selected tests because unrelated stale tests fail shared `testCompile`.

### P1 Findings And Exact Repair Conditions

1. `HttpsTurnClientContractTest.java:54-148` covers JSON, multipart, HTTP status and a network-close uncertainty,
   but it never exercises the plan-mandated `InterruptedException -> TurnTransportException.Kind.INTERRUPTED`
   path. Add one deterministic isolated-thread test proving interrupt is preserved, the result is typed
   `INTERRUPTED`, and no implicit retry/second request occurs.
2. `TurnCaptureStepExecutorContractTest.java:27-115` checks non-zero origin, ROI dimensions and empty capture, but
   does not compare decoded pixel values and does not distinguish the immutable refreshed binding snapshot from
   a later context binding drift. Add direct pixel assertions and a drift case proving capture cannot silently use
   a changed/different HWND binding; the required result is exact-snapshot use or typed closed failure according to
   the existing production contract, never capture from the drifted window.
3. `TurnInputStepExecutorContractTest.java:29-158` covers all ten input enum forms but calls `waitFor` only with
   invalid `0`. Add a positive WAIT completion assertion and a deterministic interrupted WAIT assertion proving
   `STOPPED`, interrupt preservation and zero queue/keyboard calls.

Repair scope remains exactly these three owned test files plus this report. Do not modify production, POM,
fixture, T03B tests or unrelated stale tests. After repair, rerun isolated diagnostics if useful, but the card stays
Maven-gate blocked until the original five `mvn -q -Dtest=... test` commands execute the selected tests with exit 0.

**No approved business differences; equivalent migration against baseline `696a12b0`.**

## PARENT CROSS-CARD REVIEW #5 - REPAIR #3 REQUIRED

- reviewedAt: `2026-07-15T19:29:44-04:00`
- trigger: parent TURN-T03B wiring-contract review
- verdict: `P0/P1/P2=0/1/0 / REPAIR #3`
- Evidence: real `HttpsTurnClient` construction invokes `HttpClient.newBuilder().build()` at lines `86-90`; a parent
  no-network JDK probe observed one new `HttpClient-1-SelectorManager` thread. This violates the frozen TURN-13
  invariant verified by `TurnConfigurationWiringContractTest.java:27-54`.
- Frozen Repair #3 write set: `HttpsTurnClient.java` and this report only. The existing T03B test is read-only.
- Required repair: lazily create exactly one reusable HTTP/2 client per `HttpsTurnClient` at first real request, with
  thread-safe publication. Constructor-only wiring must start zero thread. Preserve parser/auth/timeouts/single-send
  behavior; no per-request client, retry, fallback, second send, session, ledger or lifecycle owner.
- Maven is deferred while other Java writers are active. The original T03B named wiring test and T03A client test must
  be run after writers stabilize and shared testCompile debt is repaired.

**No approved business differences; equivalent migration against baseline `696a12b0`.**

## PARENT CROSS-CARD REVIEW #3 - REOPENED REPAIR #2

- reviewedAt: `2026-07-15T19:09:10-04:00`
- verdict: `P0/P1/P2=0/1/0 / REPAIR #2`
- trigger: parent T01 bilateral protocol review compared the test-owned strict contract mapper with the actual DHXY
  response parser after Review #2.

### P1-1 - Actual DHXY response parser is less strict than the frozen PG contract

- Evidence: `HttpsTurnClient.java:76-80` enables unknown-field, trailing-token, and duplicate-field rejection only.
  Unlike the frozen strict parser and Cloud ingress, it does not enable `FAIL_ON_NULL_FOR_PRIMITIVES` or
  `FAIL_ON_NUMBERS_FOR_ENUMS`, and does not disable `ACCEPT_FLOAT_AS_INT` or scalar coercion.
  `HttpsTurnClientContractTest` has no malformed-200 response cases for those paths.
- Impact: a numeric enum, quoted/float numeric value, or null primitive can be coerced before
  `TurnProtocolValidator` sees it, so the real thin client may accept JSON that T01's private strict mapper rejects.
- Repair condition: make `HttpsTurnClient`'s defensive mapper copy use the same closed strict features as Cloud
  ingress. Add deterministic one-request loopback cases proving malformed 200 JSON for numeric enum, scalar/float
  coercion, and null primitive maps to the existing typed response parse/contract failure with no second POST.

### Frozen Repair #2 write set

1. `src/main/java/com/bot/dhxy/cloud/turn/HttpsTurnClient.java`
2. `src/test/java/com/bot/dhxy/cloud/turn/HttpsTurnClientContractTest.java`
3. `docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-T03A.md`

All other production, tests, fixtures, POMs, documents, Cloud files, and Git metadata remain read-only. This repair
does not add retry, second request, fallback, runtime activation, or business behavior. Review #2 remains valid for
interrupt/capture/input; only the parser boundary is reopened.

## REPAIR DELIVERED #1 / PARENT REVIEW PENDING

- Delivery time: `2026-07-15 18:53:54 -04:00`.
- Worker state: `REPAIR DELIVERED / PARENT REVIEW PENDING`; Worker C does not self-approve.
- Repair write set remained limited to the three parent-named tests and this report. No production source,
  POM, fixture, TURN-T03B test, legacy/shared test, or Git metadata was changed.

### P1 Repair Evidence

1. `HttpsTurnClientContractTest` now runs an isolated loopback-client thread, waits until the only POST has
   reached the server, interrupts the client deterministically, and proves
   `TurnTransportException.Kind.INTERRUPTED`, preserved interrupt status, exactly one request, and no live
   client thread or second POST.
2. `TurnCaptureStepExecutorContractTest` now decodes the returned PNG and compares three exact source pixels.
   It also mutates `WindowRuntimeContext` to a different HWND after the action snapshot was created and proves
   the capture provider still receives the immutable exact snapshot (`12345`), never the drifted HWND
   (`98765`), while preserving the real `left=137/top=241` frame region.
3. `TurnInputStepExecutorContractTest` now proves a positive `WAIT(1ms)` completes and a pre-interrupted
   isolated `WAIT(10000ms)` deterministically returns `STOPPED`, preserves the interrupt flag, and performs
   zero input-queue and background-keyboard calls.

### Isolated Repair Diagnostics

- Owned five-source `javac --release 21` compilation: exit `0`.
- Supplementary direct Surefire execution used the existing POM and already compiled owned tests; it is
  diagnostic evidence only and is not represented as the required lifecycle gate.

| Direct Surefire command | Exit | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| `mvn -q -Dtest=HttpsTurnClientContractTest surefire:test` | 0 | 7 | 0 | 0 | 0 |
| `mvn -q -Dtest=TurnTemplateCacheContractTest surefire:test` | 0 | 5 | 0 | 0 | 0 |
| `mvn -q -Dtest=TurnCaptureStepExecutorContractTest surefire:test` | 0 | 5 | 0 | 0 | 0 |
| `mvn -q -Dtest=TurnMatchStepExecutorContractTest surefire:test` | 0 | 5 | 0 | 0 | 0 |
| `mvn -q -Dtest=TurnInputStepExecutorContractTest surefire:test` | 0 | 6 | 0 | 0 | 0 |

Repair diagnostic total: `28` tests, `0` failures, `0` errors, `0` skipped.

### Required Lifecycle Commands After Repair

All five exact standard commands were rerun separately. Each Maven invocation exited `1` during shared
`testCompile`, before its selected TURN-T03A class executed; therefore each selected-test count remains
truthfully `0`.

| Required command | Maven exit | Selected tests | Result |
|---|---:|---:|---|
| `mvn -q -Dtest=HttpsTurnClientContractTest test` | 1 | 0 | shared `testCompile` failure |
| `mvn -q -Dtest=TurnTemplateCacheContractTest test` | 1 | 0 | shared `testCompile` failure |
| `mvn -q -Dtest=TurnCaptureStepExecutorContractTest test` | 1 | 0 | shared `testCompile` failure |
| `mvn -q -Dtest=TurnMatchStepExecutorContractTest test` | 1 | 0 | shared `testCompile` failure |
| `mvn -q -Dtest=TurnInputStepExecutorContractTest test` | 1 | 0 | shared `testCompile` failure |

The stable first out-of-scope errors remain unchanged:

- `SummonSkillStartIndexPolicyTest.java:19/25/36` references missing
  `SummonSkillService.resolveStartIndex(...)`.
- `TaskMaintenanceSummonSkillUnknownBackoffTest.java:41/84/150/179` uses stale
  `TaskMaintenanceService` / `SummonSkillService` constructors.
- Further compiler output still includes unrelated stale test signatures; Worker C did not edit or suppress
  those tests.

### Main Compile And Handoff

- `mvn -q -DskipTests compile`: exit `0`.
- No production defect was exposed by the repaired isolated contract tests.
- The three parent P1 repair conditions now have passing isolated source/runtime evidence. The five required
  lifecycle commands still have no selected-test result because of the pre-existing shared test-compilation
  debt; final acceptance remains with the parent reviewer.

## PARENT TEST SOURCE REVIEW #2 - 2026-07-15 18:56 EDT

- Review authority: parent Codex; the Worker repair summary and isolated Surefire run were used as evidence only.
- Verdict: `P0/P1/P2=0/0/0`; all three Review #1 P1 findings are closed.
- Status: `TEST SOURCE REVIEW PASSED / REQUIRED MAVEN TESTS BLOCKED`; this is not card approval.
- `HttpsTurnClientContractTest` now deterministically interrupts the sole in-flight loopback POST and directly
  asserts typed `INTERRUPTED`, restored interrupt status, exactly one request and no live client thread/second send.
- `TurnCaptureStepExecutorContractTest` now decodes the returned PNG and compares real source pixels. Its binding
  drift case proves the executor supplies the immutable action snapshot HWND `12345`, while later context binding
  `98765` cannot replace it; non-zero absolute origin and unscaled dimensions remain asserted.
- `TurnInputStepExecutorContractTest` now directly proves positive WAIT completion and pre-interrupted WAIT returns
  typed `STOPPED`, preserves the interrupt flag and creates zero input-queue/background-keyboard calls.
- The parent re-read the corresponding production branches (`HttpsTurnClient.sendOnce`,
  `TurnCaptureStepExecutor.capture`, `TurnInputStepExecutor.waitFor`) and found no assertion bypass or production
  write-set drift. The 28 isolated passes remain diagnostic only. All five standard commands still execute zero
  selected tests because unrelated stale DHXY tests fail shared `testCompile`; the exact commands must be rerun after
  that cohort is repaired. Main DHXY compile evidence remains exit `0`.
- T03A test-source owner is released; no further repair is requested in this slice.

**No approved business differences; equivalent migration against baseline `696a12b0`.**

## PARENT TRUE EOF REISSUE - REPAIR #2 ACTIVE

- reissuedAt: `2026-07-15T19:12:20-04:00`
- authoritative current state: `P0/P1/P2=0/1/0 / REPAIR #2 ACTIVE`
- scope: only `HttpsTurnClient.java`, `HttpsTurnClientContractTest.java`, and this report.
- requirement: align the real client mapper with strict null-primitive/numeric-enum/float/scalar-coercion rejection;
  malformed 200 loopback cases must return typed failure after exactly one POST. Review #2 remains valid for the
  interrupt/capture/input slice but its “no further repair” sentence is superseded by this cross-card finding.

## REPAIR DELIVERED #2 / PARENT REVIEW PENDING

- deliveredAt: `2026-07-15 19:15:28 -04:00`
- Worker role: original TURN-T03A implementation Worker; not reviewer and not self-approved.
- Exact repair write set: `HttpsTurnClient.java`, `HttpsTurnClientContractTest.java`, and this report only.
  No other production, test, fixture, POM, document, Cloud, or Git file was changed.

### Strict Response Parser Repair

`HttpsTurnClient` now applies the same defensive-copy response parsing features as Cloud ingress in addition to
the existing unknown-property, trailing-token, and duplicate-field rejection:

- enable `FAIL_ON_NULL_FOR_PRIMITIVES`;
- enable `FAIL_ON_NUMBERS_FOR_ENUMS`;
- disable `ACCEPT_FLOAT_AS_INT`;
- disable `ALLOW_COERCION_OF_SCALARS`.

No retry, fallback, second request, runtime activation, or business decision was added.

### Deterministic Contract Evidence

`HttpsTurnClientContractTest` now serves four malformed `200 application/json` responses from the existing
loopback server:

1. numeric `TurnResponse.Status` enum;
2. `null` primitive `TurnAction.contractVersion`;
3. quoted numeric primitive coercion;
4. floating-point-to-integer coercion.

Each case asserts typed `TurnTransportException.Kind.RESPONSE_PARSE` and exactly one POST, proving no retry or
second request. Existing JSON success, raw multipart PNG, `401/409/503`, uncertain-network, and deterministic
interrupt assertions remain intact.

### Verification

| Command | Exit | Tests | Failures | Errors | Skipped | Meaning |
|---|---:|---:|---:|---:|---:|---|
| owned-source `javac --release 21` | 0 | n/a | n/a | n/a | n/a | isolated test source compiled |
| `mvn -q -Dtest=HttpsTurnClientContractTest surefire:test` | 0 | 11 | 0 | 0 | 0 | diagnostic only |
| `mvn -q -Dtest=HttpsTurnClientContractTest test` | 1 | 0 | n/a | n/a | n/a | shared `testCompile` failed before selection |
| `mvn -q -DskipTests compile` | 0 | 0 | 0 | 0 | 0 | DHXY main compile passed |

The standard Maven test command remains truthfully unexecuted for the selected class because unrelated stale
shared tests fail `testCompile`. The first stable errors remain
`SummonSkillStartIndexPolicyTest.java:19/25/36` (missing `resolveStartIndex`) and
`TaskMaintenanceSummonSkillUnknownBackoffTest.java:41/84/150/179` (stale constructors); additional unrelated
legacy test signature failures follow. Worker C did not edit, suppress, move, or skip those tests.

Repair #2 closes the implementation-side strict-parser gap with passing isolated diagnostic evidence. Final
source judgment and disposition of the blocked standard Maven gate remain with the parent reviewer.

**No approved business differences; equivalent migration against baseline `696a12b0`.**

## PARENT SOURCE/TEST REVIEW #4 - REPAIR #2 PASSED

- reviewedAt: `2026-07-15T19:20:31-04:00`
- reviewer: `CR271 parent / sole manager and final reviewer`
- verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / REQUIRED MAVEN TEST BLOCKED`
- Parent independently re-read the real response mapper in `HttpsTurnClient.java:77-85`. Its defensive copy now
  rejects null primitives and numeric enums, disables float-to-integer conversion and disables scalar coercion, in
  addition to the existing unknown-property/trailing-token/duplicate-field gates.
- Parent independently re-read `HttpsTurnClientContractTest.java:141-158,282-312`. All four malformed `200` loopback
  bodies exercise the real `HttpsTurnClient.exchange(...)` path and assert typed `RESPONSE_PARSE` plus exactly one
  request: numeric response-status enum, null action `contractVersion`, quoted numeric primitive and floating-point
  primitive. No retry, fallback or second POST was introduced.
- The exact Repair #2 write set is clean. The standard named Maven command remains `exit 1 / selected tests 0` because
  unrelated stale DHXY tests fail shared `testCompile`; isolated `11/11` is diagnostic only. DHXY main compile remains
  `exit 0`. No active Java-writer cohort build was rerun during this parent review.
- The implementation owner is released. No further T03A repair is requested; the named standard Maven command must be
  rerun after the shared test-compilation debt is repaired.

**No approved business differences; equivalent migration against baseline `696a12b0`.**

## PARENT TRUE EOF REISSUE - REPAIR #3 ACTIVE

- reissuedAt: `2026-07-15T19:30:18-04:00`
- authoritative current state: `P0/P1/P2=0/1/0 / REPAIR #3 ACTIVE`
- trigger: the passed T03B wiring-test source exposes eager `HttpClient.build()` selector-thread creation during
  otherwise inert `TurnConfiguration` construction. This supersedes Review #4's “no further repair” sentence only.
- exact write set: `HttpsTurnClient.java` and this report. Make the one reusable per-instance client thread-safe lazy
  on first real request; preserve every Repair #2 parser/single-POST assertion and add no retry/fallback/second client.
- T03B's existing wiring test remains read-only and is the acceptance test. Maven remains deferred while Java writers
  are active and until shared testCompile debt is repaired.

**No approved business differences; equivalent migration against baseline `696a12b0`.**

## REPAIR #3 SOURCE DELIVERED / PARENT REVIEW PENDING / MAVEN DEFERRED

- deliveredAt: `2026-07-15 19:36:11 -04:00`
- Worker role: original TURN-T03A implementation Worker; not reviewer and not self-approving.
- Exact changed files:
  1. `src/main/java/com/bot/dhxy/cloud/turn/HttpsTurnClient.java`
  2. `docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-T03A.md`
- `TurnConfigurationWiringContractTest`, all other tests/production/config/POM/docs, the Cloud repository, and Git
  metadata remained read-only.

### Lazy Client Invariant

- Construction now validates and stores the per-instance `connectTimeout`, request settings, authorization, URI,
  and strict defensive mapper without calling `HttpClient.newBuilder()` or starting a JDK HTTP selector thread.
- Both `exchange(...)` and `downloadTemplate(...)` retain their existing request construction and converge on the
  existing `sendOnce(...)` boundary.
- `sendOnce(...)` reads a per-instance `volatile HttpClient`. On the first real request only, a double check inside
  `synchronized (this)` creates and publishes one HTTP/2 client with the original connect timeout and redirect
  policy. Later requests reuse the published client.
- The client build occurs inside the monitor after the second null check, so concurrent first callers cannot build
  or publish losing extra clients. The volatile write/read supplies safe publication to later callers.
- There is no per-request client, static/global holder, executor, retry, fallback, second send, or wrapper layer.
  Parser, authentication, URI, timeout, content/status handling, exception typing, and business semantics are
  unchanged from Repair #2.

### Source-Only Verification

- Read-only static scan found exactly one `HttpClient.newBuilder()` and exactly one `.send(...)`, both inside
  `sendOnce(...)`; it found no static `HttpClient`, `new Thread`, executor, or alternate send path.
- Maven, named tests, compilation, and all writes to shared `target` were intentionally not run because Kuhn is
  actively writing the Cloud TURN-13H Java cohort and the parent explicitly froze Maven/shared-target activity
  while Java writers are active.
- Required follow-up after all Java writers become stable:
  1. `mvn -q -Dtest=HttpsTurnClientContractTest test`
  2. `mvn -q -Dtest=TurnConfigurationWiringContractTest test`
  3. `mvn -q -DskipTests compile`
- If shared `testCompile` debt still prevents either selected class from running, the follow-up report must retain
  the real Maven exit and selected-test count; no isolated diagnostic may replace those named commands.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## PARENT SOURCE REVIEW #6 - REPAIR #3 PASSED

- reviewedAt: `2026-07-15T19:41:26-04:00`
- reviewer: `CR271 parent / sole manager and final reviewer`
- verdict: `P0/P1/P2=0/0/0 / SOURCE REVIEW PASSED / NAMED TEST+COMPILE PENDING`
- Parent independently scanned the complete class. Construction now stores only validated configuration and starts
  no `HttpClient`. Both exchange and template download still converge on the single existing `sendOnce(...)` path.
  That method contains the class's only `HttpClient.newBuilder()` and only `.send(...)`; a per-instance volatile field
  plus a second null check under `synchronized (this)` creates and safely publishes at most one reusable client even
  under concurrent first calls.
- Parser strictness, bearer auth, URI, connect/request timeout, response bounds/status typing and exactly-one-send
  behavior from Repair #2 are unchanged. No per-request client, static holder, executor, retry, fallback or wrapper
  layer was introduced. Exact write set and whitespace check are clean.
- Maven remained deferred because TURN-13H Java/test repair is active. `HttpsTurnClientContractTest`,
  `TurnConfigurationWiringContractTest` and DHXY compile remain required after all writers stabilize; this source
  review is not a named-test or card approval claim.
- Ampere's Repair #3 owner is released. No further production repair is requested in this slice.

**No approved business differences; equivalent migration against baseline `696a12b0`.**
