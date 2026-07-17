# PRECHECK_RISKS

- Role: CR271 non-binding delivery preflight helper only.
- Snapshot: `2026-07-15T19:39:02-04:00`.
- Scope read in full: `AGENTS.md`, `docs/DHXY_CONTEXT.md`, the top of `docs/ACTIVE_WORK.md`, authoritative plan sections 18-19, the current T03B report through true EOF, all six T03B tests, and their directly exercised production paths.
- No Maven, runtime, application, server, Task, UI, capture, input, or physical desktop action was run. No Java, test, plan, ACTIVE_WORK, PACKAGE, matrix, or Git metadata was changed.
- This is preflight evidence, not a parent decision.

## Snapshot Integrity

The six test files still match the SHA-256 values recorded by the T03B delivery report at lines 109-118:

| Test | SHA-256 |
|---|---|
| `LocalServiceExecutionContractTest.java` | `9986230091B752455454269DBB75B9517292F71CDB7F7F1D2843A955EE39C84A` |
| `LocalTurnActionExecutorContractTest.java` | `A63BFCB72063A754DC52C642A62F4EB4121168A5572E941B575D4C1C562A374C` |
| `WindowTurnLoopContractTest.java` | `C70AB2916D5A8A1C1A48DC1ED268B639635BE660C3F37B71A60A4D0F6EA1B8B3` |
| `TurnLoopRegistryConcurrencyTest.java` | `49FABE0FA9DBD6F1868B19C5E4F37C9C46F85A17CA41CCD4EAAAFDD9AD3EFF68` |
| `TurnModeGuardContractTest.java` | `E3C7E70316EFA486C0F262EE241CB35182633F14052B05DA6D02E7289B950219` |
| `TurnConfigurationWiringContractTest.java` | `46C8FC6EC187C588B92680F366E02ECEDBD768291BA97D286AA5A80B0F814A17` |

The current T03B report true EOF is line 162, 12,150 bytes, SHA-256
`16C161876B6D5D775EB8C8BBCF64D84D763355C42723E163B1A3B83441BDF705`.
It was extended by the parent while this preflight was in progress, so the latest EOF was reread.

## Production Boundary Check

The tests are not wholly self-certifying fakes:

- `LocalServiceExecutionContractTest.java:31-34,57-94,103-110` invokes the real record constructor/factories and real PNG/hash/dimension validation in `LocalServiceExecution.java:38-64,90-120`.
- `LocalTurnActionExecutorContractTest.java:74,123,148-150` invokes real `LocalTurnActionExecutor.execute`; window resolution, step dispatch, terminal shaping, failure capture, and final `ExecutedTurn` validation run through `LocalTurnActionExecutor.java:58-96`, `TurnExecutionWindow.java:51-87`, `TurnOutcomeAssembler.java:28-86`, and `ExecutedTurn.java:19-38`.
- `WindowTurnLoopContractTest.java:35-38,66-84` starts the real `WindowTurnLoop`; request retention, actionId caching, and execution suppression run through `WindowTurnLoop.java:151-250`.
- `TurnLoopRegistryConcurrencyTest.java:23-42,48-66,72-126` and `TurnModeGuardContractTest.java:34-77,85-105` call the real registry, loop lifecycle, and mode guard methods.
- `TurnConfigurationWiringContractTest.java:34-52` calls every bean method declared by `TurnConfiguration.java:14-52` and checks the resulting real types/counters/thread set.

The fake capture/input/client/manager objects are mechanics boundaries around those production calls. They do not themselves manufacture the asserted `TurnOutcome`, registry state, or mode conflict.

## Risk 1 - Previous Raw Frame Is Not Exercised

The frozen contract requires ACK to clear both previous outcome and frame, and uncertain transport to retain both. See the authoritative plan at `2026-07-15-https-turn-complete-migration-card-plan.md:1327-1329,1376` and the T03B report at lines 38-39.

