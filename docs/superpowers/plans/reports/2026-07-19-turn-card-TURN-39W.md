# TURN-39W - Cloud Wubei Input Caller Retirement

## Canonical Status

- status: `READY / ZERO OWNER / UNASSIGNED`
- type: `INTEGRATION / SOURCE+TEST`
- dependsOn: `TURN-39P1 Review #15 PASSED`, `TURN-39K Source Review #2 PASSED`, and TURN-40B C2/C3 source gates
  passed. The separated C2 bag test debt does not own or collide with this card's files.
- claim rule: the first eligible Worker that can finish this whole card may append a canonical claim at this
  physical EOF before editing. The ledger does not assign this card.

## Approved Runtime Invariant

- All keyboard actions are exact-HWND background operations and may run concurrently across windows; only mouse
  remains foreground/global-serial. Same-window turn step order stays sequential.
- Preserve the user-approved business baseline and the four existing Wubei caller sequences, delays, result truth,
  retries and fallback order. This card changes input ownership only.
- Do not create a second queue/store/protocol, foreground keyboard fallback, stub, constant result or copied
  business algorithm. `无已批准业务差异；按基线等价迁移`.

## Frozen Cloud Production Write Set

All paths are in `D:\mavenProject\dhxy-cloud-brain`:

1. `src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java` - migrate the four frozen active compatibility caller
   sites to existing turn mouse-sequence/`KEY_TAP` actions; remove the retired `InputSequences` import, field and
   constructor parameter only after all four callers are migrated.
2. `src/main/java/com/bot/dhxy/task/wuhuan/FiveRingTaskV2.java` - Javadoc/reference cleanup only.
3. `src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java` - Javadoc/reference cleanup only.

Pre-claim source evidence: Wubei `9D537DBF...`, FiveRing `ECBCA059...`, Xiuluo `6B90ECD1...` (SHA-256 at card
publication). `NavigationService` and the five-file legacy deletion cohort are outside this write set.

## Frozen Test Write Set

1. `src/test/java/com/yueyunfe/dhxy/cloudbrain/task/wubei/WubeiWholeTaskTurnContractTest.java`

The retained test must remove the positional `InputSequences` constructor argument and prove all four migrated
caller sequences, action order, exact waits/timing, terminal truth and no compatibility input call. Existing
business assertions remain intact.

## Build Gate

- Run only the named `WubeiWholeTaskTurnContractTest` and applicable Cloud compile after Java writers are stable.
- Do not run runtime/application/server/Task/UI/capture/input.

<!-- TRUE_EOF: TURN-39W READY ZERO-OWNER UNASSIGNED WHOLE-CARD CLOUD WUBEI-4-CALLERS+1-TEST+2-JAVADOC EXACT-HWND-BACKGROUND-KEYBOARD ONLY-MOUSE-FOREGROUND-GLOBAL-SERIAL NO-BUSINESS-DIFFERENCE 2026-07-19T03:05:00-04:00 -->

## WHOLE-CARD CLAIM - EXTERNAL-A - 2026-07-19T01:48:00-04:00

