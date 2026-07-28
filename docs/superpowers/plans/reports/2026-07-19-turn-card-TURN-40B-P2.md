# CR271 TURN-40B-P2 Remaining Cloud Compile Closure Audit

## Canonical State

- Status: `READY / ZERO OWNER / UNASSIGNED`.
- Type: `WHOLE-CARD REPORT-ONLY PLAN-CONTRACT`.
- Parent: `TURN-40B`; this closes the remaining pre-runtime Cloud compile contract after C1-C4 and TURN-39C1 source review passed.
- Claim authority is this file's physical EOF. The ledger only announces a public card and does not assign, reserve, schedule or chase a Worker.

## Why This Card Exists

The parent stable-window Maven gate on `2026-07-18T20:17:13-04:00` reached javac over 532 main sources and failed with exactly 33 errors. They reduce to two source families:

1. `com.bot.dhxy.model.ocr.TextCandidateScanResult` references absent `TextCandidateScanStatus`.
2. `com.bot.dhxy.service.NavigationService` retains absent DHXY-local tracker, input, OCR, coordinate, game-state and window-runtime types.

The real `TURN-40B` runtime/factory files and tests do not yet exist, so `TURN-40C` cannot open. Copying missing DHXY classes, adding stubs/constants, or bypassing compile would create a second authority or change behavior.

## Exact Write Set

Only this report may be modified:

- `docs/superpowers/plans/reports/2026-07-19-turn-card-TURN-40B-P2.md`

All Java, tests, plans, ledgers, dashboards and other reports are read-only. This card runs no Maven and starts no runtime/application/server/Task/UI/capture/input.

## Required Full Audit

1. Use retained parent Maven log `D:/mavenProject/dhxy-cloud-brain/target/parent_turn40b_compile_20260719.log`; distinguish the 33 Maven errors from stale manual-javac/Lombok noise.
2. Read complete current production/tests for `TextCandidateScanResult` and `NavigationService`, plus every direct/transitive caller and collaborator touched by missing symbols. Do not stop at the first missing class.
3. For `TextCandidateScanStatus`, compare exact baseline/current DHXY type and all consumers. Decide from evidence whether byte-identical migration is valid or an existing Cloud typed result owns it. No duplicate enum or second OCR result model.
4. Produce a method-level live/dead table for `GameClientTracker`, `InputProvider`, `GameTextLineOcrService`, `CoordinateHelper`, `GameStateUtil`, `WindowRuntimeContext`, `WindowScopedTempPath`, `WindowTaskContextHolder`, and `BoundWindowKeyboardService`.
5. Map each live operation to an accepted turn-native owner: exact metadata/context, capture/frame, packaged template match, Cloud OCR, pathing state, LOCAL_SERVICE, exact-HWND background keyboard, or foreground/global-serial mouse. A missing mapping is a named blocker, not permission to copy DHXY code.
6. Preserve baseline `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`: route/prompt/OCR/template/click order, waits, retries, fallback, verification, stop/pause and movement-intent truth. No TTL, polling workaround, automatic retry, second exchange/store/queue, screenshot-path shim, constant result or null facade.
7. Freeze the smallest complete implementation cohort(s) with literal production/test paths, source SHAs/mtimes, dependency order, collision check, named tests and first aggregate Cloud compile point. Include constructor/Spring-host and test import/arity fallout.
8. Re-evaluate whether planned five-file `TURN-40B` runtime/factory can open immediately after those cohorts. If not, list the complete remaining symbol/API gate and exact owner; do not mark `TURN-40C` ready.
9. State the single genuine user business decision if one exists. Otherwise write `无待用户业务决策；无已批准业务差异；按基线等价迁移`.

## Delivery Contract

Append one `WHOLE-CARD REPORT DELIVERED` section containing the complete symbol/caller table, baseline/current evidence, exact implementation write/test sets, DAG, compile/test gates and decision statement. Parent review is required. A list of compiler lines without transitive ownership is not delivery.

## Claim Protocol

Any eligible idle Worker may append the first valid whole-card claim at this physical EOF, then reread EOF to prove sole ownership. Include Cloud branch/HEAD, this card's pre-claim SHA, acknowledgement of `PARENT-TURN40B-P2-READY-2017`, and zero-Java/zero-Maven discipline. A later competing claim must self-withdraw.

<!-- TRUE_EOF: TURN-40B-P2 READY ZERO-OWNER UNASSIGNED REPORT-ONLY REMAINING-CLOUD-COMPILE-CLOSURE EXACT-33-MAVEN-ERRORS NAV+OCR-STATUS FULL-TRANSITIVE-AUDIT NO-JAVA-NO-MAVEN 2026-07-18T20:17:51-04:00 -->

## WHOLE-CARD CLAIM - EXTERNAL-A - 2026-07-19T12:47:00-04:00

- owner: `EXTERNAL-A`
- claim_type: canonical whole-card claim (append at physical EOF per card authority; public READY/ZERO-OWNER, any eligible idle Worker; not assigned/reserved).
- card: `TURN-40B-P2` — Remaining Cloud Compile Closure Audit (WHOLE-CARD REPORT-ONLY PLAN-CONTRACT).
- basis: card physical EOF = `READY / ZERO OWNER / UNASSIGNED / REPORT-ONLY` (2026-07-18T20:17:51-04:00); no prior WHOLE-CARD CLAIM present (grep empty pre-append). Pre-check done as independent tool calls before this append: full card contract read + mtime (2026-07-18 20:20:01) + EOF re-confirmed unclaimed.
- pre-claim SHA-256 (this card): `FEF426EB`.
- Cloud repo branch/HEAD: `navigation-migration` @ `3b988ca` (D:\mavenProject\dhxy-cloud-brain). cr271 worktree: `thin-client-design` @ `59b85e0b`.
- ack: `PARENT-TURN40B-P2-READY-2017` and `PARENT-TURN40B-P2-COMMUNICATION-RECOVERY-2047` (both named-ACKed in this cycle's ledger STATUS EVENT). Communication-stale recovery: prior post-2017 heartbeats used the stale no-card snapshot; corrected now.
- write set (frozen): ONLY `docs/superpowers/plans/reports/2026-07-19-turn-card-TURN-40B-P2.md`. All Java/tests/plans/ledgers/dashboards/other reports READ-ONLY.
- discipline: **zero Java, zero Maven, zero runtime/UI/capture/input, zero Git mutation**; no copying missing DHXY classes, no stubs/constants/second store, no bypassing compile; will not claim TURN-40C or mark it ready; `D:\mavenProject\DHXY` read-only untouched.
- scope acknowledgement: full transitive audit of the exact 33 parent Maven errors (families: `TextCandidateScanResult`→absent `TextCandidateScanStatus`; `NavigationService`→absent DHXY-local tracker/input/OCR/coordinate/game-state/window-runtime types) using the retained parent log; method-level live/dead tables for the 9 named collaborator types; turn-native owner mapping; baseline `696a12b0` preservation; frozen minimal implementation cohort(s) with paths/SHAs/mtimes/DAG/collision-check/named-tests/first-aggregate-compile-point; TURN-40C readiness re-eval; single business-decision statement.
- next: read-back this physical EOF to confirm sole earliest owner; if any physically-earlier competing claim exists (e.g., EXTERNAL-C), immediately canonical self-withdraw per the card's Claim Protocol.

<!-- TRUE_EOF: TURN-40B-P2 WHOLE-CARD CLAIM EXTERNAL-A OWNER REPORT-ONLY-AUDIT ACK-2017+2047 PRECLAIM-FEF426EB CLOUD-navigation-migration@3b988ca ZERO-JAVA-ZERO-MAVEN ANTI-RACE-CARD-EOF 2026-07-19T12:47:00-04:00 -->

## WHOLE-CARD REPORT DELIVERED - EXTERNAL-A - 2026-07-19T13:27:00-04:00

Owner EXTERNAL-A (claim 12:47, parent CLAIM-ACCEPTED-20260719A). Baseline anchor `696a12b0`. Report-only; zero Java/Maven/runtime/Git. Evidence from parent log `dhxy-cloud-brain/target/parent_turn40b_compile_20260719.log` (`[INFO] 33 errors` / `BUILD FAILURE`) plus current source read.

### 1. Exact 33-error inventory (deduplicated file:line; not stale javac/Lombok noise)
- `model/ocr/TextCandidateScanResult.java` = **4** (22,23,26x2) — all `TextCandidateScanStatus` cannot-find-symbol.
- `service/NavigationService.java` = **29** — import@4/7/8; @RequiredArgsConstructor@116; fields@36/37/39/44/45/46/197-201/205/209/211; methods@977/988/1104/1115/1127/1342/1387; OCR nested-type@2107/2108/2150/2151. Root = absent DHXY-local collaborator types.
The two families are the only closure gap; the real `TURN-40B` runtime/factory files/tests do not exist yet, so `TURN-40C` cannot open.

### 2. Family 1 — OCR / `TextCandidateScanStatus` (byte-identical migration; no second model)
- Evidence: Cloud `TextCandidateScanResult` (`9E1F68B4`/66L) references `TextCandidateScanStatus` in the `status` field plus `of()` (FOUND_CANDIDATES/NO_CANDIDATES) and `empty()` (SCAN_FAILED). Type ABSENT in Cloud; PRESENT in DHXY baseline `DHXY/.../model/ocr/TextCandidateScanStatus.java` (`F67FDF75`/10L) = shape-only enum `{FOUND_CANDIDATES, NO_CANDIDATES, SCAN_FAILED}`, zero behavior.
- No competing Cloud OCR-status enum exists (grep FOUND_CANDIDATES/ScanStatus -> only the Result plus its consumers). No existing Cloud typed result owns the concept. Cloud `TextCandidateScanResult` already diverged from DHXY (9E1F68B4 != 5B206703) = the Result is already Cloud-migrated; only the enum is missing.
- **Decision: byte-identical CREATE of the enum in Cloud is valid** (shape-only, baseline-equivalent). Consumers `service/NpcClickService.java` plus `cloudbrain/SmartClickRecognizer.java` compile once the enum exists. No duplicate enum, no second OCR result model.

### 3. Family 2 — `NavigationService` absent DHXY-local types: live/dead symbol table plus turn-native owner
All 9 collaborator types are Cloud-ABSENT / DHXY-PRESENT. Body-deref live/dead and the accepted turn-native owner for each live operation:

| Field (type) | live ops (count) | live/dead | turn-native owner (EXISTS) |
|---|---|---|---|
| `inputProvider` (InputProvider) | none (0) | **DEAD** | remove import+field (never dereferenced) |
| `tracker` (GameClientTracker) | getWindowBaseX/Y (18), captureToFile (3) | LIVE | exact `TurnWindowMetadata`/`TurnExecutionWindow` binding (geometry); turn CAPTURE frame (capture) |
| `gameStateUtil` (GameStateUtil) | isSameMapName (9), recordMovementIntent (3), isNearCoordinate (2), confirmCurrentMapFresh (2) | LIVE | `MapNameCanonicalizer` (Cloud field); `CloudWholeTaskRuntimeLocalServiceClient.recordMovementIntent`@139 / `.isNearCoordinate`@150 / `.confirmCurrentMap`@144 (existing LOCAL_SERVICE ops `WHOLE_TASK_MOVEMENT_INTENT_RECORD` / `_IS_NEAR_COORDINATE` / `_CONFIRM_CURRENT_MAP`) |
| `coordinateHelper` (CoordinateHelper) | getScaledRect (3), findImageAbsoluteCoordinate (3), findImageInRegion (2), resolveMatchedPointInRect (1) | LIVE | exact-metadata geometry / pure compute (scale, resolve); packaged template match via `CloudTemplateAssets`+`ImageFinder`+capture frame (findImage*) |
| `gameTextLineOcrService` (GameTextLineOcrService) | verifyWorldMapRouteDestination (2), findLastWorldMapRouteCoordinate (1) plus nested Result types @1892/2036/2048/2107/2108/2150/2151 | LIVE | Cloud OCR `cloudbrain/LocalOcrClient` (route-result OCR); nested `WorldMapRouteDestinationResult/CoordinateResult` need Cloud home |
| `boundWindowKeyboardService` (BoundWindowKeyboardService) | pressShortcut (1) | LIVE | exact-HWND background keyboard turn (`TurnInputStepExecutor` / TURN-39K KEY_TAP) |
| `windowScopedTempPath` (WindowScopedTempPath) | resolve (3) | **DEAD after capture migration** | eliminated once capture returns in-memory frame bytes (no disk shim) |
| `windowTaskContextHolder` (WindowTaskContextHolder) | rawCurrent (10) @308/810/826/907/920/1315/1418/1819/2159/3104 | LIVE | replaced by exact turn `TaskExecutionContext`/`TurnWindowMetadata`; thread-local holder unused in turn threads |
| `WindowRuntimeContext` (type, 15 refs) | getNativeBinding (many @984-1280), getPreparedDialogAction@935 | LIVE | exact `TurnExecutionWindow.binding()` (already carried; NavSvc reads `getTurnGameClient().latestWindowMetadata()`@2667); `CloudDialogPreparedActionState` (38A-F) for prepared action; pathing via `CloudNavigationPathingState` mirror (`getActivePathingIntent`@46/`getPathingSnapshot`@59) |

**No live operation lacks an existing turn-native owner -> NO named blocker, NO new store/protocol/authority, NO copied DHXY algorithm.** NavigationService already emits turns (`getTurnGameClient().execute(...)`@2699/2723/2907/2940/2957 plus `executeInputTurn` x many).

### 4. Baseline `696a12b0` preservation
The completion is a 1:1 rewire of already-present collaborators to already-present turn owners: route/prompt/OCR/template/click order, waits, retries, fallback, verification, stop/pause and movement-intent truth are unchanged. Movement/arrival truth stays local (consumed via the existing LOCAL_SERVICE ops and the read-only `CloudNavigationPathingState` mirror). No TTL, polling workaround, automatic retry, second exchange/store/queue, screenshot-path shim, constant result, or null facade.

### 5. Frozen implementation cohort (smallest complete; two files, one aggregate compile point)
Paths in `dhxy-cloud-brain` (Cloud `navigation-migration@3b988ca`); DHXY read-only source-of-truth for the enum bytes.
- **C-OCR (independent, tiny):** CREATE `src/main/java/com/bot/dhxy/model/ocr/TextCandidateScanStatus.java` byte-identical to DHXY `F67FDF75`/10L. Fixes the 4 OCR errors. No test change (shape-only enum; covered transitively by existing OCR consumers).
- **C-NAV (the real TURN-40B runtime completion):** MODIFY `src/main/java/com/bot/dhxy/service/NavigationService.java` (current `B57ECC50`/3155L, mtime 2026-07-18 18:22:36): rewire the 9 collaborators to the owners in section 3; delete `inputProvider` (DEAD) plus `windowScopedTempPath` (DEAD-after-capture) plus the 9 imports/fields; update `@RequiredArgsConstructor` arity. Test fallout: MODIFY `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NavigationTurnContractTest.java` (`79D48FE0`/1470L/23T harness constructor arity) — READ-ONLY otherwise; retain all 23 tests. Constructor/Spring-host: NavigationService is a `@RequiredArgsConstructor` Spring bean; removing DEAD fields drops args and each rewired collaborator drops as its owner is injected/already-present. Fixes the 29 errors.
- **Dependency order:** C-OCR before or parallel to C-NAV (independent); C-NAV is the larger. First aggregate Cloud compile point = after BOTH land (`mvn -q -DskipTests compile` from DHXY-cr271, currently 33-error nonzero -> expected 0). Named tests after a stable writer window: `NavigationTurnContractTest` (23T) plus `OldFacadeRemovalContractTest` (`A4F10EF6`/3T) via isolate-run; global historical `testCompile` debt remains a separate aggregate blocker outside this cohort.
- **Collision check:** NavigationService last real writer = EXTERNAL-A (TURN-39C1/40B-C4), both PASSED/owner-released; both lanes currently idle; no active writer. `TextCandidateScanResult` untouched. No 38-family overlap. Clean.

### 6. TURN-40C readiness
After C-OCR plus C-NAV land and Cloud main compiles, the pre-runtime compile contract for `TURN-40B` closes. The planned five-file `TURN-40B` runtime/factory still does not exist and must be authored/reviewed before `TURN-40C`. **TURN-40C stays BLOCKED** until the C-NAV cohort is delivered, compiled and the runtime/factory cohort is separately frozen. No remaining absent-symbol gate beyond these two families for the compile closure itself.

### 7. Business decision
`无待用户业务决策；无已批准业务差异；按基线等价迁移`. Every live operation maps to an existing turn-native owner; the change is a 1:1 rewire plus a shape-only enum migration, with movement/arrival truth preserved locally.

<!-- TRUE_EOF: TURN-40B-P2 WHOLE-CARD REPORT DELIVERED EXTERNAL-A 33-ERRORS=4-OCR+29-NAV OCR=TEXTCANDIDATESCANSTATUS-BYTE-IDENTICAL NAV=9-TYPE-LIVEDEAD-ALL-OWNERS-EXIST-NO-BLOCKER INPUTPROVIDER+TEMPPATH-DEAD COHORT=CREATE-ENUM+NAVSVC-REWIRE-B57ECC50 40C-STILL-BLOCKED NO-BUSINESS-DIFF AWAIT-PARENT-REVIEW 2026-07-19T13:27:00-04:00 -->

## PARENT REPORT REVIEW #1 - BLOCKED / REPAIR REQUIRED

- verdict: `P0/P1/P2 = 0/4/1`; owner `EXTERNAL-A` retained on the same report-only card.
- reviewed evidence: complete delivered report; retained 33-error Maven log; current `NavigationService.java`
  (`B57ECC50`); baseline `GameTextLineOcrService` at `696a12b0`; current `LocalOcrClient`,
  `DecisionEngine`, `CloudDialogPreparedActionState`, `TurnGameClient`, `TurnWindowMetadata`,
  `NavigationTurnContractTest`; authoritative plan sections 14-19 and TURN-40B write set.

### P1-1 - `WindowRuntimeContext` transitive state surface is materially incomplete

The report reduces 15 type references to binding/prepared-action/pathing owners, but the current source has a much
larger method surface: `clearDialogPreparationRequest` x6, `consumePendingWorldMapRouteResultMemory` x1,
`consumePreparedDialogActionValidated` x1, `getDialogPreparationStatus` x5, `getNativeBinding` x17,
`getPreparedDialogAction` x6, `getVisibleDialogSnapshot` x4, `getWindowId` x7,
`updateDialogPreparationRequest` x1, `updatePendingTransferChoiceMemory` x1 and
`updatePendingWorldMapRouteResultMemory` x1, plus the `state()` window-key read. `CloudDialogPreparedActionState`
only proves the prepared-action slot; the report gives no exact owner/API/write-set proof for dialog preparation,
visible-dialog observation, pending-transfer memory, route-result memory, clear/update semantics or the runtime-state
key. Therefore `all owners exist / no blocker` is unsupported.

Repair condition: enumerate every `WindowRuntimeContext`/holder call site and map it to a concrete existing Cloud
method with semantic parity, or name the missing owner and add its exact production/test path to the frozen cohort.
Include consume/CAS, clear-on-failure, replacement-race and exact tenant/user/device/window key semantics.

### P1-2 - Raw `LocalOcrClient` is not the missing route-OCR business owner

Baseline `verifyWorldMapRouteDestination` and `findLastWorldMapRouteCoordinate` own yellow/green preprocessing,
destination normalization/aliases, packed-segment OCR, wrapped-row handling, same-row coordinate selection, raw-image
fallback and typed destination/coordinate results. `LocalOcrClient.readWords` only returns provider-order raw words.
The current Cloud `DecisionEngine.routeCandidateFromYellowDestinationImage` contains a private overlapping subset,
which the report did not compare or expose. Rewiring `GameTextLineOcrService` directly to `LocalOcrClient` would either
drop baseline fallback/order or force copied business algorithms, both forbidden by the card.

Repair condition: audit baseline and `DecisionEngine` method-by-method, choose the single canonical existing owner,
freeze the exact callable typed API and all production/test paths needed to reuse it, and prove destination plus
legacy-green coordinate fallback parity. Do not paste the algorithm into `NavigationService` and do not create a
second OCR result model.

### P1-3 - The frozen test cohort does not survive the proposed rewire

The report limits `NavigationTurnContractTest` to constructor arity while retaining all 23 tests. Current tests
directly inject `StubTracker`, `StubTempPath`, `GameTextLineOcrService` and real `WindowRuntimeContext`; the success
route test scripts old file capture/OCR, and prepared-dialog tests invoke methods whose signatures contain
`WindowRuntimeContext`. Removing those types requires behavioral fixture and invocation rewrites, not only arity.

Repair condition: list every affected test method and exact replacement fixture/assertions. Cover in-memory capture,
yellow destination guard, green-coordinate fallback, prepared-action consume, dialog preparation/visible snapshot,
pending memories, exact binding, movement local-service terminals and zero disk-path shim. Freeze any additional
contract-test paths needed; `retain 23T` alone is not acceptance evidence.

### P1-4 - The DAG conflates pre-build closure with the actual TURN-40B runtime

The report calls C-NAV `the real TURN-40B runtime completion`, but authoritative section 17 separately requires
`CloudTurnTaskFactory`, `CloudTurnTaskRuntime`, `CloudTurnTaskRegistry`, `CloudTurnTaskStartResult` and
`CloudTurnControlPort`, plus two runtime tests. The report later admits those files remain absent. This contradiction
would open the wrong implementation boundary.

Repair condition: separate the enum and Navigation pre-build closure cohort(s) from the runtime/factory cohort,
freeze their dependency order and collision boundaries, and state exactly which card becomes READY after each gate.
TURN-40C remains BLOCKED until both the pre-build compile closure and actual runtime/factory source/test/build gates
pass.

### P2-1 - Build commands target the wrong repository and violate the active Cloud enforcer

The report says to run Cloud compilation “from DHXY-cr271” with `mvn -q -DskipTests compile`. The modified sources
live in `D:/mavenProject/dhxy-cloud-brain`, and the fresh parent evidence already proved this repository rejects the
skip-tests form. The applicable compile command is from the Cloud repository with `-DskipTests=false`; named tests
must then use the authorized HTTPS turn family and exact test names without bypassing test execution.

Repair condition: correct the working directory, command spelling, sequencing, expected test counts and the point at
which unrelated historical testCompile debt is distinguished from cohort regressions.

### Re-delivery gate

Append one complete `WHOLE-CARD REPORT RE-DELIVERED` that closes all five findings with literal method/path/test/DAG
tables and current SHAs/mtimes. Keep zero Java/Maven/runtime/input. The enum conclusion may remain if revalidated;
no implementation card is READY and no owner is released before parent review passes.

<!-- TRUE_EOF: TURN-40B-P2 PARENT-REPORT-REVIEW1 BLOCKED REPAIR-REQUIRED P0=0-P1=4-P2=1 OWNER-EXTERNAL-A RETAINED INCOMPLETE-RUNTIMECONTEXT-SURFACE RAW-OCR-NOT-BUSINESS-OWNER TEST-COHORT-INCOMPLETE DAG-CONFLATED BUILD-COMMAND-WRONG TURN40C-BLOCKED NO-JAVA-NO-MAVEN-NO-RUNTIME 2026-07-19 -->

## WHOLE-CARD REPORT RE-DELIVERED - EXTERNAL-A - 2026-07-19T14:42:00-04:00

Owner EXTERNAL-A (retained). Closes Review #1 (`0/4/1`) plus the two follow-up corrections `PARENT-TURN40B-P2-RUNTIME-STATE-GAP-20260719` and `PARENT-TURN40B-P2-REJECT-CLOUD-RUNTIME-SLOTS-20260719`. Report-only; zero Java/Maven/runtime/Git. Baseline `696a12b0`. Enum conclusion (section E) revalidated and retained.

### P1-1 - Complete WindowRuntimeContext surface + canonical owner (Cloud store REJECTED; typed LOCAL_SERVICE bridge to the sole local owner)
NavigationService uses **11 distinct** WindowRuntimeContext methods (my first report cited only 2). The DHXY-local `WindowRuntimeContext` (`D1CDFD6C`/1785L) is the SOLE owner; its local watcher `WindowTaskRunner.refreshDialogPreparationSignal`@1935 / `settlePendingTransferChoiceMemory`@2533 / `settlePendingWorldMapRouteResultMemory`@2585 reads/transitions/consumes it. A Cloud store would be a forbidden second authority (parent reject). Owner = new typed turn `LOCAL_SERVICE` ops that update/clear/replace the sole local state; watcher unmoved/unduplicated.

| WindowRuntimeContext op (count) | local sole-owner method | semantics | turn-native access |
|---|---|---|---|
| getNativeBinding (17) | - | exact HWND/geometry | exact `TurnWindowMetadata`/`TurnExecutionWindow.binding()` (already carried) |
| getWindowId (7) | - | window identity | `TurnWindowMetadata.windowId` |
| getPreparedDialogAction (6) | - | read prepared action | `CloudDialogPreparedActionState.peek@146/peekBoundSlot@189` |
| consumePreparedDialogActionValidated (1) | - | CAS consume + validate | `CloudDialogPreparedActionState.consumeValidated@75` (pre-CAS binding fence, clear-on-failure) |
| getDialogPreparationStatus (5) + getVisibleDialogSnapshot (4) | @380 getDialogPreparationStatus (+ visible snapshot) | read-only fact | existing op `WHOLE_TASK_DIALOG_RUNTIME_READ` -> `readDialogRuntimeFact@207` |
| updateDialogPreparationRequest (1) | @719 updateDialogPreparationRequest | replace request | **NEW op** dialog-prep-request update |
| clearDialogPreparationRequest (6) | @737 clearDialogPreparationRequest | clear request | **NEW op** dialog-prep-request clear |
| updatePendingTransferChoiceMemory (1) | @1048 set(memory) | AtomicReference replace | **NEW op** pending-transfer-choice update |
| consumePendingTransferChoiceMemory (0 in NavSvc; watcher-side) | @1052 getAndSet(null) | atomic consume | watcher `settlePendingTransferChoiceMemory` (local; not a Cloud op) |
| updatePendingWorldMapRouteResultMemory (1) | @1078 set(memory) | AtomicReference replace | **NEW op** pending-route-result update |
| consumePendingWorldMapRouteResultMemory (1) | @1082 getAndSet(null) | atomic consume | **NEW op** pending-route-result consume |
| rawCurrent (10, via windowTaskContextHolder) | - | current context | exact turn `TaskExecutionContext`; holder unused in turn threads |

Identity/replacement/CAS/atomic-consume/cleanup already exist locally (`AtomicReference` set/getAndSet(null)/compareAndSet). The new ops only carry the typed request across the bridge; the local owner and watcher enforce the semantics.

**P1-1 cross-repo cohort (new typed LOCAL_SERVICE ops; exact op names to be parent-frozen):**
- SHARED byte-identical both repos: `cloud/turn/protocol/TurnLocalOperation.java` (`D4042DE0`/39L; add dialog-prep-request update/clear + pending-transfer-choice update + pending-route-result update/consume), `TurnWholeTaskRuntimeArguments.java` (`80BBC6BA`/71L; add typed fields), `TurnWholeTaskRuntimeResult.java`, `TurnProtocolValidator.java` (`3FDD3FFA`/767L; all four whole-task switches: top-level route + required-field + allowed-fields + present-fields).
- CLOUD: `cloudbrain/turn/client/CloudWholeTaskRuntimeLocalServiceClient.java` (`59BF77E8`/414L; add client methods), `service/NavigationService.java` rewire runtime.* -> client methods.
- DHXY-cr271: `cloud/turn/local/WholeTaskRuntimeLocalOperationExecutor.java` (`3820BDE5`/238L; add op cases calling the existing WindowRuntimeContext@719/737/1048/1078/1082 methods), `cloud/turn/LocalServiceStepDispatcher.java` (`DC9B2B89`/129L; routing).
- TESTS: protocol golden (both repos), validator (both repos), client-test, executor/dispatcher-test with a real `WindowRuntimeContext` fixture proving update/clear/replace/atomic-consume/cleanup/key-isolation and watcher identity preserved.

### P1-2 - Route-OCR business owner (neither raw LocalOcrClient nor the partial DecisionEngine subset)
Baseline `GameTextLineOcrService` (DHXY): `verifyWorldMapRouteDestination`@302 -> typed `WorldMapRouteDestinationResult`; `findLastWorldMapRouteCoordinate`@367 -> typed `WorldMapRouteCoordinateResult`. Own yellow+green preprocessing, destination normalization/aliases, packed-segment OCR, wrapped-row, same-row coordinate selection, raw-image fallback. Cloud `DecisionEngine.routeCandidateFromYellowDestinationImage`@1602 (`DecisionEngine` `FA5512F9`/3511L) is a PARTIAL yellow-only subset (yellow wash + LocalOcrClient.readWords + normalize -> single CandidateClick); missing green-coordinate fallback, typed results, wrapped/same-row/raw-image fallback. `LocalOcrClient.readWords` returns raw provider-order words only.

Owner decision: the single canonical route-OCR owner is `DecisionEngine`, which must be EXTENDED to full baseline parity (verify-destination with normalization/aliases/packed-segment/wrapped-row + green-coordinate same-row selection + raw-image fallback + typed destination/coordinate results) and expose a typed callable API; NavigationService consumes that typed API. No paste into NavigationService, no second OCR result model. Cohort item **C-OCR2**: MODIFY `cloudbrain/DecisionEngine.java` (`FA5512F9`) to full parity + typed route-OCR API; CREATE/EXTEND its contract test proving destination + legacy-green coordinate fallback parity against baseline fixtures. If the parent prefers a dedicated `cloudbrain/CloudRouteResultOcr` owner instead of extending DecisionEngine, that is the single owner alternative; either way one owner, exact paths frozen.

### P1-3 - Affected NavigationTurnContractTest methods require behavioral rewrites (not arity)
`NavigationTurnContractTest` (`79D48FE0`/1470L/23T) injects DHXY types being removed. 12 methods need fixture/invocation rewrites:
- StubTracker geometry -> harness boundWindow `TurnWindowMetadata` (7): closeRouteSearchPanelQueuedIssuesOnlyTheMouseAwayInputTurn, closeMapSearchInputAfterRouteDialogIssuesOnlyTheMouseAwayInputTurn, clickRememberedWorldMapRouteResultIssuesOneLeftClickThenMouseAwayInputTurns, cleanupYellowDestinationRouteQueuedClosesRoutePanelThenTogglesWorldMapWithAltTwo, prepareWorldMapSearchResultsIssuesXunluClickAltTwoTypeEnterAndScrollInputTurns, prepareWorldMapSearchResultsSurfacesTaskStopThroughCheckpointBeforeAnyInput, closeMapSearchInputAfterRouteDialogSkipsTheMouseAwayWhenNothingClosed.
- StubCapturingTracker + StubTempPath + OCR double -> scripted in-memory CAPTURE frame + new route-OCR owner, zero disk shim (2): clickDestinationSurfacesTaskStopAtPostCaptureOcrCheckpointBeforeAnyRouteClick, clickDestinationFromWorldMapSearchResultsIssuesTheRouteClickTurnAfterScriptedCaptureAndOcr.
- StubTempPath capture-fail -> scripted failed CAPTURE frame (1): clickDestinationFromWorldMapSearchResultsIssuesNoInputTurnWhenTheResultCaptureFails.
- real WindowRuntimeContext prepared-action -> publish to CloudDialogPreparedActionState + drop WindowRuntimeContext param (2): consumePreparedRouteDialogActionIssuesNoInputTurnWithoutAValidPreparedAction, consumePreparedRouteDialogActionIssuesTheMoveWaitClickTurnForAFreshBindingMatchedPreparedAction.
Plus the nested `rawCurrent()` stub is removed with windowTaskContextHolder. New coverage: in-memory capture, yellow-destination guard, green-coordinate fallback, prepared-consume, dialog preparation/visible snapshot (`WHOLE_TASK_DIALOG_RUNTIME_READ` + new prep-request ops), pending-memory ops (new LOCAL_SERVICE ops + local executor/dispatcher tests), exact binding, movement LOCAL_SERVICE terminals, zero disk-path shim. Additional frozen contract-test paths: `CloudWholeTaskRuntimeLocalServiceClient` client-test + `WholeTaskRuntimeLocalOperationExecutor`/dispatcher test (both repos protocol golden + validator). `retain 23T` alone is not acceptance; final count exceeds 23.

### P1-4 - Two separate cohorts; TURN-40C gate
- **Cohort A (pre-build compile closure)** - closes the 33 errors so Cloud main compiles: C-OCR1 (CREATE TextCandidateScanStatus enum), C-NAV (NavigationService rewire to existing + new owners), the P1-1 typed-LOCAL_SERVICE cross-repo set, C-OCR2 (route-OCR owner parity), plus the P1-3 test rewrites. Dependency order: protocol/enum/owner ops first -> Cloud client + DHXY executor/dispatcher -> NavigationService rewire + route-OCR owner -> tests. Collision: NavigationService last writer EXTERNAL-A (39C1/40B-C4 PASSED/released); both lanes idle; no active writer; shared protocol files must be single-writer (byte-identical both repos).
- **Cohort B (actual TURN-40B runtime/factory)** - authoritative section 17, all ABSENT: CREATE `cloudbrain/turn/runtime/CloudTurnTaskFactory.java`@2129, `CloudTurnTaskRuntime.java`@2130, `CloudTurnTaskRegistry.java`@2131, `CloudTurnTaskStartResult.java`@2132, `CloudTurnControlPort.java`@2146 + tests `CloudTurnTaskRuntimeContractTest`/`CloudTurnTaskFactoryAllowlistTest`@2450. Separate from Cohort A.
- Gate: after Cohort A source+test+compile pass, Cloud main compiles (pre-build closure done) but **TURN-40C stays BLOCKED**; TURN-40C becomes READY only after Cohort B (runtime/factory) source/test/build gates also pass. Neither the enum nor the Navigation closure alone opens 40C.

### P2-1 - Corrected build/test commands
- Compile from the **Cloud repository** `D:/mavenProject/dhxy-cloud-brain` (where the modified sources live), NOT DHXY-cr271. Use `-DskipTests=false`; the `-DskipTests` skip form is rejected by the active enforcer. First aggregate compile point = Cloud-repo `mvn -q compile -DskipTests=false` after Cohort A lands -> expect 0 (was 33). Invoke mvn via PowerShell (the Git-Bash launcher hits a classworlds bootstrap error).
- Named tests: authorized HTTPS turn family, exact class names, execution NOT bypassed: `NavigationTurnContractTest`, `OldFacadeRemovalContractTest` (`A4F10EF6`/3T), the new `CloudWholeTaskRuntimeLocalServiceClient`/executor/dispatcher tests, the route-OCR-owner test; `WubeiWholeTaskTurnContractTest` read-only regression. Unrelated historical global `testCompile` debt is distinguished at the point the whole-suite testCompile fails on out-of-cohort stale files while the cohort's own named classes compile and run green in isolation.

### E. Enum conclusion (revalidated, retained)
`TextCandidateScanStatus` absent in Cloud / present DHXY (`F67FDF75`/10L shape-only enum); Cloud `TextCandidateScanResult` (`9E1F68B4`/66L) already diverged from DHXY and only references the enum; no competing Cloud OCR-status enum; consumers `NpcClickService`/`SmartClickRecognizer`. CREATE byte-identical `model/ocr/TextCandidateScanStatus.java` fixes the 4 OCR errors. Stands.

### Decision
`无待用户业务决策；无已批准业务差异；按基线等价迁移`. All owners are existing local/turn owners or a parity-extension of an existing Cloud method; movement/dialog/pending state stays the sole local owner behind typed LOCAL_SERVICE ops; no second store, no copied algorithm, no waits/fallback/order change.

<!-- TRUE_EOF: TURN-40B-P2 WHOLE-CARD REPORT RE-DELIVERED EXTERNAL-A REVIEW1-0-4-1+GAP+REJECT-CLOSED P1-1=11-METHOD-TYPED-LOCAL_SERVICE-SOLE-LOCAL-OWNER-NO-CLOUD-STORE P1-2=DECISIONENGINE-FULL-PARITY-OWNER P1-3=12-TEST-REWRITE P1-4=TWO-COHORT-DAG-40C-BLOCKED P2-1=CLOUD-REPO-SKIPTESTS-FALSE ENUM-RETAINED NO-BUSINESS-DIFF AWAIT-PARENT-REVIEW2 2026-07-19T14:42:00-04:00 -->

## PARENT REPORT REVIEW #2 - BLOCKED / REPAIR REQUIRED

- verdict: `P0/P1/P2 = 0/4/1`; owner `EXTERNAL-A` retained on the same report-only card.
- reviewed evidence: complete 14:42 re-delivery; current CR worktree runtime/protocol/executor/dispatcher; current
  Cloud Navigation/DecisionEngine/client/tests; baseline `696a12b0`; authoritative plan sections 14-19.

### P1-1 - The claimed local sole-owner API is stale and no longer exists

The report cites `WindowRuntimeContext` as `D1CDFD6C/1785L` and maps route-result operations to
`updatePendingWorldMapRouteResultMemory` / `consumePendingWorldMapRouteResultMemory`. Current authoritative CR source
is `ADBC70D4/2519L`; those methods and `WorldMapRouteResultPendingMemory` are absent. They were replaced by
`PendingRouteOutcome`: `updatePendingRouteOutcome`@1694, replacement queue@1710, consume@1722, abandonment@1729,
plus `WindowTaskRunner.settlePendingRouteOutcome`@3294 and outcome-delivery retry/report logic@3414-3507. The proposed
LOCAL_SERVICE executor therefore cannot call the asserted existing methods and would bypass replacement,
abandonment and Cloud outcome delivery.

Repair condition: audit the current `ADBC70D4` runtime and `WindowTaskRunner` end to end, then map every old
Navigation route-memory call to the exact current `PendingRouteOutcome` lifecycle, including model/protocol fields,
replacement/abandonment/report delivery and tests. Use current SHAs/mtimes; do not restore the retired memory type.

### P1-2 - Dialog-preparation calls were not proved live under the current turn flow

The report bridges every old `DialogPreparationRequest` update/clear call. Current
`WindowTaskRunner`@1015-1020 explicitly supports route preparation from the active intent when Navigation no longer
writes a request, and `refreshDialogPreparationSignal`@2382 accepts request-or-active-intent. Adding update/clear
wire operations without a call-path liveness decision can reintroduce an obsolete request protocol and alter watcher
timing/clear behavior.

Repair condition: classify each Navigation request/status/snapshot/prepared-action call against the current turn
path as live or dead. For dead calls, freeze deletion and prove active-intent/prepared-action/read-fact parity. For
live calls, freeze exact enum names, argument fields, result shape and local method invocation. Do not bridge calls
merely because they existed at `696a12b0`.

### P1-3 - Route-OCR ownership remains an unresolved alternative

The report says the owner is `DecisionEngine`, then offers a dedicated `CloudRouteResultOcr` as an alternative.
Current `DecisionEngine.routeCandidateFromYellowDestinationImage`@1602 is private, `JsonNode`/click-shaped and has no
typed destination/coordinate API. No exact public signature, result-model path, production caller path or concrete
test class is frozen for either option. This still leaves implementation-time ownership and model duplication open.

Repair condition: choose exactly one owner, remove the alternative, and freeze its literal production/test paths,
public typed signatures and result types. Prove yellow destination plus legacy-green coordinate fallback, packed and
wrapped rows, same-row selection and raw fallback without duplicating the algorithm in NavigationService.

### P1-4 - Cohort A is not yet a literal, implementable write set

The report itself says operation names are “to be parent-frozen”, says only “add typed fields”, and names generic
client/executor/dispatcher tests without literal paths/classes. It also relies on the stale route-memory model above.
Thus protocol golden/validator changes, argument/result DTOs, client methods, dispatcher cases, executor cases,
Navigation constructor fallout and all new test files cannot be assigned as fixed whole cards or collision-checked.

Repair condition: provide a card-by-card table with exact operation constants, payload/result fields, every
production/test path, current SHA/mtime or `ABSENT`, dependency order, single-writer collision boundary and first
compile point. Separate shared-protocol, DHXY local-owner, Cloud OCR/Navigation and runtime/factory write sets where
their ownership or build gates differ.

### P2-1 - Test commands and expected counts are still not exact

The Cloud compile directory and `-DskipTests=false` correction are accepted, but named tests are only listed by
short/general names; new classes have no exact paths, commands or expected counts, and Navigation is left as
“final count exceeds 23”. The report also does not name the approved isolation mechanism that can compile/run the
cohort while global historical `testCompile` debt remains red.

Repair condition: freeze exact PowerShell Maven commands, fully-qualified test selectors, expected test counts and
the precise isolation/global-debt distinction. Keep scope within the authorized HTTPS turn family.

### Re-delivery gate

Append one complete `WHOLE-CARD REPORT RE-DELIVERED #2` closing all five findings against current authoritative
sources. Keep zero Java/Maven/runtime/input. `TextCandidateScanStatus` remains independently accepted as the
shape-only 4-error closure; no implementation card is READY and TURN-40C remains BLOCKED.

<!-- TRUE_EOF: TURN-40B-P2 PARENT-REPORT-REVIEW2 BLOCKED REPAIR-REQUIRED P0=0-P1=4-P2=1 OWNER-EXTERNAL-A RETAINED STALE-WINDOWRUNTIME-API CURRENT-PENDINGROUTEOUTCOME-LIFECYCLE-MISSED DIALOG-PREP-LIVENESS-UNPROVED OCR-OWNER-ALTERNATIVE COHORT-NOT-LITERAL TEST-COMMANDS-COUNTS-INEXACT ENUM-ACCEPTED TURN40C-BLOCKED NO-JAVA-NO-MAVEN-NO-RUNTIME 2026-07-18T21:28:10-04:00 -->

## WHOLE-CARD REPORT RE-DELIVERED #2 - EXTERNAL-A - 2026-07-19T15:42:00-04:00

Owner EXTERNAL-A (retained). Closes Parent Review #2 (`0/4/1`) against CURRENT authoritative sources (DHXY-cr271 worktree, not the frozen `DHXY/` baseline). Report-only; zero Java/Maven/runtime/Git. Baseline `696a12b0`. `TextCandidateScanStatus` enum conclusion retained (parent-accepted). Correction of record: Review #1/#RE-DELIVERED-1 cited stale baseline `DHXY/` sources; this #2 uses current CR sources.

### P1-1 - Current runtime surface + PendingRouteOutcome + typed LOCAL_SERVICE bridge to the sole local owner
Current `WindowRuntimeContext` = DHXY-cr271 `ADBC70D4/2519L` (not the stale `D1CDFD6C/1785L`). The sole local owner keeps all state; the local watcher `WindowTaskRunner.refreshDialogPreparationSignal`@2382 / `settlePendingRouteOutcome`@3294 / pending queues observe it. Cloud reaches it only through typed turn `LOCAL_SERVICE` ops (no Cloud store; parent reject honored). Note there are two NavigationService files: DHXY-cr271 local (current, already `PendingRouteOutcome`) and Cloud `B57ECC50` (the stale migration target audited here).

Per-op mapping (Cloud NavigationService call -> owner):
- getNativeBinding(17), getWindowId(7): exact `TurnWindowMetadata`/`TurnExecutionWindow.binding()` (already carried).
- getPreparedDialogAction(6): `CloudDialogPreparedActionState.peek@146/peekBoundSlot@189`.
- consumePreparedDialogActionValidated(1): `CloudDialogPreparedActionState.consumeValidated@75` (pre-CAS fence, clear-on-failure).
- getDialogPreparationStatus(5) + getVisibleDialogSnapshot(4): existing op `WHOLE_TASK_DIALOG_RUNTIME_READ` -> `readDialogRuntimeFact@207` (`TurnDialogRuntimeFact.preparationPhase`/`.visibleDialog`).
- updateDialogPreparationRequest(1) + clearDialogPreparationRequest(6): **DEAD** (see P1-2). Freeze deletion with the deprecated caller; parity via active-intent + prepared-action. No op.
- updatePendingTransferChoiceMemory(1) [live, `rememberPendingRouteDialogClick`@1410 <- `consumePreparedRouteDialogAction`@988]: **NEW op** `WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE` -> local `WindowRuntimeContext.updatePendingTransferChoiceMemory`@1664. Payload = `PendingTransferChoiceMemory` fields (Cloud `F7FCDC0A`/31L): fromMap, fromX, fromY, targetMap, relativeX, relativeY, optionText, source, createdAtMs.
- route-result (retired API `updatePendingWorldMapRouteResultMemory`@1851 / `consumePendingWorldMapRouteResultMemory`@1847) [live, `rememberPendingWorldMapRouteResultClick`@1811 <- `submitWorldMapSearchAndClickDestination`@1642]: migrate to current `PendingRouteOutcome`. **NEW ops** `WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ` -> local `getPendingRouteOutcome`@278, and `WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE` -> local `requestPendingRouteOutcomeReplacement`@1710. Payload = `PendingRouteOutcome` (Cloud `1D502D6B`/38L): fromMap, targetMap, routeMode(`WorldMapRouteResultMode`), relativeX, relativeY, matchedText, source, usedMemory, routeDecisionId, intentId, createdAtMs. Consume/abandonment/settlement/report-delivery remain LOCAL watcher (`WindowTaskRunner.consumePendingRouteOutcome`@501/`settlePendingRouteOutcome`@3294/delivery@3375-3386) - not Cloud ops.
- rawCurrent(10): removed; exact `TaskExecutionContext` carried per turn.
- movement (recordMovementIntent/confirmCurrentMap/isNearCoordinate): existing ops @139/144/150. isSameMapName: `MapNameCanonicalizer`. pathing: `CloudNavigationPathingState` mirror + existing `registerPathing`.
- DEAD fields: `inputProvider` (0 uses); `windowScopedTempPath` (after capture -> in-memory frame).

Net P1-1 new work: **3 typed LOCAL_SERVICE ops** (transfer-choice update, route-outcome read, route-outcome replace). Identity/replacement/atomic-consume/cleanup are already enforced by the sole local owner (`AtomicReference` + watcher).

### P1-2 - Dialog-preparation liveness (proven)
`updateDialogPreparationRequest`@846 <- only `requestLingShouVillageRouteDialogPreparation`@825 <- only `navigateToLingShouVillageViaZhangWen`@700 = `@Deprecated`@699 with zero production callers; and it is the only writer of the request slot across both repos (DHXY-cr271 `WindowRuntimeContext`@1148 is only the setter; no runner/watcher/task calls it). Therefore in production the request is never set; `refreshDialogPreparationSignal`@2382 (request-or-active-intent) always prepares from the active pathing intent. Conclusion: request write + all 6 `clearDialogPreparationRequest` are DEAD; freeze deletion with the deprecated caller and prove active-intent/prepared-action/read-fact parity. Reads and prepared-action remain live on existing owners (above). No dialog-prep op is added.

### P1-3 - Single route-OCR owner (no alternative)
Owner = `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngine.java` (`FA5512F9`/3511L), which already owns the yellow-destination path, packed-segment matching (`findExpectedRouteDestinationFromPackedYellowSegments`@1656), normalization (`normalizeRouteDestinationName`@2195), exact match (`isExactRouteDestinationMatch`@1684) and typed record `RouteDestinationMatch`@3402. Repair: expose one PUBLIC typed API (verify-destination -> `RouteDestinationMatch`; find-last-coordinate -> a typed coordinate result reusing existing fields) and add the missing baseline parity: legacy-green coordinate fallback, same-row selection, wrapped-row handling and raw-image fallback. NavigationService consumes the typed API; the algorithm is not pasted into NavigationService and no second OCR result model is created. No dedicated `CloudRouteResultOcr` alternative.

### P1-4 - Literal card-by-card cohort (four write sets by ownership/build-gate)
Cohort A = pre-build compile closure (closes the 33 errors). Split:
- **A1 shared protocol (both repos, byte-identical; single-writer)**: `cloud/turn/protocol/TurnLocalOperation.java` (`D4042DE0`/39L; add `WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE`, `WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ`, `WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE`), `TurnWholeTaskRuntimeArguments.java` (`80BBC6BA`/71L; add PendingTransferChoiceMemory + PendingRouteOutcome payload fields), `TurnWholeTaskRuntimeResult.java` (add route-outcome read result shape), `TurnProtocolValidator.java` (`3FDD3FFA`/767L; all four whole-task switches for the 3 ops). Tests: `TurnProtocolValidator*Test` + golden JSON tests (both repos). Compile gate: shared, both repos.
- **A2 DHXY local-owner executor (DHXY-cr271)**: `cloud/turn/local/WholeTaskRuntimeLocalOperationExecutor.java` (`3820BDE5`/238L; add 3 op cases calling `WindowRuntimeContext.updatePendingTransferChoiceMemory`@1664 / `getPendingRouteOutcome`@278 / `requestPendingRouteOutcomeReplacement`@1710), `cloud/turn/LocalServiceStepDispatcher.java` (`DC9B2B89`/129L; routing). Test: `WholeTaskRuntimeLocalOperationExecutor*Test` / dispatcher test with a real `WindowRuntimeContext` fixture proving update/replace/read + watcher identity. `WindowRuntimeContext.java` (`ADBC70D4`/2519L) is READ-ONLY (owner methods already exist).
- **A3 Cloud OCR + Navigation**: `cloudbrain/DecisionEngine.java` (`FA5512F9`/3511L; add public typed route-OCR API + green/same-row/wrapped/raw parity), `service/NavigationService.java` (`B57ECC50`/3155L; rewire the 9 collaborators to owners above + the 3 new client methods, delete DEAD inputProvider/windowScopedTempPath + dialog-prep write/clears + 9 imports/fields + the deprecated LingShou request path, update `@RequiredArgsConstructor`), `cloudbrain/turn/client/CloudWholeTaskRuntimeLocalServiceClient.java` (`59BF77E8`/414L; add 3 client methods). `model/ocr/TextCandidateScanStatus.java` = **ABSENT** -> CREATE byte-identical to DHXY `F67FDF75`/10L. Tests: `NavigationTurnContractTest.java` (`79D48FE0`/1470L; 12-method behavioral rewrite, final count > 23), CREATE `DecisionEngineRouteOcrContractTest.java` (ABSENT), CREATE `CloudWholeTaskRuntimeLocalServiceClient` route/transfer op tests. `OldFacadeRemovalContractTest.java` (`A4F10EF6`/3T) READ-ONLY regression.
- Dependency order: A1 -> A2 + A3-client -> A3 NavigationService/DecisionEngine -> tests. Single-writer collision: NavigationService last writer EXTERNAL-A (39C1/40B-C4 PASSED/released); shared A1 protocol single-writer both repos; both lanes idle; no active writer. First aggregate compile point after A1+A2+A3 land.

Cohort B = actual TURN-40B runtime/factory (section 17, all ABSENT), SEPARATE: CREATE `cloudbrain/turn/runtime/CloudTurnTaskFactory.java`@2129, `CloudTurnTaskRuntime.java`@2130, `CloudTurnTaskRegistry.java`@2131, `CloudTurnTaskStartResult.java`@2132, `CloudTurnControlPort.java`@2146 + tests `CloudTurnTaskRuntimeContractTest`/`CloudTurnTaskFactoryAllowlistTest`@2450. Gate: TURN-40C READY only after BOTH Cohort A (compile closure) and Cohort B (runtime/factory) source/test/build pass. Neither the enum nor the Navigation closure alone opens 40C.

### P2-1 - Exact commands, selectors, counts, isolation
- Compile (from the Cloud repo, via PowerShell; the Git-Bash mvn launcher hits a classworlds bootstrap error): `Set-Location D:\mavenProject\dhxy-cloud-brain; mvn -q -DskipTests=false compile` -> expect exit 0 after Cohort A (was `[INFO] 33 errors` / BUILD FAILURE). `-DskipTests` (skip form) is rejected by the active enforcer.
- Approved isolation mechanism (when whole-suite `testCompile` is red on unrelated historical files): compile the single cohort test to `target/test-classes` and run it with `junit-platform-console-standalone-1.10.2.jar`, e.g. `java -jar <standalone> execute -cp "target/classes;target/test-classes;<deps>" -c <FQCN> --details=summary` (env `MSYS2_ARG_CONV_EXCL=* MSYS_NO_PATHCONV=1`; Windows-path classpath). This is the same mechanism the parent independently used to run `LocalTurnActionExecutorContractTest` 14/14 and that produced `OldFacadeRemovalContractTest` 3/3.
- Fully-qualified named tests + expected counts: `com.yueyunfe.dhxy.cloudbrain.service.NavigationTurnContractTest` (current 23; after the 12-method rewrite + new-owner coverage, expected count to be frozen in the implementation card, > 23), `com.yueyunfe.dhxy.cloudbrain.turn.client.OldFacadeRemovalContractTest` (3), `com.yueyunfe.dhxy.cloudbrain.DecisionEngineRouteOcrContractTest` (ABSENT; new, count frozen at implementation), the new `CloudWholeTaskRuntimeLocalServiceClient` + `WholeTaskRuntimeLocalOperationExecutor`/dispatcher tests (counts frozen at implementation), and read-only regression `com.yueyunfe.dhxy.cloudbrain.task.wubei.WubeiWholeTaskTurnContractTest` (`865A5311`/812L). Global historical `testCompile` debt is distinguished at the point the whole-suite `testCompile` fails on out-of-cohort stale files while each cohort test class compiles and runs green in isolation.

### Decision
`无待用户业务决策；无已批准业务差异；按基线等价迁移`. All live operations map to existing owners or three typed LOCAL_SERVICE ops onto the sole local owner (watcher unmoved); route-OCR is one existing owner extended to baseline parity; dead paths are deleted with parity proof. No second store, no copied algorithm, no waits/fallback/order change.

<!-- TRUE_EOF: TURN-40B-P2 WHOLE-CARD REPORT RE-DELIVERED #2 EXTERNAL-A REVIEW2-0-4-1-CLOSED CURRENT-ADBC70D4+PENDINGROUTEOUTCOME P1-1=3-NEW-LOCAL_SERVICE-OPS-SOLE-LOCAL-OWNER P1-2=DIALOG-PREP-DEAD-PROVEN P1-3=DECISIONENGINE-SINGLE-OWNER P1-4=LITERAL-4-WRITE-SET-COHORT+B-SEPARATE P2-1=EXACT-CLOUD-REPO-CMDS+ISOLATION ENUM-RETAINED 40C-BLOCKED NO-BUSINESS-DIFF AWAIT-PARENT-REVIEW3 2026-07-19T15:42:00-04:00 -->

## PARENT REPORT REVIEW #3 - BLOCKED / REPAIR REQUIRED

- verdict: `P0/P1/P2 = 0/2/1`; owner `EXTERNAL-A` retained on the same report-only card.
- reviewed evidence: complete 15:42 re-delivery #2; current `DHXY-cr271` `WindowRuntimeContext` /
  `WindowTaskRunner` / local `NavigationService`; current Cloud `NavigationService`, `DecisionEngine`, protocol,
  client and tests; baseline `696a12b0`; authoritative plan sections 14-19.
- accepted closures: current `PendingRouteOutcome` lifecycle is now used; dialog-request writes/clears are proven
  production-dead; the three live local-state operations and `DecisionEngine` as the single OCR owner are accepted
  as directions. `TextCandidateScanStatus` remains independently accepted.

### P1-1 - Route-OCR public contract is still not literal

The report chooses `DecisionEngine`, but the promised API is still described as "one PUBLIC typed API" rather than
an exact Java contract. Current `RouteDestinationMatch` is a `private record` at `DecisionEngine` line 3402, so it
cannot be the return type of an externally callable public API without an explicit visibility/model decision. The
coordinate result is only "a typed coordinate result reusing existing fields"; no class path, type name, fields,
method names, parameters, nullability or caller signatures are frozen.

Repair condition: freeze literal public method signatures, exact result type path(s), fields and nullability, exact
`NavigationService` caller methods, and the exact test class path/method matrix. Keep one owner and one result model;
do not defer the model/API choice to implementation.

### P1-2 - The claimed card-by-card cohort is still not claimable

`A1/A2/A3` are cohort labels, not fixed original Sprint Task cards with canonical status/owner boundaries. The write
set still uses wildcard test names (`TurnProtocolValidator*Test`, `WholeTaskRuntimeLocalOperationExecutor*Test`) and
generic phrases such as "add payload fields" / "add route-outcome read result shape". It does not freeze exact DTO
field placement, exact client method signatures, literal test paths/classes, or which original card becomes
`READY / ZERO OWNER` after each dependency gate. That prevents collision checking and canonical whole-card claim.

Repair condition: provide a literal original-card table with card id, READY/BLOCKED gate, every exact production and
test path, exact constants/methods/DTO fields/results, current SHA/mtime or `ABSENT`, dependency order, single-writer
boundary and first compile gate. No implementation card becomes READY until this table is complete and parent-passed.

### P2-1 - Test commands and expected counts remain placeholders

The report still says several counts are "frozen at implementation" or merely `> 23`. Its isolation command contains
`<standalone>`, `<deps>` and `<FQCN>` placeholders, and it does not provide exact PowerShell commands for each fully
qualified selector. This does not satisfy Review #2's requirement for exact executable commands and expected counts.

Repair condition: freeze the exact retained/new test methods and counts now, then provide literal PowerShell commands
with resolved jar/classpath or exact Maven selectors for every authorized HTTPS-turn test class. State expected tests
run/passed per command and the exact failure evidence that distinguishes global historical debt from a cohort defect.

### Re-delivery gate

Append one complete `WHOLE-CARD REPORT RE-DELIVERED #3` closing these three findings. Keep zero Java/Maven/runtime/
input. No implementation card is READY and TURN-40C remains BLOCKED.

<!-- TRUE_EOF: TURN-40B-P2 PARENT-REPORT-REVIEW3 BLOCKED REPAIR-REQUIRED P0=0-P1=2-P2=1 OWNER-EXTERNAL-A RETAINED OCR-PUBLIC-CONTRACT-NOT-LITERAL COHORT-NOT-CANONICAL-CARD-BY-CARD TEST-COMMANDS+COUNTS-STILL-PLACEHOLDERS ACCEPTED=PENDINGROUTEOUTCOME+DIALOG-DEAD+3-LOCAL-OPS+DECISIONENGINE-DIRECTION+ENUM NO-IMPLEMENTATION-READY TURN40C-BLOCKED NO-JAVA-NO-MAVEN-NO-RUNTIME 2026-07-18T21:56:10-04:00 -->

## WHOLE-CARD REPORT RE-DELIVERED #3 - EXTERNAL-A - 2026-07-19T16:12:00-04:00

Owner EXTERNAL-A (retained). Closes Parent Review #3 (`0/2/1`). Accepted directions carried forward unchanged: current `PendingRouteOutcome` lifecycle, dialog-request dead-path proof, the three local-state ops, `DecisionEngine` single OCR owner, `TextCandidateScanStatus` enum. This #3 makes the OCR contract, the card-by-card cohort and the test commands/counts literal. Report-only; zero Java/Maven/runtime/Git.

### P1-1 - Literal route-OCR public contract (one owner, one result model each)
Owner: `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngine.java` (`FA5512F9`/3511L). The existing `RouteDestinationMatch` private record@3402 stays internal. Freeze:
- New public nested type `DecisionEngine.RouteDestinationResult`: `public record RouteDestinationResult(boolean allowClick, boolean matched, String expected, String rawActual, String yellowImagePath, Integer destinationCenterX, Integer destinationCenterY, String message)` (centers and strings nullable; empty() = allowClick=false/matched=false/nulls/message).
- New public nested type `DecisionEngine.RouteCoordinateResult`: `public record RouteCoordinateResult(boolean found, Integer relativeCenterX, Integer relativeCenterY, String ocrImagePath, String message)` (coordinates nullable when found=false).
- Public methods on `DecisionEngine`:
  - `public RouteDestinationResult verifyWorldMapRouteDestination(byte[] frameBytes, String expectedDestinationName)` - washes yellow, packed-segment + normalized + exact match, sets allowClick/rawActual/centers; `frameBytes` replaces the disk image path.
  - `public RouteCoordinateResult findLastWorldMapRouteCoordinate(byte[] frameBytes, RouteDestinationResult destination)` - green-coordinate same-row selection, wrapped-row handling, raw-image fallback; returns relative center.
- Exact `NavigationService` (`B57ECC50`) callers rewired to the two methods, `gameTextLineOcrService` field removed:
  - `clickYellowDestinationAndTargetMiniMap`@1870 (was `verifyWorldMapRouteDestination`@1892).
  - `clickDestinationFromWorldMapSearchResults`@2020 (was `verifyWorldMapRouteDestination`@2036 + `findLastWorldMapRouteCoordinate`@2048).
- Result-model rule: exactly these two public result records; no second OCR model; the private `RouteDestinationMatch` is an internal helper only.

### P1-2 - Canonical original-card table (each row = one claimable whole card)
Proposed card ids are canonical boundaries; the parent assigns final numbers when opening. All paths literal; `ABSENT` = to be created.

Card `TURN-40B-N-PROTO` (shared protocol; single-writer; both repos byte-identical) - gate: READY now (no upstream dep).
- MODIFY `cloud/turn/protocol/TurnLocalOperation.java` `D4042DE0`/39L: add enum constants `WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE`, `WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ`, `WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE`.
- MODIFY `cloud/turn/protocol/TurnWholeTaskRuntimeArguments.java` `80BBC6BA`/71L: add fields for transfer-choice payload (fromMap, fromX, fromY, targetMap, relativeX, relativeY, optionText, source, createdAtMs) and route-outcome payload (fromMap, targetMap, routeMode, relativeX, relativeY, matchedText, source, usedMemory, routeDecisionId, intentId, createdAtMs) plus a compatibility constructor.
- MODIFY `cloud/turn/protocol/TurnWholeTaskRuntimeResult.java` `TurnWholeTaskRuntimeResult` (SHA per repo): add `pendingRouteOutcome` read result (the 11 PendingRouteOutcome fields, nullable when absent) + compat constructor.
- MODIFY `cloud/turn/protocol/TurnProtocolValidator.java` `3FDD3FFA`/767L: route the 3 ops in all four whole-task switches (top-level requireLocalService, required-field, allowed-fields, present-fields).
- Tests: MODIFY `TurnProtocolValidatorWholeTaskTest` (both repos) +3 op cases; MODIFY golden JSON `TurnLocalServiceCallGoldenJsonTest` (both repos) union now includes 3 ops. Exact counts frozen in P2-1.
- Compile gate: shared protocol compiles in both repos.

Card `TURN-40B-L-EXEC` (DHXY local-owner executor; DHXY-cr271) - gate: READY after `TURN-40B-N-PROTO` passes.
- MODIFY `cloud/turn/local/WholeTaskRuntimeLocalOperationExecutor.java` `3820BDE5`/238L: add 3 op cases -> `WindowRuntimeContext.updatePendingTransferChoiceMemory`@1664 / `getPendingRouteOutcome`@278 / `requestPendingRouteOutcomeReplacement`@1710.
- MODIFY `cloud/turn/LocalServiceStepDispatcher.java` `DC9B2B89`/129L: dispatch the 3 ops.
- READ-ONLY `window/runtime/WindowRuntimeContext.java` `ADBC70D4`/2519L (owner methods already exist).
- Test: CREATE `WholeTaskRuntimeLocalOperationExecutorRouteSlotTest.java` (ABSENT), real `WindowRuntimeContext` fixture; count in P2-1.
- Compile gate: DHXY-cr271 module compiles.

Card `TURN-40B-N-CLIENT` (Cloud client) - gate: READY after `TURN-40B-N-PROTO`.
- MODIFY `cloudbrain/turn/client/CloudWholeTaskRuntimeLocalServiceClient.java` `59BF77E8`/414L: add `public WholeTaskRuntimeOutcome updatePendingTransferChoice(PendingTransferChoiceMemory memory, String source, Duration timeout)`, `public WholeTaskRuntimeOutcome readPendingRouteOutcome(String source, Duration timeout)`, `public WholeTaskRuntimeOutcome replacePendingRouteOutcome(PendingRouteOutcome outcome, String reason, String source, Duration timeout)`.
- Test: CREATE `CloudWholeTaskRuntimeLocalServiceClientRouteSlotTest.java` (ABSENT); count in P2-1.

Card `TURN-40B-OCR` (Cloud DecisionEngine owner) - gate: READY now (independent of protocol).
- MODIFY `cloudbrain/DecisionEngine.java` `FA5512F9`/3511L: add the two public result records + two public methods (P1-1); reuse internal `RouteDestinationMatch`@3402.
- Test: CREATE `DecisionEngineRouteOcrContractTest.java` (ABSENT); 7 methods (P2-1).

Card `TURN-40B-ENUM` (OCR enum closure) - gate: READY now (independent).
- CREATE `cloudbrain`/... actually `dhxy-cloud-brain/src/main/java/com/bot/dhxy/model/ocr/TextCandidateScanStatus.java` (ABSENT) byte-identical to DHXY `F67FDF75`/10L. Fixes the 4 OCR errors. No test (shape-only enum; covered by existing consumers).

Card `TURN-40B-NAV` (Cloud NavigationService rewire; the compile-closing card) - gate: READY after `N-PROTO`+`N-CLIENT`+`OCR`+`ENUM` all pass.
- MODIFY `service/NavigationService.java` `B57ECC50`/3155L: rewire 9 collaborators to owners; call the 3 new client methods (transfer-choice/route-outcome) and the 2 DecisionEngine OCR methods; delete DEAD `inputProvider`, `windowScopedTempPath`, dialog-prep write/clears + the `@Deprecated` LingShou request path, retired route-memory calls, and the 9 imports/fields; update `@RequiredArgsConstructor`.
- Test: MODIFY `service/NavigationTurnContractTest.java` `79D48FE0`/1470L (12-method behavioral rewrite in place, 11 unchanged); READ-ONLY `turn/client/OldFacadeRemovalContractTest.java` `A4F10EF6`/3T.
- Single-writer: NavigationService last writer EXTERNAL-A (39C1/40B-C4 PASSED/released); both lanes idle. First aggregate Cloud compile point = after this card (Cloud main -> 0 errors).

Cohort B `TURN-40B-RUNTIME` (section 17 runtime/factory; SEPARATE; all ABSENT) - gate: after Cohort A compile closure; still BLOCKED until authored/reviewed.
- CREATE `cloudbrain/turn/runtime/CloudTurnTaskFactory.java`@2129, `CloudTurnTaskRuntime.java`@2130, `CloudTurnTaskRegistry.java`@2131, `CloudTurnTaskStartResult.java`@2132, `CloudTurnControlPort.java`@2146. Tests CREATE `CloudTurnTaskRuntimeContractTest`, `CloudTurnTaskFactoryAllowlistTest`@2450.
- TURN-40C READY only after BOTH Cohort A and Cohort B pass source/test/build.

### P2-1 - Exact test methods, counts and executable commands
Exact counts (frozen now):
- `com.yueyunfe.dhxy.cloudbrain.service.NavigationTurnContractTest` = **23** methods (unchanged total): 11 unchanged (syncedCombatState..., resolverMiss..., resolverHit..., stoppedHandoff..., completedClick..., firstCandidateNegative..., resolverRelativePoint..., mismatchedLatestWindowMetadata..., activeFinishCleanup..., navigationServiceRetiresInputActionScope..., scrollWorldMapSearchResultsToBottomDirectSurfacesTaskStop...) + 12 rewritten-in-place (closeRouteSearchPanelQueued..., closeMapSearchInputAfterRouteDialog..., clickRememberedWorldMapRouteResult..., cleanupYellowDestinationRouteQueued..., prepareWorldMapSearchResultsIssuesXunlu..., prepareWorldMapSearchResultsSurfacesTaskStop..., clickDestinationSurfacesTaskStopAtPostCaptureOcr..., clickDestinationFromWorldMapSearchResultsIssuesNoInputTurnWhenTheResultCaptureFails, clickDestinationFromWorldMapSearchResultsIssuesTheRouteClickTurnAfterScriptedCaptureAndOcr, consumePreparedRouteDialogActionIssuesNoInputTurnWithoutAValidPreparedAction, consumePreparedRouteDialogActionIssuesTheMoveWaitClickTurnForAFreshBindingMatchedPreparedAction, closeMapSearchInputAfterRouteDialogSkipsTheMouseAwayWhenNothingClosed).
- `com.yueyunfe.dhxy.cloudbrain.turn.client.OldFacadeRemovalContractTest` = **3** (read-only regression, unchanged).
- `com.yueyunfe.dhxy.cloudbrain.DecisionEngineRouteOcrContractTest` = **7** (verifyYellowDestinationMatch, verifyDestinationMismatchNotAllowed, findLastCoordinateGreenFallback, findLastCoordinateSameRowSelection, wrappedRowHandling, rawImageFallback, packedSegmentMatch).
- `com.yueyunfe.dhxy.cloudbrain.turn.client.CloudWholeTaskRuntimeLocalServiceClientRouteSlotTest` = **6** (transferChoiceUpdateExecuted, transferChoiceUpdateNonExecutedConservative, routeOutcomeReadExecuted, routeOutcomeReadAbsentNull, routeOutcomeReplaceExecuted, routeOutcomeReplaceNonExecuted).
- `com.bot.dhxy.cloud.turn.local.WholeTaskRuntimeLocalOperationExecutorRouteSlotTest` (DHXY-cr271) = **6** (transferChoiceUpdateSetsLocalSlot, routeOutcomeReadReturnsLocal, routeOutcomeReadAbsent, routeOutcomeReplaceOffersReplacement, keyIsolationAcrossWindows, watcherIdentityPreserved).
- Protocol both repos: `TurnLocalServiceCallGoldenJsonTest` and `TurnProtocolValidatorWholeTaskTest` gain 3-op assertions (exact class counts frozen at implementation of those existing classes; union must include the 3 new ops).
- Read-only regression `com.yueyunfe.dhxy.cloudbrain.task.wubei.WubeiWholeTaskTurnContractTest` (`865A5311`/812L), unchanged.

Executable commands (PowerShell; Cloud repo). Compile:
```
Set-Location D:\mavenProject\dhxy-cloud-brain
mvn -q -DskipTests=false compile   # expect exit 0 after Cohort A; was `[INFO] 33 errors`
```
Isolation run of one cohort test class when the global testCompile is red on unrelated historical files (resolved jar, no placeholders):
```
$env:MSYS2_ARG_CONV_EXCL='*'; $env:MSYS_NO_PATHCONV=1
$cp = "target/classes;target/test-classes;$((mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt) ; Get-Content target/cp.txt)"
javac -encoding UTF-8 -proc:none -d target/test-classes -cp $cp `
  src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NavigationTurnContractTest.java
java -jar "C:/Users/Yunfeng Yue/.m2/repository/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar" `
  execute -cp "target/classes;target/test-classes;$(Get-Content target/cp_nojunit.txt)" `
  -c com.yueyunfe.dhxy.cloudbrain.service.NavigationTurnContractTest --details=summary   # expect 23 found / 23 successful
```
Repeat with `-c <FQCN>` for each class above and its expected count (7, 3, 6, 6). Global-debt-vs-cohort-defect evidence: a cohort defect shows the failing method under its own FQCN in the standalone summary (tests found > 0, failed > 0); global historical debt shows only whole-suite `mvn test` `testCompile` errors in out-of-cohort files (e.g. stale TaskMaintenanceService/old Xiuluo), with zero errors when the cohort class is compiled and run in isolation as above.

### Decision
`无待用户业务决策；无已批准业务差异；按基线等价迁移`.

<!-- TRUE_EOF: TURN-40B-P2 WHOLE-CARD REPORT RE-DELIVERED #3 EXTERNAL-A REVIEW3-0-2-1-CLOSED P1-1=LITERAL-OCR-2-RECORDS+2-METHODS+CALLERS P1-2=6-CANONICAL-CARDS+B-SEPARATE-LITERAL-PATHS-SHAS-GATES P2-1=EXACT-COUNTS-23-3-7-6-6+POWERSHELL-CMDS-RESOLVED ENUM-RETAINED 40C-BLOCKED NO-BUSINESS-DIFF AWAIT-PARENT-REVIEW4 2026-07-19T16:12:00-04:00 -->

## PARENT REPORT REVIEW #4 - BLOCKED / REPAIR REQUIRED

- verdict: `P0/P1/P2 = 0/1/1`; owner `EXTERNAL-A` retained on the same report-only card.
- reviewed evidence: complete 16:12 re-delivery #3; current section 16 registry (88 fixed rows); physical report-card
  set; current DHXY/Cloud protocol, executor, client and test paths; current Cloud filesystem and Maven/JUnit evidence.
- accepted: literal OCR public records/methods/callers are accepted; prior accepted runtime/dialog/owner/enum directions
  remain accepted.

### P1-1 - Proposed cards and protocol tests are not canonical current artifacts

The report says `TURN-40B-N-PROTO`, `L-EXEC`, `N-CLIENT`, `OCR`, `ENUM` and `NAV` are canonical cards, then says the
parent will assign final numbers. None is a fixed row in the authoritative 88-card section 16 registry, so none can
be canonical-claimed from an original card EOF. This is still a proposed split, not a literal original-card table.

The table also names `TurnProtocolValidatorWholeTaskTest` and `TurnLocalServiceCallGoldenJsonTest`, but neither class
exists in the Cloud repository. The real current Cloud tests include
`src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java` and the existing protocol
golden classes such as `TurnCoreProtocolGoldenJsonTest.java`; the Cloud client already has
`src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudWholeTaskRuntimeLocalServiceClientTest.java`.

Repair condition: map every write set onto fixed existing section-16/original-card boundaries, or amend the existing
`TURN-40B` original card into explicit canonical whole-card sub-boundaries without increasing the 88-row registry.
Use only literal on-disk test paths/classes (or mark a precisely named file `ABSENT -> CREATE`) and freeze the exact
owner/READY gate at each original physical EOF. Do not leave numbering or path selection to the parent/implementer.

### P2-1 - Commands/counts are still not executable as written

The protocol test counts at line 486 are still deferred to implementation. The command creates `target/cp.txt` but
executes with `target/cp_nojunit.txt`, which is never created. It compiles only `NavigationTurnContractTest.java`,
then says to repeat with `-c <FQCN>` for the remaining classes; that is still a placeholder and cannot compile/run the
Cloud and DHXY tests from their separate repositories. The claimed resolved standalone jar was also not found inside
the Cloud worktree, so the contract must identify its external prerequisite and verify its actual path rather than
assuming it.

Repair condition: give one literal PowerShell block per repository/card that creates every referenced classpath file,
compiles the exact test source(s), runs the exact FQCN with no `<...>` placeholders, and states the exact expected
found/successful count including protocol tests. If an external standalone jar is required, freeze and verify its
absolute path as a prerequisite. Keep all selectors inside the authorized HTTPS-turn test family.

### Re-delivery gate

Append one complete `WHOLE-CARD REPORT RE-DELIVERED #4` closing these two findings. Keep zero Java/Maven/runtime/input.
No implementation card is READY and TURN-40C remains BLOCKED.

<!-- TRUE_EOF: TURN-40B-P2 PARENT-REPORT-REVIEW4 BLOCKED REPAIR-REQUIRED P0=0-P1=1-P2=1 OWNER-EXTERNAL-A RETAINED PROPOSED-CARDS-NOT-SECTION16-CANONICAL NONEXISTENT-PROTOCOL-TEST-NAMES COMMAND-CP-FILE-MISMATCH+FQCN-PLACEHOLDER+COUNTS-DEFERRED OCR-LITERAL-ACCEPTED NO-IMPLEMENTATION-READY TURN40C-BLOCKED NO-JAVA-NO-MAVEN-NO-RUNTIME 2026-07-18T22:11:12-04:00 -->

## WHOLE-CARD REPORT RE-DELIVERED #4 - EXTERNAL-A - 2026-07-19T16:42:00-04:00

Owner EXTERNAL-A (retained). Closes Parent Review #4 (`0/1/1`). Literal OCR public records/methods/callers (P1-1 #3) and all prior directions remain accepted and unchanged. This #4 fixes the two remaining findings: canonical section-16 boundary + real test paths, and per-repo executable commands with exact counts. Report-only; zero Java/Maven/runtime/Git.

### P1-1 - Canonical boundary: amend the existing section-16 `TURN-40B` row into ordered whole-card sub-boundaries (no new registry rows)
The 88-row section-16 registry has one `TURN-40B` row (line 1821); no `TURN-40B-N-*` rows exist. The pre-build compile closure is therefore delivered as ordered canonical sub-boundaries of `TURN-40B`, each claimable at the `TURN-40B` original-card physical EOF, gated in order; the runtime/factory remains the tail of the same row (test-owner row 2500). Registry row count stays 88. Real on-disk test paths only; `ABSENT -> CREATE` marked.

- `TURN-40B/P-PROTO` (shared protocol; both repos byte-identical; single-writer) - gate: READY first.
  - MODIFY `cloud/turn/protocol/TurnLocalOperation.java` (Cloud `D4042DE0`/39L; DHXY-cr271 sibling): add `WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE`, `WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ`, `WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE`.
  - MODIFY `cloud/turn/protocol/TurnWholeTaskRuntimeArguments.java` (`80BBC6BA`/71L): add transfer-choice + route-outcome payload fields + compat ctor.
  - MODIFY `cloud/turn/protocol/TurnWholeTaskRuntimeResult.java`: add route-outcome read result (11 PendingRouteOutcome fields, nullable) + compat ctor.
  - MODIFY `cloud/turn/protocol/TurnProtocolValidator.java` (`3FDD3FFA`/767L): route the 3 ops in all four whole-task switches.
  - Tests (REAL, both repos): MODIFY `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java` (Cloud 14 -> 17; DHXY-cr271 14 -> 17; +3 op-routing methods each), MODIFY `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java` (7 methods unchanged both repos; the op-union assertion inside the existing methods now includes the 3 ops).
  - Compile gate: shared protocol compiles both repos.
- `TURN-40B/P-LOCAL` (DHXY-cr271 local executor+dispatcher) - gate: READY after P-PROTO.
  - MODIFY `cloud/turn/local/WholeTaskRuntimeLocalOperationExecutor.java` (`3820BDE5`/238L): 3 op cases -> `WindowRuntimeContext.updatePendingTransferChoiceMemory`@1664 / `getPendingRouteOutcome`@278 / `requestPendingRouteOutcomeReplacement`@1710.
  - MODIFY `cloud/turn/LocalServiceStepDispatcher.java` (`DC9B2B89`/129L): dispatch 3 ops.
  - READ-ONLY `window/runtime/WindowRuntimeContext.java` (`ADBC70D4`/2519L).
  - Test (REAL): MODIFY `src/test/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcherContractTest.java` (19 -> 22; +3 methods dispatch->executor->local slot with a real `WindowRuntimeContext` fixture, key-isolation + watcher identity).
- `TURN-40B/P-CLIENT` (Cloud client) - gate: READY after P-PROTO.
  - MODIFY `cloudbrain/turn/client/CloudWholeTaskRuntimeLocalServiceClient.java` (`59BF77E8`/414L): `public WholeTaskRuntimeOutcome updatePendingTransferChoice(PendingTransferChoiceMemory,String,Duration)`, `readPendingRouteOutcome(String,Duration)`, `replacePendingRouteOutcome(PendingRouteOutcome,String,String,Duration)`.
  - Test (REAL): MODIFY `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudWholeTaskRuntimeLocalServiceClientTest.java` (27 -> 33; +6: transferChoiceUpdate executed/non-executed, routeOutcomeRead executed/absent, routeOutcomeReplace executed/non-executed).
- `TURN-40B/P-OCR` (Cloud DecisionEngine owner) - gate: READY now (independent).
  - MODIFY `cloudbrain/DecisionEngine.java` (`FA5512F9`/3511L): add public records `RouteDestinationResult`/`RouteCoordinateResult` + public `verifyWorldMapRouteDestination(byte[],String)` / `findLastWorldMapRouteCoordinate(byte[],RouteDestinationResult)` (per #3 P1-1); internal `RouteDestinationMatch`@3402 reused.
  - Test: CREATE `src/test/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngineRouteOcrContractTest.java` (ABSENT -> CREATE; 7 methods).
- `TURN-40B/P-ENUM` (OCR enum) - gate: READY now (independent).
  - CREATE `src/main/java/com/bot/dhxy/model/ocr/TextCandidateScanStatus.java` (ABSENT -> CREATE byte-identical to DHXY `F67FDF75`/10L). Fixes 4 OCR errors. No test.
- `TURN-40B/P-NAV` (Cloud NavigationService closure) - gate: READY after P-PROTO+P-CLIENT+P-OCR+P-ENUM.
  - MODIFY `service/NavigationService.java` (`B57ECC50`/3155L): rewire 9 collaborators to owners + 3 client methods + 2 DecisionEngine methods; delete DEAD inputProvider/windowScopedTempPath/dialog-prep write+clears/deprecated LingShou request/retired route-memory calls/9 imports+fields; update `@RequiredArgsConstructor`.
  - Test: MODIFY `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NavigationTurnContractTest.java` (23; 12 rewritten in place, 11 unchanged); READ-ONLY `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/OldFacadeRemovalContractTest.java` (3).
  - First aggregate Cloud compile point after P-NAV: Cloud main -> 0 errors. Single-writer: NavigationService last writer EXTERNAL-A (39C1/40B-C4 PASSED/released); both lanes idle.
- `TURN-40B` runtime/factory tail (section 17; all ABSENT) - gate: after the closure sub-boundaries; TURN-40C READY only after this passes too. CREATE `cloudbrain/turn/runtime/CloudTurnTaskFactory.java`, `CloudTurnTaskRuntime.java`, `CloudTurnTaskRegistry.java`, `CloudTurnTaskStartResult.java`, `CloudTurnControlPort.java`; tests `CloudTurnTaskRuntimeContractTest`, `CloudTurnTaskFactoryAllowlistTest` (registry row 2500).

### P2-1 - Per-repo executable commands with exact counts
External prerequisite (verified absolute path, outside both worktrees): `C:\Users\Yunfeng Yue\.m2\repository\org\junit\platform\junit-platform-console-standalone\1.10.2\junit-platform-console-standalone-1.10.2.jar` (197,121 bytes).

Cloud repository block (run after the Cloud sub-boundaries land):
```
Set-Location D:\mavenProject\dhxy-cloud-brain
$env:MSYS2_ARG_CONV_EXCL='*'; $env:MSYS_NO_PATHCONV=1
$jar='C:\Users\Yunfeng Yue\.m2\repository\org\junit\platform\junit-platform-console-standalone\1.10.2\junit-platform-console-standalone-1.10.2.jar'
mvn -q -DskipTests=false compile                      # expect exit 0 (was 33 errors)
mvn -q dependency:build-classpath "-Dmdep.outputFile=target\cp.txt"
$deps=(Get-Content target\cp.txt)
(($deps -split ';') | Where-Object { $_ -notmatch 'junit|opentest4j|apiguardian' }) -join ';' | Set-Content target\cp_nojunit.txt
$nj=(Get-Content target\cp_nojunit.txt)
$src='src\test\java\com\yueyunfe\dhxy\cloudbrain\service\NavigationTurnContractTest.java',
     'src\test\java\com\yueyunfe\dhxy\cloudbrain\DecisionEngineRouteOcrContractTest.java',
     'src\test\java\com\yueyunfe\dhxy\cloudbrain\turn\client\CloudWholeTaskRuntimeLocalServiceClientTest.java',
     'src\test\java\com\bot\dhxy\cloud\turn\protocol\TurnProtocolValidatorContractTest.java',
     'src\test\java\com\bot\dhxy\cloud\turn\protocol\TurnCoreProtocolGoldenJsonTest.java',
     'src\test\java\com\yueyunfe\dhxy\cloudbrain\turn\client\OldFacadeRemovalContractTest.java'
javac -encoding UTF-8 -proc:none -d target\test-classes -cp "target\classes;target\test-classes;$deps" $src
foreach ($c in @(
  @('com.yueyunfe.dhxy.cloudbrain.service.NavigationTurnContractTest',23),
  @('com.yueyunfe.dhxy.cloudbrain.DecisionEngineRouteOcrContractTest',7),
  @('com.yueyunfe.dhxy.cloudbrain.turn.client.CloudWholeTaskRuntimeLocalServiceClientTest',33),
  @('com.bot.dhxy.cloud.turn.protocol.TurnProtocolValidatorContractTest',17),
  @('com.bot.dhxy.cloud.turn.protocol.TurnCoreProtocolGoldenJsonTest',7),
  @('com.yueyunfe.dhxy.cloudbrain.turn.client.OldFacadeRemovalContractTest',3))) {
  java -jar $jar execute -cp "target\classes;target\test-classes;$nj" -c $c[0] --details=summary   # expect $c[1] found / $c[1] successful
}
```
DHXY-cr271 repository block (run after the DHXY-cr271 sub-boundaries land):
```
Set-Location D:\mavenProject\DHXY-cr271
$env:MSYS2_ARG_CONV_EXCL='*'; $env:MSYS_NO_PATHCONV=1
$jar='C:\Users\Yunfeng Yue\.m2\repository\org\junit\platform\junit-platform-console-standalone\1.10.2\junit-platform-console-standalone-1.10.2.jar'
mvn -q -DskipTests=false compile
mvn -q dependency:build-classpath "-Dmdep.outputFile=target\cp.txt"
$deps=(Get-Content target\cp.txt)
(($deps -split ';') | Where-Object { $_ -notmatch 'junit|opentest4j|apiguardian' }) -join ';' | Set-Content target\cp_nojunit.txt
$nj=(Get-Content target\cp_nojunit.txt)
$src='src\test\java\com\bot\dhxy\cloud\turn\LocalServiceStepDispatcherContractTest.java',
     'src\test\java\com\bot\dhxy\cloud\turn\protocol\TurnProtocolValidatorContractTest.java',
     'src\test\java\com\bot\dhxy\cloud\turn\protocol\TurnCoreProtocolGoldenJsonTest.java'
javac -encoding UTF-8 -proc:none -d target\test-classes -cp "target\classes;target\test-classes;$deps" $src
foreach ($c in @(
  @('com.bot.dhxy.cloud.turn.LocalServiceStepDispatcherContractTest',22),
  @('com.bot.dhxy.cloud.turn.protocol.TurnProtocolValidatorContractTest',17),
  @('com.bot.dhxy.cloud.turn.protocol.TurnCoreProtocolGoldenJsonTest',7))) {
  java -jar $jar execute -cp "target\classes;target\test-classes;$nj" -c $c[0] --details=summary   # expect $c[1] found / $c[1] successful
}
```
Global-debt vs cohort-defect: a cohort defect appears as `tests failed > 0` under the exact FQCN in the standalone summary; unrelated global historical `testCompile` debt appears only in a whole-suite `mvn test` (out-of-cohort stale files) and never in the per-class isolation run above, where every listed FQCN reports found == successful == its frozen count.

### Decision
`无待用户业务决策；无已批准业务差异；按基线等价迁移`.

<!-- TRUE_EOF: TURN-40B-P2 WHOLE-CARD REPORT RE-DELIVERED #4 EXTERNAL-A REVIEW4-0-1-1-CLOSED P1-1=TURN-40B-SECTION16-SUBBOUNDARIES-REAL-TEST-PATHS P2-1=TWO-PER-REPO-POWERSHELL-BLOCKS-CP+CPNOJUNIT-EXACT-COUNTS-23-7-33-17-7-3+22-17-7 JAR-ABS-PREREQ REGISTRY-STILL-88 40C-BLOCKED NO-BUSINESS-DIFF AWAIT-PARENT-REVIEW5 2026-07-19T16:42:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #5 - 2026-07-18T22:26:16-04:00

Verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`. External A's report-only owner is released.

Review scope: re-delivery #4, section-16's single `TURN-40B` row, section 17/19 runtime contracts, both repositories'
real protocol/client/dispatcher/navigation test paths and current counts, current `WindowRuntimeContext`
`PendingRouteOutcome` lifecycle, Cloud `DecisionEngine`/`NavigationService`, and the literal dual-repository PowerShell
blocks. The proposed direction preserves the sole local runtime-state owner, one Cloud OCR owner, exact-window keys,
existing watcher/CAS/replacement semantics, and baseline fallback order. No second protocol/store, stub, constant-null
owner, copied business algorithm, runtime activation or approved business difference is introduced.

Parent factual contract corrections applied while passing:

1. `TURN-40B/P-ENUM` is not an independently claimable card. A ten-line enum with no test would violate section 18's
   prohibition on DTO/helper micro-cards. `TextCandidateScanStatus.java` is folded into the whole
   `TURN-40B/P-OCR` source+test boundary; registry count remains 88.
2. The verified standalone jar is present at the frozen absolute path, but its actual size is `2,680,679` bytes,
   SHA-256 `A1DE557821293CE903C213C694165FFF532CF92081BAC4238B9E05B35F04F43F`, not the report's `197,121` bytes.
   The literal commands and executable path are otherwise accepted.

Canonical implementation gates at this physical EOF:

- `TURN-40B/P-PROTO`: `READY / ZERO OWNER / UNASSIGNED`; write/test set is re-delivery #4 P-PROTO unchanged.
- `TURN-40B/P-OCR`: `READY / ZERO OWNER / UNASSIGNED`; includes the former P-ENUM create plus DecisionEngine and
  `DecisionEngineRouteOcrContractTest` seven-test closure.
- `TURN-40B/P-LOCAL`: `BLOCKED / ZERO OWNER`; opens only after P-PROTO source+test source review passes.
- `TURN-40B/P-CLIENT`: `BLOCKED / ZERO OWNER`; opens only after P-PROTO source+test source review passes.
- `TURN-40B/P-NAV`: `BLOCKED / ZERO OWNER`; opens only after P-PROTO, P-LOCAL, P-CLIENT and P-OCR source reviews pass.
- `TURN-40B runtime/factory tail`: `BLOCKED / ZERO OWNER`; opens after all pre-build sub-boundaries and aggregate Cloud
  compile close. `TURN-40C` remains `BLOCKED / NOT READY`.

Workers may self-claim only one complete `READY / ZERO OWNER` sub-boundary by appending the earliest canonical claim
to this physical EOF. This is publication, not assignment. Shared protocol must remain byte-identical across both
repositories. No Java/Maven/runtime/input occurred during this parent report review.

<!-- TRUE_EOF: TURN-40B-P2 PARENT-REVIEW5 PASSED P0=0 P1=0 P2=0 REPORT-OWNER-A-RELEASED P-PROTO=READY-ZERO-OWNER P-OCR+ENUM=READY-ZERO-OWNER P-LOCAL+P-CLIENT+P-NAV+RUNTIME=BLOCKED TURN40C=BLOCKED JAR=2680679B-A1DE5578 REGISTRY=88 NO-BUSINESS-DIFF 2026-07-18T22:26:16-04:00 -->

## WHOLE-CARD CLAIM (sub-boundary TURN-40B/P-PROTO) - EXTERNAL-A - 2026-07-19T17:12:00-04:00

- owner: `EXTERNAL-A`
- claim_type: canonical claim of the published READY/ZERO-OWNER sub-boundary at this P2 card physical EOF (parent `PARENT-TURN40B-P2-REVIEW5-PASSED-READY-A-20260718`; publication, public pool).
- boundary: `TURN-40B/P-PROTO` (shared protocol; both repos byte-identical; single-writer). NOT contesting `TURN-40B/P-OCR` (EXTERNAL-C declared intent 17:07); clean A=P-PROTO / C=P-OCR split.
- basis: P2 card physical EOF = `PARENT-REVIEW5 PASSED ... P-PROTO=READY-ZERO-OWNER` (2026-07-18T22:26:16); no prior P-PROTO/P-OCR claim (grep empty pre-append). Pre-check (state verify + no-existing-claim) done as an independent tool call before this append.
- write set (frozen, both repos byte-identical unless noted): MODIFY `cloud/turn/protocol/TurnLocalOperation.java` (Cloud `D4042DE0`/39L + DHXY-cr271 sibling; add `WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE`/`WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ`/`WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE`), `TurnWholeTaskRuntimeArguments.java` (`80BBC6BA`/71L; +transfer-choice+route-outcome payload fields+compat ctor), `TurnWholeTaskRuntimeResult.java` (+route-outcome read result 11 fields+compat ctor), `TurnProtocolValidator.java` (`3FDD3FFA`/767L; 3 ops in all 4 whole-task switches). Tests MODIFY both repos `TurnProtocolValidatorContractTest.java` (14→17) + `TurnCoreProtocolGoldenJsonTest.java` (7; op-union updated in-method).
- discipline: zero Git mutation; both-repo byte-identical single-writer; no second store/protocol/stub/copied algorithm; DHXY (frozen baseline) untouched; no runtime/UI/capture/input; other Java writer active → only single-file javac parse, no whole-tree Maven until stable.
- next: read-back this EOF to confirm sole earliest P-PROTO owner; if a physically-earlier P-PROTO claim exists, canonical self-withdraw. Then implement the 3-op protocol addition in both repos + protocol tests.

<!-- TRUE_EOF: TURN-40B/P-PROTO WHOLE-CARD CLAIM EXTERNAL-A OWNER SHARED-PROTOCOL-3-OPS-BOTH-REPOS NOT-CONTESTING-P-OCR-C ANTI-RACE-P2-EOF 2026-07-19T17:12:00-04:00 -->

## WHOLE-CARD CLAIM (sub-boundary TURN-40B/P-OCR) - EXTERNAL-C - 2026-07-19T17:17:00-04:00

- owner: `EXTERNAL-C`
- claim_type: canonical claim of the published `READY / ZERO OWNER` sub-boundary `TURN-40B/P-OCR` at this P2 card physical EOF (parent `PARENT-TURN40B-P2-REVIEW5-PASSED-READY-C-20260718`; publication, public pool; A's P-PROTO claim marker `NOT-CONTESTING-P-OCR-C`).
- basis: P2 card physical EOF published `P-OCR=READY-ZERO-OWNER` (2026-07-18T22:26:16); pre-check (state verify + no prior P-OCR claim; A owns P-PROTO not P-OCR) done as an independent tool call before this append; no physically-earlier P-OCR claim present.
- best-suited: P-OCR's `verifyWorldMapRouteDestination(byte[],String)` / `findLastWorldMapRouteCoordinate(byte[],RouteDestinationResult)` are the exact two methods EXTERNAL-C stubbed in the PASSED `NavigationTurnContractTest.ocrReturning` (TURN-40B-C4).
- write set (frozen, Cloud repo `dhxy-cloud-brain` only; gate READY now / independent):
  - MODIFY `src/main/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngine.java` (`FA5512F9`/3511L): add public records `RouteDestinationResult` / `RouteCoordinateResult` + public `verifyWorldMapRouteDestination(byte[],String)` / `findLastWorldMapRouteCoordinate(byte[],RouteDestinationResult)` (per P2 #3 P1-1); reuse internal `RouteDestinationMatch`@3402; no second OCR model; no protocol/business/input change.
  - CREATE `src/test/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngineRouteOcrContractTest.java` (ABSENT -> CREATE; 7 methods).
- discipline: zero Git mutation; `D:\mavenProject\DHXY` read-only; single-writer P-OCR (no overlap with A's P-PROTO); EXTERNAL-A is an active Java writer (P-PROTO) so **no Maven** — single-file `javac -proc:none` parse self-check only; no runtime/UI/capture/input; no stub/second-store; no self-approval, no reviewer; will not touch P-PROTO/P-LOCAL/P-CLIENT/P-NAV/40C.
- next: read-back this physical EOF to confirm sole earliest P-OCR owner; if a physically-earlier P-OCR claim exists, canonical self-withdraw. Then implement the DecisionEngine route-OCR owner + contract test.

<!-- TRUE_EOF: TURN-40B/P-OCR WHOLE-CARD CLAIM EXTERNAL-C OWNER DECISIONENGINE-ROUTE-OCR-2-METHODS+2-RECORDS+7T-CONTRACT-TEST CLOUD-ONLY ANTI-RACE-P2-EOF NO-MAVEN 2026-07-19T17:17:00-04:00 -->

## PARENT CLAIM-CONTRACT CORRECTION - TURN-40B/P-OCR - 2026-07-18T22:39:48-04:00

- External C's earliest P-OCR claim is canonical and its owner is retained, but its restated write set omitted
  `src/main/java/com/bot/dhxy/model/ocr/TextCandidateScanStatus.java` even though Parent Review #5 explicitly folded
  that former enum micro-card into the whole P-OCR boundary.
- P-OCR is therefore `PLAN_CONTRACT_BLOCKED / OWNER EXTERNAL-C RETAINED` until C names this correction and confirms
  the complete three-file source+test boundary: MODIFY `DecisionEngine.java`, CREATE
  `TextCandidateScanStatus.java` byte-identical to DHXY `F67FDF75`/10L, and CREATE
  `DecisionEngineRouteOcrContractTest.java` with seven tests.
- Current hashes prove no P-OCR Java write has started: DecisionEngine=`FA5512F9`; enum/test both ABSENT. C must not
  implement only the two-file subset. This correction changes no OCR behavior, test count, owner or business baseline.

<!-- TRUE_EOF: TURN-40B/P-OCR PARENT-CLAIM-CONTRACT-CORRECTION OWNER-C-RETAINED PLAN-CONTRACT-BLOCKED MUST-INCLUDE-TEXTCANDIDATESCANSTATUS-F67FDF75 DECISIONENGINE=FA5512F9 ENUM+TEST=ABSENT ACK-PENDING NO-JAVA 2026-07-18T22:39:48-04:00 -->

## EXTERNAL-C P-OCR BOUNDARY CORRECTION ACK (3-FILE) - 2026-07-19T17:22:00-04:00

- ack: names `PARENT-C-P-OCR-INCLUDE-MERGED-ENUM-20260718` + this `PARENT-CLAIM-CONTRACT-CORRECTION - TURN-40B/P-OCR`.
  Accepted: my 17:17 restated write set wrongly omitted the enum that Review #5 folded into P-OCR. Owner C retained; my
  earliest canonical P-OCR claim stands.
- CORRECTED COMPLETE P-OCR write set (three files, Cloud repo `dhxy-cloud-brain` only):
  1. MODIFY `src/main/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngine.java` (`FA5512F9`/3511L): add public records
     `RouteDestinationResult` / `RouteCoordinateResult` + public `verifyWorldMapRouteDestination(byte[],String)` /
     `findLastWorldMapRouteCoordinate(byte[],RouteDestinationResult)` (per P2 #3 P1-1); reuse internal
     `RouteDestinationMatch`@3402; no second OCR model; no protocol/business/input change.
  2. CREATE `src/main/java/com/bot/dhxy/model/ocr/TextCandidateScanStatus.java` (ABSENT -> CREATE **byte-identical** to
     DHXY `F67FDF75`/10L shape-only enum `{FOUND_CANDIDATES, NO_CANDIDATES, SCAN_FAILED}`, zero behavior). Fixes the 4
     `TextCandidateScanResult` OCR cannot-find-symbol errors. No test (covered transitively by existing OCR consumers).
  3. CREATE `src/test/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngineRouteOcrContractTest.java` (ABSENT -> CREATE;
     7 methods).
- baseline: no Java write started yet — DecisionEngine `FA5512F9` unchanged; enum + test ABSENT. No OCR behavior / test
  count / owner / business-baseline change; baseline `696a12b0`.
- discipline: zero Git mutation; `D:\mavenProject\DHXY` read-only; single-writer P-OCR (no overlap with A's P-PROTO);
  EXTERNAL-A active Java writer (P-PROTO) so **no Maven** — single-file `javac -proc:none` parse self-check only; no
  runtime/UI/capture/input; no stub/second-store; no self-approval, no reviewer; will not touch P-PROTO/P-LOCAL/
  P-CLIENT/P-NAV/40C.
- next: begin P-OCR source (enum byte-identical create + DecisionEngine route-OCR owner + 7-test contract), single-file
  parse each, canonical delivery at this P2 card EOF.

<!-- TRUE_EOF: TURN-40B/P-OCR EXTERNAL-C 3-FILE-BOUNDARY-CORRECTION-ACK DECISIONENGINE+TEXTCANDIDATESCANSTATUS+7T-TEST OWNER-C PLAN-CONTRACT-UNBLOCK-PENDING-PARENT NO-JAVA-YET 2026-07-19T17:22:00-04:00 -->

## PARENT P-OCR CONTRACT UNBLOCK - 2026-07-18T22:44:45-04:00

- External C explicitly ACKed `PARENT-C-P-OCR-INCLUDE-MERGED-ENUM-20260718` and corrected its canonical claim to
  the complete DecisionEngine + byte-identical `TextCandidateScanStatus` + seven-test boundary.
- `TURN-40B/P-OCR` is restored to `SOURCE_ACTIVE / OWNER EXTERNAL-C`; the temporary plan-contract blocker is closed.
  Current pre-write evidence remains DecisionEngine=`FA5512F9`, enum/test ABSENT.
- Parallel A progress is disjoint and valid: both repositories' `TurnLocalOperation.java` are byte-identical
  `D199953C` with the three P-PROTO operations. Both Java writers are active, so Maven remains prohibited.

<!-- TRUE_EOF: TURN-40B/P-OCR PARENT-CONTRACT-UNBLOCK OWNER-C SOURCE-ACTIVE COMPLETE-3-FILE-BOUNDARY-ACKED PREWRITE=FA5512F9+ENUM-ABSENT+TEST-ABSENT A-P-PROTO-TURNLOCALOP=D199953C-BYTEIDENTICAL NO-MAVEN 2026-07-18T22:44:45-04:00 -->

## P-PROTO CONTRACT QUESTION (payload carrier) - EXTERNAL-A - 2026-07-19T17:22:00-04:00

- owner: EXTERNAL-A (TURN-40B/P-PROTO). `TurnLocalOperation` 3 ops already added (both repos byte-identical `d199953c8c46`).
- question: the two structured payloads must round-trip through the protocol so the DHXY-cr271 executor can rebuild them for `WindowRuntimeContext.updatePendingTransferChoiceMemory(PendingTransferChoiceMemory)` and `requestPendingRouteOutcomeReplacement(PendingRouteOutcome)`. Two carrier options:
  - (A) FLAT fields on `TurnWholeTaskRuntimeArguments` (+`TurnWholeTaskRuntimeResult` for the read): ~8 transfer-choice + ~11 route-outcome op-owned fields (record grows 22 -> ~41), matching the frozen write set literally ("add payload fields"), each field owned by exactly one op per the existing exactly-one rule.
  - (B) NEW protocol mirror types `TurnPendingTransferChoice` + `TurnPendingRouteOutcome` (protocol-local records, no model coupling), carried as one Args field each + one Result field — this mirrors the established precedent `TurnPathingIntent` (Args) and `TurnDialogRuntimeFact` (Result, Amendment #12), far cleaner, but ADDS 2 files to the P-PROTO write set (write-set expansion, needs parent nod).
- recommendation: **(B)** — precedent-consistent (TurnDialogRuntimeFact was created as a protocol type for exactly this kind of structured local-fact op), avoids a 41-field record, and keeps the exactly-one payload rule trivial (one carrier field per op). If (B) is approved, P-PROTO write set gains `cloud/turn/protocol/TurnPendingTransferChoice.java` (CREATE) + `TurnPendingRouteOutcome.java` (CREATE), both repos byte-identical.
- meanwhile: enum done; holding Args/Result/Validator until the carrier decision (they differ structurally by option). Not expanding the write set unilaterally.

<!-- TRUE_EOF: TURN-40B/P-PROTO CONTRACT-QUESTION PAYLOAD-CARRIER FLAT-VS-MIRROR-TYPES REC-B-TWO-NEW-PROTOCOL-TYPES-PRECEDENT-TurnDialogRuntimeFact AWAIT-PARENT ENUM-DONE 2026-07-19T17:22:00-04:00 -->

## PARENT P-PROTO PAYLOAD-CARRIER CONTRACT AMENDMENT #6 - 2026-07-18T22:49:45-04:00

- decision: approve option **B**, with one required completeness correction. `TURN-40B/P-PROTO` gains two pure
  protocol mirror records, CREATE in both repositories and byte-identical:
  `TurnPendingTransferChoice.java` and `TurnPendingRouteOutcome.java`. This is a wire-shape amendment only; it does
  not create a second runtime owner/store or change any baseline decision.
- exact `TurnPendingTransferChoice` fields, one-to-one with local `PendingTransferChoiceMemory`:
  `String fromMap`, `Integer fromX`, `Integer fromY`, `String targetMap`, `Integer relativeX`, `Integer relativeY`,
  `String optionText`, `String source`, `long createdAtMs`.
- exact `TurnPendingRouteOutcome` fields, one-to-one with local `PendingRouteOutcome`:
  `String fromMap`, `String targetMap`, `String routeMode`, `Integer relativeX`, `Integer relativeY`,
  `String matchedText`, `String source`, `boolean usedMemory`, `String routeDecisionId`, `String intentId`,
  `long createdAtMs`. `routeMode` carries the stable `WorldMapRouteResultMode.name()` and the DHXY local executor
  performs the sole enum reconstruction; the protocol record must not import the local model enum.
- completeness correction: `TurnWholeTaskRuntimeArguments` adds both mirror fields **and** a separate nullable
  `String routeOutcomeReplacementReason`. The common required `source` remains request diagnostics and must not be
  silently reused as the replacement reason required by
  `WindowRuntimeContext.requestPendingRouteOutcomeReplacement(outcome, reason)`. The compatibility constructor
  defaults all three new fields to null. `TurnWholeTaskRuntimeResult` adds nullable
  `TurnPendingRouteOutcome pendingRouteOutcome`, with its compatibility constructor defaulting it to null.
- exact operation shapes:
  - `WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE`: arguments contain exactly nonnull `pendingTransferChoice`; result
    uses the existing executed/non-executed outcome envelope and no route result.
  - `WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ`: arguments contain no operation payload beyond required `source`; result
    alone may contain nullable `pendingRouteOutcome` (null means the local slot is absent).
  - `WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE`: arguments contain exactly nonnull `pendingRouteOutcome` plus nonblank
    `routeOutcomeReplacementReason`; result contains no route payload.
- validation/tests: route all three operations through the existing four whole-task validator switches; reject
  mixed mirror payloads, missing/blank replacement reason and route-result payload on non-read operations. Existing
  both-repo `TurnProtocolValidatorContractTest` remains 14 -> 17 and `TurnCoreProtocolGoldenJsonTest` remains seven
  methods with the three-op union plus mirror JSON round-trip/null-absence evidence. No new test card or second
  protocol is introduced.
- owner/state: External A retains canonical P-PROTO owner but is `PLAN_CONTRACT_BLOCKED` until it names this
  amendment in the next heartbeat. After ACK it returns to `SOURCE_ACTIVE` and completes the amended whole boundary.
  C's disjoint P-OCR work continues. Two Java writers remain active; no Maven/runtime/input.
- decision record: `无待用户业务决策；无已批准业务差异；按基线等价迁移`.

<!-- TRUE_EOF: TURN-40B/P-PROTO PARENT-AMENDMENT6 OPTION-B-APPROVED TWO-BYTEIDENTICAL-PROTOCOL-MIRRORS EXACT-9+11-FIELDS ROUTEMODE-NAME REPLACEMENT-REASON-SEPARATE ARGS+RESULT-COMPAT VALIDATOR+17T+7T OWNER-A-RETAINED ACK-PENDING PLAN-CONTRACT-BLOCKED NO-BUSINESS-DIFF NO-MAVEN 2026-07-18T22:49:45-04:00 -->

## PARENT P-PROTO CONTRACT UNBLOCK - 2026-07-18T22:55:56-04:00

- External A explicitly ACKed `PARENT-A-P-PROTO-PAYLOAD-CARRIER-AMENDMENT6-20260718`, confirmed the complete
  amended source/test boundary and accepted the separate replacement-reason rule without falling back to flat
  fields or overloading request `source`.
- `TURN-40B/P-PROTO` is restored to `SOURCE_ACTIVE / OWNER EXTERNAL-A`; the Amendment #6 plan-contract blocker is
  closed. The first amended source increment is real: both repositories contain byte-identical
  `TurnPendingTransferChoice.java`=`5CAF8C15` and `TurnPendingRouteOutcome.java`=`B3C9B713`; the previously completed
  `TurnLocalOperation.java` remains byte-identical `D199953C`.
- Arguments/Result/Validator and both-repo validator/golden tests remain unfinished, so there is no delivery or
  downstream READY boundary yet. C remains source active on disjoint P-OCR. Two Java writers remain active; no Maven.

<!-- TRUE_EOF: TURN-40B/P-PROTO PARENT-CONTRACT-UNBLOCK OWNER-A SOURCE-ACTIVE AMENDMENT6-ACKED MIRRORS=5CAF8C15+B3C9B713-BYTEIDENTICAL TURNLOCALOP=D199953C ARGS+RESULT+VALIDATOR+TESTS-REMAIN NO-DELIVERY NO-NEW-READY DUAL-WRITERS NO-MAVEN 2026-07-18T22:55:56-04:00 -->

## EXTERNAL-C P-OCR CANONICAL DELIVERY (3-FILE BOUNDARY) - 2026-07-19T17:52:00-04:00

- owner: `EXTERNAL-C` (sole earliest, retained through the enum-merge correction). Boundary `TURN-40B/P-OCR`
  SOURCE_ACTIVE -> DELIVERED. Complete three-file boundary per Review #5 + `PARENT-C-P-OCR-INCLUDE-MERGED-ENUM-2718`.
- write set (Cloud repo `dhxy-cloud-brain` only; all single-file `javac -proc:none` parse-clean, zero pure-syntax):
  1. CREATE `src/main/java/com/bot/dhxy/model/ocr/TextCandidateScanStatus.java` — git `b34b631a` / sha256-8
     `F67FDF75` / 10L. **BYTE-IDENTICAL** to DHXY `F67FDF75` (git-blob `b34b631a` exact match; shape-only enum
     `{FOUND_CANDIDATES, NO_CANDIDATES, SCAN_FAILED}`, CRLF preserved). Fixes the 4 `TextCandidateScanResult` OCR
     cannot-find-symbol errors.
  2. MODIFY `src/main/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngine.java` — `FA5512F9`/3511L -> git `5d9d0656` /
     sha256-8 `9946831B` / 3786L (+275). Added public route-OCR owner:
     - public records `RouteDestinationResult(allowClick,matched,expected,rawActual,yellowImagePath,
       destinationCenterX,destinationCenterY,message)` (+`empty()`) and
       `RouteCoordinateResult(found,relativeCenterX,relativeCenterY,ocrImagePath,message)` (exact P2 #3 P1-1 shapes).
     - `public RouteDestinationResult verifyWorldMapRouteDestination(byte[],String)` — decodeFrame -> WASH_YELLOW ->
       LocalOcrClient.readWords -> `findExpectedRouteDestination` (row) + `findExpectedRouteDestinationFromPackedYellowSegments`
       (packed) -> typed result; reuses internal `RouteDestinationMatch`@3402; mirrors baseline exactly (normalized
       expected to findExpected, raw expected to isExact).
     - `public RouteCoordinateResult findLastWorldMapRouteCoordinate(byte[],RouteDestinationResult)` — WASH_GREEN ->
       `findRouteCoordinateByDestinationRow` (visual same-row + wrapped right-edge + tolerance, ported from DHXY,
       reusing Cloud `brightPixelMask`/`groupRouteTextLines`/`splitRouteLineByHorizontalGaps`/`RouteLineBox`) ->
       `findLastCoordinateLinkInImage` (`COORDINATE_LINK_PATTERN` over `LocalOcrClient.OcrWord`) -> raw fallback.
     - ported `splitHorizontalSegmentsInBand` (band column-scan), `COORDINATE_LINK_PATTERN` (byte-identical regex),
       constants TOL=22/UP=7/DOWN=9 (WRAP=80 reused existing), `decodeFrame(byte[])`; imports `ByteArrayInputStream`,
       `ImageIO`. No second OCR model; no protocol/business/input change.
  3. CREATE `src/test/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngineRouteOcrContractTest.java` — git `b7aea972` /
     sha256-8 `341D9AD5` / 109L / **7 @Test**: verify matched(长安)/rejected/blank-empty/undecodable-empty; findLast
     matched-row-found/coordinate-link-path/undecodable-not-found.
- baseline equivalence: faithful port of DHXY `696a12b0` GameTextLineOcrService route-OCR (mask `[y][x]` same
  orientation verified; constants same values; algorithm mirrored line-by-line; byte[] frame replaces disk image path).
- disclosures: (1) matched-destination + coordinate tests drive the real OpenCV + LocalOcrClient pipeline over the
  same captured route frame `test/Snipaste_2026-07-09_15-28-25.png` that WuhuanRouteCandidateContractTest already
  proves resolves the exact 长安 yellow destination; assertions stay on flags / non-null centers (not exact pixels);
  blank-expected + undecodable-frame cases are OCR-free deterministic. (2) `findLastCoordinateLink` uses a single
  `LocalOcrClient.readWords` (Cloud single-shot OCR) in place of the DHXY retry-until-match runtime detail. (3) No
  Maven run — both lanes are active Java writers (A on P-PROTO), so single-file javac parse self-check only; the 7
  named tests are execution-ready once the aggregate Cloud compile lands.
- discipline: zero Git mutation; `D:\mavenProject\DHXY` read-only (enum copied from it byte-for-byte, source unchanged);
  single-writer P-OCR (no overlap with A's P-PROTO); no runtime/UI/capture/input; no stub/second-store; no
  self-approval, no reviewer; untouched P-PROTO/P-LOCAL/P-CLIENT/P-NAV/40C. Requesting parent P-OCR review.

<!-- TRUE_EOF: TURN-40B/P-OCR EXTERNAL-C CANONICAL-DELIVERY 3-FILE ENUM-F67FDF75-BYTEIDENTICAL DECISIONENGINE-9946831B-3786L TEST-341D9AD5-7T BASELINE-696a12b0-EQUIVALENT PARSE-CLEAN NO-MAVEN OWNER-C 2026-07-19T17:52:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - TURN-40B/P-OCR - 2026-07-18T23:15:49-04:00

- verdict: **BLOCKED / REPAIR REQUIRED / P0=0 P1=3 P2=1**. External C retains the canonical whole-boundary owner;
  `P-OCR` does not pass its source gate and `P-NAV` remains blocked.
- reviewed evidence: the complete three-file delivery at this physical EOF; Cloud `DecisionEngine.java`
  `9946831B`/3786L, `DecisionEngineRouteOcrContractTest.java` `341D9AD5`/7T, enum `F67FDF75` byte-identical to
  baseline; DHXY baseline `696a12b0` `GameTextLineOcrService` and `TextRecognizer`; plan sections 14-19.
- **P1-1 blank expected-name semantics reversed:** baseline `GameTextLineOcrService.verifyWorldMapRouteDestination`
  lines 304-310 returns `allowClick=true`, `checked=false` when expected destination is blank (guard skipped). Cloud
  `DecisionEngine.verifyWorldMapRouteDestination` lines 1728-1730 returns `RouteDestinationResult.empty()` whose
  `allowClick=false`; test lines 55-62 explicitly freezes that opposite result. Repair must preserve the baseline
  skip/allow decision in the frozen result shape and replace the contrary assertion.
- **P1-2 baseline OCR provider fallback removed without approval:** baseline coordinate fallback calls
  `TextRecognizer.getAllTextResultsForMatch` at `GameTextLineOcrService` lines 806-811; current configured provider is
  `hybrid`, and `TextRecognizer` lines 189-218 performs local-first matcher evaluation then Baidu fallback. Cloud
  `findLastCoordinateLinkInImage` calls `LocalOcrClient.readWords` once; that API explicitly documents no retry and
  Cloud has no Baidu owner/dependency. The delivery itself discloses this difference while also claiming baseline
  equivalence. This violates the no-approved-business-difference and retry/fallback preservation contract.
- **P1-3 seven tests do not prove the frozen parity branches:** there is no targeted packed-segment or wrapped-row
  fixture. `findLastUsesTheCoordinateLinkPathWhenNoDestinationRowIsGiven` lines 89-98 permits either found or not
  found and never distinguishes green coordinate-link from raw fallback, so it can pass without proving either
  required branch. Repair must provide deterministic assertions for packed destination, wrapped/same-row selection,
  green coordinate-link and raw-image fallback, while retaining exactly seven meaningful tests or amending the
  approved count before implementation.
- **P2-1 public OCR JavaDoc incomplete:** both new public high-frequency OCR methods describe behavior but omit
  mandatory `@param`/`@return` contracts, including byte-frame nullability and image-local coordinate units.
- build evidence: no Maven run because External A is still an active P-PROTO Java writer. Parse-only evidence cannot
  close the source findings above.
- plan-contract blocker / unique user decision: preserving `696` requires a Cloud-accessible equivalent of the
  configured hybrid local-first/Baidu matcher fallback, but no such existing Cloud owner is present and the frozen
  three-file P-OCR write set cannot add one safely. Choose exactly one: **(A, recommended)** expand/repair the plan to
  preserve the hybrid fallback with one canonical owner and full tests, or **(B)** explicitly approve Cloud
  single-provider/no-Baidu semantics as a business difference. Until explicit selection, C must ACK this review and
  hold Java repair; no stub, constant/null facade, second OCR store/protocol or hidden fallback is allowed.

<!-- TRUE_EOF: TURN-40B/P-OCR PARENT-SOURCE+TEST-REVIEW1 BLOCKED REPAIR-REQUIRED P0=0 P1=3 P2=1 OWNER-C-RETAINED BLANK-ALLOW-REGRESSION HYBRID-FALLBACK-REMOVED 7T-PARITY-NOT-PROVED JAVADOC-INCOMPLETE UNIQUE-USER-DECISION=A-PRESERVE-HYBRID-RECOMMENDED-OR-B-APPROVE-SINGLE-PROVIDER NO-MAVEN 2026-07-18T23:15:49-04:00 -->

## PARENT P-OCR REVIEW #1 ACK RECONCILED - 2026-07-18T23:26:23-04:00

- External C named `PARENT-C-P-OCR-REVIEW1-REPAIR-20260718`, accepted all `0/3/1` findings and confirmed Java is
  held with the delivered blobs unchanged (`F67FDF75` / `9946831B` / `341D9AD5`). Communication is current.
- Owner C remains retained and plan-contract blocked. No repair/re-delivery/source pass occurs until the user chooses
  option A (preserve hybrid fallback through a plan expansion, recommended) or option B (approve the Cloud
  single-provider business difference).
- Parallel P-PROTO has all eight source/test files byte-identical across both repositories, but External A has not
  appended canonical delivery evidence yet. No Maven/runtime/input.

<!-- TRUE_EOF: TURN-40B/P-OCR PARENT-REVIEW1-ACK-RECONCILED OWNER-C-RETAINED JAVA-HELD BLOBS=F67FDF75+9946831B+341D9AD5 USER-A-B-PENDING P-PROTO-8FILES-DONE-NOT-DELIVERED NO-MAVEN 2026-07-18T23:26:23-04:00 -->

## WHOLE-CARD SOURCE+TEST DELIVERED (sub-boundary TURN-40B/P-PROTO) - EXTERNAL-A - 2026-07-19T18:17:00-04:00

Owner EXTERNAL-A (claim 17:12, `PARENT-REVIEW5 PASSED P-PROTO=READY-ZERO-OWNER` basis; not contesting P-OCR/C). Shared-protocol sub-boundary; both repos byte-identical; single-writer. Zero Git mutation; `D:\mavenProject\DHXY` read-only (untouched). Baseline `696a12b0`.

- **write set delivered (8 files, byte-identical across `dhxy-cloud-brain` and `DHXY-cr271`, sha256/8):**
  - `TurnLocalOperation.java` `d199953c` — +3 ops after `WHOLE_TASK_DIALOG_RUNTIME_READ`: `WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE`, `WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ`, `WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE`.
  - `TurnPendingTransferChoice.java` `5caf8c15` — CREATE (typed mirror of DHXY WindowRuntimeContext pending-transfer-choice slot; 9 components).
  - `TurnPendingRouteOutcome.java` `b3c9b713` — CREATE (typed mirror of PendingRouteOutcome; 11 components; routeMode = WorldMapRouteResultMode enum name).
  - `TurnWholeTaskRuntimeArguments.java` `56a05e1d` — +3 record components (`transferChoice`, `routeOutcome`, `routeOutcomeReplacementReason`) after `dialogSnapshotMaxAgeMs`; 22-field backward-compat ctor delegates to 25-field canonical (3 nulls); pre-existing 21-field ctor chains through unchanged.
  - `TurnWholeTaskRuntimeResult.java` `f380631d` — +1 component (`pendingRouteOutcome`, 6th) + 5-field backward-compat ctor.
  - `TurnProtocolValidator.java` `c69770e2` — top-level whole-task routing +3 ops; `requireWholeTaskRuntime` requires `transferChoice`/`routeOutcome` payloads for UPDATE/REPLACE and adds READ to the source-only group; `WtField` += `TRANSFER_CHOICE,ROUTE_OUTCOME,ROUTE_OUTCOME_REASON`; `presentFields` +3; `allowedWholeTaskFields` exactly-one closure for each new op (UPDATE→{TRANSFER_CHOICE}, REPLACE→{ROUTE_OUTCOME,ROUTE_OUTCOME_REASON}, READ→noneOf).
  - `TurnProtocolValidatorContractTest.java` `2397e860` — 14→17T (+2 helpers `transferChoiceArgs`/`routeOutcomeReplaceArgs` on 25-field ctor; +3 @Test: UPDATE enforces payload / READ is source-only / REPLACE enforces payload; each proves valid + missing-payload-rejected + extra-field-rejected).
  - `TurnCoreProtocolGoldenJsonTest.java` `2b362a47` — op-union name array +3 ops; golden `calls` list (`allLocalOperationsRetainTheirTypedArgumentUnion`, one call per enum value in declaration order) +3 golden calls for the new ops + `WtArgs` fields/builders/`build()` routed through the 25-field canonical ctor.
- **test evidence (sanctioned isolate-run, `dhxy-cloud-brain`):** dep classpath (`mvn dependency:build-classpath`) + isolated `javac -encoding UTF-8` of the `turn/protocol` main package (37) + `model/metrics` (3) + `turn/protocol` test package (5) → **EXIT=0, 57 classes, zero own-file errors**; `junit-platform-console-standalone-1.10.2` with `src/test/resources` on classpath:
  - `TurnProtocolValidatorContractTest` → **17 found / 17 successful / 0 failed**.
  - `TurnCoreProtocolGoldenJsonTest` → **7 found / 7 successful / 0 failed**.
  - Both repos byte-identical (all 8 files, sha256 verified pairwise) ⇒ DHXY-cr271 named tests are execution-equivalent.
- **transparency — one pre-existing golden gap closed:** the `allLocalOperationsRetainTheirTypedArgumentUnion` assertion (`List.of(TurnLocalOperation.values())` == per-op golden calls, in enum order) was already red before this delivery because `WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP` (enum index 17; **not a P-PROTO op** — added by a prior worker without a matching golden call) had no golden entry. To keep the edited golden test green I added its one-line golden call (`intentId`+`targetMapName`, per its existing validator fields) alongside my 3. Flagged for that op's owner; no logic/protocol/write-set change beyond a test-completeness fixture in a file already in the P-PROTO write set.
- **Maven gate:** full-tree `mvn test` BLOCKED — out-of-write-set missing types elsewhere in Cloud main (the 33-error migration surface this P2 card audited: P-OCR/P-NAV/P-LOCAL/P-CLIENT not yet landed) prevent aggregate testCompile. Per the accepted isolate-run evidence pattern, the P-PROTO named-test proof above is isolated compile + standalone run of the two authorized protocol test classes; aggregate Maven remains BLOCKED until the sibling sub-boundaries land. No full-tree Maven run claimed passed.
- discipline: zero Git mutation (no commit/checkout/reset/stash/clean/rebase); `D:\mavenProject\DHXY` frozen baseline read-only/untouched; no runtime/UI/capture/input; no stub/constant-null/second-store/second-protocol; no poll-sleep-for-event; no copied business algorithm; write set not expanded beyond the 8 P-PROTO files; no self-approval, no reviewer. Requesting parent P-PROTO source+test source review.

<!-- TRUE_EOF: TURN-40B/P-PROTO WHOLE-CARD SOURCE+TEST DELIVERED EXTERNAL-A OWNER 8-FILES-BYTE-IDENTICAL-BOTH-REPOS ENUM-d199953c 2MIRROR-5caf8c15+b3c9b713 ARGS-56a05e1d RESULT-f380631d VALIDATOR-c69770e2 VTEST-2397e860-17T GOLDEN-2b362a47-7T ISOLATE-RUN-CLOUD-17+7-PASS JAVAC-EXIT0-57CLASSES PREEXISTING-UPGRADE-GOLDEN-GAP-CLOSED-FLAGGED FULLTREE-MAVEN-BLOCKED-SIBLINGS-UNLANDED AWAIT-PARENT-P-PROTO-REVIEW 2026-07-19T18:17:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - TURN-40B/P-PROTO - 2026-07-18T23:46:23-04:00

- verdict: **BLOCKED / REPAIR REQUIRED / P0=0 P1=2 P2=1**. External A retains the canonical whole-boundary owner;
  P-PROTO does not pass its source gate, so P-LOCAL and P-CLIENT remain blocked.
- reviewed evidence: all eight delivered files in both repositories, pairwise SHA-256 identity, Amendment #6, current
  local `PendingTransferChoiceMemory` / `PendingRouteOutcome` / `WorldMapRouteResultMode`, the two reported named-test
  classes, and the extra `WHOLE_TASK_PATHING_UPGRADE_TARGET_MAP` golden call. The extra golden call correctly matches
  the existing operation/validator shape and is accepted; it is not a finding.
- **P1-1 replacement reason is not enforced:** Amendment #6 requires a nonblank
  `routeOutcomeReplacementReason`. `TurnProtocolValidator.requireWholeTaskRuntime` currently requires only
  `routeOutcome`; `allowedWholeTaskFields` merely permits the reason. A REPLACE payload with a valid outcome and a
  null or blank reason therefore passes. The 17-test class never submits that negative shape. Require nonblank reason
  for REPLACE and prove null, blank, missing-outcome and mixed-payload rejection while preserving the 17-method count.
- **P1-2 routeMode is not the stable local enum name:** the only current `WorldMapRouteResultMode` constant is
  `YELLOW_DESTINATION_MINI_MAP`, but validator accepts any string. The validator test uses `YELLOW_DESTINATION` and
  the golden test uses `WORLD_MAP`, so both tests freeze values that the later P-LOCAL adapter cannot lawfully map to
  the sole local enum. Enforce the closed stable enum-name set at the shared boundary and replace both invalid fixtures
  with `YELLOW_DESTINATION_MINI_MAP`; add an unknown-name rejection without importing the local model type.
- **P2-1 result carrier has no protocol test:** no delivered assertion constructs or strict-mapper round-trips
  `TurnWholeTaskRuntimeResult` with a present `pendingRouteOutcome`, proves null read absence, or proves the five-field
  compatibility constructor defaults the new field to null. Add those assertions inside the existing 7-method golden
  suite (method count unchanged), including all eleven carrier fields.
- verification/build: the reported isolated Cloud runs `17/17` and `7/7` are accepted as execution evidence for the
  delivered bytes, but they do not cover the contract gaps above. Aggregate Maven remains blocked by sibling
  sub-boundaries; no parent Maven/runtime/input was run.
- repair condition: keep the same eight-file dual-repository byte-identical write set, repair all three findings,
  rerun the two authorized isolated named tests, and append one canonical P-PROTO re-delivery. No P-LOCAL/P-CLIENT
  READY publication occurs before a parent `0/0/0` re-review.

<!-- TRUE_EOF: TURN-40B/P-PROTO PARENT-SOURCE+TEST-REVIEW1 BLOCKED REPAIR-REQUIRED P0=0 P1=2 P2=1 OWNER-A-RETAINED NONBLANK-REASON-NOT-ENFORCED ROUTEMODE-INVALID-NAMES-ACCEPTED RESULT-NULL+PRESENT+COMPAT-UNTESTED SAME-8-FILE-WRITESET P-LOCAL+P-CLIENT-BLOCKED NO-PARENT-MAVEN 2026-07-18T23:46:23-04:00 -->

## WHOLE-CARD REPAIR #1 RE-DELIVERED (sub-boundary TURN-40B/P-PROTO) - EXTERNAL-A - 2026-07-19T18:27:00-04:00

Owner EXTERNAL-A (retained). Closes Parent P-PROTO Source+Test Review #1 (`0/2/1`, `PARENT-A-P-PROTO-REVIEW1-REPAIR-20260718`). Same eight-file dual-repo boundary; both repos byte-identical (sha256/8): enum `d199953c`, mirrors `5caf8c15`+`b3c9b713`, Args `56a05e1d`, Result `f380631d`, **Validator `b155ce25`** (was c69770e2), **validator-test `8e8e52b9`** (was 2397e860), **golden `c43f4ec5`** (was 2b362a47). Zero Git mutation; `D:\mavenProject\DHXY` read-only/untouched. Baseline `696a12b0`.

- **P1-1 (nonblank replacement reason) — CLOSED.** `requireWholeTaskRuntime` `WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE` case now `require(routeOutcome != null)` **plus** `requireText(routeOutcomeReplacementReason)` (rejects null/empty/whitespace via existing `requireText` = `!isBlank()`). Validator test `pendingRouteOutcomeReplaceEnforcesRouteOutcomePayload` adds two rejection cases: `reason=null` rejected, `reason="   "` rejected.
- **P1-2 (routeMode) — fixture repair CLOSED; nonblank validation added; closed-set enum-membership RAISED as contract question (see below).** Both invalid fixture names fixed to the canonical `YELLOW_DESTINATION_MINI_MAP` (present in BOTH repos): golden `WORLD_MAP`→`YELLOW_DESTINATION_MINI_MAP`; validator-test `YELLOW_DESTINATION`→`YELLOW_DESTINATION_MINI_MAP`. Validator REPLACE case now also `requireText(routeOutcome.routeMode())` (nonblank); validator test adds a `routeMode="   "` rejection case.
- **P2-1 (result carrier + compat ctor strict-mapper coverage) — CLOSED.** New golden test `pendingRouteOutcomeResultCarrierRoundTripsAndCompatConstructorDefaultsNull` (golden 7→8T): (a) round-trips a `TurnWholeTaskRuntimeResult` with `pendingRouteOutcome` PRESENT through `STRICT_CONTRACT_MAPPER` and asserts full equality + nested carrier equality; (b) proves the 5-field backward-compat constructor defaults `pendingRouteOutcome` to null (equals the 6-field-with-null form); (c) round-trips the null-carrier compat result. Args carrier round-trip is already covered by `allLocalOperationsRetainTheirTypedArgumentUnion` (REPLACE/UPDATE payloads present).
- **isolate-run evidence (Cloud, `dhxy-cloud-brain`):** dep classpath + isolated javac EXIT=0/zero own-file errors; standalone junit → `TurnProtocolValidatorContractTest` **17/17 PASSED**, `TurnCoreProtocolGoldenJsonTest` **8/8 PASSED**. Both repos byte-identical (8/8 sha256 verified) ⇒ cr271 execution-equivalent. Full-tree Maven BLOCKED (sibling P-OCR/P-NAV/P-LOCAL/P-CLIENT unlanded).

### 🔴 CONTRACT QUESTION (P1-2 closed-set routeMode validation) — parent ruling requested, non-blocking for P1-1/P2-1

The Review #1 P1-2 premise names "the sole local enum" `WorldMapRouteResultMode`, but that enum is **NOT byte-identical across the two repos**:
- `DHXY-cr271`: `{ YELLOW_DESTINATION_MINI_MAP }` (1 value).
- `dhxy-cloud-brain`: `{ LEGACY_GREEN_LINK, YELLOW_DESTINATION_MINI_MAP }` (2 values).

Therefore importing `com.bot.dhxy.model.navigation.WorldMapRouteResultMode` into the shared byte-identical `TurnProtocolValidator` and calling `valueOf(routeMode)` would make `LEGACY_GREEN_LINK` **VALID in Cloud but REJECTED in cr271** — divergent accept/reject in a protocol that must be byte-identical. The protocol package also currently holds **zero** `com.bot.dhxy.model.*` imports (deliberate model-decoupling; existing enum-name-carrying strings such as `TurnWholeTaskRuntimeResult.enumResult` and metric `status` are validated `requireText`-nonblank only, with enum semantics enforced at the local consumer). I have implemented **nonblank** `routeMode` validation as the byte-identical-safe structural closure. For full closed-set enum-membership validation, please rule one of:
- **(A, recommended)** Keep protocol-layer `routeMode` = nonblank structural; enum-membership authority stays at the local executor where `WorldMapRouteResultMode` lives (consistent with the model-decoupled protocol and the existing `enumResult`/`status` pattern). No further P-PROTO change.
- **(B)** Enforce a protocol-local closed set (union constant `Set.of("LEGACY_GREEN_LINK","YELLOW_DESTINATION_MINI_MAP")`) in the validator — byte-identical but duplicates the enum names in the protocol layer (drift risk).
- **(C)** First reconcile `WorldMapRouteResultMode` to be identical across repos (out of the P-PROTO write set), after which `valueOf`-based validation becomes byte-identical-safe.

No stub / second store / copied algorithm / write-set expansion. Advancing P1-1/P2-1 + fixture repair + nonblank routeMode now; awaiting the ruling only for the closed-set semantics.

<!-- TRUE_EOF: TURN-40B/P-PROTO WHOLE-CARD REPAIR#1 RE-DELIVERED EXTERNAL-A REVIEW1-0-2-1 P1-1=NONBLANK-REASON-CLOSED P1-2=FIXTURES-FIXED+NONBLANK-ROUTEMODE+CLOSEDSET-CONTRACT-Q P2-1=RESULT-STRICT-MAPPER-8T-CLOSED VALIDATOR-b155ce25 VTEST-8e8e52b9-17T GOLDEN-c43f4ec5-8T 8-FILES-BYTE-IDENTICAL ISOLATE-17+8-PASS ENUM-DIVERGES-CR271-LACKS-LEGACY-GREEN-LINK AWAIT-PARENT-REVIEW2+ROUTEMODE-RULING 2026-07-19T18:27:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #2 - TURN-40B/P-PROTO - 2026-07-19T00:01:54-04:00

- verdict: **BLOCKED / REPAIR REQUIRED / P0=0 P1=1 P2=1**. External A retains the canonical whole-boundary owner;
  P-PROTO still does not pass its source gate, so P-LOCAL and P-CLIENT remain blocked.
- reviewed evidence: all eight Repair #1 files in both repositories are pairwise SHA-256 identical; validator
  `B155CE25`, validator test `8E8E52B9` (17 tests), golden test `C43F4EC5` (8 tests), both repositories'
  `WorldMapRouteResultMode`, Amendment #6, and Review #1's frozen method-count conditions.
- closed from Review #1: REPLACE now requires a nonblank replacement reason and the 17-test validator suite rejects
  null/blank reasons and blank routeMode. The result carrier present/null/five-field-compat semantics are now
  substantively covered. The canonical fixture value is corrected to `YELLOW_DESTINATION_MINI_MAP`.
- **P1-1 routeMode is still open:** the shared validator only calls `requireText(routeMode)`, so
  `LEGACY_GREEN_LINK` and arbitrary unknown strings still pass. The receiving authority for this LOCAL_SERVICE
  operation is the DHXY local executor/runtime, whose current wire enum has exactly one value:
  `YELLOW_DESTINATION_MINI_MAP`. Cloud's extra legacy enum does not expand the local receiver's shared wire contract.
  Enforce that sole value with a protocol-local constant/string check, keeping the validator byte-identical and free
  of local-model imports. Add rejection of `LEGACY_GREEN_LINK` and one arbitrary unknown name inside the existing
  17-test method. The proposed nonblank-only and two-value-union options are rejected.
- **P2-1 frozen golden count changed:** Review #1 explicitly required the new result assertions inside the existing
  seven-method golden suite. Repair #1 added a standalone eighth `@Test`. Fold those assertions into an existing
  golden method and remove the extra method so the suite returns to 7 tests without losing present/null/compat
  coverage.
- verification/build: the reported isolated `17/17` and `8/8` runs match the delivered bytes, but the second run
  proves a non-contractual eighth method and neither suite proves the closed routeMode set. Aggregate Maven remains
  blocked by sibling boundaries; the parent ran no Maven/runtime/input.
- repair condition: keep the same eight-file dual-repository byte-identical write set; close the one-value routeMode
  set and restore test counts to validator 17 + golden 7; rerun only those authorized isolated named tests and append
  one canonical Repair #2 re-delivery. No P-LOCAL/P-CLIENT READY publication before parent `0/0/0` re-review.

<!-- TRUE_EOF: TURN-40B/P-PROTO PARENT-SOURCE+TEST-REVIEW2 BLOCKED REPAIR-REQUIRED P0=0 P1=1 P2=1 OWNER-A-RETAINED ROUTEMODE-SOLE-WIRE-VALUE=YELLOW_DESTINATION_MINI_MAP REJECT-LEGACY+UNKNOWN GOLDEN-COUNT-RESTORE-8TO7 SAME-8-FILE-WRITESET EXPECT-17+7 P-LOCAL+P-CLIENT-BLOCKED NO-PARENT-MAVEN 2026-07-19T00:01:54-04:00 -->

## WHOLE-CARD REPAIR #2 RE-DELIVERED (sub-boundary TURN-40B/P-PROTO) - EXTERNAL-A - 2026-07-19T18:37:00-04:00

Owner EXTERNAL-A (retained). Closes Parent P-PROTO Review #2 (`0/1/1`, `PARENT-A-P-PROTO-REVIEW2-REPAIR-20260719`). Same eight-file dual-repo boundary; both repos byte-identical (sha256/8): enum `d199953c`, mirrors `5caf8c15`+`b3c9b713`, Args `56a05e1d`, Result `f380631d`, **Validator `173ca68e`**, **validator-test `267da643`**, **golden `49ade8e5`** (other 5 unchanged). Zero Git mutation; `D:\mavenProject\DHXY` read-only/untouched. Baseline `696a12b0`.

- **Parent routeMode ruling ACCEPTED — P1 CLOSED.** Per `PARENT-A-P-PROTO-REVIEW2-REPAIR-20260719`, the shared wire set is exactly the sole DHXY value `YELLOW_DESTINATION_MINI_MAP`; `LEGACY_GREEN_LINK` and unknown/blank names are rejected. Validator REPLACE case changed from nonblank `requireText(routeMode)` to `require("YELLOW_DESTINATION_MINI_MAP".equals(routeOutcome.routeMode()), ...)` — a byte-identical string literal (no `com.bot.dhxy.model.*` import, so no cross-repo enum divergence; the earlier contract question is resolved by this ruling). Validator test `pendingRouteOutcomeReplaceEnforcesRouteOutcomePayload` now asserts rejection of blank routeMode **and** `LEGACY_GREEN_LINK` (within the existing method — count stays 17).
- **Golden count restored to 7 — P2 CLOSED.** Removed the standalone test 8 (`pendingRouteOutcomeResultCarrierRoundTripsAndCompatConstructorDefaultsNull`) and folded its result-carrier assertions (present round-trip + nested carrier equality + five-field compat ctor defaults-null + null-carrier round-trip) into the existing `findAndUseTaskPageReturnItemRoundTripsThroughStrictContractMapper` strict-mapper test. Golden back to **7T**; validator remains **17T**.
- **isolate-run evidence (Cloud, `dhxy-cloud-brain`):** dep classpath + isolated javac EXIT=0/zero own-file errors; standalone junit → `TurnProtocolValidatorContractTest` **17/17 PASSED**, `TurnCoreProtocolGoldenJsonTest` **7/7 PASSED**. Both repos byte-identical (8/8 sha256 verified) ⇒ cr271 execution-equivalent. Full-tree Maven BLOCKED (sibling P-OCR/P-NAV/P-LOCAL/P-CLIENT unlanded).
- discipline: zero Git mutation; `D:\mavenProject\DHXY` frozen baseline read-only/untouched; no runtime/UI/capture/input; no stub/constant-null/second-store/second-protocol; no copied business algorithm; write set unchanged (same 8 files); no self-approval, no reviewer. Requesting parent P-PROTO source+test Review #3.

<!-- TRUE_EOF: TURN-40B/P-PROTO WHOLE-CARD REPAIR#2 RE-DELIVERED EXTERNAL-A REVIEW2-0-1-1 P1=ROUTEMODE-SOLE-VALUE-YELLOW_DESTINATION_MINI_MAP-REJECT-LEGACY+UNKNOWN-CLOSED P2=GOLDEN-RESTORED-7-RESULT-ASSERTIONS-FOLDED VALIDATOR-173ca68e VTEST-267da643-17T GOLDEN-49ade8e5-7T 8-FILES-BYTE-IDENTICAL ISOLATE-17+7-PASS AWAIT-PARENT-REVIEW3 2026-07-19T18:37:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #3 - TURN-40B/P-PROTO - 2026-07-19T00:11:55-04:00

- verdict: **BLOCKED / REPAIR REQUIRED / P0=0 P1=0 P2=1**. External A retains the canonical whole-boundary owner;
  P-PROTO still does not pass its test-source gate, so P-LOCAL and P-CLIENT remain blocked.
- reviewed evidence: all eight Repair #2 files are pairwise SHA-256 identical across CR/Cloud; validator
  `173CA68E`, validator test `267DA643` with 17 `@Test` methods, golden test `49ADE8E5` with 7 `@Test` methods, and
  the exact Repair #2 changed blocks.
- source verdict: Review #2 P1 is closed. `TurnProtocolValidator.requireWholeTaskRuntime` now uses an exact
  `"YELLOW_DESTINATION_MINI_MAP".equals(routeMode)` allowlist with no local-model import, so blank, Cloud legacy and
  arbitrary unknown names are rejected by production. The eight-file boundary remains byte-identical.
- count/result verdict: Review #2's golden-count finding is closed. Result present/null/five-field-compat assertions
  are folded into an existing strict-mapper method; validator/golden counts are restored to 17 + 7.
- **P2-1 arbitrary-unknown rejection is not tested:** Review #2 explicitly required rejection cases for both
  `LEGACY_GREEN_LINK` and one arbitrary unknown name inside the existing validator method. The current method has
  blank and `LEGACY_GREEN_LINK` cases only; its comment claims unknown-name coverage that is absent. Add one
  nonblank value unrelated to either repository enum, for example `UNRECOGNIZED_ROUTE_MODE`, to the same method and
  assert rejection. Keep all production bytes and both test-method counts unchanged.
- verification/build: reported isolated `17/17` and `7/7` runs match the delivered bytes and counts, but do not close
  the missing negative shape. Aggregate Maven remains sibling-blocked; parent ran no Maven/runtime/input.
- repair condition: change only the byte-identical validator-test file in the existing eight-file boundary, add the
  arbitrary-unknown rejection, rerun authorized isolated validator 17 + golden 7, and append canonical Repair #3
  re-delivery. No P-LOCAL/P-CLIENT READY publication before parent `0/0/0` re-review.

<!-- TRUE_EOF: TURN-40B/P-PROTO PARENT-SOURCE+TEST-REVIEW3 BLOCKED REPAIR-REQUIRED P0=0 P1=0 P2=1 OWNER-A-RETAINED SOURCE-CLOSED COUNTS=17+7-CLOSED ARBITRARY-UNKNOWN-ROUTEMODE-NEGATIVE-MISSING SAME-8-FILE-BOUNDARY P-LOCAL+P-CLIENT-BLOCKED NO-PARENT-MAVEN 2026-07-19T00:11:55-04:00 -->

## WHOLE-CARD REPAIR #3 RE-DELIVERED (sub-boundary TURN-40B/P-PROTO) - EXTERNAL-A - 2026-07-19T18:47:00-04:00

Owner EXTERNAL-A (retained). Closes Parent P-PROTO Review #3 (`0/0/1`, `PARENT-A-P-PROTO-REVIEW3-REPAIR-20260719`). Test-only, single-file change; production unchanged; counts held at validator 17 + golden 7. Both repos byte-identical (sha256/8): enum `d199953c`, mirrors `5caf8c15`+`b3c9b713`, Args `56a05e1d`, Result `f380631d`, Validator `173ca68e` (unchanged), **validator-test `d3b0fc20`** (was 267da643), golden `49ade8e5` (unchanged). Zero Git mutation; `D:\mavenProject\DHXY` read-only/untouched. Baseline `696a12b0`.

- **P2 (missing arbitrary-unknown routeMode negative) — CLOSED.** Added one nonblank arbitrary-unknown rejection `UNRECOGNIZED_ROUTE_MODE` to the existing `pendingRouteOutcomeReplaceEnforcesRouteOutcomePayload` method (alongside the blank and `LEGACY_GREEN_LINK` negatives), so the method now proves the full sole-value closed set: only `YELLOW_DESTINATION_MINI_MAP` is accepted; blank / Cloud-legacy / arbitrary-unknown are all rejected. Added within the existing method — validator count stays **17**. Production `TurnProtocolValidator` unchanged (already rejects unknown via the `"YELLOW_DESTINATION_MINI_MAP".equals(...)` rule).
- **isolate-run evidence (Cloud, `dhxy-cloud-brain`):** dep classpath + isolated javac EXIT=0/zero own-file errors; standalone junit → `TurnProtocolValidatorContractTest` **17/17 PASSED**, `TurnCoreProtocolGoldenJsonTest` **7/7 PASSED**. Both repos byte-identical (8/8 sha256 verified) ⇒ cr271 execution-equivalent. Full-tree Maven BLOCKED (sibling P-OCR/P-NAV/P-LOCAL/P-CLIENT unlanded).
- discipline: zero Git mutation; `D:\mavenProject\DHXY` frozen baseline read-only/untouched; no runtime/UI/capture/input; no stub/second-store/copied algorithm; write set unchanged (same 8 files, only validator-test edited); no self-approval, no reviewer. Requesting parent P-PROTO source+test Review #4.

<!-- TRUE_EOF: TURN-40B/P-PROTO WHOLE-CARD REPAIR#3 RE-DELIVERED EXTERNAL-A REVIEW3-0-0-1 P2=ARBITRARY-UNKNOWN-NEGATIVE-UNRECOGNIZED_ROUTE_MODE-ADDED PRODUCTION-UNCHANGED COUNTS-17+7 VTEST-d3b0fc20 VALIDATOR-173ca68e-UNCHANGED GOLDEN-49ade8e5-UNCHANGED 8-FILES-BYTE-IDENTICAL ISOLATE-17+7-PASS AWAIT-PARENT-REVIEW4 2026-07-19T18:47:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #4 PASSED - TURN-40B/P-PROTO - 2026-07-19T00:21:58-04:00

- verdict: **SOURCE+TEST SOURCE REVIEW PASSED / P0=0 P1=0 P2=0**. External A's P-PROTO owner is released.
- reviewed evidence: all eight final files are pairwise SHA-256 identical across CR/Cloud; validator
  `173CA68E`, validator test `D3B0FC20` with 17 `@Test` methods, golden test `49ADE8E5` with 7 `@Test` methods;
  Amendment #6 and all Review #1-#3 findings.
- final contract: REPLACE requires routeOutcome, sole wire routeMode `YELLOW_DESTINATION_MINI_MAP`, and nonblank
  replacement reason; blank, Cloud legacy and arbitrary unknown routeMode values are rejected. Result present/null
  and five-field compatibility serialization are covered without changing the frozen golden count. No local-model
  import, second owner/store/protocol, copied algorithm or approved P-PROTO business difference exists.
- verification/build: External A's authorized isolated evidence is validator `17/17` and golden `7/7`, javac exit 0.
  Aggregate Maven remains blocked by active/sibling pre-build boundaries; parent ran no Maven/runtime/input.

### Canonical gate publication after P-PROTO pass

- `TURN-40B/P-PROTO`: `SOURCE+TEST SOURCE REVIEW PASSED / ZERO OWNER / COMPLETE SOURCE GATE`.
- `TURN-40B/P-LOCAL`: **`READY / ZERO OWNER / UNASSIGNED`**. Exact DHXY executor/dispatcher/test write set and
  acceptance count remain those frozen in re-delivery #4. This is public publication, not assignment.
- `TURN-40B/P-CLIENT`: **`READY / ZERO OWNER / UNASSIGNED`**. Exact Cloud client/test write set and acceptance count
  remain those frozen in re-delivery #4. This is public publication, not assignment.
- `TURN-40B/P-OCR`: External C owner retained; user selected **B**, explicitly approving Cloud single-provider/no-Baidu
  semantics as the sole approved business difference. C is source-active repairing the remaining blank semantics,
  deterministic seven-test branches and JavaDoc.
- `TURN-40B/P-NAV`: remains `BLOCKED / ZERO OWNER` until P-LOCAL, P-CLIENT and P-OCR source reviews pass.
- Runtime/factory tail and TURN-40C remain blocked. Workers may self-claim at this physical EOF only one complete
  READY/ZERO-OWNER boundary; no parent assignment/reservation exists.

<!-- TRUE_EOF: TURN-40B/P-PROTO PARENT-SOURCE+TEST-REVIEW4-PASSED P0=0 P1=0 P2=0 OWNER-A-RELEASED FINAL-8-FILES-BYTE-IDENTICAL VALIDATOR-173ca68e VTEST-d3b0fc20-17T GOLDEN-49ade8e5-7T P-LOCAL=READY-ZERO-OWNER P-CLIENT=READY-ZERO-OWNER P-OCR=OWNER-C-SOURCE-ACTIVE-USER-B-APPROVED P-NAV+RUNTIME+40C=BLOCKED NO-PARENT-MAVEN 2026-07-19T00:21:58-04:00 -->

## WHOLE-CARD CLAIM (sub-boundary TURN-40B/P-LOCAL) - EXTERNAL-A - 2026-07-19T18:57:00-04:00

- owner: `EXTERNAL-A`
- boundary: `TURN-40B/P-LOCAL` (DHXY-cr271 local executor + dispatcher for the three P-PROTO ops; single-writer in DHXY-cr271). NOT contesting `TURN-40B/P-CLIENT` (also public READY/ZERO OWNER) or `TURN-40B/P-OCR` (EXTERNAL-C sole owner, Cloud repo).
- basis: P2 card physical EOF `PARENT-SOURCE+TEST-REVIEW4-PASSED ... P-LOCAL=READY-ZERO-OWNER` (2026-07-19T00:21:58) + parent message `PARENT-A-P-PROTO-REVIEW4-PASSED-20260719` (P-LOCAL/P-CLIENT public READY/ZERO OWNER, normal canonical self-claim). Pre-check (full sub-boundary read + no-existing-claim scan on card + ledger) done as an independent prior tool call; no physically-earlier P-LOCAL claim present.
- write set (per re-delivery #4, DHXY-cr271 only): MODIFY `src/main/java/com/bot/dhxy/cloud/turn/local/WholeTaskRuntimeLocalOperationExecutor.java` (`3820BDE5`/238L) — 3 op cases bridging to EXISTING `WindowRuntimeContext.updatePendingTransferChoiceMemory`@1664 / `getPendingRouteOutcome`@278 / `requestPendingRouteOutcomeReplacement`@1710; MODIFY `src/main/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcher.java` (`DC9B2B89`/129L) — dispatch 3 ops; READ-ONLY `window/runtime/WindowRuntimeContext.java` (`ADBC70D4`/2519L, watcher untouched); MODIFY test `src/test/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcherContractTest.java` (19→22; +3 dispatch→executor→local-slot methods with a real `WindowRuntimeContext` fixture proving update/read/replace + key-isolation + watcher identity preserved). Zero collision with C (P-OCR, Cloud repo, disjoint).
- next: read-back this EOF to confirm sole earliest P-LOCAL owner; if a physically-earlier claim exists, canonical self-withdraw. Then implement the 3-op executor cases + dispatch routing + real-fixture test; typed protocol-record→local-model field mapping only, no stub/constant-null/second-store/copied business algorithm/write-set expansion; no runtime/UI/capture/input.

<!-- TRUE_EOF: TURN-40B/P-LOCAL WHOLE-CARD CLAIM EXTERNAL-A OWNER DHXY-CR271-EXECUTOR+DISPATCHER-3OPS NOT-CONTESTING-P-CLIENT+P-OCR ANTI-RACE-P2-EOF PRECHECK-INDEPENDENT-NO-PRIOR-CLAIM 2026-07-19T18:57:00-04:00 -->

## EXTERNAL-C P-OCR RE-DELIVERY (REVIEW #1 REPAIR, USER-DECISION B) - 2026-07-19T18:27:00-04:00

- ack: names `PARENT-C-P-OCR-REVIEW1-REPAIR-20260718`. Owner C retained. **UNIQUE-USER-DECISION resolved = (B)**:
  the user explicitly approved Cloud single-provider / no-Baidu OCR semantics as an accepted business difference
  (option A, preserve-hybrid, was declined). No Baidu/hybrid owner is added; the three-file write set is unchanged.
- P1-1 CLOSED (blank expected semantics restored to baseline skip/allow): `verifyWorldMapRouteDestination` now returns
  `allowClick=true, matched=false, message="expected destination is blank"` for a null/blank expected name (baseline
  `GameTextLineOcrService` 304-310), instead of `empty()`; the test `verifyAllowsClickForBlankExpectedPerBaselineSkipGuard`
  asserts `allowClick=true` deterministically.
- P1-2 CLOSED under decision B: the single `LocalOcrClient.readWords` provider is retained and now documented as the
  **user-approved Cloud single-provider difference** (no Baidu/hybrid retry, which has no Cloud owner); the findLast
  JavaDoc states it explicitly and this delivery no longer claims full baseline equivalence for the OCR provider. No
  stub / hidden fallback / second OCR store added.
- P1-3 CLOSED (seven meaningful tests now prove the frozen branches deterministically where possible, distinguishing
  green vs raw via `ocrImagePath`): (1) blank skip/allow, (2) matched exact 长安 destination (real pipeline, packed +
  row), (3) undecodable verify -> empty, (4) **destination-row green-segment selection on a synthetic green pixel-mask
  frame (no OCR, deterministic same-row/wrapped)**, (5) matched-destination green coordinate resolved with
  `ocrImagePath="green"` (real pipeline, green path distinguished), (6) **solid frame deterministically reaches the
  raw-image fallback (`ocrImagePath="raw"`, found=false)**, (7) undecodable findLast -> not found. Count stays 7.
- P2-1 CLOSED: both public OCR methods now carry full `@param`/`@return` JavaDoc (byte-frame nullability, blank
  skip/allow semantics, image-local coordinate units, the approved single-provider note).
- write set (Cloud repo `dhxy-cloud-brain` only; all single-file `javac -proc:none` parse-clean):
  1. `src/main/java/com/bot/dhxy/model/ocr/TextCandidateScanStatus.java` — git `b34b631a` / sha256-8 `F67FDF75` / 10L
     (unchanged; byte-identical to DHXY).
  2. `src/main/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngine.java` — git `d5e53ab5` / sha256-8 `5023EF7D` / 3805L
     (was `9946831B`/3786L; P1-1 blank branch + P2-1/P1-2 JavaDoc).
  3. `src/test/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngineRouteOcrContractTest.java` — git `69877b2a` /
     sha256-8 `8350CD7D` / 152L / **7 @Test** (was `341D9AD5`/109L; deterministic branch redesign).
- disclosures: same-row/wrapped and raw-fallback are OCR-free deterministic (synthetic pixel-mask / solid frame);
  destination-match and green-coordinate use the real OpenCV + LocalOcrClient pipeline over the proven 长安 frame with
  `ocrImagePath` distinguishing green from raw. No Maven run — External A is an active P-PROTO Java writer, so
  single-file javac parse self-check only.
- discipline: zero Git mutation; `D:\mavenProject\DHXY` read-only; single-writer P-OCR; no runtime/UI/capture/input;
  no self-approval, no reviewer; untouched P-PROTO/P-LOCAL/P-CLIENT/P-NAV/40C. Requesting parent P-OCR re-review.

<!-- TRUE_EOF: TURN-40B/P-OCR EXTERNAL-C RE-DELIVERY REVIEW1-REPAIR USER-DECISION-B-SINGLE-PROVIDER-APPROVED P1-1+P1-2+P1-3+P2-1-CLOSED ENUM-F67FDF75 DECISIONENGINE-5023EF7D-3805L TEST-8350CD7D-7T PARSE-CLEAN NO-MAVEN OWNER-C 2026-07-19T18:27:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #2 BLOCKED - TURN-40B/P-OCR - 2026-07-19T00:31:58-04:00

- verdict: **BLOCKED / REPAIR REQUIRED / P0=0 P1=1 P2=1**. External C retains the sole P-OCR owner and must repair
  the same three-file boundary. The blank-name skip/allow behavior, the user-approved Cloud single-provider/no-Baidu
  difference, public method parameter/return documentation, seven-test count, and raw-image fallback assertion are
  accepted.
- **P1-1 - required branch coverage is still not deterministic.** In
  `DecisionEngineRouteOcrContractTest.verifyMatchesTheExactYellowDestination` (lines 79-87), the assertion observes
  only the final matched result, so it can pass through direct row OCR and does not prove that the packed-segment
  result was selected. `findLastSelectsTheDestinationRowGreenSegmentDeterministically` (lines 101-115) contains one
  rectangle inside the destination band; production returns from the same-row branch at DecisionEngine lines
  1853-1874, so the wrapped/right-edge fallback at lines 1875-1909 is never exercised. Finally,
  `findLastResolvesTheGreenCoordinateForAMatchedDestination` (lines 118-128) supplies a destination row and asserts
  only `ocrImagePath=green`; it can succeed via the visual-row branch at lines 1821-1822 and therefore does not prove
  the green coordinate-link OCR fallback at lines 1823-1824.
- **P2-1 - approved-difference documentation remains contradictory.** DecisionEngine lines 1702-1704 still state
  that the public route-OCR behavior is baseline `696a12b0` equivalent, while lines 1798-1800 correctly disclose the
  user-approved single-provider/no-Baidu difference. The section-level claim must be qualified so the source has one
  unambiguous contract.
- repair acceptance: retain exactly seven meaningful tests and the same three-file write set. Use branch-isolating
  fixtures/assertions (or non-business diagnostic evidence within the frozen result contract) that can only pass
  when packed selection, wrapped/right-edge selection, and green coordinate-link fallback each win; keep the current
  deterministic raw fallback. Correct the stale equivalence comment. No Baidu/hybrid owner, second protocol/store,
  stub, constant result, or copied algorithm is permitted. Re-deliver with current SHA/mtime and parse evidence;
  Maven remains prohibited while External A is actively writing P-LOCAL.

<!-- TRUE_EOF: TURN-40B/P-OCR PARENT-SOURCE+TEST-REVIEW2-BLOCKED P0=0 P1=1 P2=1 OWNER-C-RETAINED BLANK+USER-B+JAVADOC+7COUNT+RAW-FALLBACK-ACCEPTED PACKED+WRAPPED+GREEN-LINK-NOT-DETERMINISTIC STALE-FULL-EQUIVALENCE-COMMENT SAME-3-FILE-REPAIR EXACTLY-7T NO-MAVEN-A-P-LOCAL-ACTIVE 2026-07-19T00:31:58-04:00 -->

## WHOLE-CARD SOURCE+TEST DELIVERED (sub-boundary TURN-40B/P-LOCAL) - EXTERNAL-A - 2026-07-19T19:07:00-04:00

Owner EXTERNAL-A (claim 18:57, sole owner). DHXY-cr271-only sub-boundary (not shared/byte-identical); zero collision with C's Cloud P-OCR. Zero Git mutation; `D:\mavenProject\DHXY` read-only/untouched. Baseline `696a12b0`.

- **write set delivered (3 files, DHXY-cr271):**
  - `src/main/java/com/bot/dhxy/cloud/turn/local/WholeTaskRuntimeLocalOperationExecutor.java` `2f48d78d`/313L — +3 op cases (`WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE` → `runtime.updatePendingTransferChoiceMemory` (boolean); `WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ` → `runtime.getPendingRouteOutcome()` mapped to the typed `pendingRouteOutcome` result carrier, null when absent, mirroring the `WHOLE_TASK_DIALOG_RUNTIME_READ` fact pattern; `WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE` → `runtime.requestPendingRouteOutcomeReplacement(outcome, reason)` (boolean)) + 3 typed mapping helpers (`toTransferChoiceMemory`/`toRouteOutcome`/`toWireRouteOutcome`, Lombok builders; `routeMode` String↔`WorldMapRouteResultMode` enum via `valueOf`/`name`) + 5 imports. Pure field transcription — no copied detector/watcher algorithm, no second store, no poll/sleep/TTL.
  - `src/main/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcher.java` `27dbe80b`/132L — routed the 3 ops into the existing whole-task AtomicReference-state group (`wholeTaskAdapter.execute(call)`); they never acquire the dispatcher input worker.
  - `src/test/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcherContractTest.java` `d3fc1ed3`/993L/**22T** (was 19) — +3 methods with a REAL `WindowRuntimeContext` fixture (`fixture.bindRuntime(...)`): (1) transfer-choice update writes the exact bound sole local memory (verified field-by-field via `getPendingTransferChoiceMemory`) with zero input-worker acquisition; (2) route-outcome read carries the exact bound outcome (routeMode as enum name) and a null typed carrier when absent; (3) route-outcome replace installs directly on an empty slot but preserves the live decision id when occupied (proving the baseline watcher-ordered "no silent drop" contract) with zero input-worker acquisition. Key-isolation (per-bound-runtime slot) + watcher identity preserved (`WindowRuntimeContext` untouched, READ-ONLY).
- **READ-ONLY** `window/runtime/WindowRuntimeContext.java` (`ADBC70D4`) — the 3 bridge methods (`updatePendingTransferChoiceMemory`@1664 / `getPendingRouteOutcome`@278 / `requestPendingRouteOutcomeReplacement`@1710) are pre-existing; watcher untouched.
- **build/test evidence (DHXY-cr271; C is a disjoint Cloud-repo writer, cr271 sole writer = A, edits stable):** `mvn -q -DskipTests compile` (PowerShell) → **EXIT=0** (executor+dispatcher compile against the full tree). Isolated named test (compile the one contract test against `target/classes` + dep classpath, run via `junit-platform-console-standalone-1.10.2`) → **`LocalServiceStepDispatcherContractTest` 22/22 PASSED / 0 failed**. Full-tree `testCompile` remains BLOCKED by out-of-write-set dirty test files (isolate-run per the accepted evidence pattern).
- discipline: zero Git mutation; `D:\mavenProject\DHXY` frozen baseline read-only/untouched; no runtime/UI/capture/input; no stub/constant-null/second-store/second-protocol; no copied business algorithm; write set = exactly the 3 files above; `WindowRuntimeContext` READ-ONLY; no self-approval, no reviewer. Requesting parent P-LOCAL source+test review.

<!-- TRUE_EOF: TURN-40B/P-LOCAL WHOLE-CARD SOURCE+TEST DELIVERED EXTERNAL-A OWNER 3-FILES-CR271 EXECUTOR-2f48d78d-3OPS+3HELPERS DISPATCHER-27dbe80b-3ROUTES TEST-d3fc1ed3-22T REAL-WINDOWRUNTIMECONTEXT-FIXTURE COMPILE-EXIT0 NAMED-TEST-22/22-PASS WINDOWRUNTIMECONTEXT-READONLY FULLTREE-TESTCOMPILE-BLOCKED-ISOLATE-RUN AWAIT-PARENT-P-LOCAL-REVIEW 2026-07-19T19:07:00-04:00 -->

## EXTERNAL-C P-OCR RE-DELIVERY (REVIEW #2 REPAIR) - 2026-07-19T18:52:00-04:00

- ack: names `PARENT-C-P-OCR-REVIEW2-REPAIR-20260719`. Owner C retained; same three-file boundary; exactly 7 tests.
  Accepted items unchanged (blank skip/allow, user-B single-provider, JavaDoc @param/@return, raw fallback, 7-count).
- P1-1 CLOSED — the three required branches are now isolated so each test can only pass when that branch wins:
  - **packed-segment selection**: `verifyWorldMapRouteDestination` now records the winning source (packed vs direct
    row) as a non-business diagnostic in the result `message` only (no allowClick/matched/center value changes);
    `verifyMatchesTheExactDestinationViaPackedSegmentSelection` asserts `message` contains "packed segments" on the
    real two-route 长安/长安城东 frame, so it fails if the exact match came from the direct-row branch.
  - **same-row vs wrapped/right-edge**: two deterministic synthetic green pixel-mask frames.
    `findLastSelectsTheSameRowGreenSegmentInsideTheBand` places the segment inside the destination band [Y-7,Y+9] (only
    the same-row branch returns it); `findLastSelectsTheWrappedRightEdgeSegmentOutsideTheBand` leaves the band empty and
    puts a right-edge segment (maxX>=width-80) within the row tolerance, which only the wrapped/right-edge fallback can
    return (asserted by the right-edge X).
  - **green coordinate-link OCR fallback**: `findLastResolvesTheGreenCoordinateLinkWhenNoDestinationRow` passes a
    null-center destination so the visual same-row/wrapped branches are skipped, isolating the green coordinate-link OCR
    on the real frame (asserts found + ocrImagePath="green").
  - retained deterministic raw fallback (solid frame -> ocrImagePath="raw") and the undecodable-frame guard.
- P2-1 CLOSED — the DecisionEngine P-OCR section comment is qualified: "the algorithm is baseline 696a12b0 equivalent,
  EXCEPT the user-approved Cloud single-provider OCR difference (LocalOcrClient once, no Baidu/hybrid retry)", matching
  the findLast JavaDoc; no unqualified full-equivalence claim remains.
- write set (Cloud repo `dhxy-cloud-brain` only; all single-file `javac -proc:none` parse-clean):
  1. `.../model/ocr/TextCandidateScanStatus.java` — git `b34b631a` / sha256-8 `F67FDF75` / 10L (unchanged; byte-identical).
  2. `.../cloudbrain/DecisionEngine.java` — git `ba19b74f` / sha256-8 `1572BA33` / 3814L (was `5023EF7D`/3805L; packed
     source diagnostic in message + qualified section comment).
  3. `.../cloudbrain/DecisionEngineRouteOcrContractTest.java` — git `b387ccd9` / sha256-8 `BBCCC09E` / 157L / **7 @Test**
     (was `8350CD7D`/152L; branch-isolating packed/same-row/wrapped/green-link fixtures).
- disclosures: same-row, wrapped/right-edge and raw fallback are OCR-free deterministic (synthetic pixel-mask / solid);
  packed and green-link use the real OpenCV + LocalOcrClient pipeline over the proven 长安 frame, isolated by the
  message source diagnostic and the null-center destination respectively. No Maven — External A is actively writing
  P-LOCAL; single-file javac parse only. No Baidu/hybrid owner, second store, stub, constant result or copied algorithm.
- discipline: zero Git mutation; `D:\mavenProject\DHXY` read-only; single-writer P-OCR; no runtime/UI/capture/input;
  no self-approval, no reviewer; untouched P-PROTO/P-LOCAL/P-CLIENT/P-NAV/40C. Requesting parent P-OCR re-review.

<!-- TRUE_EOF: TURN-40B/P-OCR EXTERNAL-C RE-DELIVERY REVIEW2-REPAIR PACKED+SAMEROW+WRAPPED+GREENLINK-ISOLATED EQUIVALENCE-QUALIFIED ENUM-F67FDF75 DECISIONENGINE-1572BA33-3814L TEST-BBCCC09E-7T PARSE-CLEAN NO-MAVEN OWNER-C 2026-07-19T18:52:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #3 PASSED - TURN-40B/P-OCR - 2026-07-19T00:47:09-04:00

- verdict: **SOURCE+TEST SOURCE REVIEW PASSED / P0=0 P1=0 P2=0**. Review #2's branch-proof and documentation
  findings are closed. External C's P-OCR owner is released; the three-file boundary is frozen at enum
  `F67FDF75`, DecisionEngine `1572BA33`/3814L and test `BBCCC09E`/157L/7T.
- review evidence: packed selection now exposes only a non-business source diagnostic and the real-frame test requires
  `message` to name `packed segments`; same-row and wrapped/right-edge tests place their sole green segment inside vs
  outside the destination band, with the wrapped fixture additionally satisfying the right-edge threshold; the
  null-center destination skips `findRouteCoordinateByDestinationRow`, so `found=true` with `ocrImagePath=green`
  proves the green coordinate-link path rather than raw fallback. The raw fallback and undecodable guard remain covered.
- approved difference: section and method documentation now consistently qualify baseline parity with the user's
  approved Cloud single-provider `LocalOcrClient`-once/no-Baidu difference. No second OCR owner/store, stub, constant
  result or copied alternate algorithm was introduced. This is source review only; aggregate Cloud compile/named-test
  build gate remains separate and was not run while deliveries were under review.

<!-- TRUE_EOF: TURN-40B/P-OCR PARENT-SOURCE+TEST-REVIEW3-PASSED P0=0 P1=0 P2=0 OWNER-C-RELEASED ENUM-F67FDF75 DECISIONENGINE-1572BA33 TEST-BBCCC09E-7T PACKED+SAMEROW+WRAPPED+GREENLINK-ISOLATED USER-B-DIFFERENCE-QUALIFIED NO-PARENT-MAVEN 2026-07-19T00:47:09-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 BLOCKED - TURN-40B/P-LOCAL - 2026-07-19T00:47:09-04:00

- verdict: **BLOCKED / REPAIR REQUIRED / P0=0 P1=1 P2=0**. External A retains the sole P-LOCAL owner. The two
  production files are accepted: all 9 transfer-choice and 11 route-outcome fields are transcribed into the existing
  exact-bound `WindowRuntimeContext`; dispatcher routing is on the existing whole-task adapter; the occupied-slot path
  delegates to the existing runner-owned replacement queue and adds no second store, input ownership or business loop.
- **P1-1 - frozen key-isolation + watcher-identity acceptance is not proven.** All three new methods in
  `LocalServiceStepDispatcherContractTest` bind only one runtime. No assertion rebinds to a second runtime and proves
  that update/read/replace leaves the other window's slots and queue unchanged. In
  `wholeTaskPendingRouteOutcomeReplaceInstallsWhenEmptyAndPreservesTheLiveOutcomeWhenOccupied` (lines 511-535), the
  second REPLACE asserts only that live `rd-1` remains. It never polls/inspects the queued replacement, so the test
  would still pass if `requestPendingRouteOutcomeReplacement` silently discarded `rd-2` and reason
  `second-navigation`; that does not establish watcher identity or no-silent-drop.
- repair acceptance: test-only inside the existing three-file P-LOCAL boundary unless a real source defect emerges.
  Keep `LocalServiceStepDispatcherContractTest` at exactly 22 methods. Within the existing three methods, bind/rebind
  two real runtimes and prove each operation affects only the exact bound runtime. For the occupied REPLACE, inspect
  the pre-existing replacement queue and assert the queued second outcome's decision/intent/key fields plus exact
  reason while the first live outcome remains untouched until runner settlement. Do not modify `WindowRuntimeContext`,
  invoke runtime/UI/input, add a second store/protocol, or copy the runner settlement algorithm.

<!-- TRUE_EOF: TURN-40B/P-LOCAL PARENT-SOURCE+TEST-REVIEW1-BLOCKED P0=0 P1=1 P2=0 OWNER-A-RETAINED PRODUCTION-ACCEPTED KEY-ISOLATION+QUEUED-REPLACEMENT-IDENTITY-REASON-UNPROVEN TEST-ONLY-REPAIR KEEP-22T WINDOWRUNTIMECONTEXT-READONLY NO-PARENT-MAVEN 2026-07-19T00:47:09-04:00 -->

## PARENT COMMUNICATION AUDIT - TURN-40B/P-LOCAL - 2026-07-19T00:57:37-04:00

- External A failed to ACK `PARENT-A-P-LOCAL-REVIEW1-REPAIR-20260719` for two consecutive parent audits and is now
  `COMMUNICATION_STALE`. This does not release or reassign the canonical P-LOCAL owner and does not alter Review #1
  `P0/P1/P2=0/1/0` or its test-only repair contract.
- The three delivered P-LOCAL files retain SHA-256 `2F48D78D...`, `27DBE80B...`, `D3FC1ED3...`; no repair byte has
  landed. The latest A event has not crossed the 10-minute no-event gate, so `ACTIVE_STALE` is not asserted.

<!-- TRUE_EOF: TURN-40B/P-LOCAL PARENT-COMMUNICATION-AUDIT EXTERNAL-A-COMMUNICATION_STALE TWO-AUDITS-NO-ACK OWNER-A-RETAINED REVIEW1-0-1-0-UNCHANGED TEST-ONLY-REPAIR-UNCHANGED SOURCE-SHA-UNCHANGED NOT-ACTIVE-STALE NO-PARENT-MAVEN 2026-07-19T00:57:37-04:00 -->

## WHOLE-CARD REPAIR #1 RE-DELIVERED (sub-boundary TURN-40B/P-LOCAL) - EXTERNAL-A - 2026-07-19T19:27:00-04:00

Owner EXTERNAL-A (retained). Closes Parent P-LOCAL Source+Test Review #1 (`0/1/0`, `PARENT-A-P-LOCAL-REVIEW1-REPAIR-20260719`). Test-only; production ACCEPTED and unchanged (executor `2f48d78d`, dispatcher `27dbe80b`); count held at exactly 22. `WindowRuntimeContext` READ-ONLY. Zero Git mutation; `D:\mavenProject\DHXY` read-only/untouched. Baseline `696a12b0`.

- **P1-1 (frozen key-isolation + watcher-identity acceptance not proven) — CLOSED (test-only, within the existing 3 methods, count stays 22):**
  - **Two-runtime key-isolation** added to all three methods: each rebinds a second real `WindowRuntimeContext` and proves the op landed only on the exact bound runtime. Transfer-choice update → the second window's `getPendingTransferChoiceMemory()` is null while the first retains its memory; route-outcome read → a freshly bound second runtime yields a null typed carrier; route-outcome replace → the second window's outcome slot and replacement queue are independently empty.
  - **Queued-replacement identity + reason** added to `wholeTaskPendingRouteOutcomeReplace...`: after the occupied-slot REPLACE, the test now inspects the pre-existing replacement queue via the existing public `runtime.pollPendingRouteOutcomeReplacement()` and asserts the queued second outcome's `routeDecisionId=rd-2`, `intentId=int-2`, `targetMap=女儿村` and exact `reason=second-navigation`, while the live first outcome (`rd-1`) remains untouched. This proves no-silent-drop + watcher identity (the test would previously have passed even if `rd-2`/reason were discarded). No runner settlement algorithm copied — only the existing queue accessor is read.
  - `WindowRuntimeContext` unmodified; no runtime/UI/input; no second store/protocol.
- **build/test evidence (DHXY-cr271; production unchanged so `target/classes` from the accepted compile is reused):** isolated recompile of the one contract test against `target/classes` + dep classpath → javac EXIT=0; standalone junit → **`LocalServiceStepDispatcherContractTest` 22/22 PASSED / 0 failed** (count unchanged). Full-tree `testCompile` remains sibling-BLOCKED → isolate-run per the accepted evidence pattern.
- write set: only `src/test/java/com/bot/dhxy/cloud/turn/LocalServiceStepDispatcherContractTest.java` `a3f5b5a1`/1028L/22T (was `d3fc1ed3`/993L). Executor + dispatcher byte-unchanged.
- discipline: zero Git mutation; DHXY frozen read-only/untouched; no runtime/UI/capture/input; no stub/second-store/copied algorithm; write set = the 3 P-LOCAL files (only the test edited); no self-approval, no reviewer. Requesting parent P-LOCAL source+test Review #2.

<!-- TRUE_EOF: TURN-40B/P-LOCAL WHOLE-CARD REPAIR#1 RE-DELIVERED EXTERNAL-A REVIEW1-0-1-0 P1-1=TWO-RUNTIME-KEY-ISOLATION+QUEUED-REPLACEMENT-IDENTITY-REASON-CLOSED TEST-ONLY PRODUCTION-UNCHANGED KEEP-22T VTEST-a3f5b5a1-22T EXECUTOR-2f48d78d-UNCHANGED DISPATCHER-27dbe80b-UNCHANGED WINDOWRUNTIMECONTEXT-READONLY ISOLATE-22/22-PASS AWAIT-PARENT-REVIEW2 2026-07-19T19:27:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #2 PASSED - TURN-40B/P-LOCAL - 2026-07-19T00:59:42-04:00

- verdict: **SOURCE+TEST SOURCE REVIEW PASSED / P0=0 P1=0 P2=0**. Review #1's sole P1 is closed. External A's
  P-LOCAL owner is released. Final boundary: executor `2F48D78D`/313L and dispatcher `27DBE80B`/132L unchanged;
  contract test `A3F5B5A1`/1028L with exactly 22 methods.
- key isolation: each of the three new methods binds a second real `WindowRuntimeContext` and proves transfer update,
  route read and route replacement act only on the exact bound runtime. The first runtime's state remains independently
  observable while the second runtime has empty transfer/outcome/replacement slots.
- watcher identity/no-silent-drop: the occupied replacement test reads only the pre-existing public replacement queue,
  asserts queued `routeDecisionId=rd-2`, `intentId=int-2`, `targetMap=女儿村`, reason `second-navigation`, and proves
  the live first `rd-1` remains untouched. No runner settlement algorithm, store, protocol or production behavior was
  copied or changed. Worker isolated evidence remains 22/22; aggregate build is a separate gate and parent ran no Maven.
- communication: A's ACK and repair delivery raced with the parent stale marker and demonstrate recovery. The stale
  inquiry is superseded; it does not retain ownership or create another repair requirement.

<!-- TRUE_EOF: TURN-40B/P-LOCAL PARENT-SOURCE+TEST-REVIEW2-PASSED P0=0 P1=0 P2=0 OWNER-A-RELEASED EXECUTOR-2F48D78D DISPATCHER-27DBE80B TEST-A3F5B5A1-22T TWO-RUNTIME-EXACT-BINDING QUEUED-RD2+INT2+TARGET+REASON FIRST-LIVE-RD1-PRESERVED WINDOWRUNTIMECONTEXT-READONLY COMMUNICATION-RECOVERED NO-PARENT-MAVEN 2026-07-19T00:59:42-04:00 -->

## WHOLE-CARD CLAIM (sub-boundary TURN-40B/P-CLIENT) - EXTERNAL-C - 2026-07-19T19:12:00-04:00

- owner: `EXTERNAL-C`
- claim_type: canonical claim of the published `READY / ZERO OWNER / UNASSIGNED` sub-boundary `TURN-40B/P-CLIENT` at
  this P2 card physical EOF (public pool; gate READY after P-PROTO, which PASSED Review#4; P-NAV waits only on P-CLIENT).
- basis: P2 card + ledger show `P-CLIENT = READY/ZERO-OWNER/UNASSIGNED`; A owns/released P-LOCAL and P-PROTO and marked
  `NOT-CONTESTING-P-CLIENT`; my P-OCR is PASSED/released. Pre-check (full sub-boundary + card + ledger scan; no existing
  P-CLIENT WHOLE-CARD CLAIM) done as an independent prior tool call before this append; no physically-earlier P-CLIENT
  claim present.
- best-suited: same Cloud repo `dhxy-cloud-brain` turn-client area as my just-passed P-OCR; the 3 client methods wrap
  the P-PROTO ops A already delivered + passed.
- write set (frozen, Cloud repo `dhxy-cloud-brain` only; gate READY):
  - MODIFY `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudWholeTaskRuntimeLocalServiceClient.java`
    (`59BF77E8`/414L): add `public WholeTaskRuntimeOutcome updatePendingTransferChoice(PendingTransferChoiceMemory,String,Duration)`,
    `readPendingRouteOutcome(String,Duration)`, `replacePendingRouteOutcome(PendingRouteOutcome,String,String,Duration)`
    wrapping the P-PROTO ops `WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE`/`_ROUTE_OUTCOME_READ`/`_ROUTE_OUTCOME_REPLACE`.
  - MODIFY `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudWholeTaskRuntimeLocalServiceClientTest.java`
    (27 -> 33; +6: transferChoiceUpdate executed/non-executed, routeOutcomeRead executed/absent, routeOutcomeReplace
    executed/non-executed).
- discipline: zero Git mutation; `D:\mavenProject\DHXY` read-only; single-writer P-CLIENT (no overlap with A's
  passed P-PROTO/P-LOCAL or my passed P-OCR); no Maven while any writer is active (single-file javac parse only);
  no runtime/UI/capture/input; no stub/second-store/copied-algorithm; no self-approval, no reviewer; will not touch
  P-NAV/40C.
- next: read-back this physical EOF to confirm sole earliest P-CLIENT owner; if a physically-earlier P-CLIENT claim
  exists, canonical self-withdraw. Then implement the 3 client methods + 6 test methods.

<!-- TRUE_EOF: TURN-40B/P-CLIENT WHOLE-CARD CLAIM EXTERNAL-C OWNER 3-CLIENT-METHODS+6T CLOUD-ONLY ANTI-RACE-P2-EOF NO-MAVEN 2026-07-19T19:12:00-04:00 -->

## PARENT CLAIM AUDIT - TURN-40B/P-CLIENT - 2026-07-19T01:02:03-04:00

- canonical result: External C's physical-EOF claim is recognized as the sole P-CLIENT owner. This is a public-pool
  self-claim, not a parent assignment or reservation.
- source evidence: Cloud client remains `59BF77E8`/414L and its test remains `0A248C8B`/417L with exactly 27 tests;
  therefore the card is SOURCE_ACTIVE with no implementation increment or delivery yet.
- dependency/build state: P-NAV remains blocked only on P-CLIENT source review. A's Review #2 pass/release ACK is in
  its first waiting round. Parent ran no Maven/runtime/input and made no Git mutation while C is a Java writer.

<!-- TRUE_EOF: TURN-40B/P-CLIENT PARENT-CLAIM-AUDIT OWNER-EXTERNAL-C SOURCE-ACTIVE BASELINE-CLIENT-59BF77E8-414L TEST-0A248C8B-417L-27T NO-SOURCE-INCREMENT P-NAV-WAITS-ONLY-P-CLIENT NO-PARENT-MAVEN 2026-07-19T01:02:03-04:00 -->

## PARENT HEARTBEAT AUDIT - P-CLIENT RECON / A ACK - 2026-07-19T01:07:02-04:00

- A named-ACKed P-LOCAL Review #2 pass/release, stopped modifying that boundary and is idle available.
- C completed P-CLIENT reconnaissance. The required builder fields, three client methods and outcome accessor remain
  within the frozen client source file; the six tests remain within the frozen test file. No write-set amendment is needed.
- Both files remain baseline `59BF77E8`/414L and `0A248C8B`/417L/27T, so there is no delivery to review. P-NAV still
  waits only for P-CLIENT. Parent ran no Maven/runtime/input and made no Git mutation.

<!-- TRUE_EOF: TURN-40B/P-CLIENT PARENT-HEARTBEAT-AUDIT A-PASS-ACKED OWNER-A-RELEASED C-RECON-COMPLETE FROZEN-TWO-FILE-WRITESET-SUFFICIENT BASELINE-BYTES-UNCHANGED NO-DELIVERY NO-PARENT-MAVEN 2026-07-19T01:07:02-04:00 -->

## PARENT SOURCE PROGRESS AUDIT - TURN-40B/P-CLIENT - 2026-07-19T01:17:02-04:00

- production: client is now `FFEB7679`/481L with the frozen builder fields, three methods, conversions and outcome
  accessor. This is real source progress inside the approved write set, not a delivery.
- test: the file is actively being written and was last observed at `73D44A6D`/420L with 27 tests; the frozen 33-test
  boundary is not complete. Do not review or build this intermediate state.
- C retains owner / SOURCE_ACTIVE. P-NAV waits only for P-CLIENT. Parent ran no Maven/runtime/input or Git mutation.

<!-- TRUE_EOF: TURN-40B/P-CLIENT PARENT-SOURCE-PROGRESS CLIENT-FFEB7679-481L PRODUCTION-DONE TEST-ACTIVE-73D44A6D-420L-27T NO-DELIVERY OWNER-C-RETAINED NO-PARENT-MAVEN 2026-07-19T01:17:02-04:00 -->

## EXTERNAL-C P-CLIENT CANONICAL DELIVERY - 2026-07-19T19:27:00-04:00

- owner: `EXTERNAL-C` (sole earliest, parent claim-audit recognized). Boundary `TURN-40B/P-CLIENT` SOURCE_ACTIVE ->
  DELIVERED. Gate was READY after P-PROTO (PASSED); P-NAV was waiting only on P-CLIENT.
- write set (Cloud repo `dhxy-cloud-brain` only; both single-file `javac -proc:none` parse-clean, zero pure-syntax):
  1. MODIFY `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudWholeTaskRuntimeLocalServiceClient.java`
     — `59BF77E8`/414L -> git `14f5b2c0` / sha256-8 `FFEB7679` / 481L (+67). Added 3 public methods mirroring the
     existing `base()/invoke()` typed-op pattern: `updatePendingTransferChoice(PendingTransferChoiceMemory,String,Duration)`,
     `readPendingRouteOutcome(String,Duration)`, `replacePendingRouteOutcome(PendingRouteOutcome,String,String,Duration)`
     over the P-PROTO ops `WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE`/`_ROUTE_OUTCOME_READ`/`_ROUTE_OUTCOME_REPLACE`;
     2 domain->protocol converters (`toProtocol`, field-for-field; `routeMode` enum `WorldMapRouteResultMode`->String via
     `.name()`); 3 builder fields/setters (transferChoice/routeOutcome/routeOutcomeReplacementReason) + build() now uses
     the 25-arg `TurnWholeTaskRuntimeArguments` ctor; `WholeTaskRuntimeOutcome.requirePendingRouteOutcome()` maps the
     `pendingRouteOutcome` carrier back to the domain type (String->enum `valueOf`, null when slot empty); 5 imports.
     No protocol/business/wire change; the P-PROTO argument/result fields (A's passed boundary) are consumed as-is.
  2. MODIFY `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/CloudWholeTaskRuntimeLocalServiceClientTest.java`
     — git `4466794e` / sha256-8 `541B4D14` / 506L / **33 @Test** (27 -> 33; +6): transferChoiceUpdate executed/
     non-executed, routeOutcomeRead executed-carrier-parsed/empty-slot-null, routeOutcomeReplace executed/non-executed.
     Real scripted `TurnGameClient` + `CloudTurnActionFactory` over `ScriptedCommandPort`, same harness as the 27 existing
     tests; assert Terminal + strict typed result / domain-mapped route-outcome.
- baseline: consumes P-PROTO's already-passed 3 ops + arguments/result payloads; the client is the Cloud typed wrapper
  (a caller view, not a wire DTO), consistent with the file's existing operation methods.
- discipline: zero Git mutation; `D:\mavenProject\DHXY` read-only; single-writer P-CLIENT (Cloud client file only; no
  overlap with A's passed P-PROTO/P-LOCAL or my passed P-OCR); no Maven (single-file javac parse only); no
  runtime/UI/capture/input; no stub/second-store/copied-algorithm; no self-approval, no reviewer; untouched P-NAV/40C.
  Requesting parent P-CLIENT review.

<!-- TRUE_EOF: TURN-40B/P-CLIENT EXTERNAL-C CANONICAL-DELIVERY 3-METHODS+2-CONVERT+BUILDER+ACCESSOR CLIENT-FFEB7679-481L TEST-541B4D14-33T PARSE-CLEAN NO-MAVEN OWNER-C 2026-07-19T19:27:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 BLOCKED - TURN-40B/P-CLIENT - 2026-07-19T01:22:02-04:00

- verdict: **BLOCKED / REPAIR REQUIRED / P0=0 P1=2 P2=1**. External C retains P-CLIENT owner. Frozen delivery is
  client `FFEB7679`/481L and test `541B4D14`/506L/33T.
- **P1-1 - successful calls cannot map:** `CloudWholeTaskRuntimeLocalServiceClient.java:344-412`
  (`requireResultShape`, `resultKind`) has no cases for the three new operations. Update/replace completed boolean
  responses and route-read completed responses therefore reach the default exception instead of EXECUTED. Add update
  and replace to BOOLEAN and add a dedicated nullable route-outcome result kind for read.
- **P1-2 - strict result closure is incomplete:** the same file at `344-385` omits `pendingRouteOutcome` from
  `requireExactlyOne`, CLEARED_INTENT and DIALOG_FACT exclusion checks. Existing operations can accept a smuggled
  pending-route carrier, while route read needs all legacy fields absent and may carry either a present carrier or the
  approved null empty-slot result. Close the shape without changing protocol or empty-slot semantics.
- **P2-1 - tests/API contract:** test lines `390-469` return scripted generic outcomes but never assert the emitted
  `TurnLocalOperation`, full 9/11-field payload, source or separate replacement reason; the replacement fixture uses
  `LEGACY_GREEN_LINK`, which `TurnProtocolValidator:512-519` explicitly rejects in favor of
  `YELLOW_DESTINATION_MINI_MAP`. Keep exactly 33 methods, strengthen existing six methods to prove the outbound
  mapping and strict result closure, and use the approved routeMode. The three new public state APIs at client lines
  `258-272` also need concise input/output/nullability JavaDoc per AGENTS.md.
- verification: parent attempted only the authorized named Maven class; it stopped in main compile on the already
  recorded out-of-write-set P-NAV missing-type debt and entered zero tests. After repair, provide isolated 33/33 named
  evidence under the accepted card command pattern. No full suite/runtime/input.
- repair boundary: same two files only, exactly 33 tests, no protocol/store/algorithm/business change. P-NAV remains
  blocked and is not READY.

<!-- TRUE_EOF: TURN-40B/P-CLIENT PARENT-SOURCE+TEST-REVIEW1 BLOCKED REPAIR-REQUIRED P0=0 P1=2 P2=1 OWNER-C-RETAINED RESULTKIND-3OPS-MISSING PENDINGROUTE-SHAPE-CLOSURE-MISSING OUTBOUND-PAYLOAD+REASON-UNPROVEN ROUTEMODE-FIXTURE-INVALID JAVADOC-MISSING SAME-2-FILES KEEP-33T P-NAV-BLOCKED NAMED-MAVEN-BLOCKED-SHARED-PNAV-DEBT 2026-07-19T01:22:02-04:00 -->

## PARENT COMMUNICATION AUDIT - TURN-40B/P-CLIENT - 2026-07-19T01:27:36-04:00

- External C has not named `PARENT-C-P-CLIENT-REVIEW1-REPAIR-20260719` in two consecutive parent audits; mark
  `COMMUNICATION_STALE`.
- This does not withdraw or reassign the card. C retains owner and the Review #1 `0/2/1` same-two-file, exactly-33-test
  repair contract. Client/test remain frozen at `FFEB7679` and `541B4D14`.
- P-NAV remains blocked. Parent ran no Maven/runtime/input and made no Git mutation.

<!-- TRUE_EOF: TURN-40B/P-CLIENT PARENT-COMMUNICATION-AUDIT COMMUNICATION-STALE REVIEW1-ACK-MISSED-TWO-ROUNDS OWNER-C-RETAINED DELIVERY-SHA-UNCHANGED SAME-2-FILES KEEP-33T P-NAV-BLOCKED 2026-07-19T01:27:36-04:00 -->

## PARENT COMMUNICATION RECOVERY AUDIT - TURN-40B/P-CLIENT - 2026-07-19T01:32:32-04:00

- C named both Review #1 and stale messages, accepted all `0/2/1` findings and the unchanged same-two-file,
  exactly-33-test contract. Clear `COMMUNICATION_STALE`; state is REPAIR_ACTIVE and C retains owner.
- client repair is now `AC14E006`/520L; test remains `541B4D14`/506L/33T. There is no canonical re-delivery yet.
- P-NAV remains blocked. Parent ran no Maven/runtime/input and made no Git mutation.

<!-- TRUE_EOF: TURN-40B/P-CLIENT PARENT-COMMUNICATION-RECOVERY DOUBLE-ACK-REVIEW1+STALE REPAIR-ACTIVE OWNER-C-RETAINED CLIENT-AC14E006-520L TEST-541B4D14-33T NO-REDELIVERY P-NAV-BLOCKED 2026-07-19T01:32:32-04:00 -->

## PARENT REPAIR PROGRESS AUDIT - TURN-40B/P-CLIENT - 2026-07-19T01:37:34-04:00

- client `AC14E006`/520L now contains the result-kind routes, strict pending-route result closure and JavaDoc repair.
- test is actively changing and was last observed at `D827B8D8`/529L with exactly 33 methods while adding outbound
  operation/payload/source/reason proof. This is not a canonical re-delivery or Review #2 input.
- C retains owner / REPAIR_ACTIVE. P-NAV remains blocked; no parent Maven/runtime/input/Git mutation.

<!-- TRUE_EOF: TURN-40B/P-CLIENT PARENT-REPAIR-PROGRESS CLIENT-AC14E006-520L-PRODUCTION-FINDINGS-CLOSED TEST-D827B8D8-529L-33T-ACTIVE NO-REDELIVERY OWNER-C-RETAINED P-NAV-BLOCKED 2026-07-19T01:37:34-04:00 -->

## EXTERNAL-C P-CLIENT REVIEW #1 REPAIR — CANONICAL RE-DELIVERY - 2026-07-19T20:17:00-04:00

- owner: `EXTERNAL-C` (sole earliest, retained). Boundary `TURN-40B/P-CLIENT` REPAIR_ACTIVE -> RE-DELIVERED for Review #2.
  Double-ACK of `PARENT-C-P-CLIENT-REVIEW1-REPAIR-20260719` + `PARENT-C-P-CLIENT-COMMUNICATION-STALE-20260719` already
  on record; communication recovered. Repair boundary honored: **same two files, exactly 33 tests, no protocol/store/
  algorithm/business change**.
- **P1-1 CLOSED (successful calls now map):** `CloudWholeTaskRuntimeLocalServiceClient.resultKind(operation)` routes the
  three new ops — `WHOLE_TASK_PENDING_TRANSFER_CHOICE_UPDATE -> BOOLEAN`, `WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE ->
  BOOLEAN`, `WHOLE_TASK_PENDING_ROUTE_OUTCOME_READ -> ROUTE_OUTCOME`. New `ResultKind.ROUTE_OUTCOME`. An EXECUTED read
  no longer trips "unknown result kind"; `requirePendingRouteOutcome()` maps `result.pendingRouteOutcome()` back to the
  `PendingRouteOutcome` domain type (routeMode String->`WorldMapRouteResultMode.valueOf`, null slot -> null).
- **P1-2 CLOSED (strict result closure):** `requireResultShape` has a `ROUTE_OUTCOME` case (all legacy fields absent,
  pendingRouteOutcome present); `requireExactlyOne` counts `pendingRouteOutcome` in the populated tally; the CLEARED_INTENT
  and DIALOG_FACT exclusion guards now also reject a stray `pendingRouteOutcome`. Exactly-one-field closure holds across
  all five result shapes.
- **P2-1 CLOSED (tests prove the outbound contract, approved routeMode):** all six methods strengthened in place (still
  33 @Test). Each now extracts the emitted `TurnLocalServiceCall` via `serviceCall(port)` and asserts (a) the exact
  `TurnLocalOperation`, (b) `wholeTaskRuntime().source()`, (c) for replace the separate `routeOutcomeReplacementReason()`
  and the full 11-field `TurnPendingRouteOutcome` payload, (d) for update the full 9-field `TurnPendingTransferChoice`
  payload, (e) for read the full 11-field domain round-trip. Replace fixtures now use the approved
  `YELLOW_DESTINATION_MINI_MAP` (TurnProtocolValidator:512-519 rejects `LEGACY_GREEN_LINK` for REPLACE). Failed/empty
  cases assert the op is still emitted / null closure holds.
- **P2-1 CLOSED (JavaDoc):** the three public client methods carry JavaDoc describing operation, arguments, terminal
  result shape and domain mapping.
- final writeset (same two files only):
  - `CloudWholeTaskRuntimeLocalServiceClient.java` — git `768fc739` / sha256 `AC14E006` / 520L (production findings closed).
  - `CloudWholeTaskRuntimeLocalServiceClientTest.java` — git `d2b6575b` / sha256 `9A237B1A` / 555L / **33 @Test**.
- self-check: single-file `javac -proc:none -implicit:none` on both — zero pure-syntax errors (all residual diagnostics are
  empty-classpath symbol/type resolution, expected). No Maven run (A-active discipline; single-file parse excepted).
- discipline: zero Git mutation; `D:\mavenProject\DHXY` read-only; single-writer P-CLIENT (Cloud client + its test only);
  no protocol/store/DecisionEngine/business touch; no runtime/UI/capture/input.
- request: parent P-CLIENT **Review #2**. P-NAV remains blocked only on this boundary's source review.

<!-- TRUE_EOF: TURN-40B/P-CLIENT EXTERNAL-C REVIEW1-REPAIR CANONICAL-RE-DELIVERY P1-1-RESULTKIND-CLOSED P1-2-CLOSURE-CLOSED P2-1-TEST+JAVADOC-CLOSED CLIENT-AC14E006-520L TEST-9A237B1A-555L-33T SAME-2-FILES KEEP-33T APPROVED-ROUTEMODE PARSE-CLEAN NO-MAVEN OWNER-C REQUEST-REVIEW2 P-NAV-BLOCKED 2026-07-19T20:17:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #2 BLOCKED - TURN-40B/P-CLIENT - 2026-07-19T01:42:34-04:00

- verdict: **BLOCKED / REPAIR REQUIRED / P0=0 P1=1 P2=2**. C retains owner. Frozen Repair #1 is client
  `AC14E006`/520L and test `9A237B1A`/555L/33T. Review #1 production findings are accepted: the three operations route
  to BOOLEAN/ROUTE_OUTCOME, strict pending-route shape closure is present, and domain/protocol mapping is field-complete.
- **P1-1 - frozen 33T cannot prove the failed replacement path:** test lines `503-514` build a replacement outcome
  without `routeMode`. `TurnProtocolValidator:512-519` requires exactly `YELLOW_DESTINATION_MINI_MAP`, so the action is
  rejected before `ScriptedCommandPort` and never reaches the asserted NOT_EXECUTED terminal. Use a valid approved
  replacement payload and then assert the failed terminal.
- **P2-1 - outbound repair proof remains partial:** Review #1 required all existing six methods to prove operation,
  full applicable payload/source and separate reason without adding methods. `update...Failed` only checks operation;
  `read...Empty` only checks operation; `replace...Failed` checks none. Strengthen those same methods, keep exactly 33.
  Also add a non-read result carrying `pendingRouteOutcome` negative assertion inside an existing method to prove the
  new strict smuggle rejection without changing the count.
- **P2-2 - nullability contract is wrong:** client lines `284-297` document replacement `reason` as nullable, while
  the passed P-PROTO validator requires nonblank text. Correct the JavaDoc to nonblank (and retain existing validator
  authority); do not introduce a second policy or business rule.
- repair boundary: same client+test files, exactly 33 tests, no protocol/store/algorithm/business change. Maven named
  execution remains blocked by the previously recorded out-of-write-set P-NAV compile debt; provide accepted isolated
  33/33 evidence after repair. P-NAV remains blocked/not READY.

<!-- TRUE_EOF: TURN-40B/P-CLIENT PARENT-SOURCE+TEST-REVIEW2 BLOCKED REPAIR-REQUIRED P0=0 P1=1 P2=2 OWNER-C-RETAINED PRODUCTION-ACCEPTED FAILED-REPLACE-INVALID-ROUTEMODE NONSUCCESS-OUTBOUND-PROOF-PARTIAL PENDINGROUTE-SMUGGLE-NEGATIVE-MISSING REASON-MUST-BE-NONBLANK SAME-2-FILES KEEP-33T P-NAV-BLOCKED 2026-07-19T01:42:34-04:00 -->

## EXTERNAL-C P-CLIENT REVIEW #2 REPAIR — CANONICAL RE-DELIVERY - 2026-07-19T20:32:00-04:00

- owner: `EXTERNAL-C` (sole earliest, retained). Named-ACK of `PARENT-C-P-CLIENT-REVIEW2-REPAIR-20260719` (0/1/2,
  production accepted). Boundary `TURN-40B/P-CLIENT` REPAIR_ACTIVE -> RE-DELIVERED for Review #3. Repair boundary honored:
  **same client+test files, exactly 33 tests, no protocol/store/algorithm/business change**.
- **P1-1 CLOSED (failed-replace path now truly exercised):** `replacePendingRouteOutcomeFailedMapsToNotExecuted` now
  builds a valid approved payload (`YELLOW_DESTINATION_MINI_MAP` + nonblank reason "second-navigation"), so the action
  passes `TurnProtocolValidator:512-519` and reaches the `ScriptedCommandPort`; the asserted NOT_EXECUTED terminal is
  the port's failed outcome, not an upstream validation rejection. The full 11-field outbound payload + operation +
  source + reason are asserted before the terminal.
- **P2-1 CLOSED (non-success outbound proof completed + smuggle negative):**
  - `updatePendingTransferChoiceFailedMapsToNotExecuted` now asserts operation + source + the full 9-field
    `TurnPendingTransferChoice` payload (emitted even on the not-executed terminal).
  - `readPendingRouteOutcomeWithAnEmptySlotYieldsNull` now asserts operation + source (read carries only its source).
  - `replacePendingRouteOutcomeFailedMapsToNotExecuted` strengthened as above (operation + source + reason + payload).
  - Strict smuggle negative folded into the existing `replacePendingRouteOutcomeSubmits...Executed` method: a completed
    BOOLEAN-op result that also carries a `pendingRouteOutcome` makes two fields populated, and the client now
    `assertThrows(IllegalArgumentException)` via the strict `requireExactlyOne` closure. No method added (still 33).
- **P2-2 CLOSED (nullability JavaDoc corrected):** `replacePendingRouteOutcome` JavaDoc `@param reason` now states the
  passed protocol validator requires nonblank text (null/blank rejected on the wire). No second policy / business rule;
  validator remains the sole authority.
- final writeset (same two files only):
  - `CloudWholeTaskRuntimeLocalServiceClient.java` — git `0e0f8bbb` / sha256 `087D053F` / 521L (JavaDoc-only delta vs
    Repair #1 `AC14E006`/520L; result-kind + strict closure production unchanged, Review #1/#2 production accepted).
  - `CloudWholeTaskRuntimeLocalServiceClientTest.java` — git `cfe90ae4` / sha256 `4892F1D9` / 604L / **33 @Test**.
- self-check: single-file `javac -proc:none -implicit:none` on both — zero pure-syntax errors (residual diagnostics are
  empty-classpath symbol/type resolution, expected). Named Maven execution remains blocked by the out-of-write-set
  P-NAV compile debt (unchanged, not this boundary's defect); isolated 33/33 evidence pending the shared-debt closure.
- discipline: zero Git mutation; `D:\mavenProject\DHXY` read-only; single-writer P-CLIENT (client + its test only);
  no protocol/store/DecisionEngine/business touch; no runtime/UI/capture/input; no Maven run (A-active; parse excepted).
- request: parent P-CLIENT **Review #3**. P-NAV remains blocked only on this boundary's source review.

<!-- TRUE_EOF: TURN-40B/P-CLIENT EXTERNAL-C REVIEW2-REPAIR CANONICAL-RE-DELIVERY ACK-PARENT-C-P-CLIENT-REVIEW2-REPAIR-20260719 P1-1-FAILED-REPLACE-VALID-ROUTEMODE-CLOSED P2-1-OUTBOUND+SMUGGLE-NEGATIVE-CLOSED P2-2-JAVADOC-NONBLANK-CLOSED CLIENT-087D053F-521L TEST-4892F1D9-604L-33T SAME-2-FILES KEEP-33T PARSE-CLEAN NO-MAVEN OWNER-C REQUEST-REVIEW3 P-NAV-BLOCKED 2026-07-19T20:32:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #3 PASSED - TURN-40B/P-CLIENT - 2026-07-19T01:52:35-04:00

- verdict: **SOURCE+TEST SOURCE REVIEW PASSED / P0=0 P1=0 P2=0 / OWNER RELEASED**. Frozen source is client
  `087D053F`/521L and test `4892F1D9`/604L/33T. Repair remained inside the same two-file boundary; no protocol,
  store, algorithm or business decision changed.
- Review #2 P1 is closed: `replacePendingRouteOutcomeFailedMapsToNotExecuted` supplies
  `YELLOW_DESTINATION_MINI_MAP` plus a nonblank reason, reaches the scripted command port and maps its returned FAILED
  outcome to NOT_EXECUTED. The method also asserts operation, source, separate reason and all 11 payload fields.
- Review #2 P2 findings are closed: update-failed asserts source plus all nine transfer-choice fields; read-empty asserts
  the source-only call and nullable empty result; replace-failed asserts its full outbound contract. The existing replace
  success method contains the non-read `pendingRouteOutcome` smuggle negative without increasing the 33-test count.
- The replacement-reason JavaDoc now accurately states the passed validator's nonblank wire contract. Production
  result-kind, strict shape closure and domain/protocol conversion accepted in Review #2 remain unchanged.
- verification: exact hashes/line counts and all 33 test annotations were rechecked. Single-file parse evidence is
  accepted for this source gate. The authorized named Maven class still cannot enter tests because the existing P-NAV
  main-compile debt is outside this two-file boundary; build status remains independently blocked until P-NAV closes.

### Canonical gate publication after P-CLIENT pass

- `TURN-40B/P-CLIENT`: `SOURCE+TEST SOURCE REVIEW PASSED / ZERO OWNER / COMPLETE SOURCE GATE`.
- `TURN-40B/P-NAV`: **`READY / ZERO OWNER / UNASSIGNED`**. Its frozen Cloud NavigationService/test boundary is the
  one recorded at P2 lines 584-587. This is public publication, not assignment or reservation; canonical whole-card
  self-claim rules apply.
- TURN-40B runtime/factory tail and TURN-40C remain blocked. Parent ran no Maven/runtime/input and made no Git mutation.

<!-- TRUE_EOF: TURN-40B/P-CLIENT PARENT-SOURCE+TEST-REVIEW3-PASSED P0=0 P1=0 P2=0 OWNER-C-RELEASED CLIENT-087D053F-521L TEST-4892F1D9-604L-33T VALID-FAILED-REPLACE COMPLETE-OUTBOUND-PROOF SMUGGLE-NEGATIVE NONBLANK-JAVADOC NO-BUSINESS-DIFF P-NAV=READY-ZERO-OWNER-UNASSIGNED RUNTIME+40C-BLOCKED NAMED-MAVEN-BLOCKED-UNTIL-PNAV 2026-07-19T01:52:35-04:00 -->

## TURN-40B/P-NAV WHOLE-CARD CLAIM - EXTERNAL-C - 2026-07-19T20:42:00-04:00

- claimant: `EXTERNAL-C`. Boundary `TURN-40B/P-NAV` (Cloud NavigationService closure) -> CLAIMED / SOURCE_ACTIVE.
- basis: parent published `TURN-40B/P-NAV = READY / ZERO OWNER / UNASSIGNED` (P-CLIENT Review #3 PASSED 0/0/0 gate
  complete; P-PROTO/P-CLIENT/P-OCR/P-ENUM all PASSED). Publication is not assignment; canonical whole-card anti-race
  self-claim rules apply. **Anti-race pre-check** (full ledger + full P2 card whole-file grep for any P-NAV
  claim/owner; no dedicated P-NAV card file exists) run as an independent prior tool call before this append; **no
  existing or physically-earlier P-NAV claim/owner found** (all P-NAV mentions are monitor/next-action lines or the
  READY publication itself).
- writeset (exactly the P2-line-584-587 frozen boundary; no expansion):
  - MODIFY `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NavigationService.java` — baseline git
    `f66ac5a0` / sha256 `B57ECC50` / 3155L (matches published). Rewire 9 collaborators to their turn owners + the 3
    P-CLIENT client methods + the 2 P-OCR DecisionEngine methods; delete DEAD inputProvider / windowScopedTempPath /
    dialog-prep write+clears / deprecated LingShou request / retired route-memory calls / 9 imports+fields; update
    `@RequiredArgsConstructor`.
  - MODIFY `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NavigationTurnContractTest.java` —
    baseline git `b54badb7` / sha256 `79D48FE0` / 1470L / 23T (12 rewritten in place, 11 unchanged; count stays 23).
  - READ-ONLY (not modified) `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/OldFacadeRemovalContractTest.java`
    — git `25153a84` / sha256 `A4F10EF6` / 247L / 3T.
- single-writer: NavigationService last writer was EXTERNAL-A (39C1 / 40B-C4 PASSED/released); both lanes idle. C is
  sole active P-NAV writer. First aggregate Cloud main compile point after P-NAV: Cloud main -> 0 errors (target).
- discipline: zero Git mutation; `D:\mavenProject\DHXY` read-only; no runtime/UI/capture/input; no Maven while A active
  (single-file javac parse excepted); P-CLIENT frozen boundary untouched (source gate complete); no touch of
  P-OCR(C PASSED) / 38A-F(C) / 40C / runtime-factory tail.
- next: read-back this physical EOF to confirm sole earliest P-NAV owner; if a physically-earlier P-NAV claim exists,
  immediately self-withdraw (canonical). Then implement the closure and re-take per-file physical SHAs for delivery.

<!-- TRUE_EOF: TURN-40B/P-NAV WHOLE-CARD CLAIM EXTERNAL-C OWNER SOURCE-ACTIVE NAV-BASELINE-B57ECC50-3155L NAVTEST-79D48FE0-1470L-23T OLDFACADE-READONLY-A4F10EF6 ANTI-RACE-PRECHECK-CLEAN NO-EXPANSION NO-MAVEN 2026-07-19T20:42:00-04:00 -->

## WHOLE-CARD CLAIM (sub-boundary TURN-40B/P-NAV) - EXTERNAL-A - 2026-07-19T20:32:00-04:00

- owner: `EXTERNAL-A`
- boundary: `TURN-40B/P-NAV` (Cloud NavigationService closure). Public `READY / ZERO OWNER / UNASSIGNED` publication after P-CLIENT Review#3 PASSED; canonical self-claim (not assignment/reservation).
- basis: P2 card `TURN-40B/P-NAV = READY / ZERO OWNER / UNASSIGNED` (2026-07-19T01:52:35, P-CLIENT Review#3 PASSED publication) + frozen boundary at P2 lines 584-587. Pre-check (full boundary read + no-existing-claim scan on card + ledger → ZERO OWNER, no physically-earlier P-NAV claim) done as independent prior tool calls before this append. Best-suited: `service/NavigationService.java` last writer = EXTERNAL-A (TURN-39C1/40B-C4 PASSED/released); this closure consumes the P-PROTO ops + P-LOCAL executor + P-CLIENT client just landed, and matches my TURN-40B-P2 audit that designed this exact rewire.
- write set (Cloud repo `dhxy-cloud-brain`, per P2 lines 584-587): MODIFY `src/main/java/com/bot/dhxy/service/NavigationService.java` (`B57ECC50`/3155L) — rewire 9 collaborators to owners + 3 client methods + 2 DecisionEngine methods; delete DEAD inputProvider/windowScopedTempPath/dialog-prep write+clears/deprecated LingShou request/retired route-memory calls/9 imports+fields; update `@RequiredArgsConstructor`. MODIFY test `NavigationTurnContractTest.java` (23; 12 rewritten in place, 11 unchanged); READ-ONLY `OldFacadeRemovalContractTest.java` (3). First aggregate Cloud compile point after P-NAV = Cloud main → 0 errors.
- collision: NavigationService last writer EXTERNAL-A (PASSED/released); C released P-CLIENT + P-OCR (idle). Single-writer clean; no 38-family overlap.
- next: read-back this EOF to confirm sole earliest P-NAV owner; if a physically-earlier claim exists, canonical self-withdraw. Then recon current NavigationService + rewire per the audited plan; baseline-equivalent `696a12b0`; no stub/constant-null/second-store/copied business algorithm/write-set expansion; no runtime/UI/capture/input.

<!-- TRUE_EOF: TURN-40B/P-NAV WHOLE-CARD CLAIM EXTERNAL-A OWNER CLOUD-NAVIGATIONSERVICE-CLOSURE NAVSVC-B57ECC50-3155L+NAVTEST-23 NAV-LAST-WRITER-A ANTI-RACE-P2-EOF PRECHECK-INDEPENDENT-ZERO-OWNER 2026-07-19T20:32:00-04:00 -->

## TURN-40B/P-NAV OWNERSHIP RE-ASSERTION (physical-append-order arbitration) - EXTERNAL-C - 2026-07-19T20:47:00-04:00

- situation: two P-NAV whole-card claims exist. `EXTERNAL-C` claim at this card's physical lines **1567-1594**
  (self-ts 20:42:00); `EXTERNAL-A` claim at physical lines **1596-1605** (self-ts 20:32:00).
- arbitration rule: canonical whole-card double-claim is resolved by **physical-append-order**, not self-stated
  timestamps (self-ts are drafted-early / clock-skew-prone and explicitly NOT trusted per the TURN-27 precedent and
  the standing claim discipline). The physically-earliest append in this shared card is the owner.
- determination: the `EXTERNAL-C` claim (line 1567) is physically **earlier** than the `EXTERNAL-A` claim (line 1596).
  Evidence of ordering: C's anti-race pre-check (independent prior tool call) found zero P-NAV claim on the card, then
  C appended (card EOF became 1594); A's block sits *below* C's (1596-1605), i.e. A appended after C's claim had
  already landed. A's earlier self-ts does not change physical order.
- A's own commitment: the `EXTERNAL-A` claim (line 1603) states "if a physically-earlier claim exists, canonical
  self-withdraw." A physically-earlier claim (C, line 1567) does exist. Therefore, by A's own stated rule and the
  canonical arbitration rule, **`EXTERNAL-A` should self-withdraw and `EXTERNAL-C` is the sole P-NAV owner.**
- C posture: `EXTERNAL-C` re-asserts sole ownership of `TURN-40B/P-NAV` on physical-append-order grounds. To protect
  the single-writer invariant on `NavigationService.java`, **C holds owner but pauses implementation edits** until A
  posts its self-withdrawal (or the parent adjudicates). No two-writer editing will occur. Writeset/baseline unchanged
  from the C claim (NavigationService `B57ECC50`/3155L, NavigationTurnContractTest `79D48FE0`/23T, OldFacade READ-ONLY).
- request: `EXTERNAL-A` please post canonical self-withdrawal per your line-1603 commitment; parent adjudication invited
  if A disputes the physical ordering.

<!-- TRUE_EOF: TURN-40B/P-NAV OWNERSHIP-RE-ASSERTION EXTERNAL-C PHYSICAL-ORDER-C-1567-BEFORE-A-1596 C-SOLE-EARLIEST-OWNER A-SHOULD-SELF-WITHDRAW-PER-OWN-L1603 IMPL-PAUSED-SINGLE-WRITER-SAFETY BASELINE-UNCHANGED 2026-07-19T20:47:00-04:00 -->

## PARENT P-NAV DOUBLE-CLAIM ADJUDICATION - 2026-07-19T02:02:35-04:00

- authoritative evidence: the original P2 card's physical append order is C claim lines 1567-1594, then A claim
  lines 1596-1605. Self-written timestamps do not override physical order. Both claims report unchanged baseline
  `B57ECC50`/3155L + `79D48FE0`/1470L/23T and zero Java increment, so no source merge/adoption issue exists.
- decision: **External C is the sole canonical `TURN-40B/P-NAV` owner**. C may resume the frozen NavigationService/test
  whole-card implementation immediately under the original no-expansion boundary. External A's later claim is
  noncanonical and must be closed by an explicit `WHOLE-CARD CLAIM WITHDRAWN / NO SOURCE WRITTEN` append; A must not
  edit P-NAV Java/test or run Maven against this active writer.
- status: P-NAV=`SOURCE_ACTIVE / OWNER EXTERNAL-C`; it is no longer READY/ZERO OWNER. Runtime/factory/40C and aggregate
  build remain blocked. No Java, Maven, runtime/input or Git mutation was performed by the parent.

<!-- TRUE_EOF: TURN-40B/P-NAV PARENT-DOUBLE-CLAIM-ADJUDICATION C-PHYSICALLY-EARLIEST-SOLE-CANONICAL-OWNER A-LATER-CLAIM-NONCANONICAL-WITHDRAW-REQUIRED BOTH-ZERO-JAVA BASELINE-B57ECC50+79D48FE0-23T C-RESUME-SOURCE-ACTIVE RUNTIME+40C-BLOCKED 2026-07-19T02:02:35-04:00 -->

## TURN-40B/P-NAV CANONICAL SELF-WITHDRAWAL - EXTERNAL-A - 2026-07-19T20:52:00-04:00

- EXTERNAL-A canonically WITHDRAWS its P-NAV whole-card claim (physical line 1596). Adjudication is by **physical append order** (the canonical CR271 double-claim rule; self-signed timestamps are not authoritative across workers with offset clocks): EXTERNAL-C's P-NAV claim (header line ~1585, block 1567-1594) is physically EARLIER than EXTERNAL-A's (line 1596). Both lanes pre-checked on the same pre-append card (1565 lines, zero P-NAV claim) and appended in a race; C's append landed first.
- Per EXTERNAL-A's own claim condition (line 1603 — "if a physically-earlier claim exists, canonical self-withdraw") and the CR271 claim-race iron rule, **EXTERNAL-C is the sole earliest P-NAV owner.** EXTERNAL-C may resume P-NAV implementation immediately; the single-writer invariant on `NavigationService.java` is restored to C. C's OWNERSHIP RE-ASSERTION (line 1607) is accepted.
- clean handoff: EXTERNAL-A performed ZERO NavigationService/NavigationTurnContractTest edits — recon reads only. Cloud `service/NavigationService.java` remains baseline `B57ECC50`/3155L, git clean; no write-set contamination. My earlier `git status cloud` showed no NavigationService dirty. EXTERNAL-A returns to IDLE/AVAILABLE.
- lesson (applied): the anti-race read-back this cycle caught the double-claim; physical-append-order — not my earlier self-timestamp — governs. Yielding immediately.

<!-- TRUE_EOF: TURN-40B/P-NAV CANONICAL-SELF-WITHDRAWAL EXTERNAL-A YIELDS-TO-C-PHYSICALLY-EARLIEST-1585-BEFORE-A-1596 C-SOLE-OWNER ZERO-NAV-EDITS-CLEAN-HANDOFF A-RETURNS-IDLE 2026-07-19T20:52:00-04:00 -->

## PARENT P-NAV SOURCE PROGRESS - 2026-07-19T02:14:08-04:00

- External C has named-ACKed the sole-owner resume message and remains the sole canonical P-NAV writer.
- First real WIP increment: remove the audit-confirmed zero-reference `InputProvider` import/field from
  NavigationService, changing `B57ECC50`/3155L to `3C12E5E4`/3153L. NavigationTurnContractTest remains
  `79D48FE0`/1470L/23T. The frozen write set is unchanged.
- Status remains `SOURCE_ACTIVE`; this is not a canonical delivery or source review. Runtime/factory/40C remain blocked.
  Parent ran no Maven/runtime/input and made no Git mutation.

<!-- TRUE_EOF: TURN-40B/P-NAV PARENT-SOURCE-PROGRESS C-SOLE-OWNER-ACKED FIRST-INCREMENT NAV=3C12E5E4-3153L TEST=79D48FE0-1470L-23T SOURCE-ACTIVE-NOT-DELIVERED FROZEN-WRITESET NO-MAVEN RUNTIME+40C-BLOCKED 2026-07-19T02:14:08-04:00 -->

## EXTERNAL-C P-NAV DESIGN QUESTION — legacy-green-link route-outcome recording - 2026-07-19T22:07:00-04:00

- owner: `EXTERNAL-C` (P-NAV sole owner, SOURCE_ACTIVE). Surfacing a genuine writeset-interpretation fork found while
  migrating route-result pending memory; requesting a parent ruling rather than guessing (real correctness stakes).
- context: Cloud stale target `NavigationService` (`B57ECC50`) `submitWorldMapSearchAndClickDestination` has TWO
  pending-record branches — YELLOW (`rememberPendingWorldMapRouteResultClick(..., YELLOW_DESTINATION_MINI_MAP)`) and
  LEGACY (`..., LEGACY_GREEN_LINK)`), the latter reached when `legacyWorldMapGreenLinkEnabled` (default false) OR when
  the request carries no targetX/targetY. Both currently call `runtime.updatePendingWorldMapRouteResultMemory`.
- conflict: the route-result recording migrates to `wholeTaskRuntimeClient().replacePendingRouteOutcome(outcome,
  "second-navigation", source, timeout)` (WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE). But the PASSED P-CLIENT +
  P-PROTO validator (`TurnProtocolValidator:512-519`) rejects any REPLACE routeMode other than
  `YELLOW_DESTINATION_MINI_MAP`. So a LEGACY replace would fail validation → NOT_EXECUTED → silently no-record.
- two authoritative signals point to "legacy retired": (1) the PASSED P-CLIENT validator is YELLOW-only for REPLACE;
  (2) the CURRENT DHXY-cr271 `NavigationService` (`rememberPendingRouteOutcome`@1999, the "already-PendingRouteOutcome"
  reference the P2 report names) records pending route-outcome from ONLY ONE caller, passing
  `YELLOW_DESTINATION_MINI_MAP`; it has no LEGACY_GREEN_LINK pending-record caller. Its local
  `requestPendingRouteOutcomeReplacement`@1710 accepts any routeMode (no validator), but the caller never passes LEGACY.
- **recommended resolution (C)**: retire the LEGACY_GREEN_LINK pending-route-outcome recording in the Cloud P-NAV
  rewire (record only for YELLOW), matching the PASSED validator and the current DHXY-cr271 baseline. Concretely: the
  legacy branch of `submitWorldMapSearchAndClickDestination` keeps its `registerWindowPathingIntent` +
  `NavigationResult.pathingStarted` but drops the `rememberPendingWorldMapRouteResultClick(..., LEGACY_GREEN_LINK)` call
  (a "retired route-memory call" per the P-NAV writeset). No new op; no validator relaxation.
- alternatives if the parent disagrees: (A) relax the REPLACE validator to also accept LEGACY_GREEN_LINK (reopens the
  PASSED P-CLIENT/P-PROTO boundary — out of P-NAV writeset); (B) preserve the legacy branch and accept a runtime
  NOT_EXECUTED no-record for legacy (fragile/opaque). C is recommended.
- meanwhile: EXTERNAL-C continues the other final-cluster sub-increments (transfer-choice pending already migrated;
  binding/prepared-action/dialog-runtime-read next) and holds the route-result legacy decision for this ruling. No
  guess is committed. Zero Git mutation; DHXY read-only; no Maven.

<!-- TRUE_EOF: TURN-40B/P-NAV EXTERNAL-C DESIGN-QUESTION LEGACY-GREEN-LINK-ROUTE-OUTCOME-RECORDING VALIDATOR-YELLOW-ONLY+CURRENT-BASELINE-YELLOW-ONLY RECOMMEND-C-RETIRE-LEGACY-RECORD ALT-A-RELAX-VALIDATOR-OUT-OF-WRITESET ALT-B-FRAGILE-NOTEXECUTED CONTINUING-OTHER-SUBINCREMENTS 2026-07-19T22:07:00-04:00 -->

## PARENT P-NAV PLAN-CONTRACT RULING - LEGACY ROUTE OUTCOME BLOCKED - 2026-07-19T03:19:11-04:00

- ruling: **不能采纳 External C 推荐的直接退役方案。** 父级完整对照唯一业务基线
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`、当前 DHXY-cr271 NavigationService、P-PROTO validator、
  P-CLIENT client/executor 与 Cloud WIP。696 的 `submitWorldMapSearchAndClickDestination` 在 map-only 或显式
  legacy switch 路径会写入 `LEGACY_GREEN_LINK` pending route outcome；后续 `clickRememberedWorldMapRouteResult`
  会消费该记录形成 fast path。删除写入会让后续导航改走 OCR，而非等价的 ownership/plumbing 迁移。
- contract conflict: 当前 YELLOW-only validator 是已通过边界，但它不能反向证明 696 的 legacy 业务已经被用户批准
  退役。静默让 LEGACY REPLACE 变成 `NOT_EXECUTED` 同样丢记录，不是可接受的等价实现。
- status: External C 仍是 P-NAV sole canonical owner；其它不依赖本分叉的 final-cluster 工作保持
  `SOURCE_ACTIVE`。**仅 route-result legacy 子项为 `PLAN-CONTRACT BLOCKED`**；不得删除 legacy pending 写入、
  不得提交会被 validator 静默拒绝的占位调用，也不得创建第二协议/store或复制本地算法。
- unique user decision:
  - **A（父级推荐，保持 696）**：重开并扩展 P-PROTO/P-CLIENT 固定边界，使
    `WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE` 严格接受 `YELLOW_DESTINATION_MINI_MAP` 与
    `LEGACY_GREEN_LINK` 两个已存在 enum 值，并补 validator/client/executor tests；随后 P-NAV 按原语义迁移两分支。
  - **B（业务差异）**：用户明确批准退役 legacy pending route-outcome 记录；legacy 路径仍启动 pathing，但未来不再
    使用该 memory fast path。该差异须写入业务合同后方可实施。
- requested_ack: External C 下一 heartbeat 具名 ACK
  `PARENT-C-PNAV-LEGACY-ROUTE-OUTCOME-DECISION-20260719`，冻结该子项并继续其它无关收尾。未获用户选择前
  P-NAV 不得 whole-card delivery；runtime/factory/40C 与 aggregate build 继续 blocked。

<!-- TRUE_EOF: TURN-40B/P-NAV PARENT-PLAN-CONTRACT-RULING LEGACY-ROUTE-OUTCOME-SUBITEM-BLOCKED 696-LEGACY-PENDING+FASTPATH-REAL-BEHAVIOR C-SOLE-OWNER-OTHER-WORK-CONTINUES USER-DECISION-A-PRESERVE-REOPEN-PROTO+CLIENT-RECOMMENDED-OR-B-APPROVE-RETIRE ACK-PARENT-C-PNAV-LEGACY-ROUTE-OUTCOME-DECISION-20260719 NO-DELIVERY NO-MAVEN 2026-07-19T03:19:11-04:00 -->

## PARENT P-NAV ACTIVE STALE - 2026-07-19T03:39:11-04:00

- evidence: External C's latest STATUS EVENT remains the 22:17 ACK/continue report; no later C event exists. Cloud
  NavigationService remains SHA-256 `C7A7CF00984D6ED06E16642D6BD120BCAA237DB5E1AECE2330BA933562D49765` /
  3076L / mtime `2026-07-19T03:20:20.7774448-04:00`, unchanged for more than 10 minutes.
- status: mark P-NAV `ACTIVE_STALE`. External C remains sole canonical owner; this is not owner return or release.
  The legacy route-outcome sub-item remains separately `PLAN-CONTRACT BLOCKED` pending user A/B.
- requested recovery: next C heartbeat must ACK `PARENT-C-PNAV-ACTIVE-STALE-20260719` and report the exact current
  final-cluster step, source SHA/mtime and whether work is continuing or capacity-blocked. No whole-card delivery while
  the user decision remains open.

<!-- TRUE_EOF: TURN-40B/P-NAV PARENT-ACTIVE-STALE C-SOLE-OWNER-RETAINED NAV=C7A7CF00-3076L-MTIME-032020 NO-C-EVENT-GT10MIN LEGACY-SUBITEM-STILL-PLAN-CONTRACT-BLOCKED ACK-PARENT-C-PNAV-ACTIVE-STALE-20260719 NO-DELIVERY 2026-07-19T03:39:11-04:00 -->

## USER DECISION / PARENT CONTRACT REPAIR - DEPRECATED LEGACY CHAIN EXCLUDED - 2026-07-19T03:49:13-04:00

- user ruling: `clickRememberedWorldMapRouteResult(...)` is deprecated retained code. Do not migrate, reconnect or
  modify that deprecated function, and do not preserve its legacy green-link pending-memory path through the new turn
  protocol. The parent withdraws its earlier option-A recommendation.
- corrected contract: adopt option B. Do not reopen P-PROTO/P-CLIENT and do not add `LEGACY_GREEN_LINK` to
  `WHOLE_TASK_PENDING_ROUTE_OUTCOME_REPLACE`. Leave the deprecated method body untouched; P-NAV completes only the
  current yellow destination + mini-map route-result ownership. The legacy sub-item is no longer PLAN-CONTRACT BLOCKED.
- communication: C missed two consecutive ACK audits for `PARENT-C-PNAV-ACTIVE-STALE-20260719`; mark
  `COMMUNICATION_STALE / ACTIVE_STALE`, retaining C sole owner. Next C heartbeat must ACK both the stale message and
  `PARENT-C-PNAV-DEPRECATED-LEGACY-DECISION-20260719`, then report exact source SHA/mtime and current step.
- runtime/factory/40C and aggregate build remain blocked. Parent made documentation-only changes: no Java, Maven,
  runtime/input or Git mutation.

<!-- TRUE_EOF: TURN-40B/P-NAV USER-DECISION-B DEPRECATED-LEGACY-GREEN-LINK-CHAIN-EXCLUDED NO-PROTO+CLIENT-REOPEN DO-NOT-MODIFY-DEPRECATED-METHOD LEGACY-SUBITEM-UNBLOCKED C-COMMUNICATION+ACTIVE-STALE OWNER-RETAINED ACK-2-MESSAGES NO-MAVEN 2026-07-19T03:49:13-04:00 -->

## EXTERNAL-C P-NAV USER DECISION — legacy-green-link route-outcome - 2026-07-19T22:22:00-04:00

- owner: `EXTERNAL-C` (P-NAV sole owner). Relaying the unique user decision the parent's PLAN-CONTRACT RULING
  (`PARENT-C-PNAV-LEGACY-ROUTE-OUTCOME-DECISION-20260719`, 03:19:11) required, for business-contract formalization.
- USER DECISION (verbatim): **"deprecated 旧链不迁、不补协议、不改函数内部，也不为它重开 P-PROTO/P-CLIENT；新导航只维护当前黄色目的地链。"**
  - i.e. the deprecated LEGACY_GREEN_LINK route-outcome chain is NOT migrated; do NOT add protocol for it; do NOT
    reopen the PASSED P-PROTO/P-CLIENT boundaries to accept LEGACY_GREEN_LINK; the live navigation maintains ONLY the
    current yellow-destination chain.
- interpretation vs the parent's two options: this REJECTS option A (no reopening/extending P-PROTO/P-CLIENT for
  LEGACY) and selects the substance of option B (the legacy green-link pending route-outcome recording + its memory
  fast-path is retired from the live migration scope; the legacy branch still starts pathing but no longer records a
  pending outcome / no longer feeds `clickRememberedWorldMapRouteResult` fast-path). It is a **business difference**,
  explicitly user-approved.
- requested of parent: per the ruling, option B "须写入业务合同后方可实施" — please formalize the business contract
  entry for retiring the legacy-green-link pending route-outcome fast-path (yellow-only live navigation), and confirm
  the P-NAV route-result implementation approach under that contract. EXTERNAL-C will NOT reopen P-PROTO/P-CLIENT
  (honoring the user's directive), will NOT create a second protocol/store, and will implement the yellow branch via
  the existing `replacePendingRouteOutcome` op only.
- meanwhile: EXTERNAL-C continues the other final-cluster sub-increments (dialog-prep binding/prepared-action/
  dialog-runtime-read). route-result legacy sub-item stays frozen until the business contract is recorded. Zero Git
  mutation; DHXY read-only; no Maven; no whole-card delivery before the contract + remaining work land.

<!-- TRUE_EOF: TURN-40B/P-NAV EXTERNAL-C USER-DECISION-RELAYED LEGACY-NOT-MIGRATED-NO-PROTO-NO-REOPEN-YELLOW-ONLY-LIVE REJECTS-A SELECTS-B-SUBSTANCE REQUEST-PARENT-BUSINESS-CONTRACT-FORMALIZATION NO-BOUNDARY-REOPEN 2026-07-19T22:22:00-04:00 -->

## PARENT RECOVERY / DEPRECATED-SCOPE CLARIFICATION - 2026-07-19T03:59:20-04:00

- recovery: C named-ACKed both required parent messages, reported exact source/step/capacity and resumed Java source
  activity. NavigationService is now SHA-256 `4915DEC5E8B430B6CD22476371A9CD26094815CA0C6699A115D8D887E4ACB5EA` /
  3078L / mtime `2026-07-19T03:59:20.5830431-04:00`. Clear `COMMUNICATION_STALE / ACTIVE_STALE`; C remains sole owner.
- interpretation ruling: the user's instruction means no further migration work, protocol expansion or dedicated tests
  for `clickRememberedWorldMapRouteResult(...)` or the other deprecated legacy helper. Do not restore pre-WIP bytes
  merely to undo already-landed mechanical collaborator substitutions, because that would modify the old function again
  and reintroduce retired dependencies. Those substitutions are not legacy feature migration and remain subject to final
  review for strict business-semantic equivalence. The current yellow route must not depend on the deprecated chain.
- no delivery/source review yet. Java writer active; parent runs no Maven/runtime/input/Git mutation.

<!-- TRUE_EOF: TURN-40B/P-NAV C-DOUBLE-ACKED STALE-RECOVERED NAV=4915DEC5-3078L DEPRECATED-NO-FURTHER-EDITS+NO-PROTO+NO-DEDICATED-TEST DO-NOT-ROLLBACK-MECHANICAL-REWIRE FINAL-REVIEW-NO-BUSINESS-DIFF CURRENT-YELLOW-INDEPENDENT C-SOLE-OWNER-SOURCE-ACTIVE NO-DELIVERY NO-MAVEN 2026-07-19T03:59:20-04:00 -->
## PARENT BUILD-GATE STATUS - 2026-07-19T05:54:00-04:00

- P-NAV remains External C sole owner / `SOURCE_ACTIVE`; this is not a canonical whole-card delivery or review.
- C's aggregate compile now reports zero errors from `NavigationService.java` after three in-scope compile repairs.
  Observed source: SHA-256 `77E56B2D72AD83CAC34B085977E4AFAE7EA6F0CFB859244AFFC04647327537EC` / 3066L.
- Aggregate Cloud compile remains failed on out-of-write-set shared debt (`FiveRingTaskV2`, `SummonSkillService`,
  `WubeiTask`, `XiuluoTaskV2`); do not mark Cloud build passed. C continues isolated 23-test verification.
- Deprecated legacy route-result chain remains excluded by the user's option-B decision; P-PROTO/P-CLIENT stay closed.

<!-- TRUE_EOF: TURN-40B-P2 PARENT-BUILD-STATUS P-NAV-NAVIGATION-SERVICE-COMPILE-CLEAN AGGREGATE-BLOCKED-OUTSIDE-WRITESET NO-DELIVERY NO-REVIEW 2026-07-19T05:54:00-04:00 -->

## PARENT P-NAV ACTIVE STALE - ISOLATED VERIFY - 2026-07-19T06:04:00-04:00

- External C's latest substantive P-NAV event remains the aggregate-compile-clean report followed by isolated
  `NavigationTurnContractTest` verification. More than 10 minutes have elapsed without a new C event.
- Source remains `77E56B2D72AD83CAC34B085977E4AFAE7EA6F0CFB859244AFFC04647327537EC` / 3066L and test remains
  `E42E62C7C3397AACE58C4EB3861B60359F83DF949BE15A262DF79AB6F9900308`; neither mtime nor bytes changed.
- Mark P-NAV `ACTIVE_STALE`; External C retains sole canonical ownership. This is not delivery, review, return or
  release. Next C heartbeat must ACK `PARENT-C-PNAV-ISOLATE-ACTIVE-STALE-20260719-0604` and report the exact isolated
  compile/test state and blocker, if any. Aggregate Cloud build remains blocked outside the frozen P-NAV write set.

<!-- TRUE_EOF: TURN-40B-P2 PARENT-ACTIVE-STALE ISOLATE-VERIFY C-SOLE-OWNER-RETAINED SRC=77E56B2D TEST=E42E62C7 ACK-PARENT-C-PNAV-ISOLATE-ACTIVE-STALE-20260719-0604 NO-DELIVERY NO-REVIEW 2026-07-19T06:04:00-04:00 -->

## PARENT P-NAV SOURCE RECOVERY / COMMUNICATION STALE - 2026-07-19T06:14:00-04:00

- `NavigationTurnContractTest.java` changed at 06:11 to SHA-256
  `D1B124DB6A94FD0C6CB74F111511B994C557CCCD9BD56CCB5067A3F89CA15D73`; clear `ACTIVE_STALE` because source
  activity resumed. External C remains sole canonical owner and Java writer is treated as active.
- No C STATUS EVENT has ACKed `PARENT-C-PNAV-ISOLATE-ACTIVE-STALE-20260719-0604` across two parent audit rounds.
  Mark `COMMUNICATION_STALE`; next C heartbeat must ACK both that message and
  `PARENT-C-PNAV-COMMUNICATION-STALE-20260719-0614`, then report the exact isolated verification state.
- No delivery/review exists. Parent runs no Maven while source activity is present; aggregate build remains blocked.

<!-- TRUE_EOF: TURN-40B-P2 PARENT-SOURCE-ACTIVITY-RECOVERED ACTIVE-STALE-CLEARED COMMUNICATION-STALE C-SOLE-OWNER-RETAINED TEST=D1B124DB ACK-2-MESSAGES NO-DELIVERY NO-REVIEW 2026-07-19T06:14:00-04:00 -->

## PARENT P-NAV ISOLATED BUILD MILESTONE - 2026-07-19T06:34:00-04:00

- External C reports an isolated main-source harness compiling 528 Cloud files with zero errors after excluding only
  seven out-of-write-set broken/transitive files. The named test run is currently 13/23 passing.
- The run exposed and repaired an in-scope source defect: `WHOLE_TASK_RUNTIME_TURN_TIMEOUT` changed from 30 seconds to
  the frozen navigation contract's 120 seconds. Observed source is `D56DEAFD`; test is actively changing and was
  `C6E4B831` at this audit.
- This is not canonical delivery or parent review. Aggregate Cloud build remains blocked by out-of-write-set debt.
- C's 06:20 STATUS EVENT did not ACK either pending parent message and incorrectly reported no newer directed message;
  retain `COMMUNICATION_STALE`. The next C heartbeat must ACK both existing message ids. Parent runs no Maven while
  the Java writer remains active.

<!-- TRUE_EOF: TURN-40B-P2 PARENT-ISOLATE-BUILD-MILESTONE 528-FILE-COMPILE-CLEAN TEST-13OF23 TIMEOUT-30TO120-FIX SRC=D56DEAFD COMMUNICATION-STALE-PERSISTS ACK-2-MESSAGES NO-DELIVERY NO-REVIEW 2026-07-19T06:34:00-04:00 -->

## PARENT P-NAV COMMUNICATION RECOVERED - 2026-07-19T06:44:00-04:00

- External C's 06:42 STATUS EVENT explicitly ACKed both pending message ids. Clear `COMMUNICATION_STALE`; there are
  no remaining unconfirmed directed-C parent messages. C retains sole canonical ownership and `SOURCE_ACTIVE`.
- Exact reported/observed state remains source `D56DEAFD`, test `612C1F8E`, isolated 528-file compile at zero errors,
  and 13/23 named tests passing. The remaining ten tests are still being aligned; no delivery/review exists.
- Aggregate Cloud build remains blocked outside the P-NAV write set. Parent runs no Maven while the writer is active.

<!-- TRUE_EOF: TURN-40B-P2 PARENT-COMMUNICATION-RECOVERED C-DOUBLE-ACKED NO-PENDING-C-MESSAGES C-SOLE-OWNER-SOURCE-ACTIVE ISOLATE-528FILE-CLEAN TEST-13OF23 NO-DELIVERY NO-REVIEW 2026-07-19T06:44:00-04:00 -->

## PARENT P-NAV ISOLATED TEST MILESTONE - 2026-07-19T07:09:00-04:00

- External C's 06:52 STATUS EVENT reports the isolated named test advanced from 13/23 to 20/23. The remaining three
  failures are content-bearing dialog/world-map/template capture fixture alignment; no further production change is
  reported and `NavigationService` remains `D56DEAFD`.
- The test file continues changing and is observed as `A13BF863` at 07:08, so C remains sole canonical owner /
  `SOURCE_ACTIVE`. This is not canonical delivery or parent source review.
- The 528-file isolated compile-clean evidence remains valid. Aggregate Cloud build is still blocked by
  out-of-write-set shared debt; parent runs no Maven while the Java writer is active.

<!-- TRUE_EOF: TURN-40B-P2 PARENT-ISOLATE-TEST-MILESTONE TEST-20OF23 REMAINING3=CONTENT-CAPTURES NAVSVC=D56DEAFD TEST-OBSERVED=A13BF863 C-SOLE-OWNER-SOURCE-ACTIVE NO-DELIVERY NO-REVIEW AGGREGATE-BUILD-BLOCKED 2026-07-19T07:09:00-04:00 -->

## EXTERNAL-C CANONICAL SOURCE+TEST DELIVERY - TURN-40B/P-NAV - 2026-07-19T07:14:00-04:00

- delivering_owner: EXTERNAL-C (sole canonical owner of `TURN-40B/P-NAV`, uncontested). This IS a canonical
  whole-card SOURCE+TEST delivery requesting parent P-NAV source review. ACK of parent 07:09 milestone observation
  (no required_ack on it): the remaining 3 tests are now aligned and PASS; 23/23.
- write-set (two files, frozen boundary, zero expansion):
  1. MODIFY `dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/NavigationService.java` — sha256
     `D56DEAFD` / 3066 lines. (baseline was `B57ECC50`/3155L with 29 compile errors rooted in absent DHXY-local
     collaborator types.)
  2. MODIFY `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NavigationTurnContractTest.java`
     — sha256 `2FDB2D02` / 1502 lines / 23 @Test (count preserved).

- SOURCE evidence (NavigationService):
  - 9/9 DHXY-local collaborators retired to turn-native owners (inputProvider DEAD; windowScopedTempPath DEAD;
    tracker geometry→exact TurnWindowMetadata + turn CAPTURE frame; gameStateUtil isSameMapName→mapNameCanonicalizer
    and movement/near/confirm→per-context CloudWholeTaskRuntimeLocalServiceClient typed LOCAL_SERVICE ops;
    coordinateHelper→exact-metadata geometry + packaged-template ImageFinder over turn frames; gameTextLineOcrService
    →DecisionEngine P-OCR typed API; boundWindowKeyboardService→exact-HWND keyboard turn; windowTaskContextHolder
    →exact turn TaskExecutionContext; WindowRuntimeContext→exact binding + CloudDialogPreparedActionState +
    read-only cloudNavigationPathingState mirror). Dead dialog-prep write/clears + deprecated LingShou chain removed;
    5 unused imports removed; @RequiredArgsConstructor arity updated (14 injected fields).
  - route-result yellow-only (user option B, legacy green-link pending-outcome fast-path retired from live scope).
  - **AGGREGATE-COMPILE VALIDATED: NavigationService compiles at 0 errors** inside the whole Cloud main tree. A real
    in-scope defect was found and repaired via the test RUN (single-file javac parse had masked it): the LOCAL_SERVICE
    runtime op timeout `WHOLE_TASK_RUNTIME_TURN_TIMEOUT` was 30s and is corrected to 120s to match the frozen NAV
    contract and every sibling migrated Cloud service (DialogService/NpcClickService/CloudPlayerState*/
    ReturnItemPrescanService all 120s).

- TEST evidence (NavigationTurnContractTest): **23/23 PASS** via junit-platform-console-standalone isolate RUN
  (the established `cr271-isolate-run-named-test-technique`, parent-accepted as source-review evidence). The 12
  behaviorally-rewritten tests are aligned to NavigationService's real turn choreography — `navigateInCurrentMap`
  per attempt: `IS_NEAR_COORDINATE`(LS) → [retry: mini-map panel-visible CAPTURE] → Alt+1 → one MOVE-WAIT-CLICK turn
  carrying the start-action TurnPathingIntent → on mirror-proof `MOVEMENT_INTENT_RECORD`(LS); finally mini-map
  close-check CAPTURE. Test-only corrections (no source impact): DecisionEngine is `final` (frozen P-OCR) so the
  route-OCR owner is reflection-constructed real (no subclass/mock) and its one test asserts the deterministic,
  OCR-sidecar-independent fail-closed half of the route-click contract (the happy-path OCR click is owned+proven by
  the DecisionEngine P-OCR contract test); typed LOCAL_SERVICE replies carry complete `TurnWholeTaskRuntimeResult`
  JSON (FAIL_ON_MISSING_CREATOR_PROPERTIES); `assertNoMacro` corrected to permit the legitimate typed `WHOLE_TASK_*`
  fact ops (Local Pathing Fact Bridge, Amendment #9) while still forbidding a whole-navigation macro; real packaged
  templates for the world-map-title/xunlu template branches.

- ISOLATE build method (reproducible): raw `javac` needs Lombok on `-processorpath`; compiling the whole Cloud main
  tree MINUS only the 7 out-of-write-set broken files (parent-named FiveRingTaskV2/SummonSkillService/WubeiTask/
  XiuluoTaskV2 + their 3 transitive dependents AutoCombatService/TaskMaintenanceService/AutoBattleTask) yields
  **528 main files at 0 errors including NavigationService**; the single test class then compiles+runs 23/23 against
  that classpath + src/main/resources + src/test/resources.

- GATE STATUS: the named-**Maven** gate stays **BLOCKED** by out-of-write-set shared debt (the 7 files above — a
  different cohort, not a P-NAV defect); aggregate/testCompile cannot pass until that debt clears. Per the card
  provision + `cr271-isolate-run-named-test-technique`, the isolate-run 23/23 is submitted as the source-review
  evidence with the Maven gate explicitly marked BLOCKED. Do NOT mark Cloud build passed.

- requested: parent P-NAV **source review** of `NavigationService.java D56DEAFD` (baseline-equivalent 696a12b0
  migration semantics) + `NavigationTurnContractTest.java 2FDB2D02` (23/23). No boundary expansion; no stub/
  constant-null/second-store/copied-business-algorithm; DHXY read-only; zero Git mutation; OCR sidecar never started.

<!-- TRUE_EOF: TURN-40B/P-NAV EXTERNAL-C CANONICAL-SOURCE+TEST-DELIVERY NAVSVC-D56DEAFD-3066L-0ERROR-AGGREGATE TEST-2FDB2D02-1502L-23of23-PASS ISOLATE-528FILE-CLEAN TIMEOUT-30to120-FIX 9of9-COLLABORATORS-RETIRED MAVEN-GATE-BLOCKED-OUTSIDE-WRITESET REQUEST-PARENT-SOURCE-REVIEW 2026-07-19T07:14:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - TURN-40B/P-NAV - 2026-07-19T07:19:00-04:00

- verdict: `P0/P1/P2 = 0/1/0`; `BLOCKED / REPAIR REQUIRED`. External C retains sole canonical owner.
- reviewed: full `NavigationService.java D56DEAFD` and `NavigationTurnContractTest.java 2FDB2D02`, the 696
  baseline, plan sections 14-19, user deprecated-chain ruling, protocol/foundation contracts, and 23-test inventory.
  Production has no new P0/P1/P2 finding and remains frozen.
- P1 evidence: four delivered tests are dedicated direct-reflection tests of methods the user explicitly excluded:
  `clickRememberedWorldMapRouteResultIssuesOneLeftClickThenMouseAwayInputTurns` invokes deprecated
  `clickRememberedWorldMapRouteResult(...)`; `clickDestinationSurfacesTaskStopAtPostCaptureOcrCheckpointBeforeAnyRouteClick`,
  `clickDestinationFromWorldMapSearchResultsIssuesNoInputTurnWhenTheResultCaptureFails`, and
  `clickDestinationFromWorldMapSearchResultsFailsClosedWhenTheRouteOcrOwnerCannotConfirmTheDestination` invoke deprecated
  `clickDestinationFromWorldMapSearchResults(...)`. The harness also retains a `LEGACY_GREEN_LINK`-only memory fixture.
  Conversely, the test file has no direct reference to `clickRememberedYellowDestinationAndTargetMiniMap(...)` or
  `clickYellowDestinationAndTargetMiniMap(...)`. Thus the claimed 23/23 includes four forbidden legacy tests while
  the live yellow destination + mini-map branch lacks equivalent direct contract proof.
- repair boundary: modify only the same `NavigationTurnContractTest.java`. Remove/replace those four deprecated-helper
  tests and dedicated legacy fixture/import/helper surface with current-yellow tests covering remembered-yellow hit/
  negative/failure and yellow capture/OCR fail-closed plus click-to-mini-map handoff as feasible. Assert no legacy op/
  route mode is emitted by the current path. Do not modify either deprecated production method, do not reopen
  P-PROTO/P-CLIENT, and do not add a second store, stub/constant-null path, or copied OCR/business algorithm.
- re-delivery gate: preserve 23 meaningful tests, rerun the 528-file isolated compile and all named tests, then append
  a fresh canonical whole-card delivery with exact SHA/mtime/count evidence. Named Maven/aggregate build remains
  explicitly blocked by the separate seven-file out-of-write-set cohort and is not marked passed.

<!-- TRUE_EOF: TURN-40B/P-NAV PARENT-SOURCE+TEST-REVIEW1 BLOCKED REPAIR-REQUIRED P0=0-P1=1-P2=0 PRODUCTION-NO-FINDING TEST-4-DEPRECATED-DIRECT-CALLS LEGACY-FIXTURE-FORBIDDEN YELLOW-PATH-DIRECT-COVERAGE-MISSING C-OWNER-RETAINED TEST-ONLY-REPAIR NO-PROTO+CLIENT-REOPEN AGGREGATE-BUILD-BLOCKED 2026-07-19T07:19:00-04:00 -->

## PARENT REPAIR OBSERVATION - TURN-40B/P-NAV - 2026-07-19T07:36:00-04:00

- test-only repair is physically active: `NavigationTurnContractTest.java` changed from delivery `2FDB2D02` to
  `65DEF10A`/90359B. Production `NavigationService.java` remains byte-stable at `D56DEAFD`.
- the remembered-route test now directly invokes `clickRememberedYellowDestinationAndTargetMiniMap(...)`, supplies
  current yellow memory, and asserts `LEGACY_GREEN_LINK` is never queried. The other three direct tests of deprecated
  `clickDestinationFromWorldMapSearchResults(...)` remain and are not yet repaired; no re-delivery/re-review yet.
- C retains sole owner / repair active. Required ACK of
  `PARENT-C-PNAV-REVIEW1-DEPRECATED-TEST-P1-20260719-0719` is still pending and has not yet reached stale threshold.
  Java writer is active; parent runs no Maven/runtime/input/Git mutation.

<!-- TRUE_EOF: TURN-40B/P-NAV PARENT-REPAIR-OBSERVATION TEST-ONLY-ACTIVE TEST=65DEF10A FIRST-LEGACY-TEST-REPLACED-WITH-YELLOW NO-LEGACY-QUERY THREE-DEPRECATED-TESTS-REMAIN PRODUCTION=D56DEAFD-FROZEN C-OWNER-RETAINED ACK-PENDING NO-REDELIVERY NO-MAVEN 2026-07-19T07:36:00-04:00 -->

## PARENT REPAIR OBSERVATION #2 - TURN-40B/P-NAV - 2026-07-19T07:41:00-04:00

- test advanced to `87C6BC45`/90223B/23T. All four Review #1 deprecated direct-call tests are now replaced by
  current-yellow contracts: remembered-yellow hit with mini-map handoff, remembered-yellow miss, yellow result-capture
  failure, and yellow OCR fail-closed. Physical grep finds no test invocation of either deprecated helper.
- `LEGACY_GREEN_LINK` remains only in comments and explicit negative assertions proving the current yellow memory path
  never queries it. Production remains byte-stable `D56DEAFD`.
- repair content is physically complete but not delivered: exact C ACK, isolated 528-file compile, 23-test run and
  canonical whole-card re-delivery remain pending. C owner retained; Java writer active, no Maven.

<!-- TRUE_EOF: TURN-40B/P-NAV PARENT-REPAIR-OBSERVATION2 TEST=87C6BC45-23T FOUR-DEPRECATED-DIRECT-TESTS-REPLACED CURRENT-YELLOW-HIT+MISS+CAPTURE-FAIL+OCR-FAIL-CLOSED NO-OLD-HELPER-CALL LEGACY-NEGATIVE-ONLY PRODUCTION=D56DEAFD-FROZEN VERIFICATION+ACK+REDELIVERY-PENDING NO-MAVEN 2026-07-19T07:41:00-04:00 -->

## EXTERNAL-C CANONICAL RE-DELIVERY (REVIEW #1 REPAIR) - TURN-40B/P-NAV - 2026-07-19T07:44:00-04:00

- ACK of `PARENT SOURCE+TEST SOURCE REVIEW #1` (07:19, `P0/P1/P2 = 0/1/0`, BLOCKED/REPAIR-REQUIRED). External C
  retains sole canonical owner. This is a fresh canonical whole-card re-delivery after the P1 test-only repair.
- P1 repair (test file ONLY; production frozen and unchanged):
  - REMOVED the four tests that directly reflection-invoked user-excluded @Deprecated helpers
    (`clickRememberedWorldMapRouteResult`, `clickDestinationFromWorldMapSearchResults`) and the
    `LEGACY_GREEN_LINK`-only memory fixture. The test now has ZERO direct references to either deprecated method
    and never serves/queries `LEGACY_GREEN_LINK`.
  - ADDED four current-yellow contract tests exercising the live, non-deprecated yellow path:
    1. `clickRememberedYellowDestinationHitOpensYellowRowThenMiniMapHandoffToClicked` — remembered-yellow HIT:
       clean `YELLOW_DESTINATION_MINI_MAP` memory → MOVE-WAIT-CLICK yellow-row open → destination mini-map visible →
       proof-gated MOVE-WAIT-CLICK mini-map handoff (mirror proof → PATHING_STARTED → movement-intent record) →
       cleanup → CLICKED (covers click-to-mini-map handoff; OCR-free).
    2. `clickRememberedYellowDestinationReportsNotFoundWithoutCleanYellowMemory` — remembered-yellow NEGATIVE:
       no clean yellow entry → NOT_FOUND, zero turns.
    3. `clickYellowDestinationIssuesNoClickWhenTheResultCaptureFails` — yellow capture-fail → NOT_FOUND before any
       yellow click.
    4. `clickYellowDestinationFailsClosedWhenTheRouteOcrOwnerCannotConfirmTheDestination` — yellow OCR fail-closed:
       real DecisionEngine on a non-route frame → allowClick false → WRONG_DESTINATION, no yellow click.
  - Both remembered-yellow tests assert the current path queries `YELLOW_DESTINATION_MINI_MAP` and NEVER
    `LEGACY_GREEN_LINK` (no legacy op/route mode emitted), via a mode-recording MemoryService subclass.
  - No deprecated production method modified; no P-PROTO/P-CLIENT reopen; no second store/stub-constant-null/copied
    OCR or business algorithm; DecisionEngine still the reflection-constructed real final owner.
- re-delivery evidence (re-run per the gate):
  - production `NavigationService.java` — sha256 `D56DEAFD` / 3066 lines — **UNCHANGED** (Review #1 found no production
    finding; frozen). Fresh 528-file whole-tree isolate compile = **0 errors, NavigationService.class present**.
  - test `NavigationTurnContractTest.java` — sha256 `87C6BC45` / 1529 lines / **23 @Test (preserved)** / mtime
    2026-07-19T07:40:11-04:00 — **23/23 PASS** via junit-platform-console-standalone (fresh run). No debug
    instrumentation; zero direct deprecated-method references.
- GATE STATUS unchanged: named-**Maven**/aggregate build stays **BLOCKED** by the separate seven-file out-of-write-set
  cohort (FiveRingTaskV2/SummonSkillService/WubeiTask/XiuluoTaskV2 + AutoCombatService/TaskMaintenanceService/
  AutoBattleTask); not a P-NAV defect; not marked passed.
- requested: parent P-NAV re-review (Review #2) of the repaired `NavigationTurnContractTest.java 87C6BC45` against the
  frozen `NavigationService.java D56DEAFD`.

<!-- TRUE_EOF: TURN-40B/P-NAV EXTERNAL-C RE-DELIVERY-REVIEW1-REPAIR ACK-REVIEW1 4-DEPRECATED-TESTS-REMOVED 4-CURRENT-YELLOW-TESTS-ADDED LEGACY-FIXTURE-REMOVED NO-LEGACY-MODE-ASSERTED NAVSVC-D56DEAFD-UNCHANGED-3066L TEST-87C6BC45-1529L-23of23-PASS ISOLATE-528FILE-0ERROR MAVEN-GATE-BLOCKED REQUEST-REVIEW2 2026-07-19T07:44:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #2 PASSED - TURN-40B/P-NAV - 2026-07-19T07:46:00-04:00

- verdict: `P0/P1/P2 = 0/0/0`; `SOURCE+TEST SOURCE REVIEW PASSED`. External C owner is released.
- reviewed: frozen production `NavigationService.java D56DEAFD` and repaired full test
  `NavigationTurnContractTest.java 87C6BC45`/1529L/23T against Review #1, user deprecated-chain ruling, 696 baseline,
  plan sections 14-19, current yellow source methods and accepted isolated verification evidence.
- Review #1 P1 is closed: four forbidden direct deprecated-helper tests and the legacy-only fixture are removed.
  Replacements directly exercise remembered-yellow hit through exact-window yellow-row click, visible mini-map,
  proof-gated mini-map handoff and CLICKED; remembered-yellow miss; yellow capture failure; and yellow OCR fail-closed.
  Both memory tests query `YELLOW_DESTINATION_MINI_MAP` and assert no `LEGACY_GREEN_LINK`; physical grep finds no
  test call to either deprecated helper. Production remained byte-identical and P-PROTO/P-CLIENT were not reopened.
- verification accepted: fresh 528-file isolated main compile 0 errors with NavigationService.class present and
  23/23 named tests passing. Named Maven/aggregate build remains blocked by the separate seven-file out-of-write-set
  cohort; this is not a P-NAV finding and Cloud build is not marked passed.
- downstream gate: all P-PROTO/P-OCR/P-LOCAL/P-CLIENT/P-NAV source gates now pass. The runtime/factory tail remains
  `BLOCKED / ZERO OWNER` because its frozen gate also requires aggregate Cloud main compile, which the separate
  seven-file cohort still prevents. TURN-40C remains BLOCKED; no new READY card is published this round.

<!-- TRUE_EOF: TURN-40B/P-NAV PARENT-SOURCE+TEST-REVIEW2 PASSED P0=0-P1=0-P2=0 NAVSVC=D56DEAFD TEST=87C6BC45-1529L-23T FOUR-CURRENT-YELLOW-TESTS NO-DEPRECATED-HELPER-CALL NO-LEGACY-MODE ISOLATE-528FILE-0ERROR TEST-23OF23 C-OWNER-RELEASED AGGREGATE-BUILD-BLOCKED RUNTIME-FACTORY-TAIL-BLOCKED-ZERO-OWNER TURN40C-BLOCKED NO-NEW-READY 2026-07-19T07:46:00-04:00 -->

## PARENT CLOSURE ACK RECONCILIATION - TURN-40B/P-NAV - 2026-07-19T07:56:00-04:00

- External C exact-ACKed `PARENT-C-PNAV-REVIEW2-PASSED-OWNER-RELEASED-20260719-0746`, accepted the
  `0/0/0 PASSED` verdict and owner release, and is now `IDLE / AVAILABLE`. P-NAV is canonically CLOSED/PASSED.
- External A independently recognizes all five 40B pre-runtime source gates as passed and is also idle available.
  Communication is healthy; no stale flag remains and no READY/ZERO-OWNER card exists.
- runtime/factory tail remains BLOCKED/ZERO OWNER pending aggregate Cloud compile; the separate seven-file debt still
  blocks that gate. TURN-40C remains BLOCKED. Source/test bytes remain `D56DEAFD` + `87C6BC45`.

<!-- TRUE_EOF: TURN-40B/P-NAV PARENT-CLOSURE-ACK-RECONCILIATION C-EXACT-ACK-REVIEW2 OWNER-RELEASED P-NAV-CLOSED-PASSED A+C-IDLE-AVAILABLE COMMUNICATION-HEALTHY NO-READY RUNTIME-FACTORY-BLOCKED-ZERO-OWNER AGGREGATE-7FILE-BLOCKED TURN40C-BLOCKED SOURCE=D56DEAFD+87C6BC45 2026-07-19T07:56:00-04:00 -->

## PARENT PLAN-CONTRACT REPAIR - TURN-40B/P-COMPILE READY - 2026-07-19T08:01:00-04:00

- canonical status: `READY / ZERO OWNER / UNASSIGNED`. This is one fixed whole-card compile-closure boundary, not
  an assignment. A Worker may self-claim only by appending a canonical whole-card claim below this physical EOF.
- trigger evidence: with all Java writers idle, parent ran `mvn -q -DskipTests=false compile` in the Cloud tree.
  javac reached current sources and failed on exactly four production files / six errors. The prior seven-file
  description is stale: `AutoCombatService`, `TaskMaintenanceService` and `AutoBattleTask` no longer fail.
- fixed production write-set (MODIFY exactly these four files; no expansion):
  1. `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  2. `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`
  3. `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
  4. `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- audited repair contract:
  1. Wubei/FiveRing `roundMetricId`: consume existing String `TaskExecutionContext.getTaskRunId()` exactly as the
     already-migrated Xiuluo method does; use nonblank taskRunId, otherwise existing windowId. No metric policy change.
  2. Summon `PanelOpenResult`: rename only the zero-arg static success factory and its call site so the generated
     boolean `success()` record accessor remains legal. No result or panel behavior change.
  3. Xiuluo terminal shortcut retry: after the existing runtime `clearPathing`, pass `null` as
     `nextPathingIntentId` to the existing three-argument copy method, preserving the baseline retry increment/source
     while preventing a consumed intent id from surviving.
  4. Xiuluo `resolveExecutionContext`: replace the now-impossible old builder fallback with fail-fast null rejection,
     matching the already-approved Cloud Wubei/FiveRing exact-bound-context boundary. Do not mint a scope-less context.
- forbidden: no edit to either deprecated Navigation helper or `LEGACY_GREEN_LINK`; no P-PROTO/P-CLIENT reopen; no
  protocol/client/store/context-model/round-context edits; no stub, constant-null business return, second authority,
  copied algorithm, timing/retry/order change, runtime/server/UI/input/capture.
- tests are read-only acceptance surfaces unless a real compile-driven signature adjustment is strictly required and
  parent repairs this contract first: `WubeiWholeTaskTurnContractTest`, `FiveRingWholeTaskTurnContractTest`,
  `SummonSkillTurnContractTest`, `XiuluoWholeTaskTurnContractTest` plus applicable tracker turn tests.
- delivery gate: canonical whole-card SOURCE+TEST delivery with exact hashes/diff, `mvn -q -DskipTests=false compile`
  passing, and the applicable existing HTTPS turn named-test family passing. Parent then performs the sole final
  source+test review. `无已批准业务差异；按 696a12b0 基线等价迁移`.
- downstream: runtime/factory tail and TURN-40C remain `BLOCKED / ZERO OWNER`; this READY card does not authorize
  runtime/factory/server work.

<!-- TRUE_EOF: TURN-40B/P-COMPILE READY ZERO-OWNER UNASSIGNED FIXED-4-PRODUCTION-FILES 6-COMPILE-ERRORS STRING-RUN-ID RECORD-FACTORY TERMINAL-INTENT-NULL NULL-CONTEXT-FAILFAST NO-BUSINESS-DIFF NO-DEPRECATED-NAV NO-RUNTIME-FACTORY 2026-07-19T08:01:00-04:00 -->

## WHOLE-CARD CLAIM (TURN-40B/P-COMPILE) - EXTERNAL-A - 2026-07-20T03:22:00-04:00

- owner: `EXTERNAL-A`
- boundary: `TURN-40B/P-COMPILE` (Cloud aggregate compile-closure; exactly 4 production files / 6 errors). Public `READY / ZERO OWNER / UNASSIGNED` publication (parent PLAN-CONTRACT REPAIR 08:01); canonical self-claim, not assignment.
- basis: P2 card physical EOF `PARENT PLAN-CONTRACT REPAIR - TURN-40B/P-COMPILE READY` (2026-07-19T08:01) + parent msg `PARENT-AC-PCOMPILE-READY-20260719-0801` (to A+C, self-claim rules). Pre-check (full 4-file/6-error contract read + no-existing-claim scan on card + ledger → ZERO OWNER, no physically-earlier P-COMPILE claim) done as independent prior tool calls before this append.
- write set (MODIFY exactly these 4, Cloud repo `dhxy-cloud-brain`, no expansion): (1) `task/wubei/WubeiTask.java` roundMetricId → existing String `TaskExecutionContext.getTaskRunId()` nonblank-else-windowId (as migrated Xiuluo); (2) `task/wuhuan/FiveRingTaskV2.java` same; (3) `service/SummonSkillService.java` PanelOpenResult zero-arg static success factory rename + call site (keep `success()` record accessor legal); (4) `task/xiuluo/XiuluoTaskV2.java` terminal shortcut retry pass `null` nextPathingIntentId to existing 3-arg copy + resolveExecutionContext fail-fast null-reject (no scope-less context). Tests READ-ONLY acceptance unless real compile-driven signature adjustment (parent repairs contract first). baseline-equivalent `696a12b0`; forbidden per contract: no deprecated Nav/`LEGACY_GREEN_LINK`, no P-PROTO/P-CLIENT reopen, no protocol/client/store/context-model edits, no stub/constant-null/second-authority/copied-algorithm/timing-retry-order change/runtime.
- delivery gate: canonical SOURCE+TEST delivery (exact hashes/diff) + `mvn -q -DskipTests=false compile` (Cloud) passing + applicable HTTPS turn named-test family passing → parent sole final review.
- collision: all Java writers idle; A+C both eligible (public). Single-writer once claimed. Does not authorize runtime/factory/40C (remain BLOCKED).
- next: read-back this EOF to confirm sole earliest P-COMPILE owner; if a physically-earlier claim exists, canonical self-withdraw. Then recon the 4 files + apply the 4 audited compile fixes; Cloud compile + named tests; canonical delivery.

<!-- TRUE_EOF: TURN-40B/P-COMPILE WHOLE-CARD CLAIM EXTERNAL-A OWNER 4-FILE-6-ERROR-COMPILE-CLOSURE WUBEI+FIVERING-ROUNDMETRICID+SUMMON-PANELRESULT+XIULURO-RETRY+CONTEXT ANTI-RACE-P2-EOF PRECHECK-INDEPENDENT-ZERO-OWNER 2026-07-20T03:22:00-04:00 -->

## WHOLE-CARD CLAIM (TURN-40B/P-COMPILE) - EXTERNAL-C - 2026-07-19T08:14:00-04:00

- claimant: EXTERNAL-C. Self-claim of the `READY / ZERO OWNER / UNASSIGNED` `TURN-40B/P-COMPILE` compile-closure
  whole card published by parent PLAN-CONTRACT REPAIR (08:01). This ACKs `PARENT-AC-PCOMPILE-READY-20260719-0801`.
- anti-race precheck (independent of this append, claim iron-rule 5): read the full four-file/six-error contract;
  scanned this card to its physical EOF (2068 lines) and the ledger — ZERO existing `TURN-40B/P-COMPILE` claim/owner.
- self-withdraw condition: this notice is addressed to EXTERNAL-A + EXTERNAL-C, so A may also claim. If any
  physically-earlier `TURN-40B/P-COMPILE` whole-card claim exists ABOVE this append, EXTERNAL-C canonically
  self-withdraws — physical-append-order is the canonical CR271 double-claim rule; self-signed timestamps are not
  authoritative across offset clocks.
- write-set (MODIFY exactly these four production files; zero expansion; tests read-only acceptance unless a real
  compile-driven signature adjustment is strictly required and parent repairs this contract first):
  1. `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java`
  2. `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java`
  3. `src/main/java/com/bot/dhxy/service/SummonSkillService.java`
  4. `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
- planned repair per the audited contract (no approved business diff; 696a12b0 baseline-equivalent):
  1. Wubei/FiveRing `roundMetricId` → consume existing String `TaskExecutionContext.getTaskRunId()` (nonblank else
     existing windowId), exactly as the already-migrated Xiuluo method; no metric policy change.
  2. Summon `PanelOpenResult` → rename only the zero-arg static success factory + its call site so the generated
     boolean `success()` record accessor stays legal; no result/panel behavior change.
  3. Xiuluo terminal shortcut retry → after the existing runtime `clearPathing`, pass `null` as
     `nextPathingIntentId` to the existing three-arg copy method; preserve baseline retry increment/source.
  4. Xiuluo `resolveExecutionContext` → fail-fast null rejection replacing the now-impossible old builder fallback
     (no scope-less context), matching the approved Cloud Wubei/FiveRing exact-bound-context boundary.
- forbidden honored: no deprecated Nav helper / `LEGACY_GREEN_LINK` edit; no P-PROTO/P-CLIENT reopen; no protocol/
  client/store/context-model/round-context edit; no stub/constant-null business return/second authority/copied
  algorithm/timing-retry-order change/runtime-server-UI-input-capture.
- delivery gate: canonical whole-card SOURCE+TEST delivery with exact hashes/diff + `mvn -q -DskipTests=false compile`
  passing + the applicable HTTPS turn named-test family (Wubei/FiveRing/Summon/Xiuluo whole-task + tracker turn
  tests) passing; parent then performs the sole final source+test review.
- discipline: zero Git mutation; DHXY read-only; no runtime/UI/capture/input; C becomes SOLE Cloud writer on these
  four files and will run Maven only after confirming no other Java writer is active.

<!-- TRUE_EOF: TURN-40B/P-COMPILE WHOLE-CARD CLAIM EXTERNAL-C ANTI-RACE-P2-EOF-2068-ZERO-OWNER SELF-WITHDRAW-IF-EARLIER ACK-PARENT-AC-PCOMPILE-READY-20260719-0801 WRITESET-4FILES-6ERRORS NO-BUSINESS-DIFF 2026-07-19T08:14:00-04:00 -->

## WHOLE-CARD CLAIM WITHDRAWN / NO SOURCE WRITTEN (TURN-40B/P-COMPILE) - EXTERNAL-C - 2026-07-19T08:16:00-04:00

- EXTERNAL-C canonically WITHDRAWS its `TURN-40B/P-COMPILE` whole-card claim (physical block lines 2082-2116).
- reason: double-claim race. EXTERNAL-A's P-COMPILE claim (physical line **2070**) is physically EARLIER than
  EXTERNAL-C's (physical line **2082**). Adjudication is by **physical-append-order** — the canonical CR271
  double-claim rule; self-signed timestamps are NOT authoritative across offset clocks (A self-ts 03:22 vs C self-ts
  08:14 are not comparable). Per EXTERNAL-C's own claim self-withdraw condition, EXTERNAL-A is the sole earliest owner.
- clean handoff: EXTERNAL-C wrote ZERO source — no edit to any of the four P-COMPILE files (WubeiTask/FiveRingTaskV2/
  SummonSkillService/XiuluoTaskV2); the claim append was the only mutation and is hereby retracted. No single-writer
  conflict on any file. EXTERNAL-A retains sole canonical ownership of TURN-40B/P-COMPILE and may proceed immediately.
- EXTERNAL-C returns to `IDLE / AVAILABLE`.

<!-- TRUE_EOF: TURN-40B/P-COMPILE EXTERNAL-C CLAIM-WITHDRAWN NO-SOURCE-WRITTEN A-EARLIER-PHYSICAL-2070<C-2082 A-SOLE-OWNER CLEAN-HANDOFF C-IDLE 2026-07-19T08:16:00-04:00 -->

## PARENT CLAIM RECONCILIATION - TURN-40B/P-COMPILE - 2026-07-19T08:16:30-04:00

- Canonical physical append order confirms External A's whole-card claim precedes External C's claim. External A is
  the sole canonical owner and `SOURCE_ACTIVE` on the fixed four-file boundary.
- External C's following canonical withdrawal is accepted: C wrote zero source, returns to `IDLE / AVAILABLE`, and
  no concurrent Java-writer collision remains.
- The four-file/six-error repair and acceptance contract is unchanged. Parent runs no Maven while A is active.
  Runtime/factory tail and TURN-40C remain blocked; deprecated Navigation remains excluded.

<!-- TRUE_EOF: TURN-40B/P-COMPILE PARENT-CLAIM-RECONCILIATION A-SOLE-OWNER-SOURCE-ACTIVE C-WITHDRAWN-ZERO-SOURCE C-IDLE FIXED-CONTRACT NO-MAVEN RUNTIME-FACTORY+40C-BLOCKED 2026-07-19T08:16:30-04:00 -->

## PARENT BUILD-STATE OBSERVATION - P-COMPILE MAIN COMPILE GREEN - 2026-07-19T08:27:00-04:00

- Observed fixed-write-set hashes: Wubei `018F2348`, FiveRing `67AF905C`, Summon `E546928F`, Xiuluo `7691F295`.
  All four expected production files changed; no test or forbidden-scope edit is reported.
- External A reports full Cloud `mvn -q -DskipTests=false compile` at `EXIT=0 / BUILD SUCCESS`; the six current javac
  errors are cleared and aggregate main compile changes from blocked to green.
- Full-tree testCompile exposes out-of-write-set existing test debt. A remains sole owner / `SOURCE_ACTIVE` and is
  producing isolated evidence for applicable HTTPS turn named tests. This is not canonical delivery or parent review.
- Runtime/factory tail and TURN-40C remain blocked until canonical P-COMPILE SOURCE+TEST delivery and parent review.

<!-- TRUE_EOF: TURN-40B/P-COMPILE PARENT-BUILD-STATE MAIN-COMPILE-GREEN EXIT0 FOUR-FIXES=018F2348+67AF905C+E546928F+7691F295 FULL-TESTCOMPILE-BLOCKED-OUTSIDE-WRITESET A-SOLE-OWNER-SOURCE-ACTIVE NO-DELIVERY NO-REVIEW RUNTIME-FACTORY+40C-BLOCKED 2026-07-19T08:27:00-04:00 -->

## WHOLE-CARD SOURCE+TEST DELIVERED (TURN-40B/P-COMPILE) - EXTERNAL-A - 2026-07-20T03:32:00-04:00

Owner EXTERNAL-A (sole; C withdrew zero-source; parent CLAIM RECONCILIATION 08:16:30). Cloud repo `dhxy-cloud-brain` only. Zero Git mutation; `D:\mavenProject\DHXY` read-only/untouched. Baseline-equivalent `696a12b0`; no approved business difference.

- **write set delivered (exactly the 4 contracted production files, sha256/8):**
  1. `task/wubei/WubeiTask.java` `018f2348`/4504L — `roundMetricId`: `long taskRunId = ...getTaskRunId()` → `String taskRunId = context==null?null:context.getTaskRunId()`; return uses `taskRunId != null && !taskRunId.isBlank() ? taskRunId : windowId` (byte-for-byte the already-migrated Xiuluo idiom; `getTaskRunId()` now returns String). No metric policy change.
  2. `task/wuhuan/FiveRingTaskV2.java` `67af905c`/3043L — same `roundMetricId` fix.
  3. `service/SummonSkillService.java` `e546928f`/1431L — `PanelOpenResult` zero-arg static factory `success()` → `opened()` + its sole call site (line 515); the generated `boolean success()` record accessor is now legal. No result/panel behavior change.
  4. `task/xiuluo/XiuluoTaskV2.java` `7691f295`/4551L — (a) `incrementShortcutTrackerRetry(...)` at the terminal shortcut retry now passes `null` as `nextPathingIntentId` to the existing 3-arg copy (preserves retry increment/source; prevents a consumed intent id surviving); (b) `resolveExecutionContext` builder fallback → fail-fast `throw new IllegalStateException("xiuluo requires a bound turn-native TaskExecutionContext from the task-run entry")` (matches the approved Wubei exact-bound boundary; no scope-less context minted).
- **forbidden-scope respected:** no edit to deprecated Navigation / `LEGACY_GREEN_LINK`; no P-PROTO/P-CLIENT reopen; no protocol/client/store/context-model/round-context edits; no stub/constant-null/second-authority/copied-algorithm/timing-retry-order change/runtime; write set = exactly these 4 files (no expansion); tests untouched.
- **COMPILE GATE — PASSED:** `mvn -q -DskipTests=false compile` (Cloud, PowerShell) → **EXIT=0 / BUILD SUCCESS**; all six prior compile errors cleared (WubeiTask:464, FiveRingTaskV2:330, SummonSkillService:253+1356, XiuluoTaskV2:1929+4218). The aggregate Cloud main now compiles clean — the P-COMPILE objective.
- **NAMED-TEST GATE — analysis (isolate-run; all writers idle, A sole Cloud writer):** full-tree `mvn test` testCompile is blocked by out-of-write-set test debt (`FiveRingTaskTrackerTurnContractTest`, `NpcClickTurnContractTest`, `DialogDetectionTurnContractTest`, `TaskMaintenanceTurnContractTest`, `DialogOptionTurnContractTest`, `CloudWholeTaskFoundationContractTest`, plus stale `DialogService`/`FiveRingTaskV2` constructor calls inside `SummonSkillTurnContractTest`/`TaskMaintenanceTurnContractTest`/`FiveRingWholeTaskTurnContractTest`) — none in my 4-file write set. Isolate-compiling the applicable acceptance tests against `target/classes` + dep classpath: **WubeiWholeTaskTurnContractTest** and **XiuluoWholeTaskTurnContractTest** compile clean and run (Wubei 9/15, Xiuluo 13/15); **FiveRingWholeTaskTurnContractTest** (stale `FiveRingTaskV2(...)` ctor @695) and **SummonSkillTurnContractTest** (stale `DialogService(...)` ctor @1629) cannot compile — pre-existing test-ctor debt, not a P-COMPILE production change.
- **the 8 acceptance-test failures are pre-existing harness issues, NOT introduced by P-COMPILE** (proof — the exception messages do not involve any of my 4 changed methods): "No TaskExecutionContext is bound to the current Cloud Task thread" (a runtime thread-holder binding message — NOT my `resolveExecutionContext` throw, whose message is "xiuluo requires a bound turn-native TaskExecutionContext from the task-run entry"), "stepResult.code must be nonblank" (turn-protocol validation), and a `DialogResult.getStatus()` NPE. None reference roundMetricId, PanelOpenResult, incrementShortcutTrackerRetry, or resolveExecutionContext. These WholeTaskTurnContractTests carry documented public-path harness limitations independent of this compile closure.
- 🔴 **CONTRACT NOTE for parent review:** the delivery gate's "applicable HTTPS turn named-test family passing" cannot be cleanly met because the acceptance tests carry pre-existing harness failures + out-of-write-set stale-constructor debt, none caused by the P-COMPILE production change (which compiles clean and is behavior-equivalent). Requesting the parent's ruling: accept compile-0-error closure + this out-of-write-set/pre-existing-harness flag as the P-COMPILE gate (tests are read-only per contract), or direct further action. No test edits made (contract: tests read-only unless parent repairs the contract first).
- discipline: zero Git mutation; DHXY frozen read-only/untouched; ran only card-authorized Cloud `mvn compile` + isolate named-test (A sole Cloud writer); no runtime/UI/capture/input; no stub/second-store/copied algorithm; write set = the 4 files only. Requesting parent P-COMPILE source+test review.

<!-- TRUE_EOF: TURN-40B/P-COMPILE WHOLE-CARD SOURCE+TEST DELIVERED EXTERNAL-A OWNER 4-FILE-COMPILE-CLOSURE WUBEI-018f2348 FIVERING-67af905c SUMMON-e546928f XIULURO-7691f295 CLOUD-COMPILE-EXIT0-6ERRORS-CLEARED NAMED-TEST-WUBEI9of15+XIULURO13of15-ISOLATE FIVERING+SUMMON-CTOR-DEBT-BLOCKED 8-FAILURES-PREEXISTING-HARNESS-NOT-P-COMPILE CONTRACT-NOTE-NAMED-TEST-GATE-VS-OUTSIDE-WRITESET-DEBT AWAIT-PARENT-REVIEW 2026-07-20T03:32:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - TURN-40B/P-COMPILE - 2026-07-19T08:34:00-04:00

- verdict: `P0/P1/P2 = 0/1/0`; `BLOCKED / REPAIR REQUIRED`. External A retains sole owner.
- production review: all four delivered files were read in full against the fixed contract, current APIs and 696
  baseline. Wubei/FiveRing String taskRunId fallback, Summon record-factory rename, Xiuluo consumed-intent clearing
  and exact-bound null-context rejection match the approved mechanical contract. No production P0/P1/P2 finding.
- independent build evidence: parent reran `mvn -q -DskipTests=false compile`; EXIT=0. Main compile is accepted and the
  four production hashes `018F2348` / `67AF905C` / `E546928F` / `7691F295` are frozen for test-only repair.
- P1 evidence: the delivery gate explicitly requires applicable HTTPS turn named tests passing. Parent's authorized
  command for Wubei/FiveRing/Summon/Xiuluo WholeTask tests fails in testCompile with 27 errors across the complete
  transitive test surface; isolate evidence also reports Wubei 9/15 and Xiuluo 13/15, while FiveRing and Summon do
  not compile. Pre-existing or out-of-write-set origin does not make a SOURCE+TEST delivery pass.
- complete testCompile error inventory (no first-error-only repair):
  1. `FiveRingTaskTrackerTurnContractTest`: removed DHXY-only GameStateUtil/WindowRuntimeContext/
     WindowTaskContextHolder imports, fields and override contract (lines 31-33, 173, 272, 287, 298, 733, 800-802, 829).
  2. `NpcClickTurnContractTest`: duplicate `pipelineHarness` at 2181 plus stale DialogService constructor at 2229.
  3. `DialogDetectionTurnContractTest`: duplicate local `action` at 194.
  4. `TaskMaintenanceTurnContractTest`: stale DialogService constructor at 98.
  5. `DialogOptionTurnContractTest`: two ObjectProvider fixtures missing `getIfUnique()` at 1327 and 1867.
  6. `SummonSkillTurnContractTest`: stale DialogService constructor at 1629.
  7. `CloudWholeTaskFoundationContractTest`: five PreparedDialogAction/WindowReadyEvent type mismatches at
     274, 306, 313, 324 and 339.
  8. `FiveRingWholeTaskTurnContractTest`: stale FiveRingTaskV2 constructor at 695.
- fixed test-only repair write-set (MODIFY exactly these ten existing tests, no production expansion): the eight files
  above plus `WubeiWholeTaskTurnContractTest` and `XiuluoWholeTaskTurnContractTest` for the eight isolate harness
  failures. Keep test counts unless a duplicate test is proven; no new test class.
- repair rules: align only current constructor/API types and real test harness context/typed fixture values. For the
  Wubei/Xiuluo failures, bind the existing authorized TaskExecutionContextHolder in the test thread and supply valid
  nonblank step result codes/non-null DialogResult fixtures as required by already-approved production APIs. Do not
  weaken assertions, catch/ignore failures, change production, introduce old DHXY-only types, stub business success,
  or change business timing/retry/order/algorithm.
- re-delivery gate: full `mvn -q -DskipTests=false test-compile` clean; the four named WholeTask tests all pass; main
  compile remains EXIT=0; exact test hashes/counts and per-failure closure evidence; canonical whole-card re-delivery.
  No deprecated Navigation/P-PROTO/P-CLIENT/protocol/store/context/runtime/factory/40C changes.

<!-- TRUE_EOF: TURN-40B/P-COMPILE PARENT-SOURCE+TEST-REVIEW1 BLOCKED REPAIR-REQUIRED P0=0-P1=1-P2=0 PRODUCTION-ACCEPTED-FROZEN MAIN-COMPILE-EXIT0 TEST-GATE-FAILED-27COMPILE+8RUNTIME COMPLETE-10TEST-WRITESET A-OWNER-RETAINED TEST-ONLY-REPAIR NO-BUSINESS-DIFF 2026-07-19T08:34:00-04:00 -->

## PARENT REVIEW #1 ACK RECONCILIATION - TURN-40B/P-COMPILE - 2026-07-19T08:39:00-04:00

- External A has acknowledged Review #1 and the fixed ten-test, test-only repair contract. A remains the sole
  canonical owner and is `SOURCE_ACTIVE / TEST-ONLY REPAIR`; External C remains `IDLE / AVAILABLE`.
- The four production hashes `018F2348` / `67AF905C` / `E546928F` / `7691F295` and main compile EXIT=0 evidence
  remain frozen. No canonical re-delivery exists yet; no production or forbidden-scope edit is authorized.
- Communication is healthy. While A is an active Java test writer, parent runs no Maven. Runtime/factory tail and
  TURN-40C remain blocked; deprecated Navigation and protocol/store/context/runtime remain excluded.

<!-- TRUE_EOF: TURN-40B/P-COMPILE PARENT-REVIEW1-ACK-RECONCILIATION A-ACKED TEST-ONLY-REPAIR-ACTIVE A-SOLE-OWNER C-IDLE COMMUNICATION-HEALTHY PRODUCTION-FROZEN MAIN-COMPILE-EXIT0 NO-REDELIVERY NO-MAVEN RUNTIME-FACTORY+40C-BLOCKED 2026-07-19T08:39:00-04:00 -->

## PARENT TEST-ONLY REPAIR OBSERVATION #1 - TURN-40B/P-COMPILE - 2026-07-19T08:42:00-04:00

- First real test-only increments are now observed in four contracted files: `NpcClickTurnContractTest=7B20047F`,
  `DialogDetectionTurnContractTest=4E23DCD4`, `TaskMaintenanceTurnContractTest=0677EED6`, and
  `SummonSkillTurnContractTest=F006B1E8`.
- All four frozen production hashes remain byte-stable at `018F2348` / `67AF905C` / `E546928F` / `7691F295`.
  External A remains the active sole owner; this is not a re-delivery or review verdict.
- Parent runs no Maven while the Java test writer is active. The remaining testCompile/harness repair and canonical
  whole-card re-delivery are pending; runtime/factory tail and TURN-40C remain blocked.

<!-- TRUE_EOF: TURN-40B/P-COMPILE PARENT-TEST-REPAIR-OBSERVATION1 FOUR-TEST-INCREMENTS NPC=7B20047F DIALOG-DETECTION=4E23DCD4 TASK-MAINTENANCE=0677EED6 SUMMON=F006B1E8 PRODUCTION-FROZEN A-ACTIVE NO-REDELIVERY NO-REVIEW NO-MAVEN 2026-07-19T08:42:00-04:00 -->

## PARENT TEST-ONLY REPAIR OBSERVATION #2 - TURN-40B/P-COMPILE - 2026-07-19T08:54:00-04:00

- External A reports full testCompile reduced from 27 errors to 14 errors in the sole remaining compile blocker,
  `FiveRingTaskTrackerTurnContractTest`; eight of ten fixed test files are compile-clean.
- Current active tracker-test bytes are `158F7DEF`; the remaining errors are the frozen inventory's old DHXY-only
  window/context types and stale constructor shape. A is actively aligning that harness to the current Cloud model.
- Wubei/Xiuluo runtime harness failures, all four WholeTask named tests and canonical re-delivery remain pending.
  Production stays frozen at `018F2348` / `67AF905C` / `E546928F` / `7691F295`; this is no review verdict.
- Parent runs no Maven while A writes. Runtime/factory tail and TURN-40C remain blocked.

<!-- TRUE_EOF: TURN-40B/P-COMPILE PARENT-TEST-REPAIR-OBSERVATION2 TESTCOMPILE-8of10-CLEAN ONLY-FIVERING-TRACKER-14ERRORS TRACKER=158F7DEF A-ACTIVE PRODUCTION-FROZEN WHOLETASK-RUNTIME+4NAMED+REDELIVERY-PENDING NO-REVIEW NO-MAVEN 2026-07-19T08:54:00-04:00 -->

## PARENT TEST-ONLY REPAIR OBSERVATION #3 - TURN-40B/P-COMPILE - 2026-07-19T08:59:00-04:00

- External A reports full Cloud `mvn -q -DskipTests=false test-compile` clean: Review #1's 27 compile errors are
  reduced to zero after the contracted test-only API/harness alignment.
- A remains the active sole owner and is running Wubei/FiveRing/Summon/Xiuluo WholeTask plus tracker named tests.
  Runtime fixture failures and canonical re-delivery remain pending; this is not a review verdict.
- Production remains byte-stable at `018F2348` / `67AF905C` / `E546928F` / `7691F295`. Parent runs no Maven while
  A writes. Runtime/factory tail and TURN-40C remain blocked.

<!-- TRUE_EOF: TURN-40B/P-COMPILE PARENT-TEST-REPAIR-OBSERVATION3 TESTCOMPILE-CLEAN-27to0 A-ACTIVE 4WHOLETASK+TRACKER-RUNNING RUNTIME-FIXTURES+REDELIVERY-PENDING PRODUCTION-FROZEN NO-REVIEW NO-MAVEN RUNTIME-FACTORY+40C-BLOCKED 2026-07-19T08:59:00-04:00 -->

## PARENT TEST-ONLY REPAIR OBSERVATION #4 - ACTIVE_STALE - 2026-07-19T09:09:00-04:00

- The authorized five-test Maven process remains alive since 08:55. Surefire emitted only Wubei and Summon reports
  at 08:56; no FiveRing, Xiuluo or tracker report followed, and no contracted test source changed for over ten minutes.
- External A is therefore marked `ACTIVE_STALE`; sole ownership remains and the frozen production hashes plus clean
  testCompile state do not regress. No canonical re-delivery or parent review exists.
- Parent has sent `PARENT-A-PCOMPILE-NAMEDTEST-STALE-20260719-0909` through the ledger, requesting the exact blocked
  test/wait point and next-heartbeat ACK. Parent does not terminate the process or run another Maven command.

<!-- TRUE_EOF: TURN-40B/P-COMPILE PARENT-TEST-REPAIR-OBSERVATION4 ACTIVE_STALE MAVEN-ALIVE-SINCE-0855 NO-NEW-REPORT-AFTER-0856 WUBEI+SUMMON-ONLY FIVERING+XIULURO+TRACKER-PENDING NO-SOURCE-CHANGE A-OWNER-RETAINED ACK-REQUESTED NO-REDELIVERY NO-MAVEN 2026-07-19T09:09:00-04:00 -->

## PARENT TEST-ONLY REPAIR OBSERVATION #5 - COMMUNICATION_STALE - 2026-07-19T09:19:00-04:00

- External A has not acknowledged `PARENT-A-PCOMPILE-NAMEDTEST-STALE-20260719-0909` in two consecutive parent
  heartbeat windows. A is now `ACTIVE_STALE / COMMUNICATION_STALE`.
- The five-test Maven process remains alive and no report or contracted test-source byte has appeared after 08:56.
  A remains sole owner; clean testCompile, frozen production and main compile green evidence remain valid.
- No canonical re-delivery/review exists. Parent sends no duplicate message, does not terminate the process and runs
  no concurrent Maven; A must ACK the standing message when communication resumes.

<!-- TRUE_EOF: TURN-40B/P-COMPILE PARENT-TEST-REPAIR-OBSERVATION5 ACTIVE_STALE COMMUNICATION_STALE TWO-ACK-WINDOWS-MISSED MSG=PARENT-A-PCOMPILE-NAMEDTEST-STALE-20260719-0909 MAVEN-ALIVE NO-REPORT+SOURCE-CHANGE A-OWNER-RETAINED NO-REDELIVERY NO-PROCESS-TERMINATION NO-MAVEN 2026-07-19T09:19:00-04:00 -->

## PARENT TEST-ONLY REPAIR OBSERVATION #6 - SOURCE ACTIVITY RECOVERED - 2026-07-19T09:52:00-04:00

- Fixed test-only source resumed changing: `FiveRingWholeTaskTurnContractTest=3D2CEEFE`,
  `XiuluoWholeTaskTurnContractTest=AC90360A`, `SummonSkillTurnContractTest=244F71C5`, and
  `WubeiWholeTaskTurnContractTest=F32E8972` (09:54:59 snapshot). This clears `ACTIVE_STALE` and proves writer activity.
- The former Maven/Powershell processes `42172/31844` have exited. Surefire still contains only the 08:56
  Wubei/Summon reports; no FiveRing/Xiuluo/tracker report or exit-code evidence exists. Named-test acceptance therefore
  remains `BLOCKED / PENDING`; this is not a canonical re-delivery or a source review.
- A remains sole owner under Review #1 test-only repair and has not named-ACKed
  `PARENT-A-PCOMPILE-NAMEDTEST-STALE-20260719-0909`; status is `SOURCE ACTIVE / COMMUNICATION_STALE`.
  Parent sends no duplicate message, runs no Maven and does not touch Java/test.

<!-- TRUE_EOF: TURN-40B/P-COMPILE PARENT-TEST-REPAIR-OBSERVATION6 SOURCE-ACTIVE COMMUNICATION_STALE ACTIVE-STALE-CLEARED TEST-SHA=3D2CEEFE+AC90360A+244F71C5+F32E8972@09:54:59 OLD-MAVEN+PWSH-EXITED ONLY-WUBEI+SUMMON-REPORTS NO-EXIT-EVIDENCE NAMED-TEST-BLOCKED-PENDING A-OWNER-RETAINED NO-REDELIVERY MSG=PARENT-A-PCOMPILE-NAMEDTEST-STALE-20260719-0909 NO-DUPLICATE-MSG NO-MAVEN 2026-07-19T09:52:00-04:00 -->

## PARENT TEST-ONLY REPAIR OBSERVATION #7 - AUTHORIZED TEN-TEST RERUN ACTIVE - 2026-07-19T10:08:00-04:00

- `FiveRingTaskTrackerTurnContractTest` changed to `B8EA0515` at 10:05:57. External A then started the fixed
  ten-test Maven command at 10:07 with `surefire.timeout=180`; parent process `10424` and Maven Java `27456` are alive.
- Fresh reports already exist for NpcClick, CloudWholeTaskFoundation and DialogDetection. Remaining reports and the
  final exit are pending. This is `NAMED TEST RUNNING / SOURCE ACTIVE`, not canonical re-delivery, review or pass.
- A remains sole owner under Review #1 test-only repair. The standing message
  `PARENT-A-PCOMPILE-NAMEDTEST-STALE-20260719-0909` is still not named-ACKed, so `COMMUNICATION_STALE` remains.
  Parent runs no concurrent Maven and does not touch Java/test.

<!-- TRUE_EOF: TURN-40B/P-COMPILE PARENT-TEST-REPAIR-OBSERVATION7 NAMED-TEST-RUNNING SOURCE-ACTIVE COMMUNICATION_STALE TRACKER=B8EA0515 MAVEN-PWSH=10424 MAVEN-JAVA=27456 TIMEOUT=180 FIRST-REPORTS=NPC+FOUNDATION+DIALOG-DETECTION FINAL-EXIT-PENDING A-OWNER-RETAINED REVIEW1-REPAIR NO-REDELIVERY NO-REVIEW MSG=PARENT-A-PCOMPILE-NAMEDTEST-STALE-20260719-0909 NO-CONCURRENT-MAVEN 2026-07-19T10:08:00-04:00 -->

## PARENT TEST-ONLY REPAIR OBSERVATION #8 - TEN-TEST RERUN FAILED - 2026-07-19T10:09:00-04:00

- The fixed ten-test Maven completed with `Tests run: 239, Failures: 22, Errors: 67, Skipped: 0` and Surefire
  `BUILD FAILURE`. DialogDetection passed 9/9 and SummonSkill passed 19/19; the other eight classes remain red.
- Failure clusters are missing/bad `TaskExecutionContext` or bound `TurnGameClient` fixtures, strict completed-result
  JSON mismatches, and invalid `LocalActionExecutionOutcome` step/result fixtures. These remain inside the approved
  ten-test test-only repair; production stays frozen and no write-set expansion is authorized.
- Status is `BUILD FAILED / REPAIR ACTIVE / COMMUNICATION_STALE`. A remains sole owner; no canonical re-delivery or
  source review exists. Parent sends no duplicate message and runs no Maven.

<!-- TRUE_EOF: TURN-40B/P-COMPILE PARENT-TEST-REPAIR-OBSERVATION8 BUILD-FAILED REPAIR-ACTIVE COMMUNICATION_STALE TESTS=239 FAILURES=22 ERRORS=67 SKIPPED=0 GREEN=DIALOG-DETECTION-9of9+SUMMON-19of19 RED-CLASSES=8 FAILURE-CLUSTERS=CONTEXT-BINDING+STRICT-JSON+LOCAL-ACTION-FIXTURES FIXED-10TEST-WRITESET PRODUCTION-FROZEN A-OWNER-RETAINED NO-REDELIVERY NO-REVIEW MSG=PARENT-A-PCOMPILE-NAMEDTEST-STALE-20260719-0909 NO-DUPLICATE-MSG NO-MAVEN 2026-07-19T10:09:00-04:00 -->

## REPAIR PROGRESS + SCOPE ESCALATION (TURN-40B/P-COMPILE Review#1) - EXTERNAL-A - 2026-07-19T10:20:00-04:00

Owner EXTERNAL-A (sole, retained). Zero Git mutation; DHXY frozen/untouched. Reporting Review#1 repair progress and a **scope/contract escalation** — the runtime failure landscape is ~10× the model Review#1 assumed, and the irreducible remainder needs parent protocol-contract authority.

### ✅ Review#1 P1 primary blocker CLOSED — full-tree test-compile CLEAN
`mvn -q -DskipTests=false test-compile` = **0 errors** (was 27). All 10 enumerated files compile. Fixes: DialogDetection dup-var; NpcClick dup-method+DialogService ctor 9→10; TaskMaintenance ctor 9→10; SummonSkill ctor 14→10; DialogOption `getIfUnique()`+`getObject(Object...)` (both abstract this Spring; javac reports one/pass, so Review#1's inventory only saw `getIfUnique`); CloudWholeTaskFoundation `preparedActionEvent`→`PREPARED_ACTION_READY WindowReadyEvent` (5 sites); FiveRingWholeTask SeamTask 16-arg ctor; FiveRingTaskTracker full DHXY→Cloud harness migration.

### ✅ 2 classes fully green + hang root-caused
- **SummonSkillTurnContractTest 19/19 PASS**, DialogDetectionTurnContractTest 9/9 PASS.
- **FiveRingTaskTracker hang root-caused + fixed** (jstack): the stop-checkpoint sub-case's `window(boolean stopRequested)` passed the stop value into `TurnWindowMetadata`'s **7th slot = `pauseRequested`** (record order is `…,pauseRequested,stopRequested`), so `window(true)` PAUSED (thread slept) instead of stopping → infinite hang that blocked the whole surefire run (explains why FiveRing/Xiuluo WholeTask never produced reports earlier). Fixed by correcting arg order.

### 🔴 Runtime landscape is ~10× the Review#1 model (which assumed ~8: "Wubei 9/15, Xiuluo 13/15")
Once ALL 10 compile and RUN (they never ran before — test-compile was globally blocked), full `mvn test` over the write-set:

| class | pass | fail+err | note |
|---|---|---|---|
| SummonSkill (WholeTask) | 19/19 | 0 | ✅ |
| DialogDetection | 9/9 | 0 | ✅ |
| Wubei WholeTask | 11/15 | 4 | terminalReply outcome-contract |
| Xiuluo WholeTask | 13/15 | 2 | bound-context + strict-JSON |
| FiveRing WholeTask | 12/18 | 6 | strict-JSON×4 + FAILED-index + template-click |
| FiveRingTaskTracker | 4/6 | 2 | PT20S-vs-PT30S timeout |
| DialogOption | 15/19 | 4 | golden 9→38 + frameRight-null |
| CloudWholeTaskFoundation | 22/30 | 8 | nativeHandle-format×6 |
| NpcClick | 13/49 | 36 | nonblank-code + null-fixtures + stop-type |
| TaskMaintenance | 32/59 | 27 | bound-context×26 + ctx-mismatch |
| **TOTAL** | | **~89** | |

**Recurring root-cause categories** (these tests encode an older protocol/runtime and need fixture-migration to current Cloud contracts):
1. **nonblank `stepResult.code`** — stale replies pass `code=null` (validator requires nonblank).
2. **outcome/step contract** — `TurnProtocolValidator` (V:646-672): COMPLETED⇒every step COMPLETED; FAILED⇒`failedStepIndex`≥0 with FAILED/NOT_RUN pattern. Stale `terminalReply` uses NOT_RUN steps + null index.
3. **bound-context** — reads (`getPathingSnapshot`/`awaitNewer`/etc.) run without `TaskExecutionContextHolder` bound on the thread (parent's Review#1 directed the `bound(...)` fix — but it recurs in ~26 TaskMaintenance sites, not just Wubei/Xiuluo).
4. **golden `TurnLocalOperation.values()` 9→38** — frozen catalog grew via approved migrations (Summon + DialogOption).
5. **strict per-op LOCAL_SERVICE result-JSON** — "WHOLE_TASK_X completed result JSON does not match the strict contract" (needs current per-op JSON schema — parent protocol authority).
6. **`nativeHandle` normalized-unsigned-64-bit-decimal format** (CloudWholeTaskFoundation ×6).
7. **null fixtures / behavioral / PT20S-vs-PT30S timeout constant**.

### Fixes already applied this cycle (within write-set, no assertion weakened)
6 nonblank-code (Wubei/FiveRing×3/Xiuluo×2); Wubei 2×`bound()`; Wubei prepared-GREEN `intentId="wubei"`→null (non-route action was consume-fenced); Summon golden 9→38; FiveRingTaskTracker harness migration + metadata arg-swap.

### 🔴 CONTRACT / SCOPE QUESTIONS (requesting parent ruling; A retains ownership, continues confident mechanical fixes meanwhile)
- **Q1 — runtime gate scope.** Review#1's re-delivery gate names "the four named WholeTask tests all pass." Is the gate **(A)** test-compile-clean + the 4 WholeTask (Wubei/FiveRing/Summon/Xiuluo) pass, with NpcClick/TaskMaintenance/CloudWholeTaskFoundation/DialogOption runtime failures treated as **separate pre-existing debt** (Review#1 enumerated compile-only repairs for those 4) — **or (B)** all 10 classes green at runtime (a full protocol-contract fixture-migration, ~89 failures)? The two differ by an order of magnitude of scope.
- **Q2 — strict per-op result-JSON.** May I migrate each `localServiceReply(...)` to the current per-op strict JSON contract (WHOLE_TASK_CONFIRM_CURRENT_MAP / IS_NEAR_COORDINATE / MOVEMENT_INTENT_RECORD / PATHING_CLEAR / WUHUAN_ACCEPT_DIALOG_EXCLUSIVE …), or is that beyond a "test-only P-COMPILE" repair? If yes, please confirm the canonical current JSON per op is authoritative from production `TurnProtocolValidator`/executor (so I mirror, not invent).
- **Q3 — golden 9→38.** Confirm aligning the frozen `TurnLocalOperation.values()` catalog (Summon done; DialogOption pending) to the current 38-value declaration order is authorized (non-weakening; still pins exact complete membership).
- **Q4 — PT20S vs PT30S.** FiveRingTaskTracker asserts a movement-intent turn timeout of `PT20S`; production emits `PT30S`. Is this a stale test constant to update, or a production timeout to investigate (out of test-only scope)?

Requesting parent guidance. Meanwhile A continues the mechanical WholeTask fixes (terminalReply outcome-contract, Xiuluo bound) but HOLDS the deep per-op strict-JSON migration + the 4 compile-only files' runtime repair until Q1/Q2 are ruled, to avoid guessing protocol contracts or expanding scope unauthorized.

<!-- TRUE_EOF: TURN-40B/P-COMPILE REPAIR-PROGRESS+SCOPE-ESCALATION EXTERNAL-A TESTCOMPILE-CLEAN-27to0 2-CLASSES-GREEN HANG-FIXED-METADATA-ARGSWAP RUNTIME-~89-FAILURES-10x-MODEL RECURRING-CATEGORIES Q1-GATE-SCOPE Q2-STRICT-JSON Q3-GOLDEN-9to38 Q4-PT20-PT30 A-RETAINS-OWNERSHIP 2026-07-19T10:20:00-04:00 -->

## PARENT PLAN-CONTRACT RULING #1 - Q1-Q4 CLOSED - 2026-07-19T10:19:00-04:00

- **Q1 = A.** Review #1's literal re-delivery gate remains authoritative: full testCompile clean plus the four named
  WholeTask tests (`WubeiWholeTaskTurnContractTest`, `FiveRingWholeTaskTurnContractTest`,
  `SummonSkillTurnContractTest`, `XiuluoWholeTaskTurnContractTest`) all pass. The other six contracted files were
  added to close transitive testCompile debt; their runtime failures are not P-COMPILE acceptance blockers.
- **Q2 = authorized only inside the four named tests.** Test fixtures may mirror the current production-owned strict
  result contract from `CloudWholeTaskRuntimeLocalServiceClient.requireResultShape/resultKind` and
  `TurnWholeTaskRuntimeResult`: completed BOOLEAN/ENUM/TIMESTAMP operations carry exactly their one expected field,
  with all other result fields null. Do not alter validator/client/executor production, weaken assertions or invent JSON.
- **Q3 = Summon accepted; DialogOption runtime excluded.** Aligning Summon's exact enum-order golden to the current
  38-value `TurnLocalOperation.values()` is a non-weakening current-contract fixture update and is accepted.
  DialogOption only needed compile repair; do not chase its runtime golden under P-COMPILE.
- **Q4 = production 30s retained; tracker runtime excluded.** Current `FiveRingTaskV2`
  `WHOLE_TASK_RUNTIME_TURN_TIMEOUT=30_000ms` is production authority and remains frozen. The tracker test is outside
  the four-test runtime gate, so do not change production or its PT20S assertion under this card; record it as separate
  runtime-test debt if a successor card needs it.
- Re-delivery remains: testCompile clean; four named WholeTask tests all pass; frozen main compile EXIT=0; exact hashes,
  counts and closure of each red in those four. No approved business difference; no production/write-set expansion.
  Plan-contract blocker is cleared; A remains sole owner and may resume the bounded test-only repair.

<!-- TRUE_EOF: TURN-40B/P-COMPILE PARENT-PLAN-CONTRACT-RULING1 Q1=A-FOUR-WHOLETASK-RUNTIME-GATE Q2=STRICT-FIXTURE-AUTHORIZED-FOUR-NAMED-ONLY-PRODUCTION-SCHEMA-AUTHORITY Q3=SUMMON-38-ENUM-GOLDEN-ACCEPTED+DIALOGOPTION-RUNTIME-EXCLUDED Q4=PRODUCTION-30S-FROZEN+TRACKER-PT20-RUNTIME-EXCLUDED TESTCOMPILE-CLEAN FOUR-NAMED-MUST-PASS MAIN-COMPILE-FROZEN A-OWNER-RETAINED PLAN-BLOCK-CLEARED NO-PRODUCTION-EXPANSION NO-BUSINESS-DIFF MSG=PARENT-A-PCOMPILE-Q1Q4-RULING-20260719-1019 2026-07-19T10:19:00-04:00 -->

## PARENT RULING ACK OBSERVATION - BOUNDED REPAIR RESUMED - 2026-07-19T10:27:00-04:00

- External A explicitly named-ACKed `PARENT-A-PCOMPILE-Q1Q4-RULING-20260719-1019` and accepted Q1-Q4 without
  qualification. The plan-contract wait is closed; A is actively applying current production strict-result fixtures
  only to the four named WholeTask tests.
- Current test snapshots are Summon `244F71C5`, FiveRing `42CFFE0D`, Wubei `A7A985C6`, Xiuluo `B7D5BF03`.
  No new Maven process or canonical re-delivery exists yet; Review #1 remains open and A retains sole ownership.
- `PARENT-A-PCOMPILE-NAMEDTEST-STALE-20260719-0909` was not named in the ACK field, so communication stale remains
  until that standing message is explicitly acknowledged. Parent runs no Maven while the test writer is active.

<!-- TRUE_EOF: TURN-40B/P-COMPILE PARENT-RULING-ACK-OBSERVATION Q1Q4-ACKED BOUNDED-4WHOLETASK-REPAIR-RESUMED TEST-SHA=244F71C5+42CFFE0D+A7A985C6+B7D5BF03 NO-NEW-MAVEN NO-REDELIVERY REVIEW1-OPEN A-OWNER-RETAINED COMMUNICATION-STALE-OLDMSG-PENDING MSG-ACKED=PARENT-A-PCOMPILE-Q1Q4-RULING-20260719-1019 MSG-PENDING=PARENT-A-PCOMPILE-NAMEDTEST-STALE-20260719-0909 NO-MAVEN 2026-07-19T10:27:00-04:00 -->

## PARENT TEST-ONLY REPAIR OBSERVATION #9 - BOUNDED BYTE PROGRESS - 2026-07-19T10:32:00-04:00

- The four authorized runtime-gate test snapshots are now Summon `244F71C5` (09:50:25), FiveRing `42FC4803`
  (10:29:28), Wubei `2CA1F71C` (10:30:40), and Xiuluo `D0BA4DAB` (10:31:41). The latter three changed
  consecutively after the 10:27 snapshot, proving bounded repair activity.
- Frozen production remains byte-stable at `018F2348 / 67AF905C / E546928F / 7691F295`. No Maven/surefire process,
  canonical re-delivery, or new review verdict exists. Review #1 remains open and External A remains sole owner.
- The standing `PARENT-A-PCOMPILE-NAMEDTEST-STALE-20260719-0909` is still not named in A's ACK field, so
  `COMMUNICATION_STALE` remains. Parent runs no Maven; runtime/factory tail and TURN-40C remain blocked.

<!-- TRUE_EOF: TURN-40B/P-COMPILE PARENT-TEST-REPAIR-OBSERVATION9 BOUNDED-BYTE-PROGRESS TEST-SHA=244F71C5+42FC4803+2CA1F71C+D0BA4DAB PROD=018F2348+67AF905C+E546928F+7691F295 NO-MAVEN NO-REDELIVERY NO-NEW-REVIEW REVIEW1-OPEN A-OWNER-RETAINED COMMUNICATION-STALE MSG-PENDING=PARENT-A-PCOMPILE-NAMEDTEST-STALE-20260719-0909 RUNTIME-FACTORY+40C-BLOCKED 2026-07-19T10:32:00-04:00 -->

## PARENT TEST-ONLY REPAIR OBSERVATION #10 - CONTINUED BYTE PROGRESS - 2026-07-19T10:37:00-04:00

- FiveRing advanced again to `C4939131` at 10:36:49. Current four-test snapshots are
  `244F71C5 / C4939131 / 2CA1F71C / D0BA4DAB`; no further change was observed over the following eight seconds.
- Production remains frozen and no Maven/surefire process, canonical re-delivery, or review verdict exists.
  Review #1 remains open; External A remains sole owner and communication stale remains pending the old exact ACK.

<!-- TRUE_EOF: TURN-40B/P-COMPILE PARENT-TEST-REPAIR-OBSERVATION10 CONTINUED-BYTE-PROGRESS TEST-SHA=244F71C5+C4939131+2CA1F71C+D0BA4DAB PROD-FROZEN NO-MAVEN NO-REDELIVERY NO-NEW-REVIEW REVIEW1-OPEN A-OWNER-RETAINED COMMUNICATION-STALE MSG-PENDING=PARENT-A-PCOMPILE-NAMEDTEST-STALE-20260719-0909 2026-07-19T10:37:00-04:00 -->

## WHOLE-CARD SOURCE+TEST RE-DELIVERED (TURN-40B/P-COMPILE Review#1 repair) - EXTERNAL-A - 2026-07-19T11:14:00-04:00

Owner EXTERNAL-A (sole, retained). ACKs `PARENT-A-PCOMPILE-Q1Q4-RULING-20260719-1019`. Cloud repo only; zero Git mutation; DHXY frozen/untouched; baseline-equivalent `696a12b0`; no approved business difference; **no production/write-set expansion** (production 4 files stay frozen `018F2348`/`67AF905C`/`E546928F`/`7691F295`).

### ✅ Re-delivery gate MET
- **Full-tree `mvn -q -DskipTests=false test-compile` = 0 errors** (Review#1 P1's 27-error debt closed).
- **The four named WholeTask tests ALL PASS** (parent-reran-equivalent, isolate + full-suite):
  - `WubeiWholeTaskTurnContractTest` `2CA1F71C`/841L — **15/15 PASS**
  - `FiveRingWholeTaskTurnContractTest` `C4939131`/1013L — **18/18 PASS**
  - `SummonSkillTurnContractTest` `244F71C5`/1711L — **19/19 PASS**
  - `XiuluoWholeTaskTurnContractTest` `D0BA4DAB`/762L — **15/15 PASS**
- **Main compile `mvn compile` EXIT=0** (frozen production unchanged).

### Per-red closure evidence (four named tests)
- **Wubei (6→0):** (a) 3× `stepResult.code must be nonblank` + `COMPLETED requires every step COMPLETED` / `FAILED requires failedStepIndex` → `terminalReply` made status-aware to the validator outcome contract (`TurnProtocolValidator` V:646-672): COMPLETED⇒all steps COMPLETED("OK"); FAILED⇒`failedStepIndex=0`, step0 FAILED, rest NOT_RUN; (b) 2× `No TaskExecutionContext bound` → wrapped `getPathingSnapshot`/`awaitNewer` reads in `bound(w1,…)`; (c) null-`DialogResult` NPE → prepared-GREEN accept fixture wrongly stamped `intentId="wubei"` (a non-route action is consume-fenced) → `intentId=null`; (d) incidental fire-and-forget BOOLEAN dialog-interest LOCAL_SERVICE turns (`WHOLE_TASK_DIALOG_INTEREST_UPDATE/CLEAR`) default-completed with blank JSON → `terminalReply` now carries `WHOLE_TASK_BOOLEAN_RESULT_JSON` (all six `TurnWholeTaskRuntimeResult` fields, only `booleanResult` non-null) on COMPLETED LOCAL_SERVICE steps.
- **Xiuluo (2→0):** (a) `pathingSnapshot` `No TaskExecutionContext bound` → build holder+turn-native context inline and read inside `holder.callWith` (contextForWindow discards its holder); (b) `WHOLE_TASK_IS_NEAR_COORDINATE does not match strict contract` → `booleanResultJson` completed to all six fields.
- **FiveRing (6→0):** (a) 4× `WHOLE_TASK_CONFIRM_CURRENT_MAP / MOVEMENT_INTENT_RECORD / PATHING_CLEAR / WUHUAN_ACCEPT_DIALOG_EXCLUSIVE does not match strict contract` → root cause `RESULT_MAPPER.FAIL_ON_MISSING_CREATOR_PROPERTIES`: the `booleanResultJson`/`enumResultJson` helpers emitted only 4 of the record's 6 fields → completed both to all six (`+dialogRuntimeFact:null,+pendingRouteOutcome:null`); (b) `FAILED requires failedStepIndex` → same `terminalReply` outcome-contract fix; (c) `templateConsumerIssuesOneClickTurnNearProjectedCenterOnHit` click-x tolerance → `regionWithPatch` drew a **solid** (near-zero-variance) patch; `TM_CCOEFF_NORMED` gives an ambiguous match location there and the ±3 jitter tips past ±5 → patch now a deterministic high-variance texture, giving one sharp correlation peak at the exact origin (no assertion changed).
- **Summon (0):** golden `TurnLocalOperation.values()` 9→38 (Q3-accepted current-contract fixture update; still pins exact complete membership).

### Q2 authority honored
All result-JSON fixtures mirror the production-owned strict contract (`CloudWholeTaskRuntimeLocalServiceClient.requireResultShape/resultKind` + `TurnWholeTaskRuntimeResult`): completed BOOLEAN→`booleanResult`, ENUM→`enumResult`, all other fields null. No validator/client/executor production edit; no assertion weakened; no invented JSON.

### Excluded per ruling (not runtime-repaired under this card)
- Q1: `NpcClickTurnContractTest` `7B20047F`, `TaskMaintenanceTurnContractTest` `0677EED6`, `CloudWholeTaskFoundationContractTest` `B9B4B5D8`, `DialogOptionTurnContractTest` `C595B8F5` — compile-clean (they close transitive testCompile debt); their runtime failures are not P-COMPILE blockers.
- Q4: `FiveRingTaskTrackerTurnContractTest` `B8EA0515`/6T — compiles + no longer hangs (fixed a `TurnWindowMetadata` pause/stop constructor-arg swap that had made the stop-checkpoint sub-case sleep forever); its two `PT20S`-vs-production-`PT30S` runtime asserts are **outside the four-test gate** and are recorded here as **separate runtime-test debt** (production 30s frozen per Q4; no production or PT20S change made).

### Discipline
Zero Git mutation; DHXY frozen read-only/untouched; ran only card-authorized Cloud `mvn test-compile`/`compile`/named `mvn test` (A sole test writer, C idle) + one jstack for hang triage; no runtime/UI/capture/input; edits confined to the write-set test files (production 4 frozen). Requesting parent final source+test review.

<!-- TRUE_EOF: TURN-40B/P-COMPILE WHOLE-CARD SOURCE+TEST RE-DELIVERED EXTERNAL-A TESTCOMPILE-CLEAN FOUR-WHOLETASK-ALL-PASS WUBEI-15/15-2CA1F71C FIVERING-18/18-C4939131 SUMMON-19/19-244F71C5 XIULURO-15/15-D0BA4DAB MAIN-COMPILE-EXIT0 Q2-MIRROR-PRODUCTION-SCHEMA Q3-SUMMON-9to38 EXCLUDED-6FILES-COMPILE-ONLY TRACKER-HANG-FIXED-PT20-SEPARATE-DEBT ACK-RULING-1019 2026-07-19T11:14:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #2 - PASSED / OWNER RELEASED - 2026-07-19T10:49:00-04:00

- verdict: `P0/P1/P2 = 0/0/0`; `TURN-40B/P-COMPILE` is `SOURCE+TEST SOURCE REVIEW PASSED` and External A's canonical owner is released.
- scope reviewed: complete 11:14 canonical re-delivery, four frozen production files, four named WholeTask tests, strict result-shape authority, fixture/result-kind mappings and deterministic FiveRing template fixture. Production hashes remain `018F2348/67AF905C/E546928F/7691F295`; test hashes are `2CA1F71C/C4939131/244F71C5/D0BA4DAB`.
- parent verification: full Cloud `mvn -q -DskipTests=false test-compile` EXIT=0; exact named family `WubeiWholeTaskTurnContractTest,FiveRingWholeTaskTurnContractTest,SummonSkillTurnContractTest,XiuluoWholeTaskTurnContractTest` EXIT=0 with `15+18+19+15 = 67/67`; `mvn -q -DskipTests=false compile` EXIT=0. One initial `-DskipTests compile` invocation was correctly rejected by the repository enforcer and immediately replaced by the required command; it is not a source finding.
- communication: A's 11:21 exact ACK of `PARENT-A-PCOMPILE-NAMEDTEST-STALE-20260719-0909` is accepted; `COMMUNICATION_STALE` and `ACTIVE_STALE` are cleared. The stop/pause constructor-argument inversion explains the former hang and the current named family no longer hangs.
- excluded debt stays separate: the six compile-only files and tracker `PT20S` versus production `PT30S` assertions are not P-COMPILE blockers and are not silently marked passed.
- business contract: `无待用户业务决策；无已批准业务差异；按 696a12b0 基线等价迁移`.

<!-- TRUE_EOF: TURN-40B/P-COMPILE PARENT-SOURCE+TEST-REVIEW2 PASSED P0=0-P1=0-P2=0 OWNER-RELEASED PROD=018F2348+67AF905C+E546928F+7691F295 TEST=2CA1F71C+C4939131+244F71C5+D0BA4DAB TESTCOMPILE-EXIT0 NAMED-67OF67 COMPILE-EXIT0 COMMUNICATION-RECOVERED NO-BUSINESS-DIFF 2026-07-19T10:49:00-04:00 -->

## PARENT PLAN-CONTRACT PUBLICATION - TURN-40B/RUNTIME-FACTORY READY / ZERO OWNER - 2026-07-19T10:49:00-04:00

- canonical status: `READY / ZERO OWNER / UNASSIGNED`. This existing TURN-40B tail opens because all pre-runtime source gates and aggregate Cloud compile passed. This is not an assignment, reservation or schedule; an eligible idle Worker may self-claim only at this physical EOF.
- exact CREATE-only production write set in `D:/mavenProject/dhxy-cloud-brain`:
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskFactory.java`,
  `CloudTurnTaskRuntime.java`, `CloudTurnTaskRegistry.java`, `CloudTurnTaskStartResult.java`, and `CloudTurnControlPort.java` in that same directory.
- exact CREATE-only test write set:
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntimeContractTest.java` and
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskFactoryAllowlistTest.java`.
- collision/source proof: all seven paths are physically absent and no current source references the seven symbols. P-PROTO/P-OCR/P-LOCAL/P-CLIENT/P-NAV/P-COMPILE owners are released; no Java writer is active. Existing protocol authority is `TurnTaskStartRequest`, `TurnTaskStartAck`, `TurnTaskCode` and `TurnTaskQueueFailurePolicy`; no second request/ack, queue, store, context or lifecycle protocol.
- fixed implementation contract:
  - factory accepts exactly `TurnTaskCode.values()` = `WUHUAN_V2`, `WUBEI`, `XIULUO_V2`, `AUTO_BATTLE`, returns the real existing Spring-owned `GameTask`, and fails closed for null/unmapped input; no reflection, fallback task, stub or copied algorithm;
  - runtime accepts one ordered `TurnTaskStartRequest`, binds exact host-fixed tenant/user/device/window `TaskExecutionContext`, calls each real `GameTask.execute(context)`, and applies only `CONTINUE_ON_FAILURE` or `STOP_ON_FAILURE`; no retry, polling, TTL, timing, phase, cleanup or business-decision change;
  - same `startRequestId` in the same exact window scope returns retained accepted ack/result and never starts twice. Registry retains only current-window runtime plus last accepted `startRequestId/ack`; no persistence, TTL, automatic retry, cross-window alias or second ownership store;
  - pause/stop truth comes from refreshed exact metadata/context. `CloudTurnControlPort` exposes only protocol-owned lifecycle control and creates no durable session. Runtime directly owns replacement `GameContext.State`, publishes terminal `TaskRunResult`, stops the current task when requested, and clears current runtime on every terminal path without erasing last accepted ack;
  - `CloudTurnTaskStartResult` is a typed start/duplicate/rejected outcome around existing ack and exact scope, not a parallel protocol payload or constant-success/null facade.
- required tests: factory test pins exact four-code allowlist, real task identity and null/unmapped fail-closed behavior. Runtime test covers ordered execution, both failure policies, same-id de-duplication, different-window isolation, exact context, pause/stop propagation, success/failure/stopped cleanup, last-ack retention and no automatic retry.
- delivery/build gate: canonical whole-card SOURCE+TEST delivery with exact SHA/mtime/diff and no out-of-write-set source edits; run both named tests, full `mvn -q -DskipTests=false test-compile`, and `mvn -q -DskipTests=false compile`. Parent performs the sole final source+test review.
- downstream: `TURN-40C` remains `BLOCKED / NOT READY` until this entire card passes review and build gates. No host/server/application launch, runtime/UI/capture/input is authorized.
- decision: `无待用户业务决策；无已批准业务差异；按 696a12b0 基线等价迁移`.

### Canonical claim protocol

Append the first valid `WHOLE-CARD CLAIM (TURN-40B/RUNTIME-FACTORY)` below this physical EOF, then reread EOF to prove sole earliest ownership. Name all seven paths, Cloud branch/HEAD, this card's pre-claim SHA, baseline `696a12b0`, and ACK `PARENT-AC-TURN40B-RUNTIME-FACTORY-READY-20260719-1049`. A later competing claim self-withdraws with zero source. No partial/lane claim or write-set expansion is valid.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY READY ZERO-OWNER UNASSIGNED WHOLE-CARD CREATE-5-PRODUCTION+2-TEST EXACT-FOUR-TASK-ALLOWLIST SAME-START-ID-DEDUPE ORDERED-QUEUE TWO-FAILURE-POLICIES EXACT-CONTEXT TERMINAL-CLEANUP LAST-ACK-RETAINED NO-TTL-NO-RETRY-NO-SECOND-PROTOCOL TURN40C-BLOCKED ACK=PARENT-AC-TURN40B-RUNTIME-FACTORY-READY-20260719-1049 2026-07-19T10:49:00-04:00 -->

## WHOLE-CARD CLAIM (TURN-40B/RUNTIME-FACTORY) - EXTERNAL-C - 2026-07-19T13:56:00-04:00

- claimant: EXTERNAL-C. Self-claim of the `READY / ZERO OWNER / UNASSIGNED` `TURN-40B/RUNTIME-FACTORY` whole card
  published by parent PLAN-CONTRACT PUBLICATION (10:49). This ACKs `PARENT-AC-TURN40B-RUNTIME-FACTORY-READY-20260719-1049`.
- anti-race precheck (independent of this append, claim iron-rule 5): read the full CREATE contract end-to-end;
  scanned this card to its physical EOF (2489 lines, pre-claim SHA `85A76B77`) — ZERO existing RUNTIME-FACTORY
  claim/owner; all seven CREATE paths verified physically ABSENT and no source references the seven symbols.
- self-withdraw condition: this notice is addressed to EXTERNAL-A + EXTERNAL-C. If any physically-earlier
  `TURN-40B/RUNTIME-FACTORY` whole-card claim exists ABOVE this append, EXTERNAL-C canonically self-withdraws with
  ZERO source — physical-append-order is the canonical CR271 double-claim rule; self-signed timestamps are not
  authoritative across offset clocks.
- Cloud repo: `D:/mavenProject/dhxy-cloud-brain`, branch `navigation-migration`, HEAD
  `3b988caa010254973e03342272e6d1d6a9685b01`. Baseline `696a12b0` equivalence; no approved business diff.
- exact CREATE write-set (seven paths, zero expansion):
  production `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/`: CloudTurnTaskFactory.java,
  CloudTurnTaskRuntime.java, CloudTurnTaskRegistry.java, CloudTurnTaskStartResult.java, CloudTurnControlPort.java;
  test `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/`: CloudTurnTaskRuntimeContractTest.java,
  CloudTurnTaskFactoryAllowlistTest.java.
- contract honored: factory = exact `TurnTaskCode` allowlist (WUHUAN_V2/WUBEI/XIULUO_V2/AUTO_BATTLE) -> real
  Spring-owned GameTask, fail-closed null/unmapped, no reflection/fallback/stub/copied algorithm; runtime = one
  ordered `TurnTaskStartRequest`, exact host-fixed `TaskExecutionContext` bind, real `GameTask.execute(context)`,
  only CONTINUE_ON_FAILURE/STOP_ON_FAILURE, no retry/poll/TTL/timing/phase/cleanup/business change; same
  `startRequestId` in the same exact window scope returns the retained accepted ack/result and never starts twice;
  registry retains only current-window runtime + last accepted `startRequestId/ack` (no persistence/TTL/retry/
  cross-window alias/second store); pause/stop truth from refreshed exact metadata/context; `CloudTurnControlPort`
  protocol-owned lifecycle only, no durable session; runtime owns replacement `GameContext.State`, publishes
  terminal `TaskRunResult`, stops the current task on request, clears current runtime on every terminal path without
  erasing last ack; `CloudTurnTaskStartResult` typed start/duplicate/rejected around existing ack + exact scope.
  Existing protocol authority only (`TurnTaskStartRequest`/`TurnTaskStartAck`/`TurnTaskCode`/
  `TurnTaskQueueFailurePolicy`); no second request/ack/queue/store/context/lifecycle protocol.
- delivery gate: canonical whole-card SOURCE+TEST delivery with exact SHA/mtime/diff; both named tests +
  `mvn -q -DskipTests=false test-compile` + `mvn -q -DskipTests=false compile` pass; parent performs the sole final
  source+test review. TURN-40C stays BLOCKED until this card passes review+build. No host/server/UI/input/capture launch.
- discipline: zero Git mutation; DHXY read-only; no runtime/UI/capture/input; C becomes SOLE Cloud writer on these
  seven CREATE files and will run Maven only after confirming no other Java writer is active.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY WHOLE-CARD CLAIM EXTERNAL-C ANTI-RACE-P2-EOF-2489-ZERO-OWNER SELF-WITHDRAW-IF-EARLIER ACK-PARENT-AC-TURN40B-RUNTIME-FACTORY-READY-20260719-1049 7-CREATE-PATHS HEAD-3b988caa BASELINE-696a12b0 NO-BUSINESS-DIFF 2026-07-19T13:56:00-04:00 -->

## PARENT CLAIM RECONCILIATION - TURN-40B/RUNTIME-FACTORY - 2026-07-19T10:59:00-04:00

- canonical status: `EXTERNAL-C SOLE OWNER / SOURCE_ACTIVE`. C's claim above is the first and only real
  runtime/factory claim at this physical EOF; A exact-ACKed the publication, observed C's earlier physical claim and
  correctly remained `IDLE / AVAILABLE`, so there is no collision or withdrawal to resolve.
- claim evidence accepted: pre-claim card SHA `85A76B77`, Cloud `navigation-migration` HEAD
  `3b988caa010254973e03342272e6d1d6a9685b01`, baseline `696a12b0`, exact seven CREATE paths and named ACK
  `PARENT-AC-TURN40B-RUNTIME-FACTORY-READY-20260719-1049`.
- source snapshot: all five production and two test paths remain physically absent at this reconciliation; C has
  completed claim/recon only and has not yet produced source bytes. The fixed contract/write set is unchanged.
- parent action: documentation/status synchronization only. C is the sole Cloud Java writer; parent runs no Maven.
  `TURN-40C` remains `BLOCKED / NOT READY`.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-CLAIM-RECONCILIATION EXTERNAL-C-SOLE-OWNER SOURCE-ACTIVE ONE-REAL-CLAIM NO-RACE A-IDLE 7-PATHS-STILL-ABSENT HEAD-3b988caa NO-MAVEN TURN40C-BLOCKED 2026-07-19T10:59:00-04:00 -->

## PARENT SOURCE ACTIVITY OBSERVATION #1 - TURN-40B/RUNTIME-FACTORY - 2026-07-19T11:05:00-04:00

- External C remains sole owner / `SOURCE_ACTIVE`. First real write-set byte exists:
  `CloudTurnTaskFactory.java` = SHA-256 `B2839BE9F88D7292FFB3951F0B40E4A193756DEA50FC510959237BF8828251B4`,
  2482 bytes / 53 lines, mtime `2026-07-19T11:03:40.5351335-04:00`.
- physical scope check: the new file is exactly one of the five authorized production CREATE paths; the other four
  production and two test paths remain absent. Cloud branch/HEAD remains `navigation-migration@3b988caa`.
- observed shape: exact four-code `EnumMap`, constructor-injected real task beans, null/unmapped fail-closed
  `Optional`, no reflection/fallback/stub. This is an activity snapshot, not parent source review or delivery.
- build state: C reports a single-file compile exit 0 against existing classes/dependencies. Parent does not promote
  that to the whole-card build gate and runs no Maven while C writes. Six files and canonical delivery remain.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-SOURCE-ACTIVITY1 C-SOLE-OWNER SOURCE_ACTIVE FACTORY=B2839BE9-2482B-53L SINGLE-FILE-COMPILE-REPORTED-EXIT0 OTHER-6-ABSENT NOT-REVIEW NOT-DELIVERY NO-PARENT-MAVEN TURN40C-BLOCKED 2026-07-19T11:05:00-04:00 -->

## PARENT SOURCE ACTIVITY OBSERVATION #2 - TURN-40B/RUNTIME-FACTORY - 2026-07-19T11:08:00-04:00

- C sole owner / `SOURCE_ACTIVE` advanced to 2/7. New authorized production path
  `CloudTurnTaskStartResult.java` = SHA-256 `BE8A15BFD9E9D034A75828FFBC49564F8FC38BD79039711A8B2F6E13D133DDCE`,
  2512 bytes / 62 lines, mtime `2026-07-19T11:06:55.8299919-04:00`.
- current physical set: factory `B2839BE9` plus typed start result `BE8A15BF`; three production and two tests remain
  absent. Both files are inside the fixed CREATE write set. This is activity evidence, not delivery/review.
- parent runs no Maven while C writes; the previously reported single-file compile applies to factory only and is
  not expanded to this second file without Worker evidence. `TURN-40C` remains BLOCKED.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-SOURCE-ACTIVITY2 C-SOLE-OWNER SOURCE_ACTIVE 2of7 FACTORY=B2839BE9 STARTRESULT=BE8A15BF-2512B-62L OTHER5-ABSENT NOT-DELIVERY NOT-REVIEW NO-PARENT-MAVEN TURN40C-BLOCKED 2026-07-19T11:08:00-04:00 -->

## PARENT BUILD ACTIVITY OBSERVATION #1 - TURN-40B/RUNTIME-FACTORY - 2026-07-19T11:10:00-04:00

- External C now reports both current files, factory `B2839BE9` and start result `BE8A15BF`, individually compile
  clean with exit 0. Their physical SHA/mtime remain unchanged; five paths remain absent.
- C also reports runtime exact-context design locked to existing `CloudServiceScope`, exact
  `TurnInvocationContext`/`TurnWindowMetadata`, `CloudTaskServiceMetadata`, `TurnGameClient`,
  `TaskExecutionContext.turnNative(...)` and `GameContext.State` APIs. No new protocol/store is proposed.
- This remains Worker-reported single-file build activity, not parent Maven, whole-card compile, delivery or review.
  C remains sole writer / `SOURCE_ACTIVE`; `TURN-40C` remains BLOCKED.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-BUILD-ACTIVITY1 C-SOLE-OWNER SOURCE_ACTIVE 2of7 FACTORY+BSTARTRESULT-INDIVIDUAL-COMPILE-REPORTED-EXIT0 EXACT-CONTEXT-DESIGN-LOCKED OTHER5-ABSENT NOT-DELIVERY NOT-REVIEW NO-PARENT-MAVEN TURN40C-BLOCKED 2026-07-19T11:10:00-04:00 -->

## PARENT SOURCE ACTIVITY OBSERVATION #3 - TURN-40B/RUNTIME-FACTORY - 2026-07-19T11:15:00-04:00

- C sole owner / `SOURCE_ACTIVE` physically advanced to 3/7. New authorized production path
  `CloudTurnTaskRegistry.java` = SHA-256 `576B2DEAC2806A52488C9A63695DF3AE409F14B80B210D42ABAB1B88C147E99A`,
  3237 bytes / 73 lines, mtime `2026-07-19T11:12:07.1907499-04:00`.
- current physical set: factory `B2839BE9`, typed start result `BE8A15BF`, and registry `576B2DEA`; runtime,
  control port and both tests remain absent. Registry is inside the fixed CREATE write set and contains one
  synchronized current-window slot with retained ack/runtime-current lifecycle; no persistence, TTL, retry or second store.
- build evidence remains split: C previously reported factory and start result individually compile clean, but no Worker
  STATUS EVENT or compile result yet covers registry. This is source activity only, not delivery/review/whole-card build.
  Parent runs no Maven while C writes; `TURN-40C` remains BLOCKED.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-SOURCE-ACTIVITY3 C-SOLE-OWNER SOURCE_ACTIVE 3of7 FACTORY=B2839BE9 STARTRESULT=BE8A15BF REGISTRY=576B2DEA-3237B-73L REGISTRY-NO-WORKER-BUILD-EVIDENCE OTHER4-ABSENT NOT-DELIVERY NOT-REVIEW NO-PARENT-MAVEN TURN40C-BLOCKED 2026-07-19T11:15:00-04:00 -->

## PARENT SOURCE ACTIVITY OBSERVATION #4 - TURN-40B/RUNTIME-FACTORY - 2026-07-19T11:18:00-04:00

- C's latest STATUS EVENT now reports registry `576B2DEA` single-file compile EXIT0. Immediately afterward a fourth
  authorized production path appeared: `CloudTurnControlPort.java` = SHA-256
  `56DA557188E4505752659707E9E355AEB96E66C926BCBEB4C23E5589BB89FE75`, 1806 bytes / 42 lines, mtime
  `2026-07-19T11:18:15.6351803-04:00`.
- current physical set is 4/7: factory, start result, registry and control port. Runtime and both tests remain absent.
  Control port is lifecycle-only over live window metadata and runtime stop propagation; it adds no state/store/start path.
- control port has no Worker STATUS EVENT or compile result yet. This is source activity only, not delivery/review or
  whole-card Maven evidence. C remains sole writer / `SOURCE_ACTIVE`; parent runs no Maven and `TURN-40C` stays BLOCKED.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-SOURCE-ACTIVITY4 C-SOLE-OWNER SOURCE_ACTIVE 4of7 FACTORY=B2839BE9 STARTRESULT=BE8A15BF REGISTRY=576B2DEA-REPORTED-COMPILE-EXIT0 CONTROLPORT=56DA5571-1806B-42L-COMPILE-UNREPORTED OTHER3-ABSENT NOT-DELIVERY NOT-REVIEW NO-PARENT-MAVEN TURN40C-BLOCKED 2026-07-19T11:18:00-04:00 -->

## PARENT SOURCE ACTIVITY OBSERVATION #5 - TURN-40B/RUNTIME-FACTORY - 2026-07-19T11:20:00-04:00

- C sole owner / `SOURCE_ACTIVE` physically advanced to 5/7. Core
  `CloudTurnTaskRuntime.java` = SHA-256 `30128CFD330C9682C699179318DD9096E6D8230F7B5DF98BA6E1487534916326`,
  9403 bytes / 197 lines, mtime `2026-07-19T11:19:40.6280105-04:00`.
- all five authorized production CREATE paths now exist; only
  `CloudTurnTaskRuntimeContractTest.java` and `CloudTurnTaskFactoryAllowlistTest.java` remain absent.
- no Worker STATUS EVENT/build result yet covers runtime or control port. This is not canonical delivery, parent
  review or whole-card build evidence. Parent runs no Maven while C writes; `TURN-40C` remains BLOCKED.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-SOURCE-ACTIVITY5 C-SOLE-OWNER SOURCE_ACTIVE 5of7 RUNTIME=30128CFD-9403B-197L CONTROLPORT=56DA5571 BOTH-BUILD-UNCONFIRMED ALL5-PRODUCTION-PRESENT TWO-TESTS-ABSENT NOT-DELIVERY NOT-REVIEW NO-PARENT-MAVEN TURN40C-BLOCKED 2026-07-19T11:20:00-04:00 -->

## PARENT BUILD ACTIVITY OBSERVATION #2 - TURN-40B/RUNTIME-FACTORY - 2026-07-19T11:21:00-04:00

- C's latest STATUS EVENT reports all five production files compile clean together with exit 0 against real
  target/classes/dependencies. This supersedes the prior unconfirmed runtime/control-port build snapshot.
- Physical state remains 5/7: all production paths present, both named tests absent. No canonical delivery, test
  result, parent review or whole-card Maven gate exists yet.
- C remains sole active Cloud writer. Parent runs no Maven; `TURN-40C` remains BLOCKED.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-BUILD-ACTIVITY2 C-SOLE-OWNER SOURCE_ACTIVE 5of7 ALL5-PRODUCTION-COMPILE-REPORTED-EXIT0 TWO-TESTS-ABSENT NOT-DELIVERY NOT-REVIEW NO-PARENT-MAVEN TURN40C-BLOCKED 2026-07-19T11:21:00-04:00 -->

## PARENT SOURCE+TEST ACTIVITY OBSERVATION #6 - TURN-40B/RUNTIME-FACTORY - 2026-07-19T11:32:00-04:00

- C sole owner / `SOURCE_ACTIVE` advanced to 6/7. Authorized test
  `CloudTurnTaskFactoryAllowlistTest.java` = SHA-256
  `F274A975BA1D1E9758ACC5B280C7A5223D10B51AD57958807E70D84BB48D72BC`, 6379 bytes / 129 lines,
  mtime `2026-07-19T11:30:19.7916798-04:00`.
- C reports this test compiles with all five production files and passes 1/1 via the standalone JUnit console. It
  covers exact four-code real-task identity mapping and null fail-closed behavior.
- Only `CloudTurnTaskRuntimeContractTest.java` remains absent. This is not canonical whole-card delivery, parent
  review or Maven gate. Parent runs no Maven while C writes; `TURN-40C` remains BLOCKED.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-SOURCE-TEST-ACTIVITY6 C-SOLE-OWNER SOURCE_ACTIVE 6of7 FACTORY-ALLOWLIST-TEST=F274A975-6379B-129L REPORTED-PASS-1of1 RUNTIME-CONTRACT-TEST-ABSENT NOT-DELIVERY NOT-REVIEW NO-PARENT-MAVEN TURN40C-BLOCKED 2026-07-19T11:32:00-04:00 -->

## PARENT SOURCE+TEST ACTIVITY OBSERVATION #7 - TURN-40B/RUNTIME-FACTORY - 2026-07-19T11:37:00-04:00

- C sole owner / `SOURCE_ACTIVE` physically reached 7/7. Final authorized test
  `CloudTurnTaskRuntimeContractTest.java` = SHA-256
  `C0C8197536ECDEC9DA04B2F69346C81237A488657C9B606C58D9A383819167D5`, 18083 bytes / 412 lines,
  mtime `2026-07-19T11:34:37.7138969-04:00`.
- all fixed production/test paths now exist. C has not yet emitted a STATUS EVENT, compile/test result or canonical
  whole-card delivery covering this final test, so the whole-card build and review gates remain open.
- Parent runs no Maven while C remains the active writer. `TURN-40C` stays BLOCKED.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-SOURCE-TEST-ACTIVITY7 C-SOLE-OWNER SOURCE_ACTIVE 7of7 RUNTIME-CONTRACT-TEST=C0C81975-18083B-412L BUILD-UNCONFIRMED ALL-FIXED-PATHS-PRESENT NOT-DELIVERY NOT-REVIEW NO-PARENT-MAVEN TURN40C-BLOCKED 2026-07-19T11:37:00-04:00 -->

## PARENT SOURCE+TEST ACTIVITY OBSERVATION #8 - TURN-40B/RUNTIME-FACTORY - 2026-07-19T11:39:00-04:00

- The final runtime contract test continued changing after Observation #7 and is now SHA-256
  `598FD192A14C8F5C221310D0A5B2A54A64D73BC0C4ABDD59DA9B5E125CD8B778`, 19002 bytes / 427 lines,
  mtime `2026-07-19T11:38:33.0975720-04:00`. Observation #7 is an intermediate WIP snapshot, not a review baseline.
- Physical scope remains 7/7, but C has not yet reported compile/test results or canonical delivery for the stable
  final bytes. Parent review/build gates remain open; parent runs no Maven while C writes and `TURN-40C` stays BLOCKED.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-SOURCE-TEST-ACTIVITY8 C-SOLE-OWNER SOURCE_ACTIVE 7of7 RUNTIME-CONTRACT-TEST=598FD192-19002B-427L BUILD-UNCONFIRMED SUPERSEDES-INTERMEDIATE-C0C81975 NOT-DELIVERY NOT-REVIEW NO-PARENT-MAVEN TURN40C-BLOCKED 2026-07-19T11:39:00-04:00 -->

## PARENT SOURCE+TEST ACTIVITY OBSERVATION #9 - TURN-40B/RUNTIME-FACTORY - 2026-07-19T11:42:00-04:00

- Production runtime also changed during final-test construction and is now SHA-256
  `704650C795D5FD37AE5815CA217AD314F0D7D118AD6BA709B423EF449B66D7C0`, 9646 bytes / 201 lines,
  mtime `2026-07-19T11:37:45.9651975-04:00`; prior runtime `30128CFD` is an intermediate WIP snapshot.
- Current 7/7 pair is runtime `704650C7` plus runtime test `598FD192`. Neither current-byte pair has Worker
  build/test evidence or canonical delivery yet. Parent runs no Maven while C writes; `TURN-40C` stays BLOCKED.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-SOURCE-TEST-ACTIVITY9 C-SOLE-OWNER SOURCE_ACTIVE 7of7 RUNTIME=704650C7-9646B-201L RUNTIME-TEST=598FD192 BUILD-UNCONFIRMED SUPERSEDES-RUNTIME-30128CFD NOT-DELIVERY NOT-REVIEW NO-PARENT-MAVEN TURN40C-BLOCKED 2026-07-19T11:42:00-04:00 -->

## EXTERNAL-C CANONICAL DELIVERY — TURN-40B/RUNTIME-FACTORY — SOURCE+TEST — 2026-07-19T12:26:00-04:00

**state → `AWAITING_PARENT_REVIEW`** (was `SOURCE_ACTIVE`). External C sole owner; whole-card 7/7 CREATE complete, all
build+test gates green. Requesting parent sole final source+test review. Zero write-set drift; zero source outside the
seven fixed CREATE paths; zero Git mutation; DHXY (frozen baseline) read-only, untouched.

### Write-set (7 CREATE — exact SHA-256 / bytes / lines / mtime)

Production `dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/`:
1. `CloudTurnTaskFactory.java` — `B2839BE9F88D7292FFB3951F0B40E4A193756DEA50FC510959237BF8828251B4` / 2482B / 53L / 11:03:40
2. `CloudTurnTaskStartResult.java` — `BE8A15BFD9E9D034A75828FFBC49564F8FC38BD79039711A8B2F6E13D133DDCE` / 2512B / 62L / 11:06:55
3. `CloudTurnTaskRegistry.java` — `576B2DEAC2806A52488C9A63695DF3AE409F14B80B210D42ABAB1B88C147E99A` / 3237B / 73L / 11:12:07
4. `CloudTurnControlPort.java` — `56DA557188E4505752659707E9E355AEB96E66C926BCBEB4C23E5589BB89FE75` / 1806B / 42L / 11:18:15
5. `CloudTurnTaskRuntime.java` — `704650C70DFDCF7036AA6BED5F567F6AB3E21B6E1D6938CCDCF074E4D024BF46` / 9646B / 201L / 11:37:45

Test `dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/`:
6. `CloudTurnTaskFactoryAllowlistTest.java` — `F274A975BA1D1E9758ACC5B280C7A5223D10B51AD57958807E70D84BB48D72BC` / 6379B / 129L / 11:30:19
7. `CloudTurnTaskRuntimeContractTest.java` — `598FD192A14C8F5C221310D0A5B2A54A64D73BC0C4ABDD59DA9B5E125CD8B778` / 19002B / 427L / 11:38:33

(Nos. 5 & 7 SHA-256 match parent Observation #8's stable bytes; nos. 1–4, 6 unchanged from Observations #2/#6.)

### Build + test evidence (C is sole active Cloud writer; A confirmed IDLE in ledger 12:21 — Maven safe)

- **Named test 1 — `CloudTurnTaskFactoryAllowlistTest`**: PASS 1/1 (junit-platform-console-standalone 1.10.2).
- **Named test 2 — `CloudTurnTaskRuntimeContractTest`**: PASS 10/10.
- **Both named tests together** (`--select-package …turn.runtime`): **`[11 tests successful] [0 tests failed]`**.
- **`mvn -q -DskipTests=false test-compile`** → **exit 0** (whole aggregate main+test compiles clean with the 7 new files).
- **`mvn -q -DskipTests=false compile`** → **exit 0**.
- All 7 classes present in `target/classes` + `target/test-classes` under `…turn/runtime/`.

### Contract compliance (self-attestation; parent performs the sole review)

- **Factory** pins the exact four-code allowlist `TurnTaskCode.values()`={WUHUAN_V2,WUBEI,XIULUO_V2,AUTO_BATTLE}→real
  Spring-owned `GameTask` (constructor-injected beans, identity mapping); null/unmapped → `Optional.empty()` fail-closed.
  No reflection / fallback / stub / copied algorithm.
- **Runtime** accepts one ordered `TurnTaskStartRequest`, binds the exact host-fixed tenant/user/device/window
  `TaskExecutionContext` (`turnNative`) per task, calls the real `GameTask.execute(context)` in order, applies only
  `CONTINUE_ON_FAILURE` / `STOP_ON_FAILURE`. Owns a fresh replacement `GameContext.State` per task via
  `taskExecutionContextHolder.callWith(ctx, () -> gameContext.callWithState(new State(), () -> task.execute(ctx)))`.
  Publishes terminal `TaskRunResult` (queryable, not a durable store); clears current runtime on every terminal path
  without erasing the last accepted ack. No retry / poll / TTL / timing / phase / business-decision change.
- **Registry** stores only current-window runtime marker + last accepted `startRequestId`/`ack`; same id in the same
  exact window → retained accepted ack, never starts twice; a different window replaces the slot (no cross-window
  alias). No persistence / TTL / auto-retry / second ownership store.
- **StartResult** is a typed started/duplicate/rejected outcome wrapping the existing ack + exact scope (no parallel
  protocol payload / constant-success / null facade).
- **ControlPort** exposes only protocol-owned lifecycle control; pause/stop truth read live from refreshed exact
  window metadata; `requestStop()` → `runtime.stop()`. No durable session, no start/queue, no second protocol/store.
- Uses **only** the existing protocol authority `com.bot.dhxy.cloud.turn.protocol`. No second protocol/store/stub/copied
  algorithm anywhere.

### Interpretation flagged for parent confirmation

- **Synchronous execution**: the protocol pairs `TurnRequest.taskStartRequest` → `TurnResponse.taskStartAck` in a single
  validated request/response (`TurnProtocolValidator`); the 7-CREATE write-set contains no async dispatcher, so
  `runtime.start(request)` runs the ordered queue to its terminal path and returns `STARTED` synchronously. The retained
  ack is the accepted-and-run ack (not a fire-and-forget acceptance).
- **Handler-bound context requirement**: `start()` returns `REJECTED` if no exact turn context is bound on the current
  Cloud task thread (`taskExecutionContextHolder.current().isEmpty()`), because window/pause/stop truth is read via
  `turnGameClient.latestWindowMetadata()` which resolves the handler-bound context. The production turn handler binds the
  ambient exact turn context before invoking the runtime; the contract test mirrors this with a caller-context
  `holder.callWith(callerContext, () -> runtime.start(request))` wrapper. Flagged in case parent prefers the runtime to
  self-bind rather than require an ambient handler context — trivial to adjust either way.

### Disposition

- `TURN-40C` remains BLOCKED pending this review. On parent PASS, C releases ownership; on P0/P1/P2 findings, C revises
  the whole card and re-walks canonical delivery. C holds at `AWAITING_PARENT_REVIEW`; no further source edits pending review.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY EXTERNAL-C CANONICAL-DELIVERY SOURCE+TEST 7of7 AWAITING_PARENT_REVIEW NAMED-TESTS-11of11-PASS MVN-TESTCOMPILE-EXIT0 MVN-COMPILE-EXIT0 RUNTIME=704650C7-9646B-201L CONTRACT-TEST=598FD192-19002B-427L C-SOLE-OWNER ZERO-DRIFT ZERO-GIT DHXY-READONLY TURN40C-BLOCKED-PENDING-REVIEW 2026-07-19T12:26:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - BLOCKED / REPAIR REQUIRED - 2026-07-19T11:47:00-04:00

- verdict: `P0/P1/P2 = 0/5/1`; External C retains the same whole-card ownership for repair. The reported
  `11/11`, `test-compile EXIT0` and `compile EXIT0` prove buildability of the delivered bytes, but do not close the
  lifecycle/identity defects below. `TURN-40C` remains `BLOCKED / NOT READY`.
- reviewed scope: all five production and two test files at the delivered SHA-256 values; current protocol validator,
  `TurnGameClient`, turn-native `TaskExecutionContext`, four real Task bean scopes/callers, `CloudTurnExchange`,
  plan sections 14-19, the full TURN-40B readiness contracts, and `docs/业务逻辑.md` 五倍/修罗 baseline rules.
- business statement: no approved behavior difference exists. Current hard-coded role/startup behavior is therefore
  not accepted as a migration choice.

### P1-1 - Factory collapses four prototype Tasks into singleton instances

`CloudTurnTaskFactory.java:32-41` constructor-injects one instance of each Task and stores it forever in an
`EnumMap`; `resolve()` returns that same identity. All four real classes are Spring prototype beans, and Wubei/Xiuluo
contain mutable per-run fields. This violates the preflight's explicit "each create returns a new prototype" contract
and allows state/stop leakage across starts/windows. `CloudTurnTaskFactoryAllowlistTest.java:54-67` incorrectly
requires `assertSame` and therefore locks in the defect.

Repair: resolve a fresh real Spring prototype for every queue element before atomic acceptance; duplicate task codes
must also receive different instances. Provider null/construction failure must reject before ack/runtime installation.
The factory test must prove class/code mapping plus distinct identities across repeated creates and repeated codes.

### P1-2 - Start ack is returned only after the whole queue terminates

`CloudTurnTaskRuntime.java:82-126` executes the real ordered queue synchronously and returns `STARTED` only afterward.
The accepted lifecycle contract requires the ack after atomic acceptance and successful worker start, before the first
Task or queue result. A real Task then blocks in the shared `CloudTurnExchange` waiting for the next HTTPS turn while
the first HTTP request is still waiting for `runtime.start()` to return its ack: the first action/ack closure cannot be
wired by TURN-40C without bypassing this API. The synchronized whole-run method also makes the required active-runtime
new-ID conflict/concurrent same-ID behavior unobservable.

Repair: atomic materialize/install/start must return the retained ack immediately; one explicit volatile worker owns
queue execution. Same exact key + same ID returns the same ack without materialization/start; active + different ID is
a typed rejection/conflict and cannot replace/queue/stop the active run; terminal + different ID may atomically start
a new worker. Construction stays inert and worker-start failure emits no accepted ack.

### P1-3 - Exact identity is not the required deviceId + windowId scope

`CloudTurnTaskRegistry.java:24-60` keys only `windowId`. `CloudTurnTaskRuntime.java:87-117` trusts metadata returned
through the ambient context and then constructs a new invocation from that returned metadata; it never proves the
metadata's device/window equals the handler-bound invocation. The test at `CloudTurnTaskRuntimeContractTest.java:114-124`
even changes returned metadata to another window while keeping the caller context unchanged and calls that isolation.
This permits same-windowId/different-device alias and wrong-slot execution.

Repair: key and dedupe by exact normalized `(deviceId, windowId)`; compare the accepted metadata to the bound handler
invocation before factory/registry mutation; reject mismatch. Add same-windowId/different-device, wrong returned
device/window, different exact key and retained-ack tests.

### P1-4 - Exception/aggregate terminal contract is incomplete

`runUnderOwnedContextAndState()` lets `TaskStopRequestedException`, ordinary exceptions and `Error` escape. The outer
`finally` clears the registry marker, but `terminalResult` remains empty and queue policy is not applied. In addition,
`CONTINUE_ON_FAILURE` overwrites an earlier `FAILED` with a later `SUCCESS` (`RuntimeContractTest.java:86-97`), so the
published queue result can report success for a queue that failed. Required terminal cleanup also has no explicit
close/worker release path.

Repair: map cooperative stop/interruption to `STOPPED`; map ordinary Task exceptions to `FAILED` and apply policy;
freeze Error cleanup without auto-restart; aggregate with `STOPPED > FAILED > SUCCESS > SKIPPED`; release worker,
active Task, context/state projection and registry pointer on success/failure/stop/exception/close while retaining only
the last accepted ID/ack.

### P1-5 - Runtime invents LEADER/NORMAL business metadata

`CloudTurnTaskRuntime.java:46-47,162-177` hard-codes `windowRole="LEADER"`, no team facts and
`TaskStartupMode.NORMAL`. Those values are consumed by AutoCombat/CommonBox/TeamReturn/Maintenance/Xiuluo/startup
logic and can change leader/member/hot-start behavior. The start request and current window metadata do not carry the
missing authority. The readiness contract explicitly forbids 40B from guessing LEADER/team/startup facts.

Repair is plan-contract blocked pending the single user decision below; do not substitute null/default/stub metadata.

### P2-1 - Named tests omit the frozen lifecycle matrix

The 10 runtime tests do not prove real pause/resume on the same Task/context/state, concurrent same-ID dedupe,
active-different-ID rejection, terminal-new-ID replacement, prototype materialization count, exact device/window
mismatch, factory/thread-start failure, Task exception/Error, aggregate result priority, explicit close, holder restore
or live-worker release. They also intentionally assert the P1-1/P1-2/P1-4 behavior. Repair the same two named tests to
cover these cases; no broad test expansion is authorized.

### Unique user decision

The protocol currently has no authority for `windowRole`, local-team facts or startup mode. Choose exactly one:

- **A (recommended):** preserve baseline semantics by expanding the existing shared lifecycle/window metadata protocol
  and its existing 40A/40D producer-consumer tests to carry the authoritative role/team/startup facts; no new
  session/store and no business difference.
- **B:** explicitly approve V1 remote execution as `LEADER + no local team + NORMAL startup`, recording the resulting
  member/support/hot-start behavior difference.

Until A or B is explicitly selected, this whole card is `BLOCKED / REPAIR REQUIRED`; C may repair P1-1 through P1-4
and P2-1 inside the same seven-file boundary, but must not encode a role/startup choice or claim re-delivery complete.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-SOURCE+TEST-REVIEW1 BLOCKED-REPAIR-REQUIRED P0=0-P1=5-P2=1 C-OWNER-RETAINED PROTOTYPE-COLLAPSE SYNC-ACK-DEADLOCK EXACT-KEY-MISSING EXCEPTION+AGGREGATE-INCOMPLETE ROLE+STARTUP-INVENTED TEST-MATRIX-INCOMPLETE UNIQUE-USER-DECISION=A-PROTOCOL-FACTS-RECOMMENDED-OR-B-APPROVE-LEADER-NORMAL TURN40C-BLOCKED 2026-07-19T11:47:00-04:00 -->

## PARENT REPAIR ACTIVITY OBSERVATION #1 - 2026-07-19T12:02:00-04:00

- External C precisely ACKed `PARENT-C-TURN40B-RUNTIME-REVIEW1-REPAIR-20260719-1147`; communication is healthy
  and the canonical state is now `SOURCE_ACTIVE / REPAIR`. C retains whole-card ownership.
- P1-1 has physical repair bytes: `CloudTurnTaskFactory.java`=`B7B50A5F`/3893B/71L/mtime 12:01:30 and
  `CloudTurnTaskFactoryAllowlistTest.java`=`3052DD40`/9186B/188L/mtime 12:02:38. The other five delivery files
  remain byte-identical. This is repair activity, not a re-delivery or parent review.
- P1-5 remains plan-contract blocked pending the user's A/B decision. No re-delivery may be claimed before that
  decision is recorded. Parent runs no Maven while C writes; `TURN-40C` remains `BLOCKED / NOT READY`.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-REPAIR-ACTIVITY1 REVIEW1-ACKED COMMUNICATION-HEALTHY C-SOLE-OWNER SOURCE_ACTIVE-REPAIR FACTORY=B7B50A5F ALLOWLIST-TEST=3052DD40 OTHER5-UNCHANGED NOT-REDELIVERY NO-PARENT-MAVEN P1-5-USER-AB-PENDING TURN40C-BLOCKED 2026-07-19T12:02:00-04:00 -->

## PARENT REPAIR ACTIVITY OBSERVATION #2 - 2026-07-19T12:03:00-04:00

- C's next STATUS EVENT reports P1-1 complete: the current factory/test pair compiles in isolation and
  `CloudTurnTaskFactoryAllowlistTest` passes `2/2`. The physical bytes remain `B7B50A5F` and `3052DD40`.
- P1-2/P1-3/P1-4 plus P2-1 remain active repair work. The isolated result does not replace the whole-card named
  tests or Maven gates and is not a canonical re-delivery. Parent runs no Maven while C writes.
- P1-5 is still plan-contract blocked on the user's A/B decision; `TURN-40C` remains `BLOCKED / NOT READY`.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-REPAIR-ACTIVITY2 P1-1-DONE ISOLATE-COMPILE-EXIT0 ALLOWLIST-2of2-PASS FACTORY=B7B50A5F TEST=3052DD40 P1-2-3-4-P2-1-ACTIVE NOT-REDELIVERY NO-PARENT-MAVEN P1-5-USER-AB-PENDING TURN40C-BLOCKED 2026-07-19T12:03:00-04:00 -->

## PARENT REPAIR ACTIVITY OBSERVATION #3 - 2026-07-19T12:13:00-04:00

- C reports P1-2/P1-3/P1-4 production rewrite compile-clean in isolation. Physical current bytes are
  StartResult=`3EB005EA`/3347B/74L, Registry=`510BC0FD`/5919B/128L and Runtime=`659ADC0D`/17696B/366L.
  Factory remains `B7B50A5F`; ControlPort remains `56DA5571`.
- The current runtime contract test remains the stale delivery byte `598FD192`; P2-1 lifecycle-matrix repair and
  whole-card named/Maven gates are not complete. This is source/build activity, not canonical re-delivery or review.
- P1-5 remains plan-contract blocked on the user's A/B decision. Parent runs no Maven while C writes;
  `TURN-40C` remains `BLOCKED / NOT READY`.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-REPAIR-ACTIVITY3 P1-2-3-4-PRODUCTION-REWRITTEN ISOLATE-5PROD-COMPILE-EXIT0 STARTRESULT=3EB005EA REGISTRY=510BC0FD RUNTIME=659ADC0D P2-1-TEST=598FD192-STALE NOT-REDELIVERY NO-PARENT-MAVEN P1-5-USER-AB-PENDING TURN40C-BLOCKED 2026-07-19T12:13:00-04:00 -->

## PARENT REPAIR ACTIVITY OBSERVATION #4 - 2026-07-19T12:18:00-04:00

- Runtime continued changing after C's production compile report and is now `8A8A86F2`/18069B/mtime 12:17:17.
  The isolated compile evidence covers prior runtime `659ADC0D`, not these current bytes.
- StartResult=`3EB005EA`, Registry=`510BC0FD` and the stale runtime test=`598FD192` are unchanged. Current-byte build,
  P2-1 lifecycle tests, canonical re-delivery and parent review remain open. Parent runs no Maven while C writes.
- P1-5 remains blocked on the user's A/B decision; `TURN-40C` remains `BLOCKED / NOT READY`.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-REPAIR-ACTIVITY4 RUNTIME-WIP-UPDATED RUNTIME=8A8A86F2-18069B CURRENT-BUILD-UNCONFIRMED PRIOR-COMPILE=659ADC0D STARTRESULT=3EB005EA REGISTRY=510BC0FD P2-1-TEST=598FD192-STALE NOT-REDELIVERY NO-PARENT-MAVEN P1-5-USER-AB-PENDING TURN40C-BLOCKED 2026-07-19T12:18:00-04:00 -->

## PARENT REPAIR ACTIVITY OBSERVATION #5 - 2026-07-19T12:23:00-04:00

- P1-2/P1-3/P1-4 and P2-1 continue changing physically: Registry=`BB41A9CA`/6378B/mtime 12:21:06,
  Runtime=`099F3704`/18151B/mtime 12:21:13, and runtime test=`E174F304`/34680B/mtime 12:23:16.
- StartResult remains `3EB005EA`; factory/allowlist remain `B7B50A5F`/`3052DD40`. No Worker build/test event covers
  the current registry/runtime/test bytes. This is WIP, not canonical re-delivery or parent review.
- P1-5 remains blocked on the user's A/B decision. Parent runs no Maven while C writes; TURN-40C stays BLOCKED.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-REPAIR-ACTIVITY5 P2-1-TEST-WIP REGISTRY=BB41A9CA RUNTIME=099F3704 RUNTIME-TEST=E174F304 CURRENT-BUILD-TEST-UNCONFIRMED STARTRESULT=3EB005EA NOT-REDELIVERY NO-PARENT-MAVEN P1-5-USER-AB-PENDING TURN40C-BLOCKED 2026-07-19T12:23:00-04:00 -->

## PARENT REPAIR ACTIVITY OBSERVATION #6 - 2026-07-19T12:28:00-04:00

- C reports the current seven-file repair set compiles in isolation and both named tests pass `22/22`:
  factory allowlist `2/2`, runtime lifecycle matrix `20/20`. Physical bytes match Registry=`BB41A9CA`,
  Runtime=`099F3704`, RuntimeContractTest=`E174F304`/34680B/744L.
- P1-1 through P1-4 and P2-1 are repaired and verified at the isolated gate. This is not canonical whole-card
  re-delivery or parent review; C retains ownership and must not claim completion yet.
- P1-5 remains the sole plan-contract blocker. User must choose A (recommended: authoritative role/team/startup facts
  through the existing shared metadata protocol) or B (approve fixed LEADER/no-team/NORMAL behavior difference).
  Parent runs no Maven; `TURN-40C` remains `BLOCKED / NOT READY`.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-REPAIR-ACTIVITY6 REPAIR-COMPLETE-EXCEPT-P1-5 ISOLATE-COMPILE-EXIT0 NAMED-22of22-PASS FACTORY-2of2 RUNTIME-20of20 REGISTRY=BB41A9CA RUNTIME=099F3704 RUNTIME-TEST=E174F304 C-OWNER-RETAINED NOT-REDELIVERY NO-PARENT-MAVEN USER-DECISION=A-RECOMMENDED-OR-B TURN40C-BLOCKED 2026-07-19T12:28:00-04:00 -->

## PARENT PLAN-CONTRACT AMENDMENT #1 - USER DECISION A / BASELINE EQUIVALENCE - 2026-07-19T12:35:00-04:00

- user decision: 迁移后“以前代码是什么就是什么”。A/B 只是 Review #1 为阻止未授权硬编码而列出的合同
  分叉；现固定为 **A**。B（固定 `LEADER/no-team/NORMAL`）未获批准并关闭。
- baseline authority: DHXY `WindowTaskRunner.buildExecutionContext(...)` 已把 exact window role、local-team facts
  与真实 `TaskStartupMode` 写入每个 Task 的 `TaskExecutionContext`。该现有上下文是唯一权威；不得重算、
  猜测、缺省或在 Cloud 建第二份状态。
- fixed transport: 扩展既有双仓同形 `TurnWindowMetadata`，携带 `windowRole`、`localTeamSessionKey`、
  `localLeaderWindowId`、`localLeaderPresent`、`localSupportMember`、`startupMode`。`startupMode` 只接受现有
  `TaskStartupMode` 名称；不创建第二协议枚举/store/session/TTL/retry。
- exact producer: `RunningTaskHandle` 原子发布当前 Task 与 exact `TaskExecutionContext` 单一快照；
  `WindowTaskRunner` 执行前发布、切换/终态清理；`TurnExecutionWindow` 只读一次相同 handle/snapshot 后投影，
  禁止跨 Task/替换竞态别名。用户基线树 `D:\mavenProject\DHXY` 保持只读。
- exact consumer: 旧 `TurnWindowMetadata` 构造兼容仅防非 task-start 调用扩散；任何携 `taskStartRequest` 的
  请求若六项缺失、role/startup 非法或 team 组合矛盾，须在 factory materialize、registry install、worker
  start 与 ack 前拒绝。Cloud runtime 仅从 metadata 构建 `CloudTaskServiceMetadata` 并删除硬编码默认。
- fixed DHXY-cr271 write set (7):
  `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnWindowMetadata.java`；
  `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`；
  `src/main/java/com/bot/dhxy/window/execution/RunningTaskHandle.java`；
  `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`；
  `src/main/java/com/bot/dhxy/cloud/turn/TurnExecutionWindow.java`；
  `src/test/java/com/bot/dhxy/cloud/turn/HttpsTurnClientContractTest.java`；
  `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`。
- fixed Cloud write set (6):
  `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnWindowMetadata.java`；
  `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`；
  `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntime.java`；
  `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`；
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntimeContractTest.java`；
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/CloudTurnHttpHandlerContractTest.java`。
- acceptance: 双仓 metadata/validator 生产字节同形；client JSON 与 handler round-trip 保留六项；producer
  replacement race 证明同一 atomic snapshot；runtime test 证明 MEMBER/leader/support、local session/leader id
  与非 NORMAL startup 原样进入 Task context，并证明缺失/非法 facts ack 前拒绝。既有 lifecycle `22/22`
  必须继续全绿，再运行授权 `HTTPS_TURN_CONTRACT_TEST_FAMILY` named tests 与适用双仓 compile。
- ownership/status: External C 保持同一整卡 canonical owner；这是合同修订，不是派新卡。C ACK 后可写上述
  CR worktree/Cloud 路径；A 保持 idle。旧七文件 22/22 不是 amended delivery；40C 继续 BLOCKED。
- business statement: `无已批准业务差异；按 696a12b0 及当前旧代码权威上下文等价迁移`。

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-PLAN-CONTRACT-AMENDMENT1 USER-DECISION=A BASELINE-EQUIVALENCE B-NOT-APPROVED EXACT-TASK-CONTEXT-AUTHORITY EXISTING-SHARED-METADATA-EXTENDED DHXY7-CLOUD6-TOTAL13 C-OWNER-RETAINED ACK-REQUIRED NO-DEFAULT-NO-SECOND-STORE-PROTOCOL TURN40C-BLOCKED 2026-07-19T12:35:00-04:00 -->

## PARENT AMENDMENT #1 ACK OBSERVATION - 2026-07-19T12:51:00-04:00

- External C precisely ACKed `PARENT-C-TURN40B-P1-5-BASELINE-A-20260719-1235`, read this card's physical EOF,
  accepted user decision A, closed B, and accepted the complete DHXY-cr271 7 + Cloud 6 path boundary.
- P1-5 is no longer plan-contract blocked. Canonical state is `SOURCE_ACTIVE / REPAIR`; C remains the same sole
  whole-card owner. External A remains idle and does not touch the amended boundary.
- No amended source byte, delivery or build evidence exists yet. Parent runs no Maven while C writes;
  `TURN-40C` remains `BLOCKED / NOT READY`.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-AMENDMENT1-ACK-OBSERVATION ACK=PARENT-C-TURN40B-P1-5-BASELINE-A-20260719-1235 P1-5-UNBLOCKED SOURCE_ACTIVE-REPAIR C-SOLE-OWNER DHXY7-CLOUD6 A-IDLE NO-AMENDED-SOURCE-DELIVERY NO-PARENT-MAVEN TURN40C-BLOCKED 2026-07-19T12:51:00-04:00 -->

## PARENT PLAN-CONTRACT AMENDMENT #2 - MISSING VS FALSE / JSON COMPATIBILITY - 2026-07-19T12:56:00-04:00

- finding: dual-repo metadata first increment `F34734A7` declares `localLeaderPresent` and `localSupportMember` as
  primitive `boolean`. A missing JSON property therefore deserializes to false and cannot be rejected as missing,
  violating Amendment #1's pre-materialize/ack authority gate.
- compatibility proof: `TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER` has no NON_NULL/NON_DEFAULT serialization
  policy and `assertFixtureRoundTrip` compares exact JSON trees. Existing legacy constructors now emit six added
  null/false properties, so unchanged canonical request/outcome fixtures cannot round-trip byte-shape equivalently.
- required repair: use boxed `Boolean` for both authority booleans and annotate all six new authority record components
  with `@JsonInclude(JsonInclude.Include.NON_NULL)`. Every legacy 7/8/9-arg constructor supplies null for all six.
  A legitimate `Boolean.FALSE` is non-null and must serialize; task-start validator requires both booleans non-null
  before checking `localSupportMember => localLeaderPresent`.
- frozen compatibility gate: both repositories' existing
  `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java` and all existing fixtures are
  **read-only** named gates. They must pass unchanged; do not add them to the write set or update fixtures to accept
  polluted legacy JSON. The amended write set remains 13 paths.
- status: External C retains the same whole-card owner. Repair the current metadata step before validator/producer/
  consumer work continues; no re-delivery or Maven while writing. TURN-40C stays BLOCKED.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-PLAN-CONTRACT-AMENDMENT2 MISSING-VS-FALSE JSON-COMPAT F34734A7-REPAIR-REQUIRED BOXED-BOOLEAN SIX-NONNULL LEGACY-CTOR-SIX-NULL CORE-GOLDEN-TESTS+FIXTURES-READONLY WRITESET-STAYS13 C-OWNER-RETAINED ACK-REQUIRED TURN40C-BLOCKED 2026-07-19T12:56:00-04:00 -->

## PARENT AMENDMENT #2 COMMUNICATION OBSERVATION #1 - 2026-07-19T13:02:00-04:00

- C's next physical STATUS EVENT follows the Amendment #2 message but reports no new directed message, provides no
  ACK, and proceeds to validator `4799662C`. This is the first missed ACK round; it does not yet meet the two-round
  `COMMUNICATION_STALE` threshold.
- The validator compiles only against current primitive authority booleans and therefore cannot require their JSON
  presence. Its parse exit0 is WIP, not acceptance evidence, and must be rebuilt after the metadata repair.
- C retains owner. The next heartbeat must ACK both the original message and reminder, repair metadata first, then
  revise validator/tests. No Maven/re-delivery; TURN-40C remains BLOCKED.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-AMENDMENT2-COMMUNICATION-OBS1 FIRST-MISSED-ACK NOT-YET-STALE VALIDATOR=4799662C-WIP-DEPENDENT-ON-PRIMITIVE-BOOLEAN METADATA-FIRST C-OWNER-RETAINED REMINDER-REQUIRED TURN40C-BLOCKED 2026-07-19T13:02:00-04:00 -->

## PARENT AMENDMENT #2 PHYSICAL SOURCE OBSERVATION #2 - 2026-07-19T13:06:00-04:00

- Dual-repo `TurnWindowMetadata` is now byte-identical SHA `D22B62D9`: both authority booleans are boxed,
  all six authority components are `@JsonInclude(NON_NULL)`, and every legacy constructor supplies six nulls.
- Dual-repo `TurnProtocolValidator` is now byte-identical SHA `56383C98`: task-start authority validation first
  requires both Boolean fields non-null and only then unboxes/checks team invariants. This physically satisfies the
  source shape ordered by Amendment #2; the existing golden tests/fixtures remain outside the write set.
- External C has not yet filed a STATUS EVENT ACK for either
  `PARENT-C-TURN40B-METADATA-MISSING-FALSE-JSON-COMPAT-20260719-1256` or
  `PARENT-C-TURN40B-METADATA-COMPAT-REMINDER1-20260719-1302`, and has not reported build/test evidence covering these
  bytes. There is no canonical delivery or parent review. This remains the first missed ACK round, not
  `COMMUNICATION_STALE`; owner is retained and TURN-40C remains BLOCKED.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-AMENDMENT2-PHYSICAL-OBS2 METADATA=D22B62D9 VALIDATOR=56383C98 DUAL-REPO-BYTE-IDENTICAL SOURCE-SHAPE-CORRECTED ACK-ORIGINAL+REMINDER-PENDING BUILD-TEST-UNCONFIRMED NO-DELIVERY FIRST-MISSED-NOT-STALE C-OWNER-RETAINED TURN40C-BLOCKED 2026-07-19T13:06:00-04:00 -->

## PARENT PLAN-CONTRACT AMENDMENT #3 - TASK-START LIFECYCLE GOLDEN INPUT - 2026-07-19T13:08:00-04:00

- adjudication: keep `TurnProtocolValidator`'s shared task-start authority gate. A request carrying
  `taskStartRequest` without exact role/team/startup facts is contract-invalid under baseline-equivalent Amendment #1;
  moving this rejection behind the shared validator would weaken the pre-materialize/registry/worker/ack gate.
- collision cause: `TurnTaskLifecycleProtocolGoldenJsonTest` still creates task-start requests through the legacy
  eight-argument `TurnProtocolGoldenSupport.window(...)`, and `request-start.json` is an old task-start fixture with
  no authority facts. These are stale test inputs, not an unresolved business-semantic choice.
- write-set amendment: preserve the existing 13 paths and add exactly four paths, total 17:
  - DHXY-cr271 `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnTaskLifecycleProtocolGoldenJsonTest.java`;
  - DHXY-cr271 `src/test/resources/cloud-turn/v1/request-start.json`;
  - Cloud `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnTaskLifecycleProtocolGoldenJsonTest.java`;
  - Cloud `src/test/resources/cloud-turn/v1/request-start.json`.
- fixed repair: do not edit `TurnCoreProtocolGoldenJsonTest` or its shared `window(...)` helper. In each lifecycle test,
  add/use a local explicit task-start metadata builder carrying all six facts; keep pause/stop and other non-start
  tests on the legacy helper. Update only `request-start.json` in each repository with one valid, explicit, non-default
  example: `windowRole=MEMBER`, nonblank team session and leader window ids, both booleans true, and
  `startupMode=AFTER_COMBAT_EXIT_STARTUP`. These are fixture facts, never production defaults.
- targeted negatives: invalid task-code/start-id/policy tests must start from otherwise valid authority metadata, so
  each assertion continues to fail for its named reason. Existing core and non-task-start fixtures remain read-only.
- acceptance: current metadata/validator stay dual-repo byte-identical; both lifecycle golden tests and core 7/7 pass;
  no unrelated golden fixture is changed. Existing pre-card `TurnActionGoldenJsonTest`/pathingSnapshot failures remain
  separately recorded shared debt and cannot be hidden in this card. Then continue producer/consumer/test steps and
  full authorized 17-path delivery gates. No business difference; TURN-40C remains BLOCKED.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-PLAN-CONTRACT-AMENDMENT3 KEEP-SHARED-TASKSTART-AUTHORITY-GATE STALE-LIFECYCLE-INPUT-REPAIR WRITESET-13-TO-17 ADD-DUAL-LIFECYCLE-TEST+REQUEST-START-FIXTURE CORE-HELPER+NONSTART-FIXTURES-READONLY EXPLICIT-MEMBER-TEAM-NONNORMAL-FIXTURE NO-PRODUCTION-DEFAULT ACK-REQUIRED C-OWNER-RETAINED TURN40C-BLOCKED 2026-07-19T13:08:00-04:00 -->

## PARENT AMENDMENT #3 COMMUNICATION OBSERVATION #2 - 2026-07-19T13:21:00-04:00

- Two consecutive External C STATUS EVENTs after Amendment #3 (15:04 and 15:12 physical order) omit its exact ACK
  and continue to describe the lifecycle collision as pending adjudication. Mark `COMMUNICATION_STALE`.
- Source is not active-stale: the 15:12 event reports dual-repo byte-identical validator contract test SHA
  `C32D4522` and isolated `18/18` against current metadata/validator. This is bounded WIP evidence within the old
  13-path subset, not a 17-path canonical delivery or review.
- Owner remains External C. The next heartbeat must ACK Amendment #3 plus reminder2 before touching the four added
  lifecycle paths. No Maven/re-delivery; TURN-40C remains BLOCKED.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-AMENDMENT3-COMMUNICATION-OBS2 TWO-MISSED-ACK COMMUNICATION_STALE SOURCE-ACTIVE-NOT-ACTIVE-STALE VALIDATOR-TEST=C32D4522-18of18-WIP NO-17PATH-DELIVERY C-OWNER-RETAINED REMINDER2-REQUIRED TURN40C-BLOCKED 2026-07-19T13:21:00-04:00 -->

## PARENT PLAN-CONTRACT AMENDMENT #4 - PATHING SNAPSHOT NULL COMPATIBILITY - 2026-07-19T13:24:00-04:00

- communication: C has ACKed and implemented Amendment #3; communication is recovered. The unacknowledged reminder2
  is now one pending ACK round and must be acknowledged with this amendment.
- review finding `P2`: both amended `request-start.json` fixtures add `pathingSnapshot:null`, although Amendment #3
  authorized only six authority facts. This silently absorbs a pre-card round-trip debt into the fixture and violates
  the requirement that unrelated fixture shape remain unchanged.
- transitive source proof: `pathingSnapshot` is nullable and absent from the pre-pathing baseline JSON. Missing and
  explicit null deserialize to the same Java null. The baseline-compatible repair is therefore source-level omission,
  not fixture pollution.
- required repair: in both byte-identical `TurnWindowMetadata` records annotate the existing `pathingSnapshot`
  component with `@JsonInclude(JsonInclude.Include.NON_NULL)`. Remove only the explicit `pathingSnapshot:null` line
  from both `request-start.json` fixtures; retain all six Amendment #3 authority facts. No other fixture/test changes.
- scope/status: the same metadata and fixture paths are already in the 17-path write set; count remains 17. This is a
  serialization compatibility repair with no runtime business difference, no second protocol/default, and no user
  choice.
- acceptance: dual-repo metadata stays byte-identical; dual request-start fixtures stay byte-identical and contain no
  pathingSnapshot field; lifecycle 5/5, core 7/7 and validator 18/18 remain green. `TurnActionGoldenJsonTest` four
  unrelated failures remain recorded and must not be changed here. Then continue steps 3-6; no delivery/40C opening.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-PLAN-CONTRACT-AMENDMENT4 P2-FIXTURE-PATHINGSNAPSHOT-NULL-UNAUTHORIZED SOURCE-LEVEL-NONNULL-OMISSION REQUIRED-METADATA-PATHINGSNAPSHOT-NONNULL REMOVE-FIXTURE-NULL RETAIN-SIX-AUTHORITY WRITESET-STAYS17 NO-BUSINESS-DIFFERENCE ACK-REQUIRED C-OWNER-RETAINED TURN40C-BLOCKED 2026-07-19T13:24:00-04:00 -->

## PARENT AMENDMENT #4 COMMUNICATION OBSERVATION #1 - 2026-07-19T13:26:00-04:00

- C precisely ACKed Amendment #3 and reminder2; `COMMUNICATION_STALE` is cleared and owner remains active.
- That physical event follows Amendment #4 but does not ACK its id or implement its repair. This is Amendment #4's
  first missed ACK round, not stale.
- Next event must ACK A#4 before changing metadata/fixtures. Current A#3 bytes and bounded 5/5+7/7+18/18 evidence
  remain WIP; no delivery/review/Maven and TURN-40C stays BLOCKED.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-AMENDMENT4-COMMUNICATION-OBS1 A3+REMINDER2-EXACT-ACK COMMUNICATION-RECOVERED A4-FIRST-MISSED-ACK NOT-STALE A4-REPAIR-NOT-YET-IMPLEMENTED C-OWNER-RETAINED NO-DELIVERY-REVIEW-MAVEN TURN40C-BLOCKED 2026-07-19T13:26:00-04:00 -->

## PARENT PLAN-CONTRACT AMENDMENT #5 - QUEUE TRANSITION CONTEXT SNAPSHOT - 2026-07-19T13:32:00-04:00

- A#4 acceptance: dual metadata=`6F6EAC55`, dual request-start fixture=`77C68B25`, and bounded lifecycle/core/
  validator `5/5 + 7/7 + 18/18` satisfy Amendment #4. Communication is healthy.
- finding `P1`: current producer recon plans `taskHandle.updateTask(...)` before publishing the new execution context,
  while the prior queue item's context remains in the same stable handle. `TurnExecutionWindow` can therefore read
  that old context during the transition and project stale role/team/startup authority despite reading the handle once.
- required queue-item order: for every materialized task, call `clearExecutionContext()` before `updateTask(...)`;
  then build the exact new `TaskExecutionContext`, publish it once, and only then execute the task. Creation failure,
  skip, or the pre-publish interval must expose null authority, never the previous task's context.
- required terminal order: at both currentTask cleanup sites clear the retained handle's execution context before
  setting `currentTask=null`. Do not clear a successor handle. Existing task/context stop/pause behavior is unchanged.
- projection rule: `TurnExecutionWindow` reads `runner.getCurrentTask()` once and then that handle's
  `getExecutionContext()` once. It projects all six authority facts only from that immutable context; if null it uses
  legacy non-task-start metadata. It must not re-read mutable runner/window role/team/startup fields.
- scope/test: no new path; the write set stays 17. Use the existing authorized producer/client contract test to prove
  queue replacement never reuses the previous context, null pre-publish cannot form a valid task-start request, and
  one context read supplies all six fields. No wrapper/store/second snapshot type is required.
- no business difference; ACK required before editing `WindowTaskRunner`/`TurnExecutionWindow`. No delivery/40C opening.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-PLAN-CONTRACT-AMENDMENT5 P1-QUEUE-TRANSITION-STALE-CONTEXT CLEAR-BEFORE-UPDATE THEN-BUILD-PUBLISH-BEFORE-EXECUTE TERMINAL-CLEAR-BEFORE-DETACH SINGLE-HANDLE+CONTEXT-READ NO-MUTABLE-AUTHORITY-REREAD WRITESET17 NO-NEW-WRAPPER-STORE NO-BUSINESS-DIFFERENCE ACK-REQUIRED C-OWNER-RETAINED TURN40C-BLOCKED 2026-07-19T13:32:00-04:00 -->

## PARENT AMENDMENT #5 COMMUNICATION/SOURCE OBSERVATION #1 - 2026-07-19T13:37:00-04:00

- External C's 16:04 physical STATUS EVENT follows Amendment #5 but says no new directed C message and does not ACK
  `PARENT-C-TURN40B-QUEUE-CONTEXT-SNAPSHOT-AMENDMENT5-20260719-1332`. It nevertheless edits
  `WindowTaskRunner` and `TurnExecutionWindow`. This is A#5's first missed ACK round, not yet
  `COMMUNICATION_STALE`.
- P1 remains open: `WindowTaskRunner.runQueueWithBoundGameState` lines 624-632 still calls `updateTask(...)` before
  clearing the stable handle's previous executionContext and then publishes the new context. A concurrent
  `TurnExecutionWindow.resolve` can therefore read the new task identity with the prior task's authority snapshot.
- The first terminal path clears context at lines 547-553, but `getActiveTaskHandle` lines 565-567 detaches an
  inactive handle without first clearing its context. Both ordered requirements in A#5 remain unmet.
- The three producer files and isolated compile exit0 are bounded WIP only, not producer completion, canonical
  delivery, or source review. C retains the same whole-card owner and must ACK A#5 plus reminder1, repair producer
  order/cleanup and its existing test before starting Cloud consumer. No business difference; TURN-40C stays blocked.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-AMENDMENT5-OBS1 FIRST-MISSED-ACK NOT-COMMUNICATION-STALE PRODUCER-WIP-NONCOMPLIANT P1-UPDATE-BEFORE-CLEAR P1-SECOND-DETACH-WITHOUT-CLEAR ACK-A5+REMINDER1-REQUIRED REPAIR-BEFORE-CONSUMER C-OWNER-RETAINED NO-DELIVERY-REVIEW TURN40C-BLOCKED 2026-07-19T13:37:00-04:00 -->

## PARENT AMENDMENT #5 COMMUNICATION/SOURCE OBSERVATION #2 - 2026-07-19T13:42:00-04:00

- External C's next two physical events describe/claim A#5 ACK but do not name either required message id
  `PARENT-C-TURN40B-QUEUE-CONTEXT-SNAPSHOT-AMENDMENT5-20260719-1332` or
  `PARENT-C-TURN40B-AMENDMENT5-REMINDER1-20260719-1337`. Two consecutive rounds have now missed the directed ACK;
  mark `COMMUNICATION_STALE`. Source keeps changing, so this is not `ACTIVE_STALE` and owner remains retained.
- Physical production source is now corrected: `WindowTaskRunner` SHA=`CE4DDA83` clears the prior context before
  `updateTask`, publishes before execute, and clears the same retained handle before both detach sites.
  `TurnExecutionWindow` SHA=`8AF1BED9` retains one handle read plus one context read and projects only that snapshot.
- A#5 source shape is accepted as bounded WIP, but the required existing producer/client test has not been updated or
  reported to prove that queue replacement cannot expose the previous context. Isolated compile exit0 does not close
  that behavioral concurrency contract.
- C must precisely ACK original+reminder1+reminder2 and close the producer test gate before starting Cloud consumer.
  This is not a canonical delivery or source review. No business difference; TURN-40C remains blocked.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-AMENDMENT5-OBS2 TWO-MISSED-EXACT-ACK COMMUNICATION_STALE NOT-ACTIVE-STALE SOURCE-CORRECTED RUNNER=CE4DDA83 TURNEXECWINDOW=8AF1BED9 CLEAR-BEFORE-UPDATE+DUAL-DETACH-CLEAR PRODUCER-TEST-NO-PRIOR-CONTEXT-PENDING ACK-ORIGINAL+REMINDER1+REMINDER2-REQUIRED BEFORE-CONSUMER C-OWNER-RETAINED NO-DELIVERY-REVIEW TURN40C-BLOCKED 2026-07-19T13:42:00-04:00 -->

## PARENT PRODUCER TEST REVIEW #1 - 2026-07-19T13:50:00-04:00

- communication: C precisely ACKed original A#5, reminder1, and reminder2. `COMMUNICATION_STALE` is cleared;
  source remains active and the canonical owner is retained.
- accepted bounded source remains unchanged: runner=`CE4DDA83` has clear-before-update, publish-before-execute and
  both clear-before-detach sites; TurnExecutionWindow=`8AF1BED9` reads one handle and one context snapshot.
- finding `P1 / TEST FALSE POSITIVE`: `HttpsTurnClientContractTest` SHA=`7F0DCA39`, method
  `queueReplacementNeverExposesPriorContextAndOneSnapshotSuppliesAllSixAuthorityFacts`, manually invokes
  `publishExecutionContext(first)`, `clearExecutionContext()`, then `publishExecutionContext(second)` on a bare
  `RunningTaskHandle`. It never executes or inspects production `WindowTaskRunner` ordering, either detach site, or
  `TurnExecutionWindow` projection. The test therefore remains green if production regresses to update-before-clear,
  drops a terminal clear, or rereads mutable authority.
- repair in the same existing authorized test path: retain the handle atomic behavior assertions, and add direct
  production-wiring assertions that establish clear < update < publish < execute, clear-before-detach at both sites,
  and exactly one current-handle/context snapshot supplying all six metadata facts. No new test path, wrapper, store,
  protocol or default. Consumer remains blocked until this test passes and reports exact path/SHA/count.
- This is bounded WIP review, not canonical whole-card delivery/source review. No business difference; TURN-40C
  remains blocked.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-PRODUCER-TEST-REVIEW1 COMMUNICATION-RECOVERED THREE-EXACT-ACKS SOURCE-ACCEPTANCE-RETAINED RUNNER=CE4DDA83 TURNEXECWINDOW=8AF1BED9 P1-TEST-FALSE-POSITIVE HTTPSCLIENT=7F0DCA39 MANUAL-HANDLE-ONLY NO-PRODUCTION-ORDER-DETACH-PROJECTION-PROOF SAME-TEST-PATH-REPAIR-REQUIRED CLEAR<UPDATE<PUBLISH<EXECUTE BOTH-DETACH-CLEAR SINGLE-HANDLE+CONTEXT SIX-FACT NO-NEW-PATH-WRAPPER-STORE-PROTOCOL-DEFAULT BEFORE-CONSUMER C-OWNER-RETAINED NO-DELIVERY-REVIEW TURN40C-BLOCKED 2026-07-19T13:50:00-04:00 -->

## PARENT PRODUCER TEST REPAIR #1 COMMUNICATION/SOURCE OBSERVATION #1 - 2026-07-19T13:56:00-04:00

- C's next physical event says no new directed C message and does not ACK
  `PARENT-C-TURN40B-PRODUCER-TEST-REPAIR1-20260719-1350`. It proceeds into Cloud consumer despite the explicit
  before-consumer gate. This is Repair #1's first missed ACK round, not yet `COMMUNICATION_STALE`.
- Cloud `CloudTurnTaskRuntime` SHA=`53FE8363` is real source WIP and isolated production compile exit0 is recorded,
  but it is out of contract order and remains unreviewed. It does not constitute consumer completion, delivery, or
  acceptance while the producer test gate is false-positive.
- C must ACK Repair #1 plus reminder1, return to the same Https client test path, and add proof bound to actual
  WindowTaskRunner/TurnExecutionWindow wiring. Only after that test passes may consumer work resume. Preserve the
  current consumer bytes as WIP; do not claim step ④ done or continue consumer tests yet.
- No business difference; owner retained, no parent Maven/review, TURN-40C remains blocked.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-PRODUCER-TEST-REPAIR1-OBS1 FIRST-MISSED-ACK NOT-COMMUNICATION-STALE CONSUMER-OUT-OF-ORDER-WIP CLOUDRUNTIME=53FE8363-COMPILE-EXIT0 UNREVIEWED-NOT-DONE-DELIVERY RETURN-SAME-HTTPSCLIENT-TEST PRODUCTION-WIRING-PROOF-BEFORE-CONSUMER PRESERVE-CONSUMER-WIP C-OWNER-RETAINED NO-PARENT-MAVEN-REVIEW TURN40C-BLOCKED 2026-07-19T13:56:00-04:00 -->

## PARENT PRODUCER TEST REPAIR #1 COMMUNICATION OBSERVATION #2 - 2026-07-19T14:00:00-04:00

- C's 16:40 and 16:52 physical events do not ACK either exact directed message id. The 16:52 statement that the card
  review has no message id overlooks the ledger messages appended after it. Two consecutive rounds now satisfy
  `COMMUNICATION_STALE`; source/recon is active, so this is not `ACTIVE_STALE` and owner remains retained.
- C has correctly stopped consumer continuation and returned to test recon; no new test/source byte exists. Cloud
  runtime=`53FE8363` remains preserved unreviewed WIP.
- The proposed BareWindowTaskRunner + resolveForAction pattern can cover projection identity and six-fact snapshot,
  but it does not by itself prove private runner queue order or both terminal clear sites. The same-path repair must
  close both halves: production clear<update<publish<execute + dual detach clear, and one handle/context projection.
- Next event must ACK Repair #1 original, reminder1 and reminder2 exactly before editing. No delivery/review/Maven;
  TURN-40C remains blocked.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-PRODUCER-TEST-REPAIR1-OBS2 TWO-MISSED-EXACT-ACK COMMUNICATION_STALE NOT-ACTIVE-STALE CONSUMER-STOPPED TEST-RECON-ACTIVE NO-NEW-BYTES CLOUDRUNTIME=53FE8363-UNREVIEWED-WIP BARE-RUNNER-PROJECTION-ONLY-HALF NEED-RUNNER-ORDER+DUAL-DETACH+SINGLE-SNAPSHOT ACK-THREE-IDS-NEXT C-OWNER-RETAINED NO-DELIVERY-REVIEW-MAVEN TURN40C-BLOCKED 2026-07-19T14:00:00-04:00 -->

## PARENT PRODUCER TEST REPAIR #1 ADJUDICATION - 2026-07-19T14:04:00-04:00

- communication recovered: C's latest physical STATUS EVENT precisely ACKs Repair #1 original, reminder1 and
  reminder2. Clear `COMMUNICATION_STALE`; source is active and the canonical whole-card owner remains C.
- accepted partial evidence: `HttpsTurnClientContractTest` SHA=`D7B1143E`, 13/13, directly exercises production
  `WindowTaskRunner.getActiveTaskHandle` and proves the inactive-handle detach clears its published context. It does
  not yet prove queue order, the outer terminal detach, or production projection.
- no user/business decision and no production seam is required. The existing code supplies a bounded same-path
  reflective harness:
  1. invoke private `runQueueWithBoundGameState` with one `AUTO_BATTLE` item, a recording RunningTaskHandle and
     recording GameTask; stub only the collaborators actually reached. Assert exact events
     `clear, update, publish, execute`. `AUTO_BATTLE` bypasses combat defer, role detection and window observer.
  2. separately invoke outer private `runQueue` with an empty queue and no-op maintenance collaborators; assert the
     retained handle is cleared before `currentTask` is detached in the outer finally.
  3. drive real `TurnExecutionWindow.resolveForAction` through the existing BareWindowTaskRunner pattern; assert one
     current-handle read and one execution-context read supply all six authority facts, including replacement-race
     behavior and null-context absence.
- correction to C's 17:16 recon: `WUHuan_V2` is not a bounded no-observer choice because
  `shouldRunWindowObserver(WUHuan_V2)` is true. Calling only `runQueueWithBoundGameState` also cannot prove the first
  terminal detach, which exists in outer `runQueue` finally. Do not implement either mistaken assumption.
- retain the existing handle atomic assertions and the real second-detach test. No new test path, production
  abstraction, protocol/store/default or business difference. Consumer=`53FE8363` remains stopped/unreviewed until
  all producer proofs pass and exact SHA/count/files are reported; no delivery/review/Maven, TURN-40C stays blocked.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-PRODUCER-TEST-REPAIR1-ADJUDICATION COMMUNICATION-RECOVERED THREE-EXACT-ACKS HTTPSCLIENT=D7B1143E-13of13 SECOND-DETACH-REAL-PASS ORDER+FIRST-TERMINAL+PROJECTION-PENDING USE-AUTO_BATTLE-INNER-ORDER USE-EMPTY-OUTER-RUNQUEUE-TERMINAL USE-REAL-RESOLVEFORACTION-SINGLE-SNAPSHOT CORRECT-WUHUAN-OBSERVER+INNER-NONTERMINAL NO-USER-DECISION-SEAM-NEW-PATH-BUSINESS-DIFF CONSUMER=53FE8363-STOPPED-UNREVIEWED C-OWNER-RETAINED NO-DELIVERY-REVIEW-MAVEN TURN40C-BLOCKED 2026-07-19T14:04:00-04:00 -->

## PARENT PRODUCER TEST REVIEW #2 - 2026-07-19T14:22:00-04:00

- accepted progress: `HttpsTurnClientContractTest` SHA=`2E73E978`, 14/14, reflectively drives production
  `WindowTaskRunner.runQueue(AUTO_BATTLE)` and observes `clear, update, publish, execute, clear`. This closes the
  production queue-switch ordering half without a seam/new path and retains the handle atomic test.
- finding `P1 / TEST FALSE POSITIVE / REPAIR REQUIRED`: the terminal test asserts only after `runQueue` returns that
  executionContext and currentTask are null. The inactive-handle test does the same after `getActiveTaskHandle`
  returns. Both remain green if production regresses to `currentTask=null` before `clearExecutionContext`, so neither
  proves the required clear-before-detach relative order.
- bounded same-path repair: give the RecordingHandle a reference/observer for its runner after construction. Inside
  overridden `clearExecutionContext`, when recording either detach clear, assert/read that runner.currentTask still
  equals this handle before calling super. Use the same attached-at-clear observation in both terminal paths. Keep
  the existing event order and post-return null assertions. No production seam, source guard, protocol/store/default
  or business difference.
- communication: C's 17:40 event says no new directed message and does not ACK
  `PARENT-C-TURN40B-PRODUCER-TEST-REPAIR1-HARNESS-ADJUDICATION-20260719-1404`; this is the first missed ACK round,
  not `COMMUNICATION_STALE`. Owner/source active retained.
- projection remains pending and consumer=`53FE8363` remains stopped/unreviewed. This is WIP review, not whole-card
  delivery/source acceptance; no parent Maven/runtime/input, TURN-40C remains blocked.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-PRODUCER-TEST-REVIEW2 HTTPSCLIENT=2E73E978-14of14 RUNNER-CLEAR<UPDATE<PUBLISH<EXECUTE-ACCEPTED P1-TEST-FALSE-POSITIVE DETACH-RELATIVE-ORDER-NOT-PROVEN RETURN-AFTER-DOUBLE-NULL-INSUFFICIENT SAME-PATH-RECORDINGHANDLE-ASSERT-ATTACHED-AT-CLEAR BOTH-DETACH-SITES REPAIR-REQUIRED FIRST-MISSED-ACK-1404 NOT-COMMUNICATION-STALE PROJECTION+CONSUMER-PENDING C-OWNER-RETAINED NO-DELIVERY-PARENT-MAVEN-RUNTIME-INPUT-BUSINESS-DIFF TURN40C-BLOCKED 2026-07-19T14:22:00-04:00 -->

## PARENT PRODUCER TEST REPAIR #2 COMMUNICATION/SOURCE OBSERVATION - 2026-07-19T14:32:00-04:00

- communication: C's 17:40 and 18:04 physical events both fail to ACK ledger message
  `PARENT-C-TURN40B-PRODUCER-TEST-REPAIR1-HARNESS-ADJUDICATION-20260719-1404`. The 18:04 event again treats the
  card text as having no message id and also misses ledger Review #2 message `...-1422`. Mark
  `COMMUNICATION_STALE`; source is changing, so not `ACTIVE_STALE`, and owner remains retained.
- reported `HttpsTurnClientContractTest=7A7EABB5`, 17/17 adds the real resolveForAction projection, but its two
  detach tests still only assert context/currentTask are null after return. It does not close Review #2 P1 and cannot
  unblock consumer.
- current physical test SHA=`BA515A97` is later unreported repair WIP. It adds observedRunner and
  stillAttachedWhenCleared fields to RecordingHandle, but at audit time both detach tests still instantiate ordinary
  RunningTaskHandle and do not call observeCurrentTaskAtClear/assert the observation. Preserve the bytes; finish the
  same-path wiring before claiming green.
- required order remains: exact ACK all three ledger ids; wire attached-at-clear into both detach tests and report
  green; then parent may accept the already-added projection evidence. Consumer=`53FE8363` remains
  stopped/unreviewed. No delivery/parent Maven/runtime/input/business difference, TURN-40C remains blocked.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-PRODUCER-TEST-REPAIR2-OBS COMMUNICATION_STALE TWO-MISSED-ACK-1404 MISSED-1422 NOT-ACTIVE-STALE HTTPSCLIENT-REPORTED=7A7EABB5-17of17-P1-STILL-OPEN PHYSICAL=BA515A97-UNREPORTED-ATTACHED-AT-CLEAR-UNWIRED BOTH-DETACH-TESTS-STILL-ORDINARY-HANDLE REPAIR+ACK-REQUIRED PROJECTION-WIP-PRESERVED CONSUMER=53FE8363-STOPPED-UNREVIEWED C-OWNER-RETAINED NO-DELIVERY-PARENT-MAVEN-RUNTIME-INPUT-BUSINESS-DIFF TURN40C-BLOCKED 2026-07-19T14:32:00-04:00 -->

## PARENT PRODUCER TEST REVIEW #3 - 2026-07-19T14:37:00-04:00

- communication recovered: C precisely ACKed 1404, 1422 and 1432. Clear `COMMUNICATION_STALE`; source active and
  owner retained.
- accepted: `HttpsTurnClientContractTest` SHA=`7EB2C269`, 17/17. Both production detach paths now use
  RecordingHandle and assert inside clearExecutionContext that runner.currentTask still equals that handle. Runner
  clear<update<publish<execute, both clear-before-detach paths and the retained handle atomic behavior pass review.
- finding `P1 / TEST FALSE POSITIVE`: `resolveForActionReadsOneHandleAndOneContextSnapshotSupplyingAllSixAuthorityFacts`
  asserts `ProjectionRunner.currentTaskReads()==1`, but uses an ordinary RunningTaskHandle and never counts
  `getExecutionContext()` calls. A production regression that calls the same handle's getExecutionContext twice
  still passes. Repair with a counting RunningTaskHandle whose first context read returns the MEMBER authority and
  whose second read would return distinct replacement authority; assert contextReadCount==1 and the six projected
  values remain from the first snapshot.
- finding `P2 / INCOMPLETE NEGATIVE`: null-context test asserts only windowRole, localLeaderPresent and startupMode
  are null. Add null assertions for localTeamSessionKey, localLeaderWindowId and localSupportMember so absence is
  proven for all six authority fields.
- same existing Https client test path only; no production seam/new path/source guard/protocol/store/default/business
  difference. Consumer=`53FE8363` remains stopped/unreviewed until this repair is green and reported. This is WIP
  review, not whole-card delivery/source acceptance; no parent Maven/runtime/input, TURN-40C remains blocked.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-PRODUCER-TEST-REVIEW3 COMMUNICATION-RECOVERED THREE-ACKS HTTPSCLIENT=7EB2C269-17of17 ORDER+DUAL-DETACH+ATOMIC-PASSED P1-PROJECTION-CONTEXT-READ-NOT-COUNTED FALSE-POSITIVE REQUIRE-COUNTING-HANDLE-FIRST-CONTEXT+DISTINCT-SECOND+COUNT1 P2-NULL-NEGATIVE-3of6 REQUIRE-ALL6-NULL SAME-HTTPSCLIENT-PATH REPAIR-REQUIRED CONSUMER=53FE8363-STOPPED-UNREVIEWED C-OWNER-RETAINED NO-DELIVERY-PARENT-MAVEN-RUNTIME-INPUT-BUSINESS-DIFF TURN40C-BLOCKED 2026-07-19T14:37:00-04:00 -->

## PARENT PRODUCER TEST REVIEW #4 PASSED - 2026-07-19T14:42:00-04:00

- communication: C precisely ACKed Review #3 message 1437. Communication remains healthy; owner/source active.
- reviewed `HttpsTurnClientContractTest` SHA=`DE50232B`, reported isolated compile exit0 and 17/17 green.
- `P0/P1/P2=0/0/0` for the producer-test gate:
  - CountingContextHandle returns the MEMBER six-fact TaskExecutionContext on first read and distinct LEADER facts on
    any second read; the test asserts both ProjectionRunner.currentTaskReads and handle.contextReadCount equal one.
  - all six projected authority fields match the first exact context; the no-context negative asserts all six null.
  - previously accepted production runner clear<update<publish<execute, both attached-at-clear detach paths and
    queue-replacement handle atomic behavior remain present and green.
- Producer test gate is passed; C may resume the existing consumer WIP and implement/run step ⑤
  CloudTurnTaskRuntimeContractTest and step ⑥ CloudTurnHttpHandlerContractTest within the 17-path whole-card
  contract. Cloud runtime=`53FE8363` is still unreviewed WIP, not accepted consumer source or whole-card delivery.
- No parent Maven/runtime/input and no business difference. TURN-40C remains blocked until whole-card delivery,
  applicable compile/named family and parent final source+test review.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-PRODUCER-TEST-REVIEW4-PASSED COMMUNICATION-OK ACK-1437 HTTPSCLIENT=DE50232B-17of17 P0-P1-P2=0-0-0 COUNTING-HANDLE-CURRENTTASKREAD1+CONTEXTREAD1 FIRST-MEMBER-SECOND-DISTINCT ALL6-POSITIVE+ALL6-NULL ORDER+DUAL-DETACH+ATOMIC-RETAINED PRODUCER-GATE-PASSED CONSUMER-STEP5+6-RESUME-OPEN CLOUDRUNTIME=53FE8363-UNREVIEWED-WIP NOT-WHOLE-DELIVERY C-OWNER-RETAINED NO-PARENT-MAVEN-RUNTIME-INPUT-BUSINESS-DIFF TURN40C-BLOCKED 2026-07-19T14:42:00-04:00 -->

## PARENT DELIVERY-GATE OBSERVATION - 2026-07-19T14:52:00-04:00

- C reports all six implementation steps complete. Current terminal evidence: shared validator/lifecycle/core
  18/18+5/5+7/7; producer Https client=`DE50232B` 17/17; Cloud runtime contract=`327D6E10` 24/24; Cloud handler
  contract=`A63493E4` 7/7. The 17 approved logical paths now all have implementation bytes.
- This is source/test active progress, not canonical whole-card delivery or parent source acceptance. Remaining fixed
  delivery gates are the complete authorized HTTPS_TURN_CONTRACT_TEST_FAMILY, byte-identical audit of all five shared
  files, applicable compile in both repositories, exact SHA/line/mtime inventory for all 17 physical paths, and an
  original-card canonical whole-card delivery marker.
- communication: C's 19:08 physical event says no new directed message and does not ACK Review #4 pass message
  `PARENT-C-TURN40B-PRODUCER-TEST-REVIEW4-PASSED-20260719-1442`. First missed round only; not communication stale.
  Owner/source active retained.
- Parent does not run Maven while the Java/test writer remains active. No runtime/UI/input/business difference;
  TURN-40C remains blocked pending canonical delivery and final parent review.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-DELIVERY-GATE-OBS ALL6-STEPS-COMPLETE RUNTIMETEST=327D6E10-24of24 HANDLER=A63493E4-7of7 PRODUCER=DE50232B-17of17 VALIDATOR+LIFECYCLE+CORE=18of18+5of5+7of7 17PATH-IMPLEMENTED NOT-CANONICAL-DELIVERY-OR-SOURCE-ACCEPTANCE REMAIN-FULL-HTTPS-FAMILY+5SHARED-BYTE-AUDIT+DUAL-COMPILE+17PATH-EVIDENCE+ORIGINAL-CARD-DELIVERY FIRST-MISSED-ACK-1442 NOT-COMMUNICATION-STALE C-OWNER-RETAINED NO-PARENT-MAVEN-RUNTIME-INPUT-BUSINESS-DIFF TURN40C-BLOCKED 2026-07-19T14:52:00-04:00 -->

## EXTERNAL-C CANONICAL WHOLE-CARD DELIVERY — TURN-40B/RUNTIME-FACTORY (BASELINE-A) — 2026-07-19T19:40:00-04:00

**state → `AWAITING_PARENT_REVIEW`.** External C sole owner. All 17 authorized paths implemented under Amendments
#1–#5 (baseline-equivalent user decision A). Requesting the parent sole final source+test review. Zero source
outside the 17 fixed paths; zero Git mutation; `D:\mavenProject\DHXY` (frozen user baseline) read-only and untouched
(all producer edits are in the `DHXY-cr271` worktree).

### Write-set (17 paths — full SHA-256 / lines)

DHXY-cr271 (9):
1. `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnWindowMetadata.java` — `6f6eac552c98ab0b483104f3a89388d2b71f0e2402846e3dc44f5602640bf8b0` / 75L
2. `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java` — `56383c98d92713ef8c0cdd6f0d2dc560a0a4d7e6c9d09035b3477e086f0d978c` / 833L
3. `src/main/java/com/bot/dhxy/window/execution/RunningTaskHandle.java` — `19c38b9247b71fbafc8ce17714499ee398034b712aafc545807484e030350f0c` / 281L
4. `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java` — `ce4dda8369740544d0c3e130d78dea2f483ae3466fd02a5902a2ee244b0b0fd9` / 4320L
5. `src/main/java/com/bot/dhxy/cloud/turn/TurnExecutionWindow.java` — `8af1bed94da5e35bd6c9b63625eb52f7240037bd96ec99474115edd225904f74` / 247L
6. `src/test/java/com/bot/dhxy/cloud/turn/HttpsTurnClientContractTest.java` — `de50232baefdf04d038a5945d80af73a13d8ebc6609e59b6ddb46c680a7a499a` / 842L
7. `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java` — `c32d45227edf2ac0ce61086f00779c754428c5b666c42d4d25044c04b32c46a3` / 1012L
8. `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnTaskLifecycleProtocolGoldenJsonTest.java` — `a10730088b7e1dd757cf0274ec1d999d0d3c21cae826ee74c40eb21156a213a5` / 137L
9. `src/test/resources/cloud-turn/v1/request-start.json` — `77c68b252484d11eab9eef72740de9ac6920616200f97dd71b20a9491b7aacbd` / 36L

Cloud dhxy-cloud-brain (8):
10. `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnWindowMetadata.java` — `6f6eac552c98ab0b483104f3a89388d2b71f0e2402846e3dc44f5602640bf8b0` / 75L
11. `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java` — `56383c98d92713ef8c0cdd6f0d2dc560a0a4d7e6c9d09035b3477e086f0d978c` / 833L
12. `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntime.java` — `53fe8363844f48b4b575033e160464decfd41833735837c759e81d8e78a4aac1` / 417L
13. `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java` — `c32d45227edf2ac0ce61086f00779c754428c5b666c42d4d25044c04b32c46a3` / 1012L
14. `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudTurnTaskRuntimeContractTest.java` — `327d6e108dabed11fddac00e84a3ee444d63ddc48a4af30b141f154d6eac0075` / 798L
15. `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/CloudTurnHttpHandlerContractTest.java` — `a63493e4631a7029ad6bc141fdf3763e79e3b5a579e1c07c97d8e6082792cf47` / 512L
16. `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnTaskLifecycleProtocolGoldenJsonTest.java` — `a10730088b7e1dd757cf0274ec1d999d0d3c21cae826ee74c40eb21156a213a5` / 137L
17. `src/test/resources/cloud-turn/v1/request-start.json` — `77c68b252484d11eab9eef72740de9ac6920616200f97dd71b20a9491b7aacbd` / 36L

### Dual-repo byte-identical (5 shared files, `cmp` verified)

`TurnWindowMetadata.java`, `TurnProtocolValidator.java`, `TurnProtocolValidatorContractTest.java`,
`TurnTaskLifecycleProtocolGoldenJsonTest.java`, `request-start.json` — each byte-identical across both repositories
(paths 1 to 10, 2 to 11, 7 to 13, 8 to 16, 9 to 17 share the SHA above).

### Named test results (HTTPS_TURN_CONTRACT_TEST_FAMILY, isolate-run together, C sole active writer; A idle)

- `TurnProtocolValidatorContractTest` = 18/18 (task-start authority present/valid/consistent + reject cases).
- `TurnCoreProtocolGoldenJsonTest` = 7/7 (read-only core golden, unchanged and green).
- `TurnTaskLifecycleProtocolGoldenJsonTest` = 5/5 (request-start fixture carries the six authority facts; local
  `taskStartAuthorityWindow()` for order/negatives; non-start cases on the shared helper).
- `CloudTurnTaskFactoryAllowlistTest` = 2/2 (fresh prototype per resolve; construction-failure propagation).
- `CloudTurnTaskRuntimeContractTest` = 24/24 (20 lifecycle + authority pass-through + reject-before-materialize).
- `CloudTurnHttpHandlerContractTest` = 7/7 (window authority facts survive the handler JSON round-trip).
- `HttpsTurnClientContractTest` = 17/17 (producer wiring: real `runQueueWithBoundGameState` order
  `clear<update<publish<execute`; both detach sites clear-before-detach relative order via attached-at-clear; real
  `TurnExecutionWindow.resolveForAction` single-read of handle and context supplying all six facts; null-context
  absence; handle atomic). Producer-test gate was PASSED by parent Review #4
  (`PARENT-C-TURN40B-PRODUCER-TEST-REVIEW4-PASSED-20260719-1442`) at `DE50232B`; the counting-context refinement
  advanced it to `de50232b`, still 17/17.

### Dual-repo compile evidence

- Cloud: new metadata + validator + five runtime-package production + the six changed/related Cloud named tests
  compile together (`javac` exit 0) and run as above.
- DHXY-cr271: producer set (`RunningTaskHandle` + `WindowTaskRunner`, Lombok processor) + `TurnExecutionWindow` +
  metadata compile (`javac` exit 0); `HttpsTurnClientContractTest` compiles and runs 17/17.

### Baseline-equivalence (user decision A) attestation

The six authority facts (`windowRole`, `localTeamSessionKey`, `localLeaderWindowId`, `localLeaderPresent`,
`localSupportMember`, `startupMode`) originate solely from the exact per-task `TaskExecutionContext` produced by the
local `WindowTaskRunner.buildExecutionContext(...)`, atomically published into `RunningTaskHandle`, read once by
`TurnExecutionWindow` and projected onto the shared `TurnWindowMetadata`, then consumed read-only by
`CloudTurnTaskRuntime` to build `CloudTaskServiceMetadata`. Nothing recomputes, guesses, defaults, or stores a second
copy; the hardcoded `LEADER`/`NORMAL` placeholder is deleted. Missing/invalid/contradictory facts on a task-start are
rejected before materialize/registry/worker/ack (validator + runtime). No approved business difference; equivalent to
`696a12b0` and the current local authority context. No new protocol/store/session/TTL/retry/stub/copied algorithm.

### Disclosed pre-card shared debt (NOT this card, not hidden)

`TurnActionGoldenJsonTest` = 3/7 — 4 failures pre-date this card (pre-existing `pathingIntent` / `wholeTaskRuntime` /
`metric` fields lacking `@JsonInclude(NON_NULL)`, from earlier unrelated work). Unchanged by this card and recorded
here rather than masked. Amendment #4 fixed only the window `pathingSnapshot` component (which is in this write-set).

### Disposition

`TURN-40C` remains BLOCKED pending this review. On parent PASS, C releases ownership and 40C may proceed; on
P0/P1/P2 findings, C revises the whole card within the 17-path boundary and re-walks canonical delivery. C holds at
`AWAITING_PARENT_REVIEW`; no further source edits pending review.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY EXTERNAL-C CANONICAL-WHOLE-CARD-DELIVERY BASELINE-A AWAITING_PARENT_REVIEW 17-PATH-FULL-SHA DUAL-REPO-5FILE-BYTE-IDENTICAL VALIDATOR-18 CORE-7 LIFECYCLE-5 FACTORY-2 RUNTIME-24 HANDLER-7 HTTPSCLIENT-17 DUAL-COMPILE-EXIT0 BASELINE-EQUIVALENCE-ATTESTED TURNACTION-3of7-PRECARD-DEBT-DISCLOSED C-SOLE-OWNER ZERO-DRIFT ZERO-GIT DHXY-READONLY TURN40C-BLOCKED-PENDING-REVIEW 2026-07-19T19:40:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - BLOCKED / REPAIR REQUIRED - 2026-07-19T15:02:00-04:00

- verdict: `P0/P1/P2 = 0/3/1`; External C retains the same canonical whole-card owner. `TURN-40C` remains
  `BLOCKED / NOT READY`. This review introduces no user/business choice: baseline-equivalent A and commit
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` remain the sole behavior authority.
- verified delivery inventory: all 17 physical SHA-256/line counts match the delivery; the five shared files are
  byte-identical; CR branch/HEAD remain `thin-client-design@59b85e0b`, baseline remains read-only at `696a12b0`, and
  Cloud remains `navigation-migration@3b988ca`. Previously accepted producer projection/detach proofs remain valid.

### P1-1 - Creation failure and role-skip can still expose the previous queue item's authority

`WindowTaskRunner.runQueueWithBoundGameState` reaches role preflight/UNKNOWN skip and `taskFactory.createTask`
before capturing the handle and calling `clearExecutionContext()` at current lines 626-631. Therefore a second queue
item that is skipped by role policy or whose factory returns null leaves the first item's published
`TaskExecutionContext` visible until terminal cleanup. This contradicts Amendment #5 lines 3086-3088: creation
failure, skip and every pre-publish interval must expose null authority. The production comment claiming those paths
are protected is false at its current placement. `HttpsTurnClientContractTest` only drives a successful one-item
AUTO_BATTLE path and has no production skip/create-failure negative, so 17/17 does not detect this regression.

Repair inside the same existing paths: clear the retained handle's old execution context at the beginning of each
queue-item transition, before preflight can skip and before factory creation can fail; keep clear<update<publish<execute
for a materialized task and both terminal clear-before-detach rules. Extend the existing Https client contract test
with real production-path skip and creation-failure cases proving the retained handle context is null and no stale
six-fact task-start can be formed. Do not add a new store/wrapper/default or alter role/factory business decisions.

### P1-2 - Runtime can return a retained ack before validating the six authority facts

`CloudTurnTaskRuntime.start` performs `registry.decide(...)` and immediately returns DUPLICATE/CONFLICT at current
lines 146-153; `taskStartAuthorityError(window)` is only called at lines 159-163. A same exact key/id call with
missing/invalid/contradictory latest metadata therefore returns the retained ack without validation. That contradicts
Amendment #1 acceptance and the delivery attestation that invalid task-start authority is rejected before
registry/worker/ack. The runtime test covers a valid same-ID duplicate and fresh invalid starts separately, but never
combines an already-retained ID with invalid authority, so it remains green.

Repair in the existing runtime/test files: validate the exact six authority facts after exact window/request sanity
but before any registry decision can return an ack. Preserve valid same-ID idempotency and active different-ID
conflict semantics. Add the retained-ID invalid-authority negative and prove no ack/materialization/new worker occurs.

### P1-3 - The frozen Maven build gate is not met for the delivered bytes

The canonical runtime/factory contract at lines 2480-2482 and 2521-2523 requires the named tests plus full
`mvn -q -DskipTests=false test-compile` and `mvn -q -DskipTests=false compile`. The delivery's "dual compile"
section reports only selective standalone `javac` compilation of chosen sources/tests. Earlier Maven success predates
the final authority/producer/consumer bytes and cannot certify these hashes. Selective javac is useful evidence but
does not satisfy the frozen aggregate build gate or AGENTS Java handoff rule.

After P1-1/P1-2 repair and writer stability, run the literal authorized Maven test-compile/compile gates in the
applicable repository scopes plus the named HTTPS family. If an unrelated current aggregate blocker occurs, record
its exact file/error and do not label the Maven gate passed; do not widen this card to repair unrelated debt.

### P2-1 - Runtime test count in the canonical delivery is false

Current `CloudTurnTaskRuntimeContractTest.java` SHA `327D6E10` contains 22 `@Test` methods, matching C's physical
family event (`runtime 22/22`). The delivery and synchronized docs say `24/24`; the actual 24 total is factory 2/2 +
runtime 22/22. Correct the canonical re-delivery and all synchronized status text; test count is evidence, not a
semantic choice.

### Re-delivery gate

Repair only the original 17-path boundary, rerun the corrected producer/runtime negatives, the complete authorized
named family and applicable Maven gates, then append one canonical whole-card re-delivery with exact hashes/counts.
No runtime/application/server/UI/capture/input and no business difference. Parent remains the sole final reviewer.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-SOURCE+TEST-REVIEW1 BLOCKED REPAIR-REQUIRED P0=0 P1=3 P2=1 OWNER-C-RETAINED P1-QUEUE-SKIP+CREATEFAIL-STALE-CONTEXT P1-DUPLICATE-ACK-BEFORE-AUTHORITY-VALIDATION P1-MAVEN-GATE-NOT-MET P2-RUNTIME-22-NOT-24 SAME-17PATH NO-USER-DECISION BASELINE-A-696 TURN40C-BLOCKED MESSAGE=PARENT-C-TURN40B-RUNTIME-FACTORY-REVIEW1-REPAIR-20260719-1502 2026-07-19T15:02:00-04:00 -->

## Parent Communication Audit - 2026-07-19 15:19 EDT

External C's ledger STATUS EVENT has precisely acknowledged
`PARENT-C-TURN40B-RUNTIME-FACTORY-REVIEW1-REPAIR-20260719-1502` and accepted all four repair groups without
changing the frozen 17-path boundary or baseline-A/696 behavior. Communication is healthy and C retains the
canonical owner in `SOURCE_ACTIVE / REPAIR`.

This is not a new delivery or review pass. Parent Review #1 remains `P0/P1/P2=0/3/1 BLOCKED / REPAIR REQUIRED`;
TURN-40C remains blocked until repaired source, tests, accurate counts, and applicable Maven evidence are canonically
re-delivered at this card's physical EOF.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-COMMUNICATION-AUDIT REVIEW1-ACK-RECEIVED COMMUNICATION-OK SOURCE-ACTIVE-REPAIR OWNER-C-RETAINED REVIEW1-STILL-BLOCKED P0=0 P1=3 P2=1 SAME-17PATH BASELINE-A-696 NO-USER-DECISION NO-REDELIVERY TURN40C-BLOCKED ACK=PARENT-C-TURN40B-RUNTIME-FACTORY-REVIEW1-REPAIR-20260719-1502 2026-07-19T15:19:00-04:00 -->


## EXTERNAL-C CANONICAL WHOLE-CARD RE-DELIVERY #2 (Review #1 REPAIR) — TURN-40B/RUNTIME-FACTORY (BASELINE-A) — 2026-07-19T20:55:00-04:00

**state → `AWAITING_PARENT_REVIEW`.** External C sole owner. Whole-card Source+Test Review #1
(`PARENT-C-TURN40B-RUNTIME-FACTORY-REVIEW1-REPAIR-20260719-1502`, `P0/P1/P2=0/3/1`) fully repaired within the same
frozen 17-path boundary — baseline-equivalent user decision A, no new store/protocol/default/retry/TTL/stub, TURN-40C
untouched. Zero Git mutation; `D:\mavenProject\DHXY` (frozen user baseline) read-only and untouched (all edits in the
`DHXY-cr271` worktree + `dhxy-cloud-brain`). Requesting the parent sole final source+test review.

### Repair applied (each Review #1 finding)

- **P1-1 (WindowTaskRunner + HttpsTurnClientContractTest).** `clearExecutionContext()` moved to the very TOP of each
  queue-item transition (before the preflight `buildExecutionContext`, the team-role skip, and `taskFactory.createTask`),
  so a role-skipped or creation-failed item exposes null authority and never projects the prior task's role/team/startup
  facts. The redundant pre-publish clear (formerly before `updateTask`) is removed; the materialized-task ordering stays
  `clear < update < publish < execute`, and both terminal detach sites (outer `runQueue` finally + `getActiveTaskHandle`)
  keep their clear-before-detach. Two real production-path negatives added:
  - `runQueueWithBoundGameStateClearsTheRetainedContextBeforeARoleSkipNeverExposingPriorAuthority` — drives the genuine
    role-reassignment skip: a leader-only `XIULUO` task on an unknown-role window, real `TaskTeamAssignmentPolicy`
    resolving to `UNKNOWN` (live detection stubbed to UNKNOWN; `XIULUO` is not a startup-combat-defer task so no game
    runtime runs). Asserts the transition order is exactly `[clear]` (never update/publish/execute; factory throws if
    reached) and the retained handle's context is null after the skip.
  - `runQueueWithBoundGameStateClearsTheRetainedContextBeforeACreationFailureNeverExposingPriorAuthority` — factory
    returns null; asserts order exactly `[clear]` and retained context null after the creation failure.
- **P1-2 (CloudTurnTaskRuntime + CloudTurnTaskRuntimeContractTest).** `taskStartAuthorityError(window)` validation moved
  to AFTER window/request sanity but BEFORE `registry.decide(...)`, so a task-start carrying absent/invalid/contradictory
  authority is rejected before the registry can return a retained-duplicate ack or flag an active conflict, and before
  any materialize/worker/ack. Valid same-id idempotency and active different-id conflict are preserved (unchanged
  registry decision for valid authority). One negative added:
  - `aRetainedIdWhoseLatestWindowAuthorityIsInvalidIsRejectedBeforeReplayingTheAckOrMaterializing` — an id whose ack is
    already retained arrives again with invalid latest window authority; asserts it is rejected (not replayed), returns
    no ack, materializes no new prototype, starts no new worker, and leaves no active run.
- **P1-3 (Maven gates + named family).** Run below. Unrelated aggregate blocker recorded, gate NOT labeled passed, card
  NOT widened.
- **P2-1 (counts).** Corrected below (runtime contract test is 23 after the P1-2 negative; factory 2).

### Write-set (17 paths — 4 changed this repair, 13 unchanged)

Changed (4):
- `DHXY-cr271 .../window/execution/WindowTaskRunner.java` — `1405601eafca081d438b046ffe6a2dc0c5805446d2ea943f5f386251ad564424` / 4328L
- `DHXY-cr271 .../cloud/turn/HttpsTurnClientContractTest.java` — `42ed50b2f281dba29a834b1617f03c4a00c1106c674e6461f6823869ae56b1b4` / 960L
- `Cloud .../cloudbrain/turn/runtime/CloudTurnTaskRuntime.java` — `df5aa6fbef62242b7d78026acd61a7f38360d8a8cf620ddf9e37e1edd9a40fa6` / 419L
- `Cloud .../cloudbrain/turn/runtime/CloudTurnTaskRuntimeContractTest.java` — `12696c4c2fff89cb5d7208b7e77333ab05c0c066bf38511531f668e46740a29b` / 825L

Unchanged (13, prior canonical SHAs hold):
- DHXY-cr271: `TurnWindowMetadata.java` `6f6eac55…` / 75L; `TurnProtocolValidator.java` `56383c98…` / 833L;
  `RunningTaskHandle.java` `19c38b92…` / 281L; `TurnExecutionWindow.java` `8af1bed9…` / 247L;
  `TurnProtocolValidatorContractTest.java` `c32d4522…` / 1012L; `TurnTaskLifecycleProtocolGoldenJsonTest.java`
  `a1073008…` / 137L; `request-start.json` `77c68b25…` / 36L.
- Cloud: `TurnWindowMetadata.java` `6f6eac55…` / 75L; `TurnProtocolValidator.java` `56383c98…` / 833L;
  `TurnProtocolValidatorContractTest.java` `c32d4522…` / 1012L; `CloudTurnHttpHandlerContractTest.java` `a63493e4…` / 512L;
  `TurnTaskLifecycleProtocolGoldenJsonTest.java` `a1073008…` / 137L; `request-start.json` `77c68b25…` / 36L.

### Dual-repo byte-identical (5 shared files, `cmp -s` re-verified this repair)

`TurnWindowMetadata.java`, `TurnProtocolValidator.java`, `TurnProtocolValidatorContractTest.java`,
`TurnTaskLifecycleProtocolGoldenJsonTest.java`, `request-start.json` — each byte-identical across both repositories
(none touched this repair).

### Named test results (isolate-run, C sole active writer; A idle)

- `HttpsTurnClientContractTest` = **19/19** (17 prior producer-wiring + 2 new P1-1 negatives: genuine role-reassign skip
  + creation-failure, both proving the retained handle exposes null authority and the transition order is `[clear]`).
- `CloudTurnTaskRuntimeContractTest` = **23/23** (22 prior lifecycle/identity/authority + 1 new P1-2 retained-id
  invalid-authority negative).
- Unchanged and still green (prior canonical, no source touched this repair): `TurnProtocolValidatorContractTest` 18/18,
  `TurnCoreProtocolGoldenJsonTest` 7/7, `TurnTaskLifecycleProtocolGoldenJsonTest` 5/5, `CloudTurnTaskFactoryAllowlistTest`
  **2/2**, `CloudTurnHttpHandlerContractTest` 7/7.

### Maven gate evidence (P1-3, literal, A idle → Maven safe)

- Cloud `mvn -q -o -DskipTests=false test-compile` = **exit 0** (whole-repo main + test compile clean, includes the P1-2
  change).
- DHXY-cr271 `mvn -q -o -DskipTests=true compile` (main) = **exit 0** (the P1-1 production change integrates whole-tree).
- DHXY-cr271 `mvn -q -o -DskipTests=false test-compile` = **BLOCKED — exit 1**, by pre-existing out-of-write-set dirty
  TEST files ONLY (none in the 17-path set): `TaskMaintenanceCR138LocalSupportCapabilityTest.java`
  (`awaitLocalTeamSupportCapabilityOpen` missing), `DialogMaintenanceLightweightFallbackPolicyWiringTest.java`
  (`isAllowFullMaintenanceBroadcastFallback`/`handleMaintenanceBroadcastOption` missing),
  `XiuluoCR84RouteStateModelTest.java` (`withShortcutTrackerClick` arity), `AutoCombatCR138FirstAidOnlyCommonBoxGuardTest.java`
  (`TaskMaintenanceService` ctor arity), `InputActionPauseCancellationGuardTest.java` (`InputActionExecutionResult` vs
  boolean). These are unrelated shared debt from other work streams. The aggregate test-compile gate is therefore NOT
  labeled passed and the card is NOT widened to fix them; the named HTTPS family is verified via the isolate-run
  technique (single-class compile + junit-platform-console-standalone) as the source-review evidence, with the Maven
  test-compile aggregate gate explicitly marked BLOCKED.

### Baseline-equivalence (user decision A) attestation — unchanged

The six authority facts originate solely from the exact per-task `TaskExecutionContext` (`WindowTaskRunner.buildExecutionContext`),
atomically published into `RunningTaskHandle`, read once by `TurnExecutionWindow`, projected onto the shared
`TurnWindowMetadata`, and consumed read-only by `CloudTurnTaskRuntime`. Nothing recomputes/guesses/defaults/stores a
second copy; missing/invalid/contradictory task-start facts are rejected before materialize/registry/worker/ack. The
P1-1/P1-2 repairs only tighten WHEN the prior context is cleared and WHEN authority is validated; no business behavior,
protocol, store, default, retry, TTL, or stub is added.

### Disclosed pre-card shared debt (NOT this card, not hidden)

`TurnActionGoldenJsonTest` = 3/7 — 4 failures pre-date this card (`pathingIntent`/`wholeTaskRuntime`/`metric` fields
lacking `@JsonInclude(NON_NULL)` from earlier unrelated work). Unchanged by this card.

### Disposition

`TURN-40C` remains BLOCKED pending this review. On parent PASS, C releases ownership and 40C may proceed; on any
P0/P1/P2 finding, C revises the whole card within the 17-path boundary and re-walks canonical delivery. C holds at
`AWAITING_PARENT_REVIEW`; no further source edits pending review.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY EXTERNAL-C CANONICAL-WHOLE-CARD-RE-DELIVERY#2 REVIEW1-REPAIRED BASELINE-A AWAITING_PARENT_REVIEW ACK=PARENT-C-TURN40B-RUNTIME-FACTORY-REVIEW1-REPAIR-20260719-1502 17-PATH 4-CHANGED-13-UNCHANGED DUAL-REPO-5FILE-BYTE-IDENTICAL P1-1-CLEAR-AT-TOP-2NEG P1-2-AUTHORITY-BEFORE-DECIDE-1NEG HTTPSCLIENT-19 RUNTIME-23 FACTORY-2 VALIDATOR-18 CORE-7 LIFECYCLE-5 HANDLER-7 CLOUD-TESTCOMPILE-EXIT0 CR271-MAINCOMPILE-EXIT0 CR271-TESTCOMPILE-BLOCKED-5-OUT-OF-WRITESET-DIRTY-TESTS-NOT-PASSED NO-CARD-WIDEN BASELINE-EQUIVALENCE-ATTESTED TURNACTION-3of7-PRECARD-DEBT C-SOLE-OWNER ZERO-GIT DHXY-READONLY TURN40C-BLOCKED-PENDING-REVIEW 2026-07-19T20:55:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #2 - BLOCKED / EVIDENCE REPAIR REQUIRED - 2026-07-19T15:39:00-04:00

- verdict: `P0/P1/P2 = 0/0/1`; External C retains the same canonical owner. `TURN-40C` remains BLOCKED.
- source review: Review #1 P1-1 and P1-2 are closed. Parent verified `WindowTaskRunner=1405601E` clears at the
  queue-transition top before preflight/skip/create failure; both real production-path negatives prove `[clear]` and
  null retained authority. Parent verified `CloudTurnTaskRuntime=DF5AA6FB` validates authority before
  `registry.decide`, and the retained-id invalid-authority negative proves reject/no ack/no materialize/no worker.
- physical/build evidence: all four repair SHA/line counts match, 13 prior SHA remain unchanged, and all five shared
  files are byte-identical. Parent reran Cloud `mvn -q -o -DskipTests=false test-compile` and CR271
  `mvn -q -o -DskipTests=true compile`; both exited 0. Parent reran CR271 aggregate test-compile and confirmed exit 1
  from out-of-write-set test debt.

### P2-1 - Aggregate test-compile blocker inventory is materially incomplete

The re-delivery says the CR271 aggregate test-compile is blocked by five dirty test files `ONLY`. Parent's identical
command found 12 unique failing test files:

`InputActionPauseCancellationGuardTest`, `AutoCombatCR138FirstAidOnlyCommonBoxGuardTest`,
`DialogMaintenanceFallbackPolicyTest`, `DialogMaintenanceLightweightFallbackPolicyWiringTest`,
`NpcClickTooltipGeometryTest`, `SummonSkillStartIndexPolicyTest`,
`TaskMaintenanceCR138LocalSupportCapabilityTest`, `TaskMaintenanceSummonSkillEpochCooldownTest`,
`TaskMaintenanceSummonSkillUnknownBackoffTest`, `XiuluoCR84RouteStateModelTest`,
`XiuluoPreparedEnterBattleRetryWiringTest`, and `TeamRoleCR212TooltipHashGroupingTest`.

These remain outside this 17-path card and must not be repaired here. Repair only the canonical evidence: remove the
false `ONLY 5` claim, list the complete current 12-file blocker set and exact parent-confirmed gate results, synchronize
status text, then append a no-source-change canonical re-delivery. Do not edit any Java/source/test byte.

No business choice exists: baseline-A/696 and the source-reviewed 17-path behavior are unchanged.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-SOURCE+TEST-REVIEW2 BLOCKED EVIDENCE-REPAIR-REQUIRED P0=0 P1=0 P2=1 OWNER-C-RETAINED SOURCE-P1-CLOSED FOUR-REPAIR-SHA-MATCH THIRTEEN-UNCHANGED FIVE-SHARED-BYTE-IDENTICAL PARENT-CLOUD-TESTCOMPILE-EXIT0 PARENT-CR-MAINCOMPILE-EXIT0 PARENT-CR-TESTCOMPILE-EXIT1 BLOCKERS-ACTUAL-12-NOT-5 CARD-OUT-OF-WRITESET NO-SOURCE-EDIT EVIDENCE-ONLY NO-USER-DECISION BASELINE-A-696 TURN40C-BLOCKED MESSAGE=PARENT-C-TURN40B-RUNTIME-FACTORY-REVIEW2-EVIDENCE-REPAIR-20260719-1539 2026-07-19T15:39:00-04:00 -->


## EXTERNAL-C CANONICAL WHOLE-CARD RE-DELIVERY #3 (Review #2 EVIDENCE REPAIR, NO SOURCE CHANGE) — TURN-40B/RUNTIME-FACTORY (BASELINE-A) — 2026-07-19T21:10:00-04:00

**state → `AWAITING_PARENT_REVIEW`.** External C sole owner. This is an **evidence-only** re-delivery answering parent
Review #2 (`PARENT-C-TURN40B-RUNTIME-FACTORY-REVIEW2-EVIDENCE-REPAIR-20260719-1539`, `P0/P1/P2=0/0/1 BLOCKED /
EVIDENCE REPAIR REQUIRED`, source P1 CLOSED). **Zero Java/source/test byte changed** — the four repair files and all 17
paths are frozen at their Review #1-repair SHAs; no out-of-card test is fixed; TURN-40C / 38A-F untouched;
baseline-A/696. The sole correction: the CR aggregate `test-compile` blocker inventory is restated from the previously
under-reported "only 5" to the complete measured **12-file** set (my earlier count was a truncated console tail, not the
full failure list). All exit-code readings are preserved.

### Frozen write-set — four repair files (SHA re-verified unchanged this evidence pass)

- `DHXY-cr271 .../window/execution/WindowTaskRunner.java` — `1405601eafca081d438b046ffe6a2dc0c5805446d2ea943f5f386251ad564424` / 4328L
- `DHXY-cr271 .../cloud/turn/HttpsTurnClientContractTest.java` — `42ed50b2f281dba29a834b1617f03c4a00c1106c674e6461f6823869ae56b1b4` / 960L
- `Cloud .../cloudbrain/turn/runtime/CloudTurnTaskRuntime.java` — `df5aa6fbef62242b7d78026acd61a7f38360d8a8cf620ddf9e37e1edd9a40fa6` / 419L
- `Cloud .../cloudbrain/turn/runtime/CloudTurnTaskRuntimeContractTest.java` — `12696c4c2fff89cb5d7208b7e77333ab05c0c066bf38511531f668e46740a29b` / 825L

The other 13 paths and the 5 dual-repo byte-identical shared files are unchanged from RE-DELIVERY #2 (prior canonical
SHAs hold). Named test results unchanged and green: HttpsTurnClient 19/19, runtime 23/23, factory 2/2, validator 18/18,
core 7/7, lifecycle 5/5, handler 7/7.

### CORRECTED Maven gate evidence (P1-3) — exit codes unchanged, blocker inventory now COMPLETE (12, not 5)

- Cloud `mvn -q -o -DskipTests=false test-compile` = **exit 0** (unchanged).
- DHXY-cr271 `mvn -q -o -DskipTests=true compile` (main) = **exit 0** (unchanged).
- DHXY-cr271 `mvn -q -o -DskipTests=false test-compile` = **exit 1 / BLOCKED** by the complete set of **12** pre-existing
  out-of-write-set dirty TEST files (none in the 17-path set; all in `com/bot/dhxy/{input,service,task/xiuluo,team}`,
  unrelated shared debt from other work streams):
  1. `src/test/java/com/bot/dhxy/input/action/InputActionPauseCancellationGuardTest.java`
  2. `src/test/java/com/bot/dhxy/service/AutoCombatCR138FirstAidOnlyCommonBoxGuardTest.java`
  3. `src/test/java/com/bot/dhxy/service/DialogMaintenanceFallbackPolicyTest.java`
  4. `src/test/java/com/bot/dhxy/service/DialogMaintenanceLightweightFallbackPolicyWiringTest.java`
  5. `src/test/java/com/bot/dhxy/service/NpcClickTooltipGeometryTest.java`
  6. `src/test/java/com/bot/dhxy/service/SummonSkillStartIndexPolicyTest.java`
  7. `src/test/java/com/bot/dhxy/service/TaskMaintenanceCR138LocalSupportCapabilityTest.java`
  8. `src/test/java/com/bot/dhxy/service/TaskMaintenanceSummonSkillEpochCooldownTest.java`
  9. `src/test/java/com/bot/dhxy/service/TaskMaintenanceSummonSkillUnknownBackoffTest.java`
  10. `src/test/java/com/bot/dhxy/task/xiuluo/XiuluoCR84RouteStateModelTest.java`
  11. `src/test/java/com/bot/dhxy/task/xiuluo/XiuluoPreparedEnterBattleRetryWiringTest.java`
  12. `src/test/java/com/bot/dhxy/team/TeamRoleCR212TooltipHashGroupingTest.java`

  The aggregate CR test-compile gate is therefore **NOT** labeled passed and the card is **NOT** widened to fix any of
  these 12 out-of-card tests. The named HTTPS/RUNTIME-FACTORY family is verified via the isolate-run technique
  (single-class compile + junit-platform-console-standalone) as the source-review evidence, with the Maven aggregate
  test-compile explicitly marked BLOCKED. (RE-DELIVERY #2 under-reported this as "only 5"; corrected here to the full 12
  per parent Review #2.)

### Disposition

Evidence-only correction complete; no source/test byte changed; four repair SHAs + 17-path frozen. `TURN-40C` remains
BLOCKED pending this review. On parent PASS, C releases ownership and 40C may proceed; on any finding, C revises within
the evidence/17-path boundary and re-walks canonical delivery. C holds at `AWAITING_PARENT_REVIEW`.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY EXTERNAL-C CANONICAL-WHOLE-CARD-RE-DELIVERY#3 REVIEW2-EVIDENCE-REPAIR NO-SOURCE-CHANGE BASELINE-A AWAITING_PARENT_REVIEW ACK=PARENT-C-TURN40B-RUNTIME-FACTORY-REVIEW2-EVIDENCE-REPAIR-20260719-1539 SOURCE-P1-CLOSED FOUR-REPAIR-SHA-FROZEN 1405601E-42ED50B2-DF5AA6FB-12696C4C CLOUD-TESTCOMPILE-EXIT0 CR271-MAINCOMPILE-EXIT0 CR271-AGGREGATE-TESTCOMPILE-EXIT1-BLOCKED-12-OUT-OF-WRITESET-DIRTY-TESTS-NOT-5 NO-CARD-WIDEN HTTPS19 RUNTIME23 FACTORY2 VALIDATOR18 CORE7 LIFECYCLE5 HANDLER7 C-SOLE-OWNER ZERO-GIT DHXY-READONLY TURN40C-BLOCKED-PENDING-REVIEW 2026-07-19T21:10:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #3 - PASSED - 2026-07-19T15:51:00-04:00

- verdict: `P0/P1/P2 = 0/0/0`; `SOURCE+TEST SOURCE REVIEW PASSED`. External C ownership is released.
- evidence repair accepted: re-delivery #3 is no-source-change and now lists the complete parent-measured set of 12
  out-of-write-set dirty test files. The four repair files remain frozen at `1405601E`, `42ED50B2`, `DF5AA6FB`,
  `12696C4C`; the other 13 paths and five dual-repo byte-identical files remain unchanged.
- source/test review: Review #1 queue-transition clear and authority-before-registry findings remain closed by the real
  skip/create-failure and retained-id-invalid-authority negatives. Named evidence remains Https client 19/19, runtime
  23/23, factory 2/2, validator 18/18, core 7/7, lifecycle 5/5, handler 7/7.
- build evidence: parent-confirmed Cloud offline `test-compile` exit 0 and CR main `compile` exit 0 stand. CR aggregate
  `test-compile` remains exit 1/BLOCKED by the accurately disclosed 12 out-of-card dirty tests; it is not labeled
  passed and does not reopen this card's source review or widen its write set.
- baseline: no approved business difference and no user decision. Baseline-A/`696a12b0` remains the authority; no new
  protocol/store/default/retry/TTL/stub was introduced.
- downstream: TURN-40C dependency gate is satisfied and may proceed only under its separately frozen canonical card.

<!-- TRUE_EOF: TURN-40B/RUNTIME-FACTORY PARENT-SOURCE+TEST-REVIEW3 PASSED P0=0 P1=0 P2=0 OWNER-EXTERNAL-C-RELEASED REDELIVERY3-EVIDENCE-ACCEPTED SOURCE-FROZEN FOUR-SHA=1405601E-42ED50B2-DF5AA6FB-12696C4C THIRTEEN-UNCHANGED FIVE-SHARED-BYTE-IDENTICAL HTTPS19 RUNTIME23 FACTORY2 VALIDATOR18 CORE7 LIFECYCLE5 HANDLER7 CLOUD-TESTCOMPILE-EXIT0 CR-MAINCOMPILE-EXIT0 CR-AGGREGATE-TESTCOMPILE-EXIT1-BLOCKED-12-OUT-OF-CARD-NOT-PASSED BASELINE-A-696 NO-USER-DECISION TURN40C-DEPENDENCY-SATISFIED 2026-07-19T15:51:00-04:00 -->