Both loop tests instead use `TurnContractFixtures.clickAction(...)` (`LocalTurnActionExecutorContractTest.java:224-231`), which has only an INPUT step and cannot produce a frame. Their shared assertion explicitly requires every recorded upload to have no PNG (`WindowTurnLoopContractTest.java:105-110`). Therefore these tests would still pass if `WindowTurnLoop.previousPng` were dropped, failed to clear, reused incorrectly, or lost its defensive-copy behavior, even though the real source currently clones/retains/clears it at `WindowTurnLoop.java:198-212,249-250`.

Impact: outcome/actionId uncertainty is covered, but the raw-frame half of the same frozen retention contract is not.

Suggested revalidation: execute an action producing a real upload frame, assert byte identity on the first outcome upload and after one typed uncertain exchange plus explicit restart, mutate the fake client's received array to prove the retained copy is isolated, then assert the first request after accepted IDLE carries neither outcome nor PNG. Physical execution count must remain one.

## Risk 2 - Lifecycle Race Coverage Is Not Deterministic

`TurnLoopRegistryConcurrencyTest.java:69-126` launches one start/remove race after a start gate, but the gate only releases both Java threads; it does not force the two calls to overlap at the lifecycle critical point. A broken implementation can run the calls sequentially and still satisfy the one-winner assertion. The class has no concurrent start/stop case: `TurnLoopRegistryConcurrencyTest.java:45-67` starts, waits, stops, waits, and removes sequentially, although the frozen TURN-12 contract explicitly includes start/stop/remove races.

`TurnModeGuardContractTest.java:60-68` uses `remoteAttempted` before `startRemote(...)` and a 100 ms negative wait to infer that the remote thread is blocked on the shared monitor. If that thread is descheduled until after the fake local supplier sets `runner.running=true`, the test can produce the expected later conflict even if the two production methods did not share a monitor.

Impact: current production synchronization is visible at `WindowTurnLoop.java:61-82,86-93,141-148`, `TurnLoopRegistry.java:31-69`, and `TurnModeGuard.java:41-100`, but the tests are not deterministic mutation guards for the exact races that previously required repair.

Suggested revalidation: add controlled critical-point coordination for concurrent start/stop, start/remove, and local/remote start; assert the contender is blocked on the exact lifecycle/mode monitor before releasing the owner. Also exercise concurrent create/create for one `windowId` and prove exactly one registered usable loop.

## Risk 3 - Failure Paths Can Leak Live Threads

Passing paths stop and await their loops, but assertion/timeout paths do not have unconditional cleanup:

- `WindowTurnLoopContractTest.java:35-38,66-77` calls `stop/awaitStopped` only after blocking-entry assertions succeed.
- `TurnLoopRegistryConcurrencyTest.java:54-62,93-113` has no `finally`; if `ready.await` at line 95 fails, `go` is never released and both named race threads remain non-daemon and blocked. Failed assertions before lines 111-113 can also leave the daemon loop alive.
- `TurnModeGuardContractTest.java:60-68` releases the local supplier only after three assertions. A failure at lines 61, 63, or 64 can leave the local non-daemon thread blocked forever; later loop cleanup at lines 103-105 is likewise not unconditional.

Impact: precisely when a lifecycle regression occurs, the named Maven run can hang or retain a live loop instead of returning a clean assertion result.

Suggested revalidation: put every latch release, loop `stop/awaitStopped`, thread interrupt, and bounded join in `finally`; after cleanup, assert all helper threads and loops are stopped.

## Risk 4 - Unsafe Narrows What The Tests Prove

`LocalTurnActionExecutorContractTest.java:167,272-289` uses `sun.misc.Unsafe.allocateInstance`. It creates an unconstructed `BareWindowTaskRunner` and an unconstructed `LocalServiceStepDispatcher` (`:180,198`). The runner overrides every API consumed in these tests (`:335-378`), so this does not bypass the real executor/loop/registry/guard methods listed above. However, it also means those tests cannot detect incompatibility with the real `WindowTaskRunner` constructor, executor ownership, shutdown transition, or `getCurrentTask/isRunning/isShutdown` coupling. The dispatcher instance has impossible null final dependencies and is safe only because no tested action contains `LOCAL_SERVICE`.

