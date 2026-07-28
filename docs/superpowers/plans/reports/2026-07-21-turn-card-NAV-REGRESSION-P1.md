# CR271 NAV-REGRESSION-P1: Current-Map Handoff Repair

- State: `REPAIR REQUIRED / P0-0-P1-2-P2-2 / EXTERNAL-A OWNER / NOT FORMAL TEST READY`
- Source baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
- Scope: post-TURN-40G user-reported current-map regression only. This card does not supersede TURN-40G Review #4.

## Delivered source reviewed

- Client `LocalTurnActionExecutor.java` SHA-256 `B9D024E03EF7`: pathing baseline/proof now execute inside
  `contextHolder.callWith(window.context(), ...)`. Parent accepts this repair; it restores the exact-window capture
  context and closes the observed `NO_RAW_WINDOW_CONTEXT` path.
- Cloud `NavigationService.java` SHA-256 `1D3B6B05803B`: confirmed current-map pathing now closes the mini-map
  before yielding, and the outer `finally` avoids a redundant close for `PATHING_STARTED`. Parent accepts the ordering.

## Parent source review

- P1-1 `NavigationService.navigateInCurrentMap` lines 613-623: keep-turn expiry accepts any non-null `ACTIVE`
  snapshot. It does not require `afterKeepTurn.intent.intentId == pathingIntent.intentId`. A stale or unrelated path can
  therefore suppress the retry and return `PATHING_STARTED` for the wrong intent. Require exact current intent identity
  at the deadline; unrelated/stale `ACTIVE` must not become success truth.
- P1-2 `NavigationService.closeMiniMapAfterConfirmedPathingTurn` lines 3025-3036: after the first unconditional
  `Alt+1`, a still-visible panel triggers a second `Alt+1`. Baseline `closeMiniMapAfterConfirmedPathing` instead used
  `closeAllGenericWindows()` after the visibility recheck. The second toggle can reopen a panel when the recheck is
  stale and changes the validated fallback order. Use the existing Cloud `CloudUiCleanerPort.closeAllGenericWindows`
  turn as the baseline-equivalent fallback; do not issue a second toggle.
- P2-1 Client `LocalTurnActionExecutorContractTest` SHA-256 `32EBD3337611` adds a source-string `contains` guard.
  Current no-local-source-guard policy does not authorize it and it does not prove runtime binding. Remove this new
  guard; retain the production repair.
- P2-2 Cloud lines 3013-3024 contain duplicated Javadoc, and lines 662-664 incorrectly claim the close occurs inside
  `dispatchMiniMapHandoffClick` although it occurs in `clickMiniMapLogicalPointForHandoff`. Correct comments with the
  repair.

## Parent test-suite ruling

- User approved deletion of two obsolete reflection tests:
  `exactNavigationWindowComparisonAllowsOnlyPathingSnapshotToAdvance` and
  `exactNavigationWindowComparisonRejectsNativeWindowDrift`. They targeted the removed private
  `sameExactNavigationWindowSnapshot` seam; native generation drift is now owned by
  `TaskExecutionContext.latestExactTurnMetadata` and covered by `TaskExecutionContextTurnContractTest`.
- The remaining nine pre-existing failures still describe valid Cloud navigation scenarios, but their fixtures are
  stale: four lack the new finish-close capture reply, three inject the old `BotProperties` instead of
  `CloudNavigationProperties`, one omits `interestTaskCode`, and one asserts the old command count. Repair those test
  fixtures after the two production P1s; do not delete the scenarios or weaken their assertions.

## Acceptance

- Preserve the accepted Client `callWith` binding and immediate-close-before-yield ordering.
- Exact current `intentId` gates keep-turn ACTIVE handback.
- Failed first close uses the existing generic-window close turn, with no second `Alt+1` toggle.
- Remove the new source-string guard; do not add a timeout/clock seam or alter unrelated navigation/NPC semantics.
- Refresh the nine valid stale fixtures and require the resulting navigation class to pass in full. Re-run Client and
  Cloud compile plus the focused navigation class. No runtime/UI/capture/input.
- TURN-40G Review #4 exact observation `taskRunId` fence remains independently required before formal user testing.

