# TURN-39C1 - Cloud Legacy Input Cohort Retirement

## Canonical Status

- status: `READY / ZERO OWNER / UNASSIGNED`
- type: `DELETE / INTEGRATION`
- dependsOn: `TURN-39K`, `TURN-40B-C4`, `TURN-39W` parent source reviews passed and owners released
- authority: this file's physical EOF; the ledger does not assign this card
- business baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`

## Source-Truth Correction

The original five-delete-only proposal assumed all external references were already zero. A full repository symbol
audit after C4 delivery proved one transitive edge remains: `NavigationService` is the sole active external consumer
of `InputActionScope`. C4 correctly preserved those checks under its own frozen contract. This successor card must
retire that edge before deleting the type. The other four cohort types have no active external type edge.

## Frozen Production Write Set

All paths are in `D:\mavenProject\dhxy-cloud-brain`.

1. MODIFY `src/main/java/com/bot/dhxy/service/NavigationService.java`:
   remove the `InputActionScope` import and every active `isCancelled()` reference in the prepare, OCR and scroll
   paths; at the same workflow boundaries use `TaskCheckpoint` directly and the existing turn outcome stop channel.
   Interrupted sleeps must surface through the same task-stop channel. Do not add a local checkpoint wrapper, change
   route order/timing/retry/fallback, add a read, or convert negative mechanical results into business truth.
2. DELETE `src/main/java/com/bot/dhxy/input/InputSequences.java`.
3. DELETE `src/main/java/com/bot/dhxy/input/action/CloudInputActionMapper.java`.
4. DELETE `src/main/java/com/bot/dhxy/input/action/InputAction.java`.
5. DELETE `src/main/java/com/bot/dhxy/input/action/InputActionType.java`.
6. DELETE `src/main/java/com/bot/dhxy/input/action/InputActionScope.java`.

The five deletions occur together only after item 1 has zeroed the active symbol edge. DHXY's same-named local input
types are outside this card and remain live. The 17-file facade SCC remains wholly owned by TURN-44A.

## Frozen Test Write Set

1. MODIFY `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/NavigationTurnContractTest.java`:
   retain all 19 C4 tests and add/adjust direct proof that the prepare, post-capture/OCR and scroll-loop checkpoints
   use the existing task stop/pause channel with no `InputActionScope`, no extra input command and unchanged order.
2. CREATE `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/OldFacadeRemovalContractTest.java`:
   prove active production symbol ownership of all five deleted Cloud types is zero; distinguish explanatory
   historical strings from imports/type/method calls; retain the explicit TURN-44A SCC allowlist.
3. READ-ONLY named regression: `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wubei/WubeiWholeTaskTurnContractTest.java`.

## Acceptance And Build Gate

- source: zero active import/type/method references to the five deleted Cloud types; no second stop channel, queue,
  protocol, store, stub, constant result or copied business algorithm.
- behavior: exact-HWND keyboard remains background and cross-window parallel; only mouse is foreground/global-serial;
  same-window steps remain ordered. `无已批准业务差异；按基线等价迁移`.
- named tests: `NavigationTurnContractTest`, `OldFacadeRemovalContractTest`, and
  `WubeiWholeTaskTurnContractTest` only, plus applicable Cloud compile.
- compile point: one Cloud compile/test point after Navigation migration and all five deletions are complete. Current
  shared Cloud missing-type debt means the build gate is `BLOCKED/PENDING`, not waived or passed.
- prohibited: runtime/application/server/Task/UI/capture/input and changes to `D:\mavenProject\DHXY`.

<!-- TRUE_EOF: TURN-39C1 FIXED-ORIGINAL-CARD READY ZERO-OWNER UNASSIGNED SOURCE-TRUE-CORRECTION NAVIGATION-INPUTACTIONSCOPE-SOLE-EXTERNAL-EDGE NAV+TEST-MODIFY FIVE-LEGACY-FILES-DELETE OLDFACADE-TEST-CREATE TASKCHECKPOINT+TURN-OUTCOME-ONLY NO-BUSINESS-DIFFERENCE BUILD-BLOCKED-PENDING 2026-07-18T21:51:19Z -->

## WHOLE-CARD CLAIM - EXTERNAL-A - 2026-07-19T05:43:00-04:00

- owner: `EXTERNAL-A`
- claim_type: canonical whole-card claim (append at physical EOF per card authority)
- card: `TURN-39C1` — Cloud Legacy Input Cohort Retirement (DELETE / INTEGRATION)
- basis: card status `READY / ZERO OWNER / UNASSIGNED`; dependsOn satisfied (TURN-39K + TURN-40B-C4 + TURN-39W all
  parent-source-review PASSED and owners released). Pre-check: full card read + mtime (2026-07-18 17:58:44) done as
  an independent tool call before this append.
- write_set (frozen, dhxy-cloud-brain only): prod MODIFY `service/NavigationService.java` (remove InputActionScope
  import + every active isCancelled() edge in prepare/OCR/scroll → TaskCheckpoint + existing turn-outcome stop
  channel; no local wrapper/second store/route-order/timing/retry change) + DELETE 5 legacy files
  (InputSequences/CloudInputActionMapper/InputAction/InputActionType/InputActionScope, only after item 1 zeroes the
  edge); test MODIFY `service/NavigationTurnContractTest.java` (retain 19 C4 tests + checkpoint proof) + CREATE
  `turn/client/OldFacadeRemovalContractTest.java` (prove zero active symbol ownership of the 5 types) + READ-ONLY
  `task/wubei/WubeiWholeTaskTurnContractTest.java`. 17-file facade SCC = TURN-44A (out of scope). DHXY same-named
  local types out of scope.
- discipline: zero Git mutation; baseline-equivalent (no second stop channel/queue/protocol/store/stub/copied
  algorithm); named tests only (NavigationTurnContractTest/OldFacadeRemovalContractTest/WubeiWholeTaskTurnContractTest)
  + applicable Cloud compile; build gate BLOCKED/PENDING (shared missing-type debt); no runtime/UI/capture/input;
  D:\mavenProject\DHXY untouched.
- next: read-back this EOF to confirm sole earliest owner; if a physically-earlier claim exists, canonical self-withdraw.

<!-- TRUE_EOF: TURN-39C1 CLAIM EXTERNAL-A OWNER WHOLE-CARD DELETE-5-LEGACY+NAV-INPUTACTIONSCOPE-EDGE-RETIRE ANTI-RACE-CARD-EOF 2026-07-19T05:43:00-04:00 -->

## SOURCE+TEST DELIVERED - EXTERNAL-A - 2026-07-19T09:27:00-04:00

- card: `TURN-39C1` — Cloud Legacy Input Cohort Retirement (DELETE / INTEGRATION)
- owner: `EXTERNAL-A` (canonical whole-card claim 05:43; sole owner; C IDLE, no competition).
- production write set — **COMPLETE**:
  1. `service/NavigationService.java`: all **20** active `InputActionScope.isCancelled()` edges migrated to the
     shared task-stop channel — Pattern A `if (isCancelled()) { [log;] return false/NOT_FOUND; }` →
     `TaskCheckpoint.throwIfStopRequested(taskExecutionContextHolder, "<msg>")`; Pattern B
     `if (!TaskSleep.sleep(X) || isCancelled()) return false;` →
     `if (!TaskSleep.sleep(X)) { TaskCheckpoint.throwIfInterrupted("<msg>"); } TaskCheckpoint.throwIfStopRequested(holder, "<msg>");`
     so interrupted sleeps surface through the SAME task-stop channel; the special `return sleep && !isCancelled();`
     expression rewritten to the same sleep-guard + `return true;`. `InputActionScope` import removed. No local
     wrapper / second store / route-order / timing / retry / fallback change; no added read; no negative mechanical
     result converted to business truth. `grep isCancelled = 0`; remaining `InputActionScope` = **2 explanatory
     comment lines only** (2077 line-comment, 2951 javadoc), permitted as historical strings.
  2–6. **DELETED** (only after item 1 zeroed the sole external edge): `input/InputSequences.java`,
     `input/action/CloudInputActionMapper.java`, `input/action/InputAction.java`, `input/action/InputActionType.java`,
     `input/action/InputActionScope.java`.
- test write set — **COMPLETE**:
  1. `service/NavigationTurnContractTest.java`: retained all 19 C4 tests; +2 checkpoint proofs —
     (a) behavioral `prepareWorldMapSearchResultsSurfacesTaskStopThroughCheckpointBeforeAnyInput`: an interrupted
     task thread makes the prepare entry checkpoint throw `TaskStopRequestedException` before any input turn
     (`executeCalls == 0`), proving the stop routes through the task-stop channel, not a boolean cancel, with no
     extra input command; (b) source-truth `navigationServiceRetiresInputActionScopeForTheSharedTaskStopCheckpoint`:
     zero `isCancelled`, zero active (non-comment) `InputActionScope` symbol, and the shared
     `TaskCheckpoint.throwIfStopRequested` + `throwIfInterrupted` channel present.
  2. **CREATED** `turn/client/OldFacadeRemovalContractTest.java`: walks all `src/main/java`, a
     comment/string-literal-blanking state machine distinguishes explanatory historical strings from active
     imports/type/method calls, proving zero active production symbol ownership of the 5 retired types; the
     word-bounded `\bInputAction\b` alternative excludes the live `TurnInputAction`; retains the explicit 17-file
     TURN-44A authority/facade SCC allowlist (out of scope for this card). Depends only on JDK+JUnit.
- **VERIFIED**: `OldFacadeRemovalContractTest` isolate-run (JDK+JUnit, junit-platform-console-standalone 1.10.2) =
  **2/2 PASSED** (5 files deleted + zero tree-wide active symbols of the retired cohort). Whole-tree main `javac`
  (532 files) confirms MY write-set files carry **zero own-fault errors**: zero errors at every one of the 20
  checkpoint sites, `TaskCheckpoint`/`TaskStopRequestedException` compile clean, no `isCancelled`/`InputActionScope`
  symbol errors anywhere in the tree. Constructor arity re-verified: NavigationService `@RequiredArgsConstructor`
  = 20 args (22 `private final` − 2 initialized `random`/`runtimeStates`), matching the harness 1:1 with
  `taskExecutionContextHolder` at slot 15 → `NavigationTurnContractTest` will link once the tree compiles.
- **BUILD GATE: BLOCKED/PENDING** by out-of-write-set shared Cloud missing-type debt — `model/ocr/TextCandidateScanResult`,
  `cloud/decision/CloudDecisionMetricsService` (CloudDecisionResult), `vision/GameTextLineOcrService`, `cloud/task/*CloudDecision`
  etc. (other cards' in-flight DHXY-absent-type migrations) fail their own symbol resolution and cascade into
  NavigationService as missing-package errors that are NOT my edits. `NavigationTurnContractTest` is therefore
  written-but-**UNVERIFIED** pending a compilable tree; **no Maven family is claimed passed**.
- discipline: zero Git mutation (the 5 `rm` deletions are the card's required working-tree removals, not a
  history/index command); no runtime/UI/capture/input; `D:\mavenProject\DHXY` untouched; no rollback of others'
  dirty/untracked files.

<!-- TRUE_EOF: TURN-39C1 SOURCE+TEST DELIVERED EXTERNAL-A NAV-20-EDGE-MIGRATED+5-FILES-DELETED OLDFACADE-2of2-PASSED NAVTEST-WRITTEN-UNVERIFIED BUILD-BLOCKED-PENDING-SHARED-DEBT ZERO-OWN-FAULT-ERRORS 2026-07-19T09:27:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - BLOCKED / REPAIR REQUIRED - 2026-07-18T22:40:24Z

- verdict: `P0/P1/P2=0/2/0`; owner `EXTERNAL-A` retained for test-only repair. Production review found no approved
  business difference: all 20 former cancellation boundaries use the existing `TaskCheckpoint`/turn-stop channel,
  the five legacy files are deleted, active production references are zero, and route/input/order/timing/retry
  behavior plus background-keyboard/foreground-mouse ownership remain unchanged.
- **P1 - frozen checkpoint proof is incomplete:**
  `NavigationTurnContractTest.java:588` proves only the prepare-entry interrupted checkpoint before input, while
  `:619` is a generic source-presence/absence assertion. The frozen test contract requires direct proof at prepare,
  post-capture/OCR, and scroll-loop boundaries through the existing stop/pause channel with no extra command or
  order change. Repair must add boundary-specific behavioral coverage (or equally exact production-boundary proof)
  for post-capture/OCR and scroll-loop stop/pause behavior while retaining the existing 21 tests and all 19 C4 tests.
- **P1 - active-zero guard can false-pass:** `OldFacadeRemovalContractTest.java:112` skips every production file
  whose basename is in `TURN_44A_SCC_ALLOWLIST`. Manual parent scan confirms those 17 files are currently clean,
  but the test would still pass if any later referenced one of the five deleted input types. Repair must scan every
  production Java file for the retired five-type cohort; retain the TURN-44A SCC assertion separately, without using
  it as an exemption from the retired-type scan.
- verification gate: re-deliver the two repaired test files on this same card. `NavigationTurnContractTest`,
  `OldFacadeRemovalContractTest`, `WubeiWholeTaskTurnContractTest`, and Cloud compile remain `BLOCKED/PENDING` on
  shared missing-type debt; do not claim Maven passed. No production expansion, runtime/UI/capture/input, or Git
  mutation is authorized.

<!-- TRUE_EOF: TURN-39C1 PARENT-REVIEW1 BLOCKED REPAIR-REQUIRED P0=0 P1=2 P2=0 OWNER-A-RETAINED TEST-ONLY OCR+SCROLL-CHECKPOINT-DIRECT-PROOF ALL-PRODUCTION-RETIRED-COHORT-SCAN NO-PRODUCTION-BUSINESS-DIFFERENCE BUILD-BLOCKED-PENDING 2026-07-18T22:40:24Z -->

## PARENT COMMUNICATION AUDIT - COMMUNICATION_STALE / REPAIR BYTES ACTIVE - 2026-07-18T22:52:27Z

- External A has not named-ACKed `PARENT-A-TURN39C1-REVIEW1-2240` in two consecutive parent audit rounds, so the
  communication state is `COMMUNICATION_STALE`.
- This is not `ACTIVE_STALE`: both authorized test files have fresh repair bytes (`NavigationTurnContractTest`
  SHA-256 `79D48FE0...`, `OldFacadeRemovalContractTest` `A4F10EF6...`) while production Navigation remains unchanged
  at `B57ECC50...`. Owner A and Review #1 `0/2/0` test-only repair scope remain.
- No reallocation, Maven, runtime/input, production expansion, or review conclusion change.

<!-- TRUE_EOF: TURN-39C1 PARENT-COMMUNICATION-AUDIT COMMUNICATION-STALE NOT-ACTIVE-STALE FRESH-TEST-REPAIR-BYTES OWNER-A-RETAINED REVIEW1-0-2-0 NO-REALLOCATION NO-MAVEN 2026-07-18T22:52:27Z -->

## SOURCE+TEST RE-DELIVERED (Review#1 P1 repair) - EXTERNAL-A - 2026-07-19T09:57:00-04:00

- responds to: `PARENT-A-TURN39C1-REVIEW1-2240` (Review #1 P0/P1/P2 = 0/2/0, test-only repair; production accepted
  unchanged, no expansion). ack in ledger STATUS EVENT `09:57`.
- production: **UNCHANGED** — all 20 checkpoint migrations, 5 deletions, order/timing/retry/input-ownership and
  no-business-difference contract preserved exactly as delivered; no production edit this repair.
- **P1 repair 1 (NavigationTurnContractTest.java)** — added two DIRECT behavioral checkpoint proofs through the
  existing task-stop channel, retaining all 21 prior tests (19 C4 + prepare-entry + source-truth) → now **23 @Test**:
  1. `clickDestinationSurfacesTaskStopAtPostCaptureOcrCheckpointBeforeAnyRouteClick`: successful capture
     (`StubCapturingTracker.captureToFile→true`) + a task stop requested inside the destination OCR double →
     the post-capture/OCR `TaskCheckpoint.throwIfStopRequested` (NavigationService 2034/2038) throws
     `TaskStopRequestedException` and issues **zero** route-click input turns (`executeCalls==0`). Zero extra
     input, unchanged order.
  2. `scrollWorldMapSearchResultsToBottomDirectSurfacesTaskStopAtItsLoopCheckpointMidScroll`: a stop requested
     while the first scroll-down turn is in flight → the in-loop checkpoint (interrupted scroll-interval sleep →
     `throwIfInterrupted`, NavigationService 2308/2310) throws after exactly the focus click + one scroll
     (`executeCalls==2`, vocabulary `[CLICK_LEFT, SCROLL]`), no further scroll turns. Mid-loop stop honored via
     the task-stop channel, zero extra input, unchanged order.
  Technique: interrupt injected into a scripted reply / OCR double so the SPECIFIC mid-method checkpoint fires
  (not merely method-entry). No production seam, no new input command, no order change.
- **P1 repair 2 (OldFacadeRemovalContractTest.java)** — the retired-cohort scan no longer skips the TURN-44A SCC
  files; it now walks and scans **every** production Java file (the five deleted types must be zero everywhere,
  SCC included). The 17-file SCC is kept as a **separate** assertion
  `turn44aFacadeSccIsRetainedUnderRemoteForItsOwnCard` (each member still present under `remote/`, retained for
  TURN-44A) that never excuses a retired-input reference. Now **3 @Test**.
- **VERIFIED**: `OldFacadeRemovalContractTest` isolate-run (JDK+JUnit, standalone 1.10.2) = **3/3 PASSED**
  (files deleted; zero active retired-cohort symbols across ALL production incl. SCC; 17 SCC members present).
  New NavigationTurnContractTest proofs mirror the already-passing prepare/CLICKED harness idioms 1:1 (confirmed
  symbols: `enqueue`/`inputReply`/`latestWindow`, exact method signatures for the two callers).
- **BUILD GATE: BLOCKED/PENDING** unchanged — out-of-write-set shared Cloud missing-type debt (TextCandidateScanResult
  / CloudDecisionResult / GameTextLineOcrService / *CloudDecision) still blocks whole-tree compile, so
  NavigationTurnContractTest (needs the tree) stays written-but-**UNVERIFIED**; **no Maven family claimed passed**.

<!-- TRUE_EOF: TURN-39C1 SOURCE+TEST RE-DELIVERED REVIEW1-P1-REPAIR EXTERNAL-A ACK-PARENT-A-TURN39C1-REVIEW1-2240 NAVTEST-23T-OCR+SCROLL-DIRECT-PROOF OLDFACADE-3of3-PASSED-SCAN-ALL+SCC-SEPARATE PRODUCTION-UNCHANGED BUILD-BLOCKED-PENDING 2026-07-19T09:57:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #2 - PASSED - 2026-07-18T22:57:25Z

- verdict: `P0/P1/P2=0/0/0`; `SOURCE+TEST SOURCE REVIEW PASSED`; External A owner released and communication
  recovered by the 09:57 double ACK.
- Review #1 P1-1 closed: `NavigationTurnContractTest` now has 23 tests. The OCR proof interrupts inside the real
  destination-OCR collaborator after successful capture and observes the real post-OCR `TaskCheckpoint` before any
  route click. The scroll proof interrupts in the first real scroll turn and observes the loop sleep/checkpoint after
  exactly `[CLICK_LEFT, SCROLL]`, with no subsequent scroll. These are boundary-specific behavioral proofs, not
  generic source-only assertions.
- Review #1 P1-2 closed: `OldFacadeRemovalContractTest` scans every production Java file for all five retired types;
  no TURN-44A SCC file is exempt. Its third test separately proves all 17 SCC files remain under `remote/` without
  weakening the retired-symbol scan. Isolated evidence is 3/3 passed.
- production remains SHA-256 `B57ECC50...`; route/input/order/timing/retry behavior and exact-HWND background
  keyboard / foreground-global-serial mouse ownership remain baseline-equivalent. `无已批准业务差异；按基线等价迁移`.
- build gate: named `NavigationTurnContractTest`, `OldFacadeRemovalContractTest`,
  `WubeiWholeTaskTurnContractTest`, and Cloud compile remain `BLOCKED/PENDING` on shared missing-type debt; this
  source-review pass does not claim Maven execution or runtime acceptance.

<!-- TRUE_EOF: TURN-39C1 PARENT-SOURCE+TEST-SOURCE-REVIEW2 PASSED P0=0 P1=0 P2=0 OWNER-A-RELEASED COMMUNICATION-RECOVERED PROD=B57ECC50 NAVTEST=79D48FE0-23T OLDFACADE=A4F10EF6-3T WUBEI=865A5311 BUILD+NAMED-TEST-BLOCKED-PENDING NO-BUSINESS-DIFFERENCE 2026-07-18T22:57:25Z -->
