# TURN-T02 Cloud Contract Test Worker Report

- card: `TURN-T02`
- status: `SOURCE + TEST DELIVERED / NAMED TEST COMMANDS EXIT 1 BEFORE TESTCOMPILE`
- worker: `CR271 Worker A (current Codex task)`
- claimedAt: `2026-07-15T18:30:47.0898092-04:00`
- dependency: `A=TURN-02R+TURN-40A`
- dependencyState: `TURN-02R SOURCE REVIEW PASSED / TEST+BUILD PENDING`; `TURN-40A SOURCE REVIEW PASSED / TEST+CLOUD BUILD PENDING`
- testFamily: `HTTPS_TURN_CONTRACT_TEST_FAMILY`
- productionScope: read-only

## Exact Write Set

1. `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\turn\CloudTurnExchangeContractTest.java`
2. `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\turn\CloudTurnExchangeFrameResultContractTest.java`
3. `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\turn\CloudTemplateCatalogContractTest.java`
4. `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\turn\CloudTurnHttpHandlerContractTest.java`
5. `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\turn\CloudTurnRoutesContractTest.java`
6. `D:\mavenProject\dhxy-cloud-brain\src\test\java\com\yueyunfe\dhxy\cloudbrain\turn\CloudTemplateHttpHandlerContractTest.java`
7. `D:\mavenProject\dhxy-cloud-brain\src\test\resources\cloud-turn\v1\frame-2x2.png`
8. `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-15-turn-card-TURN-T02.md`

No production Java, POM, other test, other fixture, application/server/runtime/Task/UI/capture/input or Git state is
owned by this worker. A production defect found by the named tests will be reported with exact evidence and will not
be repaired in this card.

## Frozen Acceptance

- Profiles: `PG+EX+IMG`.
- Cover JSON-only and raw-PNG multipart ingress, unique authorization, body/frame bounds, outcome+frame completion on
  the same command future, defensive raw-byte copies, latest metadata replacement without frame history,
  late/duplicate/busy fencing, one shared exchange/command capability, and template bytes/SHA-256/ETag single source.
- No automatic retry, TTL, session, owner, ledger or durable workflow.
- Only the six named test classes may be run. Before the final fresh run, re-read the delivered TURN-40A protocol.
- Parent Codex remains the sole reviewer; this worker cannot write `APPROVED` or `BLOCKED`.

## SOURCE + TEST DELIVERED

- deliveredAt: `2026-07-15T18:43:43.0253262-04:00`
- workerVerdict: implementation and the required named-test commands are delivered for parent review; this is not
  approval and no card-final status is asserted here.
- production/POM boundary: no production Java, POM, other test, or other fixture was modified under TURN-T02.
- execution boundary: no application/server/runtime/Task/UI/capture/input was started and no Git mutation was run.

### Delivered Files

| File | Bytes | SHA-256 |
|---|---:|---|
| `CloudTurnExchangeContractTest.java` | 20821 | `fff4d7deb53efd961348f99c01bdc394c717077ed3af0564b0e23005918a404b` |
| `CloudTurnExchangeFrameResultContractTest.java` | 9829 | `9f1f6103a537d1b880d3e1cda86826b07287c95a1f19ee5e38a30360a61208b7` |
| `CloudTemplateCatalogContractTest.java` | 4386 | `103c47a3dce797eaeaa135ea3c97d0c301fac185f717dac5efe7ed64e680d3b6` |
| `CloudTurnHttpHandlerContractTest.java` | 20937 | `5ee49610bf9652f1f38fd95515e68556a908fbd4e244041e34a6821223011ccd` |
| `CloudTurnRoutesContractTest.java` | 5016 | `67949b5bfd1c26a148a4f715c815cc583f103f494a175bb8f4e4f747da324976` |
| `CloudTemplateHttpHandlerContractTest.java` | 6780 | `5b0d09a6a582915a30210579d5546afbdf7523a4516366e4cad63d082f1dc133` |
| `src/test/resources/cloud-turn/v1/frame-2x2.png` | 126 | `0b4b8834d9fa2a0ee891481cd9e90eb8434a680bf92af684e33d7bd4fb0f8754` |

The fixture is a readable PNG with exact decoded dimensions `2x2`; tests compare its real bytes and SHA-256 rather
than metadata alone.

### Contract Coverage Delivered

- `PG`: JSON-only IDLE/action envelopes, previous-outcome acknowledgement, exact action correlation, and template
  key/hash/ETag identities are asserted through the frozen protocol records.
- `EX`: command-first and wait-first delivery, one unresolved slot, BUSY, duplicate action id, late outcome,
  interrupted waits, no replacement/retry, exactly one bearer header, strict content type, request/part/body bounds,
  same `CloudTurnExchange` command capability, and in-memory `HttpExchange` adapters with no server startup.