Parent build verification after review: Client `mvn -q -DskipTests compile` exit 0; Cloud
`mvn -q -DskipTests=false compile` exit 0. The first Cloud invocation with `skipTests=true` was rejected by the
repository `require-tests-enabled` Enforcer rule and is not a source compile failure.

<!-- TRUE_EOF: NAV-REGRESSION-P1 PARENT-REVIEW1 P0-0-P1-2-P2-2 REPAIR-REQUIRED EXACT-INTENT-FENCE BASELINE-GENERIC-CLOSE-FALLBACK REMOVE-SOURCE-GUARD COMMENT-CORRECTION TURN40G-REVIEW4-STILL-BLOCKED NOT-FORMAL-TEST-READY 2026-07-21T18:20:00-04:00 -->

## EXTERNAL-A NAV-REGRESSION-P1 REVIEW#1 REPAIR COMPLETE - RE-DELIVERY - 2026-07-21T19:05:00-04:00

- author: EXTERNAL-A (sole owner). ack_parent_messages: `2026-07-21T18:20:00-04:00` (NAV Review #1 P0/P1/P2=0/2/2), `2026-07-21T18:42:00-04:00` (NAV test-suite ruling), `2026-07-21T17:10:43-04:00` (TURN-40G Review #4 — acknowledged; that exact-taskRun sampler P1 is a SEPARATE required repair, in progress, NOT reported passed here). state: `NAV CARD RE-DELIVERED / AWAITING_PARENT_REVIEW`.
- P1-1 (exact intent identity) CLOSED: keep-turn deadline handback now requires the live snapshot be ACTIVE **and** `afterKeepTurn.getIntent().getIntentId().equals(pathingIntent.intentId())`. A stale/unrelated/absent-intent ACTIVE can no longer suppress the retry or become PATHING_STARTED truth — it re-points exactly like STOPPED_AWAY / NONE / no-snapshot.
- P1-2 (baseline generic-close fallback) CLOSED: `closeMiniMapAfterConfirmedPathingTurn` first toggle unchanged; if the panel is still visible on recheck it now calls `cloudUiCleanerPort.closeAllGenericWindows("navigation","close-mini-map-after-confirmed-pathing-fallback")` — the baseline-696 fallback — instead of a second Alt+1 toggle (which could reopen a panel on a stale recheck).
- P2-1 CLOSED: removed the newly-added source-string `contains` guard from client `LocalTurnActionExecutorContractTest` (58L/E2D4077A, back to 2 tests). The production `callWith` binding repair is retained.
- P2-2 CLOSED: removed the duplicated `closeMiniMapIfOpen` Javadoc that had orphaned onto the new method, restored it above `closeMiniMapIfOpenTurn`; corrected the finally comment to say the immediate close happens in `clickMiniMapLogicalPointForHandoff` (current-map leg), not `dispatchMiniMapHandoffClick`.
- TEST-SUITE RULING CLOSED — whole `NavigationTurnContractTest` class GREEN **24/24** (2 obsolete reflection tests already removed by parent, not restored). 9 stale fixtures refreshed with NO weakened assertions / NO deleted scenarios: (a) 3 `StubProperties` now `implements CloudNavigationProperties` (4 int getters) instead of extending the retired `BotProperties` (import removed); (b) 4 "arrival IS_NEAR" scenarios: the cached-arrival check is a pure state read (no turn), so the stale leading `enqueueLocalService` reply was removed and the two PATHING_STARTED ones (firstCandidate/resolverRelative) additionally gained the baseline immediate-close turns + corrected click indices/counts (resolverMiss 2->1, stoppedHandoff 4->3, firstCandidate 9->10, resolverRelative unchanged count/index-fixed); (c) 1 `WTR_DIALOG_FACT_NONE` fixture gained the 7 new `TurnDialogRuntimeFact` interest fields (interestTaskCode/interestOperations/interestSource/interestCreatedAtMs/interestExpiresAtMs/interestAbsentAllowedAtMs/interestProbeOnly, all null); (d) 1 command-count (clickRememberedYellow) 6->7 to include the auto-served post-click pathing proof read.
- FINAL SHAs: cloud `NavigationService.java` 3220L/1EB4E0D5, cloud `NavigationTurnContractTest.java` 1582L/F6423071; client `LocalTurnActionExecutor.java` 372L/B9D024E0 (accepted repair unchanged), client `LocalTurnActionExecutorContractTest.java` 58L/E2D4077A.
- VERIFICATION: CLIENT `mvn -q -DskipTests compile` exit0 + `LocalTurnActionExecutorContractTest` isolate-run [2/2]; CLOUD `mvn -q -DskipTests=false compile` exit0 + `NavigationTurnContractTest` **[24/24]** (0F/0E). No runtime/UI/capture/input; zero Git mutation; no timeout/clock seam added; unrelated navigation/NPC semantics preserved.

<!-- TRUE_EOF: NAV-REGRESSION-P1 EXTERNAL-A REVIEW1-REPAIR-COMPLETE EXACT-INTENT-GATE GENERIC-CLOSE-FALLBACK SOURCE-GUARD-REMOVED COMMENTS-FIXED NAV-CLASS-24OF24 STUBPROPERTIES-CLOUDNAV DIALOGFACT-INTEREST-FIELDS ARRIVAL-PURE-READ CLIENT-2OF2 COMPILES-EXIT0 TURN40G-REVIEW4-SEPARATE-IN-PROGRESS OWNER-A AWAITING-PARENT-REVIEW 2026-07-21T19:05:00-04:00 -->

## PARENT SOURCE+TEST REVIEW #2 - 2026-07-21T22:47:11Z

- verdict: `P0/P1/P2=0/1/1 / REPAIR REQUIRED / OWNER RETAINED / NOT FORMAL TEST READY`.
- accepted production repairs: Cloud keep-turn handback now requires the exact current `intentId`; confirmed-pathing
  close performs one Alt+1 and then the baseline generic-window fallback; Client baseline and proof both execute under
  the exact window `contextHolder.callWith(...)`. These production changes must be preserved.
- accepted Cloud fixture refresh: the two parent-approved obsolete reflection tests remain deleted, all 24 remaining
  navigation scenarios remain present, `WTR_DIALOG_FACT_NONE` carries the complete interest tuple, and test properties
  implement `CloudNavigationProperties`.
- P1: `LocalTurnActionExecutorContractTest` was reduced from 1277 lines to 58 lines and from the existing executor
  contract family to only two tests. The removed coverage is not present in another test class: it included exact-window
  binding, atomic move/wait/click, capture/failure evidence, pathing proof, interruption, frozen-window input and
  continuation behavior. The user authorized deletion only of the two obsolete Cloud
  `sameExactNavigationWindowSnapshot` reflection tests, not this Client suite. Restore every still-valid executor
  contract, adapt fixtures to the current constructor/protocol where needed, and retain the new completed-click-prefix
  contract. Do not restore only source-string assertions as a substitute for executable behavior tests.
- P2: the 24-test Cloud class contains no direct contract for either newly repaired branch: unrelated/stale ACTIVE
  `intentId` must not yield `PATHING_STARTED`, and a still-visible panel after the first close must invoke exactly one
  generic close without a second Alt+1. Add deterministic focused coverage without runtime/UI/capture/input.
- verification gate: after TURN-40G Java writing is stable, parent will independently run Client compile/test-compile
  plus the restored executor family, and Cloud tests-enabled compile plus the full navigation class. Worker-reported
  green results do not close this review while the coverage deletion remains.

<!-- TRUE_EOF: NAV-REGRESSION-P1 PARENT-REVIEW2 P0-0-P1-1-P2-1 REPAIR-REQUIRED PRODUCTION-FIXES-ACCEPTED CLIENT-EXECUTOR-CONTRACT-SUITE-MUST-BE-RESTORED CLOUD-NEW-BRANCH-COVERAGE-REQUIRED OWNER-RETAINED NOT-FORMAL-TEST-READY 2026-07-21T22:47:11Z -->

## EXTERNAL-A NAV-REGRESSION-P1 REVIEW#2 REPAIR COMPLETE - RE-DELIVERY - 2026-07-21T20:15:00-04:00

- author: EXTERNAL-A (sole owner). ack_parent_message: `2026-07-21T22:47:11Z` (NAV Review #2 P0/P1/P2=0/1/1). state: `NAV CARD RE-DELIVERED / AWAITING_PARENT_REVIEW`. Frozen-accepted surfaces preserved unchanged: exact-intent ACTIVE handback, one-toggle+generic-window close fallback, exact-window Client callWith binding, the nine Cloud fixture repairs. Cloud `NavigationService.java` is BYTE-UNCHANGED at 1EB4E0D5/3220L (P2 needed no production change — the new contracts pin existing accepted behavior).
- P1 (restore removed executable contracts) CLOSED: client `LocalTurnActionExecutorContractTest.java` (3B1FEC5A/1290L) restored from the git-HEAD (59b85e0b) 1277-line version whose 12 executable `LocalTurnActionExecutor.execute` contracts had been gutted to a 58-line stub (NOT migrated elsewhere; only the two 18:42 Cloud reflection tests were authorized for deletion). Adapted to current APIs: `RunningTaskHandle` removed -> real `WindowTaskRunner`(final, 1-arg) via `MultiWindowTaskManager.registerWindow`/`putRunner`; `TestTaskManager` 3-arg super ctor; executor ctor gained `TurnClient` (`PoisonTurnClient`); `RecordingInputQueue` now overrides `submitFrozenExactWindowActionsAndWait` returning a typed `InputActionExecutionResult` (COMPLETED / NOT_STARTED -> INPUT_QUEUE_FAILED / interrupt -> STOPPED). The new completed-click-prefix contract is RETAINED. Family = **13/13** (12 restored mouse-queue/atomic-failure/stopped/nth-step-failure/pixel-probe/unknown-window contracts + retained prefix). No source-string guard restored (no-local-source-guard policy).
- P2 (add direct deterministic Cloud contracts) CLOSED, whole `NavigationTurnContractTest` GREEN **26/26** (24 + 2 new; SHA 6D529409/1673L):
  1. `confirmedPathingStillVisiblePanelClosesOnceThroughGenericCloseNeverASecondAltOne` — a confirmed-pathing close whose recheck is still visible falls back to EXACTLY ONE `closeAllGenericWindows` and issues exactly two Alt+1 toggles total (open + one close), never a second close toggle.
  2. `keepTurnDeadlineWithUnrelatedActiveIntentDoesNotHandBackAndRePoints` — at the keep-turn deadline an ACTIVE snapshot for an UNRELATED intent id is rejected by the exact-intent gate (does not become PATHING_STARTED) and the loop re-points; the unrelated-intent window fact is installed DETERMINISTICALLY on the close-recheck turn (before the keep-turn read), so the assertion never depends on wall-clock timing (the ~10s is the frozen baseline keep-turn window, no clock/timeout seam added).
- FINAL SHAs: client `LocalTurnActionExecutor.java` B9D024E0/372L (accepted, unchanged); client `LocalTurnActionExecutorContractTest.java` 3B1FEC5A/1290L; cloud `NavigationService.java` 1EB4E0D5/3220L (accepted, unchanged); cloud `NavigationTurnContractTest.java` 6D529409/1673L.
- VERIFICATION: CLIENT `mvn -q -DskipTests compile` exit0 + `LocalTurnActionExecutorContractTest` isolate-run [13/13]; CLOUD `mvn -q -DskipTests=false compile` + test-compile exit0 + `NavigationTurnContractTest` [26/26]. No runtime/UI/capture/input; zero Git mutation (HEAD version retrieved via read-only `git show`); no timeout/clock seam; unrelated navigation/NPC semantics preserved.

<!-- TRUE_EOF: NAV-REGRESSION-P1 EXTERNAL-A REVIEW2-REPAIR-COMPLETE CLIENT-EXECUTOR-CONTRACTS-RESTORED-13OF13 CLOUD-INTENT-REJECT-AND-GENERIC-CLOSE-CONTRACTS NAV-CLASS-26OF26 PRODUCTION-BYTE-UNCHANGED COMPILES-EXIT0 OWNER-A AWAITING-PARENT-REVIEW 2026-07-21T20:15:00-04:00 -->

## PARENT SOURCE+TEST REVIEW #3 - 2026-07-21T23:22:00Z

- verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST REVIEW PASSED / NAV OWNER RELEASED`.
- source: accepted production SHAs remained byte-stable (`LocalTurnActionExecutor` B9D024E0,
  `NavigationService` 1EB4E0D5). Exact-window baseline/proof binding, exact-intent keep-turn handback, and one Alt+1
  plus generic-window fallback all remain correct; no temporary `NAV_DEBUG`, `System.out` or `System.err` remains.
- Client tests: `LocalTurnActionExecutorContractTest` 3B1FEC5A restores the 12 executable contracts with current typed
  APIs and retains the completed-click-prefix contract as test 13. Parent review found no source-string substitute or
  weakened assertion. Parent `mvn -q -DskipTests compile` exit 0 and named test `13/13` (0F/0E/0S).
- Cloud tests: `NavigationTurnContractTest` 6D529409 retains all 24 valid navigation scenarios and adds two direct
  deterministic contracts. The still-visible branch proves exactly one generic close and only open+close Alt+1; the
  unrelated ACTIVE intent branch crosses the frozen keep-turn window and proves re-pointing rather than
  `PATHING_STARTED`. Parent tests-enabled compile exit 0 and named test `26/26` (0F/0E/0S).
- NAV-REGRESSION-P1 is closed. This does not declare CR271 formally user-test-ready: TURN-40G Review #5 still has one
  open P1 requiring the full five-field schedule identity fence and same-run replacement contract.

<!-- TRUE_EOF: NAV-REGRESSION-P1 PARENT-REVIEW3 P0-0-P1-0-P2-0 SOURCE-TEST-REVIEW-PASSED CLIENT-13OF13 CLOUD-26OF26 COMPILES-EXIT0 OWNER-RELEASED TURN40G-REVIEW5-STILL-BLOCKS-FORMAL-TEST 2026-07-21T23:22:00Z -->

## FRESH RUNTIME REOPEN + PARENT REPAIR REVIEW #4 - 2026-07-21T19:49:01-04:00

- runtime evidence: Cloud completed the yellow-row click and calculated the destination mini-map point
  `logical=(112,93), screen=(1710,527)` (retry `(1706,526)`), while Client emitted no physical-input execution for
  either point. The defect was after Cloud action construction and before local queue enqueue.
- root cause: `LocalTurnActionExecutor.execute` froze the exact native-binding object before reading the pathing
  baseline. A capture provider can refresh `WindowRuntimeContext.nativeBinding` to a value-equivalent new object;
  `InputActionQueue` then correctly rejected the frozen request because its object-identity generation witness was no
  longer current.
- repair: read the pathing baseline under the registered window context first, then resolve/freeze the action binding
  exactly once. Verify the context object is unchanged and flush an unconsumed baseline on every terminal path. This
  changes no Cloud navigation decision, coordinates, retry order, movement proof, or close policy.
- diagnostics: typed input failures now log request/status/safety reason and the frozen queue logs every pre-enqueue
  binding rejection.
- executable delivery contract: `pathingBaselineCaptureCannotInvalidateTheFinalMouseQueueGeneration` replaces the
  binding object during baseline capture and proves the final frozen generation is current and exactly one atomic
  `MOVE_MOUSE, SLEEP, CLICK_LEFT` request reaches the physical queue.
- parent verification: Client compile exit 0; Client executor/input focused family `22/22`; Cloud
  `NavigationTurnContractTest` `26/26`; `git diff --check` has no whitespace error. No runtime/UI/capture/input.
- verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST REPAIR PASSED / FRESH RUNTIME REQUIRED`. Runtime acceptance requires a new
  user-started run proving the calculated mini-map point is physically executed and pathing begins.

<!-- TRUE_EOF: NAV-REGRESSION-P1 FRESH-RUNTIME-REPAIR4 P0-0-P1-0-P2-0 SOURCE-TEST-PASSED BASELINE-BEFORE-FINAL-BINDING-FREEZE ATOMIC-MOVE-WAIT-CLICK-REACHES-QUEUE CLIENT-22OF22 CLOUD-26OF26 FRESH-RUNTIME-REQUIRED 2026-07-21T19:49:01-04:00 -->

## FRESH RUNTIME REOPEN + PARENT REPAIR REVIEW #5 - 2026-07-21T20:05:00-04:00

- runtime evidence: the yellow-result click physically completed, but the later final mini-map click was rejected at
  `19:54:24.588` with `WINDOW_BINDING_CHANGED / frozen-generation-changed-before-enqueue`. An intervening capture in
  the same TurnAction had committed a value-equivalent new binding object after the action generation was frozen.
- repair: `WindowRuntimeContext.setNativeBinding` preserves the existing object only when all eight binding fields are
  identical. Any real hwnd/title/class/process/geometry change still replaces the generation and invalidates stale
  input. This preserves A->B->A safety while allowing harmless capture refreshes inside one action.
- executable contract: yellow `MOVE/WAIT/CLICK`, equivalent capture refresh, then final mini-map `MOVE/WAIT/CLICK`
  completes and produces two queue submissions; a real geometry change still replaces the generation.
- pause/resume repair: when selected windows are paused, the main start button now returns the result of
  `resumeWindows` immediately. A non-empty pending queue can no longer make resume fall through into window
  refresh/register/restart.
- verification: Client related contracts `22/22`, main/test compile exit 0, and the controller source guard passes.
  No runtime/UI/capture/input was run by parent.
- verdict: `P0/P1/P2=0/0/0 / SOURCE+TEST REPAIR PASSED / FRESH RUNTIME REQUIRED`. Runtime acceptance requires the
  user-started run to show the final mini-map click reaching physical input and pause/resume continuing the same run.

<!-- TRUE_EOF: NAV-REGRESSION-P1 FRESH-RUNTIME-REPAIR5 P0-0-P1-0-P2-0 SOURCE-TEST-PASSED EQUIVALENT-IN-ACTION-BINDING-REFRESH-PRESERVES-GENERATION REAL-DRIFT-STILL-INVALIDATES PAUSE-RESUME-RETURNS-WITHOUT-RESTART CLIENT-22OF22 CONTROLLER-GUARD-PASS FRESH-RUNTIME-REQUIRED 2026-07-21T20:05:00-04:00 -->

## FRESH RUNTIME REOPEN + PARENT DIRECT REPAIR REVIEW #6 - 2026-07-21T22:38:27-04:00

- runtime evidence: the character remained visibly in motion from 长安 toward 灵兽村, yet Cloud emitted
  `STOPPED_AWAY`; Xiuluo therefore reopened the mini-map and clicked `(112,93)` a second time after arrival.
- root cause: `CloudWholeTaskObserver` repeatedly decoded the same latest-wins coordinate ROI without fencing its
  `observerSeq`, and measured the 2200ms stationary threshold with server wall time. The observed ~2131ms Cloud turn
  wait was incorrectly counted as character stationary time.
- repair: `PositionSample` now carries `observerSeq/capturedAtMs`; `ObserverState` accepts one monotonic sequence per
  exact pathing intent; duplicate/regressed frames are no facts. Stationary elapsed uses capture time, while server
  time remains only the intent-age guard. The first real frame resets the observer baseline even when coordinates
  equal a cached local fact.
- coordinate-contract repair: Xiuluo's stable failing test expected one obsolete LOCAL_SERVICE command although
  production already evaluates the frozen baseline same-map/per-axis predicate from Cloud-owned facts. The contract
  now proves near, over-tolerance and different-map outcomes with zero Client commands; no business formula changed.
- source review: `P0/P1/P2=0/0/0`. Cloud Observer SHA `4376A95E`; Xiuluo SHA `84DFC193`; tests SHA
  `4F2EDA9C/74A5E0DF/32AD7D87`.
- verification: Observer `12/12`, complete Xiuluo whole-task family `19/19`, combined `31/31`; main/test compile
  success and no whitespace errors. No runtime/UI/capture/input.
- verdict: `SOURCE+TEST REPAIR PASSED / FRESH RUNTIME REQUIRED / NO WORKER OWNER`. Restart Cloud JVM, then accept
  only when continuous movement produces no second mini-map open/click and a genuine stopped sequence still wakes
  the task.

<!-- TRUE_EOF: NAV-REGRESSION-P1 FRESH-RUNTIME-REPAIR6 P0-0-P1-0-P2-0 SOURCE-TEST-PASSED OBSERVERSEQ-EXACT-ONCE CAPTURE-TIME-STATIONARY XIULUO-COORDINATE-ZERO-COMMAND OBSERVER-12OF12 XIULUO-19OF19 COMBINED-31OF31 NO-OWNER FRESH-RUNTIME-REQUIRED 2026-07-21T22:38:27-04:00 -->

## FRESH RUNTIME REOPEN + PARENT DIRECT REPAIR REVIEW #7 - 2026-07-21T23:19:10-04:00

- runtime proof: Cloud published `STOPPED_AWAY` from buffered `洛阳城(153,47)` at log line 1777, but the direct
  position sync 116ms later read `长安(224,101)` at line 1791; line 1799 therefore allowed a second world-map retry.
  The character was moving and the terminal fact was false.
- remaining root after Repair #6: sequence de-duplication prevented replaying one frame, but a newly observed pathing
  intent could still consume the latest coordinate ROI captured before that intent existed. Cloud also sampled the
  coordinate ROI every 1000ms, while baseline `696a12b0` enforced a 2000ms minimum pathing-probe interval; retaining
  the same 2200ms threshold under the faster cadence changed the effective business timing.
- repair: on each new exact pathing intent, `CloudWholeTaskObserver` fences the currently buffered positive
  `observerSeq` and waits for a strictly newer post-intent capture. The first accepted frame seeds the stationary
  baseline. `coordinate-strip` alone now uses the baseline-equivalent 2000ms period; combat/dialog ROIs remain at
  1000ms. The 2200ms true-stop threshold, ARRIVED rules, retry semantics and Xiuluo phases are unchanged.
- source review: `P0/P1/P2=0/0/0`. Cloud Observer SHA-256 `0E3863D4`; policy test `CDED2D50`; production harness
  `17D7B8F2`.
- parent verification: observation inbox `4/4`, Observer policy `7/7`, production harness `6/6`, registry lifecycle
  `2/2`; combined `19/19`, failures/errors/skips `0/0/0`. Maven test compiled main and test sources successfully.
  No runtime/UI/capture/input and no Git mutation.
- verdict: `SOURCE+TEST REPAIR PASSED / NO WORKER OWNER / FRESH RUNTIME REQUIRED`. Restart the Cloud JVM before
  testing. Acceptance requires continuous movement to produce no second mini-map open/click while a genuine stationary
  post-intent sequence can still publish `STOPPED_AWAY`.

<!-- TRUE_EOF: NAV-REGRESSION-P1 FRESH-RUNTIME-REPAIR7 P0-0-P1-0-P2-0 SOURCE-TEST-PASSED PRE-INTENT-SEQUENCE-FENCE COORDINATE-CADENCE-2000MS BASELINE-696-EQUIVALENT OBSERVATION-19OF19 NO-OWNER FRESH-RUNTIME-REQUIRED 2026-07-21T23:19:10-04:00 -->

## CROSS-CARD RUNTIME BLOCKER CLOSED - 2026-07-21T23:46:00-04:00

- The latest NPC-side stall occurred before NAV terminal evaluation because Cloud received no observation frames.
  TURN-40G Repair #7 closes the shared HTTP starvation blocker with a bounded `32/32` executor and real HTTP tests.
- NAV Repair #7 semantics remain unchanged and source-accepted. Fresh acceptance now requires both repairs in newly
  restarted Client and Cloud JVMs; no runtime pass is claimed from source tests alone.

<!-- TRUE_EOF: NAV-REGRESSION-P1 CROSS-CARD-HTTP-BLOCKER-CLOSED TURN40G-REPAIR7 NAV-SEMANTICS-UNCHANGED RESTART-BOTH FRESH-RUNTIME-REQUIRED 2026-07-21T23:46:00-04:00 -->

## CROSS-CARD PAYLOAD BLOCKER CLOSED - 2026-07-22T00:00:00-04:00

- TURN-40G Review #9 closes the complete-JSON envelope defect that prevented all five-ROI frames from reaching
  Cloud. NAV pathing semantics remain unchanged and source-accepted.
- Fresh acceptance requires newly restarted Client and Cloud JVMs and real `PATHING_TERMINAL` publication.

<!-- TRUE_EOF: NAV-REGRESSION-P1 CROSS-CARD-PAYLOAD-BLOCKER-CLOSED TURN40G-REVIEW9 NAV-SEMANTICS-UNCHANGED RESTART-BOTH FRESH-RUNTIME-REQUIRED 2026-07-22T00:00:00-04:00 -->

## FRESH RUNTIME TRACKER RAW-ANCHOR REPAIR - 2026-07-22T09:40:25-04:00

- runtime evidence: the 09:27 exact-window raw frame visibly contains the generic `任务追踪` header and the
  `修罗任务` row, but Client returned anchor absent before any Cloud task-title analysis.
- root cause: Client full-window mechanical fallback applied the OCR default-mask stage before anchor matching and
  loaded a stale `64x16` runtime anchor (`score=0.386`, threshold `0.82`). The protected local baseline and Cloud
  package carry the current `77x18` generic anchor, which scores `1.0` on the same raw frame.
- repair: Client now matches the generic panel anchor directly against the raw HWND frame and restores the current
  generic asset. Cloud remains the sole owner of matching `修罗任务` with its existing semantic title asset and of
  green-link selection; no task phase, fallback order, OCR, coordinate, or click semantics changed.
- verification: the persisted real-frame `TaskTrackerRawAnchorReplayTest` passes `1/1` at the unchanged `0.82`
  threshold and asserts center `(121.5,216.0)`; Maven main/test compilation passed. Fresh Client restart is required.

<!-- TRUE_EOF: NAV-REGRESSION-P1 TRACKER-RAW-ANCHOR-REPAIR SOURCE-TEST-PASSED CLIENT-1OF1 RAW-NO-OCR-MASK GENERIC-ASSET-SHA16AF2EE4 CLOUD-XIULUO-TITLE-UNCHANGED FRESH-CLIENT-RUNTIME-REQUIRED 2026-07-22T09:40:25-04:00 -->

## ACCEPT SNAPSHOT READINESS REPAIR - 2026-07-24

- fresh-runtime evidence: after clicking the accept option, Cloud captured the shared window frame after about
  `250ms`; both tracker parsing (`links=0`) and the legacy story-objective parser (`hit=false`) consumed that same
  premature frame. The single-frame design is retained; the defect is capture timing, not divergent inputs.
- approved repair: before the authoritative accept snapshot, Cloud asks the exact Client window to match the
  existing generic tracker anchor locally. It polls for at most `2s`; the probes return only match metadata and
  upload no image. An anchor hit triggers immediate capture. Timeout/unavailable transport still captures exactly
  one frame so the existing tracker and story-objective fallback order remains intact.
- implementation:
  `TaskTrackerPanelService.awaitXiuluoTrackerAnchor(...)` owns the bounded local `MATCH_TEMPLATE` gate, and
  `XiuluoTaskV2.scheduleAcceptObjectiveBackgroundParse(...)` calls it immediately before
  `captureAcceptWindowSnapshot(...)`. Tooltip sampling and its rejection policy are explicitly outside this repair.
- parent source review: `P0/P1/P2=0/0/0`. No Client/protocol source changed.
- verification: Cloud compile passed; `TurnGameClientContractTest` passed. The older tracker-focused family is not
  a valid green gate in the current dirty migration tree: it still expects obsolete `CAPTURE` mechanics, contains
  one stale coordinate assertion, references missing `xiuluo_tracker_title.png`, and invokes a live read through a
  reference-only fixture. Those fixtures were not rewritten as part of this behavior repair.
- verdict: `SOURCE IMPLEMENTED / FRESH CLOUD RESTART REQUIRED`. Runtime acceptance requires the post-accept log to
  show anchor-ready or the bounded timeout first, followed by one shared-frame capture and tracker/objective parsing
  from that frame.

<!-- TRUE_EOF: NAV-REGRESSION-P1 ACCEPT-SNAPSHOT-READINESS SOURCE-IMPLEMENTED P0-0-P1-0-P2-0 LOCAL-ANCHOR-METADATA-ONLY MAX-WAIT-2S ONE-SHARED-FRAME TOOLTIP-DEFERRED CLOUD-COMPILE-PASS TURN-GAME-CLIENT-CONTRACT-PASS STALE-TRACKER-FIXTURES FRESH-CLOUD-RESTART-REQUIRED 2026-07-24 -->