Impact: this is a declared test-double boundary, not evidence about real runner construction or dispatcher invariants. A future production change that relies on such an invariant may be hidden by the synthetic state.

Suggested revalidation: either replace Unsafe with a supported inert runner seam/fully constructed fake graph, or state this exclusion explicitly and add a separate compatibility assertion at the real runner API boundary. Keep dispatcher behavior owned by the existing T04 tests.

## Risk 5 - Mechanical Order Is Inferred, Not Directly Recorded

`LocalTurnActionExecutorContractTest.java:57-107` verifies the ordered result vector, capture/input counts, terminal index, `NOT_RUN`, exact bound window, and full-frame replacement pixels. This strongly exercises the real failure path. It does not maintain a shared event ledger across WAIT/capture/input collaborators, so side effects could be reordered while preserving the same per-index result list and call counts.

Impact: the `LX` requirement at authoritative plan line 1272 says mechanics execute by index, not only that the assembled result list is ordered.

Suggested revalidation: record capture and input events in one shared list and assert exact event order; retain the existing full-window replacement pixel assertion and single input submission count.

## Current Production Repair Snapshot

During this preflight a separate owner changed `HttpsTurnClient.java`; `docs/ACTIVE_WORK.md:3-9` records that production repair as a disjoint write set. At the snapshot above, its SHA-256 is `B59A2834A67DB26B09EC8165D6774D734470F147E7C2FDC9A0B66AD4B72F72C6`. The constructor only stores validated configuration (`HttpsTurnClient.java:50-87`), and the single reusable `HttpClient` is lazy, volatile, and double-checked at first real send (`HttpsTurnClient.java:294-310`). This source shape addresses the eager selector-thread condition described in the current T03B report lines 143-156 without adding a second send or retry.

Repair #3 source delivery is now recorded at `2026-07-15-turn-card-TURN-T03A.md:385-424`; its parent check and Maven gates remain pending/deferred there. No command was run in this helper pass. Revalidation point: reread the final parent material, then run the original wiring test and the existing Https client named tests together after writers are stable.

## Baseline And Write-Set Check

- The examined paths are transport/mechanics only. They do not drive a real Fivefold/Xiuluo Task phase, park/yield, cleanup, fallback, or business retry. STOP is returned as `STOPPED` without later mechanics (`LocalTurnActionExecutorContractTest.java:111-135`), consistent with `docs/业务逻辑.md:1255-1266`. No `696a12b0` business difference was observed in this scope.
- Full-window replacement is real: the test first obtains an ROI frame, forces the INPUT failure, then asserts one full-window capture, `FAILURE_EVIDENCE` metadata, full-window pixels, and no tail execution at `LocalTurnActionExecutorContractTest.java:57-107`; production clears the earlier candidate before replacement at `LocalTurnActionExecutor.java:84-96`.
- Outcome/actionId uncertain no-re-execution is real: the loop test observes two requests before the typed failure, one physical queue submission, retained outcome after explicit restart, cached action reuse, and later IDLE clearing at `WindowTurnLoopContractTest.java:53-84`.
- The six test hashes match the delivery table, and current status shows those six tests plus the T03B report as the expected T03B material. Direct production files predate the T03B test claim except the separately recorded Https repair. In this already dirty/untracked shared tree, no T03B-specific write-set overreach was observed; unrelated dirty-file authorship cannot be inferred from status alone.

## Parent Recheck Points

1. Add raw previous-frame retention/clear and defensive-copy assertions to the loop scenario.
2. Make start/stop/remove and mode-guard races deterministic at the critical point.
3. Guarantee cleanup on every assertion/timeout path and assert no helper thread remains live.
4. Decide whether the Unsafe runner boundary is explicitly acceptable or replace it with a supported inert seam.
5. Record cross-collaborator mechanical execution order.
6. Re-read the final lazy-HTTP-client delivery and run only the six original named T03B tests plus the applicable client tests/compile when the parent opens that gate.