- `IMG`: multipart transports the fixture's raw PNG bytes; outcome and matching frame complete the same command
  future; command result/frame accessors are checked for defensive copies; bad SHA, decoded dimensions, PNG and part
  count are rejected without completing the future; latest metadata replaces the prior value and reflection checks
  that the exchange retains no outcome/frame/history collection.
- Template catalog and handler assertions bind response bytes, SHA-256, content hash and ETag to one cached catalog
  authority, including 200/304 behavior and fail-closed unknown/traversal/directory/non-PNG keys.
- No automatic retry, TTL, session, owner, ledger, durable workflow, real capture, OCR or input was introduced.

### TURN-40A Fresh Dependency Read

Before the named-test run, the true EOF of
`docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-40A-T01.md` was re-read. It records TURN-40A production
`SOURCE DELIVERED`, all eight DHXY/Cloud protocol pairs byte-identical, DHXY compile exit `0`, and parent source review
`P0/P1/P2=0/0/0`; Cloud cohort compile remains pending on the same out-of-scope lifted-Service dependency errors.
The latest records include the pause/stop window metadata and task start request/ack additions, so the tests were
written and rechecked against that delivered protocol rather than the pre-TURN-40A shape. The report EOF was read
again after the six commands; no newer TURN-40A production repair superseded that delivery.

### Required Named-Test Commands

All and only the six plan-named test classes were requested from Maven, one command at a time from
`D:\mavenProject\dhxy-cloud-brain`:

| Command | Exit | Elapsed | Tests / failures / errors |
|---|---:|---:|---|
| `mvn -q "-Dtest=CloudTurnExchangeContractTest" test` | 1 | 32.3s | Surefire not reached; 0 tests executed |
| `mvn -q "-Dtest=CloudTurnExchangeFrameResultContractTest" test` | 1 | 21.1s | Surefire not reached; 0 tests executed |
| `mvn -q "-Dtest=CloudTemplateCatalogContractTest" test` | 1 | 22.4s | Surefire not reached; 0 tests executed |
| `mvn -q "-Dtest=CloudTurnHttpHandlerContractTest" test` | 1 | 22.4s | Surefire not reached; 0 tests executed |
| `mvn -q "-Dtest=CloudTurnRoutesContractTest" test` | 1 | 20.4s | Surefire not reached; 0 tests executed |
| `mvn -q "-Dtest=CloudTemplateHttpHandlerContractTest" test` | 1 | 21.3s | Surefire not reached; 0 tests executed |

Every command stopped in Maven's production `compile` phase before `testCompile`. The first exact compiler evidence
was identical in all six runs:

```text
src/main/java/com/bot/dhxy/service/TaskTrackerPanelService.java:[3,25] cannot find symbol
  symbol:   class GameClientTracker
  location: package com.bot.dhxy.core
```

The same production compile then reports missing `TextRecognizer`, `CoordinateHelper`, `OcrWindowScanService`,
`WindowScopedTempPath`, and other dependencies in the lifted Service/Task tree. None of those files are in this
card's write set, and no error from a TURN-T02 test file was emitted because test compilation was never entered.
Therefore this delivery records six truthful exit-1 executions, not passing tests; fresh named-test exits remain for
the parent/cohort after the production main-source compile dependency is restored.

## Concurrent Collision Integration - 2026-07-15T18:45:35.8686694-04:00

### Ownership And Collision Evidence

- This report's `CLAIMED` record at `2026-07-15T18:30:47.0898092-04:00` is the earliest TURN-T02 claim and remains
  the single final ownership record for all six tests plus the fixture.
- The later Hegel/T02A material report
  `docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-T02A.md` declared an overlapping write set for exactly
  `CloudTurnExchangeContractTest.java`, `CloudTurnExchangeFrameResultContractTest.java`, and
  `CloudTurnHttpHandlerContractTest.java`.
- No Hegel content was rolled back or overwritten. The current three files were read line by line against the T02A
  frozen assertions and its final delivery evidence. Their current byte lengths and SHA-256 values exactly equal the
  T02A final table and the unified table above:
  - Exchange: `20821` bytes / `fff4d7deb53efd961348f99c01bdc394c717077ed3af0564b0e23005918a404b`.
  - Frame result: `9829` bytes / `9f1f6103a537d1b880d3e1cda86826b07287c95a1f19ee5e38a30360a61208b7`.
  - HTTP handler: `20937` bytes / `5ee49610bf9652f1f38fd95515e68556a908fbd4e244041e34a6821223011ccd`.