- owner: `EXTERNAL-A`
- claim_type: canonical whole-card claim (append at physical EOF per card claim rule)
- card: `TURN-39W` — Cloud Wubei Input Caller Retirement
- basis: card status `READY / ZERO OWNER / UNASSIGNED`; dependsOn satisfied (TURN-39K Source Review #2 PASSED
  0/0/0 owner-released; TURN-39P1 #15; 40B C2/C3 gates). Pre-check: full card read + mtime (2026-07-18 14:10:47)
  done as an independent tool call before this append. Parent registry (03:05) authorizes A self-scan+claim.
- write_set (frozen, dhxy-cloud-brain only): prod `task/wubei/WubeiTask.java` (migrate 4 InputSequences callers to
  turn mouse-sequence/KEY_TAP + remove InputSequences import/field/ctor after all four migrated),
  `task/wuhuan/FiveRingTaskV2.java` + `task/xiuluo/XiuluoTaskV2.java` (Javadoc/reference cleanup only);
  test `cloudbrain/task/wubei/WubeiWholeTaskTurnContractTest.java` (drop positional InputSequences ctor arg, prove
  4 migrated caller sequences/order/timing/terminal truth/no compat input call). NavigationService + 5-file legacy
  deletion cohort OUT of scope.
- discipline: zero Git mutation; baseline-equivalent business (no second queue/store/protocol/foreground fallback/
  stub/copied algorithm); only named WubeiWholeTaskTurnContractTest + applicable Cloud compile after writers stable.
- next: read-back this EOF to confirm sole earliest owner; if a physically-earlier claim exists, canonical self-withdraw.

<!-- TRUE_EOF: TURN-39W CLAIM EXTERNAL-A OWNER WHOLE-CARD CLOUD-WUBEI-4-CALLERS ANTI-RACE-CARD-EOF 2026-07-19T01:48:00-04:00 -->

## OWNER PROGRESS - EXTERNAL-A - 2026-07-19T02:28:00-04:00

- SOURCE migration complete (all 4 InputSequences callers → turn actions; InputSequences import/field/ctor removed;
  FiveRing/Xiuluo Javadoc cleaned; test positional InputSequences ctor arg removed).
- SOURCE verification: a whole-tree `mvn -q compile` (cloud-brain) reports **zero compile errors in the 3 write-set
  production files** (WubeiTask/FiveRingTaskV2/XiuluoTaskV2) — javac reached and type-checked them cleanly.
- CLOUD BUILD-GATE BLOCKED (out of this write set): the Cloud main tree does not compile because of in-flight
  migration files NOT in this write set — `service/NavigationService.java` (TURN-40B-C4, in progress; unresolved
  DHXY-only types), `input/action/InputActionScope.java` (legacy-deletion cohort/TURN-39C1), and
  `model/ocr/TextCandidateScanResult.java`. Same blocked-gate class as TURN-39K, but the Cloud main (not just tests)
  is un-buildable, so the named `WubeiWholeTaskTurnContractTest` cannot run and isolate-compiling WubeiTask is hard
  (it depends on NavigationService as a type). Not touched.
- QUESTION TO PARENT (mirrored in ledger 02:28): 39W source is complete + type-clean; the Cloud build gate is
  blocked by out-of-write-set in-flight migration. (a) Accept isolate evidence + allow source delivery marking
  `CLOUD BUILD GATE BLOCKED/PENDING BY OUT-OF-WRITE-SET IN-FLIGHT MIGRATION` (per the TURN-39K precedent), or
  (b) hold the named-test gate until TURN-40B-C4 / TURN-39C1 make the Cloud tree buildable? Test-proof source
  continues in parallel meanwhile.

<!-- TRUE_EOF: TURN-39W OWNER-A SOURCE-DONE MY-3-FILES-ZERO-COMPILE-ERRORS CLOUD-GATE-BLOCKED-BY-OUT-OF-WRITESET-INFLIGHT-MIGRATION ASK-PARENT-ISOLATE-VS-WAIT 2026-07-19T02:28:00-04:00 -->

## PARENT DELIVERY / BUILD-GATE ADJUDICATION - 2026-07-19T02:32:00-04:00

- Option (a) is approved only after the frozen `WubeiWholeTaskTurnContractTest` proof source is complete. Then A
  may append canonical whole-card `SOURCE+TEST DELIVERED` and record Cloud compile/named test as
  `BLOCKED/PENDING BY OUT-OF-WRITE-SET IN-FLIGHT MIGRATION`.
- Production-only delivery is not accepted. Source review and build execution are separate gates: do not wait for
  C4/39C1 to restore the whole tree before requesting source+test review, but do not claim compile/test passed.
- No further Maven while C is an active Cloud Java writer. Complete only the frozen test source and delivery
  evidence; owner A and the original write set remain unchanged.

<!-- TRUE_EOF: TURN-39W PARENT-ADJUDICATION COMPLETE-FROZEN-TEST-SOURCE-THEN-SOURCE+TEST-DELIVERY BUILD-GATE-BLOCKED-PENDING-OUTSIDE-WRITESET NO-PRODUCTION-ONLY-DELIVERY NO-MORE-MAVEN OWNER-A ACK-PENDING 2026-07-19T02:32:00-04:00 -->

## PARENT ACK / SOURCE STATUS RECONCILIATION - 2026-07-19T02:40:00-04:00

- A's 02:38 ledger STATUS EVENT explicitly ACKed `PARENT-A-TURN39W-DELIVERY-BUILD-GATE-0432` and accepted the
  complete-test-source-before-delivery rule, BLOCKED/PENDING build gate and no-more-Maven condition.
- Frozen test scaffolding is now present, but the four caller proofs are still in progress. Canonical state remains
  `OWNER EXTERNAL-A / SOURCE ACTIVE`; no delivery or parent source review yet.

<!-- TRUE_EOF: TURN-39W PARENT-ACK-RECONCILIATION OWNER-A TEST-SOURCE-ACTIVE FOUR-PROOFS-PENDING BUILD-BLOCKED-PENDING COMMUNICATION-NORMAL NO-DELIVERY 2026-07-19T02:40:00-04:00 -->

## OWNER PROGRESS #2 - EXTERNAL-A - 2026-07-19T02:53:00-04:00

- Test source (per parent 0432 "complete frozen test source before delivery"): 2 of 4 migrated-caller proofs cleanly
  written in WubeiWholeTaskTurnContractTest, driving REAL production private callers by reflection through a recording
  CloudTurnCommandPort:
  - `postAcceptAltCDismountIssuesTheExactBackgroundKeyTapTurnNotInputSequences` (startPostAcceptPrepath →
    [KEY_TAP "Alt+C", WAIT 120]).
  - `chainedTrackerFastClickIssuesTheExactMouseSequenceTurnNotInputSequences` (clickCachedChainedTrackerGreen →
    [MOVE_MOUSE, WAIT 120, CLICK_LEFT clickDelay 300]).
  - Scaffolding: RecordingCommandPort (default-completes surrounding fire-and-forget local-service turns),
    terminalReply/bound/invoke/assertPointStep/mouseSequenceAction helpers, harness recording()+botProperties() options
    (default off → the 12 existing tests unchanged).
- The other 2 callers use the SAME executeInputTurn helper + step builders but are hard to drive/verify:
  - `clickTaskTrackerGreen` NPEs on null `dialogService.detectDialogTypeNoFocus` before its click, needs a scripted
    DialogService (@RequiredArgsConstructor, many args), and its click point is RANDOMIZED (only structure + same
    move/click point + WAIT 120 / CLICK 300 assertable, not exact coords).
  - `tryConsumePreparedWubeiDialog` needs a published preparedState PreparedDialogAction reaching the GREEN-template
    branch to reach its click ([MOVE, WAIT 80, CLICK 150]).
  - Both are UNVERIFIABLE now (Cloud main broken by out-of-write-set NavigationService/InputActionScope/
    TextCandidateScanResult; C active → no Maven), so blind-writing scripted DialogService/preparedState is high-risk.
- QUESTION TO PARENT (ledger 02:53): given the 2 representative end-to-end proofs cover both the keyboard and mouse
  paths through the shared helper/builders, do they suffice for "complete test source", or must the other 2 be
  written best-effort blind (scripted DialogService/preparedState, verified once the tree is buildable)?

<!-- TRUE_EOF: TURN-39W OWNER-A TEST-2OF4-CLEAN-REFLECTION-PROOFS 2-REMAIN-SCRIPTED-DIALOGSERVICE+PREPAREDSTATE-UNVERIFIABLE ASK-PARENT-2-REP-VS-ALL-4 BUILD-GATE-BLOCKED 2026-07-19T02:53:00-04:00 -->

## PARENT TEST-CONTRACT ADJUDICATION - 2026-07-19T05:22:00-04:00

- decision: the frozen acceptance requires **all four migrated caller sequences**. The two representative
  keyboard/mouse proofs do not prove reachability, branch wiring, caller-specific timing or terminal truth for
  `clickTaskTrackerGreen` and `tryConsumePreparedWubeiDialog`; `2/4` is not complete test source and cannot support
  canonical `SOURCE+TEST DELIVERED`.
- required closure: add caller-level proofs for both remaining real production callers in the existing test file.
  For randomized tracker coordinates, assert MOVE and CLICK use the same produced point plus exact `WAIT 120` and
  click delay `300`. For prepared dialog, publish the required prepared state and prove the real GREEN branch emits
  MOVE / WAIT `80` / CLICK delay `150` with existing terminal semantics.
- do not add a production seam, stub, constant result, second protocol/store or business fallback for reachability.
  If the remaining callers cannot be driven safely in the frozen test file, keep the card `BLOCKED` and report the
  one exact test-only blocker; do not downgrade the four-caller contract.
- Cloud compile/named test may remain `BLOCKED/PENDING BY OUT-OF-WRITE-SET IN-FLIGHT MIGRATION`; no Maven while C is
  an active Cloud Java writer. The build gate does not reduce source-test completeness from `4/4`.

<!-- TRUE_EOF: TURN-39W PARENT-TEST-CONTRACT ALL-4-CALLER-PROOFS-REQUIRED 2-REPRESENTATIVE-NOT-SUFFICIENT RANDOM-POINT-STRUCTURAL-ASSERT PREPARED-GREEN-80-150 NO-PRODUCTION-SEAM BUILD-GATE-SEPARATE OWNER-A ACK-PENDING 2026-07-19T05:22:00-04:00 -->

## PARENT ACK / SOURCE STATUS RECONCILIATION #2 - 2026-07-19T05:32:00-04:00

- A's next STATUS EVENT explicitly ACKed `PARENT-A-TURN39W-ALL4-CALLER-PROOFS-0522` and accepted every test,
  safety and build-gate condition. Communication is normal.
- Test source is now `3/4`: the randomized tracker caller directly drives production and proves same-point
  MOVE/CLICK plus exact WAIT120/CLICK300 without a production seam. Only prepared GREEN WAIT80/CLICK150 remains.
- Canonical state remains OWNER EXTERNAL-A / SOURCE ACTIVE / NO DELIVERY; source is progressing with no blocker.

<!-- TRUE_EOF: TURN-39W PARENT-ACK2 OWNER-A SOURCE-ACTIVE TEST-3-OF-4 ALL4-CONTRACT-ACKED REMAIN=PREPARED-GREEN NO-BLOCKER NO-DELIVERY BUILD-BLOCKED-PENDING PHYSICAL-EOF 2026-07-19T05:32:00-04:00 -->

## CANONICAL SOURCE+TEST DELIVERED - EXTERNAL-A - 2026-07-19T03:13:00-04:00

Per parent `PARENT-A-TURN39W-DELIVERY-BUILD-GATE-0432` + `PARENT-A-TURN39W-ALL4-CALLER-PROOFS-0522` (all four
migrated callers proven; no production seam; build gate BLOCKED/PENDING; no Maven while C active; tests not claimed
passed).

### Frozen write set — fresh blobs (git hash-object)
1. `3a696e4cc408c5922ce0ffeefc8eea5812d8685f`  WubeiTask.java (4504L) — SOURCE
2. `49526761df325ab913993931dee725b93f76fe33`  FiveRingTaskV2.java (3043L) — SOURCE (Javadoc-only)
3. `91c2572fb5db42be6928d7473ade4cd327942ea2`  XiuluoTaskV2.java (4554L) — SOURCE (Javadoc-only)
4. `469e6dec51b8ec3ef942842ba7846f4b728b30d1`  WubeiWholeTaskTurnContractTest.java (799L) — TEST

### Source
- `WubeiTask`: all four `InputSequences` callers migrated to turn actions via a new `executeInputTurn` helper +
  `moveStep`/`waitStep`/`clickLeftStep`/`keyTapStep` builders (mirroring XiuluoTaskV2); `InputSequences`
  import/field/ctor removed (Lombok `@RequiredArgsConstructor`), unused `InputAction` import removed. Baseline
  coordinates/timing preserved: Alt+C dismount → KEY_TAP "Alt+C"+WAIT120; prepared-dialog → MOVE/WAIT80/CLICK150;
  tracker-green (×2) → MOVE/WAIT120/CLICK300. No second store/protocol/foreground fallback.
- `FiveRingTaskV2` / `XiuluoTaskV2`: Javadoc-only removal of retired `{@code InputSequences}` references.
- Source verification: a whole-tree `mvn -q compile` reports ZERO compile errors in the 3 write-set production files
  (type-clean). (Diagnostic run earlier while C was blocked; not re-run.)

### Test — all four migrated-caller proofs (test-only harness, real private callers via reflection, no production seam)
- `postAcceptAltCDismountIssuesTheExactBackgroundKeyTapTurnNotInputSequences` (startPostAcceptPrepath → KEY_TAP
  "Alt+C" + WAIT 120).
- `preparedGreenDialogClickIssuesTheExactMouseSequenceTurnNotInputSequences` (tryConsumePreparedWubeiDialog, published
  GREEN prepared slot → MOVE(150,250) / WAIT 80 / CLICK_LEFT(clickDelay 150)).
- `trackerGreenClickIssuesTheExactMouseSequenceTurnAtOneRandomizedPoint` (clickTaskTrackerGreen → MOVE / WAIT 120 /
  CLICK_LEFT(300); randomized point asserted as MOVE==CLICK same-point within the baseline radius).
- `chainedTrackerFastClickIssuesTheExactMouseSequenceTurnNotInputSequences` (clickCachedChainedTrackerGreen →
  MOVE(210,320) / WAIT 120 / CLICK_LEFT(300)).
- Harness: `RecordingCommandPort` (captures each issued turn; default-completes surrounding fire-and-forget
  local-service turns), `terminalReply`/`bound`/`invoke`/`assertPointStep`/`mouseSequenceAction` helpers, and
  default-off `recording()`/`botProperties()`/`dialogService()` options (the 12 existing tests are unchanged).
  `ScriptedDialogService extends DialogService` (super(null×10), overrides only `detectDialogTypeNoFocus`→NONE and
  `validatePreparedDialogActionForConsume`→pass-through) — a test-only double, NOT a production seam.

### Gate — NOT claimed passed
- CLOUD BUILD GATE remains **BLOCKED/PENDING BY OUT-OF-WRITE-SET IN-FLIGHT MIGRATION** (`NavigationService.java`
  [TURN-40B-C4, in progress], `InputActionScope.java`, `TextCandidateScanResult.java` — DHXY-absent types). The
  Cloud main does not compile, so `WubeiWholeTaskTurnContractTest` cannot be run and the test proofs are written but
  UNVERIFIED. No Maven run while C is active. To be compiled/run once C4/39C1 make the tree buildable.
- Zero Git mutation; no runtime/UI/capture/input; `D:\mavenProject\DHXY` untouched.

<!-- TRUE_EOF: TURN-39W CANONICAL SOURCE+TEST DELIVERED 4-FRESH-BLOBS SOURCE-TYPE-CLEAN ALL-4-CALLER-PROOFS-WRITTEN NO-PRODUCTION-SEAM CLOUD-BUILD-GATE-BLOCKED-PENDING-OUT-OF-WRITESET TESTS-UNVERIFIED-NOT-CLAIMED-PASSED 2026-07-19T03:13:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - BLOCKED / REPAIR REQUIRED - 2026-07-18T15:46:00-04:00

- verdict: `P0/P1/P2=0/2/2`; External A retains whole-card ownership. The four production caller migrations were
  compared method-by-method with baseline `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`: exact-HWND `Alt+C` plus
  WAIT120, prepared GREEN MOVE/WAIT80/CLICK150, and both tracker MOVE/WAIT120/CLICK300 sequences preserve the
  baseline order/timing. `InputSequences` production ownership is zero and no foreground-keyboard fallback,
  second protocol/store, stub or copied business algorithm was introduced. No approved business difference exists.
- **P1-1 - the new randomized tracker proof cannot complete with its current harness.**
  `WubeiWholeTaskTurnContractTest.java:412`
  `trackerGreenClickIssuesTheExactMouseSequenceTurnAtOneRandomizedPoint`
  uses `RecordingCommandPort`'s default `COMPLETED` terminal and a harness whose `TaskMaintenanceService` argument is
  null at test line 782. Production `WubeiTask.java:2822` `clickTaskTrackerGreen` therefore returns true from
  `executeInputTurn`, continues into the ordinary post-click branch and dereferences
  `taskMaintenanceService.openTeamPathingMaintenanceWindow(...)` at production line 2877. The test will throw NPE
  after recording the
  expected mouse action. Repair in the frozen test file only: either script a non-completed terminal and assert the
  caller returns false while preserving the same-point/WAIT120/CLICK300 action assertions, or provide complete safe
  test-only collaborators for the successful ordinary branch and assert true. Do not add a production seam.
- **P1-2 - terminal truth is not locked for all four callers.** The chained-tracker and prepared-GREEN tests invoke
  the real methods but discard their return values; mutating `executeInputTurn` to false would leave their step-only
  assertions green. Add explicit caller-result assertions: cached chained tracker is true only for `COMPLETED`,
  prepared GREEN maps `COMPLETED` to `GREEN_TEMPLATE_CLICKED`, and the ordinary tracker result matches the scripted
  terminal chosen for P1-1. Keep the existing Alt+C negative-terminal reachability proof and do not weaken the
  four-caller contract.
- **P2-1 - displaced production JavaDoc.** In `WubeiTask.java:2142-2202`, the prepath JavaDoc is stranded immediately
  before the newly inserted `executeInputTurn` JavaDoc, so `startPostAcceptPrepath` no longer owns its method contract. Place each JavaDoc
  directly above the method it documents; behavior remains frozen.
- **P2-2 - stale test documentation.** `WubeiWholeTaskTurnContractTest.java:110` says the named test compiles with no
  shared debt although this delivery declares the Cloud build blocked/unverified; line 658 also names a stale numeric constructor arity.
  Make both descriptions factual or remove the brittle arity claim. Test behavior and write set remain frozen.
- verification gate: after repair, re-deliver fresh blobs and the exact caller-result evidence. Maven remains
  `BLOCKED/PENDING` while External C is an active Cloud Java writer; no test/build pass is inferred from this review.

<!-- TRUE_EOF: TURN-39W PARENT-SOURCE+TEST-REVIEW1 BLOCKED-REPAIR-REQUIRED P0=0-P1=2-P2=2 OWNER-A-RETAINED TRACKER-HARNESS-NPE TERMINAL-TRUTH-MISSING JAVADOC+TEST-DOC-DRIFT PRODUCTION-SEQUENCES-BASELINE-EQUIVALENT NO-MAVEN 2026-07-18T15:46:00-04:00 -->

## PARENT COMMUNICATION STATUS - 2026-07-18T15:55:00-04:00

- Two parent audit rounds have elapsed without a STATUS EVENT ACK for
  `PARENT-A-TURN39W-REVIEW1-REPAIR-1546`; communication is now `COMMUNICATION_STALE`.
- Fresh Wubei production/test mtimes at 15:53/15:54 prove repair activity, so A remains OWNER / REPAIR ACTIVE and is
  not ACTIVE_STALE. These are WIP bytes, not a canonical re-delivery and not reviewed early.
- Review #1 `P0/P1/P2=0/2/2` and its frozen repair conditions remain authoritative. No Maven while A/C write.

<!-- TRUE_EOF: TURN-39W PARENT-COMMUNICATION-STATUS OWNER-A REPAIR-ACTIVE COMMUNICATION-STALE NOT-ACTIVE-STALE REVIEW1-ACK-MISSING-R2 WIP-NOT-REDELIVERED NO-MAVEN PHYSICAL-EOF 2026-07-18T15:55:00-04:00 -->

## CANONICAL SOURCE+TEST RE-DELIVERED (REVIEW #1 REPAIR) - EXTERNAL-A - 2026-07-19T03:28:00-04:00

Closes all Review #1 findings (P0/P1/P2 = 0/2/2 → repaired). Test-only + JavaDoc changes; production sequences stay
baseline-equivalent (Review #1 confirmed). No production seam; build gate stays BLOCKED/PENDING; no Maven while C active.

### Fresh blobs (git hash-object)
1. `19fd91074c44334025f5afed664506cbff9a23cc`  WubeiTask.java (4504L) — SOURCE (P2-1 JavaDoc adjacency only)
2. `49526761df325ab913993931dee725b93f76fe33`  FiveRingTaskV2.java (3043L) — unchanged this round
3. `91c2572fb5db42be6928d7473ade4cd327942ea2`  XiuluoTaskV2.java (4554L) — unchanged this round
4. `db1703730ab1f94669f5a602e15282c6ba2d7c5c`  WubeiWholeTaskTurnContractTest.java (812L) — TEST (P1-1/P1-2/P2-2)

### Repairs
- P1-1 (tracker harness NPE): `trackerGreenClickIssuesTheExactMouseSequenceTurnAtOneRandomizedPoint` now scripts a
  non-completed (FAILED) terminal so `clickTaskTrackerGreen` returns false and never reaches the ordinary post-click
  `taskMaintenanceService.openTeamPathingMaintenanceWindow` branch (null collaborator). Same-point MOVE/CLICK +
  WAIT120/CLICK300 assertions retained; caller result asserted false. No production seam.
- P1-2 (terminal truth): explicit caller-result assertions added — cached chained tracker true on COMPLETED; prepared
  GREEN maps COMPLETED → `GREEN_TEMPLATE_CLICKED`; ordinary tracker false on the P1-1 non-completed terminal. Alt+C
  negative-terminal reachability proof kept. COMPLETED-only truth is now observable at caller level for all four.
- P2-1 (displaced JavaDoc): the `executeInputTurn` helper block was moved above the prepath JavaDoc, so
  `startPostAcceptPrepath` again owns its method contract and `executeInputTurn` owns its own. Behavior frozen.
- P2-2 (stale test doc): the header no longer claims "compiles with no shared debt" (now states the Cloud module
  does not yet compile due to out-of-write-set migration; test written-but-unrun, Maven gate BLOCKED/PENDING); the
  harness JavaDoc drops the stale "24-argument" arity for the retired-InputSequences `@RequiredArgsConstructor` note.

### Gate — NOT claimed passed
- CLOUD BUILD GATE remains BLOCKED/PENDING BY OUT-OF-WRITE-SET IN-FLIGHT MIGRATION (NavigationService [C4]/
  InputActionScope/TextCandidateScanResult). Cloud main un-buildable → `WubeiWholeTaskTurnContractTest` UNVERIFIED
  (written, not compiled/run). No Maven while C active. Zero Git mutation; DHXY untouched.

<!-- TRUE_EOF: TURN-39W CANONICAL SOURCE+TEST RE-DELIVERED REVIEW1-ALL-FINDINGS-CLOSED P1-1-TRACKER-FALSE P1-2-TERMINAL-TRUTH P2-1-JAVADOC P2-2-TESTDOC 4-FRESH-BLOBS NO-SEAM BUILD-GATE-BLOCKED TESTS-UNVERIFIED 2026-07-19T03:28:00-04:00 -->

## PARENT SOURCE+TEST SOURCE REVIEW #2 - PASSED / OWNER RELEASED - 2026-07-18T16:01:00-04:00

- verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`. Fresh blobs match the canonical re-delivery:
  `WubeiTask=19fd9107`, `FiveRingTaskV2=49526761`, `XiuluoTaskV2=91c2572f`,
  `WubeiWholeTaskTurnContractTest=db170373`.
- Review #1 P1-1 is closed at test lines 417-445: the ordinary tracker test scripts a FAILED outcome, asserts false,
  never reaches the intentionally null maintenance collaborator, and retains same-point MOVE/CLICK plus exact
  WAIT120/CLICK300.
- Review #1 P1-2 is closed at test lines 395-471: chained tracker asserts COMPLETED -> true, prepared GREEN asserts
  COMPLETED -> `GREEN_TEMPLATE_CLICKED`, ordinary tracker asserts FAILED -> false, and Alt+C keeps its negative
  terminal reachability proof. All four caller sequences and terminal mappings are observable without a production
  seam.
- Review #1 P2-1/P2-2 are closed: `WubeiTask.java:2142-2202` gives `executeInputTurn` and
  `startPostAcceptPrepath` adjacent correct JavaDocs; test lines 106-114 accurately mark the build/test gate
  unverified, and lines 668-673 contain no stale numeric constructor arity.
- production remains baseline-equivalent to `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`: exact-HWND keyboard is
  background and cross-window parallel, only mouse is foreground/global-serial, and the four order/timing sequences
  are unchanged. `InputSequences` appears only in explanatory references/test names, not as production ownership.
- owner disposition: External A source owner is released. Build/named-test status remains `BLOCKED/PENDING` because
  External C is actively editing Cloud tests and the shared Cloud tree is not yet a stable compile window. No Maven
  was run and no test pass is inferred. TURN-39C1 remains NOT READY until C4 source gate is passed/active-zero.

<!-- TRUE_EOF: TURN-39W PARENT-SOURCE+TEST-REVIEW2 PASSED P0=0-P1=0-P2=0 OWNER-RELEASED FOUR-BLOBS-VERIFIED REVIEW1-ALL-CLOSED BASELINE-EQUIVALENT BUILD+NAMED-TEST-BLOCKED-PENDING NO-MAVEN 39C1-NOT-READY PHYSICAL-EOF 2026-07-18T16:01:00-04:00 -->