- Exchange and frame-result sources already matched byte-for-byte, so integration required no synthetic rewrite.
  The HTTP source is the retained assertion superset: the original JSON/auth/multipart/bounds/SHA/dimension coverage
  remains, and Hegel's wrong-token, malformed-PNG, exact part-count, IDLE replay/no-retry, and invalid-frame recovery
  checks are present in the same class.
- The T02A report is input evidence only. It is not a separate card completion or review decision; parent Codex
  reviews this unified TURN-T02 delivery once.

### Unified Required Assertions

| Authority | Test methods retained in final source | Required contract evidence |
|---|---|---|
| `CloudTurnExchangeContractTest` | 7 (`:37`, `:73`, `:108`, `:142`, `:167`, `:201`, `:242`) | command-first, wait-first, BUSY, duplicate, late outcome, command/client interrupt, exact unresolved action redelivery and no implicit retry |
| `CloudTurnExchangeFrameResultContractTest` | 4 (`:29`, `:59`, `:87`, `:122`) | outcome+raw frame on one future, constructor/access defensive copies, partial/mismatched pair rejection, latest metadata replacement and no retained frame/outcome history |
| `CloudTemplateCatalogContractTest` | 3 (`:25`, `:54`, `:71`) | canonical key and PNG/SHA/content-hash/ETag one authority, cached identity and defensive bytes, invalid/missing/traversal/directory/non-PNG rejection |
| `CloudTurnHttpHandlerContractTest` | 6 (`:43`, `:62`, `:96`, `:154`, `:188`, `:209`) | JSON-only, raw multipart, exactly-one auth, real PNG bytes to same future, body/frame bounds, bad SHA/dimensions/PNG, exact two parts, IDLE acknowledgement/replay and no second action |
| `CloudTurnRoutesContractTest` | 4 (`:27`, `:40`, `:68`, `:87`) | supplied exchange is the command capability, handler and command share one slot, template route uses packaged catalog bytes/ETag, invalid authorities rejected |
| `CloudTemplateHttpHandlerContractTest` | 4 (`:24`, `:54`, `:77`, `:105`) | catalog-exact 200 bytes/ETag, 304, exactly-one auth, unknown/traversal/directory/raw-slash fail closed and GET-only behavior |
| `frame-2x2.png` | `126` bytes, decoded `2x2` | real PNG fixture SHA `0b4b8834d9fa2a0ee891481cd9e90eb8434a680bf92af684e33d7bd4fb0f8754`; raw bytes, SHA and dimensions are asserted |

### Unified Maven Evidence

The six standard command results in the preceding table are the unified TURN-T02 results for these final hashes.
For the three collided classes, T02A independently recorded the same exit `1`, zero Surefire executions and the same
first out-of-scope production compiler error; this corroborates the result but does not replace or approve the six
commands recorded by the final owner. No assertion failure has been observed because Maven has not yet reached
`testCompile` or Surefire.

## PARENT TEST SOURCE REVIEW #1 - 2026-07-15 18:52 EDT

- Review authority: parent Codex; neither Worker self-report nor the later overlapping T02A report was treated as
  reviewer approval.
- Verdict: `P0/P1/P2=0/0/0`.
- Status: `TEST SOURCE REVIEW PASSED / REQUIRED MAVEN TESTS + CLOUD COMPILE BLOCKED`; this is not card approval.
- The parent independently read all six test classes and the corresponding `CloudTurnExchange`, command-result/frame,
  turn HTTP handler, routes, template catalog and template HTTP handler production boundaries. The tests directly cover
  command-first/wait-first, BUSY/duplicate/late/interrupt fences, outcome+raw frame atomic completion and defensive
  copies, exact authorization, JSON/multipart bounds, invalid SHA/decoded dimensions/PNG/part count, one shared route
  exchange, and catalog-exact PNG/SHA/content-hash/ETag behavior.
- The parent independently recomputed every delivered file hash. The fixture is exactly 126 bytes, has PNG IHDR
  dimensions `2x2`, and SHA-256
  `0b4b8834d9fa2a0ee891481cd9e90eb8434a680bf92af684e33d7bd4fb0f8754`; the six Java hashes match the unified
  delivery table exactly.
- All six authoritative Maven commands remain exit `1` before `testCompile`/Surefire because Cloud production compile
  first fails at out-of-scope `TaskTrackerPanelService.java:3` on absent `GameClientTracker`, followed by other lifted
  Service dependencies. Thus zero selected tests ran and no green test or compile gate is claimed. The exact six
  commands must be rerun unchanged after the production compile cohort is restored.
- Test-source ownership is released after this review. No production source, POM, fixture or assertion repair is
  requested from the T02 Worker.

**No approved business differences; equivalent migration against baseline `696a12b0`.**
